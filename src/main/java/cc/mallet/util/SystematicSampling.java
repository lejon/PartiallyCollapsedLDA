package cc.mallet.util;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

public class SystematicSampling {

	public SystematicSampling() {
	}
	
	public static int [] Tmpsample(int [] counts, int n) {
		if(n<1) throw new IllegalArgumentException("Step must be bigger than 1, given was: " + n);
		//<- function(size=c(4, 140, 14, 20, 13, 110, 29, 90, 34, 29, 230), n=100){
		double l = ThreadLocalRandom.current().nextDouble() * (double)n;
		System.out.println("l: "  + l);
		double cum_sum = 0.0;
		int j = 0;
		int [] res = new int[counts.length];
		for(int i = 0; i < counts.length; i++) {
			double cum_sum_tmp = cum_sum + (double)counts[i]; 
			System.out.println("sum_sum_tmp: "  + cum_sum_tmp);
			if((cum_sum < l && cum_sum_tmp >= l) || cum_sum_tmp >= l + n) {
				res[j] = i;
				j++;
			} 
			if(cum_sum > n){
				cum_sum = cum_sum_tmp % n;
			} else {
				cum_sum = cum_sum_tmp;
			}     
		}
		return Arrays.copyOf(res, j);
	}
		
	public static int [] origsample(int [] counts, int n) {
		if(n<1) throw new IllegalArgumentException("Step must be bigger than 1, given was: " + n);
		int l = (int) (ThreadLocalRandom.current().nextDouble() * (double)n);
		int countsum = l;
		int [] res = new int[counts.length];
		int i = 0;
		int j = 0;
		while( i < counts.length ) {
			if(counts[i]<countsum) {
				countsum -= counts[i];
				i++;
			} else {
				res[j++] = i;
				while(countsum<=counts[i]) {
					countsum += n;
				}
			}
		}
		return Arrays.copyOf(res, j);
	}
	
	public static int [] sample(int [] counts, int n) {
		if(n<1) throw new IllegalArgumentException("Step must be bigger than 1, given was: " + n);
		int l = (int) Math.ceil(ThreadLocalRandom.current().nextDouble() * (double)n);
		int countsum = l;
		int [] res = new int[counts.length];
		int i = 0;
		int j = 0;
		while( i < counts.length ) {
			if(counts[i]<1) throw new IllegalArgumentException("No bin count is allowed to be less than 1, count: " + counts[i]);
			if(counts[i]<countsum) {
				countsum -= counts[i];
				i++;
			} else {
				res[j++] = i;
				int times = ((counts[i] - countsum) / n) + 1 ;
				countsum += (times * n);
			}
		}
		return Arrays.copyOf(res, j);
	}

	public static int sum(int[] counts) {
		int sum = 0;
		for (int i = 0; i < counts.length; i++) {
			sum += counts[i];
		}
		return sum;
	}

	public static int max(int[] counts) {
		int max = Integer.MIN_VALUE;
		for (int i = 0; i < counts.length; i++) {
			max = counts[i] > max ? counts[i] : max; 
		}
		return max;
	}

}
