package com.nm.commerce.catalog;

import atg.nucleus.logging.ApplicationLoggingImpl;

/**
 * NMFilterBoxHistory
 * 
 * @author vsso1
 */
public class NMFilterBoxHistory {
  
  public NMFilterBoxHistory() {
    
  }
  
  public boolean getFilterBoxDisplayed() {
    return filterBoxDisplayed;
  }
  
  public void setFilterBoxDisplayed(boolean value) {
    filterBoxDisplayed = value;
  }
  
  private Boolean filterBoxDisplayed = false;
}
