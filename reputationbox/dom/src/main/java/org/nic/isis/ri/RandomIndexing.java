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
import edu.ucla.sspace.index.TernaryPermutationFunction;
import edu.ucla.sspace.text.IteratorFactory;
import edu.ucla.sspace.vector.TernaryVector;


import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Set;

/**
 * Modified RandomIndexing class based on the S-Space library class 
 * @author David Jurgens
 */
public class RandomIndexing {

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
	private Map<String, TernaryVector> wordToIndexVectors;

	/**
	 * A mapping from each word to the int[] that the represents its semantics
	 */
	private Map<String, double[]> wordsToMeaningMap;

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
	 * An optional set of words that restricts the set of semantic vectors that
	 * this instance will retain.
	 */
	private final Set<String> semanticFilter;

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
		wordToIndexVectors = indexVectors;
		wordsToMeaningMap = contextVectors;
		semanticFilter = new HashSet<String>();
	}

	/**
	 * Removes all associations between word and semantics while still retaining
	 * the word to index vector mapping. This method can be used to re-use the
	 * same instance of a {@code RandomIndexing} on multiple corpora while
	 * keeping the same semantic space.
	 */

	public void removeAllSemantics() {
		wordsToMeaningMap.clear();
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
	private double[] getSemanticVector(String word) {
		double[] v = wordsToMeaningMap.get(word);
		if (v == null) {
			// lock on the word in case multiple threads attempt to add it at
			// once
			synchronized (this) {
				// recheck in case another thread added it while we were waiting
				// for the lock
				v = wordsToMeaningMap.get(word);
				if (v == null) {
					v = new double[vectorLength];
					wordsToMeaningMap.put(word, v);
				}
			}
		}
		return v;
	}

	/**
	 * returns the context vector for the word
	 */
	public double[] getContextVector(String word) {
		double[] v = wordsToMeaningMap.get(word);
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
		return Collections.unmodifiableSet(wordsToMeaningMap.keySet());
	}

	/**
	 * 
	 * @return a mapping from the current set of tokens to the index vector used
	 *         to represent them
	 */
	public Map<String, TernaryVector> getWordToIndexVector() {
		return wordToIndexVectors;
	}

	/**
	 * 
	 * @return a mapping from the current set of tokens to the context vector
	 *         calculated for them
	 */
	public Map<String, double[]> getWordToMeaningVector() {
		return wordsToMeaningMap;
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

					TernaryVector iv = wordToIndexVectors.get(word);
					if (usePermutations) {
						iv = permutationFunc.permute(iv, permutations);
						++permutations;
					}

					add(focusMeaning, iv);
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

					TernaryVector iv = wordToIndexVectors.get(word);
					if (usePermutations) {
						iv = permutationFunc.permute(iv, permutations);
						++permutations;
					}

					add(focusMeaning, iv);
				}
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
	private static void add(double[] semantics, TernaryVector index) {
		// Lock on the semantic vector to avoid a race condition with another
		// thread updating its semantics. Use the vector to avoid a class-level
		// lock, which would limit the concurrency.
		synchronized (semantics) {
			for (int p : index.positiveDimensions())
				// semantics.add(p, 1);
				semantics[p] = semantics[p] + 1;
			for (int n : index.negativeDimensions())
				// semantics.add(n, -1);
				semantics[n] = semantics[n] - 1;

		}
	}

	/**
	 * Math function to add (vector2 * frequency) to vector1
	 * 
	 * @param vector1
	 * @param vector2
	 * @param frequency : to multiply vector2 by frequency and add to vector1
	 * @return
	 */
	public static double[] addArrays(double[] vector1, double[] vector2, int frequency) {
		if (vector2.length != vector1.length)
			throw new IllegalArgumentException(
					"int arrays of different sizes cannot be added");

		int length = vector2.length;
		for (int i = 0; i < length; ++i) {
			double value = (vector2[i] * frequency) + vector1[i];
			vector1[i] = value;
		}
		return vector1;
	}
	
	/**
	 * Math function to add (vector2 to vector1
	 * 
	 * @param vector1
	 * @param vector2
	 * @param frequency : to multiply vector2 by frequency and add to vector1
	 * @return
	 */
	public static double[] addArrays(double[] vector1, double[] vector2) {
		if (vector2.length != vector1.length)
			throw new IllegalArgumentException(
					"int arrays of different sizes cannot be added");

		int length = vector2.length;
		for (int i = 0; i < length; ++i) {
			double value = vector2[i] + vector1[i];
			vector1[i] = value;
		}
		return vector1;
	}
	
	
	/**
	 * Math function to add vector2 to vector1
	 * 
	 * @param vector1
	 * @param vector2
	 * @param frequency
	 * @return
	 */
	public static double[] devideArray(double[] vector, int divisionFactor) {
		int length = vector.length;
		double[] resultVector = new double[length];
		for (int i = 0; i < length; ++i) {
			double value = vector[i] / divisionFactor;
			resultVector[i] = value;
		}
		return resultVector;
	}
}
