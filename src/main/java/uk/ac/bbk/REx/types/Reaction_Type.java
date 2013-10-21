
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
public class Reaction_Type extends Annotation_Type {
  /** @generated */
  @Override
  protected FSGenerator getFSGenerator() {return fsGenerator;}
  /** @generated */
  private final FSGenerator fsGenerator = 
    new FSGenerator() {
      @Override
	public FeatureStructure createFS(int addr, CASImpl cas) {
  			 if (Reaction_Type.this.useExistingInstance) {
  			   // Return eq fs instance if already created
  		     FeatureStructure fs = Reaction_Type.this.jcas.getJfsFromCaddr(addr);
  		     if (null == fs) {
  		       fs = new Reaction(addr, Reaction_Type.this);
  			   Reaction_Type.this.jcas.putJfsFromCaddr(addr, fs);
  			   return fs;
  		     }
  		     return fs;
        } else return new Reaction(addr, Reaction_Type.this);
  	  }
    };
  /** @generated */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = Reaction.typeIndexID;
  /** @generated 
     @modifiable */
  @SuppressWarnings ("hiding")
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("uk.ac.bbk.REx.types.Reaction");
 
  /** @generated */
  final Feature casFeat_substrates;
  /** @generated */
  final int     casFeatCode_substrates;
  /** @generated */ 
  public int getSubstrates(int addr) {
        if (featOkTst && casFeat_substrates == null)
      jcas.throwFeatMissing("substrates", "uk.ac.bbk.REx.types.Reaction");
    return ll_cas.ll_getRefValue(addr, casFeatCode_substrates);
  }
  /** @generated */    
  public void setSubstrates(int addr, int v) {
        if (featOkTst && casFeat_substrates == null)
      jcas.throwFeatMissing("substrates", "uk.ac.bbk.REx.types.Reaction");
    ll_cas.ll_setRefValue(addr, casFeatCode_substrates, v);}
    
  
 
  /** @generated */
  final Feature casFeat_products;
  /** @generated */
  final int     casFeatCode_products;
  /** @generated */ 
  public int getProducts(int addr) {
        if (featOkTst && casFeat_products == null)
      jcas.throwFeatMissing("products", "uk.ac.bbk.REx.types.Reaction");
    return ll_cas.ll_getRefValue(addr, casFeatCode_products);
  }
  /** @generated */    
  public void setProducts(int addr, int v) {
        if (featOkTst && casFeat_products == null)
      jcas.throwFeatMissing("products", "uk.ac.bbk.REx.types.Reaction");
    ll_cas.ll_setRefValue(addr, casFeatCode_products, v);}
    
  
 
  /** @generated */
  final Feature casFeat_enzyme;
  /** @generated */
  final int     casFeatCode_enzyme;
  /** @generated */ 
  public int getEnzyme(int addr) {
        if (featOkTst && casFeat_enzyme == null)
      jcas.throwFeatMissing("enzyme", "uk.ac.bbk.REx.types.Reaction");
    return ll_cas.ll_getRefValue(addr, casFeatCode_enzyme);
  }
  /** @generated */    
  public void setEnzyme(int addr, int v) {
        if (featOkTst && casFeat_enzyme == null)
      jcas.throwFeatMissing("enzyme", "uk.ac.bbk.REx.types.Reaction");
    ll_cas.ll_setRefValue(addr, casFeatCode_enzyme, v);}
    
  
 
  /** @generated */
  final Feature casFeat_score;
  /** @generated */
  final int     casFeatCode_score;
  /** @generated */ 
  public double getScore(int addr) {
        if (featOkTst && casFeat_score == null)
      jcas.throwFeatMissing("score", "uk.ac.bbk.REx.types.Reaction");
    return ll_cas.ll_getDoubleValue(addr, casFeatCode_score);
  }
  /** @generated */    
  public void setScore(int addr, double v) {
        if (featOkTst && casFeat_score == null)
      jcas.throwFeatMissing("score", "uk.ac.bbk.REx.types.Reaction");
    ll_cas.ll_setDoubleValue(addr, casFeatCode_score, v);}
    
  
 
  /** @generated */
  final Feature casFeat_Organisms;
  /** @generated */
  final int     casFeatCode_Organisms;
  /** @generated */ 
  public int getOrganisms(int addr) {
        if (featOkTst && casFeat_Organisms == null)
      jcas.throwFeatMissing("Organisms", "uk.ac.bbk.REx.types.Reaction");
    return ll_cas.ll_getRefValue(addr, casFeatCode_Organisms);
  }
  /** @generated */    
  public void setOrganisms(int addr, int v) {
        if (featOkTst && casFeat_Organisms == null)
      jcas.throwFeatMissing("Organisms", "uk.ac.bbk.REx.types.Reaction");
    ll_cas.ll_setRefValue(addr, casFeatCode_Organisms, v);}
    
  



  /** initialize variables to correspond with Cas Type and Features
	* @generated */
  public Reaction_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

 
    casFeat_substrates = jcas.getRequiredFeatureDE(casType, "substrates", "uima.cas.FSList", featOkTst);
    casFeatCode_substrates  = (null == casFeat_substrates) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_substrates).getCode();

 
    casFeat_products = jcas.getRequiredFeatureDE(casType, "products", "uima.cas.FSList", featOkTst);
    casFeatCode_products  = (null == casFeat_products) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_products).getCode();

 
    casFeat_enzyme = jcas.getRequiredFeatureDE(casType, "enzyme", "uk.ac.bbk.REx.types.Gene", featOkTst);
    casFeatCode_enzyme  = (null == casFeat_enzyme) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_enzyme).getCode();

 
    casFeat_score = jcas.getRequiredFeatureDE(casType, "score", "uima.cas.Double", featOkTst);
    casFeatCode_score  = (null == casFeat_score) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_score).getCode();

 
    casFeat_Organisms = jcas.getRequiredFeatureDE(casType, "Organisms", "uima.cas.FSList", featOkTst);
    casFeatCode_Organisms  = (null == casFeat_Organisms) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_Organisms).getCode();

  }
}



    