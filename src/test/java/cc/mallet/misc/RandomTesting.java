package cc.mallet.misc;

import static org.junit.Assert.assertTrue;

import org.apache.commons.math3.distribution.BetaDistribution;
import org.apache.commons.math3.distribution.GammaDistribution;
import org.apache.commons.math3.stat.inference.KolmogorovSmirnovTest;
import org.junit.Test;

import cc.mallet.util.ParallelRandoms;
import cc.mallet.util.Randoms;

public class RandomTesting {
	
	@Test
	public void testBetaSampler() {
		int noDraws = 700;
		double [] samples = new double[noDraws]; 
		double [] alphas = {0.5001, 1.0001, 2.0001, 4.0001, 8.0001, 16.0001, 32.0001, 1024.0001};
		double [] betas = {0.5, 1.0, 2.0};

		int loops = 10;
		for (int l = 0; l < loops; l++) {
			for (double alpha : alphas) {	
				for (double beta : betas) {
					for (int i = 0; i < noDraws; i++) {
						samples[i] = ParallelRandoms.rbeta(alpha, beta);
					}
					BetaDistribution betaCdf = new BetaDistribution(alpha, beta);

					KolmogorovSmirnovTest ks = new KolmogorovSmirnovTest();
					double test2 = ks.kolmogorovSmirnovTest(betaCdf, samples);
					assertTrue(test2 > 0.00001);
				}
			}
		}
	}
	
	@Test
	public void testMarsagliaKS() {
		ParallelRandoms pr = new ParallelRandoms();
		Randoms malletRnd = new Randoms();
		int noDraws = 500_000;
		double [] samplesM = new double[noDraws]; 
		double [] samplesB = new double[noDraws]; 
		double [] alphas = {0.5001, 1.0001, 2.0001, 4.0001, 8.0001, 16.0001, 32.0001, 1024.0001};
		double [] betas = {0.5, 1.0, 2.0};
		
		for (double alpha : alphas) {	
			for (double beta : betas) {
				double lambda = 0;
				for (int i = 0; i < noDraws; i++) {
					samplesM[i] = pr.nextGamma(alpha, beta, lambda); // Marsaglia (2000)
					samplesB[i] = malletRnd.nextGamma(alpha, beta, lambda); // Best
				}
				KolmogorovSmirnovTest ks = new KolmogorovSmirnovTest();
				double test1 = ks.kolmogorovSmirnovTest(samplesB, samplesM);
				// System.out.println(test1);
				assertTrue(test1 > 0.00001);
			}
		}
	}
	
	
	@Test
	public void testMarsagliaVsTrue() {
		int noDraws = 700;
		double [] samples = new double[noDraws]; 
		double [] alphas = {0.5001, 1.0001, 2.0001, 4.0001, 8.0001, 16.0001, 32.0001, 1024.0001};
		double [] betas = {0.5, 1.0, 2.0};
		
		int loops = 10;
		for (int l = 0; l < loops; l++) {
			for (double alpha : alphas) {	
				for (double beta : betas) {
					double lambda = 0;
					for (int i = 0; i < noDraws; i++) {
						samples[i] = ParallelRandoms.rgamma(alpha, beta, lambda); // Marsaglia (2000)
					}
					GammaDistribution gammaCdf = new GammaDistribution(alpha, beta);

					KolmogorovSmirnovTest ks = new KolmogorovSmirnovTest();
					double test2 = ks.kolmogorovSmirnovTest(gammaCdf, samples);
					assertTrue(test2 > 0.00001);
				}
			}
		}

	}

}
