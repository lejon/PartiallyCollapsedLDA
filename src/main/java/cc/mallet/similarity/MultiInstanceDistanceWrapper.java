package cc.mallet.similarity;

import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;

public class MultiInstanceDistanceWrapper extends InstanceDistanceWrapper implements TrainedDistance {

	InstanceList trainingset;
	public MultiInstanceDistanceWrapper(Distance dist, Vectorizer vectorizer, InstanceList trainingset) {
		super(dist, vectorizer);
		this.trainingset = trainingset;
	}

	@Override
	public void init(InstanceList trainingset) {
		this.trainingset = trainingset;
		if(dist instanceof TrainedDistance) {
			((TrainedDistance) dist).init(trainingset);
		}
	}

	@Override
	public double distanceToTrainingSample(double[] query, int sampleId) {
		double [] v2 = vectorizer.instanceToVector(trainingset.get(sampleId));
		return calculate(query, v2);
	}

	@Override
	public double[] distanceToAll(Instance testInstance) {
		double [] query = vectorizer.instanceToVector(testInstance);
		double [] results = new double [trainingset.size()];
		for (int i = 0; i < results.length; i++) {
			results[i] = distanceToTrainingSample(query, i);
		}
		return results;
	}

}
