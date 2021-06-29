package com.nm.commerce.pagedef.definition;

import atg.nucleus.ServiceMap;

/**
 * Basic definition for a sub-category template page. Configuration for main content area, layout, nav, etc.
 */
public class SubcategoryTemplatePageDefinition extends TemplatePageDefinition {
  // note: Boolean values should be Boolean internally.
  // note: Integer values should be Integer internally.
  // This allows for null values, that allow the 'extends' functionality to work.
  
  private ProductTemplatePageDefinition superAllPageDefinition;
  private Integer minimumNumberCategories;
  private Integer restrictElementCount;
  private Boolean showTopText;
  private Boolean showBottomText;
  private Boolean showAdditionalText;
  private Boolean showFeatureBanners;
  
  private String thumbnailParentIndex;
  private String alternateSaleCategoryId;
  private String postText;
  private String staticHeaderImageFile;
  private String featureCategoryBannerFile;
  private String internationalFeatureCategoryBannerFile;
  private String staticImageFile;
  private String internationalStaticImageFile;
  private String subCatTitle;
  private Boolean dynamicImagesEnabled; 
  
  //sale category carousel
  private Integer carouselsItemWebCount;
  private Integer carouselsItemMobileCount;
  private Integer subcategoryThreshold;
  private String preferredProductPreference;
  
  // superviewall setting
  private Boolean forceSuperAllDisplay;
  private String preSuperAllLinkText;
  private String postSuperAllLinkText;
  private Boolean superAllDisplayCategoryName;
  
  // refreshable icon setting
  private String refreshableIconPath;
  private ServiceMap substitutionMap;
  
  public SubcategoryTemplatePageDefinition() {
    super();
  }
  
  // getters and setters
  
  public ProductTemplatePageDefinition getSuperAllPageDefinition() {
    return getValue(superAllPageDefinition, basis, "getSuperAllPageDefinition");
  }
  
  public void setSuperAllPageDefinition(ProductTemplatePageDefinition superAllPageDefinition) {
    this.superAllPageDefinition = superAllPageDefinition;
  }
  
  public Integer getMinimumNumberCategories() {
    return getValue(minimumNumberCategories, basis, "getMinimumNumberCategories");
  }
  
  public void setMinimumNumberCategories(Integer minimumNumberCat) {
    this.minimumNumberCategories = minimumNumberCat;
  }
  
  public void setRestrictElementCount(Integer restrictElementCount) {
    this.restrictElementCount = restrictElementCount;
  }
  
  public Integer getRestrictElementCount() {
    return getValue(restrictElementCount, basis, "getRestrictElementCount");
  }
  
  public void setShowAdditionalText(Boolean showAdditionalText) {
    this.showAdditionalText = showAdditionalText;
  }
  
  public Boolean getShowAdditionalText() {
	    return getValue(showAdditionalText, basis, "getShowAdditionalText");
	  }
  
  public Integer getCarouselsItemWebCount() {
	  return getValue(carouselsItemWebCount, basis, "getCarouselsItemWebCount");
  }

  public void setCarouselsItemWebCount(Integer carouselsItemWebCount) {
	this.carouselsItemWebCount = carouselsItemWebCount;
  }

  public Integer getCarouselsItemMobileCount() {
	  return getValue(carouselsItemMobileCount, basis, "getCarouselsItemMobileCount");
  }

  public void setCarouselsItemMobileCount(Integer carouselsItemMobileCount) {
	this.carouselsItemMobileCount = carouselsItemMobileCount;
  }

  public Integer getSubcategoryThreshold() {
	return getValue(subcategoryThreshold, basis, "getSubcategoryThreshold");
  }

  public void setSubcategoryThreshold(Integer subcategoryThreshold) {
	this.subcategoryThreshold = subcategoryThreshold;
  }
  
  public String getPreferredProductPreference() {
	return getValue(preferredProductPreference, basis, "getPreferredProductPreference");
  }

  public void setPreferredProductPreference(String preferredProductPreference) {
	this.preferredProductPreference = preferredProductPreference;
  }

  public void setShowBottomText(Boolean showBottomText) {
    this.showBottomText = showBottomText;
  }
  
  public Boolean getShowBottomText() {
    return getValue(showBottomText, basis, "getShowBottomText");
  }
  
  public void setShowTopText(Boolean showTopText) {
    this.showTopText = showTopText;
  }
  
  public Boolean getShowTopText() {
    return getValue(showTopText, basis, "getShowTopText");
  }
  
  public void setShowFeatureBanners(Boolean showFeatureBanners) {
    this.showFeatureBanners = showFeatureBanners;
  }
  
  public Boolean getShowFeatureBanners() {
    return getValue(showFeatureBanners, basis, "getShowFeatureBanners");
  }
  
  public void setThumbnailParentIndex(String thumbnailParentIndex) {
    this.thumbnailParentIndex = thumbnailParentIndex;
  }
  
  public String getThumbnailParentIndex() {
    return getValue(thumbnailParentIndex, basis, "getThumbnailParentIndex");
  }
  
  public void setAlternateSaleCategoryId(String alternateSaleCategoryId) {
    this.alternateSaleCategoryId = alternateSaleCategoryId;
  }
  
  public String getAlternateSaleCategoryId() {
    return getValue(alternateSaleCategoryId, basis, "getAlternateSaleCategoryId");
  }
  
  public void setPostText(String postText) {
    this.postText = postText;
  }
  
  public String getPostText() {
    return getValue(postText, basis, "getPostText");
  }
  
  public void setStaticHeaderImageFile(String staticHeaderImageFile) {
    this.staticHeaderImageFile = staticHeaderImageFile;
  }
  
  public String getStaticHeaderImageFile() {
    return getValue(staticHeaderImageFile, basis, "getStaticHeaderImageFile");
  }
  
  public void setFeatureCategoryBannerFile(String featureCategoryBannerFile) {
    this.featureCategoryBannerFile = featureCategoryBannerFile;
  }
  
  public String getFeatureCategoryBannerFile() {
    return getValue(featureCategoryBannerFile, basis, "getFeatureCategoryBannerFile");
  }
  
  public void setForceSuperAllDisplay(Boolean forceSuperAllDisplay) {
    this.forceSuperAllDisplay = forceSuperAllDisplay;
  }
  
  public Boolean getForceSuperAllDisplay() {
    return getValue(forceSuperAllDisplay, basis, "getForceSuperAllDisplay");
  }
  
  public void setPreSuperAllLinkText(String preSuperAllLinkText) {
    this.preSuperAllLinkText = preSuperAllLinkText;
  }
  
  public String getPreSuperAllLinkText() {
    return getValue(preSuperAllLinkText, basis, "getPreSuperAllLinkText");
  }
  
  public void setPostSuperAllLinkText(String postSuperAllLinkText) {
    this.postSuperAllLinkText = postSuperAllLinkText;
  }
  
  public String getPostSuperAllLinkText() {
    return getValue(postSuperAllLinkText, basis, "getPostSuperAllLinkText");
  }
  
  public void setSuperAllDisplayCategoryName(Boolean superAllDisplayCategoryName) {
    this.superAllDisplayCategoryName = superAllDisplayCategoryName;
  }
  
  public Boolean getSuperAllDisplayCategoryName() {
    return getValue(superAllDisplayCategoryName, basis, "getSuperAllDisplayCategoryName");
  }
  
  public void setRefreshableIconPath(String refreshableIconPath) {
    this.refreshableIconPath = refreshableIconPath;
  }
  
  public String getRefreshableIconPath() {
    return getValue(refreshableIconPath, basis, "getRefreshableIconPath");
  }
  
  public void setSubstitutionMap(ServiceMap substitutionMap) {
    this.substitutionMap = substitutionMap;
  }
  
  public ServiceMap getSubstitutionMap() {
    ServiceMap map = (ServiceMap) getObjectValue(substitutionMap, basis, "getSubstitutionMap");
    return map;
  }
  
  public String getInternationalFeatureCategoryBannerFile() {
    return internationalFeatureCategoryBannerFile;
  }
  
  public void setInternationalFeatureCategoryBannerFile(String internationalFeatureCategoryBannerFile) {
    this.internationalFeatureCategoryBannerFile = internationalFeatureCategoryBannerFile;
  }
  
  public String getStaticImageFile() {
    return staticImageFile;
  }
  
  public void setStaticImageFile(String staticImageFile) {
    this.staticImageFile = staticImageFile;
  }
  
  public String getInternationalStaticImageFile() {
    return internationalStaticImageFile;
  }
  
  public void setInternationalStaticImageFile(String internationalStaticImageFile) {
    this.internationalStaticImageFile = internationalStaticImageFile;
  }
  
  public Boolean getDynamicImagesEnabled() {
    return getValue(dynamicImagesEnabled, basis, "getDynamicImagesEnabled");
	}

	public void setDynamicImagesEnabled( Boolean dynamicImagesEnabled ) {
		this.dynamicImagesEnabled = dynamicImagesEnabled;
	}

	public String getSubCatTitle() {
		return getValue(subCatTitle, basis, "getSubCatTitle");
	}

	public void setSubCatTitle(String subCatTitle) {
		this.subCatTitle = subCatTitle;
	}
  
}
