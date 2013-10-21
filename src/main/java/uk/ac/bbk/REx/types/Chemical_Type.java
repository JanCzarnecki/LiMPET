
/* First created by JCasGen Thu Mar 07 12:25:36 GMT 2013 */
package uk.ac.bbk.REx.types;

import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.FSGenerator;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.FeatureImpl;
import org.apache.uima.cas.Feature;

/** 
 * Updated by JCasGen Thu Mar 07 13:02:30 GMT 2013
 * @generated */
public class Chemical_Type extends Annotation_Type {
  /** @generated */
  @Override
  protected FSGenerator getFSGenerator() {return fsGenerator;}
  /** @generated */
  private final FSGenerator fsGenerator = 
    new FSGenerator() {
      @Override
	public FeatureStructure createFS(int addr, CASImpl cas) {
  			 if (Chemical_Type.this.useExistingInstance) {
  			   // Return eq fs instance if already created
  		     FeatureStructure fs = Chemical_Type.this.jcas.getJfsFromCaddr(addr);
  		     if (null == fs) {
  		       fs = new Chemical(addr, Chemical_Type.this);
  			   Chemical_Type.this.jcas.putJfsFromCaddr(addr, fs);
  			   return fs;
  		     }
  		     return fs;
        } else return new Chemical(addr, Chemical_Type.this);
  	  }
    };
  /** @generated */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = Chemical.typeIndexID;
  /** @generated 
     @modifiable */
  @SuppressWarnings ("hiding")
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("uk.ac.bbk.REx.types.Chemical");
 
  /** @generated */
  final Feature casFeat_smilesString;
  /** @generated */
  final int     casFeatCode_smilesString;
  /** @generated */ 
  public String getSmilesString(int addr) {
        if (featOkTst && casFeat_smilesString == null)
      jcas.throwFeatMissing("smilesString", "uk.ac.bbk.REx.types.Chemical");
    return ll_cas.ll_getStringValue(addr, casFeatCode_smilesString);
  }
  /** @generated */    
  public void setSmilesString(int addr, String v) {
        if (featOkTst && casFeat_smilesString == null)
      jcas.throwFeatMissing("smilesString", "uk.ac.bbk.REx.types.Chemical");
    ll_cas.ll_setStringValue(addr, casFeatCode_smilesString, v);}
    
  
 
  /** @generated */
  final Feature casFeat_inChiString;
  /** @generated */
  final int     casFeatCode_inChiString;
  /** @generated */ 
  public String getInChiString(int addr) {
        if (featOkTst && casFeat_inChiString == null)
      jcas.throwFeatMissing("inChiString", "uk.ac.bbk.REx.types.Chemical");
    return ll_cas.ll_getStringValue(addr, casFeatCode_inChiString);
  }
  /** @generated */    
  public void setInChiString(int addr, String v) {
        if (featOkTst && casFeat_inChiString == null)
      jcas.throwFeatMissing("inChiString", "uk.ac.bbk.REx.types.Chemical");
    ll_cas.ll_setStringValue(addr, casFeatCode_inChiString, v);}
    
  
 
  /** @generated */
  final Feature casFeat_confidence;
  /** @generated */
  final int     casFeatCode_confidence;
  /** @generated */ 
  public double getConfidence(int addr) {
        if (featOkTst && casFeat_confidence == null)
      jcas.throwFeatMissing("confidence", "uk.ac.bbk.REx.types.Chemical");
    return ll_cas.ll_getDoubleValue(addr, casFeatCode_confidence);
  }
  /** @generated */    
  public void setConfidence(int addr, double v) {
        if (featOkTst && casFeat_confidence == null)
      jcas.throwFeatMissing("confidence", "uk.ac.bbk.REx.types.Chemical");
    ll_cas.ll_setDoubleValue(addr, casFeatCode_confidence, v);}
    
  
 
  /** @generated */
  final Feature casFeat_chebiID;
  /** @generated */
  final int     casFeatCode_chebiID;
  /** @generated */ 
  public String getChebiID(int addr) {
        if (featOkTst && casFeat_chebiID == null)
      jcas.throwFeatMissing("chebiID", "uk.ac.bbk.REx.types.Chemical");
    return ll_cas.ll_getStringValue(addr, casFeatCode_chebiID);
  }
  /** @generated */    
  public void setChebiID(int addr, String v) {
        if (featOkTst && casFeat_chebiID == null)
      jcas.throwFeatMissing("chebiID", "uk.ac.bbk.REx.types.Chemical");
    ll_cas.ll_setStringValue(addr, casFeatCode_chebiID, v);}
    
  



  /** initialize variables to correspond with Cas Type and Features
	* @generated */
  public Chemical_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

 
    casFeat_smilesString = jcas.getRequiredFeatureDE(casType, "smilesString", "uima.cas.String", featOkTst);
    casFeatCode_smilesString  = (null == casFeat_smilesString) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_smilesString).getCode();

 
    casFeat_inChiString = jcas.getRequiredFeatureDE(casType, "inChiString", "uima.cas.String", featOkTst);
    casFeatCode_inChiString  = (null == casFeat_inChiString) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_inChiString).getCode();

 
    casFeat_confidence = jcas.getRequiredFeatureDE(casType, "confidence", "uima.cas.Double", featOkTst);
    casFeatCode_confidence  = (null == casFeat_confidence) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_confidence).getCode();

 
    casFeat_chebiID = jcas.getRequiredFeatureDE(casType, "chebiID", "uima.cas.String", featOkTst);
    casFeatCode_chebiID  = (null == casFeat_chebiID) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_chebiID).getCode();

  }
}



    