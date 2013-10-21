

/* First created by JCasGen Thu Mar 07 12:25:36 GMT 2013 */
package uk.ac.bbk.REx.types;

import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;



/** 
 * Updated by JCasGen Thu Mar 07 13:02:30 GMT 2013
 * XML source: /media/data/Dropbox/Uni/PhD/gitRepos/uk.ac.bbk.REx/REx/src/main/resources/uk/ac/bbk/desc/TypeSystem.xml
 * @generated */
public class Annotation extends org.apache.uima.jcas.tcas.Annotation {
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(Annotation.class);
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
  protected Annotation() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated */
  public Annotation(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated */
  public Annotation(JCas jcas) {
    super(jcas);
    readObject();   
  } 

  /** @generated */  
  public Annotation(JCas jcas, int begin, int end) {
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
  //* Feature: isAcronym

  /** getter for isAcronym - gets 
   * @generated */
  public boolean getIsAcronym() {
    if (Annotation_Type.featOkTst && ((Annotation_Type)jcasType).casFeat_isAcronym == null)
      jcasType.jcas.throwFeatMissing("isAcronym", "uk.ac.bbk.REx.types.Annotation");
    return jcasType.ll_cas.ll_getBooleanValue(addr, ((Annotation_Type)jcasType).casFeatCode_isAcronym);}
    
  /** setter for isAcronym - sets  
   * @generated */
  public void setIsAcronym(boolean v) {
    if (Annotation_Type.featOkTst && ((Annotation_Type)jcasType).casFeat_isAcronym == null)
      jcasType.jcas.throwFeatMissing("isAcronym", "uk.ac.bbk.REx.types.Annotation");
    jcasType.ll_cas.ll_setBooleanValue(addr, ((Annotation_Type)jcasType).casFeatCode_isAcronym, v);}    
   
    
  //*--------------*
  //* Feature: isWithDefinition

  /** getter for isWithDefinition - gets 
   * @generated */
  public boolean getIsWithDefinition() {
    if (Annotation_Type.featOkTst && ((Annotation_Type)jcasType).casFeat_isWithDefinition == null)
      jcasType.jcas.throwFeatMissing("isWithDefinition", "uk.ac.bbk.REx.types.Annotation");
    return jcasType.ll_cas.ll_getBooleanValue(addr, ((Annotation_Type)jcasType).casFeatCode_isWithDefinition);}
    
  /** setter for isWithDefinition - sets  
   * @generated */
  public void setIsWithDefinition(boolean v) {
    if (Annotation_Type.featOkTst && ((Annotation_Type)jcasType).casFeat_isWithDefinition == null)
      jcasType.jcas.throwFeatMissing("isWithDefinition", "uk.ac.bbk.REx.types.Annotation");
    jcasType.ll_cas.ll_setBooleanValue(addr, ((Annotation_Type)jcasType).casFeatCode_isWithDefinition, v);}    
   
    
  //*--------------*
  //* Feature: refersTo

  /** getter for refersTo - gets 
   * @generated */
  public Annotation getRefersTo() {
    if (Annotation_Type.featOkTst && ((Annotation_Type)jcasType).casFeat_refersTo == null)
      jcasType.jcas.throwFeatMissing("refersTo", "uk.ac.bbk.REx.types.Annotation");
    return (Annotation)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((Annotation_Type)jcasType).casFeatCode_refersTo)));}
    
  /** setter for refersTo - sets  
   * @generated */
  public void setRefersTo(Annotation v) {
    if (Annotation_Type.featOkTst && ((Annotation_Type)jcasType).casFeat_refersTo == null)
      jcasType.jcas.throwFeatMissing("refersTo", "uk.ac.bbk.REx.types.Annotation");
    jcasType.ll_cas.ll_setRefValue(addr, ((Annotation_Type)jcasType).casFeatCode_refersTo, jcasType.ll_cas.ll_getFSRef(v));}    
  }

    