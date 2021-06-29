/*
 * Created on Mar 5, 2004
 * 
 * To change the template for this generated file go to Window>Preferences>Java>Code Generation>Code and Comments
 */
package com.nm.commerce.promotion;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import atg.commerce.CommerceException;
import atg.commerce.order.CommerceItemRelationshipContainer;
import atg.commerce.order.Order;
import atg.commerce.order.OrderHolder;
import atg.commerce.order.OrderManager;
import atg.commerce.order.OrderQueries;
import atg.commerce.order.Relationship;
import atg.commerce.order.ShippingGroupCommerceItemRelationship;
import atg.commerce.promotion.PromotionConstants;
import atg.commerce.states.OrderStates;
import atg.commerce.states.StateDefinitions;
import atg.nucleus.Nucleus;
import atg.nucleus.naming.ComponentName;
import atg.process.ProcessException;
import atg.process.ProcessExecutionContext;
import atg.process.action.ActionImpl;
import atg.repository.RepositoryException;
import atg.scenario.ScenarioException;
import atg.servlet.DynamoHttpServletRequest;

/**
 * @author nmmpe
 * 
 *         To change the template for this generated type comment go to Window>Preferences>Java>Code Generation>Code and Comments
 */
public class IdenticalShippingAdresses extends ActionImpl {
  
  public static final String ORDERQUERIES_PATH = "/atg/commerce/order/OrderQueries";
  public static final String SHOPPING_CART_PATH = "/atg/commerce/ShoppingCart";
  public static final String ORDERMANAGER_PATH = "/atg/commerce/order/OrderManager";
  public static final String NUMBER_OF_IDENTICALS = "number_of_identicals";// set in scenario
  public static final String PROMO_CODE = "promo_code";// The flag 'FREEBASESHIPPING' code
  public static final String ADD_PROMO_FLAG = "add_promo_flag";// true/false flag in scenario
  public static final String PROMO_NAME = "promo_name";
  public static final String FILTER_ZERO_DOLLAR_ITEMS = "filter_zero_dollar";// true/false flag in scenario
  
  protected OrderQueries mOrderQuery = null;
  protected OrderManager mOrderManager = null;
  protected ComponentName mOrderHolderComponent = null;
  protected CommerceItemRelationshipContainer mCommerceItemRelationshipContainer = null;
  
  protected int orderState = 0;
  protected Boolean mAddPromoFlag = null;// true/false flag in scenario
  protected boolean accumCountflg = false;
  protected String mPromoCode = null;
  protected String mPromoName = null;
  protected int numberOfIdenticals = 0;
  protected Long itemCounter;
  protected int aCounter = 0;
  protected int foundaCounter = 0;
  protected int accumulatedAddressCount = 0;
  protected Boolean mFilterZeroDollarItems = null;
  
  public void initialize(Map pParameters) throws ProcessException {
    mOrderManager = (OrderManager) Nucleus.getGlobalNucleus().resolveName(ORDERMANAGER_PATH);
    mOrderQuery = (OrderQueries) Nucleus.getGlobalNucleus().resolveName(ORDERQUERIES_PATH);
    mOrderHolderComponent = ComponentName.getComponentName(SHOPPING_CART_PATH);
    
    storeRequiredParameter(pParameters, NUMBER_OF_IDENTICALS, java.lang.String.class);
    storeRequiredParameter(pParameters, PROMO_CODE, java.lang.String.class);// The CMOS required 'FREEBASESHIPPING' code
    storeRequiredParameter(pParameters, ADD_PROMO_FLAG, java.lang.Boolean.class);// true/false flag in scenario
    storeRequiredParameter(pParameters, PROMO_NAME, java.lang.String.class);
    storeRequiredParameter(pParameters, FILTER_ZERO_DOLLAR_ITEMS, java.lang.Boolean.class);// true/false flag in scenario
    
  }
  
  protected void executeAction(ProcessExecutionContext pContext) throws ProcessException {
    if (mOrderManager == null) throw new ProcessException(PromotionConstants.getStringResource(PromotionConstants.ORDER_MANAGER_NOT_FOUND));
    
    if (mAddPromoFlag == null) {
      mAddPromoFlag = (Boolean) getParameterValue(ADD_PROMO_FLAG, pContext);
    }
    if (mAddPromoFlag == null) throw new ScenarioException("*ISA***Add Promo Flag was not specified");
    if (mPromoCode == null) {
      mPromoCode = (String) getParameterValue(PROMO_CODE, pContext);
    }
    if (mPromoCode == null) throw new ScenarioException("*ISA***Promo Code was not specified");
    if (mPromoName == null) {
      mPromoName = (String) getParameterValue(PROMO_NAME, pContext);
    }
    if (mPromoName == null) {
      throw new ScenarioException("*ISA***Promo Name was not specified");
    }
    if (numberOfIdenticals == 0) {
      numberOfIdenticals = Integer.parseInt((String) getParameterValue(NUMBER_OF_IDENTICALS, pContext));
    }
    if (numberOfIdenticals == 0) {
      throw new ScenarioException("*ISA***NumberOfIdenticals was not specified");
    }
    if (mFilterZeroDollarItems == null) {
      mFilterZeroDollarItems = (Boolean) getParameterValue(FILTER_ZERO_DOLLAR_ITEMS, pContext);
    }
    if (mFilterZeroDollarItems == null) throw new ScenarioException("*ISA***Filter Dollar Items was not specified");
    
    try {
      modifyOrder(pContext);
    } catch (CommerceException ce) {
      throw new ScenarioException(ce);
    } catch (RepositoryException re) {
      throw new ScenarioException(re);
    }
    
  }
  
  protected void modifyOrder(ProcessExecutionContext pContext) throws ProcessException, CommerceException, RepositoryException {
    
    boolean addToList = false;
    
    try {
      Order order = getOrderToModify(pContext);
      if (order != null) {
        if (order instanceof NMOrderImpl) {
          NMOrderImpl orderImpl = (NMOrderImpl) order;
          // *************************************************
          // Where we need to check the order for addresses
          List rShipList = orderImpl.getRelationships();
          HashMap mAddressCount = new HashMap();
          ShippingGroupCommerceItemRelationship sgRelationship = new ShippingGroupCommerceItemRelationship();
          
          Iterator rShipIterator = rShipList.iterator();
          aCounter = 0;
          
          while (rShipIterator.hasNext()) {
            
            Relationship tempRel = (Relationship) rShipIterator.next();
            
            if (tempRel instanceof ShippingGroupCommerceItemRelationship) {
              
              sgRelationship = (ShippingGroupCommerceItemRelationship) tempRel;
              
              if (mFilterZeroDollarItems.booleanValue()) {
                double ciPrice = sgRelationship.getCommerceItem().getPriceInfo().getListPrice();
                if (ciPrice != 0.0) {
                  addToList = true;
                }
              } else
                // not filtering out zero items
                addToList = true;
              
              if (addToList) {
                addToList = false;
                
                if (sgRelationship.getId() != null) {
                  
                  if (!mAddressCount.containsKey(sgRelationship.getShippingGroup().getDescription())) {
                    itemCounter = new Long(sgRelationship.getQuantity());
                    accumulatedAddressCount = itemCounter.intValue();
                    mAddressCount.put(sgRelationship.getShippingGroup().getDescription(), new Integer(accumulatedAddressCount));
                    
                    if (accumulatedAddressCount >= numberOfIdenticals) {
                      accumCountflg = true;
                    }
                  } else {
                    foundaCounter = ((Integer) mAddressCount.get(sgRelationship.getShippingGroup().getDescription())).intValue();
                    itemCounter = new Long(sgRelationship.getQuantity());
                    aCounter = itemCounter.intValue();
                    accumulatedAddressCount = aCounter + foundaCounter;
                    mAddressCount.put(sgRelationship.getShippingGroup().getDescription(), new Integer(accumulatedAddressCount));
                    if (accumulatedAddressCount >= numberOfIdenticals) {
                      accumCountflg = true;
                    }
                  }
                }
              }
            }
            
          } // end whilerShipIterator.hasNext()
          
          if (mAddPromoFlag.booleanValue() && accumCountflg) {
            orderImpl.setActivatedPromoCode(mPromoCode);
            orderImpl.setPromoName(mPromoName);
            mOrderManager.updateOrder(orderImpl);
            accumCountflg = false;
          } else if (mAddPromoFlag.booleanValue() && !accumCountflg) {
            orderImpl.setRemoveActivatedPromoCode(mPromoCode);
            orderImpl.setPromoName("");
            mOrderManager.updateOrder(orderImpl);
          } else if (!mAddPromoFlag.booleanValue() && !accumCountflg) {
            orderImpl.setRemoveActivatedPromoCode(mPromoCode);
            orderImpl.setPromoName("");
            mOrderManager.updateOrder(orderImpl);
            
          }
          
        }
      } else {
        throw new ScenarioException("**ISA***Order to be modified is null");
      }
    } catch (CommerceException ce) {
      throw new ScenarioException(ce);
    }
    
  }
  
  public Order getOrderToModify(ProcessExecutionContext pContext) throws CommerceException {
    DynamoHttpServletRequest request = pContext.getRequest();
    
    if (request != null) {
      OrderHolder oh = (OrderHolder) request.resolveName(mOrderHolderComponent);
      return oh.getCurrent();
    }
    
    String profileId = pContext.getSubject().getRepositoryId();
    orderState = StateDefinitions.ORDERSTATES.getStateValue(OrderStates.INCOMPLETE);
    List incompleteOrder = mOrderQuery.getOrdersForProfileInState(profileId, orderState);
    // Just modify the user's first order that's in the correct state
    if (incompleteOrder != null) {
      if (incompleteOrder.size() > 0) {
        return (Order) incompleteOrder.get(0);
      }
    }
    
    return null;
  }
  
}
