package com.nm.commerce.promotion;

import static com.nm.common.INMGenericConstants.CLOSE_PARENTHESIS;
import static com.nm.common.INMGenericConstants.EMPTY_STRING;
import static com.nm.common.INMGenericConstants.OPEN_PARENTHESIS;
import static com.nm.estimateddeliverydate.EstimatedDeliveryDateConstants.APPROXIMATE_DELIVERY;
import static com.nm.estimateddeliverydate.EstimatedDeliveryDateConstants.DELIVERY;
import static com.nm.repository.orderrepository.OrderRepositoryConstants.INCIRCLE_BONUS_POINTS;
import static com.nm.repository.orderrepository.OrderRepositoryConstants.INCIRCLE_EARNED_LEVEL;
import static com.nm.returnseligibility.ReturnsEligibilityConstants.ALIPAY;
import static com.nm.returnseligibility.ReturnsEligibilityConstants.ALIPAY_CONSTANT;
import static com.nm.returnseligibility.ReturnsEligibilityConstants.CREDIT_CARD;
import static com.nm.returnseligibility.ReturnsEligibilityConstants.SHOPRUNNER;
import static com.nm.returnseligibility.ReturnsEligibilityConstants.SHOPRUNNER_CONSTANT;
import static com.nm.returnseligibility.ReturnsEligibilityConstants.SHOPRUNNER_EXPRESS_CONSTANT;
import static com.nm.returnseligibility.ReturnsEligibilityConstants.VISA_CHECKOUT_WALLET;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.commons.lang.StringUtils;

import atg.adapter.gsa.GSAItem;
import atg.commerce.order.CommerceItem;
import atg.commerce.order.HardgoodShippingGroup;
import atg.commerce.order.OrderImpl;
import atg.commerce.order.OrderManager;
import atg.commerce.order.ShippingGroupCommerceItemRelationship;
import atg.commerce.pricing.ItemPriceInfo;
import atg.commerce.pricing.OrderPriceInfo;
import atg.core.util.Address;
import atg.nucleus.Nucleus;
import atg.nucleus.logging.ApplicationLogging;
import atg.nucleus.logging.ClassLoggingFactory;
import atg.repository.MutableRepositoryItem;
import atg.repository.RepositoryItem;
import atg.servlet.DynamoHttpServletRequest;
import atg.servlet.ServletUtil;

import com.nm.collections.NMPromotion;
import com.nm.collections.PercentOffPromotion;
import com.nm.collections.ServiceLevel;
import com.nm.commerce.NMCommerceItem;
import com.nm.commerce.NMCreditCard;
import com.nm.commerce.NMOrderHolder;
import com.nm.commerce.NMRepositoryContactInfo;
import com.nm.commerce.beans.RecentlyChangedCommerceItem;
import com.nm.commerce.catalog.NMProduct;
import com.nm.commerce.checkout.CheckoutComponents;
import com.nm.commerce.checkout.beans.CommerceItemUpdate;
import com.nm.commerce.order.IOrder;
import com.nm.commerce.order.NMOrderManager;
import com.nm.commerce.order.NMPaymentGroupManager;
import com.nm.commerce.pricing.Markdown;
import com.nm.commerce.pricing.NMItemPriceInfo;
import com.nm.commerce.pricing.NMOrderPriceInfo;
import com.nm.common.INMGenericConstants;
import com.nm.components.CommonComponentHelper;
import com.nm.edo.constants.EmployeeDiscountConstants;
import com.nm.international.fiftyone.checkoutapi.OrderRepositoryConstants;
import com.nm.repository.stores.Store;
import com.nm.tms.constants.TMSDataDictionaryConstants;
import com.nm.utils.NmoUtils;
import com.nm.utils.ProdSkuUtil;
import com.nm.utils.PromotionsHelper;
import com.nm.utils.StringUtilities;
import com.nm.utils.SystemSpecs;
import com.nm.utils.fiftyone.FiftyOneUtils;

/**
 * Title: Neiman Marcus Description: Copyright: Copyright (c) 2001 Company:
 * 
 * @author
 * @version 1.0
 */

public class NMOrderImpl extends OrderImpl implements IOrder {
  public static final long serialVersionUID = 1000L;
  public static final int SPC_STATE_NONE = 0;
  public static final int SPC_STATE_CHECKOUT = 1;
  public static final int SPC_STATE_VERIFED_PAYMENT = 2;
  public static final int SPC_STATE_ORDER_REVIEW = 3;
  public static final int ALL_PROMOTION_TYPES = 0;
  public static final int PERCENT_OFF_PROMOTIONS = 1;
  public static final int GWP_PROMOTIONS = 2;
  public static final int DOLLAR_OFF_PROMOTIONS = 3;
  public static final int PWP_PROMOTIONS = 7;
  public static final int EMPLOYEE_LOGIN_STATUS_NONE = 0;
  public static final int EMPLOYEE_ALREADY_LOGGED_IN = 1;
  public static final int EMPLOYEE_PERFORMED_SILENT_AUTHENTICATION = 2;
  private static final ApplicationLogging logger  = ClassLoggingFactory.getFactory().getLoggerForClass(NMOrderImpl.class);
  
  private String promoEmailAddress = null;
  private Map<String, String> emailValidationPromos = new HashMap<String, String>();
  
  // this member is transient because it does not need to be serialized when the order
  // is serialized
  private transient Set<NMPromotion> awardedPromotions = new HashSet<NMPromotion>();
  
  // this member is transient because it does not need to be serialized when the order
  // is serialized.
  private transient Set<NMPromotion> encouragePromotions = new HashSet<NMPromotion>();
  
  // this is used by the NM713 project to avoid adding unearned promos to the order
  private transient boolean bypassPromoEvaluation = false;
  
  private volatile int outstandingEventCount = 0;
  
  private String transientPromoCode;
  private transient boolean freeBaseShippingAvailableFlag;
  private boolean checkForParentheticalCharges;
  private boolean wishListAdditionFlag;
  private boolean bcPromoError;
  private boolean changeToPickupInStore;
  
  /**
   * Stores promo codes that were provided by the customer but ignored by the promotion engine because a better percent off promotion was found or user input is required to select the GWP, etc.
   */
  private final HashMap<Integer, ArrayList<String>> mIgnoredPromoCodeMap = new HashMap<Integer, ArrayList<String>>();
  
  // -----------------------------------------------------
  // / vsrb1
  // / TestBadDate used for testing purposes only.
  // / Should be set to false, except to generate expired carts.
  // / Note that this Class is not currently a component,
  // / so get/set methods not called by Nucleus
  private boolean mTestBadDate = false;
  
  // -----------------------------------------------------
  // vsrb1 7/21/2003
  // InfoMessages is an ArrayList of informational messages
  // to be displayed to the user in the shopping cart page.
  private ArrayList<String> mInfoMessages = new ArrayList<String>();
  
  // -----------------------------------------------------
  // vsrb1
  // NeedingCartUtilityUpdate is a flag to determine if CartUtility.updateShoppingCart method
  // needs to run. It should only run once per session.
  private boolean mNeedingCartUtilityUpdate = true;
  
  private int failedCCAttemptCount = 0;
  
  private Set<String> activeDynamicCodes = new HashSet<String>();
  
  // INSV1
  HashMap<String, String> removedItemDetails = new HashMap<String, String>();
  
  // ShopRunner
  private boolean shopRunner = false;
  private boolean alipay = false;
  
  /* property to hold the recently changed commerce items */
  private List<RecentlyChangedCommerceItem> recentlyChangedCommerceItems;
  
  /** The international order duties. */
  private double internationalOrderDuties;
  
  /** The international shipping cost. */
  private double internationalShippingCost;
  
  /** The international taxes. */
  private double internationalTaxes;
  private int promoOrigin;
  private boolean plccPromoError = false;
  private boolean userClickedPlaceOrder = false;
  private String selectedPayment;
  // Empty constructor
  public NMOrderImpl() {}
  
  public boolean isShopRunner() {
    return shopRunner;
  }
  
  public void setShopRunner(final boolean shopRunner) {
    this.shopRunner = shopRunner;
  }
  
  /**
   * @return the expressPaypal
   */
  public boolean isExpressPaypal() {
    boolean returnValue = false;
    final Boolean expressPaypal = (Boolean) getPropertyValue("expressPaypal");
    if (expressPaypal != null) {
      returnValue = expressPaypal.booleanValue();
    }
    return returnValue;
  }
  
  /**
   * @param expressPaypal
   *          the expressPaypal to set
   */
  public void setExpressPaypal(final boolean expressPaypal) {
    
    setPropertyValue("expressPaypal", new Boolean(expressPaypal));
  }
  
  // End
  
  // EDO changes
  
  /**
   * This method checks the employee login status and returns true if it is greater than zero
   * 
   * @return true, if employee login status is greater than zero
   */
  public boolean isEvaluatePromosForEmployee() {
    return getEmployeeLoginStatus() > EMPLOYEE_LOGIN_STATUS_NONE;
  }
  
  /**
   * @return the chinaUnionPayPaymentType
   */
  public boolean isChinaUnionPayPaymentType() {
    boolean returnValue = false;
    final Boolean chinaUnionPayPaymentType = (Boolean) getPropertyValue("chinaUnionPayPaymentType");
    if (chinaUnionPayPaymentType != null) {
      returnValue = chinaUnionPayPaymentType.booleanValue();
    }
    return returnValue;
  }
  
  /**
   * @param chinaUnionPayPaymentType
   *          the chinaUnionPayPaymentType to set
   */
  public void setChinaUnionPayPaymentType(final boolean chinaUnionPayPaymentType) {
    // this.chinaUnionPayPaymentType = chinaUnionPayPaymentType;
    setPropertyValue("chinaUnionPayPaymentType", new Boolean(chinaUnionPayPaymentType));
  }
  
  /**
   * This method is used to check whether employee promotion is applied or not.
   * 
   * @return true if employee promotion is applied
   */
  public boolean isEmployeePromotionApplied() {
    return NmoUtils.getBooleanValue(getPropertyValue(EmployeeDiscountConstants.EMPLOYEE_PROMOTION_APPLIED));
  }

  /**
   * Sets the 'employeePromotionApplied' property
   * 
   * @param employeePromotionApplied
   *          the boolean variable set as true if employee promotion is applied
   */
  public void setEmployeePromotionApplied(final boolean employeePromotionApplied) {
    setPropertyValue(EmployeeDiscountConstants.EMPLOYEE_PROMOTION_APPLIED, new Boolean(employeePromotionApplied));
  }

  public boolean isConvertRussiaCurrencyToUSD() {
    boolean returnValue = false;
    final Boolean convertRussiaCurrencyToUSD = (Boolean) getPropertyValue("convertRussiaCurrencyToUSD");
    if (convertRussiaCurrencyToUSD != null) {
      returnValue = convertRussiaCurrencyToUSD.booleanValue();
    }
    return returnValue;
  }

  public void setRussiaCurrencyToUSD(final boolean convertRussiaCurrencyToUSD) {
    setPropertyValue("convertRussiaCurrencyToUSD", new Boolean(convertRussiaCurrencyToUSD));
  }

  public boolean isBypassPromoEvaluation() {
    return bypassPromoEvaluation;
  }

  public void setBypassPromoEvaluation(final boolean bypassPromoEvaluation) {
    this.bypassPromoEvaluation = bypassPromoEvaluation;
  }

  public String getPromoEmailAddress() {
    return promoEmailAddress;
  }

  public void setPromoEmailAddress(final String promoEmailAddress) {
    this.promoEmailAddress = promoEmailAddress;
  }

  public Map<String, String> getEmailValidationPromos() {
    return emailValidationPromos;
  }

  public void setEmailValidationPromos(final Map<String, String> emailValidationPromos) {
    this.emailValidationPromos = emailValidationPromos;
  }

  public void clearAwardedPromotions() {
    awardedPromotions.clear();
    clearItemLevelPromotions();
  }

  public Set<NMPromotion> getAwardedPromotions() {
    return awardedPromotions;
  }

  public void addAwardedPromotion(final NMPromotion promo) {
    awardedPromotions.add(promo);
  }

  public void removeAwardedPromotionByKey(final String key) {
    final Iterator<NMPromotion> iterator = awardedPromotions.iterator();
    
    while (iterator.hasNext()) {
      final NMPromotion promotion = iterator.next();
      final String code = promotion.getCode();
      
      if ((code != null) && code.equalsIgnoreCase(key)) {
        iterator.remove();
        break;
      }
    }
  }

  public NMPromotion getAwardedPromotion(final String promoCode) {
    NMPromotion returnValue = null;
    
    final Iterator<NMPromotion> iterator = awardedPromotions.iterator();
    
    while (iterator.hasNext()) {
      final NMPromotion promotion = iterator.next();
      final String code = promotion.getPromoCodes();
      
      if ((code != null) && code.equalsIgnoreCase(promoCode)) {
        returnValue = promotion;
        break;
      }
    }
    
    return returnValue;
  }

  public boolean getHasAwardedBogoPromotion() {
    for (NMPromotion promotion : awardedPromotions) {
      if (promotion instanceof PercentOffPromotion && ((PercentOffPromotion) promotion).isBogo()) {
        return true;
      }
    }

    return false;
  }

  public void clearEncouragePromotions() {
    encouragePromotions.clear();
  }

  public Set<NMPromotion> getEncouragePromotions() {
    return encouragePromotions;
  }

  public void addEncouragePromotion(final NMPromotion promo) {
    encouragePromotions.add(promo);
  }
 
  public void removeEncouragePromotionByKey(final String key) {
    final Iterator<NMPromotion> iterator = encouragePromotions.iterator();
    
    while (iterator.hasNext()) {
      final NMPromotion promotion = iterator.next();
      final String code = promotion.getCode();
      
      if ((code != null) && code.equalsIgnoreCase(key)) {
        iterator.remove();
        break;
      }
    }
  }
  
  public NMPromotion getEncouragePromotion(final String promoCode) {
    NMPromotion returnValue = null;
    
    final Iterator<NMPromotion> iterator = encouragePromotions.iterator();
    
    while (iterator.hasNext()) {
      final NMPromotion promotion = iterator.next();
      final String code = promotion.getPromoCodes();
      
      if ((code != null) && code.equalsIgnoreCase(promoCode)) {
        returnValue = promotion;
        break;
      }
    }
    
    return returnValue;
  }
  
  public boolean is_iPhoneOrder() {
    final Boolean iphone = (Boolean) getPropertyValue("iphoneOrder");
    if (null == iphone) {
      return false;
    }
    
    return iphone.booleanValue();
  }
  
  public void set_iPhoneOrder(final boolean iphone) {
    setPropertyValue("iphoneOrder", iphone ? Boolean.TRUE : Boolean.FALSE);
  }
  
  public boolean hasPromotionWithClass(final int classId) {
    boolean returnValue = false;
    
    final Iterator<NMPromotion> iterator = awardedPromotions.iterator();
    
    while (iterator.hasNext()) {
      final NMPromotion promotion = iterator.next();
      
      if (promotion.getPromotionClass() == classId) {
        returnValue = true;
        break;
      }
    }
    
    return returnValue;
  }
  
  public boolean getHasActiveShippingPromo() {
    final PromotionsHelper promotionsHelper = CommonComponentHelper.getPromotionsHelper();
    return hasShippingPromotion() && promotionsHelper.hasActiveShippingPromo(this);
  }
  
  public boolean isShippingToCountry(final String countryCode) {
    final Collection<String> shipToCountries = getShipToCountries();
    
    return shipToCountries.contains(countryCode);
  }
  
  public Collection<String> getShipToCountries() {
    final ArrayList<String> returnValue = new ArrayList<String>();
    
    @SuppressWarnings("unchecked")
    final Collection<HardgoodShippingGroup> shippingGroups = getShippingGroups();
    final Iterator<HardgoodShippingGroup> iterator = shippingGroups.iterator();
    while (iterator.hasNext()) {
      final HardgoodShippingGroup shippingGroup = iterator.next();
      final Address address = shippingGroup.getShippingAddress();
      if (address != null) {
        final String country = address.getCountry();
        returnValue.add(country);
      }
    }
    
    return returnValue;
  }
  
  /**
   * Returns a map where the key is a country code and the value is a collection of provinces/states for that country.
   * 
   * @return
   */
  public Map<String, Collection<String>> getShipToProvinces() {
    final HashMap<String, Collection<String>> returnValue = new HashMap<String, Collection<String>>();
    
    @SuppressWarnings("unchecked")
    final Collection<HardgoodShippingGroup> shippingGroups = getShippingGroups();
    final Iterator<HardgoodShippingGroup> iterator = shippingGroups.iterator();
    while (iterator.hasNext()) {
      final HardgoodShippingGroup shippingGroup = iterator.next();
      final Address address = shippingGroup.getShippingAddress();
      if (address != null) {
        final String country = address.getCountry();
        final String state = address.getState();
        
        if ((country != null) && (state != null)) {
          Collection<String> states = returnValue.get(country);
          if (states == null) {
            states = new ArrayList<String>();
            returnValue.put(country, states);
          }
          
          states.add(state);
        }
      }
    }
    
    return returnValue;
  }
  
  public boolean isCanadianOrder() {
    return isShippingToCountry("CA");
  }
  
  public boolean getHasShippingUpgradePromotion() {
    return hasShippingUpgradePromotion();
  }
  
  public boolean hasShippingUpgradePromotion() {
    boolean returnValue = false;
    
    final String aPCode = getActivatedPromoCode();
    
    if ((aPCode != null) && ((aPCode.indexOf("CMOSUPGRADE") != -1) || (aPCode.indexOf("UPGRADE2NDDAY") != -1) || (aPCode.indexOf("UPGRADEOVERNIGHT") != -1))) {
      returnValue = true;
    }
    
    return returnValue;
  }
  
  public boolean hasShippingPromotion() {
    return hasPromotionWithClass(NMPromotion.SHIPPING);
  }
  
  public boolean hasGiftwrapping() {
    boolean returnValue = false;
    
    final List<NMCommerceItem> commerceItems = getNmCommerceItems();
    
    final Iterator<NMCommerceItem> iterator = commerceItems.iterator();
    
    while (iterator.hasNext()) {
      final NMCommerceItem commerceItem = iterator.next();
      
      final RepositoryItem giftwrap = commerceItem.getGiftWrap();
      
      if (giftwrap != null) {
        returnValue = true;
        break;
      }
    }
    
    return returnValue;
  }
  
  public boolean hasPwpPromotion(final String productId) {
    boolean returnValue = false;
    
    final List<NMCommerceItem> commerceItems = getNmCommerceItems();
    
    final Iterator<NMCommerceItem> iterator = commerceItems.iterator();
    
    while (iterator.hasNext()) {
      final NMCommerceItem commerceItem = iterator.next();
      
      final RepositoryItem productItem = (RepositoryItem) commerceItem.getAuxiliaryData().getProductRef();
      final String currentProdId = productItem.getRepositoryId();
      if (currentProdId.equals(productId)) {
        if (commerceItem.isPwpItem()) {
          returnValue = true;
          break;
        }
      }
      
    }
    
    return returnValue;
  }
  
  public void refresh() {
    // int old = getVersion();
    invalidateOrder();
    // we just call this for its refresh-order side-affect
    getCommerceItemCount();
    // System.out.println("NMOrderImpl.refresh(): order = "+getId()+", old version = "+old+", new version = "+getVersion());
  }
  
  public void resetFinCenCodes() {
    
    final Boolean bool = (Boolean) getPropertyValue("validateFinCen");
    final String issueOrigin = (String) getPropertyValue("issueOrigin");
    final String issueType = (String) getPropertyValue("issueType");
    final String issueId = (String) getPropertyValue("issueId");
    final String birthDate = (String) getPropertyValue("birthDate");
    
    if (!bool.booleanValue()) {
      setPropertyValue("validateFinCen", new Boolean("true"));
    }
    
    if ((issueOrigin != null) && (issueOrigin != "")) {
      setPropertyValue("issueOrigin", "");
    }
    if ((issueType != null) && (issueType != "")) {
      setPropertyValue("issueType", "");
    }
    if ((issueId != null) && (issueId != "")) {
      setPropertyValue("issueId", "");
    }
    if ((birthDate != null) && (birthDate != "")) {
      setPropertyValue("birthDate", "");
    }
    
  }
  
  public boolean validateFinCen() {
    final Object o = getPropertyValue("validateFinCen");
    final Boolean bool = (Boolean) o;
    boolean result = true;
    
    if (!bool.booleanValue()) {
      final String issueType = (String) getPropertyValue("issueType");
      if ((issueType != null) && (issueType != "")) {
        result = false;
      }
    }
    return result;
  }
  
  public String getSystemCode() {
    final Object systemCode = getPropertyValue("systemCode");
    if (systemCode != null) {
      return (String) systemCode;
    } else {
      return "";
    }
  }
  
  public void setSystemCode(final String systemCode) {
    setPropertyValue("systemCode", systemCode);
    
  }
  
  public boolean hasReachedOrderReview() {
    return getSpcState() >= SPC_STATE_ORDER_REVIEW;
  }
  
  public boolean hasVerifiedSpcPayment() {
    return getSpcState() >= SPC_STATE_VERIFED_PAYMENT;
  }
  
  public boolean hasStartedCheckedOut() {
    return getSpcState() != SPC_STATE_NONE;
  }
  
  public int getSpcState() {
    int returnValue = SPC_STATE_NONE;
    
    final Integer integer = (Integer) getPropertyValue("spcState");
    
    if (integer != null) {
      returnValue = integer.intValue();
    }
    
    return returnValue;
  }
  
  // EDO changes
  
  /**
   * This method gets the employee login status. This method gets the value of 'employeeLoginStatus' property
   * 
   * @return the employee login status
   */
  public int getEmployeeLoginStatus() {
    int returnValue = EMPLOYEE_LOGIN_STATUS_NONE;
    final Integer loginStatus = (Integer) getPropertyValue("employeeLoginStatus");
    if (loginStatus != null) {
      returnValue = loginStatus.intValue();
    }
    return returnValue;
  }
  
  /**
   * Increases the current SPC state if it is less than the value passed in.
   * 
   * @param value
   */
  public void advanceSpcState(final int value) {
    final int currentState = getSpcState();
    
    if (currentState < value) {
      setSpcState(value);
    }
  }
  
  /**
   * @param value
   */
  public void setSpcState(final int value) {
    setPropertyValue("spcState", new Integer(value));
  }
  
  // EDO changes
  
  /**
   * This method sets the property 'employeeLoginStatus'.
   * 
   * @param value
   *          the new employee login status
   */
  public void setEmployeeLoginStatus(final int value) {
    setPropertyValue("employeeLoginStatus", new Integer(value));
  }
  
  public void setValidateCountry(final boolean value) {
    setPropertyValue("validateCountry", new Boolean(value));
  }
  
  public boolean shouldValidateCountry() {
    boolean returnValue = false;
    
    final Boolean validateCountry = (Boolean) getPropertyValue("validateCountry");
    
    if (validateCountry != null) {
      returnValue = validateCountry.booleanValue();
    }
    
    return returnValue;
  }
  
  public void setValidateLimitPromoUse(final boolean value) {
    final SystemSpecs systemSpecs = (SystemSpecs) Nucleus.getGlobalNucleus().resolveName("/nm/utils/SystemSpecs");
    final boolean shouldValidateLimitPromo = systemSpecs.isLimitPromoCheckEnabled();
    
    if (shouldValidateLimitPromo) {
      setPropertyValue("validateLimitPromoUse", new Boolean(value));
    } else {
      setPropertyValue("validateLimitPromoUse", new Boolean(false));
    }
  }
  
  public boolean shouldValidateLimitPromoUse() {
    boolean returnValue = false;
    
    final Boolean validateLimitPromoUse = (Boolean) getPropertyValue("validateLimitPromoUse");
    
    if (validateLimitPromoUse != null) {
      returnValue = validateLimitPromoUse.booleanValue();
    }
    
    return returnValue;
  }
  
  // -------------------------------------
  // property: TotalPromotionalItemValue
  // -------------------------------------
  /**
   * Returns the TotalPromotionalItemValue
   */
  public double getTotalPromotionalItemValue() {
    final Double totalPromotionalItemValue = (Double) getPropertyValue("totalPromotionalItemValue");
    final double returnValue = totalPromotionalItemValue == null ? 0.0 : totalPromotionalItemValue.doubleValue();
    // System.out.println("TotalPromotionalItemValue is " + (new Double(returnValue)).toString());
    return returnValue;
  }
  
  /**
   * Sets the TotalPromotionalItemValue
   */
  public void setTotalPromotionalItemValue(final double pTotalPromotionalItemValue) {
    if (getTotalPromotionalItemValue() != pTotalPromotionalItemValue) {
      setPropertyValue("totalPromotionalItemValue", new Double(pTotalPromotionalItemValue));
    }
  }
  
  // -------------------------------------
  // property: TotalPromotionalItemValue
  // -------------------------------------
  /**
   * Returns the lostShipping
   */
  public double getLostShipping() {
    final Double lostShipping = (Double) getPropertyValue("lostShipping");
    final double returnValue = lostShipping == null ? 0.0 : lostShipping.doubleValue();
    // System.out.println("TotalPromotionalItemValue is " + (new Double(returnValue)).toString());
    return returnValue;
  }
  
  /**
   * Sets the TotalPromotionalItemValue
   */
  public void setLostShipping(final double pLostShippingIn) {
    if (getTotalPromotionalItemValue() != pLostShippingIn) {
      setPropertyValue("lostShipping", new Double(pLostShippingIn));
    }
  }
  
  public boolean getHasBeenDiscounted() {
    double subtotal = 0;
    final double dubZero = 0;
    
    // List citems = ((com.nm.commerce.order.NMOrderManager)com.nm.commerce.order.NMOrderManager.getOrderManager()).getShippingGroupCommerceItemRelationships(this);
    final List<NMCommerceItem> citems = getNmCommerceItems();
    
    final Iterator<NMCommerceItem> i = citems.iterator();
    while (i.hasNext()) {
      final CommerceItem item = i.next();
      final NMCommerceItem nmCI = (NMCommerceItem) item;
      if (item.getPriceInfo() != null) {
        if (nmCI.getCoreMetricsCategory() != null) {
          if (!nmCI.isPwpItem()) {
            subtotal += ((NMItemPriceInfo) item.getPriceInfo()).getDiscountAmount();
          }
        } else {
          subtotal += ((NMItemPriceInfo) item.getPriceInfo()).getDiscountAmount();
        }
      }
    }
    if (subtotal > dubZero) {
      return true;
    } else {
      return false;
    }
  } // get total discount from items
  
  public double getTotalDiscountFromItems() {
    double subtotal = 0;
    
    final List<NMCommerceItem> citems = getNmCommerceItems();
    
    final Iterator<NMCommerceItem> i = citems.iterator();
    while (i.hasNext()) {
      final CommerceItem item = i.next();
      if (item.getPriceInfo() != null) {
        subtotal += ((NMItemPriceInfo) item.getPriceInfo()).getDiscountAmount();
      }
    }
    return subtotal;
  } // get total discount from items
  
  public double getDisplayableTotalDiscountFromItems() {
    double subtotal = 0;
    
    final List<NMCommerceItem> citems = getNmCommerceItems();
    
    final Iterator<NMCommerceItem> i = citems.iterator();
    while (i.hasNext()) {
      final CommerceItem item = i.next();
      final NMCommerceItem nmItem = (NMCommerceItem) item;
      if (item.getPriceInfo() != null) {
        
        if (nmItem.getCoreMetricsCategory() != null) {
          if (!nmItem.getCoreMetricsCategory().equals("pwp")) {
            subtotal += ((NMItemPriceInfo) item.getPriceInfo()).getDiscountAmount();
            
          }
        } else {
          subtotal += ((NMItemPriceInfo) item.getPriceInfo()).getDiscountAmount();
          
        }
      }
    }
    // deduct the employee discount from subtoal.
    final double empdiscountTotalAmt = getEmployeeOrderDiscountTotal();
    if (empdiscountTotalAmt > 0.0) {
      subtotal -= empdiscountTotalAmt;
    }
    
    return subtotal;
  } // get total discount from items
  
  /**
   * This method calculates the sum of employee promotion discount value and any additional promotion discount value for each line item and returns the total order discount value.
   * 
   * @return the employee order discount total
   */
  public double getEmployeeOrderDiscountTotal() {
    double totalEmpDiscountAmt = 0;
    final List<NMCommerceItem> citems = getNmCommerceItems();
    for (final NMCommerceItem nmCommerceItem : citems) {
      if (nmCommerceItem.getPriceInfo() != null) {
        final double empDiscount = ((NMItemPriceInfo) nmCommerceItem.getPriceInfo()).getEmployeeDiscountAmount();
        final double empExtraDiscount = ((NMItemPriceInfo) nmCommerceItem.getPriceInfo()).getEmployeeExtraDiscountAmount();
        if ((empDiscount > 0.0) || (empExtraDiscount > 0.0)) {
          final double empDiscountAmt = empDiscount + empExtraDiscount;
          totalEmpDiscountAmt += empDiscountAmt;
        }
      }
    }
    return totalEmpDiscountAmt;
  }
  
  public boolean isOrderEligibleForDiscount() {
    boolean orderEligibleForDiscount = false;
    final List<NMCommerceItem> citems = getNmCommerceItems();
    for (final NMCommerceItem nmCommerceItem : citems) {
      if (nmCommerceItem.getPriceInfo() != null) {
        final double empDiscount = ((NMItemPriceInfo) nmCommerceItem.getPriceInfo()).getEmployeeDiscountAmount();
        if (empDiscount == 0.0) {
          orderEligibleForDiscount = true;
        }
      }
    }
    return orderEligibleForDiscount;
  }
  
  public double getTotalIntParentheticalAmountFromItems() {
    double totalIntParentheticalAmount = 0;
    
    final List<NMCommerceItem> citems = getNmCommerceItems();
    
    final Iterator<NMCommerceItem> i = citems.iterator();
    while (i.hasNext()) {
      final CommerceItem item = i.next();
      final NMCommerceItem nmItem = (NMCommerceItem) item;
      if (nmItem.getProduct().getIntParentheticalCharge() != null) {
        totalIntParentheticalAmount += nmItem.getProduct().getIntParentheticalCharge() * nmItem.getQuantity();
      }
    }
    return totalIntParentheticalAmount;
  }// get total int parenthetical amount from items
  
  public double getTrueRawSubtotal() {
    double subtotal = 0;
    
    final List<NMCommerceItem> citems = getNmCommerceItems();
    
    final Iterator<NMCommerceItem> i = citems.iterator();
    while (i.hasNext()) {
      final CommerceItem item = i.next();
      if (item.getPriceInfo() != null) {
        final NMItemPriceInfo priceInfo = (NMItemPriceInfo) item.getPriceInfo();
        if (priceInfo.getPromotionalPrice() > 0.0) {
          subtotal += priceInfo.getPromotionalPrice();
        } else {
          subtotal += item.getPriceInfo().getRawTotalPrice();
        }
      }
    }
    return subtotal;
    
  } // true raw subtotal
  
  /**
   * Returns a copy of the ignored promo code collection.
   * 
   * @return
   */
  public List<String> getIgnoredPromoCodeList(final int type) {
    final ArrayList<String> returnValue = new ArrayList<String>();
    
    // we return a copy of our ignored promo code list otherwise
    // we would have to rely on the caller to synchronize their
    // access to the collection.
    synchronized (mIgnoredPromoCodeMap) {
      switch (type) {
        case ALL_PROMOTION_TYPES:
          final Collection<ArrayList<String>> arrayLists = mIgnoredPromoCodeMap.values();
          
          final Iterator<ArrayList<String>> iterator = arrayLists.iterator();
          
          while (iterator.hasNext()) {
            final ArrayList<String> promoCodes = iterator.next();
            
            returnValue.addAll(promoCodes);
          }
        break;
        case PERCENT_OFF_PROMOTIONS:
        case GWP_PROMOTIONS:
        case PWP_PROMOTIONS:
        case DOLLAR_OFF_PROMOTIONS:
          final ArrayList<String> promoCodes = mIgnoredPromoCodeMap.get(new Integer(type));
          
          if (promoCodes != null) {
            returnValue.addAll(promoCodes);
          }
        break;
      }
    }
    
    return returnValue;
  }
  
  /**
   * Adds a promo code to the ignored collection. If the promo code is already in the collection then it is not added.
   * 
   * @param promoCode
   */
  public void addIgnoredPromoCode(final int type, final String promoCode) {
    synchronized (mIgnoredPromoCodeMap) {
      switch (type) {
        case ALL_PROMOTION_TYPES:
        case PERCENT_OFF_PROMOTIONS:
        case GWP_PROMOTIONS:
        case PWP_PROMOTIONS:
        case DOLLAR_OFF_PROMOTIONS:
          ArrayList<String> promoCodes = mIgnoredPromoCodeMap.get(new Integer(type));
          
          if (promoCodes == null) {
            promoCodes = new ArrayList<String>();
            
            mIgnoredPromoCodeMap.put(new Integer(type), promoCodes);
          }
          
          promoCodes.add(promoCode);
        break;
      }
    }
  }
  
  /**
   * Clears the collection of ignored promo codes
   */
  public void clearIgnoredPromoCodes(final int type) {
    synchronized (mIgnoredPromoCodeMap) {
      switch (type) {
        case ALL_PROMOTION_TYPES:
          mIgnoredPromoCodeMap.clear();
        break;
        case PERCENT_OFF_PROMOTIONS:
        case GWP_PROMOTIONS:
        case PWP_PROMOTIONS:
        case DOLLAR_OFF_PROMOTIONS:
          mIgnoredPromoCodeMap.remove(new Integer(type));
        break;
      }
    }
  }
  
  /**
   * Removes a promoCode from the ignored list
   * 
   * @return
   */
  public void removeIgnoredPromoCode(final int type, final String promoCode) {
    synchronized (mIgnoredPromoCodeMap) {
      switch (type) {
        case PERCENT_OFF_PROMOTIONS:
        case GWP_PROMOTIONS:
        case PWP_PROMOTIONS:
        case DOLLAR_OFF_PROMOTIONS:
          final ArrayList<String> promoCodes = mIgnoredPromoCodeMap.get(new Integer(type));
          
          if (promoCodes != null) {
            promoCodes.remove(promoCode);
          }
        break;
      }
    }
  }
  
  public void addPromoCode(final String promoCode) {
    if (StringUtilities.isNotEmpty(promoCode)) {
      final String currentValue = getPromoCode();
      final StringBuffer buffer = new StringBuffer();
      
      if (currentValue == null) {
        buffer.append(promoCode).append(",");
      } else {
        buffer.append(currentValue).append(promoCode).append(",");
      }
      if (StringUtilities.isEmpty(getSystemCode())) {
        final SystemSpecs systemSpecs = CommonComponentHelper.getSystemSpecs();
        final String systemCode = systemSpecs.getProductionSystemCode();
        setSystemCode(systemCode);
      }
      setPromoCode(buffer.toString());
    }
  }
  
  public String getPromoCode() {
    final String promoCode = (String) getPropertyValue("promoCode");
    return promoCode;
  }
  
  public void removePromoCode(final String promoCode) {
    if (StringUtilities.isNotEmpty(promoCode)) {
      String currentValue = getPromoCode();
      currentValue = currentValue.replaceAll(promoCode + ",", "");
      resetPromoCode(currentValue);
    }
  }
  
  public void setPromoCode(String promoCode) {
    if (StringUtilities.isNotEmpty(promoCode)) {
      promoCode = StringUtilities.eliminateStringListDups(promoCode);
      setPropertyValue("promoCode", promoCode);
      transientPromoCode = promoCode;
    }
  }
  
  public void resetPromoCode(final String promoCode) {
    if (promoCode != null) {
      setPropertyValue("promoCode", promoCode);
    }
  }
  
  public void resetPromoName(final String promoName) {
    if (promoName != null) {
      setPropertyValue("promoName", promoName);
    }
  }
  
  public void resetPromoNameGWP() {
    final StringTokenizer myToken = new StringTokenizer(getPromoName(), ",");
    while (myToken.hasMoreTokens()) {
      final String testString = myToken.nextToken().trim();
      if (PromotionsHelper.validGWP(testString)) {
        setRemovePromoName(testString);
        // if this GWP has a promo code need to take it out of the activated and useractivated too
        final String thePromoCode = PromotionsHelper.getGWPPromoCode(testString);
        
        if ((thePromoCode != null) && !thePromoCode.trim().equals("")) {
          setRemoveActivatedPromoCode(thePromoCode);
          setRemoveUserActivatedPromoCode(thePromoCode);
        }
        
      }
    }
  }
  
  public void resetPromoNamePWP() {
    final StringTokenizer myToken = new StringTokenizer(getPromoName(), ",");
    while (myToken.hasMoreTokens()) {
      final String testString = myToken.nextToken().trim();
      if (PromotionsHelper.validPWP(testString)) {
        setRemovePromoName(testString);
        // if this GWP has a promo code need to take it out of the activated and useractivated too
        final String thePromoCode = PromotionsHelper.getPWPPromoCode(testString);
        
        if ((thePromoCode != null) && !thePromoCode.trim().equals("")) {
          setRemoveActivatedPromoCode(thePromoCode);
          setRemoveUserActivatedPromoCode(thePromoCode);
        }
        
      }
    }
  }
  
  /**
   * Returns the promo names (keys) as a list.
   * 
   * @return
   */
  public List<String> getPromoNameList() {
    final String promoName = getPromoName();
    
    return StringUtilities.makeList(promoName, ",");
  }
  
  public String getPromoName() {
    return (String) getPropertyValue("promoName");
  }
  
  public void setPromoName(final String promoName) {
    if (promoName != null) {
      final String oldPromoName = getPromoName();
      final String newPromoName = StringUtilities.addValueToDelimitedString(promoName, oldPromoName, ",", 2000);
      setPropertyValue("promoName", newPromoName);
    }
  }
  
  public void setRemovePromoName(String promoName) {
    if ((promoName != null) && !promoName.trim().equals("")) {
      promoName = promoName.trim().toUpperCase();
      String existingpromoName = getPromoName();
      
      if ((existingpromoName == null) || existingpromoName.trim().equals("")) {
        existingpromoName = "";
        return;
      }
      final String removeFlag = "remove";
      final String delimiter = ",";
      final int maxLength = 255;
      existingpromoName = StringUtilities.performAddOrRemovePromoName(removeFlag, promoName, existingpromoName, delimiter, maxLength);
      setPropertyValue("promoName", existingpromoName.trim());
    }
  }
  
  public String getActivatedPromoCode() {
    final String activatedPromoCode = (String) getPropertyValue("activatedPromoCode");
    
    if (activatedPromoCode != null) {
      return activatedPromoCode;
    } else {
      return "";
    }
  }
  
  public void setActivatedPromoCode(String activatedPromoCode) {
    
    if ((activatedPromoCode != null) && !activatedPromoCode.trim().equals("")) {
      activatedPromoCode = activatedPromoCode.trim().toUpperCase();
      String existingActivatedPromoCode = getActivatedPromoCode();
      
      if ((existingActivatedPromoCode == null) || existingActivatedPromoCode.trim().equals("")) {
        existingActivatedPromoCode = "";
      }
      
      // only update order if promo code has not already been added...
      boolean foundIt = false;
      final StringTokenizer myToken = new StringTokenizer(existingActivatedPromoCode, ",");
      while (myToken.hasMoreTokens()) {
        final String testString = myToken.nextToken().trim();
        if (testString.trim().toUpperCase().equals(activatedPromoCode.trim().toUpperCase())) {
          // the promoCode is already there so don;t add it again
          foundIt = true;
          break;
        }
        
      }// end while more tokens
      
      // if (existingActivatedPromoCode.indexOf(activatedPromoCode + ",") == -1) {
      if (!foundIt) {
        existingActivatedPromoCode = existingActivatedPromoCode + activatedPromoCode + ",";
        
        // make sure length does not exceed database constraints...
        final int maxLength = 255;
        
        if (existingActivatedPromoCode.length() > maxLength) {
          existingActivatedPromoCode = existingActivatedPromoCode.substring(0, maxLength);
        }
        
        setPropertyValue("activatedPromoCode", existingActivatedPromoCode);
      }// end check if exists
    }
  }
  
  /**
   * Returns a list of commerceItems that have the promokey
   * 
   * @param promoKey
   * @return
   */
  public List<NMCommerceItem> getCommerceItemsWithPromoKey(final String promoKey) {
    final ArrayList<NMCommerceItem> arrayList = new ArrayList<NMCommerceItem>();
    
    final List<NMCommerceItem> commerceItems = getNmCommerceItems();
    final Iterator<NMCommerceItem> iterator = commerceItems.iterator();
    
    while (iterator.hasNext()) {
      final NMCommerceItem commerceItem = iterator.next();
      final String promoName = commerceItem.getPromoName();
      if ((promoName != null) && (promoName.indexOf(promoKey) >= -1)) {
        arrayList.add(commerceItem);
      }
    }
    
    return arrayList;
  }
  
  public void setRemoveActivatedPromoCode(String activatedPromoCode) {
    if ((activatedPromoCode != null) && !activatedPromoCode.trim().equals("")) {
      activatedPromoCode = activatedPromoCode.trim().toUpperCase();
      String existingActivatedPromoCode = getActivatedPromoCode();
      
      if ((existingActivatedPromoCode == null) || existingActivatedPromoCode.trim().equals("")) {
        existingActivatedPromoCode = "";
      }
      
      final List<String> list = new ArrayList<String>();
      final StringTokenizer st = new StringTokenizer(existingActivatedPromoCode, ",");
      while (st.hasMoreElements()) {
        final String ele = (String) st.nextElement();
        if (ele != null) {
          list.add(ele.trim().toUpperCase());
        }
      }
      
      if (list.contains(activatedPromoCode)) {
        
        final StringBuffer sb = new StringBuffer();
        list.remove(activatedPromoCode);
        final Iterator<String> i = list.iterator();
        while (i.hasNext()) {
          final String addBack = i.next();
          sb.append(addBack);
          sb.append(",");
        }
        
        existingActivatedPromoCode = sb.toString();
        
        // make sure length does not exceed database constraints...
        final int maxLength = 255;
        
        if (existingActivatedPromoCode.length() > maxLength) {
          existingActivatedPromoCode = existingActivatedPromoCode.substring(0, maxLength);
        }
        
        setPropertyValue("activatedPromoCode", existingActivatedPromoCode.trim());
      }
    }
  }
  
  public String getUserActivatedPromoCode() {
    final String activatedPromoCode = (String) getPropertyValue("userActivatedPromoCode");
    
    if (activatedPromoCode != null) {
      return activatedPromoCode;
    } else {
      return "";
    }
    
  }
  
  public void setUserActivatedPromoCode(String userActivatedPromoCode) {
    
    if ((userActivatedPromoCode != null) && !userActivatedPromoCode.trim().equals("")) {
      userActivatedPromoCode = userActivatedPromoCode.trim().toUpperCase();
      String existingUserActivatedPromoCode = getUserActivatedPromoCode();
      
      if ((existingUserActivatedPromoCode == null) || existingUserActivatedPromoCode.trim().equals("")) {
        existingUserActivatedPromoCode = "";
      }
      
      // only update order if promo code has not already been added...
      boolean foundIt = false;
      int tokenCount = 0;
      final StringTokenizer myToken = new StringTokenizer(existingUserActivatedPromoCode, ",");
      
      tokenCount = myToken.countTokens();
      while (myToken.hasMoreTokens()) {
        final String testString = myToken.nextToken().trim();
        if (testString.trim().toUpperCase().equals(userActivatedPromoCode.trim().toUpperCase())) {
          // the promoCode is already there so don;t add it again
          foundIt = true;
          break;
        }
        
      }// end while more tokens
      
      if (!foundIt && (tokenCount <= 5)) {
        
        existingUserActivatedPromoCode = existingUserActivatedPromoCode + userActivatedPromoCode + ", ";
        
        // make sure length does not exceed database constraints...
        final int maxLength = 255;
        
        if (existingUserActivatedPromoCode.length() > maxLength) {
          existingUserActivatedPromoCode = existingUserActivatedPromoCode.substring(0, maxLength);
        }
        
        if (myToken.countTokens() <= 5) {
          setPropertyValue("userActivatedPromoCode", existingUserActivatedPromoCode);
          
        }
      }// end check if exists
    }
  }
  
  public void setRemoveUserActivatedPromoCode(String activatedPromoCode) {
    if ((activatedPromoCode != null) && !activatedPromoCode.trim().equals("")) {
      activatedPromoCode = activatedPromoCode.trim().toUpperCase();
      String existingActivatedPromoCode = getUserActivatedPromoCode();
      
      if ((existingActivatedPromoCode == null) || existingActivatedPromoCode.trim().equals("")) {
        existingActivatedPromoCode = "";
      }
      
      final List<String> list = new ArrayList<String>();
      final StringTokenizer st = new StringTokenizer(existingActivatedPromoCode, ",");
      while (st.hasMoreElements()) {
        final String ele = (String) st.nextElement();
        if (ele != null) {
          list.add(ele.trim().toUpperCase());
        }
      }
      
      if (list.contains(activatedPromoCode)) {
        
        final StringBuffer sb = new StringBuffer();
        boolean isFirst = true;
        list.remove(activatedPromoCode);
        final Iterator<String> i = list.iterator();
        while (i.hasNext()) {
          if (!isFirst) {
            sb.append(", ");
          }
          sb.append(i.next());
          isFirst = false;
        }
        
        existingActivatedPromoCode = sb.toString();
        
        // make sure length does not exceed database constraints...
        final int maxLength = 255;
        
        if (existingActivatedPromoCode.length() > maxLength) {
          existingActivatedPromoCode = existingActivatedPromoCode.substring(0, maxLength);
        }
        
        setPropertyValue("userActivatedPromoCode", existingActivatedPromoCode.trim());
      }
    }
  }
  
  public void rebuildAwardedPromotions() {
    final Set<NMPromotion> awardedPromotions = getAwardedPromotions();
    
    final Iterator<NMPromotion> iterator = awardedPromotions.iterator();
    while (iterator.hasNext()) {
      final NMPromotion promotion = iterator.next();
      final String code = promotion.getPromoCodes();
      if (code != null) {
        setUserActivatedPromoCode(code);
      }
    }
  }
  
  public boolean isTestBadDate() {
    return mTestBadDate;
  }
  
  public void setTestBadDate(final boolean pTestBadDate) {
    mTestBadDate = pTestBadDate;
  }
  
  // --------------------------------------------------------------------------------
  /**
   * vsrb1 Override super method, add debug code to discover when cart's lastmodifieddate property is updated. If TestBadDate==true, set last modified date to 2 weeks ago for testing purposes
   */
  @Override
  public void setLastModifiedDate(Date date) {
    if (mTestBadDate) {
      final long badtime = new Date().getTime() - (1000 * 60 * 60 * 24 * 7 * 2);
      date = new Date(badtime);
    } // end if
    
     if (isLoggingDebug()) {
       logDebug("NMOrderImpl.setLastModifiedDate: ProfileId = " + getProfileId());
       logDebug("NMOrderImpl.setLastModifiedDate: OrderId = " + getId());
       logDebug("NMOrderImpl.setLastModifiedDate: calling super.setLastModifiedDate(Date " + date + ")");
     }
    
    super.setLastModifiedDate(date);
    
    if (isLoggingDebug()) {
      logDebug("NMOrderImpl.setLastModifiedDate: finished super.setLastModifiedDate...");
      logDebug("NMOrderImpl.setLastModifiedDate: getLastModifiedDate() = " + getLastModifiedDate());
      logDebug("NMOrderImpl.setLastModifiedDate: finished\n");
    }
    
  } // end setLastModifiedDate method
  
  /**
   * vsrb1 6/23/2003 Override super method, add debug code to discover when cart's lastmodifieddate property is updated. If TestBadDate==true, set last modified time to 2 weeks ago for testing
   * purposes
   */
  @Override
  public void setLastModifiedTime(long l) {
    if (mTestBadDate) {
      if (isLoggingDebug()) {
    	  logDebug("NMOrderImpl.setLastModifiedTime Setting last modified time to 2 weeks ago for testing purposes.");
      }
      final long badtime = new Date().getTime() - (1000 * 60 * 60 * 24 * 7 * 2);
      l = badtime;
    } // end if
    
    if (isLoggingDebug()) {
      logDebug("NMOrderImpl.setLastModifiedTime: ProfileId = " + getProfileId());
      logDebug("NMOrderImpl.setLastModifiedTime: OrderId = " + getId());
      logDebug("NMOrderImpl.setLastModifiedTime: LastModifiedTime = " + new Date(l).toString());
      logDebug("NMOrderImpl.setLastModifiedTime: calling super.setLastModifiedTime(long " + l + ")");
    } // end if
    
    super.setLastModifiedTime(l);
    
    if (isLoggingDebug()) {
      logDebug("NMOrderImpl.setLastModifiedTime: finished super.setLastModifiedTime...");
      logDebug("NMOrderImpl.setLastModifiedTime: getLastModifiedTime() = " + getLastModifiedTime());
      logDebug("NMOrderImpl.setLastModifiedTime: LastModifiedTime = " + new Date(getLastModifiedTime()).toString());
      logDebug("NMOrderImpl.setLastModifiedTime: finished...\n");
    } // end if
    
  } // end setLastModifiedTime method
  
  // -------------------------------------------------------------------------------
  /**
   * vsrb1 7/24/2003 New method used (in debugging) to compare version number in the repository item with version number in the Order object via getVersion() method Returns -1000 for null repository
   * item or unreadable version in repository.
   */
  public int getRepositoryItemVersion() {
    int i = -1000;
    final MutableRepositoryItem mutablerepositoryitem = getRepositoryItem();
    if (mutablerepositoryitem != null) {
      final Integer integer = (Integer) mutablerepositoryitem.getPropertyValue("version");
      if (integer != null) {
        i = integer.intValue();
      } // end if
    } // end if
    return i;
  }// end getRepositoryItemVersion method
  
  public void setInfoMessages(final ArrayList<String> pInfoMessages) {
    mInfoMessages = pInfoMessages;
  }
  
  public ArrayList<String> getInfoMessages() {
    return mInfoMessages;
  }
  
  public void flushInfoMessages() {
    mInfoMessages = new ArrayList<String>();
  } //
  
  public void setRemovedItemDetails(final HashMap<String, String> removedItemDetail) {
    removedItemDetails = removedItemDetail;
  }
  
  public HashMap<String, String> getRemovedItemDetails() {
    return removedItemDetails;
  }
  
  public void setNeedingCartUtilityUpdate(final boolean pNeedingCartUtilityUpdate) {
    mNeedingCartUtilityUpdate = pNeedingCartUtilityUpdate;
  }
  
  public boolean isNeedingCartUtilityUpdate() {
    return mNeedingCartUtilityUpdate;
  }
  
  public boolean cartContainsOnlyGiftlistItems() {
    boolean onlyGiftlistItems = false;
    final List<NMCommerceItem> commerceItems = getNmCommerceItems();
    final Iterator<NMCommerceItem> commerceItemsIterator = commerceItems.iterator();
    
    while (commerceItemsIterator.hasNext()) {
      final NMCommerceItem nmci = commerceItemsIterator.next();
      final String glId = nmci.getRegistryId();
      if ((glId != null) && !glId.trim().equals("")) {
        onlyGiftlistItems = true;
      } else {
        onlyGiftlistItems = false;
        break;
      }
    }
    
    return onlyGiftlistItems;
  }
  
  public String getCcAppStatusTransient() {
    return (String) getPropertyValue("ccAppStatusTransient");
  }
  
  public void setCcAppStatusTransient(final String ccAppStatusTransient) {
    setPropertyValue("ccAppStatusTransient", ccAppStatusTransient);
  }
  
  public String getCcAppStatus() {
    return (String) getPropertyValue("ccAppStatus");
  }
  
  public void setCcAppStatus(final String ccAppStatus) {
    setPropertyValue("ccAppStatus", ccAppStatus);
  }
  
  public Map<String, String> getGwpSelectPromoMap() {
    @SuppressWarnings({"unchecked"})
    final Map<String, String> value = (Map<String, String>) getPropertyValue("gwpSelectPromoMap");
    return value;
  }
  
  public void setGwpSelectPromoMap(final Map<String, String> gwpSelectPromoMap) {
    setPropertyValue("gwpSelectPromoMap", gwpSelectPromoMap);
  }
  
  public Map<String, Map<String, Object>> getGwpMultiSkuPromoMap() {
    @SuppressWarnings("unchecked")
    final Map<String, Map<String, Object>> map = (Map<String, Map<String, Object>>) getPropertyValue("gwpMultiSkuPromoMap");
    return map;
  }
  
  public void setGwpMultiSkuPromoMap(final Map<String, Map<String, Object>> map) {
    setPropertyValue("gwpMultiSkuPromoMap", map);
  }
  
  public Map<String, Map<String, Object>> getPwpMultiSkuPromoMap() {
    @SuppressWarnings("unchecked")
    final Map<String, Map<String, Object>> map = (Map<String, Map<String, Object>>) getPropertyValue("pwpMultiSkuPromoMap");
    return map;
  }
  
  public void setPwpMultiSkuPromoMap(final Map<String, Map<String, Object>> map) {
    setPropertyValue("pwpMultiSkuPromoMap", map);
  }
  
  public Set<String> getUsedLimitPromoMap() {
    @SuppressWarnings("unchecked")
    final Set<String> value = (Set<String>) getPropertyValue("usedLimitPromoMap");
    return value;
  }
  
  public void setUsedLimitPromoMap(final Set<String> map) {
    setPropertyValue("usedLimitPromoMap", map);
  }
  
  public void clearUsedLimitPromoMap() {
    getUsedLimitPromoMap().clear();
  }
  
  public String getLsSiteId() {
    return (String) getPropertyValue("lsSiteId");
  }
  
  public void setLsSiteId(final String lsSiteId) {
    setPropertyValue("lsSiteId", lsSiteId);
  }
  
  public String getMid() {
    return (String) getPropertyValue("mid");
  }
  
  public void setMid(final String mid) {
    setPropertyValue("mid", mid);
  }
  
  public String getLsSiteTime() {
    return (String) getPropertyValue("lsSiteTime");
  }
  
  public void setLsSiteTime(final String lsSiteTime) {
    if ((lsSiteTime != null) && (lsSiteTime.length() <= 20)) {
      setPropertyValue("lsSiteTime", lsSiteTime);
    } else {
      if (isLoggingDebug()) {
        logDebug("Invalid value for lsSiteTime detected: " + lsSiteTime + " orderId: " + this.getRepositoryItem().getRepositoryId());
      }
      setPropertyValue("lsSiteTime", "");
    }
  }
  
  public String getVendorId() {
    return (String) getPropertyValue("vendorId");
  }
  
  public void setVendorId(final String vendorId) {
    setPropertyValue("vendorId", vendorId);
  }
  
  public String getVendorOrderId() {
    return (String) getPropertyValue("vendorOrderId");
  }
  
  public void setVendorOrderId(final String vendorOrderId) {
    setPropertyValue("vendorOrderId", vendorOrderId);
  }
  

    /**
   * Gets the incircle earned level.
   * 
   * @return the incircle earned level
   */
  public String getIncircleEarnedLevel() {
    return (String) getPropertyValue(INCIRCLE_EARNED_LEVEL);
  }
  
  /**
   * Sets the incircle earned level.
   * 
   * @param incircleEarnedLevel
   *          the new incircle earned level
   */
  public void setIncircleEarnedLevel(final String incircleEarnedLevel) {
    setPropertyValue(INCIRCLE_EARNED_LEVEL, incircleEarnedLevel);
  }
  
  /**
   * Gets the incircle bonus points.
   * 
   * @return the incircle bonus points
   */
  public String getIncircleBonusPoints() {
    return (String) getPropertyValue(INCIRCLE_BONUS_POINTS);
  }
  
  /**
   * Sets the incircle bonus points.
   * 
   * @param incircleBonusPoints
   *          the new incircle bonus points
   */
  public void setIncircleBonusPoints(final String incircleBonusPoints) {
    setPropertyValue(INCIRCLE_BONUS_POINTS, incircleBonusPoints);
  }

  public Integer getTotalNumberOfItems() {
    int itemCount = 0;
    
    try {
      final List<NMCommerceItem> cItemsList = getNmCommerceItems();
      final Iterator<NMCommerceItem> cItemsListIterator = cItemsList.iterator();
      
      while (cItemsListIterator.hasNext()) {
        final long qty = cItemsListIterator.next().getQuantity();
        itemCount += qty;
      }
      // System.out.println("itemCount-->" + itemCount + "");
      
    } catch (final Exception e) {}
    
    return new Integer(itemCount);
    
  }
  
  public void clearGwpMultiSkuPromoMap() {
    getGwpMultiSkuPromoMap().clear();
  }
  
  /**
   * @param promoKey
   * @param productId
   */
  public void addGwpMultiSkuPromoToMap(final String promoKey, final String promoCode, final String productId, final long quantity) {
    
    final Map<String, Object> entry = getGwpMultiSkuPromotionEntry(promoKey, promoCode, productId, quantity);
    
    getGwpMultiSkuPromoMap().put(promoKey, entry);
  }
  
  /**
   * @param promoKey
   * @param productId
   */
  public void addPwpMultiSkuPromoToMap(final String promoKey, final String promoCode, final String productId, final long quantity) {
    
    final Map<String, Object> entry = getGwpMultiSkuPromotionEntry(promoKey, promoCode, productId, quantity);
    
    getPwpMultiSkuPromoMap().put(promoKey, entry);
  }
  
  /**
   * @param promoKey
   * @param productId
   */
  public void addPwpRejectPromoToMap(final String promoKey, final String promoCode, final String productId) {

    getPwpRejectPromoMap().add(promoKey);
  }
  
  /**
   * @param promoKey
   * @return
   */
  public boolean hasPwpRejectPromoProductId(final String promoKey) {
    boolean returnValue = false;
    
    final Set<String> declinedPwpSelects = getPwpRejectPromoMap();
    if (declinedPwpSelects != null) {
      if (declinedPwpSelects.contains(promoKey)) {
        returnValue = true;
      }
    }
    
    return returnValue;
  }
  
  private Set<String> getPwpRejectPromoMap() {
    final DynamoHttpServletRequest request = ServletUtil.getCurrentRequest();
    final NMOrderHolder orderHolder = CheckoutComponents.getOrderHolder(request);
    final Set<String> declinedPwps = orderHolder.getDeclinedPwps();
    return declinedPwps == null ? new HashSet<String>() : declinedPwps;
  }
  
  /**
   * @param promoKey
   */
  public void addUsedLimitPromoToMap(final String promoKey) {
    getUsedLimitPromoMap().add(promoKey);
  }
  
  /**
   * @param promoKey
   * @param quantity
   */
  public void decrementGwpMultiSkuPromoMapEntry(final String promoKey, final long quantity) {
    final Map<String, Map<String, Object>> map = getGwpMultiSkuPromoMap();
    decrementMultiSkuPromoMapEntry(map, promoKey, quantity);
  }
  
  /**
   * @param promoKey
   * @param quantity
   */
  public void decrementPwpMultiSkuPromoMapEntry(final String promoKey, final long quantity) {
    final Map<String, Map<String, Object>> map = getPwpMultiSkuPromoMap();
    decrementMultiSkuPromoMapEntry(map, promoKey, quantity);
  }
  

  /**
   * @param promoKey
   * @param quantity
   */
  public void decrementMultiSkuPromoMapEntry(final Map<String, Map<String, Object>> map, final String promoKey, final long quantity) {
    
    final Map<String, Object> entry = map.get(promoKey);
    
    if (entry != null) {
      final long oldQuantity = (Long) entry.get("quantity");
      
      if (oldQuantity <= quantity) {
        map.remove(promoKey);
      } else {
        entry.put("quantity", (oldQuantity - quantity));
      }
    }
  }
  
  public void removeGwpMultiSkuPromoMapEntry(final String promoKey) {
    final Map<String, Map<String, Object>> map = getGwpMultiSkuPromoMap();
    removeMultiSkuPromoMapEntry(map, promoKey);
  }
  
  public void removePwpMultiSkuPromoMapEntry(final String promoKey) {
    final Map<String, Map<String, Object>> map = getPwpMultiSkuPromoMap();
    removeMultiSkuPromoMapEntry(map, promoKey);
  }
  
  public void removeMultiSkuPromoMapEntry(final Map<String, Map<String, Object>> map, final String promoKey) {
    final Map<String, Object> entry = map.get(promoKey);
    if (entry != null) {
      map.remove(promoKey);
    }
  }
  
  public void removeGwpMultiSkuForProduct(final String productId) {
    final Map<String, Map<String, Object>> map = getGwpMultiSkuPromoMap();
    removeMultiSkuForProduct(map, productId);
  }
  
  public void removePwpMultiSkuForProduct(final String productId) {
    final Map<String, Map<String, Object>> map = getPwpMultiSkuPromoMap();
    removeMultiSkuForProduct(map, productId);
  }
  
  public void removeMultiSkuForProduct(final Map<String, Map<String, Object>> map, final String productId) {
    final Collection<Map<String, Object>> values = map.values();
    final Iterator<Map<String, Object>> iterator = values.iterator();
    while (iterator.hasNext()) {
      final Map<String, Object> entry = iterator.next();
      
      if (entry.get("productId").equals(productId)) {
        iterator.remove();
        final String promoKey = (String) entry.get("promoKey");
        final String promoCode = (String) entry.get("promoCode");
        if (promoKey != null) {
          setRemovePromoName(promoKey);
          removeAwardedPromotionByKey(promoKey);
        }
        
        if (promoCode != null) {
          setRemoveActivatedPromoCode(promoCode);
          setRemoveUserActivatedPromoCode(promoCode);
        }
      }
    }
  }
  
  /**
   * @param promoKey
   * @return
   */
  public String getGwpMultiSkuProductId(final String promoKey) {
    final Map<String, Map<String, Object>> map = getGwpMultiSkuPromoMap();
    return getMultiSkuProductId(map, promoKey);
  }
    
  /**
   * @param promoKey
   * @return
   */
  public String getPwpMultiSkuProductId(final String promoKey) {
    final Map<String, Map<String, Object>> map = getPwpMultiSkuPromoMap();
    return getMultiSkuProductId(map, promoKey);
  }
  
  /**
   * @param promoKey
   * @return
   */
  public String getMultiSkuProductId(final Map<String, Map<String, Object>> map, final String promoKey) {
    String returnValue = null;
    final Map<String, Object> entry = map.get(promoKey);
    
    if (entry != null) {
      returnValue = (String) entry.get("productId");
    }
    
    return returnValue;
  }
  
  
  public Collection<String> getGwpMultiSkuProductIds() {
    final Map<String, Map<String, Object>> map = getGwpMultiSkuPromoMap();
    return getMultiSkuProductIds(map);
  }
  
  public Collection<String> getPwpMultiSkuProductIds() {
    final Map<String, Map<String, Object>> map = getPwpMultiSkuPromoMap();
    return getMultiSkuProductIds(map);
  }

  public Collection<String> getMultiSkuProductIds(final Map<String, Map<String, Object>> map) {
    final ArrayList<String> arrayList = new ArrayList<String>();
    final Collection<Map<String, Object>> values = map.values();
    final Iterator<Map<String, Object>> iterator = values.iterator();
    while (iterator.hasNext()) {
      final Map<String, Object> entry = iterator.next();
      arrayList.add((String) entry.get("productId"));
    }
    
    return arrayList;
  }
  
  /**
   * @param promoKey
   * @return
   */
  public boolean hasPwpMultiSkuProductId(final String promoKey) {
    boolean returnValue = false;
    
    final Map<String, Map<String, Object>> map = getPwpMultiSkuPromoMap();
    
    final Map<String, Object> entry = map.get(promoKey);
    
    if (entry != null) {
      returnValue = true;
    }
    
    return returnValue;
  }

  
  /**
   * @return
   */
  public String getFirstGwpMultiSkuPromoKey() {
    final Set<String> keys = getGwpMultiSkuPromoMap().keySet();
    
    final Iterator<String> mapEntrySetIterator = keys.iterator();
    
    final String key = mapEntrySetIterator.next();
    
    return key;
  }
  
  /**
   * @return
   */
  public String getFirstPwpMultiSkuPromoKey() {
    final Set<String> keys = getPwpMultiSkuPromoMap().keySet();
    
    final Iterator<String> mapEntrySetIterator = keys.iterator();
    
    final String key = mapEntrySetIterator.next();
    
    return key;
  }
  
  public boolean getHasGwpMultiSkuPromo() {
    return !getGwpMultiSkuPromoMap().isEmpty();
  }
  
  public boolean getHasPwpMultiSkuPromo() {
    return !getPwpMultiSkuPromoMap().isEmpty();
  }
  
  public boolean getHasGwpSelectPromo() {
    return !getGwpSelectPromoMap().isEmpty();
  }
  
  public Map.Entry<String, String> getNextGwpSelectMapEntry() {
    
    final Set<Map.Entry<String, String>> mapEntrySet = getGwpSelectPromoMap().entrySet();
    
    final Iterator<Map.Entry<String, String>> mapEntrySetIterator = mapEntrySet.iterator();
    
    final Map.Entry<String, String> mapEntry = mapEntrySetIterator.next();
    
    mapEntrySet.remove(mapEntry);
    
    return mapEntry;
  }
  
  /**
   * Returns the promo codes as a list
   * 
   * @param order
   * @return
   */
  public List<String> getPromoCodeList() {
    final String orderPromoCode = getPromoCode();
    
    return StringUtilities.makeList(orderPromoCode, ",");
  }
  
  /**
   * Returns that active promo codes as a list.
   * 
   * @return
   */
  public List<String> getActivePromoCodeList() {
    final String orderPromoCode = getActivatedPromoCode();
    return StringUtilities.makeList(orderPromoCode, ",");
  }
  
  public boolean hasNoStoreFulfilledandDropshipFlag() {
    final boolean returnValue = false;
    final List<NMCommerceItem> commerceItemsList = getNmCommerceItems();
    final Iterator<NMCommerceItem> iterator = commerceItemsList.iterator();
    
    while (iterator.hasNext()) {
      final NMCommerceItem commerceItem = iterator.next();
      if (!commerceItem.isShipFromStore() && !commerceItem.isDropship()) {
        return true;
      }
    }
    return returnValue;
  }
  
  public int getFailedCCAttemptCount() {
    return failedCCAttemptCount;
  }
  
  public void setFailedCCAttemptCount(final int failedCCAttemptCount) {
    this.failedCCAttemptCount = failedCCAttemptCount;
  }
  
  public void incrementFailedCCAttemptCount() {
    failedCCAttemptCount++;
  }
  
  public boolean hasReachedFailedCCAttemptLimit() {
    if (failedCCAttemptCount < 5) {
      return false;
    } else {
      return true;
    }
  }
  
  public int incOutstandingEvents() {
    return ++outstandingEventCount;
  }
  
  public int decOutstandingEvents() {
    return --outstandingEventCount;
  }
  
  /**
   * Wait for all outstanding scenario events for this order to be processed.
   * 
   * @param timeout
   *          Timeout in milliseconds - if events are not cleared in this amount of time the method will return.
   * @return true if all events cleared or false if return due to timeout.
   */
  public boolean waitForOutstandingEvents(final long timeout) {
    final long endTime = System.currentTimeMillis() + timeout;
    while ((outstandingEventCount > 0) && (System.currentTimeMillis() <= endTime)) {
      try {
        Thread.sleep(100L);
      } catch (final InterruptedException e) {}
    }
    
    if (outstandingEventCount > 0) {
      outstandingEventCount = 0;
      return false;
    }
    
    outstandingEventCount = 0;
    return true;
  }
  
  @Override
  protected void preEnsureContainers() {
    final OrderManager om = OrderManager.getOrderManager();
    boolean needToRepairOrder = false;
    final Set<String> itemsWithNoShipGroup = new HashSet<String>();
    final MutableRepositoryItem repItem = getRepositoryItem();
    
    if (repItem == null) {
      return;
    }
    
    @SuppressWarnings("unchecked")
    List<GSAItem> commerceItems = (List<GSAItem>) repItem.getPropertyValue("commerceItems");
    
    if (commerceItems == null) {
      return;
    }
    
    Iterator<GSAItem> commerceItemsIter = commerceItems.iterator();
    
    while (commerceItemsIter.hasNext()) {
      final atg.adapter.gsa.GSAItem commerceItem = commerceItemsIter.next();
      
      if (commerceItem == null) {
        om.logError("Null commerce item reference detected for order " + repItem.getRepositoryId());
        needToRepairOrder = true;
      } else {
        itemsWithNoShipGroup.add(commerceItem.getRepositoryId());
      }
    }
    
    @SuppressWarnings("unchecked")
    List<GSAItem> relationships = (List<GSAItem>) repItem.getPropertyValue("relationships");
    
    if (relationships != null) {
      final Iterator<GSAItem> relationshipsIter = relationships.iterator();
      
      while (relationshipsIter.hasNext()) {
        final atg.adapter.gsa.GSAItem relationship = relationshipsIter.next();
        
        if (relationship == null) {
          om.logError("Null relationship detected for order " + repItem.getRepositoryId());
          needToRepairOrder = true;
        } else {
          final String relationshipType = (String) relationship.getPropertyValue("relationshipType");
          
          if (relationshipType == null) {
            om.logError("Null relationship type detected for order " + repItem.getRepositoryId());
            needToRepairOrder = true;
          } else if (relationshipType.equals("SHIPPINGQUANTITY")) {
            final atg.adapter.gsa.GSAItem commerceItem = (atg.adapter.gsa.GSAItem) relationship.getPropertyValue("commerceItem");
            
            if (commerceItem == null) {
              om.logError("Null relationship commerce item detected for order " + repItem.getRepositoryId());
              needToRepairOrder = true;
            } else {
              itemsWithNoShipGroup.remove(commerceItem.getRepositoryId());
            }
            
            if (relationship.getPropertyValue("shippingGroup") == null) {
              om.logError("Null relationship shipping group detected for order " + repItem.getRepositoryId());
              needToRepairOrder = true;
            }
          }
        }
      }
    }
    
    if (itemsWithNoShipGroup.size() > 0) {
      om.logError("Commerce item with no shipping group detected for order " + repItem.getRepositoryId());
      needToRepairOrder = true;
    }
    
    if (needToRepairOrder) {
      final String threadName = Thread.currentThread().getName();
      final atg.servlet.DynamoHttpServletRequest req = ServletUtil.getCurrentRequest();
      
      final String orderId = repItem.getRepositoryId();
      final String profileId = (String) repItem.getPropertyValue("profileId");
      String sessionId = "";
      
      if (req != null) {
        sessionId = req.getSession(false).getId();
      } else {
        om.logError("Unable to retrieve current request from ServletUtil");
      }
      
      om.logError("Invalid order data detected.  Attempting to repair order.  [threadName=" + threadName + ";orderId=" + orderId + ";profileId=" + profileId + ";sessionId=" + sessionId + "]");
      
      final List<GSAItem> validCommerceItems = new ArrayList<GSAItem>();
      @SuppressWarnings("unchecked")
      final List<GSAItem> commerceItemList = (List<GSAItem>) repItem.getPropertyValue("commerceItems");
      commerceItems = commerceItemList;
      commerceItemsIter = commerceItems.iterator();
      
      while (commerceItemsIter.hasNext()) {
        final atg.adapter.gsa.GSAItem commerceItem = commerceItemsIter.next();
        
        if (commerceItem != null) {
          if (itemsWithNoShipGroup.contains(commerceItem.getRepositoryId())) {
            // tried to assign this commerce item to the default shipping group
            // but could not get it to work correctly, as an alternative we
            // will simply not add it back to the cart, which essentially removes
            // the item, do not uncomment this code without thorough testing
            
            /*
             * try { ShippingGroupManager sgm = om.getShippingGroupManager(); CommerceItemManager cim = om.getCommerceItemManager(); ShippingGroup sg = sgm.createShippingGroup();
             * cim.addItemQuantityToShippingGroup(this, commerceItem.getRepositoryId(), sg.getId(), ((Long)commerceItem.getPropertyValue("quantity")).longValue()); sgm.addShippingGroupToOrder(this,
             * sg); validCommerceItems.add(commerceItem); } catch (Exception e) { om.logError("Unable to repair commerce item with no shipping group.  Item will be removed from cart.", e); }
             */
          } else {
            validCommerceItems.add(commerceItem);
          }
        }
      }
      
      repItem.setPropertyValue("commerceItems", validCommerceItems);
      
      final List<GSAItem> validRelationships = new ArrayList<GSAItem>();
      @SuppressWarnings("unchecked")
      final List<GSAItem> relationshipList = (List<GSAItem>) repItem.getPropertyValue("relationships");
      relationships = relationshipList;
      
      if (relationships == null) {
        return;
      }
      
      final Iterator<GSAItem> relationshipsIter = relationships.iterator();
      
      while (relationshipsIter.hasNext()) {
        final atg.adapter.gsa.GSAItem relationship = relationshipsIter.next();
        
        if (relationship != null) {
          final String relationshipType = (String) relationship.getPropertyValue("relationshipType");
          
          if ((relationshipType != null) && relationshipType.equals("SHIPPINGQUANTITY")) {
            if (relationship.getPropertyValue("commerceItem") != null) {
              validRelationships.add(relationship);
            }
          } else {
            validRelationships.add(relationship);
          }
        }
      }
      
      repItem.setPropertyValue("relationships", validRelationships);
    }
  }
  
  public String getTransientPromoCode() {
    return transientPromoCode;
  }
  
  public void setTransientPromoCode(final String transientPromoCode) {
    this.transientPromoCode = StringUtilities.eliminateStringListDups(transientPromoCode);
  }
  
  public String debugShippingGroups(final boolean includeCommerceItems) {
    final StringBuffer returnValue = new StringBuffer();
    
    @SuppressWarnings("unchecked")
    final List<HardgoodShippingGroup> shippingGroups = getShippingGroups();
    
    final Iterator<HardgoodShippingGroup> iterator = shippingGroups.iterator();
    
    returnValue.append("Order Id: ").append(getRepositoryItem().getRepositoryId()).append("\n");
    
    while (iterator.hasNext()) {
      final HardgoodShippingGroup shippingGroup = iterator.next();
      final Address address = shippingGroup.getShippingAddress();
      returnValue.append("=====================\n");
      returnValue.append("ShippingGroup: " + shippingGroup.getRepositoryItem().getRepositoryId()).append("\n");
      returnValue.append("First Name: ").append(address.getFirstName()).append("\n");
      returnValue.append("Last Name: ").append(address.getLastName()).append("\n");
      returnValue.append("Address1: ").append(address.getAddress1()).append("\n");
      returnValue.append("State: ").append(address.getState()).append("\n");
      returnValue.append("Country: ").append(address.getCountry()).append("\n");
      
      if (includeCommerceItems) {
        @SuppressWarnings("unchecked")
        final List<ShippingGroupCommerceItemRelationship> commerceItems = shippingGroup.getCommerceItemRelationships();
        returnValue.append("---------\n");
        if ((commerceItems != null) && (commerceItems.size() > 0)) {
          final Iterator<ShippingGroupCommerceItemRelationship> commerceItemIterator = commerceItems.iterator();
          
          while (commerceItemIterator.hasNext()) {
            final ShippingGroupCommerceItemRelationship ciRel = commerceItemIterator.next();
            returnValue.append("Rel Commerce Item Id: ").append(ciRel.getCommerceItem().getId());
            returnValue.append(" rel qty: " + ciRel.getQuantity());
            
            final CommerceItem commerceItem = ciRel.getCommerceItem();
            returnValue.append(" ci qty: " + commerceItem.getQuantity()).append("\n");
            
          }
        } else {
          returnValue.append("No Commerce Items\n");
        }
      }
    }
    
    returnValue.append("===================================\n\n\n");
    
    return returnValue.toString();
  }
  
  public String debugPaymentGroups() {
    String returnValue = "";
    
    returnValue = "payment group info:\n";
    returnValue += "payment group count/relationship count: " + getPaymentGroupCount() + " \\ " + getPaymentGroupRelationshipCount() + "\n";
    @SuppressWarnings("unchecked")
    final List<NMCreditCard> paymentGroups = super.getPaymentGroups();
    final Iterator<NMCreditCard> iterator = paymentGroups.iterator();
    
    while (iterator.hasNext()) {
      final NMCreditCard paymentGroup = iterator.next();
      final Address address = paymentGroup.getBillingAddress();
      
      returnValue += "id: " + paymentGroup.getId() + "\n";
      returnValue += "address line1: " + address.getAddress1() + "\n";
      returnValue += "credit card number: " + paymentGroup.getCreditCardNumber() + "\n";
      returnValue += "credit encrypt: " + paymentGroup.getCreditCardNumberEncrypted() + "\n";
      returnValue += "credit card type: " + paymentGroup.getCreditCardType() + "\n";
      returnValue += "amount remaining: " + paymentGroup.getAmountRemaining() + "\n";
      returnValue += "isPrepaid: " + paymentGroup.isPrepaidCard() + "\n";
      returnValue += "amount: " + paymentGroup.getAmount() + "\n";
    }
    
    return returnValue;
  }
  
  public String debugPriceInfos() {
    final StringBuffer returnValue = new StringBuffer();
    
    returnValue.append("Order Id: ").append(getRepositoryItem().getRepositoryId()).append("\n");
    
    final OrderPriceInfo priceInfo = getPriceInfo();
    
    if (priceInfo != null) {
      returnValue.append("orderPriceInfo hashcode: ").append(priceInfo.hashCode()).append("\n");
      returnValue.append("orderPriceInfo amount: ").append(priceInfo.getAmount()).append("\n");
      returnValue.append("orderPriceInfo rawsubtotal: ").append(priceInfo.getRawSubtotal()).append("\n");
      returnValue.append("orderPriceInfo total: ").append(priceInfo.getTotal()).append("\n");
    } else {
      returnValue.append("orderPriceInfo is null\n");
    }
    
    final List<NMCommerceItem> commerceItems = getNmCommerceItems();
    final Iterator<NMCommerceItem> iterator = commerceItems.iterator();
    while (iterator.hasNext()) {
      final NMCommerceItem commerceItem = iterator.next();
      returnValue.append("commerceItem id: " + commerceItem.getId()).append("\n");
      final ItemPriceInfo itemPriceInfo = commerceItem.getPriceInfo();
      if (itemPriceInfo != null) {
        returnValue.append("itemPriceInfo hashcode: ").append(itemPriceInfo.hashCode()).append("\n");
        returnValue.append("itemPriceInfo amount: ").append(itemPriceInfo.getAmount()).append("\n");
        returnValue.append("itemPriceInfo rawRawTotalPrice: ").append(itemPriceInfo.getRawTotalPrice()).append("\n");
        returnValue.append("itemPriceInfo quantityDiscounted: ").append(itemPriceInfo.getQuantityDiscounted()).append("\n");
        returnValue.append("itemPriceInfo quantityAsQualifier: ").append(itemPriceInfo.getQuantityAsQualifier()).append("\n");
      }
    }
    
    return returnValue.toString();
  }
  
  public String debugAddresses() {
    final StringBuffer returnValue = new StringBuffer();
    
    @SuppressWarnings("unchecked")
    final List<HardgoodShippingGroup> shippingGroups = getShippingGroups();
    
    final Iterator<HardgoodShippingGroup> iterator = shippingGroups.iterator();
    
    returnValue.append("Order Id: ").append(getRepositoryItem().getRepositoryId()).append("\n");
    
    while (iterator.hasNext()) {
      final HardgoodShippingGroup shippingGroup = iterator.next();
      final NMRepositoryContactInfo address = (NMRepositoryContactInfo) shippingGroup.getShippingAddress();
      returnValue.append("=====================\n");
      returnValue.append("ShippingGroup: " + shippingGroup.getRepositoryItem().getRepositoryId()).append("\n");
      returnValue.append("First Name: ").append(address.getFirstName()).append("\n");
      returnValue.append("Last Name: ").append(address.getLastName()).append("\n");
      returnValue.append("Address1: ").append(address.getAddress1()).append("\n");
      returnValue.append("State: ").append(address.getState()).append("\n");
      returnValue.append("Country: ").append(address.getCountry()).append("\n");
      returnValue.append("Verification Flag: ").append(address.getVerificationFlag()).append("\n");
      returnValue.append("Address Type: ").append(address.getAddressType()).append("\n");
      returnValue.append("County Code: ").append(address.getCountyCode()).append("\n");
    }
    
    returnValue.append("payment group info:\n");
    @SuppressWarnings("unchecked")
    final List<NMCreditCard> paymentGroups = getPaymentGroups();
    final Iterator<NMCreditCard> paymentIterator = paymentGroups.iterator();
    
    while (paymentIterator.hasNext()) {
      final NMCreditCard paymentGroup = paymentIterator.next();
      final NMRepositoryContactInfo address = (NMRepositoryContactInfo) paymentGroup.getBillingAddress();
      
      returnValue.append("First Name: ").append(address.getFirstName()).append("\n");
      returnValue.append("Last Name: ").append(address.getLastName()).append("\n");
      returnValue.append("Address1: ").append(address.getAddress1()).append("\n");
      returnValue.append("State: ").append(address.getState()).append("\n");
      returnValue.append("Country: ").append(address.getCountry()).append("\n");
      returnValue.append("Verification Flag: ").append(address.getVerificationFlag()).append("\n");
      returnValue.append("Address Type: ").append(address.getAddressType()).append("\n");
      returnValue.append("County Code: ").append(address.getCountyCode()).append("\n");
    }
    
    return returnValue.toString();
  }
  
  public String debugPromotions() {
    final StringBuffer returnValue = new StringBuffer("");
    
    returnValue.append("Order id: ").append(getId()).append("\n");
    
    returnValue.append("activatedPromoCode: ").append(getActivatedPromoCode()).append("\n");
    returnValue.append("userActivedPromoCode: ").append(getUserActivatedPromoCode()).append("\n");
    returnValue.append("promoCode: ").append(getPromoCode()).append("\n");
    returnValue.append("promoName: ").append(getPromoName()).append("\n");
    returnValue.append("Activated Promotions\n");
    
    final Collection<NMPromotion> activatedPromotions = getAwardedPromotions();
    
    final Iterator<NMPromotion> iterator = activatedPromotions.iterator();
    while (iterator.hasNext()) {
      final NMPromotion promotion = iterator.next();
      returnValue.append(promotion.getCode()).append(":").append(promotion.getPromoCodes()).append("\n");
    }
    
    if (getHasGwpMultiSkuPromo()) {
      returnValue.append("GWP Multi Sku Map:\n");
      final Map<String, Map<String, Object>> gwpMap = getGwpMultiSkuPromoMap();
      final Collection<Map<String, Object>> values = gwpMap.values();
      final Iterator<Map<String, Object>> entryIterator = values.iterator();
      while (entryIterator.hasNext()) {
        final Map<String, Object> entry = entryIterator.next();
        returnValue.append(entry.get("promoKey")).append(" - ").append(entry.get("promoCode")).append(" - ").append(entry.get("productId")).append("\n");
      }
    }
    
    if (getHasGwpSelectPromo()) {
      returnValue.append("GWP Select Map:\n");
      final Map<String, String> gwpMap = getGwpSelectPromoMap();
      final Set<Map.Entry<String, String>> entrySet = gwpMap.entrySet();
      final Iterator<Map.Entry<String, String>> entryIterator = entrySet.iterator();
      while (entryIterator.hasNext()) {
        final Map.Entry<String, String> entry = entryIterator.next();
        returnValue.append(entry.getKey()).append(" - - ").append(entry.getValue()).append("\n");
      }
    }
    
    @SuppressWarnings("unchecked")
    final List<HardgoodShippingGroup> shippingGroups = getShippingGroups();
    
    final Iterator<HardgoodShippingGroup> shippingIterator = shippingGroups.iterator();
    
    final boolean isFirstCommerceItem = true;
    
    while (shippingIterator.hasNext()) {
      final HardgoodShippingGroup shippingGroup = shippingIterator.next();
      @SuppressWarnings("unchecked")
      final List<ShippingGroupCommerceItemRelationship> commerceItems = shippingGroup.getCommerceItemRelationships();
      if ((commerceItems != null) && (commerceItems.size() > 0)) {
        final Iterator<ShippingGroupCommerceItemRelationship> commerceItemIterator = commerceItems.iterator();
        
        while (commerceItemIterator.hasNext()) {
          final ShippingGroupCommerceItemRelationship ciRel = commerceItemIterator.next();
          final NMCommerceItem commerceItem = (NMCommerceItem) ciRel.getCommerceItem();
          
          final String promoName = commerceItem.getPromoName();
          if (!StringUtilities.isEmpty(promoName)) {
            if (isFirstCommerceItem) {
              returnValue.append("Promos on Commerce Items:\n---------------------\n");
            }
            returnValue.append("Commerce Item Id: ").append(commerceItem.getId());
            returnValue.append(" promo code: ").append(promoName).append("\n");
          }
        }
      } else {
        returnValue.append("No Commerce Items\n");
      }
    }
    
    // getPromoName
    
    returnValue.append("===================================\n\n\n");
    
    return returnValue.toString();
  }
  
  public boolean containsCommerceItem(final String commerceItemId) {
    final List<NMCommerceItem> commerceItems = getNmCommerceItems();
    for (final Iterator<NMCommerceItem> it = commerceItems.iterator(); it.hasNext();) {
      final NMCommerceItem commerceItem = it.next();
      if (commerceItem.getId().equals(commerceItemId)) {
        return true;
      }
    }
    
    return false;
  }
  
  /**
   * This method returns promo eligible items for dollar off promo evaluation. If/when we consolidate promo engine for all promo types, this will need to be changed to give consideration to all
   * promotion types
   */
  @Override
  public List<NMCommerceItem> getPromoEligibleOrderItems() {
    final PromotionsHelper helper = new PromotionsHelper();
    final List<NMCommerceItem> orderItems = new ArrayList<NMCommerceItem>();
    
    final List<NMCommerceItem> commerceItems = getNmCommerceItems();
    final Iterator<NMCommerceItem> i = commerceItems.iterator();
    while (i.hasNext()) {
      final NMCommerceItem commerceItem = i.next();
      // if commerceitem already has a dollar off award on its price info
      // we want to exclude it from the eligible item set
      boolean dollarOffAwarded = false;
      final NMItemPriceInfo priceInfo = (NMItemPriceInfo) commerceItem.getPriceInfo();
      
      if (priceInfo != null) {
        final Map<String, Markdown> markdownMap = priceInfo.getPromotionsApplied();
        
        if (markdownMap != null) {
          final Collection<Markdown> markdowns = markdownMap.values();
          
          if (markdowns != null) {
            final Iterator<Markdown> markdownI = markdowns.iterator();
            while (markdownI.hasNext()) {
              
              final Markdown markdown = markdownI.next();
              
              // if we already have a Dollar Off markdown type in our applied promos
              // then do nothing with this item...it is not promo eligible
              if ((markdown != null) && (markdown.getType() == Markdown.DOLLAR_OFF)) {
                dollarOffAwarded = true;
              }
            }
          }
        }
      }
      if (dollarOffAwarded) {
        continue;
      }
      
      if (helper.verifyNotExcludedItem(commerceItem)) {
        orderItems.add(commerceItem);
      }
    }
    return orderItems;
    
  }
  
  @Override
  public List<NMCommerceItem> getOrderItems() {
    final List<NMCommerceItem> orderItems = new ArrayList<NMCommerceItem>();
    
    final List<NMCommerceItem> cis = getNmCommerceItems();
    final Iterator<NMCommerceItem> i = cis.iterator();
    while (i.hasNext()) {
      final NMCommerceItem commerceItem = i.next();
      orderItems.add(commerceItem);
    }
    return orderItems;
  }
  
  private void clearItemLevelPromotions() {
    
    final List<NMCommerceItem> cis = getNmCommerceItems();
    final Iterator<NMCommerceItem> i = cis.iterator();
    while (i.hasNext()) {
      final NMCommerceItem ci = i.next();
      final NMItemPriceInfo priceInfo = (NMItemPriceInfo) ci.getPriceInfo();
      
      priceInfo.setAmount(priceInfo.getListPrice() * ci.getQuantity());
      priceInfo.setRawTotalPrice(priceInfo.getAmount());
      
      priceInfo.setOrderDiscountShare(0);
      priceInfo.getPromotionsApplied().clear();
      priceInfo.setAmountIsFinal(false);
      priceInfo.setDiscounted(false);
      
      ci.setPriceInfo(priceInfo);
    }
  }
  
  public List<NMCommerceItem> getNmCommerceItems() {
    @SuppressWarnings("unchecked")
    final List<NMCommerceItem> nmCommerceItems = getCommerceItems();
    return nmCommerceItems;
  }
  
  /**
   * Checks if the order is international
   * 
   * @author nmve1
   */
  public boolean isInternationalOrder() {
    boolean isInternationalFlg = false;
    // final FiftyOneUtils fiftyOneUtils = ((FiftyOneUtils) Nucleus.getGlobalNucleus().resolveName("/nm/utils/fiftyone/FiftyOneUtils"));
    if ((null != getVendorId()) && !getVendorId().trim().equals("")) {
      if (getVendorId().equals(FiftyOneUtils.VENDOR_ID)) {
        isInternationalFlg = true;
      }
    }
    return isInternationalFlg;
  }
  
  public boolean hasReplenishmentItems() {
    for (final NMCommerceItem item : getNmCommerceItems()) {
      if (!StringUtils.isEmpty(item.getSelectedInterval())) {
        return true;
      }
    }
    return false;
  }
  
  public boolean getHasBackOrderItems() {
    final ProdSkuUtil prodSkuUtil = CommonComponentHelper.getProdSkuUtil();
    for (final NMCommerceItem item : getNmCommerceItems()) {
      if (prodSkuUtil.getStatusBackorderString().equals(item.getTransientStatus())) {
        return true;
      }
    }
    return false;
  }
  
  public boolean getHasPreOrBackOrderItems() {
    final ProdSkuUtil prodSkuUtil = CommonComponentHelper.getProdSkuUtil();
    for (final NMCommerceItem item : getNmCommerceItems()) {
      if (prodSkuUtil.getStatusBackorderString().equals(item.getTransientStatus()) || prodSkuUtil.getStatusPreOrderString().equals(item.getTransientStatus())) {
        return true;
      }
    }
    return false;
  }
  
  public boolean hasActiveReplishmentItems() {
    boolean hasValue = false;
    for (final NMCommerceItem item : getNmCommerceItems()) {
      final String interval = item.getSelectedInterval();
      if (!StringUtils.isEmpty(interval)) {
        try {
          final int i_val = Integer.valueOf(interval);
          if ((i_val != CommerceItemUpdate.NO_INTERVAL) && (i_val != CommerceItemUpdate.NO_VALUE)) {
            hasValue = true;
            break;
          }
        } catch (final NumberFormatException e) {
        	if (logger.isLoggingDebug()) { 
        		logger.logError("Unable to recognize interval value " + interval + " while evaluating item " + item.getId());
        	}
        }
      }
    }
    return hasValue;
  }
  
  public boolean hasShipToStoreAndActiveReplenishmentItem() {
    boolean hasValue = false;
    for (final NMCommerceItem item : getNmCommerceItems()) {
      final String interval = item.getSelectedInterval();
      if (!StringUtils.isEmpty(interval)) {
        try {
          final int i_val = Integer.valueOf(interval);
          if ((i_val != CommerceItemUpdate.NO_INTERVAL) && (i_val != CommerceItemUpdate.NO_VALUE) && item.isShipToStore()) {
            hasValue = true;
            break;
          }
        } catch (final NumberFormatException e) {
          if (logger.isLoggingError()) { 
        	  logger.logError("Unable to recognize interval value " + interval + " while evaluating item " + item.getId());
          }
        }
      }
    }
    return hasValue;
  }
  
  public List<NMCreditCard> getGiftCards() {
    final NMOrderManager orderMgr = (NMOrderManager) OrderManager.getOrderManager();
    final NMPaymentGroupManager paymentGroupManager = (NMPaymentGroupManager) orderMgr.getPaymentGroupManager();
    return paymentGroupManager.getPaymentGroups(this, new int[] {NMPaymentGroupManager.GIFT_CARDS});
  }
  
  public List<NMCreditCard> getCreditCards() {
    final NMOrderManager orderMgr = (NMOrderManager) OrderManager.getOrderManager();
    final NMPaymentGroupManager paymentGroupManager = (NMPaymentGroupManager) orderMgr.getPaymentGroupManager();
    return paymentGroupManager.getPaymentGroups(this, new int[] {NMPaymentGroupManager.CREDIT_CARDS});
  }
  
  public List<NMCreditCard> getPrepaidCards() {
    final NMOrderManager orderMgr = (NMOrderManager) OrderManager.getOrderManager();
    final NMPaymentGroupManager paymentGroupManager = (NMPaymentGroupManager) orderMgr.getPaymentGroupManager();
    return paymentGroupManager.getPaymentGroups(this, new int[] {NMPaymentGroupManager.PREPAID_CARDS});
  }
  
  public String getPayPalAuthorizationId() {
    return (String) getPropertyValue("payPalAuthorizationId");
  }
  
  public void setPayPalAuthorizationId(final String authorizationId) {
    setPropertyValue("payPalAuthorizationId", authorizationId);
  }
  
  public String getPayPalOrderId() {
    return (String) getPropertyValue("payPalOrderId");
  }
  
  public void setPayPalOrderId(final String orderId) {
    setPropertyValue("payPalOrderId", orderId);
  }
  
  /***
   * Method to check if VisaChecout Paym
   * 
   * @return boolean indicating the if visa checkout pay group on order
   */
  public boolean isVisaCheckoutPayGroupOnOrder() {
    boolean isVisaCheckoutPayGroupFound = false;
    final List<NMCreditCard> payGroups = getCreditCards();
    if ((payGroups != null) && !payGroups.isEmpty()) {
      final NMCreditCard payGroup = payGroups.get(0);
      isVisaCheckoutPayGroupFound = (payGroup != null) && payGroup.isVmeCard();
    }
    return isVisaCheckoutPayGroupFound;
  }
  
  /***
   * Method to check if the Order has MasterPass Payment
   * 
   * @return boolean indicating the if MasterPass pay group on order
   */
  public boolean isMasterPassPayGroupOnOrder() {
    boolean isMasterPassPayGroupFound = false;
    final List<NMCreditCard> payGroups = getCreditCards();
    if (!NmoUtils.isEmptyCollection(payGroups)) {
      final NMCreditCard payGroup = payGroups.get(0);
      if ((payGroup != null) && payGroup.isMasterPassCard()) {
        isMasterPassPayGroupFound = true;
      }
    }
    return isMasterPassPayGroupFound;
  }
  
  /**
   * This method will return the current checkout type
   * 
   * @return checkoutType
   */
  public String getCheckoutType() {
    String checkoutType = EMPTY_STRING;
    final List<NMCreditCard> payGroups = getCreditCards();
    if (!NmoUtils.isEmptyCollection(payGroups)) {
      final NMCreditCard payGroup = payGroups.get(0);
      if ((null != payGroup) && !StringUtils.isEmpty(payGroup.getCurrentCheckoutType())) {
        checkoutType = payGroup.getCurrentCheckoutType();
      }
    }
    return checkoutType;
  }
  
  /**
   * 
   * Returns true if the order contains at least one ICE (Precious Jewelry) item
   * 
   * @author nmve1
   * @return true or false
   */
  public boolean isICEOrder() {
    for (final NMCommerceItem item : getNmCommerceItems()) {
      if (item.getCmosItemCode().toUpperCase().startsWith("P") || item.getCmosItemCode().toUpperCase().startsWith("J")) {
        return true;
      }
    }
    
    return false;
  }
  
  public boolean isPPRestrictedOrder() {
    if (logger.isLoggingDebug()){
    	logger.logDebug("--->> NMOI isPPRestrictedOrder is getting called");
    }
    for (final NMCommerceItem item : getNmCommerceItems()) {
      if (item.isPayPalRestricted()) {
    	  if (logger.isLoggingDebug()){
    		  logger.logDebug("--->> NMOI isPPRestrictedOrder is returning TRUE");
    	  }
        return true;
      }
    }
    
    return false;
  }
  
  public boolean isShopRunnerPromoActive() {
    return getActivatedPromoCode().indexOf("SHOPRUNNER") != -1;
  }
  
  public boolean isInCirclePromoActive() {
    return getActivatedPromoCode().indexOf("INCIRCLE") != -1;
  }
  
  public boolean isShopRunnerOrder() {
    boolean isShopRunnerOrder = false;
    
    if (!isCanadianOrder()) {
      
      if (!isShopRunnerPromoActive()) {
        isShopRunnerOrder = false;
      } else {
        
        final List<NMCommerceItem> items = getNmCommerceItems();
        final Iterator<NMCommerceItem> iter = items.iterator();
        
        while (iter.hasNext()) {
          final NMCommerceItem nmci = iter.next();
          
          if (nmci.getProduct().getIsShopRunnerEligible() && nmci.getServicelevel().equalsIgnoreCase(ServiceLevel.SL2_SERVICE_LEVEL_TYPE)) {
            isShopRunnerOrder = true;
            break;
          }
        }
        
      }
    }
    return isShopRunnerOrder;
  }
  
  /**
   * Method to determine where order is eligible for free delivery and processing. Rules: 1. Item should not be SUBS product. 2. Item should not contain parenthetical charges 3. Item is not Physical
   * or Virtual Gift-card 4. Order doesn't include parenthetical charges
   * */
  public boolean isEligibleForFreeDeliveryProcessing() {
    boolean additionalProcessingRequired = false;
    final List<NMCommerceItem> commerceItemsList = this.getNmCommerceItems();
    if (!commerceItemsList.isEmpty()) {
      if (isLoggingDebug()) {
        logDebug("Check if order " + this.getId() + " is eligble for free processing.");
      }
      
      for (final NMCommerceItem commerceItem : commerceItemsList) {
        final NMProduct product = commerceItem.getProduct();
        final String merchandiseType = product.getMerchandiseType();
        
        if (product.isSUBSProduct() || ((null != product.getParentheticalCharge()) && (product.getParentheticalCharge() != 0.0))
                || ((StringUtilities.isNotEmpty(merchandiseType)) && (merchandiseType.equals("7") || merchandiseType.equals("6")))) {
          if (isLoggingDebug()) {
            logDebug("Additional processing required for the product - " + product.getCmosItemCode() + "|Merch Type: " + merchandiseType + "|Is CatalogBook item : " + product.isSUBSProduct()
                    + "|Parenthetical Charges : " + product.getParentheticalCharge());
          }
          additionalProcessingRequired = true;
          break;
        }
      }
    } else {
      // When no items found, there is no free processing or processing fee applies.
      additionalProcessingRequired = true;
      if (isLoggingDebug()) {
        logDebug("No commerce items found. No free processing.");
      }
    }
    
    final NMOrderPriceInfo priceInfo = (NMOrderPriceInfo) this.getPriceInfo();
    
    if ((priceInfo != null) && (priceInfo.getDeliveryAndProcessingTotal() > 0)) {
      additionalProcessingRequired = true;
      if (isLoggingDebug()) {
        logDebug("Order " + this.getId() + " is not eligble for free processing. Processing fee : " + priceInfo.getDeliveryAndProcessingTotal());
      }
    }
    if (isLoggingDebug()) {
      logDebug("Order " + this.getId() + " is eligble for free processing : " + !additionalProcessingRequired);
    }
    return !additionalProcessingRequired;
  }
  
  public boolean getEligibleForFreeDeliveryProcessing() {
    return isEligibleForFreeDeliveryProcessing();
  }
  
  /**
   * 
   * Following code will set the property internationalPriceItem for the international product copy block for China Name of Method: setInternationalPriceItem
   */
  public void setInternationalPriceItem(final RepositoryItem internationalPriceItem) {
    setPropertyValue("internationalPriceItem", internationalPriceItem);
  }
  
  /**
   * 
   * Following code will get the property internationalPriceItem for the international product copy block for China Name of Method: getInternationalPriceItem
   */
  public RepositoryItem getInternationalPriceItem() {
    return (RepositoryItem) getPropertyValue("internationalPriceItem");
  }
  
  public void setDutyPaymentMethodAndQuoteTotal(final String dutyPayMethod, final double total, final double shipping, final double handling, final double duties, final double taxes) {
    final MutableRepositoryItem intlOrdPriceInfo = (MutableRepositoryItem) getPropertyValue("internationalPriceItem");
    if (intlOrdPriceInfo != null) {
      if (null != dutyPayMethod) {
        setDutiesPaymentMethod(dutyPayMethod);
      }
      intlOrdPriceInfo.setPropertyValue("totalPrice", total);
      intlOrdPriceInfo.setPropertyValue("extraShipping", shipping);
      intlOrdPriceInfo.setPropertyValue("extraHandling", handling);
      intlOrdPriceInfo.setPropertyValue("duties", duties);
      intlOrdPriceInfo.setPropertyValue("taxes", taxes);
    }
  }
  
  public void setQuoteIdUsed(final String quoteId) {
    setPropertyValue("quoteIdUsed", quoteId);
  }
  
  public Long getQuoteIdUsed() {
    final String quote = (String) getPropertyValue("quoteIdUsed");
    Long i = new Long(0);
    if (!StringUtils.isBlank(quote) && !StringUtils.isBlank(quote.replaceAll("\\D", ""))) {
      // Remove non digit values. just to make sure we get proper quoteId.
      i = Long.parseLong(quote.replaceAll("\\D", ""));
    }
    return i;
  }
  
  public String getCountry() {
    return (String) getPropertyValue("countryCode");
  }
  
  // INT-451 changes starts - getters and setters for the repository item dutiesPaymentMethod
  public void setDutiesPaymentMethod(final String dutiesPaymentMethod) {
    setPropertyValue("dutiesPaymentMethod", dutiesPaymentMethod);
  }
  
  public String getDutiesPaymentMethod() {
    return (String) getPropertyValue("dutiesPaymentMethod");
  }
  
  public void setMerchantOrderId(final String merchantOrderId) {
    // MerchantOrderId should be unique and set only if not available.
    if (getMerchantOrderId() == null) {
      setPropertyValue("merchantOrderId", merchantOrderId);
    }
  }
  
  public String getMerchantOrderId() {
    return (String) getPropertyValue("merchantOrderId");
  }
  
  public Integer getDeliveryPromiseMin() {
    return (Integer) getPropertyValue("deliveryPromiseMin");
  }
  
  public void setDeliveryPromiseMin(final Integer deliveryPromiseMin) {
    setPropertyValue("deliveryPromiseMin", deliveryPromiseMin);
  }
  
  public Integer getDeliveryPromiseMax() {
    return (Integer) getPropertyValue("deliveryPromiseMax");
  }
  
  public void setDeliveryPromiseMax(final Integer deliveryPromiseMax) {
    setPropertyValue("deliveryPromiseMax", deliveryPromiseMax);
  }
  
  // INT-451 changes ends - getters and setters for the repository item dutiesPaymentMethod
  // START-Added for preventing multiple orders in Borderfree system with same merchant order id
  public boolean isOrderStatusCheckRequired() {
    final Boolean checkRequired = (Boolean) getPropertyValue("isOrderStatusCheckRequired");
    return (checkRequired != null) && checkRequired.booleanValue();
  }
  
  public void setOrderStatusCheckRequired(final boolean checkRequired) {
    setPropertyValue("isOrderStatusCheckRequired", new Boolean(checkRequired));
  }
  
  public String getInternationalDebugStatusMessage() {
    return (String) getPropertyValue("debugStatusMessage");
  }
  
  public void setInternationalDebugStatusMessage(final String debugStatusMessage) {
    setPropertyValue("debugStatusMessage", debugStatusMessage);
  }
  
  // END-Added for preventing multiple orders in Borderfree system with same merch order id
  
  public boolean hasBopsItem() {
    for (final NMCommerceItem item : getNmCommerceItems()) {
      if (StringUtilities.isNotEmpty(item.getPickupStoreNo())) {
        return true;
      }
    }
    return false;
    
  }
  
  /**
   * Different from hasAllBopsItems() because this takes into account ship to store flag -
   */
  public boolean isHasAllPickupInStoreItems() {
    for (final NMCommerceItem item : getNmCommerceItems()) {
      if (StringUtilities.isEmpty(item.getPickupStoreNo()) || item.getShipToStoreFlg()) {
        return false;
      }
    }
    return true;
  }
  
  /**
   * Loops through all commerce items to see if at least one is locally priced
   */
  public boolean isHasLocallyPricedItem() {
    for (final NMCommerceItem item : getNmCommerceItems()) {
      if (item.getProduct().getProductDisplayPrice().isHasLocalizedPrice()) {
        return true;
      }
    }
    return false;
  }
  
  public boolean hasAllBopsItems() {
    for (final NMCommerceItem item : getNmCommerceItems()) {
      if (StringUtilities.isEmpty(item.getPickupStoreNo())) {
        return false;
      }
    }
    return true;
  }
  
  public boolean getHasAllBopsItems() {
    return hasAllBopsItems();
  }
  
  public boolean getHasBopsItem() {
    return hasBopsItem();
  }
  
  public Set<String> getActiveDynamicCodes() {
    return activeDynamicCodes;
  }
  
  public void setActiveDynamicCodes(final Set<String> activeDynamicCodes) {
    this.activeDynamicCodes = activeDynamicCodes;
  }
    
  /**
   * @return the alipay
   */
  public boolean isAlipay() {
    return alipay;
  }
  
  /**
   * @param alipay
   *          the alipay to set
   */
  public void setAlipay(final boolean alipay) {
    this.alipay = alipay;
  }
  
  /**
   * <p>
   * This method returns the tender type used in order like ShopRunner,Alipay etc.
   * </p>
   * 
   * @return tenderType
   */
  public String getTenderType() {
    String tenderType = CREDIT_CARD;
    if (isVisaCheckoutPayGroupOnOrder()) {
      tenderType = VISA_CHECKOUT_WALLET;
    } else {
      final String vendorId = getVendorId();
      if ((vendorId != null) && vendorId.equalsIgnoreCase(ALIPAY_CONSTANT)) {
        tenderType = ALIPAY;
      } else if ((vendorId != null) && (vendorId.equalsIgnoreCase(SHOPRUNNER_CONSTANT) || vendorId.equalsIgnoreCase(SHOPRUNNER_EXPRESS_CONSTANT))) {
        tenderType = SHOPRUNNER;
      }
    }
    return tenderType;
  }
  
  public Map<String, Object> getGwpMultiSkuPromotionEntry(final String promoKey, final String promoCode, final String productId, final long quantity) {
    Map<String, Object> gwpMultiSkuPromotionEntry = new HashMap<String, Object>();
    gwpMultiSkuPromotionEntry.put("promoKey", promoKey);
    gwpMultiSkuPromotionEntry.put("promoCode", promoCode);
    gwpMultiSkuPromotionEntry.put("productId", productId);
    gwpMultiSkuPromotionEntry.put("quantity", quantity);
    
    return gwpMultiSkuPromotionEntry;
  }
  
  /**
   * @return the testIndicator
   */
  public String getTestIndicator() {
    return (String) getPropertyValue("testIndicator");
  }
  
  /**
   * @param testIndicator
   *          the testIndicator to set
   */
  public void setTestIndicator(final String testIndicator) {
    setPropertyValue("testIndicator", testIndicator);
  }
  
  /**
   * 
   * @return isLoggingDebug.
   */
  private boolean isLoggingDebug() {
    return CommonComponentHelper.getLogger().isLoggingDebug();
  }
  
  /**
   * 
   * @param msg
   *          the meg to set logDebug
   */
  private void logDebug(final String msg) {
    CommonComponentHelper.getLogger().logDebug(msg);
  }
  
  /**
   * The purpose of this method is to check whether the order contains only virtual gift card items.
   * 
   * @return the checks for all virtual gift card items
   */
  public boolean isOrderHasAllVirtualGiftCardItems() {
    for (final NMCommerceItem item : getNmCommerceItems()) {
      if (!item.isVirtualGiftCard()) {
        return false;
      }
    }
    return true;
  }
  
  /**
   * The purpose of this method is to get the estimatedDeliveryPeriod.
   * 
   * @return the estimatedDeliveryPeriod
   */
  public String getEstimatedDeliveryPeriod() {
    return (String) getPropertyValue("estimatedDeliveryPeriod");
  }
  
  /**
   * The purpose of this method is to set the estimatedDeliveryPeriod.
   * 
   * @param estimatedDeliveryPeriod
   *          the new estimated delivery period
   */
  public void setEstimatedDeliveryPeriod(String estimatedDeliveryPeriod) {
    if (StringUtilities.isNotNull(estimatedDeliveryPeriod)) {
    	estimatedDeliveryPeriod =
                (estimatedDeliveryPeriod.replace(APPROXIMATE_DELIVERY, EMPTY_STRING)).replace(DELIVERY, EMPTY_STRING).replaceAll("[" + OPEN_PARENTHESIS + CLOSE_PARENTHESIS + ")]", EMPTY_STRING);
      setPropertyValue("estimatedDeliveryPeriod", estimatedDeliveryPeriod.trim());
    }
  }
  
  /**
   * The purpose of this method is to check if is order has all dropship item eligible to display service levels.
   * 
   * @return true, if is order has all dropship item eligible to display service levels
   */
  public boolean isOrderHasAllDropshipItemNotEligibleToDisplayServiceLevels() {
    for (final NMCommerceItem item : getNmCommerceItems()) {
      if (!item.isDropship() || item.isDropShipMerchTypeEligibleToDisplayServiceLevels()) {
        return false;
      }
    }
    return true;
  }
  
  /**
   * The purpose of the method is to check if the order contains at least one ship from store item in it.
   * 
   * @return true, if is order contains ship from store item
   */
  public boolean isShipFromStoreItemInCurrentOrder() {
    boolean shipFromStore = false;
    for (final NMCommerceItem item : getNmCommerceItems()) {
      if (item.isShipFromStore()) {
        shipFromStore = true;
        break;
      }
    }
    return shipFromStore;
  }
  
  /**
   * Gets the order merchandise total which is not including any product or order level discounts, tax, or shipping
   * 
   * @return the order merchandise total
   */
  public double getOrderMerchandiseTotal() {
    final List<NMCommerceItem> commerceItems = getNmCommerceItems();
    double orderMerchandiseTotal = 0.0;
    for (final NMCommerceItem item : commerceItems) {
      orderMerchandiseTotal += item.getPriceInfo().getRawTotalPrice();
    }
    return orderMerchandiseTotal;
  }
  
  /**
   * Gets the total count of GWP items in the order.
   * 
   * @return the total GWP items count
   */
  public int getOrderGwpCount() {
    final List<NMCommerceItem> commerceItems = getNmCommerceItems();
    int giftItemCount = 0;
    for (final NMCommerceItem item : commerceItems) {
      if (item.isGwpItem()) {
        giftItemCount++;
      }
    }
    return giftItemCount;
  }
  
  /**
   * Gets the Store Object List.
   * 
   * @return the store object list
   */
  public List<Store> getStoreList() {
    final List<Store> storeList = new ArrayList<Store>();
    final List<NMCommerceItem> commerceItems = getCommerceItems();
    for (NMCommerceItem commerceItem : commerceItems) {
    	final String storeNumber = commerceItem.getPickupStoreNo();
      if (StringUtils.isNotEmpty(storeNumber)) {
        final Store store = CheckoutComponents.getShipToStoreHelper().getStoreAddressByStoreNumber(storeNumber);
        storeList.add(store);
      }
    }
    return storeList;
  }
  
  
  /**
   * Gets the order promo code value.
   * 
   * @return the order promo code value
   */
  public Double getOrderPromoCodeValue() {
    Double promoValue = 0.00;
    final List<NMCommerceItem> commerceItems = getNmCommerceItems();
    Map<String, Markdown> markdownMap;
    for (final NMCommerceItem item : commerceItems) {
      markdownMap = ((NMItemPriceInfo) item.getPriceInfo()).getPromotionsApplied();
      if (!NmoUtils.isEmptyMap(markdownMap)) {
        for (final Map.Entry<String, Markdown> markDown : markdownMap.entrySet()) {
          if (null != markDown.getValue()) {
            promoValue += markDown.getValue().getDollarDiscount();
          }
        }
      }
    }
    return promoValue;
  }
  
  /**
   * The purpose of the method is to check if the order contains at least one drop ship item in it.
   * 
   * @return true, if is order contains drop ship item
   */
  public boolean isDropShipItemInCurrentOrder() {
    boolean dropShipItem = false;
    for (final NMCommerceItem item : getNmCommerceItems()) {
      if (item.isDropship()) {
        dropShipItem = true;
        break;
      }
    }
    return dropShipItem;
  }
  
  /**
   * @return the freeBaseShippingAvailableFlag
   */
  public boolean getFreeBaseShippingAvailableFlag() {
    return freeBaseShippingAvailableFlag;
  }
  
  /**
   * @param freeBaseShippingAvailableFlag
   *          the freeBaseShippingAvailableFlag to set
   */
  public void setFreeBaseShippingAvailableFlag(final boolean freeBaseShippingAvailableFlag) {
    this.freeBaseShippingAvailableFlag = freeBaseShippingAvailableFlag;
  }
  
  /**
   * @return the checkForParentheticalCharges
   */
  public boolean isCheckForParentheticalCharges() {
    return checkForParentheticalCharges;
  }
  
  /**
   * @param checkForParentheticalCharges
   *          the checkForParentheticalCharges to set
   */
  public void setCheckForParentheticalCharges(final boolean checkForParentheticalCharges) {
    this.checkForParentheticalCharges = checkForParentheticalCharges;
  }
  
  /**
   * @return the recentlyChangedCommerceItems
   */
  public List<RecentlyChangedCommerceItem> getRecentlyChangedCommerceItems() {
    if (NmoUtils.isEmptyCollection(recentlyChangedCommerceItems)) {
      recentlyChangedCommerceItems = new ArrayList<RecentlyChangedCommerceItem>();
    }
    return recentlyChangedCommerceItems;
  }
  
  /**
   * This method return the RecentlyChangedCommerce Item based on cmosSku.If not found it returns null.
   * @param cmosSKU
   * @return RecentlyChangedCommerceItem
   */
  public RecentlyChangedCommerceItem getRecentlyChangedCommerceItem(final String cmosSKU){
    RecentlyChangedCommerceItem recentlyChangedItem = null;
    if (StringUtils.isNotEmpty(cmosSKU)) {
      List<RecentlyChangedCommerceItem> changedItems = getRecentlyChangedCommerceItems();
      for (final RecentlyChangedCommerceItem item: changedItems) {
        if (cmosSKU.equals(item.getCartChangeProductCmosSku())) {
          recentlyChangedItem = item;
          break;
        }
      }
    }
    return recentlyChangedItem;
  }
  
  /**
   * @param recentlyChangedCommerceItems
   *          the recentlyChangedCommerceItems to set
   */
  public void setRecentlyChangedCommerceItems(final List<RecentlyChangedCommerceItem> recentlyChangedCommerceItems) {
    this.recentlyChangedCommerceItems = recentlyChangedCommerceItems;
  }
  
  /**
   * Gets the order tender type list. Ex:Gift Card, Visa Checkout, MasterPass, ALi Pay
   * 
   * @return the List of order tender type
   */
  
  public List<String> getOrderTenderTypes() {
    final List<String> orderTenderTypesList = new ArrayList<String>();
    final String vendorId = getVendorId();
    if ((vendorId != null) && vendorId.equalsIgnoreCase(ALIPAY_CONSTANT)) {
      orderTenderTypesList.add(INMGenericConstants.ALIPAY_CONSTANT);
    } else {
      @SuppressWarnings("unchecked")
      final List<NMCreditCard> paymentGroups = getPaymentGroups();
      for (final NMCreditCard paymentGroup : paymentGroups) {
        if (paymentGroup.isVmeCard()) {
          orderTenderTypesList.add(TMSDataDictionaryConstants.VISA_CHECKOUT);
        } else if (paymentGroup.isMasterPassCard()) {
          orderTenderTypesList.add(TMSDataDictionaryConstants.MASTERPASS);
        } else {
          final String type = paymentGroup.getCreditCardType();
          if (TMSDataDictionaryConstants.NEX.equals(type)) {
            orderTenderTypesList.add(TMSDataDictionaryConstants.GIFT_CARD);
          } else {
            orderTenderTypesList.add(type);
          }
        }
      }
    }
    return orderTenderTypesList;
  }
  
  /**
   * Gets the international order duties.
   * 
   * @return the internationalOrderDuties
   */
  public double getInternationalOrderDuties() {
    return internationalOrderDuties;
  }
  
  /**
   * Sets the international order duties.
   * 
   * @param internationalOrderDuties
   *          the internationalOrderDuties to set
   */
  public void setInternationalOrderDuties(final double internationalOrderDuties) {
    this.internationalOrderDuties = internationalOrderDuties;
  }
  
  /**
   * Gets the international shipping cost.
   * 
   * @return the internationalShippingCost
   */
  public double getInternationalShippingCost() {
    return internationalShippingCost;
  }
  
  /**
   * Sets the international shipping cost.
   * 
   * @param internationalShippingCost
   *          the internationalShippingCost to set
   */
  public void setInternationalShippingCost(final double internationalShippingCost) {
    this.internationalShippingCost = internationalShippingCost;
  }
  
  /**
   * Gets the international taxes.
   * 
   * @return the internationalTaxes
   */
  public double getInternationalTaxes() {
    return internationalTaxes;
  }
  
  /**
   * Sets the international taxes.
   * 
   * @param internationalTaxes
   *          the internationalTaxes to set
   */
  public void setInternationalTaxes(final double internationalTaxes) {
    this.internationalTaxes = internationalTaxes;
  }
  
  public int getPromoOrigin() {
	return promoOrigin;
  }

  public void setPromoOrigin(int promoOrigin) {
	this.promoOrigin = promoOrigin;
  }

  public boolean hasPlccPromoError() {
	return plccPromoError;
  }

  public void setPlccPromoError(boolean plccPromoError) {
	this.plccPromoError = plccPromoError;
  }

  public boolean isUserClickedPlaceOrder() {
	return userClickedPlaceOrder;
  }

  public void setUserClickedPlaceOrder(boolean userClickedPlaceOrder) {
	this.userClickedPlaceOrder = userClickedPlaceOrder;
  }

  public String getSelectedPayment() {
	return selectedPayment;
  }

  public void setSelectedPayment(String selectedPayment) {
	this.selectedPayment = selectedPayment;
  }

  /**
   * @return the wishListAdditionFlag
   */
  public boolean isWishListAdditionFlag() {
    return wishListAdditionFlag;
  }

  /**
   * @param wishListAdditionFlag the wishListAdditionFlag to set
   */
  public void setWishListAdditionFlag(boolean wishListAdditionFlag) {
    this.wishListAdditionFlag = wishListAdditionFlag;
  }

  public boolean isBcPromoError() {
    return bcPromoError;
  }

  public void setBcPromoError(boolean bcPromoError) {
    this.bcPromoError = bcPromoError;
  }

/**
 * @return the changeToPickupInStore
 */
public boolean isChangeToPickupInStore() {
	return changeToPickupInStore;
}

/**
 * @param changeToPickupInStore the changeToPickupInStore to set
 */
public void setChangeToPickupInStore(boolean changeToPickupInStore) {
	this.changeToPickupInStore = changeToPickupInStore;
}

  /**
   * Checks if is buy it now order.
   * 
   * @return true, if is buy it now order
   */
  public boolean isBuyItNowOrder() {
    return NmoUtils.getBooleanPropertyValue(getRepositoryItem(), OrderRepositoryConstants.BUYITNOWORDER);
  }
  
  /**
   * Sets the buy it now order.
   * 
   * @param buyItNowOrder
   *          the new buy it now order
   */
  public void setBuyItNowOrder(boolean buyItNowOrder) {
    setPropertyValue(OrderRepositoryConstants.BUYITNOWORDER, buyItNowOrder);
  }
  
  /**
   * Gets the buy it now commerce item id.
   * 
   * @return the buy it now commerce item id
   */
  public String getBuyItNowCommerceItemId() {
    return (String) getPropertyValue(OrderRepositoryConstants.BUYITNOWORDER_COMMERCE_ID);
  }
  
  /**
   * Sets the buy it now commerce item id.
   * 
   * @param buyItNowCommerceItemId
   *          the new buy it now commerce item id
   */
  public void setBuyItNowCommerceItemId(String buyItNowCommerceItemId) {
    setPropertyValue(OrderRepositoryConstants.BUYITNOWORDER_COMMERCE_ID, buyItNowCommerceItemId);
  }
} // end Class
