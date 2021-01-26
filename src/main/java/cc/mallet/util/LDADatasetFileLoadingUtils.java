package cc.mallet.util;


import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Iterator;

import cc.mallet.pipe.CharSequenceLowercase;
import cc.mallet.pipe.FeatureCountPipe;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.RawTokenizer;
import cc.mallet.pipe.SerialPipes;
import cc.mallet.pipe.SimpleTokenizer;
import cc.mallet.pipe.SimpleTokenizerLarge;
import cc.mallet.pipe.StringList2FeatureSequence;
import cc.mallet.pipe.Target2Label;
import cc.mallet.pipe.TfIdfPipe;
import cc.mallet.pipe.iterator.CsvIterator;
import cc.mallet.types.Alphabet;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.types.LabelAlphabet;


public class LDADatasetFileLoadingUtils {

	/**
	 * Loads instances and prunes away low occurring words
	 * 
	 * @param inputFile Input file to load
	 * @param stoplistFile File with stopwords, one per line
	 * @param pruneCount The number of times a word must occur in the corpus to be included
	 * @param keepNumbers Boolean flag to signal to keep numbers or not
	 * @param keepConnectors Keep connectors. General category "Pc" in the Unicode specification.
	 * @param dataAlphabet And optional (null else) data alphabet to use (typically used when loading a test set)
	 * @return An InstanceList with the data in the input file
	 * @throws FileNotFoundException
	 */
	public static InstanceList loadInstancesPrune(String inputFile, String stoplistFile, int pruneCount, boolean keepNumbers, 
			int maxBufSize, boolean keepConnectors, Alphabet dataAlphabet, LabelAlphabet targetAlphabet) throws FileNotFoundException {
		SimpleTokenizerLarge tokenizer;
		String lineRegex = "^(\\S*)[\\s,]*([^\\t]+)[\\s,]*(.*)$";
		int dataGroup = 3;
		int labelGroup = 2;
		int nameGroup = 1; // data, label, name fields

		tokenizer = LDAUtils.initTokenizer(stoplistFile, keepNumbers, maxBufSize, keepConnectors);

		if (pruneCount > 0) {
			CsvIterator reader = new CsvIterator(
					new FileReader(inputFile),
					lineRegex,
					dataGroup,
					labelGroup,
					nameGroup);

			ArrayList<Pipe> pipes = new ArrayList<Pipe>();
			Alphabet alphabet = null;
			if(dataAlphabet==null) {
				alphabet = new Alphabet();
			} else {
				alphabet = dataAlphabet;
			}

			CharSequenceLowercase csl = new CharSequenceLowercase();
			SimpleTokenizer st = tokenizer.deepClone();
			StringList2FeatureSequence sl2fs = new StringList2FeatureSequence(alphabet);
			FeatureCountPipe featureCounter = new FeatureCountPipe(alphabet, null);

			pipes.add(csl);
			pipes.add(st);
			pipes.add(sl2fs);
			if (pruneCount > 0) {
				pipes.add(featureCounter);
			}

			Pipe serialPipe = new SerialPipes(pipes);

			Iterator<Instance> iterator = serialPipe.newIteratorFrom(reader);

			int count = 0;

			// We aren't really interested in the instance itself,
			//  just the total feature counts.
			while (iterator.hasNext()) {
				count++;
				if (count % 100000 == 0) {
					System.out.println(count);
				}
				iterator.next();
			}

			if (pruneCount > 0) {
				featureCounter.addPrunedWordsToStoplist(tokenizer, pruneCount);
			}
		}

		CsvIterator reader = new CsvIterator(
				new FileReader(inputFile),
				lineRegex,
				dataGroup,
				labelGroup,
				nameGroup);

		ArrayList<Pipe> pipes = new ArrayList<Pipe>();
		Alphabet alphabet = null;
		if(dataAlphabet==null) {
			alphabet = new Alphabet();
		} else {
			alphabet = dataAlphabet;
		}

		CharSequenceLowercase csl = new CharSequenceLowercase();
		StringList2FeatureSequence sl2fs = new StringList2FeatureSequence(alphabet);

		LabelAlphabet tAlphabet = null;
		if(targetAlphabet==null) {
			tAlphabet = new LabelAlphabet();
		} else {
			tAlphabet = targetAlphabet;
		}

		Target2Label ttl = new Target2Label (tAlphabet);

		pipes.add(csl);
		pipes.add(tokenizer);
		pipes.add(sl2fs);
		pipes.add(ttl);

		Pipe serialPipe = new SerialPipes(pipes);

		InstanceList instances = new InstanceList(serialPipe);
		instances.addThruPipe(reader);

		return instances;
	}

	/**
	 * Loads instances and keeps the <code>keepCount</code> number of words with 
	 * the highest TF-IDF
	 * 
	 * @param inputFile Input file to load
	 * @param stoplistFile File with stopwords, one per line
	 * @param keepCount The number of words to keep (based on TF-IDF)
	 * @param keepNumbers Boolean flag to signal to keep numbers or not
	 * @param keepConnectors Keep connectors. General category "Pc" in the Unicode specification.
	 * @param dataAlphabet And optional (null else) data alphabet to use (typically used when loading a test set)
	 * @return An InstanceList with the data in the input file
	 * @throws FileNotFoundException
	 */
	public static InstanceList loadInstancesRaw(String inputFile, String stoplistFile, int keepCount, int maxBufSize, 
			Alphabet dataAlphabet, LabelAlphabet targetAlphabet) throws FileNotFoundException {
		RawTokenizer tokenizer;
		String lineRegex = "^(\\S*)[\\s,]*([^\\t]+)[\\s,]*(.*)$";
		int dataGroup = 3;
		int labelGroup = 2;
		int nameGroup = 1; // data, label, name fields

		tokenizer = LDAUtils.initRawTokenizer(stoplistFile, maxBufSize);

		if (keepCount > 0) {
			CsvIterator reader = new CsvIterator(
					new FileReader(inputFile),
					lineRegex,
					dataGroup,
					labelGroup,
					nameGroup);

			ArrayList<Pipe> pipes = new ArrayList<Pipe>();
			Alphabet alphabet = null;
			if(dataAlphabet==null) {
				alphabet = new Alphabet();
			} else {
				alphabet = dataAlphabet;
			}

			SimpleTokenizer st = tokenizer.deepClone();
			StringList2FeatureSequence sl2fs = new StringList2FeatureSequence(alphabet);
			TfIdfPipe tfIdfPipe = new TfIdfPipe(alphabet, null);

			pipes.add(st);
			pipes.add(sl2fs);
			if (keepCount > 0) {
				pipes.add(tfIdfPipe);
			}

			Pipe serialPipe = new SerialPipes(pipes);

			Iterator<Instance> iterator = serialPipe.newIteratorFrom(reader);

			int count = 0;

			// We aren't really interested in the instance itself,
			//  just the total feature counts.
			while (iterator.hasNext()) {
				count++;
				if (count % 100000 == 0) {
					System.out.println(count);
				}
				iterator.next();
			}

			if (keepCount > 0) {
				tfIdfPipe.addPrunedWordsToStoplist(tokenizer, keepCount);
			}
		}

		CsvIterator reader = new CsvIterator(
				new FileReader(inputFile),
				lineRegex,
				dataGroup,
				labelGroup,
				nameGroup);

		ArrayList<Pipe> pipes = new ArrayList<Pipe>();
		Alphabet alphabet = null;
		if(dataAlphabet==null) {
			alphabet = new Alphabet();
		} else {
			alphabet = dataAlphabet;
		}

		StringList2FeatureSequence sl2fs = new StringList2FeatureSequence(alphabet);

		LabelAlphabet tAlphabet = null;
		if(targetAlphabet==null) {
			tAlphabet = new LabelAlphabet();
		} else {
			tAlphabet = targetAlphabet;
		}

		Target2Label ttl = new Target2Label (tAlphabet);

		pipes.add(tokenizer);
		pipes.add(sl2fs);
		pipes.add(ttl);

		Pipe serialPipe = new SerialPipes(pipes);

		InstanceList instances = new InstanceList(serialPipe);
		instances.addThruPipe(reader);

		return instances;
	}

	/**
	 * Loads instances and keeps the <code>keepCount</code> number of words with 
	 * the highest TF-IDF
	 * 
	 * @param inputFile Input file to load
	 * @param stoplistFile File with stopwords, one per line
	 * @param keepCount The number of words to keep (based on TF-IDF)
	 * @param keepNumbers Boolean flag to signal to keep numbers or not
	 * @param keepConnectors Keep connectors. General category "Pc" in the Unicode specification.
	 * @param dataAlphabet And optional (null else) data alphabet to use (typically used when loading a test set)
	 * @return An InstanceList with the data in the input file
	 * @throws FileNotFoundException
	 */
	public static InstanceList loadInstancesKeep(String inputFile, String stoplistFile, int keepCount, boolean keepNumbers, 
			int maxBufSize, boolean keepConnectors, Alphabet dataAlphabet, LabelAlphabet targetAlphabet) throws FileNotFoundException {
		SimpleTokenizerLarge tokenizer;
		String lineRegex = "^(\\S*)[\\s,]*([^\\t]+)[\\s,]*(.*)$";
		int dataGroup = 3;
		int labelGroup = 2;
		int nameGroup = 1; // data, label, name fields

		tokenizer = LDAUtils.initTokenizer(stoplistFile, keepNumbers, maxBufSize, keepConnectors);

		if (keepCount > 0) {
			CsvIterator reader = new CsvIterator(
					new FileReader(inputFile),
					lineRegex,
					dataGroup,
					labelGroup,
					nameGroup);

			ArrayList<Pipe> pipes = new ArrayList<Pipe>();
			Alphabet alphabet = null;
			if(dataAlphabet==null) {
				alphabet = new Alphabet();
			} else {
				alphabet = dataAlphabet;
			}

			CharSequenceLowercase csl = new CharSequenceLowercase();
			SimpleTokenizer st = tokenizer.deepClone();
			StringList2FeatureSequence sl2fs = new StringList2FeatureSequence(alphabet);
			TfIdfPipe tfIdfPipe = new TfIdfPipe(alphabet, null);

			pipes.add(csl);
			pipes.add(st);
			pipes.add(sl2fs);
			if (keepCount > 0) {
				pipes.add(tfIdfPipe);
			}

			Pipe serialPipe = new SerialPipes(pipes);

			Iterator<Instance> iterator = serialPipe.newIteratorFrom(reader);

			int count = 0;

			// We aren't really interested in the instance itself,
			//  just the total feature counts.
			while (iterator.hasNext()) {
				count++;
				if (count % 100000 == 0) {
					System.out.println(count);
				}
				iterator.next();
			}

			if (keepCount > 0) {
				tfIdfPipe.addPrunedWordsToStoplist(tokenizer, keepCount);
			}
		}

		CsvIterator reader = new CsvIterator(
				new FileReader(inputFile),
				lineRegex,
				dataGroup,
				labelGroup,
				nameGroup);

		ArrayList<Pipe> pipes = new ArrayList<Pipe>();
		Alphabet alphabet = null;
		if(dataAlphabet==null) {
			alphabet = new Alphabet();
		} else {
			alphabet = dataAlphabet;
		}

		CharSequenceLowercase csl = new CharSequenceLowercase();
		StringList2FeatureSequence sl2fs = new StringList2FeatureSequence(alphabet);

		LabelAlphabet tAlphabet = null;
		if(targetAlphabet==null) {
			tAlphabet = new LabelAlphabet();
		} else {
			tAlphabet = targetAlphabet;
		}

		Target2Label ttl = new Target2Label (tAlphabet);

		pipes.add(csl);
		pipes.add(tokenizer);
		pipes.add(sl2fs);
		pipes.add(ttl);

		Pipe serialPipe = new SerialPipes(pipes);

		InstanceList instances = new InstanceList(serialPipe);
		instances.addThruPipe(reader);

		return instances;
	}
}
