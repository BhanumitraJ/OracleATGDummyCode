package com.nm.commerce.upsell;

import java.util.Iterator;
import java.util.List;

import atg.commerce.order.Order;
import atg.commerce.order.OrderHolder;
import atg.repository.RepositoryItem;

import com.nm.commerce.NMCommerceItem;

/**
 * Class Name: NotInCartProductList This is a ProductListDecorator that removes any products that are already in the cart
 * 
 * @author C. Chadwick (nmcjc1)
 * @author $Author: Richard A Killen (NMRAK3) $
 * @since 10/1/04 Last Modified Date: $Date: 2012/07/12 10:19:44CDT $
 * @version $Revision: 1.3 $
 * @see ProductListDecorator
 */
public class NotInCartProductList extends ProductListDecorator {
  private OrderHolder shoppingCart;
  
  /**
   * Creates a new NotInCartProductList object.
   * 
   * @param productList
   *          The original product list
   * @param shoppingCart
   *          The OrderHolder object that represents the user's shopping cart
   */
  public NotInCartProductList(List<RepositoryItem> productList, OrderHolder shoppingCart) {
    super(productList);
    this.shoppingCart = shoppingCart;
  }
  
  /**
   * Updates the list by removing all the items that match items in the cart (same ID)
   */
  protected void updateList() {
    Order currentOrder = shoppingCart.getCurrent();
    @SuppressWarnings("unchecked")
    List<NMCommerceItem> commerceItemsInCart = currentOrder.getCommerceItems();
    Iterator<NMCommerceItem> it = commerceItemsInCart.iterator();
    NMCommerceItem nmci;
    RepositoryItem productItem;
    while (it.hasNext()) {
      nmci = (NMCommerceItem) it.next();
      productItem = (RepositoryItem) nmci.getAuxiliaryData().getProductRef();
      if (getProductList().contains(productItem)) {
        getProductList().remove(productItem);
      }
    }
  }
}

// ====================================================================
// File: NotInCartProductList.java
//
// Copyright (c) 2004 Neiman Marcus.
// All rights reserved.
//
// This is an unpublished proprietary source code of the Neiman Marcus Group.
// The copyright notice above does not evidence and actual or intended publication
// of said source code.
// ====================================================================
