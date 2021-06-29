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

import atg.commerce.promotion.*;
import atg.commerce.order.*;
import atg.commerce.CommerceException;
import atg.commerce.states.StateDefinitions;
import atg.commerce.order.CommerceItemRelationship;
import atg.nucleus.Nucleus;
import atg.nucleus.logging.ApplicationLogging;
import atg.nucleus.logging.ClassLoggingFactory;
import atg.nucleus.naming.ComponentName;
import atg.servlet.DynamoHttpServletRequest;
import atg.naming.NameResolver;
import atg.repository.*;
import atg.scenario.*;
import atg.scenario.action.*;

import java.util.Map;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import javax.servlet.ServletException;

import java.io.IOException;

import com.nm.commerce.*;

/**
 * This action is responsible for deleting all CommerceItems that match a specific SkuId. Very similar in structure to ATG's out-of-the-box AddItemToOrder class.
 * 
 * @author Ert Dredge
 * @modified Chee-Chien Loo
 * @version $Id: RemoveSkuFromOrder.java 1.2 2008/04/29 13:58:30CDT nmmc5 Exp $
 */

public class RemoveSkuFromOrder extends ActionImpl {
  // -------------------------------------
  /** Class version string */
  public static final String CLASS_VERSION = "$Id: RemoveSkuFromOrder.java 1.2 2008/04/29 13:58:30CDT nmmc5 Exp  $";
  
  /** Parameter for the location of the ordermanager */
  public String ORDERMANAGER_PATH = "/atg/commerce/order/OrderManager";
  
  /** Parameter for product id information - unused */
  public static final String PRODUCT_PARAM = "product_id";
  
  /** Parameter for sku id information */
  public static final String SKU_PARAM = "sku_id";
  
  /** Parameter for quantity information - unused */
  public static final String QUANTITY_PARAM = "quantity";
  
  /** reference to the order manager object */
  protected OrderManager mOrderManager = null;
  
  /** The shopping cart component */
  public static final String SHOPPING_CART_PATH = "/atg/commerce/ShoppingCart";
  
  protected String mSkuId = null;
  protected long mQuantity;
  
  protected ComponentName mOrderHolderComponent = null;
  
  private final ApplicationLogging mLogging = ClassLoggingFactory.getFactory().getLoggerForClass(RemoveSkuFromOrder.class);
  
  // -------------------------------------
  /**
   * 
   * removeSkuFromOrder
   */
  
  public void initialize(Map pParameters) throws ScenarioException {
    mOrderManager = (OrderManager) Nucleus.getGlobalNucleus().resolveName(ORDERMANAGER_PATH);
    
    // <TBD> make sure that all strings are put in resource files.
    if (mOrderManager == null) throw new ScenarioException(PromotionConstants.getStringResource(PromotionConstants.ORDER_MANAGER_NOT_FOUND));
    
    storeRequiredParameter(pParameters, SKU_PARAM, java.lang.String.class);
    // storeOptionalParameter(pParameters, PRODUCT_PARAM, java.lang.String.class);
    // storeOptionalParameter(pParameters, QUANTITY_PARAM, java.lang.Long.class);
    
    mOrderHolderComponent = ComponentName.getComponentName(SHOPPING_CART_PATH);
  }
  
  protected void executeAction(ScenarioExecutionContext pContext) throws ScenarioException {
    
    if (mOrderManager == null) throw new ScenarioException(PromotionConstants.getStringResource(PromotionConstants.ORDER_MANAGER_NOT_FOUND));
    
    // If the sku id is null then we should see if it has been passed in. In theory this
    // should happen once and only once. At the same time we will take the opportunity to load
    // up the other properties
    
    if (mSkuId == null) {
      synchronized (SKU_PARAM) {
        mSkuId = (String) getParameterValue(SKU_PARAM, pContext);
        if (mLogging.isLoggingDebug()) {
        	mLogging.logDebug(SKU_PARAM + " parameter is " + mSkuId);
        }
        /*
         * mQuantity = ((Long) getParameterValue(QUANTITY_PARAM, pContext)).longValue(); if (mQuantity == 0) mQuantity = 1;
         */
        
      }
      
    }
    
    if (mSkuId == null) throw new ScenarioException(PromotionConstants.getStringResource(PromotionConstants.SKU_DOES_NOT_EXIST));
    
    try {
      removeSku(mSkuId, mQuantity, pContext);
    } catch (CommerceException ce) {
      throw new ScenarioException(ce);
    } catch (RepositoryException re) {
      throw new ScenarioException(re);
    }
    
  }
  
  /**
   * This method returns the order from which the CommerceItems should be removed. You should really have the order carried along in the context, otherwise it will simply choose the first order in the
   * correct state and hope for the best.
   * 
   * @param pProfileId
   *          - the id of the profile whose orders we wish to retrieve
   **/
  public Order getOrderToRemoveSkuFrom(ScenarioExecutionContext pContext) throws CommerceException, RepositoryException {
    
    DynamoHttpServletRequest request = pContext.getRequest();
    
    // If we are executing this action in the context of a session then we want to get a hold
    // of the OrderHolder and use the Order within it as the Order that we want to get
    // information from.
    if (request != null) {
      OrderHolder oh = (OrderHolder) request.resolveName(mOrderHolderComponent);
      return oh.getCurrent();
    }
    
    // Get the profile from the context
    String profileId = ((RepositoryItem) pContext.getProfile()).getRepositoryId();
    
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
  protected void removeSku(String pSkuId, long pQuantity, ScenarioExecutionContext pContext) throws CommerceException, RepositoryException {
    // Figure out which commerce item to remove then remove it
    Order order = getOrderToRemoveSkuFrom(pContext);
    if (order != null) {
      // Get an iterator over each commerceItemRelationship
      List items = mOrderManager.getAllCommerceItemRelationships(order);
      Iterator iter = items.iterator();
      
      // System.out.println("Examining Order");
      // System.out.println("<<<Looking for this pSkuId"+pSkuId);
      // System.out.println("<<<Looking for this mSkuId"+mSkuId);
      
      // Examine each commerceItem relationship
      while (iter.hasNext()) {
        CommerceItemRelationship ciRel = (CommerceItemRelationship) iter.next();
        
        // Remove all commerce items that have the correct SkuId
        CommerceItem thisItem = ciRel.getCommerceItem();
        
        // System.out.println("Examining commerceItem " + thisItem.getId() + " (catalogRefId = " + thisItem.getCatalogRefId() + ")");
        // "Examining commerceItem ci408000005 (catalogRefId = csku265209)"
        if (thisItem.getCatalogRefId().equals(mSkuId)) {
          if (thisItem instanceof NMCommerceItem) {
            NMCommerceItem nmci = (NMCommerceItem) thisItem;
            String transientStatus = nmci.getTransientStatus();
            if (transientStatus.equals("promotion")) {
              // This is the correct CommerceItem to remove. Use It.
              // System.out.println("Match!  Removing item.");
              mOrderManager.removeItemFromOrder(order, nmci.getId());
              break; // Need to have a break at here so that it won't proceed deleting.
            }
          }
        }
      } // while (iter.hasNext())
    } // if (order != null)
  }
  
} // end of class

