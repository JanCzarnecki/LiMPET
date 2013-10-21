package uk.ac.bbk.REx.annotators;

import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import opennlp.uima.Token;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;

import banner.types.uima.Gene;

import uk.ac.bbk.REx.types.Chemical;
import uk.ac.bbk.REx.types.ReactionKeyword;
import uk.ac.bbk.REx.utils.CharacterIndex;
import uk.ac.bbk.REx.utils.JCasUtils;
import uk.ac.bbk.REx.utils.StemmerUtils;

/**
 * A UIMA annotator that marks up various types of keywords relating to metabolic reactions.
 */
public class ReactionKeywordAnnotator extends JCasAnnotator_ImplBase
{
	private final static Logger LOGGER = Logger.getLogger(AcronymAnnotator.class.getName());
	private Set<String> reactionWords;
	private Set<String> productionWords;
	
	@Override
	public void initialize(UimaContext aContext) throws ResourceInitializationException
	{
		super.initialize(aContext);
		LOGGER.log(Level.INFO, "Initialising reaction keyword annotator.");
		
		reactionWords = new HashSet<String>();
		productionWords = new HashSet<String>();
	
		Scanner reactionWordsSc = new Scanner(getClass().getResourceAsStream((String)getContext().getConfigParameterValue(("reactionWords"))));
		while(reactionWordsSc.hasNextLine())
		{
			reactionWords.add(reactionWordsSc.next());
		}
		
		Scanner productionWordsSc = new Scanner(getClass().getResourceAsStream((String)getContext().getConfigParameterValue(("productionWords"))));
		while(productionWordsSc.hasNextLine())
		{
			productionWords.add(productionWordsSc.next());
		}
	}
	
	@Override
	public void process(JCas cas) throws AnalysisEngineProcessException
	{
		LOGGER.log(Level.INFO, "Marking up reaction keywords in article " + JCasUtils.getPubMedID(cas));
		CharacterIndex<Gene> geneIndex = new CharacterIndex<Gene>(cas, Gene.type);
		CharacterIndex<Chemical> chemicalIndex = new CharacterIndex<Chemical>(cas, Chemical.type);
		
		for(Annotation token : cas.getAnnotationIndex(Token.type))
		{
			//If the token is part of a gene or chemical, skip it.
			if(geneIndex.getOverlappingAnnotations(token).size() > 0
					|| chemicalIndex.getOverlappingAnnotations(token).size() > 0)
			{
				continue;
			}
			
			String stem = StemmerUtils.getStem(token.getCoveredText());
			
			if(reactionWords.contains(stem))
			{
				createKeyword(cas, token, "reaction");
			}
			else if(productionWords.contains(stem))
			{
				createKeyword(cas, token, "production");
			}
			else if(stem.equals("to")
					|| stem.equals("into"))
			{
				createKeyword(cas, token, "to");
			}
			else if(stem.equals("from"))
			{
				createKeyword(cas, token, "from");
			}
			else if(stem.equals("and")
					|| stem.equals("with"))
			{
				createKeyword(cas, token, "and");
			}
			else if(stem.equals("catalys")
					|| stem.equals("catalyz"))
			{
				createKeyword(cas, token, "catalyse");
			}
			else if(stem.equals("by"))
			{
				createKeyword(cas, token, "by");
			}
			else if(stem.equals("with"))
			{
				createKeyword(cas, token, "with");
			}
		}
	}
	
	private void createKeyword(JCas cas, Annotation token, String keywordType)
	{
		ReactionKeyword rk = new ReactionKeyword(cas);
		rk.setBegin(token.getBegin());
		rk.setEnd(token.getEnd());
		rk.setKeywordType(keywordType);
		rk.addToIndexes();
	}
}
