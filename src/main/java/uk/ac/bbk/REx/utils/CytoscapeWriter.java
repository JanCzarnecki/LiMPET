package uk.ac.bbk.REx.utils;

import uk.ac.ebi.mdk.domain.annotation.InChI;
import uk.ac.ebi.mdk.domain.annotation.rex.RExCompound;
import uk.ac.ebi.mdk.domain.entity.Metabolite;
import uk.ac.ebi.mdk.domain.entity.reaction.MetabolicParticipant;
import uk.ac.ebi.mdk.domain.entity.reaction.MetabolicReaction;

import java.io.*;
import java.util.*;

public class CytoscapeWriter
{
    public static void writeToTSV(Collection<MetabolicReaction> reactions, Collection<String> currencyMols, File outputDir) throws IOException
    {
        if(!outputDir.exists())
        {
            outputDir.mkdir();
        }

        File attributesFile = new File(outputDir, "attributes.tsv");
        Writer attributesWriter = new BufferedWriter(new FileWriter(attributesFile));
        String molAttributesFormat = "%s\t%s\t%s\t%s" + System.getProperty("line.separator");
        String reactionAttributesFormat = "%s\t%s" + System.getProperty("line.separator");
        attributesWriter.write("id\ttype\ttitle\tinchi" + System.getProperty("line.separator"));

        File pathwayFile = new File(outputDir, "pathway.tsv");
        Writer pathwayWriter = new BufferedWriter(new FileWriter(pathwayFile));
        String pathwayFormat = "%s\t%s\t%s\t%f\t%f" + System.getProperty("line.separator");
        pathwayWriter.write("metabolite\treaction\ttype\textraction\trelevance"  + System.getProperty("line.separator"));

        Set<Metabolite> metabolites = new HashSet<Metabolite>();
        Map<String, Set<String>> newCurrencyMols = new HashMap<String, Set<String>>();

        for(MetabolicReaction reaction : reactions)
        {
            String reactionLine = String.format(reactionAttributesFormat,
                    reaction.getIdentifier().toString(), "r");
            attributesWriter.write(reactionLine);

            Map<String, RExCompound> substrateCompounds = new HashMap<String, RExCompound>();
            Map<String, RExCompound> productCompounds = new HashMap<String, RExCompound>();
            for(RExCompound c : reaction.getAnnotations(RExCompound.class))
            {
                if(c.getType() == RExCompound.Type.SUBSTRATE)
                {
                    substrateCompounds.put(c.getID(), c);
                }
                else if(c.getType() == RExCompound.Type.PRODUCT)
                {
                    productCompounds.put(c.getID(), c);
                }
            }

            for(MetabolicParticipant substrate : reaction.getReactants())
            {
                Metabolite m = substrate.getMolecule();

                String inchiString = "";
                for(InChI inchi : m.getAnnotations(InChI.class))
                {
                    inchiString = inchi.toInChI();
                }

                String id = m.getIdentifier().toString();
                if(id.contains("/"))
                {
                    id = "m_" + id.replaceAll("/", "") + "_c";
                }

                RExCompound compound = substrateCompounds.get(id);

                if(currencyMols.contains(inchiString))
                {
                    if(!newCurrencyMols.containsKey(id))
                    {
                        newCurrencyMols.put(id, new HashSet<String>());
                    }

                    String newID = id + System.currentTimeMillis();
                    newCurrencyMols.get(id).add(newID);
                    id = newID;
                }

                metabolites.add(m);
                String pathwayLine = String.format(pathwayFormat,
                        id, reaction.getIdentifier().toString(), "s",
                        compound.getExtraction(), compound.getRelevance());
                pathwayWriter.write(pathwayLine);
            }

            for(MetabolicParticipant product : reaction.getProducts())
            {
                Metabolite m = product.getMolecule();

                String inchiString = "";
                for(InChI inchi : m.getAnnotations(InChI.class))
                {
                    inchiString = inchi.toInChI();
                }

                String id = m.getIdentifier().toString();
                if(id.contains("/"))
                {
                    id = "m_" + id.replaceAll("/", "") + "_c";
                }

                RExCompound compound = productCompounds.get(id);

                if(currencyMols.contains(inchiString))
                {
                    if(!newCurrencyMols.containsKey(id))
                    {
                        newCurrencyMols.put(id, new HashSet<String>());
                    }

                    String newID = id + System.currentTimeMillis();
                    newCurrencyMols.get(id).add(newID);
                    id = newID;
                }

                metabolites.add(m);
                String pathwayLine = String.format(pathwayFormat,
                        id, reaction.getIdentifier().toString(), "p",
                        compound.getExtraction(), compound.getRelevance());
                pathwayWriter.write(pathwayLine);
            }
        }

        for(Metabolite m : metabolites)
        {
            String id = m.getIdentifier().toString();
            String inchiString = "";
            for(InChI inchi : m.getAnnotations(InChI.class))
            {
                inchiString = inchi.toInChI();
            }

            if(currencyMols.contains(inchiString))
            {
                for(String newID : newCurrencyMols.get(id))
                {
                    String molLine = String.format(molAttributesFormat,
                            newID, "m", m.getName(), inchiString);
                    attributesWriter.write(molLine);
                }
            }
            else
            {
                String molLine = String.format(molAttributesFormat,
                        id, "m", m.getName(), inchiString);
                attributesWriter.write(molLine);
            }
        }

        attributesWriter.close();
        pathwayWriter.close();
    }
}
