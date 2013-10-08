package controllers;

import play.*;
import play.db.jpa.JPA;
import play.mvc.*;
import utils.SerializedAccount;

import java.awt.Color;
import java.io.File;
import java.math.BigInteger;
import java.util.*;

import javax.persistence.Query;

import org.apache.commons.lang.StringUtils;

import models.*;

@With(Secure.class)
public class Admin extends Controller {
	
	@Before
    static void setConnectedUser() throws Throwable {
		
		 Security.setupSession(true);
		 
		 Account account = Account.find("username = ?", Security.connected()).first();
		 
		 if(!account.isAdmin() && !account.isAgencyAdmin())
			 Application.index();
    }
	
	public static void index() {
		
	}
	
	public static void accounts() {
		
		Account account = Account.find("username = ?", Security.connected()).first();
		
		List<Account> accounts = null;
		List<Agency> agencies = new ArrayList<Agency>();
		
		if(account.isAdmin()) {
			accounts = Account.find("order by username").fetch();
			agencies = Agency.findAll();
		}
		else if(account.isAgencyAdmin()) {
			accounts = Account.find("agency = ? order by username", account.agency).fetch();
			agencies.add(account.agency);
		}
		else {
			Application.index();
		}
	
		render(accounts, agencies);
	}
	
	public static void createAccount(String username, String password, String email, Boolean admin, Boolean taxi, Boolean citom, Long agencyId) {
		
		Account account = Account.find("username = ?", Security.connected()).first();
		
		// don't let agency admins create accoutns for other agencies
		
		if(account.isAgencyAdmin() && !account.agency.id.equals(agencyId))
			agencyId = account.agency.id;
		
		Agency agency = Agency.findById(agencyId);
		
		if(account.isAdmin() && (agencyId == null || agency == null))
			Admin.accounts();
		
		if(!username.isEmpty() && !password.isEmpty() && !email.isEmpty() && Account.find("username = ?", username).first() == null )
			new Account(username, password, email, admin, agency);
		
		Admin.accounts();
	}
	
	public static void updateAccount(String username, String password, String email, Boolean active, Boolean admin, Boolean taxi, Boolean citom, Long agencyId) {
		
		Account account = Account.find("username = ?", Security.connected()).first();
		
		if(account.isAgencyAdmin() && !account.agency.id.equals(agencyId))
			Admin.accounts();
		
		Agency agency = Agency.findById(agencyId);
		
		Account.update(username, password, email, active, admin, agency);
		
		Admin.accounts();

	}
	
	public static void getAccount(String username) {
		
		Account account = Account.find("username = ?", username).first();
		
		Account user = Account.find("username = ?", Security.connected()).first();
		
		if(account == null)
			badRequest();
		if(user.isAgencyAdmin() && !user.agency.id.equals(account.agency.id))
			badRequest();
		
		SerializedAccount serializedAccount = new SerializedAccount(account);
		
		renderJSON(serializedAccount);
	}
	
	
	public static void checkUsername(String username) {
		
		if(Account.find("username = ?", username).first() != null)
			badRequest();
		else
			ok();
	
	}
	
	public static void changePassword(String currentPassword, String newPassword) {
		Account.changePassword(Security.connected(), currentPassword, newPassword);
	}
	
	public static void resetPassword(String username, String newPassword) {
		Account.resetPassword(username, newPassword);
	}
	
	

}