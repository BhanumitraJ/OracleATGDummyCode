package com.nm.commerce.pagedef.definition;

import java.util.Map;

/**
 * Basic definition for a iframe page.
 */
public class IframePageDefinition extends PageDefinition {
  
  private String innerContentPath;
  private Map<String, String> innerContentPathMap;
  
  public String getInnerContentPath() {
    return getValue(innerContentPath, basis, "getInnerContentPath");
  }
  
  public void setInnerContentPath(String innerContentPath) {
    this.innerContentPath = innerContentPath;
  }
  
  public Map<String, String> getInnerContentPathMap() {
    return innerContentPathMap;
  }
  
  public void setInnerContentPathMap(Map<String, String> innerContentPathMap) {
    this.innerContentPathMap = innerContentPathMap;
  }
  
} // End of Class
