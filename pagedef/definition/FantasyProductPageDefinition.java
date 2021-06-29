package com.nm.commerce.pagedef.definition;

public class FantasyProductPageDefinition extends PageDefinition {
  private String defaultFantasyProduct;
  private String CBCategoryId;
  private String CBBaseUrl;
  private String fantasyCateogryUrl;
  private String imgRefreshablePath;
  private String defaultShot;
  private String mainShot;
  private String thumbnailShot;
  private String largerShot;
  private String zoomShot;
  
  public void setDefaultFantasyProduct(String defaultFantasyProduct) {
    this.defaultFantasyProduct = defaultFantasyProduct;
  }
  
  public String getDefaultFantasyProduct() {
    return defaultFantasyProduct;
  }
  
  public void setCBCategoryId(String cBCategoryId) {
    CBCategoryId = cBCategoryId;
  }
  
  public String getCBCategoryId() {
    return CBCategoryId;
  }
  
  public void setCBBaseUrl(String cBBaseUrl) {
    CBBaseUrl = cBBaseUrl;
  }
  
  public String getCBBaseUrl() {
    return CBBaseUrl;
  }
  
  public void setFantasyCateogryUrl(String fantasyCateogryUrl) {
    this.fantasyCateogryUrl = fantasyCateogryUrl;
  }
  
  public String getFantasyCateogryUrl() {
    return fantasyCateogryUrl;
  }
  
  public void setImgRefreshablePath(String imgRefreshablePath) {
    this.imgRefreshablePath = imgRefreshablePath;
  }
  
  public String getImgRefreshablePath() {
    return imgRefreshablePath;
  }
  
  public void setDefaultShot(String defaultShot) {
    this.defaultShot = defaultShot;
  }
  
  public String getDefaultShot() {
    return defaultShot;
  }
  
  public String getMainShot() {
    return mainShot;
  }
  
  public void setMainShot(String mainShot) {
    this.mainShot = mainShot;
  }
  
  public String getThumbnailShot() {
    return thumbnailShot;
  }
  
  public void setThumbnailShot(String thumbnailShot) {
    this.thumbnailShot = thumbnailShot;
  }
  
  public String getLargerShot() {
    return largerShot;
  }
  
  public void setLargerShot(String largerShot) {
    this.largerShot = largerShot;
  }

public String getZoomShot() {
	return zoomShot;
}

public void setZoomShot(String zoomShot) {
	this.zoomShot = zoomShot;
}
  
}
