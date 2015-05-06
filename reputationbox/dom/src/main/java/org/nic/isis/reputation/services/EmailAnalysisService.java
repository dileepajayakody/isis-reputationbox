package org.nic.isis.reputation.services;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.collections4.map.HashedMap;
import org.apache.isis.applib.DomainObjectContainer;
import org.apache.isis.applib.annotation.Hidden;
import org.apache.isis.applib.annotation.Programmatic;
import org.nic.isis.clustering.EmailCluster;
import org.nic.isis.clustering.EmailContentCluster;
import org.nic.isis.clustering.EmailRecipientCluster;
import org.nic.isis.clustering.EmailWeightedSubjectBodyContentCluster;
import org.nic.isis.clustering.KMeansClustering;
import org.nic.isis.reputation.dom.Email;
import org.nic.isis.reputation.dom.EmailReputationDataModel;
import org.nic.isis.reputation.dom.UserMailBox;
import org.nic.isis.reputation.utils.EmailUtils;
import org.nic.isis.ri.RandomIndexing;
import org.nic.isis.ri.SemanticSpace;
import org.nic.isis.vector.VectorsMath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucla.sspace.clustering.OnlineClustering;
import edu.ucla.sspace.clustering.OnlineKMeans;
import edu.ucla.sspace.vector.DoubleVector;
import edu.ucla.sspace.vector.TernaryVector;

/**
 * @author dileepa
 * Performs main analysis services using Random Indexing algorithm
 * 
 */
@Hidden
public class EmailAnalysisService {

	private final static Logger logger = LoggerFactory
			.getLogger(EmailAnalysisService.class);

	@Programmatic
	public List<UserMailBox> listAllMailBoxes() {
		return container.allInstances(UserMailBox.class);
	}

	/**
	 * processes the semantic vectors for the content of the email and NLP keywords 
	 * using random indexing
	 * 
	 * @param email
	 * @param textSemantics
	 * @throws IOException
	 */
	@Programmatic
	public RandomIndexing processTextSemantics(Email email,
			RandomIndexing textSemantics) throws IOException {
		
		double[] emailTextContextVector = new double[textSemantics.getVectorLength()];
		double[] emailSubjectTextContextVector = new double[textSemantics.getVectorLength()];
		double[] emailBodyTextContextVector = new double[textSemantics.getVectorLength()];
		
		//to calculate incremental log idf
		System.out.println("no of docs processed by text semantics" + textSemantics.getNoOfDocumentsProcessed());
		
		// processing subject and body separately as 2 documents
		if(email.getSubjectContent() != null){
			String processedSubjectTokenStream = email.getSubjectContent()
					.getTokenStream();
			//logger.info(" analysing content subject tokens in emailAnalysisService: " + processedSubjectTokenStream);
			textSemantics.processDocument(new BufferedReader(new StringReader(
					processedSubjectTokenStream)));
			
			// calculating the sum of all words semantic vectors in the document
			//processing email subject content
			Set<String> subjectWords = email.getSubjectContent().getStringTokens().keySet();
					
			int totalSubjectWordCount = 0;
			for(String word: subjectWords){
				double[] wordSemanticVector = textSemantics.getVector(word);
			
				if (null != email.getSubjectContent().getStringTokens().get(word)) {
					//logger.info("adding c.vector for subject word : " + word);
					int frequency = email.getSubjectContent().getStringTokens()
							.get(word);
				
					totalSubjectWordCount += frequency;
					int totalNumberOfTokens = email.getSubjectContent().getStringTokens().size();
					// add the semantic vector of the word * the no. of times(normalized) its
					// mentioned in the doc (normalized for 100 words per document)
//					emailTextContextVector = VectorsMath.addArrays(emailTextContextVector,
//							wordSemanticVector, EmailUtils.getNormalizedFrequency(frequency, totalNumberOfTokens));
//					
//					emailSubjectTextContextVector = VectorsMath.addArrays(emailSubjectTextContextVector,
//							wordSemanticVector, EmailUtils.getNormalizedFrequency(frequency, totalNumberOfTokens));
//					
					//with incremental log idf
					int noOfDocsWithTheWord = textSemantics.getWordFrequency(word);
					double normalizedFrequency = EmailUtils.getNormalizedFrequency(frequency, totalNumberOfTokens);
					double incrementalLogIDF = EmailUtils.getIncrementalLogIDF(textSemantics.getNoOfDocumentsProcessed(), noOfDocsWithTheWord);
					double weight = incrementalLogIDF * normalizedFrequency;
					if(weight != 0){
						emailTextContextVector = VectorsMath.addArrays(emailTextContextVector,
								wordSemanticVector, weight);
						
						emailSubjectTextContextVector = VectorsMath.addArrays(emailSubjectTextContextVector,
								wordSemanticVector, weight);
						
					}else {
						logger.warn("the weight(inc.log.idf*freq) for word : " + word + " is : " + weight + " so not added to the vectors");
					}
					
					
				} else {
					logger.info("the frequency for word : " + word + " is null " );
				}
			}
			/*logger.info("the number of subject string tokens analysed and added to email text context vector : " 
						+ totalSubjectWordCount);*/
		}

		//processing email body 
		if(email.getBodyContent() != null){
			String processedBodyTokenStream = email.getBodyContent()
					.getTokenStream();
	 		//logger.info(" analysing content body tokens in emailAnalysisService : " + processedBodyTokenStream);
	 		textSemantics.processDocument(new BufferedReader(new StringReader(
					processedBodyTokenStream)));
	 		
		  	Set<String> bodyWords = email.getBodyContent().getStringTokens().keySet();
		  	int totalBodyWordCount = 0;
			for(String word: bodyWords){
				double[] wordSemanticVector = textSemantics.getVector(word);
				//logger.info("processing context for body word : " + word);
				 if (null != email.getBodyContent().getStringTokens().get(word)) {
					 //logger.info("adding c.vector for body word : " + word);
						int frequency = email.getBodyContent().getStringTokens()
								.get(word);
						totalBodyWordCount += frequency;
						
						int totalNumberOfTokens = email.getBodyContent().getStringTokens().size();
						// add the semantic vector of the word * the no. of times its
						// mentioned in the doc
//						emailTextContextVector = VectorsMath.addArrays(emailTextContextVector,
//								wordSemanticVector, EmailUtils.getNormalizedFrequency(frequency, totalNumberOfTokens));
//						emailBodyTextContextVector = VectorsMath.addArrays(emailBodyTextContextVector,
//								wordSemanticVector, EmailUtils.getNormalizedFrequency(frequency, totalNumberOfTokens));
//					
						//with incremental log idf
						int noOfDocsWithTheWord = textSemantics.getWordFrequency(word);
						double normalizedFrequency = EmailUtils.getNormalizedFrequency(frequency, totalNumberOfTokens);
						double incrementalLogIDF = EmailUtils.getIncrementalLogIDF(textSemantics.getNoOfDocumentsProcessed(), noOfDocsWithTheWord);
						double weight = incrementalLogIDF * normalizedFrequency;
						
						if(weight != 0){
							emailTextContextVector = VectorsMath.addArrays(emailTextContextVector,
									wordSemanticVector, weight);
							emailBodyTextContextVector = VectorsMath.addArrays(emailBodyTextContextVector,
									wordSemanticVector, weight);
						}else {
							logger.warn("the weight(inc.log.idf*freq) for word : " + word + " is : " + weight + " so not added to the vectors");
						}		
						
						} else {
						logger.info("the frequency for word : " + word + " is null " );
					}
			}
			/*logger.info("the number of body string tokens analysed and added to email text context vector : " 
			+ totalBodyWordCount);*/
		}
 		
		email.setTextContextVector(emailTextContextVector);
		email.setSubjectContextVector(emailSubjectTextContextVector);
		email.setBodyContextVector(emailBodyTextContextVector);
		
		//processing NLP text contextvector
		if(email.getKeywordMatrix() != null){
			double[] nlpKeywordVector = textSemantics.processWords(email.getKeywordMatrix());
			email.setNlpKeywordsVector(nlpKeywordVector);
		}
		
		return textSemantics;
	}

	/**
	 * processes the semantic vectors for the associated email addresses using
	 * random indexing
	 * 
	 * @param email
	 * @param peopleSemantics
	 * @throws IOException
	 */
	@Programmatic
	public RandomIndexing processPeopleSemantics(Email email,
			RandomIndexing peopleSemantics) throws IOException {
		List<String> toAddresses = email.getToAddresses();
		List<String> ccAddresses = email.getCcAddresses();
		String fromAddress = email.getFromAddress();
		logger.info("from address : " + fromAddress);
		
		String toAddressStr = "";
		if(toAddresses != null){
			for (String toAddress : toAddresses) {
				toAddressStr += " " + toAddress;
			}
			logger.info("to addresses : " + toAddressStr);
		}
		
		String ccAddressStr = "";
		if(ccAddresses != null){
			for (String ccAddress : ccAddresses) {
				ccAddressStr += " " + ccAddress;
			}
			logger.info("cc addresses : " + ccAddressStr);
		}
		
		//processing all addresses as one document
		String allAddressesStr = fromAddress + toAddressStr + ccAddressStr;
		logger.info("processing all addresses as one document : " + allAddressesStr);
		peopleSemantics.processDocument(new BufferedReader(new StringReader(allAddressesStr)));

		double[] emailPeopleContextVector = new double[peopleSemantics
				.getVectorLength()];
		
		double[] emailFromContextVector = new double[peopleSemantics
		                             				.getVectorLength()];
		
		double[] emailToContextVector = new double[peopleSemantics
			                             				.getVectorLength()];
		double[] emailCcContextVector = new double[peopleSemantics
			                             				.getVectorLength()];
			
		
		//adding toAddresses
		if(toAddresses != null){
			for (String toAddress : toAddresses) {
				// double[] toAddressContextVector =
				// recipientIndex.getContextVector(toAddress);
				TernaryVector toAddressIndexVector = peopleSemantics.
						getWordToIndexVector().get(toAddress);
				if (toAddressIndexVector != null) {
					
					emailPeopleContextVector = RandomIndexing.add(
							emailPeopleContextVector, toAddressIndexVector);
					emailToContextVector = RandomIndexing.add(
							emailToContextVector, toAddressIndexVector);
				}
			}
		}
	
		//adding ccAddresses
		if(ccAddresses != null){
			for (String ccAddress : ccAddresses) {
				double[] ccAddressContextVector = peopleSemantics
						.getVector(ccAddress);
				TernaryVector ccAddressIndexVector = peopleSemantics
						.getWordToIndexVector().get(ccAddress);
				if (ccAddressIndexVector != null) {
					emailPeopleContextVector = RandomIndexing.add(
							emailPeopleContextVector, ccAddressIndexVector);
					emailCcContextVector = RandomIndexing.add(
							emailCcContextVector, ccAddressIndexVector);
				}
			}
		}
		
		//adding from address
		double[] fromAddressContextVector = peopleSemantics
				.getVector(fromAddress);
		TernaryVector fromAddressIndexVector = peopleSemantics
				.getWordToIndexVector().get(fromAddress);
		if (fromAddressIndexVector != null) {
			emailPeopleContextVector = RandomIndexing.add(
					emailPeopleContextVector, fromAddressIndexVector);
			emailFromContextVector = RandomIndexing.add(
					emailFromContextVector, fromAddressIndexVector);
		}
		
		String vecStr = "";
		for(int x = 0; x < emailPeopleContextVector.length; x++){
			vecStr += emailPeopleContextVector[x] + " " ;
		}
		//logger.info("The email people context vector : " + vecStr);
		email.setRecipientContextVector(emailPeopleContextVector);
		
		//setting separate context vectors
		email.setToContextVector(emailToContextVector);
		email.setFromContextVector(emailFromContextVector);
		email.setCcContextVector(emailCcContextVector);
		
		return peopleSemantics;
	}

	
	@Programmatic
	public List<EmailContentCluster> kMeansClusterText(List<Email> emails) {
		KMeansClustering kmeans = new KMeansClustering();
		List<EmailContentCluster> clusters = kmeans.clusterBasedOnContent(emails);

		int totalEMailsInClusters = 0;
		double sumOfSquaredError = 0;
		for (EmailContentCluster cluster : clusters) {
			double contentScore = cluster.calculateClusterReputationScore();
			logger.info(" ");
			logger.info("Cluster ID : " + cluster.getId() + " No.of emails : " + cluster.getContentEmails().size()
					+ " No.of emails starred : " + cluster.getNoOfMessagesFlagged() 
					+ " No.of emails answerred : " + cluster.getNoOfMessagesAnswered()
					+ " No.of emails seen : " + cluster.getNoOfMessagesSeen()
					+ " No.of emails deleted : " + cluster.getNoOfMessagesDeleted()
					+ " Cluster reputation score : " + contentScore);
			int clusterSize = cluster.getContentEmails().size();
			totalEMailsInClusters += clusterSize;
			for (Email email : cluster.getContentEmails()) {
				//email.setTextClusterId(cluster.getId());
				logger.info(cluster.getId() + " : Email subject : " + email.getSubject() + "\n" + 
						"\n");
				email.setContentReputationScore(contentScore);
				email.setTextClusterId(cluster.getId());
				//setting reputation scores
				//logger.info("Email text stream : " + email.getBodyContent().getTokenStream());
			}
			
			//sumOfSquaredError += cluster.getSumOfSquaresError();
		}
		logger.info("TOTAL EMAILS in all Clusters : " + totalEMailsInClusters + " No. of clusters : " + clusters.size());
		//logger.info("Sum of Squared values for all content clusters : " + sumOfSquaredError);
		
		return clusters;
	}
	
	@Programmatic
	public List<EmailWeightedSubjectBodyContentCluster> kMeansClusterWeightedSubjectBodyContent(List<Email> emails) {
		KMeansClustering kmeans = new KMeansClustering();
		List<EmailWeightedSubjectBodyContentCluster> clusters = kmeans.clusterBasedOnSubjectAndBody(emails);

		int totalEMailsInClusters = 0;
		double subjectSumOfSquaredError = 0;
		double bodySumOfSquaredError = 0;
		
		for (EmailWeightedSubjectBodyContentCluster cluster : clusters) {
			double contentScore = cluster.calculateClusterReputationScore();
			logger.info(" ");
			logger.info("Cluster ID : " + cluster.getId() + " No.of emails : " + cluster.getSubjectBodyContentEmails().size()
					+ " No.of emails starred : " + cluster.getNoOfMessagesFlagged() 
					+ " No.of emails answerred : " + cluster.getNoOfMessagesAnswered()
					+ " No.of emails seen : " + cluster.getNoOfMessagesSeen()
					+ " No.of emails deleted : " + cluster.getNoOfMessagesDeleted()
					+ " Cluster reputation score : " + contentScore);
			int clusterSize = cluster.getSubjectBodyContentEmails().size();
			totalEMailsInClusters += clusterSize;
			for (Email email : cluster.getSubjectBodyContentEmails()) {
				//email.setTextClusterId(cluster.getId());
				logger.info(cluster.getId() + " : Email subject : " + email.getSubject() + "\n" + 
						"\n");
				email.setWeightedSubjectBodyContentScore(contentScore);
				email.setWeightedSubjectBodyClusterId(cluster.getId());
				//setting reputation scores
				//logger.info("Email text stream : " + email.getBodyContent().getTokenStream());
			}
			
			//subjectSumOfSquaredError += cluster.getSumOfSquaresErrorForSubject();
			//bodySumOfSquaredError += cluster.getSumOfSquaresErrorForBody();
		}
		logger.info("TOTAL EMAILS in all Clusters : " + totalEMailsInClusters + " No. of clusters : " + clusters.size());
		//logger.info("Subject sum of Squared values for all clusters : " + subjectSumOfSquaredError);
		//logger.info("Body sum of Squared values for all clusters : " + bodySumOfSquaredError);
		
		return clusters;
	}
	
	

	@Programmatic
	public List<EmailRecipientCluster> kMeansClusterRecipients(List<Email> emails) {
		KMeansClustering kmeans = new KMeansClustering();
		List<EmailRecipientCluster> clusters = kmeans.clusterBasedOnRecipients(emails);
		
		int totalEMailsInClusters = 0;
		double sumOfSquaredError = 0;
		
		for (EmailRecipientCluster cluster : clusters) {
			logger.info(" ");
			double recipientScore = cluster.calculateClusterReputationScore();
			logger.info("Cluster ID : " + cluster.getId() + " No.of emails : " + cluster.getRecipientEmails().size()
					+ " No.of emails starred : " + cluster.getNoOfMessagesFlagged() 
					+ " No.of emails answerred : " + cluster.getNoOfMessagesAnswered()
					+ " No.of emails seen : " + cluster.getNoOfMessagesSeen()
					+ " No.of emails deleted : " + cluster.getNoOfMessagesDeleted()
					+ " Cluster reputation score : " + recipientScore);
			
			int clusterSize = cluster.getRecipientEmails().size();
			totalEMailsInClusters += clusterSize;
			
			for (Email email : cluster.getRecipientEmails()) {
				email.setPeopleClusterId(cluster.getId());
				email.setRecipientReputationScore(recipientScore);
				
				List<String> toAddresses = email.getToAddresses();
				List<String> ccAddresses = email.getCcAddresses();
				String fromAddress = email.getFromAddress();
				String toAddrStr = "";
				String ccAddrStr = "";
				if(toAddresses != null){
					for(String addr : toAddresses){
						toAddrStr += addr + ", ";
					}
				}
				if(ccAddresses != null){
					for(String addr : ccAddresses){
						ccAddrStr += addr + ",";
					}
				}

				logger.info(cluster.getId() + " : Email subject : " + email.getSubject()
						+ " \nFrom Address : " + fromAddress
						+ " \nTo Addresses : " + toAddrStr
						+ " \ncc Addresses : " + ccAddrStr);
			}
			
			//sumOfSquaredError += cluster.getSumOfSquaresError();
		}
		
		logger.info("TOTAL EMAILS in all Clusters : " + totalEMailsInClusters + " No. of clusters : " + clusters.size());
		//logger.info("Sum of Squared values for all recipient clusters : " + sumOfSquaredError);
		return clusters;
	}

	
	
	
	@Programmatic
	public void onlineKMeansClusterText(UserMailBox mb){
		OnlineKMeans<DoubleVector> onlineKMeansCluster = new OnlineKMeans<DoubleVector>();
		
		OnlineClustering<DoubleVector> clusteringObj = onlineKMeansCluster.generate();
		
		//clusteringObj.addVector();
		
	}
	
	@Programmatic
	public void createThreadDataFile(UserMailBox mb){
		List<Email> allEmails = mb.getAllEmails();
		PrintWriter writer = null;
		
		try {
			writer = new PrintWriter("RB_RI_SubjectOnly_DataSet500.arff", "UTF-8");
			List<String> dataRecords = new ArrayList<String>(); 
			
			Set<String> threadIds = new HashSet<String>();
			Set<String> topicClusterIds = new HashSet<String>();
			Set<String> peopleClusterIds = new HashSet<String>();
			
			//get nominal values for threadId, topic, people clusters
			for(Email mail : allEmails){
				String sub = mail.getSubject();
				sub = sub.replaceAll("'", " ");
				String msgId = mail.getMessageId();
				msgId = msgId.replaceAll("\u0025", "/"); 
				String record = mail.getMessageId() + ",'" + sub + "'," + mail.getGmailThreadId() + "," + mail.getTextClusterId() + "," + mail.getPeopleClusterId();
				threadIds.add(mail.getGmailThreadId());
				topicClusterIds.add(mail.getTextClusterId());
				peopleClusterIds.add(mail.getPeopleClusterId());
				
				//record = record.substring(0, (record.length()-1));
				dataRecords.add(record);
				//double[] peopleVector = mail.getRecipientContextVector();
					
			}
			
			String threadIDString = "{";
			for(String threadId : threadIds){
				threadIDString += threadId + ",";
			}
			
			threadIDString = threadIDString.substring(0, (threadIDString.length()-1));
			threadIDString += "}";
			
			String textClusterIDString = "{";
			for(String clusterId : topicClusterIds){
				textClusterIDString += clusterId + ",";
			}
			
			textClusterIDString = textClusterIDString.substring(0, (textClusterIDString.length()-1));
			textClusterIDString += "}";
			
			String pplClusterIDString = "{";
			for(String clusterId : peopleClusterIds){
				pplClusterIDString += clusterId + ",";
			}
			
			pplClusterIDString = pplClusterIDString.substring(0, (pplClusterIDString.length()-1));
			pplClusterIDString += "}";
			writer.println("@RELATION reputationbox");
			writer.println();
			writer.println("@ATTRIBUTE emailId STRING");
			writer.println("@ATTRIBUTE subject STRING");
			
			writer.println("@ATTRIBUTE threadId " + threadIDString);
			writer.println("@ATTRIBUTE topicClusterId " + textClusterIDString);
			writer.println("@ATTRIBUTE peopleClusterId " + pplClusterIDString);
			
			writer.println();
			writer.println("@DATA");
			
			for(String record : dataRecords){
				writer.println(record);
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
	
	public void creatingTopicFileFromEmails(){
		
		List<UserMailBox> mbs = listAllMailBoxes();
		logger.info("creating EmailTopics arff file..");
		for(UserMailBox mb: mbs){
			createTopicWekaDataFile(mb);
		}
		logger.info("completed creating EmailTopics file");
		
	}
	
	
	
	//creating the data file to weka
	@Programmatic
	public void createTopicWekaDataFile(UserMailBox mb){
		List<Email> allEmails = mb.getAllEmails();
		int length = 4000;
		PrintWriter writer = null;
		
		try {
			writer = new PrintWriter("EmailTopics.arff", "UTF-8");
			List<String> dataRecords = new ArrayList<String>(); 
			
			for(Email mail : allEmails){
				double[] textVector = mail.getTextContextVector();
				String sub = mail.getSubject();
				sub.replace("'", " ");
				String record = mail.getMessageId() + "," + "'" + sub + "'";
				for(int i = 0; i < length; i++){
					record += "," + textVector[i];
					
				}
				//record = record.substring(0, (record.length()-1));
				dataRecords.add(record);
				//double[] peopleVector = mail.getRecipientContextVector();
					
			}
			writer.println("@RELATION reputationbox");
			writer.println();
			writer.println("@ATTRIBUTE emailId STRING");
			writer.println("@ATTRIBUTE subject STRING");
			for(int x = 1; x <= length; x++){
				writer.println("@ATTRIBUTE ri"+ x + " NUMERIC" );
			}
			writer.println();
			writer.println("@DATA");
			
			for(String record : dataRecords){
				writer.println(record);
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
	
	
	// region > dependencies
	@Inject
	DomainObjectContainer container;
}
