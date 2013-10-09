package utils;

import java.util.Date;

import org.onebusaway.gtfs.model.Route;

import models.Account;
import models.Agency;

public class SerializedRoute implements Comparable<SerializedRoute> {
	
	public String id;
	
	public String longName;
    public String shortName;
    public String polyline;

    
    public SerializedRoute(Route r) {
    	
    	longName = r.getLongName();
    	shortName = r.getShortName();
    	id = r.getId().getId();
    	
    }

    public void setPolyline(String polyline) {
    	this.polyline = polyline;
    }
    
	@Override
	public int compareTo(SerializedRoute o) {
		return shortName.compareTo(o.shortName);
	}
    
}
