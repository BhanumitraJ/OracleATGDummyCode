package com.nm.commerce.catalog;

import com.nm.utils.NmoUtils;

public class ProductDelegateImageUrlMap extends ProductImageUrlMap {
  
  private NMSuite parentSuite = null;
  
  public ProductDelegateImageUrlMap(NMProduct product) {
    super(product);
  }
  
  public ProductDelegateImageUrlMap(NMProduct product, boolean shimIfNotFound) {
    super(product, shimIfNotFound);
  }
  
  @Override
  public String get(Object key) {
    String url = mProduct.getImageUrl((String) key);
    
    if (NmoUtils.isEmpty(url)) {
      
      // Lazy-load the parent suite
      /*
       * boolean testing = mProduct.getIsInSuite(); if(mProduct.getIsInSuite()) { parentSuite = mProduct.getParentSuite(); } url = parentSuite.getImageUrl((String) key);
       */
    }
    
    return (url != null) ? url : pathIfNotFound;
  }
}
