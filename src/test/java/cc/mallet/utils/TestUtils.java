package cc.mallet.utils;

import static org.junit.Assert.assertEquals;

public class TestUtils {

	public TestUtils() {
	}

	public static void assertEqualArrays(int[][] arr1, int[][] arr2) {
		assertEquals("Dimensions are not the same: " 
				+ arr2.length + "!=" + arr1.length, 
				arr2.length, arr1.length);
		for (int i = 0; i < arr2.length; i++) {
			for (int j = 0; j < arr2[i].length; j++) {
				assertEquals("Collapsed and Uncollapsed token counts are not the same: " 
						+ arr2[i][j] + "!=" + arr1[i][j], 
						arr2[i][j], arr1[i][j]);
			}
		}
	}
	
	public static void assertEqualArrays(double[][] arr1, double[][] arr2, double precision) {
		assertEquals("Dimensions are not the same: " 
				+ arr2.length + "!=" + arr1.length, 
				arr2.length, arr1.length);
		for (int i = 0; i < arr2.length; i++) {
			for (int j = 0; j < arr2[i].length; j++) {
				assertEquals("Arrays are not the same: " 
						+ arr2[i][j] + "!=" + arr1[i][j], 
						arr2[i][j], arr1[i][j], precision);
			}
		}
	}

}
