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

otp.modules.alerts.causes = [
    { value: 'NONE', display: '(none)' },
    { value: 'TECHNICAL_PROBLEM', display: 'Problemas técnicos' },
    { value: 'STRIKE', display: 'Huelga' },
    { value: 'DEMONSTRATION', display: 'Manifestación' },
    { value: 'ACCIDENT', display: 'Accidente' },
    { value: 'HOLIDAY', display: 'Día festivo' },
    { value: 'WEATHER', display: 'Condiciones atmosféricas' },
    { value: 'MAINTENANCE', display: 'Mantenimiento' },
    { value: 'CONSTRUCTION', display: 'Obras' },
    { value: 'POLICE_ACTIVITY', display: 'Operativo policiaco' },
    { value: 'MEDICAL_EMERGENCY', display: 'Emergencia médica' },
    { value: 'UNKNOWN_CAUSE', display: 'Causa desconocida' },
    { value: 'OTHER_CAUSE', display: 'Otra causa' },
];

otp.modules.alerts.effects = [
    { value: 'NONE', display: '(none)' },
    { value: 'NO_SERVICE', display: 'Sin servicio' },
    { value: 'REDUCED_SERVICE', display: 'Servicio limitado' },
    { value: 'SIGNIFICANT_DELAYS', display: 'Retrasos significativos' },
    { value: 'DETOUR', display: 'Desvío' },
    { value: 'ADDITIONAL_SERVICE', display: 'Servicio adicional' },
    { value: 'MODIFIED_SERVICE', display: 'Servicio modificado' },
    { value: 'STOP_MOVED', display: 'Parada reubicada' },
    { value: 'OTHER_EFFECT', display: 'Otro efecto' },
    { value: 'UNKNOWN_EFFECT', display: 'Efecto desconocido' },
];

otp.modules.alerts.EditAlertView = Backbone.View.extend({

    events : {
        'keyup textarea' : 'descriptionTextChanged',
        'keyup input' : 'headerTextChanged',
        'click #addRangeButton' : 'addRangeButtonClicked',
        'click .otp-alerts-editAlert-deleteRangeButton' : 'deleteRangeButtonClicked',
        'click .otp-alerts-editAlert-deleteEntityButton' : 'deleteEntityButtonClicked',
        'click .entityRowLabel' : 'entityRowClicked',
        'change .otp-alerts-editAlert-causeSelect' : 'causeChanged',
        'change .otp-alerts-editAlert-effectSelect' : 'effectChanged',
    },    
    
    render : function() {
        var this_ = this;
        
        var context = this.options.widget.module.prepareAlertTemplateContext(this.model); //_.clone(this.model.attributes);
        var rangeIndex = entityIndex = 0;
        context = _.extend(context, {
            widgetId : this.options.widget.id,
            renderDate : function() {
                return function(date, render) { return moment(parseInt(render(date))).format(otp.config.dateFormat+' '+otp.config.timeFormat); }
            },
            rangeIndex: function() { return rangeIndex++; },
            entityIndex: function() { return entityIndex++; },
            causes: otp.modules.alerts.causes,
            effects: otp.modules.alerts.effects,
        });
        
        this.$el.html(ich['otp-alerts-alertEditor'](context));

        $("#"+this.options.widget.id+'-accordion').accordion({
            active : (this.activeAccordionIndex || 0),
            activate : function( event, ui ) {
                this_.activeAccordionIndex = $('#'+this_.options.widget.id+'-accordion').accordion('option', 'active');
                
            }
        });
        
        // set up the date/time pickers for the 'create new timerange' input
        $("#"+this.options.widget.id+'-rangeStartInput').datetimepicker({
            timeFormat: "h:mmtt", 
        }).datepicker("setDate", new Date());

        $("#"+this.options.widget.id+'-rangeEndInput').datetimepicker({
            timeFormat: "h:mmtt",
        }).datepicker("setDate", new Date());

        // allow the entities list to accept route/stop elements via drag & drop
        $("#"+this.options.widget.id+'-entitiesList').droppable({
            accept: '.otp-alerts-draggableEntity', //'.otp-alerts-entitiesWidget-entityRow',
            hoverClass: 'otp-alerts-editAlert-entitiesList-dropHover',
            drop: function(event, ui) { this_.handleEntityDrop(event, ui); }
        });

        // select the current cause/effect items
        if(this.model.get('cause')) {
            $('#'+this.options.widget.id+'-causeSelect option[value="'+this.model.get('cause')+'"]').prop('selected', true)
        }

        if(this.model.get('effect')) {
            $('#'+this.options.widget.id+'-effectSelect option[value="'+this.model.get('effect')+'"]').prop('selected', true)
        }
    },

    descriptionTextChanged : function(event) {
        var text = $('#'+this.options.widget.id+'-descriptionText').val();
        this.model.set('descriptionText', text);
    },

    headerTextChanged : function(event) {
        var text = $('#'+this.options.widget.id+'-headerText').val();
        this.model.set('headerText', text);
    },
    
    addRangeButtonClicked : function(event) {
        var start = moment($("#"+this.options.widget.id+'-rangeStartInput').val(), "MM/DD/YYYY "+otp.config.timeFormat).unix();
        var radio = $('input:radio[name='+this.options.widget.id+'-rangeEndRadio'+']:checked').val();
        var end = (radio === "indefinitely") ? null :
            moment($("#"+this.options.widget.id+'-rangeEndInput').val(), "MM/DD/YYYY "+otp.config.timeFormat).unix();
        
        if(end != null)
            end = end * 1000;

        this.model.attributes.timeRanges.push({
            startTime: start * 1000,
            endTime: end
        });
        this.render();
    },

    deleteRangeButtonClicked : function(event) {
        var index = parseInt(event.target.id.split('-').pop());
        this.model.attributes.timeRanges.splice(index, 1);
        this.render();
    },
    
    entityRowClicked : function(event) {
        var parentId = $($(event.target).parent()).attr('id');
        var index = parseInt(parentId.split('-').pop());
        var entity = this.model.attributes.informedEntities[index];
        
        if(entity.routeId !== null) {
            this.options.widget.module.drawRoute(entity.agencyId + "_" + entity.routeId);
        }
        if(entity.stopId !== null) {
            // TODO: show stop on map
        }
    },
    
    deleteEntityButtonClicked : function(event) {
        var parentId = $($(event.target).parent()).attr('id');
        var index = parseInt(parentId.split('-').pop());
        this.model.attributes.informedEntities.splice(index, 1);
        this.render();
    },
    
    causeChanged : function(event) {
        var cause = $('#'+this.options.widget.id+'-causeSelect').val();
        if(cause === "NONE") this.model.set('cause', null);
        else this.model.set('cause', cause);
    },
    
    effectChanged : function(event) {
        var effect = $('#'+this.options.widget.id+'-effectSelect').val();
        if(effect === "NONE") this.model.set('effect', null);
        else this.model.set('effect', effect);
    },
    
    handleEntityDrop : function(event, ui) {
        
        var route = $(ui.draggable.context).data('route');
        if(typeof route !== 'undefined' && route !== null) {
            if(!this.options.widget.module.isValidAgency(route.id.agencyId)) return;
            this.model.attributes.informedEntities.push({
                agencyId : route.id.agencyId,
                routeId : route.id.id,
                description: route.routeShortName
            });
        }

        var stop = $(ui.draggable.context).data('stop');
        if(typeof stop !== 'undefined' && stop !== null) {
            if(!this.options.widget.module.isValidAgency(stop.id.agencyId)) return;
            this.model.attributes.informedEntities.push({
                agencyId : stop.id.agencyId,
                stopId : stop.id.id,
                description : stop.stopName
            });
        }

        this.render();
    }
    
});


otp.modules.alerts.EditAlertWidget = 
    otp.Class(otp.widgets.Widget, {
    
    module : null,
    
    alertObj : null,
    
    affectedRoutes : [],
    affectedStops : [],

    initialize : function(id, module, alertObj) {
        var this_ = this;
        otp.widgets.Widget.prototype.initialize.call(this, id, module, {
            title : (alertObj.get('id') == null) ? 'Crear aviso' : 'Editar aviso #'+alertObj.get('id'),
            cssClass : 'otp-alerts-editAlertWidget',
            closeable: true,
        });
        
        // hack to force first valid agency as default...
        alertObj.set('agencyId', module.validAgencies[0]);           

        this.module = module;
        this.alertObj = alertObj;
 
        // set up the view 
        var view = new otp.modules.alerts.EditAlertView({
            el: $('<div />').appendTo(this.mainDiv),
            model: alertObj,
            widget: this,
        });
        
        
        // create the save and delete buttons
        var buttonRow = $('<div>').addClass('otp-alerts-entitiesWidget-buttonRow').appendTo(this.mainDiv)
        
        $(Mustache.render(otp.templates.button, { text : "Guardar"}))
        .button().appendTo(buttonRow).click(function() {
            this_.module.saveAlert(this_.alertObj);
            this_.close();
        });

        $(Mustache.render(otp.templates.button, { text : "Borrar"}))
        .button().appendTo(buttonRow).click(function() {
            this_.module.deleteAlert(this_.alertObj);
            this_.close();
        });

        view.render();

    },
    
    onClose : function() {
        delete this.module.openEditAlertWidgets[this.alertObj.get('id')];
    }
    
});

