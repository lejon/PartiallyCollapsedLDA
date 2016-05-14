package cc.mallet.topics;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import org.junit.Test;

import cc.mallet.util.LDAUtils;

public class LDAUtilsTest {

	@Test
	public void testWriteDobuleMatrixRows() throws IOException {
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


}
