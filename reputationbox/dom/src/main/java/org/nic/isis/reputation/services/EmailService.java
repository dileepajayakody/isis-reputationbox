package org.nic.isis.reputation.services;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.nic.isis.clustering.EmailContentCluster;
import org.nic.isis.clustering.EmailRecipientCluster;
import org.nic.isis.reputation.dom.Email;
import org.nic.isis.reputation.dom.EmailReputationDataModel;
import org.nic.isis.reputation.dom.UserMailBox;
import org.nic.isis.reputation.utils.EmailUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.isis.applib.DomainObjectContainer;
import org.apache.isis.applib.annotation.Hidden;
import org.apache.isis.applib.annotation.Named;
import org.apache.isis.applib.annotation.Optional;
import org.apache.isis.applib.annotation.Programmatic;
import org.apache.isis.applib.query.QueryDefault;
import org.apache.isis.applib.value.Password;
import org.apache.isis.objectstore.jdo.applib.service.support.IsisJdoSupport;

public class EmailService {

	private final static Logger logger = LoggerFactory
			.getLogger(EmailService.class);
	
	@Named("Update MailBox Model and Predict for new emails")
	public synchronized List<UserMailBox> updateNew(){
		List<UserMailBox> mailBoxes = listAllMailBoxes();
		if (mailBoxes == null || mailBoxes.isEmpty()) {
			logger.info("No mail-box added to the application. Please add a new mailbox via the web console");
		
		}else {
			for (UserMailBox mailBox : mailBoxes) {
				//if(!mailBox.isUpdatingModel() && (mailBox.getAllEmails().size() == 0)){
				if(!mailBox.isUpdatingModel() && (mailBox.getAllEmails().size() < 300)){
						
					logger.info("No emails loaded to mailbox..creating the importance model..");
					javaMailService.createImportanceModel(mailBox);
					
				//} else if(!mailBox.isUpdatingModel() && (mailBox.getAllEmails().size() > 0)){
				} else if(!mailBox.isUpdatingModel() && (mailBox.getAllEmails().size() >= 300)){
						
					logger.info("Updaing mailbox and predicting email importance for emails for mailbox : " + mailBox.getEmailId() + " since email count: " + mailBox.getAllEmails().size()
							+ " last indexed email  UID : " + mailBox.getLastIndexedMsgUid());
					javaMailService.predictImportanceForNewEmails(mailBox);
				}
				
				//mailbox needs to be persisted.
				//container.persist(mailBox);
			}	
		}
		container.flush();
		return mailBoxes;
	}

	
	@Named("Update MailBoxes")
	@Hidden
	public synchronized List<UserMailBox> updateMailBoxes(){
		
		List<UserMailBox> mailBoxes = listAllMailBoxes();	
		if (mailBoxes == null || mailBoxes.isEmpty()) {
			//logger.info("There is no mailboxes in test-datastore. creating test mailboxes for connected accounts in contextio");
			//mailBoxes = new ArrayList<UserMailBox>();
			//mailBoxes.add(createSample());
			logger.info("No mail-box added to the application. Please add a new mailbox via the web console");
		}else {
			for (UserMailBox mailBox : mailBoxes) {
				logger.info("Updaing mailbox : " + mailBox.getEmailId() + " since email count: " + mailBox.getAllEmails().size()
						+ " last indexed email  UID : " + mailBox.getLastIndexedMsgUid());
				try {
						javaMailService.updateMailBox(mailBox);
						logger.info("updated the mailBox: " + mailBox.getEmailId()
								+ " with " + mailBox.getAllEmails().size() + " emails");
				
				//printing mb cluster results..
				//printClusters(mailBox);
				
				} catch (Exception e) {
					logger.error("Error occurred  ", e);
				}
				//mailbox needs to be persisted.
				//container.persist(mailBox);
			}	
		}
		container.flush();
		return mailBoxes;
	}
	/**
	 * sync all mailboxes with new emails since last offset (email count)
	 */
	@Deprecated
	@Hidden
	@Programmatic
	public void syncMailBoxes(@Named("Max.Emails to retrieve") int maxEmailsNo) {
		//List<UserMailBox> allMailBoxes = listAllMailBoxes();
		//List<UserMailBox> allMailBoxes = null;
		List<UserMailBox> mailBoxes = listAllMailBoxes();
		
		if (mailBoxes == null || mailBoxes.isEmpty()) {
			logger.info("There is no mailboxes in test-datastore. creating test mailboxes for connected accounts in contextio");
			mailBoxes = new ArrayList<UserMailBox>();
			mailBoxes.add(createSample());
		}

		if (mailBoxes == null || mailBoxes.isEmpty()) {
			//logger.info("There is no mailboxes in test-datastore. creating test mailboxes for connected accounts in contextio");
			//mailBoxes = new ArrayList<UserMailBox>();
			//mailBoxes.add(createSample());
			logger.info("No mail-box added to the application. Please add a new mailbox via the web console");
		}else{
			for (UserMailBox mailBox : mailBoxes) {
				logger.info("Updaing mailbox : " + mailBox.getEmailId() + " mailbox added timestamp : " + mailBox.getMailBoxAddedDateTimeStamp() +" since email count: " + mailBox.getAllEmails().size());
				try {
				//for testing mailcounts.... have to uncomment	
				//	if (!mailBox.isSyncing()) {
					
						mailBox.setSyncing(true);
						//iterate all emails (testing condition : && mailBox.getAllEmails().size() < maxEmailsNo needs to be removed at live)
						while (mailBox.isSyncing() && mailBox.getAllEmails().size() < maxEmailsNo) {
							mailBox = contextIOService.updateMailBox(mailBox, 20, 1, 
									mailBox.getMailBoxAddedDateTimeStamp());
							container.flush();
						}
						logger.info("updated the mailBox: " + mailBox.getEmailId()
								+ " with " + mailBox.getAllEmails().size() + " emails");
				//	}
					
				//printing mb cluster results..
				//printClusters(mailBox);
				
				} catch (Exception e) {
					logger.error("Error occurred  ", e);
				}
				
				//mailbox needs to be persisted.
				//container.persist(mailBox);
			}
		}
			
		container.flush();
	}
	
	
	@Deprecated
	@Programmatic
	public void updateEmailModels(){
		List<UserMailBox> mailBoxes = listAllMailBoxes();
		
		for(UserMailBox mb: mailBoxes){
			String mailboxID = mb.getEmailId();
			
			logger.info("Updating Email Model for mailBox : " + mailboxID + " with emails count : " + mb.getAllEmails().size());
			
			long dateMonthAgo = EmailUtils.getMonthsBeforeDateTimeStamp(2);
			long todayTime = EmailUtils.getTodayTimestamp();
			
			List<Email> emailsForLastMonth = getEmailsForTimePeriod(dateMonthAgo, todayTime, mailboxID);
			logger.info("Emails clustered according to text cooccurence.");
			List<EmailContentCluster> contentClusters = emailAnalysisService.kMeansClusterText(emailsForLastMonth);
			
			logger.info("\n\nEmails clustered according to people cooccurence");
			List<EmailRecipientCluster> recipientClusters = emailAnalysisService.kMeansClusterRecipients(emailsForLastMonth);
			
			//creating a new periodic reputationDataModel for the mailbox
			EmailReputationDataModel newDataModel = new EmailReputationDataModel();
			newDataModel.setContentClusters(contentClusters);
			newDataModel.setRecipientClusters(recipientClusters);
			mb.setReputationDataModel(newDataModel);
		
		}
		container.flush();
	}

	
	@Named("Cluster all emails for topics and people")
	public void batchClusterEmails(){
		List<UserMailBox> mailBoxes = listAllMailBoxes();
		for (UserMailBox mb : mailBoxes){
			EmailReputationDataModel model = mb.getReputationDataModel();
			
			List<Email> allEmails = mb.getAllEmails();
			List<EmailContentCluster> contentClusters = emailAnalysisService.kMeansClusterText(allEmails);
			List<EmailRecipientCluster> recipientClusters = emailAnalysisService.kMeansClusterRecipients(allEmails);
			model.setContentClusters(contentClusters);
			model.setRecipientClusters(recipientClusters);
			logger.info("Dunn index for content clusters : " + model.getDunnIndexForContentClusters());
		}
		container.flush();

	}
	
	@Named("Print Email Importance Models")
	public void printImportanceModels(){
		List<UserMailBox> mailBoxes = listAllMailBoxes();
		for (UserMailBox mb : mailBoxes){
			EmailReputationDataModel model = mb.getReputationDataModel();
			
			//flagged
			double[] importantTopicsFlaggedProfile = mb.getImportantTopicsFlagged();
			double[] importantPeopleFlaggedProfile = mb.getImportantPeopleFlagged();
			double[] importantNLPKeywordsFlaggedProfile = mb.getImportantNLPKeywordsFlagged();
			
			double flaggedTopicProfileScore = EmailUtils.getVectorTotal(importantTopicsFlaggedProfile);
			logger.info(" flagged topic profile vector sum :" + flaggedTopicProfileScore +" no : " + mb.getNumberOfDirectEmailsFlagged());
			double flaggedPeopleProfileScore = EmailUtils.getVectorTotal(importantPeopleFlaggedProfile);
			logger.info(" flagged people profile vector sum :" + flaggedPeopleProfileScore );
			double flaggedNLPProfileScore = EmailUtils.getVectorTotal(importantNLPKeywordsFlaggedProfile);
			logger.info(" flagged keywords profile vector sum :" + flaggedNLPProfileScore);
			
			double[] importantListTopicsFlaggedProfile = mb.getImportantListTopicsFlagged();
			double[] importantListPeopleFlaggedProfile = mb.getImportantListPeopleFlagged();
			double[] importantListNLPKeywordsFlaggedProfile = mb.getImportantListNLPKeywordsFlagged();
			
			double flaggedListTopicProfileScore = EmailUtils.getVectorTotal(importantListTopicsFlaggedProfile);
			logger.info("List flagged topic profile vector sum :" + flaggedListTopicProfileScore + " no : " + mb.getNumberOfListEmailsFlagged());
			double flaggedListPeopleProfileScore = EmailUtils.getVectorTotal(importantListPeopleFlaggedProfile);
			logger.info("List flagged people profile vector sum :" + flaggedListPeopleProfileScore);
			double flaggedListNLPProfileScore = EmailUtils.getVectorTotal(importantListNLPKeywordsFlaggedProfile);
			logger.info("List flagged keywords profile vector sum :" + flaggedListNLPProfileScore);
			
			
			
			double[] importantTopicsRepliedProfile = mb.getImportantTopicsReplied();
			double[] importantPeopleRepliedProfile = mb.getImportantPeopleReplied();
			double[] importantNLPKeywordsRepliedProfile = mb.getImportantNLPKeywordsReplied();
			
			
			double repliedTopicProfileScore = EmailUtils.getVectorTotal(importantTopicsRepliedProfile);
			logger.info(" replied topic profile vector sum :" + repliedTopicProfileScore + " no : " + mb.getNumberOfDirectEmailsReplied());
			double repliedPeopleProfileScore = EmailUtils.getVectorTotal(importantPeopleRepliedProfile);
			logger.info(" replied people profile vector sum :" + repliedPeopleProfileScore);
			double repliedNLPProfileScore = EmailUtils.getVectorTotal(importantNLPKeywordsRepliedProfile);
			logger.info(" replied keywords profile vector sum :" + repliedNLPProfileScore);
			
			
			double[] importantListTopicsRepliedProfile = mb.getImportantListTopicsReplied();
			double[] importantListPeopleRepliedProfile = mb.getImportantListPeopleReplied();
			double[] importantListNLPKeywordsRepliedProfile = mb.getImportantListNLPKeywordsReplied();
			
			double repliedListTopicProfileScore = EmailUtils.getVectorTotal(importantListTopicsRepliedProfile);
			logger.info("List replied topic profile vector sum :" + repliedListTopicProfileScore + " no : " + mb.getNumberOfListEmailsReplied());
			double repliedListPeopleProfileScore = EmailUtils.getVectorTotal(importantListPeopleRepliedProfile);
			logger.info("List replied people profile vector sum :" + repliedListPeopleProfileScore);
			double repliedListNLPProfileScore = EmailUtils.getVectorTotal(importantListNLPKeywordsRepliedProfile);
			logger.info("List replied keywords profile vector sum :" + repliedListNLPProfileScore);
			
			
			double[] importantTopicsSeenProfile = mb.getImportantTopicsOnlySeen();
			double[] importantPeopleSeenProfile = mb.getImportantPeopleOnlySeen();
			double[] importantNLPKeywordsSeenProfile = mb.getImportantNLPKeywordsOnlySeen();
			
			double seenTopicProfileScore = EmailUtils.getVectorTotal(importantTopicsSeenProfile);
			logger.info("seen topic profile vector sum :" + seenTopicProfileScore+ " no :" + mb.getNumberOfDirectEmailsSeen());
			double seenPeopleProfileScore = EmailUtils.getVectorTotal(importantPeopleSeenProfile);
			logger.info("seen people profile vector sum :" + seenPeopleProfileScore);
			double seenNLPProfileScore = EmailUtils.getVectorTotal(importantNLPKeywordsSeenProfile);
			logger.info("seen keywords profile vector sum :" + seenNLPProfileScore);
			
			
			double[] importantListTopicsSeenProfile = mb.getImportantListTopicsOnlySeen();
			double[] importantListPeopleSeenProfile = mb.getImportantListPeopleOnlySeen();
			double[] importantListNLPKeywordsSeenProfile = mb.getImportantListNLPKeywordsOnlySeen();
			
			double seenListTopicProfileScore = EmailUtils.getVectorTotal(importantListTopicsSeenProfile);
			logger.info("List seen topic profile vector sum :" + seenListTopicProfileScore + " no : " + mb.getNumberOfListEmailsSeen());
			double seenListPeopleProfileScore = EmailUtils.getVectorTotal(importantListPeopleSeenProfile);
			logger.info("List seen people profile vector sum :" + seenListPeopleProfileScore);
			double seenListNLPProfileScore = EmailUtils.getVectorTotal(importantListNLPKeywordsSeenProfile);
			logger.info("List seen keywords profile vector sum :" + seenListNLPProfileScore);
			
			
			double[] spamTopicsVector = mb.getUnimportanttopicsVector();
			double[] spamPeopleVector = mb.getUnimportantPeopleVector();
			double[] spamSAVector = mb.getUnimportantSpeechActVector();
			double[] spamKeywordsVector = mb.getUnimportantDirectKeywordVector();
			
			double spamTopicProfileScore = EmailUtils.getVectorTotal(spamTopicsVector);
			logger.info("spam topic profile vector sum :" + spamTopicProfileScore + " no : " + mb.getNofOfUnimportantEmails());
			double spamPeopleProfileScore = EmailUtils.getVectorTotal(spamPeopleVector);
			logger.info("spam replied people profile vector sum :" + spamPeopleProfileScore);
			double spamNLPProfileScore = EmailUtils.getVectorTotal(spamKeywordsVector);
			logger.info("spam replied keywords profile vector sum :" + spamNLPProfileScore);
			
			
			logger.info("Dunn index for content clusters : " + model.getDunnIndexForContentClusters());
		}
		
	}
	
	@Programmatic
	private void printClusters(UserMailBox mb){
		logger.info("============================================================================");
		logger.info("Cluster results for mailbox : " + mb.getEmailId() + " with emails : " + mb.getAllEmails().size());
		EmailReputationDataModel model = mb.getReputationDataModel();
		List<EmailContentCluster> contentClusters = model.getContentClusters();
		List<EmailRecipientCluster> recipientClusters = model.getRecipientClusters();
		
		logger.info("Total number of content clusters : " + contentClusters.size());
		int totalEmailCount = 0;
		double sumOfSquaredError = 0;
		
		for(EmailContentCluster contentCluster : contentClusters){
			int flaggedNo = contentCluster.getNoOfMessagesFlagged();
			int answererdNo = contentCluster.getNoOfMessagesAnswered();
			int seenNo = contentCluster.getNoOfMessagesSeen();
			int deletedNo = contentCluster.getNoOfMessagesDeleted();
			double repScore = contentCluster.getReputationScore();
			//int totalEmailsNo = contentCluster.getEmails().size();
			int totalEmailsNo = contentCluster.getContentEmails().size();
			
			logger.info("content cluster id : " + contentCluster.getId() 
					+ " total emails : " + totalEmailsNo
					+ " no of msgs flagged : " + flaggedNo 
					+ " no of msgs answered : " + answererdNo
					+ " no of msgs seen : " + seenNo
					+ " no of msgs deleted : " + deletedNo
					+ " rep.score " + repScore);
			logger.info("Added emails....");
			for(Email email : contentCluster.getContentEmails()){
				logger.info("Email subject : " + email.getSubject());
			}
			logger.info("\n\n");
			totalEmailCount += totalEmailsNo;
			sumOfSquaredError += contentCluster.getSumOfSquaresError();
		}
		logger.info("Total emails in all content clusters : " + totalEmailCount + " in no. of clusters : " + contentClusters.size() 
				+ " Sum of Squared Error : " + sumOfSquaredError);
		logger.info("Dunn Index for all content clusters : " + model.getDunnIndexForContentClusters());
		
		logger.info("Total recipient clusters size : " + recipientClusters.size());
		int totalPeopleClusterEmails = 0;
		double sumOfSquaredErrorRecipients = 0;
		for(EmailRecipientCluster recipientCluster : recipientClusters){
			
			int flaggedNo = recipientCluster.getNoOfMessagesFlagged();
			int answererdNo = recipientCluster.getNoOfMessagesAnswered();
			int seenNo = recipientCluster.getNoOfMessagesSeen();
			int deletedNo = recipientCluster.getNoOfMessagesDeleted();
			double repScore = recipientCluster.getReputationScore();
			int totalEmails = recipientCluster.getRecipientEmails().size();
			logger.info("Recipient cluster id : " + recipientCluster.getId() 
					+ " total emails : " + totalEmails
					+ " no of msgs flagged : " + flaggedNo 
					+ " no of msgs answered : " + answererdNo
					+ " no of msgs seen : " + seenNo
					+ " no of msgs deleted : " + deletedNo
					+ " rep.score " + repScore);
			logger.info("Added emails....");
			for(Email email : recipientCluster.getRecipientEmails()){
				String fromAddr = email.getFromAddress();
				List<String> toAddr = email.getToAddresses();
				String toAddressStr = "";
				if(toAddr != null){
					for(String adrs : toAddr){
						toAddressStr += " " + adrs;
					}
				}
				
				List<String> ccAddr = email.getCcAddresses();
				String ccAddressStr = "";
				if(ccAddr != null){
					for(String adrs : ccAddr){
						ccAddressStr += " " + adrs;
					}
				}
				
				logger.info("Email subject : " + email.getSubject()
						+ " from : " +fromAddr + " to : " + toAddressStr
						+ " cc : " + ccAddressStr);
			}
			logger.info("\n\n");
			totalPeopleClusterEmails += totalEmails;
			sumOfSquaredErrorRecipients += recipientCluster.getSumOfSquaresError();
		}
		logger.info("Total emails in all recipient clusters : " + totalEmailCount + " in no. of clusters :  " + recipientClusters.size() 
				+ " Sum of Squared Error : " + sumOfSquaredErrorRecipients);
		logger.info("Dunn Index for all recipient clusters : " + model.getDunnIndexForRecipientClusters());
		
	}
	
	
	public void printClusterResults(){
		List<UserMailBox> mailBoxes = listAllMailBoxes();
		for(UserMailBox mb : mailBoxes){
			logger.info("Cluster results for mailbox : " + mb.getEmailId() + " with emails : " + mb.getAllEmails().size());
			EmailReputationDataModel model = mb.getReputationDataModel();
			List<EmailContentCluster> contentClusters = model.getContentClusters();
			List<EmailRecipientCluster> recipientClusters = model.getRecipientClusters();
			
			logger.info("Printing content cluster results...");
			int totalContentClusterEmails = 0;
			double sumOfSquaredError = 0;
			for(EmailContentCluster contentCluster : contentClusters){
				int flaggedNo = contentCluster.getNoOfMessagesFlagged();
				int answererdNo = contentCluster.getNoOfMessagesAnswered();
				int seenNo = contentCluster.getNoOfMessagesSeen();
				int deletedNo = contentCluster.getNoOfMessagesDeleted();
				double repScore = contentCluster.getReputationScore();
				int totalEmails = contentCluster.getContentEmails().size();
				logger.info("content cluster id : " + contentCluster.getId() 
						+ " total emails in cluster : " + totalEmails 
						+ " no of msgs flagged : " + flaggedNo 
						+ " no of msgs answered : " + answererdNo
						+ " no of msgs seen : " + seenNo
						+ " no of msgs deleted : " + deletedNo
						+ " rep.score " + repScore);
				logger.info("Added emails....");
				for(Email email : contentCluster.getContentEmails()){
					logger.info("Email subject : " + email.getSubject());
				}
				totalContentClusterEmails += totalEmails;
				sumOfSquaredError += contentCluster.getSumOfSquaresError();
				logger.info("-------------------------------------------------------------------------------------------------\n\n");
			}
			logger.info("Total emails in all content clusters : " + totalContentClusterEmails + " in no. of clusters : " + contentClusters.size() 
					+ " Sum of Squared Error : " + sumOfSquaredError);
			logger.info("Dunn Index for all content clusters : " + model.getDunnIndexForContentClusters());
			logger.info("\n\n===================================================================================================================");
			
			logger.info("Printing recipient cluster results...");
			int totalPeopleEmails = 0;
			double totatSumSquaresPeopl = 0;
			for(EmailRecipientCluster recipientCluster : recipientClusters){
				int size = recipientCluster.size();
				int flaggedNo = recipientCluster.getNoOfMessagesFlagged();
				int answererdNo = recipientCluster.getNoOfMessagesAnswered();
				int seenNo = recipientCluster.getNoOfMessagesSeen();
				int deletedNo = recipientCluster.getNoOfMessagesDeleted();
				double repScore = recipientCluster.getReputationScore();
				int totalEmails = recipientCluster.getRecipientEmails().size();
				logger.info("Recipient cluster id : " + recipientCluster.getId() + " no of msgs flagged : " + flaggedNo 
						+ " total emails in cluster : " + totalEmails 
						+ " no of msgs answered : " + answererdNo
						+ " no of msgs seen : " + seenNo
						+ " no of msgs deleted : " + deletedNo
						+ " rep.score " + repScore);
				logger.info("Added emails....");
				for(Email email : recipientCluster.getRecipientEmails()){
					String fromAddr = email.getFromAddress();
					List<String> toAddr = email.getToAddresses();
					String toAddressStr = "";
					if(toAddr != null){
						for(String adrs : toAddr){
							toAddressStr += " " + adrs;
						}
					}
					
					List<String> ccAddr = email.getCcAddresses();
					String ccAddressStr = "";
					if(ccAddr != null){
						for(String adrs : ccAddr){
							ccAddressStr += " " + adrs;
						}
					}
					
					logger.info("Email subject : " + email.getSubject()
							+ " from : " +fromAddr + " to : " + toAddressStr
							+ " cc : " + ccAddressStr);
				}
				totalPeopleEmails += totalEmails;
				totatSumSquaresPeopl += model.getDunnIndexForRecipientClusters();
				logger.info("-------------------------------------------------------------------------\n\n");
			}
			logger.info("Total emails in all recipient clusters : " + totalPeopleEmails + " in no. of clusters : " + recipientClusters.size() 
					+ " Sum of Squared Error : " + totatSumSquaresPeopl);
			logger.info("Dunn Index for all recipient clusters : " + model.getDunnIndexForRecipientClusters());
		}
	}
	
	@Named("List Mail Boxes")
	public List<UserMailBox> listAllMailBoxes() {
		//if(this.mailBoxes == null){
			this.mailBoxes = container.allInstances(UserMailBox.class);
		//}
		
		//List<UserMailBox> mailboxes = container.allInstances(UserMailBox.class);
		
		for(UserMailBox mb : mailBoxes){
			logger.info("Got Email mailbox from database : " + mb.getEmailId() + " with emails : " + mb.getAllEmails().size());
		}
		
		//return mailboxes;
		return mailBoxes;
	}
	
	/**
	 * @param from
	 * @param to
	 * @param mailBoxID
	 * @return the list of emails from the mailbox within the time period from-to 
	 */
	@Programmatic
	public List<Email> getEmailsForTimePeriod(long from, long to, String mailBoxID) {
		
		QueryDefault<Email> query = 
		            QueryDefault.create(
		                Email.class, 
		                "findEmailsForPeriod", 
		                "fromDate", from, 
		                "toDate", to,
		                "mailboxId", mailBoxID);
		                
		
		return container.allMatches(query);
	}

	@Programmatic
	//sample mailbox
	public UserMailBox createSample() {
		UserMailBox mb = container.newTransientInstance(UserMailBox.class);
		
		mb.setEmailId("gdc2013demo@gmail.com");
		//mb.setAccountId("530f0d8eb4810fd65d6d2149");
		
		Date today = new Date();
		
		long todayTimestamp = EmailUtils.getTodayTimestamp();
		//logger.info("Today's date in long : " + todayTimestamp);
		mb.setMailBoxAddedDateTimeStamp(todayTimestamp);
		container.persist(mb);
		
		return mb;
	}

	@Named("Add MailBox")
	public UserMailBox connectMailBox(@Named("Email Id") String emailId,
			@Optional@Named("IMAP Host Id (e.g : imap.gmail.com)") String imapHostId,
			@Named("Password") Password password,
			@Optional@Named("First Name") String fname, @Optional@Named("Last Name") String lname) {
		/*UserMailBox newMb = contextIOService.connectMailBox(emailId, password,
				fname, lname);*/
		UserMailBox mb = container.newTransientInstance(UserMailBox.class);
		mb.setEmailId(emailId);
		if(imapHostId != null && !imapHostId.isEmpty()){
			mb.setImapHostId(imapHostId);
		} else {
			mb.setImapHostId("imap.gmail.com");
		}
		
		mb.setPassword(password.getPassword());
		mb.setUserFirstName(fname);
		mb.setUserLastName(lname);
		long todayTimestamp = EmailUtils.getTodayTimestamp();
		mb.setMailBoxAddedDateTimeStamp(todayTimestamp);
		container.persist(mb);
		container.flush();
		return mb;
	}

	//creating the data file to weka
	/**
	 *create the results file for all emails
	 *
	 * 
	 * @param mb
	 */
	@Named("Create Results csv File")
	public void createResultsFile(){
		List<UserMailBox> mailBoxes = listAllMailBoxes();
		for(UserMailBox mb : mailBoxes){
			List<Email> allEmails = mb.getAllEmails();
			
			PrintWriter writer = null;		
			try {
				writer = new PrintWriter("EmailResults.csv", "UTF-8");
				String headerRecord = "mailId,sub,ismodel,ispredicted,isSpam,isDirect,isCCd,isList,"
						+ "isAnswered,isFlagged,isSeen,isImportantByHeader,importanceLevel,precedenceLevel,isSensitiveByHeader,"
						+ "isCommit,isDeliver,isMeeting,isRequest,attachments,images,"
						+ "contentScore,recipientScore,flaggedTopicScore,flaggedPeopleScore,flaggedSAScore,flaggedKeywordScore,"
						+ "repliedTopicScore,repliedPeopleScore,repliedSAScore,repliedKeywordScore,"
						+ "seenTopicScore,seenPeopleScore,seenSAScore,seenKeywordScore,"
						+ "contentClusterId,peopleClusterId";
				writer.println(headerRecord);
				
				for(Email mail : allEmails){
					String sub = mail.getSubject();
					//sub.replace("'", " ");
					boolean model = mail.isModel();
					boolean predicted = mail.isPredicted();
					
					boolean isSpam = mail.isSpam();
					boolean isDirect = mail.isDirect();
					boolean isCCd = mail.isCCd();
					boolean isList = mail.isListMail();
					
					boolean isAnswered = mail.isAnswered();
					boolean isFlagged = mail.isFlagged();
					boolean isSeen = mail.isSeen();
					
					boolean isImportantByHeader = mail.getIsImportantByHeader();
					int importanceLevel = mail.getImportanceLevelByHeader();
					String precedenceLevel = mail.getPrecedenceLevelByHeader();
					boolean isSensitiveByHeader = mail.isSensitiveByHeader();
					
					boolean isCommit = mail.isCommit();
					boolean isDelivery = mail.isDelivery();
					boolean isMeeting = mail.isMeeting();
					boolean isRequest = mail.isRequest();
					
					int attachments = mail.getNoOfAttachments();
					int images = mail.getNoOfImages();
					
					//from clustering /clasificaion results
					double contentScore = mail.getContentReputationScore();
					double recipientScore = mail.getRecipientReputationScore();
					
					double flaggedTopicScore = mail.getFlaggedTopicscore();			
					double flaggedPeopleScore = mail.getFlaggedPeoplescore();
					double flaggedSAScore = mail.getFlaggedSpeechActscore();
					double flaggedKeywordScore = mail.getFlaggedKeywordscore();
					
					double repliedTopicScore = mail.getRepliedTopicscore();
					double repliedPeopleScore = mail.getRepliedPeoplescore();
					double repliedSAScore = mail.getRepliedSpeechActscore();
					double repliedKeywordScore = mail.getRepliedKeywordscore();
					
					double seenTopicScore = mail.getSeenTopicscore();
					double seenPeopleScore = mail.getSeenPeoplescore();
					double seenSAScore = mail.getSeenSpeechActscore();
					double seenKeywordScore = mail.getSeenKeywordscore();
					
					String contentClusterId = mail.getTextClusterId();
					String peopleClusterId = mail.getPeopleClusterId();
					
					String record = mail.getMessageId() + "," + "'" + sub + "'"+ "," + model + "," + predicted
					+ "," + isSpam + "," + isDirect + "," + isCCd + "," + isList
					+ "," + isAnswered  + "," + isFlagged  + "," + isSeen
					+ "," + isImportantByHeader + "," + importanceLevel + "," + precedenceLevel + "," + isSensitiveByHeader
					+ "," + isCommit + "," + isDelivery + "," + isMeeting + "," + isRequest
					+ "," + attachments + "," + images
					+ "," + contentScore + "," + recipientScore + "," + flaggedTopicScore + "," + flaggedPeopleScore + "," + flaggedSAScore + "," + flaggedKeywordScore
					+ "," + repliedTopicScore + "," + repliedPeopleScore + "," + repliedSAScore + "," + repliedKeywordScore
					+ "," + seenTopicScore + "," + seenPeopleScore + "," + seenSAScore + "," + seenKeywordScore
					+ "," + contentClusterId + "," +peopleClusterId;
					
					
					String simpleRecord = mail.getMessageId()+ "," + "'" + sub + "'"+ "," + model + "," + predicted;
					//record = record.substring(0, (record.length()-1));
					writer.println(simpleRecord);
						
				}			
				writer.close();			
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally{
				writer.close();
			}	
		}
	}
	

	// region > dependencies
	@Inject
	DomainObjectContainer container;
	@Inject
	ContextIOService contextIOService;
	@Inject
	JavaMailService javaMailService;
	@Inject
	EmailAnalysisService emailAnalysisService;
	
	@javax.inject.Inject
	private IsisJdoSupport isisJdoSupport;
	// endregion
	
	List<UserMailBox> mailBoxes;
}
