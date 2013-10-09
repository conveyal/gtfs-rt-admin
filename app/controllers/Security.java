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
 
    static void setupSession(Boolean allowAdmins) throws Throwable {
    	
	    if(Security.isConnected()) {
	    	renderArgs.put("user", Security.connected());
	    	
	    	Account account = Account.find("username = ?", Security.connected()).first();
	    	
	    	if(account.isAdmin() || account.isAgencyAdmin()) {	
	    		renderArgs.put("admin", true);
	    	}
	    	
	    	if(account.isAdmin() && !allowAdmins) {
	    		
	    		// for admin accounts -- redirect to admin index page if not allowed
	    		
	    		Admin.accounts();
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