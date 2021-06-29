package com.nm.commerce.pagedef.evaluator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.PageContext;

import atg.nucleus.Nucleus;
import atg.repository.Repository;
import atg.repository.RepositoryItem;
import atg.servlet.DynamoHttpServletRequest;

import com.nm.abtest.AbTestHelper;
import com.nm.catalog.navigation.Breadcrumb;
import com.nm.catalog.navigation.NMCategory;
import com.nm.catalog.navigation.NMCategory.FlashSaleStatus;
import com.nm.catalog.template.TemplateUtils;
import com.nm.collections.NMPromotion;
import com.nm.commerce.NMOrderHolder;
import com.nm.commerce.NMProfile;
import com.nm.commerce.checkout.CheckoutComponents;
import com.nm.commerce.pagedef.definition.PageDefinition;
import com.nm.commerce.pagedef.definition.TemplatePageDefinition;
import com.nm.commerce.pagedef.model.PageModel;
import com.nm.commerce.pagedef.model.TemplatePageModel;
import com.nm.commerce.pagedef.model.bean.SortOption;
import com.nm.commerce.promotion.NMOrderImpl;
import com.nm.components.CommonComponentHelper;
import com.nm.droplet.FilterRedirect;
import com.nm.droplet.NMProtocolChange;
import com.nm.email.EmailInfo;
import com.nm.formhandler.ShoppingCartHandler;
import com.nm.search.GenericPagingBean;
import com.nm.search.GenericSearchUtil;
import com.nm.twoclickcheckout.util.TwoClickCheckoutUtil;
import com.nm.utils.GenericLogger;
import com.nm.utils.LocalizationUtils;
import com.nm.utils.NmoUtils;
import com.nm.utils.PersonalizedCustData;
import com.nm.utils.ProdSkuUtil;
import com.nm.utils.PromotionsHelper;
import com.nm.utils.StringUtilities;

/**
 * Performs additional checks for valid, public category. Establishes refined template page definition (P9 may become P9ViewAll).
 */
public class TemplatePageEvaluator extends SimplePageEvaluator {

  private static final String US_COUNTRY_CODE = "US";
  protected static final String SUPER_ALL_VALUE = "superall";
  protected static final String VIEW_PARAM = "view";
  protected static final String PAGE_PARAM = "page";
  protected static final String PAGE_SIZE_PARAM = "pageSize";

  private final static String FEATURED_PROD_FRAG_ID = "featuredProdCategoryId";
  private final static String DEFAULT_PROD_FRAG_ID = "defaultProdCategoryId";
  private final static String DRAWER_FEATURED_PROD_FRAG_ID = "drawerFeaturedProductsCategoryId";
  private static final String GRAPHIC_HEADER2_ABTEST = "graphicHeader2ABTest";

  protected GenericLogger log = CommonComponentHelper.getLogger();

  /** The template utils. */
  private final TemplateUtils templateUtils = CommonComponentHelper.getTemplateUtils();
  
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

    if (!super.evaluate(pageDefinition, pageContext)) {
      return false;
    }
    // redirect if not allowed to access the staging category
    if (requiresRedirect()) {
      return false;
    }

    // if (!("myfavorites".equals(pageDefinition.getId()))) {

    // Following line of code is included to get hold of the countryCode */
    final LocalizationUtils localizationUtils = (LocalizationUtils) Nucleus.getGlobalNucleus().resolveName("/nm/utils/LocalizationUtils");
    final boolean isSupportedByFiftyOne = localizationUtils.isSupportedByFiftyOne(getProfile().getCountryPreference());
    getRequest().setParameter(LocalizationUtils.IS_FIFTYONE_SUPPORTED, isSupportedByFiftyOne);
    final TemplatePageModel pageModel = (TemplatePageModel) getPageModel();
    final String countryCode = localizationUtils.getCurrentCountryCode(getRequest());
    
    /** Site Personalization for best customer */
    NMCategory category = pageModel.getCategory();
    final NMProfile profile = (NMProfile) getRequest().resolveName("/atg/userprofiling/Profile");
    final NMProtocolChange nmProtocolChange = (NMProtocolChange) getRequest().resolveName("/atg/dynamo/droplet/ProtocolChange");
    
    if(getBrandSpecs().isEnableSitePersonalization() && category!=null && category.isPersonalizedCat()){
       Object persObj=profile.getEdwCustData()!=null?profile.getEdwCustData().getPersonalizedCustData():null;
         List<String> persCategories= null;
         if(persObj !=null){
           persCategories=((PersonalizedCustData)persObj).getPersonalizedCategories();
         }
         
      // if it matches, show the category page, if not , and if he is not logged in , direct to login page. else fwd to different content.
      if(!(persCategories!=null && persCategories.contains(category.getId()))){ 
        if(!profile.isRegisteredProfile() && !profile.isAuthorized()){
          String redirectTo = getRequest().getRequestURIWithQueryString();
          final HttpSession session = getRequest().getSession();
          final String secureLoginUrl = nmProtocolChange.getSecureURL("/account/login.jsp?bcError=true", getRequest(), getResponse());          
          session.setAttribute("login_to_acct", redirectTo);
          getResponse().sendRedirect(secureLoginUrl);
          return false;                  
        }
        else{
          String redirectUrl = "/templates/nbcredirect.jsp";         
          getResponse().sendRedirect(redirectUrl);         
          return false;
        }

      }
    }
    
    
    if (!"myfavorites".equals(pageDefinition.getId())) {
      if (StringUtilities.areEqual(countryCode, US_COUNTRY_CODE)) {
        // this conditional block has original code used for domestic site
        final DynamoHttpServletRequest request = getRequest();

        request.setParameter("categoryId", pageModel.getCategory().getRepositoryId());
        pageModel.setEmailRequired(requiresEmailForEntry(pageModel.getCategory()));
        final ProdSkuUtil prodSkuUtil = CommonComponentHelper.getProdSkuUtil();
        pageModel.setAsciiDesignerNamePairing(prodSkuUtil.getAsciiDesignerNamePairing());
        if (getBrandSpecs().getPreferredProductsPCSByEntryPoint()) {
          final EmailInfo emailInfo = (EmailInfo) request.resolveName("/nm/email/EmailInfo");
          if (request.getParameter("ecid") != null) {
            emailInfo.setFromEmail(true);
            if (request.getParameter("itemId") != null) {
              emailInfo.setEmailCategory(request.getParameter("itemId"));
            }
          }
        }
      }

      else if (StringUtilities.isNotEmpty(countryCode)) {
        // Currently we have kept it same as US , ROW specific code will be replaced as and when needed
        final DynamoHttpServletRequest request = getRequest();

        request.setParameter("categoryId", pageModel.getCategory().getRepositoryId());

        final ProdSkuUtil prodSkuUtil = CommonComponentHelper.getProdSkuUtil();
        pageModel.setAsciiDesignerNamePairing(prodSkuUtil.getAsciiDesignerNamePairing());

        if (getBrandSpecs().getPreferredProductsPCSByEntryPoint()) {
          final EmailInfo emailInfo = (EmailInfo) request.resolveName("/nm/email/EmailInfo");
          if (request.getParameter("ecid") != null) {
            emailInfo.setFromEmail(true);
            if (request.getParameter("itemId") != null) {
              emailInfo.setEmailCategory(request.getParameter("itemId"));
            }
          }
        }

      } else {
        throw new Exception("No Country Selection found");
      }
      if (this.getNavHistory().getSilo() != null) {
        pageModel.setBreadcrumb(new Breadcrumb(this.getNavHistory(), ((TemplatePageDefinition) pageDefinition).getBreadcrumbHideSiloLevel()));
        pageModel.setSiloBreadcrumb(new Breadcrumb(this.getNavHistory(), false));
      }

    }

    // Changes made for enabling the promo sticker for product thumbnail pages.
    final TemplatePageDefinition templatePageDefinition = (TemplatePageDefinition) pageDefinition;
    if (templatePageDefinition.getPromoStickerEnabled()) {
      evaluateTheCategoryForPromoSticker(); // LastCall uses this.
    }
    final String grapgicHeaderTestGroup = AbTestHelper.getAbTestValue(getRequest(), GRAPHIC_HEADER2_ABTEST);
    if (getBrandSpecs().isEnableGraphicHeader2() && "testGroup".equalsIgnoreCase(grapgicHeaderTestGroup) && !pageModel.isInternational() && templatePageDefinition.getDisplayGraphicHeader2()
            && pageModel.getCategory().hasImgAvailable2()) {
      pageModel.setDisplayGraphicHeader2(true);

    }

    /* Set the sort method based on the URL Parameter */
    pageModel.setSortMethod(templateUtils.getUrlDrivenSortMethod());
    // Data Dictionary Attributes population.
    if (null != pageDefinition.getPopulatePageDefDrivenDataDictionary() && pageDefinition.getPopulatePageDefDrivenDataDictionary()) {
      CommonComponentHelper.getTemplateUtils().processTMSDataDictionaryForCategoryPages(pageDefinition, pageModel, Boolean.FALSE);
    }

    // set ILink data for Product Thumbnail and Endeca Driven Category Pages
    pageModel.setDisplayILinkData(CommonComponentHelper.getTemplateUtils().isDisplayILinkData());
    return true;
  }

  @Override
  protected PageModel allocatePageModel() {
    return new TemplatePageModel();
  }

  @Override
  protected PageDefinition derivePageDefinition(final PageDefinition pageDefinition, final PageModel pageModel) throws Exception {
    // a lot of evaluating is done here, since the pageDefinition calculation is so complex

    // ensure category is valid.
    final NMCategory category = getCategory();
    if (category == null) {
      getResponse().sendRedirect(getBrandSpecs().getCategoryNotFoundPath());
      return null;
    }

    // set category in model for later use
    final TemplatePageModel templatePageModel = (TemplatePageModel) pageModel;
    templatePageModel.setCategory(category);

    // the page definition passed in, the source of any variations.
    // pages may need to refer to this to get the original template information.
    final TemplatePageDefinition templatePageDefinition = (TemplatePageDefinition) pageDefinition;
    templatePageModel.setOriginalPageDefinition(templatePageDefinition);

    // the page definition that will be used to render the page.
    final TemplatePageDefinition currentPageDefinition = templatePageDefinition;
    return currentPageDefinition;
  }

  /**
   * Returns the category specified by the itemId parameter. Null is returned it the category ID or category is not found.
   * 
   * @return true if the category was not found and redirect occurred.
   * @throws IOException
   */
  protected NMCategory getCategory() throws IOException {
    return getCategoryHelper().getNMCategory(getRequest().getParameter("itemId"), getRequest().getParameter("parentId"));
  }

  /**
   * A promo sticker will be displayed on the category/thumbnail pages if a category has valid values for the attributes promoStickerToggle, promoStickerPath, and promoStickerPromoId.
   */
  private void evaluateTheCategoryForPromoSticker() {
    if (getRequest().getSession().getAttribute("promoStickerDisabled") == null) {
      getRequest().getSession().setAttribute("promoStickerDisabled", false);
    }
    if (!(Boolean) getRequest().getSession().getAttribute("promoStickerDisabled")) {
      String promoStickerToggle = null;
      String promoStickerPath = null;
      final TemplatePageModel pageModel = (TemplatePageModel) getPageModel();
      if (pageModel.getActiveCategory().getCategoryAttributeMap() != null) {
        promoStickerToggle = pageModel.getActiveCategory().getCategoryAttributeMap().get("promoStickerToggle");
        promoStickerPath = pageModel.getActiveCategory().getCategoryAttributeMap().get("promoStickerPath");
      }
      if (promoStickerToggle != null && promoStickerToggle.equalsIgnoreCase("true") && promoStickerPath != null && promoStickerPath.trim().length() > 0) {
        boolean displayPromoSticker = false;
        final String promoId = pageModel.getActiveCategory().getCategoryAttributeMap().get("promoStickerPromoId");
        if (promoId == null || promoId.trim().length() == 0) {
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
        getRequest().setAttribute("displayPromoSticker", displayPromoSticker);
      }
    }
  }

  /**
   * Evaluates the active category to determine if it is in the list of categories that require email access. This list is currently stored in an HTML fragment 'EMAIL_RESTRICTED_CATEGORY'. If the
   * category is restricted it will check to see if the customer has already entered an email for access.
   * 
   * @return true if the customer must enter an email to access the category
   */
  private boolean requiresEmailForEntry(final NMCategory category) {
    boolean requiresEmail = false;
    if (category.isEmailEntryRequired()) {
      /* if the customer has come from an email do not require they enter an email for access to the categories */
      if (!NmoUtils.isEmpty(getRequest().getParameter("uEm"))) {
        getProfile().setEmailCategoryAccess(category.getId());
      } else {
        requiresEmail = !getProfile().hasEmailCategoryAccess(category.getId());
      }
    }
    return requiresEmail;
  }

  /**
   * Uses the FilterRedirect droplet's evaluations methods to determine if the user has access to the category specified. This should redirect if they have the staging category in their path and they
   * are not authorized.
   * 
   * @return true if the system has redirected
   * @throws IOException
   */
  private boolean requiresRedirect() throws IOException {
    boolean isRedirected = false;
    final FilterRedirect filter = (FilterRedirect) getRequest().resolveName("/nm/droplet/FilterRedirect");
    getRequest().setParameter(FilterRedirect.CATEGORY_TO_FILTER, getBrandSpecs().getHiddenCategoryId());
    isRedirected = filter.restrictCategory(getRequest());
    if (isRedirected) {
      getResponse().sendRedirect(getBrandSpecs().getCategoryNotFoundPath());
      getResponse().setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
    }
    return isRedirected;
  }

  /**
   * Returns true if the request parameter "view" matches the specified type.
   */
  protected boolean isViewAll(final String viewType, final DynamoHttpServletRequest request) {
    final String view = request.getParameter(VIEW_PARAM);
    return view != null && view.equals(viewType);
  }

  /**
   * Returns true if a parameter is null, empty, or "null".
   */
  protected boolean isEmptyParameter(final String name, final DynamoHttpServletRequest request) {
    final String value = request.getParameter(name);
    return value == null || value.trim().equals("") || value.trim().equals("null");
  }

  protected boolean isEmpty(final String text) {
    return text == null || text.length() == 0;
  }

  /**
   * Set up paging values. Adapted from CalculatePaging.java droplet.
   */
  protected void calculatePaging(final TemplatePageModel pageModel, final TemplatePageDefinition firstPage, final TemplatePageDefinition subsequentPage, final int totalItemCount,
          final DynamoHttpServletRequest request) throws IOException {

    // determine the elements to display for the current page.
    int pageNumber = 1;

    int itemsOnFirstPage;

    final String pageNumberText = request.getParameter(PAGE_PARAM);
    if (!isEmpty(pageNumberText)) {
      pageNumber = Integer.parseInt(pageNumberText);
    }
    int itemsPerPage = 0;
    int firstPageItemsPerPage = 0;

    final String pageSize = getPageSize(request, firstPage);

    if (!isEmpty(pageSize)) {
      try {
        itemsPerPage = Integer.parseInt(pageSize);
        firstPageItemsPerPage = itemsPerPage;
      } catch (final NumberFormatException exception) {}
    }

    if (itemsPerPage == 0) {
      itemsPerPage = subsequentPage.getItemCount();
    }

    if (firstPageItemsPerPage == 0) {
      firstPageItemsPerPage = firstPage.getItemCount();
    }

    itemsOnFirstPage = firstPageItemsPerPage + firstPage.getFeatureItemCount();

    final NMCategory category = pageModel.getCategory();

    if (null != templateUtils) {
      pageModel.setPromoTilePositions(((TemplatePageDefinition) pageModel.getPageDefinition()).getPromoTilePositions());
      templateUtils.setPromoTiles(category, pageModel, itemsPerPage, pageNumber);
    }

    if (!isEmpty(firstPage.getGraphicBlockPath()) && category.getFlgStaticImage()) {
      itemsOnFirstPage--;

      if (pageNumber == 1) {
        pageModel.setShowGraphicBlock(true);
      }
    }
    if (!pageModel.getPromoTiles().isEmpty()) {
      pageModel.setShowPromoTile(true);
      if (itemsOnFirstPage == 120) {
        itemsOnFirstPage += -3;
        itemsPerPage += -3;

      } else {
        itemsOnFirstPage--;
        itemsPerPage--;
      }
    }
    if (null != templateUtils) {
      templateUtils.setGraphicHeraderPosition(pageModel, pageModel.getOriginalPageDefinition(), false);
    }

    final int totalItems = totalItemCount;

    int pageCount = (totalItems - itemsOnFirstPage) / itemsPerPage + 1;
    final int remainder = (totalItems - itemsOnFirstPage) % itemsPerPage;
    if (remainder > 0) {
      pageCount++;
    }

    pageModel.setPageSize(pageSize);
    pageModel.setPageCount(pageCount);
    pageModel.setPaging(pageCount > 1);
    pageModel.setRecordCount(totalItems);
    final GenericPagingBean pagination = GenericSearchUtil.generatePagination(pageNumber, totalItems, itemsPerPage);
    pageModel.setPagination(pagination);
    setSorts(pageModel, request.getParameter("sort"));

    // these default to values for the first page

    int firstThumbnailIndex = 1 + firstPage.getFeatureItemCount();
    int thumbnailCount = itemsOnFirstPage - firstPage.getFeatureItemCount();
    int columnCount = firstPage.getColumnCount();
    int rowCount;
    if (columnCount == 0) {
      rowCount = 0;
    } else {
      rowCount = totalItems / columnCount;

      final int remainingItems = totalItems - itemsPerPage * (pageNumber - 1);
      rowCount = remainingItems / columnCount;
      if (remainingItems % columnCount > 0) {
        rowCount++;
      }
    }

    if (pageNumber > 1) {
      firstThumbnailIndex = (pageNumber - 2) * itemsPerPage + itemsOnFirstPage + 1;
      thumbnailCount = itemsPerPage;
      columnCount = subsequentPage.getColumnCount();
    }

    // limit thumbnailCount so it will reflect exact value
    final int remainingProducts = totalItems - firstThumbnailIndex + 1;
    thumbnailCount = remainingProducts < thumbnailCount ? remainingProducts : thumbnailCount;

    pageModel.setPageNumber(pageNumber);
    pageModel.setFirstThumbnailIndex(firstThumbnailIndex);
    pageModel.setThumbnailCount(thumbnailCount);
    pageModel.setColumnCount(columnCount);
    pageModel.setShowBackToTop(rowCount >= 3);
  }

  protected String getPageSize(final DynamoHttpServletRequest request, final TemplatePageDefinition pageDefinition) {
    String returnValue = null;
    if (isPrefUserChoiceAll(pageDefinition, request, false)) {
      returnValue = String.valueOf(pageDefinition.getViewManySize());
    } else {
      returnValue = String.valueOf(pageDefinition.getItemCount());
    }
    return returnValue;
  }

  public void setSorts(final TemplatePageModel pageModel, final String sortParam) {
    final ArrayList<SortOption> sortList = new ArrayList<SortOption>();

    final NMCategory category = pageModel.getCategory();
    if (category != null) {
      final Set<RepositoryItem> sorts = category.getSortSet();
      if (sorts != null) {
        for (final RepositoryItem sort : sorts) {
          if ("rp".equals(sort.getRepositoryId())) {
            SortOption sortOption = new SortOption(((String) sort.getPropertyValue("sortName")).toUpperCase() + " HIGH TO LOW", sort.getRepositoryId(), "");
            sortList.add(sortOption);
            sortOption = new SortOption(((String) sort.getPropertyValue("sortName")).toUpperCase() + " LOW TO HIGH", "rpa", "");
            sortList.add(sortOption);
          } else {
            final SortOption sortOption = new SortOption(((String) sort.getPropertyValue("sortName")).toUpperCase(), sort.getRepositoryId(), "");
            sortList.add(sortOption);
          }
        }
      }
    }

    SortOption currentSort = null;
    if (sortParam != null) {
      for (final SortOption sort : sortList) {
        if (sort.getValue().equals(sortParam)) {
          currentSort = sort;
          break;
        }
      }
    }

    if (currentSort == null && !sortList.isEmpty()) {
      currentSort = sortList.get(0);
    }

    pageModel.setCurrentSort(currentSort);
    pageModel.setSortParam(sortParam);
    pageModel.setSorts(sortList);
  }

  /**
   * Returns the category specified by the parameter. Null is returned if the category ID or category is not found.
   * 
   * @param categoryId
   *          the categoryId to retrieve
   * @return the category, or null if not found
   */
  protected NMCategory getCategory(final String categoryId) {
    return getCategoryHelper().getNMCategory(categoryId);
    // if( !NmoUtils.isEmpty(categoryId) ) {
    // NMCategory categoryItem = getCategoryHelper().getCategory(categoryId);
    // if( categoryItem != null ) {
    // return categoryItem;
    // }
    // }

    // return null;
  }

  /**
   * Function loops through all the child categories of "The Flash Sale Category" (i.e., the saleCategoryId in the properties file), and determines the active and inactive/future sales categories,
   * with special attention to the category passed in on the parameter
   * 
   * @param NMCategory
   *          The flash sale root category object, i.e., the saleCategoryId in .properties
   * @param FlashSaleTemplatePageModel
   *          The page model
   * @param boolean Whether the user is internal
   * @throws ServletException
   * @throws IOException
   */
  protected void populateFlashSale(final NMCategory flashSaleRootCategory, final TemplatePageModel pageModel, final boolean isInternal) throws ServletException, IOException {
    final List<NMCategory> activeFlashSaleCategories = new ArrayList<NMCategory>();
    final List<NMCategory> inactiveFlashSaleCategories = new ArrayList<NMCategory>();

    try {
      if (flashSaleRootCategory != null) {
        // we need to ignore categories for featured products and default products
        final Repository productRepository = (Repository) getRequest().resolveName("/atg/commerce/catalog/ProductCatalog");
        final RepositoryItem featuredProductFrag = productRepository.getItem(FEATURED_PROD_FRAG_ID, "htmlfragments");
        final RepositoryItem defaultProductFrag = productRepository.getItem(DEFAULT_PROD_FRAG_ID, "htmlfragments");
        final RepositoryItem drawerFeatProductFrag = productRepository.getItem(DRAWER_FEATURED_PROD_FRAG_ID, "htmlfragments");

        // retrieve children categories
        final List<NMCategory> childCats = flashSaleRootCategory.getChildCategories();

        if (childCats == null) { // essentially impossible
          log.logDebug("No children to filter for " + flashSaleRootCategory.getDisplayName() + ".");
        } else {
          for (int i = 0; i < childCats.size(); i++) {
            final NMCategory childCategory = childCats.get(i);

            if (childCategory.getId().equals(featuredProductFrag.getPropertyValue("frag_value")) || childCategory.getId().equals(defaultProductFrag.getPropertyValue("frag_value"))
                    || childCategory.getId().equals(drawerFeatProductFrag.getPropertyValue("frag_value"))) {
              continue;
            } else {
              final FlashSaleStatus status = childCategory.getFlashSaleStatus(isInternal);
              if (status == FlashSaleStatus.ACTIVE) {
                activeFlashSaleCategories.add(childCategory);
              } else if (status == FlashSaleStatus.COMINGSOON) {
                inactiveFlashSaleCategories.add(childCategory);
              }
            }
          }
        }
      } else {
        log.logDebug(">>>DEBUG>>> flashSaleCategory is null in properties file");
      }
    } catch (final Exception ex) {
      log.logDebug("service exception: " + ex.toString());
    }

    /*
     * Setting the parameters in the page model
     */
    pageModel.setActiveFlashSaleCategories(activeFlashSaleCategories);
    pageModel.setInactiveFlashSaleCategories(inactiveFlashSaleCategories);

    pageModel.setFlashSaleCategory(flashSaleRootCategory);
  }

}
