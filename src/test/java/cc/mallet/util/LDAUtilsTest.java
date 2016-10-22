package cc.mallet.util;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.Random;

import org.junit.Test;

import cc.mallet.types.InstanceList;
import cc.mallet.util.LDAUtils;
import cc.mallet.utils.TestUtils;

public class LDAUtilsTest {
	
	@Test
	public void testCalculateDocDensity() {
		double[] kdDensities = {0.2,0.8,0.63,0.26};
		int [] numTopics = {20, 40, 80, 100, 200, 400, 1000, 2000, 5000, 10000}; 
		int [] numDocuments = {500, 1000, 2000, 4000, 80000, 16000, 50000, 100_000, 500_000, 1_000_000, 5_000_000, 8_200_000, 10_000_000, 100_000_000};
		for (int i = 0; i < numTopics.length; i++) {
			for (int j = 0; j < numDocuments.length; j++) {				
				double calculateDocDensity = LDAUtils.calculateDocDensity(kdDensities, numTopics[i], numDocuments[j]);
				//System.out.println(numTopics[i] + " + " + numDocuments[j] + " => " + calculateDocDensity);
				assertTrue(calculateDocDensity>0 && calculateDocDensity < 1.0);
			}
		}
	}

	@Test
	public void testWriteDoubleMatrixRows() throws IOException {
		double [][] matrix = {{1.0,2.0,3.0},{4.0,5.0,6.0},{7.0,8.0,9.0}};
		int rows = matrix.length;
		int columns = matrix[0].length;
		
		File tmp = File.createTempFile("LDAUtilsTest", ".bin");
		
		int iteration = 1;

		int [] rowIndeces = {0,2};
		LDAUtils.writeBinaryDoubleMatrixRows(matrix, iteration, rows, columns, tmp.getAbsolutePath(), rowIndeces);
		
		
		String fn = String.format(tmp.getAbsolutePath() + "_" + rows + "_" + columns + "_%05d.BINARY", iteration);
		RandomAccessFile inputPhiFile = new RandomAccessFile(fn, "rw");
		FileChannel phiChannel = inputPhiFile.getChannel();
		final int bufferSize = 8*columns*rows;
		ByteBuffer buf = phiChannel.map(FileChannel.MapMode.READ_ONLY, 0, bufferSize);

		// Writes matrix to the buffer
		for (int to = 0; to < rowIndeces.length; to++) {
			for (int ty = 0; ty < columns; ty++) {
				double readDouble = buf.getDouble();
				assertEquals(matrix[rowIndeces[to]][ty],readDouble,0.0000001);
			}
		}
		inputPhiFile.close();
	}
	
	@Test
	public void testWriteIntMatrixRows() throws IOException {
		int [][] matrix = {{1,2,3},{4,5,6},{7,8,9}};
		int rows = matrix.length;
		int columns = matrix[0].length;
		
		File tmp = File.createTempFile("LDAUtilsTest", ".bin");
		
		int iteration = 1;

		int [] rowIndeces = {0,2};
		LDAUtils.writeBinaryIntMatrixRows(matrix, iteration, rows, columns, tmp.getAbsolutePath(), rowIndeces);
		
		
		String fn = String.format(tmp.getAbsolutePath() + "_" + rows + "_" + columns + "_%05d.BINARY", iteration);
		RandomAccessFile inputPhiFile = new RandomAccessFile(fn, "rw");
		FileChannel phiChannel = inputPhiFile.getChannel();
		final int bufferSize = 8*columns*rows;
		ByteBuffer buf = phiChannel.map(FileChannel.MapMode.READ_ONLY, 0, bufferSize);

		// Writes matrix to the buffer
		for (int to = 0; to < rowIndeces.length; to++) {
			for (int ty = 0; ty < columns; ty++) {
				int readInt = buf.getInt();
				assertEquals(matrix[rowIndeces[to]][ty],readInt);
			}
		}
		inputPhiFile.close();
	}
	
	@Test
	public void testWriteDobuleMatrixIndices() throws IOException {
		double [][] matrix = {{1.0,2.0,3.0},{4.0,5.0,6.0},{7.0,8.0,9.0}};
		int rows = matrix.length;
		int columns = matrix[0].length;
		
		File tmp = File.createTempFile("LDAUtilsTest", ".bin");
		
		int iteration = 1;

		int [][] indices = {{1,2},{0,1},{0,2}};
		String wr = LDAUtils.writeBinaryDoubleMatrixIndices(matrix, iteration, tmp.getAbsolutePath(), indices);
		
		File ttmp = new File(wr);
		assertEquals(indices.length*indices[0].length*8,ttmp.length());
		RandomAccessFile inputPhiFile = new RandomAccessFile(wr, "rw");
		FileChannel phiChannel = inputPhiFile.getChannel();
		final int bufferSize = 8*columns*rows;
		ByteBuffer buf = phiChannel.map(FileChannel.MapMode.READ_ONLY, 0, bufferSize);

		// Writes matrix to the buffer
		for (int to = 0; to < indices.length; to++) {
			for (int ty = 0; ty < indices[to].length; ty++) {
				double readDouble = buf.getDouble();
				assertEquals(matrix[to][indices[to][ty]],readDouble,0.0000001);
			}
		}
		inputPhiFile.close();
	}
	
	@Test
	public void testWriteDobuleMatrixIndicesExplicit() throws IOException {
		double [][] matrix = {{1.0,2.0,3.0},{4.0,5.0,6.0},{7.0,8.0,9.0}};
		int rows = matrix.length;
		int columns = matrix[0].length;
		
		File tmp = File.createTempFile("LDAUtilsTest", ".bin");
		
		int iteration = 1;

		int [][] indices = {{1,2},{0,1},{0,2}};
		String wr = LDAUtils.writeBinaryDoubleMatrixIndices(matrix, iteration, 
				indices.length, indices[0].length, tmp.getAbsolutePath(), indices);

		File ttmp = new File(wr);
		assertEquals(indices.length*indices[0].length*8,ttmp.length());

		RandomAccessFile inputPhiFile = new RandomAccessFile(wr, "rw");
		FileChannel phiChannel = inputPhiFile.getChannel();
		final int bufferSize = 8*columns*rows;
		ByteBuffer buf = phiChannel.map(FileChannel.MapMode.READ_ONLY, 0, bufferSize);

		// Writes matrix to the buffer
		for (int to = 0; to < indices.length; to++) {
			for (int ty = 0; ty < indices[to].length; ty++) {
				double readDouble = buf.getDouble();
				assertEquals(matrix[to][indices[to][ty]],readDouble,0.0000001);
			}
		}
		inputPhiFile.close();
	}
	
	@Test
	public void testWriteDobuleMatrixCols() throws IOException {
		double [][] matrix = {{1.0,2.0,3.0},{4.0,5.0,6.0},{7.0,8.0,9.0}};
		int rows = matrix.length;
		int columns = matrix[0].length;
		
		File tmp = File.createTempFile("LDAUtilsTest", ".bin");
		
		int iteration = 1;

		int [] colIndeces = {0,2};
		LDAUtils.writeBinaryDoubleMatrixCols(matrix, iteration, rows, columns, tmp.getAbsolutePath(), colIndeces);
		
		
		String fn = String.format(tmp.getAbsolutePath() + "_" + rows + "_" + columns + "_%05d.BINARY", iteration);
		RandomAccessFile inputPhiFile = new RandomAccessFile(fn, "rw");
		FileChannel phiChannel = inputPhiFile.getChannel();
		final int bufferSize = 8*columns*rows;
		ByteBuffer buf = phiChannel.map(FileChannel.MapMode.READ_ONLY, 0, bufferSize);

		// Writes matrix to the buffer
		for (int to = 0; to < rows; to++) {
			for (int ty = 0; ty < colIndeces.length; ty++) {
				double readDouble = buf.getDouble();
				assertEquals(matrix[to][colIndeces[ty]],readDouble,0.0000001);
			}
		}
		inputPhiFile.close();
	}
	
	@Test
	public void testWriteIntMatrixCols() throws IOException {
		int [][] matrix = {{1,2,3},{4,5,6},{7,8,9}};
		int rows = matrix.length;
		int columns = matrix[0].length;
		
		File tmp = File.createTempFile("LDAUtilsTest", ".bin");
		
		int iteration = 1;

		int [] colIndeces = {0,2};
		LDAUtils.writeBinaryIntMatrixCols(matrix, iteration, rows, columns, tmp.getAbsolutePath(), colIndeces);
		
		
		String fn = String.format(tmp.getAbsolutePath() + "_" + rows + "_" + columns + "_%05d.BINARY", iteration);
		RandomAccessFile inputPhiFile = new RandomAccessFile(fn, "rw");
		FileChannel phiChannel = inputPhiFile.getChannel();
		final int bufferSize = 8*columns*rows;
		ByteBuffer buf = phiChannel.map(FileChannel.MapMode.READ_ONLY, 0, bufferSize);

		// Writes matrix to the buffer
		for (int to = 0; to < rows; to++) {
			for (int ty = 0; ty < colIndeces.length; ty++) {
				int readInt = buf.getInt();
				assertEquals(matrix[to][colIndeces[ty]],readInt);
			}
		}
		inputPhiFile.close();
	}

	@Test(expected = IllegalArgumentException.class) 
	public void testInRangeBrokenEmptyRange() {
		int [] range = {};
		int idx = 0;
		LDAUtils.inRangeInterval(idx, range);
	}

	@Test(expected = IllegalArgumentException.class) 
	public void testInRangeBrokenNoPairRange() {
		int [] range = {1};
		int idx = 0;
		LDAUtils.inRangeInterval(idx, range);
	}
	
	@Test(expected = IllegalArgumentException.class) 
	public void testInRangeBrokenNoPairRange2() {
		int [] range = {1,2,3};
		int idx = 0;
		LDAUtils.inRangeInterval(idx, range);
	}

	@Test
	public void testInRangeBelow() {
		int [] range = {1,50,100,150};
		int idx = 0;
		assertTrue(!LDAUtils.inRangeInterval(idx, range));
	}

	@Test
	public void testInRangeAbove() {
		int [] range = {1,50,100,150};
		int idx = 200;
		assertTrue(!LDAUtils.inRangeInterval(idx, range));
	}

	@Test
	public void testInRangeBetween() {
		int [] range = {1,50,100,150};
		int idx = 75;
		assertTrue(!LDAUtils.inRangeInterval(idx, range));
	}

	@Test
	public void testInRangeInLow() {
		int [] range = {1,50,100,150};
		int idx = 1;
		assertTrue(LDAUtils.inRangeInterval(idx, range));
	}
	
	@Test
	public void testInRangeInLow2() {
		int [] range = {1,50,100,150};
		int idx = 100;
		assertTrue(LDAUtils.inRangeInterval(idx, range));
	}
	
	@Test
	public void testInRangeInHigh() {
		int [] range = {1,50,100,150};
		int idx = 50;
		assertTrue(LDAUtils.inRangeInterval(idx, range));
	}

	@Test
	public void testInRangeInHigh2() {
		int [] range = {1,50,100,150};
		int idx = 150;
		assertTrue(LDAUtils.inRangeInterval(idx, range));
	}

	String ds = "src/main/resources/datasets/SmallTexts.txt";
	Random rnd = new Random(4711);

	@Test
	public void testLoadInstances() throws FileNotFoundException {
		InstanceList is = LDAUtils.loadInstances(ds, null, 0);
		assertEquals(5,is.size());
	}

	@Test
	public void testLoadInstancesPrune() throws FileNotFoundException {
		InstanceList is = LDAUtils.loadInstancesPrune(ds, null, 2, true);
		assertEquals(7,is.getDataAlphabet().size());	
	}

	@Test
	public void testLoadInstancesKeep() throws FileNotFoundException {
		InstanceList is = LDAUtils.loadInstancesKeep(ds, null, 20, true);
		assertEquals(20,is.getDataAlphabet().size());	
	}

	@Test
	public void testReadWriteBinaryDoubleMatrix() throws IOException {
		final int ROWS = 10;
		final int COLS = 20;
		final int ITER = 22;
		double [][] dMatrix = new double[ROWS][COLS];
		for (int i = 0; i < dMatrix.length; i++) {
			for (int j = 0; j < dMatrix[i].length; j++) {
				dMatrix[i][j] = rnd.nextDouble();
			}
		}
		File tmp = File.createTempFile("testLDAUtils", "");
		LDAUtils.writeBinaryDoubleMatrix(dMatrix, ITER, tmp.getAbsolutePath());
		
		String readFn = tmp.getAbsolutePath() + "_" + ROWS + "_" + COLS + "_" + "000" + ITER + ".BINARY";
		
		double [][] readMatrix = LDAUtils.readBinaryDoubleMatrix(ROWS, COLS, readFn);
		TestUtils.assertEqualArrays(dMatrix,readMatrix, 0.00000000001);
	}
	
	@Test
	public void testReadWriteBinaryIntMatrix() throws IOException {
		final int ROWS = 10;
		final int COLS = 20;
		final int ITER = 22;
		int [][] dMatrix = new int[ROWS][COLS];
		for (int i = 0; i < dMatrix.length; i++) {
			for (int j = 0; j < dMatrix[i].length; j++) {
				dMatrix[i][j] = rnd.nextInt();
			}
		}
		File tmp = File.createTempFile("testLDAUtils", "");
		LDAUtils.writeBinaryIntMatrix(dMatrix, ITER, tmp.getAbsolutePath());
		
		String readFn = tmp.getAbsolutePath() + "_" + ROWS + "_" + COLS + "_" + "000" + ITER + ".BINARY";
		
		int [][] readMatrix = LDAUtils.readBinaryIntMatrix(ROWS, COLS, readFn);
		TestUtils.assertEqualArrays(dMatrix,readMatrix);
	}
	
	@Test
	public void testReadWriteBinaryDoubleMatrixRows() throws IOException {
		final int ROWS = 10;
		final int COLS = 20;
		final int ITER = 22;
		double [][] dMatrix = new double[ROWS][COLS];
		for (int i = 0; i < dMatrix.length; i++) {
			for (int j = 0; j < dMatrix[i].length; j++) {
				dMatrix[i][j] = rnd.nextDouble();
			}
		}
		File tmp = File.createTempFile("testLDAUtils", "");
		int [] rowIndices = {2,4,6,8};
		LDAUtils.writeBinaryDoubleMatrixRows(dMatrix, ITER, rowIndices.length, COLS, tmp.getAbsolutePath(), rowIndices);
		
		String readFn = tmp.getAbsolutePath() + "_" + rowIndices.length + "_" + COLS + "_" + "000" + ITER + ".BINARY";
		
		double [][] readMatrix = LDAUtils.readBinaryDoubleMatrix(rowIndices.length, COLS, readFn);
		
		double [][] cmpMatrix = new double[rowIndices.length][COLS];
		for (int i = 0; i < cmpMatrix.length; i++) {
			cmpMatrix[i] = dMatrix[rowIndices[i]];
		}
		TestUtils.assertEqualArrays(cmpMatrix,readMatrix, 0.00000000001);
	}
	
	@Test
	public void testReadWriteBinaryDoubleMatrixCols() throws IOException {
		final int ROWS = 10;
		final int COLS = 20;
		final int ITER = 22;
		double [][] dMatrix = new double[ROWS][COLS];
		for (int i = 0; i < dMatrix.length; i++) {
			for (int j = 0; j < dMatrix[i].length; j++) {
				dMatrix[i][j] = rnd.nextDouble();
			}
		}
		File tmp = File.createTempFile("testLDAUtils", "");
		int [] colIndices = {2,4,6,8};
		LDAUtils.writeBinaryDoubleMatrixCols(dMatrix, ITER, ROWS, colIndices.length, tmp.getAbsolutePath(), colIndices);
		
		String readFn = tmp.getAbsolutePath() + "_" + ROWS + "_" + colIndices.length + "_" + "000" + ITER + ".BINARY";
		
		double [][] readMatrix = LDAUtils.readBinaryDoubleMatrix(ROWS, colIndices.length, readFn);
		
		double [][] cmpMatrix = new double[ROWS][colIndices.length];
		for (int i = 0; i < cmpMatrix.length; i++) {
			for (int j = 0; j < cmpMatrix[i].length; j++) {				
				cmpMatrix[i][j] = dMatrix[i][colIndices[j]];
			}
		}
		TestUtils.assertEqualArrays(cmpMatrix,readMatrix, 0.00000000001);
	}
	
	@Test
	public void testReadWriteBinaryIntMatrixCols() throws IOException {
		final int ROWS = 10;
		final int COLS = 20;
		final int ITER = 22;
		int [][] dMatrix = new int[ROWS][COLS];
		for (int i = 0; i < dMatrix.length; i++) {
			for (int j = 0; j < dMatrix[i].length; j++) {
				dMatrix[i][j] = rnd.nextInt();
			}
		}
		File tmp = File.createTempFile("testLDAUtils", "");
		int [] colIndices = {2,4,6,8};
		LDAUtils.writeBinaryIntMatrixCols(dMatrix, ITER, ROWS, colIndices.length, tmp.getAbsolutePath(), colIndices);
		
		String readFn = tmp.getAbsolutePath() + "_" + ROWS + "_" + colIndices.length + "_" + "000" + ITER + ".BINARY";
		
		int [][] readMatrix = LDAUtils.readBinaryIntMatrix(ROWS, colIndices.length, readFn);
		
		int [][] cmpMatrix = new int[ROWS][colIndices.length];
		for (int i = 0; i < cmpMatrix.length; i++) {
			for (int j = 0; j < cmpMatrix[i].length; j++) {				
				cmpMatrix[i][j] = dMatrix[i][colIndices[j]];
			}
		}
		TestUtils.assertEqualArrays(cmpMatrix,readMatrix);
	}
	
	@Test
	public void testReadWriteBinaryIntMatrixRows() throws IOException {
		final int ROWS = 10;
		final int COLS = 20;
		final int ITER = 22;
		int [][] dMatrix = new int[ROWS][COLS];
		for (int i = 0; i < dMatrix.length; i++) {
			for (int j = 0; j < dMatrix[i].length; j++) {
				dMatrix[i][j] = rnd.nextInt();
			}
		}
		File tmp = File.createTempFile("testLDAUtils", "");
		int [] rowIndices = {2,4,6,8};
		LDAUtils.writeBinaryIntMatrixRows(dMatrix, ITER, rowIndices.length, COLS, tmp.getAbsolutePath(), rowIndices);
		
		String readFn = tmp.getAbsolutePath() + "_" + rowIndices.length + "_" + COLS + "_" + "000" + ITER + ".BINARY";
		
		int [][] readMatrix = LDAUtils.readBinaryIntMatrix(rowIndices.length, COLS, readFn);
		
		int [][] cmpMatrix = new int[rowIndices.length][COLS];
		for (int i = 0; i < cmpMatrix.length; i++) {
			cmpMatrix[i] = dMatrix[rowIndices[i]];
		}
		TestUtils.assertEqualArrays(cmpMatrix,readMatrix);
	}
	
	@Test
	public void testReadWriteASCIIIntMatrix() throws FileNotFoundException, IOException {
		final int ROWS = 10;
		final int COLS = 20;
		final String SEP = ";";
		int [][] dMatrix = new int[ROWS][COLS];
		for (int i = 0; i < dMatrix.length; i++) {
			for (int j = 0; j < dMatrix[i].length; j++) {
				dMatrix[i][j] = rnd.nextInt();
			}
		}
		File tmp = File.createTempFile("testLDAUtils", "");
		LDAUtils.writeASCIIIntMatrix(dMatrix, tmp.getAbsolutePath(), SEP);
		
		int [][] readMatrix = LDAUtils.readASCIIIntMatrix(tmp.getAbsolutePath(),SEP);
		TestUtils.assertEqualArrays(dMatrix,readMatrix);
	}

	@Test
	public void testReadWriteASCIIDoubleMatrix() throws FileNotFoundException, IOException {
		final int ROWS = 10;
		final int COLS = 20;
		final String SEP = ";";
		double [][] dMatrix = new double[ROWS][COLS];
		for (int i = 0; i < dMatrix.length; i++) {
			for (int j = 0; j < dMatrix[i].length; j++) {
				dMatrix[i][j] = rnd.nextDouble();
			}
		}
		File tmp = File.createTempFile("testLDAUtils", "");
		LDAUtils.writeASCIIDoubleMatrix(dMatrix, tmp.getAbsolutePath(), SEP);
		
		double [][] readMatrix = LDAUtils.readASCIIDoubleMatrix(tmp.getAbsolutePath(),SEP);
		TestUtils.assertEqualArrays(dMatrix,readMatrix, 0.0001);
		for (int i = 0; i < readMatrix.length; i++) {
			System.out.println("[" + i + "]: " + Arrays.toString(readMatrix[i]));
		}
	}
	
	@Test
	public void testCalcProbWord() {
		double beta = 0.1;
		int [][] typeTopicCounts = new int [][] {
				{3,2,5},
				{1,5,2},
				{0,0,0}};
				
		int nrWords = typeTopicCounts.length;
		double totTokens = 3+2+5+1+5+2+0+0+0 + (nrWords*beta);
		double probT1 = ((3+2+5)+beta) / (double) totTokens;	
		double probT2 = ((1+5+2)+beta) / (double) totTokens;	
		double probT3 = ((0+0+0)+beta) / (double) totTokens;	
		double [] probs = LDAUtils.calcWordProb(typeTopicCounts, beta);
		assertEquals(typeTopicCounts.length, probs.length);
		assertEquals(probT1, probs[0], 0.00000001);
		assertEquals(probT2, probs[1], 0.00000001);
		assertEquals(probT3, probs[2], 0.00000001);
		assertEquals(beta/totTokens, probs[2], 0.00000001);
	}
	
	@Test
	public void testCalcWordProbGivenTopic() {
		double beta = 0.1;
		int [][] typeTopicCounts = new int [][] {
				{3,2,5},
				{1,5,2},
				{0,0,0}};
		int topic1Sum = 3+1;
		int topic2Sum = 2+5;
		int nrTopics = typeTopicCounts[0].length;
		int nrWords = typeTopicCounts.length;
		double probW1GivenT1 = (typeTopicCounts[0][0] + (beta / nrTopics)) /  (topic1Sum + (nrWords*beta/nrTopics));
		double probW2GivenT2 = (typeTopicCounts[1][1] + (beta / nrTopics)) /  (topic2Sum + (nrWords*beta/nrTopics));
		double probW3GivenT1 = (typeTopicCounts[2][0] + (beta / nrTopics)) /  (topic1Sum + (nrWords*beta/nrTopics));
		double [][] probsWordGivenTopic = LDAUtils.calcWordProbGivenTopic(typeTopicCounts, beta);
		assertEquals(probW1GivenT1, probsWordGivenTopic[0][0], 0.00000001);
		assertEquals(probW2GivenT2, probsWordGivenTopic[1][1], 0.00000001);
		assertEquals(probW3GivenT1, probsWordGivenTopic[2][0], 0.00000001);
	}
}
