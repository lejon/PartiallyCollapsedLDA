# Confiuration File Documentation

During research you typically want to test a lot of different options. 
Hence, the configuration tends to become more and more complex. You 
certainly don't need to know most of what is in this documentation. 
Here is a minimal working configuration:

```
topics = 20
dataset = src/main/resources/datasets/nips.txt
iterations = 400
```

Effort has been placed on providing resonable defaults for all configuration
parameters, but of course they won't fit everyone. 

Here is a mimal config using word priors:

```
topics = 20
dataset = src/main/resources/datasets/20newsgroups.txt
iterations = 400
topic_prior_filename = src/main/resources/configuration/20ng_priors.txt
tfidf_vocab_size = 10000
stoplist = stoplist-20ng-large.txt
scheme = polyaurn_priors
```

'20ng_priors.txt'

```
# Class sports:
0, sports
# Class politics:
1, politics 
# Class religion:
2, religion
# Class motor:
3, motor, car
# Class science:
4, science
# Class computing:
5, computing
# Class space:
6, space
# Class gun related:
7, gun
# Class baseball related:
8, baseball
# Class hockey related:
9, hockey
```

Minimal HDP config:

```
# K_max - max number of topics acceptable
topics = 1000
alpha = 0.1
beta = 0.01
hdp_gamma = 1
# K_init - number of topics to init the HDP with
hdp_nr_start_topics = 1
iterations = 200
scheme = ppu_hdplda_all_topics
dataset = src/main/resources/datasets/nips.txt
```

## Configs: 

Configs that will be run by the program. The program calls
the method getSubConfigs() on the config object, it will then 
get a list of the sub-configs listed in this variable. The program 
then MUST "activate" using activateSubconfig(conf) the subconfig that 
it wants to run.

!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

**SUBCONFIG NAMES CANNOT CONTAIN THE "." CHARACTER!!**

!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

```
configs = uncollapsed_parallel_adaptive, uncollapsed_parallel
```

## Subconfig:

One config file can have several sub-configs
sub-configs are activated in the code, with the
global settings and unique settings in the 
sub config. Any specific global setting is 
overridden by specific sub config settings
A completely new sampler is started for each 
subconfig

```
[uncollapsed_parallel_adaptive]
```

## Title: 

will be printed in the summary output of each run

```
title = PCPLDA
```

## Description: 

Will be printed in the summary output of each run

```
description = Partially Collapsed Parallel LDA with adaptive subsampling (not full random scan)
```

## Dataset: 

Filename of dataset file to use this file must follow a specific format

```
dataset = datasets/nips.txt
```

## Original Dataset: 

Filename of original dataset. This is useful if the dataset has been 
pre-processed externally and a reference to the original text is needed
perhaps to show the original text to the end user.
The order of the documents are assumed to be the same as in 'dataset'.
Not used by the sampler. 

```
original_dataset = datasets/nips_orig.txt
```

## Test Dataset: 

Filename of test dataset file to use, must follow same format as dataset file
if this file is given, the held out log likelihood will be calculated every 'topic_interval' 
relative to this dataset. 
This slows down execution substantially, so if you are not evaluating your model, don't set this option! 
The held out LL is written to the file "test_held_out_log_likelihood.txt"

```
test_dataset = datasets/enron.txt
```

## Scheme: 

Which sampling scheme to use (uncollapsed, collapsed, adlda)
paranoid is uncollapsed with additional (time consuming) consistency checks

Recommended LDA models:

```
scheme = "spalias"
scheme = "spalias_priors"
scheme = "polyaurn" // Default - fastest
scheme = "polyaurn_priors"
scheme = "adlda"
```

Recommended HDP models:

```
scheme =  "ppu_hdplda_all_topics"
```

Other models:

```
scheme = "uncollapsed"
scheme = "collapsed"
scheme = "lightcollapsed"
scheme = "efficient_uncollapsed"
scheme = "lightpclda"
scheme = "lightpclda_proposal"
scheme = "nzvsspalias"
```

## Seed: 
Seed for the random sampling, -1 means use clock time

```
seed = -1 # -1 => use LSB of current time as seed
```

## Topics, Alpha and Beta: 
The number of topics, alpha (this is NOT the sum of alphas!) and beta to use

```
topics = 20
```

```
alpha = 1.0
```

```
beta = 0.01
```

```
hdp_gamma = 1
```

## Iterations: 
How many iterations to sample

```
iterations = 8000
```

## Diagnostic interval: 
When to  to do a full dump of (phi, M and N), -1 means never

```
diagnostic_interval = 500, 1000 # Dump diagnostics between iteration 500 and 1000
diagnostic_interval = -1

```

## Base Out Dir
Top level directory for the output

```
base_out_dir
```

## Experiment Out Dir
Subdirectory in base_out_dir where the output will be stored

```
experiment_out_dir
```

## Batches: 

**DEPRICATED: NOT USED**

How many threads to use for Z sampling, (more threads are allocated for sampling Phi and count Updates)
there is a config variable ('document_sampler_split_limit') that controls the size limit of the fork/join
split. 

```
batches = 4
```

## Topic batches: 

**DEPRICATED: NOT USED**

How many threads for sampling Phi

```
topic_batches = 3
```

## Rare threshold: 

Min threshold for how many times a word must occur to be included in vocabulary

```
rare_threshold = 3
```

## TF-IDF: 

Specifies how many words the final dictionary will contain.
keeps the "tfidf_vocab_size" number of words with the highest TF-IDF values according to the following formula:
double tfIdf = (tf == 0 || idf == 0) ? 0.0 : (double) tf * Math.log(corpusSize / (double) idf);

**Stopwords are STILL respected!**

```
tfidf_vocab_size = 100
```

## Topic Interval: 

How often to print top words to console (and log likelihood to file) during sampling (takes time), 0 means never

```
topic_interval = 100
```

## DN Diagnostic interval: 

When to dump Delta N, either never (-1) or between iteration numbers
	Example: dn_diagnostic_interval = 10, 50, 5000, 7000 # Dump Delta N between iteration 10 and 50 and also between 5000 and 7000 

```
dn_diagnostic_interval = -1
```

## Start diagnostic:

Determines from which iteration Phi is printed to a binary file, when "print_phi" is set to true
turn off by setting to -1   

```
start_diagnostic = 500
```

## Print Phi

Default = false;

```
print_phi = false
```

## Measure Timing: If timing measurements should be done (takes time)

```
measure_timing = false
```

## Results size: 

**DEPRICATED: NOT USED**

How many documents to buffer on the workers before sending back for updates

```
results_size = 5
```

## Batch Building Scheme: 

Different algorithms for building the document batches
 
### EvenSplitBatchBuilder: 
Splits the full corpus evenly over the threads

```
batch_building_scheme = utils.randomscan.document.EvenSplitBatchBuilder
```

### PercentageBatchBuilder: 

Samples percentage_split_size_doc % of the corpus randomly without replacement in every iteration

```
batch_building_scheme = utils.randomscan.document.PercentageBatchBuilder
```

### percentage_split_size_doc
```
percentage_split_size_doc = 0.2
```

### AdaptiveBatchBuilder: 

Subclass of PercentageBatchBuilder that takes an instability period into consideration

```
batch_building_scheme = utils.randomscan.document.AdaptiveBatchBuilder
```

### FixedSplitBatchBuilder: 

Takes a fixed % specified by fixed_split_size_doc of the corpus per iteration  

```
batch_building_scheme = utils.randomscan.document.FixedSplitBatchBuilder
```

### Fixed Split Size Doc: 

Used by the FixedSplitBatchBuilder, below example, samples 20% of docs the first 4 iterations then 100% 
and then loops over these ratios for the rest of the iterations 

```
fixed_split_size_doc   = 0.2, 0.2, 0.2, 0.2, 1.0
```

## Topic index building schemes: 

Decides which words to sample in Phi each iteration (default are all)

### DeltaNTopicIndexBuilder: 

Samples the words that changes in the Z sampling

```
topic_index_building_scheme = utils.randomscan.topic.DeltaNTopicIndexBuilder
```

### TopWordsRandomFractionTopicIndexBuilder: 

Samples Types that have high frequency should be sampled more often but according
to random scan contract ALL types MUST have a small probability to be 
sampled. In 80% of the cases we draw the proportion of types to sample from a Beta distribution
with a mode centered on 20 %. Often we will sample the 20% most probable
words, but sometimes we will sample them all. In the other 20% it samples ALL words in Phi
A Beta(2.0,5.0) will have the mode (a-1) / (a+b-2) = 0.2 = 20%

```
topic_index_building_scheme = utils.randomscan.topic.TopWordsRandomFractionTopicIndexBuilder
```

### MandelbrotTopicIndexBuilder

Samples the top X% ('percent_top_tokens' from config) of the most frequent tokens in the corpus, 
respects the <code>full_phi_period</code> variable

## Percent Top Tokens
```
percent_top_tokens
```

### AllWordsTopicIndexBuilder: 

Samples the full Phi

```
topic_index_building_scheme = utils.randomscan.topic.AllWordsTopicIndexBuilder
```

## Full Phi Period:

For algorithms that cares about sample the full Phi at some interval
Currently used by DeltaNTopicIndexBuilder

```
full_phi_period = 5 # Sample full Phi ever 5:th interation
```

### PercentageTopicBatchBuilder

Samples X% of the ROWS (which topics) in Phi controlled by the percentage_split_size_topic
config parameter

### Percentage Split Size Topic

Used by PercentageTopicBatchBuilder to decide how large part of topics (rows in Phi) to sample

```
percentage_split_size_topic = 1.0 # (1.0 = 100%)
```

## Instability period: 

For Algorithms that care about instability periods (number of iterations)

```
instability_period = 125 # Iterations
instability_period = 0
```

## Print N Docs interval: 

List the intervals in which Theta should be printed for the "print_ndocs_cnt" number of documents

```
print_ndocs_interval = 50,100, 150,200
print_ndocs_cnt = 500 # Print theta for the first 500 documents
```

## Print N Topword interval: 

List the intervals in which Phi should be printed for the "print_ntopwords_cnt" words

```
print_ntopwords_interval = 50,100, 150,200
print_ntopwords_cnt = 500 # Print phi for the top 500 words
```

## Log Type Topic Density 

Should the density of the type/topic matrix be logged true/false,
observe that this incurs a performance penalty since the type/topic
matrix is looped over each time the density should be printed

```
log_type_topic_density = true
```

## Log Document Density 

```
log_document_density = true
```

## Stopword file

Filename of a file with one word per line of stopwords
default = stoplist.txt
If the 'stoplist.txt' file is not found, currently the sampler
throws a FileNotFoundException but this is caught and does not
affect the sampler. It will just continue without any stopwords

```
stoplist = stoplist.txt
```

## Debug:

```
debug = 0
```

## Document Topic Means:

Save the a file with the document topic means (can include zeros)

```
save_doc_topic_means = true
doc_topic_mean_filename = doc_topic_means.csv
```

## Document Theta Estimate:

Save the a file with document topic theta estimates (will not include zeros)
Unlike Phi means which are sampled with thinning, theta means is just a simple
average of the topic counts in the last iteration divided by the number of 
tokens in the document thus there is not theta_burnin or theta_thinning

```
save_doc_theta_estimate = true
doc_topic_theta_filename = doc_topic_theta.csv
```

## Rare Threshold:

Words that occur less than rare_threshold is removed from the corpus

```
rare_threshold = 50
```

# Log Phi density: 

Calculate the Ratio of Zeros in Phi (so should really be called Phi Sparsity I guess)

```
log_phi_density = true
```

# Phi Log Filename:

```
doc_topic_mean_filename = doc_topic_means.csv
```

# Percent burn in:

I.e percent of total number of iterations before start sampling phi mean

Example: iterations = 2000, phi_mean_burning = 50 => start sampling Phi at 1000 iterations

Default 0, start immediately.

```
phi_mean_burnin = 20
```

# Phi mean thinning:

Number of iteration between each Phi sample
Default 0.

```
phi_mean_thin = 10
```

# Save Phi mean:

Must be set for Phi to be to be saved

```
save_phi_mean = true
phi_mean_filename = phi_means.csv
```

## Save Doc Lengths:

Must be set for doc_lengths_file to be created

```
save_doc_lengths = true
doc_lengths_filename = doc_lengths.txt
```

## Save Term Frequencies:

Must be set for term_frequencies_file to be created
Save the number of times individual words occur in entire corpus

```
save_term_frequencies = true
term_frequencies_filename = term_frequencies.txt
```

# lambda:

Relevance value when calculating relevance words

```
lambda = 0.6
```

## Save Vocabulary:

Save the vocabulary used (after, stop words, rare words, etc...)
Order is the same as in Phi.

```
save_vocabulary = true
vocabulary_filename = lda_vocab.txt
```

# Sparse Dirichlet sampler class builder:

The full class name (package + classname) of the sparse Dirichlet sampler 
class builder this class'. The 'build' method must return a class implementing 
the SparseDirichlet interface.

```
sparse_dirichlet_sampler_builder_name = cc.mallet.types.PolyaUrnFixedCoeffPoissonDirichletSamplerBuilder
sparse_dirichlet_sampler_builder_name = cc.mallet.types.PolyaUrnDirichletSamplerBuilder
```

## File Regex:

If a directory is given instead of a filename, the instances are loaded 
from that directory (and its subdirs). file_regex is a regular expression
for which filenames to match, for .txt files, the regex should be .*\.txt$ 

```
file_regex = .*\.txt$
```

## Hyperparameter Optimization Interval:

Optimize hyperparameters alpha and beta every 'hyperparam_optim_interval' iteration
-1 means no hyperparameter opitimization

```
hyperparam_optim_interval = 100
```

## Symmetric Alpha:

Use a symmetric alpha or allow it to be non-symmetric due to hyper parameter optimization

```
symmetric_alpha = true | false
```

## Document Sampler Split Limit:

The size limit where the recursive sampler will not spawn more recursive tasks
if the document batch size is < than this value, that number of documents will
be handled by each document sampling task

```
document_sampler_split_limit (default = 100)
```

## Keep Connecting Punctuation:

Keeps "connecting punctuation" in word tokens as defined by the Unicode CONNECTOR_PUNCTUATION

```
keep_connecting_punctuation = true
```

## Log Topic Indicators:

Saves csv files with the topic indicators per iteration in the log directory
the files are named z_XX.csv where XX is the iteration number

```
log_topic_indicators = true
```

# No Preprocessing

Do minimal pre-processing of the text. With this option all pre-processing is
expected to have been done beforehand. It just tokenizes the text to words 
based on Unicode SPACE and LINE separator classes

It does NOT:

* lowercase

* remove punctuation (commas, periods, colon, etc) or quotes

* remove special characters parenthesis, underscores, split compound words ("hell-bent")

* remove numbers

```
no_preprocess = true
```

## Save Sampler
Saves a Java serialized version of the sampler. To continue using the same sampler
use the '--continue' command line option.
This is useful if you want to run a limited number of iterations to test or have 
limited time. You can also abort the sampler by creating a file called "abort" in the
folder where the sampler is running (just remember to remove it before starting again! :) ). 
It will then cleanly stop the sampling (as if it had finished naturally) and you can 
later continue sampling with the '--continue' command line option'. The sampler is
saved in the 'saved_sampler_dir' directory with a filename that contains a hashcode
of the configuration used. Since you can only continue sampling with the EXACT same
configuration as the sampler was started with.

```
save_sampler
```

## Saved Sampler Dir
Directory of where to save the serialized sampler

```
saved_sampler_dir
```

## Topic Batch Building Scheme
```
topic_batch_building_scheme
```

## Topic Index Building Scheme
```
topic_index_building_scheme
```

## Sub Topic Index Builders
```
sub_topic_index_builders
```

## Proportional Ib Skip Step
```
proportional_ib_skip_step
```

## Variable Selection Prior
```
variable_selection_prior
```

## Topic Prior Filename
```
topic_prior_filename
```

## Document Prior Filename
```
document_prior_filename
```

## Keep Numbers
```
keep_numbers
```

## Save Doc Topic Diagnostics
```
save_doc_topic_diagnostics
```

## Doc Topic Diagnostics Filename
```
doc_topic_diagnostics_filename
```

## Nr Top Words
```
nr_top_words
```

## Max Doc Buf Size
```
max_doc_buf_size
```

## Alias Poisson Threshold
```
alias_poisson_threshold
```

## Hdp Gamma
```
hdp_gamma
```

## Hdp Nr Start Topics
```
hdp_nr_start_topics
```

## Hdp K Percentile
```
hdp_k_percentile
```

## Log Tokens Per Topic
```
log_tokens_per_topic
```

## Save Corpus
```
save_corpus
```

## Corpus Filename
```
corpus_filename
```

## Sampler Class
```
sampler_class
```

## Iteration Callback Class
```
iteration_callback_class
```

## Topic Indicator Logging Format
Which format to use when logging topic indicators

mallet: Use the mallet format of '#doc pos typeindex type topic'
Example:
    #doc pos typeindex type topic
    0 0 0 'INSERT 20

 Default: standard - vector of topic indicators
 Example:
   0 2 3 2 0 8

```
topic_indicator_logging_format=mallet (default = "standard")
```
