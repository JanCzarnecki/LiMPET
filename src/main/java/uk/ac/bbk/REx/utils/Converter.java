package uk.ac.bbk.REx.utils;

import java.io.InputStream;
import java.util.*;
import javax.xml.stream.XMLStreamException;

import hu.u_szeged.rgai.bio.uima.tagger.LinnaeusSpecies;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

import uk.ac.bbk.REx.types.Document;
import uk.ac.bbk.REx.types.Section;
import uk.ac.ebi.mdk.domain.annotation.InChI;
import uk.ac.ebi.mdk.domain.annotation.rex.RExExtract;
import uk.ac.ebi.mdk.domain.annotation.rex.RExTag;
import uk.ac.ebi.mdk.domain.entity.DefaultEntityFactory;
import uk.ac.ebi.mdk.domain.entity.EntityFactory;
import uk.ac.ebi.mdk.domain.entity.Metabolite;
import uk.ac.ebi.mdk.domain.entity.reaction.BiochemicalReaction;
import uk.ac.ebi.mdk.domain.entity.reaction.MetabolicReaction;
import uk.ac.ebi.mdk.domain.identifier.PubMedIdentifier;
import uk.ac.ebi.mdk.domain.identifier.basic.BasicChemicalIdentifier;
import uk.ac.ebi.mdk.domain.tool.AutomaticCompartmentResolver;
import uk.ac.ebi.mdk.io.xml.sbml.SBMLReactionReader;

public class Converter 
{
    /**
     * Converts reaction annotations contained in a collection of CAS's to MDK reactions.
     *
     * @param cases A collection of CAS's containing reaction annotations.
     * @param sectionsToIgnore
     * @param speciesID The NCBI Taxonomy ID used in the extraction of reactions.
     * @return
     */
    public static List<BiochemicalReaction> convertUIMAReactionsToMDK(
            Collection<JCas> cases, Collection<String> sectionsToIgnore, String speciesID)
    {
        EntityFactory entityFactory = DefaultEntityFactory.getInstance();
        List<BiochemicalReaction> output = new ArrayList<BiochemicalReaction>();
        Map<String, Metabolite> metabolites = new HashMap<String, Metabolite>();

        for(JCas cas : cases)
        {
            String pmid = null;
            for(Annotation docAnnotation : cas.getAnnotationIndex(Document.type))
            {
                Document doc = (Document)docAnnotation;
                pmid = doc.getId();
            }

            CharacterIndex<Section> sectionIndex = new CharacterIndex<Section>(cas, Section.type);

            for(Annotation reactionAnnotation : cas.getAnnotationIndex(uk.ac.bbk.REx.types.Reaction.type))
            {
                uk.ac.bbk.REx.types.Reaction uimaReaction = (uk.ac.bbk.REx.types.Reaction)reactionAnnotation;

                boolean inCorrectSection = true;
                for(Section s : sectionIndex.getOverlappingAnnotations(reactionAnnotation))
                {
                    if(sectionsToIgnore.contains(s.getName()))
                    {
                        inCorrectSection = false;
                    }
                }

                boolean correctSpecies = false;
                for(LinnaeusSpecies species : JCasUtils.<LinnaeusSpecies>convertFSListToCollection(uimaReaction.getOrganisms()))
                {
                    String id = species.getMostProbableSpeciesId().replace("species:ncbi:", "");
                    if(speciesID.equals(id))
                    {
                        correctSpecies = true;
                    }
                }

                Set<Metabolite> substrates = new HashSet<Metabolite>();
                Set<Metabolite> products = new HashSet<Metabolite>();
                List<RExTag> metaboliteTags = new ArrayList<RExTag>();

                createMDKMetabolites(
                        uimaReaction,
                        JCasUtils.convertFSListToCollection(uimaReaction.getSubstrates()),
                        entityFactory,
                        metabolites,
                        substrates,
                        RExTag.Type.SUBSTRATE,
                        metaboliteTags);

                createMDKMetabolites(
                        uimaReaction,
                        JCasUtils.convertFSListToCollection(uimaReaction.getProducts()),
                        entityFactory,
                        metabolites,
                        products,
                        RExTag.Type.PRODUCT,
                        metaboliteTags);

                BiochemicalReaction mdkReaction = entityFactory.biochemicalReaction(entityFactory.reaction());

                for(Metabolite substrate : substrates)
                {
                    mdkReaction.addReactant(substrate);
                }

                for(Metabolite product : products)
                {
                    mdkReaction.addProduct(product);
                }

                RExExtract extract = new RExExtract(
                        new PubMedIdentifier(pmid), uimaReaction.getCoveredText(), metaboliteTags, correctSpecies);
                mdkReaction.addAnnotation(extract);
                output.add(mdkReaction);
            }
        }

        return output;
    }

    private static void createMDKMetabolites(
            uk.ac.bbk.REx.types.Reaction reaction,
            Collection<Annotation> annotations,
            EntityFactory entityFactory,
            Map<String, Metabolite> metabolites,
            Collection<Metabolite> metabolitesOutput,
            RExTag.Type type,
            Collection<RExTag> tagsOutput)
    {
        for(Annotation chemicalAnnotation : annotations)
        {
            uk.ac.bbk.REx.types.Chemical uimaChemical = (uk.ac.bbk.REx.types.Chemical)chemicalAnnotation;
            uk.ac.bbk.REx.types.Chemical refferedUIMAChemical = (uk.ac.bbk.REx.types.Chemical)uimaChemical.getRefersTo();
            if(refferedUIMAChemical == null)
            {
                refferedUIMAChemical = uimaChemical;
            }

            if(!metabolites.containsKey(refferedUIMAChemical.getInChiString()))
            {
                Metabolite metabolite = entityFactory.metabolite();
                metabolite.setIdentifier(BasicChemicalIdentifier.nextIdentifier());
                metabolite.setName(refferedUIMAChemical.getCoveredText());

                if(refferedUIMAChemical.getInChiString() != null)
                {
                    uk.ac.ebi.mdk.domain.annotation.Annotation inchi = new InChI(refferedUIMAChemical.getInChiString());
                    metabolite.addAnnotation(inchi);

                    metabolites.put(refferedUIMAChemical.getInChiString(), metabolite);
                }
                else
                {
                    metabolites.put("noinchi:" + refferedUIMAChemical.getCoveredText(), metabolite);
                }
            }

            Metabolite metabolite;
            if(refferedUIMAChemical.getInChiString() != null)
            {
                metabolite = metabolites.get(refferedUIMAChemical.getInChiString());
            }
            else
            {
                metabolite = metabolites.get("noinchi:" + refferedUIMAChemical.getCoveredText());
            }

            metabolitesOutput.add(metabolite);

            int start = uimaChemical.getBegin() - reaction.getBegin();
            int length = uimaChemical.getEnd() - uimaChemical.getBegin();
            RExTag tag = new RExTag(
                    metabolite.getIdentifier().toString().replaceAll("/", "") + "_c", start, length, type);

            tagsOutput.add(tag);
        }
    }

    public static List<MetabolicReaction> convertSBMLToMDK(InputStream sbmlIS) throws XMLStreamException
    {
        EntityFactory entityFactory = DefaultEntityFactory.getInstance();
        SBMLReactionReader sbmlReader = new SBMLReactionReader(
                sbmlIS, entityFactory, new AutomaticCompartmentResolver());
        List<MetabolicReaction> reactions = new ArrayList<MetabolicReaction>();

        while(sbmlReader.hasNext())
        {
            MetabolicReaction r = sbmlReader.next();
            reactions.add(r);
        }

        return reactions;
    }
}
