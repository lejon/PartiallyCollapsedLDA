package cc.mallet.util;

import java.util.ArrayList;

import cc.mallet.topics.ADLDA;
import cc.mallet.topics.SerialCollapsedLDA;
import cc.mallet.topics.TopicAssignment;
import cc.mallet.topics.UncollapsedParallelLDA;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;

public class PerplexityCalculator {

	public PerplexityCalculator() {
	}
	
	public static double perplexity(UncollapsedParallelLDA model, InstanceList testSet) {
		double perplexity = 0.0;
		ArrayList<TopicAssignment> dataset = model.getDataset();
		
		int testDocIdx = dataset.size()-testSet.size();
		System.out.println("Test docs start at:" + testDocIdx);
		System.out.println("Test set size:" + testSet.size());
		System.out.println("Training set size:" + testSet.size());
		
		// Ensure that the training set + corresponding test doc is the same as the 
		// doc in the original data set (but don't care about word order, since we
		// select the words randomly)
		for(Instance testInst : testSet) {
			Instance inst = dataset.get(testDocIdx).instance;
			
			testDocIdx++;
		}
		
		return perplexity;
	}

	public static double perplexity(ADLDA model, InstanceList testSet) {
		double perplexity = 0.0;
		return perplexity;
	}

	public static double perplexity(SerialCollapsedLDA model, InstanceList testSet) {		
		double perplexity = 0.0;
		return perplexity;
	}

}
