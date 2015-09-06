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
import javax.jdo.annotations.NotPersistent;
import javax.jdo.annotations.PrimaryKey;
import javax.jdo.annotations.VersionStrategy;

import org.apache.isis.applib.ViewModel;
import org.apache.isis.applib.annotation.Hidden;
import org.apache.isis.applib.annotation.ObjectType;
import org.apache.isis.applib.annotation.Programmatic;
import org.apache.isis.applib.annotation.Title;
import org.nic.isis.clustering.EmailContentCluster;
import org.nic.isis.clustering.EmailRecipientCluster;
import org.nic.isis.clustering.EmailWeightedSubjectBodyContentCluster;
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
	private List<Email> allEmails;
	//map of allemails msgkey:mail
	@NotPersistent
	private Map<Long,Email> emailMap;
	@NotPersistent
	private List<RandomIndexVector> contentVectors;
	@NotPersistent
	private List<RandomIndexVector> recipientVectors;
	
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
	
	//model size of the current email training set
	private int currentModelSize;
	
	//the list of marked important email uids from client for current session
	private List<Long> markedImportantEmailUids;
	//the list of marked important email uids from client for current session
	private List<Long> markedSpamEmailUids;
	
	//intial size of emails to retrieve for the month
	private int mailToRetrieveForPeriod;
	
	//last emai uid sent as prediction results to the client
	private long lastPredictedMsgUidSentToClient;
	
	//score thresholds
//	private final static double replyTopicScoreThreshold = 0.6;
//	private final static double seeTopicScoreThreshold = 0.6;
//	private final static double replyPeopleScoreThreshold = 0.6;
//	private final static double seePeopleScoreThreshold = 0.;
//	
//	private final static double replyListTopicScoreThreshold = 0.6;
//	private final static double seeListTopicScoreThreshold = 0.6;
//	private final static double replyListPeopleScoreThreshold = 0.6;
//	private final static double seeListPeopleScoreThreshold = 0.6;
	
	public UserMailBox() {
		this.allEmailContacts = new ArrayList<EmailContact>();
		this.allEmails = new ArrayList<Email>();
		this.contentVectors = new ArrayList<RandomIndexVector>();
		this.recipientVectors = new ArrayList<RandomIndexVector>();
		this.markedImportantEmailUids = new ArrayList<Long>();
		this.markedSpamEmailUids = new ArrayList<Long>();
		//set this to true when we need a new data model for email prediction
		this.requireNewModel = true;
		reputationDataModel = new EmailReputationDataModel();
		this.setLastPredictedMsgUidSentToClient(0);
		
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
		//adding the email to the emailMap for reference purposes
		if(this.emailMap == null){
			this.getEmailMap().put(email.getMsgUid(), email);
				
		}else{
			emailMap.put(email.getMsgUid(), email);
		}
		
		this.lastIndexTimestamp = email.getSentTimestamp();
		logger.info("Adding the email to mailbox and setting the last indexed message id "
				+ "of mailbox to : " + email.getMsgUid()  + " mbox size : " + this.getAllEmails().size());
		this.setLastIndexedMsgUid(email.getMsgUid());
	}
	
	//predict email importance by the profile content and recipient clusters
	public Email predictImportanceBasedOnProfileSubClusterSimilarity(Email newEmail){
		newEmail.setModel(false);
		newEmail.setPredicted(true);
		
		EmailReputationDataModel repModel = this.getReputationDataModel();
		List<EmailWeightedSubjectBodyContentCluster> repliedContentClusters = repModel.getRepliedProfileContentClusters();
		List<EmailWeightedSubjectBodyContentCluster> seenContentClusters = repModel.getSeenProfileContentClusters();
		List<EmailWeightedSubjectBodyContentCluster> repliedListContentClusters = repModel.getRepliedListProfileContentClusters();
		List<EmailWeightedSubjectBodyContentCluster> seenListContentClusters = repModel.getSeenListProfileContentClusters();
		
		List<EmailWeightedSubjectBodyContentCluster> flaggedContentClusters = repModel.getFlaggedProfileContentClusters();
		List<EmailWeightedSubjectBodyContentCluster> flaggedListContentClusters = repModel.getFlaggedListProfileContentClusters();
		List<EmailRecipientCluster> flaggedPeopleClusters = repModel.getFlaggedProfilePeopleClusters();
		List<EmailRecipientCluster> flaggedListPeopleClusters = repModel.getFlaggedListProfilePeopleClusters();
		
		
		List<EmailRecipientCluster> repliedPeopleClusters = repModel.getRepliedProfilePeopleClusters();
		List<EmailRecipientCluster> repliedListPeopleClusters = repModel.getRepliedListProfilePeopleClusters();
		List<EmailRecipientCluster> seenPeopleClusters = repModel.getSeenProfilePeopleClusters();
		List<EmailRecipientCluster> seenListPeopleClusters = repModel.getSeenListProfilePeopleClusters();
		
		
		
		
		double[] spamVector = repModel.getSpamVector();
		double[] spamPeopleVector = repModel.getSpamPeopleVector();
		
		if(newEmail.isSpam()){
			logger.info("The email to recommend reputation is pre-labeled as SPAM. hence adding to the spam list.");
			double[] unimportanttopicsVector = VectorsMath.addArrays(spamVector, newEmail.getTextContextVector());
			repModel.setSpamVector(unimportanttopicsVector);
			
			//logger.info("rep model's spam vector total :  " + EmailUtils.getVectorTotal(repModel.getSpamVector()));
			//logger.info("new email's textcontent vector : " + EmailUtils.getVectorTotal(newEmail.getTextContextVector()));
			double spamTopicSimilarity = EmailUtils.calculateCosineSimilarity(spamVector, newEmail.getTextContextVector());
			double spamPeopleSimilarity = EmailUtils.calculateCosineSimilarity(spamPeopleVector, newEmail.getRecipientContextVector()); 
			//double spamNLPKeywordSimilarity = EmailUtils.calculateCosineSimilarity(spamKeywordVector, newEmail.getNlpKeywordsVector());
				
			newEmail.setSpamTopicScore(spamTopicSimilarity);
			newEmail.setSpamPeopleScore(spamPeopleSimilarity);
			
			logger.info("The similarity of the email with SPAM emails in the model \n:"
			+" spam topic similarity : " + spamTopicSimilarity 		
			+ " spam people similarity : " + spamPeopleSimilarity);
			
		}
		if(newEmail.isDirect() || newEmail.isCCd()){
			System.out.println("The email is a direct or ccd email and not a list email");
			double maxTopicSimilarity = 0.0;
			
			if(repliedContentClusters !=  null && repliedContentClusters.size() > 0){
				for(EmailWeightedSubjectBodyContentCluster contentCluster : repliedContentClusters){
					double similarity = contentCluster.getSimilarity(newEmail.getSubjectContextVector(), newEmail.getBodyContextVector());
					if(similarity > maxTopicSimilarity){
						maxTopicSimilarity = similarity;
					}
				}
				double repliedTopicScore = maxTopicSimilarity;
				newEmail.setProfileClusterReplyContentScore(repliedTopicScore);
			}else{
				logger.error("rep-model replied profile content clusters are not created..");
			}
			
			if(repliedPeopleClusters != null && repliedPeopleClusters.size() > 0){
				double maxPeopleSimilarity = 0.0;
				for(EmailRecipientCluster peopleCluster : repliedPeopleClusters){
					double similarity = peopleCluster.getSimilarity(newEmail.getRecipientContextVector());
					if(similarity > maxPeopleSimilarity){
						maxPeopleSimilarity = similarity;
					}
				}
				double repliedPeopleScore = maxPeopleSimilarity;
				newEmail.setProfileClusterReplyRecipientScore(repliedPeopleScore);
			}else{
				logger.error("rep-model seen profile people clusters are not created..");
			}
			
			//seen emails
			double maxTopicSeenSimilarity = 0.0;
			double seenTopicScore = 0.0;
			if(seenContentClusters !=  null && seenContentClusters.size() > 0){
				for(EmailWeightedSubjectBodyContentCluster contentCluster : seenContentClusters){
					//logger.info(contentCluster.getId() + " : content cluster repScore : " + contentCluster.getReputationScore());
					double similarity = contentCluster.getSimilarity(newEmail.getSubjectContextVector(), newEmail.getBodyContextVector());
					if(similarity > maxTopicSeenSimilarity){
						maxTopicSeenSimilarity = similarity;
						//seenTopicScore = contentCluster.getReputationScore() * maxTopicSeenSimilarity;
					}
				}
				newEmail.setProfileClusterReadContentScore(maxTopicSeenSimilarity);
			}else{
				logger.error("rep-model seen profile content clusters are not created..");
			}
			if(seenPeopleClusters != null && seenPeopleClusters.size() > 0){
				double maxSeenPeopleSimilarity = 0.0;
				double seenPeopleScore = 0.0;
				for(EmailRecipientCluster peopleCluster : seenPeopleClusters){
					//logger.info(peopleCluster.getId() + " : people cluster repScore : " + peopleCluster.getReputationScore());
					double similarity = peopleCluster.getSimilarity(newEmail.getRecipientContextVector());
					if(similarity > maxSeenPeopleSimilarity){
						maxSeenPeopleSimilarity = similarity;
						//seenPeopleScore = peopleCluster.getReputationScore() * maxSeenPeopleSimilarity;
					}
				}
				newEmail.setProfileClusterReadRecipientScore(maxSeenPeopleSimilarity);
			}else{
				logger.error("rep-model seen profile people clusters are not created..");
			}
			
			//flagged emails
			double maxTopicFlaggedSimilarity = 0.0;
			double flaggedTopicScore = 0.0;
			if(flaggedContentClusters !=  null && flaggedContentClusters.size() > 0){
				for(EmailWeightedSubjectBodyContentCluster contentCluster : flaggedContentClusters){
					//logger.info(contentCluster.getId() + " : content cluster repScore : " + contentCluster.getReputationScore());
					double similarity = contentCluster.getSimilarity(newEmail.getSubjectContextVector(), newEmail.getBodyContextVector());
					if(similarity > maxTopicSeenSimilarity){
						maxTopicSeenSimilarity = similarity;
						//seenTopicScore = contentCluster.getReputationScore() * maxTopicSeenSimilarity;
					}
				}
				newEmail.setProfileClusterFlagContentScore(maxTopicSeenSimilarity);
			}else{
				logger.error("rep-model flagged profile content clusters are not created..");
			}
			if(flaggedPeopleClusters != null && flaggedPeopleClusters.size() > 0){
				double maxFlaggedPeopleSimilarity = 0.0;
				double flaggedPeopleScore = 0.0;
				for(EmailRecipientCluster peopleCluster : flaggedPeopleClusters){
					//logger.info(peopleCluster.getId() + " : people cluster repScore : " + peopleCluster.getReputationScore());
					double similarity = peopleCluster.getSimilarity(newEmail.getRecipientContextVector());
					if(similarity > maxFlaggedPeopleSimilarity){
						maxFlaggedPeopleSimilarity = similarity;
						//seenPeopleScore = peopleCluster.getReputationScore() * maxSeenPeopleSimilarity;
					}
				}
				newEmail.setProfileClusterFlagRecipientScore(maxFlaggedPeopleSimilarity);
			}else{
				logger.error("rep-model seen profile people clusters are not created..");
			}
			//spam portion
			double spamTopicSimilarity = EmailUtils.calculateCosineSimilarity(repModel.getSpamVector(), newEmail.getTextContextVector());
			newEmail.setSpamTopicScore(spamTopicSimilarity);
			
			//logger.info("replied topic similarity : " + newEmail.getRepliedTopicscore() + " replied recipient similarity: " + newEmail.getRepliedPeoplescore() );
			logger.info(" profile cluster based reply content score : " + newEmail.getProfileClusterReplyContentScore() +" profile cluster based reply recipient score: " + newEmail.getProfileClusterReplyRecipientScore());
			logger.info(" profile cluster based read content score : " + newEmail.getProfileClusterReadContentScore() +" profile cluster based read recipient score: " + newEmail.getProfileClusterReadRecipientScore());
			logger.info("spam similarity : " + newEmail.getSpamTopicScore());
			
			//normalize sub cluster based email profiles
			//newEmail = EmailUtils.normalizeClusteredProfileBasedEmailScores(newEmail, 0.7);
			//logger.info("After normalizing the scores subcluster profile read score : " + newEmail.getTotalSubClusterBasedReadScore()+" reply score : " + newEmail.getTotalSubClusterBasedReplyScore() + " flag score : " + newEmail.getTotalSubClusterBasedFlagScore());

			//checking if the email require to be replied/read
				
			} else {
				
			System.out.println("The email is not a direct email, calculating similarities with list email profiles");
			double maxTopicSimilarity = 0.0;
			if(repliedListContentClusters !=  null && repliedListContentClusters.size() > 0){
				for(EmailWeightedSubjectBodyContentCluster contentCluster : repliedListContentClusters){
					double similarity = contentCluster.getSimilarity(newEmail.getSubjectContextVector(), newEmail.getBodyContextVector());
					if(similarity > maxTopicSimilarity){
						maxTopicSimilarity = similarity;
					}
				}
				double repliedTopicScore = maxTopicSimilarity;
				newEmail.setProfileClusterReplyContentScore(repliedTopicScore);
				
			}else{
				logger.error("rep-model replied profile content clusters are not created..");
			}
			if(repliedListPeopleClusters != null && repliedListPeopleClusters.size() > 0){
				double maxPeopleSimilarity = 0.0;
				for(EmailRecipientCluster peopleCluster : repliedListPeopleClusters){
					double similarity = peopleCluster.getSimilarity(newEmail.getRecipientContextVector());
					if(similarity > maxPeopleSimilarity){
						maxPeopleSimilarity = similarity;
					}
				}
				double repliedPeopleScore = maxPeopleSimilarity;
				newEmail.setProfileClusterReplyRecipientScore(repliedPeopleScore);
			}else{
				logger.error("rep-model seen profile people clusters are not created..");
			}
			
			//seen emails
			double maxTopicSeenSimilarity = 0.0;
			double seenListTopicScore = 0.0;
			if(seenListContentClusters !=  null && seenListContentClusters.size() > 0){
				for(EmailWeightedSubjectBodyContentCluster contentCluster : seenListContentClusters){
					logger.info(contentCluster.getId() + " : List content cluster repScore : " + contentCluster.getReputationScore());
					
					double similarity = contentCluster.getSimilarity(newEmail.getSubjectContextVector(), newEmail.getBodyContextVector());
					if(similarity > maxTopicSeenSimilarity){
						maxTopicSeenSimilarity = similarity;
						//seenListTopicScore = contentCluster.getReputationScore() * maxTopicSeenSimilarity;
					}
				}
				
				newEmail.setProfileClusterReadContentScore(maxTopicSeenSimilarity);
			}else{
				logger.error("rep-model seen profile content clusters are not created..");
			}
			if(seenListPeopleClusters != null && seenListPeopleClusters.size() > 0){
				double maxSeenPeopleSimilarity = 0.0;
				double seenListPeopleScore = 0.0;
				for(EmailRecipientCluster peopleCluster : seenListPeopleClusters){
					logger.info(peopleCluster.getId() + " : people cluster repScore : " + peopleCluster.getReputationScore());
					double similarity = peopleCluster.getSimilarity(newEmail.getRecipientContextVector());
					if(similarity > maxSeenPeopleSimilarity){
						maxSeenPeopleSimilarity = similarity;
						//seenListPeopleScore = peopleCluster.getReputationScore() * maxSeenPeopleSimilarity;
					
					}
				}
				newEmail.setProfileClusterReadRecipientScore(maxSeenPeopleSimilarity);
			}else{
				logger.error("rep-model seen profile people clusters are not created..");
			}
			
			//flagged emails
			double maxTopicFlaggedSimilarity = 0.0;
			double flaggedTopicScore = 0.0;
			if(flaggedListContentClusters !=  null && flaggedListContentClusters.size() > 0){
				for(EmailWeightedSubjectBodyContentCluster contentCluster : flaggedListContentClusters){
					//logger.info(contentCluster.getId() + " : content cluster repScore : " + contentCluster.getReputationScore());
					double similarity = contentCluster.getSimilarity(newEmail.getSubjectContextVector(), newEmail.getBodyContextVector());
					if(similarity > maxTopicSeenSimilarity){
						maxTopicSeenSimilarity = similarity;
						//seenTopicScore = contentCluster.getReputationScore() * maxTopicSeenSimilarity;
					}
				}
				newEmail.setProfileClusterFlagContentScore(maxTopicSeenSimilarity);
			}else{
				logger.error("rep-model flagged profile content clusters are not created..");
			}
			if(flaggedListPeopleClusters != null && flaggedListPeopleClusters.size() > 0){
				double maxFlaggedPeopleSimilarity = 0.0;
				double flaggedPeopleScore = 0.0;
				for(EmailRecipientCluster peopleCluster : flaggedListPeopleClusters){
					//logger.info(peopleCluster.getId() + " : people cluster repScore : " + peopleCluster.getReputationScore());
					double similarity = peopleCluster.getSimilarity(newEmail.getRecipientContextVector());
					if(similarity > maxFlaggedPeopleSimilarity){
						maxFlaggedPeopleSimilarity = similarity;
						//seenPeopleScore = peopleCluster.getReputationScore() * maxSeenPeopleSimilarity;
					}
				}
				newEmail.setProfileClusterFlagRecipientScore(maxFlaggedPeopleSimilarity);
			}else{
				logger.error("rep-model seen profile people clusters are not created..");
			}
			
				
			double spamTopicSimilarity = EmailUtils.calculateCosineSimilarity(repModel.getSpamVector(), newEmail.getTextContextVector());
			newEmail.setSpamTopicScore(spamTopicSimilarity);
			//logger.info("replied topic similarity : " + newEmail.getRepliedTopicscore() + "replied recipient similarity: " + newEmail.getRepliedPeoplescore());			
		}
		
		double totalReadScore = (newEmail.getProfileClusterReadContentScore() + newEmail.getProfileClusterReadRecipientScore())/2;
		newEmail.setTotalSubClusterBasedReadScore(totalReadScore);
		double totalReplyScore = (newEmail.getProfileClusterReplyContentScore() + newEmail.getProfileClusterReplyRecipientScore())/2;
		newEmail.setTotalSubClusterBasedReplyScore(totalReplyScore);				
		double totalFlagScore = (newEmail.getProfileClusterFlagContentScore() + newEmail.getProfileClusterFlagRecipientScore())/2;
		newEmail.setTotalSubClusterBasedFlagScore(totalFlagScore);

		return newEmail;
	}
	
	
	public Email predictImportanceFromEmailUserProfile(Email newEmail){
		newEmail.setModel(false);
		newEmail.setPredicted(true);
		EmailReputationDataModel repModel = this.getReputationDataModel();
		
		if(newEmail.isSpam()){
			logger.info("The email to recommend reputation is pre-labeled as SPAM. hence adding to the spam list.");
			double[] unimportanttopicsVector = VectorsMath.addArrays(repModel.getSpamVector(), newEmail.getTextContextVector());
			repModel.setSpamVector(unimportanttopicsVector);
			
			logger.info("rep model's spam vector total :  " + EmailUtils.getVectorTotal(repModel.getSpamVector()));
			logger.info("new email's textcontent vector : " + EmailUtils.getVectorTotal(newEmail.getTextContextVector()));
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
			System.out.println("The email is a direct or ccd email and not a list email");
			
			double flaggedTopicscore = EmailUtils.calculateCosineSimilarity(repModel.getImportantTopicsFlagged(), newEmail.getTextContextVector());
			double repliedTopicscore = EmailUtils.calculateCosineSimilarity(repModel.getImportantTopicsReplied(), newEmail.getTextContextVector());
			double seenTopicscore = EmailUtils.calculateCosineSimilarity(repModel.getImportantTopicsOnlySeen(), newEmail.getTextContextVector());
			//double spamTopicSimilarity = EmailUtils.calculateCosineSimilarity(repModel.getSpamVector(), newEmail.getTextContextVector());
			
			//double totalTopicScore = flaggedTopicscore + repliedTopicscore + seenTopicscore - spamTopicSimilarity;
			
			double flaggedTopicSubjectscore = EmailUtils.calculateCosineSimilarity(repModel.getImportantTopicsSubjectsFlagged(), newEmail.getSubjectContextVector());
			double repliedTopicSubjectscore = EmailUtils.calculateCosineSimilarity(repModel.getImportantTopicsSubjectsReplied(), newEmail.getSubjectContextVector());
			double seenTopicSubjectscore = EmailUtils.calculateCosineSimilarity(repModel.getImportantTopicsSubjectsOnlySeen(), newEmail.getSubjectContextVector());
			//double spamTopicSubjectSimilarity = EmailUtils.calculateCosineSimilarity(repModel.getSpamVector(), newEmail.getSubjectContextVector());
			
			//double totalTopicSubjectsScore = flaggedTopicSubjectscore + repliedTopicSubjectscore + seenTopicSubjectscore - spamTopicSubjectSimilarity;
			
			double flaggedTopicBodyscore = EmailUtils.calculateCosineSimilarity(repModel.getImportantTopicsBodyFlagged(), newEmail.getBodyContextVector());
			double repliedTopicBodyscore = EmailUtils.calculateCosineSimilarity(repModel.getImportantTopicsBodyReplied(), newEmail.getBodyContextVector());
			double seenTopicBodyscore = EmailUtils.calculateCosineSimilarity(repModel.getImportantTopicsBodyOnlySeen(), newEmail.getBodyContextVector());
			//double spamTopicBodySimilarity = EmailUtils.calculateCosineSimilarity(repModel.getSpamVector(), newEmail.getBodyContextVector());
			
			//double totalTopicBodyScore = flaggedTopicBodyscore + repliedTopicBodyscore + seenTopicBodyscore - spamTopicBodySimilarity;
			//newEmail.setSpamTopicScore(spamTopicSimilarity);
			//newEmail.setTotalTopicScore(totalTopicScore);
			
			newEmail.setFlaggedTopicSubjectscore(flaggedTopicSubjectscore);
			newEmail.setRepliedTopicSubjectscore(repliedTopicSubjectscore);
			
			newEmail.setSeenTopicSubjectscore(seenTopicSubjectscore);
			//newEmail.setSpamTopicSubjectScore(spamTopicSubjectSimilarity);
			//newEmail.setTotalTopicSubjectScore(totalTopicSubjectsScore);
			
			newEmail.setFlaggedTopicBodyscore(flaggedTopicBodyscore);
			newEmail.setRepliedTopicBodyscore(repliedTopicBodyscore);
			newEmail.setSeenTopicBodyscore(seenTopicBodyscore);
			//newEmail.setSpamTopicBodyScore(spamTopicBodySimilarity);
			//newEmail.setTotalTopicBodyScore(totalTopicBodyScore);
			
			double readTopicScore = (seenTopicSubjectscore + seenTopicBodyscore)/2;
			newEmail.setSeenTopicscore(readTopicScore);
			double replyTopicScore = (repliedTopicSubjectscore + repliedTopicBodyscore)/2;
			newEmail.setRepliedTopicscore(replyTopicScore);
			double flagTopicScore = (flaggedTopicSubjectscore + flaggedTopicBodyscore)/2;
			newEmail.setFlaggedTopicscore(flagTopicScore);
			
			
			
			logger.info("flagged topic similarity : " + flaggedTopicscore + " subject sim : " + flaggedTopicSubjectscore + " body sim : " + flaggedTopicBodyscore);
			logger.info("replied topic similarity : " + repliedTopicscore + " subject sim : " + repliedTopicSubjectscore + " body sim : " + repliedTopicBodyscore);
			logger.info("seen topic similarity : " + seenTopicscore + " subject sim : " + seenTopicSubjectscore + " body sim : " + seenTopicBodyscore);
			//logger.info("spam similarity : " + spamTopicSimilarity  + " subject spam sim : " + spamTopicSubjectSimilarity + " body spam sim : " + spamTopicBodySimilarity);
			//logger.info("Total topic score " + totalTopicScore + " subject total sim : " + totalTopicSubjectsScore + " body total sim : " + totalTopicBodyScore);
			
			
			double flaggedPeoplescore = EmailUtils.calculateCosineSimilarity(repModel.getImportantPeopleFlagged(), newEmail.getRecipientContextVector());
			double repliedPeoplescore = EmailUtils.calculateCosineSimilarity(repModel.getImportantPeopleReplied(), newEmail.getRecipientContextVector());
			double seenPeoplescore = EmailUtils.calculateCosineSimilarity(repModel.getImportantPeopleOnlySeen(), newEmail.getRecipientContextVector());
			//double spamPeopleSimilarity = EmailUtils.calculateCosineSimilarity(repModel.getSpamPeopleVector(), newEmail.getRecipientContextVector());
			
			//double totalPeopleScore = flaggedPeoplescore + repliedPeoplescore + seenPeoplescore - spamPeopleSimilarity;
			
			newEmail.setFlaggedPeoplescore(flaggedPeoplescore);
			newEmail.setRepliedPeoplescore(repliedPeoplescore);
			newEmail.setSeenPeoplescore(seenPeoplescore);
			//newEmail.setSpamPeopleScore(spamPeopleSimilarity);
			//newEmail.setTotalPeopleScore(totalPeopleScore);
			
			double flaggedPeopleToscore = EmailUtils.calculateCosineSimilarity(repModel.getImportantPeopleToFlagged(), newEmail.getToContextVector());
			double repliedPeopleToscore = EmailUtils.calculateCosineSimilarity(repModel.getImportantPeopleToReplied(), newEmail.getToContextVector());
			double seenPeopleToscore = EmailUtils.calculateCosineSimilarity(repModel.getImportantPeopleToOnlySeen(), newEmail.getToContextVector());
			//double spamPeopleToSimilarity = EmailUtils.calculateCosineSimilarity(repModel.getSpamPeopleVector(), newEmail.getToContextVector());
			
			//double totalPeopleToScore = flaggedPeopleToscore + repliedPeopleToscore + seenPeopleToscore - spamPeopleToSimilarity;
			
			newEmail.setFlaggedPeopleToscore(flaggedPeopleToscore);
			newEmail.setRepliedPeopleToscore(repliedPeopleToscore);
			newEmail.setSeenPeopleToscore(seenPeopleToscore);
			//newEmail.setSpamPeopleToScore(spamPeopleToSimilarity);
			//newEmail.setTotalPeopleToScore(totalPeopleToScore);
			
			double flaggedPeopleFromscore = EmailUtils.calculateCosineSimilarity(repModel.getImportantPeopleFromFlagged(), newEmail.getFromContextVector());
			double repliedPeopleFromscore = EmailUtils.calculateCosineSimilarity(repModel.getImportantPeopleFromReplied(), newEmail.getFromContextVector());
			double seenPeopleFromscore = EmailUtils.calculateCosineSimilarity(repModel.getImportantPeopleFromOnlySeen(), newEmail.getFromContextVector());
			//double spamPeopleFromSimilarity = EmailUtils.calculateCosineSimilarity(repModel.getSpamPeopleVector(), newEmail.getFromContextVector());
			
			//double totalPeopleFromScore = flaggedPeopleFromscore + repliedPeopleFromscore + seenPeopleFromscore - spamPeopleFromSimilarity;
			
			newEmail.setFlaggedPeopleFromscore(flaggedPeopleFromscore);
			newEmail.setRepliedPeopleFromscore(repliedPeopleFromscore);
			newEmail.setSeenPeopleFromscore(seenPeopleFromscore);
			//newEmail.setSpamPeopleFromScore(spamPeopleFromSimilarity);
			//newEmail.setTotalPeopleFromScore(totalPeopleFromScore);

			double flaggedPeopleCCscore = EmailUtils.calculateCosineSimilarity(repModel.getImportantPeopleCCFlagged(), newEmail.getCcContextVector());
			double repliedPeopleCCscore = EmailUtils.calculateCosineSimilarity(repModel.getImportantPeopleCCReplied(), newEmail.getCcContextVector());
			double seenPeopleCCscore = EmailUtils.calculateCosineSimilarity(repModel.getImportantPeopleCCOnlySeen(), newEmail.getCcContextVector());
			//double spamPeopleCCSimilarity = EmailUtils.calculateCosineSimilarity(repModel.getSpamPeopleVector(), newEmail.getCcContextVector());
			
			//double totalPeopleCCScore = flaggedPeopleCCscore + repliedPeopleCCscore + seenPeopleCCscore - spamPeopleCCSimilarity;
			
			newEmail.setFlaggedPeopleCCscore(flaggedPeopleCCscore);
			newEmail.setRepliedPeopleCCscore(repliedPeopleCCscore);
			newEmail.setSeenPeopleCCscore(seenPeopleCCscore);
			//newEmail.setSpamPeopleCCScore(spamPeopleCCSimilarity);
			//newEmail.setTotalPeopleCCScore(totalPeopleCCScore);

//
			logger.info("flagged recipient similarity : " + flaggedPeoplescore + " flagged To people sim : " + flaggedPeopleToscore + " flagged From people sim : " + flaggedPeopleFromscore
					+ " flagged Cc people sim : " + flaggedPeopleCCscore);
			logger.info("replied recipient similarity: " + repliedPeoplescore + " replied To people sim : " + repliedPeopleToscore + " replied From people sim : " + repliedPeopleFromscore
					+ " replied Cc people sim : " + repliedPeopleCCscore );
			logger.info("seen recipient similarity: " + seenPeoplescore + " seen To people sim : " + seenPeopleToscore + " seen From people sim : " + seenPeopleFromscore
					+ " seen Cc people sim : " + seenPeopleCCscore );
			//logger.info("Total recipient similarity: " + totalPeopleScore);
			

			//normalizing profile scores for direct/ccd emails
			//newEmail = EmailUtils.normalizeUserProfileBasedEmailScores(newEmail, 0.3);
			
		}else {
			System.out.println("The email is not a direct email, calculating similarities with list email profiles");

			double flaggedTopicscore = EmailUtils.calculateCosineSimilarity(repModel.getImportantListTopicsFlagged(), newEmail.getTextContextVector());
			double repliedTopicscore = EmailUtils.calculateCosineSimilarity(repModel.getImportantListTopicsReplied(), newEmail.getTextContextVector());
			double seenTopicscore = EmailUtils.calculateCosineSimilarity(repModel.getImportantListTopicsOnlySeen(), newEmail.getTextContextVector());
			//double spamTopicSimilarity = EmailUtils.calculateCosineSimilarity(repModel.getSpamVector(), newEmail.getTextContextVector());
			
			//double totalTopicScore = flaggedTopicscore + repliedTopicscore + seenTopicscore - spamTopicSimilarity;
			
			double flaggedTopicSubjectscore = EmailUtils.calculateCosineSimilarity(repModel.getImportantListTopicsSubjectsFlagged(), newEmail.getSubjectContextVector());
			double repliedTopicSubjectscore = EmailUtils.calculateCosineSimilarity(repModel.getImportantListTopicsSubjectsReplied(), newEmail.getSubjectContextVector());
			double seenTopicSubjectscore = EmailUtils.calculateCosineSimilarity(repModel.getImportantListTopicsSubjectsOnlySeen(), newEmail.getSubjectContextVector());
			//double spamTopicSubjectSimilarity = EmailUtils.calculateCosineSimilarity(repModel.getSpamVector(), newEmail.getSubjectContextVector());
			
			//double totalTopicSubjectsScore = flaggedTopicSubjectscore + repliedTopicSubjectscore + seenTopicSubjectscore - spamTopicSubjectSimilarity;
			
			double flaggedTopicBodyscore = EmailUtils.calculateCosineSimilarity(repModel.getImportantListTopicsBodyFlagged(), newEmail.getBodyContextVector());
			double repliedTopicBodyscore = EmailUtils.calculateCosineSimilarity(repModel.getImportantListTopicsBodyReplied(), newEmail.getBodyContextVector());
			double seenTopicBodyscore = EmailUtils.calculateCosineSimilarity(repModel.getImportantListTopicsBodyOnlySeen(), newEmail.getBodyContextVector());
			//double spamTopicBodySimilarity = EmailUtils.calculateCosineSimilarity(repModel.getSpamVector(), newEmail.getBodyContextVector());
			
			//double totalTopicBodyScore = flaggedTopicBodyscore + repliedTopicBodyscore + seenTopicBodyscore - spamTopicBodySimilarity;	
			//newEmail.setSpamTopicScore(spamTopicSimilarity);
			//newEmail.setTotalTopicScore(totalTopicScore);
			
			newEmail.setFlaggedTopicSubjectscore(flaggedTopicSubjectscore);
			newEmail.setRepliedTopicSubjectscore(repliedTopicSubjectscore);
			newEmail.setSeenTopicSubjectscore(seenTopicSubjectscore);
			//newEmail.setSpamTopicSubjectScore(spamTopicSubjectSimilarity);
			//newEmail.setTotalTopicSubjectScore(totalTopicSubjectsScore);
			
			newEmail.setFlaggedTopicBodyscore(flaggedTopicBodyscore);
			newEmail.setRepliedTopicBodyscore(repliedTopicBodyscore);
			newEmail.setSeenTopicBodyscore(seenTopicBodyscore);
			//newEmail.setSpamTopicBodyScore(spamTopicBodySimilarity);
			//newEmail.setTotalTopicBodyScore(totalTopicBodyScore);
			

			double readTopicScore = (seenTopicSubjectscore + seenTopicBodyscore)/2;
			newEmail.setSeenTopicscore(readTopicScore);
			double replyTopicScore = (repliedTopicSubjectscore + repliedTopicBodyscore)/2;
			newEmail.setRepliedTopicscore(replyTopicScore);
			double flagTopicScore = (flaggedTopicSubjectscore + flaggedTopicBodyscore)/2;
			newEmail.setFlaggedTopicscore(flagTopicScore);
											
			logger.info("flagged topic similarity : " + flaggedTopicscore + " subject sim : " + flaggedTopicSubjectscore + " body sim : " + flaggedTopicBodyscore);
			logger.info("replied topic similarity : " + repliedTopicscore + " subject sim : " + repliedTopicSubjectscore + " body sim : " + repliedTopicBodyscore);
			logger.info("seen topic similarity : " + seenTopicscore + " subject sim : " + seenTopicSubjectscore + " body sim : " + seenTopicBodyscore);
			//logger.info("Total topic score " + totalTopicScore + " subject total sim : " + totalTopicSubjectsScore + " body total sim : " + totalTopicBodyScore);
			

			double flaggedPeoplescore = EmailUtils.calculateCosineSimilarity(repModel.getImportantListPeopleFlagged(), newEmail.getRecipientContextVector());
			double repliedPeoplescore = EmailUtils.calculateCosineSimilarity(repModel.getImportantListPeopleReplied(), newEmail.getRecipientContextVector());
			double seenPeoplescore = EmailUtils.calculateCosineSimilarity(repModel.getImportantListPeopleOnlySeen(), newEmail.getRecipientContextVector());
			//double spamPeopleSimilarity = EmailUtils.calculateCosineSimilarity(repModel.getSpamPeopleVector(), newEmail.getRecipientContextVector());
			
			//double totalPeopleScore = flaggedPeoplescore + repliedPeoplescore + seenPeoplescore - spamPeopleSimilarity;
			
			newEmail.setFlaggedPeoplescore(flaggedPeoplescore);
			newEmail.setRepliedPeoplescore(repliedPeoplescore);
			newEmail.setSeenPeoplescore(seenPeoplescore);
			//newEmail.setSpamPeopleScore(spamPeopleSimilarity);
			//newEmail.setTotalPeopleScore(totalPeopleScore);
			
			double flaggedPeopleToscore = EmailUtils.calculateCosineSimilarity(repModel.getImportantListPeopleToFlagged(), newEmail.getToContextVector());
			double repliedPeopleToscore = EmailUtils.calculateCosineSimilarity(repModel.getImportantListPeopleToReplied(), newEmail.getToContextVector());
			double seenPeopleToscore = EmailUtils.calculateCosineSimilarity(repModel.getImportantListPeopleToOnlySeen(), newEmail.getToContextVector());
			//double spamPeopleToSimilarity = EmailUtils.calculateCosineSimilarity(repModel.getSpamPeopleVector(), newEmail.getToContextVector());		
			//double totalPeopleToScore = flaggedPeopleToscore + repliedPeopleToscore + seenPeopleToscore - spamPeopleToSimilarity;
			
			newEmail.setFlaggedPeopleToscore(flaggedPeopleToscore);
			newEmail.setRepliedPeopleToscore(repliedPeopleToscore);
			newEmail.setSeenPeopleToscore(seenPeopleToscore);
			//newEmail.setSpamPeopleToScore(spamPeopleToSimilarity);
			//newEmail.setTotalPeopleToScore(totalPeopleToScore);
			
			double flaggedPeopleFromscore = EmailUtils.calculateCosineSimilarity(repModel.getImportantListPeopleFromFlagged(), newEmail.getFromContextVector());
			double repliedPeopleFromscore = EmailUtils.calculateCosineSimilarity(repModel.getImportantListPeopleFromReplied(), newEmail.getFromContextVector());
			double seenPeopleFromscore = EmailUtils.calculateCosineSimilarity(repModel.getImportantListPeopleFromOnlySeen(), newEmail.getFromContextVector());
			//double spamPeopleFromSimilarity = EmailUtils.calculateCosineSimilarity(repModel.getSpamPeopleVector(), newEmail.getFromContextVector());
			
			//double totalPeopleFromScore = flaggedPeopleFromscore + repliedPeopleFromscore + seenPeopleFromscore - spamPeopleFromSimilarity;
			
			newEmail.setFlaggedPeopleFromscore(flaggedPeopleFromscore);
			newEmail.setRepliedPeopleFromscore(repliedPeopleFromscore);
			newEmail.setSeenPeopleFromscore(seenPeopleFromscore);
			//newEmail.setSpamPeopleFromScore(spamPeopleFromSimilarity);
			//newEmail.setTotalPeopleFromScore(totalPeopleFromScore);

			double flaggedPeopleCCscore = EmailUtils.calculateCosineSimilarity(repModel.getImportantListPeopleCCFlagged(), newEmail.getCcContextVector());
			double repliedPeopleCCscore = EmailUtils.calculateCosineSimilarity(repModel.getImportantListPeopleCCReplied(), newEmail.getCcContextVector());
			double seenPeopleCCscore = EmailUtils.calculateCosineSimilarity(repModel.getImportantListPeopleCCOnlySeen(), newEmail.getCcContextVector());
			//double spamPeopleCCSimilarity = EmailUtils.calculateCosineSimilarity(repModel.getSpamPeopleVector(), newEmail.getCcContextVector());
			
			//double totalPeopleCCScore = flaggedPeopleCCscore + repliedPeopleCCscore + seenPeopleCCscore - spamPeopleCCSimilarity;
			
			newEmail.setFlaggedPeopleCCscore(flaggedPeopleCCscore);
			newEmail.setRepliedPeopleCCscore(repliedPeopleCCscore);
			newEmail.setSeenPeopleCCscore(seenPeopleCCscore);
			//newEmail.setSpamPeopleCCScore(spamPeopleCCSimilarity);
			//newEmail.setTotalPeopleCCScore(totalPeopleCCScore);

//
			logger.info("flagged recipient similarity : " + flaggedPeoplescore + " flagged To people sim : " + flaggedPeopleToscore + " flagged From people sim : " + flaggedPeopleFromscore
					+ " flagged Cc people sim : " + flaggedPeopleCCscore);
			logger.info("replied recipient similarity: " + repliedPeoplescore + " replied To people sim : " + repliedPeopleToscore + " replied From people sim : " + repliedPeopleFromscore
					+ " replied Cc people sim : " + repliedPeopleCCscore );
			logger.info("seen recipient similarity: " + seenPeoplescore + " seen To people sim : " + seenPeopleToscore + " seen From people sim : " + seenPeopleFromscore
					+ " seen Cc people sim : " + seenPeopleCCscore );
		}

		double totalReadScore = (newEmail.getSeenTopicscore() + newEmail.getSeenPeoplescore())/2;
		newEmail.setTotalProfileBasedReadScore(totalReadScore);
		double totalReplyScore = (newEmail.getRepliedTopicscore() + newEmail.getRepliedPeoplescore())/2;
		newEmail.setTotalProfileBasedReplyScore(totalReplyScore);
		double totalFlaggedScore = (newEmail.getFlaggedTopicscore() + newEmail.getFlaggedPeoplescore())/2;
		newEmail.setTotalProfileBasedFlagScore(totalFlaggedScore);
		
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
			//List<EmailContentCluster> contentClusters = new ArrayList<EmailContentCluster>();
			List<EmailWeightedSubjectBodyContentCluster> subjectBodyClusters = new ArrayList<EmailWeightedSubjectBodyContentCluster>();
			List<EmailRecipientCluster> recipientClusters = new ArrayList<EmailRecipientCluster>();
			/*
			 * List<Email> contentClusterEmails = new ArrayList<Email>();
			 * List<Email> recipientClusterEmails = new ArrayList<Email>();
			 * List<String> contentEmailIds = new ArrayList<String>();
			 * List<String> recipientEmailIds = new ArrayList<String>();
			 */

//			EmailContentCluster contentCluster = new EmailContentCluster("c"
//					+ String.valueOf(0));
			EmailWeightedSubjectBodyContentCluster contentCluster = new EmailWeightedSubjectBodyContentCluster("sb"+String.valueOf(0));
			contentCluster.addEmail(email.getMessageId(), email);
			subjectBodyClusters.add(contentCluster);
			model.setWeightedSubjectBodyClusters(subjectBodyClusters);
			logger.info("Created new subject body content cluster with id : "
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
//			List<EmailContentCluster> newContentClusters = KMeansClustering
//					.classifyNewEmailByContent(email,
//							model.getContentClusters());
			List<EmailWeightedSubjectBodyContentCluster> newSubjectBodyClusters = KMeansClustering.
											classifyNewEmailBySubjectBody(email, model.getWeightedSubjectBodyClusters());
			
			List<EmailRecipientCluster> newRecipientClusters = KMeansClustering
					.classifyNewEmailByPeople(email,
							model.getRecipientClusters());

			logger.info("ReputationScores of added email : "
					+ email.getMessageId() + " content cluster id :"
					+ email.getWeightedSubjectBodyClusterId() + " contentReputation : "
					+ email.getContentReputationScore()
					+ " people cluster id : " + email.getPeopleClusterId()
					+ " peopleReputation : "
					+ email.getRecipientReputationScore());
			// logger.info("Email was added to content cluster :" +
			// email.getTextClusterId() + " , recipient cluster : " +
			// email.getPeopleClusterId());
			model.setWeightedSubjectBodyClusters(newSubjectBodyClusters);
			model.setRecipientClusters(newRecipientClusters);
			this.setReputationDataModel(model);
			logger.info("After classifying the new email, the size of the mailbox's content clusters : "
					+ model.getWeightedSubjectBodyClusters().size());
			logger.info("After classifying the new email, the size of the mailbox's recipient clusters : "
					+ model.getRecipientClusters().size());

		}

		return email;
	}

	

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
		
		
		logger.info("Dunn index for content clusters : " + model.calculateDunnIndexForContentClusters());
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

	//@javax.jdo.annotations.Persistent
	//@javax.jdo.annotations.Column(allowsNull = "true") 
	public List<RandomIndexVector> getContentVectors() {
		return contentVectors;
	}

	public void setContentVectors(List<RandomIndexVector> contentVectors) {
		this.contentVectors = contentVectors;
	}
	
	//@javax.jdo.annotations.Persistent
	//@javax.jdo.annotations.Column(allowsNull = "true") 
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

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true") 
	public int getCurrentModelSize() {
		return currentModelSize;
	}

	public void setCurrentModelSize(int currentModelSize) {
		this.currentModelSize = currentModelSize;
	}
	
	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true") 
	public List<Long> getMarkedImportantEmailUids() {
		return markedImportantEmailUids;
	}

	public void setMarkedImportantEmailUids(List<Long> markedImportantEmailUids) {
		this.markedImportantEmailUids = markedImportantEmailUids;
	}

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true") 
	public List<Long> getMarkedSpamEmailUids() {
		return markedSpamEmailUids;
	}

	public void setMarkedSpamEmailUids(List<Long> markedSpamEmailUids) {
		this.markedSpamEmailUids = markedSpamEmailUids;
	}

	@Hidden
	@Programmatic
	public Map<Long,Email> getEmailMap(){
//		if(this.emailMap != null){
//			logger.info("the emailMap of this mailbox is already initialized. hence retuning it");
//			return this.emailMap;
//		}else{
			Map<Long, Email> emails = new HashMap<Long, Email>();
			for(Email mail: this.getAllEmails()){
				Long uid = mail.getMsgUid();
				emails.put(uid, mail);
			}
			logger.info("initializing the emailMap and returning..");
			this.emailMap = emails;
			logger.info("returning mailbox's emailmap with size : " + emailMap.size());
			return this.emailMap;
//		}
	}

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true") 
	public int getMailToRetrieveForPeriod() {
		return mailToRetrieveForPeriod;
	}

	public void setMailToRetrieveForPeriod(int mailToRetrieveForPeriod) {
		this.mailToRetrieveForPeriod = mailToRetrieveForPeriod;
	}

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public long getLastPredictedMsgUidSentToClient() {
		return lastPredictedMsgUidSentToClient;
	}

	public void setLastPredictedMsgUidSentToClient(
			long lastPredictedMsgUidSentToClient) {
		this.lastPredictedMsgUidSentToClient = lastPredictedMsgUidSentToClient;
	}
	
}
