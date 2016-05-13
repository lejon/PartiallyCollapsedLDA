package cc.mallet.similarity;

public class BhattacharyyaDistance implements Distance {

	@Override
	public double calculate(double[] v1, double[] v2) {
		double of = 1.0 / 4.0;
		
		double var1 = StatisticalDistance.variance(v1);
		double var2 = StatisticalDistance.variance(v2);
		double mean1 = StatisticalDistance.mean(v1);
		double mean2 = StatisticalDistance.mean(v2);
		
		double t1 = Math.log(of * (var1 / var2 + var2 / var2 + 2));
		double t2 = Math.pow(mean1-mean2,2) / (var1 + var2);
		
		return of * t1 + of * t2;
	}
}
