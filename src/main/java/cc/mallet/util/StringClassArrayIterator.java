package cc.mallet.util;

import java.net.URI;
import java.util.Iterator;

import cc.mallet.types.Instance;

/**
 * Simple Iterator for string data where you can supply the class it
 * belongs to
 * 
 * @author Leif Jonsson
 *
 */
public class StringClassArrayIterator implements Iterator<Instance> {

	String[] id;
	String[] data;
	int index = 0;
	String [] classNames;  

	public StringClassArrayIterator (String[] data)
	{
		this.data = data;
	}
	
	public StringClassArrayIterator (String[] data, String className)
	{
		this.data = data;
		this.classNames = new String[]{className};
	}
	
	public StringClassArrayIterator (String[] data, String [] classNames)
	{
		if(classNames != null && classNames.length != 1 && data.length != classNames.length) {
			throw new IllegalArgumentException("data.length != classNames.length when classNames.length != 1");
		}
		this.data = data;
		this.classNames = classNames;
	}

	public StringClassArrayIterator (String[] data, String [] classNames, String [] ids)
	{
		if(classNames != null && classNames.length != 1 && data.length != classNames.length) {
			throw new IllegalArgumentException("data.length != classNames.length when classNames.length != 1");
		}
		if(ids != null && data.length != ids.length) {
			throw new IllegalArgumentException("data.length != ids.length");
		}
		this.data = data;
		this.classNames = classNames;
		this.id = ids;
	}

	public Instance next ()
	{
		URI uri = null;
		try { 
			if(id==null) 
				uri = new URI ("" + index);
			else
				uri = new URI (id[index]);
		}
		catch (Exception e) { e.printStackTrace(); throw new IllegalStateException(); }
		String className = "<No class set>";
		if(classNames == null) className = "<No class set>";
		else if(classNames.length == 1) className = classNames[0];
		else if(classNames.length > 1) className = classNames[index];
		
		Instance i = new Instance (data[index], className, uri, null);
		index++;
		return i;
	}

	public boolean hasNext ()	{	return index < data.length;	}
	
	public void remove () {
		throw new IllegalStateException ("This Iterator<Instance> does not support remove().");
	}

}