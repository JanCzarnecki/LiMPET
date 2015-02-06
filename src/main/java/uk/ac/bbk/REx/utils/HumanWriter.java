package uk.ac.bbk.REx.utils;

import org.apache.commons.io.FilenameUtils;
import uk.ac.bbk.REx.types.Annotation;
import uk.ac.ebi.mdk.domain.annotation.rex.RExCompound;
import uk.ac.ebi.mdk.domain.annotation.rex.RExExtract;
import uk.ac.ebi.mdk.domain.entity.Metabolite;
import uk.ac.ebi.mdk.domain.entity.reaction.MetabolicParticipant;
import uk.ac.ebi.mdk.domain.entity.reaction.MetabolicReaction;

import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class HumanWriter
{
    public static void writeToTSV(Collection<MetabolicReaction> reactions, double extractionThreshold, Writer output) throws IOException {
        output.write("substrates\tproducts\trelevance\tevidence" + System.getProperty("line.separator"));

        Map<String, Metabolite> metaboliteIndex = new HashMap<String, Metabolite>();
        for(MetabolicReaction reaction : reactions)
        {
            for(MetabolicParticipant mp : reaction.getParticipants())
            {
                Metabolite m = mp.getMolecule();
                metaboliteIndex.put(m.getIdentifier().toString(), m);
            }
        }

        for(MetabolicReaction reaction : reactions)
        {
            StringBuilder substrates = new StringBuilder();
            StringBuilder products = new StringBuilder();
            int substrateCount = 0;
            int productCount = 0;
            int substratesAboveThreshold = 0;
            int productsAboveThreshold = 0;
            double highestRelevence = 0;
            for(RExCompound compound : reaction.getAnnotations(RExCompound.class))
            {
                Metabolite m = metaboliteIndex.get(compound.getID());
                String name = m.getName();

                if(compound.getExtraction() >= extractionThreshold)
                {
                    if(compound.getType() == RExCompound.Type.SUBSTRATE)
                    {
                        if(substrateCount > 0)
                        {
                            substrates.append(" + " + name);
                        }
                        else
                        {
                            substrates.append(name);
                        }
                        substratesAboveThreshold++;
                        substrateCount++;
                    }
                    else if(compound.getType() == RExCompound.Type.PRODUCT)
                    {
                        if(productCount > 0)
                        {
                            products.append(" + " + name);
                        }
                        else
                        {
                            products.append(name);
                        }
                        productsAboveThreshold++;
                        productCount++;
                    }
                }

                if(compound.getRelevance() > highestRelevence)
                {
                    highestRelevence = compound.getRelevance();
                }
            }

            if(substratesAboveThreshold == 0 || productsAboveThreshold == 0)
            {
                continue;
            }

            StringBuilder evidence = new StringBuilder();
            for(RExExtract extract : reaction.getAnnotations(RExExtract.class))
            {
                if(extract.isInCorrectOrganism())
                {
                    String source;
                    if(extract.pubmed() != null)
                    {
                        source = FilenameUtils.getBaseName(extract.pubmed().toString());
                        evidence.append("PubMed: ");
                    }
                    else if(extract.file() != null)
                    {
                        source = extract.file().toString();
                        evidence.append("File: ");
                    }
                    else
                    {
                        source = "";
                    }
                    String sentence = extract.sentence().replaceAll("\"", "\\\"");
                    evidence.append(source);
                    evidence.append(System.getProperty("line.separator"));
                    evidence.append(sentence);
                    evidence.append(System.getProperty("line.separator"));
                    evidence.append(System.getProperty("line.separator"));
                }
            }

            String format = "%s\t%s\t%f\t\"%s\"" + System.getProperty("line.separator");
            String outputString = String.format(format, substrates, products, highestRelevence, evidence);
            output.write(outputString);
        }
    }
}
