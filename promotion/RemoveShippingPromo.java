package com.nm.commerce.promotion;

import static com.nm.common.INMGenericConstants.CHECKOUT;
import static com.nm.common.INMGenericConstants.COLON_STRING;
import static com.nm.integration.VmeConstants.VME;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.transaction.TransactionManager;

import atg.commerce.CommerceException;
import atg.commerce.order.CommerceItemManager;
import atg.commerce.order.Order;
import atg.commerce.order.OrderHolder;
import atg.commerce.order.OrderQueries;
import atg.commerce.promotion.PromotionConstants;
import atg.commerce.states.OrderStates;
import atg.commerce.states.StateDefinitions;
import atg.dtm.TransactionDemarcation;
import atg.dtm.TransactionDemarcationException;
import atg.nucleus.Nucleus;
import atg.nucleus.naming.ComponentName;
import atg.process.ProcessExecutionContext;
import atg.process.action.ActionImpl;
import atg.repository.MutableRepositoryItem;
import atg.repository.RepositoryException;
import atg.repository.RepositoryItem;
import atg.scenario.ScenarioException;
import atg.servlet.DynamoHttpServletRequest;
import atg.servlet.ServletUtil;

import com.nm.collections.NMPromotion;
import com.nm.collections.Promotion;
import com.nm.collections.PromotionsArray;
import com.nm.commerce.checkout.CheckoutComponents;
import com.nm.commerce.order.NMOrderManager;
import com.nm.components.CommonComponentHelper;
import com.nm.formhandler.ShoppingCartHandler;
import com.nm.utils.ExceptionUtil;
import com.nm.utils.PromotionsHelper;
import com.nm.utils.StringUtilities;
import com.nm.utils.SystemSpecs;

/**
 * @author Todd Schultz This action is used to update activated promo code in order object if the user removes an item from the cart. It is part of the CSR tool that allows marketing to create their
 *         own shipping promotions. a generic SDL file will fire messages to this action when a remove from cart happens and this action will determine if the freeshipping has been given and if so has
 *         been disqualified by the removal of an item.
 * 
 */

public class RemoveShippingPromo extends ActionImpl implements IPromoAction {
  // -------------------------------------
  /** Class version string */
  public static final String CLASS_VERSION = "$Id: RemoveShippingPromo.java 1.40 2013/09/11 17:23:34CDT Michael Davis (vsmd4) Exp  $";
  
  public String ORDERMANAGER_PATH = "/atg/commerce/order/OrderManager";
  public static String SYSTEMSPECS_PATH = "/nm/utils/SystemSpecs";
  public static String PROMOHELPER_PATH = "/nm/utils/PromotionsHelper";
  public static String PROMOARRAY_PATH = "/nm/collections/PromotionsArray";
  public static final String SHOPPING_CART_PATH = "/atg/commerce/ShoppingCart";
  public static final String FREEBASESHIPPING = "FREEBASESHIPPING";
  public static final String CMOSUPGRADE = "CMOSUPGRADE";
  public static final String CMOSFBS = "WEBFRSH";
  public static final String WEBSL1 = "UPGRADEOVERNIGHT";
  public static final String WEBSL2 = "UPGRADE2NDDAY";
  public static final String SHOPRUNNER = "SHOPRUNNER";
  public static final String PROMO_CODE = "promo_code";// The CMOS required
                                                       // 'FREEBASESHIPPING'
                                                       // code
  
  /** reference to the order manager object and order holder object */
  protected NMOrderManager mOrderManager = null;
  protected CommerceItemManager mItemManager = null;
  protected OrderQueries mOrderQueries = null;
  
  protected ComponentName mOrderHolderComponent = null;
  protected Boolean mAddPromoFlag = null;// true/false flag in scenario
  protected String mPromoCode = null;
  
  SystemSpecs mSystemSpecs = null;
  
  private boolean logDebug;
  private boolean logError = true;
  
  PromotionsArray thePromos = null;
  PromotionsHelper mPromotionsHelper = null;
  
  @Override
  public void initialize(final Map pParameters) throws ScenarioException {
    
    mOrderManager = (NMOrderManager) Nucleus.getGlobalNucleus().resolveName(ORDERMANAGER_PATH);
    
    if (mOrderManager == null) {
      throw new ScenarioException(PromotionConstants.getStringResource(PromotionConstants.ORDER_MANAGER_NOT_FOUND));
    }
    
    mItemManager = mOrderManager.getCommerceItemManager();
    mOrderQueries = mOrderManager.getOrderQueries();
    
    // storeRequiredParameter(pParameters, PROMO_CODE,
    // java.lang.String.class);//The CMOS required 'FREEBASESHIPPING' code
    
    mOrderHolderComponent = ComponentName.getComponentName(SHOPPING_CART_PATH);
    thePromos = (PromotionsArray) Nucleus.getGlobalNucleus().resolveName(PROMOARRAY_PATH);
    mPromotionsHelper = (PromotionsHelper) Nucleus.getGlobalNucleus().resolveName(PROMOHELPER_PATH);
    mSystemSpecs = (SystemSpecs) Nucleus.getGlobalNucleus().resolveName(SYSTEMSPECS_PATH);
    logDebug = mSystemSpecs.getCsrScenarioDebug();
    logDebug("RemoveShippingPromos is Initializing");
  }
  
  /*
   * This is the method that is executed when an item is removed from cart. It gets a reference to the global component that manages all the promotions that marketing has created in the repository.
   * The first thing it checks is if free shipping has been given. If it has not been given then nothing else will happen becasue there is nothing to take away. If free shipping has been given then it
   * will get the system date and iterate thru the promotions and see if there is an effective one. NOTE... an existing limitation is that once a promotion is not effective meaning the end date is hit
   * then any user that has this promotion will not disqualify. This is becasue the promotion no longer is effective so when the code is iterating thru the promotions the first thing it looks for is
   * it active, if its not active then it will not continue. Once a promotion is found that is active then it will determine what type it is. Type 1 is dollar qualifier, Type 2 is dollar qualifier and
   * promo code, Type 6 is dept code and Type 10 is depiction qual. Once it knows what type it is then it will check to see if the promo has been disqulaified by the removal of an item from cart.
   */
  
  @Override
  protected void executeAction(final ProcessExecutionContext pContext) throws ScenarioException {
    logDebug("just hit RemoveShippingPromo");
    try {
      final Order order = getOrderToModify(pContext);
      evaluatePromo(order);
    } catch (final Exception e) {
      throw new ScenarioException(ExceptionUtil.getExceptionInfo(e), e);
    }
  }
  
  @Override
  public void evaluatePromo(final Order order) throws PromoException {
    final TransactionDemarcation td = new TransactionDemarcation();
    boolean rollBack = false;
    
    try {
      final TransactionManager tm = CommonComponentHelper.getTransactionManager();
      if (tm != null) {
        td.begin(tm, TransactionDemarcation.REQUIRED);
      }
    } catch (final TransactionDemarcationException tde) {
      tde.printStackTrace();
      throw new PromoException(tde);
    }// end-try
    try {
      String activatedPromoCodes = null;
      if (mOrderManager == null) {
        throw new PromoException(PromotionConstants.getStringResource(PromotionConstants.ORDER_MANAGER_NOT_FOUND));
      }
      if (order instanceof NMOrderImpl) {
        final NMOrderImpl NMorderImpl = (NMOrderImpl) order;
        activatedPromoCodes = NMorderImpl.getActivatedPromoCode();
      }// end nmorderimpl check
      
      try {
        checkPromotions(order, activatedPromoCodes);
        mOrderManager.updateOrder(order);
      } catch (final Exception cleare) {
        cleare.printStackTrace();
        throw new ScenarioException(cleare);
      }
    } catch (final Exception e) {
      rollBack = true;
      logError("CheckShippingPromos.executeAction(): Exception ");
      throw new PromoException(e);
    } finally {
      try {
        td.end(rollBack); // commit work
      } catch (final TransactionDemarcationException tde) {
        tde.printStackTrace();
        throw new PromoException(tde);
      }// end-try
    }// end-try
  }// end executeAction
  
  /**
   * This method returns the order which should be modified.
   * 
   * It would be better to grab the order in the context, but as it is that's not going to happen given the way this action is typically used. That could be fixed, given time.
   * 
   * @param pContext
   *          - the scenario context this is being evaluated in
   */
  public Order getOrderToModify(final ProcessExecutionContext pContext) throws CommerceException, RepositoryException {
    
    final DynamoHttpServletRequest request = pContext.getRequest();
    // If we are executing this action in the context of a session then we
    // want to get a hold
    // of the OrderHolder and use the Order within it as the Order that we
    // want to get
    // information from.
    if (request != null) {
      final OrderHolder oh = (OrderHolder) request.resolveName(mOrderHolderComponent);
      return oh.getCurrent();
    }
    
    // Get the profile from the context
    final String profileId = ((MutableRepositoryItem) pContext.getSubject()).getRepositoryId();
    
    // Only change orders that are in the "incomplete" state
    final List l = mOrderQueries.getOrdersForProfileInState(profileId, StateDefinitions.ORDERSTATES.getStateValue(OrderStates.INCOMPLETE));
    
    // Just modify the user's first order that's in the correct state
    if (l != null) {
      if (l.size() > 0) {
        return (Order) l.get(0);
      }
    }
    return null;
  }
  
  // private SystemSpecs getSystemSpecs()
  // {
  // return
  // (SystemSpecs)Nucleus.getGlobalNucleus().resolveName("/nm/utils/SystemSpecs");
  // }
  public void setLoggingDebug(final boolean bin) {
    logDebug = bin;
  }
  
  public boolean isLoggingDebug() {
    return logDebug;
  }
  
  public void setLoggingError(final boolean bin) {
    logError = bin;
  }
  
  public boolean isLoggingError() {
    return logError;
  }
  
  private void logError(final String sin) {
    if (isLoggingError()) {
      Nucleus.getGlobalNucleus().logError("removeShippingPromoCode: " + sin);
    }
  }
  
  private void logDebug(final String sin) {
    if (isLoggingDebug()) {
      Nucleus.getGlobalNucleus().logDebug("removeShippingPromo: " + sin);
    }
  }
  
  private void removeShipping(final String shippingOffer, final String theKey, final String promoCodeOrKey, final String theName, final Order order, final boolean sendToCmos) throws Exception {
    logDebug("RemoveShippingPromos:removeShipping: for " + shippingOffer + " using promocode or key " + promoCodeOrKey);
    try {
      if (order instanceof NMOrderImpl) {
        NMOrderImpl orderImpl = (NMOrderImpl) order;
        
        orderImpl.setRemoveActivatedPromoCode(shippingOffer);
        orderImpl.setRemoveActivatedPromoCode(promoCodeOrKey);
        orderImpl.setRemovePromoName(theKey);
        mPromotionsHelper.removeParentheticalIndicator(false, orderImpl);
        if (!theKey.equals(promoCodeOrKey)) {
          orderImpl.setRemoveUserActivatedPromoCode(promoCodeOrKey);
        }
        
        if (sendToCmos) {
          mPromotionsHelper.removePromoToCI(promoCodeOrKey, orderImpl, theKey);
          // have to roll the promos to the line item level since
          // thats how cmos wants it
        } else {
          mPromotionsHelper.removeKEYToCI(theKey, orderImpl);
        }
        // cmos wants us to send a FS identifier on the LI so they can
        // tell if FS was awarded
        if (shippingOffer.trim().toUpperCase().equals(FREEBASESHIPPING)) {
          
          mPromotionsHelper.removePromoToCI(CMOSFBS, orderImpl, theKey);
        }
        logDebug("removeShipping-the order version-------------------->" + orderImpl.getVersion());
        mOrderManager.updateOrder(order);
        orderImpl = (NMOrderImpl) order;
        
        logDebug("removeShipping--the order version-after updateOrder------------------->" + orderImpl.getVersion());
      }
      
    } catch (final CommerceException ce) {
      throw new ScenarioException(ce);
    } catch (final RepositoryException re) {
      throw new ScenarioException(re);
    } catch (final Exception e) {
      throw new ScenarioException(e);
    }
    
  }// end removeShipping
  
  private boolean keyMatch(final Order order, final String theKey) throws Exception {
    boolean foundPromo = false;
    if (order != null) {
      if (order instanceof NMOrderImpl) {
        final NMOrderImpl orderImpl = (NMOrderImpl) order;
        String orderPromoNames = orderImpl.getPromoName();
        if (orderPromoNames == null) {
          orderPromoNames = "";
        }
        final StringTokenizer myToken = new StringTokenizer(orderPromoNames, ",");
        while (myToken.hasMoreTokens()) {
          final String testString = myToken.nextToken();
          if (testString.trim().toUpperCase().equals(theKey.trim().toUpperCase())) {
            foundPromo = true;
            break;
          }
        }// end while
      } // end instance of NMOrderImpl
    }// end order !=null
    return foundPromo;
  }// end method keyMatch
  
  private void checkPromotions(final Order order, final String activatedPromoCodes) {
    try {
      // these are the variable declarations that will be initialized
      // below for the different types
      String theCode = "";
      String theKey = "";
      String theName = "";
      String theCatalogs = "";
      String theItems = "";
      String theDept = "";
      String theDepiction = "";
      double promoDollarQual = 0;
      Promotion theShipPromo = null;
      NMOrderImpl orderImpl = null;
      String lvl = null;
      final boolean validatePromoLimitUse = ((NMOrderImpl) order).shouldValidateLimitPromoUse();
      // DynamoHttpServletRequest req = null;
      // VisaCheckout - setting isVisaCheckout variable as true if order payment group is visa checkout
      final boolean isVisaCheckout = ((NMOrderImpl) order).isVisaCheckoutPayGroupOnOrder();
      final Map<String, NMPromotion> promoArray = thePromos.getAllActivePromotions();
      Iterator<NMPromotion> promoIter = promoArray.values().iterator();
      while (promoIter.hasNext()) {
        theShipPromo = (Promotion) promoIter.next();
          theCode = theShipPromo.getPromoCodes().trim().toUpperCase();
          theKey = theShipPromo.getCode().trim().toUpperCase();
          theName = theShipPromo.getName().trim().toUpperCase();
          theDept = theShipPromo.getDeptCodes().trim().toUpperCase();
          final String key = theShipPromo.getKey();
          final String keyType = theShipPromo.getKeyType();
          /* removing VisaCheckout and other Payment Type specific promotions if Order contains VisaCheckout MasterPass etc as a payment group */
          final boolean checkout = !StringUtilities.isEmpty(key) && !StringUtilities.isEmpty(keyType) && keyType.equalsIgnoreCase(CHECKOUT);
          if (checkout) {
            final boolean isVisaCheckoutPromoSkipped = key.equalsIgnoreCase(VME) && !isVisaCheckout;
            final String selectedCheckoutType = key + COLON_STRING + keyType;
            final String orderCheckoutType = ((NMOrderImpl) order).getCheckoutType();
            if (isVisaCheckoutPromoSkipped || (!isVisaCheckout && !selectedCheckoutType.equalsIgnoreCase(orderCheckoutType))) {
              if (((NMOrderImpl) order).getActivatedPromoCode().contains("FREEBASESHIPPING")) {
                removeShipping(FREEBASESHIPPING, theKey, theCode, theName, order, false);
              } else if (((NMOrderImpl) order).getActivatedPromoCode().contains("CMOSUPGRADE")) {
                removeShipping(CMOSUPGRADE, theKey, theCode, theName, order, true);
              }
              continue;
            }
          }
          if (theShipPromo.getType().trim().equals("1")) {
            if (keyMatch(order, theKey) && (!mPromotionsHelper.dollarQualifies(order, theShipPromo.getDollarQualifier().trim(), 0) || !mPromotionsHelper.countryQualifies(order, theKey))) {
              removeShipping(FREEBASESHIPPING, theKey, theKey, theName, order, false);
            }
          }// end type 1
          else if (theShipPromo.getType().trim().equals("2")) {
            if (keyMatch(order, theKey) && (!mPromotionsHelper.dollarQualifies(order, theShipPromo.getDollarQualifier().trim(), 0) || !mPromotionsHelper.countryQualifies(order, theKey))) {
              removeShipping(FREEBASESHIPPING, theKey, theCode, theName, order, false);
            }
          }// end type 2
          else if (theShipPromo.getType().trim().equals("5")) {
            if (keyMatch(order, theKey) && (!mPromotionsHelper.dollarQualifies(order, theShipPromo.getDollarQualifier().trim(), 0) || !mPromotionsHelper.countryQualifies(order, theKey))) {
              removeShipping(CMOSUPGRADE, theKey, theCode, theName, order, true);
            }
          }// end type 5
          else if (theShipPromo.getType().trim().equals("6")) {
            theDept = theShipPromo.getDeptCodes().trim().toUpperCase();
            if (keyMatch(order, theKey) && (!mPromotionsHelper.deptMatch(order, theDept, 0))) {
              removeShipping(FREEBASESHIPPING, theKey, theKey, theName, order, false);
            }
          }// end type 6
          else if (theShipPromo.getType().trim().equals("7")) {
            if (keyMatch(order, theKey) && (!mPromotionsHelper.deptMatch(order, theDept, 0))) {
              removeShipping(FREEBASESHIPPING, theKey, theCode, theName, order, false);
            }
            
          }// end type 7
          else if (theShipPromo.getType().trim().equals("8")) {
            promoDollarQual = Double.parseDouble(theShipPromo.getDollarQualifier());
            
            if (keyMatch(order, theKey) && (!mPromotionsHelper.deptDollarMatch(order, theDept, promoDollarQual, 0))) {
              removeShipping(FREEBASESHIPPING, theKey, theKey, theName, order, false);
            }
          }// end type 8
          
          else if (theShipPromo.getType().trim().equals("9")) {
            promoDollarQual = Double.parseDouble(theShipPromo.getDollarQualifier());
            
            if (keyMatch(order, theKey) && (!mPromotionsHelper.deptDollarMatch(order, theDept, promoDollarQual, 0))) {
              removeShipping(FREEBASESHIPPING, theKey, theCode, theName, order, false);
            }
          }// end type 9
          else if (theShipPromo.getType().trim().equals("10")) {
            theDepiction = theShipPromo.getDepiction();
            
            if (keyMatch(order, theKey) && (!mPromotionsHelper.depictionMatch(order, theDepiction, 0))) {
              removeShipping(FREEBASESHIPPING, theKey, theKey, theName, order, false);
            }
          }// end type 10
          else if (theShipPromo.getType().trim().equals("11")) {
            theDepiction = theShipPromo.getDepiction().trim().toUpperCase();
            if (keyMatch(order, theKey) && (!mPromotionsHelper.depictionMatch(order, theDepiction, 0))) {
              removeShipping(FREEBASESHIPPING, theKey, theCode, theName, order, false);
            }
            
          }// end type 11
          else if (theShipPromo.getType().trim().equals("12")) {
            
            theDepiction = theShipPromo.getDepiction();
            promoDollarQual = Double.parseDouble(theShipPromo.getDollarQualifier());
            
            if (keyMatch(order, theKey) && (!mPromotionsHelper.depictionDollarMatch(order, theDepiction, promoDollarQual, 0))) {
              removeShipping(FREEBASESHIPPING, theKey, theKey, theName, order, false);
            }
          }// end type 12
          else if (theShipPromo.getType().trim().equals("13")) {
            theDepiction = theShipPromo.getDepiction().trim().toUpperCase();
            promoDollarQual = Double.parseDouble(theShipPromo.getDollarQualifier());
            
            if (keyMatch(order, theKey) && (!mPromotionsHelper.depictionDollarMatch(order, theDepiction, promoDollarQual, 0))) {
              removeShipping(FREEBASESHIPPING, theKey, theCode, theName, order, false);
            }
          }// end type 13
          else if (theShipPromo.getType().trim().equals("14")) {
            promoDollarQual = Double.parseDouble(theShipPromo.getDollarQualifier());
            if (keyMatch(order, theKey) && (!mPromotionsHelper.saleDollarMatch(order, promoDollarQual, true, 0))) {
              removeShipping(FREEBASESHIPPING, theKey, theCode, theName, order, false);
            }
          }// end type 14
          else if (theShipPromo.getType().trim().equals("15")) {
            promoDollarQual = Double.parseDouble(theShipPromo.getDollarQualifier());
            if (keyMatch(order, theKey) && (!mPromotionsHelper.saleDollarMatch(order, promoDollarQual, false, 0))) {
              removeShipping(FREEBASESHIPPING, theKey, theCode, theName, order, false);
            }
          }// end type 15
          else if (theShipPromo.getType().trim().equals("16")) {
            if (keyMatch(order, theKey) && (!mPromotionsHelper.saleMatch(order, true, 0))) {
              removeShipping(FREEBASESHIPPING, theKey, theCode, theName, order, false);
            }
          }// end type 16
          else if (theShipPromo.getType().trim().equals("17")) {
            if (keyMatch(order, theKey) && (!mPromotionsHelper.saleMatch(order, false, 0))) {
              removeShipping(FREEBASESHIPPING, theKey, theCode, theName, order, false);
            }
          }// end type 17
          else if (theShipPromo.getType().trim().equals("18")) {
            theCatalogs = theShipPromo.getQualifyCatalog().trim().toUpperCase();
            promoDollarQual = Double.parseDouble(theShipPromo.getDollarQualifier());
            if (keyMatch(order, theKey) && (!mPromotionsHelper.catalogDollarMatch(order, theCatalogs, promoDollarQual, 0))) {
              removeShipping(FREEBASESHIPPING, theKey, theCode, theName, order, false);
            }
          }// end type 18
          else if (theShipPromo.getType().trim().equals("19")) {
            theCatalogs = theShipPromo.getQualifyCatalog().trim().toUpperCase();
            if (keyMatch(order, theKey) && (!mPromotionsHelper.catalogMatch(order, theCatalogs, 0))) {
              removeShipping(FREEBASESHIPPING, theKey, theCode, theName, order, false);
            }
          }// end type 19
          else if (theShipPromo.getType().trim().equals("20")) {
            theCatalogs = theShipPromo.getQualifyCatalog().trim().toUpperCase();
            promoDollarQual = Double.parseDouble(theShipPromo.getDollarQualifier());
            if (keyMatch(order, theKey) && (!mPromotionsHelper.catalogDollarMatch(order, theCatalogs, promoDollarQual, 0))) {
              removeShipping(FREEBASESHIPPING, theKey, theKey, theName, order, false);
            }
          }// end type 20
          else if (theShipPromo.getType().trim().equals("21")) {
            theItems = theShipPromo.getQualifyItems().trim().toUpperCase();
            if (keyMatch(order, theKey) && (!mPromotionsHelper.itemMatch(order, theItems))) {
              removeShipping(FREEBASESHIPPING, theKey, theKey, theName, order, false);
            }
          }// end type 21
          else if (theShipPromo.getType().trim().equals("22")) {
            theItems = theShipPromo.getQualifyItems().trim().toUpperCase();
            if (keyMatch(order, theKey) && (!mPromotionsHelper.itemMatch(order, theItems))) {
              removeShipping(FREEBASESHIPPING, theKey, theCode, theName, order, false);
            }
          }// end type 22
          else if (theShipPromo.getType().trim().equals("23")) {
            theItems = theShipPromo.getQualifyItems().trim().toUpperCase();
            promoDollarQual = Double.parseDouble(theShipPromo.getDollarQualifier());
            if (keyMatch(order, theKey) && (!mPromotionsHelper.itemDollarMatch(order, theItems, promoDollarQual))) {
              removeShipping(FREEBASESHIPPING, theKey, theCode, theName, order, false);
            }
          }// end type 23
          else if (theShipPromo.getType().trim().equals("24")) {
            theItems = theShipPromo.getQualifyItems().trim().toUpperCase();
            promoDollarQual = Double.parseDouble(theShipPromo.getDollarQualifier());
            if (keyMatch(order, theKey) && (!mPromotionsHelper.itemDollarMatch(order, theItems, promoDollarQual))) {
              removeShipping(FREEBASESHIPPING, theKey, theKey, theName, order, false);
            }
          }// end type 24
          else if (theShipPromo.getType().trim().equals("25")) {
            if (keyMatch(order, theKey) && (!mPromotionsHelper.deptMatch(order, theDept, 0))) {
              removeShipping(CMOSUPGRADE, theKey, theCode, theName, order, true);
            }
            
          }// end type 25
          else if (theShipPromo.getType().trim().equals("26")) {
            promoDollarQual = Double.parseDouble(theShipPromo.getDollarQualifier());
            
            if (keyMatch(order, theKey) && (!mPromotionsHelper.deptDollarMatch(order, theDept, promoDollarQual, 0))) {
              removeShipping(CMOSUPGRADE, theKey, theCode, theName, order, true);
            }
          }// end type 26
          else if (theShipPromo.getType().trim().equals("27")) {
            theDepiction = theShipPromo.getDepiction().trim().toUpperCase();
            if (keyMatch(order, theKey) && (!mPromotionsHelper.depictionMatch(order, theDepiction, 0))) {
              removeShipping(CMOSUPGRADE, theKey, theCode, theName, order, true);
            }
            
          }// end type 27
          else if (theShipPromo.getType().trim().equals("28")) {
            theDepiction = theShipPromo.getDepiction().trim().toUpperCase();
            promoDollarQual = Double.parseDouble(theShipPromo.getDollarQualifier());
            
            if (keyMatch(order, theKey) && (!mPromotionsHelper.depictionDollarMatch(order, theDepiction, promoDollarQual, 0))) {
              removeShipping(CMOSUPGRADE, theKey, theCode, theName, order, true);
            }
          }// end type 28
          else if (theShipPromo.getType().trim().equals("29")) {
            if (keyMatch(order, theKey) && (!mPromotionsHelper.saleMatch(order, true, 0))) {
              removeShipping(CMOSUPGRADE, theKey, theCode, theName, order, true);
            }
          }// end type 29
          else if (theShipPromo.getType().trim().equals("30")) {
            if (keyMatch(order, theKey) && (!mPromotionsHelper.saleMatch(order, false, 0))) {
              removeShipping(CMOSUPGRADE, theKey, theCode, theName, order, true);
            }
          }// end type 30
          else if (theShipPromo.getType().trim().equals("31")) {
            promoDollarQual = Double.parseDouble(theShipPromo.getDollarQualifier());
            if (keyMatch(order, theKey) && (!mPromotionsHelper.saleDollarMatch(order, promoDollarQual, true, 0))) {
              removeShipping(CMOSUPGRADE, theKey, theCode, theName, order, true);
            }
          }// end type 31
          else if (theShipPromo.getType().trim().equals("32")) {
            promoDollarQual = Double.parseDouble(theShipPromo.getDollarQualifier());
            if (keyMatch(order, theKey) && (!mPromotionsHelper.saleDollarMatch(order, promoDollarQual, false, 0))) {
              removeShipping(CMOSUPGRADE, theKey, theCode, theName, order, true);
            }
          }// end type 32
          else if (theShipPromo.getType().trim().equals("33")) {
            theCatalogs = theShipPromo.getQualifyCatalog().trim().toUpperCase();
            if (keyMatch(order, theKey) && (!mPromotionsHelper.catalogMatch(order, theCatalogs, 0))) {
              removeShipping(CMOSUPGRADE, theKey, theCode, theName, order, true);
            }
          }// end type 33
          else if (theShipPromo.getType().trim().equals("34")) {
            theCatalogs = theShipPromo.getQualifyCatalog().trim().toUpperCase();
            promoDollarQual = Double.parseDouble(theShipPromo.getDollarQualifier());
            if (keyMatch(order, theKey) && (!mPromotionsHelper.catalogDollarMatch(order, theCatalogs, promoDollarQual, 0))) {
              removeShipping(CMOSUPGRADE, theKey, theCode, theName, order, true);
            }
          }// end type 34
          else if (theShipPromo.getType().trim().equals("35")) {
            final Integer promoItemCount = new Integer(theShipPromo.getItemCount());
            if (keyMatch(order, theKey) && (!mPromotionsHelper.itemCountMatch(order, promoItemCount.intValue()))) {
              removeShipping(FREEBASESHIPPING, theKey, theCode, theName, order, false);
            }
          }// end type 35
          else if (theShipPromo.getType().trim().equals("36")) {
            theDept = theShipPromo.getDeptCodes().trim().toUpperCase();
            final Integer promoItemCount = new Integer(theShipPromo.getItemCount());
            if (keyMatch(order, theKey) && (!mPromotionsHelper.deptMatch(order, theDept, promoItemCount.intValue()))) {
              removeShipping(FREEBASESHIPPING, theKey, theCode, theName, order, false);
            }
          }// end type 36
          else if (theShipPromo.getType().trim().equals("37")) {
            theDept = theShipPromo.getDeptCodes().trim().toUpperCase();
            final Integer promoItemCount = new Integer(theShipPromo.getItemCount());
            promoDollarQual = Double.parseDouble(theShipPromo.getDollarQualifier());
            if (keyMatch(order, theKey) && (!mPromotionsHelper.deptDollarMatch(order, theDept, promoDollarQual, promoItemCount.intValue()))) {
              removeShipping(FREEBASESHIPPING, theKey, theCode, theName, order, false);
            }
          }// end type 37
          else if (theShipPromo.getType().trim().equals("38")) {
            theDepiction = theShipPromo.getDepiction();
            final Integer promoItemCount = new Integer(theShipPromo.getItemCount());
            if (keyMatch(order, theKey) && (!mPromotionsHelper.depictionMatch(order, theDepiction, promoItemCount.intValue()))) {
              removeShipping(FREEBASESHIPPING, theKey, theCode, theName, order, false);
            }
          }// end type 38
          else if (theShipPromo.getType().trim().equals("39")) {
            theDepiction = theShipPromo.getDepiction();
            promoDollarQual = Double.parseDouble(theShipPromo.getDollarQualifier());
            final Integer promoItemCount = new Integer(theShipPromo.getItemCount());
            if (keyMatch(order, theKey) && (!mPromotionsHelper.depictionDollarMatch(order, theDepiction, promoDollarQual, promoItemCount.intValue()))) {
              removeShipping(FREEBASESHIPPING, theKey, theCode, theName, order, false);
            }
          }// end type 39
          else if (theShipPromo.getType().trim().equals("40")) {
            final Integer promoItemCount = new Integer(theShipPromo.getItemCount());
            if (keyMatch(order, theKey) && (!mPromotionsHelper.saleMatch(order, true, promoItemCount.intValue()))) {
              removeShipping(FREEBASESHIPPING, theKey, theCode, theName, order, false);
            }
          }// end type 40
          else if (theShipPromo.getType().trim().equals("41")) {
            final Integer promoItemCount = new Integer(theShipPromo.getItemCount());
            if (keyMatch(order, theKey) && (!mPromotionsHelper.saleMatch(order, false, promoItemCount.intValue()))) {
              removeShipping(FREEBASESHIPPING, theKey, theCode, theName, order, false);
            }
          }// end type 41
          else if (theShipPromo.getType().trim().equals("42")) {
            final Integer promoItemCount = new Integer(theShipPromo.getItemCount());
            promoDollarQual = Double.parseDouble(theShipPromo.getDollarQualifier());
            if (keyMatch(order, theKey) && (!mPromotionsHelper.saleDollarMatch(order, promoDollarQual, true, promoItemCount.intValue()))) {
              removeShipping(FREEBASESHIPPING, theKey, theCode, theName, order, false);
            }
          }// end type 42
          else if (theShipPromo.getType().trim().equals("43")) {
            final Integer promoItemCount = new Integer(theShipPromo.getItemCount());
            promoDollarQual = Double.parseDouble(theShipPromo.getDollarQualifier());
            if (keyMatch(order, theKey) && (!mPromotionsHelper.saleDollarMatch(order, promoDollarQual, false, promoItemCount.intValue()))) {
              removeShipping(FREEBASESHIPPING, theKey, theCode, theName, order, false);
            }
          }// end type 43
          else if (theShipPromo.getType().trim().equals("44")) {
            theCatalogs = theShipPromo.getQualifyCatalog().trim().toUpperCase();
            final Integer promoItemCount = new Integer(theShipPromo.getItemCount());
            if (keyMatch(order, theKey) && (!mPromotionsHelper.catalogMatch(order, theCatalogs, promoItemCount.intValue()))) {
              removeShipping(FREEBASESHIPPING, theKey, theCode, theName, order, false);
            }
          }// end type 44
          else if (theShipPromo.getType().trim().equals("45")) {
            theCatalogs = theShipPromo.getQualifyCatalog().trim().toUpperCase();
            final Integer promoItemCount = new Integer(theShipPromo.getItemCount());
            promoDollarQual = Double.parseDouble(theShipPromo.getDollarQualifier());
            if (keyMatch(order, theKey) && (!mPromotionsHelper.catalogDollarMatch(order, theCatalogs, promoDollarQual, promoItemCount.intValue()))) {
              removeShipping(FREEBASESHIPPING, theKey, theCode, theName, order, false);
            }
          }// end type 45
          else if (theShipPromo.getType().trim().equals("46")) {
            if (keyMatch(order, theKey) && (!mPromotionsHelper.saleMatch(order, true, 0))) {
              removeShipping(FREEBASESHIPPING, theKey, theKey, theName, order, false);
            }
          }// end type 46
          else if (theShipPromo.getType().trim().equals("47")) {
            if (keyMatch(order, theKey) && (!mPromotionsHelper.saleMatch(order, false, 0))) {
              removeShipping(FREEBASESHIPPING, theKey, theKey, theName, order, false);
            }
          }// end type 47
          else if (theShipPromo.getType().trim().equals("48")) {
            promoDollarQual = Double.parseDouble(theShipPromo.getDollarQualifier());
            if (keyMatch(order, theKey) && (!mPromotionsHelper.saleDollarMatch(order, promoDollarQual, true, 0))) {
              removeShipping(FREEBASESHIPPING, theKey, theKey, theName, order, false);
            }
          }// end type 48
          else if (theShipPromo.getType().trim().equals("49")) {
            promoDollarQual = Double.parseDouble(theShipPromo.getDollarQualifier());
            if (keyMatch(order, theKey) && (!mPromotionsHelper.saleDollarMatch(order, promoDollarQual, false, 0))) {
              removeShipping(FREEBASESHIPPING, theKey, theKey, theName, order, false);
            }
          }// end type 49
          else if (theShipPromo.getType().trim().equals("50")) {
            theCatalogs = theShipPromo.getQualifyCatalog().trim().toUpperCase();
            if (keyMatch(order, theKey) && (!mPromotionsHelper.catalogMatch(order, theCatalogs, 0))) {
              removeShipping(FREEBASESHIPPING, theKey, theKey, theName, order, false);
            }
          }// end type 50
          if (theShipPromo.getType().trim().equals("51")) {
            final Integer promoItemCount = new Integer(theShipPromo.getItemCount());
            if (keyMatch(order, theKey) && (!mPromotionsHelper.dollarQualifies(order, theShipPromo.getDollarQualifier().trim(), promoItemCount.intValue()))) {
              removeShipping(FREEBASESHIPPING, theKey, theKey, theName, order, false);
            }
          }// end type 51
          if (theShipPromo.getType().trim().equals("52")) {
            final Integer promoItemCount = new Integer(theShipPromo.getItemCount());
            if (keyMatch(order, theKey) && (!mPromotionsHelper.dollarQualifies(order, theShipPromo.getDollarQualifier().trim(), promoItemCount.intValue()))) {
              removeShipping(FREEBASESHIPPING, theKey, theCode, theName, order, false);
            }
          }// end type 52
          else if (theShipPromo.getType().trim().equals("53")) {
            theDept = theShipPromo.getDeptCodes().trim().toUpperCase();
            final Integer promoItemCount = new Integer(theShipPromo.getItemCount());
            if (keyMatch(order, theKey) && (!mPromotionsHelper.deptMatch(order, theDept, promoItemCount.intValue()))) {
              removeShipping(FREEBASESHIPPING, theKey, theKey, theName, order, false);
            }
          }// end type 53
          else if (theShipPromo.getType().trim().equals("54")) {
            theDepiction = theShipPromo.getDepiction();
            final Integer promoItemCount = new Integer(theShipPromo.getItemCount());
            if (keyMatch(order, theKey) && (!mPromotionsHelper.depictionMatch(order, theDepiction, promoItemCount.intValue()))) {
              removeShipping(FREEBASESHIPPING, theKey, theKey, theName, order, false);
            }
          }// end type 54
          else if (theShipPromo.getType().trim().equals("55")) {
            final Integer promoItemCount = new Integer(theShipPromo.getItemCount());
            if (keyMatch(order, theKey) && (!mPromotionsHelper.saleMatch(order, true, promoItemCount.intValue()))) {
              removeShipping(FREEBASESHIPPING, theKey, theKey, theName, order, false);
            }
          }// end type 55
          else if (theShipPromo.getType().trim().equals("56")) {
            final Integer promoItemCount = new Integer(theShipPromo.getItemCount());
            if (keyMatch(order, theKey) && (!mPromotionsHelper.saleMatch(order, false, promoItemCount.intValue()))) {
              removeShipping(FREEBASESHIPPING, theKey, theKey, theName, order, false);
            }
          }// end type 56
          else if (theShipPromo.getType().trim().equals("57")) {
            final Integer promoItemCount = new Integer(theShipPromo.getItemCount());
            promoDollarQual = Double.parseDouble(theShipPromo.getDollarQualifier());
            if (keyMatch(order, theKey) && (!mPromotionsHelper.saleDollarMatch(order, promoDollarQual, true, promoItemCount.intValue()))) {
              removeShipping(FREEBASESHIPPING, theKey, theKey, theName, order, false);
            }
          }// end type 57
          else if (theShipPromo.getType().trim().equals("58")) {
            final Integer promoItemCount = new Integer(theShipPromo.getItemCount());
            promoDollarQual = Double.parseDouble(theShipPromo.getDollarQualifier());
            if (keyMatch(order, theKey) && (!mPromotionsHelper.saleDollarMatch(order, promoDollarQual, false, promoItemCount.intValue()))) {
              removeShipping(FREEBASESHIPPING, theKey, theKey, theName, order, false);
            }
          }// end type 58
          else if (theShipPromo.getType().trim().equals("59")) {
            theCatalogs = theShipPromo.getQualifyCatalog().trim().toUpperCase();
            final Integer promoItemCount = new Integer(theShipPromo.getItemCount());
            if (keyMatch(order, theKey) && (!mPromotionsHelper.catalogMatch(order, theCatalogs, promoItemCount.intValue()))) {
              removeShipping(FREEBASESHIPPING, theKey, theKey, theName, order, false);
            }
          }// end type 59
          else if (theShipPromo.getType().trim().equals("60")) {
            theCatalogs = theShipPromo.getQualifyCatalog().trim().toUpperCase();
            final Integer promoItemCount = new Integer(theShipPromo.getItemCount());
            promoDollarQual = Double.parseDouble(theShipPromo.getDollarQualifier());
            if (keyMatch(order, theKey) && (!mPromotionsHelper.catalogDollarMatch(order, theCatalogs, promoDollarQual, promoItemCount.intValue()))) {
              removeShipping(FREEBASESHIPPING, theKey, theKey, theName, order, false);
            }
          }// end type 60
          else if (theShipPromo.getType().trim().equals("61")) {
            if (keyMatch(order, theKey) && (!mPromotionsHelper.dollarQualifies(order, theShipPromo.getDollarQualifier().trim(), 0))) {
              removeShipping(FREEBASESHIPPING, theKey, theCode, theName, order, false);
              removeShipping(WEBSL1, theKey, theCode, theName, order, false);
            }
          }// end type 61
          else if (theShipPromo.getType().trim().equals("62")) {
            if (keyMatch(order, theKey) && (!mPromotionsHelper.dollarQualifies(order, theShipPromo.getDollarQualifier().trim(), 0))) {
              removeShipping(FREEBASESHIPPING, theKey, theCode, theName, order, false);
              removeShipping(WEBSL2, theKey, theCode, theName, order, false);
            }
          }// end type 62
          else if (theShipPromo.getType().trim().equals("63")) {
            if (keyMatch(order, theKey) && (!mPromotionsHelper.dollarQualifies(order, theShipPromo.getDollarQualifier().trim(), 0))) {
              removeShipping(FREEBASESHIPPING, theKey, theCode, theName, order, false);
              removeShipping(WEBSL1, theKey, theKey, theName, order, false);
            }
          }// end type 63
          else if (theShipPromo.getType().trim().equals("64")) {
            if (keyMatch(order, theKey) && (!mPromotionsHelper.dollarQualifies(order, theShipPromo.getDollarQualifier().trim(), 0))) {
              removeShipping(FREEBASESHIPPING, theKey, theCode, theName, order, false);
              removeShipping(WEBSL2, theKey, theKey, theName, order, false);
            }
          }// end type 64
          else if (theShipPromo.getType().trim().equals("65")) {
            if (keyMatch(order, theKey) && (!mPromotionsHelper.dollarQualifies(order, theShipPromo.getDollarQualifier().trim(), 0) && mPromotionsHelper.countryQualifies(order, theKey))) {
              removeShipping(FREEBASESHIPPING, theKey, theCode, theName, order, false);
              removeShipping(CMOSUPGRADE, theKey, theCode, theName, order, true);
            }
          }// end type 65
          else if (theShipPromo.getType().trim().equals("66")) {
            if (keyMatch(order, theKey) && (!mPromotionsHelper.dollarQualifies(order, theShipPromo.getDollarQualifier().trim(), 0))) {
              removeShipping(WEBSL2, theKey, theCode, theName, order, false);
            }
          }// end type 66
          else if (theShipPromo.getType().trim().equals("67")) {
            if (keyMatch(order, theKey) && (!mPromotionsHelper.dollarQualifies(order, theShipPromo.getDollarQualifier().trim(), 0))) {
              removeShipping(WEBSL1, theKey, theCode, theName, order, false);
            }
          }// end type 67
          else if (theShipPromo.getType().trim().equals("68")) {
            if (keyMatch(order, theKey) && (!mPromotionsHelper.dollarQualifies(order, theShipPromo.getDollarQualifier().trim(), 0))) {
              removeShipping(WEBSL2, theKey, theKey, theName, order, false);
            }
          }// end type 68
          else if (theShipPromo.getType().trim().equals("69")) {
            if (keyMatch(order, theKey) && (!mPromotionsHelper.dollarQualifies(order, theShipPromo.getDollarQualifier().trim(), 0))) {
              removeShipping(WEBSL1, theKey, theKey, theName, order, false);
            }
          }// end type 69
          else if (theShipPromo.getType().trim().equals("100")) {
            if (keyMatch(order, theKey) && (!mPromotionsHelper.dollarQualifies(order, theShipPromo.getDollarQualifier().trim(), 0))) {
              removeShipping("", theKey, theKey, theName, order, false);
            }
          }// end type 100
          else if (theShipPromo.getType().trim().equals("101")) {
            if (keyMatch(order, theKey) && (!mPromotionsHelper.dollarQualifies(order, theShipPromo.getDollarQualifier().trim(), 0))) {
              removeShipping("", theKey, theCode, theName, order, false);
            }
          }// end type 101
          else if (theShipPromo.getType().trim().equals("102")) {
            if (keyMatch(order, theKey) && (!mPromotionsHelper.orderHasProd(order, theKey))) {
              removeShipping("", theKey, theKey, theName, order, false);
            }
          }// end type 102
          else if (theShipPromo.getType().trim().equals("103")) {
            if (keyMatch(order, theKey) && (!mPromotionsHelper.orderHasProdDollar(order, theKey, theShipPromo.getDollarQualifier().trim()))) {
              removeShipping("", theKey, theKey, theName, order, false);
            }
          }// end type 103
          else if (theShipPromo.getType().trim().equals("104")) {
            if (keyMatch(order, theKey) && (!mPromotionsHelper.orderHasProdDollar(order, theKey, theShipPromo.getDollarQualifier().trim()))) {
              removeShipping("", theKey, theCode, theName, order, false);
            }
          }// end type 104
          else if (theShipPromo.getType().trim().equals("105")) {
            
            if (keyMatch(order, theKey) && (!mPromotionsHelper.dollarQualifies(order, theShipPromo.getDollarQualifier().trim(), 0) || !mPromotionsHelper.countryQualifies(order, theKey))) {
              removeShipping(CMOSUPGRADE, theKey, theKey, theName, order, true);
            }
          }// end type 105
          else if (theShipPromo.getType().trim().equals("106")) {
            
            if (keyMatch(order, theKey) && (!mPromotionsHelper.dollarQualifies(order, theShipPromo.getDollarQualifier().trim(), 0) || !mPromotionsHelper.countryQualifies(order, theKey))) {
              removeShipping(FREEBASESHIPPING, theKey, theKey, theName, order, false);
              removeShipping(CMOSUPGRADE, theKey, theKey, theName, order, true);
            }
            
          }// end type 106
          else if (theShipPromo.getType().trim().equals("107")) {
            try {
              if (keyMatch(order, theKey) && !ShoppingCartHandler.isInCircleAccount(CheckoutComponents.getProfile(ServletUtil.getCurrentRequest()), order)) {
                removeShipping(FREEBASESHIPPING, theKey, theKey, theName, order, false);
              }
            } catch (final Exception ce) {
              throw new ScenarioException(ce);
            }
          }// end type 107
          else if (theShipPromo.getType().trim().equals("108")) {
            try {
              if (keyMatch(order, theKey) && !ShoppingCartHandler.isInCircleAccount(CheckoutComponents.getProfile(ServletUtil.getCurrentRequest()), order)) {
                removeShipping(CMOSUPGRADE, theKey, theKey, theName, order, true);
              }
            } catch (final Exception ce) {
              throw new ScenarioException(ce);
            }
          }// end type 108
          else if (theShipPromo.getType().trim().equals("109")) {
            try {
              if (keyMatch(order, theKey) && !ShoppingCartHandler.isInCircleAccount(CheckoutComponents.getProfile(ServletUtil.getCurrentRequest()), order)) {
                removeShipping(FREEBASESHIPPING, theKey, theKey, theName, order, false);
                removeShipping(CMOSUPGRADE, theKey, theKey, theName, order, true);
              }
            } catch (final Exception ce) {
              throw new ScenarioException(ce);
            }
          } // end type 109
          else if (theShipPromo.getType().trim().equals("110")) {
            if (keyMatch(order, theKey)
                    && (!mPromotionsHelper.orderHasProdDollar(order, theKey, theShipPromo.getDollarQualifier().trim()) || !mPromotionsHelper.promoCodeMatch(order, theCode)
                            || !mPromotionsHelper.orderHasProd(order, theKey) || !mPromotionsHelper.countryQualifies(order, theKey) || ((NMOrderImpl) order).getUsedLimitPromoMap().contains(theKey))) {
              
              removeShipping(FREEBASESHIPPING, theKey, theCode, theName, order, false);
              
            }
          }// end type 110
          else if (theShipPromo.getType().trim().equals("111")) {
            if (keyMatch(order, theKey)
                    && (!mPromotionsHelper.orderHasProdDollar(order, theKey, theShipPromo.getDollarQualifier().trim()) || !mPromotionsHelper.orderHasProd(order, theKey)
                            || !mPromotionsHelper.countryQualifies(order, theKey) || ((NMOrderImpl) order).getUsedLimitPromoMap().contains(theKey))) {
              
              removeShipping(FREEBASESHIPPING, theKey, theKey, theName, order, false);
              
            }
          }// end type 111
          else if (theShipPromo.getType().trim().equals("112")) {
            if (keyMatch(order, theKey)
                    && (!mPromotionsHelper.orderHasProdDollar(order, theKey, theShipPromo.getDollarQualifier().trim()) || !mPromotionsHelper.promoCodeMatch(order, theCode)
                            || !mPromotionsHelper.orderHasProd(order, theKey) || !mPromotionsHelper.countryQualifies(order, theKey))) {
              removeShipping(CMOSUPGRADE, theKey, theCode, theName, order, true);
            }
          }// end type 112
          else if (theShipPromo.getType().trim().equals("113")) {
            if (keyMatch(order, theKey)
                    && (!mPromotionsHelper.orderHasProdDollar(order, theKey, theShipPromo.getDollarQualifier().trim()) || !mPromotionsHelper.orderHasProd(order, theKey) || !mPromotionsHelper
                            .countryQualifies(order, theKey))) {
              removeShipping(CMOSUPGRADE, theKey, theKey, theName, order, true);
            }
          }// end type 113
          else if (theShipPromo.getType().trim().equals("114")) {
            if (keyMatch(order, theKey)
                    && (!mPromotionsHelper.orderHasProdDollar(order, theKey, theShipPromo.getDollarQualifier().trim()) || !mPromotionsHelper.promoCodeMatch(order, theCode)
                            || !mPromotionsHelper.orderHasProd(order, theKey) || !mPromotionsHelper.countryQualifies(order, theKey))) {
              removeShipping(FREEBASESHIPPING, theKey, theCode, theName, order, false);
              removeShipping(CMOSUPGRADE, theKey, theCode, theName, order, true);
            }
          }// end type 114
          else if (theShipPromo.getType().trim().equals("115")) {
            if (keyMatch(order, theKey)
                    && (!mPromotionsHelper.orderHasProdDollar(order, theKey, theShipPromo.getDollarQualifier().trim()) || !mPromotionsHelper.orderHasProd(order, theKey) || !mPromotionsHelper
                            .countryQualifies(order, theKey))) {
              removeShipping(FREEBASESHIPPING, theKey, theKey, theName, order, false);
              removeShipping(CMOSUPGRADE, theKey, theKey, theName, order, true);
            }
          }// end type 115
          else if (theShipPromo.getType().trim().equals("116")) {
            final Integer promoItemCount = new Integer(theShipPromo.getItemCount());
            if (keyMatch(order, theKey)
                    && (!mPromotionsHelper.promoCodeMatch(order, theCode) || !mPromotionsHelper.orderHasProdDollar(order, theKey, theShipPromo.getDollarQualifier().trim())
                            || !mPromotionsHelper.orderHasProdCount(order, theKey, promoItemCount.intValue()) || !mPromotionsHelper.countryQualifies(order, theKey))) {
              removeShipping(FREEBASESHIPPING, theKey, theCode, theName, order, false);
            }
          }// end type 116
          else if (theShipPromo.getType().trim().equals("117")) {
            final Integer promoItemCount = new Integer(theShipPromo.getItemCount());
            if (keyMatch(order, theKey)
                    && (!mPromotionsHelper.orderHasProdDollar(order, theKey, theShipPromo.getDollarQualifier().trim())
                            || !mPromotionsHelper.orderHasProdCount(order, theKey, promoItemCount.intValue()) || !mPromotionsHelper.countryQualifies(order, theKey))) {
              removeShipping(FREEBASESHIPPING, theKey, theKey, theName, order, false);
            }
          }// end type 117
          else if (theShipPromo.getType().trim().equals("118")) {
            final Integer promoItemCount = new Integer(theShipPromo.getItemCount());
            if (keyMatch(order, theKey)
                    && (!mPromotionsHelper.promoCodeMatch(order, theCode) || !mPromotionsHelper.orderHasProdDollar(order, theKey, theShipPromo.getDollarQualifier().trim())
                            || !mPromotionsHelper.orderHasProdCount(order, theKey, promoItemCount.intValue()) || !mPromotionsHelper.countryQualifies(order, theKey))) {
              removeShipping(CMOSUPGRADE, theKey, theCode, theName, order, true);
            }
          }// end type 118
          else if (theShipPromo.getType().trim().equals("119")) {
            final Integer promoItemCount = new Integer(theShipPromo.getItemCount());
            if (keyMatch(order, theKey)
                    && (!mPromotionsHelper.orderHasProdDollar(order, theKey, theShipPromo.getDollarQualifier().trim())
                            || !mPromotionsHelper.orderHasProdCount(order, theKey, promoItemCount.intValue()) || !mPromotionsHelper.countryQualifies(order, theKey))) {
              removeShipping(CMOSUPGRADE, theKey, theKey, theName, order, true);
            }
          }// end type 119
          else if (theShipPromo.getType().trim().equals("120")) {
            final Integer promoItemCount = new Integer(theShipPromo.getItemCount());
            if (keyMatch(order, theKey)
                    && (!mPromotionsHelper.promoCodeMatch(order, theCode) || !mPromotionsHelper.orderHasProdDollar(order, theKey, theShipPromo.getDollarQualifier().trim())
                            || !mPromotionsHelper.orderHasProdCount(order, theKey, promoItemCount.intValue()) || !mPromotionsHelper.countryQualifies(order, theKey))) {
              removeShipping(FREEBASESHIPPING, theKey, theCode, theName, order, false);
              removeShipping(CMOSUPGRADE, theKey, theCode, theName, order, true);
            }
          }// end type 120
          else if (theShipPromo.getType().trim().equals("121")) {
            final Integer promoItemCount = new Integer(theShipPromo.getItemCount());
            if (keyMatch(order, theKey)
                    && (!mPromotionsHelper.orderHasProdDollar(order, theKey, theShipPromo.getDollarQualifier().trim())
                            || !mPromotionsHelper.orderHasProdCount(order, theKey, promoItemCount.intValue()) || !mPromotionsHelper.countryQualifies(order, theKey))) {
              removeShipping(FREEBASESHIPPING, theKey, theKey, theName, order, false);
              removeShipping(CMOSUPGRADE, theKey, theKey, theName, order, true);
            }
          }// end type 121
          else if (theShipPromo.getType().trim().equals("122")) {
            orderImpl = (NMOrderImpl) order;
            final boolean validateTenderType = ((Boolean) orderImpl.getPropertyValue("validateTenderType")).booleanValue();
            if (validateTenderType) {
              if (keyMatch(order, theKey)
                      && (!mPromotionsHelper.dollarQualifies(order, theShipPromo.getDollarQualifier().trim(), 0)
                              || !mPromotionsHelper.isValidCreditCard(CheckoutComponents.getProfile(ServletUtil.getCurrentRequest()), order, theKey) || !mPromotionsHelper.countryQualifies(order,
                              theKey))) {
                removeShipping(FREEBASESHIPPING, theKey, theKey, theName, order, false);
                
              }
            } else {
              if (keyMatch(order, theKey) && (!mPromotionsHelper.dollarQualifies(order, theShipPromo.getDollarQualifier().trim(), 0) || !mPromotionsHelper.countryQualifies(order, theKey))) {
                removeShipping(FREEBASESHIPPING, theKey, theKey, theName, order, false);
                
              }
            }
          } else if (theShipPromo.getType().trim().equals("123")) {
            orderImpl = (NMOrderImpl) order;
            final boolean validateTenderType = ((Boolean) orderImpl.getPropertyValue("validateTenderType")).booleanValue();
            if (validateTenderType) {
              if (keyMatch(order, theKey)
                      && (!mPromotionsHelper.dollarQualifies(order, theShipPromo.getDollarQualifier().trim(), 0)
                              || !mPromotionsHelper.isValidCreditCard(CheckoutComponents.getProfile(ServletUtil.getCurrentRequest()), order, theKey) || !mPromotionsHelper.countryQualifies(order,
                              theKey))) {
                
                removeShipping(FREEBASESHIPPING, theKey, theCode, theName, order, false);
              }
            } else {
              if (keyMatch(order, theKey) && (!mPromotionsHelper.dollarQualifies(order, theShipPromo.getDollarQualifier().trim(), 0) || !mPromotionsHelper.countryQualifies(order, theKey))) {
                removeShipping(FREEBASESHIPPING, theKey, theCode, theName, order, false);
              }
            }
          }// end type 123
          else if (theShipPromo.getType().trim().equals("124")) {
            if (keyMatch(order, theKey)
                    && (!mPromotionsHelper.dollarQualifies(order, theShipPromo.getDollarQualifier().trim(), 0)
                            || !mPromotionsHelper.isValidCreditCard(CheckoutComponents.getProfile(ServletUtil.getCurrentRequest()), order, theKey) || !mPromotionsHelper
                              .countryQualifies(order, theKey))) {
              removeShipping(CMOSUPGRADE, theKey, theKey, theName, order, true);
            }
          } else if (theShipPromo.getType().trim().equals("125")) {
            orderImpl = (NMOrderImpl) order;
            final boolean validateTenderType = ((Boolean) orderImpl.getPropertyValue("validateTenderType")).booleanValue();
            if (validateTenderType) {
              if (keyMatch(order, theKey)
                      && (!mPromotionsHelper.dollarQualifies(order, theShipPromo.getDollarQualifier().trim(), 0)
                              || !mPromotionsHelper.isValidCreditCard(CheckoutComponents.getProfile(ServletUtil.getCurrentRequest()), order, theKey) || !mPromotionsHelper.countryQualifies(order,
                              theKey))) {
                
                removeShipping(CMOSUPGRADE, theKey, theCode, theName, order, true);
              }
            } else {
              if (keyMatch(order, theKey) && (!mPromotionsHelper.dollarQualifies(order, theShipPromo.getDollarQualifier().trim(), 0) || !mPromotionsHelper.countryQualifies(order, theKey))) {
                removeShipping(CMOSUPGRADE, theKey, theCode, theName, order, true);
              }
            }
          }// end type 125
          else if (theShipPromo.getType().trim().equals("126")) {
            if (keyMatch(order, theKey)
                    && (!mPromotionsHelper.orderHasProdDollar(order, theKey, theShipPromo.getDollarQualifier().trim()) || !mPromotionsHelper.orderHasProd(order, theKey)
                            || !mPromotionsHelper.isValidCreditCard(CheckoutComponents.getProfile(ServletUtil.getCurrentRequest()), order, theKey) || !mPromotionsHelper
                              .countryQualifies(order, theKey))) {
              removeShipping(FREEBASESHIPPING, theKey, theKey, theName, order, false);
            }
          }// end type 126
          else if (theShipPromo.getType().trim().equals("127")) {
            orderImpl = (NMOrderImpl) order;
            final boolean validateTenderType = ((Boolean) orderImpl.getPropertyValue("validateTenderType")).booleanValue();
            if (validateTenderType) {
              if (keyMatch(order, theKey)
                      && (!mPromotionsHelper.orderHasProdDollar(order, theKey, theShipPromo.getDollarQualifier().trim()) || !mPromotionsHelper.orderHasProd(order, theKey)
                              || !mPromotionsHelper.isValidCreditCard(CheckoutComponents.getProfile(ServletUtil.getCurrentRequest()), order, theKey) || !mPromotionsHelper.countryQualifies(order,
                              theKey))) {
                
                removeShipping(FREEBASESHIPPING, theKey, theCode, theName, order, false);
              }
            } else {
              if (keyMatch(order, theKey)
                      && (!mPromotionsHelper.orderHasProdDollar(order, theKey, theShipPromo.getDollarQualifier().trim()) || !mPromotionsHelper.orderHasProd(order, theKey) || !mPromotionsHelper
                              .countryQualifies(order, theKey))) {
                removeShipping(FREEBASESHIPPING, theKey, theCode, theName, order, false);
              }
            }
          }// end type 127
          else if (theShipPromo.getType().trim().equals("128")) {
            if (keyMatch(order, theKey)
                    && (!mPromotionsHelper.orderHasProdDollar(order, theKey, theShipPromo.getDollarQualifier().trim()) || !mPromotionsHelper.orderHasProd(order, theKey)
                            || !mPromotionsHelper.isValidCreditCard(CheckoutComponents.getProfile(ServletUtil.getCurrentRequest()), order, theKey) || !mPromotionsHelper
                              .countryQualifies(order, theKey))) {
              removeShipping(CMOSUPGRADE, theKey, theKey, theName, order, true);
            }
          }// end type 128
          else if (theShipPromo.getType().trim().equals("129")) {
            orderImpl = (NMOrderImpl) order;
            final boolean validateTenderType = ((Boolean) orderImpl.getPropertyValue("validateTenderType")).booleanValue();
            if (validateTenderType) {
              if (keyMatch(order, theKey)
                      && (!mPromotionsHelper.orderHasProdDollar(order, theKey, theShipPromo.getDollarQualifier().trim()) || !mPromotionsHelper.orderHasProd(order, theKey)
                              || !mPromotionsHelper.isValidCreditCard(CheckoutComponents.getProfile(ServletUtil.getCurrentRequest()), order, theKey) || !mPromotionsHelper.countryQualifies(order,
                              theKey))) {
                
                removeShipping(CMOSUPGRADE, theKey, theCode, theName, order, true);
              }
            } else {
              if (keyMatch(order, theKey)
                      && (!mPromotionsHelper.orderHasProdDollar(order, theKey, theShipPromo.getDollarQualifier().trim()) || !mPromotionsHelper.orderHasProd(order, theKey) || !mPromotionsHelper
                              .countryQualifies(order, theKey))) {
                removeShipping(CMOSUPGRADE, theKey, theCode, theName, order, true);
              }
            }
          }// end type 129
          else if (theShipPromo.getType().trim().equals("130")) {
            final RepositoryItem profile = CheckoutComponents.getProfile(ServletUtil.getCurrentRequest());
            lvl = ShoppingCartHandler.findInCircleBenefitLvl(profile, order);
            if (keyMatch(order, theKey)
                    && (!mPromotionsHelper.isValidIncircleMbr(lvl) || !mPromotionsHelper.isQualifiedIncircleLvl(lvl, theKey, "shipping") || !mPromotionsHelper.dollarQualifies(order, theShipPromo
                            .getDollarQualifier().trim()))) {
              removeShipping(FREEBASESHIPPING, theKey, theKey, theName, order, false);
            }
            
          }// end type 130
          else if (theShipPromo.getType().trim().equals("131")) {
            
            orderImpl = (NMOrderImpl) order;
            final boolean validateTenderType = ((Boolean) orderImpl.getPropertyValue("validateTenderType")).booleanValue();
            if (validateTenderType) {
              final RepositoryItem profile = CheckoutComponents.getProfile(ServletUtil.getCurrentRequest());
              lvl = ShoppingCartHandler.findInCircleBenefitLvl(profile, order);
              if (keyMatch(order, theKey)
                      && (!mPromotionsHelper.dollarQualifies(order, theShipPromo.getDollarQualifier().trim()) || !mPromotionsHelper.isValidIncircleMbr(lvl) || !mPromotionsHelper
                              .isQualifiedIncircleLvl(lvl, theKey, "shipping"))) {
                removeShipping(FREEBASESHIPPING, theKey, theCode, theName, order, false);
              }
            } else {
              if (keyMatch(order, theKey) && (!mPromotionsHelper.dollarQualifies(order, theShipPromo.getDollarQualifier().trim()))) {
                removeShipping(FREEBASESHIPPING, theKey, theCode, theName, order, false);
              }
            }
            // }
          } else if (theShipPromo.getType().trim().equals("132")) {
            final RepositoryItem profile = CheckoutComponents.getProfile(ServletUtil.getCurrentRequest());
            lvl = ShoppingCartHandler.findInCircleBenefitLvl(profile, order);
            if (keyMatch(order, theKey)
                    && (!mPromotionsHelper.dollarQualifies(order, theShipPromo.getDollarQualifier().trim()) || !mPromotionsHelper.isValidIncircleMbr(lvl) || !mPromotionsHelper.isQualifiedIncircleLvl(
                            lvl, theKey, "shipping"))) {
              removeShipping(CMOSUPGRADE, theKey, theKey, theName, order, true);
              // }
            }
          } else if (theShipPromo.getType().trim().equals("133")) {
            orderImpl = (NMOrderImpl) order;
            final boolean validateTenderType = ((Boolean) orderImpl.getPropertyValue("validateTenderType")).booleanValue();
            
            if (!validateTenderType) {
              if (keyMatch(order, theKey) && (!mPromotionsHelper.dollarQualifies(order, theShipPromo.getDollarQualifier().trim()))) {
                removeShipping(CMOSUPGRADE, theKey, theCode, theName, order, true);
              }
            } else {
              final RepositoryItem profile = CheckoutComponents.getProfile(ServletUtil.getCurrentRequest());
              lvl = ShoppingCartHandler.findInCircleBenefitLvl(profile, order);
              if (keyMatch(order, theKey)
                      && (!mPromotionsHelper.dollarQualifies(order, theShipPromo.getDollarQualifier().trim()) || !mPromotionsHelper.isValidIncircleMbr(lvl) || !mPromotionsHelper
                              .isQualifiedIncircleLvl(lvl, theKey, "shipping"))) {
                removeShipping(CMOSUPGRADE, theKey, theCode, theName, order, true);
              }
              
            }
          } else if (theShipPromo.getType().trim().equals("134")) {
            final RepositoryItem profile = CheckoutComponents.getProfile(ServletUtil.getCurrentRequest());
            lvl = ShoppingCartHandler.findInCircleBenefitLvl(profile, order);
            if (keyMatch(order, theKey)
                    && (!mPromotionsHelper.dollarQualifies(order, theShipPromo.getDollarQualifier().trim()) || !mPromotionsHelper.isValidIncircleMbr(lvl) || !mPromotionsHelper.isQualifiedIncircleLvl(
                            lvl, theKey, "shipping"))) {
              removeShipping(FREEBASESHIPPING, theKey, theKey, theName, order, false);
              removeShipping(CMOSUPGRADE, theKey, theKey, theName, order, true);
            }
          } else if (theShipPromo.getType().trim().equals("135")) {
            orderImpl = (NMOrderImpl) order;
            final boolean validateTenderType = ((Boolean) orderImpl.getPropertyValue("validateTenderType")).booleanValue();
            if (!validateTenderType) {
              if (keyMatch(order, theKey) && (!mPromotionsHelper.dollarQualifies(order, theShipPromo.getDollarQualifier().trim()))) {
                removeShipping(FREEBASESHIPPING, theKey, theCode, theName, order, false);
                removeShipping(CMOSUPGRADE, theKey, theCode, theName, order, true);
              }
            } else {
              final RepositoryItem profile = CheckoutComponents.getProfile(ServletUtil.getCurrentRequest());
              lvl = ShoppingCartHandler.findInCircleBenefitLvl(profile, order);
              if (keyMatch(order, theKey)
                      && (!mPromotionsHelper.dollarQualifies(order, theShipPromo.getDollarQualifier().trim()) || !mPromotionsHelper.isValidIncircleMbr(lvl) || !mPromotionsHelper
                              .isQualifiedIncircleLvl(lvl, theKey, "shipping"))) {
                removeShipping(FREEBASESHIPPING, theKey, theCode, theName, order, false);
                removeShipping(CMOSUPGRADE, theKey, theCode, theName, order, true);
              }
              
            }
            
          }// end type 135
          else if (theShipPromo.getType().trim().equals("136")) {
            if (keyMatch(order, theKey) && (!mPromotionsHelper.orderHasBnglKey(order, theKey))) {
              removeShipping(FREEBASESHIPPING, theKey, theKey, theName, order, false);
            }
          }// end type 136
          else if (theShipPromo.getType().trim().equals("137")) {
            if (keyMatch(order, theKey) && (!mPromotionsHelper.shopRunnerMatch(order))) {
              removeShipping(SHOPRUNNER, theKey, theKey, theName, order, false);
            }
          }// end type 137
        
      } // end for
      
    } catch (final Exception e) {
      e.printStackTrace();
    }
  } // end checkPromotions
  
  public RepositoryItem getProfileForOrder(final Order order) throws PromoException {
    RepositoryItem profile = null;
    
    try {
      if (mOrderManager != null) {
        profile = mOrderManager.getOrderTools().getProfileTools().getProfileItem(order.getProfileId());
      }
    } catch (final RepositoryException e) {
      throw new PromoException(e);
    }
    
    return profile;
  }
  
}// end class
