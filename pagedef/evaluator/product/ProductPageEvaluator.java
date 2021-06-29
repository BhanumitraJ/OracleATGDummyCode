package com.nm.commerce.pagedef.evaluator.product;

import static com.nm.common.INMGenericConstants.ZERO;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.PageContext;

import atg.core.util.StringUtils;
import atg.nucleus.Nucleus;
import atg.repository.Repository;
import atg.repository.RepositoryException;
import atg.repository.RepositoryItem;
import atg.servlet.DynamoHttpServletRequest;
import atg.servlet.DynamoHttpServletResponse;
import atg.servlet.ServletUtil;

import com.nm.abtest.AbTestHelper;
import com.nm.ajax.catalog.product.utils.ProductUtils;
import com.nm.catalog.history.NMCatalogHistory;
import com.nm.catalog.history.NMCatalogHistoryItem;
import com.nm.catalog.navigation.Breadcrumb;
import com.nm.catalog.navigation.NMCategory;
import com.nm.commerce.NMOrderHolder;
import com.nm.commerce.NMProfile;
import com.nm.commerce.catalog.ImageShot;
import com.nm.commerce.catalog.NMProduct;
import com.nm.commerce.catalog.NMSuite;
import com.nm.commerce.catalog.NMSuperSuite;
import com.nm.commerce.catalog.VideoShot;
import com.nm.commerce.catalog.helper.ProductCutlineHelper;
import com.nm.commerce.catalog.helper.ProductDepictionTypeHelper;
import com.nm.commerce.catalog.helper.ProductTranslationHelper;
import com.nm.commerce.checkout.CheckoutComponents;
import com.nm.commerce.pagedef.definition.PageDefinition;
import com.nm.commerce.pagedef.definition.ProductPageDefinition;
import com.nm.commerce.pagedef.evaluator.ProductPriceHelper;
import com.nm.commerce.pagedef.evaluator.SimplePageEvaluator;
import com.nm.commerce.pagedef.model.PageModel;
import com.nm.commerce.pagedef.model.ProductPageModel;
import com.nm.commerce.pagedef.model.UIElementConstants;
import com.nm.commerce.pagedef.model.bean.RichRelevanceProductBean;
import com.nm.commerce.promotion.NMOrderImpl;
import com.nm.components.CommonComponentHelper;
import com.nm.configurator.utils.ConfiguratorConstants;
import com.nm.droplet.CatalogIdItemCodeProductLookup;
import com.nm.droplet.FilterRedirect;
import com.nm.droplet.ProductPageReferrer;
import com.nm.edo.constants.EmployeeDiscountConstants;
import com.nm.edo.exception.EmployeeDiscountsException;
import com.nm.edo.vo.ItemsListVO;
import com.nm.edo.vo.ProductDiscountServiceResponse;
import com.nm.formhandler.ProdHandler;
import com.nm.formhandler.ShoppingCartHandler;
import com.nm.returnseligibility.util.ReturnsEligibilityUtil;
import com.nm.search.bloomreach.BloomreachSearch;
import com.nm.search.bloomreach.BloomreachSearchConstants;
import com.nm.search.bloomreach.BloomreachSearchException;
import com.nm.search.bloomreach.BloomreachSearchTranslator;
import com.nm.servlet.NMRequestHelper;
import com.nm.storeinventory.InStoreSearchInput;
import com.nm.tms.core.TMSMessageContainer;
import com.nm.tms.utils.TMSDataDictionaryUtils;
import com.nm.twoclickcheckout.util.TwoClickCheckoutUtil;
import com.nm.utils.BRSessionData;
import com.nm.utils.BrandSpecs;
import com.nm.utils.GenericLogger;
import com.nm.utils.LocalizationUtils;
import com.nm.utils.NMAbstractCallable;
import com.nm.utils.NMFormHelper;
import com.nm.utils.NmoUtils;
import com.nm.utils.PCSSessionData;
import com.nm.utils.ProdCTLUtil;
import com.nm.utils.ProdSkuUtil;
import com.nm.utils.RichRelevanceClient;
import com.nm.utils.StringUtilities;
import com.nm.utils.SystemSpecs;

/**
 * Updates variables and sets up any redirects before page execution. Returns true if page content should be output.
 */
public class ProductPageEvaluator extends SimplePageEvaluator {
  
  protected final GenericLogger log = CommonComponentHelper.getLogger();
  protected static final GenericLogger staticLog = CommonComponentHelper.getLogger();
  SystemSpecs sysSpecs = CommonComponentHelper.getSystemSpecs();
  
  @Override
  protected PageModel allocatePageModel() {
    return new ProductPageModel();
  }
  
  @Override
  public boolean evaluate(final PageDefinition pageDefinition, final PageContext pageContext) throws Exception {
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

    boolean returnValue = super.evaluate(pageDefinition, pageContext);
    final DynamoHttpServletRequest request = getRequest();
    final DynamoHttpServletResponse response = getResponse();
    boolean isDynamicProductImage = false;
    boolean isDynamicSpecialInstructions = false;
    final BrandSpecs brandSpecs = (BrandSpecs) Nucleus.getGlobalNucleus().resolveName("/nm/utils/BrandSpecs");
    
    if (brandSpecs.isEnableAutomateCTL()) {
      final HttpSession session = ServletUtil.getCurrentRequest().getSession();
      @SuppressWarnings("unchecked")
      final List<String> prodIdList = (List<String>) session.getAttribute("CTLProducts");
      if (prodIdList != null) {
        if (prodIdList.size() >= 0) {
          session.setAttribute("CTLProducts", null);
        }
      }
      
    }
    String productId = request.getParameter("itemId");
    final String cid = request.getParameter("cid");
    final ProductPageDefinition productPageDefinition = (ProductPageDefinition) pageDefinition;
    productPageDefinition.setProdSelectBoxPath(productPageDefinition.getNonConfiguratorProdSelectBoxPath());
    
    if (!NmoUtils.isEmpty(productId) || !NmoUtils.isEmpty(cid)) {
      
      final PCSSessionData pcsSessionData = (PCSSessionData) request.resolveName("/nm/utils/PCSSessionData");
      if (!NmoUtils.isEmpty(productId)) {
        pcsSessionData.updateProductsViewed(productId);
      }
      
      final LocalizationUtils utils = (LocalizationUtils) Nucleus.getGlobalNucleus().resolveName("/nm/utils/LocalizationUtils");
      
      // if the cme parameter is present then we are going to change the itemId parameter
      // to the repository id of the first suite that the product is a member of.
      final String cme = request.getParameter("cme");
      final ProdSkuUtil prodSkuUtil = CommonComponentHelper.getProdSkuUtil();
      NMProduct nmProduct = prodSkuUtil.getProductObject(productId);
      if (cme != null && nmProduct != null) {
        final RepositoryItem[] suites = nmProduct.getParentSuiteRepositoryItems();
        if (suites != null && suites.length > 0) {
          productId = suites[0].getRepositoryId();
          request.setParameter("itemId", productId);
          // productId changed so create new nm product otherwise we
          // will use the previously created product above (which now knows
          // that it's not part of a suites).
          nmProduct = prodSkuUtil.getProductObject(productId);
        }
      }
      
      // check for cid (catalog_item); if passed, translate into the product id
      if (NmoUtils.isEmpty(productId)) {
        final CatalogIdItemCodeProductLookup productLookup = (CatalogIdItemCodeProductLookup) getRequest().resolveName("/nm/droplet/CatalogIdItemCodeProductLookup");
        getRequest().setParameter(CatalogIdItemCodeProductLookup.CATALOG_ITEM, cid);
        productLookup.service(getRequest(), getResponse(), false);
        productId = getRequest().getParameter(CatalogIdItemCodeProductLookup.NM_PRODUCT_ID);
        if (!NmoUtils.isEmpty(productId)) {
          pcsSessionData.updateProductsViewed(productId);
          nmProduct = prodSkuUtil.getProductObject(productId);
        }
      }
      
      if (nmProduct == null) {
        returnValue = false;
        redirectToProductNotFound(request);
      } else {
        
        if (nmProduct.getType() == 1) {
          
          final List<NMProduct> childProducts = nmProduct.getProductList();
          if (!childProducts.isEmpty()) {
            for (final NMProduct childProduct : childProducts) {
              if (childProduct.getFlgDynamicImageSpecialInstruction()) {
                isDynamicProductImage = true;
                request.setAttribute("isDynamicSiImage", "true");
                isDynamicSpecialInstructions = true;
              }
              if (childProduct.getFlgDynamicImageColor()) {
                isDynamicProductImage = true;
                request.setAttribute("isDynamicImage", "true");
              }
            }
          }
        } else if (nmProduct.getType() == 2) {
          
          final boolean hasConfigurableProduct = false;
          
          final NMSuperSuite ssuiteProduct = new NMSuperSuite(nmProduct.getId());
          final List<NMSuite> suiteProducts = ssuiteProduct.getSuiteList();
          for (final NMSuite suiteProduct : suiteProducts) {
            final List<NMProduct> childProducts = suiteProduct.getProductList();
            if (!childProducts.isEmpty()) {
              for (final NMProduct childProduct : childProducts) {
                if (childProduct.getFlgDynamicImageSpecialInstruction()) {
                  isDynamicProductImage = true;
                  isDynamicSpecialInstructions = true;
                  request.setAttribute("isDynamicSiImage", "true");
                }
                if (childProduct.getFlgDynamicImageColor()) {
                  isDynamicProductImage = true;
                  request.setAttribute("isDynamicImage", "true");
                }
              }
            }
          }
        } else {
          if (nmProduct.getFlgDynamicImageColor()) {
            isDynamicProductImage = true;
            request.setAttribute("isDynamicImage", "true");
            /*
             * if (nmProduct.getFlgConfigurable()) { request.setAttribute("", "true"); //FIXME What is this? }
             */
          }
          if (nmProduct.getFlgDynamicImageSpecialInstruction()) {
            isDynamicProductImage = true;
            isDynamicSpecialInstructions = true;
            request.setAttribute("isDynamicSiImage", "true");
          }
        }
        
        returnValue = super.evaluate(productPageDefinition, pageContext);
        final ProductPageModel pageModel = (ProductPageModel) getPageModel();
        final NMProfile nmProfile = CheckoutComponents.getProfile(request);
        
        // Retrieving the refreshable content for charge for returns or free returns.
        final ReturnsEligibilityUtil returnsEligibilityUtil = CommonComponentHelper.getReturnsEligibilityUtil();
        returnsEligibilityUtil.setReturnsChargeRefreshableContent(pageModel);
        final NMCatalogHistory catalogHistory = this.getNavHistory();
        if (null != catalogHistory) {
          final List<NMCatalogHistoryItem> navHistory = catalogHistory.getNavHistory();
          if (NmoUtils.isNotEmptyCollection(navHistory)) {
            final ArrayList<String> breadCrumbs = new ArrayList<String>();
            for (final NMCatalogHistoryItem nmNavHistory : navHistory) {
              final NMCategory currentCategory = nmNavHistory.getRepositoryItem();
              if (currentCategory != null) {
                breadCrumbs.add(currentCategory.getDisplayName());
              }
            }
            pageModel.setTrackerBreadCrumbs(breadCrumbs);
          }
        }
        pageModel.setBreadcrumb(new Breadcrumb(catalogHistory, ((ProductPageDefinition) pageDefinition).getBreadcrumbHideSiloLevel()));
        
        if (isDynamicSpecialInstructions) {
          pageModel.setMonogarammable(true);
        } else {
          pageModel.setMonogarammable(false);
        }
        
        if (isDynamicProductImage) {
          pageModel.setProdSelectBoxPath(productPageDefinition.getConfiguratorProdSelectBoxPath());
        } else {
          pageModel.setProdSelectBoxPath(productPageDefinition.getNonConfiguratorProdSelectBoxPath());
        }
        
        // now that we know the product lets see if we need to redirect
        if (requiresRedirect(request, nmProduct)) {
          return false;
        }
        
        final RepositoryItem dataSource = nmProduct.getDataSource();
        
        // if datasource is not null, this is a valid product. Otherwise redirect to product not found page
        if (dataSource != null) {
          final boolean populateSuccessful = populatePageModel(pageModel, nmProduct, request, productPageDefinition);
          if (!populateSuccessful) {
            returnValue = false;
            redirectToProductNotFound(request);
          } else {
            // this must be set directly to the request, not in model, for use by scenarios
            pageContext.getRequest().setAttribute("scenarioProduct", dataSource);
          }
        } else {
          returnValue = false;
          redirectToProductNotFound(request);
        }
        // start : The PDP page 'email a friend' icon suppression is enabled or disabled in pageModel
        
        final String profileCountry = nmProfile.getCountryPreference();
        if (utils.getEmailIconDisabledCountries().contains(profileCountry)) {
          pageModel.setDisableEmailIcon(true);
        } else {
          pageModel.setDisableEmailIcon(false);
        }
        
        if (getBrandSpecs().isHideEmailLink()) {
          pageModel.setDisableEmailIcon(true);
        }
        // end : The PDP page 'email a friend' icon suppression is enabled or disabled in pageModel
        pageModel.setVideoOnPage(hasVideoOnPage(pageModel.getValidAlternateViewsList()));
        // the editPage parameter is to avoid displaying Advertise Promotion text twice for Monogram products.issue reference: NMOBLDS-20023
        final String editPage = request.getParameter("editPage");
        if(StringUtilities.isNotBlank(editPage)){
        	pageModel.setEditPage(Boolean.valueOf(editPage));
        }
        
        // This method returns true only in the case of bloomreach
        final BRSessionData brSessionData = (BRSessionData) request.resolveName("/nm/utils/BRSessionData");
        final String rteParam = request.getParameter(BloomreachSearchConstants.RTE);
        try {
          boolean isDepictionRedirect = false;
          final String isDepictionRedirectRequestParamValue = request.getParameter(BloomreachSearchConstants.IS_DEPICTION_REDIRECT);
          if (!NmoUtils.isEmpty(isDepictionRedirectRequestParamValue) && BloomreachSearchConstants.TRUE_STRING.equals(isDepictionRedirectRequestParamValue)) {
            isDepictionRedirect = true;
          }
          if (!StringUtils.isEmpty(rteParam) && rteParam.contains(BloomreachSearchConstants.BR_SEARCH) && !StringUtils.isEmpty(brSessionData.getSearchTerm())
                  && null != brSessionData.getBrTotalRecordCountMap() && !isDepictionRedirect) {
            getPDPPagination(request, pageModel);
          }
        } catch (final BloomreachSearchException bre) {
          staticLog.logError("ProductPageEvaluator : Error occured while generating PDP pagiantion" + bre);
        }
        // Data Dictionary Attributes population.
        final TMSDataDictionaryUtils dataDictionaryUtils = CommonComponentHelper.getDataDictionaryUtils();
        dataDictionaryUtils.processTMSDataDictionaryAttributes(pageDefinition, null, nmProfile, pageModel, new TMSMessageContainer());
      }
    } else {
      returnValue = false;
      redirectToProductNotFound(request);
    }
    return returnValue;
  }
  
  /**
   * Gets the video on page.
   * 
   * @param validAlternateViewsList
   *          the valid alternate views list
   * @return the video on page
   */
  private boolean hasVideoOnPage(final List<ImageShot> validAlternateViewsList) {
    boolean hasVideoOnPage = false;
    if (NmoUtils.isNotEmptyCollection(validAlternateViewsList)) {
      for (final ImageShot imageShot : validAlternateViewsList) {
        if (imageShot instanceof VideoShot) {
          hasVideoOnPage = true;
          break;
        }
      }
    }
    return hasVideoOnPage;
  }
  
  /**
   * This method is used get the current,next and previous position of the product and set that start and rows parameters for the query.
   * 
   * @param request
   *          the request
   * @param pageModel
   *          the page model
   * @return the PDP pagination
   * @throws BloomreachSearchException
   *           the bloomreach search exception
   * @throws UnsupportedEncodingException
   */
  private void getPDPPagination(final DynamoHttpServletRequest request, final ProductPageModel pageModel) throws BloomreachSearchException, UnsupportedEncodingException {
    final BloomreachSearch brSearch = BloomreachSearch.getInstance();
    final BloomreachSearchTranslator bloomreachSearchTranslator = BloomreachSearchTranslator.getInstance();
    final DynamoHttpServletResponse response = getResponse();
    final NMProfile nmProfile = (NMProfile) request.resolveName("/atg/userprofiling/Profile");
    
    final BRSessionData brSessionData = (BRSessionData) request.resolveName("/nm/utils/BRSessionData");
    String searchTerm = request.getParameter(BloomreachSearchConstants.PARAM_Q);
    if (StringUtils.isEmpty(searchTerm)) {
      searchTerm = brSessionData.getSearchTerm();
    }
    pageModel.setSearchTerm(searchTerm);
    /*
     * As type of the property brTotalRecordCountMap in BRSessionData is Map<String, Integer>, so taking value from the Map as an Iteger and followed by that checking if null.
     */
    final Integer totalSearchCountInteger = brSessionData.getBrTotalRecordCountMap().get(searchTerm + BloomreachSearchConstants.UNDERSCORE + nmProfile.getCountryPreference());
    if (null == totalSearchCountInteger) {
      return;
    }
    // Unboxing totalSearchCountInteger
    final int totalSearchCount = totalSearchCountInteger.intValue();
    final String searchItemIndex = request.getParameter(BloomreachSearchConstants.CURRENT_ITEM_COUNT);
    int start = 0;
    int rows = 0;
    int currentPosition = 0;
    if (!StringUtils.isEmpty(searchItemIndex) && totalSearchCount != 0) {
      pageModel.setTotalCount(totalSearchCount);
      if (!StringUtils.isEmpty(searchItemIndex)) {
        currentPosition = Integer.valueOf(searchItemIndex.trim());
        pageModel.setCurrentPosition(currentPosition);
        pageModel.setPerviousProduct(currentPosition - 1);
        pageModel.setNextProduct(currentPosition + 1);
        if (currentPosition > 1) {
          pageModel.setDisplayPrev(true);
        }
        if (currentPosition < totalSearchCount) {
          pageModel.setDisplayNext(true);
        }
        if (currentPosition == 1) {
          start = currentPosition - 1;
          rows = 2;
        } else if (currentPosition == totalSearchCount) {
          start = totalSearchCount - 2;
          rows = 2;
        } else {
          start = currentPosition - 2;
          rows = 3;
        }
        request.setParameter(BloomreachSearchConstants.START, start);
        request.setParameter(BloomreachSearchConstants.ROWS, rows);
      }
      final Map<String, Object> resultMap = brSearch.getBRSearchResult(request, response, pageModel);
      bloomreachSearchTranslator.getProductUrl(resultMap, pageModel, searchItemIndex, request, brSessionData);
    }
  }
  
  public boolean populatePageModel(final ProductPageModel pageModel, final NMProduct nmProduct, final DynamoHttpServletRequest request, final ProductPageDefinition productPageDefinition)
          throws Exception {
    
    String focusProductId = "";
    final String elimSuitesAbTestGroup = "";
    pageModel.setProduct(nmProduct);
    
    if (nmProduct.getFlgConfigurable()) {
      pageModel.setConfigurableProduct(true);
    }
    setupProductTranslation(pageModel, nmProduct.getId(), request);
    final LocalizationUtils utils = (LocalizationUtils) Nucleus.getGlobalNucleus().resolveName("/nm/utils/LocalizationUtils");
    // best of beta, render product lists in evaluator instead of in jsp page
    // boolean isOldModel = productPageDefinition.getOldModel();
    final String needHelpLayout = productPageDefinition.getNeedHelpLayout();
    
    final ProdHandler prodHandler = getProdHandler();
    prodHandler.setProductId(nmProduct.getId());
    prodHandler.setActualProductId(request.getParameter("eItemId"));
    pageModel.setDisplayMobile(NMRequestHelper.isMobileRequest(getRequest()));
    if (nmProduct.getType() == 2) {
      final String selectedSuiteId = request.getParameter("selectedSuiteId");
      final NMSuperSuite nmSuperSuite = new NMSuperSuite(nmProduct.getId());
      if (StringUtilities.isNotEmpty(selectedSuiteId)) {
        nmSuperSuite.setCurrentSuiteId(selectedSuiteId);
      }
      final NMSuite subSuite = nmSuperSuite.getCurrentSelectedSuite();
      // if there is no displayable subsuite of a super suite product, then return false, to redirect to product not found page
      if (subSuite == null) {
        return false;
      } else {
        pageModel.setSelectedSuiteProduct(subSuite);
      }
      
      pageModel.setSuiteList(nmSuperSuite.getSuiteList());
    }
    List<NMProduct> productList;
    List<RelatedProductBean> relatedProducts;
    
    pageModel.setNeedHelpLayout(needHelpLayout);
    
    boolean sameDesigner = true;
    
    String prevDesignerName = "";
    final List<NMProduct> allProductList = nmProduct.getProductList();
    final Iterator<NMProduct> productListIterator = allProductList.iterator();
    
    while (productListIterator.hasNext()) {
      final NMProduct nmProd = productListIterator.next();
      final String designerName = nmProd.getWebDesignerName();
      if (StringUtilities.isEmpty(prevDesignerName)) {
        prevDesignerName = designerName;
      } else {
        if (!prevDesignerName.equals(designerName)) {
          sameDesigner = false;
        }
      }
    }
    
    pageModel.setPageDesignerName(prevDesignerName);
    pageModel.setAllDesignersSame(sameDesigner);
    
    boolean isAllShopRunnerEligible = false;
    // SHOPRUN-136 changes Start: Set pagemodel attribute based on alipay eligibility for a Product
    boolean isAlipayEligibleProduct = false;
    boolean alipayEnabled = false;
    if (null != pageModel.getElementDisplaySwitchMap().get(UIElementConstants.ALIPAY)) {
      alipayEnabled = pageModel.getElementDisplaySwitchMap().get(UIElementConstants.ALIPAY);
    }
    if (nmProduct.getType() == NMSuperSuite.TYPE) {
      final NMSuite curSuite = pageModel.getSelectedSuiteProduct();
      if (curSuite != null) {
        isAllShopRunnerEligible = curSuite.getIsShopRunnerEligible();
        if (alipayEnabled) {
          isAlipayEligibleProduct = curSuite.isAlipayEligible();
        }
      }
    } else if (nmProduct.getType() == NMSuite.TYPE || nmProduct.getType() == NMProduct.TYPE) {
      isAllShopRunnerEligible = nmProduct.getIsShopRunnerEligible();
      if (alipayEnabled) {
        isAlipayEligibleProduct = nmProduct.isAlipayEligible();
      }
    }
    
    pageModel.setAllShopRunnerEligible(isAllShopRunnerEligible);
    if (alipayEnabled) {
      pageModel.setAlipayEligibile(isAlipayEligibleProduct);
    }
    // SHOPRUN-136 Changes end: Set pagemodel attribute based on alipay eligibility for a Product
    
    loadProductInfoTabs(nmProduct, pageModel);
    if (nmProduct.getType() > 0) {
      pageModel.setSuiteSizeGuide(ProductPageEvaluatorUtils.findSuiteSizeGuide(nmProduct));
    }
    
    if (nmProduct.getType() == 2) {
      productList = ProductListHelper.setupLineItemsProductList(pageModel.getSelectedSuiteProduct(), prodHandler);
    } else {
      productList = ProductListHelper.setupLineItemsProductList(nmProduct, prodHandler);
    }
    // Compute Employee Discount Eligibility
    final NMProfile nmProfile = CheckoutComponents.getProfile(request);
    computeEmployeeDiscountEligibility(nmProfile, productList);
    
    String elimsGroup = AbTestHelper.getAbTestValue(request, AbTestHelper.ELIMS_SUITE_GROUP);
    if (StringUtilities.isNotEmpty(elimsGroup) && StringUtilities.areEqualIgnoreCase(elimsGroup, "Y")) {
      focusProductId = request.getParameter("focusProductId");
    } else {
      focusProductId = null;
    }
    //limit this only for LC brand.
	
    List<NMProduct> LCDisplayProducts = null;
    if (StringUtilities.isNotEmpty(elimsGroup) && StringUtilities.areEqualIgnoreCase(elimsGroup, "Y") && CommonComponentHelper.getSystemSpecs().getProductionSystemCode().equalsIgnoreCase("LC")) {
    	LCDisplayProducts = ProductListHelper.buildLCDisplayProductList(nmProduct, prodHandler, getRequest(), true);
    	if (LCDisplayProducts != null) {
    		productList.addAll(LCDisplayProducts);
      }
    }
    
    if (null != focusProductId) {
      if (!focusProductId.equalsIgnoreCase("")) {
        productList = setSuitesFocusProductIdList(focusProductId, productList);
      }
    }
    
    pageModel.setFocusProductId(focusProductId);
    
    pageModel.setDisplayProductList(productList);
    // set productlist with single sku for suite products
    if (nmProduct.getType() == 1) {
      ProductUtils.setSingleSkuForSuiteItems(pageModel);
    }
    // if product is "isDynamicImage", meaning the product image can change dynamically according to customer interaction
    if (Boolean.valueOf((String) request.getAttribute(ConfiguratorConstants.IS_DYNAMIC_IMAGE))) {
      final ProdSkuUtil prodSkuUtil = CommonComponentHelper.getProdSkuUtil();
      pageModel.setProductColorSwatchData(prodSkuUtil.buildProdSkuColorData(productList));
    }
    pageModel.setProductPageMap(buildProductsOnPageMap(productList));
    if (nmProduct.getType() == 2 || productList.size() >= 5) {
      pageModel.setIsLongSuperSuite(true);
    }
    
    // set up Complete The Look display product list. This MUST be done before choice stream is called
    // to prevent choice stream from returning duplicate products.
    final ProdCTLUtil prodCtlUtil = (ProdCTLUtil) Nucleus.getGlobalNucleus().resolveName("/nm/utils/ProdCTLUtil");
    final boolean showLiveProductsOnly = productPageDefinition.getShowLiveRelatedProductsOnly() == null ? false : productPageDefinition.getShowLiveRelatedProductsOnly().booleanValue();
    relatedProducts = ProductListHelper.setupCompleteTheLookProductList(nmProduct, prodHandler, getRequest(), showLiveProductsOnly);
    pageModel.setDisplayCTLProductList(relatedProducts);
    pageModel.setDisplayCTLTitle(prodCtlUtil.getDisplayLabel());
    pageModel.setProdHasCTLItems(prodCtlUtil.isProdHasCTL());
    
    /* Setup Manual More Colors */
    if (productPageDefinition.isManualMoreColors()) {
      final List<RelatedProductBean> moreColorsProductList = ProductListHelper.buildColorsRelatedProductBeanList(nmProduct, prodHandler, getRequest(), showLiveProductsOnly);
      pageModel.setDisplayMoreColorsList(moreColorsProductList);
      final String restrictedProducts = getMoreColorsProductsAsRestrictedForRR(moreColorsProductList);
      if (StringUtilities.isNotEmpty(restrictedProducts)) {
        pageModel.setRestrictedPrdsToRR(restrictedProducts);
      }
    }
    
    // if ( !isOldModel ) {
    // // set up You May Also Like display product list
    // ExecutorService executor = Executors.newSingleThreadExecutor();
    // Callable<List<String>> callable = new ChoiceStreamCallable( nmProduct, prodHandler );
    // Future<List<String>> choiceStreamFuture = executor.submit( callable );
    // executor.shutdown();
    // pageModel.setChoiceStreamProductIdList( choiceStreamFuture );
    // }
    
    determinePageProductDepiction(pageModel, nmProduct);
    
    final boolean isChanel = "chanel".equalsIgnoreCase(nmProduct.getWebDesignerName());
    final boolean isHermes = "hermes".equalsIgnoreCase(nmProduct.getWebDesignerName());
    pageModel.setChanel(isChanel);
    pageModel.setHermes(isHermes);
    if (isChanel) {
      pageModel.appendBodyClass("chanel");
    }
    
    NMProduct imageShotProduct = nmProduct;
    if (getPageModel().getPageDefinition().getContentPath().contains("editProd.jsp")) {
      imageShotProduct = useSuiteProductWhenParameterPresent(nmProduct);
    }
    
    final ProductShotHelper shotHelper = ProductShotHelper.getInstance();
    
    ArrayList<ImageShot> validAlternateViewsList = shotHelper.setupProductImageShots(imageShotProduct, request);
    
    if (StringUtilities.isNotEmpty(elimsGroup) && StringUtilities.areEqualIgnoreCase(elimsGroup, "Y")) {
      focusProductId = request.getParameter("focusProductId");
    } else {
      focusProductId = null;
    }
    
    if (null != focusProductId) {
      if (!focusProductId.equalsIgnoreCase("")) {
        if (nmProduct.getType() == 1) {
          validAlternateViewsList = setSuitesAlternateViewsFocusProductIdList(focusProductId, validAlternateViewsList);
        }
      }
    }
    
    pageModel.setDisplayAlternateViews(validAlternateViewsList.size() > 1);
    pageModel.setValidAlternateViewsList(validAlternateViewsList);
    
    pageModel.setReferringNavType(getReferringNavType());
    final ProductPagePrevNextHelper prevNextHelper = new ProductPagePrevNextHelper(request);
    pageModel.setPagingPrevNext(prevNextHelper.getPrevAndNextProducts());
    
    final ProductPriceHelper priceHelper = new ProductPriceHelper();
    
    /* Rich Revelavance ymal recommendations from server side API call */
    final SystemSpecs systemSpecs = CommonComponentHelper.getSystemSpecs();
    /*
     * INT-1955 ,2258 and 2511 story code changes country check for server side RR integration.
     */
    if (systemSpecs.isRichRelevanceServerSideEnabled() && systemSpecs.isRrServerSideCountryEnabled()) {
      final RepositoryItem datasource = nmProduct.getDataSource();
      final Collection collection = (Collection) datasource.getPropertyValue("productFlags");
      final Object collectionItem = "flgMerchManual";
      boolean manulSuggestCM2 = false;
      if (collection.contains(collectionItem)) {
        manulSuggestCM2 = true;
      }
      final Boolean isBeautyProduct = utils.checkBeautyProduct(nmProduct);
      if (!(isBeautyProduct && (pageModel.isChanel() || pageModel.isHermes()) || manulSuggestCM2)) {
        final RichRelevanceClient ymalData = (RichRelevanceClient) request.resolveName("/nm/utils/RichRelevanceClient");
        final HttpSession session = request.getSession();
        final String sessionId = session.getId();
        final String userId = getProfile().getEdwCustData().getUcid();
        final String rrBlackList = prodHandler.getRichRelevanceBlacklistItems().replace("&", "|");
        final RichRelevanceProductBean ymalProducts =
                ymalData.getProductRecordsFromRichRelevance(userId, sessionId, nmProduct.getId(), "item_page.horizontal", rrBlackList, getProfile().getCountryPreference());
        pageModel.setYmalProducts(ymalProducts);
        pageModel.setServerSideYmal(true);
        pageModel.setServerSideOOSR(true);
        if (!pageModel.getProduct().getIsShowable() && !pageModel.getProduct().getIsGWPItem()) {
          final RichRelevanceProductBean oosrProducts =
                  ymalData.getProductRecordsFromRichRelevance(userId, sessionId, nmProduct.getId(), "item_page.out_of_stock", rrBlackList, getProfile().getCountryPreference());
          pageModel.setOosRrProducts(oosrProducts);
        }
        
      }
    }
    // add inStoreSearch input to the pageModal based on the url
    final String location = request.getParameter("locationInput");
    final String radius = request.getParameter("radiusInput");
    final boolean allStores = NmoUtils.coalesce(request.getParameter("allStoresInput"), "false").equalsIgnoreCase("true");
    if (InStoreSearchInput.isGeoSearch(location, allStores, radius)) {
      final InStoreSearchInput inStoreSearchInput = new InStoreSearchInput(location, radius, allStores);
      pageModel.setInStoreSearchInput(inStoreSearchInput);
    }
    if (pageModel.isInternational()) {
      // Get the Assistance shipping and delivery category id for country.
      pageModel.setShippingAndDeliveryPolicyCatId(utils.getLocalizedShippingAndDeliveryPolicyCatId(getProfile().getCountryPreference()));
    }
    
    if (getBrandSpecs().isSameDayDeliveryEnabled()) {
      CommonComponentHelper.getSameDayDeliveryUtil().populateProductPageModel(pageModel);
    }
    
    return true;
  }
  
  /**
   * Re-Orders the Suite Product ALT IMAGES displayed on the PDP page using the focusProductId that is appended on the URL of the Thumbnails page. This is set in CM3 as a 'Display as Groups' and
   * 'Drive to Group' settings. This takes that focusProductId Images and makes it the first listed and displayed on alt list.
   * 
   * @return new validAlternateViewsList
   * 
   */
  static ArrayList<ImageShot> setSuitesAlternateViewsFocusProductIdList(final String fPID, final ArrayList<ImageShot> validAlternateViewsList) {
    ImageShot imageShot = null;
    final int vavlSize = validAlternateViewsList.size();
    final ArrayList<ImageShot> newValidalternateViewList = new ArrayList<ImageShot>();
    
    // find the focusProdId alt images and set them to the front of the list
    for (int pos = 0; pos < vavlSize; pos++) {
      imageShot = validAlternateViewsList.get(pos);
      
      final String imgShot = imageShot.getProductId();
      
      if (imgShot.equalsIgnoreCase(fPID)) {
        
        newValidalternateViewList.add(imageShot);
        
      }
      
    }
    // now add the others in their order to the new list.
    for (int pos = 0; pos < vavlSize; pos++) {
      imageShot = validAlternateViewsList.get(pos);
      
      final String imgShot = imageShot.getProductId();
      
      if (!imgShot.equalsIgnoreCase(fPID)) {
        
        newValidalternateViewList.add(imageShot);
        
      }
    }
    
    return newValidalternateViewList;
  }
  
  /**
   * Re-Orders the Suite Products displayed on the PDP page using the focusProductId that is appended on the URL of the Thumbnails page. This is set in CM3 as a 'Display as Groups' and 'Drive to
   * Group' settings. This takes that focusProductId and makes it the first listed and displayed.
   * 
   * @return new productList
   * 
   */
  static List<NMProduct> setSuitesFocusProductIdList(final String fPID, final List<NMProduct> productList) {
    NMProduct nmProduct = null;
    final int rplSize = productList.size();
    final List<NMProduct> newProductList = new ArrayList<NMProduct>();
    
    for (int pos = 0; pos < rplSize; pos++) {
      nmProduct = productList.get(pos);
      
      // this will reset isfirstshowable since we are changing it
      if (nmProduct.getIsInSuite()) {
        if (nmProduct.getIsFirstShowableProductInSuite()) {
          nmProduct.setIsFirstShowableProductInSuite(false);
        }
      }
      
      // this will be the new First Product in the productList used
      if (nmProduct.getId().equals(fPID)) {
        nmProduct.setIsFirstShowableProductInSuite(true);
        newProductList.add(nmProduct);
      }
    }
    
    // Now take the rest of the products and add them to the new list for their new order
    for (int newpos = 0; newpos < rplSize; newpos++) {
      nmProduct = productList.get(newpos);
      
      if (nmProduct.getIsInSuite()) {
        if (!nmProduct.getId().equals(fPID)) {
          newProductList.add(nmProduct);// this will be the new productList used
        }
      }
    }
    
    return newProductList;
    
  }
  
  private void redirectToProductNotFound(final DynamoHttpServletRequest request) throws IOException {
    final DynamoHttpServletResponse response = getResponse();
    String queryString = request.getQueryString();
    if (queryString != null && queryString.length() > 0) {
      queryString = "?" + queryString;
    }
    final String redirectUrl = NMFormHelper.reviseUrl("/product/productNotFound.jsp");
    response.sendRedirect(redirectUrl + queryString);
    response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
  }
  
  /**
   * Uses the FilterRedirect droplet's evaluations methods to determine if the user has access to the category specified. This should redirect if they have the staging category in their path and they
   * are not authorized or product is a lookbook only product with the BVD siCode
   * 
   * @return true if the system has redirected
   * @throws IOException
   */
  private boolean requiresRedirect(final DynamoHttpServletRequest request, final NMProduct product) throws IOException {
    boolean isRedirected = false;
    final FilterRedirect filter = (FilterRedirect) request.resolveName("/nm/droplet/FilterRedirect");
    request.setParameter(FilterRedirect.CATEGORY_TO_FILTER, getBrandSpecs().getHiddenCategoryId());
    request.setParameter(FilterRedirect.FILTER_TYPE, "HIDDEN");
    isRedirected = filter.restrictCategory(request);
    if (isRedirected) {
      final String redirectUrl = NMFormHelper.reviseUrl(getBrandSpecs().getCategoryNotFoundPath());
      getResponse().sendRedirect(redirectUrl);
      getResponse().setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
    } else {
      final List<String> siCodes = product.getSICodes();
      if (siCodes != null && siCodes.contains("BVD")) {
        redirectToProductNotFound(request);
        isRedirected = true;
      }
    }
    return isRedirected;
  }
  
  private String getReferringNavType() {
    final ProductPageReferrer ppr = (ProductPageReferrer) getRequest().resolveName("/nm/droplet/ProductPageReferrer");
    return ppr.getReferrerType(getRequest(), getNavHistory());
  }
  
  private ProdHandler getProdHandler() {
    return (ProdHandler) getRequest().resolveName("/nm/formhandler/ProdHandler");
  }
  
  /**
   * build the product map for javascript matrix on product page
   * 
   * @param displayProduct
   * @return
   */
  private Map<String, NMProduct> buildProductsOnPageMap(final List<NMProduct> displayProduct) {
    final Map<String, NMProduct> productMap = new HashMap<String, NMProduct>();
    final Iterator<NMProduct> prodIterator = displayProduct.iterator();
    NMProduct prod = null;
    while (prodIterator.hasNext()) {
      prod = prodIterator.next();
      productMap.put(prod.getId(), prod);
    }
    return productMap;
  }
  
  private void loadProductInfoTabs(NMProduct nmProduct, final ProductPageModel pageModel) {
    if (nmProduct.getType() == 2) {
      nmProduct = pageModel.getSelectedSuiteProduct();
    }
    int tabCount = 1; // Will always have overview content
    // Overview tab
    String overviewTitle = (String) nmProduct.getPropertyValue("cutlineOverviewTitle");
    overviewTitle = StringUtilities.isEmpty(overviewTitle) ? "Details" : overviewTitle;
    pageModel.setOverviewTitle(overviewTitle);
    pageModel.setProductCopyTop((String) nmProduct.getPropertyValue("suiteCopyTop"));
    pageModel.setProductCopyBottom((String) nmProduct.getPropertyValue("suiteCopyBottom"));
    pageModel.setProductCutline(ProductCutlineHelper.getLongDescriptionCutLine(nmProduct));
    
    if (pageModel.getNeedHelpLayout().equalsIgnoreCase("tab")) {
      tabCount += 1;
    }
    
    // Extra-Details tab OR Designer tab
    final String cutlineDetails = (String) nmProduct.getPropertyValue("cutlineDetails");
    if (StringUtilities.isNotEmpty(cutlineDetails)) {
      String cutlineDetailsTitle = (String) nmProduct.getPropertyValue("cutlineDetailsTitle");
      if (StringUtilities.isEmpty(cutlineDetailsTitle)) {
        cutlineDetailsTitle = "Extras";
      }
      if (StringUtilities.areEqualIgnoreCase(cutlineDetailsTitle, "Designer")) {
        tabCount += 1;
        pageModel.setDesignerContent(cutlineDetails);
      } else {
        tabCount += 1;
        pageModel.setDetailsContent(cutlineDetails);
        pageModel.setDetailsTitle(cutlineDetailsTitle);
      }
    }
    if (nmProduct.getImagePresent().get("sn")) {
      tabCount += 1;
      pageModel.setSwatchImageUrl(nmProduct.getImageOrShimUrl().get("sn"));
    }
    if (pageModel.getProductTranslationRI() != null) {
      tabCount += 1;
    }
    final boolean displayTabs = tabCount > 1 ? true : false;
    pageModel.setDisplayTabs(displayTabs);
  }
  
  private void setupProductTranslation(final ProductPageModel pageModel, final String productId, final DynamoHttpServletRequest request) {
    final Locale locale = request.getLocale();
    String localeName = locale.getDisplayName();
    final StringTokenizer st = new StringTokenizer(localeName, "(");
    if (st.hasMoreTokens()) {
      localeName = st.nextToken().trim();
    }
    final String langCode = ProductTranslationHelper.getLanguageCode(productId, locale);
    final RepositoryItem prodTransRI = ProductTranslationHelper.getProductTranslationRI(productId, langCode);
    pageModel.setLangCode(langCode);
    pageModel.setLocaleName(localeName);
    pageModel.setProductTranslationRI(prodTransRI);
  }
  
  private void determinePageProductDepiction(final ProductPageModel pageModel, final NMProduct nmProduct) throws Exception {
    final String depiction = ProductDepictionTypeHelper.getProductDepiction(nmProduct);
    pageModel.setDepiction(depiction);
  }
  
  /*
   * Returns a boolean value based on the string entered by the user for the HTML fragment
   */
  private boolean getHtmlFlagValue(final String htmlFragId) {
    boolean htmlFlagValue = false;
    
    final String fragStringValue = getHtmlFragValue(htmlFragId);
    if (fragStringValue.equalsIgnoreCase("true") || fragStringValue.equalsIgnoreCase("1") || fragStringValue.equalsIgnoreCase("on") || fragStringValue.equalsIgnoreCase("enable")
            || fragStringValue.equalsIgnoreCase("enabled")) {
      htmlFlagValue = true;
    }
    return htmlFlagValue;
  }
  
  private String getHtmlFragValue(final String htmlFragId) {
    String fragValue = "";
    try {
      final Repository productRepository = (Repository) getRequest().resolveName("/atg/commerce/catalog/ProductCatalog");
      final RepositoryItem frag = productRepository.getItem(htmlFragId, "htmlfragments");
      if (frag != null) {
        fragValue = (String) frag.getPropertyValue("frag_value");
        if (fragValue == null) {
          fragValue = "";
        }
      }
    } catch (final RepositoryException re) {
      if (log.isLoggingError()) {
        log.logError("Error in ProductPageEvaluator while attempting to retrieve html fragment '" + htmlFragId + "': " + re.getMessage());
      }
    }
    
    return fragValue;
  }
  
  /**
   * 
   * @author nmwps
   * 
   */
  private static class ChoiceStreamCallable extends NMAbstractCallable<List<String>> {
    private final NMProduct product;
    private final ProdHandler prodHandler;
    
    public ChoiceStreamCallable(final NMProduct product, final ProdHandler prodHandler) {
      this.product = product;
      this.prodHandler = prodHandler;
    }
    
    @Override
    public List<String> run() {
      List<String> returnValue = null;
      try {
        returnValue = prodHandler.getChoiceStreamProductIds(product, false);
      } catch (final Exception exception) {
        if (staticLog.isLoggingError()) {
          staticLog.logError("ChoiceStreamCallable call(): " + exception.getMessage());
        }
      }
      return returnValue;
    }
  }
  
  private NMProduct useSuiteProductWhenParameterPresent(final NMProduct nmProduct) {
    final String suiteId = getRequest().getParameter("suiteId");
    
    if (!NmoUtils.isEmpty(suiteId)) {
      final ProdSkuUtil prodSkuUtil = CommonComponentHelper.getProdSkuUtil();
      final NMProduct nmSuite = prodSkuUtil.getProductObject(suiteId);
      if (nmSuite != null) {
        return nmSuite;
      } else {
        log.warn("Invalid suiteId " + suiteId + " passed to " + getPageModel().getPageDefinition().getContentPath());
      }
    }
    
    return nmProduct;
  }
  
  /**
   * this method constructs the more colors product ids String for RichReleveance to restrict to show YMAL in product detail page.
   */
  private String getMoreColorsProductsAsRestrictedForRR(final List<RelatedProductBean> moreColorsProductList) {
    final Iterator<RelatedProductBean> iter = moreColorsProductList.iterator();
    String restrictedProducts = "";
    while (iter.hasNext()) {
      final String productId = iter.next().getProduct().getId();
      if (productId != null) {
        if (StringUtilities.isNotEmpty(restrictedProducts)) {
          restrictedProducts += "&";
        }
        restrictedProducts += productId;
      }
    }
    return restrictedProducts;
  }
  
  private void computeEmployeeDiscountEligibility(final NMProfile nmProfile, final List<NMProduct> productList) throws EmployeeDiscountsException {
    try {
      if (nmProfile.isAuthorized() && nmProfile.isAssociateLoggedIn()) {
        final ProductDiscountServiceResponse servResp = CommonComponentHelper.getEmployeeDiscountsUtil().invokeProductDiscountService(nmProfile, productList);
        if (servResp != null && servResp.getItemMap() != null && !servResp.getItemMap().isEmpty()) {
          final Map<String, ItemsListVO> itemMap = servResp.getItemMap();
          for (final NMProduct childProd : productList) {
            boolean itemDiscounted = false;
            final String key = childProd.getCmosCatalogItem();
            final ItemsListVO item = itemMap.get(key);
            if (item != null) {
              final String discountPercent = item.getEmployeeDiscountPercentage();
              if (!StringUtils.isBlank(discountPercent) && Integer.parseInt(discountPercent) > ZERO) {
                itemDiscounted = true;
              }
            }
            childProd.setEmployeeDiscountEligible(itemDiscounted);
          }
        }
      }
    } catch (final EmployeeDiscountsException e) {
      if (log.isLoggingError()) {
        log.error("Error in Product Discount Service call for PDP" + e.getMessage());
      }
      /* EDO changes for PDP */
      if (e.getErrorCode().equalsIgnoreCase(EmployeeDiscountConstants.INTERNAL_SERVICE_ERROR) || e.getErrorCode().equalsIgnoreCase(EmployeeDiscountConstants.COMMUNICATION_FAILURE)) {
        getRequest().getSession().setAttribute(EmployeeDiscountConstants.SYNC_FAIL, e.getMessage());
        nmProfile.setPropertyValue(EmployeeDiscountConstants.IS_ASSOCIATE_LOGGED_IN, true);
      }
    }
    
  }
}
