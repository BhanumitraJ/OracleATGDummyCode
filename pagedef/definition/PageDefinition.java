package com.nm.commerce.pagedef.definition;

import java.lang.reflect.Method;

import atg.nucleus.GenericService;

import com.nm.catalog.navigation.NMCategory;
import com.nm.commerce.pagedef.evaluator.PageEvaluator;

/**
 * Basic definition for a page. Configuration for page layout, header, footer, main content area, etc.
 */
public class PageDefinition extends GenericService {
  // note: Boolean values should be Boolean internally, in getter and setter.
  // note: Integer values should be Integer internally, in getter and setter.
  // This allows for null values, that allow the 'extends' functionality to
  // work.

  private String id;
  private String type;
  private String evaluator; // class that will pre-process the page content

  private String titleType; // defines the browser's title format
  private String bodyClass; // additional classes for body tag
  private String mainPath; // the top-level fragment, with <html> <head>
  // </head> <body> </body> </html>
  private String bodyPath; // the body fragment that goes inside <body>
  // </body>
  private String headerPath; // the header fragment (does not include <head>
  // ... </head>)
  private String internationalHeaderPath; // the header fragment for
  // International shipping
  private String footerPath; // the footer fragment
  private String internationalFooterPath; // the footer fragment for
  // International shipping
  private String contentPath; // the fragment between the header and footer
  private Boolean quickViewGroups;
  private String quickViewPath; // the quickView fragment

  private Boolean useTextCartLink; // Use text verions of shopping cart link
  private String cartLinkText; // Shopping cart link text
  private String activeCartLinkText; // Shopping cart link text - active
  // version
  private String serviceCartLinkText;
  private Boolean hideItemsTextCartLink; // Display text "Items" next to
  // number of items in cart
  private String accountLinkText;
  private String storeLinkText;
  private String storeLocationLinkText;
  private String regBillingText; // Display text in the registration page
  // above add billing info link
  private Boolean showZeroItemCount;
  private Boolean useSiloImgs;
  private Boolean showCartLinkItemCountParentheses;
  private Boolean showItemCountSuffix;
  private String itemCountSuffix;
  private String itemCountSuffixPlural;
  private Integer siloMenuLevels;
  private String[] drawerPromoImageKeys; // image keys to use if drawer promo
  // image available
  private Integer maxDrawerColumns;
  private Integer maxDesignerDrawerColumns;
  private Integer maxDesignerDrawerColumnLength;
  private String designersByAlphaText;
  private String viewAllDesignersText;
  private String cssBundle;
  private String jsBundle;
  private String jsGlobalBundle;
  private Boolean useTransitionalHeaderElements;

  private String cacheKey;

  private String additionalPageBottomScripts;
  private String additionalRefreshableCSS;
  private String useIdForCategory;
  private Boolean enableBloomreach;

  private String bloomreachPageType;
  private Boolean includeReferFriends;
  private String searchBoxFocus;
  private String searchBoxPosition;
  private Boolean isMobilePage;
  private Boolean showModalWindow;
  private Boolean noIndex;
  private Integer metaTagType;
  private String restPagingPath; // WB_BETA_01 - defines about us page
  private String storeLocationPagingPath; // WB_BETA_01 - defines store
  // location
  private String restaurantText;
  private String titleSubType;
  // Select Responsive Equivalent Page Definition based on the group for AB
  // test
  private String responsiveEquivalent;
  private String elimsEquivalent;

  private String freeBlitzRefreshable;
  private Boolean displayShippingFree;
  private String breadcrumbUrl;
  private String breadcrumbTitle;
  private String defaultWishlistId;
  private Boolean breadcrumbSubpage;
  private Boolean populatePageDefDrivenDataDictionary;
  
  private Boolean noResultsCarouselEnabled;
  private String noResultsCopyPrefix;

  // Google Ads
  private String googleAdsRefreshablePath;
  private String googleAdsFooterPath;
  
  //Enables loading javascript asynchronously
  private boolean enableAsyncAdapter = false;
  
  public String getDefaultWishlistId() {
    return getValue(defaultWishlistId, basis, "getDefaultWishlistId");
  }

  public void setDefaultWishlistId(final String defaultWishlistId) {
    this.defaultWishlistId = defaultWishlistId;
  }

  public Boolean getDisplayShippingFree() {
    return getValue(displayShippingFree, basis, "getDisplayShippingFree");
  }

  public void setDisplayShippingFree(final Boolean displayShippingFree) {
    this.displayShippingFree = displayShippingFree;
  }

  public String getFreeBlitzRefreshable() {
    return getValue(freeBlitzRefreshable, basis, "getFreeBlitzRefreshable");
  }

  public void setFreeBlitzRefreshable(final String freeBlitzRefreshable) {
    this.freeBlitzRefreshable = freeBlitzRefreshable;
  }

  // true if the page supports responsive design (changing layouts scaling for
  // different screen sizes)
  private Boolean responsive;
  private Boolean showAllMainPromos;

  private Boolean trackingNumInOpenHistoryItem;


  // non extends
  private Boolean requiresLogin = Boolean.FALSE;
  private Boolean enableCaching = Boolean.FALSE;

  private String pagingSeparator;

  // PromoBanner Slots
  private Boolean checkPromoBannerSlot;

  protected PageDefinition basis;

  // this instance is stored to prevent repeated lookups
  private PageEvaluator evaluatorInstance;

  private Boolean suppressMobilePhoneParsing = true;

  private String giftCardText;

  private Boolean inStoreAvailability;

  // used to include css for IE7 and IE8
  private Boolean includeLegacyCSS;
  private String legacyCSSPath;
  private String additionalCSSBundle;
  private String legacyAdditionalCSSBundle;

  // used to include css for IE9
  private String ie9BaseCSSBundle;
  private String ie9TemplateCSSBundle;

  // New look & feel to monogramming pages
  private String monogrammingStyle;
  private Boolean fixedBackToTop;
  
  private Boolean explicitWidthDrawerPromo;

  // property for determining whether to show the promoSticker on the
  // thumbnail page or not.
  private Boolean promoStickerEnabled;

  private Boolean ignoreMetaTagOverride;

  private String pageType;
  
  private String templateType;
  private Boolean showPinIt;
  private Boolean isLastChild = false;
  private String gotLocationUrl;
  private Integer maxSaleCategories;

  private String vgNotification;
  
  private Boolean displayCatName;
  /** property to hold type ahead used */
  private String usedTypeAhead;

  // Display Flag text
  /** property to hold preTextFlags */
  private String[] preTextFlags;
  /** property to hold postTextFlags */
  private String[] postTextFlags;
  /** property to hold flagDirectoryKey */
  private String flagDirectoryKey;
  private String jumioBodyCall;

  public PageDefinition() {}

  // convenience methods for getter using extends basis

  protected String getValue(final String local, final PageDefinition basis, final String methodName) {
    return (String) getObjectValue(local, basis, methodName);
  }

  protected Boolean getValue(final Boolean local, final PageDefinition basis, final String methodName) {
    final Boolean object = (Boolean) getObjectValue(local, basis, methodName);
    return object == null ? false : object.booleanValue();
  }

  protected Integer getValue(final Integer local, final PageDefinition basis, final String methodName) {
    final Integer object = (Integer) getObjectValue(local, basis, methodName);
    return object == null ? 0 : object;
  }

  protected String[] getValue(final String[] local, final PageDefinition basis, final String methodName) {
    return (String[]) getObjectValue(local, basis, methodName);
  }

  // returns the local value, if not null, then calls the get method on the
  // extends basis.
  // if the value is retrieved from the basis, it is stored locally for faster
  // lookups.
  protected Object getObjectValue(final Object local, final PageDefinition basis, final String methodName) {
    if (local != null) {
      return local;
    }

    Object result = null;
    Class<?> resultClass = null;

    if (basis != null) {
      try {
        final Method getMethod = basis.getClass().getMethod(methodName, (Class[]) null);
        if (getMethod != null) {
          result = getMethod.invoke(basis);
          resultClass = getMethod.getReturnType();
        }
      } catch (final NoSuchMethodException e) {
        // valid case: basis does not have this property
      } catch (final Exception e) {
        logError("Unable to resolve get method: " + methodName, e);
      }

      if (result != null) {
        // set the value in this definition, to avoid future lookups

        try {
          String setMethodName = methodName.replaceFirst("^get", "set");
          setMethodName = setMethodName.replaceFirst("^is", "set");
          final Class<?>[] args = {resultClass};
          final Method setMethod = getClass().getMethod(setMethodName, args);
          setMethod.invoke(this, result);
        } catch (final Exception e) {
          logError("Unable to resolve set method: " + methodName, e);
        }
      }
    }

    if (result == null) {
      // it is an error to not have a default value.
      // it will cause reflection to be performed every time,
      // which is a bad performance impact.
      // default to "", 0, false, etc. if no value is desired.
      if (isLoggingDebug()) {
        logDebug("No value found for for " + id + ": " + methodName + ".  Set a " + "default value to avoid performance impact due to reflection.");
      }
    }

    return result;
  }

  // use instance attribute if previously resolved
  public PageEvaluator getEvaluatorInstance() throws Exception {
    if (evaluatorInstance == null) {
      final Class<?> aClass = Class.forName(getEvaluator());
      evaluatorInstance = (PageEvaluator) aClass.newInstance();
    }
    return evaluatorInstance;
  }

  // getters and setters

  public PageDefinition getExtends() {
    return basis;
  }

  public void setExtends(final PageDefinition extendsDefinition) {
    this.basis = extendsDefinition;
  }

  public String getInternationalHeaderPath() {
    return getValue(internationalHeaderPath, basis, "getInternationalHeaderPath");
  }

  public void setInternationalHeaderPath(final String internationalHeaderPath) {
    this.internationalHeaderPath = internationalHeaderPath;
  }

  public String getInternationalFooterPath() {
    return getValue(internationalFooterPath, basis, "getInternationalFooterPath");
  }

  public void setInternationalFooterPath(final String internationalFooterPath) {
    this.internationalFooterPath = internationalFooterPath;
  }

  public String getEvaluator() {
    return getValue(evaluator, basis, "getEvaluator");
  }

  public void setEvaluator(final String evaluator) {
    this.evaluator = evaluator;
  }

  public String getMainPath() {
    return getValue(mainPath, basis, "getMainPath");
  }

  public void setMainPath(final String mainPath) {
    this.mainPath = mainPath;
  }

  public String getBodyPath() {
    return getValue(bodyPath, basis, "getBodyPath");
  }

  public void setTitleType(final String titleType) {
    this.titleType = titleType;
  }

  public String getTitleType() {
    return getValue(titleType, basis, "getTitleType");
  }

  public void setBodyPath(final String bodyPath) {
    this.bodyPath = bodyPath;
  }

  public String getBodyClass() {
    return getValue(bodyClass, basis, "getBodyClass");
  }

  public void setBodyClass(final String bodyClass) {
    this.bodyClass = bodyClass;
  }

  public String getHeaderPath() {
    return getValue(headerPath, basis, "getHeaderPath");
  }

  public void setHeaderPath(final String headerPath) {
    this.headerPath = headerPath;
  }

  public String getFooterPath() {
    return getValue(footerPath, basis, "getFooterPath");
  }

  public void setFooterPath(final String footerPath) {

    this.footerPath = footerPath;
  }

  public String getContentPath() {

    return getValue(contentPath, basis, "getContentPath");
  }

  public void setContentPath(final String contentPath) {
    this.contentPath = contentPath;
  }

  public String getQuickViewPath() {
    return getValue(quickViewPath, basis, "getQuickViewPath");
  }

  public void setQuickViewPath(final String quickViewPath) {
    this.quickViewPath = quickViewPath;
  }

  public Boolean getQuickViewGroups() {
    return getValue(quickViewGroups, basis, "getQuickViewGroups");
  }

  public void setQuickViewGroups(Boolean quickViewGroups) {
    this.quickViewGroups = quickViewGroups;
  }

  public Boolean getUseTextCartLink() {
    return getValue(useTextCartLink, basis, "getUseTextCartLink");
  }

  public void setUseTextCartLink(final Boolean useTextCartLink) {
    this.useTextCartLink = useTextCartLink;
  }

  public String getCartLinkText() {
    return getValue(cartLinkText, basis, "getCartLinkText");
  }

  public void setCartLinkText(final String cartLinkText) {
    this.cartLinkText = cartLinkText;
  }

  public String getActiveCartLinkText() {
    return getValue(activeCartLinkText, basis, "getActiveCartLinkText");
  }

  public void setActiveCartLinkText(final String activeCartLinkText) {
    this.activeCartLinkText = activeCartLinkText;
  }

  public String getServiceCartLinkText() {
    return getValue(serviceCartLinkText, basis, "getServiceCartLinkText");
  }

  public void setServiceCartLinkText(final String serviceCartLinkText) {
    this.serviceCartLinkText = serviceCartLinkText;
  }

  public Boolean getHideItemsTextCartLink() {
    return getValue(hideItemsTextCartLink, basis, "getHideItemsTextCartLink");
  }

  public void setHideItemsTextCartLink(final Boolean hideItemsTextCartLink) {
    this.hideItemsTextCartLink = hideItemsTextCartLink;
  }

  public String[] getDrawerPromoImageKeys() {
    return getValue(drawerPromoImageKeys, basis, "getDrawerPromoImageKeys");
  }

  public void setDrawerPromoImageKeys(final String[] drawerPromoImageKeys) {
    this.drawerPromoImageKeys = drawerPromoImageKeys;
  }

  public String getCssBundle() {
    return getValue(cssBundle, basis, "getCssBundle");
  }

  public void setCssBundle(final String cssBundle) {
    this.cssBundle = cssBundle;
  }

  public String getJsBundle() {
    return getValue(jsBundle, basis, "getJsBundle");
  }

  public void setJsBundle(final String jsBundle) {
    this.jsBundle = jsBundle;
  }

  public String getJsGlobalBundle() {
    return getValue(jsGlobalBundle, basis, "getJsGlobalBundle");
  }

  public void setJsGlobalBundle(final String jsGlobalBundle) {
    this.jsGlobalBundle = jsGlobalBundle;
  }

  public String getId() {
    return getValue(id, basis, "getId");
  }

  public void setId(final String id) {
    this.id = id;
  }

  public String getType() {
    return getValue(type, basis, "getType");
  }

  public void setType(final String type) {
    this.type = type;
  }


  public Boolean getEnableCaching() {
    return getValue(enableCaching, basis, "getEnableCaching");
  }

  public void setEnableCaching(final Boolean value) {
    enableCaching = value;
  }

  public Boolean getRequiresLogin() {
    return getValue(requiresLogin, basis, "getRequiresLogin");
  }

  public void setRequiresLogin(final Boolean value) {
    requiresLogin = value;
  }

  public String getCacheKey() {
    return getValue(cacheKey, basis, "getCacheKey");
  }

  public void setCacheKey(final String value) {
    cacheKey = value;
  }

  public String getAdditionalPageBottomScripts() {
    return getValue(additionalPageBottomScripts, basis, "getAdditionalPageBottomScripts");
  }

  public void setAdditionalPageBottomScripts(final String value) {
    additionalPageBottomScripts = value;
  }

  public String getAdditionalRefreshableCSS() {
    return getValue(additionalRefreshableCSS, basis, "getAdditionalRefreshableCSS");
  }

  public void setAdditionalRefreshableCSS(final String value) {
    additionalRefreshableCSS = value;
  }

  public String getUseIdForCategory() {
    return getValue(useIdForCategory, basis, "getUseIdForCategory");
  }

  public void setUseIdForCategory(final String value) {
    this.useIdForCategory = value;
  }

  public Boolean getShowModalWindow() {
    return getValue(showModalWindow, basis, "getShowModalWindow");
  }

  public void setShowModalWindow(final Boolean showModalWindow) {
    this.showModalWindow = showModalWindow;
  }

  public void setEnableBloomreach(final Boolean value) {
    this.enableBloomreach = value;
  }

  public Boolean getEnableBloomreach() {
    return getValue(enableBloomreach, basis, "getEnableBloomreach");
  }


  public String getBloomreachPageType() {
    return getValue(bloomreachPageType, basis, "getBloomreachPageType");
  }

  public void setBloomreachPageType(final String bloomreachPageType) {
    this.bloomreachPageType = bloomreachPageType;
  }


  public Boolean getCheckPromoBannerSlot() {
    return getValue(checkPromoBannerSlot, basis, "getCheckPromoBannerSlot");
  }

  public void setCheckPromoBannerSlot(final Boolean checkPromoBannerSlot) {
    this.checkPromoBannerSlot = checkPromoBannerSlot;
  }

  public Boolean getIncludeReferFriends() {
    return includeReferFriends;
  }

  public void setIncludeReferFriends(final Boolean includeReferFriends) {
    this.includeReferFriends = includeReferFriends;
  }

  public Boolean getTrackingNumInOpenHistoryItem() {
    return trackingNumInOpenHistoryItem;
  }

  public void setTrackingNumInOpenHistoryItem(final Boolean trackingNumInOpenHistoryItem) {
    this.trackingNumInOpenHistoryItem = trackingNumInOpenHistoryItem;
  }

  public String getPagingSeparator() {
    return getValue(pagingSeparator, basis, "getPagingSeparator");
  }

  public void setPagingSeparator(final String pagingSeparator) {
    this.pagingSeparator = pagingSeparator;
  }

  public String getSearchBoxFocus() {
    return getValue(searchBoxFocus, basis, "getSearchBoxFocus");
  }

  public void setSearchBoxFocus(final String searchBoxFocus) {
    this.searchBoxFocus = searchBoxFocus;
  }

  public String getSearchBoxPosition() {
    return getValue(searchBoxPosition, basis, "getSearchBoxPosition");
  }

  public void setSearchBoxPosition(final String searchBoxPosition) {
    this.searchBoxPosition = searchBoxPosition;
  }

  public void setMaxDrawerColumns(final Integer maxDrawerColumns) {
    this.maxDrawerColumns = maxDrawerColumns;
  }

  public Integer getMaxDrawerColumns() {
    return getValue(maxDrawerColumns, basis, "getMaxDrawerColumns");
  }

  public Integer getMaxDesignerDrawerColumns() {
    return getValue(maxDesignerDrawerColumns, basis, "getMaxDesignerDrawerColumns");
  }

  public void setMaxDesignerDrawerColumns(final Integer maxDesignerDrawerColumns) {
    this.maxDesignerDrawerColumns = maxDesignerDrawerColumns;
  }

  public Integer getMaxDesignerDrawerColumnLength() {
    return getValue(maxDesignerDrawerColumnLength, basis, "getMaxDesignerDrawerColumnLength");
  }

  public void setMaxDesignerDrawerColumnLength(final Integer maxDesignerDrawerColumnLength) {
    this.maxDesignerDrawerColumnLength = maxDesignerDrawerColumnLength;
  }

  public Boolean getIsMobilePage() {
    return getValue(isMobilePage, basis, "getIsMobilePage");
  }

  public void setIsMobilePage(final Boolean isMobilePage) {
    this.isMobilePage = isMobilePage;
  }

  // subclasses may switch definition based on category or a/b test
  public PageDefinition getAlternateDefinition(final NMCategory category) {
    return this;
  }

  public Boolean getSuppressMobilePhoneParsing() {
    return suppressMobilePhoneParsing;
  }

  public void setSuppressMobilePhoneParsing(final Boolean suppressMobilePhoneParsing) {
    this.suppressMobilePhoneParsing = suppressMobilePhoneParsing;
  }

  public String getGiftCardText() {
    return giftCardText;
  }

  public void setGiftCardText(final String giftCardText) {
    this.giftCardText = giftCardText;
  }

  public Boolean getNoIndex() {
    return noIndex;
  }

  public void setNoIndex(final Boolean noIndex) {
    this.noIndex = noIndex;
  }

  public Boolean getShowZeroItemCount() {
    return getValue(showZeroItemCount, basis, "getShowZeroItemCount");
  }

  public void setShowZeroItemCount(final Boolean showZeroItemCount) {
    this.showZeroItemCount = showZeroItemCount;
  }

  public Boolean getUseTransitionalHeaderElements() {
    return getValue(useTransitionalHeaderElements, basis, "getUseTransitionalHeaderElements");
  }

  public void setUseTransitionalHeaderElements(final Boolean useTransitionalHeaderElements) {
    this.useTransitionalHeaderElements = useTransitionalHeaderElements;
  }

  public Boolean getUseSiloImgs() {
    return getValue(useSiloImgs, basis, "getUseSiloImgs");
  }

  public void setUseSiloImgs(final Boolean useSiloImgs) {
    this.useSiloImgs = useSiloImgs;
  }

  public Boolean getShowCartLinkItemCountParentheses() {
    return getValue(showCartLinkItemCountParentheses, basis, "getShowCartLinkItemCountParentheses");
  }

  public void setShowCartLinkItemCountParentheses(final Boolean showCartLinkItemCountParentheses) {
    this.showCartLinkItemCountParentheses = showCartLinkItemCountParentheses;
  }

  public Boolean getShowItemCountSuffix() {
    return getValue(showItemCountSuffix, basis, "getShowItemCountSuffix");
  }

  public void setShowItemCountSuffix(final Boolean showItemCountSuffix) {
    this.showItemCountSuffix = showItemCountSuffix;
  }

  public String getItemCountSuffix() {
    return getValue(itemCountSuffix, basis, "getItemCountSuffix");
  }

  public void setItemCountSuffix(final String itemCountSuffix) {
    this.itemCountSuffix = itemCountSuffix;
  }

  public String getItemCountSuffixPlural() {
    return getValue(itemCountSuffixPlural, basis, "getItemCountSuffixPlural");
  }

  public void setItemCountSuffixPlural(final String itemCountSuffixPlural) {
    this.itemCountSuffixPlural = itemCountSuffixPlural;
  }

  public void setSiloMenuLevels(final Integer siloMenuLevels) {
    this.siloMenuLevels = siloMenuLevels;
  }

  public Integer getSiloMenuLevels() {
    return getValue(siloMenuLevels, basis, "getSiloMenuLevels");
  }

  public String getAccountLinkText() {
    return getValue(accountLinkText, basis, "getAccountLinkText");
  }

  public void setAccountLinkText(final String accountLinkText) {
    this.accountLinkText = accountLinkText;
  }

  public String getStoreLinkText() {
    return getValue(storeLinkText, basis, "getStoreLinkText");
  }

  public void setStoreLinkText(final String storeLinkText) {
    this.storeLinkText = storeLinkText;
  }

  public String getRegBillingText() {
    return regBillingText;
  }

  public void setRegBillingText(final String regBillingText) {
    this.regBillingText = regBillingText;
  }

  public void setMetaTagType(final Integer metaTagType) {
    this.metaTagType = metaTagType;
  }

  public Integer getMetaTagType() {
    return getValue(metaTagType, basis, "getMetaTagType");
  }

  public String getRestPagingPath() {
    return restPagingPath;
  }

  public void setRestPagingPath(final String restPagingPath) {
    this.restPagingPath = restPagingPath;
  }

  public String getStoreLocationPagingPath() {
    return storeLocationPagingPath;
  }

  public void setStoreLocationPagingPath(final String storeLocationPagingPath) {
    this.storeLocationPagingPath = storeLocationPagingPath;
  }

  public String getRestaurantText() {
    return getValue(restaurantText, basis, "getRestaurantText");
  }

  public void setRestaurantText(final String restaurantText) {
    this.restaurantText = restaurantText;
  }

  public String getStoreLocationLinkText() {
    return getValue(storeLocationLinkText, basis, "getStoreLocationLinkText");
  }

  public void setStoreLocationLinkText(final String storeLocationLinkText) {
    this.storeLocationLinkText = storeLocationLinkText;
  }

  public String getDesignersByAlphaText() {
    return getValue(designersByAlphaText, basis, "getDesignersByAlphaText");
  }

  public void setDesignersByAlphaText(final String designersByAlphaText) {
    this.designersByAlphaText = designersByAlphaText;
  }

  public String getViewAllDesignersText() {
    return getValue(viewAllDesignersText, basis, "getViewAllDesignersText");
  }

  public void setViewAllDesignersText(final String viewAllDesignersText) {
    this.viewAllDesignersText = viewAllDesignersText;
  }

  public Boolean getInStoreAvailability() {
    return getValue(inStoreAvailability, basis, "getInStoreAvailability");
  }

  public void setInStoreAvailability(final Boolean inStoreAvailability) {
    this.inStoreAvailability = inStoreAvailability;
  }

  public Boolean getResponsive() {
    return getValue(responsive, basis, "getResponsive");
  }

  public void setResponsive(final Boolean responsive) {
    this.responsive = responsive;
  }

  public Boolean getShowAllMainPromos() {
    return getValue(showAllMainPromos, basis, "getShowAllMainPromos");
  }

  public void setShowAllMainPromos(final Boolean showAllMainPromos) {
    this.showAllMainPromos = showAllMainPromos;
  }

  public Boolean getIncludeLegacyCSS() {
    return getValue(includeLegacyCSS, basis, "getIncludeLegacyCSS");
  }

  public void setIncludeLegacyCSS(final Boolean value) {
    includeLegacyCSS = value;
  }

  public String getLegacyCSSPath() {
    return getValue(legacyCSSPath, basis, "getLegacyCSSPath");
  }

  public void setLegacyCSSPath(final String legacyCSSPath) {
    this.legacyCSSPath = legacyCSSPath;
  }

  public Boolean getFixedBackToTop() {
    return getValue(fixedBackToTop, basis, "getFixedBackToTop");
  }

  public void setFixedBackToTop(final Boolean fixedBackToTop) {
    this.fixedBackToTop = fixedBackToTop;
  }

  public String getMonogrammingStyle() {
    return getValue(monogrammingStyle, basis, "getMonogrammingStyle");
  }

  public void setMonogrammingStyle(final String monogrammingStyle) {
    this.monogrammingStyle = monogrammingStyle;
  }

  public Boolean getIgnoreMetaTagOverride() {
    return getValue(ignoreMetaTagOverride, basis, "getIgnoreMetaTagOverride");
  }

  public void setIgnoreMetaTagOverride(final Boolean ignoreMetaTagOverride) {
    this.ignoreMetaTagOverride = ignoreMetaTagOverride;
  }

  public boolean getExplicitWidthDrawerPromo() {
    return getValue(explicitWidthDrawerPromo, basis, "getExplicitWidthDrawerPromo");
  }

  public void setExplicitWidthDrawerPromo(final boolean explicitWidthDrawerPromo) {
    this.explicitWidthDrawerPromo = explicitWidthDrawerPromo;
  }

  public Boolean getPromoStickerEnabled() {
    return getValue(promoStickerEnabled, basis, "getPromoStickerEnabled");
  }

  public void setPromoStickerEnabled(final Boolean promoStickerEnabled) {
    this.promoStickerEnabled = promoStickerEnabled;
  }

  public String getPageType() {
    return getValue(pageType, basis, "getPageType");
  }

  public void setPageType(final String pageType) {
    this.pageType = pageType;
  }

  public String getTitleSubType() {
    return titleSubType;
  }

  public void setTitleSubType(final String titleSubType) {
    this.titleSubType = titleSubType;
  }

  public String getTemplateType() {
    return getValue(templateType, basis, "getTemplateType");
  }

  public void setTemplateType(final String templateType) {
    this.templateType = templateType;
  }

  public String getResponsiveEquivalent() {
    return responsiveEquivalent;
  }

  public void setResponsiveEquivalent(final String responsiveEquivalent) {
    this.responsiveEquivalent = responsiveEquivalent;
  }

  public Boolean getIsLastChild() {
    return isLastChild;
  }

  public void setIsLastChild(final Boolean isLastChild) {
    this.isLastChild = isLastChild;
  }

  public Boolean getShowPinIt() {
    return showPinIt;
  }

  public void setShowPinIt(final Boolean showPinIt) {
    this.showPinIt = showPinIt;
  }

  public String getAdditionalCSSBundle() {
    return getValue(additionalCSSBundle, basis, "getAdditionalCSSBundle");
  }

  public void setAdditionalCSSBundle(final String additionalCSSBundle) {
    this.additionalCSSBundle = additionalCSSBundle;
  }

  public String getLegacyAdditionalCSSBundle() {
    return getValue(legacyAdditionalCSSBundle, basis, "getLegacyAdditionalCSSBundle");
  }

  public void setLegacyAdditionalCSSBundle(final String legacyAdditionalCSSBundle) {
    this.legacyAdditionalCSSBundle = legacyAdditionalCSSBundle;
  }

  public String getIe9BaseCSSBundle() {
    return getValue(ie9BaseCSSBundle, basis, "getIe9BaseCSSBundle");
  }

  public void setIe9BaseCSSBundle(final String ie9BaseCSSBundle) {
    this.ie9BaseCSSBundle = ie9BaseCSSBundle;
  }

  public String getIe9TemplateCSSBundle() {
    return getValue(ie9TemplateCSSBundle, basis, "getIe9TemplateCSSBundle");
  }

  public void setIe9TemplateCSSBundle(final String ie9TemplateCSSBundle) {
    this.ie9TemplateCSSBundle = ie9TemplateCSSBundle;
  }

  public String getGotLocationUrl() {
    return getValue(gotLocationUrl, basis, "getGotLocationUrl");
  }

  public void setGotLocationUrl(final String gotLocationUrl) {
    this.gotLocationUrl = gotLocationUrl;
  }

  public String getvgNotification() {
    return getValue(vgNotification, basis, "vgNotification");
  }
  
  public void setvgNotification(final String vgNotification) {
    this.vgNotification = vgNotification;
  }
  
  public String getBreadcrumbUrl() {
    return getValue(breadcrumbUrl, basis, "getBreadcrumbUrl");
  }

  public void setBreadcrumbUrl(final String breadcrumbUrl) {
    this.breadcrumbUrl = breadcrumbUrl;
  }

  public String getBreadcrumbTitle() {
    return getValue(breadcrumbTitle, basis, "getBreadcrumbTitle");
  }

  public void setBreadcrumbTitle(final String breadcrumbTitle) {
    this.breadcrumbTitle = breadcrumbTitle;
  }

  public Boolean getBreadcrumbSubpage() {
    return getValue(breadcrumbSubpage, basis, "getBreadcrumbSubpage");
  }

  public void setBreadcrumbSubpage(final Boolean breadcrumbSubpage) {
    this.breadcrumbSubpage = breadcrumbSubpage;
  }

  public Boolean getNoResultsCarouselEnabled() {
    return getValue(noResultsCarouselEnabled, basis, "getNoResultsCarouselEnabled");
  }

  public void setNoResultsCarouselEnabled(final Boolean noResultsCarouselEnabled) {
    this.noResultsCarouselEnabled = noResultsCarouselEnabled;
  }

  public String getNoResultsCopyPrefix() {
    return getValue(noResultsCopyPrefix, basis, "getNoResultsCopyPrefix");
  }

  public void setNoResultsCopyPrefix(final String noResultsCopyPrefix) {
    this.noResultsCopyPrefix = noResultsCopyPrefix;
  }

  public String getGoogleAdsRefreshablePath() {
    return getValue(googleAdsRefreshablePath, basis, "getGoogleAdsRefreshablePath");
  }

  public void setGoogleAdsRefreshablePath(final String googleAdsRefreshablePath) {
    this.googleAdsRefreshablePath = googleAdsRefreshablePath;
  }

  public String getGoogleAdsFooterPath() {
    return getValue(googleAdsFooterPath, basis, "getGoogleAdsFooterPath");
  }

  public void setGoogleAdsFooterPath(final String googleAdsFooterPath) {
    this.googleAdsFooterPath = googleAdsFooterPath;
  }

  public Integer getMaxSaleCategories() {
    return getValue(maxSaleCategories, basis, "getMaxSaleCategories");
  }

  public void setMaxSaleCategories(final Integer maxSaleCategories) {
    this.maxSaleCategories = maxSaleCategories;
  }

/**
 * @return the displayCatName
 */
public Boolean getDisplayCatName() {
    return getValue(displayCatName, basis, "getDisplayCatName");
}

/**
   * @param displayCatName
   *          the displayCatName to set
 */
  public void setDisplayCatName(final Boolean displayCatName) {
	this.displayCatName = displayCatName;
}

/**
 * @return the usedTypeAhead
 */
public String getUsedTypeAhead() {
  return usedTypeAhead;
}

/**
   * @param usedTypeAhead
   *          the usedTypeAhead to set
 */
  public void setUsedTypeAhead(final String usedTypeAhead) {
  this.usedTypeAhead = usedTypeAhead;
}

/**
   * @return the preTextFlags
   */
  public String[] getPreTextFlags() {
    return preTextFlags;
  }
  
  /**
   * @param preTextFlags
   *          the preTextFlags to set
   */
  public void setPreTextFlags(final String[] preTextFlags) {
    this.preTextFlags = preTextFlags;
  }
  
  /**
   * @return the postTextFlags
   */
  public String[] getPostTextFlags() {
    return postTextFlags;
  }
  
  /**
   * @param postTextFlags
   *          the postTextFlags to set
   */
  public void setPostTextFlags(final String[] postTextFlags) {
    this.postTextFlags = postTextFlags;
  }
  
  /**
   * @return the flagDirectoryKey
   */
  public String getFlagDirectoryKey() {
    return flagDirectoryKey;
  }
  
  /**
   * @param flagDirectoryKey
   *          the flagDirectoryKey to set
   */
  public void setFlagDirectoryKey(final String flagDirectoryKey) {
    this.flagDirectoryKey = flagDirectoryKey;
  }
  
 /** @return the populateDataDictionaryForCurrentPage
   * @return the populatePageDefDrivenDataDictionary
 */
  public Boolean getPopulatePageDefDrivenDataDictionary() {
    return populatePageDefDrivenDataDictionary;
}

/**
   * @param populatePageDefDrivenDataDictionary
   *          the populatePageDefDrivenDataDictionary to set
 */
  public void setPopulatePageDefDrivenDataDictionary(final Boolean populatePageDefDrivenDataDictionary) {
    this.populatePageDefDrivenDataDictionary = populatePageDefDrivenDataDictionary;
}
  
  public String getElimsEquivalent() {
    return elimsEquivalent;
  }
  
  public void setElimsEquivalent(String elimsEquivalent) {
    this.elimsEquivalent = elimsEquivalent;
  }
 
/**
 *  As part of the page speed insight project, script should be allowed to load with ASYNC attribute so that 
	page is rendering is not blocked. Async attribute is set on the script tag library called nmscript.
	If property called enableAsyncAdapter overrides the inline attribute value. If this property in the properties
	file is set to false, even if in the JSP attribute has been set to "async", will load the script async.
 * @param isEnabled
 */
  public void setEnableAsyncAdapter(boolean isEnabled) {
		this.enableAsyncAdapter = isEnabled;
  }
  
  public boolean getEnableAsyncAdapter() {
	  return getValue(this.enableAsyncAdapter, basis, "getEnableAsyncAdapter");
  }

  /**
 * @return the jumioBodyCall
 */
  public String getJumioBodyCall() {
	return jumioBodyCall;
  }

  /**
 * @param jumioBodyCall the jumioBodyCall to set
 */
  public void setJumioBodyCall(String jumioBodyCall) {
	this.jumioBodyCall = jumioBodyCall;
  }
}
