package com.nm.commerce.catalog.core;

public class NewProductFlag extends ProductFlag {
  
  public String getCompareDays() {
    return compareDays;
  }
  
  public void setCompareDays(String compareDays) {
    this.compareDays = compareDays;
  }
  
  private String compareDays = "-50";
  
  public String getAllowOverrideNew() {
    return allowOverrideNew;
  }
  
  public void setAllowOverrideNew(String allowOverrideNew) {
    this.allowOverrideNew = allowOverrideNew;
  }
  
  private String allowOverrideNew = "false";
  
}
