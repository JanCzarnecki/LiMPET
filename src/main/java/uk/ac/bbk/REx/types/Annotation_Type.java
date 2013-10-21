
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
public class Annotation_Type extends org.apache.uima.jcas.tcas.Annotation_Type {
  /** @generated */
  @Override
  protected FSGenerator getFSGenerator() {return fsGenerator;}
  /** @generated */
  private final FSGenerator fsGenerator = 
    new FSGenerator() {
      @Override
	public FeatureStructure createFS(int addr, CASImpl cas) {
  			 if (Annotation_Type.this.useExistingInstance) {
  			   // Return eq fs instance if already created
  		     FeatureStructure fs = Annotation_Type.this.jcas.getJfsFromCaddr(addr);
  		     if (null == fs) {
  		       fs = new Annotation(addr, Annotation_Type.this);
  			   Annotation_Type.this.jcas.putJfsFromCaddr(addr, fs);
  			   return fs;
  		     }
  		     return fs;
        } else return new Annotation(addr, Annotation_Type.this);
  	  }
    };
  /** @generated */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = Annotation.typeIndexID;
  /** @generated 
     @modifiable */
  @SuppressWarnings ("hiding")
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("uk.ac.bbk.REx.types.Annotation");
 
  /** @generated */
  final Feature casFeat_isAcronym;
  /** @generated */
  final int     casFeatCode_isAcronym;
  /** @generated */ 
  public boolean getIsAcronym(int addr) {
        if (featOkTst && casFeat_isAcronym == null)
      jcas.throwFeatMissing("isAcronym", "uk.ac.bbk.REx.types.Annotation");
    return ll_cas.ll_getBooleanValue(addr, casFeatCode_isAcronym);
  }
  /** @generated */    
  public void setIsAcronym(int addr, boolean v) {
        if (featOkTst && casFeat_isAcronym == null)
      jcas.throwFeatMissing("isAcronym", "uk.ac.bbk.REx.types.Annotation");
    ll_cas.ll_setBooleanValue(addr, casFeatCode_isAcronym, v);}
    
  
 
  /** @generated */
  final Feature casFeat_isWithDefinition;
  /** @generated */
  final int     casFeatCode_isWithDefinition;
  /** @generated */ 
  public boolean getIsWithDefinition(int addr) {
        if (featOkTst && casFeat_isWithDefinition == null)
      jcas.throwFeatMissing("isWithDefinition", "uk.ac.bbk.REx.types.Annotation");
    return ll_cas.ll_getBooleanValue(addr, casFeatCode_isWithDefinition);
  }
  /** @generated */    
  public void setIsWithDefinition(int addr, boolean v) {
        if (featOkTst && casFeat_isWithDefinition == null)
      jcas.throwFeatMissing("isWithDefinition", "uk.ac.bbk.REx.types.Annotation");
    ll_cas.ll_setBooleanValue(addr, casFeatCode_isWithDefinition, v);}
    
  
 
  /** @generated */
  final Feature casFeat_refersTo;
  /** @generated */
  final int     casFeatCode_refersTo;
  /** @generated */ 
  public int getRefersTo(int addr) {
        if (featOkTst && casFeat_refersTo == null)
      jcas.throwFeatMissing("refersTo", "uk.ac.bbk.REx.types.Annotation");
    return ll_cas.ll_getRefValue(addr, casFeatCode_refersTo);
  }
  /** @generated */    
  public void setRefersTo(int addr, int v) {
        if (featOkTst && casFeat_refersTo == null)
      jcas.throwFeatMissing("refersTo", "uk.ac.bbk.REx.types.Annotation");
    ll_cas.ll_setRefValue(addr, casFeatCode_refersTo, v);}
    
  



  /** initialize variables to correspond with Cas Type and Features
	* @generated */
  public Annotation_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

 
    casFeat_isAcronym = jcas.getRequiredFeatureDE(casType, "isAcronym", "uima.cas.Boolean", featOkTst);
    casFeatCode_isAcronym  = (null == casFeat_isAcronym) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_isAcronym).getCode();

 
    casFeat_isWithDefinition = jcas.getRequiredFeatureDE(casType, "isWithDefinition", "uima.cas.Boolean", featOkTst);
    casFeatCode_isWithDefinition  = (null == casFeat_isWithDefinition) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_isWithDefinition).getCode();

 
    casFeat_refersTo = jcas.getRequiredFeatureDE(casType, "refersTo", "uk.ac.bbk.REx.types.Annotation", featOkTst);
    casFeatCode_refersTo  = (null == casFeat_refersTo) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_refersTo).getCode();

  }
}



    