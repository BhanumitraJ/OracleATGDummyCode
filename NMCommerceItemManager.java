package com.nm.commerce;

import atg.commerce.CommerceException;
import atg.commerce.catalog.CatalogTools;
import atg.commerce.order.AuxiliaryData;
import atg.commerce.order.CommerceItem;
import atg.commerce.order.CommerceItemManager;
// import atg.commerce.order.InvalidParameterException;
// import atg.commerce.order.InvalidTypeException;
// import atg.commerce.order.ObjectCreationException;
import atg.commerce.order.Order;
import atg.commerce.order.OrderTools;
import atg.commerce.pricing.ItemPriceInfo;
import atg.core.util.ResourceUtils;
import atg.repository.MutableRepositoryItem;
import atg.repository.RepositoryException;
import atg.repository.RepositoryItem;
import atg.repository.RepositoryItemDescriptor;
import atg.repository.Repository;
import atg.servlet.ServletUtil;

/**
 * This class helps the merge/copy facility to pick up our extensions to the commerce-item.
 * <p>
 * NOTE: If you are even a novice java developer, you gotta be wondering why this is not handled by a simple call to a clone() method. Suffice it to say that that is not how this class' parent (ATG's
 * CommerceItemManager) handles its chores, which restricts the OO-purity of our solution a bit.
 * 
 * @author nmtem1
 * 
 */
public class NMCommerceItemManager extends CommerceItemManager {
  protected CommerceItem mergeOrdersCopyCommerceItem(Order pSrcOrder, Order pDstOrder, CommerceItem pItem) throws CommerceException {
    CommerceItem ci = super.mergeOrdersCopyCommerceItem(pSrcOrder, pDstOrder, pItem);
    if (ci instanceof NMCommerceItem && pItem instanceof NMCommerceItem) {
      NMCommerceItem nmci = (NMCommerceItem) ci;
      NMCommerceItem nmpItem = (NMCommerceItem) pItem;
      nmpItem.copyPropertiesInto(nmci);
    }
    return ci;
  }
  
  /**
   * An override of the super's method to protect order-copying (really merging) for ShoppingCartPersistence from the unwanted side-affect of item consolidation that is inherent in
   * CommerceItemManager.addItemToOrder().
   * 
   * @param pOrder
   *          the order.
   * @param pItem
   *          the (commerce) item to be added to the order.
   * @author nmtem1
   */
  public CommerceItem addItemToOrder(Order pOrder, CommerceItem pItem) throws CommerceException {
    return addAsSeparateItemToOrder(pOrder, pItem);
  }
  
  public NMCommerceItem retrieveCommerceItem(String commerceItemId) throws Exception {
    NMCommerceItem commerceItem = null;
    
    Repository orderRepository = getOrderTools().getOrderRepository();
    MutableRepositoryItem repositoryItem = (MutableRepositoryItem) orderRepository.getItem(commerceItemId, "commerceItem");
    commerceItem = new NMCommerceItem();
    commerceItem.setRepositoryItem(repositoryItem);
    
    String pCatalogRefId = commerceItem.getCatalogRefId();
    String pCatalogId = commerceItem.getCatalogId();
    long pQuantity = commerceItem.getQuantity();
    String pCatalogKey = commerceItem.getCatalogKey();
    Object pProductRef = null;
    String pProductId = commerceItem.getProductId();
    
    OrderTools tools = getOrderTools();
    CatalogTools ctools = tools.getCatalogTools();
    if (pCatalogRefId == null) {
      throw new Exception("no catalogRefId");
    }
    
    if (pQuantity <= 0L) {
      throw new Exception("quantity <= 0");
    }
    
    if (isLoggingDebug()) {
      logDebug("Creating commerce item. quantity is " + pQuantity + " catalogId " + pCatalogId);
    }
    
    ItemPriceInfo itemPriceInfo = commerceItem.getPriceInfo();
    if (itemPriceInfo == null) {
      try {
        itemPriceInfo = (ItemPriceInfo) tools.getDefaultItemPriceInfoClass().newInstance();
      } catch (IllegalAccessException e) {
        throw e;
      } catch (InstantiationException e) {
        throw e;
      }
    }
    
    String catalogId = pCatalogId;
    Object catalogRef = null;
    
    try {
      if (pCatalogKey == null) {
        catalogRef = ctools.findSKU(pCatalogRefId);
      } else {
        catalogRef = ctools.findSKU(pCatalogRefId, pCatalogKey);
      }
    } catch (RepositoryException e) {
      throw e;
    }
    
    Object productRef = pProductRef;
    if (pProductId != null && productRef == null) {
      try {
        if (pCatalogKey == null) {
          productRef = ctools.findProduct(pProductId);
        } else {
          productRef = ctools.findProduct(pProductId, pCatalogKey);
        }
        
        if (productRef != null) {
          if (itemPriceInfo != null) {
            RepositoryItem productRI = (RepositoryItem) productRef;
            itemPriceInfo.setAmount(((Double) productRI.getPropertyValue("retailPrice")).doubleValue());
            itemPriceInfo.setRawTotalPrice(((Double) productRI.getPropertyValue("retailPrice")).doubleValue());
          }
        }
        
      } catch (RepositoryException e) {
        throw e;
      }
    }
    
    if (tools.isAssignCatalogInCommerceItem()) {
      if (catalogId == null) {
        RepositoryItem profile = ServletUtil.getCurrentUserProfile();
        if (profile != null) {
          String propName = getProfilesCatalogPropertyName();
          if (propName != null) {
            try {
              RepositoryItemDescriptor profileDesc = profile.getItemDescriptor();
              if (profileDesc == null || profileDesc.hasProperty(propName)) {
                RepositoryItem catalog = (RepositoryItem) profile.getPropertyValue(propName);
                if (catalog != null) {
                  catalogId = catalog.getRepositoryId();
                }
              } else {
                if (isLoggingWarning()) {
                  logWarning(ResourceUtils.getMsgResource("CannotSetCatalogIdInCommerceItem", "atg.commerce.order.OrderResources", sResourceBundle));
                }
              }
            } catch (RepositoryException e) {
              if (isLoggingError()) logError(e);
            }
          }
        }
      }
      commerceItem.setCatalogId(catalogId);
    }
    
    commerceItem.setCatalogRefId(pCatalogRefId);
    commerceItem.setCatalogKey(pCatalogKey);
    commerceItem.setQuantity(pQuantity);
    commerceItem.setPriceInfo(itemPriceInfo);
    AuxiliaryData aux = commerceItem.getAuxiliaryData();
    aux.setCatalogRef(catalogRef);
    aux.setProductId(pProductId);
    aux.setProductRef(productRef);
    
    return commerceItem;
  }
  
}
