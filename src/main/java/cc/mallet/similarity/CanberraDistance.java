package cc.mallet.similarity;

public class CanberraDistance implements Distance {

	@Override
	public double calculate(double[] v1, double[] v2) {
		double sum = 0;
		for (int i = 0; i < v1.length; i++) {
			final double num = Math.abs(v1[i] - v2[i]);
			final double denom = Math.abs(v1[i]) + Math.abs(v2[i]);
			sum += denom == 0.0 ? 0.0 : num / denom;
		}
		return sum;
	}
}
