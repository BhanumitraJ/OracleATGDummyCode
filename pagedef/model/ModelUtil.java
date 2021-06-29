package com.nm.commerce.pagedef.model;

import java.util.List;

import atg.commerce.CommerceException;
import atg.commerce.order.OrderManager;
import atg.commerce.order.ShippingGroupCommerceItemRelationship;
import atg.servlet.DynamoHttpServletRequest;

import com.nm.commerce.checkout.CheckoutAPI;
import com.nm.commerce.checkout.CheckoutComponents;
import com.nm.commerce.promotion.NMOrderImpl;
import com.nm.formhandler.ShoppingCartHandler;

public class ModelUtil {
  
  public CheckoutPageModel getCheckoutPageModel(DynamoHttpServletRequest req) {
    return getCheckoutPageModel(CheckoutComponents.getCartHandler(req));
  }
  
  public CheckoutPageModel getCheckoutPageModel(ShoppingCartHandler cart) {
    CheckoutPageModel pageModel = new CheckoutPageModel();
    final NMOrderImpl order = cart.getNMOrder();
    final OrderManager orderMgr = cart.getOrderManager();
    List<ShippingGroupCommerceItemRelationship> commerceItemRelationships;
    try {
      commerceItemRelationships = CheckoutAPI.getShippingGroupCommerceItemRelationships(order, orderMgr);
      pageModel.setCommerceItemRelationships(commerceItemRelationships);
    } catch (CommerceException e) {
      e.printStackTrace();
    }
    return pageModel;
  }
  
  public static ModelUtil getInstance() {
    return util;
  }
  
  private ModelUtil() {
    
  }
  
  private static final ModelUtil util = new ModelUtil();
  
}
