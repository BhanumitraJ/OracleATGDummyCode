package com.nm.commerce.upsell;

import atg.nucleus.ServiceException;
import com.nm.commerce.NMCommerceItem;
import com.nm.repository.ProductCatalogRepository;
import com.nm.repository.UpsellRepository;

/**
 * 
 * Class Name: DepictionComparator This class acts as a comparator between two NMCommerceItems based on their depiction codes. The ranking of the depiction codes is stored in the UpsellRepository.
 * 
 * @author C. Chadwick (nmcjc1)
 * @author $Author: nmmc5 $
 * @since 8/27/2004 Last Modified Date: $Date: 2008/04/29 13:59:05CDT $
 * @version $Revision: 1.2 $
 * @see NMCommerceItem
 */
public class DepictionComparator extends CommerceItemComparator {
  
  /**
   * Default constructor
   */
  public DepictionComparator() {
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
  
  protected String getProductPropertyName() {
    return ProductCatalogRepository.PRODUCT_CMOS_ITEM_CODE;
  }
  
  protected String getRankingPropertyName() {
    return UpsellRepository.DEPICTION_RANKINGS_RANKING;
  }
  
  protected String getRankingDescriptorName() {
    return UpsellRepository.DEPICTION_RANKINGS_DESCRIPTOR;
  }
  
}

// ====================================================================
// File: DepictionComparator.java
//
// Copyright (c) 2004 Neiman Marcus.
// All rights reserved.
//
// This is an unpublished proprietary source code of the Neiman Marcus Group.
// The copyright notice above does not evidence and actual or intended publication
// of said source code.
// ====================================================================
