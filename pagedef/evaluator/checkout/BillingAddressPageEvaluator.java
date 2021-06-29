package com.nm.commerce.pagedef.evaluator.checkout;

import static com.nm.common.INMGenericConstants.TRUE_STRING;
import static com.nm.integration.MasterPassConstants.PAYMENT_TYPE_CHANGED;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.jsp.PageContext;

import atg.commerce.order.HardgoodShippingGroup;
import atg.commerce.order.ShippingGroup;
import atg.core.util.StringUtils;
import atg.servlet.DynamoHttpServletRequest;
import atg.servlet.ServletUtil;

import com.nm.commerce.NMAddress;
import com.nm.commerce.NMCreditCard;
import com.nm.commerce.NMProfile;
import com.nm.commerce.checkout.CheckoutAPI;
import com.nm.commerce.checkout.CheckoutComponents;
import com.nm.commerce.checkout.beans.ContactInfo;
import com.nm.commerce.pagedef.definition.PageDefinition;
import com.nm.commerce.pagedef.model.CheckoutPageModel;
import com.nm.commerce.promotion.NMOrderImpl;
import com.nm.components.CommonComponentHelper;
import com.nm.formhandler.ShoppingCartHandler;
import com.nm.formhandler.checkout.BillingAddressFormHandler;
import com.nm.profile.ProfileProperties;
import com.nm.sitemessages.Message;
import com.nm.sitemessages.MessageContainer;
import com.nm.tms.core.TMSMessageContainer;
import com.nm.tms.utils.TMSDataDictionaryUtils;
import com.nm.twoclickcheckout.config.TwoClickCheckoutConfig;
import com.nm.twoclickcheckout.util.TwoClickCheckoutUtil;

public class BillingAddressPageEvaluator extends CheckoutPageEvaluator {
  
  @Override
  public boolean evaluate(final PageDefinition pageDefinition, final PageContext pageContext) throws Exception {
    final boolean returnValue = super.evaluate(pageDefinition, pageContext);
    final boolean paymentTypeChanged = TRUE_STRING.equalsIgnoreCase(getRequest().getParameter(PAYMENT_TYPE_CHANGED));
    final CheckoutPageModel pageModel = (CheckoutPageModel) getPageModel();
    final String sgId = getRequest().getParameter("sgId");
    final ShoppingCartHandler cart = CheckoutComponents.getCartHandler(getRequest());
    final NMProfile profile = CheckoutComponents.getProfile(getRequest());
    final BillingAddressFormHandler addr = CheckoutComponents.getBillingAddressFormHandler(getRequest());
    final MessageContainer messagesContainer = CheckoutComponents.getMessageContainer(getRequest());
    final NMOrderImpl order = cart.getNMOrder();
    final List<ShippingGroup> shipGroups = order.getShippingGroups();
    final TMSDataDictionaryUtils dataDictionaryUtils = CommonComponentHelper.getDataDictionaryUtils();
    // resolve the TMSMessageContainer from request
    final TMSMessageContainer tmsMessageContainer = CheckoutComponents.getTMSMessageContainer(getRequest());
    final ArrayList<String> tmsMessageList = new ArrayList<String>();
    boolean skipShipping = true;
    for (final Iterator<ShippingGroup> i = shipGroups.iterator(); i.hasNext();) {
      final HardgoodShippingGroup sg = (HardgoodShippingGroup) i.next();
      if ((sg.getShippingAddress() == null) || (sg.getShippingAddress().getAddress1() == null)) {
        skipShipping = false;
        break;
      }
    }
    dataDictionaryUtils.buildMsgList(messagesContainer.getAll(), tmsMessageList);
    addr.setSkipShipping(skipShipping);
    
    if (profile != null) {
      NMAddress address = profile.getBillingNMAddress();
      List<Message> messages = addr.getMessageList();
      
      /*
       * Registered Checkout ALL information in account STARTS
       */
      if ((address != null) && (messages.isEmpty())) {
        if (CheckoutAPI.isInternationalSession(profile)) {
          String defaultBillingCountry = null;
          boolean isBorderFreeSupportedCountry = CheckoutAPI.validateBorderFreeSupportedCountries(profile);
          // Check if Billing Address is valid. If not do not prepopulate.
          ContactInfo contactInfo = CheckoutAPI.getContactInfo(profile, ProfileProperties.Profile_billingAddress);
          boolean isBillAddrSupported = CheckoutAPI.isAddressSupportedByBorderfree(contactInfo);
          if ((null != profile.getBillingNMAddress()) && (null != profile.getBillingNMAddress().getCountry())) {
            defaultBillingCountry = profile.getBillingNMAddress().getCountry();
          }
          NMCreditCard defaultCard = profile.getDefaultCreditCard();
          boolean isPaypalAddress = false;
          if ((defaultCard != null) && !StringUtils.isBlank(defaultCard.getCreditCardType()) && (defaultCard.isPayPal() || "PAYP".equalsIgnoreCase(defaultCard.getCreditCardType()))) {
            isPaypalAddress = true;
          }
          // Pre-populate address fields only if address is supported and not paypal address.
          if (((null != defaultBillingCountry) && isBorderFreeSupportedCountry && isBillAddrSupported && !isPaypalAddress)) {
            addr.prefillFields(address, profile.getEmailAddressBeanProp());
          } else {
            addr.prefillFirstAndLastNameFields(address, profile.getEmailAddressBeanProp());
          }
          if ((((null != defaultBillingCountry) && defaultBillingCountry.equalsIgnoreCase(profile.getCountryPreference())) || (null == defaultBillingCountry))
                  && CommonComponentHelper.getSystemSpecs().isFiftyOneEnabled()) {
            skipShipping = false;
            addr.setSkipShipping(skipShipping);
          }
        } else if (!paymentTypeChanged) {
          // MasterPass Changes : Skip this step for MasterPass when Clicked on "Change" button on Order review page for a guest user
          addr.prefillFields(address, profile.getEmailAddressBeanProp());
        }
      } else {
        if (profile.isAnonymous()) {
          addr.prefillEmail(addr.getEmail());
        } else {
          addr.prefillEmail(profile.getEmailAddressBeanProp());
        }
        dataDictionaryUtils.buildMsgList(messages, tmsMessageList);
      }
      /*
       * Registered Checkout ALL information in account ENDS
       */
      if (addr.getMessageList() != null) {
        String countryPreference = null;
        DynamoHttpServletRequest request = ServletUtil.getCurrentRequest();
        if (request != null) {
          NMProfile nmProfile = (NMProfile) request.resolveName("/atg/userprofiling/Profile");
          if (nmProfile != null) {
            countryPreference = nmProfile.getCountryPreference();
          }
        }
        
        Map<String, String> errMsgMap = buildErrorMsgMap(addr.getMessageList(), countryPreference, CheckoutAPI.isInternationalSession(profile));
        pageModel.setErrorMsgMap(errMsgMap);
      }
    }
    if (!profile.isAnonymous()) {
      pageModel.setIsLoggedIn(new Boolean(true));
    }
    if ((order.hasBopsItem() && (order.getCommerceItems().size() >= 1))) {
      // mixed cart of BOPS and non BOPS items or one single BOPS item
      pageModel.setDisplayUseAsShip(false);
    } else {
      // cart of only non BOPS items
      pageModel.setDisplayUseAsShip(true);
    }
    pageModel.setOrderHasActiveReplenishmentItems(order.hasActiveReplishmentItems());
    
    final TwoClickCheckoutUtil twoClickCheckoutUtil = CommonComponentHelper.getTwoClickCheckoutUtil();
    if (null != twoClickCheckoutUtil) {
      final TwoClickCheckoutConfig twoClickCheckoutConfig = twoClickCheckoutUtil.getTwoClickCheckoutConfig();
      pageModel.setExpressCheckoutEnablePageUrl(twoClickCheckoutConfig.getExpressCheckoutEnablePageUrl());
      pageModel.setExpressCheckoutBillingPageUrl(twoClickCheckoutConfig.getExpressCheckoutBillingPageUrl());
      pageModel.setExpressCheckoutShippingPageUrl(twoClickCheckoutConfig.getExpressCheckoutShippingPageUrl());
      pageModel.setExpressCheckoutPaymentPageUrl(twoClickCheckoutConfig.getExpressCheckoutPaymentPageUrl());
    }

    updateGwpFields(pageModel, order);
    // Data Dictionary Attributes population.
    tmsMessageContainer.setMessages(tmsMessageList);
    dataDictionaryUtils.processTMSDataDictionaryAttributes(pageDefinition, order, profile, pageModel,tmsMessageContainer);
    pageModel.setProfileContainsPaymentInfo(profile.isDefaultCreditCardAuthorized());
    return returnValue;
  }
  
  private Map<String, String> buildErrorMsgMap(List<Message> msgList, String country, boolean isInternational) {
    Map<String, String> errorMsgMap = new HashMap<String, String>();
    Iterator<Message> msgIterator = msgList.iterator();
    Message msg = null;
    while (msgIterator.hasNext()) {
      msg = msgIterator.next();
      // changes to get international validation message text
      if (!isInternational) {
        errorMsgMap.put(msg.getFieldId(), msg.getMsgText());
      } else {
        errorMsgMap.put(msg.getFieldId(), msg.getMsgIntlText());
      }
    }
    return errorMsgMap;
  }
  
}
