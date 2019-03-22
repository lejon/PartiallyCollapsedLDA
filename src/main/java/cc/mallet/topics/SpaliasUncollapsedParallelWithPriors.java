package cc.mallet.topics;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

import cc.mallet.configuration.LDAConfiguration;
import cc.mallet.topics.randomscan.topic.AllWordsTopicIndexBuilder;
import cc.mallet.types.Alphabet;
import cc.mallet.types.ConditionalDirichlet;
import cc.mallet.types.FeatureSequence;
import cc.mallet.types.InstanceList;
import cc.mallet.types.LabelSequence;
import cc.mallet.util.LoggingUtils;


/**
 * Adds type priors (anchor words) to standard Spalias. Increases mem requirement with vocab x topics and
 * requires access to this PhiPrior in each topic indicator draw possibly reducing cache performance. 
 * 
 * @author Leif Jonsson
 *
 */
public class SpaliasUncollapsedParallelWithPriors extends SpaliasUncollapsedParallelLDA implements LDASamplerWithPhi, LDASamplerWithPriors {
	

	private static final long serialVersionUID = 1L;
	
	protected boolean haveTopicPriors = false;
	protected double[][] topicPriors;


	public SpaliasUncollapsedParallelWithPriors(LDAConfiguration config) {
		super(config);
	}
	
	@Override
	public void addInstances (InstanceList training) {
		alphabet = training.getDataAlphabet();
		numTypes = alphabet.size();
		initializePriors(config);
		super.addInstances(training);
	}

	
	@Override
	public double[][] getTopicPriors() {
		return topicPriors;
	}
	
	@Override
	public void initialSamplePhi(int [] indices, double[][] phiMatrix) {
		super.initialSamplePhi(indices, phiMatrix);
		if(haveTopicPriors) {
			for (int topic = 0; topic < phiMatrix.length; topic++) {
				for (int type = 0; type < phiMatrix[topic].length; type++) {
					phiMatrix[topic][type] *= topicPriors[topic][type];
				}
			}
		}
	}
	
	protected static double [][] calculatePriors(String topicPriorFilename, int numTopics, 
			int numTypes, Alphabet alphabet) throws IOException {
		Map<String,Boolean> issuedWarnings = new HashMap<String, Boolean>();
		double [][] priors = new double[numTopics][numTypes];
		for (int i = 0; i < priors.length; i++) {			
			Arrays.fill(priors[i], 1.0);
		}
		List<String> lines = Files.readAllLines(Paths.get(topicPriorFilename), Charset.defaultCharset());
		@SuppressWarnings("rawtypes")
		Collection [] zeroOut = extractPriorSpec(lines, numTopics);
		for (int topic = 0; topic < zeroOut.length; topic++) {
			for (Object wordToZero : zeroOut[topic]) {
				String word = wordToZero.toString().trim();
				int wordIdx = alphabet.lookupIndex(word,false);
				if( wordIdx < 0) {
					if(issuedWarnings.get(word) == null || !issuedWarnings.get(word)) {
						System.err.println("WARNING: UncollapsedParallelLDA.calculatePriors: Word \"" + word + "\" does not exist in the dictionary!");
						issuedWarnings.put(word, Boolean.TRUE);
					}
					continue;
				}
				priors[topic][wordIdx] = 0.0;
			}
		}
		ensureConsistentPriors(priors,alphabet);
		return priors;
	}
	
	protected static void ensureConsistentPriors(double[][] priors, Alphabet alphabet) {
		double [] colsum = new double[priors[0].length];
		for (int i = 0; i < priors.length; i++) {
			double rowsum = 0;
			for (int j = 0; j < priors[i].length; j++) {
				rowsum += priors[i][j];
				colsum[j] += priors[i][j];
			}
			if(rowsum==0.0) throw new IllegalArgumentException("Inconsistent prior spec, one topic has all Zero priors!");
		}
		List<String> zeroWords = new ArrayList<String>();
		for (int i = 0; i < colsum.length; i++) {			
			if(colsum[i]==0.0) {
				String word = alphabet.lookupObject(i).toString();
				zeroWords.add(word);
			}
		}
		if(zeroWords.size()>0)
			throw new IllegalArgumentException("Inconsistent prior spec, '" + zeroWords + "' has all Zero priors!");
	}

	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	static Collection [] extractPriorSpec(List<String> lines, int numTopics) {
		TreeSet [] toZeroOut = new TreeSet[numTopics];
		TreeSet [] toKeep = new TreeSet[numTopics];
		for (int i = 0; i < numTopics; i++) {
			toZeroOut[i] = new TreeSet<String>();
		}
		for (int i = 0; i < numTopics; i++) {
			toKeep[i] = new TreeSet<String>();
		}
		
		// Each line will be in the format topic, word1, word2, word3 ...
		for (String string : lines) {
			// Skip comments
			if(string.trim().startsWith("#")) continue;
			// Skip empty lines
			if(string.trim().length()==0) continue;
			String [] spec = string.split(",");
			// First find the topic we are specifying, it is stored first
			int currentTopic;
			try {
				currentTopic = Integer.parseInt(spec[0]);
			} catch (NumberFormatException e) {
				System.err.println("Cant extract topic number from: " + spec[0]);
				throw new IllegalArgumentException(e);
			}
			for (int i = 1; i < spec.length; i++) {
				String word = spec[i];
				for (int topic = 0; topic < numTopics; topic++) {
					if(topic==currentTopic) {
						toKeep[topic].add(word);
					} else {
						toZeroOut[topic].add(word);
					}
				}
			}
		}
		
		for (int topic = 0; topic < numTopics; topic++) {
			toZeroOut[topic].removeAll(toKeep[topic]);
		}
		
		
		return toZeroOut;
	}
	
	protected void initializePriors(LDAConfiguration config) {
		if(config.getTopicPriorFilename()!=null) {
			try {
				topicPriors = calculatePriors(config.getTopicPriorFilename(), numTopics, numTypes, alphabet);
				haveTopicPriors = true;
			} catch (IOException e) {
				e.printStackTrace();
				throw new IllegalArgumentException(e);
			}
			if(logger.getLevel()==Level.INFO) {
				System.out.println("UncollapsedParallelLDA: Set priors from: " + config.getTopicPriorFilename());
			}
		} else {
			double [][] priors = new double[numTopics][numTypes];
			for (int i = 0; i < priors.length; i++) {			
				Arrays.fill(priors[i], 1.0);
			}
			topicPriors = priors;
		}
	}
	
	
	@Override
	protected LDADocSamplingResult sampleTopicAssignmentsParallel(LDADocSamplingContext ctx) {
		FeatureSequence tokens = ctx.getTokens();
		LabelSequence topics = ctx.getTopics();
		int myBatch = ctx.getMyBatch();
		
		int type, oldTopic, newTopic;

		final int docLength = tokens.getLength();
		if(docLength==0) return null;
		
		int [] tokenSequence = tokens.getFeatures();
		int [] oneDocTopics = topics.getFeatures();

		int[] localTopicCounts = new int[numTopics];

		// This vector contains the indices of the topics with non-zero entries.
		// It has to be numTopics long since the non-zero topics come and go...
		int [] nonZeroTopics = new int[numTopics];

		// So we can map back from a topic to where it is in nonZeroTopics vector
		int [] nonZeroTopicsBackMapping = new int[numTopics];
		
		// Populate topic counts
		int nonZeroTopicCnt = 0;
		for (int position = 0; position < docLength; position++) {
			int topicInd = oneDocTopics[position];
			localTopicCounts[topicInd]++;
			if(localTopicCounts[topicInd]==1) {
				nonZeroTopicCnt = insert(topicInd, nonZeroTopics, nonZeroTopicsBackMapping, nonZeroTopicCnt);
			}
		}
		
		//kdDensities[myBatch] += nonZeroTopicCnt;
		kdDensities.addAndGet(nonZeroTopicCnt);
		
		double sum; // sigma_likelihood
		double[] cumsum = new double[numTopics]; 

		//	Iterate over the words in the document
		for (int position = 0; position < docLength; position++) {
			type = tokenSequence[position];
			oldTopic = oneDocTopics[position]; // z_position
			localTopicCounts[oldTopic]--;

			// Potentially update nonZeroTopics mapping
			if(localTopicCounts[oldTopic]==0) {
				nonZeroTopicCnt = remove(oldTopic, nonZeroTopics, nonZeroTopicsBackMapping, nonZeroTopicCnt);
			}

			if(localTopicCounts[oldTopic]<0) 
				throw new IllegalStateException("Counts cannot be negative! Count for topic:" 
						+ oldTopic + " is: " + localTopicCounts[oldTopic]);

			// Propagates the update to the topic-token assignments
			/**
			 * Used to subtract and add 1 to the local structure containing the number of times
			 * each token is assigned to a certain topic. Called before and after taking a sample
			 * topic assignment z
			 */
			decrement(myBatch, oldTopic, type);
			//System.out.println("(Batch=" + myBatch + ") Decremented: topic=" + oldTopic + " type=" + type + " => " + batchLocalTopicUpdates[myBatch][oldTopic][type]);
			 
			int topic = nonZeroTopics[0];
			double score = localTopicCounts[topic] * phi[topic][type];
			cumsum[0] = score;
			// Now calculate and add up the scores for each topic for this word
			// We build a cumsum indexed by topicIndex
			int topicIdx = 1;
			while ( topicIdx < nonZeroTopicCnt ) {
				topic = nonZeroTopics[topicIdx];
				score = localTopicCounts[topic] * phi[topic][type] * topicPriors[topic][type];
				cumsum[topicIdx] = score + cumsum[topicIdx-1];
				topicIdx++;
			}
			sum = cumsum[topicIdx-1]; // sigma_likelihood

			// Choose a random point between 0 and the sum of all topic scores
			// The thread local random performs better in concurrent situations 
			// than the standard random which is thread safe and incurs lock 
			// contention
			double u = ThreadLocalRandom.current().nextDouble();
			double u_sigma = u * (typeNorm[type] + sum);
			// u ~ U(0,1)  
			// u [0,1]
			// u_sigma = u * (typeNorm[type] + sum)
			// if u_sigma < typeNorm[type] -> prior
			// u * (typeNorm[type] + sum) < typeNorm[type] => u < typeNorm[type] / (typeNorm[type] + sum)
			// else -> likelihood
			// u_prior = u_sigma / typeNorm[type] -> u_prior (0,1)
			// u_likelihood = (u_sigma - typeNorm[type]) / sum  -> u_likelihood (0,1)

			newTopic = sampleNewTopic(type, nonZeroTopics, nonZeroTopicCnt, sum, cumsum, u, u_sigma);

			// Make sure we actually sampled a valid topic
			if (newTopic < 0 || newTopic > numTopics) {
				throw new IllegalStateException ("SpaliasUncollapsedParallelLDA: New valid topic not sampled (" + newTopic + ").");
			}

			// Put that new topic into the counts
			oneDocTopics[position] = newTopic;
			localTopicCounts[newTopic]++;

			// Potentially update nonZeroTopics mapping
			if(localTopicCounts[newTopic]==1) {
				nonZeroTopicCnt = insert(newTopic, nonZeroTopics, nonZeroTopicsBackMapping, nonZeroTopicCnt);
			}

			// Propagates the update to the topic-token assignments
			/**
			 * Used to subtract and add 1 to the local structure containing the number of times
			 * each token is assigned to a certain topic. Called before and after taking a sample
			 * topic assignment z
			 */
			increment(myBatch, newTopic, type);
			//System.out.println("(Batch=" + myBatch + ") Incremented: topic=" + newTopic + " type=" + type + " => " + batchLocalTopicUpdates[myBatch][newTopic][type]);		
		}
		//System.out.println("Ratio: " + ((double)numPrior/(double)numLikelihood));
		return new LDADocSamplingResultSparseSimple(localTopicCounts,nonZeroTopicCnt,nonZeroTopics);
	}
	
	/**
	 * Samples new Phi's. If <code>topicTypeIndices</code> is NOT null it will sample phi conditionally
	 * on the indices in <code>topicTypeIndices</code>. This version takes topic priors into consideration
	 * 
	 * @param indices indices of the topics that should be sampled, the other ones are skipped 
	 * @param topicTypeIndices matrix containing the indices of the types that should be sampled (per topic)
	 * @param phiMatrix
	 */
	@Override
	public void loopOverTopics(int [] indices, int[][] topicTypeIndices, double[][] phiMatrix) {
		long beforeSamplePhi = System.currentTimeMillis();		
		for (int topic : indices) {
			int [] relevantTypeTopicCounts = topicTypeCountMapping[topic]; 
			// Generates a standard array to feed to the Dirichlet constructor
			// from the dictionary representation. 
			if(topicTypeIndices==null && !haveTopicPriors ) {
				phiMatrix[topic] = dirichletSampler.nextDistribution(relevantTypeTopicCounts);
			} else {
				double[] dirichletParams = new double[numTypes];
				for (int type = 0; type < numTypes; type++) {
					int thisCount = relevantTypeTopicCounts[type];
					dirichletParams[type] = beta + thisCount; 
				}
				
				if(topicTypeIndices==null) {
					topicTypeIndices = AllWordsTopicIndexBuilder.getAllIndicesMatrix(numTypes, numTopics);
				}
				int[] typeIndicesToSample = topicTypeIndices[topic];
				
				// If we have priors, remove any type in this topic that has zero probability
				if(haveTopicPriors) {
					List<Integer> mergedIndexList = new ArrayList<Integer>();
					double [] thisTopicPriors = topicPriors[topic];
					for (int type = 0; type < typeIndicesToSample.length; type++) {
						if(thisTopicPriors[type]!=0.0) {
							mergedIndexList.add(typeIndicesToSample[type]);
						}
					}
					int [] newTypeIndicesToSample = new int[mergedIndexList.size()];
					for (int i = 0; i < mergedIndexList.size(); i++) {
						newTypeIndicesToSample[i] = mergedIndexList.get(i);
					}
					typeIndicesToSample = newTypeIndicesToSample;
				}
								
				ConditionalDirichlet dist = new ConditionalDirichlet(dirichletParams);
				double [] newPhi = dist.nextConditionalDistribution(phiMatrix[topic],typeIndicesToSample); 
				
				phiMatrix[topic] = newPhi;
			}
			if(savePhiMeans() && samplePhiThisIteration()) {
				for (int phi = 0; phi < phiMatrix[topic].length; phi++) {
					phiMean[topic][phi] += phiMatrix[topic][phi];
				}
			}
		}
		long elapsedMillis = System.currentTimeMillis();
		long threadId = Thread.currentThread().getId();

		if(measureTimings) {
			PrintWriter pw = LoggingUtils.checkCreateAndCreateLogPrinter(
					config.getLoggingUtil().getLogDir() + "/timing_data",
					"thr_" + threadId + "_Phi_sampling.txt");
			pw.println(beforeSamplePhi + "," + elapsedMillis);
			pw.flush();
			pw.close();
		}
	}
}
