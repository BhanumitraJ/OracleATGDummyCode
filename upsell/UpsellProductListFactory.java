package com.nm.commerce.upsell;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import atg.commerce.order.OrderHolder;

import atg.nucleus.GenericService;
import atg.nucleus.ServiceException;

/**
 * Class Name: UpsellProductListFactory This factory class builds a the correct combination of the list decorators for the current implementation. Right now it always builds the same combination
 * because all the brands use the same combination. In the future some configurable way (preferably with a properties file) should be implemented to allow different combinations of the list
 * decorators.
 * 
 * @author C. Chadwick
 * @author $Author: nmmc5 $
 * @since 9/27/2004 Last Modified Date: $Date: 2008/04/29 13:58:01CDT $
 * @version $Revision: 1.2 $
 */
public class UpsellProductListFactory extends GenericService {
  private OrderHolder shoppingCart;
  
  /**
   * Creates a new UpsellProductListFactory object.
   */
  public UpsellProductListFactory() {
    super();
    
    // Nothing to do
  }
  
  /**
   * Runs when the component starts up
   * 
   * @throws ServiceException
   */
  public void doStartService() throws ServiceException {
    super.doStartService();
    
    // Nothing to do
  }
  
  /**
   * Runs when the component is stopped
   * 
   * @throws ServiceException
   */
  public void doStopService() throws ServiceException {
    // Nothing to do
    super.doStopService();
  }
  
  /**
   * Creates the correct combination of list decorators that decorate the provided list.
   * 
   * @param productList
   * 
   * @return The decorated List
   */
  public List getProductList(List productList) {
    List newProductList = new SellableProductList(productList);
    newProductList = new NotInCartProductList(newProductList, shoppingCart);
    newProductList = new RandomizedProductList(newProductList);
    if (restrictSuites) {
      // Only type 0 (products) are valid
      Set validTypes = new HashSet(1);
      validTypes.add(new Integer(0));
      newProductList = new ProductTypeProductList(newProductList, validTypes);
    }
    
    return newProductList;
  }
  
  /**
   * Getter for the shopping cart property
   * 
   * @return The session based shopping cart component
   */
  public OrderHolder getShoppingCart() {
    return shoppingCart;
  }
  
  /**
   * Setter for the shopping cart property
   * 
   * @param shoppingCart
   *          session based shopping cart component
   */
  public void setShoppingCart(OrderHolder shoppingCart) {
    this.shoppingCart = shoppingCart;
  }
  
  /**
   * Restrict suites from the product list?
   * 
   * @return Returns the restrictSuites.
   */
  public boolean isRestrictSuites() {
    return restrictSuites;
  }
  
  /**
   * Restrict suites from the product list?
   * 
   * @param restrictSuites
   *          The restrictSuites to set.
   */
  public void setRestrictSuites(boolean restrictSuites) {
    this.restrictSuites = restrictSuites;
  }
  
  private boolean restrictSuites;
  
}

// ====================================================================
// File: UpsellProductListFactory.java
//
// Copyright (c) 2004 Neiman Marcus.
// All rights reserved.
//
// This is an unpublished proprietary source code of the Neiman Marcus Group.
// The copyright notice above does not evidence and actual or intended publication
// of said source code.
// ====================================================================
