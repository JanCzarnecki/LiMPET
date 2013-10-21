package uk.ac.bbk.REx.annotators;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import uk.ac.bbk.REx.types.Section;
import uk.ac.bbk.REx.utils.JCasUtils;

public class SectionAnnotator extends JCasAnnotator_ImplBase 
{
	private final static Logger LOGGER = Logger.getLogger(AcronymAnnotator.class.getName());
	private List<Pattern> methodPatts;
	private List<Pattern> resultsPatts;
	private List<Pattern> discussionPatts;
	private List<Pattern> referencesPatts;
	
	@Override
	public void initialize(UimaContext aContext) throws ResourceInitializationException
	{
		super.initialize(aContext);
		
		methodPatts = new ArrayList<Pattern>();
		resultsPatts = new ArrayList<Pattern>();
		discussionPatts = new ArrayList<Pattern>();
		referencesPatts = new ArrayList<Pattern>();
		
		methodPatts.add(Pattern.compile("Materials and [Mm]ethods\\s+(?!Results)[A-Z0-9]"));
		methodPatts.add(Pattern.compile("MATERIALS AND METHODS\\s+(?!RESULTS)[A-Z0-9]"));
		methodPatts.add(Pattern.compile("Methods\\s+(?!Results)[A-Z0-9]"));
		methodPatts.add(Pattern.compile("METHODS\\s+(?!RESULTS)[A-Z0-9]"));
		methodPatts.add(Pattern.compile("Experimental (P|p)rocedures\\s+(?!Results)[A-Z0-9]"));
		methodPatts.add(Pattern.compile("EXPERIMENTAL PROCEDURES\\s+(?!RESULTS)[A-Z0-9]"));
		
		resultsPatts.add(Pattern.compile("Results\\s+(?!Discussion)[A-Z0-9]"));
		resultsPatts.add(Pattern.compile("RESULTS\\s+(?!DISCUSSION)[A-Z0-9]"));
		
		discussionPatts.add(Pattern.compile("Discussion\\s+(?!References)[A-Z0-9]"));
		discussionPatts.add(Pattern.compile("DISCUSSION\\s+(?!References)[A-Z0-9]"));
		
		referencesPatts.add(Pattern.compile("References\\s+[A-Z0-9]"));
		referencesPatts.add(Pattern.compile("REFERENCES\\s+[A-Z0-9]"));
	}
	
	@Override
	public void process(JCas cas) throws AnalysisEngineProcessException 
	{
		LOGGER.log(Level.INFO, "Finding sections in article " + JCasUtils.getPubMedID(cas));
		String text = cas.getDocumentText();
		
		TreeMap<Integer, String> divisions = new TreeMap<Integer, String>();
		Map<String, Integer> sections = new HashMap<String, Integer>();
		
		for(Pattern p : methodPatts)
		{
			Matcher m = p.matcher(text);
			if(m.find())
			{
				divisions.put(m.start(), "methods");
				sections.put("methods", m.start());
				break;
			}
		}
		
		for(Pattern p : resultsPatts)
		{
			Matcher m = p.matcher(text);
			if(m.find())
			{
				divisions.put(m.start(), "results");
				sections.put("results", m.start());
				break;
			}
		}
		
		for(Pattern p : discussionPatts)
		{
			Matcher m = p.matcher(text);
			if(m.find())
			{
				divisions.put(m.start(), "discussion");
				sections.put("discussion", m.start());
				break;
			}
		}
		
		for(Pattern p : referencesPatts)
		{
			Matcher m = p.matcher(text);
			if
			(
					m.find() &&
						(sections.containsKey("discussion") && m.start() > sections.get("discussion")
						||
						!sections.containsKey("discussion"))
			)
			{
				divisions.put(m.start(), "references");
				sections.put("references", m.start());
				break;
			}
		}
		
		String lastSection = null;
		
		for(int division : divisions.keySet())
		{
			if(lastSection != null)
			{
				Section section = new Section(cas);
				section.setBegin(sections.get(lastSection));
				section.setEnd(division-1);
				section.setName(lastSection);
				section.addToIndexes();
			}
			
			lastSection = divisions.get(division);
			
			if(division == divisions.lastKey())
			{
				Section section = new Section(cas);
				section.setBegin(division);
				section.setEnd(text.length());
				section.setName(divisions.get(division));
				section.addToIndexes();
			}
		}
	}

}
