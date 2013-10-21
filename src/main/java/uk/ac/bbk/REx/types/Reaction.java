

/* First created by JCasGen Thu Mar 07 13:02:30 GMT 2013 */
package uk.ac.bbk.REx.types;

import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;

import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.jcas.tcas.Annotation;


/** 
 * Updated by JCasGen Thu Mar 07 13:02:30 GMT 2013
 * XML source: /media/data/Dropbox/Uni/PhD/gitRepos/uk.ac.bbk.REx/REx/src/main/resources/uk/ac/bbk/desc/TypeSystem.xml
 * @generated */
public class Reaction extends Annotation {
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(Reaction.class);
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
  protected Reaction() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated */
  public Reaction(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated */
  public Reaction(JCas jcas) {
    super(jcas);
    readObject();   
  } 

  /** @generated */  
  public Reaction(JCas jcas, int begin, int end) {
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
  //* Feature: substrates

  /** getter for substrates - gets 
   * @generated */
  public FSList getSubstrates() {
    if (Reaction_Type.featOkTst && ((Reaction_Type)jcasType).casFeat_substrates == null)
      jcasType.jcas.throwFeatMissing("substrates", "uk.ac.bbk.REx.types.Reaction");
    return (FSList)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((Reaction_Type)jcasType).casFeatCode_substrates)));}
    
  /** setter for substrates - sets  
   * @generated */
  public void setSubstrates(FSList v) {
    if (Reaction_Type.featOkTst && ((Reaction_Type)jcasType).casFeat_substrates == null)
      jcasType.jcas.throwFeatMissing("substrates", "uk.ac.bbk.REx.types.Reaction");
    jcasType.ll_cas.ll_setRefValue(addr, ((Reaction_Type)jcasType).casFeatCode_substrates, jcasType.ll_cas.ll_getFSRef(v));}    
   
    
  //*--------------*
  //* Feature: products

  /** getter for products - gets 
   * @generated */
  public FSList getProducts() {
    if (Reaction_Type.featOkTst && ((Reaction_Type)jcasType).casFeat_products == null)
      jcasType.jcas.throwFeatMissing("products", "uk.ac.bbk.REx.types.Reaction");
    return (FSList)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((Reaction_Type)jcasType).casFeatCode_products)));}
    
  /** setter for products - sets  
   * @generated */
  public void setProducts(FSList v) {
    if (Reaction_Type.featOkTst && ((Reaction_Type)jcasType).casFeat_products == null)
      jcasType.jcas.throwFeatMissing("products", "uk.ac.bbk.REx.types.Reaction");
    jcasType.ll_cas.ll_setRefValue(addr, ((Reaction_Type)jcasType).casFeatCode_products, jcasType.ll_cas.ll_getFSRef(v));}    
   
    
  //*--------------*
  //* Feature: enzyme

  /** getter for enzyme - gets 
   * @generated */
  public Gene getEnzyme() {
    if (Reaction_Type.featOkTst && ((Reaction_Type)jcasType).casFeat_enzyme == null)
      jcasType.jcas.throwFeatMissing("enzyme", "uk.ac.bbk.REx.types.Reaction");
    return (Gene)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((Reaction_Type)jcasType).casFeatCode_enzyme)));}
    
  /** setter for enzyme - sets  
   * @generated */
  public void setEnzyme(Gene v) {
    if (Reaction_Type.featOkTst && ((Reaction_Type)jcasType).casFeat_enzyme == null)
      jcasType.jcas.throwFeatMissing("enzyme", "uk.ac.bbk.REx.types.Reaction");
    jcasType.ll_cas.ll_setRefValue(addr, ((Reaction_Type)jcasType).casFeatCode_enzyme, jcasType.ll_cas.ll_getFSRef(v));}    
   
    
  //*--------------*
  //* Feature: score

  /** getter for score - gets 
   * @generated */
  public double getScore() {
    if (Reaction_Type.featOkTst && ((Reaction_Type)jcasType).casFeat_score == null)
      jcasType.jcas.throwFeatMissing("score", "uk.ac.bbk.REx.types.Reaction");
    return jcasType.ll_cas.ll_getDoubleValue(addr, ((Reaction_Type)jcasType).casFeatCode_score);}
    
  /** setter for score - sets  
   * @generated */
  public void setScore(double v) {
    if (Reaction_Type.featOkTst && ((Reaction_Type)jcasType).casFeat_score == null)
      jcasType.jcas.throwFeatMissing("score", "uk.ac.bbk.REx.types.Reaction");
    jcasType.ll_cas.ll_setDoubleValue(addr, ((Reaction_Type)jcasType).casFeatCode_score, v);}    
   
    
  //*--------------*
  //* Feature: Organisms

  /** getter for Organisms - gets 
   * @generated */
  public FSList getOrganisms() {
    if (Reaction_Type.featOkTst && ((Reaction_Type)jcasType).casFeat_Organisms == null)
      jcasType.jcas.throwFeatMissing("Organisms", "uk.ac.bbk.REx.types.Reaction");
    return (FSList)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((Reaction_Type)jcasType).casFeatCode_Organisms)));}
    
  /** setter for Organisms - sets  
   * @generated */
  public void setOrganisms(FSList v) {
    if (Reaction_Type.featOkTst && ((Reaction_Type)jcasType).casFeat_Organisms == null)
      jcasType.jcas.throwFeatMissing("Organisms", "uk.ac.bbk.REx.types.Reaction");
    jcasType.ll_cas.ll_setRefValue(addr, ((Reaction_Type)jcasType).casFeatCode_Organisms, jcasType.ll_cas.ll_getFSRef(v));}    
  }

    