package utils;

import java.util.Date;

import org.onebusaway.gtfs.model.Stop;

import models.Account;
import models.Agency;

public class SerializedStop implements Comparable<SerializedStop> {
	
	public String id;
	
	public String name;
    public String desc;
    public Double lat;
    public Double lon;

    public SerializedStop(Stop s) {
    	
    	if(s.getName() != null)
    		name = s.getName();
    	else
    		name = "";
    	
    	if(s.getDesc() != null)
    		desc = s.getDesc();
    	else 
    		desc = "";
    	
    	lat = s.getLat();
    	lon = s.getLon();
    	
    	id = s.getId().getId();
    	
    }

	@Override
	public int compareTo(SerializedStop o) {
		return name.compareTo(o.name);
	}
    
}
