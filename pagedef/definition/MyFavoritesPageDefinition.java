package com.nm.commerce.pagedef.definition;

import java.util.List;

/**
 * Basic definition for a template page. Configuration for main content area, layout, nav, etc.
 */
public class MyFavoritesPageDefinition extends EndecaTemplatePageDefinition {
  
  private List<String> myFavEmailSubject;
  private Boolean dynamicImagesEnabled;
  
  public List<String> getMyFavEmailSubject() {
    return myFavEmailSubject;
  }
  
  public void setMyFavEmailSubject(List<String> myFavEmailSubject) {
    this.myFavEmailSubject = myFavEmailSubject;
  }
  
  @Override
  public Boolean getDynamicImagesEnabled() {
    return getValue(dynamicImagesEnabled, basis, "getDynamicImagesEnabled");
  }
  
  @Override
  public void setDynamicImagesEnabled(Boolean dynamicImagesEnabled) {
    this.dynamicImagesEnabled = dynamicImagesEnabled;
  }
}
