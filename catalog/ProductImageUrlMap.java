package com.nm.commerce.catalog;

import com.nm.utils.GetOnlyMap;

/**
 * This is a convenience class for use in the JSP pages. For example, the expression ${product.imageUrl.mg} will return the url for the mg media item associated with the underlying NMProduct. If the
 * product does not have the requested media item, and shimIfNotfound is true, then the shim image url is returned.
 * 
 * @author nmwps
 * 
 */
public class ProductImageUrlMap extends GetOnlyMap<String, String> {
  public static final String SHIM_IMAGE_URL = "/images/shim.gif";
  public NMProduct mProduct;
  public String pathIfNotFound;
  
  public ProductImageUrlMap(NMProduct product) {
    this(product, false);
  }
  
  public ProductImageUrlMap(NMProduct product, boolean shimIfNotFound) {
    mProduct = product;
    this.pathIfNotFound = shimIfNotFound ? SHIM_IMAGE_URL : null;
  }
  
  @Override
  public String get(Object key) {
    String url = mProduct.getImageUrl((String) key);
    return (url != null) ? url : pathIfNotFound;
  }
}
