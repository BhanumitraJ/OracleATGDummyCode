/*
 * <ATGCOPYRIGHT> Copyright (C) 1997-2001 Art Technology Group, Inc. All Rights Reserved. No use, copying or distribution of this work may be made except in accordance with a valid license agreement
 * from Art Technology Group. This notice must be included on all copies, modifications and derivatives of this work.
 * 
 * Art Technology Group (ATG) MAKES NO REPRESENTATIONS OR WARRANTIES ABOUT THE SUITABILITY OF THE SOFTWARE, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, OR NON-INFRINGEMENT. ATG SHALL NOT BE LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR DISTRIBUTING THIS SOFTWARE OR
 * ITS DERIVATIVES.
 * 
 * "Dynamo" is a trademark of Art Technology Group, Inc. </ATGCOPYRIGHT>
 */

package com.nm.commerce.promotion;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.transaction.TransactionManager;

import atg.commerce.CommerceException;
import atg.commerce.order.CommerceItem;
import atg.commerce.order.ItemAddedToOrder;
import atg.commerce.order.ItemQuantityChanged;
import atg.commerce.order.Order;
import atg.commerce.order.OrderManager;
import atg.commerce.order.ShippingGroup;
import atg.commerce.order.ShippingGroupCommerceItemRelationship;
import atg.commerce.promotion.AddItemToOrder;
import atg.commerce.promotion.PromotionConstants;
import atg.dtm.TransactionDemarcation;
import atg.dtm.TransactionDemarcationException;
import atg.nucleus.naming.ComponentName;
import atg.process.ProcessException;
import atg.process.ProcessExecutionContext;
import atg.repository.MutableRepositoryItem;
import atg.repository.RepositoryException;
import atg.repository.RepositoryItem;
import atg.scenario.ScenarioException;

import com.nm.commerce.NMCommerceItem;
import com.nm.commerce.order.NMOrderManager;
import com.nm.components.CommonComponentHelper;

/**
 * This action adds free gift items associated with the qualifying products into shoppingcart
 * 
 * @author Chee-Chien Loo
 * @version $Id: AddFreeGiftWithPurchaseAction.java 1.5 2012/07/06 13:19:46CDT Richard A Killen (NMRAK3) Exp $
 */

public class AddFreeGiftWithPurchaseAction extends AddItemToOrder {
  
  /** Parameter for Free gift promotion type and Promotion String */
  
  public static final String PROMO_TYPE_PARAM = "promo_type";
  public static final String PROMO_STR = "Promotion";
  public static final String PROMO_FLAG_PROPERTYNAME = "flgPromoQualifier";
  public static final String PROMO_PRODS_PROPERTYNAME = "promoProds";
  
  public void initialize(@SuppressWarnings("rawtypes") Map pParameters) throws ProcessException {
    /** Resolve OrderManager and Promotion components. */
    mOrderManager = OrderManager.getOrderManager();
    
    if (mOrderManager == null) throw new ScenarioException(PromotionConstants.getStringResource(PromotionConstants.ORDER_MANAGER_NOT_FOUND));
    
    mOrderHolderComponent = ComponentName.getComponentName("/atg/commerce/ShoppingCart");
    storeOptionalParameter(pParameters, PROMO_TYPE_PARAM, java.lang.String.class);
    
  }
  
  protected void executeAction(ProcessExecutionContext pContext) throws ProcessException {
    
    TransactionDemarcation td = new TransactionDemarcation();
    boolean rollBack = false;
    
    try {
      TransactionManager tm = CommonComponentHelper.getTransactionManager();
      if (tm != null) {
        td.begin(tm, TransactionDemarcation.REQUIRED);
      }
    } catch (TransactionDemarcationException tde) {
    	if (mOrderManager.isLoggingError()) {
    		mOrderManager.logError("TransactionDemarcationException: AddFreeGiftWithPurchaseAction.java in executeAction() ", tde);
    	}
      throw new ScenarioException(tde);
    }// end-try
    try {
      
      if (mOrderManager == null) throw new ScenarioException(PromotionConstants.getStringResource(PromotionConstants.ORDER_MANAGER_NOT_FOUND));
      
      // Get the promo type from scenario parameter
      String promoType = (String) getParameterValue(PROMO_TYPE_PARAM, pContext);
      
      // Check if it is a message of type ItemAddedToOrder
      if (pContext.getMessage() instanceof ItemAddedToOrder || pContext.getMessage() instanceof ItemQuantityChanged) {
        
        ItemQuantityChanged iOrderEdit = null;
        ItemAddedToOrder iOrderAdd = null;
        
        RepositoryItem prodItem = null;
        boolean isEdit = false;
        
        // new
        if (pContext.getMessage() instanceof ItemAddedToOrder) {
          iOrderAdd = (ItemAddedToOrder) pContext.getMessage();
          
          // Get the product item from the message
          prodItem = (RepositoryItem) iOrderAdd.getProduct();
          isEdit = false;
        }
        
        else if (pContext.getMessage() instanceof ItemQuantityChanged) {
          iOrderEdit = (ItemQuantityChanged) pContext.getMessage();
          
          // Get the product item from the message
          prodItem = (RepositoryItem) iOrderEdit.getProduct();
          isEdit = true;
        }
        
        if (prodItem != null) {
          
          // Get the promoFlag from the product item. if it is true, get the promo items associated with it
          Boolean promoFlag = (Boolean) prodItem.getPropertyValue(PROMO_FLAG_PROPERTYNAME);
          if (promoFlag == null) {
            promoFlag = new Boolean(false);
          }
          
          // If the promotion flag is true, get a list of free gift items associated with the qualifying product.
          
          if (mOrderManager.isLoggingDebug()) mOrderManager.logDebug("*******************************************************promo " + promoFlag);
          
          if (promoFlag.booleanValue()) {
            // Get the list of free gift items associated with the product
            @SuppressWarnings("unchecked")
            Set<MutableRepositoryItem> promoProdItems = (Set<MutableRepositoryItem>) prodItem.getPropertyValue(PROMO_PRODS_PROPERTYNAME);
            String qualifyingProductName = (String) prodItem.getPropertyValue("displayName");
            String productID, skuID;
            String timeStamp = Long.toString(System.currentTimeMillis());
            
            if (mOrderManager.isLoggingDebug()) mOrderManager.logDebug("qualifyingProductName " + qualifyingProductName);
            
            if (promoProdItems != null) {
              if (mOrderManager.isLoggingDebug()) mOrderManager.logDebug("In AddFreeGiftWithPurchase Action, iterating through promo product list....");
              
              Iterator<MutableRepositoryItem> prodIterator = promoProdItems.iterator();
              while (prodIterator.hasNext()) {
                RepositoryItem promoProdItem = (RepositoryItem) prodIterator.next();
                if (promoProdItem != null) {
                  Double price = (Double) promoProdItem.getPropertyValue("retailPrice");
                  @SuppressWarnings("unchecked")
                  List<String> siCodes = (List<String>) promoProdItem.getPropertyValue("codes");
                  Boolean displayFlag = (Boolean) promoProdItem.getPropertyValue("flgDisplay");
                  String promoProductName = (String) promoProdItem.getPropertyValue("displayName");
                  
                  if ((price.doubleValue() == 0) && ((siCodes == null) || (siCodes.size() == 0))) {
                    if (displayFlag.booleanValue()) {
                      productID = promoProdItem.getRepositoryId();
                      @SuppressWarnings("unchecked")
                      List<MutableRepositoryItem> skuItems = (List<MutableRepositoryItem>) promoProdItem.getPropertyValue("childSKUs");
                      
                      // Assumption of there is only one sku item for each free gift product
                      if (skuItems.size() == 1) {
                        RepositoryItem skuItem = (RepositoryItem) skuItems.get(0);
                        if (skuItem == null) {
                          throw new ScenarioException(PromotionConstants.getStringResource(PromotionConstants.SKU_DOES_NOT_EXIST));
                        } else {
                          skuID = skuItem.getRepositoryId();
                          // Add the free gift item to the cart
                          try {
                            String qualifyingPromoType = "qualifier for " + promoProductName;
                            
                            if (qualifyingPromoType.length() > 250) {
                              qualifyingPromoType = qualifyingPromoType.substring(0, 250);
                            }
                            NMCommerceItem nmci;
                            if (isEdit) {
                              nmci = (NMCommerceItem) iOrderEdit.getCommerceItem();
                            } else {
                              nmci = (NMCommerceItem) iOrderAdd.getCommerceItem();
                            }
                            long theQty = nmci.getQuantity();
                            if (mOrderManager.isLoggingDebug()) mOrderManager.logDebug("the quantity in the add action is:" + theQty);
                            
                            if (mOrderManager.isLoggingDebug()) mOrderManager.logDebug("promoProductName: " + promoProductName);
                            if (mOrderManager.isLoggingDebug()) mOrderManager.logDebug("timestamp: " + timeStamp);
                            Order order = null;
                            if (pContext.getMessage() instanceof ItemAddedToOrder) {
                              order = ((ItemAddedToOrder) pContext.getMessage()).getOrder();
                            } else if (pContext.getMessage() instanceof ItemQuantityChanged) {
                              order = ((ItemQuantityChanged) pContext.getMessage()).getOrder();
                            }
                            if (updateLinkedPromoQuantity(order, nmci, promoProdItem)) {
                              // do nothing: item quantity was updated, no need for a new line item in the cart
                            } else {
                              nmci.setPromoTimeStamp(timeStamp);
                              String currPromoType = nmci.getPromoType();
                              if (currPromoType == null) {
                                nmci.setPromoType(qualifyingPromoType);
                              } else {// check length
                                String theWholeType = currPromoType + " " + qualifyingPromoType;
                                if (theWholeType.length() > 250) {
                                  theWholeType = theWholeType.substring(0, 250);
                                }
                                nmci.setPromoType(theWholeType);
                              }
                              addItem(skuID, productID, theQty, promoType, pContext, timeStamp, qualifyingProductName);
                            }
                          } catch (CommerceException ce) {
                            throw new ScenarioException(ce);
                          } catch (RepositoryException re) {
                            throw new ScenarioException(re);
                          }
                        } // end else
                      } // end if size() == 1
                    } // if (displayFlag.booleanValue())
                    else {
                      if (mOrderManager.isLoggingDebug()) mOrderManager.logDebug("This product (" + promoProdItem.getRepositoryId() + ") is not displayable.");
                    }
                  } // if ((price.doubleValue() == 0) && ( (siCodes == null) || (siCodes.size() == 0) ))
                  else {
                    if (mOrderManager.isLoggingDebug()) mOrderManager.logDebug("This product (" + promoProdItem.getRepositoryId() + ") is not a free gift item or it has special instructions.");
                  }
                  
                  // TS5 break; // only evaluate one promo product per qualifying product...
                  
                } // if promoProdItem != null
              } // while loop
            } // if promoProdItems != null
          } // if promoFlag is true
        } // if prodItem != null
      } // if itemAddedToOrder msg
      
    } catch (Exception e) {
      rollBack = true;
      if (mOrderManager.isLoggingError()) mOrderManager.logError("AddFreeGiftWithPurchaseAction.executeAction(): Exception ");;
      
      throw new ScenarioException(e);
    } finally {
      try {
        td.end(rollBack); // commit work
      } catch (TransactionDemarcationException tde) {
      	if (mOrderManager.isLoggingError()) {
    		mOrderManager.logError("TransactionDemarcationException: AddFreeGiftWithPurchaseAction.java in executeAction() ", tde);
    	}
        throw new ScenarioException(tde);
      }// end-try
    }// end-try
    
  } // end of executeAction method
  
  /*
   * Given a product/promo pair (prodCommerceItem/promoRepositoryItem) this method checks if the cart contains the promo associated with the product. If so, when quantity changes are made to the
   * product, this method propagates them to the associated promo but if not, false is returned, and the caller adds a new line item to the cart for the promo
   */
  protected boolean updateLinkedPromoQuantity(Order order, NMCommerceItem prodCommerceItem, RepositoryItem promoRepositoryItem) {
    boolean isUpdated = false;
    boolean cartHasPromoItem = false;
    
    try {
      
      String promoId = (String) promoRepositoryItem.getPropertyValue("id");
      String prodStamp = prodCommerceItem.getPromoTimeStamp().trim();
      String linkedId, linkedStamp;
      List<ShippingGroupCommerceItemRelationship> linkedCommerceItems = ((NMOrderManager) OrderManager.getOrderManager()).getShippingGroupCommerceItemRelationships(order);
      Iterator<ShippingGroupCommerceItemRelationship> linkedItemsIterator = null;
      
      // check if the promoItem linked to the current prodItem is in the cart
      linkedItemsIterator = linkedCommerceItems.iterator();
      // System.out.println(linkedItemCount + "-----------------------------------------------------------------------");
      loop : while (linkedItemsIterator.hasNext()) {
        ShippingGroupCommerceItemRelationship promoLink = (ShippingGroupCommerceItemRelationship) linkedItemsIterator.next();
        NMCommerceItem linkedItem = (NMCommerceItem) promoLink.getCommerceItem();
        linkedStamp = linkedItem.getPromoTimeStamp().trim();
        linkedId = linkedItem.getAuxiliaryData().getProductId();
        
        // System.out.println("--STAMPS prod:linked:promo ["+prodStamp+":"+linkedStamp+":N/A]");
        // System.out.println("--IDENTS prod:linked:promo ["+prodId+":"+linkedId+":"+promoId+"]");
        if (linkedStamp.equals(prodStamp)) {
          // System.out.println("--prodStamp matches linkedStamp: this cartItem is related");
          if (linkedId.equals(promoId)) {
            // System.out.println("--and linkedId matches promoId: this cartItem is the one");
            cartHasPromoItem = true;
            break loop;
          }
        }
      }
      
      // if the correct promoItem is in the cart update the quantity
      if (cartHasPromoItem) {
        linkedItemsIterator = linkedCommerceItems.iterator();
        while (linkedItemsIterator.hasNext()) {
          ShippingGroupCommerceItemRelationship promoLink = (ShippingGroupCommerceItemRelationship) linkedItemsIterator.next();
          NMCommerceItem linkedItem = (NMCommerceItem) promoLink.getCommerceItem();
          linkedStamp = linkedItem.getPromoTimeStamp().trim();
          
          if (linkedItem.getId().equals(prodCommerceItem.getId())) {
            // do nothing: prodItem has found itself as a linkedItem
          } else if (linkedStamp.equals(prodStamp)) {
            promoLink.setQuantity(prodCommerceItem.getQuantity());
            promoLink.getCommerceItem().setQuantity(prodCommerceItem.getQuantity());
            NMOrderManager.getOrderManager().updateOrder(order);
            isUpdated = true;
          }
        }
      }
    } catch (Exception e) {
      	if (mOrderManager.isLoggingError()) {
    		mOrderManager.logError("Exception: AddFreeGiftWithPurchaseAction.java in updateLinkedPromoQuantity() ", e);
    	}
    }
    return isUpdated;
  }
  
  /**
   * This method will add item to order based on NM's business requirement, adding each item as a separate line item. Additional NM's required commerce item information is added in this method.
   * 
   * <P>
   * 
   * If different behavior is desired when an item is being added to the order, this method should be overriden.
   * 
   * @param pSkuId
   *          the sku id of the commerce item that will be created and added to the order
   * @param pProductId
   *          product id of the commerce item that will be created and added
   * @param pQuantity
   *          the quantity of the particular item to add
   * @param pContext
   *          the context in which the action is occuring
   * @exception CommerceException
   *              if an error occurs
   * @exception RepositoryException
   *              if an error occurs
   */
  protected void addItem(String pSkuId, String pProductId, long pQuantity, String promoType, ProcessExecutionContext pContext, String timeStamp, String qualifyingProductName)
          throws CommerceException, RepositoryException {
    Order o = getOrderToAddItemTo(pContext);
    
    // System.out.println(o.getId());
    
    if (o != null) {
      @SuppressWarnings("unchecked")
      List<ShippingGroup> sgs = o.getShippingGroups();
      if (sgs != null && sgs.size() > 0) {
        ShippingGroup sg = (ShippingGroup) o.getShippingGroups().get(0);
        
        // nmts5 for(int ciNo=0; ciNo < ciQuantity; ciNo++)
        // {
        if (mOrderManager instanceof NMOrderManager) {
          NMOrderManager om = (NMOrderManager) mOrderManager;
          CommerceItem ci = om.addSeparateItemToShippingGroup(o, pSkuId, pProductId, pQuantity, sg);
          if (ci instanceof NMCommerceItem) {
            // Populate data for extended commerce item properties
            
            NMCommerceItem NMci = (NMCommerceItem) ci;
            RepositoryItem prodItem = (RepositoryItem) NMci.getAuxiliaryData().getProductRef();
            RepositoryItem skuItem = (RepositoryItem) NMci.getAuxiliaryData().getCatalogRef();
            
            NMci.setCmosCatalogId((String) prodItem.getPropertyValue("cmosCatalogId"));
            NMci.setCmosItemCode((String) prodItem.getPropertyValue("cmosItemCode"));
            NMci.setCmosProdName((String) prodItem.getPropertyValue("displayName"));
            NMci.setCmosSKUId((String) skuItem.getPropertyValue("cmosSKU"));
            NMci.setDeptCode((String) prodItem.getPropertyValue("deptCode"));
            NMci.setSourceCode((String) prodItem.getPropertyValue("sourceCode"));
            NMci.setWebDesignerName((String) prodItem.getPropertyValue("cmDesignerName"));
            
            String variation1 = null;
            String variation2 = null;
            String variation3 = null;
            try {
              @SuppressWarnings("unchecked")
              Map<String, MutableRepositoryItem> tmpMapVariation = (Map<String, MutableRepositoryItem>) skuItem.getPropertyValue("skuProdInfo");
              RepositoryItem ri = (RepositoryItem) tmpMapVariation.get(prodItem.getRepositoryId());
              variation1 = (String) ri.getPropertyValue("cmVariation1");
              variation2 = (String) ri.getPropertyValue("cmVariation2");
              variation3 = (String) ri.getPropertyValue("cmVariation3");
            } catch (Exception e) {
            	if (mOrderManager.isLoggingError()) {
            		String item = prodItem.getRepositoryId();
            		mOrderManager.logError("An exception occurred in AddFreeGiftWithPurchaseActionCMOS:addItem>" + e);
            		mOrderManager.logError("Variations will not be set for product:" + item);
            	}
            }
            if (variation1 == null) {
              NMci.setProdVariation1("");
            } else {
              NMci.setProdVariation1(variation1);
            }
            
            if (variation2 == null) {
              NMci.setProdVariation2("");
            } else {
              NMci.setProdVariation2(variation2);
            }
            
            if (variation3 == null) {
              NMci.setProdVariation3("");
            } else {
              NMci.setProdVariation3(variation3);
            }
            
            NMci.setQuickOrder(false);
            NMci.setPerishable(false);
            NMci.setPromoTimeStamp(timeStamp);
            
            // This is hardcoded for the assumption that the item added is an promotional item
            NMci.setTransientStatus(PROMO_STR);
            // NMci.setPromoType(promoType);
            String promoTypeWithQualName = "free gift with purchase of " + qualifyingProductName;
            
            if (promoTypeWithQualName.length() > 250) {
              promoTypeWithQualName = promoTypeWithQualName.substring(0, 250);
            }
            
            NMci.setPromoType(promoTypeWithQualName);
            NMci.setPromoName((String) prodItem.getPropertyValue("displayName"));
            NMci.setCoreMetricsCategory("gwp");
            
          }
        }
        // end for nmts5 }
      }
    }
  }
}
