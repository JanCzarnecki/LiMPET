package uk.ac.bbk.REx.db.chebiDB;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import uk.ac.bbk.REx.exception.CHEBIException;
import uk.ac.bbk.REx.exception.NameNotFoundException;
import uk.ac.bbk.REx.exception.NameTooShortException;
import uk.ac.bbk.REx.utils.Strings;

/**
 * Represents a local CHEBI database using DerbyDB.
 */
public class CHEBIDB
{
	private Connection con;
    private String stmtTemp;
    private Pattern stereoPatt;
    private Map<String, PreparedStatement> statements;
    private PreparedStatement idStatement;
    private PreparedStatement parentIDStmt;
    private List<String> orderOfVariations;
    private PreparedStatement inchiStmt;
    private PreparedStatement idFromInchiStmt;
	
	public CHEBIDB() throws CHEBIException
	{
        try
        {
            //Not always necessary, but some Java installations require this line.
            Class.forName("org.apache.derby.jdbc.EmbeddedDriver").newInstance();
        }
        catch (InstantiationException e)
        {
            throw new CHEBIException(e);
        }
        catch (IllegalAccessException e)
        {
            throw new CHEBIException(e);
        }
        catch (ClassNotFoundException e)
        {
            throw new CHEBIException(e);
        }

        try
		{
			con = DriverManager.getConnection("jdbc:derby:data/db/chebi;");
		} 
		catch (SQLException e) 
		{
			throw new CHEBIException(e);
		}

		try
		{
            InputStream is = new BufferedInputStream(
                    CHEBIDB.class.getResourceAsStream("/uk/ac/bbk/REx/db/chebiDB/variationsStmt.sql"));
            stmtTemp = IOUtils.toString(is);
            is.close();

			stereoPatt = Pattern.compile("(\\A|\\W)[dl]-");
			
			orderOfVariations = new ArrayList<String>();
            InputStream orderOfVariationsIS = new BufferedInputStream(
                    CHEBIDB.class.getResourceAsStream("/uk/ac/bbk/REx/db/chebiDB/orderOfVariations.txt"));
            for(String variation : IOUtils.readLines(orderOfVariationsIS))
            {
                orderOfVariations.add(variation);
            }
            orderOfVariationsIS.close();

            statements = new HashMap<String, PreparedStatement>();
            for(String variation : orderOfVariations)
            {
                statements.put(variation, prepareVariationsStmt(variation));
            }
			
			idStatement = con.prepareStatement(
					"SELECT exact " +
					"FROM variations " +
					"WHERE chebiID=?");
			
			parentIDStmt = con.prepareStatement(
					"SELECT parentID " +
					"FROM compounds " +
					"WHERE chebiID=?");
			
			inchiStmt = con.prepareStatement(
					"SELECT inchi " +
					"FROM compounds " +
					"WHERE chebiID=?");

            idFromInchiStmt = con.prepareStatement(
                    "SELECT chebiID " +
                    "FROM compounds " +
                    "WHERE inchi=?"
            );
		}
		catch(SQLException e)
		{
			throw new CHEBIException(e);
		}
        catch (IOException e)
        {
            throw new CHEBIException(e);
        }
    }

    private PreparedStatement prepareVariationsStmt(String col) throws IOException, SQLException
    {
        String stmt = stmtTemp.replaceAll("\\{col\\}", col);
        return con.prepareStatement(stmt);
    }
	
	public Connection getConnection()
	{
		return con;
	}
	
	public void close() throws CHEBIException
	{
		try 
		{
			getConnection().close();
		} 
		catch (SQLException e) 
		{
			throw new CHEBIException(e);
		}
	}
	
	/**
	 * Attempts to retrieve a CHEBI ID for the given chemical name.
	 * 
	 * @param name the name of a chemical.
	 * @return the CHEBI ID for the given chemical
	 * @throws CHEBIException 
	 * @throws SQLException
	 * @throws NameNotFoundException If no ID can be found.
	 * @throws NameTooShortException If the chemical name is only one character long.
	 */
	public synchronized int getCHEBIID(String name) throws CHEBIException, NameNotFoundException, NameTooShortException
	{
		Map<String, String> variations = prepareNameForSearch(name);
		
		try
		{
			for(String varType : orderOfVariations)
			{
				PreparedStatement stmt = statements.get(varType);
				stmt.setString(1, variations.get(varType));
				ResultSet rs = stmt.executeQuery();

                Integer lowest = null;
                Integer lowestID = null;
				
				while(rs.next())
				{
                    int distance = Strings.computeLevenshteinDistance(name.toLowerCase(), rs.getString("exact"));

                    if(lowest == null || distance < lowest);
                    {
                        lowest = distance;
                        lowestID = rs.getInt("chebiID");
                    }
				}

                if(lowestID != null)
                {
                    return lowestID;
                }
			}
		}
		catch(SQLException e)
		{
			throw new CHEBIException(e);
		}
		
		throw new NameNotFoundException();
	}

    public Set<Integer> getIDFromInchi(String inchi) throws SQLException
    {
        idFromInchiStmt.setString(1, inchi);
        ResultSet rs = idFromInchiStmt.executeQuery();
        Set<Integer> ids = new HashSet<Integer>();

        while(rs.next())
        {
            ids.add(rs.getInt("chebiID"));
        }

        return ids;
    }
	
	private Map<String, String> prepareNameForSearch(String name) throws NameTooShortException
	{
		String letters = name.replaceAll("[^A-Za-z]", "");
		if(letters.length() < 2)
		{
			throw new NameTooShortException();
		}
		
		String lowercase = name.toLowerCase();
		
		Map<String, String> output = new HashMap<String, String>();

        String greek = lowercase.replaceAll("α", "alpha");
        greek = greek.replaceAll("β", "beta");
        output.put("exact", greek);
		output.put("squares", greek.replaceAll("\\[.+?\\]", ""));
		output.put("spaces", greek.replaceAll("\\s+", ""));
		output.put("nonWordChars", greek.replaceAll("\\W+", ""));

		Matcher stereoMat = stereoPatt.matcher(lowercase);
        String nonStereoName = lowercase;
        while(stereoMat.find())
        {
		    nonStereoName = stereoMat.replaceAll("$1-");
            stereoMat = stereoPatt.matcher(nonStereoName);
        }

		output.put("stereo", nonStereoName.replaceAll("\\W+|α|β", ""));
        output.put("everythingButLetters", nonStereoName.replaceAll("[^A-Za-z]+|α|β", ""));
		
		return output;
	}
	
	private String correctGreekLetters(String input)
	{
		Pattern betaPatt = Pattern.compile("(^|\\W)B|b(\\W.*)");
		Matcher betaMat = betaPatt.matcher(input);
		if(betaMat.find())
		{
			return betaMat.group(1) + "β" + betaMat.group(2);
		}
		else
		{
			return input;
		}
	}
	
	public Set<String> getSynonyms(String name) throws CHEBIException, NameTooShortException
	{
		Set<String> names = new HashSet<String>();
		
		try
		{
			int id = getCHEBIID(name);
            String inchi = getInchi(id);
            Set<Integer> ids = getIDFromInchi(inchi);

            for(int anID : ids)
            {
                idStatement.setInt(1, anID);
                ResultSet rs = idStatement.executeQuery();

                while(rs.next())
                {
                    names.add(rs.getString("exact"));
                }
            }
		}
		catch(NameNotFoundException e)
		{
			names.add(name);
		}
		catch(SQLException e)
		{
			throw new CHEBIException(e);
		}
		
		return names;
	}
	
	public Set<String> getNames(int id) throws CHEBIException
	{
		Set<String> names = new HashSet<String>();
		
		try
		{
			idStatement.setInt(1, id);
			ResultSet rs = idStatement.executeQuery();
			
			while(rs.next())
			{
				names.add(rs.getString("exact"));
			}
		}
		catch(SQLException e)
		{
			throw new CHEBIException(e);
		}
		
		return names;
	}
	
	public String getInchi(int id) throws CHEBIException
	{
		try
		{
			inchiStmt.setInt(1, id);
			ResultSet rs = inchiStmt.executeQuery();
			if(rs.next())
			{
				return rs.getString("inchi");
			}
			else
			{
				return null;
			}
		}
		catch(SQLException e)
		{
			throw new CHEBIException(e);
		}
	}

    public String debug(int id) throws SQLException
    {
        PreparedStatement stmt = con.prepareStatement
        (
            "SELECT * " +
            "FROM variations " +
            "WHERE chebiID=?"
        );
        stmt.setInt(1, id);

        ResultSet rs = stmt.executeQuery();
        StringBuilder result = new StringBuilder();
        while(rs.next())
        {
            result.append
            (
                rs.getInt(1) + " --- " +
                rs.getString(2) + " --- " +
                rs.getString(3) + " --- " +
                rs.getString(4) + " --- " +
                rs.getString(5) + " --- " +
                rs.getString(6) + " --- " +
                rs.getString(7) + " --- " +
                rs.getString(8) + "\n"
            );
        }

        return result.toString();
    }
}
