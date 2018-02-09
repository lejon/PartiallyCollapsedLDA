package cc.mallet.topics;

import java.util.concurrent.Callable;

interface TableBuilderFactory {
	Callable<WalkerAliasTableBuildResult> instance(int type);
}