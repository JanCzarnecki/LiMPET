package uk.ac.bbk.REx.db.bioCycDB;

import com.google.gson.Gson;
import uk.ac.bbk.REx.db.chebiDB.CHEBIDB;
import uk.ac.bbk.REx.exception.CHEBIException;
import uk.ac.bbk.REx.exception.NameNotFoundException;
import uk.ac.bbk.REx.exception.NameTooShortException;
import uk.ac.ebi.mdk.domain.annotation.Annotation;
import uk.ac.ebi.mdk.domain.annotation.InChI;
import uk.ac.ebi.mdk.domain.entity.DefaultEntityFactory;
import uk.ac.ebi.mdk.domain.entity.EntityFactory;
import uk.ac.ebi.mdk.domain.entity.Metabolite;
import uk.ac.ebi.mdk.domain.entity.reaction.BiochemicalReaction;
import uk.ac.ebi.mdk.domain.entity.reaction.MetabolicParticipant;
import uk.ac.ebi.mdk.domain.entity.reaction.MetabolicReaction;
import uk.ac.ebi.mdk.domain.identifier.basic.BasicChemicalIdentifier;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PathwayReader
{
    private final Logger LOGGER = Logger.getLogger(this.getClass().getName());

    private BioCycDB bc;
    private CHEBIDB chebi;
    private Pattern tagPatt;

    public PathwayReader(String bioCycUser, String bioCycPassword) throws SQLException, IOException, ClassNotFoundException, CHEBIException
    {
        bc = new BioCycDB(bioCycUser, bioCycPassword);
        //chebi = new CHEBIDB();
        tagPatt = Pattern.compile("<[^>]+?>");
    }

    public List<MetabolicReaction> getSeedPathway(String pathwayName) throws SQLException, CHEBIException
    {
        return getCombinedPathway("metacyc", pathwayName);
    }

    public List<MetabolicReaction> getSeedPathway(String[] pathwayName) throws SQLException, CHEBIException
    {
        return getCombinedPathway("metacyc", Arrays.asList(pathwayName));
    }

    public List<MetabolicReaction> getCombinedPathway(String dataSet, String pathwayName) throws SQLException, CHEBIException
    {
        Map<String, MetabolicReaction> reactions = new HashMap<String, MetabolicReaction>();
        Map<String, Metabolite> metabolites = new HashMap<String, Metabolite>();

        for(String pathwayID : bc.getPathwayIDs(dataSet, pathwayName))
        {
            String fullPathwayName = bc.getPathwayName(dataSet, pathwayID);
            reactions.putAll(getPathwayMap(dataSet, fullPathwayName, metabolites));
        }

        return new ArrayList<MetabolicReaction>(reactions.values());
    }

    public List<MetabolicReaction> getCombinedPathway(String dataSet, Collection<String> pathwayNames) throws SQLException, CHEBIException
    {
        Map<String, MetabolicReaction> reactions = new HashMap<String, MetabolicReaction>();
        Map<String, Metabolite> metabolites = new HashMap<String, Metabolite>();

        for(String pathwayName : pathwayNames)
        {
            reactions.putAll(getPathwayMap(dataSet, pathwayName, metabolites));
        }

        return new ArrayList<MetabolicReaction>(reactions.values());
    }

    public List<MetabolicReaction> getPathway(String dataSet, String pathwayName) throws SQLException, CHEBIException
    {
        return new ArrayList<MetabolicReaction>(getPathwayMap(dataSet, pathwayName, new HashMap<String, Metabolite>()).values());
    }

    private Map<String, MetabolicReaction> getPathwayMap(String dataSet, String pathwayName, Map<String, Metabolite> metabolites) throws SQLException, CHEBIException
    {
        EntityFactory entityFactory = DefaultEntityFactory.getInstance();
        Map<String, MetabolicReaction> reactions = new HashMap<String, MetabolicReaction>();
        String pathwayID = bc.getPathwayID(dataSet, pathwayName);

        for(String reactionID : bc.getReactionIDs(pathwayID))
        {
            BiochemicalReaction reaction = entityFactory.biochemicalReaction(entityFactory.reaction());

            for(String name : bc.getSubstrateNames(reactionID))
            {
                name = name.replaceAll("<.+?>", "");
                name = name.replaceAll("&.+?;", "");

                try
                {
                    constructMetabolite(name, Type.SUBSTRATE, reaction, metabolites, entityFactory, chebi);
                }
                catch(NameNotFoundException e)
                {
                    continue;
                }
                catch(NameTooShortException e)
                {
                    continue;
                }
            }

            for(String name : bc.getProductNames(reactionID))
            {
                name = name.replaceAll("<.+?>", "");
                name = name.replaceAll("&.+?;", "");

                try
                {
                    constructMetabolite(name, Type.PRODUCT, reaction, metabolites, entityFactory, chebi);
                }
                catch(NameNotFoundException e)
                {
                    continue;
                }
                catch(NameTooShortException e)
                {
                    continue;
                }
            }

            String reactionString = reactionToString(reaction);
            if(!reactions.containsKey(reactionString))
            {
                reactions.put(reactionString, reaction);
            }
        }

        return reactions;
    }

    private static void addMetabolite(String name, String inchi, Map<String, Metabolite> metabolites, EntityFactory entityFactory)
    {
        if(!metabolites.containsKey(inchi))
        {
            Metabolite m = entityFactory.metabolite();
            m.setIdentifier(BasicChemicalIdentifier.nextIdentifier());
            m.setName(name);

            Annotation inchiA = new InChI(inchi);
            m.addAnnotation(inchiA);

            metabolites.put(inchi, m);
        }
    }

    private void constructMetabolite(String name,
                                     Type type,
                                     BiochemicalReaction reaction,
                                     Map<String, Metabolite> metabolites,
                                     EntityFactory entityFactory,
                                     CHEBIDB chebi)
            throws NameNotFoundException,CHEBIException, NameTooShortException
    {
        Matcher tagMat = tagPatt.matcher(name);
        name = tagMat.replaceAll("");
        String inchi = chebi.getInchi(chebi.getCHEBIID(name));

        if(inchi != null)
        {
            addMetabolite(name, inchi, metabolites, entityFactory);
            if(type == Type.SUBSTRATE)
            {
                reaction.addReactant(metabolites.get(inchi));
            }
            else if(type == Type.PRODUCT)
            {
                reaction.addProduct(metabolites.get(inchi));
            }
        }
    }

    private static enum Type
    {
        SUBSTRATE, PRODUCT
    }

    protected Map<Type, List<String>> reactionToCollection(MetabolicReaction reaction)
    {
        Map<Type, List<String>> map = new TreeMap<Type, List<String>>();
        map.put(Type.SUBSTRATE, new ArrayList<String>());
        map.put(Type.PRODUCT, new ArrayList<String>());

        for(MetabolicParticipant substrate : reaction.getReactants())
        {
            Metabolite m = substrate.getMolecule();
            for(InChI inchi : m.getAnnotations(InChI.class))
            {
                map.get(Type.SUBSTRATE).add(inchi.toInChI());
            }
        }

        for(MetabolicParticipant product : reaction.getProducts())
        {
            Metabolite m = product.getMolecule();
            for(InChI inchi : m.getAnnotations(InChI.class))
            {
                map.get(Type.PRODUCT).add(inchi.toInChI());
            }
        }

        for(List<String> inchis : map.values())
        {
            Collections.sort(inchis);
        }

        return map;
    }

    protected String reactionToString(MetabolicReaction reaction)
    {
        Map<Type, List<String>> map = reactionToCollection(reaction);
        Gson gson = new Gson();
        return gson.toJson(map);
    }

    protected String reactionsToString(Collection<MetabolicReaction> reactions)
    {
        List<Map<Type, List<String>>> maps = new ArrayList<Map<Type, List<String>>>();
        for(MetabolicReaction reaction : reactions)
        {
            maps.add(reactionToCollection(reaction));
        }

        Gson gson = new Gson();
        return gson.toJson(maps);
    }

    public void close() throws SQLException
    {
        bc.close();
    }
}
