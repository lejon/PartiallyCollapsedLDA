package cc.mallet.types;

import static org.junit.Assert.assertTrue;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.commons.math3.stat.inference.KolmogorovSmirnovTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(value = Parameterized.class)
public class SparseDirichletDrawParameterizedTest {
	final int dirichletDim = 4;
	KolmogorovSmirnovTest ks = new KolmogorovSmirnovTest();
	Random rnd = new Random();
	double [] alphaStd = {1.0,1.0,1.0,1.0};
	double [] alphaDraw;
	{
		int dirLen = 2000;
		Dirichlet dirichletAlphas = new Dirichlet(dirLen);
		alphaDraw = dirichletAlphas.nextDistribution();
	}
	
	@SuppressWarnings("rawtypes")
	Class sparseClass;
	SparseDirichlet sparseDirichlet; 
	
	public SparseDirichletDrawParameterizedTest(@SuppressWarnings("rawtypes") Class testClass) {
		sparseClass = testClass;
	}

	@Parameters
	public static List<Object[]> data() {
		Object[][] impls = new Object[][] { { MarsagliaSparseDirichlet.class }, { PolyaUrnDirichlet.class },
											{ MarsagliaSparseDirichlet.class }, { PolyaUrnDirichlet.class }
										  };
		return Arrays.asList(impls);
	}

	@SuppressWarnings("unchecked")
	public SparseDirichlet createSparseDirichlet(double [] alpha) {
		@SuppressWarnings("rawtypes")
		Class[] argumentTypes = new Class[1];
		argumentTypes[0] = double [].class; 

		try {
			return (SparseDirichlet) sparseClass.getDeclaredConstructor(argumentTypes).newInstance(alpha);
		} catch (InstantiationException | IllegalAccessException
				| InvocationTargetException
				| NoSuchMethodException | SecurityException e) {
			e.printStackTrace();
			throw new IllegalArgumentException(e);
		}
	}
	
	@Test
	public void testNormal() {
		Dirichlet dirichlet = new Dirichlet(alphaStd);
		sparseDirichlet = createSparseDirichlet(alphaStd);
		int noLoops = 10_000;
		double [] d1s = new double[alphaStd.length];
		double [] d2s = new double[alphaStd.length];
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
	
	//@Test
	public void testCounts() {
		int [] counts  = {5, 2, 8, 20};
		sparseDirichlet = createSparseDirichlet(alphaStd);
		double [] alphaPlain = new double[alphaStd.length];
		for (int i = 0; i < alphaPlain.length; i++) {
			alphaPlain[i] = alphaStd[i] + counts[i];
		}
		Dirichlet dirichlet = new Dirichlet(alphaPlain);

		int noLoops = 10_000;
		double [] d1s = new double[alphaStd.length];
		double [] d2s = new double[alphaStd.length];
		for (int loop = 0; loop < noLoops; loop++) {
			double [] sparseDraws = sparseDirichlet.nextDistribution(counts);
			double [] draws = dirichlet.nextDistribution();
			for (int i = 0; i < draws.length; i++) {
				d1s[i] += sparseDraws[i];
				d2s[i] += draws[i];
			}
		}
		assertTrue(ks.kolmogorovSmirnovTest(d1s, d2s) > 0.00001);
		//System.out.println("Draws:");
		//for (int i = 0; i < d2s.length; i++) {
		//	System.out.print(d1s[i]  + "<=>" + d2s[i] + ", ");
		//}
		//System.out.println();

	}
	
	//@Test
	public void testSparseCounts() {
		sparseDirichlet = createSparseDirichlet(alphaDraw);
		int [] counts  = new int[alphaDraw.length];
		for (int i = 0; i < counts.length; i++) {
			double u = ThreadLocalRandom.current().nextDouble();
			if(u>0.90) {
				counts[i] = rnd.nextInt(10);
			} else {
				counts[i] = 0;
			}
		}
				
		double [] alphaPlain = new double[alphaDraw.length];
		for (int i = 0; i < alphaPlain.length; i++) {
			alphaPlain[i] = alphaDraw[i] + counts[i];
		}
		
		ParallelDirichlet dirichlet = new ParallelDirichlet(alphaPlain);

		int noLoops = 20_000;
		double [] d1s = new double[alphaDraw.length];
		//long ts = System.currentTimeMillis();
		for (int loop = 0; loop < noLoops; loop++) {
			double [] sparseDraws = sparseDirichlet.nextDistribution(counts);
			for (int i = 0; i < sparseDraws.length; i++) {
				d1s[i] += sparseDraws[i];
			}
		}
		//System.out.println("Time Sparse: " + (System.currentTimeMillis() - ts));
		double [] d2s = new double[alphaDraw.length];
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
