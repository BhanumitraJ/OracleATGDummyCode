package com.nm.commerce.pagedef.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import atg.nucleus.logging.ApplicationLogging;
import atg.nucleus.logging.ClassLoggingFactory;
import atg.repository.RepositoryItem;
import atg.servlet.DynamoHttpServletRequest;
import atg.servlet.ServletUtil;

import com.nm.catalog.navigation.NMCategory;
import com.nm.commerce.NMProfile;
import com.nm.commerce.pagedef.definition.PageDefinition;
import com.nm.commerce.pagedef.evaluator.PageEvaluator;
import com.nm.commerce.pagedef.model.bean.Breadcrumb;
import com.nm.common.NMBorderFreeGetQuoteResponse;
import com.nm.common.NMBorderFreePlaceOrderResponse;
import com.nm.droplet.bloomreach.BloomreachSuggest;
import com.nm.sitemessages.OmnitureData;
import com.nm.storeinventory.InStoreSearchInput;

/**
 * Model containing variables for use throughout a page.
 */
public class PageModel {
  
  private final ApplicationLogging mLogging = ClassLoggingFactory.getFactory().getLoggerForClass(PageModel.class);
  
  private String omnitureBeanClass;
  private NMCategory activeCategory;
  private NMCategory homeCategory;
  private List<NMCategory> seoFooter;
  private boolean isSaleCategory;
  private boolean isRobot;
  private String bodyClass = null;
  private PageEvaluator pageEvaluator;
  private PageDefinition pageDefinition;
  private Future<Map<String, String>> bloomreachFuture;
  private Map<String, String> bloomreachWidget;
  private boolean bvEnabled; // indicates whether BazaarVoice is turned on for
  // the site and the page
  private boolean bvLoadApi; // allows you to turn OFF js api script if it's
  // not needed. By default, bvLoadApi is true if
  // bvEnabled is true
  private String headerPath; // the header fragment (does not include <head>
  // ... </head>)
  private String footerPath; // the footer fragment
  private boolean isInternational; // will be set to 'true', if the profile
  // country preference value is supported
  // by FiftyOne
  private boolean displayMobile;
  private InStoreSearchInput inStoreSearchInput;
  private NMProfile profile;
  private NMBorderFreeGetQuoteResponse nmBorderFreeGetQuoteResponse;
  
  private NMBorderFreePlaceOrderResponse nmBorderFreePlaceOrderResponse;
  // variable to check internationalCopyBlock
  private String internationalCopyBlock;
  
  // INT-1900 : property to store restriction message
  private String restrMsg;
  
  // INT-2239 changes start here
  private boolean showMktgtag;
  
  private boolean restrictGoogleApi;
  
  private String onlineOrStore;
  private Breadcrumb[] breadcrumbs;
  private Breadcrumb currentBreadcrumb;
  
  /** The pageType. */
  private String pageType;
  
  /** The secondaryIdentifier. */
  private String secondaryIdentifier;
  /** The account recently registered. */
  private boolean accountRecentlyRegistered;
  
  private boolean showMIT; // Show Merchant Inventory Tool (LC only)
  
  /*
   * Map to control the UI elements display on screen. Elements like CLC, ALIPAY.
   */
  private Map<String, Boolean> elementDisplaySwitchMap = new HashMap<String, Boolean>();
  private boolean displayPromoSticker;
  private Boolean isLastChild = false;
  
  /**
   * String variable to hold Pop up messsage.
   */
  private String popupMsg;
  private String freeBlitzAbTestGroup;
  private String freeBlitzShippingAbTestGroup;
  private String googleMapUrl;
  private List<String> vendorQuantityErrorMsgList;
  private List<OmnitureData>  vendorQuantityOmnitureData;

  private List<String> selectedCategoryList;
  /**
   * Property to hold facetFieldsCategoryList
   */
  private List<Map<String, Object>> facetFieldsCategoryList;
  
  private List<String> selectedDesigner;
  private List<String> selectedPrice;
  private List<String> selectedSize;
  
  private List<String> selectedColor;
  
  private String brBreadCrumURL;
  
  /** The display ILink data */
  private boolean displayILinkData;
  /** set Attributes for SitePersonalization cookies */
  private Map<String, String> toolTipContentMap;
  
  private Boolean jumioEnabled = false;
  private String jumioToken;
  
  /**
   * Checks if is display ILink data.
   * 
   * @return true, if is display ILink data
   */
  public boolean isDisplayILinkData() {
    return displayILinkData;
  }
  
  /**
   * Sets the display ILink data.
   * 
   * @param displayILinkData
   *          the display ILink data
   */
  public void setDisplayILinkData(boolean displayILinkData) {
    this.displayILinkData = displayILinkData;
  }

  /**
   * @return the brBreadCrumURL
   */
  public String getBrBreadCrumURL() {
    return brBreadCrumURL;
  }
  
  /**
   * @param brBreadCrumURL
   *          the brBreadCrumURL to set
   */
  public void setBrBreadCrumURL(final String brBreadCrumURL) {
    this.brBreadCrumURL = brBreadCrumURL;
  }
  
  /**
   * @return the selectedPrice
   */
  public List<String> getSelectedPrice() {
    return selectedPrice;
  }
  
  /**
   * @param selectedPrice
   *          the selectedPrice to set
   */
  public void setSelectedPrice(final List<String> selectedPrice) {
    this.selectedPrice = selectedPrice;
  }
  
  /**
   * @return the selectedSize
   */
  public List<String> getSelectedSize() {
    return selectedSize;
  }
  
  /**
   * @param selectedSize
   *          the selectedSize to set
   */
  public void setSelectedSize(final List<String> selectedSize) {
    this.selectedSize = selectedSize;
  }
  
  /**
   * @return the selectedColor
   */
  public List<String> getSelectedColor() {
    return selectedColor;
  }
  
  /**
   * @param selectedColor
   *          the selectedColor to set
   */
  public void setSelectedColor(final List<String> selectedColor) {
    this.selectedColor = selectedColor;
  }
  
  /**
   * @return the selectedCategoryList
   */
  public List<String> getSelectedCategoryList() {
    return selectedCategoryList;
  }
  
  /**
   * @param selectedCategoryList
   *          the selectedCategoryList to set
   */
  public void setSelectedCategoryList(final List<String> selectedCategoryList) {
    this.selectedCategoryList = selectedCategoryList;
  }
  
  /**
   * @return the facetFieldsCategoryList
   */
  public List<Map<String, Object>> getFacetFieldsCategoryList() {
    return facetFieldsCategoryList;
  }
  
  /**
   * @param facetFieldsCategoryList
   *          the facetFieldsCategoryList to set
   */
  public void setFacetFieldsCategoryList(final List<Map<String, Object>> facetFieldsCategoryList) {
    this.facetFieldsCategoryList = facetFieldsCategoryList;
  }
  
  /**
   * @return the selectedDesigner
   */
  public List<String> getSelectedDesigner() {
    return selectedDesigner;
  }
  
  /**
   * @param selectedDesigner
   *          the selectedDesigner to set
   */
  public void setSelectedDesigner(final List<String> selectedDesigner) {
    this.selectedDesigner = selectedDesigner;
  }
  
  private long recordCount;
  
  private String previousProductUrl;
  
  private String currentProdcutUrl;
  
  private String nextProductUrl;
  
  private int perviousProduct;
  private int nextProduct;
  
  private int startWith;
  private String searchTerm;
  
  /** The page size. */
  private String pageSize;
  
  /**
   * Gets the page size.
   * 
   * @return the page size
   */
  public String getPageSize() {
    return this.pageSize;
  }
  
  /**
   * Sets the page size.
   * 
   * @param pageSize
   *          the new page size
   */
  public void setPageSize(final String pageSize) {
    this.pageSize = pageSize;
  }
  
  /**
   * @return the recordCount
   */
  public long getRecordCount() {
    return recordCount;
  }
  
  /**
   * @param recordCount
   *          the recordCount to set
   */
  public void setRecordCount(final long recordCount) {
    this.recordCount = recordCount;
  }
  
  /**
   * @return the previousProductUrl
   */
  public String getPreviousProductUrl() {
    return previousProductUrl;
  }
  
  /**
   * @param previousProductUrl
   *          the previousProductUrl to set
   */
  public void setPreviousProductUrl(final String previousProductUrl) {
    this.previousProductUrl = previousProductUrl;
  }
  
  /**
   * @return the currentProdcutUrl
   */
  public String getCurrentProdcutUrl() {
    return currentProdcutUrl;
  }
  
  /**
   * @param currentProdcutUrl
   *          the currentProdcutUrl to set
   */
  public void setCurrentProdcutUrl(final String currentProdcutUrl) {
    this.currentProdcutUrl = currentProdcutUrl;
  }
  
  /**
   * @return the nextProductUrl
   */
  public String getNextProductUrl() {
    return nextProductUrl;
  }
  
  /**
   * @param nextProductUrl
   *          the nextProductUrl to set
   */
  public void setNextProductUrl(final String nextProductUrl) {
    this.nextProductUrl = nextProductUrl;
  }
  
  /**
   * @return the perviousProduct
   */
  public int getPerviousProduct() {
    return perviousProduct;
  }
  
  /**
   * @param perviousProduct
   *          the perviousProduct to set
   */
  public void setPerviousProduct(final int perviousProduct) {
    this.perviousProduct = perviousProduct;
  }
  
  /**
   * @return the nextProduct
   */
  public int getNextProduct() {
    return nextProduct;
  }
  
  /**
   * @param nextProduct
   *          the nextProduct to set
   */
  public void setNextProduct(final int nextProduct) {
    this.nextProduct = nextProduct;
  }
  
  /**
   * @return the startWith
   */
  public int getStartWith() {
    return startWith;
  }
  
  /**
   * @param startWith
   *          the startWith to set
   */
  public void setStartWith(final int startWith) {
    this.startWith = startWith;
  }
  
  /** The url decoded email. */
  private String urlDecodedEmail;

  /**
   * Gets the url decoded email.
   * 
   * @return the urlDecodedEmail
   */
  public String getUrlDecodedEmail() {
    return urlDecodedEmail;
  }
  
  /**
   * Sets the url decoded email.
   * 
   * @param urlDecodedEmail
   *          the urlDecodedEmail to set
   */
  public void setUrlDecodedEmail(final String urlDecodedEmail) {
    this.urlDecodedEmail = urlDecodedEmail;
  }
  
  /** String variable to hold returns charge message refreshable content. */
  private String returnsChargeMessageRefreshableContent;
  
  /** The tag management json string. */
  private String dataDictionaryJsonString;
  
  /** The tag management data dictionary update script. */
  private String dataDictionaryUpdateScript;
  
  private boolean partiallyEvaluated;
  
  /** The page name. */
  private String pageName;

  /**
   * @return the dataDictionaryJsonString
   */
  public String getDataDictionaryJsonString() {
    return dataDictionaryJsonString;
  }
  
  /**
   * @param dataDictionaryJsonString
   *          the dataDictionaryJsonString to set
   */
  public void setDataDictionaryJsonString(final String dataDictionaryJsonString) {
    this.dataDictionaryJsonString = dataDictionaryJsonString;
  }
  
  public String getFreeBlitzShippingAbTestGroup() {
    return freeBlitzShippingAbTestGroup;
  }
  
  public void setFreeBlitzShippingAbTestGroup(final String freeBlitzShippingAbTestGroup) {
    this.freeBlitzShippingAbTestGroup = freeBlitzShippingAbTestGroup;
  }
  
  public String getFreeBlitzAbTestGroup() {
    return freeBlitzAbTestGroup;
  }
  
  public void setFreeBlitzAbTestGroup(final String freeBlitzAbTestGroup) {
    this.freeBlitzAbTestGroup = freeBlitzAbTestGroup;
  }
  
  // property to store country code present in Akamai header
  private String ipCountryCode;
  
  /**
   * @return the ipCountryCode
   */
  public String getIpCountryCode() {
    return ipCountryCode;
  }
  
  /**
   * @param ipCountryCode
   *          the ipCountryCode to set
   */
  public void setIpCountryCode(final String ipCountryCode) {
    this.ipCountryCode = ipCountryCode;
  }
  
  /**
   * @return showMktgtag
   */
  public boolean isShowMktgtag() {
    return showMktgtag;
  }
  
  /**
   * @param showMktgtag
   */
  public void setShowMktgtag(final boolean showMktgtag) {
    this.showMktgtag = showMktgtag;
  }
  
  /**
   * @return restrictGoogleApi
   */
  public boolean isRestrictGoogleApi() {
    return restrictGoogleApi;
  }
  
  /**
   * 
   * @param restrictGoogleApi
   */
  public void setRestrictGoogleApi(final boolean restrictGoogleApi) {
    this.restrictGoogleApi = restrictGoogleApi;
  }
  
  public String getOnlineOrStore() {
    
    return onlineOrStore;
    
  }
  
  public void setOnlineOrStore(final String onlineOrStore) {
    
    this.onlineOrStore = onlineOrStore;
    
  }
  
  // INT-2239 changes end here
  /**
   * @return restrMsg -type String
   */
  public String getRestrMsg() {
    return restrMsg;
  }
  
  /**
   * @param restrMsg
   */
  public void setRestrMsg(final String restrMsg) {
    this.restrMsg = restrMsg;
  }
  
  /**
   * 
   * @return isInternationalCopyBlock
   */
  public String getInternationalCopyBlock() {
    return internationalCopyBlock;
  }
  
  /**
   * 
   * @param isInternationalCopyBlock
   */
  public void setInternationalCopyBlock(final String internationalCopyBlock) {
    this.internationalCopyBlock = internationalCopyBlock;
  }
  
  private boolean onlyPinYin;
  private boolean showCounty;
  private boolean showCpf;
  
  /**
   * @return onlyPinYin -type boolean
   */
  public boolean isOnlyPinYin() {
    return onlyPinYin;
  }
  
  /**
   * @param onlyPinYin
   */
  public void setOnlyPinYin(final boolean onlyPinYin) {
    this.onlyPinYin = onlyPinYin;
  }
  
  /**
   * @return showCounty- type boolean
   */
  public boolean isShowCounty() {
    return showCounty;
  }
  
  /**
   * @param showCounty
   */
  public void setShowCounty(final boolean showCounty) {
    this.showCounty = showCounty;
  }
  
  /**
   * @return showCpf- type boolean
   */
  public boolean isShowCpf() {
    return showCpf;
  }
  
  /**
   * @param showCpf
   */
  public void setShowCpf(final boolean showCpf) {
    this.showCpf = showCpf;
  }
  
  // use this boolean property to check whether to show email icon or not.
  private boolean disableEmailIcon;
  
  /**
   * @return the disableEmailIcon
   */
  public boolean isDisableEmailIcon() {
    return disableEmailIcon;
  }
  
  /**
   * @param disableEmailIcon
   *          the disableEmailIcon to set
   */
  public void setDisableEmailIcon(final boolean disableEmailIcon) {
    this.disableEmailIcon = disableEmailIcon;
  }
  
  // use this boolean property to check whether to show localized language for
  // CLC or not.
  
  private boolean showLocalizedLanguage;
  private String countryInLocalLang;
  
  /**
   * @return the showLocalizedClcLanguage
   */
  public boolean isShowLocalizedLanguage() {
    return showLocalizedLanguage;
  }
  
  /**
   * @param showLocalizedClcLanguage
   *          the showLocalizedClcLanguage to set
   */
  public void setShowLocalizedLanguage(final boolean showLocalizedLanguage) {
    this.showLocalizedLanguage = showLocalizedLanguage;
  }
  
  /**
   * @return the countryInLocalLang
   */
  public String getCountryInLocalLang() {
    return countryInLocalLang;
  }
  
  /**
   * @param countryInLocalLang
   *          the countryInLocalLang to set
   */
  public void setCountryInLocalLang(final String countryInLocalLang) {
    this.countryInLocalLang = countryInLocalLang;
  }
  
  private boolean displayChinaRedirectAsset;
  
  public boolean getDisplayChinaRedirectAsset() {
    return displayChinaRedirectAsset;
  }
  
  public void setDisplayChinaRedirectAsset(final boolean displayChinaRedirectAsset) {
    this.displayChinaRedirectAsset = displayChinaRedirectAsset;
  }
  
  public NMBorderFreePlaceOrderResponse getNmBorderFreePlaceOrderResponse() {
    return nmBorderFreePlaceOrderResponse;
  }
  
  public void setNmBorderFreePlaceOrderResponse(final NMBorderFreePlaceOrderResponse nmBorderFreePlaceOrderResponse) {
    this.nmBorderFreePlaceOrderResponse = nmBorderFreePlaceOrderResponse;
  }
  
  public Future<Map<String, String>> getBloomreachFuture() {
    return bloomreachFuture;
  }
  
  public void setBloomreachFuture(final Future<Map<String, String>> bloomreachFuture) {
    this.bloomreachFuture = bloomreachFuture;
  }
  
  public NMBorderFreeGetQuoteResponse getNmBorderFreeGetQuoteResponse() {
    return nmBorderFreeGetQuoteResponse;
  }
  
  public void setNmBorderFreeGetQuoteResponse(final NMBorderFreeGetQuoteResponse nmBorderFreeGetQuoteResponse) {
    this.nmBorderFreeGetQuoteResponse = nmBorderFreeGetQuoteResponse;
  }
  
  public PageModel() {}
  
  public NMProfile getProfile() {
    return profile;
  }
  
  public void setProfile(final NMProfile profile) {
    this.profile = profile;
  }
  
  public InStoreSearchInput getInStoreSearchInput() {
    return inStoreSearchInput;
  }
  
  public void setInStoreSearchInput(final InStoreSearchInput inStoreSearchInput) {
    this.inStoreSearchInput = inStoreSearchInput;
  }
  
  public boolean getBloomreachHasContent() {
    final Map<String, String> widget = getBloomreachWidget();
    return BloomreachSuggest.hasContent(widget);
  }
  
  public void setBloomreachWidget(final Future<Map<String, String>> value) {
    bloomreachFuture = value;
  }
  
  public void setBloomreachWidget(final Map<String, String> value) {
    bloomreachWidget = value;
  }
  
  public Map<String, String> getBloomreachWidget() {
    Map<String, String> returnValue = null;
    if (bloomreachWidget != null) {
      returnValue = bloomreachWidget;
    } else if (bloomreachFuture != null) {
      try {
        returnValue = bloomreachFuture.get();
        bloomreachWidget = returnValue;
      } catch (final InterruptedException exception) {
    	  if (mLogging.isLoggingError()) {
    		  mLogging.logError("getBloomreachWidget(): " + exception.getMessage());
    	  }
      } catch (final ExecutionException exception) {
    	  if (mLogging.isLoggingError()) {
    		  mLogging.logError("getBloomreachWidget(): " + exception.getMessage());
    	  }
      }
    }
    
    return returnValue;
  }
  
  public void setPageDefinition(final PageDefinition value) {
    pageDefinition = value;
  }
  
  public PageDefinition getPageDefinition() {
    return pageDefinition;
  }
  
  public void setPageEvaluator(final PageEvaluator pageEvaluator) {
    this.pageEvaluator = pageEvaluator;
  }
  
  /**
   * The page evaluator is occasionally used during page execution, for example when the cached content area is evaluated.
   * 
   * @return
   */
  public PageEvaluator getPageEvaluator() {
    return pageEvaluator;
  }
  
  /**
   * The bodyClass is normally provided by the pageDefintion but it can be overridden by calling this method
   * 
   * @param value
   */
  public void setBodyClass(final String value) {
    bodyClass = value;
  }
  
  public String getBodyClass() {
    if (bodyClass == null) {
      return pageDefinition.getBodyClass();
    } else {
      return bodyClass;
    }
  }
  
  /**
   * Append classes to body class to allow modifying the body class in various places in the the evaluator
   * 
   * @param classNames
   */
  public void appendBodyClass(final String ... classNames) {
    for (final String className : classNames) {
      setBodyClass(getBodyClass() + " " + className);
    }
  }
  
  /**
   * Returns the omniture type for this page. This value is based on the page definition of the page tied to this model.
   * 
   * @return
   */
  public String getOmnitureBeanClass() {
    return omnitureBeanClass;
  }
  
  /**
   * Sets the omniture type for this page.
   * 
   * @param value
   */
  public void setOmnitureBeanClass(final String value) {
    omnitureBeanClass = value;
  }
  
  public void setActiveCategory(final NMCategory activeCategory) {
    this.activeCategory = activeCategory;
  }
  
  public NMCategory getActiveCategory() {
    return activeCategory;
  }
  
  public void setSeoFooter(final List<NMCategory> seoFooter) {
    this.seoFooter = seoFooter;
  }
  
  public List<NMCategory> getSeoFooter() {
    return seoFooter;
  }
  
  public boolean isSaleCategory() {
    return isSaleCategory;
  }
  
  public void setSaleCategory(final boolean isSaleCategory) {
    this.isSaleCategory = isSaleCategory;
  }
  
  public void setHomeCategory(final NMCategory homeCategory) {
    this.homeCategory = homeCategory;
  }
  
  public NMCategory getHomeCategory() {
    return homeCategory;
  }
  
  public String getPageId() {
    return pageDefinition.getId();
  }
  
  public boolean isRobot() {
    return isRobot;
  }
  
  public void setRobot(final boolean isRobot) {
    this.isRobot = isRobot;
  }
  
  public String getHeaderPath() {
    return headerPath;
  }
  
  public void setHeaderPath(final String headerPath) {
    this.headerPath = headerPath;
  }
  
  public String getFooterPath() {
    return footerPath;
  }
  
  public void setFooterPath(final String footerPath) {
    this.footerPath = footerPath;
  }
  
  protected DynamoHttpServletRequest getCurrentRequest() {
    return ServletUtil.getCurrentRequest();
  }
  
  public boolean isBvEnabled() {
    return bvEnabled;
  }
  
  public void setBvEnabled(final boolean bvEnabled) {
    this.bvEnabled = bvEnabled;
  }
  
  public boolean isBvLoadApi() {
    return bvLoadApi;
  }
  
  public void setBvLoadApi(final boolean bvLoadApi) {
    this.bvLoadApi = bvLoadApi;
  }
  
  public boolean isInternational() {
    return isInternational;
  }
  
  public void setInternational(final boolean isInternational) {
    this.isInternational = isInternational;
  }
  
  /*
   * properties to support client side (javascript) localization of prices
   */
  private boolean convertPrices;
  
  public boolean isConvertPrices() {
    return convertPrices;
  }
  
  public void setConvertPrices(final boolean convertPrices) {
    this.convertPrices = convertPrices;
  }
  
  private double exchangeRate;
  
  public double getExchangeRate() {
    return exchangeRate;
  }
  
  public void setExchangeRate(final double exchangeRate) {
    this.exchangeRate = exchangeRate;
  }
  
  private String currencyCode;
  
  public String getCurrencyCode() {
    return currencyCode;
  }
  
  public void setCurrencyCode(final String currencyCode) {
    this.currencyCode = currencyCode;
  }
  
  private long roundMethod;
  
  public long getRoundMethod() {
    return roundMethod;
  }
  
  public void setRoundMethod(final long roundMethod) {
    this.roundMethod = roundMethod;
  }
  
  private double frontLoadCoefficient;
  
  public double getFrontLoadCoefficient() {
    return frontLoadCoefficient;
  }
  
  public void setFrontLoadCoefficient(final double frontLoadCoefficient) {
    this.frontLoadCoefficient = frontLoadCoefficient;
  }
  
  public boolean isDisplayMobile() {
    return displayMobile;
  }
  
  public void setDisplayMobile(final boolean displayMobile) {
    this.displayMobile = displayMobile;
  }
  
  private boolean showMediaTags;
  
  /**
   * @return the showMediaTags
   */
  public boolean isShowMediaTags() {
    return showMediaTags;
  }
  
  /**
   * @param showMediaTags
   *          the showMediaTags to set
   */
  public void setShowMediaTags(final boolean showMediaTags) {
    this.showMediaTags = showMediaTags;
  }
  
  private String homePromoRootCatDesc = "";
  private List<RepositoryItem> intlHomePromoProducts;
  private List<NMCategory> intlHomePromoCategories;
  
  public String getHomePromoRootCatDesc() {
    return homePromoRootCatDesc;
  }
  
  public void setHomePromoRootCatDesc(final String homePromoRootCatDesc) {
    this.homePromoRootCatDesc = homePromoRootCatDesc;
  }
  
  /**
   * @return the intlHomePromoProducts
   */
  public List<RepositoryItem> getIntlHomePromoProducts() {
    return intlHomePromoProducts;
  }
  
  /**
   * @param intlHomePromoProducts
   *          the intlHomePromoProducts to set
   */
  public void setIntlHomePromoProducts(final List<RepositoryItem> intlHomePromoProducts) {
    this.intlHomePromoProducts = intlHomePromoProducts;
  }
  
  /**
   * @return the intlHomePromoCategories
   */
  public List<NMCategory> getIntlHomePromoCategories() {
    return intlHomePromoCategories;
  }
  
  /**
   * @param intlHomePromoCategories
   *          the intlHomePromoCategories to set
   */
  public void setIntlHomePromoCategories(final List<NMCategory> intlHomePromoCategories) {
    this.intlHomePromoCategories = intlHomePromoCategories;
  }
  
  private boolean emailOPtinCountry;
  private boolean optinUser;
  private boolean cheetahMailUserAPIError;
  private boolean showEmailOptinCheckBox;
  
  /** The logged in previous flag which will capture if user is loggedIn before coming to the current page. */
  private boolean loggedInPreviousPageFlag;
  
  /**
   * @return the loggedInPreviousPageFlag
   */
  public boolean isLoggedInPreviousPageFlag() {
    return loggedInPreviousPageFlag;
  }
  
  /**
   * @param loggedInPreviousPageFlag
   *          the loggedInPreviousPageFlag to set
   */
  public void setLoggedInPreviousPageFlag(final boolean loggedInPreviousPageFlag) {
    this.loggedInPreviousPageFlag = loggedInPreviousPageFlag;
  }
  
  /**
   * @return the emailOPtinCountry
   */
  public boolean isEmailOPtinCountry() {
    return emailOPtinCountry;
  }
  
  /**
   * @param emailOPtinCountry
   *          the emailOPtinCountry to set
   */
  public void setEmailOPtinCountry(final boolean emailOPtinCountry) {
    this.emailOPtinCountry = emailOPtinCountry;
  }
  
  /**
   * @return the cheetahMailUserAPIError
   */
  public boolean isCheetahMailUserAPIError() {
    return cheetahMailUserAPIError;
  }
  
  /**
   * @param cheetahMailUserAPIError
   *          the cheetahMailUserAPIError to set
   */
  public void setCheetahMailUserAPIError(final boolean cheetahMailUserAPIError) {
    this.cheetahMailUserAPIError = cheetahMailUserAPIError;
  }
  
  /**
   * @return the optinUser
   */
  public boolean isOptinUser() {
    return optinUser;
  }
  
  /**
   * @param optinUser
   *          the optinUser to set
   */
  public void setOptinUser(final boolean optinUser) {
    this.optinUser = optinUser;
  }
  
  /**
   * @return the showEmailOptinCheckBox
   */
  public boolean isShowEmailOptinCheckBox() {
    return showEmailOptinCheckBox;
  }
  
  /**
   * @param showEmailOptinCheckBox
   *          the showEmailOptinCheckBox to set
   */
  public void setShowEmailOptinCheckBox(final boolean showEmailOptinCheckBox) {
    this.showEmailOptinCheckBox = showEmailOptinCheckBox;
  }
  
  private boolean expressPayPal;
  private String expPayPalEmailId;
  
  /**
   * @return the expressPayPal
   */
  public boolean isExpressPayPal() {
    return expressPayPal;
  }
  
  /**
   * @param expressPayPal
   *          the expressPayPal to set
   */
  public void setExpressPayPal(final boolean expressPayPal) {
    this.expressPayPal = expressPayPal;
  }
  
  /**
   * @return the expPayPalEmailId
   */
  public String getExpPayPalEmailId() {
    return expPayPalEmailId;
  }
  
  /**
   * @param expPayPalEmailId
   *          the expPayPalEmailId to set
   */
  public void setExpPayPalEmailId(final String expPayPalEmailId) {
    this.expPayPalEmailId = expPayPalEmailId;
  }
  
  public Boolean getIsLastChild() {
    return isLastChild;
  }
  
  public void setIsLastChild(final Boolean isLastChild) {
    this.isLastChild = isLastChild;
  }
  
  public boolean isDisplayPromoSticker() {
    return displayPromoSticker;
  }
  
  public void setDisplayPromoSticker(final boolean displayPromoSticker) {
    this.displayPromoSticker = displayPromoSticker;
  }
  
  /**
   * @return the popupMsg
   */
  public String getPopupMsg() {
    return popupMsg;
  }
  
  /**
   * @param popupMsg
   *          the popupMsg to set
   */
  public void setPopupMsg(final String popupMsg) {
    this.popupMsg = popupMsg;
  }
  
  /**
   * @return the elementDisplaySwitchMap
   */
  public Map<String, Boolean> getElementDisplaySwitchMap() {
    return elementDisplaySwitchMap;
  }
  
  /**
   * @param elementDisplaySwitchMap
   *          the elementDisplaySwitchMap to set
   */
  public void setElementDisplaySwitchMap(final Map<String, Boolean> elementDisplaySwitchMap) {
    this.elementDisplaySwitchMap = elementDisplaySwitchMap;
  }
  
  /**
   * Gets the returns charge message refreshable content.
   * 
   * @return the returns charge message refreshable content
   */
  public String getReturnsChargeMessageRefreshableContent() {
    return returnsChargeMessageRefreshableContent;
  }
  
  /**
   * Sets the returns charge message refreshable content.
   * 
   * @param returnsChargeMessageRefreshableContent
   *          the new returns charge message refreshable content
   */
  public void setReturnsChargeMessageRefreshableContent(final String returnsChargeMessageRefreshableContent) {
    this.returnsChargeMessageRefreshableContent = returnsChargeMessageRefreshableContent;
  }
  
  public String getGoogleMapUrl() {
    return googleMapUrl;
  }
  
  public void setGoogleMapUrl(final String googleMapUrl) {
    this.googleMapUrl = googleMapUrl;
  }
  
  public Breadcrumb[] getBreadcrumbs() {
    return breadcrumbs;
  }
  
  public void setBreadcrumbs(final Breadcrumb[] breadcrumbs) {
    this.breadcrumbs = breadcrumbs;
  }
  
  public Breadcrumb getCurrentBreadcrumb() {
    return currentBreadcrumb;
  }
  
  public void setCurrentBreadcrumb(final Breadcrumb currentBreadcrumb) {
    this.currentBreadcrumb = currentBreadcrumb;
  }
  
/**
 * @return the vendorQuantityErrorMsgList
 */
public List<String> getVendorQuantityErrorMsgList() {
	return vendorQuantityErrorMsgList;
}

/**
   * @param vendorQuantityErrorMsgList
   *          the vendorQuantityErrorMsgList to set
 */
  public void setVendorQuantityErrorMsgList(final List<String> vendorQuantityErrorMsgList) {
	this.vendorQuantityErrorMsgList = vendorQuantityErrorMsgList;
}

/**
 * @return the vendorQuantityOmnitureData
 */
public List<OmnitureData> getVendorQuantityOmnitureData() {
	return vendorQuantityOmnitureData;
}

/**
   * @param vendorQuantityOmnitureData
   *          the vendorQuantityOmnitureData to set
 */
  public void setVendorQuantityOmnitureData(final List<OmnitureData> vendorQuantityOmnitureData) {
	this.vendorQuantityOmnitureData = vendorQuantityOmnitureData;
}

  /**
   * Gets the page name.
   * 
   * @return the page name
   */
  public String getPageName() {
    return pageName;
  }
  
  /**
   * Sets the page name.
   * 
   * @param pageName
   *          the new page name
   */
  public void setPageName(final String pageName) {
    this.pageName = pageName;
  }

  /**
   * Gets the page type.
   * 
   * @return the page type
   */
  public String getPageType() {
    return pageType;
  }
  
  /**
   * Sets the page type.
   * 
   * @param pageType
   *          the new page type
   */
  public void setPageType(final String pageType) {
    this.pageType = pageType;
  }
  
  /**
   * @return the secondaryIdentifier
   */
  public String getSecondaryIdentifier() {
    return secondaryIdentifier;
  }
  
  /**
   * @param secondaryIdentifier
   *          secondaryIdentifier to set
   */
  public void setSecondaryIdentifier(final String secondaryIdentifier) {
    this.secondaryIdentifier = secondaryIdentifier;
  }
  
  /*
   * @return the dataDictionaryUpdateScript
   */
  public String getDataDictionaryUpdateScript() {
    return dataDictionaryUpdateScript;
  }
  
  /**
   * @param dataDictionaryUpdateScript
   *          the dataDictionaryUpdateScript to set
   */
  public void setDataDictionaryUpdateScript(final String dataDictionaryUpdateScript) {
    this.dataDictionaryUpdateScript = dataDictionaryUpdateScript;
  }
  
  /**
   * @return the partiallyEvaluated
   */
  public boolean isPartiallyEvaluated() {
    return partiallyEvaluated;
  }
  
  /**
   * @param partiallyEvaluated
   *          the partiallyEvaluated to set
   */
  public void setPartiallyEvaluated(final boolean partiallyEvaluated) {
    this.partiallyEvaluated = partiallyEvaluated;
  }
  
  /**
   * Checks if is account recently registered.
   * 
   * @return the accountRecentlyRegistered
   */
  public boolean isAccountRecentlyRegistered() {
    return accountRecentlyRegistered;
  }
  
  /**
   * Sets the account recently registered.
   * 
   * @param accountRecentlyRegistered
   *          the accountRecentlyRegistered to set
   */
  public void setAccountRecentlyRegistered(final boolean accountRecentlyRegistered) {
    this.accountRecentlyRegistered = accountRecentlyRegistered;
  }
  
  /**
   * @return the searchTerm
   */
  public String getSearchTerm() {
    return searchTerm;
  }
  
  /**
   * @param searchTerm
   *          the searchTerm to set
   */
  public void setSearchTerm(final String searchTerm) {
    this.searchTerm = searchTerm;
  }
  
  public boolean isShowMIT() {
    return showMIT;
  }
  
  public void setShowMIT(boolean showMIT) {
    this.showMIT = showMIT;
  }
  
  /**
   * @return the toolTipContentMap
   */
  public Map<String, String> getToolTipContentMap() {
    return toolTipContentMap;
  }
  
  /**
   * @param toolTipContentMap the toolTipContentMap to set
   */
  public void setToolTipContentMap(Map<String, String> toolTipContentMap) {
    this.toolTipContentMap = toolTipContentMap;
  }  

  /**
 * @return the jumioEnabled
 */
  public Boolean getJumioEnabled() {
	return jumioEnabled;
  }

  /**
 * @param jumioEnabled the jumioEnabled to set
 */
  public void setJumioEnabled(Boolean jumioEnabled) {
	this.jumioEnabled = jumioEnabled;
  }

  /**
 * @return the jumioToken
 */
  public String getJumioToken() {
	return jumioToken;
  }

  /**
 * @param jumioToken the jumioToken to set
 */
  public void setJumioToken(String jumioToken) {
	this.jumioToken = jumioToken;
  }
  
}
