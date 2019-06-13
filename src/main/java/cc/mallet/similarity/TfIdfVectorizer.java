package cc.mallet.similarity;

import cc.mallet.pipe.TfIdfPipe;
import cc.mallet.types.Alphabet;
import cc.mallet.types.FeatureSequence;
import cc.mallet.types.Instance;
import cc.mallet.util.ArrayStringUtils;

public class TfIdfVectorizer implements Vectorizer {
	
	TfIdfPipe tp;
	
	public TfIdfVectorizer(TfIdfPipe tp) {
		this.tp = tp;
	}
	
	@Override
	public double[] instanceToVector(Instance instance) {
		FeatureSequence trainTokenSeq = (FeatureSequence) instance.getData();
		int [] tokenSequence = trainTokenSeq.getFeatures();
		double [] coordinates = new double[instance.getAlphabet().size()];
		for (int i = 0; i < tokenSequence.length; i++) {
			coordinates[tokenSequence[i]] = tp.getTfIdf().get(tokenSequence[i]);
		}
		return coordinates;
	}
	
	public String toAnnotatedString(Instance instance) {
		double [] arr = instanceToVector(instance);
		Alphabet alphabet = instance.getAlphabet();
		
		String res = "";
		res += "[" +  arr.length + "]:";
		for (int j = 0; j < arr.length; j++) {
			if(arr[j]<0.00005) {
				res += "";
			} else {				
				String word = (String) alphabet.lookupObject(j);
				res += "(" + word + "):" + j + ":" + ArrayStringUtils.formatDouble(arr[j]) + ", ";
			}
		}
		return res;
	}


}
