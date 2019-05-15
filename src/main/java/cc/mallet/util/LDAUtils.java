package cc.mallet.util;

import static java.lang.Math.log;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import cc.mallet.configuration.LDAConfiguration;
import cc.mallet.pipe.CharSequence2TokenSequence;
import cc.mallet.pipe.CharSequenceLowercase;
import cc.mallet.pipe.FeatureCountPipe;
import cc.mallet.pipe.Input2CharSequence;
import cc.mallet.pipe.KeepConnectorPunctuationNumericAlsoTokenizer;
import cc.mallet.pipe.KeepConnectorPunctuationTokenizerLarge;
import cc.mallet.pipe.NumericAlsoTokenizer;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.SerialPipes;
import cc.mallet.pipe.SimpleTokenizer;
import cc.mallet.pipe.SimpleTokenizerLarge;
import cc.mallet.pipe.StringList2FeatureSequence;
import cc.mallet.pipe.Target2Label;
import cc.mallet.pipe.TfIdfPipe;
import cc.mallet.pipe.TokenSequence2FeatureSequence;
import cc.mallet.pipe.TokenSequenceLowercase;
import cc.mallet.pipe.TokenSequencePredicateMatcher;
import cc.mallet.pipe.TokenSequenceRemoveStopwords;
import cc.mallet.pipe.iterator.CsvIterator;
import cc.mallet.pipe.iterator.FileIterator;
import cc.mallet.topics.LogState;
import cc.mallet.topics.TopicAssignment;
import cc.mallet.topics.TopicInferencer;
import cc.mallet.types.Alphabet;
import cc.mallet.types.Dirichlet;
import cc.mallet.types.FeatureSequence;
import cc.mallet.types.IDSorter;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.types.LabelAlphabet;
import cc.mallet.types.LabelSequence;

public class LDAUtils {

	public LDAUtils() {
	}

	//	public static InstanceList buildInstancePipeList(boolean numeric) {
	//		return buildInstancePipeList(numeric, true);
	//	}
	//
	//	public static InstanceList buildInstancePipeList(boolean numeric, boolean useStoplist) {
	//		// Begin by importing documents from text to feature sequences
	//		ArrayList<Pipe> pipeList = new ArrayList<Pipe>();
	//
	//		// Pipes: lowercase, tokenize, remove stopwords, map to features
	//		pipeList.add( new CharSequenceLowercase() );
	//		if(numeric) {
	//			pipeList.add( new CharSequence2TokenSequence(Pattern.compile("\\p{N}+")) );
	//		} else {
	//			pipeList.add( new CharSequence2TokenSequence(Pattern.compile("\\p{L}[\\p{L}\\p{P}]+\\p{L}")) );
	//		}
	//		if(useStoplist) {
	//			pipeList.add( new TokenSequenceRemoveStopwords(new File("stoplist.txt"), "UTF-8", false, false, false) );
	//		}
	//		pipeList.add( new TokenSequence2FeatureSequence() );
	//		//pipeList.add( new PrintInputAndTarget() );
	//
	//		return new InstanceList (new SerialPipes(pipeList));
	//	}
	
	public static Pipe buildSerialPipe(String stoplistFile) {
		return buildSerialPipe(stoplistFile, null);
	}
	
	public static Pipe buildSerialPipe(String stoplistFile, Alphabet dataAlphabet) {
		return buildSerialPipe(stoplistFile, dataAlphabet, null);
	}
	
	public static Pipe buildSerialPipe(String stoplistFile, Alphabet dataAlphabet, LabelAlphabet targetAlphabet) { 		
		int maxBufSize = 10000;
		SimpleTokenizerLarge tokenizer = new SimpleTokenizerLarge(new File(stoplistFile), maxBufSize);
		
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
		return serialPipe;
	}
	
	public static InstanceList loadDataset(LDAConfiguration config, String dataset_fn) throws FileNotFoundException {
		return loadDataset(config, dataset_fn, null);
	}
	
	public static InstanceList loadDataset(LDAConfiguration config, String dataset_fn, Alphabet alphabet) throws FileNotFoundException {
		InstanceList instances;
		
		File dsf = new File(dataset_fn); 
		if(dsf.isDirectory()) {
			instances = LDAUtils.loadInstanceDirectory(
					dataset_fn, 
					config.getFileRegex(LDAConfiguration.FILE_REGEX_DEFAULT),
					config.getStoplistFilename("stoplist.txt"), 
					config.getRareThreshold(LDAConfiguration.RARE_WORD_THRESHOLD), 
					config.keepNumbers(), 
					config.getMaxDocumentBufferSize(LDAConfiguration.MAX_DOC_BUFFFER_SIZE_DEFAULT), 
					config.getKeepConnectingPunctuation(LDAConfiguration.KEEP_CONNECTING_PUNCTUATION),
					alphabet);
			if(instances.size()==0) {
				System.err.println("No instances loaded. Perhaps your filename REGEX ('" 
						+ config.getFileRegex(LDAConfiguration.FILE_REGEX_DEFAULT) + "') was wrong?");
				System.err.println("Remember that Java RE's are not the same as Perls. \nTo match a filename that ends with '.txt', the regex would be '" 
						+ LDAConfiguration.FILE_REGEX_DEFAULT + "'");
				System.err.println("The filename given to match the regex against is the _full absolute path_ of the file.");
				System.exit(-1);
			}
		} else {
			if(config.getTfIdfVocabSize(LDAConfiguration.TF_IDF_VOCAB_SIZE_DEFAULT)>0) {
				instances = LDAUtils.loadInstancesKeep(
						dataset_fn, 
						config.getStoplistFilename("stoplist.txt"), 
						config.getTfIdfVocabSize(LDAConfiguration.TF_IDF_VOCAB_SIZE_DEFAULT), 
						config.keepNumbers(), 
						config.getMaxDocumentBufferSize(LDAConfiguration.MAX_DOC_BUFFFER_SIZE_DEFAULT), 
						config.getKeepConnectingPunctuation(LDAConfiguration.KEEP_CONNECTING_PUNCTUATION), alphabet);					
			} else {					
				instances = LDAUtils.loadInstancesPrune(
						dataset_fn, 
						config.getStoplistFilename("stoplist.txt"), 
						config.getRareThreshold(LDAConfiguration.RARE_WORD_THRESHOLD), 
						config.keepNumbers(), 
						config.getMaxDocumentBufferSize(LDAConfiguration.MAX_DOC_BUFFFER_SIZE_DEFAULT), 
						config.getKeepConnectingPunctuation(LDAConfiguration.KEEP_CONNECTING_PUNCTUATION), alphabet);
			}
		}
		return instances;
	}
	
	public static TfIdfPipe getTfIdfPipeFromConfig(LDAConfiguration config) throws FileNotFoundException {
		TfIdfPipe tfIdfPipe = LDAUtils.getTfIdfPipe(
						config.getDatasetFilename(), 
						config.getStoplistFilename("stoplist.txt"), 
						config.getTfIdfVocabSize(LDAConfiguration.TF_IDF_VOCAB_SIZE_DEFAULT), 
						config.keepNumbers(), 
						config.getMaxDocumentBufferSize(LDAConfiguration.MAX_DOC_BUFFFER_SIZE_DEFAULT), 
						config.getKeepConnectingPunctuation(LDAConfiguration.KEEP_CONNECTING_PUNCTUATION), 
						null, null);					
		return tfIdfPipe;
	}

	/**
	 * This has to be done in two sweeps to first find the counts then remove rare words
	 * @param numeric
	 * @param stoplistFile
	 * @param inputFile
	 * @return
	 * @throws FileNotFoundException 
	 */
	public static InstanceList loadInstances(String inputFile, String stoplistFile, int pruneCount) throws FileNotFoundException {
		return loadInstancesPrune(inputFile, stoplistFile, pruneCount, true, LDAConfiguration.MAX_DOC_BUFFFER_SIZE_DEFAULT, false, null);
	}
	
	public static InstanceList loadInstances(String inputFile, String stoplistFile, int pruneCount, Alphabet dataAlphabet) throws FileNotFoundException {
		return loadInstancesPrune(inputFile, stoplistFile, pruneCount, true, LDAConfiguration.MAX_DOC_BUFFFER_SIZE_DEFAULT, false, dataAlphabet);
	}
	
	public static InstanceList loadInstancesPrune(String inputFile, String stoplistFile, int pruneCount, boolean keepNumbers) throws FileNotFoundException {
		return loadInstancesPrune(inputFile, stoplistFile, pruneCount, keepNumbers, LDAConfiguration.MAX_DOC_BUFFFER_SIZE_DEFAULT, false, null);
	}

	public static InstanceList loadInstancesPrune(String inputFile, String stoplistFile, int pruneCount, boolean keepNumbers, 
			int maxBufSize, boolean keepConnectors, Alphabet dataAlphabet) throws FileNotFoundException {
		return loadInstancesPrune(inputFile, stoplistFile, pruneCount, keepNumbers,	maxBufSize, keepConnectors, dataAlphabet, null);
	}
	
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

		tokenizer = initTokenizer(stoplistFile, keepNumbers, maxBufSize, keepConnectors);

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

	public static InstanceList loadInstancesKeep(String inputFile, String stoplistFile, int keepCount, boolean keepNumbers) throws FileNotFoundException {
		return loadInstancesKeep(inputFile, stoplistFile, keepCount, keepNumbers, LDAConfiguration.MAX_DOC_BUFFFER_SIZE_DEFAULT, false, null);
	}
	
	public static InstanceList loadInstancesKeep(String inputFile, String stoplistFile, int keepCount, boolean keepNumbers, 
			int maxBufSize, boolean keepConnectors, Alphabet dataAlphabet) throws FileNotFoundException {
		return loadInstancesKeep(inputFile, stoplistFile, keepCount, keepNumbers, 
				maxBufSize, keepConnectors, dataAlphabet, null);
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

		tokenizer = initTokenizer(stoplistFile, keepNumbers, maxBufSize, keepConnectors);

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
	
	/**
	 * Re-creates the pipe that is used if loading with TF-IDF
	 * This is ugly as hell, but I wanted ti to be as similar as
	 * possible as when using loadDataset
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
	public static TfIdfPipe getTfIdfPipe(String inputFile, String stoplistFile, int keepCount, boolean keepNumbers, 
			int maxBufSize, boolean keepConnectors, Alphabet dataAlphabet, LabelAlphabet targetAlphabet) throws FileNotFoundException {
		SimpleTokenizerLarge tokenizer;
		String lineRegex = "^(\\S*)[\\s,]*([^\\t]+)[\\s,]*(.*)$";
		int dataGroup = 3;
		int labelGroup = 2;
		int nameGroup = 1; // data, label, name fields

		tokenizer = initTokenizer(stoplistFile, keepNumbers, maxBufSize, keepConnectors);

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
				return tfIdfPipe;
			}
		} else {
			return null;
		}
		return null;
	}

	static SimpleTokenizerLarge initTokenizer(String stoplistFile, boolean keepNumbers, int maxBufSize, boolean keepConnectors) {
		SimpleTokenizerLarge tokenizer;
		if(keepConnectors) {
			if (stoplistFile != null) {
				if(keepNumbers) {				
					tokenizer = new KeepConnectorPunctuationNumericAlsoTokenizer(new File(stoplistFile), maxBufSize);
				} else {
					tokenizer = new KeepConnectorPunctuationTokenizerLarge(new File(stoplistFile), maxBufSize);
				}
			} else {
				if(keepNumbers) {
					tokenizer = new KeepConnectorPunctuationNumericAlsoTokenizer(NumericAlsoTokenizer.USE_EMPTY_STOPLIST, maxBufSize);
				} else {
					tokenizer = new KeepConnectorPunctuationTokenizerLarge(NumericAlsoTokenizer.USE_EMPTY_STOPLIST, maxBufSize);
				}
			}
		} else {
			if (stoplistFile != null) {
				if(keepNumbers) {				
					tokenizer = new NumericAlsoTokenizer(new File(stoplistFile), maxBufSize);
				} else {
					tokenizer = new SimpleTokenizerLarge(new File(stoplistFile), maxBufSize);
				}
			} else {
				if(keepNumbers) {
					tokenizer = new NumericAlsoTokenizer(NumericAlsoTokenizer.USE_EMPTY_STOPLIST, maxBufSize);
				} else {
					tokenizer = new SimpleTokenizerLarge(NumericAlsoTokenizer.USE_EMPTY_STOPLIST, maxBufSize);
				}
			}
		}
		return tokenizer;
	}
	
	public static String[][] getTopRelevanceWords(int noWords, int numTypes, int numTopics, 
			int[][] typeTopicCounts, double beta, double lambda, Alphabet alphabet) {
		if(noWords>numTypes) {
			throw new IllegalArgumentException("Asked for more words (" + noWords + ") than there are types (unique words = noTypes = " + numTypes + ")."); 
		}
		IDSorter[] sortedWords = new IDSorter[numTypes];
		String [][] topTopicWords = new String[numTopics][noWords];
		
		double [][] p_w_k = calcWordProbGivenTopic(typeTopicCounts, beta);
		double [] p_w = calcWordProb(typeTopicCounts, beta);

		for (int topic = 0; topic < numTopics; topic++) {
			for (int type = 0; type < numTypes; type++) {
				double relevance = lambda * log(p_w_k[type][topic]) + (1-lambda) * (log(p_w_k[type][topic]) - log(p_w[type])); 
				sortedWords[type] = new IDSorter(type, relevance);
			}

			Arrays.sort(sortedWords);

			for (int i=0; i < noWords && i < topTopicWords[topic].length && i < numTypes; i++) {
				topTopicWords[topic][i] = (String) alphabet.lookupObject(sortedWords[i].getID());
			}
		}
		return topTopicWords;
	}
	
	public static String[][] getTopDistinctiveWords(int noWords, int numTypes, int numTopics, 
			int[][] typeTopicCounts, double beta, Alphabet alphabet) {
		if(noWords>numTypes) {
			throw new IllegalArgumentException("Asked for more words (" + noWords + ") than there are types (unique words = noTypes = " + numTypes + ")."); 
		}
		IDSorter[] sortedWords = new IDSorter[numTypes];
		String [][] topTopicWords = new String[numTopics][noWords];
		
		double [][] p_w_k = calcTopicProbGivenWord(typeTopicCounts, beta);
		double [] p_w = calcWordProb(typeTopicCounts, beta);

		double [][] distinctiveness = calcWordDistinctiveness(p_w_k, p_w);
		
		for (int topic = 0; topic < numTopics; topic++) {
			for (int type = 0; type < numTypes; type++) {
				sortedWords[type] = new IDSorter(type, distinctiveness[type][topic]);
			}

			Arrays.sort(sortedWords);

			for (int i=0; i < noWords && i < topTopicWords[topic].length && i < numTypes; i++) {
				topTopicWords[topic][i] = (String) alphabet.lookupObject(sortedWords[i].getID());
			}
		}
		return topTopicWords;
	}
	
	public static String[][] getTopSalientWords(int noWords, int numTypes, int numTopics, 
			int[][] typeTopicCounts, double beta, Alphabet alphabet) {
		if(noWords>numTypes) {
			throw new IllegalArgumentException("Asked for more words (" + noWords + ") than there are types (unique words = noTypes = " + numTypes + ")."); 
		}
		IDSorter[] sortedWords = new IDSorter[numTypes];
		String [][] topTopicWords = new String[numTopics][noWords];
		
		double [][] p_w_k = calcTopicProbGivenWord(typeTopicCounts, beta);
		double [] p_w = calcWordProb(typeTopicCounts, beta);
		
		double [][] saliency = calcWordSaliency(p_w_k,p_w);

		for (int topic = 0; topic < numTopics; topic++) {
			for (int type = 0; type < numTypes; type++) { 
				sortedWords[type] = new IDSorter(type, saliency[type][topic]);
			}

			Arrays.sort(sortedWords);

			for (int i=0; i < noWords && i < topTopicWords[topic].length && i < numTypes; i++) {
				topTopicWords[topic][i] = (String) alphabet.lookupObject(sortedWords[i].getID());
			}
		}
		return topTopicWords;
	}
	
	/**
	 * Calculate word distinctiveness as defined in: 
	 * Termite: Visualization Techniques for Assessing Textual Topic Models
	 * by Jason Chuang, Christopher D. Manning, Jeffrey Heer
	 * @param p_w_k probability of topic given word
	 * @param p_w probability of a word
	 * @return array with word distinctiveness measures dim(array) = nrWords x nrTopics
	 */
	public static double[][] calcWordDistinctiveness(double [][] p_w_k, double [] p_w) {
		int nrTopics = p_w_k[0].length;
		int nrWords = p_w_k.length;
		double [][] wordDistinctiveness = new double[nrWords][nrTopics];
		for (int w = 0; w < nrWords; w++) {
			for (int k = 0; k < nrTopics; k++) {
				wordDistinctiveness[w][k] += p_w_k[w][k] * log(p_w_k[w][k] / p_w[w]);
			}
		}
		return wordDistinctiveness;
	}
	
	/**
	 * Calculate word saliency as defined in: 
	 * Termite: Visualization Techniques for Assessing Textual Topic Models
	 * by Jason Chuang, Christopher D. Manning, Jeffrey Heer
	 * @param p_w_k probability of topic given word
	 * @param p_w probability of a word
	 * @return array with word saliency measures
	 */
	public static double[][] calcWordSaliency(double [][] p_w_k, double [] p_w) {
		int nrTopics = p_w_k[0].length;
		int nrWords = p_w_k.length;
		double [][] wordDistinctiveness = calcWordDistinctiveness(p_w_k, p_w);
		double [][] wordSaliency = new double[nrWords][nrTopics];
		for (int w = 0; w < wordSaliency.length; w++) {
			for (int k = 0; k < nrTopics; k++) {
				wordSaliency[w][k] = p_w[w] * wordDistinctiveness[w][k];
			}
		}
		return wordSaliency;
	}

	public static double[] calcTopicProb(int[][] typeTopicCounts) {
		int nrTopics = typeTopicCounts[0].length;
		int nrWords = typeTopicCounts.length;
		double [] topicProbs = new double[nrTopics];
		double totalMass = 0;
		for (int k = 0; k < nrTopics; k++) { 
			for (int w = 0; w < nrWords; w++) {
				topicProbs[k] += typeTopicCounts[w][k];
				totalMass += typeTopicCounts[w][k];
			}
		}
		for (int k = 0; k < nrTopics; k++) {
			topicProbs[k] /= totalMass;
		}
		return topicProbs;
	}

	public static double[] calcWordProb(int[][] typeTopicCounts, double beta) {
		int nrTopics = typeTopicCounts[0].length;
		int nrWords = typeTopicCounts.length;
		
		double [] wordProbs = new double[nrWords];
		double wordMass = 0;
		for (int w = 0; w < nrWords; w++) {
			for (int k = 0; k < nrTopics; k++) { 
				wordMass += typeTopicCounts[w][k] + beta;
				wordProbs[w] += typeTopicCounts[w][k] + beta;
			}
		}
		for (int w = 0; w < nrWords; w++) {
			wordProbs[w] /= wordMass;
		}
		return wordProbs;
	}
	
	public static double[] calcUnsmoothedWordProb(int[][] typeTopicCounts) {
		int nrTopics = typeTopicCounts[0].length;
		int nrWords  = typeTopicCounts.length;
		double [] wordProbs = new double[nrWords];
		double wordMass = 0;
		for (int w = 0; w < nrWords; w++) {
			for (int k = 0; k < nrTopics; k++) { 
				wordMass += typeTopicCounts[w][k];
				wordProbs[w] += typeTopicCounts[w][k];
			}
		}
		for (int w = 0; w < nrWords; w++) {
			wordProbs[w] /= wordMass;
		}
		return wordProbs;
	}
	
	
	/**
	 * Calculate KR1 re-weighting scheme as defined in "Topic and Keyword Re-ranking for LDA-based Topic Modeling"
	 * Yangqiu Song, Shimei Pan, Shixia Liu, Michelle X. Zhou, Weihong Qian
	 * 
	 * @param noWords
	 * @param numTypes
	 * @param numTopics
	 * @param typeTopicCounts
	 * @param beta
	 * @param alphabet
	 * @return
	 */
	public static String[][] getK1ReWeightedWords(int noWords, int numTypes, int numTopics, 
			int[][] typeTopicCounts, double beta, Alphabet alphabet) {
		if(noWords>numTypes) {
			throw new IllegalArgumentException("Asked for more words (" + noWords + ") than there are types (unique words = noTypes = " + numTypes + ")."); 
		}
		IDSorter[] sortedWords = new IDSorter[numTypes];
		String [][] topTopicWords = new String[numTopics][noWords];
	
		double[][] k1ReWeighted = calcK1(typeTopicCounts, beta); 

		for (int topic = 0; topic < numTopics; topic++) {
			for (int type = 0; type < numTypes; type++) {
				double relevance = k1ReWeighted[type][topic]; 
				sortedWords[type] = new IDSorter(type, relevance);
			}

			Arrays.sort(sortedWords);

			for (int i=0; i < noWords && i < topTopicWords[topic].length && i < numTypes; i++) {
				topTopicWords[topic][i] = (String) alphabet.lookupObject(sortedWords[i].getID());
			}
		}
		return topTopicWords;
	}
	
	/**
	 * Keyword Re-ranking as described in 'Topic and Keyword Re-ranking for LDA-based Topic Modeling', 
	 * by Yangqiu Song, Shimei Pan, Shixia Liu, Michelle X. Zhou, Weihong Qian
	 * 
	 * @param typeTopicCounts word frequencies according to topic
	 * @param beta beta prior
	 * @return matrix of re-weighted words given topic 
	 */
	public static double[][] calcK1(int[][] typeTopicCounts, double beta) {
		int nrTopics = typeTopicCounts[0].length;
		int nrWords = typeTopicCounts.length;
		double [][] wordK1GivenTopic = new double[nrWords][nrTopics];
		double [][] p_w_k = calcWordProbGivenTopic(typeTopicCounts, beta);
		
		for (int w = 0; w < nrWords; w++) {
			double wordMass = 0;
			for (int k = 0; k < nrTopics; k++) { 
				wordMass += p_w_k[w][k];
			}
			for (int k = 0; k < nrTopics; k++) {
				wordK1GivenTopic[w][k] = p_w_k[w][k] / wordMass;
			}
		}
		return wordK1GivenTopic;
	}

	public static double[][] calcWordProbGivenTopic(int[][] typeTopicCounts, double beta) {
		int nrTopics = typeTopicCounts[0].length;
		int nrWords = typeTopicCounts.length;
		
		double [][] wordProbGivenTopic = new double[nrWords][nrTopics];
		for (int k = 0; k < nrTopics; k++) { 
			double wordMass = 0;
			for (int w = 0; w < nrWords; w++) {
				wordMass += typeTopicCounts[w][k] + beta;
			}
			for (int w = 0; w < nrWords; w++) {
				wordProbGivenTopic[w][k] = (typeTopicCounts[w][k] + beta) / wordMass;
			}
		}
		return wordProbGivenTopic;
	}
	
	/**
	 * Calculate topic probability given a word
	 * @param typeTopicCounts
	 * @param beta beta prior
	 * @return matrix (topic first) of probabilities of topic given a word i.e matrix[topic][word] = probability of topic given word
	 */
	public static double[][] calcTopicProbGivenWord(int[][] typeTopicCounts, double beta) {
		int nrTopics = typeTopicCounts[0].length;
		int nrWords  = typeTopicCounts.length;
		
		double [] topicProbability = new double[nrTopics];
		double [] typeSum = new double[nrWords];
		double totalTokencount = 0;
		for (int type = 0; type < typeTopicCounts.length; type++) {
			for (int topic = 0; topic < typeTopicCounts[type].length; topic++) {
				double weight = typeTopicCounts[type][topic] + beta;
				topicProbability[topic] += weight;
				typeSum[type] += weight;
				totalTokencount += weight;
			}
		}
		double [][] probTopicGivenWord = new double[nrWords][nrTopics];
		for (int type = 0; type < typeTopicCounts.length; type++) {
			for (int topic = 0; topic < typeTopicCounts[type].length; topic++) {
				double weight = typeTopicCounts[type][topic] + beta;
				
				double p_w_t = weight / topicProbability[topic];
				double p_t   = topicProbability[topic] / totalTokencount;
				double p_w   = typeSum[type] / totalTokencount;
				
				double topicProb =  p_w_t * p_t / p_w;
				probTopicGivenWord[type][topic] = topicProb;
			}
		}
		
		return probTopicGivenWord;
	}
	
	public static double[][] calcUnsmoothedWordProbGivenTopic(int[][] typeTopicCounts) {
		int nrTopics = typeTopicCounts[0].length;
		int nrWords  = typeTopicCounts.length;
		double [][] wordProbGivenTopic = new double[nrWords][nrTopics];
		for (int k = 0; k < nrTopics; k++) { 
			double wordMassTopicK = 0;
			for (int w = 0; w < nrWords; w++) {
				wordMassTopicK += typeTopicCounts[w][k];
			}
			for (int w = 0; w < nrWords; w++) {
				wordProbGivenTopic[w][k] = typeTopicCounts[w][k]  / wordMassTopicK;
			}
		}
		return wordProbGivenTopic;
	}

	public static String[][] getTopWords(int noWords, int numTypes, int numTopics, int[][] typeTopicCounts, Alphabet alphabet) {
		if(noWords>numTypes) {
			throw new IllegalArgumentException("Asked for more words (" + noWords + ") than there are types (unique words = noTypes = " + numTypes + ")."); 
		}
		IDSorter[] sortedWords = new IDSorter[numTypes];
		String [][] topTopicWords = new String[numTopics][noWords];

		for (int topic = 0; topic < numTopics; topic++) {
			for (int type = 0; type < numTypes; type++) {
				sortedWords[type] = new IDSorter(type, typeTopicCounts[type][topic]);
			}

			Arrays.sort(sortedWords);

			for (int i=0; i < noWords && i < topTopicWords[topic].length && i < numTypes; i++) {
				topTopicWords[topic][i] = (String) alphabet.lookupObject(sortedWords[i].getID());
			}
		}
		return topTopicWords;
	}

	public static int[][] getTopWordIndices(int noWords, int numTypes, int numTopics, int[][] typeTopicCounts, Alphabet alphabet) {
		IDSorter[] sortedWords = new IDSorter[numTypes];
		int [][] topTopicWords = new int[numTopics][noWords];

		for (int topic = 0; topic < numTopics; topic++) {
			for (int type = 0; type < numTypes; type++) {
				sortedWords[type] = new IDSorter(type, typeTopicCounts[type][topic]);
			}

			Arrays.sort(sortedWords);

			for (int i=0; i < noWords; i++) {
				topTopicWords[topic][i] = sortedWords[i].getID();
			}
		}
		return topTopicWords;
	}


	public static void perplexityToFile(String loggingPath, int iteration,
			double testPerplexity, Logger logger) {
		String likelihoodFile;
		logger.info("Perplexity on testset is: " + testPerplexity);
		likelihoodFile = loggingPath + "/test_perplexity.txt";
		try(PrintWriter out = new PrintWriter(new BufferedWriter(
				new FileWriter(likelihoodFile, true)))) {
			out.println(iteration + "\t" + testPerplexity);
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("Could not write test perplexity file");
		}
	}

	public static void heldOutLLToFile(String loggingPath, int iteration,
			double testPerplexity, Logger logger) {
		String likelihoodFile;
		logger.info("Held out Loglikelihood on testset is: " + testPerplexity);
		likelihoodFile = loggingPath + "/test_held_out_log_likelihood.txt";
		try(PrintWriter out = new PrintWriter(new BufferedWriter(
				new FileWriter(likelihoodFile, true)))) {
			out.println(iteration + "\t" + testPerplexity);
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("Could not write test held out log likelihood file");
		}
	}
	
	public static void logLikelihoodToFile(double logLik, int iteration, 
			String wordsPerTopic, String loggingPath, Logger logger) {
		logger.info("\n<" + iteration + "> Log Likelihood: " + logLik + "\n" +wordsPerTopic);
		String likelihoodFile = loggingPath + "/likelihood.txt";
		try(PrintWriter out = new PrintWriter(new BufferedWriter(
				new FileWriter(likelihoodFile, true)))) {
			out.println(iteration + "\t" + logLik + "\t" + System.currentTimeMillis());
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}	
	
	public static void logLikelihoodToFile(LogState parameterObject) {
		String likelihoodFile = parameterObject.loggingPath + "/likelihood.txt";
		try(PrintWriter out = new PrintWriter(new BufferedWriter(
				new FileWriter(likelihoodFile, true)))) {
			out.println(parameterObject.iteration + "\t" + parameterObject.logLik);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}
	
	public static void logStatstHeaderToFile(Stats statsObject) {
		String header = "iteration\ttimestamp\tzTotalTime\tphiTotalTime\ttypeTokenDensity\tdocumentDensity";
		if(statsObject.zTimings!=null) {
			for (int i = 0; i < statsObject.zTimings.length; i++) {
				header += "\tz-" + i;
			}
		}
		if(statsObject.countTimings!=null) {
			for (int i = 0; i <  statsObject.countTimings.length; i++) {
				header += "\tcountUpdate-" + i;
			}
		}
		header += "\tphiDensity";
		if(statsObject.heldOutLL!=null) {
			header += "\theldOutLL";
		}
		String likelihoodFile = statsObject.loggingPath + "/stats.txt";
		try(PrintWriter out = new PrintWriter(new BufferedWriter(
				new FileWriter(likelihoodFile, true)))) {
			out.println(header);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}
	
	public static void logStatsToFile(Stats statsObject) {
		String likelihoodFile = statsObject.loggingPath + "/stats.txt";
		
		String logString = statsObject.iteration + "\t"
				+ statsObject.absoluteTime + "\t" + statsObject.zSamplingTokenUpdateTime + "\t" 
				+ statsObject.phiSamplingTime + "\t" + statsObject.density
				+ "\t" + statsObject.docDensity;
		
		if(statsObject.zTimings!=null) {
			for (int i = 0; i < statsObject.zTimings.length; i++) {
				logString += "\t" + statsObject.zTimings[i];
			}
		}
		if(statsObject.countTimings!=null) {
			for (int i = 0; i < statsObject.countTimings.length; i++) {
				logString += "\t" + statsObject.countTimings[i];
			}
		}
		logString += "\t" + statsObject.phiDensity;
		if(statsObject.heldOutLL != null) {
			logString += "\t" + statsObject.heldOutLL.doubleValue();
		}
		try(PrintWriter out = new PrintWriter(new BufferedWriter(
				new FileWriter(likelihoodFile, true)))) {
			out.println(logString);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}


	public static void writeBinaryDoubleMatrixRows(double[][] matrix,
			int iteration, int rows, int columns, String filename, int [] rowIndices)
					throws FileNotFoundException, IOException {
		String fn = String.format(filename + "_" + rows + "_" + columns + "_%05d.BINARY", iteration);
		try(RandomAccessFile outputPhiFile = new RandomAccessFile(fn, "rw")) {
			FileChannel phiChannel = outputPhiFile.getChannel();
			final int bufferSize = 8*columns*rows;
			ByteBuffer buf = phiChannel.map(FileChannel.MapMode.READ_WRITE, 0, bufferSize);
			for (int to = 0; to < rowIndices.length; to++) {
				for (int ty = 0; ty < columns; ty++) {
					buf.putDouble(matrix[rowIndices[to]][ty]);
				}
			}
		}
	}

	public static void writeBinaryIntMatrixRows(int[][] matrix,
			int iteration, int rows, int columns, String filename, int [] rowIndices)
					throws FileNotFoundException, IOException {
		String fn = String.format(filename + "_" + rows + "_" + columns + "_%05d.BINARY", iteration);
		try(RandomAccessFile outputPhiFile = new RandomAccessFile(fn, "rw")) {
			FileChannel phiChannel = outputPhiFile.getChannel();
			final int bufferSize = 8*columns*rows;
			ByteBuffer buf = phiChannel.map(FileChannel.MapMode.READ_WRITE, 0, bufferSize);
			for (int to = 0; to < rowIndices.length; to++) {
				for (int ty = 0; ty < columns; ty++) {
					buf.putInt(matrix[rowIndices[to]][ty]);
				}
			}
		}
	}

	public static void writeBinaryDoubleMatrixCols(double[][] matrix,
			int iteration, int rows, int columns, String filename, int [] colIndices)
					throws FileNotFoundException, IOException {
		String fn = String.format(filename + "_" + rows + "_" + columns + "_%05d.BINARY", iteration);
		try(RandomAccessFile outputPhiFile = new RandomAccessFile(fn, "rw")) {
			FileChannel phiChannel = outputPhiFile.getChannel();
			final int bufferSize = 8*columns*rows;
			ByteBuffer buf = phiChannel.map(FileChannel.MapMode.READ_WRITE, 0, bufferSize);
			for (int to = 0; to < rows; to++) {
				for (int ty = 0; ty < colIndices.length; ty++) {
					buf.putDouble(matrix[to][colIndices[ty]]);
				}
			}
		}
	}

	public static String writeBinaryDoubleMatrixIndices(double[][] matrix,
			int iteration, String filename, int [][] indices)
					throws FileNotFoundException, IOException {
		return writeBinaryDoubleMatrixIndices(matrix, iteration, indices.length, indices[0].length, filename, indices);
	}

	public static String writeBinaryDoubleMatrixIndices(double[][] matrix,
			int iteration, int rows, int columns, String filename, int [][] indices)
					throws FileNotFoundException, IOException {
		String fn = String.format(filename + "_" + rows + "_" + columns + "_%05d.BINARY", iteration);
		try(RandomAccessFile outputPhiFile = new RandomAccessFile(fn, "rw")) {
			FileChannel phiChannel = outputPhiFile.getChannel();
			final int bufferSize = 8*columns*rows;
			ByteBuffer buf = phiChannel.map(FileChannel.MapMode.READ_WRITE, 0, bufferSize);
			for (int to = 0; to < indices.length; to++) {
				for (int ty = 0; ty < indices[to].length; ty++) {
					buf.putDouble(matrix[to][indices[to][ty]]);
				}
			}
		}
		return fn;
	}

	public static void writeBinaryIntMatrixCols(int[][] matrix,
			int iteration, int rows, int columns, String filename, int [] colIndices)
					throws FileNotFoundException, IOException {
		String fn = String.format(filename + "_" + rows + "_" + columns + "_%05d.BINARY", iteration);
		try(RandomAccessFile outputPhiFile = new RandomAccessFile(fn, "rw")) {
			FileChannel phiChannel = outputPhiFile.getChannel();
			final int bufferSize = 8*columns*rows;
			ByteBuffer buf = phiChannel.map(FileChannel.MapMode.READ_WRITE, 0, bufferSize);
			for (int to = 0; to < rows; to++) {
				for (int ty = 0; ty < colIndices.length; ty++) {
					buf.putInt(matrix[to][colIndices[ty]]);
				}
			}
		}
	}
	
	public static void writeBinaryDoubleMatrix(double[][] matrix, int iteration, String filename)
					throws FileNotFoundException, IOException {
		writeBinaryDoubleMatrix(matrix, iteration, matrix.length, matrix[0].length, filename);
	}

	public static void writeBinaryDoubleMatrix(double[][] matrix,
			int iteration, int rows, int columns, String filename)
					throws FileNotFoundException, IOException {
		String fn = String.format(filename + "_" + rows + "_" + columns + "_%05d.BINARY", iteration);
		writeBinaryDoubleMatrix(matrix, rows, columns, fn);
	}

	public static void writeBinaryDoubleMatrix(double[][] matrix, int rows, int columns, String fn) throws IOException, FileNotFoundException {
		try(RandomAccessFile outputPhiFile = new RandomAccessFile(fn, "rw")) {
			FileChannel phiChannel = outputPhiFile.getChannel();
			final int bufferSize = 8*columns*rows;
			ByteBuffer buf = phiChannel.map(FileChannel.MapMode.READ_WRITE, 0, bufferSize);
			for (int to = 0; to < rows; to++) {
				for (int ty = 0; ty < columns; ty++) {
					buf.putDouble(matrix[to][ty]);
				}
			}
		}
	}

	public static void writeBinaryIntMatrix(int [][] matrix,
			int iteration, String filename) throws FileNotFoundException, IOException {
		writeBinaryIntMatrix(matrix, iteration, matrix.length, matrix[0].length, filename);
	}

	public static void writeBinaryIntMatrix(int [][] matrix,
			int iteration, int rows, int columns, String filename)
					throws FileNotFoundException, IOException {
		String fn = String.format(filename + "_" + rows + "_" + columns + "_%05d.BINARY", iteration);
		writeBinaryIntMatrix(matrix, rows, columns, fn);
	}

	public static void writeBinaryIntMatrix(int[][] matrix, int rows, int columns, String fn) throws FileNotFoundException, IOException {
		try(RandomAccessFile outputPhiFile = new RandomAccessFile(fn, "rw")) {
			FileChannel phiChannel = outputPhiFile.getChannel();
			final int bufferSize = 8*columns*rows;
			ByteBuffer buf = phiChannel.map(FileChannel.MapMode.READ_WRITE, 0, bufferSize);

			for (int to = 0; to < rows; to++) {
				for (int ty = 0; ty < columns; ty++) {
					buf.putInt(matrix[to][ty]);
				}
			}
		}
	}

	public static void writeASCIIIntMatrix(int[][] matrix, String fn, String sep) throws FileNotFoundException, IOException {
		File file = new File(fn);
		if (file.exists()) {
			System.out.println("Warning : the file " + file.getName()
					+ " already exists, overwriting...");
		}
		try (FileWriter fw = new FileWriter(file, false); 
				BufferedWriter bw = new BufferedWriter(fw);
				PrintWriter pw  = new PrintWriter(bw)) {
			for (int i = 0; i < matrix.length; i++) {
				for (int j = 0; j < matrix[i].length; j++) {
					pw.print(matrix[i][j] + "");
					if((j+1)<matrix[i].length) {
						pw.print(sep);
					}
				}
				pw.println();
			}
		} catch (IOException e) {
			throw new IllegalArgumentException("File " + file.getName()
					+ " is unwritable : " + e.toString());
		}
	}

	public static String formatDouble(double d, DecimalFormat mydecimalFormat) {
		return formatDouble(d, mydecimalFormat, 4);
	}
	
	public static String formatDouble(double d, DecimalFormat mydecimalFormat, int noDigits) {
		if ( d<0.0001 && d>0 || d > -0.0001 && d < 0) {
			return mydecimalFormat.format(d);
		} else {
			String formatString = "%." + noDigits + "f";
			return String.format(formatString, d);
		}
	}
	
	public static String formatDoubleMarkZero(double d, DecimalFormat mydecimalFormat, int noDigits) {
		if ( d == 0.0 ) return "<0.0>";
		if ( d<0.0001 && d>0 || d > -0.0001 && d < 0) {
			return mydecimalFormat.format(d);
		} else {
			String formatString = "%." + noDigits + "f";
			return String.format(formatString, d);
		}
	}


	public static void writeASCIIDoubleMatrix(double[][] matrix, String fn, String sep) throws FileNotFoundException, IOException {
		writeASCIIDoubleMatrix(matrix, matrix.length, matrix[0].length, fn, sep, 4);
	}

	public static void writeASCIIDoubleMatrix(double[][] matrix, int rows, int columns, String fn, String sep) throws FileNotFoundException, IOException {
		writeASCIIDoubleMatrix(matrix, rows, columns, fn, sep, 4);
	}

	public static void writeASCIIDoubleMatrix(double[][] matrix, int rows, int columns, String fn, String sep, int noDigits) throws FileNotFoundException, IOException {
		DecimalFormat mydecimalFormat = new DecimalFormat("00.###E0");
		File file = new File(fn);
		if (file.exists()) {
			System.out.println("Warning : the file " + file.getName()
					+ " already exists !");
		}
		try (FileWriter fw = new FileWriter(file, false); 
				BufferedWriter bw = new BufferedWriter(fw);
				PrintWriter pw  = new PrintWriter(bw)) {
			for (int i = 0; i < matrix.length; i++) {
				for (int j = 0; j < matrix[i].length; j++) {
					pw.print(formatDouble(matrix[i][j],mydecimalFormat,noDigits) + "");
					if((j+1)<matrix[i].length) {
						pw.print(sep);
					}
				}
				pw.println();
			}
		} catch (IOException e) {
			throw new IllegalArgumentException("File " + file.getName()
					+ " is unwritable : " + e.toString());
		}
	}
		
	public static int [][] readBinaryIntMatrix(int rows, int columns, String fn) throws IOException {
		File matrixFile = new File(fn);
		int [][] matrix = new int[rows][columns];
		try (DataInputStream dis =
				new DataInputStream(new BufferedInputStream(new FileInputStream(matrixFile.getAbsolutePath())))) {
			for (int i = 0; i < matrix.length; i++) {
				for (int j = 0; j < matrix[0].length; j++) {
					matrix[i][j] = dis.readInt();
				}
			}
		}
		return matrix;
	}
	
	public static int[][] readASCIIIntMatrix(String filename, String sep) throws IOException {
		List<List<Integer>> rows = new ArrayList<List<Integer>>();
		File file = new File(filename);
	    FileReader fr = new FileReader(file);
	    BufferedReader br = new BufferedReader(fr);
	    String line;
	    while((line = br.readLine()) != null){
	    	List<Integer> row = new ArrayList<Integer>();
	    	String [] ints = line.split(sep);
	    	for (int i = 0; i < ints.length; i++) {
				if(ints[i].trim().length()>0) {
					row.add(Integer.parseInt(ints[i]));
				}
			}
	    	rows.add(row);
	    }
	    br.close();
	    fr.close();
	    int [][] result = new int[rows.size()][];
	    for (int i = 0; i < rows.size(); i++) {
	    	List<Integer> row = rows.get(i);
	    	int [] irow = new int[row.size()];
	    	for (int j = 0; j < row.size(); j++) {
				irow[j] = row.get(j);
			}
	    	result[i] = irow;
	    }
		return result;
	}
	
	public static double[][] readASCIIDoubleMatrix(String filename, String sep) throws IOException {
		List<List<Double>> rows = new ArrayList<List<Double>>();
		File file = new File(filename);
	    FileReader fr = new FileReader(file);
	    BufferedReader br = new BufferedReader(fr);
	    String line;
	    while((line = br.readLine()) != null){
	    	List<Double> row = new ArrayList<Double>();
	    	String [] ints = line.split(sep);
	    	for (int i = 0; i < ints.length; i++) {
				if(ints[i].trim().length()>0) {
					row.add(Double.parseDouble(ints[i]));
				}
			}
	    	rows.add(row);
	    }
	    br.close();
	    fr.close();
	    double [][] result = new double[rows.size()][];
	    for (int i = 0; i < rows.size(); i++) {
	    	List<Double> row = rows.get(i);
	    	double [] irow = new double[row.size()];
	    	for (int j = 0; j < row.size(); j++) {
				irow[j] = row.get(j);
			}
	    	result[i] = irow;
	    }
		return result;
	}
	
	public static double[][] readBinaryDoubleMatrix(int rows, int columns, String fn) throws FileNotFoundException, IOException {
		File matrixFile = new File(fn);
		double [][] matrix = new double[rows][columns];
		try (DataInputStream dis =
				new DataInputStream(new BufferedInputStream(new FileInputStream(matrixFile.getAbsolutePath())))) {
			for (int i = 0; i < matrix.length; i++) {
				for (int j = 0; j < matrix[0].length; j++) {
					matrix[i][j] = dis.readDouble();
				}
			}
		}
		return matrix;
	}

	public static LabelAlphabet newLabelAlphabet (int numTopics) {
		LabelAlphabet ret = new LabelAlphabet();
		for (int i = 0; i < numTopics; i++)
			ret.lookupIndex("topic"+i);
		return ret;
	}

	/**
	 * This version takes a sparse (ala MALLET) type topic counts matrix
	 * 
	 *  Return an array of sorted sets (one set per topic). Each set 
	 *   contains IDSorter objects with integer keys into the alphabet.
	 *   To get direct access to the Strings, use getTopWords().
	 */
	public static ArrayList<TreeSet<IDSorter>> getSortedWordsSparse (int numTopics, int numTypes,
			int[][] typeTopicCounts) {

		ArrayList<TreeSet<IDSorter>> topicSortedWords = new ArrayList<TreeSet<IDSorter>>(numTopics);

		// Initialize the tree sets
		for (int topic = 0; topic < numTopics; topic++) {
			topicSortedWords.add(new TreeSet<IDSorter>());
		}

		int topicMask = numTopics - 1;
		int topicBits = Integer.bitCount(topicMask);

		// Collect counts
		for (int type = 0; type < numTypes; type++) {

			int[] topicCounts = typeTopicCounts[type];

			int index = 0;
			while (index < numTopics &&
					topicCounts[index] > 0) {

				int topic = topicCounts[index] & topicMask;
				int count = topicCounts[index] >> topicBits;

		topicSortedWords.get(topic).add(new IDSorter(type, count));

		index++;
			}
		}

		return topicSortedWords;
	}

	/**
	 * This version takes a dense (plain) type topic counts matrix
	 * 
	 *  Return an array of sorted sets (one set per topic). Each set 
	 *   contains IDSorter objects with integer keys into the alphabet.
	 *   To get direct access to the Strings, use getTopWords().
	 */
	public static ArrayList<TreeSet<IDSorter>> getSortedWordsDense (int numTopics, int numTypes, int[][] typeTopicCounts) {

		ArrayList<TreeSet<IDSorter>> topicSortedWords = new ArrayList<TreeSet<IDSorter>>(numTopics);

		// Initialize the tree sets
		for (int topic = 0; topic < numTopics; topic++) {
			topicSortedWords.add(new TreeSet<IDSorter>());
		}

		// Collect counts
		for (int type = 0; type < numTypes; type++) {

			int[] topicCounts = typeTopicCounts[type];

			int index = 0;
			while (index < numTopics) {

				int topic = index;
				int count = topicCounts[index];

				topicSortedWords.get(topic).add(new IDSorter(type, count));

				index++;
			}
		}

		return topicSortedWords;
	}

	
	public static String formatTopWordsAsCsv(String[][] topWords) {
		String result = "";
		for (int i = 0; i < topWords.length; i++) {
			for (int j = 0; j < topWords[i].length; j++) {
				result += topWords[i][j];
				if((j+1) < topWords[i].length) {
					result += ",";
				}
			}
			if((i+1) < topWords.length) {
				result += "\n";
			}
		}
		return result;
	}
	
	public static String formatTopWords(String[][] topWords) {
		String result = "";
		for (int i = 0; i < topWords.length; i++) {
			result += "Topic " + (i+1) + ": ";
			for (int j = 0; j < topWords[i].length; j++) {
				result += topWords[i][j];
				if((j+1) < topWords[i].length) {
					result += " ";
				}
			}
			if((i+1) < topWords.length) {
				result += "\n";
			}
		}
		return result;
	}

	public static String instancesToString(InstanceList instances) {
		return instancesToString(instances,-1);
	}

	public static String instancesToString(InstanceList instances, int noWords) {
		String result = "";
		for (Instance instance : instances) {
			result += instanceToString(instance, noWords);
			result += "\n";
		}
		return result;
	}

	public static String instanceToString(Instance instance) {
		return instanceToString(instance,-1);
	}

	public static String instanceToString(Instance instance, int noWords) {
		String result = "";
		Alphabet alphabet = instance.getAlphabet();
		FeatureSequence features = (FeatureSequence) instance.getData();
		noWords = (noWords > 0 ?  Math.min(noWords, features.size()) : features.size());
		if(noWords==0) {
			result += "<empty doc>";
		} else {
			for (int i = 0; i < noWords; i++) {
				result += alphabet.lookupObject(features.getIndexAtPosition(i));
				if(i+1<noWords) {
					result += ", ";
				}
			}
		}
		return result;
	}
	
	public static String instanceToTokenIndexString(Instance instance) {
		return instanceToTokenIndexString(instance,-1);
	}
	
	public static String instanceToTokenIndexString(Instance instance, int noWords) {
		String result = "";
		FeatureSequence features = (FeatureSequence) instance.getData();
		noWords = (noWords > 0 ?  Math.min(noWords, features.size()) : features.size());
		if(noWords==0) {
			result += "<empty doc>";
		} else {
			for (int i = 0; i < noWords; i++) {
				result += features.getIndexAtPosition(i);
				if(i+1<noWords) {
					result += ", ";
				}
			}
		}
		return result;
	}

	public static String instanceToSvmLightString(Instance instance, int noWords) {
		String result = "";
		FeatureSequence features = (FeatureSequence) instance.getData();
		noWords = (noWords > 0 ?  Math.min(noWords, features.size()) : features.size());
		if(noWords==0) {
			result += "0";
		} else {
			result += noWords + " ";
			for (int i = 0; i < noWords; i++) {
				result += features.getIndexAtPosition(i) + ":1";
				if(i<(noWords-1)) {
					result += " ";
				}
			}
		}
		return result;
	}
	
	public static String indicesToString(int [] indices, Alphabet alphabet) {
		String result = "";
		int noWords = indices.length;
		for (int i = 0; i < noWords; i++) {
			result += alphabet.lookupObject(indices[i]);
			if(i+1<noWords) {
				result += " ";
			}
		}
		return result;
	}

	public static int[][] getDocumentTopicCounts(ArrayList<TopicAssignment> data, int numTopics) {
		return getDocumentTopicCounts(data, numTopics, data.size());
	}

	public static int[][] getDocumentTopicCounts(ArrayList<TopicAssignment> data, int numTopics, int noDocs) { 
		int [][] docTopicCounts = new int [noDocs][numTopics];

		for (int doc = 0; doc < noDocs; doc++) {
			FeatureSequence tokenSequence =
					(FeatureSequence) data.get(doc).instance.getData();
			int [] topicSequence =
					data.get(doc).topicSequence.getFeatures();
			int docLength = tokenSequence.getFeatures().length;
			// Populate topic counts
			int[] localTopicCounts = new int[numTopics];
			for (int position = 0; position < docLength; position++) {
				localTopicCounts[topicSequence[position]]++;
			}
			for (int topic = 0; topic < localTopicCounts.length; topic++) {
				docTopicCounts[doc][topic] = localTopicCounts[topic];
			}
		}
		return docTopicCounts;	
	}

	/** Get the smoothed distribution over topics for a training instance. 
	 */
	public static double[] getTopicProbabilities(ArrayList<TopicAssignment>data, 
			int instanceID, int numTopics, double[] alpha) {
		LabelSequence topics = data.get(instanceID).topicSequence;
		return LDAUtils.getTopicProbabilities(topics, numTopics, alpha);
	}

	/** Get the smoothed distribution over topics for a topic sequence, 
	 * which may be from the training set or from a new instance with topics
	 * assigned by an inferencer.
	 */
	public static double[] getTopicProbabilities(LabelSequence topics, int numTopics, double[] alpha) {
		double[] topicDistribution = new double[numTopics];

		// Loop over the tokens in the document, counting the current topic
		//  assignments.
		for (int position = 0; position < topics.getLength(); position++) {
			topicDistribution[ topics.getIndexAtPosition(position) ]++;
		}

		// Add the smoothing parameters and normalize
		double sum = 0.0;
		for (int topic = 0; topic < numTopics; topic++) {
			topicDistribution[topic] += alpha[topic];
			sum += topicDistribution[topic];
		}

		// And normalize
		for (int topic = 0; topic < numTopics; topic++) {
			topicDistribution[topic] /= sum;
		}

		return topicDistribution;
	}

	/** Return a tool for estimating topic distributions for new documents */
	public static TopicInferencer getInferencer(ArrayList<TopicAssignment> data, 
			int[][] typeTopicCounts, int[] tokensPerTopic, double[] alpha, 
			double beta, double betaSum) {
		return new TopicInferencer(typeTopicCounts, tokensPerTopic,
				data.get(0).instance.getDataAlphabet(),
				alpha, beta, betaSum);
	}

	/**
	 * Returns true if index is within any of the ranges specified by rangePairs
	 * @param index
	 * @param rangePairs
	 * @return 
	 */
	public static boolean inRangeInterval(int index, int [] rangePairs) {
		if(rangePairs.length<2) throw new IllegalArgumentException("Range must be at least 2 long!");
		if(rangePairs.length%2!=0) throw new IllegalArgumentException("Range must contain an even number of pairs!");
		int idx = 0;
		while ( idx < rangePairs.length) {
			if(index>=rangePairs[idx++]&&index<=rangePairs[idx++]) return true;
		}
		return false;
	}

	/**
	 * Expects a square matrix
	 * @param countMatrix square matrix of integers
	 * @return square matrix with dirichlet draw based on countMatrix
	 */
	public static double [][] drawDirichlets(int[][] countMatrix) {
		double [][] dirDraws = new double[countMatrix.length][countMatrix[0].length];
		for (int i = 0; i < countMatrix.length; i++) {
			double [] ps = new double[countMatrix[i].length];
			for (int j = 0; j < countMatrix[i].length; j++) {
				ps[j] = countMatrix[i][j];
				if(countMatrix[i][j]==0) ps[j] = Double.MIN_NORMAL;
			}
			Dirichlet dir = new Dirichlet(ps);
			dirDraws[i] = dir.nextDistribution();
		}
		return dirDraws;
	}

	public static int[][] extractCounts(int[][] topicWordCnts,int[][] topIndices) {
		int [][] extractedCounts = new int [topIndices.length][topIndices[0].length];
		for (int i = 0; i < topIndices.length; i++) {
			for (int j = 0; j < topIndices[i].length; j++) {
				extractedCounts[i][j] = topicWordCnts[topIndices[i][j]][i];
			}
		}
		return extractedCounts;
	}

	public static double [][] transpose(double [][] matrix) {
		double [][] transpose = new double[matrix[0].length][matrix.length];
		for (int i = 0; i < matrix.length; i++) {
			for (int j = 0; j < matrix[0].length; j++) {
				transpose[j][i] = matrix [i][j];
			}
		}
		return transpose;
	}

	public static int [][] transpose(int [][] matrix) {
		int [][] transpose = new int[matrix[0].length][matrix.length];
		for (int i = 0; i < matrix.length; i++) {
			for (int j = 0; j < matrix[0].length; j++) {
				transpose[j][i] = matrix [i][j];
			}
		}
		return transpose;
	}

	public static void transpose(double[][] matrix, double [][] transpose) {
		int rows = matrix.length;
		int cols = matrix[0].length;
		for (int row = 0; row < rows; row++)
			for (int col = 0; col < cols; col++)
				transpose[col][row] = matrix[row][col];
	}
	
	public static double calculateMatrixDensity(int[][] matrix) {
		double nonZero = 0.0;
		for (int type = 0; type < matrix.length; type++) {
			for (int topic = 0; topic < matrix[type].length; topic++) {
				if(matrix[type][topic]!=0) {
					nonZero++;
				}
			}
		}
		return nonZero / (matrix.length * matrix[0].length);
	}

	public static double calculateDocDensity(double[] kdDensities, int numTopics, int numDocuments) {
		double totalKdens = 0.0;
		for (int i = 0; i < kdDensities.length; i++) {
			totalKdens += kdDensities[i];
		}
		return totalKdens / numTopics / numDocuments;
	}

	public static double calculatePhiDensity(double[][] phi) {
		int rows = phi.length;
		int cols = phi[0].length;
		double numZero = 0;
		for (int row = 0; row < rows; row++) {
			for (int col = 0; col < cols; col++) {
				if(phi[row][col]==0) {
					numZero++;
				}
			}
		}
		return numZero / (rows*cols);
	}

	public static List<String> loadDatasetAsString(String inputFile) throws FileNotFoundException {
		SimpleTokenizerLarge tokenizer = new SimpleTokenizerLarge(NumericAlsoTokenizer.USE_EMPTY_STOPLIST);
		String lineRegex = "^(\\S*)[\\s,]*([^\\t]+)[\\s,]*(.*)$";
		int dataGroup = 3;
		int labelGroup = 2;
		int nameGroup = 1; // data, label, name fields
		List<String> files = new ArrayList<>(); 

		CsvIterator reader = new CsvIterator(
				new FileReader(inputFile),
				lineRegex,
				dataGroup,
				labelGroup,
				nameGroup);

		ArrayList<Pipe> pipes = new ArrayList<Pipe>();
		Target2Label ttl = new Target2Label ();
		
		pipes.add(tokenizer);
		//pipes.add(sl2fs);
		pipes.add(ttl);

		Pipe serialPipe = new SerialPipes(pipes);

		InstanceList instances = new InstanceList(serialPipe);
		instances.addThruPipe(reader);
		for (Instance instance : instances) {
			//FeatureSequence features = (FeatureSequence) instance.getData();
			@SuppressWarnings("unchecked")
			List<String> text = (List<String>) instance.getData();
			String result = "";
			for (String string : text) {
				result += string + " ";
			}
//			for (int i = 0; i < features.size(); i++) {
//				result += alphabet.lookupObject(features.getIndexAtPosition(i)) + " ";
//			}
			files.add(result);
		}
		return files;	
		}

	public static String instanceLabelToString(Instance instance, LabelAlphabet labelAlphabet) {
		String label  = instance.getLabeling().getBestLabel().toString();
		return label;
	}

	public static String[] extractVocabulaty(Alphabet dataAlphabet) {
		String [] vocab = new String[dataAlphabet.size()];
		for (int i = 0; i < vocab.length; i++) {
			vocab[i] = dataAlphabet.lookupObject(i).toString();
		}
		return vocab;
	}

	public static void writeStringArray(String[] vocabulary, String fileName) {
		File file = new File(fileName);
		try (FileWriter fw = new FileWriter(file, false); 
				BufferedWriter bw = new BufferedWriter(fw);
				PrintWriter pw  = new PrintWriter(bw)) {
			for (int i = 0; i < vocabulary.length; i++) {
				pw.println(vocabulary[i]);
			}
		} catch (IOException e) {
			throw new IllegalArgumentException("File " + file.getName()
					+ " is unwritable : " + e.toString());
		}
	}

	public static void writeString(String string, String fileName) {
		File file = new File(fileName);
		try (FileWriter fw = new FileWriter(file, false); 
				BufferedWriter bw = new BufferedWriter(fw);
				PrintWriter pw  = new PrintWriter(bw)) {
			pw.println(string);
		} catch (IOException e) {
			throw new IllegalArgumentException("File " + file.getName()
					+ " is unwritable : " + e.toString());
		}
	}
	
	public static String toRowVectorString(int[] a) {
        if (a == null)
            return "null";
        int iMax = a.length - 1;
        if (iMax == -1)
            return "";

        StringBuilder b = new StringBuilder();
        for (int i = 0; ; i++) {
            b.append(a[i]);
            if (i == iMax)
                return b.toString();
            b.append(", ");
        }
    }
	
	public static void writeIntRowArray(int[] iarr, String fileName) {
		File file = new File(fileName);
		try (FileWriter fw = new FileWriter(file, true); 
				BufferedWriter bw = new BufferedWriter(fw);
				PrintWriter pw  = new PrintWriter(bw)) {
			pw.println(toRowVectorString(iarr));
		} catch (IOException e) {
			throw new IllegalArgumentException("File " + file.getName()
					+ " is unwritable : " + e.toString());
		}
	}
	
	public static void writeIntArray(int[] iarr, String fileName) {
		File file = new File(fileName);
		try (FileWriter fw = new FileWriter(file, false); 
				BufferedWriter bw = new BufferedWriter(fw);
				PrintWriter pw  = new PrintWriter(bw)) {
			for (int i = 0; i < iarr.length; i++) {
				pw.println(iarr[i]);
			}
		} catch (IOException e) {
			throw new IllegalArgumentException("File " + file.getName()
					+ " is unwritable : " + e.toString());
		}
	}

	
	public static int[] extractTermCounts(InstanceList instances) {
		int [] termCounts = new int[instances.getDataAlphabet().size()];
		for (int i = 0; i < instances.size(); i++) {
			Instance inst = instances.get(i);
			FeatureSequence tokenSequence =	(FeatureSequence) inst.getData();
			int [] tokens = tokenSequence.getFeatures();
			for (int j = 0; j < tokens.length; j++) {
				termCounts[tokens[j]]++;
			}
		}
		return termCounts;
	}

	public static int[] extractDocLength(InstanceList instances) {
		int [] docLens = new int[instances.size()];
		for (int i = 0; i < docLens.length; i++) {
			Instance inst = instances.get(i);
			FeatureSequence tokenSequence =	(FeatureSequence) inst.getData();
			docLens[i] = tokenSequence.size();
		}
		return docLens;
	}
	
	public static InstanceList loadInstanceDirectory(String directory, String fileRegex, String stoplistFile,
			Integer rareThreshold, boolean keepNumbers, int maxDocumentBufferSize, boolean keepConnectors, Alphabet alphabet) {
			return loadInstanceDirectories(new String[] {directory}, fileRegex, stoplistFile, rareThreshold,
				 keepNumbers, maxDocumentBufferSize, keepConnectors, alphabet);
	}
	
	public static InstanceList loadInstanceDirectories(String [] directories, final String fileRegex, String stoplistFile, Integer keepCount,
			boolean keepNumbers, int maxBufSize, boolean keepConnectors, Alphabet dataAlphabet) {
		return loadInstanceDirectories(directories, fileRegex, stoplistFile, keepCount,
				keepNumbers, maxBufSize, keepConnectors, dataAlphabet, null);
	}

	public static InstanceList loadInstanceDirectories(String [] directories, final String fileRegex, String stoplistFile, Integer keepCount,
			boolean keepNumbers, int maxBufSize, boolean keepConnectors, Alphabet dataAlphabet, LabelAlphabet targetAlphabet) {
		
		File [] fdirectories = new File[directories.length];
		for (int i = 0; i < fdirectories.length; i++) {
			fdirectories[i] = new File(directories[i]);
		}
		
		SimpleTokenizerLarge tokenizer;

		tokenizer = initTokenizer(stoplistFile, keepNumbers, maxBufSize, keepConnectors);

		if (keepCount > 0) {
			FileIterator iterator = new FileIterator(fdirectories,
                    new FileFilter() {
						@Override
						public boolean accept(File pathname) {
							return pathname.toString().matches(fileRegex);
						}
					},
                    FileIterator.LAST_DIRECTORY);

			Alphabet alphabet = null;
			if(dataAlphabet==null) {
				alphabet = new Alphabet();
			} else {
				alphabet = dataAlphabet;
			}

			TokenSequence2FeatureSequence sl2fs = new TokenSequence2FeatureSequence(alphabet);
			TfIdfPipe tfIdfPipe = new TfIdfPipe(alphabet, null);

			ArrayList<Pipe> pipes = new ArrayList<Pipe>();
			pipes.add(new Input2CharSequence("UTF-8"));
			
			Pattern tokenPattern =
		            Pattern.compile("[\\p{L}\\p{N}_]+");

			pipes.add(new CharSequence2TokenSequence(tokenPattern));
			pipes.add(new TokenSequenceLowercase());
			
			TokenSequenceRemoveStopwords stopwordFilter =
					new TokenSequenceRemoveStopwords(new File(stoplistFile),
													 Charset.defaultCharset().displayName(),
													 false, // don't include default list
													 false,
													 false);
			pipes.add(stopwordFilter);
			
			TokenSequencePredicateMatcher reMatchPipe = new TokenSequencePredicateMatcher(new TokenSequencePredicateMatcher.Predicate<String>() {
				@Override
				public boolean test(String query) {
					return !query.matches(".*(--+|__+).*");
				}
			});
			pipes.add(reMatchPipe);
			
			pipes.add(sl2fs);
			if (keepCount > 0) {
				pipes.add(tfIdfPipe);
			}

			Pipe serialPipe = new SerialPipes(pipes);

			Iterator<Instance> iiterator = serialPipe.newIteratorFrom(iterator);

			int count = 0;

			// We aren't really interested in the instance itself,
			//  just the total feature counts.
			while (iiterator.hasNext()) {
				count++;
				if (count % 100000 == 0) {
					System.out.println(count);
				}
				iiterator.next();
			}

			if (keepCount > 0) {
				tfIdfPipe.addPrunedWordsToStoplist(tokenizer, keepCount);
			}
		}

		FileIterator iterator = new FileIterator(fdirectories,
				new FileFilter() {
					@Override
					public boolean accept(File pathname) {
						return pathname.toString().matches(fileRegex);
					}
				},
				FileIterator.LAST_DIRECTORY);

		ArrayList<Pipe> pipes = new ArrayList<Pipe>();
		Alphabet alphabet = null;
		if(dataAlphabet==null) {
			alphabet = new Alphabet();
		} else {
			alphabet = dataAlphabet;
		}

		TokenSequence2FeatureSequence sl2fs = new TokenSequence2FeatureSequence(alphabet);

		pipes.add(new Input2CharSequence("UTF-8"));
		
		Pattern tokenPattern =
	            Pattern.compile("[\\p{L}\\p{N}_]+");

		pipes.add(new CharSequence2TokenSequence(tokenPattern));
		pipes.add(new TokenSequenceLowercase());
		TokenSequenceRemoveStopwords stopwordFilter =
				new TokenSequenceRemoveStopwords(new File(stoplistFile),
												 Charset.defaultCharset().displayName(),
												 false, // don't include default list
												 false,
												 false);
		pipes.add(stopwordFilter);
		
		TokenSequencePredicateMatcher reMatchPipe = new TokenSequencePredicateMatcher(new TokenSequencePredicateMatcher.Predicate<String>() {
			@Override
			public boolean test(String query) {
				return !query.matches(".*(--+|__+).*");
			}
		});
		pipes.add(reMatchPipe);
		
		pipes.add(sl2fs);

		LabelAlphabet tAlphabet = null;
		if(targetAlphabet==null) {
			tAlphabet = new LabelAlphabet();
		} else {
			tAlphabet = targetAlphabet;
		}

		Target2Label ttl = new Target2Label (tAlphabet);

		pipes.add(ttl);

		Pipe serialPipe = new SerialPipes(pipes);

		InstanceList instances = new InstanceList(serialPipe);
		instances.addThruPipe(iterator);

		return instances;	
	}

	public static int[][] extractCorpus(InstanceList instances) {
		int[][] corpus = new int[instances.size()][];
		for (int i = 0; i < corpus.length; i++) {
			Instance inst = instances.get(i);
			FeatureSequence tokenSequence =
					(FeatureSequence) inst.getData();
			corpus[i] = tokenSequence.getFeatures();
		}
		return corpus;
	}

	public static List<String> findCommonWords(Instance testInstance, Instance closestTrain) {
		Set<String>  resultTrain = new HashSet<>();
		Set<String>  resultTest = new HashSet<>();
		Alphabet alphabet = testInstance.getAlphabet();
		FeatureSequence features = (FeatureSequence) testInstance.getData();
		FeatureSequence trainFeatures = (FeatureSequence) closestTrain.getData();
		int noWords = Math.max(features.size(),trainFeatures.size());
		for (int i = 0; i < noWords; i++) {
			if(i<features.size()) {
				resultTest.add((String)alphabet.lookupObject(features.getIndexAtPosition(i)));
			}
			if(i<trainFeatures.size()) {
				resultTrain.add((String)alphabet.lookupObject(trainFeatures.getIndexAtPosition(i)));
			}
		}
		
		resultTrain.retainAll(resultTest);
		
		return resultTrain.stream().collect(Collectors.toList());
	}
}
