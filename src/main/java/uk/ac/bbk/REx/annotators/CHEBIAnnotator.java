package uk.ac.bbk.REx.annotators;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;


import uk.ac.bbk.REx.db.chebiDB.CHEBIDB;
import uk.ac.bbk.REx.exception.CHEBIException;
import uk.ac.bbk.REx.exception.NameNotFoundException;
import uk.ac.bbk.REx.exception.NameTooShortException;
import uk.ac.bbk.REx.types.Chemical;
import uk.ac.bbk.REx.utils.JCasUtils;

public class CHEBIAnnotator extends JCasAnnotator_ImplBase
{
	private final static Logger LOGGER = Logger.getLogger(CHEBIAnnotator.class.getName());
	private CHEBIDB db;
	
	@Override
	public void initialize(UimaContext aContext) throws ResourceInitializationException
	{
		LOGGER.log(Level.INFO, "Initialising CHEBI annotator.");
		super.initialize(aContext);
		
		try 
		{
			db = new CHEBIDB();
		} 
		catch (CHEBIException e) 
		{
			throw new ResourceInitializationException(e);
		}
	}
	
	@Override
	public void process(JCas cas) throws AnalysisEngineProcessException
	{
		LOGGER.log(Level.INFO, "Finding CHEBI ID's for chemicals in " + JCasUtils.getPubMedID(cas));
		Set<Chemical> toBeRemoved = new HashSet<Chemical>();
		
		for(Annotation cAnnotation : cas.getAnnotationIndex(Chemical.type))
		{
            /*
			uk.ac.bbk.REx.types.Annotation internalA = (uk.ac.bbk.REx.types.Annotation)cAnnotation;
            internalA = JCasUtils.getParentAnnotation(internalA);
            Chemical c = (Chemical)internalA;
            */
            Chemical c = (Chemical)cAnnotation;

			try 
			{
				int chebiID = db.getCHEBIID(c.getCoveredText());
				c.setChebiID(String.valueOf(chebiID));
				String inchi = db.getInchi(chebiID);
				if(inchi != null)
				{
					inchi = inchi.replaceAll("/[pmst].+", "");
				}
				c.setInChiString(inchi);
			} 
			catch (CHEBIException e) 
			{
				throw new AnalysisEngineProcessException(e);
			} 
			catch (NameNotFoundException e) 
			{
				if(c.getConfidence() <= 0.6)
				{
					toBeRemoved.add(c);
				}
			} 
			catch (NameTooShortException e) 
			{
				toBeRemoved.add(c);
			}
		}
		
		for(Chemical c : toBeRemoved)
		{
			c.removeFromIndexes();
		}
	}
}
