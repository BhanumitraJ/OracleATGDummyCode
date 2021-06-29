package com.nm.commerce.checkout;

import javax.transaction.TransactionManager;

import atg.commerce.gifts.GiftlistManager;
import atg.commerce.order.OrderHolder;
import atg.commerce.order.purchase.SaveOrderFormHandler;
import atg.nucleus.Nucleus;
import atg.service.lockmanager.ClientLockManager;
import atg.servlet.DynamoHttpServletRequest;
import atg.userprofiling.ProfileFormHandler;

import com.nm.abtest.AbTestMarkerCache;
import com.nm.collections.CountryArray;
import com.nm.collections.CreditCardArray;
import com.nm.collections.ProvinceArray;
import com.nm.collections.ServiceLevelArray;
import com.nm.commerce.GiftCardHolder;
import com.nm.commerce.NMCommerceProfileTools;
import com.nm.commerce.NMOrderHolder;
import com.nm.commerce.NMProfile;
import com.nm.commerce.checkout.beans.ResultBeanContainer;
import com.nm.commerce.pagedef.definition.CheckoutPageDefinition;
import com.nm.commerce.pagedef.definition.ReturnsCreditPageDefinition;
import com.nm.commerce.pricing.ExtraShippingData;
import com.nm.commerce.promotion.NMOrderImpl;
import com.nm.commerce.promotion.action.PromotionEvaluation;
import com.nm.common.AlipayRestrBean;
import com.nm.common.NMBorderFreeGetQuoteResponse;
import com.nm.common.NMBorderFreeOrderStatusResponse;
import com.nm.common.NMBorderFreePayPalResponse;
import com.nm.common.NMBorderFreePlaceOrderResponse;
import com.nm.common.NMGenericSession;
import com.nm.common.SmartPostServiceLevelSessionBean;
import com.nm.estimateddeliverydate.session.EstimatedDeliveryDateSession;
import com.nm.formhandler.PayPalFormHandler;
import com.nm.formhandler.ShoppingCartHandler;
import com.nm.formhandler.VmeInfoFormHandler;
import com.nm.formhandler.checkout.BillingAddressFormHandler;
import com.nm.formhandler.checkout.CartFormHandler;
import com.nm.formhandler.checkout.CheckoutEmailFormHandler;
import com.nm.formhandler.checkout.ManageAddressFormHandler;
import com.nm.formhandler.checkout.OrderSubmitFormHandler;
import com.nm.formhandler.checkout.RegistrationFormHandler;
import com.nm.formhandler.checkout.ShippingAddressFormHandler;
import com.nm.formhandler.checkout.ShoppingBagUpdateFormHandler;
import com.nm.integration.BoxHop;
import com.nm.integration.PayPal;
import com.nm.integration.ShopRunner;
import com.nm.integration.VMe;
import com.nm.integration.VMeInfoVO;
import com.nm.integration.util.paypal.esb.PayPalWebServiceHandler;
import com.nm.international.fiftyone.checkoutapi.util.ESBCheckoutAPIUtil;
import com.nm.masterpass.integration.MasterPassInfoVO;
import com.nm.masterpass.util.MasterPassUtil;
import com.nm.profile.AddressVerificationHelper;
import com.nm.returnscredit.integration.ReturnsCreditSession;
import com.nm.sitemessages.MessageContainer;
import com.nm.tms.core.TMSMessageContainer;
import com.nm.utils.EDWCustData;
import com.nm.utils.KeyIdentifierUtils;
import com.nm.utils.LocalizationUtils;
import com.nm.utils.ProdCountryUtils;
import com.nm.utils.PromoUtils;
import com.nm.utils.PromotionsHelper;
import com.nm.utils.ReplenishmentUtils;
import com.nm.utils.ServiceLevelUtils;
import com.nm.utils.ShipToStoreHelper;
import com.nm.utils.estimatedshipdate.EstimatedShipDateUtil;
import com.nm.utils.fiftyone.FiftyOneUtils;

public class CheckoutComponents {
  
  private CheckoutComponents() {};
  
  public static NMOrderImpl getCurrentOrder(final DynamoHttpServletRequest request) {
    return (NMOrderImpl) getOrderHolder(request).getCurrent();
  }
  
  public static NMOrderHolder getOrderHolder(final DynamoHttpServletRequest request) {
    return (NMOrderHolder) request.resolveName(CART_PATH);
  }
  
  public static ShoppingCartHandler getCartHandler(final DynamoHttpServletRequest request) {
    return (ShoppingCartHandler) request.resolveName(CART_HANDLER_PATH);
  }
  
  public static ShippingAddressFormHandler getShippingAddressFormHandler(final DynamoHttpServletRequest request) {
    return (ShippingAddressFormHandler) request.resolveName(SHIPPING_ADDRESS_FORM_HANDLER_PATH);
  }
  
  public static BillingAddressFormHandler getBillingAddressFormHandler(final DynamoHttpServletRequest request) {
    return (BillingAddressFormHandler) request.resolveName(BILLING_ADDRESS_FORM_HANDLER_PATH);
  }
  
  public static ManageAddressFormHandler getManageAddressFormHandler(final DynamoHttpServletRequest request) {
    return (ManageAddressFormHandler) request.resolveName(MANAGE_ADDRESS_FORM_HANDLER_PATH);
  }
  
  public static CartFormHandler getCartFormHandler(final DynamoHttpServletRequest request) {
    return (CartFormHandler) request.resolveName(CART_FORM_HANDLER_PATH);
  }
  
  public static RegistrationFormHandler getRegistrationFormHandler(final DynamoHttpServletRequest request){
	  return (RegistrationFormHandler) request.resolveName(REGISTRATION_FORM_HANDLER_PATH);
  }
  public static NMBorderFreePlaceOrderResponse getNMBorderFreePlaceOrderResponse(final DynamoHttpServletRequest request) {
    return (NMBorderFreePlaceOrderResponse) request.resolveName(PLACE_ORDER_PATH);
  }
  
  public static NMBorderFreeGetQuoteResponse getNMBorderFreeGetQuoteResponse(final DynamoHttpServletRequest request) {
    return (NMBorderFreeGetQuoteResponse) request.resolveName(GET_QUOTE_PATH);
  }
  
  public static NMBorderFreeOrderStatusResponse getNMBorderFreeOrderStatusResponse(final DynamoHttpServletRequest request) {
    return (NMBorderFreeOrderStatusResponse) request.resolveName(GET_ORDER_STATUS_PATH);
  }
  
  public static NMBorderFreePayPalResponse getNMBorderFreePayPalResponse(final DynamoHttpServletRequest request) {
    return (NMBorderFreePayPalResponse) request.resolveName(INTL_PAYPAL_PATH);
  }
  
  public static ESBCheckoutAPIUtil getESBCheckoutAPIUtil(final DynamoHttpServletRequest request) {
    return (ESBCheckoutAPIUtil) request.resolveName(ESB_CKECKOUTAPI_UTIL_PATH);
  }
  
  public static OrderHolder getATGOrderHolder(final DynamoHttpServletRequest request) {
    return (OrderHolder) request.resolveName(CART_PATH);
  }
  
  public static SaveOrderFormHandler getSaveOrderFormHandler(final DynamoHttpServletRequest request) {
    return (SaveOrderFormHandler) request.resolveName(SAVE_ORDER_FORMHANDLER_PATH);
  }
  
  public static OrderSubmitFormHandler getOrderSubmitFormHandler(final DynamoHttpServletRequest request) {
    return (OrderSubmitFormHandler) request.resolveName(ORDER_SUBMIT_FORM_HANDLER_PATH);
  }
  
  public static NMGenericSession getNMGenericSession(final DynamoHttpServletRequest request) {
    return (NMGenericSession) request.resolveName(NM_GENERIC_SESSION_PATH);
  }
  
  public static ShoppingBagUpdateFormHandler getShoppingBagUpdateFormHandler(final DynamoHttpServletRequest request) {
    return (ShoppingBagUpdateFormHandler) request.resolveName(SHOPPING_BAG_UPDATE_FORM_HANDLER_PATH);
  }
  
  public static ProfileFormHandler getProfileFormHandler(final DynamoHttpServletRequest request) {
    return (ProfileFormHandler) request.resolveName(PROFILE_FORM_HADLER_PATH);
  }
  
  public static CheckoutConfig getConfig() {
    config = (CheckoutConfig) lazyResolve(config, CONFIG_PATH);
    return config;
  }
  
  public static CountryArray getCountryArray() {
    countryArray = (CountryArray) lazyResolve(countryArray, COUNTRY_ARRAY_PATH);
    return countryArray;
  }
  
  public static NMProfile getProfile(final DynamoHttpServletRequest request) {
    return (NMProfile) request.resolveName(PROFILE_PATH);
  }
  
  public static ProdCountryUtils getProdCountryUtils() {
    prodCountryUtils = (ProdCountryUtils) lazyResolve(prodCountryUtils, PROD_COUNTRY_UTILS_PATH);
    return prodCountryUtils;
  }
  
  public static PromoUtils getPromoUtils() {
    promoUtils = (PromoUtils) lazyResolve(promoUtils, PROMO_UTILS_PATH);
    return promoUtils;
  }
  
  public static PromotionEvaluation getPromotionEvaluation() {
    promotionEvaluation = (PromotionEvaluation) lazyResolve(promotionEvaluation, PROMOTION_EVALUATION_PATH);
    return promotionEvaluation;
  }
  
  public static PromotionsHelper getPromotionsHelper() {
    promotionsHelper = (PromotionsHelper) lazyResolve(promotionsHelper, PROMOTIONS_HELPER_PATH);
    return promotionsHelper;
  }
  
  public static ProvinceArray getProvinceArray() {
    provinceArray = (ProvinceArray) lazyResolve(provinceArray, PROVINCE_ARRAY_PATH);
    return provinceArray;
  }
  
  public static ServiceLevelArray getServiceLevelArray() {
    serviceLevelArray = (ServiceLevelArray) lazyResolve(serviceLevelArray, SERVICE_LEVEL_ARRAY_PATH);
    return serviceLevelArray;
  }
  
  public static ExtraShippingData getExtraShippingData() {
    extraShippingData = (ExtraShippingData) lazyResolve(extraShippingData, EXTRA_SHIPPING_DATA);
    return extraShippingData;
  }
  
  public static NMCommerceProfileTools getCommerceProfileTools() {
    commerceProfileTools = (NMCommerceProfileTools) lazyResolve(commerceProfileTools, COMMERCE_PROFILE_TOOLS_PATH);
    return commerceProfileTools;
  }
  
  public static GiftlistManager getGiftlistManager() {
    giftlistManager = (GiftlistManager) lazyResolve(giftlistManager, GIFT_LIST_MANAGER_PATH);
    return giftlistManager;
  }
  
  public static MessageContainer getMessageContainer(final DynamoHttpServletRequest request) {
    return (MessageContainer) request.resolveName(MESSAGE_CONTAINER_PATH);
  }
  
  public static CheckoutPageDefinition getDefaultCheckoutPageDefinition() {
    defaultCheckoutPageDefinition = (CheckoutPageDefinition) lazyResolve(defaultCheckoutPageDefinition, DEFAULT_CHECKOUT_PAGE_DEF_PATH);
    return defaultCheckoutPageDefinition;
  }
  
  public static CheckoutPageDefinition getShippingPageDefinition() {
    shippingPageDefinition = (CheckoutPageDefinition) lazyResolve(shippingPageDefinition, SHIPPING_PAGE_DEF_PATH);
    return shippingPageDefinition;
  }
  
  public static CheckoutPageDefinition getShippingIntlPageDefinition() {
    shippingIntlPageDefinition = (CheckoutPageDefinition) lazyResolve(shippingIntlPageDefinition, SHIPPING_INTL_PAGE_DEF_PATH);
    return shippingIntlPageDefinition;
  }
  
  public static CheckoutPageDefinition getBillingPageDefinition() {
    billingPageDefinition = (CheckoutPageDefinition) lazyResolve(billingPageDefinition, BILLING_PAGE_DEF_PATH);
    return billingPageDefinition;
  }
  
  public static CheckoutPageDefinition getBillingIntlPageDefinition() {
    billingIntlPageDefinition = (CheckoutPageDefinition) lazyResolve(billingIntlPageDefinition, BILLING_INTL_PAGE_DEF_PATH);
    return billingIntlPageDefinition;
  }
  
  public static CheckoutPageDefinition getOrderReviewPageDefinition() {
    orderReviewPageDefinition = (CheckoutPageDefinition) lazyResolve(orderReviewPageDefinition, ORDER_REVIEW_PAGE_DEF_PATH);
    return orderReviewPageDefinition;
  }
  
  public static CheckoutPageDefinition getOrderConfirmationPageDefinition() {
    orderConfirmationPageDefinition = (CheckoutPageDefinition) lazyResolve(orderConfirmationPageDefinition, ORDER_CONFIRMATION_PAGE_DEF_PATH);
    return orderConfirmationPageDefinition;
  }
  
  public static CheckoutPageDefinition getCartPageDefinition() {
    cartPageDefinition = (CheckoutPageDefinition) lazyResolve(cartPageDefinition, CART_PAGE_DEF_PATH);
    return cartPageDefinition;
  }
  
  public static AddressVerificationHelper getAddressVerificationHelper(final DynamoHttpServletRequest request) {
    return (AddressVerificationHelper) request.resolveName(ADDRESS_VERIFICATION_HELPER_PATH);
  }
  
  public static GiftCardHolder getGiftCardHolder(final DynamoHttpServletRequest request) {
    return (GiftCardHolder) request.resolveName(GIFT_CARD_HOLDER_PATH);
  }
  
  public static CreditCardArray getCreditCardArray() {
    creditCardArray = (CreditCardArray) lazyResolve(creditCardArray, CREDIT_CARD_ARRAY_PATH);
    return creditCardArray;
  }
  
  public static CheckoutEmailFormHandler getCheckoutEmailFormHandler(final DynamoHttpServletRequest pRequest) {
    return (CheckoutEmailFormHandler) pRequest.resolveName(CHECKOUT_EMAIL_FORM_HANDLER_PATH);
  }
  
  public static EstimatedShipDateUtil getEstimatedShipDateUtil() {
    estimatedShipDateUtil = (EstimatedShipDateUtil) lazyResolve(estimatedShipDateUtil, ESTIMATED_SHIP_DATE_PATH);
    return estimatedShipDateUtil;
  }
  
  public static PayPal getPayPal() {
    payPal = (PayPal) lazyResolve(payPal, PAYPAL_PATH);
    return payPal;
  }
  
  public static VMe getvMe() {
    vMe = (VMe) lazyResolve(vMe, VME_PATH);
    return vMe;
  }
  
  public static MasterPassUtil getMasterPassUtil() {
    masterPass = (MasterPassUtil) lazyResolve(masterPass, MASTERPASS_UTIL_PATH);
    return masterPass;
  }
  
  public static ShopRunner getShopRunner() {
    shopRunner = (ShopRunner) lazyResolve(shopRunner, SHOPRUNNER_PATH);
    return shopRunner;
  }
  
  public static PayPalWebServiceHandler getPayPalWebServiceHandler() {
    payPalWebServiceHandler = (PayPalWebServiceHandler) lazyResolve(payPalWebServiceHandler, PAYPAL_WEB_SERVICE_HANDLER_PATH);
    return payPalWebServiceHandler;
  }
  
  public static PayPalFormHandler getPayPalFormHandler(final DynamoHttpServletRequest request) {
    return (PayPalFormHandler) request.resolveName(PAYPAL_FORM_HANDLER_PATH);
  }
  
  public static ShipToStoreHelper getShipToStoreHelper() {
    shipToStoreHelper = (ShipToStoreHelper) lazyResolve(shipToStoreHelper, SHIP_TO_STORE_HELPER_PATH);
    return shipToStoreHelper;
  }
  
  public static LocalizationUtils getLocalizationUtils() {
    localizationUtils = (LocalizationUtils) lazyResolve(localizationUtils, LOCALIZATION_UTILS_PATH);
    return localizationUtils;
  }
  
  public static FiftyOneUtils getFiftyOneUtils() {
    fiftyOneUtils = (FiftyOneUtils) lazyResolve(fiftyOneUtils, Fifty_ONE_UTILS_PATH);
    return fiftyOneUtils;
  }
  
  private static Object resolveName(final String component) {
    return Nucleus.getGlobalNucleus().resolveName(component);
  }
  
  private static Object lazyResolve(Object component, final String path) {
    if (component == null) {
      component = resolveName(path);
    }
    
    return component;
  }
  
  public static ResultBeanContainer getResultBeanContainer(final DynamoHttpServletRequest request) {
    return (ResultBeanContainer) request.resolveName(ResultBeanContainer.CONFIG_PATH);
  }
  
  // SmartPost: creating a generic method to access SmartPostServiceLevelBean component
  public static SmartPostServiceLevelSessionBean getSmartPostServiceLevelSessionBean(final DynamoHttpServletRequest request) {
    return (SmartPostServiceLevelSessionBean) request.resolveName(SMART_POST_SERVICE_LEVEL_BEAN_PATH);
  }
  
  // VMe changes
  public static VMeInfoVO getVMeInfoVO(final DynamoHttpServletRequest request) {
    return (VMeInfoVO) request.resolveName(V_ME_INFO_PATH);
  }
  
  public static VmeInfoFormHandler getVmeFormHandler(final DynamoHttpServletRequest request) {
    return (VmeInfoFormHandler) request.resolveName(VME_FORM_HANDLER_PATH);
  }
  
  public static MasterPassInfoVO getMasterPassInfoVO(final DynamoHttpServletRequest request) {
    return (MasterPassInfoVO) request.resolveName(MASTER_PASS_INFO_PATH);
  }
  
  public static KeyIdentifierUtils getKeyIdentifierUtils(final DynamoHttpServletRequest request) {
    return (KeyIdentifierUtils) request.resolveName(KEY_IDENTIFIER_PATH);
  }
  
  public static AlipayRestrBean getAlipayRestrBean(final DynamoHttpServletRequest request) {
    return (AlipayRestrBean) request.resolveName(ALIPAY_BEAN_PATH);
  }
  
  public static ReplenishmentUtils getReplenishmentUtils(final DynamoHttpServletRequest request) {
    return (ReplenishmentUtils) request.resolveName(REPLENISHMENT_UTIL_PATH);
  }
  
  public static final String REPLENISHMENT_UTIL_PATH = "/nm/utils/ReplenishmentUtils";
  
  // REC
  public static ReturnsCreditSession getReturnsCreditSession(final DynamoHttpServletRequest request) {
    return (ReturnsCreditSession) request.resolveName(RETURNS_CREDIT_SESSION_PATH);
  }
  
  /**
   * Gets the estimated delivery date session.
   * 
   * @param request
   *          the request
   * @return the estimated delivery date session
   */
  public static EstimatedDeliveryDateSession getEstimatedDeliveryDateSession(final DynamoHttpServletRequest request) {
    return (EstimatedDeliveryDateSession) request.resolveName(ESTIMATED_DELIVERY_DATE_SESSION_PATH);
  }
  
  /**
   * Gets the service level utils.
   * 
   * @param request
   *          the request
   * @return the service level utils
   */
  public static ServiceLevelUtils getServiceLevelUtils(final DynamoHttpServletRequest request) {
    return (ServiceLevelUtils) request.resolveName(SERVICE_LEVEL_UTILS_PATH);
  }
  
  public static ReturnsCreditPageDefinition getReturnsSelectItemPageDefinition() {
    returnsSelectItemPageDefinition = (ReturnsCreditPageDefinition) lazyResolve(returnsSelectItemPageDefinition, RETURNS_SELECT_ITEM_PAGE_DEF_PATH);
    return returnsSelectItemPageDefinition;
  }
  
  public static void setReturnsSelectItemPageDefinition(final ReturnsCreditPageDefinition returnsSelectItemPageDefinition) {
    CheckoutComponents.returnsSelectItemPageDefinition = returnsSelectItemPageDefinition;
  }
  
  public static ReturnsCreditPageDefinition getOrderLookupPageDefinition() {
    orderLookupPageDefinition = (ReturnsCreditPageDefinition) lazyResolve(orderLookupPageDefinition, ORDER_LOOKUP_PAGE_DEF_PATH);
    return orderLookupPageDefinition;
  }
  
  public static void setOrderLookupPageDefinition(final ReturnsCreditPageDefinition orderLookupPageDefinition) {
    CheckoutComponents.orderLookupPageDefinition = orderLookupPageDefinition;
  }
  
  public static ReturnsCreditPageDefinition getReturnsReviewPageDefinition() {
    returnsReviewPageDefinition = (ReturnsCreditPageDefinition) lazyResolve(returnsReviewPageDefinition, RETURNS_REVIEW_PAGE_DEF_PATH);
    return returnsReviewPageDefinition;
  }
  
  public static void setReturnsReviewPageDefinition(final ReturnsCreditPageDefinition returnsReviewPageDefinition) {
    CheckoutComponents.returnsReviewPageDefinition = returnsReviewPageDefinition;
  }
  
  public static ReturnsCreditPageDefinition getReturnsConfirmationPageDefinition() {
    returnsConfirmationPageDefinition = (ReturnsCreditPageDefinition) lazyResolve(returnsConfirmationPageDefinition, RETURNS_CONFIRMATION_PAGE_DEF_PATH);
    return returnsConfirmationPageDefinition;
  }
  
  public static void setReturnsConfirmationPageDefinition(final ReturnsCreditPageDefinition returnsConfirmationPageDefinition) {
    CheckoutComponents.returnsConfirmationPageDefinition = returnsConfirmationPageDefinition;
  }
  
  // BoxHop changes
  public static BoxHop getBoxHop(final DynamoHttpServletRequest request) {
    return (BoxHop) request.resolveName(BOXHOP_PATH);
  }
  
  public static EDWCustData getEdwCustData(final DynamoHttpServletRequest request) {
    return (EDWCustData) request.resolveName(EDW_CUST_DATA_PATH);
  }
  
  /**
   * Gets the ab test marker cache.
   * 
   * @param request
   *          the request
   * @return the ab test marker cache
   */
  public static AbTestMarkerCache getAbTestMarkerCache(final DynamoHttpServletRequest request) {
    return (AbTestMarkerCache) request.resolveName(AB_TEST_MARKER_CACHE_PATH);
  }
  
  /**
   * Gets the TMSMessageContainer
   * @param request
   * @return TMSMessageContainer
   */
  public static TMSMessageContainer getTMSMessageContainer(final DynamoHttpServletRequest request) {
    return (TMSMessageContainer)request.resolveName(TMS_MESSGE_CONTAINER);
  }
  
  public static ClientLockManager getClientLockManager(final DynamoHttpServletRequest request) {
	 return (ClientLockManager) request.resolveName(LOCK_MANAGER);
  }

  public static TransactionManager getTransactionManager(final DynamoHttpServletRequest request) {
	 return (TransactionManager) request.resolveName(TRANSACTION_MANAGER);
  }

  public static final String V_ME_INFO_PATH = "/nm/integration/VMeInfoVO";
  public static final String MASTER_PASS_INFO_PATH = "/nm/masterpass/integration/MasterPassInfoVO";
  // SmartPost : creating a constant for the path of SmartPostServiceLevelBean
  public static final String SMART_POST_SERVICE_LEVEL_BEAN_PATH = "/nm/common/SmartPostServiceLevelSessionBean";
  public static final String ADDRESS_UTIL_PATH = "/nm/commerce/checkout/AddressUtil";
  public static final String SHIPPING_ADDRESS_FORM_HANDLER_PATH = "/nm/formhandler/checkout/ShippingAddressFormHandler";
  public static final String BILLING_ADDRESS_FORM_HANDLER_PATH = "/nm/formhandler/checkout/BillingAddressFormHandler";
  public static final String MANAGE_ADDRESS_FORM_HANDLER_PATH = "/nm/formhandler/checkout/ManageAddressFormHandler";
  public static final String CART_FORM_HANDLER_PATH = "/nm/formhandler/checkout/CartFormHandler";
  public static final String REGISTRATION_FORM_HANDLER_PATH = "/nm/formhandler/checkout/RegistrationFormHandler";
  public static final String SHOPPING_BAG_UPDATE_FORM_HANDLER_PATH = "/nm/formhandler/checkout/ShoppingBagUpdateFormHandler";
  public static final String PROFILE_FORM_HADLER_PATH = "/atg/userprofiling/ProfileFormHandler";
  public static final String CART_PATH = "/atg/commerce/ShoppingCart";
  public static final String CART_HANDLER_PATH = "/atg/commerce/order/ShoppingCartModifier";
  public static final String CONFIG_PATH = "/nm/commerce/checkout/CheckoutConfig";
  public static final String COUNTRY_ARRAY_PATH = "/nm/collections/CountryArray";
  public static final String PROD_COUNTRY_UTILS_PATH = "/nm/utils/ProdCountryUtils";
  public static final String PROFILE_PATH = "/atg/userprofiling/Profile";
  public static final String PROFILE_TOOLS_PATH = "/atg/userprofiling/ProfileTools";
  public static final String PROMO_UTILS_PATH = "/nm/utils/PromoUtils";
  public static final String PROMOTION_EVALUATION_PATH = "/nm/commerce/promotion/action/PromotionEvaluation";
  public static final String PROMOTIONS_HELPER_PATH = "/nm/utils/PromotionsHelper";
  public static final String PROVINCE_ARRAY_PATH = "/nm/collections/ProvinceArray";
  public static final String SERVICE_LEVEL_ARRAY_PATH = "/nm/collections/ServiceLevelArray";
  public static final String EXTRA_SHIPPING_DATA = "/nm/commerce/pricing/ExtraShippingData";
  public static final String COMMERCE_PROFILE_TOOLS_PATH = "/atg/userprofiling/ProfileTools";
  public static final String GIFT_LIST_MANAGER_PATH = "/atg/commerce/gifts/GiftlistManager";
  public static final String MESSAGE_CONTAINER_PATH = "/nm/sitemessages/MessageContainer";
  public static final String DEFAULT_CHECKOUT_PAGE_DEF_PATH = "/nm/commerce/pagedef/checkout/DefaultCheckout";
  public static final String SHIPPING_PAGE_DEF_PATH = "/nm/commerce/pagedef/checkout/Shipping";
  public static final String BILLING_PAGE_DEF_PATH = "/nm/commerce/pagedef/checkout/Billing";
  public static final String SHIPPING_INTL_PAGE_DEF_PATH = "/nm/commerce/pagedef/checkout/ShippingIntl";
  public static final String BILLING_INTL_PAGE_DEF_PATH = "/nm/commerce/pagedef/checkout/BillingIntl";
  public static final String ORDER_REVIEW_PAGE_DEF_PATH = "/nm/commerce/pagedef/checkout/OrderReview";
  public static final String ORDER_CONFIRMATION_PAGE_DEF_PATH = "/nm/commerce/pagedef/checkout/OrderComplete";
  public static final String CART_PAGE_DEF_PATH = "/nm/commerce/pagedef/checkout/Cart";
  public static final String ADDRESS_VERIFICATION_HELPER_PATH = "/nm/profile/AddressVerificationHelper";
  public static final String GIFT_CARD_HOLDER_PATH = "/nm/commerce/GiftCardHolder";
  public static final String CREDIT_CARD_ARRAY_PATH = "/nm/collections/CreditCardArray";
  public static final String CHECKOUT_EMAIL_FORM_HANDLER_PATH = "/nm/formhandler/checkout/CheckoutEmailFormHandler";
  public static final String ESTIMATED_SHIP_DATE_PATH = "/nm/utils/estimatedshipdate/EstimatedShipDateUtil";
  public static final String PAYPAL_PATH = "/nm/integration/PayPal";
  public static final String PAYPAL_WEB_SERVICE_HANDLER_PATH = "/nm/integration/util/paypal/esb/PayPalWebServiceHandler";
  public static final String PAYPAL_FORM_HANDLER_PATH = "/nm/formhandler/PayPalFormHandler";
  public static final String SHOPRUNNER_PATH = "/nm/integration/ShopRunner";
  public static final String SHIP_TO_STORE_HELPER_PATH = "/nm/utils/ShipToStoreHelper";
  public static final String GET_QUOTE_PATH = "/nm/common/NMBorderFreeGetQuoteResponse";
  public static final String PLACE_ORDER_PATH = "/nm/common/NMBorderFreePlaceOrderResponse";
  public static final String GET_ORDER_STATUS_PATH = "/nm/common/NMBorderFreeOrderStatusResponse";
  public static final String INTL_PAYPAL_PATH = "/nm/common/NMBorderFreePayPalResponse";
  public static final String SAVE_ORDER_FORMHANDLER_PATH = "/atg/commerce/order/purchase/SaveOrderFormHandler";
  public static final String LOCALIZATION_UTILS_PATH = "/nm/utils/LocalizationUtils";
  public static final String ORDER_SUBMIT_FORM_HANDLER_PATH = "/nm/formhandler/checkout/OrderSubmitFormHandler";
  public static final String NM_GENERIC_SESSION_PATH = "/nm/common/NMGenericSession";
  public static final String Fifty_ONE_UTILS_PATH = "/nm/utils/fiftyone/FiftyOneUtils";
  public static final String ESB_CKECKOUTAPI_UTIL_PATH = "/nm/international/fiftyone/checkoutapi/util/ESBCheckoutAPIUtil";
  public static final String VME_PATH = "/nm/integration/VMe";
  public static final String MASTERPASS_UTIL_PATH = "/nm/masterpass/util/MasterPassUtil";
  public static final String VME_FORM_HANDLER_PATH = "/nm/formhandler/VmeInfoFormHandler";
  public static final String KEY_IDENTIFIER_PATH = "/nm/utils/KeyIdentifierUtils";
  public static final String ALIPAY_BEAN_PATH = "/nm/common/AlipayRestrBean";
  public static final String RETURNS_CREDIT_SESSION_PATH = "/nm/returnscredit/integration/ReturnsCreditSession";
  public static final String BOXHOP_PATH = "/nm/integration/BoxHop";
  
  /** The Constant ESTIMATED_DELIVERY_DATE_SESSION_PATH. */
  public static final String ESTIMATED_DELIVERY_DATE_SESSION_PATH = "/nm/estimateddeliverydate/session/EstimatedDeliveryDateSession";
  
  /** The Constant SERVICE_LEVEL_UTILS_PATH. */
  private static final String SERVICE_LEVEL_UTILS_PATH = "/nm/utils/ServiceLevelUtils";
  
  // REC Flow Breadcrumb
  /** The Constant ORDER_LOOKUP_PAGE_DEF_PATH. */
  public static final String ORDER_LOOKUP_PAGE_DEF_PATH = "/nm/commerce/pagedef/returnscredit/ReturnsCreditOrderLookupDefinition";
  /** The Constant RETURNS_SELECT_ITEM_PAGE_DEF_PATH. */
  public static final String RETURNS_SELECT_ITEM_PAGE_DEF_PATH = "/nm/commerce/pagedef/returnscredit/ReturnsCreditSelectItemsDefinition";
  /** The Constant RETURNS_REVIEW_PAGE_DEF_PATH. */
  public static final String RETURNS_REVIEW_PAGE_DEF_PATH = "/nm/commerce/pagedef/returnscredit/ReturnsCreditReviewItemsDefinition";
  /** The Constant RETURNS_CONFIRMATION_PAGE_DEF_PATH. */
  public static final String RETURNS_CONFIRMATION_PAGE_DEF_PATH = "/nm/commerce/pagedef/returnscredit/ReturnsCreditConfirmationDefinition";
  
  public static final String EDW_CUST_DATA_PATH = "/nm/utils/EDWCustData";
  
  /** The Constant AB_TEST_MARKER_CACHE_PATH. */
  private static final String AB_TEST_MARKER_CACHE_PATH = "/nm/abtest/AbTestMarkerCache";
  
  /**The Constant TMS_MESSGE_CONTAINER */
  private static final String TMS_MESSGE_CONTAINER = "/nm/tms/TMSMessageContainer";
  
  private static CheckoutConfig config;
  private static CountryArray countryArray;
  private static ProdCountryUtils prodCountryUtils;
  private static PromoUtils promoUtils;
  private static PromotionEvaluation promotionEvaluation;
  private static PromotionsHelper promotionsHelper;
  private static ProvinceArray provinceArray;
  private static ServiceLevelArray serviceLevelArray;
  private static ExtraShippingData extraShippingData;
  private static NMCommerceProfileTools commerceProfileTools;
  private static GiftlistManager giftlistManager;
  private static CheckoutPageDefinition defaultCheckoutPageDefinition;
  private static CheckoutPageDefinition shippingPageDefinition;
  private static CheckoutPageDefinition shippingIntlPageDefinition;
  private static CheckoutPageDefinition billingPageDefinition;
  private static CheckoutPageDefinition billingIntlPageDefinition;
  private static CheckoutPageDefinition orderReviewPageDefinition;
  private static CheckoutPageDefinition orderConfirmationPageDefinition;
  private static CheckoutPageDefinition cartPageDefinition;
  private static CreditCardArray creditCardArray;
  private static EstimatedShipDateUtil estimatedShipDateUtil;
  private static PayPal payPal;
  private static ShopRunner shopRunner;
  private static PayPalWebServiceHandler payPalWebServiceHandler;
  private static ShipToStoreHelper shipToStoreHelper;
  private static LocalizationUtils localizationUtils;
  private static FiftyOneUtils fiftyOneUtils;
  private static VMe vMe;
  private static MasterPassUtil masterPass;
  
  // REC Flow -- Breadcrumb
  /** The order lookup page definition. */
  private static ReturnsCreditPageDefinition orderLookupPageDefinition;
  /** The returns select item page definition. */
  private static ReturnsCreditPageDefinition returnsSelectItemPageDefinition;
  /** The returns review page definition. */
  private static ReturnsCreditPageDefinition returnsReviewPageDefinition;
  /** The returns confirmation page definition. */
  private static ReturnsCreditPageDefinition returnsConfirmationPageDefinition;
  
  public static final String LOCK_MANAGER="/atg/commerce/order/LocalLockManager";
  
  public static final String TRANSACTION_MANAGER="/atg/dynamo/transaction/TransactionManager";
  
}
