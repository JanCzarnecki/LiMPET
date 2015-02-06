package uk.ac.bbk.REx.utils;

public class Scores
{
    private int maxBranchLength;
    private double brenda;
    private double notBrenda;
    private double oneExtraction;
    private double twoExtractions;
    private double threeExtractions;
    private double fourExtractions;
    private double fiveOrMoreExtractions;
    private double inSeed;
    private double oneExtractionRelevance;
    private double twoExtractionsRelevance;
    private double threeExtractionsRelevance;
    private double fourExtractionsRelevance;
    private double fiveOrMoreExtractionsRelevance;
    private double seedMoleculeDiceScoreMultiplier;
    private double seedMoleculeDiceScoreConstant;

    private Scores(){}

    public int getMaxBranchLength() {
        return maxBranchLength;
    }

    public double getBrenda() {
        return brenda;
    }

    public double getNotBrenda() {
        return notBrenda;
    }

    public double getOneExtraction() {
        return oneExtraction;
    }

    public double getTwoExtractions() {
        return twoExtractions;
    }

    public double getThreeExtractions() {
        return threeExtractions;
    }

    public double getFourExtractions() {
        return fourExtractions;
    }

    public double getFiveOrMoreExtractions() {
        return fiveOrMoreExtractions;
    }

    public double getInSeed() {
        return inSeed;
    }

    public double getOneExtractionRelevance() {
        return oneExtractionRelevance;
    }

    public double getTwoExtractionsRelevance() {
        return twoExtractionsRelevance;
    }

    public double getThreeExtractionsRelevance() {
        return threeExtractionsRelevance;
    }

    public double getFourExtractionsRelevance() {
        return fourExtractionsRelevance;
    }

    public double getFiveOrMoreExtractionsRelevance() {
        return fiveOrMoreExtractionsRelevance;
    }

    public double getSeedMoleculeDiceScoreMultiplier() {
        return seedMoleculeDiceScoreMultiplier;
    }

    public double getSeedMoleculeDiceScoreConstant() {
        return seedMoleculeDiceScoreConstant;
    }
}
