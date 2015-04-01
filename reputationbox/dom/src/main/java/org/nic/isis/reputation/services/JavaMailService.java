package org.nic.isis.reputation.services;

import jangada.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Flags;
import javax.mail.Flags.Flag;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.NoSuchProviderException;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.UIDFolder;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.search.AndTerm;
import javax.mail.search.ComparisonTerm;
import javax.mail.search.ReceivedDateTerm;
import javax.mail.search.SearchTerm;

import org.apache.commons.lang.ArrayUtils;
import org.apache.isis.applib.DomainObjectContainer;
import org.apache.isis.applib.annotation.Hidden;
import org.apache.isis.applib.annotation.Programmatic;
import org.nic.isis.reputation.dom.ContextVectorMap;
import org.nic.isis.reputation.dom.Email;
import org.nic.isis.reputation.dom.EmailAttachment;
import org.nic.isis.reputation.dom.EmailReputationDataModel;
import org.nic.isis.reputation.dom.IndexVectorMap;
import org.nic.isis.reputation.dom.RandomIndexVector;
import org.nic.isis.reputation.dom.TextContent;
import org.nic.isis.reputation.dom.TextTokenMap;
import org.nic.isis.reputation.dom.UserMailBox;
import org.nic.isis.reputation.utils.EmailUtils;
import org.nic.isis.ri.RandomIndexing;
import org.nic.isis.ri.SemanticSpace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.mail.imap.protocol.FLAGS;

import edu.ucla.sspace.common.Similarity;
import edu.ucla.sspace.vector.TernaryVector;

@Hidden
public class JavaMailService {

	private final static Logger logger = LoggerFactory
			.getLogger(JavaMailService.class);

	@Programmatic
	public UserMailBox createImportanceModel(UserMailBox mailbox){
		logger.info("creating email model for important emails..");
		mailbox.setUpdatingModel(true);
		Properties props = new Properties();
		props.setProperty("mail.store.protocol", "imaps");

		RandomIndexing textSemantics = null;
		RandomIndexing recipientSemantics = null;

		// getting indexVectors and contextVectors of the mailbox, if null
		// intialize them
		List<RandomIndexVector> contentVectors = mailbox.getContentVectors();
		List<RandomIndexVector> recipientVectors = mailbox.getRecipientVectors();
		logger.info("loading contentVectors with size : " + contentVectors.size());
		logger.info("loading recipientVectors with size : " + recipientVectors.size());
		
		Map<String,TernaryVector> contentIndexVectorMap = new HashMap<String, TernaryVector>();
		Map<String, double[]> contentContextVectorMap = new HashMap<String, double[]>();
		Map<String,TernaryVector> recipientIndexVectorMap = new HashMap<String, TernaryVector>();
		Map<String, double[]> recipientContextVectorMap = new HashMap<String, double[]>();
		Map<String, Integer> wordDocFrequencies = new HashMap<String, Integer>();

		
		for(RandomIndexVector contentVector : contentVectors){
			contentIndexVectorMap.put(contentVector.getWord(), contentVector.getIndexVector());
			contentContextVectorMap.put(contentVector.getWord(), contentVector.getContextVector());
			wordDocFrequencies.put(contentVector.getWord(), contentVector.getWordDocFrequency());
		}
		
		for(RandomIndexVector recipientVector : recipientVectors){
			recipientIndexVectorMap.put(recipientVector.getWord(), recipientVector.getIndexVector());
			recipientContextVectorMap.put(recipientVector.getWord(), recipientVector.getContextVector());
		}
		
		

		Message[] messages = null;		
		Session session = null;
		Store store = null;
		try {
			session = Session.getInstance(props, null);
			store = session.getStore();
			store.connect(mailbox.getImapHostId(), mailbox.getEmailId(),
					mailbox.getPassword());
			Folder inbox = store.getFolder("INBOX");
			//Folder inbox = store.getFolder("[Gmail]/Important");
			
			inbox.open(Folder.READ_ONLY);
			UIDFolder uf = (UIDFolder) inbox;
					
			textSemantics = new RandomIndexing(
					contentIndexVectorMap,
					contentContextVectorMap, RandomIndexing.textSemanticType);
			
			//to calculate incremental logIDF for words for weighting
			textSemantics.setWordDocumentFrequencies(wordDocFrequencies);
			textSemantics.setNoOfDocumentsProcessed(mailbox.getAllEmails().size());
			
			recipientSemantics = new RandomIndexing(
					recipientIndexVectorMap,
					recipientContextVectorMap, RandomIndexing.peopleSemanticType);
			
			
			//there is a last-indexed message uid,hence retrieving emails from there
			if(mailbox.getLastIndexedMsgUid() > 1){
				Message lstMsg = inbox.getMessage(inbox.getMessageCount());
				long lastMessageUID = uf.getUID(lstMsg);			
				// check if this is a new inbox,
				// then need to traverse all mails to-date from defined beginning
				logger.info("Continuing to retrieve emails since last indexed message UID : " + mailbox.getLastIndexedMsgUid() );
				messages = uf.getMessagesByUID(mailbox.getLastIndexedMsgUid(),
							 lastMessageUID);
				//since the last indexes message is also added here as the messages[0]..
				//need to remove the first element from the messages and start with messages[1]
				messages = (Message[])ArrayUtils.subarray(messages, 1, messages.length);
				
			}else{
				//first time the emails are retrieved; retrieving the emails from,to period
				
				Calendar day = Calendar.getInstance();
				//day.add(Calendar.MONTH, -1);
				// temp changes for data retriaval
				// day.set(Calendar.MONTH, 0);
				//getting emails for last 2 weeks
				day.add(Calendar.DAY_OF_MONTH, -14);

				Date fromDate = day.getTime();

				//emails till 3 days ago
				Calendar toDay = Calendar.getInstance();
				toDay.add(Calendar.DATE, -2);			
				
				Date toDate = toDay.getTime();
				logger.info("Retrieving emails from date : " + fromDate);
				logger.info("to date : " + toDate);

				SearchTerm newerThan = new ReceivedDateTerm(ComparisonTerm.GT,
						fromDate);
				SearchTerm olderThan = new ReceivedDateTerm(ComparisonTerm.LT,
						toDate);
				SearchTerm st = new AndTerm(
						   newerThan, 
						   olderThan);
				messages = inbox.search(st);
			}

			//setting an empty email model for the mailbox
			logger.info("Emails to retrieve :" + messages.length );
			int messageLimit = 3;
			logger.info("Starting to retrieve emails with limit:" + messageLimit );
			//messageLimit should be messages.length
			
			for (int count = 0; count < messageLimit; count++) {
				try{
					
					Message msg = messages[count];
					// setting the address objects
					Address[] from = msg.getFrom();
					String fromAddress = EmailUtils.getEmailAddressString(from[0].toString());
					long msgUID = uf.getUID(msg);
					
					//process emails which are not from reputationbox1
					if(!fromAddress.equalsIgnoreCase("reputationbox1@gmail.com")){

						//processing email
						Email newEmail = EmailUtils.processEmail(msg, mailbox.getEmailId(), msgUID);
						
						textSemantics = emailAnalysisService.processTextSemantics(
								newEmail, textSemantics);
						recipientSemantics = emailAnalysisService
								.processPeopleSemantics(newEmail, recipientSemantics);

						// adding email to mailbox...
						//don't add the reputation results email to the email lists
						if(!newEmail.getFromAddress().equals("reputationbox1@gmail.com")){
								//whether to use this email to update the model or classify the email and recommend scores
							logger.info("updating the mailbox profile with the email : " + count);
							mailbox.updateMailBoxProfiles(newEmail);
							//container.flush();
						}
																	
					}

				}catch(Exception ex){
					logger.error("Error occurred while processing email", ex);
					
				}
				
			}

			List<RandomIndexVector> cVectors = textSemantics.getRandomIndexingVectors();
			List<RandomIndexVector> rVectors = recipientSemantics.getRandomIndexingVectors();
			logger.info("the size of contentvectors from text semantics : " + cVectors.size());
			int indx = 1;
			for(RandomIndexVector riVec : cVectors){
				logger.info("textSemantics index word [" + indx + "]" + riVec.getWord() );
				indx++;
			}
			mailbox.setContentVectors(cVectors);
			mailbox.setRecipientVectors(rVectors);
			
			//print model results
			//EmailUtils.printModelResults(mailbox);
			
		} catch (Exception e) {
			logger.error("Error occurred while updating mailbox", e);
		}finally{
			
			try {
				store.close();
			} catch (MessagingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		mailbox.setUpdatingModel(false);
		return mailbox;
	}
	
	/**
	 * predicts the scores of he new emails for the mailbox based on the past model
	 * also classifies new email based on content and people clusters
	 * @param mailbox
	 * @return
	 */
	@Programmatic
	public UserMailBox predictImportanceForNewEmails(UserMailBox mailbox){
		Properties props = new Properties();
		props.setProperty("mail.store.protocol", "imaps");

		RandomIndexing textSemantics = null;
		RandomIndexing recipientSemantics = null;

		// getting indexVectors and contextVectors of the mailbox, if null
		// intialize them
		List<RandomIndexVector> contentVectors = mailbox.getContentVectors();
		List<RandomIndexVector> recipientVectors = mailbox.getRecipientVectors();
		
		Map<String,TernaryVector> contentIndexVectorMap = new HashMap<String, TernaryVector>();
		Map<String, double[]> contentContextVectorMap = new HashMap<String, double[]>();
		Map<String,TernaryVector> recipientIndexVectorMap = new HashMap<String, TernaryVector>();
		Map<String, double[]> recipientContextVectorMap = new HashMap<String, double[]>();
		Map<String, Integer> wordDocFrequencies = new HashMap<String, Integer>();

		
		for(RandomIndexVector contentVector : contentVectors){
			contentIndexVectorMap.put(contentVector.getWord(), contentVector.getIndexVector());
			contentContextVectorMap.put(contentVector.getWord(), contentVector.getContextVector());
			wordDocFrequencies.put(contentVector.getWord(), contentVector.getWordDocFrequency());
		}
		
		for(RandomIndexVector recipientVector : recipientVectors){
			recipientIndexVectorMap.put(recipientVector.getWord(), recipientVector.getIndexVector());
			recipientContextVectorMap.put(recipientVector.getWord(), recipientVector.getContextVector());
		}
		
		
		Message[] messages = null;	
		Session session = null;
		Store store = null;
		try {
			textSemantics = new RandomIndexing(
					contentIndexVectorMap,
					contentContextVectorMap, RandomIndexing.textSemanticType);
			//to calculate incremental logIDF for words for weighting
			textSemantics.setWordDocumentFrequencies(wordDocFrequencies);
			textSemantics.setNoOfDocumentsProcessed(mailbox.getAllEmails().size());
			
			recipientSemantics = new RandomIndexing(
					recipientIndexVectorMap,
					recipientContextVectorMap, RandomIndexing.peopleSemanticType);
			
			session = Session.getInstance(props, null);
			store = session.getStore();
			store.connect(mailbox.getImapHostId(), mailbox.getEmailId(),
					mailbox.getPassword());
			Folder inbox = store.getFolder("INBOX");
			//Folder inbox = store.getFolder("[Gmail]/Important");
			
			inbox.open(Folder.READ_ONLY);
			UIDFolder uf = (UIDFolder) inbox;
			Message lstMsg = inbox.getMessage(inbox.getMessageCount());
			long lastMessageUID = uf.getUID(lstMsg);			
			// check if this is a new inbox,
			// then need to traverse all mails to-date from defined beginning

			logger.info("Continuing to retrieve emails since last indexed message UID : " + lastMessageUID );
			messages = uf.getMessagesByUID(mailbox.getLastIndexedMsgUid(),
						 lastMessageUID);
			//since the last indexes message is also added here as the messages[0]..
			//need to remove the first element from the messages and start with messages[1]
			messages = (Message[])ArrayUtils.subarray(messages, 1, messages.length);
			
			List<Email> newEmails = new ArrayList<Email>();			
			int messagesLimit = 40;
				
			for (int count = 0; count < messagesLimit; count++) {
				try{					
					Message msg = messages[count];
					// setting the address objects
					Address[] from = msg.getFrom();
					String fromAddress = EmailUtils.getEmailAddressString(from[0].toString());
					long msgUID = uf.getUID(msg);
					
					//process emails which are not from reputationbox1
					if(!fromAddress.equalsIgnoreCase("reputationbox1@gmail.com")){

						//processing email
						Email newEmail = EmailUtils.processEmail(msg, mailbox.getEmailId(), msgUID);
						
						textSemantics = emailAnalysisService.processTextSemantics(
								newEmail, textSemantics);
						recipientSemantics = emailAnalysisService
								.processPeopleSemantics(newEmail, recipientSemantics);

						logger.info("adding email to mailbox with size ["
								+ mailbox.getAllEmails().size() + " ] subject: "
								+ newEmail.getSubject() + " email sent timestamp : "
								+ newEmail.getSentTimestamp());
						// adding email to mailbox...
						//don't add the reputation results email to the email lists
						if(!newEmail.getFromAddress().equals("reputationbox1@gmail.com")){
							//classify and recommend the reputation of the email based on importance model
							logger.info("Predicting the reputation of the email based on existing importance model");								
							newEmail = mailbox.predictImportanceFormEmail(newEmail);													
							//also classify new email based on the content and people clusters
							newEmail = mailbox.classifyEmailBasedOnContentAndRecipients(newEmail);
							
							newEmails.add(newEmail);
							mailbox.addEmail(newEmail);
							//container.flush();
						}
						
						// sending the email reputation results email per every 20
						// emails
						if (newEmails.size() == 20) {
							logger.info("Sending reputation results email to : " + mailbox.getEmailId());
							EmailUtils.sendReputationResults(mailbox.getEmailId(),
									newEmails);
							newEmails.clear();
							
						} 
						//last message is encountered so send the last set of results..
						else if((messagesLimit - count) == 1 ) {
							EmailUtils.sendReputationResults(mailbox.getEmailId(),
									newEmails);
							logger.info("Sending reputation results email with results : " + newEmails.size());
							newEmails.clear();
							
						}
						
					}

				}catch(Exception ex){
					logger.error("Error occurred while processing email", ex);
					
				}
				
			}

			mailbox.setContentVectors(textSemantics.getRandomIndexingVectors());
			mailbox.setRecipientVectors(recipientSemantics.getRandomIndexingVectors());
			
		} catch (Exception e) {
			logger.error("Error occurred while updating mailbox", e);
		}finally{
			
			try {
				store.close();
			} catch (MessagingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return mailbox;
	}
	
//	@Programmatic
//	@Deprecated
//	public UserMailBox updateMailBox(UserMailBox mailbox) {
//		Properties props = new Properties();
//		props.setProperty("mail.store.protocol", "imaps");
//
//		RandomIndexing textSemantics = null;
//		RandomIndexing recipientSemantics = null;
//
//		// getting indexVectors and contextVectors of the mailbox, if null
//		// intialize them
//		IndexVectorMap contentIndexVectorMap = mailbox
//				.getContentIndexVectorMap();
//		if (contentIndexVectorMap == null) {
//			contentIndexVectorMap = new IndexVectorMap();
//		}
//		ContextVectorMap contentContextVectorMap = mailbox
//				.getContentContextVectorMap();
//		if (contentContextVectorMap == null) {
//			contentContextVectorMap = new ContextVectorMap();
//		}
//		IndexVectorMap recipientIndexVectorMap = mailbox
//				.getRecipientIndexVectorMap();
//		if (recipientIndexVectorMap == null) {
//			recipientIndexVectorMap = new IndexVectorMap();
//		}
//		ContextVectorMap recipientContextVectorMap = mailbox
//				.getRecipientContextVectorMap();
//		if (recipientContextVectorMap == null) {
//			recipientContextVectorMap = new ContextVectorMap();
//		}
//
//		Message[] messages = null;
//		
//		Session session = null;
//		Store store = null;
//		try {
//			session = Session.getInstance(props, null);
//			store = session.getStore();
//			store.connect(mailbox.getImapHostId(), mailbox.getEmailId(),
//					mailbox.getPassword());
//			Folder inbox = store.getFolder("INBOX");
//			//Folder inbox = store.getFolder("[Gmail]/Important");
//			
//			inbox.open(Folder.READ_ONLY);
//			UIDFolder uf = (UIDFolder) inbox;
//			Message lstMsg = inbox.getMessage(inbox.getMessageCount());
//			long lastMessageUID = uf.getUID(lstMsg);
//			
//			textSemantics = new RandomIndexing(
//					contentIndexVectorMap.getIndexVectorMap(),
//					contentContextVectorMap.getContextVectorMap());
//			recipientSemantics = new RandomIndexing(
//					recipientIndexVectorMap.getIndexVectorMap(),
//					recipientContextVectorMap.getContextVectorMap());
//
//			// check if this is a new inbox,
//			// then need to traverse all mails to-date from defined beginning
//			if (mailbox.getLastIndexedMsgUid() <= 1) {
//
//				Date lastMessageSentDate = lstMsg.getSentDate();
//				
//				Calendar day = Calendar.getInstance();
//				day.setTime(lastMessageSentDate);
//				day.add(Calendar.MONTH, -1);
//				//temp changes for data retriaval
//				//day.set(Calendar.MONTH, 0);
//				//day.set(Calendar.DAY_OF_MONTH, 15);
//				
//				Date fromDate = day.getTime();
//				
//				logger.info("Starting to retrieve emails since : " + day.get(Calendar.DATE) + "/" + day.get(Calendar.MONTH) + "/" + day.get(Calendar.YEAR));
//				
//				SearchTerm newerThan = new ReceivedDateTerm(ComparisonTerm.GT,
//						fromDate);
//				messages = inbox.search(newerThan);
//				mailbox.setLastIndexedMsgUid(lastMessageUID);
//				logger.info("the last indexed msg uid for the mailbox : " + lastMessageUID);
//
//			} else {
//				logger.info("Continuing to retrieve emails since last indexed message UID : " + lastMessageUID );
//				messages = uf.getMessagesByUID(mailbox.getLastIndexedMsgUid(),
//						 lastMessageUID);
//				
//			}
//
//			List<Email> newEmails = new ArrayList<Email>();
//			
//		
//			int messageLimit = 100;
//			if(messages.length < messageLimit ){
//				messageLimit  = messages.length - 1;
//			}
//
//				
//			for (int count = 0; count < messageLimit; count++) {
//				try{
//					
//					Message msg = messages[count];
//					// setting the address objects
//					Address[] from = msg.getFrom();
//					String fromAddress = EmailUtils.getEmailAddressString(from[0].toString());
//					long msgUID = uf.getUID(msg);
//					
//					//process emails which are not from reputationbox1
//					if(!fromAddress.equalsIgnoreCase("reputationbox1@gmail.com")){
//
//						//processing email
//						Email newEmail = EmailUtils.processEmail(msg, mailbox.getEmailId(), msgUID);
//						
//						textSemantics = emailAnalysisService.processTextSemantics(
//								newEmail, textSemantics);
//						recipientSemantics = emailAnalysisService
//								.processPeopleSemantics(newEmail, recipientSemantics);
//
//						logger.info("adding email to mailbox with size ["
//								+ mailbox.getAllEmails().size() + " ] subject: "
//								+ newEmail.getSubject() + " email sent timestamp : "
//								+ newEmail.getSentTimestamp());
//						// adding email to mailbox...
//						//don't add the reputation results email to the email lists
//						if(!newEmail.getFromAddress().equals("reputationbox1@gmail.com")){
//							
//							//whether to use this email to update the model or classify the email and recommend scores
//							newEmails.add(newEmail);
//							mailbox.addEmail(newEmail);
//							//container.flush();
//						}
//						
//						// sending the email reputation results email per every 20
//						// emails
//						if (newEmails.size() == 20) {
//							logger.info("Sending reputation results email");
//							EmailUtils.sendReputationResults(mailbox.getEmailId(),
//									newEmails);
//							newEmails.clear();
//							
//						} 
//						//last message is encountered so send the last set of results..
//						else if((messageLimit  - count) == 1 ) {
//							EmailUtils.sendReputationResults(mailbox.getEmailId(),
//									newEmails);
//							logger.info("Sending reputation results email with results : " + newEmails.size());
//							newEmails.clear();
//							
//						}
//						
//					}
//
//				}catch(Exception ex){
//					logger.error("Error occurred while processing email", ex);
//					
//				}
//				
//			}
//
//			contentIndexVectorMap.setIndexVectorMap(textSemantics
//					.getWordToIndexVector());
//			contentContextVectorMap.setContextVectorMap(textSemantics
//					.getWordToMeaningVector());
//
//			mailbox.setContentIndexVectorMap(contentIndexVectorMap);
//			mailbox.setContentContextVectorMap(contentContextVectorMap);
//
//			recipientIndexVectorMap.setIndexVectorMap(recipientSemantics
//					.getWordToIndexVector());
//			recipientContextVectorMap.setContextVectorMap(recipientSemantics
//					.getWordToMeaningVector());
//
//			mailbox.setRecipientIndexVectorMap(recipientIndexVectorMap);
//			mailbox.setRecipientContextVectorMap(recipientContextVectorMap);
//			
//		} catch (Exception e) {
//			logger.error("Error occurred while updating mailbox", e);
//		}finally{
//			
//			try {
//				store.close();
//			} catch (MessagingException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}
//
//		return mailbox;
//	}

	@javax.inject.Inject
	DomainObjectContainer container;
	@javax.inject.Inject
	EmailAnalysisService emailAnalysisService;
}
