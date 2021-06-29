package com.nm.commerce.pagedef.evaluator;

import static com.nm.common.INMGenericConstants.COLON_STRING;
import static com.nm.common.INMGenericConstants.IS_FILTER_PERSISTENCE_IN_SESSION;
import static com.nm.common.ProfileRepositoryConstants.FILTER_PERSISTENCE;

import com.nm.ajax.myfavorites.utils.MyFavoritesUtil;
import com.nm.ajax.request.JspPageInvoker;
import com.nm.ajax.request.XStreamUtil;
import com.nm.ajax.search.beans.FacetDimension;
import com.nm.ajax.search.beans.FacetDimensionGroup;
import com.nm.ajax.search.beans.FacetDimensionValue;
import com.nm.ajax.search.beans.FacetInfo;
import com.nm.ajax.search.beans.Facets;
import com.nm.catalog.navigation.CategoryHelper;
import com.nm.catalog.navigation.NMCategory;
import com.nm.catalog.navigation.NMSearchCategory;
import com.nm.catalog.template.TemplateUtils;
import com.nm.commerce.NMProfile;
import com.nm.commerce.pagedef.definition.EndecaTemplatePageDefinition;
import com.nm.commerce.pagedef.definition.PageDefinition;
import com.nm.commerce.pagedef.model.EndecaDrivenPageModel;
import com.nm.commerce.pagedef.model.PageModel;
import com.nm.commerce.pagedef.model.bean.SortOption;
import com.nm.components.CommonComponentHelper;
import com.nm.email.EmailInfo;
import com.nm.search.GenericSearchUtil;
import com.nm.search.endeca.EndecaDrivenUtil;
import com.nm.search.endeca.EndecaIndexType;
import com.nm.search.endeca.NMSearchRecord;
import com.nm.search.endeca.NMSearchResult;
import com.nm.search.endeca.NMSearchUtil;
import com.nm.search.endeca.SearchParameters;
import com.nm.storeinventory.InStoreSearchInput;
import com.nm.storeinventory.StoreAvailQuery;
import com.nm.storeinventory.StoreInventoryFacade;
import com.nm.utils.LocalizationUtils;
import com.nm.utils.NmoUtils;
import com.nm.utils.PCSSessionData;
import com.nm.utils.PreferredProductsData;
import com.nm.utils.StringUtilities;
import com.nm.utils.SystemSpecs;

import com.endeca.navigation.ENEQueryException;

import atg.core.util.StringUtils;
import atg.nucleus.Nucleus;
import atg.servlet.DynamoHttpServletRequest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import javax.servlet.http.HttpSession;
import javax.servlet.jsp.PageContext;

import org.apache.commons.lang.WordUtils;

public class EndecaDrivenCategoryEvaluator extends TemplatePageEvaluator {
  
  @Override
  protected PageModel allocatePageModel() {
    return new EndecaDrivenPageModel();
  }
  
  /**
   * Returns the category specified by the itemId parameter. Null is returned it the category ID or category is not found.
   *
   * @return true if the category was not found and redirect occurred.
   * @throws IOException
   */
  @Override
  protected NMCategory getCategory() throws IOException {
    return getCategoryHelper().getNMSearchCategory(getRequest().getParameter("itemId"), getRequest().getParameter("parentId"));
  }
  
  @Override
  public boolean evaluate(final PageDefinition pageDefinition, final PageContext pageContext) throws Exception {
    
    final DynamoHttpServletRequest request = getRequest();
    final HttpSession session = request.getSession();
    final EndecaTemplatePageDefinition pageDef = (EndecaTemplatePageDefinition) pageDefinition;
    boolean pcsEnabled = false;
    String endecaStratifyString = null;
    final EmailInfo emailInfo = (EmailInfo) request.resolveName("/nm/email/EmailInfo");
    final TemplateUtils templateUtils = CommonComponentHelper.getTemplateUtils();
    
    request.setParameter("endecaTemplate", pageDefinition);
    request.setParameter("searchType", "endecaDrivenCat");
    
    final NMCategory nmCategory = getCategory();
    final String catId = nmCategory.getId();
    
    final String perPageViewDisabledCategory = pageDef.getPerPageViewDisabledCats();
    final boolean isPerPageViewDisabled = null != catId && null != perPageViewDisabledCategory && Arrays.asList(perPageViewDisabledCategory.split(",")).contains(catId);
    if (isPerPageViewDisabled) {
      request.setParameter("pageSize", "30");
    }
    
    final QueryString queryObj = getQueryString(request, pageDef, catId, nmCategory);
    String queryString = queryObj.getQueryString();
    String refinements = queryObj.getRefinements();
    boolean isPlainSearch = queryObj.isPlainSearch();
    
    request.setAttribute("rte", queryObj.getRte());
    
    final boolean returnValue = super.evaluate(pageDefinition, pageContext);
    if (!returnValue) {
      return false;
    }
    
    final EndecaDrivenPageModel pageModel = (EndecaDrivenPageModel) getPageModel();
    pageModel.setOriginalPageDefinition(pageDef);
    
    if (isPCSEnabled()) {
      final PCSSessionData pcsSessionData = (PCSSessionData) request.resolveName("/nm/utils/PCSSessionData");
      if (getBrandSpecs().getPreferredProductsPCSByEntryPoint() && emailInfo.isFromEmail() && catId.equals(emailInfo.getEmailCategory())) {
        endecaStratifyString = pcsSessionData.getPcsProductsAsString(nmCategory, getBrandSpecs().getNumPrefProductsPCS());
      } else {
        endecaStratifyString = pcsSessionData.getPcsProductsAsString(nmCategory);
      }
      
      if (endecaStratifyString != null && !pcsSessionData.isPrefferedProducts()) {
        pcsEnabled = true;
      }

      if (null != pageModel.getActiveCategory() && null != pageModel.getCategory().getCategoryAttributeMap() && null != pageModel.getCategory().getCategoryAttributeMap().get("pcsExcludeCategory")
              && !pageModel.getCategory().getCategoryAttributeMap().get("pcsExcludeCategory").equals("true")) {
        pageModel.setPcsEnabled(pcsSessionData.isPcsCallEnabled());
        pageModel.setPcsRequestSuccessful(pcsSessionData.isPcsRequestSuccessful());
        pageModel.setPcsResponseSuccessful(pcsSessionData.isPcsResponseSuccessful());
      }
    } else {
    	final PreferredProductsData preferredProductsData=(PreferredProductsData) request.resolveName( "/nm/utils/PreferredProductsData" );
      endecaStratifyString = preferredProductsData.getPreferredProductsAsString(nmCategory);
    }
    
    request.setParameter("categoryId", catId);
    request.setAttribute("categoryId", catId);
    request.setParameter("endecaParentId", catId);
    
    if (isPerPageViewDisabled) {
      pageModel.setPerPageViewDisabled(true);
      pageModel.setPageSize("30");
    } else {
      pageModel.setPerPageViewDisabled(false);
    }
    
    Boolean isRWD = pageDef.getIsRWD();
    
    if (isRWD == null || isRWD == false) {
      isRWD = new Boolean(false);
    }
    
    setSorts(pageModel, pcsEnabled, isRWD.booleanValue());
    pageModel.setRefinements(refinements);
    final int pageNum = getRequestedPageOffset(request, pageDef) + 1;
    pageModel.setRequestedPage(Integer.toString(pageNum));
    
    NMSearchResult searchResult = null;
    
    final NMCategory category = CategoryHelper.getInstance().getNMCategory(catId);
    
    if (pageDefinition.getId().equals("EndecaDrivenPinterest")) {
      queryString = queryString + "&Nr=TREE:REPINNED";
    }
    // plain search -- get all result without filtering
    searchResult = searchEndecaDrivenCategory(catId, queryString, "", pageModel, endecaStratifyString, new StoreAvailQuery());
    
    if (searchResult != null) {
      FacetInfo facetInfo = populateFacetInfo(searchResult, catId, category);
      List<FacetDimension> facetDimension = EndecaDrivenUtil.getEndecaDriveFacetsAsList(catId, facetInfo.getFacets(), EndecaDrivenUtil.FILTER_FACET_BLACK_LIST);
      pageModel.setFacetList(facetDimension);
      Object filterPersistenceSessionAttribute = session.getAttribute(IS_FILTER_PERSISTENCE_IN_SESSION);
      if (category.isIncludeFilterPersistenceEnabled() && null != filterPersistenceSessionAttribute && (Boolean) filterPersistenceSessionAttribute) {
        final String persistRefinements = getPersistedFilterRefinements(refinements, isPlainSearch, facetDimension, pageModel);
        getProfile().setPropertyValue(FILTER_PERSISTENCE, persistRefinements);
        pageModel.setRefinements(persistRefinements);
        if (StringUtilities.isNotEmpty(persistRefinements)) {
          pageModel.setFilterPersist(Boolean.TRUE);
          refinements = persistRefinements;
          isPlainSearch = Boolean.FALSE;
        }
      } else {
        getProfile().setPropertyValue(FILTER_PERSISTENCE, null);
      }
      // filtered search: apply filters
      if (!isPlainSearch) {
        
        final InStoreSearchInput inStoreSearchInput = InStoreSearchInput.createFromRequest(request, "locationInput", "radiusInput", "allStoresInput");
        
        if (inStoreSearchInput.isGeoSearch()) {
          pageModel.setInStoreSearchInput(inStoreSearchInput);
        }
        
        final StoreAvailQuery storeAvailQuery = new StoreAvailQuery(inStoreSearchInput, (StoreInventoryFacade) request.resolveName("/nm/storeinventory/StoreInventoryFacade"), null);
        
        searchResult = searchEndecaDrivenCategory(catId, queryString, refinements, pageModel, endecaStratifyString, storeAvailQuery);
        if (searchResult != null) {
          facetInfo = populateFacetInfo(searchResult, catId, category);
          final String facetInfoJSON = XStreamUtil.getInstance().convertObjectToJson(facetInfo);
          pageModel.setRefinedFacetInfoJSON(facetInfoJSON);
          pageModel.setRefinements(refinements.replace("+", ","));
          if (refinements.length() > 0) {
            pageModel.setFilterOmniture(EndecaDrivenUtil.generateFilterOmnitureData(facetInfo, pageModel));
          }
        }
      }
      
      pageModel.setCategory(nmCategory);
      pageModel.setSearchResult(searchResult);
      
      pageModel.setColumnCount(4);
      pageModel.setShowGraphicBlock(true);
      // paging
      int pageSize = getPageSize(request, pageDef);
      int viewBy = pageSize;
      if (null != templateUtils) {
        pageModel.setPromoTilePositions(pageDef.getPromoTilePositions());
        templateUtils.setPromoTiles(category, pageModel, pageSize, pageNum);
      }
      
      if (!pageModel.getPromoTiles().isEmpty()) {
        pageModel.setShowPromoTile(true);
        pageModel.setViewBy(String.valueOf(viewBy));
        if (pageSize == 30) {
          pageSize += -1;
        } else if (pageSize == 120) {
          pageSize += -3;
        }
      }
      
      if (null != templateUtils) {
        templateUtils.setGraphicHeraderPosition(pageModel, pageDef, true);
      }
      pageModel.setPaging(EndecaDrivenUtil.isPaging(searchResult, pageSize));
      if (pageModel.isPaging()) {
        final Long totalRec = new Long(searchResult.getTotalRecordCount());
        pageModel.setPagination(GenericSearchUtil.generatePagination(getRequestedPageOffset(request, pageDef) + 1, totalRec.intValue(), pageSize));
      }
      
      pageModel.setPageSize(String.valueOf(pageSize));
      // invoke debug template only if the visual debug option is on
      if (getBrandSpecs().isVisualDebug()) {
        final JspPageInvoker invoker = new JspPageInvoker();
        final ListIterator<NMSearchRecord> records = searchResult.getSearchRecordList().listIterator();
        while (records.hasNext()) {
          final NMSearchRecord record = records.next();
          final Map<String, Object> debugMap = NMSearchUtil.getDebugMap(record);
          record.setDebugInfo(invoker.invokePage("/page/etemplate/debug/debugSearchRecord.jsp", debugMap, request, getResponse()));
        }
      }
      if (!StringUtilities.isNullOrEmpty(nmCategory.getThumbImgShot()) && pageDef.getEnableZEShot()) {
        final List<NMSearchRecord> records = searchResult.getSearchRecordList();
        for (final NMSearchRecord record : records) {
          record.setThumbImgShot(nmCategory.getThumbImgShot());
          record.setEnableZEShot(pageDef.getEnableZEShot());
          if (log.isLoggingDebug()) {
            log.logDebug("EndecaDrivenCategoryEvaluator line182 - thumbImgShot:" + nmCategory.getThumbImgShot());
          }
        }
      }
      /** My Favorites - code tuned to pass fav items list as param to favorites droplet . */
      if (getBrandSpecs().isEnableMyFavorites()) {
        pageModel.setMyFavItems(org.apache.commons.lang.StringUtils.join(MyFavoritesUtil.getInstance().getMyFavoriteItemsList(getProfile(), null, false), ','));
      }
    }
    
    pageModel.setSortValue(pageModel.getCurrentSort().getValue());
    EndecaDrivenUtil.populateSortAndPCSData(pageModel);
    /* Process the TMS data dictionary attributes */
    templateUtils.processTMSDataDictionaryForCategoryPages(pageDefinition, pageModel, false);
    
    return returnValue;
  }
  
  protected void setSorts(final EndecaDrivenPageModel pageModel, final boolean pcsEnabled, final boolean isRWD) {
    final ArrayList<SortOption> sortList = new ArrayList<SortOption>();
    final NMSearchCategory category = (NMSearchCategory) pageModel.getCategory();
    SortOption currentSort = null;
    boolean isPinned = false;
    
    final String RWD_PRICE_HIGH_LOW = "Price: high to low";
    final String RWD_PRICE_LOW_HIGH = "Price: low to high";
    final String RWD_DISCOUNT_HIGH_LOW = "Discount: high to low";
    final String RWD_DISCOUNT_LOW_HIGH = "Discount: low to high";
    final String RWD_NEWEST_FIRST = "Newest first";
    final String RWD_IN_STOCK = "In Stock";
    final String RWD_BEST_MATCH = "Best Match";
    final String RWD_FAV_PRODUCTS = "My Favorites";
    
    if (pageModel.getPageDefinition().getId().equalsIgnoreCase("EndecaDrivenPinterest")) {
      isPinned = true;
    }
    
    if ("sortOptionsB".equals(getBrandSpecs().getAlternativeSortOptions())) { // LC uses these
      String endecaNsParam = "";
      sortList.add(new SortOption("PRICE LOW TO HIGH", "MAX_PROMO_PRICE", ""));
      sortList.add(new SortOption("PRICE HIGH TO LOW", "MAX_PROMO_PRICE|1", ""));
      
      try {
        // if(!StringUtils.isEmpty(category.getId()) && NMSearchUtil.categoryHasPreferredProducts( category.getDataSource() )) {
        if (!StringUtils.isEmpty(category.getId()) && category.hasPreferredProducts()) {
          endecaNsParam = category.getId() + "|0||"; // Preferred products to the top.
          List<String> preferredProductIds = category.getPreferredProductIds();
          if (!NmoUtils.isEmptyCollection(preferredProductIds)) {
            pageModel.setPreferredProducts(preferredProductIds.size());
          }
        }
      } catch (final Exception e) {
        e.printStackTrace();
      }
      endecaNsParam += "TREE_SORT_VALUE2|1||WEB1_SORT||WEB2_SORT||WEB3_SORT||WEB4_SORT||SELLABLE_DATE|1";
      sortList.add(new SortOption("FEATURED", endecaNsParam, ""));
      sortList.add(new SortOption("JUST IN", "SELLABLE_DATE|1", ""));
      if (getBrandSpecs().isEnableMyFavorites() && getBrandSpecs().isEnableSortByFavorites()) {
        sortList.add(new SortOption(RWD_FAV_PRODUCTS, "RWD_FAV_PRODUCTS", ""));
      }
      currentSort = new SortOption("FEATURED", endecaNsParam, "");
    } else {
      if (isPinned) {
        sortList.add(new SortOption("TOP PINNED", "REPIN_COUNT|1", ""));
        currentSort = new SortOption("TOP PINNED", "REPIN_COUNT|1", "");
      }
      // Standard configuration
      
      if (isRWD) {
        sortList.add(new SortOption(RWD_PRICE_HIGH_LOW, "MAX_PROMO_PRICE|1", ""));
        sortList.add(new SortOption(RWD_PRICE_LOW_HIGH, "MAX_PROMO_PRICE", ""));
        if (category.getFlgSaleOnly()) {
          sortList.add(new SortOption(RWD_DISCOUNT_HIGH_LOW, "PCT_DISCOUNT|1", ""));
          sortList.add(new SortOption(RWD_DISCOUNT_LOW_HIGH, "PCT_DISCOUNT", ""));
        }
        if (!isPinned) {
          sortList.add(new SortOption(RWD_NEWEST_FIRST, "SELLABLE_DATE|1", ""));
          currentSort = new SortOption("NEWEST FIRST", "", "");
        }
        sortList.add(new SortOption(RWD_IN_STOCK, "STOCK_STATUS", ""));
        if (getBrandSpecs().isEnableMyFavorites() && getBrandSpecs().isEnableSortByFavorites()) {
          sortList.add(new SortOption(RWD_FAV_PRODUCTS, "RWD_FAV_PRODUCTS", ""));
        }
      } else {
        sortList.add(new SortOption("PRICE HIGH TO LOW", "MAX_PROMO_PRICE|1", ""));
        sortList.add(new SortOption("PRICE LOW TO HIGH", "MAX_PROMO_PRICE", ""));
        
        if (category.getFlgSaleOnly()) {
          sortList.add(new SortOption("% DISCOUNT HIGH TO LOW", "PCT_DISCOUNT|1", ""));
          sortList.add(new SortOption("% DISCOUNT LOW TO HIGH", "PCT_DISCOUNT", ""));
        }
        if (!isPinned) {
          sortList.add(new SortOption("NEWEST FIRST", "SELLABLE_DATE|1", ""));
          currentSort = new SortOption("NEWEST FIRST", "", "");
        }
        sortList.add(new SortOption("IN STOCK", "STOCK_STATUS", ""));
      }
    }
    
    if (pcsEnabled && !isPinned) {
      if (isRWD) {
        sortList.add(new SortOption(RWD_BEST_MATCH, PCSSessionData.PCS_SORT_OPTION_VALUE, ""));
        currentSort = new SortOption(RWD_BEST_MATCH, PCSSessionData.PCS_SORT_OPTION_VALUE, "");
      } else {
        sortList.add(new SortOption(PCSSessionData.PCS_SORT_OPTION_LABEL, PCSSessionData.PCS_SORT_OPTION_VALUE, ""));
        currentSort = new SortOption(PCSSessionData.PCS_SORT_OPTION_LABEL, PCSSessionData.PCS_SORT_OPTION_VALUE, "");
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
  
  private QueryString getQueryString(final DynamoHttpServletRequest request, final EndecaTemplatePageDefinition pageDef, final String catId, final NMCategory category) {
    
    final QueryString queryObj = new QueryString();
    
    String queryStringNav = null;

    queryObj.setPlainSearch(true);
    queryObj.setRefinements("");
    
    String queryString = request.getParameter("queryString");
    final Boolean isFromBreadCrumb = Boolean.valueOf(request.getParameter("breadcrumb"));
    int pageSize = getPageSize(request, pageDef);
    final NMProfile profile = (NMProfile) getRequest().resolveName("/atg/userprofiling/Profile");
    final LocalizationUtils utils = CommonComponentHelper.getLocalizationUtils();
    boolean isInternational = false;
    if (null != utils && null != profile) {
      if (utils.isSupportedByFiftyOne(getProfile().getCountryPreference()) && CommonComponentHelper.getSystemSpecs().isFiftyOneEnabled()) {
        isInternational = true;
      }
    }
    final TemplateUtils templateUtils = CommonComponentHelper.getTemplateUtils();
    
    final NMSearchCategory categoryItem = CategoryHelper.getInstance().getNMSearchCategory(catId);

    boolean isPromoTilesSet = false;
    if (null != templateUtils) {
      isPromoTilesSet = templateUtils.hasPromoTiles(category, isInternational);
    }
    if (isPromoTilesSet) {
      if (pageSize == 30) {
        pageSize += -1;
      } else if (pageSize == 120) {
        pageSize += -3;
      }
    }
    
    if (categoryItem.isDisplayAsSuites()) {
      queryStringNav = "Nao";
    } else {
      queryStringNav = "No";
    }
    if (!isFromBreadCrumb) { // initial page load without filtering or sorting
      queryString = "itemId=" + catId + "&pageSize=" + pageSize + "&" + queryStringNav + "=0";
    } else { // returning from breadcrumb of product page
     String requestedNo = request.getParameter(queryStringNav);
     if(StringUtilities.isNullOrEmpty( requestedNo )) requestedNo=request.getParameter("No");
     
      final HashMap<String, String> scrbQuery = NMSearchUtil.queryScrubber(queryString, EndecaDrivenUtil.MASTER_FACET_LIST);
      final String sort = request.getParameter("Ns");
      String qry = scrbQuery.get("qry");
      // if the qry string is empty default to itemId= catId
      // otherwise this will eventually get passed to the product page and result in an invalid breadcrumb URL
      if (qry.equals("")) {
        qry = "itemId=" + catId;
      }
      if(StringUtilities.isNullOrEmpty( requestedNo )){
      queryString = qry + "&pageSize=" + pageSize + "&" + queryStringNav + "=0";
      }else {
      	 queryString = qry + "&pageSize=" + pageSize + "&" + queryStringNav + "=" + requestedNo;
      }
      if (!StringUtils.isEmpty(sort)) {
        queryString += "&Ns=" + sort;
      }
      queryObj.setRefinements(request.getParameter("N"));
      final String locationInput = request.getParameter("locationInput");
      final String radiusInput = request.getParameter("radiusInput");
      final boolean allStoresSearch = NmoUtils.coalesce(request.getParameter("allStoresInput"), "false").equalsIgnoreCase("true");
      final boolean isGeoSearch = InStoreSearchInput.isGeoSearch(locationInput, allStoresSearch, radiusInput);
      if (!StringUtils.isEmpty(sort) || !StringUtils.isEmpty(queryObj.getRefinements()) || isGeoSearch) {
        queryObj.setPlainSearch(false);
      }
    }
    
    queryObj.setQueryString(queryString);
    
    try {
      final String rte = EndecaDrivenUtil.calculateRteParam(request, queryString, queryObj.getRefinements());
      queryObj.setRte(rte);
    } catch (final Exception e) {
      
    }
    
    return queryObj;
    
  }
  
  /**
   * The purpose of this method is to get the persisted filter refinements from the profile object. This method will get the available refinements for the category and update the refinements at
   * profile if any refinement is not presents in the category. If there is a refinements at profile object and include filter persistence flag is enabled in CatMan then persist the filters selection
   * for the category in web.
   *
   * @param refinements
   *          The Refinements object.
   * @param isPlainSearch
   *          The boolean value if there in no filter selections.
   * @param facetDimension
   *          The FacetDimension object.
   * @param profile
   *          The Profile object.
   * @return String The updated refinements.
   *
   */
  private String getPersistedFilterRefinements(final String refinements, final Boolean isPlainSearch, final List<FacetDimension> facetDimension, EndecaDrivenPageModel pageModel) {
    String updateFilterRefinements = StringUtilities.EMPTY;
    final String filterRefinements = (String) getProfile().getPropertyValue(FILTER_PERSISTENCE);
    if (StringUtils.isEmpty(refinements) && isPlainSearch && StringUtilities.isNotEmpty(filterRefinements)) {
      List<List<FacetDimensionValue>> facetDimensionList = new ArrayList<List<FacetDimensionValue>>();
      Map<String, String> dimIdNameMap = new HashMap<String, String>();
      final List<String> filterList = getBrandSpecs().getFilterPersistenceList();
      if (NmoUtils.isNotEmptyCollection(filterList)) {
        for (String filterName : filterList) {
          for (FacetDimension dimension : facetDimension) {
            List<FacetDimensionGroup> facetDimensionGroup = dimension.getCategories();
            for (FacetDimensionGroup group : facetDimensionGroup) {
              if (group.getName().equalsIgnoreCase(filterName.trim())) {
                facetDimensionList.add(group.getValues());
              }
            }
          }
        }
      }
      for (List<FacetDimensionValue> facetDimensions : facetDimensionList) {
        List<FacetDimensionValue> dimensionValueList = (List<FacetDimensionValue>) facetDimensions;
        for (FacetDimensionValue dimVal : dimensionValueList) {
          dimIdNameMap.put(dimVal.getId(), dimVal.getName());
        }
      }
      final List<String> refinementList = StringUtilities.makeList(filterRefinements);
      List<String> updateRefinements = new ArrayList<String>();
      List<String> dimNameList = new ArrayList<String>();
      for (String refinement : refinementList) {
        if (dimIdNameMap.containsKey(refinement)) {
          updateRefinements.add(refinement);
          dimNameList.add(dimIdNameMap.get(refinement));
        }
      }
      pageModel.setFilterPersistValues(org.apache.commons.lang.StringUtils.join(dimNameList, COLON_STRING));
      final String filterData = StringUtilities.makeString(updateRefinements);
      updateFilterRefinements = org.apache.commons.lang.StringUtils.deleteWhitespace(filterData);
    }
    return updateFilterRefinements;
  }

  private int getPageSize(final DynamoHttpServletRequest request, final EndecaTemplatePageDefinition pageDef) {
    final String requestedPageSize = request.getParameter("pageSize");
    int pageSize = Integer.valueOf(pageDef.getPageSize());
    
    if (!StringUtils.isEmpty(requestedPageSize)) {
      pageSize = Integer.valueOf(requestedPageSize);
      setUserPref(pageDef, request, requestedPageSize);
    } else if (isPrefUserChoiceAll(pageDef, request, false)) {
      pageSize = 120;
    }
    if(pageSize > 120){
      pageSize = 30;
    }
    return pageSize;
  }
  
  private int getRequestedPageOffset(final DynamoHttpServletRequest request, final EndecaTemplatePageDefinition pageDef) {
    int requestedPageOffset = 0;
    final Boolean isFromBreadCrumb = Boolean.valueOf(request.getParameter("breadcrumb"));
    
    if (isFromBreadCrumb) {
    	String requestedNo = request.getParameter("No");
      if(StringUtilities.isNullOrEmpty(requestedNo)) 
    	  requestedNo=request.getParameter("Nao");
      final int pageSize = getPageSize(request, pageDef);
      if (!StringUtils.isEmpty(requestedNo)) {
        requestedPageOffset = Integer.valueOf(requestedNo) / pageSize;
      }
    }
    return requestedPageOffset;
  }
  
  private NMSearchResult searchEndecaDrivenCategory(final String categoryId, final String queryString, final String refinements, final EndecaDrivenPageModel pageModel,
          final String endecaStratifyString, final StoreAvailQuery storeAvailQuery) throws Exception {
    try {
      final SearchParameters searchParameters = NMSearchUtil.getDefaultSearchParameters();
      if (getBrandSpecs().isVisualDebug()) {
        searchParameters.addDebugFieldList();
      }
      searchParameters.setEndecaStratifyString(endecaStratifyString);
      final List<String> filterNames = EndecaDrivenUtil.getDimensionFilterNames(categoryId);
      if (filterNames != null) {
        searchParameters.addDimensionsFromCollection(filterNames);
      }
      searchParameters.setUserSelectedCountryCode(getProfile().getCountryPreference());
      final NMSearchResult searchResult =
              NMSearchUtil.getEndecaDrivenResult(searchParameters, categoryId, queryString, refinements, true, false, EndecaIndexType.BASE_INDEX, true, storeAvailQuery, null,null);
      
      if (searchResult != null) {
        searchResult.setQueryString(queryString);
        searchResult.setFilterRefinements(refinements);
      }
      return searchResult;
    } catch (final ENEQueryException e) {
      // set pageModel error to display error message when Endeca search
      // has exception
      pageModel.setError(true);
      e.printStackTrace();
      return null;
    } catch (final Exception e) {
      throw e;
    }
  }
  
  private FacetInfo populateFacetInfo(final NMSearchResult searchResult, final String categoryId, final NMCategory category) throws Exception {
    FacetInfo facetInfo = null;
    if (searchResult != null) {
      // establish facet collections for result set
      final SearchParameters searchParameters = NMSearchUtil.getMinimumSearchParameters();
      searchParameters.addDimensionsFromCollection(EndecaDrivenUtil.getDimensionFilterNames(categoryId));
      Facets facets = EndecaDrivenUtil.getFacets(searchResult, categoryId, searchParameters, categoryId, false, false, EndecaIndexType.BASE_INDEX, true, false);
      facets = EndecaDrivenUtil.applyFacetRules(facets, EndecaDrivenUtil.CATEGORIES, EndecaDrivenUtil.LIVE_TREE, searchResult, category);
      
      /* facets = EndecaDrivenUtil.groupDesigners( searchResult, EndecaDrivenUtil.CATEGORIES, facets, categoryId, false, false, EndecaIndexType.BASE_INDEX, true ); */
      facetInfo = new FacetInfo();
      facetInfo.setFacets(facets);
      facetInfo.setTotalProducts(searchResult.getTotalRecordCount());
      
    }
    return facetInfo;
  }
  
  /*
   * private ProductTemplatePageDefinition getOriginalPageDef() { ProductTemplatePageDefinition pageDef = null;
   * 
   * try { RepositoryItem template = (RepositoryItem) getCategory().getDataSource().getPropertyValue( "template" ); if ( template != null ) { String templateElement = (String)
   * template.getPropertyValue( "templateElement" ); if ( templateElement != null ) { pageDef = (ProductTemplatePageDefinition) Nucleus.getGlobalNucleus().resolveName( templateElement ); } } } catch (
   * Exception e ) { log.logError( e ); }
   * 
   * return pageDef; }
   */
  
  private boolean isPCSEnabled() throws IOException {
    final SystemSpecs systemSpecs = (SystemSpecs) Nucleus.getGlobalNucleus().resolveName("/nm/utils/SystemSpecs");
    if (systemSpecs.isPcsEnabled()) {
      // Check whether this category is excluded from PCS in CM2
      boolean isCategoryExcludedFromPCS = false;
      if (getCategory().getCategoryAttributeMap() != null) {
        if (getCategory().getCategoryAttributeMap().get("pcsExcludeCategory") != null) {
          isCategoryExcludedFromPCS = getCategory().getCategoryAttributeMap().get("pcsExcludeCategory").equals("true");
        }
      }
      if (!isCategoryExcludedFromPCS) {
        return true;
      }
    }
    
    return false;
  }
  
  public class QueryString {
    private boolean isPlainSearch;
    private String queryString;
    private String refinements;
    private String rte;
    
    public boolean isPlainSearch() {
      return isPlainSearch;
    }
    
    public void setPlainSearch(final boolean isPlainSearch) {
      this.isPlainSearch = isPlainSearch;
    }
    
    public String getQueryString() {
      return queryString;
    }
    
    public void setQueryString(final String queryString) {
      this.queryString = queryString;
    }
    
    public String getRefinements() {
      return refinements;
    }
    
    public void setRefinements(final String refinements) {
      this.refinements = refinements;
    }
    
    public String getRte() {
      return rte;
    }
    
    public void setRte(final String rte) {
      this.rte = rte;
    }
    
  }
  
}
