package uk.ac.bbk.REx.db.chebiDB;

import java.io.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class ConstructCHEBIDB
{
    public static void main(String[] args) throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, IOException
    {
        Class.forName("org.apache.derby.jdbc.EmbeddedDriver").newInstance();
        Connection con = DriverManager.getConnection("jdbc:derby:data/db/chebi");
        con.setAutoCommit(false);
        Scanner sc;
        Pattern stereoPatt = Pattern.compile("(\\A|\\W)[dl](-)");

        con.prepareStatement("DROP TABLE compounds").executeUpdate();
        con.prepareStatement(
                "CREATE TABLE compounds " +
                        "(" +
                        "chebiID INT NOT NULL," +
                        "parentID INT," +
                        "inchi VARCHAR(10000)," +
                        "PRIMARY KEY (chebiID)" +
                        ")").execute();
        con.prepareStatement("CREATE INDEX chebiID ON compounds(chebiID)").execute();
        con.prepareStatement("CREATE INDEX parentID ON compounds(parentID)").execute();
        con.prepareStatement("CREATE INDEX inchi ON compounds(inchi)").execute();

        PreparedStatement compoundsParentStmt = con.prepareStatement("INSERT INTO compounds(chebiID, parentID) VALUES (?,?)");
        PreparedStatement compoundsNoParentStmt = con.prepareStatement("INSERT INTO compounds(chebiID) VALUES (?)");

        con.prepareStatement("DROP TABLE variations").executeUpdate();
        con.prepareStatement(
                "CREATE TABLE variations " +
                        "(" +
                        "id INT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1)," +
                        "chebiID INT NOT NULL," +
                        "exact VARCHAR(5000) NOT NULL," +
                        "squares VARCHAR(5000) NOT NULL," +
                        "spaces VARCHAR(5000) NOT NULL," +
                        "nonWordChars VARCHAR(5000) NOT NULL," +
                        "stereo VARCHAR(5000) NOT NULL," +
                        "everythingButLetters VARCHAR(5000) NOT NULL," +
                        "PRIMARY KEY (id)" +
                        ")").execute();
        con.prepareStatement("CREATE INDEX id ON variations(id)").execute();
        con.prepareStatement("CREATE INDEX chebiID ON variations(chebiID)").execute();
        con.prepareStatement("CREATE INDEX exact ON variations(exact)").execute();
        con.prepareStatement("CREATE INDEX squares ON variations(squares)").execute();
        con.prepareStatement("CREATE INDEX spaces ON variations(spaces)").execute();
        con.prepareStatement("CREATE INDEX nonWordChars ON variations(nonWordChars)").execute();
        con.prepareStatement("CREATE INDEX stereo ON variations(stereo)").execute();
        con.prepareStatement("CREATE INDEX everythingButWordChars ON variations(everythingButLetters)").execute();

        PreparedStatement variationsStmt = con.prepareStatement("INSERT INTO variations(chebiID, exact, squares, spaces, nonWordChars, stereo, everythingButLetters)  VALUES (?,?,?,?,?,?,?)");

        PreparedStatement noInchiStmt = con.prepareStatement
                (
                        "SELECT chebiID " +
                                "FROM compounds " +
                                "WHERE inchi IS NULL"
                );

        PreparedStatement removeCompoundStmt = con.prepareStatement
                (
                        "DELETE FROM compounds " +
                                "WHERE chebiID=?"
                );

        PreparedStatement removeVariationStmt = con.prepareStatement
                (
                        "DELETE FROM variations " +
                                "WHERE chebiID=?"
                );

        File compounds = new File("/home/jan/Downloads/compounds.tsv");
        long compoundsSize = compounds.length();
        long compoundsCount = 0;

        sc = new Scanner(new BufferedReader(new FileReader(compounds)));
        sc.nextLine();

        while(sc.hasNextLine())
        {
            String line = sc.nextLine();
            String[] values = line.split("\t");

            if(values[4].equals("null"))
            {
                compoundsNoParentStmt.setInt(1, Integer.parseInt(values[0]));
                compoundsNoParentStmt.executeUpdate();
            }
            else
            {
                compoundsParentStmt.setInt(1, Integer.parseInt(values[0]));
                compoundsParentStmt.setInt(2, Integer.parseInt(values[4]));
                compoundsParentStmt.executeUpdate();
            }

            if(!values[5].equals("null"))
            {
                variationsStmt.setInt(1, Integer.parseInt(values[0]));
                computeVariations(Integer.parseInt(values[0]), values[5], variationsStmt, stereoPatt);
            }

            compoundsCount = compoundsCount + line.length() + 1;
            System.out.println("1: " + compoundsCount + "/" + compoundsSize);
        }
        con.commit();
        System.out.println("Done 1.");

        sc.close();

        File names = new File("/home/jan/Downloads/names.tsv");
        long namesSize = names.length();
        sc = new Scanner(new BufferedReader(new FileReader(names)));
        sc.nextLine();
        long namesCount = 0;

        while(sc.hasNextLine())
        {
            String line = sc.nextLine();
            String[] values = line.split("\t");

            variationsStmt.setInt(1, Integer.parseInt(values[1]));
            computeVariations(Integer.parseInt(values[1]), values[4], variationsStmt, stereoPatt);

            namesCount = namesCount + line.length() + 1;
            System.out.println("2: " + namesCount + "/" + namesSize);
        }
        con.commit();
        System.out.println("Done 2.");

        sc.close();

        /*
        Pattern acidPatt = Pattern.compile("^(.+)ate\\(\\d[+-]\\)$");
        PreparedStatement updateAcidStmt = con.prepareStatement("UPDATE variations SET chebiID=? WHERE chebiID=?");
        PreparedStatement getExactStmt = con.prepareStatement("SELECT * FROM variations WHERE exact=?");

        ResultSet allRS = con.prepareStatement("SELECT * FROM variations").executeQuery();

        while(allRS.next())
        {
            String exact = allRS.getString("exact");
            Matcher acidMat = acidPatt.matcher(exact);

            if(acidMat.find())
            {
                int oldID = allRS.getInt("chebiID");

                String base = acidMat.group(1);
                String newExact = base + "ate";

                getExactStmt.setString(1, newExact);
                ResultSet newExactRS = getExactStmt.executeQuery();
                if(newExactRS.next())
                {
                    int newID = newExactRS.getInt("chebiID");
                    updateAcidStmt.setInt(1, newID);
                    updateAcidStmt.setInt(2, oldID);
                    updateAcidStmt.executeUpdate();
                    //System.out.println(oldID + " -> " + newID);
                }
            }
        }
        */

        File inchiFile = new File("/home/jan/Downloads/chebiId_inchi.tsv");
        long inchiFileSize = inchiFile.length();
        long inchiFileCount = 0;
        PreparedStatement keggIDStmt = con.prepareStatement("UPDATE compounds SET inchi=? WHERE chebiID=? OR parentID=?");

        sc = new Scanner(new BufferedReader(new FileReader(inchiFile)));
        sc.nextLine();

        while(sc.hasNextLine())
        {
            String line = sc.nextLine();
            String[] values = line.split("\t");

            int chebiID = Integer.parseInt(values[0]);
            String inchiString = values[1].replaceAll("/[pmstq].+", "");

            keggIDStmt.setString(1, inchiString);
            keggIDStmt.setInt(2, chebiID);
            keggIDStmt.setInt(3, chebiID);
            keggIDStmt.executeUpdate();

            inchiFileCount = inchiFileCount + line.length() + 1;
            System.out.println("3: " + inchiFileCount + "/" + inchiFileSize);
        }
        con.commit();

        System.out.println("Done 3.");

        sc.close();

        //Delete every compound which has no InChi
        ResultSet noInchiRS = noInchiStmt.executeQuery();
        Set<Integer> noInchiIDs = new HashSet<Integer>();

        while(noInchiRS.next())
        {
            noInchiIDs.add(noInchiRS.getInt("chebiID"));
        }

        for(int chebiID : noInchiIDs)
        {
            removeCompoundStmt.setInt(1, chebiID);
            removeCompoundStmt.executeUpdate();

            removeVariationStmt.setInt(1, chebiID);
            removeVariationStmt.executeUpdate();
        }
        con.commit();
        con.close();
    }

    public static void computeVariations(int id, String name, PreparedStatement prep, Pattern stereoPatt) throws SQLException
    {
        prep.setInt(1, id);
        String lowercase = name.toLowerCase();
        prep.setString(2, lowercase);
        prep.setString(3, lowercase.replaceAll("\\[.+?\\]", ""));
        prep.setString(4, lowercase.replaceAll("\\s+", ""));
        prep.setString(5, lowercase.replaceAll("\\W+", ""));

        Matcher stereoMat = stereoPatt.matcher(lowercase);
        String nonStereoName = stereoMat.replaceAll("$1$2");

        prep.setString(6, nonStereoName.replaceAll("\\W+|(alpha)|(beta)", ""));
        prep.setString(7, nonStereoName.replaceAll("[^A-Za-z]+|(alpha)|(beta)", ""));
        prep.executeUpdate();
    }

}
