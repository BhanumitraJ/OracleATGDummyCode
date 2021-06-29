package com.nm.commerce.promotion;

import java.util.HashSet;
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

import com.nm.collections.NMPromotion;
import com.nm.collections.PurchaseWithPurchase;
import com.nm.collections.PurchaseWithPurchaseArray;
import com.nm.commerce.NMCommerceItem;
import com.nm.commerce.NMProfile;
import com.nm.commerce.catalog.NMProduct;
import com.nm.commerce.checkout.CheckoutAPI;
import com.nm.commerce.checkout.CheckoutComponents;
import com.nm.common.INMGenericConstants;
import com.nm.components.CommonComponentHelper;
import com.nm.formhandler.ShoppingCartHandler;
import com.nm.repository.ProfileRepository;
import com.nm.utils.ExceptionUtil;
import com.nm.utils.PromotionsHelper;
import com.nm.utils.SystemSpecs;

/**
 * RemoveCsrPwp
 * 
 * This action was developed for the CSR PWP tool. It allows the user to create a PWP repository item with the CSR tool and generic scenario will call this action on a item removed from cart event.
 * This action will get a refernce to the PurchaseWithPurchaseArray component which will return an ArrayList of all the Promotions in the repository. This action will iterate thru every Promotion and
 * determine is the date current. If the date is valid then it will look at the characteristics of that type and determine if the current order needs to have the PWP removed.
 * 
 * @author Todd Schultz
 * @since 08/20/2004
 */

public class RemoveCsrPwp extends ActionImpl implements IPromoAction {
  // -------------------------------------
  /** Class version string */
  public static final String CLASS_VERSION = "$Id: RemoveCsrPwp.java 1.3.4.3 2013/11/14 09:05:40CST nmavj1 Exp  $";
  
  /** Parameter for the location of the ordermanager */
  public String ORDERMANAGER_PATH = "/atg/commerce/order/OrderManager";
  public static final String PROMOHELPER_PATH = "/nm/utils/PromotionsHelper";
  public static final String SHOPPING_CART_PATH = "/atg/commerce/ShoppingCart";
  public static final String PRODUCT_CATALOG_PATH = "/atg/commerce/catalog/ProductCatalog";
  public static final String PWP_ARRAY_PATH = "/nm/collections/PurchaseWithPurchaseArray";
  
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
  public PurchaseWithPurchaseArray thePromos = null;
  public Repository repository = null;
  
  private boolean logDebug;
  private boolean logError = true;
  private boolean logWarning;
  
  public void initialize(Map pParameters) throws ScenarioException {
    /** Resolve OrderManager and Promotion components. */
    logDebug = getSystemSpecs().getCsrScenarioDebug();
    logDebug("RemoveCsrPwp is Initializing");
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
    thePromos = (PurchaseWithPurchaseArray) Nucleus.getGlobalNucleus().resolveName(PWP_ARRAY_PATH);
    repository = (Repository) Nucleus.getGlobalNucleus().resolveName(PRODUCT_CATALOG_PATH);
    mPromotionsHelper = (PromotionsHelper) Nucleus.getGlobalNucleus().resolveName(PROMOHELPER_PATH);
    // System.out.println("#######################################RemoveCsrPwp
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
    logDebug("just hit removeCSRPWP");
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
    String lvl = null;
    
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
        
        
        String theKey = null;
        String saleQualifier = null;
        HashSet ignoredProducts = new HashSet();
        Map<String, NMPromotion> promoArray = thePromos.getAllActivePromotions();
        Iterator<NMPromotion> promoIter = promoArray.values().iterator();
        while (promoIter.hasNext()) {
          PurchaseWithPurchase temp = (PurchaseWithPurchase) promoIter.next();
          StringTokenizer products = new StringTokenizer(temp.getPwpProduct(), ",");
          while (products.hasMoreTokens()) {
            String prodId = products.nextToken();
            ignoredProducts.add(prodId);
          }
          saleQualifier = temp.getSaleQualificationName();
          NMOrderImpl orderImpl = (NMOrderImpl) order;
            if (temp.getType().trim().equals("1")) {
              theKey = temp.getCode().trim().toUpperCase();
              
              if (keyMatch(order, theKey)
                      && (!mPromotionsHelper.orderHasProdDollarWithoutQualifiers(order, theKey, temp.getDollarQualifier().trim(), saleQualifier, false, ignoredProducts)
                              || orderImpl.hasPwpRejectPromoProductId(theKey) || !validateEligibleCountry(temp, order))) {
                setupPWP(temp, order);
                
              }// end order $ qualifies
            }// is type1
            else if (temp.getType().trim().equals("2")) {
              theKey = temp.getCode().trim().toUpperCase();
              
              if (keyMatch(order, theKey)
                      && (!mPromotionsHelper.orderHasProdDollarWithoutQualifiers(order, theKey, temp.getDollarQualifier().trim(), saleQualifier, false, ignoredProducts)
                              || orderImpl.hasPwpRejectPromoProductId(theKey) || !validateEligibleCountry(temp, order))) {
                setupPWP(temp, order);
              }// end order $ qualifies
            }// is type2
            else if (temp.getType().trim().equals("3")) {
              theKey = temp.getCode().trim().toUpperCase();
              
              RepositoryItem profile = CheckoutComponents.getProfile(ServletUtil.getCurrentRequest());
              
              lvl = ShoppingCartHandler.findInCircleBenefitLvl(profile, order);
              
              if (keyMatch(order, theKey)
                      && ((!mPromotionsHelper.orderHasProdDollarWithoutQualifiers(order, theKey, temp.getDollarQualifier().trim(), saleQualifier, false, ignoredProducts)
                              || !mPromotionsHelper.isValidIncircleMbr(lvl) || !mPromotionsHelper.isQualifiedIncircleLvl(lvl, theKey, "pwp"))
                              || orderImpl.hasPwpRejectPromoProductId(theKey) || !validateEligibleCountry(temp, order))) {
                setupPWP(temp, order);
                
              }// end order $ qualifies
            }// is type3
            
            else if (temp.getType().trim().equals("4")) {
              theKey = temp.getCode().trim().toUpperCase();
              
              RepositoryItem profile = CheckoutComponents.getProfile(ServletUtil.getCurrentRequest());
              lvl = ShoppingCartHandler.findInCircleBenefitLvl(profile, order);
              
              if (keyMatch(order, theKey)
                      && ((!mPromotionsHelper.orderHasProdDollarWithoutQualifiers(order, theKey, temp.getDollarQualifier().trim(), saleQualifier, false, ignoredProducts)
                              || !mPromotionsHelper.isValidIncircleMbr(lvl) || !mPromotionsHelper.isQualifiedIncircleLvl(lvl, theKey, "pwp"))
                              || orderImpl.hasPwpRejectPromoProductId(theKey) || !validateEligibleCountry(temp, order))) {
                setupPWP(temp, order);
                
              }// end order $ qualifies
            }// is type4
        }// end for loop of promos
        
        mOrderManager.updateOrder(order);
      } catch (Exception ce) {
        ce.printStackTrace();
        throw new ScenarioException(ce);
      }
    } catch (Exception e) {
      rollBack = true;
      logError("RemoveCsrPwp.executeAction(): Exception ");
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
  
  /**
   * Checks to see if the COUNTRY_PREFERENCE on the order qualifies for the user. Supports both Neiman Marcus(US and Cannada) and FiftyOne countries Basically to check the country eligibility for the
   * CSR promotions to Apply
   * 
   * @param promo
   * @param order
   * @return
   * @throws PromoException
   */
  public boolean validateEligibleCountry(PurchaseWithPurchase promo, Order order) throws PromoException {
    // get country preference from the profile
    final NMProfile profile = (NMProfile) ServletUtil.getCurrentRequest().resolveName("/atg/userprofiling/Profile");
    String country = (String) profile.getPropertyValue(ProfileRepository.COUNTRY_PREFERENCE);
    
    // map of countries, key is country code, value is boolean
    Map<String, Boolean> countryEligibilityMap = promo.getFlgEligible();
    
    // if countryEligibilityMap is not null AND countryEligibilityMap contains key country AND key value is true, then grant promo
    if (countryEligibilityMap != null && countryEligibilityMap.containsKey(country)) {
      Boolean eligibility = (Boolean) countryEligibilityMap.get(country);
      // Contains key country AND key value is false, then do not grant promo
      if (!eligibility) {
        return false;
      }
    }
    
    try {
      // if the pwp item is not eligible to ship to the customer's country, then do not grant promo
      String promotionKey = promo.getCode();
      String cmosItem = promo.getPwpItem();
      RepositoryItem promotionalItemRI = mPromotionsHelper.getProdItem(cmosItem, promotionKey);
      
      if (isLoggingDebug()) {
        logDebug("country: " + country);
        logDebug("promotionKey: " + promotionKey);
        logDebug("cmosItem: " + cmosItem);
      }
      
      if (promotionalItemRI != null) {
        NMProduct nmProduct = new NMProduct(promotionalItemRI);
        
        if (nmProduct.isRestrictedToCountry(country)) {
          if (isLoggingDebug()) {
            logDebug("PWP is restricted to country.");
          }
          
          return false;
        }
      }
    } catch (Exception e) {
      logError("Exception thrown while checking pwp country restrictions: " + e);
    }
    
    return true;
  }
  
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
	  TransactionLockFactory tlf = CommonComponentHelper.getTransactionLockFactory();
	  if(tlf != null) {
		  lockService = tlf.getServiceInstance(); 
	  } else {
		  if (isLoggingWarning()) {
			  logWarning("missingTransactionLockFactory is null "); 
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
      // Get an iterator over each commerceItemRelationship
      List items = mItemManager.getAllCommerceItemRelationships(order);
      Iterator iter = items.iterator();
      int count = 0;
      // Examine each commerceItem relationship
      while (iter.hasNext()) {
        CommerceItemRelationship ciRel = (CommerceItemRelationship) iter.next();
        // Remove all commerce items that have the correct SkuId
        CommerceItem thisItem = ciRel.getCommerceItem();
        RepositoryItem prodRI = (RepositoryItem) thisItem.getAuxiliaryData().getProductRef();
        String prodId = prodRI.getRepositoryId();
        
        if (prodId.equals(pSkuId)) {
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
            
            if (nmci.isPwpItem()) {
              if (nmci.getSendCmosPromoKey() != null) {
                // Add the recently removed PWP item into order's recentlyChangedCommerceItem bean 
                CheckoutAPI.setRecentlyChangedItem((NMOrderImpl)order, nmci, nmci.getQuantity(), 0L, INMGenericConstants.PROMO, INMGenericConstants.REMOVE, null);
                StringTokenizer promoKeys = new StringTokenizer(nmci.getSendCmosPromoKey(), ",");
                while (promoKeys.hasMoreTokens()) {
                  String ciKey = promoKeys.nextToken();
                  if (ciKey.trim().equals(theKey)) {
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
                        orderImpl.removeIgnoredPromoCode(NMOrderImpl.PWP_PROMOTIONS, theCode);
                        orderImpl.setRemovePromoName(theKey);
                        orderImpl.setRemoveActivatedPromoCode(theCode);
                        orderImpl.setRemoveUserActivatedPromoCode(theCode);
                        orderImpl.removePwpMultiSkuPromoMapEntry(theKey);
                        logDebug("removeCSRPWP-the order version-------------------->" + orderImpl.getVersion());
                      }
                      mOrderManager.updateOrder(order);
                      
                      // Finished removing PWPs, no need to iterate the rest of commerce items
                      break;
                    }
                  }
                }
              }
            }
          }
        }
      } // while (iter.hasNext())
    } // if (order != null)
    shouldRollback = false; 
	  } finally { 
		  try { 
			  td.end( shouldRollback ); 
		  } catch(TransactionDemarcationException e) { 
			  if (isLoggingError()) logError("Unable to end transaction "+e.getMessage()); 
		  } 
	  }
  } catch (TransactionDemarcationException e) { 
	  if (isLoggingError()) logError("Unable to aquire transaction"+e.getMessage()); 
  } finally { 
	  try { 
		  if(lockService != null) 
			  lockService.releaseTransactionLock(); 
	  } 
	  catch (LockManagerException lme) { 
		  if (isLoggingError()) 
			  logError("LockManagerException while removing sku"+lme); 
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
  
  private void setupPWP(PurchaseWithPurchase thePromotion, Order order) throws Exception {
    RepositoryItem skuItem = null;
    String theKey = new String(thePromotion.getCode().trim().toUpperCase());
    String theCode = new String(thePromotion.getPromoCodes());
    String thePromo = new String(thePromotion.getPwpItem().trim().toUpperCase());
    String skuID = new String();
    
    if (theCode == null) {
      theCode = "";
    }
    RepositoryItem promoProdItem = mPromotionsHelper.getProdItem(thePromo, theKey);
    String promoProdId = promoProdItem.getRepositoryId();
    
    if (promoProdItem != null) {
      int promoCICount = mPromotionsHelper.countPromoProductCommerceItems(order, promoProdId);
      try {
        removeSku(promoProdId, promoCICount, "Promotion", order, theKey, theCode);
        logDebug("PWP HAS BEEN REMOVED" + skuID);
      } catch (CommerceException ce) {
        throw new ScenarioException(ce);
      } catch (RepositoryException re) {
        throw new ScenarioException(re);
      }
    } // if RI not null
  }// endsetupPWP
  
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
      Nucleus.getGlobalNucleus().logError("removeCsrPWP: " + sin);
    }
  }
  
  private void logDebug(String sin) {
    if (isLoggingDebug()) {
      Nucleus.getGlobalNucleus().logDebug("removeCsrPWP: " + sin);
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
  
  private void logWarning(String sin) {
	    if (isLoggingWarning()) {
	      Nucleus.getGlobalNucleus().logDebug("removeCsrGWP: " + sin);
	    }
	  }
} // end of class
