package cc.mallet.topics;

import static org.junit.Assert.assertEquals;

import java.util.Random;

import org.junit.Test;

import cc.mallet.types.ParallelDirichlet;
import cc.mallet.util.SparsityTools;

public class SparsityToolsTest {

	@Test(expected = IllegalArgumentException.class)
	public void testFindIdxEmpty() {
		double [] cumsum = new double [0];
		try {
			// If we use the linear search, it will throw an
			// ArrayIndexOutOfBoundsException but I don't want 
			// to have that check in the actual method due to performance
			// reasons, so we put that check here instead...
			SparsityTools.findIdx(cumsum, 0.5, cumsum.length-1);			
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new IllegalArgumentException(e);
		}
	}

	@Test
	public void testFindIdxOneElem() {
		double [] cumsum = {0.3};
		assertEquals(0,SparsityTools.findIdx(cumsum, 0.5, cumsum.length-1));
	}

	@Test
	public void testFindIdxTwoElemGt() {
		double [] cumsum = {0.3,0.7};
		assertEquals(1,SparsityTools.findIdx(cumsum, 0.5, cumsum.length-1));
	}

	@Test
	public void testFindIdxTwoElemLt() {
		double [] cumsum = {0.3,0.7};
		assertEquals(0,SparsityTools.findIdx(cumsum, 0.2, cumsum.length-1));
	}

	@Test
	public void testFindIdxThreeElemLt() {
		double [] cumsum = {0.2,0.3,0.7};
		assertEquals(0,SparsityTools.findIdx(cumsum, 0.2, cumsum.length-1));
	}
	
	@Test
	public void testFindIdxThreeElemGt() {
		double [] cumsum = {0.2,0.3,0.7};
		assertEquals(2,SparsityTools.findIdx(cumsum, 0.6, cumsum.length-1));
	}

	@Test
	public void testFindIdxThreeElemMdl() {
		double [] cumsum = {0.2,0.3,0.7};
		assertEquals(1,SparsityTools.findIdx(cumsum, 0.25, cumsum.length-1));
	}

	@Test
	public void testFindIdx7ElemUpper() {
		double [] cumsum = {0.1,0.2,0.3,0.5,0.7,0.9,1.0};
		assertEquals(6,SparsityTools.findIdx(cumsum, 0.95, cumsum.length-1));
	}

	@Test
	public void testFindIdx7ElemLower() {
		double [] cumsum = {0.1,0.2,0.3,0.5,0.7,0.9,1.0};
		assertEquals(0,SparsityTools.findIdx(cumsum, 0.05, cumsum.length-1));
	}

	@Test
	public void testVarious() {
		Random rnd = new Random();
		int noTests = 10000;
		for (int i = 0; i < noTests; i++) {			
			int len = rnd.nextInt(5000);
			if(len<2) len = 2;
			ParallelDirichlet pd = new ParallelDirichlet(len);
			double [] dirDraw = pd.nextDistribution();
			double [] cumsum = new double[len];
			cumsum[0] = dirDraw[0];
			for (int j = 1; j < cumsum.length; j++) {
				cumsum[j] = cumsum[j-1] + dirDraw[j];
			}
			double u = rnd.nextDouble();
			int idx = 0;
			// Find index by linear search...
			for (idx = 0; idx < cumsum.length; idx++) {
				if(u<=cumsum[idx]) {
					break;
				}
			}
			int findIdx = SparsityTools.findIdx(cumsum, u, cumsum.length-1);
			if(idx != findIdx) {
				System.out.println(getRes(cumsum,u,idx));
			}
			assertEquals("Wrong index!",idx, findIdx);
		}
	}

	private String getRes(double[] cumsum, double u, int idx) {
		String res = "Idx=" + idx + "\n";
		res += ("U=" + u) + "\n";
		res += "Cumsum:\n\t[";
		for (int i = 0; i < cumsum.length; i++) {
			res += ("{" + i + "," + cumsum[i] + "},"); 
		}
		
		return res + "]";
	}
	
	@Test
	public void testInsertEmpty() {
		int [] nonZeroTopics = new int [20];
		int [] nonZeroTopicBackMapping = new int [20];
		int nonZeroCount = SparsityTools.insertSorted(15,nonZeroTopics,nonZeroTopicBackMapping,0);
		assertEquals(1,nonZeroCount);
		assertEquals(15,nonZeroTopics[0]);
		assertEquals(0,nonZeroTopicBackMapping[15]);		
	}
	
	@Test
	public void testFillUp() {
		int numTopics = 20;
		int [] nonZeroTopics = new int [numTopics];
		int [] nonZeroTopicBackMapping = new int [numTopics];
		int nonZeroCount = 0;
		int [] expected      = {0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19};
		int [] xpctZeroTopicBackMapping = {0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19};
		
		int [] toInsert = {3,8,18,15,11,12,17,13,19,16,10,5,1,2,6,9,7,4,14,0};
		assertEquals(numTopics,toInsert.length);
		
		for (int i = 0; i < toInsert.length; i++) {
			nonZeroCount = SparsityTools.insertSorted(toInsert[i],
					nonZeroTopics,nonZeroTopicBackMapping,nonZeroCount);			
		}
		
		assertEquals(20,nonZeroCount);
		for (int i = 0; i < nonZeroCount; i++) {
			assertEquals(expected[i],nonZeroTopics[i]);
			assertEquals(xpctZeroTopicBackMapping[i],nonZeroTopicBackMapping[i]);
		}
	}
	
	@Test
	public void testInsertLast() {
		int numTopics = 20;
		int [] nonZeroTopics = {0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,0};
		assertEquals(numTopics,nonZeroTopics.length);
		int [] nonZeroTopicBackMapping = {0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,0};
		assertEquals(numTopics,nonZeroTopicBackMapping.length);
		int nonZeroCount = 19;
		int newTopic = 19;
		nonZeroCount = SparsityTools.insertSorted(newTopic,
				nonZeroTopics,nonZeroTopicBackMapping,nonZeroCount);
		assertEquals(20,nonZeroCount);
		for (int i = 0; i < nonZeroCount; i++) {
			assertEquals(i,nonZeroTopics[i]);
			assertEquals(i,nonZeroTopicBackMapping[i]);
		}
	}
	
	@Test
	public void testInsertFirst() {
		int numTopics = 20;
		int [] nonZeroTopics = {1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,0};
		assertEquals(numTopics,nonZeroTopics.length);
		int [] nonZeroTopicBackMapping = {-1,0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18};
		assertEquals(numTopics,nonZeroTopicBackMapping.length);
		int nonZeroCount = 19;
		int newTopic = 0;
		nonZeroCount = SparsityTools.insertSorted(newTopic,
				nonZeroTopics,nonZeroTopicBackMapping,nonZeroCount);
		assertEquals(20,nonZeroCount);
		for (int i = 0; i < nonZeroCount; i++) {
			assertEquals(i,nonZeroTopics[i]);
			assertEquals(i,nonZeroTopicBackMapping[i]);
		}
	}
	
	@Test
	public void testInsertInBetween() {
		int numTopics = 20;
		int [] nonZeroTopics = {0,1,2,3,4,5,6,7,8,9,10,12,13,14,15,16,17,18,19,0};
		assertEquals(numTopics,nonZeroTopics.length);
		int [] nonZeroTopicBackMapping = {0,1,2,3,4,5,6,7,8,9,10,-1,11,12,13,14,15,16,17,18};
		assertEquals(numTopics,nonZeroTopicBackMapping.length);
		int nonZeroCount = 19;
		int newTopic = 11;
		nonZeroCount = SparsityTools.insertSorted(newTopic,
				nonZeroTopics,nonZeroTopicBackMapping,nonZeroCount);
		assertEquals(20,nonZeroCount);
		for (int i = 0; i < nonZeroCount; i++) {
			assertEquals(i,nonZeroTopics[i]);
			assertEquals(i,nonZeroTopicBackMapping[i]);
		}
	}
	
	@Test
	public void testInsertSecond() {
		int numTopics = 20;
		int [] nonZeroTopics = {0,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,0};
		assertEquals(numTopics,nonZeroTopics.length);
		int [] nonZeroTopicBackMapping = {0,-1,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18};
		assertEquals(numTopics,nonZeroTopicBackMapping.length);
		int nonZeroCount = 19;
		int newTopic = 1;
		nonZeroCount = SparsityTools.insertSorted(newTopic,
				nonZeroTopics,nonZeroTopicBackMapping,nonZeroCount);
		assertEquals(20,nonZeroCount);
		for (int i = 0; i < nonZeroCount; i++) {
			assertEquals(i,nonZeroTopics[i]);
			assertEquals(i,nonZeroTopicBackMapping[i]);
		}
	}

	@Test
	public void testInsertNextToLast() {
		int numTopics = 20;
		int [] nonZeroTopics = {0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,19,0};
		assertEquals(numTopics,nonZeroTopics.length);
		int [] nonZeroTopicBackMapping = {0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,-1,18};
		assertEquals(numTopics,nonZeroTopicBackMapping.length);
		int nonZeroCount = 19;
		int newTopic = 18;
		nonZeroCount = SparsityTools.insertSorted(newTopic,
				nonZeroTopics,nonZeroTopicBackMapping,nonZeroCount);
		assertEquals(20,nonZeroCount);
		for (int i = 0; i < nonZeroCount; i++) {
			System.out.print(nonZeroTopics[i] + ",");
			assertEquals(i,nonZeroTopics[i]);
			assertEquals(i,nonZeroTopicBackMapping[i]);
		}
	}
	
	@Test
	public void testInsertSeveral() {
		int numTopics = 20;
		int [] nonZeroTopics = {1,2,4,5,6,7,9,10,12,13,14,15,16,18,19,0,0,0,0,0};
		assertEquals(numTopics,nonZeroTopics.length);
		int [] nonZeroTopicBackMapping = {-1,1,2,-1,4,5,6,7,-1,9,10,-1,12,13,14,15,16,-1,18,19};
		assertEquals(numTopics,nonZeroTopicBackMapping.length);

		int [] toInsert = {0,3,17,8,11};
		int nonZeroCount = numTopics - toInsert.length;
		
		for (int i = 0; i < toInsert.length; i++) {
			nonZeroCount = SparsityTools.insertSorted(toInsert[i],
					nonZeroTopics,nonZeroTopicBackMapping,nonZeroCount);			
		}
		
		assertEquals(20,nonZeroCount);
		for (int i = 0; i < nonZeroCount; i++) {
			System.out.print(nonZeroTopics[i] + ",");
			assertEquals(i,nonZeroTopics[i]);
			assertEquals(i,nonZeroTopicBackMapping[i]);
		}
	}
	
	
	
	
	@Test(expected = IllegalArgumentException.class)
	public void testRemoveEmpty() {
		int [] nonZeroTopics = new int [20];
		int [] nonZeroTopicBackMapping = new int [20];
		SparsityTools.removeSorted(15,nonZeroTopics,nonZeroTopicBackMapping,0);
	}
	
	@Test
	public void testRemoveLast() {
		int numTopics = 20;
		int [] nonZeroTopics = {0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19};
		int [] expected      = {0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19};
		assertEquals(numTopics,nonZeroTopics.length);
		int [] nonZeroTopicBackMapping = {0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19};
		assertEquals(numTopics,nonZeroTopicBackMapping.length);
		int nonZeroCount = 20;
		int newTopic = 19;
		nonZeroCount = SparsityTools.removeSorted(newTopic,
				nonZeroTopics,nonZeroTopicBackMapping,nonZeroCount);
		assertEquals(19,nonZeroCount);
		for (int i = 0; i < nonZeroCount; i++) {
			assertEquals(expected[i],nonZeroTopics[i]);
			//assertEquals(i,nonZeroTopicBackMapping[i]);
		}
	}
	
	@Test
	public void testRemoveFirst() {
		int numTopics = 20;
		int [] nonZeroTopics = {0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19};
		int [] expected      = {1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,0};
		assertEquals(numTopics,nonZeroTopics.length);
		int [] nonZeroTopicBackMapping = {0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19};
		assertEquals(numTopics,nonZeroTopicBackMapping.length);
		int nonZeroCount = 20;
		int newTopic = 0;
		nonZeroCount = SparsityTools.removeSorted(newTopic,
				nonZeroTopics,nonZeroTopicBackMapping,nonZeroCount);
		assertEquals(19,nonZeroCount);
		for (int i = 0; i < nonZeroCount; i++) {
			assertEquals(expected[i],nonZeroTopics[i]);
			//assertEquals(i,nonZeroTopicBackMapping[i]);
		}
	}
	
	@Test
	public void testRemoveInBetween() {
		int numTopics = 20;
		int [] nonZeroTopics = {0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19};
		int [] expected      = {0,1,2,3,4,5,6,7,8,9,10,12,13,14,15,16,17,18,19,0};
		assertEquals(numTopics,nonZeroTopics.length);
		int [] nonZeroTopicBackMapping = {0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19};
		assertEquals(numTopics,nonZeroTopicBackMapping.length);
		int nonZeroCount = 20;
		int newTopic = 11;
		nonZeroCount = SparsityTools.removeSorted(newTopic,
				nonZeroTopics,nonZeroTopicBackMapping,nonZeroCount);
		assertEquals(19,nonZeroCount);
		for (int i = 0; i < nonZeroCount; i++) {
			assertEquals(expected[i],nonZeroTopics[i]);
			//assertEquals(i,nonZeroTopicBackMapping[i]);
		}
	}
	
	@Test
	public void testRemoveSecond() {
		int numTopics = 20;
		int [] nonZeroTopics = {0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19};
		int [] expected      = {0,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,0};
		assertEquals(numTopics,nonZeroTopics.length);
		int [] nonZeroTopicBackMapping = {0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19};
		assertEquals(numTopics,nonZeroTopicBackMapping.length);
		int nonZeroCount = 20;
		int newTopic = 1;
		nonZeroCount = SparsityTools.removeSorted(newTopic,
				nonZeroTopics,nonZeroTopicBackMapping,nonZeroCount);
		assertEquals(19,nonZeroCount);
		for (int i = 0; i < nonZeroCount; i++) {
			assertEquals(expected[i],nonZeroTopics[i]);
			//assertEquals(i,nonZeroTopicBackMapping[i]);
		}
	}

	@Test
	public void testRemoveNextToLast() {
		int numTopics = 20;
		int [] nonZeroTopics = {0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19};
		int [] expected      = {0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,19,0};
		assertEquals(numTopics,nonZeroTopics.length);
		int [] nonZeroTopicBackMapping = {0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19};
		assertEquals(numTopics,nonZeroTopicBackMapping.length);
		int nonZeroCount = 20;
		int newTopic = 18;
		nonZeroCount = SparsityTools.removeSorted(newTopic,
				nonZeroTopics,nonZeroTopicBackMapping,nonZeroCount);
		assertEquals(19,nonZeroCount);
		for (int i = 0; i < nonZeroCount; i++) {
			assertEquals(expected[i],nonZeroTopics[i]);
			//assertEquals(i,nonZeroTopicBackMapping[i]);
		}
	}
	
	@Test
	public void testRemoveSeveral() {
		int numTopics = 20;
		int [] nonZeroTopics = {0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19};
		int [] expected      = {1,2,4,5,6,7,9,10,12,13,14,15,16,18,19,0,0,0,0,0};
		assertEquals(numTopics,nonZeroTopics.length);
		int [] nonZeroTopicBackMapping = {0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19};
		assertEquals(numTopics,nonZeroTopicBackMapping.length);

		int [] toRemove = {0,3,17,8,11};
		int nonZeroCount = 20;
		
		for (int i = 0; i < toRemove.length; i++) {
			nonZeroCount = SparsityTools.removeSorted(toRemove[i],
					nonZeroTopics,nonZeroTopicBackMapping,nonZeroCount);			
		}
		
		assertEquals(numTopics - toRemove.length,nonZeroCount);
		for (int i = 0; i < nonZeroCount; i++) {
			assertEquals(expected[i],nonZeroTopics[i]);
			//assertEquals(i,nonZeroTopicBackMapping[i]);
		}
	}
	
}
