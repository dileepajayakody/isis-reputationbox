package org.nic.isis.reputation.dom;

import java.util.Date;
import java.util.List;

import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;
import javax.jdo.annotations.VersionStrategy;

import org.apache.isis.applib.annotation.ObjectType;
import org.apache.isis.applib.annotation.Title;

@javax.jdo.annotations.PersistenceCapable(identityType=IdentityType.DATASTORE)
@javax.jdo.annotations.DatastoreIdentity(
        strategy=javax.jdo.annotations.IdGeneratorStrategy.IDENTITY,
         column="id")
@javax.jdo.annotations.Version(
        strategy=VersionStrategy.VERSION_NUMBER, 
        column="version")
@ObjectType("EMAIL")
public class Email {

	@Persistent
	private String messageId;
	
	@Title
	@javax.jdo.annotations.Column(allowsNull="false")
	public String getMessageId() {
		return messageId;
	}

	public void setMessageId(String messageId) {
		this.messageId = messageId;
	}
	
	//for email analysis
	@Persistent
	private List<String> subjectTerms;
	
	public List<String> getSubjectTerms() {
		return subjectTerms;
	}

	public void setSubjectTerms(List<String> subjectTerms) {
		this.subjectTerms = subjectTerms;
	}

	@Persistent
	private List<String> bodyTerms;
	
	public List<String> getBodyTerms() {
		return bodyTerms;
	}

	public void setBodyTerms(List<String> bodyTerms) {
		this.bodyTerms = bodyTerms;
	}

	@Persistent
	private String inReplytoMessageId;

	@javax.jdo.annotations.Column(allowsNull="true")
	public String getInReplytoMessageId() {
		return inReplytoMessageId;
	}

	public void setInReplytoMessageId(String inReplytoMessageId) {
		this.inReplytoMessageId = inReplytoMessageId;
	}

	@Persistent
	private String subject;
	
	@javax.jdo.annotations.Column(allowsNull="true")
	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	@Persistent
	private Reputation reputation;
	
	@javax.jdo.annotations.Column(allowsNull="true")
	public Reputation getReputation() {
		return reputation;
	}

	public void setReputation(Reputation reputation) {
		this.reputation = reputation;
	}

	@Persistent
	private List<String> toAddresses;
	
	public List<String> getToAddresses() {
		return toAddresses;
	}

	public void setToAddresses(List<String> toAddresses) {
		this.toAddresses = toAddresses;
	}

	@Persistent
	private List<String> ccAddresses;
	
	public List<String> getCcAddresses() {
		return ccAddresses;
	}

	public void setCcAddresses(List<String> ccAddresses) {
		this.ccAddresses = ccAddresses;
	}

	@Persistent
	private String fromAddress;
	
	@javax.jdo.annotations.Column(allowsNull="true")
	public String getFromAddress() {
		return fromAddress;
	}

	public void setFromAddress(String fromAddress) {
		this.fromAddress = fromAddress;
	}

	@Persistent
	private String replyToAddress;
	
	@javax.jdo.annotations.Column(allowsNull="true")
	public String getReplyToAddress() {
		return replyToAddress;
	}

	public void setReplyToAddress(String replyToAddress) {
		this.replyToAddress = replyToAddress;
	}

	@Persistent
	private String listAddress;
	
	@javax.jdo.annotations.Column(allowsNull="true")
	public String getListAddress() {
		return listAddress;
	}

	public void setListAddress(String listAddress) {
		this.listAddress = listAddress;
	}

	@Persistent
	private String contentType;
	
	@javax.jdo.annotations.Column(allowsNull="true")
	public String getContentType() {
		return contentType;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	@Persistent
	private String charSet;
	
	@javax.jdo.annotations.Column(allowsNull="true")
	public String getCharSet() {
		return charSet;
	}

	public void setCharSet(String charSet) {
		this.charSet = charSet;
	}

	@Persistent
	private Date date;
	
	@javax.jdo.annotations.Column(allowsNull="true")
	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	@Persistent
	private String gmailThreadId;
	
	@javax.jdo.annotations.Column(allowsNull="true")
	public String getGmailThreadId() {
		return gmailThreadId;
	}

	public void setGmailThreadId(String gmailThreadId) {
		this.gmailThreadId = gmailThreadId;
	}

	@Persistent
	private List<String> folders;
	
	public List<String> getFolders() {
		return folders;
	}

	public void setFolders(List<String> folders) {
		this.folders = folders;
	}

	public Reputation calcReputation(ReputationCriteria criteria){
		//to do
		return null;
	}
}
