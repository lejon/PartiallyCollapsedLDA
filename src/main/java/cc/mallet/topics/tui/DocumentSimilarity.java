package cc.mallet.topics.tui;

import java.util.ArrayList;
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
import cc.mallet.similarity.CosineDistance;
import cc.mallet.similarity.DocumentDistancer;
import cc.mallet.similarity.LDADistancer;
import cc.mallet.similarity.TokenIndexVectorizer;
import cc.mallet.types.CrossValidationIterator;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.types.LabelAlphabet;
import cc.mallet.util.LDAUtils;
import cc.mallet.util.LoggingUtils;
import cc.mallet.util.Timer;

public class DocumentSimilarity {
	public static String PROGRAM_NAME = "DocumentSimilarity";

	public static void main(String[] args) throws Exception {
		DocumentSimilarity cl = new DocumentSimilarity();
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

				InstanceList instances = LDAUtils.loadDataset(config, dataset_fn);	
				instances.getAlphabet().stopGrowth();
								
				List<String> strInstances = LDAUtils.loadDatasetAsString(dataset_fn);
				
				System.out.println("Loaded plain text...");
				Map<Integer,String> toPlaintext = new HashMap<>();
				
				int idx = 0;
				for (Instance instance : instances) {
					toPlaintext.put(instance.hashCode(),strInstances.get(idx++));
				}
				
				if(config.getTfIdfVocabSize(LDAConfiguration.TF_IDF_VOCAB_SIZE_DEFAULT)>0) {
					System.out.println(String.format("Top TF-IDF threshold: %d", 
							config.getTfIdfVocabSize(LDAConfiguration.TF_IDF_VOCAB_SIZE_DEFAULT)));
				} else {
					System.out.println(String.format("Rare word threshold: %d", 
							config.getRareThreshold(LDAConfiguration.RARE_WORD_THRESHOLD)));
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

				
				InstanceList train;
				InstanceList test;
				if(config.getTestDatasetFilename() == null) {
					int folds = 2;
					Random r = new Random();
					CrossValidationIterator cvIter = new CrossValidationIterator(instances, folds, r);
					InstanceList[] cvSplit = null;

					cvSplit = cvIter.next();

					int TRAINING = 0;
					int TESTING = 1;

					System.out.println("Splitting dataset in test/train...");
					train = cvSplit[TRAINING];
					System.out.println("Train size: " + train.size());
					test = cvSplit[TESTING];
					System.out.println("Test size: " + test.size());
				} else {
					train = instances;
					test = LDAUtils.loadDataset(config, config.getTestDatasetFilename(), instances.getAlphabet());
					
					List<String> testStrs = LDAUtils.loadDatasetAsString(config.getTestDatasetFilename());
					strInstances.addAll(testStrs);
					
					for (Instance instance : test) {
						toPlaintext.put(instance.hashCode(),strInstances.get(idx++));
					}
					
				}
				
				System.out.println("Starting model training:" + new Date());
				Timer t = new Timer();
				t.start();
				DocumentDistancer model = trainModel(config, train);	
				t.stop();

				evaluate(instances, toPlaintext, model, train, test);

				// Save file with summary of results and metadata 
				List<String> metadata = new ArrayList<String>();
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
		System.err.println("Finished! Exiting...");
		System.exit(0);
	}

	private DocumentDistancer trainModel(LDAConfiguration config, InstanceList train) {
		DocumentDistancer model = new DocumentDistancer(config);
		//model.setDist(new UberDistance());
		
//		model.setDist(new CosineDistance());
//		try {
//			model.setInstanceVectorizer(new TfIdfVectorizer(LDAUtils.getTfIdfPipeFromConfig(config)));
//		} catch (FileNotFoundException e) {
//			e.printStackTrace();
//		}
		
		//model.setDist(new cc.mallet.similarity.ManhattanDistance());
		//model.setDist(new KLDistance());
		//model.setDist(new KolmogorovSmirnovDistance());
		//model.setDist(new HellingerDistance());

//		model.setDist(new NormalizedCompressionDistance(new PPMCompressor()));
//		model.setInstanceVectorizer(new TokenIndexVectorizer());
		
//		model.setDist(new JaccardDistance());
//		model.setInstanceVectorizer(new TokenOccurenceVectorizer());
//		model.setInstanceVectorizer(new TokenFrequencyVectorizer());

//		model.setDist(new LDADistancer(config,new KLDistance()));
//		model.setDist(new LDADistancer(config,new SymmetricKLDistance()));
		model.setDist(new LDADistancer(config,new CosineDistance()));
		model.setInstanceVectorizer(new TokenIndexVectorizer());
		
//		model.setDist(new BM25Distance());
//		model.setInstanceVectorizer(new TokenIndexVectorizer());

//		model.setDist(new LikelihoodDistance());
//		model.setInstanceVectorizer(new TokenFrequencyVectorizer());
//		
//		model.setDist(new LDALikelihoodDistance(config));
//		model.setInstanceVectorizer(new TokenFrequencyVectorizer());
		
		model.train(train);
		
		return model;
	}

	private void evaluate(InstanceList instances, Map<Integer, String> toPlaintext, DocumentDistancer model,
			InstanceList train, InstanceList test) {
		int testCnt = 0;
		for (Instance testInstance : test) {
			
			System.out.println("Test doc (" + 
					LDAUtils.instanceLabelToString(testInstance) + 
					"):\n----------------------------\n"+ 
					stringToBlock(toPlaintext.get(testInstance.hashCode())));
			System.out.println("============================");
			
			double [] idxDist = model.getClosestIdx(testInstance);
			int minDistIdx = (int) idxDist[0];
			double minDist = idxDist[1];

			System.out.println("Test doc: " + model.toAnnotatedString(testInstance));
			
			if(idxDist[0]>-1) {
				System.out.println("Min Distance is: " + minDist + " for Idx: "+ minDistIdx);
				Instance closestTrain = train.get(minDistIdx);
				List<String> common = LDAUtils.findCommonWords(testInstance,closestTrain);
				System.out.println("Common words: " + common);

				System.out.println("Train doc: " + model.toAnnotatedString(closestTrain));
				System.out.println("============================");
				System.out.println("Closest train doc (" + 
						LDAUtils.instanceLabelToString(closestTrain) + 
						"):\n----------------------------\n" + 
						stringToBlock(toPlaintext.get(closestTrain.hashCode())));
				System.out.println("----------------------------");

				System.out.println();
			} else {
				System.out.println("Could not find distance for:" +  stringToBlock(toPlaintext.get(testInstance.hashCode())));
			}
			testCnt++;
			System.out.println(testCnt + "/" + test.size()+ "\n");
		}
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
