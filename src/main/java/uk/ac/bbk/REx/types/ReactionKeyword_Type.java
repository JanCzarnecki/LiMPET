
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
import org.apache.uima.jcas.tcas.Annotation_Type;

/** 
 * Updated by JCasGen Thu Mar 07 13:02:30 GMT 2013
 * @generated */
public class ReactionKeyword_Type extends Annotation_Type {
  /** @generated */
  @Override
  protected FSGenerator getFSGenerator() {return fsGenerator;}
  /** @generated */
  private final FSGenerator fsGenerator = 
    new FSGenerator() {
      @Override
	public FeatureStructure createFS(int addr, CASImpl cas) {
  			 if (ReactionKeyword_Type.this.useExistingInstance) {
  			   // Return eq fs instance if already created
  		     FeatureStructure fs = ReactionKeyword_Type.this.jcas.getJfsFromCaddr(addr);
  		     if (null == fs) {
  		       fs = new ReactionKeyword(addr, ReactionKeyword_Type.this);
  			   ReactionKeyword_Type.this.jcas.putJfsFromCaddr(addr, fs);
  			   return fs;
  		     }
  		     return fs;
        } else return new ReactionKeyword(addr, ReactionKeyword_Type.this);
  	  }
    };
  /** @generated */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = ReactionKeyword.typeIndexID;
  /** @generated 
     @modifiable */
  @SuppressWarnings ("hiding")
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("uk.ac.bbk.REx.types.ReactionKeyword");
 
  /** @generated */
  final Feature casFeat_keywordType;
  /** @generated */
  final int     casFeatCode_keywordType;
  /** @generated */ 
  public String getKeywordType(int addr) {
        if (featOkTst && casFeat_keywordType == null)
      jcas.throwFeatMissing("keywordType", "uk.ac.bbk.REx.types.ReactionKeyword");
    return ll_cas.ll_getStringValue(addr, casFeatCode_keywordType);
  }
  /** @generated */    
  public void setKeywordType(int addr, String v) {
        if (featOkTst && casFeat_keywordType == null)
      jcas.throwFeatMissing("keywordType", "uk.ac.bbk.REx.types.ReactionKeyword");
    ll_cas.ll_setStringValue(addr, casFeatCode_keywordType, v);}
    
  



  /** initialize variables to correspond with Cas Type and Features
	* @generated */
  public ReactionKeyword_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

 
    casFeat_keywordType = jcas.getRequiredFeatureDE(casType, "keywordType", "uima.cas.String", featOkTst);
    casFeatCode_keywordType  = (null == casFeat_keywordType) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_keywordType).getCode();

  }
}



    