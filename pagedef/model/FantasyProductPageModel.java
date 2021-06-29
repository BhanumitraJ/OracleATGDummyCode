package com.nm.commerce.pagedef.model;

import com.nm.commerce.pagedef.evaluator.product.FantasyProduct;

public class FantasyProductPageModel extends PageModel {
  private String returnUrl;
  private String returnCatDesc;
  private FantasyProduct fproduct;
  private boolean hasAltImg;
  
  public void setFproduct(FantasyProduct fproduct) {
    this.fproduct = fproduct;
  }
  
  public FantasyProduct getFproduct() {
    return fproduct;
  }
  
  public void setHasAltImg(boolean hasAltImg) {
    this.hasAltImg = hasAltImg;
  }
  
  public boolean isHasAltImg() {
    return hasAltImg;
  }
  
  public void setReturnUrl(String returnUrl) {
    this.returnUrl = returnUrl;
  }
  
  public String getReturnUrl() {
    return returnUrl;
  }
  
  public void setReturnCatDesc(String returnCatDesc) {
    this.returnCatDesc = returnCatDesc;
  }
  
  public String getReturnCatDesc() {
    return returnCatDesc;
  }
  
}
