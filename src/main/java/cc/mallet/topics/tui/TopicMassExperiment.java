package cc.mallet.topics.tui;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import org.apache.commons.cli.ParseException;
import org.apache.commons.configuration.ConfigurationException;

import cc.mallet.configuration.ConfigFactory;
import cc.mallet.configuration.LDACommandLineParser;
import cc.mallet.configuration.LDAConfiguration;
import cc.mallet.configuration.ParsedLDAConfiguration;
import cc.mallet.configuration.SimpleLDAConfiguration;
import cc.mallet.topics.LDAGibbsSampler;
import cc.mallet.topics.UncollapsedParallelLDA;
import cc.mallet.types.InstanceList;
import cc.mallet.util.FileLoggingUtils;
import cc.mallet.util.LDALoggingUtils;
import cc.mallet.util.LDAUtils;
import cc.mallet.util.LoggingUtils;

public class TopicMassExperiment {

	private static PrintWriter pw;

	public static void main(String[] args) throws Exception {
		
		Thread.setDefaultUncaughtExceptionHandler(new Thread.
				UncaughtExceptionHandler() {
			public void uncaughtException(Thread t, Throwable e) {
				System.out.println(t + " throws exception: " + e);
				e.printStackTrace();
				System.err.println("Main thread Exiting.");
				System.exit(-1);
			}
		});
		
		topicMassExperiment(args);
		
		
		int [] rwds = {0,10,100,200,500,1000,2000};
		pw = new PrintWriter(new FileWriter(new File("typeMass.csv")));
		pw.println("RareWords, Dataset, VocabSize, CorpusSize, Instances");
		for (int i = 0; i < rwds.length; i++) {
			rareWordsExperiment(args, rwds[i]);
			pw.println();
		}
		pw.flush();
		pw.close();
	}

	static void topicMassExperiment(String[] args) throws ParseException, ConfigurationException, FileNotFoundException {
		LDACommandLineParser cp = new LDACommandLineParser(args);
		String logSuitePath = "Runs/RunSuite" + FileLoggingUtils.getDateStamp();
		LDAConfiguration config = (LDAConfiguration) ConfigFactory.getMainConfiguration(cp);
		LDALoggingUtils lu = new LoggingUtils();
		lu.checkAndCreateCurrentLogDir(logSuitePath);
		config.setLoggingUtil(lu);

		int commonSeed = config.getSeed(LDAConfiguration.SEED_DEFAULT);
		String [] configs = config.getSubConfigs();
		for(String conf : configs) {
			lu.checkCreateAndSetSubLogDir(conf);
			config.activateSubconfig(conf);

			System.out.println("Using Config: " + config.whereAmI());
			String dataset_fn = config.getDatasetFilename();
			System.out.println("Using dataset: " + dataset_fn);
			String whichModel = config.getScheme();
			System.out.println("Scheme: " + whichModel);

			InstanceList instances = LDAUtils.loadInstances(dataset_fn, 
					"stoplist.txt", config.getRareThreshold(LDAConfiguration.RARE_WORD_THRESHOLD));

			LDAGibbsSampler model = new UncollapsedParallelLDA(config);
			System.out.println(
					String.format("Uncollapsed LDA (%d batches).", 
							config.getNoBatches(LDAConfiguration.NO_BATCHES_DEFAULT)));
			System.out.println(String.format("Rare word threshold: %d", config.getRareThreshold(LDAConfiguration.RARE_WORD_THRESHOLD)));

			InstanceList trainingSet = instances;
			model.setRandomSeed(commonSeed);
			System.out.println("Using seed " + config.getSeed(LDAConfiguration.SEED_DEFAULT));
			// Imports the data into the model

			System.out.println("Vocabulary size: " + trainingSet.getDataAlphabet().size() + "\n");
			System.out.println("Loading " + trainingSet.size() + " instances...\n");

			model.addInstances(trainingSet);
			
			int [] wordIdxs = model.getTopTypeFrequencyIndices(); 
			int [] counts   = model.getTypeFrequencies();
			int totSum = 0;
			//System.out.println("Word order:");
			for (int i = wordIdxs.length-1; i >= 0; i--) {
				totSum += counts[wordIdxs[i]];
				//System.out.println("Word ["+wordIdxs[i]+", " +i+"] " + model.getAlphabet().lookupObject(wordIdxs[i]) + " occurs: " + counts[wordIdxs[i]] + " times");
			}
			System.out.println("Tot sum:" + totSum + " Alphabet size: " + model.getAlphabet().size());
			double [] cumsum = model.getTypeMassCumSum();
			//System.out.println("Cumsum:" + model.get);
			for (int i = 0; i < cumsum.length; i++) {
				double mass = (((double)i)/cumsum.length);
				//System.out.println("CumSum[" + mass + ", " + i + "]: " + cumsum[i]);
				//if( i > 100) Thread.sleep(10000);
				if(i%50==0) {
					System.out.printf("CumSum[%.4f]:", mass);
					System.out.println(cumsum[i]);
				}
			}
			System.out.println("Dataset:" + dataset_fn);
		}
	}
	
	static void rareWordsExperiment(String[] args, int rareWordTh) throws ParseException, 
	ConfigurationException, IOException {
		LDACommandLineParser cp = new LDACommandLineParser(args);
		String logSuitePath = "Runs/RunSuite" + FileLoggingUtils.getDateStamp();
		ParsedLDAConfiguration origConfig = (ParsedLDAConfiguration) ConfigFactory.getMainConfiguration(cp);
		
		SimpleLDAConfiguration config = origConfig.simpleLDAConfiguration();
		
		config.setRareThreshold(rareWordTh);
		
		LDALoggingUtils lu = new LoggingUtils();
		lu.checkAndCreateCurrentLogDir(logSuitePath);
		config.setLoggingUtil(lu);

		int commonSeed = config.getSeed(LDAConfiguration.SEED_DEFAULT);
		String [] configs = config.getSubConfigs();
		for(String conf : configs) {
			lu.checkCreateAndSetSubLogDir(conf);
			config.activateSubconfig(conf);

			String dataset_fn = config.getDatasetFilename();

			InstanceList instances = LDAUtils.loadInstances(dataset_fn, 
					"stoplist.txt", config.getRareThreshold(LDAConfiguration.RARE_WORD_THRESHOLD));

			LDAGibbsSampler model = new UncollapsedParallelLDA(config);
			System.out.println(
					String.format("Uncollapsed LDA (%d batches).", 
							config.getNoBatches(LDAConfiguration.NO_BATCHES_DEFAULT)));

			InstanceList trainingSet = instances;
			model.setRandomSeed(commonSeed);
			model.addInstances(trainingSet);
			
			pw.print(rareWordTh 
					+ ", " + dataset_fn
					+ ", " + trainingSet.getDataAlphabet().size()
					+ ", " + model.getCorpusSize()
					+ ", " + trainingSet.size()
					);
			pw.println();
			
			System.out.println("Rare word threshold: " + rareWordTh);
			System.out.println("Dataset            : " + dataset_fn);
			System.out.println("Vocabulary size    : " + trainingSet.getDataAlphabet().size());
			System.out.println("Instances          : " + trainingSet.size());
			System.out.println("Coprus size      : " + model.getCorpusSize());
		}
	}
}
