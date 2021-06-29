package com.nm.commerce.pagedef.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import atg.commerce.order.ShippingGroupCommerceItemRelationship;

import com.nm.ajax.estimatedshipdate.EstimatedShipDetailBean;
import com.nm.collections.CreditCard;
import com.nm.collections.ServiceLevel;
import com.nm.commerce.NMAddress;
import com.nm.commerce.NMCommerceItem;
import com.nm.commerce.NMCreditCard;
import com.nm.commerce.NMOrderHolder;
import com.nm.commerce.checkout.beans.PromoBean;
import com.nm.commerce.checkout.beans.SaveForLaterItemCollection;
import com.nm.commerce.checkout.beans.ShopRunnerResponse;
import com.nm.commerce.pagedef.model.bean.Breadcrumb;
import com.nm.commerce.pagedef.model.bean.RichRelevanceProductBean;
import com.nm.commerce.pagedef.model.bean.ShippingGroupAux;
import com.nm.commerce.promotion.NMOrderImpl;
import com.nm.formhandler.ShoppingCartHandler;
import com.nm.profile.AddressVerificationData;
import com.nm.repository.stores.Store;
import com.nm.sitemessages.Message;

public class CheckoutPageModel extends PageModel {
  
  // order
  private NMOrderImpl order;
  private double amountRemainingOnOrder;
  private String activePromoCodeText;
  private NMCreditCard orderCreditCard;
  private CreditCard orderCreditCardType;
  private boolean invalidOrderAddresses;
  private boolean showEstimatedOrderTotal;
  private String orderId;
  private boolean displayPayPal;
  private boolean showFinCenModal;
  private boolean showAddressVerificationModal;
  private String addressVerificationId;
  private boolean virtualGiftCardOnly;
  private boolean orderContainsVirtualGiftCard;
  private boolean orderContainsGiftCard;
  private boolean orderContainsWishListItem;
  
  /** The service levels fetched already from cmos. */
  private boolean serviceLevelsFetchedFromCmos = false;
  
  /** The cmos service levels contains s l2. */
  private boolean cmosServiceLevelsContainsSL2 = false;
  
  /** The change in service levels from cmos. */
  private boolean changeInServiceLevelsFromCmos = false;
  
  /** The estimated delivery date cmos call failed. */
  private boolean estimatedDeliveryDateCmosCallFailed = false;
  
  // Rich Relevance server side
  private boolean isChanel;
  private boolean isHermes;
  private RichRelevanceProductBean ymalProducts;
  private boolean serverSideYmal = false; // if we get Ymal recommendation from server side integration
  
  private ShopRunnerResponse shopRunner;
  private boolean orderEligibleForFreeDeliveryProcessing;
  private boolean displayFreeBlitzBanner;
  private boolean displayFreeShipping = false;
  private boolean showIncirclePromoMsg;
  private boolean parentheticalChargesPresent;
  
  /** String variable to hold returns charge banner refreshable content for cart page. */
  private String returnsChargeBannerRefreshableContent;
  /** The unavailable msg for configured item. */
  private Map<String, String> unavailableMsgForConfiguredItem = new HashMap<String, String>();
  private boolean orderEligibleForDiscount = false;
  /** String variable to hold MasterPass Email. */
  private String masterPassEmail;
  /** String variable to hold MasterPass Billing First Name. */
  private String masterPassBillingFirstName;
  /** String variable to hold MasterPass Billing Last Name. */
  private String masterPassBillingLastName;
  
  /** The selected shipping method description. */
  private List<String> selectedShippingMethodDescriptions;
  
  /** The estimated delivery dates. */
  private List<String> estimatedDeliveryDates;
  private boolean plccRepriceOrderFlag;
  
  /** pay pal logged in flag */
  private boolean payPalLoggedIn;
  
  /** vme returned flag */
  private boolean vmeReturned;
  
  /** the property for highAdornPrice*/
  private List<Double> highAdornPrice;
  
  /** the property for high price */
  private List<Double> highPrice;
  
  /** the property for low price */
  private List<Double> lowPrice; 
  
  /** List to populate shipping level error messages */
  private String serviceLevelErrMsg;

  /** The map containing grouped line items based on their estimated delivery dates . */
  private Map<Date, Map<ShippingGroupAux, List<NMCommerceItem>>> groupedLineItemsByEstimatedDeliveryDate;
  
  /** The map containing estimated delivery day or date ranges which are populated against the start date as key. */
  private Map<Date, String> estimatedDeliveryDateRanges;
  
  /** The messages for grouped line items based on items's id. */
  private Map<String, String> estimatedDeliveryDateLineItemMessages;
  
  /** The estimated delivery dates grouping layout. */
  private boolean estimatedDeliveryDateGroupingLayout; 
  
  /** The profile contains billing address. */
  private boolean profileContainsBillingAddress;
  
  /** The profile contains shipping address. */
  private boolean profileContainsShippingAddress;
  
  /** The profile contains payment info. */
  private boolean profileContainsPaymentInfo;
 
  /** The express checkout enable page url. */
  private String expressCheckoutEnablePageUrl;
  
  /** The express checkout billing page url. */
  private String expressCheckoutBillingPageUrl;
  
  /** The express checkout shipping page url. */
  private String expressCheckoutShippingPageUrl;
  
  /** The express checkout payment page url. */
  private String expressCheckoutPaymentPageUrl;
  
  /**
   * @return the masterPassBillingFirstName
   */
  public String getMasterPassBillingFirstName() {
    return masterPassBillingFirstName;
  }
  
  /**
   * @param masterPassBillingFirstName
   *          the masterPassBillingFirstName to set
   */
  public void setMasterPassBillingFirstName(final String masterPassBillingFirstName) {
    this.masterPassBillingFirstName = masterPassBillingFirstName;
  }
  
  /**
   * @return the masterPassBillingLastName
   */
  public String getMasterPassBillingLastName() {
    return masterPassBillingLastName;
  }
  
  /**
   * @param masterPassBillingLastName
   *          the masterPassBillingLastName to set
   */
  public void setMasterPassBillingLastName(final String masterPassBillingLastName) {
    this.masterPassBillingLastName = masterPassBillingLastName;
  }
  
  /**
   * @return the masterPassEmail
   */
  public String getMasterPassEmail() {
    return masterPassEmail;
  }
  
  /**
   * @param masterPassEmail
   *          the masterPassEmail to set
   */
  public void setMasterPassEmail(final String masterPassEmail) {
    this.masterPassEmail = masterPassEmail;
  }
  
  public boolean isOrderEligibleForDiscount() {
    return orderEligibleForDiscount;
  }
  
  public void setOrderEligibleForDiscount(final boolean orderEligibleForDiscount) {
    this.orderEligibleForDiscount = orderEligibleForDiscount;
  }
  
  public boolean isDisplayFreeShipping() {
    return displayFreeShipping;
  }
  
  public void setDisplayFreeShipping(final boolean displayFreeShipping) {
    this.displayFreeShipping = displayFreeShipping;
  }
  
  public boolean isDisplayFreeBlitzBanner() {
    return displayFreeBlitzBanner;
  }
  
  public void setDisplayFreeBlitzBanner(final boolean displayFreeBlitzBanner) {
    this.displayFreeBlitzBanner = displayFreeBlitzBanner;
  }
  
  // vme changes
  private boolean vmeCard;
  private boolean masterPassCard;
  private Map<String, String> vmeErrorMsgMap;
  private boolean profileRegistered;
  private boolean collectShipping = false;
  private String rootUrl;
  private String activePromoCodes;
  private boolean vmeSessionExists;
  private Message masterPassErrorMsg;
  
  // property to hold a list of CommerceItems that are Alipay ineligible
  private List<NMCommerceItem> alipayRestrProdList;
  
  /**
   * @return alipayRestrProdList
   */
  public List<NMCommerceItem> getAlipayRestrProdList() {
    return alipayRestrProdList;
  }
  
  /**
   * @param alipayRestrProdList
   */
  public void setAlipayRestrProdList(final List<NMCommerceItem> alipayRestrProdList) {
    this.alipayRestrProdList = alipayRestrProdList;
  }
  
  /**
   * @return the vmeSessionExists
   */
  public boolean isVmeSessionExists() {
    return vmeSessionExists;
  }
  
  /**
   * @param vmeSessionExists
   *          the vmeSessionExists to set
   */
  public void setVmeSessionExists(final boolean vmeSessionExists) {
    this.vmeSessionExists = vmeSessionExists;
  }
  
  /**
   * @return the vmeErrorMsgMap
   */
  public Map<String, String> getVmeErrorMsgMap() {
    return vmeErrorMsgMap;
  }
  
  /**
   * @param vmeErrorMsgMap
   *          the vmeErrorMsgMap to set
   */
  public void setVmeErrorMsgMap(final Map<String, String> vmeErrorMsgMap) {
    this.vmeErrorMsgMap = vmeErrorMsgMap;
  }
  
  /**
   * @return activePromoCodes
   */
  public String getActivePromoCodes() {
    return activePromoCodes;
  }
  
  /**
   * @param activePromoCodes
   */
  public void setActivePromoCodes(final String activePromoCodes) {
    this.activePromoCodes = activePromoCodes;
  }
  
  /**
   * @return rootUrl
   */
  public String getRootUrl() {
    return rootUrl;
  }
  
  /**
   * @param rootUrl
   */
  public void setRootUrl(final String rootUrl) {
    this.rootUrl = rootUrl;
  }
  
  /**
   * @return the collectShipping
   */
  public boolean isCollectShipping() {
    return collectShipping;
  }
  
  /**
   * @param collectShipping
   *          the collectShipping to set
   */
  public void setCollectShipping(final boolean collectShipping) {
    this.collectShipping = collectShipping;
  }
  
  /**
   * @return the vmeCard
   */
  public boolean isVmeCard() {
    return vmeCard;
  }
  
  /**
   * @param vmeCard
   *          the vmeCard to set
   */
  public void setVmeCard(final boolean vmeCard) {
    this.vmeCard = vmeCard;
  }
  
  /**
   * @return the masterPassCard
   */
  public boolean isMasterPassCard() {
    return masterPassCard;
  }
  
  /**
   * @param masterPassCard
   *          the masterPassCard to set
   */
  public void setMasterPassCard(final boolean masterPassCard) {
    this.masterPassCard = masterPassCard;
  }
  
  /**
   * @return the masterPassErrorMsg
   */
  public Message getMasterPassErrorMsg() {
    return masterPassErrorMsg;
  }
  
  /**
   * @param masterPassErrorMsg
   *          the masterPassErrorMsg to set
   */
  public void setMasterPassErrorMsg(final Message masterPassErrorMsg) {
    this.masterPassErrorMsg = masterPassErrorMsg;
  }
  
  /**
   * @return the profileRegistered
   */
  public boolean isProfileRegistered() {
    return profileRegistered;
  }
  
  /**
   * @param profileRegistered
   *          the profileRegistered to set
   */
  public void setProfileRegistered(final boolean profileRegistered) {
    this.profileRegistered = profileRegistered;
  }
  
  public boolean isChanel() {
    return isChanel;
  }
  
  public void setChanel(final boolean isChanel) {
    this.isChanel = isChanel;
  }
  
  public boolean isHermes() {
    return isHermes;
  }
  
  public void setHermes(final boolean isHermes) {
    this.isHermes = isHermes;
  }
  
  public RichRelevanceProductBean getYmalProducts() {
    return ymalProducts;
  }
  
  public void setYmalProducts(final RichRelevanceProductBean ymalProducts) {
    this.ymalProducts = ymalProducts;
  }
  
  public boolean isServerSideYmal() {
    return serverSideYmal;
  }
  
  public void setServerSideYmal(final boolean serverSideYmal) {
    this.serverSideYmal = serverSideYmal;
  }
  
  private String borderFreeMsgs;
  private boolean onlyPinYin;
  private boolean showCounty;
  private boolean showCpf;
  private boolean disableSubmitOption;
  private boolean zipcodeMandatory;
  private boolean incompleteBorderFreeQuote;
  // INT-1027 muliple shipping options changes starts
  private ArrayList shippingCarrierList;
  private String defaultShippingCarrier;
  
  /**
   * @return the defaultShippingCarrier
   */
  public String getDefaultShippingCarrier() {
    return defaultShippingCarrier;
  }
  
  /**
   * @param defaultShippingCarrier
   *          the defaultShippingCarrier to set
   */
  public void setDefaultShippingCarrier(final String defaultShippingCarrier) {
    this.defaultShippingCarrier = defaultShippingCarrier;
  }
  
  /**
   * @return the shippingCarrierList
   */
  public ArrayList getShippingCarrierList() {
    return shippingCarrierList;
  }
  
  /**
   * @param shippingCarrierList
   *          the shippingCarrierList to set
   */
  public void setShippingCarrierList(final ArrayList shippingCarrierList) {
    this.shippingCarrierList = shippingCarrierList;
  }
  
  // INT-1027 muliple shipping options changes ends
  /**
   * @return the showCounty
   */
  @Override
  public boolean isShowCounty() {
    return showCounty;
  }
  
  /**
   * @param showCounty
   *          the showCounty to set
   */
  @Override
  public void setShowCounty(final boolean showCounty) {
    this.showCounty = showCounty;
  }
  
  /**
   * @return the onlyPinYin
   */
  @Override
  public boolean isOnlyPinYin() {
    return onlyPinYin;
  }
  
  /**
   * @param onlyPinYin
   *          the onlyPinYin to set
   */
  @Override
  public void setOnlyPinYin(final boolean onlyPinYin) {
    this.onlyPinYin = onlyPinYin;
  }
  
  @Override
  public boolean isShowCpf() {
    return showCpf;
  }
  
  @Override
  public void setShowCpf(final boolean showCpf) {
    this.showCpf = showCpf;
  }
  
  public boolean isDisableSubmitOption() {
    return disableSubmitOption;
  }
  
  public void setDisableSubmitOption(final boolean disableSubmitOption) {
    this.disableSubmitOption = disableSubmitOption;
  }
  
  /**
   * @return the incompleteBorderFreeQuote
   */
  public boolean isIncompleteBorderFreeQuote() {
    return incompleteBorderFreeQuote;
  }
  
  /**
   * @param incompleteBorderFreeQuote
   *          the incompleteBorderFreeQuote to set
   */
  public void setIncompleteBorderFreeQuote(final boolean incompleteBorderFreeQuote) {
    this.incompleteBorderFreeQuote = incompleteBorderFreeQuote;
  }
  
  /**
   * @return the zipcodeMandatory
   */
  public boolean isZipcodeMandatory() {
    return zipcodeMandatory;
  }
  
  /**
   * @param zipcodeMandatory
   *          the zipcodeMandatory to set
   */
  public void setZipcodeMandatory(final boolean zipcodeMandatory) {
    this.zipcodeMandatory = zipcodeMandatory;
  }
  
  Map<String, Object> responseMap = new HashMap<String, Object>();
  
  public Map<String, Object> getResponseMap() {
    return responseMap;
  }
  
  public void setResponseMap(final Map<String, Object> responseMap) {
    this.responseMap = responseMap;
  }
  
  public String getBorderFreeMsgs() {
    return borderFreeMsgs;
  }
  
  public void setBorderFreeMsgs(final String borderFreeMsgs) {
    this.borderFreeMsgs = borderFreeMsgs;
  }
  
  public String getBrandOrderId() {
    return orderId;
  }
  
  public void setBrandOrderId(final String orderId) {
    this.orderId = orderId;
  }
  
  // profile
  private Map<String, NMAddress> allShippingAddresses;
  private Map<String, NMCreditCard> creditCardMap;
  
  // shipping
  private boolean isShipFromStore;
  private boolean isShipToMultiple;
  private boolean isShipToStore;
  private boolean isBopsEnabled;
  private boolean isStoreCallEnabled;
  private boolean isOrderContainsStoreCallItems;
  private Map<String, Boolean> cIdsToBopsError;
  private boolean hasMultipleServiceLevels;
  private ServiceLevel[] validServiceLevels;
  private ShippingGroupAux[] shippingGroups;
  private ShippingGroupAux[] deliveryPhoneShippingGroups;
  private boolean hasDeliveryPhoneItems;
  private Map<String, Store> cIdsToStores;
  private boolean isDisplayUseAsShip;
  private boolean displaySameDayDeliverySL;
  private boolean isSameDayDeliveryOrder;
  private String selectedServiceLevel;
  private String currentServiceLevel;
  
  private NMOrderImpl lastOrder;
  private List<ShippingGroupCommerceItemRelationship> commerceItemRelationships;
  private SaveForLaterItemCollection saveForLaterItems;
  private boolean showSeeAllSaveForLaterLink;
  private int numSaveForLaterItemsToDisplay;
  private Map<String, String> errorMsgMap;
  
  private Map<String, AddressVerificationData> addresses;
  private int numberOfAddresses;
  private String formtext;
  private boolean alwaysShowAddressHeading;
  // Country name in shipping and billing address
  private String billingCountry;
  private String shippingCountry;
  // masterpass credit card
  private NMCreditCard masterPassCreditcard;
  
  public String getBillingCountry() {
    return billingCountry;
  }
  
  public void setBillingCountry(final String billingCountry) {
    this.billingCountry = billingCountry;
  }
  
  public String getShippingCountry() {
    return shippingCountry;
  }
  
  public void setShippingCountry(final String shippingCountry) {
    this.shippingCountry = shippingCountry;
  }
  
  private Map<String, Set<EstimatedShipDetailBean>> shipDetails;
  
  // credit card types
  private Map<String, CreditCard> creditCardArrayMap;
  private CreditCard payPalCardType;
  
  // messaging
  private Map<String, Message> messages;
  private Map<String, Message> messageFields;
  private Map<String, Collection<Message>> messageGroups;
  
  private String email;
  private boolean isLoggedIn;
  private boolean emailLoginError;
  private boolean passwordLoginError;
  private boolean lockedAccountError;
  private boolean firstTime;
  
  private Breadcrumb[] breadcrumbs;
  private Breadcrumb currentBreadcrumb;
  
  private boolean canEditCart = true;
  private boolean hasUnresolvedGwps;
  private boolean hasDeclinedGwpSelects;
  
  private PromoBean promoBean;
  
  // printing confirmation
  private String incirclePromoMessage;
  private boolean bgBrandFlag;
  private boolean deliveryPhoneFlag;
  private boolean displayEdit;
  private String confirmText;
  
  private int checkoutState = 0;
  private boolean emptyCart;
  private long itemCount;
  
  private boolean orderHasReplenishmentItems;
  private boolean orderHasActiveReplenishmentItems;
  
  // required for order confirmation marketing tags, these values are only set from order confirmation
  private NMOrderHolder orderHolder;
  private ShoppingCartHandler cart;
  
  // used for Canada order for BG
  private boolean atLeastOneInternationalShipTo;
  
  // Ship To Store
  private boolean shipToStoreEligibleByLocation;
  private boolean shipToStoreEligibleByItems;
  
  // ShopRunner
  private boolean loggedInShopRunner;
  // SHOPRUN-138 Changes
  private boolean supportedByAlipay = false;
  
  /**
   * @return supportedByAlipay
   */
  public boolean isSupportedByAlipay() {
    return supportedByAlipay;
  }
  
  /**
   * @param supportedByAlipay
   */
  public void setSupportedByAlipay(final boolean supportedByAlipay) {
    this.supportedByAlipay = supportedByAlipay;
  }
  
  public boolean isOrderHasActiveReplenishmentItems() {
    return orderHasActiveReplenishmentItems;
  }
  
  public void setOrderHasActiveReplenishmentItems(final boolean orderHasActiveReplenishmentItems) {
    this.orderHasActiveReplenishmentItems = orderHasActiveReplenishmentItems;
  }
  
  private boolean showEstimate;
  
  public long getItemCount() {
    return itemCount;
  }
  
  public void setItemCount(final long l) {
    itemCount = l;
  }
  
  public NMOrderImpl getOrder() {
    return order;
  }
  
  public void setOrder(final NMOrderImpl order) {
    this.order = order;
  }
  
  public double getAmountRemainingOnOrder() {
    return amountRemainingOnOrder;
  }
  
  public void setAmountRemainingOnOrder(final double amountRemainingOnOrder) {
    this.amountRemainingOnOrder = amountRemainingOnOrder;
  }
  
  public Map<String, NMAddress> getAllShippingAddresses() {
    return allShippingAddresses;
  }
  
  public void setAllShippingAddresses(final Map<String, NMAddress> allShippingAddresses) {
    this.allShippingAddresses = allShippingAddresses;
  }
  
  public boolean getIsShipFromStore() {
    return isShipFromStore;
  }
  
  public void setIsShipFromStore(final boolean isShipFromStore) {
    this.isShipFromStore = isShipFromStore;
  }
  
  public boolean getIsShipToMultiple() {
    return isShipToMultiple;
  }
  
  public void setIsShipToMultiple(final boolean isShipToMultiple) {
    this.isShipToMultiple = isShipToMultiple;
  }
  
  public boolean getHasMultipleServiceLevels() {
    return hasMultipleServiceLevels;
  }
  
  public void setHasMultipleServiceLevels(final boolean hasMultipleServiceLevels) {
    this.hasMultipleServiceLevels = hasMultipleServiceLevels;
  }
  
  public ServiceLevel[] getValidServiceLevels() {
    return validServiceLevels;
  }
  
  public void setValidServiceLevels(final ServiceLevel[] validServiceLevels) {
    this.validServiceLevels = validServiceLevels;
  }
  
  public ShippingGroupAux[] getShippingGroups() {
    return shippingGroups;
  }
  
  public void setShippingGroups(final ShippingGroupAux[] shippingGroups) {
    this.shippingGroups = shippingGroups;
  }
  
  public NMOrderImpl getLastOrder() {
    return lastOrder;
  }
  
  public void setLastOrder(final NMOrderImpl lastOrder) {
    this.lastOrder = lastOrder;
  }
  
  public List<ShippingGroupCommerceItemRelationship> getCommerceItemRelationships() {
    return commerceItemRelationships;
  }
  
  public void setCommerceItemRelationships(final List<ShippingGroupCommerceItemRelationship> commerceItemRelationships) {
    this.commerceItemRelationships = commerceItemRelationships;
  }
  
  public SaveForLaterItemCollection getSaveForLaterItems() {
    return saveForLaterItems;
  }
  
  public void setSaveForLaterItems(final SaveForLaterItemCollection saveForLaterItems) {
    this.saveForLaterItems = saveForLaterItems;
  }
  
  public boolean getShowSeeAllSaveForLaterLink() {
    return showSeeAllSaveForLaterLink;
  }
  
  public void setShowSeeAllSaveForLaterLink(final boolean showSeeAllSaveForLaterLink) {
    this.showSeeAllSaveForLaterLink = showSeeAllSaveForLaterLink;
  }
  
  public int getNumSaveForLaterItemsToDisplay() {
    return numSaveForLaterItemsToDisplay;
  }
  
  public void setNumSaveForLaterItemsToDisplay(final int numSaveForLaterItemsToDisplay) {
    this.numSaveForLaterItemsToDisplay = numSaveForLaterItemsToDisplay;
  }
  
  public Map<String, AddressVerificationData> getAddresses() {
    return addresses;
  }
  
  public void setAddresses(final Map<String, AddressVerificationData> addresses) {
    this.addresses = addresses;
  }
  
  public int getNumberOfAddresses() {
    return numberOfAddresses;
  }
  
  public void setNumberOfAddresses(final int numberOfAddresses) {
    this.numberOfAddresses = numberOfAddresses;
  }
  
  public String getFormtext() {
    return formtext;
  }
  
  public void setFormtext(final String formtext) {
    this.formtext = formtext;
  }
  
  public boolean isAlwaysShowAddressHeading() {
    return alwaysShowAddressHeading;
  }
  
  public void setAlwaysShowAddressHeading(final boolean alwaysShowAddressHeading) {
    this.alwaysShowAddressHeading = alwaysShowAddressHeading;
  }
  
  public Map<String, String> getErrorMsgMap() {
    return errorMsgMap;
  }
  
  public void setErrorMsgMap(final Map<String, String> errorMsgMap) {
    this.errorMsgMap = errorMsgMap;
  }
  
  public Map<String, Message> getMessages() {
    return messages;
  }
  
  public void setMessages(final Map<String, Message> messages) {
    this.messages = messages;
  }
  
  public Map<String, Message> getMessageFields() {
    return messageFields;
  }
  
  public void setMessageFields(final Map<String, Message> messageFields) {
    this.messageFields = messageFields;
  }
  
  public Map<String, Collection<Message>> getMessageGroups() {
    return messageGroups;
  }
  
  public void setMessageGroups(final Map<String, Collection<Message>> messageGroups) {
    this.messageGroups = messageGroups;
  }
  
  public void addMessageGroups(final Map<String, Collection<Message>> messageGroups) {
    this.messageGroups.putAll(messageGroups);
  }
  
  public String getEmail() {
    return email;
  }
  
  public void setEmail(final String email) {
    this.email = email;
  }
  
  public boolean getIsLoggedIn() {
    return isLoggedIn;
  }
  
  public void setIsLoggedIn(final boolean isLoggedIn) {
    this.isLoggedIn = isLoggedIn;
  }
  
  public boolean isEmailLoginError() {
    return emailLoginError;
  }
  
  public void setEmailLoginError(final boolean emailLoginError) {
    this.emailLoginError = emailLoginError;
  }
  
  public boolean isPasswordLoginError() {
    return passwordLoginError;
  }
  
  public void setPasswordLoginError(final boolean passwordLoginError) {
    this.passwordLoginError = passwordLoginError;
  }
  
  public boolean isLockedAccountError() {
    return lockedAccountError;
  }
  
  public void setLockedAccountError(final boolean lockedAccountError) {
    this.lockedAccountError = lockedAccountError;
  }
  
  @Override
  public Breadcrumb[] getBreadcrumbs() {
    return breadcrumbs;
  }
  
  @Override
  public void setBreadcrumbs(final Breadcrumb[] breadcrumbs) {
    this.breadcrumbs = breadcrumbs;
  }
  
  @Override
  public Breadcrumb getCurrentBreadcrumb() {
    return currentBreadcrumb;
  }
  
  @Override
  public void setCurrentBreadcrumb(final Breadcrumb currentBreadcrumb) {
    this.currentBreadcrumb = currentBreadcrumb;
  }
  
  public String getIncirclePromoMessage() {
    return incirclePromoMessage;
  }
  
  public void setIncirclePromoMessage(final String incirclePromoMessage) {
    this.incirclePromoMessage = incirclePromoMessage;
  }
  
  public boolean getBgBrandFlag() {
    return bgBrandFlag;
  }
  
  public void setBgBrandFlag(final boolean bgBrandFlag) {
    this.bgBrandFlag = bgBrandFlag;
  }
  
  public boolean getDeliveryPhoneFlag() {
    return deliveryPhoneFlag;
  }
  
  public void setDeliveryPhoneFlag(final boolean deliveryPhoneFlag) {
    this.deliveryPhoneFlag = deliveryPhoneFlag;
  }
  
  public boolean getDisplayEdit() {
    return displayEdit;
  }
  
  public void setDisplayEdit(final boolean displayEdit) {
    this.displayEdit = displayEdit;
  }
  
  public boolean getCanEditCart() {
    return canEditCart;
  }
  
  public void setCanEditCart(final boolean canEditCart) {
    this.canEditCart = canEditCart;
  }
  
  public int getCheckoutState() {
    return checkoutState;
  }
  
  public void setCheckoutState(final int checkoutState) {
    this.checkoutState = checkoutState;
  }
  
  public boolean isEmptyCart() {
    return emptyCart;
  }
  
  public void setEmptyCart(final boolean emptyCart) {
    this.emptyCart = emptyCart;
  }
  
  public void setHasUnresolvedGwps(final boolean hasUnresolvedGwps) {
    this.hasUnresolvedGwps = hasUnresolvedGwps;
  }
  
  /** includes GWP, GWP select, PWP */
  public boolean getHasUnresolvedGwps() {
    return hasUnresolvedGwps;
  }
  
  public void setHasDeclinedGwpSelects(final boolean hasDeclinedGwpSelects) {
    this.hasDeclinedGwpSelects = hasDeclinedGwpSelects;
  }
  
  /** used to optionally display 'add beauty samples' button */
  public boolean getHasDeclinedGwpSelects() {
    return hasDeclinedGwpSelects;
  }
  
  public String getActivePromoCodeText() {
    return activePromoCodeText;
  }
  
  public void setActivePromoCodeText(final String activePromoCodeText) {
    this.activePromoCodeText = activePromoCodeText;
  }
  
  public boolean isFirstTime() {
    return firstTime;
  }
  
  public void setFirstTime(final boolean firstTime) {
    this.firstTime = firstTime;
  }
  
  public NMCreditCard getOrderCreditCard() {
    return orderCreditCard;
  }
  
  public void setOrderCreditCard(final NMCreditCard orderCreditCard) {
    this.orderCreditCard = orderCreditCard;
  }
  
  public CreditCard getOrderCreditCardType() {
    return orderCreditCardType;
  }
  
  public void setOrderCreditCardType(final CreditCard orderCreditCardType) {
    this.orderCreditCardType = orderCreditCardType;
  }
  
  public boolean getHasDeliveryPhoneItems() {
    return hasDeliveryPhoneItems;
  }
  
  public void setHasDeliveryPhoneItems(final boolean hasDeliveryPhoneItems) {
    this.hasDeliveryPhoneItems = hasDeliveryPhoneItems;
  }
  
  public ShippingGroupAux[] getDeliveryPhoneShippingGroups() {
    return deliveryPhoneShippingGroups;
  }
  
  public void setDeliveryPhoneShippingGroups(final ShippingGroupAux[] deliveryPhoneShippingGroups) {
    this.deliveryPhoneShippingGroups = deliveryPhoneShippingGroups;
  }
  
  public Map<String, Set<EstimatedShipDetailBean>> getShipDetails() {
    return shipDetails;
  }
  
  public void setShipDetails(final Map<String, Set<EstimatedShipDetailBean>> shipDetails) {
    this.shipDetails = shipDetails;
  }
  
  public Map<String, NMCreditCard> getCreditCardMap() {
    return creditCardMap;
  }
  
  public void setCreditCardMap(final Map<String, NMCreditCard> creditCardMap) {
    this.creditCardMap = creditCardMap;
  }
  
  public PromoBean getPromoBean() {
    return promoBean;
  }
  
  public void setPromoBean(final PromoBean promoBean) {
    this.promoBean = promoBean;
  }
  
  public boolean isOrderHasReplenishmentItems() {
    return orderHasReplenishmentItems;
  }
  
  public void setOrderHasReplenishmentItems(final boolean orderHasReplenishmentItems) {
    this.orderHasReplenishmentItems = orderHasReplenishmentItems;
  }
  
  public Map<String, CreditCard> getCreditCardArrayMap() {
    return creditCardArrayMap;
  }
  
  public void setCreditCardArrayMap(final Map<String, CreditCard> creditCardArrayMap) {
    this.creditCardArrayMap = creditCardArrayMap;
  }
  
  public boolean isShowEstimate() {
    return showEstimate;
  }
  
  public void setShowEstimate(final boolean showEstimate) {
    this.showEstimate = showEstimate;
  }
  
  public boolean isInvalidOrderAddresses() {
    return invalidOrderAddresses;
  }
  
  public void setInvalidOrderAddresses(final boolean invalidOrderAddresses) {
    this.invalidOrderAddresses = invalidOrderAddresses;
  }
  
  public String getConfirmText() {
    return confirmText;
  }
  
  public void setConfirmText(final String confirmText) {
    this.confirmText = confirmText;
  }
  
  public NMOrderHolder getOrderHolder() {
    return orderHolder;
  }
  
  public void setOrderHolder(final NMOrderHolder orderHolder) {
    this.orderHolder = orderHolder;
  }
  
  public ShoppingCartHandler getCart() {
    return cart;
  }
  
  public void setCart(final ShoppingCartHandler cart) {
    this.cart = cart;
  }
  
  public boolean isShowEstimatedOrderTotal() {
    return showEstimatedOrderTotal;
  }
  
  public void setShowEstimatedOrderTotal(final boolean showEstimatedOrderTotal) {
    this.showEstimatedOrderTotal = showEstimatedOrderTotal;
  }
  
  public boolean isAtLeastOneInternationalShipTo() {
    return atLeastOneInternationalShipTo;
  }
  
  public void setAtLeastOneInternationalShipTo(final boolean atLeastOneInternationalShipTo) {
    this.atLeastOneInternationalShipTo = atLeastOneInternationalShipTo;
  }
  
  public boolean isShipToStoreEligibleByLocation() {
    return shipToStoreEligibleByLocation;
  }
  
  public void setShipToStoreEligibleByLocation(final boolean shipToStoreEligibleByLocation) {
    this.shipToStoreEligibleByLocation = shipToStoreEligibleByLocation;
  }
  
  public boolean isShipToStoreEligibleByItems() {
    return shipToStoreEligibleByItems;
  }
  
  public void setShipToStoreEligibleByItems(final boolean shipToStoreEligibleByItems) {
    this.shipToStoreEligibleByItems = shipToStoreEligibleByItems;
  }
  
  public boolean getIsShipToStore() {
    return isShipToStore;
  }
  
  public void setIsShipToStore(final boolean isShipToStore) {
    this.isShipToStore = isShipToStore;
  }
  
  public boolean isBopsEnabled() {
    return isBopsEnabled;
  }
  
  public void setBopsEnabled(final boolean isBopsEnabled) {
    this.isBopsEnabled = isBopsEnabled;
  }
  
  public CreditCard getPayPalCardType() {
    return payPalCardType;
  }
  
  public void setShowFinCenModal(final boolean value) {
    showFinCenModal = value;
  }
  
  public boolean getShowFinCenModal() {
    return showFinCenModal;
  }
  
  public void setShowAddressVerificationModal(final boolean value) {
    showAddressVerificationModal = value;
  }
  
  public boolean getShowAddressVerificationModal() {
    return showAddressVerificationModal;
  }
  
  public void setAddressVerificationId(final String value) {
    addressVerificationId = value;
  }
  
  public String getAddressVerificationId() {
    return addressVerificationId;
  }
  
  public boolean isVirtualGiftCardOnly() {
    return virtualGiftCardOnly;
  }
  
  public void setVirtualGiftCardOnly(final boolean virtualGiftCardOnly) {
    this.virtualGiftCardOnly = virtualGiftCardOnly;
  }
  
  public boolean isOrderContainsVirtualGiftCard() {
    return orderContainsVirtualGiftCard;
  }
  
  public void setOrderContainsVirtualGiftCard(final boolean orderContainsVirtualGiftCard) {
    this.orderContainsVirtualGiftCard = orderContainsVirtualGiftCard;
  }
  
  public boolean isOrderContainsGiftCard() {
    return orderContainsGiftCard;
  }
  
  public void setOrderContainsGiftCard(final boolean orderContainsGiftCard) {
    this.orderContainsGiftCard = orderContainsGiftCard;
  }
  
  public boolean isOrderContainsWishListItem() {
    return orderContainsWishListItem;
  }
  
  public void setOrderContainsWishListItem(final boolean orderContainsWishListItem) {
    this.orderContainsWishListItem = orderContainsWishListItem;
  }
  
  public void setPayPalCardType(final CreditCard payPalCardType) {
    this.payPalCardType = payPalCardType;
  }
  
  public boolean isDisplayPayPal() {
    return displayPayPal;
  }
  
  public void setDisplayPayPal(final boolean displayPayPal) {
    this.displayPayPal = displayPayPal;
  }
  
  public boolean isLoggedInShopRunner() {
    return loggedInShopRunner;
  }
  
  public void setLoggedInShopRunner(final boolean loggedInShopRunner) {
    this.loggedInShopRunner = loggedInShopRunner;
  }
  
  public boolean isNotSupportedByShopRunner() {
    return (isOrderContainsGiftCard() || isOrderContainsVirtualGiftCard() || isOrderHasReplenishmentItems() || isOrderContainsWishListItem() || getIsShipToMultiple() || (getItemCount() <= 0));
  }
  
  public void setNotSupportedByShopRunner(final boolean notSupportedByShopRunner) {}
  
  // registered checkout changes starts
  private boolean cardSupportedFlag;
  
  /**
   * @return the cardSupportedFlag
   */
  public boolean isCardSupportedFlag() {
    return cardSupportedFlag;
  }
  
  /**
   * @param cardSupportedFlag
   *          the cardSupportedFlag to set
   */
  public void setCardSupportedFlag(final boolean cardSupportedFlag) {
    this.cardSupportedFlag = cardSupportedFlag;
  }
  
  public ShopRunnerResponse getShopRunner() {
    return shopRunner;
  }
  
  public void setShopRunner(final ShopRunnerResponse shopRunner) {
    this.shopRunner = shopRunner;
  }
  
  private boolean selectedCardChinaUnionPay;
  
  /**
   * @return the selectedCardChinaUnionPay
   */
  public boolean isSelectedCardChinaUnionPay() {
    return selectedCardChinaUnionPay;
  }
  
  /**
   * @param selectedCardChinaUnionPay
   *          the selectedCardChinaUnionPay to set
   */
  public void setSelectedCardChinaUnionPay(final boolean selectedCardChinaUnionPay) {
    this.selectedCardChinaUnionPay = selectedCardChinaUnionPay;
  }
  
  private boolean borderFreeConnectivityIssue;
  
  /**
   * @return the borderFreeConnectivityIssue
   */
  public boolean isBorderFreeConnectivityIssue() {
    return borderFreeConnectivityIssue;
  }
  
  /**
   * @param borderFreeConnectivityIssue
   *          the borderFreeConnectivityIssue to set
   */
  public void setBorderFreeConnectivityIssue(final boolean borderFreeConnectivityIssue) {
    this.borderFreeConnectivityIssue = borderFreeConnectivityIssue;
  }
  
  // registered checkout changes ends
  private boolean expressPayPalFlag;
  
  /**
   * @return the expressPayPalFlag
   */
  public boolean isExpressPayPalFlag() {
    return expressPayPalFlag;
  }
  
  /**
   * @param expressPayPalFlag
   *          the expressPayPalFlag to set
   */
  public void setExpressPayPalFlag(final boolean expressPayPalFlag) {
    this.expressPayPalFlag = expressPayPalFlag;
  }
  
  private boolean expressPayPalClcShipToCountry;
  
  /**
   * @return the expressPayPalClcShipToCountry
   */
  public boolean isExpressPayPalClcShipToCountry() {
    return expressPayPalClcShipToCountry;
  }
  
  /**
   * @param expressPayPalClcShipToCountry
   *          the expressPayPalClcShipToCountry to set
   */
  public void setExpressPayPalClcShipToCountry(final boolean expressPayPalClcShipToCountry) {
    this.expressPayPalClcShipToCountry = expressPayPalClcShipToCountry;
  }
  
  private boolean enableCheckoutButton;
  
  /**
   * @return the enableCheckoutButton
   */
  public boolean isEnableCheckoutButton() {
    return enableCheckoutButton;
  }
  
  /**
   * @param enableCheckoutButton
   *          the enableCheckoutButton to set
   */
  public void setEnableCheckoutButton(final boolean enableCheckoutButton) {
    this.enableCheckoutButton = enableCheckoutButton;
  }
  
  public Map<String, Store> getcIdsToStores() {
    return cIdsToStores;
  }
  
  public void setcIdsToStores(final Map<String, Store> cIdsToStores) {
    this.cIdsToStores = cIdsToStores;
  }
  
  public boolean isDisplayUseAsShip() {
    return isDisplayUseAsShip;
  }
  
  public void setDisplayUseAsShip(final boolean isDisplayUseAsShip) {
    this.isDisplayUseAsShip = isDisplayUseAsShip;
  }
  
  public Map<String, Boolean> getcIdsToBopsError() {
    return cIdsToBopsError;
  }
  
  public void setcIdsToBopsError(final Map<String, Boolean> cIdsToBopsError) {
    this.cIdsToBopsError = cIdsToBopsError;
  }
  
  public boolean getDisplaySameDayDeliverySL() {
    return displaySameDayDeliverySL;
  }
  
  public void setDisplaySameDayDeliverySL(final boolean displaySameDayDeliverySL) {
    this.displaySameDayDeliverySL = displaySameDayDeliverySL;
  }
  
  public boolean isSameDayDeliveryOrder() {
    return isSameDayDeliveryOrder;
  }
  
  public void setSameDayDeliveryOrder(final boolean isSameDayDeliveryOrder) {
    this.isSameDayDeliveryOrder = isSameDayDeliveryOrder;
    
  }
  
  /**
   * @return the isStoreCallEnabled
   */
  public boolean isStoreCallEnabled() {
    return isStoreCallEnabled;
  }
  
  /**
   * @param isStoreCallEnabled
   *          the isStoreCallEnabled to set
   */
  public void setStoreCallEnabled(final boolean isStoreCallEnabled) {
    this.isStoreCallEnabled = isStoreCallEnabled;
  }
  
  /**
   * @return the isOrderContainsStoreCallItems
   */
  public boolean isOrderContainsStoreCallItems() {
    return isOrderContainsStoreCallItems;
  }
  
  /**
   * @param isOrderContainsStoreCallItems
   *          the isOrderContainsStoreCallItems to set
   */
  public void setOrderContainsStoreCallItems(final boolean isOrderContainsStoreCallItems) {
    this.isOrderContainsStoreCallItems = isOrderContainsStoreCallItems;
  }
  
  public String getSelectedServiceLevel() {
    return selectedServiceLevel;
  }
  
  public void setSelectedServiceLevel(final String selectedServiceLevel) {
    this.selectedServiceLevel = selectedServiceLevel;
  }
  
  // SmartPost : variable to check whether the smart post test group B is eligible or no
  private boolean smartPostTestGrpBEligible;
  
  /**
   * @return the smartPostTestGrpBEligible
   */
  public boolean isSmartPostTestGrpBEligible() {
    return smartPostTestGrpBEligible;
  }
  
  /**
   * @param smartPostTestGrpBEligible
   *          the smartPostTestGrpBEligible to set
   */
  public void setSmartPostTestGrpBEligible(final boolean smartPostTestGrpBEligible) {
    this.smartPostTestGrpBEligible = smartPostTestGrpBEligible;
  }
  
  private boolean shippingPromotionApplied = false;
  
  /**
   * @return the shippingPromotionApplied
   */
  public boolean isShippingPromotionApplied() {
    return shippingPromotionApplied;
  }
  
  /**
   * @param shippingPromotionApplied
   *          the shippingPromotionApplied to set
   */
  public void setShippingPromotionApplied(final boolean shippingPromotionApplied) {
    this.shippingPromotionApplied = shippingPromotionApplied;
  }
  
  public boolean isShowIncirclePromoMsg() {
    return showIncirclePromoMsg;
  }
  
  public void setShowIncirclePromoMsg(final boolean showIncirclePromoMsg) {
    this.showIncirclePromoMsg = showIncirclePromoMsg;
  }
  
  /**
   * Gets the returns charge banner refreshable content.
   * 
   * @return the returns charge banner refreshable content
   */
  public String getReturnsChargeBannerRefreshableContent() {
    return returnsChargeBannerRefreshableContent;
  }
  
  /**
   * Sets the returns charge banner refreshable content.
   * 
   * @param returnsChargeBannerRefreshableContent
   *          the new returns charge banner refreshable content
   */
  public void setReturnsChargeBannerRefreshableContent(final String returnsChargeBannerRefreshableContent) {
    this.returnsChargeBannerRefreshableContent = returnsChargeBannerRefreshableContent;
  }
  
  /**
   * Checks if is order eligible for free delivery processing.
   * 
   * @return true, if is order eligible for free delivery processing
   */
  public boolean isOrderEligibleForFreeDeliveryProcessing() {
    return orderEligibleForFreeDeliveryProcessing;
  }
  
  /**
   * Sets the order eligible for free delivery processing.
   * 
   * @param orderEligibleForFreeDeliveryProcessing
   *          the new order eligible for free delivery processing
   */
  public void setOrderEligibleForFreeDeliveryProcessing(final boolean orderEligibleForFreeDeliveryProcessing) {
    this.orderEligibleForFreeDeliveryProcessing = orderEligibleForFreeDeliveryProcessing;
  }
  
  /**
   * @return the unavailableMsgForConfiguredItem
   */
  public Map<String, String> getUnavailableMsgForConfiguredItem() {
    return unavailableMsgForConfiguredItem;
  }
  
  /**
   * @param unavailableMsgForConfiguredItem
   *          the unavailableMsgForConfiguredItem to set
   */
  public void setUnavailableMsgForConfiguredItem(final Map<String, String> unavailableMsgForConfiguredItem) {
    this.unavailableMsgForConfiguredItem = unavailableMsgForConfiguredItem;
  }
  
  /**
   * Checks if is service levels fetched already from cmos.
   * 
   * @return true, if is service levels fetched already from cmos
   */
  public boolean isServiceLevelsFetchedFromCmos() {
    return serviceLevelsFetchedFromCmos;
  }
  
  /**
   * Sets the service levels fetched already from cmos.
   * 
   * @param serviceLevelsFetchedAlreadyFromCmos
   *          the new service levels fetched already from cmos
   */
  public void setServiceLevelsFetchedFromCmos(final boolean serviceLevelsFetchedFromCmos) {
    this.serviceLevelsFetchedFromCmos = serviceLevelsFetchedFromCmos;
  }
  
  /**
   * Checks if is change in service levels from cmos.
   * 
   * @return true, if is change in service levels from cmos
   */
  public boolean isChangeInServiceLevelsFromCmos() {
    return changeInServiceLevelsFromCmos;
  }
  
  /**
   * Sets the change in service levels from cmos.
   * 
   * @param changeInServiceLevelsFromCmos
   *          the new change in service levels from cmos
   */
  public void setChangeInServiceLevelsFromCmos(final boolean changeInServiceLevelsFromCmos) {
    this.changeInServiceLevelsFromCmos = changeInServiceLevelsFromCmos;
  }
  
  /**
   * Checks if is estimated delivery date cmos call failed.
   * 
   * @return true, if is estimated delivery date cmos call failed
   */
  public boolean isEstimatedDeliveryDateCmosCallFailed() {
    return estimatedDeliveryDateCmosCallFailed;
  }
  
  /**
   * Sets the estimated delivery date cmos call failed.
   * 
   * @param estimatedDeliveryDateCmosCallFailed
   *          the new estimated delivery date cmos call failed
   */
  public void setEstimatedDeliveryDateCmosCallFailed(final boolean estimatedDeliveryDateCmosCallFailed) {
    this.estimatedDeliveryDateCmosCallFailed = estimatedDeliveryDateCmosCallFailed;
  }
  
  /**
   * @return the parentheticalChargesPresent
   */
  public boolean isParentheticalChargesPresent() {
    return parentheticalChargesPresent;
  }
  
  /**
   * @param parentheticalChargesPresent
   *          the parentheticalChargesPresent to set
   */
  public void setParentheticalChargesPresent(final boolean parentheticalChargesPresent) {
    this.parentheticalChargesPresent = parentheticalChargesPresent;
  }
  
  /**
   * Checks if is cmos service levels contains s l2.
   * 
   * @return true, if is cmos service levels contains s l2
   */
  public boolean isCmosServiceLevelsContainsSL2() {
    return cmosServiceLevelsContainsSL2;
  }
  
  /**
   * Sets the cmos service levels contains s l2.
   * 
   * @param cmosServiceLevelsContainsSL2
   *          the new cmos service levels contains s l2
   */
  public void setCmosServiceLevelsContainsSL2(boolean cmosServiceLevelsContainsSL2) {
    this.cmosServiceLevelsContainsSL2 = cmosServiceLevelsContainsSL2;
  }
  
  /**
   * @return the masterPassCreditcard
   */
  public NMCreditCard getMasterPassCreditcard() {
    return masterPassCreditcard;
  }
  
  /**
   * @param masterPassCreditcard
   *          the masterPassCreditcard to set
   */
  public void setMasterPassCreditcard(NMCreditCard masterPassCreditcard) {
    this.masterPassCreditcard = masterPassCreditcard;
  }
  
  /**
   * @return the estimatedDeliveryDates
   */
  public List<String> getEstimatedDeliveryDates() {
    return estimatedDeliveryDates;
  }
  
  /**
   * @param estimatedDeliveryDates
   *          the estimatedDeliveryDates to set
   */
  public void setEstimatedDeliveryDates(final List<String> estimatedDeliveryDates) {
    this.estimatedDeliveryDates = estimatedDeliveryDates;
  }
  
  /**
   * @return the selectedShippingMethodDescriptions
   */
  public List<String> getSelectedShippingMethodDescriptions() {
    return selectedShippingMethodDescriptions;
  }
  
  /**
   * @param selectedShippingMethodDescriptions
   *          the selectedShippingMethodDescriptions to set
   */
  public void setSelectedShippingMethodDescriptions(List<String> selectedShippingMethodDescriptions) {
    this.selectedShippingMethodDescriptions = selectedShippingMethodDescriptions;
  }
  
  public boolean isPlccRepriceOrderFlag() {
    return plccRepriceOrderFlag;
  }
  
  public void setPlccRepriceOrderFlag(boolean plccRepriceOrderFlag) {
    this.plccRepriceOrderFlag = plccRepriceOrderFlag;
  }
  
  /**
   * Gets the grouped line items by estimated delivery date.
   * 
   * @return the grouped line items by estimated delivery date
   */
  public Map<Date, Map<ShippingGroupAux, List<NMCommerceItem>>> getGroupedLineItemsByEstimatedDeliveryDate() {
    return groupedLineItemsByEstimatedDeliveryDate;
  }
  
  /**
   * Sets the grouped line items by estimated delivery date.
   * 
   * @param groupedLineItemsByEstimatedDeliveryDate
   *          the grouped line items by estimated delivery date
   */
  public void setGroupedLineItemsByEstimatedDeliveryDate(Map<Date, Map<ShippingGroupAux, List<NMCommerceItem>>> groupedLineItemsByEstimatedDeliveryDate) {
    this.groupedLineItemsByEstimatedDeliveryDate = groupedLineItemsByEstimatedDeliveryDate;
  }
  
  /**
   * Gets the estimated delivery date ranges to be displayed for the grouped line items based on the date key.
   * 
   * @return the estimated delivery date ranges
   */
  public Map<Date, String> getEstimatedDeliveryDateRanges() {
    return estimatedDeliveryDateRanges;
  }
  
  /**
   * Sets the estimated delivery date ranges to be displayed for the grouped line items.
   * 
   * @param estimatedDeliveryDateRanges
   *          the estimated delivery day or date ranges
   */
  public void setEstimatedDeliveryDateRanges(Map<Date, String> estimatedDeliveryDateRanges) {
    this.estimatedDeliveryDateRanges = estimatedDeliveryDateRanges;
  }  
  
  /**
   * Gets the estimated delivery date line item messages.
   * 
   * @return the estimated delivery date line item messages
   */
  public Map<String, String> getEstimatedDeliveryDateLineItemMessages() {
    return estimatedDeliveryDateLineItemMessages;
  }
  
  /**
   * Sets the estimated delivery date line item messages.
   * 
   * @param eddLineItemMessages
   *          the edd line item messages
   */
  public void setEstimatedDeliveryDateLineItemMessages(Map<String, String> estimatedDeliveryDateLineItemMessages) {
    this.estimatedDeliveryDateLineItemMessages = estimatedDeliveryDateLineItemMessages;
  }
  
  /**
   * Checks if is estimated delivery date grouping layout.
   * 
   * @return true, if is estimated delivery date grouping layout
   */
  public boolean isEstimatedDeliveryDateGroupingLayout() {
    return estimatedDeliveryDateGroupingLayout;
  }
  
  /**
   * Sets the estimated delivery date grouping layout.
   * 
   * @param estimatedDeliveryDateGroupingLayout
   *          the new estimated delivery date grouping layout
   */
  public void setEstimatedDeliveryDateGroupingLayout(boolean estimatedDeliveryDateGroupingLayout) {
    this.estimatedDeliveryDateGroupingLayout = estimatedDeliveryDateGroupingLayout;
  }
  /**
   * @return the payPalLoggedIn
   */
  public boolean isPayPalLoggedIn() {
  	return payPalLoggedIn;
  }

  /**
   * @param payPalLoggedIn the payPalLoggedIn to set
   */
  public void setPayPalLoggedIn(boolean payPalLoggedIn) {
  	this.payPalLoggedIn = payPalLoggedIn;
  }

/**
 * @return the vmeReturned
 */
public boolean isVmeReturned() {
	return vmeReturned;
}

/**
 * @param vmeReturned the vmeReturned to set
 */
public void setVmeReturned(boolean vmeReturned) {
	this.vmeReturned = vmeReturned;
}

/**
 * @return the highAdornPrice
 */
public List<Double> getHighAdornPrice() {
	return highAdornPrice;
}

/**
 * @param highAdornPrice the highAdornPrice to set
 */
public void setHighAdornPrice(List<Double> highAdornPrice) {
	this.highAdornPrice = highAdornPrice;
}

/**
 * @return the highPrice
 */
public List<Double> getHighPrice() {
	return highPrice;
}

/**
 * @param highPrice the highPrice to set
 */
public void setHighPrice(List<Double> highPrice) {
	this.highPrice = highPrice;
}

/**
 * @return the lowPrice
 */
public List<Double> getLowPrice() {
	return lowPrice;
}

/**
 * @param lowPrice the lowPrice to set
 */
public void setLowPrice(List<Double> lowPrice) {
	this.lowPrice = lowPrice;
}

/**
 * Gets the service level error message. 
 * 
 * @return the serviceLevelErrMsg
 */
public String getServiceLevelErrMsg() {
return serviceLevelErrMsg;
}

/**
 * Sets the service level error message.
 * 
 * @param serviceLevelErrMsg
 *          the serviceLevelErrMsg
 */
public void setServiceLevelErrMsg(String serviceLevelErrMsg) {
this.serviceLevelErrMsg = serviceLevelErrMsg;
}

public String getCurrentServiceLevel() {
  return currentServiceLevel;
}

public void setCurrentServiceLevel(final String currentServiceLevel) {
  this.currentServiceLevel = currentServiceLevel;
}

  /**
   * Checks if is profile contains billing address.
   * 
   * @return true, if is profile contains billing address
   */
  public boolean isProfileContainsBillingAddress() {
    return profileContainsBillingAddress;
  }
  
  /**
   * Sets the profile contains billing address.
   * 
   * @param profileContainsBillingAddress
   *          the new profile contains billing address
   */
  public void setProfileContainsBillingAddress(final boolean profileContainsBillingAddress) {
    this.profileContainsBillingAddress = profileContainsBillingAddress;
  }
  
  /**
   * Checks if is profile contains shipping address.
   * 
   * @return true, if is profile contains shipping address
   */
  public boolean isProfileContainsShippingAddress() {
    return profileContainsShippingAddress;
  }
  
  /**
   * Sets the profile contains shipping address.
   * 
   * @param profileContainsShippingAddress
   *          the new profile contains shipping address
   */
  public void setProfileContainsShippingAddress(final boolean profileContainsShippingAddress) {
    this.profileContainsShippingAddress = profileContainsShippingAddress;
  }
  
  /**
   * Checks if is profile contains payment info.
   * 
   * @return true, if is profile contains payment info
   */
  public boolean isProfileContainsPaymentInfo() {
    return profileContainsPaymentInfo;
  }
  
  /**
   * Sets the profile contains payment info.
   * 
   * @param profileContainsPaymentInfo
   *          the new profile contains payment info
   */
  public void setProfileContainsPaymentInfo(final boolean profileContainsPaymentInfo) {
    this.profileContainsPaymentInfo = profileContainsPaymentInfo;
  }
  
  /**
   * Gets the express checkout enable page url.
   * 
   * @return the expressCheckoutEnablePageUrl
   */
  public String getExpressCheckoutEnablePageUrl() {
    return expressCheckoutEnablePageUrl;
  }
  
  /**
   * Sets the express checkout enable page url.
   * 
   * @param expressCheckoutEnablePageUrl
   *          the expressCheckoutEnablePageUrl to set
   */
  public void setExpressCheckoutEnablePageUrl(final String expressCheckoutEnablePageUrl) {
    this.expressCheckoutEnablePageUrl = expressCheckoutEnablePageUrl;
  }
  
  /**
   * Gets the express checkout billing page url.
   * 
   * @return the expressCheckoutBillingPageUrl
   */
  public String getExpressCheckoutBillingPageUrl() {
    return expressCheckoutBillingPageUrl;
  }
  
  /**
   * Sets the express checkout billing page url.
   * 
   * @param expressCheckoutBillingPageUrl
   *          the expressCheckoutBillingPageUrl to set
   */
  public void setExpressCheckoutBillingPageUrl(final String expressCheckoutBillingPageUrl) {
    this.expressCheckoutBillingPageUrl = expressCheckoutBillingPageUrl;
  }
  
  /**
   * Gets the express checkout shipping page url.
   * 
   * @return the expressCheckoutShippingPageUrl
   */
  public String getExpressCheckoutShippingPageUrl() {
    return expressCheckoutShippingPageUrl;
  }

  /**
   * Sets the express checkout shipping page url.
   * 
   * @param expressCheckoutShippingPageUrl
   *          the expressCheckoutShippingPageUrl to set
   */
  public void setExpressCheckoutShippingPageUrl(final String expressCheckoutShippingPageUrl) {
    this.expressCheckoutShippingPageUrl = expressCheckoutShippingPageUrl;
  }

  /**
   * Gets the express checkout payment page url.
   * 
   * @return the express checkout payment page url
   */
  public String getExpressCheckoutPaymentPageUrl() {
    return expressCheckoutPaymentPageUrl;
  }

  /**
   * Sets the express checkout payment page url.
   * 
   * @param expressCheckoutPaymentPageUrl
   *          the new express checkout payment page url
   */
  public void setExpressCheckoutPaymentPageUrl(final String expressCheckoutPaymentPageUrl) {
    this.expressCheckoutPaymentPageUrl = expressCheckoutPaymentPageUrl;
  }
}
