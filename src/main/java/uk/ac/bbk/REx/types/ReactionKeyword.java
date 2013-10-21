

/* First created by JCasGen Thu Mar 07 12:25:36 GMT 2013 */
package uk.ac.bbk.REx.types;

import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;

import org.apache.uima.jcas.tcas.Annotation;


/** 
 * Updated by JCasGen Thu Mar 07 13:02:30 GMT 2013
 * XML source: /media/data/Dropbox/Uni/PhD/gitRepos/uk.ac.bbk.REx/REx/src/main/resources/uk/ac/bbk/desc/TypeSystem.xml
 * @generated */
public class ReactionKeyword extends Annotation {
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(ReactionKeyword.class);
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
  protected ReactionKeyword() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated */
  public ReactionKeyword(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated */
  public ReactionKeyword(JCas jcas) {
    super(jcas);
    readObject();   
  } 

  /** @generated */  
  public ReactionKeyword(JCas jcas, int begin, int end) {
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
  //* Feature: keywordType

  /** getter for keywordType - gets 
   * @generated */
  public String getKeywordType() {
    if (ReactionKeyword_Type.featOkTst && ((ReactionKeyword_Type)jcasType).casFeat_keywordType == null)
      jcasType.jcas.throwFeatMissing("keywordType", "uk.ac.bbk.REx.types.ReactionKeyword");
    return jcasType.ll_cas.ll_getStringValue(addr, ((ReactionKeyword_Type)jcasType).casFeatCode_keywordType);}
    
  /** setter for keywordType - sets  
   * @generated */
  public void setKeywordType(String v) {
    if (ReactionKeyword_Type.featOkTst && ((ReactionKeyword_Type)jcasType).casFeat_keywordType == null)
      jcasType.jcas.throwFeatMissing("keywordType", "uk.ac.bbk.REx.types.ReactionKeyword");
    jcasType.ll_cas.ll_setStringValue(addr, ((ReactionKeyword_Type)jcasType).casFeatCode_keywordType, v);}    
  }

    