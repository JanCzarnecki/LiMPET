package uk.ac.bbk.REx.utils;

import uk.ac.bbk.REx.db.bkmDB.BKMDB;
import uk.ac.ebi.mdk.domain.annotation.InChI;
import uk.ac.ebi.mdk.domain.annotation.rex.RExCompound;
import uk.ac.ebi.mdk.domain.annotation.rex.RExExtract;
import uk.ac.ebi.mdk.domain.annotation.rex.RExTag;
import uk.ac.ebi.mdk.domain.entity.Metabolite;
import uk.ac.ebi.mdk.domain.entity.reaction.MetabolicParticipant;
import uk.ac.ebi.mdk.domain.entity.reaction.MetabolicReaction;

import java.sql.SQLException;
import java.util.*;

public class CompoundAnnotator
{
    /**
     * Add compound annotations to a collection of MDK MetabolicReactions.
     *
     * @param reactions The reactions to which annotations will be added.
     * @param seedReactions The seed reactions used to extract the reactions.
     * @param bkmDB A BKM-React database object.
     * @param currencyMols A collection of currency molecules to ignore.
     * @throws SQLException If there is a problem reading the BKMDB.
     */
    public static void annotateReactions(
            Collection<MetabolicReaction> reactions,
            Collection<MetabolicReaction> seedReactions,
            BKMDB bkmDB,
            Set<String> currencyMols) throws SQLException
    {
        Set<String> seedInChIs = new HashSet<String>();
        Map<String, Set<MetabolicReaction>> seedSubstrateIndex = new HashMap<String, Set<MetabolicReaction>>();
        Map<String, Set<MetabolicReaction>> seedProductIndex = new HashMap<String, Set<MetabolicReaction>>();

        for(MetabolicReaction seedReaction : seedReactions)
        {
            for(MetabolicParticipant substrate : seedReaction.getReactants())
            {
                Collection<InChI> inchis = substrate.getMolecule().getAnnotations(InChI.class);
                if(!inchis.isEmpty())
                {
                    String id = "";
                    for(InChI inchi : inchis)
                    {
                        id = inchi.toInChI();
                    }

                    if(!seedSubstrateIndex.containsKey(id))
                    {
                        seedSubstrateIndex.put(id, new HashSet<MetabolicReaction>());
                    }

                    seedSubstrateIndex.get(id).add(seedReaction);
                    seedInChIs.add(id);
                }
            }

            for(MetabolicParticipant product : seedReaction.getProducts())
            {
                Collection<InChI> inchis = product.getMolecule().getAnnotations(InChI.class);
                if(!inchis.isEmpty())
                {
                    String id = "";
                    for(InChI inchi : inchis)
                    {
                        id = inchi.toInChI();
                    }

                    if(!seedProductIndex.containsKey(id))
                    {
                        seedProductIndex.put(id, new HashSet<MetabolicReaction>());
                    }

                    seedProductIndex.get(id).add(seedReaction);
                    seedInChIs.add(id);
                }
            }
        }

        Set<Integer> seedReactionsFound = new HashSet<Integer>();
        for(MetabolicReaction seedReaction : seedReactions)
        {
            //Determine if this is a terminal reaction
            List<MetabolicParticipant> substrates = seedReaction.getReactants();
            List<MetabolicParticipant> products = seedReaction.getProducts();

            int substratesNotInOtherReactions = 0;
            for(MetabolicParticipant substrate : seedReaction.getReactants())
            {
                Metabolite m = substrate.getMolecule();
                for(InChI inchi : m.getAnnotations(InChI.class))
                {
                    if(!seedProductIndex.containsKey(inchi.toInChI()))
                    {
                        substratesNotInOtherReactions++;
                        break;
                    }
                }
            }

            int productsNotInOtherReactions = 0;
            for(MetabolicParticipant product : seedReaction.getProducts())
            {
                Metabolite m = product.getMolecule();
                for(InChI inchi : m.getAnnotations(InChI.class))
                {
                    if(!seedSubstrateIndex.containsKey(inchi.toInChI()))
                    {
                        productsNotInOtherReactions++;
                        break;
                    }
                }
            }

            boolean startTerminus = false;
            if(substrates.size()-substratesNotInOtherReactions == 0)
            {
                startTerminus = true;
            }

            boolean endTerminus = false;
            if(products.size()-productsNotInOtherReactions == 0)
            {
                endTerminus = true;
            }

            for(MetabolicParticipant substrate : seedReaction.getReactants())
            {
                Metabolite substrateCompound = substrate.getMolecule();
                Collection<InChI> substrateInchis = substrateCompound.getAnnotations(InChI.class);
                String substrateInchiString = "";
                for(InChI inchi : substrateInchis)
                {
                    substrateInchiString = inchi.toInChI();
                }

                if(startTerminus || seedProductIndex.containsKey(substrateInchiString))
                {
                    for(MetabolicParticipant product : seedReaction.getProducts())
                    {
                        Metabolite productCompound = product.getMolecule();
                        Collection<InChI> productInchis = productCompound.getAnnotations(InChI.class);
                        String productInchiString = "";
                        for(InChI inchi : productInchis)
                        {
                            productInchiString = inchi.toInChI();
                        }

                        if(endTerminus || seedSubstrateIndex.containsKey(productInchiString))
                        {
                            seedReactionsFound.addAll(
                                    bkmDB.getReactionsContainingSubstrateAndProduct(
                                            substrateInchiString, productInchiString));
                        }
                    }
                }
            }
        }

        Set<String> seedPathwaysFound = new HashSet<String>();
        for(Integer seedReactionFound : seedReactionsFound)
        {
            seedPathwaysFound.addAll(bkmDB.getPathwaysContainingReaction(seedReactionFound));
        }

        //Branches
        int maxLength = 4;
        Map<String, Set<MetabolicReaction>> substrateIndex = new HashMap<String, Set<MetabolicReaction>>();
        Map<String, Set<MetabolicReaction>> productIndex = new HashMap<String, Set<MetabolicReaction>>();
        for(MetabolicReaction reaction : reactions)
        {
            for(MetabolicParticipant substrate : reaction.getReactants())
            {
                Metabolite m = substrate.getMolecule();
                for(InChI inchi : m.getAnnotations(InChI.class))
                {
                    if(currencyMols.contains(inchi.toInChI()))
                    {
                        continue;
                    }

                    if(!substrateIndex.containsKey(inchi.toInChI()))
                    {
                        substrateIndex.put(inchi.toInChI(), new HashSet<MetabolicReaction>());
                    }

                    substrateIndex.get(inchi.toInChI()).add(reaction);
                }
            }

            for(MetabolicParticipant product : reaction.getProducts())
            {
                Metabolite m = product.getMolecule();
                for(InChI inchi : m.getAnnotations(InChI.class))
                {
                    if(currencyMols.contains(inchi.toInChI()))
                    {
                        continue;
                    }

                    if(!productIndex.containsKey(inchi.toInChI()))
                    {
                        productIndex.put(inchi.toInChI(), new HashSet<MetabolicReaction>());
                    }

                    productIndex.get(inchi.toInChI()).add(reaction);
                }
            }
        }

        Set<List<MetabolicReaction>> branches = new HashSet<List<MetabolicReaction>>();
        Set<List<MetabolicReaction>> alternativeBranches = new HashSet<List<MetabolicReaction>>();

        for(String inchi : seedInChIs)
        {
            if(substrateIndex.containsKey(inchi))
            {
                for(MetabolicReaction reaction : substrateIndex.get(inchi))
                {
                    List<MetabolicReaction> branch = new ArrayList<MetabolicReaction>();
                    branch.add(reaction);
                    branches.add(branch);
                }
            }
        }

        for(int i=0; i<maxLength; i++)
        {
            //Find any branches that have returned to the seed pathway
            for(List<MetabolicReaction> branch : branches)
            {
                MetabolicReaction lastReaction = branch.get(branch.size()-1);

                for(MetabolicParticipant product : lastReaction.getProducts())
                {
                    Metabolite m = product.getMolecule();
                    String inchiString = "";
                    for(InChI inchi : m.getAnnotations(InChI.class))
                    {
                        inchiString = inchi.toInChI();
                    }

                    if(seedInChIs.contains(inchiString))
                    {
                        alternativeBranches.add(branch);
                    }
                }
            }

            //Prevent the branches found in the last step from extending any further
            for(List<MetabolicReaction> alternativeBranch : alternativeBranches)
            {
                branches.remove(alternativeBranch);
            }

            //Extend all remaining branches
            Set<List<MetabolicReaction>> extendedBranches = new HashSet<List<MetabolicReaction>>();
            Set<List<MetabolicReaction>> branchesToAdd = new HashSet<List<MetabolicReaction>>();
            for(List<MetabolicReaction> branch : branches)
            {
                MetabolicReaction lastReaction = branch.get(branch.size()-1);

                for(MetabolicParticipant product : lastReaction.getProducts())
                {
                    Metabolite m = product.getMolecule();
                    String inchiString = "";
                    for(InChI inchi : m.getAnnotations(InChI.class))
                    {
                        inchiString = inchi.toInChI();
                    }

                    if(substrateIndex.containsKey(inchiString))
                    {
                        for(MetabolicReaction nextReaction : substrateIndex.get(inchiString))
                        {
                            extendedBranches.add(branch);
                            List<MetabolicReaction> newBranch = new ArrayList<MetabolicReaction>(branch);
                            newBranch.add(nextReaction);
                            branchesToAdd.add(newBranch);
                        }
                    }
                }
            }

            branches.addAll(branchesToAdd);
            branches.removeAll(extendedBranches);
        }

        Set<MetabolicReaction> finalBranches = new HashSet<MetabolicReaction>();

        for(List<MetabolicReaction> alternativeBranch : alternativeBranches)
        {
            //Discount branches that start and end with the same molecule i.e. a circle
            Set<String> startMols = new HashSet<String>();
            for(MetabolicParticipant substrate : alternativeBranch.get(0).getReactants())
            {
                Metabolite m = substrate.getMolecule();
                String inchiString = "";
                for(InChI inchi : m.getAnnotations(InChI.class))
                {
                    inchiString = inchi.toInChI();
                }

                if(seedInChIs.contains(inchiString))
                {
                    startMols.add(inchiString);
                }
            }

            Set<String> endMols = new HashSet<String>();
            for(MetabolicParticipant product : alternativeBranch.get(alternativeBranch.size()-1).getProducts())
            {
                Metabolite m = product.getMolecule();
                String inchiString = "";
                for(InChI inchi : m.getAnnotations(InChI.class))
                {
                    inchiString = inchi.toInChI();
                }

                if(seedInChIs.contains(inchiString))
                {
                    endMols.add(inchiString);
                }
            }

            Set<String> uniqueMols = new HashSet<String>(startMols);
            uniqueMols.removeAll(endMols);

            if(startMols.size() == uniqueMols.size())
            {
                finalBranches.addAll(alternativeBranch);
            }
        }

        for(MetabolicReaction reaction : reactions)
        {
            Map<String, Integer> substrateOccurrences = new HashMap<String, Integer>();
            Map<String, Integer> productOccurrences = new HashMap<String, Integer>();
            for(RExExtract extract : reaction.getAnnotations(RExExtract.class))
            {
                if(extract.isInCorrectOrganism())
                {
                    for(RExTag tag : extract.tags())
                    {
                        String id = tag.id();
                        if(tag.type() == RExTag.Type.SUBSTRATE)
                        {
                            if(!substrateOccurrences.containsKey(id))
                            {
                                substrateOccurrences.put(id, 0);
                            }

                            substrateOccurrences.put(id, substrateOccurrences.get(id) + 1);
                        }

                        if(tag.type() == RExTag.Type.PRODUCT)
                        {
                            if(!productOccurrences.containsKey(id))
                            {
                                productOccurrences.put(id, 0);
                            }

                            productOccurrences.put(id, productOccurrences.get(id) + 1);
                        }
                    }
                }
            }

            Map<String, Boolean> substrateIsInBRENDA = new HashMap<String, Boolean>();
            Map<String, Boolean> productIsInBRENDA = new HashMap<String, Boolean>();

            Map<String, Boolean> substrateIsInSeed = new HashMap<String, Boolean>();
            Map<String, Boolean> productIsInSeed = new HashMap<String, Boolean>();

            Set<String> substrateAlternativePathways = new HashSet<String>();
            Set<String> productAlternativePathways = new HashSet<String>();

            Set<String> substrateOtherPathways = new HashSet<String>();
            Set<String> productOtherPathways = new HashSet<String>();

            for(MetabolicParticipant substrate : reaction.getReactants())
            {
                Metabolite substrateCompound = substrate.getMolecule();
                Collection<InChI> substrateInchis = substrateCompound.getAnnotations(InChI.class);
                String substrateID = "";
                for(InChI inchi : substrateInchis)
                {
                    substrateID = inchi.toInChI();
                }

                for(MetabolicParticipant product : reaction.getProducts())
                {
                    Metabolite productCompound = product.getMolecule();
                    Collection<InChI> productInchis = productCompound.getAnnotations(InChI.class);
                    String productID = "";
                    for(InChI inchi : productInchis)
                    {
                        productID = inchi.toInChI();
                    }

                    //BRENDA
                    if(!bkmDB.getReactionsContainingSubstrateAndProduct(substrateID, productID).isEmpty())
                    {
                        substrateIsInBRENDA.put(substrateID, true);
                        productIsInBRENDA.put(productID, true);
                    }

                    //Seed
                    Set<MetabolicReaction> commonReactions = new HashSet<MetabolicReaction>();

                    if(seedSubstrateIndex.containsKey(substrateID)
                            && seedProductIndex.containsKey(productID))
                    {
                        Set<MetabolicReaction> substrateReactions = seedSubstrateIndex.get(substrateID);
                        Set<MetabolicReaction> productReactions = seedProductIndex.get(productID);

                        commonReactions = new HashSet<MetabolicReaction>(substrateReactions);
                        commonReactions.retainAll(productReactions);
                    }

                    if(!commonReactions.isEmpty())
                    {
                        substrateIsInSeed.put(substrateID, true);
                        productIsInSeed.put(productID, true);
                    }

                    //Alternative & Other
                    List<Integer> reactionsFound = bkmDB.getReactionsContainingSubstrateAndProduct(substrateID, productID);
                    Set<String> pathwaysFound = new HashSet<String>();
                    for(int reactionFound : reactionsFound)
                    {
                        pathwaysFound.addAll(bkmDB.getPathwaysContainingReaction(reactionFound));
                    }

                    for(String pathwayFound : pathwaysFound)
                    {
                        boolean alternative = false;
                        String pathwayFoundName = bkmDB.getPathwayName(pathwayFound);
                        for(String seedPathwayFound : seedPathwaysFound)
                        {
                            String seedPathwayFoundName = bkmDB.getPathwayName(seedPathwayFound);
                            if(bkmDB.arePathwayNamesAlternatives(pathwayFoundName, seedPathwayFoundName))
                            {
                                alternative = true;
                                break;
                            }
                        }

                        if(alternative)
                        {
                            substrateAlternativePathways.add(pathwayFound);
                            productAlternativePathways.add(pathwayFound);
                        }
                        else
                        {
                            substrateOtherPathways.add(pathwayFound);
                            productOtherPathways.add(pathwayFound);
                        }
                    }
                }
            }

            for(MetabolicParticipant substrate : reaction.getReactants())
            {
                String inchiString = "";
                for(InChI inchi : substrate.getMolecule().getAnnotations(InChI.class))
                {
                    inchiString = inchi.toInChI();
                }

                boolean substrateIsInBRENDABool = false;
                if(substrateIsInBRENDA.containsKey(inchiString))
                {
                    substrateIsInBRENDABool = true;
                }

                boolean substrateIsInSeedBool = false;
                if(substrateIsInSeed.containsKey(inchiString))
                {
                    substrateIsInSeedBool = true;
                }

                boolean isInBranch = finalBranches.contains(reaction);

                double extraction = 0;

                String id = substrate.getMolecule().getIdentifier().toString();
                if(id.contains("/"))
                {
                    id = substrate.getMolecule().getIdentifier().toString().replaceAll("/", "") + "_c";
                }

                if(substrateOccurrences.containsKey(id))
                {
                    int occurrences = substrateOccurrences.get(id);
                    extraction += occurrences;

                    if(substrateIsInBRENDABool)
                    {
                        extraction += 5.0;
                    }
                }

                RExCompound compound = new RExCompound(id,
                        RExCompound.Type.SUBSTRATE,
                        substrateIsInBRENDABool,
                        substrateIsInSeedBool,
                        isInBranch,
                        new ArrayList<String>(substrateAlternativePathways),
                        new ArrayList<String>(substrateOtherPathways),
                        extraction,
                        0);
                reaction.addAnnotation(compound);
            }

            for(MetabolicParticipant product : reaction.getProducts())
            {
                String inchiString = "";
                for(InChI inchi : product.getMolecule().getAnnotations(InChI.class))
                {
                    inchiString = inchi.toInChI();
                }

                boolean productIsInBRENDABool = false;
                if(productIsInBRENDA.containsKey(inchiString))
                {
                    productIsInBRENDABool = true;
                }

                boolean productIsInSeedBool = false;
                if(productIsInSeed.containsKey(inchiString))
                {
                    productIsInSeedBool = true;
                }

                boolean isInBranch = finalBranches.contains(reaction);

                double extraction = 0;

                String id = product.getMolecule().getIdentifier().toString();
                if(id.contains("/"))
                {
                    id = product.getMolecule().getIdentifier().toString().replaceAll("/", "") + "_c";
                }

                if(productOccurrences.containsKey(id))
                {
                    int occurrences = productOccurrences.get(id);
                    extraction += occurrences;

                    if(productIsInBRENDABool)
                    {
                        extraction += 5.0;
                    }
                }

                RExCompound compound = new RExCompound(id,
                        RExCompound.Type.PRODUCT,
                        productIsInBRENDABool,
                        productIsInSeedBool,
                        isInBranch,
                        new ArrayList<String>(productAlternativePathways),
                        new ArrayList<String>(productOtherPathways),
                        extraction,
                        0);
                reaction.addAnnotation(compound);
            }
        }
    }

    /**
     * Calculate the relevance of a collection of MetabolicReactions and update their compound annotations.
     *
     * @param reactions The reactions to calculate relevance for.
     */
    public static void calculateAlternativePathwayRelevance(Collection<MetabolicReaction> reactions)
    {
        for(MetabolicReaction reaction : reactions)
        {
            for(RExCompound compound : reaction.getAnnotations(RExCompound.class))
            {
                if(compound.isInBranch())
                {
                    compound.setRelevance(0.25);
                }

                if(!compound.getAlternativePathways().isEmpty())
                {
                    compound.setRelevance(0.5);
                }

                if(compound.isInBranch()
                        && !compound.getAlternativePathways().isEmpty())
                {
                    compound.setRelevance(0.75);
                }

                if(compound.isInSeed())
                {
                    compound.setRelevance(1.0);
                }
            }
        }
    }
}
