package uk.ac.bbk.REx.annotators;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
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
    private Map<String, String> acronymIDs;
    private Map<String, String> acronymInchis;
	
	@Override
	public void initialize(UimaContext aContext) throws ResourceInitializationException
	{
		LOGGER.log(Level.INFO, "Initialising CHEBI annotator.");
		super.initialize(aContext);
        File userdata = new File((String)getContext().getConfigParameterValue(("userdataDir")));
		
		try 
		{
			db = new CHEBIDB(userdata);
		} 
		catch (CHEBIException e) 
		{
			throw new ResourceInitializationException(e);
		}
        catch (UnsupportedEncodingException e)
        {
            throw new ResourceInitializationException(e);
        }

        acronymIDs = new HashMap<String, String>();
        acronymInchis = new HashMap<String, String>();
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
            if(c.getRefersTo() != null)
            {
                Chemical parent = (Chemical)JCasUtils.getParentAnnotation((uk.ac.bbk.REx.types.Annotation)cAnnotation);

                if(parent.getChebiID() != null && parent.getInChiString() != null)
                {
                    c.setChebiID(parent.getChebiID());
                    c.setInChiString(parent.getInChiString());
                }
                else
                {
                    try
                    {
                        int chebiID = db.getCHEBIID(parent.getCoveredText());
                        parent.setChebiID(String.valueOf(chebiID));
                        c.setChebiID(String.valueOf(chebiID));

                        String inchi = db.getInchi(chebiID);
                        if(inchi != null)
                        {
                            inchi = inchi.replaceAll("/[pmstb].+", "");
                        }
                        parent.setInChiString(inchi);
                        c.setInChiString(inchi);
                    }
                    catch (Exception e)
                    {
                        try
                        {
                            int chebiID = db.getCHEBIID(c.getCoveredText());
                            parent.setChebiID(String.valueOf(chebiID));
                            c.setChebiID(String.valueOf(chebiID));

                            String inchi = db.getInchi(chebiID);
                            if(inchi != null)
                            {
                                inchi = inchi.replaceAll("/[pmstb].+", "");
                            }
                            parent.setInChiString(inchi);
                            c.setInChiString(inchi);

                            for(Annotation annotation : cas.getAnnotationIndex(Chemical.type))
                            {
                                if(annotation.getCoveredText().equals(parent.getCoveredText()))
                                {
                                    Chemical chemical = (Chemical)annotation;
                                    chemical.setChebiID(parent.getChebiID());
                                    chemical.setInChiString(parent.getInChiString());

                                    acronymIDs.put(annotation.getCoveredText(), parent.getChebiID());
                                    acronymInchis.put(annotation.getCoveredText(), parent.getInChiString());
                                }
                            }
                        }
                        catch (CHEBIException e2)
                        {
                            throw new AnalysisEngineProcessException(e2);
                        }
                        catch (NameNotFoundException e2)
                        {
                            if(c.getConfidence() <= 0.6)
                            {
                                toBeRemoved.add(c);
                            }
                        }
                        catch (NameTooShortException e2)
                        {
                            toBeRemoved.add(c);
                        }
                    }
                }
            }
            else
            {
                try
                {
                    int chebiID = db.getCHEBIID(c.getCoveredText());
                    c.setChebiID(String.valueOf(chebiID));

                    String inchi = db.getInchi(chebiID);
                    if(inchi != null)
                    {
                        inchi = inchi.replaceAll("/[pmstb].+", "");
                    }
                    c.setInChiString(inchi);
                }
                catch (CHEBIException e)
                {
                    throw new AnalysisEngineProcessException(e);
                }
                catch (NameNotFoundException e)
                {
                    if(acronymIDs.containsKey(c.getCoveredText()))
                    {
                        c.setChebiID(acronymIDs.get(c.getCoveredText()));
                        c.setInChiString(acronymInchis.get(c.getCoveredText()));
                    }
                    else if(c.getConfidence() <= 0.6)
                    {
                        toBeRemoved.add(c);
                    }
                }
                catch (NameTooShortException e)
                {
                    toBeRemoved.add(c);
                }
            }
		}
		
		for(Chemical c : toBeRemoved)
		{
			c.removeFromIndexes();
		}
	}
}
