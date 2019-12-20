package cc.mallet.types;

import static org.junit.Assert.assertFalse;

import org.apache.commons.math3.distribution.PoissonDistribution;
import org.apache.commons.math3.stat.inference.ChiSquareTest;
import org.junit.Test;

public class SamplerTest {
	
	ChiSquareTest cs = new ChiSquareTest();

	@Test
	public void testPoissonNormalApprox() {
		double beta = 0.01;
		int betaAdd = 30;
		double lambda = beta + betaAdd; 
		int nrDraws = 10_000;		
		
		int ll = 20;
		int ul = 40;
		long [] fepDraws = new long[ul-ll];
		long [] stdDraws = new long[ul-ll];
		PoissonDistribution stdPois = new PoissonDistribution(lambda);
		for (int i = 0; i < nrDraws; i++) {
			{
				long nd = PolyaUrnDirichlet.nextPoissonNormalApproximation(lambda);
				if(nd<ul && nd>=ll) fepDraws[(int)(nd-ll)]++;
			}
			
			{
				long stdnd = stdPois.sample();
				if(stdnd<ul && stdnd>=ll) stdDraws[(int)(stdnd-ll)]++;
			}

		}

		//System.out.println(Arrays.toString(fepDraws));
		//System.out.println(Arrays.toString(stdDraws));

		assertFalse(cs.chiSquareTestDataSetsComparison(fepDraws, stdDraws, 0.01));		
	}
	
	// TODO: Fix test
//	@Test
//	public void testBinomialNormalApprox() {
//		double p = 0.01;
//		int trials = 30; 
//		int nrDraws = 10_000;		
//		
//		int ll = 20;
//		int ul = 40;
//		long [] fepDraws = new long[ul-ll];
//		long [] stdDraws = new long[ul-ll];
//				
//		for (int i = 0; i < nrDraws; i++) {
//			{
//				double meanNormal = trials * p;
//				double variance = trials * p * (1-p); 
//				long nd = (int) Math.round(Math.sqrt(variance) * ThreadLocalRandom.current().nextGaussian() + meanNormal);
//				if(nd<ul && nd>=ll) fepDraws[(int)(nd-ll)]++;
//			}
//			
//			{
//				BinomialDistribution c_j_k = new BinomialDistribution(trials, p);
//				long stdnd = c_j_k.sample();
//				if(stdnd<ul && stdnd>=ll) stdDraws[(int)(stdnd-ll)]++;
//			}
//
//		}
//
//		//System.out.println(Arrays.toString(fepDraws));
//		//System.out.println(Arrays.toString(stdDraws));
//
//		assertFalse(cs.chiSquareTestDataSetsComparison(fepDraws, stdDraws, 0.01));
//	}

}
