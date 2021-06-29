package com.nm.commerce.catalog;

import com.nm.utils.GetOnlyMap;

/**
 * This is a convenience class for use in the JSP pages. For example, the expression ${product.imagePresent.mg} will return true if the NMProduct has an mg media item associated with it.
 * 
 * @author nmwps
 * 
 */
public class ProductImagePresentMap extends GetOnlyMap<String, Boolean> {
  private NMProduct mProduct;
  
  public ProductImagePresentMap(NMProduct product) {
    mProduct = product;
  }
  
  /*
   * Returns true if the underlying NMProduct has the requested image key
   * 
   * @see java.util.Map#get(java.lang.Object)
   */
  public Boolean get(Object key) {
    return (mProduct.getImageUrl((String) key) != null) ? true : false;
  }
}
