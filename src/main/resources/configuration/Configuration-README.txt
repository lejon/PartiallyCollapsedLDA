# Comments start with "#"

## Configs: Configs that will be run by the program. The program calls
## the method getSubConfigs() on the config object it will then 
## get a list of the sub-configs listed in this variable. The program 
## then MUST "activate" using activateSubconfig(conf) the subconfig that 
## it wants to run

!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
#################### SUBCONFIG NAMES CANNOT CONTAIN THE "." CHARACTER!! ####################  
!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

configs = uncollapsed_parallel_adaptive, uncollapsed_parallel

### Subconfig, one config file can have several sub-configs
### sub-configs are activated in the code

[uncollapsed_parallel_adaptive]

## Title: will be printed in the summary output of each run
title = PCPLDA

## Description: will be printed in the summary output of each run
description = Partially Collapsed Parallel LDA with adaptive subsampling (not full random scan)

## Dataset: Filename of dataset file to use this file must follow a specific format
#dataset = datasets/enron.txt
#dataset = datasets/smallnips.txt
dataset = datasets/nips.txt
#dataset = datasets/pubmed.txt

## Original Dataset: Filename of original dataset. This is useful if the dataset has been 
## pre-processed externally and a reference to the original text is needed
## the order of the documents are assumed to be the same as in 'dataset'

## Test Dataset: Filename of test dataset file to use, must follow same format as dataset file
## if this file is given, the held out log likelihood will be calculated every 'topic_interval' 
## relative to this dataset. 
## This slows down execution substantially, so if you are not evaluating your model, don't set this option! 
## The held out LL is written to the file "test_held_out_log_likelihood.txt"
test_dataset = datasets/enron.txt

## Scheme: Which sampling scheme to use (uncollapsed, collapsed, adlda)
##		paranoid is uncollapsed with additional (time consuming) consistency checks
#scheme = uncollapsed
scheme = spalias

## Seed: seed for the random sampling, -1 means use clock time
seed = -1 # -1 => use LSB of current time as seed

## Topics, Alpha and Beta: The number of topics, alpha (this is NOT the sum of alphas!) and beta to use
topics = 20
alpha = 1.0
beta = 0.01
hdp_gamma = 1

## Iterations: How many iterations to sample
iterations = 8000

## Diagnostic interval: When to  to do a full dump of (phi, M and N), -1 means never
##	Example: diagnostic_interval = 500, 1000 # Dump diagnostics between iteration 500 and 1000
diagnostic_interval = -1

## Batches: How many threads to use for Z sampling, (more threads are allocated for sampling Phi and count Updates)
## DEPRICATED: NOT USED
## there is a config variable ('document_sampler_split_limit') that controls the size limit of the fork/join
## split. 
#batches = 4

## Topic batches: How many threads for sampling Phi
#topic_batches = 3

## Rare threshold: Min threshold for how many times a word must occur to be included in vocabulary
rare_threshold = 3

## TF-IDF: 
## Specifies how many words the final dictionary will contain.
## keeps the "tfidf_vocab_size" number of words with the highest TF-IDF values according to the following formula:
## double tfIdf = (tf == 0 || idf == 0) ? 0.0 : (double) tf * Math.log(corpusSize / (double) idf);
## Stopwords are STILL respected!
tfidf_vocab_size = 100

## Topic Interval: How often to print top words to console (and log likelihood to file) during sampling (takes time), 0 means never
topic_interval = 100

## DN Diagnostic interval: When to dump Delta N, either never (-1) or between iteration numbers
##		Example: dn_diagnostic_interval = 10, 50, 5000, 7000 # Dump Delta N between iteration 10 and 50 and also between 5000 and 7000 
dn_diagnostic_interval = -1

## Start diagnostic:
# Determines from which iteration Phi is printed to a binary file, when "print_phi" is set to true
# turn off by setting to -1   
start_diagnostic = 500

## Measure Timing: If timing measurements should be done (takes time)
measure_timing = false

## Results size: How many documents to buffer on the workers before sending back for updates
## DEPRICATED: NOT USED
#results_size = 5

### Batch Building Scheme: Different algorithms for building the document batches
 
## EvenSplitBatchBuilder: Splits the full corpus evenly over the threads
#batch_building_scheme = utils.randomscan.document.EvenSplitBatchBuilder

## PercentageBatchBuilder: Samples percentage_split_size_doc % of the corpus randomly without replacement in every iteration
batch_building_scheme = utils.randomscan.document.PercentageBatchBuilder
## percentage_split_size_doc
percentage_split_size_doc = 0.2

## AdaptiveBatchBuilder: Subclass of PercentageBatchBuilder that takes an instability period into consideration
#batch_building_scheme = utils.randomscan.document.AdaptiveBatchBuilder

## FixedSplitBatchBuilder: Takes a fixed % specified by fixed_split_size_doc of the corpus per iteration  
batch_building_scheme = utils.randomscan.document.FixedSplitBatchBuilder

## Fixed Split Size Doc: Used by the FixedSplitBatchBuilder, below example, samples 20% of docs the first 4 iterations then 100% 
## 	and then loops over these ratios for the rest of the iterations 
fixed_split_size_doc   = 0.2, 0.2, 0.2, 0.2, 1.0

### Topic index building schemes: Decides which words to sample in Phi
## DeltaNTopicIndexBuilder: Samples the words that changes in the Z sampling
topic_index_building_scheme = utils.randomscan.topic.DeltaNTopicIndexBuilder

## TopWordsRandomFractionTopicIndexBuilder: 
# Samples Types that have high frequency should be sampled more often but according
# to random scan contract ALL types MUST have a small probability to be 
# sampled. In 80% of the cases we draw the proportion of types to sample from a Beta distribution
# with a mode centered on 20 %. Often we will sample the 20% most probable
# words, but sometimes we will sample them all. In the other 20% it samples ALL words in Phi
# A Beta(2.0,5.0) will have the mode (a-1) / (a+b-2) = 0.2 = 20%
topic_index_building_scheme = utils.randomscan.topic.TopWordsRandomFractionTopicIndexBuilder

## MandelbrotTopicIndexBuilder
# Samples the top X% ('percent_top_tokens' from config) of the most frequent tokens in the corpus, 
# respects the <code>full_phi_period</code> variable

## AllWordsTopicIndexBuilder: Samples the full Phi
topic_index_building_scheme = utils.randomscan.topic.AllWordsTopicIndexBuilder

## Full Phi Period:
# For algorithms that cares about sample the full Phi at some interval
# Currently used by DeltaNTopicIndexBuilder
full_phi_period = 5 # Sample full Phi ever 5:th interation

## PercentageTopicBatchBuilder
# Samples X% of the ROWS (which topics) in Phi controlled by the percentage_split_size_topic
# config parameter

# Used by PercentageTopicBatchBuilder to decide how large part of topics (rows in Phi) to sample
percentage_split_size_topic = 1.0 # (1.0 = 100%)

## Instability period: For Algorithms that care about instability periods (number of iterations)
#instability_period = 125 # Iterations
instability_period = 0

## Print N Docs interval: List the intervals in which Theta should be printed for the "print_ndocs_cnt" number of documents
print_ndocs_interval = 50,100, 150,200
print_ndocs_cnt = 500 # Print theta for the first 500 documents

## Print N Topword interval: List the intervals in which Phi should be printed for the "print_ntopwords_cnt" words
print_ntopwords_interval = 50,100, 150,200
print_ntopwords_cnt = 500 # Print phi for the top 500 words

## Should the density of the type/topic matrix be logged true/false,
# observe that this incurs a performance penalty since the type/topic
# matrix is looped over each time the density should be printed
log_type_topic_density = false

## Stopword file
# filename of a file with one word per line of stopwords
# default = stoplist.txt
stoplist = stoplist.txt

debug = 0

log_document_density = true
log_type_topic_density = true

# Save the a file with the document topic means (can include zeros)
save_doc_topic_means = true
doc_topic_mean_filename = doc_topic_means.csv

# Save the a file with document topic theta estimates (will not include zeros)
# Unlike Phi means which are sampled with thinning, theta means is just a simple
# average of the topic counts in the last iteration divided by the number of 
# tokens in the document thus there is not theta_burnin or theta_thinning
save_doc_theta_estimate = true
doc_topic_theta_filename = doc_topic_theta.csv

save_phi_means = true
phi_mean_filename = phi_means.csv


save_term_frequencies = true
term_frequencies_filename = term_frequencies.txt

# Words that occur less than rare_threshold is removed from the corpus
rare_threshold = 50

# Save the vocabulary (order matches the Phi matrix) 
save_vocabulary = true
vocabulary_filename = lda_vocab.txt

# Log Phi: Calculate the Ratio of Zeros in Phi (so should really be called Phi Sparsity I guess)
log_phi_density = true
# Phi Log filename
doc_topic_mean_filename = doc_topic_means.csv

# Percent burn in (i.e percent of total number of iterations) before start sampling phi mean
# Example: iterations = 2000, phi_mean_burning = 50 => start sampling Phi at 1000 iterations
phi_mean_burnin = 20

# Phi mean thinning, number of iteration between each Phi sample
phi_mean_thin = 10

# Save Phi means, must be set for output to be created
save_phi_means = true
phi_mean_filename = phi_means.csv

# Must be set for doc_lengths_file to be created
save_doc_lengths = true
doc_lengths_filename = doc_lengths.txt

# Must be set for term_frequencies_file to be created
save_term_frequencies = true
term_frequencies_filename = term_frequencies.txt

# lambda - relevance value when calculating relevance words
lambda = 0.6

# Save the number of times individual words occur in entire corpus
save_term_frequencies = true
term_frequencies_filename = term_frequencies.txt

# Save the vocabulary used (after, stop words, rare words, etc...)
# Order is the same as in Phi
save_vocabulary = true
vocabulary_filename = lda_vocab.txt

# The full class name (package + classname) of the sparse dirichlet sampler 
# class builder this class' 'build' method must return a class implementing 
# the SparseDirichlet interface.
# Examples:
sparse_dirichlet_sampler_builder_name = cc.mallet.types.PolyaUrnFixedCoeffPoissonDirichletSamplerBuilder
sparse_dirichlet_sampler_builder_name = cc.mallet.types.PolyaUrnDirichletSamplerBuilder

# If a directory is given instead of a filename, the instances are loaded 
# from that directory (and its subdirs). file_regex is a regular expression
# for which filenames to match, for .txt files, the regex should be .*\.txt$ 
file_regex = .*\.txt$


# Optimize hyperparameters alpha and beta every 'hyperparam_optim_interval' iteration
# -1 means no hyperparameter opitimization
hyperparam_optim_interval = 100

# Use a symmetric alpha or allow it to be non-symmetric due to hyper parameter optimization
symmetric_alpha = true | false

# The size limit where the recursive sampler will not spawn more recursive tasks
# if the document batch size is < than this value, that number of documents will
# be handled by each document sampling task
document_sampler_split_limit (default = 100)

# A pointer to the original dataset.
# Can be used to show the original text to the end user
original_dataset =  datasets/nips_orig.txt

# Keeps "connecting punctuation" in word tokens as defined by the Unicode CONNECTOR_PUNCTUATION
keep_connecting_punctuation = true

# Saves csv files with the topic indicators per iteration in the log directory
# the files are named z_XX.csv where XX is the iteration number
log_topic_indicators = true

# Do minimal pre-processing of the text. With this option all pre-processing is
# expected to have been done beforehand. It just tokenizes the text to words 
# based on Unicode SPACE and LINE separator classes
# It does NOT:
# * lowercase
# * remove punctuation (commas, periods, colon, etc) or quotes
# * remove special characters parenthesis, underscores, split compound words ("hell-bent")
# * remove numbers
no_preprocess = true

