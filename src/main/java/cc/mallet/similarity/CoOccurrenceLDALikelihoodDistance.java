package cc.mallet.similarity;

import cc.mallet.configuration.LDAConfiguration;
import cc.mallet.topics.LDASamplerWithPhi; 

public class CoOccurrenceLDALikelihoodDistance extends LDALikelihoodDistance {

	StreamCorpusStatistics cs;
	
	public CoOccurrenceLDALikelihoodDistance(double alpha, StreamCorpusStatistics cs) {
		super(alpha);
		this.cs = cs;
	}
	
	public CoOccurrenceLDALikelihoodDistance(LDASamplerWithPhi trainedSampler,StreamCorpusStatistics cs) {
		super(trainedSampler);
		this.cs = cs;
	}

	public CoOccurrenceLDALikelihoodDistance(LDAConfiguration config,StreamCorpusStatistics cs) {
		super(config);
		this.cs = cs;
	}
	
	/**
	 * Calculate p(query|document) 
	 * @param query Frequency encoded query (query.length == vocabulary.length)
	 * @param document Frequency encoded document (document.length == vocabulary.length)
	 * @param theta
	 * @return logLikelihood of document generating query
	 */
	public double ldaLoglikelihood(int[] query, int[] document, double[] theta) {
		//Map<Integer, Double> p_w_d = calcProbWordGivenDocMLFrequencyEncoding(document);
		double [] p_w_d = calcProbWordGivenDocMLFrequencyEncoding(document);

		double querylength = getDocLength(query);
		double doclength = getDocLength(document);

		// Some sanity check first
		if(querylength == 0 && doclength == 0) return 0;
		if(querylength == 0 && doclength != 0) return Double.POSITIVE_INFINITY;
		if(querylength != 0 && doclength == 0) return Double.POSITIVE_INFINITY;

		if(mixtureRatio<0) {
			mixtureRatio = (doclength / (doclength + mu));
		}

		double p_q_d = 0.0;		
		for (int i = 0; i < query.length; i++) {
			double wordProb = 0.0;
			int wordFreq = (int)query[i];
			if(wordFreq > 0) {
				int word = i;
				double wordTopicProb;
				// No need to calculate this if it will have no effect
				if(lambda < 1) {
					wordTopicProb = calcProbWordGivenTheta(theta, word, phi);
				} else { 
					wordTopicProb = 0.0;
				}
				double wordCorpusProb = calcProbWordGivenCorpus(word);

				if(p_w_d[word] != 0.0) {
					wordProb = p_w_d[word];
				} else {
					wordProb = coOccurrenceScore(word,document,cs);
				}
				p_q_d += Math.log(lambda * (mixtureRatio * wordProb + 
						(1-mixtureRatio) * wordCorpusProb) + 
						(1-lambda) * wordTopicProb);	
			}
		}

		return p_q_d;
	}

	static double coOccurrenceScore(int word, int[] document, StreamCorpusStatistics cs) {
		double score = 0.0;
		double documentLength = 0.0;
		for (int coOccurringWord = 0; coOccurringWord < document.length; coOccurringWord++) {
			int coOccurringWordFreq = document[coOccurringWord];
			double probInc = cs.getCoOccurrence(word, coOccurringWord) / (double) cs.getDocFreqs()[word];
			score += (coOccurringWordFreq * probInc);
			documentLength += coOccurringWordFreq;
		}
		return score / documentLength;
	}
}
