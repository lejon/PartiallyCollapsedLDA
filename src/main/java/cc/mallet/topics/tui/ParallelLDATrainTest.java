package cc.mallet.topics.tui;

import java.io.File;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import cc.mallet.configuration.ConfigFactory;
import cc.mallet.configuration.Configuration;
import cc.mallet.configuration.LDACommandLineParser;
import cc.mallet.configuration.LDAConfiguration;
import cc.mallet.configuration.LDATrainTestCommandLineParser;
import cc.mallet.configuration.LDATrainTestConfiguration;
import cc.mallet.configuration.ParsedLDAConfiguration;
import cc.mallet.topics.SpaliasUncollapsedParallelLDA;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.util.LDAUtils;
import cc.mallet.util.LoggingUtils;
import cc.mallet.util.Timer;

public class ParallelLDATrainTest extends ParallelLDA {
	public static String PROGRAM_NAME = "ParallelLDATrainTest";
	final static int TRAINING = 0;
	final static int TESTING = 1;

	public static void main(String[] args) throws Exception {
		
		if(args.length == 0) {
			System.out.println("\n" + PROGRAM_NAME + ": No args given, you should typically call it along the lines of: \n" 
					+ "java -cp PCPLDA-X.X.X.jar cc.mallet.topics.tui.ParallelLDA --run_cfg=src/main/resources/configuration/PLDAConfig.cfg\n" 
					+ "or\n" 
					+ "java -jar PCPLDA-X.X.X.jar —run_cfg=src/main/resources/configuration/PLDAConfig.cfg\n");
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
		
		LDACommandLineParser cp = new LDATrainTestCommandLineParser(args);
		
		// We have to create this temporary config because at this stage if we want to create a new config for each run
		ParsedLDAConfiguration tmpconfig = (ParsedLDAConfiguration) ConfigFactory.getTrainTestConfiguration(cp);			
		
		int numberOfRuns = tmpconfig.getInt("no_runs");
		System.out.println("Doing: " + numberOfRuns + " runs");
		// Reading in command line parameters		
		for (int i = 0; i < numberOfRuns; i++) {
			System.out.println("Starting run: " + i);
			
			LDATrainTestConfiguration config = (LDATrainTestConfiguration) ConfigFactory.getTrainTestConfiguration(cp);
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
				String whichModel = config.getScheme();
				System.out.println("Scheme: " + whichModel);

				InstanceList instances;
				if(config.getTfIdfVocabSize(LDAConfiguration.TF_IDF_VOCAB_SIZE_DEFAULT)>0) {
					instances = LDAUtils.loadInstancesKeep(dataset_fn, 
							config.getStoplistFilename("stoplist.txt"), config.getTfIdfVocabSize(LDAConfiguration.TF_IDF_VOCAB_SIZE_DEFAULT), config.keepNumbers());					
				} else {					
					instances = LDAUtils.loadInstancesPrune(dataset_fn, 
							config.getStoplistFilename("stoplist.txt"), config.getRareThreshold(LDAConfiguration.RARE_WORD_THRESHOLD), config.keepNumbers());
				}

				System.out.println(String.format("Rare word threshold: %d", config.getRareThreshold(LDAConfiguration.RARE_WORD_THRESHOLD)));

				System.out.println("Vocabulary size: " + instances.getDataAlphabet().size() + "\n");
				System.out.println("Instance list is: " + instances.size());
				System.out.println("Loading data instances...");

				// Sets the frequent with which top words for each topic are printed
				//model.setShowTopicsInterval(config.getTopicInterval(LDAConfiguration.TOPIC_INTER_DEFAULT));
				System.out.println("Config seed:" + config.getSeed(LDAConfiguration.SEED_DEFAULT));
				
				String testInstancesFilename = config.getTextDatasetTestIdsFilename();
				List<String> unTrimmedTestIds = Files.readAllLines(Paths.get(testInstancesFilename), Charset.defaultCharset());
				List<String> testIds = new ArrayList<String>();
				for (String string : unTrimmedTestIds) {
					testIds.add(string.trim());
				}
				InstanceList [] trainTest = extractTrainTestInstances(instances, testIds);
				
				System.out.println("Starting:" + new Date());
				Timer t = new Timer();
				t.start();
				InstanceList trainingInstances = trainTest[TRAINING];
				System.out.println("Training set contains: " + trainingInstances.size() + " instances");
				SpaliasUncollapsedParallelLDA trainedSampler = XValidationCreator.sampleTrainingset(trainingInstances, config);
				InstanceList testInstances = trainTest[TESTING];
				System.out.println("Test set contains: " + testInstances.size() + " instances");
				SpaliasUncollapsedParallelLDA testSampler = XValidationCreator.sampleTestset(testInstances, trainingInstances, trainedSampler.getPhi(), config);
				t.stop();
				System.out.println("Finished:" + new Date());
				
				File lgDir = lu.getLogDir();

				List<String> metadata = new ArrayList<String>();
				metadata.add("No. Topics: " + trainedSampler.getNoTopics());
				metadata.add("Start Seed: " + trainedSampler.getStartSeed());
				// Save stats for this run
				lu.dynamicLogRun("Runs", t, cp, (Configuration) config, null, 
						ParallelLDATrainTest.class.getName(), "Convergence", "HEADING", "PLDA", 1, metadata);
				PrintWriter out = new PrintWriter(lgDir.getAbsolutePath() + "/TopWords.txt");
				String topWords = LDAUtils.formatTopWordsAsCsv(
						LDAUtils.getTopRelevanceWords(config.getNrTopWords(LDAConfiguration.NO_TOP_WORDS_DEFAULT), 
								trainedSampler.getAlphabet().size(), 
								trainedSampler.getNoTopics(), 
								trainedSampler.getTypeTopicMatrix(),  
								config.getBeta(LDAConfiguration.BETA_DEFAULT),
								config.getLambda(LDAConfiguration.LAMBDA_DEFAULT), 
								trainedSampler.getAlphabet()));
				out.println(topWords);
				out.flush();
				out.close();

				System.out.println("Top words are: \n" + topWords);
				
				String testTopWords = LDAUtils.formatTopWordsAsCsv(
						LDAUtils.getTopRelevanceWords(config.getNrTopWords(LDAConfiguration.NO_TOP_WORDS_DEFAULT), 
								testSampler.getAlphabet().size(), 
								testSampler.getNoTopics(), 
								testSampler.getTypeTopicMatrix(),  
								config.getBeta(LDAConfiguration.BETA_DEFAULT),
								config.getLambda(LDAConfiguration.LAMBDA_DEFAULT), 
								testSampler.getAlphabet()));
				
				System.out.println("Test words are: \n" + testTopWords);
				
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
	}

	public static InstanceList [] extractTrainTestInstances(InstanceList instances, List<String> testIds) {
		InstanceList training = new InstanceList(instances.getPipe());
		InstanceList test = new InstanceList(instances.getPipe());

		for (Instance instance : instances) {
			if(testIds.contains(instance.getName().toString())) {
				test.add(instance);
			} else {
				training.add(instance);
			}
		}

		InstanceList[] datasets = new InstanceList[2];
		datasets[TRAINING] = training;
		datasets[TESTING] = test;
		return datasets;
	}
}	
