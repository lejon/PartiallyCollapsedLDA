package cc.mallet.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.commons.math3.stat.inference.ChiSquareTest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import cc.mallet.types.Dirichlet;
import cc.mallet.util.GentleAliasMethod;
import cc.mallet.util.OptimizedGentleAliasMethod;
import cc.mallet.util.OptimizedGentleAliasMethodDynamicSize;
import cc.mallet.util.WalkerAliasTable;

@RunWith(value = Parameterized.class)
public class WalkerAliasTableTest {

	@SuppressWarnings("rawtypes")
	Class walkerClass;
	WalkerAliasTable walker;
	double epsilon = 0.001;
	double alpha = 0.001;

	public WalkerAliasTableTest(@SuppressWarnings("rawtypes") Class testClass) {
		walkerClass = testClass;
	}

	@Parameters
	public static List<Object[]> data() {
		Object[][] impls = new Object[][] { { GentleAliasMethod.class }, { OptimizedGentleAliasMethod.class }, { OptimizedGentleAliasMethodDynamicSize.class } };
		return Arrays.asList(impls);
	}

	@Before
	public void noSetup() throws InstantiationException, IllegalAccessException {
		walker = (WalkerAliasTable) walkerClass.newInstance();
	}

	//@Test Hmm, TODO what is the strategy for handling this??
	public void divTest() {
		assertTrue((3./3.0)-(1.0/3.0) <= 1.0/3.0);
	}
	
	@Test
	public void basicTest() {
		double [] probs = {2.0/15.0,7.0/15.0,6.0/15.0};
		walker.initTableNormalizedProbabilities(probs);	
		int noSamples = 100000;
		int [] samples = new int[noSamples];
		int [] cnts = new int[probs.length];
		for (int i = 0; i < samples.length; i++) {
			samples[i] = walker.generateSample();
			cnts[samples[i]]++;
		}
		//System.out.println("Counts: ");
		for (int i = 0; i < cnts.length; i++) {			
			//System.out.print(cnts[i] + " (" + ((double)cnts[i])/noSamples + "),"  + probs[i]);
			double meanDiff = probs[i] - (((double)cnts[i])/noSamples);
			double stdi = Math.sqrt((probs[i]*(1-probs[i]))/noSamples);
			double sigmas = Math.abs(meanDiff) / stdi;
			assertTrue(sigmas<6);
		}
	}
	
	@Test
	public void normalizationTest() {
		double [] uNormProbs = {10.0,20.0,5.0,60.0,0.5};
		double [] probs = uNormProbs.clone(); 
		double sum = 0.0;
		for (int i = 0; i < uNormProbs.length; i++) {
			sum+=uNormProbs[i];
		}
		
		for (int i = 0; i < probs.length; i++) {
			probs[i] = probs[i] / sum;
		}
		
		walker.initTable(uNormProbs,sum);	
		
		int noLoops = 100;
		for (int j = 0; j < noLoops; j++) {
			int noSamples = 1_000_000;
			int [] samples = new int[noSamples];
			int [] cnts = new int[uNormProbs.length];
			for (int i = 0; i < samples.length; i++) {
				samples[i] = walker.generateSample();
				cnts[samples[i]]++;
			}
			//System.out.println("Counts: ");
			for (int i = 0; i < cnts.length; i++) {			
				//System.out.print(cnts[i] + " (" + ((double)cnts[i])/noSamples + "),");
				double meanDiff = uNormProbs[i] - (((double)cnts[i])/noSamples);
				double stdi = Math.sqrt(noSamples * (probs[i]*(1-probs[i])));
				double sigmas = meanDiff / stdi;
				assertTrue(sigmas<6);
				//assertEquals(probs[i], ((double)cnts[i])/noSamples, epsilon);
			}
			//System.out.println();
		}
	}

	@Test
	public void testModerate() {
		double [] alphas = {0.5,0.3,0.1,0.01,0.01};
		Dirichlet distgen = new Dirichlet(alphas);
		double [] probs = distgen.nextDistribution(); 
		walker.initTableNormalizedProbabilities(probs);
		int noSamples = 5_000_000;
		int [] samples = new int[noSamples];
		long [] cnts = new long[probs.length];
		for (int i = 0; i < samples.length; i++) {
			samples[i] = walker.generateSample();
			cnts[samples[i]]++;
		}
		ChiSquareTest cs = new ChiSquareTest();
		if(cs.chiSquareTest(probs, cnts, alpha)) {
			double [] calcProbs = new double[probs.length];
			for (int i = 0; i < calcProbs.length; i++) {
				calcProbs[i] = ((double)cnts[i] / noSamples);
			}
			fail("Probs are not equal: " + Arrays.toString(probs) + "\n" +  Arrays.toString(calcProbs));
		}
	}


	@Test
	public void testExtreme() {
		Dirichlet distgen = new Dirichlet(900);
		double [] probs = distgen.nextDistribution(); 
		walker.initTableNormalizedProbabilities(probs);
		int noSamples = 15_000_000;
		int [] samples = new int[noSamples];
		long [] cnts = new long[probs.length];
		for (int i = 0; i < samples.length; i++) {
			samples[i] = walker.generateSample();
			cnts[samples[i]]++;
		}
		ChiSquareTest cs = new ChiSquareTest();
		if(cs.chiSquareTest(probs, cnts, alpha)) {
			double [] calcProbs = new double[probs.length];
			for (int i = 0; i < calcProbs.length; i++) {
				calcProbs[i] = ((double)cnts[i] / noSamples);
			}
			fail("Probs are not equal: " + Arrays.toString(probs) + "\n" +  Arrays.toString(calcProbs));
		}
	}

	@Test
	public void testChiSq() {
		double [] probs = {2.0/15.0,7.0/15.0,6.0/15.0};
		walker.initTableNormalizedProbabilities(probs);
		int noSamples = 1_000_000;
		int [] samples = new int[noSamples];
		for (int i = 0; i < samples.length; i++) {
			samples[i] = walker.generateSample();
		}
		long [] cnts = new long[probs.length];
		for (int i = 0; i < samples.length; i++) {
			cnts[samples[i]]++;
		}

		ChiSquareTest cs = new ChiSquareTest();
		
		if(cs.chiSquareTest(probs, cnts, alpha)) {
			double [] calcProbs = new double[probs.length];
			for (int i = 0; i < calcProbs.length; i++) {
				calcProbs[i] = ((double)cnts[i] / noSamples);
			}
			fail("Probs are not equal: " + Arrays.toString(probs) + "\n" +  Arrays.toString(calcProbs));
		}
	}
	
	@Test
	public void testChiSqZeroProb0() {
		double [] probs = {0.0/15.0, 5.0/15.0, 10.0/15.0};
		walker.initTableNormalizedProbabilities(probs);
		int noSamples = 1_000_000;
		int [] samples = new int[noSamples];
		for (int i = 0; i < samples.length; i++) {
			samples[i] = walker.generateSample();
		}
		long [] cnts = new long[probs.length];
		for (int i = 0; i < samples.length; i++) {
			cnts[samples[i]]++;
		}
		assertEquals(0, cnts[0]);
	}
	
	@Test
	public void testChiSqZeroProb1() {
		double [] probs = {5.0/15.0, 0.0/15.0, 10.0/15.0};
		walker.initTableNormalizedProbabilities(probs);
		int noSamples = 1_000_000;
		int [] samples = new int[noSamples];
		for (int i = 0; i < samples.length; i++) {
			samples[i] = walker.generateSample();
		}
		long [] cnts = new long[probs.length];
		for (int i = 0; i < samples.length; i++) {
			cnts[samples[i]]++;
		}
		assertEquals(0, cnts[1]);
	}

	@Test
	public void testChiSqZeroProb3() {
		double [] probs = {10.0/15.0, 5.0/15.0, 0.0/15.0};
		walker.initTableNormalizedProbabilities(probs);
		int noSamples = 1_000_000;
		int [] samples = new int[noSamples];
		for (int i = 0; i < samples.length; i++) {
			samples[i] = walker.generateSample();
		}
		long [] cnts = new long[probs.length];
		for (int i = 0; i < samples.length; i++) {
			cnts[samples[i]]++;
		}
		assertEquals(0, cnts[2]);
	}

	
	@Test
	public void testCompareMultinomialSampling() {
		int dirichletDim = 1000;
		Dirichlet distgen = new Dirichlet(dirichletDim);
		double [] probs = distgen.nextDistribution(); 
		walker.initTableNormalizedProbabilities(probs);
		int noSamples = 1000000;
		int [] samples = new int[noSamples];
		
		// Generate samples from Alias table
		int [] cnts = new int[probs.length];
//		long tstart = System.currentTimeMillis();
		for (int i = 0; i < noSamples; i++) {
			samples[i] = walker.generateSample();
			cnts[samples[i]]++;
		}
//		long tend = System.currentTimeMillis();
				
		// Generate samples from plain multinomial sampler
//		long mstart = System.currentTimeMillis();
		multinomialSampler(probs, noSamples);	
//		long mend = System.currentTimeMillis();
		
		
		int [] multiCnts = new int[probs.length];
		for (int i = 0; i < samples.length; i++) {
			multiCnts[samples[i]]++;
		}

		//System.out.println("Counts: ");
		for (int i = 0; i < probs.length; i++) {			
			//System.out.print(samples[i] + " (" + ((double)samples[i])/noSamples + "),");
			assertEquals(probs[i], ((double)cnts[i])/noSamples, epsilon);
			assertEquals(probs[i], ((double)multiCnts[i])/noSamples, epsilon);
			assertEquals(multiCnts[i],cnts[i]);
		}
		//System.out.println();

		//System.out.println("Alias Table sampling took: " + (tend-tstart) + " milliseconds");
		//System.out.println("Multinomial sampling took: " + (mend-mstart) + " milliseconds");
		//System.out.println("For " + noSamples + " samples and a " + dirichletDim + " dimensional Dirichlet...");
	}

	int [] multinomialSampler(double[] probs, int noSamples) {
		int[] multinomialSamples = new int[noSamples];
		for (int i = 0; i < noSamples; i++) {
			double U = ThreadLocalRandom.current().nextDouble();
			int theSample = -1;
			while (U > 0.0) {
				theSample++;
				U -= probs[theSample];
			} 
			multinomialSamples[i] = theSample;
		}
		return multinomialSamples;
	}

}
