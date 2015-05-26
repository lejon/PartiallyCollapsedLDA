package cc.mallet.util;

import java.util.Arrays;
import java.util.Comparator;

public class IndexSorter {

	public IndexSorter() {
	}
	
	public static int [] getSortedIndices(double [] probabilities) {
		double [][] pairs = new double[probabilities.length][];
		int i = 0;
		for(double p : probabilities)
			pairs[i] = new double[] { p, i++ };
	
		pairs = sortPairs(pairs);
	
		int [] result = new int[pairs.length];
		for( int j = 0; j < pairs.length; j++) {
			result[j] = (int)pairs[j][1];
		}
		return result;
	}

	public static double [][] sortPairs(double [][] pairs) {
		// Sort the pairs on probability in descending order
		Arrays.sort(pairs, new Comparator<Object>() {
			public int compare(Object o1, Object o2) {
				if(o1 == o2)                 return 0;
				if(o1 == null && o2 == null) return 0;
				if(o1 == null)      		 return 1;
				if(o2 == null)				 return -1;
				return ((double[])o1)[0] < ((double[])o2)[0] ? 1 : -1;
			}
		});
		return pairs;
	}

	public static int [] getSortedIndices(int [] values) {
		int [][] pairs = new int[values.length][];
		int i = 0;
		for(int p : values)
			pairs[i] = new int[] { p, i++ };
	
		pairs = sortPairs(pairs);
	
		int [] result = new int[pairs.length];
		for( int j = 0; j < pairs.length; j++) {
			result[j] = (int)pairs[j][1];
		}
		return result;
	}

	public static int [][] sortPairs(int [][] pairs) {
		// Sort the pairs on value in descending order
		Arrays.sort(pairs, new Comparator<Object>() {
			public int compare(Object o1, Object o2) {
				if(o1 == o2)                 return 0;
				if(o1 == null && o2 == null) return 0;
				if(o1 == null)      		 return 1;
				if(o2 == null)				 return -1;
				return ((int[])o1)[0] < ((int[])o2)[0] ? 1 :
					((((int[])o1)[0] == ((int[])o2)[0]) ? 0 : -1);
			}
		});
		return pairs;
	}

}
