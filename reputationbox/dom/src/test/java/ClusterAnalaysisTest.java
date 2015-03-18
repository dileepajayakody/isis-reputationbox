import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.map.HashedMap;

/**
 * @author dileepa
 *
 *A test class to analyse the Weka result file
 */
public class ClusterAnalaysisTest {

	/**
	 * @param args
	 */
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String directoryPath = "/home/dileepa/Desktop/research/DATA/reputationBasedSpamFilter/Project_Results/RandomIndexing_WholeEmail/processing";
		File subjectOnlyFile = new File(directoryPath, "subjectOnly_analysis.txt");
		//topicClusterId:List of emailIds
		Map<String,List<String>> topicClusters = new HashedMap<String, List<String>>(); 
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(subjectOnlyFile));
			String line;
			int lineId = 1;
			
			while ((line = br.readLine()) != null) {
				//<B15B3B8D-5308-4105-AB33-551D27DE413E@gmail.com>,'Re: Getting started with OpenNLP',14942f58be07ea82,null,null
				String[] tokens = line.split(",");
				int skipIndex = 0;
				String topicClusterId  = null;
				if(tokens.length > 5){
					int begin = line.indexOf("'")+1;
					int end = line.lastIndexOf("'");
					
					String subject = line.substring(begin, end);
					//System.out.println(lineId + " : " +  subject);
					String[] topicTokens = subject.split(",");
					int topicTokenSize = topicTokens.length;
					skipIndex += topicTokenSize;
					topicClusterId = tokens[2+skipIndex];
				}else {
					topicClusterId = tokens[3];
				}
				
				
				System.out.println(lineId +"  : " + topicClusterId);
				
				if(topicClusters.get(topicClusterId) != null){
					topicClusters.get(topicClusterId).add(tokens[0]);
				} else {
					List<String> emailIds = new ArrayList<String>();
					emailIds.add(tokens[0]);
					topicClusters.put(topicClusterId, emailIds);
				}
				
				lineId++;
			}
			System.out.println("Topic Cluster Results.....");
			for(String topicId : topicClusters.keySet()){
				System.out.println(topicId + " :" + topicClusters.get(topicId).size());
				for(Object emailId : topicClusters.get(topicId).toArray()){
					System.out.println((String)emailId);
				}

				System.out.println();
			}
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	
	
}
