package cc.mallet.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import org.apache.commons.lang.NotImplementedException;
import org.junit.Test;

import cc.mallet.configuration.LDAConfiguration;
import cc.mallet.configuration.SimpleLDAConfiguration;
import cc.mallet.topics.LDAGibbsSampler;
import cc.mallet.topics.TopicAssignment;
import cc.mallet.topics.UncollapsedParallelLDA;
import cc.mallet.topics.randomscan.document.AdaptiveBatchBuilder;
import cc.mallet.topics.randomscan.document.BatchBuilderFactory;
import cc.mallet.topics.randomscan.document.DocumentBatchBuilder;
import cc.mallet.topics.randomscan.document.FixedSplitBatchBuilder;
import cc.mallet.topics.randomscan.topic.ProportionalTopicIndexBuilder;
import cc.mallet.types.Alphabet;
import cc.mallet.types.InstanceList;
import cc.mallet.util.LDAUtils;
import cc.mallet.util.LoggingUtils;

public class BatchBuilderTest {
	
	SimpleLDAConfiguration getStdConfig(String whichModel, Integer numIter,	Integer numBatches) {
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
	
	class MockSampler implements LDAGibbsSampler {
		int iteration = 0;
		@Override
		public void setConfiguration(LDAConfiguration config) {}

		@Override
		public void addInstances(InstanceList training) {}

		@Override
		public void sample(int iterations) throws IOException {iteration+=iterations;}

		@Override
		public void setRandomSeed(int seed) {}

		@Override
		public int getNoTopics() {return 0;}

		@Override
		public int getCurrentIteration() {return iteration;}

		@Override
		public int[][] getZIndicators() {return null;}

		@Override
		public void setZIndicators(int[][] zIndicators) {}

		@Override
		public InstanceList getDataset() {return null;}

		@Override
		public int[][] getDeltaStatistics() {return null;}

		@Override
		public int[] getTopTypeFrequencyIndices() {return null;}

		@Override
		public Alphabet getAlphabet() {return null;}

		@Override
		public int[] getTypeFrequencies() {return null;}

		@Override
		public int getStartSeed() {return 0;}

		@Override
		public double[] getTypeMassCumSum() {return null;}

		@Override
		public int getCorpusSize() {
			return -1;
		}

		@Override
		public int[][] getDocumentTopicMatrix() {
			return null;
		}

		@Override
		public int[][] getTypeTopicMatrix() {
			return null;
		}

		@Override
		public double[][] getZbar() {
			return null;
		}
		@Override
		public double[][] getThetaEstimate() {
			return null;
		}

		@Override
		public void preIteration() {
			
		}

		@Override
		public void postIteration() {
			
		}

		@Override
		public void preSample() {
			
		}

		@Override
		public void postSample() {
			
		}

		@Override
		public void postZ() {
			
		}

		@Override
		public void preZ() {
			
		}

		@Override
		public LDAConfiguration getConfiguration() {
			return null;
		}

		@Override
		public int getNoTypes() {
			return 0;
		}
		@Override
		public void addTestInstances(InstanceList testSet) {
			throw new NotImplementedException();
		}

		@Override
		public int getNumTopics() {
			return getNoTopics();
		}

		@Override
		public ArrayList<TopicAssignment> getData() {
			return null;
		}

		@Override
		public int[] getTopicTotals() {
			return null;
		}

		@Override
		public double getBeta() {
			return 0;
		}

		@Override
		public double[] getAlpha() {
			return null;
		}

		@Override
		public void abort() {
			
		}

		@Override
		public boolean getAbort() {
			return false;
		}

		@Override
		public double[] getLogLikelihood() {
			return null;
		}

		@Override
		public double[] getHeldOutLogLikelihood() {
			return null;
		}
	}

	@Test
	public void testEvenSplit() throws UnsupportedEncodingException, FileNotFoundException {
		String whichModel = "uncollapsed";
		Integer numIter = 1000;
		Integer numBatches = 6;
				
		SimpleLDAConfiguration config = getStdConfig(whichModel, numIter, numBatches);

		LoggingUtils lu = new LoggingUtils();
		lu.checkAndCreateCurrentLogDir("Runs");
		config.setLoggingUtil(lu);
		config.activateSubconfig("demo-nips");
		config.setBatchBuildingScheme(BatchBuilderFactory.EVEN_SPLIT);

		//System.out.println("Using Config: " + config.whereAmI());

		String dataset_fn = config.getDatasetFilename();
		//System.out.println("Using dataset: " + dataset_fn);
		//System.out.println("Scheme: " + whichModel);

		InstanceList instances = LDAUtils.loadInstances(dataset_fn, 
				"stoplist.txt", config.getRareThreshold(LDAConfiguration.RARE_WORD_THRESHOLD));

		UncollapsedParallelLDA uncollapsed = new UncollapsedParallelLDA(config);
		uncollapsed.setRandomSeed(config.getSeed(LDAConfiguration.SEED_DEFAULT));
		uncollapsed.addInstances(instances);

		DocumentBatchBuilder bb = BatchBuilderFactory.get(config, uncollapsed);

		bb.calculateBatch();

		int sumdocs = 0;
		for (int [] docIndicies : bb.documentBatches()) {				
			sumdocs += docIndicies.length;
		}
		// Ensures that we loop over ALL documents
		assertEquals(uncollapsed.getData().size(),sumdocs);

		// Ensures that we loop over ALL documents
		assertEquals(uncollapsed.getData().size(),bb.getDocumentsInIteration(0));

		// Assures that we have non-overlapping documents batches
		// This ALSO ensures that no document exists in more than one batch!
		int prevIdx = -1;
		for (int [] docIndicies : bb.documentBatches()) {
			for (int i = 0; i < docIndicies.length; i++) {				
				assertEquals(docIndicies[i],prevIdx+1);
				prevIdx = docIndicies[i];
			}
		}
	}

	@Test
	public void testPercentageSplit() throws UnsupportedEncodingException, FileNotFoundException {
		String whichModel = "uncollapsed";
		Integer numIter = 1000;
		Integer numBatches = 6;
				
		SimpleLDAConfiguration config = getStdConfig(whichModel, numIter, numBatches);

		LoggingUtils lu = new LoggingUtils();
		lu.checkAndCreateCurrentLogDir("Runs");
		config.setLoggingUtil(lu);
		config.activateSubconfig("demo-nips");
		config.setBatchBuildingScheme(BatchBuilderFactory.PERCENTAGE_SPLIT);
		double percentageSplit = 0.1;
		config.setDocPercentageSplitSize(percentageSplit);

		//System.out.println("Using Config: " + config.whereAmI());

		String dataset_fn = config.getDatasetFilename();
		//System.out.println("Using dataset: " + dataset_fn);
		//System.out.println("Scheme: " + whichModel);

		InstanceList instances = LDAUtils.loadInstances(dataset_fn, 
				"stoplist.txt", config.getRareThreshold(LDAConfiguration.RARE_WORD_THRESHOLD));

		UncollapsedParallelLDA uncollapsed = new UncollapsedParallelLDA(config);
		uncollapsed.setRandomSeed(config.getSeed(LDAConfiguration.SEED_DEFAULT));
		uncollapsed.addInstances(instances);

		DocumentBatchBuilder bb = BatchBuilderFactory.get(config, uncollapsed);

		int [][] prevBatch = new int[0][];
		int testLoops = 500;
		for (int i = 0; i < testLoops; i++) {
			bb.calculateBatch();
			boolean [] docIsSeen = new boolean[instances.size()]; 
			int sumdocs = 0;
			int numDifferent = 0;

			// This loop calculates a rough measure of how much the batches change between iterations
			int[][] iterationBatches = bb.documentBatches();
			for (int batchNo = 0; batchNo < iterationBatches.length; batchNo++) {	
				int [] docIndicies = iterationBatches[batchNo];
				// If we have more documents than batches each batch should have at least one document 
				if(numBatches<instances.size()) {
					assertTrue("Batch has no documents assigned to it: ", docIndicies.length>0);
				}
				if(prevBatch.length==0 || prevBatch[batchNo]==null) {
					if(prevBatch.length==0) {
						prevBatch = new int[iterationBatches.length][];
					}
					prevBatch[batchNo] = new int[iterationBatches[batchNo].length];
				}
				for (int docIdx = 0; docIdx < Math.min(docIndicies.length, prevBatch[batchNo].length); docIdx++) {
					numDifferent = (docIndicies[docIdx] != prevBatch[batchNo][docIdx]) ? (numDifferent+1) : numDifferent;
					// Now move that cell over to the previous batch
					prevBatch[batchNo][docIdx]=docIndicies[docIdx];						
				}	
				sumdocs += docIndicies.length;
			}
			
			int upperRange = (int) ((percentageSplit + 0.1) * uncollapsed.getData().size());
			int lowerRange = (int) ((percentageSplit - 0.1) * uncollapsed.getData().size());
			// Ensures that we are within the alloted percentage of sampled documents
			assertTrue("Upper is not bigger than looped over documents", upperRange > sumdocs);
			assertTrue("Lower is not smaller than looped over documents", lowerRange < sumdocs);

			// Ensure that we have different baches in each random scan
			if(prevBatch.length>0) {
				assertTrue("Smaller than 10 % difference in batches! Different: " + numDifferent, 
						numDifferent>(percentageSplit*uncollapsed.getData().size()*0.1));
			}

			//System.out.println("Batches are:");
			for (int j = 0; j < iterationBatches.length; j++) {	
				int [] docIndicies = iterationBatches[j];
				//System.out.println("Batch " + j + ": Size = " + docIndicies.length);
				for (int k = 0; k < docIndicies.length; k++) {
					if(docIsSeen[docIndicies[k]]) {
						throw new IllegalStateException("The same document is in more than one batch, this is now allowed, since sampling count can go below 0!");
					}
					docIsSeen[docIndicies[k]] = true;
					//System.out.print(docIndicies[k] + ", ");
				}	
				//System.out.println();
				sumdocs += docIndicies.length;
			}
//			System.out.println();
		}
	}
	
	@Test
	public void testAdaptive() throws IOException {
		String whichModel = "uncollapsed";
		Integer numIter = 1000;
		Integer numBatches = 6;

		SimpleLDAConfiguration config = getStdConfig(whichModel, numIter, numBatches);

		LoggingUtils lu = new LoggingUtils();
		lu.checkAndCreateCurrentLogDir("Runs");
		config.setLoggingUtil(lu);
		config.activateSubconfig("demo-nips");
		config.setBatchBuildingScheme(BatchBuilderFactory.ADAPTIVE_SPLIT);
		double percentageSplit = 0.1;
		config.setDocPercentageSplitSize(percentageSplit);

//		System.out.println("Using Config: " + config.whereAmI());

		String dataset_fn = config.getDatasetFilename();
//		System.out.println("Using dataset: " + dataset_fn);
//		System.out.println("Scheme: " + whichModel);

		InstanceList instances = LDAUtils.loadInstances(dataset_fn, 
				"stoplist.txt", config.getRareThreshold(LDAConfiguration.RARE_WORD_THRESHOLD));

		UncollapsedParallelLDA uncollapsed = new UncollapsedParallelLDA(config);
		uncollapsed.setRandomSeed(config.getSeed(LDAConfiguration.SEED_DEFAULT));
		uncollapsed.addInstances(instances);

		int instabilityPeriod = 200;
		AdaptiveBatchBuilder bb = (AdaptiveBatchBuilder) BatchBuilderFactory.get(config, uncollapsed);
		MockSampler mockSampler = new MockSampler();
		bb.setSampler(mockSampler);
		bb.setDeltaInstabilityPeriod(instabilityPeriod);

		int [][] prevBatch = new int[0][];
		int testLoops = 500;
		for (int i = 0; i < testLoops; i++) {
			bb.calculateBatch();
			boolean [] docIsSeen = new boolean[instances.size()]; 
			int sumdocs = 0;
			int numDifferent = 0;

			// This loop calculates a rough measure of how much the batches change between iterations
			int[][] iterationBatches = bb.documentBatches();
			for (int batchNo = 0; batchNo < iterationBatches.length; batchNo++) {	
				int [] docIndicies = iterationBatches[batchNo];
				// If we have more documents than batches each batch should have at least one document 
				if(numBatches<instances.size()) {
					assertTrue("Batch has no documents assigned to it: ", docIndicies.length>0);
				}
				if(prevBatch.length==0 || prevBatch[batchNo]==null) {
					if(prevBatch.length==0) {
						prevBatch = new int[iterationBatches.length][];
					}
					prevBatch[batchNo] = new int[iterationBatches[batchNo].length];
				}
				for (int docIdx = 0; docIdx < Math.min(docIndicies.length, prevBatch[batchNo].length); docIdx++) {
					numDifferent = (docIndicies[docIdx] != prevBatch[batchNo][docIdx]) ? (numDifferent+1) : numDifferent;
					// Now move that cell over to the previous batch
					prevBatch[batchNo][docIdx]=docIndicies[docIdx];						
				}	
				sumdocs += docIndicies.length;
			}
			
			int upperRange = (int) ((percentageSplit + 0.1) * uncollapsed.getData().size());
			int lowerRange = (int) ((percentageSplit - 0.1) * uncollapsed.getData().size());
			// Ensures that we are within the alloted percentage of sampled documents
			if(i > bb.getDeltaInstabilityPeriod()) {
				assertTrue("Upper is not bigger than looped over documents", upperRange > sumdocs);
			}
			assertTrue("Lower is not smaller than looped over documents", lowerRange < sumdocs);

			// Ensure that we have different baches in each random scan
			if(prevBatch.length>0 && i > bb.getDeltaInstabilityPeriod()) {
				assertTrue("Smaller than 10 % difference in batches! Different: " + numDifferent, 
						numDifferent>(percentageSplit*uncollapsed.getData().size()*0.1));
			}

			//System.out.println("Batches are:");
			for (int j = 0; j < iterationBatches.length; j++) {	
				int [] docIndicies = iterationBatches[j];
				//System.out.println("Batch " + j + ": Size = " + docIndicies.length);
				for (int k = 0; k < docIndicies.length; k++) {
					if(docIsSeen[docIndicies[k]]) {
						throw new IllegalStateException("The same document is in more than one batch, this is now allowed, since sampling count can go below 0!");
					}
					docIsSeen[docIndicies[k]] = true;
					//System.out.print(docIndicies[k] + ", ");
				}	
				//System.out.println();
				sumdocs += docIndicies.length;
			}
			//System.out.println();
			
			if(i < bb.getDeltaInstabilityPeriod()) {
				assertEquals(instances.size(), bb.getDocumentsInIteration(i));
			} else {
				assertTrue(instances.size()*(percentageSplit+0.1) > bb.getDocumentsInIteration(i));
			}
			
			mockSampler.sample(1);
		}
	}
	
	@Test
	public void testFixedSplit() throws IOException {
		String whichModel = "uncollapsed";
		Integer numIter = 1000;
		Integer numBatches = 6;

		SimpleLDAConfiguration config = getStdConfig(whichModel, numIter, numBatches);

		LoggingUtils lu = new LoggingUtils();
		lu.checkAndCreateCurrentLogDir("Runs");
		config.setLoggingUtil(lu);
		config.activateSubconfig("demo-nips");
		config.setBatchBuildingScheme(BatchBuilderFactory.FIXED_SPLIT);
		double [] percentageSplits = {0.1,0.2,0.3,0.4,0.5,0.7,0.8,1.0};
		config.setFixedSplitSizeDoc(percentageSplits);

//		System.out.println("Using Config: " + config.whereAmI());

		String dataset_fn = config.getDatasetFilename();
//		System.out.println("Using dataset: " + dataset_fn);
//		System.out.println("Scheme: " + whichModel);

		InstanceList instances = LDAUtils.loadInstances(dataset_fn, 
				"stoplist.txt", config.getRareThreshold(LDAConfiguration.RARE_WORD_THRESHOLD));

		UncollapsedParallelLDA uncollapsed = new UncollapsedParallelLDA(config);
		uncollapsed.setRandomSeed(config.getSeed(LDAConfiguration.SEED_DEFAULT));
		uncollapsed.addInstances(instances);

		MockSampler mockSampler = new MockSampler();
		FixedSplitBatchBuilder bb = (FixedSplitBatchBuilder) BatchBuilderFactory.get(config, uncollapsed);
		bb.setSampler(mockSampler);

		int [][] prevBatch = new int[0][];
		int testLoops = 500;
		int globalDocIdx = 0;
		int prevGlobalDocIdx = 0;
		int percentageSplitPointer = 0;
		for (int i = 0; i < testLoops; i++) {
			bb.calculateBatch();
			int sumdocs = 0;

			int[][] iterationBatches = bb.documentBatches();
			for (int batchNo = 0; batchNo < iterationBatches.length; batchNo++) {	
				int [] docIndicies = iterationBatches[batchNo];
				// If we have more documents than batches each batch should have at least one document 
				if(numBatches<instances.size()) {
					assertTrue("Batch has no documents assigned to it: ", docIndicies.length>0);
				}
				if(prevBatch.length==0 || prevBatch[batchNo]==null) {
					if(prevBatch.length==0) {
						prevBatch = new int[iterationBatches.length][];
					}
				}
				prevBatch[batchNo] = new int[iterationBatches[batchNo].length];
				for (int docIdx = 0; docIdx < docIndicies.length; docIdx++) {
					// ensure that indices are consecutive
					globalDocIdx = docIndicies[docIdx];
					assertEquals(prevGlobalDocIdx+1,globalDocIdx);
					prevGlobalDocIdx = globalDocIdx;
					// When we have done all docs, we should wrap around
					if((prevGlobalDocIdx+1)==instances.size()) {
						prevGlobalDocIdx = -1;
					}
					
					// Now copy that cell over to the previous batch
					prevBatch[batchNo][docIdx]=docIndicies[docIdx];						
				}	
				sumdocs += docIndicies.length;
			}
			
			// Ensures that we are within the alloted percentage of sampled documents
			int upperRange = (int) ((percentageSplits[percentageSplitPointer] + 0.1) * uncollapsed.getData().size());
			assertTrue("Upper is not bigger than looped over documents", upperRange > sumdocs);
			int lowerRange = (int) ((percentageSplits[percentageSplitPointer] - 0.1) * uncollapsed.getData().size());
			assertTrue("Lower is not smaller than looped over documents", lowerRange < sumdocs);

			boolean [] docIsSeen = new boolean[instances.size()];
			//System.out.println("Batches are:");
			for (int j = 0; j < iterationBatches.length; j++) {
				int [] docIndicies = iterationBatches[j];
//				System.out.println("Batch  " + j + " contains : " + docIndicies.length 
//						+ " documents (" + percentageSplits[percentageSplitPointer] + ") out of: " + instances.size());
				//System.out.println("Batch " + j + ": Size = " + docIndicies.length);
				for (int k = 0; k < docIndicies.length; k++) {
					if(docIsSeen[docIndicies[k]]) {
						throw new IllegalStateException("The same document is in more than one batch, this is now allowed, since sampling count can go below 0!");
					}
					docIsSeen[docIndicies[k]] = true;
//					System.out.print(docIndicies[k] + ", ");
				}	
//				System.out.println();
				sumdocs += docIndicies.length;
			}
			//System.out.println();
			
			mockSampler.sample(1);
			percentageSplitPointer = (percentageSplitPointer+1) % percentageSplits.length;
//			System.out.println("Lap " + i);
		}
	}
	
	
	/**
	 * Tests that an unconfigured ProportionalTopicIndexBuilder returns all types
	 * @throws IOException
	 */
	@Test
	public void testProportionalTopicIndexBuilderDefault() throws IOException {
		String whichModel = "uncollapsed";
		Integer numIter = 1000;
		Integer numBatches = 6;

		SimpleLDAConfiguration config = getStdConfig(whichModel, numIter, numBatches);

		LoggingUtils lu = new LoggingUtils();
		lu.checkAndCreateCurrentLogDir("Runs");
		config.setLoggingUtil(lu);
		config.activateSubconfig("demo-nips");
		config.setBatchBuildingScheme(BatchBuilderFactory.FIXED_SPLIT);
		double [] percentageSplits = {0.1,0.2,0.3,0.4,0.5,0.7,0.8,1.0};
		config.setFixedSplitSizeDoc(percentageSplits);

//		System.out.println("Using Config: " + config.whereAmI());

		String dataset_fn = config.getDatasetFilename();
//		System.out.println("Using dataset: " + dataset_fn);
//		System.out.println("Scheme: " + whichModel);

		InstanceList instances = LDAUtils.loadInstances(dataset_fn, 
				"stoplist.txt", config.getRareThreshold(LDAConfiguration.RARE_WORD_THRESHOLD));

		UncollapsedParallelLDA uncollapsed = new UncollapsedParallelLDA(config);
		uncollapsed.setRandomSeed(config.getSeed(LDAConfiguration.SEED_DEFAULT));
		uncollapsed.addInstances(instances);
		
		ProportionalTopicIndexBuilder ptib = new ProportionalTopicIndexBuilder(config, uncollapsed);
		int [][] indices = ptib.getTopicTypeIndices();
		org.junit.Assert.assertEquals("Didn't get as many batches as topics", indices.length, (int) config.getNoTopics(LDAConfiguration.NO_TOPICS_DEFAULT));
		for (int i = 0; i < indices.length; i++) {
			for (int j = 0; j < indices[i].length && j < 50; j++) {
				assertEquals(indices[i][j], j);
			}
		}
	}
	
	/**
	 * Tests that an unconfigured ProportionalTopicIndexBuilder returns all types
	 * @throws IOException
	 */
	@Test
	public void testProportionalTopicIndexBuilderN200() throws IOException {
		String whichModel = "uncollapsed";
		Integer numIter = 1000;
		Integer numBatches = 6;

		SimpleLDAConfiguration config = getStdConfig(whichModel, numIter, numBatches);
		
		int n = 200;
		config.setProportionalTopicIndexBuilderSkipStep(n);

		LoggingUtils lu = new LoggingUtils();
		lu.checkAndCreateCurrentLogDir("Runs");
		config.setLoggingUtil(lu);
		config.activateSubconfig("demo-nips");
		config.setBatchBuildingScheme(BatchBuilderFactory.FIXED_SPLIT);
		double [] percentageSplits = {0.1,0.2,0.3,0.4,0.5,0.7,0.8,1.0};
		config.setFixedSplitSizeDoc(percentageSplits);

//		System.out.println("Using Config: " + config.whereAmI());

		String dataset_fn = config.getDatasetFilename();
//		System.out.println("Using dataset: " + dataset_fn);
//		System.out.println("Scheme: " + whichModel);

		InstanceList instances = LDAUtils.loadInstances(dataset_fn, 
				"stoplist.txt", config.getRareThreshold(LDAConfiguration.RARE_WORD_THRESHOLD));

		UncollapsedParallelLDA uncollapsed = new UncollapsedParallelLDA(config);
		uncollapsed.setRandomSeed(config.getSeed(LDAConfiguration.SEED_DEFAULT));
		uncollapsed.addInstances(instances);
		
		int [] typeFreqs = uncollapsed.getTypeFrequencies();
		int [] hasMoreThanN = new int[typeFreqs.length];
		int j = 0;
		for (int i = 0; i < hasMoreThanN.length; i++) {
			if(typeFreqs[i]>n) {
				hasMoreThanN[j++] = i;
			}
		}
		// Save how many was > n (200)
		int maxIdxs = j;
		
		ProportionalTopicIndexBuilder ptib = new ProportionalTopicIndexBuilder(config, uncollapsed);
		int [][] indices = ptib.getTopicTypeIndices();
		org.junit.Assert.assertEquals("Didn't get as many batches as topics", indices.length, (int) config.getNoTopics(LDAConfiguration.NO_TOPICS_DEFAULT));
		for (int i = 0; i < maxIdxs; i++) {
			int idx = hasMoreThanN[i];
			ensureInIndices(idx, indices, maxIdxs);
		}

//		System.out.println(maxIdxs + " was bigger than " + n);
	
	}

	private void ensureInIndices(int idx, int[][] indices, int minSize) {
		int foundCnt = 0;
		for (int i = 0; i < indices.length; i++) {
			for (int j = 0; j < indices[i].length; j++) {
				assertTrue(indices[i].length>minSize);
				if(idx==indices[i][j]) {
					foundCnt++;
				}
				if(foundCnt==indices.length) {
					return;
				}
			}
		}
		org.junit.Assert.fail("Could not find " + idx  + " in indices");
	}

}
