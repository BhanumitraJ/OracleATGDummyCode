package com.nm.commerce.pagedef.definition;

import java.util.HashMap;
import java.util.Map;

public class MyNMPageDefinition extends PageDefinition {
  
  private String productWidgets;
  private String productWidgetTemplates;
  private String editorialWidgetTemplates;
  private String productEditorialWidgetTemplates;
  private Map<String, String> defaultTitleMap = new HashMap<String, String>();
  private int numOfDisplayableProducts;
  private String imageKey;
  private int cityNameCharLimit;
  private String productWidgetList;
  private boolean isRWD;
  
  public String getProductWidgets() {
    return productWidgets;
  }
  
  public int getCityNameCharLimit() {
    return cityNameCharLimit;
  }
  
  public void setCityNameCharLimit(final int cityNameCharLimit) {
    this.cityNameCharLimit = cityNameCharLimit;
  }
  
  public void setProductWidgets(final String productWidgets) {
    this.productWidgets = productWidgets;
  }
  
  public String getProductWidgetTemplates() {
    return productWidgetTemplates;
  }
  
  public void setProductWidgetTemplates(final String productWidgetTemplates) {
    this.productWidgetTemplates = productWidgetTemplates;
  }
  
  public String getEditorialWidgetTemplates() {
    return editorialWidgetTemplates;
  }
  
  public void setEditorialWidgetTemplates(final String editorialWidgetTemplates) {
    this.editorialWidgetTemplates = editorialWidgetTemplates;
  }
  
  public String getProductEditorialWidgetTemplates() {
    return productEditorialWidgetTemplates;
  }
  
  public void setProductEditorialWidgetTemplates(final String productEditorialWidgetTemplates) {
    this.productEditorialWidgetTemplates = productEditorialWidgetTemplates;
  }
  
  public Map<String, String> getDefaultTitleMap() {
    return defaultTitleMap;
  }
  
  public void setDefaultTitleMap(final Map<String, String> defaultTitleMap) {
    this.defaultTitleMap = defaultTitleMap;
  }
  
  public int getNumOfDisplayableProducts() {
    return numOfDisplayableProducts;
  }
  
  public void setNumOfDisplayableProducts(final int numOfDisplayableProducts) {
    this.numOfDisplayableProducts = numOfDisplayableProducts;
  }
  
  public String getImageKey() {
    return imageKey;
  }
  
  public void setImageKey(final String imageKey) {
    this.imageKey = imageKey;
  }
  
  public String getProductWidgetList() {
    return productWidgetList;
  }
  
  public void setProductWidgetList(final String productWidgetList) {
    this.productWidgetList = productWidgetList;
  }
  
  public boolean isRWD() {
    return isRWD;
  }
  
  public void setRWD(final boolean isRWD) {
    this.isRWD = isRWD;
  }
  
}
