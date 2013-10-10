package controllers;
 
import models.*;
 
public class Security extends Secure.Security {
	
    static boolean authenticate(String username, String password) {
        return Account.connect(username, password);
    }
    
    static boolean check(String profile) {
        if("admin".equals(profile)) {
            return Account.find("username", connected()).<Account>first().isAdmin();
        }
   
        return false;
    }
 
    static void setupSession() throws Throwable {
    	
	    if(Security.isConnected()) {
	    	renderArgs.put("user", Security.connected());
	    	
	    	Account account = Account.find("username = ?", Security.connected()).first();
	    	
	    	if(account.isAdmin() || account.isAgencyAdmin()) {	
	    		renderArgs.put("admin", true);
	    	}
	    	
	    	if(account.isAdmin()) {
	    		if(session.get("selectedAgency") != null && !session.get("selectedAgency").isEmpty()) {
		    		renderArgs.put("agencyName", Application.entities.agencyMap.get(session.get("selectedAgency")));
			        renderArgs.put("agencyId", session.get("selectedAgency"));
	    		}
	    		
	    		renderArgs.put("availableAgencies", Agency.find("order by name").fetch());
	    	}
	    	else if(account.agency != null) {
	    		renderArgs.put("agencyName", Application.entities.agencyMap.get(account.agency.gtfsAgencyId));
		        renderArgs.put("agencyId", account.agency.gtfsAgencyId);
	    	} 
	    }
	    else {
	    	
	    	if(Account.count("admin = true") == 0) {
	    		Account account = new Account("admin", "admin", "admin@test.com", true, null);
	        	account.save();
	        }
	    	
	    	Secure.login();
	    }
	    
    }
    
    static Account getAccount()
    {
    	return Account.find("username", connected()).<Account>first();
    }
    
}