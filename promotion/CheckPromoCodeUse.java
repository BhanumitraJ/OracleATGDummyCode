package com.nm.commerce.promotion;

import java.util.List;
import java.util.Map;

import atg.commerce.CommerceException;
import atg.commerce.order.Order;
import atg.commerce.order.OrderHolder;
import atg.commerce.order.OrderManager;
import atg.commerce.promotion.PromotionConstants;
import atg.nucleus.Nucleus;
import atg.nucleus.naming.ComponentName;
import atg.repository.MutableRepositoryItem;
import atg.repository.RepositoryException;
import atg.scenario.ScenarioException;
import atg.scenario.ScenarioExecutionContext;
import atg.scenario.action.ActionImpl;
import atg.servlet.DynamoHttpServletRequest;

// This class is a Scenario Condition.
// It will simply:
// 1. Build a list of Customers PromoCodes
// 2. Check for a MaximumUse reached condition-
// from DB NM_USER_PROMOCODES & NM_PROMOCODE_LIST.NUM_TIMES & MAX_USE
// 3. return True/False
//
public class CheckPromoCodeUse extends ActionImpl {
  /** Parameter for if we need to check for a maximum limit used */
  public static final String CHECK_FOR_MAXIMUM_USE = "check_for_maximum_use";
  
  /** Parameter for maximum times to use from scenario file */
  public static final String MAXIMUM_USE_TIMES = "maximum_use_times";
  
  /** Parameter for the location of the ordermanager */
  public String ORDERMANAGER_PATH = "/atg/commerce/order/OrderManager";
  public static final String SHOPPING_CART_PATH = "/atg/commerce/ShoppingCart";
  
  /** reference to the order manager object and order holder object */
  protected OrderManager mOrderManager = null;
  protected ComponentName mOrderHolderComponent = null;
  
  protected Boolean mCheckForMaximumUse = null; // flag for whether we check
                                                // maximum used times
                                                // :true/false
  protected boolean mMaximumUseReached = false; // flag if maximum use has
                                                // been met :true/false
  protected Integer mMaxNumUse = null; // int set from scenario max number
                                       // of times to be used.
  
  public String RepositoryId = "";
  public String loginID = "";
  public String promocodeInOrder = "";// promocodeInOrder to be checked
                                      // against List of codes used by
                                      // customer.
  
  public void initialize(Map pParameters) throws ScenarioException {
    mOrderManager = (OrderManager) Nucleus.getGlobalNucleus().resolveName(ORDERMANAGER_PATH);
    
    if (mOrderManager == null) throw new ScenarioException(PromotionConstants.getStringResource(PromotionConstants.ORDER_MANAGER_NOT_FOUND));
    
    storeRequiredParameter(pParameters, CHECK_FOR_MAXIMUM_USE, java.lang.Boolean.class);// flag
                                                                                        // set
                                                                                        // in
                                                                                        // Scenario
                                                                                        // for
                                                                                        // use
                                                                                        // checking
    storeRequiredParameter(pParameters, MAXIMUM_USE_TIMES, java.lang.Integer.class);// int
                                                                                    // set
                                                                                    // from
                                                                                    // scenario
                                                                                    // max
                                                                                    // number
                                                                                    // of
                                                                                    // times
                                                                                    // to
                                                                                    // be
                                                                                    // used.
    
    mOrderHolderComponent = ComponentName.getComponentName(SHOPPING_CART_PATH);
  }
  
  protected void executeAction(ScenarioExecutionContext pContext) throws ScenarioException {
    if (mOrderManager == null) throw new ScenarioException(PromotionConstants.getStringResource(PromotionConstants.ORDER_MANAGER_NOT_FOUND));
    
    if (mCheckForMaximumUse == null) {
      synchronized (CHECK_FOR_MAXIMUM_USE) {
        mCheckForMaximumUse = (Boolean) getParameterValue(CHECK_FOR_MAXIMUM_USE, pContext);
        // System.out.println("*CPU**CPU*****"+ CHECK_FOR_MAXIMUM_USE +
        // " parameter is " + mCheckForMaximumUse);
      }
    }
    if (mCheckForMaximumUse == null) throw new ScenarioException("*CPU**CPU*CHECK_FOR_MAXIMUM_USE was not specified");
    
    if (mMaxNumUse == null) {
      synchronized (MAXIMUM_USE_TIMES) {
        mMaxNumUse = (Integer) getParameterValue(MAXIMUM_USE_TIMES, pContext);
        // System.out.println("*CPU**CPU*****"+ MAXIMUM_USE_TIMES + "
        // parameter is " + mMaxNumUse);
      }
    }
    if (mMaxNumUse == null) throw new ScenarioException("*CPU**CPU*MAXIMUM_USE_TIMES was not specified");
    
    // ***************************
    // if the flag mCheckForMaximumUse is true, then we
    // check for the number of times it has been used.
    if (mCheckForMaximumUse.booleanValue()) {
      // Read list of used codes for this customer.
      // return the boolean mMaximumUseReached if it is.
      // System.out.println("*CPU**CPU****In the
      // mCheckForMaximumUse.booleanValue() if statement***");
      // System.out.println("*CPU**CPU****mMaximumUseReached value
      // is:"+mMaximumUseReached);
      RepositoryId = ((MutableRepositoryItem) pContext.getProfile()).getRepositoryId();
      // System.out.println("*CPU**CPU****profileId is:"+RepositoryId);
      loginID = (String) ((MutableRepositoryItem) pContext.getProfile()).getPropertyValue("login");
      // System.out.println("*CPU**CPU****loginID is:"+loginID);
      
      // Get a list of Promocodes used by user.
      // System.out.println("*CPU*CPU*CPU****making repositories");
      MutableRepositoryItem userPCodeItem = ((MutableRepositoryItem) pContext.getProfile());
      // System.out.println("*CPU*CPU*CPU****making List");
      List usedPromos = (List) userPCodeItem.getPropertyValue("PromosUsed");
      // System.out.println("*CPU*CPU*CPU****List made");
      int count = usedPromos.size();
      
      // Get the Current Order's promoCode that has been entered by
      // customer.
      try {
        Order order = getOrderInfo(pContext);
        
        if (order != null) {
          if (order instanceof NMOrderImpl) {
            // System.out.println("*CPU*CPU*CPU***getting order
            // instance");
            NMOrderImpl orderImpl = (NMOrderImpl) order;
            promocodeInOrder = orderImpl.getPromoCode();
            // System.out.println("*CPU*CPU*CPU***Customer's order
            // Promo Code="+promocodeInOrder);
          }
        } else {
          throw new ScenarioException("*CPU*CPU*CPU*Order is null");
        }
      } catch (CommerceException ce) {
        throw new ScenarioException(ce);
      } catch (RepositoryException re) {
        throw new ScenarioException(re);
      }
      
      // 1. List of codes does not exist.
      if (usedPromos.isEmpty()) {
        // System.out.println("*CPU*CPU*CPU****usedPromos is empty");
      }
      
      // 2. List of codes exists.
      // Now we start to go through the list of codes already used.
      // The index(j) on the List will correspond with the codes used in
      // the repository
      for (int j = 0; j < count; j++) {
        // System.out.println("*CPU*CPU*CPU****items in list");
        // System.out.println("*CPU*CPU*CPU****"+j+usedPromos.get(j));
        // System.out.println("*CPU*CPU*CPU****"+((MutableRepositoryItem)usedPromos.get(j)).getPropertyValue("PROMOCODE")
        // );
        
        String pcode = (String) ((MutableRepositoryItem) usedPromos.get(j)).getPropertyValue("PROMOCODE");
        // System.out.println("*CPU*CPU*CPU****pcode is:"+pcode);
        // System.out.println("*CPU*CPU*CPU****promocodeInOrder
        // is:"+promocodeInOrder);
        
        // if one in List matches the one entered, we have a match.
        // Code has been found.
        // Index(j) of the List will correspond with the index of
        // Repository
        if (promocodeInOrder.indexOf(pcode) != -1) {
          MutableRepositoryItem itemFound = (MutableRepositoryItem) usedPromos.get(j);
          // System.out.println("*CPU*CPU*CPU****Code Found!");
          // ******************************
          // Where we will increment the use of code
          Integer nt = (Integer) itemFound.getPropertyValue("NUM_TIMES");
          int ntt = nt.intValue();
          int mnu = mMaxNumUse.intValue();
          // System.out.println("*CPU*CPU*CPU****ntt is:"+ntt);
          // System.out.println("*CPU*CPU*CPU****mnu is:"+mnu);
          
          if (ntt >= mnu) {
            mMaximumUseReached = true;
            // System.out.println("*CPU*CPU*CPU****ntt >= mnu
            // is:"+ntt+">="+mnu);
            throw new ScenarioException("profileId:" + RepositoryId + " PromoCode maximum use for this customer is reached! Used " + ntt + " times!");
            
          } else {
            // System.out.println("*CPU*CPU*CPU****ntt < mnu
            // is:"+ntt+"<"+mnu);
            // System.out.println("*CPU*CPU*CPU****Maximum use has
            // not been met");
          }
          
        } // end if
        
      }// end for
      
    }// end if mCheckForMaximumUse
    
  }// end of executeAction
  
  public Order getOrderInfo(ScenarioExecutionContext pContext) throws CommerceException, RepositoryException {
    
    DynamoHttpServletRequest request = pContext.getRequest();
    
    // If we are executing this action in the context of a session then we
    // want to get a hold
    // of the OrderHolder and use the Order within it as the Order that we
    // want to get
    // information from.
    if (request != null) {
      
      OrderHolder oh = (OrderHolder) request.resolveName(mOrderHolderComponent);
      System.out.println("*CPU*CPU*CPU****Getting Current Order***" + oh.getCurrent());
      // System.out.println("*RPC*RPC*RPC****OrderHolder***"+oh);
      // System.out.println("*RPC*RPC*RPC****Getting the Last
      // Order(current)***");
      // System.out.println("*RPC*RPC*RPC"+oh.getLast());
      
      // return oh.getLast();
      return oh.getCurrent();
    }
    
    return null;
  }
  
}// end of class
