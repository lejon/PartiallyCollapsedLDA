package cc.mallet.types;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.apache.commons.math3.distribution.BinomialDistribution;
import org.apache.commons.math3.stat.inference.ChiSquareTest;
import org.junit.Ignore;
import org.junit.Test;

import cc.mallet.topics.PoissonPolyaUrnTest;

@Ignore("not robust enough yet") public class BinomialSamplerTest {

//  These statistical tests fail too often, they are now
//  more a nuisance than a help. We have confidence enough
//  in the sampler to remove them. We get Similar failing 
//	results if we run the same setup in R
//	@Test
//	public void testBinomialSamplerUsingCounts() {
//		// Setup draws
//		int noDraws = 500_000;
//		int [] trialss = {2, 10, 20, 50, 100, 200};
//		double alpha = 0.005;
//		double [] probs = {0.001, 0.01, 0.1, 0.5};
//
//		int samplesLen = 1000;
//		for (int trials : trialss) {	
//			for (double prob : probs) {
//				long [] samplesBinSampler = new long[samplesLen]; 
//				long [] samplesB = new long[samplesLen]; 
//
//				BinomialDistribution binDist = new BinomialDistribution(trials, prob);
//				for (int i = 0; i < noDraws; i++) {
//					int rbinomDraw = (int)BinomialSampler.rbinom(trials,prob);
//					if(rbinomDraw>trials) fail("Binomial draw is bigger than trials");
//					samplesBinSampler[rbinomDraw]++;
//					int binSample = binDist.sample(); 
//					samplesB[binSample]++;
//				}
//
//				int [] rg = PoissonPolyaUrnTest.findSeqRange(samplesB);
//
//				int smallestIdx = rg[0];
//				int largestIdx = rg[1];
//
//				int obsLen = largestIdx - smallestIdx;
//				//				System.out.println("Obs. Len.: " + obsLen);
//				// Adapt to the test preconditions
//				long [] obsSampler = new long[obsLen];
//				long [] obsBin = new long[obsLen];
//				for (int i = smallestIdx; i < largestIdx; i++) {
//					obsSampler[i-smallestIdx] = samplesBinSampler[i];
//					obsBin[i-smallestIdx] = samplesB[i];
//				}
//
//				ChiSquareTest cs = new ChiSquareTest();
//				double test1 = cs.chiSquareTestDataSetsComparison(obsBin, obsSampler);
//				//System.out.println("P-value: " + test1);
//				if(!(test1 > alpha)) {
//					System.out.println("Trials: " + trials + " prob:" + prob);
//					System.out.println("Obs: " + Arrays.toString(obsBin));
//					System.out.println("Sam: " + Arrays.toString(obsSampler));
//					System.out.println("P-value: " + test1);
//				}
//				assertTrue(test1 > alpha);
//			}
//		}
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
				double alpha = 0.005;
				if(!(test1 > alpha)) {
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

//  These statistical tests fail too often, they are now
//  more a nuisance than a help. We have confidence enough
//  in the sampler to remove them. We get Similar failing 
//	results if we run the same setup in R
//	@Test
//	public void testBinomialUsingProbs() {
//		// Setup draws
//		for(int loops = 0; loops < 50; loops++) {
//			System.out.println("Loop: " + loops);
//		int noDraws = 500_000;
//		int tableLength = 200;
//		int [] trialss = {15, 20, 50, 100};
//		double [] probs = {0.01, 0.1, 0.5, 0.75};
//
//		for (int trials : trialss) {	
//			for (double prob : probs) {
//				long [] samplesB = new long[tableLength]; 
//
//				double [] trialProbabilities = new double [tableLength];
//				BinomialDistribution binDistCalc = new BinomialDistribution(trials, prob);
//				for (int j = 0; j < tableLength; j++) {
//					trialProbabilities[j] =  binDistCalc.probability(j);
//				}
//
//				int maxIdx = 0;
//				for (int i = 0; i < noDraws; i++) {
//					int draw = BinomialSampler.rbinom(trials, prob);
//					samplesB[draw]++;
//					if(draw>maxIdx) {
//						maxIdx = draw;
//					}
//				}
//				
//				int [] rg = PoissonPolyaUrnTest.findSeqRange(samplesB);
//
//				int smallestIdx = rg[0];
//				int largestIdx = Math.min(rg[1],maxIdx);
//				
//				int obsLen = largestIdx - smallestIdx;
//
//				// Adapt to the test preconditions
//				long [] obsBinomSampler = new long[obsLen];
//				double [] possibleProbs = new double[obsLen];
//				for (int i = smallestIdx; i < largestIdx; i++) {
//					obsBinomSampler[i-smallestIdx] = samplesB[i];
//					possibleProbs[i-smallestIdx] = trialProbabilities[i];
//				}
//				// Ensure that probs sum to 1
//				double probsum = 0.0;
//				for (int i = 0; i < possibleProbs.length; i++) {
//					probsum += possibleProbs[i];
//				}
//				for (int i = 0; i < possibleProbs.length; i++) {
//					possibleProbs[i] /= probsum;
//				}
//
//				ChiSquareTest cs = new ChiSquareTest();
//				double test1 = cs.chiSquareTest(possibleProbs,obsBinomSampler);
//				double alpha = 0.005;
//				//System.out.println("P-value: " + test1);
//				if(test1 <= alpha) {
//					System.out.println("Samples: " + noDraws);
//					System.out.println("Range: " + smallestIdx + ".." + largestIdx);
//					System.out.println("Trials: " + trials + " prob: " + prob);
//					System.out.println("Probs:" + Arrays.toString(possibleProbs));
//					System.out.println("Sampler:" + Arrays.toString(obsBinomSampler));
//					System.out.println("P-value: " + test1);
//					System.out.println();
//				}
//				assertTrue(test1 > alpha);
//			}
//		}
//		}
//	}
}
