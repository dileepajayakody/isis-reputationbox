/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.nic.isis.reputation.dom;

import java.util.ArrayList;
import java.util.List;

import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.VersionStrategy;

import org.apache.isis.applib.ViewModel;
import org.apache.isis.applib.annotation.ObjectType;
import org.apache.isis.applib.annotation.Programmatic;

/**
 * @author dileepa
 * 
 */
@javax.jdo.annotations.Queries({
		@javax.jdo.annotations.Query(name = "find_mailbox", language = "JDOQL", value = "SELECT FROM org.nic.isis.reputation.dom.UserMailBox WHERE accountId == :accountId") 
		})
@javax.jdo.annotations.PersistenceCapable(identityType = IdentityType.DATASTORE)
@javax.jdo.annotations.DatastoreIdentity(strategy = javax.jdo.annotations.IdGeneratorStrategy.IDENTITY, column = "id")
@javax.jdo.annotations.Version(strategy = VersionStrategy.VERSION_NUMBER, column = "version")
@ObjectType("USERMAILBOX")
public class UserMailBox {

	// contextio account id
	private String accountId;

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "false")
	public String getAccountId() {
		return accountId;
	}

	public void setAccountId(String accountId) {
		this.accountId = accountId;
	}

	// region > emailId (property)
	private String emailId;

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "false")
	public String getEmailId() {
		return emailId;
	}

	public void setEmailId(String userId) {
		this.emailId = userId;
	}

	// endregion

	// region > userFirstName (property)
	private String userFirstName;

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public String getUserFirstName() {
		return userFirstName;
	}

	public void setUserFirstName(String userfname) {
		this.userFirstName = userfname;
	}

	// endregion

	// region > userFirstName (property)
	private String userLastName;

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public String getUserLastName() {
		return userLastName;
	}

	public void setUserLastName(String userName) {
		this.userLastName = userName;
	}

	// endregion

	// region > emailCount (property)
	private int emailCount = 0;

	@javax.jdo.annotations.Persistent
	public int getEmailCount() {
		return emailCount;
	}

	public void setEmailCount(int emailCount) {
		this.emailCount = emailCount;
	}

	// endregion

	// region > termCount (property)
	private int termCount = 0;

	@javax.jdo.annotations.Persistent
	public int getTermCount() {
		return termCount;
	}

	public void setTermCount(int termCount) {
		this.termCount = termCount;
	}

	// endregion

	// region > lastIndexTimestamp (property)
	// last index timestamp of the context.io call
	private int lastIndexTimestamp = 0;

	@javax.jdo.annotations.Persistent
	public int getLastIndexTimestamp() {
		return lastIndexTimestamp;
	}

	public void setLastIndexTimestamp(int lastIndexTimestamp) {
		this.lastIndexTimestamp = lastIndexTimestamp;
	}

	// endregion

		
	// region > allEmails (programmatic), addEmail (action)
	private List<Email> allEmails = new ArrayList<Email>();

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "false")
	@Programmatic
	public List<Email> getAllEmails() {
		return allEmails;
	}

	@Programmatic
	public void setAllEmails(List<Email> allEmails) {
		this.allEmails = allEmails;
	}

	public void addEmail(Email email) {
		this.allEmails.add(email);
		emailCount++;
	}

	// endregion

	// region > allEmailContacts (collection)
	private List<EmailContact> allEmailContacts = new ArrayList<EmailContact>();

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public List<EmailContact> getAllEmailContacts() {
		return allEmailContacts;
	}

	public void setAllEmailContacts(List<EmailContact> allEmailContacts) {
		this.allEmailContacts = allEmailContacts;
	}

	// endregion

	private volatile boolean isSyncing = false;

	public boolean isSyncing() {
		return isSyncing;
	}

	public void setSyncing(boolean isSyncing) {
		this.isSyncing = isSyncing;
	}

}
