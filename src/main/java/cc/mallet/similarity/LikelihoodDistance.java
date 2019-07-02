package cc.mallet.similarity;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import cc.mallet.types.FeatureSequence;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList; 

/**
 * 
 * This distance measure assumes that the features are coded with word frequency
 * It the input documents are both of the same size (the length of the Alphabet)
 * and each slot has the number of times that word occurs in the document
 * 
 * @author Leif Jonsson
 *
 */
public class LikelihoodDistance implements TrainedDistance, InstanceDistance {
	int [] N_d;
	double mu = 1;
	double [] p_w_coll;
	double mixtureRatio = -1;
	
	InstanceList trainingset;
	
	public LikelihoodDistance() {
		
	}
	
	public LikelihoodDistance(InstanceList trainingset) {
		super();
		this.trainingset = trainingset;
		init(trainingset);
	}

	/**
	 * returns the loglikelihood of document v2 generating document v1
	 * given the language model and corpus
	 */
	@Override
	public double calculate(double[] v1, double[] v2) {
		Set<Integer> uniqueDocumentWords = new HashSet<>();
		Map<Integer,Double> p_w_d = new HashMap<>();

		double d1length = 0;
		for (int i = 0; i < v1.length; i++) {
			d1length += v1[i];
		}
		
		double d2length = 0;
		// Find the number of unique words in v2
		// Find the number of times each unique word occurs in v2
		for (int i = 0; i < v2.length; i++) {
			int wordFreq = (int)v2[i];
			if(wordFreq>0) {
				d2length += wordFreq;
				int word = i;
				uniqueDocumentWords.add(word);
				if(p_w_d.get(word) == null) {
					p_w_d.put(word,0.0);
				}
				p_w_d.put(word,p_w_d.get(word) + wordFreq);
			}
		}

		// Some sanity check first
		if(d1length == 0 && d2length == 0) return 0;
		if(d1length == 0 && d2length != 0) return Double.POSITIVE_INFINITY;
		if(d1length != 0 && d2length == 0) return Double.POSITIVE_INFINITY;

		// Normalize
		if(d2length!=0) {
			for (Integer word : p_w_d.keySet()) {
				p_w_d.put(word,p_w_d.get(word) / d2length);
			}
		}

		if(mixtureRatio<0) {
			mixtureRatio = (d2length / (d2length + mu));
		}

		double p_w = 0.0;
		for (int i = 0; i < v1.length; i++) {
			double wordProb = 0.0;
			int wordFreq = (int)v1[i];
			if(wordFreq > 0) {
				int word = i;
				if(p_w_d.get(word) != null) {
					wordProb = p_w_d.get(word);
				}
				p_w += Math.log(mixtureRatio * wordProb + 
						(1-mixtureRatio) * p_w_coll[word]);
			}
		}
		
		return -p_w;
	}
	
	public double getMixtureRatio() {
		return mixtureRatio;
	}

	public void setMixtureRatio(double mixtureRatio) {
		this.mixtureRatio = mixtureRatio;
	}

	@Override
	public void init(InstanceList trainingset) {
		this.trainingset = trainingset;
		N_d = new int[trainingset.size()];
		p_w_coll = new double[trainingset.getAlphabet().size()];
		mu = trainingset.getAlphabet().size();
		long corpusSize = 0;
		for (int i = 0; i < trainingset.size(); i++) {
			FeatureSequence words = (FeatureSequence)trainingset.get(i).getData();
			N_d[i] = words.size();
			for (int j = 0; j < words.size(); j++) {
				p_w_coll[words.getIndexAtPosition(j)]++;
				corpusSize++;
			}
		}	
		
		//normalize
		for (int i = 0; i < p_w_coll.length; i++) {
			p_w_coll[i] /= corpusSize;
		}
	}

	@Override
	public double distanceToTrainingSample(double[] query, int sampleId) {	
		TokenFrequencyVectorizer tv = new TokenFrequencyVectorizer();
		double [] coordinates = tv.instanceToVector(trainingset.get(sampleId));
		
		double dd = calculate(query, coordinates);
		//System.out.println(dd + ": to " + LDAUtils.instanceToString(trainingset.get(sampleId)));
		return dd;
	}

	@Override
	public double[] distanceToAll(Instance testInstance) {
		TokenFrequencyVectorizer tfv = new TokenFrequencyVectorizer();
		double [] testVector = tfv.instanceToVector(testInstance);
		double [] result = new double[trainingset.size()];
		for (int i = 0; i < trainingset.size(); i++) {
			result[i] = distanceToTrainingSample(testVector,i);
		}
		return result;
	}

	@Override
	public double distance(Instance instance1, Instance instance2) {
		TokenFrequencyVectorizer tv = new TokenFrequencyVectorizer();
		double [] coordinates1 = tv.instanceToVector(instance1);
		double [] coordinates2 = tv.instanceToVector(instance2);

		double dd = calculate(coordinates1, coordinates2);
		return dd;

	}

}
