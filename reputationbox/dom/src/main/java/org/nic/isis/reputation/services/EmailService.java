package org.nic.isis.reputation.services;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.inject.Inject;

import org.nic.isis.reputation.dom.Email;
import org.nic.isis.reputation.dom.UserMailBox;
import org.nic.isis.ri.RandomIndexing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.isis.applib.DomainObjectContainer;
import org.apache.isis.applib.annotation.Named;
import org.apache.isis.applib.annotation.Programmatic;


public class EmailService {

	private final static Logger logger = LoggerFactory
			.getLogger(EmailService.class);

	/**
	 * sync all mailboxes with new emails since last indexed timestamp
	 */
	public void syncMailBoxes() {
		List<UserMailBox> allMailBoxes = listAllMailBoxes();
		if (allMailBoxes == null || allMailBoxes.isEmpty()) {
			logger.info("There is no mailboxes in test-datastore. creating test mailboxes for connected accounts in contextio");
			allMailBoxes = new ArrayList<UserMailBox>();
			allMailBoxes.add(create("gdc2013demo@gmail.com"));
		}
		
		for (UserMailBox mailBox : allMailBoxes) {
			//for testing email update and analysis in one transaction..
			try {
				mailBox = contextIOService.updateMailBox(mailBox, 4);
				RandomIndexing randomIndexing = new RandomIndexing(mailBox.getWordToIndexVector(), mailBox.getWordToMeaningMap());
				//processing random indexing for emails
				for(Email email : mailBox.getAllEmails()){
					mailBox.processTextSemantics(email, randomIndexing);
				}
				mailBox.setWordToIndexVector(randomIndexing.getWordToIndexVector());
				mailBox.setWordToMeaningMap(randomIndexing.getWordToMeaningVector());
				
				logger.info("The context vectors of emails processed by Random Indexing: " + mailBox.getEmailId());
				List<int[]> documentVectors = new ArrayList<int[]>();
				for(Email email : mailBox.getAllEmails()){
					int[] docVector = email.getDocumentContextVector();
					documentVectors.add(docVector);
					
					String vectorString = "[";
					for (int i = 0; i < docVector.length; i++){
						int	 val = docVector[i];
						vectorString +=  val + ", ";  
					}
					vectorString += "]";
					logger.info(email.getMessageId() + " : " + vectorString);
				}	
				
				/*LinkClustering linkClustering = new LinkClustering();
				SparseMatrix m = Matrices.asSparseMatrix(documentVectors);
				Assignments a = linkClustering.cluster(m, new Properties());
			    Assignment[] assignments = a.assignments();
			      for (int i = 0; i < assignments.length; ++i) 
			          System.out.printf("Email %d is in clusters %s%n",
			                            i, Arrays.toString(assignments[i].assignments()));
*/
				/*for(Email email : mailBox.getAllEmails()){
					for (Email innerEmail : mailBox.getAllEmails()){
						if (!email.getMessageId().equalsIgnoreCase(innerEmail.getMessageId())){
							DoubleVector v1 = email.getDocumentContextVector();
							DoubleVector v2 = innerEmail.getDocumentContextVector();
							double similarity = Similarity.cosineSimilarity(v1, v2);
							logger.info(" Similarity of EMAIL_1 : " + email.getSubject() + " and \n EMAIL_2 : " + innerEmail.getSubject() + " =  " + similarity);
							logger.info("EMAIL_1 tokens : " + email.getTextContent().getTokenStream());
							logger.info("EMAIL_2 tokens : " + innerEmail.getTextContent().getTokenStream() + " \n\n");
						}
						
					}
				}*/
				
			} catch (Exception e) {
				logger.error("Error occurred  " , e);
			}
			
			
			container.persistIfNotAlready(mailBox);
			
			/*if (!mailBox.isSyncing()) {
				mailBox.setSyncing(true);
				while (mailBox.isSyncing()) {
					contextIOService.updateMailBox(mailBox, 20);
				}
				container.persist(mailBox);
				logger.info("updated the mailBox: " + mailBox.getEmailId()
						+ " with " + mailBox.getEmailCount() + " emails");
			}*/

		}
	}

	@Programmatic
	public List<UserMailBox> listAllMailBoxes() {
		return container.allInstances(UserMailBox.class);
	}

	@Programmatic
	public UserMailBox create(final String userId) {
		UserMailBox mb = container.newTransientInstance(UserMailBox.class);
		mb.setEmailId(userId);
		mb.setAccountId("53214991facaddd22d812863");
		container.persistIfNotAlready(mb);
		return mb;
	}

	public void connectMailBox(@Named("Email Id") String emailId,
			@Named("Password") String password,
			@Named("First Name") String fname, @Named("Last Name") String lname) {
		UserMailBox newMb = contextIOService.connectMailBox(emailId, password,
				fname, lname);

	}

	// region > dependencies
	@Inject
	DomainObjectContainer container;
	@Inject
	ContextIOService contextIOService;
	// endregion
}
