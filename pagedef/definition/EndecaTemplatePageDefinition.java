package com.nm.commerce.pagedef.definition;

import java.util.HashMap;
import java.util.Properties;

import com.nm.search.endeca.EndecaDrivenUtil;
import com.nm.search.endeca.SearchParameters;

public class EndecaTemplatePageDefinition extends ProductTemplatePageDefinition {
  public static final String TYPE = "endecaTemplate";
  
  private String searchType;
  private String pageSize;
  private String vaPageSize;
  private String defaultPageSize = "20";
  private String queryString;
  private String defaultQueryString;
  private String additionalQueryString;
  private String sdiAdditionalQueryString;
  private String nonSaleQueryString = "";
  private String exposedDimensions;
  private String defaultItemIdForSearch;
  private String zoneUsedToCalculateColumns;
  private Boolean enableFlagOverlay;
  private String[] preTextFlags;
  private String[] postTextFlags;
  private Boolean showOnlyXLeft;
  private String imageKeyIndicator;
  private String thumbnailTextLinkType;
  private String flagDirectoryKey;
  private String flagRetainDimensionControls;
  private String flagRetainSortsOnNav;
  private String numCols;
  private String numColsMerchZone;
  private String numColsShort;
  private String vaThreshold;
  private String vaPageSizeShort;
  private String vaThresholdShort;
  private Boolean showDesignerBox;
  private String templatePath;
  private Boolean removeDescriptorsForWeb2;
  private String leftNavViewAllCategoriesButton;
  private String pagingViewAllButton;
  private String pagingNextButton;
  private String pagingPreviousButton;
  private String breadCrumbDimensionNumCols;
  private String omnitureBreadCrumbingSeparator;
  private EndecaTemplatePageDefinition linkTemplate;
  private EndecaTemplatePageDefinition viewAllTemplate;
  private EndecaTemplatePageDefinition landingTemplate;
  private EndecaTemplatePageDefinition saleSearchTemplate;
  private EndecaTemplatePageDefinition bcTemplate;
  private EndecaTemplatePageDefinition lcTemplate;
  private EndecaTemplatePageDefinition pvTemplate;
  private EndecaTemplatePageDefinition catalogLandingTemplate;
  private Boolean showPageText;
  private Boolean highlightDesigner;
  private String evar4DesignerIdxOverride;
  private String explicitParentId;
  private String secondarySorts = "";
  private String endecaSaleCategoryId;
  private Integer elementsInRow;
  private Boolean useBackToTopLink;
  private Boolean resultsZoneGblock;
  private String searchParametersName;
  private String pagingSeparator;
  private String pagingFirstLastSeparator;
  private Boolean pagingLinkFirst;
  private Boolean pagingLinkLast;
  private String switchView;
  private String leftNavDisplayTreePath;
  private String searchReportPath;
  private String autoSuggestFormat = "";
  private String resultsReportFormat = "";
  private String itemsPerPagePath;
  private String sortPath;
  private String filterPath;
  private Boolean sortFilterAtTop;
  private String alternateAjaxSearchMethod;
  private Boolean enableFilterCurrentSelectionPopup;
  private String allTabLabel;
  private String saleTabLabel;
  private Boolean boxedResults;
  private Boolean isRWD;
  private Boolean dynamicImagesEnabled;
  
  private Boolean disableCenterZone;
  private String[] allowedCenterZoneDims;
  private String priceDimensionName = EndecaDrivenUtil.PRICE;
  
  // Omniture properties
  private String omnitureEvar26;
  private String omnitureEvar4;
  private String omnitureEvar16;
  private String omnitureChannel;
  private String omnitureProp11;
  private String omnitureProp21;
  private String omnitureEvar6;
  
  private Boolean cacheEnabled;
  private Boolean backButtonEnabled;
  
  private boolean expandFilterVar45;
  
  private Boolean noResultsSale;
  private Boolean useMainSearchType;
  
  private static HashMap<String, SearchParameters> searchParametersMap = new HashMap<String, SearchParameters>();
  private static final String DEFAULT_SEARCH_PARAMETERS = "default";
  
  private boolean combineResultsMessageAndFilters = false;
  private String autoCorrectedText = "AUTOCORRECTED TO";
  private String clearAllFiltersText = "CLEAR ALL FILTERS";
  
  // Only for LC
  private String useNavCatDim;
  private Properties queryStringMap;
  private Boolean expandWeb1 = false;
  private Boolean filterWeb1 = false;
  private Boolean useExpandedResults = false;
  private boolean crossPromoteLinksEnable = false;
  private String zoneName;
  private String centerZoneName;
  private Boolean showPriceFilterCurrencyCode;
  
  // expanded results, only LC for now
  private String nullResultsCopyPrefix;
  private String expandedResultsCopyPrefix;
  private String expandedResultsLinkCopyPrefix;
  private String contentHeaderForEdgeCaseFile;
  static {
    // The searchParametersMap is a map of different search parameters
    // (field list, dimensions, etc)
    // that a page can use to selectively retrieve specific fields and
    // dimensions from Endeca.Pages that extend
    // /nm/commerce/pageDef/Search inherit the default searchParameters
    // unless they
    // a) provide a different searchParametersName property (and modify this
    // class to
    // define that name) or b) provide a new page definition that overrides
    // the
    // getSearchParameters() method of this class and returns some new
    // searchParameters not defined here.
    final SearchParameters searchParameters = new SearchParameters();
    searchParameters.addDefaultFieldList();
    searchParametersMap.put(DEFAULT_SEARCH_PARAMETERS, searchParameters);
    System.out.println("EndecaTemplatePageDefinition: finished static initialization");
  }
  
  public static SearchParameters getSearchParameters(final String key) {
    SearchParameters returnValue = null;
    final SearchParameters searchParameters = searchParametersMap.get(key);
    if (searchParameters != null) {
      // clone the search parameters since it has mutable members
      returnValue = new SearchParameters(searchParameters);
    } else {
      returnValue = new SearchParameters();
    }
    
    return returnValue;
  }
  
  public EndecaTemplatePageDefinition() {
    super();
    setType("endecaTemplate");
    
    linkTemplate = this;
    viewAllTemplate = this;
    landingTemplate = this;
  }
  
  public void setCombineResultsMessageAndFilters(final boolean value) {
    combineResultsMessageAndFilters = value;
  }
  
  public boolean getCombineResultsMessageAndFilters() {
    return getValue(combineResultsMessageAndFilters, basis, "getCombineResultsMessageAndFilters");
  }
  
  public void setClearAllFiltersText(final String value) {
    clearAllFiltersText = value;
  }
  
  public String getClearAllFiltersText() {
    return getValue(clearAllFiltersText, basis, "getClearAllFiltersText");
  }
  
  public void setAutoCorrectedText(final String value) {
    autoCorrectedText = value;
  }
  
  public String getAutoCorrectedText() {
    return getValue(autoCorrectedText, basis, "getAutoCorrectedText");
  }
  
  public String getSearchParametersName() {
    return getValue(searchParametersName, basis, "getSearchParametersName");
  }
  
  public void setSearchParametersName(final String value) {
    searchParametersName = value;
  }
  
  public SearchParameters getSearchParameters() {
    return getSearchParameters(searchParametersName);
  }
  
  public String getSearchType() {
    return getValue(searchType, basis, "getSearchType");
  }
  
  public void setSearchType(final String searchType) {
    this.searchType = searchType;
  }
  
  public String getPageSize() {
    return getValue(pageSize, basis, "getPageSize");
  }
  
  public void setPageSize(final String pageSize) {
    this.pageSize = pageSize;
  }
  
  public String getVaPageSize() {
    return getValue(vaPageSize, basis, "getVaPageSize");
  }
  
  public void setVaPageSize(final String vaPageSize) {
    this.vaPageSize = vaPageSize;
  }
  
  public String getQueryString() {
    return getValue(queryString, basis, "getQueryString");
  }
  
  public void setQueryString(final String queryString) {
    this.queryString = queryString;
  }
  
  public String getDefaultQueryString() {
    return getValue(defaultQueryString, basis, "getDefaultQueryString");
  }
  
  public void setDefaultQueryString(final String defaultQueryString) {
    this.defaultQueryString = defaultQueryString;
  }
  
  public String getAdditionalQueryString() {
    return getValue(additionalQueryString, basis, "getAdditionalQueryString");
  }
  
  public void setAdditionalQueryString(final String additionalQueryString) {
    this.additionalQueryString = additionalQueryString;
  }
  
  public String getSdiAdditionalQueryString() {
    return getValue(sdiAdditionalQueryString, basis, "getSdiAdditionalQueryString");
  }
  
  public void setSdiAdditionalQueryString(final String sdiAdditionalQueryString) {
    this.sdiAdditionalQueryString = sdiAdditionalQueryString;
  }
  
  public String getNonSaleQueryString() {
    return nonSaleQueryString;
  }
  
  public void setNonSaleQueryString(final String nonSaleQueryString) {
    this.nonSaleQueryString = nonSaleQueryString;
  }
  
  public String getExposedDimensions() {
    return getValue(exposedDimensions, basis, "getExposedDimensions");
  }
  
  public void setExposedDimensions(final String exposedDimensions) {
    this.exposedDimensions = exposedDimensions;
  }
  
  public String getDefaultItemIdForSearch() {
    return getValue(defaultItemIdForSearch, basis, "getDefaultItemIdForSearch");
  }
  
  public void setDefaultItemIdForSearch(final String defaultItemIdForSearch) {
    this.defaultItemIdForSearch = defaultItemIdForSearch;
  }
  
  public String getZoneUsedToCalculateColumns() {
    return getValue(zoneUsedToCalculateColumns, basis, "getZoneUsedToCalculateColumns");
  }
  
  public void setZoneUsedToCalculateColumns(final String zoneUsedToCalculateColumns) {
    this.zoneUsedToCalculateColumns = zoneUsedToCalculateColumns;
  }
  
  @Override
  public Boolean getEnableFlagOverlay() {
    return getValue(enableFlagOverlay, basis, "getEnableFlagOverlay");
  }
  
  @Override
  public void setEnableFlagOverlay(final Boolean enableFlagOverlay) {
    this.enableFlagOverlay = enableFlagOverlay;
  }
  
  @Override
  public String[] getPreTextFlags() {
    return getValue(preTextFlags, basis, "getPreTextFlags");
  }
  
  @Override
  public void setPreTextFlags(final String[] preTextFlags) {
    this.preTextFlags = preTextFlags;
  }
  
  @Override
  public String[] getPostTextFlags() {
    return getValue(postTextFlags, basis, "getPostTextFlags");
  }
  
  @Override
  public void setPostTextFlags(final String[] postTextFlags) {
    this.postTextFlags = postTextFlags;
  }
  
  public String getImageKeyIndicator() {
    return getValue(imageKeyIndicator, basis, "getImageKeyIndicator");
  }
  
  public void setImageKeyIndicator(final String imageKeyIndicator) {
    this.imageKeyIndicator = imageKeyIndicator;
  }
  
  @Override
  public String getThumbnailTextLinkType() {
    return getValue(thumbnailTextLinkType, basis, "getThumbnailTextLinkType");
  }
  
  @Override
  public void setThumbnailTextLinkType(final String thumbnailTextLinkType) {
    this.thumbnailTextLinkType = thumbnailTextLinkType;
  }
  
  public String getNumCols() {
    return getValue(numCols, basis, "getNumCols");
  }
  
  public void setNumCols(final String numCols) {
    this.numCols = numCols;
  }
  
  public String getBreadCrumbDimensionNumCols() {
    return getValue(breadCrumbDimensionNumCols, basis, "getBreadCrumbDimensionNumCols");
  }
  
  public void setBreadCrumbDimensionNumCols(final String breadCrumbDimensionNumCols) {
    this.breadCrumbDimensionNumCols = breadCrumbDimensionNumCols;
  }
  
  @Override
  public String getOmnitureBreadCrumbingSeparator() {
    return getValue(omnitureBreadCrumbingSeparator, basis, "getOmnitureBreadCrumbingSeparator");
  }
  
  @Override
  public void setOmnitureBreadCrumbingSeparator(final String omnitureBreadCrumbingSeparator) {
    this.omnitureBreadCrumbingSeparator = omnitureBreadCrumbingSeparator;
  }
  
  public String getNumColsMerchZone() {
    return getValue(numColsMerchZone, basis, "getNumColsMerchZone");
  }
  
  public void setNumColsMerchZone(final String numColsMerchZone) {
    this.numColsMerchZone = numColsMerchZone;
  }
  
  public String getNumColsShort() {
    return getValue(numColsShort, basis, "getNumColsShort");
  }
  
  public void setNumColsShort(final String numColsShort) {
    this.numColsShort = numColsShort;
  }
  
  public String getVaThreshold() {
    return getValue(vaThreshold, basis, "getVaThreshold");
  }
  
  public void setVaThreshold(final String vaThreshold) {
    this.vaThreshold = vaThreshold;
  }
  
  public String getVaPageSizeShort() {
    return getValue(vaPageSizeShort, basis, "getVaPageSizeShort");
  }
  
  public void setVaPageSizeShort(final String vaPageSizeShort) {
    this.vaPageSizeShort = vaPageSizeShort;
  }
  
  public String getVaThresholdShort() {
    return getValue(vaThresholdShort, basis, "getVaThresholdShort");
  }
  
  public void setVaThresholdShort(final String vaThresholdShort) {
    this.vaThresholdShort = vaThresholdShort;
  }
  
  public Boolean getShowDesignerBox() {
    return getValue(showDesignerBox, basis, "getShowDesignerBox");
  }
  
  public void setShowDesignerBox(final Boolean showDesignerBox) {
    this.showDesignerBox = showDesignerBox;
  }
  
  @Override
  public Boolean getShowOnlyXLeft() {
    return getValue(showOnlyXLeft, basis, "getShowOnlyXLeft");
  }
  
  @Override
  public void setShowOnlyXLeft(final Boolean showOnlyXLeft) {
    this.showOnlyXLeft = showOnlyXLeft;
  }
  
  @Override
  public String getFlagDirectoryKey() {
    return getValue(flagDirectoryKey, basis, "getFlagDirectoryKey");
  }
  
  @Override
  public void setFlagDirectoryKey(final String flagDirectoryKey) {
    this.flagDirectoryKey = flagDirectoryKey;
  }
  
  public String getFlagRetainDimensionControls() {
    return getValue(flagRetainDimensionControls, basis, "getFlagRetainDimensionControls");
  }
  
  public void setFlagRetainDimensionControls(final String flagRetainDimensionControls) {
    this.flagRetainDimensionControls = flagRetainDimensionControls;
  }
  
  public String getFlagRetainSortsOnNav() {
    return getValue(flagRetainSortsOnNav, basis, "getFlagRetainSortsOnNav");
  }
  
  public void setFlagRetainSortsOnNav(final String flagRetainSortsOnNav) {
    this.flagRetainSortsOnNav = flagRetainSortsOnNav;
  }
  
  public void setRemoveDescriptorsForWeb2(final Boolean value) {
    removeDescriptorsForWeb2 = value;
  }
  
  public Boolean getRemoveDescriptorsForWeb2() {
    return getValue(removeDescriptorsForWeb2, basis, "getRemoveDescriptorsForWeb2");
  }
  
  public String getTemplatePath() {
    return getValue(templatePath, basis, "getTemplatePath");
  }
  
  public void setTemplatePath(final String templatePath) {
    this.templatePath = templatePath;
  }
  
  public String getLeftNavViewAllCategoriesButton() {
    return getValue(leftNavViewAllCategoriesButton, basis, "getLeftNavViewAllCategoriesButton");
  }
  
  public void setLeftNavViewAllCategoriesButton(final String leftNavViewAllCategoriesButton) {
    this.leftNavViewAllCategoriesButton = leftNavViewAllCategoriesButton;
  }
  
  public String getPagingNextButton() {
    return getValue(pagingNextButton, basis, "getPagingNextButton");
  }
  
  public void setPagingNextButton(final String pagingNextButton) {
    this.pagingNextButton = pagingNextButton;
  }
  
  public String getPagingPreviousButton() {
    return getValue(pagingPreviousButton, basis, "getPagingPreviousButton");
  }
  
  public void setPagingPreviousButton(final String pagingPreviousButton) {
    this.pagingPreviousButton = pagingPreviousButton;
  }
  
  public String getPagingViewAllButton() {
    return getValue(pagingViewAllButton, basis, "getPagingViewAllButton");
  }
  
  public void setPagingViewAllButton(final String pagingViewAllButton) {
    this.pagingViewAllButton = pagingViewAllButton;
  }
  
  public EndecaTemplatePageDefinition getLandingTemplate() {
    return getValue(landingTemplate, basis, "getLandingTemplate");
  }
  
  public void setLandingTemplate(final EndecaTemplatePageDefinition landingTemplate) {
    this.landingTemplate = landingTemplate;
  }
  
  public EndecaTemplatePageDefinition getViewAllTemplate() {
    return getValue(viewAllTemplate, basis, "EndecaTemplatePageDefinition");
  }
  
  public void setViewAllTemplate(final EndecaTemplatePageDefinition viewAllTemplate) {
    this.viewAllTemplate = viewAllTemplate;
  }
  
  @Override
  public EndecaTemplatePageDefinition getLinkTemplate() {
    return getValue(linkTemplate, basis, "getLinkTemplate");
  }
  
  @Override
  public void setLinkTemplate(final EndecaTemplatePageDefinition linkTemplate) {
    this.linkTemplate = linkTemplate;
  }
  
  public EndecaTemplatePageDefinition getcatalogLandingTemplate() {
    return getValue(catalogLandingTemplate, basis, "getcatalogLandingTemplate");
  }
  
  public void setCatalogLandingTemplateTemplate(final EndecaTemplatePageDefinition catalogLandingTemplate) {
    this.catalogLandingTemplate = catalogLandingTemplate;
  }
  
  public Boolean getShowPageText() {
    return getValue(showPageText, basis, "getShowPageText");
  }
  
  public void setShowPageText(final Boolean showPageText) {
    this.showPageText = showPageText;
  }
  
  public Boolean getHighlightDesigner() {
    return getValue(highlightDesigner, basis, "getHighlightDesigner");
  }
  
  public void setHighlightDesigner(final Boolean highlightDesigner) {
    this.highlightDesigner = highlightDesigner;
  }
  
  public String getOmnitureEvar26() {
    return getValue(omnitureEvar26, basis, "getOmnitureEvar26");
  }
  
  public void setOmnitureEvar26(final String omnitureEvar26) {
    this.omnitureEvar26 = omnitureEvar26;
  }
  
  public String getOmnitureProp11() {
    return getValue(omnitureProp11, basis, "getOmnitureProp11");
  }
  
  public void setOmnitureProp11(final String omnitureProp11) {
    this.omnitureProp11 = omnitureProp11;
  }
  
  public String getOmnitureEvar6() {
    return getValue(omnitureEvar6, basis, "getOmnitureEvar6");
  }
  
  public void setOmnitureEvar6(final String omnitureEvar6) {
    this.omnitureEvar6 = omnitureEvar6;
  }
  
  public String getOmnitureEvar4() {
    return getValue(omnitureEvar4, basis, "getOmnitureEvar4");
  }
  
  public void setOmnitureEvar4(final String omnitureEvar4) {
    this.omnitureEvar4 = omnitureEvar4;
  }
  
  public String getOmnitureChannel() {
    return getValue(omnitureChannel, basis, "getOmnitureChannel");
  }
  
  public void setOmnitureChannel(final String omnitureChannel) {
    this.omnitureChannel = omnitureChannel;
  }
  
  public String getOmnitureEvar16() {
    return getValue(omnitureEvar16, basis, "getOmnitureEvar16");
  }
  
  public void setOmnitureEvar16(final String omnitureEvar16) {
    this.omnitureEvar16 = omnitureEvar16;
  }
  
  public String getOmnitureProp21() {
    return getValue(omnitureProp21, basis, "getOmnitureProp21");
  }
  
  public void setOmnitureProp21(final String omnitureProp21) {
    this.omnitureProp21 = omnitureProp21;
  }
  
  public String getEvar4DesignerIdxOverride() {
    return getValue(evar4DesignerIdxOverride, basis, "getEvar4DesignerIdxOverride");
  }
  
  public void setEvar4DesignerIdxOverride(final String evar4DesignerIdxOverride) {
    this.evar4DesignerIdxOverride = evar4DesignerIdxOverride;
  }
  
  public void setSaleSearchTemplate(final EndecaTemplatePageDefinition saleSearchTemplate) {
    this.saleSearchTemplate = saleSearchTemplate;
  }
  
  public EndecaTemplatePageDefinition getSaleSearchTemplate() {
    return getValue(saleSearchTemplate, basis, "getSaleSearchTemplate");
  }
  
  public void setLcTemplate(final EndecaTemplatePageDefinition lcTemplate) {
    this.lcTemplate = lcTemplate;
  }
  
  public EndecaTemplatePageDefinition getLcTemplate() {
    return getValue(lcTemplate, basis, "getLcTemplate");
  }
  
  public void setBcTemplate(final EndecaTemplatePageDefinition bcTemplate) {
    this.bcTemplate = bcTemplate;
  }
  
  public EndecaTemplatePageDefinition getBcTemplate() {
    return getValue(bcTemplate, basis, "getBcTemplate");
  }
  
  public void setPvTemplate(final EndecaTemplatePageDefinition pvTemplate) {
    this.pvTemplate = pvTemplate;
  }
  
  public EndecaTemplatePageDefinition getPvTemplate() {
    return getValue(pvTemplate, basis, "getPvTemplate");
  }
  
  public void setExplicitParentId(final String explicitParentId) {
    this.explicitParentId = explicitParentId;
  }
  
  public String getExplicitParentId() {
    return getValue(explicitParentId, basis, "getExplicitParentId");
  }
  
  public void setSecondarySorts(final String secondarySorts) {
    this.secondarySorts = secondarySorts;
  }
  
  public String getSecondarySorts() {
    return getValue(secondarySorts, basis, "getSecondarySorts");
  }
  
  public void setEndecaSaleCategoryId(final String endecaSaleCategoryId) {
    this.endecaSaleCategoryId = endecaSaleCategoryId;
  }
  
  public String getEndecaSaleCategoryId() {
    return getValue(endecaSaleCategoryId, basis, "getEndecaSaleCategoryId");
  }
  
  public void setElementsInRow(final Integer elementsInRow) {
    this.elementsInRow = elementsInRow;
  }
  
  public Integer getElementsInRow() {
    return getValue(elementsInRow, basis, "getElementsInRow");
  }
  
  public void setUseBackToTopLink(final Boolean useBackToTopLink) {
    this.useBackToTopLink = useBackToTopLink;
  }
  
  public Boolean getUseBackToTopLink() {
    return getValue(useBackToTopLink, basis, "getUseBackToTopLink");
  }
  
  public void setResultsZoneGblock(final Boolean resultsZoneGblock) {
    this.resultsZoneGblock = resultsZoneGblock;
  }
  
  public Boolean getResultsZoneGblock() {
    return getValue(resultsZoneGblock, basis, "getResultsZoneGblock");
  }
  
  @Override
  public String getPagingSeparator() {
    return getValue(pagingSeparator, basis, "getPagingSeparator");
  }
  
  @Override
  public void setPagingSeparator(final String pagingSeparator) {
    this.pagingSeparator = pagingSeparator;
  }
  
  public String getUseNavCatDim() {
    return getValue(useNavCatDim, basis, "getUseNavCatDim");
  }
  
  public void setUseNavCatDim(final String useNavCatDim) {
    this.useNavCatDim = useNavCatDim;
  }
  
  public String getSwitchView() {
    return getValue(switchView, basis, "getSwitchView");
  }
  
  public void setSwitchView(final String switchView) {
    this.switchView = switchView;
  }
  
  public Properties getQueryStringMap() {
    return queryStringMap;
  }
  
  public void setQueryStringMap(final Properties queryStringMap) {
    this.queryStringMap = queryStringMap;
  }
  
  public Boolean getExpandWeb1() {
    return expandWeb1;
  }
  
  public void setExpandWeb1(final Boolean expandWeb1) {
    this.expandWeb1 = expandWeb1;
  }
  
  public Boolean getFilterWeb1() {
    return filterWeb1;
  }
  
  public void setFilterWeb1(final Boolean filterWeb1) {
    this.filterWeb1 = filterWeb1;
  }
  
  public Boolean getUseExpandedResults() {
    return useExpandedResults;
  }
  
  public void setUseExpandedResults(final Boolean useExpandedResults) {
    this.useExpandedResults = useExpandedResults;
  }
  
  public Boolean getCacheEnabled() {
    return cacheEnabled;
  }
  
  public void setCacheEnabled(final Boolean cacheEnabled) {
    this.cacheEnabled = cacheEnabled;
  }
  
  public Boolean getBackButtonEnabled() {
    return getValue(backButtonEnabled, basis, "getBackButtonEnabled");
  }
  
  public void setBackButtonEnabled(final Boolean backButtonEnabled) {
    this.backButtonEnabled = backButtonEnabled;
  }
  
  public String getDefaultPageSize() {
    return defaultPageSize;
  }
  
  public void setDefaultPageSize(final String defaultPageSize) {
    this.defaultPageSize = defaultPageSize;
  }
  
  public String getLeftNavDisplayTreePath() {
    return getValue(leftNavDisplayTreePath, basis, "getLeftNavDisplayTreePath");
  }
  
  public void setLeftNavDisplayTreePath(final String leftNavDisplayTreePath) {
    this.leftNavDisplayTreePath = leftNavDisplayTreePath;
  }
  
  public boolean isExpandFilterVar45() {
    return getValue(expandFilterVar45, basis, "isExpandFilterVar45");
  }
  
  public void setExpandFilterVar45(final boolean expandFilterVar45) {
    this.expandFilterVar45 = expandFilterVar45;
  }
  
  public Boolean getNoResultsSale() {
    return noResultsSale;
  }
  
  public void setNoResultsSale(final Boolean noResultsSale) {
    this.noResultsSale = noResultsSale;
  }
  
  public String getSearchReportPath() {
    return getValue(searchReportPath, basis, "getSearchReportPath");
  }
  
  public void setSearchReportPath(final String searchReportPath) {
    this.searchReportPath = searchReportPath;
  }
  
  public void setAutoSuggestFormat(final String autoSuggestFormat) {
    this.autoSuggestFormat = autoSuggestFormat;
  }
  
  public String getAutoSuggestFormat() {
    return getValue(autoSuggestFormat, basis, "getAutoSuggestFormat");
  }
  
  public EndecaTemplatePageDefinition getCatalogLandingTemplate() {
    return catalogLandingTemplate;
  }
  
  public void setCatalogLandingTemplate(final EndecaTemplatePageDefinition catalogLandingTemplate) {
    this.catalogLandingTemplate = catalogLandingTemplate;
  }
  
  public String getResultsReportFormat() {
    return getValue(resultsReportFormat, basis, "getResultsReportFormat");
  }
  
  public void setResultsReportFormat(final String resultsReportFormat) {
    this.resultsReportFormat = resultsReportFormat;
  }
  
  @Override
  public String getSortPath() {
    return getValue(sortPath, basis, "getSortPath");
  }
  
  @Override
  public void setSortPath(final String sortPath) {
    this.sortPath = sortPath;
  }
  
  @Override
  public String getItemsPerPagePath() {
    return getValue(itemsPerPagePath, basis, "getItemsPerPagePath");
  }
  
  @Override
  public void setItemsPerPagePath(final String itemsPerPagePath) {
    this.itemsPerPagePath = itemsPerPagePath;
  }
  
  @Override
  public String getFilterPath() {
    return getValue(filterPath, basis, "getFilterPath");
  }
  
  @Override
  public void setFilterPath(final String filterPath) {
    this.filterPath = filterPath;
  }
  
  public String getPagingFirstLastSeparator() {
    return getValue(pagingFirstLastSeparator, basis, "getPagingFirstLastSeparator");
  }
  
  public void setPagingFirstLastSeparator(final String pagingFirstLastSeparator) {
    this.pagingFirstLastSeparator = pagingFirstLastSeparator;
  }
  
  public Boolean getPagingLinkFirst() {
    return getValue(pagingLinkFirst, basis, "getPagingLinkFirst");
  }
  
  public void setPagingLinkFirst(final Boolean pagingLinkFirst) {
    this.pagingLinkFirst = pagingLinkFirst;
  }
  
  public Boolean getPagingLinkLast() {
    return getValue(pagingLinkLast, basis, "getPagingLinkLast");
  }
  
  public void setPagingLinkLast(final Boolean pagingLinkLast) {
    this.pagingLinkLast = pagingLinkLast;
  }
  
  public Boolean getSortFilterAtTop() {
    return getValue(sortFilterAtTop, basis, "getSortFilterAtTop");
  }
  
  public void setSortFilterAtTop(final Boolean sortFilterAtTop) {
    this.sortFilterAtTop = sortFilterAtTop;
  }
  
  public String getAlternateAjaxSearchMethod() {
    return getValue(alternateAjaxSearchMethod, basis, "getAlternateAjaxSearchMethod");
  }
  
  public void setAlternateAjaxSearchMethod(final String alternateAjaxSearchMethod) {
    this.alternateAjaxSearchMethod = alternateAjaxSearchMethod;
  }
  
  public Boolean getEnableFilterCurrentSelectionPopup() {
    return getValue(enableFilterCurrentSelectionPopup, basis, "getEnableFilterCurrentSelectionPopup");
  }
  
  public void setEnableFilterCurrentSelectionPopup(final Boolean enableFilterCurrentSelectionPopup) {
    this.enableFilterCurrentSelectionPopup = enableFilterCurrentSelectionPopup;
  }
  
  public String getNullResultsCopyPrefix() {
    return getValue(nullResultsCopyPrefix, basis, "getNullResultsCopyPrefix");
  }
  
  public void setNullResultsCopyPrefix(final String nullResultsCopyPrefix) {
    this.nullResultsCopyPrefix = nullResultsCopyPrefix;
  }
  
  public String getExpandedResultsCopyPrefix() {
    return getValue(expandedResultsCopyPrefix, basis, "getExpandedResultsCopyPrefix");
  }
  
  public void setExpandedResultsCopyPrefix(final String expandedResultsCopyPrefix) {
    this.expandedResultsCopyPrefix = expandedResultsCopyPrefix;
  }
  
  public String getExpandedResultsLinkCopyPrefix() {
    return getValue(expandedResultsLinkCopyPrefix, basis, "getExpandedResultsLinkCopyPrefix");
  }
  
  public void setExpandedResultsLinkCopyPrefix(final String expandedResultsLinkCopyPrefix) {
    this.expandedResultsLinkCopyPrefix = expandedResultsLinkCopyPrefix;
  }
  
  public Boolean getDisableCenterZone() {
    return getValue(disableCenterZone, basis, "getDisableCenterZone");
  }
  
  public void setDisableCenterZone(final Boolean disableCenterZone) {
    this.disableCenterZone = disableCenterZone;
  }
  
  public String[] getAllowedCenterZoneDims() {
    return getValue(allowedCenterZoneDims, basis, "getAllowedCenterZoneDims");
  }
  
  public void setAllowedCenterZoneDims(final String[] allowedCenterZoneDims) {
    this.allowedCenterZoneDims = allowedCenterZoneDims;
  }
  
  public boolean getCrossPromoteLinksEnable() {
    return getValue(crossPromoteLinksEnable, basis, "getCrossPromoteLinksEnable");
  }
  
  public void setCrossPromoteLinksEnable(final boolean crossPromoteLinksEnable) {
    this.crossPromoteLinksEnable = crossPromoteLinksEnable;
  }
  
  public String getZoneName() {
    return getValue(zoneName, basis, "getZoneName");
  }
  
  public void setZoneName(final String zoneName) {
    this.zoneName = zoneName;
  }
  
  public String getCenterZoneName() {
    return getValue(centerZoneName, basis, "getCenterZoneName");
  }
  
  public void setCenterZoneName(final String centerZoneName) {
    this.centerZoneName = centerZoneName;
  }
  
  public void setPriceDimensionName(final String value) {
    priceDimensionName = value;
  }
  
  public String getPriceDimensionName() {
    return getValue(priceDimensionName, basis, "getPriceDimensionName");
  }
  
  public void setUseMainSearchType(final Boolean useMainSearchType) {
    this.useMainSearchType = useMainSearchType;
  }
  
  public Boolean getUseMainSearchType() {
    return getValue(useMainSearchType, basis, "getUseMainSearchType");
  }
  
  public void setShowPriceFilterCurrencyCode(final Boolean showPriceFilterCurrencyCode) {
    this.showPriceFilterCurrencyCode = showPriceFilterCurrencyCode;
  }
  
  public Boolean getShowPriceFilterCurrencyCode() {
    return getValue(showPriceFilterCurrencyCode, basis, "getShowPriceFilterCurrencyCode");
  }
  
  public void setAllTabLabel(final String allTabLabel) {
    this.allTabLabel = allTabLabel;
  }
  
  public String getAllTabLabel() {
    return getValue(allTabLabel, basis, "getAllTabLabel");
  }
  
  public void setSaleTabLabel(final String saleTabLabel) {
    this.saleTabLabel = saleTabLabel;
  }
  
  public String getSaleTabLabel() {
    return getValue(saleTabLabel, basis, "getSaleTabLabel");
  }
  
  public void setBoxedResults(final Boolean boxedResults) {
    this.boxedResults = boxedResults;
  }
  
  public Boolean getBoxedResults() {
    return getValue(boxedResults, basis, "getBoxedResults");
  }
  
  public Boolean getIsRWD() {
    return getValue(isRWD, basis, "getIsRWD");
  }
  
  public void setIsRWD(final Boolean isRWD) {
    this.isRWD = isRWD;
  }
  
  @Override
  public Boolean getDynamicImagesEnabled() {
    return getValue(dynamicImagesEnabled, basis, "getDynamicImagesEnabled");
  }
  
  @Override
  public void setDynamicImagesEnabled(final Boolean dynamicImagesEnabled) {
    this.dynamicImagesEnabled = dynamicImagesEnabled;
  }

	public String getContentHeaderForEdgeCaseFile() {
		return contentHeaderForEdgeCaseFile;
	}

	public void setContentHeaderForEdgeCaseFile( String contentHeaderForEdgeCaseFile ) {
		this.contentHeaderForEdgeCaseFile = contentHeaderForEdgeCaseFile;
	}

} // End of Class
