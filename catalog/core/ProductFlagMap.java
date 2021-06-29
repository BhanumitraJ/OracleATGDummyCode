package com.nm.commerce.catalog.core;

import atg.nucleus.GenericService;
import atg.nucleus.ServiceMap;

/**
 * ProductFlagMap contains a ServiceMap reference that holds the configuration path to a product flag element template based on its database product_flag_id
 * 
 * @author nmecd
 */
public class ProductFlagMap extends GenericService {
  
  /**
   * @returns the key/value map to the product flag elements
   */
  public ServiceMap getProductFlags() {
    return productFlags;
  }
  
  /**
   * @param productFlags
   *          sets the key/value map to the product flag elements
   */
  public void setProductFlags(ServiceMap productFlags) {
    this.productFlags = productFlags;
  }
  
  private ServiceMap productFlags;
  
}
