package cc.mallet.types;

import static org.junit.Assert.assertTrue;

import org.apache.commons.math3.distribution.BinomialDistribution;
import org.apache.commons.math3.stat.inference.ChiSquareTest;
import org.junit.Test;

import cc.mallet.topics.PoissonPolyaUrnTest;

public class BinomialSamplerTest {

	@Test
	public void testBinomialSamplerUsingCounts() {
		// Setup draws
		int noDraws = 500_000;
		int [] trialss = {2, 10, 20, 50, 100, 200};
		double [] probs = {0.001, 0.01, 0.1, 0.5};
		
		int samplesLen = 1000;
		for (int trials : trialss) {	
			for (double prob : probs) {
				long [] samplesBinSampler = new long[samplesLen]; 
				long [] samplesB = new long[samplesLen]; 

				BinomialDistribution binDist = new BinomialDistribution(trials, prob);
				for (int i = 0; i < noDraws; i++) {
					samplesBinSampler[(int)BinomialSampler.rbinom(trials,prob)]++;
					int binSample = binDist.sample(); 
					samplesB[binSample]++;
				}
				
				int [] rg = PoissonPolyaUrnTest.findSeqRange(samplesB);

				int smallestIdx = rg[0];
				int largestIdx = rg[1];
				
				int obsLen = largestIdx - smallestIdx;
//				System.out.println("Obs. Len.: " + obsLen);
				// Adapt to the test preconditions
				long [] obsSampler = new long[obsLen];
				long [] obsBin = new long[obsLen];
				for (int i = smallestIdx; i < largestIdx; i++) {
					obsSampler[i-smallestIdx] = samplesBinSampler[i];
					obsBin[i-smallestIdx] = samplesB[i];
				}
								
				ChiSquareTest cs = new ChiSquareTest();
				double test1 = cs.chiSquareTestDataSetsComparison(obsBin, obsSampler);
//				System.out.println(test1);
				assertTrue(test1 > 0.01);
//				System.out.println();
			}
		}
	}
	

}
