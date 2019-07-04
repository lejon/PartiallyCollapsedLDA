package cc.mallet.similarity;

public class KLDistance implements Distance {

	@Override
	public double calculate(double[] v1, double[] v2) {
		if(v1.length != v2.length) throw new IllegalArgumentException("Vectors have to be of equal length for KLDistance distance! v1.length=" 
	+ v1.length + " v2.legth=" + v2.length);
		return cc.mallet.util.Maths.klDivergence(v1, v2);
	}

}
