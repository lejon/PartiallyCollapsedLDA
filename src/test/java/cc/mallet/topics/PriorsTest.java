package cc.mallet.topics;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import cc.mallet.types.Alphabet;
import cc.mallet.types.MatrixOps;

public class PriorsTest {
	
	// 0, java, jvm, jre, NullPointerException
	// 2, cell, control, cabinet

	@Test
	public void testTopicPriors() throws IOException {
		List<String> lines = Files.readAllLines(Paths.get("src/test/resources/topic_priors.txt"), Charset.defaultCharset());
		Alphabet a = new Alphabet();
		for (String string : lines) {
			String [] spec = string.split(",");
			for (int i = 1; i < spec.length; i++) {
				String word = spec[i].trim();
				a.lookupIndex (word, true);
			}
		}
		
		// Add some other words to the alphabet
		String [] otherWords = {"apple", "banan", "citron", "durian"};
		for (int i = 0; i < otherWords.length; i++) {
			String word = otherWords[i];
			a.lookupIndex (word, true);
		}
		
		System.out.println("Alphabet is:\n" + a);
		System.out.println("====");
		Collection [] spec = SpaliasUncollapsedParallelWithPriors.extractPriorSpec(lines, 4);
		System.out.println("Zero Out");
		for (Collection string : spec) {
			System.out.println(string);
		}
		
		assertEquals(Arrays.asList(new String [] {"cabinet",  "cell",  "control"}), new ArrayList<String>(spec[0]));
		assertEquals(Arrays.asList(new String [] {"NullPointerException", "cabinet", "cell", "control", "java", "jre", "jvm"}), new ArrayList<String>(spec[1]));
		assertEquals(Arrays.asList(new String [] {"NullPointerException", "java", "jre", "jvm"}), new ArrayList<String>(spec[2]));
		assertEquals(Arrays.asList(new String [] {"NullPointerException", "cabinet", "cell", "control", "java", "jre", "jvm"}), new ArrayList<String>(spec[3]));
		
		System.out.println();
		
		System.out.println("Alphabet contains: " + a.size() + " words");
		MatrixOps.print(SpaliasUncollapsedParallelWithPriors.calculatePriors("src/test/resources/topic_priors.txt", 4, a.size(), a));
		
		double [][] priors = SpaliasUncollapsedParallelWithPriors.calculatePriors("src/test/resources/topic_priors.txt", 4, a.size(), a);
		myAssertDoubleArrayEquals(new double [] {1.0, 1.0, 1.0, 1.0, 0.0, 0.0, 0.0, 1.0, 1.0, 1.0, 1.0}, priors[0], 0.0000001);
		myAssertDoubleArrayEquals(new double [] {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 1.0, 1.0, 1.0}, priors[1], 0.0000001);
		myAssertDoubleArrayEquals(new double [] {0.0, 0.0, 0.0, 0.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0}, priors[2], 0.0000001);
		myAssertDoubleArrayEquals(new double [] {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 1.0, 1.0, 1.0}, priors[3], 0.0000001);
	}
	
	private void myAssertDoubleArrayEquals(double[] ds, double[] ds2, double tolerance) {
		assertEquals(ds.length,ds2.length);
		for (int i = 0; i < ds.length; i++) {
			assertEquals(ds[i],ds2[i],tolerance);
		}
	}

	@Test
	public void testDocumentPriors() throws IOException {
		List<String> lines = Files.readAllLines(Paths.get("src/test/resources/document_priors.txt"), Charset.defaultCharset());
		for (String string : lines) {
			System.out.println(string);
		}
		Map<Integer,int []> specs = SpaliasUncollapsedParallelWithPriors.calculateSparsePriors("src/test/resources/document_priors.txt");
		assertEquals(2,specs.size());
		System.out.println(specs);
		assertArrayEquals(new int [] {0,2,4}, specs.get(0));
		assertArrayEquals(new int [] {1,3,5}, specs.get(19));
		
	}

}
