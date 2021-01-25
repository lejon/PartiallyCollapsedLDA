package cc.mallet.similarity;

import java.util.Arrays;

import org.apache.commons.math3.distribution.TDistribution;

import cc.mallet.configuration.LDAConfiguration;
import cc.mallet.topics.LDASamplerWithPhi;
import cc.mallet.types.Alphabet;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.util.LDAUtils;
import cc.mallet.util.StringClassArrayIterator; 

public class LongQueryLDALikelihoodDistance extends LDALikelihoodDistance {

	public LongQueryLDALikelihoodDistance(int K, double alpha) {
		super(alpha);
	}
	
	public LongQueryLDALikelihoodDistance(LDASamplerWithPhi trainedSampler) {
		super(trainedSampler);
	}

	public LongQueryLDALikelihoodDistance(LDAConfiguration config) {
		super(config);
	}
	
	@Override
	public double calculate(double[] v1, double[] v2) {
		int doc1length = getDocLength(v1);
		int doc2length = getDocLength(v2);

		int [] v1Indices = new int[doc1length];
		int [] v2Indices = new int[doc2length];
		
		int wc = 0;
		for (int i = 0; i < v1.length; i++) {
			int wordFreq = (int) v1[i];
			if(wordFreq>0) {
				for (int j = 0; j < wordFreq; j++) {					
					v1Indices[wc++] = i;
				}
			}
		}

		wc = 0;
		for (int i = 0; i < v2.length; i++) {
			int wordFreq = (int) v2[i];
			if(wordFreq>0) {
				for (int j = 0; j < wordFreq; j++) {					
					v2Indices[wc++] = i;
				}
			}
		}
		
		Alphabet alphabet = trainingset.getAlphabet();
		String s1 = LDAUtils.indicesToString(v1Indices, alphabet);
		String s2 = LDAUtils.indicesToString(v2Indices, alphabet);
		String [] doclines = new String [] {s1,s2};
		StringClassArrayIterator readerTest = new StringClassArrayIterator (
				doclines, "X"); 

		InstanceList testInstances = new InstanceList(trainingset.getPipe());
		testInstances.addThruPipe(readerTest);
		
		Instance instanceQuery = testInstances.get(0);
		int[] wordTokensQuery = LDAUtils.getWordTokens(instanceQuery);
		int hashCodeQuery = Arrays.hashCode(wordTokensQuery);
		double [] thetaQuery;
		if(!cache.containsKey(hashCodeQuery)) {
			thetaQuery = sample(instanceQuery);
			cache.put(hashCodeQuery, thetaQuery);
		} else {
			thetaQuery = cache.get(hashCodeQuery);
		}

		Instance instanceDoc = testInstances.get(1);
		int[] wordTokensDoc = LDAUtils.getWordTokens(instanceDoc);
		int hashCodeDoc = Arrays.hashCode(wordTokensDoc);
		double [] thetaDoc;
		if(!cache.containsKey(hashCodeDoc)) {
			thetaDoc = sample(instanceDoc);
			cache.put(hashCodeDoc, thetaDoc);
		} else {
			thetaDoc = cache.get(hashCodeDoc);
		}

		sampledQueryTopics.put(Arrays.hashCode(v1), thetaQuery);
		sampledQueryTopics.put(Arrays.hashCode(v2), thetaDoc);

		int [] v1FreqDoc = Arrays.stream(v1).mapToInt(x -> (int) x).toArray();
		int [] v2FreqDoc = Arrays.stream(v2).mapToInt(x -> (int) x).toArray();
		return ldaLoglikelihood(v1FreqDoc, thetaQuery, v2FreqDoc, thetaDoc);
	}

	
	/**
	 * Calculate p(query|document) 
	 * @param query Frequency encoded query (query.length == vocabulary.length)
	 * @param document Frequency encoded document (document.length == vocabulary.length)
	 * @param theta
	 * @return logLikelihood of document generating query
	 */
	public double ldaLoglikelihood(int[] query, double[] queryTheta, int[] document, double[] documentTheta) {
		//Map<Integer, Double> p_w_d = calcProbWordGivenDocMLFrequencyEncoding(document);
		double [] p_w_d = calcProbWordGivenDocMLFrequencyEncoding(document);
		
		double querylength1 = getDocLength(query);
		double doclength1 = getDocLength(document);
		
		// Some sanity check first
		if(querylength1 == 0 && doclength1 == 0) return 0;
		if(querylength1 == 0 && doclength1 != 0) return Double.POSITIVE_INFINITY;
		if(querylength1 != 0 && doclength1 == 0) return Double.POSITIVE_INFINITY;
		
		if(mixtureRatio<0) {
			mixtureRatio = (doclength1 / (doclength1 + mu));
		}
		
		double p_q_d = 0.0;		
		for (int i = 0; i < query.length; i++) {
			double wordProb = 0.0;
			int wordFreq = (int)query[i];
			if(wordFreq > 0) {
				int word = i;
				double wordCorpusProb = calcProbWordGivenCorpus(word);
				
				wordProb = p_w_d[word];
				
				p_q_d += Math.log(mixtureRatio * wordProb + 
						(1-mixtureRatio) * wordCorpusProb);	
			}
		}
		
		
		// Add KL distance between documents
		KLDistance kld = new KLDistance();
		double kldist = kld.calculate(queryTheta, documentTheta);
		p_q_d += kldist;
		
		double querylength = getDocLength(query);
		double doclength = getDocLength(document);

		double df = 150;
		TDistribution tdist = new TDistribution(df);
		double diff = Math.log(Math.abs(querylength-doclength));
		double length_prob;
		if((querylength-doclength)==0) {
			length_prob = 0;
		} else {
			length_prob = -1 * Math.log(tdist.density(diff));
		}
		
		// Add length distance between docs
		p_q_d += length_prob;
		
		return p_q_d;
	}
	
	@Override
	public double distanceToTrainingSample(double[] query, int sampleId) {		
		double [] thetaDoc = trainingSetTopicDists[sampleId];
		
		int [] queryDoc = new int[query.length];
		for (int i = 0; i < queryDoc.length; i++) {
			queryDoc[i] = (int)query[i];
		}
		
		int querylength = getDocLength(query);
		int [] v1Indices = new int[querylength];
		
		int wc = 0;
		for (int i = 0; i < queryDoc.length; i++) {
			int wordFreq = (int) queryDoc[i];
			if(wordFreq>0) {
				for (int j = 0; j < wordFreq; j++) {					
					v1Indices[wc++] = i;
				}
			}
		}
		
		Alphabet alphabet = trainingset.getAlphabet();
		String s1 = LDAUtils.indicesToString(v1Indices, alphabet);
		String [] doclines = new String [] {s1};
		StringClassArrayIterator readerTest = new StringClassArrayIterator (
				doclines, "X"); 

		InstanceList testInstances = new InstanceList(trainingset.getPipe());
		testInstances.addThruPipe(readerTest);
		
		Instance instanceQuery = testInstances.get(0);
		int hashCodeQuery = Arrays.hashCode(LDAUtils.getWordTokens(instanceQuery));
		double [] queryTheta;
		if(!cache.containsKey(hashCodeQuery)) {
			queryTheta = sample(instanceQuery);
			cache.put(hashCodeQuery, queryTheta);
		} else {
			queryTheta = cache.get(hashCodeQuery);
		}

		TokenFrequencyVectorizer tv = new TokenFrequencyVectorizer();
		int [] testDoc = Arrays
				.stream(tv.instanceToVector(trainingset.get(sampleId)))
				.mapToInt(x -> (int)x)
				.toArray();
		
		return -ldaLoglikelihood(queryDoc, queryTheta, testDoc, thetaDoc);
	}
	
	@Override
	public double distance(Instance queryInstance, Instance documentInstance) {
		TokenFrequencyVectorizer tv = new TokenFrequencyVectorizer();
		int [] queryDoc = tv.instanceToIntVector(queryInstance);
		int [] document = tv.instanceToIntVector(documentInstance);
		
		double [] docTheta;
		if(sampledTopics.get(documentInstance) == null) {
			docTheta = sample(documentInstance);
			sampledTopics.put(documentInstance, docTheta);
		} else {
			docTheta = sampledTopics.get(documentInstance);
		}

		double [] queryTheta;
		if(sampledTopics.get(queryInstance) == null) {
			queryTheta = sample(queryInstance);
			sampledTopics.put(queryInstance, docTheta);
		} else {
			queryTheta = sampledTopics.get(queryInstance);
		}
		
		return -ldaLoglikelihood(queryDoc, queryTheta, document, docTheta);
	}


}
