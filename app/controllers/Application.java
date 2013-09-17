package controllers;

import play.*;
import play.mvc.*;

import java.util.*;

import models.*;


@With(Secure.class)
public class Application extends Controller {

	@Before
	static void initSession() throws Throwable {

    List<Agency> agencies = new ArrayList<Agency>();
    	
	    if(Security.isConnected()) {
	    	renderArgs.put("user", Security.connected());
	    	
	    	Account account = Account.find("username = ?", Security.connected()).first();
	            
	        renderArgs.put("agencyId", account.agencyId);
        }
        else {
        	
        	if(Account.count() == 0) {
        		Account account = new Account("admin", "admin", "admin@test.com", true, null);
	        	account.save();
	        }
        	
        	Secure.login();
        }
    }
	

    public static void createAccount(String username, String password, String email, Boolean admin, Long agencyId)
	{
		if(!username.isEmpty() && !password.isEmpty() && !email.isEmpty() && Account.find("username = ?", username).first() == null )
			new Account(username, password, email, admin, agencyId);
		
		Application.index();
	}
	
    public static void index() {
        render();
    }

}