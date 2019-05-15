package cc.mallet.topics.tui;

import java.io.File;
import java.io.FileNotFoundException;

import org.apache.commons.cli.ParseException;
import org.apache.commons.configuration.ConfigurationException;

import cc.mallet.configuration.ConfigFactory;
import cc.mallet.configuration.LDACommandLineParser;
import cc.mallet.configuration.LDAConfiguration;
import cc.mallet.configuration.ParsedLDAConfiguration;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.util.LDAUtils;
import cc.mallet.util.LoggingUtils;

public class SvmLightExporter {

	public static void main(String[] args) throws FileNotFoundException, ParseException, ConfigurationException {
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
			File lgDir = lu.getLogDir();

			String [] configs = config.getSubConfigs();
			for(String conf : configs) {
				lu.checkCreateAndSetSubLogDir(conf);
				config.activateSubconfig(conf);

				System.out.println("Using Config: " + config.whereAmI());
				System.out.println("Runnin subconfig: " + conf);
				String dataset_fn = config.getDatasetFilename();
				System.out.println("# topics: " + config.getNoTopics(-1));
				System.out.println("Using dataset: " + dataset_fn);
				if(config.getTestDatasetFilename()!=null) {
					System.out.println("Using TEST dataset: " + config.getTestDatasetFilename());
				}
				String whichModel = config.getScheme();
				System.out.println("Scheme: " + whichModel);

				InstanceList instances = LDAUtils.loadDataset(config, dataset_fn);
				//writeSvnLight(instances,lgDir.getAbsolutePath(),"cgcbib.corpus", "cgcbib.vocab");
				writeTokensPerRow(instances,lgDir.getAbsolutePath(),conf + "-corpus.txt");
				String vocabFn = conf + "-vocabulary.txt";
				String [] vobaculary = LDAUtils.extractVocabulaty(instances.getDataAlphabet());
				LDAUtils.writeStringArray(vobaculary,lgDir.getAbsolutePath() + "/" + vocabFn);

			}
		}
	}

	public static void writeSvnLight(InstanceList instances, String targetDir, String corpusFn) {
		String [] smvLightInstances = new String[instances.size()];
		int sidx = 0;
		for(Instance instance : instances) {
			smvLightInstances[sidx++] = LDAUtils.instanceToSvmLightString(instance, -1);
		}
		LDAUtils.writeStringArray(smvLightInstances,targetDir + "/" + corpusFn);
	}
	
	public static void writeTokensPerRow(InstanceList instances, String targetDir, String corpusFn) {
		String [] stringInstances = new String[instances.size()];
		int sidx = 0;
		for(Instance instance : instances) {
			stringInstances[sidx++] = LDAUtils.instanceToTokenIndexString(instance, -1);
		}
		LDAUtils.writeStringArray(stringInstances,targetDir + "/" + corpusFn);
	}
}
