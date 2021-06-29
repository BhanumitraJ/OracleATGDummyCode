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

import com.nm.collections.GiftWithPurchaseSelect;
import com.nm.collections.GiftWithPurchaseSelectArray;
import com.nm.collections.NMPromotion;
import com.nm.commerce.NMProfile;
import com.nm.components.CommonComponentHelper;
import com.nm.repository.ProfileRepository;
import com.nm.utils.ExceptionUtil;
import com.nm.utils.LocalizationUtils;
import com.nm.utils.PromotionsHelper;
import com.nm.utils.SystemSpecs;
import com.nm.utils.datasource.NMTransactionDemarcation;

/**
 * CheckCsrGwpSelect
 * 
 * This action was developed for the CSR GWP tool. It allows the user to create a GWP repository item with the CSR tool and generic scenario will call this action on a item added to cart event. This
 * action will get a refernce to the GiftWithPurchaseArray component which will return an ArrayList of all the Promotions in the repository. This action will iterate thru every Promotion and determine
 * is the date current. If the date is valid then it will look at the characteristics of that type and determine if the current order qualifes and award the GWP.
 * 
 * @author Todd Schultz
 * @since 08/20/2004
 */

public class CheckCsrGwpSelect extends AddItemToOrder implements IPromoAction {
  public static final String PROMO_CODE = "promo_code";// The CMOS required
                                                       // 'FREEBASESHIPPING'
                                                       // code
  public static final String PROMO_STR = "Promotion";
  public static final String PROMOHELPER_PATH = "/nm/utils/PromotionsHelper";
  public static final String SHOPPING_CART_PATH = "/atg/commerce/ShoppingCart";
  public static final String PRODUCT_CATALOG_PATH = "/atg/commerce/catalog/ProductCatalog";
  public static final String GWP_ARRAY_PATH = "/nm/collections/GiftWithPurchaseSelectArray";
  
  public GiftWithPurchaseSelectArray thePromos = null;
  public Repository repository = null;
  PromotionsHelper mPromotionsHelper = null;
  
  private boolean logDebug;
  private boolean logError = true;
  
  public static GiftWithPurchaseSelectArray getGiftWithPurchaseSelectArray() {
    return ((GiftWithPurchaseSelectArray) Nucleus.getGlobalNucleus().resolveName(GWP_ARRAY_PATH));
  }
  
  @Override
  public void initialize(Map pParameters) throws ProcessException {
    /** Resolve OrderManager and Promotion components. */
    mOrderManager = OrderManager.getOrderManager();
    logDebug = getSystemSpecs().getCsrScenarioDebug();
    logDebug("CheckCsrGwpSelect is Initializing");
    if (mOrderManager == null) {
      throw new ScenarioException(PromotionConstants.getStringResource(PromotionConstants.ORDER_MANAGER_NOT_FOUND));
    }
    
    mOrderHolderComponent = ComponentName.getComponentName(SHOPPING_CART_PATH);
    // storeOptionalParameter(pParameters, PROMO_CODE,
    // java.lang.String.class);
    // storeOptionalParameter(pParameters, PROMO_STR,
    // java.lang.String.class);
    
    thePromos = getGiftWithPurchaseSelectArray();
    repository = (Repository) Nucleus.getGlobalNucleus().resolveName(PRODUCT_CATALOG_PATH);
    mPromotionsHelper = (PromotionsHelper) Nucleus.getGlobalNucleus().resolveName(PROMOHELPER_PATH);
    
  }
  
  /***************************************************************************
   * This method is called from scenario, note no params are accepted but they are still required to be passed by scenario. Once validation is performed that all values needed are populated RQL
   * queries are perfored to create Repository Item
   * 
   * @param ScenarioExecutionContext
   *          pContext
   * @return void
   * @exception ScenarioException
   **************************************************************************/
  @Override
  protected void executeAction(ProcessExecutionContext pContext) throws ProcessException {
    try {
      Order order = getOrderToAddItemTo(pContext);
      evaluatePromo(order);
    } catch (final Exception e) {
      throw new ProcessException(ExceptionUtil.getExceptionInfo(e), e);
    }
  }
  
  @Override
  public void evaluatePromo(Order order) throws PromoException {
    TransactionDemarcation td = new NMTransactionDemarcation();
    boolean rollBack = false;
    boolean evalPromosForEmployee = false;
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
      
      if (order instanceof NMOrderImpl) {
        NMOrderImpl nmOrderImpl = (NMOrderImpl) order;
        evalPromosForEmployee = nmOrderImpl.isEvaluatePromosForEmployee();
      }
      if (mOrderManager == null) {
        throw new PromoException(PromotionConstants.getStringResource(PromotionConstants.ORDER_MANAGER_NOT_FOUND));
      }
      
      try {
        Date mystartDate = Calendar.getInstance().getTime();
        
        String theDept = new String();
        String theClass = new String();
        String theDesigner = new String();
        String theDepiction = new String();
        Double tempDollar = new Double(0.0);
        double doubleDollar = 0;
        boolean onlineCatOnly = false;
        Map<String, NMPromotion> promotionCollection = thePromos.getAllPromotions();
        Iterator iterator = promotionCollection.values().iterator();
        
        while (iterator.hasNext()) {
          GiftWithPurchaseSelect promotion = (GiftWithPurchaseSelect) iterator.next();
          int type = Integer.parseInt(promotion.getType());
          /* If isExcludeEmployeesFlag is true ,don't apply GWPSelect promotion to the employee order */
          if (evalPromosForEmployee && promotion.isExcludeEmployeesFlag()) {
            continue;
          }
          if (mystartDate.after(promotion.getStartDate()) && mystartDate.before(promotion.getEndDate())) {
            // GWP Select promos should never be granted to international customers regardless of promo type used
            final NMProfile profile = (NMProfile) ServletUtil.getCurrentRequest().resolveName("/atg/userprofiling/Profile");
            String country = (String) profile.getPropertyValue(ProfileRepository.COUNTRY_PREFERENCE);
            LocalizationUtils localizationUtils = ((LocalizationUtils) Nucleus.getGlobalNucleus().resolveName("/nm/utils/LocalizationUtils"));
            
            if (localizationUtils.isSupportedByFiftyOne(country)) {
              continue;
            }
            
            switch (type) {
              case 1: {
                theDept = promotion.getDeptCodes().trim().toUpperCase();
                theClass = promotion.getClassCodes().trim().toUpperCase();
                tempDollar = Double.valueOf(promotion.getDollarQualifier());
                doubleDollar = tempDollar.doubleValue();
                onlineCatOnly = promotion.getOnlineCatOnly().booleanValue();
                
                if (mPromotionsHelper.deptClassDollarMatch(order, theDept, theClass, doubleDollar, onlineCatOnly)) {
                  setupGWP(promotion, order);
                }
                break;
              }
              case 2: {
                theDepiction = promotion.getDepiction().trim().toUpperCase();
                tempDollar = Double.valueOf(promotion.getDollarQualifier());
                doubleDollar = tempDollar.doubleValue();
                if (mPromotionsHelper.depictionDollarMatch(order, theDepiction, doubleDollar)) {
                  setupGWP(promotion, order);
                }
                break;
              }
              case 3: {
                theDesigner = promotion.getVendor().trim().toUpperCase();
                tempDollar = Double.valueOf(promotion.getDollarQualifier());
                doubleDollar = tempDollar.doubleValue();
                if (mPromotionsHelper.vendorDollarMatch(order, theDesigner, doubleDollar)) {
                  setupGWP(promotion, order);
                }
                break;
              }
              case 4: {
                theDesigner = promotion.getVendor().trim().toUpperCase();
                theDept = promotion.getDeptCodes().trim().toUpperCase();
                theClass = promotion.getClassCodes().trim().toUpperCase();
                onlineCatOnly = promotion.getOnlineCatOnly().booleanValue();
                tempDollar = Double.valueOf(promotion.getDollarQualifier());
                doubleDollar = tempDollar.doubleValue();
                double customersTotal = 0;
                
                double deptClassDollar = mPromotionsHelper.vendorDollarDeptClassMatch(order, theDesigner, theDept, theClass, onlineCatOnly, doubleDollar);
                customersTotal = customersTotal + deptClassDollar;
                
                if (customersTotal > doubleDollar) {
                  setupGWP(promotion, order);
                }
                break;
              }
            }
          }// end date is good
        }// end for loop of promos
        
        mOrderManager.updateOrder(order);
      } catch (Exception ce) {
        ce.printStackTrace();
        throw new ScenarioException(ce);
      }
    } catch (Exception e) {
      rollBack = true;
      logError("CheckCsrGwpSelect.executeAction(): Exception ");
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
  
  private SystemSpecs getSystemSpecs() {
    return (SystemSpecs) Nucleus.getGlobalNucleus().resolveName("/nm/utils/SystemSpecs");
  }
  
  private void setupGWP(GiftWithPurchaseSelect promotion, Order order) throws Exception {
    String promoKey = promotion.getCode();
    String cmosItem = promotion.getGwpItem();
    
    RepositoryItem promotionalItemRI = mPromotionsHelper.getProdItem(cmosItem, promoKey);
    
    if (promotionalItemRI != null) {
      String productID = promotionalItemRI.getRepositoryId();
      
      // Add the free gift item to the cart
      try {
        if (!mPromotionsHelper.promoAlreadyAwarded(order, productID)) {
          NMOrderImpl orderImpl = (NMOrderImpl) order;
          
          boolean hasValidShipToLocations = true;
          
          // check to make sure we have at least one good shipping location (or 0 shipping groups for a new order)
          // before adding the item to the cart
          if (orderImpl.shouldValidateCountry()) {
            String promoCode = new String(promotion.getPromoCodes());
            hasValidShipToLocations = mPromotionsHelper.getFirstValidShippingGroupForGwp(promotionalItemRI, orderImpl) != null;
          }
          
          if (hasValidShipToLocations) {
            orderImpl.setPromoName(promoKey);
            orderImpl.addAwardedPromotion(promotion);
            
            logDebug("CSRGWPSELECT before updateorder-the order version-------------------->" + orderImpl.getVersion());
            mOrderManager.updateOrder(order);
            logDebug("CSRGWP after updateorder--the order version-after ------------------->" + orderImpl.getVersion());
          }
          
        }
      } catch (CommerceException ce) {
        throw new ScenarioException(ce);
      } catch (RepositoryException re) {
        throw new ScenarioException(re);
      }
    } else {
      logError("SetupGWP: promotionalItemRI is null. This should never happen!");
    }
  }// endsetupGWP
  
  /***************************************************************************
   * 
   * This is the start of the common logic for the CheckCsrGwpSelect & the removeCsrGWP. If you make a change to one of the utility methods you need to make it in the other too.
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
      Nucleus.getGlobalNucleus().logError("CheckCsrGwpSelect: " + sin);
    }
  }
  
  private void logDebug(String sin) {
    if (isLoggingDebug()) {
      Nucleus.getGlobalNucleus().logDebug("CheckCsrGwpSelect: " + sin);
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
