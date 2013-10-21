package uk.ac.bbk.REx.utils;

import org.tartarus.martin.Stemmer;

/**
 * A utility class to ease the use of the Porter stemming algorithm.
 */
public class StemmerUtils
{
	private StemmerUtils(){}
	
	/**
	 * Returns the stem for a given word.
	 * 
	 * @param word
	 * @return
	 */
	public static String getStem(String word)
	{
		Stemmer st = new Stemmer();
	
		for(char c : word.toCharArray())
		{
			st.add(c);
		}
		
		st.stem();
		return st.toString();
	}
}
