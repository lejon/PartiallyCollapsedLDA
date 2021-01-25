package cc.mallet.pipe;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;

import cc.mallet.types.Instance;

/**
 * This tokenizer tries to do as little as possible with the input text 
 * to facilitate doing text-preprocessing outside of LDA 
 * 
 * @author Leif Jonsson
 *
 */
public class RawTokenizer extends SimpleTokenizer {

	private static final long serialVersionUID = 1L;
	protected int tokenBufferSize = 10000;

	public RawTokenizer(File stopfile) {
		super(stopfile);
	}
	
	public RawTokenizer(int languageFlag) {
		super(languageFlag);
	}
	
	public RawTokenizer(HashSet<String> stoplist) {
		super(stoplist);
	}
	
	public RawTokenizer(File stopfile, int bufferSize) {
		super(stopfile);
		tokenBufferSize = bufferSize;
	}
	
	public RawTokenizer(int languageFlag, int bufferSize) {
		super(languageFlag);
		tokenBufferSize = bufferSize;
	}
	
	public RawTokenizer(HashSet<String> stoplist, int bufferSize) {
		super(stoplist);
		tokenBufferSize = bufferSize;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public RawTokenizer deepClone() {
		return new RawTokenizer((HashSet<String>) stoplist.clone(), tokenBufferSize);
	}
	
	public Instance pipe(Instance instance) {
		
		if (instance.getData() instanceof CharSequence) {
				
			CharSequence characters = (CharSequence) instance.getData();

			ArrayList<String> tokens = new ArrayList<String>();

			int[] tokenBuffer = new int[tokenBufferSize];
			int length = -1;

			int totalCodePoints = Character.codePointCount(characters, 0, characters.length());

			for (int i=0; i < totalCodePoints; i++) {

				int codePoint = Character.codePointAt(characters, i);
				int codePointType = Character.getType(codePoint);

				if (codePointType == Character.SPACE_SEPARATOR ||
						 codePointType == Character.LINE_SEPARATOR) {
					
					if (length != -1) {
						String token = new String(tokenBuffer, 0, length + 1);
						if (! stoplist.contains(token)) {
							tokens.add(token);
						}
						length = -1;
					}
				}
				else {
					length++;
					tokenBuffer[length] = codePoint;
				}
			}

			if (length != -1) {
				String token = new String(tokenBuffer, 0, length + 1);
				if (! stoplist.contains(token)) {
					tokens.add(token);
				}
			}

			instance.setData(tokens);
		}
		else {
			throw new IllegalArgumentException("Looking for a CharSequence, found a " + 
											   instance.getData().getClass());
		}
		
		return instance;
	}

	public int getTokenBufferSize() {
		return tokenBufferSize;
	}

	public void setTokenBufferSize(int tokenBufferSize) {
		this.tokenBufferSize = tokenBufferSize;
	}
	
}
