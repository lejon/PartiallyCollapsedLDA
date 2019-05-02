package cc.mallet.topics.tui;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import cc.mallet.configuration.ConfigFactory;
import cc.mallet.configuration.Configuration;
import cc.mallet.configuration.LDACommandLineParser;
import cc.mallet.configuration.LDAConfiguration;
import cc.mallet.configuration.ParsedLDAConfiguration;
import cc.mallet.similarity.LDADistancer;
import cc.mallet.topics.LDASamplerWithPhi;
import cc.mallet.types.CrossValidationIterator;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.types.LabelAlphabet;
import cc.mallet.util.IndexSorter;
import cc.mallet.util.LDAUtils;
import cc.mallet.util.LoggingUtils;
import cc.mallet.util.Timer;

public class LDASimilarity {
	public static String PROGRAM_NAME = "LDASimilarity";

	public static void main(String[] args) throws Exception {
		LDASimilarity cl = new LDASimilarity();
		cl.doRun(args);
	}

	public void doRun(String[] args) throws Exception {
		if(args.length == 0) {
			System.out.println("\n" + PROGRAM_NAME + ": No args given, you should typically call it along the lines of: \n" 
					+ "java -cp PCPLDA-X.X.X.jar cc.mallet.topics.tui.LDASimilarity --run_cfg=src/main/resources/configuration/PLDAConfig.cfg\n");
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
								
				List<String> strInstances = LDAUtils.loadDatasetAsString(dataset_fn);
				
				System.out.println("Loaded plain text...");
				Map<Integer,String> toPlaintext = new HashMap<>();
				
				int idx = 0;
				for (Instance instance : instances) {
					toPlaintext.put(instance.hashCode(),strInstances.get(idx++));
				}
				
				LDADistancer model = new LDADistancer(config);
				//model.setDist(new UberDistance());
				//model.setDist(new CosineDistance());
				model.setDist(new cc.mallet.similarity.ManhattanDistance());
				//model.setDist(new KLDistance());
				//model.setDist(new KolmogorovSmirnovDistance());
				//model.setDist(new HellingerDistance());

				if(config.getTfIdfVocabSize(LDAConfiguration.TF_IDF_VOCAB_SIZE_DEFAULT)>0) {
					System.out.println(String.format("Top TF-IDF threshold: %d", config.getTfIdfVocabSize(LDAConfiguration.TF_IDF_VOCAB_SIZE_DEFAULT)));
				} else {
					System.out.println(String.format("Rare word threshold: %d", config.getRareThreshold(LDAConfiguration.RARE_WORD_THRESHOLD)));
				}

				System.out.println("Vocabulary size: " + instances.getDataAlphabet().size() + "\n");
				System.out.println("Instance list is: " + instances.size());
				System.out.println("Loading data instances...");

				// Sets the frequent with which top words for each topic are printed
				//model.setShowTopicsInterval(config.getTopicInterval(LDAConfiguration.TOPIC_INTER_DEFAULT));
				System.out.println("Config seed:" + config.getSeed(LDAConfiguration.SEED_DEFAULT));
				// Imports the data into the model

				System.out.println("Starting iterations (" + config.getNoIterations(LDAConfiguration.NO_ITER_DEFAULT) + " total).");
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
				
				model.setNoTRainingSamples(test.size());
				// Runs the model
				System.out.println("Starting:" + new Date());
				Timer t = new Timer();
				t.start();
				LDASamplerWithPhi ldaModel = model.train(train);	
				t.stop();
				
				String [][] topWords = model.getTopWords(10);
				double [][] thetaEstimates = ldaModel.getThetaEstimate();
				
				System.out.println("Topic distribution:");
				for (int j = 0; j < thetaEstimates.length; j++) {
					System.out.println("[" + j + "]" + LDADistancer.arrToStr(thetaEstimates[j], "Topic Dist:", true));
				}
				
				int tCnt = 0;
				//for (Instance instance : train) {
				for (Instance instance : test) {
					double minDist = Double.POSITIVE_INFINITY;
					int minDistIdx = -1;
					double [] distances = model.distance(instance);
					for (int j = 0; j < distances.length; j++) {
						if(minDist>distances[j]) {
							minDist = distances[j];
							minDistIdx = j;
						}
					}
					System.out.println("Distances: " + Arrays.toString(distances));
					if(minDistIdx>-1) {
						System.out.println("Min Distance is: " + minDist + " for Idx: "+ minDistIdx);

						System.out.println("Test doc (" + LDAUtils.instanceLabelToString(instance,(LabelAlphabet) instances.getTargetAlphabet()) + "):\n"+ stringToBlock(toPlaintext.get(instance.hashCode())));
						double[] thetaEst = model.getSampledTestTopics()[tCnt];
						int [] maxIdxs = IndexSorter.getSortedIndices(thetaEst);
						System.out.println("Top Words 1: [" + maxIdxs[0] + "]" + Arrays.toString(topWords[maxIdxs[0]]));
						System.out.println("Top Words 2: [" + maxIdxs[1] + "]" + Arrays.toString(topWords[maxIdxs[1]]));
						System.out.println("Top Words 3: [" + maxIdxs[2] + "]" + Arrays.toString(topWords[maxIdxs[2]]));
						System.out.println("Theta Test :" + LDADistancer.arrToStr(thetaEst, "Topic Dist:", true));
						System.out.println("Theta Train:" + LDADistancer.arrToStr(thetaEstimates[minDistIdx], "Topic Dist:", true));
						thetaEst = thetaEstimates[minDistIdx];
						maxIdxs = IndexSorter.getSortedIndices(thetaEst);
						System.out.println("Top Words 1: [" + maxIdxs[0] + "]" + Arrays.toString(topWords[maxIdxs[0]]));
						System.out.println("Top Words 2: [" + maxIdxs[1] + "]" + Arrays.toString(topWords[maxIdxs[1]]));
						System.out.println("Top Words 3: [" + maxIdxs[2] + "]" + Arrays.toString(topWords[maxIdxs[2]]));
						System.out.println("Closest train doc(" + LDAUtils.instanceLabelToString(train.get(minDistIdx),(LabelAlphabet) instances.getTargetAlphabet()) + "):\n"+ stringToBlock(toPlaintext.get(train.get(minDistIdx).hashCode())));

						System.out.println();
					} else {
						System.out.println("Could not find distance for:" +  stringToBlock(toPlaintext.get(instance.hashCode())));
					}
					tCnt++;
				}

				// Save file with summary of results and metadata 
				List<String> metadata = new ArrayList<String>();
				metadata.add("No. Topics: " + config.getNoTopics(-1));
				// Save stats for this run
				lu.dynamicLogRun("Runs", t, cp, (Configuration) config, null, 
						this.getClass().getName(), this.getClass().getSimpleName() + "-results", "HEADING", "PCLDA", numberOfRuns, metadata);

				File lgDir = lu.getLogDir();
				

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
