package uk.ac.bbk.REx.utils;

import java.io.*;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import uk.ac.bbk.REx.db.chebiDB.CHEBIDB;
import uk.ac.bbk.REx.exception.CHEBIException;

public class QueryBuilder 
{
	private CHEBIDB chebiDB;
    private Pattern unwantedLayers;
    File data;
	
	public QueryBuilder(File data) throws CHEBIException, UnsupportedEncodingException
    {
        this.data = data;
		chebiDB = new CHEBIDB(data);
        unwantedLayers = Pattern.compile("/[pqbtmsifr].+");
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
	public Set<String> build(String speciesID, Collection<String> extraNames, Set<String> inchis, Set<String> currencyMols) throws CHEBIException, FileNotFoundException, UnsupportedEncodingException
    {
        List<String> organismNames = new ArrayList<String>();
        File parent = Files.getParentDirectory();
        Scanner sc = new Scanner(new BufferedReader(new FileReader(new File(data, "species-light.tsv"))));
        Pattern idPatt = Pattern.compile("species:ncbi:" + speciesID + "\\t(.*)");
        while(sc.hasNextLine())
        {
            String line = sc.nextLine();
            Matcher idMat = idPatt.matcher(line);
            if(idMat.find())
            {
                String namesString = idMat.group(1);
                String[] namesArray = namesString.split("\\|");
                organismNames = new ArrayList<String>(Arrays.asList(namesArray));
            }
        }

        organismNames.addAll(new ArrayList<String>(extraNames));

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

            organismName = organismName.replaceAll("\\(", "");
            organismName = organismName.replaceAll("\\)", "");
            organismQuery.append("(\"" + organismName + "\"[All Fields])");
        }
        organismQuery.append(")");
		
		for(String inchi : inchis)
		{
            Matcher unwantedLayersMat = unwantedLayers.matcher(inchi);
            inchi = unwantedLayersMat.replaceAll("");
            if(currencyMols.contains(inchi))
            {
                continue;
            }
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

                //So that eUtils doesn't throw an Error 414.
                if(chemicalQuery.length() + name.length() <= 3000)
                {
                    if(count > 1)
                    {
                        chemicalQuery.append(" OR ");
                    }

                    name = name.replaceAll("\\(", "");
                    name = name.replaceAll("\\)", "");
                    chemicalQuery.append("(\"" + name + "\"[All Fields])");
                }
                else
                {
                    break;
                }
			}
			
			chemicalQuery.append(")");
			
			queries.add(organismQuery + " AND " + chemicalQuery);
		}
		
		return queries;
	}
}
