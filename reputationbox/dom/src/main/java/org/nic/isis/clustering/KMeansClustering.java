package org.nic.isis.clustering;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;
import org.nic.isis.reputation.dom.Email;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KMeansClustering {
	
	private final static Logger logger = LoggerFactory
			.getLogger(KMeansClustering.class);

	private String[] initialClusterAssignments = null;
	//need scientific reasoning for these values
	private static double contentClusterThreshold = 0.6;
	private static double peopleClusterThreshold = 0.4;
	//private static int maxClusters = 100;

	public void setInitialClusterAssignments(String[] documentNames) {
		this.initialClusterAssignments = documentNames;
	}

	public List<EmailContentCluster> clusterBasedOnContent(List<Email> emailCollection) {
		int numDocs = emailCollection.size();
		int numClusters = 0;
		
		List<EmailContentCluster> clusters = new ArrayList<EmailContentCluster>();
		
		if (initialClusterAssignments == null) {
			// compute initial cluster assignments randomly
			Random randomGen = new Random();
			//the number of clusters is taken as the square-root value of total no.of docs
			numClusters = (int) Math.floor(Math.sqrt(numDocs));
			initialClusterAssignments = new String[numClusters];
			
			for (int i = 0; i < numClusters; i++) {
				int docIndex = randomGen.nextInt(numDocs);
				Email randomEmail = emailCollection.get(docIndex);
				initialClusterAssignments[i] = randomEmail.getMessageId();
				
				//building initial clusters
				EmailContentCluster cluster = new EmailContentCluster("c" + String.valueOf(i));
				cluster.addEmail(initialClusterAssignments[i], randomEmail);
				clusters.add(cluster);
			}
		} else {
			numClusters = initialClusterAssignments.length;
		}

		// build initial clusters
		/*for (int i = 0; i < numClusters; i++) {
			EmailContentCluster cluster = new EmailContentCluster(String.valueOf(i));
			cluster.addEmail(initialClusterAssignments[i],
					emailCollection.get(i));
			clusters.add(cluster);
		}*/

		List<EmailContentCluster> prevClusters = new ArrayList<EmailContentCluster>();

		// Repeat until termination conditions are satisfied
		while (true) {
			// For every cluster i, recompute the centroid based on the
			// current member documents.
			List<double[]> centroids = new ArrayList<double[]>();
			for (int i = 0; i < numClusters; i++) {
				//double[] centroid = clusters.get(i).calculateAverageCentroid();
				//testing get centroid
				double[] centroid = clusters.get(i).getCentroid();
				centroids.add(i, centroid);
			}
			// For every document d, find the cluster i whose centroid is
			// most similar, assign d to cluster i. (If a document is
			// equally similar from all centroids, then just dump it into
			// cluster 0).
			for (int i = 0; i < numDocs; i++) {
				int bestCluster = 0;
				double maxSimilarity = Double.MIN_VALUE;
				Email email = emailCollection.get(i);
				String messageId = email.getMessageId();
				for (int j = 0; j < numClusters; j++) {
					double similarity = 0.0D;
					similarity = clusters.get(j).getSimilarity(
								email.getTextContextVector());
					
					if (similarity > maxSimilarity) {
						bestCluster = j;
						maxSimilarity = similarity;
					}
				}
				for (EmailContentCluster cluster : clusters) {
					//if (cluster.getEmail(messageId) != null) {
						cluster.removeEmail(messageId);
					//}
				}
				clusters.get(bestCluster).addEmail(messageId, email);
			}
			// Check for termination -- minimal or no change to the assignment
			// of documents to clusters.
			if (CollectionUtils.isEqualCollection(clusters, prevClusters)) {
				break;
			}
			prevClusters.clear();
			prevClusters.addAll(clusters);
		}
		return clusters;
	}
	
	
	
	public List<EmailRecipientCluster> clusterBasedOnRecipients(List<Email> emailCollection) {
		int numDocs = emailCollection.size();
		int numClusters = 0;
		// build initial clusters
		List<EmailRecipientCluster> clusters = new ArrayList<EmailRecipientCluster>();
		
		if (initialClusterAssignments == null) {
			// compute initial cluster assignments randomly
			Random randomGen = new Random();
			//the number of clusters is taken as the square-root value of total no.of docs
			numClusters = (int) Math.floor(Math.sqrt(numDocs));
			initialClusterAssignments = new String[numClusters];
			for (int i = 0; i < numClusters; i++) {
				int docIndex = randomGen.nextInt(numDocs);
				Email randomEmail = emailCollection.get(docIndex);
				initialClusterAssignments[i] = emailCollection.get(docIndex)
						.getMessageId();
				
				//building initial clusters
				EmailRecipientCluster cluster = new EmailRecipientCluster("p" + String.valueOf(i));
				cluster.addEmail(initialClusterAssignments[i], randomEmail);
				clusters.add(cluster);
			}
		} else {
			numClusters = initialClusterAssignments.length;
		}

		
		/*for (int i = 0; i < numClusters; i++) {
			EmailRecipientCluster cluster = new EmailRecipientCluster(String.valueOf(i));
			cluster.addEmail(initialClusterAssignments[i],
					emailCollection.get(i));
			clusters.add(cluster);
		}*/

		List<EmailRecipientCluster> prevClusters = new ArrayList<EmailRecipientCluster>();

		// Repeat until termination conditions are satisfied
		while (true) {
			// For every cluster i, recompute the centroid based on the
			// current member documents.
			List<double[]> centroids = new ArrayList<double[]>();
			for (int i = 0; i < numClusters; i++) {
				//double[] centroid = clusters.get(i).calculateAverageCentroid();
				double[] centroid = clusters.get(i).getCentroid();
				centroids.add(i, centroid);
			}
			// For every document d, find the cluster i whose centroid is
			// most similar, assign d to cluster i. (If a document is
			// equally similar from all centroids, then just dump it into
			// cluster 0).
			for (int i = 0; i < numDocs; i++) {
				int bestCluster = 0;
				double maxSimilarity = Double.MIN_VALUE;
				Email email = emailCollection.get(i);
				String messageId = email.getMessageId();
				for (int j = 0; j < numClusters; j++) {
					double similarity = 0.0D;
					similarity = clusters.get(j).getSimilarity(
								email.getRecipientContextVector());

					if (similarity > maxSimilarity) {
						bestCluster = j;
						maxSimilarity = similarity;
					}
				}
				for (EmailRecipientCluster cluster : clusters) {
					//if (cluster.getEmail(messageId) != null) {
						cluster.removeEmail(messageId);
					//}
				}
				clusters.get(bestCluster).addEmail(messageId, email);
			}
			// Check for termination -- minimal or no change to the assignment
			// of documents to clusters.
			if (CollectionUtils.isEqualCollection(clusters, prevClusters)) {
				break;
			}
			prevClusters.clear();
			prevClusters.addAll(clusters);
		}
		return clusters;
	}
	
	
	/**
	 * cluster emails based on all Features
	 * @return
	 */
	public List<EmailAllFeatureCluster> clusterBasedOnAllFeatures(List<Email> emailCollection){
		int numDocs = emailCollection.size();
		int numClusters = 0;
		// build initial clusters
		List<EmailAllFeatureCluster> clusters = new ArrayList<EmailAllFeatureCluster>();
		
		if (initialClusterAssignments == null) {
			// compute initial cluster assignments randomly
			Random randomGen = new Random();
			//the number of clusters is taken as the square-root value of total no.of docs
			numClusters = (int) Math.floor(Math.sqrt(numDocs));
			initialClusterAssignments = new String[numClusters];
			for (int i = 0; i < numClusters; i++) {
				int docIndex = randomGen.nextInt(numDocs);
				Email randomEmail = emailCollection.get(docIndex);
				initialClusterAssignments[i] = emailCollection.get(docIndex)
						.getMessageId();
				
				//building initial clusters
				EmailAllFeatureCluster cluster = new EmailAllFeatureCluster("p" + String.valueOf(i));
				cluster.addEmail(initialClusterAssignments[i], randomEmail);
				clusters.add(cluster);
			}
		} else {
			numClusters = initialClusterAssignments.length;
		}

		
		/*for (int i = 0; i < numClusters; i++) {
			EmailRecipientCluster cluster = new EmailRecipientCluster(String.valueOf(i));
			cluster.addEmail(initialClusterAssignments[i],
					emailCollection.get(i));
			clusters.add(cluster);
		}*/

		List<EmailAllFeatureCluster> prevClusters = new ArrayList<EmailAllFeatureCluster>();

		// Repeat until termination conditions are satisfied
		while (true) {
			// For every cluster i, recompute the centroid based on the
			// current member documents.
			List<double[]> centroids = new ArrayList<double[]>();
			for (int i = 0; i < numClusters; i++) {
				//double[] centroid = clusters.get(i).calculateAverageCentroid();
				double[] centroid = clusters.get(i).getCentroid();
				centroids.add(i, centroid);
			}
			// For every document d, find the cluster i whose centroid is
			// most similar, assign d to cluster i. (If a document is
			// equally similar from all centroids, then just dump it into
			// cluster 0).
			for (int i = 0; i < numDocs; i++) {
				int bestCluster = 0;
				double maxSimilarity = Double.MIN_VALUE;
				Email email = emailCollection.get(i);
				String messageId = email.getMessageId();
				for (int j = 0; j < numClusters; j++) {
					double similarity = 0.0D;
					similarity = clusters.get(j).getSimilarity(
								email.getAllFeatureVector());

					if (similarity > maxSimilarity) {
						bestCluster = j;
						maxSimilarity = similarity;
					}
				}
				for (EmailAllFeatureCluster cluster : clusters) {
					//if (cluster.getEmail(messageId) != null) {
						cluster.removeEmail(messageId);
					//}
				}
				clusters.get(bestCluster).addEmail(messageId, email);
			}
			// Check for termination -- minimal or no change to the assignment
			// of documents to clusters.
			if (CollectionUtils.isEqualCollection(clusters, prevClusters)) {
				break;
			}
			prevClusters.clear();
			prevClusters.addAll(clusters);
		}
		return clusters;
	}
	
	public static List<EmailContentCluster> classifyNewEmailByContent(Email newEmail, List<EmailContentCluster> contentClusters){
		logger.info("Classifying email by content");
		int bestCluster = 0;
		double maxSimilarity = Double.MIN_VALUE;
		String messageId = newEmail.getMessageId();
		EmailContentCluster bestMatchCluster = null;
		
		for (int j = 0; j < contentClusters.size(); j++) {
			double similarity = 0.0D;
			similarity = contentClusters.get(j).getSimilarity(
						newEmail.getTextContextVector());
			/*logger.info("Similarity of email : " + newEmail.getMessageId() + " with cluster : " + contentClusters.get(j).getId() + " similarity :"
					+ similarity);*/
			if (similarity > maxSimilarity) {
				bestCluster = j;
				maxSimilarity = similarity;
			}
		}
		//added recenty
		bestMatchCluster = contentClusters
				.get(bestCluster);
		if (maxSimilarity >= contentClusterThreshold) { 
			//	|| contentClusters.size() >= maxClusters) {
				logger.info(" The best cluster : " + bestMatchCluster.getId());
				if (bestMatchCluster != null) {
					logger.info("The best content cluster for email found... clusterId : "
							+ bestMatchCluster.getId() + " similarity : " + maxSimilarity);		
					bestMatchCluster.addEmail(messageId, newEmail);
					double clusterRepuScore = bestMatchCluster
							.calculateClusterReputationScore();
					newEmail.setTextClusterId(bestMatchCluster.getId());
					
					newEmail.setContentReputationScore(clusterRepuScore);
					
					//setting the reference of the clusters back to the list..
					contentClusters.set(bestCluster, bestMatchCluster);
				}	
            } else {
                // lock to ensure that the number of clusters doesn't change
                // while we add this one
                synchronized(contentClusters) {
                    // Perform an additional check to see whether the number of
                    // contentClusters changed while we waiting on the lock
//                    if (contentClusters.size() < maxClusters) {
                        bestMatchCluster = new EmailContentCluster("c" + String.valueOf(contentClusters.size()));
                        logger.info("Adding the email to the newly added content cluster  " + bestMatchCluster.getId());
    					bestMatchCluster.addEmail(messageId, newEmail);
    					double clusterRepuScore = bestMatchCluster
    							.calculateClusterReputationScore();
    					newEmail.setTextClusterId(bestMatchCluster.getId());
    					newEmail.setContentReputationScore(clusterRepuScore);
                        contentClusters.add(bestMatchCluster);
//                    } else {
//                    	logger.info(" the max content cluster number has exceed and the email has not gained threshold similarity to any existing clusters.."
//                    			+ " hence ignoring email from clustering ");
//                    }    
                }
            }
		return contentClusters;
	}
	
	public static List<EmailRecipientCluster> classifyNewEmailByPeople(Email newEmail, List<EmailRecipientCluster> recipientClusters){
		logger.info("Classifying email by recipients");
		int bestCluster = 0;
		double maxSimilarity = Double.MIN_VALUE;
		String messageId = newEmail.getMessageId();
		EmailRecipientCluster bestMatchCluster = null;
		
		for (int j = 0; j < recipientClusters.size(); j++) {
			double similarity = 0.0D;
			similarity = recipientClusters.get(j).getSimilarity(
						newEmail.getRecipientContextVector());
//			logger.info("Similarity of email : " + newEmail.getMessageId() + " with cluster : " + recipientClusters.get(j).getId() + " similarity :"
//					+ similarity);
			if (similarity > maxSimilarity) {
				bestCluster = j;
				maxSimilarity = similarity;
			}
		}
		
		bestMatchCluster = recipientClusters
				.get(bestCluster);
		//added recenty
		if (maxSimilarity >= peopleClusterThreshold ) { 
               // || recipientClusters.size() >= maxClusters) {
				
				logger.info("The best cluster : " + bestMatchCluster.getId());
				if (bestMatchCluster != null) {
					logger.info("The best recipient cluster for email found... clusterId : "
							+ bestMatchCluster.getId() +  " similarity : " + maxSimilarity);
					bestMatchCluster.addEmail(messageId, newEmail);
					double clusterRepuScore = bestMatchCluster
							.calculateClusterReputationScore();
					
					newEmail.setPeopleClusterId(bestMatchCluster.getId());
					newEmail.setRecipientReputationScore(clusterRepuScore);
					
					recipientClusters.set(bestCluster, bestMatchCluster);
				}	
            } else {
                // lock to ensure that the number of clusters doesn't change
                // while we add this one
                synchronized(recipientClusters) {
                    // Perform an additional check to see whether the number of
                    // contentClusters changed while we waiting on the lock
                    
                	//if (recipientClusters.size() < maxClusters) {
                        bestMatchCluster = new EmailRecipientCluster("p" + String.valueOf(recipientClusters.size()));
                        bestMatchCluster.addEmail(messageId, newEmail);
                    	double clusterRepuScore = bestMatchCluster
    							.calculateClusterReputationScore();
                    	logger.info("Adding the email to the newly added recipient cluster  " + bestMatchCluster.getId());
    					newEmail.setPeopleClusterId(bestMatchCluster.getId());
    					newEmail.setRecipientReputationScore(clusterRepuScore);
                        recipientClusters.add(bestMatchCluster);
//                    } 
//                    else {
//                    	logger.info("The max recipient cluster number has exceed and the email has not gained threshold similarity to any existing clusters.."
//                    			+ " hence ignoring email from clustering ");
//                    }
                        
                }
            }
		return recipientClusters;
	}
}
