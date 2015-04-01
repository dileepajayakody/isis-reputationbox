package org.nic.isis.reputation.dom;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.VersionStrategy;

import org.apache.isis.applib.annotation.ObjectType;
import org.apache.isis.applib.annotation.Programmatic;
import org.nic.isis.ri.RandomIndexing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucla.sspace.vector.TernaryVector;

/**
 * Data Structure for IndexVectors for words
 * @author dileepa
 *
 */

public class IndexVectorMap {

	private List<String> indexWords;
	private List<TernaryVector> indexVectors;

	//for computing incremental logidf keeping track of word document frequencies
	//how many documents has this word
	private List<Integer> wordFrequencies = new ArrayList<Integer>();
	
	
	private final static Logger logger = LoggerFactory
			.getLogger(IndexVectorMap.class);
	
	public IndexVectorMap(){
		indexWords = new ArrayList<String>();
		indexVectors = new ArrayList<TernaryVector>();
	}
	
	public synchronized void putIndexVector(String key, TernaryVector value) {
		int[] arrayVal = value.toArray();
		/*String valueStr = "<";
		for(int x : arrayVal){
			valueStr += ", " + x;
		}
		valueStr += ">";
		*/
//		int[] posArray = value.positiveDimensions();
//		int[] negArray = value.negativeDimensions();
//		String posIndexes = "";
//		for(int index :  posArray){
//			posIndexes += index + ", ";
//		}
//		String negIndexes = "";
//		for(int index :  negArray){
//			negIndexes += index + ", ";
//		}
		
	    //if (!indexWords.contains(key)) {
	      //logger.info("Adding new word to IndexVectorMap : " + key);	
	      indexWords.add(key);
	      indexVectors.add(value);
	     // wordFrequencies.add(new Integer(1));
	    
	    //} else {
	    //  logger.info("the word : " + key + " is already in the indexVectorMap");
	      //int index = indexWords.indexOf(key);
	      //indexVectors.set(index, value);
	    //}
	      
	  }
	
//	public int getFrequencyForWord(String key){
//		if (!indexWords.contains(key)) {
//			/*logger.warn("No index vector found for key : " + key
//					+ " hence returning null");*/
//			return 0;
//		} else {
//			Integer frequency = wordFrequencies.get(indexWords.indexOf(key));
//			return frequency;
//			//logger.warn("GETTING indexVector for word : " + key + " pos Indexes : " + posIndexes +  " neg Indexes : " + negIndexes);
//		}
//		
//	}
	
	public Integer getWordFrequency(String key){
		Integer frequency = 0;
		if(!indexWords.contains(key)){
			return 0;
		}else {
			frequency = wordFrequencies.get(indexWords.indexOf(key));
		}
		return frequency;
	}
	
	public void setWordDocFrequencies(Map<String,Integer> wordDocFrequencies){
		for(String key: wordDocFrequencies.keySet()){
			
			      int index = indexWords.indexOf(key);
			      logger.info("index vector  : [" + index + "] word : " + key);
			      if(index < 0){
			    	  logger.info("the word: " + key + " is not properly added to indexWords..disregarding ");
			      }
			      else if(index == 0){
			    	  wordFrequencies.add(0, wordDocFrequencies.get(key));
					  
			      }else if(wordFrequencies.size() <= index){
			    	  while(wordFrequencies.size() <= (index + 1)){
			    		  wordFrequencies.add(null);
			    	  }
			    	  wordFrequencies.set(index, wordDocFrequencies.get(key));
			      } else {
			    	  wordFrequencies.set(index, wordDocFrequencies.get(key));				        
			      }
		}
	}
	


	public TernaryVector getIndexVector(String key) {
		TernaryVector returnedVector = null; 
		if (!indexWords.contains(key)) {
			logger.warn("No index vector found for key : " + key
					+ " hence returning null; VERY ODD");
			returnedVector = null;
		} else {
			returnedVector = indexVectors.get(indexWords.indexOf(key));
//			int[] posArray = returnedVector.positiveDimensions();
//			int[] negArray = returnedVector.negativeDimensions();
//			String posIndexes = "";
//			for (int index : posArray) {
//				posIndexes += index + ", ";
//			}
//			String negIndexes = "";
//			for (int index : negArray) {
//				negIndexes += index + ", ";
//			}
			//logger.warn("GETTING indexVector for word : " + key + " pos Indexes : " + posIndexes +  " neg Indexes : " + negIndexes);
		}
		
		/*int[] arrayVal = returnedVector.toArray();
		String valueStr = "<";
		for(int x : arrayVal){
			valueStr += ", " + x;
		}
		valueStr += ">";
		*/
		
		return returnedVector;
	}

	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true") 
	public List<String> getIndexWords() {
		return indexWords;
	}

	public void setIndexWords(List<String> indexWords) {
		this.indexWords = indexWords;
	}
	
	
	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public List<TernaryVector> getIndexVectors() {
		return indexVectors;
	}

	public void setIndexVectors(List<TernaryVector> indexVectors) {
		this.indexVectors = indexVectors;
	}
	
	@Programmatic
	public Map<String, TernaryVector> getIndexVectorMap(){
		Map<String, TernaryVector> returned = new HashMap<String, TernaryVector>();
		for(String word : indexWords){
			returned.put(word, getIndexVector(word));
		}
		return returned;
	}
	
	@Programmatic
	public Map<String, Integer> getWordDocFrequencyMap(){
		Map<String, Integer> wordDocFrequencyMap = new HashMap<String, Integer>();
		for(String word : indexWords){
			wordDocFrequencyMap.put(word, getWordFrequency(word));
		}
		return wordDocFrequencyMap;
	}
	
	@Programmatic
	public void setIndexVectorMap(Map<String, TernaryVector> ivs){
		this.indexWords = new ArrayList<String>();
		this.indexVectors = new ArrayList<TernaryVector>();
		for(String key : ivs.keySet()){
			//this.putIndexVector(key, ivs.get(key));
			indexWords.add(key);
			int ind = indexWords.indexOf(key);
			if(indexVectors.size() <= ind){
				 while(indexVectors.size() <= (ind + 1)){
		    		  indexVectors.add(null);
		    	  }
			}
			indexVectors.set(ind,ivs.get(key));
		}
	}
	
	@javax.jdo.annotations.Persistent
	@javax.jdo.annotations.Column(allowsNull = "true")
	public List<Integer> getWordFrequencies() {
		return wordFrequencies;
	}

	public void setWordFrequencies(List<Integer> wordFrequencies) {
		this.wordFrequencies = wordFrequencies;
	}

	public String toString(){
		String ivString = " size of indexvectormap : " + indexWords.size() + "\n";
			
		for(String iword : indexWords) {
			TernaryVector iv = this.getIndexVector(iword);
			String posIndexes = "";
			String negIndexes = "";	
			
			int[] posArray = iv.positiveDimensions();
			int[] negArray = iv.negativeDimensions();
			for(int index :  posArray){
				posIndexes += index + ", ";
			}
			for(int index :  negArray){
				negIndexes += index + ", ";
			}
			ivString += " Index word : " + iword + "  posIndexes: " + posIndexes + " negIndexes : " + negIndexes + " \n";
		}
		return ivString;
		
	}
}
