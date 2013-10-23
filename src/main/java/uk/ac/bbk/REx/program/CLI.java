package uk.ac.bbk.REx.program;

import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.cli.*;
import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.ConfigurationParameterSettings;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLInputSource;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLException;
import org.sbml.jsbml.SBMLWriter;
import uk.ac.bbk.REx.db.bkmDB.BKMDB;
import uk.ac.bbk.REx.exception.CHEBIException;
import uk.ac.bbk.REx.internalTypes.SeedPathway;
import uk.ac.bbk.REx.utils.*;
import uk.ac.ebi.mdk.domain.annotation.rex.RExCompound;
import uk.ac.ebi.mdk.domain.annotation.rex.RExExtract;
import uk.ac.ebi.mdk.domain.entity.DefaultEntityFactory;
import uk.ac.ebi.mdk.domain.entity.EntityFactory;
import uk.ac.ebi.mdk.domain.entity.Reconstruction;
import uk.ac.ebi.mdk.domain.entity.reaction.BiochemicalReaction;
import uk.ac.ebi.mdk.domain.entity.reaction.MetabolicReaction;
import uk.ac.ebi.mdk.domain.tool.AutomaticCompartmentResolver;
import uk.ac.ebi.mdk.io.xml.sbml.SBMLIOUtil;
import uk.ac.ebi.mdk.io.xml.sbml.SBMLReactionReader;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import java.io.*;
import java.lang.reflect.Type;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class CLI
{
    public static void main(String[] args) throws
            IOException,
            CHEBIException,
            XMLStreamException,
            InvalidXMLException,
            ResourceInitializationException,
            CollectionException,
            AnalysisEngineProcessException,
            CASException,
            SBMLException,
            JAXBException,
            ParserConfigurationException,
            ClassNotFoundException,
            SQLException,
            InstantiationException,
            IllegalAccessException
    {
        System.setProperty("java.util.logging.config.file", "/uk/ac/bbk/REx/settings/log.properties");
        LogManager logMan=LogManager.getLogManager();
        logMan.readConfiguration(CLI.class.getResourceAsStream("/uk/ac/bbk/REx/settings/log.properties"));
        List<Logger> parentLoggers = new ArrayList<Logger>();
        parentLoggers.add(Logger.getLogger("uk.ac.bbk.REx"));

        Options options = new Options();
        options.addOption("h", "help", false, "Print this message.");
        options.addOption("m", "mode", true, "The mode.");
        options.addOption("i", "input", true, "Either the seed pathway with which to find articles or the object " +
                "file containing previously extracted reactions.");
        options.addOption("p", "pmidCutOffs", true, "");
        options.addOption("ic", "inputCutOffs", true, "");
        options.addOption("e", "expected", true, "");
        options.addOption("n", "numberOfArticles", true, "The maximum number of articles to retrieve " +
                "per small molecule.");
        options.addOption("q", "queries", true, "A file of custom queries (if necessary).");
        options.addOption("s", "speciesID", true, "The NCBI Taxonomy ID of the species to extract PPIs from.");
        options.addOption("sn", "speciesName", true, "The name of the species to extract PPIs from.");
        options.addOption("o", "output", true, "The output file for the extracted pathway.");

        CommandLineParser parser = new PosixParser();
        CommandLine cmd;
        try
        {
            cmd = parser.parse(options, args);
        }
        catch (ParseException e)
        {
            System.err.println("The arguments could not be parsed.");
            System.exit(1);
            return;
        }

        if(cmd.hasOption("h")
                || cmd.getOptions().length == 0)
        {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("java -jar REx.jar", options);
            System.exit(0);
            return;
        }

        if(cmd.getOptionValue("m").equals("extraction"))
        {
            InputStream input = new BufferedInputStream(new FileInputStream(new File(cmd.getOptionValue("i"))));
            SBMLReactionReader sbmlReader = new SBMLReactionReader(
                    input, DefaultEntityFactory.getInstance(), new AutomaticCompartmentResolver());
            List<MetabolicReaction> seedReactions = new ArrayList<MetabolicReaction>();

            while(sbmlReader.hasNext())
            {
                MetabolicReaction r = sbmlReader.next();
                seedReactions.add(r);
            }
            sbmlReader.close();
            input.close();

            int maxReturn = Integer.parseInt(cmd.getOptionValue("n"));
            String speciesID = cmd.getOptionValue("s");

            InputStream speciesNames = null;
            if(cmd.hasOption("sn"))
            {
                speciesNames = new BufferedInputStream(new FileInputStream(new File(cmd.getOptionValue("sn"))));
            }

            InputStream queriesStream = null;
            if(cmd.hasOption("q"))
            {
                queriesStream = new BufferedInputStream(new FileInputStream(new File(cmd.getOptionValue("q"))));
            }

            String pmidCutOffs = null;
            if(cmd.hasOption("p"))
            {
                pmidCutOffs = cmd.getOptionValue("p");
            }

            List<MetabolicReaction> combinedReactions = extractReactions(
                    seedReactions, maxReturn, speciesID, speciesNames, queriesStream, pmidCutOffs);
            speciesNames.close();
            queriesStream.close();

            EntityFactory entityFactory = DefaultEntityFactory.getInstance();
            Reconstruction recon = entityFactory.newReconstruction();

            for(MetabolicReaction mdkReaction : combinedReactions)
            {
                recon.addReaction(mdkReaction);
            }

            SBMLIOUtil util = new SBMLIOUtil(entityFactory, 2, 4);
            SBMLDocument doc = util.getDocument(recon);

            SBMLWriter writer = new SBMLWriter();
            writer.write(doc, new BufferedOutputStream(new FileOutputStream(new File(cmd.getOptionValue("o")))));
        }
        else if(cmd.getOptionValue("m").equals("relevance"))
        {
            InputStream input = new BufferedInputStream(new FileInputStream(new File(cmd.getOptionValue("i"))));
            SBMLReactionReader sbmlReader = new SBMLReactionReader(
                    input, DefaultEntityFactory.getInstance(), new AutomaticCompartmentResolver());
            Set<MetabolicReaction> reactions = new HashSet<MetabolicReaction>();

            while(sbmlReader.hasNext())
            {
                MetabolicReaction r = sbmlReader.next();
                reactions.add(r);
            }
            input.close();

            CompoundAnnotator.calculateAlternativePathwayRelevance(reactions);

            EntityFactory entityFactory = DefaultEntityFactory.getInstance();
            Reconstruction recon = entityFactory.newReconstruction();

            for(MetabolicReaction mdkReaction : reactions)
            {
                recon.addReaction(mdkReaction);
            }

            SBMLIOUtil util = new SBMLIOUtil(entityFactory, 2, 4);
            SBMLDocument doc = util.getDocument(recon);

            SBMLWriter writer = new SBMLWriter();
            writer.write(doc, new BufferedOutputStream(new FileOutputStream(new File(cmd.getOptionValue("o")))));
        }
        else if(cmd.getOptionValue("m").equals("continuousTest"))
        {
            Map<String, Integer> pmidCutoffs = new HashMap<String, Integer>();
            Scanner sc = new Scanner(new BufferedReader(new FileReader(new File(cmd.getOptionValue("ic")))));
            String line;
            while(sc.hasNextLine())
            {
                line = sc.nextLine();
                String[] values = line.split(",");
                pmidCutoffs.put(values[0], Integer.parseInt(values[1]));
            }

            sc.close();

            int highest = 0;
            for(String pmid : pmidCutoffs.keySet())
            {
                int cutoff = pmidCutoffs.get(pmid);
                if(cutoff > highest)
                {
                    highest = cutoff;
                }
            }

            InputStream input = new BufferedInputStream(new FileInputStream(new File(cmd.getOptionValue("i"))));
            SBMLReactionReader sbmlReader = new SBMLReactionReader(
                    input, DefaultEntityFactory.getInstance(), new AutomaticCompartmentResolver());
            Set<MetabolicReaction> reactions = new HashSet<MetabolicReaction>();

            while(sbmlReader.hasNext())
            {
                MetabolicReaction r = sbmlReader.next();
                reactions.add(r);
            }
            sbmlReader.close();

            InputStream seedInput = new BufferedInputStream(new FileInputStream(new File(cmd.getOptionValue("s"))));
            SBMLReactionReader seedSBMLReader = new SBMLReactionReader(
                    seedInput, DefaultEntityFactory.getInstance(), new AutomaticCompartmentResolver());
            Set<MetabolicReaction> seedReactions = new HashSet<MetabolicReaction>();

            while(seedSBMLReader.hasNext())
            {
                MetabolicReaction r = seedSBMLReader.next();
                seedReactions.add(r);
            }
            seedSBMLReader.close();

            InputStream expectedInput = new BufferedInputStream(new FileInputStream(new File(cmd.getOptionValue("e"))));
            SBMLReactionReader expectedSBMLReader = new SBMLReactionReader(
                    expectedInput, DefaultEntityFactory.getInstance(), new AutomaticCompartmentResolver());
            Set<MetabolicReaction> expectedReactions = new HashSet<MetabolicReaction>();

            while(expectedSBMLReader.hasNext())
            {
                MetabolicReaction r = expectedSBMLReader.next();
                expectedReactions.add(r);
            }
            expectedSBMLReader.close();

            BKMDB bkmDB = new BKMDB();
            Gson gson = new Gson();
            Type mapType = new TypeToken<Map<String, String>>(){}.getType();
            Map<String, String> currencyMoleculesMap = gson.fromJson(new InputStreamReader(
                    CLI.class.getResourceAsStream("/uk/ac/bbk/REx/settings/currencyMolecules.json")), mapType);
            Set<String> currencyMolecules = new HashSet<String>(currencyMoleculesMap.values());

            Writer w = new BufferedWriter(new FileWriter(new File(cmd.getOptionValue("o"))));
            for(int i=highest; i>0; i--)
            {
                Set<MetabolicReaction> reactionsToRemove = new HashSet<MetabolicReaction>();
                for(MetabolicReaction r : reactions)
                {
                    Set<RExExtract> extractsToRemove = new HashSet<RExExtract>();
                    for(RExExtract extract : r.getAnnotations(RExExtract.class))
                    {
                        String pmid = extract.source().getAccession();
                        if(pmidCutoffs.get(pmid) == i)
                        {
                            extractsToRemove.add(extract);
                        }
                    }

                    for(RExExtract extract : extractsToRemove)
                    {
                        r.removeAnnotation(extract);
                    }

                    if(r.getAnnotations(RExExtract.class).isEmpty())
                    {
                        reactionsToRemove.add(r);
                    }
                }

                for(MetabolicReaction r : reactionsToRemove)
                {
                    reactions.remove(r);
                }

                for(MetabolicReaction r : reactions)
                {
                    Set<RExCompound> compoundsToRemove = new HashSet<RExCompound>();
                    Collection<RExCompound> compounds = r.getAnnotations(RExCompound.class);
                    for(RExCompound compound : compounds)
                    {
                        compoundsToRemove.add(compound);
                    }

                    for(RExCompound compound : compoundsToRemove)
                    {
                        r.removeAnnotation(compound);
                    }
                }

                CompoundAnnotator.annotateReactions(reactions, seedReactions, bkmDB, currencyMolecules);
                CompoundAnnotator.calculateAlternativePathwayRelevance(reactions);

                Results results = Evaluator.evaluateResults(reactions, expectedReactions, new HashSet<String>());
                w.write(i + "," + results.getRecall() + "," + results.getPrecision() + "\n");
            }

            w.close();
        }
    }

    public static List<MetabolicReaction> extractReactions(List<MetabolicReaction> seedReactions,
                                                    int maxReturn,
                                                    String speciesID,
                                                    InputStream speciesNames,
                                                    InputStream queriesStream,
                                                    String pmidCutOffs)
            throws CHEBIException, XMLStreamException, IOException, InvalidXMLException, ResourceInitializationException, CollectionException, AnalysisEngineProcessException, CASException, ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException
    {
        SeedPathway seed = new SeedPathway(seedReactions);
        Set<String> queries;

        File originalSpeciesFile = new File("data/species-light-original.tsv");
        File speciesFile = new File("data/species-light.tsv");

        if(speciesNames != null)
        {
            Scanner sc = new Scanner(speciesNames);
            Set<String> organismNames = new HashSet<String>();
            while(sc.hasNextLine())
            {
                organismNames.add(sc.nextLine());
            }
            sc.close();

            StringBuilder newOrganismNames = new StringBuilder();
            for(String newOrganismName : organismNames)
            {
                newOrganismNames.append(newOrganismName).append("|");
            }

            StringBuilder originalSpeciesFileString = new StringBuilder();
            Scanner sc2 = new Scanner(new BufferedInputStream(new FileInputStream(originalSpeciesFile)));
            while(sc2.hasNextLine())
            {
                originalSpeciesFileString.append(sc2.nextLine()).append("\n");
            }
            sc2.close();

            String speciesFileString = originalSpeciesFileString.toString().replaceAll(
                    "species:ncbi:" + speciesID + "\t", "species:ncbi:" + speciesID + "\t" + newOrganismNames);

            Writer w = new BufferedWriter(new FileWriter(speciesFile));
            w.write(speciesFileString);
            w.close();
        }
        else
        {
            Files.copy(originalSpeciesFile, speciesFile);
        }

        if(queriesStream != null)
        {
            Scanner sc = new Scanner(queriesStream);
            queries = new HashSet<String>();
            while(sc.hasNextLine())
            {
                queries.add(sc.nextLine());
            }
        }
        else
        {
            queries = seed.constructQueries(speciesID);
        }

        Gson gson = new Gson();
        String queriesJSON = gson.toJson(queries);

        //Initialise the PubMed reader
        XMLInputSource reader = new XMLInputSource(
                CLI.class.getResourceAsStream("/uk/ac/bbk/REx/desc/MultiplePubMedReader.xml"), null);
        CollectionReaderDescription crDesc = UIMAFramework.getXMLParser().parseCollectionReaderDescription(reader);

        ConfigurationParameterSettings crParams = crDesc.getMetaData().getConfigurationParameterSettings();
        crParams.setParameterValue("queries", queriesJSON);
        crParams.setParameterValue("maxReturn", maxReturn);

        if(pmidCutOffs != null)
        {
            crParams.setParameterValue("pmidCutOffs", pmidCutOffs);
        }

        System.out.println("Retrieving list of articles from PubMed.");

        CollectionReader cr = null;
        try
        {
            cr = UIMAFramework.produceCollectionReader(crDesc);
        }
        catch(ResourceInitializationException e)
        {
            System.out.println("There seems to be a problem connecting to PubMed. Try running the program again.");
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            Logger.getLogger("uk.ac.bbk.REx.program.CLI").log(Level.INFO, sw.toString());
            System.exit(1);
        }

        //Initialise the analysis engine.
        XMLInputSource in = new XMLInputSource(
                CLI.class.getResourceAsStream("/uk/ac/bbk/REx/desc/RExAnnotator.xml"), null);
        AnalysisEngineDescription aeDesc = UIMAFramework.getXMLParser().parseAnalysisEngineDescription(in);
        ConfigurationParameterSettings aeParams = aeDesc.getMetaData().getConfigurationParameterSettings();
        aeParams.setParameterValue("organism", speciesID);

        //BANNER writes to System.out. Suppress by assigning a dummy PrintStream to System.out.
        PrintStream originalStream = System.out;
        PrintStream dummyStream = new PrintStream(new OutputStream(){
            public void write(int b)
            {
                //Do nothing
            }
        });
        System.setOut(dummyStream);

        AnalysisEngine ae = UIMAFramework.produceAnalysisEngine(aeDesc);

        System.setOut(originalStream);

        Set<String> sections = new HashSet<String>();
        sections.add("methods");
        sections.add("references");

        System.out.println("Extracting reactions from articles.");

        Type mapType = new TypeToken<Map<String, String>>(){}.getType();
        Map<String, String> currencyMoleculesMap = gson.fromJson(new InputStreamReader(
                CLI.class.getResourceAsStream("/uk/ac/bbk/REx/settings/currencyMolecules.json")), mapType);
        Set<String> currencyMolecules = new HashSet<String>(currencyMoleculesMap.values());
        List<JCas> cases = new ArrayList<JCas>();

        //Loop through each CAS in the reader.
        while(cr.hasNext())
        {
            //create a CAS, given an Analysis Engine (ae)
            CAS cas = ae.newCAS();

            try
            {
                cr.getNext(cas);
            }
            catch(SocketTimeoutException e)
            {
                continue;
            }
            catch(ConnectException e)
            {
                continue;
            }

            ae.process(cas);
            cases.add(cas.getJCas());

            double progress = ((double)cr.getProgress()[0].getCompleted() / (double)cr.getProgress()[0].getTotal()) * 100;
            if(cr.hasNext())
            {
                System.out.print("Progress: " + (int)Math.floor(progress) + "%\r");
            }
            else
            {
                System.out.println("Progress: 100%");
            }
        }

        cr.destroy();

        List<BiochemicalReaction> mdkReactions = Converter.convertUIMAReactionsToMDK(cases, sections, speciesID);
        EntityFactory entityFactory = DefaultEntityFactory.getInstance();

        List<MetabolicReaction> combinedReactions = ReactionCombiner.combineReactions(
                mdkReactions, new HashSet<String>(), entityFactory);
        BKMDB bkmDB = new BKMDB();
        CompoundAnnotator.annotateReactions(combinedReactions, seedReactions, bkmDB, currencyMolecules);
        return combinedReactions;
    }
}
