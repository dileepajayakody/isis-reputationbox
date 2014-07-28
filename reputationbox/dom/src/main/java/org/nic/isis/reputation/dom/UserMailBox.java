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
import javax.jdo.annotations.VersionStrategy;

import org.apache.isis.applib.ViewModel;
import org.apache.isis.applib.annotation.ObjectType;
import org.apache.isis.applib.annotation.Programmatic;
import org.nic.isis.ri.RandomIndexing;

import edu.ucla.sspace.index.RandomIndexVectorGenerator;
import edu.ucla.sspace.util.GeneratorMap;
import edu.ucla.sspace.vector.CompactSparseIntegerVector;
import edu.ucla.sspace.vector.CompactSparseVector;
import edu.ucla.sspace.vector.IntegerVector;
import edu.ucla.sspace.vector.SparseDoubleVector;
import edu.ucla.sspace.vector.TernaryVector;
import edu.ucla.sspace.vector.Vector;
import edu.ucla.sspace.vector.VectorMath;

/**
 * @author dileepa
 * 
 */
@javax.jdo.annotations.Queries({ @javax.jdo.annotations.Query(name = "findMailbox", language = "JDOQL", value = "SELECT FROM org.nic.isis.reputation.dom.UserMailBox WHERE  emailId == : emailId") })
@javax.jdo.annotations.PersistenceCapable(identityType = IdentityType.DATASTORE)
@javax.jdo.annotations.DatastoreIdentity(strategy = javax.jdo.annotations.IdGeneratorStrategy.IDENTITY, column = "id")
@javax.jdo.annotations.Version(strategy = VersionStrategy.VERSION_NUMBER, column = "version")
@ObjectType("USERMAILBOX")
public class UserMailBox {

	public UserMailBox() {
		this.allEmailContacts = new ArrayList<EmailContact>();
		this.allEmails = new ArrayList<Email>();
		//required setup for email text analysis using RandomIndexing
		RandomIndexVectorGenerator indexVectorGenerator = 
                new RandomIndexVectorGenerator(RandomIndexing.DEFAULT_VECTOR_LENGTH, System.getProperties());
		this.wordToIndexVector = new GeneratorMap<TernaryVector>(
                indexVectorGenerator);
		this.wordToMeaningMap = new HashMap<String, int[]>();
	}

	// contextio account id
	private String accountId;

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "false")
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
	public String getEmailId() {
		return emailId;
	}

	public void setEmailId(String userId) {
		this.emailId = userId;
	}

	// endregion

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
	private int emailCount = 0;

	@javax.jdo.annotations.Persistent
	public int getEmailCount() {
		return emailCount;
	}

	public void setEmailCount(int emailCount) {
		this.emailCount = emailCount;
	}

	// endregion


	// region > lastIndexTimestamp (property)
	// last index timestamp of the context.io call
	private int lastIndexTimestamp = 0;

	@javax.jdo.annotations.Persistent
	public int getLastIndexTimestamp() {
		return lastIndexTimestamp;
	}

	public void setLastIndexTimestamp(int lastIndexTimestamp) {
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
		this.allEmails.add(email);
		this.lastIndexTimestamp = email.getSentTimestamp();
		emailCount++;
	}

	// endregion

	/**
	 * processes the semantic vectors for the content of the email using random indexing
	 * @param email
	 * @param randomIndex
	 * @throws IOException
	 */
	public void processTextSemantics(Email email, RandomIndexing randomIndex) throws IOException {
		String processedTokenStream = email.getTextContent().getTokenStream();
		randomIndex.processDocument(new BufferedReader(new StringReader(
				processedTokenStream)));
		int[] docSemanticVector = new int[randomIndex.getVectorLength()];
		for (String word : email.getTextContent().getStringTokens().keySet()) {
			int[] semanticVector = randomIndex.getContextVector(word);
			int frequency = email.getTextContent().getStringTokens().get(word);
				//add the semantic vector of the word * the no. of times its mentioned in the doc
				docSemanticVector = RandomIndexing.addVectors(
						docSemanticVector, semanticVector, frequency);

		}
		email.setDocumentContextVector(docSemanticVector);
	}

	// region > allEmailContacts (collection)
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

	public boolean isSyncing() {
		return isSyncing;
	}

	public void setSyncing(boolean isSyncing) {
		this.isSyncing = isSyncing;
	}
	
	/**
    * A mapping from each word to its associated index vector
    */
	private Map<String,TernaryVector> wordToIndexVector;
	
	@javax.jdo.annotations.Column(allowsNull = "true")
	public Map<String,TernaryVector> getWordToIndexVector() {
		return wordToIndexVector;
	}

	public void setWordToIndexVector(Map<String,TernaryVector> wordToIndexVector) {
		this.wordToIndexVector = wordToIndexVector;
	}

	
	/**
     * A mapping from each word to the vector the represents its semantics
     */
    private Map<String, int[]> wordToMeaningMap;
    
    public void setWordToMeaningMap(Map<String, int[]> wordToMeaning) {
		this.wordToMeaningMap = wordToMeaning;
	}

    @javax.jdo.annotations.Column(allowsNull = "true")
	public Map<String, int[]> getWordToMeaningMap() {
		return wordToMeaningMap;
	}

}
