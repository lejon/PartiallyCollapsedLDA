package cc.mallet.util;

import java.io.File;
import java.io.FileFilter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Pattern;

import cc.mallet.configuration.LDAConfiguration;
import cc.mallet.pipe.CharSequence2TokenSequence;
import cc.mallet.pipe.Input2CharSequence;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.SerialPipes;
import cc.mallet.pipe.SimpleTokenizerLarge;
import cc.mallet.pipe.Target2Label;
import cc.mallet.pipe.TfIdfPipe;
import cc.mallet.pipe.TokenSequence2FeatureSequence;
import cc.mallet.pipe.TokenSequenceLowercase;
import cc.mallet.pipe.TokenSequencePredicateMatcher;
import cc.mallet.pipe.TokenSequenceRemoveStopwords;
import cc.mallet.pipe.iterator.FileIterator;
import cc.mallet.types.Alphabet;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.types.LabelAlphabet;

public class LDADatasetDirectoryLoadingUtils {
	
	static InstanceList loadFromDir(LDAConfiguration config, String dataset_fn, Alphabet alphabet) {
		InstanceList instances;
		instances = loadInstanceDirectory(
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

		tokenizer = LDAUtils.initTokenizer(stoplistFile, keepNumbers, maxBufSize, keepConnectors);

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

}
