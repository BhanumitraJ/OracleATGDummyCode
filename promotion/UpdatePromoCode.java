package com.nm.commerce.promotion;

import atg.commerce.promotion.*;
import atg.commerce.order.*;
import atg.commerce.CommerceException;
import atg.commerce.states.StateDefinitions;
import atg.nucleus.Nucleus;
import atg.nucleus.naming.ComponentName;
import atg.servlet.DynamoHttpServletRequest;
import atg.repository.*;
import atg.scenario.*;
import atg.scenario.action.*;
import java.util.Map;
import java.util.List;
import com.nm.utils.SystemSpecs;

/**
 * This action is used to update activated promo code in order object
 * 
 * @author Todd Schultz
 * @version $Id: UpdatePromoCode.java 1.2 2008/04/29 13:59:30CDT nmmc5 Exp $
 */

public class UpdatePromoCode extends ActionImpl {
  // -------------------------------------
  /** Class version string */
  public static final String CLASS_VERSION = "$Id: UpdatePromoCode.java 1.2 2008/04/29 13:59:30CDT nmmc5 Exp  $";
  
  /** Parameter for the location of the ordermanager */
  public String ORDERMANAGER_PATH = "/atg/commerce/order/OrderManager";
  
  /** Parameter for which value to update */
  public static final String PROMO_CODE = "promo_code";// The CMOS required 'FREEBASESHIPPING' code
  public static final String PROMO_NAME = "promo_name";
  
  /** Parameter for whether to add or remove promo code */
  public static final String ADD_PROMO_FLAG = "add_promo_flag";// true/false flag in scenario
  
  /** The shopping cart component */
  public static final String SHOPPING_CART_PATH = "/atg/commerce/ShoppingCart";
  
  /** reference to the order manager object and order holder object */
  protected OrderManager mOrderManager = null;
  protected ComponentName mOrderHolderComponent = null;
  protected Boolean mAddPromoFlag = null;// true/false flag in scenario
  protected String mPromoCode = null;
  protected String mPromoName = null;
  
  /** Debug */
  
  private boolean logDebug;
  private boolean logError = true;
  
  public void initialize(Map pParameters) throws ScenarioException {
    
    mOrderManager = (OrderManager) Nucleus.getGlobalNucleus().resolveName(ORDERMANAGER_PATH);
    logDebug = getSystemSpecs().getCsrScenarioDebug();
    logDebug("updatePromoCode is Initializing");
    
    // <TBD> make sure that all strings are put in resource files.
    if (mOrderManager == null) throw new ScenarioException(PromotionConstants.getStringResource(PromotionConstants.ORDER_MANAGER_NOT_FOUND));
    logDebug("promo code:" + PROMO_CODE + "; promo name:" + PROMO_NAME + "; ADD FLAG: " + ADD_PROMO_FLAG);
    
    storeRequiredParameter(pParameters, PROMO_CODE, java.lang.String.class);// The CMOS required 'FREEBASESHIPPING' code
    storeRequiredParameter(pParameters, PROMO_NAME, java.lang.String.class);
    storeRequiredParameter(pParameters, ADD_PROMO_FLAG, java.lang.Boolean.class);// true/false flag in scenario
    
    mOrderHolderComponent = ComponentName.getComponentName(SHOPPING_CART_PATH);
  }
  
  protected void executeAction(ScenarioExecutionContext pContext) throws ScenarioException {
    
    if (mOrderManager == null) throw new ScenarioException(PromotionConstants.getStringResource(PromotionConstants.ORDER_MANAGER_NOT_FOUND));
    logDebug("in execute action");
    
    if (mAddPromoFlag == null) {
      synchronized (ADD_PROMO_FLAG) {// true/false flag in scenario
        mAddPromoFlag = (Boolean) getParameterValue(ADD_PROMO_FLAG, pContext);// true/false flag in scenario
        // System.out.println("*UPC**UPC*ADD_PROMO_FLAG "+ADD_PROMO_FLAG + " parameter is " + mAddPromoFlag);
      } // synchronized (ADD_PROMO_FLAG)
    } // mAddPromoFlag == null
    
    if (mAddPromoFlag == null) throw new ScenarioException("*UPC**UPC*Add Promo Flag was not specified");
    
    if (mPromoCode == null) {// The CMOS required 'FREEBASESHIPPING' code
      synchronized (PROMO_CODE) {
        mPromoCode = (String) getParameterValue(PROMO_CODE, pContext);// The CMOS required 'FREEBASESHIPPING' code
        // System.out.println("*UPC**UPC*PROMO_CODE "+PROMO_CODE + " parameter is " + mPromoCode);
      } // synchronized (PROMO_CODE)
    } // mPromoCode == null
    
    if (mPromoCode == null) throw new ScenarioException("*UPC**UPC*Promo Code was not specified");
    
    if (mPromoName == null) {
      synchronized (PROMO_NAME) {
        mPromoName = (String) getParameterValue(PROMO_NAME, pContext);
        // System.out.println("*UPC**UPC*PROMO_CODE "+PROMO_CODE + " parameter is " + mPromoCode);
      } // synchronized (PROMO_NAME)
    } // mPromoName == null
    
    if (mPromoName == null) {
      throw new ScenarioException("Promo Name was not specified");
    }
    
    try {
      Order order = getOrderToModify(pContext);
      
      if (order != null) {
        if (order instanceof NMOrderImpl) {
          NMOrderImpl orderImpl = (NMOrderImpl) order;
          if (mAddPromoFlag.booleanValue()) {
            logDebug(" just added promo code-->" + mPromoCode);
            orderImpl.setPromoCode(mPromoCode);
            orderImpl.setPromoName(mPromoName);
          } else {
            orderImpl.setPromoName("");
          }
          mOrderManager.updateOrder(order);
        }
      } else {
        throw new ScenarioException("*UPC**UPC*Order to be modified is null");
      }
    } catch (CommerceException ce) {
      throw new ScenarioException(ce);
    } catch (RepositoryException re) {
      throw new ScenarioException(re);
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
  public Order getOrderToModify(ScenarioExecutionContext pContext) throws CommerceException, RepositoryException {
    
    DynamoHttpServletRequest request = pContext.getRequest();
    // System.out.println("**pContext.getRequest() Inside getOrderToModify**");
    // If we are executing this action in the context of a session then we want to get a hold
    // of the OrderHolder and use the Order within it as the Order that we want to get
    // information from.
    if (request != null) {
      OrderHolder oh = (OrderHolder) request.resolveName(mOrderHolderComponent);
      // System.out.println("*UPC**UPC*about to getCurrent ORder");
      // System.out.println("*UPC**UPC*"+oh.getCurrent());
      // System.out.println("*UPC**UPC*about to getLAST ORder");
      // System.out.println("*UPC**UPC*"+oh.getLast());
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
  
  public void setLoggingDebug(boolean bin) {
    logDebug = bin;
  }
  
  public boolean isLoggingDebug() {
    return logDebug;
  }
  
  public void setLoggingError(boolean bin) {
    logError = bin;
  }
  
  public boolean isLoggingError() {
    return logError;
  }
  
  private void logError(String sin) {
    if (isLoggingError()) {
      Nucleus.getGlobalNucleus().logError("updatePromoCode: " + sin);
    }
  }
  
  private void logDebug(String sin) {
    if (isLoggingDebug()) {
      Nucleus.getGlobalNucleus().logDebug("updatePromoCode: " + sin);
    }
  }
  
  private SystemSpecs getSystemSpecs() {
    return (SystemSpecs) Nucleus.getGlobalNucleus().resolveName("/nm/utils/SystemSpecs");
  }
}
