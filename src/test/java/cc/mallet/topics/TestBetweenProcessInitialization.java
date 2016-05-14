package cc.mallet.topics;

import static org.junit.Assert.assertEquals;

import java.io.File;

import cc.mallet.configuration.LDAConfiguration;
import cc.mallet.configuration.SimpleLDAConfiguration;
import cc.mallet.topics.ADLDA;
import cc.mallet.topics.SerialCollapsedLDA;
import cc.mallet.topics.UncollapsedParallelLDA;
import cc.mallet.types.InstanceList;
import cc.mallet.util.LDAUtils;
import cc.mallet.util.LoggingUtils;
import cc.mallet.utils.TestUtils;

/**
 * 
 * This test must be run manually twice since the normal since it is 
 * intended to test "between process" initialization
 *
 */
public class TestBetweenProcessInitialization {

	public static void main(String[] args) throws Exception {
		
		int fixedSeed = 4711;
		
		//String [] args = {"--run_cfg=src/main/resources/configuration/TestConfig.cfg"};
		//LDACommandLineParser cp = new LDACommandLineParser(args);
		//LDAConfiguration config = (LDAConfiguration) ConfigFactory.getMainConfiguration(cp);
		Integer numTopics = 20;
		Double alphaSum = 1.0; 
		Double beta = 0.01;
		Integer numIter = 1000;
		Integer numBatches = 6;
		Integer rareWordThreshold = 0;
		Integer showTopicsInterval = 50;
		Integer startDiagnosticOutput = 500;

		SimpleLDAConfiguration config = new SimpleLDAConfiguration(new LoggingUtils(), "ALL",
				numTopics, alphaSum, beta, numIter,
				numBatches, rareWordThreshold, showTopicsInterval,
				startDiagnosticOutput,fixedSeed,"src/main/resources/datasets/nips.txt");

		LoggingUtils lu = new LoggingUtils();
		lu.checkAndCreateCurrentLogDir("Runs");
		config.setLoggingUtil(lu);
		config.activateSubconfig("demo-nips");

		System.out.println("Using Config: " + config.whereAmI());

		String dataset_fn = config.getDatasetFilename();
		System.out.println("Using dataset: " + dataset_fn);

		InstanceList instances = LDAUtils.loadInstances(dataset_fn, 
				"stoplist.txt", config.getRareThreshold(LDAConfiguration.RARE_WORD_THRESHOLD));

		SerialCollapsedLDA collapsed = new SerialCollapsedLDA(numTopics, 
				config.getAlpha(LDAConfiguration.ALPHA_DEFAULT), 
				config.getBeta(LDAConfiguration.BETA_DEFAULT));
		collapsed.setRandomSeed(fixedSeed);
		collapsed.setConfiguration(config);
		collapsed.addInstances(instances);

		UncollapsedParallelLDA uncollapsed 
		= new UncollapsedParallelLDA(config);
		uncollapsed.setRandomSeed(fixedSeed);
		uncollapsed.addInstances(instances);

		ADLDA adlda = new ADLDA(config);
		adlda.setRandomSeed(fixedSeed);
		adlda.addInstances(instances);

		int [][] collapsedTopicIndicators   = collapsed.getTypeTopicCounts();
		int [][] uncollapsedTopicIndicators = uncollapsed.getTypeTopicCounts();
		int [][] adldaTopicIndicators       = adlda.getTypeTopicCounts();
		
		// Save Z indicators between runs...
		String prefix = "TestBetweenProcessInitialization";
		String suffix = "bin";
		String testTmpDir = "src/test/tmp";
		File tmpDir = new File(testTmpDir);
		if(!tmpDir.exists()) {
			tmpDir.mkdir();
		}
		
		String collapsedFn = testTmpDir + "/" + prefix + "-collapsed." + suffix;
		String unCollapsedFn = testTmpDir + "/" + prefix + "-unCollapsed." + suffix;
		String adldaFn = testTmpDir + "/" + prefix + "-ADLDA." + suffix;
		
		String llsFn = testTmpDir + "/lls.bin";
		
		File prevLLs = new File(llsFn);
		File prevZIndsCollapsed = new File(collapsedFn);
		File prevZIndsUnCollapsed = new File(unCollapsedFn);
		File prevZIndsAdldaCollapsed = new File(adldaFn);
		if(prevZIndsCollapsed.exists()) {
			System.out.println("This is the second run, using Z inds from: " + prevZIndsCollapsed.getAbsolutePath());
			int [][] loadedCollapsedTopicIndicators = LDAUtils.readBinaryIntMatrix(collapsedTopicIndicators.length, 
					collapsedTopicIndicators[0].length, collapsedFn);
			int [][] loadedUnCollapsedTopicIndicators = LDAUtils.readBinaryIntMatrix(uncollapsedTopicIndicators.length, 
					collapsedTopicIndicators[0].length, unCollapsedFn);
			int [][] loadedAdldaTopicIndicators = LDAUtils.readBinaryIntMatrix(adldaTopicIndicators.length, 
					collapsedTopicIndicators[0].length, adldaFn);
			
			double [][] lls = LDAUtils.readBinaryDoubleMatrix(1,3,llsFn);
			
			// If we add another 0 to epsilon it will fail against ADLDA for some reason
			// (I.e we get one less decimal place with ADLDA)
			double epsilon = 0.00000001;
			assertEquals("Log likelihoods does not match!", lls[0][0],collapsed.modelLogLikelihood(),epsilon);
			assertEquals("Log likelihoods does not match!", lls[0][1],uncollapsed.modelLogLikelihood(),epsilon);
			assertEquals("Log likelihoods does not match!", lls[0][2],adlda.modelLogLikelihood(),epsilon);
			assertEquals("Log likelihoods does not match!", collapsed.modelLogLikelihood(),adlda.modelLogLikelihood(),epsilon);
			assertEquals("Log likelihoods does not match!", collapsed.modelLogLikelihood(),uncollapsed.modelLogLikelihood(),epsilon);
			
			System.out.println("Log likelihoods:" + lls[0][0] + " =? " + collapsed.modelLogLikelihood());
			System.out.println("Log likelihoods:" + lls[0][1] + " =? " + uncollapsed.modelLogLikelihood());
			System.out.println("Log likelihoods:" + lls[0][2] + " =? " + adlda.modelLogLikelihood());
			
			compareLoaded(collapsedTopicIndicators,uncollapsedTopicIndicators,adldaTopicIndicators,
					loadedCollapsedTopicIndicators,loadedUnCollapsedTopicIndicators,loadedAdldaTopicIndicators);
			
			prevZIndsCollapsed.delete();
			prevZIndsUnCollapsed.delete();
			prevZIndsAdldaCollapsed.delete();
			prevLLs.delete();
			
			System.out.println("\n\n############## SECOND PART OF THE TEST PASSED!! ##############");
			System.out.println("############## NOW THE FULL TEST PASSED!! ##############");
		} else {
			System.out.println("This is the first run, saving Z inds to: " + prevZIndsCollapsed.getAbsolutePath());
			LDAUtils.writeBinaryIntMatrix(collapsedTopicIndicators, 
					collapsedTopicIndicators.length, 
					collapsedTopicIndicators[0].length,
					collapsedFn);
			LDAUtils.writeBinaryIntMatrix(uncollapsedTopicIndicators, 
					uncollapsedTopicIndicators.length, 
					uncollapsedTopicIndicators[0].length,
					unCollapsedFn);
			LDAUtils.writeBinaryIntMatrix(adldaTopicIndicators, 
					adldaTopicIndicators.length, 
					adldaTopicIndicators[0].length,
					adldaFn);
			
			double [][] lls = {{collapsed.modelLogLikelihood(), uncollapsed.modelLogLikelihood(), adlda.modelLogLikelihood()}};
			LDAUtils.writeBinaryDoubleMatrix(lls, 1,3, llsFn);
			
			assertEqualIndicators(collapsed, uncollapsed, adlda, collapsedTopicIndicators, uncollapsedTopicIndicators,
					adldaTopicIndicators);
			
			System.out.println("\n\n############## FIRST PART OF THE TEST PASSED!! ##############");
			System.out.println("############## NOW RUN AGAIN TO TEST SECOND PART!! ##############");
		}
		
		
	}

	private static void compareLoaded(int[][] collapsedTopicIndicators, int[][] uncollapsedTopicIndicators,
			int[][] adldaTopicIndicators, int[][] loadedCollapsedTopicIndicators,
			int[][] loadedUnCollapsedTopicIndicators, int[][] loadedAdldaTopicIndicators) {
		TestUtils.assertEqualArrays(collapsedTopicIndicators, loadedCollapsedTopicIndicators);
		TestUtils.assertEqualArrays(uncollapsedTopicIndicators, loadedUnCollapsedTopicIndicators);
		TestUtils.assertEqualArrays(adldaTopicIndicators, loadedAdldaTopicIndicators);
	}

	static void assertEqualIndicators(SerialCollapsedLDA collapsed, UncollapsedParallelLDA uncollapsed, ADLDA adlda,
			int[][] collapsedTopicIndicators, int[][] uncollapsedTopicIndicators, int[][] adldaTopicIndicators) {
		for (int i = 0; i < uncollapsedTopicIndicators.length; i++) {
			for (int j = 0; j < uncollapsedTopicIndicators[0].length; j++) {
				assertEquals("Collapsed and UnCollapsed are not the same: " 
						+ collapsedTopicIndicators[i][j] + "!=" + uncollapsedTopicIndicators[i][j], 
						collapsedTopicIndicators[i][j], uncollapsedTopicIndicators[i][j]);
				assertEquals("Collapsed and ADLDA are not the same: " 
								+ collapsedTopicIndicators[i][j] + "!=" + adldaTopicIndicators[i][j], 
								collapsedTopicIndicators[i][j], adldaTopicIndicators[i][j]);
			}
		}

		int [] collapsedTokensPerTopic   = collapsed.getTopicTotals();
		int [] uncollapsedTokensPerTopic = uncollapsed.getTopicTotals();
		int [] adldaTokensPerTopic      = adlda.getTopicTotals();

		for (int i = 0; i < collapsedTokensPerTopic.length; i++) {
			assertEquals("Collapsed and ADLA token counts are not the same: " 
					+ collapsedTokensPerTopic[i] + "!=" + adldaTokensPerTopic[i], 
					collapsedTokensPerTopic[i], adldaTokensPerTopic[i]);
			assertEquals("Collapsed and UnCollapsed token counts are not the same: " 
					+ collapsedTokensPerTopic[i] + "!=" + uncollapsedTokensPerTopic[i], 
					collapsedTokensPerTopic[i], uncollapsedTokensPerTopic[i]);
		}

		double collapsedModelLogLikelihood   = collapsed.modelLogLikelihood();
		double uncollapsedModelLogLikelihood = uncollapsed.modelLogLikelihood();
		double adldaModelLogLikelihood       = adlda.modelLogLikelihood();

		assertEquals("ADLDA and Collapsed LogLikelihoods are not the same: " 
				+ adldaModelLogLikelihood + " != " + collapsedModelLogLikelihood 
				+ " Diff: " + (adldaModelLogLikelihood-collapsedModelLogLikelihood) ,
				adldaModelLogLikelihood,	collapsedModelLogLikelihood, 0.01);

		assertEquals("Collapsed and UnCollapsed LogLikelihoods are not the same: " 
				+ collapsedModelLogLikelihood + " != " + uncollapsedModelLogLikelihood 
				+ " Diff: " + (collapsedModelLogLikelihood-uncollapsedModelLogLikelihood) ,
				collapsedModelLogLikelihood,	uncollapsedModelLogLikelihood, 0.01);

		int [][] ttcountsColl = collapsed.getTypeTopicCounts();
		int [][] ttcountsUnColl = uncollapsed.getTypeTopicCounts();

		TestUtils.assertEqualArrays(ttcountsColl, ttcountsUnColl);
	}
}	
