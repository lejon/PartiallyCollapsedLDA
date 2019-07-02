package cc.mallet.similarity;

import org.apache.commons.math3.distribution.TDistribution;

import cc.mallet.configuration.LDAConfiguration;
import cc.mallet.topics.LDASamplerWithPhi; 

public class LengthLDALikelihoodDistance extends LDALikelihoodDistance {

	public LengthLDALikelihoodDistance(int K, double alpha) {
		super(K,alpha);
	}
	
	public LengthLDALikelihoodDistance(LDASamplerWithPhi trainedSampler) {
		super(trainedSampler);
	}

	public LengthLDALikelihoodDistance(LDAConfiguration config) {
		super(config);
	}
	
	/**
	 * Calculate p(query|document) 
	 * @param query Frequency encoded query (query.length == vocabulary.length)
	 * @param document Frequency encoded document (document.length == vocabulary.length)
	 * @param theta
	 * @return logLikelihood of document generating query
	 */
	public double ldaLoglikelihood(int[] query, int[] document, double[] theta) {
		double p_q_d = super.ldaLoglikelihood(query, document, theta);
		
		double querylength = getDocLength(query);
		double doclength = getDocLength(document);

		double df = 150;
		TDistribution tdist = new TDistribution(df);
		double diff = Math.log(Math.abs(querylength-doclength));
		double length_prob;
		if((querylength-doclength)==0) {
			length_prob = 0;
		} else {
			length_prob = Math.log(tdist.density(diff));
		}

		p_q_d = p_q_d + length_prob;
		
		return p_q_d;
	}
}
