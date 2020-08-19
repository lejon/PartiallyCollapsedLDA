package cc.mallet.topics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;

import org.junit.Assert;
import org.junit.Test;

import cc.mallet.configuration.LDAConfiguration;
import cc.mallet.configuration.SimpleLDAConfiguration;
import cc.mallet.types.InstanceList;
import cc.mallet.types.MatrixOps;
import cc.mallet.util.LDALoggingUtils;
import cc.mallet.util.LDAUtils;
import cc.mallet.util.LoggingUtils;

public class SpaliasUncollapsedTestPhiPriors {
	
	
	SimpleLDAConfiguration getStdCfg(String whichModel, Integer numIter, Integer numBatches) {
		Integer numTopics = 20;
		Double alpha = 0.1; 
		Double beta = 0.01;
		Integer rareWordThreshold = 0;
		Integer showTopicsInterval = 10;
		Integer startDiagnosticOutput = 0;

		SimpleLDAConfiguration config = new SimpleLDAConfiguration(new LoggingUtils(), whichModel,
				numTopics, alpha, beta, numIter,
				numBatches, rareWordThreshold, showTopicsInterval,
				startDiagnosticOutput,4711,"src/main/resources/datasets/nips.txt");
		
		return config;
	}
	
	@Test
	public void smokeTest() throws IOException {	
		String whichModel = "spalias_uncollapsed";
		Integer numBatches = 6;

		Integer numIter = 100;
		SimpleLDAConfiguration config = getStdCfg(whichModel, numIter, numBatches);

		String dataset_fn = config.getDatasetFilename();
		System.out.println("Using dataset: " + dataset_fn);
		System.out.println("Scheme: " + whichModel);
		LDALoggingUtils lu = new LoggingUtils();
		lu.checkAndCreateCurrentLogDir("TestRuns");
		config.setLoggingUtil(lu);

		InstanceList instances = LDAUtils.loadInstances(dataset_fn, 
				"stoplist.txt", config.getRareThreshold(LDAConfiguration.RARE_WORD_THRESHOLD));

		LDAGibbsSampler model = new SpaliasUncollapsedParallelLDA(config);
		System.out.println(
				String.format("Spalias Uncollapsed Parallell LDA (%d batches).", 
						config.getNoBatches(LDAConfiguration.NO_BATCHES_DEFAULT)));

		System.out.println("Vocabulary size: " + instances.getDataAlphabet().size() + "\n");
		System.out.println("Instance list is: " + instances.size());
		System.out.println("Loading data instances...");

		model.setRandomSeed(config.getSeed(LDAConfiguration.SEED_DEFAULT));
		model.addInstances(instances);

		System.out.println("Starting iterations (" + config.getNoIterations(LDAConfiguration.NO_ITER_DEFAULT) + " total).");
		System.out.println("_____________________________\n");

		// Runs the model
		System.out.println("Starting:" + new Date());
		model.sample(config.getNoIterations(LDAConfiguration.NO_ITER_DEFAULT));
		System.out.println("Finished:" + new Date());

		System.out.println("I am done!");
	}
	
	@Test
	public void sampleZGivenPhi() throws IOException {	
		String whichModel = "spalias_uncollapsed";
		Integer numBatches = 6;

		Integer numIter = 100;
		SimpleLDAConfiguration config = getStdCfg(whichModel, numIter,
				numBatches);

		String dataset_fn = config.getDatasetFilename();
		System.out.println("Using dataset: " + dataset_fn);
		System.out.println("Scheme: " + whichModel);
		LDALoggingUtils lu = new LoggingUtils();
		lu.checkAndCreateCurrentLogDir("TestRuns");
		config.setLoggingUtil(lu);

		InstanceList instances = LDAUtils.loadInstances(dataset_fn, 
				"stoplist.txt", config.getRareThreshold(LDAConfiguration.RARE_WORD_THRESHOLD));

		SpaliasUncollapsedParallelLDA model = new SpaliasUncollapsedParallelLDA(config);
		System.out.println(
				String.format("Spalias Uncollapsed Parallell LDA (%d batches).", 
						config.getNoBatches(LDAConfiguration.NO_BATCHES_DEFAULT)));

		System.out.println("Vocabulary size: " + instances.getDataAlphabet().size() + "\n");
		System.out.println("Instance list is: " + instances.size());
		System.out.println("Loading data instances...");

		model.setRandomSeed(config.getSeed(LDAConfiguration.SEED_DEFAULT));
		model.addInstances(instances);

		System.out.println("Starting iterations (" + config.getNoIterations(LDAConfiguration.NO_ITER_DEFAULT) + " total).");
		System.out.println("_____________________________\n");

		// Runs the model
		model.sample(config.getNoIterations(LDAConfiguration.NO_ITER_DEFAULT));
		
		double [][] phiBefore = clone(model.getPhi());
		
		model.sampleZGivenPhi(config.getNoIterations(LDAConfiguration.NO_ITER_DEFAULT));
		
		double [][] phiAfter = clone(model.getPhi());
		
		for (int i = 0; i < phiBefore.length; i++) {			
			Assert.assertArrayEquals(phiBefore[i], phiAfter[i], 0.00000001);
		}
		
	}
	
	public static double [][] clone(double [][] arr1) {
		double [][] result = new double [arr1.length][];
		
		for (int i = 0; i < arr1.length; i++) {
			result[i] = arr1[i].clone();
		}
		return result;
	}
	
	@Test
	public void testSetPriorsNoPriors() throws IOException {	
		String whichModel = "spalias_uncollapsed";
		Integer numBatches = 6;

		Integer numIter = 100;
		SimpleLDAConfiguration config = getStdCfg(whichModel, numIter,numBatches);
		config.setDatasetFilename("src/main/resources/datasets/SmallTexts.txt");
		config.setNoTopics(4);

		String dataset_fn = config.getDatasetFilename();
		System.out.println("Using dataset: " + dataset_fn);
		System.out.println("Scheme: " + whichModel);
		LDALoggingUtils lu = new LoggingUtils();
		lu.checkAndCreateCurrentLogDir("TestRuns");
		config.setLoggingUtil(lu);

		InstanceList instances = LDAUtils.loadInstances(dataset_fn, 
				"stoplist.txt", config.getRareThreshold(LDAConfiguration.RARE_WORD_THRESHOLD));

		SpaliasUncollapsedParallelWithPriors model = new SpaliasUncollapsedParallelWithPriors(config);
		System.out.println(
				String.format("Spalias Uncollapsed Parallell LDA (%d batches).", 
						config.getNoBatches(LDAConfiguration.NO_BATCHES_DEFAULT)));

		System.out.println("Vocabulary size: " + instances.getDataAlphabet().size() + "\n");
		System.out.println("Instance list is: " + instances.size());
		System.out.println("Loading data instances...");

		model.setRandomSeed(config.getSeed(LDAConfiguration.SEED_DEFAULT));
		model.addInstances(instances);
		
		// Assert that ALL priors == 1.0
		double [][] topicPriors = model.getTopicPriors();
		//MatrixOps.print(topicPriors);
		for (int i = 0; i < topicPriors.length; i++) {
			for (int j = 0; j < topicPriors[i].length; j++) {
				assertEquals(1.0, topicPriors[i][j], 0.0000000001);
			}
		}
		
		System.out.println("Starting iterations (" + config.getNoIterations(LDAConfiguration.NO_ITER_DEFAULT) + " total).");
		System.out.println("_____________________________\n");

		// Runs the model
		System.out.println("Starting:" + new Date());
		model.sample(config.getNoIterations(LDAConfiguration.NO_ITER_DEFAULT));
		System.out.println("Finished:" + new Date());

		System.out.println("I am done!");
	}
	
	@Test
	public void testSetPriorsNoWordsIsInDictionary() throws IOException {	
		String whichModel = "spalias_uncollapsed";
		Integer numBatches = 6;

		Integer numIter = 100;
		SimpleLDAConfiguration config = getStdCfg(whichModel, numIter,numBatches);
		config.setDatasetFilename("src/main/resources/datasets/SmallTexts.txt");
		config.setNoTopics(4);
		config.setTopicPriorFilename("src/test/resources/topic_priors.txt");

		String dataset_fn = config.getDatasetFilename();
		System.out.println("Using dataset: " + dataset_fn);
		System.out.println("Scheme: " + whichModel);
		LDALoggingUtils lu = new LoggingUtils();
		lu.checkAndCreateCurrentLogDir("TestRuns");
		config.setLoggingUtil(lu);

		InstanceList instances = LDAUtils.loadInstances(dataset_fn, 
				"stoplist.txt", config.getRareThreshold(LDAConfiguration.RARE_WORD_THRESHOLD));

		SpaliasUncollapsedParallelWithPriors model = new SpaliasUncollapsedParallelWithPriors(config);
		System.out.println(
				String.format("Spalias Uncollapsed Parallell LDA (%d batches).", 
						config.getNoBatches(LDAConfiguration.NO_BATCHES_DEFAULT)));

		System.out.println("Vocabulary size: " + instances.getDataAlphabet().size() + "\n");
		System.out.println("Instance list is: " + instances.size());
		System.out.println("Loading data instances...");

		model.setRandomSeed(config.getSeed(LDAConfiguration.SEED_DEFAULT));
		model.addInstances(instances);
		
		// Assert that ALL priors == 1.0
		double [][] topicPriors = model.getTopicPriors();
		//MatrixOps.print(topicPriors);
		for (int i = 0; i < topicPriors.length; i++) {
			for (int j = 0; j < topicPriors[i].length; j++) {
				assertEquals(1.0, topicPriors[i][j], 0.0000000001);
			}
		}
		
		System.out.println("Starting iterations (" + config.getNoIterations(LDAConfiguration.NO_ITER_DEFAULT) + " total).");
		System.out.println("_____________________________\n");

		// Runs the model
		System.out.println("Starting:" + new Date());
		model.sample(config.getNoIterations(LDAConfiguration.NO_ITER_DEFAULT));
		System.out.println("Finished:" + new Date());

		System.out.println("I am done!");
	}
	
	@Test
	public void testSetPriors() throws IOException {	
		String whichModel = "spalias_uncollapsed";
		Integer numBatches = 6;

		Integer numIter = 100;
		SimpleLDAConfiguration config = getStdCfg(whichModel, numIter,numBatches);
		config.setDatasetFilename("src/main/resources/datasets/SmallTexts.txt");
		config.setNoTopics(4);
		config.setTopicPriorFilename("src/test/resources/topic_priors_SmallTexts.txt");

		String dataset_fn = config.getDatasetFilename();
		System.out.println("Using dataset: " + dataset_fn);
		System.out.println("Scheme: " + whichModel);
		LDALoggingUtils lu = new LoggingUtils();
		lu.checkAndCreateCurrentLogDir("TestRuns");
		config.setLoggingUtil(lu);

		InstanceList instances = LDAUtils.loadInstances(dataset_fn, 
				"stoplist.txt", config.getRareThreshold(LDAConfiguration.RARE_WORD_THRESHOLD));

		SpaliasUncollapsedParallelWithPriors model = new SpaliasUncollapsedParallelWithPriors(config);
		System.out.println(
				String.format("Spalias Uncollapsed Parallell LDA (%d batches).", 
						config.getNoBatches(LDAConfiguration.NO_BATCHES_DEFAULT)));

		System.out.println("Vocabulary size: " + instances.getDataAlphabet().size() + "\n");
		System.out.println("Instance list is: " + instances.size());
		System.out.println("Loading data instances...");

		model.setRandomSeed(config.getSeed(LDAConfiguration.SEED_DEFAULT));
		model.addInstances(instances);
		
		System.out.println("Alphabet is: " + model.getAlphabet());
		
		// Assert that ALL priors == 1.0
		double [][] topicPriors = model.getTopicPriors();
		//MatrixOps.print(topicPriors);
		
		assertTrue(1.0 == topicPriors[0][model.getAlphabet().lookupIndex("mother",false)]);
		assertTrue(0.0 == topicPriors[1][model.getAlphabet().lookupIndex("mother",false)]);
		assertTrue(0.0 == topicPriors[2][model.getAlphabet().lookupIndex("mother",false)]);
		assertTrue(0.0 == topicPriors[3][model.getAlphabet().lookupIndex("mother",false)]);

		assertTrue(1.0 == topicPriors[0][model.getAlphabet().lookupIndex("slip",false)]);
		assertTrue(0.0 == topicPriors[1][model.getAlphabet().lookupIndex("slip",false)]);
		assertTrue(0.0 == topicPriors[2][model.getAlphabet().lookupIndex("slip",false)]);
		assertTrue(0.0 == topicPriors[3][model.getAlphabet().lookupIndex("slip",false)]);

		assertTrue(1.0 == topicPriors[3][model.getAlphabet().lookupIndex("disk",false)]);
		assertTrue(0.0 == topicPriors[1][model.getAlphabet().lookupIndex("disk",false)]);
		assertTrue(0.0 == topicPriors[2][model.getAlphabet().lookupIndex("disk",false)]);
		assertTrue(0.0 == topicPriors[0][model.getAlphabet().lookupIndex("disk",false)]);

		assertTrue(1.0 == topicPriors[3][model.getAlphabet().lookupIndex("drive",false)]);
		assertTrue(0.0 == topicPriors[1][model.getAlphabet().lookupIndex("drive",false)]);
		assertTrue(0.0 == topicPriors[2][model.getAlphabet().lookupIndex("drive",false)]);
		assertTrue(0.0 == topicPriors[0][model.getAlphabet().lookupIndex("drive",false)]);
		
		System.out.println("Starting iterations (" + config.getNoIterations(LDAConfiguration.NO_ITER_DEFAULT) + " total).");
		System.out.println("_____________________________\n");

		// Runs the model
		System.out.println("Starting:" + new Date());
		model.sample(config.getNoIterations(LDAConfiguration.NO_ITER_DEFAULT));
		
		// Assert that ALL priors == 1.0
		double [][] topicPosteriors = model.getPhi();
		//System.out.println("Posterior Phi is: ");
		//MatrixOps.print(topicPosteriors);
		//System.out.println("Alphabet is: " + model.getAlphabet());
		
		// Ensure that the posterior is still 0
		assertTrue(0.0 == topicPosteriors[1][model.getAlphabet().lookupIndex("mother",false)]);
		assertTrue(0.0 == topicPosteriors[2][model.getAlphabet().lookupIndex("mother",false)]);
		assertTrue(0.0 == topicPosteriors[3][model.getAlphabet().lookupIndex("mother",false)]);

		assertTrue(0.0 == topicPosteriors[1][model.getAlphabet().lookupIndex("slip",false)]);
		assertTrue(0.0 == topicPosteriors[2][model.getAlphabet().lookupIndex("slip",false)]);
		assertTrue(0.0 == topicPosteriors[3][model.getAlphabet().lookupIndex("slip",false)]);

		assertTrue(0.0 == topicPosteriors[1][model.getAlphabet().lookupIndex("disk",false)]);
		assertTrue(0.0 == topicPosteriors[2][model.getAlphabet().lookupIndex("disk",false)]);
		assertTrue(0.0 == topicPosteriors[0][model.getAlphabet().lookupIndex("disk",false)]);

		assertTrue(0.0 == topicPosteriors[1][model.getAlphabet().lookupIndex("drive",false)]);
		assertTrue(0.0 == topicPosteriors[2][model.getAlphabet().lookupIndex("drive",false)]);
		assertTrue(0.0 == topicPosteriors[0][model.getAlphabet().lookupIndex("drive",false)]);

		
		System.out.println("Finished:" + new Date());

		System.out.println("I am done!");
	}
	
	@Test
	public void testSetPriorsPolyaUrn() throws IOException {	
		String whichModel = "polya_urn_with_priors";
		Integer numBatches = 6;

		Integer numIter = 1500;
		SimpleLDAConfiguration config = getStdCfg(whichModel, numIter,numBatches);
		config.setDatasetFilename("src/main/resources/datasets/SmallTexts.txt");
		config.setNoTopics(4);
		config.setTopicPriorFilename("src/test/resources/topic_priors_SmallTexts.txt");

		String dataset_fn = config.getDatasetFilename();
		System.out.println("Using dataset: " + dataset_fn);
		System.out.println("Scheme: " + whichModel);
		LDALoggingUtils lu = new LoggingUtils();
		lu.checkAndCreateCurrentLogDir("TestRuns");
		config.setLoggingUtil(lu);

		InstanceList instances = LDAUtils.loadInstances(dataset_fn, 
				"stoplist.txt", config.getRareThreshold(LDAConfiguration.RARE_WORD_THRESHOLD));

		PolyaUrnSpaliasLDAWithPriors model = new PolyaUrnSpaliasLDAWithPriors(config);
		System.out.println(
				String.format("Spalias Uncollapsed Parallell LDA (%d batches).", 
						config.getNoBatches(LDAConfiguration.NO_BATCHES_DEFAULT)));

		System.out.println("Vocabulary size: " + instances.getDataAlphabet().size() + "\n");
		System.out.println("Instance list is: " + instances.size());
		System.out.println("Loading data instances...");

		model.setRandomSeed(config.getSeed(LDAConfiguration.SEED_DEFAULT));
		model.addInstances(instances);
		
		System.out.println("Alphabet is: " + model.getAlphabet());
		
		// Assert that ALL priors == 1.0
		double [][] topicPriors = model.getTopicPriors();
		//MatrixOps.print(topicPriors);
		
		assertTrue(1.0 == topicPriors[0][model.getAlphabet().lookupIndex("mother",false)]);
		assertTrue(0.0 == topicPriors[1][model.getAlphabet().lookupIndex("mother",false)]);
		assertTrue(0.0 == topicPriors[2][model.getAlphabet().lookupIndex("mother",false)]);
		assertTrue(0.0 == topicPriors[3][model.getAlphabet().lookupIndex("mother",false)]);

		assertTrue(1.0 == topicPriors[0][model.getAlphabet().lookupIndex("slip",false)]);
		assertTrue(0.0 == topicPriors[1][model.getAlphabet().lookupIndex("slip",false)]);
		assertTrue(0.0 == topicPriors[2][model.getAlphabet().lookupIndex("slip",false)]);
		assertTrue(0.0 == topicPriors[3][model.getAlphabet().lookupIndex("slip",false)]);

		assertTrue(1.0 == topicPriors[3][model.getAlphabet().lookupIndex("disk",false)]);
		assertTrue(0.0 == topicPriors[1][model.getAlphabet().lookupIndex("disk",false)]);
		assertTrue(0.0 == topicPriors[2][model.getAlphabet().lookupIndex("disk",false)]);
		assertTrue(0.0 == topicPriors[0][model.getAlphabet().lookupIndex("disk",false)]);

		assertTrue(1.0 == topicPriors[3][model.getAlphabet().lookupIndex("drive",false)]);
		assertTrue(0.0 == topicPriors[1][model.getAlphabet().lookupIndex("drive",false)]);
		assertTrue(0.0 == topicPriors[2][model.getAlphabet().lookupIndex("drive",false)]);
		assertTrue(0.0 == topicPriors[0][model.getAlphabet().lookupIndex("drive",false)]);
		
		System.out.println("Starting iterations (" + config.getNoIterations(LDAConfiguration.NO_ITER_DEFAULT) + " total).");
		System.out.println("_____________________________\n");

		// Runs the model
		System.out.println("Starting:" + new Date());
		model.sample(config.getNoIterations(LDAConfiguration.NO_ITER_DEFAULT));
		
		// Assert that ALL priors == 1.0
		double [][] topicPosteriors = model.getPhi();
		System.out.println("Posterior Phi is: ");
		String [] vocabulary = LDAUtils.extractVocabulaty(instances.getDataAlphabet());
		System.out.println(Arrays.asList(vocabulary));
		MatrixOps.print(topicPosteriors);
		//System.out.println("Alphabet is: " + model.getAlphabet());
		
		// Ensure that the posterior is still 0
		assertTrue(0.0 == topicPosteriors[1][model.getAlphabet().lookupIndex("mother",false)]);
		assertTrue(0.0 == topicPosteriors[2][model.getAlphabet().lookupIndex("mother",false)]);
		assertTrue(0.0 == topicPosteriors[3][model.getAlphabet().lookupIndex("mother",false)]);

		assertTrue(0.0 == topicPosteriors[1][model.getAlphabet().lookupIndex("slip",false)]);
		assertTrue(0.0 == topicPosteriors[2][model.getAlphabet().lookupIndex("slip",false)]);
		assertTrue(0.0 == topicPosteriors[3][model.getAlphabet().lookupIndex("slip",false)]);

		assertTrue(0.0 == topicPosteriors[1][model.getAlphabet().lookupIndex("disk",false)]);
		assertTrue(0.0 == topicPosteriors[2][model.getAlphabet().lookupIndex("disk",false)]);
		assertTrue(0.0 == topicPosteriors[0][model.getAlphabet().lookupIndex("disk",false)]);

		assertTrue(0.0 == topicPosteriors[1][model.getAlphabet().lookupIndex("drive",false)]);
		assertTrue(0.0 == topicPosteriors[2][model.getAlphabet().lookupIndex("drive",false)]);
		assertTrue(0.0 == topicPosteriors[0][model.getAlphabet().lookupIndex("drive",false)]);
		
		System.out.println("Finished:" + new Date());

		System.out.println("I am done!");
	}
	
	@Test
	public void testSetPriorsNips() throws IOException {	
		String whichModel = "spalias_uncollapsed";
		Integer numBatches = 6;

		Integer numIter = 500;
		SimpleLDAConfiguration config = getStdCfg(whichModel, numIter,numBatches);
		config.setDatasetFilename("src/main/resources/datasets/nips.txt");
		config.setNoTopics(20);
		config.setTopicPriorFilename("src/main/resources/topic_priors.txt");

		String dataset_fn = config.getDatasetFilename();
		System.out.println("Using dataset: " + dataset_fn);
		System.out.println("Scheme: " + whichModel);
		LDALoggingUtils lu = new LoggingUtils();
		lu.checkAndCreateCurrentLogDir("TestRuns");
		config.setLoggingUtil(lu);

		InstanceList instances = LDAUtils.loadInstances(dataset_fn, 
				"stoplist.txt", config.getRareThreshold(LDAConfiguration.RARE_WORD_THRESHOLD));

		SpaliasUncollapsedParallelWithPriors model = new SpaliasUncollapsedParallelWithPriors(config);
		System.out.println(
				String.format("Spalias Uncollapsed Parallell LDA (%d batches).", 
						config.getNoBatches(LDAConfiguration.NO_BATCHES_DEFAULT)));

		System.out.println("Vocabulary size: " + instances.getDataAlphabet().size() + "\n");
		System.out.println("Instance list is: " + instances.size());
		System.out.println("Loading data instances...");

		model.setRandomSeed(config.getSeed(LDAConfiguration.SEED_DEFAULT));
		model.addInstances(instances);
		
		// Assert that ALL priors == 1.0
		double [][] topicPriors = model.getTopicPriors();
		//MatrixOps.print(topicPriors);
		
		assertTrue(1.0 == topicPriors[0][model.getAlphabet().lookupIndex("cell",false)]);
		assertTrue(0.0 == topicPriors[1][model.getAlphabet().lookupIndex("cell",false)]);
		assertTrue(0.0 == topicPriors[2][model.getAlphabet().lookupIndex("cell",false)]);
		assertTrue(0.0 == topicPriors[3][model.getAlphabet().lookupIndex("cell",false)]);

		assertTrue(1.0 == topicPriors[0][model.getAlphabet().lookupIndex("stimulus",false)]);
		assertTrue(0.0 == topicPriors[1][model.getAlphabet().lookupIndex("stimulus",false)]);
		assertTrue(0.0 == topicPriors[2][model.getAlphabet().lookupIndex("stimulus",false)]);
		assertTrue(0.0 == topicPriors[3][model.getAlphabet().lookupIndex("stimulus",false)]);

		assertTrue(1.0 == topicPriors[19][model.getAlphabet().lookupIndex("image",false)]);
		assertTrue(0.0 == topicPriors[1][model.getAlphabet().lookupIndex("image",false)]);
		assertTrue(0.0 == topicPriors[2][model.getAlphabet().lookupIndex("image",false)]);
		assertTrue(0.0 == topicPriors[0][model.getAlphabet().lookupIndex("image",false)]);

		assertTrue(1.0 == topicPriors[19][model.getAlphabet().lookupIndex("pixel",false)]);
		assertTrue(0.0 == topicPriors[1][model.getAlphabet().lookupIndex("pixel",false)]);
		assertTrue(0.0 == topicPriors[2][model.getAlphabet().lookupIndex("pixel",false)]);
		assertTrue(0.0 == topicPriors[0][model.getAlphabet().lookupIndex("pixel",false)]);
		
		System.out.println("Starting iterations (" + config.getNoIterations(LDAConfiguration.NO_ITER_DEFAULT) + " total).");
		System.out.println("_____________________________\n");

		// Runs the model
		System.out.println("Starting:" + new Date());
		model.sample(config.getNoIterations(LDAConfiguration.NO_ITER_DEFAULT));
		
		// Assert that ALL priors == 1.0
		double [][] topicPosteriors = model.getPhi();
		
		// Ensure that the posterior is still 0
		assertTrue(0.0 == topicPosteriors[1][model.getAlphabet().lookupIndex("cell",false)]);
		assertTrue(0.0 == topicPosteriors[2][model.getAlphabet().lookupIndex("cell",false)]);
		assertTrue(0.0 == topicPosteriors[3][model.getAlphabet().lookupIndex("cell",false)]);

		assertTrue(0.0 == topicPosteriors[1][model.getAlphabet().lookupIndex("stimulus",false)]);
		assertTrue(0.0 == topicPosteriors[2][model.getAlphabet().lookupIndex("stimulus",false)]);
		assertTrue(0.0 == topicPosteriors[3][model.getAlphabet().lookupIndex("stimulus",false)]);

		assertTrue(0.0 == topicPosteriors[1][model.getAlphabet().lookupIndex("image",false)]);
		assertTrue(0.0 == topicPosteriors[2][model.getAlphabet().lookupIndex("image",false)]);
		assertTrue(0.0 == topicPosteriors[0][model.getAlphabet().lookupIndex("image",false)]);

		assertTrue(0.0 == topicPosteriors[1][model.getAlphabet().lookupIndex("pixel",false)]);
		assertTrue(0.0 == topicPosteriors[2][model.getAlphabet().lookupIndex("pixel",false)]);
		assertTrue(0.0 == topicPosteriors[0][model.getAlphabet().lookupIndex("pixel",false)]);
		
		System.out.println("Finished:" + new Date());

		System.out.println("I am done!");
	}
	
	@Test
	public void testGetPhiMeans() throws IOException {
		String whichModel = "spalias_uncollapsed";
		Integer numBatches = 6;

		Integer numIter = 10;
		SimpleLDAConfiguration config = getStdCfg(whichModel, numIter, numBatches);
		config.setSavePhi(true);
		config.setPhiBurnIn(20);

		String dataset_fn = config.getDatasetFilename();
		System.out.println("Using dataset: " + dataset_fn);
		System.out.println("Scheme: " + whichModel);
		LDALoggingUtils lu = new LoggingUtils();
		lu.checkAndCreateCurrentLogDir("TestRuns");
		config.setLoggingUtil(lu);

		InstanceList instances = LDAUtils.loadInstances(dataset_fn, 
				"stoplist.txt", config.getRareThreshold(LDAConfiguration.RARE_WORD_THRESHOLD));

		LDAGibbsSampler model = new SpaliasUncollapsedParallelLDA(config);
		System.out.println(
				String.format("Spalias Uncollapsed Parallell LDA (%d batches).", 
						config.getNoBatches(LDAConfiguration.NO_BATCHES_DEFAULT)));

		System.out.println("Vocabulary size: " + instances.getDataAlphabet().size() + "\n");
		System.out.println("Instance list is: " + instances.size());
		System.out.println("Loading data instances...");

		model.setRandomSeed(config.getSeed(LDAConfiguration.SEED_DEFAULT));
		model.addInstances(instances);

		Integer noIterations = config.getNoIterations(LDAConfiguration.NO_ITER_DEFAULT);
		System.out.println("Starting iterations (" + noIterations + " total).");

		// Runs the model
		model.sample(noIterations);

		LDASamplerWithPhi modelWithPhi = (LDASamplerWithPhi) model;
		double [][] means = modelWithPhi.getPhiMeans();
		
		int burnInIter = (int)(((double)config.getPhiBurnInPercent(LDAConfiguration.PHI_BURN_IN_DEFAULT) / 100) * noIterations);
		assertEquals(numIter - burnInIter, ((SpaliasUncollapsedParallelLDA)model).getNoSampledPhi()); 
		assertEquals(means.length,config.getNoTopics(LDAConfiguration.NO_TOPICS_DEFAULT).intValue());
		assertEquals(means[0].length,instances.getDataAlphabet().size());
		
		int noNonZero = 0;
		for (int i = 0; i < means.length; i++) {
			for (int j = 0; j < means[i].length; j++) {
				if(means[i][j]!=0) noNonZero++;
			}
		}
		
		assertEquals(means.length * means[0].length, noNonZero);
	}
	
	@Test
	public void testNoPhiMeans() throws IOException {
		String whichModel = "spalias_uncollapsed";
		Integer numBatches = 6;

		Integer numIter = 10;
		SimpleLDAConfiguration config = getStdCfg(whichModel, numIter, numBatches);
		config.setSavePhi(false);
		config.setPhiBurnIn(20);

		String dataset_fn = config.getDatasetFilename();
		System.out.println("Using dataset: " + dataset_fn);
		System.out.println("Scheme: " + whichModel);
		LDALoggingUtils lu = new LoggingUtils();
		lu.checkAndCreateCurrentLogDir("TestRuns");
		config.setLoggingUtil(lu);

		InstanceList instances = LDAUtils.loadInstances(dataset_fn, 
				"stoplist.txt", config.getRareThreshold(LDAConfiguration.RARE_WORD_THRESHOLD));

		LDAGibbsSampler model = new SpaliasUncollapsedParallelLDA(config);
		System.out.println(
				String.format("Spalias Uncollapsed Parallell LDA (%d batches).", 
						config.getNoBatches(LDAConfiguration.NO_BATCHES_DEFAULT)));

		System.out.println("Vocabulary size: " + instances.getDataAlphabet().size() + "\n");
		System.out.println("Instance list is: " + instances.size());
		System.out.println("Loading data instances...");

		model.setRandomSeed(config.getSeed(LDAConfiguration.SEED_DEFAULT));
		model.addInstances(instances);

		Integer noIterations = config.getNoIterations(LDAConfiguration.NO_ITER_DEFAULT);
		System.out.println("Starting iterations (" + noIterations + " total).");

		// Runs the model
		model.sample(noIterations);

		LDASamplerWithPhi modelWithPhi = (LDASamplerWithPhi) model;
		assertEquals(0, ((SpaliasUncollapsedParallelLDA)model).getNoSampledPhi()); 
		assertTrue(modelWithPhi.getPhiMeans()==null);		
	}


}
