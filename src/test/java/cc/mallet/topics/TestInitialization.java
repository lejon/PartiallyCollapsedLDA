package cc.mallet.topics;

import static org.junit.Assert.assertEquals;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.apache.commons.cli.ParseException;
import org.apache.commons.configuration.ConfigurationException;
import org.junit.Test;

import cc.mallet.configuration.LDAConfiguration;
import cc.mallet.configuration.SimpleLDAConfiguration;
import cc.mallet.topics.randomscan.document.BatchBuilderFactory;
import cc.mallet.types.FeatureSequence;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.util.LDAUtils;
import cc.mallet.util.LoggingUtils;
import cc.mallet.utils.TestUtils;

public class TestInitialization {
	double epsilon = 0.000001;

	@Test
	public void testEqualInitialization() throws ParseException, ConfigurationException, IOException {
		//String [] args = {"--run_cfg=src/main/resources/configuration/TestConfig.cfg"};
		//LDACommandLineParser cp = new LDACommandLineParser(args);
		//LDAConfiguration config = (LDAConfiguration) ConfigFactory.getMainConfiguration(cp);
		
		int seed = 20150326;
		Integer numTopics = 20;
		Double alpha = 0.1; 
		Double beta = 0.01;
		Integer numIter = 1000;
		Integer numBatches = 4;
		Integer rareWordThreshold = 10;
		Integer showTopicsInterval = 50;
		Integer startDiagnosticOutput = 500;

		SimpleLDAConfiguration config = new SimpleLDAConfiguration(new LoggingUtils(), "ALL",
				numTopics, alpha, beta, numIter,
				numBatches, rareWordThreshold, showTopicsInterval,
				startDiagnosticOutput,seed,"src/main/resources/datasets/nips.txt");

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
		collapsed.setRandomSeed(config.getSeed(LDAConfiguration.SEED_DEFAULT));
		collapsed.setConfiguration(config);
		collapsed.addInstances(instances);

		SpaliasUncollapsedParallelLDA spalias = new SpaliasUncollapsedParallelLDA(config);
		spalias.setRandomSeed(config.getSeed(LDAConfiguration.SEED_DEFAULT));
		spalias.addInstances(instances);
		
		UncollapsedParallelLDA uncollapsed = new UncollapsedParallelLDA(config);
		uncollapsed.setRandomSeed(config.getSeed(LDAConfiguration.SEED_DEFAULT));
		uncollapsed.addInstances(instances);
		
		ADLDA adlda = new ADLDA(config);
		adlda.setRandomSeed(config.getSeed(LDAConfiguration.SEED_DEFAULT));
		adlda.addInstances(instances);

		LightPCLDA lightpclda = new LightPCLDA(config);
		lightpclda.setRandomSeed(config.getSeed(LDAConfiguration.SEED_DEFAULT));
		lightpclda.addInstances(instances);

		LightPCLDAtypeTopicProposal lightpcldaTTP = new LightPCLDAtypeTopicProposal(config);
		lightpcldaTTP.setRandomSeed(config.getSeed(LDAConfiguration.SEED_DEFAULT));
		lightpcldaTTP.addInstances(instances);
		
		CollapsedLightLDA collapsedlight = new CollapsedLightLDA(config);
		collapsedlight.setRandomSeed(config.getSeed(LDAConfiguration.SEED_DEFAULT));
		collapsedlight.addInstances(instances);
		
		int [][] spaliasTopicIndicators     = spalias.getTypeTopicCounts();
		int [][] collapsedTopicIndicators   = collapsed.getTypeTopicCounts();
		int [][] uncollapsedTopicIndicators = uncollapsed.getTypeTopicCounts();
		int [][] adldaTopicIndicators       = adlda.getTypeTopicCounts();
		int [][] lightPcLdaTopicIndicators  = lightpclda.getTypeTopicCounts();
		int [][] collapsedlightTopicIndicators  = collapsedlight.getTypeTopicCounts();
		int [][] lightPcLdaTTPTopicIndicators  = lightpcldaTTP.getTypeTopicCounts();

		for (int i = 0; i < uncollapsedTopicIndicators.length; i++) {
			for (int j = 0; j < uncollapsedTopicIndicators[i].length; j++) {
				assertEquals("Collapsed and Spalias are not the same: " 
						+ collapsedTopicIndicators[i][j] + "!=" + spaliasTopicIndicators[i][j], 
						collapsedTopicIndicators[i][j], spaliasTopicIndicators[i][j]);
				assertEquals("Collapsed and UnCollapsed are not the same: " 
						+ collapsedTopicIndicators[i][j] + "!=" + uncollapsedTopicIndicators[i][j], 
						collapsedTopicIndicators[i][j], uncollapsedTopicIndicators[i][j]);
				assertEquals("Collapsed and ADLDA are not the same: " 
						+ collapsedTopicIndicators[i][j] + "!=" + adldaTopicIndicators[i][j], 
						collapsedTopicIndicators[i][j], adldaTopicIndicators[i][j]);
				assertEquals("Collapsed and LightPCLDA are not the same: " 
						+ collapsedTopicIndicators[i][j] + "!=" + lightPcLdaTopicIndicators[i][j], 
						collapsedTopicIndicators[i][j], lightPcLdaTopicIndicators[i][j]);
				assertEquals("Collapsed and CollapsedLight are not the same: " 
						+ collapsedTopicIndicators[i][j] + "!=" + collapsedlightTopicIndicators[i][j], 
						collapsedTopicIndicators[i][j], collapsedlightTopicIndicators[i][j]);
				assertEquals("Collapsed and LightPCLDATTP are not the same: " 
						+ collapsedTopicIndicators[i][j] + "!=" + lightPcLdaTTPTopicIndicators[i][j], 
						collapsedTopicIndicators[i][j], lightPcLdaTTPTopicIndicators[i][j]);
			}
		}
		
		// CollapsedLight and LightPCLDAtypeTopicProposal should have the same "tokens per type"
		int [] collapsedLightTTP =  collapsedlight.getTokensPerType(); 
		int [] lightPCLDATTP = lightpcldaTTP.getTokensPerType();
		assertEquals("Collapsed Light and LightPCLDAtypeTopicProposal does not initialize the same length of tokens per type",
				collapsedLightTTP.length,
				lightPCLDATTP.length);
		for (int i = 0; i < lightPCLDATTP.length; i++) {			
			assertEquals("Collapsed Light and LightPCLDAtypeTopicProposal does not initialize the same tokens per type",
					collapsedLightTTP[i],
					lightPCLDATTP[i]);
		}

		int [] spaliasTokensPerTopic   = spalias.getTopicTotals();
		int [] collapsedTokensPerTopic   = collapsed.getTopicTotals();
		int [] uncollapsedTokensPerTopic = uncollapsed.getTopicTotals();
		int [] adldaTokensPerTopic       = adlda.getTopicTotals();
		int [] lightPcLdaTokensPerTopic  = lightpclda.getTopicTotals();
		int [] lightPcLdaTTPTokensPerTopic  = lightpcldaTTP.getTopicTotals();
		int [] collapsedlightTokensPerTopic  = collapsedlight.getTopicTotals();

		for (int i = 0; i < collapsedTokensPerTopic.length; i++) {
			assertEquals("Collapsed and ADLA token counts are not the same: " 
					+ collapsedTokensPerTopic[i] + "!=" + adldaTokensPerTopic[i], 
					collapsedTokensPerTopic[i], adldaTokensPerTopic[i]);
			assertEquals("Collapsed and Spalias token counts are not the same: " 
					+ collapsedTokensPerTopic[i] + "!=" + spaliasTokensPerTopic[i], 
					collapsedTokensPerTopic[i], spaliasTokensPerTopic[i]);
			assertEquals("Collapsed and UnCollapsed token counts are not the same: " 
					+ collapsedTokensPerTopic[i] + "!=" + uncollapsedTokensPerTopic[i], 
					collapsedTokensPerTopic[i], uncollapsedTokensPerTopic[i]);
			assertEquals("Collapsed and LightPCLDA token counts are not the same: " 
					+ collapsedTokensPerTopic[i] + "!=" + lightPcLdaTokensPerTopic[i], 
					collapsedTokensPerTopic[i], lightPcLdaTokensPerTopic[i]);
			assertEquals("Collapsed and CollapsedLight token counts are not the same: " 
					+ collapsedTokensPerTopic[i] + "!=" + collapsedlightTokensPerTopic[i], 
					collapsedTokensPerTopic[i], collapsedlightTokensPerTopic[i]);
			assertEquals("Collapsed and LightPCLDAtypeTopicProposal token counts are not the same: " 
					+ collapsedTokensPerTopic[i] + "!=" + lightPcLdaTTPTokensPerTopic[i], 
					collapsedTokensPerTopic[i], lightPcLdaTTPTokensPerTopic[i]);
		}

		double spaliasModelLogLikelihood   = spalias.modelLogLikelihood();
		double collapsedModelLogLikelihood   = collapsed.modelLogLikelihood();
		double uncollapsedModelLogLikelihood = uncollapsed.modelLogLikelihood();
		double adldaModelLogLikelihood       = adlda.modelLogLikelihood();
		double lightpcldaModelLogLikelihood  = lightpclda.modelLogLikelihood();
		double lightpcldaTTPModelLogLikelihood  = lightpcldaTTP.modelLogLikelihood();
		double collapsedlightModelLogLikelihood  = collapsedlight.modelLogLikelihood();
		
		assertEquals("ADLDA and Collapsed LogLikelihoods are not the same: " 
				+ adldaModelLogLikelihood + " != " + collapsedModelLogLikelihood 
				+ " Diff: " + (adldaModelLogLikelihood-collapsedModelLogLikelihood) ,
				adldaModelLogLikelihood,	collapsedModelLogLikelihood, epsilon);
		assertEquals("Spalias and Collapsed LogLikelihoods are not the same: " 
				+ spaliasModelLogLikelihood + " != " + collapsedModelLogLikelihood 
				+ " Diff: " + (spaliasModelLogLikelihood-collapsedModelLogLikelihood) ,
				spaliasModelLogLikelihood,	collapsedModelLogLikelihood, epsilon);
		assertEquals("Collapsed and UnCollapsed LogLikelihoods are not the same: " 
				+ collapsedModelLogLikelihood + " != " + uncollapsedModelLogLikelihood 
				+ " Diff: " + (collapsedModelLogLikelihood-uncollapsedModelLogLikelihood) ,
				collapsedModelLogLikelihood,	uncollapsedModelLogLikelihood, epsilon);
		assertEquals("Collapsed and LightPCLDA LogLikelihoods are not the same: " 
				+ collapsedModelLogLikelihood + " != " + lightpcldaModelLogLikelihood 
				+ " Diff: " + (collapsedModelLogLikelihood-lightpcldaModelLogLikelihood) ,
				collapsedModelLogLikelihood, lightpcldaModelLogLikelihood, epsilon);
		assertEquals("Collapsed and CollapsedLight LogLikelihoods are not the same: " 
				+ collapsedModelLogLikelihood + " != " + collapsedlightModelLogLikelihood 
				+ " Diff: " + (collapsedModelLogLikelihood-collapsedlightModelLogLikelihood) ,
				collapsedModelLogLikelihood, collapsedlightModelLogLikelihood, epsilon);
		assertEquals("Collapsed and LightPCLDAtypeTopicProposal LogLikelihoods are not the same: " 
				+ collapsedModelLogLikelihood + " != " + lightpcldaTTPModelLogLikelihood 
				+ " Diff: " + (collapsedModelLogLikelihood-lightpcldaTTPModelLogLikelihood) ,
				collapsedModelLogLikelihood, lightpcldaTTPModelLogLikelihood, epsilon);

		TestUtils.assertEqualArrays(collapsed.getTypeTopicCounts(), uncollapsed.getTypeTopicCounts());
	
		int [][] spaliasZIndicators   = spalias.getZIndicators();
		int [][] collapsedZIndicators   = collapsed.getZIndicators();
		int [][] uncollapsedZIndicators = uncollapsed.getZIndicators();
		int [][] adldaZIndicators       = adlda.getZIndicators();
		int [][] lightPcLdaZIndicators  = lightpclda.getZIndicators();
		int [][] lightPcLdaTTPZIndicators  = lightpcldaTTP.getZIndicators();
		int [][] collapsedlightZIndicators  = collapsedlight.getZIndicators();
		
		for (int i = 0; i < collapsedZIndicators.length; i++) {
			for (int j = 0; j < collapsedZIndicators[i].length; j++) {
				assertEquals("Collapsed and UnCollapsed ZIndicators are not the same: " 
						+ collapsedZIndicators[i][j] + "!=" + uncollapsedZIndicators[i][j], 
						collapsedZIndicators[i][j], uncollapsedZIndicators[i][j]);
				assertEquals("Collapsed and Spalias ZIndicators are not the same: " 
						+ collapsedZIndicators[i][j] + "!=" + spaliasZIndicators[i][j], 
						collapsedZIndicators[i][j], spaliasZIndicators[i][j]);
				assertEquals("Collapsed and ADLDA ZIndicators are not the same: " 
						+ collapsedZIndicators[i][j] + "!=" + adldaZIndicators[i][j], 
						collapsedZIndicators[i][j], adldaZIndicators[i][j]);
				assertEquals("Collapsed and LightPCLDA ZIndicators are not the same: " 
						+ collapsedZIndicators[i][j] + "!=" + lightPcLdaZIndicators[i][j], 
						collapsedZIndicators[i][j], lightPcLdaZIndicators[i][j]);
				assertEquals("Collapsed and LightPCLDA ZIndicators are not the same: " 
						+ collapsedZIndicators[i][j] + "!=" + collapsedlightZIndicators[i][j], 
						collapsedZIndicators[i][j], collapsedlightZIndicators[i][j]);
				assertEquals("Collapsed and LightPCLDAtypeTopicProposal ZIndicators are not the same: " 
						+ collapsedZIndicators[i][j] + "!=" + lightPcLdaTTPZIndicators[i][j], 
						collapsedZIndicators[i][j], lightPcLdaTTPZIndicators[i][j]);
			}
		}
	}
	
	@Test
	public void testEqualInitializationPercentageSplit() throws ParseException, ConfigurationException, IOException {
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
		config.setBatchBuildingScheme(BatchBuilderFactory.PERCENTAGE_SPLIT);
		config.setLoggingUtil(lu);
		config.activateSubconfig("demo-nips");
		double percentageSplit = 0.3;
		config.setDocPercentageSplitSize(percentageSplit);
		config.setTopicPercentageSplitSize(percentageSplit);


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

		UncollapsedParallelLDA uncollapsed 
		= new UncollapsedParallelLDA(config);
		uncollapsed.setRandomSeed(config.getSeed(LDAConfiguration.SEED_DEFAULT));
		uncollapsed.addInstances(instances);

		ADLDA adlda = new ADLDA(config);
		adlda.setRandomSeed(config.getSeed(LDAConfiguration.SEED_DEFAULT));
		adlda.addInstances(instances);

		int [][] collapsedTopicIndicators   = collapsed.getTypeTopicCounts();
		int [][] uncollapsedTopicIndicators = uncollapsed.getTypeTopicCounts();


		int [][] adldaTopicIndicators       = adlda.getTypeTopicCounts();

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
				adldaModelLogLikelihood,	collapsedModelLogLikelihood, epsilon);

		assertEquals("Collapsed and UnCollapsed LogLikelihoods are not the same: " 
				+ collapsedModelLogLikelihood + " != " + uncollapsedModelLogLikelihood 
				+ " Diff: " + (collapsedModelLogLikelihood-uncollapsedModelLogLikelihood) ,
				collapsedModelLogLikelihood,	uncollapsedModelLogLikelihood, epsilon);
		
		TestUtils.assertEqualArrays(collapsed.getTypeTopicCounts(), uncollapsed.getTypeTopicCounts());
	}

	@Test
	public void testLoadRareWords() throws UnsupportedEncodingException, FileNotFoundException {
		String dataset_fn = "src/main/resources/datasets/SmallTexts.txt";
		InstanceList nonPrunedInstances = LDAUtils.loadInstances(dataset_fn, "stoplist.txt",0);
		System.out.println(LDAUtils.instancesToString(nonPrunedInstances));
		System.out.println("Non pruned Alphabet size: " + nonPrunedInstances.getDataAlphabet().size());
		System.out.println("No. instances: " + nonPrunedInstances.size());
		
		InstanceList originalInstances = LDAUtils.loadInstances(dataset_fn, "stoplist.txt",2);	
		System.out.println("Alphabet size: " + originalInstances.getDataAlphabet().size());
		System.out.println(LDAUtils.instancesToString(originalInstances));
		System.out.println("No. instances: " + originalInstances.size());
		
		int [] wordCounts = {0,3,3,0,0};
		int idx = 0;
		for(Instance instance : originalInstances) {
			FeatureSequence fs = (FeatureSequence) instance.getData();
			// This assertion would fail for eventhough the feature sequence
			// is "empty" the underlying array is 2 long.
			//assertEquals(wordCounts[idx++], fs.getFeatures().length);
			assertEquals(wordCounts[idx++], fs.size());
		}
	}
	
	/*
	@Test
	public void testHandleRareWords() throws UnsupportedEncodingException, FileNotFoundException {
		String dataset_fn = "src/main/resources/datasets/SmallTexts.txt";
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
				startDiagnosticOutput,4711,dataset_fn);
		InstanceList prunedInstances = LDAUtils.loadInstances(dataset_fn, "stoplist.txt",2);
		
		System.out.println(LDAUtils.instancesToString(prunedInstances));
		
		UncollapsedParallelLDA uncollapsed = new UncollapsedParallelLDA(config);
		uncollapsed.setRandomSeed(config.getSeed(LDAConfiguration.SEED_DEFAULT));
		uncollapsed.addInstances(prunedInstances);
		
		int [] indices    = {0,1,2,3,4};
		int [] wordCounts = {0,3,3,0,0};
		int [][] batch = uncollapsed.createBatch(indices);
		
		int idx = 0;
		for (int i = 0; i < batch.length; i++) {
			for (int j = 0; j < batch[j].length; j++) {			
				if(i%2==0) {
					//System.out.print(prunedInstances.getAlphabet().lookupObject(batch[i][j]) + ", ");
					assertEquals(wordCounts[idx++], batch[i].length);
				} 
				else { 
					System.out.print(batch[i][j] + ", ");
				}
			}
			System.out.println();
		}	
	}
	
	@Test
	public void testHandleLargeRareWords() throws UnsupportedEncodingException, FileNotFoundException {
		String dataset_fn = "src/main/resources/datasets/enron.txt";
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
				startDiagnosticOutput,4711,dataset_fn);
		InstanceList prunedInstances = LDAUtils.loadInstances(dataset_fn, "stoplist.txt",5000);
		
		int docIdx = 0;
		for (Instance instance : prunedInstances) {
			String doc = LDAUtils.instanceToString(instance, -1);
			System.out.println("Doc " + docIdx++ + ":  " + doc);
			if(doc.equals("<empty doc>")) {
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			System.out.println();
		}
		
		UncollapsedParallelLDA uncollapsed = new UncollapsedParallelLDA(config);
		uncollapsed.setRandomSeed(config.getSeed(LDAConfiguration.SEED_DEFAULT));
		uncollapsed.addInstances(prunedInstances);
		
		int [] indices    = {0,1,2,3,4};
		int [] wordCounts = {0,3,3,0,0};
		int [][] batch = uncollapsed.createBatch(indices);
		
		int idx = 0;
		for (int i = 0; i < batch.length; i++) {
			for (int j = 0; j < batch[j].length; j++) {			
				if(i%2==0) {
					//System.out.print(prunedInstances.getAlphabet().lookupObject(batch[i][j]) + ", ");
					assertEquals(wordCounts[idx++], batch[i].length);
				} 
				else { 
					System.out.print(batch[i][j] + ", ");
				}
			}
			System.out.println();
		}
		
		
	}*/


	@Test
	public void testSettingZIndicators() throws IOException {
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

		System.out.println("TTCounts 1 ok!");

		double collapsedModelLogLikelihood   = collapsed.modelLogLikelihood();
		double uncollapsedModelLogLikelihood = uncollapsed.modelLogLikelihood();

		TestUtils.assertEqualArrays(collapsed.getTypeTopicCounts(), uncollapsed.getTypeTopicCounts());

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
		
		// Sample 5 iterations to change z
		uncollapsed.sample(5);
		
		int[][] uncollapsedZas = uncollapsed.getZIndicators();
		collapsed.setZIndicators(uncollapsedZas);
		int [][] setZascollapsed = collapsed.getZIndicators();
		
		TestUtils.assertEqualArrays(uncollapsedZas, setZascollapsed);
		
		
		System.out.println("Z indicators are equal...");
		
		//printTTCounts(collapsed.getTypeTopicCounts(),uncollapsed.getTypeTopicCounts());
		
		TestUtils.assertEqualArrays(collapsed.getTypeTopicCounts(), uncollapsed.getTypeTopicCounts());

		collapsedModelLogLikelihood   = collapsed.modelLogLikelihood();
		uncollapsedModelLogLikelihood = uncollapsed.modelLogLikelihood();

		assertEquals("Collapsed and UnCollapsed LogLikelihoods are not the same: " 
				+ collapsedModelLogLikelihood + " != " + uncollapsedModelLogLikelihood 
				+ " Diff: " + (collapsedModelLogLikelihood-uncollapsedModelLogLikelihood) ,
				collapsedModelLogLikelihood,	uncollapsedModelLogLikelihood, epsilon);
	}

	public void printTTCounts(int[][] arr1, int[][] arr2) {
		for (int i = 0; i < arr1.length; i++) {
			for (int j = 0; j < arr1[i].length; j++) {
				System.out.print(arr1[i][j] + " =? " +  arr2[i][j] + "; ");
			}
			System.out.println();
		}
	}


}
