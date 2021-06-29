package com.nm.commerce.upsell;

import java.util.Comparator;

import atg.nucleus.GenericService;
import atg.nucleus.ServiceException;

import atg.repository.Repository;
import atg.repository.RepositoryItem;
import com.nm.commerce.NMCommerceItem;
import com.nm.repository.ProductCatalogRepository;

/**
 * Class Name: PriceComparator This class acts as a comparator based on price
 * 
 * @author C. Chadwick
 * @author $Author: nmmc5 $
 * @since 8/31/2004 Last Modified Date: $Date: 2008/04/29 14:00:07CDT $
 * @version $Revision: 1.2 $
 */
public class PriceComparator extends GenericService implements Comparator {
  private Repository upsellRepository;
  
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
   * Compares the two commerce items using their price. This class orders higher prices before lower prices (reverse ordering).
   * 
   * @param o1
   *          The first item to be compared
   * @param o2
   *          The second item to be compared
   * 
   * @return >0 if o1 is greater than o2. <0 if o1 is less than 02. =0 if they are equal
   * @throws ClassCastException
   *           if both the objects are not NMCommcerceItems
   */
  public int compare(Object o1, Object o2) throws ClassCastException {
    checkedLogDebug("DepartmentComparator.compare()...");
    
    int comparisonResult = 0;
    NMCommerceItem ci1 = (NMCommerceItem) o1;
    NMCommerceItem ci2 = (NMCommerceItem) o2;
    
    if (ci1 == null) {
      throw new ClassCastException("Object 1 is not a NMCommerceItem");
    }
    
    if (ci2 == null) {
      throw new ClassCastException("Object 2 is not a NMCommerceItem");
    }
    
    Double itemPrice1 = new Double(ci1.getPriceInfo().getAmount());
    Double itemPrice2 = new Double(ci2.getPriceInfo().getAmount());
    checkedLogDebug("Price1=" + itemPrice1 + " Price2=" + itemPrice2);
    return itemPrice2.compareTo(itemPrice1);
  }
  
  /**
   * Getter for the upsellRepository property
   * 
   * @return Returns the upsellRepository.
   */
  public Repository getUpsellRepository() {
    return upsellRepository;
  }
  
  /**
   * Setter for the upsellRepository property
   * 
   * @param upsellRepository
   *          The upsellRepository to set.
   */
  public void setUpsellRepository(Repository upsellRepository) {
    this.upsellRepository = upsellRepository;
  }
  
  private void checkedLogDebug(String message) {
    if (isLoggingDebug()) {
      logDebug(message);
    }
  }
}

// ====================================================================
// File: PriceComparator.java
//
// Copyright (c) 2004 Neiman Marcus.
// All rights reserved.
//
// This is an unpublished proprietary source code of the Neiman Marcus Group.
// The copyright notice above does not evidence and actual or intended publication
// of said source code.
// ====================================================================
