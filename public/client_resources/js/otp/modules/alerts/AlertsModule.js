/* This program is free software: you can redistribute it and/or
   modify it under the terms of the GNU Lesser General Public License
   as published by the Free Software Foundation, either version 3 of
   the License, or (at your option) any later version.
   
   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.
   
   You should have received a copy of the GNU General Public License
   along with this program.  If not, see <http://www.gnu.org/licenses/>. 
*/

otp.namespace("otp.modules.alerts");


otp.modules.alerts.AlertsModule = 
    otp.Class(otp.modules.Module, {
    
    moduleName  : "Alerts Manager",

    minimumZoomForStops : 14,
    
    openEditAlertWidgets : { }, // maps the alert id to the widget object
    
    routeStops : { },
    windowStops : { },
    
    validAgencies : [],
    
    initialize : function(webapp) {
        otp.modules.Module.prototype.initialize.apply(this, arguments);        
        
        this.validAgencies = [ 'METRO' ];
    },
    
    activate : function() {
        if(this.activated) return;

        var this_ = this;
        
        $.get(otp.config.resourcePath + 'js/otp/modules/alerts/alerts-templates.html')
        .success(function(data) {
            $('<div style="display:none;" />').appendTo($("body")).html(data);
            ich.grabTemplates();
            
            this_.webapp.transitIndex.loadRoutes(this_, function() {
            
                this_.alertsWidget = new otp.modules.alerts.AlertsWidget('otp-'+this_.id+'-alertsWidget', this_);
                this_.fetchAlerts();

                this_.entitiesWidget = new otp.modules.alerts.EntitiesWidget('otp-'+this_.id+'-entitiesWidget', this_);
            });
        });
            
        otp.modules.Module.prototype.activate.apply(this);

        this.stopHighlightLayer = new L.LayerGroup();
        this.routeHighlightLayer = new L.LayerGroup();
        this.stopsLayer = new L.LayerGroup();
        this.routesLayer = new L.LayerGroup();
    
        this.addLayer("Route Highlights", this.routeHighlightLayer);
        this.addLayer("Stop Highlights", this.stopHighlightLayer);
        this.addLayer("Stops", this.stopsLayer);
        this.addLayer("Routes", this.routesLayer);

        this.activated = true;
    },
    
    mapBoundsChanged : function(event) {
        if(this.webapp.map.lmap.getZoom() >= this.minimumZoomForStops) {
            this.webapp.transitIndex.loadStopsInRectangle(null, this.webapp.map.lmap.getBounds(), this, function(data) {
                this.windowStops = { };
                for(var i = 0; i < data.stops.length; i++) {
                    if(!this.isValidAgency(data.stops[i].id.agencyId)) continue;
                    var agencyAndId = data.stops[i].id.agencyId + "_" + data.stops[i].id.id;
                    this.windowStops[agencyAndId] = data.stops[i];
                }
                this.updateStops();
            });
        }
        else {
            var diff = this.minimumZoomForStops - this.webapp.map.lmap.getZoom();
            if(this.entitiesWidget)
                this.entitiesWidget.stopsText("<i>Please zoom an additional " + diff + " zoom level" + (diff > 1 ? "s" : "") + " to see stops.</i>");
            //if(this.stopsLayer) this.stopsLayer.clearLayers();
            this.windowStops = {};
            this.updateStops();
        }

    },
    
    updateStops : function() {
        var this_ = this;
        this.stopsLayer.clearLayers();
        
        var stops = _.values(_.extend(_.clone(this.routeStops), this.windowStops));
        this.entitiesWidget.updateStops(stops);
        
        for(var i=0; i<stops.length; i++) {
            var stop = stops[i];
            //console.log(stop);
            
            var icon = L.divIcon({
                className : 'otp-alerts-stopIcon',
                iconSize: [13,23],
                iconAnchor: [7,23],
                html: '<div id="stopMarker-'+i+'" class="otp-alerts-draggableEntity otp-alerts-stopMarker" />'
            });
            
            var popupContent = ich['otp-alerts-stopPopup'](stop) //$('<div>Routes serving this stop (drag to add to alert):</div>'); 
            if(stop.routes) {
                for(var r = 0; r < stop.routes.length; r++) {
                    var agencyAndId = stop.routes[r].agencyId + '_' + stop.routes[r].id;
                    var routeData = this.webapp.transitIndex.routes[agencyAndId].routeData;
                    ich['otp-alerts-routeRow'](routeData).appendTo(popupContent)
                    .data('route', routeData)
                    .draggable({
                        helper: 'clone',
                        revert: 'invalid',
                        appendTo: 'body',
                        zIndex: '100000',
                        drag: function(event,ui){ 
                            $(ui.helper).css("border", '2px solid gray');
                        }                    
                    });
                    
                }
            }
                    
            L.marker([stop.stopLat || stop.lat, stop.stopLon || stop.lon], {
                icon : icon,
            }).addTo(this.stopsLayer)
            .bindPopup(popupContent.get(0), {
                offset: new L.Point(0, -20),
            });

            $('#stopMarker-'+i).data('stop', stop)
            .draggable({
                helper : 'clone',
                appendTo : 'body',
                zIndex: 100000,
                revert : 'invalid',
            }).hover(function(evt, ui) { // disable map panning before dragging begins
                this_.webapp.map.lmap.dragging.disable();
            }, function(evt, ui) {
                this_.webapp.map.lmap.dragging.enable();
            });
            
        }
    },
    
    alertWidgetCount : 0,
    
    newAlertWidget : function(affectedRoutes, affectedStops) {
        var alertObj = new otp.modules.alerts.Alert({
            timeRanges : [],
            informedEntities : []
        });
        
        if(affectedRoutes) {
            for(var i = 0; i < affectedRoutes.length; i++) {
                console.log(affectedRoutes[i]);
                var entity = {
                    agencyId: affectedRoutes[i].routeData.id.agencyId,
                    routeId: affectedRoutes[i].routeData.id.id
                };
                alertObj.attributes.informedEntities.push(entity);
            }
        }
        
        if(affectedStops) {
            for(var i = 0; i < affectedStops.length; i++) {
                var entity = {
                    agencyId: affectedStops[i].id.agencyId,
                    stopId: affectedStops[i].id.id
                };
                alertObj.attributes.informedEntities.push(entity);
            }
        }
                        
        var widget = new otp.modules.alerts.EditAlertWidget('otp-'+this.id+'-editAlertWidget-'+this.alertWidgetCount, this, alertObj);
        widget.bringToFront();
        this.alertWidgetCount++;
    },
    
    editAlertWidget : function(alertObj) {
        if(_.has(this.openEditAlertWidgets, alertObj.get('id'))) {
            var widget = this.openEditAlertWidgets[alertObj.get('id')];
            if(widget.minimized) widget.unminimize();
        }
        else {
            var widget = new otp.modules.alerts.EditAlertWidget('otp-'+this.id+'-editAlertWidget-'+this.alertWidgetCount, this, alertObj);
            this.openEditAlertWidgets[alertObj.get('id')] = widget;
            this.alertWidgetCount++;
        }
        widget.bringToFront();
    },    
    
    fetchAlerts : function() {
        var this_ = this;
        // fetch data from server
        this.alerts = new otp.modules.alerts.Alerts();
        this.alerts.fetch({success: function(collection, response, options) {
            this_.alertsWidget.refreshAlerts(this_.alerts);
        }});
    },
            
    saveAlert : function(alertObj) {
        var this_ = this;
        alertObj.save({}, {
            success : function() {
                console.log("saved!");
                this_.fetchAlerts();
            }
        });
    },
    
    deleteAlert : function(alertObj) {
        var this_ = this;
        alertObj.destroy({
            dataType: "text", // success is not triggered unless we do this
            success : function() {
                console.log("deleted!");
                this_.fetchAlerts();
            },
            error: function(model, response) {
                console.log("Error deleting");
                console.log(response);
            }            
        });
    },

    highlightRoute : function(agencyAndId) {
        this.webapp.transitIndex.loadVariants(agencyAndId, this, function(variants) {
            for(variantName in variants) {
                var polyline = new L.Polyline(otp.util.Geo.decodePolyline(variants[variantName].geometry.points));
                polyline.setStyle({ color : "#3cf", weight: 6, opacity: 0.4 });
                this.routeHighlightLayer.addLayer(polyline);            
            }
        });
    },
    
    highlightStop : function(stopObj) {
        L.marker([stopObj.stopLat || stopObj.lat, stopObj.stopLon || stopObj.lon]).addTo(this.stopHighlightLayer);
    },

    
    clearHighlights : function() {
        this.stopHighlightLayer.clearLayers(); 
        this.routeHighlightLayer.clearLayers(); 
    },
    
    drawRoute : function(agencyAndId) {
        if(!this.isValidAgency(agencyAndId.split('_')[0])) return;
        this.routeHighlightLayer.clearLayers(); 
        this.routesLayer.clearLayers(); 
        this.routeStops = {};
        this.webapp.transitIndex.loadVariants(agencyAndId, this, function(variants) {
            for(variantName in variants) {
                var variant = variants[variantName];
                var polyline = new L.Polyline(otp.util.Geo.decodePolyline(variant.geometry.points));
                polyline.setStyle({ color : "#39f", weight: 6, opacity: 0.4 });
                this.routesLayer.addLayer(polyline);            
                var stops = { };
                for(var i = 0; i < variant.stops.length; i++) {
                    var agencyAndId = variant.stops[i].id.agencyId + "_" + variant.stops[i].id.id;
                    this.routeStops[agencyAndId] = variant.stops[i];                    
                }
                _.extend(this.routeStops, stops);
            }
            this.updateStops();
        });
        this.panToRoute(agencyAndId);
    },

    panToRoute : function(agencyAndId) {
        var this_ = this;
        this.webapp.transitIndex.loadVariants(agencyAndId, this, function(variants) {
            var latLngs = [];
            for(variantName in variants) {
                var variant = variants[variantName];
                var polyline = new L.Polyline(otp.util.Geo.decodePolyline(variant.geometry.points));
                latLngs = latLngs.concat(polyline.getLatLngs())
            }
            this_.webapp.map.lmap.fitBounds(new L.LatLngBounds(latLngs));
        });
    },    
    
    
    prepareAlertTemplateContext : function(alertObj) {

        var context = _.clone(alertObj.attributes);

        var informedEntitiesCopy = [];
        for(var i=0; i < context.informedEntities.length; i++) {
            var informedEntityCopy = _.clone(context.informedEntities[i]);
            if(informedEntityCopy.routeId) {
                var agencyAndId = informedEntityCopy.agencyId + '_' + informedEntityCopy.routeId; 
                var routeData = this.webapp.transitIndex.routes[agencyAndId].routeData;
                informedEntityCopy.routeReference = otp.util.Itin.getRouteShortReference(routeData);
            }
            informedEntitiesCopy.push(informedEntityCopy);
        }
        context.informedEntities = informedEntitiesCopy;
        
        return context;
    },
    
    isValidAgency : function(agencyId) {
        return _.contains(this.validAgencies, agencyId);
    },
});
