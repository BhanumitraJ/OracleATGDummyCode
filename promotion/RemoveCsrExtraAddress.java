package com.nm.commerce.promotion;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.transaction.TransactionManager;

import atg.commerce.CommerceException;
import atg.commerce.order.CommerceItemManager;
import atg.commerce.order.Order;
import atg.commerce.order.OrderHolder;
import atg.commerce.order.OrderManager;
import atg.commerce.order.OrderQueries;
import atg.commerce.promotion.PromotionConstants;
import atg.commerce.states.OrderStates;
import atg.commerce.states.StateDefinitions;
import atg.dtm.TransactionDemarcation;
import atg.dtm.TransactionDemarcationException;
import atg.nucleus.Nucleus;
import atg.nucleus.naming.ComponentName;
import atg.process.ProcessException;
import atg.process.ProcessExecutionContext;
import atg.process.action.ActionImpl;
import atg.repository.MutableRepositoryItem;
import atg.repository.Repository;
import atg.repository.RepositoryException;
import atg.servlet.DynamoHttpServletRequest;

import com.nm.collections.ExtraAddress;
import com.nm.collections.ExtraAddressArray;
import com.nm.collections.NMPromotion;
import com.nm.components.CommonComponentHelper;
import com.nm.utils.ExceptionUtil;
import com.nm.utils.PromotionsHelper;
import com.nm.utils.SystemSpecs;
import com.nm.utils.datasource.NMTransactionDemarcation;

public class RemoveCsrExtraAddress extends ActionImpl implements IPromoAction {
  
  // -------------------------------------
  /** Class version string */
  public static final String CLASS_VERSION = "$Id: RemoveCsrExtraAddress.java 1.10 2010/08/25 08:57:09CDT nmavj1 Exp  $";
  
  /** Parameter for the location of the ordermanager */
  public String ORDERMANAGER_PATH = "/atg/commerce/order/OrderManager";
  public static final String PROMOHELPER_PATH = "/nm/utils/PromotionsHelper";
  public static final String SHOPPING_CART_PATH = "/atg/commerce/ShoppingCart";
  public static final String EA_ARRAY_PATH = "/nm/collections/ExtraAddressArray";
  
  /** Parameter for Free gift promotion type */
  public static final String PROMO_TYPE_PARAM = "promo_type";
  /** Parameter for promtion string */
  public static final String PROMO_STR = "Promotion";
  /** Parameter for cmos sku */
  public static final String CMOSSKU_PARAM = "cmosSKU";
  
  PromotionsHelper mPromotionsHelper = null;
  
  /** reference to the order manager object */
  protected OrderManager mOrderManager = null;
  protected CommerceItemManager mItemManager = null;
  protected OrderQueries mOrderQueries = null;
  
  protected ComponentName mOrderHolderComponent = null;
  public ExtraAddressArray thePromos = null;
  public Repository repository = null;
  
  private boolean logDebug;
  private boolean logError = true;
  
  public void initialize(Map pParameters) throws ProcessException {
    /** Resolve OrderManager and Promotion components. */
    logDebug = getSystemSpecs().getCsrScenarioDebug();
    logDebug("RemoveCsrExtraAddress is Initializing");
    mOrderManager = (OrderManager) Nucleus.getGlobalNucleus().resolveName(ORDERMANAGER_PATH);
    
    if (mOrderManager == null) throw new ProcessException(PromotionConstants.getStringResource(PromotionConstants.ORDER_MANAGER_NOT_FOUND));
    
    mItemManager = mOrderManager.getCommerceItemManager();
    mOrderQueries = mOrderManager.getOrderQueries();
    
    // storeOptionalParameter(pParameters, CMOSSKU_PARAM,
    // java.lang.String.class);
    // storeOptionalParameter(pParameters, PROMO_TYPE_PARAM,
    // java.lang.String.class);
    mOrderHolderComponent = ComponentName.getComponentName(SHOPPING_CART_PATH);
    thePromos = (ExtraAddressArray) Nucleus.getGlobalNucleus().resolveName(EA_ARRAY_PATH);
    mPromotionsHelper = (PromotionsHelper) Nucleus.getGlobalNucleus().resolveName(PROMOHELPER_PATH);
    // System.out.println("#######################################RemoveCsrExtraAddress
    // initialized");
  }
  
  /***************************************************************************
   * This method is called from scenario, note no params are accepted but they are still required to be passed by scenario. Once validation is performed that all values needed are populated RQL
   * queries are perfored to create Repository Item
   * 
   * @param ProcessExecutionContext
   *          pContext
   * @return void
   * @exception ProcessException
   **************************************************************************/
  
  protected void executeAction(ProcessExecutionContext pContext) throws ProcessException {
    logDebug("just hit RemoveCsrExtraAddress");
    try {
      Order order = getOrderToModify(pContext);
      evaluatePromo(order);
    } catch (final Exception e) {
      throw new ProcessException(ExceptionUtil.getExceptionInfo(e), e);
    }
  }
  
  public void evaluatePromo(Order order) throws PromoException {
    TransactionDemarcation td = new NMTransactionDemarcation();
    boolean rollBack = false;
    
    try {
      TransactionManager tm = CommonComponentHelper.getTransactionManager();
      if (tm != null) {
        td.begin(tm, TransactionDemarcation.REQUIRED);
      }
    } catch (TransactionDemarcationException tde) {
      tde.printStackTrace();
      throw new PromoException(tde);
    }// end-try
    
    try {
      
      if (mOrderManager == null) throw new ProcessException(PromotionConstants.getStringResource(PromotionConstants.ORDER_MANAGER_NOT_FOUND));
      
      // Check if it is a message of type ItemRemovedFromOrder
      
      
      try {
        String theKey = null;
        Map<String, NMPromotion> promoArray = thePromos.getAllActivePromotions();
        Iterator<NMPromotion> promoIter = promoArray.values().iterator();
        while (promoIter.hasNext()) {
          ExtraAddress temp = (ExtraAddress) promoIter.next();
            if (temp.getType().trim().equals("1")) {
              theKey = temp.getCode().trim().toUpperCase();
              if (keyMatch(order, theKey) && (!mPromotionsHelper.dollarQualifies(order, temp.getDollarQualifier()))) {
                removeExtraAddress(temp, order);
              }
            }// is type1
            else if (temp.getType().trim().equals("2")) {
              theKey = temp.getCode().trim().toUpperCase();
              if (keyMatch(order, theKey) && (!mPromotionsHelper.dollarQualifies(order, temp.getDollarQualifier()))) {
                removeExtraAddress(temp, order);
              }
            }// is type2
            else if (temp.getType().trim().equals("3")) {
              theKey = temp.getCode().trim().toUpperCase();
              if (keyMatch(order, theKey) && (!mPromotionsHelper.orderHasBnglKey(order, theKey))) {
                removeExtraAddress(temp, order);
              }// end order $ qualifies
            }// is type3
        }// end for loop of promos
        
        mOrderManager.updateOrder(order);
      } catch (Exception ce) {
        ce.printStackTrace();
        throw new ProcessException(ce);
      }
      
    } catch (Exception e) {
      rollBack = true;
      logError("RemoveCsrExtraAddress.executeAction(): Exception ");
      throw new PromoException(e);
    } finally {
      try {
        td.end(rollBack); // commit work
      } catch (TransactionDemarcationException tde) {
        tde.printStackTrace();
        throw new PromoException(tde);
      }// end-try
    }// end-try
    
  } // end of executeAction method
  
  private boolean keyMatch(Order order, String theKey) throws Exception {
    boolean foundPromo = false;
    if (order != null) {
      if (order instanceof NMOrderImpl) {
        NMOrderImpl orderImpl = (NMOrderImpl) order;
        String orderPromoNames = orderImpl.getPromoName();
        if (orderPromoNames == null) {
          orderPromoNames = "";
        }
        StringTokenizer myToken = new StringTokenizer(orderPromoNames, ",");
        while (myToken.hasMoreTokens()) {
          String testString = myToken.nextToken();
          if (testString.trim().toUpperCase().equals(theKey.trim().toUpperCase())) {
            foundPromo = true;
            break;
          }
        }// end while
      } // end instance of NMOrderImpl
    }// end order !=null
    return foundPromo;
  }// end method promoCodeMatch
  
  /**
   * This method returns the order from which the CommerceItems should be removed. You should really have the order carried along in the context, otherwise it will simply choose the first order in the
   * correct state and hope for the best.
   * 
   * @param pProfileId
   *          - the id of the profile whose orders we wish to retrieve
   */
  public Order getOrderToRemoveSkuFrom(ProcessExecutionContext pContext) throws CommerceException, RepositoryException {
    
    DynamoHttpServletRequest request = pContext.getRequest();
    
    // If we are executing this action in the context of a session then we
    // want to get a hold
    // of the OrderHolder and use the Order within it as the Order that we
    // want to get
    // information from.
    if (request != null) {
      OrderHolder oh = (OrderHolder) request.resolveName(mOrderHolderComponent);
      return oh.getCurrent();
    }
    
    // Get the profile from the context
    String profileId = ((MutableRepositoryItem) pContext.getSubject()).getRepositoryId();
    
    // Only change orders that are in the "incomplete" state
    List l = mOrderQueries.getOrdersForProfileInState(profileId, StateDefinitions.ORDERSTATES.getStateValue(OrderStates.INCOMPLETE));
    
    // Just modify the user's first order that's in the correct state
    if (l != null) {
      if (l.size() > 0) {
        return (Order) l.get(0);
      }
    }
    return null;
  }
  
  public String getCurrentCatalogCode() {
    String catcode = getSystemSpecs().getCatalogCode();
    // System.out.println("***GettingCurrentCatCode***" + catcode);
    return catcode;
  }
  
  private SystemSpecs getSystemSpecs() {
    return (SystemSpecs) Nucleus.getGlobalNucleus().resolveName("/nm/utils/SystemSpecs");
  }
  
  /**
   * This method returns the order which should be modified.
   * 
   * It would be better to grab the order in the context, but as it is that's not going to happen given the way this action is typically used. That could be fixed, given time.
   * 
   * @param pContext
   *          - the scenario context this is being evaluated in
   */
  public Order getOrderToModify(ProcessExecutionContext pContext) throws CommerceException, RepositoryException {
    DynamoHttpServletRequest request = pContext.getRequest();
    // If we are executing this action in the context of a session then we
    // want to get a hold
    // of the OrderHolder and use the Order within it as the Order that we
    // want to get
    // information from.
    if (request != null) {
      OrderHolder oh = (OrderHolder) request.resolveName(mOrderHolderComponent);
      return oh.getCurrent();
    }
    
    // Get the profile from the context
    String profileId = ((MutableRepositoryItem) pContext.getSubject()).getRepositoryId();
    // Only change orders that are in the "incomplete" state
    List l = mOrderQueries.getOrdersForProfileInState(profileId, StateDefinitions.ORDERSTATES.getStateValue(OrderStates.INCOMPLETE));
    // Just modify the user's first order that's in the correct state
    if (l != null) {
      if (l.size() > 0) {
        return (Order) l.get(0);
      }
    }
    return null;
  }// end getOrderToModify method
  
  private void removeExtraAddress(ExtraAddress thePromotion, Order order) throws Exception {
    // order = getOrderToAddItemTo(pContext);
    String theCode = new String(thePromotion.getPromoCodes().trim().toUpperCase());
    String theKey = new String(thePromotion.getCode().trim().toUpperCase());
    
    try {
      if (mPromotionsHelper.keyMatch(order, theKey)) {
        if (order instanceof NMOrderImpl) {
          // order = getOrderToAddItemTo(pContext);
          NMOrderImpl orderImpl = (NMOrderImpl) order;
          orderImpl.setRemoveActivatedPromoCode(theCode);
          orderImpl.setRemoveUserActivatedPromoCode(theCode);
          orderImpl.setRemovePromoName(theKey);
          mPromotionsHelper.removeKEYToCI(theKey, orderImpl);
          logDebug("extraaddress HAS BEEN REMOVED  theCode--->" + theCode);
          logDebug("extraaddress HAS BEEN REMOVED  theKey--->" + theKey);
          mOrderManager.updateOrder(order);
        }
        
      }// end promoAlreadyAwarded
    } catch (CommerceException ce) {
      throw new ProcessException(ce);
    } catch (RepositoryException re) {
      throw new ProcessException(re);
    }
    
  }// endremoveExtraAddress
  
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
      Nucleus.getGlobalNucleus().logError("RemoveCsrExtraAddress: " + sin);
    }
  }
  
  private void logDebug(String sin) {
    if (isLoggingDebug()) {
      Nucleus.getGlobalNucleus().logDebug("RemoveCsrExtraAddress: " + sin);
    }
  }
  
} // end of class
