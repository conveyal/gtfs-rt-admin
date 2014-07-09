package models;

import java.security.MessageDigest;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.UUID;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Transient;

import org.apache.commons.codec.binary.Hex;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonManagedReference;
import org.hsqldb.lib.MD5;

import play.Play;
import play.db.jpa.Model;

@JsonIgnoreProperties({"entityId", "persistent", "pos"})
@Entity
public class Alert extends Model implements Comparable {

	/*
	active_period		TimeRange			repeated	 Time when the alert should be shown to the user. If missing, the alert will be shown as long as it appears in the feed. If multiple ranges are given, the alert will be shown during all of them.
	informed_entity		EntitySelector		repeated	 Entities whose users we should notify of this alert.
	cause				Cause				optional	
	effect				Effect				optional	
	url					TranslatedString	optional	 The URL which provides additional information about the alert.
	header_text			TranslatedString	optional	 Header for the alert. This plain-text string will be highlighted, for example in boldface.
	description_text	TranslatedString	optional	 Description for the alert. This plain-text string will be formatted as the body of the alert (or shown on an explicit "expand" request by the user). The information in the description should add to the information of the header.
	*/
	
	@JsonManagedReference
	 @OneToMany(cascade = CascadeType.ALL, mappedBy = "alert", orphanRemoval=true)
    public List<TimeRange> timeRanges;
	
	@JsonManagedReference
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "alert", orphanRemoval=true)
    public List<InformedEntity> informedEntities;
	
	public String agencyId;
	
	public String cause;
	
	public String effect;
	
    public String url;
    
	public String headerText;
    
    @Column(length = 8000,columnDefinition="TEXT")
    public String descriptionText;
    
    @Column(length = 8000,columnDefinition="TEXT")
    public String commentsText;
    
    public Date created;
    
    public Date lastUpdated;
    
    public Boolean publiclyVisible;
    
    public Boolean deleted;
    
    @Transient
    public transient String lastUpdatedStr;
    
    @JsonCreator
    public static Alert factory(long id) {
      return Alert.findById(id);
    }

    @JsonCreator
    public static Alert factory(String id) {
      return Alert.findById(Long.parseLong(id));
    }
    
    public List<InformedEntity> affectedEntities() {
    	
    	List<InformedEntity> entities = InformedEntity.find("alert = ?", this).fetch();
    	return entities;
   
    }
    
    public void setLastUpdatedString(DateFormat df) {
    	
    	lastUpdatedStr = df.format(lastUpdated);
    	
    }
    
    static public List<Alert> findActiveAlerts(String agencyId, Boolean publiclyVisible) {
    	
    	List<TimeRange> timeRanges = TimeRange.find("(startTime < now() or startTime is null) and (endTime > now() or endTime is null) order by startTime, endTime").fetch();
    	
    	HashMap<Long, Alert> alerts = new HashMap<Long, Alert>();
    	
    	for(TimeRange tr : timeRanges){
    		
    		if(!alerts.containsKey(tr.alert.id) && (agencyId == null || tr.alert.agencyId.equals(agencyId))) {
    			
    			if(publiclyVisible == null || publiclyVisible == false || publiclyVisible == tr.alert.publiclyVisible)
    				alerts.put(tr.alert.id, tr.alert);
    		}
    	}
    	
    	List<Alert> alertsWithoutRanges = Alert.findAll();
    	
    	for(Alert a : alertsWithoutRanges){
    		
    		if((agencyId == null || a.agencyId.equals(agencyId)) && a.timeRanges.isEmpty() && !alerts.containsKey(a.id)) {
    			
    			if(publiclyVisible == null || publiclyVisible == false || publiclyVisible == a.publiclyVisible)
    				alerts.put(a.id, a);
    		}
    	}
 
    	ArrayList alertList = new ArrayList<Alert>(alerts.values());
    	
    	Collections.sort(alertList);
    	
    	return alertList;
    }
    
    static public List<Alert> findFutureAlerts(String agencyId) {
    	
    	List<TimeRange> timeRanges = TimeRange.find("startTime > now() order by startTime, endTime").fetch();
    	
    	HashMap<Long, Alert> alerts = new HashMap<Long, Alert>();
    	
    	for(TimeRange tr : timeRanges){
    		
    		if(!alerts.containsKey(tr.alert.id) && (agencyId == null || tr.alert.agencyId.equals(agencyId))) {
    			alerts.put(tr.alert.id, tr.alert);
    		}
    		
    	}
    	
    	List<Alert> alertsWithoutRanges = Alert.findAll();
    	
    	for(Alert a : alertsWithoutRanges){
    		
    		if((agencyId == null || a.agencyId.equals(agencyId)) && a.timeRanges.isEmpty() && !alerts.containsKey(a.id)) {
    			alerts.put(a.id, a);
    		}
    		
    	}
 
    	ArrayList alertList = new ArrayList<Alert>(alerts.values());
    	
    	Collections.sort(alertList);
    	
    	return alertList;
    }
    
    public Alert delete() {

    	this.informedEntities = new ArrayList<InformedEntity>();
    	this.timeRanges = new ArrayList<TimeRange>();
    	this.save();

        InformedEntity.delete("alert = ?", this);
        TimeRange.delete("alert = ?", this);

        return super.delete();
    }

    public Boolean securityCheck(String agencyId) {
    	
    	if(agencyId == null || agencyId.isEmpty() || !agencyId.equals(agencyId))
         	return false;
    	
    	return true;
    }

	@Override
	public int compareTo(Object o) {
		
		if(this.lastUpdated != null && ((Alert)o).lastUpdated != null)
			return -(this.lastUpdated.compareTo(((Alert)o).lastUpdated));
		else
			return 0;
	}
    

}
