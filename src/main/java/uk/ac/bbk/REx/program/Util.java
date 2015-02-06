package uk.ac.bbk.REx.program;

import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLWriter;
import uk.ac.ebi.mdk.domain.entity.*;
import uk.ac.ebi.mdk.domain.entity.reaction.BiochemicalReaction;
import uk.ac.ebi.mdk.domain.entity.reaction.MetabolicParticipant;
import uk.ac.ebi.mdk.domain.entity.reaction.MetabolicReaction;
import uk.ac.ebi.mdk.domain.identifier.Identifier;
import uk.ac.ebi.mdk.domain.tool.AutomaticCompartmentResolver;
import uk.ac.ebi.mdk.io.xml.sbml.SBMLIOUtil;
import uk.ac.ebi.mdk.io.xml.sbml.SBMLReactionReader;

import javax.xml.stream.XMLStreamException;
import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Util
{
    private Util(){}

    public static List<MetabolicReaction> readInSBML(String file, PrintStream errorStream)
    {
        InputStream input = null;
        try
        {
            input = new BufferedInputStream(new FileInputStream(new File(file)));
        }
        catch (FileNotFoundException e)
        {
            errorStream.println(String.format("The file %s could not be found.", file));
            logStackTrace(e);
            System.exit(1);
        }

        SBMLReactionReader sbmlReader = null;
        try
        {
            sbmlReader = new SBMLReactionReader(
                    input, DefaultEntityFactory.getInstance(), new AutomaticCompartmentResolver());
        }
        catch (XMLStreamException e)
        {
            errorStream.println(String.format("The input XML file, %s, could not be read.", file));
            logStackTrace(e);
            System.exit(1);
        }
        List<MetabolicReaction> reactions = new ArrayList<MetabolicReaction>();

        while(sbmlReader.hasNext())
        {
            MetabolicReaction r = sbmlReader.next();
            reactions.add(r);
        }

        try
        {
            input.close();
        }
        catch (IOException e)
        {
            errorStream.println(String.format("The stream from the file, %s, could not be closed.", file));
            logStackTrace(e);
            System.exit(1);
        }

        return reactions;
    }

    public static void writeOutSBML(Collection<MetabolicReaction> reactions, File file, PrintStream errorStream)
    {
        EntityFactory entityFactory = DefaultEntityFactory.getInstance();
        Reconstruction recon = entityFactory.newReconstruction();

        for(MetabolicReaction mdkReaction : reactions)
        {
            recon.addReaction(mdkReaction);
        }

        SBMLIOUtil util = new SBMLIOUtil(entityFactory, 2, 4);
        SBMLDocument doc = util.getDocument(recon);

        SBMLWriter writer = new SBMLWriter();
        try
        {
            writer.write(doc, new BufferedOutputStream(new FileOutputStream(file)));
        }
        catch (XMLStreamException e)
        {
            errorStream.println(String.format("Error writing to file %s.", file));
            logStackTrace(e);
            System.exit(1);
        }
        catch (FileNotFoundException e)
        {
            errorStream.println(String.format("Error writing to file %s.", file));
            logStackTrace(e);
            System.exit(1);
        }
    }

    private static void logStackTrace(Throwable e)
    {
        Logger LOGGER = Logger.getLogger(Util.class.getName());
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        LOGGER.log(Level.INFO, sw.toString());
    }

    public static String reactionToString(MetabolicReaction reaction)
    {
        StringBuilder output = new StringBuilder();

        List<MetabolicParticipant> substrates = reaction.getReactants();
        int totalSubstrates = substrates.size();
        int count = 0;
        for(MetabolicParticipant substrate : substrates)
        {
            count++;
            output.append(substrate.getMolecule().getName() + " ");

            if(count < totalSubstrates)
            {
                output.append("+ ");
            }
        }

        output.append("-> ");

        List<MetabolicParticipant> products = reaction.getProducts();
        int totalProducts = products.size();
        count = 0;
        for(MetabolicParticipant product : products)
        {
            count++;
            output.append(product.getMolecule().getName() + " ");

            if(count < totalProducts)
            {
                output.append("+ ");
            }
        }

        return output.toString();
    }
}
