package com.nm.commerce.pagedef.definition;

/**
 * Basic definition for a product template page. Configuration for content area, thumbnail flags, etc.
 */
public class ProductTemplatePageDefinition extends TemplatePageDefinition {
  // note: Boolean values should be Boolean internally, in getter and setter.
  // note: Integer values should be Integer internally, in getter and setter.
  // This allows for null values, that allow the 'extends' functionality to work.
  
  private Boolean isDefaultToAll;
  
  // associated page definitions
  private ProductTemplatePageDefinition alternateFilterPageDefinition;
  private ProductTemplatePageDefinition viewAllPageDefinition;
  private ProductTemplatePageDefinition alternatePageDefinition;
  
  // content properties
  private Boolean showContentShadow;
  private Boolean showContentEmpty;
  private String contentEmptyFile;
  
  // sorts and filters
  private String filterPath;
  private String sortPath;
  private Boolean onlyShowSaleSort;
  
  // paging properties
  private String nextPageImagePath;
  private String nextPageText;
  private String pagingReturnImagePath;
  private String pagingSeparator;
  private String previousPageImagePath;
  private String previousPageText;
  private Boolean showPagingText;
  
  // thumbnail settings - also apply to feature product
  // suppressMoreAttributes is deprecated; it is the equivalent of preTextFlags=flgSoldOut
  private String thumbnailTextLinkType;
  private Boolean showProductTextLink;
  private Boolean displayPrice;
  private Boolean editorialTemplate;
  private Boolean reloadWithFeatureProduct;
  private Boolean showOnlyXLeft;
  private String[] returnParameters;
  
  // filter type title image paths
  private String filterBarTitleImageNameDesigner;
  private String filterBarTitleImageNameBrand;
  private String filterBarTitleImageNameCollection;
  private String filterBarTitleImageNameSize;
  
  // sort type title image path and use text flag
  private String sortBarTitleImageName;
  private Boolean useSortTypeText;
  
  // pagingReturn View X per page image
  private String pagingReturnViewXPerPageImage;
  private String filterMouseBehavior;
  
  // property for determining if PCS (Personalized Category Sort & Rank project) is enabled for this template.
  private Boolean pcsEnabledForTemplate;
  
  // property for determining if template is a collection of subcat products
  private Boolean subcatProductCollection;
  private Boolean hideHeaderTitle;
  private Boolean showVendorName;
  private Boolean dynamicImagesEnabled;
  
  //Sale Silo Product Carousel
  private int carouselsCount;
  private int carouselsCountForMobile;
  private int prefProductCount;
  private int carouselProductWebCount;
  private int carouselProductTabCount;
  private float carouselProductMobileCount;
  
  //To set banner
  private String featureCategoryBannerFile;
  private String internationalFeatureCategoryBannerFile;
  private Boolean showFeatureBanners;
  private Boolean staticHeaderImageFile;
  private int initialCarouselsCount;
  private Boolean enableLazyLoading;
  
  //Limit slyce products
  private int slyceProductCount;
  
  /**
 * @return the featureCategoryBannerFile
 */
public String getFeatureCategoryBannerFile() {
	return featureCategoryBannerFile;
}

/**
   * @param featureCategoryBannerFile
   *          the featureCategoryBannerFile to set
 */
  public void setFeatureCategoryBannerFile(final String featureCategoryBannerFile) {
	this.featureCategoryBannerFile = featureCategoryBannerFile;
}

/**
 * @return the internationalFeatureCategoryBannerFile
 */
public String getInternationalFeatureCategoryBannerFile() {
	return internationalFeatureCategoryBannerFile;
}

/**
   * @param internationalFeatureCategoryBannerFile
   *          the internationalFeatureCategoryBannerFile to set
 */
  public void setInternationalFeatureCategoryBannerFile(final String internationalFeatureCategoryBannerFile) {
	this.internationalFeatureCategoryBannerFile = internationalFeatureCategoryBannerFile;
}

/**
 * @return the showFeatureBanners
 */
public Boolean getShowFeatureBanners() {
	return showFeatureBanners;
}

/**
   * @param showFeatureBanners
   *          the showFeatureBanners to set
 */
  public void setShowFeatureBanners(final Boolean showFeatureBanners) {
	this.showFeatureBanners = showFeatureBanners;
}

/**
 * @return the staticHeaderImageFile
 */
public Boolean getStaticHeaderImageFile() {
	return staticHeaderImageFile;
}

/**
   * @param staticHeaderImageFile
   *          the staticHeaderImageFile to set
 */
  public void setStaticHeaderImageFile(final Boolean staticHeaderImageFile) {
	this.staticHeaderImageFile = staticHeaderImageFile;
}

  public Boolean getShowVendorName() {
    return getValue(showVendorName, basis, "getShowVendorName");
  }
  
  public void setShowVendorName(final Boolean showVendorName) {
    this.showVendorName = showVendorName;
  }
  
  public Boolean getHideHeaderTitle() {
    return getValue(hideHeaderTitle, basis, "getHideHeaderTitle");
  }
  
  public void setHideHeaderTitle(final Boolean hideHeaderTitle) {
    this.hideHeaderTitle = hideHeaderTitle;
  }
  
  public ProductTemplatePageDefinition() {
    super();
    setType(TYPE);
    
    // before any properties are set,
    // associated page definitions are assigned to this definition
    alternateFilterPageDefinition = this;
    viewAllPageDefinition = this;
    alternatePageDefinition = this;
  }
  
  // getters and setters
  
  public ProductTemplatePageDefinition getViewAllPageDefinition() {
    return getValue(viewAllPageDefinition, basis, "getViewAllPageDefinition");
  }
  
  public void setViewAllPageDefinition(final ProductTemplatePageDefinition viewAllPageDefinition) {
    this.viewAllPageDefinition = viewAllPageDefinition;
  }
  
  public ProductTemplatePageDefinition getAlternatePageDefinition() {
    return getValue(alternatePageDefinition, basis, "getAlternatePageDefinition");
  }
  
  public void setAlternatePageDefinition(final ProductTemplatePageDefinition alternatePageDefinition) {
    this.alternatePageDefinition = alternatePageDefinition;
  }
  
  public void setAlternateFilterPageDefinition(final ProductTemplatePageDefinition alternateFilterPageDefinition) {
    this.alternateFilterPageDefinition = alternateFilterPageDefinition;
  }
  
  public ProductTemplatePageDefinition getAlternateFilterPageDefinition() {
    return getValue(alternateFilterPageDefinition, basis, "getAlternateFilterPageDefinition");
  }
  
  public Boolean getDefaultToAll() {
    return getValue(isDefaultToAll, basis, "getDefaultToAll");
  }
  
  public void setDefaultToAll(final Boolean isDefaultToAll) {
    this.isDefaultToAll = isDefaultToAll;
  }
  
  public Boolean getShowContentShadow() {
    return getValue(showContentShadow, basis, "getShowContentShadow");
  }
  
  public void setShowContentShadow(final Boolean showContentShadow) {
    this.showContentShadow = showContentShadow;
  }
  
  public Boolean getEditorialTemplate() {
    return getValue(editorialTemplate, basis, "getEditorialTemplate");
  }
  
  public void setEditorialTemplate(final Boolean editorialTemplate) {
    this.editorialTemplate = editorialTemplate;
  }
  
  public String getThumbnailTextLinkType() {
    return getValue(thumbnailTextLinkType, basis, "getThumbnailTextLinkType");
  }
  
  public void setThumbnailTextLinkType(final String thumbnailTextLinkType) {
    this.thumbnailTextLinkType = thumbnailTextLinkType;
  }
  
  public Boolean getReloadWithFeatureProduct() {
    return getValue(reloadWithFeatureProduct, basis, "getReloadWithFeatureProduct");
  }
  
  public void setReloadWithFeatureProduct(final Boolean reloadWithFeatureProduct) {
    this.reloadWithFeatureProduct = reloadWithFeatureProduct;
  }
  
  public Boolean getShowProductTextLink() {
    return getValue(showProductTextLink, basis, "getShowProductTextLink");
  }
  
  public void setShowProductTextLink(final Boolean showProductTextLink) {
    this.showProductTextLink = showProductTextLink;
  }
  
  public Boolean getDisplayPrice() {
    return getValue(displayPrice, basis, "getDisplayPrice");
  }
  
  public void setDisplayPrice(final Boolean displayPrice) {
    this.displayPrice = displayPrice;
  }
  
  public Boolean getShowOnlyXLeft() {
    return getValue(showOnlyXLeft, basis, "getShowOnlyXLeft");
  }
  
  public void setShowOnlyXLeft(final Boolean showOnlyXLeft) {
    this.showOnlyXLeft = showOnlyXLeft;
  }
  
  public String getNextPageImagePath() {
    return getValue(nextPageImagePath, basis, "getNextPageImagePath");
  }
  
  public void setNextPageImagePath(final String nextPageImagePath) {
    this.nextPageImagePath = nextPageImagePath;
  }
  
  public String getNextPageText() {
    return getValue(nextPageText, basis, "getNextPageText");
  }
  
  public void setNextPageText(final String nextPageText) {
    this.nextPageText = nextPageText;
  }
  
  public String getPagingReturnImagePath() {
    return getValue(pagingReturnImagePath, basis, "getPagingReturnImagePath");
  }
  
  public void setPagingReturnImagePath(final String pagingReturnImagePath) {
    this.pagingReturnImagePath = pagingReturnImagePath;
  }
  
  @Override
  public String getPagingSeparator() {
    return getValue(pagingSeparator, basis, "getPagingSeparator");
  }
  
  @Override
  public void setPagingSeparator(final String pagingSeparator) {
    this.pagingSeparator = pagingSeparator;
  }
  
  public String getPreviousPageImagePath() {
    return getValue(previousPageImagePath, basis, "getPreviousPageImagePath");
  }
  
  public void setPreviousPageImagePath(final String previousPageImagePath) {
    this.previousPageImagePath = previousPageImagePath;
  }
  
  public String getPreviousPageText() {
    return getValue(previousPageText, basis, "getPreviousPageText");
  }
  
  public void setPreviousPageText(final String previousPageText) {
    this.previousPageText = previousPageText;
  }
  
  public Boolean getShowPagingText() {
    return getValue(showPagingText, basis, "getShowPagingText");
  }
  
  public void setShowPagingText(final Boolean showPagingText) {
    this.showPagingText = showPagingText;
  }
  
  public String getFilterPath() {
    return getValue(filterPath, basis, "getFilterPath");
  }
  
  public void setFilterPath(final String filterPath) {
    this.filterPath = filterPath;
  }
  
  public String getSortPath() {
    return getValue(sortPath, basis, "getSortPath");
  }
  
  public void setSortPath(final String sortPath) {
    this.sortPath = sortPath;
  }
  
  public String getFilterBarTitleImageNameDesigner() {
    return getValue(filterBarTitleImageNameDesigner, basis, "getFilterBarTitleImageNameDesigner");
  }
  
  public void setFilterBarTitleImageNameDesigner(final String filterBarTitleImageNameDesigner) {
    this.filterBarTitleImageNameDesigner = filterBarTitleImageNameDesigner;
  }
  
  public String getFilterBarTitleImageNameBrand() {
    return getValue(filterBarTitleImageNameBrand, basis, "getFilterBarTitleImageNameBrand");
  }
  
  public void setFilterBarTitleImageNameBrand(final String filterBarTitleImageNameBrand) {
    this.filterBarTitleImageNameBrand = filterBarTitleImageNameBrand;
  }
  
  public String getFilterBarTitleImageNameCollection() {
    return getValue(filterBarTitleImageNameCollection, basis, "getFilterBarTitleImageNameCollection");
  }
  
  public void setFilterBarTitleImageNameCollection(final String filterBarTitleImageNameCollection) {
    this.filterBarTitleImageNameCollection = filterBarTitleImageNameCollection;
  }
  
  public String getFilterBarTitleImageNameSize() {
    return getValue(filterBarTitleImageNameSize, basis, "getFilterBarTitleImageNameSize");
  }
  
  public void setFilterBarTitleImageNameSize(final String filterBarTitleImageNameSize) {
    this.filterBarTitleImageNameSize = filterBarTitleImageNameSize;
  }
  
  public String getSortBarTitleImageName() {
    return getValue(sortBarTitleImageName, basis, "getSortBarTitleImageName");
  }
  
  public void setSortBarTitleImageName(final String sortBarTitleImageName) {
    this.sortBarTitleImageName = sortBarTitleImageName;
  }
  
  public Boolean getUseSortTypeText() {
    return getValue(useSortTypeText, basis, "getUseSortTypeText");
  }
  
  public void setUseSortTypeText(final Boolean useSortTypeText) {
    this.useSortTypeText = useSortTypeText;
  }
  
  public String getPagingReturnViewXPerPageImage() {
    return getValue(pagingReturnViewXPerPageImage, basis, "getPagingReturnViewXPerPageImage");
  }
  
  public void setPagingReturnViewXPerPageImage(final String pagingReturnViewXPerPageImage) {
    this.pagingReturnViewXPerPageImage = pagingReturnViewXPerPageImage;
  }
  
  public String getContentEmptyFile() {
    return getValue(contentEmptyFile, basis, "getContentEmptyFile");
  }
  
  public void setContentEmptyFile(final String contentEmptyFile) {
    this.contentEmptyFile = contentEmptyFile;
  }
  
  public Boolean getShowContentEmpty() {
    return getValue(showContentEmpty, basis, "getShowContentEmpty");
  }
  
  public void setShowContentEmpty(final Boolean showContentEmpty) {
    this.showContentEmpty = showContentEmpty;
  }
  
  public String[] getReturnParameters() {
    return getValue(returnParameters, basis, "getReturnParameters");
  }
  
  public void setReturnParameters(final String[] returnParams) {
    this.returnParameters = returnParams;
  }
  
  public void setOnlyShowSaleSort(final Boolean onlyShowSaleSort) {
    this.onlyShowSaleSort = onlyShowSaleSort;
  }
  
  public Boolean getOnlyShowSaleSort() {
    return getValue(onlyShowSaleSort, basis, "getOnlyShowSaleSort");
  }
  
  public String getFilterMouseBehavior() {
    return getValue(filterMouseBehavior, basis, "getFilterMouseBehavior");
  }
  
  public void setFilterMouseBehavior(final String filterMouseBehavior) {
    this.filterMouseBehavior = filterMouseBehavior;
  }
  
  public Boolean isPcsEnabledForTemplate() {
    return getValue(pcsEnabledForTemplate, basis, "isPcsEnabledForTemplate");
  }
  
  public void setPcsEnabledForTemplate(final Boolean pcsEnabledForTemplate) {
    this.pcsEnabledForTemplate = pcsEnabledForTemplate;
  }
  
  public Boolean isSubcatProductCollection() {
    return getValue(subcatProductCollection, basis, "isSubcatProductCollection");
  }
  
  public void setSubcatProductCollection(final Boolean subcatProductCollection) {
    this.subcatProductCollection = subcatProductCollection;
  }
  
  @Override
  public Boolean getDynamicImagesEnabled() {
    return getValue(dynamicImagesEnabled, basis, "getDynamicImagesEnabled");
  }
  
  @Override
  public void setDynamicImagesEnabled(final Boolean dynamicImagesEnabled) {
    this.dynamicImagesEnabled = dynamicImagesEnabled;
  }
  
  public int getCarouselsCount() {
	return getValue(carouselsCount, basis, "getCarouselsCount");
  }

  public void setCarouselsCount(final int carouselsCount) {
	this.carouselsCount = carouselsCount;
  }

  public int getPrefProductCount() {
	return getValue(prefProductCount, basis, "getPrefProductCount");
  }

  public void setPrefProductCount(final int prefProductCount) {
	this.prefProductCount = prefProductCount;
  }

  public int getCarouselProductWebCount() {
	  return getValue(carouselProductWebCount, basis, "getCarouselProductWebCount");
  }

  public void setCarouselProductWebCount(final int carouselProductWebCount) {
	  this.carouselProductWebCount = carouselProductWebCount;
  }

  public int getCarouselProductTabCount() {
	  return getValue(carouselProductTabCount, basis, "getCarouselProductTabCount");
  }

  public void setCarouselProductTabCount(final int carouselProductTabCount) {
	  this.carouselProductTabCount = carouselProductTabCount;
  }

 public int getCarouselsCountForMobile() {
	return getValue(carouselsCountForMobile, basis, "getCarouselsCountForMobile");
 }

  public void setCarouselsCountForMobile(final int carouselsCountForMobile) {
	this.carouselsCountForMobile = carouselsCountForMobile;
 }

 public float getCarouselProductMobileCount() {
	return carouselProductMobileCount;
 }

  public void setCarouselProductMobileCount(final float carouselProductMobileCount) {
	this.carouselProductMobileCount = carouselProductMobileCount;
 }

  public int getInitialCarouselsCount() {
    return getValue(initialCarouselsCount, basis, "getInitialCarouselsCount");
  }
  
  public void setInitialCarouselsCount(final int initialCarouselsCount) {
    this.initialCarouselsCount = initialCarouselsCount;
  }
  
  public Boolean getEnableLazyLoading() {
    return getValue(enableLazyLoading, basis, "getEnableLazyLoading");
  }
  
  public void setEnableLazyLoading(final Boolean enableLazyLoading) {
    this.enableLazyLoading = enableLazyLoading;
  }

  public int getSlyceProductCount() {
	  return slyceProductCount;
  }

  public void setSlyceProductCount(int slyceProductCount) {
	  this.slyceProductCount = slyceProductCount;
  }

 
}
