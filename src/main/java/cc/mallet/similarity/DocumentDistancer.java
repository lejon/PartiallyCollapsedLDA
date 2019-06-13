package cc.mallet.similarity;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToDoubleFunction;

import cc.mallet.configuration.LDAConfiguration;
import cc.mallet.pipe.Pipe;
import cc.mallet.types.FeatureSequence;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.util.IndexSorter;

public class DocumentDistancer  {
	static DecimalFormat mydecimalFormat = new DecimalFormat("00.###E0");
	public static int noDigits = 4;

	public boolean abort = false;
	
	InstanceList trainingset;
	List<Instance> trainingsetList = new ArrayList<>();
	int noClassified = 0;
	double alpha;
	String [] testRowIds;
	Map<Instance,double []> testCoordinates = new HashMap<>();
	double [][] trainingCoordinates;
	
	Distance dist = new KLDistance();
	Vectorizer instanceVectorizer;


	Pipe instancePipe;
	LDAConfiguration config;

	
	public DocumentDistancer(LDAConfiguration config) {
		this.config = config;
	}

	public DocumentDistancer(LDAConfiguration config, Distance d, Vectorizer v) {
		this.instanceVectorizer = v;
		this.dist = d;
		this.config = config;
	}

	public Vectorizer getInstanceVectorizer() {
		return instanceVectorizer;
	}
	
	public void setInstanceVectorizer(Vectorizer instanceVectorizer) {
		this.instanceVectorizer = instanceVectorizer;
	}
	
	public Distance getDist() {
		return dist;
	}

	public void setDist(Distance dist) {
		this.dist = dist;
	}
	
	public void setNoTRainingSamples(int samples) {
		trainingCoordinates = new double[samples][];
	}
	
	double [] distance(Instance testInstance) {
		boolean serial = true;
		if(serial) {
			return distanceSerial(testInstance);
		} else {
			return distancePar(testInstance);
		}
	}
	
	double [] distanceSerial(Instance testInstance) {
		double[] distances = new double[trainingCoordinates.length];
		double [] testDoc = instanceVectorizer.instanceToVector(testInstance);
		testCoordinates.put(testInstance, testDoc);
		
		FeatureSequence testTokenSeq = (FeatureSequence) testInstance.getData();
		 
		
		// try-with-resource block
		//try (ProgressBar pb = new ProgressBar("Test", trainingCoordinates.length)) { // name, initial max
//		 // Use ProgressBar("Test", 100, ProgressBarStyle.ASCII) if you want ASCII output style
//		  for ( /* TASK TO TRACK */ ) {
//		    pb.step(); // step by 1
//		    pb.stepBy(n); // step by n
//		    ...
//		    pb.stepTo(n); // step directly to n
//		    ...
//		    pb.maxHint(n);
//		    // reset the max of this progress bar as n. This may be useful when the program
//		    // gets new information about the current progress.
//		    // Can set n to be less than zero: this means that this progress bar would become
//		    // indefinite: the max would be unknown.
//		    ...
//		    pb.setExtraMessage("Reading..."); // Set extra message to display at the end of the bar
//		  }
		
		  for (int i = 0 ; i < trainingCoordinates.length; i++) {
			  //pb.step();
			  Instance trainInst = trainingset.get(i);
			  FeatureSequence trainTokenSeq = (FeatureSequence) trainInst.getData();
			  // If both doc has length 0
			  if(trainTokenSeq.getLength()==0 && testTokenSeq.getLength()==0) {
				  distances[i] = 0.0;
				  // If one has length zero 
			  } else if(trainTokenSeq.getLength()==0 || testTokenSeq.getLength()==0) {
				  distances[i] = Double.POSITIVE_INFINITY;
			  } else {
				  double [] trainingDoc = trainingCoordinates[i];
				  //System.out.println("Doc: " + arrToStr(trainingDoc));
				  //System.out.println("Test doc: " + arrToStr(testDoc));
				  double distance;
					if(dist instanceof TrainedDistance) {
						TrainedDistance tDist = (TrainedDistance) (dist);
						distance = tDist.distanceToTrainingSample(testDoc, i);
					} else {
						distance = dist.calculate(testDoc, trainingDoc);						
					}

				  //System.out.println("Distance vs. " + LDAUtils.instanceToString(trainInst) +" is:" + distance);
				  //System.out.println("Distance is:" + distance);
				  // We need to transform the kl-divergencies (low is good) to scores (high is good)
				  distances[i] = distance;
			  }
		  }
		//} // progress bar stops automatically after completion of try-with-resource block
						
		return distances;		
	}
	
	public double [] distancePar(Instance testInstance) {
		double[] distances = new double[trainingCoordinates.length];
		double [] testDoc = instanceVectorizer.instanceToVector(testInstance);
		testCoordinates.put(testInstance,testDoc);
		
		FeatureSequence testTokenSeq = (FeatureSequence) testInstance.getData();
		
		ToDoubleFunction<Instance> tdf = new ToDoubleFunction<Instance>() {
			@Override
			public double applyAsDouble(Instance trainInst) {

				FeatureSequence trainTokenSeq = (FeatureSequence) trainInst.getData();
				// If both doc has length 0
				if(trainTokenSeq.getLength()==0 && testTokenSeq.getLength()==0) {
					return 0.0;
					// If one has length zero 
				} else if(trainTokenSeq.getLength()==0 || testTokenSeq.getLength()==0) {
					return Double.POSITIVE_INFINITY;
				} else {
					double [] trainingDoc = instanceVectorizer.instanceToVector(trainInst);
//					System.out.println("Doc: " + arrToStr(trainingDoc));
//					System.out.println("Test doc: " + arrToStr(testDoc));
					double distance = dist.calculate(testDoc, trainingDoc);
					//System.out.println("Distance vs. " + LDAUtils.instanceToString(trainInst) +" is:" + distance);
					//System.out.println("Distance is:" + distance);
					// We need to transform the kl-divergencies (low is good) to scores (high is good)
					return distance;
				}
			}
		};
		
		distances = trainingsetList.parallelStream().mapToDouble(tdf).toArray();
						
		return distances;
	}

	public double[] getClosestIdx(Instance instance) {
		double [] distances = distance(instance);
		int closest = getClosestIdx(distances);
		double closestDist = Double.POSITIVE_INFINITY;
		if(closest>=0) {
			closestDist = distances[closest];
		}
		return new double [] {(double)closest,closestDist};
	}
 
	public int getClosestIdx(double [] distances) {
		double minDist = Double.POSITIVE_INFINITY;
		int minDistIdx = -1;
		for (int j = 0; j < distances.length; j++) {
			if(minDist>distances[j]) {
				minDist = distances[j];
				minDistIdx = j;
			}
		}
		return minDistIdx;
	}
		
	public static String formatDouble(double d) {
		if ( d == 0.0 ) return "<0.0>";
		if ( d<0.0001 && d>0 || d > -0.0001 && d < 0) {
			return mydecimalFormat.format(d);
		} else {
			String formatString = "%." + noDigits + "f";
			return String.format(formatString, d);
		}
	}

	public static String arrToStr(double [] arr) {
		return arrToStr(arr, "DoubleArray", Integer.MAX_VALUE, false);
	}

	public static String arrToStr(double [] arr, String title) {
		return arrToStr(arr, title, Integer.MAX_VALUE, false);
	}

	public static String arrToStr(double [] arr, String title, boolean mark) {
		return arrToStr(arr, title, Integer.MAX_VALUE, mark);
	}
	
	public static String arrToStr(double [] arr, String title, int maxLen, boolean mark) {
		String res = "";
		res += title + "[" +  arr.length + "]:";
		for (int j = 0; j < arr.length && j < maxLen; j++) {
			if(arr[j]<0.05) {
				res += "     *, ";
			} else {				
				res += formatDouble(arr[j]) + ", ";
			}
		}
		return res;
	}
	
	public static String [] extractRowIds(InstanceList trainingSet) {
		String [] result = new String[trainingSet.size()];
		int copied = 0;
		for(Instance instance : trainingSet) {
			result[copied++] = instance.getName().toString();
		}
		return result;
	}
	
	public boolean getAbort() {
		return abort;
	}

	public void setAbort(boolean abort) {
		this.abort = abort;
	}

	public DocumentDistancer train(InstanceList trainingSet) {
		if(dist instanceof TrainedDistance) {
			TrainedDistance tDist = (TrainedDistance) (dist);
			tDist.init(trainingSet);
		}
		
		this.trainingset = trainingSet;
		
		for (Instance instance : trainingset) {			
			trainingsetList.add(instance);
		}

		trainingCoordinates = new double [trainingSet.size()][];
		int copied = 0;
		for(Instance instance : trainingSet) {
			trainingCoordinates[copied++] = instanceVectorizer.instanceToVector(instance);
		}
		return this;
	}

	public double[] getTestCoordinates(Instance instance) {
		return testCoordinates.get(instance);
	}

	public double[][] getTestCoordinates() {
		double [][] testCoords = new double[testCoordinates.size()][];
		int i = 0;
		for (double[] ds : testCoordinates.values()) {
			testCoords[i++] = ds;
		}
		return testCoords;
	}

	public double[][] getTrainCoordinates() {
		return trainingCoordinates;
	}

	public String toAnnotatedString(Instance instance) {
		String annotation = instanceVectorizer.toAnnotatedString(instance);
		if(dist instanceof LDADistancer) {
			annotation += "\n";
			
			LDADistancer ldaDist = (LDADistancer) dist; 
			int numWords = Math.min(10, instance.getAlphabet().size());
			String [][] topWords = ldaDist.getTopWords(numWords);
			
			double[] thetaEst = ldaDist.getSampledTopics(instance);
			if(thetaEst==null) {
				thetaEst = ldaDist.getSampledQueryTopics(instanceVectorizer.instanceToVector(instance));
				System.out.println("From query");
			} else {
				System.out.println("From train");
			}
			if(thetaEst!=null) {
				int [] maxIdxs = IndexSorter.getSortedIndices(thetaEst);
				System.out.println("Theta [" + thetaEst.length + "]: " + Arrays.toString(thetaEst));
				annotation += "Top Words 1: [" + maxIdxs[0] + "]" + Arrays.toString(topWords[maxIdxs[0]]) + "\n";
				annotation += "Top Words 2: [" + maxIdxs[1] + "]" + Arrays.toString(topWords[maxIdxs[1]]) + "\n";
				annotation += "Top Words 3: [" + maxIdxs[2] + "]" + Arrays.toString(topWords[maxIdxs[2]]) + "\n";
			} else {
				annotation += "Could not find Theta for: " + instance;
			}
		}
		if(dist instanceof LDALikelihoodDistance) {
			annotation += "\n";
			
			LDALikelihoodDistance ldaDist = (LDALikelihoodDistance) dist; 
			String [][] topWords = ldaDist.getTopWords(10);
			
			double[] thetaEst = ldaDist.getSampledTopics(instance);
			if(thetaEst==null) {
				thetaEst = ldaDist.getSampledQueryTopics(instanceVectorizer.instanceToVector(instance));
				System.out.println("From query");
			} else {
				System.out.println("From train");
			}
			if(thetaEst!=null) {
				int [] maxIdxs = IndexSorter.getSortedIndices(thetaEst);
				System.out.println("Theta [" + thetaEst.length + "]: " + Arrays.toString(thetaEst));
				annotation += "Top Words 1: [" + maxIdxs[0] + "]" + Arrays.toString(topWords[maxIdxs[0]]) + "\n";
				annotation += "Top Words 2: [" + maxIdxs[1] + "]" + Arrays.toString(topWords[maxIdxs[1]]) + "\n";
				annotation += "Top Words 3: [" + maxIdxs[2] + "]" + Arrays.toString(topWords[maxIdxs[2]]) + "\n";
			} else {
				annotation += "Could not find Theta for: " + instance;
			}
		}
		return  annotation;
	}
	
}
