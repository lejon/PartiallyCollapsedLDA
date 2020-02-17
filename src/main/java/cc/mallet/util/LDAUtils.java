package cc.mallet.util;

import static java.lang.Math.log;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipFile;

import com.google.common.io.ByteStreams;

import cc.mallet.configuration.LDAConfiguration;
import cc.mallet.configuration.ModelFactory;
import cc.mallet.configuration.ParsedLDAConfiguration;
import cc.mallet.pipe.CharSequence2TokenSequence;
import cc.mallet.pipe.CharSequenceLowercase;
import cc.mallet.pipe.FeatureCountPipe;
import cc.mallet.pipe.Input2CharSequence;
import cc.mallet.pipe.KeepConnectorPunctuationNumericAlsoTokenizer;
import cc.mallet.pipe.KeepConnectorPunctuationTokenizerLarge;
import cc.mallet.pipe.NumericAlsoTokenizer;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.RawTokenizer;
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
import cc.mallet.topics.LDAGibbsSampler;
import cc.mallet.topics.LDASamplerWithPhi;
import cc.mallet.topics.LogState;
import cc.mallet.topics.PolyaUrnSpaliasLDA;
import cc.mallet.topics.SpaliasUncollapsedParallelLDA;
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

	private static final String SAVED_SIMILARITY_SAMPLERNAME_PREFIX = "saved_lda_sampler";

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
		return buildSerialPipe(stoplistFile, dataAlphabet, null, false);
	}

	public static Pipe buildSerialPipe(String stoplistFile, Alphabet dataAlphabet, boolean raw) {
		return buildSerialPipe(stoplistFile, dataAlphabet, null, raw);
	}

	public static Pipe buildSerialPipe(String stoplistFile, Alphabet dataAlphabet, LabelAlphabet targetAlphabet, boolean raw) { 		
		int maxBufSize = 10000;
		Pipe tokenizer = null;
		if(raw) {
			if(stoplistFile==null) {
				tokenizer = new RawTokenizer(new HashSet<String>(), maxBufSize);
			} else {
				tokenizer = new RawTokenizer(new File(stoplistFile), maxBufSize);
			}
		} else {
			if(stoplistFile==null) {
				tokenizer = new SimpleTokenizerLarge(new HashSet<String>(), maxBufSize);
			} else {
				tokenizer = new SimpleTokenizerLarge(new File(stoplistFile), maxBufSize);
			}
		}

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

		if(!raw) pipes.add(csl);
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
		return loadDataset(config, dataset_fn, alphabet, null);
	}

	public static InstanceList loadDataset(LDAConfiguration config, String dataset_fn, Alphabet alphabet, LabelAlphabet targetAlphabet) throws FileNotFoundException {
		InstanceList instances;

		if(config.noPreprocess()) {
			instances = LDAUtils.loadInstancesRaw(dataset_fn, 
					config.getStoplistFilename("stoplist.txt"), 
					config.getTfIdfVocabSize(LDAConfiguration.TF_IDF_VOCAB_SIZE_DEFAULT));
		} else {

			File dsf = new File(dataset_fn); 
			if(dsf.isDirectory()) {
				instances = loadFromDir(config, dataset_fn, alphabet);
			} else {
				if(config.getTfIdfVocabSize(LDAConfiguration.TF_IDF_VOCAB_SIZE_DEFAULT)>0) {
					instances = LDAUtils.loadInstancesKeep(
							dataset_fn, 
							config.getStoplistFilename("stoplist.txt"), 
							config.getTfIdfVocabSize(LDAConfiguration.TF_IDF_VOCAB_SIZE_DEFAULT), 
							config.keepNumbers(), 
							config.getMaxDocumentBufferSize(LDAConfiguration.MAX_DOC_BUFFFER_SIZE_DEFAULT), 
							config.getKeepConnectingPunctuation(LDAConfiguration.KEEP_CONNECTING_PUNCTUATION), 
							alphabet,
							targetAlphabet);					
				} else {					
					instances = LDAUtils.loadInstancesPrune(
							dataset_fn, 
							config.getStoplistFilename("stoplist.txt"), 
							config.getRareThreshold(LDAConfiguration.RARE_WORD_THRESHOLD), 
							config.keepNumbers(), 
							config.getMaxDocumentBufferSize(LDAConfiguration.MAX_DOC_BUFFFER_SIZE_DEFAULT), 
							config.getKeepConnectingPunctuation(LDAConfiguration.KEEP_CONNECTING_PUNCTUATION), 
							alphabet,
							targetAlphabet);
				}
			}
		}
		return instances;
	}

	static InstanceList loadFromDir(LDAConfiguration config, String dataset_fn, Alphabet alphabet) {
		InstanceList instances;
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

		try (BufferedInputStream in = new BufferedInputStream(streamFromFile(inputFile))) {
			in.mark(Integer.MAX_VALUE);
			return loadInstancesPrune(in, stoplistFile, pruneCount, keepNumbers, 
					maxBufSize, keepConnectors, dataAlphabet, targetAlphabet);
		} catch (IOException e) {
			throw new IllegalArgumentException(e);
		}
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
	public static InstanceList loadInstancesPrune(BufferedInputStream in, String stoplistFile, int pruneCount, boolean keepNumbers, 
			int maxBufSize, boolean keepConnectors, Alphabet dataAlphabet, LabelAlphabet targetAlphabet) throws FileNotFoundException {
		SimpleTokenizerLarge tokenizer;
		String lineRegex = "^(\\S*)[\\s,]*([^\\t]+)[\\s,]*(.*)$";
		int dataGroup = 3;
		int labelGroup = 2;
		int nameGroup = 1; // data, label, name fields

		in.mark(Integer.MAX_VALUE);

		tokenizer = initTokenizer(stoplistFile, keepNumbers, maxBufSize, keepConnectors);

		if (pruneCount > 0) {
			CsvIterator reader = new CsvIterator(
					new InputStreamReader(in),
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

		// Now we reset the BufferedInput stream so we don't have to read from disk again.
		try {
			in.reset();
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}

		CsvIterator reader = new CsvIterator(
				new InputStreamReader(in),
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

	public static InstanceList loadInstancesRaw(String inputFile, String stoplistFile, int keepCount) throws FileNotFoundException {
		return loadInstancesRaw(inputFile, stoplistFile, keepCount, LDAConfiguration.MAX_DOC_BUFFFER_SIZE_DEFAULT, null, null);
	}

	public static InstanceList loadInstancesRaw(String inputFile, String stoplistFile, int keepCount, int maxBufSize) throws FileNotFoundException {
		return loadInstancesRaw(inputFile, stoplistFile, keepCount, maxBufSize, null, null);
	}

	public static InstanceList loadInstancesRaw(String inputFile, String stoplistFile, int maxBufSize, int keepCount, Alphabet dataAlphabet) throws FileNotFoundException {
		return loadInstancesRaw(inputFile, stoplistFile, keepCount, maxBufSize, dataAlphabet, null);
	}

	public static InputStream streamFromFile(String inputFile) throws FileNotFoundException, IOException {
		InputStream in = null;
		if(inputFile.toLowerCase().endsWith(".gz")) {
			in = new GZIPInputStream(new FileInputStream(inputFile));
			byte[] buffer = ByteStreams.toByteArray(in);
			ByteArrayInputStream sout = new ByteArrayInputStream(buffer);
			return sout;
		} else if(inputFile.toLowerCase().endsWith(".zip")) {
			ZipFile zf = new ZipFile(inputFile);
			try {
				String nameWithoutDotZip = inputFile.substring(0, inputFile.length() - ".zip".length());
				String shortNameWithoutDotZip = ((new File(nameWithoutDotZip)).getName());
				in = zf.getInputStream(zf.getEntry(shortNameWithoutDotZip));
				byte[] buffer = ByteStreams.toByteArray(in);
				zf.close();
				ByteArrayInputStream sout = new ByteArrayInputStream(buffer);
				return sout;
			} finally {
				zf.close();
			}
		} else {
			FileInputStream fs = new FileInputStream(new File(inputFile));
			byte[] buffer = ByteStreams.toByteArray(fs);
			ByteArrayInputStream sout = new ByteArrayInputStream(buffer);
			return sout;
		}
	}

	public static InstanceList loadInstancesRaw(String inputFile, String stoplistFile, int keepCount, int maxBufSize, 
			Alphabet dataAlphabet, LabelAlphabet targetAlphabet) throws FileNotFoundException {		
		try (BufferedInputStream in = new BufferedInputStream(streamFromFile(inputFile))){
			in.mark(Integer.MAX_VALUE);
			return loadInstancesRaw(in, stoplistFile, keepCount, maxBufSize, dataAlphabet, targetAlphabet);
		} catch (IOException e) {
			throw new IllegalArgumentException(e);
		}
	}


	/**
	 * Loads instances and keeps the <code>keepCount</code> number of words with 
	 * the highest TF-IDF. Does no preprocessing of the input other than splitting
	 * on \s
	 * 
	 * @param in BufferedInputStream to read data from
	 * @param stoplistFile File with stopwords, one per line
	 * @param keepCount The number of words to keep (based on TF-IDF)
	 * @param keepNumbers Boolean flag to signal to keep numbers or not
	 * @param keepConnectors Keep connectors. General category "Pc" in the Unicode specification.
	 * @param dataAlphabet And optional (null else) data alphabet to use (typically used when loading a test set)
	 * @return An InstanceList with the data in the input file
	 * @throws FileNotFoundException
	 */
	public static InstanceList loadInstancesRaw(BufferedInputStream in, String stoplistFile, int keepCount, int maxBufSize, 
			Alphabet dataAlphabet, LabelAlphabet targetAlphabet) throws FileNotFoundException {
		RawTokenizer tokenizer;
		String lineRegex = "^(\\S*)[\\s,]*([^\\t]+)[\\s,]*(.*)$";
		int dataGroup = 3;
		int labelGroup = 2;
		int nameGroup = 1; // data, label, name fields

		in.mark(Integer.MAX_VALUE);

		tokenizer = initRawTokenizer(stoplistFile, maxBufSize);

		if (keepCount > 0) {
			CsvIterator reader = new CsvIterator(
					new InputStreamReader(in),
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

		// Now we reset the BufferedInput stream so we don't have to read from disk again.
		try {
			in.reset();
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}

		CsvIterator reader = new CsvIterator(
				new InputStreamReader(in),
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


	public static InstanceList loadInstancesKeep(String inputFile, String stoplistFile, int keepCount, boolean keepNumbers, 
			int maxBufSize, boolean keepConnectors, Alphabet dataAlphabet, LabelAlphabet targetAlphabet) throws FileNotFoundException {		
		try (BufferedInputStream in = new BufferedInputStream(streamFromFile(inputFile))){
			in.mark(Integer.MAX_VALUE);
			return loadInstancesKeep(in, stoplistFile, keepCount, keepNumbers, 
					maxBufSize, keepConnectors, dataAlphabet, targetAlphabet);

		} catch (IOException e) {
			throw new IllegalArgumentException(e);
		}
	}

	/**
	 * Loads instances and keeps the <code>keepCount</code> number of words with 
	 * the highest TF-IDF
	 * 
	 * @param in BufferedInputStream to read data from
	 * @param stoplistFile File with stopwords, one per line
	 * @param keepCount The number of words to keep (based on TF-IDF)
	 * @param keepNumbers Boolean flag to signal to keep numbers or not
	 * @param keepConnectors Keep connectors. General category "Pc" in the Unicode specification.
	 * @param dataAlphabet And optional (null else) data alphabet to use (typically used when loading a test set)
	 * @return An InstanceList with the data in the input file
	 * @throws FileNotFoundException
	 */
	public static InstanceList loadInstancesKeep(BufferedInputStream in, String stoplistFile, int keepCount, boolean keepNumbers, 
			int maxBufSize, boolean keepConnectors, Alphabet dataAlphabet, LabelAlphabet targetAlphabet) throws FileNotFoundException {
		SimpleTokenizerLarge tokenizer;
		String lineRegex = "^(\\S*)[\\s,]*([^\\t]+)[\\s,]*(.*)$";
		int dataGroup = 3;
		int labelGroup = 2;
		int nameGroup = 1; // data, label, name fields

		in.mark(Integer.MAX_VALUE);
		tokenizer = initTokenizer(stoplistFile, keepNumbers, maxBufSize, keepConnectors);

		if (keepCount > 0) {
			CsvIterator reader = new CsvIterator(
					new InputStreamReader(in),
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

		// Now we reset the BufferedInput stream so we don't have to read from disk again.
		try {
			in.reset();
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}

		CsvIterator reader = new CsvIterator(
				new InputStreamReader(in),
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
	 * This is ugly as hell, but I wanted it to be as similar as
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

		if (keepCount > 0) {
			tokenizer = initTokenizer(stoplistFile, keepNumbers, maxBufSize, keepConnectors);
			try (BufferedInputStream in = new BufferedInputStream(streamFromFile(inputFile))) {
				CsvIterator reader = new CsvIterator(
						new InputStreamReader(in),
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
			} catch (IOException e) {
				throw new IllegalArgumentException(e);
			}

		} else {
			return null;
		}
		return null;
	}

	static RawTokenizer initRawTokenizer(String stoplistFile, int maxBuffSize) {
		return new RawTokenizer(new File(stoplistFile),maxBuffSize);
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
		StringBuilder result = new StringBuilder("");
		for (Instance instance : instances) {
			result.append(instanceToString(instance, noWords));
			result.append("\n");
		}
		return result.toString();
	}

	public static String instanceToString(Instance instance) {
		return instanceToString(instance,-1);
	}

	public static String instanceToString(Instance instance, int noWords) {
		StringBuilder result = new StringBuilder("");
		Alphabet alphabet = instance.getAlphabet();
		FeatureSequence features = (FeatureSequence) instance.getData();
		noWords = (noWords > 0 ?  Math.min(noWords, features.size()) : features.size());
		if(noWords==0) {
			result.append("<empty doc>");
		} else {
			for (int i = 0; i < noWords; i++) {
				result.append(alphabet.lookupObject(features.getIndexAtPosition(i)));
				if(i+1<noWords) {
					result.append(", ");
				}
			}
		}
		return result.toString();
	}

	public static String instanceToTokenIndexString(Instance instance) {
		return instanceToTokenIndexString(instance,-1);
	}

	public static String instanceToTokenIndexString(Instance instance, int noWords) {
		StringBuilder result = new StringBuilder("");
		FeatureSequence features = (FeatureSequence) instance.getData();
		noWords = (noWords > 0 ?  Math.min(noWords, features.size()) : features.size());
		if(noWords==0) {
			result.append("<empty doc>");
		} else {
			for (int i = 0; i < noWords; i++) {
				result.append(features.getIndexAtPosition(i));
				if(i+1<noWords) {
					result.append(", ");
				}
			}
		}
		return result.toString();
	}

	public static int [] instanceToTokenIndices(Instance instance) {
		FeatureSequence features = (FeatureSequence) instance.getData();
		int [] result = new int [features.size()];
		for (int i = 0; i < result.length; i++) {
			result[i] = features.getIndexAtPosition(i);
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
		StringBuilder result = new StringBuilder("");
		int noWords = indices.length;
		for (int i = 0; i < noWords; i++) {
			result.append(alphabet.lookupObject(indices[i]));
			if(i+1<noWords) {
				result.append(" ");
			}
		}
		return result.toString();
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

	public static String instanceLabelToString(Instance instance) {
		String label  = instance.getLabeling().getBestLabel().toString();
		return label;
	}

	public static String instanceIdToString(Instance instance) {
		String label  = instance.getName().toString();
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

	public static InstanceList loadInstanceDirectory(String directory, String fileRegex, String stoplistFile,
			Integer rareThreshold, boolean keepNumbers, int maxDocumentBufferSize, boolean keepConnectors, 
			Alphabet alphabet, LabelAlphabet targetAlphabet) {
		return loadInstanceDirectories(new String[] {directory}, fileRegex, stoplistFile, rareThreshold,
				keepNumbers, maxDocumentBufferSize, keepConnectors, alphabet, targetAlphabet);
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

	/**
	 * NOTE: In the special case where a document is 1 token long, the below
	 * method still returns and array that is 2 long (this is part of how MALLET works)
	 * 
	 * @param instance
	 * @return
	 */
	public static int[] getWordTokens(Instance instance) {
		return ((FeatureSequence) instance.getData()).getFeatures();
	}

	public static InstanceList loadInstancesStrings(String [] doclines, Pipe pipe) {
		return loadInstancesStrings(doclines, "X", "stoplist.txt", pipe);
	}

	public static InstanceList loadInstancesStrings(String [] doclines) {
		return loadInstancesStrings(doclines, "X", "stoplist.txt");
	}

	public static InstanceList loadInstancesStrings(String [] doclines, String className) {
		return loadInstancesStrings(doclines, className, "stoplist.txt");
	}

	public static InstanceList loadInstancesStrings(String [] doclines, String className, String stoplistFile) {
		return loadInstancesStrings(doclines, className, stoplistFile, null);
	}

	public static InstanceList loadInstancesStrings(String [] doclines, String className, String stoplistFile, Pipe pipe) {
		StringClassArrayIterator readerTrain = new StringClassArrayIterator(doclines, className); 

		if(pipe == null) {
			pipe = LDAUtils.buildSerialPipe(stoplistFile,null);
		}

		InstanceList instances = new InstanceList(pipe);
		instances.addThruPipe(readerTrain);
		return instances;
	}

	public static LDASamplerWithPhi loadStoredSampler(LDAConfiguration config, String saveDir) {
		String configHash = getConfigSetHash(config);
		if(!saveDir.endsWith(File.separator)) saveDir = saveDir + File.separator;
		String samplerFn = saveDir + buildSamplerSaveFilename(configHash);
		String storedConfigHash = readStoredTrainingsetHash(saveDir + SAVED_SIMILARITY_SAMPLERNAME_PREFIX + "-config_hash-" + configHash);
		File storedSampler = new File(samplerFn);
		LDASamplerWithPhi trainedSampler = null;
		if(storedSampler.exists() && configHash.equals(storedConfigHash)) {
			try {
				System.out.println("Using pretrained sampler @:" + storedSampler.getAbsolutePath());
				LDASamplerWithPhi tmp = (LDASamplerWithPhi) ModelFactory.get(config);
				if(tmp instanceof PolyaUrnSpaliasLDA) {
					System.out.println("Loading PolyaUrn sampler...");
					trainedSampler = PolyaUrnSpaliasLDA.read(storedSampler);
				} else if (tmp instanceof SpaliasUncollapsedParallelLDA) {
					System.out.println("Loading Spalias sampler...");
					trainedSampler = SpaliasUncollapsedParallelLDA.read(storedSampler);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			System.out.println("No stored samplers found for hash: " + configHash);
		}
		return trainedSampler;
	}

	static String buildSamplerSaveFilename(String configHash) {
		return SAVED_SIMILARITY_SAMPLERNAME_PREFIX + "-" + configHash + ".ser";
	}

	public static void saveSampler(LDAGibbsSampler trainedSampler, LDAConfiguration config, String saveDir) {
		String configHash = getConfigSetHash(config);
		if(!saveDir.endsWith(File.separator)) saveDir = saveDir + File.separator;
		String samplerFn = saveDir + buildSamplerSaveFilename(configHash);
		File storedSampler = new File(samplerFn);
		File tmpDir = new File(saveDir);
		if(!tmpDir.exists()) {
			tmpDir.mkdir();
		}
		if(trainedSampler instanceof PolyaUrnSpaliasLDA) {
			System.out.println("Storing PolyaUrn sampler (hash="+configHash+")...");
			((PolyaUrnSpaliasLDA) trainedSampler).write(storedSampler);
		} else if (trainedSampler instanceof SpaliasUncollapsedParallelLDA) {
			System.out.println("Storing SpaliasUncollapsedParallelLDA sampler (hash="+configHash+")...");
			((SpaliasUncollapsedParallelLDA) trainedSampler).write(storedSampler);
		}
		writeHash(configHash,saveDir + SAVED_SIMILARITY_SAMPLERNAME_PREFIX + "-config_hash-" + configHash);
	}

	static String getConfigSetHash(LDAConfiguration config) {
		if(config instanceof ParsedLDAConfiguration) {
			return getParsedConfigSetHash(config);
		} else {
			return getSimpleConfigHash(config);
		}
	}

	static String getSimpleConfigHash(LDAConfiguration config) {
		return config.hashCode() + "";
	}

	static String getParsedConfigSetHash(LDAConfiguration config) {
		String configFn = config.whereAmI();

		byte[] bytes = null;
		try {
			bytes = Files.readAllBytes(Paths.get(configFn));
		} catch (IOException e) {
			e.printStackTrace();
		}

		String hashtext = hashFrombytes(bytes); 
		return hashtext; 
	}

	private static String hashFrombytes(byte[] bytes) {
		// Static getInstance method is called with hashing MD5 
		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} 

		// digest() method is called to calculate message digest 
		// of an input digest() return array of byte 
		byte[] messageDigest = md.digest(bytes); 

		// Convert byte array into signum representation 
		BigInteger no = new BigInteger(1, messageDigest);

		// Convert message digest into hex value 
		String hashtext = no.toString(16); 
		while (hashtext.length() < 32) { 
			hashtext = "0" + hashtext; 
		}
		return hashtext;
	} 

	static void writeHash(String hash, String string) {
		try {
			Files.write(Paths.get(string), hash.getBytes());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	static String readStoredTrainingsetHash(String filePath) {
		String hash = null;
		try {
			hash = new String(Files.readAllBytes(Paths.get(filePath)), StandardCharsets.UTF_8);
		} catch (IOException e) {
		}

		return hash;
	}

}
