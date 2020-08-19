package cc.mallet.similarity;

import static org.junit.Assert.*;

import org.junit.Test;

public class CosineDistanceTest {

	@Test
	public void test() {
		double [] v1 = {3.0,8.0,7.0,5.0,2.0,9.0};
		double [] v2 = {10.0,8.0,6.0,6.0,4.0,5.0};
		
		CosineDistance cd = new CosineDistance();
		double result = cd.calculate(v1, v2);
		
		assertEquals(1-0.8638935626791596, result, 0.000000000000001);
	}

}
