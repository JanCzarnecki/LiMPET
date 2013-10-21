package uk.ac.bbk.REx.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import uk.ac.bbk.REx.db.chebiDB.CHEBIDB;
import uk.ac.bbk.REx.exception.CHEBIException;

public class QueryBuilder 
{
	private CHEBIDB chebiDB;
	
	public QueryBuilder() throws CHEBIException
	{
		chebiDB = new CHEBIDB();
	}

    /**
     * Constructs queries suitable for querying PubMed for articles to mine.
     *
     * @param speciesID The NCBI Taxonomy ID of the organism of interest.
     * @param inchis A collection of InChIs. A query will be constructed for each small molecule.
     * @return
     * @throws CHEBIException
     * @throws FileNotFoundException
     */
	public Set<String> build(String speciesID, Collection<String> inchis) throws CHEBIException, FileNotFoundException
    {
        List<String> organismNames = new ArrayList<String>();
        Scanner sc = new Scanner(new BufferedReader(new FileReader(new File("data/species-light.tsv"))));
        Pattern idPatt = Pattern.compile("species:ncbi:" + speciesID + "\\t(.*)");
        while(sc.hasNextLine())
        {
            String line = sc.nextLine();
            Matcher idMat = idPatt.matcher(line);
            if(idMat.find())
            {
                String namesString = idMat.group(1);
                String[] namesArray = namesString.split("\\|");
                organismNames = Arrays.asList(namesArray);
            }
        }

		Set<String> queries = new HashSet<String>();
		StringBuilder organismQuery = new StringBuilder("(");
        int count = 0;
        for(String organismName : organismNames)
        {
            count++;
            if(count > 1)
            {
                organismQuery.append(" OR ");
            }

            organismQuery.append("(\"" + organismName + "\")");
        }
        organismQuery.append(")");
		
		for(String inchi : inchis)
		{
            Set<Integer> ids = null;
            try
            {
                ids = chebiDB.getIDFromInchi(inchi);
            }
            catch (SQLException e)
            {
                throw new CHEBIException(e);
            }
            Set<String> names = new HashSet<String>();
            for(int id : ids)
            {
                names.addAll(chebiDB.getNames(id));
            }
			
			StringBuilder chemicalQuery = new StringBuilder("(");
			
			count = 0;
			for(String name : names)
			{
                count++;

				if(name.length() == 1)
				{
					continue;
				}

				if(count > 1)
				{
					chemicalQuery.append(" OR ");
				}
				
				name = name.replaceAll("\\(", "");
				name = name.replaceAll("\\)", "");
				chemicalQuery.append("(" + name + ")");
			}
			
			chemicalQuery.append(")");
			
			queries.add(organismQuery + " AND " + chemicalQuery);
		}
		
		return queries;
	}
}
