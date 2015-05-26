PCLDA
=====
Repo for our Partially Collapsed ParallelLDA implementation described in the article: 

   @unpublished{MagnussonJonsson2015,
      author = "Måns Magnusson, Leif Jonsson, Mattias Villani, David Broman",
      title = "Parallelizing LDA using Partially Collapsed Gibbs Sampling",
      note = "http://github.com/lejon/PartiallyCollapsedLDA",
      year = 2015}

The toolkit is Open Source Software, and is released under the Common Public License. You are welcome to use the code under the terms of the license for research or commercial purposes, however please acknowledge its use with a citation:
  Magnusson, Jonsson, Villani, Broman.  "Parallelizing LDA using Partially Collapsed Gibbs Sampling."

The dataset (in the datasets folder) and the stopwords file (stopwords.txt, included in the repository) should be in the same folder as you run the sampler.

Example Run command:
java -cp PCPLDA-X.X.X.jar cc.mallet.topics.tui.ParallelLDA --run_cfg=src/main/resources/configuration/PLDAConfig.cfg
java -jar PCPLDA-X.X.X.jar --run_cfg=src/main/resources/configuration/PLDAConfig.cfg

(PCPLDA-X.X.X.jar is created in the 'target' folder by the 'mvn package' command)

For very large datasets you might need to add the -Xmx60g flag to Java

Please remember that this is a research prototype and the standard disclaimers apply.
You will see printouts during unit tests, commented out code, old stuff not cleaned out yet etc.
 
But the basic sampler is tested and evaluated in a scientific manner and we have gone to great pains to ensure that it is correct.
The sampler that is referred to in the article as "PC sampler" or "PC LDA" corresponds to the class 'cc.mallet.topics.SpaliasUncollapsedParallelLDA' in the code. The variable selection parts are implemented in the 'cc.mallet.topics.NZVSSpaliasUncollapsedParallelLDA' class.

An example of a "main" class is cc.mallet.topics.tui.ParallelLDA

## Installation

1. Install Apache Maven and run:

```mvn package```

in bash.

