package cc.mallet.pipe;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;

import cc.mallet.types.Instance;

public class NumericAlsoTokenizer extends SimpleTokenizerLarge {

	private static final long serialVersionUID = 1L;

	public NumericAlsoTokenizer(int languageFlag) {
		super(languageFlag);
	}

	public NumericAlsoTokenizer(File stopfile) {
		super(stopfile);
	}

	public NumericAlsoTokenizer(HashSet<String> stoplist) {
		super(stoplist);
	}
	
	public NumericAlsoTokenizer(int languageFlag, int maxBufSize) {
		super(languageFlag, maxBufSize);
	}

	public NumericAlsoTokenizer(File stopfile, int maxBufSize) {
		super(stopfile, maxBufSize);
	}

	public NumericAlsoTokenizer(HashSet<String> stoplist, int maxBufSize) {
		super(stoplist, maxBufSize);
	}

	@Override
	public Instance pipe(Instance instance) {
		
	if (instance.getData() instanceof CharSequence) {
			
		CharSequence characters = (CharSequence) instance.getData();

		ArrayList<String> tokens = new ArrayList<String>();

		int[] tokenBuffer = new int[tokenBufferSize];
		int length = -1;

		// Using code points instead of chars allows us
		//  to support extended Unicode, and has no significant
		//  efficiency costs.
		
		int totalCodePoints = Character.codePointCount(characters, 0, characters.length());

		for (int i=0; i < totalCodePoints; i++) {
			int codePoint = Character.codePointAt(characters, i);
			int codePointType = Character.getType(codePoint);

			if (codePointType == Character.LOWERCASE_LETTER ||
				codePointType == Character.UPPERCASE_LETTER) {
				length++;
				tokenBuffer[length] = codePoint;
			}
			else if (codePointType == Character.SPACE_SEPARATOR ||
					 codePointType == Character.LINE_SEPARATOR ||
					 codePointType == Character.PARAGRAPH_SEPARATOR ||
					 codePointType == Character.END_PUNCTUATION ||
					 codePointType == Character.DASH_PUNCTUATION ||
					 codePointType == Character.CONNECTOR_PUNCTUATION ||
					 codePointType == Character.START_PUNCTUATION ||
					 codePointType == Character.INITIAL_QUOTE_PUNCTUATION ||
					 codePointType == Character.FINAL_QUOTE_PUNCTUATION ||
					 codePointType == Character.OTHER_PUNCTUATION) {
				
				// Things that delimit words
				if (length != -1) {
					String token = new String(tokenBuffer, 0, length + 1);
					if (! stoplist.contains(token)) {
						tokens.add(token);
					}
					length = -1;
				}
			}
			else if (codePointType == Character.COMBINING_SPACING_MARK ||
					 codePointType == Character.ENCLOSING_MARK ||
					 codePointType == Character.NON_SPACING_MARK ||
					 codePointType == Character.TITLECASE_LETTER ||
					 codePointType == Character.MODIFIER_LETTER ||
					 codePointType == Character.OTHER_LETTER) {
				// Obscure things that are technically part of words.
				//  Marks are especially useful for Indic scripts.

				length++;
				tokenBuffer[length] = codePoint;
			}
			else if (codePointType == Character.DECIMAL_DIGIT_NUMBER) {
				length++;
				tokenBuffer[length] = codePoint;				
			}
			else {
				// Character.DECIMAL_DIGIT_NUMBER
				// Character.CONTROL
				// Character.MATH_SYMBOL
				//System.out.println("type " + codePointType);
				//length++;
				//tokenBuffer[length] = codePoint;				
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
}
