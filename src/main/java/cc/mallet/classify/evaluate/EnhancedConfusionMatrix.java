package cc.mallet.classify.evaluate;

import cc.mallet.classify.Classification;
import cc.mallet.classify.Trial;
import cc.mallet.classify.evaluate.ConfusionMatrix;
import cc.mallet.types.Instance;
import cc.mallet.types.LabelAlphabet;
import cc.mallet.types.LabelVector;
import cc.mallet.types.Labeling;


public class EnhancedConfusionMatrix extends ConfusionMatrix {
	protected String MATRIX_TYPE = "Confusion Matrix";
	protected int numClasses;
	// Since it is package private in parent...
	protected Trial myTrial;
	protected int [][] myValues;
	protected int numCorrect = 0;
	protected int total = 0;
	protected double averageAccuracy = 0.0;
	
	public EnhancedConfusionMatrix(Trial t) {
		super(t);
		
		this.myTrial = t;
		Labeling tempLabeling =
			((Classification)t.get(0)).getLabeling();
		this.numClasses = tempLabeling.getLabelAlphabet().size();
		
		myValues = new int[numClasses][numClasses];
		for(int i=0; i < t.size(); i++)
		{
			LabelVector lv =
				((Classification)t.get(i)).getLabelVector();
			Instance inst = ((Classification)t.get(i)).getInstance();
			int bestIndex = lv.getBestIndex();
			int correctIndex = inst.getLabeling().getBestIndex();
			assert(correctIndex != -1);
			//System.out.println("Best index="+bestIndex+". Correct="+correctIndex);
			myValues[correctIndex][bestIndex]++;
			total++;
		}		
		
		for (int i = 0; i < t.size(); i++)
			if (t.get(i).bestLabelIsCorrect())
				numCorrect++;
		
		averageAccuracy = (double) numCorrect / total;
	}
	
	public EnhancedConfusionMatrix(Trial [] ts) {
		super(ts[0]);
		
		MATRIX_TYPE = "Combined Confusion Matrix";
		
		Trial ttmp = ts[0];
		this.myTrial = ttmp;
		Labeling tempLabeling =
				((Classification)ttmp.get(0)).getLabeling();
		this.numClasses = tempLabeling.getLabelAlphabet().size();
		myValues = new int[numClasses][numClasses];
		
		for (Trial t : ts) {
			for(int i=0; i < t.size(); i++)
			{
				LabelVector lv =
					((Classification)t.get(i)).getLabelVector();
				Instance inst = ((Classification)t.get(i)).getInstance();
				int bestIndex = lv.getBestIndex();
				int correctIndex = inst.getLabeling().getBestIndex();
				assert(correctIndex != -1);
				//System.out.println("Best index="+bestIndex+". Correct="+correctIndex);
				myValues[correctIndex][bestIndex]++;
				total++;
				if (t.get(i).bestLabelIsCorrect())
					numCorrect++;
			}					
		}
		averageAccuracy = (double) numCorrect / total;
	}
	
	public String toCsv(String sep) {
		StringBuffer sb = new StringBuffer ();
		int maxLabelNameLength = 0;
		LabelAlphabet labelAlphabet = myTrial.getClassifier().getLabelAlphabet();
		for (int i = 0; i < numClasses; i++) {
			int len = labelAlphabet.lookupLabel(i).toString().length();
			if (maxLabelNameLength < len)
				maxLabelNameLength = len;
		}

		sb.append ("Label (R=true C=Predicted)" + sep);
		for (int c2 = 0; c2 < numClasses; c2++) sb.append (labelAlphabet.lookupLabel(c2).toString() + sep);
		sb.append ("total\n");
		for (int c = 0; c < numClasses; c++) {
			String labelName = labelAlphabet.lookupLabel(c).toString();
			sb.append (labelName + sep);
			for (int c2 = 0; c2 < numClasses; c2++) {
				sb.append (myValues[c][c2] + sep);
			}
			sb.append (sum(myValues[c]));
			sb.append ('\n');
		}
		sb.append (sep);
		int [] colsums = colSum(myValues);
		for (int c = 0; c < numClasses; c++) {
			sb.append(colsums[c] + sep);
		}
		sb.append (sum(myValues));
		sb.append ('\n');
		return sb.toString(); 
	}
	
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer ();
		int maxLabelNameLength = 0;
		LabelAlphabet labelAlphabet = myTrial.getClassifier().getLabelAlphabet();
		for (int i = 0; i < numClasses; i++) {
			int len = labelAlphabet.lookupLabel(i).toString().length();
			if (maxLabelNameLength < len)
				maxLabelNameLength = len;
		}

		sb.append (MATRIX_TYPE + ", row=true, column=predicted  accuracy=" + averageAccuracy + " Correct=" + numCorrect + "\n");
		for (int i = 0; i < maxLabelNameLength-5+4; i++) sb.append (' ');
		sb.append ("label");
		for (int c2 = 0; c2 < Math.min(10,numClasses); c2++)	sb.append ("   "+c2);
		for (int c2 = 10; c2 < numClasses; c2++)	sb.append ("  "+c2);
		sb.append ("  |total\n");
		for (int c = 0; c < numClasses; c++) {
			appendJustifiedInt (sb, c, false);
			String labelName = labelAlphabet.lookupLabel(c).toString();
			for (int i = 0; i < maxLabelNameLength-labelName.length(); i++) sb.append (' ');
			sb.append (" "+labelName+" ");
			for (int c2 = 0; c2 < numClasses; c2++) {
				appendJustifiedInt (sb, myValues[c][c2], true);
				sb.append (' ');
			}
			sb.append (" |"+ sum(myValues[c]));
			sb.append ('\n');
		}
		sb.append ("----");
		for (int i = 0; i < maxLabelNameLength; i++) sb.append ('-');
		sb.append ("+");
		for (int c = 0; c < numClasses; c++) {
			sb.append ("----");
		}
		sb.append (" |");
		sb.append ('\n');
		sb.append ("     ");
		int [] colsums = colSum(myValues);
		for (int i = 0; i < maxLabelNameLength; i++) sb.append (' ');
		for (int c = 0; c < numClasses; c++) {
			appendJustifiedInt (sb, colsums[c], true);
			sb.append (' ');
		}
		sb.append (" |"+ sum(myValues));

		return sb.toString(); 
	}
	
	// So, I'll copy paste this since it is private...
	static private void appendJustifiedInt (StringBuffer sb, int i, boolean zeroDot) {
		if (i < 100) sb.append (' ');
		if (i < 10) sb.append (' ');
		if (i == 0 && zeroDot)
			sb.append (".");
		else
			sb.append (""+i);
	}
	
	public static double sum(double [] vector) {
		double res = 0.0;
		for (int i = 0; i < vector.length; i++) {
			res += vector[i];
		}
		return res;
	}
	
	// Unit Tested
	/**
	 * @param vector
	 * @return sum of all values in the matrix
	 */
	public static int sum(int [] vector) {
		int sum = 0;
		for (int i = 0; i < vector.length; i++) {
			sum+=vector[i];
		}
		return sum;
	}
	
	/**
	 * @param matrix
	 * @return sum of all values in the matrix
	 */
	public static int sum(int [][] matrix) {
		int sum = 0;
		for (int i = 0; i < matrix.length; i++) {
			for (int j = 0; j < matrix[0].length; j++) {
				sum+=matrix[i][j];
			}
		}
		return sum;
	}

	public static int [] colSum(int [][] matrix) {
		int [] result = new int[matrix[0].length];
		for (int j = 0; j < matrix[0].length; j++) {
			int rowsum = 0;
			for (int i = 0; i < matrix.length; i++) {
				rowsum += matrix[i][j];
			}
			result[j] = rowsum;
		}
		return result;
	}

}
