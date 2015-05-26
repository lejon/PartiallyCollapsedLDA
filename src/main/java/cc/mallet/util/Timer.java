package cc.mallet.util;

import java.util.Date;

public class Timer {
	
	protected Date startdate = new Date();
	protected Date enddate;
	protected String start = startdate.toString();
	protected long startTime;
	protected long estimatedTime;
    
    public Timer() {
    }
    
    public void start() {
    	startdate = new Date();
        start = startdate.toString();
        startTime = System.nanoTime();   
    }
    
    public void stop() {
    	enddate = new Date();
    	estimatedTime = System.nanoTime() - startTime;
    }
    
    public Date getStartdate() {
		return startdate;
	}

	public void setStartdate(Date startdate) {
		this.startdate = startdate;
	}

	public Date getEnddate() {
		return enddate;
	}

	public void setEnddate(Date enddate) {
		this.enddate = enddate;
	}
	
	public long getEllapsedTime() {
		return (enddate.getTime() - startdate.getTime());
	}

	public void report(String prefix) {
	    System.err.println(prefix + estimatedTime/1000000000 + " seconds");
	    System.out.println("Started: " + start);
	    System.out.println("Ended  : " + enddate.toString());
    }
    
}
