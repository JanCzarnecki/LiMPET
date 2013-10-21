package uk.ac.bbk.REx.annotators;

import hu.u_szeged.rgai.bio.uima.tagger.LinnaeusSpecies;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;

import uk.ac.bbk.REx.types.Chemical;
import uk.ac.bbk.REx.types.Gene;
import uk.ac.bbk.REx.utils.CharacterIndex;
import uk.ac.bbk.REx.utils.JCasUtils;

/**
 * A UIMA annotator that resolves gene/protein conflicts caused by the GeneAnnotator and ChemicalAnnotator.
 */
public class GeneChemicalConflictResolver extends JCasAnnotator_ImplBase
{
	private final static Logger LOGGER = Logger.getLogger(GeneChemicalConflictResolver.class.getName());
	
	@Override
	public void initialize(UimaContext aContext) throws ResourceInitializationException
	{
		super.initialize(aContext);
		LOGGER.log(Level.INFO, "Initialising gene/chemical conflict resolver.");
	}
	
	@Override
	public void process(JCas cas) throws AnalysisEngineProcessException
	{
		LOGGER.log(Level.INFO, "Resolving conflicts in article " + JCasUtils.getPubMedID(cas));
		CharacterIndex<Chemical> chemicalCharacterIndex = new CharacterIndex<Chemical>(cas, Chemical.type);
		CharacterIndex<Gene> geneCharacterIndex = new CharacterIndex<Gene>(cas, Gene.type);
		Set<Annotation> annotationsToRemove = new HashSet<Annotation>();
		
		for(Annotation annotation : cas.getAnnotationIndex(Gene.type))
		{
			Gene geneAnnotation = (Gene)annotation;
			List<Chemical> overlappingChemicals = chemicalCharacterIndex.getOverlappingAnnotations(geneAnnotation);
			
			//If multiple chemicals overlap with a gene then is marked up as a gene.
			//This is typical with long enzyme names.
			if(overlappingChemicals.size() > 1)
			{
				annotationsToRemove.addAll(overlappingChemicals);
			}
			else if(overlappingChemicals.size() == 1)
			{
				for(Chemical chemicalAnnotation : overlappingChemicals)
				{
					//If the chemical annotation is shorter than the gene, the chemical
					//is removed. This is also typical with enzyme names.
					if(chemicalAnnotation.getCoveredText().length() < geneAnnotation.getCoveredText().length())
					{
						annotationsToRemove.addAll(overlappingChemicals);
					}
					else
					{
						//If it is a confirmed acronym
						if(chemicalAnnotation.getIsAcronym())
						{
							annotationsToRemove.add(geneAnnotation);
						}
						else if(geneAnnotation.getIsAcronym())
						{
							annotationsToRemove.add(chemicalAnnotation);
						}
						else
						{
							if(Double.valueOf(chemicalAnnotation.getConfidence()) < 0.9)
							{
								annotationsToRemove.add(chemicalAnnotation);
							}
							else
							{
								annotationsToRemove.add(geneAnnotation);
							}
						}
					}
				}
			}
		}
		
		for(Annotation a : cas.getAnnotationIndex(LinnaeusSpecies.type))
		{
			for(Chemical c : chemicalCharacterIndex.getOverlappingAnnotations(a))
			{
				annotationsToRemove.add(c);
			}
			
			for(Gene g : geneCharacterIndex.getOverlappingAnnotations(a))
			{
				annotationsToRemove.add(g);
			}
		}
		
		for(Annotation a : annotationsToRemove)
		{
			a.removeFromIndexes();
		}
	}

}
