

/* First created by JCasGen Thu Mar 07 12:25:36 GMT 2013 */
package banner.types.uima;

import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;

import org.apache.uima.jcas.tcas.Annotation;


/** stores information about the source of the input
 * Updated by JCasGen Thu Mar 07 13:02:30 GMT 2013
 * XML source: /media/data/Dropbox/Uni/PhD/gitRepos/uk.ac.bbk.REx/REx/src/main/resources/uk/ac/bbk/desc/TypeSystem.xml
 * @generated */
public class Source extends Annotation {
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(Source.class);
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
  protected Source() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated */
  public Source(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated */
  public Source(JCas jcas) {
    super(jcas);
    readObject();   
  } 

  /** @generated */  
  public Source(JCas jcas, int begin, int end) {
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
  //* Feature: filePath

  /** getter for filePath - gets file path of the source
   * @generated */
  public String getFilePath() {
    if (Source_Type.featOkTst && ((Source_Type)jcasType).casFeat_filePath == null)
      jcasType.jcas.throwFeatMissing("filePath", "banner.types.uima.Source");
    return jcasType.ll_cas.ll_getStringValue(addr, ((Source_Type)jcasType).casFeatCode_filePath);}
    
  /** setter for filePath - sets file path of the source 
   * @generated */
  public void setFilePath(String v) {
    if (Source_Type.featOkTst && ((Source_Type)jcasType).casFeat_filePath == null)
      jcasType.jcas.throwFeatMissing("filePath", "banner.types.uima.Source");
    jcasType.ll_cas.ll_setStringValue(addr, ((Source_Type)jcasType).casFeatCode_filePath, v);}    
   
    
  //*--------------*
  //* Feature: text

  /** getter for text - gets text in the file or line
   * @generated */
  public String getText() {
    if (Source_Type.featOkTst && ((Source_Type)jcasType).casFeat_text == null)
      jcasType.jcas.throwFeatMissing("text", "banner.types.uima.Source");
    return jcasType.ll_cas.ll_getStringValue(addr, ((Source_Type)jcasType).casFeatCode_text);}
    
  /** setter for text - sets text in the file or line 
   * @generated */
  public void setText(String v) {
    if (Source_Type.featOkTst && ((Source_Type)jcasType).casFeat_text == null)
      jcasType.jcas.throwFeatMissing("text", "banner.types.uima.Source");
    jcasType.ll_cas.ll_setStringValue(addr, ((Source_Type)jcasType).casFeatCode_text, v);}    
  }

    