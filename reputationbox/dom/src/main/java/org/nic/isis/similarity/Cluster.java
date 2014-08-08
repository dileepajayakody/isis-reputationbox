package org.nic.isis.similarity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.nic.isis.reputation.dom.Email;
import org.nic.isis.ri.RandomIndexing;

public class Cluster {

	private String id;
	private Map<String, Email> emails = new HashMap<String, Email>();
	private List<String> emailIds = new ArrayList<String>();
	private double[] centroid = null;
	private String clusterType;

	public String getClusterType() {
		return clusterType;
	}

	public void setClusterType(String clusterType) {
		this.clusterType = clusterType;
	}

	public Cluster(String id, String clusterType) {
		this.id = id;
		this.clusterType = clusterType;
	}

	public String getId() {
		return id;
	}

	public Set<String> getEmailIds() {
		return emails.keySet();
	}

	public String getEmailId(int pos) {
		return emailIds.get(pos);
	}

	public Email getEmail(String messageId) {
		return emails.get(messageId);
	}

	public Email getEmail(int pos) {
		return emails.get(emailIds.get(pos));
	}

	public void addDocument(String docName, Email email) {
		emails.put(docName, email);
		emailIds.add(docName);
	}

	public void removeDocument(String docName) {
		emails.remove(docName);
		emailIds.remove(docName);
	}

	public int size() {
		return emails.size();
	}

	public boolean contains(String docName) {
		return emails.containsKey(docName);
	}

	/**
	 * Returns a document consisting of the average of the vectors of the
	 * documents in the cluster.
	 * 
	 * @return the centroid of the cluster
	 */
	public double[] getCentroid() {
		if (emails.size() == 0) {
			return null;
		}
		double[] d = null;
		if (KMeansCluster.TEXT_CLUSTER_TYPE.equals(this.clusterType)) {
			d = emails.get(emailIds.get(0)).getTextContextVector();

		} else if (KMeansCluster.RECIPIENT_CLUSTER_TYPE.equals(this.clusterType)) {
			d = emails.get(emailIds.get(0)).getRecipientContextVector();
		}

		double[] tempVector = new double[d.length];
		for (String docName : emails.keySet()) {
			double[] contextVector = null;
			if (KMeansCluster.TEXT_CLUSTER_TYPE.equals(this.clusterType)) {
				contextVector = emails.get(docName).getTextContextVector();
			} else if (KMeansCluster.RECIPIENT_CLUSTER_TYPE
					.equals(this.clusterType)) {
				contextVector = emails.get(docName).getRecipientContextVector();
			}
			tempVector = RandomIndexing.addArrays(tempVector, contextVector);
		}
		centroid = new double[d.length];
		centroid = RandomIndexing.devideArray(tempVector, emails.size());
		return centroid;
	}

	/**
	 * Returns the cosine similarity between the centroid of this cluster and
	 * the new document.
	 * 
	 * @param docVector
	 *            the document to be compared for similarity.
	 * @return the similarity of the centroid of the cluster to the document
	 */
	public double getSimilarity(double[] docVector) {
		if (centroid != null) {
			double similarity = CosineSimilarity.calculateCosineSimilarity(
					centroid, docVector);
			return similarity;
		}
		return 0.0D;
	}

}