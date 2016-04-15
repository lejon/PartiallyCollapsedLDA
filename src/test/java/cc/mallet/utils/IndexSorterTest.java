package cc.mallet.utils;

import static org.junit.Assert.*;

import org.junit.Test;

import cc.mallet.util.IndexSorter;

public class IndexSorterTest {

	@Test
	public void testSortInts() {
		int [] values = {4,2,7,3,8,0};
		int [] si = IndexSorter.getSortedIndices(values);
		int [] expected = {4,2,0,3,1,5};
		for (int i = 0; i < expected.length; i++) {
			assertEquals(expected[i], si[i]);
		}
		
		values = new int[0];
		si = IndexSorter.getSortedIndices(values);
		assertEquals(0, si.length);

		int [] values2 = {4,2,7,-3,3,8,0};
		si = IndexSorter.getSortedIndices(values2);
		int [] expected2 = {5,2,0,4,1,6,3};
		for (int i = 0; i < expected.length; i++) {
			assertEquals(expected2[i], si[i]);
		}		
	}
	
	@Test
	public void testSortDoubles() {
		double [] values = {4.0,2.0,7.0,3.0,8.0,0.0};
		int [] si = IndexSorter.getSortedIndices(values);
		int [] expected = {4,2,0,3,1,5};
		for (int i = 0; i < expected.length; i++) {
			assertEquals(expected[i], si[i]);
		}
		
		values = new double[0];
		si = IndexSorter.getSortedIndices(values);
		assertEquals(0, si.length);

		int [] values2 = {4,2,7,-3,3,8,0};
		si = IndexSorter.getSortedIndices(values2);
		int [] expected2 = {5,2,0,4,1,6,3};
		for (int i = 0; i < expected.length; i++) {
			assertEquals(expected2[i], si[i]);
		}		
	}

}
