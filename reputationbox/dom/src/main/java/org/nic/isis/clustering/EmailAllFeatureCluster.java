package org.nic.isis.clustering;

import java.util.ArrayList;
import java.util.List;

import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.VersionStrategy;

import org.apache.isis.applib.annotation.ObjectType;
import org.apache.isis.applib.annotation.Programmatic;
import org.nic.isis.reputation.dom.Email;
import org.nic.isis.ri.RandomIndexing;
import org.nic.isis.vector.VectorsMath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucla.sspace.common.Similarity;

@javax.jdo.annotations.PersistenceCapable(identityType=IdentityType.DATASTORE)
@javax.jdo.annotations.DatastoreIdentity(
        strategy=javax.jdo.annotations.IdGeneratorStrategy.IDENTITY,
         column="id")
@javax.jdo.annotations.Version(
        strategy=VersionStrategy.VERSION_NUMBER, 
        column="version")
@ObjectType("EMAILALLFEATURECLUSTER")
public class EmailAllFeatureCluster {
	public final static String TEXT_CLUSTER_TYPE = "TOPIC";
	public final static String RECIPIENT_CLUSTER_TYPE = "PEOPLE";
	
	protected String id;
	//cannot persist maps in Isis
	protected List<Email> emails;
	protected List<String> emailIds;
	protected double[] centroid = new double[8016];
	protected String clusterType;
	
	protected int noOfMessagesAnswered;
	protected int noOfMessagesSeen;
	//flagged as important
	protected int noOfMessagesFlagged;
	protected int noOfMessagesDeleted;
	
	protected double reputationScore;
	
	public static final int FLAGGED_WEIGHT = 5, ANSWERERD_WEIGHT = 4, SEEN_WEIGHT = 2, DELETED_WEIGHT = 11;
	
	private final static Logger logger = LoggerFactory
			.getLogger(EmailAllFeatureCluster.class);
	
	public EmailAllFeatureCluster(){
		centroid = new double[8016];
	}

	public EmailAllFeatureCluster(double[] v){
		 centroid = v;
	}
	
	public EmailAllFeatureCluster(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}

	public String getEmailId(int pos) {
		return emailIds.get(pos);
	}

	


	public void addEmail(String emailId, Email email) {
		addVector(email.getAllFeatureVector());
		
		if(this.getEmailIds() == null){
			this.setEmailIds(new ArrayList<String>());
		}
		if(this.getEmails() == null){
			this.setEmails(new ArrayList<Email>());
		}
		
		logger.info(" Adding email " + email.getMessageId() + "to content cluster : " + this.id);
		this.emails.add(email);
		this.emailIds.add(emailId);
		
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

	}

	public void removeEmail(String emailId) {
		
		for(int i=0; i< emails.size(); i++){
			Email email = emails.get(i);
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
				emails.remove(i);
				//logger.info("REMOVING email : " + email.getMessageId() + " from cluster : " + this.id);
				break;
			}
		}
	}
	
	
	@javax.jdo.annotations.Persistent
	public double getReputationScore() {
		return reputationScore;
	}

	public void setReputationScore(double reputationScore) {
		this.reputationScore = reputationScore;
	}

	public int size() {
		return getEmails().size();
	}

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public List<Email> getEmails() {
		return emails;
	}

	public void setEmails(List<Email> emails) {
		this.emails = emails;
	}

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public List<String> getEmailIds() {
		return emailIds;
	}

	public void setEmailIds(List<String> emailIds) {
		this.emailIds = emailIds;
	}
	
	public int getNoOfMessagesAnswered() {
		return noOfMessagesAnswered;
	}

	public void setNoOfMessagesAnswered(int noOfMessagesAnswered) {
		this.noOfMessagesAnswered = noOfMessagesAnswered;
	}

	public int getNoOfMessagesSeen() {
		return noOfMessagesSeen;
	}

	public void setNoOfMessagesSeen(int noOfMessagesSeen) {
		this.noOfMessagesSeen = noOfMessagesSeen;
	}

	public int getNoOfMessagesFlagged() {
		return noOfMessagesFlagged;
	}

	public void setNoOfMessagesFlagged(int noOfMessagesFlagged) {
		this.noOfMessagesFlagged = noOfMessagesFlagged;
	}

	public int getNoOfMessagesDeleted() {
		return noOfMessagesDeleted;
	}

	public void setNoOfMessagesDeleted(int noOfMessagesDeleted) {
		this.noOfMessagesDeleted = noOfMessagesDeleted;
	}
	
	public void addMessageFlagged(){
		this.noOfMessagesFlagged++;
	}
	
	public void addMessageSeen(){
		this.noOfMessagesSeen++;
	}

	public void addMessageAnswered(){
		this.noOfMessagesAnswered++;
	}
	
	public void addMessageDeleted(){
		this.noOfMessagesDeleted++;
	}
	
	//when removing a message needs to reduce these indexes as well
	public void decrementMessageFlagged(){
		this.noOfMessagesFlagged--;
	}
	
	public void decrementMessageSeen(){
		this.noOfMessagesSeen--;
	}

	public void decrementMessageAnswered(){
		this.noOfMessagesAnswered--;
	}
	
	public void decrementMessageDeleted(){
		this.noOfMessagesDeleted--;
	}
	
	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public String getClusterType() {
		return clusterType;
	}

	public void setClusterType(String clusterType) {
		this.clusterType = clusterType;
	}
	
	public double[] getCentroid() {
		return centroid;
	}

	@Programmatic
	public double getSimilarity(double[] vector) {
		return Similarity.cosineSimilarity(centroid, vector);
	}
	
	@Programmatic
    public void addVector(double[] vector) {
		centroid = VectorsMath.addArrays(centroid, vector);
        centroid = VectorsMath.devideArray(centroid, 2);
    }

	public void merge(CentroidCluster other) {
        centroid = VectorsMath.addArrays(centroid, other.getCentroid());
        centroid = VectorsMath.devideArray(centroid, 2);
          
    }
	
	public double[] calculateAverageCentroid() {
		// TODO Auto-generated method stub
		return centroid;
	}
	
	public double getSumOfSquaresError(){
		double sumOfSquaredError = 0; 
		for(Email email : this.getEmails()){
			double[] x = email.getAllFeatureVector();
			double[] centroid = this.getCentroid();
			double squaredDistance = VectorsMath.getSquaredDistance(x, centroid);
			sumOfSquaredError += squaredDistance;
		}
		return sumOfSquaredError;
	}

	public double calculateClusterReputationScore(){
		int totalPositiveWeight = FLAGGED_WEIGHT + ANSWERERD_WEIGHT
				+ SEEN_WEIGHT;
		double reputationScore = (double)((noOfMessagesFlagged * FLAGGED_WEIGHT)
				+ (noOfMessagesAnswered * ANSWERERD_WEIGHT)
				+ (noOfMessagesSeen * SEEN_WEIGHT) - (noOfMessagesDeleted * DELETED_WEIGHT)) 
				/ (double)(this.getEmails().size() * totalPositiveWeight);
		
		logger.info("Calculating reputation score of content cluster id : " + this.id 
				+ " : ("+ noOfMessagesFlagged + " * " + FLAGGED_WEIGHT + ") + ("
				+ noOfMessagesAnswered + " * " + ANSWERERD_WEIGHT + ") + ("
				+ noOfMessagesSeen + " * " + SEEN_WEIGHT + ") - ("
				+ noOfMessagesDeleted + " * " + DELETED_WEIGHT + ") / (" 
				+ this.getEmails().size() +" * " + totalPositiveWeight + ")"
				+ " Score: " + reputationScore);
		this.setReputationScore(reputationScore);
		return this.reputationScore;
	}
}
