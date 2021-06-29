package com.nm.commerce.pagedef.evaluator.myfavorites;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpSession;
import javax.servlet.jsp.PageContext;

import atg.json.JSONObject;
import atg.repository.RepositoryItem;
import atg.servlet.DynamoHttpServletRequest;
import atg.servlet.ServletUtil;

import com.nm.ajax.myfavorites.utils.MyFavoritesUtil;
import com.nm.ajax.mynm.beans.MyNMProduct;
import com.nm.ajax.mynm.beans.MyNMStore;
import com.nm.ajax.mynm.utils.MyNMAjaxUtils;
import com.nm.authentication.AuthenticationHelper;
import com.nm.commerce.NMProfile;
import com.nm.commerce.catalog.NMProduct;
import com.nm.commerce.checkout.CheckoutComponents;
import com.nm.commerce.pagedef.definition.MyFavoritesPageDefinition;
import com.nm.commerce.pagedef.definition.PageDefinition;
import com.nm.commerce.pagedef.evaluator.EndecaPageEvaluator;
import com.nm.commerce.pagedef.model.MyFavoritesPageModel;
import com.nm.commerce.pagedef.model.PageModel;
import com.nm.components.CommonComponentHelper;
import com.nm.mynm.utils.MyNMCacheUtils;
import com.nm.mynm.utils.MyNMUtils;
import com.nm.mynm.utils.MyNMWidgetUtils;
import com.nm.tms.constants.TMSDataDictionaryConstants;
import com.nm.tms.core.TMSMessageContainer;
import com.nm.tms.utils.TMSDataDictionaryUtils;
import com.nm.utils.AkamaiUrlHelper;
import com.nm.utils.ProdSkuUtil;

public class MyFavoritesEvaluator extends EndecaPageEvaluator {
  
  private AkamaiUrlHelper akamaiHelper = null;
  private static final String AKAMAI_HELPER_PATH = "/nm/utils/AkamaiUrlHelper";
  
  @Override
  protected PageModel allocatePageModel() {
    return new MyFavoritesPageModel();
  }
  
  @Override
  public boolean evaluate(final PageDefinition pageDefinition, final PageContext pageContext) throws Exception {
    
    final DynamoHttpServletRequest request = this.getRequest();
    final String requestParameter = request.getParameter("view");
    final boolean isStorePage = (requestParameter != null) && requestParameter.equals("favs");
    final boolean isDesignerPage = (requestParameter != null) && requestParameter.equals("favd");
    final boolean isWelcomePage = (requestParameter != null) && requestParameter.equals("welcome");
    final boolean isItemsPage = (!isStorePage) && (!isDesignerPage) && (!isWelcomePage);
    
    if (!super.evaluate(pageDefinition, pageContext)) {
      return false;
    }
    
    final MyFavoritesPageModel myFavoritesPageModel = (MyFavoritesPageModel) getPageModel();
    final MyFavoritesPageDefinition myFavPageDefinition = (MyFavoritesPageDefinition) myFavoritesPageModel.getPageDefinition();
    
    final AuthenticationHelper authHelper = (AuthenticationHelper) getRequest().resolveName("/nm/authentication/AuthenticationHelper");
    
    boolean isAuth = false;
    isAuth = authHelper.sessionHasLoggedInRegisteredUser(getRequest());
    final NMProfile profile = CheckoutComponents.getProfile(request);
    profile.getSecurityStatus();
    final boolean isReg = profile.isRegisteredProfile();
    profile.isAnonymous();
    profile.isAuthorized();
    profile.getWebId();
    
    final List<NMProduct> nmProducts = getMyFavNMProducts(MyFavoritesUtil.getInstance().getMyFavProdListItems(profile, null), false);
    myFavoritesPageModel.setMyFavNMProducts(nmProducts);
    
    final List<NMProduct> nmSoldOutProducts = MyFavoritesUtil.getInstance().getSoldOutProducts(profile, null);
    myFavoritesPageModel.setMyFavNMsoldOutProducts(nmSoldOutProducts);
    // place it in session
    final HttpSession session = ServletUtil.getCurrentRequest().getSession();
    session.setAttribute("soldOutNMProducts", null);
    session.setAttribute("soldOutNMProducts", nmSoldOutProducts);
    
    if (myFavoritesPageModel.getMyFavEmailSubject() == null) {
      myFavoritesPageModel.setMyFavEmailSubject(myFavPageDefinition.getMyFavEmailSubject());
    }
    
    if (isStorePage || isDesignerPage || isWelcomePage || isItemsPage) {
      myFavoritesPageModel.setLoginStatus(isAuth);
      myFavoritesPageModel.setRecognizedStatus(isReg);
    }
    if (isItemsPage) {
      final List<MyNMProduct> recomProds = getRecommendedProducts(nmProducts);
      myFavoritesPageModel.setRecomCustFavProds(recomProds);
    }
    if (isStorePage || isItemsPage) {
      String storeID = null;
      String storeName = null;
      final MyNMStore store = MyNMWidgetUtils.getInstance().getMyStore(profile, request);
      
      if (store != null) {
        storeID = store.getId();
        storeName = store.getName();
      }
      myFavoritesPageModel.setStoreID(storeID);
      myFavoritesPageModel.setStoreName(storeName);
      
    }
    myFavoritesPageModel.setPageType(TMSDataDictionaryConstants.MY_NM);
    // Data Dictionary Attributes population.
    final TMSDataDictionaryUtils dataDictionaryUtils = CommonComponentHelper.getDataDictionaryUtils();
    dataDictionaryUtils.processTMSDataDictionaryAttributes(pageDefinition, null, profile, myFavoritesPageModel, new TMSMessageContainer());
    return true;
  }
  
  private List<NMProduct> getMyFavNMProducts(final List<RepositoryItem> displayableProductList, final boolean isSaleCategory) {
    final List<NMProduct> nmProducts = new ArrayList<NMProduct>();
    for (final RepositoryItem displayableProduct : displayableProductList) {
      if (displayableProduct != null) {
        final String productId = displayableProduct.getRepositoryId();
        final ProdSkuUtil prodSkuUtil = CommonComponentHelper.getProdSkuUtil();
        final NMProduct nmProduct = prodSkuUtil.getProductObject(productId);
        nmProducts.add(nmProduct);
      }
    }

    return nmProducts;
  }
  
  private List<MyNMProduct> getRecommendedProducts(final List<NMProduct> nmProducts) {
    List<String> favProdIds = new ArrayList<String>();
    List<String> recomProdIds = new ArrayList<String>();
    final List<MyNMProduct> recomProds = new ArrayList<MyNMProduct>();

    final int maxProds = 30;
    int productcount = 0;
    favProdIds = MyFavoritesUtil.getInstance().getMyFavoriteItemsList(getProfile(), null, false);
    try {
      JSONObject myFavoritesDefaultList = null;
      final MyNMCacheUtils cacheUtils = MyNMCacheUtils.getInstance();
      myFavoritesDefaultList = cacheUtils.getMyNMCacheData(MyNMUtils.MYNM_NM_MY_FAVORITES);
      if (!myFavoritesDefaultList.equals(null)) {
        recomProdIds = MyNMWidgetUtils.getInstance().processMyfavoritesJson(myFavoritesDefaultList);
        if (log.isLoggingDebug()) {
          log.logDebug("MyNMUtils My favorites had no returns");
          log.logDebug("recomProdIds->"+recomProdIds+"-count="+recomProdIds.size());
        }
      }
    } catch (final Exception e) {
      log.error("MyNMUtils getMyfavorites() getRecommendedProducts() exception: " + e.getMessage());
    }
    akamaiHelper = (AkamaiUrlHelper) ServletUtil.getCurrentRequest().resolveName(AKAMAI_HELPER_PATH);
    MyNMAjaxUtils.getInstance().setAkamaiHelper(akamaiHelper);
    final Iterator<String> iterator = recomProdIds.iterator();   
    while (iterator.hasNext()) {
      final NMProduct nmProduct = new NMProduct(iterator.next());
      if (!MyNMAjaxUtils.getInstance().isDesignerRestrictedProduct(nmProduct.getWebDesignerName())) {
    	if(nmProduct.getType()==1){
    		if (log.isLoggingDebug()) {
    	          log.logDebug("nmSuite-"+nmProduct.getId()+"disp-"+nmProduct.getIsDisplayable());
    	        }
	        if ((nmProduct != null) && nmProduct.getIsDisplayable() && !favProdIds.contains(nmProduct.getId())) {
	          recomProds.add((MyNMAjaxUtils.getInstance().getMyNMProduct(nmProduct)));
	          productcount++;
	        }
	      }
    	else{
    		if (log.isLoggingDebug()) {
  	          log.logDebug("nmProduct-"+nmProduct.getId()+"disp-"+nmProduct.getIsDisplayable()+"sell-"+nmProduct.getIsSellableItem());
  	        }
          if ((nmProduct != null) && nmProduct.getIsDisplayable() && nmProduct.getIsSellableItem() && !favProdIds.contains(nmProduct.getId())) {
  		          recomProds.add((MyNMAjaxUtils.getInstance().getMyNMProduct(nmProduct)));
  		          productcount++;
  		        }
  		      }
      	}
      
      if (productcount >= maxProds) {
        break;
      }
    }
    if (log.isLoggingDebug()) {
          log.logDebug("final recomProds->"+recomProds+"-count-"+recomProds.size());
        }
    return recomProds;
  }
}
