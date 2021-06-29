package com.nm.commerce.checkout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import atg.commerce.order.Order;
import atg.commerce.order.OrderManager;
import atg.commerce.order.ShippingGroupCommerceItemRelationship;
import atg.core.util.StringUtils;
import atg.nucleus.GenericService;
import atg.repository.Repository;
import atg.repository.RepositoryException;
import atg.repository.RepositoryItem;
import atg.repository.RepositoryItemDescriptor;
import atg.repository.RepositoryView;
import atg.repository.rql.RqlStatement;
import atg.servlet.DynamoHttpServletRequest;
import atg.servlet.ServletUtil;
import atg.userprofiling.Profile;

import com.nm.ajax.checkout.utils.ComponentUtils;
import com.nm.ajax.checkout.utils.RepositoryUtils;
import com.nm.ajax.inventory.InventoryRequest;
import com.nm.ajax.inventory.InventoryResponse;
import com.nm.ajax.inventory.InventoryResponse.InventoryResponseLineItem;
import com.nm.collections.ServiceLevel;
import com.nm.commerce.NMCommerceItem;
import com.nm.commerce.NMCreditCard;
import com.nm.commerce.NMOrderHolder;
import com.nm.commerce.NMProfile;
import com.nm.commerce.catalog.NMProduct;
import com.nm.commerce.checkout.beans.OutOfStockItemDetails;
import com.nm.commerce.promotion.NMOrderImpl;
import com.nm.components.CommonComponentHelper;
import com.nm.configurator.utils.ConfiguratorUtils;
import com.nm.formhandler.ShoppingCartHandler;
import com.nm.sitemessages.Message;
import com.nm.sitemessages.MessageDefs;
import com.nm.sitemessages.StockLevelMessage;
import com.nm.utils.CollectionUtils;
import com.nm.utils.NmoUtils;
import com.nm.utils.StringUtilities;
import com.nm.utils.fiftyone.FiftyOneUtils;

/**
 * Package access - outside of this package, methods should be accessed through CheckoutAPI.
 */
/* package */class InventoryUtil {
  
  private static InventoryUtil INSTANCE; // avoid static initialization
  
  // private constructor enforces singleton behavior
  private InventoryUtil() {}
  
  final GenericService config = CheckoutComponents.getConfig();
  
  public static synchronized InventoryUtil getInstance() {
    INSTANCE = INSTANCE == null ? new InventoryUtil() : INSTANCE;
    return INSTANCE;
  }
  
  protected DynamoHttpServletRequest getRequest() {
    return ServletUtil.getCurrentRequest();
  }
  
  public Collection<Message> performFinalStockCheck(final ShoppingCartHandler cart, final Order order, final OrderManager orderMgr, final Profile profile) throws Exception {
    final List<Message> messages = new ArrayList<Message>();
    final Repository productRepos = CommonComponentHelper.getProductRepository();
    final OrderUtil orderUtil = OrderUtil.getInstance();
    ConfiguratorUtils configuratorUtils = CommonComponentHelper.getConfiguratorUtils();
    
    final List commerceItemList = order.getCommerceItems();
    
    final Iterator commerceItemIterator = commerceItemList.iterator();
    RepositoryItem sku = null;
    RepositoryItem product = null;
    final HashMap tempItems = new HashMap();
    Map hmPickupFlg = new HashMap();
    
    final NMOrderImpl orderImpl = (NMOrderImpl) order;
    hmPickupFlg = NMCommerceItem.isPickupinStoreNumber(orderImpl);
    StringBuffer errorMessage = new StringBuffer();
    
    while (commerceItemIterator.hasNext()) {
      try {
        final NMCommerceItem commerceItem = (NMCommerceItem) commerceItemIterator.next();
        
        // we're only checking stock on non promotional items
        if (commerceItem.getPriceInfo().getAmount() > 0) {
          final String itemKey = commerceItem.getCmosSKUId();
          TempItem tempItem = (TempItem) tempItems.get(itemKey);
          
          // if tempItem is null then we have not seen this catalog item before
          if (tempItem == null) {
            tempItem = new TempItem();
            final String catalogRefId = commerceItem.getCatalogRefId();
            
            sku = productRepos.getItem(catalogRefId, "sku");
            product = CommonComponentHelper.getProdSkuUtil().getProductRepositoryItem(commerceItem.getCmosCatalogId(), commerceItem.getCmosItemCode());
            
            tempItem.itemName = commerceItem.getProduct().getWebProductDisplayName();
            tempItem.variation1 = commerceItem.getProdVariation1();
            tempItem.variation2 = commerceItem.getProdVariation2();
            tempItem.qtyWanted = new Long(commerceItem.getQuantity()).intValue();
            tempItem.skuQty = Integer.parseInt((String) sku.getPropertyValue("stockLevel"));
            tempItem.discontinuedCode = CommonComponentHelper.getProdSkuUtil().getDiscontinuedCode(product, sku);
            tempItem.exclusiveFlag = ((Boolean) product.getPropertyValue("flgExclusive")).booleanValue();
            tempItem.strSKUId = commerceItem.getCmosSKUId();
            tempItem.skuId = catalogRefId;
            tempItem.discontinuedCode = commerceItem.getCmosSKUId();
            tempItem.id = commerceItem.getId();
            tempItem.cmosCatalogId = commerceItem.getCmosCatalogId();
            tempItem.cmosItemCode = commerceItem.getCmosItemCode();
            tempItem.storeFulfillFlag = commerceItem.isPickupInStore();
            // NMOBLDS-3260
            tempItem.configurationKey = commerceItem.getConfigurationKey();
            tempItem.dynamicImageUrl = commerceItem.getDynamicImageUrl();
            final Boolean isSameDayDelivery = StringUtilities.isNotEmpty(commerceItem.getFulfillmentFacility()) && ServiceLevel.SL0_SERVICE_LEVEL_TYPE.equals(commerceItem.getServicelevel());
            final Boolean isPickupInStore = StringUtilities.isNotEmpty(commerceItem.getPickupStoreNo()) && !commerceItem.getShipToStoreFlg();
            String facilityId = "";
            if (isSameDayDelivery || isPickupInStore) {
              if (isSameDayDelivery) {
                facilityId = commerceItem.getFulfillmentFacility();
              } else {
                facilityId = commerceItem.getPickupStoreNo();
              }
            }
            tempItem.facilityId = facilityId;
            
            if (hmPickupFlg != null) {
              tempItem.strPickupStoreFlg = (String) hmPickupFlg.get(commerceItem.getId());
            }
            
            if (sku.getPropertyValue("quantityOnOrder") != null) {
              tempItem.qtyOnOrder = ((Double) sku.getPropertyValue("quantityOnOrder")).doubleValue();
            } else {
              tempItem.qtyOnOrder = 0.0;
            }
            
            tempItem.dropshipFlag = ((Boolean) sku.getPropertyValue("flgDropship")).booleanValue();
            tempItem.productId = product.getRepositoryId();
            tempItem.commerceItems.add(commerceItem);
            
            tempItems.put(itemKey, tempItem);
          } else { // if we have already seen this item, then simply increase the requested quantity
            tempItem.qtyWanted += tempItem.qtyWanted;
            tempItem.commerceItems.add(commerceItem);
          }
        }
      } catch (final Exception e) {
        CommonComponentHelper.getLogger().error("Error looping through the ShoppingCartQtyMap (currentStockCheck) ", e);
      }
    }
    
    // Populate Inventory Request
    final InventoryRequest inventoryRequest = new InventoryRequest();
    inventoryRequest.setExternalOrderNumber(order.getId());
    final String brandCode = ComponentUtils.getInstance().getSystemSpecs().getProductionSystemCode();
    inventoryRequest.setVendorId(((NMOrderImpl) order).getVendorId());
    inventoryRequest.setBrandCode(brandCode);
    
    final Iterator catRefIdIterator = tempItems.keySet().iterator();
    
    while (catRefIdIterator.hasNext()) {
      final String key = (String) catRefIdIterator.next();
      final TempItem tempItem = (TempItem) tempItems.get(key);
      inventoryRequest.addLineItem(tempItem.id, tempItem.strSKUId, tempItem.cmosCatalogId, tempItem.cmosItemCode, tempItem.qtyWanted, tempItem.storeFulfillFlag, tempItem.facilityId);
    }
    
    final InventoryResponse inventoryResponse = com.nm.utils.InventoryUtil.getInstance().sendInventoryRequestToCmos(inventoryRequest, true);
    final HashMap<String, List<InventoryResponse.InventoryResponseLineItem>> inventory = inventoryResponse.getLineItemMap();
    
    final ArrayList<StockLevelMessage> errorMessages = new ArrayList<StockLevelMessage>();
    final ArrayList<String> stockErrorMessages = new ArrayList<String>();
    final ArrayList<StockLevelMessage> stockMessages = new ArrayList<StockLevelMessage>();
    final List<String> maxPurchaseQtyList = new ArrayList<String>();
    
    final Iterator tempItemsIterator = tempItems.keySet().iterator();
    final List<NMCommerceItem> oosItems = new ArrayList<NMCommerceItem>();
    
    while (tempItemsIterator.hasNext()) {
      final String key = (String) tempItemsIterator.next();
      final TempItem tempItem = (TempItem) tempItems.get(key);
      
      checkPayPalRestricted(orderImpl, errorMessages, tempItem, inventory);
      
      try {
        product = productRepos.getItem(tempItem.productId, "product");
        final Iterator commerceItemsIterator = tempItem.commerceItems.iterator();
        final String skuId = tempItem.skuId;
        
        sku = productRepos.getItem(skuId, "sku");
        int stockLevel = 0;
        
        if (!inventoryResponse.isErrorOrccured()) {
          if (inventoryResponse.isQuantitiesUpdated()) {
            stockLevel = getStockLevel(inventory.get(tempItem.strSKUId), tempItem.qtyWanted);
          } else {
            stockLevel = new Double(tempItem.qtyWanted).intValue();
          }
        } else {
          if ("N".equalsIgnoreCase(tempItem.strPickupStoreFlg) || StringUtilities.isNullOrEmpty(tempItem.strPickupStoreFlg)) {
            stockLevel = CommonComponentHelper.getProdSkuUtil().getStockLevel(product, sku);
            // System.out.println(">>>OrderUtils.performFinalStockCheck()"+CommonComponentHelper.getProdSkuUtil().getStoreSkuInventoryRIBySkuIdStoreNum(sku,tempItem.strPickupStoreFlg) );
          } else {
            // stockLevel=CommonComponentHelper.getProdSkuUtil().getStoreSkuInventoryRIBySkuIdStoreNum(sku,tempItem.strPickupStoreFlg) ;
            stockLevel = CommonComponentHelper.getProdSkuUtil().getStockLevel(product, sku);
          }
        }
        
        if (stockLevel == 0) {
          // displayStockMessage = true;
          errorMessage = new StringBuffer("This item ");
          errorMessage.append(tempItem.itemName.trim()).append(tempItem.getVariationString()).append(" is no longer in stock.");
          if (CommonComponentHelper.getBrandSpecs().isEnableCartOOSRecommendation()
                  && !CheckoutAPI.isInternationalSession((NMProfile) profile)) {
            stockErrorMessages.add(errorMessage.toString());
          } else {
            errorMessages.add(new StockLevelMessage(0, errorMessage.toString()));
          }
          while (commerceItemsIterator.hasNext()) {
            final NMCommerceItem commerceItem = (NMCommerceItem) commerceItemsIterator.next();
            if (CommonComponentHelper.getBrandSpecs().isEnableCartOOSRecommendation() && !CheckoutAPI.isInternationalSession((NMProfile) profile)) {
              oosItems.add(commerceItem);
            }
            orderUtil.removeItemFromOrder(cart, commerceItem.getId());
          }
        }
        // Start NMOBLDS-3260
        // This if condition iterates all the configured commerce items and saves them to OutOfStockItem array list and removes the commerce item from order.
        else if (!StringUtils.isBlank(tempItem.configurationKey)) {
          while (commerceItemsIterator.hasNext()) {
            final NMCommerceItem commerceItem = (NMCommerceItem) commerceItemsIterator.next();
            if (tempItem.id.equalsIgnoreCase(commerceItem.getId()) && configuratorUtils.evaluateSelectionSet(commerceItem, ((NMProfile) profile).getCountryPreference())) {
              errorMessage = new StringBuffer();
              errorMessage.append("One or more options of item ");
              errorMessage.append(tempItem.itemName.trim()).append(" Made to Order are no longer available.");
              stockErrorMessages.add(errorMessage.toString());
              oosItems.add(commerceItem);
              orderUtil.removeItemFromOrder(cart, commerceItem.getId());
            }
          }
        }
        // End
        else if ((tempItem.qtyWanted > stockLevel) && !tempItem.dropshipFlag) {
          tempItem.getVariationString();
          errorMessage = new StringBuffer("We no longer have [");
          errorMessage.append(tempItem.qtyWanted).append("] available in the <B>").append(tempItem.itemName.trim());
          errorMessage.append("</B>.  We have [").append(stockLevel).append("] still available.");
          errorMessages.add(new StockLevelMessage(tempItem.qtyWanted, errorMessage.toString()));
          
          final StringBuffer stockDetail = new StringBuffer("a quantity of [").append(stockLevel).append("] ").append(tempItem.itemName.trim());
          // stockDetail.append(variationString);
          stockMessages.add(new StockLevelMessage(tempItem.qtyWanted, stockDetail.toString()));
          
          int qtyToRemove = tempItem.qtyWanted - stockLevel;
          
          while (commerceItemsIterator.hasNext() && (qtyToRemove > 0)) {
            final NMCommerceItem commerceItem = (NMCommerceItem) commerceItemsIterator.next();
            
            final int qtyRequested = new Long(commerceItem.getQuantity()).intValue();
            
            if (qtyToRemove >= qtyRequested) {
              orderUtil.removeItemFromOrder(cart, commerceItem.getId());
              qtyToRemove = qtyToRemove - qtyRequested;
            } else {
              // change the quantity if a match is found
              final long remainingQty = new Integer(qtyRequested - qtyToRemove).longValue();
              final List rShipList = orderUtil.getShippingGroupCommerceItemRelationships(order, orderMgr);
              final Iterator rShipIterator = rShipList.iterator();
              
              while (rShipIterator.hasNext()) {
                final ShippingGroupCommerceItemRelationship sgcRel = (ShippingGroupCommerceItemRelationship) rShipIterator.next();
                // This will ensure that only the edited
                // commerceItem gets checked
                if (sgcRel.getCommerceItem().getId().equalsIgnoreCase(commerceItem.getId())) {
                  if (!commerceItem.getTransientStatus().equalsIgnoreCase("promotion")) {
                    checkChangedQty(cart, (NMOrderImpl) order, remainingQty, sgcRel, true, (NMProfile) profile);
                  }
                  
                  if (!((NMCommerceItem) sgcRel.getCommerceItem()).getPromoTimeStamp().equals("")
                          && ((NMCommerceItem) sgcRel.getCommerceItem()).getPromoTimeStamp().equals(commerceItem.getPromoTimeStamp())) {
                    checkChangedQty(cart, (NMOrderImpl) order, remainingQty, sgcRel, false, (NMProfile) profile);
                  }
                }
                
                if (!((NMCommerceItem) sgcRel.getCommerceItem()).getPromoTimeStamp().equals("")
                        && ((NMCommerceItem) sgcRel.getCommerceItem()).getPromoTimeStamp().equals(commerceItem.getPromoTimeStamp())) {
                  checkChangedQty(cart, (NMOrderImpl) order, remainingQty, sgcRel, false, (NMProfile) profile);
                }
              }
              
              qtyToRemove = 0;
            }
            
            if (orderImpl.isExpressPaypal()) {
              if (commerceItem.isPayPalRestricted()) {
                errorMessage = new StringBuffer();
                errorMessage.append("One or more item(s) in your shopping bag cannot be purchased with PayPal. Please choose another form of payment or edit your shopping bag.");
                errorMessages.add(new StockLevelMessage(tempItem.qtyWanted, errorMessage.toString()));
                
              }
            }
            
          }
        } else {
          // provide max purchase error if maxPurchaseQty is greater than stockLevel
          final boolean exceedMaxPurchaseQty = CommonComponentHelper.getProdSkuUtil().exceedMaxPurchaseQty(orderImpl, product);
          if (exceedMaxPurchaseQty) {
            if (!maxPurchaseQtyList.contains(tempItem.productId)) {
              maxPurchaseQtyList.add(tempItem.productId);
              final int maxPurchaseQty = CommonComponentHelper.getProdSkuUtil().getMaxPurchaseQty(product);
              // display max quantity message
              errorMessage = new StringBuffer("We're sorry, the " + tempItem.itemName.trim() + " is limited to " + maxPurchaseQty + " per order. Please update the quantity.\n");
              errorMessages.add(new StockLevelMessage(tempItem.qtyWanted, errorMessage.toString()));
            }
          }
        }
        
        // checkPayPalRestricted(orderImpl, errorMessages, tempItem);
        
      } catch (final Exception e) {
        CommonComponentHelper.getLogger().error("Error looping through the TEMP ShoppingCartQtyMap (currentStockCheck) ", e);
      }
    }
    if (!oosItems.isEmpty()) {
      setOosItemsInProfile(oosItems, (NMProfile) profile);
    }
    
    if (order.getCommerceItemCount() > 0) {
      for (final String msg : stockErrorMessages) {
        errorMessages.add(new StockLevelMessage(0, msg));
      }
      
    } else {
      for (final String msg : stockErrorMessages) {
        final Message stockErrorMessage = new Message();
        stockErrorMessage.setMsgText(msg);
        messages.add(stockErrorMessage);
      }
    }
    
    if (errorMessages.size() > 0) {
      
      try {
        final NMOrderImpl theOrder = (NMOrderImpl) order;
        theOrder.setValidateLimitPromoUse(true);
        final Collection<Message> promoMessages = PricingUtil.getInstance().repriceOrder(cart, (NMProfile) profile, SnapshotUtil.STOCK_CHECK);
        final boolean displayDisqualificationMessage = promoMessages.size() > 0;
        orderMgr.updateOrder(order);
        messages.add(getFinalStockCheckMessage(errorMessages, stockMessages, promoMessages, displayDisqualificationMessage, order.getCommerceItemCount()));
        
      } catch (final Exception e) {
        CommonComponentHelper.getLogger().error("Error while repricing the order  (currentStockCheck) ", e);
      }
    } else {
      final TreeSet emptyMessages = new TreeSet();
      final NMOrderImpl theOrder = (NMOrderImpl) order;
      theOrder.setValidateLimitPromoUse(true);
      final Collection<Message> promoMessages = PricingUtil.getInstance().repriceOrder(cart, (NMProfile) profile, SnapshotUtil.LIMIT_PROMO);
      orderMgr.updateOrder(order);
      final boolean displayDisqualificationMessage = promoMessages.size() > 0;
      
      if (promoMessages.size() > 0) {
        messages.add(getFinalStockCheckMessage(emptyMessages, stockMessages, promoMessages, displayDisqualificationMessage, order.getCommerceItemCount()));
      }
    }
    
    return messages;
  }
  
  private void checkPayPalRestricted(final NMOrderImpl orderImpl, final ArrayList<StockLevelMessage> errorMessages, final TempItem tempItem,
          final HashMap<String, List<InventoryResponse.InventoryResponseLineItem>> inventory) {
    // private void checkPayPalRestricted(final List<InventoryResponseLineItem> lineItems){
    final StringBuffer errorMessage;
    final NMCreditCard paymentGroup = orderImpl.getCreditCards().iterator().next();
    final String ccType = paymentGroup.getCreditCardType();
    
    if ("Paypal".equalsIgnoreCase(ccType)) {
      CommonComponentHelper.getLogger().debug("--->> IU checkPayPalRestricted called, PayPal use detected");
      
      final Iterator commerceItemsIterator = tempItem.commerceItems.iterator();
      while (commerceItemsIterator.hasNext()) {
        final NMCommerceItem commerceItem = (NMCommerceItem) commerceItemsIterator.next();
        
        final List<InventoryResponseLineItem> lineItemList = inventory.get(commerceItem.getCmosSKUId());
        // TO FAKE REQUEST FOR UAT ITEM THAT IS OMNI (comment out above, enable below)
        // final List<InventoryResponseLineItem> lineItemList = inventory.get("2499A075301305");
        if (lineItemList != null) {
          final Iterator<InventoryResponseLineItem> items = lineItemList.iterator();
          while (items.hasNext()) {
            final InventoryResponseLineItem item = items.next();
            if (config.isLoggingDebug()) {
            	config.logDebug("-->>> IU CALLING isPayPalRestricted");
            }
            try {
              isPayPalRestricted(item, commerceItem);
            } catch (final RepositoryException e) {
              e.printStackTrace();
            }
          }
        }
      }
    }
  }
  
  private void setOosItemsInProfile(final List<NMCommerceItem> list, final NMProfile profile) {
    final List<OutOfStockItemDetails> oosItems = profile.getOosItems();
    final List skuIds = new ArrayList<String>();
    for (final OutOfStockItemDetails oos : oosItems) {
      skuIds.add(oos.getSkuId());
    }
    OutOfStockItemDetails itemDetails = null;
    NMProduct product = null;
    for (final NMCommerceItem item : list) {
      if (!skuIds.contains(item.getCatalogRefId())) {
        itemDetails = new OutOfStockItemDetails();
        product = item.getProduct();
        if (null != product) {
          itemDetails.setDataSource(product.getDataSource());
          itemDetails.setCmosCatalogId(product.getCmosCatalogId());
          itemDetails.setCmosItemCode(product.getCmosItemCode());
          itemDetails.setDisplayName(product.getDisplayName());
        }
        itemDetails.setSkuId(item.getCatalogRefId());
        itemDetails.setProductId(item.getProductId());
        itemDetails.setWebDesignerName(item.getWebDesignerName());
        itemDetails.setQty(item.getQuantity());
        itemDetails.setColor(item.getProdVariation1());
        itemDetails.setSize(item.getProdVariation2());
        itemDetails.setSuiteId(item.getSuiteId());
        // Start NMOBLDS-3260
        itemDetails.setConfigurationSetKey(item.getConfigurationKey());
        itemDetails.setOptionChoices(item.getOptionChoices());
        itemDetails.setDynamicImageUrl(item.getDynamicImageUrl());
        // Ens
        oosItems.add(itemDetails);
      }
    }
  }
  
  private int getStockLevel(final List<InventoryResponseLineItem> lineItems, final double orginalQty) throws RepositoryException {
    int stockLevel = 0;
    if (lineItems != null) {
      if (lineItems.size() == 1) {
        stockLevel = lineItems.get(0).getUpdatedQty();
      } else {
        final Iterator<InventoryResponseLineItem> i = lineItems.iterator();
        while (i.hasNext()) {
          final InventoryResponseLineItem item = i.next();
          if (item.getOriginalQty() == orginalQty) {
            stockLevel = item.getUpdatedQty();
            i.remove();
            break;
          }
        }
      }
    }
    
    return stockLevel;
  }
  
  /**
   * If error occurred and inventoryResponseLineItem is null, return stock level from DB
   * 
   * @param lineItems
   * @param catalogRefId
   * @param orginalQty
   * @return
   * @throws RepositoryException
   */
  private int
          getStockLevel(final List<InventoryResponseLineItem> lineItems, final boolean checkDB, final String catalogRefId, final long orginalQty, final String storeNo, final RepositoryItem product)
                  throws RepositoryException {
    final GenericService config = CheckoutComponents.getConfig();
    int stockLevel = 0;
    if (!checkDB) {
      if (lineItems != null) {
        if (lineItems.size() == 1) {
          stockLevel = lineItems.get(0).getUpdatedQty();
        } else {
          final Iterator<InventoryResponseLineItem> i = lineItems.iterator();
          while (i.hasNext()) {
            final InventoryResponseLineItem item = i.next();
            if (item.getOriginalQty() == orginalQty) {
              stockLevel = item.getUpdatedQty();
              config.logDebug("Product: " + catalogRefId + " is on two different line items");
              i.remove();
              break;
            }
          }
        }
      }
      config.logDebug("IU CMOS returned a stock level of " + stockLevel + " for product: " + catalogRefId);
    } else {
      final RepositoryItem sku = RepositoryUtils.getInstance().getProductRepository().getItem(catalogRefId, "sku");
      
      if (StringUtils.isBlank(storeNo)) {
        stockLevel = CommonComponentHelper.getProdSkuUtil().getStockLevel(product, sku);
        config.logDebug("IU Stock level is " + stockLevel + " for product: " + catalogRefId + ", " + product.getItemDisplayName());
      } else {
        stockLevel = getAvailableStockForPickUp(sku.getRepositoryId(), storeNo);
        if (stockLevel == 0) {
          // indicates this item should be ship to store, check regular stock levels
          stockLevel = CommonComponentHelper.getProdSkuUtil().getStockLevel(product, sku);
        }
        config.logDebug("Store Pickup - Stock level is " + stockLevel + " for product: " + catalogRefId + ", " + product.getItemDisplayName());
      }
    }
    
    return stockLevel;
  }
  
  // INSV1 Changes
  public int getAvailableStockForPickUp(final String skuId, final String storeNo) {
    int availableQty = 0;
    try {
      RepositoryItem[] items = null;
      final Repository storeSKURepo = CommonComponentHelper.getStoreSkuInventoryUtils().getStoreSkuInventoryRepository();
      final RepositoryItemDescriptor storeSKUDesc = storeSKURepo.getItemDescriptor("storeSkuInventory");
      final RepositoryView storeSKUView = storeSKUDesc.getRepositoryView();
      final RqlStatement statement = RqlStatement.parseRqlStatement("skuId = ?0 AND storeNo = ?1");
      final Object[] params = new Object[2];
      params[0] = skuId;
      params[1] = storeNo;
      items = statement.executeQuery(storeSKUView, params);
      if ((items != null) && (items.length != 0)) {
        availableQty = (Integer) items[0].getPropertyValue("qty");
      }
    } catch (final Exception e) {
      
    }
    
    return availableQty;
  }
  
  private void checkChangedQty(final ShoppingCartHandler cart, final NMOrderImpl order, final long qty, final ShippingGroupCommerceItemRelationship sgcRel, final boolean ignoreQty,
          final NMProfile profile) throws Exception {
    if (ignoreQty) {
      sgcRel.setQuantity(qty);
      sgcRel.getCommerceItem().setQuantity(qty);
      PricingUtil.getInstance().runProcessRepriceOrder("ORDER_TOTAL", cart, profile);
    } else if (qty != sgcRel.getCommerceItem().getQuantity()) {
      sgcRel.setQuantity(qty);
      sgcRel.getCommerceItem().setQuantity(qty);
      PricingUtil.getInstance().runProcessRepriceOrder("ORDER_TOTAL", cart, profile);
    }
  }
  
  public Message getFinalStockCheckMessage(final Collection errorMessages, final Collection stockMessages, final Collection promoMessages, final boolean displayDisqualificationMessage,
          final int itemCount) throws Exception {
    
    final Map<String, Object> params = new HashMap<String, Object>();
    params.put("errorMessages", errorMessages);
    params.put("messageCount", new Integer(errorMessages.size()));
    params.put("stockMessages", stockMessages);
    params.put("promoMessages", promoMessages);
    params.put("returnToShopping", new Boolean(itemCount == 0));
    params.put("displayDisqualificationMessage", new Boolean(displayDisqualificationMessage));
    params.put("limitPromoEvaluation", new Boolean(errorMessages.size() == 0));
    params.put("continueCheckoutHandledSeparately", true);
    if (NmoUtils.isNotEmptyCollection(promoMessages)) {
    	for (final Object promoMsg : promoMessages) {
    		String promoMsgId = ((Message)promoMsg).getMsgId();
    		if (promoMsg != null && promoMsg instanceof Message &&	promoMsgId.contains("_PLCC_ERROR_")) {
    			params.put("showOnlyPLLCPromoError", Boolean.TRUE);
    			params.put("PLCCPromoCode", promoMsgId.substring(promoMsgId.lastIndexOf('_')+1));
    			break;
    		}
    	}
    }
    final Message msg = new Message();
    msg.setFrgName("/page/checkoutb/messages/finalStockCheck.jsp");
    msg.setExtraParams(params);
    msg.setMsgId("finalStockCheck");
    msg.setError(false);
    
    return msg;
  }
  
  public Collection<Message> checkCmosStockLevel(final ShoppingCartHandler cart, final boolean allocate, final NMProfile profile) throws Exception {
    final List<Message> messages = new ArrayList<Message>();
    final NMOrderImpl order = cart.getNMOrder();
    final NMOrderHolder orderHolder = (NMOrderHolder) cart.getOrderHolder();
    final OrderManager orderManager = cart.getOrderManager();
    final GenericService config = CheckoutComponents.getConfig();
    final ConfiguratorUtils configuratorUtils = CommonComponentHelper.getConfiguratorUtils();
    // Populate CMOS Inventory Request
    final InventoryRequest inventoryRequest = new InventoryRequest();
    inventoryRequest.setExternalOrderNumber(order.getId());
    final String brandCode = ComponentUtils.getInstance().getSystemSpecs().getProductionSystemCode();
    inventoryRequest.setBrandCode(brandCode);
    
    if (CartUtil.getInstance().isInternationalSession(profile)) {
      inventoryRequest.setVendorId(FiftyOneUtils.VENDOR_ID);
    }
    
    @SuppressWarnings({"unchecked" , "rawtypes"})
    final Iterator<NMCommerceItem> i = new ArrayList(order.getCommerceItems()).iterator();
    
    while (i.hasNext()) {
      final NMCommerceItem ci = i.next();
      final Boolean isSameDayDelivery = StringUtilities.isNotEmpty(ci.getFulfillmentFacility()) && ServiceLevel.SL0_SERVICE_LEVEL_TYPE.equals(ci.getServicelevel());
      final Boolean isPickupInStore = StringUtilities.isNotEmpty(ci.getPickupStoreNo()) && !ci.getShipToStoreFlg();
      String facilityId = "";
      if (isSameDayDelivery || isPickupInStore) {
        if (isSameDayDelivery) {
          facilityId = ci.getFulfillmentFacility();
        } else {
          facilityId = ci.getPickupStoreNo();
        }
      }
      inventoryRequest.addLineItem(NmoUtils.invalidCharacterCleanup(ci.getId()), ci.getCmosSKUId(), ci.getCmosCatalogId(), ci.getCmosItemCode(), new Long(ci.getQuantity()).intValue(),
              ci.isPickupInStore(), facilityId);
    }
    
    // Send request to CMOS
    final InventoryResponse inventoryResponse = com.nm.utils.InventoryUtil.getInstance().sendInventoryRequestToCmos(inventoryRequest, allocate);
    
    final HashMap<String, List<InventoryResponse.InventoryResponseLineItem>> inventory = inventoryResponse.getLineItemMap();
    // Sort needed to display error messages next to the correct line item
    @SuppressWarnings("unchecked")
    final Collection<NMCommerceItem> sortedOrder = sortItemsInOrderToMatchCartPage(order.getCommerceItems());
    final Iterator<NMCommerceItem> ci = sortedOrder.iterator();
    
    final List<NMCommerceItem> oosItems = new ArrayList<NMCommerceItem>();
    final List<NMCommerceItem> list = order.getCommerceItems();
    final List<String> orderSkus = new ArrayList<String>();
    
    // If CMOS determines not all inventory is available - update qty and create stock level message
    // If error occurred, system will validate inventory against db
    if (inventoryResponse.isErrorOrccured() || inventoryResponse.isQuantitiesUpdated()) {
      int count = 0;
      for (final NMCommerceItem cItem : list) {
        orderSkus.add(cItem.getCatalogRefId());
      }
      final List<OutOfStockItemDetails> oosProducts = profile.getOosItems();
      for (final OutOfStockItemDetails oos : oosProducts) {
        if (orderSkus.contains(oos.getSkuId())) {
          oosProducts.remove(oos);
        }
      }
      while (ci.hasNext()) {
        final NMCommerceItem nmCItem = ci.next();
        final List<InventoryResponseLineItem> lineItemList = inventory.get(nmCItem.getCmosSKUId());
        final int stockLevel =
                getStockLevel(lineItemList, inventoryResponse.isErrorOrccured(), nmCItem.getCatalogRefId(), nmCItem.getQuantity(), nmCItem.getPickupStoreNo(), (RepositoryItem) nmCItem
                        .getAuxiliaryData().getProductRef());
        if (stockLevel < nmCItem.getQuantity()) {
          LoggingUtil.logRealtimeInventoryCheckInfo(profile, nmCItem, stockLevel, false);
          if (stockLevel == 0) {
            OrderUtil.getInstance().removeItem(orderHolder, orderManager, nmCItem.getId(), cart);
            if (CommonComponentHelper.getBrandSpecs().isEnableCartOOSRecommendation()
                    && !CheckoutAPI.isInternationalSession(profile)) {
              final Message errorMessage = new Message();
              errorMessage.setMsgText("Item is no longer in stock");
              messages.add(errorMessage);
              oosItems.add(nmCItem);
            } else {
              final String variationString = getVariationString(nmCItem.getProdVariation1(), nmCItem.getProdVariation2());
              messages.add(buildStockLevelErrorMessage(nmCItem.getQuantity(), nmCItem.getProduct().getWebProductDisplayName(), stockLevel, variationString, count));
            }
          } else {
            final String variationString = getVariationString(nmCItem.getProdVariation1(), nmCItem.getProdVariation2());
            messages.add(buildStockLevelErrorMessage(nmCItem.getQuantity(), nmCItem.getProduct().getWebProductDisplayName(), stockLevel, variationString, count));
            nmCItem.setQuantity(stockLevel);
            List<ShippingGroupCommerceItemRelationship> relationships = nmCItem.getShippingGroupRelationships();
            for (final Iterator<ShippingGroupCommerceItemRelationship> it = relationships.iterator(); it.hasNext();) {
              final ShippingGroupCommerceItemRelationship relationship = it.next();
              relationship.setQuantity(stockLevel);
            }
            count++;
          }
        } else {
          count++;
          // Start NMOBLDS-3260
          if (!StringUtils.isBlank(nmCItem.getConfigurationKey()) && configuratorUtils.evaluateSelectionSet(nmCItem, profile.getCountryPreference())) {
            evaluateOOSConfiguredItems(cart, messages, orderHolder, orderManager, oosItems, nmCItem);
          }
          // End
        }
      }
    } else {
      try {
        while (ci.hasNext()) {
          final NMCommerceItem nmCItem = ci.next();
          if (!StringUtils.isBlank(nmCItem.getConfigurationKey()) && configuratorUtils.evaluateSelectionSet(nmCItem, profile.getCountryPreference())) {
            evaluateOOSConfiguredItems(cart, messages, orderHolder, orderManager, oosItems, nmCItem);
          }
        }
      } catch (Exception e) {
        config.logDebug("Configurator App is Down:" + e.getMessage());
      }
    }
    if (!oosItems.isEmpty()) {
      setOosItemsInProfile(oosItems, profile);
    }
    while (ci.hasNext()) {
      final NMCommerceItem cur = ci.next();
      final List<InventoryResponseLineItem> lineItemList = inventory.get(cur.getCmosSKUId());
      // TO FAKE REQUEST FOR UAT ITEM THAT IS OMNI (comment out above, enable below)
      // ** final List<InventoryResponseLineItem> lineItemList = inventory.get("2499A075301305");
      if (lineItemList != null) {
        final Iterator<InventoryResponseLineItem> items = lineItemList.iterator();
        while (items.hasNext()) {
          final InventoryResponseLineItem item = items.next();
          if (config.isLoggingDebug()) { 
        	  config.logDebug("-->>> IU CALLING isPayPalRestricted");
          }
          isPayPalRestricted(item, cur);
        }
      }
    }
    
    if (!messages.isEmpty()) {
      orderManager.updateOrder(order);
    }
    return messages;
  }
  
  /**
   * Evaluate oos configured items.
   * 
   * @param cart
   *          the cart
   * @param messages
   *          the messages
   * @param orderHolder
   *          the order holder
   * @param orderManager
   *          the order manager
   * @param oosItems
   *          the oos items
   * @param nmCItem
   *          the nm c item
   * @throws Exception
   *           the exception
   */
  private void evaluateOOSConfiguredItems(final ShoppingCartHandler cart, final List<Message> messages, final NMOrderHolder orderHolder, final OrderManager orderManager,
          final List<NMCommerceItem> oosItems, final NMCommerceItem nmCItem) throws Exception {
    OrderUtil.getInstance().removeItem(orderHolder, orderManager, nmCItem.getId(), cart);
    final Message errorMessage = new Message();
    errorMessage.setMsgText("We're sorry one or more options of your configure item is not available");
    messages.add(errorMessage);
    oosItems.add(nmCItem);
  }
  
  private boolean isPayPalRestricted(final InventoryResponseLineItem lineItem, final NMCommerceItem ci) throws RepositoryException {
    final GenericService config = CheckoutComponents.getConfig();
    if (config.isLoggingDebug()) { 
    	config.logDebug("-->>> IU INSIDE isPayPalRestricted");
    }
    // Checks for mainly Omni BG products that come from facilities 63,64,65
    // but should hold any other restricted PP items
    // that cannot be purchased with PP
    List<String> OmniFacilities = null;
    OmniFacilities = Arrays.asList(ComponentUtils.getInstance().getSystemSpecs().getPayPalRestrictedFacility());
    
    boolean omniChannelProduct = false;
    String facilityId = "";
    
    if (OmniFacilities != null) {
      if (lineItem != null) {
        facilityId = lineItem.getFacility();
        if (OmniFacilities.contains(facilityId)) {
          ci.setPayPalRestricted(true);
          omniChannelProduct = true;
          if (config.isLoggingDebug()) {
        	  config.logDebug("-->>> IU isPayPalRestricted has found omniChannelProduct:" + ci.getCmosItemCode() + "-" + lineItem.getFacility());
          }
        }
      }
    }
    
    return omniChannelProduct;
  }
  
  /**
   * Builds stock level error message if inventory is not available in CMOS
   * 
   * @param qtyWanted
   * @param itemName
   * @param stockLevel
   * @param variationString
   * @return
   */
  private Message buildStockLevelErrorMessage(final long qtyWanted, final String itemName, final int stockLevel, final String variationString, final int count) {
    final Message message = MessageDefs.getMessage(MessageDefs.MSG_QuantityNotAvailable);
    final StringBuffer errorMessage = new StringBuffer();
    
    if (stockLevel == 0) {
      errorMessage.append("This item ");
      errorMessage.append(itemName.trim()).append(variationString).append(" is no longer in stock.");
      message.setFieldId("cartHeader");
    } else {
      errorMessage.append("We no longer have [");
      errorMessage.append(qtyWanted).append("] available in the <B>").append(itemName.trim());
      errorMessage.append("</B>.  We have [").append(stockLevel).append("] still available.");
      message.setFieldId("qtyText_" + count);
    }
    
    message.setMsgText(errorMessage.toString());
    
    return message;
  }
  
  @SuppressWarnings("unchecked")
  private Collection<NMCommerceItem> sortItemsInOrderToMatchCartPage(final Collection<NMCommerceItem> itemsInUsersOrder) {
    final Collection<NMCommerceItem> copy = new ArrayList<NMCommerceItem>();
    copy.addAll(itemsInUsersOrder);
    final ArrayList<String> sortFieldList = new ArrayList<String>();
    sortFieldList.add("commerceItemDate");
    sortFieldList.add("id");
    
    final ArrayList<Boolean> sortDirection = new ArrayList<Boolean>();
    sortDirection.add(Boolean.FALSE);
    sortDirection.add(Boolean.FALSE);
    
    return CollectionUtils.dynamicCollectionSort(copy, sortFieldList, sortDirection);
  }
  
  public String getVariationString(final String variation1, final String variation2) {
    String returnValue = "";
    
    if ((variation1 != null) && !variation1.equals("")) {
      returnValue += ", " + variation1.trim();
    }
    
    if ((variation2 != null) && !variation2.equals("")) {
      returnValue += ", " + variation2.trim();
    }
    
    return returnValue;
  }
  
  // These items are used to tally the total quantity requested for a given
  // product.
  class TempItem {
    private String itemName;
    private String productId;
    private int qtyWanted;
    private int skuQty;
    private String variation1;
    private String variation2;
    private String discontinuedCode;
    private boolean exclusiveFlag;
    private double qtyOnOrder;
    private boolean dropshipFlag;
    private final List<NMCommerceItem> commerceItems = new ArrayList<NMCommerceItem>();
    private String strPickupStoreFlg;
    private String strSKUId;
    private String skuId;
    private String id;
    private String cmosCatalogId;
    private String cmosItemCode;
    private boolean storeFulfillFlag;
    private String facilityId;
    private String configurationKey;
    private String dynamicImageUrl;
    
    public String getVariationString() {
      String returnValue = "";
      
      if ((variation1 != null) && !variation1.equals("")) {
        returnValue += ", " + this.variation1.trim();
      }
      
      if ((variation2 != null) && !variation2.equals("")) {
        returnValue += ", " + variation2.trim();
      }
      
      return returnValue;
    }
  }
}
