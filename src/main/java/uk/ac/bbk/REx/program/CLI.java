package uk.ac.bbk.REx.program;

import org.apache.commons.cli.*;
import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class CLI
{
    private static final Logger LOGGER = Logger.getLogger(CLI.class.getName());

    public static void main(String[] args)
    {
        System.setProperty("java.util.logging.config.file", "/uk/ac/bbk/REx/settings/log.properties");
        LogManager logMan=LogManager.getLogManager();

        try
        {
            logMan.readConfiguration(CLI.class.getResourceAsStream("/uk/ac/bbk/REx/settings/log.properties"));
        }
        catch(IOException e)
        {
            System.err.println("Unable to read logging configuration file. The program will continue, but may not be " +
                    "logged correctly.");
            logStackTrace(e);
        }

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
            logStackTrace(e);
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

        if(!cmd.hasOption("m"))
        {
            System.err.println("The mode (option m) must be provided.");
            LOGGER.log(Level.INFO, "The mode was not provided. Exiting.");
            System.exit(1);
        }

        if(cmd.getOptionValue("m").equals("extraction"))
        {
            String inputFile;
            if(cmd.hasOption("i"))
            {
                inputFile = cmd.getOptionValue("i");
            }
            else
            {
                System.err.println("An input SBML file (option i) must be provided.");
                LOGGER.log(Level.INFO, "The input SBML file was not provided. Exiting.");
                System.exit(1);
                return;
            }

            int maxReturn;
            if(cmd.hasOption("n"))
            {
                maxReturn = Integer.parseInt(cmd.getOptionValue("n"));
            }
            else
            {
                maxReturn = 10;
                LOGGER.log(Level.INFO, "No maximum number of documents to return provided. " +
                        "The default maximum of 10 will be used.");
            }

            String speciesID;
            if(cmd.hasOption("s"))
            {
                speciesID = cmd.getOptionValue("s");
            }
            else
            {
                System.err.println("A species of interest (option s) must be provided.");
                LOGGER.log(Level.INFO, "A species of interest was not provided. Exiting.");
                System.exit(1);
                return;
            }

            String speciesNameFile;
            if(cmd.hasOption("sn"))
            {
                speciesNameFile = cmd.getOptionValue("sn");
            }
            else
            {
                speciesNameFile = null;
                LOGGER.log(Level.INFO, "No species names have been provided. " +
                        "No names will be added to the species name dictionary.");
            }

            String queriesFile;
            if(cmd.hasOption("q"))
            {
                queriesFile = cmd.getOptionValue("q");
            }
            else
            {
                queriesFile = null;
                LOGGER.log(Level.INFO, "No extra queries provided.");
            }

            String pmidCutoffsFile;
            if(cmd.hasOption("p"))
            {
                pmidCutoffsFile = cmd.getOptionValue("p");
            }
            else
            {
                pmidCutoffsFile = null;
                LOGGER.log(Level.INFO, "No file provided to save article cut-off levels. " +
                        "The cut-off levels will not be saved.");
            }

            String outputFile;
            if(cmd.hasOption("o"))
            {
                outputFile = cmd.getOptionValue("o");
            }
            else
            {
                System.err.println("The output file (option o) must be provided.");
                LOGGER.log(Level.INFO, "An output file was not provided. Exiting.");
                System.exit(1);
                return;
            }

            Methods.extraction(inputFile,
                    maxReturn,
                    speciesID,
                    speciesNameFile,
                    queriesFile,
                    pmidCutoffsFile,
                    outputFile);
        }
        else if(cmd.getOptionValue("m").equals("alternativePathwayRelevance"))
        {
            String inputFile;
            if(cmd.hasOption("i"))
            {
                inputFile = cmd.getOptionValue("i");
            }
            else
            {
                System.err.println("An input SBML file (option i) must be provided.");
                LOGGER.log(Level.INFO, "The input SBML file was not provided. Exiting.");
                System.exit(1);
                return;
            }

            String outputFile;
            if(cmd.hasOption("o"))
            {
                outputFile = cmd.getOptionValue("o");
            }
            else
            {
                System.err.println("The output file (option o) must be provided.");
                LOGGER.log(Level.INFO, "An output file was not provided. Exiting.");
                System.exit(1);
                return;
            }

            Methods.alternativePathwayRelevance(inputFile, outputFile);
        }
        else if(cmd.getOptionValue("m").equals("pathwayLinkRelevance"))
        {
            String inputFile;
            if(cmd.hasOption("i"))
            {
                inputFile = cmd.getOptionValue("i");
            }
            else
            {
                System.err.println("An input SBML file (option i) must be provided.");
                LOGGER.log(Level.INFO, "The input SBML file was not provided. Exiting.");
                System.exit(1);
                return;
            }

            String pathwaysFile;
            if(cmd.hasOption("p"))
            {
                pathwaysFile = cmd.getOptionValue("p");
            }
            else
            {
                System.err.println("A file containing pathways of interest (option p) must be provided.");
                LOGGER.log(Level.INFO, "The file of pathways was not provided. Exiting.");
                System.exit(1);
                return;
            }

            String outputFile;
            if(cmd.hasOption("o"))
            {
                outputFile = cmd.getOptionValue("o");
            }
            else
            {
                System.err.println("The output file (option o) must be provided.");
                LOGGER.log(Level.INFO, "An output file was not provided. Exiting.");
                System.exit(1);
                return;
            }

            Methods.pathwayLinkRelevance(inputFile, pathwaysFile, outputFile);
        }
        else if(cmd.getOptionValue("m").equals("continuousTest"))
        {
            String inputFile;
            if(cmd.hasOption("i"))
            {
                inputFile = cmd.getOptionValue("i");
            }
            else
            {
                System.err.println("An input SBML file (option i) must be provided.");
                LOGGER.log(Level.INFO, "The input SBML file was not provided. Exiting.");
                System.exit(1);
                return;
            }

            String seedFile;
            if(cmd.hasOption("s"))
            {
                seedFile = cmd.getOptionValue("s");
            }
            else
            {
                System.err.println("The seed SBML file (option s) must be provided.");
                LOGGER.log(Level.INFO, "The seed SBML file was not provided. Exiting.");
                System.exit(1);
                return;
            }

            String expectedFile;
            if(cmd.hasOption("e"))
            {
                expectedFile = cmd.getOptionValue("e");
            }
            else
            {
                System.err.println("An SBML file of expected reactions (option e) must be provided.");
                LOGGER.log(Level.INFO, "The SBML file of expected reactions was not provided. Exiting.");
                System.exit(1);
                return;
            }

            String pmidCutoffsFile;
            if(cmd.hasOption("ic"))
            {
                pmidCutoffsFile = cmd.getOptionValue("ic");
            }
            else
            {
                System.err.println("An PubMed article cut-offs (option ic) must be provided.");
                LOGGER.log(Level.INFO, "The PubMed article cut-offs were not provided. Exiting.");
                System.exit(1);
                return;
            }

            String outputFile;
            if(cmd.hasOption("o"))
            {
                outputFile = cmd.getOptionValue("o");
            }
            else
            {
                System.err.println("The output file (option o) must be provided.");
                LOGGER.log(Level.INFO, "An output file was not provided. Exiting.");
                System.exit(1);
                return;
            }

            Methods.continuousTest(inputFile,
                    seedFile,
                    expectedFile,
                    pmidCutoffsFile,
                    outputFile);
        }
        else if(cmd.getOptionValue("m").equals("evaluate"))
        {
            String inputFile;
            if(cmd.hasOption("i"))
            {
                inputFile = cmd.getOptionValue("i");
            }
            else
            {
                System.err.println("An input SBML file (option i) must be provided.");
                LOGGER.log(Level.INFO, "The input SBML file was not provided. Exiting.");
                System.exit(1);
                return;
            }

            String expectedFile;
            if(cmd.hasOption("e"))
            {
                expectedFile = cmd.getOptionValue("e");
            }
            else
            {
                System.err.println("An SBML file of expected reactions (option e) must be provided.");
                LOGGER.log(Level.INFO, "The SBML file of expected reactions was not provided. Exiting.");
                System.exit(1);
                return;
            }

            String outputFile;
            if(cmd.hasOption("o"))
            {
                outputFile = cmd.getOptionValue("o");
            }
            else
            {
                System.err.println("The output file (option o) must be provided.");
                LOGGER.log(Level.INFO, "An output file was not provided. Exiting.");
                System.exit(1);
                return;
            }

            Methods.evaluate(inputFile, expectedFile, outputFile);
        }
        else if(cmd.getOptionValue("m").equals("clearCache"))
        {
            Methods.clearCache();
        }
        else
        {
            System.err.println("The mode was not recognised.");
            LOGGER.log(Level.INFO, "Mode " + cmd.getOptionValue("m") + " not recognised.");
            System.exit(1);
            return;
        }
    }

    private static void logStackTrace(Throwable e)
    {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        LOGGER.log(Level.INFO, sw.toString());
    }
}
