package cc.mallet.util;

import java.util.concurrent.atomic.AtomicInteger;

public class IntArraySortUtils {

	// Insertion sort
	public static boolean arrayIntSetAddSorted(int[] array, int value, AtomicInteger size) {
		int currSize = size.get();
		// Find the place to insert
		int i = 0;
		while (i < currSize && array[i]<value) i++;
		// topic is already inserted
		if(array[i]==value) {
			// Special case if we insert Zero into an empty set
			if(i==currSize) {
				size.incrementAndGet();
				return true;
			}
			return false;
		};
		// topic was not in set and it is the smallest value so far, insert and increase count
		if(i==currSize) { array[i]=value; size.incrementAndGet(); return true;}
		// Else insert topic at this pos and shift the others to the right
		int tmp1 = value;
		int tmp2;
		while(i<currSize) {
			tmp2 = array[i];
			array[i] = tmp1;
			tmp1 = tmp2;
			i++;
		}
		// Insert the last element
		array[i] = tmp1;
		size.incrementAndGet();
		return true;
	}

	// Insertion sort
	public static boolean arrayIntSetRemoveSorted(int[] array, int value, AtomicInteger size) {
		int currSize = size.get();
		// Find the place to remove
		int i = 0;
		while (i < currSize && array[i]!=value) i++;
		// Didn't find element
		if(i==currSize) {return false;}
		// topic was the last element, just remove it
		if(i!=(currSize-1)) { 
			// Else remove topic at this pos and shift the others to the left
			while(i<(currSize-1)) {
				array[i] = array[i+1]; 
				i++;
			}
		}
		size.decrementAndGet();
		return true;
	}
}
