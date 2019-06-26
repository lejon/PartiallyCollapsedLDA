package cc.mallet.similarity;

import cc.mallet.types.Alphabet;
import cc.mallet.types.FeatureSequence;
import cc.mallet.types.Instance;

public class TokenOccurenceVectorizer implements Vectorizer {
		
	@Override
	public double[] instanceToVector(Instance instance) {
		FeatureSequence features = (FeatureSequence) instance.getData();
		double [] coordinates = new double[instance.getAlphabet().size()];
		for (int i = 0; i < features.size(); i++) {
			coordinates[features.getIndexAtPosition(i)] = 1;
		}
		return coordinates;
	}

	@Override
	public int[] instanceToIntVector(Instance instance) {
		FeatureSequence features = (FeatureSequence) instance.getData();
		int [] coordinates = new int[instance.getAlphabet().size()];
		for (int i = 0; i < features.size(); i++) {
			coordinates[features.getIndexAtPosition(i)] = 1;
		}
		return coordinates;
	}
	
	public String toAnnotatedString(Instance instance) {
		FeatureSequence features = (FeatureSequence) instance.getData();
		int nrWords = features.size();
		Alphabet alphabet = instance.getAlphabet();
		String res = "";
		res += "[" +  nrWords + "]:";
		for (int j = 0; j < nrWords; j++) {
			String word = (String) alphabet.lookupObject(features.getIndexAtPosition(j));
			res += "(" + word + "):" + j + ":1";
		}
		return res;
	}

}
