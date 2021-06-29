package com.nm.commerce;

import java.util.Iterator;
import java.util.List;

import atg.commerce.order.Order;

/**
 * This class provides utility functions for the ShoppingCartHandler
 * 
 * @author nmcjc1 Last Modified $Date: 2008/04/29 13:59:06CDT $ Last Modified By $Author: nmmc5 $
 * @version $Revision: 1.2 $
 */
public class ShoppingCartHandlerTools {
  
  /**
   * Default constructor. Made private because nobody should be instantiating this class. All methods are static.
   * 
   */
  private ShoppingCartHandlerTools() {
    // Do nothing
  }
  
  /**
   * Finds the item linked to this item via a promotion.
   * 
   * @param promoItem
   * @return The linked item if one exists, null otherwise
   */
  public static NMCommerceItem findLinkedPromoItem(NMCommerceItem promoItem, Order order) {
    String timestamp = promoItem.getPromoTimeStamp();
    if (timestamp == null || timestamp.trim().equals("")) {
      // Since there is no timestamp we cannot match this up with the qualifying item.
      return null;
    }
    List commerceItems = order.getCommerceItems();
    Iterator it = commerceItems.iterator();
    NMCommerceItem linkedItem = null;
    while (it.hasNext()) {
      NMCommerceItem commerceItem = (NMCommerceItem) it.next();
      if (timestamp.equals(commerceItem.getPromoTimeStamp()) && !promoItem.getId().equals(commerceItem.getId())) {
        linkedItem = commerceItem;
        break;
      }
    }
    return linkedItem;
  }
  
}
