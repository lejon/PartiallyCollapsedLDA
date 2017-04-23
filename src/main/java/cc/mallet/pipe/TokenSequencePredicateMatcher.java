package cc.mallet.pipe;

import cc.mallet.types.Instance;
import cc.mallet.types.Token;
import cc.mallet.types.TokenSequence;

public class TokenSequencePredicateMatcher extends Pipe
{
	
	public interface Predicate<T> {
		boolean test(T query);
	}
	
	private static final long serialVersionUID = 1L;
	Predicate<String> predicate;
	
	public TokenSequencePredicateMatcher (Predicate<String> p)
	{
		this.predicate = p;
	}

	public Instance pipe (Instance carrier)
	{
		TokenSequence ts = (TokenSequence) carrier.getData();
		TokenSequence newts = new TokenSequence();
		for (int i = 0; i < ts.size(); i++) {
			Token t = ts.get(i);
			if(predicate.test(t.getText())) {
				newts.add(t.getText());
			}
		}
		carrier.setData(newts);
		return carrier;
	}
}
