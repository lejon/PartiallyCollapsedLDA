package cc.mallet.similarity;

import org.apache.commons.math3.stat.inference.KolmogorovSmirnovTest;

public class KolmogorovSmirnovDistance implements Distance {

	KolmogorovSmirnovTest kstest = new KolmogorovSmirnovTest();
	
	@Override
	public double calculate(double[] v1, double[] v2) {
		return kstest.kolmogorovSmirnovStatistic(v1, v2);
	}

}
