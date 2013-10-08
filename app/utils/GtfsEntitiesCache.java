package utils;

import java.io.File;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import models.Account;
import models.Agency;

import org.onebusaway.gtfs.impl.GtfsDaoImpl;
import org.onebusaway.gtfs.serialization.GtfsReader;

import play.Logger;
import play.Play;

public class GtfsEntitiesCache {
	
	public Map<String, String> agencyMap = new HashMap<String, String>();
	public Map<String, String> routeMap = new HashMap<String, String>();
	public Map<String, String> stopMap = new HashMap<String, String>();
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
        		String routeName = gtfsRoute.getShortName() + " " + gtfsRoute.getLongName();
        		
        		routeMap.put(routeId, routeName);
        		
        		if(agencyRouteMap.get(agencyId) == null)
        			agencyRouteMap.put(agencyId, new ArrayList<String>());
        		
        		agencyRouteMap.get(agencyId).add(routeId);	
        			
        	}
        	
        	for (org.onebusaway.gtfs.model.StopTime stopTime : store.getAllStopTimes()) {
        		String stopId = stopTime.getStop().getId().getId();
        		String stopName = stopTime.getStop().getName();
        		
        		String rotueId = stopTime.getTrip().getRoute().getId().getId();
        		
        		if(routeStopMap.get(rotueId) == null)
        			routeStopMap.put(rotueId, new ArrayList<String>());
        		else if(routeStopMap.get(rotueId).contains(stopId)){
        			continue;
        		}
        		
        		routeStopMap.get(rotueId).add(stopId);
        		
        		stopMap.put(stopId, stopName);
        	}
              	
    	}
    	catch (Exception e) {
    		
        	Logger.error(e.toString()); 
        	
    	}
	}

}
