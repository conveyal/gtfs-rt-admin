var GtfsRt = GtfsRt || {};

(function(G, $) {

  G.Alert = Backbone.Model.extend({
    urlRoot: '/api/alert/',
    
    defaults: {
      id: null,
      timeRanges: [],
      informedEntities: [],
      cause: null,
      effect: null,
      url: null,
      descriptionText: null
    }
  });

  G.Alerts = Backbone.Collection.extend({
    type: 'Alerts',
    model: G.Alert,
    url: '/api/alert/'
  });

  G.TimeRange = Backbone.Model.extend({
    urlRoot: '/api/tr/',

    defaults: {
      id: null,
      startTime: null,
      endTime: null,
    }
  });
  
  G.TimeRanges = Backbone.Collection.extend({
    type: 'TimeRanges',
    model: G.TimeRange,
    url: '/api/tr/'
  });

  G.InformedEntity = Backbone.Model.extend({
    urlRoot: '/api/ie/',

    defaults: {
      id: null,
      agencyId: null,
      routeId: null,
      stopId: null
    }
  });
  
  G.InformedEntities = Backbone.Collection.extend({
    type: 'InformedEntities',
    model: G.InformedEntity,
    url: '/api/ie/'
  });

 
})(GtfsRt, jQuery);
                                                                
