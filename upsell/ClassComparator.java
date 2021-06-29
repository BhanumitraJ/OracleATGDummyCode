package com.nm.commerce.upsell;

import atg.nucleus.ServiceException;
import com.nm.repository.ProductCatalogRepository;
import com.nm.repository.UpsellRepository;

/**
 * Class Name: ClassComparator This class compares CommerceItems based on their CMOS class. The class ranking is determined by the upsellRepository
 * 
 * @author C. Chadwick
 * @author $Author: nmmc5 $
 * @since 8/30/2004 Last Modified Date: $Date: 2008/04/29 13:59:06CDT $
 * @version $Revision: 1.2 $
 */
public class ClassComparator extends CommerceItemComparator {
  
  /**
   * Default constructor
   */
  public ClassComparator() {
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
    return ProductCatalogRepository.PRODUCT_SOURCE_CODE;
  }
  
  protected String getRankingPropertyName() {
    return UpsellRepository.CLASS_RANKINGS_RANKING;
  }
  
  protected String getRankingDescriptorName() {
    return UpsellRepository.CLASS_RANKINGS_DESCRIPTOR;
  }
  
}

// ====================================================================
// File: ClassComparator.java
//
// Copyright (c) 2004 Neiman Marcus.
// All rights reserved.
//
// This is an unpublished proprietary source code of the Neiman Marcus Group.
// The copyright notice above does not evidence and actual or intended publication
// of said source code.
// ====================================================================
