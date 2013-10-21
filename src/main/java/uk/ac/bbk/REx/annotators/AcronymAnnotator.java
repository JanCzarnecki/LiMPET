package uk.ac.bbk.REx.annotators;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.uima.UIMAFramework;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Level;
import org.apache.uima.util.Logger;

import uk.ac.bbk.REx.types.Annotation;
import uk.ac.bbk.REx.types.Chemical;
import uk.ac.bbk.REx.types.Gene;
import uk.ac.bbk.REx.utils.CharacterIndex;
import uk.ac.bbk.REx.utils.JCasUtils;

/**
 * A UIMA annotator that finds Chemical and Gene acronyms and the entities that they reference.
 */
public class AcronymAnnotator extends JCasAnnotator_ImplBase
{
	private Logger LOGGER;
	private Pattern figurePatt;
	private Pattern noWordPatt;
	
	@Override
	public void initialize(UimaContext aContext) throws ResourceInitializationException
	{
		super.initialize(aContext);
		LOGGER = UIMAFramework.getLogger(AcronymAnnotator.class);
		LOGGER.log(Level.INFO, "Initialising acronym annotator.");
		figurePatt = Pattern.compile("Fig\\.\\s*\\d");
		noWordPatt = Pattern.compile("^[^A-Za-z]$");
	}
	
	@Override
	public void process(JCas cas) throws AnalysisEngineProcessException
	{
		LOGGER.log(Level.INFO, "Resolving acronyms in article " + JCasUtils.getPubMedID(cas));
		
		for(org.apache.uima.jcas.tcas.Annotation bg : cas.getAnnotationIndex(banner.types.uima.Gene.type))
		{
			Gene g = new Gene(cas);
			g.setBegin(bg.getBegin());
			g.setEnd(bg.getEnd());
			g.addToIndexes();
		}
		
		try
		{
			this.<Chemical>findAcronyms(cas, Chemical.type, Chemical.class);
			this.<Gene>findAcronyms(cas, Gene.type, Gene.class);
		}
		catch(NoSuchMethodException e)
		{
			throw new AnalysisEngineProcessException(e);
		}
		catch(InvocationTargetException e)
		{
			throw new AnalysisEngineProcessException(e);
		}
		catch(InstantiationException e)
		{
			throw new AnalysisEngineProcessException(e);
		}
		catch(IllegalAccessException e)
		{
			throw new AnalysisEngineProcessException(e);
		}
		catch(IllegalArgumentException e)
		{
			throw new AnalysisEngineProcessException(e);
		}
	}
	
	private <A extends Annotation> void findAcronyms(JCas cas, int type, Class<A> cl) throws InstantiationException, IllegalAccessException, IllegalArgumentException, SecurityException, InvocationTargetException, NoSuchMethodException
	{
		CharacterIndex<A> charIndex = new CharacterIndex<A>(cas, type);
		Map<String, A> acronyms = new HashMap<String, A>();
		
		//Pattern looks for a single string of characters enclosed by brackets
		//e.g. (ATP), (PPi)
		//The string of characters is captured.
		Pattern acronymDefinitionPattern = Pattern.compile("\\((\\S+?)\\)");
		Matcher acronymDefinitionMatcher = acronymDefinitionPattern.matcher(cas.getDocumentText());
		
		while(acronymDefinitionMatcher.find())
		{
			Matcher figureMat = figurePatt.matcher(acronymDefinitionMatcher.group(1));
			Matcher noWordMat = noWordPatt.matcher(acronymDefinitionMatcher.group(1));
			if(figureMat.find() || noWordMat.find())
			{
				continue;
			}
			
			int startIndex = acronymDefinitionMatcher.start(1);
			
			//If the acronym definition immediately follows an Annotation, store
			//them in the hashmap.
			if(charIndex.isAnnotationAtIndex(startIndex-3)
					&& !acronyms.containsKey(acronymDefinitionMatcher.group(1)))
			{
				acronyms.put(acronymDefinitionMatcher.group(1), charIndex.getAnnotationAtIndex(startIndex-3));
			}
		}
		
		//Find each occurrence of each acronym in the text.
		for(String acronym : acronyms.keySet())
		{
			Pattern acronymPattern = Pattern.compile("\\W(\\Q" + acronym + "\\E)\\W");
			Matcher acronymMatcher = acronymPattern.matcher(cas.getDocumentText());
			
			while(acronymMatcher.find())
			{
				if(charIndex.isAnnotationAtIndex(acronymMatcher.start(1)))
				{
					A annotation = charIndex.getAnnotationAtIndex(acronymMatcher.start(1));
					annotation.setIsAcronym(true);
					annotation.setRefersTo(acronyms.get(acronym));
				}
				else
				{
					A annotation = cl.getDeclaredConstructor(JCas.class).newInstance(cas);
					annotation.setBegin(acronymMatcher.start(1));
					annotation.setEnd(acronymMatcher.end(1));
					annotation.setIsAcronym(true);
					annotation.setRefersTo(acronyms.get(acronym));
					annotation.addToIndexes();
				}
			}
		}
	}
}
