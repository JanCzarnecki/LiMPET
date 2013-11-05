package uk.ac.bbk.REx.utils;

import org.junit.Test;
import uk.ac.bbk.REx.internalTypes.Pathway;
import uk.ac.ebi.mdk.domain.annotation.rex.RExCompound;
import uk.ac.ebi.mdk.domain.entity.DefaultEntityFactory;
import uk.ac.ebi.mdk.domain.entity.reaction.MetabolicReaction;
import uk.ac.ebi.mdk.domain.tool.AutomaticCompartmentResolver;
import uk.ac.ebi.mdk.io.xml.sbml.SBMLReactionReader;

import javax.xml.stream.XMLStreamException;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static junit.framework.Assert.assertEquals;

public class CompoundAnnotatorTest
{
    @Test
    public void calculatePathwayRelevanceTest() throws XMLStreamException
    {
        InputStream input = new BufferedInputStream(
                this.getClass().getResourceAsStream("/uk/ac/bbk/REx/utils/linkRelevanceTest.xml"));
        SBMLReactionReader sbmlReader = new SBMLReactionReader(
                    input, DefaultEntityFactory.getInstance(), new AutomaticCompartmentResolver());

        List<MetabolicReaction> reactions = new ArrayList<MetabolicReaction>();
        while(sbmlReader.hasNext())
        {
            reactions.add(sbmlReader.next());
        }

        Pathway pathway = new Pathway(reactions);

        List<String> pathwaysOfInterest = new ArrayList<String>();
        pathwaysOfInterest.add("interestingPathway");

        CompoundAnnotator.calculatePathwayLinkRelevance(pathway, pathwaysOfInterest);
        double relevance = 0;

        for(MetabolicReaction reaction : pathway.getReactions())
        {
            if(reaction.getIdentifier().toString().equals("rex5"))
            {
                for(RExCompound compound : reaction.getAnnotations(RExCompound.class))
                {
                    if(compound.getID().equals("link1"))
                    {
                        relevance = compound.getRelevance();
                    }
                }
            }
        }

        assertEquals(0.5, relevance);
    }
}
