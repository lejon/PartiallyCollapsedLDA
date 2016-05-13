package cc.mallet.similarity;

public class JaccardDistance implements Distance {

	@Override
	public double calculate(double[] v1, double[] v2) {
		double intersection = 0.0;
		double union = 0.0;
		for (int i = 0; i < v1.length; i++) {
			intersection += Math.min(v1[i], v2[i]);
			union += Math.max(v1[i], v2[i]);
		}
		if (intersection > 0.0D) {
			return 1-(intersection / union);
		} else {
			return 0.0;
		}
	}
}
