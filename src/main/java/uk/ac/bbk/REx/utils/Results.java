package uk.ac.bbk.REx.utils;

import com.google.gson.*;
import uk.ac.bbk.REx.program.Util;
import uk.ac.ebi.mdk.domain.entity.reaction.MetabolicReaction;

import java.lang.reflect.Type;
import java.util.List;

public class Results
{
    private double recall;
    private double precision;
    private List<MetabolicReaction> reactionsExpectedAndFound;
    private List<MetabolicReaction> reactionsExpectedButNotFound;
    private List<MetabolicReaction> reactionsFoundButNotExpected;

    public Results(double recall,
                   double precision,
                   List<MetabolicReaction> reactionsExpectedAndFound,
                   List<MetabolicReaction> reactionsExpectedButNotFound,
                   List<MetabolicReaction> reactionsFoundButNotExpected)
    {
        this.recall = recall;
        this.precision = precision;
        this.reactionsExpectedAndFound = reactionsExpectedAndFound;
        this.reactionsExpectedButNotFound = reactionsExpectedButNotFound;
        this.reactionsFoundButNotExpected = reactionsFoundButNotExpected;
    }

    public double getRecall() {
        return recall;
    }

    public double getPrecision() {
        return precision;
    }

    public List<MetabolicReaction> getReactionsExpectedAndFound() {
        return reactionsExpectedAndFound;
    }

    public List<MetabolicReaction> getReactionsExpectedButNotFound() {
        return reactionsExpectedButNotFound;
    }

    public List<MetabolicReaction> getReactionsFoundButNotExpected() {
        return reactionsFoundButNotExpected;
    }
}
