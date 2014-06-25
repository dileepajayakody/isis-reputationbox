/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.nic.isis.reputation.dom;

import java.util.ArrayList;		
import java.util.HashMap;	
import java.util.List;
import java.util.Map;

import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;
import javax.jdo.annotations.VersionStrategy;

import org.apache.isis.applib.annotation.ObjectType;
import org.apache.isis.applib.annotation.Programmatic;
import org.nic.isis.reputation.utils.EmailUtils;

/**
 * @author dileepa
 *
 */
@javax.jdo.annotations.PersistenceCapable(identityType=IdentityType.DATASTORE)
@javax.jdo.annotations.DatastoreIdentity(
        strategy=javax.jdo.annotations.IdGeneratorStrategy.IDENTITY,
         column="id")
@javax.jdo.annotations.Version(
        strategy=VersionStrategy.VERSION_NUMBER, 
        column="version")
@ObjectType("USERMAILBOX")
public class UserMailBox {
	
	@Persistent
	private String emailId;
	
	@javax.jdo.annotations.Column(allowsNull="false")
	
	
	public String getEmailId() {
		return emailId;
	}

	public void setEmailId(String userId) {
		this.emailId = userId;
	}

	@Persistent
	private String userFirstName;
	
	@javax.jdo.annotations.Column(allowsNull="true")
	public String getUserFirstName() {
		return userFirstName;
	}

	public void setUserFirstName(String userfname) {
		this.userFirstName = userfname;
	}
	
	@Persistent
	private String userLastName;
	
	@javax.jdo.annotations.Column(allowsNull="true")
	public String getUserLastName() {
		return userLastName;
	}

	public void setUserLastName(String userName) {
		this.userLastName = userName;
	}

	@Persistent
	private int emailCount = 0;
	
	public int getEmailCount() {
		return emailCount;
	}

	public void setEmailCount(int emailCount) {
		this.emailCount = emailCount;
	}
	
	@Persistent
	private int termCount = 0;
	
	public int getTermCount() {
		return termCount;
	}

	public void setTermCount(int termCount) {
		this.termCount = termCount;
	}
	
	@Persistent
	private int lastIndexTimestamp = 0;
	
	public int getLastIndexTimestamp() {
		return lastIndexTimestamp;
	}

	public void setLastIndexTimestamp(int lastIndexTimestamp) {
		this.lastIndexTimestamp = lastIndexTimestamp;
	}
	
	@Persistent
	private List<Email> allEmails = new ArrayList<Email>();
		
	@Programmatic
	@javax.jdo.annotations.Column(allowsNull="true")
	public List<Email> getAllEmails() {
		return allEmails;
	}

	@Programmatic
	public void setAllEmails(List<Email> allEmails) {
		this.allEmails = allEmails;
	}
	
	@Persistent
	private List<EmailContact> allEmailContacts = new ArrayList<EmailContact>() ;
	
	@javax.jdo.annotations.Column(allowsNull="true")
	public List<EmailContact> getAllEmailContacts() {
		return allEmailContacts;
	}

	public void setAllEmailContacts(List<EmailContact> allEmailContacts) {
		this.allEmailContacts = allEmailContacts;
	}
	
	public void addEmail(Email email){
		this.allEmails.add(email);
		emailCount++;
	}
		
}
