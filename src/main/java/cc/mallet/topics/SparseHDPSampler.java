package cc.mallet.topics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import cc.mallet.configuration.LDAConfiguration;

public abstract class SparseHDPSampler extends PolyaUrnSpaliasLDA implements HDPSamplerWithPhi {

	private static final long serialVersionUID = 1L;
	
	List<Integer> activeTopicHistory = new ArrayList<Integer>();
	List<Integer> activeTopicInDataHistory = new ArrayList<Integer>();
	// Used to track the number of times a topic occurs in the dataset
	int [] topicOcurrenceCount;

	public SparseHDPSampler(LDAConfiguration config) {
		super(config);
	}
	
	public int[] getTopicOcurrenceCount() {
		return topicOcurrenceCount;
	}

	public void setTopicOcurrenceCount(int[] topicOcurrenceCount) {
		this.topicOcurrenceCount = topicOcurrenceCount;
	}

	public List<Integer> getActiveTopicHistory() {
		return activeTopicHistory;
	}

	public void setActiveTopicHistory(List<Integer> activeTopicHistory) {
		this.activeTopicHistory = activeTopicHistory;
	}

	public List<Integer> getActiveTopicInDataHistory() {
		return activeTopicInDataHistory;
	}

	public void setActiveTopicInDataHistory(List<Integer> activeInDataTopicHistory) {
		this.activeTopicInDataHistory = activeInDataTopicHistory;
	}

	public static int calcK(double percentile, int [] tokensPerTopic) {
		int [] sortedAllocation = Arrays.copyOf(tokensPerTopic, tokensPerTopic.length);
		Arrays.sort(sortedAllocation);
		int [] ecdf = calcEcdf(sortedAllocation);  
		int k95 = findPercentile(ecdf,percentile);
		return k95;
	}

	public static int findPercentile(int[] ecdf, double percentile) {
		double total = ecdf[ecdf.length-1];
		for (int j = 0; j < ecdf.length; j++) {
			if(ecdf[j]/total > percentile) {
				return j;
			}
		}
		return ecdf.length;
	}

	public static int[] calcEcdf(int[] sortedAllocation) {
		int [] ecdf = new int[sortedAllocation.length]; 
		ecdf[0] = sortedAllocation[sortedAllocation.length-1];
		for(int i = 1; i < sortedAllocation.length; i++) {
			ecdf[i] = sortedAllocation[sortedAllocation.length - i - 1] + ecdf[i-1]; 
		}

		return ecdf;
	}

}
