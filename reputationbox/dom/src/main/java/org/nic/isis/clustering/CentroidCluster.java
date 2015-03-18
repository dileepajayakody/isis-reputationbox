package org.nic.isis.clustering;

import java.util.BitSet;

import org.apache.isis.applib.annotation.Programmatic;
import org.nic.isis.ri.RandomIndexing;
import org.nic.isis.vector.VectorsMath;

import edu.ucla.sspace.common.Similarity;

public class CentroidCluster implements Cluster {

	/**
     * The centroid of this {@link EmailCluster}.  This is the only data
     * representation stored for the {@link EmailCluster}.
     */
    private double[] centroid;

    /**
     * The set of data point id's that assigned to this {@link EmailCluster}.
     * TODO: Consider replacing with a TIntSet after merging the
     * graphical-update branch which depends on Trove.
     */
    //private BitSet assignments;

    /**
     * Creates a new {@link CentroidCluster} that takes ownership of {@code
     * emptyVector} as the centroid for this {@link EmailCluster}. {@code
     * emptyVector} should have length equal to the length of vectors that will
     * be assigned to this {@link EmailCluster} and should be dense if a large number
     * of vectors, or any dense vectors, are expected to be assigned to this
     * {@link EmailCluster}.
     */
    public CentroidCluster(double[] emptyVector) {
        centroid = emptyVector;
        //assignments = new BitSet();
    }

    public CentroidCluster(){
    	//assignments = new BitSet();
    	centroid = new double[RandomIndexing.DEFAULT_VECTOR_LENGTH];
    }
    /**
     * {@inheritDoc}
     */
    @Programmatic
    public void addVector(double[] vector) {
        centroid = VectorsMath.addArrays(centroid, vector);
        centroid = VectorsMath.devideArray(centroid, 2);
    }

    /**
     * {@inheritDoc}
     */
/*  public BitSet dataPointIds() {
        return assignments;
    }
*/
    /**
     * {@inheritDoc}
     */
    public void merge(CentroidCluster other) {
        centroid = VectorsMath.addArrays(centroid, other.getCentroid());
        centroid = VectorsMath.devideArray(centroid, 2);
/*	centroid clusters don't keep other data points, only keeps centroid
 * 	
 * for (T otherDataPoint : other.dataPointValues())
            VectorMath.add(centroid, otherDataPoint);

        for (int i = other.dataPointIds().nextSetBit(0); i >= 0;
                 i = other.dataPointIds().nextSetBit(i+1))
            assignments.set(i);
*/            
    }

    /**
     * {@inheritDoc}
     */
/*  public int size() {
        return assignments.size();
    }
*/
	
	public double[] getCentroid() {
		return centroid;
	}

	@Override
	@Programmatic
	public double getSimilarity(double[] vector) {
		return Similarity.cosineSimilarity(centroid, vector);
	}

	@Override
	public double[] calculateAverageCentroid() {
		// TODO Auto-generated method stub
		return centroid;
	}

	
}
