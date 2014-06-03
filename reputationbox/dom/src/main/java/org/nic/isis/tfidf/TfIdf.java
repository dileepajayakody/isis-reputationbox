/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.nic.isis.tfidf;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Class to calculate TfIdf of term.
 * @author Mubin Shrestha
 */
public class TfIdf {
    
    /**
     * Calculated the tf of term termToCheck
     * @param totaltermsinEmail : Array of all the words under processing email
     * @param termToCheck : term of which tf is to be calculated.
     * @return tf(term frequency) of term termToCheck
     */
    public static double tfCalculator(List<String> totaltermsinEmail, String termToCheck) {
        double count = 0;  //to count the overall occurrence of the term termToCheck
        for (String s : totaltermsinEmail) {
            if (s.equalsIgnoreCase(termToCheck)) {
                count++;
            }
        }
        return count / totaltermsinEmail.size();
    }

    /**
     * Calculated idf of term termToCheck
     * @param allTerms : all the terms of all the documents
     * @param termToCheck
     * @return idf(inverse document frequency) score
     */
    public static double idfCalculator(Collection<List<String>> allEmailTerms, String termToCheck) {
		double count = 0;
		for (List<String> termsInAnEmail : allEmailTerms) {
			for (String s : termsInAnEmail){
				if (s.equalsIgnoreCase(termToCheck)) {
					count++;
					break;
				}
			}
			
		}
		return Math.log(allEmailTerms.size() / count);
    }
}