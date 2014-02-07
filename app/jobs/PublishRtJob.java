package jobs;

import java.io.ByteArrayInputStream;
import java.io.File;
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

import models.Agency;
import models.Alert;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.util.Md5Utils;

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

		// create time zone object 
		TimeZone tzone = TimeZone.getTimeZone("America/Mexico_City");
	      
	    // set time zone to default
	    tzone.setDefault(tzone);
		
		SimpleDateFormat formatNow = 
		    new SimpleDateFormat("EEEE d 'de' MMMM 'de' yyyy", new Locale("es", "ES"));
		String now = formatNow.format(new Date());
				
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
			html.setContentType("text/html");
			
			if(conn != null) {
				conn.putObject(remoteBucket, remoteFile, stream, html);
				conn.setObjectAcl(remoteBucket, remoteFile, CannedAccessControlList.PublicRead);
			}
		}
		
		return currentHash;
	}
	
}
