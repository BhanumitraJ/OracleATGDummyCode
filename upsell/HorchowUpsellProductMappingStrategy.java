package com.nm.commerce.upsell;

import java.util.Map;

import atg.nucleus.ServiceException;
import atg.repository.RepositoryItem;

import com.nm.commerce.NMCommerceItem;

import com.nm.repository.ProductCatalogRepository;

/**
 * Class Name: HorchowUpsellProductMappingStrategy This class handles mapping products in the cart to upsell products for Horchow. In order to understand what this class is doing, it's vital to know
 * the business requirements:
 * 
 * <li>Using the finest grained product categorization possible, use that categorization code (depiction code, department code, or class code) to map to N categories to pull upsell products from <li>
 * From those product catalog categories only products that are sellable, not in the cart and not already in the upsell shall be used. <li>If a particular categories products are unavailable (all
 * non-sellable, already in the cart or duplicates) then products shall be chosen round-robin from the other categories
 * 
 * @author C. Chadwick (nmcjc1)
 * @author $Author: nmmc5 $
 * @since 9/9/2004 Last Modified Date: $Date: 2008/04/29 13:58:14CDT $
 * @version $Revision: 1.2 $
 * @see UpsellProductMappingStrategy
 */
public class HorchowUpsellProductMappingStrategy extends UpsellProductMappingStrategy {
  
  /**
   * Creates a new HorchowUpsellCategoryMappingStrategy object.
   */
  public HorchowUpsellProductMappingStrategy() {
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
   */
  protected Map getCategoriesForProduct(NMCommerceItem productInCart) {
    Map categoryMap = null;
    RepositoryItem productRef = (RepositoryItem) productInCart.getAuxiliaryData().getProductRef();
    String designer = (String) productRef.getPropertyValue(ProductCatalogRepository.PRODUCT_DESIGNER);
    String classCode = (String) productRef.getPropertyValue(ProductCatalogRepository.PRODUCT_SOURCE_CODE);
    String departmentCode = (String) productRef.getPropertyValue(ProductCatalogRepository.PRODUCT_DEPARTMENT_CODE);
    
    char depictionCodeChar = productInCart.getDepictionCode();
    String depictionCode = null;
    if (depictionCodeChar != ' ') {
      depictionCode = new Character(depictionCodeChar).toString();
    }
    
    categoryMap = getCategories(depictionCode, departmentCode, designer, classCode);
    
    if (categoryMap == null) {
      categoryMap = getCategories(depictionCode, departmentCode, designer, null);
    }
    
    if (categoryMap == null) {
      categoryMap = getCategories(depictionCode, departmentCode, null, classCode);
    }
    
    if (categoryMap == null) {
      categoryMap = getCategories(depictionCode, departmentCode, null, null);
    }
    
    if (categoryMap == null) {
      categoryMap = getCategories(depictionCode, null, designer, classCode);
    }
    
    if (categoryMap == null) {
      categoryMap = getCategories(depictionCode, null, designer, null);
    }
    
    if (categoryMap == null) {
      categoryMap = getCategories(depictionCode, null, null, classCode);
    }
    
    if (categoryMap == null) {
      categoryMap = getCategories(depictionCode, null, null, null);
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
// File: HorchowUpsellCategoryMappingStrategy.java
//
// Copyright (c) 2004 Neiman Marcus.
// All rights reserved.
//
// This is an unpublished proprietary source code of the Neiman Marcus Group.
// The copyright notice above does not evidence and actual or intended publication
// of said source code.
// ====================================================================
