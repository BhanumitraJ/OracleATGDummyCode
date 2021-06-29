package com.nm.commerce.promotion;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.transaction.TransactionManager;

import atg.commerce.order.CommerceItem;
import atg.commerce.order.CommerceItemImpl;
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

import com.nm.collections.NMPromotion;
import com.nm.collections.PurchaseWithPurchase;
import com.nm.collections.PurchaseWithPurchaseArray;
import com.nm.commerce.NMCommerceItem;
import com.nm.commerce.NMProfile;
import com.nm.commerce.catalog.NMProduct;
import com.nm.commerce.checkout.CheckoutComponents;
import com.nm.components.CommonComponentHelper;
import com.nm.formhandler.ShoppingCartHandler;
import com.nm.repository.ProfileRepository;
import com.nm.utils.ExceptionUtil;
import com.nm.utils.PromotionsHelper;
import com.nm.utils.SystemSpecs;

/**
 * CheckCsrPwp
 * 
 * This action was developed for the CSR PWP tool. It allows the user to create a PWP repository item with the CSR tool and generic scenario will call this action on a item added to cart event. This
 * action will get a refernce to the PurchaseWithPurchaseArray component which will return an ArrayList of all the Promotions in the repository. This action will iterate thru every Promotion and
 * determine is the date current. If the date is valid then it will look at the characteristics of that type and determine if the current order qualifes and award the PWP.
 * 
 * @author Todd Schultz
 * @since 08/20/2004
 */

public class CheckCsrPwp extends AddItemToOrder implements IPromoAction {
  public static final String PROMO_CODE = "promo_code";// The CMOS required
                                                       // 'FREEBASESHIPPING'
                                                       // code
  public static final String PROMO_STR = "Promotion";
  public static final String PROMO_TYPE = "Promotion";
  public static final String PROMO_NAME = "Promotion";
  public static final String PROMOHELPER_PATH = "/nm/utils/PromotionsHelper";
  public static final String SHOPPING_CART_PATH = "/atg/commerce/ShoppingCart";
  public static final String PRODUCT_CATALOG_PATH = "/atg/commerce/catalog/ProductCatalog";
  public static final String PWP_ARRAY_PATH = "/nm/collections/PurchaseWithPurchaseArray";
  
  public static final long REMOVE_ALL_ITEMS = Long.MAX_VALUE;
  public static final int PWP_AWARD = 2;
  
  public PurchaseWithPurchaseArray thePromos = null;
  public Repository repository = null;
  PromotionsHelper mPromotionsHelper = null;
  
  private boolean logDebug = true;
  private boolean logError = true;
  
  public static PromotionsHelper getPromotionsHelper() {
    return ((PromotionsHelper) Nucleus.getGlobalNucleus().resolveName(PROMOHELPER_PATH));
  }
  
  public static PurchaseWithPurchaseArray getPurchaseWithPurchaseArray() {
    return ((PurchaseWithPurchaseArray) Nucleus.getGlobalNucleus().resolveName(PWP_ARRAY_PATH));
  }
  
  @Override
  public void initialize(final Map pParameters) throws ProcessException {
    /** Resolve OrderManager and Promotion components. */
    mOrderManager = OrderManager.getOrderManager();
    logDebug = getSystemSpecs().getCsrScenarioDebug();
    logDebug("CheckCsrPwp is Initializing");
    if (mOrderManager == null) {
      throw new ScenarioException(PromotionConstants.getStringResource(PromotionConstants.ORDER_MANAGER_NOT_FOUND));
    }
    
    mOrderHolderComponent = ComponentName.getComponentName(SHOPPING_CART_PATH);
    // storeOptionalParameter(pParameters, PROMO_CODE, java.lang.String.class);
    // storeOptionalParameter(pParameters, PROMO_STR, java.lang.String.class);
    
    thePromos = getPurchaseWithPurchaseArray();
    repository = (Repository) Nucleus.getGlobalNucleus().resolveName(PRODUCT_CATALOG_PATH);
    mPromotionsHelper = getPromotionsHelper();
    // System.out.println("###################################CheckCsrPwp initizialized");
    
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
  protected void executeAction(final ProcessExecutionContext pContext) throws ProcessException {
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
    String lvl = null;
    
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
      
      
      if (mOrderManager == null) {
        throw new ScenarioException(PromotionConstants.getStringResource(PromotionConstants.ORDER_MANAGER_NOT_FOUND));
      }
      
      if (order instanceof NMOrderImpl) {
        try {
          final Date mystartDate = Calendar.getInstance().getTime();
          
          final NMOrderImpl nmOrderImpl = (NMOrderImpl) order;
          
          boolean onlineCatOnly = false;
          final boolean evalPromosForEmployee = nmOrderImpl.isEvaluatePromosForEmployee();
          
          // clear any Pwp's that have multiple SKUs that have not
          // been processed by the order yet. We're going to
          // re-qualify the promos now.
          // nmOrderImpl.clearPwpMultiSkuPromoMap();
          final List activatedPromotions = nmOrderImpl.getPromoNameList();
          
          HashSet restrictedFreePwpItems = null;
          final DynamoHttpServletRequest req = ServletUtil.getCurrentRequest();
          if (null != req) {
            restrictedFreePwpItems = (HashSet) req.getSession().getAttribute("restrictedFreePWPItems");
          }
          final Map<String, NMPromotion> promoArray = thePromos.getAllPromotions();
          Iterator<NMPromotion> promoIter = promoArray.values().iterator();
          while (promoIter.hasNext()) {
            final PurchaseWithPurchase temp = (PurchaseWithPurchase) promoIter.next();
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
            final HashSet ignoredProducts = new HashSet();
            final StringTokenizer products = new StringTokenizer(temp.getPwpProduct(), ",");
            while (products.hasMoreTokens()) {
              final String prodId = products.nextToken();
              ignoredProducts.add(prodId);
            }
            if (mystartDate.after(temp.getStartDate()) && mystartDate.before(temp.getEndDate())) {
              final String promoCode = temp.getPromoCodes().trim().toUpperCase();
              final String department = temp.getDeptCodes().trim().toUpperCase();
              final String classCode = temp.getClassCodes().trim().toUpperCase();
              final String depiction = temp.getDepiction().trim().toUpperCase();
              final String designer = temp.getVendor().trim().toUpperCase();
              final String items = temp.getQualifyItems().trim().toUpperCase();
              final String promoKey = temp.getCode().trim().toUpperCase();
              final String saleQualifier = temp.getSaleQualificationName();
              double doubleDollar = 0.0;
              
              final String dollarQualifier = temp.getDollarQualifier();
              
              if ((dollarQualifier != null) && (dollarQualifier.length() > 0)) {
                final Double tempDollar = Double.valueOf(temp.getDollarQualifier());
                doubleDollar = tempDollar.doubleValue();
              }
              
              onlineCatOnly = false;
              int type = 0;
              try {
                type = Integer.parseInt(temp.getType().trim());
              } catch (final NumberFormatException e) {}
              
              switch (type) {
                case 1: {
                  if (mPromotionsHelper.orderHasProdDollarWithoutQualifiers(order, promoKey, temp.getDollarQualifier().trim(), saleQualifier, false, ignoredProducts)
                          && validateEligibleCountry(temp, order)) {
                    if (!nmOrderImpl.hasPwpRejectPromoProductId(promoKey)) {
                      setupPWP(temp, nmOrderImpl, restrictedFreePwpItems, 1);
                    }
                  }
                  
                  break;
                }
                
                case 2: {
                  if (mPromotionsHelper.promoCodeMatch(order, promoCode, saleQualifier)
                          && mPromotionsHelper.orderHasProdDollarWithoutQualifiers(order, promoKey, temp.getDollarQualifier().trim(), saleQualifier, false, ignoredProducts)
                          && validateEligibleCountry(temp, order)) {
                    if (!(nmOrderImpl.hasPwpRejectPromoProductId(promoKey))) {
                      setupPWP(temp, nmOrderImpl, restrictedFreePwpItems, 1);
                    }
                  }
                  break;
                }
                
                case 3: {
                  if (mPromotionsHelper.orderHasProdDollarWithoutQualifiers(order, promoKey, temp.getDollarQualifier().trim(), saleQualifier, false, ignoredProducts)
                          && validateEligibleCountry(temp, order)) {
                    
                    RepositoryItem profile = CheckoutComponents.getProfile(ServletUtil.getCurrentRequest());
                    lvl = ShoppingCartHandler.findInCircleBenefitLvl(profile, order);
                    
                    if (mPromotionsHelper.isValidIncircleMbr(lvl)) {
                      if (mPromotionsHelper.isQualifiedIncircleLvl(lvl, promoKey, "pwp")) {
                        if (!nmOrderImpl.hasPwpRejectPromoProductId(promoKey)) {
                          setupPWP(temp, nmOrderImpl, restrictedFreePwpItems, 1);
                        }
                      }
                    }
                  }
                  break;
                }
                
                case 4: {
                  if (mPromotionsHelper.promoCodeMatch(order, promoCode, saleQualifier)
                          && mPromotionsHelper.orderHasProdDollarWithoutQualifiers(order, promoKey, temp.getDollarQualifier().trim(), saleQualifier, false, ignoredProducts)
                          && validateEligibleCountry(temp, order)) {
                    RepositoryItem profile = CheckoutComponents.getProfile(ServletUtil.getCurrentRequest());
                    lvl = ShoppingCartHandler.findInCircleBenefitLvl(profile, order);
                    
                    if (mPromotionsHelper.isValidIncircleMbr(lvl)) {
                      if (mPromotionsHelper.isQualifiedIncircleLvl(lvl, promoKey, "pwp")) {
                        if (!nmOrderImpl.hasPwpRejectPromoProductId(promoKey)) {
                          setupPWP(temp, nmOrderImpl, restrictedFreePwpItems, 1);
                        }
                      }
                    }
                  }
                  
                  break;
                }
                
              }
            }
          }
          
          mOrderManager.updateOrder(order);
        } catch (final Exception ce) {
          ce.printStackTrace();
          throw new ScenarioException(ce);
        }
      }
    } catch (final Exception e) {
      rollBack = true;
      logError("CheckCSRPWP.executeAction(): Exception ");
      throw new PromoException(e);
    } finally {
      try {
        td.end(rollBack); // commit work
      } catch (final TransactionDemarcationException tde) {
        tde.printStackTrace();
        throw new PromoException(tde);
      }
    }
    
  } // end of executeAction method
  
  /**
   * Checks to see if the COUNTRY_PREFERENCE on the order qualifies for the user. Supports both Neiman Marcus(US and Cannada) and FiftyOne countries Basically to check the country eligibility for the
   * CSR promotions to Apply
   * 
   * @param temp
   * @param order
   * @return
   * @throws PromoException
   */
  public boolean validateEligibleCountry(final PurchaseWithPurchase temp, final Order order) throws PromoException {
    // get country preference from the profile
    final NMProfile profile = (NMProfile) ServletUtil.getCurrentRequest().resolveName("/atg/userprofiling/Profile");
    final String country = (String) profile.getPropertyValue(ProfileRepository.COUNTRY_PREFERENCE);
    
    // map of countries, key is country code, value is boolean
    final Map<String, Boolean> countryEligibilityMap = temp.getFlgEligible();
    
    // if countryEligibilityMap is not null AND countryEligibilityMap contains key country AND key value is true, then grant promo
    if ((countryEligibilityMap != null) && countryEligibilityMap.containsKey(country)) {
      final Boolean eligibility = countryEligibilityMap.get(country);
      // Contains key country AND key value is false, then do not grant promo
      if (!eligibility) {
        return false;
      }
    }
    
    try {
      // if the pwp item is not eligible to ship to the customer's country, then do not grant promo
      final String promotionKey = temp.getCode();
      final String cmosItem = temp.getPwpItem();
      final RepositoryItem promotionalItemRI = mPromotionsHelper.getProdItem(cmosItem, promotionKey);
      
      if (isLoggingDebug()) {
        logDebug("country: " + country);
        logDebug("promotionKey: " + promotionKey);
        logDebug("cmosItem: " + cmosItem);
      }
      
      if (promotionalItemRI != null) {
        final NMProduct nmProduct = new NMProduct(promotionalItemRI);
        
        if (nmProduct.isRestrictedToCountry(country)) {
          if (isLoggingDebug()) {
            logDebug("PWP is restricted to country.");
          }
          
          return false;
        }
      }
    } catch (final Exception e) {
      logError("Exception thrown while checking pwp country restrictions: " + e);
    }
    
    return true;
  }
  
  public String getCurrentCatalogCode() {
    final String catcode = getSystemSpecs().getCatalogCode();
    
    return catcode;
  }
  
  private SystemSpecs getSystemSpecs() {
    return (SystemSpecs) Nucleus.getGlobalNucleus().resolveName("/nm/utils/SystemSpecs");
  }
  
  /**
     * 
     */
  private void setupPWP(final PurchaseWithPurchase promotion, final NMOrderImpl order, final HashSet restrictedFreePwpItems, final long quantity) throws Exception {
    final String promoCode = new String(promotion.getPromoCodes().trim().toUpperCase());
    final String promoKey = new String(promotion.getCode().trim().toUpperCase());
    final String cmosItem = new String(promotion.getPwpItem().trim().toUpperCase());
    
    final RepositoryItem promotionalItemRI = mPromotionsHelper.getProdItem(cmosItem, promoKey);
    // it's possible that a promotion may exist for items that are no longer in the
    // product repository so the RI could be null.
    if (promotionalItemRI != null) {
      final String promotionalProductId = promotionalItemRI.getRepositoryId();
      boolean hasValidShipToLocations = true;
      
      if (order.shouldValidateCountry()) {
        hasValidShipToLocations = mPromotionsHelper.getFirstValidShippingGroupForPwp(promotionalItemRI, order) != null;
      }
      
      if (hasValidShipToLocations) {
        final String productOnPromoMap = order.getPwpMultiSkuProductId(promoKey);
        final boolean promoAwarded = mPromotionsHelper.promoPwpAlreadyAwarded(order, promotionalProductId, promoKey);
        
        // if the pwp is already in the cart make sure the promo name is set on the order
        if (promoAwarded) {
          
          if (promoCode.trim().length() > 1) {
            order.setActivatedPromoCode(promoCode);
            order.setUserActivatedPromoCode(promoCode);
          } else {
            order.setActivatedPromoCode(promoKey);
            order.setUserActivatedPromoCode(promoKey);
          }
          order.setPromoName(promoKey);
          order.addIgnoredPromoCode(NMOrderImpl.PWP_PROMOTIONS, promoCode);
          order.addAwardedPromotion(promotion);
          // Get an iterator over each commerceItemRelationship
          final List items = order.getCommerceItems();
          final Iterator iter = items.iterator();
          
          // Examine each commerceItem relationship
          while (iter.hasNext()) {
            // Examine all commerce items
            final CommerceItem thisItem = (CommerceItem) iter.next();
            final CommerceItemImpl thisItemImpl = (CommerceItemImpl) thisItem;
            
            final RepositoryItem productItem = (RepositoryItem) thisItemImpl.getAuxiliaryData().getProductRef();
            
            final String tempKey = productItem.getRepositoryId();
            final NMCommerceItem nmCi = (NMCommerceItem) thisItem;
            
            if (tempKey.trim().toUpperCase().equals(promotionalProductId.trim().toUpperCase())) {
              if (nmCi.getSendCmosPromoKey() != null) {
                final StringTokenizer promoKeys = new StringTokenizer(nmCi.getSendCmosPromoKey(), ",");
                while (promoKeys.hasMoreTokens()) {
                  final String ciKey = promoKeys.nextToken();
                  if (ciKey.trim().equals(promoKey)) {
                    final NMCommerceItem pwpCI = (NMCommerceItem) thisItem;
                    final NMProduct nmProduct = new NMProduct(promotionalItemRI);
                    mPromotionsHelper.addPwpMarkdown(pwpCI, nmProduct, promoKey, promoCode);
                    break;
                  }
                }
              }
              
            }
          }// end while
          mOrderManager.updateOrder(order);
          
        }
        
        if (!promoAwarded) {
          synchronized (order) {
            final List skuItems = (List) promotionalItemRI.getPropertyValue("childSKUs");
            final Double thePrice = (Double) promotionalItemRI.getPropertyValue("retailPrice");
            final Boolean flgDisplay = (Boolean) promotionalItemRI.getPropertyValue("flgDisplay");
            
            final boolean flagDisplay = flgDisplay.booleanValue();
            
            if (flagDisplay) {
              final long quantityEntitledTo = 1;
              final long quantityInCart = mPromotionsHelper.countPromoProductQty(order, promotionalProductId);
              
              if ((quantityEntitledTo < 1) && (promoAwarded)) {
                if (removePwpItem(order, cmosItem, promoKey, REMOVE_ALL_ITEMS)) {
                  // order.setRemoveActivatedPromoCode(promoCode);
                  // order.setRemoveUserActivatedPromoCode(promoCode);
                  // order.removeIgnoredPromoCode(NMOrderImpl.PWP_PROMOTIONS, promoCode);
                  // order.setRemovePromoName(promoKey);
                  // order.removeAwardedPromotionByKey(promoKey);
                  order.removePwpMultiSkuPromoMapEntry(promoKey);
                  mOrderManager.updateOrder(order);
                }
                return;
              }
              
              if (!promoAwarded) {
                if (!order.hasPwpRejectPromoProductId(promoKey)) {
                  order.addPwpMultiSkuPromoToMap(promoKey, promoCode, promotionalProductId, quantityEntitledTo);
                  order.addIgnoredPromoCode(NMOrderImpl.PWP_PROMOTIONS, promoCode);
                  order.addAwardedPromotion(promotion);
                  mOrderManager.updateOrder(order);
                }
              } else {
                
                if (promotion.isGWPShipwithItemActive()) {
                  final DynamoHttpServletRequest req = ServletUtil.getCurrentRequest();
                  final ShoppingCartHandler sc = (ShoppingCartHandler) req.resolveName("/atg/commerce/order/ShoppingCartModifier");
                  sc.evaluateGwpMustShipWith();
                }
                if (quantityEntitledTo == quantityInCart) {
                  // remove this entry from the promo map so that user is not prompted to add more pwps
                  order.removePwpMultiSkuPromoMapEntry(promoKey);
                  mOrderManager.updateOrder(order);
                } else if (quantityEntitledTo > quantityInCart) {
                  // set the quantity to the amount that still needs to added to cart
                  order.addAwardedPromotion(promotion);
                  order.addPwpMultiSkuPromoToMap(promoKey, promoCode, promotionalProductId, (quantityEntitledTo - quantityInCart));
                  mOrderManager.updateOrder(order);
                } else if (quantityEntitledTo < quantityInCart) {
                  // need to remove the pwps that the user no longer qualifies for
                  // removePwpItem(order, cmosItem, promoKey, (quantityInCart - quantityEntitledTo));
                  // remove this entry from the promo map so that user is not prompted to add more pwps
                  order.removePwpMultiSkuPromoMapEntry(promoKey);
                  mOrderManager.updateOrder(order);
                }
                
              }
            }
          }
        }
      }
    }
  }// endsetupPWP
  
  /**
   * 
   * @param order
   * @param promotion
   */
  private void removePwpPromotion(final NMOrderImpl order, final PurchaseWithPurchase promotion, final List activatedPromotions, final long quantityToRemove) throws ScenarioException {
    if ((activatedPromotions != null) && activatedPromotions.contains(promotion.getCode())) {
      synchronized (order) {
        try {
          final String promoKey = promotion.getCode();
          final String promoCode = promotion.getPromoCodes();
          final String cmosItem = promotion.getPwpItem().trim().toUpperCase();
          
          if (removePwpItem(order, cmosItem, promoKey, quantityToRemove)) {
            
            if (quantityToRemove == REMOVE_ALL_ITEMS) {
              order.setRemoveActivatedPromoCode(promoCode);
              order.setRemoveUserActivatedPromoCode(promoCode);
              order.removeIgnoredPromoCode(NMOrderImpl.PWP_PROMOTIONS, promoCode);
              order.setRemovePromoName(promoKey);
            }
            
            mOrderManager.updateOrder(order);
          }
          
        } catch (final Exception exception) {
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
  private boolean removePwpItem(final NMOrderImpl order, final String cmosItem, final String promoKey, long quantityToRemove) throws Exception {
    boolean returnValue = false;
    
    synchronized (order) {
      final List commerceItemsList = order.getCommerceItems();
      
      // copy the commerceItems to another list so that they can be
      // removed from the order without generating a concurrent
      // modification
      // exception on the order's commerce item collection.
      final ArrayList commerceItems = new ArrayList(commerceItemsList);
      
      final Iterator iterator = commerceItems.iterator();
      
      final RepositoryItem promoProdItem = mPromotionsHelper.getProdItem(cmosItem, promoKey);
      final String promotionProductId = promoProdItem.getRepositoryId();
      
      while (iterator.hasNext() && (quantityToRemove > 0)) {
        final CommerceItem commerceItem = (CommerceItem) iterator.next();
        
        if (commerceItem instanceof NMCommerceItem) {
          final NMCommerceItem nmCommerceItem = (NMCommerceItem) commerceItem;
          final RepositoryItem productItem = (RepositoryItem) nmCommerceItem.getAuxiliaryData().getProductRef();
          
          if (productItem != null) {
            final List activatedPromotions = nmCommerceItem.getSendCmosPromoKeyList();
            
            final String commerceItemProductId = (String) productItem.getPropertyValue("id");
            
            // if the product id of the commerce item matches the
            // product id of the
            // the promotional item and the commerce item has the
            // promo key
            // then remove the item from the order.
            if ((promotionProductId != null) && (commerceItemProductId != null) && (promotionProductId.equals(commerceItemProductId) && activatedPromotions.contains(promoKey))) {
              
              final long quantity = commerceItem.getQuantity();
              
              if (quantity > quantityToRemove) {
                mPromotionsHelper.updatePromoProductQty(order, productItem.getRepositoryId(), (quantity - quantityToRemove));
                logDebug("PWP ci=" + commerceItem.getId() + " qty adjusted to " + (quantity - quantityToRemove));
                quantityToRemove = 0;
              } else {
                quantityToRemove -= quantity;
                mOrderManager.getCommerceItemManager().removeItemFromOrder(order, commerceItem.getId());
                logDebug("PWP ci=" + commerceItem.getId() + " qty=" + commerceItem.getQuantity() + " removed from order");
              }
              
              returnValue = true;
              logDebug("removeCSRPWP-the order version-------------------->" + order.getVersion());
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
   * This is the start of the common logic for the checkCsrPWP & the removeCsrPWP. If you make a change to one of the utility methods you need to make it in the other too.
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
      Nucleus.getGlobalNucleus().logError("checkCsrPWP: " + sin);
    }
  }
  
  private void logDebug(final String sin) {
    if (isLoggingDebug()) {
      Nucleus.getGlobalNucleus().logDebug("checkCsrPWP: " + sin);
    }
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
  
}
