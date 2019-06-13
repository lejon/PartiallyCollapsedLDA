package cc.mallet.similarity;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import cc.mallet.types.FeatureSequence;
import cc.mallet.types.InstanceList; 

/**
 * 
 * This distance measure assumes that the features are coded with word frequency
 * 
 * @author Leif Jonsson
 *
 */
public class LikelihoodDistance implements TrainedDistance {
	int [] N_d;
	double mu = 1000;
	double epsilon = 0.0;
	double [] p_w_coll;
	
	InstanceList trainingset;
	
	@Override
	public double calculate(double[] v1, double[] v2) {
		double p_w = 0.0;
		double N_d = v2.length;

		Set<Integer> uniqueDocumentWords = new HashSet<>();
		Map<Integer,Double> p_w_d = new HashMap<>();

		// Find the number of unique words in v2
		// Find the number of times each word occurs in v2
		for (int i = 0; i < v2.length; i++) {
			int wordFreq = (int)v2[i];
			if(wordFreq>0) {
				int word = i;
				uniqueDocumentWords.add(word);
				if(p_w_d.get(word) == null) {
					p_w_d.put(word,0.0);
				}
				p_w_d.put(word,p_w_d.get(word) + wordFreq);
			}
		}

		// Normalize
		for (Integer word : p_w_d.keySet()) {
			p_w_d.put(word,p_w_d.get(word) /  N_d);
		}

		double mixtureRatio = (N_d / (N_d + mu));
		for (int i = 0; i < v1.length; i++) {
			double wordProb = epsilon;
			int wordFreq = (int)v1[i];
			if(wordFreq > 0) {
				int word = i;
				if(p_w_d.get(word) != null) {
					wordProb = p_w_d.get(word);
				}
				p_w += mixtureRatio * wordProb + 
						(1-mixtureRatio) * p_w_coll[word];
			}
		}
		
		return 1-p_w;
	}

	@Override
	public void init(InstanceList trainingset) {
		this.trainingset = trainingset;
		N_d = new int[trainingset.size()];
		p_w_coll = new double[trainingset.getAlphabet().size()];
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
		FeatureSequence features = (FeatureSequence) trainingset.get(sampleId).getData();
		double [] coordinates = new double[trainingset.get(sampleId).getAlphabet().size()];
		for (int i = 0; i < features.size(); i++) {
			coordinates[features.getIndexAtPosition(i)]++;
		}
		
		double dd = calculate(query, coordinates);
		//System.out.println(dd + ": to " + LDAUtils.instanceToString(trainingset.get(sampleId)));
		return dd;
	}

}
