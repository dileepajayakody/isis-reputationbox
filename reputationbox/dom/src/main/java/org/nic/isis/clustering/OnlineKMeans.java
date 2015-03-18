package org.nic.isis.clustering;

import edu.ucla.sspace.vector.DoubleVector;
import edu.ucla.sspace.vector.Vectors;
import edu.ucla.sspace.vector.VectorMath;
import edu.ucla.sspace.clustering.OnlineClustering;
import edu.ucla.sspace.util.Generator;
import edu.ucla.sspace.util.Properties;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * A {@link Generator} class for generating a new {@code OnlineKMeansClustering}
 * instance. This class supports the following properties:
 *
 * <dl style="margin-left: 1em">
 *
 * <dt> <i>Property:</i> <code><b>{@value #WEIGHTING_PROPERTY}
 *      </b></code> <br>
 *      <i>Default:</i> {@value #DEFAULT_WEIGHT}
 *
 * <dd style="padding-top: .5em">This variable sets the weight given to the mean
 * vector in a rolling average of vectors.</p>
 *
 * <dt> <i>Property:</i> <code><b>{@value #MERGE_THRESHOLD_PROPERTY }
 *      </b></code> <br>
 *      <i>Default:</i> {@value #DEFAULT_MERGE_THRESHOLD}
 *
 * <dd style="padding-top: .5em">This variable sets the threshold for merging
 * two clusters. </p>
 *
 * <dt> <i>Property:</i> <code><b>{@value #NUM_CLUSTERS}
 *      </b></code> <br>
 *      <i>Default:</i> {@value #DEFAULT_MAX_CLUSTERS}
 *
 * <dd style="padding-top: .5em">This variable sets the maximum number of
 * clusters used.</p>
 *
 * </dl>
 *
 * @author Keith Stevens
 */
public class OnlineKMeans<T> {

    /**
     * A property prefix.
     */
    private static final String PROPERTY_PREFIX =
        "edu.ucla.sspace.cluster.OnlineKMeans";

    /**
     * The property for setting the threshold for merging two clusters.
     */
    public static final String MERGE_THRESHOLD_PROPERTY =
        PROPERTY_PREFIX + ".merge";

    /**
     * The default merge threshold.
     */
    public static final double DEFAULT_MERGE_THRESHOLD = .35;

    /**
     * The default number of clusters.
     */
    public static final int DEFAULT_MAX_CLUSTERS = 15;

    /**
     * The threshold for clustering
     */
    private final double clusterThreshold;

    /** 
     * The maximum number of clusters permitted.
     */
    private final int maxNumClusters;

    /**
     * Creates a new generator using the system properties.
     */
    public OnlineKMeans() {
        this(new Properties());
    }

    /**
     * Creates a new generator using the given properties.
     */
    public OnlineKMeans(Properties props) {
        clusterThreshold = props.getProperty(
                    MERGE_THRESHOLD_PROPERTY, DEFAULT_MERGE_THRESHOLD);
        maxNumClusters = props.getProperty(
            OnlineClustering.NUM_CLUSTERS_PROPERTY, DEFAULT_MAX_CLUSTERS);
    }

  
    public String toString() {
        return "OnLineKMeans_" + maxNumClusters + "c_";
    }

    /**
     * A simple online implementation of K-Means clustering for {@code Vector}s,
     * with the option to perform agglomerative clustering once all elements
     * have been clustered.
     *
     * @author Keith Stevens
     */
    public class OnlineKMeansClustering<T > {

        /**
         * The threshold for clustering
         */
        private final double clusterThreshold;

        /** 
         * The maximum number of clusters permitted.
         */
        private final int maxNumClusters;

        /**
         * The set of clusters.
         */
        private final List<Cluster> elements;

        /**
         * A counter for generating item identifiers.
         */
        private final AtomicInteger idCounter;

        /**
         * Creates a new instance of online KMeans clustering.
         */
        public OnlineKMeansClustering(double mergeThreshold,
                                      int maxNumClusters) {
            elements = new CopyOnWriteArrayList<Cluster>();
            idCounter = new AtomicInteger(0);

            this.clusterThreshold = mergeThreshold;
            this.maxNumClusters = maxNumClusters;
        }

        /**
         * {@inheritDoc}
         */
        public int addVector(double[] value) {
            int id = idCounter.getAndAdd(1);

            Iterator<Cluster> elementIter = elements.iterator();

            // Find the centriod with the best similarity.
            Cluster bestMatch = null;
            int bestIndex = elements.size();
            double bestScore = -1;
            double similarity = -1;
            int i = 0;
            while (elementIter.hasNext()) {
                Cluster cluster = elementIter.next();
                similarity = cluster.getSimilarity(value);
                if (similarity >= bestScore) {
                    bestScore = similarity;
                    bestMatch = cluster;
                    bestIndex = i;
                }
                ++i;
            }

            // Add the current term vector if the similarity is high enough, or
            // set it as a new centroid.        
            if (bestScore >= clusterThreshold || 
                elements.size() >= maxNumClusters) {
                bestMatch.addVector(value);
            } else {
                // lock to ensure that the number of clusters doesn't change
                // while we add this one
                synchronized(elements) {
                    // Perform an additional check to see whether the number of
                    // elements changed while we waiting on the lock
                    if (elements.size() < maxNumClusters) {
                        bestMatch = new CentroidCluster(value);
                        elements.add(bestMatch);
                    } 
                    if (bestMatch != null)
                        bestMatch.addVector(value);
                }
            }
            return id;
        }

        /**
         * {@inheritDoc}
         */
        public Cluster getCluster(int clusterIndex) {
            if (elements.size() <= clusterIndex)
                return null;
            return elements.get(clusterIndex);
        }

        /**
         * {@inheritDoc}
         */
        public List<Cluster> getClusters() {
            return elements;
        }

        /**
         * {@inheritDoc}
         */
        public synchronized int size() {
            return elements.size();
        }

        /**
         * Returns a string describing this {@code ClusterMap}.
         */
        public String toString() {
            return "OnlineKMeansClustering-maxNumClusters" + maxNumClusters +
                   "-threshold" + clusterThreshold;
        }

		
    }
}
