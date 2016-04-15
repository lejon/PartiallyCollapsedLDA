package cc.mallet.misc;

import static org.junit.Assert.*;

import java.util.Arrays;

import org.apache.commons.math3.distribution.ChiSquaredDistribution;
import org.apache.commons.math3.stat.inference.AlternativeHypothesis;
import org.apache.commons.math3.stat.inference.BinomialTest;
import org.apache.commons.math3.stat.inference.ChiSquareTest;
import org.junit.Test;

import cc.mallet.types.Dirichlet;
import cc.mallet.util.SystematicSampling;

public class SystematicSamplingTest {

	@Test(expected=IllegalArgumentException.class)
	public void testZeroCountFirst() {
		int [] counts = {0, 1, 140, 14, 20, 13, 110, 4, 29, 90, 34, 1, 29, 230, 1};
		int sum = SystematicSampling.sum(counts);
		System.out.println("Sum is: " + sum);		
		int [] samples = new int[counts.length]; 
		int n = sum;
		int loops = 100000;
		System.out.println("max is: " + n);
		for (int loop = 0; loop < loops; loop++) {
			int [] indices = SystematicSampling.sample(counts, n);
			for (int j = 0; j < indices.length; j++) {
				samples[indices[j]] += 1;
			}
		}
		// Shouldn't get here
		fail();
	}

	@Test(expected=IllegalArgumentException.class)
	public void testZeroCountLast() {
		int [] counts = {1, 140, 14, 20, 13, 110, 4, 29, 90, 34, 1, 29, 230, 1, 0};
		int sum = SystematicSampling.sum(counts);
		System.out.println("Sum is: " + sum);		
		int [] samples = new int[counts.length]; 
		int n = sum;
		int loops = 100000;
		System.out.println("max is: " + n);
		for (int loop = 0; loop < loops; loop++) {
			int [] indices = SystematicSampling.sample(counts, n);
			for (int j = 0; j < indices.length; j++) {
				samples[indices[j]] += 1;
			}
		}
		// Shouldn't get here
		fail();
	}

	@Test(expected=IllegalArgumentException.class)
	public void testZeroCountsomewhere() {
		int [] counts = {1, 140, 14, 20, 13, 110, 4, 0, 29, 90, 34, 1, 29, 230, 1};
		int sum = SystematicSampling.sum(counts);
		System.out.println("Sum is: " + sum);		
		int [] samples = new int[counts.length]; 
		int n = sum;
		int loops = 100000;
		System.out.println("max is: " + n);
		for (int loop = 0; loop < loops; loop++) {
			int [] indices = SystematicSampling.sample(counts, n);
			for (int j = 0; j < indices.length; j++) {
				samples[indices[j]] += 1;
			}
		}
		// Shouldn't get here
		fail();
	}

	@Test
	public void testSystemicSamplingProbs() {
		BinomialTest bt = new BinomialTest();
		double alpha = 0.0001;
		int [] counts = {1, 140, 14, 20, 13, 110, 4, 29, 90, 34, 1, 29, 230, 1};
		int sum = SystematicSampling.sum(counts);
		System.out.println("Sum is: " + sum);		
		int [] samples = new int[counts.length]; 
		int n = sum;
		int loops = 100_000;
		System.out.println("max is: " + n);
		for (int loop = 0; loop < loops; loop++) {
			int [] indices = SystematicSampling.sample(counts, n);
			for (int j = 0; j < indices.length; j++) {
				samples[indices[j]] += 1;
			}
		}

		double [] probs = new double[counts.length];
		for (int i = 0; i < probs.length; i++) {
			probs[i] = ((double)counts[i] / (double)n);
		}

		double [] sampleProbs = new double[counts.length];
		for (int i = 0; i < probs.length; i++) {
			sampleProbs[i] = ((double)samples[i] / (double)loops);
		}

		System.out.println("Given probs are:");
		for (int i = 0; i < probs.length; i++) {
			System.out.print(String.format("%02f, ",probs[i]));
		}
		System.out.println();
		System.out.println("Sample probs are:");
		for (int i = 0; i < sampleProbs.length; i++) {
			System.out.print(String.format("%02f, ", sampleProbs[i]));
		}
		System.out.println();

		for (int i = 0; i < samples.length; i++) {			
			double p_value = bt.binomialTest(loops, samples[i], ((double)counts[i] / (double)n), AlternativeHypothesis.TWO_SIDED);
			System.out.println("P-value: " + p_value);
			assertTrue(p_value>alpha);
		}
	}

	@Test
	public void testSystemicSamplingProbsWithProbOne() {
		BinomialTest bt = new BinomialTest();
		double alpha = 0.0001;
		int [] counts = {1, 140, 14, 20, 13, 110, 4, 29, 90, 34, 1, 29, 230, 1};
		int sum = SystematicSampling.sum(counts);
		System.out.println("Sum is: " + sum);		
		int [] samples = new int[counts.length]; 
		int n = 100;
		int loops = 100000;
		System.out.println("max is: " + n);
		for (int loop = 0; loop < loops; loop++) {
			int [] indices = SystematicSampling.sample(counts, n);
			for (int j = 0; j < indices.length; j++) {
				samples[indices[j]] += 1;
			}
		}

		double [] probs = new double[counts.length];
		for (int i = 0; i < probs.length; i++) {
			probs[i] = ((double)counts[i] / (double)n);
		}

		double [] sampleProbs = new double[counts.length];
		for (int i = 0; i < probs.length; i++) {
			sampleProbs[i] = ((double)samples[i] / (double)loops);
		}

		System.out.println("Given probs are:");
		for (int i = 0; i < probs.length; i++) {
			System.out.print(String.format("%02f, ",probs[i]));
		}
		System.out.println();
		System.out.println("Sample probs are:");
		for (int i = 0; i < sampleProbs.length; i++) {
			System.out.print(String.format("%02f, ", sampleProbs[i]));
		}
		System.out.println();

		for (int i = 0; i < samples.length; i++) {	
			double probability = (double)counts[i] / (double)n;
			if(probability>=1.0) {
				assertTrue(loops == samples[i]);
			} else {
				double p_value = bt.binomialTest(loops, samples[i], probability, AlternativeHypothesis.TWO_SIDED);
				System.out.println("P-value: " + p_value);
				assertTrue(p_value>alpha);
			}
		}
	}

	@Test
	public void testSystemicSampling() {
		double epsilon = 0.001;
		int [] counts = {4, 140, 14, 20, 13, 110, 29, 90, 34, 29, 230};
		int sum = SystematicSampling.sum(counts);
		System.out.println("Sum is: " + sum);
		double [] probs = new double[counts.length];
		for (int i = 0; i < probs.length; i++) {
			probs[i] = ((double)counts[i]) / sum;
		}

		int [] samples = new int[counts.length]; 
		int n = SystematicSampling.max(counts);
		int loops = 5000;
		System.out.println("max is: " + n);
		for (int loop = 0; loop < loops; loop++) {
			int [] indices = SystematicSampling.sample(counts, n);
			for (int j = 0; j < indices.length; j++) {
				samples[indices[j]] += 1;
			}
		}
		System.out.println("Samples are:");
		for (int i = 0; i < samples.length; i++) {
			System.out.print(samples[i] + ", ");
		}
		System.out.println();

		int sampleSum = SystematicSampling.sum(samples);
		double [] sampleProbs = new double[counts.length];
		for (int i = 0; i < probs.length; i++) {
			sampleProbs[i] = ((double)samples[i]) / sampleSum;
		}

		System.out.println("Given probs are:");
		for (int i = 0; i < probs.length; i++) {
			System.out.print(String.format("%02f, ",probs[i]));
		}
		System.out.println();
		System.out.println("Sample probs are:");
		for (int i = 0; i < sampleProbs.length; i++) {
			System.out.print(String.format("%02f, ", sampleProbs[i]));
		}
		System.out.println();

		Dirichlet dir = new Dirichlet(probs);
		double llratio = dir.dirichletMultinomialLikelihoodRatio(counts, samples);
		System.out.println("LLratio: " + llratio);

		ChiSquareTest cs = new ChiSquareTest();
		long [] lcounts = new long[counts.length];
		for (int i = 0; i < lcounts.length; i++) {
			lcounts[i] = counts [i];
		}
		long [] lsamples = new long[samples.length];
		for (int i = 0; i < lcounts.length; i++) {
			lsamples[i] = samples[i];
		}

		ChiSquaredDistribution distribution = new ChiSquaredDistribution((double) counts.length - 1);
		System.out.println("Chi of llratio: " +  (1-distribution.cumulativeProbability(llratio)));

		double pval = cs.chiSquareTestDataSetsComparison(lcounts, lsamples);
		System.out.println("p-value: " + pval);
		// The test returns:
		/* The number (pval) returned is the smallest significance level at which one
		 * can reject the null hypothesis that the observed counts conform to the
		 * same distribution.
		 */
		if(llratio<epsilon) {
			fail("Counts are not from the same distribution: p-value = " + pval);
		}
	}
	
	@Test
	public void testSystemicSamplingMaxTimes() {
		double epsilon = 0.001;
		int [] counts = {4, 140, 14, 20, 13, 110, 29, 90, 34, 29, 230};
		
		System.out.println("Given counts are:");
		for (int i = 0; i < counts.length; i++) {
			System.out.print(counts[i] + ", ");
		}
		System.out.println();
		
		int sum = SystematicSampling.sum(counts);
		System.out.println("Sum is: " + sum);
		double [] probs = new double[counts.length];
		for (int i = 0; i < probs.length; i++) {
			probs[i] = ((double)counts[i]) / sum;
		}

		int [] samples = new int[counts.length]; 
		int n = SystematicSampling.max(counts);
		int loops = n;
		System.out.println("max is: " + n);
		for (int loop = 0; loop < loops; loop++) {
			int [] indices = SystematicSampling.sample(counts, n);
			for (int j = 0; j < indices.length; j++) {
				samples[indices[j]] += 1;
			}
		}
		System.out.println("Samples are:");
		for (int i = 0; i < samples.length; i++) {
			System.out.print(samples[i] + ", ");
		}
		System.out.println();

		int sampleSum = SystematicSampling.sum(samples);
		double [] sampleProbs = new double[counts.length];
		for (int i = 0; i < probs.length; i++) {
			sampleProbs[i] = ((double)samples[i]) / sampleSum;
		}

		System.out.println("Given probs are:");
		for (int i = 0; i < probs.length; i++) {
			System.out.print(String.format("%02f, ",probs[i]));
		}
		System.out.println();
		System.out.println("Sample probs are:");
		for (int i = 0; i < sampleProbs.length; i++) {
			System.out.print(String.format("%02f, ", sampleProbs[i]));
		}
		System.out.println();

		Dirichlet dir = new Dirichlet(probs);
		double llratio = dir.dirichletMultinomialLikelihoodRatio(counts, samples);
		System.out.println("LLratio: " + llratio);

		ChiSquareTest cs = new ChiSquareTest();
		long [] lcounts = new long[counts.length];
		for (int i = 0; i < lcounts.length; i++) {
			lcounts[i] = counts [i];
		}
		long [] lsamples = new long[samples.length];
		for (int i = 0; i < lcounts.length; i++) {
			lsamples[i] = samples[i];
		}

		ChiSquaredDistribution distribution = new ChiSquaredDistribution((double) counts.length - 1);
		System.out.println("Chi of llratio: " +  (1-distribution.cumulativeProbability(llratio)));

		double pval = cs.chiSquareTestDataSetsComparison(lcounts, lsamples);
		System.out.println("p-value: " + pval);
		// The test returns:
		/* The number (pval) returned is the smallest significance level at which one
		 * can reject the null hypothesis that the observed counts conform to the
		 * same distribution.
		 */
		if(llratio<epsilon) {
			fail("Counts are not from the same distribution: p-value = " + pval);
		}
	}

	/** 
	 * When the step is 1 all bins should be included as many times as
	 * we sample.
	 */
	@Test
	public void testSystemicSamplingStep1() {
		int [] counts = {4, 140, 14, 20, 13, 110, 29, 90, 34, 29, 230};
		int [] samples = new int[counts.length]; 
		int loops = SystematicSampling.max(counts);
		for (int loop = 0; loop < loops; loop++) {
			int [] indices = SystematicSampling.sample(counts, 1);
			for (int j = 0; j < indices.length; j++) {
				samples[indices[j]] += 1;
			}
		}		
		int [] expected = new int[counts.length];
		Arrays.fill(expected, loops);
		for (int i = 0; i < expected.length; i++) {			
			assertEquals(expected[i], samples[i]);
		}
	}

	/** 
	 * When the step is 50 all bins with counts > 50 should be included 
	 * as many times as we sample.
	 */
	@Test
	public void testSystemicSamplingStep50() {
		int [] counts = {5, 4, 140, 14, 20, 13, 110, 29, 90, 34, 29, 230};
		int [] samples = new int[counts.length]; 
		int n = SystematicSampling.max(counts);
		//n = 1;
		for (int loop = 0; loop < n; loop++) {
			int [] indices = SystematicSampling.sample(counts, 50);
			for (int j = 0; j < indices.length; j++) {
				samples[indices[j]] += 1;
			}
		}		
		int [] expectedIdxs = {2,6,8,11}; 
		int [] notExpectedIdxs = {0,1,3,4,5,7,9,10}; 

		for (int i = 0; i < samples.length; i++) {	
			System.out.print(samples[i] + ", ");
		}
		for (int i = 0; i < expectedIdxs.length; i++) {	
			assertTrue(samples[expectedIdxs[i]]==n);
		}
		for (int i = 0; i < notExpectedIdxs.length; i++) {	
			assertTrue(samples[notExpectedIdxs[i]]<n);
		}
	}
}
