package com.nm.commerce.catalog;

import javax.servlet.http.HttpServletRequest;
import com.nm.ajax.nm713.utils.NM713Utils;
import com.nm.components.CommonComponentHelper;
import com.nm.utils.NmoUtils;
import atg.repository.RepositoryItem;
import atg.servlet.DynamoHttpServletRequest;

/**
 * NMProductFactory
 * 
 * Creates either a NMProduct, a NMSuite, or a NMSuperSuite, from a RepositoryItem
 * 
 */
public class NMProductFactory {
  
  /*
   * Creates either a NMProduct, a NMSuite, or a NMSuperSuite, from a RepositoryItem
   */
  public static NMProduct createNMProduct(RepositoryItem productRI) {
    
    NMProduct nmProduct = null;
    
    try {
      
      String itemDescriptorName = productRI.getItemDescriptor().getItemDescriptorName();
      
      if (itemDescriptorName.equals("product")) {
        
        switch ((Integer) (productRI.getPropertyValue("type"))) {
        
          case (0):
            nmProduct = new NMProduct(productRI);
          break;
          
          case (1):
            nmProduct = new NMSuite(productRI);
          break;
          
          case (2):
            nmProduct = new NMSuperSuite(productRI);
          break;
        
        }
      } else if (itemDescriptorName.equals("supersuite")) {
        nmProduct = new NMSuperSuite(productRI);
        
      } else if (itemDescriptorName.equals("suite")) {
        nmProduct = new NMSuite(productRI);
        
      } else {
        throw new Exception("ERROR: productRI item descriptor name is '" + itemDescriptorName + "'. Item must be of type 'product', 'suite', or 'supersuite'.");
      }
      
    } catch (Exception e) {
      e.printStackTrace();
      
    }
    
    return nmProduct;
    
  }
  
  /*
   * Extracts a NMProduct from a DynamoHttpServletRequest object by looking for: - a NMProduct object - a product id - a RepositoryItem object
   * 
   * Returns null if none of these are found
   */
  public static NMProduct getNMProductFromRequest(DynamoHttpServletRequest request, String productIdKey, String nmProductKey, String productRepoItemKey) {
    
    NMProduct nmProduct = null;
    if (!NmoUtils.isEmpty(nmProductKey)) {
      nmProduct = (NMProduct) request.getObjectParameter(nmProductKey);
    }
    
    String productId = null;
    if (!NmoUtils.isEmpty(productIdKey)) {
      productId = request.getParameter(productIdKey);
    }
    
    RepositoryItem productRepoItem = null;
    if (!NmoUtils.isEmpty(productRepoItemKey)) {
      productRepoItem = (RepositoryItem) request.getObjectParameter(productRepoItemKey);
    }
    
    // If there is is a product Id, create an object based on it
    if (nmProduct == null && productId != null) {
      
      nmProduct = CommonComponentHelper.getProdSkuUtil().getProductObject(productId);
      
    } else if (nmProduct == null && productRepoItem != null) {
      
      // If there is a repository item, create an object based on that.
      nmProduct = NMProductFactory.createNMProduct(productRepoItem);
      
    }
    
    return nmProduct;
    
  }
  
}
