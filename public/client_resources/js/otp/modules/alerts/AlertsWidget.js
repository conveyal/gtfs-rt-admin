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

otp.modules.alerts.AlertsWidget = 
    otp.Class(otp.widgets.Widget, {
    
    module : null,
    
    routesLookup : null,
    stopsLookup : null,
    
    affectedRoutes : [],
    affectedStops : [],
    
    filterMode : 'current',

    initialize : function(id, module, routes, stops) {
        var this_ = this;
        this.module = module;
        otp.widgets.Widget.prototype.initialize.call(this, id, module, {
            title : 'Interrupciones',
            cssClass : 'otp-alerts-alertsWidget',
            closeable: false
        });
        
        ich['otp-alerts-filterRadio']({
            widgetId : this.id,
            initialStartDate : moment().format("MM/DD/YYYY"),
            initialEndDate : moment().add('d',30).format("MM/DD/YYYY"),
        }).appendTo(this.mainDiv);
        $('input:radio[name='+this.id+'-filterRadio]').click(function() {
            this_.filterMode = $('input:radio[name='+this_.id+'-filterRadio]:checked').val();
            this_.refreshAlerts(this_.module.alerts);
        })
        $('#'+this.id+'-rangeStartInput').datepicker()
        .change(function() {
            this_.refreshAlerts(this_.module.alerts);
        });
        $('#'+this.id+'-rangeEndInput').datepicker()
        .change(function() {
            this_.refreshAlerts(this_.module.alerts);
        });
            
        this.alertsList = $(Mustache.render(otp.templates.div, {
            id : this.id+'-alertsList',
            cssClass : 'otp-alerts-alertsWidget-alertsList notDraggable'
        })).appendTo(this.mainDiv);
        
        // set up the 'new alert' button
        var buttonRow = $('<div>').addClass('otp-alerts-entitiesWidget-buttonRow').appendTo(this.mainDiv)
        
        $(Mustache.render(otp.templates.button, { text : "Crear Interrupción"}))
        .button().appendTo(buttonRow).click(function() {
            this_.module.newAlertWidget();
        });        
    },
    
    refreshAlerts : function(alerts) {
        var this_ = this;
        this.alertsList.empty();
        for(var i = 0; i < alerts.length; i++) {

            if(!this.filterAlert(alerts.at(i))) continue;

            var context = this.module.prepareAlertTemplateContext(alerts.models[i]);

            var routeIdArr = [], stopIdArr = [];
            for(var e = 0; e < context.informedEntities.length; e++) {
                var entity = context.informedEntities[e];
                if(entity.routeId) routeIdArr.push(entity.routeReference);
                if(entity.stopId) stopIdArr.push(entity.description);
            }
            context['routeIds'] = routeIdArr.join(', ');
            context['stopIds'] = stopIdArr.join(', ');
            
            ich['otp-alerts-alertRow'](context).appendTo(this.alertsList)
            .data('alertObj', alerts.at(i))
            .click(function() {
                var alertObj = $(this).data('alertObj');
                this_.module.editAlertWidget(alertObj);
            });
        }
    },
    
    filterAlert : function(alert) {
        // if no time information, show it regardless of mode
        if(typeof alert.attributes.timeRanges == 'undefined' || alert.attributes.timeRanges == null || alert.attributes.timeRanges.length == 0) {
            return true;
        }
        
        var filterStart = filterEnd = null;
        var now = moment().unix() * 1000;
        for(var i = 0; i < alert.attributes.timeRanges.length; i++) {
            //var alert = alert.timeRanges[i];
            var start = alert.attributes.timeRanges[i].startTime;
            var end = (alert.attributes.timeRanges[i].endTime !== null) ? alert.attributes.timeRanges[i] : null;

            if(this.filterMode == 'current') {
                if(start <= now && (end == null || end >= now)) {
                    return true;
                }
            }
            if(this.filterMode == 'range') {
                filterStart = filterStart || moment($('#'+this.id+'-rangeStartInput').val()).unix();
                filterEnd = filterEnd || moment($('#'+this.id+'-rangeEndInput').val()).unix();

                // timeRange straddles start of filterRange
                if(start <= filterStart && (end == null || end >= filterStart)) {
                    return true;                    
                }
                // timeRange straddles end of filterRange
                if(start <= filterEnd && (end == null || end >= filterEnd)) {
                    return true;
                }
                // timeRange completely contained in filterRange
                if(start >= filterStart && start <= filterEnd && (end != null || (end >= filterStart && end <= filterEnd))) {
                    return true;
                }
            }        
        }
        
        return false;
    },
});
