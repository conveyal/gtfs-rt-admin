package controllers;

import play.*;
import play.mvc.*;
import utils.IdValuePair;
import utils.SerializedRoute;
import utils.SerializedStop;

import java.io.IOException;
import java.io.StringWriter;
import java.util.*;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

import models.*;


@With(Secure.class)
public class Api extends Controller {
	
	@Before
	static void initSession() throws Throwable {
		
		Security.setupSession(false);
		
    }
	

    private static ObjectMapper mapper = new ObjectMapper();
    private static JsonFactory jf = new JsonFactory();

    private static String toJson(Object pojo, boolean prettyPrint)
            throws JsonMappingException, JsonGenerationException, IOException {
                StringWriter sw = new StringWriter();
                JsonGenerator jg = jf.createJsonGenerator(sw);
                if (prettyPrint) {
                    jg.useDefaultPrettyPrinter();
                }
                mapper.writeValue(jg, pojo);
                return sw.toString();
            }

    // **** alert controllers ****

    public static void getAlert(Long id) {
        try {
            if(id != null) {
                Alert alert = Alert.findById(id);
                if(alert != null)
                    renderJSON(Api.toJson(alert, false));
                else
                    notFound();
            }
            else {
                renderJSON(Api.toJson(Alert.all().fetch(), false));
            }
        } catch (Exception e) {
            e.printStackTrace();
            badRequest();
        }

    }

    public static void createAlert() {

    	Alert alert;
        
        String agencyId = renderArgs.get("agencyId").toString();

        try {
            alert = mapper.readValue(params.get("body"), Alert.class);
            
            // security check
            if(!alert.securityCheck((String)renderArgs.get("agencyId")))
            	badRequest();
            alert.agencyId = agencyId;
            alert.created = new Date();
            alert.save();

            renderJSON(Api.toJson(alert, false));
        } catch (Exception e) {
            e.printStackTrace();
            badRequest();
        }
    
    }

    public static void updateAlert() {
        Alert alert;

        try {
            alert = mapper.readValue(params.get("body"), Alert.class);

            // security check
            if(!alert.securityCheck((String)renderArgs.get("agencyId")))
            	badRequest();
            
            if(alert.id == null || Alert.findById(alert.id) == null)
                badRequest();

            Alert updatedAlert = Alert.em().merge(alert);
            updatedAlert.lastUpdated = new Date();
            updatedAlert.save();

            renderJSON(Api.toJson(updatedAlert, false));
        } catch (Exception e) {
            e.printStackTrace();
            badRequest();
        }
    }

    public static void deleteAlert(Long id) {
        if(id == null)
            badRequest();

        Alert alert = Alert.findById(id);

        if(alert == null)
            badRequest();
        
        // security check
        if(!alert.securityCheck((String)renderArgs.get("agencyId")))
        	badRequest();

        alert.lastUpdated = new Date();
        alert.deleted = true;
        alert.save();

        ok();
    }
    
    // **** InformedEntity controllers ****
    
    public static void getInformedEntity(Long id) {
        try {
            if(id != null) {
            	InformedEntity ie = Alert.findById(id);
                if(ie != null)
                    renderJSON(Api.toJson(ie, false));
                else
                    notFound();
            }
            else {
                renderJSON(Api.toJson(InformedEntity.all().fetch(), false));
            }
        } catch (Exception e) {
            e.printStackTrace();
            badRequest();
        }

    }

    public static void createInformedEntity() {
    	InformedEntity ie;

        try {
        	ie = mapper.readValue(params.get("body"), InformedEntity.class);
            ie.save();

            renderJSON(Api.toJson(ie, false));
        } catch (Exception e) {
            e.printStackTrace();
            badRequest();
        }
    }

    public static void updateInformedEntity() {
    	InformedEntity ie;

        try {
        	ie = mapper.readValue(params.get("body"), InformedEntity.class);

            if(ie.id == null || Alert.findById(ie.id) == null)
                badRequest();

            InformedEntity updatedInformedEntity = InformedEntity.em().merge(ie);
            updatedInformedEntity.save();

            renderJSON(Api.toJson(updatedInformedEntity, false));
        } catch (Exception e) {
            e.printStackTrace();
            badRequest();
        }
    }

    public static void deleteInformedEntity(Long id) {
        if(id == null)
            badRequest();

        InformedEntity ie = InformedEntity.findById(id);

        if(ie == null)
            badRequest();

        ie.delete();

        ok();
    }

    // **** TimeRange controllers ****

    public static void getTimeRange(Long id) {
        try {
            if(id != null) {
            	TimeRange tr = TimeRange.findById(id);
                if(tr != null)
                    renderJSON(Api.toJson(tr, false));
                else
                    notFound();
            }
            else {
                renderJSON(Api.toJson(TimeRange.all().fetch(), false));
            }
        } catch (Exception e) {
            e.printStackTrace();
            badRequest();
        }

    }

    public static void createTimeRange() {
    	TimeRange tr;

        try {
        	tr = mapper.readValue(params.get("body"), TimeRange.class);
        	tr.save();

            renderJSON(Api.toJson(tr, false));
        } catch (Exception e) {
            e.printStackTrace();
            badRequest();
        }
    }


    public static void updateTimeRange() {
    	TimeRange tr;

        try {
        	tr = mapper.readValue(params.get("body"), TimeRange.class);

            if(tr.id == null || TimeRange.findById(tr.id) == null)
                badRequest();

            TimeRange updatedTimeRange = TimeRange.em().merge(tr);
            updatedTimeRange.save();

            renderJSON(Api.toJson(updatedTimeRange, false));
        } catch (Exception e) {
            e.printStackTrace();
            badRequest();
        }
    }

    public static void deleteTimeRange(Long id) {
        if(id == null)
            badRequest();

        TimeRange tr = TimeRange.findById(id);

        if(tr == null)
            badRequest();

        tr.delete();

        ok();
    }
    
    
    public static void routes() throws JsonMappingException, JsonGenerationException, IOException {
    	String agencyId = renderArgs.get("agencyId").toString();
    	
    	ArrayList<IdValuePair> pairs = new ArrayList<IdValuePair>();
    	
    	List<String> routes = Application.entities.agencyRouteMap.get(agencyId);
       	for(String routeId : routes) {
       		IdValuePair<SerializedRoute> pair = new IdValuePair<SerializedRoute>(routeId, Application.entities.routeMap.get(routeId));
    		pairs.add(pair);
    	}
       	Collections.sort(pairs);
        renderJSON(toJson(pairs, false));
    }
    
    public static void stops(String routeId) throws JsonMappingException, JsonGenerationException, IOException {
    	
    	List<IdValuePair> pairs = new ArrayList<IdValuePair>();
    	
    	List<String> stops = Application.entities.routeStopMap.get(routeId);
       	
    	for(String stopId : stops) {
       		IdValuePair<SerializedStop> pair = new IdValuePair<SerializedStop>(stopId, Application.entities.stopMap.get(stopId));
    		pairs.add(pair);
    	}
       	
    	Collections.sort(pairs);
        renderJSON(toJson(pairs, false));
    }
    

}