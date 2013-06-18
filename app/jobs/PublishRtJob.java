package jobs;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import models.Alert;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;

import play.Logger;
import play.Play;
import play.jobs.Job;
import play.jobs.Every;
import play.templates.Template;
import play.templates.TemplateLoader;

@Every("10s")
public class PublishRtJob extends Job {
	
	private static Object publishPending = new Object();
	
	public void doJob() throws UnsupportedEncodingException {
		
		synchronized (publishPending) {
			
			List<Alert> alerts = Alert.findActiveAlerts();
			
			AWSCredentials credentials = new BasicAWSCredentials(Play.configuration.getProperty("aws.key"), Play.configuration.getProperty("aws.secret"));
			AmazonS3 conn = new AmazonS3Client(credentials);
			conn.setEndpoint("s3.amazonaws.com");
			
			Map<String, Object> args = new HashMap<String, Object>();
		
			args.put("alerts", alerts);
			args.put("lastUpdate", (new Date()).toString());
			
			Template template = TemplateLoader.load("app/views/gtfs-rt.html");
			String s = template.render(args);
			InputStream stream = new ByteArrayInputStream(s.getBytes("UTF-8"));
	
			ObjectMetadata html = new ObjectMetadata();
			html.setContentType("text/html");
			
			conn.putObject("setravi", "gtfs-rt.html", stream, html);
			conn.setObjectAcl("setravi", "gtfs-rt.html", CannedAccessControlList.PublicRead);
		}
		
	}
}
