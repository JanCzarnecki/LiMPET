package opennlp.tools.tokenize;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import opennlp.uima.Token;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLInputSource;
import org.junit.Before;
import org.junit.Test;

public class TokenizerTest 
{
	AnalysisEngine sentenceAE;
	AnalysisEngine tokenAE;
	JCas cas;
	
	@Before
	public void initialise() throws ResourceInitializationException, InvalidXMLException
	{
		XMLInputSource sentenceIn = new XMLInputSource(getClass().getResourceAsStream("/org/apache/opennlp/opennlpuima/desc/SentenceDetector.xml"), null);
		AnalysisEngineDescription sentenceDesc = UIMAFramework.getXMLParser().parseAnalysisEngineDescription(sentenceIn);
		sentenceAE = UIMAFramework.produceAnalysisEngine(sentenceDesc);
		
		XMLInputSource tokenIn = new XMLInputSource(getClass().getResourceAsStream("/org/apache/opennlp/opennlpuima/desc/Tokenizer.xml"), null);
		AnalysisEngineDescription tokenDesc = UIMAFramework.getXMLParser().parseAnalysisEngineDescription(tokenIn);
		tokenAE = UIMAFramework.produceAnalysisEngine(tokenDesc);
		
		cas = tokenAE.newJCas();
	}
	
	@Test
	public void test() throws AnalysisEngineProcessException 
	{
		cas.setDocumentText("Alcohol dehydrogenase dehydrogenates ethanol.");
		sentenceAE.process(cas);
		tokenAE.process(cas);
		
		List<String> results = new ArrayList<String>();
		for(Annotation token : cas.getAnnotationIndex(Token.type))
		{
			results.add(token.getCoveredText());
		}
		
		assertEquals("[Alcohol, dehydrogenase, dehydrogenates, ethanol, .]", results.toString());
	}
}
