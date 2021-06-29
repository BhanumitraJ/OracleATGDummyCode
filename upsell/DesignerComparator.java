package com.nm.commerce.upsell;

import atg.nucleus.ServiceException;
import atg.repository.Repository;
import com.nm.repository.ProductCatalogRepository;
import com.nm.repository.UpsellRepository;

/**
 * Class Name: DesignerComparator This class compares commerceItems based on the web designer attribute.
 * 
 * THIS CLASS IS COMPLETELY UNTESTED!!! . The functionality that this class fulfills was requested for Personalization 1.2, but the merchants did not need it for that release and therefore could
 * provide no way to rank designers. The assumption is made that the designer ranking will work in the same way as the depiction, department, and class ranking, but remains to be proven. Therefore a
 * unit test for this class should be created and run once a method for ranking designers is settled upon.
 * 
 * @author C. Chadwick
 * @author $Author: nmmc5 $
 * @since 8/30/2004 Last Modified Date: $Date: 2008/04/29 13:58:11CDT $
 * @version $Revision: 1.2 $
 */
public class DesignerComparator extends CommerceItemComparator {
  private Repository upsellRepository;
  
  /**
   * Default constructor
   */
  public DesignerComparator() {
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
    return ProductCatalogRepository.PRODUCT_DESIGNER;
  }
  
  protected String getRankingPropertyName() {
    return UpsellRepository.DESIGNER_RANKINGS_RANKING;
  }
  
  protected String getRankingDescriptorName() {
    return UpsellRepository.DESIGNER_RANKINGS_DESCRIPTOR;
  }
}

// ====================================================================
// File: DesignerComparator.java
//
// Copyright (c) 2004 Neiman Marcus.
// All rights reserved.
//
// This is an unpublished proprietary source code of the Neiman Marcus Group.
// The copyright notice above does not evidence and actual or intended publication
// of said source code.
// ====================================================================
