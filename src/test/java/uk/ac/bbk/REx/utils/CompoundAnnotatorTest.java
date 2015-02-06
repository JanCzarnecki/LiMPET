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
import java.util.TreeMap;

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
        TreeMap<String, Double> results = new TreeMap<String, Double>();

        for(MetabolicReaction reaction : pathway.getReactions())
        {
            for(RExCompound compound : reaction.getAnnotations(RExCompound.class))
            {
                String id = String.format("%s-%s", reaction.getIdentifier().toString(), compound.getID());
                results.put(id, compound.getRelevance());
            }
        }

        String resultsString = String.format("%s - %s - %s - %s",
                results.get("rex1-seed1"),
                results.get("rex3-otherPathway1"),
                results.get("rex5-link1"),
                results.get("rex8-other1"));

        assertEquals("1.0 - 1.0 - 0.5 - 0.0", resultsString);
    }

    @Test
    public void sourceMolsScoreTest()
    {
        System.out.println(CompoundAnnotator.sourceMolsScore(10));
    }

    @Test
    public void finalScoreTest()
    {
        System.out.println(CompoundAnnotator.finalScore(5.2));
    }
}
