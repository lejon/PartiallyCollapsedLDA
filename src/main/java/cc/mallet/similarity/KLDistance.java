package cc.mallet.similarity;

public class KLDistance implements Distance {

	@Override
	public double calculate(double[] v1, double[] v2) {
		if(v1.length != v2.length) throw new ArrayIndexOutOfBoundsException("Vectors have to be of equal length for KLDistance distance!");
		return cc.mallet.util.Maths.klDivergence(v1, v2);
	}

}
