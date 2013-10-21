

/* First created by JCasGen Thu Mar 07 12:25:36 GMT 2013 */
package hu.u_szeged.rgai.bio.uima.tagger;

import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;

import org.apache.uima.jcas.tcas.Annotation;


/** Species annotation type holds information for Linnaeus species tagger's annotation.
 * Updated by JCasGen Thu Mar 07 13:02:30 GMT 2013
 * XML source: /media/data/Dropbox/Uni/PhD/gitRepos/uk.ac.bbk.REx/REx/src/main/resources/uk/ac/bbk/desc/TypeSystem.xml
 * @generated */
public class LinnaeusSpecies extends Annotation {
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(LinnaeusSpecies.class);
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int type = typeIndexID;
  /** @generated  */
  @Override
  public              int getTypeIndexID() {return typeIndexID;}
 
  /** Never called.  Disable default constructor
   * @generated */
  protected LinnaeusSpecies() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated */
  public LinnaeusSpecies(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated */
  public LinnaeusSpecies(JCas jcas) {
    super(jcas);
    readObject();   
  } 

  /** @generated */  
  public LinnaeusSpecies(JCas jcas, int begin, int end) {
    super(jcas);
    setBegin(begin);
    setEnd(end);
    readObject();
  }   

  /** <!-- begin-user-doc -->
    * Write your own initialization here
    * <!-- end-user-doc -->
  @generated modifiable */
  private void readObject() {/*default - does nothing empty block */}
     
 
    
  //*--------------*
  //* Feature: mostProbableSpeciesId

  /** getter for mostProbableSpeciesId - gets This feature contains the value of the most probable NCBI Taxonomy Id for the annotated species occurence.
   * @generated */
  public String getMostProbableSpeciesId() {
    if (LinnaeusSpecies_Type.featOkTst && ((LinnaeusSpecies_Type)jcasType).casFeat_mostProbableSpeciesId == null)
      jcasType.jcas.throwFeatMissing("mostProbableSpeciesId", "hu.u_szeged.rgai.bio.uima.tagger.LinnaeusSpecies");
    return jcasType.ll_cas.ll_getStringValue(addr, ((LinnaeusSpecies_Type)jcasType).casFeatCode_mostProbableSpeciesId);}
    
  /** setter for mostProbableSpeciesId - sets This feature contains the value of the most probable NCBI Taxonomy Id for the annotated species occurence. 
   * @generated */
  public void setMostProbableSpeciesId(String v) {
    if (LinnaeusSpecies_Type.featOkTst && ((LinnaeusSpecies_Type)jcasType).casFeat_mostProbableSpeciesId == null)
      jcasType.jcas.throwFeatMissing("mostProbableSpeciesId", "hu.u_szeged.rgai.bio.uima.tagger.LinnaeusSpecies");
    jcasType.ll_cas.ll_setStringValue(addr, ((LinnaeusSpecies_Type)jcasType).casFeatCode_mostProbableSpeciesId, v);}    
   
    
  //*--------------*
  //* Feature: allIdsString

  /** getter for allIdsString - gets This feature contains all possible NCBI Taxonomy IDs.
   * @generated */
  public String getAllIdsString() {
    if (LinnaeusSpecies_Type.featOkTst && ((LinnaeusSpecies_Type)jcasType).casFeat_allIdsString == null)
      jcasType.jcas.throwFeatMissing("allIdsString", "hu.u_szeged.rgai.bio.uima.tagger.LinnaeusSpecies");
    return jcasType.ll_cas.ll_getStringValue(addr, ((LinnaeusSpecies_Type)jcasType).casFeatCode_allIdsString);}
    
  /** setter for allIdsString - sets This feature contains all possible NCBI Taxonomy IDs. 
   * @generated */
  public void setAllIdsString(String v) {
    if (LinnaeusSpecies_Type.featOkTst && ((LinnaeusSpecies_Type)jcasType).casFeat_allIdsString == null)
      jcasType.jcas.throwFeatMissing("allIdsString", "hu.u_szeged.rgai.bio.uima.tagger.LinnaeusSpecies");
    jcasType.ll_cas.ll_setStringValue(addr, ((LinnaeusSpecies_Type)jcasType).casFeatCode_allIdsString, v);}    
   
    
  //*--------------*
  //* Feature: ambigous

  /** getter for ambigous - gets True if the species tagging is ambigous.
   * @generated */
  public boolean getAmbigous() {
    if (LinnaeusSpecies_Type.featOkTst && ((LinnaeusSpecies_Type)jcasType).casFeat_ambigous == null)
      jcasType.jcas.throwFeatMissing("ambigous", "hu.u_szeged.rgai.bio.uima.tagger.LinnaeusSpecies");
    return jcasType.ll_cas.ll_getBooleanValue(addr, ((LinnaeusSpecies_Type)jcasType).casFeatCode_ambigous);}
    
  /** setter for ambigous - sets True if the species tagging is ambigous. 
   * @generated */
  public void setAmbigous(boolean v) {
    if (LinnaeusSpecies_Type.featOkTst && ((LinnaeusSpecies_Type)jcasType).casFeat_ambigous == null)
      jcasType.jcas.throwFeatMissing("ambigous", "hu.u_szeged.rgai.bio.uima.tagger.LinnaeusSpecies");
    jcasType.ll_cas.ll_setBooleanValue(addr, ((LinnaeusSpecies_Type)jcasType).casFeatCode_ambigous, v);}    
  }

    