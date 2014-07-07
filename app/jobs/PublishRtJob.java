package jobs;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import org.apache.commons.codec.binary.Hex;
import org.codehaus.jackson.map.ObjectMapper;

import models.Agency;
import models.Alert;
import models.InformedEntity;
import models.TimeRange;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.util.Md5Utils;
import com.google.transit.realtime.GtfsRealtime;

import play.Logger;
import play.Play;
import play.jobs.Job;
import play.jobs.Every;
import play.templates.Template;
import play.templates.TemplateLoader;

@Every("10s")
public class PublishRtJob extends Job {

	static Boolean publishSucessful = true;
	
	static String fullHtmlHash = "";
	static HashMap<String,String> agencyHashes = new HashMap<String, String>();
		
	public void doJob() throws UnsupportedEncodingException {

		String timeZoneStr = "America/Mexico_City";
		
		// create time zone object 
		TimeZone tzone = TimeZone.getTimeZone(timeZoneStr);
	   
		SimpleDateFormat formatNow = 
		    new SimpleDateFormat("EEEE d 'de' MMMM 'de' yyyy", new Locale("es", "ES"));
		
		SimpleDateFormat formatLastUpdate = 
			    new SimpleDateFormat("dd/MM/yy HH:mm", new Locale("es", "ES"));
		
		formatLastUpdate.setTimeZone(tzone);
		formatNow.setTimeZone(tzone);
		String now = formatNow.format(new Date());
		
		now = now.substring(0, 1).toUpperCase() + now.substring(1);
				
		AmazonS3 conn = null;
		
		if(Play.configuration.getProperty("aws.key") != null && Play.configuration.getProperty("aws.secret") != null) {
			AWSCredentials credentials = new BasicAWSCredentials(Play.configuration.getProperty("aws.key"), Play.configuration.getProperty("aws.secret"));
			conn = new AmazonS3Client(credentials);
			conn.setEndpoint("s3.amazonaws.com");
		}
		
		Map<String, Object> args = new HashMap<String, Object>();
		
		try {
			
			// generate full html template 
			
			List<Alert> alerts = Alert.findActiveAlerts(null, true);
			args.put("alerts", alerts);
			args.put("now", now);
			publishPb(alerts, conn, "setravi");
			
			for(Alert a : alerts) {
				a.setLastUpdatedString(formatLastUpdate);
			}
			
			String newHash = publishHtml("pub/gtfs-rt.html", args, fullHtmlHash, conn, "setravi", "gtfs-rt.html"); 
			
			if(!newHash.equals(fullHtmlHash)) {
				synchronized(fullHtmlHash) {
					fullHtmlHash = newHash;
				}
			}
		
			List<Agency> agencies = Agency.findAll();
			
			for(Agency agency : agencies) {
				
				String agencyId = agency.gtfsAgencyId;
				
				synchronized(agencyHashes) {
					if(!agencyHashes.containsKey(agencyId)) {
						agencyHashes.put(agencyId, "");
					}	
				}
				
				List<Alert> agencyAlerts = Alert.findActiveAlerts(agencyId, true);
				args.put("alerts", agencyAlerts);
				args.put("now", now);
								
				for(Alert a : agencyAlerts) {
					a.setLastUpdatedString(formatLastUpdate);
				}
				
				String newAgencyHash = publishHtml("pub/gtfs-rt.html", args, agencyHashes.get(agencyId), conn, "setravi", "gtfs-rt-" + agencyId + ".html"); 
				
				if(!newHash.equals(agencyHashes)) {
					synchronized(agencyHashes) {
						agencyHashes.put(agencyId, newAgencyHash);
					}
				}
					
			}
				
			synchronized(publishSucessful) {
				publishSucessful = true;
			}
		
		}
		catch(Exception e) {
			
			Logger.error(e.toString());
			
			synchronized(publishSucessful) {
				publishSucessful = false;
			}
			
			synchronized(fullHtmlHash) {
				fullHtmlHash = "";	
			}
			
			synchronized(agencyHashes) {
				agencyHashes.clear();
			}
			
		}
	}
	
	private void publishPb(List<Alert> alerts, AmazonS3 conn, String remoteBucket) {
		
		FileInputStream stream;
		String currentHash = "";
		try {
			stream = new FileInputStream(new File("public/feeds/gtfs-rt.pb"));
			currentHash = Hex.encodeHexString(Md5Utils.computeMD5Hash(stream));
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		

		GtfsRealtime.FeedMessage.Builder feedBuilder = GtfsRealtime.FeedMessage.newBuilder();
		
		feedBuilder.getHeaderBuilder().setGtfsRealtimeVersion("1.0").setTimestamp(new Date().getTime());		
		
		for(Alert alert : alerts) {
			
			GtfsRealtime.FeedEntity.Builder entity = feedBuilder.addEntityBuilder();
			
			entity.setId(alert.id.toString());
			
			GtfsRealtime.Alert.Builder alertBuilder = entity.getAlertBuilder();
				
			if(alert.headerText != null)
				alertBuilder.getHeaderTextBuilder().addTranslationBuilder().setLanguage("es").setText(alert.headerText);
			
			if(alert.descriptionText != null)
				alertBuilder.getDescriptionTextBuilder().addTranslationBuilder().setLanguage("es").setText(alert.descriptionText);	
			
			for(TimeRange range : alert.timeRanges) {
				
				GtfsRealtime.TimeRange.Builder period = GtfsRealtime.TimeRange.newBuilder();
				
				if(range.startTime != null)
					period.setStart(range.startTime.getTime());
				
				if(range.endTime != null)
					period.setStart(range.endTime.getTime());
				
				alertBuilder.addActivePeriod(period);
			}
			
			for(InformedEntity ie : alert.informedEntities) {
				GtfsRealtime.EntitySelector.Builder entitySelector = GtfsRealtime.EntitySelector.newBuilder();
				
				entitySelector.setAgencyId(alert.agencyId);
				
				if(ie.routeId != null)
					entitySelector.setRouteId(ie.routeId);
				
				if(ie.stopId != null)
					entitySelector.setStopId(ie.stopId);
				
				alertBuilder.addInformedEntity(entitySelector);
			}
			entity.setAlert(alertBuilder);
			feedBuilder.addEntity(entity);
		}
		
		FileOutputStream fos;
		try {
			fos = new FileOutputStream(new File("public/feeds/gtfs-rt.pb"));
			GtfsRealtime.FeedMessage feed = feedBuilder.build();
			
			feed.writeTo(fos);
			
			fos.close();
			
			ObjectMapper mapper = new ObjectMapper();
			JsonHeader header = new JsonHeader();
			header.alerts = alerts;
			mapper.writeValue(new File("public/feeds/gtfs-rt.json"), header);
			
			String newHash = "";
			
			try {
				stream = new FileInputStream(new File("public/feeds/gtfs-rt.pb"));
				newHash = Hex.encodeHexString(Md5Utils.computeMD5Hash(stream));
				stream.close();
				
				if(!newHash.isEmpty() && !newHash.equals(currentHash)) {
					
					File jsonFile = new File("public/feeds/gtfs-rt.json");
					File pbFile = new File("public/feeds/gtfs-rt.pb");
					
					FileInputStream pbStream = new FileInputStream(pbFile);
					FileInputStream jsonStream = new FileInputStream(jsonFile);
					
					ObjectMetadata protoBuff = new ObjectMetadata();
					protoBuff.setContentLength(pbFile.length());
					protoBuff.setContentType("application/x-protobuf");
					
					ObjectMetadata json = new ObjectMetadata();
					json.setContentLength(jsonFile.length());
					json.setContentType("text/json");
					
					if(conn != null) {
						conn.putObject(remoteBucket, "gtfs-rt.pb", pbStream, protoBuff);
						conn.setObjectAcl(remoteBucket, "gtfs-rt.pb", CannedAccessControlList.PublicRead);
						
						conn.putObject(remoteBucket, "gtfs-rt.json", jsonStream, json);
						conn.setObjectAcl(remoteBucket, "gtfs-rt.json", CannedAccessControlList.PublicRead);
					}
					
					pbStream.close();
					jsonStream.close();
				}
				

				
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
			
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	
		
		
	}
	
	private String publishHtml(String templatePath, Map<String, Object> args, String previousHash,  AmazonS3 conn, String remoteBucket, String remoteFile) throws IOException, NoSuchAlgorithmException {
		
		Template template = TemplateLoader.load(templatePath);
		String fullHtml = template.render(args);
		InputStream stream = new ByteArrayInputStream(fullHtml.getBytes("UTF-8"));
		
		String currentHash = Hex.encodeHexString(Md5Utils.computeMD5Hash(stream));

		if(!previousHash.equals(currentHash)) {
			
			stream.reset();
			
			File file = new File("public/feeds/", remoteFile);
			FileOutputStream fileOutputStream = new FileOutputStream(file);
			
			int read = 0;
			byte[] bytes = new byte[1024];
	 
			while ((read = stream.read(bytes)) != -1) {
				fileOutputStream.write(bytes, 0, read);
			}

			stream.reset();
			
			ObjectMetadata html = new ObjectMetadata();
			html.setContentLength(file.length());
			html.setContentType("text/html");
			
			if(conn != null) {
				conn.putObject(remoteBucket, remoteFile, stream, html);
				conn.setObjectAcl(remoteBucket, remoteFile, CannedAccessControlList.PublicRead);
			}
			
			fileOutputStream.close();
		}
		
		return currentHash;
	}
	
	public class JsonHeader {
		public List<Alert> alerts = null;
		public Date timestamp = new Date();
	}
	
}
