package com.nm.commerce.pagedef.definition;

public class DesignerIndexPageDefinition extends TemplatePageDefinition {
  
  private String designerIndexId;
  private Boolean useSiloImages;
  private String numColumns;
  private String splitThreshold;
  private String defaultSilo;
  private String navigationAction;
  private Boolean useNavigationImages;
  private Boolean isOldModel;
  private Boolean isByCategory;
  private Boolean useSiloSpecificPromos;
  private String designerIndexLetterText;
  private String designerIndexVendorColumn;
  private String leftBottomContentPath;
  private String leftTopContentPath;
  private String rightBottomContentPath;
  private String rightTopContentPath;
  private String topCenterContentPath;
  private String designerBodyContentPath;
  private Boolean useStructuredAZ;
  private boolean indexBySilo;
  private String designerPromoFile;
  private String internationalDesignerPromoFile;
  private String myFavoritesAddDesignersUrl;
  
  // support SEO URLs on designer index
  private Boolean useSeoHyperlinks = false;
  
  public Boolean getUseStructuredAZ() {
    return this.getValue(useStructuredAZ, basis, "getUseStructuredAZ");
  }
  
  public void setUseStructuredAZ(Boolean useStructuredAZ) {
    this.useStructuredAZ = useStructuredAZ;
  }
  
  public String getDesignerIndexId() {
    return getValue(designerIndexId, basis, "getDesignerIndexId");
  }
  
  public void setDesignerIndexId(String designerIndexId) {
    this.designerIndexId = designerIndexId;
  }
  
  public Boolean getUseSiloImages() {
    return getValue(useSiloImages, basis, "getUseSiloImages");
  }
  
  public void setUseSiloImages(Boolean useSiloImages) {
    this.useSiloImages = useSiloImages;
  }
  
  public String getNumColumns() {
    return numColumns;
  }
  
  public void setNumColumns(String numColumns) {
    this.numColumns = numColumns;
  }
  
  public String getSplitThreshold() {
    return splitThreshold;
  }
  
  public void setSplitThreshold(String splitThreshold) {
    this.splitThreshold = splitThreshold;
  }
  
  public String getDefaultSilo() {
    return defaultSilo;
  }
  
  public void setDefaultSilo(String defaultSilo) {
    this.defaultSilo = defaultSilo;
  }
  
  public String getNavigationAction() {
    return getValue(navigationAction, basis, "getNavigationAction");
  }
  
  public void setNavigationAction(String navigationAction) {
    this.navigationAction = navigationAction;
  }
  
  public Boolean getUseNavigationImages() {
    return getValue(useNavigationImages, basis, "getUseNavigationImages");
  }
  
  public void setUseNavigationImages(Boolean useNavigationImages) {
    this.useNavigationImages = useNavigationImages;
  }
  
  public void setIsOldModel(Boolean isOldModel) {
    this.isOldModel = isOldModel;
  }
  
  public Boolean getIsOldModel() {
    return isOldModel;
  }
  
  public void setIsByCategory(Boolean isByCategory) {
    this.isByCategory = isByCategory;
  }
  
  public Boolean getIsByCategory() {
    return isByCategory;
  }
  
  public void setUseSiloSpecificPromos(Boolean useSiloSpecificPromos) {
    this.useSiloSpecificPromos = useSiloSpecificPromos;
  }
  
  public Boolean getUseSiloSpecificPromos() {
    return useSiloSpecificPromos;
  }
  
  public String getLeftBottomContentPath() {
    return leftBottomContentPath;
  }
  
  public void setLeftBottomContentPath(String leftBottomContentPath) {
    this.leftBottomContentPath = leftBottomContentPath;
  }
  
  public String getLeftTopContentPath() {
    return leftTopContentPath;
  }
  
  public void setLeftTopContentPath(String leftTopContentPath) {
    this.leftTopContentPath = leftTopContentPath;
  }
  
  public String getRightBottomContentPath() {
    return rightBottomContentPath;
  }
  
  public void setRightBottomContentPath(String rightBottomContentPath) {
    this.rightBottomContentPath = rightBottomContentPath;
  }
  
  public String getRightTopContentPath() {
    return rightTopContentPath;
  }
  
  public void setRightTopContentPath(String rightTopContentPath) {
    this.rightTopContentPath = rightTopContentPath;
  }
  
  public String getTopCenterContentPath() {
    return topCenterContentPath;
  }
  
  public void setTopCenterContentPath(String topCenterContentPath) {
    this.topCenterContentPath = topCenterContentPath;
  }
  
  public String getDesignerBodyContentPath() {
    return designerBodyContentPath;
  }
  
  public void setDesignerBodyContentPath(String designerBodyContentPath) {
    this.designerBodyContentPath = designerBodyContentPath;
  }
  
  public String getDesignerIndexLetterText() {
    return designerIndexLetterText;
  }
  
  public void setDesignerIndexLetterText(String designerIndexLetterText) {
    this.designerIndexLetterText = designerIndexLetterText;
  }
  
  public String getDesignerIndexVendorColumn() {
    return designerIndexVendorColumn;
  }
  
  public void setDesignerIndexVendorColumn(String designerIndexVendorColumn) {
    this.designerIndexVendorColumn = designerIndexVendorColumn;
  }
  
  public boolean isIndexBySilo() {
    return indexBySilo;
  }
  
  public void setIndexBySilo(boolean indexBySilo) {
    this.indexBySilo = indexBySilo;
  }
  
  public Boolean getUseSeoHyperlinks() {
    return getValue(useSeoHyperlinks, basis, "getUseSeoHyperlinks");
  }
  
  public void setUseSeoHyperlinks(Boolean useSeoHyperlinks) {
    this.useSeoHyperlinks = useSeoHyperlinks;
  }
  
  public String getDesignerPromoFile() {
    return designerPromoFile;
  }
  
  public void setDesignerPromoFile(String designerPromoFile) {
    this.designerPromoFile = designerPromoFile;
  }
  
  public String getInternationalDesignerPromoFile() {
    return internationalDesignerPromoFile;
  }
  
  public void setInternationalDesignerPromoFile(String internationalDesignerPromoFile) {
    this.internationalDesignerPromoFile = internationalDesignerPromoFile;
  }

  public String getMyFavoritesAddDesignersUrl() {
    return getValue(myFavoritesAddDesignersUrl, basis, "getMyFavoritesAddDesignersUrl");
  }

  public void setMyFavoritesAddDesignersUrl(String myFavoritesAddDesignersUrl) {
    this.myFavoritesAddDesignersUrl = myFavoritesAddDesignersUrl;
  }

}
