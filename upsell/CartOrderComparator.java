package com.nm.commerce.upsell;

import java.util.Comparator;

import atg.nucleus.GenericService;
import atg.nucleus.ServiceException;

import com.nm.commerce.NMCommerceItem;

/**
 * Class Name: CartOrderComparator This class orders NMCommerceItems based on the order they were added to the cart. The assumption is that the larger the commerceItemId, the later it was added to the
 * cart. The main reason this needs to be done is that the mergesort that the Collections class uses does not keep the ordering of equal items. Therefore items that are equal will have their order
 * re-arranged and be out of "cart order". This comparator fixes that.
 * 
 * @author C. Chadwick
 * @author $Author: Richard A Killen (NMRAK3) $
 * @since 8/30/2004 Last Modified Date: $Date: 2012/07/24 17:26:09CDT $
 * @version $Revision: 1.5 $
 */
public class CartOrderComparator extends GenericService implements Comparator<NMCommerceItem> {
  
  /**
   * Default constructor
   */
  public CartOrderComparator() {
    // Nothing to do
  }
  
  /**
   * Runs when the component starts up
   */
  public void doStartService() throws ServiceException {
    super.doStartService();
    // Nothing to do
  }
  
  /**
   * Runs when the component is stopped
   */
  public void doStopService() throws ServiceException {
    // Nothing to do
    super.doStopService();
  }
  
  /**
   * Compares two NMCommerceItem by Id.
   * 
   * @param o1
   *          The first item to be compared
   * @param o2
   *          The second item to be compared
   * 
   * @return <0 if o1 is greater than o2. >0 if o1 is less than 02. =0 if they are equal
   * @throws ClassCastException
   *           if both the objects are not NMCommcerceItems
   */
  public int compare(NMCommerceItem o1, NMCommerceItem o2) throws ClassCastException {
    checkedLogDebug("CartOrderComparator.compare()...");
    
    NMCommerceItem ci1 = (NMCommerceItem) o1;
    NMCommerceItem ci2 = (NMCommerceItem) o2;
    
    if (ci1 == null) {
      throw new ClassCastException("Object 1 is not a NMCommerceItem");
    }
    
    if (ci2 == null) {
      throw new ClassCastException("Object 2 is not a NMCommerceItem");
    }
    Long commerceId1 = getNumericPortionOfCommerceId(ci1.getId());
    Long commerceId2 = getNumericPortionOfCommerceId(ci2.getId());
    if (commerceId1 == null || commerceId2 == null) {
      if (isLoggingWarning()) {
        logWarning("Unable to compare " + ci1.toString() + " to " + ci2.toString() + " because a commerceItem Id was null");
      }
      return 0; // Can't compare so they have to be equal
    }
    return commerceId1.compareTo(commerceId2);
  }
  
  /**
   * Gets the depiction code from a cmosItemCode.
   * 
   * @param cmosItemCode
   * 
   * @return The item's depiction code (the first character)
   */
  public String getDepictionCode(String cmosItemCode) {
    return cmosItemCode.substring(0, 1);
  }
  
  private Long getNumericPortionOfCommerceId(String id) {
    int i = 0;
    String numericString;
    
    if (id == null || id.length() < 2) {
      return null;
    }
    
    i = id.toLowerCase().indexOf("ci");
    if (i >= 0) {
      numericString = id.substring(i + 2);
    } else {
      numericString = id;
    }
    
    checkedLogDebug("Parsing commerce ID... was " + id + " now " + numericString);
    
    return new Long(numericString);
  }
  
  private void checkedLogDebug(String message) {
    if (isLoggingDebug()) {
      logDebug(message);
    }
  }
  
}

// ====================================================================
// File: CartOrderComparator.java
//
// Copyright (c) 2004 Neiman Marcus.
// All rights reserved.
//
// This is an unpublished proprietary source code of the Neiman Marcus Group.
// The copyright notice above does not evidence and actual or intended publication
// of said source code.
// ====================================================================
