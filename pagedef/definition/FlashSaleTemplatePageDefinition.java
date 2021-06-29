package com.nm.commerce.pagedef.definition;

/**
 * Basic definition for a flash sale (fashion dash) template page. Configuration for main content area, layout, nav, etc.
 */
public class FlashSaleTemplatePageDefinition extends ProductTemplatePageDefinition {
  
  private String flashSaleCategoryId; // was BrandSpecs.saleCategoryId
  private String yourFlashSaleCategoryId; // was BrandSpecs.yourSalesId
  private String rootSaleCategoryId; // was BrandSpecs.rootCategoryId
  private String flashSaleDir; // refreshables directory
  private String showCountdownClock; // display a countdown clock if it's a fashion dash with image available, and a head_long.html refreshable
  private String mobilePageDefinitionPath;
  private Boolean countDownToPromoEnd = false;// LC Fashion Dash Promo Clock
  
  public FlashSaleTemplatePageDefinition() {
    super();
  }
  
  // getters and setters
  public String getFlashSaleCategoryId() {
    return getValue(flashSaleCategoryId, basis, "getFlashSaleCategoryId");
  }
  
  public void setFlashSaleCategoryId(String flashSaleCategoryId) {
    this.flashSaleCategoryId = flashSaleCategoryId;
  }
  
  public String getYourFlashSaleCategoryId() {
    return getValue(yourFlashSaleCategoryId, basis, "getYourFlashSaleCategoryId");
  }
  
  public void setYourFlashSaleCategoryId(String yourFlashSaleCategoryId) {
    this.yourFlashSaleCategoryId = yourFlashSaleCategoryId;
  }
  
  public String getRootSaleCategoryId() {
    return getValue(rootSaleCategoryId, basis, "getRootSaleCategoryId");
  }
  
  public void setRootSaleCategoryId(String rootSaleCategoryId) {
    this.rootSaleCategoryId = rootSaleCategoryId;
  }
  
  public String getFlashSaleDir() {
    return getValue(flashSaleDir, basis, "getFlashSaleDir");
  }
  
  public void setFlashSaleDir(String flashSaleDir) {
    this.flashSaleDir = flashSaleDir;
  }
  
  public String getShowCountdownClock() {
    return showCountdownClock;
  }
  
  public void setShowCountdownClock(String showCountdownClock) {
    this.showCountdownClock = showCountdownClock;
  }
  
  public String getMobilePageDefinitionPath() {
    return mobilePageDefinitionPath;
  }
  
  public void setMobilePageDefinitionPath(String mobilePageDefinitionPath) {
    this.mobilePageDefinitionPath = mobilePageDefinitionPath;
  }
  
  public Boolean getCountDownToPromoEnd() {
    return getValue(countDownToPromoEnd, basis, "getCountDownToPromoEnd");
  }
  
  public void setCountDownToPromoEnd(Boolean countDownToPromoEnd) {
    this.countDownToPromoEnd = countDownToPromoEnd;
  }
  
}
