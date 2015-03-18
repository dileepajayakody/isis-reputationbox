/*
 * Copyright 2009 Keith Stevens 
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

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import edu.ucla.sspace.vector.TernaryVector;

/**
 * A common interface for interacting with semantic space models of meaning.
 * Implementations should expect the following sequence of processing.
 *
 * <ol>

 * <li> {@link #processDocument(BufferedReader) processDocument} will be called
 *      one or more times with the text of the corpus.
 *
 * <li> {@link #processSpace(Properties) processSpace} will be called after all
 *      the documents have been used.  Once this method has been called, no
 *      further calls to {@code processDocument} should be made
 *
 * <li> {@link #getVector(String) getVector} may be called after the space has
 *      been processed.  Implementations may optionally support this method
 *      being called prior to {@code processSpace} but this is not required.
 *
 * </ol>
 *
 * In addition, {@link #getWords()} may be called at any time to determine which
 * words are currently represented in the space.  Implementations should specify
 * in their class documentations what parameters are available as properties for
 * the {@code processSpace} method, and what the default value of those
 * parameters are.
 */
public interface SemanticSpace {

    /**
     * Processes the contents of the provided file as a document.
     *
     * @param document a reader that allows access to the text of the document
     *
     * @throws IOException if any error occurs while reading the document
     */
    void processDocument(BufferedReader document) throws IOException;

    
    /**
     * mainly used for NLP keyword processing
     * @param words
     * @return the aggregated index vectors of the words
     */
    double[] processWords(Map<String,Integer> words);

    /**
     * Returns the set of words that are represented in this semantic space.
     *
     * @return the set of words that are represented in this semantic space.
     */
    Set<String> getWords();
    
    //added by Dileepa for generalization purpose
    Map<String, TernaryVector> getWordToIndexVector();
    Map<String, double[]> getWordToMeaningVector();

    /**
     * Returns the semantic vector for the provided word.
     *
     * @param word a word that may be in the semantic space
     *
     * @return The vector for the provided word or {@code null} if the
     *          word was not in the space.
     */
    double[] getVector(String word);

    /**
     * Once all the documents have been processed, performs any post-processing
     * steps on the data.  An algorithm should treat this as a no-op if no
     * post-processing is required.  Callers may specify the values for any
     * exposed parameters using the {@code properties} argument.
     *
     * <p>
     *
     * By general contract, once this method has been called, {@code
     * processDocument} will not be called again.
     *
     * @param properties a set of properties and values that may be used to
     *        configure any exposed parameters of the algorithm.
     */
    void processSpace(Properties properties);

    /**
     * Returns a unique string describing the name and configuration of this
     * algorithm.  Any configurable parameters that would affect the resulting
     * semantic space should be expressed as a part of this name.
     */
    String getSpaceName();

    /**
     * Returns the length of vectors in this semantic space.  Implementations
     * are left free to define whether the returned value is valid before {@link
     * #processSpace(Properties) processSpace} is called.
     */
    int getVectorLength();
}
