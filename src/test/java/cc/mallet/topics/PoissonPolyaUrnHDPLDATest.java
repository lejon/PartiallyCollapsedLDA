package cc.mallet.topics;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.apache.commons.math3.distribution.BinomialDistribution;
import org.apache.commons.math3.stat.inference.ChiSquareTest;
import org.junit.Test;

import cc.mallet.types.BinomialSampler;

public class PoissonPolyaUrnHDPLDATest {

	//	@Test
	//	public void testSampleLOneDocAnalytic() {
	//		int numTopics = 5;
	//		DocTopicTokenFreqTable docTopicTokenFreqTable = new DocTopicTokenFreqTable(numTopics);
	//		int numDocs = 1;
	//		int [][] documentLocalTopicCounts = {{3,3,3,2,1}};
	//
	//		for (int docNo = 0; docNo < numDocs; docNo++) {
	//			int [] localTopicCounts = documentLocalTopicCounts[docNo];
	//			for (int i = 0; i < numTopics; i++) {
	//				if(localTopicCounts[i]!=0) {
	//					docTopicTokenFreqTable.increment(i,(int)localTopicCounts[i]);
	//				}
	//			}
	//		}
	//
	//		int topic = 0;
	//		System.out.println("Freq table: \n" + docTopicTokenFreqTable);
	//		System.out.println("Reverse cumsum 0: " + Arrays.toString(docTopicTokenFreqTable.getReverseCumulativeSum(topic)));
	//		double [] psi = {0.5,0.2,0.1,0.1,0.1}; // psi_k
	//		double alpha = 1;	
	//		// Mans comment: I have not done this correctly I think
	//		WalkerAliasTable [][] binomialTables = PoissonPolyaUrnHDPLDA.initBinomialAlias(6, gamma, 50, 50, 50);
	//		
	//		// 		
	//		// True values computed using the Anonika distribution (9) in paper
	//		double [][] trueProp = {{0.53333333, 0.40000000, 0.06666667,0,0}, {0.75757576, 0.22727273, 0.01515152,0,0}, {0.865800866, 0.129870130, 0.0043290041, 0, 0}, {0.90909091, 0.09090909,0,0,0}, {1,0,0,0,0}};
	//		int sampleSize = 10000;
	//		for (int topicIdx = 0; topicIdx < numTopics; topicIdx++)
	//			int [] lSamples = new int[200];
	//			for (int i = 0; i < sampleSize; i++) {
	//				// Mans comment: This need to use the correct parameters
	//				int l_k = PoissonPolyaUrnHDPLDA.sampleL(topic, gamma, 6, docTopicTokenFreqTable, binomialTables);
	//				lSamples[l_k]++;
	//			}
	//			double test1 = cs.chiSquareTestDataSetsComparison(lSamples, trueProp[topicIdx]*sampleSize);
	//		}
	//		System.out.println("Samples: " + Arrays.toString(lSamples));
	//	}
	

	//	@Test
	//	public void testSampleLSimR() {
	//		int numTopics = 5;
	//		DocTopicTokenFreqTable docTopicTokenFreqTable = new DocTopicTokenFreqTable(numTopics);
	//		int numDocs = 3;
	//		int [][] documentLocalTopicCounts = {{0,0,5,1,0}, {2,0,1,0,0},{3,0,1,1,0}};
	//
	//		for (int docNo = 0; docNo < numDocs; docNo++) {
	//			int [] localTopicCounts = documentLocalTopicCounts[docNo];
	//			for (int i = 0; i < numTopics; i++) {
	//				if(localTopicCounts[i]!=0) {
	//					docTopicTokenFreqTable.increment(i,(int)localTopicCounts[i]);
	//				}
	//			}
	//		}
	//
	//		int topic = 0;
	//		System.out.println("Freq table: \n" + docTopicTokenFreqTable);
	//		System.out.println("Reverse cumsum 0: " + Arrays.toString(docTopicTokenFreqTable.getReverseCumulativeSum(topic)));
	//		double psi = 0.1;
	//		double alpha = 1;
	//		// Mans comment: This is not fixed!
	//		WalkerAliasTable [][] binomialTables = PoissonPolyaUrnHDPLDA.initBinomialAlias(6, gamma, 50, 50, 50);
	//		
	//		int [] lSamples = new int[200];
	//		int sampleSize = 10000;
	//		for (int i = 0; i < sampleSize; i++) {
	//			// Mans comment: This is not fixed!
	//			int l_k = PoissonPolyaUrnHDPLDA.sampleL(topic, gamma, 6, docTopicTokenFreqTable, binomialTables);
	//			lSamples[l_k]++;
	//		}
	//		double [] trueProp = {0, 0.787784 0.196259 0.015583 0.000374 }; // Based on simulations so accept larger deviations
	//		double test1 = cs.chiSquareTestDataSetsComparison(lSamples, trueProp[topicIdx]*sampleSize);
	//		System.out.println("Samples: " + Arrays.toString(lSamples));
	//	}	
	
	//	@Test
	//	public void testSampleL() {
	//		int numTopics = 5;
	//		DocTopicTokenFreqTable docTopicTokenFreqTable = new DocTopicTokenFreqTable(numTopics);
	//		int numDocs = 3;
	//		int [][] documentLocalTopicCounts = {{0,0,5,1,0}, {1,0,1,0,0},{3,0,1,1,0}};
	//
	//		for (int docNo = 0; docNo < numDocs; docNo++) {
	//			int [] localTopicCounts = documentLocalTopicCounts[docNo];
	//			for (int i = 0; i < numTopics; i++) {
	//				if(localTopicCounts[i]!=0) {
	//					docTopicTokenFreqTable.increment(i,(int)localTopicCounts[i]);
	//				}
	//			}
	//		}
	//
	//		int topic = 0;
	//		System.out.println("Freq table: \n" + docTopicTokenFreqTable);
	//		System.out.println("Reverse cumsum 0: " + Arrays.toString(docTopicTokenFreqTable.getReverseCumulativeSum(topic)));
	//		double gamma = 5;
	//		WalkerAliasTable [][] binomialTables = PoissonPolyaUrnHDPLDA.initBinomialAlias(6, gamma, 50, 50, 50);
	//		
	//		int [] lSamples = new int[200];
	//		for (int i = 0; i < 2000; i++) {
	//			int l_k = PoissonPolyaUrnHDPLDA.sampleL(topic, gamma, 6, docTopicTokenFreqTable, binomialTables);
	//			lSamples[l_k]++;
	//		}
	//		
	//		System.out.println("Samples: " + Arrays.toString(lSamples));
	//	}

	@Test
	public void test5005() {
		// Setup draws
		int noDraws = 500_000;
		int [] trialss = {50};
		double [] probs = {0.5};

		int samplesLen = 1000;
		for (int trials : trialss) {	
			for (double prob : probs) {
				long [] samplesBinomSampler = new long[samplesLen]; 
				long [] samplesB = new long[samplesLen]; 

				BinomialDistribution binDist = new BinomialDistribution(trials, prob);
				for (int i = 0; i < noDraws; i++) {
					int pSample = BinomialSampler.rbinom(trials, prob);
					samplesBinomSampler[pSample]++;
					int binSample = binDist.sample(); 
					samplesB[binSample]++;
				}

				int [] rg = PoissonPolyaUrnTest.findSeqRange(samplesB);

				int smallestIdx = rg[0];
				int largestIdx = rg[1];

				int obsLen = largestIdx - smallestIdx;
				// Adapt to the test preconditions
				long [] obsBinomSampler = new long[obsLen];
				long [] obsBin = new long[obsLen];
				for (int i = smallestIdx; i < largestIdx; i++) {
					obsBinomSampler[i-smallestIdx] = samplesBinomSampler[i];
					obsBin[i-smallestIdx] = samplesB[i];
				}

				ChiSquareTest cs = new ChiSquareTest();
				double test1 = cs.chiSquareTestDataSetsComparison(obsBin, obsBinomSampler);
				if(!(test1 > 0.01)) {
					System.out.println("Test:" + test1);
					System.out.println("Trials:" + trials);
					System.out.println("Prob:" + prob);
					System.out.println("Exact samples  : " + Arrays.toString(obsBin));
					System.out.println("SampleL samples: " + Arrays.toString(obsBinomSampler));
					System.out.println("Exact samples  : " + Arrays.toString(samplesB));
					System.out.println("SampleL samples: " + Arrays.toString(samplesBinomSampler));
					System.err.println("TEST FAILS");
				} 
				assertTrue(test1 > 0.01);
				System.out.println();
			}
		}
	}

	@Test
	public void testTheTest() {
		// Setup draws
		int noDraws = 500_000;
		int [] trialss = {50};
		double [] probs = {0.5};

		int samplesLen = 1000;
		for (int trials : trialss) {	
			for (double prob : probs) {
				long [] samplesB1 = new long[samplesLen]; 
				long [] samplesB = new long[samplesLen]; 

				BinomialDistribution binDist = new BinomialDistribution(trials, prob);
				BinomialDistribution binDist1 = new BinomialDistribution(trials, prob);
				for (int i = 0; i < noDraws; i++) {
					int pSample = binDist1.sample();
					samplesB1[pSample]++;
					int binSample = binDist.sample(); 
					samplesB[binSample]++;
				}

				int [] rg = PoissonPolyaUrnTest.findSeqRange(samplesB);

				int smallestIdx = rg[0];
				int largestIdx = rg[1];


				int obsLen = largestIdx - smallestIdx;
				//				System.out.println("Obs. Len.: " + obsLen);
				// Adapt to the test preconditions
				long [] obsB1 = new long[obsLen];
				long [] obsBin = new long[obsLen];
				for (int i = smallestIdx; i < largestIdx; i++) {
					obsB1[i-smallestIdx] = samplesB1[i];
					obsBin[i-smallestIdx] = samplesB[i];
				}

				ChiSquareTest cs = new ChiSquareTest();
				double test1 = cs.chiSquareTestDataSetsComparison(obsBin, obsB1);
				if(!(test1 > 0.01)) {
					System.out.println("Test:" + test1);
					System.out.println("Trials:" + trials);
					System.out.println("Prob:" + prob);
					System.out.println("Exact samples  : " + Arrays.toString(obsBin));
					System.out.println("SampleL samples: " + Arrays.toString(obsB1));
					System.out.println("Exact samples  : " + Arrays.toString(samplesB));
					System.out.println("SampleL samples: " + Arrays.toString(samplesB1));
					System.err.println("TEST FAILS");
					System.out.println();
				} 
				assertTrue(test1 > 0.01);
			}
		}
	}

	@Test
	public void testPoissonSampleBinomial() {
		// Setup draws
		int noDraws = 500_000;
		int [] trialss = {2, 10, 20, 50, 100};
		double [] probs = {0.01, 0.1, 0.5};

		int samplesLen = 1000;
		for (int trials : trialss) {	
			for (double prob : probs) {
				long [] samplesBinomialSampler = new long[samplesLen]; 
				long [] samplesB = new long[samplesLen]; 

				BinomialDistribution binDist = new BinomialDistribution(trials, prob);
				for (int i = 0; i < noDraws; i++) {
					int pSample = BinomialSampler.rbinom(trials, prob);
					samplesBinomialSampler[pSample]++;
					int binSample = binDist.sample(); 
					samplesB[binSample]++;
				}

				int [] rg = PoissonPolyaUrnTest.findSeqRange(samplesB);

				int smallestIdx = rg[0];
				int largestIdx = rg[1];

				int obsLen = largestIdx - smallestIdx;
				// Adapt to the test preconditions
				long [] obsBinomialSampler = new long[obsLen];
				long [] obsBin = new long[obsLen];
				for (int i = smallestIdx; i < largestIdx; i++) {
					obsBinomialSampler[i-smallestIdx] = samplesBinomialSampler[i];
					obsBin[i-smallestIdx] = samplesB[i];
				}

				ChiSquareTest cs = new ChiSquareTest();
				double test1 = cs.chiSquareTestDataSetsComparison(obsBin, obsBinomialSampler);
				if(!(test1 > 0.01)) {
					System.out.println("Test:" + test1);
					System.out.println("Trials:" + trials);
					System.out.println("Prob:" + prob);
					System.out.println("Exact samples  : " + Arrays.toString(obsBin));
					System.out.println("SampleL samples: " + Arrays.toString(obsBinomialSampler));
					System.out.println("Exact samples  : " + Arrays.toString(samplesB));
					System.out.println("SampleL samples: " + Arrays.toString(samplesBinomialSampler));
					System.err.println("TEST FAILS");
					System.out.println();
				} 
				assertTrue(test1 > 0.01);
			}
		}
	}

	@Test
	public void testBinomialUsingProbs() {
		// Setup draws
		int noDraws = 500_000;
		int tableLength = 200;
		int [] trialss = {15, 20, 50, 100};
		double [] probs = {0.01, 0.1, 0.5, 0.75};

		for (int trials : trialss) {	
			for (double prob : probs) {
				long [] samplesB = new long[tableLength]; 
				int maxIdx = 0;

				double [] trialProbabilities = new double [tableLength];
				for (int i = 1; i < tableLength; i++) {
					BinomialDistribution binDistCalc = new BinomialDistribution(trials, prob);
					for (int j = 0; j < tableLength; j++) {
						trialProbabilities[j] =  binDistCalc.probability(j);
						if(trialProbabilities[j]>0.00000000001) {
							maxIdx = j+1;
						}
					}
				}

				for (int i = 0; i < noDraws; i++) {
					samplesB[BinomialSampler.rbinom(trials, prob)]++; 
				}

				// Adapt to the test preconditions
				long [] obsBinomSampler = new long[maxIdx];
				double [] possibleProbs = new double[maxIdx];
				for (int i = 0; i < maxIdx; i++) {
					obsBinomSampler[i] = samplesB[i];
					possibleProbs[i] = trialProbabilities[i];
				}

				ChiSquareTest cs = new ChiSquareTest();
				double test1 = cs.chiSquareTest(possibleProbs,obsBinomSampler);
				double alpha = 0.01;
				if(test1 <= alpha) {
					System.out.println("Trials: " + trials + " prob: " + prob);
					System.out.println("Probs:" + Arrays.toString(possibleProbs));
					System.out.println("Sampler:" + Arrays.toString(obsBinomSampler));
					System.out.println(test1);
					System.out.println();
				}
				assertTrue(test1 > alpha);
			}
		}
	}


}
