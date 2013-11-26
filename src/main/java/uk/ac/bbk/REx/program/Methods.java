package uk.ac.bbk.REx.program;

import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
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
import uk.ac.bbk.REx.db.bkmDB.BKMDB;
import uk.ac.bbk.REx.db.documentDB.DocumentDB;
import uk.ac.bbk.REx.exception.BKMException;
import uk.ac.bbk.REx.exception.CHEBIException;
import uk.ac.bbk.REx.internalTypes.Pathway;
import uk.ac.bbk.REx.utils.*;
import uk.ac.ebi.mdk.domain.annotation.rex.RExCompound;
import uk.ac.ebi.mdk.domain.annotation.rex.RExExtract;
import uk.ac.ebi.mdk.domain.entity.DefaultEntityFactory;
import uk.ac.ebi.mdk.domain.entity.EntityFactory;
import uk.ac.ebi.mdk.domain.entity.reaction.BiochemicalReaction;
import uk.ac.ebi.mdk.domain.entity.reaction.MetabolicReaction;

import java.io.*;
import java.lang.reflect.Type;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Methods
{
    private static final Logger LOGGER = Logger.getLogger(Methods.class.getName());

    private Methods(){}

    public static void extraction(String inputFile, int maxReturn, String speciesID, String speciesNameFile,
                                  String queriesFile, String pmidCutoffs, String outputFile)
    {
        List<MetabolicReaction> seedReactions = Util.readInSBML(inputFile, System.err);

        InputStream speciesNames = null;
        if(speciesNameFile != null)
        {
            try
            {
                speciesNames = new BufferedInputStream(new FileInputStream(new File(speciesNameFile)));
            }
            catch (FileNotFoundException e)
            {
                System.err.println(String.format("The species name file %s could not be found.",
                        speciesNameFile));
                logStackTrace(e);
                System.exit(1);
            }
        }

        InputStream queriesStream = null;
        if(queriesFile != null)
        {
            try
            {
                queriesStream = new BufferedInputStream(new FileInputStream(new File(queriesFile)));
            }
            catch (FileNotFoundException e)
            {
                System.err.println(String.format("The queries file %s could not be found.",
                        queriesFile));
                logStackTrace(e);
                System.exit(1);
            }
        }

        List<MetabolicReaction> combinedReactions = extractReactions(
                seedReactions, maxReturn, speciesID, speciesNames, queriesStream, pmidCutoffs);

        if(speciesNames != null)
        {
            try
            {
                speciesNames.close();
            }
            catch (IOException e)
            {
                System.err.println(String.format("Error closing stream from file %s.\n", speciesNameFile));
                logStackTrace(e);
                System.exit(1);
            }
        }

        if(queriesStream != null)
        {
            try
            {
                queriesStream.close();
            }
            catch (IOException e)
            {
                System.err.println(String.format("Error closing stream from file %s.\n", queriesFile));
                logStackTrace(e);
                System.exit(1);
            }
        }

        Util.writeOutSBML(combinedReactions, outputFile, System.err);
    }

    public static void alternativePathwayRelevance(String inputFile, String outputFile)
    {
        List<MetabolicReaction> reactions = Util.readInSBML(inputFile, System.err);

        CompoundAnnotator.calculateAlternativePathwayRelevance(reactions);

        Util.writeOutSBML(reactions, outputFile, System.err);
    }

    public static void pathwayLinkRelevance(String inputFile, String pathwaysFile, String outputFile)
    {
        List<MetabolicReaction> reactions = Util.readInSBML(inputFile, System.err);

        InputStream pathwaysStream = null;
        try
        {
            pathwaysStream = new BufferedInputStream(new FileInputStream(new File(pathwaysFile)));
        }
        catch (FileNotFoundException e)
        {
            System.err.println(String.format("The file %s could not be found.", pathwaysFile));
            logStackTrace(e);
            System.exit(1);
        }

        List<String> pathwayIDs = new ArrayList<String>();
        Scanner sc = new Scanner(new BufferedInputStream(pathwaysStream));
        while(sc.hasNextLine())
        {
            pathwayIDs.add(sc.nextLine());
        }

        sc.close();

        CompoundAnnotator.calculatePathwayLinkRelevance(new Pathway(reactions), pathwayIDs);

        Util.writeOutSBML(reactions, outputFile, System.err);
    }

    public static void continuousTest(String inputFile, String seedFile, String expectedFile,
                                      String pmidCutoffsFile, String outputFile)
    {
        Map<String, Integer> pmidCutoffs = new HashMap<String, Integer>();
        Scanner sc = null;
        try
        {
            sc = new Scanner(new BufferedReader(new FileReader(new File(pmidCutoffsFile))));
        }
        catch (FileNotFoundException e)
        {
            System.err.println(String.format("The file %s could not be found.", pmidCutoffsFile));
            logStackTrace(e);
            System.exit(1);
        }

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

        List<MetabolicReaction> reactions = Util.readInSBML(inputFile, System.err);
        List<MetabolicReaction> seedReactions = Util.readInSBML(seedFile, System.err);
        List<MetabolicReaction> expectedReactions = Util.readInSBML(expectedFile, System.err);

        BKMDB bkmDB = null;
        try
        {
            bkmDB = new BKMDB();
        }
        catch (BKMException e)
        {
            System.err.println("Error reading internal BKM-React database.");
            logStackTrace(e);
            System.exit(1);
        }

        Gson gson = new Gson();
        Type mapType = new TypeToken<Map<String, String>>(){}.getType();
        Map<String, String> currencyMoleculesMap = gson.fromJson(new InputStreamReader(
                CLI.class.getResourceAsStream("/uk/ac/bbk/REx/settings/currencyMolecules.json")), mapType);
        Set<String> currencyMolecules = new HashSet<String>(currencyMoleculesMap.values());

        Writer w = null;
        try
        {
            w = new BufferedWriter(new FileWriter(new File(outputFile)));
        }
        catch (IOException e)
        {
            System.err.println(String.format("Error writing to file %s.\n", outputFile));
            logStackTrace(e);
            System.exit(1);
        }
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

            try
            {
                CompoundAnnotator.annotateReactions(reactions, seedReactions, bkmDB, currencyMolecules);
            }
            catch (SQLException e)
            {
                System.err.println("Error reading internal BKM-React database.");
                logStackTrace(e);
                System.exit(1);
            }
            CompoundAnnotator.calculateAlternativePathwayRelevance(reactions);

            Results results = Evaluator.evaluateResults(reactions, expectedReactions, currencyMolecules);
            try
            {
                w.write(i + "," + results.getRecall() + "," + results.getPrecision() + "\n");
            }
            catch (IOException e)
            {
                System.err.println(String.format("Error writing to file %s.\n", outputFile));
                logStackTrace(e);
                System.exit(1);
            }
        }

        try
        {
            w.close();
        }
        catch (IOException e)
        {
            System.err.println(String.format("The stream to the %s file could not be closed.", outputFile));
            logStackTrace(e);
            System.exit(1);
        }
    }

    public static void evaluate(String inputFile, String expectedFile, String outputFile)
    {
        List<MetabolicReaction> reactions = Util.readInSBML(inputFile, System.err);
        List<MetabolicReaction> expectedReactions = Util.readInSBML(expectedFile, System.err);

        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(MetabolicReaction.class, new MetabolicReactionSerializer());
        builder.setPrettyPrinting();
        Gson gson = builder.create();
        Type mapType = new TypeToken<Map<String, String>>(){}.getType();
        Map<String, String> currencyMoleculesMap = gson.fromJson(new InputStreamReader(
                CLI.class.getResourceAsStream("/uk/ac/bbk/REx/settings/currencyMolecules.json")), mapType);
        Set<String> currencyMolecules = new HashSet<String>(currencyMoleculesMap.values());

        Results results = Evaluator.evaluateResults(reactions, expectedReactions, currencyMolecules);

        Writer writer;
        try
        {
            writer = new BufferedWriter(new FileWriter(new File(outputFile)));
        }
        catch (IOException e)
        {
            System.err.println(String.format("The file %s could not be written to.", outputFile));
            logStackTrace(e);
            System.exit(1);
            return;
        }

        String output = gson.toJson(results);

        try
        {
            writer.write(output);
        }
        catch (IOException e)
        {
            System.err.println(String.format("The file %s could not be written to.", outputFile));
            logStackTrace(e);
            System.exit(1);
        }

        try
        {
            writer.close();
        }
        catch(IOException e)
        {
            System.err.println(String.format("The stream to the file %s could not be closed.", outputFile));
            logStackTrace(e);
            System.exit(1);
        }
    }

    public static void clearCache()
    {
        try
        {
            DocumentDB docDB = new DocumentDB();
            docDB.clearDatabase();
        }
        catch (InstantiationException e)
        {
            System.err.println("Unable to read document database.");
            logStackTrace(e);
            System.exit(1);
        }
        catch (IllegalAccessException e)
        {
            System.err.println("Unable to read document database.");
            logStackTrace(e);
            System.exit(1);
        }
        catch (ClassNotFoundException e)
        {
            System.err.println("Unable to read document database.");
            logStackTrace(e);
            System.exit(1);
        }
        catch (SQLException e)
        {
            System.err.println("Unable to read document database.");
            logStackTrace(e);
            System.exit(1);
        }
    }

    private static List<MetabolicReaction> extractReactions(Collection<MetabolicReaction> seedReactions,
                                                           int maxReturn,
                                                           String speciesID,
                                                           InputStream speciesNames,
                                                           InputStream queriesStream,
                                                           String pmidCutOffs)
    {

        Gson gson = new Gson();
        Type mapType = new TypeToken<Map<String, String>>(){}.getType();
        Map<String, String> currencyMoleculesMap = gson.fromJson(new InputStreamReader(
                CLI.class.getResourceAsStream("/uk/ac/bbk/REx/settings/currencyMolecules.json")), mapType);
        Set<String> currencyMolecules = new HashSet<String>(currencyMoleculesMap.values());

        Pathway seed = null;
        seed = new Pathway(seedReactions);
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
            Scanner sc2 = null;
            try
            {
                sc2 = new Scanner(new BufferedInputStream(new FileInputStream(originalSpeciesFile)));
            }
            catch (FileNotFoundException e)
            {
                System.err.println("The file species-light-original.tsv in the data directory could not be found.");
                logStackTrace(e);
                System.exit(1);
            }
            while(sc2.hasNextLine())
            {
                originalSpeciesFileString.append(sc2.nextLine()).append("\n");
            }
            sc2.close();

            String speciesFileString = originalSpeciesFileString.toString().replaceAll(
                    "species:ncbi:" + speciesID + "\t", "species:ncbi:" + speciesID + "\t" + newOrganismNames);

            try
            {
                Writer w = new BufferedWriter(new FileWriter(speciesFile));
                w.write(speciesFileString);
                w.close();
            }
            catch (IOException e)
            {
                System.err.println("The file species-light.tsv could not be written to the data directory.");
                logStackTrace(e);
                System.exit(1);
            }
        }
        else
        {
            try
            {
                Files.copy(originalSpeciesFile, speciesFile);
            }
            catch (IOException e)
            {
                System.err.println("The file species-light-original.tsv in the data directory could not be copied.");
                logStackTrace(e);
                System.exit(1);
            }
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
            try
            {
                queries = seed.constructQueries(speciesID, currencyMolecules);
            }
            catch (CHEBIException e)
            {
                System.err.println("The internal ChEBI database could not be read.");
                logStackTrace(e);
                System.exit(1);
                //Return so that the compiler knows that queries must have been initialised. It does not know
                //that a System.exit() ends the program.
                return null;
            }
            catch (FileNotFoundException e)
            {
                System.err.println("The file species-light.tsv in the data directory could not be read.");
                logStackTrace(e);
                System.exit(1);
                return null;
            }
        }

        String queriesJSON = gson.toJson(queries);

        //Initialise the PubMed reader
        XMLInputSource reader = new XMLInputSource(
                CLI.class.getResourceAsStream("/uk/ac/bbk/REx/desc/PubMedReader.xml"), null);
        CollectionReaderDescription crDesc = null;
        try
        {
            crDesc = UIMAFramework.getXMLParser().parseCollectionReaderDescription(reader);
        }
        catch (InvalidXMLException e)
        {
            System.err.println("There was an error reading internal configuration files.");
            logStackTrace(e);
            System.exit(1);
        }

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
            LOGGER.log(Level.INFO, sw.toString());
            System.exit(1);
        }

        //Initialise the analysis engine.
        XMLInputSource in = new XMLInputSource(
                CLI.class.getResourceAsStream("/uk/ac/bbk/REx/desc/RExAnnotator.xml"), null);
        AnalysisEngineDescription aeDesc = null;
        try
        {
            aeDesc = UIMAFramework.getXMLParser().parseAnalysisEngineDescription(in);
        }
        catch (InvalidXMLException e)
        {
            System.err.println("There was an error reading internal configuration files.");
            logStackTrace(e);
            System.exit(1);
        }
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

        AnalysisEngine ae = null;
        try
        {
            ae = UIMAFramework.produceAnalysisEngine(aeDesc);
        }
        catch (ResourceInitializationException e)
        {
            System.err.println("Error initialising analysis engine.");
            logStackTrace(e);
            System.exit(1);
        }

        System.setOut(originalStream);

        Set<String> sections = new HashSet<String>();
        sections.add("methods");
        sections.add("references");

        System.out.println("Extracting reactions from articles.");

        List<JCas> cases = new ArrayList<JCas>();

        boolean hasNext = false;
        try
        {
            hasNext = cr.hasNext();
        }
        catch (IOException e)
        {
            System.err.println("Error retrieving PubMed documents.");
            logStackTrace(e);
            System.exit(1);
        }
        catch (CollectionException e)
        {
            System.err.println("Error retrieving PubMed documents.");
            logStackTrace(e);
            System.exit(1);
        }

        //Loop through each CAS in the reader.
        while(hasNext)
        {
            //create a CAS, given an Analysis Engine (ae)
            CAS cas = null;
            try
            {
                cas = ae.newCAS();
            }
            catch (ResourceInitializationException e)
            {
                System.err.println("Error annotating document.");
                logStackTrace(e);
                System.exit(1);
            }

            try
            {
                cr.getNext(cas);
            }
            catch (CollectionException e)
            {
                logStackTrace(e);
                System.exit(1);
            }
            catch (IOException e)
            {
                logStackTrace(e);
                System.exit(1);
            }

            try
            {
                try
                {
                    ae.process(cas);
                }
                catch (AnalysisEngineProcessException e)
                {
                    System.err.println("Error annotating document.");
                    logStackTrace(e);
                    System.exit(1);
                }


                try
                {
                    cases.add(cas.getJCas());
                }
                catch (CASException e)
                {
                    System.err.println("Error annotating document.");
                    logStackTrace(e);
                    System.exit(1);
                }

                double progress =
                        ((double)cr.getProgress()[0].getCompleted() / (double)cr.getProgress()[0].getTotal()) * 100;
                try
                {
                    hasNext = cr.hasNext();
                }
                catch (IOException e)
                {
                    System.err.println("Error retrieving PubMed documents.");
                    logStackTrace(e);
                    System.exit(1);
                }
                catch (CollectionException e)
                {
                    System.err.println("Error retrieving PubMed documents.");
                    logStackTrace(e);
                    System.exit(1);
                }

                if(hasNext)
                {
                    System.out.print("Progress: " + (int)Math.floor(progress) + "%\r");
                }
                else
                {
                    System.out.println("Progress: 100%");
                }
            }
            catch(Exception e)
            {
                logStackTrace(e);
                continue;
            }
        }

        cr.destroy();

        List<BiochemicalReaction> mdkReactions = Converter.convertUIMAReactionsToMDK(cases, sections, speciesID,
                seed.getMetabolites(currencyMolecules));
        EntityFactory entityFactory = DefaultEntityFactory.getInstance();

        List<MetabolicReaction> combinedReactions = ReactionCombiner.combineReactions(
                mdkReactions, new HashSet<String>(), entityFactory);
        BKMDB bkmDB = null;
        try
        {
            bkmDB = new BKMDB();
        }
        catch (BKMException e)
        {
            System.err.println("Error reading internal BKM-React database.");
            logStackTrace(e);
            System.exit(1);
        }

        try
        {
            CompoundAnnotator.annotateReactions(combinedReactions, seedReactions, bkmDB, currencyMolecules);
        }
        catch (SQLException e)
        {
            System.err.println("Error reading internal BKM-React database.");
            logStackTrace(e);
            System.exit(1);
        }
        return combinedReactions;
    }

    private static void logStackTrace(Throwable e)
    {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        LOGGER.log(Level.INFO, sw.toString());
    }
}
