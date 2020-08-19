package cc.mallet.topics;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.cli.ParseException;
import org.apache.commons.configuration.ConfigurationException;
import org.junit.Test;

import cc.mallet.configuration.LDACommandLineParser;
import cc.mallet.configuration.LDAConfiguration;
import cc.mallet.configuration.ParsedLDAConfiguration;
import cc.mallet.configuration.SimpleLDAConfiguration;
import cc.mallet.types.InstanceList;
import cc.mallet.util.LDALoggingUtils;
import cc.mallet.util.LDAUtils;
import cc.mallet.util.LoggingUtils;
import cc.mallet.utils.TestUtils;

public class ReadWriteTest {

	@Test
	public void testWRWithSimpleConfigUncollapsed() throws IOException {
		String whichModel = "uncollapsed";
		Integer numTopics = 20;
		Double alphaSum = 1.0; 
		Double beta = 0.01;
		Integer numIter = 100;
		Integer numBatches = 6;
		Integer rareWordThreshold = 0;
		Integer showTopicsInterval = 50;
		Integer startDiagnosticOutput = 500;
		
		SimpleLDAConfiguration config = new SimpleLDAConfiguration(new LoggingUtils(), whichModel,
				numTopics, alphaSum, beta, numIter,
				numBatches, rareWordThreshold, showTopicsInterval,
				startDiagnosticOutput,4711,"src/main/resources/datasets/nips.txt");

		String objectFile = File.createTempFile("lda_rw_unttest", ".bin").getAbsolutePath();
		
		double uncollapsedModelLogLikelihood;
		
		UncollapsedParallelLDA uncollapsed;
		{
			LDALoggingUtils lu = new LoggingUtils();
			lu.checkAndCreateCurrentLogDir("Runs");
			config.setLoggingUtil(lu);
			config.activateSubconfig("demo-nips");

			System.out.println("Using Config: " + config.whereAmI());

			String dataset_fn = config.getDatasetFilename();
			System.out.println("Using dataset: " + dataset_fn);
			System.out.println("Scheme: " + whichModel);

			InstanceList instances = LDAUtils.loadInstances(dataset_fn, 
					"stoplist.txt", config.getRareThreshold(LDAConfiguration.RARE_WORD_THRESHOLD));

			uncollapsed = new UncollapsedParallelLDA(config);
			uncollapsed.setRandomSeed(config.getSeed(LDAConfiguration.SEED_DEFAULT));
			uncollapsed.addInstances(instances);


			uncollapsed.sample(config.getNoIterations(20));
			uncollapsedModelLogLikelihood = uncollapsed.modelLogLikelihood();

			System.out.println("Finished sampling!");
			
			uncollapsed.write(new File(objectFile));
		}
		
		{
			try {
				UncollapsedParallelLDA model = UncollapsedParallelLDA.read(new File(objectFile));
				double readModelLogLikelihood = model.modelLogLikelihood();
				
				assertEquals(uncollapsedModelLogLikelihood, readModelLogLikelihood, 0.00000000001);
				
				InstanceList instances = model.getDataset();
				
				File lgDir = model.getConfiguration().getLoggingUtil().getLogDir();
				assertEquals(lgDir, uncollapsed.getConfiguration().getLoggingUtil().getLogDir());
				
				int requestedWords = model.getConfiguration().getNrTopWords(LDAConfiguration.NO_TOP_WORDS_DEFAULT);
				assertEquals(requestedWords, uncollapsed.getConfiguration().getNrTopWords(LDAConfiguration.NO_TOP_WORDS_DEFAULT));
				
				double [][] means = model.getZbar();
				double [][] orig_means = uncollapsed.getZbar();

				TestUtils.assertEqualArrays(means, orig_means, 0.000000001);
					
				double [][] theta_means = model.getThetaEstimate();
				double [][] orig_theta_means = uncollapsed.getThetaEstimate();

				TestUtils.assertEqualArrays(theta_means, orig_theta_means, 0.000000001);

				double [][] phi_means = model.getPhi();
				double [][] orig_phi_means = uncollapsed.getPhi();
				TestUtils.assertEqualArrays(phi_means, orig_phi_means, 0.000000001);						

				String [] vocabulary = LDAUtils.extractVocabulaty(instances.getDataAlphabet());
				String [] orig_vocabulary = LDAUtils.extractVocabulaty(uncollapsed.getDataset().getDataAlphabet());
				
				assertArrayEquals(vocabulary, orig_vocabulary);
					
				int [][] corpus = LDAUtils.extractCorpus(instances);
				int [][] orig_corpus = LDAUtils.extractCorpus(uncollapsed.getDataset());

				assertArrayEquals(corpus, orig_corpus);
				
				int [] freqs = LDAUtils.extractTermCounts(instances);
				int [] orig_freqs = LDAUtils.extractTermCounts(uncollapsed.getDataset());
				
				assertArrayEquals(freqs, orig_freqs);
				
				int [] doc_freqs = LDAUtils.extractDocLength(instances);
				int [] orig_doc_freqs = LDAUtils.extractDocLength(uncollapsed.getDataset());
				
				assertArrayEquals(doc_freqs, orig_doc_freqs);
				
				String [][] tw = LDAUtils.getTopWords(requestedWords, 
						model.getAlphabet().size(), 
						model.getNoTopics(), 
						model.getTypeTopicMatrix(), 
						model.getAlphabet());

				String [][] orig_tw = LDAUtils.getTopWords(requestedWords, 
						uncollapsed.getAlphabet().size(), 
						uncollapsed.getNoTopics(), 
						uncollapsed.getTypeTopicMatrix(), 
						uncollapsed.getAlphabet());

				assertArrayEquals(tw, orig_tw);
								
			} catch (Exception e) {
				fail("Could not read serialized model from file: " + objectFile);
			}
		}
	}
	
	
	@Test
	public void testWRWithParsedConfigUncollapsed() throws IOException, ParseException, ConfigurationException {
		String [] args = {"--run_cfg=src/main/resources/configuration/UnitTestConfig.cfg"};

		LDACommandLineParser cp = new LDACommandLineParser(args);
		LDAConfiguration config = new ParsedLDAConfiguration(cp);
		
		config.activateSubconfig("demo");
		
		String objectFile = File.createTempFile("lda_rw_unttest", ".bin").getAbsolutePath();
		
		double uncollapsedModelLogLikelihood;
		
		UncollapsedParallelLDA uncollapsed;
		{
			LDALoggingUtils lu = new LoggingUtils();
			lu.checkAndCreateCurrentLogDir("Runs");
			config.setLoggingUtil(lu);
			config.activateSubconfig("demo");

			System.out.println("Using Config: " + config.whereAmI());

			String dataset_fn = config.getDatasetFilename();
			System.out.println("Using dataset: " + dataset_fn);

			InstanceList instances = LDAUtils.loadInstances(dataset_fn, 
					"stoplist.txt", config.getRareThreshold(LDAConfiguration.RARE_WORD_THRESHOLD));

			uncollapsed = new UncollapsedParallelLDA(config);
			uncollapsed.setRandomSeed(config.getSeed(LDAConfiguration.SEED_DEFAULT));
			uncollapsed.addInstances(instances);


			uncollapsed.sample(20);
			uncollapsedModelLogLikelihood = uncollapsed.modelLogLikelihood();

			System.out.println("Finished sampling!");
			
			uncollapsed.write(new File(objectFile));
		}
		
		{
			try {
				UncollapsedParallelLDA model = UncollapsedParallelLDA.read(new File(objectFile));
				double readModelLogLikelihood = model.modelLogLikelihood();
				
				assertEquals(uncollapsedModelLogLikelihood, readModelLogLikelihood, 0.00000000001);
				
				InstanceList instances = model.getDataset();
				
				File lgDir = model.getConfiguration().getLoggingUtil().getLogDir();
				assertTrue(lgDir != null);
				
				int requestedWords = model.getConfiguration().getNrTopWords(LDAConfiguration.NO_TOP_WORDS_DEFAULT);
				assertEquals(requestedWords, uncollapsed.getConfiguration().getNrTopWords(LDAConfiguration.NO_TOP_WORDS_DEFAULT));
				
				double [][] means = model.getZbar();
				double [][] orig_means = uncollapsed.getZbar();

				TestUtils.assertEqualArrays(means, orig_means, 0.000000001);
					
				double [][] theta_means = model.getThetaEstimate();
				double [][] orig_theta_means = uncollapsed.getThetaEstimate();

				TestUtils.assertEqualArrays(theta_means, orig_theta_means, 0.000000001);

				double [][] phi_means = model.getPhi();
				double [][] orig_phi_means = uncollapsed.getPhi();
				TestUtils.assertEqualArrays(phi_means, orig_phi_means, 0.000000001);						

				String [] vocabulary = LDAUtils.extractVocabulaty(instances.getDataAlphabet());
				String [] orig_vocabulary = LDAUtils.extractVocabulaty(uncollapsed.getDataset().getDataAlphabet());
				
				assertArrayEquals(vocabulary, orig_vocabulary);
					
				int [][] corpus = LDAUtils.extractCorpus(instances);
				int [][] orig_corpus = LDAUtils.extractCorpus(uncollapsed.getDataset());

				assertArrayEquals(corpus, orig_corpus);
				
				int [] freqs = LDAUtils.extractTermCounts(instances);
				int [] orig_freqs = LDAUtils.extractTermCounts(uncollapsed.getDataset());
				
				assertArrayEquals(freqs, orig_freqs);
				
				int [] doc_freqs = LDAUtils.extractDocLength(instances);
				int [] orig_doc_freqs = LDAUtils.extractDocLength(uncollapsed.getDataset());
				
				assertArrayEquals(doc_freqs, orig_doc_freqs);
				
				String [][] tw = LDAUtils.getTopWords(requestedWords, 
						model.getAlphabet().size(), 
						model.getNoTopics(), 
						model.getTypeTopicMatrix(), 
						model.getAlphabet());

				String [][] orig_tw = LDAUtils.getTopWords(requestedWords, 
						uncollapsed.getAlphabet().size(), 
						uncollapsed.getNoTopics(), 
						uncollapsed.getTypeTopicMatrix(), 
						uncollapsed.getAlphabet());
				
				assertArrayEquals(tw, orig_tw);
								
			} catch (Exception e) {
				e.printStackTrace();
				fail("Could not read serialized model from file: " + objectFile);
			}
		}
	}
	
	
	
	@Test
	public void testWRWithSimpleConfigHDP() throws IOException {
		String whichModel = "uncollapsed";
		Integer numTopics = 20;
		Double alphaSum = 1.0; 
		Double beta = 0.01;
		Integer numIter = 100;
		Integer numBatches = 6;
		Integer rareWordThreshold = 0;
		Integer showTopicsInterval = 50;
		Integer startDiagnosticOutput = 500;
		
		SimpleLDAConfiguration config = new SimpleLDAConfiguration(new LoggingUtils(), whichModel,
				numTopics, alphaSum, beta, numIter,
				numBatches, rareWordThreshold, showTopicsInterval,
				startDiagnosticOutput,4711,"src/main/resources/datasets/nips.txt");

		String objectFile = File.createTempFile("lda_rw_unttest", ".bin").getAbsolutePath();
		
		double uncollapsedModelLogLikelihood;
		
		PoissonPolyaUrnHDPLDAInfiniteTopics origHDP;
		{
			LDALoggingUtils lu = new LoggingUtils();
			lu.checkAndCreateCurrentLogDir("Runs");
			config.setLoggingUtil(lu);
			config.activateSubconfig("demo-nips");

			System.out.println("Using Config: " + config.whereAmI());

			String dataset_fn = config.getDatasetFilename();
			System.out.println("Using dataset: " + dataset_fn);
			System.out.println("Scheme: " + whichModel);

			InstanceList instances = LDAUtils.loadInstances(dataset_fn, 
					"stoplist.txt", config.getRareThreshold(LDAConfiguration.RARE_WORD_THRESHOLD));

			origHDP = new PoissonPolyaUrnHDPLDAInfiniteTopics(config);
			origHDP.setRandomSeed(config.getSeed(LDAConfiguration.SEED_DEFAULT));
			origHDP.addInstances(instances);


			origHDP.sample(config.getNoIterations(20));
			uncollapsedModelLogLikelihood = origHDP.modelLogLikelihood();

			System.out.println("Finished sampling!");
			
			origHDP.write(new File(objectFile));
		}
		
		{
			try {
				PoissonPolyaUrnHDPLDAInfiniteTopics model = PoissonPolyaUrnHDPLDAInfiniteTopics.read(new File(objectFile));
				double readModelLogLikelihood = model.modelLogLikelihood();
				
				assertEquals(uncollapsedModelLogLikelihood, readModelLogLikelihood, 0.00000000001);
				
				InstanceList instances = model.getDataset();
				
				File lgDir = model.getConfiguration().getLoggingUtil().getLogDir();
				assertEquals(lgDir, origHDP.getConfiguration().getLoggingUtil().getLogDir());
				
				int requestedWords = model.getConfiguration().getNrTopWords(LDAConfiguration.NO_TOP_WORDS_DEFAULT);
				assertEquals(requestedWords, origHDP.getConfiguration().getNrTopWords(LDAConfiguration.NO_TOP_WORDS_DEFAULT));
				
				double [][] means = model.getZbar();
				double [][] orig_means = origHDP.getZbar();

				TestUtils.assertEqualArrays(means, orig_means, 0.000000001);
					
				double [][] theta_means = model.getThetaEstimate();
				double [][] orig_theta_means = origHDP.getThetaEstimate();

				TestUtils.assertEqualArrays(theta_means, orig_theta_means, 0.000000001);

				double [][] phi_means = model.getPhi();
				double [][] orig_phi_means = origHDP.getPhi();
				TestUtils.assertEqualArrays(phi_means, orig_phi_means, 0.000000001);						

				String [] vocabulary = LDAUtils.extractVocabulaty(instances.getDataAlphabet());
				String [] orig_vocabulary = LDAUtils.extractVocabulaty(origHDP.getDataset().getDataAlphabet());
				
				assertArrayEquals(vocabulary, orig_vocabulary);
					
				int [][] corpus = LDAUtils.extractCorpus(instances);
				int [][] orig_corpus = LDAUtils.extractCorpus(origHDP.getDataset());

				assertArrayEquals(corpus, orig_corpus);
				
				int [] freqs = LDAUtils.extractTermCounts(instances);
				int [] orig_freqs = LDAUtils.extractTermCounts(origHDP.getDataset());
				
				assertArrayEquals(freqs, orig_freqs);
				
				int [] doc_freqs = LDAUtils.extractDocLength(instances);
				int [] orig_doc_freqs = LDAUtils.extractDocLength(origHDP.getDataset());
				
				assertArrayEquals(doc_freqs, orig_doc_freqs);
				
				String [][] tw = LDAUtils.getTopWords(requestedWords, 
						model.getAlphabet().size(), 
						model.getNoTopics(), 
						model.getTypeTopicMatrix(), 
						model.getAlphabet());

				String [][] orig_tw = LDAUtils.getTopWords(requestedWords, 
						origHDP.getAlphabet().size(), 
						origHDP.getNoTopics(), 
						origHDP.getTypeTopicMatrix(), 
						origHDP.getAlphabet());

				assertArrayEquals(tw, orig_tw);
				
				int [] occur_count = model.getTopicOcurrenceCount();
				int [] orig_occur_count = origHDP.getTopicOcurrenceCount();
				
				assertArrayEquals(occur_count, orig_occur_count);
				
				List<Integer> hist = model.getActiveTopicHistory();
				List<Integer> orig_hist = origHDP.getActiveTopicHistory();
				
				assertTrue(hist.equals(orig_hist));
				
				List<Integer> data_hist = model.getActiveTopicInDataHistory();
				List<Integer> orig_data_hist = origHDP.getActiveTopicInDataHistory();
				
				assertTrue(data_hist.equals(orig_data_hist));
								
			} catch (Exception e) {
				fail("Could not read serialized model from file: " + objectFile);
			}
		}
	}
	
	
	@Test
	public void testWRWithParsedConfigHDP() throws IOException, ParseException, ConfigurationException {
		String [] args = {"--run_cfg=src/main/resources/configuration/UnitTestConfig.cfg"};

		LDACommandLineParser cp = new LDACommandLineParser(args);
		LDAConfiguration config = new ParsedLDAConfiguration(cp);
		
		config.activateSubconfig("demo");
		
		String objectFile = File.createTempFile("lda_rw_unttest", ".bin").getAbsolutePath();
		
		double uncollapsedModelLogLikelihood;
		
		PoissonPolyaUrnHDPLDAInfiniteTopics origHDP;
		{
			LDALoggingUtils lu = new LoggingUtils();
			lu.checkAndCreateCurrentLogDir("Runs");
			config.setLoggingUtil(lu);
			config.activateSubconfig("demo");

			System.out.println("Using Config: " + config.whereAmI());

			String dataset_fn = config.getDatasetFilename();
			System.out.println("Using dataset: " + dataset_fn);

			InstanceList instances = LDAUtils.loadInstances(dataset_fn, 
					"stoplist.txt", config.getRareThreshold(LDAConfiguration.RARE_WORD_THRESHOLD));

			origHDP = new PoissonPolyaUrnHDPLDAInfiniteTopics(config);
			origHDP.setRandomSeed(config.getSeed(LDAConfiguration.SEED_DEFAULT));
			origHDP.addInstances(instances);


			origHDP.sample(20);
			uncollapsedModelLogLikelihood = origHDP.modelLogLikelihood();

			System.out.println("Finished sampling!");
			
			origHDP.write(new File(objectFile));
		}
		
		{
			try {
				PoissonPolyaUrnHDPLDAInfiniteTopics model = PoissonPolyaUrnHDPLDAInfiniteTopics.read(new File(objectFile));
				double readModelLogLikelihood = model.modelLogLikelihood();
				
				assertEquals(uncollapsedModelLogLikelihood, readModelLogLikelihood, 0.00000000001);
				
				InstanceList instances = model.getDataset();
				
				File lgDir = model.getConfiguration().getLoggingUtil().getLogDir();
				assertTrue(lgDir != null);
				
				int requestedWords = model.getConfiguration().getNrTopWords(LDAConfiguration.NO_TOP_WORDS_DEFAULT);
				assertEquals(requestedWords, origHDP.getConfiguration().getNrTopWords(LDAConfiguration.NO_TOP_WORDS_DEFAULT));
				
				double [][] means = model.getZbar();
				double [][] orig_means = origHDP.getZbar();

				TestUtils.assertEqualArrays(means, orig_means, 0.000000001);
					
				double [][] theta_means = model.getThetaEstimate();
				double [][] orig_theta_means = origHDP.getThetaEstimate();

				TestUtils.assertEqualArrays(theta_means, orig_theta_means, 0.000000001);

				double [][] phi_means = model.getPhi();
				double [][] orig_phi_means = origHDP.getPhi();
				TestUtils.assertEqualArrays(phi_means, orig_phi_means, 0.000000001);						

				String [] vocabulary = LDAUtils.extractVocabulaty(instances.getDataAlphabet());
				String [] orig_vocabulary = LDAUtils.extractVocabulaty(origHDP.getDataset().getDataAlphabet());
				
				assertArrayEquals(vocabulary, orig_vocabulary);
					
				int [][] corpus = LDAUtils.extractCorpus(instances);
				int [][] orig_corpus = LDAUtils.extractCorpus(origHDP.getDataset());

				assertArrayEquals(corpus, orig_corpus);
				
				int [] freqs = LDAUtils.extractTermCounts(instances);
				int [] orig_freqs = LDAUtils.extractTermCounts(origHDP.getDataset());
				
				assertArrayEquals(freqs, orig_freqs);
				
				int [] doc_freqs = LDAUtils.extractDocLength(instances);
				int [] orig_doc_freqs = LDAUtils.extractDocLength(origHDP.getDataset());
				
				assertArrayEquals(doc_freqs, orig_doc_freqs);
				
				String [][] tw = LDAUtils.getTopWords(requestedWords, 
						model.getAlphabet().size(), 
						model.getNoTopics(), 
						model.getTypeTopicMatrix(), 
						model.getAlphabet());

				String [][] orig_tw = LDAUtils.getTopWords(requestedWords, 
						origHDP.getAlphabet().size(), 
						origHDP.getNoTopics(), 
						origHDP.getTypeTopicMatrix(), 
						origHDP.getAlphabet());
				
				int [] occur_count = model.getTopicOcurrenceCount();
				int [] orig_occur_count = origHDP.getTopicOcurrenceCount();
				
				assertArrayEquals(occur_count, orig_occur_count);
				
				List<Integer> hist = model.getActiveTopicHistory();
				List<Integer> orig_hist = origHDP.getActiveTopicHistory();
				
				assertTrue(hist.equals(orig_hist));
				
				List<Integer> data_hist = model.getActiveTopicInDataHistory();
				List<Integer> orig_data_hist = origHDP.getActiveTopicInDataHistory();
				
				assertTrue(data_hist.equals(orig_data_hist));

				assertArrayEquals(tw, orig_tw);
								
			} catch (Exception e) {
				e.printStackTrace();
				fail("Could not read serialized model from file: " + objectFile);
			}
		}
	}
}
