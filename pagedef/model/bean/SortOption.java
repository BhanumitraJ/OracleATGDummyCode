package com.nm.commerce.pagedef.model.bean;

public class SortOption {
  private String mLabel;
  private String mValue;
  private String mOrder;
  
  public SortOption(String label, String value, String order) {
    mLabel = label;
    mValue = value;
    mOrder = order;
  }
  
  public String getLabel() {
    return mLabel;
  }
  
  public String getValue() {
    return mValue;
  }
  
  public String getOrder() {
    return mOrder;
  }
}
