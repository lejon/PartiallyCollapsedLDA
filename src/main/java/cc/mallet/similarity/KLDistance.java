package cc.mallet.similarity;

public class KLDistance implements Distance {

	@Override
	public double calculate(double[] v1, double[] v2) {
		// Use symmetrisized KL divergence
		double u1 = cc.mallet.util.Maths.klDivergence(v1, v2);
		double u2 = cc.mallet.util.Maths.klDivergence(v2, v1);
		return (u1 + u2) / 2;
	}

}
