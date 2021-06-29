package com.nm.commerce.pagedef.definition;

public class StorePageDefinition extends PageDefinition {

  private String storeResult;
  private String storesLanding;
  private String storePage;
  private Boolean showStoreLinks;
  private Boolean showStoreType;
  private Boolean showYoullFind;
  
  public String getStorePage() {
    return storePage;
  }

  public void setStorePage(final String storePage) {
    this.storePage = storePage;
  }

  public String getStoreResult() {
    return storeResult;
  }

  public void setStoreResult(final String storeResult) {
    this.storeResult = storeResult;
  }

  public String getStoresLanding() {
    return storesLanding;
  }

  public void setStoresLanding(final String storesLanding) {
    this.storesLanding = storesLanding;
  }
  
  public Boolean getShowStoreLinks() {
    return showStoreLinks;
  }
  
  public void setShowStoreLinks(final Boolean showStoreLinks) {
    this.showStoreLinks = showStoreLinks;
  }
  
  public Boolean getShowStoreType() {
    return showStoreType;
  }
  
  public void setShowStoreType(Boolean showStoreType) {
    this.showStoreType = showStoreType;
  }
  
  public Boolean getShowYoullFind() {
    return showYoullFind;
  }
  
  public void setShowYoullFind(Boolean showYoullFind) {
    this.showYoullFind = showYoullFind;
  }
  
}
