package cc.mallet.topics;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import cc.mallet.configuration.SimpleLDAConfiguration;

public class PoissonPolyaUrnHDPLDATest {
	
	@Test
	public void testUpdateNrActiveTopics() {
		PoissonPolyaUrnHDPLDA s = new PoissonPolyaUrnHDPLDA(new SimpleLDAConfiguration());
		List<Integer> at = new ArrayList<Integer>();
		at.add(1);
		at.add(2);
		at.add(3);
		int [] et = new int [] {1,2};
		int nt = s.updateNrActiveTopics(et, at);
		assertEquals(1, nt);
		assertEquals(1, at.size());
	}
	
	@Test
	public void testUpdateNrActiveTopicsNoChange() {
		PoissonPolyaUrnHDPLDA s = new PoissonPolyaUrnHDPLDA(new SimpleLDAConfiguration());
		List<Integer> at = new ArrayList<Integer>();
		at.add(1);
		at.add(2);
		at.add(3);
		int [] et = new int [] {};
		int nt = s.updateNrActiveTopics(et, at);
		assertEquals(3, nt);
		assertEquals(3, at.size());
	}
	
	@Test
	public void testCalcNewTopicsEmpty() {
		PoissonPolyaUrnHDPLDA s = new PoissonPolyaUrnHDPLDA(new SimpleLDAConfiguration());
		int [] nt = s.calcNewTopics(new ArrayList<Integer>(), new int [] {});
		assertEquals(0, nt.length);
	}
	
	@Test
	public void testCalcNewTopicsNoNew() {
		PoissonPolyaUrnHDPLDA s = new PoissonPolyaUrnHDPLDA(new SimpleLDAConfiguration());
		List<Integer> at = Arrays.asList(new Integer[]{1, 2, 3});
		int [] nt = s.calcNewTopics(at, new int [] {1,2,3});
		assertEquals(0, nt.length);
	}

	@Test
	public void testCalcNewTopics() {
		PoissonPolyaUrnHDPLDA s = new PoissonPolyaUrnHDPLDA(new SimpleLDAConfiguration());
		List<Integer> at = Arrays.asList(new Integer[]{1, 2, 3});
		int [] nt = s.calcNewTopics(at, new int [] {2,3,4});
		assertEquals(1, nt.length);
		assertEquals(4, nt[0]);
	}

	@Test
	public void testCalcNewTopicsDuplicateSampled() {
		PoissonPolyaUrnHDPLDA s = new PoissonPolyaUrnHDPLDA(new SimpleLDAConfiguration());
		List<Integer> at = Arrays.asList(new Integer[]{1, 2, 3});
		int [] nt = s.calcNewTopics(at, new int [] {2,3,4,4,4,4});
		assertEquals(1, nt.length);
		assertEquals(4, nt[0]);
	}
	
	@Test
	public void testCalcNewTopicsDisjoint() {
		PoissonPolyaUrnHDPLDA s = new PoissonPolyaUrnHDPLDA(new SimpleLDAConfiguration());
		List<Integer> at = Arrays.asList(new Integer[]{1, 2, 3});
		int [] nt = s.calcNewTopics(at, new int [] {4,4,4,4,5,6});
		assertEquals(3, nt.length);
		assertEquals(4, nt[0]);
		assertEquals(5, nt[1]);
		assertEquals(6, nt[2]);
	}
}
