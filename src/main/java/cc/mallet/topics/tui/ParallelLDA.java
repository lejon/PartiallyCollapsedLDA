package cc.mallet.topics.tui;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import cc.mallet.configuration.ConfigFactory;
import cc.mallet.configuration.Configuration;
import cc.mallet.configuration.LDACommandLineParser;
import cc.mallet.configuration.LDAConfiguration;
import cc.mallet.configuration.ParsedLDAConfiguration;
import cc.mallet.topics.ADLDA;
import cc.mallet.topics.CollapsedLightLDA;
import cc.mallet.topics.EfficientUncollapsedParallelLDA;
import cc.mallet.topics.HDPSamplerWithPhi;
import cc.mallet.topics.LDAGibbsSampler;
import cc.mallet.topics.LDASamplerWithPhi;
import cc.mallet.topics.LightPCLDA;
import cc.mallet.topics.LightPCLDAtypeTopicProposal;
import cc.mallet.topics.NZVSSpaliasUncollapsedParallelLDA;
import cc.mallet.topics.PoissonPolyaUrnHDPLDA;
import cc.mallet.topics.PoissonPolyaUrnHLDA;
import cc.mallet.topics.PolyaUrnSpaliasLDA;
import cc.mallet.topics.SerialCollapsedLDA;
import cc.mallet.topics.SpaliasUncollapsedParallelLDA;
import cc.mallet.topics.SpaliasUncollapsedParallelWithPriors;
import cc.mallet.topics.TopicModelDiagnosticsPlain;
import cc.mallet.topics.UncollapsedParallelLDA;
import cc.mallet.types.InstanceList;
import cc.mallet.util.LDAUtils;
import cc.mallet.util.LoggingUtils;
import cc.mallet.util.Timer;

public class ParallelLDA {
	public static String PROGRAM_NAME = "ParallelLDA";

	public static void main(String[] args) throws Exception {
		
		if(args.length == 0) {
			System.out.println("\n" + PROGRAM_NAME + ": No args given, you should typically call it along the lines of: \n" 
					+ "java -cp PCPLDA-X.X.X.jar cc.mallet.topics.tui.ParallelLDA --run_cfg=src/main/resources/configuration/PLDAConfig.cfg\n" 
					+ "or\n" 
					+ "java -jar PCPLDA-X.X.X.jar -run_cfg=src/main/resources/configuration/PLDAConfig.cfg\n");
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
				int commonSeed = config.getSeed(LDAConfiguration.SEED_DEFAULT);

				System.out.println("Using Config: " + config.whereAmI());
				System.out.println("Runnin subconfig: " + conf);
				String dataset_fn = config.getDatasetFilename();
				System.out.println("Using dataset: " + dataset_fn);
				if(config.getTestDatasetFilename()!=null) {
					System.out.println("Using TEST dataset: " + config.getTestDatasetFilename());
				}
				String whichModel = config.getScheme();
				System.out.println("Scheme: " + whichModel);

				InstanceList instances = LDAUtils.loadDataset(config, dataset_fn);
				instances.getAlphabet().stopGrowth();

				LDAGibbsSampler model = createModel(config, whichModel);
				
				model.setRandomSeed(commonSeed);
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
				System.out.println("Start seed: " + model.getStartSeed());
				// Imports the data into the model
				model.addInstances(instances);
				if(config.getTestDatasetFilename()!=null) {
					InstanceList testInstances = LDAUtils.loadDataset(config, config.getTestDatasetFilename(),instances.getAlphabet());
					model.addTestInstances(testInstances);
				}
			
				System.out.println("Loaded " + model.getDataset().size() + " documents, with " + model.getCorpusSize() + " words in total.");

				System.out.println("Starting iterations (" + config.getNoIterations(LDAConfiguration.NO_ITER_DEFAULT) + " total).");
				System.out.println("_____________________________\n");

				// Runs the model
				System.out.println("Starting:" + new Date());
				Timer t = new Timer();
				t.start();
				model.sample(config.getNoIterations(LDAConfiguration.NO_ITER_DEFAULT));
				t.stop();
				System.out.println("Finished:" + new Date());
				
				int requestedWords = config.getNrTopWords(LDAConfiguration.NO_TOP_WORDS_DEFAULT);
				TopicModelDiagnosticsPlain tmd = new TopicModelDiagnosticsPlain(model, requestedWords);
				//System.out.println("Topic model diagnostics:");
				//System.out.println(tmd.toString());				
				
				File lgDir = lu.getLogDir();
				if(config.saveDocumentTopicMeans()) {
					String docTopicMeanFn = config.getDocumentTopicMeansOutputFilename();
					double [][] means = model.getZbar();
					LDAUtils.writeASCIIDoubleMatrix(means, lgDir.getAbsolutePath() + "/" + docTopicMeanFn, ",");
				}
				
				if(config.saveDocumentTopicDiagnostics()) {
					String docTopicDiagFn = config.getDocumentTopicDiagnosticsOutputFilename();
					PrintWriter out = new PrintWriter(lgDir.getAbsolutePath() + "/" + docTopicDiagFn);
					out.println(tmd.topicsToCsv());
					out.flush();
					out.close();
				}

				if(config.saveDocumentThetaEstimate()) {
					String docTopicThetaFn = config.getDocumentTopicThetaOutputFilename();
					double [][] means = model.getThetaEstimate();
					LDAUtils.writeASCIIDoubleMatrix(means, lgDir.getAbsolutePath() + "/" + docTopicThetaFn, ",");
				}

				if(model instanceof LDASamplerWithPhi) {
					LDASamplerWithPhi modelWithPhi = (LDASamplerWithPhi) model;
					if(config.savePhiMeans(LDAConfiguration.SAVE_PHI_MEAN_DEFAULT)) {
						String docTopicMeanFn = config.getPhiMeansOutputFilename();
						double [][] means = modelWithPhi.getPhiMeans();
						if(means!=null) {
						LDAUtils.writeASCIIDoubleMatrix(means, lgDir.getAbsolutePath() + "/" + docTopicMeanFn, ",");
						} else {
							System.err.println("WARNING: ParallelLDA: No Phi means where sampled, not saving Phi means! This is likely due to a combination of configuration settings of phi_mean_burnin, phi_mean_thin and save_phi_mean");
						}
						// No big point in saving Phi without the vocabulary
						String vocabFn = config.getVocabularyFilename();
						if(vocabFn==null || vocabFn.length()==0) { vocabFn = "phi_vocabulary.txt"; }
						String [] vobaculary = LDAUtils.extractVocabulaty(instances.getDataAlphabet());
						LDAUtils.writeStringArray(vobaculary,lgDir.getAbsolutePath() + "/" + vocabFn);
					}
				}

				if(config.saveVocabulary(false)) {
					String vocabFn = config.getVocabularyFilename();
					String [] vobaculary = LDAUtils.extractVocabulaty(instances.getDataAlphabet());
					LDAUtils.writeStringArray(vobaculary,lgDir.getAbsolutePath() + "/" + vocabFn);
				}
				
				if(config.saveTermFrequencies(false)) {
					String termCntFn = config.getTermFrequencyFilename();
					int [] freqs = LDAUtils.extractTermCounts(instances);
					LDAUtils.writeIntArray(freqs, lgDir.getAbsolutePath() + "/" + termCntFn);
				}
				
				if(config.saveDocLengths(false)) {
					String docLensFn = config.getDocLengthsFilename();
					int [] freqs = LDAUtils.extractDocLength(instances);
					LDAUtils.writeIntArray(freqs, lgDir.getAbsolutePath() + "/" + docLensFn);
					
				}
				
				List<String> metadata = new ArrayList<String>();
				metadata.add("No. Topics: " + model.getNoTopics());
				metadata.add("Start Seed: " + model.getStartSeed());
				// Save stats for this run
				lu.dynamicLogRun("Runs", t, cp, (Configuration) config, null, 
						ParallelLDA.class.getName(), "Convergence", "HEADING", "PLDA", 1, metadata);
				
				if(requestedWords>instances.getDataAlphabet().size()) {
					requestedWords = instances.getDataAlphabet().size();
				}
				
				PrintWriter out = new PrintWriter(lgDir.getAbsolutePath() + "/TopWords.txt");
				out.println(LDAUtils.formatTopWordsAsCsv(
						LDAUtils.getTopWords(requestedWords, 
								model.getAlphabet().size(), 
								model.getNoTopics(), 
								model.getTypeTopicMatrix(), 
								model.getAlphabet())));
				out.flush();
				out.close();
				
				out = new PrintWriter(lgDir.getAbsolutePath() + "/RelevanceWords.txt");
				out.println(LDAUtils.formatTopWordsAsCsv(
						LDAUtils.getTopRelevanceWords(requestedWords, 
								model.getAlphabet().size(), 
								model.getNoTopics(), 
								model.getTypeTopicMatrix(),  
								config.getBeta(LDAConfiguration.BETA_DEFAULT),
								config.getLambda(LDAConfiguration.LAMBDA_DEFAULT), 
								model.getAlphabet())));
				out.flush();
				out.close();

				System.out.println("Top words are: \n" + 
						LDAUtils.formatTopWords(LDAUtils.getTopRelevanceWords(20, 
								model.getAlphabet().size(), 
								model.getNoTopics(), 
								model.getTypeTopicMatrix(),  
								config.getBeta(LDAConfiguration.BETA_DEFAULT),
								config.getLambda(LDAConfiguration.LAMBDA_DEFAULT), 
								model.getAlphabet())));
				System.out.println("Relevance words are: \n" + 
						LDAUtils.formatTopWords(LDAUtils.getTopRelevanceWords(20, 
								model.getAlphabet().size(), 
								model.getNoTopics(), 
								model.getTypeTopicMatrix(),  
								config.getBeta(LDAConfiguration.BETA_DEFAULT),
								config.getLambda(LDAConfiguration.LAMBDA_DEFAULT), 
								model.getAlphabet())));
//				System.out.println("Salient words are: \n" + 
//						LDAUtils.formatTopWords(LDAUtils.getTopSalientWords(20, 
//								model.getAlphabet().size(), 
//								model.getNoTopics(), 
//								model.getTypeTopicMatrix(),  
//								config.getBeta(LDAConfiguration.BETA_DEFAULT),
//								model.getAlphabet())));
//				System.out.println("KR1 re-weighted words are: \n" + 
//						LDAUtils.formatTopWords(LDAUtils.getK1ReWeightedWords(20, 
//								model.getAlphabet().size(), 
//								model.getNoTopics(), 
//								model.getTypeTopicMatrix(),  
//								config.getBeta(LDAConfiguration.BETA_DEFAULT),
//								model.getAlphabet())));
				
				if(model instanceof HDPSamplerWithPhi) {
					HDPSamplerWithPhi modelWithPhi = (HDPSamplerWithPhi) model;
					System.out.println("Topic Occurence Count:");
					System.out.println(Arrays.toString(modelWithPhi.getTopicOcurrenceCount()));
					LDAUtils.writeIntArray(modelWithPhi.getTopicOcurrenceCount(), lgDir.getAbsolutePath() + "/TopicOccurenceCount.csv");
					System.out.println("Active topics:");
					List<Integer> activeTopicHistoryList = modelWithPhi.getActiveTopicHistory();
					System.out.println(activeTopicHistoryList);
					LDAUtils.writeString(activeTopicHistoryList.toString().substring(1, activeTopicHistoryList.toString().length()-1), lgDir.getAbsolutePath() + "/ActiveTopics.csv");
					System.out.println("Active topics in data:");
					List<Integer> activeTopicInDataHistory = modelWithPhi.getActiveTopicInDataHistory();
					System.out.println(activeTopicInDataHistory);
					LDAUtils.writeString(activeTopicInDataHistory.toString().substring(1, activeTopicInDataHistory.toString().length()-1), 
							lgDir.getAbsolutePath() + "/ActiveTopicsInData.csv");
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
	}

	public static LDAGibbsSampler createModel(LDAConfiguration config, String whichModel) {
		LDAGibbsSampler model;
		switch(whichModel) {
		case "adlda": {
			model = new ADLDA(config);
			System.out.println("ADLDA.");
			break;
		}
		case "uncollapsed": {
			model = new UncollapsedParallelLDA(config);
			System.out.println("Uncollapsed Parallell LDA.");
			break;
		}
		case "collapsed": {
			model = new SerialCollapsedLDA(config);
			System.out.println("Uncollapsed Parallell LDA.");
			break;
		}
		case "lightcollapsed": {
			model = new CollapsedLightLDA(config);
			System.out.println("CollapsedLightLDA Parallell LDA.");
			break;
		}
		case "efficient_uncollapsed": {
			model = new EfficientUncollapsedParallelLDA(config);
			System.out.println("EfficientUncollapsedParallelLDA Parallell LDA.");
			break;
		}
		case "spalias": {
			model = new SpaliasUncollapsedParallelLDA(config);
			System.out.println("SpaliasUncollapsed Parallell LDA.");
			break;
		}
		case "polyaurn": {
			model = new PolyaUrnSpaliasLDA(config);
			System.out.println("PolyaUrnSpaliasLDA Parallell LDA.");
			break;
		}
		case "ppu_hlda": {
			model = new PoissonPolyaUrnHLDA(config);
			System.out.println("PoissonPolyaUrnHLDA Parallell LDA.");
			break;
		}
		case "ppu_hdplda": {
			model = new PoissonPolyaUrnHDPLDA(config);
			System.out.println("PoissonPolyaUrnHLDA Parallell LDA.");
			break;
		}
		case "spalias_priors": {
			model = new SpaliasUncollapsedParallelWithPriors(config);
			System.out.println("SpaliasUncollapsed Parallell LDA with Priors.");
			break;
		}
		case "lightpclda": {
			model = new LightPCLDA(config);
			System.out.println("Light PC LDA.");
			break;
		}
		case "lightpcldaw2": {
			model = new LightPCLDAtypeTopicProposal(config);
			System.out.println("Light PC LDA with proposal 2.");
			break;
		}
		case "nzvsspalias": {
			model = new NZVSSpaliasUncollapsedParallelLDA(config);
			System.out.println("NZVSSpaliasUncollapsedParallelLDA Parallell LDA.");
			break;
		}
		default : {
			System.out.println("Invalid model type. Aborting");
			return null;
		}
		}
		return model;
	}
}	
