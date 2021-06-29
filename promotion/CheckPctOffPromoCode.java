package com.nm.commerce.promotion;

import static com.nm.common.INMGenericConstants.CHECKOUT;
import static com.nm.common.INMGenericConstants.COLON_STRING;
import static com.nm.integration.MasterPassConstants.DYNAMIC_PERCENT_OFF;
import static com.nm.integration.VmeConstants.VME;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.Cookie;
import javax.transaction.TransactionManager;

import atg.commerce.CommerceException;
import atg.commerce.order.CommerceItem;
import atg.commerce.order.CommerceItemManager;
import atg.commerce.order.InvalidVersionException;
import atg.commerce.order.Order;
import atg.commerce.order.OrderHolder;
import atg.commerce.order.OrderManager;
import atg.commerce.order.OrderQueries;
import atg.commerce.pricing.ItemPriceInfo;
import atg.commerce.pricing.PricingTools;
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

import com.nm.ajax.checkout.order.OrderUtils;
import com.nm.collections.NMPromotion;
import com.nm.collections.PercentOffPromotion;
import com.nm.collections.PercentOffPromotionsArray;
import com.nm.collections.Promotion;
import com.nm.commerce.NMCommerceItem;
import com.nm.commerce.NMProfile;
import com.nm.commerce.pricing.Markdown;
import com.nm.commerce.pricing.NMItemPriceInfo;
import com.nm.components.CommonComponentHelper;
import com.nm.droplet.EnablePromotionsPreview;
import com.nm.integration.util.NMCheckoutTypeUtil;
import com.nm.utils.BrandSpecs;
import com.nm.utils.DynamicComparator;
import com.nm.utils.ExceptionUtil;
import com.nm.utils.NmoUtils;
import com.nm.utils.PersonalizedCustData;
import com.nm.utils.PromotionsHelper;
import com.nm.utils.StringUtilities;
import com.nm.utils.SystemSpecs;

/**
 * This action is called when a promo code is entered. It will iterate thru the promotion array maintained by the global component PercentOffPromotionsArray and look for an effective promotion. If it
 * finds one that is effective then is will see if it is a promo code driven promotion and determine if a percent off can be applied
 */

public class CheckPctOffPromoCode extends ActionImpl implements IPromoAction {
  // -------------------------------------
  /** Class version string */
  public static final String CLASS_VERSION = "$Id: CheckPctOffPromoCode.java 1.44 2014/06/29 17:34:14CDT nmavj1 Exp  $";
  
  /** Parameter for the location of the ordermanager */
  public String ORDERMANAGER_PATH = "/atg/commerce/order/OrderManager";
  
  public String PRICINGTOOLS_PATH = "/atg/commerce/pricing/PricingTools";
  
  /** Parameter for which value to update */
  public static final String PROMO_CODE = "promo_code";
  
  public static String PROMOHELPER_PATH = "/nm/utils/PromotionsHelper";
  
  public static String COOKIE_NAME = "OTPOP"; // One Time Percent Off Promo
  public static String COOKIE_DELIMITER = "~";
  
  private boolean logDebug;
  private boolean logError = true;
  
  private String theCurrentCatCode;
  private int defaultMaxPercentOff;
  private boolean allowStackablePercentOffPromos = false;
  
  /** The shopping cart component */
  public static final String SHOPPING_CART_PATH = "/atg/commerce/ShoppingCart";
  
  /** reference to the order manager object and order holder object */
  protected OrderManager mOrderManager = null;
  protected CommerceItemManager mItemManager = null;
  protected OrderQueries mOrderQueries = null;
  
  protected ComponentName mOrderHolderComponent = null;
  protected Boolean mAddPromoFlag = null;// true/false flag in scenario
  protected String mPromoCode = null;
  public static final String PERCENTOFF_ARRAY_PATH = "/nm/collections/PercentOffPromotionsArray";
  protected PricingTools mPricingTools;
  private PromotionsHelper mPromotionsHelper = null;
  private PercentOffPromotionsArray mThePromos = null;
  
  public static final String ENABLE_PROMOTIONS_PREVIEW_PATH = "/nm/droplet/EnablePromotionsPreview";
  private EnablePromotionsPreview mEnablePromotionsPreview;
  
  @Override
  public void initialize(final Map pParameters) throws ProcessException {
    logDebug = getSystemSpecs().getCsrScenarioDebug();
    mOrderManager = (OrderManager) Nucleus.getGlobalNucleus().resolveName(ORDERMANAGER_PATH);
    mPricingTools = (PricingTools) Nucleus.getGlobalNucleus().resolveName(PRICINGTOOLS_PATH);
    mPromotionsHelper = (PromotionsHelper) Nucleus.getGlobalNucleus().resolveName(PROMOHELPER_PATH);
    
    mThePromos = (PercentOffPromotionsArray) Nucleus.getGlobalNucleus().resolveName(PERCENTOFF_ARRAY_PATH);
    
    theCurrentCatCode = getSystemSpecs().getCatalogCode();
    defaultMaxPercentOff = getSystemSpecs().getDefaultMaxPercentoff();
    allowStackablePercentOffPromos = getSystemSpecs().getAllowStackablePercentOffPromos();
    
    logDebug("the default catalog that will be used is " + theCurrentCatCode);
    
    if (mOrderManager == null) {
      throw new ProcessException(PromotionConstants.getStringResource(PromotionConstants.ORDER_MANAGER_NOT_FOUND));
    }
    
    mItemManager = mOrderManager.getCommerceItemManager();
    mOrderQueries = mOrderManager.getOrderQueries();
    mOrderHolderComponent = ComponentName.getComponentName(SHOPPING_CART_PATH);
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
      Nucleus.getGlobalNucleus().logError("checkPctOffPromoCode: " + sin);
    }
  }
  
  private void logDebug(final String sin) {
    if (isLoggingDebug()) {
      Nucleus.getGlobalNucleus().logDebug("checkPctOffPromoCode: " + sin);
    }
  }
  
  /**
     * 
     */
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
    }
    
    try {
      if (mOrderManager == null) {
        throw new PromoException(PromotionConstants.getStringResource(PromotionConstants.ORDER_MANAGER_NOT_FOUND));
      }
      
      try {
        // dump("updating promos due to message being thrown:
        // "+message.getClass().getName());
        final NMOrderImpl orderImpl = (NMOrderImpl) order;
        
        clearOrderDiscounts(orderImpl);
        mOrderManager.updateOrder(order);
        
        handleValidatePromoCode(order);
        mOrderManager.updateOrder(order);
        
        handleStorePromoCodes(order);
        
        logDebug("CSR%off-the order version---------before updateorder----------->" + orderImpl.getVersion());
        mOrderManager.updateOrder(order);
        logDebug("CSR%off-the order version---------after updateorder----------->" + orderImpl.getVersion());
      } catch (final InvalidVersionException ive) {
        ive.printStackTrace();
      } catch (final Exception cleare) {
        cleare.printStackTrace();
        throw new PromoException(cleare);
      }
    } catch (final Exception e) {
      rollBack = true;
      logError("CheckPctOffPromoCode.executeAction(): Exception ");
      throw new PromoException(e);
    } finally {
      try {
        td.end(rollBack); // commit work
      } catch (final TransactionDemarcationException tde) {
        tde.printStackTrace();
        throw new PromoException(tde);
      }
    }
  }
  
  /**
   * 
   * @param order
   * @throws Exception
   */
  private void clearOrderDiscounts(final NMOrderImpl order) throws Exception {
    // this is for percent off and needs to happen in the action as well as
    // in cart. If any modifications
    // are made to one make sure to make it to the other.
    logDebug("clearOrderDiscounts: cleaning out all applied discounts");
    
    // now remove all activated promo codes that are related to % off
    
    // we need to look at the promos on the order and iterate thru them to
    // determine
    // if a percent off promo is on the order
    final Map<String, NMPromotion> promoArray = mThePromos.getAllPromotions();
    Iterator<NMPromotion> promoIter = promoArray.values().iterator();
    while (promoIter.hasNext()) {
      final PercentOffPromotion promo = (PercentOffPromotion) promoIter.next();
      final String theKey = promo.getCode();
      String thePromoCode = promo.getPromoCodes();
      
      if (thePromoCode == null) {
        thePromoCode = "";
      }
      
      if (mPromotionsHelper.keyMatch(order, theKey)) {
        logDebug("removing active promo key: " + theKey);
        order.setRemoveActivatedPromoCode(thePromoCode);
        order.setRemoveUserActivatedPromoCode(thePromoCode);
        order.setRemovePromoName(theKey.trim());
        order.setTotalPromotionalItemValue(0.0);
        // dump("now showing active promos to be: "+
        // order.getActivatedPromoCode() );
        // dump("showing entered promo codes to be:
        // "+order.getPromoCode() );
        final List ci = order.getCommerceItems();
        for (int j = 0; j < ci.size(); j++) {
          final CommerceItem item = (CommerceItem) ci.get(j);
          clearDiscount(item, thePromoCode, theKey);
        }
      }
    }// end for loop of all promos in repository
    if (order.getPriceInfo() != null) {
      order.getPriceInfo().setDiscounted(false);
    }
    // ((NMOrderManager)mOrderManager).refreshOrder(order);
    logDebug(":clearOrderDiscounts-before updateorder------------------>" + order.getVersion());
    mOrderManager.updateOrder(order);
    logDebug(":clearOrderDiscounts after updateOrder------------------->" + order.getVersion());
  } // clear order discounts
  
  private void clearDiscount(final CommerceItem ci, final String theCodeToClear, final String theKEY) {
    logDebug("clearing send to cmos promocode from CI level-->" + theCodeToClear);
    final NMCommerceItem nm = (NMCommerceItem) ci;
    nm.setRemoveSendCmosPromoCode(theCodeToClear);
    nm.setRemoveSendCmosPromoKey(theKEY);
    final ItemPriceInfo ipi = ci.getPriceInfo();
    if (ipi == null) {
      // dump("the IPI on restore is null");
      return;
    }
    ipi.setAmountIsFinal(false);
    ipi.setDiscounted(false);
    ipi.setAmount(ipi.getListPrice() * ci.getQuantity());
    ipi.setOrderDiscountShare(0);
    ipi.setRawTotalPrice(ipi.getAmount());
    
    // dump("changed item price to: "+ci.getPriceInfo().getAmount() +" and
    // the raw price is: "+ci.getPriceInfo().getRawTotalPrice());
  }
  
  /**
   * 
   * @param pContext
   * @throws Exception
   */
  private void handleStorePromoCodes(final Order order) throws Exception {
    final DynamoHttpServletResponse resp = ServletUtil.getCurrentResponse();
    
    // don't bother if this isn't being run in a req/resp nature
    if (resp == null) {
      return;
    }
    
    final NMOrderImpl nmorder = (NMOrderImpl) order;
    
    // Get all the promotions associated with this order
    final Collection<PercentOffPromotion> promos = findPercentOffPromos(nmorder);
    
    // Search through the promotions to see if there are any
    // one time promotions. If there are, then we will set a
    // cookie on the client with that promo's code (key).
    if (promos.size() > 0) {
      final StringBuffer oneTimePromos = new StringBuffer("");
      
      final Iterator<PercentOffPromotion> iterator = promos.iterator();
      
      while (iterator.hasNext()) {
        final PercentOffPromotion promo = iterator.next();
        
        if (promo.getOneTime().booleanValue() == true) {
          logDebug("going to set a one time use cookie for " + promo.getCode());
          
          oneTimePromos.append(promo.getCode());
          oneTimePromos.append(COOKIE_DELIMITER);
        }
      }
      
      if (oneTimePromos.length() > 0) {
        // set the cookie on the client to store the one time promo code
        // (key)
        // set a two year expiration on the cookie
        final Cookie onetime = new Cookie(COOKIE_NAME, oneTimePromos.toString());
        onetime.setMaxAge(60 * 60 * 24 * 365 * 2);
        resp.addCookie(onetime);
      }
      
      // add the total discount to the order so it's stored
      final double currentamnt = nmorder.getTotalPromotionalItemValue();
      nmorder.setTotalPromotionalItemValue(currentamnt + nmorder.getTotalDiscountFromItems());
    }
  }
  
  /**
   * Returns collection of PercentOffPromotions associated with this order.
   * 
   * @param order
   * @return
   */
  private Collection<PercentOffPromotion> findPercentOffPromos(final NMOrderImpl order) {
    final Collection<PercentOffPromotion> returnValue = new ArrayList<PercentOffPromotion>();
    
    try {
      final NMOrderImpl orderImpl = order;
      
      final List promoNames = orderImpl.getPromoNameList();
      
      final Iterator iterator = promoNames.iterator();
      
      while (iterator.hasNext()) {
        final String testString = (String) iterator.next();
        
        logDebug("findPercentOffPromo found this promotion on the order--->" + testString);
        
        final PercentOffPromotion percentOffPromo = mThePromos.getPromotion(testString);
        
        if (percentOffPromo != null) {
          logDebug("found a promo for percent off --->" + percentOffPromo.getCode());
          returnValue.add(percentOffPromo);
        }
      } // end while
    } catch (final Exception e) {
      e.printStackTrace();
    }
    
    return returnValue;
  }
  
  /**
   * Returns true if the session variable for enabling the promotions preview is set to true
   * 
   * @param pContext
   * @return
   */
  protected boolean isPreviewSessionEnabled(final ProcessExecutionContext pContext) {
    
    boolean returnValue = false;
    if (mEnablePromotionsPreview == null) {
      mEnablePromotionsPreview = (EnablePromotionsPreview) pContext.getRequest().resolveName(ENABLE_PROMOTIONS_PREVIEW_PATH);
    }
    returnValue = mEnablePromotionsPreview.getPromoPreview();
    
    return returnValue;
  }
  
  /**
   * Search through the collection of promotions and find the ones that provide the best discount to the customer. Each line item may qualify for a different promotion.
   * 
   */
  private void handleValidatePromoCode(final Order order) throws Exception {
    final NMOrderImpl orderImpl = (NMOrderImpl) order;
    
    if (orderImpl.getPriceInfo() == null) {
      // dump("leaving validate promo call because price info objects will
      // be null");
      return;
    }
    
    @SuppressWarnings("unchecked")
    List<NMCommerceItem> commerceItems = orderImpl.getCommerceItems();
    CommerceItemPromoRelationship[] itemPromoRelationships = initializeItemPromoRelationships(orderImpl);
    
    // debugging
    logDebug(">>> commerceItems upon entering handleValidatePromoCode: <<<");
    displayCommerceItems(orderImpl);
    
    final List promoArray = new ArrayList<NMPromotion>(mThePromos.getAllPromotions().values()) ;
    // highest priced item promotions should always be processed at the END of the list in descending percent off order
    DynamicComparator.sort(promoArray, "percentOff", false);
    
    final DynamoHttpServletRequest request = ServletUtil.getCurrentRequest();
    processPromotions(orderImpl, request, promoArray, itemPromoRelationships);
    
    // check for updates. If the order has been updated (commerce items split)
    // then we will re-initialize the relationship array and reprocess promotions.
    // if commerce line items have been split, then we need to update & reprocess the promos.
    // (1) reinitialize commerce items
    // (2) reinitialize the relationship array
    // (3) reprocess promotions
    while (checkForUpdates(orderImpl, itemPromoRelationships)) {
      logDebug("-------> about to reinitialize itemPromoRelationships and re-process promotions <-------");
      commerceItems = orderImpl.getCommerceItems();
      itemPromoRelationships = initializeItemPromoRelationships(orderImpl);
      processPromotions(orderImpl, request, promoArray, itemPromoRelationships);
    }
    
    logDebug(">>> commerceItems after processPromotions & checkForUpdates: <<<");
    displayCommerceItems(orderImpl);
    
    boolean orderHasBeenDiscounted = false;
    
    final ArrayList<String> ignoredPromoCodes = new ArrayList<String>();
    
    // Now that we have examined all the promos, let's apply the promos to
    // each line item based on the information stored in the
    // itemPromoRelationships.
    for (int i = 0; i < itemPromoRelationships.length; ++i) {
      final NMCommerceItem commerceItem = itemPromoRelationships[i].getCommerceItem();
      final PercentOffPromotion bestPromotion = itemPromoRelationships[i].getBestPromotion();
      PercentOffPromotion extraPercentOffPromo = itemPromoRelationships[i].getExtraPercentOffPromotion();
      
      commerceItem.setBogoGroup(null);
      
      // if this commerce item is the qualified item of a bogo, update the order with the
      // necessary info. Note that we can't do this while processing the commerce item
      // WITH the bogo, because of repository funkiness pulling back obsolete data
      for (CommerceItemPromoRelationship itemPromoRelationship : itemPromoRelationships) {
        if (itemPromoRelationships[i].equals(itemPromoRelationship.getBogoQualifiedItem())) {
          CommerceItemPromoRelationship qualifyingItem = itemPromoRelationship;
          String qualifiedItemPromoCode = qualifyingItem.getBestPromotion().getBuyOneGetOneQualifiedPromotion();
          Promotion qualifiedItemPromo = mPromotionsHelper.getShippingPromotionViaKey(qualifiedItemPromoCode);
          commerceItem.setBogoGroup(commerceItem.getId());
          commerceItem.setSendCmosPromoCode(qualifiedItemPromo.getPromoCodes());
          commerceItem.setSendCmosPromoKey(qualifiedItemPromo.getCode());
          orderImpl.setActivatedPromoCode(qualifiedItemPromo.getPromoCodes());
          orderImpl.setUserActivatedPromoCode(qualifiedItemPromo.getPromoCodes());
          orderImpl.addAwardedPromotion(qualifiedItemPromo);
          break;
        }
      }
      
      // if we have found a promo that applies to this line item, then
      // apply it.
      if (bestPromotion != null) {
        if (bestPromotion.isBogo() && (itemPromoRelationships[i].getBogoQualifiedItem() != null)) {
          commerceItem.setBogoGroup(itemPromoRelationships[i].getBogoQualifiedItem().getCommerceItem().getId());
          extraPercentOffPromo = null;
        }
        
        applyDiscountToLineItem(bestPromotion, commerceItem, orderImpl);
        orderImpl.setActivatedPromoCode(bestPromotion.getPromoCodes());
        orderImpl.setUserActivatedPromoCode(bestPromotion.getPromoCodes());
        orderHasBeenDiscounted = true;
        
        ignoredPromoCodes.addAll(itemPromoRelationships[i].getIgnoredPromoCodes());
      }
      
      if (extraPercentOffPromo != null) {
        applyExtraDiscountToLineItem(extraPercentOffPromo, commerceItem, orderImpl);
        orderImpl.setActivatedPromoCode(extraPercentOffPromo.getPromoCodes());
        orderImpl.setUserActivatedPromoCode(extraPercentOffPromo.getPromoCodes());
        orderHasBeenDiscounted = true;
        
        if (extraPercentOffPromo.hasPromoCodes()) {
          ignoredPromoCodes.remove(extraPercentOffPromo.getPromoCodes());
        }
      }
    }
    
    for (CommerceItemPromoRelationship itemPromoRelationship : itemPromoRelationships) {
      // clean up the item promo relationship so that it does contain references to the
      // promotion or the commerce item (to assist garbage collection)
      itemPromoRelationship.cleanUp();
    }
    
    orderImpl.clearIgnoredPromoCodes(NMOrderImpl.PERCENT_OFF_PROMOTIONS);
    
    final List activePromoCodes = orderImpl.getActivePromoCodeList();
    final List enteredPromoCodes = orderImpl.getPromoCodeList();
    
    final Iterator iterator = enteredPromoCodes.iterator();
    
    // Check the promos on the order to see if they were applied or
    // ignored. If the promo did not appear in either of these lists
    // then there is an error associated with the promo and the
    // ui will display some type of warning to the user.
    while (iterator.hasNext()) {
      final String enteredCode = (String) iterator.next();
      
      if (!activePromoCodes.contains(enteredCode) && ignoredPromoCodes.contains(enteredCode)) {
        orderImpl.addIgnoredPromoCode(NMOrderImpl.PERCENT_OFF_PROMOTIONS, enteredCode);
      }
    }
    
    // clear the ignoredPromotions array to assist
    // garbage collection.
    ignoredPromoCodes.clear();
    
    if (orderHasBeenDiscounted) {
      orderImpl.getPriceInfo().setDiscounted(true);
      mOrderManager.updateOrder(orderImpl);
    }
  }
  
  private CommerceItemPromoRelationship[] initializeItemPromoRelationships(final NMOrderImpl orderImpl) {
    List commerceItems = orderImpl.getCommerceItems();
    CommerceItemPromoRelationship[] itemPromoRelationships = null;
    
    if (commerceItems != null) {
      itemPromoRelationships = new CommerceItemPromoRelationship[commerceItems.size()];
    } else {
      itemPromoRelationships = new CommerceItemPromoRelationship[0];
    }
    
    // Create an array to store the relationship between each commerce item
    // and
    // the promotions that we test against it. In the end, the relationship
    // will contain the best percent off promotion plus a list of promotions
    // that did qualify but were not selected as the best.
    for (int i = 0; i < itemPromoRelationships.length; ++i) {
      itemPromoRelationships[i] = new CommerceItemPromoRelationship((NMCommerceItem) commerceItems.get(i), allowStackablePercentOffPromos);
    }
    
    return itemPromoRelationships;
    
  }
  
  /**
   * Looping all the promos in the nm_prodocut_promomotions table to determine if the promo is valid on each item in the order.
   * <ul>
   * <li>
   * This method checks the dates of the promo then calls the validatePromos subroutine.</li>
   * </ul>
   * 
   * @param order
   * @param request
   * @param promoArray
   * @param itemPromoRelationships
   * @throws Exception
   */
  private void processPromotions(final NMOrderImpl order, final DynamoHttpServletRequest request, final List promoArray, final CommerceItemPromoRelationship[] itemPromoRelationships) throws Exception {
    final boolean evalPromosForEmployee = order.isEvaluatePromosForEmployee();
    boolean isVisaCheckout = order.isVisaCheckoutPayGroupOnOrder();
    final List<PercentOffPromotion> bogoPromos = new ArrayList<PercentOffPromotion>();
    
    NMProfile nmProfile =null;
    BrandSpecs brandSpecs=null;
    Object persObj=null;
    
    if(request!=null){
      nmProfile = (NMProfile) request.resolveName("/atg/userprofiling/Profile");
      brandSpecs =  CommonComponentHelper.getBrandSpecs();
      persObj=nmProfile.getEdwCustData()!=null?nmProfile.getEdwCustData().getPersonalizedCustData():null;
    }
    
    List<String> persPromoCodes= null;
    if (persObj != null && brandSpecs.isEnableSitePersonalization()) {
    	persPromoCodes=((PersonalizedCustData)persObj).getPersonalizedPromos();
    }
    
    NMCheckoutTypeUtil nmCheckoutTypeUtil = CommonComponentHelper.getNMCheckoutTypeUtil();
    final List enteredPromoCodes = order.getPromoCodeList();
    
    for (int i = 0; i < promoArray.size(); i++) {
      final PercentOffPromotion promo = (PercentOffPromotion) promoArray.get(i);
      final String key = promo.getKey();
      String code= promo.getCode();
      String promoCode =promo.getPromoCodes();
      
      // personalized flag is true for promotion and the code does not match api response, skip it. else apply.
      if(promo.isPersonalizedFlag()){    	  
    	  if(!(code !=null && (persPromoCodes!=null && persPromoCodes.contains(code) ))){
    	    if(enteredPromoCodes.contains(promoCode)){
    	      order.setBcPromoError(true);
    	    }
    		  continue;
    	  }   		  
      }      
      
      final String keyType = promo.getKeyType();
      if (!NmoUtils.isEmptyMap(promo.getFlgValidCheckoutTypes()) && !(nmCheckoutTypeUtil.isPaymentTypeEligibleForPromotion(order, promo.getFlgValidCheckoutTypes()))) {
	      final Map<String, Boolean> checkoutTypes = promo.getFlgValidCheckoutTypes();
	      if (!NmoUtils.isEmptyMap(checkoutTypes) && !(nmCheckoutTypeUtil.isPaymentTypeEligibleForPromotion(order, checkoutTypes))) {
	        continue;
	      }
      }
      /*
       * If isExcludeEmployeesFlag is true ,don't apply PercentOffPromotion to the employee order Checks to see if the isExcludeEmployeesFlag on the order qualifies for the user. Basically to check
       * isExcludeEmployeesFlag eligibility for the CSR promotions to Apply
       */
      
      if (evalPromosForEmployee && promo.isExcludeEmployeesFlag()) {
        continue;
      }
      // If this is not a one time promo and the dates are valid, then
      // validate the promo against each line item.
      if (!isOneTimePromoInCookies(request, promo)) {
        if (mPromotionsHelper.validateDate(promo)) {
          // check to see if we should apply the promo to each of the
          // line items
          validatePromos(order, itemPromoRelationships, promo, bogoPromos);
        }
      }
   
    
    // if any bogoPromos were collected, see if any should be applied
    if (bogoPromos.size() > 0) {
      validateBogoPromos(itemPromoRelationships, bogoPromos);
    }
    
    // if any commerce item has a best promotion that is a highest price item
    // promotion, we want to exclude them from being considered as an extra
    // promotion.
    final ArrayList<PercentOffPromotion> usedHighestPriceItemPromotions = new ArrayList<PercentOffPromotion>();
    
    for (final CommerceItemPromoRelationship itemPromoRelationship : itemPromoRelationships) {
    	final PercentOffPromotion promotion = itemPromoRelationship.getBestPromotion();
    	
    	if(promotion!=null){
    		// personalized flag is true for promotion and the code does not match api response, skip it. else apply.
    		if(promotion.isPersonalizedFlag()){    	  
    			if(!(code !=null && (persPromoCodes!=null && persPromoCodes.contains(code)))){
    				continue;
    			}    		  
    		}
    	}

    	if ((promotion != null) && promotion.getHighestPricedItemFlag()) {
    		usedHighestPriceItemPromotions.add(promotion);
    	}
    }
    
    // STACKABLE PROMOTIONS
    // At this point, we've looped all the promotions and determined the
    // best percent off promo for each commerce item (item in the order).
    // Now we loop the promos again and see if a second
    // or extra percent off promo should also apply.    

      // personalized flag is true for promotion and the code does not match api response, skip it. else apply.
      if(promo.isPersonalizedFlag()){    	  
    	  if(!(code !=null && (persPromoCodes!=null && persPromoCodes.contains(code) ))){
    		  continue;
    	  }    		  
      }

      final boolean checkout = !StringUtilities.isEmpty(key) && !StringUtilities.isEmpty(keyType) && keyType.equalsIgnoreCase(CHECKOUT);
      if (checkout) {
        final boolean isVisaCheckoutPromoSkipped = key.equalsIgnoreCase(VME) && !isVisaCheckout;
        final String csrSelectedCheckoutType = key + COLON_STRING + keyType;
        final String orderCheckoutType = order.getCheckoutType();
        /* Skip VisaCheckout and MasterPass Only promotion if Order Payment Type is not VisaCheckout and MasterPass respectively */
        if (isVisaCheckoutPromoSkipped || (!isVisaCheckout && !csrSelectedCheckoutType.equalsIgnoreCase(orderCheckoutType))) {
          continue;
        }
      }
      /* skip bogo */
      if (promo.isBogo()) {
        continue;
      }
      /* If isExcludeEmployeesFlag is true ,don't apply PercentOffPromotion to the employee order */
      if (evalPromosForEmployee && promo.isExcludeEmployeesFlag()) {
        continue;
      }
      // If this is not a one time promo and the dates are valid, then
      // validate the promo against each line item.
      if (!isOneTimePromoInCookies(request, promo)) {
        if (mPromotionsHelper.validateDate(promo) && !usedHighestPriceItemPromotions.contains(promo)) {
          // check to see if we should apply the promo to each of the
          // line items
          validateExtraPercentOffPromo(order, itemPromoRelationships, promo);
        }
      }
    }
  }
  
  private boolean checkForUpdates(final NMOrderImpl order, final CommerceItemPromoRelationship[] itemPromoRelationships) throws Exception {
    
    final OrderUtils orderUtils = new OrderUtils();
    
    boolean dirtyFlag = false;
    
    for (int i = 0; i < itemPromoRelationships.length; ++i) {
      final CommerceItemPromoRelationship itemRel = itemPromoRelationships[i];
      if (itemRel.getSplitCommerceItemFlag()) {
        final CommerceItem ci = itemRel.getCommerceItem();
        orderUtils.splitUpCommerceItem(order, mOrderManager, ci.getId(), true, true);
        dirtyFlag = true;
        logDebug("dirtyFlag is true");
      }
      
    }
    
    return dirtyFlag;
    
  }
  
  /**
   * Returns true if the promotion has been stored as a cookie on the client. If it is a one time promotion then we will not accept it if it is stored as a cookie.
   * 
   * @param request
   * @param promotion
   * @return
   */
  private boolean isOneTimePromoInCookies(final DynamoHttpServletRequest request, final PercentOffPromotion promotion) {
    logDebug("validatePromoCode: going to check cookies for " + promotion.getCode());
    
    boolean returnValue = false;
    
    if ((request != null) && promotion.getOneTime().booleanValue()) {
      logDebug("validatePromoCode: the request is not null ");
      
      final String cookieValue = request.getCookieParameter(COOKIE_NAME);
      
      if (cookieValue != null) {
        final String[] cookies = cookieValue.split(COOKIE_DELIMITER);
        
        logDebug("validatePromoCode: cookies are not null and need a match on -->" + promotion.getCode());
        for (int c = 0; c < cookies.length; c++) {
          if (cookies[c].equals(promotion.getCode())) {
            logDebug("validatePromoCode: one time use on percent off and backing off for KEY-->" + promotion.getCode());
            // this promo code has already been used
            returnValue = true;
            break;
          }
        }
      }
    }
    
    return returnValue;
  }
  
  /**
   * Check to see if the promotion can be applied to each of the line items. If a promo does apply to a line item, then it is added to the item promotion relationship so that it can be applied after
   * all the promotions have been evaluated. The doesOrderQualify validates each promotion type, for example, making sure that promos which must be entered by the user have been actually entered by
   * the user.
   * 
   * @param order
   * @param req
   * @param promo
   * @return
   * @throws Exception
   */
  private void validatePromos(final NMOrderImpl order, final CommerceItemPromoRelationship[] itemPromoRelationships, final PercentOffPromotion promo, List<PercentOffPromotion> bogoPromos)
          throws Exception {
    final String type = promo.getType();
    
    // Check to see if the order qualifies for the promotion. If it does,
    // then add it to each of the line items. This does not "apply" the
    // promotion yet, it simply indicates that the line item qualifies for
    // the promotion.
    if (mPromotionsHelper.doesOrderQualifyForPercentOffPromotion(promo, order)) {
      if (type.equals("103") || type.equals("104")) {
        addDiscountToOrder(promo, itemPromoRelationships);
      } else if (type.equals("105") || type.equals("107")) {
        addDiscountToDepictions(promo, itemPromoRelationships);
      } else if (type.equals("106") || type.equals("108")) {
        addDiscountToDepartments(promo, itemPromoRelationships);
      } else if (type.equals("109")) {
        addDiscountToCatalog(promo, itemPromoRelationships);
      } else if (type.equals("110") || type.equals("111")) {
        addDiscountToVendor(promo, itemPromoRelationships);
      } else if (type.equals("112") || type.equals("113") || type.equals("114") || type.equals("115") || type.equals(DYNAMIC_PERCENT_OFF)) {
        if (promo.isBogo()) {
          bogoPromos.add(promo);
        } else {
          addDiscountToKEY(order, promo, itemPromoRelationships);
        }
      }
    }
    // spend more save more: if the type is 114 and the promo has the spend
    // more flag checked, and the order doesn't qualify for the promotion
    // based on order dollar amount, add the promo to a collection of
    // promotions to be evaluated later in a droplet for spend more
    // messaging. Don't do eligible country as SMSM is only for LastCall, 
    // which doesn't support international.
    else {
      if ("114".equals(type) && promo.getSpendMoreFlag()) {
        if (mPromotionsHelper.orderHasProdCount(order, promo.getCode(), 1)) {
          order.addEncouragePromotion(promo);
    	}
      }
    }
  }
  
  /**
   * Method determines if any of the bogoPromos, previously validated, should actually apply to any line items. Each BOGO promo is considered in sorted-descending (by percent off) order. If we find a
   * qualified item, and at least 1 (in sorted ascending by item price) qualifying item, we apply the BOGO promo. We apply the promo to up to n items. We then reconsider if the BOGO should be applied
   * a second time to the same order (but different items). Since we've previously sorted by percent off, we don't need to worry about an applied BOGO being superseded by a following BOGO. A
   * limitation of this method, however, is that in some cases the customer will not get the optimized best combination of BOGO promos, since the 100% off BOGO might be applied to a $50 item while a
   * 80% off BOGO might be applied to a $200 item if the $50 item was applicable for both but the $50 item was found first. Also: This method is aggressive about splitting line items so that a
   * qualified item may be used for different sets of qualifying items.
   * 
   * @param itemPromoRelationships
   *          the items in the cart
   * @param bogoPromos
   *          all BOGO promos that MAY apply to the cart based on prior validation
   * @throws Exception
   */
  private void validateBogoPromos(CommerceItemPromoRelationship[] itemPromoRelationships, List<PercentOffPromotion> bogoPromos) throws Exception {
    // sort items by list price ascending
    final CommerceItemPromoRelationship[] sortedItemPromoRelationships = sortItemPromoRelationships(itemPromoRelationships, true);
    itemPromoRelationships = sortedItemPromoRelationships;
    List<CommerceItemPromoRelationship> lowestPricedPromoItems = null;
    
    // sort bogo promos by percent off descending
    if (bogoPromos.size() > 1) {
      Collections.sort(bogoPromos);
      Collections.reverse(bogoPromos);
    }
    
    for (PercentOffPromotion bogoPromo : bogoPromos) {
      CommerceItemPromoRelationship qualifiedItem = mPromotionsHelper.getBestQualifiedItemForBogoPromo(itemPromoRelationships, bogoPromo, bogoPromos);
      
      while (qualifiedItem != null) {
        // BOGO acts as a lowest price item promotion for up to n items, where n is the qualifying item count
        lowestPricedPromoItems = new ArrayList<CommerceItemPromoRelationship>();
        List<CommerceItemPromoRelationship> bogoPromoConsumedItems = new ArrayList<CommerceItemPromoRelationship>();
        
        for (int i = 0; i < itemPromoRelationships.length; ++i) {
          CommerceItemPromoRelationship candidateItemPromoRelationship = itemPromoRelationships[i];
          if (candidateItemPromoRelationship.getBogoQualifiedItem() == null) {
            final NMCommerceItem item = itemPromoRelationships[i].getCommerceItem();
            if (mPromotionsHelper.validateKeyOnItem(item, bogoPromo.getCode())) {
              if (candidateItemPromoRelationship.isBetterPromotion(bogoPromo, candidateItemPromoRelationship.getBestPromotion())) {
                if (candidateItemPromoRelationship.equals(qualifiedItem)) {
                  if (qualifiedItem.getCommerceItem().getQuantity() > 1) {
                    qualifiedItem.setSplitCommerceItemFlag(true);
                    return;
                  }
                } else {
                  lowestPricedPromoItems.add(candidateItemPromoRelationship);
                }
              }
            }
          } else {
            bogoPromoConsumedItems.add(candidateItemPromoRelationship.getBogoQualifiedItem());
          }
        } // while next item
        
        lowestPricedPromoItems.removeAll(bogoPromoConsumedItems);
        
        if ((lowestPricedPromoItems.size() > 0) && (qualifiedItem.getCommerceItem().getQuantity() > 1)) {
          qualifiedItem.setSplitCommerceItemFlag(true);
          return;
        }
        
        int consumed = 0;
        int maxToConsume = new Integer(bogoPromo.getBuyOneGetOneQualifyingItemCount()).intValue();
        
        for (CommerceItemPromoRelationship commerceItemPromoRelationship : lowestPricedPromoItems) {
          consumed += commerceItemPromoRelationship.getCommerceItem().getQuantity();
          if (consumed > maxToConsume) {
            if (commerceItemPromoRelationship.getCommerceItem().getQuantity() > 1) {
              commerceItemPromoRelationship.setSplitCommerceItemFlag(true);
              return;
            }
          } else {
            commerceItemPromoRelationship.addPromotion(bogoPromo);
            commerceItemPromoRelationship.setBogoQualifiedItem(qualifiedItem);
          }
          
          if (consumed == maxToConsume) {
            break;
          }
        }
        
        if (consumed > 0) {
          qualifiedItem = mPromotionsHelper.getBestQualifiedItemForBogoPromo(itemPromoRelationships, bogoPromo, bogoPromos);
        } else {
          qualifiedItem = null; // break out
        }
      }
    }
  }
  
  /**
   * Check to see if the promotion can be applied to each of the line items. If a promo does apply to a line item, then it is added to the item promotion relationship so that it can be applied after
   * all the promotions have been evaluated. The doesOrderQualify validates each promotion type, for example, making sure that promos which must be entered by the user have been actually entered by
   * the user.
   * 
   * @param order
   * @param req
   * @param promo
   * @return
   * @throws Exception
   */
  private void validateExtraPercentOffPromo(final NMOrderImpl order, final CommerceItemPromoRelationship[] itemPromoRelationships, final PercentOffPromotion promo) throws Exception {
    final String type = promo.getType();
    
    // Check to see if the order qualifies for the promotion. If it does,
    // then add it to each of the line items. This does not "apply" the
    // promotion yet, it simply indicates that the line item qualifies for
    // the promotion.
    if (mPromotionsHelper.doesOrderQualifyForExtraPercentOffPromotion(promo, order, itemPromoRelationships)) {
      if (type.equals("112") || type.equals("113")) {
        addExtraPercentOffDiscountToKEY(order, promo, itemPromoRelationships);
      }
    }
  }
  
  /**
   * 
   * @param promotion
   * @param itemPromoRelationships
   * @throws Exception
   */
  private void addDiscountToOrder(final PercentOffPromotion promotion, final CommerceItemPromoRelationship[] itemPromoRelationships) throws Exception {
    for (int i = 0; i < itemPromoRelationships.length; i++) {
      final NMCommerceItem item = itemPromoRelationships[i].getCommerceItem();
      
      final boolean vendorfound = mPromotionsHelper.validateVendorItem(promotion, item);
      final boolean deptfound = mPromotionsHelper.validateDepartmentOnItem(promotion, item);
      final boolean itemfound = mPromotionsHelper.validateCatalogOnItem(promotion, item);
      
      if (!vendorfound && !deptfound && !itemfound) {
        itemPromoRelationships[i].addPromotion(promotion);
      }
    }
  }
  
  /**
   * 
   * @param amnt
   * @param ci
   * @param order
   * @throws Exception
   */
  private void applyDiscount(final PercentOffPromotion promo, final CommerceItem ci, final NMOrderImpl order) throws Exception {
    logDebug("**% OFF applyDiscount****");
    final double amount = ci.getPriceInfo().getRawTotalPrice();
    logDebug("CI Rawamount-->" + amount);
    
    final double roundedRawTotalPrice = mPricingTools.round(amount);
    final String promoPercentOff = promo.getPercentOff();
    final double discRate = Double.parseDouble(promoPercentOff) / 100.0;
    double discamount = discRate * roundedRawTotalPrice;
    
    double roundedDiscAmt = mPricingTools.round(discamount);
    
    double newamnt = roundedRawTotalPrice - roundedDiscAmt;
    newamnt = mPricingTools.round(newamnt);
    double roundedSum = newamnt + roundedDiscAmt;
    roundedSum = mPricingTools.round(roundedSum);
    
    logDebug("**************************");
    logDebug("amount-->" + amount);
    logDebug("roundedRawTotalPrice-->" + roundedRawTotalPrice);
    logDebug("discRate-->" + String.valueOf(discRate));
    logDebug("discamount-->" + discamount);
    logDebug("roundedDiscAmt-->" + roundedDiscAmt);
    logDebug("newamnt-->" + newamnt);
    logDebug("roundedSum-->" + roundedSum);
    
    final String promotionId = promo.getCode();
    final String promoCode = promo.getPromoCodes();
    
    if (roundedSum == roundedRawTotalPrice) {
      NMItemPriceInfo ipi = (NMItemPriceInfo) ci.getPriceInfo();
      ipi.setAmount(newamnt);
      ipi.setDiscounted(true);
      ipi.setPromoPercentOff(promoPercentOff);
      Map<String, Markdown> promotionsApplied = ipi.getPromotionsApplied();
      Markdown percentOff = new Markdown(promotionId, promoCode, 0, discamount, promoPercentOff, mPromotionsHelper.findAdvertisePromotion(promotionId));
      promotionsApplied.put(promotionId, percentOff);
      // ipi.setPromotionsApplied(promotionsApplied);
      
      ipi.setOrderDiscountShare(roundedDiscAmt);
      ipi.markAsFinal();
      // ci.setPriceInfo(ipi);
      mOrderManager.updateOrder(order);
      
      // try to find the leftovers
      // the leftovers are the remainders from rounding every unit of the
      // CommerceItem
      // we are doing this after updating the original commerce item so we
      // can determine
      // that if we need to account for any fraction that is lost with
      // averaging the price of
      // the CI and the discount applied
      final double amountAfterDiscount = ci.getPriceInfo().getAmount();
      final double averagePrice = amountAfterDiscount / ci.getQuantity();
      final double roundedAverage = mPricingTools.round(averagePrice);
      final double leftovers = ci.getPriceInfo().getRawTotalPrice() - (roundedAverage * ci.getQuantity());
      double lineItemTotal = ci.getQuantity() * roundedAverage;
      lineItemTotal = mPricingTools.round(lineItemTotal);
      
      discamount = roundedRawTotalPrice - lineItemTotal;
      roundedDiscAmt = mPricingTools.round(discamount);
      
      logDebug("************************");
      logDebug("averagePrice-->" + averagePrice);
      logDebug("roundedAverage-->" + roundedAverage);
      logDebug("leftovers-->" + leftovers);
      logDebug("lineItemTotal-->" + lineItemTotal);
      logDebug("roundedDiscAmt-->" + roundedDiscAmt);
      logDebug("************************");
      
      ipi = (NMItemPriceInfo) ci.getPriceInfo();
      ipi.setAmount(lineItemTotal);
      ipi.setDiscounted(true);
      ipi.setPromoPercentOff(promoPercentOff);
      
      promotionsApplied = ipi.getPromotionsApplied();
      percentOff = new Markdown(promotionId, promoCode, 0, discamount, promoPercentOff, mPromotionsHelper.findAdvertisePromotion(promotionId));
      promotionsApplied.put(promotionId, percentOff);
      // ipi.setPromotionsApplied(promotionsApplied);
      
      ipi.setOrderDiscountShare(roundedDiscAmt);
      ipi.markAsFinal();
      // ci.setPriceInfo(ipi);
      mOrderManager.updateOrder(order);
    } else {
      
      // the amounts did not add up so try to see if it was a rounding
      // fluke and if we add a penny to the
      // customer it works
      
      roundedDiscAmt = roundedDiscAmt + 0.01;
      roundedDiscAmt = mPricingTools.round(roundedDiscAmt);
      
      double theSum2 = newamnt + roundedDiscAmt;
      theSum2 = mPricingTools.round(theSum2);
      
      if (theSum2 == amount) {
        logDebug("HAD TO ADD a PENNY TO CUSTOMER TO FIX ROUNDING");
        final NMItemPriceInfo ipi = (NMItemPriceInfo) ci.getPriceInfo();
        ipi.setAmount(newamnt);
        ipi.setDiscounted(true);
        ipi.setPromoPercentOff(promoPercentOff);
        
        final Map<String, Markdown> promotionsApplied = ipi.getPromotionsApplied();
        final Markdown percentOff = new Markdown(promotionId, promoCode, 0, discamount, promoPercentOff, mPromotionsHelper.findAdvertisePromotion(promotionId));
        promotionsApplied.put(promotionId, percentOff);
        // ipi.setPromotionsApplied(promotionsApplied);
        
        ipi.setOrderDiscountShare(roundedDiscAmt);
        ipi.markAsFinal();
        mOrderManager.updateOrder(order);
      } else {
        logError("PERCENT OFF CALCULATION ERROR");
        logError("ROUNDING DID NOT WORK");
        logError("NOTIFY APPLICATION DEVELOPMENT");
        logError("amount-->" + amount);
        logError("theSum2-->" + theSum2);
        logError("roundedRawTotalPrice-->" + roundedRawTotalPrice);
        logError("discRate-->" + discRate);
        logError("discamount-->" + discamount);
        logError("roundedDiscAmt-->" + roundedDiscAmt);
        logError("newamnt-->" + newamnt);
        logError("roundedSum-->" + roundedSum);
        logError("**************END % ERRROR*************");
      }
    }
  }
  
  /**
   * 
   * @param amnt
   * @param ci
   * @param order
   * @throws Exception
   */
  private void applyExtraDiscount(final PercentOffPromotion promo, final CommerceItem ci, final NMOrderImpl order) throws Exception {
    logDebug("**% OFF applyExtraDiscount****");
    final double amount = ci.getPriceInfo().getAmount();
    logDebug("CI previously discounted amount-->" + amount);
    
    final double roundedAmount = mPricingTools.round(amount);
    final String promoPercentOff = promo.getPercentOff();
    final double discRate = Double.parseDouble(promoPercentOff) / 100.0;
    double discamount = discRate * roundedAmount;
    
    double roundedDiscAmt = mPricingTools.round(discamount);
    
    double newamnt = roundedAmount - roundedDiscAmt;
    newamnt = mPricingTools.round(newamnt);
    double roundedSum = newamnt + roundedDiscAmt;
    roundedSum = mPricingTools.round(roundedSum);
    
    logDebug("**************************");
    logDebug("amount-->" + amount);
    logDebug("roundedAmount-->" + roundedAmount);
    logDebug("discRate-->" + String.valueOf(discRate));
    logDebug("discamount-->" + discamount);
    logDebug("roundedDiscAmt-->" + roundedDiscAmt);
    logDebug("newamnt-->" + newamnt);
    logDebug("roundedSum-->" + roundedSum);
    
    final String promotionId = promo.getCode();
    final String promoCode = promo.getPromoCodes();
    
    if (roundedSum == roundedAmount) {
      NMItemPriceInfo ipi = (NMItemPriceInfo) ci.getPriceInfo();
      ipi.setAmount(newamnt);
      ipi.setDiscounted(true);
      ipi.setPromoExtraPercentOff(promoPercentOff);
      Map<String, Markdown> promotionsApplied = ipi.getPromotionsApplied();
      Markdown percentOff = new Markdown(promotionId, promoCode, 0, discamount, promoPercentOff, mPromotionsHelper.findAdvertisePromotion(promotionId));
      promotionsApplied.put(promotionId, percentOff);
      // ipi.setPromotionsApplied(promotionsApplied);
      
      final double previousDiscountAmount = ipi.getOrderDiscountShare();
      
      ipi.setOrderDiscountShare(previousDiscountAmount + roundedDiscAmt);
      ipi.markAsFinal();
      // ci.setPriceInfo(ipi);
      mOrderManager.updateOrder(order);
      
      // try to find the leftovers
      // the leftovers are the remainders from rounding every unit of the
      // CommerceItem
      // we are doing this after updating the original commerce item so we
      // can determine
      // that if we need to account for any fraction that is lost with
      // averaging the price of
      // the CI and the discount applied
      final double amountAfterDiscount = ci.getPriceInfo().getAmount();
      final double averagePrice = amountAfterDiscount / ci.getQuantity();
      final double roundedAverage = mPricingTools.round(averagePrice);
      final double leftovers = ci.getPriceInfo().getRawTotalPrice() - (roundedAverage * ci.getQuantity());
      double lineItemTotal = ci.getQuantity() * roundedAverage;
      lineItemTotal = mPricingTools.round(lineItemTotal);
      
      discamount = roundedAmount - lineItemTotal;
      roundedDiscAmt = mPricingTools.round(discamount);
      
      logDebug("************************");
      logDebug("averagePrice-->" + averagePrice);
      logDebug("roundedAverage-->" + roundedAverage);
      logDebug("leftovers-->" + leftovers);
      logDebug("lineItemTotal-->" + lineItemTotal);
      logDebug("roundedDiscAmt-->" + roundedDiscAmt);
      logDebug("************************");
      
      ipi = (NMItemPriceInfo) ci.getPriceInfo();
      ipi.setAmount(lineItemTotal);
      ipi.setDiscounted(true);
      ipi.setPromoExtraPercentOff(promoPercentOff);
      
      promotionsApplied = ipi.getPromotionsApplied();
      percentOff = new Markdown(promotionId, promoCode, 0, discamount, promoPercentOff, mPromotionsHelper.findAdvertisePromotion(promotionId));
      promotionsApplied.put(promotionId, percentOff);
      // ipi.setPromotionsApplied(promotionsApplied);
      
      ipi.setOrderDiscountShare(previousDiscountAmount + roundedDiscAmt);
      ipi.markAsFinal();
      // ci.setPriceInfo(ipi);
      mOrderManager.updateOrder(order);
    } else {
      
      // the amounts did not add up so try to see if it was a rounding
      // fluke and if we add a penny to the
      // customer it works
      
      roundedDiscAmt = roundedDiscAmt + 0.01;
      roundedDiscAmt = mPricingTools.round(roundedDiscAmt);
      
      double theSum2 = newamnt + roundedDiscAmt;
      theSum2 = mPricingTools.round(theSum2);
      
      if (theSum2 == amount) {
        logDebug("HAD TO ADD a PENNY TO CUSTOMER TO FIX ROUNDING");
        final NMItemPriceInfo ipi = (NMItemPriceInfo) ci.getPriceInfo();
        ipi.setAmount(newamnt);
        ipi.setDiscounted(true);
        ipi.setPromoPercentOff(promoPercentOff);
        
        final Map<String, Markdown> promotionsApplied = ipi.getPromotionsApplied();
        final Markdown percentOff = new Markdown(promotionId, promoCode, 0, discamount, promoPercentOff, mPromotionsHelper.findAdvertisePromotion(promotionId));
        promotionsApplied.put(promotionId, percentOff);
        ipi.setPromotionsApplied(promotionsApplied);
        
        ipi.setOrderDiscountShare(roundedDiscAmt);
        ipi.markAsFinal();
        mOrderManager.updateOrder(order);
      } else {
        logError("PERCENT OFF CALCULATION ERROR");
        logError("ROUNDING DID NOT WORK");
        logError("NOTIFY APPLICATION DEVELOPMENT");
        logError("amount-->" + amount);
        logError("theSum2-->" + theSum2);
        logError("roundedAmount-->" + roundedAmount);
        logError("discRate-->" + discRate);
        logError("discamount-->" + discamount);
        logError("roundedDiscAmt-->" + roundedDiscAmt);
        logError("newamnt-->" + newamnt);
        logError("roundedSum-->" + roundedSum);
        logError("**************END % ERRROR*************");
      }
    }
  }
  
  /**
   * Applies the promotion to the line item on the order.
   * 
   * @param promo
   * @param ci
   * @param order
   * @throws Exception
   */
  private void applyDiscountToLineItem(final PercentOffPromotion promo, final NMCommerceItem ci, final NMOrderImpl order) throws Exception {
    if (mPromotionsHelper.verifyNotExcludedItem(ci)) {
      applyDiscount(promo, ci, order);
      ci.setSendCmosPromoCode(promo.getPromoCodes());
      ci.setSendCmosPromoKey(promo.getCode());
      order.setPromoName(promo.getCode().trim());
      order.addAwardedPromotion(promo);
      logDebug("just gave discount to commerceItem-->" + ((RepositoryItem) ci.getAuxiliaryData().getProductRef()).getRepositoryId());
    }
  }
  
  /**
   * Applies an extra promotion to the line item on the order.
   * 
   * @param promo
   * @param ci
   * @param order
   * @throws Exception
   */
  private void applyExtraDiscountToLineItem(final PercentOffPromotion extraPromo, final NMCommerceItem ci, final NMOrderImpl order) throws Exception {
    if (mPromotionsHelper.verifyNotExcludedItem(ci)) {
      applyExtraDiscount(extraPromo, ci, order);
      ci.setSendCmosPromoCode(extraPromo.getPromoCodes());
      ci.setSendCmosPromoKey(extraPromo.getCode());
      order.setPromoName(extraPromo.getCode().trim());
      order.addAwardedPromotion(extraPromo);
      logDebug("just gave discount to commerceItem-->" + ((RepositoryItem) ci.getAuxiliaryData().getProductRef()).getRepositoryId());
    }
  }
  
  /**
   * 
   * @param promo
   * @param relationships
   * @throws Exception
   */
  private void addDiscountToDepictions(final PercentOffPromotion promo, final CommerceItemPromoRelationship[] itemPromoRelationships) {
    for (int i = 0; i < itemPromoRelationships.length; ++i) {
      final NMCommerceItem item = itemPromoRelationships[i].getCommerceItem();
      
      if (mPromotionsHelper.validateDepictionOnItem(promo, item)) {
        itemPromoRelationships[i].addPromotion(promo);
      }
    }
  }
  
  /**
   * 
   * @param promotion
   * @param itemPromoRelationships
   */
  private void addDiscountToDepartments(final PercentOffPromotion promotion, final CommerceItemPromoRelationship[] itemPromoRelationships) {
    for (int i = 0; i < itemPromoRelationships.length; ++i) {
      final NMCommerceItem item = itemPromoRelationships[i].getCommerceItem();
      
      // if item is in correct department then add the promotion
      if (mPromotionsHelper.validateDepartmentOnItem(promotion, item)) {
        itemPromoRelationships[i].addPromotion(promotion);
      }
    }
  }
  
  /**
   * 
   * @param promotion
   * @param itemPromoRelationships
   */
  private void addDiscountToVendor(final PercentOffPromotion promotion, final CommerceItemPromoRelationship[] itemPromoRelationships) {
    for (int i = 0; i < itemPromoRelationships.length; ++i) {
      final NMCommerceItem item = itemPromoRelationships[i].getCommerceItem();
      if (mPromotionsHelper.validateVendorItem(promotion, item)) {
        itemPromoRelationships[i].addPromotion(promotion);
      }
    }
  }
  
  /**
   * Method adds promotion to each commerce item the promotion is eligible for, qualified by, if the promotion has "highest priced item" flag checked, only one item will have the promotion added. In a
   * situation where item 1 is the highest priced item and has a 50% promotion already, and item 2 is a lower priced item and has no promotion, and a 40% highest price item promotion is being
   * considered, the logic will grant the 40% promotion to the lower priced item because we want the customer to get the 40% on something and it would not make sense to overwrite the 50% on the
   * highest priced item with a 40% promotion.
   * 
   * Stackable promotions: for the stackable promotion project, we do not want to grant the 40% item to more than one item, so -if- a highest price item promotion is granted to any item, we exclude it
   * from processing as an extra promotion.
   * 
   */
  private void addDiscountToKEY(final NMOrderImpl order, final PercentOffPromotion promotion, CommerceItemPromoRelationship[] itemPromoRelationships) throws Exception {
    if (promotion.highestPricedItemFlag() && (itemPromoRelationships.length > 0)) {
      // sort the itemPromoRelationship records by price per item (list price) for each CI line.
      final CommerceItemPromoRelationship[] sortedItemPromoRelationships = sortItemPromoRelationships(itemPromoRelationships, false);
      // reset the relationships array to the new sorted list.
      itemPromoRelationships = sortedItemPromoRelationships;
    }
    
    int highestPricedItemNdx = 0;
    boolean highestPricedItemQualified = false;
    
    for (int i = 0; i < itemPromoRelationships.length; ++i) {
      final NMCommerceItem item = itemPromoRelationships[i].getCommerceItem();
      if (mPromotionsHelper.validateKeyOnItem(item, promotion.getCode())) {
        // highest price pass: if there are two items that qualify for the promotion and they are the same
        // price, favor the one with the least quantity to try to avoid having to split line items
        if (promotion.highestPricedItemFlag()) {
          logDebug("highestPricedItem flag is true for promotion " + promotion.getCode());
          final double currItemPrice = itemPromoRelationships[i].getCommerceItem().getPriceInfo().getListPrice();
          final double currQty = itemPromoRelationships[i].getCommerceItem().getItemQuantity();
          
          final boolean isBetterPromotion = itemPromoRelationships[i].isBetterPromotion(promotion, itemPromoRelationships[i].getBestPromotion());
          
          if (isBetterPromotion) {
            logDebug(promotion.getCode() + " isBetterPromotion for " + itemPromoRelationships[i].getCommerceItem().getCmosItemCode());
            final double highestPrice = itemPromoRelationships[highestPricedItemNdx].getCommerceItem().getPriceInfo().getListPrice();
            final double hpQty = itemPromoRelationships[highestPricedItemNdx].getCommerceItem().getItemQuantity();
            if (((highestPrice == currItemPrice) && (currQty < hpQty)) || !highestPricedItemQualified) {
              highestPricedItemNdx = i;
            }
            highestPricedItemQualified = true;
          }
        } else {
          itemPromoRelationships[i].addPromotion(promotion);
        }
      }
      logDebug(itemPromoRelationships[i].toString());
    } // while next item
    
    if (promotion.highestPricedItemFlag() & highestPricedItemQualified) {
      if (itemPromoRelationships[highestPricedItemNdx].getCommerceItem().getQuantity() == 1) {
        itemPromoRelationships[highestPricedItemNdx].addPromotion(promotion);
      } else {
        itemPromoRelationships[highestPricedItemNdx].setSplitCommerceItemFlag(true);
      }
      logDebug(">>> This item has been set as the highest priced item to be used <<<");
      logDebug(itemPromoRelationships[highestPricedItemNdx].toString());
    }
  } // apply discount to key
  
  /**
   * 
   */
  private void addExtraPercentOffDiscountToKEY(final NMOrderImpl order, final PercentOffPromotion promotion, CommerceItemPromoRelationship[] itemPromoRelationships) throws Exception {
    if (promotion.highestPricedItemFlag() && (itemPromoRelationships.length > 0)) {
      // sort the itemPromoRelationship records by price per item (list price) for each CI line.
      final CommerceItemPromoRelationship[] sortedItemPromoRelationships = sortItemPromoRelationships(itemPromoRelationships, false);
      // reset the relationships array to the new sorted list.
      itemPromoRelationships = sortedItemPromoRelationships;
    }
    
    int highestPricedItemNdx = 0;
    boolean highestPricedItemQualified = false;
    for (int i = 0; i < itemPromoRelationships.length; ++i) {
      final NMCommerceItem item = itemPromoRelationships[i].getCommerceItem();
      if (mPromotionsHelper.validateKeyOnItem(item, promotion.getCode())) {
        if (promotion.highestPricedItemFlag()) {
          logDebug("highestPricedItem flag is true for promotion " + promotion.getCode());
          final double currItemPrice = itemPromoRelationships[i].getCommerceItem().getPriceInfo().getListPrice();
          final double currQty = itemPromoRelationships[i].getCommerceItem().getItemQuantity();
          
          final boolean isBetterPromotion = itemPromoRelationships[i].isBetterPromotion(promotion, itemPromoRelationships[i].getExtraPercentOffPromotion());
          
          if (isBetterPromotion) {
            logDebug(promotion.getCode() + " isBetterPromotion for " + itemPromoRelationships[i].getCommerceItem().getCmosItemCode());
            final double highestPrice = itemPromoRelationships[highestPricedItemNdx].getCommerceItem().getPriceInfo().getListPrice();
            final double hpQty = itemPromoRelationships[highestPricedItemNdx].getCommerceItem().getItemQuantity();
            if (((highestPrice == currItemPrice) && (currQty < hpQty)) || !highestPricedItemQualified) {
              highestPricedItemNdx = i;
            }
            highestPricedItemQualified = true;
          }
        } else {
          itemPromoRelationships[i].addExtraPromotion(promotion, defaultMaxPercentOff);
        }
      }
      logDebug(itemPromoRelationships[i].toString());
    } // while next item
    
    if (promotion.highestPricedItemFlag() & highestPricedItemQualified) {
      if (itemPromoRelationships[highestPricedItemNdx].getCommerceItem().getQuantity() == 1) {
        itemPromoRelationships[highestPricedItemNdx].addExtraPromotion(promotion, defaultMaxPercentOff);
      } else {
        itemPromoRelationships[highestPricedItemNdx].setSplitCommerceItemFlag(true);
      }
      logDebug(">>> This item has been set as the highest priced item to be used <<<");
      logDebug(itemPromoRelationships[highestPricedItemNdx].toString());
    }
  } // apply discount to vendor
  
  private CommerceItemPromoRelationship[] sortItemPromoRelationships(final CommerceItemPromoRelationship[] itemPromoRelationships, boolean sortAscending) {
    
    final List<CommerceItemPromoRelationship> sortedItemList = Arrays.asList(itemPromoRelationships);
    Collections.sort(sortedItemList);
    
    if (!sortAscending) {
      Collections.reverse(sortedItemList);
    }
    
    return (CommerceItemPromoRelationship[]) sortedItemList.toArray();
  }
  
  /**
   * 
   * @param promotion
   * @param itemPromoRelationships
   */
  private void addDiscountToCatalog(final PercentOffPromotion promotion, final CommerceItemPromoRelationship[] itemPromoRelationships) {
    for (int i = 0; i < itemPromoRelationships.length; ++i) {
      final NMCommerceItem item = itemPromoRelationships[i].getCommerceItem();
      if (mPromotionsHelper.validateCatalogOnItem(promotion, item)) {
        itemPromoRelationships[i].addPromotion(promotion);
      }
    }
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
    // logDebug("profileId from UpdateACtivatedPC is:"+profileId);
    
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
  
  private void displayCommerceItems(final NMOrderImpl order) {
    final List commerceItems = order.getCommerceItems();
    
    logDebug("********************   COMMERCE ITEMS  *********************");
    for (int i = 0; i < commerceItems.size(); i++) {
      final NMCommerceItem currCi = (NMCommerceItem) commerceItems.get(i);
      logDebug("........ commerceItems[" + i + "] " + currCi.getProductId() + "/" + currCi.getCmosItemCode() + ":  qty=" + new Long(currCi.getQuantity()).toString() + " / price per item="
              + currCi.getPriceInfo().getListPrice());
    }
    logDebug("******************  COMMERCE ITEMS END  *******************");
    
  }
  
  /**
   * 
   * @return
   */
  public String getCurrentCatalogCode() {
    // String catcode = getSystemSpecs().getCatalogCode();
    
    return theCurrentCatCode;
  }
  
  /**
   * 
   * @return
   */
  private SystemSpecs getSystemSpecs() {
    return (SystemSpecs) Nucleus.getGlobalNucleus().resolveName("/nm/utils/SystemSpecs");
  }
  
  /**
   * This class maintains the relationship between a commerce item and best percent off promotion that could be added to it and also a list of promotion that could have been added but weren't because
   * they weren't the best. The ignored list is used as feedback (via the order) to the UI so that we don't display an error when the user enters a validate promo code which is ultimately ignored
   * because it wasn't the best promo.
   * 
   * @author nmwps
   * 
   */
  public static class CommerceItemPromoRelationship implements Comparable<CommerceItemPromoRelationship> {
    private NMCommerceItem mCommerceItem;
    private PercentOffPromotion mBestPromotion;
    private ArrayList<String> mIgnoredPromoCodes = new ArrayList<String>();
    private boolean mSplitCommerceItemFlag;
    private PercentOffPromotion mExtraPercentOffPromotion;
    private final boolean mAllowStackablePercentOffPromos;
    private CommerceItemPromoRelationship mBogoQualifiedItem;
    
    /**
     * 
     * @param commerceItem
     */
    public CommerceItemPromoRelationship(final NMCommerceItem commerceItem, final boolean allowStackablePercentOffPromos) {
      mCommerceItem = commerceItem;
      mBestPromotion = null;
      mSplitCommerceItemFlag = false;
      mAllowStackablePercentOffPromos = allowStackablePercentOffPromos;
    }
    
    /**
     * 
     * @param relationships
     * @param promotion
     */
    public static void addPromotionToAllItems(final CommerceItemPromoRelationship[] relationships, final PercentOffPromotion promotion) {
      for (int i = 0; i < relationships.length; ++i) {
        relationships[i].addPromotion(promotion);
      }
    }
    
    /**
     * 
     * @return
     */
    public PercentOffPromotion getBestPromotion() {
      return mBestPromotion;
    }
    
    /**
     * 
     * @return
     */
    public PercentOffPromotion getExtraPercentOffPromotion() {
      return mExtraPercentOffPromotion;
    }
    
    public CommerceItemPromoRelationship getBogoQualifiedItem() {
      return mBogoQualifiedItem;
    }
    
    public void setBogoQualifiedItem(CommerceItemPromoRelationship item) {
      mBogoQualifiedItem = item;
    }
    
    /**
     * As each promotion is added, check to see if it's better than a previous promotion, and if so, replace the previous best promotion. Keep track of ignored codes, codes of promos that have already
     * been considered for best percent off promo. IgnoredCodes are added to the order object, and later, when we do a "isApplied" check, we consider promos applied if they are in the ignored list
     * presumably to not bombard customers with messages about percent off promos they did not get applied because there was a better promo. Because of stackable promos, if the brand allows stackable
     * percent off promos, we don't store as many ignored codes; specifically, 112s that are discarded are not ignored so that we CAN display a message to the user.
     * 
     * @param promotion
     */
    public void addPromotion(final PercentOffPromotion promotion) {
      if (mBestPromotion == null) {
        mBestPromotion = promotion;
      } else {
        if (isBetterPromotion(promotion, mBestPromotion)) {
          if (mBestPromotion.hasPromoCodes()) {
            if ("112".equals(mBestPromotion.getType())) {
              if (!mAllowStackablePercentOffPromos) {
                ignorePromoCode(mBestPromotion.getPromoCodes());
              }
            } else {
              ignorePromoCode(mBestPromotion.getPromoCodes());
            }
          }
          mBestPromotion = promotion;
        } else {
          if (promotion.hasPromoCodes()) {
            if ("112".equals(promotion.getType())) {
              if (!mAllowStackablePercentOffPromos) {
                ignorePromoCode(promotion.getPromoCodes());
              }
            } else {
              ignorePromoCode(promotion.getPromoCodes());
            }
          }
        }
      }
    }
    
    /**
     * As each candidate for extra percent off promo is added, check to see if it's better than previous candidates for extra percent off promotion.
     * 
     * @param promotion
     */
    public void addExtraPromotion(final PercentOffPromotion promotion, final int maxPercentOff) {
      if ((mBestPromotion == null) || !"113".equals(mBestPromotion.getType())) {
        return;
      }
      
      if (mBestPromotion.getStackableFlag().equals(promotion.getStackableFlag())) {
        return;
      }
      
      // over the max percent off for the two?
      final double stackedPercentOff = PromotionsHelper.getStackedPercentOff(mBestPromotion, promotion, false);
      
      if (stackedPercentOff > maxPercentOff) {
        return;
      }
      
      if (mExtraPercentOffPromotion == null) {
        mExtraPercentOffPromotion = promotion;
      } else {
        if (isBetterPromotion(promotion, mExtraPercentOffPromotion)) {
          mExtraPercentOffPromotion = promotion;
        }
      }
    }
    
    /*
     * This method simply tells you if the promotion is a better deal or not.
     */
    public boolean isBetterPromotion(final PercentOffPromotion candidatePromotion, final PercentOffPromotion existingPromotion) {
      boolean betterPercentOff = false;
      
      if (existingPromotion == null) {
        betterPercentOff = true;
      } else {
        final String bestPercentOffString = existingPromotion.getPercentOff();
        final String candidatePercentOffString = candidatePromotion.getPercentOff();
        
        double bestPercentOff = 0;
        double candidatePercentOff = 0;
        
        try {
          bestPercentOff = Double.parseDouble(bestPercentOffString);
        } catch (final Exception exception) {}
        
        try {
          candidatePercentOff = Double.parseDouble(candidatePercentOffString);
        } catch (final Exception exception) {}
        
        if (bestPercentOff < candidatePercentOff) {
          betterPercentOff = true;
        } else {
          betterPercentOff = false;
        }
      }
      return betterPercentOff;
    }
    
    /**
     * 
     * @param promotion
     */
    public void ignorePromoCode(final String promoCode) {
      if (!mIgnoredPromoCodes.contains(promoCode)) {
        mIgnoredPromoCodes.add(promoCode);
      }
    }
    
    /**
     * 
     * @return
     */
    public NMCommerceItem getCommerceItem() {
      return mCommerceItem;
    }
    
    /**
     * 
     * @return
     */
    public boolean getSplitCommerceItemFlag() {
      return mSplitCommerceItemFlag;
    }
    
    /**
     * 
     * @return
     */
    public void setSplitCommerceItemFlag(final boolean splitCommerceItemFlag) {
      mSplitCommerceItemFlag = splitCommerceItemFlag;
    }
    
    public ArrayList<String> getIgnoredPromoCodes() {
      return mIgnoredPromoCodes;
    }
    
    /**
     * 
     */
    public void cleanUp() {
      mIgnoredPromoCodes.clear();
      mIgnoredPromoCodes = null;
      mBestPromotion = null;
      mCommerceItem = null;
      mExtraPercentOffPromotion = null;
      mBogoQualifiedItem = null;
    }
    
    @Override
    public String toString() {
      final StringBuffer ciPromoRel = new StringBuffer();
      ciPromoRel.append("\n************  CommerceItemPromoRelationship  ************");
      
      ciPromoRel.append("\n\n****====----> CommerceItem:  ");
      if (this.getCommerceItem() == null) {
        ciPromoRel.append("null");
      } else {
        ciPromoRel.append("\npid=" + this.getCommerceItem().getProductId() + ", cmos id=" + this.getCommerceItem().getCmosCatalogId() + "_" + this.getCommerceItem().getCmosItemCode());
        ciPromoRel.append(", qty=" + new Long(this.getCommerceItem().getQuantity()).toString() + ", price per item=" + new Double(this.getCommerceItem().getPriceInfo().getListPrice()).toString());
      }
      
      ciPromoRel.append("\n\n****====----> BestPromotion:  ");
      if (this.getBestPromotion() == null) {
        ciPromoRel.append("null");
      } else {
        ciPromoRel.append("\ncode=" + this.getBestPromotion().getCode() + ", pctoff=" + this.getBestPromotion().getPercentOff() + ", promocode=" + this.getBestPromotion().getPromoCodes());
      }
      
      ciPromoRel.append("\n\n****====----> IgnoredPromoCodes  :");
      if ((this.getIgnoredPromoCodes() == null) || (this.getIgnoredPromoCodes().size() == 0)) {
        ciPromoRel.append("null");
      } else {
        ciPromoRel.append("\ncodes=" + this.getIgnoredPromoCodes().toString());
      }
      
      ciPromoRel.append("\n\n****====----> SplitCommerceItemFlag:  " + new Boolean(this.getSplitCommerceItemFlag()).toString());
      
      return ciPromoRel.toString();
    }
    
    @Override
    public int compareTo(final CommerceItemPromoRelationship item) {
      
      final NMCommerceItem ci = item.getCommerceItem();
      
      if (this.getCommerceItem().getPriceInfo().getListPrice() > ci.getPriceInfo().getListPrice()) {
        return 1;
      } else if (this.getCommerceItem().getPriceInfo().getListPrice() < ci.getPriceInfo().getListPrice()) {
        return -1;
      } else {
        return 0;
      }
      
    }
  }
}
