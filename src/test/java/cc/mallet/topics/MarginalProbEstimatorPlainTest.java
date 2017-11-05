package cc.mallet.topics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;

import cc.mallet.configuration.LDAConfiguration;
import cc.mallet.configuration.SimpleLDAConfiguration;
import cc.mallet.types.InstanceList;
import cc.mallet.util.LDAUtils;
import cc.mallet.util.LoggingUtils;
import cc.mallet.util.PerplexityDatasetBuilder;

public class MarginalProbEstimatorPlainTest {

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
	public void testMarginalProbabilityEstimatorPlainWithPerplexityDatasetBuilder() throws IOException {
		String dataset_fn = "src/main/resources/datasets/nips.txt";

		String whichModel = "uncollapsed_paranoid";
		Integer numBatches = 6;
		Integer numIter = 100;

		LDAConfiguration config = getStdCfg(whichModel, numIter, numBatches);

		InstanceList originalInstances = LDAUtils.loadInstances(dataset_fn, 
				"stoplist.txt", config.getRareThreshold(LDAConfiguration.RARE_WORD_THRESHOLD));

		InstanceList [] datasets = PerplexityDatasetBuilder.buildPerplexityDataset(originalInstances, 5);
		if(datasets[0]==null || datasets[1] == null) {
			throw new IllegalStateException("Could not build proper datasets");
		}

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

		double [] symmetricAlpha;
		int numParticles = 100;
		MarginalProbEstimatorPlain evaluator = null;
		int numTopics = model.getNoTopics();
		double alphaSum = config.getAlpha(0.1);
		double beta = config.getBeta(0.01);
		symmetricAlpha = new double[numTopics];
		for (int i = 0; i < symmetricAlpha.length; i++) {
			symmetricAlpha[i] = alphaSum / numTopics;
		}

		evaluator = new MarginalProbEstimatorPlain(numTopics,
				symmetricAlpha, alphaSum,
				beta,
				model.getTypeTopicCounts(), 
				model.getTopicTotals());

		double result = evaluator.evaluateLeftToRight(testSet, numParticles, null);
		double adldaResult = runADLDAMarginalProbabilityEstimatorPlainWithPerplexityDatasetBuilder(dataset_fn,config);
		double threshold = 0.1;
		
		assertTrue("Result is not within " + (threshold * 100) + "% of ADLDA: " + result + "<=>" + adldaResult, 
				result>(adldaResult + (adldaResult*threshold)) && result<(adldaResult - (adldaResult*threshold)));
	}


	public double runADLDAMarginalProbabilityEstimatorPlainWithPerplexityDatasetBuilder(
			String dataset_fn, LDAConfiguration config) throws IOException {
		System.out.println("Running ADLDA for comparison...");
		InstanceList originalInstances = LDAUtils.loadInstances(dataset_fn, 
				"stoplist.txt", config.getRareThreshold(LDAConfiguration.RARE_WORD_THRESHOLD));

		InstanceList [] datasets = PerplexityDatasetBuilder.buildPerplexityDataset(originalInstances, 5);
		if(datasets[0]==null || datasets[1] == null) {
			throw new IllegalStateException("Could not build proper datasets");
		}

		System.out.println("Using dataset: " + dataset_fn);
		LoggingUtils lu = new LoggingUtils();
		lu.checkAndCreateCurrentLogDir("TestRuns");
		config.setLoggingUtil(lu);

		InstanceList trainingSet = datasets[0];
		assertEquals(originalInstances.size(),  trainingSet.size());
		InstanceList testSet = datasets[1];

		assertTrue(originalInstances.size() > testSet.size());

		ADLDA model = new ADLDA(config);
		model.addInstances(trainingSet);
		model.sample(config.getNoIterations(LDAConfiguration.NO_ITER_DEFAULT));

		double [] symmetricAlpha;
		int numParticles = 100;
		boolean usingResampling = false;
		MarginalProbEstimator evaluator = null;
		int numTopics = model.getNoTopics();
		double alphaSum = config.getAlpha(0.1);
		double beta = config.getBeta(0.01);
		symmetricAlpha = new double[numTopics];
		for (int i = 0; i < symmetricAlpha.length; i++) {
			symmetricAlpha[i] = alphaSum / numTopics;
		}

		evaluator = new MarginalProbEstimator(model.getNoTopics(),
				symmetricAlpha, alphaSum,
				beta,
				model.typeTopicCounts, 
				model.getTopicTotals());

		return evaluator.evaluateLeftToRight(testSet, numParticles, 
				usingResampling,
				null);
	}

}
