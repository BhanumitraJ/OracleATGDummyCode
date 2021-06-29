package com.nm.commerce.checkout;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import atg.commerce.order.OrderManager;
import atg.commerce.order.ShippingGroupCommerceItemRelationship;
import atg.nucleus.GenericService;
import atg.repository.Repository;
import atg.repository.RepositoryItem;
import atg.servlet.DynamoHttpServletRequest;
import atg.servlet.ServletUtil;

import com.nm.ajax.catalog.product.utils.ProductUtils;
import com.nm.collections.GiftWithPurchase;
import com.nm.collections.PurchaseWithPurchase;
import com.nm.commerce.NMCommerceItem;
import com.nm.commerce.NMOrderHolder;
import com.nm.commerce.NMProfile;
import com.nm.commerce.NMRepositoryContactInfo;
import com.nm.commerce.catalog.NMProduct;
import com.nm.commerce.checkout.beans.GwpBean;
import com.nm.commerce.checkout.beans.ResultBean;
import com.nm.commerce.checkout.beans.ResultBeanHelper;
import com.nm.commerce.promotion.NMOrderImpl;
import com.nm.common.NMBorderFreeGetQuoteResponse;
import com.nm.components.CommonComponentHelper;
import com.nm.formhandler.ShoppingCartHandler;
import com.nm.international.fiftyone.checkoutapi.util.ESBCheckoutAPIUtil;
import com.nm.utils.ProdCountryUtils;
import com.nm.utils.ProdSkuUtil;
import com.nm.utils.PromoUtils;
import com.nm.utils.PromotionsHelper;

/**
 * Package access - outside of this package, methods should be accessed through CheckoutAPI.
 */
/* package */class GwpUtil extends GenericService {
  
  private static GwpUtil INSTANCE; // avoid static initialization
  private int itemcount = 0;
  public int pwpcount = 0;
  Set<String> rest_bf_prod = new HashSet<String>();
  
  // private constructor enforces singleton behavior
  private GwpUtil() {}
  
  public static synchronized GwpUtil getInstance() {
    INSTANCE = (INSTANCE == null) ? new GwpUtil() : INSTANCE;
    return INSTANCE;
  }
  
  /**
   * Returns the next GWP, GWP select, or PWP
   */
  public GwpBean getNextGwp(ShoppingCartHandler cart) throws Exception {
    CheckoutConfig config = CheckoutComponents.getConfig();
    
    NMOrderImpl order = cart.getNMOrder();
    NMOrderHolder orderHolder = (NMOrderHolder) cart.getOrderHolder();
    Set<String> declinedGwpSelects = orderHolder.getDeclinedGwpSelects();
    Set<String> declinedPwps = orderHolder.getDeclinedPwps();
    
    /* is there a regular GWP? */
    
    Map<String, ? extends Object> gwpMap = order.getGwpMultiSkuPromoMap();
    for (String promoKey : gwpMap.keySet()) {
      String productId = order.getGwpMultiSkuProductId(promoKey);
      Repository productRepository = CommonComponentHelper.getProductRepository();
      RepositoryItem productItem = productRepository.getItem(productId, "product");
      if (productItem == null) {
        config.logError("Invalid GWP product ID: " + productId);
        continue;
      }
      
      Repository gwpRepository = CommonComponentHelper.getCsrGwpRepository();
      RepositoryItem gwpItem = gwpRepository.getItem(promoKey, "marketinggwp");
      if (gwpItem == null) {
        config.logError("GWP not found: " + promoKey);
        continue;
      }
      
      List<NMProduct> products = new ArrayList<NMProduct>();
      products.add(new NMProduct(productItem));
      
      GwpBean gwp = new GwpBean();
      gwp.setType(GwpBean.REGULAR_TYPE);
      gwp.setPromoKey(promoKey);
      gwp.setProductId(productId);
      gwp.setName((String) gwpItem.getPropertyValue("name"));
      gwp.setProducts(products);
      return gwp;
    }
    
    /* is there a GWP select? */
    
    Map<String, String> map = order.getGwpSelectPromoMap();
    for (Map.Entry<String, String> entry : map.entrySet()) {
      String suiteId = entry.getValue();
      String promoKey = entry.getKey();
      
      if (!declinedGwpSelects.contains(promoKey) && validateProdSamplesAccess(promoKey, suiteId, order)) {
        Repository productRepository = CommonComponentHelper.getProductRepository();
        RepositoryItem suiteItem = productRepository.getItem(suiteId, "suite");
        if (suiteItem == null) {
          config.logError("Invalid GWP select suite ID: " + suiteId);
          continue;
        }
        
        List<NMProduct> products = getSelectGwpProducts(suiteItem, cart);
        if (products.size() == 0) {
          // didn't get products so try the next one
          config.logInfo("GWP Select promotion (" + promoKey + ") does not have any displayable products.");
          continue;
        }
        
        Repository gwpSelectRepository = CommonComponentHelper.getCsrGwpSelectRepository();
        RepositoryItem gwpItem = gwpSelectRepository.getItem(promoKey, "marketinggwpselect");
        if (gwpItem == null) {
          config.logError("GWP select not found: " + promoKey);
          continue;
        }
        
        GwpBean gwp = new GwpBean();
        gwp.setType(GwpBean.SELECT_TYPE);
        gwp.setPromoKey(promoKey);
        gwp.setProductId(suiteId);
        gwp.setName((String) gwpItem.getPropertyValue("name"));
        gwp.setProducts(products);
        
        try {
          String countText = (String) gwpItem.getPropertyValue("itemCount");
          int count = Integer.parseInt(countText);
          if (count >= itemcount) {
            gwp.setItemCount(itemcount);
          } else {
            gwp.setItemCount(count);
          }
          
        } catch (Exception e) {
          config.logError("GWP select has non-numeric item count: " + promoKey);
          continue;
        }
        itemcount = 0;
        return gwp;
      }
    }
    
    /* is there a PWP? */
    NMProfile nmProfile = (NMProfile) ServletUtil.getCurrentRequest().resolveName("/atg/userprofiling/Profile");
    String currentCountry = nmProfile.getCountryPreference();
    NMBorderFreeGetQuoteResponse nmBFG = CheckoutComponents.getNMBorderFreeGetQuoteResponse(ServletUtil.getCurrentRequest());
    if (nmBFG.isEnablePopUp()) {
      nmBFG.setEnablePopUp(false);
      pwpcount = 0;
    }
    if (nmBFG.isCartEmpty()) {
      rest_bf_prod.clear();
      nmBFG.setCartEmpty(false);
      pwpcount = 1;
    }
    String restricted_prod_id = null;
    Map<String, ? extends Object> pwpMap = order.getPwpMultiSkuPromoMap();
    ESBCheckoutAPIUtil eSBCheckoutAPIUtil = null;
    eSBCheckoutAPIUtil = CheckoutComponents.getESBCheckoutAPIUtil(ServletUtil.getCurrentRequest());
    List<NMCommerceItem> itemRestrictedByBf = eSBCheckoutAPIUtil.getBfRestrictedItems();
    
    if (itemRestrictedByBf != null && pwpcount == 0) {
      for (int i = 0; i < itemRestrictedByBf.size(); i++) {
        NMCommerceItem restrictedItem = itemRestrictedByBf.get(i);
        restricted_prod_id = restrictedItem.getProduct().getId();
        rest_bf_prod.add(restricted_prod_id);
      }
    }
    for (String promoKey : pwpMap.keySet()) {
      String productId = order.getPwpMultiSkuProductId(promoKey);
      if (rest_bf_prod != null && rest_bf_prod.contains(productId) && !currentCountry.equalsIgnoreCase("US")) {
        pwpMap.remove(promoKey);
      }
    }
    for (String promoKey : pwpMap.keySet()) {
      if (!declinedPwps.contains(promoKey)) {
        String productId = order.getPwpMultiSkuProductId(promoKey);
        Repository productRepository = CommonComponentHelper.getProductRepository();
        RepositoryItem productItem = productRepository.getItem(productId, "product");
        if (rest_bf_prod != null && rest_bf_prod.contains(productId) && !currentCountry.equalsIgnoreCase("US")) {
          continue;
        }
        if (productItem == null) {
          config.logError("Invalid PWP product ID: " + productId);
          continue;
        }
        
        Repository pwpRepository = CommonComponentHelper.getCsrPwpRepository();
        RepositoryItem pwpItem = pwpRepository.getItem(promoKey, "marketingpwp");
        if (pwpItem == null) {
          config.logError("PWP not found: " + promoKey);
          continue;
        }
        
        List<NMProduct> products = new ArrayList<NMProduct>();
        products.add(new NMProduct(productItem));
        
        GwpBean gwp = new GwpBean();
        gwp.setType(GwpBean.PWP_TYPE);
        gwp.setPromoKey(promoKey);
        gwp.setProductId(productId);
        gwp.setName((String) pwpItem.getPropertyValue("name"));
        gwp.setProducts(products);
        gwp.setDetails((String) pwpItem.getPropertyValue("details"));
        pwpcount = 0;
        return gwp;
      }
    }
    if (nmBFG.isRemoveItem()) {
      rest_bf_prod.clear();
      nmBFG.setRemoveItem(false);
      pwpcount = 1;
    }
    return null;
  }
  
  private List<NMProduct> getSelectGwpProducts(RepositoryItem suiteItem, ShoppingCartHandler cart) throws Exception {
    @SuppressWarnings("unchecked")
    List<RepositoryItem> products = (List<RepositoryItem>) suiteItem.getPropertyValue("subproducts");
    List<NMProduct> gwpProducts = new ArrayList<NMProduct>();
    OrderUtil orderUtil = OrderUtil.getInstance();
    NMOrderImpl order = cart.getNMOrder();
    OrderManager orderManager = cart.getOrderManager();
    synchronized (order) {
      List<ShippingGroupCommerceItemRelationship> rShipList = orderUtil.getShippingGroupCommerceItemRelationships(order, orderManager);
      Iterator<ShippingGroupCommerceItemRelationship> rShipIterator = rShipList.iterator();
      Set<String> countryCodes = new HashSet<String>();
      
      while (rShipIterator.hasNext()) {
        ShippingGroupCommerceItemRelationship rShip = (ShippingGroupCommerceItemRelationship) rShipIterator.next();
        NMRepositoryContactInfo shippingAddress = ShippingGroupUtil.getInstance().getAddress(rShip.getShippingGroup());
        if (shippingAddress.getCountry() != null) {
          countryCodes.add(shippingAddress.getCountry());
        }
      }
      final ProdCountryUtils prodCountryUtils = CheckoutComponents.getProdCountryUtils();
      
      for (int i = 0; i < products.size(); i++) {
        final RepositoryItem productItem = products.get(i);
        final NMProduct product = new NMProduct(productItem);
        product.setIsNonZeroPriceShowable(false);
        product.setIsZeroPriceShowable(true);
        
        if (product.getIsShowable()) {
          gwpProducts.add(product);
        }
        for (final String country : countryCodes) {
          if (!prodCountryUtils.isProductShippableToCountry(productItem, country) && gwpProducts.contains(product)) {
            gwpProducts.remove(product);
            itemcount--;
          }
        }
        
        itemcount++;
      }
      return gwpProducts;
    }
  }
  
  private boolean validateProdSamplesAccess(final String promoId, final String productId, final NMOrderImpl order) throws Exception {
    final PromoUtils promoUtils = CheckoutComponents.getPromoUtils();
    final boolean isGWPSelectPromo = promoUtils.isGWPSelectPromo(promoId);
    
    if (isGWPSelectPromo) {
      // verify it is the product for this promo
      final List<String> gwpSelectPromoList = promoUtils.getEligibleGWPSelect(order.getPromoName());
      final Map<String, String> promoProdMap = promoUtils.getGWPSelectPromoProdIdMap(order, gwpSelectPromoList);
      String productIdForPromo = promoProdMap.get(promoId);
      productIdForPromo = productIdForPromo != null ? productIdForPromo.trim() : null;
      if (productIdForPromo != null && productIdForPromo.equalsIgnoreCase(productId)) {
        final int numPromoItemsOnOrder = promoUtils.getPromoIdCountOnOrder(promoId, order);
        if (numPromoItemsOnOrder == 0) {
          return true;
        }
      }
    }
    return false;
  }
  
  public ResultBean addGwp(ShoppingCartHandler cart, GwpBean gwp) throws Exception {
    ResultBean result = new ResultBean();
    
    NMOrderImpl order = cart.getNMOrder();
    OrderManager orderManager = cart.getOrderManager();
    
    synchronized (order) {
      int gwpType = gwp.getType();
      String promoKey = gwp.getPromoKey();
      PromoUtils promoUtils = CheckoutComponents.getPromoUtils();
      promoUtils.removePromoItemsFromOrder(promoKey, order);
      
      Repository productRepository = CommonComponentHelper.getProductRepository();
      PromotionsHelper promoHelper = CheckoutComponents.getPromotionsHelper();
      ProdSkuUtil prodSkuUtil = CommonComponentHelper.getProdSkuUtil();
      String suiteId = gwp.getProductId();
      
      List<GwpBean.Selection> selections = gwp.getSelections();
      if ((selections == null) || selections.isEmpty()) {
        declineGwp(cart, gwp);
      } else if (gwpType == GwpBean.SELECT_TYPE) {
        for (GwpBean.Selection selection : selections) {
          String productId = selection.getProductId();
          RepositoryItem productItem = productRepository.getItem(productId, "product");
          NMProduct product = new NMProduct(productItem);
          String color = selection.getColor();
          String size = selection.getSize();
          String skuId = ProductUtils.getInstance().retrieveSku(productId, color, size, "", prodSkuUtil, product);
          
          promoHelper.addGwpSelectItemToOrder(skuId, product, 1, null, promoKey, promoKey, suiteId, order);
        }
        ResultBeanHelper.addGWPSamples(result);
      } else if (gwpType == GwpBean.PWP_TYPE) {
        String productId = order.getPwpMultiSkuProductId(promoKey);
        
        // if the checkProductId is not null then the order is still eligible for the
        // promo otherwise we're not going to add this item to the cart. (this is ugly).
        if (productId != null) {
          RepositoryItem repositoryItem = PromotionsHelper.getPwpRepositoryItem(promoKey);
          PurchaseWithPurchase promotion = new PurchaseWithPurchase(repositoryItem);
          
          String promotionalItemId = promotion.getPwpItem();
          String promoCode = promotion.getCode();
          
          RepositoryItem promotionalItem = promoHelper.getProdItem(promotionalItemId, promoCode);
          String promotionalProductId = promotionalItem.getRepositoryId();
          
          if (!promoHelper.promoPwpAlreadyAwarded(order, promotionalProductId, promoKey)) {
            DynamoHttpServletRequest request = ServletUtil.getCurrentRequest();
            
            @SuppressWarnings("unchecked")
            HashSet<String> restrictedFreePwpItems = (HashSet<String>) request.getSession().getAttribute("restrictedFreePWPItems");
            GwpBean.Selection selection = gwp.getSelections().get(0);
            String color = selection.getColor();
            String size = selection.getSize();
            final NMProduct nmProduct = new NMProduct(productId);
            String sku = ProductUtils.getInstance().retrieveSku(productId, color, size, "", prodSkuUtil, nmProduct);
            
            promoHelper.addPwpPromotionToOrder(promotion, "promotion", "promotion", restrictedFreePwpItems, order, sku, 1);
            order.decrementPwpMultiSkuPromoMapEntry(promoCode, 1);
          } else {
        	if(isLoggingDebug()) {
        		logDebug("################################################# PWP promotional item " + promotionalProductId + " not already awarded");
        		logDebug("################################################# Probably should decrementPwpMultiSkuPromoMapEntry");
        	}
          }
        }
      } else {
        RepositoryItem gwpItem = PromotionsHelper.getGwpRepositoryItem(promoKey);
        GiftWithPurchase promotion = new GiftWithPurchase(gwpItem);
        
        String promotionalItem = promotion.getGwpItem();
        String promoCode = promotion.getCode();
        Boolean multiGwpPromotionFlag = promotion.getMultiGWP();
        boolean isMultiGwpPromotion = (multiGwpPromotionFlag == null) ? false : multiGwpPromotionFlag.booleanValue();
        
        boolean multiGWPDollarQual = false;
        Boolean multiGWPDollarQualFlag = promotion.getMultiGWPDollarQual();
        if ((multiGWPDollarQualFlag != null) && (multiGWPDollarQualFlag.booleanValue())) {
          multiGWPDollarQual = true;
        }
        
        RepositoryItem productItem = promoHelper.getProdItem(promotionalItem, promoCode);
        String productId = productItem.getRepositoryId();
        
        if (multiGWPDollarQual || isMultiGwpPromotion || !promoHelper.promoAlreadyAwarded(order, productId)) {
          DynamoHttpServletRequest request = ServletUtil.getCurrentRequest();
          
          @SuppressWarnings("unchecked")
          HashSet<String> restrictedFreeGwpItems = (HashSet<String>) request.getSession().getAttribute("restrictedFreeGWPItems");
          
          GwpBean.Selection selection = gwp.getSelections().get(0);
          String color = selection.getColor();
          String size = selection.getSize();
          final NMProduct nmProduct = new NMProduct(productId);
          String sku = ProductUtils.getInstance().retrieveSku(productId, color, size, "", prodSkuUtil, nmProduct);
          
          promoHelper.addGwpPromotionToOrder(promotion, "promotion", "promotion", restrictedFreeGwpItems, order, sku, 1);
          order.decrementGwpMultiSkuPromoMapEntry(promoCode, 1);
        }
      }
    }
    
    OrderUtil.getInstance().evaluateGwpMustShipWith(order, orderManager);
    orderManager.updateOrder(order);
    return result;
  }
  
  public ResultBean declineGwp(ShoppingCartHandler cart, GwpBean gwp) throws Exception {
    ResultBean result = new ResultBean();
    NMOrderHolder orderHolder = (NMOrderHolder) cart.getOrderHolder();
    
    if (gwp.getType() == GwpBean.SELECT_TYPE) {
      Set<String> declinedGwpSelects = orderHolder.getDeclinedGwpSelects();
      declinedGwpSelects.add(gwp.getPromoKey());
    } else if (gwp.getType() == GwpBean.PWP_TYPE) {
      NMOrderImpl order = cart.getNMOrder();
      
      String promoKey = gwp.getPromoKey();
      RepositoryItem repositoryItem = PromotionsHelper.getPwpRepositoryItem(promoKey);
      PurchaseWithPurchase promotion = new PurchaseWithPurchase(repositoryItem);
      String promoCode = promotion.getCode();
      
      Set<String> declinedPwps = orderHolder.getDeclinedPwps();
      declinedPwps.add(gwp.getPromoKey());
      
      order.removePwpMultiSkuPromoMapEntry(promoKey);
      order.removeAwardedPromotionByKey(promoKey);
      order.addPwpRejectPromoToMap(promoKey, promoCode, null);
      order.setRemovePromoName(promoKey);
      order.setRemoveActivatedPromoCode(promoCode);
      order.setRemoveUserActivatedPromoCode(promoCode);
      
      OrderUtil.getInstance().evaluateGwpMustShipWith(order, cart.getOrderManager());
      
      cart.getOrderManager().updateOrder(order);
    }
    
    return result;
  }
  
  /**
   * Removed any declined GWP selects (not regular GWPs or PWPs).
   */
  public ResultBean restoreGwpSelects(ShoppingCartHandler cart) {
    ResultBean result = new ResultBean();
    NMOrderHolder orderHolder = (NMOrderHolder) cart.getOrderHolder();
    Set<String> declinedGwpSelects = orderHolder.getDeclinedGwpSelects();
    declinedGwpSelects.clear();
    return result;
  }
}
