package cc.mallet.topics.tui;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cc.mallet.types.Dirichlet;

public class LoglikelihoodCalculator {
	
	String cfn;
	String vfn; 
	String zfn;
	int numTopics;
	int [][] w;
	Map<Integer,String> vocab = new HashMap<>();
	int [][] zs;
	
	public static void main(String[] args) { 
		//String base = "/Users/eralljn/workspace/PartiallyCollapsedLDA/Runs/RunSuite2019-05-10--16_37_20/Run2019-05-10--16_37_20/HDPLDA-all-nips";
		//String base = "/Users/eralljn/workspace/PartiallyCollapsedLDA/Runs/RunSuite2019-05-13--20_49_17/Run2019-05-13--20_49_17/HDPLDA-all-cgcbib";
		//String base = "/Users/eralljn/workspace-sts4/InfiniteLDAMalletIntegration/Runs/RunSuite2019-05-14--09_58_24/Run2019-05-14--09_58_24/ILDAcgcbib1PU";
		//String cfn  = base + "/" + "lda_corpus.csv";
		//String vfn  = base + "/" + "lda_vocab.txt";
		
		String base = "/Users/eralljn/Downloads/results_neurips_subcluster";
		//String base = "/Users/eralljn/Downloads/results_nips_subcluster_10h";
		String cfn  = base + "/" + "HDPLDAnips1PU-corpus.txt";
		String vfn  = base + "/" + "HDPLDAnips1PU-vocabulary.txt";

		double alpha = 0.1;
		double beta = 0.01;
		
		int [][] w = readIndicators(cfn);
		Map<Integer,String> vocab = readVocabulary(vfn);
		int numTopics = 1000;
		
		File file = new File("likelihood.txt");
		
		try (FileWriter fw = new FileWriter(file, false); 
				BufferedWriter bw = new BufferedWriter(fw);
				PrintWriter pw  = new PrintWriter(bw)) {

			List<Double> lls = new ArrayList<>();
			for (int i = 1; i <= 346; i++) {
				String zfn = base + "/" + "z_" + i + ".csv";
				System.out.println("Loading: " + zfn);

				int [][] zs = readIndicators(zfn);
				boolean isSubcluster = true;

				if(isSubcluster) {
					for (int z_r = 0; z_r < zs.length; z_r++) {
						for (int j = 0; j < zs[z_r].length; j++) {
							zs[z_r][j] += 1;
							zs[z_r][j] /= 2;
						}
					}
				}
				
				LoglikelihoodCalculator llc = new LoglikelihoodCalculator(numTopics, w, vocab, zs);
				double ll = llc.calcLL(alpha, beta);
				lls.add(ll);
				pw.println(i + "\t" + ll);
				i++;
			}
		} catch (IOException e) {
			throw new IllegalArgumentException("File " + file.getName()
			+ " is unwritable : " + e.toString());
		}
	}
	
	public LoglikelihoodCalculator(int numTopics, int [][] w, Map<Integer,String> vocab, int [][] zs) {
		this.numTopics = numTopics;
		this.w = w;
		this.vocab = vocab;
		this.zs = zs;
	}
	
	static int findNumTopics(int[][] zs) {
		Set<Integer> zset = new HashSet<>();
		for (int i = 0; i < zs.length; i++) {
			for (int j = 0; j < zs[i].length; j++) {
				zset.add(zs[i][j]);
			}
		}
		return zset.size();
	}
		
	public double calcLL(double alpha, double beta) {
		int M = w.length;
		int V = vocab.size();
		int [][] nmk = new int[M][numTopics];
		int [][] nkt = new int[numTopics][V];
		int [] nk = new int[numTopics];
		for( int i = 0; i < zs.length; i++) {
			int [] row = zs[i];
			updateLocalTopicCounts(nmk[i],row);
			updateTypeTopicMatrix(nkt, w[i], row);
			updateTopicCounts(nk,row);
		}	
		return modelLogLikelihood(numTopics, alpha, beta, M, nmk, V, w, nkt, nk);
	}
	
	private static int[][] readIndicators(String zfn) {
		int [][] result = null;
		try {
			String z = new String(Files.readAllBytes(Paths.get(zfn)), StandardCharsets.UTF_8);
			String [] docZss = z.split("\n");
			result = new int[docZss.length][];
			int idx = 0;
			for(String docZ : docZss) {
				String [] zss = docZ.split(",");
				int [] zs = Arrays.stream(zss).mapToInt(i -> Integer.parseInt(i.trim())).toArray();
				result[idx++] = zs;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return result;
	}

	private static Map<Integer,String> readVocabulary(String vfn) {
		String z;
		Map<Integer,String> vocab = new HashMap<>();
		try {
			z = new String(Files.readAllBytes(Paths.get(vfn)), StandardCharsets.UTF_8);
			String [] words = z.split("\n");
			for (int i = 0; i < words.length; i++) {
				vocab.put(i, words[i]);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return vocab;
	}

	void updateTopicCounts(int[] nk, int[] row) {
		for (int i = 0; i < row.length; i++) {
			nk[row[i]]++;
		}
	}

	void updateTypeTopicMatrix(int[][] nkt, int [] ws, int[] row) {
		for (int i = 0; i < row.length; i++) {
			nkt[row[i]][ws[i]]++;
		}
	}

	void updateLocalTopicCounts(int[] nmk, int[] row) {
		for (int i = 0; i < row.length; i++) {
			nmk[row[i]]++;
		}
	}

	public static double modelLogLikelihood(int numTopics, double alpha, double beta, int M, int [][] nmk,
			int V, int [][]w, int [][] nkt, int [] nk) {
		double logLikelihood = 0.0;
		//int nonZeroTopics;

		// The likelihood of the model is a combination of a 
		// Dirichlet-multinomial for the words in each topic
		// and a Dirichlet-multinomial for the topics in each
		// document.

		// The likelihood function of a dirichlet multinomial is
		//	 Gamma( sum_i alpha_i )	 prod_i Gamma( alpha_i + N_i )
		//	prod_i Gamma( alpha_i )	  Gamma( sum_i (alpha_i + N_i) )

		// So the log likelihood is 
		//	logGamma ( sum_i alpha_i ) - logGamma ( sum_i (alpha_i + N_i) ) + 
		//	 sum_i [ logGamma( alpha_i + N_i) - logGamma( alpha_i ) ]

		// Do the documents first
		double alphaSum = alpha * numTopics;

		double[] topicLogGammas = new double[numTopics];

		for (int topic=0; topic < numTopics; topic++) {
			topicLogGammas[ topic ] = Dirichlet.logGammaStirling( alpha );
		}

		for (int doc=0; doc < M; doc++) {

			for (int topic=0; topic < numTopics; topic++) {
				if (nmk[doc][topic] > 0) {
					logLikelihood += (Dirichlet.logGammaStirling(alpha + nmk[doc][topic]) -
							topicLogGammas[ topic ]);
				}
			}

			// subtract the (count + parameter) sum term
			logLikelihood -= Dirichlet.logGammaStirling(alphaSum + w[doc].length);
		}

		// add the parameter sum term
		logLikelihood += M * Dirichlet.logGammaStirling(alphaSum);

		// And the topics

		// Count the number of type-topic pairs that are not just (logGamma(beta) - logGamma(beta))
		int nonZeroTypeTopics = 0;

		for (int type=0; type < V; type++) {
			// reuse this array as a pointer

			for (int topic = 0; topic < numTopics; topic++) {
				if (nkt[topic][type] == 0) { continue; }

				nonZeroTypeTopics++;
				logLikelihood += Dirichlet.logGammaStirling(beta + nkt[topic][type]);

				if (Double.isNaN(logLikelihood)) {
					System.err.println("NaN in log likelihood calculation: " + nkt[topic][type]);
					System.exit(1);
				} 
				else if (Double.isInfinite(logLikelihood)) {
					System.err.println("infinite log likelihood");
					System.exit(1);
				}
			}
		}

		for (int topic=0; topic < numTopics; topic++) {
			int tokensPerTopic = nk[topic];
			
			logLikelihood -= 
					Dirichlet.logGammaStirling( (beta * V) +	tokensPerTopic);
			if (Double.isNaN(logLikelihood)) {
				System.out.println("NaN after topic " + topic + " " + tokensPerTopic);
				return 0;
			}
			else if (Double.isInfinite(logLikelihood)) {
				System.out.println("Infinite value after topic " + topic + " " + tokensPerTopic);
				return 0;
			}
		}

		// logGamma(|V|*beta) for every topic
		logLikelihood += 
				Dirichlet.logGammaStirling(beta * V) * numTopics;

		// logGamma(beta) for all type/topic pairs with non-zero count
		logLikelihood -=
				Dirichlet.logGammaStirling(beta) * nonZeroTypeTopics;

		if (Double.isNaN(logLikelihood)) {
			System.out.println("at the end");
		}
		else if (Double.isInfinite(logLikelihood)) {
			System.out.println("Infinite value beta " + beta + " * " + V);
			return 0;
		}

		return logLikelihood;
	}
	
}
