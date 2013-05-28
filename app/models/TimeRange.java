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
public class TimeRange extends Model {

	@JsonBackReference
	@ManyToOne
    public Alert alert;

	public Date startTime;
	public Date endTime;
	
	@JsonCreator
    public static TimeRange factory(long id) {
      return TimeRange.findById(id);
    }

    @JsonCreator
    public static TimeRange factory(String id) {
      return TimeRange.findById(Long.parseLong(id));
    }

}
