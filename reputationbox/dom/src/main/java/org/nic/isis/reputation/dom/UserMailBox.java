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
import org.nic.isis.reputation.utils.EmailUtils;
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
	
	
//	private IndexVectorMap contentIndexVectorMap;
//	private ContextVectorMap contentContextVectorMap;
//
//	private IndexVectorMap recipientIndexVectorMap;
//	private ContextVectorMap recipientContextVectorMap;
	
	private List<RandomIndexVector> contentVectors;
	private List<RandomIndexVector> recipientVectors;
	
	
	//important content profiles for the mailbox; for direct emails
//	private double[] importantTopicsReplied;
//	private double[] importantTopicsOnlySeen;
//	private double[] importantTopicsFlagged;
//	//important people profiles for the mailbox
//	private double[] importantPeopleReplied;
//	private double[] importantPeopleOnlySeen;
//	private double[] importantPeopleFlagged;
//	//important speech acts for the mailbox (meeting, request, delivery, proposal, commit)
//	private double[] importantSpeechActReplied;
//	private double[] importantSpeechActOnlySeen;
//	private double[] importantSpeechActFlagged;
//	
//	//important list emails; which are handled separately because the load of list emails is high 
//	private double[] importantListTopicsReplied;
//	private double[] importantListTopicsOnlySeen;
//	private double[] importantListTopicsFlagged;
//	//important people profiles for the mailbox
//	private double[] importantListPeopleReplied;
//	private double[] importantListPeopleOnlySeen;
//	private double[] importantListPeopleFlagged;
//	//important speech acts for the mailbox (meeting, request, delivery, proposal, commit)
//	private double[] importantListSpeechActReplied;
//	private double[] importantListSpeechActOnlySeen;
//	private double[] importantListSpeechActFlagged;
//	
//	
//	//unimportant topics/people/speech act profile from spam, 
//	private double[] unimportanttopicsVector;
//	private double[] unimportantPeopleVector;
//	private double[] unimportantSpeechActVector;
//	
//	
//	private double[] importantNLPKeywordsReplied;
//	private double[] importantNLPKeywordsOnlySeen;
//	private double[] importantNLPKeywordsFlagged;
//	
//	private double[] importantListNLPKeywordsReplied;
//	private double[] importantListNLPKeywordsOnlySeen;
//	private double[] importantListNLPKeywordsFlagged;
//	
//	private double[] unimportantDirectKeywordVector;
//	
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
	//periodically set this flag to update the monthly email importance model
	private boolean requireNewModel;
	
	//unimportant emails from deleted, spam tagged emails
	

	public UserMailBox() {
		this.allEmailContacts = new ArrayList<EmailContact>();
		this.allEmails = new ArrayList<Email>();
		this.contentVectors = new ArrayList<RandomIndexVector>();
		this.recipientVectors = new ArrayList<RandomIndexVector>();
		//set this to true when we need a new data model for email prediction
		this.requireNewModel = true;
		
//		importantTopicsReplied = new double[4000];
//		importantTopicsOnlySeen = new double[4000];
//		importantTopicsFlagged = new double[4000];
//		importantPeopleReplied = new double[4000];
//		importantPeopleOnlySeen = new double[4000];
//		importantPeopleFlagged = new double[4000];
//		
//		importantSpeechActReplied = new double[5];
//		importantSpeechActOnlySeen = new double[5];
//		importantSpeechActFlagged = new double[5];
//		
//		//list importance vectors
//		importantListTopicsReplied = new double[4000];
//		importantListTopicsOnlySeen = new double[4000];
//		importantListTopicsFlagged = new double[4000];
//		importantListPeopleReplied = new double[4000];
//		importantListPeopleOnlySeen = new double[4000];
//		importantListPeopleFlagged = new double[4000];
//		
//		importantListSpeechActReplied = new double[5];
//		importantListSpeechActOnlySeen = new double[5];
//		importantListSpeechActFlagged = new double[5];
//		
//		
//		unimportanttopicsVector = new double[4000];
//		unimportantPeopleVector = new double[4000];
//		unimportantSpeechActVector = new double[5];
//		
//		//keyword vectors
//		importantNLPKeywordsReplied = new double[4000];
//		importantNLPKeywordsOnlySeen = new double[4000];
//		importantNLPKeywordsFlagged = new double[4000];
//		
//		importantListNLPKeywordsReplied = new double[4000];
//		importantListNLPKeywordsOnlySeen = new double[4000];
//		importantListNLPKeywordsFlagged = new double[4000];
//		
//		unimportantDirectKeywordVector = new double[4000];
//		
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

	@Deprecated
	public void updateMailBoxProfiles(Email email){
			//processing the important/unimportant profile vectors
		//processing emails sent to lists and not directly to the user
		email.setModel(true);
		email.setPredicted(false);
		EmailReputationDataModel repModel = this.getReputationDataModel();
		
		//unimportant models
		if( email.isSpam() || email.isDeleted()){
			logger.info("this is a spam email recognized by flag or header");
			
			double[] unimportanttopicsVector = VectorsMath.addArrays(repModel.getSpamVector(), email.getTextContextVector());
			repModel.setSpamVector(unimportanttopicsVector);
			//logger.info("emai text vector sum : " + EmailUtils.getVectorTotal(email.getTextContextVector()) + "spam topic vector sum: " + EmailUtils.getVectorTotal(unimportanttopicsVector));
			
			
			double[] unimportantPeopleVector = VectorsMath.addArrays(repModel.getSpamPeopleVector(), email.getRecipientContextVector());
			repModel.setSpamPeopleVector(unimportantPeopleVector);
			
			double[] unimportantDirectKeywordVector = VectorsMath.addArrays(repModel.getSpamNLPKeywordVector(), email.getNlpKeywordsVector());
			repModel.setSpamNLPKeywordVector(unimportantDirectKeywordVector);
			nofOfUnimportantEmails++;
			
			//trying out the spam vector from reputation data model
			this.getReputationDataModel().getSpamEmails().add(email);
		}else {
			//important emails
			if(email.isDirect() || email.isCCd()){
				//direct emails
				//processing emails sent directly,ccd to user
				numberOfDirectEmails++;
				if(email.isAnswered()){
					logger.info("this is a direct email answered");
					double[] importantTopicsReplied = VectorsMath.addArrays(repModel.getImportantTopicsReplied(), email.getTextContextVector());
					repModel.setImportantTopicsReplied(importantTopicsReplied);
					//logger.info("emai text vector sum : " + EmailUtils.getVectorTotal(email.getTextContextVector()) + "direct replied important-topic vector sum: " + EmailUtils.getVectorTotal(importantTopicsReplied));
					
					double[] importantPeopleReplied = VectorsMath.addArrays(repModel.getImportantPeopleReplied(), email.getRecipientContextVector());
					repModel.setImportantPeopleReplied(importantPeopleReplied);
					
					double[] importantNLPKeywordsReplied = VectorsMath.addArrays(repModel.getImportantNLPKeywordsReplied(), email.getNlpKeywordsVector());
					repModel.setImportantNLPKeywordsReplied(importantNLPKeywordsReplied);
					
					numberOfDirectEmailsReplied++;
					this.getReputationDataModel().getRepliedEmails().add(email);
				}
				else if(email.isSeen()){
					logger.info("this is a direct email seen");
					
					double[] importantTopicsOnlySeen = VectorsMath.addArrays(repModel.getImportantTopicsOnlySeen(), email.getTextContextVector());
					repModel.setImportantTopicsOnlySeen(importantTopicsOnlySeen);
					//logger.info("emai text vector sum : " + EmailUtils.getVectorTotal(email.getTextContextVector()) + "direct seen important-topic vector sum: " + EmailUtils.getVectorTotal(importantTopicsOnlySeen));
					
					double[] importantPeopleOnlySeen = VectorsMath.addArrays(repModel.getImportantPeopleOnlySeen(), email.getRecipientContextVector());
					repModel.setImportantPeopleOnlySeen(importantPeopleOnlySeen);
					
					double[] importantNLPKeywordsOnlySeen = VectorsMath.addArrays(repModel.getImportantNLPKeywordsOnlySeen(), email.getNlpKeywordsVector());
					repModel.setImportantNLPKeywordsOnlySeen(importantNLPKeywordsOnlySeen);
					
					numberOfDirectEmailsSeen++;
					this.getReputationDataModel().getSeenEmails().add(email);
				}
				if(email.isFlagged()){
					logger.info("this is a direct email flagged");
					
					double[] importantTopicsFlagged = VectorsMath.addArrays(repModel.getImportantTopicsFlagged(), email.getTextContextVector());
					repModel.setImportantTopicsFlagged(importantTopicsFlagged);
					
					double[] importantPeopleFlagged = VectorsMath.addArrays(repModel.getImportantPeopleFlagged(), email.getRecipientContextVector());
					repModel.setImportantPeopleFlagged(importantPeopleFlagged);
					
					double[] importantNLPKeywordsFlagged = VectorsMath.addArrays(repModel.getImportantNLPKeywordsFlagged(), email.getNlpKeywordsVector());
					repModel.setImportantNLPKeywordsFlagged(importantNLPKeywordsFlagged);
					
					numberOfDirectEmailsFlagged++;
					this.getReputationDataModel().getFlaggedEmails().add(email);
				}
				
			}else {
				numberOfListEmails++;
				if(email.isAnswered()){
					logger.info("this is a list email answered");
					double[] importantListTopicsReplied = VectorsMath.addArrays
							(repModel.getImportantListTopicsReplied(), email.getTextContextVector());
					repModel.setImportantListTopicsReplied(importantListTopicsReplied);		
					//logger.info("emai text vector sum : " + EmailUtils.getVectorTotal(email.getTextContextVector()) + "list-replied-topic vector sum: " + EmailUtils.getVectorTotal(importantListTopicsReplied));
							
					double[] importantListTopicsSubjectReplied = VectorsMath.addArrays(repModel.getImportantListTopicsSubjectsReplied(), email.getSubjectContextVector());
					repModel.setImportantListTopicsSubjectsReplied(importantListTopicsSubjectReplied);
					double[] importantListTopicsBodyReplied = VectorsMath.addArrays(repModel.getImportantListTopicsBodyReplied(), email.getBodyContextVector());
					repModel.setImportantListTopicsBodyReplied(importantListTopicsBodyReplied);
					
					
					double[] importantListPeopleReplied = VectorsMath.addArrays
							(repModel.getImportantListPeopleReplied(), email.getRecipientContextVector());
					repModel.setImportantListPeopleReplied(importantListPeopleReplied);
					double[] importantListFromPeopleReplied = VectorsMath.addArrays
							(repModel.getImportantListPeopleFromReplied(), email.getFromContextVector());
					repModel.setImportantListPeopleFromReplied(importantListFromPeopleReplied);
					double[] importantListToPeopleReplied = VectorsMath.addArrays
							(repModel.getImportantListPeopleToReplied(), email.getToContextVector());
					repModel.setImportantListPeopleToReplied(importantListToPeopleReplied);
					double[] importantListCCPeopleReplied = VectorsMath.addArrays
							(repModel.getImportantListPeopleCCReplied(), email.getCcContextVector());
					repModel.setImportantListPeopleCCReplied(importantListCCPeopleReplied);
					
					
					
					double[] importantListNLPKeywordsReplied = VectorsMath.addArrays
							(repModel.getImportantListNLPKeywordsReplied(), email.getNlpKeywordsVector());
					repModel.setImportantListNLPKeywordsReplied(importantListNLPKeywordsReplied);
					
					numberOfListEmailsReplied++;
					this.getReputationDataModel().getRepliedListEmails().add(email);
									
				}
				else if(email.isSeen()){
					logger.info("this is a list email seen");
					double[] importantListTopicsOnlySeen = VectorsMath.addArrays
							(repModel.getImportantListTopicsOnlySeen(), email.getTextContextVector());
					repModel.setImportantListTopicsOnlySeen(importantListTopicsOnlySeen);
					//logger.info("emai text vector sum : " + EmailUtils.getVectorTotal(email.getTextContextVector()) + "list-seen-topic vector sum: " + EmailUtils.getVectorTotal(importantListTopicsOnlySeen));
					double[] importantListTopicsSubjectSeen = VectorsMath.addArrays(repModel.getImportantListTopicsSubjectsOnlySeen(), email.getSubjectContextVector());
					repModel.setImportantListTopicsSubjectsOnlySeen(importantListTopicsSubjectSeen);
					double[] importantListTopicsBodySeen = VectorsMath.addArrays(repModel.getImportantListTopicsBodyOnlySeen(), email.getBodyContextVector());
					repModel.setImportantListTopicsBodyOnlySeen(importantListTopicsBodySeen);
					
					
					double[] importantListPeopleOnlySeen = VectorsMath.addArrays(repModel.getImportantListPeopleOnlySeen(), email.getRecipientContextVector());
					repModel.setImportantListPeopleOnlySeen(importantListPeopleOnlySeen);
					//logger.info("emai recipient vector sum : " + EmailUtils.getVectorTotal(email.getRecipientContextVector()) + "list-seen-people vector sum: " + EmailUtils.getVectorTotal(importantListPeopleOnlySeen));
					double[] importantListFromPeopleOnlySeen = VectorsMath.addArrays
							(repModel.getImportantListPeopleFromOnlySeen(), email.getFromContextVector());
					repModel.setImportantListPeopleFromOnlySeen(importantListFromPeopleOnlySeen);
					double[] importantListToPeopleOnlySeen = VectorsMath.addArrays
							(repModel.getImportantListPeopleToOnlySeen(), email.getToContextVector());
					repModel.setImportantListPeopleToOnlySeen(importantListToPeopleOnlySeen);
					double[] importantListCCPeopleOnlySeen = VectorsMath.addArrays
							(repModel.getImportantListPeopleCCOnlySeen(), email.getCcContextVector());
					repModel.setImportantListPeopleCCOnlySeen(importantListCCPeopleOnlySeen);
					
					
					double[] importantListNLPKeywordsOnlySeen = VectorsMath.addArrays(repModel.getImportantListNLPKeywordsOnlySeen(), email.getNlpKeywordsVector());
					repModel.setImportantListNLPKeywordsOnlySeen(importantListNLPKeywordsOnlySeen);
					//logger.info("emai nlp keyword vector sum : " + EmailUtils.getVectorTotal(email.getNlpKeywordsVector()) + "list-seen-keyword vector sum: " + EmailUtils.getVectorTotal(importantListNLPKeywordsOnlySeen));
					
					numberOfListEmailsSeen++;
					this.getReputationDataModel().getSeenListEmails().add(email);
				}
				if(email.isFlagged()){
					logger.info("this is a list email flagged");
					double[] importantListTopicsFlagged = VectorsMath.addArrays(repModel.getImportantListTopicsFlagged(), email.getTextContextVector());
					repModel.setImportantListTopicsFlagged(importantListTopicsFlagged);
					//logger.info("emai text vector sum : " + EmailUtils.getVectorTotal(email.getTextContextVector()) + "flagged important-list-topic vector sum: " + EmailUtils.getVectorTotal(importantListTopicsFlagged));
					double[] importantListTopicsSubjectFlagged = VectorsMath.addArrays(repModel.getImportantListTopicsSubjectsFlagged(), email.getSubjectContextVector());
					repModel.setImportantListTopicsSubjectsFlagged(importantListTopicsSubjectFlagged);
					double[] importantListTopicsBodyFlagged = VectorsMath.addArrays(repModel.getImportantListTopicsBodyFlagged(), email.getBodyContextVector());
					repModel.setImportantListTopicsBodyFlagged(importantListTopicsBodyFlagged);
					
					
					double[] importantListPeopleFlagged = VectorsMath.addArrays(repModel.getImportantListPeopleFlagged(), email.getRecipientContextVector());
					repModel.setImportantListPeopleFlagged(importantListPeopleFlagged);
					
					double[] importantListFromPeopleFlagged = VectorsMath.addArrays
							(repModel.getImportantListPeopleFromFlagged(), email.getFromContextVector());
					repModel.setImportantListPeopleFromFlagged(importantListFromPeopleFlagged);
					double[] importantListToPeopleFlagged = VectorsMath.addArrays
							(repModel.getImportantListPeopleToFlagged(), email.getToContextVector());
					repModel.setImportantListPeopleToFlagged(importantListToPeopleFlagged);
					double[] importantListCCPeopleFlagged = VectorsMath.addArrays
							(repModel.getImportantListPeopleCCFlagged(), email.getCcContextVector());
					repModel.setImportantListPeopleCCFlagged(importantListCCPeopleFlagged);
					
					
					double[] importantListNLPKeywordsFlagged = VectorsMath.addArrays(repModel.getImportantListNLPKeywordsFlagged(), email.getNlpKeywordsVector());
					repModel.setImportantListNLPKeywordsFlagged(importantListNLPKeywordsFlagged);
					
					numberOfListEmailsFlagged++;
					this.getReputationDataModel().getFlaggedListEmails().add(email);
				}
			}
		}
		
			//adding email to the mailbox
			logger.info("adding email to mailbox and reputation-model with size ["
					+ this.getAllEmails().size() + " ] subject: "	
					+ email.getSubject() + " email sent timestamp : "
					+ email.getSentTimestamp());						
			this.addEmail(email);
			this.setReputationDataModel(repModel);
	}
	
	public Email predictImportanceFormEmail(Email newEmail){
		newEmail.setModel(false);
		newEmail.setPredicted(true);
		EmailReputationDataModel repModel = this.getReputationDataModel();
		
		if(newEmail.isSpam()){
			logger.info("The email to recommend reputation is pre-labeled as SPAM..");
			double spamTopicSimilarity = EmailUtils.calculateCosineSimilarity(repModel.getSpamVector(), newEmail.getTextContextVector());
			double spamPeopleSimilarity = EmailUtils.calculateCosineSimilarity(repModel.getSpamPeopleVector(), newEmail.getRecipientContextVector()); 
			double spamNLPKeywordSimilarity = EmailUtils.calculateCosineSimilarity(repModel.getSpamNLPKeywordVector(), newEmail.getNlpKeywordsVector());
			
			newEmail.setSpamTopicScore(spamTopicSimilarity);
			newEmail.setSpamPeopleScore(spamPeopleSimilarity);
			newEmail.setSpamKeywordscore(spamNLPKeywordSimilarity);
			
			logger.info("The similarity of the email with SPAM emails in the model \n:"
			+" spam topic similarity : " + spamTopicSimilarity 
			+ " spam NLP keyword similarity : " + spamNLPKeywordSimilarity 			
			+ " spam people similarity : " + spamPeopleSimilarity);
			
		}
		if(newEmail.isDirect() || newEmail.isCCd()){
			logger.info("The email is a direct or ccd email and not a list email");
			
			double flaggedTopicscore = EmailUtils.calculateCosineSimilarity(repModel.getImportantTopicsFlagged(), newEmail.getTextContextVector());
			double repliedTopicscore = EmailUtils.calculateCosineSimilarity(repModel.getImportantTopicsReplied(), newEmail.getTextContextVector());
			double seenTopicscore = EmailUtils.calculateCosineSimilarity(repModel.getImportantTopicsOnlySeen(), newEmail.getTextContextVector());
			double spamTopicSimilarity = EmailUtils.calculateCosineSimilarity(repModel.getSpamVector(), newEmail.getTextContextVector());
			
			double totalTopicScore = flaggedTopicscore + repliedTopicscore + seenTopicscore - spamTopicSimilarity;
			
			double flaggedTopicSubjectscore = EmailUtils.calculateCosineSimilarity(repModel.getImportantTopicsSubjectsFlagged(), newEmail.getSubjectContextVector());
			double repliedTopicSubjectscore = EmailUtils.calculateCosineSimilarity(repModel.getImportantTopicsSubjectsReplied(), newEmail.getSubjectContextVector());
			double seenTopicSubjectscore = EmailUtils.calculateCosineSimilarity(repModel.getImportantTopicsSubjectsOnlySeen(), newEmail.getSubjectContextVector());
			double spamTopicSubjectSimilarity = EmailUtils.calculateCosineSimilarity(repModel.getSpamVector(), newEmail.getSubjectContextVector());
			
			double totalTopicSubjectsScore = flaggedTopicSubjectscore + repliedTopicSubjectscore + seenTopicSubjectscore - spamTopicSubjectSimilarity;
			
			double flaggedTopicBodyscore = EmailUtils.calculateCosineSimilarity(repModel.getImportantTopicsBodyFlagged(), newEmail.getBodyContextVector());
			double repliedTopicBodyscore = EmailUtils.calculateCosineSimilarity(repModel.getImportantTopicsBodyReplied(), newEmail.getBodyContextVector());
			double seenTopicBodyscore = EmailUtils.calculateCosineSimilarity(repModel.getImportantTopicsBodyOnlySeen(), newEmail.getBodyContextVector());
			double spamTopicBodySimilarity = EmailUtils.calculateCosineSimilarity(repModel.getSpamVector(), newEmail.getBodyContextVector());
			
			double totalTopicBodyScore = flaggedTopicBodyscore + repliedTopicBodyscore + seenTopicBodyscore - spamTopicBodySimilarity;
			
			newEmail.setFlaggedTopicscore(flaggedTopicscore);
			newEmail.setRepliedTopicscore(repliedTopicscore);
			newEmail.setSeenTopicscore(seenTopicscore);
			newEmail.setSpamTopicScore(spamTopicSimilarity);
			newEmail.setTotalTopicScore(totalTopicScore);
			
			newEmail.setFlaggedTopicSubjectscore(flaggedTopicSubjectscore);
			newEmail.setRepliedTopicSubjectscore(repliedTopicSubjectscore);
			newEmail.setSeenTopicSubjectscore(seenTopicSubjectscore);
			newEmail.setSpamTopicSubjectScore(spamTopicSubjectSimilarity);
			newEmail.setTotalTopicSubjectScore(totalTopicSubjectsScore);
			
			newEmail.setFlaggedTopicBodyscore(flaggedTopicBodyscore);
			newEmail.setRepliedTopicBodyscore(repliedTopicBodyscore);
			newEmail.setSeenTopicBodyscore(seenTopicBodyscore);
			newEmail.setSpamTopicBodyScore(spamTopicBodySimilarity);
			newEmail.setTotalTopicBodyScore(totalTopicBodyScore);
									
			logger.info("flagged topic similarity : " + flaggedTopicscore + " subject sim : " + flaggedTopicSubjectscore + " body sim : " + flaggedTopicBodyscore);
			logger.info("replied topic similarity : " + repliedTopicscore + " subject sim : " + repliedTopicSubjectscore + " body sim : " + repliedTopicBodyscore);
			logger.info("seen topic similarity : " + seenTopicscore + " subject sim : " + seenTopicSubjectscore + " body sim : " + seenTopicBodyscore);
			logger.info("spam similarity : " + spamTopicSimilarity  + " subject spam sim : " + spamTopicSubjectSimilarity + " body spam sim : " + spamTopicBodySimilarity);
			logger.info("Total topic score " + totalTopicScore + " subject total sim : " + totalTopicSubjectsScore + " body total sim : " + totalTopicBodyScore);
			
			
			double flaggedKeywordsScore = EmailUtils.calculateCosineSimilarity(repModel.getImportantNLPKeywordsFlagged(), newEmail.getNlpKeywordsVector());
			double repliedKeywordsScore = EmailUtils.calculateCosineSimilarity(repModel.getImportantNLPKeywordsReplied(), newEmail.getNlpKeywordsVector());
			double seenKeywordsScore = EmailUtils.calculateCosineSimilarity(repModel.getImportantNLPKeywordsOnlySeen(), newEmail.getNlpKeywordsVector());
			double spamKeywordScore = EmailUtils.calculateCosineSimilarity(repModel.getSpamNLPKeywordVector(), newEmail.getNlpKeywordsVector());
			double totalKeywordScore = flaggedKeywordsScore + repliedKeywordsScore + seenKeywordsScore - spamKeywordScore;			
			
			logger.info("flagged keyword similarity : " + flaggedKeywordsScore);
			logger.info("replied keyword similarity: " + repliedKeywordsScore);
			logger.info("seen keyword similarity : " + seenKeywordsScore);
			logger.info("Spam keyword similarity : " + spamKeywordScore);
			logger.info("Total keyword similarity " + totalKeywordScore);
			newEmail.setFlaggedKeywordscore(flaggedKeywordsScore);
			newEmail.setRepliedKeywordscore(repliedKeywordsScore);
			newEmail.setSeenKeywordscore(seenKeywordsScore);
			newEmail.setSpamKeywordscore(spamKeywordScore);
			newEmail.setTotalKeywordscore(totalKeywordScore);
			
			double flaggedPeoplescore = EmailUtils.calculateCosineSimilarity(repModel.getImportantPeopleFlagged(), newEmail.getRecipientContextVector());
			double repliedPeoplescore = EmailUtils.calculateCosineSimilarity(repModel.getImportantPeopleReplied(), newEmail.getRecipientContextVector());
			double seenPeoplescore = EmailUtils.calculateCosineSimilarity(repModel.getImportantPeopleOnlySeen(), newEmail.getRecipientContextVector());
			double spamPeopleSimilarity = EmailUtils.calculateCosineSimilarity(repModel.getSpamPeopleVector(), newEmail.getRecipientContextVector());
			
			double totalPeopleScore = flaggedPeoplescore + repliedPeoplescore + seenPeoplescore - spamPeopleSimilarity;
			
			newEmail.setFlaggedPeoplescore(flaggedPeoplescore);
			newEmail.setRepliedPeoplescore(repliedPeoplescore);
			newEmail.setSeenPeoplescore(seenPeoplescore);
			newEmail.setSpamPeopleScore(spamPeopleSimilarity);
			newEmail.setTotalPeopleScore(totalPeopleScore);
			
			double flaggedPeopleToscore = EmailUtils.calculateCosineSimilarity(repModel.getImportantPeopleToFlagged(), newEmail.getToContextVector());
			double repliedPeopleToscore = EmailUtils.calculateCosineSimilarity(repModel.getImportantPeopleToReplied(), newEmail.getToContextVector());
			double seenPeopleToscore = EmailUtils.calculateCosineSimilarity(repModel.getImportantPeopleToOnlySeen(), newEmail.getToContextVector());
			double spamPeopleToSimilarity = EmailUtils.calculateCosineSimilarity(repModel.getSpamPeopleVector(), newEmail.getToContextVector());
			
			double totalPeopleToScore = flaggedPeopleToscore + repliedPeopleToscore + seenPeopleToscore - spamPeopleToSimilarity;
			
			newEmail.setFlaggedPeopleToscore(flaggedPeopleToscore);
			newEmail.setRepliedPeopleToscore(repliedPeopleToscore);
			newEmail.setSeenPeopleToscore(seenPeopleToscore);
			newEmail.setSpamPeopleToScore(spamPeopleToSimilarity);
			newEmail.setTotalPeopleToScore(totalPeopleToScore);
			
			double flaggedPeopleFromscore = EmailUtils.calculateCosineSimilarity(repModel.getImportantPeopleFromFlagged(), newEmail.getFromContextVector());
			double repliedPeopleFromscore = EmailUtils.calculateCosineSimilarity(repModel.getImportantPeopleFromReplied(), newEmail.getFromContextVector());
			double seenPeopleFromscore = EmailUtils.calculateCosineSimilarity(repModel.getImportantPeopleFromOnlySeen(), newEmail.getFromContextVector());
			double spamPeopleFromSimilarity = EmailUtils.calculateCosineSimilarity(repModel.getSpamPeopleVector(), newEmail.getFromContextVector());
			
			double totalPeopleFromScore = flaggedPeopleFromscore + repliedPeopleFromscore + seenPeopleFromscore - spamPeopleFromSimilarity;
			
			newEmail.setFlaggedPeopleFromscore(flaggedPeopleFromscore);
			newEmail.setRepliedPeopleFromscore(repliedPeopleFromscore);
			newEmail.setSeenPeopleFromscore(seenPeopleFromscore);
			newEmail.setSpamPeopleFromScore(spamPeopleFromSimilarity);
			newEmail.setTotalPeopleFromScore(totalPeopleFromScore);

			double flaggedPeopleCCscore = EmailUtils.calculateCosineSimilarity(repModel.getImportantPeopleCCFlagged(), newEmail.getCcContextVector());
			double repliedPeopleCCscore = EmailUtils.calculateCosineSimilarity(repModel.getImportantPeopleCCReplied(), newEmail.getCcContextVector());
			double seenPeopleCCscore = EmailUtils.calculateCosineSimilarity(repModel.getImportantPeopleCCOnlySeen(), newEmail.getCcContextVector());
			double spamPeopleCCSimilarity = EmailUtils.calculateCosineSimilarity(repModel.getSpamPeopleVector(), newEmail.getCcContextVector());
			
			double totalPeopleCCScore = flaggedPeopleCCscore + repliedPeopleCCscore + seenPeopleCCscore - spamPeopleCCSimilarity;
			
			newEmail.setFlaggedPeopleCCscore(flaggedPeopleCCscore);
			newEmail.setRepliedPeopleCCscore(repliedPeopleCCscore);
			newEmail.setSeenPeopleCCscore(seenPeopleCCscore);
			newEmail.setSpamPeopleCCScore(spamPeopleCCSimilarity);
			newEmail.setTotalPeopleCCScore(totalPeopleCCScore);

//
			logger.info("flagged recipient similarity : " + flaggedPeoplescore + " flagged To people sim : " + flaggedPeopleToscore + " flagged From people sim : " + flaggedPeopleFromscore
					+ " flagged Cc people sim : " + flaggedPeopleCCscore);
			logger.info("replied recipient similarity: " + repliedPeoplescore + " replied To people sim : " + repliedPeopleToscore + " replied From people sim : " + repliedPeopleFromscore
					+ " replied Cc people sim : " + repliedPeopleCCscore );
			logger.info("seen recipient similarity: " + seenPeoplescore + " seen To people sim : " + seenPeopleToscore + " seen From people sim : " + seenPeopleFromscore
					+ " seen Cc people sim : " + seenPeopleCCscore );
			logger.info("spam recipient similarity: " + spamPeopleSimilarity + " spam To people sim : " + spamPeopleToSimilarity + " spam From people sim : " + spamPeopleFromSimilarity
					+ " spam Cc people sim : " + spamPeopleCCSimilarity );
			logger.info("Total recipient similarity: " + totalPeopleScore);
			
//			double[] importantSpeechActsFlagged = this.getImportantSpeechActFlagged();
//			double[] importantRepliedSpeechActs = this.getImportantSpeechActReplied();
//			double[] importantSeenSpeechActs = this.getImportantSpeechActOnlySeen();
//			
//			double flaggedSAScore = Similarity.cosineSimilarity(importantSpeechActsFlagged, newEmail.getSpeechActVector());
//			double repliedSAScore = Similarity.cosineSimilarity(importantRepliedSpeechActs, newEmail.getSpeechActVector());
//			double seenSAScore = Similarity.cosineSimilarity(importantSeenSpeechActs, newEmail.getSpeechActVector());
//			double spamSpeechActSimilarity = Similarity.cosineSimilarity(unimportantSpeechActVector, newEmail.getSpeechActVector());
//			
//			double totalSAScore = flaggedSAScore + repliedSAScore + seenSAScore - spamSpeechActSimilarity;
//			logger.info("flagged SA score : " + flaggedSAScore);
//			logger.info("replied SA score : " + repliedSAScore);
//			logger.info("seen SA score : " + seenSAScore);
//			logger.info("Total SA score : " + totalSAScore);				
//			newEmail.setFlaggedSpeechActscore(totalSAScore);
//			newEmail.setRepliedSpeechActscore(repliedSAScore);
//			newEmail.setSeenSpeechActscore(seenSAScore);
//			newEmail.setTotalSpeechActscore(totalSAScore);
		}else {
			logger.info("The email is not a direct email, calculating similarities with list email profiles");

			double flaggedTopicscore = EmailUtils.calculateCosineSimilarity(repModel.getImportantListTopicsFlagged(), newEmail.getTextContextVector());
			double repliedTopicscore = EmailUtils.calculateCosineSimilarity(repModel.getImportantListTopicsReplied(), newEmail.getTextContextVector());
			double seenTopicscore = EmailUtils.calculateCosineSimilarity(repModel.getImportantListTopicsOnlySeen(), newEmail.getTextContextVector());
			double spamTopicSimilarity = EmailUtils.calculateCosineSimilarity(repModel.getSpamVector(), newEmail.getTextContextVector());
			
			double totalTopicScore = flaggedTopicscore + repliedTopicscore + seenTopicscore - spamTopicSimilarity;
			
			double flaggedTopicSubjectscore = EmailUtils.calculateCosineSimilarity(repModel.getImportantListTopicsSubjectsFlagged(), newEmail.getSubjectContextVector());
			double repliedTopicSubjectscore = EmailUtils.calculateCosineSimilarity(repModel.getImportantListTopicsSubjectsReplied(), newEmail.getSubjectContextVector());
			double seenTopicSubjectscore = EmailUtils.calculateCosineSimilarity(repModel.getImportantListTopicsSubjectsOnlySeen(), newEmail.getSubjectContextVector());
			double spamTopicSubjectSimilarity = EmailUtils.calculateCosineSimilarity(repModel.getSpamVector(), newEmail.getSubjectContextVector());
			
			double totalTopicSubjectsScore = flaggedTopicSubjectscore + repliedTopicSubjectscore + seenTopicSubjectscore - spamTopicSubjectSimilarity;
			
			double flaggedTopicBodyscore = EmailUtils.calculateCosineSimilarity(repModel.getImportantListTopicsBodyFlagged(), newEmail.getBodyContextVector());
			double repliedTopicBodyscore = EmailUtils.calculateCosineSimilarity(repModel.getImportantListTopicsBodyReplied(), newEmail.getBodyContextVector());
			double seenTopicBodyscore = EmailUtils.calculateCosineSimilarity(repModel.getImportantListTopicsBodyOnlySeen(), newEmail.getBodyContextVector());
			double spamTopicBodySimilarity = EmailUtils.calculateCosineSimilarity(repModel.getSpamVector(), newEmail.getBodyContextVector());
			
			double totalTopicBodyScore = flaggedTopicBodyscore + repliedTopicBodyscore + seenTopicBodyscore - spamTopicBodySimilarity;
			
			newEmail.setFlaggedTopicscore(flaggedTopicscore);
			newEmail.setRepliedTopicscore(repliedTopicscore);
			newEmail.setSeenTopicscore(seenTopicscore);
			newEmail.setSpamTopicScore(spamTopicSimilarity);
			newEmail.setTotalTopicScore(totalTopicScore);
			
			newEmail.setFlaggedTopicSubjectscore(flaggedTopicSubjectscore);
			newEmail.setRepliedTopicSubjectscore(repliedTopicSubjectscore);
			newEmail.setSeenTopicSubjectscore(seenTopicSubjectscore);
			newEmail.setSpamTopicSubjectScore(spamTopicSubjectSimilarity);
			newEmail.setTotalTopicSubjectScore(totalTopicSubjectsScore);
			
			newEmail.setFlaggedTopicBodyscore(flaggedTopicBodyscore);
			newEmail.setRepliedTopicBodyscore(repliedTopicBodyscore);
			newEmail.setSeenTopicBodyscore(seenTopicBodyscore);
			newEmail.setSpamTopicBodyScore(spamTopicBodySimilarity);
			newEmail.setTotalTopicBodyScore(totalTopicBodyScore);
									
			logger.info("flagged topic similarity : " + flaggedTopicscore + " subject sim : " + flaggedTopicSubjectscore + " body sim : " + flaggedTopicBodyscore);
			logger.info("replied topic similarity : " + repliedTopicscore + " subject sim : " + repliedTopicSubjectscore + " body sim : " + repliedTopicBodyscore);
			logger.info("seen topic similarity : " + seenTopicscore + " subject sim : " + seenTopicSubjectscore + " body sim : " + seenTopicBodyscore);
			logger.info("spam similarity : " + spamTopicSimilarity  + " subject spam sim : " + spamTopicSubjectSimilarity + " body spam sim : " + spamTopicBodySimilarity);
			logger.info("Total topic score " + totalTopicScore + " subject total sim : " + totalTopicSubjectsScore + " body total sim : " + totalTopicBodyScore);
			
			double flaggedKeywordsScore = EmailUtils.calculateCosineSimilarity(repModel.getImportantListNLPKeywordsFlagged(), newEmail.getNlpKeywordsVector());
			double repliedKeywordsScore = EmailUtils.calculateCosineSimilarity(repModel.getImportantListNLPKeywordsReplied(), newEmail.getNlpKeywordsVector());
			double seenKeywordsScore = EmailUtils.calculateCosineSimilarity(repModel.getImportantListNLPKeywordsOnlySeen(), newEmail.getNlpKeywordsVector());
			double spamKeywordScore = EmailUtils.calculateCosineSimilarity(repModel.getSpamNLPKeywordVector(), newEmail.getNlpKeywordsVector());
			double totalListKeywordScore = flaggedKeywordsScore + repliedKeywordsScore + seenKeywordsScore - spamKeywordScore;		
			
			logger.info("flagged list keyword similarity : " + flaggedKeywordsScore);
			logger.info("replied list keyword similarity : " + repliedKeywordsScore);
			logger.info("seen list keyword similarity : " + seenKeywordsScore);
			logger.info("Spam list keyword similarity : " + spamKeywordScore);
			logger.info("Total list keyword similarity : " + totalListKeywordScore);
			newEmail.setFlaggedKeywordscore(flaggedKeywordsScore);
			newEmail.setRepliedKeywordscore(repliedKeywordsScore);
			newEmail.setSeenKeywordscore(seenKeywordsScore);
			newEmail.setSpamKeywordscore(spamKeywordScore);
			newEmail.setTotalKeywordscore(totalListKeywordScore);						
			

			double flaggedPeoplescore = EmailUtils.calculateCosineSimilarity(repModel.getImportantListPeopleFlagged(), newEmail.getRecipientContextVector());
			double repliedPeoplescore = EmailUtils.calculateCosineSimilarity(repModel.getImportantListPeopleReplied(), newEmail.getRecipientContextVector());
			double seenPeoplescore = EmailUtils.calculateCosineSimilarity(repModel.getImportantListPeopleOnlySeen(), newEmail.getRecipientContextVector());
			double spamPeopleSimilarity = EmailUtils.calculateCosineSimilarity(repModel.getSpamPeopleVector(), newEmail.getRecipientContextVector());
			
			double totalPeopleScore = flaggedPeoplescore + repliedPeoplescore + seenPeoplescore - spamPeopleSimilarity;
			
			newEmail.setFlaggedPeoplescore(flaggedPeoplescore);
			newEmail.setRepliedPeoplescore(repliedPeoplescore);
			newEmail.setSeenPeoplescore(seenPeoplescore);
			newEmail.setSpamPeopleScore(spamPeopleSimilarity);
			newEmail.setTotalPeopleScore(totalPeopleScore);
			
			double flaggedPeopleToscore = EmailUtils.calculateCosineSimilarity(repModel.getImportantListPeopleToFlagged(), newEmail.getToContextVector());
			double repliedPeopleToscore = EmailUtils.calculateCosineSimilarity(repModel.getImportantListPeopleToReplied(), newEmail.getToContextVector());
			double seenPeopleToscore = EmailUtils.calculateCosineSimilarity(repModel.getImportantListPeopleToOnlySeen(), newEmail.getToContextVector());
			double spamPeopleToSimilarity = EmailUtils.calculateCosineSimilarity(repModel.getSpamPeopleVector(), newEmail.getToContextVector());
			
			double totalPeopleToScore = flaggedPeopleToscore + repliedPeopleToscore + seenPeopleToscore - spamPeopleToSimilarity;
			
			newEmail.setFlaggedPeopleToscore(flaggedPeopleToscore);
			newEmail.setRepliedPeopleToscore(repliedPeopleToscore);
			newEmail.setSeenPeopleToscore(seenPeopleToscore);
			newEmail.setSpamPeopleToScore(spamPeopleToSimilarity);
			newEmail.setTotalPeopleToScore(totalPeopleToScore);
			
			double flaggedPeopleFromscore = EmailUtils.calculateCosineSimilarity(repModel.getImportantListPeopleFromFlagged(), newEmail.getFromContextVector());
			double repliedPeopleFromscore = EmailUtils.calculateCosineSimilarity(repModel.getImportantListPeopleFromReplied(), newEmail.getFromContextVector());
			double seenPeopleFromscore = EmailUtils.calculateCosineSimilarity(repModel.getImportantListPeopleFromOnlySeen(), newEmail.getFromContextVector());
			double spamPeopleFromSimilarity = EmailUtils.calculateCosineSimilarity(repModel.getSpamPeopleVector(), newEmail.getFromContextVector());
			
			double totalPeopleFromScore = flaggedPeopleFromscore + repliedPeopleFromscore + seenPeopleFromscore - spamPeopleFromSimilarity;
			
			newEmail.setFlaggedPeopleFromscore(flaggedPeopleFromscore);
			newEmail.setRepliedPeopleFromscore(repliedPeopleFromscore);
			newEmail.setSeenPeopleFromscore(seenPeopleFromscore);
			newEmail.setSpamPeopleFromScore(spamPeopleFromSimilarity);
			newEmail.setTotalPeopleFromScore(totalPeopleFromScore);

			double flaggedPeopleCCscore = EmailUtils.calculateCosineSimilarity(repModel.getImportantListPeopleCCFlagged(), newEmail.getCcContextVector());
			double repliedPeopleCCscore = EmailUtils.calculateCosineSimilarity(repModel.getImportantListPeopleCCReplied(), newEmail.getCcContextVector());
			double seenPeopleCCscore = EmailUtils.calculateCosineSimilarity(repModel.getImportantListPeopleCCOnlySeen(), newEmail.getCcContextVector());
			double spamPeopleCCSimilarity = EmailUtils.calculateCosineSimilarity(repModel.getSpamPeopleVector(), newEmail.getCcContextVector());
			
			double totalPeopleCCScore = flaggedPeopleCCscore + repliedPeopleCCscore + seenPeopleCCscore - spamPeopleCCSimilarity;
			
			newEmail.setFlaggedPeopleCCscore(flaggedPeopleCCscore);
			newEmail.setRepliedPeopleCCscore(repliedPeopleCCscore);
			newEmail.setSeenPeopleCCscore(seenPeopleCCscore);
			newEmail.setSpamPeopleCCScore(spamPeopleCCSimilarity);
			newEmail.setTotalPeopleCCScore(totalPeopleCCScore);

//
			logger.info("flagged recipient similarity : " + flaggedPeoplescore + " flagged To people sim : " + flaggedPeopleToscore + " flagged From people sim : " + flaggedPeopleFromscore
					+ " flagged Cc people sim : " + flaggedPeopleCCscore);
			logger.info("replied recipient similarity: " + repliedPeoplescore + " replied To people sim : " + repliedPeopleToscore + " replied From people sim : " + repliedPeopleFromscore
					+ " replied Cc people sim : " + repliedPeopleCCscore );
			logger.info("seen recipient similarity: " + seenPeoplescore + " seen To people sim : " + seenPeopleToscore + " seen From people sim : " + seenPeopleFromscore
					+ " seen Cc people sim : " + seenPeopleCCscore );
			logger.info("spam recipient similarity: " + spamPeopleSimilarity + " spam To people sim : " + spamPeopleToSimilarity + " spam From people sim : " + spamPeopleFromSimilarity
					+ " spam Cc people sim : " + spamPeopleCCSimilarity );
			logger.info("Total recipient similarity: " + totalPeopleScore);
			
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
	
		
	@Hidden
	@javax.jdo.annotations.Persistent
	public int getNofOfUnimportantEmails() {
		return nofOfUnimportantEmails;
	}

	public void setNofOfUnimportantEmails(int nofOfUnimportantEmails) {
		this.nofOfUnimportantEmails = nofOfUnimportantEmails;
	}

	@Hidden
	@javax.jdo.annotations.Persistent
	public int getNumberOfListEmails() {
		return numberOfListEmails;
	}

	public void setNumberOfListEmails(int numberOfListEmails) {
		this.numberOfListEmails = numberOfListEmails;
	}

	@Hidden
	@javax.jdo.annotations.Persistent
	public int getNumberOfDirectEmails() {
		return numberOfDirectEmails;
	}

	public void setNumberOfDirectEmails(int numberOfDirectEmails) {
		this.numberOfDirectEmails = numberOfDirectEmails;
	}

	@Hidden
	@javax.jdo.annotations.Persistent
	public int getNumberOfListEmailsReplied() {
		return numberOfListEmailsReplied;
	}

	public void setNumberOfListEmailsReplied(int numberOfListEmailsReplied) {
		this.numberOfListEmailsReplied = numberOfListEmailsReplied;
	}

	@Hidden
	@javax.jdo.annotations.Persistent
	public int getNumberOfListEmailsFlagged() {
		return numberOfListEmailsFlagged;
	}

	public void setNumberOfListEmailsFlagged(int numberOfListEmailsFlagged) {
		this.numberOfListEmailsFlagged = numberOfListEmailsFlagged;
	}
	
	
	public void printImportanceModelForMailBox(){
		EmailReputationDataModel model = this.getReputationDataModel();
		
		//flagged
		double[] importantTopicsFlaggedProfile = model.getImportantTopicsFlagged();
		double[] importantPeopleFlaggedProfile = model.getImportantPeopleFlagged();
		double[] importantNLPKeywordsFlaggedProfile = model.getImportantNLPKeywordsFlagged();
		
		double flaggedTopicProfileScore = EmailUtils.getVectorTotal(importantTopicsFlaggedProfile);
		logger.info(" flagged direct topic profile vector sum :" + flaggedTopicProfileScore +" no : " + this.reputationDataModel.getFlaggedEmails().size());
		double flaggedPeopleProfileScore = EmailUtils.getVectorTotal(importantPeopleFlaggedProfile);
		logger.info(" flagged  direct people profile vector sum :" + flaggedPeopleProfileScore );
		double flaggedNLPProfileScore = EmailUtils.getVectorTotal(importantNLPKeywordsFlaggedProfile);
		logger.info(" flagged  direct keywords profile vector sum :" + flaggedNLPProfileScore);
		
		double[] importantListTopicsFlaggedProfile = model.getImportantListTopicsFlagged();
		double[] importantListPeopleFlaggedProfile = model.getImportantListPeopleFlagged();
		double[] importantListNLPKeywordsFlaggedProfile = model.getImportantListNLPKeywordsFlagged();
		
		double flaggedListTopicProfileScore = EmailUtils.getVectorTotal(importantListTopicsFlaggedProfile);
		logger.info("List flagged topic profile vector sum :" + flaggedListTopicProfileScore + " no : " + this.reputationDataModel.getFlaggedListEmails().size());
		double flaggedListPeopleProfileScore = EmailUtils.getVectorTotal(importantListPeopleFlaggedProfile);
		logger.info("List flagged people profile vector sum :" + flaggedListPeopleProfileScore);
		double flaggedListNLPProfileScore = EmailUtils.getVectorTotal(importantListNLPKeywordsFlaggedProfile);
		logger.info("List flagged keywords profile vector sum :" + flaggedListNLPProfileScore);
		
		
		
		double[] importantTopicsRepliedProfile = model.getImportantTopicsReplied();
		double[] importantPeopleRepliedProfile = model.getImportantPeopleReplied();
		double[] importantNLPKeywordsRepliedProfile = model.getImportantNLPKeywordsReplied();
		
		
		double repliedTopicProfileScore = EmailUtils.getVectorTotal(importantTopicsRepliedProfile);
		logger.info(" replied  direct topic profile vector sum :" + repliedTopicProfileScore + " no : " + this.reputationDataModel.getRepliedEmails().size());
		double repliedPeopleProfileScore = EmailUtils.getVectorTotal(importantPeopleRepliedProfile);
		logger.info(" replied  direct people profile vector sum :" + repliedPeopleProfileScore);
		double repliedNLPProfileScore = EmailUtils.getVectorTotal(importantNLPKeywordsRepliedProfile);
		logger.info(" replied  direct keywords profile vector sum :" + repliedNLPProfileScore);
		
		
		double[] importantListTopicsRepliedProfile = model.getImportantListTopicsReplied();
		double[] importantListPeopleRepliedProfile = model.getImportantListPeopleReplied();
		double[] importantListNLPKeywordsRepliedProfile = model.getImportantListNLPKeywordsReplied();
		
		double repliedListTopicProfileScore = EmailUtils.getVectorTotal(importantListTopicsRepliedProfile);
		logger.info("List replied topic profile vector sum :" + repliedListTopicProfileScore + " no : " + this.reputationDataModel.getRepliedListEmails());
		double repliedListPeopleProfileScore = EmailUtils.getVectorTotal(importantListPeopleRepliedProfile);
		logger.info("List replied people profile vector sum :" + repliedListPeopleProfileScore);
		double repliedListNLPProfileScore = EmailUtils.getVectorTotal(importantListNLPKeywordsRepliedProfile);
		logger.info("List replied keywords profile vector sum :" + repliedListNLPProfileScore);
		
		
		double[] importantTopicsSeenProfile = model.getImportantTopicsOnlySeen();
		double[] importantPeopleSeenProfile = model.getImportantPeopleOnlySeen();
		double[] importantNLPKeywordsSeenProfile = model.getImportantNLPKeywordsOnlySeen();
		
		double seenTopicProfileScore = EmailUtils.getVectorTotal(importantTopicsSeenProfile);
		logger.info("seen  direct topic profile vector sum :" + seenTopicProfileScore+ " no :" + this.reputationDataModel.getSeenEmails().size());
		double seenPeopleProfileScore = EmailUtils.getVectorTotal(importantPeopleSeenProfile);
		logger.info("seen  direct people profile vector sum :" + seenPeopleProfileScore);
		double seenNLPProfileScore = EmailUtils.getVectorTotal(importantNLPKeywordsSeenProfile);
		logger.info("seen  direct keywords profile vector sum :" + seenNLPProfileScore);
		
		
		double[] importantListTopicsSeenProfile = model.getImportantListTopicsOnlySeen();
		double[] importantListPeopleSeenProfile = model.getImportantListPeopleOnlySeen();
		double[] importantListNLPKeywordsSeenProfile = model.getImportantListNLPKeywordsOnlySeen();
		
		double seenListTopicProfileScore = EmailUtils.getVectorTotal(importantListTopicsSeenProfile);
		logger.info("List seen topic profile vector sum :" + seenListTopicProfileScore + " no : " + this.reputationDataModel.getSeenListEmails().size());
		double seenListPeopleProfileScore = EmailUtils.getVectorTotal(importantListPeopleSeenProfile);
		logger.info("List seen people profile vector sum :" + seenListPeopleProfileScore);
		double seenListNLPProfileScore = EmailUtils.getVectorTotal(importantListNLPKeywordsSeenProfile);
		logger.info("List seen keywords profile vector sum :" + seenListNLPProfileScore);
		
		
		double[] spamTopicsVector = model.getSpamVector();
		double[] spamPeopleVector = model.getSpamPeopleVector();
		double[] spamKeywordsVector = model.getSpamNLPKeywordVector();
		
		double spamTopicProfileScore = EmailUtils.getVectorTotal(spamTopicsVector);
		logger.info("spam topic profile vector sum :" + spamTopicProfileScore + " no : " + this.reputationDataModel.getSpamEmails().size());
		double spamVectorSum = EmailUtils.getVectorTotal(this.getReputationDataModel().getSpamVector());
		logger.info("reputationDataModel spam topic vector sum :" + spamVectorSum + " no : " + this.getReputationDataModel().getSpamEmails().size());
		
		double spamPeopleProfileScore = EmailUtils.getVectorTotal(spamPeopleVector);
		logger.info("spam replied people profile vector sum :" + spamPeopleProfileScore);
		double spamNLPProfileScore = EmailUtils.getVectorTotal(spamKeywordsVector);
		logger.info("spam replied keywords profile vector sum :" + spamNLPProfileScore);
		
		
		logger.info("Dunn index for content clusters : " + model.getDunnIndexForContentClusters());
	}

	@Hidden
	@javax.jdo.annotations.Persistent
	public int getNumberOfListEmailsSeen() {
		return numberOfListEmailsSeen;
	}

	public void setNumberOfListEmailsSeen(int numberOfListEmailsSeen) {
		this.numberOfListEmailsSeen = numberOfListEmailsSeen;
	}
	
	@Hidden
	@javax.jdo.annotations.Persistent
	public int getNumberOfDirectEmailsReplied() {
		return numberOfDirectEmailsReplied;
	}

	public void setNumberOfDirectEmailsReplied(int numberOfDirectEmailsReplied) {
		this.numberOfDirectEmailsReplied = numberOfDirectEmailsReplied;
	}

	@Hidden
	@javax.jdo.annotations.Persistent
	public int getNumberOfDirectEmailsFlagged() {
		return numberOfDirectEmailsFlagged;
	}

	public void setNumberOfDirectEmailsFlagged(int numberOfDirectEmailsFlagged) {
		this.numberOfDirectEmailsFlagged = numberOfDirectEmailsFlagged;
	}

	@Hidden
	@javax.jdo.annotations.Persistent
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

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true") 
	public List<RandomIndexVector> getContentVectors() {
		return contentVectors;
	}

	public void setContentVectors(List<RandomIndexVector> contentVectors) {
		this.contentVectors = contentVectors;
	}
	
	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true") 
	public List<RandomIndexVector> getRecipientVectors() {
		return recipientVectors;
	}

	public void setRecipientVectors(List<RandomIndexVector> recipientVectors) {
		this.recipientVectors = recipientVectors;
	}
	
	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true") 
	public boolean isRequireNewModel() {
		return requireNewModel;
	}

	public void setRequireNewModel(boolean requireNewModel) {
		this.requireNewModel = requireNewModel;
	}

	
}
