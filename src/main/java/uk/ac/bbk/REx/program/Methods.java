package uk.ac.bbk.REx.program;

import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.io.FileUtils;
import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.impl.Serialization;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.ConfigurationParameterSettings;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLInputSource;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLWriter;
import org.xml.sax.SAXException;
import uk.ac.bbk.REx.db.bioCycDB.PathwayConstructor;
import uk.ac.bbk.REx.db.bioCycDB.PathwayReader;
import uk.ac.bbk.REx.db.bkmDB.BKMDB;
import uk.ac.bbk.REx.db.documentDB.DocumentDB;
import uk.ac.bbk.REx.exception.BKMException;
import uk.ac.bbk.REx.exception.BioCycException;
import uk.ac.bbk.REx.exception.CHEBIException;
import uk.ac.bbk.REx.internalTypes.Pathway;
import uk.ac.bbk.REx.readers.PMCArticle;
import uk.ac.bbk.REx.readers.PubMedDownloader;
import uk.ac.bbk.REx.readers.PubMedSearcher;
import uk.ac.bbk.REx.types.Document;
import uk.ac.bbk.REx.utils.*;
import uk.ac.ebi.mdk.domain.annotation.rex.RExCompound;
import uk.ac.ebi.mdk.domain.annotation.rex.RExExtract;
import uk.ac.ebi.mdk.domain.entity.DefaultEntityFactory;
import uk.ac.ebi.mdk.domain.entity.EntityFactory;
import uk.ac.ebi.mdk.domain.entity.Reconstruction;
import uk.ac.ebi.mdk.domain.entity.reaction.BiochemicalReaction;
import uk.ac.ebi.mdk.domain.entity.reaction.MetabolicReaction;
import uk.ac.ebi.mdk.io.xml.sbml.SBMLIOUtil;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPathExpressionException;
import java.io.*;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Methods
{
    private static final Logger LOGGER = Logger.getLogger(Methods.class.getName());

    private Methods(){}

    public static void seedQueries(String inputFile, String speciesID, String speciesNameFile, String outputFile, File data)
    {
        System.out.println("Reading in pathway.");
        List<MetabolicReaction> seedReactions = Util.readInSBML(inputFile, System.err);
        System.out.println("Pathway read in successfully.");

        List<String> speciesNames = new ArrayList<String>();
        if(speciesNameFile != null)
        {
            InputStream speciesNamesStream = null;
            try
            {
                speciesNamesStream = new BufferedInputStream(new FileInputStream(new File(speciesNameFile)));
            }
            catch (FileNotFoundException e)
            {
                System.err.println(String.format("The species name file %s could not be found.",
                        speciesNameFile));
                logStackTrace(e);
                System.exit(1);
            }

            Scanner sc = new Scanner(speciesNamesStream);
            while(sc.hasNextLine())
            {
                speciesNames.add(sc.nextLine());
            }
        }

        Gson gson = new Gson();
        Type mapType = new TypeToken<Map<String, String>>(){}.getType();
        Map<String, String> currencyMoleculesMap = gson.fromJson(new InputStreamReader(
                Methods.class.getResourceAsStream("/uk/ac/bbk/REx/settings/currencyMolecules.json")), mapType);
        Set<String> currencyMolecules = new HashSet<String>(currencyMoleculesMap.values());
        Pathway seed = new Pathway(seedReactions);

        System.out.println("Constructing queries.");
        Set<String> queries;
        try
        {
            queries = seed.constructQueries(speciesID, speciesNames, currencyMolecules, data);
        }
        catch (CHEBIException e)
        {
            System.err.println("The internal ChEBI database could not be read.");
            logStackTrace(e);
            System.exit(1);
            return;
        }
        catch (FileNotFoundException e)
        {
            System.err.println("The file species-light.tsv in the data directory could not be read.");
            logStackTrace(e);
            System.exit(1);
            return;
        }
        catch (UnsupportedEncodingException e)
        {
            System.err.println("The data directory could not be read.");
            logStackTrace(e);
            System.exit(1);
            return;
        }

        System.out.println(String.format("Writing queries to %s", outputFile));
        Writer output;
        try
        {
            output = new BufferedWriter(new FileWriter(new File(outputFile)));

            for(String query : queries)
            {
                output.write(query + System.getProperty("line.separator"));
            }

            output.close();
        }
        catch (IOException e)
        {
            System.err.println(String.format(
                    "The file %s could not be written.", outputFile));
            logStackTrace(e);
            System.exit(1);
            return;
        }

        System.out.println(String.format("Queries written to %s", outputFile));
    }

    public static void metaCycDownload(String pathwayIDs, String outputFileString)
    {
        EntityFactory entityFactory = DefaultEntityFactory.getInstance();
        SBMLIOUtil util = new SBMLIOUtil(entityFactory, 2, 4);
        SBMLWriter writer = new SBMLWriter();

        List ids = Arrays.asList(pathwayIDs.split("\\|"));
        List<MetabolicReaction> reactions;

        try {
            PathwayConstructor pc = new PathwayConstructor();
            reactions = pc.constructPathway("META", ids);
        } catch (BioCycException e) {
            System.err.println("There was an error retrieving the MetaCyc pathways.");
            logStackTrace(e);
            System.exit(1);
            return;
        }

        File outputFile = new File(outputFileString);
        System.out.println(String.format("Writing pathway to %s", outputFile.getAbsolutePath()));
        Util.writeOutSBML(reactions, outputFile, System.err);
        System.out.println(String.format("Pathway written to %s", outputFile.getAbsolutePath()));
    }

    public static void downloadArticles(String queriesFileString, int maxReturn, String outputDirString)
    {
        File queriesFile = new File(queriesFileString);
        List<String> queries = new ArrayList<String>();
        Scanner sc = null;
        try
        {
            sc = new Scanner(new BufferedReader(new FileReader(queriesFile)));
        }
        catch (FileNotFoundException e)
        {
            System.err.println(String.format("The queries file %s could not be read.", queriesFileString));
            logStackTrace(e);
            System.exit(1);
            return;
        }

        while(sc.hasNextLine())
        {
            queries.add(sc.nextLine());
        }

        sc.close();

        Set<String> pmids = new HashSet<String>();
        PubMedSearcher searcher = null;
        try
        {
            searcher = new PubMedSearcher();
        }
        catch (XPathExpressionException e)
        {
            System.err.println("Error connecting to PubMed.");
            logStackTrace(e);
            System.exit(1);
            return;
        }
        catch (MalformedURLException e)
        {
            System.err.println("Error connecting to PubMed.");
            logStackTrace(e);
            System.exit(1);
            return;
        }
        catch (ParserConfigurationException e)
        {
            System.err.println("Error connecting to PubMed.");
            logStackTrace(e);
            System.exit(1);
            return;
        }

        for(String query : queries)
        {
            try
            {
                pmids.addAll(searcher.getPMIDs(query, maxReturn));
            }
            catch (IOException e)
            {
                System.err.println("Error searching PubMed.");
                logStackTrace(e);
                System.exit(1);
                return;
            }
            catch (SAXException e)
            {
                System.err.println("Error searching PubMed.");
                logStackTrace(e);
                System.exit(1);
                return;
            }
            catch (XPathExpressionException e)
            {
                System.err.println("Error searching PubMed.");
                logStackTrace(e);
                System.exit(1);
                return;
            }
            catch (InterruptedException e)
            {
                System.err.println("Error searching PubMed.");
                logStackTrace(e);
                System.exit(1);
                return;
            }
        }

        InaccessibleArticlesDocument inaccessibleArticlesDocument = null;
        try
        {
            inaccessibleArticlesDocument = new InaccessibleArticlesDocument();
        }
        catch (IOException e)
        {
            System.err.println("Error creating inaccessible articles document.");
            logStackTrace(e);
            System.exit(1);
            return;
        }
        File outputDir = new File(outputDirString);
        if(!outputDir.exists())
        {
            outputDir.mkdir();
        }

        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = null;
        try
        {
            transformer = tf.newTransformer();
        }
        catch (TransformerConfigurationException e)
        {
            System.err.println("Error writing articles.");
            logStackTrace(e);
            System.exit(1);
            return;
        }

        try
        {
            PubMedDownloader pmd = new PubMedDownloader(new ArrayList<String>(pmids), outputDir);
        }
        catch(Exception e)
        {
            System.err.println("Error downloading articles.");
            logStackTrace(e);
            System.exit(1);
            return;
        }

        Writer w = null;
        File inaccessibleArticlesFile = new File(outputDir, "inaccessibleArticles.html");
        try
        {
            w = new BufferedWriter(new FileWriter(inaccessibleArticlesFile));
            inaccessibleArticlesDocument.writeDocument(w);
            w.close();
        }
        catch (IOException e)
        {
            System.err.println(String.format("Error writing file %s", inaccessibleArticlesFile.getName()));
            logStackTrace(e);
            System.exit(1);
            return;
        }
    }

    public static void extractionFromDir(String inputDir, String speciesID, String speciesNames,
                                         String scoresFileString, String outputString, File data)
    {
        File output = new File(outputString);

        Gson gson = new Gson();
        Type mapType = new TypeToken<Map<String, String>>(){}.getType();
        Map<String, String> currencyMoleculesMap = gson.fromJson(new InputStreamReader(
                Methods.class.getResourceAsStream("/uk/ac/bbk/REx/settings/currencyMolecules.json")), mapType);
        Set<String> currencyMolecules = new HashSet<String>(currencyMoleculesMap.values());

        File scoresFile;
        if(scoresFileString == null)
        {
            File parent = null;
            try
            {
                parent = uk.ac.bbk.REx.utils.Files.getParentDirectory();
            }
            catch (UnsupportedEncodingException e)
            {
                System.err.println("The data directory could not be read.");
                logStackTrace(e);
                System.exit(1);
                return;
            }
            scoresFile = new File(parent, "/data/scores.json");
        }
        else
        {
            scoresFile = new File(scoresFileString);
        }

        Reader scoresReader = null;
        try
        {
            scoresReader = new BufferedReader(new FileReader(scoresFile));
        }
        catch (FileNotFoundException e)
        {
            System.err.println(String.format("The scores file %s could not be found.",
                    scoresFile));
            logStackTrace(e);
            System.exit(1);
        }

        Scores scores = gson.fromJson(scoresReader, Scores.class);

        //Initialise the PubMed reader
        XMLInputSource reader = new XMLInputSource(
                Methods.class.getResourceAsStream("/uk/ac/bbk/REx/desc/DirectoryReader.xml"), null);
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
        crParams.setParameterValue("directory", inputDir);

        CollectionReader cr = null;
        try
        {
            cr = UIMAFramework.produceCollectionReader(crDesc);
        }
        catch(ResourceInitializationException e)
        {
            System.out.println("There seems to be a problem reading the input files. Try running the program again.");
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            LOGGER.log(Level.INFO, sw.toString());
            System.exit(1);
        }

        InputStream namesStream = null;
        if(speciesID != null && speciesNames != null)
        {
            try
            {
                namesStream = new BufferedInputStream(new FileInputStream(new File(speciesNames)));
            }
            catch (FileNotFoundException e)
            {
                System.err.println(String.format("Error reading %s", speciesNames));
                logStackTrace(e);
                System.exit(1);
                return;
            }
        }

        List<MetabolicReaction> combinedReactions = extractReactions(
                cr, speciesID, namesStream, scores, currencyMolecules, data);

        Util.writeOutSBML(combinedReactions, output, System.err);
    }

    public static void scorePathway(String inputFile, String organism, String seedIDsString, String outputFileString,
                                    String seedFile, String scoresFileString, File data) {
        List<MetabolicReaction> reactions = Util.readInSBML(inputFile, System.err);
        Util.writeOutSBML(reactions, new File("test.xml"), System.err);
        Pathway seedPathway = new Pathway(Util.readInSBML(seedFile, System.err));

        List<String> seedIDs = null;
        if(seedIDsString != null)
        {
            seedIDs = new ArrayList<String>();
            for(String seedID : seedIDsString.split("\\|"))
            {
                seedIDs.add(seedID);
            }
        }

        Gson gson = new Gson();
        Type mapType = new TypeToken<Map<String, String>>(){}.getType();
        Map<String, String> currencyMoleculesMap = gson.fromJson(new InputStreamReader(
                Methods.class.getResourceAsStream("/uk/ac/bbk/REx/settings/currencyMolecules.json")), mapType);
        Set<String> currencyMolecules = new HashSet<String>(currencyMoleculesMap.values());

        File scoresFile;
        if(scoresFileString == null)
        {
            File parent = null;
            try
            {
                parent = uk.ac.bbk.REx.utils.Files.getParentDirectory();
            }
            catch (UnsupportedEncodingException e)
            {
                System.err.println("The data directory could not be read.");
                logStackTrace(e);
                System.exit(1);
                return;
            }
            scoresFile = new File(parent.getPath() + "/data/scores.json");
        }
        else
        {
            scoresFile = new File(scoresFileString);
        }

        Reader scoresReader = null;
        try
        {
            scoresReader = new BufferedReader(new FileReader(scoresFile));
        }
        catch(FileNotFoundException e)
        {
            System.err.println(String.format("The file %s could not be found.", scoresFile));
            logStackTrace(e);
            System.exit(1);
        }

        Scores scores = gson.fromJson(scoresReader, Scores.class);

        CompoundAnnotator.calculateExtraction(reactions, organism, scores);
        CompoundAnnotator.calculateBranches(reactions, seedPathway.getInchis(), scores, currencyMolecules);

        try
        {
            CompoundAnnotator.calculateAlternativePathwayRelevance(
                    reactions, seedPathway, organism, seedIDs, currencyMolecules, scores, new BKMDB(data));
        }
        catch (SQLException e)
        {
            System.err.println("Error reading internal BKM-React database.");
            logStackTrace(e);
            System.exit(1);
            return;
        }
        catch (BKMException e)
        {
            System.err.println("Error reading internal BKM-React database.");
            logStackTrace(e);
            System.exit(1);
            return;
        }

        File outputFile = new File(outputFileString);

        Util.writeOutSBML(reactions, outputFile, System.err);
    }

    public static void pathwayLinkRelevance(String inputFile, String pathwaysFile, String outputFileString)
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

        File outputFile = new File(outputFileString);

        Util.writeOutSBML(reactions, outputFile, System.err);
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
                Methods.class.getResourceAsStream("/uk/ac/bbk/REx/settings/currencyMolecules.json")), mapType);
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

    public static void convertToCytoscapeTSV(String inputFile, String outputDirString)
    {
        List<MetabolicReaction> reactions = Util.readInSBML(inputFile, System.err);
        File outputDir = new File(outputDirString);

        Gson gson = new Gson();
        Type mapType = new TypeToken<Map<String, String>>(){}.getType();
        Map<String, String> currencyMoleculesMap = gson.fromJson(new InputStreamReader(
                Methods.class.getResourceAsStream("/uk/ac/bbk/REx/settings/currencyMolecules.json")), mapType);
        Set<String> currencyMolecules = new HashSet<String>(currencyMoleculesMap.values());

        try
        {
            CytoscapeWriter.writeToTSV(reactions, currencyMolecules, outputDir);
        }
        catch (IOException e)
        {
            System.err.println("Error outputting files.");
            logStackTrace(e);
            System.exit(1);
        }
    }

    public static void convertToHumanTSV(String inputString, double extractionThreshold, String outputString)
    {
        List<MetabolicReaction> reactions = Util.readInSBML(inputString, System.err);
        File outputFile = new File(outputString);
        Writer w;
        try
        {
            w = new BufferedWriter(new FileWriter(outputFile));
        }
        catch (IOException e)
        {
            System.err.println("The output file could not be written to.");
            logStackTrace(e);
            System.exit(1);
            return;
        }

        try
        {
            HumanWriter.writeToTSV(reactions, extractionThreshold, w);
        }
        catch (IOException e)
        {
            System.err.println("Error outputting files.");
            logStackTrace(e);
            System.exit(1);
        }

        try
        {
            w.close();
        }
        catch (IOException e)
        {
            System.err.println("The output file could not be closed.");
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

    public static void rescoreExtraction(String inputFile, String seedPathwayFile, String seedName,
                                         String scoresFileString, String outputFile, File data)
    {
        List<MetabolicReaction> reactions = Util.readInSBML(inputFile, System.err);

        for(MetabolicReaction reaction : reactions)
        {
            List<RExCompound> compounds = new ArrayList<RExCompound>(reaction.getAnnotations(RExCompound.class));
            for(RExCompound compound : compounds)
            {
                reaction.removeAnnotation(compound);
            }
        }

        List<MetabolicReaction> seedReactions = Util.readInSBML(seedPathwayFile, System.err);

        Gson gson = new Gson();
        Type mapType = new TypeToken<Map<String, String>>(){}.getType();
        Map<String, String> currencyMoleculesMap = gson.fromJson(new InputStreamReader(
                Methods.class.getResourceAsStream("/uk/ac/bbk/REx/settings/currencyMolecules.json")), mapType);
        Set<String> currencyMolecules = new HashSet<String>(currencyMoleculesMap.values());

        File scoresFile;
        if(scoresFileString == null)
        {
            File parent = null;
            try
            {
                parent = uk.ac.bbk.REx.utils.Files.getParentDirectory();
            }
            catch (UnsupportedEncodingException e)
            {
                System.err.println("The data directory could not be read.");
                logStackTrace(e);
                System.exit(1);
                return;
            }
            scoresFile = new File(parent.getPath() + "/data/scores.json");
        }
        else
        {
            scoresFile = new File(scoresFileString);
        }

        Reader scoresReader = null;
        try
        {
            scoresReader = new BufferedReader(new FileReader(scoresFile));
        }
        catch (FileNotFoundException e)
        {
            System.err.println(String.format("The scores file %s could not be found.",
                    scoresFile));
            logStackTrace(e);
            System.exit(1);
        }

        Scores scores = gson.fromJson(scoresReader, Scores.class);

        BKMDB bkmDB = null;
        try
        {
            bkmDB = new BKMDB(data);
        }
        catch (BKMException e)
        {
            System.err.println("Error reading internal BKM-React database.");
            logStackTrace(e);
            System.exit(1);
        }

        try
        {
            CompoundAnnotator.annotateReactions(reactions, bkmDB, currencyMolecules);
        }
        catch (SQLException e)
        {
            System.err.println("Error reading internal BKM-React database.");
            logStackTrace(e);
            System.exit(1);
        }

        File output = new File(outputFile);
        Util.writeOutSBML(reactions, output, System.err);
    }

    private static List<MetabolicReaction> extractReactions(CollectionReader cr,
                                                           String speciesID,
                                                           InputStream speciesNames,
                                                           Scores scores,
                                                           Set<String> currencyMolecules,
                                                           File data)
    {
        File originalSpeciesFile = new File(data, "species-light-original.tsv");
        File speciesFile = new File(data, "species-light.tsv");

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

        //Initialise the analysis engine.
        XMLInputSource in = new XMLInputSource(
                Methods.class.getResourceAsStream("/uk/ac/bbk/REx/desc/RExAnnotator.xml"), null);
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
        aeParams.setParameterValue("userdataDir", data.getPath());

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

        File tempDir = new File(System.getProperty("java.io.tmpdir") + "/rex/");
        tempDir.mkdir();
        tempDir.deleteOnExit();

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
                continue;
            }

            try
            {
                cr.getNext(cas);
            }
            catch (CollectionException e)
            {
                logStackTrace(e);
                try
                {
                    hasNext = cr.hasNext();
                }
                catch (IOException e2)
                {
                    System.err.println("Error retrieving PubMed documents.");
                    logStackTrace(e2);
                    System.exit(1);
                }
                catch (CollectionException e2)
                {
                    System.err.println("Error retrieving PubMed documents.");
                    logStackTrace(e2);
                    System.exit(1);
                }
                continue;
            }
            catch (IOException e)
            {
                logStackTrace(e);
                try
                {
                    hasNext = cr.hasNext();
                }
                catch (IOException e2)
                {
                    System.err.println("Error retrieving PubMed documents.");
                    logStackTrace(e);
                    System.exit(1);
                }
                catch (CollectionException e2)
                {
                    System.err.println("Error retrieving PubMed documents.");
                    logStackTrace(e);
                    System.exit(1);
                }
                continue;
            }
            catch (RuntimeException e)
            {
                logStackTrace(e);
                try
                {
                    hasNext = cr.hasNext();
                }
                catch (IOException e2)
                {
                    System.err.println("Error retrieving PubMed documents.");
                    logStackTrace(e2);
                    System.exit(1);
                }
                catch (CollectionException e2)
                {
                    System.err.println("Error retrieving PubMed documents.");
                    logStackTrace(e2);
                    System.exit(1);
                }
                continue;
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
                    continue;
                }

                try
                {
                    String pmid = JCasUtils.getPubMedID(cas.getJCas());
                    File tempFile = new File(tempDir.getAbsolutePath() + "/" + pmid + ".ser");
                    tempFile.deleteOnExit();
                    OutputStream out = new BufferedOutputStream(
                            new FileOutputStream(tempFile));
                    Serialization.serializeCAS(cas, out);
                    out.close();
                }
                catch(IOException e)
                {
                    System.err.println("Error storing annotated documents.");
                    logStackTrace(e);
                    continue;
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
                    System.out.print("Progress: " + (int)java.lang.Math.floor(progress) + "%\r");
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

        List<BiochemicalReaction> mdkReactions = null;
        try
        {
            mdkReactions = Converter.convertUIMAReactionsToMDK(tempDir.listFiles(), ae, sections, currencyMolecules);
        }
        catch (IOException e)
        {
            System.err.println("Can't find temp directory.");
            logStackTrace(e);
            System.exit(1);
        }
        catch (ResourceInitializationException e)
        {
            System.err.println("Error reading annotations.");
            logStackTrace(e);
            System.exit(1);
        }
        catch (CASException e)
        {
            System.err.println("Error reading annotations.");
            logStackTrace(e);
            System.exit(1);
        }

        EntityFactory entityFactory = DefaultEntityFactory.getInstance();

        List<MetabolicReaction> combinedReactions = ReactionCombiner.combineReactions(
                mdkReactions, currencyMolecules, entityFactory);
        BKMDB bkmDB = null;
        try
        {
            bkmDB = new BKMDB(data);
        }
        catch (BKMException e)
        {
            System.err.println("Error reading internal BKM-React database.");
            logStackTrace(e);
            System.exit(1);
        }

        try
        {
            CompoundAnnotator.annotateReactions(combinedReactions, bkmDB, currencyMolecules);
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
