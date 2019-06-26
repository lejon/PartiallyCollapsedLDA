package cc.mallet.similarity;

import cc.mallet.types.Instance;

public class InstanceDistanceWrapper implements InstanceDistance, Distance {
	
	Distance dist;
	Vectorizer vectorizer;
	
	public InstanceDistanceWrapper(Distance dist, Vectorizer vectorizer) {
		super();
		this.dist = dist;
		this.vectorizer = vectorizer;
	}

	@Override
	public double distance(Instance i1, Instance i2) {
		double [] v1 = vectorizer.instanceToVector(i1);
		double [] v2 = vectorizer.instanceToVector(i2);
		return dist.calculate(v1, v2);
	}

	@Override
	public double calculate(double[] v1, double[] v2) {
		return dist.calculate(v1, v2);
	}
}
