package cc.mallet.utils;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import cc.mallet.types.Dirichlet;
import cc.mallet.util.GentleAliasMethod;
import cc.mallet.util.OptimizedGentleAliasMethod;
import cc.mallet.util.WalkerAliasTable;

@RunWith(value = Parameterized.class)
public class WalkerAliasTableTest {

	@SuppressWarnings("rawtypes")
	Class walkerClass;
	WalkerAliasTable walker;
	double epsilon = 0.001;

	public WalkerAliasTableTest(@SuppressWarnings("rawtypes") Class testClass) {
		walkerClass = testClass;
	}

	@Parameters
	public static List<Object[]> data() {
		Object[][] impls = new Object[][] { { GentleAliasMethod.class }, { OptimizedGentleAliasMethod.class } };
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
		for (int i = 0; i < probs.length; i++) {
			System.out.println(probs[i]);
		}
		walker.initTableNormalizedProbabilities(probs);	
		int noSamples = 100000;
		int [] samples = new int[noSamples];
		for (int i = 0; i < samples.length; i++) {
			samples[i] = walker.generateSample();
		}
		int [] cnts = new int[probs.length];
		for (int i = 0; i < samples.length; i++) {
			cnts[samples[i]]++;
		}
		System.out.println("Counts: ");
		for (int i = 0; i < cnts.length; i++) {			
			System.out.print(cnts[i] + " (" + ((double)cnts[i])/noSamples + "),"  + probs[i]);
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
			for (int i = 0; i < samples.length; i++) {
				samples[i] = walker.generateSample();
			}
			int [] cnts = new int[uNormProbs.length];
			for (int i = 0; i < samples.length; i++) {
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
		int noLoops = 100;
		for (int j = 0; j < noLoops; j++) {
			double [] probs = distgen.nextDistribution(); 
			walker.initTableNormalizedProbabilities(probs);
			int noSamples = 1_000_000;
			int [] samples = new int[noSamples];
			for (int i = 0; i < samples.length; i++) {
				samples[i] = walker.generateSample();
			}
			int [] cnts = new int[probs.length];
			for (int i = 0; i < samples.length; i++) {
				cnts[samples[i]]++;
			}
			//System.out.println("Counts: ");
			for (int i = 0; i < cnts.length; i++) {			
				//System.out.print(cnts[i] + " (" + ((double)cnts[i])/noSamples + "),");
				double meanDiff = probs[i] - (((double)cnts[i])/((double)noSamples));
				double stdi = Math.sqrt((probs[i]*(1-probs[i]))/noSamples);
				double sigmas = Math.abs(meanDiff) / stdi;
				if(sigmas<6) {
					System.out.println("Sigmas = " + sigmas);
					System.out.print("Probs: [");
					for (int k = 0; k < probs.length; k++) {
						System.out.print(probs[i] + ", ");
					}
					System.out.println("]");
				}
				assertTrue(sigmas<6);
				//assertEquals(probs[i], ((double)cnts[i])/noSamples, epsilon);
			}
			//System.out.println();
		}
	}


	@Test
	public void testExtreme() {
		Dirichlet distgen = new Dirichlet(900);
		int noLoops = 100;
		for (int j = 0; j < noLoops; j++) {
			double [] probs = distgen.nextDistribution(); 
			walker.initTableNormalizedProbabilities(probs);
			int noSamples = 1_000_000;
			int [] samples = new int[noSamples];
			for (int i = 0; i < samples.length; i++) {
				samples[i] = walker.generateSample();
			}
			int [] cnts = new int[probs.length];
			for (int i = 0; i < samples.length; i++) {
				cnts[samples[i]]++;
			}
			//System.out.println("Counts: ");
			for (int i = 0; i < cnts.length; i++) {			
				//System.out.print(cnts[i] + " (" + ((double)cnts[i])/noSamples + "),");
				double meanDiff = probs[i] - (((double)cnts[i])/noSamples);
				double stdi = Math.sqrt((probs[i]*(1-probs[i]))/noSamples);
				double sigmas = Math.abs(meanDiff) / stdi;
				if(sigmas>6) {
					System.out.println("Sigmas = " + sigmas);
					System.out.print("Probs: [");
					for (int k = 0; k < probs.length; k++) {
						System.out.print(probs[i] + ", ");
					}
					System.out.println("]");
				}
				assertTrue("Sigma was: "+ sigmas, sigmas<6);
			}
			//System.out.println();
		}
	}

	@Test
	public void testCompareMultinomialSampling() {
		int dirichletDim = 1000;
		Dirichlet distgen = new Dirichlet(dirichletDim);
		double [] probs = distgen.nextDistribution(); 
		walker.initTableNormalizedProbabilities(probs);
		int noSamples = 1000000;
		int [] samples = new int[noSamples];
		long tstart = System.currentTimeMillis();
		for (int i = 0; i < noSamples; i++) {
			samples[i] = walker.generateSample();
		}
		long tend = System.currentTimeMillis();
		int [] cnts = new int[probs.length];
		for (int i = 0; i < samples.length; i++) {
			cnts[samples[i]]++;
		}
		int [] multinomialSamples = new int[noSamples];
		long mstart = System.currentTimeMillis();
		for (int i = 0; i < noSamples; i++) {
			double U = ThreadLocalRandom.current().nextDouble();
			int theSample = -1;
			while (U > 0.0) {
				theSample++;
				U -= probs[theSample];
			} 
			multinomialSamples[i] = theSample;
		}	
		long mend = System.currentTimeMillis();
		int [] multiCnts = new int[probs.length];
		for (int i = 0; i < samples.length; i++) {
			multiCnts[samples[i]]++;
		}

		System.out.println("Counts: ");
		for (int i = 0; i < probs.length; i++) {			
			System.out.print(samples[i] + " (" + ((double)samples[i])/noSamples + "),");
			assertEquals(probs[i], ((double)cnts[i])/noSamples, epsilon);
			assertEquals(probs[i], ((double)multiCnts[i])/noSamples, epsilon);
			assertEquals(multiCnts[i],cnts[i]);
		}
		System.out.println();

		System.out.println("Alias Table sampling took: " + (tend-tstart) + " milliseconds");
		System.out.println("Multinomial sampling took: " + (mend-mstart) + " milliseconds");
		System.out.println("For " + noSamples + " samples and a " + dirichletDim + " dimensional Dirichlet...");
	}

}
