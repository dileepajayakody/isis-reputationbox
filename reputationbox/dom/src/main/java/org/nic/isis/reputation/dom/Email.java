package org.nic.isis.reputation.dom;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.VersionStrategy;

import org.apache.isis.applib.annotation.ObjectType;
import org.apache.isis.applib.annotation.Title;

import edu.ucla.sspace.common.Similarity;
import edu.ucla.sspace.vector.IntegerVector;
import edu.ucla.sspace.vector.SparseDoubleVector;
import edu.ucla.sspace.vector.Vector;

@javax.jdo.annotations.Queries({ @javax.jdo.annotations.Query(
		name = "findEmailsForPeriod", 
		language = "JDOQL", 
		value = "SELECT FROM org.nic.isis.reputation.dom.Email WHERE mailboxId == mailboxId "
				+ "&& sentTimestamp > fromDate && sentTimestamp <= toDate "
				+ "PARAMETERS String mailboxId, long fromDate, long toDate "
				+ "ORDER BY sentTimestamp ASC") })
@javax.jdo.annotations.PersistenceCapable(identityType = IdentityType.DATASTORE)
@javax.jdo.annotations.DatastoreIdentity(strategy = javax.jdo.annotations.IdGeneratorStrategy.IDENTITY, column = "id")
@javax.jdo.annotations.Version(strategy = VersionStrategy.VERSION_NUMBER, column = "version")
@ObjectType("EMAIL")
public class Email {

	public static final String ANSWERED = "/Answered";
	public static final String SEEN = "/Seen";
	public static final String FLAGGED = "/Flagged";
	public static final String DELETED = "/Deleted";
	public static final String DRAFT = "/Draft";
	
	private String mailboxId;
	private String textClusterId;
	private String peopleClusterId;
	//added for subject and body based clustering
	private String weightedSubjectBodyClusterId;
	
	private String messageId;
	private String gmailThreadId;
	private String inReplytoMessageId;
	
	private boolean isReceivedMail = false;
	private boolean isSentMail = false;
	
	@javax.jdo.annotations.Persistent
	private TextContent bodyContent;
	
	@javax.jdo.annotations.Persistent
	private TextContent subjectContent;

	private String subject;
	
	
	private List<String> toAddresses;
	private List<String> ccAddresses;
	private List<String> bccAddresses;
	
	//NLP POS results for content
	private Set<String> persons;
	private Set<String> locations;
	private Set<String> organizations;
	//now only using keywords for NN, NNS type pos
	private List<String> keywords;
	
	@javax.jdo.annotations.NotPersistent
	private Map<String, Integer> keywordMatrix = new HashMap<String, Integer>();
	
	private String fromAddress;	
	private String replyToAddress;
	//these attributes are extracted from the email header
	//can use for classification
	private String listAddress;
	private boolean isSpam;
	private boolean isListMail;
	//1: Highest, 2:High, 3:Normal, 5:low
	private  int importanceLevelByHeader;
	private boolean isImportantByHeader;
	//personal, private, company confidential
	private boolean isSensitiveByHeader;
	//list, bulk, first-class, junk
	private String precedenceLevelByHeader;
	
	//speech act features
	//request, delivery, commit, propose, meeting
	private boolean isRequest;
	private boolean isDelivery;
	private boolean isCommit;
	private boolean isPropose;
	private boolean isMeeting;
	
	private String contentType;
	private String charSet;	
	
	private long sentTimestamp;

	private List<String> folders;
	private List<EmailAttachment> emailAttachments;
	private int noOfAttachments;
	private int noOfImages;
	
	private String emailHeaders;
	
	private double[] textContextVector;
	private double[] recipientContextVector;
	private double[] speechActVector;
	private double[] nlpKeywordsVector;
	//all feature contextvector
	private double[] allFeatureVector;
	
	//separate vectors to represent subject and body
	private double[] subjectContextVector;
	private double[] bodyContextVector;
	//separate vectors for from,to,cc
	private double[] fromContextVector;
	private double[] toContextVector;
	private double[] ccContextVector;
	
	
	private boolean isAnswered = false;
	private boolean isSeen = false;
	private boolean isFlagged = false;
	private boolean isDeleted = false;
	private List<String> userFlags;
	
	//whether this is ccd or bccd or to'ed to me directly
	private boolean isDirect = false;
	private boolean isCCd = false;
	private boolean isBCCd = false;
	
	
	
	//private Reputation reputation;
	//for simple score..have to extend using Reputation object
	//these are assigned for clustering results
	private double contentReputationScore;
	private double recipientReputationScore;
	private double weightedSubjectBodyContentScore;
	
	//whether this is used for model creation or prediction
	private boolean isModel;
	private boolean isPredicted;
	
	//recommended actions for new emails to classify
	private boolean needReply;
	private boolean needRead;
	private boolean needFlag;
	private boolean needIgnore;
	private boolean needDelete;
	
	//new scores for each category flaggedTopicScore, repliedTopicScore etc
	private double flaggedTopicscore;
	private double repliedTopicscore; 
	private double seenTopicscore;
	private double spamTopicScore;
	private double totalTopicScore;
	
	private double flaggedPeoplescore;
	private double repliedPeoplescore; 
	private double seenPeoplescore;
	private double spamPeopleScore;
	private double totalPeopleScore;
	
	//nlp keywords
	private double flaggedKeywordscore;
	private double repliedKeywordscore; 
	private double seenKeywordscore;
	private double spamKeywordscore;
	private double totalKeywordscore;
	
	private double flaggedSpeechActscore;
	private double repliedSpeechActscore; 
	private double seenSpeechActscore;
	private double spamSpeechActscore;
	private double totalSpeechActscore;
	
	
	private long msgUid;
	
	private TextTokenMap wordFrequencyMap;
	
	public Reputation calcReputation(ReputationCriteria criteria) {
		// to do
		return null;
	}
	
	
	public boolean isAnswered() {
		return isAnswered;
	}

	public void setAnswered(boolean isAnswered) {
		this.isAnswered = isAnswered;
	}

	public boolean isSeen() {
		return isSeen;
	}

	public void setSeen(boolean isSeen) {
		this.isSeen = isSeen;
	}

	public boolean isFlagged() {
		return isFlagged;
	}

	public void setFlagged(boolean isFlagged) {
		this.isFlagged = isFlagged;
	}

	public boolean isDeleted() {
		return isDeleted;
	}

	public void setDeleted(boolean isDeleted) {
		this.isDeleted = isDeleted;
	}
	
	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public String getTextClusterId() {
		return textClusterId;
	}

	public void setTextClusterId(String textClusterId) {
		this.textClusterId = textClusterId;
	}
	
	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public String getMailboxId() {
		return mailboxId;
	}

	public void setMailboxId(String mailbox) {
		this.mailboxId = mailbox;
	}
	
	@javax.jdo.annotations.Column(allowsNull = "true")
	public double[] getRecipientContextVector() {
		return recipientContextVector;
	}

	public void setRecipientContextVector(double[] recipientContextVector) {
		this.recipientContextVector = recipientContextVector;
	}
	
	@javax.jdo.annotations.Column(allowsNull = "true")
	public double[] getTextContextVector() {
		return textContextVector;
	}

	public void setTextContextVector(double[] txtContextVector) {
		this.textContextVector = txtContextVector;
	}
	
	@javax.jdo.annotations.Column(allowsNull = "true", length = 1000)
	public String getEmailHeaders() {
		return emailHeaders;
	}

	public void setEmailHeaders(String emailHedears) {
		this.emailHeaders = emailHedears;
	}

	//private EmailFlag emailFlag;
	
	/*@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public EmailFlag getEmailFlags() {
		return emailFlag;
	}

	public void setEmailFlags(EmailFlag emailFlag) {
		this.emailFlag = emailFlag;
		this.setFlagged(emailFlag.isFlagged());
		this.setSeen(emailFlag.isSeen());
		this.setAnswered(emailFlag.isAnswered());
		this.setDeleted(emailFlag.isDeleted());		
	}*/
	
	@javax.jdo.annotations.Persistent
	public List<EmailAttachment> getEmailAttachments() {
		return emailAttachments;
	}

	public void setEmailAttachments(List<EmailAttachment> emailAttachments) {
		this.emailAttachments = emailAttachments;
	}

	@javax.jdo.annotations.Persistent
	public List<String> getFolders() {
		return folders;
	}

	public void setFolders(List<String> folders) {
		this.folders = folders;
		/*if(folders != null){
			//process folders to see if it's a received or sent email
			if(folders.contains("INBOX")){
				//received email
				isSentMail = false;
				isReceivedMail = true;
			}
			
			else if (folders.contains("[Gmail]/Sent Mail")){
				//sent email
				isReceivedMail = false;
				isSentMail = true;
			}
		}
*/
	}
	
	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public long getSentTimestamp() {
		return sentTimestamp;
	}

	public void setSentTimestamp(long timestamp) {
		this.sentTimestamp = timestamp;
	}
	
	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public String getGmailThreadId() {
		return gmailThreadId;
	}

	public void setGmailThreadId(String gmailThreadId) {
		this.gmailThreadId = gmailThreadId;
	}
	
	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public String getCharSet() {
		return charSet;
	}

	public void setCharSet(String charSet) {
		this.charSet = charSet;
	}

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public String getContentType() {
		return contentType;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public String getListAddress() {
		return listAddress;
	}

	public void setListAddress(String listAddress) {
		this.listAddress = listAddress;
	}
	
	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public String getReplyToAddress() {
		return replyToAddress;
	}

	public void setReplyToAddress(String replyToAddress) {
		this.replyToAddress = replyToAddress;
	}
	
	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public String getFromAddress() {
		return fromAddress;
	}

	public void setFromAddress(String fromAddress) {
		this.fromAddress = fromAddress;
	}
	
	@javax.jdo.annotations.Persistent
	public List<String> getToAddresses() {
		return toAddresses;
	}

	public void setToAddresses(List<String> toAddresses) {
		this.toAddresses = toAddresses;
		/*for(String toAddress : toAddresses){
			if(toAddress.equalsIgnoreCase(mailboxId)){
				this.setDirect(true);
				break;
			}
		}*/
	}
	
	@javax.jdo.annotations.Persistent
	public List<String> getCcAddresses() {
		return ccAddresses;
	}

	public void setCcAddresses(List<String> ccAddresses) {
		this.ccAddresses = ccAddresses;
		/*for(String ccAddress : ccAddresses){
			if(ccAddress.equalsIgnoreCase(mailboxId)){
				this.setCCd(true);
				break;
			}
		}*/
	}
	
	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}
	
	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public String getInReplytoMessageId() {
		return inReplytoMessageId;
	}

	public void setInReplytoMessageId(String inReplytoMessageId) {
		this.inReplytoMessageId = inReplytoMessageId;
	}

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public TextContent getSubjectContent() {
		return subjectContent;
	}

	public void setSubjectContent(TextContent text) {
		this.subjectContent = text;
	}
	
	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public TextContent getBodyContent() {
		return bodyContent;
	}

	public void setBodyContent(TextContent text) {
		this.bodyContent = text;
	}
	
	@javax.jdo.annotations.Persistent
	public boolean isReceivedMail() {
		return isReceivedMail;
	}

	public void setReceivedMail(boolean isReceivedMail) {
		this.isReceivedMail = isReceivedMail;
	}
	
	@javax.jdo.annotations.Persistent
	public boolean isSentMail() {
		return isSentMail;
	}

	public void setSentMail(boolean isSentMail) {
		this.isSentMail = isSentMail;
	}

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public String getPeopleClusterId() {
		return peopleClusterId;
	}

	public void setPeopleClusterId(String peopleClusterId) {
		this.peopleClusterId = peopleClusterId;
	}
	
	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "false")
	public String getMessageId() {
		return messageId;
	}

	public void setMessageId(String messageId) {
		this.messageId = messageId;
	}


	public boolean isDirect() {
		return isDirect;
	}


	public void setDirect(boolean isDirect) {
		this.isDirect = isDirect;
	}


	public boolean isCCd() {
		return isCCd;
	}


	public void setCCd(boolean isCCd) {
		this.isCCd = isCCd;
	}


	public boolean isBCCd() {
		return isBCCd;
	}


	public void setBCCd(boolean isBCCd) {
		this.isBCCd = isBCCd;
	}


	@javax.jdo.annotations.Persistent
	public double getContentReputationScore() {
		return contentReputationScore;
	}


	public void setContentReputationScore(double reputationScore) {
		this.contentReputationScore = reputationScore;
	}


	@javax.jdo.annotations.Persistent
	public double getRecipientReputationScore() {
		return recipientReputationScore;
	}


	public void setRecipientReputationScore(double recipientReputationScore) {
		this.recipientReputationScore = recipientReputationScore;
	}

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "false")
	public TextTokenMap getWordFrequencyMap() {
		return wordFrequencyMap;
	}


	public void setWordFrequencyMap(TextTokenMap wordFrequencyMap) {
		this.wordFrequencyMap = wordFrequencyMap;
	}

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public long getMsgUid() {
		return msgUid;
	}


	public void setMsgUid(long msgUid) {
		this.msgUid = msgUid;
	}

	@javax.jdo.annotations.Persistent
	public List<String> getBccAddresses() {
		return bccAddresses;
	}


	public void setBccAddresses(List<String> bccAddresses) {
		this.bccAddresses = bccAddresses;
	}


	public int getNoOfAttachments() {
		return noOfAttachments;
	}


	public void setNoOfAttachments(int noOfAttachments) {
		this.noOfAttachments = noOfAttachments;
	}


	public int getNoOfImages() {
		return noOfImages;
	}


	public void setNoOfImages(int noOfImages) {
		this.noOfImages = noOfImages;
	}
	
	public boolean isSpam() {
		return isSpam;
	}

	public void setSpam(boolean isSpam) {
		this.isSpam = isSpam;
	}

	public boolean isListMail() {
		return isListMail;
	}


	public void setListMail(boolean isListMail) {
		this.isListMail = isListMail;
	}


	public int getImportanceLevelByHeader() {
		return importanceLevelByHeader;
	}


	public void setImportanceLevelByHeader(int importanceLevelByHeader) {
		this.importanceLevelByHeader = importanceLevelByHeader;
	}


	public boolean isSensitiveByHeader() {
		return isSensitiveByHeader;
	}


	public void setSensitiveByHeader(boolean isSensitiveByHeader) {
		this.isSensitiveByHeader = isSensitiveByHeader;
	}


	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public String getPrecedenceLevelByHeader() {
		return precedenceLevelByHeader;
	}


	public void setPrecedenceLevelByHeader(String precedenceLevelByHeader) {
		this.precedenceLevelByHeader = precedenceLevelByHeader;
	}


	public boolean isRequest() {
		return isRequest;
	}


	public void setRequest(boolean isRequest) {
		this.isRequest = isRequest;
	}


	public boolean isDelivery() {
		return isDelivery;
	}


	public void setDelivery(boolean isDelivery) {
		this.isDelivery = isDelivery;
	}


	public boolean isCommit() {
		return isCommit;
	}


	public void setCommit(boolean isCommit) {
		this.isCommit = isCommit;
	}


	public boolean isPropose() {
		return isPropose;
	}


	public void setPropose(boolean isPropose) {
		this.isPropose = isPropose;
	}


	public boolean isMeeting() {
		return isMeeting;
	}


	public void setMeeting(boolean isMeeting) {
		this.isMeeting = isMeeting;
	}

	@javax.jdo.annotations.Persistent
	public List<String> getUserFlags() {
		return userFlags;
	}


	public void setUserFlags(List<String> userFlags) {
		this.userFlags = userFlags;
	}

	@javax.jdo.annotations.Column(allowsNull = "true")
	public double[] getAllFeatureVector() {
		return allFeatureVector;
	}


	public void setAllFeatureVector(double[] allFeatureVector) {
		this.allFeatureVector = allFeatureVector;
	}


	public boolean getIsImportantByHeader() {
		return isImportantByHeader;
	}


	public void setIsImportantByHeader(boolean isImportantByHeader) {
		this.isImportantByHeader = isImportantByHeader;
	}

	@javax.jdo.annotations.Column(allowsNull= "true" )
	public double[] getSpeechActVector() {
		return speechActVector;
	}


	public void setSpeechActVector(double[] speechActVector) {
		this.speechActVector = speechActVector;
	}


	

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public Set<String> getPersons() {
		return persons;
	}


	public void setPersons(Set<String> persons) {
		this.persons = persons;
	}

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public Set<String> getLocations() {
		return locations;
	}


	public void setLocations(Set<String> locations) {
		this.locations = locations;
	}

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public Set<String> getOrganizations() {
		return organizations;
	}


	public void setOrganizations(Set<String> organizations) {
		this.organizations = organizations;
	}


	public boolean isNeedReply() {
		return needReply;
	}


	public void setNeedReply(boolean needReply) {
		this.needReply = needReply;
	}


	public boolean isNeedRead() {
		return needRead;
	}


	public void setNeedRead(boolean needRead) {
		this.needRead = needRead;
	}


	public boolean isNeedFlag() {
		return needFlag;
	}


	public void setNeedFlag(boolean needFlag) {
		this.needFlag = needFlag;
	}


	public boolean isNeedIgnore() {
		return needIgnore;
	}


	public void setNeedIgnore(boolean needIgnore) {
		this.needIgnore = needIgnore;
	}


	public boolean isNeedDelete() {
		return needDelete;
	}


	public void setNeedDelete(boolean needDelete) {
		this.needDelete = needDelete;
	}


	
	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public List<String> getKeywords() {
		return keywords;
	}


	public void setKeywords(List<String> keywords) {
		this.keywords = keywords;
	}

	@javax.jdo.annotations.NotPersistent
	public Map<String, Integer> getKeywordMatrix() {
		return keywordMatrix;
	}


	public void setKeywordMatrix(Map<String, Integer> keywordMatrix) {
		this.keywordMatrix = keywordMatrix;
	}

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public double[] getNlpKeywordsVector() {
		return nlpKeywordsVector;
	}


	public void setNlpKeywordsVector(double[] nlpKeywordsVector) {
		this.nlpKeywordsVector = nlpKeywordsVector;
	}

	
	public boolean isModel() {
		return isModel;
	}


	public boolean isPredicted() {
		return isPredicted;
	}


	public void setImportantByHeader(boolean isImportantByHeader) {
		this.isImportantByHeader = isImportantByHeader;
	}


	public void setModel(boolean isModel) {
		this.isModel = isModel;
	}


	public void setPredicted(boolean isPredicted) {
		this.isPredicted = isPredicted;
	}


	public double getFlaggedTopicscore() {
		return flaggedTopicscore;
	}


	public void setFlaggedTopicscore(double flaggedTopicscore) {
		this.flaggedTopicscore = flaggedTopicscore;
	}


	public double getRepliedTopicscore() {
		return repliedTopicscore;
	}


	public void setRepliedTopicscore(double repliedTopicscore) {
		this.repliedTopicscore = repliedTopicscore;
	}


	public double getSeenTopicscore() {
		return seenTopicscore;
	}


	public void setSeenTopicscore(double seenTopicscore) {
		this.seenTopicscore = seenTopicscore;
	}


	public double getSpamTopicScore() {
		return spamTopicScore;
	}


	public void setSpamTopicScore(double spamTopicScore) {
		this.spamTopicScore = spamTopicScore;
	}


	public double getFlaggedPeoplescore() {
		return flaggedPeoplescore;
	}


	public void setFlaggedPeoplescore(double flaggedPeoplescore) {
		this.flaggedPeoplescore = flaggedPeoplescore;
	}


	public double getRepliedPeoplescore() {
		return repliedPeoplescore;
	}


	public void setRepliedPeoplescore(double repliedPeoplescore) {
		this.repliedPeoplescore = repliedPeoplescore;
	}


	public double getSeenPeoplescore() {
		return seenPeoplescore;
	}


	public void setSeenPeoplescore(double seenPeoplescore) {
		this.seenPeoplescore = seenPeoplescore;
	}


	public double getSpamPeopleScore() {
		return spamPeopleScore;
	}


	public void setSpamPeopleScore(double spamPeopleScore) {
		this.spamPeopleScore = spamPeopleScore;
	}


	public double getFlaggedKeywordscore() {
		return flaggedKeywordscore;
	}


	public void setFlaggedKeywordscore(double flaggedKeywordscore) {
		this.flaggedKeywordscore = flaggedKeywordscore;
	}


	public double getRepliedKeywordscore() {
		return repliedKeywordscore;
	}


	public void setRepliedKeywordscore(double repliedKeywordscore) {
		this.repliedKeywordscore = repliedKeywordscore;
	}


	public double getSeenKeywordscore() {
		return seenKeywordscore;
	}


	public void setSeenKeywordscore(double seenKeywordscore) {
		this.seenKeywordscore = seenKeywordscore;
	}


	public double getSpamKeywordscore() {
		return spamKeywordscore;
	}


	public void setSpamKeywordscore(double spamKeywordscore) {
		this.spamKeywordscore = spamKeywordscore;
	}


	public double getTotalKeywordscore() {
		return totalKeywordscore;
	}


	public void setTotalKeywordscore(double totalKeywordscore) {
		this.totalKeywordscore = totalKeywordscore;
	}


	public double getFlaggedSpeechActscore() {
		return flaggedSpeechActscore;
	}


	public void setFlaggedSpeechActscore(double flaggedSpeechActscore) {
		this.flaggedSpeechActscore = flaggedSpeechActscore;
	}


	public double getRepliedSpeechActscore() {
		return repliedSpeechActscore;
	}


	public void setRepliedSpeechActscore(double repliedSpeechActscore) {
		this.repliedSpeechActscore = repliedSpeechActscore;
	}


	public double getSeenSpeechActscore() {
		return seenSpeechActscore;
	}


	public void setSeenSpeechActscore(double seenSpeechActscore) {
		this.seenSpeechActscore = seenSpeechActscore;
	}


	public double getSpamSpeechActscore() {
		return spamSpeechActscore;
	}


	public void setSpamSpeechActscore(double spamSpeechActscore) {
		this.spamSpeechActscore = spamSpeechActscore;
	}


	public double getTotalSpeechActscore() {
		return totalSpeechActscore;
	}


	public void setTotalSpeechActscore(double totalSpeechActscore) {
		this.totalSpeechActscore = totalSpeechActscore;
	}


	public double getTotalTopicScore() {
		return totalTopicScore;
	}


	public void setTotalTopicScore(double totalTopicScore) {
		this.totalTopicScore = totalTopicScore;
	}


	public double getTotalPeopleScore() {
		return totalPeopleScore;
	}


	public void setTotalPeopleScore(double totalPeopleScore) {
		this.totalPeopleScore = totalPeopleScore;
	}

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public double[] getSubjectContextVector() {
		return subjectContextVector;
	}


	public void setSubjectContextVector(double[] subjectContextVector) {
		this.subjectContextVector = subjectContextVector;
	}

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public double[] getBodyContextVector() {
		return bodyContextVector;
	}


	public void setBodyContextVector(double[] bodyContextVector) {
		this.bodyContextVector = bodyContextVector;
	}

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public double[] getFromContextVector() {
		return fromContextVector;
	}


	public void setFromContextVector(double[] fromContextVector) {
		this.fromContextVector = fromContextVector;
	}

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public double[] getToContextVector() {
		return toContextVector;
	}


	public void setToContextVector(double[] toContextVector) {
		this.toContextVector = toContextVector;
	}

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public double[] getCcContextVector() {
		return ccContextVector;
	}


	public void setCcContextVector(double[] ccContextVector) {
		this.ccContextVector = ccContextVector;
	}

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public String getWeightedSubjectBodyClusterId() {
		return weightedSubjectBodyClusterId;
	}


	public void setWeightedSubjectBodyClusterId(
			String weightedSubjectBodyClusterId) {
		this.weightedSubjectBodyClusterId = weightedSubjectBodyClusterId;
	}

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public double getWeightedSubjectBodyContentScore() {
		return weightedSubjectBodyContentScore;
	}


	public void setWeightedSubjectBodyContentScore(
			double weightedSubjectBodyContentScore) {
		this.weightedSubjectBodyContentScore = weightedSubjectBodyContentScore;
	}



}
