package uk.ac.bbk.REx.db.bkmDB;

import org.apache.commons.io.IOUtils;
import uk.ac.bbk.REx.exception.BKMException;
import uk.ac.bbk.REx.utils.Files;

import java.io.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BKMDB 
{
	Connection con;
	PreparedStatement reactionsInPathwayStmt;
	PreparedStatement reactionsContainingSubstrateStmt;
	PreparedStatement reactionsContainingProductStmt;
	PreparedStatement pathwayNameStmt;
	PreparedStatement pathwayIDsContainingReactionStmt;
    PreparedStatement pathwayNamesContainingReactionStmt;
	
	Pattern romanNumeralPatt;
	
	public BKMDB(File data) throws BKMException
    {
        try
        {
            //Not always necessary, but some Java installations require this line.
            Class.forName("org.apache.derby.jdbc.EmbeddedDriver").newInstance();
        }
        catch (InstantiationException e)
        {
            throw new BKMException(e);
        }
        catch (IllegalAccessException e)
        {
            throw new BKMException(e);
        }
        catch (ClassNotFoundException e)
        {
            throw new BKMException(e);
        }

        try
        {
            con = DriverManager.getConnection(
                    "jdbc:derby:" + data.getPath() + "/db/bkm;");
        }
        catch (SQLException e)
        {
            throw new BKMException(e);
        }

        try
        {
            reactionsInPathwayStmt = prepareStatementFromResources("reactionsInPathwayStmt.sql");
            reactionsContainingSubstrateStmt = prepareStatementFromResources("reactionsContainingSubstrateStmt.sql");
            reactionsContainingProductStmt = prepareStatementFromResources("reactionsContainingProductStmt.sql");
            pathwayNameStmt = prepareStatementFromResources("pathwayNameStmt.sql");
            pathwayIDsContainingReactionStmt = prepareStatementFromResources("pathwayIDsContainingReactionStmt.sql");
            pathwayNamesContainingReactionStmt = prepareStatementFromResources("pathwayNamesContainingReactionStmt.sql");
        }
        catch (IOException e)
        {
            throw new BKMException(e);
        }
        catch (SQLException e)
        {
            throw new BKMException(e);
        }

		romanNumeralPatt = Pattern.compile("\\b[IVX]+\\b");
	}

    private PreparedStatement prepareStatementFromResources(String file) throws IOException, SQLException
    {
        InputStream is = new BufferedInputStream(BKMDB.class.getResourceAsStream("/uk/ac/bbk/REx/db/bkmDB/" + file));
        String stmt = IOUtils.toString(is);
        is.close();
        return con.prepareStatement(stmt);
    }
	
	public void close() throws SQLException
	{
		con.close();
	}
	
	public List<Integer> getReactionsInPathway(String metacycPathway) throws SQLException
	{
		reactionsInPathwayStmt.setString(1, metacycPathway);
		ResultSet reactionsRS = reactionsInPathwayStmt.executeQuery();
		
		List<Integer> reactionIDs = new ArrayList<Integer>();
		while(reactionsRS.next())
		{
			reactionIDs.add(reactionsRS.getInt("reactionID"));
		}
		
		return reactionIDs;
	}

    public List<String> getPathwaysContainingReaction(int reactionID) throws SQLException
    {
        pathwayIDsContainingReactionStmt.setInt(1, reactionID);
        ResultSet rs = pathwayIDsContainingReactionStmt.executeQuery();

        List<String> pathwayNames = new ArrayList<String>();
        while(rs.next())
        {
            pathwayNames.add(rs.getString("pathwayID"));
        }

        return pathwayNames;
    }
	
	public List<String> getPathwayNamesContainingReaction(int reactionID) throws SQLException
	{
		pathwayNamesContainingReactionStmt.setInt(1, reactionID);
		ResultSet rs = pathwayNamesContainingReactionStmt.executeQuery();
		
		List<String> pathwayNames = new ArrayList<String>();
		while(rs.next())
		{
			pathwayNames.add(rs.getString("pathwayName"));
		}
		
		return pathwayNames;
	}
	
	public List<Integer> getReactionsContainingSubstrate(String inchi) throws SQLException
	{
		inchi = inchi.replaceAll("/[pmst].*", "");
		reactionsContainingSubstrateStmt.setString(1, inchi);
		ResultSet reactionsRS = reactionsContainingSubstrateStmt.executeQuery();
		
		List<Integer> reactionIDs = new ArrayList<Integer>();
		while(reactionsRS.next())
		{
			reactionIDs.add(reactionsRS.getInt("reactionID"));
		}
		
		return reactionIDs;
	}
	
	public List<Integer> getReactionsContainingProduct(String inchi) throws SQLException
	{
		inchi = inchi.replaceAll("/[pmst].+", "");
		reactionsContainingProductStmt.setString(1, inchi);
		ResultSet reactionsRS = reactionsContainingProductStmt.executeQuery();
		
		List<Integer> reactionIDs = new ArrayList<Integer>();
		while(reactionsRS.next())
		{
			reactionIDs.add(reactionsRS.getInt("reactionID"));
		}
		
		return reactionIDs;
	}
	
	public List<Integer> getReactionsContainingSubstrateAndProduct(String substrateInchi, String productInchi) throws SQLException
	{
		Set<Integer> substrateReactions = new HashSet<Integer>(getReactionsContainingSubstrate(substrateInchi));
		Set<Integer> productReactions = new HashSet<Integer>(getReactionsContainingProduct(productInchi));
		
		Set<Integer> commonReactions = new HashSet<Integer>(substrateReactions);
		commonReactions.retainAll(productReactions);

        List<Integer> output = new ArrayList<Integer>(commonReactions);
        Collections.sort(output);
		
		return output;
	}
	
	public List<Integer> getReactionsContainingBothSubstrates(String inchi1, String inchi2) throws SQLException
	{
		Set<Integer> reactions1 = new HashSet<Integer>(getReactionsContainingSubstrate(inchi1));
		Set<Integer> reactions2 = new HashSet<Integer>(getReactionsContainingSubstrate(inchi2));
		
		Set<Integer> commonReactions = new HashSet<Integer>(reactions1);
		commonReactions.retainAll(reactions2);

        List<Integer> output = new ArrayList<Integer>(commonReactions);
        Collections.sort(output);
		
		return output;
	}
	
	public List<Integer> getReactionsContainingBothProducts(String inchi1, String inchi2) throws SQLException
	{
		Set<Integer> reactions1 = new HashSet<Integer>(getReactionsContainingProduct(inchi1));
		Set<Integer> reactions2 = new HashSet<Integer>(getReactionsContainingProduct(inchi2));
		
		Set<Integer> commonReactions = new HashSet<Integer>(reactions1);
		commonReactions.retainAll(reactions2);

        List<Integer> output = new ArrayList<Integer>();
        Collections.sort(output);
		
		return output;
	}
	
	public boolean isReactionInPathway(String pathwayID, String substrateInchi, String productInchi) throws SQLException
	{
		Set<Integer> reactionsInPathway = new HashSet<Integer>(getReactionsInPathway(pathwayID));
		Set<Integer> reactions = new HashSet<Integer>(getReactionsContainingSubstrateAndProduct(substrateInchi, productInchi));
		
		Set<Integer> commonReactions = new HashSet<Integer>(reactionsInPathway);
		commonReactions.retainAll(reactions);
		
		if(commonReactions.size() > 0)
		{
			return true;
		}
		else
		{
			return false;
		}
	}
	
	public boolean isReactionInAlternativePathway(String pathwayID, String substrateInchi, String productInchi) throws SQLException
	{
		String pathwayName = getPathwayName(pathwayID);
		List<Integer> allReactions = getReactionsContainingSubstrateAndProduct(substrateInchi, productInchi);
		
		Set<String> allPathways = new HashSet<String>();
		for(int reactionID : allReactions)
		{
			allPathways.addAll(getPathwayNamesContainingReaction(reactionID));
		}
		
		for(String testPathwayName : allPathways)
		{
			if(arePathwayNamesAlternatives(pathwayName, testPathwayName))
			{
				return true;
			}
		}
		
		return false;
	}

    public Set<Integer> getReactionForSubstratePair(String substrateInchi1, String substrateInchi2, String productInchi) throws SQLException
    {
        Set<Integer> reactions1 = new HashSet<Integer>(getReactionsContainingSubstrate(substrateInchi1));
        Set<Integer> reactions2 = new HashSet<Integer>(getReactionsContainingSubstrate(substrateInchi2));
        Set<Integer> reactions3 = new HashSet<Integer>(getReactionsContainingProduct(productInchi));

        Set<Integer> commonReactions = new HashSet<Integer>(reactions1);
        commonReactions.retainAll(reactions2);
        commonReactions.retainAll(reactions3);

        return commonReactions;
    }

    public Set<Integer> getReactionForProductPair(String substrateInchi, String productInchi1, String productInchi2) throws SQLException
    {
        Set<Integer> reactions1 = new HashSet<Integer>(getReactionsContainingSubstrate(substrateInchi));
        Set<Integer> reactions2 = new HashSet<Integer>(getReactionsContainingProduct(productInchi1));
        Set<Integer> reactions3 = new HashSet<Integer>(getReactionsContainingProduct(productInchi2));

        Set<Integer> commonReactions = new HashSet<Integer>(reactions1);
        commonReactions.retainAll(reactions2);
        commonReactions.retainAll(reactions3);

        return commonReactions;
    }
	
	public String getPathwayName(String pathwayID) throws SQLException
	{
		pathwayNameStmt.setString(1, pathwayID);
		ResultSet rs = pathwayNameStmt.executeQuery();
		
		if(rs.next())
		{
			return rs.getString("pathwayName");
		}
		else
		{
			throw new IllegalArgumentException("Pathway " + pathwayID + " not found.");
		}
	}
	
	public boolean arePathwayNamesAlternatives(String pathway1, String pathway2)
	{
		Matcher m1 = romanNumeralPatt.matcher(pathway1);
		Matcher m2 = romanNumeralPatt.matcher(pathway2);
		
		if(m1.find() && m2.find())
		{
			String name1 = pathway1.substring(0, m1.start()-1);
			String name2 = pathway2.substring(0, m2.start()-1);
			
			if(name1.equals(name2))
			{
				return true;
			}
			else
			{
				return false;
			}
		}
		else if(pathway1.contains(pathway2) || pathway2.contains(pathway1))
        {
            return true;
        }
        else
		{
			return false;
		}
    }

    public boolean isPathwayInSet(String pathwaySet, String pathway)
    {
        if(pathway.contains(pathwaySet))
        {
            return true;
        }
        else
        {
            return false;
        }
    }
}
