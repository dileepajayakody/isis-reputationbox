package org.nic.isis.reputation.dom;

import java.util.Date;
import java.util.List;
import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.VersionStrategy;
import org.apache.isis.applib.annotation.ObjectType;
import org.apache.isis.applib.annotation.Title;

@javax.jdo.annotations.PersistenceCapable(identityType = IdentityType.DATASTORE)
@javax.jdo.annotations.DatastoreIdentity(strategy = javax.jdo.annotations.IdGeneratorStrategy.IDENTITY, column = "id")
@javax.jdo.annotations.Version(strategy = VersionStrategy.VERSION_NUMBER, column = "version")
@ObjectType("EMAIL")
public class Email {

	// region > messageId (property)
	private String messageId;

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "false")
	@Title
	public String getMessageId() {
		return messageId;
	}

	public void setMessageId(String messageId) {
		this.messageId = messageId;
	}

	// endregion

	// endregion

	// region > textContent (property)
	@javax.jdo.annotations.Persistent
	private TextContent textContent;

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public TextContent getTextContent() {
		return textContent;
	}

	public void setTextContent(TextContent bodyContent) {
		this.textContent = bodyContent;
	}

	// endregion

	// region > inReplytoMessageId (property)
	private String inReplytoMessageId;

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public String getInReplytoMessageId() {
		return inReplytoMessageId;
	}

	public void setInReplytoMessageId(String inReplytoMessageId) {
		this.inReplytoMessageId = inReplytoMessageId;
	}

	// endregion

	// region > subject (property)
	private String subject;

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	// endregion

	// region > reputation (property)
	private Reputation reputation;

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public Reputation getReputation() {
		return reputation;
	}

	public void setReputation(Reputation reputation) {
		this.reputation = reputation;
	}

	// endregion

	// region > toAddresses (collection of strings)
	private List<String> toAddresses;

	@javax.jdo.annotations.Persistent
	public List<String> getToAddresses() {
		return toAddresses;
	}

	public void setToAddresses(List<String> toAddresses) {
		this.toAddresses = toAddresses;
	}

	// endregion

	// region > ccAddresses (collection of strings)
	private List<String> ccAddresses;

	@javax.jdo.annotations.Persistent
	public List<String> getCcAddresses() {
		return ccAddresses;
	}

	public void setCcAddresses(List<String> ccAddresses) {
		this.ccAddresses = ccAddresses;
	}

	// endregion

	// region > fromAddress (property)
	private String fromAddress;

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public String getFromAddress() {
		return fromAddress;
	}

	public void setFromAddress(String fromAddress) {
		this.fromAddress = fromAddress;
	}

	// endregion

	// region > replyToAddress (property)
	private String replyToAddress;

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public String getReplyToAddress() {
		return replyToAddress;
	}

	public void setReplyToAddress(String replyToAddress) {
		this.replyToAddress = replyToAddress;
	}

	// endregion

	// region > listAddress (property)
	private String listAddress;

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public String getListAddress() {
		return listAddress;
	}

	public void setListAddress(String listAddress) {
		this.listAddress = listAddress;
	}

	// endregion

	// region > contentType (property)
	private String contentType;

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public String getContentType() {
		return contentType;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	// endregion

	// region > charSet (property)
	private String charSet;

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public String getCharSet() {
		return charSet;
	}

	public void setCharSet(String charSet) {
		this.charSet = charSet;
	}

	// endregion

	// region > date (property)
	private int sentTimestamp;

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public int getSentTimestamp() {
		return sentTimestamp;
	}

	public void setSentTimestamp(int timestamp) {
		this.sentTimestamp = timestamp;
	}

	// endregion

	// region > gmailThreadId (property)
	private String gmailThreadId;

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public String getGmailThreadId() {
		return gmailThreadId;
	}

	public void setGmailThreadId(String gmailThreadId) {
		this.gmailThreadId = gmailThreadId;
	}

	// endregion

	// region > folders (collection of strings)
	private List<String> folders;

	@javax.jdo.annotations.Persistent
	public List<String> getFolders() {
		return folders;
	}

	public void setFolders(List<String> folders) {
		this.folders = folders;
	}

	// endregion

	private List<EmailAttachment> emailAttachments;

	@javax.jdo.annotations.Persistent
	public List<EmailAttachment> getEmailAttachments() {
		return emailAttachments;
	}

	public void setEmailAttachments(List<EmailAttachment> emailAttachments) {
		this.emailAttachments = emailAttachments;
	}

	private String emailHedears;

	@javax.jdo.annotations.Column(allowsNull="true")
	public String getEmailHedears() {
		return emailHedears;
	}

	public void setEmailHedears(String emailHedears) {
		this.emailHedears = emailHedears;
	}

	// region > calcReputation (action)
	public Reputation calcReputation(ReputationCriteria criteria) {
		// to do
		return null;
	}
	// endregion

}
