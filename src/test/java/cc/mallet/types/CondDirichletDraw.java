package cc.mallet.types;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;

public class CondDirichletDraw {


	@Test
	public void test() {
		double phi0[] = {0.1, 0.2, 0.5, 0.2};
		double phi1[] = Arrays.copyOf(phi0, phi0.length);
		double alpha[] = {1.0, 1.0, 1.0, 1.0};
		int phi_index[] = {0, 2};	
		ConditionalDirichlet testDirichlet1 = new ConditionalDirichlet(1.0, alpha);
		phi1 = testDirichlet1.nextConditionalDistribution(phi0, phi_index);
	
		// Test that drawn phi are the same
		int[] phi_index_test0 = {1, 3};
		for (int i = 0; i < phi_index_test0.length; i++){
			double diff = phi0[phi_index_test0[i]] - phi1[phi_index_test0[i]];
			boolean test = Math.abs(diff) < 0.000000001;
			assertTrue(test);
		}
			
		// Test that drawn phi are not the same
		int[] phi_index_test1 = phi_index;
		for (int i = 0; i < phi_index_test1.length; i++){
			double diff = phi0[phi_index_test1[i]] - phi1[phi_index_test1[i]];
			boolean test = Math.abs(diff) > 0.000000001;
			assertTrue(test);
		}
		// Assert that the sums of the new draw are correct
		double sum_full_phi = 0;
		for (int i = 0; i < phi1.length; i++){
			sum_full_phi += phi1[i];
		}
		double diff = sum_full_phi - 1.0;
		System.out.println(diff);
		boolean test = Math.abs(diff) < 0.000000001;
		assertTrue(test);

		double sum_part_phi0 = 0;
		double sum_part_phi1 = 0;
		for (int i = 0; i < phi_index_test0.length; i++){
			sum_part_phi0 += phi0[phi_index_test0[i]];
			sum_part_phi1 += phi1[phi_index_test0[i]];
		}
		double diff1 = sum_part_phi0 - sum_part_phi1;
		boolean test1 = Math.abs(diff1) < 0.000000001;
		assertTrue(test1);
	}
}
