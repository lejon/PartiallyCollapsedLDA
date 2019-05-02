package cc.mallet.similarity;

import org.apache.commons.math3.stat.inference.TTest;

public class TDistance implements Distance {

	TTest t = new TTest();
	
	@Override
	public double calculate(double[] v1, double[] v2) {
		return t.t(v1, v2);
	}

}
