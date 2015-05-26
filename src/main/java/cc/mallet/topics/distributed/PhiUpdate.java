package cc.mallet.topics.distributed;
import java.io.Serializable;


public class PhiUpdate implements Serializable {

	private static final long serialVersionUID = 1L;
	
	public double [][] phi; 

	public PhiUpdate(double [][] phi) {
		this.phi = phi;
	}

}
