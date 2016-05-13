package cc.mallet.similarity;

public class StatisticalDistance implements Distance {

	@Override
	public double calculate(double[] v1, double[] v2) {
		return -(correlation(v1, v2)-1);
	}
	
	public static double mean(double [] vector) {
		double sum = 0.0;
		for (int i = 0; i < vector.length; i++) {
			sum +=vector[i];
		}
		return sum/vector.length;
	}
	
	public static double variance(double [] v)
    {
        double mean = mean(v);
        double var = 0;
        for(double a : v)
            var += (mean-a)*(mean-a);
        return var/v.length;
    }

	public static double sd(double [] v)
    {
        return Math.sqrt(variance(v));
    }
	
    public static double correlation(double [] v1, double [] v2) {
    	return covariance(v1, v2) / Math.sqrt(variance(v1) * variance(v2));
    }
    
	public static double covariance(double [] v1, double [] v2) {
		double result = 0.0;
		double m1 = mean(v1);
        double m2 = mean(v2);
        for (int i = 0; i < v1.length; i++) {
            double v1Deviation = v1[i] - m1;
            double v2Deviation = v2[i] - m2;
            result += (v1Deviation * v2Deviation - result) / (i + 1);
        }
        return result;
	}

}
