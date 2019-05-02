package cc.mallet.similarity;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(value = Parameterized.class)
@Ignore("To be finalized")  public class SimilarityTest {

	@SuppressWarnings("rawtypes")
	Class distanceClass;
	Distance dist;
	double epsilon = 0.001;

	public SimilarityTest(@SuppressWarnings("rawtypes") Class testClass) {
		distanceClass = testClass;
	}

	@Parameters
	public static List<Object[]> data() {
		Object[][] impls = new Object[][] { 
				//{ BhattacharyyaDistance.class },
				//{ BM25Distance.class },
				{ CanberraDistance.class },
				{ ChebychevDistance.class },
				{ CosineDistance.class },
				{ EuclidianDistance.class },
				{ HellingerDistance.class },
				{ JensenShannonDistance.class },
				{ JaccardDistance.class },
				{ KLDistance.class },
				{ KolmogorovSmirnovDistance.class },
				{ ManhattanDistance.class },
				{ StatisticalDistance.class }
				};
		return Arrays.asList(impls);
	}

	@Before
	public void noSetup() throws InstantiationException, IllegalAccessException {
		dist = (Distance) distanceClass.newInstance();
	}

	@Test
	public void testSame() {
		double [] v1 = {0.2, 0.3, 0.5, 0.7};
		double calcDist = dist.calculate(v1, v1);
		assertTrue(dist.getClass().getSimpleName() + "Distance was: " + calcDist, calcDist < 0.00001);
	}

	@Test
	public void testNotSame() {
		double [] v1 = {0.2, 0.3, 0.5, 0.7};
		double [] v2 = {0.5, 0.8, 0.1, 0.7};
		double calcDistSame = dist.calculate(v1, v1);
		double calcDist = dist.calculate(v1, v2);
		assertTrue(dist.getClass().getSimpleName() + "Distance was: " + calcDist, calcDist > calcDistSame);
	}

	@Test
	public void testVsOne() {
		double [] v1 = {0.2, 0.3, 0.5, 0.7};
		double [] v2 = {0.0, 0.1, 0.0, 0.0};
		double calcDistSame = dist.calculate(v1, v1);
		double calcDist = dist.calculate(v1, v2);
		assertTrue(dist.getClass().getSimpleName() + "Distance was: " + calcDist, calcDist > calcDistSame);
	}

}
