package org.nic.isis.reputation.services;

import jangada.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
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
import org.jfree.util.Log;
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
	public UserMailBox addMailsToModel(UserMailBox mailbox){
		logger.info("creating email model for important emails..");
		mailbox.setUpdatingModel(true);
		
		RandomIndexing textSemantics = null;
		RandomIndexing recipientSemantics = null;

		// getting indexVectors and contextVectors of the mailbox, if null
		// intialize them
		List<RandomIndexVector> contentVectors = mailbox.getContentVectors();
		List<RandomIndexVector> recipientVectors = mailbox.getRecipientVectors();
		logger.info("loading contentVectors with size : " + contentVectors.size());
		logger.info("loading recipientVectors with size : " + recipientVectors.size());
		
		//load the RIvectors from file
		
		Map<String,TernaryVector> contentIndexVectorMap = new HashMap<String, TernaryVector>();
		//Map<String, double[]> contentContextVectorMap = new HashMap<String, double[]>();
		Map<String,TernaryVector> recipientIndexVectorMap = new HashMap<String, TernaryVector>();
		//Map<String, double[]> recipientContextVectorMap = new HashMap<String, double[]>();
		Map<String, Integer> wordDocFrequencies = new HashMap<String, Integer>();

		
		for(RandomIndexVector contentVector : contentVectors){
			TernaryVector tv = new TernaryVector(RandomIndexing.DEFAULT_VECTOR_LENGTH, contentVector.getPositiveIndexes(), contentVector.getNegativeIndexes());
			contentIndexVectorMap.put(contentVector.getWord(), tv);
			//contentContextVectorMap.put(contentVector.getWord(), contentVector.getContextVector());
			wordDocFrequencies.put(contentVector.getWord(), contentVector.getWordDocFrequency());
			
		}
		
		for(RandomIndexVector recipientVector : recipientVectors){
			TernaryVector tv = new TernaryVector(RandomIndexing.DEFAULT_VECTOR_LENGTH, recipientVector.getPositiveIndexes(), recipientVector.getNegativeIndexes());
			recipientIndexVectorMap.put(recipientVector.getWord(), tv);
			//recipientContextVectorMap.put(recipientVector.getWord(), recipientVector.getContextVector());
		}
		
		

		Properties props = new Properties();
		props.setProperty("mail.store.protocol", "imaps");
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
					null, RandomIndexing.textSemanticType);
			
			//to calculate incremental logIDF for words for weighting
			textSemantics.setWordDocumentFrequencies(wordDocFrequencies);
			textSemantics.setNoOfDocumentsProcessed(mailbox.getAllEmails().size());
			
			recipientSemantics = new RandomIndexing(
					recipientIndexVectorMap,
					null, RandomIndexing.peopleSemanticType);
			//to avoid the null pointer
			recipientSemantics.setWordDocumentFrequencies(new HashMap<String, Integer>());
			
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
				//first time the emails are retrieved; retrieving the emails from (-14),to(-2) period since current day
				//messages = getModelEmailSetForPeriod(inbox, -28, -14);
				messages = getModelEmailSetForPeriod(inbox, -28, -7);
			}

			//setting an empty email model for the mailbox
			logger.info("Emails to retrieve :" + messages.length );
			int messageLimit = 50;
			if(messages.length < messageLimit){
				messageLimit = messages.length;
			}
			logger.info("Starting to retrieve emails with limit:" + messageLimit );
			//messageLimit should be messages.length
			
			for (int count = 0; count < messageLimit; count++) {
				try{
					//the email count shouldn't exceed the current model size
					if(mailbox.getAllEmails().size() <= mailbox.getCurrentModelSize()){

						Message msg = messages[count];
						// setting the address objects
						Address[] from = msg.getFrom();
						String fromAddress = EmailUtils.getEmailAddressString(from[0].toString());
						long msgUID = uf.getUID(msg);
						
						//process emails which are not from reputationbox1
						if(!fromAddress.equalsIgnoreCase("reputationbox1@gmail.com")){

							//processing email
							Email newEmail = EmailUtils.processEmail(msg, mailbox.getEmailId(), msgUID);
							
							//setting the email as spam for test purpose
							//newEmail.setSpam(true);
							
							textSemantics = emailAnalysisService.processTextSemantics(
									newEmail, textSemantics);
							//to calculate incremental logIDF for words for weighting
							textSemantics.setWordDocumentFrequencies(wordDocFrequencies);
							textSemantics.setNoOfDocumentsProcessed(mailbox.getAllEmails().size());
							
							recipientSemantics = emailAnalysisService
									.processPeopleSemantics(newEmail, recipientSemantics);
							//to avoid the null pointer
							recipientSemantics.setWordDocumentFrequencies(new HashMap<String, Integer>());
							
							
							//clear out the context words for the words assigned above
							//because they are document dependent.
							textSemantics.removeAllSemantics();
							recipientSemantics.removeAllSemantics();
							
							// adding email to mailbox...
							
							//don't add the reputation results email to the email lists
							//if(!newEmail.getFromAddress().equals("reputationbox1@gmail.com")){
								//logger.info("adding the email to the mailbox with emails count : " + count +" mbox size : " + mailbox.getAllEmails().size());
								//mailbox.updateMailBoxProfiles(newEmail);
								mailbox.addEmail(newEmail);
								
								//container.flush();
							//}
																		
						}
					} else {
						logger.info("mailbox's email count exceeds the current model size. hence returning");
						break;
					}

				}catch(Exception ex){
					logger.error("Error occurred while processing email", ex);
					
				}
				
			}

			List<RandomIndexVector> cVectors = textSemantics.getRandomIndexingVectors();
			List<RandomIndexVector> rVectors = recipientSemantics.getRandomIndexingVectors();
			logger.info("the size of contentvectors from text semantics : " + cVectors.size());
//			int indx = 1;
//			for(RandomIndexVector riVec : cVectors){
//				logger.info("Persisting random index vector for word [" + indx + "]" + riVec.getWord() + " word frequency in all docs: " + riVec.getWordDocFrequency() );
//				indx++;
//			}
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
				logger.error("Error occurred while closing IMAP store", e);
			}
			//need to set this not updating finally	
			mailbox.setUpdatingModel(false);
		}
		
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
		
		RandomIndexing textSemantics = null;
		RandomIndexing recipientSemantics = null;

		// getting indexVectors and contextVectors of the mailbox, if null
		// intialize them
		List<RandomIndexVector> contentVectors = mailbox.getContentVectors();
		List<RandomIndexVector> recipientVectors = mailbox.getRecipientVectors();
		
		Map<String,TernaryVector> contentIndexVectorMap = new HashMap<String, TernaryVector>();
		//Map<String, double[]> contentContextVectorMap = new HashMap<String, double[]>();
		Map<String,TernaryVector> recipientIndexVectorMap = new HashMap<String, TernaryVector>();
		//Map<String, double[]> recipientContextVectorMap = new HashMap<String, double[]>();
		Map<String, Integer> wordDocFrequencies = new HashMap<String, Integer>();

		
		for(RandomIndexVector contentVector : contentVectors){
			//contentIndexVectorMap.put(contentVector.getWord(), contentVector.getIndexVector());
			TernaryVector tv = new TernaryVector(RandomIndexing.DEFAULT_VECTOR_LENGTH, contentVector.getPositiveIndexes(), contentVector.getNegativeIndexes());
			contentIndexVectorMap.put(contentVector.getWord(), tv);
			//contentContextVectorMap.put(contentVector.getWord(), contentVector.getContextVector());
			wordDocFrequencies.put(contentVector.getWord(), contentVector.getWordDocFrequency());
		}
		
		for(RandomIndexVector recipientVector : recipientVectors){
			//recipientIndexVectorMap.put(recipientVector.getWord(), recipientVector.getIndexVector());
			TernaryVector tv = new TernaryVector(RandomIndexing.DEFAULT_VECTOR_LENGTH, recipientVector.getPositiveIndexes(), recipientVector.getNegativeIndexes());
			recipientIndexVectorMap.put(recipientVector.getWord(), tv);
			//recipientContextVectorMap.put(recipientVector.getWord(), recipientVector.getContextVector());
		}
		
		
		Properties props = new Properties();
		props.setProperty("mail.store.protocol", "imaps");
		Message[] messages = null;	
		Session session = null;
		Store store = null;
		try {
			textSemantics = new RandomIndexing(
					contentIndexVectorMap,
					null, RandomIndexing.textSemanticType);
			//to calculate incremental logIDF for words for weighting
			textSemantics.setWordDocumentFrequencies(wordDocFrequencies);
			textSemantics.setNoOfDocumentsProcessed(mailbox.getAllEmails().size());
			
			recipientSemantics = new RandomIndexing(
					recipientIndexVectorMap,
					null, RandomIndexing.peopleSemanticType);
			recipientSemantics.setWordDocumentFrequencies(new HashMap<String, Integer>());
			
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

			logger.info("Continuing to retrieve emails since last indexed message UID : " + mailbox.getLastIndexedMsgUid() + " to : " + lastMessageUID );
			messages = uf.getMessagesByUID(mailbox.getLastIndexedMsgUid(),
						 lastMessageUID);
			//since the last indexes message is also added here as the messages[0]..
			//need to remove the first element from the messages and start with messages[1]
			messages = (Message[])ArrayUtils.subarray(messages, 1, messages.length);
			
			List<Email> newEmails = new ArrayList<Email>();			
			int messagesLimit = 50;
			if(messages.length < messagesLimit){
				messagesLimit = messages.length;
			}
				
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
						//clearing semantic vectors in the RIs for the above processed document
						textSemantics.removeAllSemantics();
						recipientSemantics.removeAllSemantics();
						
						logger.info("adding email to mailbox with size ["
								+ mailbox.getAllEmails().size() + " ] subject: "
								+ newEmail.getSubject() + " email sent timestamp : "
								+ newEmail.getSentTimestamp());
						// adding email to mailbox...
						//don't add the reputation results email to the email lists
						//if(!newEmail.getFromAddress().equals("reputationbox1@gmail.com")){
							//classify and recommend the reputation of the email based on importance model
							logger.info("Predicting the reputation of the email based on existing importance model");								
							//newEmail = mailbox.predictImportanceFromEmail(newEmail);													
							newEmail = mailbox.predictImportanceBasedOnClusterSimilarity(newEmail);
							
							//also classify new email based on the content and people clusters
							//newEmail = mailbox.classifyEmailBasedOnContentAndRecipients(newEmail);
							
							newEmails.add(newEmail);
							mailbox.addEmail(newEmail);
							//container.flush();
						}
						
						// sending the email reputation results email per every 20
						// emails
//						if (newEmails.size() == 50) {
//							logger.info("Sending reputation results email to : " + mailbox.getEmailId());
//							EmailUtils.sendReputationResults(mailbox.getEmailId(),
//									newEmails);
//							newEmails.clear();
//							
//						} 
//						//last message is encountered so send the last set of results..
//						else if((messagesLimit - count) == 1 ) {
//							EmailUtils.sendReputationResults(mailbox.getEmailId(),
//									newEmails);
//							logger.info("Sending reputation results email with results : " + newEmails.size());
//							newEmails.clear();
//							
//						}
						
				//	}

				}catch(Exception ex){
					logger.error("Error occurred while processing email", ex);
					
				}
				
			}
			
			//print the results here coz below takes persistence time
			//printResultsToFile(mailbox);
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
	
	
	/**
	 * newly added method to avoid persistence delay....
	 * @param mb
	 */
	private void printResultsToFile(UserMailBox mb){

		List<Email> allEmails = mb.getAllEmails();
		logger.info("Starting to print a results Email.....the number of emails for the csv : " + allEmails.size());
		String headerRecord = "msgUID,is_model,is_predicted,isDirect,isCCd,isList,"
				+ "isAnswered,isFlagged,isSeen,isSpam,isImportantByHeader,importanceLevel,isSensitiveByHeader,"
				+ "isDelivery,isMeeting,isRequest,isProposal,"	
				+ "contentClusterScore,contentCID,recipientScore,recipientCID,"
				+ "flagTopicScore,flagKwScore,flagSubjectScore,flagBodyScore,flagPplScore,flagFromScore,flagCCScore,flagToScore,"
				+ "replyTopicScore,replyKwScore,replySubjectScore,replyBodyScore,replyPplScore,replyFromScore,replyCCScore,replyToScore,"
				+ "seenTopicScore,seenKwScore,seenSubjectScore,seenBodyScore,seenPplScore,seenFromScore,seenCCScore,seenToScore,"
				+ "spamTopicScore,spamSubjectScore,spamBodyScore,spamKwScore,spamPplScore,spamFromScore,spamCCScore,spamToScore,"
				+ "to,from,cc,keywords,subject";
		
    	
		PrintWriter writer = null;		
		try {
			writer = new PrintWriter("EmailResults_23_4.csv", "UTF-8");
		
			writer.println(headerRecord);
		
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
				boolean isSpam = mail.isSpam();
			
				boolean isImportantByHeader = mail.getIsImportantByHeader();
				int importanceLevel = mail.getImportanceLevelByHeader();
				boolean isSensitiveByHeader = mail.isSensitiveByHeader();
				
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
				double flaggedKeywordScore = mail.getFlaggedKeywordscore();
				double flaggedSubjectScore = mail.getFlaggedTopicSubjectscore();
				double flaggedBodyScore = mail.getFlaggedTopicBodyscore();
				double flaggedPeopleScore = mail.getFlaggedPeoplescore();
				double flaggedFromScore = mail.getFlaggedPeopleFromscore();
				double flaggedCCScore = mail.getFlaggedPeopleCCscore();
				double flaggedToScore = mail.getFlaggedPeopleToscore();
				  
				double repliedTopicScore = mail.getRepliedTopicscore();
				double repliedKeywordScore = mail.getRepliedKeywordscore();
				double repliedTopicSubjectScore = mail.getRepliedTopicSubjectscore();
				double repliedTopicBodyScore = mail.getRepliedTopicBodyscore();
				double repliedPeopleScore = mail.getRepliedPeoplescore();
				double repliedFromScore = mail.getRepliedPeopleFromscore();
				double repliedCCScore = mail.getRepliedPeopleCCscore();
				double repliedToScore = mail.getRepliedPeopleToscore();
				 
				double seenTopicScore = mail.getSeenTopicscore();
				double seenKeywordScore = mail.getSeenKeywordscore();
				double seenTopicSubjectScore = mail.getSeenTopicSubjectscore();
				double seenTopicBodyScore = mail.getSeenTopicBodyscore();
				double seenPeopleScore = mail.getSeenPeoplescore();
				double seenFromScore = mail.getSeenPeopleFromscore();
				double seenCCScore = mail.getSeenPeopleCCscore();
				double seenToScore = mail.getSeenPeopleToscore();
				 
				double spamTopicScore = mail.getSpamTopicScore();
				double spamSubjectScore = mail.getSpamTopicSubjectScore();
				double spamBodyScore = mail.getSpamTopicBodyScore();  
				double spamKeywordScore = mail.getSpamKeywordscore();
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
		    	String keywords = "";
		    		if(mail.getKeywords() != null){
		    			for(String kw : mail.getKeywords()){
			    			keywords += kw + " | ";
			    		}	
		    		}
		    	String sub = mail.getSubject();
					
				//sub.replace("'", " ");
				
		    	
				String record = uid + "," + model + "," + predicted + "," + isDirect + "," + isCCd + "," + isList
				+ "," + isAnswered  + "," + isFlagged  + "," + isSeen + "," + isSpam
				+ "," + isImportantByHeader + "," + importanceLevel + "," + isSensitiveByHeader
			    + "," + isDelivery + "," + isMeeting + "," + isRequest + "," + isProposal
				+ "," + contentScore + "," + contentClusterId + "," + recipientScore + "," +peopleClusterId 
				+ "," + flaggedTopicScore + "," + flaggedKeywordScore + "," + flaggedSubjectScore + "," +flaggedBodyScore
				+ "," + flaggedPeopleScore + "," + flaggedFromScore + "," + flaggedCCScore + "," + flaggedToScore  
				+ "," + repliedTopicScore + "," + repliedKeywordScore + "," + repliedTopicSubjectScore + "," +repliedTopicBodyScore+ "," + repliedPeopleScore 
				+ "," + repliedFromScore + "," + repliedCCScore + "," + repliedToScore
				+ "," + seenTopicScore + "," + seenKeywordScore + "," + seenTopicSubjectScore + "," + seenTopicBodyScore+ "," + seenPeopleScore
				+ "," + seenFromScore + "," + seenCCScore + "," + seenToScore
				+ "," + spamTopicScore + "," + spamSubjectScore + "," + spamBodyScore + "," + spamKeywordScore + "," + spamPeopleScore 
				+ "," + spamPeopleFromScore + "," + spamPeopleCCScore + "," + spamPeopleToScore
				+ "," + toAddrs + "," + fromAddrs + "," + ccAddr + "," + keywords + "," +"'" + sub + "'";
				
				
				//String simpleRecord = mail.getMessageId()+ "," + "'" + sub + "'"+ "," + model + "," + predicted;
				//record = record.substring(0, (record.length()-1));
				writer.println(record);
				count++;
//				if(count > 5){
//					break;
//				}
					
			}			
			writer.close();	
			logger.info("Results file has been created successfully!!");
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

	/**
	 * getting the set of model messages for the training period
	 * @param inbox
	 * @param from
	 * @param to
	 * @return
	 * @throws MessagingException
	 */
	@Programmatic
	public Message[] getModelEmailSetForPeriod(Folder inbox, int from, int to) throws MessagingException{
		if(!inbox.isOpen()){
			inbox.open(Folder.READ_ONLY);
		}

		Calendar day = Calendar.getInstance();
		//day.add(Calendar.MONTH, -1);
		// temp changes for data retriaval
		// day.set(Calendar.MONTH, 0);
		//getting emails for last 2 weeks
		day.add(Calendar.DAY_OF_MONTH, from);

		Date fromDate = day.getTime();

		//emails till 3 days ago
		Calendar toDay = Calendar.getInstance();
		toDay.add(Calendar.DATE, to);			
		
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
		return inbox.search(st);
	}
	@javax.inject.Inject
	DomainObjectContainer container;
	@javax.inject.Inject
	EmailAnalysisService emailAnalysisService;
}
