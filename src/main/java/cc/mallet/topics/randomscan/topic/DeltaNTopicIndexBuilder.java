package cc.mallet.topics.randomscan.topic;

import cc.mallet.configuration.LDAConfiguration;
import cc.mallet.topics.LDAGibbsSampler;

public class DeltaNTopicIndexBuilder implements TopicIndexBuilder {
	
	LDAConfiguration config;
	LDAGibbsSampler sampler; 
	int instabilityPeriod = 0;
	int fullPhiPeriod = -1;
	AllWordsTopicIndexBuilder allWords;

	public DeltaNTopicIndexBuilder(LDAConfiguration config, LDAGibbsSampler sampler) {
		this.config = config;
		this.sampler = sampler;
		instabilityPeriod = config.getInstabilityPeriod(0); 
		fullPhiPeriod = config.getFullPhiPeriod(-1);
		allWords = new AllWordsTopicIndexBuilder(config,sampler);
	}
			
	/**
	 * Decide which types (words) to sample in Phi proportional how much they changed
	 * in the last update round. 
	 */
	@Override
	public int[][] getTopicTypeIndices() {
		// If we are in the instable period, sample everything (null means everything)
		int currentIteration = sampler.getCurrentIteration();
		if(currentIteration<instabilityPeriod) return null;
		
		// Every fullPhiPeriod we sample the whole Phi
		if(fullPhiPeriod>0 && ((currentIteration % fullPhiPeriod) == 0)) {
			return allWords.getTopicTypeIndices();			
		} else {
			return sampler.getDeltaStatistics();
		}
	}

}
