package cc.mallet.topics;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;

import cc.mallet.configuration.LDAConfiguration;
import cc.mallet.configuration.SimpleLDAConfiguration;
import cc.mallet.types.InstanceList;
import cc.mallet.util.LDAUtils;
import cc.mallet.util.LoggingUtils;

public class PolyaUrnSpaliasTest {
	
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
	public void testGetPhiMeans() throws IOException {
		String whichModel = "polyaurn";
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

		LDAGibbsSampler model = new PolyaUrnSpaliasLDA(config);
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
		assertEquals(numIter - burnInIter, ((PolyaUrnSpaliasLDA)model).getNoSampledPhi()); 
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
	

}
