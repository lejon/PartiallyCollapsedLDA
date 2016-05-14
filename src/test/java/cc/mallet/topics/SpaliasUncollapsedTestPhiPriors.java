package cc.mallet.topics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Date;
import java.util.Random;

import org.junit.Assert;
import org.junit.Test;

import cc.mallet.configuration.LDAConfiguration;
import cc.mallet.configuration.SimpleLDAConfiguration;
import cc.mallet.types.InstanceList;
import cc.mallet.types.ParallelDirichlet;
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
		LoggingUtils lu = new LoggingUtils();
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
		LoggingUtils lu = new LoggingUtils();
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
		LoggingUtils lu = new LoggingUtils();
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
		LoggingUtils lu = new LoggingUtils();
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
		LoggingUtils lu = new LoggingUtils();
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
		
		assertEquals(1.0, topicPriors[0][model.getAlphabet().lookupIndex("mother",false)], 0.0000000001);
		assertEquals(0.0, topicPriors[1][model.getAlphabet().lookupIndex("mother",false)], 0.0000000001);
		assertEquals(0.0, topicPriors[2][model.getAlphabet().lookupIndex("mother",false)], 0.0000000001);
		assertEquals(0.0, topicPriors[3][model.getAlphabet().lookupIndex("mother",false)], 0.0000000001);

		assertEquals(1.0, topicPriors[0][model.getAlphabet().lookupIndex("slip",false)], 0.0000000001);
		assertEquals(0.0, topicPriors[1][model.getAlphabet().lookupIndex("slip",false)], 0.0000000001);
		assertEquals(0.0, topicPriors[2][model.getAlphabet().lookupIndex("slip",false)], 0.0000000001);
		assertEquals(0.0, topicPriors[3][model.getAlphabet().lookupIndex("slip",false)], 0.0000000001);

		assertEquals(1.0, topicPriors[3][model.getAlphabet().lookupIndex("disk",false)], 0.0000000001);
		assertEquals(0.0, topicPriors[1][model.getAlphabet().lookupIndex("disk",false)], 0.0000000001);
		assertEquals(0.0, topicPriors[2][model.getAlphabet().lookupIndex("disk",false)], 0.0000000001);
		assertEquals(0.0, topicPriors[0][model.getAlphabet().lookupIndex("disk",false)], 0.0000000001);

		assertEquals(1.0, topicPriors[3][model.getAlphabet().lookupIndex("drive",false)], 0.0000000001);
		assertEquals(0.0, topicPriors[1][model.getAlphabet().lookupIndex("drive",false)], 0.0000000001);
		assertEquals(0.0, topicPriors[2][model.getAlphabet().lookupIndex("drive",false)], 0.0000000001);
		assertEquals(0.0, topicPriors[0][model.getAlphabet().lookupIndex("drive",false)], 0.0000000001);
		
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
		assertEquals(0.0, topicPosteriors[1][model.getAlphabet().lookupIndex("mother",false)], 0.0000000001);
		assertEquals(0.0, topicPosteriors[2][model.getAlphabet().lookupIndex("mother",false)], 0.0000000001);
		assertEquals(0.0, topicPosteriors[3][model.getAlphabet().lookupIndex("mother",false)], 0.0000000001);

		assertEquals(0.0, topicPosteriors[1][model.getAlphabet().lookupIndex("slip",false)], 0.0000000001);
		assertEquals(0.0, topicPosteriors[2][model.getAlphabet().lookupIndex("slip",false)], 0.0000000001);
		assertEquals(0.0, topicPosteriors[3][model.getAlphabet().lookupIndex("slip",false)], 0.0000000001);

		assertEquals(0.0, topicPosteriors[1][model.getAlphabet().lookupIndex("disk",false)], 0.0000000001);
		assertEquals(0.0, topicPosteriors[2][model.getAlphabet().lookupIndex("disk",false)], 0.0000000001);
		assertEquals(0.0, topicPosteriors[0][model.getAlphabet().lookupIndex("disk",false)], 0.0000000001);

		assertEquals(0.0, topicPosteriors[1][model.getAlphabet().lookupIndex("drive",false)], 0.0000000001);
		assertEquals(0.0, topicPosteriors[2][model.getAlphabet().lookupIndex("drive",false)], 0.0000000001);
		assertEquals(0.0, topicPosteriors[0][model.getAlphabet().lookupIndex("drive",false)], 0.0000000001);
		
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
		LoggingUtils lu = new LoggingUtils();
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
		
		assertEquals(1.0, topicPriors[0][model.getAlphabet().lookupIndex("cell",false)], 0.0000000001);
		assertEquals(0.0, topicPriors[1][model.getAlphabet().lookupIndex("cell",false)], 0.0000000001);
		assertEquals(0.0, topicPriors[2][model.getAlphabet().lookupIndex("cell",false)], 0.0000000001);
		assertEquals(0.0, topicPriors[3][model.getAlphabet().lookupIndex("cell",false)], 0.0000000001);

		assertEquals(1.0, topicPriors[0][model.getAlphabet().lookupIndex("stimulus",false)], 0.0000000001);
		assertEquals(0.0, topicPriors[1][model.getAlphabet().lookupIndex("stimulus",false)], 0.0000000001);
		assertEquals(0.0, topicPriors[2][model.getAlphabet().lookupIndex("stimulus",false)], 0.0000000001);
		assertEquals(0.0, topicPriors[3][model.getAlphabet().lookupIndex("stimulus",false)], 0.0000000001);

		assertEquals(1.0, topicPriors[19][model.getAlphabet().lookupIndex("image",false)], 0.0000000001);
		assertEquals(0.0, topicPriors[1][model.getAlphabet().lookupIndex("image",false)], 0.0000000001);
		assertEquals(0.0, topicPriors[2][model.getAlphabet().lookupIndex("image",false)], 0.0000000001);
		assertEquals(0.0, topicPriors[0][model.getAlphabet().lookupIndex("image",false)], 0.0000000001);

		assertEquals(1.0, topicPriors[19][model.getAlphabet().lookupIndex("pixel",false)], 0.0000000001);
		assertEquals(0.0, topicPriors[1][model.getAlphabet().lookupIndex("pixel",false)], 0.0000000001);
		assertEquals(0.0, topicPriors[2][model.getAlphabet().lookupIndex("pixel",false)], 0.0000000001);
		assertEquals(0.0, topicPriors[0][model.getAlphabet().lookupIndex("pixel",false)], 0.0000000001);
		
		System.out.println("Starting iterations (" + config.getNoIterations(LDAConfiguration.NO_ITER_DEFAULT) + " total).");
		System.out.println("_____________________________\n");

		// Runs the model
		System.out.println("Starting:" + new Date());
		model.sample(config.getNoIterations(LDAConfiguration.NO_ITER_DEFAULT));
		
		// Assert that ALL priors == 1.0
		double [][] topicPosteriors = model.getPhi();
		
		// Ensure that the posterior is still 0
		assertEquals(0.0, topicPosteriors[1][model.getAlphabet().lookupIndex("cell",false)], 0.0000000001);
		assertEquals(0.0, topicPosteriors[2][model.getAlphabet().lookupIndex("cell",false)], 0.0000000001);
		assertEquals(0.0, topicPosteriors[3][model.getAlphabet().lookupIndex("cell",false)], 0.0000000001);

		assertEquals(0.0, topicPosteriors[1][model.getAlphabet().lookupIndex("stimulus",false)], 0.0000000001);
		assertEquals(0.0, topicPosteriors[2][model.getAlphabet().lookupIndex("stimulus",false)], 0.0000000001);
		assertEquals(0.0, topicPosteriors[3][model.getAlphabet().lookupIndex("stimulus",false)], 0.0000000001);

		assertEquals(0.0, topicPosteriors[1][model.getAlphabet().lookupIndex("image",false)], 0.0000000001);
		assertEquals(0.0, topicPosteriors[2][model.getAlphabet().lookupIndex("image",false)], 0.0000000001);
		assertEquals(0.0, topicPosteriors[0][model.getAlphabet().lookupIndex("image",false)], 0.0000000001);

		assertEquals(0.0, topicPosteriors[1][model.getAlphabet().lookupIndex("pixel",false)], 0.0000000001);
		assertEquals(0.0, topicPosteriors[2][model.getAlphabet().lookupIndex("pixel",false)], 0.0000000001);
		assertEquals(0.0, topicPosteriors[0][model.getAlphabet().lookupIndex("pixel",false)], 0.0000000001);
		
		System.out.println("Finished:" + new Date());

		System.out.println("I am done!");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testFindIdxEmpty() {
		double [] cumsum = new double [0];
		try {
			// If we use the linear search, it will throw an
			// ArrayIndexOutOfBoundsException but I don't want 
			// to have that check in the actual method due to performance
			// reasons, so we put that check here instead...
			SpaliasUncollapsedParallelLDA.findIdx(cumsum, 0.5, cumsum.length-1);			
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new IllegalArgumentException(e);
		}
	}

	@Test
	public void testFindIdxOneElem() {
		double [] cumsum = {0.3};
		assertEquals(0,SpaliasUncollapsedParallelLDA.findIdx(cumsum, 0.5, cumsum.length-1));
	}

	@Test
	public void testFindIdxTwoElemGt() {
		double [] cumsum = {0.3,0.7};
		assertEquals(1,SpaliasUncollapsedParallelLDA.findIdx(cumsum, 0.5, cumsum.length-1));
	}

	@Test
	public void testFindIdxTwoElemLt() {
		double [] cumsum = {0.3,0.7};
		assertEquals(0,SpaliasUncollapsedParallelLDA.findIdx(cumsum, 0.2, cumsum.length-1));
	}

	@Test
	public void testFindIdxThreeElemLt() {
		double [] cumsum = {0.2,0.3,0.7};
		assertEquals(0,SpaliasUncollapsedParallelLDA.findIdx(cumsum, 0.2, cumsum.length-1));
	}
	
	@Test
	public void testFindIdxThreeElemGt() {
		double [] cumsum = {0.2,0.3,0.7};
		assertEquals(2,SpaliasUncollapsedParallelLDA.findIdx(cumsum, 0.6, cumsum.length-1));
	}

	@Test
	public void testFindIdxThreeElemMdl() {
		double [] cumsum = {0.2,0.3,0.7};
		assertEquals(1,SpaliasUncollapsedParallelLDA.findIdx(cumsum, 0.25, cumsum.length-1));
	}

	@Test
	public void testFindIdx7ElemUpper() {
		double [] cumsum = {0.1,0.2,0.3,0.5,0.7,0.9,1.0};
		assertEquals(6,SpaliasUncollapsedParallelLDA.findIdx(cumsum, 0.95, cumsum.length-1));
	}

	@Test
	public void testFindIdx7ElemLower() {
		double [] cumsum = {0.1,0.2,0.3,0.5,0.7,0.9,1.0};
		assertEquals(0,SpaliasUncollapsedParallelLDA.findIdx(cumsum, 0.05, cumsum.length-1));
	}

	@Test
	public void testVarious() {
		Random rnd = new Random();
		int noTests = 10000;
		for (int i = 0; i < noTests; i++) {			
			int len = rnd.nextInt(5000);
			if(len<2) len = 2;
			ParallelDirichlet pd = new ParallelDirichlet(len);
			double [] dirDraw = pd.nextDistribution();
			double [] cumsum = new double[len];
			cumsum[0] = dirDraw[0];
			for (int j = 1; j < cumsum.length; j++) {
				cumsum[j] = cumsum[j-1] + dirDraw[j];
			}
			double u = rnd.nextDouble();
			int idx = 0;
			// Find index by linear search...
			for (idx = 0; idx < cumsum.length; idx++) {
				if(u<=cumsum[idx]) {
					break;
				}
			}
			int findIdx = SpaliasUncollapsedParallelLDA.findIdx(cumsum, u, cumsum.length-1);
			if(idx != findIdx) {
				System.out.println(getRes(cumsum,u,idx));
			}
			assertEquals("Wrong index!",idx, findIdx);
		}
	}

	private String getRes(double[] cumsum, double u, int idx) {
		String res = "Idx=" + idx + "\n";
		res += ("U=" + u) + "\n";
		res += "Cumsum:\n\t[";
		for (int i = 0; i < cumsum.length; i++) {
			res += ("{" + i + "," + cumsum[i] + "},"); 
		}
		
		return res + "]";
	}
	
	@Test
	public void testInsertEmpty() {
		int [] nonZeroTopics = new int [20];
		int [] nonZeroTopicBackMapping = new int [20];
		int nonZeroCount = SpaliasUncollapsedParallelLDA.insertSorted(15,nonZeroTopics,nonZeroTopicBackMapping,0);
		assertEquals(1,nonZeroCount);
		assertEquals(15,nonZeroTopics[0]);
		assertEquals(0,nonZeroTopicBackMapping[15]);		
	}
	
	@Test
	public void testFillUp() {
		int numTopics = 20;
		int [] nonZeroTopics = new int [numTopics];
		int [] nonZeroTopicBackMapping = new int [numTopics];
		int nonZeroCount = 0;
		int [] expected      = {0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19};
		int [] xpctZeroTopicBackMapping = {0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19};
		
		int [] toInsert = {3,8,18,15,11,12,17,13,19,16,10,5,1,2,6,9,7,4,14,0};
		assertEquals(numTopics,toInsert.length);
		
		for (int i = 0; i < toInsert.length; i++) {
			nonZeroCount = SpaliasUncollapsedParallelLDA.insertSorted(toInsert[i],
					nonZeroTopics,nonZeroTopicBackMapping,nonZeroCount);			
		}
		
		assertEquals(20,nonZeroCount);
		for (int i = 0; i < nonZeroCount; i++) {
			assertEquals(expected[i],nonZeroTopics[i]);
			assertEquals(xpctZeroTopicBackMapping[i],nonZeroTopicBackMapping[i]);
		}
	}
	
	@Test
	public void testInsertLast() {
		int numTopics = 20;
		int [] nonZeroTopics = {0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,0};
		assertEquals(numTopics,nonZeroTopics.length);
		int [] nonZeroTopicBackMapping = {0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,0};
		assertEquals(numTopics,nonZeroTopicBackMapping.length);
		int nonZeroCount = 19;
		int newTopic = 19;
		nonZeroCount = SpaliasUncollapsedParallelLDA.insertSorted(newTopic,
				nonZeroTopics,nonZeroTopicBackMapping,nonZeroCount);
		assertEquals(20,nonZeroCount);
		for (int i = 0; i < nonZeroCount; i++) {
			assertEquals(i,nonZeroTopics[i]);
			assertEquals(i,nonZeroTopicBackMapping[i]);
		}
	}
	
	@Test
	public void testInsertFirst() {
		int numTopics = 20;
		int [] nonZeroTopics = {1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,0};
		assertEquals(numTopics,nonZeroTopics.length);
		int [] nonZeroTopicBackMapping = {-1,0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18};
		assertEquals(numTopics,nonZeroTopicBackMapping.length);
		int nonZeroCount = 19;
		int newTopic = 0;
		nonZeroCount = SpaliasUncollapsedParallelLDA.insertSorted(newTopic,
				nonZeroTopics,nonZeroTopicBackMapping,nonZeroCount);
		assertEquals(20,nonZeroCount);
		for (int i = 0; i < nonZeroCount; i++) {
			assertEquals(i,nonZeroTopics[i]);
			assertEquals(i,nonZeroTopicBackMapping[i]);
		}
	}
	
	@Test
	public void testInsertInBetween() {
		int numTopics = 20;
		int [] nonZeroTopics = {0,1,2,3,4,5,6,7,8,9,10,12,13,14,15,16,17,18,19,0};
		assertEquals(numTopics,nonZeroTopics.length);
		int [] nonZeroTopicBackMapping = {0,1,2,3,4,5,6,7,8,9,10,-1,11,12,13,14,15,16,17,18};
		assertEquals(numTopics,nonZeroTopicBackMapping.length);
		int nonZeroCount = 19;
		int newTopic = 11;
		nonZeroCount = SpaliasUncollapsedParallelLDA.insertSorted(newTopic,
				nonZeroTopics,nonZeroTopicBackMapping,nonZeroCount);
		assertEquals(20,nonZeroCount);
		for (int i = 0; i < nonZeroCount; i++) {
			assertEquals(i,nonZeroTopics[i]);
			assertEquals(i,nonZeroTopicBackMapping[i]);
		}
	}
	
	@Test
	public void testInsertSecond() {
		int numTopics = 20;
		int [] nonZeroTopics = {0,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,0};
		assertEquals(numTopics,nonZeroTopics.length);
		int [] nonZeroTopicBackMapping = {0,-1,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18};
		assertEquals(numTopics,nonZeroTopicBackMapping.length);
		int nonZeroCount = 19;
		int newTopic = 1;
		nonZeroCount = SpaliasUncollapsedParallelLDA.insertSorted(newTopic,
				nonZeroTopics,nonZeroTopicBackMapping,nonZeroCount);
		assertEquals(20,nonZeroCount);
		for (int i = 0; i < nonZeroCount; i++) {
			assertEquals(i,nonZeroTopics[i]);
			assertEquals(i,nonZeroTopicBackMapping[i]);
		}
	}

	@Test
	public void testInsertNextToLast() {
		int numTopics = 20;
		int [] nonZeroTopics = {0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,19,0};
		assertEquals(numTopics,nonZeroTopics.length);
		int [] nonZeroTopicBackMapping = {0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,-1,18};
		assertEquals(numTopics,nonZeroTopicBackMapping.length);
		int nonZeroCount = 19;
		int newTopic = 18;
		nonZeroCount = SpaliasUncollapsedParallelLDA.insertSorted(newTopic,
				nonZeroTopics,nonZeroTopicBackMapping,nonZeroCount);
		assertEquals(20,nonZeroCount);
		for (int i = 0; i < nonZeroCount; i++) {
			System.out.print(nonZeroTopics[i] + ",");
			assertEquals(i,nonZeroTopics[i]);
			assertEquals(i,nonZeroTopicBackMapping[i]);
		}
	}
	
	@Test
	public void testInsertSeveral() {
		int numTopics = 20;
		int [] nonZeroTopics = {1,2,4,5,6,7,9,10,12,13,14,15,16,18,19,0,0,0,0,0};
		assertEquals(numTopics,nonZeroTopics.length);
		int [] nonZeroTopicBackMapping = {-1,1,2,-1,4,5,6,7,-1,9,10,-1,12,13,14,15,16,-1,18,19};
		assertEquals(numTopics,nonZeroTopicBackMapping.length);

		int [] toInsert = {0,3,17,8,11};
		int nonZeroCount = numTopics - toInsert.length;
		
		for (int i = 0; i < toInsert.length; i++) {
			nonZeroCount = SpaliasUncollapsedParallelLDA.insertSorted(toInsert[i],
					nonZeroTopics,nonZeroTopicBackMapping,nonZeroCount);			
		}
		
		assertEquals(20,nonZeroCount);
		for (int i = 0; i < nonZeroCount; i++) {
			System.out.print(nonZeroTopics[i] + ",");
			assertEquals(i,nonZeroTopics[i]);
			assertEquals(i,nonZeroTopicBackMapping[i]);
		}
	}
	
	
	
	
	@Test(expected = IllegalArgumentException.class)
	public void testRemoveEmpty() {
		int [] nonZeroTopics = new int [20];
		int [] nonZeroTopicBackMapping = new int [20];
		SpaliasUncollapsedParallelLDA.removeSorted(15,nonZeroTopics,nonZeroTopicBackMapping,0);
	}
	
	@Test
	public void testRemoveLast() {
		int numTopics = 20;
		int [] nonZeroTopics = {0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19};
		int [] expected      = {0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19};
		assertEquals(numTopics,nonZeroTopics.length);
		int [] nonZeroTopicBackMapping = {0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19};
		assertEquals(numTopics,nonZeroTopicBackMapping.length);
		int nonZeroCount = 20;
		int newTopic = 19;
		nonZeroCount = SpaliasUncollapsedParallelLDA.removeSorted(newTopic,
				nonZeroTopics,nonZeroTopicBackMapping,nonZeroCount);
		assertEquals(19,nonZeroCount);
		for (int i = 0; i < nonZeroCount; i++) {
			assertEquals(expected[i],nonZeroTopics[i]);
			//assertEquals(i,nonZeroTopicBackMapping[i]);
		}
	}
	
	@Test
	public void testRemoveFirst() {
		int numTopics = 20;
		int [] nonZeroTopics = {0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19};
		int [] expected      = {1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,0};
		assertEquals(numTopics,nonZeroTopics.length);
		int [] nonZeroTopicBackMapping = {0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19};
		assertEquals(numTopics,nonZeroTopicBackMapping.length);
		int nonZeroCount = 20;
		int newTopic = 0;
		nonZeroCount = SpaliasUncollapsedParallelLDA.removeSorted(newTopic,
				nonZeroTopics,nonZeroTopicBackMapping,nonZeroCount);
		assertEquals(19,nonZeroCount);
		for (int i = 0; i < nonZeroCount; i++) {
			assertEquals(expected[i],nonZeroTopics[i]);
			//assertEquals(i,nonZeroTopicBackMapping[i]);
		}
	}
	
	@Test
	public void testRemoveInBetween() {
		int numTopics = 20;
		int [] nonZeroTopics = {0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19};
		int [] expected      = {0,1,2,3,4,5,6,7,8,9,10,12,13,14,15,16,17,18,19,0};
		assertEquals(numTopics,nonZeroTopics.length);
		int [] nonZeroTopicBackMapping = {0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19};
		assertEquals(numTopics,nonZeroTopicBackMapping.length);
		int nonZeroCount = 20;
		int newTopic = 11;
		nonZeroCount = SpaliasUncollapsedParallelLDA.removeSorted(newTopic,
				nonZeroTopics,nonZeroTopicBackMapping,nonZeroCount);
		assertEquals(19,nonZeroCount);
		for (int i = 0; i < nonZeroCount; i++) {
			assertEquals(expected[i],nonZeroTopics[i]);
			//assertEquals(i,nonZeroTopicBackMapping[i]);
		}
	}
	
	@Test
	public void testRemoveSecond() {
		int numTopics = 20;
		int [] nonZeroTopics = {0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19};
		int [] expected      = {0,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,0};
		assertEquals(numTopics,nonZeroTopics.length);
		int [] nonZeroTopicBackMapping = {0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19};
		assertEquals(numTopics,nonZeroTopicBackMapping.length);
		int nonZeroCount = 20;
		int newTopic = 1;
		nonZeroCount = SpaliasUncollapsedParallelLDA.removeSorted(newTopic,
				nonZeroTopics,nonZeroTopicBackMapping,nonZeroCount);
		assertEquals(19,nonZeroCount);
		for (int i = 0; i < nonZeroCount; i++) {
			assertEquals(expected[i],nonZeroTopics[i]);
			//assertEquals(i,nonZeroTopicBackMapping[i]);
		}
	}

	@Test
	public void testRemoveNextToLast() {
		int numTopics = 20;
		int [] nonZeroTopics = {0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19};
		int [] expected      = {0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,19,0};
		assertEquals(numTopics,nonZeroTopics.length);
		int [] nonZeroTopicBackMapping = {0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19};
		assertEquals(numTopics,nonZeroTopicBackMapping.length);
		int nonZeroCount = 20;
		int newTopic = 18;
		nonZeroCount = SpaliasUncollapsedParallelLDA.removeSorted(newTopic,
				nonZeroTopics,nonZeroTopicBackMapping,nonZeroCount);
		assertEquals(19,nonZeroCount);
		for (int i = 0; i < nonZeroCount; i++) {
			assertEquals(expected[i],nonZeroTopics[i]);
			//assertEquals(i,nonZeroTopicBackMapping[i]);
		}
	}
	
	@Test
	public void testRemoveSeveral() {
		int numTopics = 20;
		int [] nonZeroTopics = {0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19};
		int [] expected      = {1,2,4,5,6,7,9,10,12,13,14,15,16,18,19,0,0,0,0,0};
		assertEquals(numTopics,nonZeroTopics.length);
		int [] nonZeroTopicBackMapping = {0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19};
		assertEquals(numTopics,nonZeroTopicBackMapping.length);

		int [] toRemove = {0,3,17,8,11};
		int nonZeroCount = 20;
		
		for (int i = 0; i < toRemove.length; i++) {
			nonZeroCount = SpaliasUncollapsedParallelLDA.removeSorted(toRemove[i],
					nonZeroTopics,nonZeroTopicBackMapping,nonZeroCount);			
		}
		
		assertEquals(numTopics - toRemove.length,nonZeroCount);
		for (int i = 0; i < nonZeroCount; i++) {
			assertEquals(expected[i],nonZeroTopics[i]);
			//assertEquals(i,nonZeroTopicBackMapping[i]);
		}
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
		LoggingUtils lu = new LoggingUtils();
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
		LoggingUtils lu = new LoggingUtils();
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
