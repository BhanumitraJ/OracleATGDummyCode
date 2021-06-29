package com.nm.commerce.promotion;

import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.transaction.TransactionManager;

import atg.commerce.CommerceException;
import atg.commerce.order.Order;
import atg.commerce.order.OrderManager;
import atg.commerce.promotion.AddItemToOrder;
import atg.commerce.promotion.PromotionConstants;
import atg.dtm.TransactionDemarcation;
import atg.dtm.TransactionDemarcationException;
import atg.nucleus.Nucleus;
import atg.nucleus.naming.ComponentName;
import atg.process.ProcessException;
import atg.process.ProcessExecutionContext;
import atg.repository.Repository;
import atg.repository.RepositoryException;
import atg.repository.RepositoryItem;
import atg.scenario.ScenarioException;
import atg.servlet.ServletUtil;

import com.nm.collections.Giftwrap;
import com.nm.collections.GiftwrapArray;
import com.nm.collections.NMPromotion;
import com.nm.commerce.NMProfile;
import com.nm.commerce.pricing.NMPricingTools;
import com.nm.components.CommonComponentHelper;
import com.nm.formhandler.ShoppingCartHandler;
import com.nm.repository.ProfileRepository;
import com.nm.utils.ExceptionUtil;
import com.nm.utils.LocalizationUtils;
import com.nm.utils.PromotionsHelper;
import com.nm.utils.SystemSpecs;

public class CheckCsrGiftwrap extends AddItemToOrder implements IPromoAction {
  
  public static final String PROMO_CODE = "promo_code";
  public static final String PROMO_STR = "Promotion";
  public static final String PROMOHELPER_PATH = "/nm/utils/PromotionsHelper";
  public static final String SHOPPING_CART_PATH = "/atg/commerce/ShoppingCart";
  public static final String GW_ARRAY_PATH = "/nm/collections/GiftwrapArray";
  public static final String PRICINGTOOLS_PATH = "/atg/commerce/pricing/PricingTools";
  
  public GiftwrapArray thePromos = null;
  public Repository repository = null;
  PromotionsHelper mPromotionsHelper = null;
  
  private boolean logDebug;
  private boolean logError = true;
  
  public static GiftwrapArray getGiftwrapArray() {
    return ((GiftwrapArray) Nucleus.getGlobalNucleus().resolveName(GW_ARRAY_PATH));
  }
  
  public static NMPricingTools getPricingTools() {
    return ((NMPricingTools) Nucleus.getGlobalNucleus().resolveName(PRICINGTOOLS_PATH));
  }
  
  @Override
  public void initialize(final Map pParameters) throws ProcessException {
    /** Resolve OrderManager and Promotion components. */
    mOrderManager = OrderManager.getOrderManager();
    logDebug = getSystemSpecs().getCsrScenarioDebug();
    logDebug("CheckCsrGiftwrap is Initializing");
    if (mOrderManager == null) {
      throw new ScenarioException(PromotionConstants.getStringResource(PromotionConstants.ORDER_MANAGER_NOT_FOUND));
    }
    
    mOrderHolderComponent = ComponentName.getComponentName(SHOPPING_CART_PATH);
    // storeOptionalParameter(pParameters, PROMO_CODE,
    // java.lang.String.class);
    // storeOptionalParameter(pParameters, PROMO_STR,
    // java.lang.String.class);
    
    thePromos = (GiftwrapArray) Nucleus.getGlobalNucleus().resolveName(GW_ARRAY_PATH);
    // repository =
    // (Repository)Nucleus.getGlobalNucleus().resolveName(PRODUCT_CATALOG_PATH);
    mPromotionsHelper = (PromotionsHelper) Nucleus.getGlobalNucleus().resolveName(PROMOHELPER_PATH);
    // System.out.println("###################################CheckCsrGiftwrap
    // initizialized");
    
  }
  
  @Override
  protected void executeAction(final ProcessExecutionContext pContext) throws ProcessException {
    logDebug("###################################CheckCsrGiftwrap called");
    try {
      final Order order = getOrderToAddItemTo(pContext);
      evaluatePromo(order);
    } catch (final Exception e) {
      throw new ProcessException(ExceptionUtil.getExceptionInfo(e), e);
    }
  }
  
  @Override
  public void evaluatePromo(final Order order) throws PromoException {
    final TransactionDemarcation td = new TransactionDemarcation();
    boolean rollBack = false;
    boolean evalPromosForEmployee = false;
    try {
      final TransactionManager tm = CommonComponentHelper.getTransactionManager();
      if (tm != null) {
        td.begin(tm, TransactionDemarcation.REQUIRED);
      }
    } catch (final TransactionDemarcationException tde) {
      tde.printStackTrace();
      throw new PromoException(tde);
    }
    
    try {
      
      mOrderHolderComponent = ComponentName.getComponentName(SHOPPING_CART_PATH);
      // storeOptionalParameter(pParameters, PROMO_CODE, java.lang.String.class);
      // storeOptionalParameter(pParameters, PROMO_STR, java.lang.String.class);
      
      thePromos = (GiftwrapArray) Nucleus.getGlobalNucleus().resolveName(GW_ARRAY_PATH);
      // repository = (Repository)Nucleus.getGlobalNucleus().resolveName(PRODUCT_CATALOG_PATH);
      mPromotionsHelper = (PromotionsHelper) Nucleus.getGlobalNucleus().resolveName(PROMOHELPER_PATH);
      
      
      final String promoType = "promotion";
      final String promoName = "promotion";
      
      if (mOrderManager == null) {
        throw new ScenarioException(PromotionConstants.getStringResource(PromotionConstants.ORDER_MANAGER_NOT_FOUND));
      }
      
      try {
        final Date mystartDate = Calendar.getInstance().getTime();
        
        String theCode = new String();
        String theKey = new String();
        String lvl = null;
        if (order instanceof NMOrderImpl) {
          final NMOrderImpl nmOrderImpl = (NMOrderImpl) order;
          evalPromosForEmployee = nmOrderImpl.isEvaluatePromosForEmployee();
        }
        final Map<String, NMPromotion> promoArray = thePromos.getAllPromotions();
        Iterator<NMPromotion> promoIter = promoArray.values().iterator();
        while (promoIter.hasNext()) {
          final Giftwrap temp = (Giftwrap) promoIter.next();
          /**
           * Check isExcludeEmployeesFlag eligibility for the CSR promotions to Apply
           * <ul>
           * <li>
           * If isExcludeEmployeesFlag is true, don't apply pwp promotion to the employee order</li>
           * </ul>
           */
          if (evalPromosForEmployee && temp.isExcludeEmployeesFlag()) {
            continue;
          }
          if (mystartDate.after(temp.getStartDate()) && mystartDate.before(temp.getEndDate())) {
            // CSR Gift wrap promos should never be granted to international customers regardless of promo type used
            final NMProfile profile = (NMProfile) ServletUtil.getCurrentRequest().resolveName("/atg/userprofiling/Profile");
            final String country = (String) profile.getPropertyValue(ProfileRepository.COUNTRY_PREFERENCE);
            final LocalizationUtils localizationUtils = ((LocalizationUtils) Nucleus.getGlobalNucleus().resolveName("/nm/utils/LocalizationUtils"));
            
            if (localizationUtils.isSupportedByFiftyOne(country)) {
              continue;
            }
            
            if (temp.getType().trim().equals("1")) {
              theCode = temp.getPromoCodes().trim().toUpperCase();
              
              if (mPromotionsHelper.promoCodeMatch(order, theCode) && mPromotionsHelper.dollarQualifies(order, temp.getDollarQualifier())) {
                setupGiftwrap(temp, order, promoType, promoName);
              }// end order $ qualifies
            }// is type1
            else if (temp.getType().trim().equals("2")) {
              // theCode =
              // temp.getPromoCodes().trim().toUpperCase();
              
              if (mPromotionsHelper.dollarQualifies(order, temp.getDollarQualifier())) {
                setupGiftwrap(temp, order, promoType, promoName);
              }// end order $ qualifies
            }// is type2
            else if (temp.getType().trim().equals("3")) {
              theKey = temp.getCode().trim().toUpperCase();
              
              if (mPromotionsHelper.orderHasProd(order, theKey)) {
                setupGiftwrap(temp, order, promoType, promoName);
              }// end order $ qualifies
            }// is type3
            else if (temp.getType().trim().equals("4")) {
              theKey = temp.getCode().trim().toUpperCase();
              
              if (mPromotionsHelper.orderHasProdDollar(order, theKey, temp.getDollarQualifier().trim())) {
                setupGiftwrap(temp, order, promoType, promoName);
              }// end order $ qualifies
            }// is type4
            else if (temp.getType().trim().equals("5")) {
              theKey = temp.getCode().trim().toUpperCase();
              theCode = temp.getPromoCodes().trim().toUpperCase();
              
              if (mPromotionsHelper.promoCodeMatch(order, theCode) && mPromotionsHelper.orderHasProdDollar(order, theKey, temp.getDollarQualifier().trim())) {
                setupGiftwrap(temp, order, promoType, promoName);
              }// end order $ qualifies
            }// is type5
            else if (temp.getType().trim().equals("6")) {
              if (mPromotionsHelper.dollarQualifies(order, temp.getDollarQualifier())) {
                theKey = temp.getCode().trim().toUpperCase();
                lvl = ShoppingCartHandler.findInCircleBenefitLvl(profile, order);
                if (mPromotionsHelper.isValidIncircleMbr(lvl)) {
                  if (mPromotionsHelper.isQualifiedIncircleLvl(lvl, theKey, "giftwrap")) {
                    setupGiftwrap(temp, order, promoType, promoName);
                  }
                }
              }// end order $ qualifies
            }// is type6
            else if (temp.getType().trim().equals("7")) {
              theCode = temp.getPromoCodes().trim().toUpperCase();
              theKey = temp.getCode().trim().toUpperCase();
              final boolean autoGiftwrap = temp.getAutoGwFlag();
              
              if (mPromotionsHelper.orderHasBnglKey(order, theKey) && mPromotionsHelper.promoCodeMatch(order, theCode)) {
                setupGiftwrap(temp, order, promoType, promoName);
                if (autoGiftwrap) {
                  final String giftWrapId = getSystemSpecs().getGiftwrapCode();
                  final RepositoryItem giftwrapItem = getPricingTools().getGiftWrapItem(giftWrapId);
                  mPromotionsHelper.addGiftwrapQualItems(theKey, order, giftwrapItem);
                  mOrderManager.updateOrder(order);
                }
              }// end order $ qualifies
              
            }// is type7
          }// end date is good
        }// end for loop of promos
        
        mOrderManager.updateOrder(order);
      } catch (final Exception ce) {
        ce.printStackTrace();
        throw new PromoException(ce);
      }
    } catch (final Exception e) {
      rollBack = true;
      logError("CheckCsrGiftwrap.executeAction(): Exception ");
      throw new PromoException(e);
    } finally {
      try {
        td.end(rollBack); // commit work
      } catch (final TransactionDemarcationException tde) {
        tde.printStackTrace();
        throw new PromoException(tde);
      }// end-try
    }// end-try
    
  } // end of executeAction method
  
  public String getCurrentCatalogCode() {
    final String catcode = getSystemSpecs().getCatalogCode();
    
    return catcode;
  }
  
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
  
  private SystemSpecs getSystemSpecs() {
    return (SystemSpecs) Nucleus.getGlobalNucleus().resolveName("/nm/utils/SystemSpecs");
  }
  
  private void setupGiftwrap(final Giftwrap thePromotion, final Order order, final String promoType, final String promoName) throws Exception {
    final String theCode = new String(thePromotion.getPromoCodes().trim().toUpperCase());
    final String theKey = new String(thePromotion.getCode().trim().toUpperCase());
    
    if (order instanceof NMOrderImpl) {
      try {
        final NMOrderImpl orderImpl = (NMOrderImpl) order;
        orderImpl.addAwardedPromotion(thePromotion);
        
        if (!mPromotionsHelper.keyMatch(order, theKey)) {
          orderImpl.setActivatedPromoCode(theCode);
          orderImpl.setUserActivatedPromoCode(theCode);
          orderImpl.setPromoName(theKey);
          
          logDebug("giftwrap HAS BEEN AWARDED");
          // now put the key on all the CI to send to cmos
          mPromotionsHelper.addKEYToCI(theKey, orderImpl);
          mOrderManager.updateOrder(order);
        }// end promoAlreadyAwarded
      } catch (final CommerceException ce) {
        throw new ScenarioException(ce);
      } catch (final RepositoryException re) {
        throw new ScenarioException(re);
      }
    }
    
  }// endsetupGiftwrap
  
  /***************************************************************************
   * 
   * This is the start of the common logic for the CheckCsrGiftwrap & the removeCsrGWP. If you make a change to one of the utility methods you need to make it in the other too.
   * 
   * 
   * 
   **************************************************************************/
  
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
      Nucleus.getGlobalNucleus().logError("CheckCsrGiftwrap: " + sin);
    }
  }
  
  private void logDebug(final String sin) {
    if (isLoggingDebug()) {
      Nucleus.getGlobalNucleus().logDebug("CheckCsrGiftwrap: " + sin);
    }
  }
}
