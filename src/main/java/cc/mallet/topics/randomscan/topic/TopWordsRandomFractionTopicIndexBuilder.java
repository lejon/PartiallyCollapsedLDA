package cc.mallet.topics.randomscan.topic;

import org.apache.commons.math3.distribution.BetaDistribution;

import cc.mallet.configuration.LDAConfiguration;
import cc.mallet.topics.LDAGibbsSampler;

public class TopWordsRandomFractionTopicIndexBuilder implements TopicIndexBuilder {
	
	LDAConfiguration config;
	LDAGibbsSampler sampler; 
	int instabilityPeriod = 0;
	double a = 2.0;
	double b = 5.0;
	double finalA = 5.0;
	double finalB = 0.05;
	double ainc; 
	double binc; 
	int fullPhiPeriod;
	AllWordsTopicIndexBuilder allWords;

	public TopWordsRandomFractionTopicIndexBuilder(LDAConfiguration config, LDAGibbsSampler sampler) {
		this.config = config;
		this.sampler = sampler;
		instabilityPeriod = config.getInstabilityPeriod(0); 
		fullPhiPeriod = config.getFullPhiPeriod(-1);
		allWords = new AllWordsTopicIndexBuilder(config,sampler);
		int noIter = 200;
		ainc = (finalA-a)/noIter;
		binc = (b-finalB)/noIter;
	}
			
	/**
	 * Decide which types (words) to sample in Phi proportional to the corpus frequency the type.
	 * Types that have high frequency should be sampled more often but according
	 * to random scan contract ALL types MUST have a small probability to be 
	 * sampled. We draw the proportion of types to sample from a Beta distribution
	 * with a mode centered on 20 % from the start which tends towards 100% as the number
	 * of iterations tend towards Inf. In the beginning we will sample the 20% most probable
	 * words, but sometimes we will sample them all. After a while we always sample full Phi
	 * A Beta(2.0,5.0) will have the mode (a-1) / (a+b-2) = 0.2 = 20%
	 */
	@Override
	public int[][] getTopicTypeIndices() {		
		// If we are in the instable period, sample everything (null means everything)
		if(sampler.getCurrentIteration()<instabilityPeriod) return null;

		if(a>finalA && b <finalB) 
			return allWords.getTopicTypeIndices();
		
		if(fullPhiPeriod>0 && ((sampler.getCurrentIteration() % fullPhiPeriod) == 0))
			return allWords.getTopicTypeIndices();			
		
		int [] topIndices = sampler.getTopTypeFrequencyIndices();
		
		BetaDistribution beta = new BetaDistribution(a,b);
		double percentToSample = beta.sample();
		if(a<finalA)
			a += ainc;
		if(b>finalB)
			b -= binc;
		
		int noToSample = (int) Math.ceil(percentToSample*topIndices.length);
		int [] indicesToSample = new int[noToSample];
		for (int i = 0; i < noToSample; i++) {
			indicesToSample[i] = topIndices[i];
		}
		int [][] topicTypeIndices = new int [sampler.getNoTopics()][];
		// In the basic version we sample the same tokens (words) in all the topics
		for (int i = 0; i < topicTypeIndices.length; i++) {
			topicTypeIndices[i] = indicesToSample;
		}
		return topicTypeIndices;
	}

}
