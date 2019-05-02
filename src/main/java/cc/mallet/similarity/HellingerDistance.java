package cc.mallet.similarity;

import static java.lang.Math.*;

public class HellingerDistance implements Distance {

	@Override
	public double calculate(double[] v1, double[] v2) {
        double sum = 0;
        for (int i = 0; i < v1.length; i++) {
            final double dp = sqrt(v1[i]) - sqrt(v2[i]);
            sum += (dp * dp);
        }
        return sum;
	}
}
