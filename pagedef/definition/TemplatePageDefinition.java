package com.nm.commerce.pagedef.definition;

import java.util.Properties;

import com.nm.catalog.navigation.NMCategory;

/**
 * Basic definition for a template page. Configuration for main content area, layout, nav, etc.
 */
public class TemplatePageDefinition extends PageDefinition {
  public static final String TYPE = "template";
  
  public static final String TEMPLATE_TYPE_PRODUCT = "product";
  public static final String TEMPLATE_TYPE_CATEGORY = "subcategory";
  
  // note: Boolean values should be Boolean internally, in getter and setter.
  // note: Integer values should be Integer internally, in getter and setter.
  // This allows for null values, that allow the 'extends' functionality to work.
  
  private String templateType;
  
  private String breadcrumbPath;
  private String breadcrumbPreTextImagePath;
  private String breadcrumbSiloImageFile;
  private String breadcrumbSeparator;
  private String omnitureBreadCrumbingSeparator;
  private Boolean breadcrumbDisplaySeparatorFirst;
  private Boolean breadcrumbHideSiloLevel;
  private Boolean breadcrumbDontLinkLastElement;
  private String designerIndexVendorColumn;
  private Boolean useSiloSpecificPromos;
  
  private String navPath;
  private Boolean useNavigationImages;
  private Boolean isSuppressBoutiqueNav;
  private String navigationOpenImagePath;
  private String navigationCloseImagePath;
  
  // Left Nav Customization Options
  private Boolean leftNavDisplayParent;
  private Boolean leftNavDisplayDesignerAtBottom;
  private Boolean leftNavHideRefinement;
  private String leftNavRefinementPosition;
  private Boolean leftNavShowLvl4;
  private Boolean leftNavShowLvl5;
  private Boolean showOnlyChildNav;
  private Boolean hideDimensionName;
  
  private String innerContentPath;
  private String topPromoPath; // above feature image and header
  private String contentHeaderPath; // fragment at top of content;
  private String contentHeaderFile; // name of refreshable file
  private String internationalContentHeaderFile;
  
  private String preHeaderText;
  private String postHeaderText;
  private String emailCollectorPath; // path to the jsp with the category email access form
  
  // Paging Customization
  private Integer pagingNumberDisplayed;
  
  // feature product properties
  private String featureProductPath;
  private Integer featureItemCount;
  private Boolean showFeatureProductName;
  private Boolean showFeatureProductDesigner;
  private Boolean showFeatureCategoryDescription;
  private String staticFeatureImagePath;
  private String[] featureImageKeys;
  private Boolean useFeatureOverlay;
  private Boolean dynamicImagesEnabled;
  private String[] promoTilePositions;
  
  // flags relative to feature product, also product thumbnails
  private Boolean showAdvertisedPromo;
  
  // thumbnails layout
  private String thumbnailPath;
  private String[] imageKeys;
  private String[] toggleImageKeys;
  private String[] carouselImageKeys;
  private Boolean enableZEShot;
  private boolean displayGraphicHeader2;
  private Integer mobileColumnCount;
  
  public String[] getCarouselImageKeys() {
    return carouselImageKeys;
  }
  
  public void setCarouselImageKeys(final String[] carouselImageKeys) {
    this.carouselImageKeys = carouselImageKeys;
  }
  
  private String quickText;
  private Integer columnCount;
  private Integer itemCount;
  private Integer itemCountForMobile;
  private String graphicBlockPath;
  
  private String viewAllImagePath;
  private String viewAllText;
  private String viewManyImagePath;
  private String viewManyText;
  private Integer viewManySize;
  
  private String topPagingPath;
  private String bottomPagingPath;
  private String backToTopPath;
  private String pageLinkPath;
  
  private String itemsPerPagePath;
  
  private String alternateCatId;
  
  private Boolean displayHeaderInContentDiv;
  
  // thumbnail enhancements
  private Boolean useDogear;
  private Boolean useThumbnailToggleImage;
  private String dogEarImagePath;
  private String thumbnailStyle;
  
  // limit number of product flags displayed
  private Integer maxFlags;
  
  // user view choice
  private Boolean userViewChoiceEnabled;
  
  // alternate views properties
  private Integer alternateViewIndex = new Integer(0);
  private String altViewButton;
  private String altViewButtonSel;
  private String alternateViewPath;
  private EndecaTemplatePageDefinition alternateTemplate;
  
  private Boolean enableFlagOverlay;
  private Boolean enableMultipleFlagOverlays;
  private Integer maxOverlayFlags;
  private String[] overlayFlags;
  private String[] preTextFlags;
  private String[] postTextFlags;
  private String flagDirectoryKey;
  private Boolean hideSaleLabel = false;
  
  private String perPageViewDisabledCats;
  
  public String getPerPageViewDisabledCats() {
    return perPageViewDisabledCats;
  }
  
  public void setPerPageViewDisabledCats(final String perPageViewDisabledCats) {
    this.perPageViewDisabledCats = perPageViewDisabledCats;
  }
  
  private Boolean truncateIntegerPrices = true;
  
  private Boolean wrapPosition1Price;
  
  private Integer numberPromos = new Integer(0);
  
  private EndecaTemplatePageDefinition linkTemplate;
  
  private Boolean showOnlyXLeftFlag = false;
  
  private PageDefinition endecaDrivenDefinition;
  
  private String staticImageFile;
  private String internationalStaticImageFile;
  // Properties map to select the home template based on the country code
  private Properties internationalTemplateMappings;
  
  private boolean infiniteScrollEnabled;
  
  private Boolean seoCategoryDescriptionEnabled;
  
  private String templateId;
  
  //to show max carousel in mobile
  private Boolean maxCarouselInMobile;
  
  public TemplatePageDefinition() {
    super();
    setType(TYPE);
  }
  
  // convenience methods for getter using extends basis
  
  protected ProductTemplatePageDefinition getValue(final ProductTemplatePageDefinition local, final PageDefinition basis, final String methodName) {
    return (ProductTemplatePageDefinition) getObjectValue(local, basis, methodName);
  }
  
  protected EndecaTemplatePageDefinition getValue(final EndecaTemplatePageDefinition local, final PageDefinition basis, final String methodName) {
    return (EndecaTemplatePageDefinition) getObjectValue(local, basis, methodName);
  }
  
  protected SubcategoryTemplatePageDefinition getValue(final SubcategoryTemplatePageDefinition local, final PageDefinition basis, final String methodName) {
    return (SubcategoryTemplatePageDefinition) getObjectValue(local, basis, methodName);
  }
  
  @Override
  public PageDefinition getAlternateDefinition(final NMCategory category) {
    if ((category != null) && category.getFlgEndecaDriven()) {
      final PageDefinition altDefinition = getEndecaDrivenDefinition();
      if (altDefinition != null) {
        return altDefinition;
      }
    }
    return this;
  }
  
  // getters and setters
  
  public String getBreadcrumbPath() {
    return getValue(breadcrumbPath, basis, "getBreadcrumbPath");
  }
  
  public void setBreadcrumbPath(final String breadcrumbPath) {
    this.breadcrumbPath = breadcrumbPath;
  }
  
  public String getNavPath() {
    return getValue(navPath, basis, "getNavPath");
  }
  
  public void setNavPath(final String navPath) {
    this.navPath = navPath;
  }
  
  public Boolean getShowOnlyChildNav() {
    return getValue(useNavigationImages, basis, "getShowOnlyChildNav");
  }
  
  public void setShowOnlyChildNav(final Boolean showOnlyChildNav) {
    this.showOnlyChildNav = showOnlyChildNav;
  }
  
  public String getInnerContentPath() {
    return getValue(innerContentPath, basis, "getInnerContentPath");
  }
  
  public void setInnerContentPath(final String innerContentPath) {
    this.innerContentPath = innerContentPath;
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
  
  public Boolean getUseNavigationImages() {
    return getValue(useNavigationImages, basis, "getUseNavigationImages");
  }
  
  public void setUseNavigationImages(final Boolean useNavigationImages) {
    this.useNavigationImages = useNavigationImages;
  }
  
  public Boolean getIsSuppressBoutiqueNav() {
    return getValue(isSuppressBoutiqueNav, basis, "getIsSuppressBoutiqueNav");
  }
  
  public void setIsSuppressBoutiqueNav(final Boolean isSuppressBoutiqueNav) {
    this.isSuppressBoutiqueNav = isSuppressBoutiqueNav;
  }
  
  public String getNavigationOpenImagePath() {
    return getValue(navigationOpenImagePath, basis, "getNavigationOpenImagePath");
  }
  
  public void setNavigationOpenImagePath(final String navigationOpenImagePath) {
    this.navigationOpenImagePath = navigationOpenImagePath;
  }
  
  public String getNavigationCloseImagePath() {
    return getValue(navigationCloseImagePath, basis, "getNavigationCloseImagePath");
  }
  
  public void setNavigationCloseImagePath(final String navigationCloseImagePath) {
    this.navigationCloseImagePath = navigationCloseImagePath;
  }
  
  public String getContentHeaderFile() {
    return getValue(contentHeaderFile, basis, "getContentHeaderFile");
  }
  
  public void setContentHeaderFile(final String contentHeaderFile) {
    this.contentHeaderFile = contentHeaderFile;
  }
  
  @Override
  public String getTemplateType() {
    return getValue(templateType, basis, "getTemplateType");
  }
  
  @Override
  public void setTemplateType(final String templateType) {
    this.templateType = templateType;
  }
  
  public String getGraphicBlockPath() {
    return getValue(graphicBlockPath, basis, "getGraphicBlockPath");
  }
  
  public void setGraphicBlockPath(final String graphicBlockPath) {
    this.graphicBlockPath = graphicBlockPath;
  }
  
  public Integer getColumnCount() {
    return getValue(columnCount, basis, "getColumnCount");
  }
  
  public void setColumnCount(final Integer columnCount) {
    this.columnCount = columnCount;
  }
  
  public Integer getItemCount() {
    return getValue(itemCount, basis, "getItemCount");
  }
  
  public void setItemCount(final Integer itemCount) {
    this.itemCount = itemCount;
  }
  
  public Integer getItemCountForMobile() {
	  return getValue(itemCountForMobile, basis, "getItemCountForMobile");
   }
	  
   public void setItemCountForMobile(final Integer itemCountForMobile) {
	    this.itemCountForMobile = itemCountForMobile;
   }
	  
  
  public void setNumberPromos(final Integer numberPromos) {
    this.numberPromos = numberPromos;
  }
  
  public Integer getNumberPromos() {
    return getValue(numberPromos, basis, "getNumberPromos");
  }
  
  public Integer getFeatureItemCount() {
    return getValue(featureItemCount, basis, "getFeatureItemCount");
  }
  
  public void setFeatureItemCount(final Integer featureItemCount) {
    this.featureItemCount = featureItemCount;
  }
  
  public String[] getImageKeys() {
    return getValue(imageKeys, basis, "getImageKeys");
  }
  
  public void setImageKeys(final String[] imageKeys) {
    this.imageKeys = imageKeys;
  }
  
  public String[] getToggleImageKeys() {
    return getValue(toggleImageKeys, basis, "getToggleImageKeys");
  }
  
  public void setToggleImageKeys(final String[] toggleImageKeys) {
    this.toggleImageKeys = toggleImageKeys;
  }
  
  public String getQuickText() {
    return getValue(quickText, basis, "getQuickText");
  }
  
  public void setQuickText(final String quickText) {
    this.quickText = quickText;
  }
  
  public String getContentHeaderPath() {
    return getValue(contentHeaderPath, basis, "getContentHeaderPath");
  }
  
  public void setContentHeaderPath(final String contentHeaderPath) {
    this.contentHeaderPath = contentHeaderPath;
  }
  
  public String getTopPromoPath() {
    return getValue(topPromoPath, basis, "getTopPromoPath");
  }
  
  public void setTopPromoPath(final String topPromoPath) {
    this.topPromoPath = topPromoPath;
  }
  
  public String getFeatureProductPath() {
    return getValue(featureProductPath, basis, "getFeatureProductPath");
  }
  
  public void setFeatureProductPath(final String featureProductPath) {
    this.featureProductPath = featureProductPath;
  }
  
  public Boolean getShowFeatureProductName() {
    return getValue(showFeatureProductName, basis, "getShowFeatureProductName");
  }
  
  public void setShowFeatureProductName(final Boolean showFeatureProductName) {
    this.showFeatureProductName = showFeatureProductName;
  }
  
  public Boolean getShowFeatureProductDesigner() {
    return getValue(showFeatureProductDesigner, basis, "getShowFeatureProductDesigner");
  }
  
  public void setShowFeatureProductDesigner(final Boolean showFeatureProductDesigner) {
    this.showFeatureProductDesigner = showFeatureProductDesigner;
  }
  
  public Boolean getShowFeatureCategoryDescription() {
    return getValue(showFeatureCategoryDescription, basis, "getShowFeatureCategoryDescription");
  }
  
  public void setShowFeatureCategoryDescription(final Boolean showFeatureCategoryDescription) {
    this.showFeatureCategoryDescription = showFeatureCategoryDescription;
  }
  
  public String getStaticFeatureImagePath() {
    return getValue(staticFeatureImagePath, basis, "getStaticFeatureImagePath");
  }
  
  public void setStaticFeatureImagePath(final String staticFeatureImagePath) {
    this.staticFeatureImagePath = staticFeatureImagePath;
  }
  
  public String[] getFeatureImageKeys() {
    return getValue(featureImageKeys, basis, "getFeatureImageKeys");
  }
  
  public void setFeatureImageKeys(final String[] featureImageKeys) {
    this.featureImageKeys = featureImageKeys;
  }
  
  public Boolean getUseFeatureOverlay() {
    return getValue(useFeatureOverlay, basis, "getUseFeatureOverlay");
  }
  
  public void setUseFeatureOverlay(final Boolean useFeatureOverlay) {
    this.useFeatureOverlay = useFeatureOverlay;
  }
  
  public Boolean getShowAdvertisedPromo() {
    return getValue(showAdvertisedPromo, basis, "getShowAdvertisedPromo");
  }
  
  public void setShowAdvertisedPromo(final Boolean showAdvertisedPromo) {
    this.showAdvertisedPromo = showAdvertisedPromo;
  }
  
  public String getThumbnailPath() {
    return getValue(thumbnailPath, basis, "getThumbnailPath");
  }
  
  public void setThumbnailPath(final String thumbnailPath) {
    this.thumbnailPath = thumbnailPath;
  }
  
  public String getViewAllImagePath() {
    return getValue(viewAllImagePath, basis, "getViewAllImagePath");
  }
  
  public void setViewAllImagePath(final String viewAllImagePath) {
    this.viewAllImagePath = viewAllImagePath;
  }
  
  public String getViewAllText() {
    return getValue(viewAllText, basis, "getViewAllText");
  }
  
  public void setViewAllText(final String viewAllText) {
    this.viewAllText = viewAllText;
  }
  
  public String getViewManyText() {
    return getValue(viewManyText, basis, "getViewManyText");
  }
  
  public void setViewManyText(final String viewManyText) {
    this.viewManyText = viewManyText;
  }
  
  public String getViewManyImagePath() {
    return getValue(viewManyImagePath, basis, "getViewManyImagePath");
  }
  
  public void setViewManyImagePath(final String viewManyImagePath) {
    this.viewManyImagePath = viewManyImagePath;
  }
  
  public Integer getViewManySize() {
    return getValue(viewManySize, basis, "getViewManySize");
  }
  
  public void setViewManySize(final Integer viewManySize) {
    this.viewManySize = viewManySize;
  }
  
  public String getTopPagingPath() {
    return getValue(topPagingPath, basis, "getTopPagingPath");
  }
  
  public void setTopPagingPath(final String topPagingPath) {
    this.topPagingPath = topPagingPath;
  }
  
  public String getBottomPagingPath() {
    return getValue(bottomPagingPath, basis, "getBottomPagingPath");
  }
  
  public void setBottomPagingPath(final String bottomPagingPath) {
    this.bottomPagingPath = bottomPagingPath;
  }
  
  public String getBackToTopPath() {
    return getValue(backToTopPath, basis, "getBackToTopPath");
  }
  
  public void setBackToTopPath(final String backToTopPath) {
    this.backToTopPath = backToTopPath;
  }
  
  public String getPageLinkPath() {
    return getValue(pageLinkPath, basis, "getPageLinkPath");
  }
  
  public void setPageLinkPath(final String pageLinkPath) {
    this.pageLinkPath = pageLinkPath;
  }
  
  public String getAlternateCatId() {
    return alternateCatId;
  }
  
  public void setAlternateCatId(final String alternateCatId) {
    this.alternateCatId = alternateCatId;
  }
  
  public void setPreHeaderText(final String preHeaderText) {
    this.preHeaderText = preHeaderText;
  }
  
  public String getPreHeaderText() {
    return getValue(preHeaderText, basis, "getPreHeaderText");
  }
  
  public void setPostHeaderText(final String postHeaderText) {
    this.postHeaderText = postHeaderText;
  }
  
  public String getPostHeaderText() {
    return getValue(postHeaderText, basis, "getPostHeaderText");
  }
  
  public String getEmailCollectorPath() {
    return getValue(emailCollectorPath, basis, "getEmailCollectorPath");
  }
  
  public void setEmailCollectorPath(final String emailCollectorPath) {
    this.emailCollectorPath = emailCollectorPath;
  }
  
  public Boolean getUserViewChoiceEnabled() {
    return getValue(userViewChoiceEnabled, basis, "getUserViewChoiceEnabled");
  }
  
  public void setUserViewChoiceEnabled(final Boolean userViewChoiceEnabled) {
    this.userViewChoiceEnabled = userViewChoiceEnabled;
  }
  
  public Boolean getUseDogear() {
    return getValue(useDogear, basis, "getUseDogear");
  }
  
  public void setUseDogear(final Boolean useDogear) {
    this.useDogear = useDogear;
  }
  
  public Boolean getUseThumbnailToggleImage() {
    return getValue(useThumbnailToggleImage, basis, "getUseThumbnailToggleImage");
  }
  
  public void setUseThumbnailToggleImage(final Boolean useThumbnailToggleImage) {
    this.useThumbnailToggleImage = useThumbnailToggleImage;
  }
  
  public String getDogEarImagePath() {
    return getValue(dogEarImagePath, basis, "getDogEarImagePath");
  }
  
  public void setDogEarImagePath(final String dogEarImagePath) {
    this.dogEarImagePath = dogEarImagePath;
  }
  
  public Integer getAlternateViewIndex() {
    return getValue(alternateViewIndex, basis, "getAlternateViewIndex");
  }
  
  public void setAlternateViewIndex(final Integer alternateViewIndex) {
    this.alternateViewIndex = alternateViewIndex;
  }
  
  public String getAltViewButton() {
    return getValue(altViewButton, basis, "getAltViewButton");
  }
  
  public void setAltViewButton(final String altViewButton) {
    this.altViewButton = altViewButton;
  }
  
  public String getAltViewButtonSel() {
    return getValue(altViewButtonSel, basis, "getAltViewButtonSel");
  }
  
  public void setAltViewButtonSel(final String altViewButtonSel) {
    this.altViewButtonSel = altViewButtonSel;
  }
  
  public EndecaTemplatePageDefinition getAlternateTemplate() {
    return getValue(alternateTemplate, basis, "getAlternateTemplate");
  }
  
  public void setAlternateTemplate(final EndecaTemplatePageDefinition alternateTemplate) {
    this.alternateTemplate = alternateTemplate;
  }
  
  public String getAlternateViewPath() {
    return getValue(alternateViewPath, basis, "getAlternateViewPath");
  }
  
  public void setAlternateViewPath(final String alternateViewPath) {
    this.alternateViewPath = alternateViewPath;
  }
  
  public Boolean getHideSaleLabel() {
    return getValue(hideSaleLabel, basis, "getHideSaleLabel");
  }
  
  public void setHideSaleLabel(final Boolean hideSaleLabel) {
    this.hideSaleLabel = hideSaleLabel;
  }
  
  public Boolean getEnableFlagOverlay() {
    return getValue(enableFlagOverlay, basis, "getEnableFlagOverlay");
  }
  
  public void setEnableFlagOverlay(final Boolean enableFlagOverlay) {
    this.enableFlagOverlay = enableFlagOverlay;
  }
  
  public Boolean getEnableMultipleFlagOverlays() {
    return getValue(enableMultipleFlagOverlays, basis, "getEnableMultipleFlagOverlays");
  }
  
  public void setEnableMultipleFlagOverlays(final Boolean enableMultipleFlagOverlays) {
    this.enableMultipleFlagOverlays = enableMultipleFlagOverlays;
  }
  
  public Integer getMaxOverlayFlags() {
    return getValue(maxOverlayFlags, basis, "getMaxOverlayFlags");
  }
  
  public void setMaxOverlayFlags(final Integer maxOverlayFlags) {
    this.maxOverlayFlags = maxOverlayFlags;
  }
  
  public String[] getPreTextFlags() {
    return getValue(preTextFlags, basis, "getPreTextFlags");
  }
  
  public void setPreTextFlags(final String[] preTextFlags) {
    this.preTextFlags = preTextFlags;
  }
  
  public String[] getPostTextFlags() {
    return getValue(postTextFlags, basis, "getPostTextFlags");
  }
  
  public void setPostTextFlags(final String[] postTextFlags) {
    this.postTextFlags = postTextFlags;
  }
  
  public String getFlagDirectoryKey() {
    return getValue(flagDirectoryKey, basis, "getFlagDirectoryKey");
  }
  
  public void setFlagDirectoryKey(final String flagDirectoryKey) {
    this.flagDirectoryKey = flagDirectoryKey;
  }
  
  public EndecaTemplatePageDefinition getLinkTemplate() {
    return getValue(linkTemplate, basis, "getLinkTemplate");
  }
  
  public void setLinkTemplate(final EndecaTemplatePageDefinition linkTemplate) {
    this.linkTemplate = linkTemplate;
  }
  
  public Boolean getShowOnlyXLeftFlag() {
    return getValue(showOnlyXLeftFlag, basis, "getShowOnlyXLeftFlag");
  }
  
  public void setShowOnlyXLeftFlag(final Boolean showOnlyXLeftFlag) {
    this.showOnlyXLeftFlag = showOnlyXLeftFlag;
  }
  
  public String getThumbnailStyle() {
    return getValue(thumbnailStyle, basis, "getThumbnailStyle");
  }
  
  public void setThumbnailStyle(final String thumbnailStyle) {
    this.thumbnailStyle = thumbnailStyle;
  }
  
  public Integer getMaxFlags() {
    return getValue(maxFlags, basis, "getMaxFlags");
  }
  
  public void setMaxFlags(final Integer maxFlags) {
    this.maxFlags = maxFlags;
  }
  
  public PageDefinition getEndecaDrivenDefinition() {
    // value is not inheritable, don't use getValue here
    return endecaDrivenDefinition;
  }
  
  public void setEndecaDrivenDefinition(final PageDefinition endecaDrivenDefinition) {
    this.endecaDrivenDefinition = endecaDrivenDefinition;
  }
  
  public Boolean getDisplayHeaderInContentDiv() {
    return getValue(displayHeaderInContentDiv, basis, "getDisplayHeaderInContentDiv");
  }
  
  public void setDisplayHeaderInContentDiv(final Boolean displayHeaderInContentDiv) {
    this.displayHeaderInContentDiv = displayHeaderInContentDiv;
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
  
  public Boolean getLeftNavDisplayParent() {
    return getValue(leftNavDisplayParent, basis, "getLeftNavDisplayParent");
  }
  
  public void setLeftNavDisplayParent(final Boolean leftNavDisplayParent) {
    this.leftNavDisplayParent = leftNavDisplayParent;
  }
  
  public Boolean getLeftNavDisplayDesignerAtBottom() {
    return getValue(leftNavDisplayDesignerAtBottom, basis, "getLeftNavDisplayDesignerAtBottom");
  }
  
  public void setLeftNavDisplayDesignerAtBottom(final Boolean leftNavDisplayDesignerAtBottom) {
    this.leftNavDisplayDesignerAtBottom = leftNavDisplayDesignerAtBottom;
  }
  
  public Boolean getLeftNavHideRefinement() {
    return getValue(leftNavHideRefinement, basis, "getLeftNavHideRefinement");
  }
  
  public void setLeftNavHideRefinement(final Boolean leftNavHideRefinement) {
    this.leftNavHideRefinement = leftNavHideRefinement;
  }
  
  public String getLeftNavRefinementPosition() {
    return getValue(leftNavRefinementPosition, basis, "getLeftNavRefinementPosition");
  }
  
  public void setLeftNavRefinementPosition(final String leftNavRefinementPosition) {
    this.leftNavRefinementPosition = leftNavRefinementPosition;
  }
  
  public Boolean getHideDimensionName() {
    return getValue(hideDimensionName, basis, "getHideDimensionName");
  }
  
  public void setHideDimensionName(final Boolean hideDimensionName) {
    this.hideDimensionName = hideDimensionName;
  }
  
  public Boolean getLeftNavShowLvl4() {
    return getValue(leftNavShowLvl4, basis, "getLeftNavShowLvl4");
  }
  
  public void setLeftNavShowLvl4(final Boolean leftNavShowLvl4) {
    this.leftNavShowLvl4 = leftNavShowLvl4;
  }
  
  public String[] getOverlayFlags() {
    return getValue(overlayFlags, basis, "getOverlayFlags");
  }
  
  public void setOverlayFlags(final String[] overlayFlags) {
    this.overlayFlags = overlayFlags;
  }
  
  public void setTruncateIntegerPrices(final Boolean truncateIntegerPrices) {
    this.truncateIntegerPrices = truncateIntegerPrices;
  }
  
  public Boolean getTruncateIntegerPrices() {
    return getValue(truncateIntegerPrices, basis, "getTruncateIntegerPrices");
  }
  
  public void setWrapPosition1Price(final Boolean wrapPosition1Price) {
    this.wrapPosition1Price = wrapPosition1Price;
  }
  
  public Boolean getWrapPosition1Price() {
    return getValue(wrapPosition1Price, basis, "getWrapPosition1Price");
  }
  
  public Integer getPagingNumberDisplayed() {
    return getValue(pagingNumberDisplayed, basis, "getPagingNumberDisplayed");
  }
  
  public void setPagingNumberDisplayed(final Integer pagingNumberDisplayed) {
    this.pagingNumberDisplayed = pagingNumberDisplayed;
  }
  
  public String getItemsPerPagePath() {
    return getValue(itemsPerPagePath, basis, "getItemsPerPagePath");
  }
  
  public void setItemsPerPagePath(final String itemsPerPagePath) {
    this.itemsPerPagePath = itemsPerPagePath;
  }
  
  public String getDesignerIndexVendorColumn() {
    return getValue(designerIndexVendorColumn, basis, "getDesignerIndexVendorColumn");
  }
  
  public void setDesignerIndexVendorColumn(final String designerIndexVendorColumn) {
    this.designerIndexVendorColumn = designerIndexVendorColumn;
  }
  
  public Boolean getUseSiloSpecificPromos() {
    return getValue(useSiloSpecificPromos, basis, "getUseSiloSpecificPromos");
  }
  
  public void setUseSiloSpecificPromos(final Boolean useSiloSpecificPromos) {
    this.useSiloSpecificPromos = useSiloSpecificPromos;
  }
  
  public Boolean getLeftNavShowLvl5() {
    return getValue(leftNavShowLvl5, basis, "getLeftNavShowLvl5");
  }
  
  public void setLeftNavShowLvl5(final Boolean leftNavShowLvl5) {
    this.leftNavShowLvl5 = leftNavShowLvl5;
  }
  
  /**
   * @return country-temlate mapped properties
   */
  public Properties getInternationalTemplateMappings() {
    return this.internationalTemplateMappings;
  }
  
  /**
   * @param internationalTemplateMappings
   *          sets country-temlate mapped properties
   */
  public void setInternationalTemplateMappings(final Properties internationalTemplateMappings) {
    this.internationalTemplateMappings = internationalTemplateMappings;
  }
  
  public String getStaticImageFile() {
    return staticImageFile;
  }
  
  public void setStaticImageFile(final String staticImageFile) {
    this.staticImageFile = staticImageFile;
  }
  
  public String getInternationalStaticImageFile() {
    return internationalStaticImageFile;
  }
  
  public void setInternationalStaticImageFile(final String internationalStaticImageFile) {
    this.internationalStaticImageFile = internationalStaticImageFile;
  }
  
  public String getInternationalContentHeaderFile() {
    return internationalContentHeaderFile;
  }
  
  public void setInternationalContentHeaderFile(final String internationalContentHeaderFile) {
    this.internationalContentHeaderFile = internationalContentHeaderFile;
  }
  
  public boolean isInfiniteScrollEnabled() {
    return getValue(infiniteScrollEnabled, basis, "isInfiniteScrollEnabled");
  }
  
  public void setInfiniteScrollEnabled(final boolean infiniteScrollEnabled) {
    this.infiniteScrollEnabled = infiniteScrollEnabled;
  }
  
  public Boolean getSeoCategoryDescriptionEnabled() {
    return getValue(seoCategoryDescriptionEnabled, basis, "getSeoCategoryDescriptionEnabled");
  }
  
  public void setSeoCategoryDescriptionEnabled(final Boolean seoCategoryDescriptionEnabled) {
    this.seoCategoryDescriptionEnabled = seoCategoryDescriptionEnabled;
  }
  
  public Boolean getDynamicImagesEnabled() {
    return getValue(dynamicImagesEnabled, basis, "getDynamicImagesEnabled");
  }
  
  public void setDynamicImagesEnabled(final Boolean dynamicImagesEnabled) {
    this.dynamicImagesEnabled = dynamicImagesEnabled;
  }
  
  public Boolean getEnableZEShot() {
    return getValue(enableZEShot, basis, "getEnableZEShot");
  }
  
  public void setEnableZEShot(final Boolean enableZEShot) {
    this.enableZEShot = enableZEShot;
    
  }
  
  public boolean getDisplayGraphicHeader2() {
    return getValue(displayGraphicHeader2, basis, "getDisplayGraphicHeader2");
  }
  
  public void setDisplayGraphicHeader2(final boolean displayGraphicHeader2) {
    this.displayGraphicHeader2 = displayGraphicHeader2;
    
  }
  
  public Integer getMobileColumnCount() {
    return getValue(mobileColumnCount, basis, "getMobileColumnCount");
  }
  
  public void setMobileColumnCount(final Integer mobileColumnCount) {
    this.mobileColumnCount = mobileColumnCount;
  }
  
  public String[] getPromoTilePositions() {
    return getValue(promoTilePositions, basis, "getPromoTilePositions");
  }
  
  public void setPromoTilePositions(final String[] promoTilePositions) {
    this.promoTilePositions = promoTilePositions;
  }

public String getTemplateId() {
	return getValue(templateId, basis, "getTemplateId");
}

public void setTemplateId(String templateId) {
	this.templateId = templateId;
}

public Boolean getMaxCarouselInMobile() {
   return getValue(maxCarouselInMobile, basis, "getMaxCarouselInMobile");
}

public void setMaxCarouselInMobile(Boolean maxCarouselInMobile) {
	this.maxCarouselInMobile = maxCarouselInMobile;
}


  
} // End of Class
