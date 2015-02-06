package uk.ac.bbk.REx.utils;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.stream.XMLStreamException;

import hu.u_szeged.rgai.bio.uima.tagger.LinnaeusSpecies;
import org.apache.commons.io.FilenameUtils;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.impl.Serialization;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

import org.apache.uima.resource.ResourceInitializationException;
import uk.ac.bbk.REx.types.Chemical;
import uk.ac.bbk.REx.types.Document;
import uk.ac.bbk.REx.types.Section;
import uk.ac.ebi.mdk.domain.DefaultIdentifierFactory;
import uk.ac.ebi.mdk.domain.annotation.InChI;
import uk.ac.ebi.mdk.domain.annotation.rex.RExExtract;
import uk.ac.ebi.mdk.domain.annotation.rex.RExTag;
import uk.ac.ebi.mdk.domain.entity.DefaultEntityFactory;
import uk.ac.ebi.mdk.domain.entity.EntityFactory;
import uk.ac.ebi.mdk.domain.entity.Metabolite;
import uk.ac.ebi.mdk.domain.entity.reaction.BiochemicalReaction;
import uk.ac.ebi.mdk.domain.entity.reaction.MetabolicReaction;
import uk.ac.ebi.mdk.domain.identifier.Identifier;
import uk.ac.ebi.mdk.domain.identifier.IdentifierFactory;
import uk.ac.ebi.mdk.domain.identifier.PubMedIdentifier;
import uk.ac.ebi.mdk.domain.identifier.Taxonomy;
import uk.ac.ebi.mdk.domain.identifier.basic.BasicChemicalIdentifier;
import uk.ac.ebi.mdk.domain.tool.AutomaticCompartmentResolver;
import uk.ac.ebi.mdk.io.xml.sbml.SBMLReactionReader;

public class Converter 
{
    public static List<BiochemicalReaction> convertUIMAReactionsToMDK(
            File[] casFiles, AnalysisEngine ae, Collection<String> sectionsToIgnore, Collection<String> currencyMols) throws IOException, ResourceInitializationException, CASException
    {
        IdentifierFactory identifiers = DefaultIdentifierFactory.getInstance();
        EntityFactory entityFactory = DefaultEntityFactory.getInstance();
        List<BiochemicalReaction> output = new ArrayList<BiochemicalReaction>();
        Map<String, Metabolite> metabolites = new HashMap<String, Metabolite>();
        Set<String> names = new HashSet<String>();

        Pattern pubmedPatt = Pattern.compile("^\\d+$");

        for(File casFile : casFiles)
        {
            InputStream in = new BufferedInputStream(new FileInputStream(casFile));
            CAS cas = ae.newCAS();
            Serialization.deserializeCAS(cas, in);
            in.close();
            JCas jcas = cas.getJCas();

            String id = null;
            for(Annotation docAnnotation : jcas.getAnnotationIndex(Document.type))
            {
                Document doc = (Document)docAnnotation;
                id = doc.getId();
            }

            //Find seed metabolites in document.
            //List<Metabolite> seedMetabolitesFound = JCasUtils.metabolitesInCAS(seedMetabolites, jcas);
            int uniqueMetabolites = JCasUtils.uniqueMetabolitesInCAS(jcas);

            Set<String> inchiSet = new HashSet<String>();
            List<InChI> inchis = new ArrayList<InChI>();
            for(Annotation chemicalA : jcas.getAnnotationIndex(Chemical.type))
            {
                Chemical c = (Chemical)chemicalA;
                if(c.getInChiString() != null)
                {
                    inchiSet.add(c.getInChiString());
                }
            }

            for(String inchiString : inchiSet)
            {
                inchis.add(new InChI(inchiString));
            }

            CharacterIndex<Section> sectionIndex = new CharacterIndex<Section>(jcas, Section.type);

            for(Annotation reactionAnnotation : jcas.getAnnotationIndex(uk.ac.bbk.REx.types.Reaction.type))
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

                List<Identifier> organisms = new ArrayList<Identifier>();
                for(Annotation speciesA : JCasUtils.convertFSListToCollection(uimaReaction.getOrganisms()))
                {
                    LinnaeusSpecies species = (LinnaeusSpecies)speciesA;
                    String organismID = species.getMostProbableSpeciesId().replaceAll("species:ncbi:", "");
                    organisms.add(identifiers.ofURL("http://identifiers.org/taxonomy/" + organismID));
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
                        names,
                        RExTag.Type.SUBSTRATE,
                        metaboliteTags,
                        currencyMols);

                createMDKMetabolites(
                        uimaReaction,
                        JCasUtils.convertFSListToCollection(uimaReaction.getProducts()),
                        entityFactory,
                        metabolites,
                        products,
                        names,
                        RExTag.Type.PRODUCT,
                        metaboliteTags,
                        currencyMols);

                BiochemicalReaction mdkReaction = entityFactory.biochemicalReaction(entityFactory.reaction());

                for(Metabolite substrate : substrates)
                {
                    mdkReaction.addReactant(substrate);
                }

                for(Metabolite product : products)
                {
                    mdkReaction.addProduct(product);
                }

                RExExtract extract;
                if(id != null)
                {
                    String base = FilenameUtils.getBaseName(id);
                    Matcher m = pubmedPatt.matcher(base);
                    if(m.matches())
                    {
                        extract = new RExExtract(new PubMedIdentifier(m.group()), null, uimaReaction.getCoveredText(),
                                metaboliteTags, organisms, inchis, false, 0, uniqueMetabolites);
                    }
                    else
                    {
                        extract = new RExExtract(null, id, uimaReaction.getCoveredText(),
                                metaboliteTags, organisms, inchis, false, 0, uniqueMetabolites);
                    }
                }
                else
                {
                    extract = new RExExtract(null, null, uimaReaction.getCoveredText(),
                            metaboliteTags, organisms, inchis, false, 0, uniqueMetabolites);
                }

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
            Set<String> names,
            RExTag.Type type,
            Collection<RExTag> tagsOutput,
            Collection<String> currencyMols)
    {
        for(Annotation chemicalAnnotation : annotations)
        {
            uk.ac.bbk.REx.types.Chemical uimaChemical = (uk.ac.bbk.REx.types.Chemical)chemicalAnnotation;

            uk.ac.bbk.REx.types.Annotation interAnnotation = (uk.ac.bbk.REx.types.Annotation)chemicalAnnotation;
            uk.ac.bbk.REx.types.Annotation parentAnnotation = JCasUtils.getParentAnnotation(interAnnotation);
            uk.ac.bbk.REx.types.Chemical parentUIMAChemical = (uk.ac.bbk.REx.types.Chemical)parentAnnotation;

            Metabolite metabolite = entityFactory.metabolite();
            if((!metabolites.containsKey(parentUIMAChemical.getInChiString())
                    && !metabolites.containsKey("noinchi:" + parentUIMAChemical.getCoveredText()))
            ||(!metabolites.containsKey(parentUIMAChemical.getInChiString())
                &&(parentUIMAChemical.getInChiString() != null)))
            {
                metabolite.setIdentifier(BasicChemicalIdentifier.nextIdentifier());
                String name = parentUIMAChemical.getCoveredText();
                if(names.contains(name))
                {
                    name = name + "2";
                }
                metabolite.setName(name);
                names.add(name);

                if(parentUIMAChemical.getInChiString() != null)
                {
                    uk.ac.ebi.mdk.domain.annotation.Annotation inchi = new InChI(parentUIMAChemical.getInChiString());
                    metabolite.addAnnotation(inchi);

                    metabolites.put(parentUIMAChemical.getInChiString(), metabolite);
                }
                else if(parentUIMAChemical.getInChiString() == null)
                {
                    metabolites.put("noinchi:" + parentUIMAChemical.getCoveredText(), metabolite);
                }
            }

            if(parentUIMAChemical.getInChiString() != null
                    && metabolites.containsKey(parentUIMAChemical.getInChiString()))
            {
                metabolite = metabolites.get(parentUIMAChemical.getInChiString());
            }
            else if(parentUIMAChemical.getInChiString() == null
                    && metabolites.containsKey("noinchi:" + parentUIMAChemical.getCoveredText()))
            {
                metabolite = metabolites.get("noinchi:" + parentUIMAChemical.getCoveredText());
            }

            metabolitesOutput.add(metabolite);

            int start = uimaChemical.getBegin() - reaction.getBegin();
            int length = uimaChemical.getEnd() - uimaChemical.getBegin();
            RExTag tag = new RExTag(
                    "m_" + metabolite.getIdentifier().toString().replaceAll("/", "") + "_c", start, length, type);

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
