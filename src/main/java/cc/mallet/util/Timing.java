package cc.mallet.util;

public class Timing {
	long start;
	long stop;
	String label;
	
	public Timing() {

	}
	
	public Timing(long start, long stop, String label) {
		super();
		this.start = start;
		this.stop = stop;
		this.label = label;
	}

	/**
	 * @return the start
	 */
	public long getStart() {
		return start;
	}

	/**
	 * @param start the start to set
	 */
	public void setStart(int start) {
		this.start = start;
	}

	/**
	 * @return the stop
	 */
	public long getStop() {
		return stop;
	}

	/**
	 * @param stop the stop to set
	 */
	public void setStop(int stop) {
		this.stop = stop;
	}

	/**
	 * @return the label
	 */
	public String getLabel() {
		return label;
	}

	/**
	 * @param label the label to set
	 */
	public void setLabel(String label) {
		this.label = label;
	}

}
