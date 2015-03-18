package org.nic.isis.clustering;

import java.util.ArrayList;
import java.util.List;

import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.VersionStrategy;

import org.apache.isis.applib.annotation.ObjectType;
import org.nic.isis.reputation.dom.Email;
import org.nic.isis.ri.RandomIndexing;
import org.nic.isis.vector.VectorsMath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@javax.jdo.annotations.PersistenceCapable(identityType=IdentityType.DATASTORE)
@javax.jdo.annotations.DatastoreIdentity(
        strategy=javax.jdo.annotations.IdGeneratorStrategy.IDENTITY,
         column="id")
@ObjectType("EMAILCONTENTCLUSTER")
public class EmailContentCluster extends EmailCluster {
	
	private final static Logger logger = LoggerFactory
			.getLogger(EmailContentCluster.class);
	
	private List<Email> contentEmails;	
	private List<String> contentEmailIds;

	public EmailContentCluster(double[] v) {
		super(v);
		this.setClusterType(TEXT_CLUSTER_TYPE);
	}
	
	public EmailContentCluster(String id) {
		super(id);
	}
	
	public void addEmail(String emailId, Email email){
		//super.addEmail(emailId, email);
		super.addVector(email.getTextContextVector());
		
		if(this.getContentEmailIds() == null){
			this.setContentEmailIds(new ArrayList<String>());
		}
		if(this.getContentEmails() == null){
			this.setContentEmails(new ArrayList<Email>());
		}
		
		//logger.info(" Adding email " + email.getMessageId() + "to content cluster : " + this.id);
		this.contentEmails.add(email);
		this.contentEmailIds.add(emailId);
		
		//addEmailAtributes
		if (email.isAnswered()) {
			this.addMessageAnswered();
			//calculate the response time for the email
			String receivedMessageId = email.getMessageId();
			
		}
		if (email.isFlagged()) {
			this.addMessageFlagged();
		}
		if (email.isSeen()){
			this.addMessageSeen();
		}
		if(email.isDeleted()){
			this.addMessageDeleted();
		}
		/*centroid = VectorsMath.addArrays(centroid, email.getTextContextVector());
		centroid = VectorsMath.devideArray(centroid, emails.size());*/
	}
	
	public void removeEmail(String emailId) {
		
		for(int i=0; i< this.getContentEmails().size(); i++){
			Email email = this.getContentEmails().get(i);
			if(email.getMessageId().equals(emailId)){
				if(email.isAnswered()){
					decrementMessageAnswered();
				}
				if (email.isFlagged()) {
					this.decrementMessageFlagged();
				}
				if (email.isSeen()){
					this.decrementMessageSeen();
				}
				if(email.isDeleted()){
					this.decrementMessageDeleted();
				}
				this.getContentEmails().remove(i);
				//logger.info("REMOVING email : " + email.getMessageId() + " from cluster : " + this.id);
				break;
			}
		}
	}
	
	public double calculateClusterReputationScore(){
		int totalPositiveWeight = FLAGGED_WEIGHT + ANSWERERD_WEIGHT
				+ SEEN_WEIGHT;
		double reputationScore = (double)((noOfMessagesFlagged * FLAGGED_WEIGHT)
				+ (noOfMessagesAnswered * ANSWERERD_WEIGHT)
				+ (noOfMessagesSeen * SEEN_WEIGHT) - (noOfMessagesDeleted * DELETED_WEIGHT)) 
				/ (double)(this.getContentEmails().size() * totalPositiveWeight);
		
		logger.info("Calculating reputation score of content cluster id : " + this.id 
				+ " : ("+ noOfMessagesFlagged + " * " + FLAGGED_WEIGHT + ") + ("
				+ noOfMessagesAnswered + " * " + ANSWERERD_WEIGHT + ") + ("
				+ noOfMessagesSeen + " * " + SEEN_WEIGHT + ") - ("
				+ noOfMessagesDeleted + " * " + DELETED_WEIGHT + ") / (" 
				+ this.getContentEmails().size() +" * " + totalPositiveWeight + ")"
				+ " Score: " + reputationScore);
		this.setReputationScore(reputationScore);
		return this.reputationScore;
	}
	/**
	 * Returns a document consisting of the average of the vectors of the
	 * documents in the cluster.
	 * 
	 * @return the centroid of the cluster
	 */
	public double[] calculateAverageCentroid(){
		if (emails.size() == 0) {
			return null;
		}
		
		double[] tempVector = new double[RandomIndexing.DEFAULT_VECTOR_LENGTH];
		for (Email em : emails) {
			double[] contextVector = null;
			contextVector = em.getTextContextVector();
			tempVector = VectorsMath.addArrays(tempVector, contextVector);
		}
		centroid = new double[RandomIndexing.DEFAULT_VECTOR_LENGTH];
		centroid = VectorsMath.devideArray(tempVector, emails.size());
		return centroid;
	}	
	
	public double getSumOfSquaresError(){
		double sumOfSquaredError = 0; 
		for(Email email : this.getContentEmails()){
			double[] x = email.getTextContextVector();
			double[] centroid = this.getCentroid();
			double squaredDistance = VectorsMath.getSquaredDistance(x, centroid);
			sumOfSquaredError += squaredDistance;
		}
		return sumOfSquaredError;
	}

	

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public List<Email> getContentEmails() {
		return contentEmails;
	}

	public void setContentEmails(List<Email> contentEmails) {
		this.contentEmails = contentEmails;
	}
	
	public List<String> getContentEmailIds() {
		return contentEmailIds;
	}

	public void setContentEmailIds(List<String> contentEmailIds) {
		this.contentEmailIds = contentEmailIds;
	}
}
