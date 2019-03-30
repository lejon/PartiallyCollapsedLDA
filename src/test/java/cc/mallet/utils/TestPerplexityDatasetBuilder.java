package cc.mallet.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import cc.mallet.configuration.LDAConfiguration;
import cc.mallet.configuration.SimpleLDAConfiguration;
import cc.mallet.topics.ParanoidUncollapsedParallelLDA;
import cc.mallet.topics.TopicAssignment;
import cc.mallet.types.Alphabet;
import cc.mallet.types.FeatureSequence;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.util.LDAUtils;
import cc.mallet.util.LoggingUtils;
import cc.mallet.util.PerplexityDatasetBuilder;

public class TestPerplexityDatasetBuilder {
	
	SimpleLDAConfiguration getStdCfg(String whichModel, Integer numIter, Integer numBatches) {
		Integer numTopics = 20;
		Double alphaSum = 1.0; 
		Double beta = 0.01;
		Integer rareWordThreshold = 0;
		Integer showTopicsInterval = 50;
		Integer startDiagnosticOutput = 500;

		SimpleLDAConfiguration config = new SimpleLDAConfiguration(new LoggingUtils(), whichModel,
				numTopics, alphaSum, beta, numIter,
				numBatches, rareWordThreshold, showTopicsInterval,
				startDiagnosticOutput,4711,"src/main/resources/datasets/nips.txt");
		
		return config;
	}

	@Test
	public void testSmall() throws UnsupportedEncodingException, FileNotFoundException {
		String dataset_fn = "src/main/resources/datasets/small.txt";
		//InstanceList originalInstances = LDAUtils.loadInstances(dataset_fn,	"stoplist.txt", 1);
		InstanceList originalInstances = LDAUtils.loadInstances(dataset_fn,	null, 1);
		InstanceList [] datasets = PerplexityDatasetBuilder.buildPerplexityDataset(originalInstances, 2);
		
		System.out.println("Original:");
		System.out.println(LDAUtils.instancesToString(originalInstances));
		assertEquals(10,originalInstances.size()); // The small set contains 4 documents
		System.out.println("Training set:");
		InstanceList trainingSet = datasets[0];
		assertEquals(originalInstances.size(), trainingSet.size()); // 4 total => 2 train and 2 test
		System.out.println(LDAUtils.instancesToString(trainingSet));
		System.out.println("Test:");
		InstanceList testSet = datasets[1];
		assertEquals(5, testSet.size()); 
		System.out.println(LDAUtils.instancesToString(testSet));
		
		System.out.println("Final Training set:");
		System.out.println(LDAUtils.instancesToString(trainingSet));
		assertEquals(originalInstances.size(), trainingSet.size());

		System.out.println("Final Test set:");
		System.out.println(LDAUtils.instancesToString(testSet));
	}

	@Test
	public void testNips() throws UnsupportedEncodingException, FileNotFoundException {
		String dataset_fn = "src/main/resources/datasets/nips.txt";
		InstanceList originalInstances = LDAUtils.loadInstances(dataset_fn, "stoplist.txt", 0);
		
		InstanceList [] datasets = PerplexityDatasetBuilder.buildPerplexityDataset(originalInstances, 5);
		
		InstanceList trainingSet = datasets[0];
		assertEquals(originalInstances.size(),  trainingSet.size());
		InstanceList testSet = datasets[1];
				
		assertTrue(originalInstances.size() > testSet.size());
		
//		System.out.println("Orig dataset is (" + originalInstances.size() + "):\n" 
//				+ LDAUtils.instancesToString(originalInstances, 1));
//		System.out.println("Test dataset is (" + testSet.size() + "):\n" 
//				+ LDAUtils.instancesToString(testSet, 1));
//		System.out.println("Training dataset is (" + trainingSet.size() + "):\n" 
//				+ LDAUtils.instancesToString(trainingSet, 1));
		
		int testDocIdx = originalInstances.size()-testSet.size();
		System.out.println("Test docs start at:" + testDocIdx);
		System.out.println("Test set size:" + testSet.size());
		System.out.println("Training set size:" + trainingSet.size());
		
		// Ensure that the training set + corresponding test doc is the same as the 
		// doc in the original data set (but don't care about word order, since we
		// select the words randomly)
		for(Instance testInst : testSet) {
			//System.out.println("Looking at: " + testDocIdx);
//			System.out.println("Test Instance is: " + instanceToString(testInst));
//			System.out.println("Train Instance is: " + instanceToString(trainingSet.get(testDocIdx)));
//			System.out.println("Orig Instance is: " + instanceToString(originalInstances.get(testDocIdx)));
			Set<String> testInstance = instanceToStringSet(testInst); 
			Set<String> trainingInstance = instanceToStringSet(trainingSet.get(testDocIdx));
			assertTrue(testInstance.addAll(trainingInstance));
			assertTrue(canFind(testInstance,originalInstances));
			testDocIdx++;
		}
	}

	@Test
	public void testNipsWithSampling() throws IOException {
		String dataset_fn = "src/main/resources/datasets/nips.txt";
		
		String whichModel = "uncollapsed_paranoid";
		Integer numBatches = 6;
		Integer numIter = 100;

		LDAConfiguration config = getStdCfg(whichModel, numIter, numBatches);
		
		InstanceList originalInstances = LDAUtils.loadInstances(dataset_fn, 
				"stoplist.txt", config.getRareThreshold(LDAConfiguration.RARE_WORD_THRESHOLD));
		
		InstanceList [] datasets = PerplexityDatasetBuilder.buildPerplexityDataset(originalInstances, 5);
		
		System.out.println("Using dataset: " + dataset_fn);
		System.out.println("Scheme: " + whichModel);
		LoggingUtils lu = new LoggingUtils();
		lu.checkAndCreateCurrentLogDir("TestRuns");
		config.setLoggingUtil(lu);
		
		InstanceList trainingSet = datasets[0];
		assertEquals(originalInstances.size(),  trainingSet.size());
		InstanceList testSet = datasets[1];
				
		assertTrue(originalInstances.size() > testSet.size());
		
		ParanoidUncollapsedParallelLDA model = new ParanoidUncollapsedParallelLDA(config);
		model.addInstances(trainingSet);
		model.sample(config.getNoIterations(LDAConfiguration.NO_ITER_DEFAULT));
		
		ArrayList<TopicAssignment> dataset = model.getData();
		
		int testDocIdx = originalInstances.size()-testSet.size();
		System.out.println("Test docs start at:" + testDocIdx);
		System.out.println("Test set size:" + testSet.size());
		System.out.println("Training set size:" + trainingSet.size());
		
		// Ensure that the training set + corresponding test doc is the same as the 
		// doc in the original data set (but don't care about word order, since we
		// select the words randomly)
		for(Instance testInst : testSet) {
			Set<String> testInstance = instanceToStringSet(testInst); 
			Set<String> trainingInstance = instanceToStringSet(dataset.get(testDocIdx).instance);
			assertTrue(testInstance.addAll(trainingInstance));
			assertTrue(canFind(testInstance,originalInstances));
			testDocIdx++;
		}
	}

	
	/**
	 * Since the document order is shuffled in the training set relative the original data set
	 * we must search the whole data set for each instance 
	 * @param testInstance
	 * @param originalInstances
	 * @return
	 */
	private boolean canFind(Set<String> testInstance,InstanceList originalInstances) {
		for(Instance inst : originalInstances) {
			if(testInstance.equals(instanceToStringSet(inst))) {
				return true;
			}
		}
		return false;
	}

	private Set<String> instanceToStringSet(Instance trainInst) {
		Set<String> result = new HashSet<String>();
		Alphabet alphabet = trainInst.getAlphabet();
		FeatureSequence features = (FeatureSequence) trainInst.getData();
		for (int i = 0; i < features.size(); i++) {
			result.add((String)alphabet.lookupObject(features.getIndexAtPosition(i)));
		}
		return result;
	}

}
