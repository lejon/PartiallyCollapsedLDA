package cc.mallet.utils;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import cc.mallet.util.IntArraySortUtils;
import cc.mallet.util.Randoms;

public class InsertionSortTest {

	@Test
	public void testInsertZero() {
		int [] zeroTypeTopicIdxs = new int[10];
		AtomicInteger size = new AtomicInteger();
		IntArraySortUtils.arrayIntSetAddSorted(zeroTypeTopicIdxs, 0, size);
		int [] expected = {0,0,0,0,0,0,0,0,0,0};
		assertArrayEquals(expected, zeroTypeTopicIdxs);
		assertEquals(1, size.get());
	}
	
	@Test
	public void testInsertEmpty() {
		int [] zeroTypeTopicIdxs = new int[10];
		AtomicInteger size = new AtomicInteger();
		IntArraySortUtils.arrayIntSetAddSorted(zeroTypeTopicIdxs, 15, size);
		int [] expected = {15,0,0,0,0,0,0,0,0,0};
		assertArrayEquals(expected, zeroTypeTopicIdxs);
		assertEquals(1, size.get());
	}
	
	@Test
	public void testInsertOneBigger() {
		int [] zeroTypeTopicIdxs = new int[10];
		AtomicInteger size = new AtomicInteger();
		IntArraySortUtils.arrayIntSetAddSorted(zeroTypeTopicIdxs, 15, size);
		int [] expected = {15,0,0,0,0,0,0,0,0,0};
		assertArrayEquals(expected, zeroTypeTopicIdxs);
		assertEquals(1, size.get());
		IntArraySortUtils.arrayIntSetAddSorted(zeroTypeTopicIdxs, 20, size);
		int [] expected2 = {15,20,0,0,0,0,0,0,0,0};
		assertArrayEquals(expected2, zeroTypeTopicIdxs);
		assertEquals(2, size.get());
	}
	
	@Test
	public void testInsertOneSmaller() {
		int [] zeroTypeTopicIdxs = new int[10];
		AtomicInteger size = new AtomicInteger();
		IntArraySortUtils.arrayIntSetAddSorted(zeroTypeTopicIdxs, 15, size);
		int [] expected = {15,0,0,0,0,0,0,0,0,0};
		assertArrayEquals(expected, zeroTypeTopicIdxs);
		assertEquals(1, size.get());
		IntArraySortUtils.arrayIntSetAddSorted(zeroTypeTopicIdxs, 10, size);
		int [] expected2 = {10,15,0,0,0,0,0,0,0,0};
		assertArrayEquals(expected2, zeroTypeTopicIdxs);
		assertEquals(2, size.get());
	}
	
	@Test
	public void testInsertInBetween() {
		int [] zeroTypeTopicIdxs = new int[10];
		AtomicInteger size = new AtomicInteger();
		IntArraySortUtils.arrayIntSetAddSorted(zeroTypeTopicIdxs, 15, size);
		int [] expected = {15,0,0,0,0,0,0,0,0,0};
		assertArrayEquals(expected, zeroTypeTopicIdxs);
		assertEquals(1, size.get());
		IntArraySortUtils.arrayIntSetAddSorted(zeroTypeTopicIdxs, 10, size);
		int [] expected2 = {10,15,0,0,0,0,0,0,0,0};
		assertArrayEquals(expected2, zeroTypeTopicIdxs);
		assertEquals(2, size.get());
		IntArraySortUtils.arrayIntSetAddSorted(zeroTypeTopicIdxs, 12, size);
		int [] expected3 = {10,12,15,0,0,0,0,0,0,0};
		assertArrayEquals(expected3, zeroTypeTopicIdxs);
		assertEquals(3, size.get());
	}
	
	@Test
	public void testInsertExistingFirst() {
		int [] zeroTypeTopicIdxs = new int[10];
		AtomicInteger size = new AtomicInteger();
		IntArraySortUtils.arrayIntSetAddSorted(zeroTypeTopicIdxs, 15, size);
		int [] expected = {15,0,0,0,0,0,0,0,0,0};
		assertArrayEquals(expected, zeroTypeTopicIdxs);
		assertEquals(1, size.get());
		IntArraySortUtils.arrayIntSetAddSorted(zeroTypeTopicIdxs, 10, size);
		int [] expected2 = {10,15,0,0,0,0,0,0,0,0};
		assertArrayEquals(expected2, zeroTypeTopicIdxs);
		assertEquals(2, size.get());
		IntArraySortUtils.arrayIntSetAddSorted(zeroTypeTopicIdxs, 12, size);
		int [] expected3 = {10,12,15,0,0,0,0,0,0,0};
		assertArrayEquals(expected3, zeroTypeTopicIdxs);
		assertEquals(3, size.get());
		IntArraySortUtils.arrayIntSetAddSorted(zeroTypeTopicIdxs, 10, size);
		assertArrayEquals(expected3, zeroTypeTopicIdxs);
		assertEquals(3, size.get());
	}

	
	@Test
	public void testInsertExistingMiddle() {
		int [] zeroTypeTopicIdxs = new int[10];
		AtomicInteger size = new AtomicInteger();
		IntArraySortUtils.arrayIntSetAddSorted(zeroTypeTopicIdxs, 15, size);
		int [] expected = {15,0,0,0,0,0,0,0,0,0};
		assertArrayEquals(expected, zeroTypeTopicIdxs);
		assertEquals(1, size.get());
		IntArraySortUtils.arrayIntSetAddSorted(zeroTypeTopicIdxs, 10, size);
		int [] expected2 = {10,15,0,0,0,0,0,0,0,0};
		assertArrayEquals(expected2, zeroTypeTopicIdxs);
		assertEquals(2, size.get());
		IntArraySortUtils.arrayIntSetAddSorted(zeroTypeTopicIdxs, 12, size);
		int [] expected3 = {10,12,15,0,0,0,0,0,0,0};
		assertArrayEquals(expected3, zeroTypeTopicIdxs);
		assertEquals(3, size.get());
		IntArraySortUtils.arrayIntSetAddSorted(zeroTypeTopicIdxs, 12, size);
		assertArrayEquals(expected3, zeroTypeTopicIdxs);
		assertEquals(3, size.get());
	}
	
	@Test
	public void testInsertExistingLast() {
		int [] zeroTypeTopicIdxs = new int[10];
		AtomicInteger size = new AtomicInteger();
		IntArraySortUtils.arrayIntSetAddSorted(zeroTypeTopicIdxs, 15, size);
		int [] expected = {15,0,0,0,0,0,0,0,0,0};
		assertArrayEquals(expected, zeroTypeTopicIdxs);
		assertEquals(1, size.get());
		IntArraySortUtils.arrayIntSetAddSorted(zeroTypeTopicIdxs, 10, size);
		int [] expected2 = {10,15,0,0,0,0,0,0,0,0};
		assertArrayEquals(expected2, zeroTypeTopicIdxs);
		assertEquals(2, size.get());
		IntArraySortUtils.arrayIntSetAddSorted(zeroTypeTopicIdxs, 12, size);
		int [] expected3 = {10,12,15,0,0,0,0,0,0,0};
		assertArrayEquals(expected3, zeroTypeTopicIdxs);
		assertEquals(3, size.get());
		IntArraySortUtils.arrayIntSetAddSorted(zeroTypeTopicIdxs, 15, size);
		assertArrayEquals(expected3, zeroTypeTopicIdxs);
		assertEquals(3, size.get());
	}
	
	@Test
	public void testInsertToFullAddLast() {
		int [] zeroTypeTopicIdxs = new int[10];
		AtomicInteger size = new AtomicInteger();
		IntArraySortUtils.arrayIntSetAddSorted(zeroTypeTopicIdxs, 15, size);
		int [] expected = {15,0,0,0,0,0,0,0,0,0};
		assertArrayEquals(expected, zeroTypeTopicIdxs);
		assertEquals(1, size.get());
		IntArraySortUtils.arrayIntSetAddSorted(zeroTypeTopicIdxs, 10, size);
		int [] expected2 = {10,15,0,0,0,0,0,0,0,0};
		assertArrayEquals(expected2, zeroTypeTopicIdxs);
		assertEquals(2, size.get());
		IntArraySortUtils.arrayIntSetAddSorted(zeroTypeTopicIdxs, 12, size);
		int [] expected3 = {10,12,15,0,0,0,0,0,0,0};
		assertArrayEquals(expected3, zeroTypeTopicIdxs);
		assertEquals(3, size.get());
		IntArraySortUtils.arrayIntSetAddSorted(zeroTypeTopicIdxs, 9, size);
		int [] expected4 = {9,10,12,15,0,0,0,0,0,0};
		assertArrayEquals(expected4, zeroTypeTopicIdxs);
		assertEquals(4, size.get());
		IntArraySortUtils.arrayIntSetAddSorted(zeroTypeTopicIdxs, 11, size);
		int [] expected5 = {9,10,11,12,15,0,0,0,0,0};
		assertArrayEquals(expected5, zeroTypeTopicIdxs);
		assertEquals(5, size.get());
		IntArraySortUtils.arrayIntSetAddSorted(zeroTypeTopicIdxs, 18, size);
		int [] expected6 = {9,10,11,12,15,18,0,0,0,0};
		assertArrayEquals(expected6, zeroTypeTopicIdxs);
		assertEquals(6, size.get());
		IntArraySortUtils.arrayIntSetAddSorted(zeroTypeTopicIdxs, 0, size);
		int [] expected7 = {0,9,10,11,12,15,18,0,0,0};
		assertArrayEquals(expected7, zeroTypeTopicIdxs);
		assertEquals(7, size.get());
		IntArraySortUtils.arrayIntSetAddSorted(zeroTypeTopicIdxs, -1, size);
		int [] expected8 = {-1,0,9,10,11,12,15,18,0,0};
		assertArrayEquals(expected8, zeroTypeTopicIdxs);
		assertEquals(8, size.get());
		IntArraySortUtils.arrayIntSetAddSorted(zeroTypeTopicIdxs, -1, size);
		assertArrayEquals(expected8, zeroTypeTopicIdxs);
		assertEquals(8, size.get());
		IntArraySortUtils.arrayIntSetAddSorted(zeroTypeTopicIdxs, 16, size);
		int [] expected9 = {-1,0,9,10,11,12,15,16,18,0};
		assertArrayEquals(expected9, zeroTypeTopicIdxs);
		assertEquals(9, size.get());
		IntArraySortUtils.arrayIntSetAddSorted(zeroTypeTopicIdxs, 20, size);
		int [] expected10 = {-1,0,9,10,11,12,15,16,18,20};
		assertArrayEquals(expected10, zeroTypeTopicIdxs);
		assertEquals(10, size.get());
	}
	
	@Test
	public void testInsertToFullNextToLast() {
		int [] zeroTypeTopicIdxs = new int[10];
		AtomicInteger size = new AtomicInteger();
		IntArraySortUtils.arrayIntSetAddSorted(zeroTypeTopicIdxs, 15, size);
		int [] expected = {15,0,0,0,0,0,0,0,0,0};
		assertArrayEquals(expected, zeroTypeTopicIdxs);
		assertEquals(1, size.get());
		IntArraySortUtils.arrayIntSetAddSorted(zeroTypeTopicIdxs, 10, size);
		int [] expected2 = {10,15,0,0,0,0,0,0,0,0};
		assertArrayEquals(expected2, zeroTypeTopicIdxs);
		assertEquals(2, size.get());
		IntArraySortUtils.arrayIntSetAddSorted(zeroTypeTopicIdxs, 12, size);
		int [] expected3 = {10,12,15,0,0,0,0,0,0,0};
		assertArrayEquals(expected3, zeroTypeTopicIdxs);
		assertEquals(3, size.get());
		IntArraySortUtils.arrayIntSetAddSorted(zeroTypeTopicIdxs, 9, size);
		int [] expected4 = {9,10,12,15,0,0,0,0,0,0};
		assertArrayEquals(expected4, zeroTypeTopicIdxs);
		assertEquals(4, size.get());
		IntArraySortUtils.arrayIntSetAddSorted(zeroTypeTopicIdxs, 11, size);
		int [] expected5 = {9,10,11,12,15,0,0,0,0,0};
		assertArrayEquals(expected5, zeroTypeTopicIdxs);
		assertEquals(5, size.get());
		IntArraySortUtils.arrayIntSetAddSorted(zeroTypeTopicIdxs, 18, size);
		int [] expected6 = {9,10,11,12,15,18,0,0,0,0};
		assertArrayEquals(expected6, zeroTypeTopicIdxs);
		assertEquals(6, size.get());
		IntArraySortUtils.arrayIntSetAddSorted(zeroTypeTopicIdxs, 0, size);
		int [] expected7 = {0,9,10,11,12,15,18,0,0,0};
		assertArrayEquals(expected7, zeroTypeTopicIdxs);
		assertEquals(7, size.get());
		IntArraySortUtils.arrayIntSetAddSorted(zeroTypeTopicIdxs, -1, size);
		int [] expected8 = {-1,0,9,10,11,12,15,18,0,0};
		assertArrayEquals(expected8, zeroTypeTopicIdxs);
		assertEquals(8, size.get());
		IntArraySortUtils.arrayIntSetAddSorted(zeroTypeTopicIdxs, -1, size);
		assertArrayEquals(expected8, zeroTypeTopicIdxs);
		assertEquals(8, size.get());
		IntArraySortUtils.arrayIntSetAddSorted(zeroTypeTopicIdxs, 16, size);
		int [] expected9 = {-1,0,9,10,11,12,15,16,18,0};
		assertArrayEquals(expected9, zeroTypeTopicIdxs);
		assertEquals(9, size.get());
		IntArraySortUtils.arrayIntSetAddSorted(zeroTypeTopicIdxs, 17, size);
		int [] expected10 = {-1,0,9,10,11,12,15,16,17,18};
		assertArrayEquals(expected10, zeroTypeTopicIdxs);
		assertEquals(10, size.get());
	}
	
	@Test
	public void testInsertToFullFirst() {
		int [] zeroTypeTopicIdxs = new int[10];
		AtomicInteger size = new AtomicInteger();
		IntArraySortUtils.arrayIntSetAddSorted(zeroTypeTopicIdxs, 15, size);
		int [] expected = {15,0,0,0,0,0,0,0,0,0};
		assertArrayEquals(expected, zeroTypeTopicIdxs);
		assertEquals(1, size.get());
		IntArraySortUtils.arrayIntSetAddSorted(zeroTypeTopicIdxs, 10, size);
		int [] expected2 = {10,15,0,0,0,0,0,0,0,0};
		assertArrayEquals(expected2, zeroTypeTopicIdxs);
		assertEquals(2, size.get());
		IntArraySortUtils.arrayIntSetAddSorted(zeroTypeTopicIdxs, 12, size);
		int [] expected3 = {10,12,15,0,0,0,0,0,0,0};
		assertArrayEquals(expected3, zeroTypeTopicIdxs);
		assertEquals(3, size.get());
		IntArraySortUtils.arrayIntSetAddSorted(zeroTypeTopicIdxs, 9, size);
		int [] expected4 = {9,10,12,15,0,0,0,0,0,0};
		assertArrayEquals(expected4, zeroTypeTopicIdxs);
		assertEquals(4, size.get());
		IntArraySortUtils.arrayIntSetAddSorted(zeroTypeTopicIdxs, 11, size);
		int [] expected5 = {9,10,11,12,15,0,0,0,0,0};
		assertArrayEquals(expected5, zeroTypeTopicIdxs);
		assertEquals(5, size.get());
		IntArraySortUtils.arrayIntSetAddSorted(zeroTypeTopicIdxs, 18, size);
		int [] expected6 = {9,10,11,12,15,18,0,0,0,0};
		assertArrayEquals(expected6, zeroTypeTopicIdxs);
		assertEquals(6, size.get());
		IntArraySortUtils.arrayIntSetAddSorted(zeroTypeTopicIdxs, 0, size);
		int [] expected7 = {0,9,10,11,12,15,18,0,0,0};
		assertArrayEquals(expected7, zeroTypeTopicIdxs);
		assertEquals(7, size.get());
		IntArraySortUtils.arrayIntSetAddSorted(zeroTypeTopicIdxs, -1, size);
		int [] expected8 = {-1,0,9,10,11,12,15,18,0,0};
		assertArrayEquals(expected8, zeroTypeTopicIdxs);
		assertEquals(8, size.get());
		IntArraySortUtils.arrayIntSetAddSorted(zeroTypeTopicIdxs, -1, size);
		assertArrayEquals(expected8, zeroTypeTopicIdxs);
		assertEquals(8, size.get());
		IntArraySortUtils.arrayIntSetAddSorted(zeroTypeTopicIdxs, 16, size);
		int [] expected9 = {-1,0,9,10,11,12,15,16,18,0};
		assertArrayEquals(expected9, zeroTypeTopicIdxs);
		assertEquals(9, size.get());
		IntArraySortUtils.arrayIntSetAddSorted(zeroTypeTopicIdxs, -7, size);
		int [] expected10 = {-7,-1,0,9,10,11,12,15,16,18};
		assertArrayEquals(expected10, zeroTypeTopicIdxs);
		assertEquals(10, size.get());
	}

	@Test
	public void testRemoveEmpty() {
		int [] zeroTypeTopicIdxs = new int[10];
		AtomicInteger size = new AtomicInteger();
		IntArraySortUtils.arrayIntSetRemoveSorted(zeroTypeTopicIdxs, 15, size);
		int [] expected = {0,0,0,0,0,0,0,0,0,0};
		assertArrayEquals(expected, zeroTypeTopicIdxs);
		assertEquals(0, size.get());
	}

	@Test
	public void testRemoveOne() {
		int [] zeroTypeTopicIdxs = new int[10];
		AtomicInteger size = new AtomicInteger();
		IntArraySortUtils.arrayIntSetAddSorted(zeroTypeTopicIdxs, 15, size);
		int [] expected = {15,0,0,0,0,0,0,0,0,0};
		assertArrayEquals(expected, zeroTypeTopicIdxs);
		assertEquals(1, size.get());
		IntArraySortUtils.arrayIntSetRemoveSorted(zeroTypeTopicIdxs, 15, size);
		int [] expected2 = {15,0,0,0,0,0,0,0,0,0};
		assertArrayEquals(expected2, zeroTypeTopicIdxs);
		assertEquals(0, size.get());
	}
	
	@Test
	public void testRemoveToEmpty() {
		int [] zeroTypeTopicIdxs = new int[10];
		AtomicInteger size = new AtomicInteger();
		IntArraySortUtils.arrayIntSetAddSorted(zeroTypeTopicIdxs, 15, size);
		int [] expected = {15,0,0,0,0,0,0,0,0,0};
		assertArrayEquals(expected, zeroTypeTopicIdxs);
		assertEquals(1, size.get());
		IntArraySortUtils.arrayIntSetAddSorted(zeroTypeTopicIdxs, 20, size);
		int [] expected2 = {15,20,0,0,0,0,0,0,0,0};
		assertArrayEquals(expected2, zeroTypeTopicIdxs);
		assertEquals(2, size.get());
		IntArraySortUtils.arrayIntSetRemoveSorted(zeroTypeTopicIdxs, 15, size);
		int [] expected3 = {20,20,0,0,0,0,0,0,0,0};
		assertArrayEquals(expected3, zeroTypeTopicIdxs);
		assertEquals(1, size.get());
		IntArraySortUtils.arrayIntSetRemoveSorted(zeroTypeTopicIdxs, 20, size);
		assertArrayEquals(expected3, zeroTypeTopicIdxs);
		assertEquals(0, size.get());
	}
	
	@Test
	public void testInsertAndRemoveToEmpty() {
		int [] zeroTypeTopicIdxs = new int[10];
		AtomicInteger size = new AtomicInteger();
		IntArraySortUtils.arrayIntSetAddSorted(zeroTypeTopicIdxs, 15, size);
		int [] expected = {15,0,0,0,0,0,0,0,0,0};
		assertArrayEquals(expected, zeroTypeTopicIdxs);
		assertEquals(1, size.get());
		IntArraySortUtils.arrayIntSetAddSorted(zeroTypeTopicIdxs, 20, size);
		int [] expected2 = {15,20,0,0,0,0,0,0,0,0};
		assertArrayEquals(expected2, zeroTypeTopicIdxs);
		assertEquals(2, size.get());
		IntArraySortUtils.arrayIntSetRemoveSorted(zeroTypeTopicIdxs, 15, size);
		int [] expected3 = {20,20,0,0,0,0,0,0,0,0};
		assertArrayEquals(expected3, zeroTypeTopicIdxs);
		assertEquals(1, size.get());
		IntArraySortUtils.arrayIntSetRemoveSorted(zeroTypeTopicIdxs, 20, size);
		assertArrayEquals(expected3, zeroTypeTopicIdxs);
		assertEquals(0, size.get());
		IntArraySortUtils.arrayIntSetAddSorted(zeroTypeTopicIdxs, 20, size);
		assertArrayEquals(expected3, zeroTypeTopicIdxs);
		assertEquals(1, size.get());
		IntArraySortUtils.arrayIntSetAddSorted(zeroTypeTopicIdxs, 15, size);
		assertArrayEquals(expected2, zeroTypeTopicIdxs);
		assertEquals(2, size.get());
	}
	
	@Test
	public void testInsertAndRemoveExample() {
		int [] zeroTypeTopicIdxs = new int[233];
		AtomicInteger size = new AtomicInteger();
		IntArraySortUtils.arrayIntSetRemoveSorted(zeroTypeTopicIdxs, 969, size);
		int [] expected = new int[233];
		assertArrayEquals(expected, zeroTypeTopicIdxs);
		assertEquals(0, size.get());
		IntArraySortUtils.arrayIntSetRemoveSorted(zeroTypeTopicIdxs, 614, size);
		assertArrayEquals(expected, zeroTypeTopicIdxs);
		assertEquals(0, size.get());
		IntArraySortUtils.arrayIntSetAddSorted(zeroTypeTopicIdxs, 545, size);
		int [] expected1 = new int[233];
		expected1[0] = 545;
		assertArrayEquals(expected1, zeroTypeTopicIdxs);
		assertEquals(1, size.get());
		IntArraySortUtils.arrayIntSetRemoveSorted(zeroTypeTopicIdxs, 96, size);
		assertArrayEquals(expected1, zeroTypeTopicIdxs);
		assertEquals(1, size.get());
	}
	
	@Test
	public void testRandomInsertAndRemoves() {
		Randoms random = new Randoms();
		int loops = 1000;
		for (int loop = 0; loop < loops; loop++) {
			Set<Integer> ss = new HashSet<Integer>();
			int inserts  = random.nextInt(2000);
			AtomicInteger size = new AtomicInteger();
			int arrLen = random.nextInt(200);
			arrLen = arrLen == 0 ? arrLen+1 : arrLen;
			int [] zeroTypeTopicIdxs = new int[arrLen];
			for (int insert = 0; insert < inserts; insert++) {
				int toInsertOrRemove = random.nextInt(arrLen);
				int prevSize = size.get();
				boolean didModify;
				if(random.nextDouble()>0.5) {
					//System.out.println("Insert: " + toInsertOrRemove);
					ss.add(toInsertOrRemove);
					didModify = IntArraySortUtils.arrayIntSetAddSorted(zeroTypeTopicIdxs, toInsertOrRemove, size);
					if(didModify)
						assertEquals(prevSize+1, size.get());
					else 
						assertEquals(prevSize, size.get());
					//System.out.println("Now: " + arrToStr(zeroTypeTopicIdxs, "ZZ"));
				} else {
					//System.out.println("Remove: " + toInsertOrRemove);
					ss.remove(toInsertOrRemove);
					didModify = IntArraySortUtils.arrayIntSetRemoveSorted(zeroTypeTopicIdxs, toInsertOrRemove, size);
					if(prevSize>0) {
						if(didModify)
							assertEquals(prevSize-1, size.get());
						else 
							assertEquals(prevSize, size.get());
					}
					else
						assertEquals(0, size.get());
					//System.out.println("Now: " + arrToStr(zeroTypeTopicIdxs, "ZZ"));
				}
				sameAs(ss, size, zeroTypeTopicIdxs);
			}
		}
	}

	void sameAs(Set<Integer> ss, AtomicInteger size, int[] zeroTypeTopicIdxs) {
		Integer [] ints = ss.toArray(new Integer[0]);
		Arrays.sort(ints);
		for (int i = 0; i < ints.length; i++) {
			if(((Integer)ints[i]).intValue() != zeroTypeTopicIdxs[i]) {
				System.out.println(arrToStr(ints, "SS"));
				System.out.println(arrToStr(zeroTypeTopicIdxs, "MY"));
			}

			assertEquals(((Integer)ints[i]).intValue(), zeroTypeTopicIdxs[i]);					
		}
	}
	
	String arrToStr(int [] arr, String title) {
		String res = "";
		res += title + "[" +  arr.length + "]:";
		for (int j = 0; j < arr.length; j++) {
			res += arr[j] + ", ";
		}
		res += "\n";
		return res;
	}

	String arrToStr(Integer [] arr, String title) {
		String res = "";
		res += title + "[" +  arr.length + "]:";
		for (int j = 0; j < arr.length; j++) {
			res += arr[j] + ", ";
		}
		res += "\n";
		return res;
	}

}
