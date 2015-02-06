package uk.ac.bbk.REx.utils;

import org.junit.Test;
import uk.ac.ebi.mdk.domain.entity.DefaultEntityFactory;
import uk.ac.ebi.mdk.domain.entity.reaction.MetabolicReaction;
import uk.ac.ebi.mdk.domain.tool.AutomaticCompartmentResolver;
import uk.ac.ebi.mdk.io.xml.sbml.SBMLIOUtil;
import uk.ac.ebi.mdk.io.xml.sbml.SBMLReactionReader;

import javax.xml.stream.XMLStreamException;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

public class HumanWriterTest
{
    @Test
    public void writeToTSVTest() throws XMLStreamException, IOException {
        InputStream input = new BufferedInputStream(this.getClass().getResourceAsStream("completeOutput.xml"));
        SBMLReactionReader sbmlReader = new SBMLReactionReader(
                input, DefaultEntityFactory.getInstance(), new AutomaticCompartmentResolver());
        List<MetabolicReaction> reactions = new ArrayList<MetabolicReaction>();

        while(sbmlReader.hasNext())
        {
            MetabolicReaction r = sbmlReader.next();
            reactions.add(r);
        }

        input.close();

        StringWriter sw = new StringWriter();
        HumanWriter.writeToTSV(reactions, 0.75, sw);
        System.out.println(sw.toString());
    }
}
