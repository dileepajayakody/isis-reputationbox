package org.nic.isis.reputation.dom;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.VersionStrategy;

import org.apache.isis.applib.annotation.ObjectType;

@javax.jdo.annotations.PersistenceCapable(identityType=IdentityType.DATASTORE)
@javax.jdo.annotations.DatastoreIdentity(
        strategy=javax.jdo.annotations.IdGeneratorStrategy.IDENTITY,
         column="id")
@javax.jdo.annotations.Version(
        strategy=VersionStrategy.VERSION_NUMBER, 
        column="version")
@ObjectType("EMAILCONTACT")
public class EmailContact {

    //region > constructor
    public EmailContact(){
        emailAddresses = new HashSet<EmailAddress>();
        fromMessageIds = new ArrayList<String>();
        toMessageIds = new ArrayList<String>();
        ccMessageIds = new ArrayList<String>();
    }
    
    private String name;
    private String primaryEmailAddress;
    
	private Reputation contactReputation; 
	private Set<EmailAddress> emailAddresses;

	public void addEmailAddress(EmailAddress emailAddress){
		emailAddresses.add(emailAddress);
	}
    //endregion
	
	private int noOfMessagesFrom;
	private int noOfMessagesCC;
	private int noOfMessagesTo;
	
	private int noOfMesagesSeen;
	private int noOfMessagesAnswered;
	private int noOfMessagesFlagged;
	private int noOfMessagesDeleted;
	
	private List<String> fromMessageIds;
	private List<String> toMessageIds;
	private List<String> ccMessageIds;
	
	public int getNoOfMessagesFrom() {
		return noOfMessagesFrom;
	}

	public void setNoOfMessagesFrom(int noOfMessagesFrom) {
		this.noOfMessagesFrom = noOfMessagesFrom;
	}

	public int getNoOfMessagesCC() {
		return noOfMessagesCC;
	}

	public void setNoOfMessagesCC(int noOfMessagesCC) {
		this.noOfMessagesCC = noOfMessagesCC;
	}


	public int getNoOfMessagesAnswered() {
		return noOfMessagesAnswered;
	}

	public void setNoOfMessagesAnswered(int noOfMessagesAnswered) {
		this.noOfMessagesAnswered = noOfMessagesAnswered;
	}

	public int getNoOfMesagesSeen() {
		return noOfMesagesSeen;
	}

	public void setNoOfMesagesSeen(int noOfMesagesSeen) {
		this.noOfMesagesSeen = noOfMesagesSeen;
	}

	public int getNoOfMessagesFlagged() {
		return noOfMessagesFlagged;
	}

	public void setNoOfMessagesFlagged(int noOfMessagesFlagged) {
		this.noOfMessagesFlagged = noOfMessagesFlagged;
	}

	public int getNoOfMessagesDeleted() {
		return noOfMessagesDeleted;
	}

	public void setNoOfMessagesDeleted(int noOfMessagesDeleted) {
		this.noOfMessagesDeleted = noOfMessagesDeleted;
	}
	
	public void addMessageFrom(String msgId){
		this.noOfMessagesFrom++;
		this.fromMessageIds.add(msgId);
	}
	
	public void addMessageCC(String msgId){
		this.noOfMessagesCC++;
		this.ccMessageIds.add(msgId);
	}
	
	public void addMessageTo(String msgId){
		this.noOfMessagesTo++;
		this.toMessageIds.add(msgId);
	}

	public void addMessageAnswered(){
		this.noOfMessagesAnswered++;
	}
	
	public void addMessageSeen(){
		this.noOfMesagesSeen++;
	}
	
	public void addMessageFlagged(){
		this.noOfMessagesFlagged++;
	}
	
	public void addMessageDeleted(){
		this.noOfMessagesDeleted++;
	}
	
	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull="true")
	public List<String> getFromMessageIds() {
		return fromMessageIds;
	}

	public void setFromMessageIds(List<String> fromMessageIds) {
		this.fromMessageIds = fromMessageIds;
	}

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull="true")
	public List<String> getToMessageIds() {
		return toMessageIds;
	}

	public void setToMessageIds(List<String> toMessageIds) {
		this.toMessageIds = toMessageIds;
	}

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull="true")
	public List<String> getCcMessageIds() {
		return ccMessageIds;
	}

	public void setCcMessageIds(List<String> ccMessageIds) {
		this.ccMessageIds = ccMessageIds;
	}

	public int getNoOfMessagesTo() {
		return noOfMessagesTo;
	}

	public void setNoOfMessagesTo(int noOfMessagesTo) {
		this.noOfMessagesTo = noOfMessagesTo;
	}

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull="true")
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public Reputation getContactReputation() {
		return contactReputation;
	}

	public void setContactReputation(Reputation contactReputation) {
		this.contactReputation = contactReputation;
	}
	
	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public Set<EmailAddress> getEmailAddresses() {
		return emailAddresses;
	}

	public void setEmailAddresses(Set<EmailAddress> emailAddresses) {
		this.emailAddresses = emailAddresses;
	}

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull="true")
	public String getPrimaryEmailAddress() {
		return primaryEmailAddress;
	}

	public void setPrimaryEmailAddress(String primaryEmailAddress) {
		this.primaryEmailAddress = primaryEmailAddress;
	}
}
