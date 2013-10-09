package utils;

import java.io.File;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import models.Account;
import models.Agency;

import org.apache.commons.lang.StringUtils;
import org.onebusaway.gtfs.impl.GtfsDaoImpl;
import org.onebusaway.gtfs.serialization.GtfsReader;
import org.opentripplanner.util.PolylineEncoder;
import org.opentripplanner.util.model.EncodedPolylineBean;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

import play.Logger;
import play.Play;

public class GtfsEntitiesCache {
	
	public Map<String, String> agencyMap = new HashMap<String, String>();
	public Map<String, SerializedRoute> routeMap = new HashMap<String, SerializedRoute>();
	public Map<String, SerializedStop> stopMap = new HashMap<String, SerializedStop>();
	public Map<String, List<String>> agencyRouteMap = new HashMap<String, List<String>>();
	public Map<String, List<String>> routeStopMap = new HashMap<String, List<String>>();
	
	public GtfsEntitiesCache() {
		load();
	}
	
	public void load() {
		
		GtfsReader reader = new GtfsReader();
    	GtfsDaoImpl store = new GtfsDaoImpl();
    	
    	try {
    		
    		File gtfsFile = new File(Play.configuration.getProperty("application.gtfsFile"));
    		
    		reader.setInputLocation(gtfsFile);
        	reader.setEntityStore(store);
        	reader.run();
        		
        	for (org.onebusaway.gtfs.model.Agency gtfsAgency : store.getAllAgencies()) {
        		
        		
        		agencyMap.put(gtfsAgency.getId(), gtfsAgency.getName());
        		
        		// add agencies to db
        		
        		Agency agency = Agency.find("gtfsAgencyId = ?", gtfsAgency.getId()).first();
        		
        		if(agency == null) {
        			
        			agency = new Agency();
        			
        			agency.gtfsAgencyId = gtfsAgency.getId();
        			agency.name = gtfsAgency.getName();
        			
        			agency.save();
        			
        			Account account = new Account(gtfsAgency.getId().toLowerCase(), "admin", "admin@test.com", true, agency);
    	        	account.save();
    	        	
    	        	Logger.info("creating account for: " + gtfsAgency.getId() + " -- " + gtfsAgency.getName());	
        		}
	    	}
        	
        	for (org.onebusaway.gtfs.model.Route gtfsRoute : store.getAllRoutes()) {
        		
        		String routeId = gtfsRoute.getId().getId();
        		String agencyId = gtfsRoute.getAgency().getId();
        		
        		routeMap.put(routeId, new SerializedRoute(gtfsRoute));
        		
        		if(agencyRouteMap.get(agencyId) == null)
        			agencyRouteMap.put(agencyId, new ArrayList<String>());
        		
        		agencyRouteMap.get(agencyId).add(routeId);	
        			
        	}
        	
        	HashMap<String,Integer> shapeLengthMap = new HashMap<String,Integer>();
        	HashMap<String,String> shapeRouteMap = new HashMap<String,String>();
        	
        	for (org.onebusaway.gtfs.model.StopTime stopTime : store.getAllStopTimes()) {
        		
        		String stopId = stopTime.getStop().getId().getId();
        		String routeId = stopTime.getTrip().getRoute().getId().getId();
        		String shapeId = stopTime.getTrip().getShapeId().toString();
        		
        		if(!shapeRouteMap.containsKey(shapeId)) {
        			shapeRouteMap.put(shapeId, routeId);
        			shapeLengthMap.put(shapeId, 0);
        		}
        		
        		shapeLengthMap.put(shapeId, (shapeLengthMap.get(shapeId) + 1));
        			
        		if(routeStopMap.get(routeId) == null)
        			routeStopMap.put(routeId, new ArrayList<String>());
        		else if(routeStopMap.get(routeId).contains(stopId)){
        			continue;
        		}
        		
        		routeStopMap.get(routeId).add(stopId);
        		
        		
        		
        		stopMap.put(stopId, new SerializedStop(stopTime.getStop()));
        	}
        	
        	Logger.info("loading shapes...");
        	
        	// find longest trips for each route
        	
        	HashMap<String,String> longestShapeMap = new HashMap<String,String>();
        	
        	for(String shapeId : shapeRouteMap.keySet() ) {
        		
        		String routeId = shapeRouteMap.get(shapeId);
        		
        		if(!longestShapeMap.containsKey(routeId)) {
        			longestShapeMap.put(routeId, shapeId);
        		}
        		else {
        			String currentLongestTrip = longestShapeMap.get(routeId);
        			if(shapeLengthMap.get(shapeId) > shapeLengthMap.get(currentLongestTrip)) {
        				longestShapeMap.put(routeId, shapeId);
        			}
        		}
        	}
        	
        	   
	        // sort/load points 
        	
        	Integer pointCount = 0;

        	Map<String, List<org.onebusaway.gtfs.model.ShapePoint>> shapePointIdMap = new HashMap<String, List<org.onebusaway.gtfs.model.ShapePoint>>();
        	
        	for (org.onebusaway.gtfs.model.ShapePoint shapePoint : store.getAllShapePoints()) {
    	        
	        	List<org.onebusaway.gtfs.model.ShapePoint> shapePoints  = shapePointIdMap.get(shapePoint.getShapeId().toString());
	        
	        	pointCount++;
	        	
	        	if(shapePoints  != null)
	        	{
	        		shapePoints.add(shapePoint);
	        	}
	        	else
	        	{
	        		shapePoints = new ArrayList<org.onebusaway.gtfs.model.ShapePoint>();
	        		shapePoints.add(shapePoint);
	        	
	        		shapePointIdMap.put(shapePoint.getShapeId().toString(), shapePoints);
	        	}
	        }
        	
        	Logger.info(pointCount + " points loaded.");
        	Logger.info(shapePointIdMap.keySet().size() + " shapes loaded.");
        
	        // build polyline
	     
        	GeometryFactory geometryFactory = new GeometryFactory();
        	
        	HashMap<String,String> shapePolylineMap = new HashMap<String,String>();
	        
	        for(String shapeId : shapePointIdMap.keySet())
	        {
	        	
	        	List<org.onebusaway.gtfs.model.ShapePoint> shapePoints  = shapePointIdMap.get(shapeId);
	        	
	        	Collections.sort(shapePoints);
	        	
	        	Double describedDistance = new Double(0);
	        	List<Coordinate> points = new ArrayList<Coordinate>();
	        	
	        	for(org.onebusaway.gtfs.model.ShapePoint shapePoint : shapePoints)
	        	{
	        		describedDistance += shapePoint.getDistTraveled();
	      
	        		points.add(new Coordinate(shapePoint.getLon(), shapePoint.getLat()));
	        		
	        	}
	        	
	        	Coordinate[] cs = new Coordinate[0];
	        	Geometry geom = geometryFactory.createLineString(points.toArray(cs));
	        	EncodedPolylineBean polylineBean =  PolylineEncoder.createEncodings(geom);
	        	
	        	shapePolylineMap.put(shapeId, polylineBean.getPoints());
	        	
	        }
	        
	        Integer routeCount = 0;
	        Integer matchingCount = 0;
	        
	        for(String routeId : longestShapeMap.keySet()) {
        		
	        	routeCount++;
	        	
	        	if(longestShapeMap.containsKey(routeId)) {
	        	
	        		matchingCount++;
	        		
	        		SerializedRoute route = routeMap.get(routeId);
		        	route.setPolyline(shapePolylineMap.get(longestShapeMap.get(routeId)));
		        	
		        	routeMap.put(routeId, route);
	        	}
        	}
	        
	        Logger.info(routeCount + " routes loaded.");
	        Logger.info(matchingCount + " routes with shape.");
    	}
    	catch (Exception e) {
    		
        	Logger.error(e.toString()); 
        	
    	}
	}

}
