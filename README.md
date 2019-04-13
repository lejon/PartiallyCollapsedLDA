[![Build Status](https://travis-ci.org/lejon/PartiallyCollapsedLDA.svg?branch=master)](https://travis-ci.org/lejon/PartiallyCollapsedLDA) 

![YourKit](https://www.yourkit.com/images/yklogo.png)

PC-LDA
=====
Repo for our Partially Collapsed Parallel LDA implementations described in the articles: 

Måns Magnusson, Leif Jonsson, Mattias Villani, and David Broman. (2017). Sparse Partially Collapsed MCMC for Parallel Inference in Topic Models. Journal of Computational and Graphical Statistics.

```
@article{magnusson2017sparse,
  title={Sparse Partially Collapsed MCMC for Parallel Inference in Topic Models},
  author={Magnusson, M{\aa}ns and Jonsson, Leif and Villani, Mattias and Broman, David},
  journal={Journal of Computational and Graphical Statistics},
  year={2017},
  publisher={Taylor \& Francis}
}
```

Alexander Terenin, Måns Magnusson, Leif Jonsson, and David Draper. “Polya Urn Latent Dirichlet Allocation: a doubly sparse massively parallel sampler”. In IEEE Transactions on Pattern Analysis and Machine Intelligence. 2017.

```
@inproceedings{jonsson:2018,
	author={{Terenin}, Alexander and {Magnusson}, M{\aa}ns and {Jonsson}, 
	Leif and {Draper}, David},
	title={Polya Urn Latent Dirichlet Allocation: a doubly sparse massively 
	parallel sampler},
	booktitle={Accepted for publication in IEEE Transactions on Pattern Analysis and 
	Machine Intelligence}
}
```

The toolkit is Open Source Software, and is released under the Common Public License. You are welcome to use the code under the terms of the license for research or commercial purposes, however please acknowledge its use with a citation:
  Terenin, Magnusson, Jonsson.  "Polya Urn Latent Dirichlet Allocation: a doubly sparse massively parallel sampler"

The dataset (in the datasets folder) and the stopwords file (stopwords.txt, included in the repository) should be in the same folder as you run the sampler.

Example Run command:
```
java -cp PCPLDA-X.X.X.jar cc.mallet.topics.tui.ParallelLDA --run_cfg=src/main/resources/configuration/PLDAConfig.cfg
java -jar PCPLDA-X.X.X.jar --run_cfg=src/main/resources/configuration/PLDAConfig.cfg
```

(PCPLDA-X.X.X.jar is created in the 'target' folder by the 'mvn package' command)

For very large datasets you might need to add the -Xmx60g flag to Java

Please remember that this is a research prototype and the standard disclaimers apply.
You will see printouts during unit tests, commented out code, old stuff not cleaned out yet etc.
 
But the basic sampler is tested and evaluated in a scientific manner and we have gone to great pains to ensure that it is correct.
The sampler that is referred to in the article as "PC sampler" or "PC-LDA" corresponds to the class 
'cc.mallet.topics.SpaliasUncollapsedParallelLDA' in the code for the sparse parallel and 
'cc.mallet.topics.PolyaUrnSpaliasLDA' for the Polya Urn version. 
The variable selection parts are implemented in the 'cc.mallet.topics.NZVSSpaliasUncollapsedParallelLDA' class.

An example of a "main" class is cc.mallet.topics.tui.ParallelLDA

## Installation

1. Install Apache Maven
2. Install the package using maven as follows:

```mvn package```
in bash.

Occasionally some of the "probabilistic" tests fail due to random chance. This is ok in a statistical sense but not for a test suite so this should eventually be tuned. For now if the suite is re-run it should be ok.

To install without running tests use

```mvn package -DskipTests```
in bash.


## Example run using binary (the release JAR)

```java -cp PCPLDA-4.7.3.jar cc.mallet.topics.tui.ParallelLDA --run_cfg=src/main/resources/configuration/PLDAConfig.cfg```

## Public DOI
[![DOI](https://zenodo.org/badge/13374/lejon/PartiallyCollapsedLDA.svg)](http://dx.doi.org/10.5281/zenodo.18102)

Acknowledgements
----------------
I'm a very satisfied user of the YourKit profiler. A Great product with great support. It has been sucessfully used for profiling in this project.

![YourKit](https://www.yourkit.com/images/yklogo.png)

YourKit supports open source projects with its full-featured Java Profiler.
YourKit, LLC is the creator of [YourKit Java Profiler](https://www.yourkit.com/java/profiler/)
and [YourKit .NET Profiler](https://www.yourkit.com/.net/profiler/),
innovative and intelligent tools for profiling Java and .NET applications.

