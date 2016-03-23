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
	
	// Alias Table Backmapping
	int [][] aliasBackMapping = new int[numTypes][numTopics];
	// typeTopic Backmapping to access given topics in O(1)
	// The matrix is of size types*topics with the position [type][k] indicating the
	// index for topic k in typeTopicCounts so typeTopicCounts[type][typeTopicCountsBackmapping[k]] 
	// will access the true counts.
	// TODO: Leif we need this to sample in O(1) right?
	// TODO: This needs to be copied to each worker that will edit it.
	int [][] typeTopicCountsBackmapping = new int[numTypes][numTopics];
	
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
			// TODO: Check that this uses bubble typeTopicCounts
			// TODO: Go through with Leif.
			
			// Loop over nonzero topic counts to identify no of nonzero 
			// topics and calculate normalization.
			int first_zero_idx = 0;
			double topicMass = 0;
	        while (typeTopicCounts[type][first_zero_idx] > 0) {
	        	// Add the topic counts
	        	topicMass =+ typeTopicCounts[type][first_zero_idx] >> topicBits;
	            first_zero_idx++;
	        }
	        // Create probabilities to use in Alias table
			double [] probs = new double[first_zero_idx - 1];

			// TODO: Check with Leif, is this alpha in alpha*phi?
			double typeMass = 1.0; // Type prior mass set to 1.0 in this situation.

			for (int i = 0; i < first_zero_idx; i++) {
				probs[i] = typeTopicCounts[type][i] / (double) topicMass;
				// Set the backmapping table with the topic (from Alias table to topic)
				aliasBackMapping[type][i] = typeTopicCounts[type][i] & topicMask;
			}

			// TODO: Check: I now assume that the aliasTable return the position in prob.
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
}
