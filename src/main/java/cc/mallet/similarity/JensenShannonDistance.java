package cc.mallet.similarity;

public class JensenShannonDistance implements Distance {

	KLDistance kldist = new KLDistance();

	@Override
	public double calculate(double[] v1, double[] v2) {
		double[] avg = new double[v1.length];
		for (int i = 0; i < v1.length; ++i) {
			avg[i] += (v1[i] + v2[i])/2.0;
		}
		return (kldist.calculate(v1, avg) + kldist.calculate(v2, avg))/2;
	}

}
