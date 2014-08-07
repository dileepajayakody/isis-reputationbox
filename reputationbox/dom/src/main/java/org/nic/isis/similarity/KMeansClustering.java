package org.nic.isis.similarity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeSet;

import org.apache.commons.collections4.CollectionUtils;
import org.nic.isis.reputation.dom.Email;
import org.nic.isis.reputation.services.EmailAnalysisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KMeansClustering {
	private final static Logger logger = LoggerFactory
			.getLogger(KMeansClustering.class);
	  
	  private String[] initialClusterAssignments = null;
	  
	  public void setInitialClusterAssignments(String[] documentNames) {
	    this.initialClusterAssignments = documentNames;
	  }
	  
	  public List<Cluster> cluster(List<Email> emailCollection) {
	    int numDocs = emailCollection.size();
	    int numClusters = 0;
	    if (initialClusterAssignments == null) {
	      // compute initial cluster assignments randomly
	      Random randomGen = new Random();
	      numClusters = (int) Math.floor(Math.sqrt(numDocs));
	      initialClusterAssignments = new String[numClusters];
	      for (int i = 0; i < numClusters; i++) {
	        int docIndex = randomGen.nextInt(numDocs);
	        initialClusterAssignments[i] = emailCollection.get(docIndex).getMessageId();
	      }
	    } else {
	      numClusters = initialClusterAssignments.length;
	    }

	    // build initial clusters
	    List<Cluster> clusters = new ArrayList<Cluster>();
	    for (int i = 0; i < numClusters; i++) {
	      Cluster cluster = new Cluster("C" + i);
	      cluster.addDocument(initialClusterAssignments[i], 
	        emailCollection.get(i));
	      clusters.add(cluster);
	    }
	  	
	    List<Cluster> prevClusters = new ArrayList<Cluster>();

	    // Repeat until termination conditions are satisfied
	    while (true) {
	      // For every cluster i, recompute the centroid based on the
	      // current member documents.
	      List<double[]> centroids = new ArrayList<double[]>();
	      for (int i = 0; i < numClusters; i++) {
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
	        email.getDocumentContextVector();
	        for (int j = 0; j < numClusters; j++) {
	          double similarity = clusters.get(j).getSimilarity(email.getDocumentContextVector());
	          if (similarity > maxSimilarity) {
	            bestCluster = j;
	            maxSimilarity = similarity;
	          }
	        }
	        for (Cluster cluster : clusters) {
	          if (cluster.getEmail(messageId) != null) {
	            cluster.removeDocument(messageId);
	          }
	        }
	        clusters.get(bestCluster).addDocument(messageId, email);
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
}
