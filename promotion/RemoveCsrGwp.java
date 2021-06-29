package com.nm.commerce.promotion;

import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.transaction.TransactionManager;

import atg.commerce.CommerceException;
import atg.commerce.order.CommerceItem;
import atg.commerce.order.CommerceItemManager;
import atg.commerce.order.CommerceItemRelationship;
import atg.commerce.order.Order;
import atg.commerce.order.OrderHolder;
import atg.commerce.order.OrderManager;
import atg.commerce.order.OrderQueries;
import atg.commerce.promotion.PromotionConstants;
import atg.commerce.states.OrderStates;
import atg.commerce.states.StateDefinitions;
import atg.commerce.util.NoLockNameException;
import atg.commerce.util.TransactionLockFactory;
import atg.commerce.util.TransactionLockService;
import atg.dtm.TransactionDemarcation;
import atg.dtm.TransactionDemarcationException;
import atg.nucleus.Nucleus;
import atg.nucleus.naming.ComponentName;
import atg.process.ProcessExecutionContext;
import atg.process.action.ActionImpl;
import atg.repository.MutableRepositoryItem;
import atg.repository.Repository;
import atg.repository.RepositoryException;
import atg.repository.RepositoryItem;
import atg.scenario.ScenarioException;
import atg.service.lockmanager.DeadlockException;
import atg.service.lockmanager.LockManagerException;
import atg.servlet.DynamoHttpServletRequest;
import atg.servlet.ServletUtil;

import com.nm.collections.GiftWithPurchase;
import com.nm.collections.GiftWithPurchaseArray;
import com.nm.collections.NMPromotion;
import com.nm.commerce.NMCommerceItem;
import com.nm.commerce.checkout.CheckoutAPI;
import com.nm.common.INMGenericConstants;
import com.nm.components.CommonComponentHelper;
import com.nm.utils.ExceptionUtil;
import com.nm.utils.PromotionsHelper;
import com.nm.utils.SystemSpecs;

/**
 * RemoveCsrGwp
 * 
 * This action was developed for the CSR GWP tool. It allows the user to create a GWP repository item with the CSR tool and generic scenario will call this action on a item removed from cart event.
 * This action will get a refernce to the GiftWithPurchaseArray component which will return an ArrayList of all the Promotions in the repository. This action will iterate thru every Promotion and
 * determine is the date current. If the date is valid then it will look at the characteristics of that type and determine if the current order needs to have the GWP removed.
 * 
 * @author Todd Schultz
 * @since 08/20/2004
 */

public class RemoveCsrGwp extends ActionImpl implements IPromoAction {
  // -------------------------------------
  /** Class version string */
  public static final String CLASS_VERSION = "$Id: RemoveCsrGwp.java 1.24 2013/03/20 13:28:36CDT Richard A Killen   (NMRAK3) Exp  $";

/** Parameter for the location of the ordermanager */
  public String ORDERMANAGER_PATH = "/atg/commerce/order/OrderManager";
  public static final String PROMOHELPER_PATH = "/nm/utils/PromotionsHelper";
  public static final String SHOPPING_CART_PATH = "/atg/commerce/ShoppingCart";
  public static final String PRODUCT_CATALOG_PATH = "/atg/commerce/catalog/ProductCatalog";
  public static final String GWP_ARRAY_PATH = "/nm/collections/GiftWithPurchaseArray";
  
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
  public GiftWithPurchaseArray thePromos = null;
  public Repository repository = null;
  
  private boolean logDebug;
  private boolean logError = true;
  private boolean logWarning;
  
  public void initialize(Map pParameters) throws ScenarioException {
    /** Resolve OrderManager and Promotion components. */
    logDebug = getSystemSpecs().getCsrScenarioDebug();
    logDebug("RemoveCsrGwp is Initializing");
    mOrderManager = (OrderManager) Nucleus.getGlobalNucleus().resolveName(ORDERMANAGER_PATH);
    
    if (mOrderManager == null) throw new ScenarioException(PromotionConstants.getStringResource(PromotionConstants.ORDER_MANAGER_NOT_FOUND));
    
    // mItemManager = mOrderManager.getCommerceItemManager();
    mItemManager = (CommerceItemManager) Nucleus.getGlobalNucleus().resolveName("/atg/commerce/order/CommerceItemManager");
    mOrderQueries = mOrderManager.getOrderQueries();
    
    // storeOptionalParameter(pParameters, CMOSSKU_PARAM,
    // java.lang.String.class);
    // storeOptionalParameter(pParameters, PROMO_TYPE_PARAM,
    // java.lang.String.class);
    mOrderHolderComponent = ComponentName.getComponentName(SHOPPING_CART_PATH);
    thePromos = (GiftWithPurchaseArray) Nucleus.getGlobalNucleus().resolveName(GWP_ARRAY_PATH);
    repository = (Repository) Nucleus.getGlobalNucleus().resolveName(PRODUCT_CATALOG_PATH);
    mPromotionsHelper = (PromotionsHelper) Nucleus.getGlobalNucleus().resolveName(PROMOHELPER_PATH);
    // System.out.println("#######################################RemoveCsrGwp
    // initialized");
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
  
  protected void executeAction(ProcessExecutionContext pContext) throws ScenarioException {
    logDebug("just hit removeCSRGWP");
    try {
      Order order = getOrderToModify(pContext);
      evaluatePromo(order);
    } catch (final Exception e) {
      throw new ScenarioException(ExceptionUtil.getExceptionInfo(e), e);
    }
  }
  
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
      
      if (mOrderManager == null) throw new ScenarioException(PromotionConstants.getStringResource(PromotionConstants.ORDER_MANAGER_NOT_FOUND));
      
      // Check if it is a message of type ItemRemovedFromOrder
      
      
      try {
        
        Date mystartDate = Calendar.getInstance().getTime();
        String theKey = "";
        String theCat = "";
        String theDept = "";
        String theClass = "";
        String theItems = "";
        String onlineCatTest = "";
        String theDesigner = "";
        String theDepiction = "";
        String theCategories = "";
        Double tempDollar = new Double(0.0);
        String saleQualifier = null;
        double doubleDollar = .01;
        boolean onlineCatOnly = false;
        Map<String, NMPromotion> promoArray = thePromos.getAllActivePromotions();
        Iterator<NMPromotion> promoIter = promoArray.values().iterator();
        while (promoIter.hasNext()) {
          GiftWithPurchase temp = (GiftWithPurchase) promoIter.next();
          saleQualifier = temp.getSaleQualificationName();
          
            if (temp.getType().trim().equals("1")) {
              theKey = temp.getCode().trim().toUpperCase();
              
              if (keyMatch(order, theKey) && (!mPromotionsHelper.dollarQualifies(order, temp.getDollarQualifier(), saleQualifier))) {
                setupGWP(temp, order);
              }// end order $ qualifies
            }// is type1
            else if (temp.getType().trim().equals("2")) {
              theKey = temp.getCode().trim().toUpperCase();
              
              if (keyMatch(order, theKey) && (order.getPriceInfo().getAmount() < doubleDollar)) {
                setupGWP(temp, order);
              }// end order $ qualifies
            }// is type2
            else if (temp.getType().trim().equals("3")) {
              theKey = temp.getCode().trim().toUpperCase();
              
              if (keyMatch(order, theKey) && (!mPromotionsHelper.dollarQualifies(order, temp.getDollarQualifier(), saleQualifier))) {
                setupGWP(temp, order);
              }// end order $ qualifies
            }// is type3
            else if (temp.getType().trim().equals("4")) {
              theKey = temp.getCode().trim().toUpperCase();
              theDept = temp.getDeptCodes().trim().toUpperCase();
              theClass = temp.getClassCodes().trim().toUpperCase();
              onlineCatTest = temp.getOnlineCatOnly().trim().toUpperCase();
              
              if (onlineCatTest.equals("TRUE")) {
                onlineCatOnly = true;
              } else {
                onlineCatOnly = false;
              }
              
              if (keyMatch(order, theKey) && (!mPromotionsHelper.deptClassMatch(order, theDept, theClass, onlineCatOnly, saleQualifier))) {
                setupGWP(temp, order);
              }// end order $ qualifies
            }// is type4
            else if (temp.getType().trim().equals("5")) {
              theKey = temp.getCode().trim().toUpperCase();
              theDept = temp.getDeptCodes().trim().toUpperCase();
              theClass = temp.getClassCodes().trim().toUpperCase();
              onlineCatTest = temp.getOnlineCatOnly().trim().toUpperCase();
              if (onlineCatTest.equals("TRUE")) {
                onlineCatOnly = true;
              } else {
                onlineCatOnly = false;
              }
              
              if (keyMatch(order, theKey) && (!mPromotionsHelper.deptClassMatch(order, theDept, theClass, onlineCatOnly, saleQualifier))) {
                setupGWP(temp, order);
              }// end order $ qualifies
            }// is type5
            else if (temp.getType().trim().equals("6")) {
              theKey = temp.getCode().trim().toUpperCase();
              theDept = temp.getDeptCodes().trim().toUpperCase();
              theClass = temp.getClassCodes().trim().toUpperCase();
              tempDollar = Double.valueOf(temp.getDollarQualifier());
              doubleDollar = tempDollar.doubleValue();
              onlineCatTest = temp.getOnlineCatOnly().trim().toUpperCase();
              
              if (onlineCatTest.equals("TRUE")) {
                onlineCatOnly = true;
              } else {
                onlineCatOnly = false;
              }
              
              if (keyMatch(order, theKey) && (!mPromotionsHelper.deptClassDollarMatch(order, theDept, theClass, doubleDollar, onlineCatOnly, saleQualifier))) {
                setupGWP(temp, order);
              }// end order $ qualifies
            }// is type6
            else if (temp.getType().trim().equals("7")) {
              theKey = temp.getCode().trim().toUpperCase();
              theDept = temp.getDeptCodes().trim().toUpperCase();
              theClass = temp.getClassCodes().trim().toUpperCase();
              tempDollar = Double.valueOf(temp.getDollarQualifier());
              doubleDollar = tempDollar.doubleValue();
              
              onlineCatTest = temp.getOnlineCatOnly().trim().toUpperCase();
              
              if (onlineCatTest.equals("TRUE")) {
                onlineCatOnly = true;
              } else {
                onlineCatOnly = false;
              }
              
              if (keyMatch(order, theKey) && (!mPromotionsHelper.deptClassDollarMatch(order, theDept, theClass, doubleDollar, onlineCatOnly, saleQualifier))) {
                setupGWP(temp, order);
              }// end order $ qualifies
            }// is type7
            else if (temp.getType().trim().equals("8")) {
              theKey = temp.getCode().trim().toUpperCase();
              theDepiction = temp.getDepiction().trim().toUpperCase();
              
              if (keyMatch(order, theKey) && (!mPromotionsHelper.depictionMatch(order, theDepiction, saleQualifier))) {
                setupGWP(temp, order);
              }// end order $ qualifies
            }// is type8
            else if (temp.getType().trim().equals("9")) {
              theKey = temp.getCode().trim().toUpperCase();
              theDepiction = temp.getDepiction().trim().toUpperCase();
              
              if (keyMatch(order, theKey) && (!mPromotionsHelper.depictionMatch(order, theDepiction, saleQualifier))) {
                setupGWP(temp, order);
              }// end order $ qualifies
            }// is type9
            else if (temp.getType().trim().equals("10")) {
              theKey = temp.getCode().trim().toUpperCase();
              theDepiction = temp.getDepiction().trim().toUpperCase();
              
              tempDollar = Double.valueOf(temp.getDollarQualifier());
              doubleDollar = tempDollar.doubleValue();
              if (keyMatch(order, theKey) && (!mPromotionsHelper.depictionDollarMatch(order, theDepiction, doubleDollar, saleQualifier))) {
                setupGWP(temp, order);
              }// end order $ qualifies
            }// is type10
            else if (temp.getType().trim().equals("11")) {
              theKey = temp.getCode().trim().toUpperCase();
              theDepiction = temp.getDepiction().trim().toUpperCase();
              
              tempDollar = Double.valueOf(temp.getDollarQualifier());
              doubleDollar = tempDollar.doubleValue();
              if (keyMatch(order, theKey) && (!mPromotionsHelper.depictionDollarMatch(order, theDepiction, doubleDollar, saleQualifier))) {
                setupGWP(temp, order);
              }// end order $ qualifies
            }// is type11
            else if (temp.getType().trim().equals("12")) {
              theKey = temp.getCode().trim().toUpperCase();
              theCat = temp.getQualifyCatalog().trim().toUpperCase();
              theItems = temp.getQualifyItems().trim().toUpperCase();
              
              tempDollar = Double.valueOf(temp.getDollarQualifier());
              doubleDollar = tempDollar.doubleValue();
              if (keyMatch(order, theKey) && (!mPromotionsHelper.itemDollarMatch(order, theCat, theItems, doubleDollar, saleQualifier))) {
                setupGWP(temp, order);
              }// end order $ qualifies
            }// is type12
            else if (temp.getType().trim().equals("13")) {
              theKey = temp.getCode().trim().toUpperCase();
              theCat = temp.getQualifyCatalog().trim().toUpperCase();
              theItems = temp.getQualifyItems().trim().toUpperCase();
              
              tempDollar = Double.valueOf(temp.getDollarQualifier());
              doubleDollar = tempDollar.doubleValue();
              if (keyMatch(order, theKey) && (!mPromotionsHelper.itemDollarMatch(order, theCat, theItems, doubleDollar, saleQualifier))) {
                setupGWP(temp, order);
              }// end order $ qualifies
            }// is type13
            else if (temp.getType().trim().equals("14")) {
              theKey = temp.getCode().trim().toUpperCase();
              theCat = temp.getQualifyCatalog().trim().toUpperCase();
              
              if (keyMatch(order, theKey) && (!mPromotionsHelper.catalogMatch(order, theCat, saleQualifier))) {
                setupGWP(temp, order);
              }// end order $ qualifies
            }// is type14
            else if (temp.getType().trim().equals("15")) {
              theKey = temp.getCode().trim().toUpperCase();
              theCat = temp.getQualifyCatalog().trim().toUpperCase();
              
              tempDollar = Double.valueOf(temp.getDollarQualifier());
              doubleDollar = tempDollar.doubleValue();
              if (keyMatch(order, theKey) && (!mPromotionsHelper.catalogDollarMatch(order, theCat, doubleDollar, saleQualifier))) {
                setupGWP(temp, order);
              }// end order $ qualifies
            }// is type15
            else if (temp.getType().trim().equals("16")) {
              theKey = temp.getCode().trim().toUpperCase();
              theCat = temp.getQualifyCatalog().trim().toUpperCase();
              
              if (keyMatch(order, theKey) && (!mPromotionsHelper.catalogMatch(order, theCat, saleQualifier))) {
                setupGWP(temp, order);
              }// end order $ qualifies
            }// is type16
            else if (temp.getType().trim().equals("17")) {
              theKey = temp.getCode().trim().toUpperCase();
              theCat = temp.getQualifyCatalog().trim().toUpperCase();
              
              tempDollar = Double.valueOf(temp.getDollarQualifier());
              doubleDollar = tempDollar.doubleValue();
              if (keyMatch(order, theKey) && (!mPromotionsHelper.catalogDollarMatch(order, theCat, doubleDollar, saleQualifier))) {
                setupGWP(temp, order);
              }// end order $ qualifies
            }// is type17
            else if (temp.getType().trim().equals("18")) {
              theKey = temp.getCode().trim().toUpperCase();
              theDesigner = temp.getVendor().trim().toUpperCase();
              
              if (keyMatch(order, theKey) && (!mPromotionsHelper.vendorMatch(order, theDesigner, saleQualifier))) {
                setupGWP(temp, order);
              }// end order $ qualifies
            }// is type18
            else if (temp.getType().trim().equals("19")) {
              theKey = temp.getCode().trim().toUpperCase();
              theDesigner = temp.getVendor().trim().toUpperCase();
              
              tempDollar = Double.valueOf(temp.getDollarQualifier());
              doubleDollar = tempDollar.doubleValue();
              if (keyMatch(order, theKey) && (!mPromotionsHelper.vendorDollarMatch(order, theDesigner, doubleDollar, saleQualifier))) {
                setupGWP(temp, order);
              }// end order $ qualifies
            }// is type19
            else if (temp.getType().trim().equals("20")) {
              theKey = temp.getCode().trim().toUpperCase();
              theDesigner = temp.getVendor().trim().toUpperCase();
              
              if (keyMatch(order, theKey) && (!mPromotionsHelper.vendorMatch(order, theDesigner, saleQualifier))) {
                setupGWP(temp, order);
              }// end order $ qualifies
            }// is type20
            else if (temp.getType().trim().equals("21")) {
              theKey = temp.getCode().trim().toUpperCase();
              theDesigner = temp.getVendor().trim().toUpperCase();
              
              tempDollar = Double.valueOf(temp.getDollarQualifier());
              doubleDollar = tempDollar.doubleValue();
              if (keyMatch(order, theKey) && (!mPromotionsHelper.vendorDollarMatch(order, theDesigner, doubleDollar, saleQualifier))) {
                setupGWP(temp, order);
              }// end order $ qualifies
            }// is type21
            else if (temp.getType().trim().equals("22")) {
              theKey = temp.getCode().trim().toUpperCase();
              theDept = temp.getDeptCodes().trim().toUpperCase();
              theClass = temp.getClassCodes().trim().toUpperCase();
              theDesigner = temp.getVendor().trim().toUpperCase();
              
              onlineCatTest = temp.getOnlineCatOnly().trim().toUpperCase();
              
              if (onlineCatTest.equals("TRUE")) {
                onlineCatOnly = true;
              } else {
                onlineCatOnly = false;
              }
              
              if (keyMatch(order, theKey)
                      && ((!mPromotionsHelper.deptClassMatch(order, theDept, theClass, onlineCatOnly, saleQualifier)) || (!mPromotionsHelper.vendorMatch(order, theDesigner, saleQualifier)))) {
                setupGWP(temp, order);
              }// end order $ qualifies
            }// is type22
            else if (temp.getType().trim().equals("23")) {
              
              theKey = temp.getCode().trim().toUpperCase();
              theDesigner = temp.getVendor().trim().toUpperCase();
              theDept = temp.getDeptCodes().trim().toUpperCase();
              theClass = temp.getClassCodes().trim().toUpperCase();
              tempDollar = Double.valueOf(temp.getDollarQualifier());
              onlineCatTest = temp.getOnlineCatOnly().trim().toUpperCase();
              
              if (onlineCatTest.equals("TRUE")) {
                onlineCatOnly = true;
              } else {
                onlineCatOnly = false;
              }
              doubleDollar = tempDollar.doubleValue();
              double customersTotal = 0;
              
              double deptClassDollar = mPromotionsHelper.vendorDollarDeptClassMatch(order, theDesigner, theDept, theClass, onlineCatOnly, doubleDollar, saleQualifier);
              customersTotal = customersTotal + deptClassDollar;
              
              if (keyMatch(order, theKey) && (customersTotal < doubleDollar)) {
                setupGWP(temp, order);
              }// end order $ qualifies
            }// is type23
            else if (temp.getType().trim().equals("24")) {
              theKey = temp.getCode().trim().toUpperCase();
              theDesigner = temp.getVendor().trim().toUpperCase();
              theDept = temp.getDeptCodes().trim().toUpperCase();
              theClass = temp.getClassCodes().trim().toUpperCase();
              tempDollar = Double.valueOf(temp.getDollarQualifier());
              double customersTotal = 0;
              onlineCatTest = temp.getOnlineCatOnly().trim().toUpperCase();
              
              if (onlineCatTest.equals("TRUE")) {
                onlineCatOnly = true;
              } else {
                onlineCatOnly = false;
              }
              doubleDollar = tempDollar.doubleValue();
              
              double deptClassDollar = mPromotionsHelper.vendorDollarDeptClassMatch(order, theDesigner, theDept, theClass, onlineCatOnly, doubleDollar, saleQualifier);
              customersTotal = customersTotal + deptClassDollar;
              
              if (keyMatch(order, theKey) && (customersTotal < doubleDollar)) {
                setupGWP(temp, order);
              }// end order $ qualifies
            }// is type24
            else if (temp.getType().trim().equals("25")) {
              theKey = temp.getCode().trim().toUpperCase();
              theDept = temp.getDeptCodes().trim().toUpperCase();
              theClass = temp.getClassCodes().trim().toUpperCase();
              theDesigner = temp.getVendor().trim().toUpperCase();
              onlineCatTest = temp.getOnlineCatOnly().trim().toUpperCase();
              
              if (onlineCatTest.equals("TRUE")) {
                onlineCatOnly = true;
              } else {
                onlineCatOnly = false;
              }
              
              if (keyMatch(order, theKey)
                      && ((!mPromotionsHelper.deptClassMatch(order, theDept, theClass, onlineCatOnly, saleQualifier)) || (!mPromotionsHelper.vendorMatch(order, theDesigner, saleQualifier)))) {
                setupGWP(temp, order);
              }// end order $ qualifies
            }// is type25
            else if (temp.getType().trim().equals("26")) {
              theKey = temp.getCode().trim().toUpperCase();
              theCategories = temp.getQualifyingCategories().trim().toLowerCase();
              tempDollar = Double.valueOf(temp.getDollarQualifier());
              doubleDollar = tempDollar.doubleValue();
              if (keyMatch(order, theKey) && ((!mPromotionsHelper.categoriesDollarMatch(order, theCategories, doubleDollar, saleQualifier)))) {
                setupGWP(temp, order);
              }// end categories & $ qualifies
            }// is type26
            else if (temp.getType().trim().equals("27")) {
              theKey = temp.getCode().trim().toUpperCase();
              theCategories = temp.getQualifyingCategories().trim().toLowerCase();
              tempDollar = Double.valueOf("0.01");
              doubleDollar = tempDollar.doubleValue();
              if (keyMatch(order, theKey) && ((!mPromotionsHelper.categoriesDollarMatch(order, theCategories, doubleDollar, saleQualifier)))) {
                setupGWP(temp, order);
              }// end categories promo
            }// is type27
            else if (temp.getType().trim().equals("28")) {
              theKey = temp.getCode().trim().toUpperCase();
              theCategories = temp.getQualifyingCategories().trim().toLowerCase();
              tempDollar = Double.valueOf(temp.getDollarQualifier());
              doubleDollar = tempDollar.doubleValue();
              if (keyMatch(order, theKey) && ((!mPromotionsHelper.categoriesDollarMatch(order, theCategories, doubleDollar, saleQualifier)))) {
                setupGWP(temp, order);
              }// end categories promo
            }// is type28
            else if (temp.getType().trim().equals("29")) {
              theKey = temp.getCode().trim().toUpperCase();
              theDept = temp.getDeptCodes().trim().toUpperCase();
              onlineCatTest = temp.getOnlineCatOnly().trim().toUpperCase();
              
              if (onlineCatTest.equals("TRUE")) {
                onlineCatOnly = true;
              } else {
                onlineCatOnly = false;
              }
              
              if (keyMatch(order, theKey) && (!mPromotionsHelper.multiDeptClassMatch(order, theDept, onlineCatOnly, saleQualifier))) {
                setupGWP(temp, order);
              }// end order $ qualifies
            }// is type29
            else if (temp.getType().trim().equals("30")) {
              theKey = temp.getCode().trim().toUpperCase();
              theDept = temp.getDeptCodes().trim().toUpperCase();
              theClass = temp.getClassCodes().trim().toUpperCase();
              onlineCatTest = temp.getOnlineCatOnly().trim().toUpperCase();
              if (onlineCatTest.equals("TRUE")) {
                onlineCatOnly = true;
              } else {
                onlineCatOnly = false;
              }
              
              if (keyMatch(order, theKey) && (!mPromotionsHelper.multiDeptClassMatch(order, theDept, onlineCatOnly, saleQualifier))) {
                setupGWP(temp, order);
              }// end order $ qualifies
            }// is type30
            else if (temp.getType().trim().equals("31")) {
              theKey = temp.getCode().trim().toUpperCase();
              theDept = temp.getDeptCodes().trim().toUpperCase();
              tempDollar = Double.valueOf(temp.getDollarQualifier());
              doubleDollar = tempDollar.doubleValue();
              onlineCatTest = temp.getOnlineCatOnly().trim().toUpperCase();
              
              if (onlineCatTest.equals("TRUE")) {
                onlineCatOnly = true;
              } else {
                onlineCatOnly = false;
              }
              
              if (keyMatch(order, theKey) && (!mPromotionsHelper.multiDeptClassDollarMatch(order, theDept, doubleDollar, onlineCatOnly, saleQualifier))) {
                setupGWP(temp, order);
              }// end order $ qualifies
            }// is type31
            else if (temp.getType().trim().equals("32")) {
              theKey = temp.getCode().trim().toUpperCase();
              theDept = temp.getDeptCodes().trim().toUpperCase();
              tempDollar = Double.valueOf(temp.getDollarQualifier());
              doubleDollar = tempDollar.doubleValue();
              
              onlineCatTest = temp.getOnlineCatOnly().trim().toUpperCase();
              
              if (onlineCatTest.equals("TRUE")) {
                onlineCatOnly = true;
              } else {
                onlineCatOnly = false;
              }
              if (keyMatch(order, theKey) && (!mPromotionsHelper.multiDeptClassDollarMatch(order, theDept, doubleDollar, onlineCatOnly, saleQualifier))) {
                setupGWP(temp, order);
              }// end order $ qualifies
            }// is type32
            else if (temp.getType().trim().equals("33")) {
              theKey = temp.getCode().trim().toUpperCase();
              theDept = temp.getDeptCodes().trim().toUpperCase();
              theClass = temp.getClassCodes().trim().toUpperCase();
              theDesigner = temp.getVendor().trim().toUpperCase();
              
              onlineCatTest = temp.getOnlineCatOnly().trim().toUpperCase();
              
              if (onlineCatTest.equals("TRUE")) {
                onlineCatOnly = true;
              } else {
                onlineCatOnly = false;
              }
              
              if (keyMatch(order, theKey) && ((!mPromotionsHelper.multiDeptClassMatch(order, theDept, onlineCatOnly)) || (!mPromotionsHelper.vendorMatch(order, theDesigner, saleQualifier)))) {
                setupGWP(temp, order);
              }// end order $ qualifies
            }// is type33
            else if (temp.getType().trim().equals("34")) {
              theKey = temp.getCode().trim().toUpperCase();
              theDept = temp.getDeptCodes().trim().toUpperCase();
              theClass = temp.getClassCodes().trim().toUpperCase();
              theDesigner = temp.getVendor().trim().toUpperCase();
              onlineCatTest = temp.getOnlineCatOnly().trim().toUpperCase();
              
              if (onlineCatTest.equals("TRUE")) {
                onlineCatOnly = true;
              } else {
                onlineCatOnly = false;
              }
              
              if (keyMatch(order, theKey)
                      && ((!mPromotionsHelper.multiDeptClassMatch(order, theDept, onlineCatOnly, saleQualifier)) || (!mPromotionsHelper.vendorMatch(order, theDesigner, saleQualifier)))) {
                setupGWP(temp, order);
              }// end order $ qualifies
            }// is type34
            else if (temp.getType().trim().equals("35")) {
              theKey = temp.getCode().trim().toUpperCase();
              theDesigner = temp.getVendor().trim().toUpperCase();
              theDept = temp.getDeptCodes().trim().toUpperCase();
              tempDollar = Double.valueOf(temp.getDollarQualifier());
              doubleDollar = tempDollar.doubleValue();
              
              onlineCatTest = temp.getOnlineCatOnly().trim().toUpperCase();
              if (onlineCatTest.equals("TRUE")) {
                onlineCatOnly = true;
              } else {
                onlineCatOnly = false;
              }
              
              if (keyMatch(order, theKey) && (!mPromotionsHelper.multiDeptClassVendorDollarMatch(order, theDept, theDesigner, doubleDollar, onlineCatOnly, saleQualifier))) {
                setupGWP(temp, order);
              }// end order $ qualifies
            }// is type35
            else if (temp.getType().trim().equals("36")) {
              theKey = temp.getCode().trim().toUpperCase();
              theDesigner = temp.getVendor().trim().toUpperCase();
              theDept = temp.getDeptCodes().trim().toUpperCase();
              tempDollar = Double.valueOf(temp.getDollarQualifier());
              doubleDollar = tempDollar.doubleValue();
              
              onlineCatTest = temp.getOnlineCatOnly().trim().toUpperCase();
              if (onlineCatTest.equals("TRUE")) {
                onlineCatOnly = true;
              } else {
                onlineCatOnly = false;
              }
              
              if (keyMatch(order, theKey) && (!mPromotionsHelper.multiDeptClassVendorDollarMatch(order, theDept, theDesigner, doubleDollar, onlineCatOnly, saleQualifier))) {
                setupGWP(temp, order);
              }// end order $ qualifies
            }// is type36
            else if (temp.getType().trim().equals("37")) {
              theKey = temp.getCode().trim().toUpperCase();
              
              if (keyMatch(order, theKey) && (!mPromotionsHelper.orderHasSaleChkProd(order, theKey, saleQualifier))) {
                setupGWP(temp, order);
              }
            }// is type37
            else if (temp.getType().trim().equals("38")) {
              theKey = temp.getCode().trim().toUpperCase();
              
              if (keyMatch(order, theKey)
                      && (!mPromotionsHelper.orderHasProdDollar(order, theKey, temp.getDollarQualifier().trim(), saleQualifier) || ((NMOrderImpl) order).getUsedLimitPromoMap().contains(theKey))) ;
              {
                setupGWP(temp, order);
              }
            }// is type38
            else if (temp.getType().trim().equals("39")) {
              theKey = temp.getCode().trim().toUpperCase();
              
              if (keyMatch(order, theKey)
                      && (!mPromotionsHelper.orderHasProdDollar(order, theKey, temp.getDollarQualifier().trim(), saleQualifier) || ((NMOrderImpl) order).getUsedLimitPromoMap().contains(theKey))) ;
              {
                setupGWP(temp, order);
              }
            }// is type39
        }// end for loop of promos
        
        mOrderManager.updateOrder(order);
      } catch (Exception ce) {
        ce.printStackTrace();
        throw new ScenarioException(ce);
      }
    } catch (Exception e) {
      rollBack = true;
      logError("RemoveCsrGwp.executeAction(): Exception ");
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
  
  /**
   * This method will actually perform the action of removing an Sku from an order.
   * 
   * @param pSkuId
   *          the sku id of the commerce item that will be removed from the order
   * @param promoCICount
   *          the number of commerce items of the particular item to remove
   * @param pContext
   *          the context in which the action is occuring
   * @exception CommerceException
   *              if an error occurs
   * @exception RepositoryException
   *              if an error occurs
   */
  protected void removeSku(String pSkuId, int promoCICount, String pPromoType, Order order, String theKey, String theCode) throws CommerceException, RepositoryException {
	  // Figure out which commerce item to remove then remove it
	  TransactionLockService lockService = null; 
	  RepositoryItem profile = ServletUtil.getCurrentUserProfile();
      
		  TransactionLockFactory tlf = CommonComponentHelper.getTransactionLockFactory();
		  if(profile != null && tlf != null) {
			  lockService = tlf.getServiceInstance(); 
		  } else {
			  if(profile == null) {
				 if(isLoggingDebug())
				   logDebug("The user profile is not available. Locking will be skipped.");
			  } else if (isLoggingWarning()) {
				  logWarning("missingTransactionLockFactory is null"); 
			  }
		  }
		  try { 
			  if(lockService != null) 
				  lockService.acquireTransactionLock(); 
		  } 
		  catch (NoLockNameException exc) { 
			  if(isLoggingError()) {
				  logError("no Lock Name found while acquiring lock "+exc.getMessage()); 
			  }
		  } 
		  catch (DeadlockException de) { 
			  if (isLoggingError()) {
				  logError("deadlock found while acquiring lock "+de.getMessage()); 
			  }   
		  }
	  try{
		  TransactionDemarcation td = new TransactionDemarcation(); 
		  td.begin(CommonComponentHelper.getTransactionManager()); 
		  boolean shouldRollback = true; 
		  try { 
			  if (order != null) {
				  synchronized (order) {
					  // Get an iterator over each commerceItemRelationship
					  List items = mItemManager.getAllCommerceItemRelationships(order);
					  Iterator iter = items.iterator();
					  int count = 0;
					  // Examine each commerceItem relationship
					  while (iter.hasNext()) {
						  CommerceItemRelationship ciRel = (CommerceItemRelationship) iter.next();
						  // Remove all commerce items that have the correct SkuId
						  CommerceItem thisItem = ciRel.getCommerceItem();
						  if (thisItem.getCatalogRefId().equals(pSkuId)) {
							  if (thisItem instanceof NMCommerceItem) {
								  NMCommerceItem nmci = (NMCommerceItem) thisItem;
								  String transientStatus = nmci.getTransientStatus();
								  String promoType = nmci.getPromoType();
								  if (transientStatus == null) {
									  transientStatus = "";
								  }
								  if (promoType == null) {
									  promoType = "";
								  }
								  if (pPromoType == null) {
									  pPromoType = "";
								  }
								  // Add the recently added promo item into order's recentlyChangedCommerceItem bean 
                  CheckoutAPI.setRecentlyChangedItem((NMOrderImpl)order, nmci, nmci.getQuantity(), 0L, INMGenericConstants.PROMO, INMGenericConstants.REMOVE, null);
								  if (transientStatus.trim().toUpperCase().equals(PROMO_STR.trim().toUpperCase()) && promoType.trim().toUpperCase().equals(pPromoType.trim().toUpperCase())) {
									  // This is the correct CommerceItem to remove.
									  mItemManager.removeItemFromOrder(order, nmci.getId());
									  count++;

									  mOrderManager.updateOrder(order);

									  // The count variable would determine the quantity
									  // of the free items that has to be removed
									  if (count == promoCICount) {
										  // Finished removing promo commerce items - remove promo from order object
										  if (order instanceof NMOrderImpl) {
											  NMOrderImpl orderImpl = (NMOrderImpl) order;
											  orderImpl.setRemovePromoName(theKey);
											  orderImpl.setRemoveActivatedPromoCode(theCode);
											  orderImpl.setRemoveUserActivatedPromoCode(theCode);
											  logDebug("removeCSRGWP-the order version-------------------->" + orderImpl.getVersion());
										  }
										  mOrderManager.updateOrder(order);
										  // Finished removing GWPs, no need to iterate the rest of commerce items
										  break;
									  }
								  }
							  }
						  }
					  } // while (iter.hasNext())
				  }
			  }// if (order != null)
			  shouldRollback = false; 
		  }finally { 
			  try { 
				  td.end( shouldRollback ); 
			  } catch(TransactionDemarcationException e) { 
				  if (isLoggingError()) logError("Unable to end transaction "+e.getMessage()); 
			  } 
		  }
	  } catch (TransactionDemarcationException e) { 
		  if (isLoggingError()) logError("Unable to acquire transaction "+e.getMessage()); 
	  } finally { 
		  try { 
			  if(lockService != null) 
				  lockService.releaseTransactionLock(); 
		  } 
		  catch (LockManagerException lme) { 
			  if (isLoggingError()) 
				  logError("LockManagerException while removing sku "+lme.getMessage()); 
		  } 
	  } 
  }
  
private boolean isLoggingWarning() {
	return logWarning;
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
  
  private void setupGWP(GiftWithPurchase thePromotion, Order order) throws Exception {
    RepositoryItem skuItem = null;
    String theKey = new String(thePromotion.getCode().trim().toUpperCase());
    String theCode = new String(thePromotion.getPromoCodes());
    String thePromo = new String(thePromotion.getGwpItem().trim().toUpperCase());
    String skuID = new String();
    
    if (theCode == null) {
      theCode = "";
    }
    RepositoryItem promoProdItem = mPromotionsHelper.getProdItem(thePromo, theKey);
    
    if (promoProdItem == null) {
      logError("Product not found for GWP: " + thePromo + " " + theKey);
    } else {
      String promoProdId = promoProdItem.getRepositoryId();
      List skuItems = (List) promoProdItem.getPropertyValue("childSKUs");
      
      // Assumption of there is only one sku item for each free gift product
      if (skuItems.size() == 1) {
        skuItem = (RepositoryItem) skuItems.get(0);
        if (skuItem == null) {
          throw new ScenarioException(PromotionConstants.getStringResource(PromotionConstants.SKU_DOES_NOT_EXIST));
        } else {
          skuID = skuItem.getRepositoryId();
        }
      }// end (skuItems.size() == 1)
      
      if (skuItem != null) {
        skuID = skuItem.getRepositoryId();
        int promoCICount = mPromotionsHelper.countPromoProductCommerceItems(order, promoProdId);
        
        try {
          removeSku(skuID, promoCICount, "Promotion", order, theKey, theCode);
          logDebug("GWP HAS BEEN REMOVED" + skuID);
        } catch (CommerceException ce) {
          throw new ScenarioException(ce);
        } catch (RepositoryException re) {
          throw new ScenarioException(re);
        }
      } // if RI not null
    }
  }// endsetupGWP
  
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
      Nucleus.getGlobalNucleus().logError("removeCsrGWP: " + sin);
    }
  }
  
  private void logDebug(String sin) {
    if (isLoggingDebug()) {
      Nucleus.getGlobalNucleus().logDebug("removeCsrGWP: " + sin);
    }
  }
  
  private void logWarning(String sin) {
	    if (isLoggingWarning()) {
	      Nucleus.getGlobalNucleus().logDebug("removeCsrGWP: " + sin);
	    }
	  }
  
} // end of class
