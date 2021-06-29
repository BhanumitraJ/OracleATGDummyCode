package com.nm.commerce.promotion;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.transaction.TransactionManager;

import atg.commerce.CommerceException;
import atg.commerce.order.CommerceItem;
import atg.commerce.order.ItemAddedToOrder;
import atg.commerce.order.ItemRemovedFromOrder;
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
import atg.servlet.DynamoHttpServletRequest;
import atg.servlet.ServletUtil;

import com.nm.collections.GiftWithPurchase;
import com.nm.collections.GiftWithPurchaseArray;
import com.nm.collections.NMPromotion;
import com.nm.commerce.NMCommerceItem;
import com.nm.commerce.NMProfile;
import com.nm.commerce.checkout.CheckoutAPI;
import com.nm.commerce.order.NMOrderManager;
import com.nm.common.INMGenericConstants;
import com.nm.components.CommonComponentHelper;
import com.nm.formhandler.ShoppingCartHandler;
import com.nm.repository.ProfileRepository;
import com.nm.utils.ExceptionUtil;
import com.nm.utils.LocalizationUtils;
import com.nm.utils.PromotionsHelper;
import com.nm.utils.SystemSpecs;

/**
 * CheckCsrGwp
 * 
 * This action was developed for the CSR GWP tool. It allows the user to create a GWP repository item with the CSR tool and generic scenario will call this action on a item added to cart event. This
 * action will get a refernce to the GiftWithPurchaseArray component which will return an ArrayList of all the Promotions in the repository. This action will iterate thru every Promotion and determine
 * is the date current. If the date is valid then it will look at the characteristics of that type and determine if the current order qualifes and award the GWP.
 * 
 * @author Todd Schultz
 * @since 08/20/2004
 */

public class CheckCsrGwp extends AddItemToOrder implements IPromoAction {
  public static final String PROMO_CODE = "promo_code";// The CMOS required
                                                       // 'FREEBASESHIPPING'
                                                       // code
  public static final String PROMO_STR = "Promotion";
  public static final String PROMO_TYPE = "Promotion";
  public static final String PROMO_NAME = "Promotion";
  public static final String PROMOHELPER_PATH = "/nm/utils/PromotionsHelper";
  public static final String SHOPPING_CART_PATH = "/atg/commerce/ShoppingCart";
  public static final String PRODUCT_CATALOG_PATH = "/atg/commerce/catalog/ProductCatalog";
  public static final String GWP_ARRAY_PATH = "/nm/collections/GiftWithPurchaseArray";
  
  public static final long REMOVE_ALL_ITEMS = Long.MAX_VALUE;
  
  public GiftWithPurchaseArray thePromos = null;
  public Repository repository = null;
  PromotionsHelper mPromotionsHelper = null;
  
  private boolean logDebug = true;
  private boolean logError = true;
  
  public static PromotionsHelper getPromotionsHelper() {
    return ((PromotionsHelper) Nucleus.getGlobalNucleus().resolveName(PROMOHELPER_PATH));
  }
  
  public static GiftWithPurchaseArray getGiftWithPurchaseArray() {
    return ((GiftWithPurchaseArray) Nucleus.getGlobalNucleus().resolveName(GWP_ARRAY_PATH));
  }
  
  @Override
  public void initialize(Map pParameters) throws ProcessException {
    /** Resolve OrderManager and Promotion components. */
    mOrderManager = OrderManager.getOrderManager();
    logDebug = getSystemSpecs().getCsrScenarioDebug();
    logDebug("CheckCsrGwp is Initializing");
    if (mOrderManager == null) {
      throw new ScenarioException(PromotionConstants.getStringResource(PromotionConstants.ORDER_MANAGER_NOT_FOUND));
    }
    
    mOrderHolderComponent = ComponentName.getComponentName(SHOPPING_CART_PATH);
    // storeOptionalParameter(pParameters, PROMO_CODE, java.lang.String.class);
    // storeOptionalParameter(pParameters, PROMO_STR, java.lang.String.class);
    
    thePromos = getGiftWithPurchaseArray();
    repository = (Repository) Nucleus.getGlobalNucleus().resolveName(PRODUCT_CATALOG_PATH);
    mPromotionsHelper = getPromotionsHelper();
    // System.out.println("###################################CheckCsrGwp initizialized");
    
  }
  
  /***************************************************************************
   * This method is called from scenario, note no params are accepted but they are still required to be passed by scenario. Once validation is performed that all values needed are populated RQL
   * queries are performed to create Repository Item
   * 
   * @param ScenarioExecutionContext
   *          pContext
   * @return void
   * @exception ScenarioException
   **************************************************************************/
  @Override
  protected void executeAction(ProcessExecutionContext pContext) throws ProcessException {
    // if the cart change was caused by splitting items on the
    // shipping
    // select screen then we should not evaluate the order for GWs.
    final Object ctxMsg = pContext.getMessage();
    NMCommerceItem commItem = null;
    if (ctxMsg instanceof ItemAddedToOrder) {
      commItem = (NMCommerceItem) ((ItemAddedToOrder) ctxMsg).getCommerceItem();
    } else if (ctxMsg instanceof ItemRemovedFromOrder) {
      commItem = (NMCommerceItem) ((ItemRemovedFromOrder) ctxMsg).getCommerceItem();
    }
    
    if ((null != commItem) && commItem.isShoppingCardItemSplitInProgress()) {
      return;
    }
    
    try {
      Order order = getOrderToAddItemTo(pContext);
      evaluatePromo(order);
    } catch (final Exception e) {
      throw new ProcessException(ExceptionUtil.getExceptionInfo(e), e);
    }
  }
  
  @Override
  public void evaluatePromo(Order order) throws PromoException {
    TransactionDemarcation td = new TransactionDemarcation();
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
        throw new ScenarioException(PromotionConstants.getStringResource(PromotionConstants.ORDER_MANAGER_NOT_FOUND));
      }
      
      if (order instanceof NMOrderImpl) {
        try {
          Date mystartDate = Calendar.getInstance().getTime();
          
          NMOrderImpl nmOrderImpl = (NMOrderImpl) order;
          
          boolean onlineCatOnly = false;
          boolean evalPromosForEmployee = nmOrderImpl.isEvaluatePromosForEmployee();
          
          // clear any Gwp's that have multiple SKUs that have not
          // been processed by the order yet. We're going to
          // re-qualify the promos now.
          nmOrderImpl.clearGwpMultiSkuPromoMap();
          List activatedPromotions = nmOrderImpl.getPromoNameList();
          
          HashSet restrictedFreeGwpItems = null;
          final DynamoHttpServletRequest req = ServletUtil.getCurrentRequest();
          if (null != req) {
            restrictedFreeGwpItems = (HashSet) req.getSession().getAttribute("restrictedFreeGWPItems");
          }

          Map<String, NMPromotion> promoArray = thePromos.getAllPromotions();
          Iterator<NMPromotion> promoIter = promoArray.values().iterator();
          while (promoIter.hasNext()) {
            GiftWithPurchase temp = (GiftWithPurchase) promoIter.next();
            /* If isExcludeEmployeesFlag is true ,don't apply gwp promotion to the employee order */
            if (evalPromosForEmployee && temp.isExcludeEmployeesFlag()) {
              continue;
            }
            
            if (mystartDate.after(temp.getStartDate()) && mystartDate.before(temp.getEndDate())) {
              String promoCode = temp.getPromoCodes().trim().toUpperCase();
              String department = temp.getDeptCodes().trim().toUpperCase();
              String classCode = temp.getClassCodes().trim().toUpperCase();
              String depiction = temp.getDepiction().trim().toUpperCase();
              String catalog = temp.getQualifyCatalog().trim().toUpperCase();
              String onlineCatTest = temp.getOnlineCatOnly().trim().toUpperCase();
              String designer = temp.getVendor().trim().toUpperCase();
              String items = temp.getQualifyItems().trim().toUpperCase();
              String promoKey = temp.getCode().trim().toUpperCase();
              String categories = temp.getQualifyingCategories().trim().toLowerCase();
              String saleQualifier = temp.getSaleQualificationName();
              double doubleDollar = 0.0;
              
              String dollarQualifier = temp.getDollarQualifier();
              
              if ((dollarQualifier != null) && (dollarQualifier.length() > 0)) {
                Double tempDollar = Double.valueOf(temp.getDollarQualifier());
                doubleDollar = tempDollar.doubleValue();
              }
              
              onlineCatOnly = onlineCatTest.equals("TRUE");
              int type = 0;
              try {
                type = Integer.parseInt(temp.getType().trim());
              } catch (NumberFormatException e) {}
              
              // GWP promos should never be granted to international customers regardless of promo type used
              final NMProfile profile = (NMProfile) ServletUtil.getCurrentRequest().resolveName("/atg/userprofiling/Profile");
              String country = (String) profile.getPropertyValue(ProfileRepository.COUNTRY_PREFERENCE);
              LocalizationUtils localizationUtils = ((LocalizationUtils) Nucleus.getGlobalNucleus().resolveName("/nm/utils/LocalizationUtils"));
              
              if (localizationUtils.isSupportedByFiftyOne(country)) {
                removeGwpPromotion(nmOrderImpl, temp, activatedPromotions, REMOVE_ALL_ITEMS);
                continue;
              }
              
              switch (type) {
                case 1: {
                  if (mPromotionsHelper.dollarQualifies(order, temp.getDollarQualifier(), saleQualifier)) {
                    setupGWP(temp, nmOrderImpl, restrictedFreeGwpItems, 1);
                  } else {
                    removeGwpPromotion(nmOrderImpl, temp, activatedPromotions, REMOVE_ALL_ITEMS);
                  }
                  
                  break;
                }
                
                case 2: {
                  if (mPromotionsHelper.promoCodeMatch(order, promoCode, saleQualifier)) {
                    setupGWP(temp, nmOrderImpl, restrictedFreeGwpItems, 1);
                  } else {
                    removeGwpPromotion(nmOrderImpl, temp, activatedPromotions, REMOVE_ALL_ITEMS);
                  }
                  
                  break;
                }
                
                case 3: {
                  if (mPromotionsHelper.promoCodeMatch(order, promoCode, saleQualifier) && mPromotionsHelper.dollarQualifies(order, temp.getDollarQualifier(), saleQualifier)) {
                    setupGWP(temp, nmOrderImpl, restrictedFreeGwpItems, 1);
                  } else {
                    removeGwpPromotion(nmOrderImpl, temp, activatedPromotions, REMOVE_ALL_ITEMS);
                  }
                  
                  break;
                }
                
                case 4: {
                  if (mPromotionsHelper.deptClassMatch(order, department, classCode, onlineCatOnly, saleQualifier)) {
                    setupGWP(temp, nmOrderImpl, restrictedFreeGwpItems, 1);
                  } else {
                    removeGwpPromotion(nmOrderImpl, temp, activatedPromotions, REMOVE_ALL_ITEMS);
                  }
                  
                  break;
                }
                
                case 5: {
                  if (mPromotionsHelper.promoCodeMatch(nmOrderImpl, promoCode, saleQualifier) && mPromotionsHelper.deptClassMatch(order, department, classCode, onlineCatOnly, saleQualifier)) {
                    setupGWP(temp, nmOrderImpl, restrictedFreeGwpItems, 1);
                  } else {
                    removeGwpPromotion(nmOrderImpl, temp, activatedPromotions, REMOVE_ALL_ITEMS);
                  }
                  
                  break;
                }
                
                case 6: {
                  if (mPromotionsHelper.deptClassDollarMatch(order, department, classCode, doubleDollar, onlineCatOnly, saleQualifier)) {
                    setupGWP(temp, nmOrderImpl, restrictedFreeGwpItems, 1);
                  } else {
                    removeGwpPromotion(nmOrderImpl, temp, activatedPromotions, REMOVE_ALL_ITEMS);
                  }
                  
                  break;
                }
                
                case 7: {
                  if (mPromotionsHelper.promoCodeMatch(order, promoCode, saleQualifier)
                          && mPromotionsHelper.deptClassDollarMatch(order, department, classCode, doubleDollar, onlineCatOnly, saleQualifier)) {
                    setupGWP(temp, nmOrderImpl, restrictedFreeGwpItems, 1);
                  } else {
                    removeGwpPromotion(nmOrderImpl, temp, activatedPromotions, REMOVE_ALL_ITEMS);
                  }
                  
                  break;
                }
                
                case 8: {
                  if (mPromotionsHelper.depictionMatch(order, depiction, saleQualifier)) {
                    setupGWP(temp, nmOrderImpl, restrictedFreeGwpItems, 1);
                  } else {
                    removeGwpPromotion(nmOrderImpl, temp, activatedPromotions, REMOVE_ALL_ITEMS);
                  }
                  
                  break;
                }
                
                case 9: {
                  if (mPromotionsHelper.promoCodeMatch(order, promoCode, saleQualifier) && mPromotionsHelper.depictionMatch(order, depiction, saleQualifier)) {
                    setupGWP(temp, nmOrderImpl, restrictedFreeGwpItems, 1);
                  } else {
                    removeGwpPromotion(nmOrderImpl, temp, activatedPromotions, REMOVE_ALL_ITEMS);
                  }
                  
                  break;
                }
                
                case 10: {
                  if (mPromotionsHelper.depictionDollarMatch(order, depiction, doubleDollar, saleQualifier)) {
                    setupGWP(temp, nmOrderImpl, restrictedFreeGwpItems, 1);
                  } else {
                    removeGwpPromotion(nmOrderImpl, temp, activatedPromotions, REMOVE_ALL_ITEMS);
                  }
                  
                  break;
                }
                
                case 11: {
                  if (mPromotionsHelper.promoCodeMatch(order, promoCode, saleQualifier) && mPromotionsHelper.depictionDollarMatch(order, depiction, doubleDollar, saleQualifier)) {
                    setupGWP(temp, nmOrderImpl, restrictedFreeGwpItems, 1);
                  } else {
                    removeGwpPromotion(nmOrderImpl, temp, activatedPromotions, REMOVE_ALL_ITEMS);
                  }
                  
                  break;
                }
                
                case 12: {
                  if (mPromotionsHelper.itemDollarMatch(order, catalog, items, doubleDollar, saleQualifier)) {
                    setupGWP(temp, nmOrderImpl, restrictedFreeGwpItems, 1);
                  } else {
                    removeGwpPromotion(nmOrderImpl, temp, activatedPromotions, REMOVE_ALL_ITEMS);
                  }
                  
                  break;
                }
                
                case 13: {
                  if (mPromotionsHelper.promoCodeMatch(order, promoCode, saleQualifier) && mPromotionsHelper.itemDollarMatch(order, catalog, items, doubleDollar, saleQualifier)) {
                    setupGWP(temp, nmOrderImpl, restrictedFreeGwpItems, 1);
                  } else {
                    removeGwpPromotion(nmOrderImpl, temp, activatedPromotions, REMOVE_ALL_ITEMS);
                  }
                  
                  break;
                }
                
                case 14: {
                  if (mPromotionsHelper.catalogMatch(order, catalog, saleQualifier)) {
                    setupGWP(temp, nmOrderImpl, restrictedFreeGwpItems, 1);
                  } else {
                    removeGwpPromotion(nmOrderImpl, temp, activatedPromotions, REMOVE_ALL_ITEMS);
                  }
                  
                  break;
                }
                
                case 15: {
                  if (mPromotionsHelper.catalogDollarMatch(order, catalog, doubleDollar, saleQualifier)) {
                    setupGWP(temp, nmOrderImpl, restrictedFreeGwpItems, 1);
                  } else {
                    removeGwpPromotion(nmOrderImpl, temp, activatedPromotions, REMOVE_ALL_ITEMS);
                  }
                  
                  break;
                }
                
                case 16: {
                  if (mPromotionsHelper.promoCodeMatch(order, promoCode, saleQualifier) && mPromotionsHelper.catalogMatch(order, catalog, saleQualifier)) {
                    setupGWP(temp, nmOrderImpl, restrictedFreeGwpItems, 1);
                  } else {
                    removeGwpPromotion(nmOrderImpl, temp, activatedPromotions, REMOVE_ALL_ITEMS);
                  }
                  
                  break;
                }
                
                case 17: {
                  if (mPromotionsHelper.promoCodeMatch(order, promoCode, saleQualifier) && mPromotionsHelper.catalogDollarMatch(order, catalog, doubleDollar, saleQualifier)) {
                    setupGWP(temp, nmOrderImpl, restrictedFreeGwpItems, 1);
                  } else {
                    removeGwpPromotion(nmOrderImpl, temp, activatedPromotions, REMOVE_ALL_ITEMS);
                  }
                  
                  break;
                }
                
                case 18: {
                  if (mPromotionsHelper.vendorMatch(order, designer, saleQualifier)) {
                    setupGWP(temp, nmOrderImpl, restrictedFreeGwpItems, 1);
                  } else {
                    removeGwpPromotion(nmOrderImpl, temp, activatedPromotions, REMOVE_ALL_ITEMS);
                  }
                  
                  break;
                }
                
                case 19: {
                  if (mPromotionsHelper.vendorDollarMatch(order, designer, doubleDollar, saleQualifier)) {
                    setupGWP(temp, nmOrderImpl, restrictedFreeGwpItems, 1);
                  } else {
                    removeGwpPromotion(nmOrderImpl, temp, activatedPromotions, REMOVE_ALL_ITEMS);
                  }
                  
                  break;
                }
                
                case 20: {
                  if (mPromotionsHelper.promoCodeMatch(order, promoCode, saleQualifier) && mPromotionsHelper.vendorMatch(order, designer, saleQualifier)) {
                    setupGWP(temp, nmOrderImpl, restrictedFreeGwpItems, 1);
                  } else {
                    removeGwpPromotion(nmOrderImpl, temp, activatedPromotions, REMOVE_ALL_ITEMS);
                  }
                  
                  break;
                }
                
                case 21: {
                  if (mPromotionsHelper.promoCodeMatch(order, promoCode, saleQualifier) && mPromotionsHelper.vendorDollarMatch(order, designer, doubleDollar, saleQualifier)) {
                    setupGWP(temp, nmOrderImpl, restrictedFreeGwpItems, 1);
                  } else {
                    removeGwpPromotion(nmOrderImpl, temp, activatedPromotions, REMOVE_ALL_ITEMS);
                  }
                  
                  break;
                }
                
                case 22: {
                  if (mPromotionsHelper.deptClassMatch(order, department, classCode, onlineCatOnly, saleQualifier) && mPromotionsHelper.vendorMatch(order, designer, saleQualifier)) {
                    setupGWP(temp, nmOrderImpl, restrictedFreeGwpItems, 1);
                  } else {
                    removeGwpPromotion(nmOrderImpl, temp, activatedPromotions, REMOVE_ALL_ITEMS);
                  }
                  
                  break;
                }
                
                case 23: {
                  double customersTotal = 0;
                  double deptClassDollar = mPromotionsHelper.vendorDollarDeptClassMatch(order, designer, department, classCode, onlineCatOnly, doubleDollar, saleQualifier);
                  customersTotal = customersTotal + deptClassDollar;
                  
                  if (customersTotal >= doubleDollar) {
                    setupGWP(temp, nmOrderImpl, restrictedFreeGwpItems, 1);
                  } else {
                    removeGwpPromotion(nmOrderImpl, temp, activatedPromotions, REMOVE_ALL_ITEMS);
                  }
                  
                  break;
                }
                
                case 24: {
                  double deptClassDollar = mPromotionsHelper.vendorDollarDeptClassMatch(order, designer, department, classCode, onlineCatOnly, doubleDollar, saleQualifier);
                  double customersTotal = deptClassDollar;
                  
                  if (mPromotionsHelper.promoCodeMatch(order, promoCode, saleQualifier) && (customersTotal >= doubleDollar)) {
                    setupGWP(temp, nmOrderImpl, restrictedFreeGwpItems, 1);
                  } else {
                    removeGwpPromotion(nmOrderImpl, temp, activatedPromotions, REMOVE_ALL_ITEMS);
                  }
                  
                  break;
                }
                
                case 25: {
                  if (mPromotionsHelper.promoCodeMatch(order, promoCode, saleQualifier) && mPromotionsHelper.deptClassMatch(order, department, classCode, onlineCatOnly, saleQualifier)
                          && mPromotionsHelper.vendorMatch(order, designer, saleQualifier)) {
                    setupGWP(temp, nmOrderImpl, restrictedFreeGwpItems, 1);
                  } else {
                    removeGwpPromotion(nmOrderImpl, temp, activatedPromotions, REMOVE_ALL_ITEMS);
                  }
                  
                  break;
                }
                
                case 26: {
                  if (mPromotionsHelper.categoriesDollarMatch(order, categories, doubleDollar, saleQualifier)) {
                    setupGWP(temp, nmOrderImpl, restrictedFreeGwpItems, 1);
                  } else {
                    removeGwpPromotion(nmOrderImpl, temp, activatedPromotions, REMOVE_ALL_ITEMS);
                  }
                  
                  break;
                }
                
                case 27: {
                  // tempDollar = Double.valueOf("0.01");
                  doubleDollar = 0.01; // tempDollar.doubleValue();
                  if (mPromotionsHelper.promoCodeMatch(order, promoCode, saleQualifier) && mPromotionsHelper.categoriesDollarMatch(order, categories, doubleDollar, saleQualifier)) {
                    setupGWP(temp, nmOrderImpl, restrictedFreeGwpItems, 1);
                  } else {
                    removeGwpPromotion(nmOrderImpl, temp, activatedPromotions, REMOVE_ALL_ITEMS);
                  }
                  
                  break;
                }
                
                case 28: {
                  if (mPromotionsHelper.promoCodeMatch(order, promoCode, saleQualifier) && mPromotionsHelper.categoriesDollarMatch(order, categories, doubleDollar, saleQualifier)) {
                    setupGWP(temp, nmOrderImpl, restrictedFreeGwpItems, 1);
                  } else {
                    removeGwpPromotion(nmOrderImpl, temp, activatedPromotions, REMOVE_ALL_ITEMS);
                  }
                  
                  break;
                }
                
                case 29: {
                  if (mPromotionsHelper.multiDeptClassMatch(order, department, onlineCatOnly, saleQualifier)) {
                    setupGWP(temp, nmOrderImpl, restrictedFreeGwpItems, 1);
                  } else {
                    removeGwpPromotion(nmOrderImpl, temp, activatedPromotions, REMOVE_ALL_ITEMS);
                  }
                  
                  break;
                }
                
                case 30: {
                  if (mPromotionsHelper.promoCodeMatch(order, promoCode, saleQualifier) && mPromotionsHelper.multiDeptClassMatch(order, department, onlineCatOnly, saleQualifier)) {
                    setupGWP(temp, nmOrderImpl, restrictedFreeGwpItems, 1);
                  } else {
                    removeGwpPromotion(nmOrderImpl, temp, activatedPromotions, REMOVE_ALL_ITEMS);
                  }
                  
                  break;
                }
                
                case 31: {
                  if (mPromotionsHelper.multiDeptClassDollarMatch(order, department, doubleDollar, onlineCatOnly, saleQualifier)) {
                    setupGWP(temp, nmOrderImpl, restrictedFreeGwpItems, 1);
                  } else {
                    removeGwpPromotion(nmOrderImpl, temp, activatedPromotions, REMOVE_ALL_ITEMS);
                  }
                  
                  break;
                }
                
                case 32: {
                  if (mPromotionsHelper.promoCodeMatch(order, promoCode, saleQualifier) && mPromotionsHelper.multiDeptClassDollarMatch(order, department, doubleDollar, onlineCatOnly, saleQualifier)) {
                    setupGWP(temp, nmOrderImpl, restrictedFreeGwpItems, 1);
                  } else {
                    removeGwpPromotion(nmOrderImpl, temp, activatedPromotions, REMOVE_ALL_ITEMS);
                  }
                  
                  break;
                }
                
                case 33: {
                  if (mPromotionsHelper.multiDeptClassMatch(order, department, onlineCatOnly, saleQualifier) && mPromotionsHelper.vendorMatch(order, designer, saleQualifier)) {
                    setupGWP(temp, nmOrderImpl, restrictedFreeGwpItems, 1);
                  } else {
                    removeGwpPromotion(nmOrderImpl, temp, activatedPromotions, REMOVE_ALL_ITEMS);
                  }
                  
                  break;
                }
                
                case 34: {
                  if (mPromotionsHelper.promoCodeMatch(order, promoCode, saleQualifier) && mPromotionsHelper.multiDeptClassMatch(order, department, onlineCatOnly, saleQualifier)
                          && mPromotionsHelper.vendorMatch(order, designer, saleQualifier)) {
                    setupGWP(temp, nmOrderImpl, restrictedFreeGwpItems, 1);
                  } else {
                    removeGwpPromotion(nmOrderImpl, temp, activatedPromotions, REMOVE_ALL_ITEMS);
                  }
                  
                  break;
                }
                
                case 35: {
                  if (mPromotionsHelper.multiDeptClassVendorDollarMatch(order, department, designer, doubleDollar, onlineCatOnly, saleQualifier)) {
                    setupGWP(temp, nmOrderImpl, restrictedFreeGwpItems, 1);
                  } else {
                    removeGwpPromotion(nmOrderImpl, temp, activatedPromotions, REMOVE_ALL_ITEMS);
                  }
                  
                  break;
                }
                
                case 36: {
                  if (mPromotionsHelper.promoCodeMatch(order, promoCode, saleQualifier)
                          && mPromotionsHelper.multiDeptClassVendorDollarMatch(order, department, designer, doubleDollar, onlineCatOnly, saleQualifier)) {
                    setupGWP(temp, nmOrderImpl, restrictedFreeGwpItems, 1);
                  } else {
                    removeGwpPromotion(nmOrderImpl, temp, activatedPromotions, REMOVE_ALL_ITEMS);
                  }
                  
                  break;
                }
                
                case 37: {
                  Boolean multiGwpPromotionFlag = temp.getMultiGWP();
                  boolean isMultiGwpPromotion = (multiGwpPromotionFlag == null) ? false : multiGwpPromotionFlag.booleanValue();
                  
                  long theMultiQty = 1;
                  
                  if (mPromotionsHelper.orderHasSaleChkProd(order, promoKey, saleQualifier)) {
                    if (isMultiGwpPromotion) {
                      theMultiQty = mPromotionsHelper.determineMultiQty(order, promoKey, saleQualifier);
                    }
                    
                    if (theMultiQty == 0) {
                      removeGwpPromotion(nmOrderImpl, temp, activatedPromotions, REMOVE_ALL_ITEMS);
                    } else {
                      String cmosItem = temp.getGwpItem();
                      RepositoryItem promotionalItemRI = mPromotionsHelper.getProdItem(cmosItem, promoKey);
                      String promotionalProductId = promotionalItemRI.getRepositoryId();
                      
                      long quantityAwarded = mPromotionsHelper.getGwpQuantityAwarded(order, promotionalProductId);
                      
                      if (quantityAwarded < theMultiQty) {
                        setupGWP(temp, nmOrderImpl, restrictedFreeGwpItems, theMultiQty - quantityAwarded);
                      } else if (quantityAwarded > theMultiQty) {
                        removeGwpPromotion(nmOrderImpl, temp, activatedPromotions, quantityAwarded - theMultiQty);
                      }
                    }
                  } else {
                    removeGwpPromotion(nmOrderImpl, temp, activatedPromotions, REMOVE_ALL_ITEMS);
                  }
                  
                  break;
                }
                
                case 38: {
                  if ((mPromotionsHelper.orderHasProdDollar(order, promoKey, temp.getDollarQualifier().trim(), saleQualifier)) && !((NMOrderImpl) order).getUsedLimitPromoMap().contains(promoKey)) {
                    setupGWP(temp, nmOrderImpl, restrictedFreeGwpItems, 1);
                  } else {
                    removeGwpPromotion(nmOrderImpl, temp, activatedPromotions, REMOVE_ALL_ITEMS);
                  }
                  
                  break;
                }
                
                case 39: {
                  if (mPromotionsHelper.promoCodeMatch(order, promoCode, saleQualifier)
                          && (mPromotionsHelper.orderHasProdDollar(order, promoKey, temp.getDollarQualifier().trim(), saleQualifier) && !((NMOrderImpl) order).getUsedLimitPromoMap()
                                  .contains(promoKey))) {
                    setupGWP(temp, nmOrderImpl, restrictedFreeGwpItems, 1);
                  } else {
                    removeGwpPromotion(nmOrderImpl, temp, activatedPromotions, REMOVE_ALL_ITEMS);
                  }
                  
                  break;
                }
              }
            }
          }
          
          mOrderManager.updateOrder(order);
        } catch (Exception ce) {
          ce.printStackTrace();
          throw new ScenarioException(ce);
        }
      }
    } catch (Exception e) {
      rollBack = true;
      logError("CheckCSRGWP.executeAction(): Exception ");
      throw new PromoException(e);
    } finally {
      try {
        td.end(rollBack); // commit work
      } catch (TransactionDemarcationException tde) {
        tde.printStackTrace();
        throw new PromoException(tde);
      }
    }
    
  } // end of executeAction method
  
  public String getCurrentCatalogCode() {
    String catcode = getSystemSpecs().getCatalogCode();
    
    return catcode;
  }
  
  private SystemSpecs getSystemSpecs() {
    return (SystemSpecs) Nucleus.getGlobalNucleus().resolveName("/nm/utils/SystemSpecs");
  }
  
  /**
     * 
     */
  private void setupGWP(GiftWithPurchase promotion, NMOrderImpl order, HashSet restrictedFreeGwpItems, long quantity) throws Exception {
    String promoCode = new String(promotion.getPromoCodes().trim().toUpperCase());
    String promoKey = new String(promotion.getCode().trim().toUpperCase());
    String cmosItem = new String(promotion.getGwpItem().trim().toUpperCase());
    
    RepositoryItem promotionalItemRI = mPromotionsHelper.getProdItem(cmosItem, promoKey);
    
    // it's possible that a promotion may exist for items that are no longer in the
    // product repository so the RI could be null.
    if (promotionalItemRI != null) {
      String promotionalProductId = promotionalItemRI.getRepositoryId();
      boolean hasValidShipToLocations = true;
      
      if (order.shouldValidateCountry()) {
        hasValidShipToLocations = mPromotionsHelper.getFirstValidShippingGroupForGwp(promotionalItemRI, order) != null;
      }
      
      if (hasValidShipToLocations) {
        final boolean promoAwarded = mPromotionsHelper.promoAlreadyAwarded(order, promotionalProductId);
        
        setSendCmosPromoKey(order,promoKey,cmosItem);
        
        // if the gwp is already in the cart make sure the promo name is set on the order
        if (promoAwarded) {
          order.setPromoName(promoKey);
        }
        order.addAwardedPromotion(promotion);
        if (promotion.isMultiGWP() || promotion.isMultiGWPDollarQualActive() || !promoAwarded) {
          synchronized (order) {
            List skuItems = (List) promotionalItemRI.getPropertyValue("childSKUs");
            Double thePrice = (Double) promotionalItemRI.getPropertyValue("retailPrice");
            Boolean flgDisplay = (Boolean) promotionalItemRI.getPropertyValue("flgDisplay");
            
            boolean flagDisplay = flgDisplay.booleanValue();
            boolean zeroPrice = false;
            
            if (thePrice.toString().trim().equals("0.0")) {
              zeroPrice = true;
            }
            
            if ((skuItems.size() == 1) && zeroPrice && flagDisplay) {
              RepositoryItem skuItem = (RepositoryItem) skuItems.get(0);
              
              if (skuItem == null) {
                throw new ScenarioException(PromotionConstants.getStringResource(PromotionConstants.SKU_DOES_NOT_EXIST));
              }
              
              String skuId = skuItem.getRepositoryId();
              
              if (promotion.isMultiGWPDollarQualActive()) {
                quantity = mPromotionsHelper.determineMultiDollarHurdleQty(order, promotion);
                if (quantity < 1) {
                  if (promoAwarded) {
                    if (removeGwpItem(order, cmosItem, promoKey, REMOVE_ALL_ITEMS)) {
                      order.setRemoveActivatedPromoCode(promoCode);
                      order.setRemoveUserActivatedPromoCode(promoCode);
                      order.removeIgnoredPromoCode(NMOrderImpl.GWP_PROMOTIONS, promoCode);
                      order.setRemovePromoName(promoKey);
                      mOrderManager.updateOrder(order);
                    }
                  }
                  return;
                }
              }
              
              if (promoAwarded) {
                final long qtyOnOrder = mPromotionsHelper.countPromoProductQty(order, promotionalProductId);
                if (qtyOnOrder != quantity) {
                  if (promotion.isMultiGWPDollarQualActive()) {
                    try {
                      ShoppingCartHandler.mergeSplitCommerceItems(order, (NMOrderManager) mOrderManager, true, true, false);
                    } catch (final CommerceException e) {
                      e.printStackTrace();
                    }
                  }
                  
                  mPromotionsHelper.updatePromoProductQty(order, promotionalProductId, quantity);
                  
                  if (promotion.isMultiGWPDollarQualActive() && promotion.isGWPShipwithItemActive()) {
                    final DynamoHttpServletRequest req = ServletUtil.getCurrentRequest();
                    final ShoppingCartHandler sc = (ShoppingCartHandler) req.resolveName("/atg/commerce/order/ShoppingCartModifier");
                    sc.evaluateGwpMustShipWith();
                  }
                }
              } else {
                mPromotionsHelper.addGwpPromotionToOrder(promotion, PROMO_TYPE, PROMO_NAME, restrictedFreeGwpItems, order, skuId, quantity);
              }
            } else {
              if ((skuItems.size() > 1) && zeroPrice && flagDisplay) {
                long quantityEntitledTo = mPromotionsHelper.determineMultiDollarHurdleQty(order, promotion);
                long quantityInCart = mPromotionsHelper.countPromoProductQty(order, promotionalProductId);
                
                if ((quantityEntitledTo < 1) && (promoAwarded)) {
                  if (removeGwpItem(order, cmosItem, promoKey, REMOVE_ALL_ITEMS)) {
                    order.setRemoveActivatedPromoCode(promoCode);
                    order.setRemoveUserActivatedPromoCode(promoCode);
                    order.removeIgnoredPromoCode(NMOrderImpl.GWP_PROMOTIONS, promoCode);
                    order.setRemovePromoName(promoKey);
                    mOrderManager.updateOrder(order);
                  }
                  
                  return;
                }
                
                if (!promoAwarded) {
                  order.addGwpMultiSkuPromoToMap(promoKey, promoCode, promotionalProductId, quantityEntitledTo);
                  order.addIgnoredPromoCode(NMOrderImpl.GWP_PROMOTIONS, promoCode);
                } else {
                  if (quantityEntitledTo == quantityInCart) {
                    // remove this entry from the promo map so that user is not prompted to add more gwps
                    order.removeGwpMultiSkuPromoMapEntry(promoKey);
                  } else if (quantityEntitledTo > quantityInCart) {
                    // set the quantity to the amount that still needs to added to cart
                    order.addGwpMultiSkuPromoToMap(promoKey, promoCode, promotionalProductId, (quantityEntitledTo - quantityInCart));
                  } else if (quantityEntitledTo < quantityInCart) {
                    // need to remove the gwps that the user no longer qualifies for
                    removeGwpItem(order, cmosItem, promoKey, (quantityInCart - quantityEntitledTo));
                    // remove this entry from the promo map so that user is not prompted to add more gwps
                    order.removeGwpMultiSkuPromoMapEntry(promoKey);
                  }
                }
              }
            }
          }
        }
      }
    }
  }// endsetupGWP
  
  /*
   * In case session gets expire, this method will reset the sendcmospromokey of promotional commerceitem
   */
  private void setSendCmosPromoKey(NMOrderImpl order, String promoKey, String cmosItem) throws Exception{
	  
	      List commerceItemsList = order.getCommerceItems();
	      
	      ArrayList commerceItems = new ArrayList(commerceItemsList);
	      
	      Iterator iterator = commerceItems.iterator();
	      
	      RepositoryItem promoProdItem = mPromotionsHelper.getProdItem(cmosItem, promoKey);
	      String promotionProductId = promoProdItem.getRepositoryId();
	      
	      while (iterator.hasNext()) {
	        final CommerceItem commerceItem = (CommerceItem) iterator.next();
	        
	        if (commerceItem instanceof NMCommerceItem) {
	          NMCommerceItem nmCommerceItem = (NMCommerceItem) commerceItem;
	          RepositoryItem productItem = (RepositoryItem) nmCommerceItem.getAuxiliaryData().getProductRef();
	          
	          if (productItem != null) {
	            List activatedPromotions = nmCommerceItem.getSendCmosPromoKeyList();
	            
	            String commerceItemProductId = (String) productItem.getPropertyValue("id");
	            
	            if((commerceItemProductId != null) && (promotionProductId.equals(commerceItemProductId))){
	            	
	              if(activatedPromotions.isEmpty() || activatedPromotions == null){
	            		nmCommerceItem.setSendCmosPromoKey(promoKey);
	              }
	            }
	         }
	      }
	    }
      }
  /**
   * 
   * @param order
   * @param promotion
   */
  private void removeGwpPromotion(NMOrderImpl order, GiftWithPurchase promotion, List activatedPromotions, long quantityToRemove) throws ScenarioException {
    if ((activatedPromotions != null) && activatedPromotions.contains(promotion.getCode())) {
      synchronized (order) {
        try {
          String promoKey = promotion.getCode();
          String promoCode = promotion.getPromoCodes();
          String cmosItem = promotion.getGwpItem().trim().toUpperCase();
          
          if (removeGwpItem(order, cmosItem, promoKey, quantityToRemove)) {
            
            if (quantityToRemove == REMOVE_ALL_ITEMS) {
              order.setRemoveActivatedPromoCode(promoCode);
              order.setRemoveUserActivatedPromoCode(promoCode);
              order.removeIgnoredPromoCode(NMOrderImpl.GWP_PROMOTIONS, promoCode);
              order.setRemovePromoName(promoKey);
            }
            
            mOrderManager.updateOrder(order);
          }
          
        } catch (Exception exception) {
          throw new ScenarioException(exception);
        }
      }
    }
  }
  
  /**
   * 
   * @param order
   * @param promotionalItem
   * @param promoKey
   */
  private boolean removeGwpItem(NMOrderImpl order, String cmosItem, String promoKey, long quantityToRemove) throws Exception {
    boolean returnValue = false;
    
    synchronized (order) {
      List commerceItemsList = order.getCommerceItems();
      
      // copy the commerceItems to another list so that they can be
      // removed from the order without generating a concurrent
      // modification
      // exception on the order's commerce item collection.
      ArrayList commerceItems = new ArrayList(commerceItemsList);
      
      Iterator iterator = commerceItems.iterator();
      
      RepositoryItem promoProdItem = mPromotionsHelper.getProdItem(cmosItem, promoKey);
      String promotionProductId = promoProdItem.getRepositoryId();
      
      while (iterator.hasNext() && (quantityToRemove > 0)) {
        final CommerceItem commerceItem = (CommerceItem) iterator.next();
        
        if (commerceItem instanceof NMCommerceItem) {
          NMCommerceItem nmCommerceItem = (NMCommerceItem) commerceItem;
          RepositoryItem productItem = (RepositoryItem) nmCommerceItem.getAuxiliaryData().getProductRef();
          
          if (productItem != null) {
            List activatedPromotions = nmCommerceItem.getSendCmosPromoKeyList();
            
            String commerceItemProductId = (String) productItem.getPropertyValue("id");
            
            // if the product id of the commerce item matches the
            // product id of the
            // the promotional item and the commerce item has the
            // promo key
            // then remove the item from the order.
            if ((promotionProductId != null) && (commerceItemProductId != null) && (promotionProductId.equals(commerceItemProductId) && activatedPromotions.contains(promoKey))) {
              
              long quantity = commerceItem.getQuantity();
              
              if (quantity > quantityToRemove) {
                mPromotionsHelper.updatePromoProductQty(order, productItem.getRepositoryId(), (quantity - quantityToRemove));
                logDebug("GWP ci=" + commerceItem.getId() + " qty adjusted to " + (quantity - quantityToRemove));
                quantityToRemove = 0;
              } else {
                quantityToRemove -= quantity;
                // Add the recently removed promo item into order's recentlyChangedCommerceItem bean 
                CheckoutAPI.setRecentlyChangedItem(order, nmCommerceItem, quantity, 0L, INMGenericConstants.PROMO, INMGenericConstants.REMOVE, null);
                mOrderManager.getCommerceItemManager().removeItemFromOrder(order, commerceItem.getId());
                logDebug("GWP ci=" + commerceItem.getId() + " qty=" + commerceItem.getQuantity() + " removed from order");
              }
              
              returnValue = true;
              logDebug("removeCSRGWP-the order version-------------------->" + order.getVersion());
            }
          }
        }
      }
      
      mOrderManager.updateOrder(order);
    }
    
    return returnValue;
  }
  
  /***************************************************************************
   * 
   * This is the start of the common logic for the checkCsrGWP & the removeCsrGWP. If you make a change to one of the utility methods you need to make it in the other too.
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
      Nucleus.getGlobalNucleus().logError("checkCsrGWP: " + sin);
    }
  }
  
  private void logDebug(String sin) {
    if (isLoggingDebug()) {
      Nucleus.getGlobalNucleus().logDebug("checkCsrGWP: " + sin);
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
