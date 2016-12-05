package cc.mallet.topics.tui;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import cc.mallet.classify.KLDivergenceClassifier;
import cc.mallet.classify.KLDivergenceClassifierMultiCorpus;
import cc.mallet.classify.Trial;
import cc.mallet.classify.evaluate.EnhancedConfusionMatrix;
import cc.mallet.configuration.ConfigFactory;
import cc.mallet.configuration.Configuration;
import cc.mallet.configuration.LDACommandLineParser;
import cc.mallet.configuration.LDAConfiguration;
import cc.mallet.configuration.ParsedLDAConfiguration;
import cc.mallet.topics.LDASamplerWithPhi;
import cc.mallet.types.InstanceList;
import cc.mallet.util.LDAUtils;
import cc.mallet.util.LoggingUtils;
import cc.mallet.util.Timer;

public class KLClassifier {
	public static String PROGRAM_NAME = "KLClassifier";

	public static void main(String[] args) throws Exception {
		KLClassifier cl = new KLClassifier();
		cl.doRun(args);
	}

	public void doRun(String[] inargs) throws Exception {
		// Use multicorpus per default
		boolean multiCorpus = true;
		List<String> realArgs = new ArrayList<>();
		for (int i = 0; i < inargs.length; i++) {
			String multicorpus = "-multicorpus";
			String singlecorpus = "-singlecorpus";
			if(inargs[i].trim().toLowerCase().equals(multicorpus) || inargs[i].trim().toLowerCase().equals(singlecorpus)) {
				if(inargs[i].trim().toLowerCase().equals(multicorpus))
					multiCorpus = true;
				if(inargs[i].trim().toLowerCase().equals(singlecorpus))
					multiCorpus = false;
			} else {
				realArgs.add(inargs[i]);
			}
		}
		String [] args = new String[realArgs.size()];
		for (int i = 0; i < args.length; i++) {
			args[i] = realArgs.get(i);
		}
		System.out.println(PROGRAM_NAME + ": Using " + (multiCorpus ? "Multicorpus" : "Singlecorpus") + "...");
		
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

				KLDivergenceClassifierMultiCorpus mcmodel = null;
				KLDivergenceClassifier model = null;
				if(multiCorpus) {
					mcmodel = new KLDivergenceClassifierMultiCorpus(config);
				}
				else { 
					model = new KLDivergenceClassifier(config);
				}

				System.out.println(String.format("Rare word threshold: %d", config.getRareThreshold(LDAConfiguration.RARE_WORD_THRESHOLD)));

				System.out.println("Vocabulary size: " + instances.getDataAlphabet().size() + "\n");
				System.out.println("Instance list is: " + instances.size());
				System.out.println("Loading data instances...");

				// Sets the frequent with which top words for each topic are printed
				//model.setShowTopicsInterval(config.getTopicInterval(LDAConfiguration.TOPIC_INTER_DEFAULT));
				System.out.println("Config seed:" + config.getSeed(LDAConfiguration.SEED_DEFAULT));
				// Imports the data into the model

				System.out.println("Starting iterations (" + config.getNoIterations(LDAConfiguration.NO_ITER_DEFAULT) + " total).");
				System.out.println("_____________________________\n");

				// Runs the model
				System.out.println("Starting:" + new Date());
				Timer t = new Timer();
				t.start();
				Trial [] trials;
				if(multiCorpus) {
					trials = mcmodel.crossValidate(instances,5);
				}
				else {	
					trials = model.crossValidate(instances,5);	
				}
				t.stop();

				String trialResuls = "[";
				double average = 0.0;
				for (int trialNo = 0; trialNo < trials.length; trialNo++) {
					average += trials[trialNo].getAccuracy();
					trialResuls += String.format("%.4f",trials[trialNo].getAccuracy()) + ", ";
				}
				trialResuls = trialResuls.substring(0, trialResuls.length()-1) + "]";
				System.out.println();

				EnhancedConfusionMatrix combinedConfusionMatrix = new EnhancedConfusionMatrix(trials);
				System.out.println("Combined Confusion Matrix: \n" + combinedConfusionMatrix);
				System.out.println();
				double xvalidationAverage = average / trials.length;
				System.out.println("X-validation: " + trialResuls + " average: " + xvalidationAverage);
				System.out.println(PROGRAM_NAME + " cross validation took: " + (t.getEllapsedTime() / 1000) + " seconds");

				/*if(saveConfusionMatrixAsCsv) {
					PrintWriter pw = new PrintWriter(lgDir.getAbsolutePath() + "/last-confusion-matrix.csv");
					pw.println(combinedConfusionMatrix.toCsv(","));
					pw.flush();
					pw.close();
				}*/

				/*
				if(config.doPlot()) {
					ClassificationResultPlot.plot2D(labels, xs);					
				}
				 */

				// Save file with summary of results and metadata 
				List<String> metadata = new ArrayList<String>();
				metadata.add("No. Topics: " + config.getNoTopics(-1));
				metadata.add("Accuracy: " + String.format("%.0f",(xvalidationAverage*100)));
				metadata.add("ConfusionMatrix: " + "\n" + combinedConfusionMatrix);
				metadata.add("KLClassifier type: " + (multiCorpus ? "Multicorpus" : "Singlecorpus"));
				// Save stats for this run
				lu.dynamicLogRun("Runs", t, cp, (Configuration) config, null, 
						this.getClass().getName(), this.getClass().getSimpleName() + "-results", "HEADING", "PCLDA", numberOfRuns, metadata);

				File lgDir = lu.getLogDir();
				
				
				if(multiCorpus) {
					Map<String, LDASamplerWithPhi> samplers = mcmodel.getTrainedSamplers();
					PrintWriter out = new PrintWriter(lgDir.getAbsolutePath() + "/TopWords.txt");
					for (String key : samplers.keySet()) {
						LDASamplerWithPhi sampler = samplers.get(key);
						String [][] topWordMatrix = LDAUtils.getTopWords(config.getNrTopWords(LDAConfiguration.NO_TOP_WORDS_DEFAULT), 
								sampler.getAlphabet().size(), config.getNoTopics(LDAConfiguration.NO_TOPICS_DEFAULT), sampler.getTypeTopicMatrix(), mcmodel.getAlphabet());
						String topWords = LDAUtils.formatTopWordsAsCsv(topWordMatrix);
						out.println("Top words for class "+ key +" are: \n" + topWords);
						System.out.println("Top words for class "+ key +" are: \n" + topWords);
					}
					out.flush();
					out.close();
				} else {
					LDASamplerWithPhi sampler = model.getTrainedSampler();
					PrintWriter out = new PrintWriter(lgDir.getAbsolutePath() + "/TopWords.txt");
					String [][] topWordMatrix = LDAUtils.getTopWords(config.getNrTopWords(LDAConfiguration.NO_TOP_WORDS_DEFAULT), 
							sampler.getAlphabet().size(), config.getNoTopics(LDAConfiguration.NO_TOPICS_DEFAULT), sampler.getTypeTopicMatrix(), mcmodel.getAlphabet());
					String topWords = LDAUtils.formatTopWordsAsCsv(topWordMatrix);
					out.println(topWords);
					System.out.println("Top words for class are: \n" + topWords);
					out.flush();
					out.close();					
				}

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
}
