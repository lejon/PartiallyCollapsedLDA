package cc.mallet.configuration;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.math3.stat.inference.AlternativeHypothesis;
import org.apache.commons.math3.stat.inference.BinomialTest;
import org.junit.Test;

import com.google.common.collect.Sets;
import com.google.common.primitives.Ints;

public class SmokeTest {
	BinomialTest bt = new BinomialTest();

	/*int [][] topicMatrixGroundTruth = {
			{1,2,3,4,5},
			{6,7,8,9,10},
			{1,6,11,16,21},
			{2,7,12,17,22},
			{3,8,13,18,23},
			{11,12,13,14,15},
			{4,9,14,19,24},
			{5,10,15,20,25},
			{16,17,18,19,20},
			{21,22,23,24,25}
	};*/
	
	double [][] dm = {
			  {0.43, 0.00, 0.11, 0.00, 0.25, 0.11, 0.25, 0.00, 0.11, 0.00},
			  {0.00, 0.43, 0.00, 0.11, 0.11, 0.11, 0.25, 0.11, 0.11, 0.00},
			  {0.00, 0.25, 0.00, 0.11, 0.11, 0.11, 0.25, 0.11, 0.25, 0.00},
			  {0.11, 0.11, 0.67, 0.00, 0.11, 0.11, 0.00, 0.00, 0.11, 0.11},
			  {0.11, 0.00, 0.00, 0.43, 0.11, 0.25, 0.11, 0.00, 0.25, 0.00},
			  {0.25, 0.00, 0.00, 0.00, 0.43, 0.11, 0.11, 0.11, 0.25, 0.00},
			  {0.11, 0.11, 0.00, 0.00, 0.43, 0.11, 0.11, 0.11, 0.11, 0.11},
			  {0.00, 0.00, 0.11, 0.00, 0.11, 0.43, 0.43, 0.00, 0.11, 0.11},
			  {0.11, 0.00, 0.00, 0.11, 0.11, 0.11, 0.11, 0.25, 0.11, 0.25},
			  {0.11, 0.00, 0.00, 0.00, 0.11, 0.25, 0.11, 0.43, 0.25, 0.00},
		};

	@Test
	public void testCalibration() {
		int [][] topTopicWords = {
				{1,2,3,4,5},
				{5,7,8,9,11}, // Two faulty
				{11,12,13,14,15},
				{16,17,18,19,20},
				{21,22,23,24,25},
				{1,6,11,16,21},
				{2,7,12,17,22},
				{3,8,13,18,23},
				{4,9,14,19,24},
				{5,10,15,20,25}
		};

		boolean passFail = false;
		double alphaLevel = 0.01;
		for(int i = 0; i < topTopicWords.length;i++) {
			int noCorrectInClosestTopic = findNoCorrectInClosestTopic(topTopicWords[i]);
			int numberOfTrials = topTopicWords[i].length;
			System.out.println("Topic " + i + ": " + noCorrectInClosestTopic + "/" + numberOfTrials);
			passFail = bt.binomialTest(numberOfTrials, noCorrectInClosestTopic, 1/25.0,
					AlternativeHypothesis.TWO_SIDED,alphaLevel);
			System.out.println("We have a " + (passFail?"Pass":"Fail"));
			assertTrue(passFail);
		}
	}

	@Test
	public void testTotalCalibration() {
		int [][] topTopicWords = {{1,2,3,4,5},
				{5,7,8,9,11}, // Two faulty
				{11,12,13,14,15},
				{16,17,5,19,20}, // One faulty
				{21,1,23,24,25}, // One faulty
				{1,6,11,16,21},
				{2,7,12,17,22},
				{3,8,13,18,23},
				{1,1,1,1,1},
				//{4,9,14,19,24},
				{5,10,15,20,25}
		};

		//		int [][] topTopicWords = {
		//				{0,0,0,0,0},
		//				{1,0,0,0,0},
		//				{1,0,0,0,0},
		//				{1,0,0,0,0}, 
		//				{1,0,0,0,0}, 
		//				{1,0,0,0,0},
		//				{1,0,0,0,0},
		//				{1,0,0,0,0},
		//				{1,0,0,0,0},
		//				{1,0,0,0,0}
		//		};


		boolean passFail = false;
		double alphaLevel = 0.01;
		int totalCorrect = 0;
		for(int i = 0; i < topTopicWords.length;i++) {
			int noCorrectInClosestTopic = findNoCorrectInClosestTopic(topTopicWords[i]);
			int numberOfTrials = topTopicWords[i].length;
			System.out.println("Topic " + i + ": " + noCorrectInClosestTopic + "/" + numberOfTrials);
			System.out.println("We have a " + (passFail?"Pass":"Fail"));
			totalCorrect += noCorrectInClosestTopic;
		}

		int trials = topTopicWords.length*topTopicWords[0].length;
		System.out.println("\nTotal no. sucess:" + totalCorrect);
		System.out.println("Trials: " + trials);

		passFail = bt.binomialTest(trials, totalCorrect, 1/25.0,
				AlternativeHypothesis.TWO_SIDED,alphaLevel);

		System.out.println("FINAL VERDICT: " + (passFail?"Pass":"Fail"));
		assertTrue(passFail);
	}
	
	protected String vectorToString(int[] vector) {
		String res = "{";
		for (int j = 0; j < vector.length; j++) {
			res+= vector[j] + ","; 
		}
		res = res.substring(0, res.length()-1);
		res += "}";
		return res;
	}


	protected String matrixToString(int[][] topTopicWords) {
		String res = "{";
		for (int i = 0; i < topTopicWords.length; i++) {
			res += "\n\t{";
			for (int j = 0; j < topTopicWords[i].length; j++) {
				res+= String.format("%1$2s",topTopicWords[i][j]) + ","; 
			}
			res = res.substring(0, res.length()-1);
			res += "}";
		}
		res += "\n}";
		return res;
	}

	protected String matrixToString(double[][] topTopicWords) {
		String res = "{";
		for (int i = 0; i < topTopicWords.length; i++) {
			res += "\n\t " + i + " {";
			for (int j = 0; j < topTopicWords[i].length; j++) {
				res+= String.format("%1$2s", topTopicWords[i][j]) + ", "; 
			}
			res = res.substring(0, res.length()-1);
			res += "}";
		}
		res += "\n}";
		return res;
	}

	protected int findNoCorrectInRow(int[][] topTopicWords, int inRow, int [][] topicMatrix) {
		Set<Integer> intersection = new HashSet<>(Ints.asList(topTopicWords[inRow]));
		Set<Integer> set = new HashSet<>(Ints.asList(topicMatrix[inRow]));
		intersection.retainAll(set);
		return intersection.size();
	}

	protected int[][] sortTopicMatrix(int[][] topTopicWords,int[][] topicMatrix) {
		int[][] sortedMatrix = new int[topTopicWords.length][];
		double [][] dists = new double[topTopicWords.length][topicMatrix.length];
		// calculate the Jaccard distances between the rows in the true topic matrix
		// and the found 
		for (int i = 0; i < topTopicWords.length; i++) {
			for (int j = 0; j < topicMatrix.length; j++) {
				dists[i][j] = jaccardDistance(topTopicWords[i], topicMatrix[j]);
			}
		}

		System.out.println("Dists are:");
		System.out.println(matrixToString(dists));

		Integer [] distOrder = indexSortRows(dists);
		System.out.println("Dist order:" + vectorToString(distOrder) );

		/*double [][] sortedDists = new double[dists.length][];
		for (int i = 0; i < distOrder.length; i++) {
			sortedDists[i] = dists[distOrder[i]];
		}

		System.out.println("Sorted Dists are:");
		System.out.println(matrixToString(sortedDists));*/

		System.out.println("TW:");
		System.out.println(matrixToString(topTopicWords));
		System.out.println("TWG:");
		System.out.println(matrixToString(topicMatrix));

		Set<Integer> used = new HashSet<>();
		// Find the best matching rows
		for (int i = (distOrder.length-1); i >= 0; i--) {
				int fromRow = distOrder[i];
				int toRow = -1;
				while( toRow < 0 ) {
					toRow = maximum(dists[fromRow]);
					if(used.contains(toRow)) {
						dists[fromRow][toRow] = Double.NEGATIVE_INFINITY;
						toRow = -1;
					} else {
						used.add(toRow);
					}
				}
				System.out.println("Moving row: " + fromRow + " to " + toRow);
				sortedMatrix[toRow] = topTopicWords[fromRow];
				Arrays.sort(sortedMatrix[toRow]);
		}
		
		System.out.println("Sorted Matrix is:");
		System.out.println(matrixToString(sortedMatrix));
		return sortedMatrix;
	}

	protected String vectorToString(Integer[] vector) {
		String res = "{";
		for (int j = 0; j < vector.length; j++) {
			res+= vector[j] + ","; 
		}
		res = res.substring(0, res.length()-1);
		res += "}";
		return res;
	}

	protected int minimum(double[] ds) {
		int minIdx = -1;
		double minVal = Double.MAX_VALUE;
		for (int i = 0; i < ds.length; i++) {
			if(ds[i]<minVal) {minVal = ds[i];minIdx=i;}
		}
		return minIdx;
	}

	protected int maximum(double[] ds) {
		int maxIdx = -1;
		double maxVal = Double.NEGATIVE_INFINITY;
		for (int i = 0; i < ds.length; i++) {
			if(ds[i]>maxVal) {maxVal = ds[i];maxIdx=i;}
		}
		return maxIdx;
	}

	protected int maximum(int[] ds) {
		int maxIdx = -1;
		int maxVal = Integer.MIN_VALUE;
		for (int i = 0; i < ds.length; i++) {
			if(ds[i]>maxVal) {maxVal = ds[i];maxIdx=i;}
		}
		return maxIdx;
	}

	protected double jaccardDistance(int [] v1, int [] v2) {
		if(v1.length==v2.length && v1.length==0) return 1;

		Set<Integer> union = new HashSet<>(Ints.asList(v1));
		Set<Integer> intersection = new HashSet<>(Ints.asList(v1));
		Set<Integer> set = new HashSet<>(Ints.asList(v2));

		union.addAll(set);
		intersection.retainAll(set);

		return ((double) intersection.size()) / ((double) union.size()); 
	}

	Integer [] indexSort(final double[] dists) {
		final Integer[] idx = new Integer[dists.length];
		for (int i = 0; i < dists.length; i++) {
			idx[i] = i;
		}
		Arrays.sort(idx, new Comparator<Integer>() {
			@Override public int compare(final Integer o1, final Integer o2) {
				return Double.compare(dists[o1], dists[o2]);
			}
		});
		return idx;
	}

	Integer [] indexSortRows(final int [][] rows) {
		final Integer[] idx = new Integer[rows.length];
		for (int i = 0; i < rows.length; i++) {
			idx[i] = i;
		}
		Arrays.sort(idx, new Comparator<Integer>() {
			@Override public int compare(final Integer o1, final Integer o2) {
				int r1Max = maximum(rows[o1]);
				int r2Max = maximum(rows[o2]);
				int val1 = rows[o1][r1Max];
				int val2 = rows[o2][r2Max];
				return Integer.compare(val1, val2);
			}
		});
		return idx;
	}

	Integer [] indexSortRows(final double [][] rows) {
		final Integer[] idx = new Integer[rows.length];
		for (int i = 0; i < rows.length; i++) {
			idx[i] = i;
		}
		Arrays.sort(idx, new Comparator<Integer>() {
			@Override public int compare(final Integer o1, final Integer o2) {
				int r1Max = maximum(rows[o1]);
				int r2Max = maximum(rows[o2]);
				double val1 = rows[o1][r1Max];
				double val2 = rows[o2][r2Max];
				return Double.compare(val1, val2);
			}
		});
		return idx;
	}

	// Should be called dim-sum! :)
	static int [][] sum(int [][] matrix, int axis) {
		// Axis = 0 => sum columns
		// Axis = 1 => sum rows
		int [][] result;
		if( axis == 0) {
			result = new int[1][matrix[0].length];
			for (int j = 0; j < matrix[0].length; j++) {
				int rowsum = 0;
				for (int i = 0; i < matrix.length; i++) {
					rowsum += matrix[i][j];
				}
				result[0][j] = rowsum;
			}
		}   else if (axis == 1) {
			result = new int[matrix.length][1];
			for (int i = 0; i < matrix.length; i++) {
				int colsum = 0;
				for (int j = 0; j < matrix[0].length; j++) {
					colsum += matrix[i][j];
				}
				result[i][0] = colsum;
			}
		}   else {
			throw  new IllegalArgumentException("Axes other than 0,1 is unsupported");
		}
		return result;
	}

	private int findNoCorrectInClosestTopic(int[] topTopicWords) {
		int best = 0;
		Arrays.sort(topTopicWords);
		System.out.print("Topic: [");
		for (int i = 0; i < topTopicWords.length; i++) {
			System.out.print(topTopicWords[i] + ",");
		}
		System.out.println("]");
		for(int i = 0; i < topTopicWords.length; i++) {
			int correct = 0;
			int[] test = createCorrectTopicMatrixRow(topTopicWords, i);
			Set<Integer> testSet = Sets.newHashSet(Ints.asList(test));
			Set<Integer> givenSet = Sets.newHashSet(Ints.asList(topTopicWords));
			Set<Integer> matchedElements = Sets.intersection(testSet,givenSet);
			correct = matchedElements.size();
			//correct += (topTopicWords[j]==word ? 1 : 0);
			best = (correct > best ? correct : best);

			correct = 0;
			for (int j = 0; j < topTopicWords.length; j++) {
				int word = (i + (j*topTopicWords.length) + 1);
				//System.out.println("Word => " + word);
				//correct += (topTopicWords[j]==word ? 1 : 0);
				test[j] = word;
			}
			testSet = Sets.newHashSet(Ints.asList(test));
			givenSet = Sets.newHashSet(Ints.asList(topTopicWords));
			matchedElements = Sets.intersection(testSet,givenSet);
			correct = matchedElements.size();
			best = (correct > best ? correct : best);

			System.out.println("Best was:" + best);
		}
		return best;
	}

	int[] createCorrectTopicMatrixRow(int[] topTopicWords, int i) {
		int [] test = new int[topTopicWords.length];
		for (int j = 0; j < topTopicWords.length; j++) {
			int word = ((i*topTopicWords.length) + j + 1);
			//System.out.println("Word => " + word);
			test[j] = word;
		}
		return test;
	}
	
	int[][] createCorrectTopicMatrix(int noCols) {
		int noRows = noCols * 2;
		int [][] matrix = new int[noRows][noCols];
		int i = 0;
		for (; i < noCols; i++) {			
			for (int j = 0; j < noCols; j++) {
				int word = ((i*noCols) + j + 1);
				//System.out.println("Word => " + word);
				matrix[i][j] = word;
			}
		}
		for (; i < noRows; i++) {			
			for (int j = 0; j < noCols; j++) {
				int word = matrix[j][i-noCols];
				//System.out.println("Word => " + word);
				matrix[i][j] = word;
			}
		}
		
		return matrix;
	}

}	
