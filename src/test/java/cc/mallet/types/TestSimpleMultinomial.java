package cc.mallet.types;

import static org.junit.Assert.*;

import org.apache.commons.math3.stat.inference.ChiSquareTest;
import org.junit.Test;

public class TestSimpleMultinomial {

	@Test
	public void test() {
		double[] dirichletParams = {1, 1, 1};
		ChiSquareTest cs = new ChiSquareTest();
		Dirichlet dir = new ParallelDirichlet(dirichletParams);
		for (int loop = 0; loop < 10; loop++) {
			double [] expected = dir.nextDistribution();
			SimpleMultinomial sm = new SimpleMultinomial(expected);
			int [] draw = sm.draw(10000);
			long [] observed = new long[expected.length];
			for (int i = 0; i < observed.length; i++) {
				observed[i] = draw[i];
			}
			assertFalse(cs.chiSquareTest(expected, observed, 0.001));
		}
	}
}
