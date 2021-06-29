package com.nm.commerce.catalog.core;

/**

 */
public class ProductFlagRefreshableElement extends RefreshableElement {
  public String getAssetFileRoot() {
    return assetFileRoot;
  }
  
  public void setAssetFileRoot(String assetFileRoot) {
    this.assetFileRoot = assetFileRoot;
  }
  
  private String assetFileRoot;
  
  public String getQuicklookFileName() {
    return this.quicklookFileName;
  }
  
  public void setQuicklookFileName(String quicklookFileName) {
    this.quicklookFileName = quicklookFileName;
  }
  
  private String quicklookFileName;
}
