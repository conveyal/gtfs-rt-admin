package models;

import java.security.MessageDigest;
import java.util.Date;
import java.util.UUID;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;

import org.apache.commons.codec.binary.Hex;
import org.codehaus.jackson.annotate.JsonBackReference;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.hsqldb.lib.MD5;

import play.Play;
import play.db.jpa.Model;

@JsonIgnoreProperties({"entityId", "persistent"})
@Entity
public class InformedEntity extends Model {

	/*
	agency_id	string			optional	
	route_id	string			optional	
	route_type	int32			optional	
	trip		TripDescriptor	optional	
	stop_id		string			optional
	*/
	
	@JsonBackReference
	@ManyToOne
    public Alert alert;
	
	public String agencyId;
	public String routeId;
	public String stopId;

	// trip descriptor TBD !!! 
	
    @JsonCreator
    public static InformedEntity factory(long id) {
      return InformedEntity.findById(id);
    }

    @JsonCreator
    public static InformedEntity factory(String id) {
      return InformedEntity.findById(Long.parseLong(id));
    }

}
