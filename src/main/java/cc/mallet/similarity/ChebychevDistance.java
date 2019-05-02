package cc.mallet.similarity;


public class ChebychevDistance implements Distance {

	@Override
	public double calculate(double[] v1, double[] v2) {
		double max = 0;
        for (int i = 0; i < v1.length; i++) {
            max = Math.max(max, Math.abs(v1[i] - v2[i]));
        }
        return max;
	}

}
