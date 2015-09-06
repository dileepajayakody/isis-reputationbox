package org.nic.isis.clustering;

import java.util.ArrayList;
import java.util.List;

import javax.jdo.annotations.IdentityType;

import org.apache.isis.applib.annotation.ObjectType;
import org.apache.isis.applib.annotation.Programmatic;
import org.nic.isis.reputation.dom.Email;
import org.nic.isis.reputation.dom.EmailBody;
import org.nic.isis.reputation.utils.EmailUtils;
import org.nic.isis.ri.RandomIndexing;
import org.nic.isis.vector.VectorsMath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucla.sspace.common.Similarity;

@javax.jdo.annotations.PersistenceCapable(identityType=IdentityType.DATASTORE)
@javax.jdo.annotations.DatastoreIdentity(
        strategy=javax.jdo.annotations.IdGeneratorStrategy.IDENTITY,
         column="id")
@ObjectType("EMAILWEIGHTEDSUBJECTBODYCONTENTCLUSTER")
public class EmailWeightedSubjectBodyContentCluster {
	
	protected String id;
	//cannot persist maps in Isis
	protected List<Email> subjectBodyContentEmails;
	protected List<String> emailIds;
	//weight subject:body 1:1
	private double[] subjectCentroid  = new double[RandomIndexing.DEFAULT_VECTOR_LENGTH];
	private double[]  bodyCentroid  = new double[RandomIndexing.DEFAULT_VECTOR_LENGTH];

	protected String clusterType = "WEIGHTED_SUBJECT_BODY_CONTENT";
	
	public static final String subjectVectorType = "SUBJECT_VECTOR";
	public static final String bodyVectorType = "BODY_VECTOR";
	
	
	protected int noOfMessagesAnswered;
	protected int noOfMessagesSeen;
	//flagged as important
	protected int noOfMessagesFlagged;
	protected int noOfMessagesDeleted;
	
	protected double reputationScore;
	
	public static final int FLAGGED_WEIGHT = 5, ANSWERERD_WEIGHT = 4, SEEN_WEIGHT = 2, DELETED_WEIGHT = 11;
	
	private final static Logger logger = LoggerFactory
			.getLogger( EmailWeightedSubjectBodyContentCluster.class);
	
	public EmailWeightedSubjectBodyContentCluster(){
		this.setClusterType(EmailCluster.WEIGHTED_SUBJECT_BODY_CLUSTER_TYPE);
	}
	
	public EmailWeightedSubjectBodyContentCluster(String id) {
		this.id = id;
		this.setClusterType(EmailCluster.WEIGHTED_SUBJECT_BODY_CLUSTER_TYPE);
	}

	public String getId() {
		return id;
	}

	public String getEmailId(int pos) {
		return emailIds.get(pos);
	}

	


	public void addEmail(String emailId, Email email) {
		
		subjectCentroid = VectorsMath.addArrays(subjectCentroid, email.getSubjectContextVector());
		bodyCentroid = VectorsMath.addArrays(bodyCentroid, email.getBodyContextVector());
		
		if(this.getEmailIds() == null){
			this.setEmailIds(new ArrayList<String>());
		}
		if(this.getSubjectBodyContentEmails() == null){
			this.setSubjectBodyContentEmails(new ArrayList<Email>());
		}
		
		//logger.info(" Adding email " + email.getMessageId() + "to content cluster : " + this.id);
		this.subjectBodyContentEmails.add(email);
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
		
		for(int i=0; i< subjectBodyContentEmails.size(); i++){
			Email email = subjectBodyContentEmails.get(i);
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
				subjectBodyContentEmails.remove(i);
				//logger.info("REMOVING email : " + email.getMessageId() + " from cluster : " + this.id);
				double[] emailSubjectContextVector = email.getSubjectContextVector();
				double[] emailBodyContextVector = email.getBodyContextVector();
				
				this.subjectCentroid = VectorsMath.substractArrays(this.subjectCentroid, emailSubjectContextVector);
				this.bodyCentroid = VectorsMath.substractArrays(this.bodyCentroid, emailBodyContextVector);
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
		return getSubjectBodyContentEmails().size();
	}

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public List<Email> getSubjectBodyContentEmails() {
		return subjectBodyContentEmails;
	}

	public void setSubjectBodyContentEmails(List<Email> emails) {
		this.subjectBodyContentEmails = emails;
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
	
	@Programmatic
	public double getSimilarity(double[] subjectVector, double[] bodyVector) {
		
		double subjectSimilarity = Similarity.cosineSimilarity(subjectCentroid, subjectVector);
		double bodySimilarity = Similarity.cosineSimilarity(bodyCentroid, bodyVector);
		//subject:body weight is 1:1
		double avgSimilarity = (subjectSimilarity + bodySimilarity)/2;
		return avgSimilarity;
	}
	
	/**
	 * gets the avg. subject centroid
	 * @return
	 */
	@Deprecated
	public double[] getAverageSubjectCentroid() {
		// TODO Auto-generated method stub
		double[] avgCentroid = subjectCentroid;
		if(this.getSubjectBodyContentEmails() != null && this.getSubjectBodyContentEmails().size() > 0){
			avgCentroid = VectorsMath.devideArray(avgCentroid, this.getSubjectBodyContentEmails().size());	
			logger.info("getting average centroid total : " + EmailUtils.getVectorTotal(avgCentroid)
					+ "and the cluster content emails size : " + this.getSubjectBodyContentEmails().size());			
		}
		return avgCentroid ;
	}
	
	/**
	 * gets the avg. body centroid
	 * @return
	 */
	@Deprecated
	public double[] getAverageBodyCentroid() {
		// TODO Auto-generated method stub
		double[] avgCentroid = bodyCentroid;
		if(this.getSubjectBodyContentEmails() != null && this.getSubjectBodyContentEmails().size() > 0){
			avgCentroid = VectorsMath.devideArray(avgCentroid, this.getSubjectBodyContentEmails().size());	
			logger.info("getting average centroid total : " + EmailUtils.getVectorTotal(avgCentroid)
					+ "and the cluster content emails size : " + this.getSubjectBodyContentEmails().size());			
		}
		return avgCentroid ;
	}
	
	public double getSumOfSquaresErrorForSubject(){
		double sumOfSquaredError = 0; 
		for(Email email : this.getSubjectBodyContentEmails()){
			double[] x = email.getSubjectContextVector();
			double[] centroid = this.getSubjectCentroid();
			double squaredDistance = VectorsMath.getSquaredDistance(x, centroid);
			sumOfSquaredError += squaredDistance;
		}
		return sumOfSquaredError;
	}

	public double getSumOfSquaresErrorForBody(){
		double sumOfSquaredError = 0; 
		for(Email email : this.getSubjectBodyContentEmails()){
			double[] x = email.getBodyContextVector();
			double[] centroid = this.getBodyCentroid();
			double squaredDistance = VectorsMath.getSquaredDistance(x, centroid);
			sumOfSquaredError += squaredDistance;
		}
		return sumOfSquaredError;
	}
	
	public double calculateClusterReputationScore(){
		
		int totalPositiveWeight = FLAGGED_WEIGHT + ANSWERERD_WEIGHT
				+ SEEN_WEIGHT;
		noOfMessagesAnswered = 0;
		noOfMessagesSeen = 0;
		noOfMessagesFlagged = 0;
		noOfMessagesDeleted = 0;
		
		for(Email email : this.getSubjectBodyContentEmails()){
			if(email.isAnswered()){
				noOfMessagesAnswered++;
			}
			if (email.isFlagged()) {
				noOfMessagesFlagged++;
			}
			if (email.isSeen()){
				noOfMessagesSeen++;
			}
			if(email.isDeleted()){
				noOfMessagesDeleted++;
			}
		}
		double reputationScore = (double)((noOfMessagesFlagged * FLAGGED_WEIGHT)
				+ (noOfMessagesAnswered * ANSWERERD_WEIGHT)
				+ (noOfMessagesSeen * SEEN_WEIGHT) - (noOfMessagesDeleted * DELETED_WEIGHT)) 
				/ (double)(this.getSubjectBodyContentEmails().size() * totalPositiveWeight);
		
		logger.info("Calculating reputation score of content cluster id : " + this.id 
				+ " : ("+ noOfMessagesFlagged + " * " + FLAGGED_WEIGHT + ") + ("
				+ noOfMessagesAnswered + " * " + ANSWERERD_WEIGHT + ") + ("
				+ noOfMessagesSeen + " * " + SEEN_WEIGHT + ") - ("
				+ noOfMessagesDeleted + " * " + DELETED_WEIGHT + ") / (" 
				+ this.getSubjectBodyContentEmails().size() +" * " + totalPositiveWeight + ")"
				+ " Score: " + reputationScore);
		this.setReputationScore(reputationScore);
		return this.reputationScore;
	}
	
	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public double[] getSubjectCentroid() {
		return subjectCentroid;
	}

	public void setSubjectCentroid(double[] subjectCentroid) {
		this.subjectCentroid = subjectCentroid;
	}
	
	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public double[] getBodyCentroid() {
		return bodyCentroid;
	}

	public void setBodyCentroid(double[] bodyCentroid) {
		this.bodyCentroid = bodyCentroid;
	}
}
