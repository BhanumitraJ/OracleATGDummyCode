package com.nm.commerce.pagedef.definition;

public class EndecaFlashSaleTemplatePageDefinition extends EndecaTemplatePageDefinition {
  
  private String mobilePageDefinitionPath;
  private String flashSaleCategoryId;
  private String showCountdownClock; // display a countdown clock if it's a fashion dash with image available, and a head_long.html refreshable
  private Boolean countDownToPromoEnd = false; // LC Fashion Dash Promo Clock
  
  public String getFlashSaleCategoryId() {
    return getValue(flashSaleCategoryId, basis, "getFlashSaleCategoryId");
  }
  
  public void setFlashSaleCategoryId(String flashSaleCategoryId) {
    this.flashSaleCategoryId = flashSaleCategoryId;
  }
  
  public String getMobilePageDefinitionPath() {
    return getValue(mobilePageDefinitionPath, basis, "getMobilePageDefinitionPath");
  }
  
  public void setMobilePageDefinitionPath(String mobilePageDefinitionPath) {
    this.mobilePageDefinitionPath = mobilePageDefinitionPath;
  }
  
  public String getShowCountdownClock() {
    return showCountdownClock;
  }
  
  public void setShowCountdownClock(String showCountdownClock) {
    this.showCountdownClock = showCountdownClock;
  }
  
  public Boolean getCountDownToPromoEnd() {
    return getValue(countDownToPromoEnd, basis, "getCountDownToPromoEnd");
  }
  
  public void setCountDownToPromoEnd(Boolean countDownToPromoEnd) {
    this.countDownToPromoEnd = countDownToPromoEnd;
  }
  
}
