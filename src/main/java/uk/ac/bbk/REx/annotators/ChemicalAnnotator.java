package uk.ac.bbk.REx.annotators;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import uk.ac.bbk.REx.types.Chemical;
import uk.ac.bbk.REx.utils.JCasUtils;
import uk.ac.cam.ch.wwmm.oscar.Oscar;
import uk.ac.cam.ch.wwmm.oscar.document.NamedEntity;
import uk.ac.cam.ch.wwmm.oscar.types.NamedEntityType;

/**
 * A UIMA annotator that uses OSCAR4 to find small molecules in the text and marks them as Chemicals.
 */
public class ChemicalAnnotator extends JCasAnnotator_ImplBase
{
	private final static Logger LOGGER = Logger.getLogger(ChemicalAnnotator.class.getName());
	private Oscar oscar;
	
	@Override
	public void initialize(UimaContext aContext) throws ResourceInitializationException
	{
		LOGGER.log(Level.INFO, "Initialising chemical annotator.");
		super.initialize(aContext);
		oscar = new Oscar();
	}

	@Override
	public void process(JCas cas) throws AnalysisEngineProcessException
	{
		LOGGER.log(Level.INFO, "Finding chemical entities in article " + JCasUtils.getPubMedID(cas));
		List<NamedEntity> entities = oscar.findNamedEntities(cas.getDocumentText());
		LOGGER.log(Level.INFO, "Found chemical entities in article " + JCasUtils.getPubMedID(cas));
		for (NamedEntity ne : entities) 
		{
			if(ne.getType() == NamedEntityType.COMPOUND)
			{
				Chemical newChemical = new Chemical(cas);
				newChemical.setBegin(ne.getStart());
				newChemical.setEnd(ne.getEnd());
				newChemical.setConfidence(ne.getConfidence());
				newChemical.addToIndexes();
			}
		}
	}

}
