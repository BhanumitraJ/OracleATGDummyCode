package com.nm.commerce.pagedef.evaluator;

import static com.nm.tms.constants.TMSDataDictionaryConstants.WISH_LIST;

import java.util.List;

import javax.servlet.jsp.PageContext;

import atg.nucleus.Nucleus;
import atg.repository.RepositoryItem;
import atg.servlet.DynamoHttpServletRequest;
import atg.servlet.DynamoHttpServletResponse;
import atg.servlet.ServletUtil;

import com.nm.ajax.checkout.services.CheckoutService;
import com.nm.commerce.NMCommerceItemTempHolder;
import com.nm.commerce.NMProfile;
import com.nm.commerce.beans.RecentlyChangedCommerceItem;
import com.nm.commerce.checkout.CheckoutComponents;
import com.nm.commerce.giftlist.GiftItem;
import com.nm.commerce.giftlist.GiftlistProductHolder;
import com.nm.commerce.giftlist.NMGiftlistManager;
import com.nm.commerce.giftlist.WishlistGiftCardFilter;
import com.nm.commerce.giftlist.WishlistPagingModel;
import com.nm.commerce.giftlist.WishlistProductHolder;
import com.nm.commerce.pagedef.definition.PageDefinition;
import com.nm.commerce.pagedef.model.PageModel;
import com.nm.commerce.pagedef.model.WishlistPageModel;
import com.nm.commerce.promotion.NMOrderImpl;
import com.nm.components.CommonComponentHelper;
import com.nm.droplet.NMProtocolChange;
import com.nm.formhandler.ProdHandler;
import com.nm.formhandler.RegistryProductFormHandler;
import com.nm.tms.core.TMSMessageContainer;
import com.nm.tms.utils.TMSDataDictionaryUtils;
import com.nm.utils.BrandSpecs;
import com.nm.utils.LocalizationUtils;
import com.nm.utils.NMFormHelper;
import com.nm.utils.NmoUtils;
import com.nm.utils.StringUtilities;

/**
 * Updates variables and sets up any redirects before page execution. Returns true if page content should be output.
 */
public class WishlistPageEvaluator extends SimplePageEvaluator {
  private static final String SIGNIN_URL = "/wishlist/signin.jsp";
  private static final String CREATE_URL = "/wishlist/createWishlist.jsp";
  private static final String MANAGE_URL = "/wishlist/manageWishlist.jsp";
  private static final String WISHLIST  = "Wishlist";
  private static final String MANAGE_WISHLIST= "manageWishlist";
  
  private static final NMProtocolChange protocolChange = (NMProtocolChange) Nucleus.getGlobalNucleus().resolveName("/atg/dynamo/droplet/ProtocolChange");
  
  @Override
  protected PageModel allocatePageModel() {
    return new WishlistPageModel();
  }
  
  @Override
  public boolean evaluate(PageDefinition pageDefinition, PageContext pageContext) throws Exception {
    boolean returnValue = true;
    
    Boolean requiresLogin = pageDefinition.getRequiresLogin();
    String pageId = pageDefinition.getId();
    NMProfile profile = getProfile();
    boolean currentUserHasWishlist = (profile.getTransActiveWishlist() != null);
    final NMOrderImpl order = CheckoutComponents.getCurrentOrder(ServletUtil.getCurrentRequest());
    CheckoutService checkoutService = new CheckoutService();
    
    DynamoHttpServletResponse response = this.getResponse();
    // If the user is an International User, redirect them to the Homepage.
    if (checkoutService.isInternationalSession()) {
      String loginUrl = "/index.jsp";
      loginUrl = NMFormHelper.reviseUrl(loginUrl);
      response.sendLocalRedirect(loginUrl, getRequest());
    } else {
      if (requiresLogin.booleanValue() && profile.isAnonymous()) {
        // DynamoHttpServletResponse response = this.getResponse();
        response.sendLocalRedirect(protocolChange.getSecureURL(SIGNIN_URL, this.getRequest(), response), getRequest());
        returnValue = false;
      } else if ("manageWishlist".equals(pageId)) {
        if (!currentUserHasWishlist) {
          // DynamoHttpServletResponse response = this.getResponse();
          response.sendLocalRedirect(protocolChange.getSecureURL(CREATE_URL, this.getRequest(), response), getRequest());
          returnValue = false;
        } else {
          super.evaluate(pageDefinition, pageContext);
          WishlistPageModel pageModel = (WishlistPageModel) getPageModel();
          DynamoHttpServletRequest request = getRequest();
          NMProfile nmProfile = (NMProfile) ServletUtil.getCurrentRequest().resolveName("/atg/userprofiling/Profile");
          LocalizationUtils utils = ((LocalizationUtils) Nucleus.getGlobalNucleus().resolveName("/nm/utils/LocalizationUtils"));
          String preferredCountry = nmProfile.getCountryPreference();
          if (utils.getAllowPinYinCharactersForCountries().contains(preferredCountry)) {
            pageModel.setOnlyPinYin(true);
          } else {
            pageModel.setOnlyPinYin(false);
          }
          
          String pg = request.getParameter("pg");
          int currPageNumber = 1;
          if (pg != null) {
            try {
              currPageNumber = Integer.parseInt(pg);
            } catch (NumberFormatException exception) {}
          }
          
          RegistryProductFormHandler productFormHandler = (RegistryProductFormHandler) request.resolveName("/nm/formhandler/ManageWishlistProductFormHandler");
          WishlistGiftCardFilter wishlistGiftCardFilter = (WishlistGiftCardFilter) request.resolveName("/nm/commerce/giftlist/WishlistGiftCardFilter");
          GiftlistProductHolder giftlistProductHolder = (GiftlistProductHolder) request.resolveName("/nm/commerce/giftlist/ManageWishlistProductHolder");
          String giftlistId = profile.getTransActiveWishlist().getRepositoryId();
          giftlistProductHolder.setGiftlistId(giftlistId);
          List<GiftItem> registryProductList = giftlistProductHolder.getProductList();
          WishlistPagingModel pagingModel = wishlistGiftCardFilter.filter(currPageNumber, registryProductList, request);
          pageModel.setProductFormHandler(productFormHandler);
          pageModel.setProfile(profile);
          profile.setLastViewedGiftlistId(giftlistId);
          pageModel.setPagingModel(pagingModel);
          
          WishlistProductHolder wishlistProductHolder = (WishlistProductHolder) request.resolveName("/nm/commerce/giftlist/ManageWishlistProductHolder");
          List<NMCommerceItemTempHolder> tempProductList = wishlistProductHolder.getAddToWishlistTempList();
          pageModel.setTempProductList(tempProductList);
          setRecentWishListItem(pageModel.getTempProductList(),order);
          wishlistProductHolder.setAddToWishlistTempList(null);
        }
      } else if ("viewWishlist".equals(pageId)) {
        super.evaluate(pageDefinition, pageContext);
        WishlistPageModel pageModel = (WishlistPageModel) getPageModel();
        DynamoHttpServletRequest request = getRequest();
        NMProfile nmProfile = (NMProfile) ServletUtil.getCurrentRequest().resolveName("/atg/userprofiling/Profile");
        LocalizationUtils utils = ((LocalizationUtils) Nucleus.getGlobalNucleus().resolveName("/nm/utils/LocalizationUtils"));
        String preferredCountry = nmProfile.getCountryPreference();
        pageModel.setPageType(WISH_LIST);
        if (utils.getAllowPinYinCharactersForCountries().contains(preferredCountry)) {
          pageModel.setOnlyPinYin(true);
        } else {
          pageModel.setOnlyPinYin(false);
        }
        
        if (utils.getShowCPFFieldForCountries().contains(preferredCountry)) {
          pageModel.setShowCpf(true);
        } else {
          pageModel.setShowCpf(false);
        }
        
        if (utils.getShowCountyFieldForCountries().contains(preferredCountry)) {
          pageModel.setShowCounty(true);
        } else {
          pageModel.setShowCounty(false);
        }
        
        String pg = request.getParameter("pg");
        int currPageNumber = 1;
        if (pg != null) {
          try {
            currPageNumber = Integer.parseInt(pg);
          } catch (NumberFormatException exception) {}
        }
        
        String giftlistId = request.getParameter("giftlistId");
        if (StringUtilities.isNotEmpty(giftlistId)) {
          NMGiftlistManager giftlistManager = (NMGiftlistManager) request.resolveName("/atg/commerce/gifts/GiftlistManager");
          RepositoryItem giftlist = giftlistManager.getGiftlist(giftlistId);
          if (giftlist != null) {
            RegistryProductFormHandler productFormHandler = (RegistryProductFormHandler) request.resolveName("/nm/formhandler/ManageWishlistProductFormHandler");
            WishlistGiftCardFilter wishlistGiftCardFilter = (WishlistGiftCardFilter) request.resolveName("/nm/commerce/giftlist/WishlistGiftCardFilter");
            GiftlistProductHolder giftlistProductHolder = (GiftlistProductHolder) request.resolveName("/nm/commerce/giftlist/ManageWishlistProductHolder");
            giftlistProductHolder.setGiftlistId(giftlist.getRepositoryId());
            List<GiftItem> registryProductList = giftlistProductHolder.getProductList();
            WishlistPagingModel pagingModel = wishlistGiftCardFilter.filter(currPageNumber, registryProductList, request);
            pageModel.setProductFormHandler(productFormHandler);
            NMProfile owner = new NMProfile();
            owner.setDataSource((RepositoryItem) giftlist.getPropertyValue("owner"));
            pageModel.setProfile(owner);
            profile.setLastViewedGiftlistId(giftlistId);
            pageModel.setPagingModel(pagingModel);
          }
        }
      } else if ("createWishlist".equals(pageId) && currentUserHasWishlist) {
        // DynamoHttpServletResponse response = this.getResponse();
        response.sendLocalRedirect(protocolChange.getSecureURL(MANAGE_URL, this.getRequest(), response), getRequest());
        returnValue = false;
      } else {
        if (super.evaluate(pageDefinition, pageContext)) {
          WishlistPageModel pageModel = (WishlistPageModel) getPageModel();
          DynamoHttpServletRequest request = getRequest();
          NMProfile nmProfile = (NMProfile) ServletUtil.getCurrentRequest().resolveName("/atg/userprofiling/Profile");
          LocalizationUtils utils = ((LocalizationUtils) Nucleus.getGlobalNucleus().resolveName("/nm/utils/LocalizationUtils"));
          String preferredCountry = nmProfile.getCountryPreference();
          pageModel.setPageType(WISH_LIST);
          if (utils.getAllowPinYinCharactersForCountries().contains(preferredCountry)) {
            pageModel.setOnlyPinYin(true);
          } else {
            pageModel.setOnlyPinYin(false);
          }
          
          if (utils.getShowCPFFieldForCountries().contains(preferredCountry)) {
            pageModel.setShowCpf(true);
          } else {
            pageModel.setShowCpf(false);
          }
          
          if (utils.getShowCountyFieldForCountries().contains(preferredCountry)) {
            pageModel.setShowCounty(true);
          } else {
            pageModel.setShowCounty(false);
          }
          
          if ("searchWishlist".equals(pageId)) {
            BrandSpecs brandSpecs = (BrandSpecs) request.resolveName("/nm/utils/BrandSpecs");
            String giftCategory = brandSpecs.getGiftCategory();
            pageModel.setGiftCategory(giftCategory);
            String giftCardCategory = brandSpecs.getGiftCardCategory();
            pageModel.setGiftCardCategory(giftCardCategory);
          }
          
          WishlistProductHolder wishlistProductHolder = (WishlistProductHolder) request.resolveName("/nm/commerce/giftlist/ManageWishlistProductHolder");
          List<NMCommerceItemTempHolder> tempProductList = wishlistProductHolder.getAddToWishlistTempList();
          pageModel.setTempProductList(tempProductList);
          //set the temp wishlist product in the recently changed item bean
          setRecentWishListItem(pageModel.getTempProductList(),order);
          ProdHandler prodHandler = (ProdHandler) request.resolveName("/nm/formhandler/ProdHandler");
          pageModel.setIsFirstProductOnCart(prodHandler.getIsFirstProductOnCart());
        } else {
          returnValue = false;
        }
      }
    }
	if (null != getPageModel()) {
		getPageModel().setPageType(WISH_LIST);
		// Data Dictionary Attributes population.
		final TMSDataDictionaryUtils dataDictionaryUtils = CommonComponentHelper.getDataDictionaryUtils();
		dataDictionaryUtils.processTMSDataDictionaryAttributes(pageDefinition, order, getProfile(), getPageModel(),new TMSMessageContainer());
	}
    if(MANAGE_WISHLIST.equals(pageId)){
      order.getRecentlyChangedCommerceItems().clear();
      order.setWishListAdditionFlag(false);
    }
    return returnValue;
  }
  
  /**
   * This method sets the recently added wish list item to order for user which logs in from the wish list signin page.
   * @param tempProductList
   * @param order
   */
  public void setRecentWishListItem(List<NMCommerceItemTempHolder> tempProductList, NMOrderImpl order){
    List<RecentlyChangedCommerceItem> recentlyChangedItems = order.getRecentlyChangedCommerceItems(); 
    // add the product information to recently changed bean in order for TMS
    if(recentlyChangedItems.size()==00 && NmoUtils.isNotEmptyCollection(tempProductList)){
      for(NMCommerceItemTempHolder tempItem:tempProductList){
          final RecentlyChangedCommerceItem changedItem = new RecentlyChangedCommerceItem();
          changedItem.setCartChangeProductId(tempItem.getDynamoProductId());
          changedItem.setCartChangeProductCmosSku(tempItem.getCmosSKUId());
          changedItem.setCartChangeProductCmosItem(tempItem.getCmosItemCode());
          changedItem.setCartChangeProductQuantity(new Long(tempItem.getQty()));
          recentlyChangedItems.add(changedItem);
          order.setRecentlyChangedCommerceItems(recentlyChangedItems);
          order.setWishListAdditionFlag(true);
      }
    }
  }
}
