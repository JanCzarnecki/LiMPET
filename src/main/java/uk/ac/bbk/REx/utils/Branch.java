package uk.ac.bbk.REx.utils;

import uk.ac.ebi.mdk.domain.annotation.InChI;
import uk.ac.ebi.mdk.domain.annotation.rex.RExCompound;
import uk.ac.ebi.mdk.domain.entity.Metabolite;
import uk.ac.ebi.mdk.domain.entity.reaction.MetabolicParticipant;
import uk.ac.ebi.mdk.domain.entity.reaction.MetabolicReaction;

import java.util.*;

public class Branch
{
    private List<MetabolicReaction> reactions;
    private List<List<Metabolite>> metabolites;
    private Set<Metabolite> allMetabolites;
    private Map<MetabolicReaction, List<Metabolite>> substrateIndex;
    private Map<MetabolicReaction, List<Metabolite>> productIndex;
    private List<List<Double>> extractionScores;
    private double score;

    public Branch(MetabolicReaction reaction, Set<String> seedInchis)
    {
        reactions = new ArrayList<MetabolicReaction>();
        metabolites = new ArrayList<List<Metabolite>>();
        allMetabolites = new HashSet<Metabolite>();
        substrateIndex = new HashMap<MetabolicReaction, List<Metabolite>>();
        productIndex = new HashMap<MetabolicReaction, List<Metabolite>>();
        extractionScores = new ArrayList<List<Double>>();

        reactions.add(reaction);

        List<Metabolite> firstSubstrates = new ArrayList<Metabolite>();
        List<Double> firstExtractionScores = new ArrayList<Double>();
        for(MetabolicParticipant substrate : reaction.getReactants())
        {
            Metabolite m = substrate.getMolecule();
            for(InChI inchi : m.getAnnotations(InChI.class))
            {
                if(seedInchis.contains(inchi.toInChI()))
                {
                    firstSubstrates.add(m);
                }
            }

            String id = m.getIdentifier().toString();
            if(id.contains("/"))
            {
                id = "m_" + id.replaceAll("/", "") + "_c";
            }

            for(RExCompound compound : reaction.getAnnotations(RExCompound.class))
            {
                if(compound.getID().equals(id))
                {
                    firstExtractionScores.add(compound.getExtraction());
                }
            }
        }

        metabolites.add(firstSubstrates);
        allMetabolites.addAll(firstSubstrates);
        substrateIndex.put(reaction, firstSubstrates);
        extractionScores.add(firstExtractionScores);
        calculateScore();
    }

    public Branch(List<MetabolicReaction> reactions,
                  List<List<Metabolite>> metabolites,
                  Set<Metabolite> allMetabolites,
                  Map<MetabolicReaction, List<Metabolite>> substrateIndex,
                  Map<MetabolicReaction, List<Metabolite>> productIndex,
                  List<List<Double>> extractionScores)
    {
        this.reactions = new ArrayList<MetabolicReaction>(reactions);
        this.metabolites = new ArrayList<List<Metabolite>>(metabolites);
        this.allMetabolites = new HashSet<Metabolite>(allMetabolites);
        this.substrateIndex = new HashMap<MetabolicReaction, List<Metabolite>>(substrateIndex);
        this.productIndex = new HashMap<MetabolicReaction, List<Metabolite>>(productIndex);
        this.extractionScores = new ArrayList<List<Double>>(extractionScores);
        calculateScore();
    }

    public void addReaction(MetabolicReaction reaction, Collection<Metabolite> substrates)
    {
        List<Double> lastExtractionScores = new ArrayList<Double>();
        for(MetabolicParticipant product : getFinalReaction().getProducts())
        {
            Metabolite m = product.getMolecule();
            if(substrates.contains(m))
            {
                String id = m.getIdentifier().toString();
                if(id.contains("/"))
                {
                    id = "m_" + id.replaceAll("/", "") + "_c";
                }

                for(RExCompound compound : getFinalReaction().getAnnotations(RExCompound.class))
                {
                    if(compound.getID().equals(id))
                    {
                        lastExtractionScores.add(compound.getExtraction());
                    }
                }
            }
        }
        extractionScores.add(lastExtractionScores);

        List<Metabolite> substratesList = new ArrayList<Metabolite>(substrates);
        reactions.add(reaction);
        metabolites.add(substratesList);
        allMetabolites.addAll(substrates);
        substrateIndex.put(reaction, substratesList);
        productIndex.put(reactions.get(reactions.size()-2), substratesList);

        List<Double> nextExtractionScores = new ArrayList<Double>();
        for(MetabolicParticipant substrate : reaction.getReactants())
        {
            Metabolite m = substrate.getMolecule();
            if(substrates.contains(m))
            {
                String id = m.getIdentifier().toString();
                if(id.contains("/"))
                {
                    id = "m_" + id.replaceAll("/", "") + "_c";
                }

                for(RExCompound compound : reaction.getAnnotations(RExCompound.class))
                {
                    if(compound.getID().equals(id))
                    {
                        nextExtractionScores.add(compound.getExtraction());
                    }
                }
            }
        }
        extractionScores.add(nextExtractionScores);

        calculateScore();
    }

    public Branch extendBranch(MetabolicReaction reaction, Collection<Metabolite> substrates)
    {
        Branch extendedBranch = new Branch(reactions, metabolites, allMetabolites, substrateIndex, productIndex, extractionScores);
        extendedBranch.addReaction(reaction, substrates);
        return extendedBranch;
    }

    public boolean startsAndEndsWithSameSeedMolecule()
    {
        Set<Metabolite> firstMet = new HashSet<Metabolite>();
        for(Metabolite metabolite : metabolites.get(0))
        {
            firstMet.add(metabolite);
        }

        Set<Metabolite> lastMet = new HashSet<Metabolite>();
        for(Metabolite metabolite : metabolites.get(metabolites.size() - 1))
        {
            lastMet.add(metabolite);
        }

        Set<Metabolite> commonMolecules = new HashSet<Metabolite>(firstMet);
        commonMolecules.retainAll(lastMet);

        if(commonMolecules.size() > 0)
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    public void finalise(Set<String> seedInchis)
    {
        List<Metabolite> molecules = new ArrayList<Metabolite>();

        List<Double> nextExtractionScores = new ArrayList<Double>();
        for(MetabolicParticipant product : getFinalReaction().getProducts())
        {
            Metabolite m = product.getMolecule();
            for(InChI inchi : m.getAnnotations(InChI.class))
            {
                if(seedInchis.contains(inchi.toInChI()))
                {
                    if(!productIndex.containsKey(getFinalReaction()))
                    {
                        productIndex.put(getFinalReaction(), new ArrayList<Metabolite>());
                    }

                    productIndex.get(getFinalReaction()).add(m);
                    molecules.add(m);

                    String id = m.getIdentifier().toString();
                    if(id.contains("/"))
                    {
                        id = "m_" + id.replaceAll("/", "") + "_c";
                    }

                    for(RExCompound compound : getFinalReaction().getAnnotations(RExCompound.class))
                    {
                        if(compound.getID().equals(id))
                        {
                            nextExtractionScores.add(compound.getExtraction());
                        }
                    }
                }
            }
        }

        metabolites.add(molecules);
        allMetabolites.addAll(molecules);
        extractionScores.add(nextExtractionScores);
        calculateScore();
    }

    public List<MetabolicReaction> getReactions()
    {
        return reactions;
    }

    public List<Metabolite> getSubstrates(MetabolicReaction reaction)
    {
        return substrateIndex.get(reaction);
    }

    public List<Metabolite> getProducts(MetabolicReaction reaction)
    {
        return productIndex.get(reaction);
    }

    public MetabolicReaction getFinalReaction()
    {
        return reactions.get(reactions.size()-1);
    }

    public int length()
    {
        return reactions.size();
    }

    private void calculateScore()
    {
        score = 1;

        for(List<Double> theseExtractionScores : extractionScores)
        {
            double highest = 0;
            for(double extractionScore : theseExtractionScores)
            {
                if(extractionScore > highest)
                {
                    highest = extractionScore;
                }
            }

            score = score*highest;
        }
    }

    public double getScore()
    {
        return score;
    }

    public boolean containsReaction(MetabolicReaction reaction)
    {
        return substrateIndex.containsKey(reaction);
    }

    public boolean containsMetabolite(Collection<Metabolite> metabolites)
    {
        boolean containsMetabolite = false;
        for(Metabolite metabolite : metabolites)
        {
            if(allMetabolites.contains(metabolite))
            {
                containsMetabolite = true;
            }
        }

        return containsMetabolite;
    }

    public String toString()
    {
        StringBuilder output = new StringBuilder();

        for(MetabolicReaction reaction : reactions)
        {
            output.append(reaction.getIdentifier().toString() + ": ");

            for(MetabolicParticipant substrate : reaction.getReactants())
            {
                output.append(substrate.getMolecule().getName() + " + ");
            }

            output.append("---> ");

            for(MetabolicParticipant product : reaction.getProducts())
            {
                output.append(product.getMolecule().getName() + " + ");
            }

            output.append("\n");
        }

        return output.toString();
    }
}
