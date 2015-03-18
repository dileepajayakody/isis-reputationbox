package org.nic.isis.clustering;

public interface Cluster {

	public final static String TEXT_CLUSTER_TYPE = "TOPIC";
	public final static String RECIPIENT_CLUSTER_TYPE = "PEOPLE";
	
	double[] getCentroid();
	
	double[] calculateAverageCentroid();
	
	void addVector(double[] vector);
	
	double getSimilarity(double[] vector);
	
}
