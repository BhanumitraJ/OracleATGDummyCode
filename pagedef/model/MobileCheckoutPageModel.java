package com.nm.commerce.pagedef.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import atg.servlet.DynamoHttpServletRequest;

import com.nm.commerce.NMCommerceItem;
import com.nm.commerce.order.NMOrderManager;
import com.nm.commerce.promotion.NMOrderImpl;
import com.nm.formhandler.ShoppingCartHandler;

public class MobileCheckoutPageModel extends PageModel {
  private String initialJSP;
  public final static String CART_JSP = "/page/mobile/checkout/cart/cart.jsp";
  public final static String SERVICE_LEVEL_JSP = "/page/mobile/checkout/shipping/serviceLevel.jsp";
  public final static String PAYMENT_JSP = "/page/mobile/checkout/payment/payment.jsp";
  public final static String LOGIN_JSP = "/page/mobile/checkout/login/login.jsp";
  
  public void setInitialJSP(String initialJSP) {
    this.initialJSP = initialJSP;
  }
  
  public String getInitialJSP() {
    return initialJSP;
  }
  
  public List<NMCommerceItem> getSortedProductList() {
    // original commerce item list
    @SuppressWarnings("unchecked")
    List<NMCommerceItem> ciList = ((NMOrderImpl) getCurrentOrder()).getCommerceItems();
    // sorted commerce item list
    List<NMCommerceItem> sortedList = new ArrayList<NMCommerceItem>(ciList);
    Comparator<NMCommerceItem> comparatorSortSequence = MobileCheckoutPageModel.CommerceItemDateComparator;
    try {
      Collections.sort(sortedList, comparatorSortSequence);
    } catch (RuntimeException e) {
      e.printStackTrace();
    }
    return sortedList;
  }
  
  @SuppressWarnings({"unchecked" , "rawtypes"})
  public static Comparator<NMCommerceItem> CommerceItemDateComparator = new Comparator() {
    public int compare(Object ciObj, Object anotherCiObj) {
      NMCommerceItem commerceItem = (NMCommerceItem) ciObj;
      NMCommerceItem anotherCommerceItem = (NMCommerceItem) anotherCiObj;
      
      Date CommerceItemDate1 = commerceItem.getCommerceItemDate();
      Date CommerceItemDate2 = anotherCommerceItem.getCommerceItemDate();
      
      String id1 = commerceItem.getId();
      String id2 = anotherCommerceItem.getId();
      
      if ((CommerceItemDate1 == null) || (CommerceItemDate2 == null)) {
        int result = 0;
        if ((CommerceItemDate2 == null) && (CommerceItemDate1 == null)) {
          result = id2.compareTo(id1);
        } else if (CommerceItemDate2 == null) {
          result = -1;
        } else if (CommerceItemDate1 == null) {
          result = 1;
        }
        return result;
      } else {
        if (!CommerceItemDate1.equals(CommerceItemDate2)) {
          // primary sort
          return CommerceItemDate2.compareTo(CommerceItemDate1);
        } else {
          // secondary sort
          return id2.compareTo(id1);
        }
      }
    }
    
  };
  
  public NMOrderImpl getCurrentOrder() {
    final ShoppingCartHandler sc = getShoppingCartHandler();
    if (null == sc) {
      return (null);
    }
    
    return ((NMOrderImpl) sc.getOrder());
  }
  
  public ShoppingCartHandler getShoppingCartHandler() {
    final DynamoHttpServletRequest req = getCurrentRequest();
    if (null == req) {
      return (null);
    }
    
    final ShoppingCartHandler sc = (ShoppingCartHandler) req.resolveName("/atg/commerce/order/ShoppingCartModifier");
    return (sc);
  }
  
  public NMOrderManager getOrderManager() {
    final ShoppingCartHandler sc = getShoppingCartHandler();
    if (null == sc) {
      return (null);
    }
    
    return (NMOrderManager) (sc.getOrderManager());
  }
}
