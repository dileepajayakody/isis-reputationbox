/*
 * Copyright 2009 David Jurgens
 *
 * This file is part of the S-Space package and is covered under the terms and
 * conditions therein.
 *
 * The S-Space package is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as published
 * by the Free Software Foundation and distributed hereunder to you.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS" AND NO REPRESENTATIONS OR WARRANTIES,
 * EXPRESS OR IMPLIED ARE MADE.  BY WAY OF EXAMPLE, BUT NOT LIMITATION, WE MAKE
 * NO REPRESENTATIONS OR WARRANTIES OF MERCHANT- ABILITY OR FITNESS FOR ANY
 * PARTICULAR PURPOSE OR THAT THE USE OF THE LICENSED SOFTWARE OR DOCUMENTATION
 * WILL NOT INFRINGE ANY THIRD PARTY PATENTS, COPYRIGHTS, TRADEMARKS OR OTHER
 * RIGHTS.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.nic.isis.ri;

import edu.ucla.sspace.index.PermutationFunction;
import edu.ucla.sspace.index.RandomIndexVectorGenerator;
import edu.ucla.sspace.index.TernaryPermutationFunction;
import edu.ucla.sspace.text.IteratorFactory;
import edu.ucla.sspace.util.GeneratorMap;
import edu.ucla.sspace.vector.IntegerVector;
import edu.ucla.sspace.vector.TernaryVector;
import edu.ucla.sspace.vector.Vector;
import edu.ucla.sspace.vector.Vectors;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.isis.applib.annotation.Named;
import org.nic.isis.vector.VectorsMath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Modified RandomIndexing class based on the S-Space library class 
 * @author David Jurgens
 */
public class RandomIndexing implements SemanticSpace {

	public static final String RI_SSPACE_NAME = "random-indexing";

	/**
	 * The prefix for naming public properties.
	 */
	private static final String PROPERTY_PREFIX = "edu.ucla.sspace.ri.RandomIndexing";

	/**
	 * The property to specify the number of dimensions to be used by the index
	 * and semantic vectors.
	 */
	public static final String VECTOR_LENGTH_PROPERTY = PROPERTY_PREFIX
			+ ".vectorLength";

	/**
	 * The property to specify the number of words to view before and after each
	 * word in focus.
	 */
	public static final String WINDOW_SIZE_PROPERTY = PROPERTY_PREFIX
			+ ".windowSize";

	/**
	 * The property to specify whether the index vectors for co-occurrent words
	 * should be permuted based on their relative position.
	 */
	public static final String USE_PERMUTATIONS_PROPERTY = PROPERTY_PREFIX
			+ ".usePermutations";

	/**
	 * The property to specify the fully qualified named of a
	 * {@link PermutationFunction} if using permutations is enabled.
	 */
	public static final String PERMUTATION_FUNCTION_PROPERTY = PROPERTY_PREFIX
			+ ".permutationFunction";

	/**
	 * Specifies whether to use a sparse encoding for each word's semantics,
	 * which saves space but requires more computation.
	 */
	public static final String USE_SPARSE_SEMANTICS_PROPERTY = PROPERTY_PREFIX
			+ ".sparseSemantics";

	/**
	 * The default number of words to view before and after each word in focus.
	 */
	public static final int DEFAULT_WINDOW_SIZE = 2; // +2/-2

	/**
	 * The default number of dimensions to be used by the index and semantic
	 * vectors.
	 */
	public static final int DEFAULT_VECTOR_LENGTH = 4000;

	/**
	 * A private source of randomization used for creating the index vectors.
	 */
	// We use our own source rather than Math.random() to ensure reproduceable
	// behavior when a specific seed is set.
	//
	// NOTE: intentionally package-private to allow other RI-related classes to
	// based their randomness on a this class's seed.
	static final Random RANDOM = new Random();

	/**
	 * A mapping from each word to its associated index vector
	 */
	private Map<String, TernaryVector> wordToIndexVector;

	/**
	 * A mapping from each word to the int[] that the represents its semantics
	 */
	private Map<String, double[]> wordToMeaning;

	/**
	 * The number of dimensions for the semantic and index vectors.
	 */
	private final int vectorLength;

	/**
	 * The number of words to view before and after each focus word in a window.
	 */
	private final int windowSize;

	/**
	 * Whether the index vectors for co-occurrent words should be permuted based
	 * on their relative position.
	 */
	private final boolean usePermutations;

	/**
	 * If permutations are enabled, the permutation function to use on the index
	 * vectors.
	 */
	private final PermutationFunction<TernaryVector> permutationFunc;

	   /**
     * A flag for whether this instance should use {@code SparseIntegerVector}
     * instances for representic a word's semantics, which saves space but
     * requires more computation.
     */
	private final boolean useSparseSemantics;
	
	/**
	 * An optional set of words that restricts the set of semantic vectors that
	 * this instance will retain.
	 */
	private final Set<String> semanticFilter;

	private final static Logger logger = LoggerFactory
			.getLogger(RandomIndexing.class);
	 /**
     * Creates a new {@code RandomIndexing} instance using the provided
     * properites for configuration.
     */
	
	private RandomIndexVectorGenerator generator;

	/**
	 * Creates a new {@code RandomIndexing} instance from an existing
	 * wordToIndexVector and wordToMeaning map
	 */
	public RandomIndexing(Map<String, TernaryVector> indexVectors,
			Map<String, double[]> contextVectors) {
		vectorLength = DEFAULT_VECTOR_LENGTH;
		windowSize = DEFAULT_WINDOW_SIZE;
		usePermutations = false;
		permutationFunc = new TernaryPermutationFunction();
		wordToIndexVector = indexVectors;
		useSparseSemantics = true;
		
		generator = new RandomIndexVectorGenerator(DEFAULT_VECTOR_LENGTH);
		
		//the context vectors of words depends on the contextual usage within each doc
		//hence cannot have a global set of context vectors for a mailbox like index vectors
		//test this also with a global contextVectors in the mailbox and compare results
		wordToMeaning = contextVectors;
		//wordToMeaning = new HashMap<String, double[]>();
		
		semanticFilter = new HashSet<String>();
	}

	/**
	 * Removes all associations between word and semantics while still retaining
	 * the word to index vector mapping. This method can be used to re-use the
	 * same instance of a {@code RandomIndexing} on multiple corpora while
	 * keeping the same semantic space.
	 */

	public void removeAllSemantics() {
		wordToMeaning.clear();
	}

	/**
	 * Returns the current semantic vector for the provided word, or if the word
	 * is not currently in the semantic space, a vector is added for it and
	 * returned.
	 * 
	 * @param word
	 *            a word
	 * 
	 * @return the {@code SemanticVector} for the provide word.
	 */
	private synchronized double[] getSemanticVector(String word) {
		double[] v = wordToMeaning.get(word);
		if (v == null) {
			// lock on the word in case multiple threads attempt to add it at
			// once
			synchronized (this) {
				// recheck in case another thread added it while we were waiting
				// for the lock
				v = wordToMeaning.get(word);
				if (v == null) {
					v = new double[vectorLength];
					wordToMeaning.put(word, v);
				}
			}
		}
		return v;
	}

	public synchronized void putSemanticVector(String word, double[] vector){
		this.wordToMeaning.put(word, vector);
	}
	/**
	 * returns the context vector for the word
	 *//*
	public double[] getContextVector(String word) {
		double[] v = wordToMeaning.get(word);
		if (v == null) {
			return null;
		}
		return v;
	}
*/
	 /**
     * {@inheritDoc}
     */ 
    public double[] getVector(String word) {
        double[] v = wordToMeaning.get(word);
        if (v == null) {
            return null;
        }
        return v;
    }

   
	/**
	 * {@inheritDoc}
	 */
	public String getSpaceName() {
		return RI_SSPACE_NAME
				+ "-"
				+ vectorLength
				+ "v-"
				+ windowSize
				+ "w-"
				+ ((usePermutations) ? permutationFunc.toString()
						: "noPermutations");
	}

	/**
	 * {@inheritDoc}
	 */
	public int getVectorLength() {
		return vectorLength;
	}

	/**
	 * {@inheritDoc}
	 */

	public Set<String> getWords() {
		return Collections.unmodifiableSet(wordToMeaning.keySet());
	}

	/**
	 * 
	 * @return a mapping from the current set of tokens to the index vector used
	 *         to represent them
	 */
	public Map<String, TernaryVector> getWordToIndexVector() {
		return wordToIndexVector;
	}

	 /**
     * Assigns the token to {@link IntegerVector} mapping to be used by this
     * instance.  The contents of the map are copied, so any additions of new
     * index words by this instance will not be reflected in the parameter's
     * mapping.
     *
     * @param m a mapping from token to the {@code IntegerVector} that should be
     *        used represent it when calculating other word's semantics
     */
    public void setWordToIndexVector(Map<String,TernaryVector> m) {
        wordToIndexVector.clear();
        wordToIndexVector.putAll(m);
    }
	
	/**
	 * 
	 * @return a mapping from the current set of tokens to the context vector
	 *         calculated for them
	 */
	public Map<String, double[]> getWordToMeaningVector() {
		return wordToMeaning;
	}

	 /**
     * Removes all associations between word and semantics while still retaining
     * the word to index vector mapping.  This method can be used to re-use the
     * same instance of a {@code RandomIndexing} on multiple corpora while
     * keeping the same semantic space.
     */
    public void removeSemantics() {
        wordToMeaning.clear();
    }
    
    private synchronized TernaryVector generateIndexVector(String word){
		
			// Confirm that some other thread has not created an index
			// vector for this term.
			TernaryVector v = wordToIndexVector.get(word);
			if (v == null) {
				// Generate the index vector for this term and store it.
				v = generator.generate();
				wordToIndexVector.put((String) word, v);
			}
			return v;
	}
    
    /**
     * just like processDocument, but not using context location of the word.
     * 
     * @param word
     */
    public double[] processWords(Map<String,Integer> words){
    	double [] resultVector = new double[vectorLength];
    	for(String word : words.keySet()){
    		TernaryVector iv = wordToIndexVector.get(word);
    		//if the word doesn't have a indexVector create one and put in the IndexVectorMap
    		if(iv == null) {
    			iv = this.generateIndexVector(word);
    			wordToIndexVector.put(word, iv);   		
    		}
    		
    		int frequency = words.get(word);
    		resultVector = add(resultVector, iv, frequency);

    	}
    	return resultVector;
    }
	/**
	 * Updates the context vectors based on the words in the document.
	 * 
	 * @param document
	 *            {@inheritDoc}
	 */
	public void processDocument(BufferedReader document) throws IOException {
		Queue<String> prevWords = new ArrayDeque<String>(windowSize);
		Queue<String> nextWords = new ArrayDeque<String>(windowSize);

		Iterator<String> documentTokens = IteratorFactory
				.tokenizeOrdered(document);
		
		String focusWord = null;

		// prefetch the first windowSize words
		for (int i = 0; i < windowSize && documentTokens.hasNext(); ++i)
			nextWords.offer(documentTokens.next());

		while (!nextWords.isEmpty()) {
			focusWord = nextWords.remove();

			// shift over the window to the next word
			if (documentTokens.hasNext()) {
				String windowEdge = documentTokens.next();
				nextWords.offer(windowEdge);
			}

			// If we are filtering the semantic vectors, check whether this word
			// should have its semantics calculated. In addition, if there is a
			// filter and it would have excluded the word, do not keep its
			// semantics around
			boolean calculateSemantics = semanticFilter.isEmpty()
					|| semanticFilter.contains(focusWord)
					&& !focusWord.equals(IteratorFactory.EMPTY_TOKEN);

			if (calculateSemantics) {
				double[] focusMeaning = getSemanticVector(focusWord);

				// Sum up the index vector for all the surrounding words. If
				// permutations are enabled, permute the index vector based on
				// its relative position to the focus word.
				int permutations = -(prevWords.size());
				for (String word : prevWords) {
					// Skip the addition of any words that are excluded from the
					// filter set. Note that by doing the exclusion here, we
					// ensure that the token stream maintains its existing
					// ordering, which is necessary when permutations are taken
					// into account.
					if (word.equals(IteratorFactory.EMPTY_TOKEN)) {
						++permutations;
						continue;
					}

					TernaryVector iv = wordToIndexVector.get(word);
					//check if iv is null and generate vector
					if(iv == null){
						//logger.error("No index vector for previous word : " + word + " putting new one");
						iv = this.generateIndexVector(word);
						wordToIndexVector.put(word, iv);
					}
					
					 
					if (usePermutations) {
						iv = permutationFunc.permute(iv, permutations);
						++permutations;
					}

					focusMeaning = add(focusMeaning, iv);
				}

				// Repeat for the words in the forward window.
				permutations = 1;
				for (String word : nextWords) {
					// Skip the addition of any words that are excluded from the
					// filter set. Note that by doing the exclusion here, we
					// ensure that the token stream maintains its existing
					// ordering, which is necessary when permutations are taken
					// into account.
					if (word.equals(IteratorFactory.EMPTY_TOKEN)) {
						++permutations;
						continue;
					}

					TernaryVector iv = wordToIndexVector.get(word);
					//if the word doesn't have a indexVector create one and put in the IndexVectorMap
					if(iv == null) {
						//logger.error("No index vector for next word : " + word + " putting new one");
						iv = this.generateIndexVector(word);
						wordToIndexVector.put(word, iv);
					}
					
					//logger.info("PROCESSING WORD in processDocument: " + word);
					//logger.info("the iv" + iv.toString());
					
					
					if (usePermutations) {
						iv = permutationFunc.permute(iv, permutations);
						++permutations;
					}

					//logger.info("After permutations: " + iv.toString());
					
					focusMeaning = add(focusMeaning, iv);
				}
				//putSemanticVector(focusWord, focusMeaning);
			}

			// Last put this focus word in the prev words and shift off the
			// front of the previous word window if it now contains more words
			// than the maximum window size
			prevWords.offer(focusWord);
			if (prevWords.size() > windowSize) {
				prevWords.remove();
			}
			
			
		}

		document.close();
	}
	
	
	
	/**
	 * {@inheritDoc} Note that all words will still have an index vector
	 * assigned to them, which is necessary to properly compute the semantics.
	 * 
	 * @param semanticsToRetain
	 *            the set of words for which semantics should be computed.
	 */
	public void setSemanticFilter(Set<String> semanticsToRetain) {
		semanticFilter.clear();
		semanticFilter.addAll(semanticsToRetain);
	}

	/**
	 * Atomically adds the values of the index vector to the semantic vector.
	 * This is a special case addition operation that only iterates over the
	 * non-zero values of the index vector.
	 */
	public static double[] add(double[] semantics, TernaryVector index) {
		// Lock on the semantic vector to avoid a race condition with another
		// thread updating its semantics. Use the vector to avoid a class-level
		// lock, which would limit the concurrency.
		synchronized (semantics) {
			if(index.positiveDimensions() != null){
				for (int p : index.positiveDimensions())
					// semantics.add(p, 1);
					semantics[p] = semantics[p] + 1;
			} else {
				logger.error("no positive dimentions found for index: " + index.toString());
			}
			if(index.negativeDimensions() != null){
				for (int n : index.negativeDimensions())
					// semantics.add(n, -1);
					semantics[n] = semantics[n] - 1;
			} else {
				logger.error("no negative dimentions found for index: " + index.toString());
			}

		}
		return semantics;
	}

	/**
	 * Atomically adds the values of the index vector to the semantic vector with given frequency.
	 * This is a special case addition operation that only iterates over the
	 * non-zero values of the index vector.
	 */
	public static double[] add(double[] semantics, TernaryVector index, int frequency) {
		// Lock on the semantic vector to avoid a race condition with another
		// thread updating its semantics. Use the vector to avoid a class-level
		// lock, which would limit the concurrency.
		synchronized (semantics) {
			if(index.positiveDimensions() != null){
				for (int p : index.positiveDimensions())
					// semantics.add(p, 1);
					semantics[p] = semantics[p] + (1*frequency);
			} else {
				logger.error("no positive dimentions found for index: " + index.toString());
			}
			if(index.negativeDimensions() != null){
				for (int n : index.negativeDimensions())
					// semantics.add(n, -1);
					semantics[n] = semantics[n] - (1*frequency);
			} else {
				logger.error("no negative dimentions found for index: " + index.toString());
			}

		}
		return semantics;
	}
	
	
	 /**
     * Returns an instance of the the provided class name, that implements
     * {@code PermutationFunction}.
     *
     * @param className the fully qualified name of a class
     */ 
    @SuppressWarnings("unchecked")
    private static PermutationFunction<TernaryVector> loadPermutationFunction(
            String className) {
        try {
            Class clazz = Class.forName(className);
            return (PermutationFunction<TernaryVector>)(clazz.newInstance());
        } catch (Exception e) {
            // catch all of the exception and rethrow them as an error
            throw new Error(e);
        }
    }

	@Override
	public void processSpace(Properties properties) {
		// TODO Auto-generated method stub
		
	}
}
