import jangada.ReplyToAnnotator;
import jangada.SigFilePredictor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.Transport;
import javax.mail.UIDFolder;
import javax.mail.Flags.Flag;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.search.AndTerm;
import javax.mail.search.ComparisonTerm;
import javax.mail.search.ReceivedDateTerm;
import javax.mail.search.SearchTerm;

import org.json.JSONArray;
import org.json.JSONObject;
import org.nic.isis.clustering.EmailAllFeatureCluster;
import org.nic.isis.clustering.EmailCluster;
import org.nic.isis.clustering.EmailContentCluster;
import org.nic.isis.clustering.EmailWeightedSubjectBodyContentCluster;
import org.nic.isis.clustering.KMeansClustering;
import org.nic.isis.reputation.dom.ContextVectorMap;
import org.nic.isis.reputation.dom.Email;
import org.nic.isis.reputation.dom.EmailBody;
import org.nic.isis.reputation.dom.IndexVectorMap;
import org.nic.isis.reputation.dom.TextTokenMap;
import org.nic.isis.reputation.dom.UserMailBox;
import org.nic.isis.reputation.services.EmailAnalysisService;
import org.nic.isis.reputation.utils.EmailUtils;
import org.nic.isis.ri.RandomIndexing;
import org.nic.isis.ri.SemanticSpace;
import org.nic.isis.vector.VectorsMath;

import edu.stanford.nlp.dcoref.CoNLL2011DocumentReader.NamedEntityAnnotation;
import edu.stanford.nlp.dcoref.CorefChain;
import edu.stanford.nlp.dcoref.CorefCoreAnnotations.CorefChainAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations.TreeAnnotation;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.TypesafeMap.Key;

public class MailTest {


	public static void sendReputationResults(){
		 JSONObject root = new JSONObject();
		 
		 JSONArray resultsArray = new JSONArray();
		 
		 for(int x = 0; x < 2; x++){
			 JSONObject reputationObj = new JSONObject();
			 reputationObj.put("id", "testId");
			 reputationObj.put("contentscore",23);
			 reputationObj.put("peoplescore", 44);
			 reputationObj.put("contentclusterid", "c1");
			 reputationObj.put("peopleclusterid", "p1");
			 resultsArray.put(reputationObj);
		 }
		 root.put("reputation", resultsArray);
		 
		
		 System.out.print(root.toString());
	}
	public static void main(String[] args) {
	/*	Date today =  new Date();
		long todayTime = today.getTime();
		long todayTimeRound = todayTime/1000;
		System.out.println("TodayTime : " + todayTime);
		System.out.println("TodayTime Rounded: " + todayTimeRound);
		
		long mailsFromDateTimeStamp = todayTimeRound - (2 * 2628000);
		System.out.println("the 2months before time : " + mailsFromDateTimeStamp);*/
		MailTest mt = new MailTest();
		//mt.sendMessage();
		
		//mt.sendReputationResults();
		mt.processEnronImportanceResults();
		//mt.javaMailIteratorTest();
		//mt.testStanfordNlp();
		//testVectors();
	}
	
	
	
	
	public void processEnronEmailSet(){
		  File mailDir = new File("/home/dileepa/Desktop/research/DATA/reputationBasedSpamFilter/Enron_Sample_Data/enron_mail_20110402/maildir/allen-p/inbox");
		  //File mailDir = new File("/home/dileepa/Desktop/research/DATA/reputationBasedSpamFilter/Enron_Sample_Data/Dileepa_Samples");
		  
		  
		  File[] mailFiles = mailDir.listFiles();
		  String host = "host.com";
		  java.util.Properties properties = System.getProperties();
		  properties.setProperty("mail.smtp.host", host);
		  Session session = Session.getDefaultInstance(properties);
		  ReplyToAnnotator replyExtractor = new ReplyToAnnotator();				
		  SigFilePredictor signatureDetector = new SigFilePredictor();	
		  EmailAnalysisService emailAnalysisService = new EmailAnalysisService();
			
		  RandomIndexing textSemantics = new RandomIndexing(
					new IndexVectorMap().getIndexVectorMap(), new ContextVectorMap().getContextVectorMap(), RandomIndexing.textSemanticType);
		  RandomIndexing recipientSemantics = new RandomIndexing(
				  new IndexVectorMap().getIndexVectorMap(), new ContextVectorMap().getContextVectorMap(), RandomIndexing.peopleSemanticType);
		  
		  textSemantics.setWordDocumentFrequencies(new HashMap<String, Integer>());
		  recipientSemantics.setWordDocumentFrequencies(new HashMap<String, Integer>());

		  List<Email> emails = new ArrayList<Email>();
		  int x = 1;
		  System.out.println("no of mails in the directory : " + mailFiles.length);
		  for (File tmpFile : mailFiles) {
		     MimeMessage email = null;
		     try {
		        FileInputStream fis = new FileInputStream(tmpFile);
		        email = new MimeMessage(session, fis);
		        System.out.println("-------------------------------------");
		        System.out.println("mailNo : " + x);
		        System.out.println("content type: " + email.getContentType());
		        System.out.println("subject: " + email.getSubject());
		      
		       //System.out.println("recipients: " + Arrays.asList(email.getRecipients(Message.RecipientType.TO))); 
		       //String messageContent = EmailUtils.getTextFromEmail(email);
//		        EmailBody emailBody = EmailUtils.getEmailBody(email, new EmailBody());
//		        String messageContent = emailBody.getMessageContent();
//		        System.out.println("message : \n" + messageContent);
//		        
//		        String replyLines = replyExtractor.getMsgReplyLines(messageContent);
//		        String signature = signatureDetector.getSignatureLines(messageContent);
//		        //System.out.println("Reply Line : " + replyLines);
		        //System.out.println("Signature : " + signature);
		        
		        Email newEmail = EmailUtils.processEmail(email, "test@enron.com", x);
		        
		        textSemantics = emailAnalysisService.processTextSemantics(
						newEmail, textSemantics);
				recipientSemantics = emailAnalysisService
						.processPeopleSemantics(newEmail, recipientSemantics);
				
				//get all feature vector
				//EmailUtils.getAllFeatureVector(newEmail);
				
				//speech act results
//				System.out.println("body content token stream : " + newEmail.getBodyContent().getTokenStream());
//				System.out.println("is commit : " + newEmail.isCommit());
//				System.out.println("is delivery : " + newEmail.isDelivery());
//				System.out.println("is meeting : " + newEmail.isMeeting());
//				System.out.println("is proposal : " + newEmail.isPropose());
//				System.out.println("is request : " + newEmail.isRequest());
//				
				emails.add(newEmail);
		        System.out.println("\n");
		        x++;
		        
		     } catch (Exception e) {
		    	 System.err.println("Error while processing email : " + e.getMessage());
		    	 e.printStackTrace();
		     }
		  }
		  //kmeans clustering
		  clusterEnronEmails(emails);			
	}

	public static void testStanfordNlp(){
	    // creates a StanfordCoreNLP object, with POS tagging, lemmatization, NER, parsing, and coreference resolution 
	    Properties props = new Properties();
	    props.setProperty("annotators", "tokenize, ssplit, pos, lemma, ner, parse, dcoref");
	    StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
	    
	    // read some text in the text variable
	    String text = "Hi Dileepa, Ian Bell is a cricketer from England. he also works for the Bank of england and works well with meetings and organizations"
	    		+ " Thanks, Richard "; // Add your text here!
	    
	    String text2 = "Hello,I am attempting to build Stanbol on my local machine, during the build I encounter the following error : Warning: Could not find resource url http://dev.iks-project.eu/downloads/stanbol-indices/dbpedia_26k.solrindex.bz2 to copy. "
	    		+ "I ran the dev.iks-project.eu server name through http://www.isitdownrightnow.com/ which returns server down. Could anyone suggest an alternative location to download the file from? Many thanks,John";
	    
	    // create an empty Annotation just with the given text
	    Annotation document = new Annotation(text2);
	    
	    // run all Annotators on this text
	    pipeline.annotate(document);
	    
	    // these are all the sentences in this document
	    // a CoreMap is essentially a Map that uses class objects as keys and has values with custom types
	    List<CoreMap> sentences = document.get(SentencesAnnotation.class);
	    
	    for(CoreMap sentence: sentences) {
	      // traversing the words in the current sentence
	      // a CoreLabel is a CoreMap with additional token-specific methods
	      for (CoreLabel token: sentence.get(TokensAnnotation.class)) {
	        // this is the text of the token
	        String word = token.get(TextAnnotation.class);
	        // this is the POS tag of the token
	        String pos = token.get(PartOfSpeechAnnotation.class);
	        // this is the NER label of the token
	        String ne = token.get(NamedEntityTagAnnotation.class);
	        
	        String category = token.category();
	        
	        System.out.println("word : " + word + " pos : " + pos + " Named entity : " + ne + " category : " + category);
	      }

	      // this is the parse tree of the current sentence
	      Tree tree = sentence.get(TreeAnnotation.class);

	      
	      tree.pennPrint();
	      // this is the Stanford dependency graph of the current sentence
	      SemanticGraph dependencies = sentence.get(CollapsedCCProcessedDependenciesAnnotation.class);
	      System.out.println("pretty printing dependency graph");
	      dependencies.prettyPrint();
	    }

	    // This is the coreference link graph
	    // Each chain stores a set of mentions that link to each other,
	    // along with a method for getting the most representative mention
	    // Both sentence and token offsets start at 1!
	    Map<Integer, CorefChain> graph = 
	      document.get(CorefChainAnnotation.class);
	    for(Integer key: graph.keySet()){
	    	CorefChain chain = graph.get(key);
	    	
	    }
	    
	    
	}
	
	public static void javaMailIteratorTest(){
		Properties props = new Properties();
		props.setProperty("mail.store.protocol", "imaps");
		try {
			
			//initializing stanfordNLP
			 EmailAnalysisService emailAnalysisService = new EmailAnalysisService();
				
			 RandomIndexing textSemantics = new RandomIndexing(
						new IndexVectorMap().getIndexVectorMap(), new ContextVectorMap().getContextVectorMap(), RandomIndexing.textSemanticType);
			 RandomIndexing recipientSemantics = new RandomIndexing(
					  new IndexVectorMap().getIndexVectorMap(), new ContextVectorMap().getContextVectorMap(), RandomIndexing.peopleSemanticType);

			
			Session session = Session.getInstance(props, null);
			Store store = session.getStore();
			store.connect("imap.gmail.com", "gdcdemo2013@gmail.com",
					"gdcdemo2013pass");
			Folder inbox = store.getFolder("Trash");
			//Folder inbox = store.getFolder("[Gmail]/Important");
			UIDFolder uf = (UIDFolder) inbox;

			inbox.open(Folder.READ_ONLY);
			Message msg = null;
			// Message[] messages = inbox.getMessages();
			int totalMsgs = inbox.getMessageCount();

			// Message[] messages = uf.getMessagesByUID(1, 10);

			Message lstMsg = inbox.getMessage(inbox.getMessageCount());

			// Date fromDate = new Date(1420070400000L);
			Date lastMessageSentDate = lstMsg.getSentDate();
			Calendar day = Calendar.getInstance();
			day.set(Calendar.MONTH, 0);
			//day.set(Calendar.DAY_OF_MONTH, 1);
			//day.setTime(lastMessageSentDate);
			//day.add(Calendar.MONTH, -1);
			Date fromDate = day.getTime();
			
			Calendar toDay = Calendar.getInstance();
			toDay.set(Calendar.MONTH, 2);
			toDay.set(Calendar.DAY_OF_MONTH, 13);
			Date toDate = toDay.getTime();
			System.out.println("from date : " + fromDate + " miliseconds : "
					+ fromDate.getTime());
			System.out.println("to date : " + toDate + " miliseconds : "
					+ toDate.getTime());

			SearchTerm newerThan = new ReceivedDateTerm(ComparisonTerm.GT,
					fromDate);
			SearchTerm olderThan = new ReceivedDateTerm(ComparisonTerm.LT,
					toDate);
			SearchTerm st = new AndTerm(
					   newerThan, 
					   olderThan);
			Message[] messages = inbox.search(newerThan);

			System.out.println("Total number of messages : " + totalMsgs);
			// System.out.println("Last message UID : " + uf.getUID(lstMsg));
			// System.out.println("last message subject : " +
			// lstMsg.getSubject() + " sent date : " + lstMsg.getSentDate());
			System.out.println("number of emails retrieving: "
					+ messages.length);

			List<Email> emailsList = new ArrayList<Email>();
			
			int limit = 1000;
			if(messages.length < limit){
				limit = messages.length - 1;
			}

			
			for (int x = 0; x < limit; x++) {
				msg = messages[x];

				String[] msgIds = msg.getHeader("Message-ID");
				long msgUID = uf.getUID(msg);
				System.out.println(x + " : Message UID : " + msgUID);
				System.out.println("Message Content Type : " + msg.getContentType());
				System.out.println("SUBJECT:" + msg.getSubject());
				System.out.println("SENT DATE:" + msg.getSentDate());
				

				Email email = EmailUtils.processEmail(msg, "dileepajayakody@gmail.com", msgUID);
				System.out.println("is email deleted : " + email.isDeleted());
//
//				//processing email semantics;
//				 
//		        textSemantics = emailAnalysisService.processTextSemantics(
//						email, textSemantics);
//				recipientSemantics = emailAnalysisService
//						.processPeopleSemantics(email, recipientSemantics);
//				System.out.println("the body content : " + email.getBodyContent().getTokenStream());
//				System.out.println("\n=====================================================================================\n");
//				emailsList.add(email);
			//}	    
		}//end for loop for messages
				
		} catch (Exception mex) {
			mex.printStackTrace();
		}

	}
	
	public static void testVectors(){
		double[] centroid = {1.0,5.0,3.0};
		double[] testVec = centroid;
		
		testVec = VectorsMath.devideArray(testVec, 2);
		System.out.println("testVec : " + testVec[0] + " , " + testVec[1] + " , " + testVec[2] );
		System.out.println("centroid : " + centroid[0] + " , " + centroid[1] + " , " + centroid[2] );
	}
	
	
	public void processEnronImportanceResults(){
		 File importantMailDir = new File("/home/dileepa/Desktop/research/DATA/reputationBasedSpamFilter/Enron_Sample_Data/enron_mail_20110402/maildir/stclair-c/important_e_mails");
		 	
		 File[] mailFiles = importantMailDir.listFiles();
		 String host = "host.com";
		 java.util.Properties properties = System.getProperties();
		 properties.setProperty("mail.smtp.host", host);
		 Session session = Session.getDefaultInstance(properties);
		 EmailAnalysisService emailAnalysisService = new EmailAnalysisService();
			
		  RandomIndexing textSemantics = new RandomIndexing(
					new IndexVectorMap().getIndexVectorMap(), new ContextVectorMap().getContextVectorMap(), RandomIndexing.textSemanticType);
		  RandomIndexing recipientSemantics = new RandomIndexing(
				  new IndexVectorMap().getIndexVectorMap(), new ContextVectorMap().getContextVectorMap(), RandomIndexing.peopleSemanticType);
		  
		  textSemantics.setWordDocumentFrequencies(new HashMap<String, Integer>());
		  recipientSemantics.setWordDocumentFrequencies(new HashMap<String, Integer>());

		  List<Email> emails = new ArrayList<Email>();
		  int x = 1;
		  System.out.println("no of mails in the directory : " + mailFiles.length);
		  for (File tmpFile : mailFiles) {
		     MimeMessage email = null;
		     try {
		        FileInputStream fis = new FileInputStream(tmpFile);
		        email = new MimeMessage(session, fis);
		        System.out.println("-------------------------------------");
		        System.out.println("mailNo : " + x);
		        System.out.println("content type: " + email.getContentType());
		        System.out.println("subject: " + email.getSubject());
		      
		       //System.out.println("recipients: " + Arrays.asList(email.getRecipients(Message.RecipientType.TO))); 
		       //String messageContent = EmailUtils.getTextFromEmail(email);
//		        EmailBody emailBody = EmailUtils.getEmailBody(email, new EmailBody());
//		        String messageContent = emailBody.getMessageContent();
//		        System.out.println("message : \n" + messageContent);
//		        
//		        String replyLines = replyExtractor.getMsgReplyLines(messageContent);
//		        String signature = signatureDetector.getSignatureLines(messageContent);
//		        //System.out.println("Reply Line : " + replyLines);
		        //System.out.println("Signature : " + signature);
		        
		        Email newEmail = EmailUtils.processEmail(email, "carol.clair@enron.com", x);
		        
		        textSemantics = emailAnalysisService.processTextSemantics(
						newEmail, textSemantics);
				recipientSemantics = emailAnalysisService
						.processPeopleSemantics(newEmail, recipientSemantics);
				
				//setting these emails as important by flagging emails
				newEmail.setFlagged(true);
				newEmail.setSeen(true);
				
				emails.add(newEmail);
					
		        System.out.println("\n");
		        x++;
		        
		     } catch (Exception e) {
		    	 System.err.println("Error while processing email : " + e.getMessage());
		    	 e.printStackTrace();
		     }
		  }
		 
		  UserMailBox mb = new UserMailBox();
		  mb.setAllEmails(emails);
		  //now create the importance model using it
		  mb = EmailUtils.calculateImportanceModel(mb);
		  
		  clusterEnronEmails(emails);
		  
		  List<Email> allEmails = mb.getAllEmails();
		  for(Email email : allEmails){
			  mb.predictImportanceFormEmail(email);
			  email.getContentReputationScore();
			  double flaggedKeywordScore = email.getFlaggedKeywordscore();
			  double flaggedFromScore = email.getFlaggedPeopleFromscore();
			  double flaggedCCScore = email.getFlaggedPeopleCCscore();
			  double flaggedToScore = email.getFlaggedPeopleToscore();
			  double flaggedTopicScore = email.getFlaggedTopicscore();
			  double flaggedSubjectScore = email.getFlaggedTopicSubjectscore();
			  double flaggedBodyScore = email.getFlaggedTopicBodyscore();
			
			  System.out.println("email : uid : " + email.getMsgUid() + " subject : " + email.getSubject() );
			  String toAddrs = "";
	    		if(email.getToAddresses() != null && email.getToAddresses().size() > 0){
	    			for(String toAdd : email.getToAddresses()){
		    			toAddrs += toAdd + ", ";
		    		}	
	    		}
	    		
	    		String ccAddr = "";
	    		if(email.getCcAddresses() != null && email.getCcAddresses().size() > 0){
	    			for(String ccAdd : email.getCcAddresses()){
		    			ccAddr += ccAdd + ", ";
		    		}	
	    		}
			  System.out.println(" from : " + email.getFromAddress() + " to : " + toAddrs + " cc :" + ccAddr);
			  System.out.println("predicted scores for email; flaggedKeywordScore: " + flaggedKeywordScore + 
					  " flaggedFromScore : " + flaggedFromScore + " flaggedToScore : " + flaggedToScore + " flagged cc score : " + flaggedCCScore + 
					  " flaggedTopicScore : " + flaggedTopicScore + " flaggedSubject score : " + flaggedSubjectScore + " flagged bodyscore : " + flaggedBodyScore);
			  System.out.println("\n\n");
		  }
		  
	}
	
	public void clusterEnronEmails(List<Email> emails){
		KMeansClustering kmeans = new KMeansClustering();
		
		System.out.println("Clustering emails by Content....");
		List<EmailContentCluster> contentClusters = kmeans.clusterBasedOnContent(emails);
		int totalEMailsInContentClusters = 0;
		double contentSumOfSquaredError = 0;
		
		for (EmailContentCluster cluster : contentClusters) {
			System.out.println("-----------------------------------------------------------------------------------");
			System.out.println("Cluster ID : " + cluster.getId() + " No.of emails : " + cluster.getContentEmails().size());
					
			int clusterSize = cluster.getContentEmails().size();
			totalEMailsInContentClusters += clusterSize;
			for (Email email : cluster.getContentEmails()) {
				//email.setTextClusterId(cluster.getId());
				String toAddrs = "";
	    		if(email.getToAddresses() != null && email.getToAddresses().size() > 0){
	    			for(String toAdd : email.getToAddresses()){
		    			toAddrs += toAdd + ", ";
		    		}	
	    		}
	    		
	    		String ccAddr = "";
	    		if(email.getCcAddresses() != null && email.getCcAddresses().size() > 0){
	    			for(String ccAdd : email.getCcAddresses()){
		    			ccAddr += ccAdd + ", ";
		    		}	
	    		}
	    		System.out.println(email.getMsgUid() + " : Email subject : " + email.getSubject());
			}				
			contentSumOfSquaredError += cluster.getSumOfSquaresError();
		}
		System.out.println("TOTAL EMAILS in all content Clusters : " + totalEMailsInContentClusters + " No. of clusters : " + contentClusters.size());
		System.out.println("Sum of Squared values for all content clusters : " + contentSumOfSquaredError);
		double dunnIndex = EmailUtils.getDunnIndexForContentClusters(contentClusters);
		System.out.println("Dunn Index for content clusters : " + dunnIndex);
		
		//clustering weighted subject  body content clusters
		System.out.println("Clustering emails by Weighted Subject Body Content....");
		List<EmailWeightedSubjectBodyContentCluster> weightedContentClusters = kmeans.clusterBasedOnSubjectAndBody(emails);
		int totalEMailsInWeightedContentClusters = 0;
		//double weightedContentSumOfSquaredError = 0;
	
		for (EmailWeightedSubjectBodyContentCluster cluster : weightedContentClusters) {
			System.out.println("-----------------------------------------------------------------------------------");
			System.out.println("Cluster ID : " + cluster.getId() + " No.of emails : " + cluster.getSubjectBodyContentEmails().size());
					
			int clusterSize = cluster.getSubjectBodyContentEmails().size();
			totalEMailsInWeightedContentClusters += clusterSize;
			for (Email email : cluster.getSubjectBodyContentEmails()) {
				//email.setTextClusterId(cluster.getId());
				String toAddrs = "";
	    		if(email.getToAddresses() != null && email.getToAddresses().size() > 0){
	    			for(String toAdd : email.getToAddresses()){
		    			toAddrs += toAdd + ", ";
		    		}	
	    		}
	    		
	    		String ccAddr = "";
	    		if(email.getCcAddresses() != null && email.getCcAddresses().size() > 0){
	    			for(String ccAdd : email.getCcAddresses()){
		    			ccAddr += ccAdd + ", ";
		    		}	
	    		}
	    		System.out.println(email.getMsgUid() + " : Email subject : " + email.getSubject());

			}				
			//weightedContentSumOfSquaredError += cluster.getSumOfSquaresError();
		}
		System.out.println("TOTAL EMAILS in all weighted content Clusters : " + totalEMailsInWeightedContentClusters + " No. of clusters : " + weightedContentClusters.size());
		double subjectCentroidDunnIndex = EmailUtils.getDunnIndexForWeightedSubBodyContentClusters(weightedContentClusters, EmailWeightedSubjectBodyContentCluster.subjectVectorType);
		double bodyCentroidDunnIndex = EmailUtils.getDunnIndexForWeightedSubBodyContentClusters(weightedContentClusters, EmailWeightedSubjectBodyContentCluster.bodyVectorType);
		
		System.out.println("Dunn Index for subject centroids : " + subjectCentroidDunnIndex);
		System.out.println("Dunn Index for body centroids : " + bodyCentroidDunnIndex);
	}
	
	
	

}
