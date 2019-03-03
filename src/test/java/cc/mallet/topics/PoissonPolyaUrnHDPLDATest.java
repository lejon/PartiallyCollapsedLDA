package cc.mallet.topics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.math3.distribution.BinomialDistribution;
import org.apache.commons.math3.stat.inference.ChiSquareTest;
import org.junit.Test;

import cc.mallet.configuration.SimpleLDAConfiguration;
import cc.mallet.types.BinomialSampler;

public class PoissonPolyaUrnHDPLDATest {
	
	@Test
	public void testUpdateNrActiveTopics() {
		PoissonPolyaUrnHDPLDA s = new PoissonPolyaUrnHDPLDA(new SimpleLDAConfiguration());
		List<Integer> at = new ArrayList<Integer>();
		at.add(1);
		at.add(2);
		at.add(3);
		int [] et = new int [] {1,2};
		int nt = s.updateNrActiveTopics(et, at);
		assertEquals(1, nt);
		assertEquals(1, at.size());
	}
	
	@Test
	public void testUpdateNrActiveTopicsNoChange() {
		PoissonPolyaUrnHDPLDA s = new PoissonPolyaUrnHDPLDA(new SimpleLDAConfiguration());
		List<Integer> at = new ArrayList<Integer>();
		at.add(1);
		at.add(2);
		at.add(3);
		int [] et = new int [] {};
		int nt = s.updateNrActiveTopics(et, at);
		assertEquals(3, nt);
		assertEquals(3, at.size());
	}
	
	@Test
	public void testCalcNewTopicsEmpty() {
		PoissonPolyaUrnHDPLDA s = new PoissonPolyaUrnHDPLDA(new SimpleLDAConfiguration());
		int [] nt = s.calcNewTopics(new ArrayList<Integer>(), new int [] {});
		assertEquals(0, nt.length);
	}
	
	@Test
	public void testCalcNewTopicsNoNew() {
		PoissonPolyaUrnHDPLDA s = new PoissonPolyaUrnHDPLDA(new SimpleLDAConfiguration());
		List<Integer> at = Arrays.asList(new Integer[]{1, 2, 3});
		int [] nt = s.calcNewTopics(at, new int [] {1,2,3});
		assertEquals(0, nt.length);
	}

	@Test
	public void testCalcNewTopics() {
		PoissonPolyaUrnHDPLDA s = new PoissonPolyaUrnHDPLDA(new SimpleLDAConfiguration());
		List<Integer> at = Arrays.asList(new Integer[]{1, 2, 3});
		int [] nt = s.calcNewTopics(at, new int [] {2,3,4});
		assertEquals(1, nt.length);
		assertEquals(4, nt[0]);
	}

	@Test
	public void testCalcNewTopicsDuplicateSampled() {
		PoissonPolyaUrnHDPLDA s = new PoissonPolyaUrnHDPLDA(new SimpleLDAConfiguration());
		List<Integer> at = Arrays.asList(new Integer[]{1, 2, 3});
		int [] nt = s.calcNewTopics(at, new int [] {2,3,4,4,4,4});
		assertEquals(1, nt.length);
		assertEquals(4, nt[0]);
	}
	
	@Test
	public void testCalcNewTopicsDisjoint() {
		PoissonPolyaUrnHDPLDA s = new PoissonPolyaUrnHDPLDA(new SimpleLDAConfiguration());
		List<Integer> at = Arrays.asList(new Integer[]{1, 2, 3});
		int [] nt = s.calcNewTopics(at, new int [] {4,4,4,4,5,6});
		assertEquals(3, nt.length);
		assertEquals(4, nt[0]);
		assertEquals(5, nt[1]);
		assertEquals(6, nt[2]);
	}

	@Test
	public void testSampleLOneDocAnalytic() {
		int numTopics = 5;
		DocTopicTokenFreqTable docTopicTokenFreqTable = new DocTopicTokenFreqTable(numTopics,12);
		int numDocs = 1;
		int [][] documentLocalTopicCounts = {{3,3,3,2,1}};
		int maxDocLen = 0;
		for (int i = 0; i < documentLocalTopicCounts.length; i++) {
			maxDocLen += documentLocalTopicCounts[0][i];
		}

		for (int docNo = 0; docNo < numDocs; docNo++) {
			int [] localTopicCounts = documentLocalTopicCounts[docNo];
			for (int i = 0; i < numTopics; i++) {
				if(localTopicCounts[i]!=0) {
					docTopicTokenFreqTable.increment(i,(int)localTopicCounts[i]);
				}
			}
		}

		double [] psi = {0.5,0.2,0.1,0.1,0.1};
		double alpha = 1;	
		// 		
		// True values computed using the Anoniak distribution (9) in paper
		// Proportion (given local topic counts and psi) of the number of times
		// we have 1, 2, 3 tables
		double [][] trueProp = {
				{0.53333333, 0.40000000, 0.06666667}, 
				{0.75757576, 0.22727273, 0.01515152}, 
				{0.865800866, 0.129870130, 0.0043290041}, 
				{0.90909091, 0.09090909}, 
				{1}};
		int sampleSize = 10000;
		long [][] lSamples = new long[sampleSize][numTopics];
		for (int i = 0; i < sampleSize; i++) {
			for (int topic = 0; topic < numTopics; topic++) {
				// l_k is the number of tables
				int l_k = PoissonPolyaUrnHDPLDA.sampleL(topic, maxDocLen, docTopicTokenFreqTable, alpha, psi[topic]);
				lSamples[i][topic] = l_k;
			}
		}

		for (int topic = 0; topic < numTopics; topic++) {
			long [] props = new long[documentLocalTopicCounts[0][topic]];
			for (int sampleNr = 0; sampleNr < sampleSize; sampleNr++) {
				// Sampled l_k is the number of tables (1-3 in this case), so we have to subtract 1
				// when we use l_l as an index...
				props[(int)lSamples[sampleNr][topic]-1]++;
			}

			if(trueProp[topic].length==1) {
				assertEquals(sampleSize, props[0]);
			} else {
				ChiSquareTest cs = new ChiSquareTest();
				double test1 = cs.chiSquareTest(trueProp[topic], props);
				if(!(test1 > 0.01)) {
					System.out.println("Test:" + test1);
					System.err.println("TEST FAILS");
				} 
				assertTrue(test1 > 0.01);
			}
		}
	}

	@Test
	public void testSampleLSimR() {
		int numTopics = 5;
		DocTopicTokenFreqTable docTopicTokenFreqTable = new DocTopicTokenFreqTable(numTopics,7);
		int [][] documentLocalTopicCounts = {{2,1,3,1,0}, {3,0,1,0,0}, {0,0,1,1,0}};
		int numDocs = documentLocalTopicCounts.length;
		int maxDocLen = 0;
		for (int i = 0; i < documentLocalTopicCounts.length; i++) {
			int tmpLen = 0;
			for (int j = 0; j < documentLocalTopicCounts[i].length; j++) {				
				tmpLen += documentLocalTopicCounts[i][j];
			}
			if(tmpLen>maxDocLen) maxDocLen = tmpLen;
		}

		for (int docNo = 0; docNo < numDocs; docNo++) {
			int [] localTopicCounts = documentLocalTopicCounts[docNo];
			for (int i = 0; i < numTopics; i++) {
				if(localTopicCounts[i]!=0) {
					docTopicTokenFreqTable.increment(i,(int)localTopicCounts[i]);
				}
			}
		}

//		System.out.println("Freq table: \n" + docTopicTokenFreqTable);
//		System.out.println("Reverse cumsum 0: " + Arrays.toString(docTopicTokenFreqTable.getReverseCumulativeSum(0)));
		double [] psi = {0.1,0.1,0.1,0.1,0.1};
		double alpha = 1;

		int sampleSize = 10000;
		long [][] lSamples = new long[sampleSize][numTopics];
		for (int i = 0; i < sampleSize; i++) {
			for (int topic = 0; topic < numTopics; topic++) {
				// l_k is the number of tables
				int l_k = PoissonPolyaUrnHDPLDA.sampleL(topic, maxDocLen, docTopicTokenFreqTable, alpha, psi[topic]);
				lSamples[i][topic] = l_k;
			}
		}

		double [][] trueProp = {{0.787784, 0.196259, 0.015583, 0.000374},
				{1}, 
				{0.865271, 0.130358, 0.004371},
				{0, 1},
				{0}
		}; // Based on simulations so accept larger deviations
		
		// ### Topic 0
		long [] props = new long[maxDocLen];
		for (int sampleNr = 0; sampleNr < sampleSize; sampleNr++) {
			// Sampled l_k is the number of tables (1-3 in this case), so we have to subtract 1
			// when we use l_l as an index...
			props[(int)lSamples[sampleNr][0]-1]++;
		}
		long [] nonZeroCounts = new long[4];
		for (int i = 0; i < nonZeroCounts.length; i++) {
			// The first slot will be 0 so add +1
			nonZeroCounts[i] = props[i+1];
		} 

//		System.out.println("Props: " + Arrays.toString(nonZeroCounts));
		ChiSquareTest cs = new ChiSquareTest();
		double test1 = cs.chiSquareTest(trueProp[0], nonZeroCounts);
		if(!(test1 > 0.01)) {
			System.out.println("Test:" + test1);
			System.err.println("TEST FAILS");
		} 
		assertTrue(test1 > 0.01);
		
		// ### Topic 1
		props = new long[maxDocLen];
		for (int sampleNr = 0; sampleNr < sampleSize; sampleNr++) {
			// Sampled l_k is the number of tables (1-3 in this case), so we have to subtract 1
			// when we use l_l as an index...
			props[(int)lSamples[sampleNr][1]-1]++;
		}
//		System.out.println("Props: " + Arrays.toString(props));
		assertEquals(sampleSize, props[0]);
		
		// ### Topic 2
		props = new long[maxDocLen];
		for (int sampleNr = 0; sampleNr < sampleSize; sampleNr++) {
			// Sampled l_k is the number of tables (1-3 in this case), so we have to subtract 1
			// when we use l_l as an index...
			props[(int)lSamples[sampleNr][2]-1]++;
		}
		nonZeroCounts = new long[3];
		for (int i = 0; i < nonZeroCounts.length; i++) {
			// The two first slots will be 0 so add +2
			nonZeroCounts[i] = props[i+2];
		} 

//		System.out.println("Props: " + Arrays.toString(nonZeroCounts));
		cs = new ChiSquareTest();
		test1 = cs.chiSquareTest(trueProp[2], nonZeroCounts);
		if(!(test1 > 0.01)) {
			System.out.println("Test:" + test1);
			System.err.println("TEST FAILS");
		} 
		assertTrue(test1 > 0.01);
		
		// ### Topic 3
		props = new long[maxDocLen];
		for (int sampleNr = 0; sampleNr < sampleSize; sampleNr++) {
			// Sampled l_k is the number of tables (1-3 in this case), so we have to subtract 1
			// when we use l_l as an index...
			props[(int)lSamples[sampleNr][3]-1]++;
		}
//		System.out.println("Props: " + Arrays.toString(props));
		assertEquals(0, props[0]);
		assertEquals(sampleSize, props[1]);
		
		
		// ### Topic 4
		long sum = 0;
		for (int sampleNr = 0; sampleNr < sampleSize; sampleNr++) {
			// Sampled l_k is the number of tables (1-3 in this case), so we have to subtract 1
			// when we use l_l as an index...
			sum += lSamples[sampleNr][4];
		}
//		System.out.println("Sum: " + sum);
		assertEquals(0, sum);
	}

	@Test
	public void testSampleL() {
		int numTopics = 5;
		DocTopicTokenFreqTable docTopicTokenFreqTable = new DocTopicTokenFreqTable(numTopics,6);
		int numDocs = 3;
		int [][] documentLocalTopicCounts = {{0,0,5,1,0}, {1,0,1,0,0},{3,0,1,1,0}};
		int maxDocLen = 0;
		for (int i = 0; i < documentLocalTopicCounts.length; i++) {
			int tmpLen = 0;
			for (int j = 0; j < documentLocalTopicCounts[i].length; j++) {				
				tmpLen += documentLocalTopicCounts[i][j];
			}
			if(tmpLen>maxDocLen) maxDocLen = tmpLen;
		}

		for (int docNo = 0; docNo < numDocs; docNo++) {
			int [] localTopicCounts = documentLocalTopicCounts[docNo];
			for (int i = 0; i < numTopics; i++) {
				if(localTopicCounts[i]!=0) {
					docTopicTokenFreqTable.increment(i,(int)localTopicCounts[i]);
				}
			}
		}

		int topic = 0;
//		System.out.println("Freq table: \n" + docTopicTokenFreqTable);
//		System.out.println("Reverse cumsum 0: " + Arrays.toString(docTopicTokenFreqTable.getReverseCumulativeSum(topic)));
		double psi = 0.1;
		double alpha = 1;

		int [] lSamples = new int[200];
		for (int i = 0; i < 2000; i++) {
			int l_k = PoissonPolyaUrnHDPLDA.sampleL(topic, maxDocLen, docTopicTokenFreqTable, alpha, psi);
			lSamples[l_k]++;
		}

//		System.out.println("Samples: " + Arrays.toString(lSamples));
	}

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
