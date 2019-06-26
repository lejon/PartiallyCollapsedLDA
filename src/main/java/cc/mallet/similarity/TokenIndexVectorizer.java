package cc.mallet.similarity;

import cc.mallet.types.Alphabet;
import cc.mallet.types.FeatureSequence;
import cc.mallet.types.Instance;
import cc.mallet.util.ArrayStringUtils;

public class TokenIndexVectorizer implements Vectorizer {
		
	@Override
	public double[] instanceToVector(Instance instance) {
		FeatureSequence features = (FeatureSequence) instance.getData();
		double [] coordinates = new double[features.size()];
		
		for(int i = 0; i < features.size(); i++) {
			coordinates[i] = features.getIndexAtPosition(i);
		}

		return coordinates;
	}

	@Override
	public int[] instanceToIntVector(Instance instance) {
		FeatureSequence features = (FeatureSequence) instance.getData();
		int [] coordinates = new int[features.size()];
		
		for(int i = 0; i < features.size(); i++) {
			coordinates[i] = features.getIndexAtPosition(i);
		}

		return coordinates;
	}

	public String toAnnotatedString(Instance instance) {
		double [] arr = instanceToVector(instance);
		Alphabet alphabet = instance.getAlphabet();
		
		String res = "";
		res += "[" +  arr.length + "]:";
		for (int j = 0; j < arr.length; j++) {
			String word = (String) alphabet.lookupObject((int)arr[j]);
			res += "(" + word + "):" + j + ":" + ArrayStringUtils.formatDouble(arr[j]) + ", ";
		}
		return res;
	}


}
