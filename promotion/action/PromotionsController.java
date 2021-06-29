package com.nm.commerce.promotion.action;

import java.sql.Date;
import java.util.List;
import java.util.Map;

import atg.commerce.CommerceException;
import atg.commerce.order.InvalidVersionException;
import atg.commerce.order.Order;
import atg.commerce.order.OrderHolder;
import atg.commerce.order.OrderManager;
import atg.commerce.promotion.PromotionConstants;
import atg.commerce.states.StateDefinitions;
import atg.nucleus.Nucleus;
import atg.nucleus.naming.ComponentName;
import atg.process.ProcessException;
import atg.process.ProcessExecutionContext;
import atg.process.action.Action;
import atg.process.action.ActionImpl;
import atg.repository.MutableRepositoryItem;
import atg.repository.RepositoryException;
import atg.servlet.DynamoHttpServletRequest;

import com.nm.commerce.promotion.NMOrderImpl;

/**
 * This is the master controller for all promotion scenario events.
 */

public class PromotionsController extends atg.process.action.ActionImpl {
  
  public String ORDERMANAGER_PATH = "/atg/commerce/order/OrderManager";
  public String PRICINGTOOLS_PATH = "/atg/commerce/pricing/PricingTools";
  public static final String SHOPPING_CART_PATH = "/atg/commerce/ShoppingCart";
  public static final String ACTION_CONFIGURATION = "/nm/commerce/promotion/action/PromoActionConfiguration";
  
  protected OrderManager mOrderManager = null;
  protected ComponentName mOrderHolderComponent = null;
  
  ActionConfiguration actionConfiguration = null;
  
  public void initialize(Map pParameters) throws ProcessException {
    
    mOrderManager = (OrderManager) Nucleus.getGlobalNucleus().resolveName(ORDERMANAGER_PATH);
    
    if (mOrderManager == null) throw new ProcessException(PromotionConstants.getStringResource(PromotionConstants.ORDER_MANAGER_NOT_FOUND));
    
    mOrderHolderComponent = ComponentName.getComponentName(SHOPPING_CART_PATH);
    actionConfiguration = (ActionConfiguration) Nucleus.getGlobalNucleus().resolveName(ACTION_CONFIGURATION);
    
    if (actionConfiguration != null && actionConfiguration.isLoggingDebug()) {
      actionConfiguration.logDebug("PromotionsController is Initializing");
    }
  }
  
  protected void executeAction(ProcessExecutionContext pContext) throws ProcessException {
    
    if (actionConfiguration != null && actionConfiguration.isLoggingDebug()) {
      actionConfiguration.logDebug("------------------------- PROMOTIONS CONTROLLER RECEIVING EVENT ------------------------");
    }
    
    if (!actionConfiguration.isScenarioDrivenPromotions()) {
      if (actionConfiguration != null && actionConfiguration.isLoggingDebug()) actionConfiguration.logDebug("Scenario driven promotions are disabled");
      
      return;
    }
    
    synchronized (pContext.getSubject()) {
      
      if (mOrderManager == null) {
        throw new ProcessException(PromotionConstants.getStringResource(PromotionConstants.ORDER_MANAGER_NOT_FOUND));
      }
      
      try {
        Order order = getOrderToModify(pContext);
        
        synchronized (order) {
          
          if (actionConfiguration != null) {
            
            Action[] actions = actionConfiguration.getActionsForEvent(pContext);
            
            for (int i = 0; i < actions.length; i++) {
              ActionImpl actionImpl = (ActionImpl) actions[i];
              if (actionConfiguration.isLoggingDebug()) {
                actionConfiguration.logDebug("event: " + pContext.getMessageType() + ": > " + actionImpl.getActionName());
              }
              actionImpl.execute(pContext);
            }
          }
          
          // need to revisit this to implement wait until scenario messages are complete
          // ((NMOrderImpl)order).decOutstandingEvents();
        }
        
      } catch (InvalidVersionException ive) {
        ive.printStackTrace();
      } catch (Exception e) {
        Nucleus.getGlobalNucleus().logError("PromotionsController.executeAction(): Exception " + e);
        throw new ProcessException(e);
      }
    }
  }
  
  /**
   * This method returns the order which should be modified.
   * 
   * It would be better to grab the order in the context, but as it is that's not going to happen given the way this action is typically used. That could be fixed, given time.
   * 
   * @param pContext
   *          - the scenario context this is being evaluated in
   **/
  public Order getOrderToModify(ProcessExecutionContext pContext) throws CommerceException, RepositoryException {
    
    DynamoHttpServletRequest request = pContext.getRequest();
    
    // If we are executing this action in the context of a session then we want to get a hold
    // of the OrderHolder and use the Order within it as the Order that we want to get
    // information from.
    if (request != null) {
      OrderHolder oh = (OrderHolder) request.resolveName(mOrderHolderComponent);
      return oh.getCurrent();
    }
    
    // Get the profile from the context
    String profileId = ((MutableRepositoryItem) pContext.getSubject()).getRepositoryId();
    
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
}
