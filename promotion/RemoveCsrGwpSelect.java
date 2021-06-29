package com.nm.commerce.promotion;


import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
import atg.process.ProcessException;
import atg.process.ProcessExecutionContext;
import atg.process.action.ActionImpl;
import atg.repository.MutableRepositoryItem;
import atg.repository.Repository;
import atg.repository.RepositoryException;
import atg.scenario.ScenarioException;
import atg.service.lockmanager.DeadlockException;
import atg.service.lockmanager.LockManagerException;
import atg.servlet.DynamoHttpServletRequest;

import com.nm.collections.GiftWithPurchaseSelect;
import com.nm.collections.GiftWithPurchaseSelectArray;
import com.nm.collections.NMPromotion;
import com.nm.commerce.NMCommerceItem;
import com.nm.commerce.checkout.CheckoutAPI;
import com.nm.common.INMGenericConstants;
import com.nm.components.CommonComponentHelper;
import com.nm.utils.ExceptionUtil;
import com.nm.utils.PromotionsHelper;
import com.nm.utils.SystemSpecs;

/**
 * RemoveCsrGwpSelect
 * 
 * This action was developed for the CSR GWP tool. It allows the user to create a GWP repository item with the CSR tool and generic scenario will call this action on a item removed from cart event.
 * This action will get a refernce to the GiftWithPurchaseArray component which will return an ArrayList of all the Promotions in the repository. This action will iterate thru every Promotion and
 * determine is the date current. If the date is valid then it will look at the characteristics of that type and determine if the current order needs to have the GWP removed.
 * 
 * @author Todd Schultz
 * @since 08/20/2004
 */

public class RemoveCsrGwpSelect extends ActionImpl implements IPromoAction {
  // -------------------------------------
  /** Class version string */
  public static final String CLASS_VERSION = "$Id: RemoveCsrGwpSelect.java 1.15 2009/12/18 13:18:04CST William P Shows (NMWPS) Exp  $";
  
  /** Parameter for the location of the ordermanager */
  public String ORDERMANAGER_PATH = "/atg/commerce/order/OrderManager";
  public static final String PROMOHELPER_PATH = "/nm/utils/PromotionsHelper";
  public static final String SHOPPING_CART_PATH = "/atg/commerce/ShoppingCart";
  public static final String PRODUCT_CATALOG_PATH = "/atg/commerce/catalog/ProductCatalog";
  public static final String GWP_ARRAY_PATH = "/nm/collections/GiftWithPurchaseSelectArray";
  
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
  public GiftWithPurchaseSelectArray thePromos = null;
  public Repository repository = null;
  
  private boolean logDebug;
  private boolean logError = true;
  private boolean logWarning;
  
  public void initialize(Map pParameters) throws ProcessException {
    /** Resolve OrderManager and Promotion components. */
    logDebug = getSystemSpecs().getCsrScenarioDebug();
    logDebug("RemoveCsrGwpSelect is Initializing");
    mOrderManager = (OrderManager) Nucleus.getGlobalNucleus().resolveName(ORDERMANAGER_PATH);
    
    if (mOrderManager == null) throw new ProcessException(PromotionConstants.getStringResource(PromotionConstants.ORDER_MANAGER_NOT_FOUND));
    
    // mItemManager = mOrderManager.getCommerceItemManager();
    mItemManager = (CommerceItemManager) Nucleus.getGlobalNucleus().resolveName("/atg/commerce/order/CommerceItemManager");
    mOrderQueries = mOrderManager.getOrderQueries();
    
    // storeOptionalParameter(pParameters, CMOSSKU_PARAM,
    // java.lang.String.class);
    // storeOptionalParameter(pParameters, PROMO_TYPE_PARAM,
    // java.lang.String.class);
    mOrderHolderComponent = ComponentName.getComponentName(SHOPPING_CART_PATH);
    thePromos = (GiftWithPurchaseSelectArray) Nucleus.getGlobalNucleus().resolveName(GWP_ARRAY_PATH);
    repository = (Repository) Nucleus.getGlobalNucleus().resolveName(PRODUCT_CATALOG_PATH);
    mPromotionsHelper = (PromotionsHelper) Nucleus.getGlobalNucleus().resolveName(PROMOHELPER_PATH);
  }
  
  /***************************************************************************
   * This method is called from scenario, note no params are accepted but they are still required to be passed by scenario. Once validation is performed that all values needed are populated RQL
   * queries are perfored to create Repository Item
   * 
   * @param ProcessExecutionContext
   *          pContext
   * @return void
   * @exception ScenarioException
   **************************************************************************/
  
  protected void executeAction(ProcessExecutionContext pContext) throws ProcessException {
    try {
      Order order = getOrderToModify(pContext);
      evaluatePromo(order);
    } catch (final Exception e) {
      throw new ProcessException(ExceptionUtil.getExceptionInfo(e), e);
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
      
      if (mOrderManager == null) throw new PromoException(PromotionConstants.getStringResource(PromotionConstants.ORDER_MANAGER_NOT_FOUND));
      
      // Check if it is a message of type ItemRemovedFromOrder
      
      
      try {
        String theKey = "";
        String theDept = "";
        String theClass = "";
        String theDesigner = "";
        String theDepiction = "";
        Double tempDollar = new Double(0.0);
        double doubleDollar = .01;
        boolean onlineCatOnly = false;
        
        NMOrderImpl nmOrderImpl = (NMOrderImpl) order;
        Map<String, NMPromotion> promoArray = thePromos.getAllActivePromotions();
        Iterator<NMPromotion> promoIter = promoArray.values().iterator();
        while (promoIter.hasNext()) {
          GiftWithPurchaseSelect temp = (GiftWithPurchaseSelect) promoIter.next();
            int type = Integer.parseInt(temp.getType());
            
            switch (type) {
              case 1: {
                theKey = temp.getCode().trim().toUpperCase();
                theDept = temp.getDeptCodes().trim().toUpperCase();
                theClass = temp.getClassCodes().trim().toUpperCase();
                tempDollar = Double.valueOf(temp.getDollarQualifier());
                doubleDollar = tempDollar.doubleValue();
                onlineCatOnly = temp.getOnlineCatOnly().booleanValue();
                
                if (!mPromotionsHelper.deptClassDollarMatch(nmOrderImpl, theDept, theClass, doubleDollar, onlineCatOnly)) {
                  removeGwpSelect(nmOrderImpl, theKey);
                }
              }
              break;
              case 2: {
                theKey = temp.getCode().trim().toUpperCase();
                theDepiction = temp.getDepiction().trim().toUpperCase();
                tempDollar = Double.valueOf(temp.getDollarQualifier());
                doubleDollar = tempDollar.doubleValue();
                if (!mPromotionsHelper.depictionDollarMatch(nmOrderImpl, theDepiction, doubleDollar)) {
                  removeGwpSelect(nmOrderImpl, theKey);
                }
              }
              break;
              case 3: {
                theKey = temp.getCode().trim().toUpperCase();
                theDesigner = temp.getVendor().trim().toUpperCase();
                tempDollar = Double.valueOf(temp.getDollarQualifier());
                doubleDollar = tempDollar.doubleValue();
                if (!mPromotionsHelper.vendorDollarMatch(nmOrderImpl, theDesigner, doubleDollar)) {
                  removeGwpSelect(nmOrderImpl, theKey);
                }
              }
              break;
              case 4: {
                theKey = temp.getCode().trim().toUpperCase();
                theDesigner = temp.getVendor().trim().toUpperCase();
                theDept = temp.getDeptCodes().trim().toUpperCase();
                theClass = temp.getClassCodes().trim().toUpperCase();
                onlineCatOnly = temp.getOnlineCatOnly().booleanValue();
                tempDollar = Double.valueOf(temp.getDollarQualifier());
                doubleDollar = tempDollar.doubleValue();
                double customersTotal = 0;
                
                double deptClassDollar = mPromotionsHelper.vendorDollarDeptClassMatch(nmOrderImpl, theDesigner, theDept, theClass, onlineCatOnly, doubleDollar);
                customersTotal = customersTotal + deptClassDollar;
                
                if (customersTotal < doubleDollar) {
                  removeGwpSelect(nmOrderImpl, theKey);
                }// end order $ qualifies
              }
              break;
            }
        }// end for loop of promos
        
        mOrderManager.updateOrder(order);
      } catch (Exception ce) {
        ce.printStackTrace();
        throw new ProcessException(ce);
      }
    } catch (Exception e) {
      rollBack = true;
      logError("RemoveCsrGwpSelect.executeAction(): Exception ");
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
   * Remove the GWP select promo from the order. It will remove the promoKey from the order and remove any skus that have the key also.
   * 
   * @param order
   * @param promoKey
   * @throws Exception
   */
  private void removeGwpSelect(NMOrderImpl order, String promoKey) throws Exception {
    // Remove key from order if present
    if (mPromotionsHelper.keyMatch(order, promoKey)) {
      order.setRemovePromoName(promoKey);
      order.removeAwardedPromotionByKey(promoKey);
    }
    
    // In theory the skus should only have the promokey if the order has
    // the promokey but I can't swear by that so the order level issue is
    // addressed above and the commerceitem level is addressed below.
    
    // Remove skus if the promotions items have already been added.
    if (keyMatch(order, promoKey)) {
      removeSkus(promoKey, order);
    }
  }
  
  private boolean keyMatch(Order order, String theKey) throws Exception {
    
    if (order != null) {
      if (order instanceof NMOrderImpl) {
        List items = mItemManager.getAllCommerceItemRelationships(order);
        Iterator iter = items.iterator();
        
        // Examine each commerceItem relationship for matching promo key on the item
        // if any of the items in the order have a matching key, then pass keyMatch condition
        // so that the item can be evaluated for qualification
        CommerceItemRelationship ciRel;
        CommerceItem thisItem;
        NMCommerceItem nmci;
        while (iter.hasNext()) {
          ciRel = (CommerceItemRelationship) iter.next();
          thisItem = ciRel.getCommerceItem();
          if (thisItem instanceof NMCommerceItem) {
            nmci = (NMCommerceItem) thisItem;
            // look and see if the KEY is on the CI and if so return keyMatch = true (key found)
            String promoNameCI = (String) nmci.getPromoName();
            if (promoNameCI != null && promoNameCI.trim().toLowerCase().equals(theKey.trim().toLowerCase())) return true;
          }
        } // while (iter.hasNext())
      } // end instance of NMOrderImpl
    }// end order !=null
    return false;
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
   * @param pQuantity
   *          the quantity of the particular item to remove
   * @param pContext
   *          the context in which the action is occuring
   * @exception CommerceException
   *              if an error occurs
   * @exception RepositoryException
   *              if an error occurs
   */
  protected void removeSkus(String promoKey, Order order) throws CommerceException, RepositoryException {
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
				  // Examine each commerceItem relationship
				  while (iter.hasNext()) {
					  CommerceItemRelationship ciRel = (CommerceItemRelationship) iter.next();
					  // Remove all commerce items that have the correct SkuId
					  CommerceItem thisItem = ciRel.getCommerceItem();

					  if (thisItem instanceof NMCommerceItem) {

						  NMCommerceItem nmci = (NMCommerceItem) thisItem;
						  // look and see if the KEY is on the CI and if so remove it
						  String promoNameCI = (String) nmci.getPromoName();
						  if (promoNameCI != null && promoNameCI.trim().toLowerCase().equals(promoKey.trim().toLowerCase())) {
						    CheckoutAPI.setRecentlyChangedItem((NMOrderImpl)order, nmci, nmci.getQuantity(), 0L, INMGenericConstants.PROMO, INMGenericConstants.REMOVE, null);
						    mItemManager.removeItemFromOrder(order, nmci.getId());
						  }
					  }
				  } // while (iter.hasNext())

				  if (order instanceof NMOrderImpl) {
					  NMOrderImpl orderImpl = (NMOrderImpl) order;
					  orderImpl.setRemovePromoName(promoKey);
					  logDebug("RemoveCsrGwpSelect-the order version-------------------->" + orderImpl.getVersion());
				  }
				  mOrderManager.updateOrder(order);

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
		  if (isLoggingError()) logError("Unable to aquire transaction "+e.getMessage()); 
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
      Nucleus.getGlobalNucleus().logError("RemoveCsrGwpSelect: " + sin);
    }
  }
  
  private void logDebug(String sin) {
    if (isLoggingDebug()) {
      Nucleus.getGlobalNucleus().logDebug("RemoveCsrGwpSelect: " + sin);
    }
  }
  
  private void logWarning(String sin) {
	    if (isLoggingWarning()) {
	      Nucleus.getGlobalNucleus().logDebug("removeCsrGWP: " + sin);
	    }
	  }
} // end of class
