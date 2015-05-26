package cc.mallet.topics.randomscan.document;

import cc.mallet.configuration.LDAConfiguration;
import cc.mallet.topics.LDAGibbsSampler;

public class AdaptiveBatchBuilder extends PercentageBatchBuilder {

	// Set default period to be 0 iterations
	// For the NIPS data 125 iterations seems to be a good value
	int deltaInstabilityPeriod = 0;
		
	public AdaptiveBatchBuilder(LDAConfiguration config, LDAGibbsSampler sampler) {
		super(config, sampler);
		deltaInstabilityPeriod = config.getInstabilityPeriod(0); 
	}

	public int getDeltaInstabilityPeriod() {
		return deltaInstabilityPeriod;
	}

	public void setDeltaInstabilityPeriod(int instabilityPeriod) {
		this.deltaInstabilityPeriod = instabilityPeriod;
	}

	boolean inInstabilityPeriod() {
		return sampler.getCurrentIteration()<getDeltaInstabilityPeriod();
	}
	
	@Override
	protected void calculateDocumentsToSample(LDAConfiguration config,
			double docsPercentage) {
		if(inInstabilityPeriod()) {
			super.calculateDocumentsToSample(config, 1.0);
		} else {			
			super.calculateDocumentsToSample(config, docsPercentage);
		}
	}

	@Override
	public void calculateBatch() {
		if(inInstabilityPeriod()) {
			calculateDocumentsToSample(config, 1.0);
			int docIdx = 0;
			for(int i = 0; i < numBatches; i++) {
				int docsInBatch = docsPerBatch + (remainder > i ? 1 : 0);
				int [] indices = new int[docsInBatch];
				for (int j = 0; j < docsInBatch; j++) {
					indices[j] = docIdx++;
				}
				docBatches[i] = indices;
			}
		} else {			
			calculateDocumentsToSample(config, docsPercentage);
			super.calculateBatch();
		}
	}

	@Override
	int calcDocsToSample() {
		if(inInstabilityPeriod()) {
			return totalDocsAvailable;
		} else {			
			return super.calcDocsToSample();
		}
	}
}
