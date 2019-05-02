package cc.mallet.topics.tui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import cc.mallet.configuration.ConfigFactory;
import cc.mallet.configuration.Configuration;
import cc.mallet.configuration.LDACommandLineParser;
import cc.mallet.configuration.LDAConfiguration;
import cc.mallet.configuration.ParsedLDAConfiguration;
import cc.mallet.similarity.BM25Distance;
import cc.mallet.similarity.CorpusStatistics;
import cc.mallet.types.CrossValidationIterator;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.types.LabelAlphabet;
import cc.mallet.util.LDAUtils;
import cc.mallet.util.LoggingUtils;
import cc.mallet.util.Timer;

public class BM25Search {
	public static String PROGRAM_NAME = "BM25Search";

	public static void main(String[] args) throws Exception {
		BM25Search cl = new BM25Search();
		cl.doRun(args);
	}

	public void doRun(String[] args) throws Exception {
		if(args.length == 0) {
			System.out.println("\n" + PROGRAM_NAME + ": No args given, you should typically call it along the lines of: \n" 
					+ "java -cp PCPLDA-X.X.X.jar cc.mallet.topics.tui." + PROGRAM_NAME + "--run_cfg=src/main/resources/configuration/PLDAConfig.cfg\n");
			System.exit(-1);
		}

		Thread.setDefaultUncaughtExceptionHandler(new Thread.
				UncaughtExceptionHandler() {
			public void uncaughtException(Thread t, Throwable e) {
				System.out.println(t + " throws exception: " + e);
				e.printStackTrace();
				System.err.println("Main thread Exiting.");
				System.exit(-1);
			}
		});

		System.out.println("We have: " + Runtime.getRuntime().availableProcessors() 
				+ " processors avaiable");
		String buildVer = LoggingUtils.getManifestInfo("Implementation-Build","PCPLDA");
		String implVer  = LoggingUtils.getManifestInfo("Implementation-Version", "PCPLDA");
		if(buildVer==null||implVer==null) {
			System.out.println("GIT info:" + LoggingUtils.getLatestCommit());
		} else {
			System.out.println("Build info:" 
					+ "Implementation-Build = " + buildVer + ", " 
					+ "Implementation-Version = " + implVer);
		}

		LDACommandLineParser cp = new LDACommandLineParser(args);

		// We have to create this temporary config because at this stage if we want to create a new config for each run
		ParsedLDAConfiguration tmpconfig = (ParsedLDAConfiguration) ConfigFactory.getMainConfiguration(cp);			

		int numberOfRuns = tmpconfig.getInt("no_runs");
		System.out.println("Doing: " + numberOfRuns + " runs");
		// Reading in command line parameters		
		for (int i = 0; i < numberOfRuns; i++) {
			System.out.println("Starting run: " + i);

			LDAConfiguration config = (LDAConfiguration) ConfigFactory.getMainConfiguration(cp);
			LoggingUtils lu = new LoggingUtils();
			String expDir = config.getExperimentOutputDirectory("");
			if(!expDir.equals("")) {
				expDir += "/";
			}
			String logSuitePath = "Runs/" + expDir + "RunSuite" + LoggingUtils.getDateStamp();
			System.out.println("Logging to: " + logSuitePath);
			lu.checkAndCreateCurrentLogDir(logSuitePath);
			config.setLoggingUtil(lu);

			String [] configs = config.getSubConfigs();
			for(String conf : configs) {
				lu.checkCreateAndSetSubLogDir(conf);
				config.activateSubconfig(conf);

				System.out.println("Using Config: " + config.whereAmI());
				System.out.println("Runnin subconfig: " + conf);
				String dataset_fn = config.getDatasetFilename();
				System.out.println("Using dataset: " + dataset_fn);

				InstanceList instances;
				if(config.getTfIdfVocabSize(LDAConfiguration.TF_IDF_VOCAB_SIZE_DEFAULT)>0) {
					instances = LDAUtils.loadInstancesKeep(dataset_fn, 
							config.getStoplistFilename("stoplist.txt"), config.getTfIdfVocabSize(LDAConfiguration.TF_IDF_VOCAB_SIZE_DEFAULT), config.keepNumbers());					
				} else {					
					instances = LDAUtils.loadInstancesPrune(dataset_fn, 
							config.getStoplistFilename("stoplist.txt"), config.getRareThreshold(LDAConfiguration.RARE_WORD_THRESHOLD), config.keepNumbers());
				}
				
				CorpusStatistics cs = new CorpusStatistics(instances); 
				List<String> strInstances = LDAUtils.loadDatasetAsString(dataset_fn);
				
				System.out.println("Loaded plain text...");
				Map<Integer,String> toPlaintext = new HashMap<>();
				
				int idx = 0;
				for (Instance instance : instances) {
					toPlaintext.put(instance.hashCode(),strInstances.get(idx++));
				}
				

				System.out.println(String.format("Rare word threshold: %d", config.getRareThreshold(LDAConfiguration.RARE_WORD_THRESHOLD)));

				System.out.println("Vocabulary size: " + instances.getDataAlphabet().size() + "\n");
				System.out.println("Instance list is: " + instances.size());
				System.out.println("Loading data instances...");
				System.out.println("_____________________________\n");

				
				int folds = 2;
				Random r = new Random();
				CrossValidationIterator cvIter = new CrossValidationIterator(instances, folds, r);
				InstanceList[] cvSplit = null;
				
				cvSplit = cvIter.next();
				
				int TRAINING = 0;
				int TESTING = 1;

				InstanceList train = cvSplit[TRAINING];
				InstanceList test = cvSplit[TESTING];
								
				BM25Distance bm25 = new BM25Distance(cs.size(), cs.getAvgDocLen(), cs.getDocFreqs());
				Timer t = new Timer();
				t.start();
				//for (Instance instance : test) {
				for (Instance instance : train) {
					double maxScore = Double.NEGATIVE_INFINITY;
					int minDistIdx = -1;

					double [] scores = new double[train.size()];
					for (int trainDocIdx = 0; trainDocIdx < train.size(); trainDocIdx++) {
						Instance trainDoc = train.get(trainDocIdx);
						double[] trainTf = CorpusStatistics.calcTf(cs, trainDoc);
						double [] tf = CorpusStatistics.calcQueryTf(cs, instance, trainDocIdx);
						scores[trainDocIdx] = bm25.calculate(tf,trainTf);
					}
					for (int j = 0; j < scores.length; j++) {
						if(maxScore<scores[j]) {
							maxScore = scores[j];
							minDistIdx = j;
						}
					}
					if(minDistIdx>-1) {
					
					System.out.println("Test doc (" + LDAUtils.instanceLabelToString(instance,(LabelAlphabet) instances.getTargetAlphabet()) + "):\n"+ stringToBlock(toPlaintext.get(instance.hashCode())));
					System.out.println("Closest train doc(" + LDAUtils.instanceLabelToString(train.get(minDistIdx),(LabelAlphabet) instances.getTargetAlphabet()) + "):\n"+ stringToBlock(toPlaintext.get(train.get(minDistIdx).hashCode())));
					
					System.out.println();
					} else {
						System.out.println("Could not find distance for:" +  stringToBlock(toPlaintext.get(instance.hashCode())));
					}
				}
				t.stop();

				// Save file with summary of results and metadata 
				List<String> metadata = new ArrayList<String>();
				metadata.add("No. Topics: " + config.getNoTopics(-1));
				// Save stats for this run
				lu.dynamicLogRun("Runs", t, cp, (Configuration) config, null, 
						this.getClass().getName(), this.getClass().getSimpleName() + "-results", "HEADING", "PCLDA", numberOfRuns, metadata);

				System.out.println("I am done!");
			}
			if(buildVer==null||implVer==null) {
				System.out.println("GIT info:" + LoggingUtils.getLatestCommit());
			} else {
				System.out.println("Build info:" 
						+ "Implementation-Build = " + buildVer + ", " 
						+ "Implementation-Version = " + implVer);
			}
		}
		// Ensure that we exit even if there are non-daemon threads hanging around
		System.err.println("Finished Exiting...");
		System.exit(0);
	}
	
	String stringToBlock(String oneLiner) {
		String result = "";
		String [] splitted = oneLiner.split(" ");
		int cnt = 0;
		for (String string : splitted) {
			result += string + " ";
			cnt++;
			if(cnt>20) {
				result += "\n";
				cnt = 0;
			}
		}
		
		return result;
	}
}
