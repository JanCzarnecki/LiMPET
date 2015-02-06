package uk.ac.bbk.REx.annotators;

import hu.u_szeged.rgai.bio.uima.tagger.LinnaeusSpecies;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import opennlp.uima.Sentence;
import opennlp.uima.Token;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;

import uk.ac.bbk.REx.settings.ScoringSettings;
import uk.ac.bbk.REx.types.Chemical;
import uk.ac.bbk.REx.types.Gene;
import uk.ac.bbk.REx.types.ReactionKeyword;
import uk.ac.bbk.REx.utils.CharacterIndex;
import uk.ac.bbk.REx.utils.JCasUtils;
import uk.ac.bbk.REx.utils.StemmerUtils;

/**
 * UIMA annotator that finds metabolic reactions in a CAS containing Chemical and Gene entities.
 */
public class ReactionAnnotator extends JCasAnnotator_ImplBase
{
	private final static Logger LOGGER = Logger.getLogger(ReactionAnnotator.class.getName());
	private ScoringSettings scores;
	private String organismID;
	
	@Override
	public void initialize(UimaContext aContext) throws ResourceInitializationException
	{
		super.initialize(aContext);
		LOGGER.log(Level.INFO, "Initialising reaction annotator.");
		
		try
		{
			scores = ScoringSettings.build((String)getContext().getConfigParameterValue(("scoringSettings")));
		}	
		catch (IOException e)
		{
			throw new ResourceInitializationException(e);
		}
		
		organismID = (String)getContext().getConfigParameterValue("organism");
	}
	
	@Override
	public void process(JCas cas) throws AnalysisEngineProcessException
	{
		LOGGER.log(Level.INFO, "Finding reactions in article " + JCasUtils.getPubMedID(cas));
		CharacterIndex<Chemical> chemicalIndex = new CharacterIndex<Chemical>(cas, Chemical.type);
		CharacterIndex<Gene> geneIndex = new CharacterIndex<Gene>(cas, Gene.type);
		CharacterIndex<Token> tokenIndex = new CharacterIndex<Token>(cas, Token.type);
		CharacterIndex<ReactionKeyword> kwIndex = new CharacterIndex<ReactionKeyword>(cas, ReactionKeyword.type);
		CharacterIndex<LinnaeusSpecies> speciesIndex = new CharacterIndex<LinnaeusSpecies>(cas, LinnaeusSpecies.type);
		
		LinnaeusSpecies firstOrganism = null;
		for(Annotation organism : cas.getAnnotationIndex(LinnaeusSpecies.type))
		{
			firstOrganism = (LinnaeusSpecies)organism;
			break;
		}

		for(Annotation sentence : cas.getAnnotationIndex(Sentence.type))
		{
			if(chemicalIndex.getOverlappingAnnotations(sentence).size() > 15)
			{
				continue;
			}

            Set<LinnaeusSpecies> organisms = new HashSet<LinnaeusSpecies>();
            if(firstOrganism !=null)
            {
                List<LinnaeusSpecies> organismsInSentence = speciesIndex.getOverlappingAnnotations(sentence);
                if(organismsInSentence.size() > 0)
                {
                    organisms.addAll(organismsInSentence);
                }
                else
                {
                    organisms.add(firstOrganism);
                }
            }
            FSList organismsFSList = JCasUtils.createFSList(cas, organisms);
			
			List<Chemical> chemicals = chemicalIndex.getOverlappingAnnotations(sentence);
			List<Gene> genes = geneIndex.getOverlappingAnnotations(sentence);
			
			//Reactions involving 8 or more chemicals are rare. Time to extract gets exponentially longer as
			//the number of entities increases.
			/*
			if(chemicals.size() >= 8)
			{
				continue;
			}
			*/
			
			chemicals = JCasUtils.removeAcronymsWithDefinition(chemicals);
			Set<TreeMap<Integer, Chemical>> combs = getAllContiguousCombinations(chemicals);
			
			//Find all possible reactions
			Set<Reaction> possibleReactions = new HashSet<Reaction>();
			
			for(TreeMap<Integer, Chemical> comb : combs)
			{
				//Get chemicals before and after the combination
				int first = comb.firstKey();
				int last = comb.lastKey();
				
				List<Chemical> before = new ArrayList<Chemical>();
				List<Chemical> after = new ArrayList<Chemical>();
				
				for(int i=0; i<first; i++)
				{
					before.add(chemicals.get(i));
				}
				
				for(int i=last+1; i<chemicals.size(); i++)
				{
					after.add(chemicals.get(i));
				}
				
				List<Chemical> combList = new ArrayList<Chemical>();
				for(int key : comb.keySet())
				{
					combList.add(comb.get(key));
				}
				
				Reaction r;
				
				if(before.size() > 0)
				{
					Set<TreeMap<Integer, Chemical>> beforeCombs = getAllContiguousCombinations(before);
					for(TreeMap<Integer, Chemical> beforeMap : beforeCombs)
					{
						List<Chemical> beforeList = new ArrayList<Chemical>(beforeMap.values());
					
						//Create the reactions with no enzyme assigned
						r = new Reaction(cas, sentence, beforeList, combList, kwIndex, chemicalIndex, tokenIndex);
						if(!reactionContainsSameChemicalAsSubstrateAndProduct(r))
						{
							possibleReactions.add(r);
						}
						
						r = new Reaction(cas, sentence, combList, beforeList, kwIndex, chemicalIndex, tokenIndex);
						if(!reactionContainsSameChemicalAsSubstrateAndProduct(r))
						{
							possibleReactions.add(r);
						}
						
						//Now create reactions with enzymes assigned, when the gene is not within a list
						//of reactants or products.
						for(Gene g : genes)
						{
							if(g.getEnd() < beforeList.get(0).getBegin()
									|| g.getBegin() > combList.get(combList.size()-1).getEnd()
									|| (
											g.getBegin() > beforeList.get(beforeList.size()-1).getEnd()
											&& g.getEnd() < combList.get(0).getBegin()
										)
							)
							{
								r = new Reaction(cas, sentence, beforeList, combList, kwIndex, chemicalIndex, tokenIndex, g);
								if(!reactionContainsSameChemicalAsSubstrateAndProduct(r))
								{
									possibleReactions.add(r);
								}
								
								r = new Reaction(cas, sentence, combList, beforeList, kwIndex, chemicalIndex, tokenIndex, g);
								if(!reactionContainsSameChemicalAsSubstrateAndProduct(r))
								{
									possibleReactions.add(r);
								}
							}
						}
					}
				}
				
				/* Do I need after? Or are they all sorted out before.
				if(after.size() > 0)
				{
					possibleReactions.add(new Reaction(cas, sentence, after, combList, kwIndex, chemicalIndex, tokenIndex));
					possibleReactions.add(new Reaction(cas, sentence, combList, after, kwIndex, chemicalIndex, tokenIndex));
					
					for(Gene g : genes)
					{
						if(g.getEnd() < combList.get(0).getBegin()
								|| g.getBegin() > after.get(after.size()-1).getEnd()
								|| (
										g.getBegin() > combList.get(combList.size()-1).getEnd()
										&& g.getEnd() < after.get(0).getBegin()
									)
						)
						{
							possibleReactions.add(new Reaction(cas, sentence, after, combList, kwIndex, chemicalIndex, tokenIndex, g));
							possibleReactions.add(new Reaction(cas, sentence, combList, after, kwIndex, chemicalIndex, tokenIndex, g));
						}
					}
				}
				*/
			}
			
			//If the score is below the threshold, remove the reaction. Of any overlapping reactions, keep the
			//highest scoring one.
			Set<Reaction> reactionsToExclude = new HashSet<Reaction>();
			for(Reaction possibleReaction : possibleReactions)
			{
				if(possibleReaction.getScore() < scores.getThreshold())
				{
					reactionsToExclude.add(possibleReaction);
					continue;
				}
				
				for(Reaction possibleReaction2 : possibleReactions)
				{
					if(possibleReaction2.getScore() >= scores.getThreshold()
							&& possibleReaction != possibleReaction2
							&& possibleReaction.overlapsWith(possibleReaction2))
					{
						if(possibleReaction.getScore() > possibleReaction2.getScore())
						{
							reactionsToExclude.add(possibleReaction2);
						}
						else if(possibleReaction2.getScore() > possibleReaction.getScore())
						{
							reactionsToExclude.add(possibleReaction);
						}
					}
				}
			}
			
			possibleReactions.removeAll(reactionsToExclude);
			
			//Create a CAS annotation for each reaction.
			for(Reaction possibleReaction : possibleReactions)
			{
				uk.ac.bbk.REx.types.Reaction casReaction = new uk.ac.bbk.REx.types.Reaction(cas);
				casReaction.setBegin(sentence.getBegin());
				casReaction.setEnd(sentence.getEnd());
				casReaction.setSubstrates(JCasUtils.createFSList(cas, possibleReaction.getSubstrates()));
				casReaction.setProducts(JCasUtils.createFSList(cas, possibleReaction.getProducts()));
				casReaction.setEnzyme(possibleReaction.getEnzyme());
				casReaction.setScore(possibleReaction.getScore());
                casReaction.setOrganisms(organismsFSList);
				
				casReaction.addToIndexes();
			}
		}
	}
	
	/**
	 * Get all contiguous combinations of a particular annotation type. For instance, given the sequence '123', the contiguous
	 * combinations returned would be: '1', '2', '3', '12', '23', '123'.
	 * 
	 * @param annotationsList
	 * @return
	 */
	private <A extends Annotation> Set<TreeMap<Integer, A>> getAllContiguousCombinations(List<A> annotationsList)
	{
		Set<TreeMap<Integer, A>> output = new HashSet<TreeMap<Integer, A>>();
		
		int combinationSize = 1;
		int startIndex;
		
		while(combinationSize <= annotationsList.size())
		{
			startIndex = 0;
			
			while(startIndex + combinationSize <= annotationsList.size())
			{
				TreeMap<Integer, A> currentCombination = new TreeMap<Integer, A>();
				for(int i=startIndex; i<startIndex+combinationSize; i++)
				{
					currentCombination.put(i, annotationsList.get(i));
				}
				output.add(currentCombination);
				
				startIndex++;
			}
			
			combinationSize++;
		}
		
		return output;
	}
	
	private class Reaction
	{
		private JCas cas;
		private Annotation sentence;
		private List<Chemical> substrates;
		private List<Chemical> products;
		private Gene enzyme;
		private double score;
		
		private CharacterIndex<ReactionKeyword> kwIndex;
		private CharacterIndex<Chemical> cIndex;
		private CharacterIndex<Token> tIndex;
		
		public Reaction(JCas aCas, 
				        Annotation aSentence, 
				        List<Chemical> someSubstrates, 
				        List<Chemical> someProducts, 
				        CharacterIndex<ReactionKeyword> aKwIndex, 
				        CharacterIndex<Chemical> aCIndex,
				        CharacterIndex<Token> aTIndex)
		{
			cas = aCas;
			sentence = aSentence;
			substrates = someSubstrates;
			products = someProducts;
			
			kwIndex = aKwIndex;
			cIndex = aCIndex;
			tIndex = aTIndex;
			
			calculateScore();
		}
		
		public Reaction(JCas aCas, 
				        Annotation aSentence, 
				        List<Chemical> someSubstrates, 
				        List<Chemical> someProducts, 
				        CharacterIndex<ReactionKeyword> aKwIndex, 
				        CharacterIndex<Chemical> aCIndex, 
				        CharacterIndex<Token> aTIndex,
				        Gene anEnzyme)
		{
			cas = aCas;
			sentence = aSentence;
			substrates = someSubstrates;
			products = someProducts;
			enzyme = anEnzyme;
			
			kwIndex = aKwIndex;
			cIndex = aCIndex;
			tIndex = aTIndex;
			
			calculateScore();
		}
		
		@Override
		public String toString()
		{
			StringBuilder output = new StringBuilder();
			for(Chemical substrate : substrates)
			{
				output.append(substrate.getCoveredText() + " + ");
			}
			
			output.replace(output.length()-2, output.length()-1, "");
			output.append("-> ");
			
			for(Chemical product : products)
			{
				output.append(product.getCoveredText() + " + ");
			}
			
			output.replace(output.length()-2, output.length()-1, "");
			
			output.append(" -- " + getScore());
			return output.toString();
		}
		
		public Annotation getSentence()
		{
			return sentence;
		}
		
		public List<Chemical> getSubstrates()
		{
			return substrates;
		}
		
		public List<Chemical> getProducts()
		{
			return products;
		}
		
		public Set<Chemical> getChemicals()
		{
			Set<Chemical> output = new HashSet<Chemical>();
			output.addAll(getSubstrates());
			output.addAll(getProducts());
			return output;
		}
		
		public Gene getEnzyme()
		{
			return enzyme;
		}
		
		public double getScore()
		{
			return score;
		}
		
		/**
		 * Determines whether the reaction overlaps with another given reaction. Two reactions overlap if
		 * if any chemical entity exists in both.
		 * 
		 * @param anotherReaction
		 * @return
		 */
		public boolean overlapsWith(Reaction anotherReaction)
		{
			for(Chemical c : getChemicals())
			{
				if(anotherReaction.getChemicals().contains(c))
				{
					return true;
				}
			}
			
			return false;
		}
		
		/**
		 * Calculate the score for this particular reaction.
		 */
		public void calculateScore()
		{
			score = 0;
			
			//Each reaction has an order of entities, for instance, enzyme-substrates-products (or ESP).
			//Each is scored slightly differently.
			
			//SP
			if(substrates.get(substrates.size()-1).getEnd() < products.get(0).getBegin()
					&& enzyme == null)
			{		
				score = (substrates.size() + products.size()) * scores.getEntity();
				
				List<ReactionKeyword> kwBefore = kwIndex.getAnnotationsBefore(substrates.get(0));

                if(hasGoodWordBeforeSmallMolecules(kwBefore, "reaction", "production", cIndex))
                {
                    score = score + scores.getKeyword();
                    //Get the last reaction word
                    List<ReactionKeyword> kwBeforeReverse = new ArrayList<ReactionKeyword>(kwBefore);
                    Collections.reverse(kwBeforeReverse);
                    for(ReactionKeyword rk : kwBeforeReverse)
                    {
                        if(rk.getKeywordType() == "reaction")
                        {
                            score = score - (tIndex.getAnnotationsBetween(rk, substrates.get(0)).size() * scores.getPhraseBetweenPenalty());
                            break;
                        }
                    }
                }
				
				//Between substrates and products
				List<Token> tokensBetween = tIndex.getAnnotationsBetween(substrates.get(substrates.size()-1), products.get(0));
				
				Set<String> secondWords = new HashSet<String>();
				secondWords.add("production");
				secondWords.add("to");
				int multiplier = goodWordsBetweenSmallMolecules(tokensBetween, "reaction", secondWords, "from", cIndex, kwIndex);
				score = score + scores.getKeyword()*multiplier;
				
				score = score - (wordsBetween(tIndex, cIndex, substrates.get(substrates.size()-1), products.get(0)) * scores.getPhraseBetweenPenalty());
			}
			//PS
			else if(products.get(products.size()-1).getEnd() < substrates.get(0).getBegin()
					&& enzyme == null)
			{
				score = (substrates.size() + products.size()) * scores.getEntity();
				
				//Add to score if there is a production word before the products.
				List<ReactionKeyword> kwBefore = kwIndex.getAnnotationsBefore(products.get(0));
				
				if(hasGoodWordBeforeSmallMolecules(kwBefore, "production", "reaction", cIndex))
				{
					score = score + scores.getKeyword();
					
					List<ReactionKeyword> kwBeforeReverse = new ArrayList<ReactionKeyword>(kwBefore);
					Collections.reverse(kwBeforeReverse);
					for(ReactionKeyword rk : kwBeforeReverse)
					{
						if(rk.getKeywordType() == "product")
						{
							score = score - (tIndex.getAnnotationsBetween(rk, substrates.get(0)).size() * scores.getPhraseBetweenPenalty());
							break;
						}
					}
				}
				
				//Between substrates and products
				List<Token> tokensBetween = tIndex.getAnnotationsBetween(products.get(products.size()-1), substrates.get(0));
				
				Set<String> secondWords = new HashSet<String>();
				secondWords.add("reaction");
				secondWords.add("from");
				int multiplier = goodWordsBetweenSmallMolecules(tokensBetween, "production", secondWords, "to", cIndex, kwIndex);
				score = score + scores.getKeyword()*multiplier;
				
				score = score - (wordsBetween(tIndex, cIndex, products.get(products.size()-1), substrates.get(0)) * scores.getPhraseBetweenPenalty());
			}
			//ESP
			else if(enzyme != null
					&& enzyme.getEnd() < substrates.get(0).getBegin()
					&& substrates.get(substrates.size()-1).getEnd() < products.get(0).getBegin())
			{
				score = (substrates.size() + products.size() + 1) * scores.getEntity();
				
				List<ReactionKeyword> kwBefore = kwIndex.getAnnotationsBetween(enzyme, substrates.get(0));

                if(hasGoodWordBeforeSmallMolecules(kwBefore, "reaction", "production", cIndex))
                {
                    score = score + scores.getKeyword();
                }
				
				//Between substrates and products
				List<Token> tokensBetween = tIndex.getAnnotationsBetween(substrates.get(substrates.size()-1), products.get(0));
				
				Set<String> secondWords = new HashSet<String>();
				secondWords.add("production");
				secondWords.add("to");
				int multiplier = goodWordsBetweenSmallMolecules(tokensBetween, "reaction", secondWords, "from", cIndex, kwIndex);
				score = score + scores.getKeyword()*multiplier;
				
				score = score - (wordsBetween(tIndex, cIndex, enzyme, substrates.get(0)) * scores.getEnzymeDistancePenalty());
				score = score - (wordsBetween(tIndex, cIndex, substrates.get(substrates.size()-1), products.get(0)) * scores.getPhraseBetweenPenalty());
			}
			//EPS
			else if(enzyme != null
					&& enzyme.getEnd() < products.get(0).getBegin()
					&& products.get(products.size()-1).getEnd() < substrates.get(0).getBegin())
			{
				score = (substrates.size() + products.size() + 1) * scores.getEntity();
				
				//Add to score if there is a reaction word between the enzyme and the products.
				List<ReactionKeyword> kwBefore = kwIndex.getAnnotationsBetween(enzyme, products.get(0));
				
				if(hasGoodWordBeforeSmallMolecules(kwBefore, "production", "reaction", cIndex))
				{
					score = score + scores.getKeyword();
				}
				
				//Between substrates and products
				List<Token> tokensBetween = tIndex.getAnnotationsBetween(products.get(products.size()-1), substrates.get(0));
				
				Set<String> secondWords = new HashSet<String>();
				secondWords.add("reaction");
				secondWords.add("from");
				int multiplier = goodWordsBetweenSmallMolecules(tokensBetween, "production", secondWords, "to", cIndex, kwIndex);
				score = score + scores.getKeyword()*multiplier;
				
				score = score - (wordsBetween(tIndex, cIndex, enzyme, products.get(0)) * scores.getEnzymeDistancePenalty());
				score = score - (wordsBetween(tIndex, cIndex, products.get(products.size()-1), substrates.get(0)) * scores.getPhraseBetweenPenalty());
			}
			//SPE
			else if(enzyme != null
					&& substrates.get(substrates.size()-1).getEnd() < products.get(0).getBegin()
					&& products.get(products.size()-1).getEnd() < enzyme.getBegin())
			{
				score = (substrates.size() + products.size() + 1) * scores.getEntity();
				
				List<ReactionKeyword> kwBefore = kwIndex.getAnnotationsBefore(substrates.get(0));
				
				boolean isAddPresent = false;
				boolean isToPresent = false;
				for(ReactionKeyword rw : kwBefore)
				{
					if(StemmerUtils.getStem(rw.getCoveredText()).equals("add")
							|| StemmerUtils.getStem(rw.getCoveredText()).equals("transfer"))
					{
						isAddPresent = true;
						break;
					}
				}
				
				List<ReactionKeyword> kwBetween = kwIndex.getAnnotationsBetween(substrates.get(0), substrates.get(substrates.size()-1));
				for(ReactionKeyword rw : kwBetween)
				{
					if(rw.getKeywordType().equals("to"))
					{
						isToPresent = true;
						break;
					}
				}
				
				if(isAddPresent && isToPresent)
				{
					score = score + (2*scores.getKeyword());
				}
				else
				{
					if(hasGoodWordBeforeSmallMolecules(kwBefore, "reaction", "production", cIndex))
					{
						score = score + scores.getKeyword();
					}
				}
				
				//Between substrates and products
				List<Token> tokensBetween = tIndex.getAnnotationsBetween(substrates.get(substrates.size()-1), products.get(0));
				
				Set<String> secondWords = new HashSet<String>();
				secondWords.add("production");
				secondWords.add("to");
				int multiplier = goodWordsBetweenSmallMolecules(tokensBetween, "reaction", secondWords, "from", cIndex, kwIndex);
				score = score + scores.getKeyword()*multiplier;
				
				score = score - (wordsBetween(tIndex, cIndex, substrates.get(substrates.size()-1), products.get(0)) * scores.getPhraseBetweenPenalty());
				score = score - (wordsBetween(tIndex, cIndex, products.get(products.size()-1), enzyme) * scores.getPhraseBetweenPenalty());
				
				//Between products and enzyme
				List<ReactionKeyword> kwAfter = kwIndex.getAnnotationsBetween(products.get(products.size()-1), enzyme);
				boolean hasBy = false;
				for(ReactionKeyword kw : kwAfter)
				{
					if(kw.getKeywordType().equals("by"))
					{
						hasBy = true;
					}
				}
				
				if(hasBy)
				{
					score = score + scores.getKeyword();
				}
			}
			//PSE
			else if(enzyme != null
					&& products.get(products.size()-1).getEnd() < substrates.get(0).getBegin()
					&& substrates.get(substrates.size()-1).getEnd() < enzyme.getBegin())
			{
				score = (substrates.size() + products.size() + 1) * scores.getEntity();
				
				//Add to score if there is a production word before the products.
				List<ReactionKeyword> kwBefore = kwIndex.getAnnotationsBefore(products.get(0));
				
				if(hasGoodWordBeforeSmallMolecules(kwBefore, "production", "reaction", cIndex))
				{
					score = score + scores.getKeyword();
				}
				
				//Between substrates and products
				List<Token> tokensBetween = tIndex.getAnnotationsBetween(products.get(products.size()-1), substrates.get(0));
				
				Set<String> secondWords = new HashSet<String>();
				secondWords.add("reaction");
				secondWords.add("from");
				int multiplier = goodWordsBetweenSmallMolecules(tokensBetween, "production", secondWords, "to", cIndex, kwIndex);
				score = score + scores.getKeyword()*multiplier;
				
				score = score - (wordsBetween(tIndex, cIndex, products.get(products.size()-1), substrates.get(0)) * scores.getPhraseBetweenPenalty());
				score = score - (wordsBetween(tIndex, cIndex, substrates.get(substrates.size()-1), enzyme) * scores.getPhraseBetweenPenalty());
				
				//Between substrates and enzyme
				List<ReactionKeyword> kwAfter = kwIndex.getAnnotationsBetween(substrates.get(substrates.size()-1), enzyme);
				boolean hasBy = false;
				for(ReactionKeyword kw : kwAfter)
				{
					if(kw.getKeywordType().equals("by"))
					{
						hasBy = true;
					}
				}
				
				if(hasBy)
				{
					score = score + scores.getKeyword();
				}
			}
			//SEP
			else if(enzyme != null
					&& substrates.get(substrates.size()-1).getEnd() < enzyme.getBegin()
					&& enzyme.getEnd() < products.get(0).getBegin())
			{
				score = (substrates.size() + products.size() + 1) * scores.getEntity();
				
				//Add to score if there is a reaction word before the substrates.
				List<ReactionKeyword> kwBefore = kwIndex.getAnnotationsBefore(substrates.get(0));
				
				if(hasGoodWordBeforeSmallMolecules(kwBefore, "reaction", "production", cIndex))
				{
					score = score + scores.getKeyword();
				}
				
				//Between substrates and enzyme
				List<ReactionKeyword> kwBetweenSE = kwIndex.getAnnotationsBetween(substrates.get(substrates.size()-1), enzyme);
				
				for(ReactionKeyword kw : kwBetweenSE)
				{
					if(kw.getKeywordType().equals("reaction")
							|| kw.getKeywordType().equals("by"))
					{
						score = score + scores.getKeyword();
						break;
					}
				}
				
				//Between enzyme and products
				List<ReactionKeyword> kwBetweenEP = kwIndex.getAnnotationsBetween(enzyme, products.get(0));
				
				for(ReactionKeyword kw : kwBetweenEP)
				{
					if(kw.getKeywordType().equals("production")
							|| kw.getKeywordType().equals("to"))
					{
						score = score + scores.getKeyword();
						break;
					}
				}
				
				score = score - (wordsBetween(tIndex, cIndex, substrates.get(substrates.size()-1), products.get(0)) * scores.getPhraseBetweenPenalty());
			}
			//PES
			else if(enzyme != null
					&& products.get(products.size()-1).getEnd() < enzyme.getBegin()
					&& enzyme.getEnd() < substrates.get(0).getBegin())
			{
				score = (substrates.size() + products.size() + 1) * scores.getEntity();
				
				//Add to score if there is a production word before the products.
				List<ReactionKeyword> kwBefore = kwIndex.getAnnotationsBefore(products.get(0));
				
				if(hasGoodWordBeforeSmallMolecules(kwBefore, "production", "reaction", cIndex))
				{
					score = score + scores.getKeyword();
				}
				
				//Between products and enzyme
				List<ReactionKeyword> kwBetweenPE = kwIndex.getAnnotationsBetween(products.get(products.size()-1), enzyme);
				
				for(ReactionKeyword kw : kwBetweenPE)
				{
					if(kw.getKeywordType().equals("production")
							|| kw.getKeywordType().equals("by"))
					{
						score = score + scores.getKeyword();
						break;
					}
				}
				
				//Between enzyme and products
				List<ReactionKeyword> kwBetweenES = kwIndex.getAnnotationsBetween(enzyme, substrates.get(0));
				
				for(ReactionKeyword kw : kwBetweenES)
				{
					if(kw.getKeywordType().equals("reaction")
							|| kw.getKeywordType().equals("from"))
					{
						score = score + scores.getKeyword();
						break;
					}
				}
				
				score = score - (wordsBetween(tIndex, cIndex, products.get(products.size()-1), substrates.get(0)) * scores.getPhraseBetweenPenalty());
			}
			
			score = score + bonuses();
			score = score - (wordsBetween(tIndex, cIndex, substrates.get(0), substrates.get(substrates.size()-1)) * scores.getPhraseBetweenPenalty());
			score = score - (wordsBetween(tIndex, cIndex, products.get(0), products.get(products.size()-1)) * scores.getPhraseBetweenPenalty());
		}
		
		/**
		 * Calculate the part of the score that is worked out the same for all reaction orders.
		 * 
		 * @return
		 */
		private double bonuses()
		{
			double andBonus = 0;
			
			if(substrates.size() > 1)
			{
				//Add a bonus if a list of substrates or products has the word 'and' between the last two entities.
				List<ReactionKeyword> kwBetweenLast2Substrates = kwIndex.getAnnotationsBetween(substrates.get(substrates.size()-2), substrates.get(substrates.size()-1));
				boolean andPresent = false;
				for(ReactionKeyword kw : kwBetweenLast2Substrates)
				{
					if(kw.getKeywordType().equals("and"))
					{
						andPresent = true;
					}
				}
				
				if(andPresent == false)
				{
					andBonus = andBonus - scores.getAnd();
				}
				
				//If the list contains certain keywords, subtract from the score.
				for(ReactionKeyword kw : kwIndex.getAnnotationsBetween(substrates.get(0), substrates.get(substrates.size()-1)))
				{
					if(kw.getKeywordType().equals("reaction")
							|| kw.getKeywordType().equals("production")
							|| kw.getKeywordType().equals("to")
							|| kw.getKeywordType().equals("from"))
					{
						andBonus = andBonus - scores.getEntity();
					}
				}
			}
			
			if(products.size() > 1)
			{
				List<ReactionKeyword> kwBetweenLast2Products = kwIndex.getAnnotationsBetween(products.get(products.size()-2), products.get(products.size()-1));
				boolean andPresent = false;
				for(ReactionKeyword kw : kwBetweenLast2Products)
				{
					if(kw.getKeywordType().equals("and"))
					{
						andPresent = true;
					}
				}
				
				if(andPresent == false)
				{
					andBonus = andBonus - scores.getAnd();
				}
				
				for(ReactionKeyword kw : kwIndex.getAnnotationsBetween(products.get(0), products.get(products.size()-1)))
				{
					if(kw.getKeywordType().equals("reaction")
							|| kw.getKeywordType().equals("production")
							|| kw.getKeywordType().equals("to")
							|| kw.getKeywordType().equals("from"))
					{
						andBonus = andBonus - scores.getEntity();
					}
				}
			}
			
			//Add to the score if the sentence contains 'catalyse' (or words from the same stem).
			for(Annotation annotation : cas.getAnnotationIndex(ReactionKeyword.type))
			{
				ReactionKeyword rk = (ReactionKeyword)annotation;
				
				if(rk.getKeywordType().equals("catalyse"))
				{
					andBonus = andBonus + scores.getAnd();
					break;
				}
			}
			
			return andBonus;
		}
	}
	
	/**
	 * Returns the number of tokens between two annotations.
	 * 
	 * @param tokenIndex
	 * @param chemicalIndex
	 * @param c1
	 * @param c2
	 * @return
	 */
	public int wordsBetween(CharacterIndex<Token> tokenIndex, CharacterIndex<Chemical> chemicalIndex, Annotation c1, Annotation c2)
	{
		int total = tokenIndex.getAnnotationsBetween(c1, c2).size();
		
		return total;
	}
	
	/**
	 * Looks for a type of keyword (good) before all chemicals. If a good word is present, and
	 * a bad word is not present between the good word an the chemicals, true is returned.
	 * 
	 * @param kws
	 * @param good
	 * @param bad
	 * @param cIndex
	 * @return
	 */
	public boolean hasGoodWordBeforeSmallMolecules(List<ReactionKeyword> kws, String good, String bad, CharacterIndex<Chemical> cIndex)
	{
		boolean hasGoodWord = false;
		boolean hasBadWordAfter = false;
		for(ReactionKeyword kw : kws)
		{
			if(kw.getKeywordType().equals(good))
			{
				hasGoodWord = true;
				hasBadWordAfter = false;
			}
			else if(hasGoodWord == true
					&& (kw.getKeywordType().equals(bad)))
			{
				hasBadWordAfter = true;
			}
		}
		
		if(hasGoodWord == true && hasBadWordAfter == false)
		{
			return true;
		}
		else
		{
			return false;
		}
	}
	
	/**
	 * Looks for a type of keyword, first, before a type of keyword, second, in between the substrates and products.
	 * 
	 * @param tokens
	 * @param first
	 * @param second
	 * @param bad
	 * @param cIndex
	 * @return
	 */
	public int goodWordsBetweenSmallMolecules(List<Token> tokens, String first, Set<String> second, String bad, CharacterIndex<Chemical> cIndex, CharacterIndex<ReactionKeyword> kwIndex)
	{
		boolean hasSecondWord = false;
		boolean hasFirstWordBefore = false;
		boolean hasFirstWordAfter = false;
		boolean hasChemical = false;
		boolean hasBadWord = false;
		boolean hasWith = false;
		for(Token token : tokens)
		{
			if(kwIndex.getOverlappingAnnotations(token).size() > 0)
			{
				ReactionKeyword kw = kwIndex.getOverlappingAnnotations(token).get(0);
				
				if((kw.getKeywordType().equals(first)
						&& hasSecondWord == false))
				{
					hasFirstWordBefore = true;
				}
				else if((kw.getKeywordType().equals(first)
						&& hasSecondWord == true))
				{
					hasFirstWordAfter = true;
				}
				else if(second.contains(kw.getKeywordType()))
				{
					hasSecondWord = true;
				}
				else if(kw.getKeywordType().equals(bad))
				{
					hasBadWord = true;
				}
				else if(kw.getKeywordType().equals("with"))
				{
					hasWith = true;
				}
			}
			else if(cIndex.getOverlappingAnnotations(token).size() > 0)
			{
				hasChemical = true;
			}
		}
		
		if(!hasChemical && !hasBadWord
				&& !hasFirstWordAfter
				&& hasSecondWord
				&& hasFirstWordBefore
				&& !hasWith)
		{
			return 2;
		}
		else if(!hasChemical && !hasBadWord
				&& !hasFirstWordAfter
				&& hasSecondWord
				&& !hasFirstWordBefore
				&& !hasWith)
		{
			return 1;
		}
		else
		{
			return 0;
		}
	}
	
	public boolean reactionContainsSameChemicalAsSubstrateAndProduct(Reaction r)
	{
		Set<String> substrates = new HashSet<String>();
		for(Chemical c : r.getSubstrates())
		{
			substrates.add(JCasUtils.getID(c));
		}
		
		Set<String> products = new HashSet<String>();
		for(Chemical c : r.getProducts())
		{
			products.add(JCasUtils.getID(c));
		}
		
		for(String s : substrates)
		{
			if(products.contains(s))
			{
				return true;
			}
		}
		
		return false;
	}
}
