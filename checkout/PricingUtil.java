package com.nm.commerce.checkout;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.servlet.ServletException;

import atg.commerce.pricing.PricingModelHolder;
import atg.service.pipeline.RunProcessException;
import atg.userprofiling.Profile;

import com.nm.commerce.NMCommerceItem;
import com.nm.commerce.NMOrderHolder;
import com.nm.commerce.NMProfile;
import com.nm.commerce.order.NMOrderManager;
import com.nm.commerce.pricing.NMItemPriceInfo;
import com.nm.commerce.promotion.NMOrderImpl;
import com.nm.formhandler.ShoppingCartHandler;
import com.nm.sitemessages.Message;

/**
 * Package access - outside of this package, methods should be accessed through CheckoutAPI.
 */
/* package */class PricingUtil {
  
  private static PricingUtil INSTANCE; // avoid static initialization
  
  // private constructor enforces singleton behavior
  private PricingUtil() {}
  
  public static synchronized PricingUtil getInstance() {
    INSTANCE = (INSTANCE == null) ? new PricingUtil() : INSTANCE;
    return INSTANCE;
  }
  
  public List<Message> repriceOrder(ShoppingCartHandler cart, NMProfile profile, int context) throws Exception {
    OrderUtil orderUtil = OrderUtil.getInstance();
    PaymentUtil paymentUtil = PaymentUtil.getInstance();
    ShippingUtil shippingUtil = ShippingUtil.getInstance();
    
    NMOrderManager orderManager = (NMOrderManager) cart.getOrderManager();
    NMOrderHolder orderHolder = (NMOrderHolder) cart.getOrderHolder();
    NMOrderImpl order = cart.getNMOrder();
    // take a snapshot of the current order, to check for changes later
    OrderSnapshot snapOne = new OrderSnapshot(order);
    
    if (profile.getCountryPreference() != "US") {
      NMCommerceItem item = null;
      Iterator<NMCommerceItem> iter = (Iterator<NMCommerceItem>) order.getCommerceItems().iterator();
      if (iter != null) {
        while (iter.hasNext()) {
          item = (NMCommerceItem) iter.next();
          NMItemPriceInfo ipi = (NMItemPriceInfo) item.getPriceInfo();
          ipi.unmarkAsFinal();
        }
      }
    }
    
    runProcessRepriceOrder("", cart, profile);
    ArrayList<Message> validationMessages = new ArrayList<Message>();
    
    if (order.isBypassPromoEvaluation() && profile.getCountryPreference() == "US") {
      // do not evaluate promotions for this order
    } else {
      cart.getPromotionEvaluation().evaluatePromotions(order);
      orderUtil.evaluateGwpMustShipWith(order, orderManager);
    }
    
    // remove invalid gwp's
    if (context != SnapshotUtil.EDIT_ITEM_FROM_REGULAR_CART || order.shouldValidateCountry()) {
      shippingUtil.validateShipToCountries(cart, validationMessages);
      shippingUtil.validateGwpMultiSkus(order);
    }
    
    runProcessRepriceOrder("", cart, profile);
    orderUtil.updateLineItemStatus(order, orderManager);
    paymentUtil.updatePaymentGroups(PaymentUtil.REPRICE_GIFT_AND_PREPAID_CARDS, order, (NMOrderManager) orderManager);
    runProcessRepriceOrder("", cart, profile);
    
    Collection<Message> promoMessages = null;
    
    final List<NMCommerceItem> items = order.getNmCommerceItems();
    cart.getPromoUtils().recalculateGwpSelectPromoMap(order);
    
    if (items != null && items.size() > 0) {
      OrderSnapshot snapTwo = new OrderSnapshot(order);
      Set<String> declinedGwpSelects = orderHolder.getDeclinedGwpSelects();
      Set<String> declinedPwps = orderHolder.getDeclinedPwps();
      promoMessages = SnapshotUtil.generatePromoStatusMessages(snapOne, snapTwo, context, order, declinedGwpSelects, declinedPwps);
    } else {
      promoMessages = new ArrayList<Message>();
    }
    
    List<Message> repriceMessages = new ArrayList<Message>();
    repriceMessages.addAll(promoMessages);
    repriceMessages.addAll(validationMessages);
    
    // this fixes the issue of multiple promotions having the same promo code
    order.rebuildAwardedPromotions();
    
    return repriceMessages;
  }
  
  // from OrderUtils
  public void runProcessRepriceOrder(String pricingOper, ShoppingCartHandler cart, Profile profile) throws RunProcessException, ServletException, IOException {
    
    PricingModelHolder upm = cart.getUserPricingModels();
    Locale userLocale = cart.getDefaultLocale();
    NMOrderImpl order = (NMOrderImpl) cart.getOrder();
    cart.runProcessRepriceOrder(pricingOper, order, upm, userLocale, profile);
    // NMOrderPriceInfo priceInfo = (NMOrderPriceInfo) order.getPriceInfo();
  }
}
