package models;

import java.security.MessageDigest;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import org.apache.commons.codec.binary.Hex;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonManagedReference;
import org.hsqldb.lib.MD5;

import play.Play;
import play.db.jpa.Model;

@JsonIgnoreProperties({"entityId", "persistent"})
@Entity
public class Alert extends Model {

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
    @OneToMany(cascade = CascadeType.ALL)
    public List<TimeRange> timeRanges;
	
	@JsonManagedReference
    @OneToMany(cascade = CascadeType.ALL)
    public List<InformedEntity> informedEntities;
	
	public String cause;
	public String effect;
	
    public String url;
    public String headerText;
    public String descriptionText;
    
    @JsonCreator
    public static Alert factory(long id) {
      return Alert.findById(id);
    }

    @JsonCreator
    public static Alert factory(String id) {
      return Alert.findById(Long.parseLong(id));
    }


}
