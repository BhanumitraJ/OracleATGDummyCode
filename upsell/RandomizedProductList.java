package com.nm.commerce.upsell;

import java.util.Collections;
import java.util.List;

import atg.repository.RepositoryItem;

/**
 * Class Name: RandomizedProductList This class decorates a list by randomizing the order of it's contents
 * 
 * @author C. Chadwick
 * @author $Author: Richard A Killen (NMRAK3) $
 * @since 10/1/2004 Last Modified Date: $Date: 2012/07/12 10:19:46CDT $
 * @version $Revision: 1.3 $
 * @see ProductListDecorator
 */
public class RandomizedProductList extends ProductListDecorator {
  /**
   * Creates a new RandomizedProductList object.
   * 
   * @param productList
   *          The list we are decorationg
   */
  public RandomizedProductList(List<RepositoryItem> productList) {
    super(productList);
    Collections.shuffle(getProductList());
  }
  
  /**
   * We don't re-randomize the list (what's the point?) so therefore this method does nothing.
   */
  protected void updateList() {
    // Nothing to do
  }
}

// ====================================================================
// File: RandomizedProductList.java
//
// Copyright (c) 2004 Neiman Marcus.
// All rights reserved.
//
// This is an unpublished proprietary source code of the Neiman Marcus Group.
// The copyright notice above does not evidence and actual or intended publication
// of said source code.
// ====================================================================
