package uk.ac.bbk.REx.internalTypes;

import java.io.FileNotFoundException;
import java.util.*;

import uk.ac.bbk.REx.exception.CHEBIException;
import uk.ac.bbk.REx.utils.QueryBuilder;
import uk.ac.ebi.mdk.domain.annotation.InChI;
import uk.ac.ebi.mdk.domain.entity.Metabolite;
import uk.ac.ebi.mdk.domain.entity.reaction.MetabolicParticipant;
import uk.ac.ebi.mdk.domain.entity.reaction.MetabolicReaction;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SeedPathway
{
	List<MetabolicReaction> reactions;
	Set<String> chemicalNames;
	Map<String, Set<MetabolicReaction>> substrateInchiIndex;
	Map<String, Set<MetabolicReaction>> productInchiIndex;
    Set<String> inchis;

	public SeedPathway(List<MetabolicReaction> someReactions) throws CHEBIException
	{
		reactions = someReactions;
		chemicalNames = new HashSet<String>();
		substrateInchiIndex = new HashMap<String, Set<MetabolicReaction>>();
		productInchiIndex = new HashMap<String, Set<MetabolicReaction>>();
        inchis = new HashSet<String>();
		
		Set<Metabolite> metabolites = new HashSet<Metabolite>();
		for(MetabolicReaction r : reactions)
		{
            for(MetabolicParticipant participant : r.getParticipants())
            {
			    metabolites.add(participant.getMolecule());
            }
			
			for(MetabolicParticipant substrate : r.getReactants())
			{
                Metabolite metabolite = substrate.getMolecule();
				chemicalNames.add(metabolite.getName());

                String inchi = null;
                if(metabolite.hasAnnotation(InChI.class))
                {
                    for(InChI inchiAnnotation : metabolite.getAnnotations(InChI.class))
                    {
                        inchi = inchiAnnotation.toInChI();
                    }
                }

                if(!substrateInchiIndex.containsKey(inchi))
                {
                    substrateInchiIndex.put(inchi, new HashSet<MetabolicReaction>());
                }
                substrateInchiIndex.get(inchi).add(r);
                inchis.add(inchi);
			}

            for(MetabolicParticipant product : r.getProducts())
            {
                Metabolite metabolite = product.getMolecule();
                chemicalNames.add(metabolite.getName());

                String inchi = null;
                if(metabolite.hasAnnotation(InChI.class))
                {
                    for(InChI inchiAnnotation : metabolite.getAnnotations(InChI.class))
                    {
                        inchi = inchiAnnotation.toInChI();
                    }
                }

                if(!productInchiIndex.containsKey(inchi))
                {
                    productInchiIndex.put(inchi, new HashSet<MetabolicReaction>());
                }
                productInchiIndex.get(inchi).add(r);
                inchis.add(inchi);
            }
		}
	}
	
	public List<MetabolicReaction> getReactions()
	{
		return reactions;
	}
	
	public Set<String> getChemicalNames()
	{
		return chemicalNames;
	}
	
	public Set<String> constructQueries(String organismID) throws CHEBIException, FileNotFoundException
    {
		QueryBuilder qb = new QueryBuilder();
		return qb.build(organismID, inchis);
	}
	
	public boolean containsPair(String substrateInchi, String productInchi)
	{
		if(!substrateInchiIndex.containsKey(substrateInchi)
				|| !productInchiIndex.containsKey(productInchi))
		{
			return false;
		}
		Set<MetabolicReaction> substrateReactions = substrateInchiIndex.get(substrateInchi);
		Set<MetabolicReaction> productReactions = productInchiIndex.get(productInchi);
		
		Set<MetabolicReaction> commonReactions = new HashSet<MetabolicReaction>(substrateReactions);
		commonReactions.retainAll(productReactions);
		
		if(commonReactions.size() > 0)
		{
			return true;
		}
		else
		{
			return false;
		}
	}
	
	public boolean containsReaction(MetabolicReaction input)
	{
		Set<MetabolicReaction> substratesWith = new HashSet<MetabolicReaction>();
		for(MetabolicParticipant substrate : input.getReactants())
		{
            Metabolite metabolite = substrate.getMolecule();
            String inchi = null;
            if(metabolite.hasAnnotation(InChI.class))
            {
                for(InChI inchiAnnotation : metabolite.getAnnotations(InChI.class))
                {
                    inchi = inchiAnnotation.toInChI();
                }
            }

			if(substrateInchiIndex.containsKey(inchi))
			{
				substratesWith.addAll(substrateInchiIndex.get(inchi));
			}
		}

        Set<MetabolicReaction> productsWith = new HashSet<MetabolicReaction>();
        for(MetabolicParticipant product : input.getProducts())
        {
            Metabolite metabolite = product.getMolecule();
            String inchi = null;
            if(metabolite.hasAnnotation(InChI.class))
            {
                for(InChI inchiAnnotation : metabolite.getAnnotations(InChI.class))
                {
                    inchi = inchiAnnotation.toInChI();
                }
            }

            if(productInchiIndex.containsKey(inchi))
            {
                productsWith.addAll(productInchiIndex.get(inchi));
            }
        }
		
		substratesWith.retainAll(productsWith);
		if(substratesWith.size() > 0)
		{
			return true;
		}
		else
		{
			return false;
		}
	}
}
