package cc.mallet.topics;

import gnu.trove.TIntIntHashMap;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import cc.mallet.configuration.LDAConfiguration;
import cc.mallet.topics.SpaliasUncollapsedParallelLDA.TableBuildResult;
import cc.mallet.types.InstanceList;
import cc.mallet.util.LoggingUtils;
import cc.mallet.util.MalletLogger;
import cc.mallet.util.OptimizedGentleAliasMethod;
import cc.mallet.util.Randoms;
import cc.mallet.util.WalkerAliasTable;


public class CollapsedLightLDA extends ADLDA implements LDAGibbsSampler {

	private static final long serialVersionUID = 1L;
	
	private static Logger logger = MalletLogger.getLogger(ADLDA.class.getName());
	
	boolean measureTimings = false;

	int [] deltaNInterval;
	String dNOutputFn;
	DataOutputStream deltaOutput;
	TIntIntHashMap [] globalDeltaNUpdates;

	int corpusWordCount = 0;

	protected boolean	debug;

	AtomicInteger kdDensities = new AtomicInteger();
	long [] zTimings;
	long [] countTimings;
	
	private ExecutorService tableBuilderExecutor;
	protected TableBuilderFactory tbFactory = new LightLDATableBuilderFactory();
	
	WalkerAliasTable [] aliasTables; 
	double [] typeNorm; 
	
	// Sparse matrix structure
	// This 2D-array contains the indices of the topics with non-zero entries.
	// It has to be numTopics long since the non-zero topics come and go...
	int [][] nonZeroTypeTopics = new int[numTypes][numTopics];
	// So we can map back from a topic to where it is in nonZeroTopics vector
	int [][] nonZeroTypeTopicsBackMapping = new int[numTypes][numTopics];
	// Populate sparse global topic counts
	int[] nonZeroTypeTopicCnt = new int[numTypes];	
	
	public CollapsedLightLDA(LDAConfiguration config) {
		super(config);
		
		// With job stealing we can only have one global z / counts timing
		zTimings = new long[1];
		countTimings = new long[1];

		debug = config.getDebug();
		measureTimings = config.getMeasureTiming();

		int  [] defaultVal = {-1};
		deltaNInterval = config.getIntArrayProperty("dn_diagnostic_interval",defaultVal);
		if(deltaNInterval.length > 1) {
			dNOutputFn = LoggingUtils.checkCreateAndCreateDir(config.getLoggingUtil().getLogDir().getAbsolutePath() 
					+ "/delta_n").getAbsolutePath();
			dNOutputFn += "/DeltaNs" + "_noDocs_" + data.size() + "_vocab_" 
					+ numTypes + "_iter_" + currentIteration + ".BINARY";
			try {
				deltaOutput = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(dNOutputFn)));
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				throw new IllegalArgumentException(e);
			}
		}

		globalDeltaNUpdates = new TIntIntHashMap[numTopics];
		for (int i = 0; i < globalDeltaNUpdates.length; i++) {
			globalDeltaNUpdates[i] = new TIntIntHashMap();
		}
	}
	
	MyWorkerRunnable getWorkerSingle(int docsPerThread, int offset, Randoms random) {
		return new LightLDAWorkerRunnable(numTopics,
				alpha, alphaSum, beta,
				random, data,
				typeTopicCounts, tokensPerTopic, typeTotals,
				offset, docsPerThread, aliasTables);
	}

	MyWorkerRunnable getWorker(int docsPerThread, int offset, int[] runnableTotals, int[][] runnableCounts,
			Randoms random) {
		return new LightLDAWorkerRunnable(numTopics,
				alpha, alphaSum, beta,
				random, data,
				runnableCounts, runnableTotals, typeTotals,
				offset, docsPerThread, aliasTables);
	}

	MyWorkerRunnable[] getInitialWorkers(int numThreads) {
		return new LightLDAWorkerRunnable[numThreads];
	}

	/**
	 * Imports the training instances and initializes the LDA model internals.
	 */
	@Override
	public void addInstances (InstanceList training) {		
		super.addInstances(training);
		aliasTables = new WalkerAliasTable[numTypes];
		typeNorm    = new double[numTypes];
		if(logger.getLevel()==Level.INFO) {
			System.out.println("Loaded " + data.size() + " documents, with " + corpusWordCount + " words in total.");
		}
	}

	class LightLDATableBuilderFactory implements TableBuilderFactory {
		public Callable<TableBuildResult> instance(int type) {
			return new LightLDAParallelTableBuilder(type);
		}
	}

	class LightLDAParallelTableBuilder implements Callable<TableBuildResult> {
		int type;
		public LightLDAParallelTableBuilder(int type) {
			this.type = type;
		}
		@Override
		public TableBuildResult call() {
			// TODO: I dont know to fix this, nonZeroTypeTopicCnt should be a variable that is reached.
			//double [] probs = new double[nonZeroTypeTopicCnt[type]];
			double [] probs = new double[typeTopicCounts[type].length];
			// int [] myTypeTopicCounts = typeTopicCounts[type];
			//int [] myNonZeroTypeTopics = nonZeroTypeTopics[type];
			
			// for (int i = 0; i < myTypeTopicCounts.length; i++) {
			// 	  topicMass += myTypeTopicCounts[i];
			//}
			
			// Iterate over nonzero topic indicators
			int topicMass = 0;
//			for (int i = 0; i < myNonZeroTypeTopics.length; i++) {
//			 	  topicMass += typeTopicCounts[type][myNonZeroTypeTopics[i]];
//			}
			for (int i = 0; i < typeTopicCounts[type].length; i++) {
			 	  topicMass += typeTopicCounts[type][i];
			}

			// for (int i = 0; i < myTypeTopicCounts.length; i++) {
			// 	typeMass += probs[i] = myTypeTopicCounts[i] / (double) topicMass;
			// }
			
			double typeMass = 0; // Type prior mass
//			for (int i = 0; i < myNonZeroTypeTopics.length; i++) {
//				typeMass += probs[i] = typeTopicCounts[type][myNonZeroTypeTopics[i]] / (double) topicMass;
//			}
			for (int i = 0; i < typeTopicCounts[type].length; i++) {
				typeMass += probs[i] = typeTopicCounts[type][i] / (double) topicMass;
			}

			// TODO: New tables in all iterations (different sizes)?
			// TODO: I now assume that the aliasTable return the position in prob.
			if(aliasTables[type]==null) {
				aliasTables[type] = new OptimizedGentleAliasMethod(probs,typeMass);
			} else {
				aliasTables[type].reGenerateAliasTable(probs, typeMass);
			}
						
			return new TableBuildResult(type, aliasTables[type], typeMass);
		}   
	}
	
	public void preIteration() {
		List<Callable<TableBuildResult>> builders = new ArrayList<>();
		for (int type = 0; type < numTypes; type++) {
			builders.add(tbFactory.instance(type));
		}
		
		List<Future<TableBuildResult>> results;
		try {
			results = tableBuilderExecutor.invokeAll(builders);
			for (Future<TableBuildResult> result : results) {
				aliasTables[result.get().type] = result.get().table;
				typeNorm[result.get().type] = result.get().typeNorm; // typeNorm is sigma_prior
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
			System.exit(-1);
		} catch (ExecutionException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	@Override
	public void postSample() {
		tableBuilderExecutor.shutdown();
		flushDeltaOut();
	}

	@Override
	public void preSample() {
		int  [] defaultVal = {-1};
		deltaNInterval = config.getIntArrayProperty("dn_diagnostic_interval", defaultVal);
		if(deltaNInterval.length > 1) {
			dNOutputFn = LoggingUtils.checkCreateAndCreateDir(config.getLoggingUtil().getLogDir().getAbsolutePath() 
					+ "/delta_n").getAbsolutePath();
			dNOutputFn += "/DeltaNs" + "_noDocs_" + data.size() + "_vocab_" 
					+ numTypes + "_iter_" + currentIteration + ".BINARY";
			try {
				deltaOutput = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(dNOutputFn)));
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				throw new IllegalArgumentException(e);
			}
		}
		int poolSize = 2;
		tableBuilderExecutor = Executors.newFixedThreadPool(Math.max(1, poolSize));
	}

	void flushDeltaOut() {
		if(deltaOutput!=null) {
			try {
				deltaOutput.flush();
				deltaOutput.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}
		}
	}
	
	protected void initNonZeroTypeTopic() {		
		for (int typeidx = 0; typeidx < numTypes; typeidx++) {
			for (int topicidx = 0; topicidx < numTopics; topicidx++) {
			if(typeTopicCounts[typeidx][topicidx] > 0){
				nonZeroTypeTopicCnt[typeidx] = insertNonZeroTopicTypes(topicidx, nonZeroTypeTopics[typeidx], nonZeroTypeTopicsBackMapping[typeidx], nonZeroTypeTopicCnt[typeidx]);
				}
			}
		}
	}
	
	protected static int insertNonZeroTopicTypes(int topic, int[] nonZeroTopics, int[] nonZeroTopicsBackMapping, int nonZeroTopicCnt) {
		// This function should only be called when the element typeTopicCounts[type][topic] increases from 0 to > 0.
		// We have a new non-zero topic put it in the last empty slot and increase the count
		nonZeroTopics[nonZeroTopicCnt] = topic;
		nonZeroTopicsBackMapping[topic] = nonZeroTopicCnt;
		return ++nonZeroTopicCnt;
	}
	
	protected static int removeNonZeroTopicTypes(int topic, int[] nonZeroTopics, int[] nonZeroTopicsBackMapping, int nonZeroTopicCnt) {
		// This function should only be called when the element typeTopicCounts[type][topic] decreases from > 0 to 0.
		// We remove the actual topic from the array and put the last element there instead.
		// Update the backmapping.
		int idx_nzt = nonZeroTopicsBackMapping[topic]; 
		nonZeroTopics[idx_nzt] = nonZeroTopics[nonZeroTopicCnt];
		nonZeroTopicsBackMapping[nonZeroTopics[nonZeroTopicCnt]] = idx_nzt;
		nonZeroTopicsBackMapping[topic] = -1;
		nonZeroTopics[nonZeroTopicCnt] = -1;
		return --nonZeroTopicCnt;
	}

	
}
