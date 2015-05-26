package cc.mallet.types;

import java.util.concurrent.ThreadLocalRandom;

public class SimpleMultinomial {

	double [] probs; 
	double sum = 0.0;
	public SimpleMultinomial(double [] probs) {
		this.probs = probs;
		for (int i = 0; i < probs.length; i++) {
			sum += probs[i];
		}
	}

	int [] draw(int draws) {
		int [] res = new int[probs.length];
		for (int draw = 0; draw < draws; draw++) {
			double u = ThreadLocalRandom.current().nextDouble();
			int category = 0;
			while( category < probs.length && u > 0) {
				u-=probs[category];
				category++;
			}
			res[category-1]++;
		}
		return res;
	}

}
