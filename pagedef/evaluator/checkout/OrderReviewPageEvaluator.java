package com.nm.commerce.pagedef.evaluator.checkout;

import static com.nm.common.INMGenericConstants.ACTIVE;
import static com.nm.estimateddeliverydate.EstimatedDeliveryDateConstants.EDD_NEW_LAYOUT_AB_TEST_GROUP;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.PageContext;

import org.apache.commons.lang.StringUtils;

import atg.commerce.order.OrderManager;
import atg.commerce.order.ShippingGroupCommerceItemRelationship;
import atg.naming.NameContext;
import atg.nucleus.Nucleus;
import atg.repository.RepositoryItem;
import atg.servlet.DynamoHttpServletRequest;
import atg.servlet.ServletUtil;

import com.nm.abtest.AbTestHelper;
import com.nm.collections.CreditCard;
import com.nm.collections.CreditCardArray;
import com.nm.collections.ServiceLevel;
import com.nm.commerce.NMCommerceItem;
import com.nm.commerce.NMCommerceProfileTools;
import com.nm.commerce.NMCreditCard;
import com.nm.commerce.NMOrderHolder;
import com.nm.commerce.NMProfile;
import com.nm.commerce.TempPayment;
import com.nm.commerce.checkout.CheckoutAPI;
import com.nm.commerce.checkout.CheckoutComponents;
import com.nm.commerce.checkout.SnapshotUtil;
import com.nm.commerce.checkout.beans.ContactInfo;
import com.nm.commerce.checkout.beans.PaymentInfo;
import com.nm.commerce.checkout.beans.SameDayDeliveryOrderInfo;
import com.nm.commerce.order.NMOrderManager;
import com.nm.commerce.pagedef.definition.CheckoutPageDefinition;
import com.nm.commerce.pagedef.definition.PageDefinition;
import com.nm.commerce.pagedef.model.CheckoutPageModel;
import com.nm.commerce.pagedef.model.UIElementConstants;
import com.nm.commerce.pagedef.model.bean.ShippingGroupAux;
import com.nm.commerce.promotion.NMOrderImpl;
import com.nm.common.NMBorderFreeGetQuoteResponse;
import com.nm.common.NMBorderFreePayPalResponse;
import com.nm.common.ServiceLevelConstants;
import com.nm.common.SmartPostServiceLevelSessionBean;
import com.nm.components.CommonComponentHelper;
import com.nm.edo.EmployeeDiscountsConfig;
import com.nm.edo.constants.EmployeeDiscountConstants;
import com.nm.estimateddeliverydate.session.EstimatedDeliveryDateSession;
import com.nm.estimateddeliverydate.vo.EstimatedDeliveryDateCmosShippingVO;
import com.nm.formhandler.ShoppingCartHandler;
import com.nm.formhandler.checkout.OrderSubmitFormHandler;
import com.nm.international.fiftyone.checkoutapi.BorderFreeConstants;
import com.nm.international.fiftyone.checkoutapi.ESBErrorMessages;
import com.nm.international.fiftyone.checkoutapi.vo.AvailablePaymentMethodsResposeVO;
import com.nm.profile.ProfileProperties;
import com.nm.sitemessages.Message;
import com.nm.sitemessages.MessageContainer;
import com.nm.sitemessages.MessageDefs;
import com.nm.tms.core.TMSMessageContainer;
import com.nm.tms.utils.TMSDataDictionaryUtils;
import com.nm.utils.CookieUtil;
import com.nm.utils.LocalizationUtils;
import com.nm.utils.NMSpecUtils;
import com.nm.utils.NmoUtils;
import com.nm.utils.SameDayDeliveryUtil;
import com.nm.utils.StringUtilities;
import com.nm.utils.crypto.CryptoJumio;
import com.nm.utils.fiftyone.FiftyOneUtils;

public class OrderReviewPageEvaluator extends EstimatedShipDatePageEvaluator {
  
  private static final FiftyOneUtils fiftyOneUtils = (FiftyOneUtils) Nucleus.getGlobalNucleus().resolveName("nm/utils/fiftyone/FiftyOneUtils");
  private static final ESBErrorMessages esbErrorMessages = (ESBErrorMessages) Nucleus.getGlobalNucleus().resolveName("/nm/international/fiftyone/checkoutapi/ESBErrorMessages");
  public static final String ORDERREVIEWREGISTRATION_ABTESTGROUP = "orderReviewRegistrationABTest";
  public static final String PLCC_ABTESTGROUP_INACTIVE = "inactive";
  public static final String IS_REGISTEREDEMAIL = "isRegisteredEmail";
  public static final String JUMIO_ABTESTGROUP = "jumioAbTest";
  public static final String JUMIO_KEY = "jumioKey";
  public static final String JUMIO_KEY_TYPE = "checkout";
  
  @Override
  public boolean evaluate(final PageDefinition pageDefinition, final PageContext pageContext) throws Exception {
    if (!super.evaluate(pageDefinition, pageContext)) {
      return false;
    }
    final NMBorderFreePayPalResponse nmBorderFreePayPalResponse = CheckoutComponents.getNMBorderFreePayPalResponse(getRequest());
    final CheckoutPageModel pageModel = (CheckoutPageModel) getPageModel();
    final CheckoutPageDefinition checkoutPageDefinition = (CheckoutPageDefinition) pageDefinition;
    final NMProfile profile = CheckoutComponents.getProfile(getRequest());
    final ShoppingCartHandler cart = CheckoutComponents.getCartHandler(getRequest());
    final String changePPX = getRequest().getParameter("changePPX");
    final NMOrderImpl order = cart.getNMOrder();
    pageModel.setOrderEligibleForDiscount(order.isOrderEligibleForDiscount());
    pageModel.setVirtualGiftCardOnly(!CheckoutAPI.containsVirtualGiftCardsOnly(order));
    final NMOrderManager orderMgr = (NMOrderManager) cart.getOrderManager();
    final NMOrderHolder orderHolder = CheckoutComponents.getOrderHolder(getRequest());
    final OrderSubmitFormHandler handler = CheckoutComponents.getOrderSubmitFormHandler(getRequest());
    final CreditCardArray creditCardArray = CheckoutComponents.getCreditCardArray();
    final MessageContainer messages = CheckoutComponents.getMessageContainer(getRequest());
    final TMSDataDictionaryUtils dataDictionaryUtils = CommonComponentHelper.getDataDictionaryUtils();
    // get the TMSMessageContainer from request
    final TMSMessageContainer tmsMessageContainer = CheckoutComponents.getTMSMessageContainer(getRequest());
    final ArrayList<String> tmsMessageList = new ArrayList<String>();
    // add the container messages in the tmsMessageList
    dataDictionaryUtils.buildMsgList(messages.getAll(), tmsMessageList);
    dataDictionaryUtils.setFormErrorsToTmsMessageList(handler, tmsMessageList);
    final boolean isInternational = CheckoutAPI.isInternationalSession(profile);
    final String countryPref = profile.getCountryPreference();
    CheckoutAPI.mergeSplitCommerceItems(order, orderMgr, false, false, false);
    final String localizedPriceError = ServletUtil.getCurrentRequest().getParameter("payPalLocalizedPriceError");
    
    if (StringUtilities.isNotEmpty(localizedPriceError) && localizedPriceError.equals("true")) {
      addMessage(messages, null, "Your ship-to country has changed. As a result, your order total has been updated.", tmsMessageList, BorderFreeConstants.GROUP_TOP, Boolean.TRUE, null);
      pageModel.getMessageGroups().putAll(messages.setFieldIds(BorderFreeConstants.GROUP_TOP).groupByPrefix("group"));
    }
    
    updateSameDayDeliveryProperties(pageModel, cart, order, messages, isInternational);
    
    // SmartPost : setting a pageModel attribute to true if smartPostGrpB is enabled and order is not shop runner and shipping promotion is not applied
    final String abTestGroup = CheckoutComponents.getServiceLevelArray().getServiceLevelABTestGroup();
    if (CommonComponentHelper.getLogger().isLoggingDebug()) {
      CommonComponentHelper.getLogger().logDebug(
              "[Page : Order review]SmartPost ABTest Status : " + abTestGroup + " SmartPost/NonSmartPost group : " + CheckoutComponents.getServiceLevelArray().getServiceLevelGroupBasedOnAbTest());
    }
    
    final String shipToCountry = CheckoutComponents.getServiceLevelArray().getShipToCountry(order);
    if (CommonComponentHelper.getSystemSpecs().isSmartPostEnabled() && abTestGroup.equalsIgnoreCase(UIElementConstants.AB_TEST_SERVICE_LEVEL_GROUPS)) {
      boolean smartPostGroupBEnabled = false;
      boolean defaultServiceLevelAvailable = false;
      final SmartPostServiceLevelSessionBean smartPostServiceLevelSessionBean = CheckoutComponents.getSmartPostServiceLevelSessionBean(getRequest());
      if (pageModel.getElementDisplaySwitchMap().containsKey(UIElementConstants.SERVICE_LEVEL_ABTEST_GROUP_2)) {
        smartPostGroupBEnabled = pageModel.getElementDisplaySwitchMap().get(UIElementConstants.SERVICE_LEVEL_ABTEST_GROUP_2);
      }
      for (final ServiceLevel serviceLevel : pageModel.getValidServiceLevels()) {
        if (smartPostServiceLevelSessionBean.getSmartPostSelectedServiceLevel().equalsIgnoreCase(serviceLevel.getCode())) {
          defaultServiceLevelAvailable = true;
        }
      }
      if (order.hasShippingPromotion() || order.hasShippingUpgradePromotion()) {
        pageModel.setShippingPromotionApplied(true);
      }
      if (smartPostGroupBEnabled && !(order.isShopRunnerOrder() && loggedInShopRunner(getRequest()))
              && (!defaultServiceLevelAvailable || smartPostServiceLevelSessionBean.getSmartPostSelectedServiceLevel().isEmpty()) && StringUtilities.isNotEmpty(shipToCountry)
              && shipToCountry.equalsIgnoreCase("US")) {
        pageModel.setSmartPostTestGrpBEligible(true);
      }
      
      if (CommonComponentHelper.getLogger().isLoggingDebug()) {
        CommonComponentHelper.getLogger().logDebug(
                "SmartPost Test Group B eligible : " + pageModel.isSmartPostTestGrpBEligible() + " smartPostGroupBEnabled value :" + smartPostGroupBEnabled + " defaultServiceLevelAvailable : "
                        + defaultServiceLevelAvailable + " SmartPostServiceLevelSessionBean :" + smartPostServiceLevelSessionBean.getSmartPostSelectedServiceLevel()
                        + "Order has Shipping promotion : " + order.hasShippingPromotion() + "Order has shipping upgraded promotion : " + order.hasShippingUpgradePromotion());
      }
      // SmartPost-Omniture changes : Set the error message from name context instance to a request attribute
      final NameContext nameContext = getRequest().getRequestScope();
      if ((nameContext != null) && (null != nameContext.getElement(ServiceLevelConstants.SPOST_SHIPPING_OPTION_ERROR_MSG))) {
        final String smartPostShippingErrorMsg = nameContext.getElement(ServiceLevelConstants.SPOST_SHIPPING_OPTION_ERROR_MSG).toString();
        if (StringUtilities.isNotEmpty(smartPostShippingErrorMsg)) {
          getRequest().setAttribute(ServiceLevelConstants.SPOST_SHIPPING_OPTION_ERROR_MSG, smartPostShippingErrorMsg);
        }
      }
    }
    // SmartPost changes ends
    
    // Abtesting for Jumio Scan Button on order review page
    String jumioAbTest = AbTestHelper.getAbTestValue(getRequest(), JUMIO_ABTESTGROUP);
    if (jumioAbTest == null) {
    	jumioAbTest = "inactive";
    }
    getRequest().setParameter(JUMIO_ABTESTGROUP, jumioAbTest);
    // On~Off Switch for Jumio using NM_GENERIC_SYSTEM_SPECS table to verify if the attribute is true or false.
	// This will be false on all pages except the Order Review page
    if(isJumioFeatureEnabled()){
    	pageModel.setJumioEnabled(true);//Review htmlHead.jsp
    	CryptoJumio crytoJumio = new CryptoJumio();
    	crytoJumio.generateJumioToken(profile.getWebId());
    	String jumioToken = crytoJumio.getJumioAuthorizedToken();
    	pageModel.setJumioToken(jumioToken);
    }
    
    final String payerId = getRequest().getParameter("PayerID");
    final EmployeeDiscountsConfig employeeDiscountsConfig = CommonComponentHelper.getEmployeeDiscountsConfig();
    /*
     * Check if we need to update shipping adress to a store address. 1. Pickup Number is not empty 2. BOPS enabled or (Store Call Available And Associate LoggedIn)
     */
    final boolean shouldUpdateShippingAddress =
            CheckoutAPI.shouldUpdateShippingAddressToStoreAdress(CommonComponentHelper.getBrandSpecs().isBOPSEnabled(), profile.isAssociateLoggedIn(),
                    employeeDiscountsConfig.isEmployeeDiscountsEnabled(), order);
    if (shouldUpdateShippingAddress) {
      CheckoutAPI.changeShippingAddressToStoreAddress(cart, order, profile);
      if (CheckoutAPI.hasMustShipWithItemGWP(order.getNmCommerceItems())) {
        // if in this case the GWP items are in a different shipping group, merge them back together
        CheckoutAPI.evaluateGwpMustShipWith(order, orderMgr);
        // now the shipping groups and commerce items are no longer BOPS, fix this.
        CheckoutAPI.correctGwpMustShipWithAndBOPSIssue(cart, order);
      }
      // new shipping groups could have been created, check multi ship again
      final boolean multiShip = CheckoutAPI.getIsShipToMultiple(order);
      pageModel.setIsShipToMultiple(multiShip);
    }
    
    if (order != null) {
      final List<NMCommerceItem> items = order.getCommerceItems();
      for (final NMCommerceItem item : items) {
        if (!StringUtils.isEmpty(item.getPickupStoreNo())) {
          pageModel.setShowEstimate(false);
          break;
        }
      }
    }
    
    if (null != payerId) {
      pageModel.setPayPalLoggedIn(true);
      // When user signs into paypal ,Temppayment will be updated with SignedIntoPaypal true
      final TempPayment tp = orderHolder.getTemporaryPayment();
      if (tp != null) {
        tp.setSignedIntoPaypal(true);
      }
    }
    
    // 1874 story cookie setting
    if (isInternational) {
      for (final String key : messages.getIdMap().keySet()) {
        if (StringUtilities.isNotEmpty(key)) {
          if (key.equals(BorderFreeConstants.BFREDSTATUS_COOKIE)) {
            final String startMillis = String.valueOf(System.currentTimeMillis());
            CookieUtil.setCookie(ServletUtil.getCurrentRequest(), ServletUtil.getCurrentResponse(), BorderFreeConstants.BFREDSTATUS_COOKIE, startMillis, fiftyOneUtils.getCheckoutButtonDisabledTime());
            break;
          }
        }
      }
    }
    // 1874 story cookie setting End
    if ((nmBorderFreePayPalResponse.getPalPalResponseVO() != null)
            && ((null != nmBorderFreePayPalResponse.getPalPalResponseVO().getPayPalErrMsg()) || !StringUtils.isBlank(nmBorderFreePayPalResponse.getPalPalResponseVO().getErrorMessage()))) {
      String messageTxt = null;
      
      if (nmBorderFreePayPalResponse.getPalPalResponseVO().getErrorMessage() != null) {
        messageTxt = nmBorderFreePayPalResponse.getPalPalResponseVO().getErrorMessage();
        tmsMessageList.add(messageTxt);
      }
      if (null != nmBorderFreePayPalResponse.getPalPalResponseVO().getPayPalErrMsg()) {
        for (int i = 0; i < nmBorderFreePayPalResponse.getPalPalResponseVO().getPayPalErrMsg().size(); i++) {
          messageTxt = nmBorderFreePayPalResponse.getPalPalResponseVO().getPayPalErrMsg().get(i);
          tmsMessageList.add(messageTxt);
        }
      }
      
      addMessage(messages, MessageDefs.MSG_GenericFormErrorMessage, messageTxt, null, BorderFreeConstants.GROUP_TOP, null, null);
      
      pageModel.setMessages(messages.getIdMap());
      pageModel.setMessageFields(messages.getFieldIdMap());
      pageModel.setMessageGroups(messages.groupByPrefix(BorderFreeConstants.GROUP));
      pageModel.addMessageGroups(messages.groupByMsgId());
      nmBorderFreePayPalResponse.getPalPalResponseVO().setErrorMessage(null);
      nmBorderFreePayPalResponse.getPalPalResponseVO().setPayPalErrMsg(null);
      
    }
    // setting dytiesPaymentMethod to 1 i.e PREPAID by default
    /* order.setDutiesPaymentMethod("PREPAID"); */
    /* ExpPaypal_Flow start */
    // skipped for Express paypal
    if (!order.isExpressPaypal() && !Boolean.valueOf(changePPX)) {
      if (CheckoutAPI.isEmptyCart(order)) {
        getResponse().sendRedirect("/checkout/cart.jsp");
        return false;
      } else {
        // MasterPass Changes : Including a check for MasterPass Order
        if (CheckoutAPI.profileHasBillingAddress(profile) || order.isMasterPassPayGroupOnOrder()) {
          final boolean masterPassError = order.isMasterPassPayGroupOnOrder() && (messages.size() > 0) && messages.getIdMap().containsKey("MasterPassInvalidPostalCode");
          if ((!CheckoutAPI.orderContainsShippingAddress(order) && !order.hasAllBopsItems()) || masterPassError) {
            final CheckoutPageDefinition redirectPageDefinition = CheckoutComponents.getShippingPageDefinition();
            if (messages.size() > 0) {
              getResponse().sendLocalRedirect(redirectPageDefinition.getBreadcrumbUrl(), getRequest());
            } else {
              getResponse().sendRedirect(redirectPageDefinition.getBreadcrumbUrl());
            }
            getResponse().setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
            return false;
          } else if (isInternational && !CheckoutAPI.isInternationalOrderShipToValid(order, profile)) {
            // Ship to Country is not same as preferred country, redirect user to use different address.
            final CheckoutPageDefinition redirectPageDefinition = CheckoutComponents.getShippingPageDefinition();
            if (messages.size() > 0) {
              getResponse().sendLocalRedirect(redirectPageDefinition.getBreadcrumbUrl(), getRequest());
            } else {
              getResponse().sendRedirect(redirectPageDefinition.getBreadcrumbUrl());
            }
            getResponse().setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
            return false;
          } else if (order.hasShipToStoreAndActiveReplenishmentItem()) {
            final CheckoutPageDefinition redirectPageDefinition = CheckoutComponents.getCartPageDefinition();
            redirectPageDefinition.getBreadcrumbUrl();
            if (messages.size() > 0) {
              getResponse().sendLocalRedirect("/checkout/cart.jsp", getRequest());
            } else {
              getResponse().sendRedirect("/checkout/cart.jsp");
            }
            getResponse().setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
            return false;
          } else if (!isInternational && !CheckoutAPI.isDomesticOrderShipToValid(order)) {
            // 41279 - if Domestic ShipTo is not valid, redirect user to Shipping page.
            // Ship to Country is not same as preferred country, redirect user to use different address.
            final CheckoutPageDefinition redirectPageDefinition = CheckoutComponents.getShippingPageDefinition();
            if (messages.size() > 0) {
              getResponse().sendLocalRedirect(redirectPageDefinition.getBreadcrumbUrl(), getRequest());
            } else {
              getResponse().sendRedirect(redirectPageDefinition.getBreadcrumbUrl());
            }
            getResponse().setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
            return false;
            
          }
        } else {
          final CheckoutPageDefinition redirectPageDefinition = CheckoutComponents.getBillingPageDefinition();
          getResponse().sendRedirect(redirectPageDefinition.getBreadcrumbUrl());
          getResponse().setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
          return false;
        }
      }
      
      messages.clear();
      messages.addAll(CheckoutAPI.validateOrderAddresses(order, profile));
      if (!messages.isEmpty()) {
        pageModel.setInvalidOrderAddresses(true);
        pageModel.getMessageGroups().putAll(messages.setErrors(false).setFieldIds("groupTop").groupByPrefix("group"));
      } else {
        messages.addAll(CheckoutAPI.validateOrderShipping(cart, order, profile));
        messages.addAll(CheckoutAPI.updateShopRunnerPoBoxesToStandard(order));
        pageModel.getMessageGroups().putAll(messages.setErrors(false).setFieldIds("groupTop").groupByPrefix("group"));
      }
      dataDictionaryUtils.buildMsgList(messages.getAll(), tmsMessageList);
    }
    
    if (shouldUpdateShippingAddress) {
      EstimatedDeliveryDateSession getEstimatedDeliveryDateSession = CheckoutComponents.getEstimatedDeliveryDateSession(ServletUtil.getCurrentRequest());
      getEstimatedDeliveryDateSession.setEstimatedDeliveryDateCmosShippingVO(new EstimatedDeliveryDateCmosShippingVO());
      getEstimatedDeliveryDateSession.setExcludeServiceLevelsFromDisplay(false);
      pageModel.setServiceLevelsFetchedFromCmos(false);
    }
    
    CheckoutAPI.repriceOrder(cart, profile, -1);
    updateShippingGroups(order, pageModel, checkoutPageDefinition);
    if (shouldUpdateShippingAddress) {
      // If the second ESB call is made to the cmos to fetch service levels, then the same day delivery properties should be updated, In case the second call to Cmos thorugh ESB fails then the same
      // day delivery call will be made and the properties will be updated for the old flow.
      updateSameDayDeliveryProperties(pageModel, cart, order, messages, isInternational);
      CheckoutAPI.repriceOrder(cart, profile, -1);
    }
    final Map<String, CreditCard> ccTypes = new HashMap<String, CreditCard>();
    for (final CreditCard ccType : creditCardArray.getDisplayedCreditCards()) {
      ccTypes.put(ccType.getLongName(), ccType);
    }
    pageModel.setCreditCardArrayMap(ccTypes);
    pageModel.setPayPalCardType(creditCardArray.getCardTypeByLongName("PayPal"));
    
    // MasterPass changes : setting MasterPass card flag as true if MasterPass is selected
    if (order.isMasterPassPayGroupOnOrder()) {
      pageModel.setMasterPassCard(true);
      final NMCreditCard paymentGroup = order.getCreditCards().get(0);
      pageModel.setOrderCreditCard(paymentGroup);
      pageModel.setOrderCreditCardType(creditCardArray.getCardTypeByLongName(paymentGroup.getCreditCardType()));
      // clearing masterpass details if payment group contains any other paymentgroup other than masterpass
      if ((paymentGroup.isPayPal() || paymentGroup.isVmeCard() || CommonComponentHelper.getEmployeeDiscountsUtil().isAssociateLoggedIn(profile)) && paymentGroup.isMasterPassCard()) {
        CheckoutAPI.clearMasterPassDetails(order);
        pageModel.setMasterPassCard(false);
      }
    }
    
    if (!order.isExpressPaypal() && !order.isMasterPassPayGroupOnOrder()) {
      setOrderCreditCard(order, orderMgr, profile);
      final List<NMCreditCard> orderCreditCards = order.getCreditCards();
      if (!orderCreditCards.isEmpty()) {
        final NMCreditCard paymentGroup = orderCreditCards.get(0);
        
        // vme changes : clearing VmeDetails if paymentgroup contains either paypal or masterpass with Vme Card
        if (paymentGroup.isPayPal() && paymentGroup.isVmeCard()) {
          CheckoutAPI.clearVmeDetails(order);
        }
        if(paymentGroup.isPayPal()){
          pageModel.setPayPalLoggedIn(true);
        }
        // vme changes : setting vme card flag as true if vme is selected
        if (paymentGroup.isVmeCard()) {
          pageModel.setVmeCard(true);
        }
        
        // vme changes : added vme in below condition
        if (!StringUtilities.isEmpty(paymentGroup.getCreditCardNumber()) || paymentGroup.isPayPal() || paymentGroup.isVmeCard()) {
          setPaymentInfoToHandler(orderHolder, handler, isInternational, pageModel);
          final CreditCard creditCardType = creditCardArray.getCardTypeByLongName(paymentGroup.getCreditCardType());
          final NMCreditCard profileCard = paymentGroup.isPayPal() ? profile.getPayPalByEmail(paymentGroup.getPayPalEmail()) : profile.getCreditCardByNumber(paymentGroup.getCreditCardNumber());
          
          if (profileCard != null) {
            paymentGroup.setCidAuthorized(profileCard.getCidAuthorized());
          }
          
          pageModel.setOrderCreditCard(paymentGroup);
          pageModel.setOrderCreditCardType(creditCardType);
          
          if (!paymentGroup.isPayPal()) {
            final PaymentInfo paymentInfo = CheckoutAPI.creditCardToPaymentInfo(profileCard, profile.getBillingAddress());
            paymentInfo.setSkipSecCodeValidation(true);
            messages.getAll().clear();
            messages.addAll(CheckoutAPI.validatePaymentInfo(order, profile, paymentInfo));
            if (!messages.isEmpty()) {
              addMessage(messages, null, "Some of your credit card information is invalid. Please click change to update your card.", tmsMessageList, null, Boolean.FALSE, new Integer(0));
              pageModel.getMessageGroups().putAll(messages.setFieldIds("groupCreditCard").groupByPrefix("group"));
            }
          } else {
            // ICE (Precious Jewelry) orders cannot use PayPal
            if (order.isICEOrder()) {
              messages.getAll().clear();
              addMessage(messages, null, "We apologize, one or more item(s) cannot be purchased using PayPal. Please enter another form of payment.", tmsMessageList, null, Boolean.TRUE, null);
              pageModel.getMessageGroups().putAll(messages.setFieldIds("groupCreditCard").groupByPrefix("group"));
            } else {
              if (order.isPPRestrictedOrder()) {
                messages.getAll().clear();
                addMessage(messages, null, "One or more item(s) in your shopping bag cannot be purchased with PayPal. Please choose another form of payment or edit your shopping bag.",
                        tmsMessageList, null, Boolean.TRUE, null);
                pageModel.getMessageGroups().putAll(messages.setFieldIds("groupCreditCard").groupByPrefix("group"));
                
              }
              
            }
            
            pageModel.setDisplayPayPal(CheckoutAPI.isDisplayPayPalInfo(profile, CheckoutAPI.getPayPalToken(getRequest())));
          }
        }
        
        else {
          
          /*
           * If we are falling into this condition we don't have any payment information available for the customer. Check to see if we stored off any additional data in the temp object on the order.
           * If so reset the information into the form handler.
           */
          setPaymentInfoToHandler(orderHolder, handler, isInternational, pageModel);
        }
        
      }
    }
    
    // vme changes starts
    boolean isVmeShipppingAddrChanged = false;
    if (pageModel.isVmeCard()) {
      isVmeShipppingAddrChanged = CheckoutAPI.isVmeShippingAddrChanged(order);
    }
    if (!isInternational && (!pageModel.isVmeCard() || isVmeShipppingAddrChanged)) {
      pageModel.setShowAddressVerificationModal(CheckoutAPI.doesOrderRequireAddressVerification(profile, order));
    }
    // vme changes ends
    
    updateGwpFields(pageModel, order);
    
    pageModel.setEmail((String) profile.getPropertyValue(ProfileProperties.Profile_email));
    pageModel.setIsLoggedIn(profile.isAuthorized());
    pageModel.setFirstTime(orderHolder.getResponsiveState() < NMOrderHolder.RESPONSIVE_STATE_ORDER_REVIEW);
    
    final int securityStatus = ((Integer) profile.getPropertyValue("securityStatus")).intValue();
    // if ( (profile.isAnonymous() && profile.isRegisteredProfile() && securityStatus == 2) || !profile.getCountryPreference().equalsIgnoreCase( "US" ) ) {
    // second condition removed for Defect fix:40946
    if (profile.isAnonymous() && profile.isRegisteredProfile() && (securityStatus == 2)) {
      orderHolder.setResponsiveState(NMOrderHolder.RESPONSIVE_STATE_NONE);
    } else {
      orderHolder.setResponsiveState(NMOrderHolder.RESPONSIVE_STATE_ORDER_REVIEW);
    }
    
    final List<ShippingGroupCommerceItemRelationship> commerceItemRelationships = CheckoutAPI.getShippingGroupCommerceItemRelationships(order, orderMgr);
    pageModel.setCommerceItemRelationships(commerceItemRelationships);
    
    pageModel.setShowFinCenModal(CheckoutAPI.doFinCenCheck(order));
    
    final DynamoHttpServletRequest request = ServletUtil.getCurrentRequest();
    pageModel.setLoggedInShopRunner(loggedInShopRunner(request));
    if (isInternational) {
      final NMBorderFreeGetQuoteResponse nmBorderFreeGetQuoteResponse = CheckoutComponents.getNMBorderFreeGetQuoteResponse(getRequest());
      if (!StringUtils.isBlank(nmBorderFreeGetQuoteResponse.getErrorMessage())) {
        if (order.isExpressPaypal() && nmBorderFreeGetQuoteResponse.getErrorMessage().equalsIgnoreCase(esbErrorMessages.getErrorMessage("OPT_DISC_INVALID_COUPON_CODE", countryPref))) {
          addMessage(messages, null, esbErrorMessages.getErrorMessage("PPX_PROMO_RESTR_MSG", countryPref), tmsMessageList, null, Boolean.TRUE, null);
        } else {
          addMessage(messages, null, nmBorderFreeGetQuoteResponse.getErrorMessage(), tmsMessageList, null, Boolean.TRUE, null);
        }
        pageModel.getMessageGroups().putAll(messages.setErrors(false).setFieldIds("groupTop").groupByPrefix("group"));
      }
    }
    /*
     * Making CheetahmailAPI call for whether emailid is opted or not. Rel-2 configured Australia, Germany, Japan and Canada countries.
     */
    
    final ContactInfo billingAddress = CheckoutAPI.getContactInfo(profile, ProfileProperties.Profile_billingAddress);
    final String bCountryCode = billingAddress.getCountry();
    final String bEmail = pageModel.getProfile().getEmailAddressBeanProp();
    final LocalizationUtils localizationUtils = CheckoutComponents.getLocalizationUtils();
    final boolean candianEmail = localizationUtils.isCanadianEmail(bEmail);
    
    if (FiftyOneUtils.DEFAULT_COUNTRY_CODE.equalsIgnoreCase(bCountryCode) && !candianEmail) {// if bill to country is US,Do not show optin checkbox
      pageModel.setShowEmailOptinCheckBox(false);
      pageModel.setOptinUser(true);
      
    } else {
      if (isInternational) {
        // for International checkout Show option checkbox if bill to country is listed in the optinCountry list or email is canada email
        if (candianEmail || localizationUtils.isEmailOptinCountries(bCountryCode)) {
          pageModel.setShowEmailOptinCheckBox(true);
        } else {
          pageModel.setOptinUser(true);
        }
      } else {// For Domestic Checkout Billing country is not "US" or email is canada email , show optin Checkbox
        pageModel.setShowEmailOptinCheckBox(true);
      }
    }
    final HttpSession session = request.getSession();
    if (!StringUtils.isBlank((String) session.getAttribute(EmployeeDiscountConstants.SYNC_FAIL))) {
      pageModel.setPopupMsg((String) session.getAttribute(EmployeeDiscountConstants.SYNC_FAIL));
      session.removeAttribute(EmployeeDiscountConstants.SYNC_FAIL);
    }
    
    // Abtesting for the allow guest user to register in order review page
    String guestUserAbTest = AbTestHelper.getAbTestValue(getRequest(), ORDERREVIEWREGISTRATION_ABTESTGROUP);
    if (guestUserAbTest == null) {
      guestUserAbTest = "inactive";
    }
    
    final boolean isEmailRegistered = isEmailRegistered((String) profile.getPropertyValue(ProfileProperties.Profile_email));
    request.setParameter(ORDERREVIEWREGISTRATION_ABTESTGROUP, guestUserAbTest);
    request.setParameter(IS_REGISTEREDEMAIL, isEmailRegistered);
    
    if (isEmailRegistered) {
      orderHolder.storeTemporaryUserInfo(null);
    }
    
    // if a BOGO is awarded but the order has more than one shipping group, set an error
    if (order.getHasAwardedBogoPromotion() && CheckoutAPI.getIsShipToMultiple(order)) {
      addMessage(messages, MessageDefs.MSG_BOGO_Promotion_address_limit, null, tmsMessageList, null, Boolean.TRUE, null);
      pageModel.getMessageGroups().putAll(messages.setErrors(false).setFieldIds("groupTop").groupByPrefix("group"));
    }
    if (CommonComponentHelper.getSystemSpecs().isPercentOffPromoForPLCCEnabled()) {
      // the below change is for to restrict the ajax call from orderreview page payment drop down,only if 112 is present and PLCC flag enabled.
      pageModel.setPlccRepriceOrderFlag(CheckoutAPI.verifyReviewPagePromoCodePLCC(order));
      // this change is to allow PLCC 112 percent off promotion on the orderreview page.as this promotion has to be applied all the time, if promotion is present in the order, till the user clicks the
      // place order.
      order.setUserClickedPlaceOrder(false);
    }
    if(CheckoutAPI.isVmeReturned(getRequest())){
        pageModel.setVmeReturned(true);
    }
    
    if ((!(order.isShopRunnerOrder() && loggedInShopRunner(getRequest()))) || (!isInternational)){
      if (pageModel.getShippingGroups() != null && pageModel.getShippingGroups().length > 0 && pageModel.getShippingGroups()[0] != null) {
        ServiceLevel sgServiceLevel = pageModel.getShippingGroups()[0].getServiceLevel();
        if(sgServiceLevel != null){
          pageModel.setCurrentServiceLevel(pageModel.getShippingGroups()[0].getServiceLevel().getCode());
        }
      }
      List<String> serviceLevelErrMsg = new ArrayList<String>();
      CheckoutAPI.setValidServiceLevelsWhenCmosFails(pageModel.getValidServiceLevels());
      serviceLevelErrMsg.addAll(CheckoutAPI.updateProductCountryValidServiceLevels(cart, order.getCommerceItems(), shipToCountry, pageModel.getCurrentServiceLevel()));
      String html="";
      for (int i=0; i < serviceLevelErrMsg.size(); i++) {
        html = html + serviceLevelErrMsg.get(i)+"<br>";
      }
      pageModel.setServiceLevelErrMsg(html);
    }
    // Logic has been added for using expresscheckout functionality available in normal checkout flow.
    if (null != pageModel.getPageId() && order.isBuyItNowOrder()) {
      CheckoutAPI.paymentCheckoutValidation(pageModel, order, orderMgr, profile, getRequest(), tmsMessageList);
    }
    // Data Dictionary Attributes population.
    // set the collected message list to TMSContainer
    tmsMessageContainer.setMessages(tmsMessageList);
    getPageModel().setAccountRecentlyRegistered(getProfile().isAccountRecentlyRegistered());
    getProfile().setAccountRecentlyRegistered(false);
    dataDictionaryUtils.processTMSDataDictionaryAttributes(pageDefinition, order, profile, pageModel, tmsMessageContainer);
    // setting estimated delivery date AB test group
    if (ACTIVE.equalsIgnoreCase(AbTestHelper.getAbTestValue(ServletUtil.getCurrentRequest(), EDD_NEW_LAYOUT_AB_TEST_GROUP))) {
      if (!NmoUtils.isEmptyMap(pageModel.getGroupedLineItemsByEstimatedDeliveryDate())) {
        pageModel.setEstimatedDeliveryDateGroupingLayout(true);
      }
    }
        
    return true;
  }
  
  /**
   * The purpose of this method is to update same day delivery properties.
   * 
   * @param pageModel
   *          the page model
   * @param cart
   *          the cart
   * @param order
   *          the order
   * @param messages
   *          the messages
   * @param isInternational
   *          the is international
   * @throws Exception
   *           the exception
   */
  private void updateSameDayDeliveryProperties(final CheckoutPageModel pageModel, final ShoppingCartHandler cart, final NMOrderImpl order, final MessageContainer messages,
          final boolean isInternational) throws Exception {
    boolean restrictSDDOldFlow = false;
    EstimatedDeliveryDateCmosShippingVO estimatedDeliveryDateCmosShippingVO = CheckoutComponents.getEstimatedDeliveryDateSession(getRequest()).getEstimatedDeliveryDateCmosShippingVO();
    
    if ((null != estimatedDeliveryDateCmosShippingVO) && NmoUtils.isNotEmptyCollection(estimatedDeliveryDateCmosShippingVO.getServiceLevels())) {
      restrictSDDOldFlow = true;
    }
    
    if (CommonComponentHelper.getBrandSpecs().isSameDayDeliveryEnabled() && !isInternational && !restrictSDDOldFlow) {
      final ShippingGroupAux[] shippingGroups = pageModel.getShippingGroups();
      final SameDayDeliveryUtil sameDayDeliveryUtil = CommonComponentHelper.getSameDayDeliveryUtil();
      final Boolean orderHasAtLeastOneSDDItem = sameDayDeliveryUtil.isOrderHasAtLeastOneSDDItem(order.getNmCommerceItems());
      final SameDayDeliveryOrderInfo sameDayDeliveryOrderInfo = sameDayDeliveryUtil.getOrderEligibleForSdd(order, shippingGroups);
      if (orderHasAtLeastOneSDDItem && !sameDayDeliveryOrderInfo.isSddEligible()) {
        CheckoutAPI.updateAllShippingGroupServiceLevels((NMOrderManager) OrderManager.getOrderManager(), order, getProfile(), CheckoutComponents.getServiceLevelArray()
                .determineFreeShippingServiceLevel(), null);
        CheckoutAPI.repriceOrder(cart, getProfile(), SnapshotUtil.CHANGE_SHIP_METHOD);
        
        if (!sameDayDeliveryOrderInfo.getSddEligibleByItems()) {
          addMessage(messages, null, sameDayDeliveryUtil.getSddItemDisqualificationText(), null, null, null, null);
        } else if (!sameDayDeliveryOrderInfo.getSddEligibleByLocation()) {
          addMessage(messages, null, sameDayDeliveryUtil.getSddZipCodeDisqualificationText(), null, null, null, null);
        }
        pageModel.getMessageGroups().putAll(messages.setErrors(false).setFieldIds(BorderFreeConstants.GROUP_TOP).groupByPrefix(BorderFreeConstants.GROUP));
      }
      final Boolean isSameDayDeliveryOrder = CommonComponentHelper.getSameDayDeliveryUtil().isSameDayDeliveryOrder(order);
      final Boolean isDisplaySameDayDeliverySL = sameDayDeliveryOrderInfo.isSddEligible() && !order.hasAllBopsItems();
      pageModel.setDisplaySameDayDeliverySL(isDisplaySameDayDeliverySL);
      pageModel.setSameDayDeliveryOrder(isSameDayDeliveryOrder);
    } else if (pageModel.isServiceLevelsFetchedFromCmos()) {
      for (ServiceLevel cmosServiceLevel : pageModel.getValidServiceLevels()) {
        if (ServiceLevel.SL0_SERVICE_LEVEL_TYPE.equalsIgnoreCase(cmosServiceLevel.getCode())) {
          CheckoutAPI.updateSameDayDeliveryProperties(order, pageModel);
        }
      }
    }
  }
  
  private boolean isEmailRegistered(final String email) {
    boolean result = false;
    try {
      final NMCommerceProfileTools profileTools = CheckoutComponents.getCommerceProfileTools();
      final RepositoryItem profile = profileTools.getItem(email.toUpperCase(), null);
      if (profile != null) {
        result = true;
      }
    } catch (final Exception e) {
      // in case of exception return false
    }
    return result;
  }
  
  protected boolean loggedInShopRunner(final DynamoHttpServletRequest request) {
    final String shopRunnerToken = CheckoutAPI.getShopRunnerToken(request);
    boolean loggedInSR = false;
    if (shopRunnerToken != null) {
      loggedInSR = true;
    }
    return loggedInSR;
  }
  
  protected void setOrderCreditCard(final NMOrderImpl order, final NMOrderManager orderMgr, final NMProfile profile) throws Exception {
    final List<NMCreditCard> orderCreditCards = order.getCreditCards();
    if (!orderCreditCards.isEmpty()) {
      final NMCreditCard paymentGroup = orderCreditCards.get(0);
      
      if ((paymentGroup.isPayPal() && (profile.getPayPalByEmail(paymentGroup.getPayPalEmail()) == null)) || StringUtilities.isEmpty(paymentGroup.getCreditCardNumber())
              || (profile.getCreditCardByNumber(paymentGroup.getCreditCardNumber()) == null)) {
        if (profile.getDefaultCreditCard() != null) {
          CheckoutAPI.changeCreditCardOnOrder(order, orderMgr, profile, profile.getDefaultCreditCard());
        } else {
          paymentGroup.setCreditCardNumber("");
          paymentGroup.setCreditCardType("");
          paymentGroup.setCid("");
          paymentGroup.setExpirationMonth("");
          paymentGroup.setExpirationYear("");
          paymentGroup.setCidAuthorized(false);
        }
      }
    } else if (profile.getDefaultCreditCard() != null) {
      CheckoutAPI.changeCreditCardOnOrder(order, orderMgr, profile, profile.getDefaultCreditCard());
    }
  }
  
  private void setPaymentInfoToHandler(final NMOrderHolder orderHolder, final OrderSubmitFormHandler handler, final boolean isInternational, final CheckoutPageModel pageModel) {
    if ((orderHolder != null) && (orderHolder.getTemporaryPayment() != null)) {
      final TempPayment tp = orderHolder.getTemporaryPayment();
      if ((handler != null) && isPersistedCardApplicableToSelectedCountry(tp, isInternational, pageModel)) {
        Properties mappings = null;
        final LocalizationUtils localizationUtils = CheckoutComponents.getLocalizationUtils();
        if (isInternational) {
          mappings = localizationUtils.getDomesticIntlPaymentMethodMapping();
        } else {
          mappings = localizationUtils.getIntlDomesticPaymentMethodMapping();
        }
        
        if ((null != mappings) && (null != tp.getCardType()) && (null != mappings.getProperty(tp.getCardType()))) {
          handler.setCreditCardTypeCode(mappings.getProperty(tp.getCardType()));
        } else {
          handler.setCreditCardTypeCode(tp.getCardType());
        }
        handler.setCreditCardNumber(tp.getMaskedCardNumber());
        handler.setCreditCardExpMonth(tp.getExpirationMonth());
        handler.setCreditCardExpYear(tp.getExpirationYear());
        handler.setCreditCardSecCode(tp.getMaskedSecurityCode());
      } else {
        orderHolder.storeTemporaryPayment(null);
      }
    }
  }
  
  private boolean isPersistedCardApplicableToSelectedCountry(final TempPayment tp, final boolean isInternational, final CheckoutPageModel pageModel) {
    final String internationalPayPal = "PAYPAL_PAYMENT";
    final String domesticPayPal = "PAYP";
    final String cardType = tp.getCardType();
    boolean persist = false;
    final Set<String> allCards = new HashSet<String>(25);
    
    if (StringUtilities.isEmpty(cardType)) {
      // when there is a default credit card in profile and cardType is
      // null as there is no payment selection dropdown.
      // Only secure code field is visible for some cards and needs to be
      // persisted
      persist = true;
    } else if (isInternational) {
      if (tp.isSignedIntoPaypal() && domesticPayPal.equals(cardType)) {
        // when user switched from domestic to international, dont
        // persist paypal fields
        persist = false;
      } else {
        final LocalizationUtils localizationUtils = CheckoutComponents.getLocalizationUtils();
        Properties mappings = localizationUtils.getIntlDomesticPaymentMethodMapping();
        if (mappings == null) {
          mappings = new Properties();
        }
        final NMBorderFreeGetQuoteResponse nmBorderFreeGetQuoteResponse = CheckoutComponents.getNMBorderFreeGetQuoteResponse(getRequest());
        String intlCardType;
        final AvailablePaymentMethodsResposeVO[] avaliablePaymentMethods = nmBorderFreeGetQuoteResponse.getAvailablePaymentMethodsResponseVO();
        if (avaliablePaymentMethods != null) {
          for (final AvailablePaymentMethodsResposeVO availablePaymentMethods : avaliablePaymentMethods) {
            // adding all payment types returned by Borderfree to all cards list
            allCards.add(availablePaymentMethods.getPaymentMethodId());
            intlCardType = mappings.getProperty(availablePaymentMethods.getPaymentMethodId());
            if (intlCardType != null) {
              allCards.add(intlCardType);
            }
          }
          
        }
        // Adding CUP card to List as BorderFree always does not return
        // CUP
        allCards.add("CUP");
        if (allCards.contains(cardType)) {
          // Checking the persisted card is applicable to selected country or not
          persist = true;
        }
      }
    } else {// domestic
      if (internationalPayPal.equals(cardType)) {
        // when user switched from international to domestic, do not
        // persist paypal fields
        persist = false;
      } else {
        for (final CreditCard ccType : pageModel.getCreditCardArrayMap().values()) {
          // adding all cards from DB to set
          allCards.add(ccType.getCode());
        }
        if (pageModel.getPayPalCardType() != null) {
          // adding PAYPAL cards from as it does not come from DB
          allCards.add(pageModel.getPayPalCardType().getCode());
          allCards.add(internationalPayPal);
        }
        if (allCards.contains(cardType)) {
          // Checking the persisted card is applicable to selected country or not
          persist = true;
        }
      }
    }
    
    return persist;
  }
  
  /**
   * Function adds a message indicating the order can not be completed
   * 
   * @param messages
   *          the collection of messages to add the Message to
   * @param msgId
   *          the message ID to construct from MessageDefs; may be null
   * @param msgText
   *          the message text to add to the collection of messages
   * @param tmsMessageList
   *          the tmsMessageList to also append the msgText to, if not null
   * @param fieldId
   *          the fieldId to set into the Message, if not null
   * @param isError
   *          if not null, set the error on the Message to this
   * @param index
   *          if not null, set the new Message to this index in the collection of messages
   */
  private void addMessage(final MessageContainer messages, final String msgId, final String msgText, final ArrayList<String> tmsMessageList, final String fieldId, final Boolean isError,
          final Integer index) {
    Message msg = null;
    
    if (msgId == null) {
      msg = new Message();
    } else {
      msg = MessageDefs.getMessage(msgId);
    }
    
    if (msgText != null) {
      msg.setMsgText(msgText);
    }
    
    if (tmsMessageList != null) {
      tmsMessageList.add(msg.getMsgText());
    }
    
    if (fieldId != null) {
      msg.setFieldId(fieldId);
    }
    
    if (isError != null) {
      msg.setError(isError);
    }
    
    if (index == null) {
      messages.add(msg);
    } else {
      messages.add(index.intValue(), msg);
    }
  }

  public Boolean isJumioFeatureEnabled() {
	  final NMSpecUtils nmSpecUtils = CommonComponentHelper.getNMSpecUtils();
	  final Boolean isEnabled = nmSpecUtils.isFeatureActive(JUMIO_KEY, JUMIO_KEY_TYPE);
    return isEnabled;
  }
}
