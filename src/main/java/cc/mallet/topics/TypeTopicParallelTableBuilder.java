package cc.mallet.topics;

import java.util.concurrent.Callable;

import cc.mallet.util.OptimizedGentleAliasMethodDynamicSize;
import cc.mallet.util.WalkerAliasTable;

class TypeTopicParallelTableBuilder implements Callable<WalkerAliasTableBuildResult> {
	int type;
	int [] nonZeroTypeTopicCnt;
	int [][] nonZeroTypeTopics;
	int [][] typeTopicCounts;
	double [] topicCountBetaHat;
	WalkerAliasTable [] aliasTables;
	int numTopics;
	
	public TypeTopicParallelTableBuilder(int type, int[] nonZeroTypeTopicCnt, int[][] nonZeroTypeTopics, 
			int[][] typeTopicCounts, double[] topicCountBetaHat, WalkerAliasTable[] aliasTables, int numTopics) {
		this.type = type;
		this.nonZeroTypeTopicCnt = nonZeroTypeTopicCnt;
		this.nonZeroTypeTopics = nonZeroTypeTopics;
		this.typeTopicCounts = typeTopicCounts;
		this.topicCountBetaHat = topicCountBetaHat;
		this.aliasTables = aliasTables;
		this.numTopics = numTopics;
	}
	@Override
	public WalkerAliasTableBuildResult call() {
		/* Nonsparse solution
		double [] probs = new double[numTopics];
		double typeMass = 0; // Type prior mass
		for (int topic = 0; topic < numTopics; topic++) {
			// TODO: If this works we can use a sparse version instead
			typeMass += probs[topic] = (typeTopicCounts[type][topic] + beta) / topicCountBetaHat[topic];
		}
		*/
		
		double [] probs = new double[nonZeroTypeTopicCnt[type]];
		// Iterate over nonzero topic indicators
		double typeMass = 0.0;
		for (int i = 0; i < nonZeroTypeTopicCnt[type]; i++) {
			typeMass += probs[i] = typeTopicCounts[type][nonZeroTypeTopics[type][i]] / topicCountBetaHat[nonZeroTypeTopics[type][i]];
		}
		
		if(aliasTables[type]==null) {
			int aliasSize = numTopics;
			// TODO Fix so that Alias tables uses a minimum of memory by maximizing the Alias tables size to the number of tokens per type
			aliasTables[type] = new OptimizedGentleAliasMethodDynamicSize(probs, typeMass, aliasSize);
		} else {
			aliasTables[type].reGenerateAliasTable(probs, typeMass);
		}
			
		return new WalkerAliasTableBuildResult(type, aliasTables[type], typeMass);
	}   
	
	/*class TypeTopicParallelTableBuilder implements Callable<TableBuildResult> {
	int type;
	public TypeTopicParallelTableBuilder(int type) {
		this.type = type;
	}
	@Override
	public TableBuildResult call() {
		
		double [] probs = new double[nonZeroTypeTopicCnt[type]];

		// Iterate over nonzero topic indicators
		int normConstant = 0;
		for (int i = 0; i < nonZeroTypeTopicCnt[type]; i++) {
			normConstant += probs[i] = typeTopicCounts[type][nonZeroTypeTopics[type][i]] / topicCountBetaHat[nonZeroTypeTopics[type][i]];
		}

		// for (int i = 0; i < myTypeTopicCounts.length; i++) {
		// 	typeMass += probs[i] = myTypeTopicCounts[i] / (double) topicMass;
		// }
		
		// Normalize probabilities
		// for (int i = 0; i < nonZeroTypeTopicCnt[type]; i++) {
		//	  probs[i] = typeTopicCounts[type][nonZeroTypeTopics[type][i]] / (double) normConstant;
		// }

		if(aliasTables[type]==null) {
			int aliasSize;
			// TODO Fix so that Alias tables uses a minimum of memory by maximizing the Alias tables size to the number of tokens per type
			//if(tokensPerType[type] > numTopics){
				aliasSize = numTopics;
			//} else {
			//	aliasSize = tokensPerType[type];
			//}
			aliasTables[type] = new OptimizedGentleAliasMethodDynamicSize(probs, normConstant, aliasSize);
		} else {
			aliasTables[type].reGenerateAliasTable(probs, normConstant);
		}
		
		// TODO: Check Spalias that typeNorm is correct (and not normalized).
		return new TableBuildResult(type, aliasTables[type], -1);
	}   
}*/
	
}