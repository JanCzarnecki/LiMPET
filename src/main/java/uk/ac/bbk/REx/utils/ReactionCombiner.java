package uk.ac.bbk.REx.utils;

import uk.ac.ebi.mdk.domain.annotation.Annotation;
import uk.ac.ebi.mdk.domain.annotation.InChI;
import uk.ac.ebi.mdk.domain.annotation.Synonym;
import uk.ac.ebi.mdk.domain.annotation.rex.RExExtract;
import uk.ac.ebi.mdk.domain.entity.EntityFactory;
import uk.ac.ebi.mdk.domain.entity.Metabolite;
import uk.ac.ebi.mdk.domain.entity.reaction.MetabolicParticipant;
import uk.ac.ebi.mdk.domain.entity.reaction.MetabolicReaction;

import java.util.*;

public class ReactionCombiner
{
    /**
     * Combines separate MetabolicReactions that are determined to be describing the reaction.
     *
     * @param singleReactions A collection of reactions (that have, typically, just been converted from UIMA reactions.
     * @param currencyMols A collection of currency molecules that will be ignored when combining reactions.
     * @param entityFactory
     * @return
     */
    public static List<MetabolicReaction> combineReactions(
            Collection<? extends MetabolicReaction> singleReactions,
            Collection<String> currencyMols,
            EntityFactory entityFactory)
    {
        //Create chemical indexes
        Map<String, Set<MetabolicReaction>> substrateIndex = new HashMap<String, Set<MetabolicReaction>>();
        Map<String, Set<MetabolicReaction>> productIndex = new HashMap<String, Set<MetabolicReaction>>();

        for(MetabolicReaction singleReaction : singleReactions)
        {
            for(MetabolicParticipant substrate : singleReaction.getReactants())
            {
                Metabolite metabolite = substrate.getMolecule();
                String inchi = getMetaboliteInChI(metabolite);

                if(!currencyMols.contains(inchi))
                {
                    if(!substrateIndex.containsKey(metabolite.getIdentifier().toString()))
                    {
                        substrateIndex.put(metabolite.getIdentifier().toString(), new HashSet<MetabolicReaction>());
                    }

                    substrateIndex.get(metabolite.getIdentifier().toString()).add(singleReaction);
                }
            }

            for(MetabolicParticipant product : singleReaction.getProducts())
            {
                Metabolite metabolite = product.getMolecule();
                String inchi = getMetaboliteInChI(metabolite);

                if(!currencyMols.contains(inchi))
                {
                    if(!productIndex.containsKey(metabolite.getIdentifier().toString()))
                    {
                        productIndex.put(metabolite.getIdentifier().toString(), new HashSet<MetabolicReaction>());
                    }

                    productIndex.get(metabolite.getIdentifier().toString()).add(singleReaction);
                }
            }
        }

        Set<Set<MetabolicReaction>> reactionGroups = new HashSet<Set<MetabolicReaction>>();
        Set<MetabolicReaction> linkedReactions = new HashSet<MetabolicReaction>();

        for(MetabolicReaction r : singleReactions)
        {
            if(!linkedReactions.contains(r))
            {
                Set<MetabolicReaction> commonReactions = new HashSet<MetabolicReaction>();
                Set<MetabolicReaction> links = new HashSet<MetabolicReaction>();

                boolean first = true;
                boolean allCurrency = true;
                for(MetabolicParticipant substrate : r.getReactants())
                {
                    Metabolite m = substrate.getMolecule();
                    String id = m.getIdentifier().toString();
                    String inchi = getMetaboliteInChI(m);

                    if(!currencyMols.contains(inchi))
                    {
                        if(first)
                        {
                            commonReactions.addAll(substrateIndex.get(id));
                        }
                        else
                        {
                            commonReactions.retainAll(substrateIndex.get(id));
                        }

                        allCurrency = false;
                    }

                    first = false;
                }

                if(allCurrency)
                {
                    commonReactions.clear();
                }

                allCurrency = true;
                for(MetabolicParticipant product : r.getProducts())
                {
                    Metabolite m = product.getMolecule();
                    String id = m.getIdentifier().toString();
                    String inchi = getMetaboliteInChI(m);

                    if(!currencyMols.contains(inchi))
                    {
                        commonReactions.retainAll(productIndex.get(id));
                        allCurrency = false;
                    }
                }

                if(allCurrency)
                {
                    commonReactions.clear();
                }

                for(MetabolicReaction possibleLink : commonReactions)
                {
                    Set<MetabolicReaction> theseCommonReactions = new HashSet<MetabolicReaction>();

                    first = true;
                    allCurrency = true;
                    for(MetabolicParticipant substrate : possibleLink.getReactants())
                    {
                        Metabolite m = substrate.getMolecule();
                        String id = m.getIdentifier().toString();
                        String inchi = getMetaboliteInChI(m);

                        if(!currencyMols.contains(inchi))
                        {
                            if(first)
                            {
                                theseCommonReactions.addAll(substrateIndex.get(id));
                            }
                            else
                            {
                                theseCommonReactions.retainAll(substrateIndex.get(id));
                            }

                            allCurrency = false;
                        }

                        first = false;
                    }

                    if(allCurrency)
                    {
                        theseCommonReactions.clear();
                    }

                    allCurrency = true;
                    for(MetabolicParticipant product : possibleLink.getProducts())
                    {
                        Metabolite m = product.getMolecule();
                        String id = m.getIdentifier().toString();
                        String inchi = getMetaboliteInChI(m);

                        if(!currencyMols.contains(inchi))
                        {
                            theseCommonReactions.retainAll(productIndex.get(id));
                            allCurrency = false;
                        }
                    }

                    if(allCurrency)
                    {
                        theseCommonReactions.clear();
                    }

                    if(theseCommonReactions.contains(r))
                    {
                        linkedReactions.add(r);
                        linkedReactions.add(possibleLink);
                        links.add(r);
                        links.add(possibleLink);
                    }
                }

                reactionGroups.add(links);
            }
        }

        List<MetabolicReaction> combinedReactions = new ArrayList<MetabolicReaction>();
        for(Set<MetabolicReaction> group : reactionGroups)
        {
            combinedReactions.add(combineReactions(group, entityFactory));
        }

        return combinedReactions;

        /*
        for(MetabolicReaction r : singleReactions)
        {
            for(MetabolicParticipant substrate : r.getReactants())
            {
                if(!currencyMols.contains(getMetaboliteInChI(substrate.getMolecule())))
                {
                    for(MetabolicParticipant product : r.getProducts())
                    {
                        if(!currencyMols.contains(getMetaboliteInChI(product.getMolecule())))
                        {
                            Set<MetabolicReaction> substrateReactions = substrateIndex.get(substrate.getMolecule().getIdentifier().toString());
                            Set<MetabolicReaction> productReactions = productIndex.get(product.getMolecule().getIdentifier().toString());

                            Set<MetabolicReaction> commonReactions = new HashSet<MetabolicReaction>(substrateReactions);
                            commonReactions.retainAll(productReactions);

                            allLinkedReactions.add(commonReactions);
                        }
                    }
                }
            }
        }

        Map<MetabolicReaction, Set<MetabolicReaction>> reactionLinks = new HashMap<MetabolicReaction, Set<MetabolicReaction>>();

        for(Set<MetabolicReaction> linkedReactions : allLinkedReactions)
        {
            for(MetabolicReaction r : linkedReactions)
            {
                if(!reactionLinks.containsKey(r))
                {
                    reactionLinks.put(r, new HashSet<MetabolicReaction>());
                }

                for(MetabolicReaction r2 : linkedReactions)
                {
                    if(r != r2)
                    {
                        reactionLinks.get(r).add(r2);
                    }
                }
            }
        }

        Set<MetabolicReaction> reactionsLeft = new HashSet<MetabolicReaction>(singleReactions);
        Set<Set<MetabolicReaction>> groups = new HashSet<Set<MetabolicReaction>>();

        for(MetabolicReaction r : singleReactions)
        {
            if(reactionsLeft.contains(r))
            {
                Set<MetabolicReaction> group = new HashSet<MetabolicReaction>();
                group.add(r);
                reactionsLeft.remove(r);
                int groupSizeBefore = 1;
                int groupSizeAfter = -1;

                while(groupSizeBefore != groupSizeAfter)
                {
                    groupSizeBefore = group.size();
                    Set<MetabolicReaction> addToGroup = new HashSet<MetabolicReaction>();

                    for(MetabolicReaction reactionInCurrentGroup : group)
                    {
                        if(reactionLinks.containsKey(reactionInCurrentGroup))
                        {
                            for(MetabolicReaction linkedReaction : reactionLinks.get(reactionInCurrentGroup))
                            {
                                addToGroup.add(linkedReaction);
                                reactionsLeft.remove(linkedReaction);
                            }
                        }
                    }

                    group.addAll(addToGroup);
                    groupSizeAfter = group.size();
                }

                groups.add(group);
            }
        }

        List<MetabolicReaction> combinedReactions = new ArrayList<MetabolicReaction>();
        for(Set<MetabolicReaction> group : groups)
        {
            combinedReactions.add(combineReactions(group, entityFactory));
        }

        return combinedReactions;
        */
    }

    private static String getMetaboliteInChI(Metabolite metabolite)
    {
        String id = null;
        for(InChI inchi : metabolite.getAnnotations(InChI.class))
        {
            id = inchi.toInChI();
        }

        return id;
    }

    private static MetabolicReaction combineReactions(Set<MetabolicReaction> reactions, EntityFactory entityFactory)
    {
        Map<String, Metabolite> substrates = new HashMap<String, Metabolite>();
        Map<String, Metabolite> products = new HashMap<String, Metabolite>();
        Set<RExExtract> extracts = new HashSet<RExExtract>();

        for(MetabolicReaction reaction : reactions)
        {
            for(MetabolicParticipant substrate : reaction.getReactants())
            {
                Metabolite metabolite = substrate.getMolecule();
                String id = metabolite.getIdentifier().toString();

                if(!substrates.containsKey(id))
                {
                    substrates.put(id, metabolite);
                }

                Annotation synonym = new Synonym(metabolite.getName());
                substrates.get(id).addAnnotation(synonym);
            }

            for(MetabolicParticipant product : reaction.getProducts())
            {
                Metabolite metabolite = product.getMolecule();
                String id = metabolite.getIdentifier().toString();

                if(!products.containsKey(id))
                {
                    products.put(id, metabolite);
                }

                Annotation synonym = new Synonym(metabolite.getName());
                products.get(id).addAnnotation(synonym);
            }

            for(RExExtract extract : reaction.getAnnotations(RExExtract.class))
            {
                extracts.add(extract);
            }
        }

        MetabolicReaction combinedReaction = entityFactory.reaction();

        for(Metabolite substrate : substrates.values())
        {
            combinedReaction.addReactant(substrate);
        }

        for(Metabolite product : products.values())
        {
            combinedReaction.addProduct(product);
        }

        for(RExExtract extract : extracts)
        {
            combinedReaction.addAnnotation(extract);
        }

        return combinedReaction;
    }
}
