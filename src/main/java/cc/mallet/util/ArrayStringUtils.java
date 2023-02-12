package cc.mallet.util;

import java.text.DecimalFormat;

public class ArrayStringUtils {
	public static DecimalFormat mydecimalFormat = new DecimalFormat("00.###E0");
	private static String DEFAULT_TITLE  = "Vector";
	public static int noDigits = 4;
	
	public static String toStringFixedWidth(int[] a, int width) {
		String sWidth = "" + width;
		if (a == null)
			return "null";
		int iMax = a.length - 1;
		if (iMax == -1)
			return "[]";

		StringBuilder b = new StringBuilder();
		b.append('[');
		for (int i = 0; ; i++) {
			b.append(String.format("% "+sWidth+"d", a[i]));
			if (i == iMax)
				return b.append(']').toString();
			b.append(", ");
		}
	}

	public static String toStringFixedWidth(short[] a, int width) {
		String sWidth = "" + width;
		if (a == null)
			return "null";
		int iMax = a.length - 1;
		if (iMax == -1)
			return "[]";

		StringBuilder b = new StringBuilder();
		b.append('[');
		for (int i = 0; ; i++) {
			b.append(String.format("% "+sWidth+"d", a[i]));
			if (i == iMax)
				return b.append(']').toString();
			b.append(", ");
		}
	}

	public static String toStringFixedWidth(float[] a, int intWidth, int decimalWidth) {
		String sWidth = intWidth + "." + decimalWidth;
		if (a == null)
			return "null";

		int iMax = a.length - 1;
		if (iMax == -1)
			return "[]";

		StringBuilder b = new StringBuilder();
		b.append('[');
		for (int i = 0; ; i++) {
			b.append(String.format("% "+sWidth+"f", a[i]));
			if (i == iMax)
				return b.append(']').toString();
			b.append(", ");
		}
	}

	public static String toStringFixedWidth(double[] a, int intWidth, int decimalWidth) {
		String sWidth = intWidth + "." + decimalWidth;
		if (a == null)
			return "null";
		int iMax = a.length - 1;
		if (iMax == -1)
			return "[]";

		StringBuilder b = new StringBuilder();
		b.append('[');
		for (int i = 0; ; i++) {
			b.append(String.format("% "+sWidth+"f", a[i]));
			if (i == iMax)
				return b.append(']').toString();
			b.append(", ");
		}
	}

	public static String toStringFixedWidthAsInt(double[] a, int intWidth) {
		String sWidth = intWidth + "";
		if (a == null)
			return "null";
		int iMax = a.length - 1;
		if (iMax == -1)
			return "[]";

		StringBuilder b = new StringBuilder();
		b.append('[');
		for (int i = 0; ; i++) {
			b.append(String.format("% "+sWidth+"d", a[i]));
			if (i == iMax)
				return b.append(']').toString();
			b.append(", ");
		}
	}

	public static String toStringFixedWidthWithIndex(int[] a, int width) {
		String sWidth = "" + width;
		if (a == null)
			return "null";
		int iMax = a.length - 1;
		if (iMax == -1)
			return "[]";

		StringBuilder b = new StringBuilder();
		b.append('[');
		for (int i = 0; ; i++) {
			b.append("(" + i + "=>"  + String.format("% "+sWidth+"d", a[i]) + ")");
			if (i == iMax)
				return b.append(']').toString();
			b.append(", ");
		}	
	}

	public static String arrToStr(int [] arr, String title) {
		String res = "";
		res += title + "[" +  arr.length + "]:[";
		for (int j = 0; j < arr.length; j++) {
			res += arr[j];
			if(j+1<arr.length) 
				res += ", ";
		}
		return res + "]";
	}

	public static String arrToCsv(int [] arr) {
		String res = "";
		for (int j = 0; j < arr.length; j++) {
			res += arr[j];
			if(j+1<arr.length) 
				res += ", ";
		}
		return res;
	}

	public static String arrToCsv(double [] arr) {
		String res = "";
		for (int j = 0; j < arr.length; j++) {
			res += arr[j];
			if(j+1<arr.length) 
				res += ", ";
		}
		return res;
	}

	public static String arrToStr(int [] arr) {
		return arrToStr(arr, "IntVector");
	}

	public static String arrToStr(double [] arr) {
		return arrToStr(arr, DEFAULT_TITLE, Integer.MAX_VALUE);
	}

	public static String arrToStr(double [] arr, int maxLen) {
		return arrToStr(arr, DEFAULT_TITLE, maxLen);
	}
	
	public static String arrToStr(double [] arr, String title) {
		return arrToStr(arr, title, Integer.MAX_VALUE);
	}

	public static String arrToStr(double [] arr, String title, int maxLen) {
		String res = "";
		res += title + "[" +  arr.length + "]:";
		for (int j = 0; j < arr.length && j < maxLen; j++) {
			res += formatDouble(arr[j]) + ", ";
		}
		return res;
	}

	public static String doubleArrayToPrintString(double[][] m) {
		return doubleArrayToPrintString(m, ", ", Integer.MAX_VALUE, m.length, Integer.MAX_VALUE, "\n");
	}

	public static String doubleArrayToPrintString(double[][] m, int maxRows) {
		return doubleArrayToPrintString(m, ", ", maxRows, maxRows, Integer.MAX_VALUE, "\n");
	}

	public static String doubleArrayToPrintString(double[][] m, String colDelimiter) {
		return doubleArrayToPrintString(m, colDelimiter, Integer.MAX_VALUE, -1, Integer.MAX_VALUE, "\n");
	}

	public static String doubleArrayToPrintString(double[][] m, String colDelimiter, int toprowlim) {
		return doubleArrayToPrintString(m, colDelimiter, toprowlim, -1, Integer.MAX_VALUE, "\n");
	}

	public static String doubleArrayToPrintString(double[][] m, int toprowlim, int btmrowlim) {
		return doubleArrayToPrintString(m, ", ", toprowlim, btmrowlim, Integer.MAX_VALUE, "\n");
	}

	public static String doubleArrayToPrintString(double[][] m, int toprowlim, int btmrowlim, int collim) {
		return doubleArrayToPrintString(m, ", ", toprowlim, btmrowlim, collim, "\n");
	}

	public static String doubleArrayToPrintString(double[][] m, String colDelimiter, int toprowlim, int btmrowlim) {
		return doubleArrayToPrintString(m, colDelimiter, toprowlim, btmrowlim, Integer.MAX_VALUE, "\n");
	}

	public static String doubleArrayToPrintString(double[][] m, String colDelimiter, int toprowlim, int btmrowlim, int collim) {
		return doubleArrayToPrintString(m, colDelimiter, toprowlim, btmrowlim, collim, "\n");
	}
	
	public static String doubleArrayToPrintString(double[][] m, String colDelimiter, int toprowlim, int btmrowlim, int collim, String sentenceDelimiter) {
		StringBuffer str = new StringBuffer(m.length * m[0].length);

		str.append("Dim:" + m.length + " x " + m[0].length + "\n");

		int i = 0;
		int lastCol = m[i].length - 1;
		for (; i < m.length && i < toprowlim; i++) {
			String rowPref = i < 1000 ? String.format("%03d", i) : String.format("%04d", i);
			str.append(rowPref+": [");
			for (int j = 0; j < lastCol && j < collim; j++) {
				String formatted = formatDouble(m[i][j]);
				str = str.append(formatted);
				str = str.append(colDelimiter);
			}
			if(lastCol>0)
				str = str.append(formatDouble(m[i][lastCol]));

			if( collim == Integer.MAX_VALUE) { 
				str.append("]");
			} else {
				str.append("...]");
			}
			if (i < m.length - 1) {
				str = str.append(sentenceDelimiter);
			}
		}
		if(btmrowlim<0) return str.toString();
		while(i<(m.length-btmrowlim)) i++;
		if( i < m.length) str.append("\t.\n\t.\n\t.\n");
		for (; i < m.length; i++) {
			String rowPref = i < 1000 ? String.format("%03d", i) : String.format("%04d", i);
			str.append(rowPref+": [");
			for (int j = 0; j < lastCol && j < collim; j++) {
				str = str.append(formatDouble(m[i][j]));
				str = str.append(colDelimiter);
			}
			if(lastCol>0)
				str = str.append(formatDouble(m[i][lastCol]));

			if( collim > m[i].length ) { 
				str.append("]");
			} else {
				str.append(", ...]");
			}
			if (i < m.length - 1) {
				str = str.append(sentenceDelimiter);
			}
		}
		return str.toString();
	}

	public static String formatDouble(double d) {
		if ( d == 0.0 ) {
			if(d == (long) d)
				return String.format("%1$3s",(long)d);
		    else
				return "<0.0>";
		}
		if ( d<0.0001 && d>0 || d > -0.0001 && d < 0) {
			return mydecimalFormat.format(d);
		} else {			
			if(d == (long) d) {
		        return String.format("%1$3s",(long)d);
			}
		    else {
		        return String.format("%." + noDigits + "f",d);
			}
		}
	}

	public static String doubleArrayToString(double[][] m) {
		return doubleArrayToString(m, ",");
	}

	public static String doubleArrayToString(double[][] m, String colDelimiter) {
		StringBuffer str = new StringBuffer(m.length * m[0].length);
		for (int i = 0; i < m.length; i++) {
			for (int j = 0; j < m[i].length - 1; j++) {
				str = str.append(Double.toString(m[i][j]));
				str = str.append(colDelimiter);
			}
			str = str.append(Double.toString(m[i][m[i].length - 1]));
			str = str.append("\n");
		}
		return str.toString();
	}
}
