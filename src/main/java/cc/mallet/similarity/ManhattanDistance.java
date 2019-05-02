package cc.mallet.similarity;

public class ManhattanDistance implements Distance {

	@Override
	public double calculate(double[] v1, double[] v2) {
        double sum = 0;
        for (int i = 0; i < v1.length; i++) {
            sum += Math.abs(v1[i] - v2[i]);
        }
        return sum;
	}
}
