package org.nic.isis.reputation.utils;

import edu.ucla.sspace.text.DocumentPreprocessor;
import edu.ucla.sspace.text.EnglishStemmer;

import java.util.Arrays;
import java.util.List;

public final class EmailUtils {

    private EmailUtils(){}

	/**
	 * tokenize the email content and performs various preprocessing
	 * @param content
	 * @return tokenized terms
	 */
	public static List<String> tokenizeContent(String content) {
		// to get individual terms
		DocumentPreprocessor docPreproc = new DocumentPreprocessor();
		content = docPreproc.process(content);
		String[] tokenizedTerms = content.replaceAll("[\\W&&[^\\s]]", "")
				.split("\\W+");
		//stem words to avoid same word being indexed with same meaning
		EnglishStemmer stemmer = new EnglishStemmer();
		for(int i=0; i < tokenizedTerms.length; i++){
			String stemmedToken = stemmer.stem(tokenizedTerms[i]);
			tokenizedTerms[i] = stemmedToken;
		}
		
		return Arrays.asList(tokenizedTerms);
	}

}
