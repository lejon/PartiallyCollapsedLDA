package cc.mallet.util;

import gnu.trove.TIntHashSet;
import cc.mallet.types.FeatureSequence;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;

public class PerplexityDatasetBuilder {
	
	public static InstanceList [] buildPerplexityDataset(InstanceList instances, int noFolds) {
		InstanceList.CrossValidationIterator cviter = instances.crossValidationIterator(noFolds);
		InstanceList [] tSets = cviter.nextSplit();
		InstanceList trainingSet = tSets[0];
		InstanceList fullTestSet = tSets[1];
		InstanceList testDocs = new InstanceList(trainingSet.getDataAlphabet(),trainingSet.getTargetAlphabet());
		
		// For the perplexity calculations we want to split each "test document" into two parts where half the document 
		// goes into the training set and the other into the test set (!)
		
		for(Instance testDoc : fullTestSet) {
			FeatureSequence tokens = (FeatureSequence) testDoc.getData();
			
			// Select half of the words in the document to use in the test set
			int noWordsToSample = tokens.size() / 2;
			IndexSampler is = new WithoutReplacementSampler(0,tokens.size());
			TIntHashSet inds = new TIntHashSet();
			int [] testfeatures = new int[noWordsToSample];
			for (int i = 0; i < noWordsToSample; i++) {
				int idx = is.nextSample();
				testfeatures[i] = tokens.getIndexAtPosition(idx);
				inds.add(idx);
			}
			FeatureSequence testFs = new FeatureSequence(testDoc.getAlphabet(),testfeatures);
			Instance testPart = new Instance(testFs,testDoc.getTarget(), testDoc.getName(), testDoc.getSource());
			testDocs.add(testPart);
	
			// Select the other half of the test document to put back in the training set
			int noWordsLeft = tokens.size()-noWordsToSample;
			int [] trainfeatures = new int[noWordsLeft];
			int added = 0;
			for (int i = 0; i < tokens.size(); i++) {
				if(!inds.contains(i)) {
					trainfeatures[added++] = tokens.getIndexAtPosition(i);
				}
			}
			FeatureSequence trainFs = new FeatureSequence(testDoc.getAlphabet(),trainfeatures);
			Instance trainPart = new Instance(trainFs,testDoc.getTarget(), testDoc.getName(), testDoc.getSource());
			// Now add this half to the training set
			trainingSet.add(trainPart);
		}
		
		InstanceList [] result = new InstanceList[2];
		result[0] = trainingSet;		
		result[1] = testDocs;
		
		return result;
	}

}
