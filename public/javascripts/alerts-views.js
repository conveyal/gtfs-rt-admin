	
var GtfsRtEditor = GtfsRtEditor || {};


(function(G, $) {


G.AffectedTimesView = Backbone.View.extend({

	events : {    
		'click #removeRange' : 'removeRange',
		'click #addRange' : 'addRange',
    },

	initialize: function() {

		_.bindAll(this, 'addRange', 'removeRange', 'dateChange');

		this.listenTo(this.model, "change", this.render);

		var source   = $("#affectedTimesTemplate").html();
		this.template = Handlebars.compile(source);

		this.render();

	},

	render : function(){

		if(this.model.get('timeRanges').length == 0) {
			var range = {};
			var ranges = [];
			ranges.push(range);
			this.model.set('timeRanges', ranges);
		}

		data = [];		

		for(i in  this.model.get('timeRanges')) {
			var e = this.model.get('timeRanges')[i]	;
			e.pos = i;
			data.push(e);
		}

		this.$el.html(this.template({ranges: data}));

		for(i in  this.model.get('timeRanges')) {

			this.$('#dtFrom_' + i).datetimepicker({language: 'es'}).data('datetimepicker').setDate(new Date(this.model.get('timeRanges')[i].startTime));
			
			this.$('#dtFrom_' + i).on('changeDate', this.dateChange);

			this.$('#dtTo_' + i).datetimepicker({language: 'es'}).data('datetimepicker').setDate(new Date(this.model.get('timeRanges')[i].endTime));

			this.$('#dtTo_' + i).on('changeDate', this.dateChange);
		}

	},

	dateChange : function(evt) {
		var pos = $(evt.target).data('position');
		var fromto = $(evt.target).data('fromto');

		var existingRanges = this.model.get('timeRanges');

		if(fromto == 'from') {
			existingRanges[pos].startTime = evt.date.getTime();
		}
		else if(fromto == 'to') {
			existingRanges[pos].endTime = evt.date.getTime();
		}

		this.model.set('timeRanges', existingRanges);

		// focing change;
		this.model.forceChange(); 
	},

	addRange : function() {
	
		var range = {};

		var existingRanges = this.model.get('timeRanges');

		var ranges = [];

		for(i in existingRanges) {
			ranges.push(existingRanges[i])
		}

		ranges.push(range);

		this.model.set('timeRanges', ranges);
	},

	removeRange : function (evt) {

		var pos = $(evt.target).data('position');

		var existingRanges = this.model.get('timeRanges');

		var ranges = [];

		for(i in existingRanges) {
			if(i != pos)
				ranges.push(existingRanges[i])
		}

		this.model.set('timeRanges', ranges);
	}

});


G.SelectedEntityView = Backbone.View.extend({

	events : {    
		'click #removeEntity' : 'removeEntity'
    },

	initialize: function() {

		_.bindAll(this, 'removeEntity')

		this.listenTo(this.model, "change", this.render);

		var source   = $("#selectedEntitiesTemplate").html();
		this.template = Handlebars.compile(source);

		this.render();

	},

	render : function(){

		data = []

		for(i in  this.model.get('informedEntities')) {
			var e = this.model.get('informedEntities')[i];
			e.pos = i;
			data.push(e);
		}

		this.$el.html(this.template({entities: data}));

	},

	removeEntity : function (evt) {

		var pos = $(evt.target).data('position');

		var existingEntities = this.model.get('informedEntities');

		var entities = [];

		for(i in existingEntities) {
			if(i != pos)
				entities.push(existingEntities[i])
		}

		this.model.set('informedEntities', entities);
	},

	addStop : function(stopId, stopName) {
		
		var entity = {stopId: stopId, description: stopName};

		var existingEntities = this.model.get('informedEntities');

		var entities = [];

		for(i in existingEntities) {
			entities.push(existingEntities[i])
		}

		entities.push(entity);

		this.model.set('informedEntities', entities);

	},

	addRoute : function(routeId, routeName) {
	
		var entity = {routeId: routeId, description: routeName};

		var existingEntities = this.model.get('informedEntities');

		var entities = [];

		for(i in existingEntities) {
			entities.push(existingEntities[i])
		}

		entities.push(entity);

		this.model.set('informedEntities', entities);
	}

});

G.AlertEditorView = Backbone.View.extend({

	events : {    
		'click #addStop' : 'addStop',
		'click #addRoute' : 'addRoute',
		'click #saveAlert' : 'saveAlert',
		'change #title' : 'onFieldChange',
		'change #description' : 'onFieldChange',
		'change #comments' : 'onFieldChange',
		'change #cause' : 'onFieldChange',
		'change #effect' : 'onFieldChange',
		'change #publiclyVisible' : 'onFieldChange',
		'click #showMap' : 'showMap',
		'click #hideMap' : 'hideMap',
		'change select#route' : 'changeRoute',
		'change select#stop' : 'changeStop'
    },

	initialize: function() {

	  this.agencySelectedStopIcon = L.icon({
        iconUrl: '/public/leaflet/images/marker-icon.png',
        iconSize: [25, 41],
        iconAnchor: [12, 41],
        popupAnchor: [1, -34],
        shadowUrl: '/public/leaflet/images/marker-shadow.png',
        shadowSize: [41, 41]
      });

      // Custom icons
      this.agencyStopIcon = L.icon({
        iconUrl: '/public/leaflet/images/marker-gray.png',
        iconSize: [25, 41],
        iconAnchor: [12, 41],
        popupAnchor: [1, -34],
        shadowUrl: '/public/leaflet/images/marker-shadow.png',
        shadowSize: [41, 41]
      });


		var source   = $("#alertFromTemplate").html();
		this.template = Handlebars.compile(source);

		this.render();

		this.listenTo(this.model, "dateChange", this.enableSave);
		this.listenTo(this.model, "change", this.enableSave);

		_.bindAll(this, 'loadRoutes', 'loadStops', 'addRoute', 'addStop', 'saveAlert', 'onFieldChange', 'enableSave', 'showMap', 'hideMap', 'changeStop', 'changeRoute')

	},

	render : function(){

		var this_ = this;

		this.$el.html(this.template());

		this.$('#save-warning').hide();

		this.hideMap();

		this.loadRoutes();

		this.$("select#route").change(function(){
	    	this_.loadStops();
	    });

		this.setFields();

	    this.entityView = new G.SelectedEntityView({
	    	model: this.model,
	    	el: this.$('#selected-entities')
	    });

	    this.timeView = new G.AffectedTimesView({
	    	model: this.model,
	    	el: this.$('#affected-times')
	    });

	    if(this.model.attributes.id) {
	    	this.model.fetch({success: function() {
	    		this_.setFields();
	    	}})
	    }
		
	},

	enableSave : function() {
		$('#saveAlert').prop('disabled', false);
	},

	loadRoutes : function() {

		var this_ = this;

		$.getJSON("/api/routes", function(routes){
	      var options = '';
	      this_.routes = routes;
	      for (var i = 0; i < routes.length; i++) {
	        options += '<option value="' + routes[i].id + '">' + routes[i].value.shortName + ' ' + routes[i].value.longName + '</option>';
	      }
	      this_.$("select#route").html(options);
	      
	      this_.loadStops();
	      this_.updateOverlay(false);
	    });

	},

	loadStops : function() {

		var this_ = this;

		$.getJSON("/api/stops/" + this_.$("select#route").val(), function(stops){
	      
	      var options = '';
	      this_.stops = stops;
	      
	      for (var i = 0; i < stops.length; i++) {
	        options += '<option value="' + stops[i].id + '">' + stops[i].value.name + '</option>';
	      }
	      this_.$("select#stop").html(options);
	      this_.updateOverlay(false);
	    });

	},

	addStop : function() {
		var stopId = this.$("select#stop").val();
		var stopName = this.$("select#stop option:selected").text();
		
		this.entityView.addStop(stopId, stopName);

	},

	addRoute : function() {
		var routeId = this.$("select#route").val();
		var routeName = this.$("select#route option:selected").text();
		
		this.entityView.addRoute(routeId, routeName);
	},

	setFields : function(evt) {

		this.$('#title').val(this.model.get('headerText'));
		this.$('#description').val(this.model.get('descriptionText'));
		this.$('#comments').val(this.model.get('commentsText'));
		
		if(this.model.get('cause'))
			this.$('#cause').val(this.model.get('cause'));

		if(this.model.get('effect'))
			this.$('#effect').val(this.model.get('effect'));

		if(this.model.get('publiclyVisible') == true)
			this.$('#publiclyVisible').prop('checked', true);


	},

	onFieldChange : function(evt) {

		this.model.set('headerText', this.$('#title').val());
		this.model.set('descriptionText', this.$('#description').val());
		this.model.set('commentsText', this.$('#comments').val());
		this.model.set('cause', this.$('#cause').val());
		this.model.set('effect', this.$('#effect').val());
		this.model.set('publiclyVisible', this.$('#publiclyVisible').is(":checked"));

	},

	saveAlert : function() {

		var this_ = this;

		this.$('#save-warning').hide();

		this.model.save({}, {success : function() {

				this_.$('#saveAlert').prop('disabled', true);

			}, 
			error: function() {

				this.$('#save-warning').show();
				this_.$('#saveAlert').prop('disabled', false);

			}

		});
	},

	initMap : function() {

		if(!this.entitiesMap) {
			this.entitiesMap = L.map('entitiesMap').setView([51.505, -0.09], 13);
			

			this.entitiesLayer = L.tileLayer('http://{s}.tiles.mapbox.com/v3/' + G.config.mapKey + '/{z}/{x}/{y}.png', {
	    		attribution: '<a href="http://mapbox.com/about/maps">Terms & Feedback</a>'
				}).addTo(this.entitiesMap);

			this.updateOverlay();
		}

	},


	updateOverlay : function(centerStop) {
		if(this.entitiesMap) {

			if(!this.entitiesOverlay) {
				this.entitiesOverlay = L.featureGroup().addTo(this.entitiesMap)
			}

			this.entitiesOverlay.clearLayers()

			for(r in this.routes) {

				var route = this.routes[r];

				if(route.id == this.$("select#route").val()) {
					
					var polyline = L.Polyline.fromEncoded(route.value.polyline);
					polyline.addTo(this.entitiesOverlay);
				}
			}

			var selectedStop = null

			var this_ = this;

			for(s in this.stops) {
				
				var stop = this.stops[s];

				marker = this.agencyStopIcon;

				if(stop.id == this.$("select#stop").val()) {
					selectedStop = stop;					
					marker = this.agencySelectedStopIcon
				}
					

				var m = L.marker([stop.value.lat, stop.value.lon], {riseOnHover: true, icon: marker})
					.on('click', function(evt) {  
						this_.$("select#stop").val(evt.target.stopId);
						this_.changeStop();
						})
					.bindLabel(stop.value.name).addTo(this.entitiesOverlay);

				m.stopId = stop.id;
			}

			if(centerStop)
				this.entitiesMap.setView([selectedStop.value.lat, selectedStop.value.lon], 17)
			else
				this.entitiesMap.fitBounds(this.entitiesOverlay.getBounds());
		}
	},

	showMap : function() {

		$('#mapRow').show();
		$('#hideMap').show();
		$('#showMap').hide();

		this.initMap();
	},

	hideMap : function() {

		$('#mapRow').hide();
		$('#hideMap').hide();
		$('#showMap').show();
	},

	changeStop : function() {
		this.updateOverlay(true);
	},

	changeRoute : function() {
		this.updateOverlay(false);
	}

});


})(GtfsRtEditor, jQuery);
