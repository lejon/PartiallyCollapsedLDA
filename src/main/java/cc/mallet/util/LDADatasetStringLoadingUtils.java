package cc.mallet.util;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;

import cc.mallet.pipe.CharSequenceLowercase;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.RawTokenizer;
import cc.mallet.pipe.SerialPipes;
import cc.mallet.pipe.SimpleTokenizerLarge;
import cc.mallet.pipe.StringList2FeatureSequence;
import cc.mallet.pipe.Target2Label;
import cc.mallet.types.Alphabet;
import cc.mallet.types.InstanceList;
import cc.mallet.types.LabelAlphabet;

public class LDADatasetStringLoadingUtils {
	public static InstanceList loadInstancesStrings(String [] doclines, Pipe pipe) {
		String [] classNames = new String [doclines.length];
		for (int i = 0; i < classNames.length; i++) {
			classNames[i] = "X";
		}

		return loadInstancesStrings(doclines, classNames, null, "stoplist.txt", pipe, false);
	}

	public static InstanceList loadInstancesStrings(String [] doclines, String [] classNames, Pipe pipe) {
		return loadInstancesStrings(doclines, classNames, null, "stoplist.txt", pipe, false);
	}


	public static InstanceList loadInstancesStrings(String [] doclines) {
		return loadInstancesStrings(doclines, "X");
	}

	/**
	 * Do no preprocessing of the input data at all, no lowercase, no stoplist
	 * 
	 * @param doclines
	 * @param className
	 * @return
	 */
	public static InstanceList loadInstancesStringsRaw(String [] doclines, String className) {
		return loadInstancesStrings(doclines, className, true);
	}

	public static InstanceList loadInstancesStrings(String [] doclines, String className) {
		return loadInstancesStrings(doclines, className, false);
	}


	public static InstanceList loadInstancesStrings(String [] doclines, String className, boolean raw) {
		String [] classNames = new String [doclines.length];
		for (int i = 0; i < classNames.length; i++) {
			classNames[i] = "X";
		}
		return loadInstancesStrings(doclines, classNames, (String []) null, null, null, raw);
	}

	public static InstanceList loadInstancesStrings(String [] doclines, String [] classNames) {
		return loadInstancesStrings(doclines, classNames, (String) null);
	}

	public static InstanceList loadInstancesStrings(String [] doclines, String [] classNames, String [] docIds) {
		return loadInstancesStrings(doclines, classNames, docIds, "stoplist.txt", null, false);
	}

	public static InstanceList loadInstancesStrings(String [] doclines, String [] classNames, String [] docIds, Pipe pipe) {
		return loadInstancesStrings(doclines, classNames, docIds, "stoplist.txt", pipe, false);
	}

	public static InstanceList loadInstancesStrings(String [] doclines, String [] classNames, String stoplistFile) {
		return loadInstancesStrings(doclines, classNames, null, stoplistFile, null, false);
	}

	public static InstanceList loadInstancesStrings(String [] doclines, String [] classNames, String [] docIds, 
			String stoplistFile, Pipe pipe, boolean raw) {
		InstanceList instances;
		int bufferSize = 10000;
		int tries = 0;

		instances = loadInstancesStrings(doclines, classNames, docIds, stoplistFile, pipe, raw, bufferSize);

		if(instances == null) {
			// For really large documents we might need to increase the size of the 
			// tokenizer buffer...
			do {
				tries++;
				bufferSize = bufferSize * 2;
				System.out.println("Doubling buffer size to: " + bufferSize);
				instances = loadInstancesStrings(doclines, classNames, docIds, stoplistFile, pipe, raw, bufferSize);
			} while(instances == null && tries < 10);
		}
		return instances;
	}


	public static InstanceList loadInstancesStrings(String [] doclines, String [] classNames, String [] docIds, 
			String stoplistFile, Pipe pipe, boolean raw, int bufferSize) {

		StringClassArrayIterator readerTrain = new StringClassArrayIterator(doclines, classNames, docIds); 

		if(pipe == null) {
			pipe = buildSerialPipe(stoplistFile, null, null, raw, bufferSize);
		}

		InstanceList instances = new InstanceList(pipe);
		try {
			instances.addThruPipe(readerTrain);
		} catch (ArrayIndexOutOfBoundsException e) {
			return null;
		}
		return instances;
	}	
	
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
		return buildSerialPipe(stoplistFile, dataAlphabet, targetAlphabet, raw, 10000);
	}

	public static Pipe buildSerialPipe(String stoplistFile, Alphabet dataAlphabet, 
			LabelAlphabet targetAlphabet, boolean raw, int maxBufSize) { 		
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

}
