package com.nm.commerce.upsell;

import java.util.Map;

import atg.nucleus.ServiceException;
import atg.repository.RepositoryItem;
import com.nm.commerce.NMCommerceItem;
import com.nm.repository.ProductCatalogRepository;

/**
 * Class Name: ChefsUpsellCategoryMappingStrategy Handles the Chef's Specific portion of mapping a product to a group of upsell products in the upsell bar
 * 
 * 
 * @author C. Chadwick
 * @author $Author: nmmc5 $
 * @since 10/13/04 Last Modified Date: $Date: 2008/04/29 13:59:45CDT $
 * @version $Revision: 1.2 $
 */
public class ChefsUpsellCategoryMappingStrategy extends UpsellProductMappingStrategy {
  /**
   * Creates a new ChefsUpsellCategoryMappingStrategy object.
   */
  public ChefsUpsellCategoryMappingStrategy() {
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
   * Gets the categories for the product specified. Iteratively tries different combinations until one yields a mapping. Goes from most specific to least specific.
   * 
   * 
   * @param productInCart
   * 
   * @return a map of slot numbers (Integer) to categoryIds(String)
   */
  public Map getCategoriesForProduct(NMCommerceItem productInCart) {
    Map categoryMap = null;
    RepositoryItem productRef = (RepositoryItem) productInCart.getAuxiliaryData().getProductRef();
    String designer = (String) productRef.getPropertyValue(ProductCatalogRepository.PRODUCT_DESIGNER);
    String classCode = (String) productRef.getPropertyValue(ProductCatalogRepository.PRODUCT_SOURCE_CODE);
    String departmentCode = (String) productRef.getPropertyValue(ProductCatalogRepository.PRODUCT_DEPARTMENT_CODE);
    
    if (categoryMap == null) {
      categoryMap = getCategories(null, departmentCode, designer, classCode);
    }
    
    if (categoryMap == null) {
      categoryMap = getCategories(null, departmentCode, designer, null);
    }
    
    if (categoryMap == null) {
      categoryMap = getCategories(null, departmentCode, null, classCode);
    }
    
    if (categoryMap == null) {
      categoryMap = getCategories(null, departmentCode, null, null);
    }
    
    if (categoryMap == null || categoryMap.size() < 1) {
      checkedLogDebug("Category Map was empty. Using default categories");
      categoryMap = getDefaultCategories();
    }
    checkedLogDebug("Category Map is " + categoryMap);
    return categoryMap;
  }
  
}

// ====================================================================
// File: ChefsUpsellCategoryMappingStrategy.java
//
// Copyright (c) 2004 Neiman Marcus.
// All rights reserved.
//
// This is an unpublished proprietary source code of the Neiman Marcus Group.
// The copyright notice above does not evidence and actual or intended publication
// of said source code.
// ====================================================================
