package uk.ac.bbk.REx.utils;

import uk.ac.bbk.REx.db.bkmDB.BKMDB;
import uk.ac.bbk.REx.internalTypes.Pathway;
import uk.ac.ebi.mdk.domain.annotation.InChI;
import uk.ac.ebi.mdk.domain.annotation.rex.RExCompound;
import uk.ac.ebi.mdk.domain.annotation.rex.RExExtract;
import uk.ac.ebi.mdk.domain.annotation.rex.RExTag;
import uk.ac.ebi.mdk.domain.entity.Metabolite;
import uk.ac.ebi.mdk.domain.entity.reaction.MetabolicParticipant;
import uk.ac.ebi.mdk.domain.entity.reaction.MetabolicReaction;
import uk.ac.ebi.mdk.domain.identifier.Identifier;

import java.sql.SQLException;
import java.util.*;

public class CompoundAnnotator
{
    /**
     * Add compound annotations to a collection of MDK MetabolicReactions.
     *
     * @param reactions The reactions to which annotations will be added.
     * @param bkmDB A BKM-React database object.
     * @param currencyMols A collection of currency molecules to ignore.
     * @throws SQLException If there is a problem reading the BKMDB.
     */
    public static void annotateReactions(
            Collection<MetabolicReaction> reactions,
            BKMDB bkmDB,
            Set<String> currencyMols) throws SQLException
    {
        for(MetabolicReaction reaction : reactions)
        {
            Map<String, Boolean> substrateIsInBRENDA = new HashMap<String, Boolean>();
            Map<String, Boolean> productIsInBRENDA = new HashMap<String, Boolean>();

            Map<String, Map<String, String>> substrateOtherPathways = new HashMap<String, Map<String, String>>();
            Map<String, Map<String, String>> productOtherPathways = new HashMap<String, Map<String, String>>();

            for(MetabolicParticipant substrate : reaction.getReactants())
            {
                Metabolite substrateCompound = substrate.getMolecule();
                String sid = substrateCompound.getIdentifier().toString();
                if(sid.contains("/"))
                {
                    sid = "m_" + substrateCompound.getIdentifier().toString().replaceAll("/", "") + "_c";
                }

                substrateOtherPathways.put(sid, new HashMap<String, String>());
            }

            for(MetabolicParticipant product : reaction.getProducts())
            {
                Metabolite productCompound = product.getMolecule();
                String pid = productCompound.getIdentifier().toString();
                if(pid.contains("/"))
                {
                    pid = "m_" + productCompound.getIdentifier().toString().replaceAll("/", "") + "_c";
                }

                productOtherPathways.put(pid, new HashMap<String, String>());
            }

            for(MetabolicParticipant substrate : reaction.getReactants())
            {
                Metabolite substrateCompound = substrate.getMolecule();
                String sid = substrateCompound.getIdentifier().toString();
                if(sid.contains("/"))
                {
                    sid = "m_" + substrateCompound.getIdentifier().toString().replaceAll("/", "") + "_c";
                }

                Collection<InChI> substrateInchis = substrateCompound.getAnnotations(InChI.class);

                for(InChI subInchi : substrateInchis)
                {
                    String substrateID = subInchi.toInChI();

                    for(MetabolicParticipant product : reaction.getProducts())
                    {
                        Metabolite productCompound = product.getMolecule();
                        String pid = productCompound.getIdentifier().toString();
                        if(pid.contains("/"))
                        {
                            pid = "m_" + productCompound.getIdentifier().toString().replaceAll("/", "") + "_c";
                        }

                        Collection<InChI> productInchis = productCompound.getAnnotations(InChI.class);

                        for(InChI proInchi : productInchis)
                        {
                            String productID = proInchi.toInChI();

                            //BRENDA
                            if(!bkmDB.getReactionsContainingSubstrateAndProduct(substrateID, productID).isEmpty())
                            {
                                substrateIsInBRENDA.put(substrateID, true);
                                productIsInBRENDA.put(productID, true);
                            }

                            //Alternative & Other
                            if(!currencyMols.contains(substrateID) && !currencyMols.contains(productID))
                            {
                                List<Integer> reactionsFound = bkmDB.getReactionsContainingSubstrateAndProduct(substrateID, productID);
                                Set<String> pathwaysFound = new HashSet<String>();
                                for(int reactionFound : reactionsFound)
                                {
                                    pathwaysFound.addAll(bkmDB.getPathwaysContainingReaction(reactionFound));
                                }

                                for(String pathwayFound : pathwaysFound)
                                {
                                    String pathwayFoundName = bkmDB.getPathwayName(pathwayFound);
                                    substrateOtherPathways.get(sid).put(pathwayFound, pathwayFoundName);
                                    productOtherPathways.get(pid).put(pathwayFound, pathwayFoundName);
                                }
                            }
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

                String id = substrate.getMolecule().getIdentifier().toString();
                if(id.contains("/"))
                {
                    id = "m_" + substrate.getMolecule().getIdentifier().toString().replaceAll("/", "") + "_c";
                }

                RExCompound compound = new RExCompound(id,
                        RExCompound.Type.SUBSTRATE,
                        substrateIsInBRENDABool,
                        false,
                        new HashMap<String, String>(),
                        substrateOtherPathways.get(id),
                        new HashMap<String, Integer>(),
                        new HashMap<String, Double>(),
                        0,
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

                String id = product.getMolecule().getIdentifier().toString();
                if(id.contains("/"))
                {
                    id = "m_" + product.getMolecule().getIdentifier().toString().replaceAll("/", "") + "_c";
                }

                RExCompound compound = new RExCompound(id,
                        RExCompound.Type.PRODUCT,
                        productIsInBRENDABool,
                        false,
                        new HashMap<String, String>(),
                        productOtherPathways.get(id),
                        new HashMap<String, Integer>(),
                        new HashMap<String, Double>(),
                        0,
                        0);
                reaction.addAnnotation(compound);
            }
        }
    }

    public static void calculateExtraction(Collection<MetabolicReaction> reactions, String organism, Scores scores)
    {
        for(MetabolicReaction reaction : reactions)
        {
            int occurrences = 0;
            for(RExExtract extract : reaction.getAnnotations(RExExtract.class))
            {
                for(Identifier orgID : extract.organisms())
                {
                    if(orgID.toString().equals(organism))
                    {
                        extract.setIsInCorrectOrganism(true);
                        occurrences++;
                    }
                }
            }

            for(RExCompound compound : reaction.getAnnotations(RExCompound.class))
            {
                double brendaScore;
                if(compound.isInBRENDA())
                {
                    brendaScore = scores.getBrenda();
                }
                else
                {
                    brendaScore = scores.getNotBrenda();
                }

                double occurrencesScore = scores.getOneExtraction();
                if(occurrences == 2)
                {
                    occurrencesScore = scores.getTwoExtractions();
                }
                else if(occurrences == 3)
                {
                    occurrencesScore = scores.getThreeExtractions();
                }
                else if(occurrences == 4)
                {
                    occurrencesScore = scores.getFourExtractions();
                }
                else if(occurrences >= 5)
                {
                    occurrencesScore = scores.getFiveOrMoreExtractions();
                }

                double extraction = (1-occurrencesScore)*brendaScore + occurrencesScore;

                if(occurrences == 0)
                {
                    extraction = 0;
                }

                compound.setExtraction(extraction);
            }
        }
    }

    public static void calculateBranches(Collection<MetabolicReaction> reactions, Set<String> seedInChIs, Scores scores, Set<String> currencyMols)
    {
        int maxLength = scores.getMaxBranchLength();
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

        Set<Branch> branches = new HashSet<Branch>();
        Set<Branch> alternativeBranches = new HashSet<Branch>();

        for(String inchi : seedInChIs)
        {
            if(substrateIndex.containsKey(inchi))
            {
                for(MetabolicReaction reaction : substrateIndex.get(inchi))
                {
                    Set<String> seedIDs = new HashSet<String>();
                    for(MetabolicParticipant substrate : reaction.getReactants())
                    {
                        Metabolite m = substrate.getMolecule();
                        String id = m.getIdentifier().toString();
                        if(id.contains("/"))
                        {
                            id = "m_" + m.getIdentifier().toString().replaceAll("/", "") + "_c";
                        }

                        for(InChI inchia : m.getAnnotations(InChI.class))
                        {
                            if(seedInChIs.contains(inchia.toInChI()))
                            {
                                seedIDs.add(id);
                            }
                        }
                    }

                    boolean isInSeed = false;
                    boolean aboveThreshold = false;
                    for(RExCompound compound : reaction.getAnnotations(RExCompound.class))
                    {
                        if(compound.isInSeed())
                        {
                            isInSeed = true;
                        }

                        if(seedIDs.contains(compound.getID()) && compound.getExtraction() > 0.75)
                        {
                            aboveThreshold = true;
                        }
                    }

                    if(!isInSeed && aboveThreshold)
                    {
                        branches.add(new Branch(reaction, seedInChIs));
                    }
                }
            }
        }

        for(int i=0; i<maxLength; i++)
        {
            //Find any branches that have returned to the seed pathway
            for(Branch branch : branches)
            {
                MetabolicReaction lastReaction = branch.getFinalReaction();

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
                        branch.finalise(seedInChIs);
                        if(!branch.startsAndEndsWithSameSeedMolecule())
                        {
                            alternativeBranches.add(branch);
                        }
                    }
                }
            }

            //Prevent the branches found in the last step from extending any further
            for(Branch alternativeBranch : alternativeBranches)
            {
                branches.remove(alternativeBranch);
            }

            //Extend all remaining branches
            Set<Branch> branchesToAdd = new HashSet<Branch>();
            for(Branch branch : branches)
            {
                MetabolicReaction lastReaction = branch.getFinalReaction();

                Map<MetabolicReaction, Set<Metabolite>> nextReactionsWithLinkingMolecules =
                        new HashMap<MetabolicReaction, Set<Metabolite>>();
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
                            if(!nextReactionsWithLinkingMolecules.containsKey(nextReaction))
                            {
                                nextReactionsWithLinkingMolecules.put(nextReaction, new HashSet<Metabolite>());
                            }

                            nextReactionsWithLinkingMolecules.get(nextReaction).add(m);
                        }
                    }
                }

                for(MetabolicReaction reaction : nextReactionsWithLinkingMolecules.keySet())
                {
                    boolean isInSeed = false;
                    for(RExCompound compound : reaction.getAnnotations(RExCompound.class))
                    {
                        if(compound.isInSeed())
                        {
                            isInSeed = true;
                        }
                    }

                    //Are the linking molecules currency molecules?
                    boolean linkersAreCurrency = true;
                    boolean aboveThreshold = false;
                    for(Metabolite linker : nextReactionsWithLinkingMolecules.get(reaction))
                    {
                        for(MetabolicParticipant substrate : reaction.getReactants())
                        {
                            if(linker == substrate.getMolecule())
                            {
                                String id = linker.getIdentifier().toString();
                                if(id.contains("/"))
                                {
                                    id = "m_" + linker.getIdentifier().toString().replaceAll("/", "") + "_c";
                                }

                                for(RExCompound compound : reaction.getAnnotations(RExCompound.class))
                                {
                                    if(compound.getID().equals(id))
                                    {
                                        //if(compound.getExtraction() > 0.75)
                                        //{
                                        aboveThreshold = true;

                                        //}
                                    }
                                }
                            }
                        }

                        for(InChI inchi : linker.getAnnotations(InChI.class))
                        {
                            if(!currencyMols.contains(inchi.toString()))
                            {
                                linkersAreCurrency = false;
                            }
                        }
                    }

                    if(!isInSeed
                            && aboveThreshold
                            && !branch.containsReaction(reaction)
                            && !branch.containsMetabolite(nextReactionsWithLinkingMolecules.get(reaction))
                            && !linkersAreCurrency)
                    {
                        Branch newBranch = branch.extendBranch(reaction, nextReactionsWithLinkingMolecules.get(reaction));
                        branchesToAdd.add(newBranch);
                    }
                }
            }

            branches.clear();
            branches.addAll(branchesToAdd);
        }

        int branchID = 0;
        for(Branch branch : alternativeBranches)
        {
            branchID++;

            for(MetabolicReaction reaction : branch.getReactions())
            {
                Set<String> substrateIDs = new HashSet<String>();
                for(Metabolite m : branch.getSubstrates(reaction))
                {
                    String id = m.getIdentifier().toString();
                    if(id.contains("/"))
                    {
                        id = "m_" + m.getIdentifier().toString().replaceAll("/", "") + "_c";
                    }
                    substrateIDs.add(id);
                }

                Set<String> productIDs = new HashSet<String>();
                for(Metabolite m : branch.getProducts(reaction))
                {
                    String id = m.getIdentifier().toString();
                    if(id.contains("/"))
                    {
                        id = "m_" + m.getIdentifier().toString().replaceAll("/", "") + "_c";
                    }
                    productIDs.add(id);
                }

                for(RExCompound compound : reaction.getAnnotations(RExCompound.class))
                {
                    if(substrateIDs.contains(compound.getID())
                            || productIDs.contains(compound.getID()))
                    {
                        compound.addBranch(Integer.toString(branchID), branch.length(), branch.getScore());
                    }
                }
            }
        }
    }

    /**
     * Calculate the relevance of a collection of MetabolicReactions and update their compound annotations.
     *
     * @param reactions The reactions to calculate relevance for.
     */
    public static void calculateAlternativePathwayRelevance(Collection<MetabolicReaction> reactions, Pathway seed,
                                                            String organism, Collection<String> seedIDs, Set<String> currencyMols,
                                                            Scores scores, BKMDB bkmDB) throws SQLException {
        List<MetabolicReaction> seedReactions = seed.getReactions();
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

                    if(!currencyMols.contains(id))
                    {
                        seedInChIs.add(id);
                    }
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

                    if(!currencyMols.contains(id))
                    {
                        seedInChIs.add(id);
                    }
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

                if((startTerminus || seedProductIndex.containsKey(substrateInchiString))
                        && !currencyMols.contains(substrateInchiString))
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

                        if((endTerminus || seedSubstrateIndex.containsKey(productInchiString))
                                && !currencyMols.contains(productInchiString))
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

        if(seedIDs == null)
        {
            for(Integer seedReactionFound : seedReactionsFound)
            {
                List<String> pathwayIDs = bkmDB.getPathwaysContainingReaction(seedReactionFound);
                seedPathwaysFound.addAll(pathwayIDs);
            }
        }
        else
        {
            seedPathwaysFound.addAll(seedIDs);
        }

        for(MetabolicReaction reaction : reactions)
        {
            for(RExCompound compound : reaction.getAnnotations(RExCompound.class))
            {
                for(String pathwayID : compound.getOtherPathways().keySet())
                {
                    if(seedPathwaysFound.contains(pathwayID))
                    {
                        compound.addAlternativePathway(pathwayID, compound.getOtherPathways().get(pathwayID));
                    }
                }

                for(String pathwayID : seedPathwaysFound)
                {
                    compound.removeOtherPathway(pathwayID);
                }
            }
        }

        for(MetabolicReaction reaction : reactions)
        {
            Set<String> substrateIsInSeedIndex = new HashSet<String>();
            Set<String> productIsInSeedIndex = new HashSet<String>();
            for(MetabolicParticipant substrate : reaction.getReactants())
            {
                Metabolite substrateCompound = substrate.getMolecule();
                String sid = substrateCompound.getIdentifier().toString();
                if(sid.contains("/"))
                {
                    sid = "m_" + substrateCompound.getIdentifier().toString().replaceAll("/", "") + "_c";
                }

                for(InChI substrateInchi : substrate.getMolecule().getAnnotations(InChI.class))
                {
                    for(MetabolicParticipant product : reaction.getProducts())
                    {
                        Metabolite productCompound = product.getMolecule();
                        String pid = productCompound.getIdentifier().toString();
                        if(sid.contains("/"))
                        {
                            pid = "m_" + productCompound.getIdentifier().toString().replaceAll("/", "") + "_c";
                        }

                        for(InChI productInchi : product.getMolecule().getAnnotations(InChI.class))
                        {
                            if(seed.containsPair(substrateInchi.toInChI(), productInchi.toInChI()))
                            {
                                if(!substrateIsInSeedIndex.contains(sid))
                                {
                                    substrateIsInSeedIndex.add(sid);
                                }

                                if(!productIsInSeedIndex.contains(pid))
                                {
                                    productIsInSeedIndex.add(pid);
                                }
                            }
                        }
                    }
                }
            }

            for(RExCompound compound : reaction.getAnnotations(RExCompound.class))
            {
                if(compound.getType() == RExCompound.Type.SUBSTRATE)
                {
                    if(substrateIsInSeedIndex.contains(compound.getID()))
                    {
                        compound.setIsInSeed(true);
                    }
                }
                else if(compound.getType() == RExCompound.Type.PRODUCT)
                {
                    if(productIsInSeedIndex.contains(compound.getID()))
                    {
                        compound.setIsInSeed(true);
                    }
                }
            }
        }

        Pathway pathway = new Pathway(reactions);

        Map<MetabolicReaction, Double> greatestFScores = new HashMap<MetabolicReaction, Double>();
        for(MetabolicReaction reaction : reactions)
        {
            TreeSet<Double> fscores = new TreeSet<Double>();
            for(RExExtract extract : reaction.getAnnotations(RExExtract.class))
            {
                Set<String> inchis = new HashSet<String>();
                for(InChI inchi : extract.molecules())
                {
                    inchis.add(inchi.toInChI());
                }
                extract.setTotalMetabolitesInSource(inchis.size());
                inchis.retainAll(seedInChIs);
                extract.setTotalSeedMetabolitesInSource(inchis.size());

                if(extract.isInCorrectOrganism())
                {
                    fscores.add(calculateFScore(extract.totalMetabolitesInSource(),
                            seedInChIs.size(), extract.totalSeedMetabolitesInSource()));
                }
            }

            if(!fscores.isEmpty())
            {
                double greatestFScore = fscores.last();
                greatestFScores.put(reaction, greatestFScore);
            }
            else
            {
                greatestFScores.put(reaction, 0.0);
            }
        }

        for(MetabolicReaction reaction : reactions)
        {
            int occurrences = 0;
            for(RExExtract extract : reaction.getAnnotations(RExExtract.class))
            {
                for(Identifier orgID : extract.organisms())
                {
                    if(orgID.toString().equals(organism))
                    {
                        extract.setIsInCorrectOrganism(true);
                        occurrences++;
                    }
                }
            }

            double fscore = greatestFScores.get(reaction);

            double sourceScore = Math.log(fscore)
                                 * scores.getSeedMoleculeDiceScoreMultiplier()
                                 + scores.getSeedMoleculeDiceScoreConstant();
            if(sourceScore < 0)
            {
                sourceScore = 0;
            }

            for(RExCompound compound : reaction.getAnnotations(RExCompound.class))
            {
                double score = sourceScore;

                if(compound.isInSeed())
                {
                    score = score + ((1-score)*scores.getInSeed());
                }

                if(occurrences == 1)
                {
                    score = score + ((1-score)*scores.getOneExtractionRelevance());
                }
                else if(occurrences == 2)
                {
                    score = score + ((1-score)*scores.getTwoExtractionsRelevance());
                }
                else if(occurrences == 3)
                {
                    score = score + ((1-score)*scores.getThreeExtractionsRelevance());
                }
                else if(occurrences == 4)
                {
                    score = score + ((1-score)*scores.getFourExtractionsRelevance());
                }
                else if(occurrences >= 5)
                {
                    score = score + ((1-score)*scores.getFiveOrMoreExtractionsRelevance());
                }

                TreeSet<Double> branchScores = new TreeSet<Double>(compound.getBranchScores().values());
                if(!branchScores.isEmpty())
                {
                    score = score + ((1-score)*branchScores.last());
                }

                compound.setRelevance(score);
            }
        }
    }

    public static void calculatePathwayLinkRelevance(
            Pathway pathway, Collection<String> pathwaysOfInterest)
    {
        Set<MetabolicReaction> seedReactions = new HashSet<MetabolicReaction>();
        Set<MetabolicReaction> reactionsInPathwaysOfInterest = new HashSet<MetabolicReaction>();

        for(MetabolicReaction reaction : pathway.getReactions())
        {
            Set<String> pathwaysFound  = new HashSet<String>();
            for(RExCompound compound : reaction.getAnnotations(RExCompound.class))
            {
                pathwaysFound.addAll(compound.getAlternativePathways().keySet());
                pathwaysFound.addAll(compound.getOtherPathways().keySet());

                if(compound.isInSeed())
                {
                    seedReactions.add(reaction);
                }
            }

            pathwaysFound.retainAll(pathwaysOfInterest);

            if(!pathwaysFound.isEmpty())
            {
                reactionsInPathwaysOfInterest.add(reaction);
            }
        }

        //Find links from seed reactions to reactions in pathways of interest.
        Set<List<MetabolicReaction>> links =
                findLinksBetweenReactions(seedReactions, reactionsInPathwaysOfInterest, pathway);

        //Find links from reactions in pathways of interest and seedReactions.
        links.addAll(findLinksBetweenReactions(reactionsInPathwaysOfInterest, seedReactions, pathway));


        //Score the relevances
        for(List<MetabolicReaction> link : links)
        {
            for(MetabolicReaction reaction : link)
            {
                for(RExCompound compound : reaction.getAnnotations(RExCompound.class))
                {
                    compound.setRelevance(0.5);
                }
            }
        }

        for(MetabolicReaction seedReaction : seedReactions)
        {
            for(RExCompound compound : seedReaction.getAnnotations(RExCompound.class))
            {
                compound.setRelevance(1.0);
            }
        }

        for(MetabolicReaction reactionInPathwayOfInterest : reactionsInPathwaysOfInterest)
        {
            for(RExCompound compound : reactionInPathwayOfInterest.getAnnotations(RExCompound.class))
            {
                compound.setRelevance(1.0);
            }
        }
    }

    private static Set<List<MetabolicReaction>> findLinksBetweenReactions(
            Set<MetabolicReaction> startReactions, Set<MetabolicReaction> endReactions, Pathway pathway)
    {
        Set<List<MetabolicReaction>> linksInProgress = new HashSet<List<MetabolicReaction>>();
        Set<MetabolicReaction> seenReactions = new HashSet<MetabolicReaction>();
        for(MetabolicReaction reaction : startReactions)
        {
            List<MetabolicReaction> link = new ArrayList<MetabolicReaction>();
            link.add(reaction);
            linksInProgress.add(link);
            seenReactions.add(reaction);
        }

        Set<List<MetabolicReaction>> completedLinks = new HashSet<List<MetabolicReaction>>();

        while(!linksInProgress.isEmpty())
        {
            Set<List<MetabolicReaction>> newLinks = new HashSet<List<MetabolicReaction>>();

            for(List<MetabolicReaction> linkInProgress : linksInProgress)
            {
                MetabolicReaction lastReaction = linkInProgress.get(linkInProgress.size()-1);
                for(RExCompound lastCompound : lastReaction.getAnnotations(RExCompound.class))
                {
                    if(lastCompound.getType() == RExCompound.Type.PRODUCT
                            && lastCompound.getExtraction() > 5.0)
                    {
                        for(MetabolicReaction nextReaction : pathway.getReactionsContainingSubstrate(lastCompound.getID()))
                        {
                            if(!seenReactions.contains(nextReaction))
                            {
                                List<MetabolicReaction> newLink = new ArrayList<MetabolicReaction>(linkInProgress);
                                newLink.add(nextReaction);

                                if(endReactions.contains(nextReaction))
                                {
                                    completedLinks.add(newLink);
                                }
                                else
                                {
                                    //Check if the same compound has an adequate extraction score.
                                    boolean foundNextCompound = false;
                                    for(RExCompound nextCompound : nextReaction.getAnnotations(RExCompound.class))
                                    {
                                        if(nextCompound.getID().equals(lastCompound.getID())
                                                && nextCompound.getType() == RExCompound.Type.SUBSTRATE
                                                && nextCompound.getExtraction() > 5.0)
                                        {
                                            foundNextCompound = true;
                                        }
                                    }

                                    if(foundNextCompound)
                                    {
                                        newLinks.add(newLink);
                                    }
                                }
                            }
                        }
                    }
                }
            }

            linksInProgress = newLinks;
        }

        return completedLinks;
    }

    public static double sourceMolsScore(int sourceMols)
    {
        //return 3.0*(Math.tanh(0.75*(double)sourceMols - 2.0) + 1.0);
        return Math.tanh(0.75*(double)sourceMols - 2.0) + 1.0;
    }

    public static double finalScore(double score)
    {
        double wholeScore = (Math.tanh(score/2.0 - 2.0) + 1.0)/2.0;
        double threeDP = (double)Math.round(wholeScore * 1000.0)/1000.0;
        return threeDP;
    }

    private static double calculateFScore(int totalFound, int totalCorrect, int correctFound)
    {
        if(correctFound == 0)
        {
            return 0;
        }

        double precision = (double)correctFound/(double)totalFound;
        double recall = (double)correctFound/(double)totalCorrect;

        return 2*((precision*recall)/(precision+recall));
    }
}
