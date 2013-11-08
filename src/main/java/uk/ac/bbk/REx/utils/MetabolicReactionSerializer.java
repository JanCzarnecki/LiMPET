package uk.ac.bbk.REx.utils;

import com.google.gson.*;
import uk.ac.ebi.mdk.domain.entity.reaction.MetabolicParticipant;
import uk.ac.ebi.mdk.domain.entity.reaction.MetabolicReaction;

import java.lang.reflect.Type;

public class MetabolicReactionSerializer implements JsonSerializer<MetabolicReaction>
{
    public JsonElement serialize(MetabolicReaction reaction, Type type, JsonSerializationContext context)
    {
        JsonObject object = new JsonObject();

        JsonArray substrates = new JsonArray();
        for(MetabolicParticipant substrate : reaction.getReactants())
        {
            substrates.add(new JsonPrimitive(substrate.getMolecule().getName()));
        }
        object.add("substrates", substrates);

        JsonArray products = new JsonArray();
        for(MetabolicParticipant product : reaction.getProducts())
        {
            products.add(new JsonPrimitive(product.getMolecule().getName()));
        }
        object.add("products", products);

        return object;
    }
}
