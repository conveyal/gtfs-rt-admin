$(document).ready(function() {
   // page init scripts
  

   // add language select handler
     $("#runDemo").click(function(e){

        var alertObj = new GtfsRt.Alert();

        alertObj.set('cause', 'TECHNICAL_PROBLEM');

        var now  = (new Date()).getTime();
      
        var timeRange = new GtfsRt.TimeRange();

        timeRange.set('startTime', now);
        timeRange.set('endTime', now);

        var timeRanges = [];
        timeRanges.push(timeRange);

        alertObj.set('timeRanges', timeRanges);

    
        var informedEntity = new GtfsRt.InformedEntity();

        informedEntity.set('stopId', 'STOP_123');
        informedEntity.set('routeId', 'ROUTE_123');

        var informedEntities = [];
        informedEntities.push(informedEntity);

        alertObj.set('informedEntities', informedEntities);


        alertObj.save();


        var alerts = new GtfsRt.Alerts();

    
        alerts.fetch({success: function(collection, response, options) {

          alert(collection.length);

        }});


        
     });

 });
