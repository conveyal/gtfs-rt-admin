var alerts12;
$(document).ready(function() {
   // page init scripts

   // add language select handler
     $("#runDemo").click(function(e){


        // create backbone model Alert
        var alertObj = new GtfsRt.Alert();

        // set a property 
        alertObj.set('cause', 'TECHNICAL_PROBLEM');

        var now  = (new Date()).getTime();
      

        // create a backbone TimeRange object
        var timeRange = new GtfsRt.TimeRange();

        // set properties
        timeRange.set('startTime', now);
        timeRange.set('endTime', now);


        // create (or add to existing) array of time ranges
        var timeRanges = [];
        timeRanges.push(timeRange);

        // add the array to the alert object
        alertObj.set('timeRanges', timeRanges);


        // repete for informed entities    
        var informedEntity = new GtfsRt.InformedEntity();

        informedEntity.set('stopId', 'STOP_123');
        informedEntity.set('routeId', 'ROUTE_123');

        var informedEntities = [];
        informedEntities.push(informedEntity);

        alertObj.set('informedEntities', informedEntities);


        // save alert object -- syncs with the sever, including nested timernage and informedentities
        alertObj.save();


        // create a new ALerts collection
        var alerts = new GtfsRt.Alerts();

        // fetch data from server
        alerts.fetch({success: function(collection, response, options) {

          console.log(collection.length);
          
          var alert = collection.at(0);
          
          alert.set('descriptionText', 'detour');
          alert.save(); // fails -- 404 on PUT request 
          
          alert.destroy(); // fails -- 404 on DELETE

        }});


        
     });

 });
