package com.nm.commerce.upsell;

import atg.nucleus.ServiceException;
import atg.repository.Repository;
import com.nm.repository.ProductCatalogRepository;
import com.nm.repository.UpsellRepository;

/**
 * Class Name: DepartmentComparator This class compares two commerce items based on their department. The department rankings are stored in the upsell repository
 * 
 * @author C. Chadwick
 * @author $Author: nmmc5 $
 * @since 8/30/2004 Last Modified Date: $Date: 2008/04/29 13:58:46CDT $
 * @version $Revision: 1.2 $
 */
public class DepartmentComparator extends CommerceItemComparator {
  private Repository upsellRepository;
  
  /**
   * Default constructor
   */
  public DepartmentComparator() {
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
    return ProductCatalogRepository.PRODUCT_DEPARTMENT_CODE;
  }
  
  protected String getRankingPropertyName() {
    return UpsellRepository.DEPARTMENT_RANKINGS_RANKING;
  }
  
  protected String getRankingDescriptorName() {
    return UpsellRepository.DEPARTMENT_RANKINGS_DESCRIPTOR;
  }
}

// ====================================================================
// File: DepartmentComparator.java
//
// Copyright (c) 2004 Neiman Marcus.
// All rights reserved.
//
// This is an unpublished proprietary source code of the Neiman Marcus Group.
// The copyright notice above does not evidence and actual or intended publication
// of said source code.
// ====================================================================
