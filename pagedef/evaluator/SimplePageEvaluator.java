package com.nm.commerce.pagedef.evaluator;

import static com.nm.common.ProfileRepositoryConstants.FILTER_PERSISTENCE;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.servlet.ServletException;
import javax.servlet.jsp.PageContext;

import org.apache.commons.lang.ArrayUtils;

import atg.core.util.StringUtils;
import atg.naming.NameContext;
import atg.nucleus.Nucleus;
import atg.nucleus.logging.ApplicationLogging;
import atg.nucleus.logging.ClassLoggingFactory;
import atg.repository.RepositoryItem;
import atg.servlet.DynamoHttpServletRequest;
import atg.servlet.DynamoHttpServletResponse;
import atg.servlet.ServletUtil;
import atg.userprofiling.Profile;

import com.nm.abtest.AbTestHelper;
import com.nm.catalog.history.NMCatalogHistory;
import com.nm.catalog.history.NMCatalogHistoryCollector;
import com.nm.catalog.navigation.CategoryHelper;
import com.nm.catalog.navigation.NMCategory;
import com.nm.collections.NMPromotion;
import com.nm.commerce.NMProfile;
import com.nm.commerce.beans.BaseResultBeanContainer;
import com.nm.commerce.checkout.CheckoutComponents;
import com.nm.commerce.pagedef.definition.PageDefinition;
import com.nm.commerce.pagedef.definition.TemplatePageDefinition;
import com.nm.commerce.pagedef.model.EndecaDrivenPageModel;
import com.nm.commerce.pagedef.model.PageModel;
import com.nm.commerce.pagedef.model.UIElementConstants;
import com.nm.commerce.promotion.NMOrderImpl;
import com.nm.common.INMGenericConstants;
import com.nm.components.CommonComponentHelper;
import com.nm.droplet.EnablePromotionsPreview;
import com.nm.droplet.FilterCategoryGroup;
import com.nm.droplet.RobotDetectDroplet;
import com.nm.droplet.bloomreach.BloomreachSuggest;
import com.nm.edo.constants.EmployeeDiscountConstants;
import com.nm.formhandler.ShoppingCartHandler;
import com.nm.integration.ShopRunnerConstants;
import com.nm.servlet.NMRequestHelper;
import com.nm.servlet.filter.AbTestFilter;
import com.nm.sitepersonalization.SitePersonalizationConfig;
import com.nm.tms.constants.TMSDataDictionaryConstants;
import com.nm.tms.core.TMSMessageContainer;
import com.nm.tms.utils.TMSDataDictionaryUtils;
import com.nm.utils.BrandSpecs;
import com.nm.utils.CategoryFilterCode;
import com.nm.utils.LocalizationUtils;
import com.nm.utils.NMAbstractCallable;
import com.nm.utils.NMConfigurations;
import com.nm.utils.NmoUtils;
import com.nm.utils.ProdSkuUtil;
import com.nm.utils.PromotionsHelper;
import com.nm.utils.StringUtilities;
import com.nm.utils.crypto.CryptoLinkedEmail;
import com.nm.utils.fiftyone.FiftyOneUtils;

/**
 * Updates variables and sets up any redirects before page execution. Returns true if page content should be output.
 */
public class SimplePageEvaluator implements PageEvaluator {
  
  public static final String COMPAREMETRICS_ABTESTGROUP = "compareMetricsAbTest";
  public static final String FREE_BLITZ_ABTESTGROUP = "freeBlitzABTest";
  public static final String FREE_BLITZ_SHIPPING_ABTESTGROUP = "freeBlitzShippingABTest";
  public static final String ALIPAY_ABTEST_GROUP = "alipayAbTestFlag";
  public static final String GROUP_A = "A";
  public static final String GROUP_B = "B";
  public static final String SEARCH_GROUP = "SearchGroup";
  public static final String UNSUPPORTEDBROWSERS = "unsupportedBrowsers";
  public static final String SALE_SILO_DRAWER_ABTEST = "saleSiloDrawer";
  public static final String SEARCH = "search";
  public final Integer maxResults = 99999;
  private String cookieName;
  
  /**
   * @return the cookieName
   */
  public String getCookieName() {
    return cookieName;
  }
  
  /**
   * @param cookieName
   *          the cookieName to set
   */
  public void setCookieName(String cookieName) {
    this.cookieName = cookieName;
  }
  
  protected static final ApplicationLogging log = ClassLoggingFactory.getFactory().getLoggerForClass(SimplePageEvaluator.class);
  
  @Override
  @SuppressWarnings({"unused" , "unchecked"})
  public boolean evaluate(final PageDefinition pageDefinition, final PageContext pageContext) throws Exception {
    
    final LocalizationUtils utils = (LocalizationUtils) Nucleus.getGlobalNucleus().resolveName("/nm/utils/LocalizationUtils");
    if (!NmoUtils.isEmpty(pageDefinition.getUseIdForCategory())) {
      getRequest().setParameter("itemId", pageDefinition.getUseIdForCategory());
    }
    
    final NMProfile nmProfile = (NMProfile) ServletUtil.getCurrentRequest().resolveName("/atg/userprofiling/Profile");
    NMCatalogHistoryCollector.collect(getRequest());
    initializePromoPreview();
    
    
    // set ABTest filter
    final String categoryGroup = AbTestHelper.getAbTestValue(getRequest(), AbTestHelper.GROUP_NAME_KEY);
    if (categoryGroup != null) {
      getCategoryFilterCode().setFilterCode(categoryGroup);
    }
    
    String compareMetricsAbTestGroup = AbTestHelper.getAbTestValue(getRequest(), COMPAREMETRICS_ABTESTGROUP);
    if (compareMetricsAbTestGroup == null) {
      compareMetricsAbTestGroup = "inactive";
    }
    getRequest().setParameter(COMPAREMETRICS_ABTESTGROUP, compareMetricsAbTestGroup);
    
    final String edgeScapeCountryCode = utils.findCountryCodeUsingEdgescape(getRequest());
    
    boolean isImplicitFilterBehaviour = false;
    if (getBrandSpecs() != null) {
      isImplicitFilterBehaviour = getBrandSpecs().isImplicitFilterBehaviour();
    }
    
    String freeBlitzAbTestGroup = AbTestHelper.getAbTestValue(getRequest(), FREE_BLITZ_ABTESTGROUP);
    if (freeBlitzAbTestGroup == null) {
      freeBlitzAbTestGroup = "inactive";
    }
    getRequest().setParameter(FREE_BLITZ_ABTESTGROUP, freeBlitzAbTestGroup);
    
    String freeBlitzShippingAbTestGroup = AbTestHelper.getAbTestValue(getRequest(), FREE_BLITZ_SHIPPING_ABTESTGROUP);
    if (freeBlitzShippingAbTestGroup == null) {
      freeBlitzShippingAbTestGroup = "inactive";
    }
    getRequest().setParameter(FREE_BLITZ_SHIPPING_ABTESTGROUP, freeBlitzShippingAbTestGroup);
    String saleSiloAbTestGroup = AbTestHelper.getAbTestValue(getRequest(), SALE_SILO_DRAWER_ABTEST);
    if (saleSiloAbTestGroup == null) {
      saleSiloAbTestGroup = "inactive";
    }
    getRequest().setParameter(SALE_SILO_DRAWER_ABTEST, saleSiloAbTestGroup);
    // establish pageModel at request level (entire page)
    final PageModel pageModel = allocatePageModel();
    
    // If the page model is other than Endeca driven page model OR Search Result Page than clear the filter selection persistence from profile.
    if (!(pageModel instanceof EndecaDrivenPageModel) || null != pageDefinition.getTemplateType() && pageDefinition.getTemplateType().equalsIgnoreCase(SEARCH)) {
      nmProfile.setPropertyValue(FILTER_PERSISTENCE, null);
    }
    
    pageModel.setPageEvaluator(this);
    pageModel.setProfile(nmProfile);
    pageModel.setFreeBlitzAbTestGroup(freeBlitzAbTestGroup);
    pageModel.setFreeBlitzShippingAbTestGroup(freeBlitzShippingAbTestGroup);
    // Set akamai header's country code to pagemodel
    if (!StringUtils.isBlank(edgeScapeCountryCode)) {
      pageModel.setIpCountryCode(edgeScapeCountryCode);
    }
    pageModel.setActiveCategory(getCategoryHelper().getActiveCategory(getRequest(), utils.getCurrentCountryCode(getRequest())));
    pageModel.setSaleCategory(getCategoryHelper().isSaleCategory(pageModel.getActiveCategory().getDataSource(), getNavHistory(), getRequest()));
    
    // Set akamai header's country code to pagemodel
    if (!StringUtils.isBlank(edgeScapeCountryCode)) {
      pageModel.setIpCountryCode(edgeScapeCountryCode);
    }
    
    // setting variables for internationalization
    final String preferredCountry = nmProfile.getCountryPreference();
    pageModel.setOnlyPinYin(utils.getAllowPinYinCharactersForCountries().contains(preferredCountry));
    pageModel.setShowCpf(utils.getShowCPFFieldForCountries().contains(preferredCountry));
    pageModel.setShowCounty(utils.getShowCountyFieldForCountries().contains(preferredCountry));
    pageModel.setShowMediaTags(utils.getShowMediaTagsForCountries().contains(preferredCountry));
    
    if (CommonComponentHelper.getSystemSpecs().getDisplayMktgTags() && CommonComponentHelper.getSystemSpecs().isFiftyOneEnabled()) {
      if (!StringUtils.isBlank(utils.findCountryCodeUsingEdgescape(getRequest())) && utils.getMktgTagEnabledCountries().contains(utils.findCountryCodeUsingEdgescape(getRequest()))) {
        pageModel.setShowMktgtag(true);
      } else {
        pageModel.setShowMktgtag(false);
      }
    }
    
    if (!StringUtils.isBlank(utils.findCountryCodeUsingEdgescape(getRequest())) && utils.getGoogleAPIRestrictedCountries().contains(utils.findCountryCodeUsingEdgescape(getRequest()))) {
      pageModel.setRestrictGoogleApi(true);
    } else {
      pageModel.setRestrictGoogleApi(false);
    }
    
    if (getBrandSpecs().isEnableFreeBlitz()) {
      final String countryCode = utils.findCountryCodeUsingEdgescape(ServletUtil.getCurrentRequest());
      final String[] freeBlitzUnsupportedCountries = getBrandSpecs().getFreeBlitzUnsupportedCountries();
      if (null != freeBlitzUnsupportedCountries && ArrayUtils.contains(freeBlitzUnsupportedCountries, countryCode)) {
        pageModel.getElementDisplaySwitchMap().put(UIElementConstants.FREEBLITZ, Boolean.FALSE);
      } else {
        pageModel.getElementDisplaySwitchMap().put(UIElementConstants.FREEBLITZ, Boolean.TRUE);
      }
    }
    
    // INT-2239 changes end here
    // Retrieving the current status of "alipayFlag" AB test created in CSR
    String alipayAbTest = AbTestHelper.getAbTestValue(getRequest(), ALIPAY_ABTEST_GROUP);
    if (alipayAbTest == null) {
      alipayAbTest = ShopRunnerConstants.INACTIVE;
    }
    
    // Set pageModel attribute to true/false based on whether Alipay is turned ON or not
    if (CommonComponentHelper.getSystemSpecs().isAlipayEnabled() && alipayAbTest.equalsIgnoreCase(ShopRunnerConstants.ACTIVE)) {
      if (!StringUtils.isBlank(edgeScapeCountryCode) && edgeScapeCountryCode.equalsIgnoreCase(ShopRunnerConstants.CHINA_COUNTRY_CODE)) {
        pageModel.getElementDisplaySwitchMap().put(UIElementConstants.ALIPAY, Boolean.TRUE);
      } else {
        pageModel.getElementDisplaySwitchMap().put(UIElementConstants.ALIPAY, Boolean.FALSE);
      }
    }
    
    /* A list is created then filtered to see which categories are blacklisted.Silos are included in this list as well. */
    final NMCategory category = getCategory();
    if (category != null) {
      List<RepositoryItem> nmcategory = (List<RepositoryItem>) getCategory().getPropertyValue("childCategories");
      getFilterCategoryGroup().setCategoryFilterCode(getCategoryFilterCode());
      nmcategory = getFilterCategoryGroup().filterCategoryList(nmcategory);
    }
    /* get hold of the countryCode and Load country specific silo using tree configured through CatMan */
    // final String countryCode = utils.getCurrentCountryCode(ServletUtil.getCurrentRequest());
    // if (utils.getCountryCodeMappings().getProperty(US_COUNTRY_CODE).equals(countryCode)) {
    // pageModel.setHomeCategory(getCategoryHelper().getRootCategory(getBrandSpecs().getRootCategoryId()));
    // } else {
    // pageModel.setHomeCategory(getCategoryHelper().getRootCategory(utils.getCountryRootCategoryId(countryCode)));
    // }
    
    final NMCategory nmCat = getCategoryHelper().getRootCategory(utils.getCurrentCountryCode(getRequest()));
    nmCat.setFilterCategoryGroup(getFilterCategoryGroup());
    pageModel.setHomeCategory(nmCat);
    pageModel.setRobot(getRobotDetectDroplet().isRobot(getRequest()));
    pageModel.setSeoFooter(getSeoFooterCategory());
    pageModel.setHeaderPath(pageDefinition.getHeaderPath());
    pageModel.setFooterPath(pageDefinition.getFooterPath());
    pageModel.setInternational(false);
    pageModel.setConvertPrices(false);
    
    if (utils.checkForInternational(nmProfile)) {
      pageModel.setInternational(true);
      pageModel.setConvertPrices(true);
      pageModel.setCurrencyCode(nmProfile.getCurrencyPreference());
      pageModel.setExchangeRate(utils.getExchangeRateForShopperCurrency(nmProfile.getCurrencyPreference()));
      pageModel.setRoundMethod(utils.getRoundMethodForShopperCurrency(nmProfile.getCurrencyPreference()));
      pageModel.setFrontLoadCoefficient(utils.getFrontLoadCoefficientForCountry(nmProfile.getCountryPreference()));
    } else {
      pageModel.setHeaderPath(pageDefinition.getHeaderPath());
      pageModel.setFooterPath(pageDefinition.getFooterPath());
      pageModel.setInternational(false);
      pageModel.setConvertPrices(false);
    }
    
    // Merchant Inventory Tool
    // CHECK BRANDSPECS, INTERNAL USER, AND URL PARAMETER FOR LC MERCHANT INVENTORY TOOL ( smit )
    boolean showMIT = false;
    boolean isInternal = NMRequestHelper.isInternalRequest(getRequest());
    if (getRequest().getSession() != null && getBrandSpecs().getShowMerchantInventoryTool() && isInternal) {
      if (getRequest().getSession().getAttribute("showMerchantInventoryTool") == null) {
        getRequest().getSession().setAttribute("showMerchantInventoryTool", false);
      }
      if ("false".equals(getRequest().getParameter("smit"))) {
        getRequest().getSession().setAttribute("showMerchantInventoryTool", false);
      } else if (("true".equals(getRequest().getParameter("smit"))) || ((Boolean) getRequest().getSession().getAttribute("showMerchantInventoryTool") == true)) {
        getRequest().getSession().setAttribute("showMerchantInventoryTool", true);
        showMIT = true;
      }
    }
    pageModel.setShowMIT(showMIT);
    
    // expose the pageModel to the request as an attribute so it can be used in the jsp
    getRequest().setAttribute("pageModel", pageModel);
    
    // establish pageDefinition at request level (entire page)
    final PageDefinition derivedPageDefinition = derivePageDefinition(pageDefinition, pageModel);
    pageModel.setPageDefinition(derivedPageDefinition);
    
    final List<NMCategory> lSiloCats = getAllSiloCategories(pageModel);
    final String currentCatId = getRequest().getParameter("itemId");
    boolean isSiloCategory = false;
    
    if (currentCatId != null) {
      for (final NMCategory cat : lSiloCats) {
        if (currentCatId.equals(cat.getId().toString())) {
          isSiloCategory = true;
          break;
        }
        
      }
    }
    
    // must defer this until the derivedPageDefintion is determined above.
    
    if (Boolean.TRUE.equals(derivedPageDefinition.getEnableBloomreach()) && !isSiloCategory) {
      final ExecutorService executor = Executors.newSingleThreadExecutor();
      final String pType = derivedPageDefinition.getBloomreachPageType();
      final Callable<Map<String, String>> callable = new BloomreachCallable(getRequest(), pType);
      final Future<Map<String, String>> bloomreachFuture = executor.submit(callable);
      pageModel.setBloomreachWidget(bloomreachFuture);
      executor.shutdown();
    }
    
    getRequest().setAttribute("pageDefinition", derivedPageDefinition);
    
    /* defect 41605 start */
    
    if (!(getPageModel().getPageDefinition().getId().equalsIgnoreCase("OrderReview") || getPageModel().getPageDefinition().getId().equalsIgnoreCase("ManagePayment"))) {
      final ShoppingCartHandler cart = CheckoutComponents.getCartHandler(getRequest());
      final NMOrderImpl order = cart.getNMOrder();
      final String retryKey = "RETRY_COUNT_" + order.getId();
      if (null != getRequest().getSession()) {
		  getRequest().getSession().setAttribute(retryKey, 0);
	  }
    }
    /* defect 41605 End */
    
    // check to see if page should display custom Chinese redirect asset
    final String chinaRedirect = getRequest().getParameter("OverlayAsset");
    if (StringUtilities.isNotEmpty(chinaRedirect) && chinaRedirect.equals("true")) {
      pageModel.setDisplayChinaRedirectAsset(true);
    }
    
    // enable localized language display in dropdown for specific country and set loclized values for CLC
    final String profileCountry = nmProfile.getCountryPreference();
    pageModel.setShowLocalizedLanguage(false);
    getProfile().setShowLocalizedLanguage(false);
    
    // INT-1642 : Changes on Home Promo Category for Borderfree supported countries starts here
    final Boolean populateSellableProducts = (Boolean) pageContext.getRequest().getAttribute("populateSellableProducts");
    if (populateSellableProducts != null && populateSellableProducts.booleanValue()) {
      final NMConfigurations nmConfigurations = (NMConfigurations) Nucleus.getGlobalNucleus().resolveName("/nm/utils/NMConfigurations");
      final ProdSkuUtil prodSkuUtil = CommonComponentHelper.getProdSkuUtil();
      final NMCategory rootCategoryRI = CategoryHelper.getInstance().getNMCategory(nmConfigurations.getHomePromoRootId());
      if (null != rootCategoryRI) {
        final String homePromoRootCatDesc = rootCategoryRI.getLongDescription();
        if (homePromoRootCatDesc != null) {
          pageModel.setHomePromoRootCatDesc(homePromoRootCatDesc);
        }
        final List<NMCategory> promoCategoriesList = rootCategoryRI.getChildCategories();
        if (null != promoCategoriesList && promoCategoriesList.size() > 0) {
          final List<NMCategory> nonBlacklistedCategories = CategoryHelper.getInstance().getNonRestrictedPromoCategories(promoCategoriesList, rootCategoryRI.getCountryCode());
          if (null != nonBlacklistedCategories && nonBlacklistedCategories.size() > 0) {
            pageModel.setIntlHomePromoCategories(nonBlacklistedCategories);
          }
        }
        // INT-1642 changes starts - getting the sellable products and filtering that for non restricted products
        final List<RepositoryItem> sellableProducts = prodSkuUtil.getSellableChildProducts(rootCategoryRI);
        if (null != sellableProducts && sellableProducts.size() > 0) {
          pageModel.setIntlHomePromoProducts(prodSkuUtil.filterRestrictedandNonSellableProducts(sellableProducts));
        }
        // INT-1642 changes ends
      }
    }
    // INT-1642 : Changes on Home Promo Category for Borderfree supported countries ends here
    
    // Changes made for enabling the promo sticker for product thumbnail pages.
    if (pageDefinition.getPromoStickerEnabled()) {
      evaluateTheCategoryForPromoSticker(); // LastCall uses this.
    }
    /* Hide CLC for Employee Experience. */
    final Boolean isAssociateLoggedIn = (Boolean) nmProfile.getPropertyValue("isAssociateLoggedIn");
    if (nmProfile.isAuthorized() && isAssociateLoggedIn != null && isAssociateLoggedIn.booleanValue()) {
      // Hide CLC in Employee Landing Page.
      pageModel.getElementDisplaySwitchMap().put(UIElementConstants.CLC, Boolean.FALSE);
    }
    // EDO-Setting ErrorMessage for SyncFail.
    if (null != getRequest().getSession() && !StringUtils.isBlank((String) getRequest().getSession().getAttribute(EmployeeDiscountConstants.SYNC_FAIL)) && !getPageModel().getPageDefinition().getId().equalsIgnoreCase("OrderComplete")) {
      pageModel.setPopupMsg((String) getRequest().getSession().getAttribute(EmployeeDiscountConstants.SYNC_FAIL));
      getRequest().getSession().removeAttribute(EmployeeDiscountConstants.SYNC_FAIL);
    }
    // EDO-Setting ErrorMessage for SyncFail.
    
    // Setting the wMat parameter in the session. This is to disable Alipay welcome mat for AMEX promo on the China site.
    final String wMat = getRequest().getParameter("wMat");
    if (null != wMat && null != getRequest().getSession()) {
      getRequest().getSession().setAttribute("wMatInSession", wMat);
    } else if (null != getRequest().getSession() && null != getRequest().getSession().getAttribute("wMatInSession")) {
      final String wMatInSession = (String) getRequest().getSession().getAttribute("wMatInSession");
      getRequest().setParameter("wMat", wMatInSession);
    }
    
    // Setting the BFX parameter to set vendor parameters.
    final Boolean bfxEnabled = getBrandSpecs().isBfxEnabled();
    if (bfxEnabled) {
      final String bfxURL = getRequest().getHeader("referer");
      final FiftyOneUtils fiftyOneUtils = (FiftyOneUtils) Nucleus.getGlobalNucleus().resolveName("/nm/utils/fiftyone/FiftyOneUtils");
      if (getRequest().getSession() != null && !StringUtils.isEmpty(bfxURL) && bfxURL.contains(fiftyOneUtils.getBfxReferer())) {
        getRequest().getSession().setAttribute("bfxOrder", true);
      }
    }
    // Setting the loggedInPreviousPageFlag in pageModel from nameContext.
    final NameContext nameContext = getRequest().getRequestScope();
    if (nameContext != null) {
      Object elementValue = nameContext.getElement(TMSDataDictionaryConstants.LOGGED_IN_PREVIOUS_PAGE);
      if (elementValue != null) {
        pageModel.setLoggedInPreviousPageFlag(NmoUtils.getBooleanValue(elementValue));
      }
    }
    pageModel.setUrlDecodedEmail(getDecodedEmailUrl());
    // This method displays tooltips if EnableSitePersonalization flag is true for a brand
    if (getBrandSpecs().isEnableSitePersonalization()) {
      getSitePersonalizationTooltips(pageDefinition, pageModel);
    }
    
    // Data Dictionary Attributes population.
    if (SimplePageEvaluator.class.equals(this.getClass())) {
      final TMSDataDictionaryUtils dataDictionaryUtils = CommonComponentHelper.getDataDictionaryUtils();
      dataDictionaryUtils.processTMSDataDictionaryAttributes(pageDefinition, null, getProfile(), pageModel, new TMSMessageContainer());
    }
    
    /* Added as a part off NMOBLDS-12817 */
    // Setting the mobilePreview parameter in the session.
    final String mobilePreview = getRequest().getParameter("mobilePreview");
    final String updateSession = getRequest().getParameter("updateSession");
    String mobilePreviewSessionValue = null;
    if (getRequest().getSession() != null) {
      mobilePreviewSessionValue = (String) getRequest().getSession().getAttribute("mobilePreviewInSession");
    }
    if (getRequest().getSession() != null && null != mobilePreview && !"false".equalsIgnoreCase(updateSession)) {
      getRequest().getSession().setAttribute("mobilePreviewInSession", mobilePreview);
    } else if (null != mobilePreviewSessionValue && !"false".equalsIgnoreCase(updateSession)) {
      getRequest().setParameter("mobilePreview", mobilePreviewSessionValue);
    } else if (null != mobilePreviewSessionValue) {
      getRequest().setParameter("mobilePreview", mobilePreview);
    }
    if (getRequest().getSession() != null) {
      getRequest().getSession().setAttribute("scrollCount", null);
    }
    
    return true;
    
  }
  
  /**
   * This method invoke method in SitePersonalizationConfig Class to display ToolTips
   * 
   * @param pageDefinition
   * @param pageModel
   */
  private void getSitePersonalizationTooltips(final PageDefinition pageDefinition, final PageModel pageModel) {
    final SitePersonalizationConfig sitePersonalizationConfig = CommonComponentHelper.getSitePersonalizationConfig();
    sitePersonalizationConfig.setPageModelAttributeWithToolTipData(getRequest(), getResponse(), pageModel);
  }
  
  /**
   * Gets the decoded email url.
   * 
   * @return the decoded email url
   */
  public String getDecodedEmailUrl() {
    String decodedUrl = null;
    final String urlEmail = getRequest().getParameter("uEm");
    if (StringUtilities.isNotEmpty(urlEmail)) {
      final CryptoLinkedEmail clEmail = new CryptoLinkedEmail();
      if (clEmail.validateEncryptedValue(urlEmail)) {
        final String uEmDecoded = clEmail.decrypt(urlEmail, INMGenericConstants.EMPTY_STRING);
        decodedUrl = uEmDecoded;
      }
    }
    return decodedUrl;
  }
  
  private NMCategory getCategory() throws IOException {
    return getCategoryHelper().getNMCategory(getRequest().getParameter("itemId"), getRequest().getParameter("parentId"));
  }
  
  /**
   * A promo sticker will be displayed on the thumbnail pages if a category has valid values for the attributes promoStickerToggle, promoStickerPath, and promoStickerPromoId.
   */
  private void evaluateTheCategoryForPromoSticker() {
    if (getRequest().getSession() != null && getRequest().getSession().getAttribute("promoStickerDisabled") == null) {
      getRequest().getSession().setAttribute("promoStickerDisabled", false);
    }
    if (getRequest().getSession() != null && !(Boolean) getRequest().getSession().getAttribute("promoStickerDisabled")) {
      final PageModel pageModel = getPageModel();
      if (pageModel.getActiveCategory() != null) {
        String promoStickerToggle = null;
        String promoStickerPath = null;
        String promoId = null;
        final Map<String, String> categoryAttributesMap = pageModel.getActiveCategory().getCategoryAttributeMap();
        if (categoryAttributesMap != null) {
          promoStickerToggle = categoryAttributesMap.get("promoStickerToggle");
          promoStickerPath = categoryAttributesMap.get("promoStickerPath");
          promoId = categoryAttributesMap.get("promoStickerPromoId");
        }
        if (!NmoUtils.isEmpty(promoStickerToggle) && promoStickerToggle.trim().equalsIgnoreCase("true") && !NmoUtils.isEmpty(promoStickerPath)) {
          boolean displayPromoSticker = false;
          if (NmoUtils.isEmpty(promoId)) {
            displayPromoSticker = true;
          } else {
            final PromotionsHelper promoHelper = (PromotionsHelper) Nucleus.getGlobalNucleus().resolveName("/nm/utils/PromotionsHelper");
            final NMPromotion promo = promoHelper.getPromotionFromPromoId(promoId);
            boolean isPromoActive = false;
            if (promo != null) {
              isPromoActive = promo.isPromotionActive();
            }
            displayPromoSticker = isPromoActive;
          }
          pageModel.setDisplayPromoSticker(displayPromoSticker);
        }
      }
    }
  }
  
  // extend this method with code to execute only for the cached content area
  @Override
  public void evaluateContent(final PageContext context) throws Exception {
    // Do nothing by default
  }
  
  protected CategoryFilterCode getCategoryFilterCode() {
    return (CategoryFilterCode) getRequest().resolveName("/nm/utils/CategoryFilterCode");
  }
  
  protected FilterCategoryGroup getFilterCategoryGroup() {
    return (FilterCategoryGroup) getRequest().resolveName("/nm/droplet/FilterCategoryGroup");
  }
  
  // extend this method for a more specific page model
  protected PageModel allocatePageModel() {
    return new PageModel();
  }
  
  // extend this method to revise the page definition.
  // return null to not include the tag contents, as in a redirect.
  protected PageDefinition derivePageDefinition(final PageDefinition pageDefinition, final PageModel pageModel) throws Exception {
    return pageDefinition;
  }
  
  protected DynamoHttpServletRequest getRequest() {
    return ServletUtil.getCurrentRequest();
  }
  
  protected DynamoHttpServletResponse getResponse() {
    return ServletUtil.getCurrentResponse();
  }
  
  protected boolean isSecure() {
    return "1".equals(getRequest().getHeader("ISSECURE"));
  }
  
  protected PageModel getPageModel() {
    final PageModel model = (PageModel) getRequest().getAttribute("pageModel");
    return model;
  }
  
  public static BaseResultBeanContainer getBaseResultBeanContainer(final DynamoHttpServletRequest request) {
    return (BaseResultBeanContainer) request.resolveName(BaseResultBeanContainer.CONFIG_PATH);
  }
  
  public BrandSpecs getBrandSpecs() {
    return (BrandSpecs) getRequest().resolveName("/nm/utils/BrandSpecs");
  }
  
  protected NMCatalogHistory getNavHistory() {
    return (NMCatalogHistory) getRequest().resolveName(NMCatalogHistory.COMPONENT_NAME);
  }
  
  protected NMProfile getProfile() {
    return (NMProfile) getRequest().resolveName("/atg/userprofiling/Profile");
  }
  
  /**
   * Retrieves the /nm/commerce/catalog/CategoryHelper component
   * 
   * @return CategoryHelper instance
   */
  protected CategoryHelper getCategoryHelper() {
    return (CategoryHelper) getRequest().resolveName("/nm/commerce/catalog/CategoryHelper");
  }
  
  protected RobotDetectDroplet getRobotDetectDroplet() {
    return (RobotDetectDroplet) getRequest().resolveName("/nm/droplet/RobotDetectDroplet");
  }
  
  private void initializePromoPreview() throws ServletException, IOException {
    final EnablePromotionsPreview preview = (EnablePromotionsPreview) getRequest().resolveName("/nm/droplet/EnablePromotionsPreview");
    final String promoPreview = getRequest().getParameter("promoPreview");
    final boolean isEnabled = promoPreview == null ? preview.getPromoPreview() : Boolean.valueOf(promoPreview).booleanValue();
    preview.setPromoPreview(isEnabled);
  }
  
  private List<NMCategory> getSeoFooterCategory() {
    return CategoryHelper.getInstance().getSeoFooterCategories(getFilterCategoryGroup());
    /*
     * RepositoryItem seoCat = (RepositoryItem)getCategoryHelper().getCategory(getBrandSpecs().getSeoFooterCategoryId()); List<NMCategory> seoFooter = new java.util.ArrayList<NMCategory>(); if(seoCat
     * != null){ try{ Repository productRepository = (Repository)getRequest().resolveName("/atg/commerce/catalog/ProductCatalog"); RepositoryItem frag =
     * (RepositoryItem)productRepository.getItem(getBrandSpecs().getFooterSiloFragmentId(), "htmlfragments"); if(frag != null){ String s_count = (String)frag.getPropertyValue("frag_value"); int count
     * = -1; if(s_count != null){ count = Integer.valueOf(s_count.trim()).intValue(); } if(count > 0){ seoFooter = (new NMCategory(seoCat, null,
     * getFilterCategoryGroup())).getChildCategoriesByCount(count); } } }catch(RepositoryException re){
     * System.out.println("Error in SimplePageEvaluator:"+re.getMessage()+" attempting to retreive seo footer silo fragement."); }catch(NumberFormatException nfe){
     * System.out.println("Error in SimplePageEvaluator: NumberFormatException retrieving silo count seo footer value from fragement."); } catch (Exception e) {
     * System.out.println("Error in SimplePageEvaluator:"+e.getMessage()+" attempting to retreive seo footer silo fragement."); }
     * 
     * } return seoFooter;
     */
  }
  
  protected boolean isPrefUserChoiceAll(final TemplatePageDefinition pageDefinition, final DynamoHttpServletRequest request, final boolean isEndeca) {
    if (!isEndeca) {
      final String view = request.getParameter("view");
      final String fview = request.getParameter("fview");
      if (view != null && view.equals("limit") || fview != null && view != null && view.equals("favi") && fview.equals("limit")) {
        return false;
      }
    }
    final Profile profile = getProfile();
    if (pageDefinition.getUserViewChoiceEnabled()) {
      final String userViewChoice = (String) profile.getPropertyValue("preferredViewChoice");
      if (userViewChoice != null && userViewChoice.equals("ALL")) {
        return true;
      }
    }
    return false;
  }
  
  protected void setUserPref(final TemplatePageDefinition pageDefinition, final DynamoHttpServletRequest request, final String Value) {
    final String viewClick = request.getParameter("viewClick");
    final Profile profile = getProfile();
    if (pageDefinition.getUserViewChoiceEnabled() && viewClick != null) {
      profile.setPropertyValue("preferredViewChoice", Value);
    }
    
  }
  
  private static class BloomreachCallable extends NMAbstractCallable<Map<String, String>> {
    DynamoHttpServletRequest request;
    String pType;
    
    public BloomreachCallable(final DynamoHttpServletRequest request, final String pType) {
      this.request = request;
      this.pType = pType;
    }
    
    @Override
    public Map<String, String> run() {
      // long startTime = System.currentTimeMillis();
      Map<String, String> returnValue = null;
      final BloomreachSuggest bloomreachSuggest = (BloomreachSuggest) request.resolveName("/nm/droplet/bloomreach/BloomreachSuggest");
      final String scheme = request.getScheme();
      String uri = (String) request.getAttribute(AbTestFilter.ABTEST_ORIGINAL_URI);
      if (uri == null) {
        uri = request.getRequestURI();
      }
      final String queryString = request.getQueryString();
      final String userAgent = request.getHeader("User-Agent");
      try {
        returnValue = bloomreachSuggest.getWidgetMap(scheme, uri, queryString, pType, userAgent, false, false);
      } catch (final UnsupportedEncodingException exception) {
        if (log.isLoggingError()) {
          log.logError("bloomreachCallable call(): " + exception.getMessage());
        }
      }
      // System.out.println("(nmwps will remove sysout) BloomreachCallable duration: " + (System.currentTimeMillis() - startTime));
      return returnValue;
    }
  }
  
  private List<NMCategory> getAllSiloCategories(final PageModel pageModel) {
    List<NMCategory> lSiloCategories = new ArrayList<NMCategory>();
    lSiloCategories = pageModel.getHomeCategory().getChildCategories();
    return lSiloCategories;
  }
  
}
