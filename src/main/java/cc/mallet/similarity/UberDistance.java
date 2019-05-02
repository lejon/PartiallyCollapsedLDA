package cc.mallet.similarity;

public class UberDistance implements Distance {
	
	Distance [] measures = { 
			new CanberraDistance(),
			new ChebychevDistance(),
			new CosineDistance(),
			new EuclidianDistance(),
			new JaccardDistance(),
			new KLDistance(),
			new ManhattanDistance()
	};

	@Override
	public double calculate(double[] v1, double[] v2) {
        double sum = 0;
        for (int i = 0; i < measures.length; i++) {
            sum += measures[i].calculate(v1, v2);
        }
        return sum / measures.length;
	}
}
