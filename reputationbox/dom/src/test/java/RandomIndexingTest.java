import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import org.nic.isis.ri.RandomIndexing;
import org.nic.isis.ri.SemanticSpace;

import edu.ucla.sspace.vector.TernaryVector;


public class RandomIndexingTest {

	public static double getNormalizedFrequency(int wordCount, int totalWords){
		double normalizedFrequency = (double)wordCount/(double)totalWords;
		return normalizedFrequency;
	}
	
	public static void main(String[] args) {
		Map<String,TernaryVector> indexVectors = new HashMap<String, TernaryVector>();
		Map<String, double[]> contextVectors =new HashMap<String, double[]>();
		
		String sample = "this test";
		SemanticSpace ri = new RandomIndexing(indexVectors, contextVectors);
		
		String sample2 = "another text with more words";
		
		
		try {
			ri.processDocument(new BufferedReader(new StringReader(
					sample)));
			ri.processDocument(new BufferedReader(new StringReader(
					sample2)));
			
			
			Map<String, TernaryVector> indexVs = ri.getWordToIndexVector();
			Map<String, double[]> contextVs = ri.getWordToMeaningVector();
			for(String word : indexVs.keySet()){
				System.out.println("");
				TernaryVector iv = indexVs.get(word);
				int[] ivArray = iv.toArray();
				String vecStr = "";
				for(int x = 0; x < ivArray.length; x++){
					vecStr += ivArray[x] + " " ;
				}
				//System.out.println("The index vector for word : " + word + "\n" + vecStr + " \n");
				
				//double [] contextV = contextVs.get(word);
				double[] contextV = ri.getVector(word);
				String cvecStr = "";
				for(int x = 0; x < contextV.length; x++){
					cvecStr += contextV[x] + " " ;
				}
				System.out.println("The context vector for word : " + word + "\n" + cvecStr + " \n");
				
			}
			
			//System.out.println(getNormalizedFrequency(1, 100));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
}
