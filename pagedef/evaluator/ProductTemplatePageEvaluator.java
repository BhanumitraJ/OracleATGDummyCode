package com.nm.commerce.pagedef.evaluator;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.servlet.jsp.PageContext;

import org.apache.commons.lang.StringUtils;

import atg.nucleus.Nucleus;
import atg.repository.RepositoryItem;
import atg.servlet.DynamoHttpServletRequest;

import com.nm.ajax.myfavorites.utils.MyFavoritesUtil;
import com.nm.catalog.navigation.NMCategory;
import com.nm.commerce.catalog.NMProduct;
import com.nm.commerce.catalog.SuperProduct;
import com.nm.commerce.pagedef.definition.PageDefinition;
import com.nm.commerce.pagedef.definition.ProductTemplatePageDefinition;
import com.nm.commerce.pagedef.definition.TemplatePageDefinition;
import com.nm.commerce.pagedef.evaluator.template.SuperViewAllLogicHelper;
import com.nm.commerce.pagedef.model.PageModel;
import com.nm.commerce.pagedef.model.TemplatePageModel;
import com.nm.components.CommonComponentHelper;
import com.nm.formhandler.CatalogFilter;
import com.nm.utils.LocalizationUtils;
import com.nm.utils.ProdSkuUtil;

/**
 * Performs additional checks for valid, public category. Establishes refined template page definition (P9 may become P9ViewAll).
 */
public class ProductTemplatePageEvaluator extends TemplatePageEvaluator {
  
  private static final LocalizationUtils localizationUtils = (LocalizationUtils) Nucleus.getGlobalNucleus().resolveName("/nm/utils/LocalizationUtils");
  private static final String ALT_FILTER_PARAM = "altFilter";
  private static final String FILTER_PARAM = "filter1Type";
  private static final String SORT_PARAM = "sort";
  private static final String LIMIT_VALUE = "limit";
  protected static final String VIEW_ALL_VALUE = "all";
  public static final String FIRST_PAGE_KEY = "NM_FIRST_PAGE_DEFINITON";
  
  @Override
  public boolean evaluate(final PageDefinition pageDefinition, final PageContext pageContext) throws Exception {
    if (!super.evaluate(pageDefinition, pageContext)) {
      return false;
    }
    
    final TemplatePageModel templatePageModel = (TemplatePageModel) getPageModel();
    final ProductTemplatePageDefinition productTemplatePageDefinition = (ProductTemplatePageDefinition) pageDefinition;
    
    templatePageModel.setReturnParameters(buildReturnParameters(productTemplatePageDefinition, getRequest()));
    templatePageModel.setPartiallyEvaluated(true);
    /* Process the TMS data dictionary attributes */
    CommonComponentHelper.getTemplateUtils().processTMSDataDictionaryForCategoryPages(productTemplatePageDefinition, templatePageModel, Boolean.FALSE);
    return true;
  }
  
  @Override
  public void evaluateContent(final PageContext pageContext) throws Exception {
    // evaluate child products and paging here, only when the cached content is generated.
    
    final DynamoHttpServletRequest request = this.getRequest();
    final TemplatePageModel templatePageModel = (TemplatePageModel) getPageModel();
    final PageDefinition templatePageDefinition = templatePageModel.getPageDefinition();
    final NMCategory category = templatePageModel.getCategory();
    
    // this was set in evaluate / derivePageDefinition
    final ProductTemplatePageDefinition firstPageDefinition = (ProductTemplatePageDefinition) request.getAttribute(FIRST_PAGE_KEY);
    
    // the definition of every page after the first, needed to calculate paging
    final TemplatePageDefinition subsequentPageDefinition = firstPageDefinition.getAlternatePageDefinition();
    
    int totalItemCount = 0;
    final CatalogFilter catalogFilter = (CatalogFilter) getRequest().resolveName("/nm/formhandler/CatalogFilter");
    List<RepositoryItem> products = catalogFilter.initializeCatalogFilter(category, getRequest(), getProfile());
    // Added logic to support filtering products from categories using CM3 Flag Hide Internationally
    getFilterCategoryGroup().setCategoryFilterCode(getCategoryFilterCode());
    products = getFilterCategoryGroup().filterCategoryProductList(products);
    templatePageModel.setDisplayableProductList(products);
    
    /** My Favorites - code tuned to pass fav items list as param to favorites droplet . */
    if (getBrandSpecs().isEnableMyFavorites()) {
      templatePageModel.setMyFavItems(org.apache.commons.lang.StringUtils.join(MyFavoritesUtil.getInstance().getMyFavoriteItemsList(getProfile(), null,false), ','));
    }
    if (products != null) {
      templatePageModel.setRecordCount(products.size());
    }
    
    if (templatePageModel.isSubcatProductCollection()) {
      // intl. product restrictions requirement
      // get the displayable product list
      final SuperViewAllLogicHelper superViewAllLogic = new SuperViewAllLogicHelper(firstPageDefinition, getRequest());
      final List<SuperProduct> superProducts = superViewAllLogic.renderDisplayableProductList(category);
      templatePageModel.setDisplayableSuperProductList(superProducts);
      totalItemCount = templatePageModel.getDisplayableSuperProductList().size();
    } else {
      totalItemCount = templatePageModel.getDisplayableProductList().size();
      final List<NMProduct> nmProducts = createDisplayableNMProductList(templatePageModel.getDisplayableProductList(), templatePageModel.isSaleCategory());
      templatePageModel.setDisplayableNMProductList(nmProducts);
    }
    
    calculatePaging(templatePageModel, firstPageDefinition, subsequentPageDefinition, totalItemCount, getRequest());
    
    templatePageModel.setFlashSale(isFlashSale(category));
    /* Process the TMS data dictionary attributes */
    CommonComponentHelper.getTemplateUtils().processTMSDataDictionaryForCategoryPages(templatePageDefinition, templatePageModel, true);
  }
  
  private List<NMProduct> createDisplayableNMProductList(final List<? extends RepositoryItem> displayableProductList, final boolean isSaleCategory) {
    
    final List<NMProduct> nmProducts = new ArrayList<NMProduct>();
    for (final RepositoryItem displayableProduct : displayableProductList) {
      final String productId = displayableProduct.getRepositoryId();
      final ProdSkuUtil prodSkuUtil = CommonComponentHelper.getProdSkuUtil();
      final NMProduct nmProduct = prodSkuUtil.getProductObject(productId);
      if (isSaleCategory) {
        localizationUtils.populateProductDisplayPrice(nmProduct, true);
      }
      nmProducts.add(nmProduct);
    }
    return nmProducts;
  }
  
  @Override
  protected PageDefinition derivePageDefinition(final PageDefinition pageDefinition, final PageModel pageModel) throws Exception {
    PageDefinition currentPageDefinition = super.derivePageDefinition(pageDefinition, pageModel);
    
    if (currentPageDefinition == null) {
      return null;
    }
    
    final ProductTemplatePageDefinition productPageDefinition = (ProductTemplatePageDefinition) currentPageDefinition;
    
    final DynamoHttpServletRequest request = this.getRequest();
    final TemplatePageModel templatePageModel = (TemplatePageModel) pageModel;
    final NMCategory category = templatePageModel.getCategory();
    
    // determine if a variation of the original page should be used.
    // this code is adapted from EvaluateCatalogTemplate.java .
    
    // the definition of the first page, needed to calculate paging.
    ProductTemplatePageDefinition firstPageDefinition = productPageDefinition;
    
    if (isViewAll(productPageDefinition, request)) {
      firstPageDefinition = productPageDefinition.getViewAllPageDefinition();
      currentPageDefinition = productPageDefinition.getViewAllPageDefinition();
      templatePageModel.setViewAll(true);
      setUserPref(productPageDefinition, request, "ALL");
    } else if (isViewAll(SUPER_ALL_VALUE, request)) {
      firstPageDefinition = productPageDefinition.getViewAllPageDefinition();
      currentPageDefinition = productPageDefinition.getViewAllPageDefinition();
      templatePageModel.setViewAll(Boolean.TRUE);
      request.setParameter(VIEW_PARAM, VIEW_ALL_VALUE);
      if (log.isLoggingWarning()) {
        log.logWarning("Category " + category + " has superviewall checked on a product thumbnail template.");
      }
      setUserPref(productPageDefinition, request, "ALL");
    } else if (isFilter(request) || isAltFilter(request)) {
      if (isPrefUserChoiceAll(productPageDefinition, request, false)) {
        firstPageDefinition = productPageDefinition.getViewAllPageDefinition();
        currentPageDefinition = productPageDefinition.getViewAllPageDefinition();
        templatePageModel.setViewAll(true);
      } else {
        firstPageDefinition = productPageDefinition.getAlternateFilterPageDefinition();
        currentPageDefinition = productPageDefinition.getAlternateFilterPageDefinition();
      }
      templatePageModel.setFiltered(Boolean.TRUE);
    } else if (isPaging(request)) {
      if (isPrefUserChoiceAll(productPageDefinition, request, false)) {
        firstPageDefinition = productPageDefinition.getViewAllPageDefinition();
        currentPageDefinition = productPageDefinition.getViewAllPageDefinition();
        templatePageModel.setViewAll(true);
      } else {
        currentPageDefinition = productPageDefinition.getAlternatePageDefinition();
      }
      templatePageModel.setPaging(Boolean.TRUE);
    } else if (isPrefUserChoiceAll(productPageDefinition, request, false)) {
      firstPageDefinition = productPageDefinition.getViewAllPageDefinition();
      currentPageDefinition = productPageDefinition.getViewAllPageDefinition();
      templatePageModel.setViewAll(true);
    }
    
    templatePageModel.setSubcatProductCollection(Boolean.valueOf(productPageDefinition.isSubcatProductCollection()));
    
    if (!templatePageModel.isViewAll()) {
      setUserPref(productPageDefinition, request, "DEFAULT");
    }
    
    // this value is needed by evaluateContent further down the page
    request.setAttribute(FIRST_PAGE_KEY, firstPageDefinition);
    
    return currentPageDefinition;
  }
  
  /**
   * Returns true if the request parameter specifies "view all". If the request parameter is not "view all" and not "limit", returns the value of "default to all" on page definition.
   */
  private boolean isViewAll(final ProductTemplatePageDefinition pageDefinition, final DynamoHttpServletRequest request) {
    boolean isAll = isViewAll(VIEW_ALL_VALUE, request);
    final String view = request.getParameter(VIEW_PARAM);
    if (!isAll && (view == null || !view.equals(LIMIT_VALUE))) {
      isAll = pageDefinition.getDefaultToAll();
    }
    return isAll;
  }
  
  /**
   * Returns true if page parameter is greater than one, or equals one on a view all page.
   */
  private boolean isPaging(final DynamoHttpServletRequest request) {
    boolean isPaging = false;
    try {
      final String pageText = request.getParameter(PAGE_PARAM);
      if (pageText != null) {
        final int page = Integer.parseInt(pageText);
        if (page > 1) {
          isPaging = true;
        }
        if ((page == 1) && isViewAll(VIEW_ALL_VALUE, request)) {
          isPaging = true;
        }
      }
    } catch (final NumberFormatException nfe) {
      // do nothing
    }
    return isPaging;
  }
  
  /**
   * Returns true if the sort or filter parameters are specified.
   */
  private boolean isFilter(final DynamoHttpServletRequest request) {
    return !isEmptyParameter(FILTER_PARAM, request) || !isEmptyParameter(SORT_PARAM, request);
  }
  
  /**
   * Returns true if the alt filter parameter is "true"
   */
  private boolean isAltFilter(final DynamoHttpServletRequest request) {
    final String altFilter = request.getParameter(ALT_FILTER_PARAM);
    return ((altFilter != null) && altFilter.trim().equals("true"));
  }
  
  private String buildReturnParameters(final ProductTemplatePageDefinition pageDefinition, final DynamoHttpServletRequest request) {
    final List<String> returnParameters = new ArrayList<String>();
    final String[] returnParamNames = pageDefinition.getReturnParameters();
    
    if (returnParamNames.length > 0) {
      for (final String returnParamName : returnParamNames) {
        final String returnParam = request.getParameter(returnParamName);
        if (StringUtils.isNotEmpty(returnParam)) {
          returnParameters.add(returnParamName + "=" + returnParam);
        }
      }
    }
    
    return StringUtils.join(returnParameters, ",");
  }
  
  /**
   * Returns true if category has flash sales enabled
   */
  private boolean isFlashSale(final NMCategory category) {
    final List<RepositoryItem> flashSales = new ArrayList<RepositoryItem>((Set<RepositoryItem>) category.getPropertyValue("flashSaleSet"));
    boolean isFlashSale = false;
    for (int num = 0; num < flashSales.size() && !isFlashSale; num++) {
      final RepositoryItem flashSale = flashSales.get(num);
      if (flashSale != null) {
        // Possibly in future will want to validate flash sale timer start/end
        // Currently only need to verify flgEnable to indicate it is a flash sale category
        isFlashSale = (Boolean) flashSale.getPropertyValue("flgEnable");
      }
    }
    return isFlashSale;
  }
  
}
