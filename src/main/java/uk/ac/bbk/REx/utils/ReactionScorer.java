package uk.ac.bbk.REx.utils;

import uk.ac.ebi.mdk.domain.annotation.rex.RExCompound;
import uk.ac.ebi.mdk.domain.annotation.rex.RExExtract;
import uk.ac.ebi.mdk.domain.annotation.rex.RExTag;
import uk.ac.ebi.mdk.domain.entity.reaction.MetabolicParticipant;
import uk.ac.ebi.mdk.domain.entity.reaction.MetabolicReaction;

import java.sql.SQLException;
import java.util.*;

public class ReactionScorer
{
    public static void scoreReactionExtraction(MetabolicReaction reaction) throws SQLException
    {
        Map<String, Integer> substrateOccurrences = new HashMap<String, Integer>();
        Map<String, Integer> productOccurrences = new HashMap<String, Integer>();

        for(RExExtract extract : reaction.getAnnotations(RExExtract.class))
        {
            for(RExTag tag : extract.tags())
            {
                if(tag.type().equals("substrate"))
                {
                    if(!substrateOccurrences.containsKey(tag.id()))
                    {
                        substrateOccurrences.put(tag.id(), 0);
                    }

                    substrateOccurrences.put(tag.id(), substrateOccurrences.get(tag.id()) + 1);
                }

                if(tag.type().equals("product"))
                {
                    if(!productOccurrences.containsKey(tag.id()))
                    {
                        productOccurrences.put(tag.id(), 0);
                    }

                    productOccurrences.put(tag.id(), productOccurrences.get(tag.id()) + 1);
                }
            }
        }

        for(RExCompound compound : reaction.getAnnotations(RExCompound.class))
        {
            double score = 0;

            if(compound.getType() == RExCompound.Type.SUBSTRATE)
            {
                score += substrateOccurrences.get(compound.getID());
            }
            else if(compound.getType() == RExCompound.Type.PRODUCT)
            {
                score += productOccurrences.get(compound.getID());
            }

            if(compound.isInBRENDA())
            {
                score += 5.0;
            }

            compound.setExtraction(score);
        }
    }

    public static void scoreReactionsAlternativePathwayRelevance(List<MetabolicReaction> reactions)
    {
        Set<String> seedPathways = new HashSet<String>();

        for(MetabolicReaction reaction : reactions)
        {
            for(MetabolicParticipant substrate : reaction.getReactants())
            {

            }
        }
    }
}
