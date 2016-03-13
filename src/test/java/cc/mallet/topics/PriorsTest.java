package cc.mallet.topics;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;

import org.junit.Test;

import cc.mallet.types.Alphabet;
import cc.mallet.types.MatrixOps;

public class PriorsTest {

	@Test
	public void test() throws IOException {
		List<String> lines = Files.readAllLines(Paths.get("src/test/resources/topic_priors.txt"), Charset.defaultCharset());
		for (String string : lines) {
			System.out.println(string);
		}
		Alphabet a = new Alphabet();
		for (String string : lines) {
			String [] spec = string.split(",");
			for (int i = 1; i < spec.length; i++) {
				String word = spec[i];
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
		Collection [] spec = SpaliasUncollapsedParallelWithPriors.extractPriorSpec(lines, 4);
		System.out.println("Zero Out");
		for (Collection string : spec) {
			System.out.println(string);
		}
		
		System.out.println();
		
		System.out.println("Alphabet contains: " + a.size() + " words");
		MatrixOps.print(SpaliasUncollapsedParallelWithPriors.calculatePriors("src/test/resources/topic_priors.txt", lines.size(), a.size(), a));
		
	}

}
