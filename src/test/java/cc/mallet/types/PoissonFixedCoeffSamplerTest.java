package cc.mallet.types;

import static org.junit.Assert.assertFalse;

import org.apache.commons.math3.distribution.PoissonDistribution;
import org.apache.commons.math3.stat.inference.ChiSquareTest;
import org.junit.Test;

/**
 * Test class for the PoissonFixedCoeffSampler.
 * 
 * We draw samples for different values of the counts (called betaAdd) from the PoissonFixedCoeffSampler
 * and draw the same number of samples from a standard Poisson with lambda = beta + betaAdd and compare
 * these distributions WHERE THEY HAVE SUBSTANTIAL MASS, i.e where the expected counts > 5. This is
 * manifested in the 'testlimit' values below (ul and ll for when betaAdd = 50 which moves the Poisson
 * far enough from 0 to generate 0's for the low counts)
 * 
 * @author Leif Jonsson
 *
 */
public class PoissonFixedCoeffSamplerTest {
	ChiSquareTest cs = new ChiSquareTest();

	@Test
	public void testChiSquareVsStdPoisson_Add50() {
		double beta = 0.01;
		int L = 100;
		PoissonFixedCoeffSampler fep = new PoissonFixedCoeffSampler(beta, L);

		int nrDraws = 1_000_000;
		int betaAdd = 50;
		int ll = 24;
		int ul = 85;
		long [] fepDraws = new long[ul-ll];
		long [] stdDraws = new long[ul-ll];
		PoissonDistribution stdPois = new PoissonDistribution(beta+betaAdd);
		for (int i = 0; i < nrDraws; i++) {
			{
				long nd = fep.nextPoisson(betaAdd);
				if(nd<ul && nd>=ll) fepDraws[(int)(nd-ll)]++;
			}
			
			{
				long stdnd = stdPois.sample();
				if(stdnd<ul && stdnd>=ll) stdDraws[(int)(stdnd-ll)]++;
			}

		}

		//System.out.println(Arrays.toString(fepDraws));
		//System.out.println(Arrays.toString(stdDraws));

		assertFalse(cs.chiSquareTestDataSetsComparison(fepDraws, stdDraws, 0.001));
	}

	@Test
	public void testChiSquareVsStdPoisson_Add10() {
		double beta = 0.01;
		int L = 100;
		PoissonFixedCoeffSampler fep = new PoissonFixedCoeffSampler(beta, L);

		int nrDraws = 1_000_000;
		int betaAdd = 10;
		int testlimit = 18;
		long [] fepDraws = new long[testlimit];
		long [] stdDraws = new long[testlimit];
		PoissonDistribution stdPois = new PoissonDistribution(beta+betaAdd);
		for (int i = 0; i < nrDraws; i++) {
			long nd = fep.nextPoisson(betaAdd);
			if(nd<testlimit) fepDraws[(int)nd]++;

			long stdnd = stdPois.sample();
			if(stdnd<testlimit) stdDraws[(int)stdnd]++;
		}

		//System.out.println(Arrays.toString(fepDraws));
		//System.out.println(Arrays.toString(stdDraws));

		assertFalse(cs.chiSquareTestDataSetsComparison(fepDraws, stdDraws, 0.001));
	}

	@Test
	public void testChiSquareVsStdPoisson_Add5() {
		double beta = 0.01;
		int L = 100;
		PoissonFixedCoeffSampler fep = new PoissonFixedCoeffSampler(beta, L);

		int nrDraws = 1_000_000;
		int betaAdd = 5;
		int testlimit = 18;
		long [] fepDraws = new long[testlimit];
		long [] stdDraws = new long[testlimit];
		PoissonDistribution stdPois = new PoissonDistribution(beta+betaAdd);
		for (int i = 0; i < nrDraws; i++) {
			long nd = fep.nextPoisson(betaAdd);
			if(nd<testlimit) fepDraws[(int)nd]++;

			long stdnd = stdPois.sample();
			if(stdnd<testlimit) stdDraws[(int)stdnd]++;
		}

		//System.out.println(Arrays.toString(fepDraws));
		//System.out.println(Arrays.toString(stdDraws));

		assertFalse(cs.chiSquareTestDataSetsComparison(fepDraws, stdDraws, 0.001));
	}

	@Test
	public void testChiSquareVsStdPoisson_Add2() {
		double beta = 0.01;
		int L = 100;
		PoissonFixedCoeffSampler fep = new PoissonFixedCoeffSampler(beta, L);

		int nrDraws = 1_000_000;
		int betaAdd = 2;
		int testlimit = 12;
		long [] fepDraws = new long[testlimit];
		long [] stdDraws = new long[testlimit];
		PoissonDistribution stdPois = new PoissonDistribution(beta+betaAdd);
		for (int i = 0; i < nrDraws; i++) {
			long nd = fep.nextPoisson(betaAdd);
			if(nd<testlimit) fepDraws[(int)nd]++;

			long stdnd = stdPois.sample();
			if(stdnd<testlimit) stdDraws[(int)stdnd]++;
		}

		//System.out.println(Arrays.toString(fepDraws));
		//System.out.println(Arrays.toString(stdDraws));

		assertFalse(cs.chiSquareTestDataSetsComparison(fepDraws, stdDraws, 0.001));
	}

	@Test
	public void testChiSquareVsStdPoisson_Add0() {
		double beta = 0.01;
		int L = 100;
		PoissonFixedCoeffSampler fep = new PoissonFixedCoeffSampler(beta, L);

		int nrDraws = 1_000_000;
		int betaAdd = 0;
		int testlimit = 3;
		long [] fepDraws = new long[testlimit];
		long [] stdDraws = new long[testlimit];
		PoissonDistribution stdPois = new PoissonDistribution(beta+betaAdd);
		for (int i = 0; i < nrDraws; i++) {
			long nd = fep.nextPoisson(betaAdd);
			if(nd<testlimit) fepDraws[(int)nd]++;

			long stdnd = stdPois.sample();
			if(stdnd<testlimit) stdDraws[(int)stdnd]++;
		}

		//System.out.println(Arrays.toString(fepDraws));
		//System.out.println(Arrays.toString(stdDraws));

		assertFalse(cs.chiSquareTestDataSetsComparison(fepDraws, stdDraws, 0.001));
	}
	
	// Compare timings between ordinary and fixed coeff sampler
	public void testChiSquareVsStdPoisson_Time() {
		double beta = 0.01;
		int L = 100;
		PoissonFixedCoeffSampler fep = new PoissonFixedCoeffSampler(beta, L);

		int nrDraws = 10_000_000;
		int betaAdd = 99;
		int testlimit = 3;
		long [] fepDraws = new long[testlimit];
		long [] stdDraws = new long[testlimit];
		long start = System.currentTimeMillis();
		for (int i = 0; i < nrDraws; i++) {
			long nd = fep.nextPoisson(betaAdd);
			if(nd<testlimit) fepDraws[(int)nd]++;
		}
		long tFep = System.currentTimeMillis();
		System.out.println("Time FEP = " + (tFep - start));

		PoissonDistribution stdPois = new PoissonDistribution(beta+betaAdd);
		for (int i = 0; i < nrDraws; i++) {
			long stdnd = stdPois.sample();
			if(stdnd<testlimit) stdDraws[(int)stdnd]++;
		}
		long tStd = System.currentTimeMillis();
		System.out.println("Time STD = " + (tStd - tFep));

		
		//System.out.println(Arrays.toString(fepDraws));
		//System.out.println(Arrays.toString(stdDraws));

		assertFalse(cs.chiSquareTestDataSetsComparison(fepDraws, stdDraws, 0.001));
	}

}
