package cc.mallet.util;

import java.util.ArrayList;
import java.util.List;

public class EclipseDetector {
	/**
	 * Checks incoming arguments for '-runningInEclipse || --runningInEclipse' if
	 * this is detected returns a new String [] with this removed, else returns null
	 * 
	 * @param args
	 * @return
	 */
	public static String [] runningInEclipse(String [] args) {
		boolean inEclipse = false;
		List<String> resultList = new ArrayList<String>();
		for (int i = 0; i < args.length; i++) {
			if(args[i].equals("-runningInEclipse") || args[i].equals("--runningInEclipse")) {
				inEclipse = true;
			} else {				
				resultList.add(args[i]);
			}
		}
		if(inEclipse) {
			String [] result = new String[resultList.size()];
			return resultList.toArray(result);
		} else {
			return null;
		}
	}
}
