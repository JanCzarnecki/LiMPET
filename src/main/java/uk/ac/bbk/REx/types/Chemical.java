

/* First created by JCasGen Thu Mar 07 12:25:36 GMT 2013 */
package uk.ac.bbk.REx.types;

import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;



/** 
 * Updated by JCasGen Thu Mar 07 13:02:30 GMT 2013
 * XML source: /media/data/Dropbox/Uni/PhD/gitRepos/uk.ac.bbk.REx/REx/src/main/resources/uk/ac/bbk/desc/TypeSystem.xml
 * @generated */
public class Chemical extends Annotation {
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(Chemical.class);
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
  protected Chemical() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated */
  public Chemical(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated */
  public Chemical(JCas jcas) {
    super(jcas);
    readObject();   
  } 

  /** @generated */  
  public Chemical(JCas jcas, int begin, int end) {
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
  //* Feature: smilesString

  /** getter for smilesString - gets 
   * @generated */
  public String getSmilesString() {
    if (Chemical_Type.featOkTst && ((Chemical_Type)jcasType).casFeat_smilesString == null)
      jcasType.jcas.throwFeatMissing("smilesString", "uk.ac.bbk.REx.types.Chemical");
    return jcasType.ll_cas.ll_getStringValue(addr, ((Chemical_Type)jcasType).casFeatCode_smilesString);}
    
  /** setter for smilesString - sets  
   * @generated */
  public void setSmilesString(String v) {
    if (Chemical_Type.featOkTst && ((Chemical_Type)jcasType).casFeat_smilesString == null)
      jcasType.jcas.throwFeatMissing("smilesString", "uk.ac.bbk.REx.types.Chemical");
    jcasType.ll_cas.ll_setStringValue(addr, ((Chemical_Type)jcasType).casFeatCode_smilesString, v);}    
   
    
  //*--------------*
  //* Feature: inChiString

  /** getter for inChiString - gets 
   * @generated */
  public String getInChiString() {
    if (Chemical_Type.featOkTst && ((Chemical_Type)jcasType).casFeat_inChiString == null)
      jcasType.jcas.throwFeatMissing("inChiString", "uk.ac.bbk.REx.types.Chemical");
    return jcasType.ll_cas.ll_getStringValue(addr, ((Chemical_Type)jcasType).casFeatCode_inChiString);}
    
  /** setter for inChiString - sets  
   * @generated */
  public void setInChiString(String v) {
    if (Chemical_Type.featOkTst && ((Chemical_Type)jcasType).casFeat_inChiString == null)
      jcasType.jcas.throwFeatMissing("inChiString", "uk.ac.bbk.REx.types.Chemical");
    jcasType.ll_cas.ll_setStringValue(addr, ((Chemical_Type)jcasType).casFeatCode_inChiString, v);}    
   
    
  //*--------------*
  //* Feature: confidence

  /** getter for confidence - gets 
   * @generated */
  public double getConfidence() {
    if (Chemical_Type.featOkTst && ((Chemical_Type)jcasType).casFeat_confidence == null)
      jcasType.jcas.throwFeatMissing("confidence", "uk.ac.bbk.REx.types.Chemical");
    return jcasType.ll_cas.ll_getDoubleValue(addr, ((Chemical_Type)jcasType).casFeatCode_confidence);}
    
  /** setter for confidence - sets  
   * @generated */
  public void setConfidence(double v) {
    if (Chemical_Type.featOkTst && ((Chemical_Type)jcasType).casFeat_confidence == null)
      jcasType.jcas.throwFeatMissing("confidence", "uk.ac.bbk.REx.types.Chemical");
    jcasType.ll_cas.ll_setDoubleValue(addr, ((Chemical_Type)jcasType).casFeatCode_confidence, v);}    
   
    
  //*--------------*
  //* Feature: chebiID

  /** getter for chebiID - gets 
   * @generated */
  public String getChebiID() {
    if (Chemical_Type.featOkTst && ((Chemical_Type)jcasType).casFeat_chebiID == null)
      jcasType.jcas.throwFeatMissing("chebiID", "uk.ac.bbk.REx.types.Chemical");
    return jcasType.ll_cas.ll_getStringValue(addr, ((Chemical_Type)jcasType).casFeatCode_chebiID);}
    
  /** setter for chebiID - sets  
   * @generated */
  public void setChebiID(String v) {
    if (Chemical_Type.featOkTst && ((Chemical_Type)jcasType).casFeat_chebiID == null)
      jcasType.jcas.throwFeatMissing("chebiID", "uk.ac.bbk.REx.types.Chemical");
    jcasType.ll_cas.ll_setStringValue(addr, ((Chemical_Type)jcasType).casFeatCode_chebiID, v);}    
  }

    