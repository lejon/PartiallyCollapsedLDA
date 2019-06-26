package cc.mallet.similarity;

import java.util.HashSet;
import java.util.Set;

import cc.mallet.types.FeatureSequence;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;

// This code was adapted from: BM25.java with the original Copyright

/*Copyright 2011 Deminem Solutions
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at
       http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software distributed under 
the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, 
either express or implied. See the License for the specific language governing permissions and limitations 
under the License.
*/

public class BM25Distance implements TrainedDistance, InstanceDistance {

	protected double k_1 = 1.2;
	protected double k_3 = 8;
	protected double b = 0.5;
	
	int corpusSize = 0;
	double avgDocLen = 0.0;
	int [] docFreq;
	InstanceList trainingset;

	public BM25Distance() {
		
	}
	
	public BM25Distance(int corpusSize, double avgDocLen, int [] docFreq) {
		this.corpusSize = corpusSize;
		this.avgDocLen = avgDocLen;
		this.docFreq = docFreq;
	}

	@Override
	public double calculate(double[] v1, double[] v2) {
		double totBM25 = 0.0;
		//totBM25 += bm25fext(v1[i], corpusSize, v2.length, avgDocLen, v2[i], docFreq[i] );
		int queryFreqIndoc = 0;
		for (int j = 0; j < v1.length; j++) {
			for (int j2 = 0; j2 < v2.length; j2++) {
				if(v1[j]==v2[j2]) {
					queryFreqIndoc++;
				}
			}
			totBM25 += bm25f(queryFreqIndoc, corpusSize, v2.length, avgDocLen, docFreq[(int)v1[j]] );
		}
		if(totBM25==0.0) {
			totBM25 = Double.NEGATIVE_INFINITY;
		}
		return -totBM25;
	}
	
	/**
	 * BM25F is particularly developed to compute similarity of a short document (i.e., query) 
	 * with a longer document. It is typically used for search engines, where user queries are 
	 * usually short and consist of only a few words.
	 *  
	 * @param queryTermFrequency the frequency of the query term in the document
	 * @param numberOfDocuments in vocabulary
	 * @param docLength
	 * @param averageDocumentLength
	 * @param queryFrequency
	 * @param documentFrequency the number of documents that contains the term
	 * @return
	 */
	public final double bm25f(double queryTermFrequency, 
    		double numberOfDocuments, 
    		double docLength, 
    		double averageDocumentLength, 
    		double documentFrequency) {
    	
            double K = k_1 * ((1 - b) + ((b * docLength) / averageDocumentLength));
            double tf = ( ((k_1 + 1) * queryTermFrequency) / (K + queryTermFrequency) );
            
            double idf = calcIdf(numberOfDocuments, documentFrequency);
			double score = tf * idf;	
            return score;
    }

	public static double calcIdf(double numberOfDocuments, double documentFrequency) {
		return Math.log(1 + (numberOfDocuments - documentFrequency + 0.5) / (documentFrequency + 0.5));
	}
	
	/**
	 * BM25Fext is a similarity function that measures the similarity of two documents each 
	 * of which are relatively long textual documents. BM25Fext is an extension of BM25F, 
	 * which considers the term frequencies in query documents
	 * 
	 * @param queryTermFrequency the frequency of the query term in the document
	 * @param numberOfDocuments
	 * @param docLength
	 * @param averageDocumentLength
	 * @param queryDocumentTermFrequency the frequency of the query term in the query document
	 * @param documentFrequency
	 * @return
	 */
	public final double bm25fext(double queryTermFrequency, 
    		double numberOfDocuments, 
    		double docLength, 
    		double averageDocumentLength, 
    		double queryDocumentTermFrequency, 
    		double documentFrequency) {
    	
            double bm25 = bm25f(queryTermFrequency,numberOfDocuments, docLength, averageDocumentLength, documentFrequency);
            double tf_ext = bm25 * ( ((k_3 + 1) * queryDocumentTermFrequency) / (k_3 + queryDocumentTermFrequency) );	//second part
            
            double idf = calcIdf(numberOfDocuments, documentFrequency);
			double score = idf * tf_ext;	
            return score;
    }
	
	@Override
	public void init(InstanceList trainingset) {
		this.trainingset = trainingset;
		corpusSize = trainingset.size();
		int alphabetSize = trainingset.getAlphabet().size();
		docFreq = new int [alphabetSize];
		Set<Integer> docUnique = new HashSet<>();
		long docLenSum = 0;
		for (int docIdx = 0; docIdx < trainingset.size(); docIdx++) {
			FeatureSequence tokenSequence =
					(FeatureSequence) trainingset.get(docIdx).getData();
			int [] docTokens = tokenSequence.getFeatures();
			docLenSum += docTokens.length;
			// In MALLET documents with one word still has length(docTokens) == 2
			for (int i = 0; i < tokenSequence.size(); i++) {
				int ti = docTokens[i];
				docUnique.add(ti);
			}
			for (Integer integer : docUnique) {
				docFreq[integer]++;
			}
		}	
		avgDocLen = docLenSum / (double) trainingset.size();
	}

	@Override
	public double distanceToTrainingSample(double[] query, int sampleId) {
		TokenIndexVectorizer tv = new TokenIndexVectorizer();
		double [] trainingInstance = tv.instanceToVector(trainingset.get(sampleId));
		return calculate(query, trainingInstance);
	}
	
	@Override
	public double[] distanceToAll(Instance testInstance) {
		TokenIndexVectorizer tv = new TokenIndexVectorizer();
		double [] testVector = tv.instanceToVector(testInstance);
		double [] result = new double[trainingset.size()];
		for (int i = 0; i < trainingset.size(); i++) {
			result[i] = distanceToTrainingSample(testVector,i);
		}
		return result;
	}

	@Override
	public double distance(Instance instance1, Instance instance2) {
		TokenIndexVectorizer tv = new TokenIndexVectorizer();
		double [] trainingInstance1 = tv.instanceToVector(instance1);
		double [] trainingInstance2 = tv.instanceToVector(instance2);
		return calculate(trainingInstance1, trainingInstance2);
	}

}
