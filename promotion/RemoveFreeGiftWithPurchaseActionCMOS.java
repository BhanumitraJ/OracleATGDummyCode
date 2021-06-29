package com.nm.commerce.promotion;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import atg.commerce.CommerceException;
import atg.commerce.order.CommerceItem;
import atg.commerce.order.CommerceItemRelationship;
import atg.commerce.order.ItemAddedToOrder;
import atg.commerce.order.ItemRemovedFromOrder;
import atg.commerce.order.Order;
import atg.commerce.order.OrderHolder;
import atg.commerce.order.OrderManager;
import atg.commerce.promotion.PromotionConstants;
import atg.commerce.states.StateDefinitions;
import atg.nucleus.Nucleus;
import atg.nucleus.logging.ApplicationLogging;
import atg.nucleus.logging.ClassLoggingFactory;
import atg.nucleus.naming.ComponentName;
import atg.repository.MutableRepositoryItem;
import atg.repository.Repository;
import atg.repository.RepositoryException;
import atg.repository.RepositoryItem;
import atg.repository.RepositoryView;
import atg.repository.rql.RqlStatement;
import atg.scenario.ScenarioException;
import atg.scenario.ScenarioExecutionContext;
import atg.scenario.action.ActionImpl;
import atg.servlet.DynamoHttpServletRequest;

import com.nm.commerce.NMCommerceItem;

/**
 * RemoveFreeGiftWithPurchaseActionCMOS
 * 
 * This custom action was developed to allow scenarios to be deployed across environments. It utilizes String variables passed from scenario and then creates a sku Repository Item which will be
 * removed from cart. All of the variables are required to be passed in if they are blank an exception will be generated.
 * 
 * @author Todd Schultz
 * @since 03/27/2002
 */

public class RemoveFreeGiftWithPurchaseActionCMOS extends ActionImpl {
  
  // -------------------------------------
  /** Class version string */
  public static final String CLASS_VERSION = "$Id: RemoveFreeGiftWithPurchaseActionCMOS.java 1.3 2008/08/14 13:25:30CDT nmrww3 Exp  $";
  
  /** Parameter for the location of the ordermanager */
  public String ORDERMANAGER_PATH = "/atg/commerce/order/OrderManager";
  
  /** Parameter for Free gift promotion type */
  public static final String PROMO_TYPE_PARAM = "promo_type";
  /** Parameter for promtion string */
  public static final String PROMO_STR = "Promotion";
  /** Parameter for cmos sku */
  public static final String CMOSSKU_PARAM = "cmosSKU";
  
  /** reference to the order manager object */
  protected OrderManager mOrderManager = null;
  
  protected ComponentName mOrderHolderComponent = null;
  
  private final ApplicationLogging mLogging = ClassLoggingFactory.getFactory().getLoggerForClass(RemoveFreeGiftWithPurchaseActionCMOS.class);
  
  public void initialize(Map pParameters) throws ScenarioException {
    /** Resolve OrderManager and Promotion components. */
    
    mOrderManager = (OrderManager) Nucleus.getGlobalNucleus().resolveName(ORDERMANAGER_PATH);
    
    if (mOrderManager == null) throw new ScenarioException(PromotionConstants.getStringResource(PromotionConstants.ORDER_MANAGER_NOT_FOUND));
    
    mOrderHolderComponent = ComponentName.getComponentName("/atg/commerce/ShoppingCart");
    storeOptionalParameter(pParameters, CMOSSKU_PARAM, java.lang.String.class);
    storeOptionalParameter(pParameters, PROMO_TYPE_PARAM, java.lang.String.class);
    
  }
  
  /*****************************************************************************************
   * This method is called from scenario, note no params are accepted but they are still required to be passed by scenario. Once validation is performed that all values needed are populated RQL
   * queries are perfored to create Repository Item
   * 
   * @param ScenarioExecutionContext
   *          pContext
   * @return void
   * @exception ScenarioException
   *****************************************************************************************/
  
  protected void executeAction(ScenarioExecutionContext pContext) throws ScenarioException {
    // System.out.println("#######################################Just hit RemoveFreeGiftWithPurchaseActionCMOS");
    if (mOrderManager == null) throw new ScenarioException(PromotionConstants.getStringResource(PromotionConstants.ORDER_MANAGER_NOT_FOUND));
    
    // Get the promo type from scenario parameter
    String cmosSku = (String) getParameterValue(CMOSSKU_PARAM, pContext);
    String promoType = (String) getParameterValue(PROMO_TYPE_PARAM, pContext);
    // System.out.println("***RFGWPACMOS***item to be removed"+cmosSku);
    
    // Prepare to remove
    Object message = pContext.getMessage();
    // System.out.println("########## RemoveFreeGiftWithPurchaseActionCMOS message-type[" + message.getClass().getName() + "] cmosSku[" + cmosSku + "]");
    boolean hasRemoveMessage = message instanceof ItemRemovedFromOrder;
    boolean hasAddMessage = message instanceof ItemAddedToOrder;
    boolean hasPromoMessage = message instanceof com.nm.scenario.PromoCodeMessage;
    long ciQuantity = 1L;
    if (hasRemoveMessage) {
      ItemRemovedFromOrder iOrder = (ItemRemovedFromOrder) pContext.getMessage();
      ciQuantity = iOrder.getQuantity();
    }
    
    // Check if it is a message of type ItemRemovedFromOrder
    if (hasRemoveMessage || hasAddMessage) {
      // RepositoryItem prodItem = null;
      RepositoryItem skuItem = null;
      
      // System.out.println("promoType " + promoType);
      // System.out.println("cmosSku " + cmosSku);
      
      try {
        Repository repository = (Repository) Nucleus.getGlobalNucleus().resolveName("/atg/commerce/catalog/ProductCatalog");
        RepositoryView m_view = repository.getView("sku");
        
        RqlStatement statement = RqlStatement.parseRqlStatement("cmosSKU = ?0");
        Object params[] = new Object[1];
        params[0] = cmosSku;
        RepositoryItem skuArray[] = statement.executeQuery(m_view, params);
        
        if (skuArray == null) {
          throw new NullPointerException("skuArray is null for [SELECT * FROM " + "NM_SKU_AUX" + " WHERE cmos_sku='" + params[0] + "']");
        } else {
          if (skuArray[0] != null)
            skuItem = (RepositoryItem) skuArray[0];
          else
            throw new NullPointerException("skuArray[0] is null");
        }
        
      } catch (Exception e) {
    	  if (mLogging.isLoggingError()) {
    		  mLogging.logError("exception caught getting Repository Items in RemoveFreeGiftWithPurchaseActionCMOS  ");
    	  }
      }
      
      if (skuItem != null) {
        String productID, skuID;
        
        skuID = skuItem.getRepositoryId();
        try {
          removeSku(skuID, ciQuantity, promoType, pContext);
        } catch (CommerceException ce) {
          throw new ScenarioException(ce);
        } catch (RepositoryException re) {
          throw new ScenarioException(re);
        }
        
      } // if RI not null
    } // is itemAddedToOrder
  } // end of executeAction method
  
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
  protected void removeSku(String pSkuId, long pQuantity, String pPromoType, ScenarioExecutionContext pContext) throws CommerceException, RepositoryException {
    // Figure out which commerce item to remove then remove it
    Order order = getOrderToRemoveSkuFrom(pContext);
    if (order != null) {
      // Get an iterator over each commerceItemRelationship
      List items = mOrderManager.getAllCommerceItemRelationships(order);
      Iterator iter = items.iterator();
      
      // System.out.println("Examining Order");
      // System.out.println("<<<Looking for this pSkuId: " + pSkuId);
      
      int count = 0;
      
      // Examine each commerceItem relationship
      
      while (iter.hasNext()) {
        CommerceItemRelationship ciRel = (CommerceItemRelationship) iter.next();
        
        // Remove all commerce items that have the correct SkuId
        
        CommerceItem thisItem = ciRel.getCommerceItem();
        
        // System.out.println("Examining commerceItem " + thisItem.getId() + " (catalogRefId = " + thisItem.getCatalogRefId() + ")");
        // System.out.println("## Examining commerceItem " + thisItem.getCatalogRefId() + " (catalogRefId = " + pSkuId + ")");
        
        if (thisItem.getCatalogRefId().equals(pSkuId)) {
          if (thisItem instanceof NMCommerceItem) {
            NMCommerceItem nmci = (NMCommerceItem) thisItem;
            String transientStatus = nmci.getTransientStatus();
            String promoType = nmci.getPromoType();
            
            // System.out.println(promoType);
            // System.out.println(" transientStatus: " + transientStatus + " needs to = " + " PROMO_STR: " + PROMO_STR );
            // System.out.println(" promoType: " + promoType + "  needs to = " + " pPromoType: " + pPromoType );
            if (transientStatus.equals(PROMO_STR) && promoType.equals(pPromoType)) {
              // This is the correct CommerceItem to remove. Use It.
              // System.out.println("***RFGWPACMOS***Match!  Removing item.");
              mOrderManager.removeItemFromOrder(order, nmci.getId());
              count++;
              
              // The count variable would determine the quantity of the free items that has to be removed
              
              if (count == pQuantity) break; // Need to have a break at here so that it won't proceed deleting.
            }
          }
        }
      } // while (iter.hasNext())
    } // if (order != null)
  }
  
} // end of class
