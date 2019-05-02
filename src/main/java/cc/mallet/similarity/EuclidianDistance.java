package cc.mallet.similarity;


public class EuclidianDistance implements Distance {

	@Override
	public double calculate(double[] v1, double[] v2) {
        double sum = 0;
        for (int i = 0; i < v1.length; i++) {
            final double dp = v1[i] - v2[i];
            sum += dp * dp;
        }
        return Math.sqrt(sum);
	}
}
