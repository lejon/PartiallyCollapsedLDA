package cc.mallet.topics;

import java.io.IOException;
import java.util.Date;

import org.junit.Test;

import cc.mallet.configuration.LDAConfiguration;
import cc.mallet.configuration.SimpleLDAConfiguration;
import cc.mallet.topics.LDAGibbsSampler;
import cc.mallet.topics.ParanoidSpaliasUncollapsedLDA;
import cc.mallet.topics.ParanoidUncollapsedParallelLDA;
import cc.mallet.topics.ParanoidVSSpaliasUncollapsedLDA;
import cc.mallet.topics.randomscan.document.BatchBuilderFactory;
import cc.mallet.types.InstanceList;
import cc.mallet.util.LDAUtils;
import cc.mallet.util.LoggingUtils;

public class ParanoidTest {
	
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
	public void testParanoid() throws IOException {	
		String whichModel = "uncollapsed_paranoid";
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

		LDAGibbsSampler model = new ParanoidUncollapsedParallelLDA(config);
		System.out.println(
				String.format("Uncollapsed Parallell LDA (%d batches).", 
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
	public void testParanoidSpalias() throws IOException {	
		String whichModel = "uncollapsed_paranoid";
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

		LDAGibbsSampler model = new ParanoidSpaliasUncollapsedLDA(config);
		System.out.println(
				String.format("Uncollapsed Parallell LDA (%d batches).", 
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
	public void testParanoidPoissonPolyaUrn() throws IOException {	
		String whichModel = "HDP";
		Integer numBatches = 6;

		Integer numIter = 50;
		SimpleLDAConfiguration config = getStdCfg(whichModel, numIter,
				numBatches);
		
		config.setNoTopics(100);

		String dataset_fn = config.getDatasetFilename();
		System.out.println("Using dataset: " + dataset_fn);
		System.out.println("Scheme: " + whichModel);
		LoggingUtils lu = new LoggingUtils();
		lu.checkAndCreateCurrentLogDir("TestRuns");
		config.setLoggingUtil(lu);

		InstanceList instances = LDAUtils.loadInstances(dataset_fn, 
				"stoplist.txt", config.getRareThreshold(LDAConfiguration.RARE_WORD_THRESHOLD));

		LDAGibbsSampler model = new ParanoidPoissonPolyaUrnHDP(config);
		System.out.println(
				String.format("Poisson Parallell HDP (%d batches).", 
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
	public void testParanoidLightLDAtypeTopicProposal() throws IOException {	
		String whichModel = "uncollapsed_lightttp_paranoid";
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

		LDAGibbsSampler model = new ParanoidLightPCLDAtypeTopicProposal(config);
		System.out.println(
				String.format("Uncollapsed LightPCLDA with type topic proposal Parallell LDA (%d batches).", 
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
	public void testParanoidCollapsedLight() throws IOException {	
		String whichModel = "collapsed_light_paranoid";
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

		LDAGibbsSampler model = new ParanoidCollapsedLightLDA(config);
		System.out.println(
				String.format("Collapsed Light Parallell LDA (%d batches).", 
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
	public void testParanoidVSSpalias() throws IOException {	
		String whichModel = "uncollapsed_paranoid";
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

		LDAGibbsSampler model = new ParanoidVSSpaliasUncollapsedLDA(config);
		System.out.println(
				String.format("Uncollapsed Parallell LDA (%d batches).", 
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
	public void testParanoidWPercentageSplit() throws IOException {	
		String whichModel = "uncollapsed_paranoid";
		Integer numIter = 100;
		Integer numBatches = 6;

		SimpleLDAConfiguration config = getStdCfg(whichModel, numIter,
				numBatches);

		String dataset_fn = config.getDatasetFilename();
		System.out.println("Using dataset: " + dataset_fn);
		System.out.println("Scheme: " + whichModel);
		LoggingUtils lu = new LoggingUtils();
		lu.checkAndCreateCurrentLogDir("TestRuns");
		config.setLoggingUtil(lu);
		config.setBatchBuildingScheme(BatchBuilderFactory.PERCENTAGE_SPLIT);
		double percentageSplit = 0.3;
		config.setDocPercentageSplitSize(percentageSplit);

		InstanceList instances = LDAUtils.loadInstances(dataset_fn, 
				"stoplist.txt", config.getRareThreshold(LDAConfiguration.RARE_WORD_THRESHOLD));

		LDAGibbsSampler model = new ParanoidUncollapsedParallelLDA(config);
		System.out.println(
				String.format("Uncollapsed Parallell LDA (%d batches).", 
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
	public void testParanoidWAdaptiveSplit() throws IOException {	
		String whichModel = "uncollapsed_paranoid";
		Integer numIter = 100;
		Integer numBatches = 6;

		SimpleLDAConfiguration config = getStdCfg(whichModel, numIter,
				numBatches);

		String dataset_fn = config.getDatasetFilename();
		System.out.println("Using dataset: " + dataset_fn);
		System.out.println("Scheme: " + whichModel);
		LoggingUtils lu = new LoggingUtils();
		lu.checkAndCreateCurrentLogDir("TestRuns");
		config.setLoggingUtil(lu);
		config.setBatchBuildingScheme(BatchBuilderFactory.ADAPTIVE_SPLIT);
		double percentageSplit = 0.3;
		config.setDocPercentageSplitSize(percentageSplit);

		InstanceList instances = LDAUtils.loadInstances(dataset_fn, 
				"stoplist.txt", config.getRareThreshold(LDAConfiguration.RARE_WORD_THRESHOLD));

		LDAGibbsSampler model = new ParanoidUncollapsedParallelLDA(config);
		System.out.println(
				String.format("Uncollapsed Parallell LDA (%d batches).", 
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
	public void testParanoidWFixedSplit() throws IOException {	
		String whichModel = "uncollapsed_paranoid";
		Integer numIter = 100;
		Integer numBatches = 6;

		SimpleLDAConfiguration config = getStdCfg(whichModel, numIter,
				numBatches);

		String dataset_fn = config.getDatasetFilename();
		System.out.println("Using dataset: " + dataset_fn);
		System.out.println("Scheme: " + whichModel);
		LoggingUtils lu = new LoggingUtils();
		lu.checkAndCreateCurrentLogDir("TestRuns");
		config.setLoggingUtil(lu);
		config.setBatchBuildingScheme(BatchBuilderFactory.FIXED_SPLIT);
		double [] percentageSplit = {0.1, 0.2, 0.3, 0.5, 0.7, 1.0, 0.4};
		config.setFixedSplitSizeDoc(percentageSplit);

		InstanceList instances = LDAUtils.loadInstances(dataset_fn, 
				"stoplist.txt", config.getRareThreshold(LDAConfiguration.RARE_WORD_THRESHOLD));

		LDAGibbsSampler model = new ParanoidUncollapsedParallelLDA(config);
		System.out.println(
				String.format("Uncollapsed Parallell LDA (%d batches).", 
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

}
