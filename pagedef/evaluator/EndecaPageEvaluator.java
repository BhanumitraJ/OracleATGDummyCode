package com.nm.commerce.pagedef.evaluator;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.PageContext;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;
import org.apache.http.impl.cookie.BasicClientCookie;

import atg.core.net.URLUtils;
import atg.nucleus.Nucleus;
import atg.repository.RepositoryItem;
import atg.servlet.DynamoHttpServletRequest;
import atg.servlet.DynamoHttpServletResponse;
import atg.servlet.ServletUtil;
import atg.userprofiling.NMCookieManager;

import com.endeca.navigation.Dimension;
import com.endeca.navigation.ETInstrumentor;
import com.endeca.navigation.Navigation;
import com.endeca.navigation.PropertyMap;
import com.nm.abtest.AbTestHelper;
import com.nm.ajax.checkout.utils.ComponentUtils;
import com.nm.ajax.request.JspPageInvoker;
import com.nm.ajax.request.XStreamUtil;
import com.nm.ajax.search.beans.FacetDimension;
import com.nm.ajax.search.beans.FacetDimensionGroup;
import com.nm.ajax.search.beans.FacetDimensionValue;
import com.nm.ajax.search.beans.FacetInfo;
import com.nm.ajax.search.beans.Facets;
import com.nm.catalog.navigation.CategoryHelper;
import com.nm.catalog.navigation.NMCategory;
import com.nm.collections.DesignerIndexer;
import com.nm.collections.DesignerIndexer.Designer;
import com.nm.collections.SaleDesignerIndexer;
import com.nm.commerce.NMOrderHolder;
import com.nm.commerce.NMProfile;
import com.nm.commerce.catalog.NMProduct;
import com.nm.commerce.checkout.CheckoutComponents;
import com.nm.commerce.pagedef.definition.EndecaTemplatePageDefinition;
import com.nm.commerce.pagedef.definition.PageDefinition;
import com.nm.commerce.pagedef.model.EndecaDrivenPageModel;
import com.nm.commerce.pagedef.model.EndecaPageModel;
import com.nm.commerce.pagedef.model.PageModel;
import com.nm.commerce.pagedef.model.bean.SortOption;
import com.nm.commerce.promotion.NMOrderImpl;
import com.nm.common.INMGenericConstants;
import com.nm.components.CommonComponentHelper;
import com.nm.droplet.EndecaZoneLookup;
import com.nm.formhandler.EndecaSearchFormHandler;
import com.nm.formhandler.ShoppingCartHandler;
import com.nm.search.GenericSearchUtil;
import com.nm.search.bloomreach.BloomreachSearch;
import com.nm.search.endeca.EndecaDrivenUtil;
import com.nm.search.endeca.EndecaQueryHelper;
import com.nm.search.endeca.EndecaQueryHelper.EndecaQueryOutput;
import com.nm.search.endeca.GeoSearchParams;
import com.nm.search.endeca.NMDimension;
import com.nm.search.endeca.NMSearchQuery;
import com.nm.search.endeca.NMSearchRecord;
import com.nm.search.endeca.NMSearchResult;
import com.nm.search.endeca.NMSearchSessionData;
import com.nm.search.endeca.NMSearchUtil;
import com.nm.search.endeca.SearchParameters;
import com.nm.storeinventory.GeoLocation;
import com.nm.storeinventory.InStoreSearchInput;
import com.nm.storeinventory.StoreInventoryFacade;
import com.nm.tms.core.TMSMessageContainer;
import com.nm.tms.utils.TMSDataDictionaryUtils;
import com.nm.twoclickcheckout.util.TwoClickCheckoutUtil;
import com.nm.utils.BRSessionData;
import com.nm.utils.BrandSpecs;
import com.nm.utils.GenericLogger;
import com.nm.utils.LocalizationUtils;
import com.nm.utils.NMFormHelper;
import com.nm.utils.NmoUtils;
import com.nm.utils.PCSSessionData;
import com.nm.utils.StringUtilities;

/**
 * Updates variables and sets up any redirects before page execution. Returns true if page content should be output.
 */
public class EndecaPageEvaluator extends SimplePageEvaluator {
  
  protected GenericLogger log = CommonComponentHelper.getLogger();
  /** Constant string to hold end */
  public static final String SORT_VALUE_END = "end";
  /** */
  public static final String CATEGORY = "Category";
  
  @Override
  protected PageModel allocatePageModel() {
    return new EndecaDrivenPageModel();
  }
  
  @Override
  public boolean evaluate(PageDefinition pageDefinition, final PageContext pageContext) throws Exception {
    final ShoppingCartHandler cart = CheckoutComponents.getCartHandler(getRequest());
    final NMOrderHolder orderHolder = (NMOrderHolder) cart.getOrderHolder();
    NMOrderImpl order = (NMOrderImpl) orderHolder.getCurrent();
    /* If the order is two click checkout order and the two click checkout flow is cancelled then the saved order should be merged. */
    if (order.isBuyItNowOrder()) {
      final TwoClickCheckoutUtil twoClickCheckoutUtil = CommonComponentHelper.getTwoClickCheckoutUtil();
      if (null != twoClickCheckoutUtil) {
        order = twoClickCheckoutUtil.moveSavedOrderBackToCurrent(orderHolder);
      }
    }
    final String defID = pageDefinition.getId();
    final PageDefinition origPageDefinition = pageDefinition;
    
    final DynamoHttpServletRequest request = getRequest();
    final String requestParameter = request.getParameter("view");
    final boolean isStorePage = requestParameter != null && requestParameter.equals("favs");
    final boolean isWelcomePage = requestParameter != null && requestParameter.equals("welcome");
    final TMSMessageContainer tmsMessageContainer = CheckoutComponents.getTMSMessageContainer(getRequest());
    
    if (isStorePage || isWelcomePage) {
      super.evaluate(pageDefinition, pageContext);
      return true;
    }
    
    final LocalizationUtils utils = (LocalizationUtils) Nucleus.getGlobalNucleus().resolveName("/nm/utils/LocalizationUtils");
    final NMProfile nmProfile = (NMProfile) ServletUtil.getCurrentRequest().resolveName("/atg/userprofiling/Profile");
    final BrandSpecs brandSpecs = getBrandSpecs();

    // BR Changes- Retrieve BR/Endeca Search ABTest value
    // To remove final String searchAbTestGroup = CommonComponentHelper.getBrSearchUtil().getSearchGroup();
    final BloomreachSearch brSearch = BloomreachSearch.getInstance();
    final String eFrom = request.getParameter("from");
    final boolean isMyFavPage = pageDefinition.getId() != null && (pageDefinition.getId().equalsIgnoreCase("myfavoriteitems") || pageDefinition.getId().equalsIgnoreCase("myfavorites"));
    
    // Should not go to Bloomreach Search for SaleDesignerIndex and EventDesignerIndex pages
    if (brSearch.isEnabled() && !isMyFavPage && brandSpecs.isShowBloomReachSearch() && !StringUtils.isEmpty(eFrom)) {
      if (StringUtilities.isNotEmpty(eFrom) && !(eFrom.equalsIgnoreCase("brSearchExcep") || eFrom.equalsIgnoreCase("eventdi") || eFrom.equalsIgnoreCase("saledi")) || null == eFrom
              && !(request.getRequestURI().contains("SaleDesignerIndex") || request.getRequestURI().contains("eventDesignerIndex"))) {
        redirectToBloomReachSearch(request, getResponse());
        return false;
      }
    }
    final boolean isSupportedByFiftyOne = utils.isSupportedByFiftyOne(getProfile().getCountryPreference());
    request.setParameter(LocalizationUtils.IS_FIFTYONE_SUPPORTED, isSupportedByFiftyOne);
    // String systemCode = ComponentUtils.getInstance().getSystemSpecs().getProductionSystemCode();
    boolean isKeywordSearch = false;
    boolean isEventDI = false;
    boolean isSaleDI = false;
    
    String sTerm = request.getParameter("Ntt");
    sTerm = URLUtils.escapeUrlString(sTerm);
    
    if (defID.equals("EventDesignerIndexE") || StringUtilities.isNotEmpty(eFrom) && eFrom.equalsIgnoreCase("eventdi")) {
      // needed to pull down the correct sort options on the EF1 template
      isEventDI = true;
    }
    
    if (StringUtilities.isNotEmpty(eFrom) && eFrom.equalsIgnoreCase("saledi")) {
      isSaleDI = true;
    }
    
    final String onlineOrStore = request.getParameter("onlineOrStore");
    boolean isOnline = false;
    if (null != onlineOrStore && onlineOrStore.equals("online")) {
      isOnline = true;
    }
    
    if (!NmoUtils.isEmpty(sTerm)) {
      isKeywordSearch = true;
      // START - Added to record all the terms user searched, for feeding to the Rich Relevance API (PCS)
      final PCSSessionData sessionData = (PCSSessionData) request.resolveName("/nm/utils/PCSSessionData");
      if (sessionData != null) {
        sessionData.updateTermsUsedForSearch(sTerm);
      }
      // END - Added to record all the terms user searched, for feeding to the Rich Relevance API (PCS)
    }
    
    final boolean displaySaleNav = getBrandSpecs().getDisplaySaleNav();
    String sale = request.getParameter("st");
    String Nval = request.getParameter("N");
    String RaVal = request.getParameter("Ra");
    
    if (StringUtils.isNotEmpty(Nval)) {
      Nval = Nval.replace("+", "").trim();
    } else {
      Nval = "";
    }
    
    // The below code checks if the category nav is applied or not
    // Zero value is reserved for the endeca category dimval id
    // in N parameter of the URL
    boolean categoryNavApplied = true;
    
    if (Nval.trim().startsWith("0")) {
      categoryNavApplied = false;
    }
    // Check for the applied category nav ends here
    
    if (StringUtils.isEmpty(RaVal)) {
      RaVal = "false";
    }
    
    if (StringUtilities.isNotEmpty(sTerm) && sTerm.toLowerCase().contains("sale") && (Nval.equals("0") || !categoryNavApplied) && !RaVal.equals("true")) {
      sale = "s";
    }
    
    if (sale != null && sale.equalsIgnoreCase("s") && displaySaleNav) {
      // fetch alternative page definition when search for sale
      pageDefinition = ((EndecaTemplatePageDefinition) pageDefinition).getSaleSearchTemplate();
    }
    
    final String tv = request.getParameter("tv");
    if ((defID.equals("ELs") || defID.equals("EFs") || defID.equals("EFsNew") || defID.equals("EF3s")) && (tv != null)) {
      if (tv.equals("bc") && ((EndecaTemplatePageDefinition) pageDefinition).getBcTemplate() != null) {
        pageDefinition = ((EndecaTemplatePageDefinition) pageDefinition).getBcTemplate();
      }
      if (tv.equals("lc") && ((EndecaTemplatePageDefinition) pageDefinition).getLcTemplate() != null) {
        pageDefinition = ((EndecaTemplatePageDefinition) pageDefinition).getLcTemplate();
      }
      if (tv.equals("pv") && ((EndecaTemplatePageDefinition) pageDefinition).getPvTemplate() != null) {
        pageDefinition = ((EndecaTemplatePageDefinition) pageDefinition).getPvTemplate();
      }
    }
    
    final EndecaTemplatePageDefinition etPageDefinition = (EndecaTemplatePageDefinition) pageDefinition;
    
    SearchParameters searchParameters = etPageDefinition.getSearchParameters();
    if (searchParameters == null) {
      searchParameters = NMSearchUtil.getDefaultSearchParameters();
    }
    
    if (getBrandSpecs().isVisualDebug()) {
      searchParameters.addDebugFieldList();
    }
    if (defID.equalsIgnoreCase("ES") || defID.equalsIgnoreCase("ESs")) {
      searchParameters.setSearchPage(true);
    }
    if ("Y".equalsIgnoreCase(etPageDefinition.getSwitchView())) {
      this.getProfile().setPropertyValue("preferredEndecaTemplate", etPageDefinition.getTemplatePath());
      request.setParameter("defaultEndecaTemplateView", etPageDefinition.getTemplatePath());
    }
    
    final String location = request.getParameter("location");
    final String radius = request.getParameter("radius");
    final Boolean allStoresSearch = NmoUtils.coalesce(request.getParameter("allStores"), "").equals("1");
    String eQLFilter = null;
    GeoSearchParams geoParams = null;
    InStoreSearchInput inStoreSearchInput = InStoreSearchInput.createFromRequest(request, "location", "radius", "allStores");
    // StoreAvailQuery storeAvailQuery = new StoreAvailQuery(inStoreSearchInput, (StoreInventoryFacade) request.resolveName("/nm/storeinventory/StoreInventoryFacade"), null);
    
    if (NmoUtils.notEmpty(location, radius) || allStoresSearch) {
      inStoreSearchInput = new InStoreSearchInput(location, radius, allStoresSearch);
      if (!allStoresSearch) {
        final StoreInventoryFacade storeInventoryFacade = (StoreInventoryFacade) request.resolveName("/nm/storeinventory/StoreInventoryFacade");
        final GeoLocation geoLocation = storeInventoryFacade.geoCodeAddress(location);
        if (null != geoLocation) {
          geoParams = new GeoSearchParams(geoLocation, NmoUtils.convertMilesToKilometers(inStoreSearchInput.getRadius()));
        }
      }
      eQLFilter = "&Nrs=" + NMSearchQuery.constructEQLSearchString(geoParams, inStoreSearchInput.getAllStoresSearch());
    }
    
    if (null != onlineOrStore && onlineOrStore.equals("online")) {
      eQLFilter = "&Nrs=" + new StringBuilder("collection()/record[").append("not(IN_STORE)").append("]");
      
    }
    
    // set request parameters before calling NMCatalogHistoryCollector in
    // super, to load the parameterMap correctly for the NMShoppableClick
    if (eQLFilter != null) {
      searchParameters.setQueryString(getQueryString(etPageDefinition) + eQLFilter);
    } else {
      searchParameters.setQueryString(getQueryString(etPageDefinition));
    }
    searchParameters.setPageSize(getPageSize(etPageDefinition));
    
    if (isSaleDI) {
      if (sale != null && sale.equalsIgnoreCase("s")) {
        searchParameters.setAdditionalQueryString(CommonComponentHelper.getNMSearchPages().getSaleDIQueryString());
      } else {
        searchParameters.setAdditionalQueryString(CommonComponentHelper.getNMSearchPages().getSaleDIQueryStringLive());
      }
    } else {
      searchParameters.setAdditionalQueryString(getAdditionalQueryString(etPageDefinition));
    }
    
    searchParameters.setGenerateSaleNav(false);
    if (displaySaleNav) {
      searchParameters.setGenerateSaleNav(true);
      if (StringUtilities.isNotEmpty(sale) && sale.equalsIgnoreCase("s")) {
        searchParameters.setSaleQuery(true);
        if (isSaleDI) {
          searchParameters.setAltAdditionalQueryString(CommonComponentHelper.getNMSearchPages().getSaleDIQueryStringLive());
        } else {
          searchParameters.setAltAdditionalQueryString(((EndecaTemplatePageDefinition) origPageDefinition).getAdditionalQueryString());
        }
        
      } else {
        searchParameters.setSaleQuery(false);
        if (isSaleDI) {
          searchParameters.setAltAdditionalQueryString(CommonComponentHelper.getNMSearchPages().getSaleDIQueryString());
        } else {
          searchParameters.setAltAdditionalQueryString(etPageDefinition.getSaleSearchTemplate().getAdditionalQueryString());
        }
      }
    }
    
    searchParameters.setDefaultQueryString(etPageDefinition.getDefaultQueryString());
    searchParameters.setExposedDimensions(etPageDefinition.getExposedDimensions());
    searchParameters.setProfileCampaigns(getProfile().getProfileCampaigns());
    searchParameters.setBuildTree(true);
    searchParameters.setSecondarySorts(etPageDefinition.getSecondarySorts());
    
    if (StringUtils.isEmpty(etPageDefinition.getUseNavCatDim())) {
      searchParameters.setUseNavCatDim(false);
    } else {
      searchParameters.setUseNavCatDim(Boolean.valueOf(etPageDefinition.getUseNavCatDim()));
    }
    
    // catId will be null if from search input (ntt=)
    final String catId = request.getParameter("itemId");
    
    searchParameters.setExpandWeb1(etPageDefinition.getExpandWeb1());
    searchParameters.setFilterWeb1(etPageDefinition.getFilterWeb1());
    searchParameters.setUserSelectedCountryCode(getProfile().getCountryPreference());
    
    // Removing the eqlFilter parameter because this query string ends up in the urls of the breadcrumb
    String urlQueryString = searchParameters.getQueryString().replaceFirst(Pattern.quote(NmoUtils.coalesce(eQLFilter, "")), "");
    urlQueryString = NMSearchUtil.replaceById(urlQueryString, "Ntt", sTerm).replace(" ", "+");
    urlQueryString = NMSearchUtil.replaceById(urlQueryString, "Ra", "false");
    
    // I have to set this parameter in order to get the
    // shoppableClick to work. This is a ridiculous carry over of our
    // abuse of the DynamoHttpServletRequest's ability to set parameters.
    request.setParameter("queryString", urlQueryString);
    
    // parameter endecaTemplate is required to make the lastShoppableClick
    // as NMEndecaShoppableClick in NMCatalogHistoryCollector.
    request.setParameter("endecaTemplate", pageDefinition);
    
    // this sets itemId parameter on the request to the page definition
    // getUseIdForCategory
    if (!super.evaluate(pageDefinition, pageContext)) {
      return false;
    }
    
    final EndecaDrivenPageModel ePageModel = (EndecaDrivenPageModel) getPageModel();

    final String fixedDimensionId = request.getParameter("fixedDimId");
    ArrayList<String> fixedDimensionIds = new ArrayList<String>();
    boolean suppressDesignerFilter = false;
    
    if (fixedDimensionId == null) {
      searchParameters.setItemId(catId);
      request.setParameter("categoryId", catId);
      request.setAttribute("categoryId", catId);
    } else {
      ePageModel.setFixedDimId(fixedDimensionId);
      fixedDimensionIds.add(fixedDimensionId);
      DesignerIndexer designerIndexer = DesignerIndexUtils.getDesignerIndexer();
      Map<String, Designer> designersByDimensionId = designerIndexer.getEndecaDrivenDesignersByDimensionIdMap();
      if (designersByDimensionId.get(fixedDimensionId) != null) {
        suppressDesignerFilter = true;
        ePageModel.setFixedDesigner(designersByDimensionId.get(fixedDimensionId));
      }
    }
    
    if (inStoreSearchInput.isGeoSearch()) {
      ePageModel.setInStoreSearchInput(inStoreSearchInput);
    }
    
    ePageModel.setOnlineOrStore(onlineOrStore);
    
    if (!NmoUtils.isEmpty(etPageDefinition.getExplicitParentId())) {
      request.setParameter("parentId", etPageDefinition.getExplicitParentId());
    }
    
    if (!NmoUtils.isEmpty(etPageDefinition.getUseIdForCategory())) {
      request.removeParameter("itemId");
    }
    
    ePageModel.setSaleQuery(false);
    
    // seo enhancement: capture seoSiloName, seoCategoryName and seoDesignerName from request URL
    final String seoSiloName = request.getParameter("seoSiloName");
    final String seoCategoryName = request.getParameter("seoCategoryName");
    final String seoDesignerName = request.getParameter("seoDesignerName");
    final String seoSuffix = request.getParameter("seoSuffix");
    
    if (seoSiloName != null) {
      ePageModel.setSeoSiloName(seoSiloName);
    }
    if (seoCategoryName != null) {
      ePageModel.setSeoCategoryName(seoCategoryName);
    }
    if (seoDesignerName != null) {
      ePageModel.setSeoDesignerName(seoDesignerName);
    }
    if (seoSuffix != null) {
      ePageModel.setSeoSuffix(seoSuffix);
    }
    
    if (sale != null && sale.equalsIgnoreCase("s")) {
      ePageModel.setSaleQuery(true);
    }
    
    if (StringUtilities.isNotEmpty(eFrom)) {
      ePageModel.setFromParam(eFrom);
    }
    
    final String explicitParentId = etPageDefinition.getExplicitParentId();
    if (StringUtils.isNotEmpty(explicitParentId)) {
      final NMCategory categoryItem = getCategoryHelper().getNMCategory(explicitParentId, null);
      if (categoryItem != null) {
        ePageModel.setCategory(categoryItem);
        request.setParameter("endecaParentId", explicitParentId);
      }
    } else {
      final NMCategory category = getCategory();
      if (category != null) {
        ePageModel.setCategory(category);
        request.setParameter("endecaParentId", category.getId());
      }
    }
    
    if (pageDefinition.getId().equals("EventDesignerIndexE") || isEventDI) {
      final String eId = request.getParameter("eId");
      searchParameters.setEid(eId);
      // INT-684 to apply the product restrictions to endeca query
      searchParameters.setUserSelectedCountryCode(getProfile().getCountryPreference());
      String eventDesignerAdditionalQuery = "";
      String eventDesignerAltAdditionalQuery = "";
      if (StringUtilities.isNotEmpty(eId)) {
        if (pageDefinition.getId().equals("EventDesignerIndexE")) {
          eventDesignerAdditionalQuery = CommonComponentHelper.getNMSearchPages().getEventDIQueryString() + "&Nr=AND(TREE:" + eId + ",IS_PRIMARY:1)";
        } else {
          if (sale != null && sale.equalsIgnoreCase("s")) {
            eventDesignerAltAdditionalQuery = CommonComponentHelper.getNMSearchPages().getEventDIQueryStringLive() + "&Nr=AND(TREE:" + eId + ",IS_PRIMARY:1)";
            eventDesignerAdditionalQuery = CommonComponentHelper.getNMSearchPages().getEventDIQueryStringLive() + "&Nr=AND(AND(OR(TREE:SALE,TREE:CLEARANCE),TREE:" + eId + "),IS_PRIMARY:1)";
          } else {
            eventDesignerAdditionalQuery = CommonComponentHelper.getNMSearchPages().getEventDIQueryStringLive() + "&Nr=AND(TREE:" + eId + ",IS_PRIMARY:1)";
            eventDesignerAltAdditionalQuery = CommonComponentHelper.getNMSearchPages().getEventDIQueryStringLive() + "&Nr=AND(AND(OR(TREE:SALE,TREE:CLEARANCE),TREE:" + eId + "),IS_PRIMARY:1)";
          }
        }
        searchParameters.setAdditionalQueryString(eventDesignerAdditionalQuery);
        searchParameters.setAltAdditionalQueryString(eventDesignerAltAdditionalQuery);
      }
      if (pageDefinition.getId().equals("EventDesignerIndexE")) {
        final String queryString = getQueryString(etPageDefinition) + "&from=eventdi";
        searchParameters.setQueryString(queryString);
      }
      
    }
    searchParameters.setSaleQuery(ePageModel.isSaleQuery());
    
    final String wFallUrl = request.getParameter("wfall");
    ePageModel.setwFallUrl(wFallUrl);
    
    final EndecaQueryHelper eQuery = (EndecaQueryHelper) request.resolveName("/nm/search/endeca/EndecaQueryHelper");
    // BrandSpecs brandSpecs = getBrandSpecs();
    boolean wfEnabled = brandSpecs.getEnableWaterfall();
    if (StringUtils.isNotEmpty(wFallUrl)) {
      wfEnabled = Boolean.parseBoolean(wFallUrl);
    }
    boolean redirects = true;
    final String redUrl = request.getParameter("redirect");
    if (StringUtils.isNotEmpty(redUrl) || StringUtils.isNotEmpty(wFallUrl)) {
      redirects = Boolean.parseBoolean(redUrl);
    }
    
    if (log.isLoggingDebug()) {
      log.logDebug(">>> Debug Designer Name - EndecaPageEvaluator line 433 - startDebugDesignerName");
    }
    final long startTime = System.currentTimeMillis();
    EndecaQueryHelper.EndecaQueryOutput output = eQuery.query(searchParameters, wfEnabled, redirects, wFallUrl);
    if (log.isLoggingDebug()) {
      log.logDebug(">>> EPH WFSearch - callTime: " + (System.currentTimeMillis() - startTime) + "ms");
      
      log.logDebug(">>> Debug Designer Name - EndecaPageEvaluator line 439 - using searchResult ID " + System.identityHashCode(output.getSearchResult()));
    }
    if (isBRSEnabled() && !isSaleDI && (defID.equals("ES") || defID.equals("ESs"))) {
      wfEnabled = false; // do not use waterfall for BR
      ePageModel.setBREnabled(true);
    } else {
      ePageModel.setBREnabled(false);
    }
    
    if (output.isError()) {
      ePageModel.setError(output.isError());
      return true;
    }
    
    if (wfEnabled && output.getParameters() != null) {
      searchParameters = output.getParameters();
      ePageModel.setSaleQuery(searchParameters.getSaleQuery());
    }
    
    if (output.getSearchResult().getTotalRecordCount() == 0) {
      
      if (ePageModel.isSaleQuery() && displaySaleNav) {
        // If this is a search with "sale" in the search string, but no sale items were returned, do
        // the search again on non-sale items.
        searchParameters.setAdditionalQueryString(((EndecaTemplatePageDefinition) pageDefinition).getNonSaleQueryString());
        searchParameters.setSaleQuery(false);
        searchParameters.setGenerateSaleNav(false);
        ePageModel.setSaleQuery(false);
        
        output = eQuery.query(searchParameters, wfEnabled, redirects, wFallUrl);
      }
    }
    
    final NMSearchResult sResult = output.getSearchResult();
    if (sResult == null) {
      return true;
    }
    
    if (redirects) {
      if (keywordRedirect(sResult)) {
        return false;
      }
      if (zoneRedirect(sResult, etPageDefinition.getSearchType(), etPageDefinition.getUseMainSearchType())) {
        return false;
      }
      if (depictionRedirect(sResult)) {
        return false;
      }
    }
    
    // Second search result for facetList built from baseline N values--those which are not selectable by the user through the filter interface, such as 'Category'.
    NMSearchResult baselineResults = null;
    
    /************************************** START - BR SEARCH ******************************************/
    if (isBRSEnabled() && !isSaleDI && (defID.equals("ES") || defID.equals("ESs"))) {
      final String queryString = searchParameters.getQueryString();
      searchParameters = useBRSearchValues(searchParameters, sTerm, sResult, eQuery, displaySaleNav, ePageModel, ePageModel.isSaleQuery(), pageDefinition, etPageDefinition);
      searchParameters.setQueryString(queryString);
    } else if (isMyFavPage) {
      final String queryString = searchParameters.getQueryString();
      searchParameters = NMSearchUtil.useFavoriteSearchValues(null, queryString, searchParameters, sResult, eQuery, displaySaleNav, ePageModel);
      // searchParameters.setQueryString(queryString);
    } else {
      ePageModel.setHasBRResults(false);
      final Map<String, Object> spMap = searchParameters.getParameterMap();
      spMap.remove("Ntx");
      searchParameters.setParameterMap(spMap);
      ePageModel.setSearchResult(sResult);
    }
    /************************************** END- BR SEARCH ******************************************/
    
    if (etPageDefinition.getAlternateAjaxSearchMethod().equals("getFilteredEndecaResult")) { // If using ajax on search page.
    
      String queryString = searchParameters.getQueryString();
      String searchString = searchParameters.getSearchString();
      
      final String acTerm = ePageModel.getSearchResult().getAutoCorrectedTerm();
      if (StringUtils.isNotEmpty(acTerm) && StringUtils.isNotEmpty(sTerm)) {
        queryString = queryString.replace(sTerm, acTerm);
        searchString = searchString.replace(sTerm, acTerm);
      }
      
      if (!ePageModel.isHasBRResults()) {
        wfEnabled = brandSpecs.getEnableWaterfall();
      }
      
      String nVals = NMSearchUtil.kvURLGrabber(queryString, "N", false, true);
      if (nVals != null) {
        nVals = nVals.trim().replace(" ", "+");
        if (log.isLoggingDebug()) {
          log.logDebug(">>> Debug Designer Name - EndecaPageEvaluator line 530 - stripFilterableRefinements ");
        }
        queryString = NMSearchUtil.replaceById(queryString, "N", NMSearchUtil.stripFilterableRefinements(nVals, fixedDimensionIds));
      }
      
      nVals = NMSearchUtil.kvURLGrabber(searchString, "N", false, true);
      if (nVals != null) {
        nVals = nVals.trim().replace(" ", "+");
        if (log.isLoggingDebug()) {
          log.logDebug(">>> Debug Designer Name - EndecaPageEvaluator line 537 - stripFilterableRefinements ");
        }
        searchString = NMSearchUtil.replaceById(searchString, "N", NMSearchUtil.stripFilterableRefinements(nVals, fixedDimensionIds));
      }
      
      searchParameters.setQueryString(queryString);
      searchParameters.setSearchString(searchString);
      if (log.isLoggingDebug()) {
        log.logDebug(">>> Debug Designer Name - EndecaPageEvaluator line 544 - start baselineResults ");
      }
      baselineResults = eQuery.query(searchParameters, wfEnabled, redirects, wFallUrl).getSearchResult();
      if (log.isLoggingDebug()) {
        log.logDebug(">>> Debug Designer Name - EndecaPageEvaluator line 546 - baselineResults using searchResult ID " + System.identityHashCode(baselineResults));
      }
    }
    
    if (etPageDefinition.getAllowedCenterZoneDims() != null) {
      ePageModel.getSearchResult().setAllowedCenterZoneDims(Arrays.asList(etPageDefinition.getAllowedCenterZoneDims()));
    }
    
    final String userTemplatePath = (String) this.getProfile().getPropertyValue("preferredEndecaTemplate");
    if (!NmoUtils.isEmpty(userTemplatePath)) {
      ePageModel.setTemplatePath(userTemplatePath);
    } else {
      ePageModel.setTemplatePath(etPageDefinition.getTemplatePath());
    }
    
    if (!(isBRSEnabled() && ePageModel.getHasBRResults()) || isSaleDI) {
      if (displaySaleNav) {
        ePageModel.setAltResult(output.getAltSearchResult());
        ePageModel.setSaleRecordCount(output.getSaleRecordCount());
      }
      ePageModel.setZeroDimRecordCount(output.getZeroDimRecordCount());
    }
    
    // Removing the eqlFilter parameter because this query string ends up in the urls of the breadcrumb
    urlQueryString = searchParameters.getQueryString().replaceFirst(Pattern.quote(NmoUtils.coalesce(eQLFilter, "")), "");
    urlQueryString = NMSearchUtil.replaceById(urlQueryString, "Ntt", sTerm).replace(" ", "+");
    ePageModel.setQueryString(urlQueryString);
    urlQueryString = NMSearchUtil.replaceById(urlQueryString, "Ntt", sTerm).replace(" ", "+");
    ePageModel.setQueryStringOutput(urlQueryString);
    ePageModel.setOmnitureException(output.getOmnitureException());
    ePageModel.setAdditionalQueryString(searchParameters.getAdditionalQueryString());
    ePageModel.setSearchTerm(URLDecoder.decode(sTerm, "UTF-8"));
    
    // invoke debug template only if the visual debug option is on
    if (getBrandSpecs().isVisualDebug()) {
      final JspPageInvoker invoker = new JspPageInvoker();
      final Map<String, Object> params = new java.util.HashMap<String, Object>();
      params.put("debugList", output.getDebugInfo());
      ePageModel.setDebugInfo(invoker.invokePage("/page/etemplate/debug/debugSearchOverview.jsp", params, request, getResponse()));
      
      final ListIterator<NMSearchRecord> records = ePageModel.getSearchResult().getSearchRecordList().listIterator();
      while (records.hasNext()) {
        final NMSearchRecord record = records.next();
        final Map<String, Object> debugMap = NMSearchUtil.getDebugMap(record);
        record.setDebugInfo(invoker.invokePage("/page/etemplate/debug/debugSearchRecord.jsp", debugMap, request, getResponse()));
      }
    }
    if (null != getCategory() && etPageDefinition.getEnableZEShot()) {
      final List<NMSearchRecord> records = ePageModel.getSearchResult().getSearchRecordList();
      for (final NMSearchRecord record : records) {
        record.setThumbImgShot(getCategory().getThumbImgShot());
        if (log.isLoggingDebug()) {
          log.logDebug("EndecaPageEvaluator line590 - thumbImgShot:" + getCategory().getThumbImgShot());
        }
      }
    }
    // build up new search parameter object for refinement requests
    searchParameters = etPageDefinition.getSearchParameters();
    if (searchParameters == null) {
      searchParameters = NMSearchUtil.getDefaultSearchParameters();
    }
    if (defID.equalsIgnoreCase("ES") || defID.equalsIgnoreCase("ESs")) {
      searchParameters.setSearchPage(true);
    }
    // set request parameters before calling NMCatalogHistoryCollector in
    // super, to load the parameterMap correctly for the NMShoppableClick
    searchParameters.setQueryString(getQueryString(etPageDefinition));
    searchParameters.setAdditionalQueryString(getAdditionalQueryString(etPageDefinition));
    searchParameters.setExposedDimensions(etPageDefinition.getExposedDimensions());
    searchParameters.setSecondarySorts(etPageDefinition.getSecondarySorts());
    searchParameters.setEid(request.getParameter("eId"));
    searchParameters.setProfileCampaigns(getProfile().getProfileCampaigns());
    searchParameters.setBuildTree(true);
    if (log.isLoggingDebug()) {
      log.logDebug(">>> Debug Designer Name - EndecaPageEvaluator line 610 - buildRefinements");
    }
    buildRefinementResults(searchParameters, ePageModel, eQuery);
    initializeNavigationObject(ePageModel, ePageModel.getSearchResult());
    setDimensionControls();
    setOmnitureVars(etPageDefinition, ePageModel);
    setUserViewChoicePref(etPageDefinition);
    
    ePageModel.setEndecaCategory(this.getEndecaCategory(ePageModel.getSearchResult()));
    
    // set this terrible parameter for the search box :(
    request.setParameter("queryString", urlQueryString);
    
    // paging
    final String pageSize = getPageSize(etPageDefinition);
    if (!(pageSize == null || pageSize.equals(""))) {
      ePageModel.setPaging(EndecaDrivenUtil.isPaging(ePageModel.getSearchResult(), Integer.parseInt(pageSize)));
      if (ePageModel.isPaging()) {
        final Long totalRec = new Long(ePageModel.getSearchResult().getTotalRecordCount());
        ePageModel.setPagination(GenericSearchUtil.generatePagination(getRequestedPageOffset(request, etPageDefinition) + 1, totalRec.intValue(), Integer.parseInt(pageSize)));
      }
      
      ePageModel.setPageSize(pageSize);
    }
    
    // for FacetList, filters
    FacetInfo facetInfo = populateFacetInfo(etPageDefinition, ePageModel.getSearchResult(), catId, isEventDI, isOnline, suppressDesignerFilter);
    final String facetInfoJSON = XStreamUtil.getInstance().convertObjectToJson(facetInfo);
    
    // String brSTestGroup = AbTestHelper.getAbTestValue(ServletUtil.getCurrentRequest(), AbTestHelper.BLOOMREACH_SEARCH_GROUP);
    // if (brSTestGroup == null || StringUtilities.areNotEqual(brSTestGroup, "BRS") ) {
    
    if (baselineResults != null && etPageDefinition.getAlternateAjaxSearchMethod().equals("getFilteredEndecaResult")) {
      // This facetInfo is pulled from a second result set "baselineResults" that represents the available filter options without
      // any of selected refinements on the page considered.
      // It is only used to populate the check boxes in the filter drawers. Without this the filter options won't generate correctly
      // after a user filters and travels through the left navigation because the filter options won't have been generated.
      facetInfo = populateFacetInfo(etPageDefinition, baselineResults, catId, isEventDI, isOnline, suppressDesignerFilter);
      ePageModel.setFacetList(EndecaDrivenUtil.getEndecaDriveFacetsAsList(catId, facetInfo.getFacets(), EndecaDrivenUtil.FILTER_FACET_BLACK_LIST_SEARCH));
      ePageModel.setRefinedFacetInfoJSON(facetInfoJSON);
    }
    // }
    
    // Empty list to avoid the blacklist for LC, need this to get color filter to work.
    // See about removing blacklist in the future.
    String refinements = rebuildRefinements(ePageModel.getSearchResult(), new ArrayList());
    if (refinements != null && refinements.length() > 1) {
      if (refinements.charAt(0) == '+') {
        refinements = refinements.substring(1);
      }
    }
    ePageModel.setRefinements(refinements);
    final List<String> selectedRefinements = buildSelectedRefinements(ePageModel.getSearchResult(), new ArrayList());
    ePageModel.setSelectedRefinements(selectedRefinements);
    
    final String sort = NMSearchUtil.kvURLGrabber(searchParameters.getQueryString(), "Ns");
    
    Boolean isRWD = etPageDefinition.getIsRWD();
    
    if (isRWD == null || isRWD == false) {
      isRWD = new Boolean(false);
    }
    
    if (StringUtilities.isNotEmpty(sort) && etPageDefinition.getFlagRetainSortsOnNav().equalsIgnoreCase("true")) {
      setSorts(ePageModel, isKeywordSearch, isEventDI, sort, isRWD.booleanValue());
    } else {
      setSorts(ePageModel, isKeywordSearch, isEventDI, isRWD.booleanValue());
    }
    
    // specific for WN
    final NMCategory activeCategory = super.getPageModel().getActiveCategory();
    String categoryGroup = "";
    if (activeCategory != null) {
      // applyCategory();
      categoryGroup = AbTestHelper.getAbTestValue(request, AbTestHelper.GROUP_NAME_KEY);
      if (categoryGroup != null) {
        getCategoryFilterCode().setFilterCode(categoryGroup);
      }
      activeCategory.setFilterCategoryGroup(getFilterCategoryGroup());
      // ePageModel.getCategory().setFilterCategoryGroup(getFilterCategoryGroup());
    }
    // if("WN".equals(systemCode) || "WB".equals(systemCode)) {
    final SaleDesignerIndexer indexer = new SaleDesignerIndexer();
    if (categoryGroup == null || categoryGroup.trim().equals("")) {
      categoryGroup = "#";
    }
    indexer.setCategoryList(ePageModel.getSearchResult().getFullTreeList());
    if (ePageModel.getSearchResult().getDesignerDimension() != null) {
      indexer.setSaleDesigners(ePageModel.getSearchResult().getDesignerDimension().getDimensionValues());
      indexer.setAlphaHeaders();
      indexer.setAlphaDesignerListMap(indexer.getAlphaHeaders(), indexer.getSaleDesigners());
      categoryGroup = categoryGroup.toUpperCase();
      ePageModel.setAlphaHeaderList(indexer.getAlphaHeaders());
      ePageModel.setEmptyAlphaList(indexer.getEmptyAlphaList());
      ePageModel.setAlphaDesignerListMap(indexer.getAlphaDesignerListMap());
      ePageModel.setSaleDesignerCategories(indexer.getCategoryList());
    }
    // }
    ePageModel.setPageDefinition(pageDefinition);
    setFilterAndSortValues(ePageModel, refinements, selectedRefinements);
    final TMSDataDictionaryUtils dataDictionaryUtils = CommonComponentHelper.getDataDictionaryUtils();
    dataDictionaryUtils.processTMSDataDictionaryAttributes(pageDefinition, null, nmProfile, ePageModel, tmsMessageContainer);
    // set ILink data for Endeca Search Page
    ePageModel.setDisplayILinkData(CommonComponentHelper.getTemplateUtils().isDisplayILinkData());
    return true;
  }
  
  /**
   * This method sets the selected filter type, filter selections and sort method into the page model
   *
   * @param ePageModel
   * @param refinements
   * @param selectedRefinements
   */
  private void setFilterAndSortValues(final EndecaDrivenPageModel ePageModel, final String refinements, final List<String> selectedRefinements) {
    /* Identifies the filter and their selections */
    final List<String> filterType = new ArrayList<String>();
    String currentSort = INMGenericConstants.EMPTY_STRING;
    if (null != ePageModel.getCurrentSort()) {
      currentSort = ePageModel.getCurrentSort().getValue();
    }
    final List<String> filterSelections = new ArrayList<String>();
    final List<FacetDimension> facetList = ePageModel.getFacetList();
    if (StringUtilities.isNotEmpty(refinements) && NmoUtils.isNotEmptyCollection(facetList)) {
      final List<String> refinementsList = selectedRefinements;
      for (final FacetDimension facetDimension : facetList) {
        for (final FacetDimensionGroup facetDimensionGroup : facetDimension.getCategories()) {
          for (final FacetDimensionValue facetDimensionValue : facetDimensionGroup.getValues()) {
            if (refinementsList.contains(facetDimensionValue.getId())) {
              filterType.add(facetDimensionGroup.getName());
              filterSelections.add(facetDimensionValue.getName());
            }
          }
        }
      }
    }
    ePageModel.setFilterType(filterType);
    ePageModel.setFilterSelections(filterSelections);
    ePageModel.setSortMethod(EndecaDrivenUtil.getCurrentSortValue(ePageModel, currentSort, SORT_VALUE_END, false));
  }
  
  private SearchParameters useBRSearchValues(final SearchParameters searchParameters, final String sTerm, final NMSearchResult sResult, final EndecaQueryHelper eQuery, final Boolean displaySaleNav,
          final EndecaDrivenPageModel ePageModel, final Boolean isSaleSearch, final PageDefinition pageDefinition, final EndecaTemplatePageDefinition etPageDefinition) throws Exception {
    final String NRSVALUE = "nRSValue";
    final String NSVALUE = "nSValue";
    String brUid2 = "";
    String brSearchTerm = sTerm;
    // brSearchTerm = URLEncoder.encode(sTerm, "UTF-8"); //--seems to be giving black%2Bdress %2B instead of space for plus
    // brSearchTerm = URLEncoder.encode(sTerm, "UTF-8").replaceAll("\\+", " "); //--seems to be giving black%2Bdress %2B instead of space or plus
    brSearchTerm = brSearchTerm.replaceAll("\\+", " "); // if we leave in the plus between words, it will dramatically change the output from BRS. like from 200 to 19 returns
    brSearchTerm = URLDecoder.decode(brSearchTerm, "UTF-8");
    
    final DynamoHttpServletRequest request = getRequest();
    // get all params for bloomreach api call:
    final String userAgent = request.getHeader("User-Agent");
    String userIP = "";
    
    if (request.getHeader("nmcip") != null && !"".equals(request.getHeader("nmcip"))) {
      userIP = NmoUtils.invalidCharacterCleanup(request.getHeader("nmcip"));
    } else {
      userIP = NmoUtils.invalidCharacterCleanup(request.getRemoteAddr());
    }
    
    if (sResult.getAutoCorrectedTerm() != null) {
      brSearchTerm = sResult.getAutoCorrectedTerm();
    }
    
    final String refUrl = request.getHeader("referer");
    brUid2 = NMCookieManager.getCookieValue(request.getCookies(), "_br_uid_2");
    final Map<String, String> brMCookies = new java.util.HashMap<String, String>();
    
    // Need to get all cookies matching the regular expression: _br_m.*
    final String brCookiePrefix = "_br_m";
    final Cookie[] cookies = request.getCookies();
    
    String cookieName = "";
    String cookieValue = "";
    
    if (cookies != null) {
      for (final javax.servlet.http.Cookie cookie : request.getCookies()) {
        
        if (cookie.getName().startsWith(brCookiePrefix)) {
          cookieName = cookie.getName();
          cookieValue = cookie.getValue();
          final BasicClientCookie newCookie = new BasicClientCookie(cookieName, cookieValue);
          newCookie.setPath("/");
          brMCookies.put(cookieName, cookieValue);
        }
      }
    }
    
    // System.out.println("--->> BR stuff:-"+brSearchTerm+"-:-"+translatedSTerm+"-:-"+userAgent+"-:-"+userIP+"-:-"+url+"-:-"+refUrl+"-:-"+encodedbrUid2+"-:-"+brMCookies );
    final BRSessionData brSessionData = (BRSessionData) request.resolveName("/nm/utils/BRSessionData");
    
    if (brSessionData != null) {
      HashMap<String, String> brSProducts = new HashMap<String, String>();
      brSProducts = brSessionData.getDataFromBloomReach(brSearchTerm, "", userAgent, userIP, refUrl, brUid2, brMCookies);
      
      // /addd this as a pagemodel example
      ePageModel.setBRSCalled(true);
      // if brProducts is null--None found--
      // then run search with WF TRUE
      
      if (brSProducts != null) {
        final SearchParameters sParams = new SearchParameters(searchParameters);
        String nrsvalues = brSProducts.get(NRSVALUE).toString();
        final String nsvalues = brSProducts.get(NSVALUE).toString();
        
        final HashMap<String, Object> brNRS = new HashMap<String, Object>();
        // add the new nSValue from BRSessionData also
        brNRS.put("BRSearch", new Boolean(true));
        
        String qVal = NMSearchUtil.removeParameter(searchParameters.getQueryString(), "Ntt");
        if (qVal.contains("Nrs=")) {
          String nrsQVal = NMSearchUtil.kvURLGrabber(searchParameters.getQueryString(), "Nrs");
          qVal = NMSearchUtil.removeParameter(searchParameters.getQueryString(), "Nrs");
          
          nrsQVal = nrsQVal.substring(20, nrsQVal.length() - 1);
          nrsvalues = nrsvalues.substring(20, nrsvalues.length() - 1);
          brNRS.put("Nrs", "collection()/record[(" + nrsQVal + ")and(" + nrsvalues + ")]");
        } else {
          brNRS.put("Nrs", nrsvalues);
        }
        
        if (!qVal.contains("Ns=")) {
          sParams.setEndecaStratifyString(nsvalues);
        }
        
        sParams.setSearchString(qVal);
        sParams.setQueryString(qVal);
        sParams.setParameterMap(brNRS);
        
        String nrVal = NMSearchUtil.kvURLGrabber(searchParameters.getAdditionalQueryString(), "Nr");
        sParams.setAdditionalQueryString("Nr=" + nrVal);
        if (StringUtils.isNotEmpty(searchParameters.getAltAdditionalQueryString())) {
          nrVal = NMSearchUtil.kvURLGrabber(searchParameters.getAltAdditionalQueryString(), "Nr");
          sParams.setAltAdditionalQueryString("Nr=" + nrVal);
        }
        sParams.setBuildTree(searchParameters.getBuildTree());
        sParams.setGenerateSaleNav(searchParameters.getGenerateSaleNav());
        sParams.setExposeAllDimensions(searchParameters.getExposeAllDimensions());
        
        final long start2 = System.currentTimeMillis();
        final EndecaQueryOutput brOutput = eQuery.query(sParams); // /false turns off waterfall for BR search, only use wnts or wnsts
        
        log.logWarning(">>> BRSearch - Endeca returnTime with BR PRODS: " + (System.currentTimeMillis() - start2) + "ms");
        
        final NMSearchResult brResult = brOutput.getSearchResult();
        
        brResult.setAutoCorrectedTerm(sResult.getAutoCorrectedTerm());
        brResult.setBreadCrumbOveride(true);
        brResult.setSupplementsList(sResult.getSupplementsList());
        
        ePageModel.setSearchResult(brResult);
        ePageModel.setHasBRResults(true);
        
        if (displaySaleNav) {
          ePageModel.setAltResult(brOutput.getAltSearchResult());
          ePageModel.setSaleRecordCount(brOutput.getSaleRecordCount());
        }
        ePageModel.setZeroDimRecordCount(brOutput.getZeroDimRecordCount());
        
        return sParams;
      } else {
        // BR SOCKET Timed out Here
        // then run ENDECA search with WF TRUE
        // CHECK DEFAULT WFENABLED HERE
        log.logWarning(">> EPE BR SOCKET  Timed out,running endecaSearch now on term:" + brSearchTerm);
        runEndecaSearch(searchParameters, eQuery, displaySaleNav, ePageModel, isSaleSearch, pageDefinition, sResult);
      }
    } else {
      // BR CLIENT CONNECTION Timed out Here
      // then run ENDECA search with WF TRUE
      // CHECK DEFAULT WFENABLED HERE
      log.logWarning(">> EPE BR CLIENT CONNECTION  Timed out,running endecaSearch now");
      runEndecaSearch(searchParameters, eQuery, displaySaleNav, ePageModel, isSaleSearch, pageDefinition, sResult);
    }
    
    return searchParameters;
  }
  
  private void runEndecaSearch(SearchParameters searchParameters, final EndecaQueryHelper eQuery, final Boolean displaySaleNav, final EndecaDrivenPageModel ePageModel, final Boolean isSaleSearch,
          final PageDefinition pageDefinition, final NMSearchResult sResult) throws ServletException, IOException {
    
    final BrandSpecs brandSpecs = getBrandSpecs();
    final boolean wfEnabled = brandSpecs.getEnableWaterfall();
    searchParameters.setParameterMap(null);
    searchParameters.setSearchString(null);
    
    EndecaQueryHelper.EndecaQueryOutput output = eQuery.query(searchParameters, wfEnabled);
    
    if (output.isError()) {
      ePageModel.setError(output.isError());
      return;
    }
    
    if (wfEnabled && output.getParameters() != null) {
      searchParameters = output.getParameters();
      ePageModel.setSaleQuery(searchParameters.getSaleQuery());
    }
    
    if (output.getSearchResult().getTotalRecordCount() == 0) {
      
      if (isSaleSearch && displaySaleNav) {
        // If this is a search with "sale" in the search string, but no sale items were returned, do
        // the search again on non-sale items.
        searchParameters.setAdditionalQueryString(((EndecaTemplatePageDefinition) pageDefinition).getNonSaleQueryString());
        searchParameters.setSaleQuery(false);
        searchParameters.setGenerateSaleNav(false);
        ePageModel.setSaleQuery(false);
        output = eQuery.query(searchParameters);
      }
    }
    ePageModel.setZeroDimRecordCount(output.getZeroDimRecordCount());
    final NMSearchResult bypassResult = output.getSearchResult();
    
    if (displaySaleNav) {
      ePageModel.setAltResult(output.getAltSearchResult());
      ePageModel.setSaleRecordCount(output.getSaleRecordCount());
    }
    ePageModel.setHasBRResults(false);
    sResult.setENEQueryResults(bypassResult.getENEQueryResults());
    sResult.setFullTreeList(bypassResult.getFullTreeList());
    ePageModel.setSearchResult(sResult);
  }
  
  private boolean isBRSEnabled() {
    if (ComponentUtils.getInstance().getSystemSpecs().isBrsEnabled()) {
      final String brSTestGroup = AbTestHelper.getAbTestValue(ServletUtil.getCurrentRequest(), AbTestHelper.BLOOMREACH_SEARCH_GROUP);
      if (brSTestGroup != null && StringUtilities.areEqual(brSTestGroup, "BRS")) {
        return true;
      }
    }
    return false;
  }
  
  private static String rebuildRefinements(final NMSearchResult sResult, final List<String> excludes) {
    final List<Dimension> descriptors = getDescriptorDimensions(sResult.getNavigation());
    final Iterator<Dimension> iDims = descriptors.iterator();
    String refinements = "";
    while (iDims.hasNext()) {
      final Dimension dim = iDims.next();
      final String myType = dim.getRoot().getDimensionName();
      if (!(excludes.contains(myType.toUpperCase()) || excludes.contains(myType.toLowerCase()))) {
        final String dimID = String.valueOf(dim.getDescriptor().getId());
        if (refinements.indexOf(dimID) == -1) {
          refinements += "+" + dimID;
        }
      }
    }
    return refinements;
  }
  
  /**
   * This method builds the selected refinements list and also excludes the "Category" refinement from the generated refinements
   *
   * @param sResult
   * @param excludes
   * @return refinements excluding "Category"
   */
  private static List<String> buildSelectedRefinements(final NMSearchResult sResult, final List<String> excludes) {
    final List<Dimension> descriptors = getDescriptorDimensions(sResult.getNavigation());
    final List<String> refinements = new ArrayList<String>();
    for (final Dimension dim : descriptors) {
      final String myType = dim.getRoot().getDimensionName();
      if (!(excludes.contains(myType.toUpperCase()) || excludes.contains(myType.toLowerCase())) && !CATEGORY.equalsIgnoreCase(myType)) {
        final String dimID = String.valueOf(dim.getDescriptor().getId());
        refinements.add(dimID);
      }
    }
    return refinements;
  }
  
  @SuppressWarnings("unchecked")
  private static List<Dimension> getDescriptorDimensions(final Navigation navigation) {
    return navigation.getDescriptorDimensions();
  }
  
  private FacetInfo populateFacetInfo(final EndecaTemplatePageDefinition pageDefinition, final NMSearchResult searchResult, final String categoryId, 
    final boolean isEventDI, final boolean isOnline, final boolean suppressDesignerFilter) throws Exception {
    FacetInfo facetInfo = null;
    if (searchResult != null) {
      // establish facet collections for result set
      final BrandSpecs brandSpecs = CommonComponentHelper.getBrandSpecs();
      final Facets facets = new Facets();
      final String endecaColorFilter = brandSpecs.getEndecaColorFilterAttribute().trim();
      final String endecaSizeFilter = brandSpecs.getEndecaSizeFilterAttribute().trim();

      String priceDim = EndecaDrivenUtil.PROMO_PRICE_DIM1;
      if (ComponentUtils.getInstance().getSystemSpecs().getProductionSystemCode().trim().equalsIgnoreCase("LC")) {
        priceDim = EndecaDrivenUtil.PROMO_PRICE_DIM2;
      }
      
      // Take the filter/attribute from BrandSpecs.properties else use the default
      List<FacetDimensionGroup> dGroups = EndecaDrivenUtil.buildDimensionGroups(endecaColorFilter, categoryId, searchResult, searchResult);
      
      if (dGroups != null) {
        final FacetDimension fDim = new FacetDimension();
        fDim.setCategories(dGroups);
        facets.setCOLOR(fDim);
      }
      
      dGroups = EndecaDrivenUtil.buildDimensionGroups(endecaSizeFilter, categoryId, searchResult, searchResult);
      
      if (dGroups != null) {
        final FacetDimension fDim = new FacetDimension();
        fDim.setCategories(dGroups);
        facets.setSIZE(fDim);
      }
      
      if (!suppressDesignerFilter) {
        dGroups = EndecaDrivenUtil.buildDimensionGroups(EndecaDrivenUtil.DESIGNER, categoryId, searchResult, searchResult);
 
        if (dGroups != null && dGroups.size() > 0 && brandSpecs.isEnableMyFavorites() && brandSpecs.isEnableFilterByFavoriteDesigners()) {
          EndecaDrivenUtil.addFavoriteDesignersFacet(dGroups);
        }

        if (dGroups != null) {
          final FacetDimension fDim = new FacetDimension();
          fDim.setCategories(dGroups);
          facets.setDESIGNER(fDim);
        }
      }

      dGroups = EndecaDrivenUtil.buildDimensionGroups(priceDim, categoryId, searchResult, searchResult);
      if (dGroups != null) {
        final FacetDimension fDim = new FacetDimension();
        fDim.setCategories(dGroups);
        facets.setPRICE(fDim);
      }
      
      if (pageDefinition.getInStoreAvailability()) {
        dGroups = EndecaDrivenUtil.buildDimensionGroups(EndecaDrivenUtil.IN_STORE, categoryId, searchResult, searchResult);
        if (dGroups != null) {
          final FacetDimension fDim = new FacetDimension();
          fDim.setCategories(dGroups);
          facets.setIN_STORE(fDim);
        }
      }
      
      // Removed the if block for isEventDI for story NMOBLDS-832:private event template.
      if (isOnline) {
        dGroups = EndecaDrivenUtil.buildDimensionGroupsForOnline(EndecaDrivenUtil.IN_STORE, categoryId, searchResult, searchResult);
        
        if (dGroups != null) {
          final FacetDimension fDim = new FacetDimension();
          fDim.setCategories(dGroups);
          facets.setIN_STORE(fDim);
        }
      }
      
      facetInfo = new FacetInfo();
      facetInfo.setFacets(facets);
      facetInfo.setTotalProducts(searchResult.getTotalRecordCount());
    }
    
    return facetInfo;
  }
  
  private int getRequestedPageOffset(final DynamoHttpServletRequest request, final EndecaTemplatePageDefinition etPageDefinition) {
    int requestedPageOffset = 0;
    final Boolean isFromBreadCrumb = Boolean.valueOf(request.getParameter("breadcrumb"));
    
    if (isFromBreadCrumb) {
      final String requestedNo = request.getParameter("No");
      final int pageSize = Integer.parseInt(getPageSize(etPageDefinition));
      requestedPageOffset = Integer.valueOf(requestedNo) / pageSize;
    }
    return requestedPageOffset;
  }
  
  private void setUserViewChoicePref(final EndecaTemplatePageDefinition etPageDefinition) {
    final String pageSize = getNMSearchSessionData().getPageSize();
    if (getRequest().getParameter("viewClick") != null && pageSize != null) {
      if (pageSize.equals(etPageDefinition.getDefaultPageSize())) {
        setUserPref(etPageDefinition, getRequest(), "DEFAULT");
      } else {
        setUserPref(etPageDefinition, getRequest(), "ALL");
      }
    }
    
  }
  
  @Override
  protected PageDefinition derivePageDefinition(final PageDefinition pageDefinition, final PageModel pageModel) throws Exception {
    final EndecaDrivenPageModel ePageModel = (EndecaDrivenPageModel) pageModel;
    
    // the page definition passed in, the source of any variations.
    // pages may need to refer to this to get the original template
    // information.
    ePageModel.setOriginalPageDefinition((EndecaTemplatePageDefinition) pageDefinition);
    
    // the page definition that will be used to render the page.
    final EndecaTemplatePageDefinition etPageDefinition = (EndecaTemplatePageDefinition) pageDefinition;
    return etPageDefinition;
  }
  
  /**
   * Returns the category specified by the itemId parameter. Null is returned it the category ID or category is not found.
   *
   * @return true if the category was not found and redirect occurred.
   * @throws IOException
   */
  private NMCategory getCategory() throws IOException {
    return CategoryHelper.getInstance().getNMCategory(getRequest().getParameter("itemId"), getRequest().getParameter("parentId"));
  }
  
  /**
   * Returns the Additional Query String as passed to page or defaults value
   *
   * @return true if the category was not found and redirect occurred.
   */
  private String getAdditionalQueryString(final EndecaTemplatePageDefinition etPageDefinition) {
    final String additionalQueryString = getRequest().getParameter("additionalQueryString");
    
    if (!NmoUtils.isEmpty(additionalQueryString)) {
      return additionalQueryString;
    }
    
    return etPageDefinition.getAdditionalQueryString();
  }
  
  private String getQueryString(final EndecaTemplatePageDefinition etPageDefinition) {
    String qString = getRequest().getQueryString();
    
    if (qString != null) {
      String sTerm = NMSearchUtil.kvURLGrabber(qString, "Ntt");
      if (StringUtils.isNotEmpty(sTerm) && sTerm.toLowerCase().contains("sale")) {
        sTerm = sTerm.toLowerCase().replace("sale", "").trim();
        qString = NMSearchUtil.replaceById(qString, "Ntt", sTerm);
      }
      
      qString = NmoUtils.removeUrlParam(qString, "location");
      qString = NmoUtils.removeUrlParam(qString, "radius");
      qString = NmoUtils.removeUrlParam(qString, "allStores");
    }
    
    if (!NmoUtils.isEmpty(etPageDefinition.getQueryString())) {
      qString = etPageDefinition.getQueryString();
    }
    if (qString == null) {
      if (!NmoUtils.isEmpty(etPageDefinition.getDefaultQueryString())) {
        return etPageDefinition.getDefaultQueryString();
      }
      return "";
    }
    return qString;
  }
  
  private String getPageSize(final EndecaTemplatePageDefinition etPageDefinition) {
    final DynamoHttpServletRequest request = getRequest();
    
    String pageSize = request.getParameter("pageSize");
    final String sessionPageSize = getNMSearchSessionData().getPageSize();
        
    if (!StringUtilities.isEmpty(pageSize)) {
      int pageSizeNum = 0;
      try {
        pageSizeNum = Integer.parseInt(pageSize);
        if(pageSizeNum > 120){
          pageSize = "30";
        }
      } catch (NumberFormatException exception) {
        pageSize = "0";
      }
    }
    
    if (StringUtils.isEmpty(pageSize)) {
      if (StringUtils.isNotEmpty(sessionPageSize) && (sessionPageSize.equals(etPageDefinition.getPageSize()) || sessionPageSize.equals(etPageDefinition.getVaPageSize()))) {
        pageSize = sessionPageSize;
      } else {
        if (isPrefUserChoiceAll(etPageDefinition, getRequest(), true)) {
          pageSize = etPageDefinition.getVaPageSize();
        } else {
          pageSize = etPageDefinition.getPageSize();
        }
      }
    }
    
    final String vaTest = request.getParameter("va");
    if ("t".equals(vaTest)) {
      pageSize = etPageDefinition.getVaPageSize();
    }
    getNMSearchSessionData().setPageSize(pageSize);
    
    return pageSize;
  }
  
  private NMSearchSessionData getNMSearchSessionData() {
    return (NMSearchSessionData) getRequest().resolveName("/nm/search/endeca/NMSearchSessionData");
  }
  
  private boolean keywordRedirect(final NMSearchResult sResult) throws IOException {
    boolean isRedirected = false;
    
    if (!StringUtils.isEmpty(sResult.getKeywordRedirectURL())) {
      final DynamoHttpServletResponse response = getResponse();
      String redirectURL = sResult.getKeywordRedirectURL();
      final String searchValue = (String) sResult.getParameterMap().get("Ntt");
      if (StringUtilities.isNotEmpty(searchValue)) {
        if (redirectURL.contains("?")) {
          redirectURL = redirectURL + "&eVar6=" + searchValue;
        } else {
          redirectURL = redirectURL + "?eVar6=" + searchValue;
        }
      }
      response.sendRedirect(NMFormHelper.reviseUrl(redirectURL));
      response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
      isRedirected = true;
    }
    return isRedirected;
  }
  
  private boolean depictionRedirect(final NMSearchResult sResult) throws IOException {
    boolean isRedirected = false;
    final String searchKey = (String) sResult.getParameterMap().get("Ntk");
    
    if ("direct_match".equals(searchKey) && sResult.getTotalRecordCount() > 0) {
      final NMProduct prod;
      final PropertyMap propsMap = sResult.getSearchRecordList().get(0).getProperties();
      if (propsMap != null && propsMap.get("GROUP_ID") != null) {
        prod = new NMProduct((String) propsMap.get("GROUP_ID"));
      } else {
        prod = sResult.getSearchRecordList().get(0).getProduct();
      }
      String redirecturl = "";
      if (prod != null) {
        final DynamoHttpServletResponse response = getResponse();
        final String searchValue = (String) sResult.getParameterMap().get("Ntt");
        if (prod.getCanonicalUrl() != null) {
          String redirectURL = prod.getCanonicalUrl();
          if (StringUtilities.isNotEmpty(searchValue)) {
            if (redirectURL.contains("?")) {
              redirectURL = redirectURL + "&eVar6=" + searchValue;
            } else {
              redirectURL = redirectURL + "?eVar6=" + searchValue;
            }
          }
          response.sendRedirect(NMFormHelper.reviseUrl(redirectURL));
        } else {
          redirecturl = prod.getTemplateUrl() + "?itemId=" + prod.getId();
          if (StringUtilities.isNotEmpty(searchValue)) {
            redirecturl = redirecturl + "&eVar6=" + searchValue;
          }
          response.sendRedirect(NMFormHelper.reviseUrl(redirecturl));
        }
        response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
        isRedirected = true;
      }
    }
    
    return isRedirected;
  }
  
  private boolean zoneRedirect(final NMSearchResult sResult, final String sType, final Boolean useMainSearch) throws IOException, ServletException {
    boolean isRedirected = false;
    String typeName = "SearchRedirect";
    if (sType.equals("SALE")) {
      typeName = "SaleRedirect";
    }
    
    if (useMainSearch) {
      typeName = "MAIN";
    }
    
    if (sResult.getSupplementsList() != null) {
      
      final DynamoHttpServletRequest request = getRequest();
      final EndecaZoneLookup zoneLookup = (EndecaZoneLookup) request.resolveName("/nm/droplet/EndecaZoneLookup");
      
      final EndecaZoneLookup.EndecaZoneLookupOutput output = zoneLookup.lookup(typeName, sResult.getSupplementsList(), request);
      
      String redirectURL = output.getRedirectURL();
      if (!StringUtils.isEmpty(redirectURL)) {
        final String searchValue = (String) sResult.getParameterMap().get("Ntt");
        if (StringUtilities.isNotEmpty(searchValue)) {
          if (redirectURL.contains("?")) {
            redirectURL = redirectURL + "&eVar6=" + searchValue;
          } else {
            redirectURL = redirectURL + "?eVar6=" + searchValue;
          }
        }
        
        final DynamoHttpServletResponse response = getResponse();
        response.sendRedirect(NMFormHelper.reviseUrl(redirectURL));
        response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
        isRedirected = true;
      }
    }
    
    return isRedirected;
  }
  
  private void initializeNavigationObject(final EndecaPageModel ePageModel, final NMSearchResult sResult) {
    final ETInstrumentor eti = new ETInstrumentor();
    final Navigation nav = sResult.getNavigation();
    if (nav != null) {
      ePageModel.setNavObject(eti.htmlInstrumentNavigation(nav));
    }
  }
  
  private NMCategory setDimensionControls() throws IOException {
    final DynamoHttpServletRequest request = getRequest();
    final String rd = request.getParameter("rd");
    
    if (!NmoUtils.isEmpty(rd) && "1".equals(rd)) {
      request.setParameter("flgRemoveDimensionControlsForWeb2", "false");
    }
    
    return null;
  }
  
  private void buildRefinementResults(final SearchParameters searchParameters, final EndecaPageModel ePageModel, final EndecaQueryHelper eQuery) throws Exception {
    searchParameters.setPageSize("0");
    searchParameters.setBuildTree(false);
    searchParameters.addDimension(EndecaDrivenUtil.PROMO_PRICE_DIM1);
    searchParameters.addDimension(EndecaDrivenUtil.EDISON_COLOR);
    searchParameters.addDimension(EndecaDrivenUtil.SIZE);
    searchParameters.addDimension(EndecaDrivenUtil.DESIGNER);
    String priceDim = EndecaDrivenUtil.PROMO_PRICE_DIM1;
    if (ComponentUtils.getInstance().getSystemSpecs().getProductionSystemCode().trim().equalsIgnoreCase("LC")) {
      priceDim = EndecaDrivenUtil.PROMO_PRICE_DIM2;
    }
    searchParameters.addDimension(priceDim);
    
    final ArrayList<String> refinements = new ArrayList<String>();
    refinements.add(EndecaDrivenUtil.PROMO_PRICE_DIM1);
    refinements.add(EndecaDrivenUtil.EDISON_COLOR);
    refinements.add(EndecaDrivenUtil.SIZE);
    refinements.add(EndecaDrivenUtil.DESIGNER);
    refinements.add(priceDim);
    searchParameters.setExcludeDescriptorList(refinements);
    if (log.isLoggingDebug()) {
      log.logDebug(">>> Debug Designer Name - EndecaPageEvaluator line 1258 - start refinements");
    }
    final EndecaQueryHelper.EndecaQueryOutput output = eQuery.queryRefinements(searchParameters);
    if (!output.getRefinementResultMap().isEmpty()) {
      final Iterator it = output.getRefinementResultMap().entrySet().iterator();
      while (it.hasNext()) {
        final Map.Entry pairs = (Map.Entry) it.next();
        ePageModel.setRefinementResult((String) pairs.getKey(), (NMSearchResult) pairs.getValue());
        it.remove(); // avoids a ConcurrentModificationException
      }
    }
  }
  
  private void setRefinementResult(final SearchParameters searchParameters, final String refinementName, final EndecaPageModel ePageModel, final EndecaQueryHelper eQuery) throws Exception {
    searchParameters.setExcludeDescriptors(refinementName);
    final EndecaQueryHelper.EndecaQueryOutput output = eQuery.query(searchParameters);
    if (output.getRefinementResult() != null) {
      ePageModel.setRefinementResult(refinementName, output.getRefinementResult());
    }
  }
  
  private void setOmnitureVars(final EndecaTemplatePageDefinition etPageDefinition, final EndecaDrivenPageModel ePageModel) {
    final DynamoHttpServletRequest request = getRequest();
    
    // Set wether or not type ahead was selected
    final EndecaSearchFormHandler endecaSearchFormHandler = (EndecaSearchFormHandler) request.resolveName("/nm/formhandler/SearchFormHandler");
    
    // always set so previous value does not get sent, if it is empty then s_code will throw it away.
    final String usedTypeAheadValue = endecaSearchFormHandler.getUsedTypeAhead();
    if (StringUtilities.isNotEmpty(usedTypeAheadValue)) {
      etPageDefinition.setOmnitureProp21(usedTypeAheadValue);
    } else {
      etPageDefinition.setOmnitureProp21("NO");
    }
    // split the usedTypeAheadValue to get the used type ahead and internal search term string
    if (StringUtilities.isNotBlank(usedTypeAheadValue) && usedTypeAheadValue.contains(">")) {
        final String TypeAheadString[] = usedTypeAheadValue.split(">");
        ePageModel.setInternalSearchTerm(TypeAheadString[0]);
        etPageDefinition.setUsedTypeAhead(TypeAheadString[1]);
     } else {
        etPageDefinition.setUsedTypeAhead(null);
        ePageModel.setInternalSearchTerm(ePageModel.getSearchTerm());
     }
    // If we are coming from a designer link on an Endeca-driven Designer
    // Index page,
    // we need to set the evar4 to 'Browse' instead of 'Search'
    String priorPage = request.getHeader("referer");
    
    if (priorPage != null) {
      priorPage = priorPage.substring(priorPage.lastIndexOf("/") + 1);
      // Make sure 'Ntt' is null -- Tells us they didn't run a search from
      // the Designer Index Page.
      // Make sure 'N' is not null -- Tells us they clicked on a designer
      // link from an Endeca-driven Designer Index page.
      final String nttVal = request.getParameter("Ntt");
      final String nVal = request.getParameter("N");
      if (priorPage.startsWith("designerIndexAZ.jhtml") && nttVal == null && nVal != null) {
        etPageDefinition.setOmnitureEvar4("Browse");
        etPageDefinition.setEvar4DesignerIdxOverride("1");
      }
    }
  }
  
  public NMSearchUtil getNMSearchUtil() {
    return (NMSearchUtil) getRequest().resolveName("/nm/search/endeca/NMSearchUtil");
  }
  
  private NMCategory getEndecaCategory(final NMSearchResult searchResult) {
    NMCategory endecaCat = null;
    
    if (searchResult.getBreadCrumbDimensionList().size() > 0) {
      final NMDimension nmDim = searchResult.getBreadCrumbDimensionList().get(searchResult.getBreadCrumbDimensionList().size() - 1);
      final String endecaName = nmDim.getDescriptor().getName();
      try {
        final RepositoryItem parentCategory = getCategory();
        if (parentCategory != null) {
          final List<RepositoryItem> categoryItems = (List<RepositoryItem>) parentCategory.getPropertyValue("childCategories");
          if (categoryItems != null && endecaName != null) {
            final Iterator<RepositoryItem> i = categoryItems.iterator();
            while (i.hasNext()) {
              final RepositoryItem cat = i.next();
              if (endecaName.equalsIgnoreCase((String) cat.getPropertyValue("displayName"))) {
                endecaCat = CategoryHelper.getInstance().getNMCategory(cat, null);
                break;
              }
            }
          }
        }
      } catch (final Exception e) {
        // return null for override
      }
    }
    return endecaCat;
  }
  
  protected void setSorts(final EndecaDrivenPageModel pageModel, final boolean isKeywordSearch, final boolean isEventDI, final boolean isRWD) {
    setSorts(pageModel, isKeywordSearch, isEventDI, null, isRWD);
  }
  
  protected void setSorts(final EndecaDrivenPageModel pageModel, final boolean isKeywordSearch, final boolean isEventDI, final String sort, final boolean isRWD) {
    final ArrayList<SortOption> sortList = new ArrayList<SortOption>();
    final String RWD_PRICE_HIGH_LOW = "Price: high to low";
    final String RWD_PRICE_LOW_HIGH = "Price: low to high";
    final String RWD_DISCOUNT_HIGH_LOW = "Discount: high to low";
    final String RWD_DISCOUNT_LOW_HIGH = "Discount: low to high";
    final String RWD_NEWEST_FIRST = "Newest first";
    final String RWD_BEST_MATCH = "Best Match";
    final String RWD_IN_STOCK = "In Stock";
    final String RWD_FAV_PRODUCTS = "My Favorites";
    String pageId = "";
    pageId = pageModel.getPageDefinition().getId();
    final boolean isFavPage = pageId.equalsIgnoreCase("myfavoriteitems");
    
    if ("sortOptionsB".equals(getBrandSpecs().getAlternativeSortOptions())) { // LastCall uses these.
      if (!getBrandSpecs().isEnableMyFavorites() && !isFavPage) {
    	  sortList.add(new SortOption("FEATURED", "", ""));
      }
      sortList.add(new SortOption("PRICE LOW TO HIGH", "MAX_PROMO_PRICE", ""));
      sortList.add(new SortOption("PRICE HIGH TO LOW", "MAX_PROMO_PRICE|1", ""));
      sortList.add(new SortOption("FEATURED", "WEB1_SORT||WEB2_SORT||WEB3_SORT||WEB4_SORT", ""));
      sortList.add(new SortOption("JUST IN", "SELLABLE_DATE|1", ""));
      if (getBrandSpecs().isEnableMyFavorites()){
    	  if (!isFavPage && getBrandSpecs().isEnableSortByFavorites()) {
    		  sortList.add(new SortOption(RWD_FAV_PRODUCTS, "RWD_FAV_PRODUCTS", ""));
    	  }
    	  if (isFavPage) {
    		  sortList.add(new SortOption(RWD_BEST_MATCH, "", ""));
    	  }
      }
    } else { // Standard options.
      if (isKeywordSearch || isEventDI || isFavPage) { // On the search page.
        if (isRWD) {
          sortList.add(new SortOption(RWD_PRICE_HIGH_LOW, "MAX_PROMO_PRICE|1", ""));
          sortList.add(new SortOption(RWD_PRICE_LOW_HIGH, "MAX_PROMO_PRICE", ""));
          sortList.add(new SortOption(RWD_DISCOUNT_HIGH_LOW, "PCT_DISCOUNT|1", ""));
          sortList.add(new SortOption(RWD_DISCOUNT_LOW_HIGH, "PCT_DISCOUNT", ""));
          sortList.add(new SortOption(RWD_IN_STOCK, "STOCK_STATUS", ""));
          sortList.add(new SortOption(RWD_NEWEST_FIRST, "SELLABLE_DATE|1", ""));
          if (!isFavPage && getBrandSpecs().isEnableMyFavorites() && getBrandSpecs().isEnableSortByFavorites()) {
            sortList.add(new SortOption(RWD_FAV_PRODUCTS, "RWD_FAV_PRODUCTS", ""));
          }
          sortList.add(new SortOption(RWD_BEST_MATCH, "", ""));
        } else {
          sortList.add(new SortOption("PRICE HIGH TO LOW", "MAX_PROMO_PRICE|1", ""));
          sortList.add(new SortOption("PRICE LOW TO HIGH", "MAX_PROMO_PRICE", ""));
          sortList.add(new SortOption("% DISCOUNT HIGH TO LOW", "PCT_DISCOUNT|1", ""));
          sortList.add(new SortOption("% DISCOUNT LOW TO HIGH", "PCT_DISCOUNT", ""));
          sortList.add(new SortOption("NEWEST FIRST", "SELLABLE_DATE|1", ""));
          // Removed the sort option "Best Match" to fix duplicate for story NMOBLDS-833.
          sortList.add(new SortOption("IN STOCK", "STOCK_STATUS", ""));
        }
      } else {
        if (isRWD) {
          sortList.add(new SortOption(RWD_PRICE_HIGH_LOW, "MAX_RETAIL_PRICE|1", ""));
          sortList.add(new SortOption(RWD_PRICE_LOW_HIGH, "MAX_RETAIL_PRICE", ""));
          sortList.add(new SortOption(RWD_IN_STOCK, "STOCK_STATUS", ""));
          sortList.add(new SortOption(RWD_NEWEST_FIRST, "SELLABLE_DATE|1", ""));
        } else {
          sortList.add(new SortOption("PRICE HIGH TO LOW", "MAX_RETAIL_PRICE|1", ""));
          sortList.add(new SortOption("PRICE LOW TO HIGH", "MAX_RETAIL_PRICE", ""));
          sortList.add(new SortOption("NEWEST FIRST", "SELLABLE_DATE|1", ""));
          sortList.add(new SortOption("IN STOCK", "STOCK_STATUS", ""));
        }
      }
    }
    SortOption currentSort = null;
    if ("sortOptionsB".equals(getBrandSpecs().getAlternativeSortOptions())) { // LC uses these.
  	  if (isFavPage) {
  		  currentSort = new SortOption(RWD_BEST_MATCH, "", "");
	  } else {
		  currentSort = new SortOption("FEATURED", "", "");
	  }
    } else { // Standard choices
      if (isRWD) {
        if (isKeywordSearch || isEventDI || isFavPage) { // on the search page.
          if (StringUtilities.isNotEmpty(sort)) {
            String sortLabel = "BEST MATCH";
            
            if (sort.equalsIgnoreCase("MAX_RETAIL_PRICE|1")) {
              sortLabel = RWD_PRICE_HIGH_LOW;
            } else if (sort.equalsIgnoreCase("MAX_RETAIL_PRICE")) {
              sortLabel = RWD_PRICE_LOW_HIGH;
            } else if (sort.equalsIgnoreCase("MAX_PROMO_PRICE|1")) {
              sortLabel = RWD_PRICE_HIGH_LOW;
            } else if (sort.equalsIgnoreCase("MAX_PROMO_PRICE")) {
              sortLabel = RWD_PRICE_LOW_HIGH;
            } else if (sort.equalsIgnoreCase("STOCK_STATUS")) {
              sortLabel = RWD_IN_STOCK;
            } else if (sort.equalsIgnoreCase("PCT_DISCOUNT|1")) {
              sortLabel = RWD_DISCOUNT_HIGH_LOW;
            } else if (sort.equalsIgnoreCase("PCT_DISCOUNT")) {
              sortLabel = RWD_DISCOUNT_LOW_HIGH;
            } else if (sort.equalsIgnoreCase("SELLABLE_DATE|1")) {
              sortLabel = RWD_NEWEST_FIRST;
            } else if (sort.equalsIgnoreCase("RWD_FAV_PRODUCTS")) {
              sortLabel = RWD_FAV_PRODUCTS;
            }
            currentSort = new SortOption(sortLabel, sort, "");
          } else {
            currentSort = new SortOption("Best Match", "", "");
          }
        } else {
          currentSort = new SortOption("NEWEST FIRST", "", "");
        }
      } else {
        if (isKeywordSearch || isEventDI) { // on the search page.
          if (StringUtilities.isNotEmpty(sort)) {
            String sortLabel = "BEST MATCH";
            
            if (sort.equalsIgnoreCase("MAX_RETAIL_PRICE|1")) {
              sortLabel = "PRICE HIGH TO LOW";
            } else if (sort.equalsIgnoreCase("MAX_RETAIL_PRICE")) {
              sortLabel = "PRICE LOW TO HIGH";
            } else if (sort.equalsIgnoreCase("MAX_PROMO_PRICE|1")) {
              sortLabel = "PRICE HIGH TO LOW";
            } else if (sort.equalsIgnoreCase("MAX_PROMO_PRICE")) {
              sortLabel = "PRICE LOW TO HIGH";
            } else if (sort.equalsIgnoreCase("STOCK_STATUS")) {
              sortLabel = "IN STOCK";
            } else if (sort.equalsIgnoreCase("PCT_DISCOUNT|1")) {
              sortLabel = "% DISCOUNT HIGH TO LOW";
            } else if (sort.equalsIgnoreCase("PCT_DISCOUNT")) {
              sortLabel = "% DISCOUNT LOW TO HIGH";
            } else if (sort.equalsIgnoreCase("SELLABLE_DATE|1")) {
              sortLabel = "NEWEST FIRST";
            } else if (sort.equalsIgnoreCase("RWD_FAV_PRODUCTS")) {
              sortLabel = RWD_FAV_PRODUCTS;
            }
            if (!sortLabel.equalsIgnoreCase("BEST MATCH") && isKeywordSearch) {
              sortList.add(new SortOption("BEST MATCH", "", ""));
            }
            currentSort = new SortOption(sortLabel, sort, "");
          } else {
            currentSort = new SortOption("Best Match", "", "");
          }
        } else {
          currentSort = new SortOption("NEWEST FIRST", "", "");
        }
      }
    }
    pageModel.setSorts(sortList);
    pageModel.setCurrentSort(currentSort);
    
    if (getBrandSpecs().getBrandName().equalsIgnoreCase("The Horchow Collection")) {
      
      final List<SortOption> hcSortList = new ArrayList<SortOption>();
      SortOption hcSortOption = new SortOption("", "", "");
      for (final SortOption sp : sortList) {
        final String label = WordUtils.capitalizeFully(sp.getLabel());
        hcSortList.add(new SortOption(label, sp.getValue(), sp.getOrder()));
      }
      
      final String currentLabel = WordUtils.capitalizeFully(currentSort.getLabel());
      hcSortOption = new SortOption(currentLabel, currentSort.getValue(), currentSort.getOrder());
      
      pageModel.setSorts(hcSortList);
      pageModel.setCurrentSort(hcSortOption);
    }
    
  }
  
  private void redirectToBloomReachSearch(final DynamoHttpServletRequest request, final DynamoHttpServletResponse response) {
    try {// url changed to search.jsp as BR search will redirect to search.jsp instead of brSearch.jsp
      String url = "/search.jsp?from=brSearch&request_type=search&search_type=keyword&q=" + request.getParameter("Ntt") + "&l=" + request.getParameter("Ntt");
      url = NMFormHelper.reviseUrl(url);
      response.sendLocalRedirect(url, request);
    } catch (final Exception e) {
      
    }
  }
  
} // End of Class
