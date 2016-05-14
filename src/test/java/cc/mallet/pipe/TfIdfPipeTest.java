package cc.mallet.pipe;

import static org.junit.Assert.assertEquals;
import gnu.trove.TIntDoubleHashMap;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Map;

import org.junit.Test;

import cc.mallet.configuration.LDAConfiguration;
import cc.mallet.configuration.SimpleLDAConfiguration;
import cc.mallet.types.Alphabet;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.util.LDAUtils;
import cc.mallet.util.LoggingUtils;

public class TfIdfPipeTest {

	int seed = 20150326;
	Integer numTopics = 20;
	Double alpha = 0.1; 
	Double beta = 0.01;
	Integer numIter = 1000;
	Integer numBatches = 4;
	Integer rareWordThreshold = 0;
	Integer showTopicsInterval = 50;
	Integer startDiagnosticOutput = 500;

	SimpleLDAConfiguration config = new SimpleLDAConfiguration(new LoggingUtils(), "ALL",
			numTopics, alpha, beta, numIter,
			numBatches, rareWordThreshold, showTopicsInterval,
			startDiagnosticOutput,seed,"src/main/resources/datasets/tfidf-samples.txt");
	
	LoggingUtils lu = new LoggingUtils();

	
	@Test
	public void testTf() throws FileNotFoundException {
		lu.checkAndCreateCurrentLogDir("Runs");
		config.setLoggingUtil(lu);
		config.activateSubconfig("demo-nips");

		String dataset_fn = config.getDatasetFilename();

		InstanceList instances = LDAUtils.loadInstances(dataset_fn, 
				null, config.getRareThreshold(LDAConfiguration.RARE_WORD_THRESHOLD));
		
		TfIdfPipe tfIdfPipe = new TfIdfPipe(instances.getDataAlphabet(), instances.getTargetAlphabet());
		
		for (Instance instance : instances) {
			tfIdfPipe.pipe(instance);
		}
		
		
		Map<String,Integer> expectedTfs = new java.util.HashMap<String, Integer>();
		expectedTfs.put("this", 2);
		expectedTfs.put("is", 2);
		expectedTfs.put("a", 2);
		expectedTfs.put("sample", 1);
		expectedTfs.put("another", 2);
		expectedTfs.put("example", 3);
		
		for (String key : expectedTfs.keySet()) {
			int featureIdx = instances.getDataAlphabet().lookupIndex(key);
			assertEquals(expectedTfs.get(key).intValue(),tfIdfPipe.getTf(featureIdx));
		}
		
		
		//TIntDoubleHashMap tfidfs = tfIdfPipe.getTfIdf();
		/*int [] ranks = tfIdfPipe.freqSortWords(tfidfs, instances.getDataAlphabet());
		
		for(int cnt = 0; cnt < ranks.length; cnt++) {
			System.out.println("Feature: " + instances.getDataAlphabet().lookupObject(ranks[cnt])  + " => " + tfidfs.get(ranks[cnt]));
		}*/
	}
	
	@Test
	public void testIdf() throws FileNotFoundException {
		lu.checkAndCreateCurrentLogDir("Runs");
		config.setLoggingUtil(lu);
		config.activateSubconfig("demo-nips");

		String dataset_fn = config.getDatasetFilename();

		InstanceList instances = LDAUtils.loadInstances(dataset_fn, 
				null, config.getRareThreshold(LDAConfiguration.RARE_WORD_THRESHOLD));
		
		TfIdfPipe tfIdfPipe = new TfIdfPipe(instances.getDataAlphabet(), instances.getTargetAlphabet());
		
		for (Instance instance : instances) {
			tfIdfPipe.pipe(instance);
		}
		
		Map<String,Integer> expectedTfs = new java.util.HashMap<String, Integer>();
		expectedTfs.put("this", 2);
		expectedTfs.put("is", 2);
		expectedTfs.put("a", 2);
		expectedTfs.put("sample", 1);
		expectedTfs.put("another", 1);
		expectedTfs.put("example", 1);
		
		for (String key : expectedTfs.keySet()) {
			int featureIdx = instances.getDataAlphabet().lookupIndex(key);
			assertEquals(expectedTfs.get(key).intValue(),tfIdfPipe.getIdf(featureIdx));
		}
	}
	
	@Test
	public void testRank() throws FileNotFoundException {
		lu.checkAndCreateCurrentLogDir("Runs");
		config.setLoggingUtil(lu);
		config.activateSubconfig("demo-nips");

		String dataset_fn = config.getDatasetFilename();

		InstanceList instances = LDAUtils.loadInstances(dataset_fn, 
				null, config.getRareThreshold(LDAConfiguration.RARE_WORD_THRESHOLD));
		
		TfIdfPipe tfIdfPipe = new TfIdfPipe(instances.getDataAlphabet(), instances.getTargetAlphabet());
		
		for (Instance instance : instances) {
			tfIdfPipe.pipe(instance);
		}
		
		
		Map<String,Integer> expectedRank = new java.util.HashMap<String, Integer>();
		expectedRank.put("this", 5);
		expectedRank.put("is", 4);
		expectedRank.put("a", 3);
		expectedRank.put("sample", 2);
		expectedRank.put("another", 1);
		expectedRank.put("example", 0);
		
		TIntDoubleHashMap tfidfs = tfIdfPipe.getTfIdf();
		int [] ranks = tfIdfPipe.freqSortWords(tfidfs, instances.getDataAlphabet());
		
		for (String key : expectedRank.keySet()) {
			int featureIdx = instances.getDataAlphabet().lookupIndex(key);
			assertEquals(expectedRank.get(key).intValue(),ranks[featureIdx]);
		}	
	}
	
	@Test
	public void testCutoff() throws FileNotFoundException {
		
		final int KEEP_CNT = 2;
		
		lu.checkAndCreateCurrentLogDir("Runs");
		config.setLoggingUtil(lu);
		config.activateSubconfig("demo-nips");

		String dataset_fn = config.getDatasetFilename();

		InstanceList instances = LDAUtils.loadInstances(dataset_fn, 
				null, config.getRareThreshold(LDAConfiguration.RARE_WORD_THRESHOLD));
		
		TfIdfPipe tfIdfPipe = new TfIdfPipe(instances.getDataAlphabet(), instances.getTargetAlphabet());
		
		for (Instance instance : instances) {
			tfIdfPipe.pipe(instance);
		}
		
		File stopfile = new File("stoplist-empty.txt");
		StoppingSimpleTokenizer st = new StoppingSimpleTokenizer(stopfile);
		st.setDataAlphabet(instances.getDataAlphabet());
		tfIdfPipe.addPrunedWordsToStoplist(st, KEEP_CNT);
				
		int nonStopCnt = 0;
		for (int i = 0; i < instances.getDataAlphabet().size(); i++) {
			if(!st.isStopping(i)) {
				nonStopCnt++;
			}
		}
		assertEquals(KEEP_CNT,nonStopCnt);
	}
	
	class StoppingSimpleTokenizer extends SimpleTokenizer {
		private static final long serialVersionUID = 1L;

		public StoppingSimpleTokenizer(File stopfile) {
			super(stopfile);
		}
		
		boolean isStopping(int featureIdx) {
			return stoplist.contains(dataAlphabet.lookupObject(featureIdx));
		}
	}
	
	@Test
	public void testPrunedAlphabet() throws FileNotFoundException {
		
		final int KEEP_CNT = 3;
		
		lu.checkAndCreateCurrentLogDir("Runs");
		config.setLoggingUtil(lu);
		config.activateSubconfig("demo-nips");

		String dataset_fn = config.getDatasetFilename();

		InstanceList instances = LDAUtils.loadInstances(dataset_fn, 
				null, config.getRareThreshold(LDAConfiguration.RARE_WORD_THRESHOLD));
		
		TfIdfPipe tfIdfPipe = new TfIdfPipe(instances.getDataAlphabet(), instances.getTargetAlphabet());
		
		for (Instance instance : instances) {
			tfIdfPipe.pipe(instance);
		}
		
		Alphabet pruned = tfIdfPipe.getPrunedAlphabet(KEEP_CNT);
		assertEquals(KEEP_CNT, pruned.size());
	}
	
	@Test
	public void testLoadInstances() throws FileNotFoundException {
		
		final int KEEP_CNT = 3;
		
		lu.checkAndCreateCurrentLogDir("Runs");
		config.setLoggingUtil(lu);
		config.activateSubconfig("demo-nips");

		String dataset_fn = config.getDatasetFilename();

		InstanceList instances = LDAUtils.loadInstancesKeep(dataset_fn, 
				null, KEEP_CNT, true);
		
		assertEquals(KEEP_CNT, instances.getDataAlphabet().size());
	}

	@Test
	public void testLoadInstancesNegCnt() throws FileNotFoundException {
		
		final int KEEP_CNT = -1;
		
		lu.checkAndCreateCurrentLogDir("Runs");
		config.setLoggingUtil(lu);
		config.activateSubconfig("demo-nips");

		String dataset_fn = config.getDatasetFilename();

		InstanceList instances = LDAUtils.loadInstancesKeep(dataset_fn, 
				null, KEEP_CNT, true);
		
		assertEquals(6, instances.getDataAlphabet().size());
	}
	
	public static void main(String [] args) throws FileNotFoundException {
		int seed = 20150326;
		Integer numTopics = 20;
		Double alpha = 0.1; 
		Double beta = 0.01;
		Integer numIter = 1000;
		Integer numBatches = 4;
		Integer rareWordThreshold = 0;
		Integer showTopicsInterval = 50;
		Integer startDiagnosticOutput = 500;
		final int KEEP_CNT = 2000;
		
		SimpleLDAConfiguration nipsCfg = new SimpleLDAConfiguration(new LoggingUtils(), "ALL",
				numTopics, alpha, beta, numIter,
				numBatches, rareWordThreshold, showTopicsInterval,
				startDiagnosticOutput,seed,"src/main/resources/datasets/nips.txt");
		
		LoggingUtils lu = new LoggingUtils();
		nipsCfg.setLoggingUtil(lu);
		nipsCfg.activateSubconfig("demo-nips");
		String dataset_fn = nipsCfg.getDatasetFilename();
		
		InstanceList instances = LDAUtils.loadInstancesKeep(dataset_fn, 
				null, KEEP_CNT, true);

		TfIdfPipe tfIdfPipe = new TfIdfPipe(instances.getDataAlphabet(), instances.getTargetAlphabet());
		
		for (Instance instance : instances) {
			tfIdfPipe.pipe(instance);
		}
		
		TIntDoubleHashMap tfidfs = tfIdfPipe.getTfIdf();
		int [] ranks = tfIdfPipe.freqSortWords(tfidfs, instances.getDataAlphabet());
		
		for (int i = 0; i < ranks.length; i++) {
			System.out.println(instances.getDataAlphabet().lookupObject(ranks[i]) + " => " + tfidfs.get(ranks[i]));
		}
	}

	
}
