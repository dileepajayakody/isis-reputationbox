/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.nic.isis.reputation.dom;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.PrimaryKey;
import javax.jdo.annotations.VersionStrategy;

import org.apache.isis.applib.ViewModel;
import org.apache.isis.applib.annotation.Hidden;
import org.apache.isis.applib.annotation.ObjectType;
import org.apache.isis.applib.annotation.Programmatic;
import org.apache.isis.applib.annotation.Title;
import org.nic.isis.clustering.EmailContentCluster;
import org.nic.isis.clustering.EmailRecipientCluster;
import org.nic.isis.clustering.KMeansClustering;
import org.nic.isis.reputation.services.EmailAnalysisService;
import org.nic.isis.ri.RandomIndexing;
import org.nic.isis.vector.VectorsMath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucla.sspace.clustering.OnlineKMeans;
import edu.ucla.sspace.common.Similarity;
import edu.ucla.sspace.index.RandomIndexVectorGenerator;
import edu.ucla.sspace.util.GeneratorMap;
import edu.ucla.sspace.vector.CompactSparseIntegerVector;
import edu.ucla.sspace.vector.CompactSparseVector;
import edu.ucla.sspace.vector.DoubleVector;
import edu.ucla.sspace.vector.IntegerVector;
import edu.ucla.sspace.vector.SparseDoubleVector;
import edu.ucla.sspace.vector.TernaryVector;
import edu.ucla.sspace.vector.Vector;
import edu.ucla.sspace.vector.VectorMath;

/**
 * @author dileepa
 * 
 */
@javax.jdo.annotations.Queries({ @javax.jdo.annotations.Query(name = "findMailbox", language = "JDOQL", value = "SELECT FROM org.nic.isis.reputation.dom.UserMailBox WHERE  emailId == emailId PARAMETERS String emailId") })
@javax.jdo.annotations.PersistenceCapable(identityType = IdentityType.DATASTORE)
@javax.jdo.annotations.DatastoreIdentity(strategy = javax.jdo.annotations.IdGeneratorStrategy.IDENTITY, column = "id")
@javax.jdo.annotations.Version(strategy = VersionStrategy.VERSION_NUMBER, column = "version")
@ObjectType("USERMAILBOX")
public class UserMailBox {

	private final static Logger logger = LoggerFactory
			.getLogger(UserMailBox.class);
	
	
	private IndexVectorMap contentIndexVectorMap;
	private ContextVectorMap contentContextVectorMap;

	private IndexVectorMap recipientIndexVectorMap;
	private ContextVectorMap recipientContextVectorMap;
	
	//important content profiles for the mailbox; for direct emails
	private double[] importantTopicsReplied;
	private double[] importantTopicsOnlySeen;
	private double[] importantTopicsFlagged;
	//important people profiles for the mailbox
	private double[] importantPeopleReplied;
	private double[] importantPeopleOnlySeen;
	private double[] importantPeopleFlagged;
	//important speech acts for the mailbox (meeting, request, delivery, proposal, commit)
	private double[] importantSpeechActReplied;
	private double[] importantSpeechActOnlySeen;
	private double[] importantSpeechActFlagged;
	
	//important list emails; which are handled separately because the load of list emails is high 
	private double[] importantListTopicsReplied;
	private double[] importantListTopicsOnlySeen;
	private double[] importantListTopicsFlagged;
	//important people profiles for the mailbox
	private double[] importantListPeopleReplied;
	private double[] importantListPeopleOnlySeen;
	private double[] importantListPeopleFlagged;
	//important speech acts for the mailbox (meeting, request, delivery, proposal, commit)
	private double[] importantListSpeechActReplied;
	private double[] importantListSpeechActOnlySeen;
	private double[] importantListSpeechActFlagged;
	
	
	//unimportant topics/people/speech act profile from spam, 
	private double[] unimportanttopicsVector;
	private double[] unimportantPeopleVector;
	private double[] unimportantSpeechActVector;
	
	
	private double[] importantNLPKeywordsReplied;
	private double[] importantNLPKeywordsOnlySeen;
	private double[] importantNLPKeywordsFlagged;
	
	private double[] importantListNLPKeywordsReplied;
	private double[] importantListNLPKeywordsOnlySeen;
	private double[] importantListNLPKeywordsFlagged;
	
	private double[] unimportantDirectKeywordVector;
	
	//important email numbers for the current model
	private int numberOfListEmails;
	private int numberOfDirectEmails;
	
	private int numberOfListEmailsReplied;
	private int numberOfListEmailsFlagged;
	private int numberOfListEmailsSeen;
	
	private int numberOfDirectEmailsReplied;
	private int numberOfDirectEmailsFlagged;
	private int numberOfDirectEmailsSeen;
	
	private int nofOfUnimportantEmails;
	private EmailReputationDataModel reputationDataModel;

	//if still updating the importance model
	private boolean isUpdatingModel;
	//unimportant emails from deleted, spam tagged emails
	

	public UserMailBox() {
		this.allEmailContacts = new ArrayList<EmailContact>();
		this.allEmails = new ArrayList<Email>();
		
		importantTopicsReplied = new double[4000];
		importantTopicsOnlySeen = new double[4000];
		importantTopicsFlagged = new double[4000];
		importantPeopleReplied = new double[4000];
		importantPeopleOnlySeen = new double[4000];
		importantPeopleFlagged = new double[4000];
		
		importantSpeechActReplied = new double[5];
		importantSpeechActOnlySeen = new double[5];
		importantSpeechActFlagged = new double[5];
		
		//list importance vectors
		importantListTopicsReplied = new double[4000];
		importantListTopicsOnlySeen = new double[4000];
		importantListTopicsFlagged = new double[4000];
		importantListPeopleReplied = new double[4000];
		importantListPeopleOnlySeen = new double[4000];
		importantListPeopleFlagged = new double[4000];
		
		importantListSpeechActReplied = new double[5];
		importantListSpeechActOnlySeen = new double[5];
		importantListSpeechActFlagged = new double[5];
		
		
		unimportanttopicsVector = new double[4000];
		unimportantPeopleVector = new double[4000];
		unimportantSpeechActVector = new double[5];
		
		//keyword vectors
		importantNLPKeywordsReplied = new double[4000];
		importantNLPKeywordsOnlySeen = new double[4000];
		importantNLPKeywordsFlagged = new double[4000];
		
		importantListNLPKeywordsReplied = new double[4000];
		importantListNLPKeywordsOnlySeen = new double[4000];
		importantListNLPKeywordsFlagged = new double[4000];
		
		unimportantDirectKeywordVector = new double[4000];
		
		reputationDataModel = new EmailReputationDataModel();
		
		// for tf, idf purposes for weighting RI results at clustering
		// this.wordMentionedEmailMap = new HashMap<String, List<String>>();
		//this.allWordCounts = new HashMap<String, Integer>();

		// required setup for email text analysis using RandomIndexing
		//RandomIndexVectorGenerator indexVectorGenerator = new RandomIndexVectorGenerator(
		//		RandomIndexing.DEFAULT_VECTOR_LENGTH, System.getProperties());
		
		//this.wordToIndexVector = new GeneratorMap<TernaryVector>(indexVectorGenerator);
		//this.wordToMeaningMap = new HashMap<String, double[]>();

		//this.recipientToIndexVector = new GeneratorMap<TernaryVector>(indexVectorGenerator);
		//this.recipientToMeaningMap = new HashMap<String, double[]>();

		
		// onlineKmeansClustering = new OnlineKMeans<DoubleVector>();
		
	}

	
	
	/*
	 * private OnlineKMeans<DoubleVector> onlineKmeansClustering;
	 * 
	 * @javax.jdo.annotations.Persistent
	 * 
	 * @javax.jdo.annotations.Column(allowsNull = "true") public
	 * OnlineKMeans<DoubleVector> getOnlineKmeansClustering() { return
	 * onlineKmeansClustering; }
	 * 
	 * public void setOnlineKmeansClustering( OnlineKMeans<DoubleVector>
	 * onlineKmeansClustering) { this.onlineKmeansClustering =
	 * onlineKmeansClustering; }
	 */

	// contextio account id
	private String accountId;

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	@Hidden
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
	@Title
	public String getEmailId() {
		return emailId;
	}

	public void setEmailId(String userId) {
		this.emailId = userId;
	}

	// endregion

	private String password;
	private String imapHostId;
	
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
	private int emailCount;

	@javax.jdo.annotations.Persistent
	@Hidden
	public int getEmailCount() {
		return emailCount;
	}

	public void setEmailCount(int emailCount) {
		this.emailCount = emailCount;
	}

	// endregion

	// region > receivedEmailCount (property)
	private int receivedEmailCount;

	@javax.jdo.annotations.Persistent
	@Hidden
	public int getReceivedEmailCount() {
		return receivedEmailCount;
	}

	public void setReceivedEmailCount(int emailCount) {
		this.receivedEmailCount = emailCount;
	}

	private void incrementReceivedEmailCount() {
		this.receivedEmailCount++;
	}

	// endregion

	// region > receivedEmailCount (property)
	private int sentEmailCount;

	@javax.jdo.annotations.Persistent
	public int getSentEmailCount() {
		return sentEmailCount;
	}

	public void setSentEmailCount(int emailCount) {
		this.sentEmailCount = emailCount;
	}

	private void incrementSentEmailCount() {
		this.sentEmailCount++;
	}

	// endregion

	// region > lastIndexTimestamp (property)
	// last index timestamp of the context.io call
	private long lastIndexTimestamp;
	
	private long lastIndexedMsgUid;

	@javax.jdo.annotations.Persistent
	public long getLastIndexTimestamp() {
		return lastIndexTimestamp;
	}

	public void setLastIndexTimestamp(long lastIndexTimestamp) {
		this.lastIndexTimestamp = lastIndexTimestamp;
	}

	// endregion

	// region > allEmails (programmatic), addEmail (action)
	private List<Email> allEmails;

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
		
		emailCount++;
		if (email.isReceivedMail()) {
			incrementReceivedEmailCount();
		} else {
			incrementSentEmailCount();
		}

		//classify email based on online-kmeans approach for content and recipients
		//email = classifyEmailBasedOnContentAndRecipients(email);
			
		this.getAllEmails().add(email);
		this.lastIndexTimestamp = email.getSentTimestamp();
		logger.info("Adding the email to mailbox and setting the last indexed message id "
				+ "of mailbox to : " + email.getMsgUid());
		this.setLastIndexedMsgUid(email.getMsgUid());
		
		
		
		// process EmailContact details;
		
		/*String fromAddress = email.getFromAddress();
		List<String> ccAddresses = email.getCcAddresses();
		List<String> toAddresses = email.getToAddresses();

		processEmailContact(fromAddress, email, true, false, false);
		for (String ccAddress : ccAddresses) {
			processEmailContact(ccAddress, email, false, true, false);
		}
		for (String toAddress : toAddresses) {
			processEmailContact(toAddress, email, false, false, true);
		}*/
		
		// populating the global matrices for word counts (for logIDF calc.)
		// addWords(email.getMessageId(),
		// email.getBodyContent().getStringTokens());
		// addWords(email.getMessageId(),
		// email.getSubjectContent().getStringTokens());

	}

	public void updateMailBoxProfiles(Email email){
			//processing the important/unimportant profile vectors
		//processing emails sent to lists and not directly to the user
		email.setModel(true);
		email.setPredicted(false);
		
		if(email.isListMail() && !email.isDirect() && !email.isCCd()){
			numberOfListEmails++;
			if(email.isAnswered()){
				logger.info("this is a list email answered");
				this.importantListTopicsReplied = VectorsMath.getMergedVector(this.importantListTopicsReplied, email.getTextContextVector());
				this.importantListPeopleReplied = VectorsMath.getMergedVector(this.importantListPeopleReplied, email.getRecipientContextVector());
				this.importantListSpeechActReplied = VectorsMath.getMergedVector(this.importantListSpeechActReplied, email.getSpeechActVector());	
				this.importantListNLPKeywordsReplied = VectorsMath.getMergedVector(this.importantListNLPKeywordsReplied, email.getNlpKeywordsVector());
				numberOfListEmailsReplied++;
				this.getReputationDataModel().getRepliedListEmails().add(email);
			}
			else if(email.isSeen()){
				logger.info("this is a list email seen");
				this.importantListTopicsOnlySeen = VectorsMath.getMergedVector(this.importantListTopicsOnlySeen, email.getTextContextVector());
				this.importantListPeopleOnlySeen = VectorsMath.getMergedVector(this.importantListPeopleOnlySeen, email.getRecipientContextVector());
				this.importantListSpeechActOnlySeen = VectorsMath.getMergedVector(this.importantListSpeechActOnlySeen, email.getSpeechActVector());
				
				this.importantListNLPKeywordsOnlySeen = VectorsMath.getMergedVector(this.importantListNLPKeywordsOnlySeen, email.getNlpKeywordsVector());
				numberOfListEmailsSeen++;
				this.getReputationDataModel().getSeenListEmails().add(email);
			}
			if(email.isFlagged()){
				logger.info("this is a list email flagged");
				this.importantListTopicsFlagged = VectorsMath.getMergedVector(this.importantListTopicsFlagged, email.getTextContextVector());
				this.importantListPeopleFlagged = VectorsMath.getMergedVector(this.importantListPeopleFlagged, email.getRecipientContextVector());
				this.importantListSpeechActFlagged = VectorsMath.getMergedVector(this.importantListSpeechActFlagged, email.getSpeechActVector());
				
				this.importantListNLPKeywordsFlagged = VectorsMath.getMergedVector(this.importantListNLPKeywordsFlagged, email.getNlpKeywordsVector());
				numberOfListEmailsFlagged++;
				this.getReputationDataModel().getFlaggedListEmails().add(email);
			}
		}else {
			//processing emails sent directly,ccd to user
			numberOfDirectEmails++;
			if(email.isAnswered()){
				logger.info("this is a direct email answered");
				this.importantTopicsReplied = VectorsMath.getMergedVector(this.importantTopicsReplied, email.getTextContextVector());
				this.importantPeopleReplied = VectorsMath.getMergedVector(this.importantPeopleReplied, email.getRecipientContextVector());
				this.importantSpeechActReplied = VectorsMath.getMergedVector(this.importantSpeechActReplied, email.getSpeechActVector());
				
				this.importantNLPKeywordsReplied = VectorsMath.getMergedVector(this.importantNLPKeywordsReplied, email.getNlpKeywordsVector());
				numberOfDirectEmailsReplied++;
				this.getReputationDataModel().getRepliedEmails().add(email);
			}
			else if(email.isSeen()){
				logger.info("this is a direct email seen");
				
				this.importantTopicsOnlySeen = VectorsMath.getMergedVector(this.importantTopicsOnlySeen, email.getTextContextVector());
				this.importantPeopleOnlySeen = VectorsMath.getMergedVector(this.importantPeopleOnlySeen, email.getRecipientContextVector());
				this.importantSpeechActOnlySeen = VectorsMath.getMergedVector(this.importantSpeechActOnlySeen, email.getSpeechActVector());
				
				this.importantNLPKeywordsOnlySeen = VectorsMath.getMergedVector(this.importantNLPKeywordsOnlySeen, email.getNlpKeywordsVector());
				numberOfDirectEmailsSeen++;
				this.getReputationDataModel().getSeenEmails().add(email);
			}
			if(email.isFlagged()){
				logger.info("this is a direct email flagged");
				
				this.importantTopicsFlagged = VectorsMath.getMergedVector(this.importantTopicsFlagged, email.getTextContextVector());
				this.importantPeopleFlagged = VectorsMath.getMergedVector(this.importantPeopleFlagged, email.getRecipientContextVector());
				this.importantSpeechActFlagged = VectorsMath.getMergedVector(this.importantSpeechActFlagged, email.getSpeechActVector());
				
				this.importantNLPKeywordsFlagged = VectorsMath.getMergedVector(this.importantNLPKeywordsFlagged, email.getNlpKeywordsVector());
				numberOfDirectEmailsFlagged++;
				this.getReputationDataModel().getFlaggedEmails().add(email);
			}
		}
			//unimportant models
			if( email.isSpam() || email.isDeleted()){
				logger.info("this is a spam email recognized by flag or header");
				
				this.unimportanttopicsVector = VectorsMath.getMergedVector(this.unimportanttopicsVector, email.getTextContextVector());
				this.unimportantPeopleVector = VectorsMath.getMergedVector(this.unimportantPeopleVector, email.getRecipientContextVector());
				this.unimportantSpeechActVector = VectorsMath.getMergedVector(this.unimportantSpeechActVector, email.getSpeechActVector());
				
				this.unimportantDirectKeywordVector = VectorsMath.getMergedVector(this.unimportantDirectKeywordVector, email.getNlpKeywordsVector());
				nofOfUnimportantEmails++;
				this.getReputationDataModel().getSpamEmails().add(email);
			}
	}
	
	public Email predictImportanceFormEmail(Email newEmail){
		newEmail.setModel(false);
		newEmail.setPredicted(true);
		
		if(newEmail.isSpam()){
			logger.info("The email to recommend reputation is pre-labeled as spam..");
			double spamTopicSimilarity = Similarity.cosineSimilarity(unimportanttopicsVector, newEmail.getTextContextVector());
			double spamPeopleSimilarity = Similarity.cosineSimilarity(unimportantPeopleVector, newEmail.getRecipientContextVector());
			double spamSpeechActSimilarity = Similarity.cosineSimilarity(unimportantSpeechActVector, newEmail.getSpeechActVector());
			 
			double spamNLPKeywordSimilarity = Similarity.cosineSimilarity(unimportantDirectKeywordVector, newEmail.getNlpKeywordsVector());
			
			logger.info("The similarity of the email with SPAM emails in the model \n:"
			+" spam topic similarity : " + spamTopicSimilarity 
			+ " spam NLP keyword similarity : " + spamNLPKeywordSimilarity 			
			+ " spam people similarity : " + spamPeopleSimilarity 
			+ " spam speech act similarity : " + spamSpeechActSimilarity);
			
		}
		if(newEmail.isListMail() && !newEmail.isDirect() && !newEmail.isCCd()){
			logger.info("The email is not a direct email, calculating similarities with list email profiles");
			double[] importantListFlaggedTopics = this.getImportantListTopicsFlagged();
			double[] importantListRepliedTopics = this.getImportantListTopicsReplied();
			double[] importantListSeenTopics = this.getImportantListTopicsOnlySeen();
			
			double flaggedTopicscore = Similarity.cosineSimilarity(importantListFlaggedTopics, newEmail.getTextContextVector());
			double repliedTopicscore = Similarity.cosineSimilarity(importantListRepliedTopics, newEmail.getTextContextVector());
			double seenTopicscore = Similarity.cosineSimilarity(importantListSeenTopics, newEmail.getTextContextVector());
			double spamTopicSimilarity = Similarity.cosineSimilarity(unimportanttopicsVector, newEmail.getTextContextVector());
			double totalTopicScore = flaggedTopicscore + repliedTopicscore + seenTopicscore - spamTopicSimilarity;			
			
			logger.info("flagged list topic score : " + flaggedTopicscore);
			logger.info("replied list topic score : " + repliedTopicscore);
			logger.info("seen list topic score : " + seenTopicscore);
			logger.info("Spam list topic score : " + spamTopicSimilarity);
			logger.info("Total list topic score " + totalTopicScore);
			newEmail.setFlaggedTopicscore(flaggedTopicscore);
			newEmail.setRepliedTopicscore(repliedTopicscore);
			newEmail.setSeenTopicscore(seenTopicscore);
			newEmail.setSpamTopicScore(spamTopicSimilarity);
			newEmail.setTotalTopicScore(totalTopicScore);
			
			
			
			double flaggedKeywordsScore = Similarity.cosineSimilarity(importantListNLPKeywordsFlagged, newEmail.getNlpKeywordsVector());
			double repliedKeywordsScore = Similarity.cosineSimilarity(importantListNLPKeywordsReplied, newEmail.getNlpKeywordsVector());
			double seenKeywordsScore = Similarity.cosineSimilarity(importantListNLPKeywordsOnlySeen, newEmail.getNlpKeywordsVector());
			double spamKeywordScore = Similarity.cosineSimilarity(unimportantDirectKeywordVector, newEmail.getNlpKeywordsVector());
			double totalListKeywordScore = flaggedKeywordsScore + repliedKeywordsScore + seenKeywordsScore - spamKeywordScore;		
			
			logger.info("flagged list keyword score : " + flaggedKeywordsScore);
			logger.info("replied list keyword score : " + repliedKeywordsScore);
			logger.info("seen list keyword score : " + seenKeywordsScore);
			logger.info("Spam list keyword score : " + spamKeywordScore);
			logger.info("Total list keyword score " + totalListKeywordScore);
			newEmail.setFlaggedKeywordscore(flaggedKeywordsScore);
			newEmail.setRepliedKeywordscore(repliedKeywordsScore);
			newEmail.setSeenKeywordscore(seenKeywordsScore);
			newEmail.setSpamKeywordscore(spamKeywordScore);
			newEmail.setTotalKeywordscore(totalListKeywordScore);
			
			
			
			double[] importantListFlaggedPeople = this.getImportantListPeopleFlagged();
			double[] importantListRepliedPeople = this.getImportantListPeopleReplied();
			double[] importantListSeenPeople = this.getImportantListPeopleOnlySeen();
			
			double flaggedPeoplescore = Similarity.cosineSimilarity(importantListFlaggedPeople, newEmail.getRecipientContextVector());
			double repliedPeoplescore = Similarity.cosineSimilarity(importantListRepliedPeople, newEmail.getRecipientContextVector());
			double seenPeoplescore = Similarity.cosineSimilarity(importantListSeenPeople, newEmail.getRecipientContextVector());
			double spamPeopleSimilarity = Similarity.cosineSimilarity(unimportantPeopleVector, newEmail.getRecipientContextVector());
			
			double totalPeopleScore = flaggedPeoplescore + repliedPeoplescore + seenPeoplescore - spamPeopleSimilarity;
			
			logger.info("flagged list people score : " + flaggedPeoplescore);
			logger.info("replied list people score : " + repliedPeoplescore);
			logger.info("seen list people score : " + seenPeoplescore);
			logger.info("Spam list people score : " + spamPeopleSimilarity);
			
			logger.info("Total list people score : " + totalPeopleScore);
			newEmail.setFlaggedPeoplescore(flaggedPeoplescore);
			newEmail.setRepliedPeoplescore(repliedPeoplescore);
			newEmail.setSeenPeoplescore(seenPeoplescore);
			newEmail.setSpamPeopleScore(spamPeopleSimilarity);
			newEmail.setTotalPeopleScore(totalPeopleScore);
			
			
			double[] importantListSpeechActsFlagged = this.getImportantListSpeechActFlagged();
			double[] importantListRepliedSpeechActs = this.getImportantListSpeechActReplied();
			double[] importantListSeenSpeechActs = this.getImportantListSpeechActOnlySeen();
			
			double flaggedSAScore = Similarity.cosineSimilarity(importantListSpeechActsFlagged, newEmail.getSpeechActVector());
			double repliedSAScore = Similarity.cosineSimilarity(importantListRepliedSpeechActs, newEmail.getSpeechActVector());
			double seenSAScore = Similarity.cosineSimilarity(importantListSeenSpeechActs, newEmail.getSpeechActVector());
			double spamSpeechActSimilarity = Similarity.cosineSimilarity(unimportantSpeechActVector, newEmail.getSpeechActVector());
			
			double totalSAScore = flaggedSAScore + repliedSAScore + seenSAScore - spamSpeechActSimilarity;
			
			
			logger.info("flagged list SA score : " + flaggedSAScore);
			logger.info("replied list SA score : " + repliedSAScore);
			logger.info("seen list SA score : " + seenSAScore);
			logger.info("Spam list people score : " + spamSpeechActSimilarity);
			logger.info("Total list SA score : " + totalSAScore);				
			newEmail.setFlaggedSpeechActscore(totalSAScore);
			newEmail.setRepliedSpeechActscore(repliedSAScore);
			newEmail.setSeenSpeechActscore(seenSAScore);
			newEmail.setTotalSpeechActscore(totalSAScore);
			
			
		}else{
			logger.info("The email is a direct or ccd email and not a list email");
			double[] importantFlaggedTopics = this.getImportantTopicsFlagged();
			double[] importantRepliedTopics = this.getImportantTopicsReplied();
			double[] importantSeenTopics = this.getImportantTopicsOnlySeen();
			
			double flaggedTopicscore = Similarity.cosineSimilarity(importantFlaggedTopics, newEmail.getTextContextVector());
			double repliedTopicscore = Similarity.cosineSimilarity(importantRepliedTopics, newEmail.getTextContextVector());
			double seenTopicscore = Similarity.cosineSimilarity(importantSeenTopics, newEmail.getTextContextVector());
			double spamTopicSimilarity = Similarity.cosineSimilarity(unimportanttopicsVector, newEmail.getTextContextVector());
			
			double totalTopicScore = flaggedTopicscore + repliedTopicscore + seenTopicscore - spamTopicSimilarity;
			
			logger.info("flagged topic score : " + flaggedTopicscore);
			logger.info("replied topic score : " + repliedTopicscore);
			logger.info("seen topic score : " + seenTopicscore);
			logger.info("Total topic score " + totalTopicScore);
			newEmail.setFlaggedTopicscore(flaggedTopicscore);
			newEmail.setRepliedTopicscore(repliedTopicscore);
			newEmail.setSeenTopicscore(seenTopicscore);
			newEmail.setSpamTopicScore(spamTopicSimilarity);
			newEmail.setTotalTopicScore(totalTopicScore);
			
			
			double flaggedKeywordsScore = Similarity.cosineSimilarity(importantNLPKeywordsFlagged, newEmail.getNlpKeywordsVector());
			double repliedKeywordsScore = Similarity.cosineSimilarity(importantNLPKeywordsReplied, newEmail.getNlpKeywordsVector());
			double seenKeywordsScore = Similarity.cosineSimilarity(importantNLPKeywordsOnlySeen, newEmail.getNlpKeywordsVector());
			double spamKeywordScore = Similarity.cosineSimilarity(unimportantDirectKeywordVector, newEmail.getNlpKeywordsVector());
			double totalKeywordScore = flaggedKeywordsScore + repliedKeywordsScore + seenKeywordsScore - spamKeywordScore;			
			
			logger.info("flagged keyword score : " + flaggedKeywordsScore);
			logger.info("replied keyword score : " + repliedKeywordsScore);
			logger.info("seen keyword score : " + seenKeywordsScore);
			logger.info("Spam keyword score : " + spamKeywordScore);
			logger.info("Total keyword score " + totalKeywordScore);
			newEmail.setFlaggedKeywordscore(flaggedKeywordsScore);
			newEmail.setRepliedKeywordscore(repliedKeywordsScore);
			newEmail.setSeenKeywordscore(seenKeywordsScore);
			newEmail.setSpamKeywordscore(spamKeywordScore);
			newEmail.setTotalKeywordscore(totalKeywordScore);
			
			
			double[] importantFlaggedPeople = this.getImportantPeopleFlagged();
			double[] importantRepliedPeople = this.getImportantPeopleReplied();
			double[] importantSeenPeople = this.getImportantPeopleOnlySeen();
			
			double flaggedPeoplescore = Similarity.cosineSimilarity(importantFlaggedPeople, newEmail.getRecipientContextVector());
			double repliedPeoplescore = Similarity.cosineSimilarity(importantRepliedPeople, newEmail.getRecipientContextVector());
			double seenPeoplescore = Similarity.cosineSimilarity(importantSeenPeople, newEmail.getRecipientContextVector());
			double spamPeopleSimilarity = Similarity.cosineSimilarity(unimportantPeopleVector, newEmail.getRecipientContextVector());
			
			double totalPeopleScore = flaggedPeoplescore + repliedPeoplescore + seenPeoplescore - spamPeopleSimilarity;
			logger.info("flagged people score : " + flaggedPeoplescore);
			logger.info("replied people score : " + repliedPeoplescore);
			logger.info("seen people score : " + seenPeoplescore);
			logger.info("Total people score : " + totalPeopleScore);
			newEmail.setFlaggedPeoplescore(flaggedPeoplescore);
			newEmail.setRepliedPeoplescore(repliedPeoplescore);
			newEmail.setSeenPeoplescore(seenPeoplescore);
			newEmail.setSpamPeopleScore(spamPeopleSimilarity);
			newEmail.setTotalPeopleScore(totalPeopleScore);
			
			double[] importantSpeechActsFlagged = this.getImportantSpeechActFlagged();
			double[] importantRepliedSpeechActs = this.getImportantSpeechActReplied();
			double[] importantSeenSpeechActs = this.getImportantSpeechActOnlySeen();
			
			double flaggedSAScore = Similarity.cosineSimilarity(importantSpeechActsFlagged, newEmail.getSpeechActVector());
			double repliedSAScore = Similarity.cosineSimilarity(importantRepliedSpeechActs, newEmail.getSpeechActVector());
			double seenSAScore = Similarity.cosineSimilarity(importantSeenSpeechActs, newEmail.getSpeechActVector());
			double spamSpeechActSimilarity = Similarity.cosineSimilarity(unimportantSpeechActVector, newEmail.getSpeechActVector());
			
			double totalSAScore = flaggedSAScore + repliedSAScore + seenSAScore - spamSpeechActSimilarity;
			logger.info("flagged SA score : " + flaggedSAScore);
			logger.info("replied SA score : " + repliedSAScore);
			logger.info("seen SA score : " + seenSAScore);
			logger.info("Total SA score : " + totalSAScore);				
			newEmail.setFlaggedSpeechActscore(totalSAScore);
			newEmail.setRepliedSpeechActscore(repliedSAScore);
			newEmail.setSeenSpeechActscore(seenSAScore);
			newEmail.setTotalSpeechActscore(totalSAScore);
		}	
		return newEmail;
	}

	public Email classifyEmailBasedOnContentAndRecipients(Email email){
		EmailReputationDataModel model = this.getReputationDataModel();
		// processing reputationScores for new email
		if (model == null) {
			// creating a new model with the email in new content and recipient
			// clusters
			logger.info("since there is no datamodel for this mailbox creating a new one...");
			model = new EmailReputationDataModel();
			List<EmailContentCluster> contentClusters = new ArrayList<EmailContentCluster>();
			List<EmailRecipientCluster> recipientClusters = new ArrayList<EmailRecipientCluster>();
			/*
			 * List<Email> contentClusterEmails = new ArrayList<Email>();
			 * List<Email> recipientClusterEmails = new ArrayList<Email>();
			 * List<String> contentEmailIds = new ArrayList<String>();
			 * List<String> recipientEmailIds = new ArrayList<String>();
			 */

			EmailContentCluster contentCluster = new EmailContentCluster("c"
					+ String.valueOf(0));
			contentCluster.addEmail(email.getMessageId(), email);
			contentClusters.add(contentCluster);
			model.setContentClusters(contentClusters);
			logger.info("Created new content cluster with id : "
					+ contentCluster.getId() + " and added email : "
					+ email.getMessageId());

			EmailRecipientCluster recipientCluster = new EmailRecipientCluster(
					"p" + String.valueOf(0));
			recipientCluster.addEmail(email.getMessageId(), email);
			recipientClusters.add(recipientCluster);
			model.setRecipientClusters(recipientClusters);

			logger.info("Created new recipient cluster with id : "
					+ recipientCluster.getId() + " and added email : "
					+ email.getMessageId());
			email.setTextClusterId(contentCluster.getId());
			email.setPeopleClusterId(recipientCluster.getId());
			this.setReputationDataModel(model);
		} else {
			
			logger.info(" Classifying email using the existing reputation data model...");
			List<EmailContentCluster> newContentClusters = KMeansClustering
					.classifyNewEmailByContent(email,
							model.getContentClusters());
			List<EmailRecipientCluster> newRecipientClusters = KMeansClustering
					.classifyNewEmailByPeople(email,
							model.getRecipientClusters());

			logger.info("ReputationScores of added email : "
					+ email.getMessageId() + " content cluster id :"
					+ email.getTextClusterId() + " contentReputation : "
					+ email.getContentReputationScore()
					+ " people cluster id : " + email.getPeopleClusterId()
					+ " peopleReputation : "
					+ email.getRecipientReputationScore());
			// logger.info("Email was added to content cluster :" +
			// email.getTextClusterId() + " , recipient cluster : " +
			// email.getPeopleClusterId());
			model.setContentClusters(newContentClusters);
			model.setRecipientClusters(newRecipientClusters);
			this.setReputationDataModel(model);
			logger.info("After classifying the new email, the size of the mailbox's content clusters : "
					+ model.getContentClusters().size());
			logger.info("After classifying the new email, the size of the mailbox's recipient clusters : "
					+ model.getRecipientClusters().size());

		}

		return email;
	}

	private void processEmailContact(String contactEmailAddress, Email email,
			boolean isFrom, boolean isCC, boolean isTo) {
		// check if the email address is the user's email address
		if (contactEmailAddress != this.getEmailId()) {
			boolean foundContact = false;
			EmailContact emailContact = null;
			for (EmailContact contact : allEmailContacts) {
				if (contact.getPrimaryEmailAddress().equalsIgnoreCase(
						contactEmailAddress)) {
					emailContact = contact;
					if (email.isSeen()) {
						emailContact.addMessageSeen();
					}
					if (email.isAnswered()) {
						emailContact.addMessageAnswered();
					}
					if (email.isFlagged()) {
						emailContact.addMessageFlagged();
					}
					if (email.isDeleted()) {
						emailContact.addMessageDeleted();
					}
					
					//process if the message was to,cc,from the contact
					if(isFrom) {
						emailContact.addMessageFrom(email.getMessageId());
					} else if(isTo) {
						emailContact.addMessageTo(email.getMessageId());
					} else if(isCC) {
						emailContact.addMessageCC(email.getMessageId());
					}

					
					foundContact = true;
					break;
				}
			}
			// not yet added, needs to add the new contact
			if (!foundContact) {
				emailContact = new EmailContact();
				emailContact.setPrimaryEmailAddress(contactEmailAddress);
				emailContact.addEmailAddress(new EmailAddress(
						contactEmailAddress));
				
				if (email.isSeen()) {
					emailContact.addMessageSeen();
				}
				if (email.isAnswered()) {
					emailContact.addMessageAnswered();
				}
				if (email.isFlagged()) {
					emailContact.addMessageFlagged();
				}
				if (email.isDeleted()) {
					emailContact.addMessageDeleted();
				}
				
				//process if the message was to,cc,from the contact
				if(isFrom) {
					emailContact.addMessageFrom(email.getMessageId());
				} else if(isTo) {
					emailContact.addMessageTo(email.getMessageId());
				} else if(isCC) {
					emailContact.addMessageCC(email.getMessageId());
				}
				allEmailContacts.add(emailContact);
			}

		}
	}

	/**
	 * adding new words to the mailbox's global word freq. matrix &
	 * wordMentionedEmail matrix for logIDF calculation purposes
	 * 
	 * @param emailId
	 * @param words
	 *            tokens from the email
	 */
	/*
	 * public void addWords(String emailId, Map<String, Integer> words){
	 * for(String word: words.keySet()){ int newWordCount =
	 * words.get(word).intValue(); if(this.allWordCounts.containsKey(word)){
	 * Integer count = this.allWordCounts.get(word); int newCount =
	 * count.intValue() + newWordCount; this.allWordCounts.put(word, newCount);
	 * } else { this.allWordCounts.put(word,newWordCount); }
	 * 
	 * //adding to wordMentionedEmailMap
	 * if(this.wordMentionedEmailMap.containsKey(word)){ List<String> emailIds =
	 * this.wordMentionedEmailMap.get(word); emailIds.add(emailId);
	 * this.wordMentionedEmailMap.put(word, emailIds); } else { List<String>
	 * newWordforEmails = new ArrayList<String>();
	 * newWordforEmails.add(emailId); wordMentionedEmailMap.put(word,
	 * newWordforEmails); } } }
	 */
	// endregion

	// region > allEmailContacts (collection)
	// contactEmailId: emailContact object
	private List<EmailContact> allEmailContacts;

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
	@Hidden
	public boolean isSyncing() {
		return isSyncing;
	}

	public void setSyncing(boolean isSyncing) {
		this.isSyncing = isSyncing;
	}

	/**
	 * A mapping from each word to its associated index vector
	 *//*
	private Map<String, TernaryVector> wordToIndexVector;

	@javax.jdo.annotations.Column(allowsNull = "true")
	public Map<String, TernaryVector> getWordToIndexVector() {
		return wordToIndexVector;
		//modified to support data persistence..
		//return this.contentIndexVectorMap.getIndexVectorMap();
	}

	public void setWordToIndexVector(
			Map<String, TernaryVector> wordToIndexVector) {
		this.wordToIndexVector = wordToIndexVector;
		//modified to support data persistence..
		//this.contentIndexVectorMap.setIndexVectorMap(wordToIndexVector);;
	}*/

	/**
	 * A mapping from each word to it's context vector
	 *//*
	private Map<String, double[]> wordToMeaningMap;

	public void setWordToMeaningMap(Map<String, double[]> wordToMeaning) {
		this.wordToMeaningMap = wordToMeaning;
		//modified to support data persistence..
		//this.contentContextVectorMap.setContextVectorMap(wordToMeaning);
	}

	@javax.jdo.annotations.Column(allowsNull = "true")
	public Map<String, double[]> getWordToMeaningMap() {
		return wordToMeaningMap;
		//modified to support data persistence..
		//return this.contentContextVectorMap.getContextVectorMap();
	}*/

	/*private Map<String, Integer> allWordCounts;
	// word: List of emailIds of emails where the word is mentioned
	// private Map<String,List<String>> wordMentionedEmailMap;

	*//**
	 * A mapping from each recipient to an index vector
	 *//*
	private Map<String, TernaryVector> recipientToIndexVector;

	@javax.jdo.annotations.Column(allowsNull = "true")
	public Map<String, TernaryVector> getRecipientToIndexVector() {
		return recipientToIndexVector;
		//modified to support data persistence..
		//return this.recipientIndexVectorMap.getIndexVectorMap();
	}

	public void setRecipientToIndexVector(
			Map<String, TernaryVector> recipientToIndexVector) {
		this.recipientToIndexVector = recipientToIndexVector;
		//modified to support data persistence..
		//this.recipientIndexVectorMap.setIndexVectorMap(recipientToIndexVector);
	}
*/
	/**
	 * A mapping from each recipient to it's context vector
	 *//*
	private Map<String, double[]> recipientToMeaningMap;

	public void setRecipientToMeaningMap(
			Map<String, double[]> recipientToMeaning) {
		this.recipientToMeaningMap = recipientToMeaning;
		//modified to support data persistence..
		//this.recipientContextVectorMap.setContextVectorMap(recipientToMeaning);
	}

	@javax.jdo.annotations.Column(allowsNull = "true")
	public Map<String, double[]> getRecipientToMeaningMap() {
		return recipientToMeaningMap;
		//modified to support data persistence..
		//return this.recipientContextVectorMap.getContextVectorMap();
	}*/

	/*@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public Map<String, Integer> getAllWordCounts() {
		return allWordCounts;
	}

	public void setAllWordCounts(Map<String, Integer> allWordCounts) {
		this.allWordCounts = allWordCounts;
	}*/

	/*
	 * @javax.jdo.annotations.Persistent
	 * 
	 * @javax.jdo.annotations.Column(allowsNull = "true") public
	 * Map<String,List<String>> getWordMentionedEmailMap() { return
	 * wordMentionedEmailMap; }
	 * 
	 * 
	 * public void setWordMentionedEmailMap(Map<String,List<String>>
	 * wordMentionedEmailMap) { this.wordMentionedEmailMap =
	 * wordMentionedEmailMap; }
	 */

	private long mailBoxAddedDateTimeStamp;

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "false")
	@Hidden
	public long getMailBoxAddedDateTimeStamp() {
		return mailBoxAddedDateTimeStamp;
	}

	public void setMailBoxAddedDateTimeStamp(long mailBoxAddedDateTimeStamp) {
		this.mailBoxAddedDateTimeStamp = mailBoxAddedDateTimeStamp;
	}
	
	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true") 
	@Hidden
	public EmailReputationDataModel getReputationDataModel() {
		return reputationDataModel;
	}

	public void setReputationDataModel(EmailReputationDataModel reputationDataModel) {
		this.reputationDataModel = reputationDataModel;
	}

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true") 
	@Hidden
	public IndexVectorMap getContentIndexVectorMap() {
		return contentIndexVectorMap;
	}

	public void setContentIndexVectorMap(IndexVectorMap contentIndexVectorMap) {
		this.contentIndexVectorMap = contentIndexVectorMap;
	}

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true") 
	@Hidden
	public ContextVectorMap getContentContextVectorMap() {
		return contentContextVectorMap;
	}

	public void setContentContextVectorMap(ContextVectorMap contentContextVectorMap) {
		this.contentContextVectorMap = contentContextVectorMap;
	}

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true") 
	@Hidden
	public IndexVectorMap getRecipientIndexVectorMap() {
		return recipientIndexVectorMap;
	}

	public void setRecipientIndexVectorMap(IndexVectorMap recipientIndexVectorMap) {
		this.recipientIndexVectorMap = recipientIndexVectorMap;
	}

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true") 
	@Hidden
	public ContextVectorMap getRecipientContextVectorMap() {
		return recipientContextVectorMap;
	}

	public void setRecipientContextVectorMap(
			ContextVectorMap recipientContextVectorMap) {
		this.recipientContextVectorMap = recipientContextVectorMap;
	}

	@javax.jdo.annotations.Persistent
	public long getLastIndexedMsgUid() {
		return lastIndexedMsgUid;
	}

	public void setLastIndexedMsgUid(long lastIndexedMsgUid) {
		this.lastIndexedMsgUid = lastIndexedMsgUid;
	}

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true") 
	@Hidden
	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true") 
	public String getImapHostId() {
		return imapHostId;
	}

	public void setImapHostId(String imapHostId) {
		this.imapHostId = imapHostId;
	}

	@javax.jdo.annotations.Column(allowsNull= "true" )
	@Hidden
	public double[] getImportantTopicsReplied() {
		return importantTopicsReplied;
	}

	public void setImportantTopicsReplied(double[] importantTopicsReplied) {
		this.importantTopicsReplied = importantTopicsReplied;
	}

	@javax.jdo.annotations.Column(allowsNull= "true" )
	@Hidden
	public double[] getImportantTopicsOnlySeen() {
		return importantTopicsOnlySeen;
	}

	public void setImportantTopicsOnlySeen(double[] importantTopicsOnlySeen) {
		this.importantTopicsOnlySeen = importantTopicsOnlySeen;
	}

	@javax.jdo.annotations.Column(allowsNull= "true" )
	@Hidden
	public double[] getImportantTopicsFlagged() {
		return importantTopicsFlagged;
	}

	public void setImportantTopicsFlagged(double[] importantTopicsFlagged) {
		this.importantTopicsFlagged = importantTopicsFlagged;
	}

	@javax.jdo.annotations.Column(allowsNull= "true" )
	@Hidden
	public double[] getImportantPeopleReplied() {
		return importantPeopleReplied;
	}

	public void setImportantPeopleReplied(double[] importantPeopleReplied) {
		this.importantPeopleReplied = importantPeopleReplied;
	}

	@javax.jdo.annotations.Column(allowsNull= "true" )
	@Hidden
	public double[] getImportantPeopleOnlySeen() {
		return importantPeopleOnlySeen;
	}

	public void setImportantPeopleOnlySeen(double[] importantPeopleOnlySeen) {
		this.importantPeopleOnlySeen = importantPeopleOnlySeen;
	}

	@javax.jdo.annotations.Column(allowsNull= "true" )
	@Hidden
	public double[] getImportantPeopleFlagged() {
		return importantPeopleFlagged;
	}

	public void setImportantPeopleFlagged(double[] importantPeopleFlagged) {
		this.importantPeopleFlagged = importantPeopleFlagged;
	}

	@javax.jdo.annotations.Column(allowsNull= "true" )
	@Hidden
	public double[] getImportantSpeechActReplied() {
		return importantSpeechActReplied;
	}

	public void setImportantSpeechActReplied(double[] importantSpeechActReplied) {
		this.importantSpeechActReplied = importantSpeechActReplied;
	}

	@javax.jdo.annotations.Column(allowsNull= "true" )
	@Hidden
	public double[] getImportantSpeechActOnlySeen() {
		return importantSpeechActOnlySeen;
	}

	public void setImportantSpeechActOnlySeen(double[] importantSpeechActOnlySeen) {
		this.importantSpeechActOnlySeen = importantSpeechActOnlySeen;
	}
	
	@javax.jdo.annotations.Column(allowsNull= "true" )
	@Hidden
	public double[] getImportantSpeechActFlagged() {
		return importantSpeechActFlagged;
	}

	public void setImportantSpeechActFlagged(double[] importantSpeechActFlagged) {
		this.importantSpeechActFlagged = importantSpeechActFlagged;
	}

	@javax.jdo.annotations.Column(allowsNull= "true" )
	@Hidden
	public double[] getUnimportanttopicsVector() {
		return unimportanttopicsVector;
	}

	public void setUnimportanttopicsVector(double[] unimportanttopicsVector) {
		this.unimportanttopicsVector = unimportanttopicsVector;
	}
	
	@javax.jdo.annotations.Column(allowsNull= "true" )
	@Hidden
	public double[] getUnimportantPeopleVector() {
		return unimportantPeopleVector;
	}

	public void setUnimportantPeopleVector(double[] unimportantPeopleVector) {
		this.unimportantPeopleVector = unimportantPeopleVector;
	}

	@javax.jdo.annotations.Column(allowsNull= "true" )
	@Hidden
	public double[] getUnimportantSpeechActVector() {
		return unimportantSpeechActVector;
	}

	public void setUnimportantSpeechActVector(double[] unimportantSpeechActVector) {
		this.unimportantSpeechActVector = unimportantSpeechActVector;
	}

	

	@Hidden
	public int getNofOfUnimportantEmails() {
		return nofOfUnimportantEmails;
	}

	public void setNofOfUnimportantEmails(int nofOfUnimportantEmails) {
		this.nofOfUnimportantEmails = nofOfUnimportantEmails;
	}
	
	@javax.jdo.annotations.Column(allowsNull= "true" )
	@Hidden
	public double[] getImportantListTopicsReplied() {
		return importantListTopicsReplied;
	}

	public void setImportantListTopicsReplied(double[] importantListTopicsReplied) {
		this.importantListTopicsReplied = importantListTopicsReplied;
	}

	@javax.jdo.annotations.Column(allowsNull= "true" )
	@Hidden
	public double[] getImportantListTopicsOnlySeen() {
		return importantListTopicsOnlySeen;
	}

	public void setImportantListTopicsOnlySeen(double[] importantListTopicsOnlySeen) {
		this.importantListTopicsOnlySeen = importantListTopicsOnlySeen;
	}

	@javax.jdo.annotations.Column(allowsNull= "true" )
	@Hidden
	public double[] getImportantListTopicsFlagged() {
		return importantListTopicsFlagged;
	}

	public void setImportantListTopicsFlagged(double[] importantListTopicsFlagged) {
		this.importantListTopicsFlagged = importantListTopicsFlagged;
	}

	@javax.jdo.annotations.Column(allowsNull= "true" )
	@Hidden
	public double[] getImportantListPeopleReplied() {
		return importantListPeopleReplied;
	}

	public void setImportantListPeopleReplied(double[] importantListPeopleReplied) {
		this.importantListPeopleReplied = importantListPeopleReplied;
	}
	
	@javax.jdo.annotations.Column(allowsNull= "true" )
	@Hidden
	public double[] getImportantListPeopleOnlySeen() {
		return importantListPeopleOnlySeen;
	}

	public void setImportantListPeopleOnlySeen(double[] importantListPeopleOnlySeen) {
		this.importantListPeopleOnlySeen = importantListPeopleOnlySeen;
	}
	
	@javax.jdo.annotations.Column(allowsNull= "true" )
	@Hidden
	public double[] getImportantListPeopleFlagged() {
		return importantListPeopleFlagged;
	}

	public void setImportantListPeopleFlagged(double[] importantListPeopleFlagged) {
		this.importantListPeopleFlagged = importantListPeopleFlagged;
	}

	@javax.jdo.annotations.Column(allowsNull= "true" )
	@Hidden
	public double[] getImportantListSpeechActReplied() {
		return importantListSpeechActReplied;
	}

	public void setImportantListSpeechActReplied(
			double[] importantListSpeechActReplied) {
		this.importantListSpeechActReplied = importantListSpeechActReplied;
	}

	@javax.jdo.annotations.Column(allowsNull= "true" )
	@Hidden
	public double[] getImportantListSpeechActOnlySeen() {
		return importantListSpeechActOnlySeen;
	}

	public void setImportantListSpeechActOnlySeen(
			double[] importantListSpeechActOnlySeen) {
		this.importantListSpeechActOnlySeen = importantListSpeechActOnlySeen;
	}

	@javax.jdo.annotations.Column(allowsNull= "true" )
	@Hidden
	public double[] getImportantListSpeechActFlagged() {
		return importantListSpeechActFlagged;
	}

	public void setImportantListSpeechActFlagged(
			double[] importantListSpeechActFlagged) {
		this.importantListSpeechActFlagged = importantListSpeechActFlagged;
	}

	@Hidden
	public int getNumberOfListEmails() {
		return numberOfListEmails;
	}

	public void setNumberOfListEmails(int numberOfListEmails) {
		this.numberOfListEmails = numberOfListEmails;
	}

	@Hidden
	public int getNumberOfDirectEmails() {
		return numberOfDirectEmails;
	}

	public void setNumberOfDirectEmails(int numberOfDirectEmails) {
		this.numberOfDirectEmails = numberOfDirectEmails;
	}

	@Hidden
	public int getNumberOfListEmailsReplied() {
		return numberOfListEmailsReplied;
	}

	public void setNumberOfListEmailsReplied(int numberOfListEmailsReplied) {
		this.numberOfListEmailsReplied = numberOfListEmailsReplied;
	}

	@Hidden
	public int getNumberOfListEmailsFlagged() {
		return numberOfListEmailsFlagged;
	}

	public void setNumberOfListEmailsFlagged(int numberOfListEmailsFlagged) {
		this.numberOfListEmailsFlagged = numberOfListEmailsFlagged;
	}

	@Hidden
	public int getNumberOfListEmailsSeen() {
		return numberOfListEmailsSeen;
	}

	public void setNumberOfListEmailsSeen(int numberOfListEmailsSeen) {
		this.numberOfListEmailsSeen = numberOfListEmailsSeen;
	}
	
	@Hidden
	public int getNumberOfDirectEmailsReplied() {
		return numberOfDirectEmailsReplied;
	}

	public void setNumberOfDirectEmailsReplied(int numberOfDirectEmailsReplied) {
		this.numberOfDirectEmailsReplied = numberOfDirectEmailsReplied;
	}

	@Hidden
	public int getNumberOfDirectEmailsFlagged() {
		return numberOfDirectEmailsFlagged;
	}

	public void setNumberOfDirectEmailsFlagged(int numberOfDirectEmailsFlagged) {
		this.numberOfDirectEmailsFlagged = numberOfDirectEmailsFlagged;
	}

	@Hidden
	public int getNumberOfDirectEmailsSeen() {
		return numberOfDirectEmailsSeen;
	}

	public void setNumberOfDirectEmailsSeen(int numberOfDirectEmailsSeen) {
		this.numberOfDirectEmailsSeen = numberOfDirectEmailsSeen;
	}

	public boolean isUpdatingModel() {
		return isUpdatingModel;
	}

	public void setUpdatingModel(boolean isUpdatingModel) {
		this.isUpdatingModel = isUpdatingModel;
	}

	@javax.jdo.annotations.Column(allowsNull= "true" )
	@Hidden
	public double[] getImportantNLPKeywordsReplied() {
		return importantNLPKeywordsReplied;
	}

	public void setImportantNLPKeywordsReplied(
			double[] importantNLPKeywordsReplied) {
		this.importantNLPKeywordsReplied = importantNLPKeywordsReplied;
	}
	
	@javax.jdo.annotations.Column(allowsNull= "true" )
	@Hidden
	public double[] getImportantNLPKeywordsOnlySeen() {
		return importantNLPKeywordsOnlySeen;
	}

	@javax.jdo.annotations.Column(allowsNull= "true" )
	@Hidden
	public double[] getImportantNLPKeywordsFlagged() {
		return importantNLPKeywordsFlagged;
	}

	@javax.jdo.annotations.Column(allowsNull= "true" )
	@Hidden
	public double[] getImportantListNLPKeywordsReplied() {
		return importantListNLPKeywordsReplied;
	}

	@javax.jdo.annotations.Column(allowsNull= "true" )
	@Hidden
	public double[] getImportantListNLPKeywordsOnlySeen() {
		return importantListNLPKeywordsOnlySeen;
	}
	
	@javax.jdo.annotations.Column(allowsNull= "true" )
	@Hidden
	public double[] getImportantListNLPKeywordsFlagged() {
		return importantListNLPKeywordsFlagged;
	}

	public void setImportantNLPKeywordsOnlySeen(
			double[] importantNLPKeywordsOnlySeen) {
		this.importantNLPKeywordsOnlySeen = importantNLPKeywordsOnlySeen;
	}

	public void setImportantNLPKeywordsFlagged(double[] importantNLPKeywordsFlagged) {
		this.importantNLPKeywordsFlagged = importantNLPKeywordsFlagged;
	}

	public void setImportantListNLPKeywordsReplied(
			double[] importantListNLPKeywordsReplied) {
		this.importantListNLPKeywordsReplied = importantListNLPKeywordsReplied;
	}

	public void setImportantListNLPKeywordsOnlySeen(
			double[] importantListNLPKeywordsOnlySeen) {
		this.importantListNLPKeywordsOnlySeen = importantListNLPKeywordsOnlySeen;
	}

	public void setImportantListNLPKeywordsFlagged(
			double[] importantListNLPKeywordsFlagged) {
		this.importantListNLPKeywordsFlagged = importantListNLPKeywordsFlagged;
	}

	@javax.jdo.annotations.Column(allowsNull= "true" )
	@Hidden
	public double[] getUnimportantDirectKeywordVector() {
		return unimportantDirectKeywordVector;
	}
	
	public void setUnimportantDirectKeywordVector(
			double[] unimportantDirectKeywordVector) {
		this.unimportantDirectKeywordVector = unimportantDirectKeywordVector;
	}

}
