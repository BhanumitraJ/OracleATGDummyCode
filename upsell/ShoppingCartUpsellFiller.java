package com.nm.commerce.upsell;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import atg.commerce.order.Order;
import atg.commerce.order.OrderHolder;

import atg.nucleus.GenericService;
import atg.nucleus.ServiceException;

import atg.repository.Repository;

import atg.scenario.targeting.Slot;

/**
 * Class Name: ShoppingCartUpsellFiller This class is meant to be the "glue" that holds all the different parts of shopping cart upsell together. That's why it is so configurable. Think of it as a
 * plugboard where you plug the various components you are going to use in.
 * 
 * The goal was to provide a large amount of flexibility so that you could change the way shopping cart upsell works by plugging in new components as opposed to changing existing ones.
 * 
 * @author C. Chadwick
 * @author $Author: nmmc5 $
 * @since 10/3/2004 Last Modified Date: $Date: 2008/04/29 13:58:21CDT $
 * @version $Revision: 1.2 $
 */
public class ShoppingCartUpsellFiller extends GenericService {
  private UpsellProductMappingStrategy categoryMappingStrategy;
  private Comparator commerceItemComparator;
  private UpsellProductListFactory upsellListFactory;
  private Slot slot1;
  private Slot slot2;
  private Slot slot3;
  private Slot slot4;
  private Slot slot5;
  private OrderHolder shoppingCart;
  
  /**
   * Creates a new ShoppingCartUpsellFiller object.
   */
  public ShoppingCartUpsellFiller() {
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
   * Fill the upsell slots by asking the categoryMappingStrategy for the correct products.
   */
  public void fillUpsellSlots() {
    Order currentOrder = shoppingCart.getCurrent();
    List commerceItems = currentOrder.getCommerceItems();
    
    // Burn me once.... put it into a new list to avoid "ChangeAwareList" problems
    List shoppingCartContents = new ArrayList(commerceItems);
    Collections.sort(shoppingCartContents, commerceItemComparator);
    
    Map slotProductMap = categoryMappingStrategy.getUpsellProducts(shoppingCartContents, getUpsellListFactory());
    
    if (slotProductMap != null) {
      // Fill the slots
      slot1.add(slotProductMap.get(new Integer(1)));
      slot2.add(slotProductMap.get(new Integer(2)));
      slot3.add(slotProductMap.get(new Integer(3)));
      slot4.add(slotProductMap.get(new Integer(4)));
      slot5.add(slotProductMap.get(new Integer(5)));
    } else {
      if (isLoggingError()) {
        logError("Unable to fill upsell slots because no categories were returned from the mapping strategy!");
      }
    }
  }
  
  /**
   * Getter for the categoryMappingStrategy property. Which strategy should we use to determine which products to upsell?
   * 
   * @return Returns the categoryMappingStrategy.
   */
  public UpsellProductMappingStrategy getCategoryMappingStrategy() {
    return categoryMappingStrategy;
  }
  
  /**
   * Setter for the categoryMappingStrategy property. Which strategy should we use to determine which products to upsell?
   * 
   * @param categoryMappingStrategy
   *          The categoryMappingStrategy to set.
   */
  public void setCategoryMappingStrategy(UpsellProductMappingStrategy categoryMappingStrategy) {
    this.categoryMappingStrategy = categoryMappingStrategy;
  }
  
  /**
   * Getter for the commerceItemComparator property.
   * 
   * @return Returns the commerceItemComparator.
   */
  public Comparator getCommerceItemComparator() {
    return commerceItemComparator;
  }
  
  /**
   * Setter for the commerceItemComparator property.
   * 
   * @param commerceItemComparator
   *          The commerceItemComparator to set.
   */
  public void setCommerceItemComparator(Comparator commerceItemComparator) {
    this.commerceItemComparator = commerceItemComparator;
  }
  
  /**
   * Getter for the upsellListFactory property.
   * 
   * @return Returns the upsellListFactory.
   */
  public UpsellProductListFactory getUpsellListFactory() {
    return upsellListFactory;
  }
  
  /**
   * Setter for the upsellListFactory property.
   * 
   * @param upsellListFactory
   *          The upsellListFactory to set.
   */
  public void setUpsellListFactory(UpsellProductListFactory upsellListFactory) {
    this.upsellListFactory = upsellListFactory;
  }
  
  /**
   * Getter for the slot1 property
   * 
   * @return Returns the slot1.
   */
  public Slot getSlot1() {
    return slot1;
  }
  
  /**
   * Setter for the slot1 property
   * 
   * @param slot1
   *          The slot1 to set.
   */
  public void setSlot1(Slot slot1) {
    this.slot1 = slot1;
  }
  
  /**
   * Getter for the slot2 property
   * 
   * @return Returns the slot2.
   */
  public Slot getSlot2() {
    return slot2;
  }
  
  /**
   * Setter for the slot2 property
   * 
   * @param slot2
   *          The slot2 to set.
   */
  public void setSlot2(Slot slot2) {
    this.slot2 = slot2;
  }
  
  /**
   * Getter for the slot3 property
   * 
   * @return Returns the slot3.
   */
  public Slot getSlot3() {
    return slot3;
  }
  
  /**
   * Setter for the slot3 property
   * 
   * @param slot3
   *          The slot3 to set.
   */
  public void setSlot3(Slot slot3) {
    this.slot3 = slot3;
  }
  
  /**
   * Getter for the slot4 property
   * 
   * @return Returns the slot4.
   */
  public Slot getSlot4() {
    return slot4;
  }
  
  /**
   * Setter for the slot4 property
   * 
   * @param slot4
   *          The slot4 to set.
   */
  public void setSlot4(Slot slot4) {
    this.slot4 = slot4;
  }
  
  /**
   * Getter for the slot5 property
   * 
   * @return Returns the slot5.
   */
  public Slot getSlot5() {
    return slot5;
  }
  
  /**
   * Setter for the slot5 property
   * 
   * @param slot5
   *          The slot5 to set.
   */
  public void setSlot5(Slot slot5) {
    this.slot5 = slot5;
  }
  
  /**
   * Getter for the shoppingCart property
   * 
   * @return Returns the shoppingCart.
   */
  public OrderHolder getShoppingCart() {
    return shoppingCart;
  }
  
  /**
   * Setter for the shoppingCart property
   * 
   * @param shoppingCart
   *          The shoppingCart to set.
   */
  public void setShoppingCart(OrderHolder shoppingCart) {
    this.shoppingCart = shoppingCart;
  }
}

// ====================================================================
// File: ShoppingCartUpsellFiller.java
//
// Copyright (c) 2004 Neiman Marcus.
// All rights reserved.
//
// This is an unpublished proprietary source code of the Neiman Marcus Group.
// The copyright notice above does not evidence and actual or intended publication
// of said source code.
// ====================================================================
