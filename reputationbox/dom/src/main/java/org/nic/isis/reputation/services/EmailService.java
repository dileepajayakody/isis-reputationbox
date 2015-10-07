package org.nic.isis.reputation.services;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.channels.ReadPendingException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;

import org.json.JSONArray;
import org.json.JSONObject;
import org.nic.isis.clustering.EmailContentCluster;
import org.nic.isis.clustering.EmailRecipientCluster;
import org.nic.isis.clustering.EmailWeightedSubjectBodyContentCluster;
import org.nic.isis.reputation.dom.Email;
import org.nic.isis.reputation.dom.EmailReputationDataModel;
import org.nic.isis.reputation.dom.RandomIndexVector;
import org.nic.isis.reputation.dom.UserMailBox;
import org.nic.isis.reputation.utils.EmailUtils;
import org.nic.isis.reputation.viewmodels.EmailVectorViewModel;
import org.nic.isis.ri.RandomIndexing;
import org.nic.isis.vector.VectorsMath;
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

import edu.ucla.sspace.common.Similarity;

/**
 * @author dileepa
 *
 */
public class EmailService {

	private final static Logger logger = LoggerFactory
			.getLogger(EmailService.class);
	
	private final static String CONTENT_VECTOR_FILE = "contentvectors.csv";
	private final static String RECIPIENT_VECTOR_FILE = "recipientvectors.csv";
	private final static String MAILBOX_PROPERTIES_FILE = "mailbox.properties";
	private final static int DEFAULT_FROM_DATE = -7;
	private final static int DEFAULT_TO_DATE = -2;		
	
	//current session's emailboxes
	private List<UserMailBox> mailBoxes;
	
	@Named("Update MailBox Model with new emails")
	public synchronized List<UserMailBox> updateNew(){
		//List<UserMailBox> mailBoxes = listAllMailBoxes();
		long startTime = System.currentTimeMillis();
		mailBoxes = listAllMailBoxes();
		
//		if (mailBoxes == null || mailBoxes.isEmpty()) {
//			logger.info("No mail-box added to the application. Please add a new mailbox via the web console");
//		
//		}
		if (mailBoxes == null || mailBoxes.isEmpty()) {
			logger.info("There is no mailboxes added in ReputationBox. creating a new mailbox");
			//mailBoxes = new ArrayList<UserMailBox>();
			//createSample();
			UserMailBox newMb = createMailBoxFromFile();
			if(newMb != null){
				mailBoxes = new ArrayList<UserMailBox>();
				mailBoxes.add(newMb);
			}
		}
		if(mailBoxes != null && !mailBoxes.isEmpty()){
			logger.info("Loading mailboxes...");
			for (UserMailBox mailBox : mailBoxes) {
				if(mailBox.getContentVectors() == null || mailBox.getContentVectors().size() == 0){
					//getting the riVectors from file
					File contentVectorFile = new File(CONTENT_VECTOR_FILE);
					if(!contentVectorFile.exists()){
						logger.info("The content index vector files don't exist for mailbox");
						
					}else{
						List<RandomIndexVector> contentVectors = EmailUtils.getVectorsFromFile(contentVectorFile);
						logger.info("Got content vectors from file with size : " + contentVectors.size());
						mailBox.setContentVectors(contentVectors);
					}	
				}
				if(mailBox.getRecipientVectors() == null || mailBox.getRecipientVectors().size() == 0){
					File recipientVectorFile = new File(RECIPIENT_VECTOR_FILE);				
					if(!recipientVectorFile.exists()){
						logger.info("The recipient index vector files don't exist for mailbox");
						
					}else{
						List<RandomIndexVector> recipientVectors = EmailUtils.getVectorsFromFile(recipientVectorFile);
						logger.info("Got recipient vectos from file with size : " + recipientVectors.size());
						mailBox.setRecipientVectors(recipientVectors);
					}	
				}
				
				//starting the email retrieval from beginning
				//set the modelSize by calculating the no.of emails to retrieve for the time-period
				if(!mailBox.isUpdatingModel() && mailBox.isRequireNewModel() && mailBox.getCurrentModelSize() == 0){
					Properties props = loadMailBoxProperties();
					if(props == null){
						props = new Properties();		
					}
					props.setProperty("mail.store.protocol", "imaps");
					//loading dates
					String fromStr = props.getProperty("from");
					String toStr = props.getProperty("to");
					int fromDate = DEFAULT_FROM_DATE;
					int toDate = DEFAULT_TO_DATE;
					if(fromStr != null && toStr != null){
						fromDate = Integer.parseInt(fromStr);
						toDate = Integer.parseInt(toStr);
					}
						
					Message[] modelMessages = null;		
					Session session = null;
					Store store = null;
					try {
						session = Session.getInstance(props, null);
						store = session.getStore();
						store.connect(mailBox.getImapHostId(), mailBox.getEmailId(),
								mailBox.getPassword());
						Folder inbox = store.getFolder("INBOX");
						//Folder inbox = store.getFolder("[Gmail]/Important");
						inbox.open(Folder.READ_ONLY);
						Message[] totalMessagesForPeriod = javaMailService.getModelEmailSetForPeriod(inbox, fromDate, 0);
						modelMessages = javaMailService.getModelEmailSetForPeriod(inbox, fromDate, toDate);
						
						logger.info("Total number of messages to retrieve for the past " + (-fromDate)+ "  days : " + totalMessagesForPeriod.length);
						mailBox.setMailToRetrieveForPeriod(totalMessagesForPeriod.length);
						logger.info("Setting model size for the model period : " + modelMessages.length + " model start day : " + fromDate + "; to day: " + toDate) ;
						mailBox.setCurrentModelSize(modelMessages.length);
						logger.info("set mailbox model size : " + mailBox.getCurrentModelSize());
						
					}catch(Exception ex){
						logger.error("Error occurred while connecting to IMAP store to get model size", ex);
					}finally{
						try {
							store.close();
						} catch (MessagingException e) {
							logger.error("Error occurred while closing IMAP store", e);
						}
					}	
				}
				
				//int modelSize = 1200;
				int modelSize = mailBox.getCurrentModelSize();
				
				if(!mailBox.isUpdatingModel() && mailBox.isRequireNewModel() && (mailBox.getAllEmails().size() < modelSize)){
					
					logger.info("Loading mailbox : " + mailBox.getEmailId());	
					logger.info("The training email data set is not yet completely retrieved to mailbox..updating the email training model..");
					mailBox = javaMailService.addMailsToModel(mailBox);
					
					//create the content and people clusters and average vectors for flagged,replied,seen emails
					//at the end of the model email retrieval
					if(mailBox.getAllEmails().size() >= modelSize){
						
						logger.info("Updating importance model indexes ..");
						mailBox = EmailUtils.calculateImportanceModel(mailBox, mailBox.getAllEmails());
							
						//since the model has sufficient model data
						mailBox.setRequireNewModel(false);
						
					}
					
				}else if(!mailBox.isUpdatingModel() && mailBox.isRequireNewModel() && (mailBox.getAllEmails().size() >= modelSize)){
					//still the model is not set hence setting the model
					logger.info("Updating profile vector indexes ..");
					mailBox = EmailUtils.calculateImportanceModel(mailBox, mailBox.getAllEmails());
					
					//since the model has sufficient model data
					mailBox.setRequireNewModel(false);
				} else if(!mailBox.isUpdatingModel() && !mailBox.isRequireNewModel() && (mailBox.getAllEmails().size() >= modelSize)){
					logger.info("Loading mailbox : " + mailBox.getEmailId());			
					logger.info("Updaing mailbox and predicting email importance for emails for mailbox : " + mailBox.getEmailId() + " since email count: " + mailBox.getAllEmails().size()
							+ " last indexed email  UID : " + mailBox.getLastIndexedMsgUid());
					
					//printing the repmodel profile content clusters
//					List<EmailWeightedSubjectBodyContentCluster> repliedContentClusters = mailBox.getReputationDataModel().getRepliedProfileContentClusters();
//					List<EmailRecipientCluster> repliedPeopleClusters = mailBox.getReputationDataModel().getRepliedProfilePeopleClusters();
//					List<EmailWeightedSubjectBodyContentCluster> repliedListContentClusters = mailBox.getReputationDataModel().getRepliedListProfileContentClusters();
//					List<EmailRecipientCluster> repliedListPeopleClusters = mailBox.getReputationDataModel().getRepliedListProfilePeopleClusters();
//					
					List<EmailWeightedSubjectBodyContentCluster> seenContentClusters = mailBox.getReputationDataModel().getSeenProfileContentClusters();
					List<EmailWeightedSubjectBodyContentCluster> seenListContentClusters = mailBox.getReputationDataModel().getSeenListProfileContentClusters();
					List<EmailRecipientCluster> seenPeopleClusters = mailBox.getReputationDataModel().getSeenProfilePeopleClusters();					
					List<EmailRecipientCluster> seenListPeopleClusters = mailBox.getReputationDataModel().getSeenListProfilePeopleClusters();
					double[] spamVector = mailBox.getReputationDataModel().getSpamVector();
					double[] spamPeopleVector = mailBox.getReputationDataModel().getSpamPeopleVector();
					
					
		
					
					//seen profiles
					//printing the repmodel profile content clusters
//					logger.info("Printing the repmodel seen profile content clusters");
					for(EmailWeightedSubjectBodyContentCluster contentCluster : seenContentClusters){
						List<Email> repliedClusterEmails = contentCluster.getSubjectBodyContentEmails();
						
						double[] subjectCentroid = contentCluster.getSubjectCentroid();
						double[] bodyCentroid = contentCluster.getBodyCentroid();
						double subjectTotal = EmailUtils.getVectorTotal(subjectCentroid);
						double bodyTotal = EmailUtils.getVectorTotal(bodyCentroid);
						String clusterId = contentCluster.getId();

					}
//					
//					logger.info("Printing the repmodel replied list profile content clusters");
					for(EmailWeightedSubjectBodyContentCluster contentCluster : seenListContentClusters){
						List<Email> repliedClusterEmails = contentCluster.getSubjectBodyContentEmails();
						double[] subjectCentroid = contentCluster.getSubjectCentroid();
						double[] bodyCentroid = contentCluster.getBodyCentroid();
						double subjectTotal = EmailUtils.getVectorTotal(subjectCentroid);
						double bodyTotal = EmailUtils.getVectorTotal(bodyCentroid);
						String clusterId = contentCluster.getId();						
//						logger.info(clusterId + " subject vector total : " + subjectTotal + " body total : " + bodyTotal);
//						for(Email email : repliedClusterEmails){
//							logger.info(clusterId + " : " + email.getMsgUid() + " subject : " + email.getSubject());
//						}
					}
					
					for(EmailRecipientCluster seenCluster : seenPeopleClusters){
						List<Email> pplMails = seenCluster.getRecipientEmails();
						double centroidTotal = EmailUtils.getVectorTotal(seenCluster.getCentroid());
						logger.info(seenCluster.getId() + " : centroid vec.total : " + centroidTotal);
						String clusterId = seenCluster.getId();
					}
//					logger.info("Printing the repmodel seen list people clusters");
					for(EmailRecipientCluster seenListCluster : seenListPeopleClusters){
						List<Email> pplMails = seenListCluster.getRecipientEmails();
						double centroidTotal = EmailUtils.getVectorTotal(seenListCluster.getCentroid());
						logger.info(seenListCluster.getId() + " : centroid vec.total : " + centroidTotal);
						String clusterId = seenListCluster.getId();
//						for(Email email : pplMails){
//							logger.info(clusterId + " : " + email.getMsgUid() + " from : " + email.getFromAddress());
//						}
					}
					
					//predict importance
					mailBox = javaMailService.predictImportanceForNewEmails(mailBox);			
				}
				
				else{
					logger.info("something wrong with if condition :  isUpdatingModel: " + mailBox.isUpdatingModel() 
							+ " require new model : " + mailBox.isRequireNewModel() + " mailBox.size(): " + mailBox.getAllEmails().size());
					mailBox.setUpdatingModel(false);
					
				}	
				//mailbox needs to be persisted.
				List<RandomIndexVector> contentVectors = mailBox.getContentVectors();
				List<RandomIndexVector> recipientVectors = mailBox.getRecipientVectors();
				//persist the index vectors to file
				logger.info("persisting content vectors for words : " + contentVectors.size());
				EmailUtils.persistIndexVectors(CONTENT_VECTOR_FILE, contentVectors);
				logger.info("persisting recipient vectors for words : " + recipientVectors.size());
				EmailUtils.persistIndexVectors(RECIPIENT_VECTOR_FILE, recipientVectors);

				//container.persist(mailBox);
			}
		}
		//logger.info("creating the results for the emails model and test data");
		//createResultsFile();
		
		container.flush();
		long endTime = System.currentTimeMillis();
		long timeTaken = endTime - startTime;
		logger.info("Time taken to update mailboxes (ms): " + timeTaken);
		return mailBoxes;
	}

	
	
	@Hidden
	public void updateEmailModels(){
		List<UserMailBox> mailBoxes = listAllMailBoxes();
		
		for(UserMailBox mb: mailBoxes){
			String mailboxID = mb.getEmailId();
			
			logger.info("Updating Email Model for mailBox : " + mailboxID + " with emails count : " + mb.getAllEmails().size());
			
//			long dateMonthAgo = EmailUtils.getMonthsBeforeDateTimeStamp(2);
//			long todayTime = EmailUtils.getTodayTimestamp();
//			
//			List<Email> emailsForLastMonth = getEmailsForTimePeriod(dateMonthAgo, todayTime, mailboxID);
//			
			logger.info("Updating profile vector indexes ..");
			mb = EmailUtils.calculateImportanceModel(mb, mb.getAllEmails());
			
			logger.info("Emails clustered according to text cooccurence.");
			List<EmailContentCluster> contentClusters = emailAnalysisService.kMeansClusterText(mb.getAllEmails());	
			logger.info("\n\nEmails clustered according to people cooccurence");
			List<EmailRecipientCluster> recipientClusters = emailAnalysisService.kMeansClusterRecipients(mb.getAllEmails());

			logger.info("Emails clustered according to subject and body weighted vectors.");
			List<EmailWeightedSubjectBodyContentCluster> subjectBodyClusters = emailAnalysisService.kMeansClusterWeightedSubjectBodyContent(mb.getAllEmails());	
			
			//creating a new periodic reputationDataModel for the mailbox
			mb.getReputationDataModel().setContentClusters(contentClusters);
			mb.getReputationDataModel().setRecipientClusters(recipientClusters);
			mb.getReputationDataModel().setWeightedSubjectBodyClusters(subjectBodyClusters);
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
			List<EmailWeightedSubjectBodyContentCluster> weightedSubjectBodyClusters = emailAnalysisService.kMeansClusterWeightedSubjectBodyContent(allEmails);
			model.setContentClusters(contentClusters);
			model.setRecipientClusters(recipientClusters);
			model.setWeightedSubjectBodyClusters(weightedSubjectBodyClusters);
			
			logger.info("Dunn index for content clusters : " + model.calculateDunnIndexForContentClusters());
		}
		container.flush();

	}
	
	
	@Named("Update mailbox importance profiles")
	public void updateMailBoxImportanceProfiles(){
		List<UserMailBox> mailBoxes = listAllMailBoxes();
		for (UserMailBox mb : mailBoxes){
			mb.setNumberOfDirectEmailsFlagged(0);
			mb.setNumberOfDirectEmailsReplied(0);
			mb.setNumberOfDirectEmailsSeen(0);
			
			mb.setNumberOfListEmailsFlagged(0);
			mb.setNumberOfListEmailsReplied(0);
			mb.setNumberOfListEmailsSeen(0);
			
			mb.setNofOfUnimportantEmails(0);
			
			mb = EmailUtils.calculateImportanceModel(mb, mb.getAllEmails());
			logger.info("\n\n");
			//EmailUtils.printImportanceModelForMailBox(mb);
			mb.printImportanceModelForMailBox();
		}
		container.flush();
	}
	
	@Named("Print Email Importance Models")
	public void printImportanceModels(){
		List<UserMailBox> mailBoxes = listAllMailBoxes();
		for (UserMailBox mb : mailBoxes){
			//EmailUtils.printImportanceModelForMailBox(mb);
			mb.printImportanceModelForMailBox();
		}
		
	}
	
//	@Named("Print Cluster Centroids")
//	public void printClusterCentroids(){
//		List<UserMailBox> mailBoxes = listAllMailBoxes();
//		for (UserMailBox mb : mailBoxes){
//			List<EmailContentCluster> emailContentClusters = mb.getReputationDataModel().getContentClusters();
//			logger.info("printing cluster centroids...");
//			for(EmailContentCluster cluster : emailContentClusters){
//				logger.info("cluster : " + cluster.getId() + " centroid : " + EmailUtils.getVectorTotal(cluster.getCentroid()));
//			}
//		}
//	}
	
//	@Named("Check Similarity with the same vector")
//	public void checkSimilarity(){
//		List<UserMailBox> mailBoxes = listAllMailBoxes();
//		for (UserMailBox mb : mailBoxes){
//			List<Email> emails = mb.getAllEmails();
//			logger.info("Checking similarity with own vector");
//			
//			for(Email email : emails){
//				double[] textVector = email.getTextContextVector();
//				double[] peopleVector= email.getRecipientContextVector();
//				
//				logger.info("email id  " + email.getMsgUid() + " text vector cosine sim : " + Similarity.cosineSimilarity(textVector, textVector));
//				logger.info("email id  " + email.getMsgUid() + " people vector cosine sim : " + Similarity.cosineSimilarity(peopleVector, peopleVector));
//				logger.info("\n");
//			}
//		}
//	}
//	
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
		logger.info("Dunn Index for all content clusters : " + model.calculateDunnIndexForContentClusters());
		
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
		logger.info("Dunn Index for all recipient clusters : " + model.calculateDunnIndexForRecipientClusters());
		
	}
	
	public void printRepModelClusters(){
		List<UserMailBox> mailBoxes = listAllMailBoxes();
		for(UserMailBox mb : mailBoxes){
			EmailReputationDataModel repModel = mb.getReputationDataModel();
			List<EmailWeightedSubjectBodyContentCluster> repliedContentClusters = repModel.getRepliedProfileContentClusters();
			List<EmailWeightedSubjectBodyContentCluster> repliedListContentClusters = repModel.getRepliedListProfileContentClusters();
			List<EmailRecipientCluster> repliedPeopleClusters = repModel.getRepliedProfilePeopleClusters();
			List<EmailRecipientCluster> repliedListPeopleClusters = repModel.getRepliedListProfilePeopleClusters();
			
			List<EmailWeightedSubjectBodyContentCluster> seenContentClusters = repModel.getSeenProfileContentClusters();
			List<EmailWeightedSubjectBodyContentCluster> seenListContentClusters = repModel.getSeenListProfileContentClusters();
			List<EmailRecipientCluster> seenPeopleClusters = repModel.getSeenProfilePeopleClusters();
			List<EmailRecipientCluster> seenListPeopleClusters = repModel.getSeenListProfilePeopleClusters();

			logger.info("printing replied content clusters...");
			for(EmailWeightedSubjectBodyContentCluster repliedContentCluster : repliedContentClusters){
				logger.info(repliedContentCluster.getId() + " bodyCentroid : " + EmailUtils.getVectorTotal(repliedContentCluster.getBodyCentroid()));
				for(Email mail : repliedContentCluster.getSubjectBodyContentEmails()){
					logger.info(repliedContentCluster.getId() + " subject : " + mail.getSubject());
				}
			}
			logger.info("\n\n printing replied list content clusters");
			for(EmailWeightedSubjectBodyContentCluster repliedContentCluster : repliedListContentClusters){
				logger.info(repliedContentCluster.getId() + " bodyCentroid : " + EmailUtils.getVectorTotal(repliedContentCluster.getBodyCentroid()));
				for(Email mail : repliedContentCluster.getSubjectBodyContentEmails()){
					logger.info(repliedContentCluster.getId() + " subject : " + mail.getSubject());
				}
			}
			logger.info("printing seen content clusters...");
			for(EmailWeightedSubjectBodyContentCluster seenContentCluster : seenContentClusters){
				logger.info(seenContentCluster.getId() + " bodyCentroid : " + EmailUtils.getVectorTotal(seenContentCluster.getBodyCentroid()));
				for(Email mail : seenContentCluster.getSubjectBodyContentEmails()){
					logger.info(seenContentCluster.getId() + " subject : " + mail.getSubject());
				}
			}
			logger.info("\n\n printing seen list content clusters");
			for(EmailWeightedSubjectBodyContentCluster seenContentCluster : seenListContentClusters){
				logger.info(seenContentCluster.getId() + " bodyCentroid : " + EmailUtils.getVectorTotal(seenContentCluster.getBodyCentroid()));
				for(Email mail : seenContentCluster.getSubjectBodyContentEmails()){
					logger.info(seenContentCluster.getId() + " subject : " + mail.getSubject());
				}
			}
			
			logger.info("\n\n printing replied clusters");
			for(EmailRecipientCluster repliedPplCluster : repliedPeopleClusters){
				logger.info(repliedPplCluster.getId() + " centroid : " + EmailUtils.getVectorTotal(repliedPplCluster.getCentroid()));
				for(Email email : repliedPplCluster.getRecipientEmails()){
					String toAddressStr = "";
					if(email.getToAddresses() != null){
						for(String adrs : email.getToAddresses()){
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
					logger.info(repliedPplCluster.getId() + " from : " + email.getFromAddress() +  " to : " + toAddressStr 
							+ " cc : " + ccAddressStr);
				}
			}
			
			logger.info("\n\n printing seen ppl clusters");
			for(EmailRecipientCluster seenPplCluster : seenPeopleClusters){
				logger.info(seenPplCluster.getId() + " centroid : " + EmailUtils.getVectorTotal(seenPplCluster.getCentroid()));
				for(Email email : seenPplCluster.getRecipientEmails()){
					String toAddressStr = "";
					if(email.getToAddresses() != null){
						for(String adrs : email.getToAddresses()){
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
					logger.info(seenPplCluster.getId() + " from : " + email.getFromAddress() +  " to : " + toAddressStr 
							+ " cc : " + ccAddressStr);
				}
			}
			
			logger.info("\n\n printing list replied clusters");
			for(EmailRecipientCluster repliedPplCluster : repliedListPeopleClusters){
				logger.info(repliedPplCluster.getId() + " centroid : " + EmailUtils.getVectorTotal(repliedPplCluster.getCentroid()));
				for(Email email : repliedPplCluster.getRecipientEmails()){
					String toAddressStr = "";
					if(email.getToAddresses() != null){
						for(String adrs : email.getToAddresses()){
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
					logger.info(repliedPplCluster.getId() + " from : " + email.getFromAddress() +  " to : " + toAddressStr 
							+ " cc : " + ccAddressStr);
				}
			}
			
			logger.info("\n\n printing seen list ppl clusters");
			for(EmailRecipientCluster seenPplCluster : seenListPeopleClusters){
				logger.info(seenPplCluster.getId() + " centroid : " + EmailUtils.getVectorTotal(seenPplCluster.getCentroid()));
				for(Email email : seenPplCluster.getRecipientEmails()){
					String toAddressStr = "";
					if(email.getToAddresses() != null){
						for(String adrs : email.getToAddresses()){
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
					logger.info(seenPplCluster.getId() + " from : " + email.getFromAddress() +  " to : " + toAddressStr 
							+ " cc : " + ccAddressStr);
				}
			}
			
		}

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
			logger.info("Dunn Index for all content clusters : " + model.calculateDunnIndexForContentClusters());
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
				totatSumSquaresPeopl += model.calculateDunnIndexForRecipientClusters();
				logger.info("-------------------------------------------------------------------------\n\n");
			}
			logger.info("Total emails in all recipient clusters : " + totalPeopleEmails + " in no. of clusters : " + recipientClusters.size() 
					+ " Sum of Squared Error : " + totatSumSquaresPeopl);
			logger.info("Dunn Index for all recipient clusters : " + model.calculateDunnIndexForRecipientClusters());
		}
	}
	
	
	/**
	 * setting this mailbox requires a new email model (need to call this monthly)
	 */
	public void setNewEmailModelsForMailBoxes() {
		List<UserMailBox> mailBoxes = listAllMailBoxes();
		for(UserMailBox mb : mailBoxes){
			mb.setRequireNewModel(true);
			mb.setCurrentModelSize(0);
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


	@Programmatic
	//sample mailbox
	public UserMailBox createSample() {
		
		UserMailBox mb = container.newTransientInstance(UserMailBox.class);	
		mb.setEmailId("dileepajayakody@gmail.com");
		mb.setImapHostId("imap.gmail.com");
		//mb.setPassword("test");
		mb.setUserFirstName("Dileepa");
		mb.setUserLastName("Jayakody");
		
		Date today = new Date();
		
		long todayTimestamp = EmailUtils.getTodayTimestamp();
		//logger.info("Today's date in long : " + todayTimestamp);
		mb.setMailBoxAddedDateTimeStamp(todayTimestamp);
		container.persist(mb);
		
		return mb;
	}
	
	@Programmatic
	private Properties loadMailBoxProperties(){
		Properties prop = new Properties();
		File propFile = new File(MAILBOX_PROPERTIES_FILE);
		if(propFile.exists()){
			InputStream input = null;
			try {
				input = new FileInputStream(propFile);
				// load a properties file
				prop.load(input);
			} catch (Exception e) {
				logger.error("Error occurred when loading mailbox properties", e);
				return null;
			}finally {
				if (input != null) {
					try {
						input.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			return prop;		
		} else {
			logger.info("No mailbox.properties file configured in app-root. Please add mailbox.properties file");
			return null;
		}	
	}
	
	@Programmatic
	private UserMailBox createMailBoxFromFile(){
		
		Properties prop = new Properties();
		InputStream input = null;
		UserMailBox mb = null;	
		File propFile = new File(MAILBOX_PROPERTIES_FILE);
		if(propFile.exists()){
			try {	 
				mb = container.newTransientInstance(UserMailBox.class);
				input = new FileInputStream(propFile);
				// load a properties file
				prop.load(input);
				String firstName = prop.getProperty("firstname");
				String lastName = prop.getProperty("lastname");
				String user = prop.getProperty("username");
				String password = prop.getProperty("password");
				String imap = prop.getProperty("imap");
				// get the property value and print it out
				
				mb.setEmailId(user);
				mb.setImapHostId(imap);
				mb.setPassword(password);
				mb.setUserFirstName(firstName);
				mb.setUserLastName(lastName);
				Date today = new Date();
				
				long todayTimestamp = EmailUtils.getTodayTimestamp();
				//logger.info("Today's date in long : " + todayTimestamp);
				mb.setMailBoxAddedDateTimeStamp(todayTimestamp);
				container.persist(mb);
				logger.info("creating a new mailbox based on mailbox.properties : email address : " + user);
				
		 
			} catch (IOException ex) {
				ex.printStackTrace();
				return null;
			} finally {
				if (input != null) {
					try {
						input.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		 
		}else {
			logger.info("No mailbox.properties file configured in app-root. Please add mailbox.properties file or add mailbox from web console");
		}
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
	@Named("Create RepuBox Results File")
	public void createResultsFile(){
	//	if(this.mailBoxes == null){
			this.mailBoxes = listAllMailBoxes();
	//	}
		//List<UserMailBox> mailBoxes = listAllMailBoxes();
		for(UserMailBox mb : mailBoxes){
			List<Email> allEmails = mb.getAllEmails();
			logger.info(mb.getEmailId() + " : The number of emails in the mailbox : " + allEmails.size());
//			String headerRecord = "msgUID,is_model,is_predicted,isDirect,isCCd,isList,"
//					+ "isAnswered,isFlagged,isSeen,isSpam,isImportantByHeader,importanceLevel,isSensitiveByHeader,"
//					+ "isDelivery,isMeeting,isRequest,isProposal,"	
//					+ "contentClusterScore,contentCID,recipientScore,recipientCID,"
//					+ "flagTopicScore,flagKwScore,flagSubjectScore,flagBodyScore,flagPplScore,flagFromScore,flagCCScore,flagToScore,"
//					+ "replyTopicScore,replyKwScore,replySubjectScore,replyBodyScore,replyPplScore,replyFromScore,replyCCScore,replyToScore,"
//					+ "seenTopicScore,seenKwScore,seenSubjectScore,seenBodyScore,seenPplScore,seenFromScore,seenCCScore,seenToScore,"
//					+ "spamTopicScore,spamSubjectScore,spamBodyScore,spamKwScore,spamPplScore,spamFromScore,spamCCScore,spamToScore,"
//					+ "to,from,cc,keywords,subject,reply,read,important,contentClusterScore,recipientClusterScore";
//			
	    	
			String headerRecord = 
					"msgUID,is_model,is_predicted,isDirect,isCCd,isList,"
					+ "isAnswered,isFlagged,isRead"
					+ "isDelivery,isMeeting,isRequest,isProposal,"	
					+ "contentClusterScore,contentCID,recipientScore,recipientCID,"
				//	+ "flagTopicScore,flagKwScore,flagSubjectScore,flagBodyScore,flagPplScore,flagFromScore,flagCCScore,flagToScore,"
					+ "replyTopicScore,replySubjectScore,replyBodyScore,replyPplScore,replyFromScore,replyCCScore,replyToScore,"
					+ "seenTopicScore,seenSubjectScore,seenBodyScore,seenPplScore,seenFromScore,seenCCScore,seenToScore,"
				//	+ "spamTopicScore,spamSubjectScore,spamBodyScore,spamKwScore,spamPplScore,spamFromScore,spamCCScore,spamToScore,"
					+ "to,from,cc,subject,reply,read,important,contentClusterScore,recipientClusterScore,replyTotalScore,readTotalScore";
			
	    	
			
			PrintWriter modelEmailWriter = null;		
			PrintWriter predictedEmailWriter = null;
			try {
				modelEmailWriter = new PrintWriter("Emails_model.csv", "UTF-8");
				predictedEmailWriter = new PrintWriter("Emails_predicted.csv", "UTF-8");
				
				modelEmailWriter.println(headerRecord);
				predictedEmailWriter.println(headerRecord);
				
				int count = 0;
				for(Email mail : allEmails){
					//String messageId = mail.getMessageId();
					long uid = mail.getMsgUid();
					boolean model = mail.isModel();
					boolean predicted = mail.isPredicted();
					boolean isDirect = mail.isDirect();
					boolean isCCd = mail.isCCd();
					boolean isList = mail.isListMail();
					boolean isAnswered = mail.isAnswered();
					boolean isFlagged = mail.isFlagged();
					boolean isSeen = mail.isSeen();
					//boolean isSpam = mail.isSpam();
				
					//boolean isImportantByHeader = mail.getIsImportantByHeader();
					//int importanceLevel = mail.getImportanceLevelByHeader();
					//boolean isSensitiveByHeader = mail.isSensitiveByHeader();
					
					boolean isDelivery = mail.isDelivery();
					boolean isMeeting = mail.isMeeting();
					boolean isRequest = mail.isRequest();
					boolean isProposal = mail.isPropose();
				
					//from clustering /clasificaion results
					double contentScore = mail.getContentReputationScore();
					String contentClusterId = mail.getWeightedSubjectBodyClusterId();
					
					double recipientScore = mail.getRecipientReputationScore();
					String peopleClusterId = mail.getPeopleClusterId();
					
//					double flaggedTopicScore = mail.getFlaggedTopicscore();			
//					double flaggedKeywordScore = mail.getFlaggedKeywordscore();
//					double flaggedSubjectScore = mail.getFlaggedTopicSubjectscore();
//					double flaggedBodyScore = mail.getFlaggedTopicBodyscore();
//					double flaggedPeopleScore = mail.getFlaggedPeoplescore();
//					double flaggedFromScore = mail.getFlaggedPeopleFromscore();
//					double flaggedCCScore = mail.getFlaggedPeopleCCscore();
//					double flaggedToScore = mail.getFlaggedPeopleToscore();
//					  
					double repliedTopicScore = mail.getRepliedTopicscore();
//					double repliedKeywordScore = mail.getRepliedKeywordscore();
					double repliedTopicSubjectScore = mail.getRepliedTopicSubjectscore();
					double repliedTopicBodyScore = mail.getRepliedTopicBodyscore();
					double repliedPeopleScore = mail.getRepliedPeoplescore();
					double repliedFromScore = mail.getRepliedPeopleFromscore();
					double repliedCCScore = mail.getRepliedPeopleCCscore();
					double repliedToScore = mail.getRepliedPeopleToscore();
					 
					double repliedTotalScore = (((repliedTopicSubjectScore + repliedTopicBodyScore)/2)+repliedPeopleScore)/2;
					
					double seenTopicScore = mail.getSeenTopicscore();
//					double seenKeywordScore = mail.getSeenKeywordscore();
					double seenTopicSubjectScore = mail.getSeenTopicSubjectscore();
					double seenTopicBodyScore = mail.getSeenTopicBodyscore();
					double seenPeopleScore = mail.getSeenPeoplescore();
					double seenFromScore = mail.getSeenPeopleFromscore();
					double seenCCScore = mail.getSeenPeopleCCscore();
					double seenToScore = mail.getSeenPeopleToscore();
					
					double readTotalScore = (((seenTopicSubjectScore + seenTopicBodyScore)/2) + seenPeopleScore)/2;
					
//					double spamTopicScore = mail.getSpamTopicScore();
//					double spamSubjectScore = mail.getSpamTopicSubjectScore();
//					double spamBodyScore = mail.getSpamTopicBodyScore();  
//					double spamKeywordScore = mail.getSpamKeywordscore();
//					double spamPeopleScore = mail.getSpamPeopleScore();
//					double spamPeopleFromScore = mail.getSpamPeopleFromScore();
//					double spamPeopleCCScore = mail.getSpamPeopleCCScore();
//					double spamPeopleToScore = mail.getSpamPeopleToScore();
//							
					
					 String toAddrs = "";
			    		if(mail.getToAddresses() != null && mail.getToAddresses().size() > 0){
			    			for(String toAdd : mail.getToAddresses()){
				    			toAddrs += toAdd + " : ";
				    		}	
			    		}
			    		
			    	String fromAddrs = mail.getFromAddress();
			    	
			    		String ccAddr = "";
			    		if(mail.getCcAddresses() != null && mail.getCcAddresses().size() > 0){
			    			for(String ccAdd : mail.getCcAddresses()){
				    			ccAddr += ccAdd + " : ";
				    		}	
			    		}
//			    	String keywords = "";
//			    		if(mail.getKeywords() != null){
//			    			for(String kw : mail.getKeywords()){
//				    			keywords += kw + " | ";
//				    		}	
//			    		}
			    	String sub = mail.getSubject();
					if(sub != null){
						sub = sub.replace(",", " ");	
					}	
					
			    	//final conclusions if the email is predicted for reply,read, important
			    	boolean shouldReply = mail.isNeedReply();
			    	boolean shouldRead = mail.isNeedRead();
			    	boolean shouldFlag = mail.isNeedFlag();
			    	
// 			    	if(repliedTopicScore > 0.4 && repliedPeopleScore > 0.3){
// 			    		shouldReply = true;
//			    	}
// 			    	if(seenTopicScore > 0.3 && seenPeopleScore > 0.4){
// 			    		shouldRead = true;
// 			    	}
// 			    	if(flaggedTopicScore > 0.4 && flaggedPeopleScore > 0.4){
// 			    		shouldFlag = true;
// 			    	}
//			    	
// 			   	    	
					
			    	String record = uid + "," + model + "," + predicted + "," + isDirect + "," + isCCd + "," + isList
					+ "," + isAnswered  + "," + isFlagged  + "," + isSeen 
					+ "," + isDelivery + "," + isMeeting + "," + isRequest + "," + isProposal
					+ "," + contentScore + "," + contentClusterId + "," + recipientScore + "," +peopleClusterId 
					+ "," + repliedTopicScore  + "," + repliedTopicSubjectScore + "," +repliedTopicBodyScore+ "," + repliedPeopleScore 
					+ "," + repliedFromScore + "," + repliedCCScore + "," + repliedToScore
					+ "," + seenTopicScore  + "," + seenTopicSubjectScore + "," + seenTopicBodyScore+ "," + seenPeopleScore
					+ "," + seenFromScore + "," + seenCCScore + "," + seenToScore
					+ "," + toAddrs + "," + fromAddrs + "," + ccAddr + "," +"'" + sub + "'"
					+ "," + contentScore + "," + recipientScore+ "," + repliedTotalScore +  "," + readTotalScore;
					
					
					//String simpleRecord = mail.getMessageId()+ "," + "'" + sub + "'"+ "," + model + "," + predicted;
					//record = record.substring(0, (record.length()-1));
					if(mail.isPredicted()){
						predictedEmailWriter.println(record);
					}else{
						modelEmailWriter.println(record);
					}
					count++;
					
				}			
				modelEmailWriter.close();	
				predictedEmailWriter.close();
				logger.info("Results file has been created successfully!!");
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally{
				modelEmailWriter.close();
			}	
		}
	}
	
	
	
	
	//sample rest object retrieval from backend using a param
	@Hidden
	public String returnParamString(final long param)
	{
		logger.info("GOT a param string : " + param);
		return "got "+param ;
	}
	
	public void markMailImportant(final long mailid){
		//flagged by user
		if(this.mailBoxes == null){
			mailBoxes = listAllMailBoxes();	
		}
		for(UserMailBox mb : mailBoxes){
			logger.info("got user flagged important email from client : " + mailid);
			mb.getMarkedImportantEmailUids().add(mailid);
		}
	}
	

	public void markMailSpam(final long mailid){
		//flagged by user
		if(this.mailBoxes == null){
			mailBoxes = listAllMailBoxes();	
		}
		for(UserMailBox mb : mailBoxes){
			logger.info("got user flagged spam email from client : " + mailid);
			mb.getMarkedSpamEmailUids().add(mailid);
		}
	}

	public String getReputationForMessages(String msgkeysString){
		String[] keys = msgkeysString.split("M");
		Set<Long> msgKeys = new HashSet<Long>();
		for(String key : keys){
			if(key.length() > 0){
				try{
					long msgKey = Long.parseLong(key);		
					logger.info("Parsing email msgkey : " + msgKey);
					msgKeys.add(msgKey);
				}catch(NumberFormatException nex){
					logger.error("Error occured while parsing msg key for reputation request", nex);
				}
			}
		}
//		if(this.mailBoxes == null){
//			logger.info("the mailboxes is empty. loading mailboxes from database..");
//			mailBoxes = listAllMailBoxes();	
//		}
		mailBoxes = listAllMailBoxes();
		
		String reputationResultString = null;
		for(UserMailBox mb : mailBoxes){
			//Map<Long,Email> emailMap = mb.getEmailMap();
			Map<Long,Email> emailMap = new HashMap<Long, Email>();
			
			List<Email> mails = mb.getAllEmails();
			int modelSize = mb.getCurrentModelSize();
			long lastUid = mb.getLastIndexedMsgUid();
			long predictionStartEmailUid = lastUid - modelSize;
			
			for(Email mail : mails){
				emailMap.put(mail.getMsgUid(), mail);
			}
			logger.info("loaded emailMap with size : " + emailMap.size() + " with model size : " + modelSize 
					+ " starting prediction of emails from uid : " + predictionStartEmailUid);
			
			
			
			JSONObject root = new JSONObject();
			JSONArray resultsArray = new JSONArray();
			for(Long msgKey : msgKeys){
				logger.info("Getting the email for the msgKey : " + msgKey);
				//getting message results for emails of this month's prediction model
				if(msgKey >= predictionStartEmailUid){
					Email mail = emailMap.get(msgKey);
					if(mail != null){
						JSONObject reputationObj = EmailUtils.getReputationObjectJSON(mail);
						resultsArray.put(reputationObj);
					}else {
						logger.info("The mail is not yet processed for msgKey : " + msgKey);
					}	
				}else{
					//this requested email id is too old for this model
					JSONObject reputationObj = new JSONObject();
					reputationObj.put("uid", msgKey);
					reputationObj.put("is_predicted", 0);
					resultsArray.put(reputationObj);
				}
				
			}
			root.put("reputation", resultsArray);
			reputationResultString = root.toString();
		}
		return reputationResultString;
	}
	
	@Hidden
	public String getReputationForEmail(long uid){
		JSONObject reputationObj = null;
		
		//if(this.mailBoxes == null){
			mailBoxes = listAllMailBoxes();	
		//}
		for(UserMailBox mb : mailBoxes){
			Map<Long,Email> emailMap = mb.getEmailMap();
			logger.info("loaded email map for mailbox with emails : " + emailMap.size());
			Email email = emailMap.get(uid);
			if(email == null){
				//return null;
				reputationObj = new JSONObject();
				reputationObj.put("id", "NaN");
				reputationObj.put("is_predicted", "0");
			}else{
				if(email.isModel() || !email.isPredicted()){
					reputationObj = new JSONObject();
					reputationObj.put("id", email.getMessageId());
					reputationObj.put("uid", email.getMsgUid());
					reputationObj.put("is_predicted", "0");
				}else if(email.isPredicted()){
					reputationObj = EmailUtils.getReputationObjectJSON(email);
					reputationObj.put("is_predicted", "1");
				}

			}
		}
		return reputationObj.toString();
	}

	
    @Hidden
	public void getEmailSenderReputation(String emailAddress){
		QueryDefault<Email> query = 
	            QueryDefault.create(
	                Email.class, 
	                "findSenderReputation", 
	                "emailAddress", emailAddress);
	                		
		List<Email> matchedEmails = container.allMatches(query);
		logger.info("Got results : " + matchedEmails.size());
		int totalEmails = matchedEmails.size();
		double repliedEmails = 0;
		double seenEmails = 0;
	
		for(Email mail : matchedEmails){
			if(mail.isAnswered()){
				repliedEmails++;
			}
			if(mail.isSeen()){
				seenEmails++;
			}
		}
		double replyReputation = 0.0;
		double readReputation = 0.0;
		
		if(repliedEmails > 0){
			replyReputation = repliedEmails / totalEmails;
		}
		if(seenEmails > 0){
			readReputation = seenEmails / totalEmails;
		}
		
		logger.info(emailAddress + "replyReputation : " + replyReputation + " readReputation :" + readReputation);
	
	}
	
    @Hidden
	@Programmatic
	public List<Email> getEmailsForTimePeriod(long from, long to) {
		
		QueryDefault<Email> query = 
		            QueryDefault.create(
		                Email.class, 
		                "findEmailsBetweenMsgUid", 
		                "fromUid", from, 
		                "toUid", to);
		                		
		List<Email> matchedEmails = container.allMatches(query);
		logger.info("Got emails from db for query from uid : " + from + " to: "+ to+ " results size: "  + matchedEmails.size());
//		for(Email mail : matchedEmails){
//			logger.info("email uid : "+ mail.getMsgUid());
//		}
		return matchedEmails;
	}
	
	//reset properties for email loading..
	public void resetLastPredictedMsgUid(){
		mailBoxes = listAllMailBoxes();
		for(UserMailBox mb : mailBoxes){
			mb.setLastPredictedMsgUidSentToClient(0);
		}
	}

	//the periodic method to call from thunderbird..
	public String receivePredictedEmails() {
		logger.info("predicted emails retriaval method called..");
		JSONObject root = new JSONObject();
		JSONArray resultsArray = new JSONArray();
		
		long lastPredictedMsgUid = 0;
		mailBoxes = listAllMailBoxes();
		for(UserMailBox mb : mailBoxes) {
			lastPredictedMsgUid = mb.getLastPredictedMsgUidSentToClient();
		
		QueryDefault<Email> query = 
		            QueryDefault.create(
		                Email.class, 
		                "findPredictedEmails", 
		                "fromUid", lastPredictedMsgUid);
		                		
		List<Email> matchedEmails = container.allMatches(query);
		logger.info("Got predicted emails from db for query from uid : " + lastPredictedMsgUid + " results size: "  + matchedEmails.size());
			int msgLimit = 50;
			int count = 0;
			if(matchedEmails.size() > 0){
				for(Email mail : matchedEmails){
					lastPredictedMsgUid = mail.getMsgUid();
					JSONObject reputationObj = EmailUtils.getReputationObjectJSON(mail);
					resultsArray.put(reputationObj);
					count++;
					if(count >= msgLimit){
						break;
					}
				}
			}else {
				logger.info("No predicted emails retrieved.. ");
			}
			mb.setLastPredictedMsgUidSentToClient(lastPredictedMsgUid);
		}		
		
		root.put("reputation", resultsArray);
		String reputationResultString = root.toString();
		return reputationResultString;
	}
	
	

	@Named("Recalculate Importance models and Predict Emails")
	public void recalculateModelAndProcessPredictions(){
		mailBoxes = listAllMailBoxes();	
		for(UserMailBox mb : mailBoxes){
			logger.info("mailbox : " + mb.getEmailId() + " loaded with "+ mb.getAllEmails().size() + " model size: " + mb.getCurrentModelSize());
			int mailsToPredict = mb.getAllEmails().size() - mb.getCurrentModelSize();
			//int mailsToPredict = mb.getAllEmails().size() - 2995;
			logger.info("no of emails to predict based on parameters : " + mailsToPredict);
			long modelLimitUid = mb.getLastIndexedMsgUid() - mailsToPredict;
			logger.info("calculated last email uid for model : " + modelLimitUid);
			List<Email> modelEmails = getEmailsForTimePeriod(0, modelLimitUid);
			List<Email> predictEmails = getEmailsForTimePeriod(modelLimitUid, mb.getLastIndexedMsgUid());
			logger.info("mailbox size : " + mb.getAllEmails().size() + " processing model emails : " + modelEmails.size() + " emails to predict : " + predictEmails.size());
			List<Email> resultEmails = new ArrayList<Email>();
			
			mb = EmailUtils.calculateImportanceModel(mb, modelEmails);
			for(Email emailToPredict : predictEmails){
				Email predictedEmail = mb.predictImportanceFromEmailUserProfile(emailToPredict);
				predictedEmail = mb.predictImportanceBasedOnProfileSubClusterSimilarity(emailToPredict);
				predictedEmail = mb.classifyEmailBasedOnContentAndRecipients(emailToPredict);
				resultEmails.add(predictedEmail);
			}
			createResultFileWithProfileAndSubClusterResults(mb.getEmailId(), resultEmails);
		}
	}
	
	
	private static void createResultFileWithProfileAndSubClusterResults(String emailId,List<Email> mails){
		logger.info(emailId + " : The number of emails to write to the result file : " + mails.size());
		String headerRecord = "uid,msgID,is_model,is_predicted,isDirect,isCCd,isList,"
				+ "isAnswered,isFlagged,isSeen,"
				+ "isDelivery,isMeeting,isRequest,isProposal,"	
				+ "contentClusterScore,contentCID,recipientClusterScore,recipientCID,"
				+ "to,from,cc,subject,"
				+ "readProfileContentScore,readProfilePeopleScore,replyProfileContentScore,replyProfilePeopleScore,flagProfileContentScore,flagProfilePeopleScore,"
				+ "totalReadProfileScore,totalReplyProfileScore,totalFlagProfileScore,"
				+ "subClusterReadContentScore,subClusterReadPeopleScore,subClusterReplyContentScore,subClusterReplyPeopleScore,subClusterFlagContentScore,subClusterFlagPeopleScore,"
				+ "readProfileSubClusterScore,replyProfileSubClusterScore,flagProfileSubClusterScore";
		
		PrintWriter modelEmailWriter = null;		
		PrintWriter predictedEmailWriter = null;
		try {
			modelEmailWriter = new PrintWriter("Emails_model.csv", "UTF-8");
			predictedEmailWriter = new PrintWriter("Emails_predicted.csv", "UTF-8");
			
			modelEmailWriter.println(headerRecord);
			predictedEmailWriter.println(headerRecord);
			
			int count = 0;
			for(Email mail : mails){
				//String messageId = mail.getMessageId();
				long uid = mail.getMsgUid();
				boolean model = mail.isModel();
				boolean predicted = mail.isPredicted();
				boolean isDirect = mail.isDirect();
				boolean isCCd = mail.isCCd();
				boolean isList = mail.isListMail();
				boolean isAnswered = mail.isAnswered();
				boolean isFlagged = mail.isFlagged();
				boolean isSeen = mail.isSeen();
				
				boolean isDelivery = mail.isDelivery();
				boolean isMeeting = mail.isMeeting();
				boolean isRequest = mail.isRequest();
				boolean isProposal = mail.isPropose();
			
				//from clustering /clasificaion results
				double contentScore = mail.getContentReputationScore();
				String contentClusterId = mail.getTextClusterId();
				
				double recipientScore = mail.getRecipientReputationScore();
				String peopleClusterId = mail.getPeopleClusterId();
							
				double subClusterReadScore = mail.getTotalSubClusterBasedReadScore();
				double subClusterReplyScore = mail.getTotalSubClusterBasedReplyScore();
				double subClusterFlagScore = mail.getTotalSubClusterBasedFlagScore();
						
				double profileReadScore = mail.getTotalProfileBasedReadScore();
				double profileReplyScore = mail.getTotalProfileBasedReplyScore();
				double profileFlagScore = mail.getTotalProfileBasedFlagScore();
								
				 String toAddrs = "";
		    		if(mail.getToAddresses() != null && mail.getToAddresses().size() > 0){
		    			for(String toAdd : mail.getToAddresses()){
			    			toAddrs += toAdd + " : ";
			    		}	
		    		}
		    		
		    	String fromAddrs = mail.getFromAddress();
		    	
		    		String ccAddr = "";
		    		if(mail.getCcAddresses() != null && mail.getCcAddresses().size() > 0){
		    			for(String ccAdd : mail.getCcAddresses()){
			    			ccAddr += ccAdd + " : ";
			    		}	
		    		}
		    
		    	String sub = mail.getSubject();
					
				sub = sub.replace(",", " ");	

				String record = uid + "," + mail.getMessageId()+ "," + model + "," + predicted 
				+ "," + isDirect + "," + isCCd + "," + isList
				+ "," + isAnswered  + "," + isFlagged  + "," + isSeen 
				+ "," + isDelivery + "," + isMeeting + "," + isRequest + "," + isProposal
				+ "," + contentScore + "," + contentClusterId + "," + recipientScore + "," +peopleClusterId 
				+ "," + toAddrs + "," + fromAddrs + "," + ccAddr + "," +"'" + sub + "'"
				+ "," + mail.getSeenTopicscore() + "," + mail.getSeenPeoplescore() 
				+ "," + mail.getRepliedTopicscore() + "," + mail.getRepliedPeoplescore() 
				+ "," + mail.getFlaggedTopicscore() + "," + mail.getFlaggedPeoplescore() 
				+ "," + profileReadScore + "," + profileReplyScore + "," + profileFlagScore
				+ "," + mail.getProfileClusterReadContentScore() + "," + mail.getProfileClusterReadRecipientScore() 
				+ "," + mail.getProfileClusterReplyContentScore() + "," + mail.getProfileClusterReplyRecipientScore() 
				+ "," + mail.getProfileClusterFlagContentScore() + "," + mail.getProfileClusterFlagRecipientScore()
				+ "," + subClusterReadScore + "," + subClusterReplyScore + "," + subClusterFlagScore;
				
				if(mail.isPredicted()){
					predictedEmailWriter.println(record);
				}else{
					modelEmailWriter.println(record);
				}
				count++;
//				if(count > 5){
//					break;
//				}
					
			}			
			modelEmailWriter.close();	
			predictedEmailWriter.close();
			logger.info("Results file has been created successfully!!");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally{
			modelEmailWriter.close();
		}
		
	}

	@Programmatic
	private static void createResultFileForEmails(String emailId,List<Email> mails){
		logger.info(emailId + " : The number of emails to write to the result file : " + mails.size());
		String headerRecord = "uid,msgID,is_model,is_predicted,isDirect,isCCd,isList,"
				+ "isAnswered,isFlagged,isSeen,isSpam,"
				+ "isDelivery,isMeeting,isRequest,isProposal,"	
				+ "contentClusterScore,contentCID,recipientClusterScore,recipientCID,"
				+ "readProfileClusterContentScore,replyProfileClusterContentScore,"
				+ "readProfileClusterRecipientScore,replyProfileClusterRecipientScore,"
				+ "flagTopicScore,flagSubjectScore,flagBodyScore,flagPplScore,flagFromScore,flagCCScore,flagToScore,"
				+ "replyTextTopicScore,replySubjectScore,replyBodyScore,replyPplScore,replyFromScore,replyCCScore,replyToScore,"
				+ "seenTextTopicScore,seenSubjectScore,seenBodyScore,seenPplScore,seenFromScore,seenCCScore,seenToScore,"
				+ "spamTopicScore,spamSubjectScore,spamBodyScore,spamPplScore,spamFromScore,spamCCScore,spamToScore,"
				+ "to,from,cc,subject,seeProfileScore,replyProfileScore,flagProfileScore,"
				+ "seeProfileSubClusterScore,replyProfileSubClusterScore,contentClusterScore,recipientClusterScore";
		
    	
		PrintWriter modelEmailWriter = null;		
		PrintWriter predictedEmailWriter = null;
		try {
			modelEmailWriter = new PrintWriter("Emails_model.csv", "UTF-8");
			predictedEmailWriter = new PrintWriter("Emails_predicted.csv", "UTF-8");
			
			modelEmailWriter.println(headerRecord);
			predictedEmailWriter.println(headerRecord);
			
			int count = 0;
			for(Email mail : mails){
				//String messageId = mail.getMessageId();
				long uid = mail.getMsgUid();
				boolean model = mail.isModel();
				boolean predicted = mail.isPredicted();
				boolean isDirect = mail.isDirect();
				boolean isCCd = mail.isCCd();
				boolean isList = mail.isListMail();
				boolean isAnswered = mail.isAnswered();
				boolean isFlagged = mail.isFlagged();
				boolean isSeen = mail.isSeen();
				boolean isSpam = mail.isSpam();
			
				boolean isDelivery = mail.isDelivery();
				boolean isMeeting = mail.isMeeting();
				boolean isRequest = mail.isRequest();
				boolean isProposal = mail.isPropose();
			
				//from clustering /clasificaion results
				double contentScore = mail.getContentReputationScore();
				String contentClusterId = mail.getTextClusterId();
				
				double recipientScore = mail.getRecipientReputationScore();
				String peopleClusterId = mail.getPeopleClusterId();
							
				double flaggedTopicScore = mail.getFlaggedTopicscore();			
				double flaggedSubjectScore = mail.getFlaggedTopicSubjectscore();
				double flaggedBodyScore = mail.getFlaggedTopicBodyscore();
				double flaggedPeopleScore = mail.getFlaggedPeoplescore();
				double flaggedFromScore = mail.getFlaggedPeopleFromscore();
				double flaggedCCScore = mail.getFlaggedPeopleCCscore();
				double flaggedToScore = mail.getFlaggedPeopleToscore();
				  
				double repliedTopicScore = mail.getRepliedTopicscore();
				double repliedTopicSubjectScore = mail.getRepliedTopicSubjectscore();
				double repliedTopicBodyScore = mail.getRepliedTopicBodyscore();
				double repliedPeopleScore = mail.getRepliedPeoplescore();
				double repliedFromScore = mail.getRepliedPeopleFromscore();
				double repliedCCScore = mail.getRepliedPeopleCCscore();
				double repliedToScore = mail.getRepliedPeopleToscore();
				 
				double seenTopicScore = mail.getSeenTopicscore();
				double seenTopicSubjectScore = mail.getSeenTopicSubjectscore();
				double seenTopicBodyScore = mail.getSeenTopicBodyscore();
				double seenPeopleScore = mail.getSeenPeoplescore();
				double seenFromScore = mail.getSeenPeopleFromscore();
				double seenCCScore = mail.getSeenPeopleCCscore();
				double seenToScore = mail.getSeenPeopleToscore();
				
				double readProfileClusterContentScore = mail.getProfileClusterReadContentScore();
				double readProfileClusterRecipientScore = mail.getProfileClusterReadRecipientScore(); 
				double replyProfileClusterContentScore = mail.getProfileClusterReplyContentScore();
				double replyProfileClusterRecipientScore = mail.getProfileClusterReplyRecipientScore();
				
				double readProfileClusterTotalScore = (readProfileClusterContentScore + readProfileClusterRecipientScore)/2;
				double replyProfileClusterTotalScore = (replyProfileClusterContentScore + replyProfileClusterRecipientScore)/2;
				
				
				double spamTopicScore = mail.getSpamTopicScore();
				double spamSubjectScore = mail.getSpamTopicSubjectScore();
				double spamBodyScore = mail.getSpamTopicBodyScore();  
				double spamPeopleScore = mail.getSpamPeopleScore();
				double spamPeopleFromScore = mail.getSpamPeopleFromScore();
				double spamPeopleCCScore = mail.getSpamPeopleCCScore();
				double spamPeopleToScore = mail.getSpamPeopleToScore();
						
				
				 String toAddrs = "";
		    		if(mail.getToAddresses() != null && mail.getToAddresses().size() > 0){
		    			for(String toAdd : mail.getToAddresses()){
			    			toAddrs += toAdd + " | ";
			    		}	
		    		}
		    		
		    	String fromAddrs = mail.getFromAddress();
		    	
		    		String ccAddr = "";
		    		if(mail.getCcAddresses() != null && mail.getCcAddresses().size() > 0){
		    			for(String ccAdd : mail.getCcAddresses()){
			    			ccAddr += ccAdd + " | ";
			    		}	
		    		}
		    
		    	String sub = mail.getSubject();
					
				sub = sub.replace(",", " ");	
					
				//totalReadProfileScore
				double totalSeeProfileScore = ((seenTopicSubjectScore + seenTopicBodyScore)/2 + seenPeopleScore)/2;
				double totalReplyProfileScore = ((repliedTopicSubjectScore+ repliedTopicBodyScore)/2 + repliedPeopleScore)/2;
				double totalFlagProfilScore = ((flaggedSubjectScore + flaggedBodyScore)/2 + flaggedPeopleScore)/2;
//				
//				+ "to,from,cc,subject,seeProfileScore,replyProfileScore,flagProfileScore,"
//				+ "seeProfileSubClusterScore,replyProfileSubClusterScore,contentClusterScore,recipientClusterScore";
		
				String record = uid + "," + mail.getMessageId()+ "," + model + "," + predicted + "," + isDirect + "," + isCCd + "," + isList
				+ "," + isAnswered  + "," + isFlagged  + "," + isSeen + "," + isSpam
				+ "," + isDelivery + "," + isMeeting + "," + isRequest + "," + isProposal
				+ "," + contentScore + "," + contentClusterId + "," + recipientScore + "," +peopleClusterId 
				+ "," + readProfileClusterContentScore + "," + replyProfileClusterContentScore + "," + readProfileClusterRecipientScore + "," + replyProfileClusterRecipientScore
				+ "," + flaggedTopicScore +  "," + flaggedSubjectScore + "," +flaggedBodyScore
				+ "," + flaggedPeopleScore + "," + flaggedFromScore + "," + flaggedCCScore + "," + flaggedToScore  
				+ "," + repliedTopicScore + ","  + repliedTopicSubjectScore + "," +repliedTopicBodyScore+ "," + repliedPeopleScore 
				+ "," + repliedFromScore + "," + repliedCCScore + "," + repliedToScore
				+ "," + seenTopicScore + "," + seenTopicSubjectScore + "," + seenTopicBodyScore+ "," + seenPeopleScore
				+ "," + seenFromScore + "," + seenCCScore + "," + seenToScore
				+ "," + spamTopicScore + "," + spamSubjectScore + "," + spamBodyScore + "," + spamPeopleScore 
				+ "," + spamPeopleFromScore + "," + spamPeopleCCScore + "," + spamPeopleToScore
				+ "," + toAddrs + "," + fromAddrs + "," + ccAddr + "," +"'" + sub + "'"
				+ "," + totalSeeProfileScore + "," + totalReplyProfileScore + "," + totalFlagProfilScore
				+ "," + readProfileClusterTotalScore + "," + replyProfileClusterTotalScore + "," + contentScore + "," + recipientScore;
				
				
				//String simpleRecord = mail.getMessageId()+ "," + "'" + sub + "'"+ "," + model + "," + predicted;
				//record = record.substring(0, (record.length()-1));
				if(mail.isPredicted()){
					predictedEmailWriter.println(record);
				}else{
					modelEmailWriter.println(record);
				}
				count++;
//				if(count > 5){
//					break;
//				}
					
			}			
			modelEmailWriter.close();	
			predictedEmailWriter.close();
			logger.info("Results file has been created successfully!!");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally{
			modelEmailWriter.close();
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
}
