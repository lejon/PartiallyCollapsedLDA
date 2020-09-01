package cc.mallet.topics;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.configuration.ConfigurationException;

import cc.mallet.configuration.LDAConfiguration;
import cc.mallet.configuration.ParsedLDAConfiguration;
import cc.mallet.types.Alphabet;
import cc.mallet.types.BinomialSampler;
import cc.mallet.types.InstanceList;
import cc.mallet.types.LabelAlphabet;
import cc.mallet.types.VariableSelectionResult;
import cc.mallet.util.FileLoggingUtils;
import cc.mallet.util.IndexSorter;
import cc.mallet.util.LDALoggingUtils;
import cc.mallet.util.LoggingUtils;
import cc.mallet.util.MalletLogger;
import cc.mallet.util.ParallelRandoms;

/**
 * This is a parallel implementation of the Poisson Polya Urn HDP LDA
 * 
 * What this class adds on top of the PolyaUrn LDA is additional sampling
 * of the number of topics to use in each iteration. 
 * 
 * This version has no notion of active topics which it can sample from, 
 * rather it can sample from all topics (up to k (Aka k_max)). So in this
 * implementation numTopics should be set high (say 500)
 * 
 * @author Leif Jonsson
 *
 */
public class PoissonPolyaUrnHDPLDAInfiniteTopics extends SparseHDPSampler implements HDPSamplerWithPhi {

	{ 
		logger = MalletLogger.getLogger(PoissonPolyaUrnHDPLDAInfiniteTopics.class.getName());
	}

	private static final long serialVersionUID = 1L;

	int poissonNormalApproxThreshold;
	PsiSampler psiSampler;
	double gamma;
	
	// activeTopics stores the k_95's for post analysis  
	List<Integer> activeTopics = new ArrayList<>();
	
	// This is \alpha in paper, i.e. the concentration parameter within documents.
	double alphaCoef; 
	
	DocTopicTokenFreqTable docTopicTokenFreqTable; 
	int nrStartTopics;
	
	// numTopics is the same as K_max in paper, the maximum number of topics.
	
	double k_percentile;

	boolean[] deceasedTopics;

	public PoissonPolyaUrnHDPLDAInfiniteTopics(LDAConfiguration config) {
		super(config);
		
		gamma = config.getHDPGamma(LDAConfiguration.HDP_GAMMA_DEFAULT);
		k_percentile = config.getHDPKPercentile(LDAConfiguration.HDP_K_PERCENTILE);
		nrStartTopics = config.getHDPNrStartTopics(LDAConfiguration.HDP_START_TOPICS_DEFAULT);
		
		System.out.println("HDP gamma: " + gamma);
		System.out.println("HDP start topics: " + nrStartTopics);
		
		alphaCoef = config.getAlpha(LDAConfiguration.ALPHA_DEFAULT); 
		poissonNormalApproxThreshold = config.getAliasPoissonThreshold(LDAConfiguration.ALIAS_POISSON_DEFAULT_THRESHOLD); 
				
		// We should NOT do hyperparameter optimization of alpha or beta in the HDP
		// Let it be controlled by config setting
		//hyperparameterOptimizationInterval = -1;
		
		topicOcurrenceCount = new int[numTopics];
		deceasedTopics = new boolean[numTopics];
				
		psiSampler = new GEMBasedPsiSampler(gamma);
	}
		
	@Override
	public void addInstances(InstanceList training) {
		super.addInstances(training);
		docTopicTokenFreqTable = new DocTopicTokenFreqTable(numTopics,longestDocLength);
	}
	
	/* When we initialize Z we have to limit the topic indicators
	 * to nrStartTopics
	 * @see cc.mallet.topics.UncollapsedParallelLDA#initialDrawTopicIndicator()
	 */
	@Override
	int initialDrawTopicIndicator(int docIdx) {
		return random.nextInt(nrStartTopics);
	}
	
	/**
	 * This method can only be called from threads working
	 * on separate topics. It is not thread safe if several threads
	 * work on the same topic
	 * 
	 * This is overridden from Uncollapsed since we need to book keep
	 * when a topic (possibly temporarily) dies (i.e goes from having
	 * tokens assigned to it to not having any). In this case
	 * we sample Phi for that topic from the prior in loopOverTopics.
	 * 
	 * @param type
	 * @param topic
	 * @param count
	 */
	@Override
	protected void updateTypeTopicCount(int type, int topic, int count) {
		// Topic resurrected... yay! (not really)
		if(tokensPerTopic[topic]==0 && deceasedTopics[topic]) {
			// I.e if a topic has 0 tokens and it is changed it MUST increase
			// since it cannot be negative, and if it was then previously deceased
			// it is now resurrected
			deceasedTopics[topic] = false;
		}
		super.updateTypeTopicCount(type, topic, count);
		// Topic died... whimp...
		if(tokensPerTopic[topic]==0) {
			deceasedTopics[topic] = true;
		}
	}

	@Override
	public void postPhi() {
		super.postPhi();

		// This is a global parameter so normalization need to be done after psi_k has been sampled in parallel.
		psiSampler.finalizeSampling();
	}
		
	/**
	 * Re-arranges the topics in the typeTopic matrix based
	 * on tokensPerTopic
	 * 
	 * @param tokensPerTopic
	 */
	@Override
	protected void reArrangeTopics(int [] tokensPerTopic) {
		int [] sortedIndices =  IndexSorter.getSortedIndices(tokensPerTopic);
				
		for (int i = 0; i < sortedIndices.length; i++) {
			if(i != sortedIndices[i]) {
				moveTopic(sortedIndices[i], i);					
				docTopicTokenFreqTable.moveTopic(sortedIndices[i],i);
				sortedIndices = IndexSorter.getSortedIndices(this.tokensPerTopic);
			}
		}
	}
	
	@Override
	public void postSample() {
		super.postSample();
		tableBuilderExecutor.shutdown();
		reArrangeTopics(tokensPerTopic);
	}
	
	interface PsiSampler {
		/**
		 * Updates one topic given its sufficient statistic
		 * @param topic
		 * @param sufficientStatistic
		 */
		void updateTopic(int topic, int sufficientStatistic);
		void reset();
		void finalizeSampling();
		double [] getPsi();
		void incrementTopic(int i);
	}
	
	class GEMBasedPsiSampler implements PsiSampler, Serializable {
		private static final long serialVersionUID = 1L;
		double [] psi; // This is capital Psi in paper.
		int [] l;
		long lSum = -1;
		double gamma;
		
		public GEMBasedPsiSampler(double gamma) {
			this.gamma = gamma;
			l = new int[numTopics];
			
			psi = new double[numTopics];
			
			// Init psi by drawing from prior
			finalizeSampling();
//			for (int i = 0; i < nrStartTopics; i++) {
//				psi[i] = 1.0 / nrStartTopics;
//			}
		}
		
		@Override
		public void reset() {
			l = new int[numTopics];
			lSum = 0;
		}
		
		/* (non-Javadoc)
		 * @see cc.mallet.topics.PoissonPolyaUrnHDPLDA.PsiSampler#updateTopic(int, double)
		 */
		@Override
		public void updateTopic(int topic, int l_k) {
			// \Psi ~ GD(1,1,..,1, \gamma, \gamma, ..,\gamma)		
			// alphaPrime_j = alpha_j (always 1) + y_j (i.e. l_k)
			l[topic] = l_k;
			lSum += l_k;
		}

		@Override
		public void finalizeSampling() {

			// We want to sample from the Psi prior to init psi
			// at that point we have to allow that lSum == -1
			if(lSum > 0 && lSum < data.size()) {
				throw new ArrayIndexOutOfBoundsException("l ("+ lSum + ") is smaller than the number of documents in the corpus " 
						+ "("+ data.size() + ")");
			}
			
			// We want to sample from the Psi prior to init psi
			// at that point we have to allow that lSum == -1
			if(lSum > 0 && lSum > getCorpusSize()) {
				throw new ArrayIndexOutOfBoundsException("l ("+ lSum + ") is bigger than the number of tokens in the corpus " 
						+ "("+ getCorpusSize() + ")");
			}

			// n = sum over l_k
			
			// \Psi ~ GD(1,1,..,1, \gamma, \gamma, ..,\gamma)
			// betaPrime = b_j (always gamma gamma) + sum_{j+1}{k+1} y_i (i.e l_k)
			// This must be wrong on the Wikipedia page since "k+1" does not make sense...
			long [] betaPrime = new long[numTopics-1];
			for (int topic = 0; topic < (numTopics-1); topic++) {
				for (int topic_l = (topic+1); topic_l < numTopics; topic_l++) {
					betaPrime[topic] += l[topic_l];
				}
			}

			// Draw \nu_i ~ Beta(c_i,d_i) for all i...
			double [] nu = new double[numTopics];
			for (int topic = 0; topic < (numTopics-1); topic++) {
				nu[topic] = ParallelRandoms.rbeta(l[topic] + 1, gamma + (double) betaPrime[topic]);
			}
			// ...except the final one, which is set to 1
			nu[numTopics-1] = 1;
						
			// Finish psi sampling
			double psiSum = 0;
			double oneMinusPsiProd = 1;
			for (int topic = 0; topic < (numTopics-1); topic++) {
				psi[topic] = nu[topic] * oneMinusPsiProd;
				oneMinusPsiProd *= (1-nu[topic]); 
				psiSum += psi[topic];
			}
			
			//System.out.println("Psi=" + Arrays.toString(psi));
			
			// Take care of rounding errors
			if(psiSum>1.0) {
				psi[numTopics-1] = Double.MIN_VALUE;
			} else {				
				psi[numTopics-1] = 1-psiSum;			
			}
		}
		
		@Override
		public double [] getPsi() {
			return psi;
		}

		@Override
		public void incrementTopic(int topic) {

		}
	}

	@Override
	public void postZ() {
		super.postZ();
				
		// activeInData are topics that were non-zero in the last iteration.
		int[] emptyTopics = docTopicTokenFreqTable.getEmptyTopics();
		int activeInData = numTopics - emptyTopics.length;
		activeTopicInDataHistory.add(activeInData); // This is only used for post statistics not for inference
		int calcK = calcK(k_percentile, tokensPerTopic);
		activeTopicHistory.add(calcK); // This is only used for post statistics not for inference
				
								
		if (showTopicsInterval > 0 && currentIteration % showTopicsInterval == 0) {
			logger.info("Active in data: " + activeInData 
					+ "\tK = " + calcK 
					+ "\tEmpty topics (" + emptyTopics.length + ")");
		}

		psiSampler.reset();
	}
	
	@Override
	protected LDADocSamplingResult sampleTopicAssignmentsParallel(LDADocSamplingContext ctx) {
		LDADocSamplingResultSparse res = (LDADocSamplingResultSparse) super.sampleTopicAssignmentsParallel(ctx);

		int [] nonZeroTopics = res.getNonZeroIndices();
		int [] localTopicCounts = res.getLocalTopicCounts();
		// Update the document topic frequency count table
		for (int topic = 0; topic < res.getNonZeroTopicCounts(); topic++) {
			docTopicTokenFreqTable.increment(nonZeroTopics[topic],(int)localTopicCounts[nonZeroTopics[topic]]);
		}

		return res;
	}
	
	/**
	 * Samples new Phi's.
	 * 
	 * 
	 * @param indices the topics the thread should sample
	 * @param topicTypeIndices Choose which elements in Phi top sample (random scan Gibbs sampling)
	 * @param phiMatrix
	 */
	@Override
	public void loopOverTopics(int [] indices, int[][] topicTypeIndices, double[][] phiMatrix) {
		long beforeSamplePhi = System.currentTimeMillis();
		int topicsToSample = indices.length;
		for (int topicIdx = 0; topicIdx < topicsToSample; topicIdx++) {
			int topic = indices[topicIdx];

			VariableSelectionResult res;
			if(tokensPerTopic[topic]>0) {
				// First part of Psi sampling. Normalization must be done 
				// in postIteration when all Psi_k has been sampled
				int l_k = sampleL(topic, longestDocLength, docTopicTokenFreqTable, alphaCoef, psiSampler.getPsi()[topic]);
				psiSampler.updateTopic(topic, l_k);
				
				int [] relevantTypeTopicCounts = topicTypeCountMapping[topic];
				res = dirichletSampler.nextDistributionWithSparseness(relevantTypeTopicCounts);
				phiMatrix[topic] = res.getPhi();
			} else {
				res = dirichletSampler.nextDistributionWithSparseness(beta);
				phiMatrix[topic] = res.getPhi();
				//phiMatrix[topic] = dirichletSampler.nextDistribution();
			}
			
			// We won't use the table any more, so we reset the topic here
			// so we don't have to loop over all in serial in postPhi
			docTopicTokenFreqTable.reset(topic);
		}
		long elapsedMillis = System.currentTimeMillis();
		long threadId = Thread.currentThread().getId();

		LDALoggingUtils lu = config.getLoggingUtil();
		if(measureTimings) {
			PrintWriter pw = lu.checkCreateAndCreateLogPrinter(
					lu.getLogDir() + "/timing_data",
					"thr_" + threadId + "_Phi_sampling.txt");
			pw.println(beforeSamplePhi + "," + elapsedMillis);
			pw.flush();
			pw.close();
		}
	}

	// See. PoissonPolyaUrnHDPLDATest.testSampleLOneDocAnalytic and testSampleLSimR: 
	// SampleL for one document should have the antoniak distribution (see pdf in eq (9) in paper)
	// Very detailed code review (Måns and Leif) 2019-03-12, cross verified with unit tests 
	protected int sampleL(int topic, int maxDocLen, 
			DocTopicTokenFreqTable docTopicTokenFreqTable, 
			double alpha, double psi_k) {
		if(psi_k<0) throw new IllegalArgumentException("Non positive psi_k: " + psi_k);
		// freqHist is D(j, k = topic)
		int [] freqHist = docTopicTokenFreqTable.getReverseCumulativeSum(topic);
		
		// Sum over c_j_k
		
		// We skip the step where nrTopicIndicators==1, since we know that for this
		// case p==1 and rbinom(1,X,1) == X i.e freqHist[0] 
		// (i.e the number of documents in the corpus)
		int lSum = freqHist[0];
		// nrTopicIndicators is j in paper
		// nrDocsWithMoreTopicIndicators is D(j,k = topic) in paper	
		for(int nrTopicIndicators = 2; nrTopicIndicators <= maxDocLen; nrTopicIndicators++) {
			int nrDocsWithMoreTopicIndicators = 0;
			if( freqHist.length >= nrTopicIndicators ) {				
				nrDocsWithMoreTopicIndicators = freqHist[nrTopicIndicators-1];
			}

			// As soon as we see zero, i.e 
			// "how many documents have more than nrTopicIndicators allocated to topic == 0"
			// we know the rest will be zero also since it is a reverse cumulative sum, and 
			// thus not contribute to the sum, so we can exit.
			if(nrDocsWithMoreTopicIndicators==0) break;

			//double p = gamma / (gamma + nrTopicIndicators - 1);
			
			// Calculate on log scale to reduce risk of underflow
			//double nom = Math.exp(Math.log(alpha) + Math.log(psi_k));
			double nom = alpha * psi_k;
			double denom = (nom + nrTopicIndicators - 1);
			double p;
			// 0 / 0 should => 1
			if(nom==0.0 && denom == 0.0) {
				p = 1;
			} else {
				//p = Math.exp(Math.log(nom) - Math.log(denom));
				p = nom / denom;
			}

			// Handling roundoff errors
			if(p<= 0.0) {
				continue;
			}
			if(p >= 1) { 
				p = 1;
				lSum += nrDocsWithMoreTopicIndicators;
				continue;
			}

			lSum += BinomialSampler.rbinom(nrDocsWithMoreTopicIndicators, p);
		}
		if(lSum > tokensPerTopic[topic]) {
			throw new ArrayIndexOutOfBoundsException("l_" + topic 
					+ " ("+ lSum + ") is bigger than # tokens assigned to topic " 
					+ topic + "("+ tokensPerTopic[topic] + ") Iter: " + getCurrentIteration());
		}
		return lSum;
	}	
	
//	/**
//	 * Adapted for HDP LDA
//	 *
//	 */
//	@Override
//	public double modelLogLikelihood() {
//		double logLikelihood = 0.0;
//		//int nonZeroTopics;
//
//		// The likelihood of the model is a combination of a 
//		// Dirichlet-multinomial for the words in each topic
//		// and a Dirichlet-multinomial for the topics in each
//		// document.
//
//		// The likelihood function of a dirichlet multinomial is
//		//	 Gamma( sum_i alpha_i )	 prod_i Gamma( alpha_i + N_i )
//		//	prod_i Gamma( alpha_i )	  Gamma( sum_i (alpha_i + N_i) )
//
//		// So the log likelihood is 
//		//	logGamma ( sum_i alpha_i ) - logGamma ( sum_i (alpha_i + N_i) ) + 
//		//	 sum_i [ logGamma( alpha_i + N_i) - logGamma( alpha_i ) ]
//
//		// Do the documents first
//
//		int[] topicCounts = new int[numTopics];
//		double[] topicLogGammas = new double[numTopics];
//		int[] docTopics;
//
//		for (int topic=0; topic < numTopics; topic++) {
//			topicLogGammas[ topic ] = Dirichlet.logGammaStirling( alpha[topic] * psiSampler.getPsi()[topic] );
//		}
//
//		for (int doc=0; doc < data.size(); doc++) {
//			LabelSequence topicSequence =	(LabelSequence) data.get(doc).topicSequence;
//
//			docTopics = topicSequence.getFeatures();
//
//			for (int token=0; token < docTopics.length; token++) {
//				topicCounts[ docTopics[token] ]++;
//			}
//
//			for (int topic=0; topic < numTopics; topic++) {
//				if (topicCounts[topic] > 0) {
//					logLikelihood += (Dirichlet.logGammaStirling(alpha[topic] * psiSampler.getPsi()[topic] + topicCounts[topic]) -
//							topicLogGammas[ topic ]);
//				}
//			}
//
//			// subtract the (count + parameter) sum term
//			logLikelihood -= Dirichlet.logGammaStirling(alphaSum + docTopics.length);
//
//			Arrays.fill(topicCounts, 0);
//		}
//
//		// add the parameter sum term
//		logLikelihood += data.size() * Dirichlet.logGammaStirling(alphaSum);
//
//		// And the topics
//
//		// Count the number of type-topic pairs that are not just (logGamma(beta) - logGamma(beta))
//		int nonZeroTypeTopics = 0;
//
//		for (int type=0; type < numTypes; type++) {
//			// reuse this array as a pointer
//
//			topicCounts = typeTopicCounts[type];
//
//			for (int topic = 0; topic < numTopics; topic++) {
//				if (topicCounts[topic] == 0) { continue; }
//
//				nonZeroTypeTopics++;
//				logLikelihood += Dirichlet.logGammaStirling(beta + topicCounts[topic]);
//
//				if (Double.isNaN(logLikelihood)) {
//					System.err.println("NaN in log likelihood calculation: " + topicCounts[topic]);
//					System.exit(1);
//				} 
//				else if (Double.isInfinite(logLikelihood)) {
//					logger.warning("infinite log likelihood");
//					System.exit(1);
//				}
//			}
//		}
//
//		for (int topic=0; topic < numTopics; topic++) {
//			logLikelihood -= 
//					Dirichlet.logGammaStirling( (beta * numTypes) +
//							tokensPerTopic[ topic ] );
//
//			if (Double.isNaN(logLikelihood)) {
//				logger.info("NaN after topic " + topic + " " + tokensPerTopic[ topic ]);
//				return 0;
//			}
//			else if (Double.isInfinite(logLikelihood)) {
//				logger.info("Infinite value after topic " + topic + " " + tokensPerTopic[ topic ]);
//				return 0;
//			}
//
//		}
//
//		// logGamma(|V|*beta) for every topic
//		logLikelihood += 
//				Dirichlet.logGammaStirling(beta * numTypes) * numTopics;
//
//		// logGamma(beta) for all type/topic pairs with non-zero count
//		logLikelihood -=
//				Dirichlet.logGammaStirling(beta) * nonZeroTypeTopics;
//
//		if (Double.isNaN(logLikelihood)) {
//			logger.info("at the end");
//		}
//		else if (Double.isInfinite(logLikelihood)) {
//			logger.info("Infinite value beta " + beta + " * " + numTypes);
//			return 0;
//		}
//
//		return logLikelihood;
//	}
	
	// Serialization

		private static int PARSED_CONFIG = 0;
		private static int SIMPLE_CONFIG = 1;

		private void writeObject(ObjectOutputStream out) throws IOException {
			if(ParsedLDAConfiguration.class.isAssignableFrom(config.getClass())) {
				out.writeInt(PARSED_CONFIG);
				System.out.flush();
			} else {
				out.writeInt(SIMPLE_CONFIG);
			}
			out.writeObject(data);
			out.writeObject(alphabet);
			out.writeObject(topicAlphabet);

			out.writeInt(numTopics);

			out.writeInt(numTypes);

			out.writeObject(alpha);
			out.writeDouble(alphaSum);
			out.writeDouble(beta);
			out.writeDouble(betaSum);

			out.writeObject(phi);
			out.writeObject(phiMean);
			out.writeInt(phiBurnIn);
			out.writeInt(phiMeanThin);
			out.writeInt(noSampledPhi);

			out.writeObject(typeTopicCounts);
			out.writeObject(tokensPerTopic);

			out.writeObject(docLengthCounts);
			out.writeObject(topicDocCounts);

			out.writeInt(showTopicsInterval);
			out.writeInt(wordsPerTopic);

			out.writeObject(formatter);
			out.writeBoolean(printLogLikelihood);
			
			
			out.writeInt(poissonNormalApproxThreshold);
			out.writeObject(psiSampler);
			out.writeDouble(gamma);
			  
			out.writeObject(activeTopics);
			
			out.writeDouble(alphaCoef); 
			
			out.writeObject(docTopicTokenFreqTable); 
			out.writeInt(nrStartTopics);
			
			out.writeObject(activeTopicHistory); 
			out.writeObject(activeTopicInDataHistory);
			
			out.writeObject(topicOcurrenceCount);
			
			out.writeDouble(k_percentile);

			out.writeObject(deceasedTopics);
			
			if(ParsedLDAConfiguration.class.isAssignableFrom(config.getClass())) {
				out.writeObject(config.whereAmI());
			} else {
				out.writeObject(config);
			}
		}

		@SuppressWarnings("unchecked")
		private void readObject (ObjectInputStream in) throws IOException, ClassNotFoundException {
			int version = in.readInt ();

			data = (ArrayList<TopicAssignment>) in.readObject ();
			alphabet = (Alphabet) in.readObject();
			topicAlphabet = (LabelAlphabet) in.readObject();

			numTopics = in.readInt();

			numTypes = in.readInt();

			alpha = (double[]) in.readObject();
			alphaSum = in.readDouble();
			beta = in.readDouble();
			betaSum = in.readDouble();

			phi = (double[][]) in.readObject();
			phiMean  = (double[][]) in.readObject();
			phiBurnIn = in.readInt();
			phiMeanThin = in.readInt();
			noSampledPhi = in.readInt();

			typeTopicCounts = (int[][]) in.readObject();
			tokensPerTopic = (int[]) in.readObject();

			docLengthCounts = (int[]) in.readObject();
			topicDocCounts = (int[][]) in.readObject();

			showTopicsInterval = in.readInt();
			wordsPerTopic = in.readInt();

			formatter = (NumberFormat) in.readObject();
			printLogLikelihood = in.readBoolean();
			
			poissonNormalApproxThreshold = in.readInt();
			psiSampler = (PsiSampler) in.readObject();
			gamma = in.readDouble();
			  
			activeTopics = (List<Integer>) in.readObject();
			
			alphaCoef = in.readDouble(); 
			
			docTopicTokenFreqTable = (DocTopicTokenFreqTable) in.readObject(); 
			nrStartTopics = in.readInt();
			
			activeTopicHistory = (List<Integer>) in.readObject(); 
			activeTopicInDataHistory = (List<Integer>) in.readObject();
			
			topicOcurrenceCount = (int []) in.readObject();
			
			k_percentile = in.readDouble();

			deceasedTopics = (boolean []) in.readObject();

			if(version==SIMPLE_CONFIG) {
				config = (LDAConfiguration) in.readObject();
			} else {
				String cfg_file = (String) in.readObject();
				System.out.println("Reading config from:" + cfg_file);
				try {
					config = new ParsedLDAConfiguration(cfg_file);

					String expDir = config.getExperimentOutputDirectory("");
					if(!expDir.equals("")) {
						expDir += "/";
					}
					String logSuitePath = "Runs/" + expDir + "RunSuite" + FileLoggingUtils.getDateStamp();
					LDALoggingUtils lu = new LoggingUtils();
					lu.checkAndCreateCurrentLogDir(logSuitePath);
					config.setLoggingUtil(lu);

					System.out.println("Done Reading config!");
				} catch (ConfigurationException e) {
					e.printStackTrace();
				}
			}
		}

		public void write (File serializedModelFile) {
			try {
				ObjectOutputStream oos = new ObjectOutputStream (new FileOutputStream(serializedModelFile));
				oos.writeObject(this);
				oos.close();
			} catch (IOException e) {
				e.printStackTrace();
				System.err.println("Problem serializing TopicModel to file " +
						serializedModelFile + ": " + e);
			}
		}

		public static PoissonPolyaUrnHDPLDAInfiniteTopics read (File f) throws Exception {
			ObjectInputStream ois = new ObjectInputStream (new FileInputStream(f));
			PoissonPolyaUrnHDPLDAInfiniteTopics topicModel = (PoissonPolyaUrnHDPLDAInfiniteTopics) ois.readObject();
			ois.close();

			return topicModel;
		}

}
