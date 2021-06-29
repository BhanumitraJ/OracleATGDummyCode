package com.nm.commerce.catalog.core;

/**
 * The ProductFlag class is a representation of the configurations available for configuring a unique product flag element.
 * 
 * @author nmecd
 */

public class ProductFlag {
  
  /**
   * @return String value that represents the className
   */
  public String getClassName() {
    return className;
  }
  
  /**
   * @param className
   *          set the String value for className
   */
  public void setClassName(String className) {
    this.className = className;
  }
  
  /**
   * @return RefreshableElement that represents the product flag
   */
  public RefreshableElement getFlagAsset() {
    return flagAsset;
  }
  
  /**
   * @param flagAsset
   *          set the RefreshableElement value for the product flag
   */
  public void setFlagAsset(RefreshableElement flagAsset) {
    this.flagAsset = flagAsset;
  }
  
  /**
   * @return the RefreshableElement value for the overlayed image
   */
  public RefreshableElement getOverlayAsset() {
    return overlayAsset;
  }
  
  /**
   * @param overlayAsset
   *          - sets the element for the overlayAsset
   */
  public void setOverlayAsset(RefreshableElement overlayAsset) {
    this.overlayAsset = overlayAsset;
  }
  
  // convenience methods to abstract TemplateElement implementation from JSPs
  
  public String getOverlayFileName() {
    return (overlayAsset == null) ? null : overlayAsset.getFileName();
  }
  
  public boolean isImageType() {
    if (flagAsset != null) {
      return (flagAsset.getRefreshableType() == 2);
    }
    return false;
  }
  
  public String getImageFileName() {
    return (flagAsset == null) ? null : flagAsset.getFileName();
  }
  
  public String getText() {
    return (flagAsset == null) ? null : flagAsset.getAssetText();
  }
  
  private String className;
  private RefreshableElement flagAsset;
  private RefreshableElement overlayAsset;
}
