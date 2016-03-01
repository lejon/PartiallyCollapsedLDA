package cc.mallet.topics;

import java.util.concurrent.Callable;

import cc.mallet.topics.SpaliasUncollapsedParallelLDA.TableBuildResult;

interface TableBuilderFactory {
	Callable<TableBuildResult> instance(int type);
}