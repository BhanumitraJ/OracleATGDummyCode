package com.nm.commerce.checkout;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import atg.servlet.DynamoHttpServletRequest;
import atg.servlet.ServletUtil;

import com.nm.commerce.NMCommerceItem;
import com.nm.commerce.NMProfile;
import com.nm.components.CommonComponentHelper;

/**
 * Utility class for logging common events between the old and new checkout versions.
 * 
 * @author nmjjm4
 * 
 */
public class LoggingUtil {
  
  /**
   * Logs an info message to the server when a product is removed or updated from a customers cart as part of the real time inventory check and real time inventory allocation.
   * 
   * @param profile
   *          - profile of the customer affected
   * @param item
   *          - commerce item updated
   * @param stocklevel
   *          - actual stock according to cmos of the item
   * @param isOldCheckout
   *          - true if this is old checkout
   */
  public static void logRealtimeInventoryCheckInfo(NMProfile profile, NMCommerceItem item, int stocklevel, boolean isOldCheckout) {
    
    try {
      String pipe = "|";
      
      StringBuffer message = new StringBuffer();
      
      message.append("REQUESTED INVENTORY UNAVAILABLE:[");
      
      message.append("TIME:" + (new SimpleDateFormat("MM-dd-yyyy HH:mm:ss")).format(Calendar.getInstance().getTime()));
      message.append(pipe);
      
      DynamoHttpServletRequest request = ServletUtil.getCurrentRequest();
      if (request != null) {
        message.append("USER-AGENT:" + request.getHeader("User-Agent"));
        message.append(pipe);
        if (request.getSession() != null) {
          message.append("SESSION:" + request.getSession().getId());
          message.append(pipe);
        }
      }
      
      if (profile != null) {
        message.append("PROFILE:" + profile.getRepositoryId());
        message.append(pipe);
      }
      
      if (item != null) {
        message.append("PRODUCT:" + item.getProductId());
        message.append(pipe);
        
        message.append("PRODUCT_NAME:" + item.getProduct().getDisplayName());
        message.append(pipe);
        
        message.append("CMOS_ID:" + item.getCmosCatalogId() + "_" + item.getCmosItemCode());
        message.append(pipe);
        
        message.append("SKU:" + item.getSkuNumber());
        message.append(pipe);
        
        message.append("REQUESTED:" + item.getQuantity());
        message.append(pipe);
        
        message.append("AVAILABLE:" + stocklevel);
        message.append(pipe);
      }
      
      if (isOldCheckout) {
        message.append("VERSION:Ajax");
      } else {
        message.append("VERSION:Responsive");
      }
      message.append(pipe);
      
      message.append("SITE:" + CommonComponentHelper.getSystemSpecs().getProductionSystemCode());
      
      message.append("]");
      
      CommonComponentHelper.getLogger().info(message.toString());
      
    } catch (Exception e) {
      // should never occur, but this is short order, so to ensure no issues within checkout catch all issues.
      System.out.println("Unexpected error while logging LogginUtil#logRealtimeInventoryCheckInfo " + e.getMessage());
    }
    
  }
  
}
