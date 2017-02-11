package cc.mallet.types;

import static org.junit.Assert.assertTrue;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.commons.math3.stat.inference.KolmogorovSmirnovTest;
import org.junit.Test;

public class SparseDirichletDrawTest {
	KolmogorovSmirnovTest ks = new KolmogorovSmirnovTest();
	Random rnd = new Random();

	@Test
	public void testNormal() {
		double alpha[] = {1.0, 1.0, 1.0, 1.0};	
		SparseDirichlet sparseDirichlet = new MarsagliaSparseDirichlet(alpha);
		Dirichlet dirichlet = new Dirichlet(alpha);

		int noLoops = 10_000;
		double [] d1s = new double[alpha.length];
		double [] d2s = new double[alpha.length];
		for (int loop = 0; loop < noLoops; loop++) {
			double [] sparseDraws = sparseDirichlet.nextDistribution();
			double [] draws = dirichlet.nextDistribution();
			for (int i = 0; i < draws.length; i++) {
				d1s[i] += sparseDraws[i];
				d2s[i] += draws[i];
			}
		}
		assertTrue(ks.kolmogorovSmirnovTest(d1s, d2s) > 0.00001);
//		System.out.println("Draws:");
//		for (int i = 0; i < d2s.length; i++) {
//			System.out.print(d1s[i]  + "<=>" + d2s[i] + ", ");
//		}
//		System.out.println();

	}
	
	@Test
	public void testCounts() {
		double alpha[] = {1.0, 1.0, 1.0, 1.0};	
		int [] counts  = {5, 2, 8, 20};
		double [] alphaPlain = new double[alpha.length];
		for (int i = 0; i < alphaPlain.length; i++) {
			alphaPlain[i] = alpha[i] + counts[i];
		}
		SparseDirichlet sparseDirichlet = new MarsagliaSparseDirichlet(alpha);
		Dirichlet dirichlet = new Dirichlet(alphaPlain);

		int noLoops = 10_000;
		double [] d1s = new double[alpha.length];
		double [] d2s = new double[alpha.length];
		for (int loop = 0; loop < noLoops; loop++) {
			double [] sparseDraws = sparseDirichlet.nextDistribution(counts);
			double [] draws = dirichlet.nextDistribution();
			for (int i = 0; i < draws.length; i++) {
				d1s[i] += sparseDraws[i];
				d2s[i] += draws[i];
			}
		}
		assertTrue(ks.kolmogorovSmirnovTest(d1s, d2s) > 0.00001);
//		System.out.println("Draws:");
//		for (int i = 0; i < d2s.length; i++) {
//			System.out.print(d1s[i]  + "<=>" + d2s[i] + ", ");
//		}
//		System.out.println();

	}
	
	@Test
	public void testSparseCounts() {
		int dirLen = 2000;
		Dirichlet dirichletAlphas = new Dirichlet(dirLen);
		double alpha[] = dirichletAlphas.nextDistribution();
		int [] counts  = new int[alpha.length];
		for (int i = 0; i < counts.length; i++) {
			double u = ThreadLocalRandom.current().nextDouble();
			if(u>0.90) {
				counts[i] = rnd.nextInt(10);
			} else {
				counts[i] = 0;
			}
		}
		
		double [] alphaPlain = new double[alpha.length];
		for (int i = 0; i < alphaPlain.length; i++) {
			alphaPlain[i] = alpha[i] + counts[i];
		}
		
		SparseDirichlet sparseDirichlet = new MarsagliaSparseDirichlet(alpha);
		ParallelDirichlet dirichlet = new ParallelDirichlet(alphaPlain);

		int noLoops = 20_000;
		double [] d1s = new double[alpha.length];
		//long ts = System.currentTimeMillis();
		for (int loop = 0; loop < noLoops; loop++) {
			double [] sparseDraws = sparseDirichlet.nextDistribution(counts);
			for (int i = 0; i < sparseDraws.length; i++) {
				d1s[i] += sparseDraws[i];
			}
		}
		//System.out.println("Time Sparse: " + (System.currentTimeMillis() - ts));
		double [] d2s = new double[alpha.length];
		//long tp = System.currentTimeMillis();
		for (int loop = 0; loop < noLoops; loop++) {
			double [] draws = dirichlet.nextDistribution();
			for (int i = 0; i < draws.length; i++) {
				d2s[i] += draws[i];
			}
		}
		//System.out.println("Time Plain: " + (System.currentTimeMillis() - tp));
		double pval = ks.kolmogorovSmirnovTest(d1s, d2s);

//		System.out.println("Counts:");
//		for (int i = 0; i < counts.length; i++) {
//			System.out.print(counts[i]  + ", ");
//		}
//		System.out.println();

		assertTrue("Pval is: " + pval, pval > 0.00001);
	}
}
