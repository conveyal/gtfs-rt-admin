package utils;

import java.util.Date;

import models.Account;
import models.Agency;

public class SerializedAccount {
	
	public Long id;
	
	public String username;
    public String email;
    public Date lastLogin;
    public Boolean active;       
    public Boolean admin;
    public Boolean agencyAdmin;
    public Agency agency;
    
    public SerializedAccount(Account a) {
    	
    	this.id = a.id;
    	this.username = a.username;
    	this.email = a.email;
    	this.lastLogin = a.lastLogin;
    	this.active = a.active;
    	this.agencyAdmin = a.agencyAdmin;
    	this.admin = a.admin;
    	this.agency = a.agency;
    }
    
}
