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

import com.nm.collections.ExtraAddress;
import com.nm.collections.ExtraAddressArray;
import com.nm.collections.NMPromotion;
import com.nm.commerce.NMProfile;
import com.nm.components.CommonComponentHelper;
import com.nm.repository.ProfileRepository;
import com.nm.utils.ExceptionUtil;
import com.nm.utils.LocalizationUtils;
import com.nm.utils.PromotionsHelper;
import com.nm.utils.SystemSpecs;
import com.nm.utils.datasource.NMTransactionDemarcation;

public class CheckCsrExtraAddress extends AddItemToOrder implements IPromoAction {
  
  public static final String PROMO_CODE = "promo_code";
  public static final String PROMO_STR = "Promotion";
  public static final String PROMOHELPER_PATH = "/nm/utils/PromotionsHelper";
  public static final String SHOPPING_CART_PATH = "/atg/commerce/ShoppingCart";
  public static final String EA_ARRAY_PATH = "/nm/collections/ExtraAddressArray";
  
  public ExtraAddressArray thePromos = null;
  public Repository repository = null;
  PromotionsHelper mPromotionsHelper = null;
  
  private boolean logDebug;
  private boolean logError = true;
  
  public static ExtraAddressArray getExtraAddressArray() {
    return ((ExtraAddressArray) Nucleus.getGlobalNucleus().resolveName(EA_ARRAY_PATH));
  }
  
  public void initialize(Map pParameters) throws ProcessException {
    /** Resolve OrderManager and Promotion components. */
    mOrderManager = OrderManager.getOrderManager();
    logDebug = getSystemSpecs().getCsrScenarioDebug();
    logDebug("CheckCsrExtraAddress is Initializing");
    if (mOrderManager == null) throw new ScenarioException(PromotionConstants.getStringResource(PromotionConstants.ORDER_MANAGER_NOT_FOUND));
    
    mOrderHolderComponent = ComponentName.getComponentName(SHOPPING_CART_PATH);
    // storeOptionalParameter(pParameters, PROMO_CODE,
    // java.lang.String.class);
    // storeOptionalParameter(pParameters, PROMO_STR,
    // java.lang.String.class);
    
    thePromos = (ExtraAddressArray) Nucleus.getGlobalNucleus().resolveName(EA_ARRAY_PATH);
    // repository =
    // (Repository)Nucleus.getGlobalNucleus().resolveName(PRODUCT_CATALOG_PATH);
    mPromotionsHelper = (PromotionsHelper) Nucleus.getGlobalNucleus().resolveName(PROMOHELPER_PATH);
    // System.out.println("###################################CheckCsrExtraAddress
    // initizialized");
    
  }
  
  protected void executeAction(ProcessExecutionContext pContext) throws ProcessException {
    logDebug("###################################CheckCsrExtraAddress called");
    try {
      Order order = getOrderToAddItemTo(pContext);
      evaluatePromo(order);
    } catch (final Exception e) {
      throw new ProcessException(ExceptionUtil.getExceptionInfo(e), e);
    }
  }
  
  public void evaluatePromo(final Order order) throws PromoException {
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
    if (mOrderManager == null) {
            throw new PromoException(PromotionConstants.getStringResource(PromotionConstants.ORDER_MANAGER_NOT_FOUND));
       }
      mOrderHolderComponent = ComponentName.getComponentName(SHOPPING_CART_PATH);
      // storeOptionalParameter(pParameters, PROMO_CODE, java.lang.String.class);
      // storeOptionalParameter(pParameters, PROMO_STR, java.lang.String.class);
      
      thePromos = (ExtraAddressArray) Nucleus.getGlobalNucleus().resolveName(EA_ARRAY_PATH);
      // repository = (Repository)Nucleus.getGlobalNucleus().resolveName(PRODUCT_CATALOG_PATH);
      mPromotionsHelper = (PromotionsHelper) Nucleus.getGlobalNucleus().resolveName(PROMOHELPER_PATH);
      
      String promoType = "promotion";
      String promoName = "promotion";
      
      try {
        Date mystartDate = Calendar.getInstance().getTime();
        String theKey = new String();
        String theCode = new String();
        Map<String, NMPromotion> promoArray = thePromos.getAllActivePromotions();
        Iterator<NMPromotion> promoIter = promoArray.values().iterator();
        while (promoIter.hasNext()) {
          ExtraAddress temp = (ExtraAddress) promoIter.next();
            // CSR Extra Address promos should never be granted to international customers regardless of promo type used
            final NMProfile profile = (NMProfile) ServletUtil.getCurrentRequest().resolveName("/atg/userprofiling/Profile");
            String country = (String) profile.getPropertyValue(ProfileRepository.COUNTRY_PREFERENCE);
            LocalizationUtils localizationUtils = ((LocalizationUtils) Nucleus.getGlobalNucleus().resolveName("/nm/utils/LocalizationUtils"));
            
            if (localizationUtils.isSupportedByFiftyOne(country)) {
              continue;
            }
            if (temp.getType().trim().equals("1")) {
              theCode = temp.getPromoCodes().trim().toUpperCase();
              
              if (mPromotionsHelper.dollarQualifies(order, temp.getDollarQualifier()) && mPromotionsHelper.promoCodeMatch(order, theCode)) {
                setupExtraAddress(temp, order, promoType, promoName);
              }// end order $ qualifies
            }// is type1
            else if (temp.getType().trim().equals("2")) {
              theCode = temp.getPromoCodes().trim().toUpperCase();
              
              if (mPromotionsHelper.dollarQualifies(order, temp.getDollarQualifier())) {
                setupExtraAddress(temp, order, promoType, promoName);
              }// end order $ qualifies
            }// is type2
            else if (temp.getType().trim().equals("3")) {
              theKey = temp.getCode().trim().toUpperCase();
              theCode = temp.getPromoCodes().trim().toUpperCase();
              if (mPromotionsHelper.orderHasBnglKey(order, theKey) && mPromotionsHelper.promoCodeMatch(order, theCode)) {
                setupExtraAddress(temp, order, promoType, promoName);
              }// end order $ qualifies
            }// is type3
        }// end for loop of promos
        
        mOrderManager.updateOrder(order);
      } catch (Exception ce) {
        ce.printStackTrace();
        throw new ScenarioException(ce);
      }
      
    } catch (Exception e) {
      rollBack = true;
      logError("CheckCsrExtraAddress.executeAction(): Exception ");
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
  
  public String getCurrentCatalogCode() {
    String catcode = getSystemSpecs().getCatalogCode();
    
    return catcode;
  }
  
  private SystemSpecs getSystemSpecs() {
    return (SystemSpecs) Nucleus.getGlobalNucleus().resolveName("/nm/utils/SystemSpecs");
  }
  
  private void setupExtraAddress(ExtraAddress thePromotion, Order order, String promoType, String promoName) throws Exception {
    String theCode = new String(thePromotion.getPromoCodes().trim().toUpperCase());
    String theKey = new String(thePromotion.getCode().trim().toUpperCase());
    
    if (order instanceof NMOrderImpl) {
      try {
        NMOrderImpl orderImpl = (NMOrderImpl) order;
        
        orderImpl.addAwardedPromotion(thePromotion);
        
        if (!mPromotionsHelper.keyMatch(orderImpl, theKey)) {
          orderImpl.setActivatedPromoCode(theCode);
          orderImpl.setUserActivatedPromoCode(theCode);
          orderImpl.setPromoName(theKey);
          mPromotionsHelper.addKEYToCI(theKey, orderImpl);
          logDebug("extraaddress HAS BEEN AWARDED--code-->" + theCode);
          logDebug("extraaddress HAS BEEN AWARDED--theKey-->" + theKey);
          
          mOrderManager.updateOrder(order);
          
        }// end promoAlreadyAwarded
      } catch (CommerceException ce) {
        throw new ScenarioException(ce);
      } catch (RepositoryException re) {
        throw new ScenarioException(re);
      }
    }
    
  }// endsetupExtraAddress
  
  /***************************************************************************
   * 
   * This is the start of the common logic for the CheckCsrExtraAddress & the removeCsrGWP. If you make a change to one of the utility methods you need to make it in the other too.
   * 
   * 
   * 
   **************************************************************************/
  
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
      Nucleus.getGlobalNucleus().logError("CheckCsrExtraAddress: " + sin);
    }
  }
  
  private void logDebug(String sin) {
    if (isLoggingDebug()) {
      Nucleus.getGlobalNucleus().logDebug("CheckCsrExtraAddress: " + sin);
    }
  }
  
  public RepositoryItem getProfileForOrder(Order order) throws PromoException {
    RepositoryItem profile = null;
    
    try {
      if (mOrderManager != null) {
        profile = mOrderManager.getOrderTools().getProfileTools().getProfileItem(order.getProfileId());
      }
    } catch (RepositoryException e) {
      throw new PromoException(e);
    }
    
    return profile;
  }
}
