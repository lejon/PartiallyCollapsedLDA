package cc.mallet.topics.tui;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.Random;

import org.apache.commons.configuration.ConfigurationException;

import cc.mallet.configuration.ConfigFactory;
import cc.mallet.configuration.LDACommandLineParser;
import cc.mallet.configuration.LDAConfiguration;
import cc.mallet.topics.SpaliasUncollapsedParallelLDA;
import cc.mallet.types.CrossValidationIterator;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.util.LDAUtils;
import cc.mallet.util.LoggingUtils;

public class XValidationCreator {

	public static void main(String[] mainargs) throws Exception {
		// Code review by Mans Magnusson 2015-11-03
		
		int folds = Integer.parseInt(mainargs[mainargs.length-1]);
		
		// Copy the args except the folds argument
		String [] args = new String[mainargs.length-1];
		for (int i = 0; i < args.length; i++) {
			args[i] = mainargs[i];
		}
		
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

			createXValidationDataset(instances, folds, config);
			
		}
		if(buildVer==null||implVer==null) {
			System.out.println("GIT info:" + LoggingUtils.getLatestCommit());
		} else {
		System.out.println("Build info:" 
				+ "Implementation-Build = " + buildVer + ", " 
				+ "Implementation-Version = " + implVer);
		}
	}
	
	public static void createXValidationDataset(InstanceList instances, int folds, LDAConfiguration config) throws Exception {
		// Code review by Mans Magnusson 2015-11-03
		Random r = new Random ();
		int TRAINING = 0;
		int TESTING = 1;
		CrossValidationIterator cvIter = new CrossValidationIterator(instances, folds, r);
		InstanceList[] cvSplit = null;

		String basedir = config.getLoggingUtil().getLogDir().getName();
		for (int fold = 0; fold < folds; fold++) {
			cvSplit = cvIter.next();
			config.getLoggingUtil().checkCreateAndSetSubLogDir(basedir + "-" + (fold+1));
			SpaliasUncollapsedParallelLDA spalias = sampleTrainingset(cvSplit[TRAINING], config);
			sampleTestset(cvSplit[TESTING], cvSplit[TRAINING], spalias.getPhiMeans(), config); 
		}
	}

	protected static SpaliasUncollapsedParallelLDA sampleTrainingset(InstanceList trainingInstances, LDAConfiguration config) throws ConfigurationException, IOException {
		SpaliasUncollapsedParallelLDA spalias = new SpaliasUncollapsedParallelLDA(config);
		int commonSeed = config.getSeed(LDAConfiguration.SEED_DEFAULT);
		spalias.addInstances(trainingInstances);
		spalias.setRandomSeed(commonSeed);

		System.out.println("Starting iterations (" + config.getNoIterations(LDAConfiguration.NO_ITER_DEFAULT) + " total).");
		System.out.println("_____________________________\n");

		// Runs the model
		System.out.println("Starting:" + new Date());
		spalias.sample(config.getNoIterations(LDAConfiguration.NO_ITER_DEFAULT));
		System.out.println("Finished:" + new Date());
		
		String docTopicMeanFn = config.getDocumentTopicMeansOutputFilename();
		double [][] dtMeans = spalias.getZbar();
		LDAUtils.writeASCIIDoubleMatrix(dtMeans, config.getLoggingUtil().getLogDir().getAbsolutePath() + "/train-" + docTopicMeanFn, ",");

		String phiMeanFn = config.getPhiMeansOutputFilename();
		double [][] phiMeans = spalias.getPhiMeans();
		LDAUtils.writeASCIIDoubleMatrix(phiMeans, config.getLoggingUtil().getLogDir().getAbsolutePath() + "/train-" + phiMeanFn, ",");

		PrintWriter out = new PrintWriter(config.getLoggingUtil().getLogDir().getAbsolutePath() + "/train-ids.txt");
		String [] ids = extractRowIds(trainingInstances);
		for (int i = 0; i < ids.length; i++) {
			out.println(ids[i]);			
		}
		out.flush();
		out.close();
		
		//LDAUtils.perplexity(conf, testSet, topicTypeCounts, phi);

		return spalias;
	}

	
	protected static SpaliasUncollapsedParallelLDA sampleTestset(InstanceList testInstances, InstanceList trainingInstances, double[][] phi, LDAConfiguration config) throws ConfigurationException, IOException {
		SpaliasUncollapsedParallelLDA spalias = new SpaliasUncollapsedParallelLDA(config);
		spalias.addInstances(testInstances);
		double [][] phiCopy = new double[phi.length][phi[0].length];
		for (int i = 0; i < phiCopy.length; i++) {
			for (int j = 0; j < phiCopy[i].length; j++) {
				phiCopy[i][j] = phi[i][j];
			}
		}
		spalias.setPhi(phiCopy,trainingInstances.getAlphabet(), trainingInstances.getTargetAlphabet());
		spalias.sampleZGivenPhi(config.getNoIterations(LDAConfiguration.NO_ITER_DEFAULT));
		String docTopicMeanFn = config.getDocumentTopicMeansOutputFilename();
		
		double [][] dtMeans = spalias.getZbar();
		LDAUtils.writeASCIIDoubleMatrix(dtMeans, config.getLoggingUtil().getLogDir().getAbsolutePath() + "/test-" + docTopicMeanFn, ",");
		
		PrintWriter out = new PrintWriter(config.getLoggingUtil().getLogDir().getAbsolutePath() + "/test-ids.txt");
		String [] ids = extractRowIds(testInstances);
		for (int i = 0; i < ids.length; i++) {
			out.println(ids[i]);			
		}
		out.flush();
		out.close();
		return spalias;
	}
	
	public static String [] extractRowIds(InstanceList dataset) {
		String [] result = new String[dataset.size()];
		int copied = 0;
		for(Instance instance : dataset) {
			result[copied++] = instance.getName().toString();
		}
		return result;
	}


}
