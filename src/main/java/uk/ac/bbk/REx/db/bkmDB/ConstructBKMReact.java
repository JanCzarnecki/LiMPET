package uk.ac.bbk.REx.db.bkmDB;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import au.com.bytecode.opencsv.CSVReader;


public class ConstructBKMReact
{
    private static Pattern multPatt = Pattern.compile("^(\\d+|n)\\s+");

    public static void main(String[] args) throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException
    {
        Class.forName("org.apache.derby.jdbc.EmbeddedDriver").newInstance();
        Connection con = DriverManager.getConnection("jdbc:derby:data/db/bkm;create=true");

        con.prepareStatement("DROP TABLE brendaReactions").executeUpdate();
        con.prepareStatement(
                "CREATE TABLE brendaReactions " +
                        "(" +
                        "id INT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1)," +
                        "reactionID INT NOT NULL, " +
                        "brendaID VARCHAR(20), " +
                        "PRIMARY KEY (id)" +
                        ")").execute();
        con.prepareStatement("CREATE INDEX reactionIDBrenda ON brendaReactions(reactionID)").execute();
        con.prepareStatement("CREATE INDEX brendaID ON brendaReactions(brendaID)").execute();

        con.prepareStatement("DROP TABLE metacycReactions").executeUpdate();
        con.prepareStatement(
                "CREATE TABLE metacycReactions " +
                        "(" +
                        "id INT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1)," +
                        "reactionID INT NOT NULL, " +
                        "metacycID VARCHAR(100), " +
                        "PRIMARY KEY (id)" +
                        ")").execute();
        con.prepareStatement("CREATE INDEX reactionIDMetacyc ON metacycReactions(reactionID)").execute();
        con.prepareStatement("CREATE INDEX metacycID ON metacycReactions(metacycID)").execute();

        con.prepareStatement("DROP TABLE pathways").executeUpdate();
        con.prepareStatement(
                "CREATE TABLE pathways " +
                        "(" +
                        "pathwayID VARCHAR(50) NOT NULL, " +
                        "pathwayName VARCHAR(200) NOT NULL, " +
                        "PRIMARY KEY (pathwayID)" +
                        ")").execute();
        con.prepareStatement("CREATE INDEX pathwayIDPathways ON pathways(pathwayID)").execute();
        con.prepareStatement("CREATE INDEX pathwayName ON pathways(pathwayName)").execute();

        con.prepareStatement("DROP TABLE reactionsToPathways").executeUpdate();
        con.prepareStatement(
                "CREATE TABLE reactionsToPathways " +
                        "(" +
                        "id INT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1)," +
                        "reactionID INT NOT NULL, " +
                        "pathwayID VARCHAR(50) NOT NULL, " +
                        "PRIMARY KEY (id)" +
                        ")").execute();
        con.prepareStatement("CREATE INDEX reactionIDLinks ON reactionsToPathways(reactionID)").execute();
        con.prepareStatement("CREATE INDEX pathwayIDLinks ON reactionsToPathways(pathwayID)").execute();

        con.prepareStatement("DROP TABLE substrates").executeUpdate();
        con.prepareStatement(
                "CREATE TABLE substrates " +
                        "(" +
                        "id INT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1)," +
                        "reactionID INT NOT NULL," +
                        "compoundID INT NOT NULL," +
                        "PRIMARY KEY (id)" +
                        ")").execute();
        con.prepareStatement("CREATE INDEX reactionIDSub ON substrates(reactionID)").execute();
        con.prepareStatement("CREATE INDEX compoundIDSub ON substrates(compoundID)").execute();

        con.prepareStatement("DROP TABLE products").executeUpdate();
        con.prepareStatement(
                "CREATE TABLE products " +
                        "(" +
                        "id INT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1)," +
                        "reactionID INT NOT NULL," +
                        "compoundID INT NOT NULL," +
                        "PRIMARY KEY (id)" +
                        ")").execute();
        con.prepareStatement("CREATE INDEX reactionIDProd ON products(reactionID)").execute();
        con.prepareStatement("CREATE INDEX compoundIDProd ON products(compoundID)").execute();

        con.prepareStatement("DROP TABLE compounds").executeUpdate();
        con.prepareStatement(
                "CREATE TABLE compounds " +
                        "(" +
                        "id INT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1)," +
                        "compoundID INT NOT NULL," +
                        "inchi VARCHAR(10000) NOT NULL," +
                        "PRIMARY KEY (id)" +
                        ")").execute();
        con.prepareStatement("CREATE INDEX compoundID ON compounds(compoundID)").execute();
        con.prepareStatement("CREATE INDEX inchi ON compounds(inchi)").execute();

        File file;
        CSVReader reader;
        String[] nextLine;
        int count;
        long fileSize;

        PreparedStatement insertBrendaReactionStmt = con.prepareStatement(
                "INSERT INTO brendaReactions(reactionID,brendaID) " +
                        "VALUES(?,?)");
        PreparedStatement insertMetacycReactionStmt = con.prepareStatement(
                "INSERT INTO metacycReactions(reactionID,metacycID) " +
                        "VALUES(?,?)");
        PreparedStatement pathwayCountStmt = con.prepareStatement(
                "SELECT COUNT(pathwayID) AS count " +
                        "FROM pathways " +
                        "WHERE pathwayID=?");
        PreparedStatement insertPathwayStmt = con.prepareStatement(
                "INSERT INTO pathways(pathwayID,pathwayName) " +
                        "VALUES(?,?)");
        PreparedStatement insertReactionToPathwayStmt = con.prepareStatement(
                "INSERT INTO reactionsToPathways(reactionID,pathwayID) " +
                        "VALUES(?,?)");

        file = new File("/media/data/bioinformatics/bkmreact/Reactions_KMB_unique.csv");
        reader = new CSVReader(new FileReader(file));
        count = 0;
        fileSize = file.length();
        while ((nextLine = reader.readNext()) != null)
        {
            // nextLine[] is an array of values from the line
            int reactionID = Integer.parseInt(nextLine[0]);
            String brendaReactionsString = nextLine[2];
            String metacycReactionsString = nextLine[4];
            if(brendaReactionsString.equals("NULL")
                    && metacycReactionsString.equals("NULL"))
            {
                continue;
            }

            if(!brendaReactionsString.equals("NULL"))
            {
                List<String> brendaReactions = Arrays.asList(brendaReactionsString.split(","));

                for(String brendaReaction : brendaReactions)
                {
                    insertBrendaReactionStmt.setInt(1, reactionID);
                    insertBrendaReactionStmt.setString(2, brendaReaction);
                    insertBrendaReactionStmt.executeUpdate();
                }
            }

            if(!metacycReactionsString.equals("NULL"))
            {
                List<String> metacycReactions = Arrays.asList(metacycReactionsString.split(","));

                for(String metacycReaction : metacycReactions)
                {
                    insertMetacycReactionStmt.setInt(1, reactionID);
                    insertMetacycReactionStmt.setString(2, metacycReaction);
                    insertMetacycReactionStmt.executeUpdate();
                }
            }

            String metaCycPathwayIDsString = nextLine[8];
            String metaCycPathwayNamesString = nextLine[10];
            if(!metaCycPathwayIDsString.equals(""))
            {
                List<String> metacycPathwayIDs = Arrays.asList(metaCycPathwayIDsString.split("; "));
                List<String> metacycPathwayNames = Arrays.asList(metaCycPathwayNamesString.split("; "));

                for(int i=0; i<metacycPathwayIDs.size(); i++)
                {
                    pathwayCountStmt.setString(1, metacycPathwayIDs.get(i));
                    ResultSet pathwayCountRS = pathwayCountStmt.executeQuery();
                    pathwayCountRS.next();
                    int pathwayCount = pathwayCountRS.getInt("count");

                    if(pathwayCount == 0)
                    {
                        insertPathwayStmt.setString(1, metacycPathwayIDs.get(i));
                        insertPathwayStmt.setString(2, metacycPathwayNames.get(i));
                        insertPathwayStmt.executeUpdate();
                    }

                    insertReactionToPathwayStmt.setInt(1, reactionID);
                    insertReactionToPathwayStmt.setString(2, metacycPathwayIDs.get(i));
                    insertReactionToPathwayStmt.executeUpdate();
                }
            }

            count = count + nextLine.length + 1;
            double percentDone = ((double)count/(double)fileSize) * 100.0;
            System.out.println("1: " + percentDone);
        }
        reader.close();

        PreparedStatement insertCompoundStmt = con.prepareStatement(
                "INSERT INTO compounds(compoundID,inchi) " +
                        "VALUES(?,?)");

        file = new File("/media/data/bioinformatics/bkmreact/KMB_Names_IDs_InChIs_Sources.csv");
        reader = new CSVReader(new FileReader(file));
        count = 0;
        fileSize = file.length();
        while ((nextLine = reader.readNext()) != null)
        {
            int compoundID = Integer.parseInt(nextLine[0]);
            String inchi = nextLine[3].replaceAll("\\/[ptmsq].+", "");

            insertCompoundStmt.setInt(1, compoundID);
            insertCompoundStmt.setString(2, inchi);
            insertCompoundStmt.executeUpdate();

            count = count + nextLine.length + 1;
            double percentDone = ((double)count/(double)fileSize) * 100.0;
            System.out.println("2: " + percentDone);
        }
        reader.close();

        PreparedStatement insertSubstrateStmt = con.prepareStatement(
                "INSERT INTO substrates(reactionID,compoundID) " +
                        "VALUES(?,?)");
        PreparedStatement insertProductStmt = con.prepareStatement(
                "INSERT INTO products(reactionID,compoundID) " +
                        "VALUES(?,?)");
        file = new File("/media/data/bioinformatics/bkmreact/Link_ReactionID_CompoundID_SorP.csv");
        reader = new CSVReader(new FileReader(file));
        count = 0;
        fileSize = file.length();
        while ((nextLine = reader.readNext()) != null)
        {
            // nextLine[] is an array of values from the line
            if(nextLine[4].equals("S")
                    && !nextLine[5].equals("KEGG"))
            {
                insertSubstrateStmt.setInt(1, Integer.parseInt(nextLine[0]));
                insertSubstrateStmt.setInt(2, Integer.parseInt(nextLine[1]));
                insertSubstrateStmt.executeUpdate();
            }
            else if(nextLine[4].equals("P")
                    && !nextLine[5].equals("KEGG"))
            {
                insertProductStmt.setInt(1, Integer.parseInt(nextLine[0]));
                insertProductStmt.setInt(2, Integer.parseInt(nextLine[1]));
                insertProductStmt.executeUpdate();
            }

            count = count + nextLine.length + 1;
            double percentDone = ((double)count/(double)fileSize) * 100.0;
            System.out.println("3: " + percentDone);
        }
        reader.close();

    }
}
