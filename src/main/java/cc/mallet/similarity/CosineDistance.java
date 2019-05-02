package cc.mallet.similarity;

public class CosineDistance implements Distance {

	@Override
	public double calculate(double[] v1, double[] v2) {
		double dotProduct = 0.0;
	    double normA = 0.0;
	    double normB = 0.0;
	    for (int i = 0; i < v1.length; i++) {
	        dotProduct += v1[i] * v2[i];
	        normA += Math.pow(v1[i], 2);
	        normB += Math.pow(v2[i], 2);
	    }   
	    return 1 + (-(dotProduct / (Math.sqrt(normA) * Math.sqrt(normB))));
	}

}
