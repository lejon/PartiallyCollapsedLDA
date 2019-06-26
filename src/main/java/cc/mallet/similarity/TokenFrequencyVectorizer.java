package cc.mallet.similarity;

import cc.mallet.types.Alphabet;
import cc.mallet.types.FeatureSequence;
import cc.mallet.types.Instance;
import cc.mallet.util.ArrayStringUtils;

public class TokenFrequencyVectorizer implements Vectorizer {
		
	@Override
	public double[] instanceToVector(Instance instance) {
		FeatureSequence features = (FeatureSequence) instance.getData();
		double [] coordinates = new double[instance.getAlphabet().size()];
		for (int i = 0; i < features.size(); i++) {
			coordinates[features.getIndexAtPosition(i)]++;
		}
		return coordinates;
	}

	@Override
	public int[] instanceToIntVector(Instance instance) {
		FeatureSequence features = (FeatureSequence) instance.getData();
		int [] coordinates = new int[instance.getAlphabet().size()];
		for (int i = 0; i < features.size(); i++) {
			coordinates[features.getIndexAtPosition(i)]++;
		}
		return coordinates;
	}

	
	public String toAnnotatedString(Instance instance) {
		double [] arr = instanceToVector(instance);
		Alphabet alphabet = instance.getAlphabet();
		
		String res = "";
		int nonZero = 0;
		for (int j = 0; j < arr.length; j++) {
			if(arr[j]<0.00005) {
				res += "";
			} else {				
				String word = (String) alphabet.lookupObject(j);
				res += "(" + word + "):" + j + ":" + ArrayStringUtils.formatDouble(arr[j]) + ", ";
				nonZero++;
			}
		}
		return "[" +  nonZero + "]:" + res;
	}


}
