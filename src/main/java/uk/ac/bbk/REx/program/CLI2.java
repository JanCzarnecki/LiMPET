package uk.ac.bbk.REx.program;

import org.apache.commons.cli.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import uk.ac.bbk.REx.exception.BKMException;
import uk.ac.bbk.REx.utils.Files;

import java.io.*;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class CLI2
{
    private static final Logger LOGGER = Logger.getLogger(CLI2.class.getName());

    public static void main(String[] args) throws ParseException
    {
        System.setProperty("java.util.logging.config.file", "/uk/ac/bbk/REx/settings/log.properties");
        LogManager logMan=LogManager.getLogManager();

        try
        {
            logMan.readConfiguration(CLI2.class.getResourceAsStream("/uk/ac/bbk/REx/settings/log.properties"));
        }
        catch(IOException e)
        {
            System.err.println("Unable to read logging configuration file. The program will continue, but may not be " +
                    "logged correctly.");
            logStackTrace(e);
        }

        System.out.println(String.format("Logging to %s/%s", System.getProperty("java.io.tmpdir"), "rex.log"));

        InputStream commandsStream = new BufferedInputStream(CLI2.class.getResourceAsStream("commands.json"));
        JSONTokener tokener = new JSONTokener(commandsStream);
        JSONArray commandsArray = new JSONArray(tokener);

        Options commands = new Options();
        Map<String, Option> commandsMap = new LinkedHashMap<String, Option>();
        Map<String, Options> commandNamesWithOptions = new LinkedHashMap<String, Options>();

        for(int i=0; i<commandsArray.length(); i++)
        {
            JSONObject commandObject = commandsArray.getJSONObject(i);
            Option command = OptionBuilder
                    .withLongOpt(commandObject.getString("longOpt"))
                    .withDescription(commandObject.getString("description"))
                    .create();
            commands.addOption(command);
            commandsMap.put(command.getLongOpt(), command);

            Options options = new Options();
            JSONArray optionsArray = commandObject.getJSONArray("options");
            for(int j=0; j<optionsArray.length(); j++)
            {
                JSONObject optionObject = optionsArray.getJSONObject(j);

                if(optionObject.has("argName"))
                {
                    options.addOption(
                            OptionBuilder
                                    .withLongOpt(optionObject.getString("longOpt"))
                                    .hasArg()
                                    .withArgName(optionObject.getString("argName"))
                                    .withDescription(optionObject.getString("description"))
                                    .create(optionObject.getString("shortOpt"))
                    );
                }
                else
                {
                    options.addOption(
                            OptionBuilder
                                    .withLongOpt(optionObject.getString("longOpt"))
                                    .withDescription(optionObject.getString("description"))
                                    .create(optionObject.getString("shortOpt"))
                    );
                }
            }

            commandNamesWithOptions.put(command.getLongOpt(), options);
        }

        CommandLine commandsCLI = new ExtendedParser(true).parse(commands, args);

        if(commandsCLI.hasOption("metaCycDownload"))
        {
            CommandLine cmd = new ExtendedParser(true).parse(commandNamesWithOptions.get("metaCycDownload"), args);

            String pathwayIDs;
            if(cmd.hasOption("p"))
            {
                pathwayIDs = cmd.getOptionValue("p");
            }
            else
            {
                System.err.println("A list of pathway IDs (option p) must be specified.");
                LOGGER.log(Level.INFO, "No pathway IDs were provided. Exiting.");
                System.exit(1);
                return;
            }

            String outputDir;
            if(cmd.hasOption("o"))
            {
                outputDir = cmd.getOptionValue("o");
            }
            else
            {
                System.err.println("An output file (option o) must be specified.");
                LOGGER.log(Level.INFO, "The output file was not provided. Exiting.");
                System.exit(1);
                return;
            }

            Methods.metaCycDownload(pathwayIDs, outputDir);
        }
        else if(commandsCLI.hasOption("seedQueries"))
        {
            CommandLine cmd = new ExtendedParser(true).parse(commandNamesWithOptions.get("seedQueries"), args);

            String inputFile;
            if(cmd.hasOption("p"))
            {
                inputFile = cmd.getOptionValue("p");
            }
            else
            {
                System.err.println("An input SBML file (option p) must be provided.");
                LOGGER.log(Level.INFO, "The input SBML file was not provided. Exiting.");
                System.exit(1);
                return;
            }

            String species;
            if(cmd.hasOption("s"))
            {
                species = cmd.getOptionValue("s");
            }
            else
            {
                System.err.println("The target species must be provided.");
                LOGGER.log(Level.INFO, "The target species was not provided. Exiting.");
                System.exit(1);
                return;
            }

            String speciesNames;
            if(cmd.hasOption("n"))
            {
                speciesNames = cmd.getOptionValue("n");
            }
            else
            {
                speciesNames = null;
            }

            String outputFile;
            if(cmd.hasOption("o"))
            {
                outputFile = cmd.getOptionValue("o");
            }
            else
            {
                System.err.println("An output file (option o) must be specified.");
                LOGGER.log(Level.INFO, "The output file was not provided. Exiting.");
                System.exit(1);
                return;
            }

            File userdata;
            if(cmd.hasOption("d"))
            {
                userdata = new File(cmd.getOptionValue("d"));
            }
            else
            {
                try
                {
                    userdata = new File(Files.getParentDirectory(), "data/userdata");
                }
                catch (UnsupportedEncodingException e)
                {
                    System.err.println("Unable to read userdata directory.");
                    LOGGER.log(Level.INFO, "Unable to read userdata directory.");
                    System.exit(1);
                    return;
                }
            }

            Methods.seedQueries(inputFile, species, speciesNames, outputFile, userdata);
        }
        else if(commandsCLI.hasOption("downloadArticles"))
        {
            CommandLine cmd = new ExtendedParser(true).parse(commandNamesWithOptions.get("downloadArticles"), args);

            String queriesFile;
            if(cmd.hasOption("q"))
            {
                queriesFile = cmd.getOptionValue("q");
            }
            else
            {
                System.err.println("A queries file (option q) must be specified.");
                LOGGER.log(Level.INFO, "The queries file was not provided. Exiting.");
                System.exit(1);
                return;
            }

            int numOfArticles;
            if(cmd.hasOption("n"))
            {
                try
                {
                    numOfArticles = Integer.parseInt(cmd.getOptionValue("n"));
                }
                catch(NumberFormatException e)
                {
                    System.err.println("Option n must be an integer.");
                    LOGGER.log(Level.INFO, "Option n not an integer.");
                    System.exit(1);
                    return;
                }
            }
            else
            {
                numOfArticles = 20;
                System.out.println("The maximum number of articles to retrieve for each query" +
                        "was not specified. The default of 20 will be used.");
            }

            String outputDir;
            if(cmd.hasOption("o"))
            {
                outputDir = cmd.getOptionValue("o");
            }
            else
            {
                System.err.println("An output directory (option o) must be specified.");
                LOGGER.log(Level.INFO, "The output directory was not provided. Exiting.");
                System.exit(1);
                return;
            }

            Methods.downloadArticles(queriesFile, numOfArticles, outputDir);
        }
        else if(commandsCLI.hasOption("extraction"))
        {
            CommandLine cmd = new ExtendedParser(true).parse(commandNamesWithOptions.get("extraction"), args);

            String articlesDir;
            if(cmd.hasOption("a"))
            {
                articlesDir = cmd.getOptionValue("a");
            }
            else
            {
                System.err.println("A directory of articles (option a) containing text to mine must be provided.");
                LOGGER.log(Level.INFO, "The directory of articles was not provided. Exiting.");
                System.exit(1);
                return;
            }

            String speciesID = null;
            String speciesNames = null;
            if(cmd.hasOption("s") && cmd.hasOption("sn"))
            {
                speciesID = cmd.getOptionValue("s");
                speciesNames = cmd.getOptionValue("sn");
            }

            String scoresFile;
            if(cmd.hasOption("c"))
            {
                scoresFile = cmd.getOptionValue("c");
            }
            else
            {
                scoresFile = null;
            }

            String outputDir;
            if(cmd.hasOption("o"))
            {
                outputDir = cmd.getOptionValue("o");
            }
            else
            {
                System.err.println("The output file (option o) must be provided.");
                LOGGER.log(Level.INFO, "An output file was not provided. Exiting.");
                System.exit(1);
                return;
            }

            File userdata;
            if(cmd.hasOption("d"))
            {
                userdata = new File(cmd.getOptionValue("d"));
            }
            else
            {
                try
                {
                    userdata = new File(Files.getParentDirectory(), "data/userdata");
                }
                catch (UnsupportedEncodingException e)
                {
                    System.err.println("Unable to read userdata directory.");
                    LOGGER.log(Level.INFO, "Unable to read userdata directory.");
                    System.exit(1);
                    return;
                }
            }

            Methods.extractionFromDir(articlesDir,
                    speciesID,
                    speciesNames,
                    scoresFile,
                    outputDir,
                    userdata);
        }
        else if(commandsCLI.hasOption("scorePathway"))
        {
            CommandLine cmd = new ExtendedParser(true).parse(commandNamesWithOptions.get("scorePathway"), args);

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

            String speciesID;
            if(cmd.hasOption("sp"))
            {
                speciesID = cmd.getOptionValue("sp");
            }
            else
            {
                System.err.println("A species of interest (option s) must be provided.");
                LOGGER.log(Level.INFO, "A species of interest was not provided. Exiting.");
                System.exit(1);
                return;
            }

            String seedFile;
            if(cmd.hasOption("p"))
            {
                seedFile = cmd.getOptionValue("p");
            }
            else
            {
                System.err.println("The seed file (option p) must be provided.");
                LOGGER.log(Level.INFO, "A seed file was not provided. Exiting.");
                System.exit(1);
                return;
            }

            String pathwayIDs;
            if(cmd.hasOption("pid"))
            {
                pathwayIDs = cmd.getOptionValue("pid");
            }
            else
            {
                pathwayIDs = null;
            }

            String scoresFile;
            if(cmd.hasOption("s"))
            {
                scoresFile = cmd.getOptionValue("s");
            }
            else
            {
                scoresFile = null;
            }

            File userdata;
            if(cmd.hasOption("d"))
            {
                userdata = new File(cmd.getOptionValue("d"));
            }
            else
            {
                try
                {
                    userdata = new File(Files.getParentDirectory(), "data/userdata");
                }
                catch (UnsupportedEncodingException e)
                {
                    System.err.println("Unable to read userdata directory.");
                    LOGGER.log(Level.INFO, "Unable to read userdata directory.");
                    System.exit(1);
                    return;
                }
            }

            Methods.scorePathway(inputFile, speciesID, pathwayIDs, outputFile, seedFile, scoresFile, userdata);
        }
        else if(commandsCLI.hasOption("cytoscape"))
        {
            CommandLine cmd = new ExtendedParser(true).parse(commandNamesWithOptions.get("cytoscape"), args);

            String inputFile;
            if(cmd.hasOption("p"))
            {
                inputFile = cmd.getOptionValue("p");
            }
            else
            {
                System.err.println("A pathway (option p) must be provided.");
                LOGGER.log(Level.INFO, "The input SBML file was not provided. Exiting.");
                System.exit(1);
                return;
            }

            String outputDir;
            if(cmd.hasOption("o"))
            {
                outputDir = cmd.getOptionValue("o");
            }
            else
            {
                System.err.println("An output directory (option o) must be specified.");
                LOGGER.log(Level.INFO, "The output directory was not provided. Exiting.");
                System.exit(1);
                return;
            }

            Methods.convertToCytoscapeTSV(inputFile, outputDir);
        }
        else if(commandsCLI.hasOption("humanOutput"))
        {
            CommandLine cmd = new ExtendedParser(true).parse(commandNamesWithOptions.get("humanOutput"), args);

            String inputFile;
            if(cmd.hasOption("p"))
            {
                inputFile = cmd.getOptionValue("p");
            }
            else
            {
                System.err.println("A pathway (option p) must be provided.");
                LOGGER.log(Level.INFO, "The input SBML file was not provided. Exiting.");
                System.exit(1);
                return;
            }

            double extractionThreshold;
            if(cmd.hasOption("t"))
            {
                extractionThreshold = Double.parseDouble(cmd.getOptionValue("t"));
            }
            else
            {
                System.err.println("An extraction threshold (option t) must be provided.");
                LOGGER.log(Level.INFO, "An extraction threshold was not provided. Exiting.");
                System.exit(1);
                return;
            }

            String output;
            if(cmd.hasOption("o"))
            {
                output = cmd.getOptionValue("o");
            }
            else
            {
                System.err.println("An output file (option o) must be specified.");
                LOGGER.log(Level.INFO, "The output file was not provided. Exiting.");
                System.exit(1);
                return;
            }

            Methods.convertToHumanTSV(inputFile, extractionThreshold, output);
        }
        else if(commandsCLI.hasOption("rescoreExtraction"))
        {
            CommandLine cmd = new ExtendedParser(true).parse(commandNamesWithOptions.get("rescoreExtraction"), args);

            String inputFile;
            if(cmd.hasOption("p"))
            {
                inputFile = cmd.getOptionValue("p");
            }
            else
            {
                System.err.println("A pathway (option p) must be provided.");
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
                System.err.println("A seed pathway (option s) must be provided.");
                LOGGER.log(Level.INFO, "The seed SBML file was not provided. Exiting.");
                System.exit(1);
                return;
            }

            String seedName;
            if(cmd.hasOption("n"))
            {
                seedName = cmd.getOptionValue("n");
            }
            else
            {
                System.err.println("The name of the seed pathway (option n) must be provided.");
                LOGGER.log(Level.INFO, "The name of the seed pathway was not provided. Exiting.");
                System.exit(1);
                return;
            }

            String scoresFile;
            if(cmd.hasOption("c"))
            {
                scoresFile = cmd.getOptionValue("c");
            }
            else
            {
                System.err.println("The scores file (option c) must be provided.");
                LOGGER.log(Level.INFO, "The scores file was not provided. Exiting.");
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
                System.err.println("The output file (option o) must be specified.");
                LOGGER.log(Level.INFO, "The output file was not provided. Exiting.");
                System.exit(1);
                return;
            }

            File userdata;
            if(cmd.hasOption("d"))
            {
                userdata = new File(cmd.getOptionValue("d"));
            }
            else
            {
                try
                {
                    userdata = new File(Files.getParentDirectory(), "data/userdata");
                }
                catch (UnsupportedEncodingException e)
                {
                    System.err.println("Unable to read userdata directory.");
                    LOGGER.log(Level.INFO, "Unable to read userdata directory.");
                    System.exit(1);
                    return;
                }
            }

            Methods.rescoreExtraction(inputFile, seedFile, seedName, scoresFile, outputFile, userdata);
        }
        else
        {
            StringWriter stringWriter = new StringWriter();
            PrintWriter helpWriter = new PrintWriter(stringWriter);
            HelpFormatter helpFormatter = new HelpFormatter();

            int width = 100;
            int leftPad = 5;
            int descPad = 3;

            helpWriter.write("Usage: java -jar REx.jar <command> <options>\n\n");

            helpWriter.write("The commands and options are described below in the following format:\n");
            helpWriter.write("<command> : <description>\n     <option> <description>\n     <option> <description>\n\n");

            for(String commandName : commandsMap.keySet())
            {
                Option command = commandsMap.get(commandName);
                helpWriter.write(String.format("\n--%s : %s\n", command.getLongOpt(), command.getDescription()));
                helpFormatter.printOptions(
                        helpWriter, width, commandNamesWithOptions.get(commandName), leftPad, descPad);
            }

            System.out.println(stringWriter.toString());
        }
    }

    private static void logStackTrace(Throwable e)
    {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        LOGGER.log(Level.INFO, sw.toString());
    }
}
