package uk.ac.bbk.REx.db.bioCycDB;


import com.google.gson.Gson;
import org.apache.commons.lang.StringEscapeUtils;
import uk.ac.bbk.REx.exception.BioCycException;
import uk.ac.ebi.mdk.domain.annotation.Annotation;
import uk.ac.ebi.mdk.domain.annotation.InChI;
import uk.ac.ebi.mdk.domain.entity.DefaultEntityFactory;
import uk.ac.ebi.mdk.domain.entity.EntityFactory;
import uk.ac.ebi.mdk.domain.entity.Metabolite;
import uk.ac.ebi.mdk.domain.entity.reaction.MetabolicParticipant;
import uk.ac.ebi.mdk.domain.entity.reaction.MetabolicReaction;
import uk.ac.ebi.mdk.domain.identifier.basic.BasicChemicalIdentifier;

import java.util.*;
import java.util.logging.Logger;

public class PathwayConstructor {
    private final Logger LOGGER = Logger.getLogger(this.getClass().getName());
    private BioCyc bc;
    private EntityFactory entityFactory;
    private Map<String, Metabolite> metaboliteIndex;

    public PathwayConstructor() throws BioCycException {
        bc = new BioCyc();
        entityFactory = DefaultEntityFactory.getInstance();
        metaboliteIndex = new HashMap<String, Metabolite>();
    }

    public List<MetabolicReaction> constructPathway(String dataset, Collection<String> pathwayIDs) throws BioCycException {
        List<MetabolicReaction> reactions = new ArrayList<MetabolicReaction>();
        for(String pathwayID : pathwayIDs)
        {
            reactions.addAll(constructPathway(dataset, pathwayID));
        }
        return reactions;
    }

    public List<MetabolicReaction> constructPathway(String dataset, String pathwayID) throws BioCycException {
        LOGGER.info(String.format("Building pathway %s:%s from BioCyc.", dataset, pathwayID));
        List<MetabolicReaction> reactions = new ArrayList<MetabolicReaction>();
        List<String> reactionIDs = bc.getReactionIDs(dataset, pathwayID);
        for(String reactionID : reactionIDs) {
            MetabolicReaction r = entityFactory.reaction();

            List<String> substrateIDs = bc.getSubstrateIDs(dataset, reactionID);
            for(String substrateID : substrateIDs) {
                Metabolite m;
                if(metaboliteIndex.containsKey(substrateID)) {
                    m = metaboliteIndex.get(substrateID);
                    r.addReactant(m);
                } else {
                    Map<String, String> nameAndInchi = bc.getCompoundNameAndInchi(dataset, substrateID);

                    if(nameAndInchi.get("inchi") != null)
                    {
                        m = constructMetabolite(nameAndInchi.get("name"), nameAndInchi.get("inchi").replaceAll("/[pmstb].+", ""));
                        metaboliteIndex.put(substrateID, m);

                        r.addReactant(m);
                    }
                }
            }

            List<String> productIDs = bc.getProductIDs(dataset, reactionID);
            for(String productID : productIDs) {
                Metabolite m;
                if(metaboliteIndex.containsKey(productID)) {
                    m = metaboliteIndex.get(productID);
                    r.addProduct(m);
                } else {
                    Map<String, String> nameAndInchi = bc.getCompoundNameAndInchi(dataset, productID);

                    if(nameAndInchi.get("inchi") != null)
                    {
                        m = constructMetabolite(nameAndInchi.get("name"), nameAndInchi.get("inchi").replaceAll("/[pmstb].+", ""));
                        metaboliteIndex.put(productID, m);

                        r.addProduct(m);
                    }
                }
            }

            reactions.add(r);
        }

        LOGGER.info(String.format("Pathway %s:%s from BioCyc successfully built.", dataset, pathwayID));
        return reactions;
    }

    private Metabolite constructMetabolite(String name, String inchi) {
        Metabolite m = entityFactory.metabolite();
        m.setIdentifier(BasicChemicalIdentifier.nextIdentifier());
        m.setName(StringEscapeUtils.escapeXml(name));

        Annotation inchiA = new InChI(inchi);
        m.addAnnotation(inchiA);
        return m;
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
}
