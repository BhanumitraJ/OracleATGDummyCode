package com.nm.commerce.promotion;

import static com.nm.integration.MasterPassConstants.DYNAMIC_FREE_SHIPPING;
import static com.nm.integration.MasterPassConstants.DYNAMIC_FREE_SHIPPING_UPGRADE;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.servlet.http.Cookie;
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
import atg.process.ProcessException;
import atg.process.ProcessExecutionContext;
import atg.process.action.ActionImpl;
import atg.repository.MutableRepositoryItem;
import atg.repository.RepositoryException;
import atg.repository.RepositoryItem;
import atg.servlet.DynamoHttpServletRequest;
import atg.servlet.DynamoHttpServletResponse;
import atg.servlet.ServletUtil;

import com.nm.abtest.AbTestHelper;
import com.nm.collections.NMPromotion;
import com.nm.collections.Promotion;
import com.nm.collections.PromotionsArray;
import com.nm.commerce.NMProfile;
import com.nm.commerce.checkout.CheckoutComponents;
import com.nm.commerce.order.NMOrderManager;
import com.nm.components.CommonComponentHelper;
import com.nm.formhandler.ShoppingCartHandler;
import com.nm.integration.util.NMCheckoutTypeUtil;
import com.nm.repository.ProfileRepository;
import com.nm.utils.BrandSpecs;
import com.nm.utils.ExceptionUtil;
import com.nm.utils.LocalizationUtils;
import com.nm.utils.NmoUtils;
import com.nm.utils.PersonalizedCustData;
import com.nm.utils.PromotionsHelper;
import com.nm.utils.SystemSpecs;

/**
 * This action is called when an item is added to cart. It will iterate thru the promtion array maintained by the global component PromotionsArray and look for an effective promotion. If it finds one
 * that is effective then is will see if it is a non promo code driven promotion and determine if free shipping or upgrade is qualified.
 * 
 * @author Todd Schultz
 * @version $Id: CheckShippingPromos.java 1.24.1.1 2009/02/17 10:24:14CST Robert Withers (nmrww3) Exp Robert Withers (nmrww3)(2009/02/18 14:24:56CST) $
 */

public class CheckShippingPromos extends ActionImpl implements IPromoAction {
  // -------------------------------------
  /** Class version string */
  public static final String CLASS_VERSION = "$Id: CheckShippingPromos.java 1.47.1.1.1.6 2014/03/28 15:14:44CDT nmavj1 Exp  $";
  
  /** Parameter for the location of the ordermanager */
  public static final String ORDERMANAGER_PATH = "/atg/commerce/order/OrderManager";
  public static final String SYSTEMSPECS_PATH = "/nm/utils/SystemSpecs";
  public static final String PROMOHELPER_PATH = "/nm/utils/PromotionsHelper";
  public static final String PROMOARRAY_PATH = "/nm/collections/PromotionsArray";
  public static final String SHOPPING_CART_PATH = "/atg/commerce/ShoppingCart";
  
  /** Parameter for which value to update */
  public static final String PROMO_CODE = "promo_code";// The CMOS required
  // 'FREEBASESHIPPING'
  // code
  public static final String FREEBASESHIPPING = "FREEBASESHIPPING";
  public static final String CMOSUPGRADE = "CMOSUPGRADE";
  public static final String CMOSFBS = "WEBFRSH";
  public static final String SHOPRUNNER = "SHOPRUNNER";
  public static final String WEBSL1 = "UPGRADEOVERNIGHT";
  public static final String WEBSL2 = "UPGRADE2NDDAY";
  private static final int PARENTHETICAL_NONE = 0;
  private static final int PARENTHETICAL_ALL = 1;
  private static final int PARENTHETICAL_BY_ITEM = 2;
  // public static final String COOKIE_NAME = "OTSHIP"; // One Time Ship Promo
  
  /** reference to the order manager object and order holder object */
  private NMOrderManager mOrderManager = null;
  protected CommerceItemManager mItemManager = null;
  protected OrderQueries mOrderQueries = null;
  
  private ComponentName mOrderHolderComponent = null;
  private SystemSpecs mSystemSpecs = null;
  
  private boolean logDebug;
  private boolean logError = true;
  
  PromotionsArray thePromos = null;
  PromotionsHelper mPromotionsHelper = null;
  private static final LocalizationUtils localizationUtils = (LocalizationUtils) Nucleus.getGlobalNucleus().resolveName("/nm/utils/LocalizationUtils");
  
  public static PromotionsArray getShippingPromosArray() {
    return (PromotionsArray) Nucleus.getGlobalNucleus().resolveName(PROMOARRAY_PATH);
  }
  
  @Override
  public void initialize(final Map pParameters) throws ProcessException {
    mOrderManager = (NMOrderManager) Nucleus.getGlobalNucleus().resolveName(ORDERMANAGER_PATH);
    
    if (mOrderManager == null) {
      throw new ProcessException(PromotionConstants.getStringResource(PromotionConstants.ORDER_MANAGER_NOT_FOUND));
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
    logDebug("CheckShippingPromos is Initializing ");
  }
  
  @Override
  protected void executeAction(final ProcessExecutionContext pContext) throws ProcessException {
    try {
      final Order order = getOrderToModify(pContext);
      evaluatePromo(order);
    } catch (final Exception e) {
      throw new ProcessException(ExceptionUtil.getExceptionInfo(e), e);
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
      logDebug("just hit checkShippingPromos");
      
      if (mOrderManager == null) {
        throw new PromoException(PromotionConstants.getStringResource(PromotionConstants.ORDER_MANAGER_NOT_FOUND));
      }
      
      boolean validateCountry = true;
      boolean validateLimitPromoUse = false;
      
      if (order instanceof NMOrderImpl) {
        final NMOrderImpl nmOrderImpl = (NMOrderImpl) order;
        activatedPromoCodes = nmOrderImpl.getActivatedPromoCode();
        validateCountry = nmOrderImpl.shouldValidateCountry();
        validateLimitPromoUse = nmOrderImpl.shouldValidateLimitPromoUse();
        
      }// end nmorderimpl check
      
      checkPromotions(order, activatedPromoCodes, validateCountry, validateLimitPromoUse);
      mOrderManager.updateOrder(order);
      handleStorePromoCodes(order);
      mOrderManager.updateOrder(order);
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
    
  }
  
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
      Nucleus.getGlobalNucleus().logError("checkShippingPromos: " + sin);
    }
  }
  
  private void logDebug(final String sin) {
    if (isLoggingDebug()) {
      Nucleus.getGlobalNucleus().logDebug("checkShippingPromos: " + sin);
    }
  }
  
  /*
   * This is the method that will set the type of upgrade in the activatedPromo code. The activated promo code is where the shipping algorithm is looking for the tag FREEBASESHIPPING to give free
   * shipping. If we award CMOS upgrade we pass this thru to CMOS so they can award the cheapest ship method to get it there in time. The key that is passed in here is the Repository Item key for the
   * promotion, we put this in the promo name for the order so we know which one was given in the remove action. The promoCodeOrKey is either the same key that was passsed in previously or the promo
   * code if the promotion uses a promo code. We put this in the activated promo code because this is where marketing is currently running reporting from. They query by promo code if it has one or
   * they use the key if not. The sendToCMos flag coming in indicates if we need to put the promoCode on the CI level so the loaders will send it to CMOS so they can take some action such as upgrade
   * or double incircle points used for type 99 generic promotion.
   */
  
  private void awardShipping(final Promotion promo, final String shippingOffer, final String theKey, final String promoCodeOrKey, final String theName, final Order order, final boolean sendToCmos)
          throws Exception {
    awardShipping(promo, shippingOffer, theKey, promoCodeOrKey, theName, order, sendToCmos, PARENTHETICAL_NONE);
  }
  
  private void awardShipping(final Promotion promo, final String shippingOffer, final String theKey, final String promoCodeOrKey, final String theName, final Order order, final boolean sendToCmos,
          final int freeParenthetical) throws Exception {
    try {
      // System.out.println("awardShipping: for "+shippingOffer+ " using
      // promocode or key "+promoCodeOrKey+ ", theKey "+theKey+ ", theName
      // "+theName+ ", sendToCmos "+sendToCmos+".");
      if (order instanceof NMOrderImpl) {
        final NMOrderImpl orderImpl = (NMOrderImpl) order;
        // MP Phase-II for shipping types 138 and 139 bypassing the setting of shipping offer and promocode to orderImpl.
        if (!DYNAMIC_FREE_SHIPPING.equalsIgnoreCase(promo.getType()) && !DYNAMIC_FREE_SHIPPING_UPGRADE.equalsIgnoreCase(promo.getType())) {
          orderImpl.setActivatedPromoCode(shippingOffer);
          orderImpl.setActivatedPromoCode(promoCodeOrKey);
        }
        orderImpl.setPromoName(theKey);
        orderImpl.addAwardedPromotion(promo);
        
        if (freeParenthetical == PARENTHETICAL_ALL) {
          mPromotionsHelper.applyParentheticalIndicator(true, orderImpl);
        } else if (freeParenthetical == PARENTHETICAL_BY_ITEM) {
          mPromotionsHelper.applyParentheticalByItem(orderImpl, theKey);
        } else {
          mPromotionsHelper.applyParentheticalIndicator(false, orderImpl);
        }
        
        if (!theKey.equals(promoCodeOrKey)) {
          orderImpl.setUserActivatedPromoCode(promoCodeOrKey);
        }
        // Don't send ShopRunner Promo to CMOS
        if (!promo.getType().equalsIgnoreCase("137")) {
          if (sendToCmos) {
            if (shippingOffer.trim().toUpperCase().equals(CMOSUPGRADE))
            // if(checkDisqualify(shippingOffer.trim().toUpperCase()))
            {
              mPromotionsHelper.addPromoToCI(promoCodeOrKey, orderImpl, true, theKey);
            } else {
              mPromotionsHelper.addPromoToCI(promoCodeOrKey, orderImpl, false, theKey);
            }
            // have to roll the promos to the line item level since
            // thats how cmos wants it
            
          } else {
            // send the key to CMOS since they are not getting the promo
            // code sent
            mPromotionsHelper.addKEYToCI(theKey, orderImpl);
          }
        }
        // cmos wants us to send a FS identifier on the LI so they can
        // tell if FS was awarded
        if (shippingOffer.trim().toUpperCase().equals(FREEBASESHIPPING)) {
          
          mPromotionsHelper.addPromoToCI(CMOSFBS, orderImpl, true, theKey);
        }
        logDebug("CheckShippingPromos-the order version-------------------->" + orderImpl.getVersion());
        mOrderManager.updateOrder(order);
        logDebug("CheckShippingPromos--the order version-after updateOrder------------------->" + orderImpl.getVersion());
      }
      
    } catch (final CommerceException ce) {
      throw new ProcessException(ce);
    } catch (final RepositoryException re) {
      throw new ProcessException(re);
    } catch (final Exception e) {
      throw new ProcessException(e);
    }
  }// end awardShipping
  
  private void checkPromotions(final Order order, final String activatedPromoCodes, final boolean validateCountry, final boolean validateLimitPromoUse) {
    // System.out.println("checkPromotions: for "+activatedPromoCodes+".");
    
    
    try {
      final Date mystartDate = Calendar.getInstance().getTime();
      // these are the variable declarations that will be initialized
      // below for the different types
      String theCode = new String();
      String theKey = new String();
      String theName = new String();
      String theCatalogs = new String();
      String theItems = new String();
      String theDept = new String();
      String theDepiction = new String();
      double promoDollarQual = 0;
      Promotion theShipPromo = null;
      DynamoHttpServletRequest req = null;
      boolean foundCookie = false;
      NMOrderImpl orderImpl = null;
      String lvl = null;
      NMProfile nmProfile =null;
      BrandSpecs brandSpecs=null;
      Object persObj =null;
      
      req = ServletUtil.getCurrentRequest();
      if(req!=null){
        nmProfile = (NMProfile) req.resolveName("/atg/userprofiling/Profile");
        brandSpecs = CommonComponentHelper.getBrandSpecs();
        persObj = nmProfile.getEdwCustData()!=null?nmProfile.getEdwCustData().getPersonalizedCustData():null;
      }
     
      List<String> persPromoCodes = null;
      if (persObj != null && brandSpecs.isEnableSitePersonalization()) {
        persPromoCodes = ((PersonalizedCustData) persObj).getPersonalizedPromos();
      }

      String freeShippingGroup = AbTestHelper.getAbTestValue(getRequest(), AbTestHelper.FREE_SHIP_KEY);
      // Clear the promo messages on all items present in the order. Messages will be added to an item when promotion
      // is applied and corresponding item meets the qualifying criteria.
      // mPromotionsHelper.clearPromoMessagesOnItems((NMOrderImpl) order);
      NMCheckoutTypeUtil nmCheckoutTypeUtil = CommonComponentHelper.getNMCheckoutTypeUtil();
      final Map<String, NMPromotion> promoArray = thePromos.getAllPromotions();
      Iterator<NMPromotion> promoIter = promoArray.values().iterator();
      while (promoIter.hasNext()) {
        theShipPromo = (Promotion) promoIter.next();
        theCode = theShipPromo.getPromoCodes().trim().toUpperCase();
        theKey = theShipPromo.getCode().trim().toUpperCase();
        theName = theShipPromo.getName().trim().toUpperCase();
        
        
        if (theShipPromo != null && theShipPromo.isPersonalizedFlag()) {
          if (!(theKey != null && (persPromoCodes != null && persPromoCodes.contains(theKey)))) {
            logDebug("Skipping the personalized promotion");
            continue;
          }
        }
        final Map<String, Boolean> checkoutTypes = theShipPromo.getFlgValidCheckoutTypes();
        if (!NmoUtils.isEmptyMap(checkoutTypes) && !(nmCheckoutTypeUtil.isPaymentTypeEligibleForPromotion((NMOrderImpl) order, checkoutTypes))) {
          continue;
        }
        // System.out.println("checkPromotions: for theShipPromo "+theShipPromo.getType()+".");
        if (mystartDate.after(theShipPromo.getStartDate()) && mystartDate.before(theShipPromo.getEndDate())) {
          foundCookie = false;

          // if the user has a cookie set to this promo code, then
          // it's a one time, and we won't accept it again
          logDebug("checkPromotions: going to check cookies for " + theShipPromo.getCode());
          if ((req != null) && theShipPromo.getOneTime().booleanValue()) {
            logDebug("checkPromotions: the request is not null ");
            // see if there's a matching cookie
            final String[] cookies = req.getCookieParameterValues(theShipPromo.getCode());
            if (cookies != null) {
              logDebug("checkPromotions: cookies are not null and need a match on -->" + theShipPromo.getCode());
              for (int c = 0; c < cookies.length; c++) {
                logDebug("----------looking at cookie------> " + cookies[c]);
                if (cookies[c].equals(theShipPromo.getCode())) {
                  // this promo code has already been used
                  logDebug("checkPromotions: one time use on shipping and backing off for promo code " + theShipPromo.getCode());
                  foundCookie = true;
                  break;
                }
              } // for
            } // if there are matching cookies
          } // if req != null and onetime = true
          
          if (foundCookie) {
            logDebug("foundcookie out of cookie loop skipping this promo -->" + theShipPromo.getCode());
            continue;
          }
          
          // shipping promos should never be granted to customers localized to a fiftyone supported country
          if (req != null) {
            final NMProfile profile = (NMProfile) ServletUtil.getCurrentRequest().resolveName("/atg/userprofiling/Profile");
            final String country = (String) profile.getPropertyValue(ProfileRepository.COUNTRY_PREFERENCE);
            final String promoType = theShipPromo.getType().trim();
            if (!"103".equals(promoType) && !"104".equals(promoType) && localizationUtils.isSupportedByFiftyOne(country)) {
              continue;
            }
          }
          
          // System.out.println("checkPromotions: apply theShipPromo
          // "+theShipPromo.getType()+".");
          logDebug("----------made it past cookie check and now checking order for promo------> " + theShipPromo.getCode());
          
          int promotionType = 0;
          
          try {
            promotionType = Integer.parseInt(theShipPromo.getType().trim());
          } catch (final NumberFormatException exception) {
            this.logError("Invalid promotion type: " + theShipPromo.getType());
          }
          if (freeShippingGroup != null) {
            freeShippingGroup = freeShippingGroup.toUpperCase().trim();
          }
          if ((freeShippingGroup != null) && freeShippingGroup.equals(theCode)) {
            int panentheticalType = PARENTHETICAL_NONE;
            if (theShipPromo.getAwardParenthetical().booleanValue()) {
              panentheticalType = PARENTHETICAL_ALL;
            }
            if (!validateCountry) {
              awardShipping(theShipPromo, FREEBASESHIPPING, theKey, theCode, theName, order, false, panentheticalType);
            } else if (mPromotionsHelper.countryQualifies(order, theKey)) {
              awardShipping(theShipPromo, FREEBASESHIPPING, theKey, theCode, theName, order, false, panentheticalType);
            }
          } else {
            switch (promotionType) {
              case 1: {
                int panentheticalType = PARENTHETICAL_NONE;
                if (theShipPromo.getAwardParenthetical().booleanValue()) {
                  panentheticalType = PARENTHETICAL_ALL;
                }
                if (mPromotionsHelper.dollarQualifies(order, theShipPromo.getDollarQualifier().trim(), 0)) {
                  if (!validateCountry) {
                    awardShipping(theShipPromo, FREEBASESHIPPING, theKey, theKey, theName, order, false, panentheticalType);
                  } else if (mPromotionsHelper.countryQualifies(order, theKey)) {
                    awardShipping(theShipPromo, FREEBASESHIPPING, theKey, theKey, theName, order, false, panentheticalType);
                  }
                } else if (theShipPromo.getSpendMoreFlag() && (!validateCountry || mPromotionsHelper.countryQualifies(order, theKey))) {
                  orderImpl = (NMOrderImpl) order;
                  orderImpl.addEncouragePromotion(theShipPromo);
                }
                break;
              }
              case 2: {
                int panentheticalType = PARENTHETICAL_NONE;
                if (theShipPromo.getAwardParenthetical().booleanValue()) {
                  panentheticalType = PARENTHETICAL_ALL;
                }
                
                final boolean promoCodeMatch = mPromotionsHelper.promoCodeMatch(order, theCode);
                final boolean dollarQualifies = mPromotionsHelper.dollarQualifies(order, theShipPromo.getDollarQualifier().trim(), 0);
                
                if (promoCodeMatch && dollarQualifies) {
                  if (!validateCountry) {
                    awardShipping(theShipPromo, FREEBASESHIPPING, theKey, theCode, theName, order, false, panentheticalType);
                  } else if (mPromotionsHelper.countryQualifies(order, theKey)) {
                    awardShipping(theShipPromo, FREEBASESHIPPING, theKey, theCode, theName, order, false, panentheticalType);
                  }
                } else if (theShipPromo.getSpendMoreFlag()) {
                  if (!validateCountry || mPromotionsHelper.countryQualifies(order, theKey)) {
                    orderImpl = (NMOrderImpl) order;
                    orderImpl.addEncouragePromotion(theShipPromo);
                  }
                }
                break;
              }
              case 3: {
                try {
                  if (mPromotionsHelper.promoCodeMatch(order, theCode)) {
                    awardShipping(theShipPromo, FREEBASESHIPPING, theKey, theCode, theName, order, false);
                  }
                } catch (final Exception ce) {
                  throw new ProcessException(ce);
                }
                break;
              }
              case 4: {
                try {
                  if (mPromotionsHelper.promoCodeMatch(order, theCode)) {
                    awardShipping(theShipPromo, CMOSUPGRADE, theKey, theCode, theName, order, true);
                  }
                } catch (final Exception ce) {
                  throw new ProcessException(ce);
                }
                break;
              }
              case 5: {
                try {
                  final boolean promoCodeMatch = mPromotionsHelper.promoCodeMatch(order, theCode);
                  final boolean dollarQualifies = mPromotionsHelper.dollarQualifies(order, theShipPromo.getDollarQualifier(), 0);
                  
                  if (promoCodeMatch && dollarQualifies) {
                    if (!validateCountry) {
                      awardShipping(theShipPromo, CMOSUPGRADE, theKey, theCode, theName, order, true);
                    } else if (mPromotionsHelper.countryQualifies(order, theKey)) {
                      awardShipping(theShipPromo, CMOSUPGRADE, theKey, theCode, theName, order, true);
                    }
                  } else if (theShipPromo.getSpendMoreFlag()) {
                    if (!validateCountry || mPromotionsHelper.countryQualifies(order, theKey)) {
                      orderImpl = (NMOrderImpl) order;
                      orderImpl.addEncouragePromotion(theShipPromo);
                    }
                  }
                } catch (final Exception ce) {
                  throw new ProcessException(ce);
                }
                break;
              }
              case 6: {
                theDept = theShipPromo.getDeptCodes().trim().toUpperCase();
                if (mPromotionsHelper.deptMatch(order, theDept, 0)) {
                  awardShipping(theShipPromo, FREEBASESHIPPING, theKey, theKey, theName, order, false);
                }
                break;
              }
              case 7: {
                theDept = theShipPromo.getDeptCodes().trim().toUpperCase();
                if (mPromotionsHelper.promoCodeMatch(order, theCode) && mPromotionsHelper.deptMatch(order, theDept, 0)) {
                  awardShipping(theShipPromo, FREEBASESHIPPING, theKey, theCode, theName, order, false);
                }
                break;
              }
              case 8: {
                theDept = theShipPromo.getDeptCodes().trim().toUpperCase();
                promoDollarQual = Double.parseDouble(theShipPromo.getDollarQualifier());
                if (mPromotionsHelper.deptDollarMatch(order, theDept, promoDollarQual, 0)) {
                  awardShipping(theShipPromo, FREEBASESHIPPING, theKey, theKey, theName, order, false);
                }
                break;
              }
              case 9: {
                theDept = theShipPromo.getDeptCodes().trim().toUpperCase();
                promoDollarQual = Double.parseDouble(theShipPromo.getDollarQualifier());
                
                if (mPromotionsHelper.promoCodeMatch(order, theCode) && mPromotionsHelper.deptDollarMatch(order, theDept, promoDollarQual, 0)) {
                  awardShipping(theShipPromo, FREEBASESHIPPING, theKey, theCode, theName, order, false);
                }
                break;
              }
              case 10: {
                theDepiction = theShipPromo.getDepiction();
                
                if (mPromotionsHelper.depictionMatch(order, theDepiction, 0)) {
                  awardShipping(theShipPromo, FREEBASESHIPPING, theKey, theKey, theName, order, false);
                }
                break;
              }
              case 11: {
                theDepiction = theShipPromo.getDepiction().trim().toUpperCase();
                if (mPromotionsHelper.promoCodeMatch(order, theCode) && mPromotionsHelper.depictionMatch(order, theDepiction, 0)) {
                  awardShipping(theShipPromo, FREEBASESHIPPING, theKey, theCode, theName, order, false);
                }
                break;
              }
              case 12: {
                theDepiction = theShipPromo.getDepiction();
                promoDollarQual = Double.parseDouble(theShipPromo.getDollarQualifier());
                
                if (mPromotionsHelper.depictionDollarMatch(order, theDepiction, promoDollarQual, 0)) {
                  awardShipping(theShipPromo, FREEBASESHIPPING, theKey, theKey, theName, order, false);
                }
                break;
              }
              case 13: {
                theDepiction = theShipPromo.getDepiction().trim().toUpperCase();
                promoDollarQual = Double.parseDouble(theShipPromo.getDollarQualifier());
                
                if (mPromotionsHelper.promoCodeMatch(order, theCode) && mPromotionsHelper.depictionDollarMatch(order, theDepiction, promoDollarQual, 0)) {
                  awardShipping(theShipPromo, FREEBASESHIPPING, theKey, theCode, theName, order, false);
                }
                break;
              }
              case 14: {
                promoDollarQual = Double.parseDouble(theShipPromo.getDollarQualifier());
                if (mPromotionsHelper.promoCodeMatch(order, theCode) && mPromotionsHelper.saleDollarMatch(order, promoDollarQual, true, 0)) {
                  awardShipping(theShipPromo, FREEBASESHIPPING, theKey, theCode, theName, order, false);
                }
                break;
              }
              case 15: {
                promoDollarQual = Double.parseDouble(theShipPromo.getDollarQualifier());
                if (mPromotionsHelper.promoCodeMatch(order, theCode) && mPromotionsHelper.saleDollarMatch(order, promoDollarQual, false, 0)) {
                  awardShipping(theShipPromo, FREEBASESHIPPING, theKey, theCode, theName, order, false);
                }
                break;
              }
              case 16: {
                if (mPromotionsHelper.promoCodeMatch(order, theCode) && mPromotionsHelper.saleMatch(order, true, 0)) {
                  awardShipping(theShipPromo, FREEBASESHIPPING, theKey, theCode, theName, order, false);
                }
                break;
              }
              case 17: {
                if (mPromotionsHelper.promoCodeMatch(order, theCode) && mPromotionsHelper.saleMatch(order, false, 0)) {
                  awardShipping(theShipPromo, FREEBASESHIPPING, theKey, theCode, theName, order, false);
                }
                break;
              }
              case 18: {
                theCatalogs = theShipPromo.getQualifyCatalog().trim().toUpperCase();
                promoDollarQual = Double.parseDouble(theShipPromo.getDollarQualifier());
                if (mPromotionsHelper.promoCodeMatch(order, theCode) && mPromotionsHelper.catalogDollarMatch(order, theCatalogs, promoDollarQual, 0)) {
                  awardShipping(theShipPromo, FREEBASESHIPPING, theKey, theCode, theName, order, false);
                }
                break;
              }
              case 19: {
                theCatalogs = theShipPromo.getQualifyCatalog().trim().toUpperCase();
                if (mPromotionsHelper.promoCodeMatch(order, theCode) && mPromotionsHelper.catalogMatch(order, theCatalogs, 0)) {
                  awardShipping(theShipPromo, FREEBASESHIPPING, theKey, theCode, theName, order, false);
                }
                break;
              }
              case 20: {
                theCatalogs = theShipPromo.getQualifyCatalog().trim().toUpperCase();
                promoDollarQual = Double.parseDouble(theShipPromo.getDollarQualifier());
                if (mPromotionsHelper.catalogDollarMatch(order, theCatalogs, promoDollarQual, 0)) {
                  awardShipping(theShipPromo, FREEBASESHIPPING, theKey, theKey, theName, order, false);
                }
                break;
              }
              case 21: {
                theItems = theShipPromo.getQualifyItems().trim().toUpperCase();
                if (mPromotionsHelper.itemMatch(order, theItems)) {
                  awardShipping(theShipPromo, FREEBASESHIPPING, theKey, theKey, theName, order, false);
                }
                break;
              }
              case 22: {
                theItems = theShipPromo.getQualifyItems().trim().toUpperCase();
                if (mPromotionsHelper.promoCodeMatch(order, theCode) && mPromotionsHelper.itemMatch(order, theItems)) {
                  awardShipping(theShipPromo, FREEBASESHIPPING, theKey, theCode, theName, order, false);
                }
                break;
              }
              case 23: {
                theItems = theShipPromo.getQualifyItems().trim().toUpperCase();
                promoDollarQual = Double.parseDouble(theShipPromo.getDollarQualifier());
                if (mPromotionsHelper.promoCodeMatch(order, theCode) && mPromotionsHelper.itemDollarMatch(order, theItems, promoDollarQual)) {
                  awardShipping(theShipPromo, FREEBASESHIPPING, theKey, theCode, theName, order, false);
                }
                break;
              }
              case 24: {
                theItems = theShipPromo.getQualifyItems().trim().toUpperCase();
                promoDollarQual = Double.parseDouble(theShipPromo.getDollarQualifier());
                if (mPromotionsHelper.itemDollarMatch(order, theItems, promoDollarQual)) {
                  awardShipping(theShipPromo, FREEBASESHIPPING, theKey, theKey, theName, order, false);
                }
                break;
              }
              case 25: {
                theDept = theShipPromo.getDeptCodes().trim().toUpperCase();
                if (mPromotionsHelper.promoCodeMatch(order, theCode) && mPromotionsHelper.deptMatch(order, theDept, 0)) {
                  awardShipping(theShipPromo, CMOSUPGRADE, theKey, theCode, theName, order, true);
                }
                break;
              }
              case 26: {
                theDept = theShipPromo.getDeptCodes().trim().toUpperCase();
                promoDollarQual = Double.parseDouble(theShipPromo.getDollarQualifier());
                
                if (mPromotionsHelper.promoCodeMatch(order, theCode) && mPromotionsHelper.deptDollarMatch(order, theDept, promoDollarQual, 0)) {
                  awardShipping(theShipPromo, CMOSUPGRADE, theKey, theCode, theName, order, true);
                }
                break;
              }
              case 27: {
                theDepiction = theShipPromo.getDepiction().trim().toUpperCase();
                if (mPromotionsHelper.promoCodeMatch(order, theCode) && mPromotionsHelper.depictionMatch(order, theDepiction, 0)) {
                  awardShipping(theShipPromo, CMOSUPGRADE, theKey, theCode, theName, order, true);
                }
                break;
              }
              case 28: {
                theDepiction = theShipPromo.getDepiction().trim().toUpperCase();
                promoDollarQual = Double.parseDouble(theShipPromo.getDollarQualifier());
                
                if (mPromotionsHelper.promoCodeMatch(order, theCode) && mPromotionsHelper.depictionDollarMatch(order, theDepiction, promoDollarQual, 0)) {
                  awardShipping(theShipPromo, CMOSUPGRADE, theKey, theCode, theName, order, true);
                }
                break;
              }
              case 29: {
                if (mPromotionsHelper.promoCodeMatch(order, theCode) && mPromotionsHelper.saleMatch(order, true, 0)) {
                  awardShipping(theShipPromo, CMOSUPGRADE, theKey, theCode, theName, order, true);
                }
                break;
              }
              case 30: {
                if (mPromotionsHelper.promoCodeMatch(order, theCode) && mPromotionsHelper.saleMatch(order, false, 0)) {
                  awardShipping(theShipPromo, CMOSUPGRADE, theKey, theCode, theName, order, true);
                }
                break;
              }
              case 31: {
                promoDollarQual = Double.parseDouble(theShipPromo.getDollarQualifier());
                if (mPromotionsHelper.promoCodeMatch(order, theCode) && mPromotionsHelper.saleDollarMatch(order, promoDollarQual, true, 0)) {
                  awardShipping(theShipPromo, CMOSUPGRADE, theKey, theCode, theName, order, true);
                }
                break;
              }// end type 31
              case 32: {
                promoDollarQual = Double.parseDouble(theShipPromo.getDollarQualifier());
                if (mPromotionsHelper.promoCodeMatch(order, theCode) && mPromotionsHelper.saleDollarMatch(order, promoDollarQual, false, 0)) {
                  awardShipping(theShipPromo, CMOSUPGRADE, theKey, theCode, theName, order, true);
                }
                break;
              }// end type 32
              case 33: {
                theCatalogs = theShipPromo.getQualifyCatalog().trim().toUpperCase();
                if (mPromotionsHelper.promoCodeMatch(order, theCode) && mPromotionsHelper.catalogMatch(order, theCatalogs, 0)) {
                  awardShipping(theShipPromo, CMOSUPGRADE, theKey, theCode, theName, order, true);
                }
                break;
              }// end type 33
              case 34: {
                theCatalogs = theShipPromo.getQualifyCatalog().trim().toUpperCase();
                promoDollarQual = Double.parseDouble(theShipPromo.getDollarQualifier());
                if (mPromotionsHelper.promoCodeMatch(order, theCode) && mPromotionsHelper.catalogDollarMatch(order, theCatalogs, promoDollarQual, 0)) {
                  awardShipping(theShipPromo, CMOSUPGRADE, theKey, theCode, theName, order, true);
                }
                break;
              }// end type 34
              case 35: {
                final Integer promoItemCount = new Integer(theShipPromo.getItemCount());
                if (mPromotionsHelper.promoCodeMatch(order, theCode) && mPromotionsHelper.itemCountMatch(order, promoItemCount.intValue())) {
                  awardShipping(theShipPromo, FREEBASESHIPPING, theKey, theCode, theName, order, false);
                }
                break;
              }// end type 35
              case 36: {
                theDept = theShipPromo.getDeptCodes().trim().toUpperCase();
                final Integer promoItemCount = new Integer(theShipPromo.getItemCount());
                if (mPromotionsHelper.promoCodeMatch(order, theCode) && mPromotionsHelper.deptMatch(order, theDept, promoItemCount.intValue())) {
                  awardShipping(theShipPromo, FREEBASESHIPPING, theKey, theCode, theName, order, false);
                }
                break;
              }// end type 36
              case 37: {
                theDept = theShipPromo.getDeptCodes().trim().toUpperCase();
                final Integer promoItemCount = new Integer(theShipPromo.getItemCount());
                promoDollarQual = Double.parseDouble(theShipPromo.getDollarQualifier());
                if (mPromotionsHelper.promoCodeMatch(order, theCode) && mPromotionsHelper.deptDollarMatch(order, theDept, promoDollarQual, promoItemCount.intValue())) {
                  awardShipping(theShipPromo, FREEBASESHIPPING, theKey, theCode, theName, order, false);
                }
                break;
              }// end type 37
              case 38: {
                theDepiction = theShipPromo.getDepiction();
                final Integer promoItemCount = new Integer(theShipPromo.getItemCount());
                if (mPromotionsHelper.promoCodeMatch(order, theCode) && mPromotionsHelper.depictionMatch(order, theDepiction, promoItemCount.intValue())) {
                  awardShipping(theShipPromo, FREEBASESHIPPING, theKey, theCode, theName, order, false);
                }
                break;
              }// end type 38
              case 39: {
                theDepiction = theShipPromo.getDepiction();
                final Integer promoItemCount = new Integer(theShipPromo.getItemCount());
                promoDollarQual = Double.parseDouble(theShipPromo.getDollarQualifier());
                if (mPromotionsHelper.promoCodeMatch(order, theCode) && mPromotionsHelper.depictionDollarMatch(order, theDepiction, promoDollarQual, promoItemCount.intValue())) {
                  awardShipping(theShipPromo, FREEBASESHIPPING, theKey, theCode, theName, order, false);
                }
                break;
              }// end type 39
              case 40: {
                final Integer promoItemCount = new Integer(theShipPromo.getItemCount());
                if (mPromotionsHelper.promoCodeMatch(order, theCode) && mPromotionsHelper.saleMatch(order, true, promoItemCount.intValue())) {
                  awardShipping(theShipPromo, FREEBASESHIPPING, theKey, theCode, theName, order, false);
                }
                break;
              }// end type 40
              case 41: {
                final Integer promoItemCount = new Integer(theShipPromo.getItemCount());
                if (mPromotionsHelper.promoCodeMatch(order, theCode) && mPromotionsHelper.saleMatch(order, false, promoItemCount.intValue())) {
                  awardShipping(theShipPromo, FREEBASESHIPPING, theKey, theCode, theName, order, false);
                }
                break;
              }// end type 41
              case 42: {
                promoDollarQual = Double.parseDouble(theShipPromo.getDollarQualifier());
                final Integer promoItemCount = new Integer(theShipPromo.getItemCount());
                if (mPromotionsHelper.promoCodeMatch(order, theCode) && mPromotionsHelper.saleDollarMatch(order, promoDollarQual, true, promoItemCount.intValue())) {
                  awardShipping(theShipPromo, FREEBASESHIPPING, theKey, theCode, theName, order, false);
                }
                break;
              }// end type 42
              case 43: {
                promoDollarQual = Double.parseDouble(theShipPromo.getDollarQualifier());
                final Integer promoItemCount = new Integer(theShipPromo.getItemCount());
                if (mPromotionsHelper.promoCodeMatch(order, theCode) && mPromotionsHelper.saleDollarMatch(order, promoDollarQual, false, promoItemCount.intValue())) {
                  awardShipping(theShipPromo, FREEBASESHIPPING, theKey, theCode, theName, order, false);
                }
                break;
              }// end type 43
              case 44: {
                theCatalogs = theShipPromo.getQualifyCatalog().trim().toUpperCase();
                final Integer promoItemCount = new Integer(theShipPromo.getItemCount());
                if (mPromotionsHelper.promoCodeMatch(order, theCode) && mPromotionsHelper.catalogMatch(order, theCatalogs, promoItemCount.intValue())) {
                  awardShipping(theShipPromo, FREEBASESHIPPING, theKey, theCode, theName, order, false);
                }
                break;
              }// end type 44
              case 45: {
                promoDollarQual = Double.parseDouble(theShipPromo.getDollarQualifier());
                theCatalogs = theShipPromo.getQualifyCatalog().trim().toUpperCase();
                final Integer promoItemCount = new Integer(theShipPromo.getItemCount());
                if (mPromotionsHelper.promoCodeMatch(order, theCode) && mPromotionsHelper.catalogDollarMatch(order, theCatalogs, promoDollarQual, promoItemCount.intValue())) {
                  awardShipping(theShipPromo, FREEBASESHIPPING, theKey, theCode, theName, order, false);
                }
                break;
              }// end type 45
              case 46: {
                if (mPromotionsHelper.saleMatch(order, true, 0)) {
                  awardShipping(theShipPromo, FREEBASESHIPPING, theKey, theKey, theName, order, false);
                }
                break;
              }
              case 47: {
                if (mPromotionsHelper.saleMatch(order, false, 0)) {
                  awardShipping(theShipPromo, FREEBASESHIPPING, theKey, theKey, theName, order, false);
                }
                break;
              }// end type 47
              case 48: {
                promoDollarQual = Double.parseDouble(theShipPromo.getDollarQualifier());
                if (mPromotionsHelper.saleDollarMatch(order, promoDollarQual, true, 0)) {
                  awardShipping(theShipPromo, FREEBASESHIPPING, theKey, theKey, theName, order, false);
                }
                break;
              }// end type 48
              case 49: {
                promoDollarQual = Double.parseDouble(theShipPromo.getDollarQualifier());
                if (mPromotionsHelper.saleDollarMatch(order, promoDollarQual, false, 0)) {
                  awardShipping(theShipPromo, FREEBASESHIPPING, theKey, theKey, theName, order, false);
                }
                break;
              }// end type 49
              case 50: {
                theCatalogs = theShipPromo.getQualifyCatalog().trim().toUpperCase();
                if (mPromotionsHelper.catalogMatch(order, theCatalogs, 0)) {
                  awardShipping(theShipPromo, FREEBASESHIPPING, theKey, theKey, theName, order, false);
                }
                break;
              }// end type 50
              case 51: {
                final Integer promoItemCount = new Integer(theShipPromo.getItemCount());
                if (mPromotionsHelper.dollarQualifies(order, theShipPromo.getDollarQualifier().trim(), promoItemCount.intValue())) {
                  awardShipping(theShipPromo, FREEBASESHIPPING, theKey, theKey, theName, order, false);
                }
                break;
              }// end type 51
              case 52: {
                final Integer promoItemCount = new Integer(theShipPromo.getItemCount());
                if (mPromotionsHelper.promoCodeMatch(order, theCode) && mPromotionsHelper.dollarQualifies(order, theShipPromo.getDollarQualifier().trim(), promoItemCount.intValue())) {
                  awardShipping(theShipPromo, FREEBASESHIPPING, theKey, theCode, theName, order, false);
                }
                break;
              }// end type 52
              case 53: {
                final Integer promoItemCount = new Integer(theShipPromo.getItemCount());
                theDept = theShipPromo.getDeptCodes().trim().toUpperCase();
                if (mPromotionsHelper.deptMatch(order, theDept, promoItemCount.intValue())) {
                  awardShipping(theShipPromo, FREEBASESHIPPING, theKey, theKey, theName, order, false);
                }
                break;
              }// end type 53
              case 54: {
                theDepiction = theShipPromo.getDepiction();
                final Integer promoItemCount = new Integer(theShipPromo.getItemCount());
                if (mPromotionsHelper.depictionMatch(order, theDepiction, promoItemCount.intValue())) {
                  awardShipping(theShipPromo, FREEBASESHIPPING, theKey, theKey, theName, order, false);
                }
                break;
              }// end type 54
              case 55: {
                final Integer promoItemCount = new Integer(theShipPromo.getItemCount());
                if (mPromotionsHelper.saleMatch(order, true, promoItemCount.intValue())) {
                  awardShipping(theShipPromo, FREEBASESHIPPING, theKey, theKey, theName, order, false);
                }
                break;
              }// end type 55
              case 56: {
                final Integer promoItemCount = new Integer(theShipPromo.getItemCount());
                if (mPromotionsHelper.saleMatch(order, false, promoItemCount.intValue())) {
                  awardShipping(theShipPromo, FREEBASESHIPPING, theKey, theKey, theName, order, false);
                }
                break;
              }// end type 56
              case 57: {
                promoDollarQual = Double.parseDouble(theShipPromo.getDollarQualifier());
                final Integer promoItemCount = new Integer(theShipPromo.getItemCount());
                if (mPromotionsHelper.saleDollarMatch(order, promoDollarQual, true, promoItemCount.intValue())) {
                  awardShipping(theShipPromo, FREEBASESHIPPING, theKey, theKey, theName, order, false);
                }
                break;
              }// end type 57
              case 58: {
                promoDollarQual = Double.parseDouble(theShipPromo.getDollarQualifier());
                final Integer promoItemCount = new Integer(theShipPromo.getItemCount());
                if (mPromotionsHelper.saleDollarMatch(order, promoDollarQual, false, promoItemCount.intValue())) {
                  awardShipping(theShipPromo, FREEBASESHIPPING, theKey, theKey, theName, order, false);
                }
                break;
              }// end type 58
              case 59: {
                theCatalogs = theShipPromo.getQualifyCatalog().trim().toUpperCase();
                final Integer promoItemCount = new Integer(theShipPromo.getItemCount());
                if (mPromotionsHelper.catalogMatch(order, theCatalogs, promoItemCount.intValue())) {
                  awardShipping(theShipPromo, FREEBASESHIPPING, theKey, theKey, theName, order, false);
                }
                break;
              }// end type 59
              case 60: {
                theCatalogs = theShipPromo.getQualifyCatalog().trim().toUpperCase();
                final Integer promoItemCount = new Integer(theShipPromo.getItemCount());
                promoDollarQual = Double.parseDouble(theShipPromo.getDollarQualifier());
                if (mPromotionsHelper.catalogDollarMatch(order, theCatalogs, promoDollarQual, promoItemCount.intValue())) {
                  awardShipping(theShipPromo, FREEBASESHIPPING, theKey, theKey, theName, order, false);
                }
                break;
              }// end type 60
              case 61: {
                if (mPromotionsHelper.promoCodeMatch(order, theCode) && mPromotionsHelper.dollarQualifies(order, theShipPromo.getDollarQualifier().trim(), 0)) {
                  awardShipping(theShipPromo, FREEBASESHIPPING, theKey, theCode, theName, order, false);
                  awardShipping(theShipPromo, WEBSL1, theKey, theCode, theName, order, false);
                }
                break;
              }// end type 61
              case 62: {
                if (mPromotionsHelper.promoCodeMatch(order, theCode) && mPromotionsHelper.dollarQualifies(order, theShipPromo.getDollarQualifier().trim(), 0)) {
                  awardShipping(theShipPromo, FREEBASESHIPPING, theKey, theCode, theName, order, false);
                  awardShipping(theShipPromo, WEBSL2, theKey, theCode, theName, order, false);
                }
                break;
              }// end type 62
              case 63: {
                if (mPromotionsHelper.dollarQualifies(order, theShipPromo.getDollarQualifier().trim(), 0)) {
                  awardShipping(theShipPromo, FREEBASESHIPPING, theKey, theCode, theName, order, false);
                  awardShipping(theShipPromo, WEBSL1, theKey, theKey, theName, order, false);
                }
                break;
              }// end type 63
              case 64: {
                if (mPromotionsHelper.dollarQualifies(order, theShipPromo.getDollarQualifier().trim(), 0)) {
                  awardShipping(theShipPromo, FREEBASESHIPPING, theKey, theCode, theName, order, false);
                  awardShipping(theShipPromo, WEBSL2, theKey, theKey, theName, order, false);
                }
                break;
              }// end type 64
              case 65: {
                if (mPromotionsHelper.promoCodeMatch(order, theCode) && mPromotionsHelper.dollarQualifies(order, theShipPromo.getDollarQualifier().trim(), 0)) {
                  if (!validateCountry) {
                    awardShipping(theShipPromo, FREEBASESHIPPING, theKey, theCode, theName, order, false);
                    awardShipping(theShipPromo, CMOSUPGRADE, theKey, theCode, theName, order, true);
                  } else if (mPromotionsHelper.countryQualifies(order, theKey)) {
                    awardShipping(theShipPromo, FREEBASESHIPPING, theKey, theCode, theName, order, false);
                    awardShipping(theShipPromo, CMOSUPGRADE, theKey, theCode, theName, order, true);
                  }
                }
                break;
              }// end type 65
              case 66: {
                if (mPromotionsHelper.promoCodeMatch(order, theCode) && mPromotionsHelper.dollarQualifies(order, theShipPromo.getDollarQualifier().trim(), 0)) {
                  awardShipping(theShipPromo, WEBSL2, theKey, theCode, theName, order, false);
                }
                break;
              }// end type 66
              case 67: {
                if (mPromotionsHelper.promoCodeMatch(order, theCode) && mPromotionsHelper.dollarQualifies(order, theShipPromo.getDollarQualifier().trim(), 0)) {
                  awardShipping(theShipPromo, WEBSL1, theKey, theCode, theName, order, false);
                }
                break;
              }// end type 67
              case 68: {
                if (mPromotionsHelper.dollarQualifies(order, theShipPromo.getDollarQualifier().trim(), 0)) {
                  awardShipping(theShipPromo, WEBSL2, theKey, theKey, theName, order, false);
                }
              }// end type 68
              case 69: {
                if (mPromotionsHelper.dollarQualifies(order, theShipPromo.getDollarQualifier().trim(), 0)) {
                  awardShipping(theShipPromo, WEBSL1, theKey, theKey, theName, order, false);
                }
                break;
              }// end type 69
              
              case 99: {
                try {
                  if (mPromotionsHelper.promoCodeMatch(order, theCode)) {
                    awardShipping(theShipPromo, "", theKey, theCode, theName, order, true);
                  }
                } catch (final Exception ce) {
                  throw new ProcessException(ce);
                }
                break;
              }// end type 99
              case 100: {
                try {
                  if (mPromotionsHelper.dollarQualifies(order, theShipPromo.getDollarQualifier().trim(), 0)) {
                    awardShipping(theShipPromo, "", theKey, theKey, theName, order, false);
                  }
                } catch (final Exception ce) {
                  throw new ProcessException(ce);
                }
                break;
              }// end type 100
              case 101: {
                try {
                  if (mPromotionsHelper.promoCodeMatch(order, theCode) && mPromotionsHelper.dollarQualifies(order, theShipPromo.getDollarQualifier().trim(), 0)) {
                    awardShipping(theShipPromo, "", theKey, theCode, theName, order, false);
                  }
                } catch (final Exception ce) {
                  throw new ProcessException(ce);
                }
                break;
              }// end type 101
              case 102: {
                try {
                  if (mPromotionsHelper.orderHasProd(order, theKey)) {
                    awardShipping(theShipPromo, "", theKey, theKey, theName, order, false);
                  }
                } catch (final Exception ce) {
                  throw new ProcessException(ce);
                }
                break;
              }// end type 102
              case 103: {
                try {
                  if (mPromotionsHelper.validateCountryForShipping(theShipPromo) && mPromotionsHelper.orderHasProdDollar(order, theKey, theShipPromo.getDollarQualifier().trim())) {
                    awardShipping(theShipPromo, "", theKey, theKey, theName, order, false);
                  }
                } catch (final Exception ce) {
                  throw new ProcessException(ce);
                }
                break;
              }// end type 103
              case 104: {
                try {
                  if (mPromotionsHelper.validateCountryForShipping(theShipPromo) && mPromotionsHelper.promoCodeMatch(order, theCode)
                          && mPromotionsHelper.orderHasProdDollar(order, theKey, theShipPromo.getDollarQualifier().trim())) {
                    awardShipping(theShipPromo, "", theKey, theCode, theName, order, false);
                    mPromotionsHelper.addActivePromoCodesToItems((NMOrderImpl) order, theKey, theCode);
                  }
                } catch (final Exception ce) {
                  throw new ProcessException(ce);
                }
                break;
              }// end type 104
              case 105: {
                try {
                  if (mPromotionsHelper.dollarQualifies(order, theShipPromo.getDollarQualifier().trim(), 0)) {
                    if (!validateCountry) {
                      awardShipping(theShipPromo, CMOSUPGRADE, theKey, theKey, theName, order, true);
                    } else if (mPromotionsHelper.countryQualifies(order, theKey)) {
                      awardShipping(theShipPromo, CMOSUPGRADE, theKey, theKey, theName, order, true);
                    }
                  } else if (theShipPromo.getSpendMoreFlag()) {
                    if (!validateCountry || mPromotionsHelper.countryQualifies(order, theKey)) {
                      orderImpl = (NMOrderImpl) order;
                      orderImpl.addEncouragePromotion(theShipPromo);
                    }
                  }
                } catch (final Exception ce) {
                  throw new ProcessException(ce);
                }
                break;
              }// end type 105
              case 106: {
                try {
                  if (mPromotionsHelper.dollarQualifies(order, theShipPromo.getDollarQualifier().trim(), 0)) {
                    if (!validateCountry) {
                      awardShipping(theShipPromo, FREEBASESHIPPING, theKey, theKey, theName, order, false);
                      awardShipping(theShipPromo, CMOSUPGRADE, theKey, theKey, theName, order, true);
                    } else if (mPromotionsHelper.countryQualifies(order, theKey)) {
                      awardShipping(theShipPromo, FREEBASESHIPPING, theKey, theKey, theName, order, false);
                      awardShipping(theShipPromo, CMOSUPGRADE, theKey, theKey, theName, order, true);
                    }
                  }
                } catch (final Exception ce) {
                  throw new ProcessException(ce);
                }
                break;
              }// end type 106
              case 107: {
                try {
                  if (ShoppingCartHandler.isInCircleAccount(CheckoutComponents.getProfile(ServletUtil.getCurrentRequest()), order)) {
                    awardShipping(theShipPromo, FREEBASESHIPPING, theKey, theKey, theName, order, false);
                  }
                } catch (final Exception ce) {
                  throw new ProcessException(ce);
                }
                break;
              }// end type 107
              case 108: {
                try {
                  if (ShoppingCartHandler.isInCircleAccount(CheckoutComponents.getProfile(ServletUtil.getCurrentRequest()), order)) {
                    awardShipping(theShipPromo, CMOSUPGRADE, theKey, theKey, theName, order, true);
                  }
                } catch (final Exception ce) {
                  throw new ProcessException(ce);
                }
                break;
              }// end type 108
              case 109: {
                try {
                  if (ShoppingCartHandler.isInCircleAccount(CheckoutComponents.getProfile(ServletUtil.getCurrentRequest()), order)) {
                    awardShipping(theShipPromo, FREEBASESHIPPING, theKey, theKey, theName, order, false);
                    awardShipping(theShipPromo, CMOSUPGRADE, theKey, theKey, theName, order, true);
                  }
                } catch (final Exception ce) {
                  throw new ProcessException(ce);
                }
                break;
              }// end type 109
              case 110: {
                int panentheticalType = PARENTHETICAL_NONE;
                if (theShipPromo.getAwardParenthetical().booleanValue()) {
                  panentheticalType = PARENTHETICAL_BY_ITEM;
                }
                if (mPromotionsHelper.orderHasProdDollar(order, theKey, theShipPromo.getDollarQualifier().trim()) && mPromotionsHelper.promoCodeMatch(order, theCode)
                        && mPromotionsHelper.orderHasProd(order, theKey)) {
                  if (!validateCountry) {
                    
                    final NMOrderImpl nmOrderImpl = (NMOrderImpl) order;
                    if (!nmOrderImpl.getUsedLimitPromoMap().contains(theKey)) {
                      awardShipping(theShipPromo, FREEBASESHIPPING, theKey, theCode, theName, order, false, panentheticalType);
                    }
                    
                  } else if (mPromotionsHelper.countryQualifies(order, theKey)) {
                    final NMOrderImpl nmOrderImpl = (NMOrderImpl) order;
                    if (!nmOrderImpl.getUsedLimitPromoMap().contains(theKey)) {
                      awardShipping(theShipPromo, FREEBASESHIPPING, theKey, theCode, theName, order, false, panentheticalType);
                    }
                    
                  }
                }
                
                mOrderManager.updateOrder(order);
                break;
              }// end type 110
              case 111: {
                int panentheticalType = PARENTHETICAL_NONE;
                if (theShipPromo.getAwardParenthetical().booleanValue()) {
                  panentheticalType = PARENTHETICAL_BY_ITEM;
                }
                if (mPromotionsHelper.orderHasProdDollar(order, theKey, theShipPromo.getDollarQualifier().trim()) && mPromotionsHelper.orderHasProd(order, theKey)) {
                  if (!validateCountry) {
                    
                    final NMOrderImpl nmOrderImpl = (NMOrderImpl) order;
                    if (!nmOrderImpl.getUsedLimitPromoMap().contains(theKey)) {
                      awardShipping(theShipPromo, FREEBASESHIPPING, theKey, theKey, theName, order, false, panentheticalType);
                    }
                    
                  } else if (mPromotionsHelper.countryQualifies(order, theKey)) {
                    
                    final NMOrderImpl nmOrderImpl = (NMOrderImpl) order;
                    if (!nmOrderImpl.getUsedLimitPromoMap().contains(theKey)) {
                      awardShipping(theShipPromo, FREEBASESHIPPING, theKey, theKey, theName, order, false, panentheticalType);
                    }
                    
                  }
                }
                break;
              }// end type 111
              case 112: {
                if (mPromotionsHelper.orderHasProdDollar(order, theKey, theShipPromo.getDollarQualifier().trim()) && mPromotionsHelper.promoCodeMatch(order, theCode)
                        && mPromotionsHelper.orderHasProd(order, theKey)) {
                  if (!validateCountry) {
                    awardShipping(theShipPromo, CMOSUPGRADE, theKey, theCode, theName, order, true);
                  } else if (mPromotionsHelper.countryQualifies(order, theKey)) {
                    awardShipping(theShipPromo, CMOSUPGRADE, theKey, theCode, theName, order, true);
                  }
                }
                break;
              }// end type 112
              case 113: {
                if (mPromotionsHelper.orderHasProdDollar(order, theKey, theShipPromo.getDollarQualifier().trim()) && mPromotionsHelper.orderHasProd(order, theKey)) {
                  if (!validateCountry) {
                    awardShipping(theShipPromo, CMOSUPGRADE, theKey, theKey, theName, order, true);
                  } else if (mPromotionsHelper.countryQualifies(order, theKey)) {
                    awardShipping(theShipPromo, CMOSUPGRADE, theKey, theKey, theName, order, true);
                  }
                }
                break;
              }// end type 113
              case 114: {
                if (mPromotionsHelper.orderHasProdDollar(order, theKey, theShipPromo.getDollarQualifier().trim()) && mPromotionsHelper.promoCodeMatch(order, theCode)
                        && mPromotionsHelper.orderHasProd(order, theKey)) {
                  if (!validateCountry) {
                    awardShipping(theShipPromo, FREEBASESHIPPING, theKey, theCode, theName, order, false);
                    awardShipping(theShipPromo, CMOSUPGRADE, theKey, theCode, theName, order, true);
                  } else if (mPromotionsHelper.countryQualifies(order, theKey)) {
                    awardShipping(theShipPromo, FREEBASESHIPPING, theKey, theCode, theName, order, false);
                    awardShipping(theShipPromo, CMOSUPGRADE, theKey, theCode, theName, order, true);
                  }
                }
                break;
              }// end type 114
              case 115: {
                if (mPromotionsHelper.orderHasProdDollar(order, theKey, theShipPromo.getDollarQualifier().trim()) && mPromotionsHelper.orderHasProd(order, theKey)) {
                  if (!validateCountry) {
                    awardShipping(theShipPromo, FREEBASESHIPPING, theKey, theKey, theName, order, false);
                    awardShipping(theShipPromo, CMOSUPGRADE, theKey, theKey, theName, order, true);
                  } else if (mPromotionsHelper.countryQualifies(order, theKey)) {
                    awardShipping(theShipPromo, FREEBASESHIPPING, theKey, theKey, theName, order, false);
                    awardShipping(theShipPromo, CMOSUPGRADE, theKey, theKey, theName, order, true);
                  }
                }
                break;
              }// end type 115
              case 116: {
                final Integer promoItemCount = new Integer(theShipPromo.getItemCount());
                if (mPromotionsHelper.orderHasProdDollar(order, theKey, theShipPromo.getDollarQualifier().trim()) && mPromotionsHelper.promoCodeMatch(order, theCode)
                        && mPromotionsHelper.orderHasProdCount(order, theKey, promoItemCount.intValue())) {
                  if (!validateCountry) {
                    awardShipping(theShipPromo, FREEBASESHIPPING, theKey, theCode, theName, order, false);
                  } else if (mPromotionsHelper.countryQualifies(order, theKey)) {
                    awardShipping(theShipPromo, FREEBASESHIPPING, theKey, theCode, theName, order, false);
                  }
                }
                break;
              }// end type 116
              case 117: {
                final Integer promoItemCount = new Integer(theShipPromo.getItemCount());
                if (mPromotionsHelper.orderHasProdDollar(order, theKey, theShipPromo.getDollarQualifier().trim()) && mPromotionsHelper.orderHasProdCount(order, theKey, promoItemCount.intValue())) {
                  if (!validateCountry) {
                    awardShipping(theShipPromo, FREEBASESHIPPING, theKey, theKey, theName, order, false);
                  } else if (mPromotionsHelper.countryQualifies(order, theKey)) {
                    awardShipping(theShipPromo, FREEBASESHIPPING, theKey, theKey, theName, order, false);
                  }
                }
                break;
              }// end type 117
              case 118: {
                final Integer promoItemCount = new Integer(theShipPromo.getItemCount());
                if (mPromotionsHelper.orderHasProdDollar(order, theKey, theShipPromo.getDollarQualifier().trim()) && mPromotionsHelper.promoCodeMatch(order, theCode)
                        && mPromotionsHelper.orderHasProdCount(order, theKey, promoItemCount.intValue())) {
                  if (!validateCountry) {
                    awardShipping(theShipPromo, CMOSUPGRADE, theKey, theCode, theName, order, true);
                  } else if (mPromotionsHelper.countryQualifies(order, theKey)) {
                    awardShipping(theShipPromo, CMOSUPGRADE, theKey, theCode, theName, order, true);
                  }
                }
                break;
              }// end type 118
              case 119: {
                final Integer promoItemCount = new Integer(theShipPromo.getItemCount());
                if (mPromotionsHelper.orderHasProdDollar(order, theKey, theShipPromo.getDollarQualifier().trim()) && mPromotionsHelper.orderHasProdCount(order, theKey, promoItemCount.intValue())) {
                  if (!validateCountry) {
                    awardShipping(theShipPromo, CMOSUPGRADE, theKey, theKey, theName, order, true);
                  } else if (mPromotionsHelper.countryQualifies(order, theKey)) {
                    awardShipping(theShipPromo, CMOSUPGRADE, theKey, theKey, theName, order, true);
                  }
                }
                break;
              }// end type 119
              case 120: {
                final Integer promoItemCount = new Integer(theShipPromo.getItemCount());
                if (mPromotionsHelper.orderHasProdDollar(order, theKey, theShipPromo.getDollarQualifier().trim()) && mPromotionsHelper.promoCodeMatch(order, theCode)
                        && mPromotionsHelper.orderHasProdCount(order, theKey, promoItemCount.intValue())) {
                  if (!validateCountry) {
                    awardShipping(theShipPromo, FREEBASESHIPPING, theKey, theCode, theName, order, false);
                    awardShipping(theShipPromo, CMOSUPGRADE, theKey, theCode, theName, order, true);
                  } else if (mPromotionsHelper.countryQualifies(order, theKey)) {
                    awardShipping(theShipPromo, FREEBASESHIPPING, theKey, theCode, theName, order, false);
                    awardShipping(theShipPromo, CMOSUPGRADE, theKey, theCode, theName, order, true);
                  }
                }
                break;
              }// end type 120
              case 121: {
                final Integer promoItemCount = new Integer(theShipPromo.getItemCount());
                if (mPromotionsHelper.orderHasProdDollar(order, theKey, theShipPromo.getDollarQualifier().trim()) && mPromotionsHelper.orderHasProdCount(order, theKey, promoItemCount.intValue())) {
                  if (!validateCountry) {
                    awardShipping(theShipPromo, FREEBASESHIPPING, theKey, theKey, theName, order, false);
                    awardShipping(theShipPromo, CMOSUPGRADE, theKey, theKey, theName, order, true);
                  } else if (mPromotionsHelper.countryQualifies(order, theKey)) {
                    awardShipping(theShipPromo, FREEBASESHIPPING, theKey, theKey, theName, order, false);
                    awardShipping(theShipPromo, CMOSUPGRADE, theKey, theKey, theName, order, true);
                  }
                }
                break;
              }// end type 121
              case 122: {
                
                if (mPromotionsHelper.dollarQualifies(order, theShipPromo.getDollarQualifier().trim(), 0)
                        && mPromotionsHelper.isValidCreditCard(CheckoutComponents.getProfile(ServletUtil.getCurrentRequest()), order, theKey)) {
                  if (!validateCountry) {
                    awardShipping(theShipPromo, FREEBASESHIPPING, theKey, theKey, theName, order, false);
                  } else if (mPromotionsHelper.countryQualifies(order, theKey)) {
                    awardShipping(theShipPromo, FREEBASESHIPPING, theKey, theKey, theName, order, false);
                  }
                  
                }
                break;
              }// end type 122
              case 123: {
                if (mPromotionsHelper.promoCodeMatch(order, theCode) && mPromotionsHelper.dollarQualifies(order, theShipPromo.getDollarQualifier().trim(), 0)) {
                  
                  orderImpl = (NMOrderImpl) order;
                  final boolean validateTenderType = ((Boolean) orderImpl.getPropertyValue("validateTenderType")).booleanValue();
                  if (!validateTenderType) {
                    if (!validateCountry) {
                      awardShipping(theShipPromo, FREEBASESHIPPING, theKey, theCode, theName, order, false);
                    } else if (mPromotionsHelper.countryQualifies(order, theKey)) {
                      awardShipping(theShipPromo, FREEBASESHIPPING, theKey, theCode, theName, order, false);
                    }
                  } else if (mPromotionsHelper.isValidCreditCard(CheckoutComponents.getProfile(ServletUtil.getCurrentRequest()), order, theKey)) {
                    if (!validateCountry) {
                      awardShipping(theShipPromo, FREEBASESHIPPING, theKey, theCode, theName, order, false);
                    } else if (mPromotionsHelper.countryQualifies(order, theKey)) {
                      awardShipping(theShipPromo, FREEBASESHIPPING, theKey, theCode, theName, order, false);
                    }
                  }
                }
                break;
              }// end type 123
              case 124: {
                if (mPromotionsHelper.dollarQualifies(order, theShipPromo.getDollarQualifier().trim(), 0)
                        && mPromotionsHelper.isValidCreditCard(CheckoutComponents.getProfile(ServletUtil.getCurrentRequest()), order, theKey)) {
                  if (!validateCountry) {
                    awardShipping(theShipPromo, CMOSUPGRADE, theKey, theKey, theName, order, false);
                  } else if (mPromotionsHelper.countryQualifies(order, theKey)) {
                    awardShipping(theShipPromo, CMOSUPGRADE, theKey, theKey, theName, order, false);
                  }
                }
                break;
              }// end type 124
              case 125: {
                if (mPromotionsHelper.promoCodeMatch(order, theCode) && mPromotionsHelper.dollarQualifies(order, theShipPromo.getDollarQualifier().trim(), 0)) {
                  orderImpl = (NMOrderImpl) order;
                  final boolean validateTenderType = ((Boolean) orderImpl.getPropertyValue("validateTenderType")).booleanValue();
                  if (!validateTenderType) {
                    if (!validateCountry) {
                      awardShipping(theShipPromo, CMOSUPGRADE, theKey, theCode, theName, order, false);
                    } else if (mPromotionsHelper.countryQualifies(order, theKey)) {
                      awardShipping(theShipPromo, CMOSUPGRADE, theKey, theCode, theName, order, false);
                    }
                  } else if (mPromotionsHelper.isValidCreditCard(CheckoutComponents.getProfile(ServletUtil.getCurrentRequest()), order, theKey)) {
                    if (!validateCountry) {
                      awardShipping(theShipPromo, CMOSUPGRADE, theKey, theCode, theName, order, false);
                    } else if (mPromotionsHelper.countryQualifies(order, theKey)) {
                      awardShipping(theShipPromo, CMOSUPGRADE, theKey, theCode, theName, order, false);
                    }
                  }
                }
                break;
              }// end type 125
              case 126: {
                if (mPromotionsHelper.orderHasProdDollar(order, theKey, theShipPromo.getDollarQualifier().trim()) && mPromotionsHelper.orderHasProd(order, theKey)
                        && mPromotionsHelper.isValidCreditCard(CheckoutComponents.getProfile(ServletUtil.getCurrentRequest()), order, theKey)) {
                  if (!validateCountry) {
                    awardShipping(theShipPromo, FREEBASESHIPPING, theKey, theKey, theName, order, false);
                  } else if (mPromotionsHelper.countryQualifies(order, theKey)) {
                    awardShipping(theShipPromo, FREEBASESHIPPING, theKey, theKey, theName, order, false);
                  }
                }
                break;
              }
              case 127: {
                if (mPromotionsHelper.promoCodeMatch(order, theCode) && mPromotionsHelper.orderHasProdDollar(order, theKey, theShipPromo.getDollarQualifier().trim())
                        && mPromotionsHelper.orderHasProd(order, theKey)) {
                  orderImpl = (NMOrderImpl) order;
                  final boolean validateTenderType = ((Boolean) orderImpl.getPropertyValue("validateTenderType")).booleanValue();
                  if (!validateTenderType) {
                    if (!validateCountry) {
                      awardShipping(theShipPromo, FREEBASESHIPPING, theKey, theCode, theName, order, false);
                    } else if (mPromotionsHelper.countryQualifies(order, theKey)) {
                      awardShipping(theShipPromo, FREEBASESHIPPING, theKey, theCode, theName, order, false);
                    }
                  } else if (mPromotionsHelper.isValidCreditCard(CheckoutComponents.getProfile(ServletUtil.getCurrentRequest()), order, theKey)) {
                    if (!validateCountry) {
                      awardShipping(theShipPromo, FREEBASESHIPPING, theKey, theCode, theName, order, false);
                    } else if (mPromotionsHelper.countryQualifies(order, theKey)) {
                      awardShipping(theShipPromo, FREEBASESHIPPING, theKey, theCode, theName, order, false);
                    }
                  }
                }
                break;
              }
              case 128: {
                if (mPromotionsHelper.orderHasProdDollar(order, theKey, theShipPromo.getDollarQualifier().trim()) && mPromotionsHelper.orderHasProd(order, theKey)
                        && mPromotionsHelper.isValidCreditCard(CheckoutComponents.getProfile(ServletUtil.getCurrentRequest()), order, theKey)) {
                  if (!validateCountry) {
                    awardShipping(theShipPromo, CMOSUPGRADE, theKey, theKey, theName, order, false);
                  } else if (mPromotionsHelper.countryQualifies(order, theKey)) {
                    awardShipping(theShipPromo, CMOSUPGRADE, theKey, theKey, theName, order, false);
                  }
                }
                break;
              }
              case 129: {
                if (mPromotionsHelper.promoCodeMatch(order, theCode) && mPromotionsHelper.orderHasProdDollar(order, theKey, theShipPromo.getDollarQualifier().trim())
                        && mPromotionsHelper.orderHasProd(order, theKey)) {
                  orderImpl = (NMOrderImpl) order;
                  final boolean validateTenderType = ((Boolean) orderImpl.getPropertyValue("validateTenderType")).booleanValue();
                  if (!validateTenderType) {
                    if (!validateCountry) {
                      awardShipping(theShipPromo, CMOSUPGRADE, theKey, theCode, theName, order, false);
                    } else if (mPromotionsHelper.countryQualifies(order, theKey)) {
                      awardShipping(theShipPromo, CMOSUPGRADE, theKey, theCode, theName, order, false);
                    }
                  } else if (mPromotionsHelper.isValidCreditCard(CheckoutComponents.getProfile(ServletUtil.getCurrentRequest()), order, theKey)) {
                    if (!validateCountry) {
                      awardShipping(theShipPromo, CMOSUPGRADE, theKey, theCode, theName, order, false);
                    } else if (mPromotionsHelper.countryQualifies(order, theKey)) {
                      awardShipping(theShipPromo, CMOSUPGRADE, theKey, theCode, theName, order, false);
                    }
                  }
                }
                break;
              }
              case 130: {
                int panentheticalType = PARENTHETICAL_NONE;
                if (theShipPromo.getAwardParenthetical().booleanValue()) {
                  panentheticalType = PARENTHETICAL_ALL;
                }
                
                if (mPromotionsHelper.dollarQualifies(order, theShipPromo.getDollarQualifier().trim())) {
                  final RepositoryItem profile = CheckoutComponents.getProfile(ServletUtil.getCurrentRequest());
                  lvl = ShoppingCartHandler.findInCircleBenefitLvl(profile, order);
                  
                  if (mPromotionsHelper.isValidIncircleMbr(lvl)) {
                    if (mPromotionsHelper.isQualifiedIncircleLvl(lvl, theKey, "shipping")) {
                      awardShipping(theShipPromo, FREEBASESHIPPING, theKey, theKey, theName, order, false, panentheticalType);
                    }
                  }
                }
                break;
              }
              case 131: {
                int panentheticalType = PARENTHETICAL_NONE;
                if (theShipPromo.getAwardParenthetical().booleanValue()) {
                  panentheticalType = PARENTHETICAL_ALL;
                }
                
                orderImpl = (NMOrderImpl) order;
                final Object o = orderImpl.getPropertyValue("validateTenderType");
                final Boolean bool = (Boolean) o;
                final boolean validateTenderType = bool.booleanValue();
                
                if (mPromotionsHelper.promoCodeMatch(order, theCode)) {
                  if (mPromotionsHelper.dollarQualifies(order, theShipPromo.getDollarQualifier().trim())) {
                    if (!validateTenderType) {
                      awardShipping(theShipPromo, FREEBASESHIPPING, theKey, theCode, theName, order, false, panentheticalType);
                    } else {
                      final RepositoryItem profile = CheckoutComponents.getProfile(ServletUtil.getCurrentRequest());
                      lvl = ShoppingCartHandler.findInCircleBenefitLvl(profile, order);
                      
                      if (mPromotionsHelper.isValidIncircleMbr(lvl)) {
                        if (mPromotionsHelper.isQualifiedIncircleLvl(lvl, theKey, "shipping")) {
                          awardShipping(theShipPromo, FREEBASESHIPPING, theKey, theCode, theName, order, false, panentheticalType);
                        }
                      }
                    }
                  }
                }
                break;
              }
              case 132: {
                orderImpl = (NMOrderImpl) order;
                if (mPromotionsHelper.dollarQualifies(order, theShipPromo.getDollarQualifier().trim())) {
                  final RepositoryItem profile = CheckoutComponents.getProfile(ServletUtil.getCurrentRequest());
                  lvl = ShoppingCartHandler.findInCircleBenefitLvl(profile, order);
                  
                  if (mPromotionsHelper.isValidIncircleMbr(lvl)) {
                    if (mPromotionsHelper.isQualifiedIncircleLvl(lvl, theKey, "shipping")) {
                      awardShipping(theShipPromo, CMOSUPGRADE, theKey, theKey, theName, order, true);
                    }
                  }
                }
                break;
              }
              case 133: {
                orderImpl = (NMOrderImpl) order;
                final Object o = orderImpl.getPropertyValue("validateTenderType");
                final Boolean bool = (Boolean) o;
                final boolean validateTenderType = bool.booleanValue();
                
                if (mPromotionsHelper.promoCodeMatch(order, theCode)) {
                  if (mPromotionsHelper.dollarQualifies(order, theShipPromo.getDollarQualifier().trim())) {
                    if (!validateTenderType) {
                      awardShipping(theShipPromo, CMOSUPGRADE, theKey, theCode, theName, order, true);
                    } else {
                      final RepositoryItem profile = CheckoutComponents.getProfile(ServletUtil.getCurrentRequest());
                      lvl = ShoppingCartHandler.findInCircleBenefitLvl(profile, order);
                      
                      if (mPromotionsHelper.isValidIncircleMbr(lvl)) {
                        if (mPromotionsHelper.isQualifiedIncircleLvl(lvl, theKey, "shipping")) {
                          awardShipping(theShipPromo, CMOSUPGRADE, theKey, theCode, theName, order, true);
                        }
                      }
                    }
                  }
                }
                break;
              }
              case 134: {
                int panentheticalType = PARENTHETICAL_NONE;
                if (theShipPromo.getAwardParenthetical().booleanValue()) {
                  panentheticalType = PARENTHETICAL_ALL;
                }
                
                if (mPromotionsHelper.dollarQualifies(order, theShipPromo.getDollarQualifier().trim())) {
                  final RepositoryItem profile = CheckoutComponents.getProfile(ServletUtil.getCurrentRequest());
                  lvl = ShoppingCartHandler.findInCircleBenefitLvl(profile, order);
                  
                  if (mPromotionsHelper.isValidIncircleMbr(lvl)) {
                    if (mPromotionsHelper.isQualifiedIncircleLvl(lvl, theKey, "shipping")) {
                      awardShipping(theShipPromo, FREEBASESHIPPING, theKey, theKey, theName, order, false, panentheticalType);
                      awardShipping(theShipPromo, CMOSUPGRADE, theKey, theKey, theName, order, true, panentheticalType);
                    }
                  }
                }
                break;
              }
              case 135: {
                int panentheticalType = PARENTHETICAL_NONE;
                if (theShipPromo.getAwardParenthetical().booleanValue()) {
                  panentheticalType = PARENTHETICAL_ALL;
                }
                
                orderImpl = (NMOrderImpl) order;
                final Object o = orderImpl.getPropertyValue("validateTenderType");
                final Boolean bool = (Boolean) o;
                final boolean validateTenderType = bool.booleanValue();
                
                if (mPromotionsHelper.promoCodeMatch(order, theCode)) {
                  if (mPromotionsHelper.dollarQualifies(order, theShipPromo.getDollarQualifier().trim())) {
                    if (!validateTenderType) {
                      awardShipping(theShipPromo, FREEBASESHIPPING, theKey, theCode, theName, order, false, panentheticalType);
                      awardShipping(theShipPromo, CMOSUPGRADE, theKey, theCode, theName, order, true, panentheticalType);
                    } else {
                      final RepositoryItem profile = CheckoutComponents.getProfile(ServletUtil.getCurrentRequest());
                      lvl = ShoppingCartHandler.findInCircleBenefitLvl(profile, order);
                      if (mPromotionsHelper.isValidIncircleMbr(lvl)) {
                        if (mPromotionsHelper.isQualifiedIncircleLvl(lvl, theKey, "shipping") || mPromotionsHelper.isQualifiedInCircleLstYrSpent(ShoppingCartHandler.keyIdentifierValue)) {
                          awardShipping(theShipPromo, FREEBASESHIPPING, theKey, theCode, theName, order, false, panentheticalType);
                          awardShipping(theShipPromo, CMOSUPGRADE, theKey, theCode, theName, order, true, panentheticalType);
                        }
                      }
                    }
                  }
                }
                break;
              }
              case 136: {
                try {
                  if (mPromotionsHelper.orderHasBnglKey(order, theKey) && mPromotionsHelper.promoCodeMatch(order, theCode)) {
                    awardShipping(theShipPromo, FREEBASESHIPPING, theKey, theKey, theName, order, false);
                  }
                } catch (final Exception ce) {
                  throw new ProcessException(ce);
                }
                
                break;
              }// end 136
              case 137: {
                if (mPromotionsHelper.shopRunnerMatch(order)) {
                  orderImpl = (NMOrderImpl) order;
                  if (!orderImpl.isCanadianOrder()) {
                    awardShipping(theShipPromo, SHOPRUNNER, theKey, theCode, theName, order, false);
                  }
                }
                break;
              }// end 137
              case 138: {
                int panentheticalType = PARENTHETICAL_NONE;
                if (theShipPromo.getAwardParenthetical().booleanValue()) {
                  panentheticalType = PARENTHETICAL_ALL;
                }
                if (mPromotionsHelper.promoCodeMatch(order, theCode)) {
                  if (!validateCountry) {
                    awardShipping(theShipPromo, FREEBASESHIPPING, theKey, theCode, theName, order, false, panentheticalType);
                    ((NMOrderImpl) order).setActivatedPromoCode(theCode);
                  } else if (mPromotionsHelper.countryQualifies(order, theKey)) {
                    awardShipping(theShipPromo, FREEBASESHIPPING, theKey, theCode, theName, order, false, panentheticalType);
                    ((NMOrderImpl) order).setActivatedPromoCode(theCode);
                  }
                }
                break;
              }// end 138
              case 139: {
                int panentheticalType = PARENTHETICAL_NONE;
                if (theShipPromo.getAwardParenthetical().booleanValue()) {
                  panentheticalType = PARENTHETICAL_ALL;
                }
                if (mPromotionsHelper.promoCodeMatch(order, theCode)) {
                  if (!validateCountry) {
                    awardShipping(theShipPromo, CMOSUPGRADE, theKey, theCode, theName, order, false, panentheticalType);
                    ((NMOrderImpl) order).setActivatedPromoCode(theCode);
                  } else if (mPromotionsHelper.countryQualifies(order, theKey)) {
                    awardShipping(theShipPromo, CMOSUPGRADE, theKey, theCode, theName, order, false, panentheticalType);
                    ((NMOrderImpl) order).setActivatedPromoCode(theCode);
                  }
                }
                break;
              }// end 139
            }// end switch(promotionType)
          }// end else
        } // end date
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
  
  private void handleStorePromoCodes(final Order order) throws Exception {
    logDebug("in handleStorePromoCodes");
    final DynamoHttpServletResponse resp = ServletUtil.getCurrentResponse();
    if (resp == null) { // don't bother if this isn't being run in a req/resp nature
      return;
    }
    
    final NMOrderImpl nmorder = (NMOrderImpl) order;
    
    final ArrayList foundPromos = findMatchingPromos(getActivePromoCodes(nmorder));
    
    if (foundPromos == null) { // no active promos are in the shipping realm
      logDebug(":handleStorePromoCodes:no active promos in this submitted order");
      return;
    }
    for (int i = 0; i < foundPromos.size(); i++) {
      final Promotion promo = (Promotion) foundPromos.get(i);
      logDebug(":handleStorePromoCodes:looking at promo " + promo.getCode());
      
      // if there's an active promo code that is a shipping code,
      // set the one time cookie, if appropriate
      if (promo.getOneTime().booleanValue() == true) {
        logDebug("going to set a one time use cookie for " + promo.getCode());
        // this is a onetime, so set the cookie
        // we need to be able to handle multiple cookies
        final Cookie onetime = new Cookie(promo.getCode(), promo.getCode());
        onetime.setMaxAge(60 * 60 * 24 * 365 * 2); // 2 yrs should be
        // long enough
        onetime.setPath("/");
        // dump("about to set a one-time cookie");
        resp.addCookie(onetime);
      } // if it's a one time promo
    }// end for loop
    
  } // handleStorePromoCodes
  
  private List getActivePromoCodes(final NMOrderImpl order) {
    final String orderPromoCode = order.getActivatedPromoCode();
    // dump("activated promo codes in the order are: "+orderPromoCode);
    return makeList(orderPromoCode);
  }
  
  private ArrayList findMatchingPromos(final List codes) throws Exception {
    
    boolean foundAPromo = false;
    final Iterator i = codes.iterator();
    final ArrayList foundPromos = new ArrayList();
    while (i.hasNext()) {
      final String code = (String) i.next();
      logDebug("Shipping findMatchingPromos: checking the promo array to find: [" + code + "]");
      
      final Promotion promo = thePromos.getPromotion(code);
      if (promo != null) {
        logDebug("Shipping findMatchingPromos: the search for code [" + code + "] came back with a result");
        foundPromos.add(promo);
        foundAPromo = true;
      }
    } // while entered codes
    if (foundAPromo) {
      return foundPromos;
    } else {
      logDebug("Shipping findMatchingPromos: the search for a matching promocode came back empty");
      return null;
    }
  }
  
  private List makeList(final String source) {
    return makeList(source, false);
  }
  
  private List makeList(final String source, final boolean convert) {
    final List codes = new ArrayList();
    if (source == null) {
      return codes;
    }
    final StringTokenizer myToken = new StringTokenizer(source, ",");
    while (myToken.hasMoreTokens()) {
      final String testString = myToken.nextToken().trim();
      if (convert) {
        codes.add(testString.toUpperCase());
        
      } else {
        codes.add(testString);
      }
    }
    
    return codes;
    
  }
  
  protected DynamoHttpServletRequest getRequest() {
    return ServletUtil.getCurrentRequest();
  }
}
