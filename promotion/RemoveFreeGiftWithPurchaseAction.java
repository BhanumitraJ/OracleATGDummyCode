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

import atg.commerce.CommerceException;
import atg.commerce.order.CommerceItem;
import atg.commerce.order.CommerceItemRelationship;
import atg.commerce.order.ItemRemovedFromOrder;
import atg.commerce.order.Order;
import atg.commerce.order.OrderHolder;
import atg.commerce.order.OrderManager;
import atg.commerce.promotion.PromotionConstants;
import atg.commerce.states.StateDefinitions;
import atg.nucleus.Nucleus;
import atg.nucleus.naming.ComponentName;
import atg.repository.MutableRepositoryItem;
import atg.repository.RepositoryException;
import atg.repository.RepositoryItem;
import atg.scenario.ScenarioException;
import atg.scenario.ScenarioExecutionContext;
import atg.scenario.action.ActionImpl;
import atg.servlet.DynamoHttpServletRequest;

import com.nm.commerce.NMCommerceItem;

/**
 * This action removes free gift items associated with the qualifying products into shoppingcart
 * 
 * @author Chee-Chien Loo
 * @version $Id: RemoveFreeGiftWithPurchaseAction.java 1.3 2008/08/14 13:25:29CDT nmrww3 Exp $
 */

public class RemoveFreeGiftWithPurchaseAction extends ActionImpl {
  
  // -------------------------------------
  /** Class version string */
  public static final String CLASS_VERSION = "$Id: RemoveFreeGiftWithPurchaseAction.java 1.3 2008/08/14 13:25:29CDT nmrww3 Exp  $";
  
  /** Parameter for the location of the ordermanager */
  public String ORDERMANAGER_PATH = "/atg/commerce/order/OrderManager";
  
  /** Parameter for Free gift promotion type and Promotion String */
  
  public static final String PROMO_TYPE_PARAM = "promo_type";
  public String PROMO_STR = "Promotion";
  public static final String PROMO_FLAG_PROPERTYNAME = "flgPromoProd";
  public static final String PROMOQUAL_FLAG_PROPERTYNAME = "flgPromoQualifier";
  // public static final String PROMO_PRODS_PROPERTYNAME = "promoProds";
  
  /** reference to the order manager object */
  protected OrderManager mOrderManager = null;
  
  protected ComponentName mOrderHolderComponent = null;
  
  public void initialize(Map pParameters) throws ScenarioException {
    
    /** Resolve OrderManager and Promotion components. */
    
    mOrderManager = (OrderManager) Nucleus.getGlobalNucleus().resolveName(ORDERMANAGER_PATH);
    
    if (mOrderManager == null) throw new ScenarioException(PromotionConstants.getStringResource(PromotionConstants.ORDER_MANAGER_NOT_FOUND));
    
    mOrderHolderComponent = ComponentName.getComponentName("/atg/commerce/ShoppingCart");
    storeOptionalParameter(pParameters, PROMO_TYPE_PARAM, java.lang.String.class);
    
  }
  
  protected void executeAction(ScenarioExecutionContext pContext) throws ScenarioException {
    
    if (mOrderManager == null) throw new ScenarioException(PromotionConstants.getStringResource(PromotionConstants.ORDER_MANAGER_NOT_FOUND));
    
    // Get the promo type from scenario parameter
    String promoType = (String) getParameterValue(PROMO_TYPE_PARAM, pContext);
    // Check if it is a message of type ItemRemovedFromOrder
    if (pContext.getMessage() instanceof ItemRemovedFromOrder) {
      
      ItemRemovedFromOrder iOrder = (ItemRemovedFromOrder) pContext.getMessage();
      RepositoryItem prodItem = (RepositoryItem) iOrder.getProduct();
      CommerceItem cItem = (CommerceItem) iOrder.getCommerceItem();
      
      if ((cItem != null) && (cItem instanceof NMCommerceItem)) {
        NMCommerceItem nmCI = (NMCommerceItem) cItem;
        // First, get info about the product that has been deleted (the prodItem), and the specifics on the CommerceItem
        Boolean prodItemFlgPromoQualifier = (Boolean) prodItem.getPropertyValue(PROMOQUAL_FLAG_PROPERTYNAME);
        Boolean prodItemFlgPromoProd = (Boolean) prodItem.getPropertyValue(PROMO_FLAG_PROPERTYNAME);
        String prodItemTimeStamp = (String) nmCI.getPromoTimeStamp();
        
        if (prodItemFlgPromoQualifier == null) {
          prodItemFlgPromoQualifier = new Boolean(false);
        }
        
        if (prodItemFlgPromoProd == null) {
          prodItemFlgPromoProd = new Boolean(false);
        }
        
        if (prodItemTimeStamp == null) {
          prodItemTimeStamp = "";
        }
        // Then execute one of these scenarios (This remove scenario matches the removeSIItem in SpecialInstructionsFormHandler)
        // Scenario 1 - No timestamp on the requested delete, assume no constraints
        if ((prodItemTimeStamp.length()) < 2) { // Just remove the index - this item isn't involved in a promotional link
          // In this class, this is a no-op. It is here for consistency with SpecialInstructionFormHandler.removeSIItem()
        } else
        
        // Scenario 2 - Timestamp and flgPromoQualifier indicate that we should delete all items with timestamp
        if ((prodItemFlgPromoQualifier.booleanValue()) && (!prodItemFlgPromoProd.booleanValue()) && ((prodItemTimeStamp.length()) > 2)) { // We will remove all products with this timestamp
          try {
            removeAllTimeStampedItems(prodItemTimeStamp, pContext);
          } catch (RepositoryException re) {
            throw new ScenarioException(re);
          } catch (CommerceException ce) {
            throw new ScenarioException(ce);
          }
        } else
        
        // Scenario 3 - Timestamp and flgPromoProd indicate that we should remove the timestamp from the Qualifier
        if ((prodItemFlgPromoProd.booleanValue()) && (!prodItemFlgPromoQualifier.booleanValue()) && ((prodItemTimeStamp.length()) > 2)) { // We will remove the promo after we clear the timestamp from
                                                                                                                                          // the promo and qualifier
          int timestampCounter = 0;
          // first get a count of items with timestamp
          try {
            timestampCounter = countTimeStampedItem(prodItemTimeStamp, pContext);
          } catch (RepositoryException re) {
            throw new ScenarioException(re);
          } catch (CommerceException ce) {
            throw new ScenarioException(ce);
          }// end catch
          
          if (timestampCounter == 1) {
            try {
              clearTimeStampFromAllCommerceItems(prodItemTimeStamp, pContext);
            } catch (RepositoryException re) {
              throw new ScenarioException(re);
            } catch (CommerceException ce) {
              throw new ScenarioException(ce);
            }// end catch
          }// end counter == 1
           // clearTimeStamp()
        } else
        
        // We have encountered a strange combination of data
        if (mOrderManager.isLoggingDebug()) {
          mOrderManager.logDebug("This product  (" + prodItem.getRepositoryId() + ") has conflicting promotion attributes.");
          throw new ScenarioException(PromotionConstants.getStringResource(PromotionConstants.UNUSABLE_PARAMETER_OBJECT));
        } else {
          throw new ScenarioException(PromotionConstants.getStringResource(PromotionConstants.UNUSABLE_PARAMETER_OBJECT));
        }
        
        // end
      } // if prodItem != null
    } // if message of type ItemRemovedFromOrder
  }
  
  /**
   * This method returns the order from which the CommerceItems should be removed. You should really have the order carried along in the context, otherwise it will simply choose the first order in the
   * correct state and hope for the best.
   * 
   * @param pProfileId
   *          - the id of the profile whose orders we wish to retrieve
   **/
  public Order getOrderToUpdate(ScenarioExecutionContext pContext) throws CommerceException, RepositoryException {
    
    DynamoHttpServletRequest request = pContext.getRequest();
    
    // If we are executing this action in the context of a session then we want to get a hold
    // of the OrderHolder and use the Order within it as the Order that we want to get
    // information from.
    if (request != null) {
      OrderHolder oh = (OrderHolder) request.resolveName(mOrderHolderComponent);
      
      return oh.getCurrent();
    }
    
    // Get the profile from the context
    String profileId = ((MutableRepositoryItem) pContext.getProfile()).getRepositoryId();
    
    // Only change orders that are in the "incomplete" state
    List l = mOrderManager.getOrdersForProfileInState(profileId, StateDefinitions.ORDERSTATES.getStateValue(StateDefinitions.ORDERSTATES.INCOMPLETE));
    
    // Just modify the user's first order that's in the correct state
    if (l != null) {
      if (l.size() > 0) {
        return (Order) l.get(0);
      }
    }
    return null;
  }
  
  /**
   * This method will actually perform the action of removing an Sku from an order.
   * 
   * @param pSkuId
   *          the sku id of the commerce item that will be removed from the order
   * @param pQuantity
   *          the quantity of the particular item to remove
   * @param pContext
   *          the context in which the action is occuring
   * @exception CommerceException
   *              if an error occurs
   * @exception RepositoryException
   *              if an error occurs
   */
  protected void removeAllTimeStampedItems(String prodItemTimeStamp, ScenarioExecutionContext pContext) throws CommerceException, RepositoryException {
    // Figure out which commerce item to remove then remove it
    Order order = getOrderToUpdate(pContext);
    
    if (order != null) {
      // Get an iterator over each commerceItemRelationship
      List items = mOrderManager.getAllCommerceItemRelationships(order);
      Iterator iter = items.iterator();
      
      // System.out.println("Examining Order");
      // System.out.println("<<<Looking for this pSkuId: " + pSkuId);
      
      // Examine each commerceItem relationship
      while (iter.hasNext()) {
        CommerceItemRelationship ciRel = (CommerceItemRelationship) iter.next();
        
        // Remove all commerce items that match the prodPromoTimeStamp
        CommerceItem thisItem = ciRel.getCommerceItem();
        
        if (thisItem instanceof NMCommerceItem) {
          NMCommerceItem nmci = (NMCommerceItem) thisItem;
          if (prodItemTimeStamp.equals(nmci.getPromoTimeStamp())) {
            mOrderManager.removeItemFromOrder(order, nmci.getId());
          }
        }
      } // while (iter.hasNext())
    } // if (order != null)
  }
  
  /**
   * This method will return the number of items with the timestamp.
   * 
   * @param pSkuId
   *          the sku id of the commerce item that will be removed from the order
   * @param pQuantity
   *          the quantity of the particular item to remove
   * @param pContext
   *          the context in which the action is occuring
   * @exception CommerceException
   *              if an error occurs
   * @exception RepositoryException
   *              if an error occurs
   */
  protected int countTimeStampedItem(String prodItemTimeStamp, ScenarioExecutionContext pContext) throws CommerceException, RepositoryException {
    // Figure out which commerce item to remove then remove it
    Order order = getOrderToUpdate(pContext);
    
    int itemsCounter = 0;
    
    if (order != null) {
      // Get an iterator over each commerceItemRelationship
      List items = mOrderManager.getAllCommerceItemRelationships(order);
      Iterator iter = items.iterator();
      
      // System.out.println("Examining Order");
      // System.out.println("<<<Looking for this pSkuId: " + pSkuId);
      
      // Examine each commerceItem relationship
      while (iter.hasNext()) {
        CommerceItemRelationship ciRel = (CommerceItemRelationship) iter.next();
        
        // Remove all commerce items that match the prodPromoTimeStamp
        CommerceItem cItem = ciRel.getCommerceItem();
        
        if (cItem instanceof NMCommerceItem) {
          NMCommerceItem nmci = (NMCommerceItem) cItem;
          if (prodItemTimeStamp.equals(nmci.getPromoTimeStamp())) {
            itemsCounter = itemsCounter + 1;
            // mOrderManager.removeItemFromOrder(order, cItem.getId());
          }
        }
      } // while (iter.hasNext())
    } // if (order != null)
    return itemsCounter;
  }
  
  /**
   * This method will clear the promoTimeStamp from a CI (user has removed the promo item, we're looking for the item that was previously a qualifier.
   * 
   * @param pSkuId
   *          the sku id of the commerce item that will be removed from the order
   * @param pQuantity
   *          the quantity of the particular item to remove
   * @param pContext
   *          the context in which the action is occuring
   * @exception CommerceException
   *              if an error occurs
   * @exception RepositoryException
   *              if an error occurs
   */
  
  protected void clearTimeStampFromAllCommerceItems(String prodItemTimeStamp, ScenarioExecutionContext pContext) throws CommerceException, RepositoryException {
    // Figure out which commerce item to remove then remove it
    Order order = getOrderToUpdate(pContext);
    
    if (order != null) {
      // In the first pass, we
      // Get an iterator over each commerceItemRelationship
      List items = mOrderManager.getAllCommerceItemRelationships(order);
      Iterator iter = items.iterator();
      
      // System.out.println("Examining Order");
      // System.out.println("<<<Looking for this pSkuId: " + pSkuId);
      
      // Examine each commerceItem relationship
      while (iter.hasNext()) {
        CommerceItemRelationship ciRel = (CommerceItemRelationship) iter.next();
        
        // Clear the promoTimeStamp on all commerce items that match the prodPromoTimeStamp
        CommerceItem thisItem = ciRel.getCommerceItem();
        
        if (thisItem instanceof NMCommerceItem) {
          NMCommerceItem nmci = (NMCommerceItem) thisItem;
          if (prodItemTimeStamp.equals(nmci.getPromoTimeStamp())) {
            nmci.setPromoTimeStamp("");
          }
        }
      } // while (iter.hasNext())
      
    } // if (order != null)
    mOrderManager.updateOrder(order);
  }
  
} // end of class
