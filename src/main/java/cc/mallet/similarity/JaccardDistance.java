package cc.mallet.similarity;

import java.util.HashSet;
import java.util.Set;

public class JaccardDistance implements Distance {

	@Override
	public double calculate(double[] v1, double[] v2) {
		Set<Integer> union = new HashSet<>();
		Set<Integer> intersection = new HashSet<>();
		Set<Integer> v1s = new HashSet<>();
		
		for (int i = 0; i < v1.length; i++) {
			if(v1[i]>0) {
				union.add(i);
				v1s.add(i);
			}
		}
		for (int i = 0; i < v2.length; i++) {
			if(v2[i]>0) {
				intersection.add(i);
			}
		}
		
		union.addAll(intersection);
		intersection.retainAll(v1s);
		
		if (intersection.size() > 0) {
			return 1-(intersection.size() / (double) union.size());
		} else {
			return Double.MAX_VALUE;
		}
	}
}
