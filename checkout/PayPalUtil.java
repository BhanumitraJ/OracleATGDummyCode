package com.nm.commerce.checkout;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.httpclient.ConnectTimeoutException;

import atg.commerce.order.CommerceItem;
import atg.commerce.order.HardgoodShippingGroup;
import atg.commerce.order.ShippingGroup;
import atg.commerce.pricing.OrderPriceInfo;
import atg.core.util.StringUtils;
import atg.nucleus.Nucleus;
import atg.repository.MutableRepository;
import atg.repository.MutableRepositoryItem;
import atg.repository.RepositoryItem;

import com.nm.commerce.NMAddress;
import com.nm.commerce.NMCommerceItem;
import com.nm.commerce.NMCommerceProfileTools;
import com.nm.commerce.NMCreditCard;
import com.nm.commerce.NMProfile;
import com.nm.commerce.NMRepositoryContactInfo;
import com.nm.commerce.catalog.NMProduct;
import com.nm.commerce.checkout.beans.ContactInfo;
import com.nm.commerce.order.NMOrderManager;
import com.nm.commerce.pricing.NMOrderPriceInfo;
import com.nm.commerce.promotion.NMOrderImpl;
import com.nm.components.CommonComponentHelper;
import com.nm.formhandler.ShoppingCartHandler;
import com.nm.integration.PayPal;
import com.nm.integration.util.paypal.PayPalException;
import com.nm.integration.util.paypal.PayPalFundingFailureException;
import com.nm.integration.util.paypal.PayPalInvalidAddressException;
import com.nm.integration.util.paypal.PayPalTokenExpiredException;
import com.nm.integration.util.paypal.esb.DoAuthorizeTransactionRequest;
import com.nm.integration.util.paypal.esb.DoAuthorizeTransactionResponse;
import com.nm.integration.util.paypal.esb.DoReferenceTransactionRequest;
import com.nm.integration.util.paypal.esb.DoReferenceTransactionResponse;
import com.nm.integration.util.paypal.esb.PayPalAddressEsbDTO;
import com.nm.integration.util.paypal.esb.PayPalWebServiceHandler;
import com.nm.integration.util.paypal.params.DoExpressCheckoutRequest;
import com.nm.integration.util.paypal.params.DoExpressCheckoutResponse;
import com.nm.integration.util.paypal.params.GetExpressCheckoutRequest;
import com.nm.integration.util.paypal.params.GetExpressCheckoutResponse;
import com.nm.integration.util.paypal.params.PayPalError;
import com.nm.integration.util.paypal.params.PayPalParams;
import com.nm.integration.util.paypal.params.PayPalResponseParams;
import com.nm.integration.util.paypal.params.PaymentInfo;
import com.nm.integration.util.paypal.params.PaymentRequest;
import com.nm.integration.util.paypal.params.PaymentRequestDetails;
import com.nm.integration.util.paypal.params.PaymentRequestItem;
import com.nm.integration.util.paypal.params.SetExpressCheckoutRequest;
import com.nm.integration.util.paypal.params.SetExpressCheckoutResponse;
import com.nm.profile.ProfileProperties;
import com.nm.sitemessages.Message;
import com.nm.utils.AddressNickname;
import com.nm.utils.BrandSpecs;
import com.nm.utils.ProdSkuUtil;
import com.nm.utils.StringUtilities;
import com.nm.utils.SystemSpecs;
import com.nm.utils.VendorSpecs;

/* package */class PayPalUtil {

  public static synchronized PayPalUtil getInstance() {
    INSTANCE = INSTANCE == null ? new PayPalUtil() : INSTANCE;
    return INSTANCE;
  }

  public PayPal getPayPal() {
    return payPal;
  }

  public String setExpressCheckout(final NMOrderImpl order, final NMProfile profile, final String rootUrl, final String returnPath, final String cancelPath) throws PayPalException {
    return setExpressCheckout(order, profile, rootUrl, returnPath, PayPal.DEFAULT_RETURN_PARAM_VALUE, cancelPath);
  }

  public boolean containsVirtualGiftCardsOnly(final NMOrderImpl order) {

    final List<CommerceItem> items = order.getCommerceItems();

    for (final CommerceItem item : items) {
      final NMCommerceItem ci = (NMCommerceItem) item;
      final NMProduct product = ci.getProduct();
      final boolean isVirtual = prodSkuUtil.isVirtualGiftCard(product.getDataSource());
      if (!isVirtual) {
        return false;
      }
    }
    return true;

  }

  public String setExpressCheckout(final NMOrderImpl order, final NMProfile profile, final String rootUrl, final String returnPath, final String returnValue, final String cancelPath)
          throws PayPalException {
    final Collection<PayPalParams> paymentRequests = buildPaymentRequests(order, profile, rootUrl, false);
    final String returnUrl = payPal.appendReturnUrlParam(rootUrl + returnPath, returnValue);
    final String cancelUrl = payPal.appendCancelUrlParam(rootUrl + cancelPath);
    @SuppressWarnings("unchecked")
    final boolean hideShipping = CheckoutAPI.getNonWishlistShippingGroupCount(order.getShippingGroups()) != 1;

    final SetExpressCheckoutRequest request =
            new SetExpressCheckoutRequest.Builder(payPal.getVersion(), payPal.getUsername(), payPal.getPassword(), payPal.getMerchantEmail(), returnUrl, cancelUrl).noShipping(hideShipping ? 1 : 2)
            .allowNote(false).localeCode(profile.getCountryPreference()).email(profile.getEmailAddressBeanProp()).landingPage("Login")
            .logoImg(joinUrl(rootUrl, brandSpecs.getPaypalBrandLogoPath())).customerServiceNumber(systemSpecs.getEcarePhoneNumber()).buyerEmailOptInEnable(false)
            .paymentRequests(paymentRequests).billingTypes("MerchantInitiatedBillingSingleAgreement").reqBillingAddress(true).build();

    SetExpressCheckoutResponse response = null;
    try {
      response = payPal.setExpressCheckout(request);
    } catch (final ConnectTimeoutException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    checkFailure(response);

    return response.getToken();
  }

  public Collection<Message> getExpressCheckout(final ShoppingCartHandler cart, final NMProfile profile, final String token) throws Exception {
    final List<Message> messages = new ArrayList<Message>();
    final NMOrderImpl order = cart.getNMOrder();
    final NMOrderManager orderMgr = (NMOrderManager) cart.getOrderHolder().getOrderManager();
    final MutableRepository profileRepo = (MutableRepository) profile.getRepository();

    final GetExpressCheckoutRequest request = new GetExpressCheckoutRequest.Builder(payPal.getVersion(), payPal.getUsername(), payPal.getPassword(), payPal.getMerchantEmail(), token).build();

    final GetExpressCheckoutResponse response = payPal.getExpressCheckout(request);

    checkTokenExpired(response);
    checkFailure(response);
    checkPayPalInvalidAddress(response);
    final boolean isVmeCard = order.isVisaCheckoutPayGroupOnOrder();

    if (StringUtilities.isEmpty(profile.getEmailAddressBeanProp()) || isVmeCard) {
      profile.setEmailAddressBeanProp(response.getEmail().toLowerCase());
    }

    for (final PaymentRequestDetails paymentRequest : response.getPaymentRequests()) {
      final String fullName = paymentRequest.getShipToName();
      String firstName = null;
      String lastName = null;
      if (fullName != null) {
        final String[] firstLast = fullName.split(" ");
        firstName = firstLast[0];
        lastName = firstLast[1];
      }
      final String country = paymentRequest.getShipToCountryCode();
      final String address1 = paymentRequest.getShipToStreet();
      final String address2 = paymentRequest.getShipToStreet2();
      final String city = paymentRequest.getShipToCity();
      final String state = paymentRequest.getShipToState();
      String zip = paymentRequest.getShipToZip();
      if (null != zip && zip.trim().length() != 0 && zip.length() > 5) {
        zip = zip.substring(0, 5);
      }
      final String phoneNum = paymentRequest.getShipToPhoneNum();

      if (!StringUtils.isEmpty(country) && !"US".equals(country)) {
        final Message msg = new Message();
        if (systemSpecs.isFiftyOneEnabled()) {
          msg.setMsgText("You have selected an international address on PayPal. Please choose a U.S. address or <a href='#' id='changeCountryLink'><b>click here</b></a> to change your shipping country and checkout with our international shipping partner, Borderfree.");
        } else {
          msg.setMsgText("You have selected an international address on PayPal. Please choose a U.S. shipping address.");
        }
        msg.setFieldId("groupTop");
        messages.add(msg);
      } else if (StringUtilities.isNotEmpty(StringUtilities.trimSpaces(address1))) {
        NMAddress shippingAddress = null;
        final String payPalNickname = AddressNickname.createNickname(firstName, lastName, address1, city, state, zip, country);
        final NMAddress defaultAddress = profile.getShippingNMAddress();

        final String defaultNickname = defaultAddress != null ? defaultAddress.getAddressNickname() : null;
        boolean isDefaultAddress = false;
        boolean isOldShippingGroupAddress = false;

        final List<ShippingGroup> shippingGroups = order.getShippingGroups();
        for (final ShippingGroup shippingGroup : CheckoutAPI.getNonWishlistShippingGroups(shippingGroups)) {
          if (!StringUtilities.isEmpty(payPalNickname)) {
            isOldShippingGroupAddress = shippingGroup.getDescription().equalsIgnoreCase(payPalNickname);
          }
        }

        if (!StringUtilities.isEmpty(payPalNickname)) {
          if (payPalNickname.equalsIgnoreCase(defaultNickname)) {
            shippingAddress = defaultAddress;
            isDefaultAddress = true;
          } else {
            final MutableRepositoryItem secondaryAddress = profile.getSecondaryAddresses().remove(payPalNickname);

            if (secondaryAddress != null) {
              shippingAddress = new NMAddress(secondaryAddress);
            }
          }
        }

        if (shippingAddress == null) {
          final MutableRepositoryItem addressItem = profileRepo.createItem(ProfileProperties.Profile_Desc_contactInfo);
          shippingAddress = new NMAddress(addressItem);
        }

        if (!isDefaultAddress) {
          if (defaultAddress != null && !StringUtilities.isEmpty(defaultAddress.getAddress1())) {
            profile.getSecondaryAddresses().put(defaultAddress.getAddressNickname(), defaultAddress.getRepositoryItem());
          }

          profile.setShippingAddress(shippingAddress.getRepositoryItem());
        }

        shippingAddress.setFirstName(firstName);
        shippingAddress.setLastName(lastName);
        shippingAddress.setCountry(country);
        shippingAddress.setAddress1(address1);
        shippingAddress.setAddress2(address2);
        shippingAddress.setCity(city);
        setStateOrProvince(country, state, shippingAddress);
        shippingAddress.setPostalCode(zip);
        shippingAddress.setPhoneNumber(StringUtilities.isNotEmpty(phoneNum) ? phoneNum : response.getPhoneNum());
        shippingAddress.updateNickname();

        for (final ShippingGroup shippingGroup : CheckoutAPI.getNonWishlistShippingGroups(shippingGroups)) {
          if (CheckoutAPI.orderContainsShippingAddress(order)) {
            if (!isOldShippingGroupAddress) {
              final ContactInfo contactInfo = CheckoutAPI.profileAddressToContactInfo(shippingAddress.getRepositoryItem());
              CheckoutAPI.updateShippingGroupAddressPayPal(cart, profile, order, shippingGroup.getId(), contactInfo);
            }
          } else {
            CheckoutAPI.resetCommerceItemsToDefaultAddresses(profile, order, orderMgr);
          }
        }
      }
    }

    boolean newPaypalCard = false;
    NMCreditCard paypalCard = null;
    final NMCreditCard defaultCard = profile.getDefaultCreditCard();

    if (defaultCard == null || StringUtilities.isEmpty(defaultCard.getCreditCardType())) {
      paypalCard = defaultCard;
    }

    if (paypalCard == null) {
      paypalCard = profile.getPayPalByEmail(response.getEmail());
    }

    if (paypalCard == null) {
      newPaypalCard = true;

      final NMCommerceProfileTools profileTools = CheckoutComponents.getCommerceProfileTools();
      paypalCard = new NMCreditCard();
      paypalCard.setRepositoryItem(profileTools.createCreditCardItem(profile));
    }

    if (paypalCard.getBillingAddressItem() == null) {
      final RepositoryItem billingAddress = profileRepo.createItem(ProfileProperties.Profile_Desc_contactInfo);
      paypalCard.setPropertyValue(ProfileProperties.CreditCard_billingAddress, billingAddress);
    }

    paypalCard.setCreditCardType("PayPal");
    paypalCard.setPayPalEmail(response.getEmail().toLowerCase());
    paypalCard.setPayPalPayerId(response.getPayerId());

    final NMAddress billingAddress = new NMAddress((MutableRepositoryItem) paypalCard.getBillingAddressItem());
    final String existingPhoneNumber = billingAddress.getPhoneNumber();
    final String country = response.getCountryCode();

    final String fullName = response.getBillingName();
    if (fullName != null) {
      final String[] firstLast = fullName.split(" ");
      billingAddress.setFirstName(firstLast[0]);
      billingAddress.setLastName(firstLast[1]);
    }
    billingAddress.setCountry(country);
    billingAddress.setAddress1(response.getStreet());
    billingAddress.setAddress2(response.getStreet2());
    billingAddress.setCity(response.getCity());
    setStateOrProvince(country, response.getState(), billingAddress);
    billingAddress.setPostalCode(response.getZip());
    billingAddress.setPhoneNumber(StringUtilities.isEmpty(response.getPhoneNum()) ? existingPhoneNumber : response.getPhoneNum());

    profile.setBillingAddress(billingAddress.getRepositoryItem());

    if (newPaypalCard) {
      profile.addCreditCard(paypalCard);
    }

    profile.changeDefaultCreditCard(paypalCard);
    CheckoutAPI.changeCreditCardOnOrder(order, orderMgr, profile, paypalCard);

    return messages;
  }

  public boolean useBillingAgreement(final ShoppingCartHandler cart, final NMProfile profile) throws Exception {
    final Map<String, NMCreditCard> profileCards = profile.getCreditCardMap();
    final NMOrderImpl order = cart.getNMOrder();
    final NMOrderManager orderMgr = (NMOrderManager) cart.getOrderHolder().getOrderManager();

    for (final NMCreditCard profileCard : profileCards.values()) {
      if (profileCard.isPayPal() && StringUtilities.isNotEmpty(profileCard.getPayPalBillingAgreementId())) {
        profile.changeDefaultCreditCard(profileCard);
        CheckoutAPI.changeCreditCardOnOrder(order, orderMgr, profile, profileCard);

        return true;
      }
    }

    return false;
  }

  public String doExpressCheckout(final ShoppingCartHandler cart, final NMProfile profile, final String rootUrl, final String token) throws Exception {
    String paypalOrderId = null;
    final NMOrderImpl order = cart.getNMOrder();
    final Collection<PayPalParams> paymentRequests = buildPaymentRequests(order, profile, rootUrl);
    final NMCreditCard paymentGroup = order.getCreditCards().iterator().next();

    final DoExpressCheckoutRequest request =
            new DoExpressCheckoutRequest.Builder(payPal.getVersion(), payPal.getUsername(), payPal.getPassword(), payPal.getMerchantEmail(), token, paymentGroup.getPayPalPayerId()).paymentRequests(
                    paymentRequests).build();

    final DoExpressCheckoutResponse response = payPal.doExpressCheckout(request);

    checkTokenExpired(response);
    checkFailure(response);
    checkPayPalInvalidAddress(response);

    for (final PaymentInfo paymentInfo : response.getPaymentInfo()) {
      paypalOrderId = paymentInfo.getTransactionId();
    }

    paymentGroup.setCreditCardNumber(paypalOrderId);
    paymentGroup.setPayPalBillingAgreementId(response.getBillingAgreementId());

    CheckoutAPI.repriceOrder(cart, profile, -1);

    return paypalOrderId;
  }

  public String doReferenceTransaction(final ShoppingCartHandler cart) throws Exception {
    final SystemSpecs systemSpecs = CommonComponentHelper.getSystemSpecs();
    final NMOrderImpl order = cart.getNMOrder();
    final String orderId = systemSpecs.getProductionSystemCode() + order.getId();
    final OrderPriceInfo priceInfo = order.getPriceInfo();
    final String currency = priceInfo.getCurrencyCode();
    final NMOrderManager orderMgr = (NMOrderManager) cart.getOrderManager();
    final DecimalFormat df = new DecimalFormat("#0.00");
    final String authorizationAmount = df.format(orderMgr.getCurrentOrderAmountRemaining(order));
    final String authorizationNetAmount = authorizationAmount;
    final String authorizationTaxAmount = "";
    final String authorizationCharge = "";

    final String cardNumber = order.getCreditCards().iterator().next().getPayPalBillingAgreementId();
    final NMRepositoryContactInfo shippingAddress = getShippingAddressFromOrder(order);
    final PayPalAddressEsbDTO address = mapShippingAddressToEsbDTO(shippingAddress);
    final DoReferenceTransactionResponse response =
            serviceHandler.doReferenceTransaction(new DoReferenceTransactionRequest(orderId, getTransactionDate(), cardNumber, getSourceTransactionId(order.getId()), authorizationAmount,
                    authorizationNetAmount, currency, address, authorizationTaxAmount, authorizationCharge));

    if (response == null) {
      return null;
    }

    return response.getAuthorizationId();
  }

  public String doAuthorizeTransaction(final ShoppingCartHandler cart, final String payPalOrderId) throws Exception {
    final SystemSpecs systemSpecs = CommonComponentHelper.getSystemSpecs();
    final NMOrderImpl order = cart.getNMOrder();
    final String orderId = systemSpecs.getProductionSystemCode() + order.getId();
    final OrderPriceInfo priceInfo = order.getPriceInfo();
    final String currency = priceInfo.getCurrencyCode();
    final NMOrderManager orderMgr = (NMOrderManager) cart.getOrderManager();
    final DecimalFormat df = new DecimalFormat("#0.00");
    final String authorizationAmount = df.format(orderMgr.getCurrentOrderAmountRemaining(order));
    final String authorizationTaxAmount = "";
    final String authorizationNetAmount = authorizationAmount;
    final String authorizationCharge = "";

    final DoAuthorizeTransactionResponse response =
            serviceHandler.doAuthorizeTransaction(new DoAuthorizeTransactionRequest(orderId, getTransactionDate(), payPalOrderId, getSourceTransactionId(order.getId()), authorizationAmount,
                    authorizationNetAmount, currency, authorizationTaxAmount, authorizationCharge));

    if (response == null) {
    	return null;
    } else if(!StringUtils.isEmpty(payPalOrderId)) {
    	checkPaypalFundingFailure(response, payPalOrderId);
    }

    return response.getAuthorizationId();
  }

  public Collection<Message> handlePayPalCheckout(final ShoppingCartHandler cart, final NMProfile profile, final String rootUrl, final String token) throws Exception {
    final List<Message> messages = new ArrayList<Message>();
    final NMOrderManager orderMgr = (NMOrderManager) cart.getOrderManager();
    final NMCreditCard paymentGroup = cart.getNMOrder().getCreditCards().iterator().next();
    final NMOrderImpl order = cart.getNMOrder();
    String payPalOrderId;
    String payPalAuthId;

    if (order.isPPRestrictedOrder()) {
      final Message msg = new Message();
      msg.setMsgText("One or more item(s) in your shopping bag cannot be purchased with PayPal. Please choose another form of payment or edit your shopping bag.");
      msg.setError(true);
      messages.add(msg);
    } else {
      
      if (StringUtilities.isEmpty(paymentGroup.getPayPalBillingAgreementId()) || profile.isAnonymous()) {
        payPalOrderId = doExpressCheckout(cart, profile, rootUrl, token);
      } else {
        payPalOrderId = doReferenceTransaction(cart);
      }
      
      if (StringUtilities.isEmpty(payPalOrderId)) {
        final Message msg = new Message();
        msg.setMsgText("Could not submit order with PayPal at this time.");
        msg.setFieldId("groupTop");
        messages.add(msg);
      } else {
        
        if (order.isPPRestrictedOrder()) {
          final Message msg = new Message();
          msg.setMsgText("One or more item(s) in your shopping bag cannot be purchased with PayPal. Please choose another form of payment or edit your shopping bag.");
          msg.setError(true);
          messages.add(msg);
        } else {
          
          payPalAuthId = doAuthorizeTransaction(cart, payPalOrderId);
          
          if (StringUtilities.isEmpty(payPalAuthId)) {
            final Message msg = new Message();
            msg.setMsgText("Could not authorize order with PayPal at this time.");
            msg.setFieldId("groupTop");
            messages.add(msg);
          } else {
            
            order.setPayPalOrderId(payPalOrderId);
            order.setPayPalAuthorizationId(payPalAuthId);
            orderMgr.updateOrder(order);
          }
        }
      }
    }
    return messages;
  }

  private Collection<PayPalParams> buildPaymentRequests(final NMOrderImpl order, final NMProfile profile, final String rootUrl) {
    return buildPaymentRequests(order, profile, rootUrl, true);
  }

  private Collection<PayPalParams> buildPaymentRequests(final NMOrderImpl order, final NMProfile profile, final String rootUrl, final boolean sendWishlistAddress) {
    final NMOrderPriceInfo orderPriceInfo = (NMOrderPriceInfo) order.getPriceInfo();
    NMRepositoryContactInfo shippingAddress = null;
    double highestItemTotal = 0;

    final List<PayPalParams> paymentRequests = new ArrayList<PayPalParams>();
    final List<PayPalParams> paymentRequestItems = new ArrayList<PayPalParams>();

    for (final HardgoodShippingGroup group : OrderUtil.getInstance().getHardgoodShippingGroups(order)) {
      final List<NMCommerceItem> items = ShippingGroupUtil.getInstance().getItems(group);
      double itemTotal = 0;

      for (final NMCommerceItem item : items) {
        final NMProduct product = item.getProduct();
        String name;
        final String url = joinUrl(rootUrl, product.getCanonicalUrl());
        final double amt = item.getCurrentItemPrice() / item.getQuantity();

        if (amt == 0.0) {
          continue;
        }

        itemTotal += item.getCurrentItemPrice();

        final String webDesigner = product.getWebDesignerName();
        if (!StringUtilities.isEmpty(webDesigner) && !vendorSpecs.isHideAllBrands() && !vendorSpecs.getBrandOnly().equals(webDesigner)) {
          name = String.format("%s %s", webDesigner, product.getDisplayName());
        } else {
          name = product.getDisplayName();
        }

        paymentRequestItems.add(new PaymentRequestItem.Builder().name(name).amt(amt).number(String.format("%s_%s", product.getCmosCatalogId(), product.getCmosItemCode())).qty(item.getQuantity())
                .itemUrl(url).itemCategory(prodSkuUtil.isVirtualGiftCard(product.getDataSource()) ? "Digital" : "Physical").build());
      }

      if (itemTotal > highestItemTotal) {
        final NMRepositoryContactInfo address = ShippingGroupUtil.getInstance().getAddress(group);
        if (sendWishlistAddress || !address.getFlgGiftReg()) {
          highestItemTotal = itemTotal;
          shippingAddress = address;
        }
      }
    }

    final Nucleus globalNucleus = Nucleus.getGlobalNucleus();
    final NMOrderManager orderManager = (NMOrderManager) globalNucleus.resolveName("/atg/commerce/order/OrderManager");
    final double remainingAmout = orderManager.getCurrentOrderAmountRemaining(order);
    final double orderTotalAmt = orderPriceInfo.getTotal();
    double authorizeAmt = orderTotalAmt;
    final boolean isPartialPayment = remainingAmout != orderTotalAmt;
    if (isPartialPayment) {
      authorizeAmt = remainingAmout;
      paymentRequestItems.add(new PaymentRequestItem.Builder().name("Payment using GiftCard").amt((orderTotalAmt - remainingAmout) * -1).qty(1).itemCategory("Physical").build());

    }
    if (orderTotalAmt - orderPriceInfo.getAmount() > 0) {
      paymentRequestItems.add(new PaymentRequestItem.Builder().name("Tax and Shipping and other charges").amt(orderTotalAmt - orderPriceInfo.getAmount()).qty(1).itemCategory("Physical").build());
    }

    final PaymentRequest.Builder paymentRequest =
            new PaymentRequest.Builder(authorizeAmt).paymentReason("None").currencyCode(orderPriceInfo.getCurrencyCode()).itemAmt(authorizeAmt).shippingAmt(0.0).taxAmt(0.0)
            .invNum(StringUtilities.emptyIfNull(systemSpecs.getProductionSystemCode()) + order.getId()).paymentAction("Order").paymentRequestItems(paymentRequestItems);

    final boolean hasShipping = shippingAddress != null && !StringUtilities.isEmpty(shippingAddress.getAddress1());

    if (hasShipping) {
      String firstName = shippingAddress.getFirstName();
      // check to see if shipping address is ship to store, if so then prepend string "S2S" this was done to prevent PayPal from invalidating ship to store orders
      if (!StringUtilities.isEmpty(shippingAddress.getShipToStoreNumber())) {
        firstName = "S2S " + firstName;
      }
      paymentRequest.shipToName(String.format("%s %s", firstName, shippingAddress.getLastName())).shipToCountryCode(shippingAddress.getCountry()).shipToStreet(shippingAddress.getAddress1())
      .shipToStreet2(shippingAddress.getAddress2()).shipToCity(shippingAddress.getCity()).shipToState(shippingAddress.getState()).shipToZip(shippingAddress.getPostalCode())
      .shipToPhoneNum(shippingAddress.getPhoneNumber());
    }

    paymentRequests.add(paymentRequest.build());

    return paymentRequests;
  }

  private String getTransactionDate() {
    final SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy");
    return format.format(new Date());
  }

  private String getSourceTransactionId(final String orderId) {
    final StringBuilder builder = new StringBuilder();

    final SystemSpecs systemSpecs = (SystemSpecs) Nucleus.getGlobalNucleus().resolveName("/nm/utils/SystemSpecs");
    builder.append(systemSpecs.getProductionSystemCode());

    builder.append(orderId);

    final SimpleDateFormat format = new SimpleDateFormat("ddMMyyyyHHmmss");
    builder.append(format.format(new Date()));

    return builder.toString();
  }

  private String joinUrl(String host, String path) {
    if (host == null || path == null) {
      return null;
    }

    host = host.replaceAll("/[\\s]*$", "");
    if (!path.startsWith("/")) {
      path = "/" + path;
    }

    return host + path;
  }

  private void checkTokenExpired(final PayPalResponseParams response) throws PayPalTokenExpiredException {
    if (response.isFailure()) {
      for (final PayPalError error : response.getErrors()) {
        if (TOKEN_EXPIRED_CODE.equals(error.getErrorCode())) {
          throw new PayPalTokenExpiredException(error.getLongMessage());
        }
      }
    }
  }

  private void checkPayPalInvalidAddress(final PayPalResponseParams response) throws PayPalInvalidAddressException {
    if (response.isFailure()) {
      for (final PayPalError error : response.getErrors()) {
        if (ILLEGAL_ADDRESS_CODE.equals(error.getErrorCode())) {
          throw new PayPalInvalidAddressException(error.getLongMessage());
        }
      }
    }
  }
  /**
   * Below method checks for the failure occurred due to low funds during PayPal checkout 
   * @param response
   * @param payPalOrderId
   * @throws PayPalFundingFailureException
   */
  private void checkPaypalFundingFailure(final DoAuthorizeTransactionResponse response, String payPalOrderId) throws PayPalFundingFailureException {
	  if(response.getStatus().equals(FAILED_STATUS)) {
		  if(response.getStatusReasonDescription().contains(FUNDING_FAILURE_CODE)){
			  throw new PayPalFundingFailureException(payPalOrderId);
		  }
	  }
  }

  private void checkFailure(final PayPalResponseParams response) throws PayPalException {
    if (response.isFailure()) {
      final Map<String, String> errors = new HashMap<String, String>();

      for (final PayPalError error : response.getErrors()) {
        errors.put(error.getErrorCode(), error.getLongMessage());
      }

      throw new PayPalException(errors.toString());
    }
  }

  private void setStateOrProvince(final String country, final String stateOrProvince, final NMAddress address) {
    if ("US".equalsIgnoreCase(country)) {
      address.setState(stateOrProvince);
      address.setProvince(null);
    } else {
      address.setState(null);
      address.setProvince(stateOrProvince);
    }
  }

  private NMRepositoryContactInfo getShippingAddressFromOrder(final NMOrderImpl order) {
    NMRepositoryContactInfo address = null;
    for (final HardgoodShippingGroup group : OrderUtil.getInstance().getHardgoodShippingGroups(order)) {
      address = ShippingGroupUtil.getInstance().getAddress(group);
      if (!StringUtilities.isEmpty(address.getShipToStoreNumber())) {
        // shipping to a store
        break;
      }
    }
    return address;
  }

  private PayPalAddressEsbDTO mapShippingAddressToEsbDTO(final NMRepositoryContactInfo shippingAddress) {
    final PayPalAddressEsbDTO addressDTO = new PayPalAddressEsbDTO();
    String firstName = shippingAddress.getFirstName();
    // check to see if shipping address is ship to store, if so then prepend string "S2S" this was done to prevent PayPal from invalidating ship to store orders
    if (!StringUtilities.isEmpty(shippingAddress.getShipToStoreNumber())) {
      firstName = "S2S " + firstName;
    }
    addressDTO.setFirstName(firstName);
    addressDTO.setLastName(shippingAddress.getLastName());
    addressDTO.setAddressLine1(shippingAddress.getAddress1());
    if (shippingAddress.getAddress2() == null) {
      // set default
      addressDTO.setAddressLine2("");
    } else {
      addressDTO.setAddressLine2(shippingAddress.getAddress2());
    }
    addressDTO.setCity(shippingAddress.getCity());
    addressDTO.setState(shippingAddress.getState());
    addressDTO.setPostalCode1(shippingAddress.getPostalCode());
    addressDTO.setCountry(shippingAddress.getCountry());
    String phoneNumber = shippingAddress.getPhoneNumber();
    if (StringUtils.isEmpty(shippingAddress.getPhoneNumber())) {
      phoneNumber = systemSpecs.getShipToStoreContactPhone();
    }
    addressDTO.setPhoneNumber1(phoneNumber);
    return addressDTO;
  }

  // private constructor enforces singleton behavior
  private PayPalUtil() {
    payPal = CheckoutComponents.getPayPal();
    serviceHandler = CheckoutComponents.getPayPalWebServiceHandler();
    systemSpecs = CommonComponentHelper.getSystemSpecs();
    brandSpecs = (BrandSpecs) Nucleus.getGlobalNucleus().resolveName("/nm/utils/BrandSpecs");
    vendorSpecs = (VendorSpecs) Nucleus.getGlobalNucleus().resolveName("/nm/utils/VendorSpecs");
    prodSkuUtil = CommonComponentHelper.getProdSkuUtil();
  }

  private static final String TOKEN_EXPIRED_CODE = "10411";
  private static final String ILLEGAL_ADDRESS_CODE = "10736";
  private static final String FUNDING_FAILURE_CODE = "10486";
  private static final String FAILED_STATUS= "Failed";
  private static PayPalUtil INSTANCE;
  private final PayPal payPal;
  private final PayPalWebServiceHandler serviceHandler;
  private final SystemSpecs systemSpecs;
  private final BrandSpecs brandSpecs;
  private final VendorSpecs vendorSpecs;
  private final ProdSkuUtil prodSkuUtil;

}
