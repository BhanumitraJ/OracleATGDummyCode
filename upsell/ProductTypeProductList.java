package com.nm.commerce.upsell;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import atg.repository.RepositoryItem;

import com.nm.repository.ProductCatalogRepository;

/**
 * Class Name: NoSuitesProductList List decorator that only shows products, not suites or supersuite
 * 
 * @author C. Chadwick
 * @author $author$
 * @since 11/12/2004 Last Modified Date: $Date: 2012/07/12 10:19:46CDT $
 * @version $Revision: 1.3 $
 * @see ProductListDecorator
 */
public class ProductTypeProductList extends ProductListDecorator {
  
  /**
   * Creates a new NoSuitesProductList object.
   * 
   * @param productList
   *          The original product list
   * @param validProductTypes
   *          A set of Integers that represent the product types that should appear in this list
   */
  public ProductTypeProductList(List<RepositoryItem> productList, Set<Integer> validProductTypes) {
    super(productList);
    this.validProductTypes = validProductTypes;
  }
  
  /**
   * Updates the list removed products that are not of valid types
   */
  protected void updateList() {
    if (validProductTypes != null) {
      RepositoryItem productItem;
      Iterator<RepositoryItem> it = getProductList().iterator();
      Integer type;
      
      while (it.hasNext()) {
        productItem = (RepositoryItem) it.next();
        type = (Integer) productItem.getPropertyValue(ProductCatalogRepository.PRODUCT_TYPE);
        if (!validProductTypes.contains(type)) {
          it.remove();
        }
      }
    }
  }
  
  private Set<Integer> validProductTypes;
}

// ====================================================================
// File: NoSuitesProductList.java
//
// Copyright (c) 2004 Neiman Marcus.
// All rights reserved.
//
// This is an unpublished proprietary source code of the Neiman Marcus Group.
// The copyright notice above does not evidence and actual or intended publication
// of said source code.
// ====================================================================
