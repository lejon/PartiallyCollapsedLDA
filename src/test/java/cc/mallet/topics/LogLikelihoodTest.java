package cc.mallet.topics;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;

import cc.mallet.configuration.LDAConfiguration;
import cc.mallet.configuration.SimpleLDAConfiguration;
import cc.mallet.topics.SerialCollapsedLDA;
import cc.mallet.topics.UncollapsedParallelLDA;
import cc.mallet.types.InstanceList;
import cc.mallet.util.LDAUtils;
import cc.mallet.util.LoggingUtils;
import cc.mallet.utils.TestUtils;

public class LogLikelihoodTest {
	
	double epsilon = 0.000_000_000_000_000_000_000_000_000_000_001;
	
	@Test
	public void testLogLikelihood() throws IOException {
		String whichModel = "uncollapsed";
		Integer numTopics = 20;
		Double alphaSum = 1.0; 
		Double beta = 0.01;
		Integer numIter = 1000;
		Integer numBatches = 6;
		Integer rareWordThreshold = 0;
		Integer showTopicsInterval = 50;
		Integer startDiagnosticOutput = 500;

		SimpleLDAConfiguration config = new SimpleLDAConfiguration(new LoggingUtils(), whichModel,
				numTopics, alphaSum, beta, numIter,
				numBatches, rareWordThreshold, showTopicsInterval,
				startDiagnosticOutput,4711,"src/main/resources/datasets/nips.txt");

		LoggingUtils lu = new LoggingUtils();
		lu.checkAndCreateCurrentLogDir("Runs");
		config.setLoggingUtil(lu);
		config.activateSubconfig("demo-nips");

		System.out.println("Using Config: " + config.whereAmI());

		String dataset_fn = config.getDatasetFilename();
		System.out.println("Using dataset: " + dataset_fn);
		System.out.println("Scheme: " + whichModel);

		InstanceList instances = LDAUtils.loadInstances(dataset_fn, 
				"stoplist.txt", config.getRareThreshold(LDAConfiguration.RARE_WORD_THRESHOLD));

		SerialCollapsedLDA collapsed = new SerialCollapsedLDA(numTopics, 
				config.getAlpha(LDAConfiguration.ALPHA_DEFAULT), 
				config.getBeta(LDAConfiguration.BETA_DEFAULT));
		collapsed.setRandomSeed(config.getSeed(LDAConfiguration.SEED_DEFAULT));
		collapsed.setConfiguration(config);
		collapsed.addInstances(instances);

		UncollapsedParallelLDA uncollapsed = new UncollapsedParallelLDA(config);
		uncollapsed.setRandomSeed(config.getSeed(LDAConfiguration.SEED_DEFAULT));
		uncollapsed.addInstances(instances);
		
		TestUtils.assertEqualArrays(collapsed.getTypeTopicCounts(), uncollapsed.getTypeTopicCounts());
		org.junit.Assert.assertArrayEquals(collapsed.getTopicTotals(), uncollapsed.getTopicTotals());
		
		System.out.println("TTCounts 1 ok!");

		double collapsedModelLogLikelihood   = collapsed.modelLogLikelihood();
		double uncollapsedModelLogLikelihood = uncollapsed.modelLogLikelihood();

		TestUtils.assertEqualArrays(collapsed.getTypeTopicCounts(), uncollapsed.getTypeTopicCounts());
		org.junit.Assert.assertArrayEquals(collapsed.getTopicTotals(), uncollapsed.getTopicTotals());

		System.out.println("TTCounts 2 ok!");
		
		assertEquals("Collapsed and UnCollapsed LogLikelihoods are not the same: " 
				+ collapsedModelLogLikelihood + " != " + uncollapsedModelLogLikelihood 
				+ " Diff: " + (collapsedModelLogLikelihood-uncollapsedModelLogLikelihood) ,
				collapsedModelLogLikelihood,	uncollapsedModelLogLikelihood, epsilon);

		System.out.println("Precheck ok!");

		// sample 100 iterations just for the sake of doing getting to a
		// something other than the start state 
		collapsed.sample(50);
		
		System.out.println("Finished sampling!");

		int[][] collapsedZas = collapsed.getZIndicators();
		uncollapsed.setZIndicators(collapsedZas);

		int [][] setZas = uncollapsed.getZIndicators();

		TestUtils.assertEqualArrays(collapsedZas, setZas);
		TestUtils.assertEqualArrays(collapsed.getTypeTopicCounts(), uncollapsed.getTypeTopicCounts());
		org.junit.Assert.assertArrayEquals(collapsed.getTopicTotals(), uncollapsed.getTopicTotals());
		
		// Sample 5 iterations to change z
		uncollapsed.sample(5);
		
		int[][] uncollapsedZas = uncollapsed.getZIndicators();
		collapsed.setZIndicators(uncollapsedZas);
		int [][] setZascollapsed = collapsed.getZIndicators();
		
		TestUtils.assertEqualArrays(uncollapsedZas, setZascollapsed);

		System.out.println("Z indicators are equal...");
		
		//printTTCounts(collapsed.getTypeTopicCounts(),uncollapsed.getTypeTopicCounts());
		
		TestUtils.assertEqualArrays(collapsed.getTypeTopicCounts(), uncollapsed.getTypeTopicCounts());
		org.junit.Assert.assertArrayEquals(collapsed.getTopicTotals(), uncollapsed.getTopicTotals());

		collapsedModelLogLikelihood   = collapsed.modelLogLikelihood();
		uncollapsedModelLogLikelihood = uncollapsed.modelLogLikelihood();

		assertEquals("Collapsed and UnCollapsed LogLikelihoods are not the same: " 
				+ collapsedModelLogLikelihood + " != " + uncollapsedModelLogLikelihood 
				+ " Diff: " + (collapsedModelLogLikelihood-uncollapsedModelLogLikelihood) ,
				collapsedModelLogLikelihood,	uncollapsedModelLogLikelihood, epsilon);
	}
}
