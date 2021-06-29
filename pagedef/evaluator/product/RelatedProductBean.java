package com.nm.commerce.pagedef.evaluator.product;

import atg.repository.RepositoryItem;

import com.nm.commerce.catalog.NMProduct;
import com.nm.commerce.catalog.NMSuite;

/**
 * This bean contains all the values needed in jsp for a related product to be displayed in ymal/ctl section This bean is populated by ProductPageEvaluator class
 * 
 * @author nmjh94
 * 
 */
public class RelatedProductBean {
  private String imageUrl;
  private NMProduct product;
  private NMSuite parentSuite;
  private String designerName;
  private String cmosCatalogId;
  private String cmosItemCode;
  private String parentCmosCatalogId;
  private String parentCmosItemCode;
  private Boolean flgPricingAdornments;
  private RepositoryItem productDataSource;
  private Boolean isInSuite;
  private String relatedProdId;
  private String recommendationSource = "NM";
  
  public RelatedProductBean() {}
  
  public void setRecommendationSource(String value) {
    recommendationSource = value;
  }
  
  public String getRecommendationSource() {
    return recommendationSource;
  }
  
  public void setImageUrl(String imageUrl) {
    this.imageUrl = imageUrl;
  }
  
  public String getImageUrl() {
    return imageUrl;
  }
  
  public void setProduct(NMProduct product) {
    this.product = product;
  }
  
  public NMProduct getProduct() {
    return product;
  }
  
  public void setParentSuite(NMSuite parentSuite) {
    this.parentSuite = parentSuite;
  }
  
  public NMSuite getParentSuite() {
    return parentSuite;
  }
  
  public void setDesignerName(String designerName) {
    this.designerName = designerName;
  }
  
  public String getDesignerName() {
    return designerName;
  }
  
  public void setCmosCatalogId(String cmosCatalogId) {
    this.cmosCatalogId = cmosCatalogId;
  }
  
  public String getCmosCatalogId() {
    return cmosCatalogId;
  }
  
  public void setCmosItemCode(String cmosItemCode) {
    this.cmosItemCode = cmosItemCode;
  }
  
  public String getCmosItemCode() {
    return cmosItemCode;
  }
  
  public void setParentCmosCatalogId(String parentCmosCatalogId) {
    this.parentCmosCatalogId = parentCmosCatalogId;
  }
  
  public String getParentCmosCatalogId() {
    return parentCmosCatalogId;
  }
  
  public void setParentCmosItemCode(String parentCmosItemCode) {
    this.parentCmosItemCode = parentCmosItemCode;
  }
  
  public String getParentCmosItemCode() {
    return parentCmosItemCode;
  }
  
  public void setFlgPricingAdornments(Boolean flgPricingAdornments) {
    this.flgPricingAdornments = flgPricingAdornments;
  }
  
  public Boolean getFlgPricingAdornments() {
    return flgPricingAdornments;
  }
  
  public void setProductDataSource(RepositoryItem productDataSource) {
    this.productDataSource = productDataSource;
  }
  
  public RepositoryItem getProductDataSource() {
    return productDataSource;
  }
  
  public void setIsInSuite(Boolean isInSuite) {
    this.isInSuite = isInSuite;
  }
  
  public Boolean getIsInSuite() {
    return isInSuite;
  }
  
  public void setRelatedProdId(String relatedProdId) {
    this.relatedProdId = relatedProdId;
  }
  
  public String getRelatedProdId() {
    return relatedProdId;
  }
  
}
