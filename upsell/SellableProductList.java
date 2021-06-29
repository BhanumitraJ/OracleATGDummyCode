package com.nm.commerce.upsell;

import java.util.Iterator;
import java.util.List;

import atg.repository.RepositoryItem;

import com.nm.components.CommonComponentHelper;

/**
 * Class Name: SellableProductList Decorates a product list and filters out non-sellable items. Uses ProdSkuUtil.isDisplayable() method to determine if a product is displayable
 * 
 * @author C. Chadwick
 * @author $Author: Richard A Killen (NMRAK3) $
 * @since 10/1/2004 Last Modified Date: $Date: 2012/07/12 10:19:47CDT $
 * @version $Revision: 1.3 $
 * @see ProdSkuUtil
 */
public class SellableProductList extends ProductListDecorator {
  
  /**
   * Creates a new SellableProductList object.
   * 
   * @param productList
   *          The list we are decorating
   */
  public SellableProductList(List<RepositoryItem> productList) {
    super(productList);
  }
  
  /**
   * Update the list by removing all non-sellable items
   */
  protected void updateList() {
    Iterator<RepositoryItem> it = getProductList().iterator();
    
    while (it.hasNext()) {
      RepositoryItem product = (RepositoryItem) it.next();
      
      if ((product != null) && !CommonComponentHelper.getProdSkuUtil().isDisplayable(product)) {
        it.remove();
      }
    }
  }
}

// ====================================================================
// File: SellableProductList.java
//
// Copyright (c) 2004 Neiman Marcus.
// All rights reserved.
//
// This is an unpublished proprietary source code of the Neiman Marcus Group.
// The copyright notice above does not evidence and actual or intended publication
// of said source code.
// ====================================================================
