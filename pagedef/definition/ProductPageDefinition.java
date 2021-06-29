package com.nm.commerce.pagedef.definition;

public class ProductPageDefinition extends PageDefinition {
  public static final String TYPE = "product";
  
  private String breadcrumbPreTextImagePath;
  private String breadcrumbSiloImageFile;
  private String breadcrumbSeparator;
  private String omnitureBreadCrumbingSeparator;
  private Boolean separatorBeforeBreadcrumbItems = false;
  private Boolean breadcrumbDisplaySeparatorFirst;
  private Boolean breadcrumbHideSiloLevel;
  private Boolean breadcrumbDontLinkLastElement;
  private String endecaProductPagingPath;
  private String manualProductPagingPath;
  private String heroImagePath;
  private String productOptionPage;
  private String controlButtonsPath;
  private String superSuiteNavPath;
  private String relatedProductPath;
  private String productDetailsPath;
  private String productTabsPath;
  private String ymalTitlePath;
  private String ctlTitlePath;
  private Boolean displayCTLFirst = false;
  private String relatedLayout;
  private Boolean oldModel; // uses matrix, form submit, less pageModel
  private String shareWithFriendJS;
  private String recentlyViewedItemsPath;
  private String needHelpLayout;
  private String promo1Layout;
  private String socialStylist;
  private String socialEmailImg;
  private String socialTwitterImg;
  private String socialFacebookImg;
  private String socialFacebookImgNew;
  private String socialTwitterImgNew;
  private String socialWeiboImg;
  private String forwardFriendBlockPath;
  private Boolean showSocialIconOnTopOfYMAL;
  private String viewAllLinkText;
  private String prevPrefix;
  private String nextSuffix;
  private Boolean removeShortDesigner = Boolean.FALSE;
  private Boolean displayPriceInTitle = Boolean.FALSE;
  private boolean estimateShipping;
  private boolean promoPriceOriginalText;
  private boolean priceAdornStrikethrough;
  private Boolean moveQtyBelowColorSelect;
  private boolean manualMoreColors;
  private boolean detailsLinkToDetailsPage;
  private String prodSelectBoxPath;
  private String configuratorProdSelectBoxPath;
  private String nonConfiguratorProdSelectBoxPath;
  private String configuratorEquivalent;
  private String prodContent;
  private String configuratorProdContent;
  private String newProdSelectBoxPath;
  private String newControlButtonsPath;
  private String newProductOptionPage;

  
  public String getNonConfiguratorProdSelectBoxPath() {
    return getValue(nonConfiguratorProdSelectBoxPath, basis, "getNonConfiguratorProdSelectBoxPath");
  }
  
  private Boolean showLiveRelatedProductsOnly = Boolean.FALSE;
  
  public void setNonConfiguratorProdSelectBoxPath(final String nonConfiguratorProdSelectBoxPath) {
    this.nonConfiguratorProdSelectBoxPath = nonConfiguratorProdSelectBoxPath;
  }
  
  public String getBreadcrumbPreTextImagePath() {
    return getValue(breadcrumbPreTextImagePath, basis, "getBreadcrumbPreTextImagePath");
  }
  
  public void setBreadcrumbPreTextImagePath(final String breadcrumbPreTextImagePath) {
    this.breadcrumbPreTextImagePath = breadcrumbPreTextImagePath;
  }
  
  public String getBreadcrumbSiloImageFile() {
    return getValue(breadcrumbSiloImageFile, basis, "getBreadcrumbSiloImageFile");
  }
  
  public void setBreadcrumbSiloImageFile(final String breadcrumbSiloImageFile) {
    this.breadcrumbSiloImageFile = breadcrumbSiloImageFile;
  }
  
  public String getBreadcrumbSeparator() {
    return getValue(breadcrumbSeparator, basis, "getBreadcrumbSeparator");
  }
  
  public void setBreadcrumbSeparator(final String breadcrumbSeparator) {
    this.breadcrumbSeparator = breadcrumbSeparator;
  }
  
  public String getOmnitureBreadCrumbingSeparator() {
    return getValue(omnitureBreadCrumbingSeparator, basis, "getOmnitureBreadCrumbingSeparator");
  }
  
  public void setOmnitureBreadCrumbingSeparator(final String omnitureBreadCrumbingSeparator) {
    this.omnitureBreadCrumbingSeparator = omnitureBreadCrumbingSeparator;
  }
  
  public void setSeparatorBeforeBreadcrumbItems(final Boolean separatorBeforeBreadcrumbItems) {
    this.separatorBeforeBreadcrumbItems = separatorBeforeBreadcrumbItems;
  }
  
  public Boolean getSeparatorBeforeBreadcrumbItems() {
    return getValue(separatorBeforeBreadcrumbItems, basis, "getSeparatorBeforeBreadcrumbItems");
  }
  
  public void setEndecaProductPagingPath(final String endecaProductPagingPath) {
    this.endecaProductPagingPath = endecaProductPagingPath;
  }
  
  public String getEndecaProductPagingPath() {
    return getValue(endecaProductPagingPath, basis, "getEndecaProductPagingPath");
  }
  
  public void setManualProductPagingPath(final String manualProductPagingPath) {
    this.manualProductPagingPath = manualProductPagingPath;
  }
  
  public String getManualProductPagingPath() {
    return getValue(manualProductPagingPath, basis, "getManualProductPagingPath");
  }
  
  public String getHeroImagePath() {
    return getValue(heroImagePath, basis, "getHeroImagePath");
  }
  
  public void setHeroImagePath(final String heroImagePath) {
    this.heroImagePath = heroImagePath;
  }
  
  public String getProductOptionPage() {
    return getValue(productOptionPage, basis, "getProductOptionPage");
  }
  
  public void setProductOptionPage(final String productOptionPage) {
    this.productOptionPage = productOptionPage;
  }
  
  public String getControlButtonsPath() {
    return getValue(controlButtonsPath, basis, "getControlButtonsPath");
  }
  
  public void setControlButtonsPath(final String controlButtonsPath) {
    this.controlButtonsPath = controlButtonsPath;
  }
  
  public String getSuperSuiteNavPath() {
    return getValue(superSuiteNavPath, basis, "getSuperSuiteNavPath");
  }
  
  public void setSuperSuiteNavPath(final String superSuiteNavPath) {
    this.superSuiteNavPath = superSuiteNavPath;
  }
  
  public void setRelatedProductPath(final String relatedProductPath) {
    this.relatedProductPath = relatedProductPath;
  }
  
  public String getRelatedProductPath() {
    return getValue(relatedProductPath, basis, "getRelatedProductPath");
  }
  
  public void setProductDetailsPath(final String productDetailsPath) {
    this.productDetailsPath = productDetailsPath;
  }
  
  public String getProductDetailsPath() {
    return getValue(productDetailsPath, basis, "getProductDetailsPath");
  }
  
  public String getProductTabsPath() {
    return productTabsPath;
  }
  
  public void setProductTabsPath(final String productTabsPath) {
    this.productTabsPath = productTabsPath;
  }
  
  public void setYmalTitlePath(final String ymalTitlePath) {
    this.ymalTitlePath = ymalTitlePath;
  }
  
  public String getYmalTitlePath() {
    return getValue(ymalTitlePath, basis, "getYmalTitlePath");
  }
  
  public void setCtlTitlePath(final String ctlTitlePath) {
    this.ctlTitlePath = ctlTitlePath;
  }
  
  public String getCtlTitlePath() {
    return getValue(ctlTitlePath, basis, "getCtlTitlePath");
  }
  
  public void setRelatedLayout(final String relatedLayout) {
    this.relatedLayout = relatedLayout;
  }
  
  public String getRelatedLayout() {
    return getValue(relatedLayout, basis, "getRelatedLayout");
  }
  
  public void setDisplayCTLFirst(final Boolean displayCTLFirst) {
    this.displayCTLFirst = displayCTLFirst;
  }
  
  public Boolean getDisplayCTLFirst() {
    return getValue(displayCTLFirst, basis, "getDisplayCTLFirst");
  }
  
  public void setOldModel(final Boolean oldModel) {
    this.oldModel = oldModel;
  }
  
  public Boolean getOldModel() {
    return getValue(oldModel, basis, "getOldModel");
  }
  
  public void setShareWithFriendJS(final String shareWithFriendJS) {
    this.shareWithFriendJS = shareWithFriendJS;
  }
  
  public String getShareWithFriendJS() {
	return shareWithFriendJS;
  }
  
  public String getNeedHelpLayout() {
    return getValue(needHelpLayout, basis, "getNeedHelpLayout");
  }
  
  public void setNeedHelpLayout(final String needHelpLayout) {
    this.needHelpLayout = needHelpLayout;
  }
  
  public String getPromo1Layout() {
    return getValue(promo1Layout, basis, "getPromo1Layout");
  }
  
  public void setPromo1Layout(final String promo1Layout) {
    this.promo1Layout = promo1Layout;
  }
  
  public String getRecentlyViewedItemsPath() {
    return getValue(recentlyViewedItemsPath, basis, "getRecentlyViewedItemsPath");
  }
  
  public void setRecentlyViewedItemsPath(final String recentlyViewedItemsPath) {
    this.recentlyViewedItemsPath = recentlyViewedItemsPath;
  }
  
  public String getSocialStylist() {
    return getValue(socialStylist, basis, "getSocialStylist");
  }
  
  public void setSocialStylist(final String socialStylist) {
    this.socialStylist = socialStylist;
  }
  
  public String getSocialEmailImg() {
    return getValue(socialEmailImg, basis, "getSocialEmailImg");
  }
  
  public void setSocialEmailImg(final String socialEmailImg) {
    this.socialEmailImg = socialEmailImg;
  }
  
  public String getSocialTwitterImg() {
    return getValue(socialTwitterImg, basis, "getSocialTwitterImg");
  }
  
  public void setSocialTwitterImg(final String socialTwitterImg) {
    this.socialTwitterImg = socialTwitterImg;
  }
  
  public String getSocialTwitterImgNew() {
    return socialTwitterImgNew;
  }
  
  public void setSocialTwitterImgNew(String socialTwitterImgNew) {
    this.socialTwitterImgNew = socialTwitterImgNew;
  }
  
  public String getSocialFacebookImg() {
    return getValue(socialFacebookImg, basis, "getSocialFacebookImg");
  }
  
  public void setSocialFacebookImg(final String socialFacebookImg) {
    this.socialFacebookImg = socialFacebookImg;
  }
  
  public String getSocialFacebookImgNew() {
    return socialFacebookImgNew;
  }
  
  public void setSocialFacebookImgNew(String socialFacebookImgNew) {
    this.socialFacebookImgNew = socialFacebookImgNew;
  }
  
	public String getSocialWeiboImg() {
		return getValue(socialWeiboImg, basis, "getSocialWeiboImg");
	}

	public void setSocialWeiboImg(final String socialWeiboImg ) {
		this.socialWeiboImg = socialWeiboImg;
	}

	public String getForwardFriendBlockPath() {
    return getValue(forwardFriendBlockPath, basis, "getForwardFriendBlockPath");
  }
  
  public void setForwardFriendBlockPath(final String forwardFriendBlockPath) {
    this.forwardFriendBlockPath = forwardFriendBlockPath;
  }
  
  public Boolean getShowSocialIconOnTopOfYMAL() {
	  return getValue(showSocialIconOnTopOfYMAL, basis, "getShowSocialIconOnTopOfYMAL");
  }
  
  public void setShowSocialIconOnTopOfYMAL(final Boolean showSocialIconOnTopOfYMAL){
	  this.showSocialIconOnTopOfYMAL = showSocialIconOnTopOfYMAL;
  }
  
  public String getViewAllLinkText() {
    return viewAllLinkText;
  }
  
  public void setViewAllLinkText(final String viewAllLinkText) {
    this.viewAllLinkText = viewAllLinkText;
  }
  
  public String getPrevPrefix() {
    return prevPrefix;
  }
  
  public void setPrevPrefix(final String prevPrefix) {
    this.prevPrefix = prevPrefix;
  }
  
  public String getNextSuffix() {
    return nextSuffix;
  }
  
  public void setNextSuffix(final String nextSuffix) {
    this.nextSuffix = nextSuffix;
  }
  
  public void setRemoveShortDesigner(final Boolean removeShortDesigner) {
    this.removeShortDesigner = removeShortDesigner;
  }
  
  public Boolean getRemoveShortDesigner() {
    return getValue(removeShortDesigner, basis, "getRemoveShortDesigner");
  }
  
  public Boolean getBreadcrumbDisplaySeparatorFirst() {
    return getValue(breadcrumbDisplaySeparatorFirst, basis, "getBreadcrumbDisplaySeparatorFirst");
  }
  
  public void setBreadcrumbDisplaySeparatorFirst(final Boolean breadcrumbDisplaySeparatorFirst) {
    this.breadcrumbDisplaySeparatorFirst = breadcrumbDisplaySeparatorFirst;
  }
  
  public Boolean getBreadcrumbHideSiloLevel() {
    return getValue(breadcrumbHideSiloLevel, basis, "getBreadcrumbHideSiloLevel");
  }
  
  public void setBreadcrumbHideSiloLevel(final Boolean breadcrumbHideSiloLevel) {
    this.breadcrumbHideSiloLevel = breadcrumbHideSiloLevel;
  }
  
  public Boolean getBreadcrumbDontLinkLastElement() {
    return getValue(breadcrumbDontLinkLastElement, basis, "getBreadcrumbDontLinkLastElement");
  }
  
  public void setBreadcrumbDontLinkLastElement(final Boolean breadcrumbDontLinkLastElement) {
    this.breadcrumbDontLinkLastElement = breadcrumbDontLinkLastElement;
  }
  
  public void setDisplayPriceInTitle(final Boolean displayPriceInTitle) {
    this.displayPriceInTitle = displayPriceInTitle;
  }
  
  public Boolean getDisplayPriceInTitle() {
    return getValue(displayPriceInTitle, basis, "getDisplayPriceInTitle");
  }
  
  public boolean isEstimateShipping() {
    return estimateShipping;
  }
  
  public void setEstimateShipping(final boolean estimateShipping) {
    this.estimateShipping = estimateShipping;
  }
  
  public Boolean getMoveQtyBelowColorSelect() {
    return getValue(moveQtyBelowColorSelect, basis, "getMoveQtyBelowColorSelect");
  }
  
  public void setMoveQtyBelowColorSelect(final Boolean moveQtyBelowColorSelect) {
    this.moveQtyBelowColorSelect = moveQtyBelowColorSelect;
  }
  
  public boolean getPromoPriceOriginalText() {
    return promoPriceOriginalText;
  }
  
  public void setPromoPriceOriginalText(final boolean promoPriceOriginalText) {
    this.promoPriceOriginalText = promoPriceOriginalText;
  }
  
  public boolean getPriceAdornStrikethrough() {
    return getValue(priceAdornStrikethrough, basis, "getPriceAdornStrikethrough");
  }
  
  public void setPriceAdornStrikethrough(final boolean priceAdornStrikethrough) {
    this.priceAdornStrikethrough = priceAdornStrikethrough;
  }

  public boolean isManualMoreColors() {
    return getValue(manualMoreColors, basis, "isManualMoreColors");
  }
  
  public void setManualMoreColors(final boolean manualMoreColors) {
    this.manualMoreColors = manualMoreColors;
  }
  
  public boolean isDetailsLinkToDetailsPage() {
    return getValue(detailsLinkToDetailsPage, basis, "isDetailsLinkToDetailsPage");
  }
  
  public void setDetailsLinkToDetailsPage(final boolean detailsLinkToDetailsPage) {
    this.detailsLinkToDetailsPage = detailsLinkToDetailsPage;
  }
  
  public String getProdSelectBoxPath() {
    return getValue(prodSelectBoxPath, basis, "getProdSelectBoxPath");
  }
  
  public void setProdSelectBoxPath(final String prodSelectBoxPath) {
    this.prodSelectBoxPath = prodSelectBoxPath;
  }
  
  public String getConfiguratorProdSelectBoxPath() {
    return getValue(configuratorProdSelectBoxPath, basis, "getConfiguratorProdSelectBoxPath");
  }
  
  public void setConfiguratorProdSelectBoxPath(final String configuratorProdSelectBoxPath) {
    this.configuratorProdSelectBoxPath = configuratorProdSelectBoxPath;
  }
  
  public void setShowLiveRelatedProductsOnly(final Boolean showLiveRelatedProductsOnly) {
    this.showLiveRelatedProductsOnly = showLiveRelatedProductsOnly;
  }
  
  public Boolean getShowLiveRelatedProductsOnly() {
    return getValue(showLiveRelatedProductsOnly, basis, "getShowLiveRelatedProductsOnly");
  }

  public String getConfiguratorEquivalent() {
    return configuratorEquivalent;
  }

  public void setConfiguratorEquivalent(String configuratorEquivalent) {
    this.configuratorEquivalent = configuratorEquivalent;
  }
  
  public String getProdContent() {
	  return getValue(prodContent, basis, "getProdContent");
	}

  public void setProdContent(String prodContent) {
	  this.prodContent = prodContent;
  }

public String getConfiguratorProdContent() {
	return getValue(configuratorProdContent, basis, "getConfiguratorProdContent");
}

public void setConfiguratorProdContent(String configuratorProdContent) {
	this.configuratorProdContent = configuratorProdContent;
}

public String getNewProdSelectBoxPath() {
	return getValue(newProdSelectBoxPath, basis, "getNewProdSelectBoxPath");
}

public void setNewProdSelectBoxPath(String newProdSelectBoxPath) {
	this.newProdSelectBoxPath = newProdSelectBoxPath;
}

public String getNewControlButtonsPath() {
	return getValue(newControlButtonsPath, basis, "getNewControlButtonsPath");	
}

public void setNewControlButtonsPath(String newControlButtonsPath) {
	this.newControlButtonsPath = newControlButtonsPath;
}

public String getNewProductOptionPage() {
	return getValue(newProductOptionPage, basis, "getNewProductOptionPage");	
}

public void setNewProductOptionPage(String newProductOptionPage) {
	this.newProductOptionPage = newProductOptionPage;
}

}
