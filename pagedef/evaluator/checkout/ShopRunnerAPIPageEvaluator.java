package com.nm.commerce.pagedef.evaluator.checkout;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.PageContext;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import atg.commerce.order.OrderManager;
import atg.commerce.order.ShippingGroup;
import atg.commerce.order.ShippingGroupCommerceItemRelationship;
import atg.commerce.pricing.OrderPriceInfo;
import atg.core.util.StringUtils;
import atg.nucleus.GenericService;
import atg.repository.RepositoryItem;
import atg.servlet.DynamoHttpServletRequest;
import atg.servlet.DynamoHttpServletResponse;
import atg.userprofiling.CookieManager;
import com.neimanmarcus.service.esb.web_services.GetShoprunnerPaymentInfoServiceStub.GetShoprunnerPaymentInfoResponse;
import com.nm.ajax.checkout.utils.ComponentUtils;
import com.nm.collections.ServiceLevel;
import com.nm.commerce.GiftCard;
import com.nm.commerce.NMCommerceItem;
import com.nm.commerce.NMCreditCard;
import com.nm.commerce.NMProfile;
import com.nm.commerce.NMRepositoryContactInfo;
import com.nm.commerce.ShopRunnerGiftCard;
import com.nm.commerce.catalog.NMProduct;
import com.nm.commerce.checkout.CheckoutAPI;
import com.nm.commerce.checkout.CheckoutComponents;
import com.nm.commerce.checkout.SnapshotUtil;
import com.nm.commerce.checkout.beans.ContactInfo;
import com.nm.commerce.checkout.beans.LoginBean;
import com.nm.commerce.checkout.beans.OrderInfo;
import com.nm.commerce.checkout.beans.PaymentInfo;
import com.nm.commerce.checkout.beans.PromoBean;
import com.nm.commerce.checkout.beans.ResultBean;
import com.nm.commerce.checkout.beans.ShopRunnerResponse;
import com.nm.commerce.order.NMOrderManager;
import com.nm.commerce.pagedef.definition.PageDefinition;
import com.nm.commerce.pagedef.model.CheckoutPageModel;
import com.nm.commerce.promotion.NMOrderImpl;
import com.nm.components.CommonComponentHelper;
import com.nm.droplet.LinkshareCookie;
import com.nm.formhandler.NMProfileFormHandler;
import com.nm.formhandler.ShoppingCartHandler;
import com.nm.integration.ShopRunnerConstants;
import com.nm.profile.AddressVerificationContainer;
import com.nm.profile.AddressVerificationData;
import com.nm.profile.ProfileProperties;
import com.nm.sitemessages.AddressMessageFields;
import com.nm.sitemessages.Message;
import com.nm.sitemessages.MessageDefs;
import com.nm.sitemessages.StockLevelMessage;
import com.nm.utils.BrandSpecs;
import com.nm.utils.PCSSessionData;
import com.nm.utils.StringUtilities;
import com.nm.utils.bing.BingSessionData;

public class ShopRunnerAPIPageEvaluator extends BaseCheckoutPageEvaluator {
  
  private String dropShipPh = "";
  
  @Override
  public boolean evaluate(final PageDefinition pageDefinition, final PageContext pageContext) throws Exception {
    
    if (!super.evaluate(pageDefinition, pageContext)) {
      return false;
    }
    
    final DynamoHttpServletRequest request = getRequest();
    final DynamoHttpServletResponse response = getResponse();
    final NMProfile profile = CheckoutComponents.getProfile(request);
    
    getLogger().logDebug("CheckoutAPI.isShopRunnerSessionValid :: " + CheckoutAPI.isShopRunnerSessionValid(request));
    
    final String method = request.getParameter("method");
    
    final ShoppingCartHandler cartHandler = CheckoutComponents.getCartHandler(request);
    final NMOrderImpl order = cartHandler.getNMOrder();
    if (order.isVisaCheckoutPayGroupOnOrder()) {
      CheckoutAPI.clearVmeDetails(order);
    } else if (order.isMasterPassPayGroupOnOrder()) {
      CheckoutAPI.clearMasterPassDetails(order);
    }
    if (!CheckoutAPI.isShopRunnerSessionValid(request)) {
      CheckoutAPI.deleteShopRunnerToken(request, response);
      buildShopRunnerErrorResponse(ShopRunnerConstants.SR_STATUS_42_SESSION_TIMEOUT);
    } else if (method.equals(ShopRunnerConstants.START_PR_CHECKOUT)) {
      CheckoutAPI.setShopRunnerSL(true);
      startPRCheckout(profile, request, response);
    } else if (method.equals(ShopRunnerConstants.UPDATE_PR_CHECKOUT)) {
      updatePRCart(profile, request);
    } else if (method.equals(ShopRunnerConstants.APPLY_PR_GIFTCARD)) {
      applyPRGiftCard(request);
    } else if (method.equals(ShopRunnerConstants.PROCESS_PR_ORDER)) {
      processPROrder(profile, request, response);
    } else if (method.equals(ShopRunnerConstants.ABANDON_PR_CART)) {
      abandonPRCart(profile, request);
    } else if (method.equals(ShopRunnerConstants.REVIEW_ORDER)) {
      reviewOrder(profile, request);
    }
    return true;
  }
  
  /**
   * Start ShopRunner checkout- share cart information
   * 
   * @param profile
   * @param request
   * @param response
   * @throws Exception
   */
	private void startPRCheckout(final NMProfile profile, final DynamoHttpServletRequest request, final DynamoHttpServletResponse response) throws Exception {
    final String[] jsonStringsCart = request.getParameterValues("cart");
    logExtraDebugInfo("---ShopRunnerAPIPageEvaluator:startPRCheckout");
    if (jsonStringsCart != null) {
      try {
        int statusCode = 0;
        final ShoppingCartHandler cartHandler = CheckoutComponents.getCartHandler(request);
        NMOrderImpl order = cartHandler.getNMOrder();
        final OrderManager orderMgr = cartHandler.getOrderManager();
        final JSONParser parser = new JSONParser();
        String address1="";
        final Object cart = parser.parse(jsonStringsCart[0]);
        final JSONObject cartObject = (JSONObject) cart;
        JSONObject addressObj = (JSONObject) cartObject.get("address");
				if (addressObj != null) {
					address1 = (String) addressObj.get("address1");
				} else {
					logExtraDebugInfo("Address is empty for the cartObject"+cartObject);
        }
        String addrRestrictionErrorMessage = "";
        if (StringUtilities.isNotEmpty(address1)) {
          final ContactInfo addr = CheckoutAPI.processShopRunnerProfileData(profile, addressObj, request);
          CheckoutAPI.updateAllItemServiceLevels(order, ServiceLevel.SL2_SERVICE_LEVEL_TYPE);
          CheckoutAPI.resetCommerceItemsToShopRunnerShippingAddress(profile, addr, order, orderMgr);
          
          // temporarily set the nmOrder.isShopRunner to true, so that the shipping calculation knows to calculate shipping for shoprunner
          final boolean wasShopRunner = order.isShopRunner();
          order.setShopRunner(true);
          
          CheckoutAPI.repriceOrder(cartHandler, profile, SnapshotUtil.CHANGE_SHIP_DESTINATION);
          final Collection<Message> messages = CheckoutAPI.validateOrderShipping(cartHandler, order, profile);
          if (!messages.isEmpty()) {
            addrRestrictionErrorMessage = getShippingRestrictionErrorMessages(messages);
            if (addrRestrictionErrorMessage != null) {
              statusCode = ShopRunnerConstants.SR_STATUS_8_ADDR_RESTRICTION;
              buildShopRunnerWithError(profile, statusCode, addrRestrictionErrorMessage, ShopRunnerConstants.URL_CART, cartHandler);
              return;
            }
          }
        }
        buildShopRunner(profile, statusCode, cartHandler);
      } catch (final ParseException pe) {
        getLogger().logError("Exception in startPRCheckout: " + pe.getMessage());
        throw pe;
      }
    }
  }
  
  /**
   * Build ShopRunner response
   * 
   * @param profile
   * @param statusCode
   * @param request
   * @throws Exception
   */
  @SuppressWarnings({"unchecked"})
  private void buildShopRunner(final NMProfile profile, final int statusCode, final ShoppingCartHandler cartHandler) throws Exception {
    // final ShoppingCartHandler cartHandler = CheckoutComponents.getCartHandler(request);
    final NMOrderImpl order = cartHandler.getNMOrder();
    final CheckoutPageModel pageModel = (CheckoutPageModel) getPageModel();
    
    if (order != null) {
      pageModel.setHasDeliveryPhoneItems(!CheckoutAPI.getDeliveryPhoneItems(order, cartHandler.getOrderManager()).isEmpty());
    }
    final NMOrderManager orderMgr = (NMOrderManager) cartHandler.getOrderManager();
    final List<ShippingGroupCommerceItemRelationship> commerceItemRelationships = CheckoutAPI.getShippingGroupCommerceItemRelationships(order, orderMgr);
    pageModel.setCommerceItemRelationships(commerceItemRelationships);
    pageModel.setOrder(order);
    
    final JSONObject cart = CheckoutAPI.buildCart(pageModel, order, orderMgr, dropShipPh);
    // Gift cards from order
    final JSONArray giftCardsArray = CheckoutAPI.buildGiftCards(order);
    cart.put("giftCards", giftCardsArray);
    
    // Order level promotions
    final JSONArray promotions = CheckoutAPI.buildPromotions(order);
    cart.put("promotions", promotions);
    
    final JSONObject cartWrapper = new JSONObject();
    cartWrapper.put("cart", cart);
    cartWrapper.put("status", statusCode);
    
    final ShopRunnerResponse shopRunner = new ShopRunnerResponse();
    shopRunner.setResponse(cartWrapper);
    pageModel.setShopRunner(shopRunner);
  }
  
  /**
   * Update ShopRunner cart
   * 
   * @throws Exception
   */
  @SuppressWarnings("unused")
  private void updatePRCart(final NMProfile profile, final DynamoHttpServletRequest request) throws Exception {
    logExtraDebugInfo("---ShopRunnerAPIPageEvaluator:updatePRCart");
    final String[] jsonStringsCart = request.getParameterValues("cart");
    final ShoppingCartHandler cartHandler = CheckoutComponents.getCartHandler(request);
    int statusCode = ShopRunnerConstants.SR_STATUS_0_DEFAULT;
    String errorMessage = "";
    String brandName = "";
    final BrandSpecs brandSpecs = (BrandSpecs) request.resolveName("/nm/utils/BrandSpecs");
    brandName = brandSpecs.getBrandName();
    
    final NMOrderImpl order = cartHandler.getNMOrder();
    final CheckoutPageModel pageModel = (CheckoutPageModel) getPageModel();
    final NMOrderManager orderMgr = (NMOrderManager) cartHandler.getOrderManager();
    
    final List<ShippingGroupCommerceItemRelationship> commerceItemRelationships = CheckoutAPI.getShippingGroupCommerceItemRelationships(order, orderMgr);
    pageModel.setCommerceItemRelationships(commerceItemRelationships);
    pageModel.setOrder(order);
    final List<ShippingGroupCommerceItemRelationship> items = pageModel.getCommerceItemRelationships();
    try {
      final JSONParser parser = new JSONParser();
      final Object cart = parser.parse(jsonStringsCart[0]);
      final JSONObject cartObject = (JSONObject) cart;
      final JSONObject errorObject = null;
      
      final JSONArray shippingGroups = (JSONArray) cartObject.get("shippingGroups");
      final JSONArray products = (JSONArray) cartObject.get("products");
      dropShipPh = (String) cartObject.get("dropship_ph");
      
      if (shippingGroups.size() > 0) {
        for (int i = 0; i < shippingGroups.size(); i++) {
          final JSONObject shippingGroup = (JSONObject) shippingGroups.get(i);
          final JSONArray groups = (JSONArray) shippingGroup.get("shipping");
          final Long shippingGroupIndex = (Long) shippingGroup.get("shippingGroup");
          for (int j = 0; j < groups.size(); j++) {
            final JSONObject group = (JSONObject) groups.get(j);
            if ((Boolean) group.get("selected") == Boolean.TRUE) {
              for (int k = 0; k < products.size(); k++) {
                final JSONObject product = (JSONObject) products.get(k);
                if ((Long) product.get("shippingGroup") == shippingGroupIndex) {
                  final String sku = (String) product.get("sku");
                  final String method = (String) group.get("method");
                  for (int l = 0; l < items.size(); l++) {
                    final ShippingGroupCommerceItemRelationship commerceItem = items.get(l);
                    final NMCommerceItem item = (NMCommerceItem) commerceItem.getCommerceItem();
                    final NMProduct p = item.getProduct();
                    if (sku.equals(item.getCatalogRefId())) {
                      if (!item.getServicelevel().equals(method)) {
                        item.setServicelevel(method);
                      }
                    }
                  }
                }
              }
            }
          }
        }
        orderMgr.updateOrder(order);
        final Collection<Message> shippingMessages = CheckoutAPI.repriceOrder(cartHandler, profile, SnapshotUtil.EDIT_ITEM_FROM_REGULAR_CART);
      }
      // get shipping address
      final JSONObject addressObject = (JSONObject) cartObject.get("address");
      final String address1 = (String) addressObject.get("address1");
      if (StringUtilities.isNotEmpty(address1)) {
        final ContactInfo shippingAddress = CheckoutAPI.processShopRunnerProfileData(profile, addressObject, request);
        CheckoutAPI.resetCommerceItemsToShopRunnerShippingAddress(profile, shippingAddress, order, orderMgr);
        CheckoutAPI.repriceOrder(cartHandler, profile, SnapshotUtil.CHANGE_SHIP_DESTINATION);
        final Collection<Message> messages = CheckoutAPI.validateOrderShipping(cartHandler, order, profile);
        if (!messages.isEmpty()) {
          errorMessage = getShippingRestrictionErrorMessages(messages);
          if (errorMessage != null) {
            statusCode = ShopRunnerConstants.SR_STATUS_8_ADDR_RESTRICTION;
          }
        }
        // apply promo code if any from ShopRunner request
        final JSONArray promotions = (JSONArray) cartObject.get("promotions");
        if (promotions.size() > 0) {
          final JSONObject promo = (JSONObject) promotions.get(promotions.size() - 1);
          String promoCode = (String) promo.get("code");
          if (promoCode != null) {
            promoCode = promoCode.trim();
          }
          final PromoBean promoBean = new PromoBean();
          final int validationRequiredPromoType = CheckoutAPI.getActionRequiredPromoType(promoCode);
          // set the promo code
          promoBean.setPromoCode(promoCode);
          final String activatedPromoCode = cartHandler.getNMOrder().getActivatedPromoCode();
          final boolean isPromoAlreadyApplied = CheckoutAPI.isPromoAlreadyApplied(activatedPromoCode, promoCode);
          if (validationRequiredPromoType == ShopRunnerConstants.EMAIL_VALIDATION_REQ_PROMO) {
            if (!isPromoAlreadyApplied) {
              getLogger().logError("Email Validation promotion " + promoCode);
              errorMessage = "This Promo code " + promoCode + " must be applied on the " + brandName + " site.";
              statusCode = ShopRunnerConstants.SR_STATUS_43_VALIDATE_PROMO;
            }
          } else {
            // apply the promo code
            final ResultBean resultBean = CheckoutAPI.applyPromoCode(promoBean, cartHandler, profile);
            final Collection<Message> promoMessages = CheckoutAPI.repriceOrder(cartHandler, profile, SnapshotUtil.EDIT_ITEM_FROM_REGULAR_CART);
            updatePromotionFields(cartHandler);
            if (!resultBean.getMessages().isEmpty()) {
              final Message promoMessage = resultBean.getMessages().get(0);
              if (StringUtilities.isNotNull(promoMessage.getMessageText()) && (isPromoAlreadyApplied || isCustomMessagePromoError(promoMessage))) {
                statusCode = ShopRunnerConstants.SR_STATUS_111_CUSTOM;
                errorMessage = promoMessage.getMessageText();
                getLogger().logError("---ShopRunnerAPIPageEvaluator:updatePRCart line 308:promotionMessages:setting 111 for msg " + (errorMessage == null ? "null" : errorMessage));
              } else {
                if ((validationRequiredPromoType == ShopRunnerConstants.GIFTWRAP_SELECTION_REQ_PROMO) && !order.hasGiftwrapping()) {
                  getLogger().logError("Gift Wrap promotion " + promoCode);
                  errorMessage = CheckoutComponents.getShopRunner().getErrorPromoAddMessage() + promoCode + ".";
                  statusCode = ShopRunnerConstants.SR_STATUS_43_VALIDATE_PROMO;
                }
              }
            } else {
              if (!isPromoAlreadyApplied && (validationRequiredPromoType == ShopRunnerConstants.GWP_SKU_SELECTION_REQ_PROMO)) {
                getLogger().logError("GWP with sku select promotion " + promoCode);
                errorMessage = CheckoutComponents.getShopRunner().getErrorFreeGiftMessage();
                statusCode = ShopRunnerConstants.SR_STATUS_43_VALIDATE_PROMO;
              }
            }
          }
          
        }
      } else {
        errorMessage = CheckoutComponents.getShopRunner().getErrorAddShippingAddress();
        statusCode = ShopRunnerConstants.SR_STATUS_111_CUSTOM;
        getLogger().logError("---ShopRunnerAPIPageEvaluator:updatePRCart line 329:ErrorAddShippingAddress:setting 111 for msg " + (errorMessage == null ? "null" : errorMessage));
      }
      if (statusCode == ShopRunnerConstants.SR_STATUS_0_DEFAULT) {
        buildShopRunner(profile, statusCode, cartHandler);
      }
      if (statusCode == ShopRunnerConstants.SR_STATUS_8_ADDR_RESTRICTION) {
        buildShopRunnerWithError(profile, statusCode, errorMessage, ShopRunnerConstants.URL_CART, cartHandler);
        
      }
      if (statusCode == ShopRunnerConstants.SR_STATUS_43_VALIDATE_PROMO) {
        buildPromoValidationShopRunner(statusCode, errorMessage);
      }
      if (statusCode == ShopRunnerConstants.SR_STATUS_111_CUSTOM) {
    	getLogger().logError("---ShopRunnerAPIPageEvaluator:updatePRCart line 342:setting 111 for msg " + (errorMessage == null ? "null" : errorMessage));
        buildShopRunnerCustomErrorResponse(errorMessage);
      }
    } catch (final Exception e) {
      getLogger().logError("Exception in updatePRCart: " + e.getMessage());
    }
  }
  
  /**
   * Apply gift card to cart
   * 
   * @throws Exception
   */
  private void applyPRGiftCard(final DynamoHttpServletRequest request) throws Exception {
    final String[] jsonStringGiftCards = request.getParameterValues("gc");
    if (jsonStringGiftCards != null) {
      try {
        final JSONParser parser = new JSONParser();
        final Object genericGiftCardObject = parser.parse(jsonStringGiftCards[0]);
        final JSONObject giftCardWrapperObject = (JSONObject) genericGiftCardObject;
        final JSONArray giftCardJSONArray = (JSONArray) giftCardWrapperObject.get("giftCards");
        final JSONObject giftCardJSONObject = (JSONObject) giftCardJSONArray.get(0);
        applyGiftCards(giftCardJSONObject, request);
      } catch (final ParseException pe) {
        getLogger().logDebug("Parse Exception in applyPRGiftCard: " + pe.getMessage());
        throw pe;
      }
    }
  }
  
  /**
   * Apply gift card to cart
   * 
   * @param giftCardJSONObject
   * @param request
   */
  private void applyGiftCards(final JSONObject giftCardJSONObject, final DynamoHttpServletRequest request) {
    final ShopRunnerGiftCard shopRunnerGiftCard = buildShopRunnerGiftCard(giftCardJSONObject, request);
    buildShopRunnerGiftCardResponse(shopRunnerGiftCard);
  }
  
  /**
   * Build ShopRunnerGiftCard object
   * 
   * @param giftCardObject
   * @return
   */
  private ShopRunnerGiftCard buildShopRunnerGiftCard(final JSONObject giftCardObject, final DynamoHttpServletRequest request) {
    logExtraDebugInfo("---ShopRunnerAPIPageEvaluator.buildShopRunnerGiftCard");
    final ShoppingCartHandler cartHandler = CheckoutComponents.getCartHandler(request);
    final NMOrderImpl order = cartHandler.getNMOrder();
    final NMOrderManager orderMgr = (NMOrderManager) cartHandler.getOrderManager();
    
    int statusCode = ShopRunnerConstants.SR_STATUS_0_DEFAULT;
    String message = "";
    JSONObject messageObject = null;
    
    double gcBalance = CheckoutAPI.getGiftCardBalance(order);
    double newOrderBalance = orderMgr.getCurrentOrderAmountRemaining(order, false);
    
    NMCreditCard giftCard = null;
    final String number = giftCardObject.get("number").toString();
    final String cin = giftCardObject.get("code").toString();
    
    if (!CheckoutAPI.isGiftCardAlreadyApplied(order, number)) {
      try {
        final Collection<Message> giftCardMessages = CheckoutAPI.applyGiftCard(order, orderMgr, ComponentUtils.getInstance().getGiftCardHolder(), number, cin);
        if ((giftCardMessages != null) && !giftCardMessages.isEmpty()) {
          for (final Message msg : giftCardMessages) {
            statusCode = ShopRunnerConstants.SR_STATUS_111_CUSTOM;
            message = msg.getMsgText();
            getLogger().logError("---ShopRunnerAPIPageEvaluator:giftCardErrorMessages:setting 111 for msg " + (!StringUtils.isBlank(msg.getMsgText())? msg.getMsgText() : "null"));
          }
        } else {
          giftCard = CheckoutAPI.getGiftCard(order, number);
          if (giftCard != null) {
            if (newOrderBalance > giftCard.getAmountAvailable()) {
              gcBalance = giftCard.getAmountAvailable();
            } else {
              gcBalance = newOrderBalance;
            }
            newOrderBalance = orderMgr.getCurrentOrderAmountRemaining(order, false);
          } else {
            throw new Exception(String.format("gift card %s was neither invalid nor applied to order", number));
          }
        }
      } catch (final Exception e) {
        getLogger().logDebug("Error applying gift card to order: " + e.getMessage());
      }
    } else {
      statusCode = ShopRunnerConstants.SR_STATUS_111_CUSTOM;
      message = CheckoutComponents.getShopRunner().getErrorGiftCardAlreadyApplied();
      getLogger().logError("---ShopRunnerAPIPageEvaluator:ErrorGiftCardAlreadyApplied on order:setting 111 for msg :");
    }
    messageObject = buildSRMessage(statusCode, message);
    return new ShopRunnerGiftCard(number, cin, gcBalance, newOrderBalance, messageObject);
  }
  
  /**
   * Build ShopRunner message object
   * 
   * @param statusCode
   * @param text
   * @return
   */
  @SuppressWarnings("unchecked")
  private JSONObject buildSRMessage(final int statusCode, final String message) {
    final JSONObject messageObject = new JSONObject();
    messageObject.put("code", statusCode);
    messageObject.put("text", message);
    return messageObject;
  }
  
  @SuppressWarnings("unchecked")
  private void buildPromoValidationShopRunner(final int statusCode, final String errorMessage) throws Exception {
    final CheckoutPageModel pageModel = (CheckoutPageModel) getPageModel();
    final JSONObject cart = new JSONObject();
    cart.put("message", errorMessage);
    cart.put("redirectURL", ShopRunnerConstants.URL_CART);
    final JSONObject cartWrapper = new JSONObject();
    cartWrapper.put("cart", cart);
    cartWrapper.put("status", statusCode);
    final ShopRunnerResponse shopRunner = new ShopRunnerResponse();
    shopRunner.setResponse(cartWrapper);
    pageModel.setShopRunner(shopRunner);
    
  }
  
  /**
   * Build ShopRunner custom error response
   * 
   * @param errorMessage
   */
  @SuppressWarnings("unchecked")
  private void buildShopRunnerCustomErrorResponse(final String errorMessage) {
    logExtraDebugInfo("---ShopRunnerAPIPageEvaluator:buildShopRunnerCustomErrorResponse with msg " + (errorMessage == null ? "null" : errorMessage));
    if (StringUtilities.containsOnlyDigits(errorMessage)) {
      buildShopRunnerErrorResponse(new Integer(errorMessage));
    } else {
      getLogger().logError(errorMessage);
      final CheckoutPageModel pageModel = (CheckoutPageModel) getPageModel();
      final ShopRunnerResponse shopRunner = new ShopRunnerResponse();
      
      final JSONObject responseObject = new JSONObject();
      responseObject.put("status", ShopRunnerConstants.SR_STATUS_111_CUSTOM);
      responseObject.put("message", errorMessage);
      
      shopRunner.setResponse(responseObject);
      pageModel.setShopRunner(shopRunner);
    }
  }
  
  /**
   * Build ShopRunner custom error response
   * 
   * @param statusCode
   */
  @SuppressWarnings("unchecked")
  private void buildShopRunnerErrorResponse(final int statusCode) {
    logExtraDebugInfo("---ShopRunnerAPIPageEvaluator:buildShopRunnerErrorResponse with code " + statusCode);
    getLogger().logError("Shoprunner Error., status code=" + statusCode);
    final CheckoutPageModel pageModel = (CheckoutPageModel) getPageModel();
    final ShopRunnerResponse shopRunner = new ShopRunnerResponse();
    
    final JSONObject responseObject = new JSONObject();
    responseObject.put("status", statusCode);
    
    shopRunner.setResponse(responseObject);
    pageModel.setShopRunner(shopRunner);
  }
  
  /**
   * Build ShopRunner gift card response
   * 
   * @param shopRunnerGiftCard
   */
  @SuppressWarnings("unchecked")
  private void buildShopRunnerGiftCardResponse(final ShopRunnerGiftCard shopRunnerGiftCard) {
    final CheckoutPageModel pageModel = (CheckoutPageModel) getPageModel();
    final ShopRunnerResponse shopRunner = new ShopRunnerResponse();
    
    final int statusCode = (Integer) shopRunnerGiftCard.getMessageObject().get("code");
    final String errorMessage = (String) shopRunnerGiftCard.getMessageObject().get("text");
    
    final JSONObject giftCardWrapper = new JSONObject();
    giftCardWrapper.put("status", statusCode);
    giftCardWrapper.put("message", errorMessage);
    
    if (statusCode == ShopRunnerConstants.SR_STATUS_0_DEFAULT) {
      giftCardWrapper.put("newOrderBalance", shopRunnerGiftCard.getNewOrderBalance());
      giftCardWrapper.put("gcBalance", shopRunnerGiftCard.getGiftCardBalance());
    }
    
    shopRunner.setGiftCard(shopRunnerGiftCard);
    shopRunner.setResponse(giftCardWrapper);
    pageModel.setShopRunner(shopRunner);
  }
  
  /**
   * Process shoprunner order
   * 
   * @param profile
   * @param request
   * @param response
   * @throws Exception
   */
  private void processPROrder(NMProfile profile, final DynamoHttpServletRequest request, final DynamoHttpServletResponse response) throws Exception {
    logExtraDebugInfo("---ShopRunnerAPIPageEvaluator:processPROrder");
    final ShoppingCartHandler cart = CheckoutComponents.getCartHandler(request);
    profile = CheckoutComponents.getProfile(request);
    String regLoginEmail = null;
    String regProfileId = null;
    boolean isUserAuthenticated = false;
    boolean isAlipayOrder = false;
    NMOrderImpl order = null;
    try {
      if (profile.isAuthorized()) {
        regLoginEmail = profile.getLogin();
        regProfileId = profile.getId();
        isUserAuthenticated = true;
      }
      NMOrderManager orderMgr = (NMOrderManager) cart.getOrderManager();
      order = cart.getNMOrder();
      // ShopRunner has requested not to store any of ShopRunner user data (Addresses, credit card etc.,)
      // Anonymous profile is created to avoid any overwrites to NEIMAN profile with ShopRunner data.
      // Get an anonymous profile if user is either authorized (logged in) or registered, place an order anonymously and
      // map the order to the original profile
      if (profile.isAuthorized() || profile.isRegisteredProfile()) {
        final List<GiftCard> giftCards = CheckoutAPI.getGiftCardsFromOrder(order);
        order.setShopRunner(true);
        profile = getAnonymousProfile(cart, request);
        orderMgr = (NMOrderManager) cart.getOrderManager();
        order = cart.getNMOrder();
        // CheckoutAPI.repriceOrder( cart, profile, -1);
        // Hard logout clears all payment group relationships in order, retain gift cards from order before doing hard-logout and
        // re-apply gift cards to order(if any) after getting an anonymous profile
        if (!giftCards.isEmpty()) {
          for (final GiftCard gc : giftCards) {
            CheckoutAPI.applyGiftCard(order, orderMgr, CheckoutComponents.getGiftCardHolder(request), gc.getCardNumber(), gc.getCid());
          }
        }
      }
      final JSONObject srCart = getShopRunnerCart(request);
      // Set a boolean flag to identify if the Order is Alipay based on the Cart object attribute
      if (null != srCart.get(ShopRunnerConstants.ALIPAY_ORDER)) {
        isAlipayOrder = (Boolean) srCart.get(ShopRunnerConstants.ALIPAY_ORDER);
      }
      
      if (isAlipayOrder) {
        order.setAlipay(true);
      }
      
      // read cart data from SR request
      final String message = validateSRCart(srCart, cart);
      if (message != null) {
        buildShopRunnerCustomErrorResponse(message);
        return;
      }
      // get shipping address
      final JSONObject addressObject = (JSONObject) srCart.get("address");
      final ContactInfo shippingAddress = getShippingAddressFromSR(request, addressObject);
      if (shippingAddress == null) {
        getLogger().logError("Empty Shipping Address");
        buildShopRunnerErrorResponse(ShopRunnerConstants.SR_STATUS_13_INCOMPLETE_ADDRESS);
        return;
      }
      
      final NMProfileFormHandler profileHandler = (NMProfileFormHandler) CheckoutComponents.getProfileFormHandler(request);
      
      // validate shipping address
      List<Message> addrMsgs = CheckoutAPI.validateAddress(shippingAddress, profileHandler, AddressMessageFields.KEY_SHIPPINGEDIT, "shippingaddress", false);
      if (!addrMsgs.isEmpty()) {
        getLogger().logError("Shipping Address validation error :" + addrMsgs.get(0).getMessageText());
        buildShopRunnerErrorResponse(ShopRunnerConstants.SR_STATUS_13_INCOMPLETE_ADDRESS);
        return;
      }
      // verify and standardize shipping address
      if (!isAlipayOrder) {
        verifyAndStandardizeAddress(shippingAddress, cart);
      }
      
      // validate delivery phone numbers for drop ship items in order
      boolean dropShipFlag = false;
      if (srCart.containsKey("dropship_ph_flag")) {
        dropShipFlag = (Boolean) srCart.get("dropship_ph_flag");
      }
      String deliveryPhoneNumber = null;
      if (dropShipFlag) {
        deliveryPhoneNumber = (String) srCart.get("dropship_ph");
        if ((deliveryPhoneNumber == null) || deliveryPhoneNumber.equals("")) {
          getLogger().logError(CheckoutComponents.getShopRunner().getErrorDeliveryPhoneMissing());
          buildShopRunnerCustomErrorResponse(CheckoutComponents.getShopRunner().getErrorDeliveryPhoneMissing());
          return;
        }
      }
      // Validate billing address and payment information
      final String srCheckOutId = request.getParameter("SRCheckoutId");
      // ESB call to get payment info
      GetShoprunnerPaymentInfoResponse srPaymentResponse = null;
      ContactInfo billingAddress = null;
      PaymentInfo paymentInfo = null;
      Collection<Message> messages = null;
      
      // ESB call to get shoprunner payment info is made only if credit card is required for order processing.
      double orderTotal = 0.0;
      if (orderMgr.getCurrentOrderAmountRemaining(order, false) > 0.0) {
        // SHOPRUN-252 :Sending Order Total in ESB request for ShopRunner & Alipay
        OrderPriceInfo priceInfo = order.getPriceInfo();  
        if(priceInfo != null){
          orderTotal = priceInfo.getTotal();
        }
        srPaymentResponse = getShopRunnerPaymentInfo(srCheckOutId, orderTotal);
        if (srPaymentResponse == null) {
          buildShopRunnerErrorResponse(ShopRunnerConstants.SR_STATUS_15_NETWORK_TIMEOUT);
          return;
        }
        // Set billing address based on the type of order
        billingAddress = setBillingAddress(isAlipayOrder, shippingAddress, srPaymentResponse);
        if (billingAddress == null) {
          getLogger().logError("Empty Billing Address");
          buildShopRunnerErrorResponse(ShopRunnerConstants.SR_STATUS_14_INVALID_ADDRESS);
          return;
        }
        
        // validate billing address
        addrMsgs = CheckoutAPI.validateAddress(billingAddress, profileHandler, AddressMessageFields.KEY_BILLINGADDRESS, "billingaddress", false);
        if (!addrMsgs.isEmpty()) {
          getLogger().logError("Billing Address validation error : " + addrMsgs.get(0).getMessageText());
          buildShopRunnerErrorResponse(ShopRunnerConstants.SR_STATUS_14_INVALID_ADDRESS);
          return;
        }
        // verify and standardize billing address
        if (!isAlipayOrder) {
          verifyAndStandardizeAddress(billingAddress, cart);
        }
        paymentInfo = getPaymentInfoFromSR(srPaymentResponse);
        // validate paymentinfo and update order with new shipping and payment info
        messages = CheckoutAPI.validatePaymentInfo(order, profile, paymentInfo);
        if (!messages.isEmpty()) {
          buildShopRunnerCustomErrorResponse("Payment info validation error: " + ((Message) messages.toArray()[0]).getMsgText());
          return;
        }
      } else {
        // set billing address as shipping address if credit card details are not required for SR order
        billingAddress = new ContactInfo();
        billingAddress.copyFrom(shippingAddress);
        billingAddress.setEmailAddress(shippingAddress.getEmailAddress());
      }
      // update profile shipping and order shipping address/levels
      updateProfileAndOrderShippingInfo(profile, shippingAddress, cart);
      // update delivery phone for drop ship items in order
      if (deliveryPhoneNumber != null) {
        final ShippingGroup shippingGroup = (ShippingGroup) order.getShippingGroups().get(0);
        final NMRepositoryContactInfo address = CheckoutAPI.getShippingGroupAddress(shippingGroup);
        address.setDeliveryPhoneNumber(deliveryPhoneNumber);
      }
      // update profile billing and email address
      updateProfileBillingAddress(profile, billingAddress);
      // set payment details to order
      if (paymentInfo != null) {
        CheckoutAPI.changeCreditCardOnOrder(order, orderMgr, profile.getBillingAddress(), paymentInfo);
      }
      
      // validate order
      final OrderInfo orderInfo = getOrderInfo(request);
      // orderInfo.setCreditCardSecCode( paymentInfo.getCardSecCode() );
      orderInfo.setUserLocale(cart.getUserLocale(request, null));
      logExtraDebugInfo("---ShopRunnerAPIPageEvaluator:processPROrder line 712:calling validateOrder");
      messages = CheckoutAPI.validateOrder(cart, profile, profileHandler, orderInfo, false);
      logExtraDebugInfo("---ShopRunnerAPIPageEvaluator:processPROrder line 714:returned from validateOrder with messages size " + messages.size());
      if (!messages.isEmpty()) {
        // inventory error messages
        String errorMessage = getInventoryErrorMessages(messages);
        logExtraDebugInfo("---ShopRunnerAPIPageEvaluator:processPROrder line 718:returned from getInventoryErrorMessages with msg " + (errorMessage == null ? "null" : errorMessage));
        if (errorMessage != null) {
          final int statusCode = ShopRunnerConstants.SR_STATUS_6_OUT_OF_STOCK;
          logExtraDebugInfo("---ShopRunnerAPIPageEvaluator:processPROrder line 721:calling buildShopRunnerWithError with status code/error msg " + statusCode + "/" + errorMessage);
          buildShopRunnerWithError(profile, statusCode, errorMessage, ShopRunnerConstants.URL_CART, cart);
          return;
        }
        // credit card and other validation messages
        errorMessage = getOrderValidationErrorMessage(messages);
        if (errorMessage != null) {
          buildShopRunnerCustomErrorResponse(errorMessage);
          return;
        }
      }
      logExtraDebugInfo("---ShopRunnerAPIPageEvaluator:processPROrder line 732:no messages to process");
      // submit order
      // Set OrderInfo property for SR and Alipay flow
      setOrderInfoCheckoutType(isAlipayOrder, orderInfo);
      CheckoutAPI.submitOrder(cart, profile, profileHandler, orderInfo);
      getLogger().logInfo(CheckoutComponents.getShopRunner().getMessageOrderPlaced());
      
      // update profile billing and email address - set only names to billing address, profile names set from billing address on user registration
      final ContactInfo newbillingAddress = new ContactInfo();
      newbillingAddress.setFirstName(billingAddress.getFirstName());
      newbillingAddress.setLastName(billingAddress.getLastName());
      newbillingAddress.setEmailAddress(billingAddress.getEmailAddress());
      updateProfileBillingAddress(profile, newbillingAddress);
      
      // clear addresses and payment methods
      clearProfileData(profile);
      // reset profile to actual(registered and logged in) from anonymous after successful order placement or during any excpetion
      if (isUserAuthenticated && (regProfileId != null) && (regLoginEmail != null)) {
        resetProfile(profile, regLoginEmail, request, response);
        CheckoutAPI.updateOrderProfile(cart, profile, order.getId());
      }
      // submit response
      submitOrderResponseToShopRunner(order.getId());
    } catch (final Exception e) {
      getLogger().logError(e.getMessage(), e);
      buildShopRunnerCustomErrorResponse(CheckoutComponents.getShopRunner().getErrorOrderSubmission());
    } finally {
      // reset profile to actual(registerd and logged in) from anonymous after successful order placement or during any exception
      if (isUserAuthenticated && (regProfileId != null) && (regLoginEmail != null) && !profile.getId().equals(regProfileId)) {
        resetProfile(profile, regLoginEmail, request, response);
        CheckoutAPI.updateOrderProfile(cart, profile, order.getId());
      }
    }
  }
  
  /**
   * This method sets OrderInfo instance based on the type of Order
   * 
   * @param isAlipayOrder
   * @param orderInfo
   */
  private void setOrderInfoCheckoutType(final boolean isAlipayOrder, final OrderInfo orderInfo) {
  	orderInfo.setMktOptIn(true);
    if (isAlipayOrder) {
      orderInfo.setAlipayExpressCheckout(true);
    } else {
      orderInfo.setSRExpressCheckOut(true);
    }
  }
  
  /**
   * This method sets the billingAddreaa based on the type of order
   * 
   * @param isAlipayOrder
   * @param shippingAddress
   * @param srPaymentResponse
   * @return ContactInfo
   */
  private ContactInfo setBillingAddress(final boolean isAlipayOrder, final ContactInfo shippingAddress, final GetShoprunnerPaymentInfoResponse srPaymentResponse) {
    ContactInfo billingAddress;
    if (isAlipayOrder) {
      // Set Shipping address as billing address for Alipay Orders
      billingAddress = new ContactInfo();
      billingAddress.copyFrom(shippingAddress);
      billingAddress.setEmailAddress(shippingAddress.getEmailAddress());
    } else {
      billingAddress = getBillingAddressFromSR(srPaymentResponse);
    }
    return billingAddress;
  }
  
  /**
   * Build ShopRunner cart response with error code and message
   * 
   * @param statusCode
   * @param errorMessage
   * @param redirectUrl
   */
  @SuppressWarnings("unchecked")
  private void buildShopRunnerWithError(final NMProfile profile, final int statusCode, final String errorMessage, final String redirectUrl, final ShoppingCartHandler cartHandler) throws Exception {
    logExtraDebugInfo("---ShopRunnerAPIPageEvaluator:buildShopRunnerWithError:statusCode/errorMessage is " + statusCode + "/" + (errorMessage == null ? "null" : errorMessage));
    buildShopRunner(profile, statusCode, cartHandler);
    final CheckoutPageModel pageModel = (CheckoutPageModel) getPageModel();
    final ShopRunnerResponse shopRunner = pageModel.getShopRunner();
    final JSONObject cartWrapper = shopRunner.getResponse();
    cartWrapper.put("status", statusCode);
    cartWrapper.put("message", errorMessage);
    cartWrapper.put("redirectURL", redirectUrl);
    shopRunner.setResponse(cartWrapper);
    pageModel.setShopRunner(shopRunner);
  }
  
  /**
   * Build inventory error message during ShopRunner express checkout
   * 
   * @param messages
   * @return String
   */
  @SuppressWarnings("unchecked")
  private String getInventoryErrorMessages(final Collection<Message> messages) {
    logExtraDebugInfo("---ShopRunnerAPIPageEvaluator:getInventoryErrorMessages:count of messages is " + messages.size());
    String errorMessage = null;
    // Message msg = (Message) messages.toArray()[0];
    for (final Message msg : messages) {
      logExtraDebugInfo("---ShopRunnerAPIPageEvaluator:getInventoryErrorMessages line 835:processing msg " + msg.getMsgId());
      if ((msg != null) && "finalStockCheck".equals(msg.getMsgId())) {
        if (msg.getExtraParams().containsKey("errorMessages")) {
          logExtraDebugInfo("---ShopRunnerAPIPageEvaluator:getInventoryErrorMessages line 838:extra params includes errorMessages");
          if (msg.getExtraParams().get("errorMessages") instanceof List) {
            final List<StockLevelMessage> stockMessages = (List<StockLevelMessage>) msg.getExtraParams().get("errorMessages");
            if (!stockMessages.isEmpty()) {
              CheckoutComponents.getMessageContainer(getRequest()).addAll(messages);
              final HttpSession session = getRequest().getSession();
              if (session != null) {
                session.setAttribute("INVENTORY_ERROR_MESSAGES", CheckoutComponents.getMessageContainer(getRequest()));
              }
              logExtraDebugInfo("---ShopRunnerAPIPageEvaluator:getInventoryErrorMessages line 847:stockMessages not empty, retrieving errorOutOfStock msg");
              errorMessage = CheckoutComponents.getShopRunner().getErrorOutOfStock();
              break;
            }
          } else if (msg.getExtraParams().get("errorMessages") instanceof TreeSet) {
            logExtraDebugInfo("---ShopRunnerAPIPageEvaluator:getInventoryErrorMessages line 852:extraParams.errorMesages is TreeSet");
            if (msg.getExtraParams().containsKey("promoMessages") && (msg.getExtraParams().get("promoMessages") instanceof List)) {
              final List<StockLevelMessage> stockMessages = (List<StockLevelMessage>) msg.getExtraParams().get("promoMessages");
              if (!stockMessages.isEmpty()) {
                if (getLogger().isLoggingDebug()) { 
                	getLogger().logDebug("Promo messages after inventory CO in SR Express>>" + stockMessages);
                }
              }
            }
          }
        }
      }
    }
    logExtraDebugInfo("---ShopRunnerAPIPageEvaluator:getInventoryErrorMessages line 860:returning " + (errorMessage == null ? "null" : errorMessage));
    return errorMessage;
  }
  
  private String getShippingRestrictionErrorMessages(final Collection<Message> messages) {
    String errorMessage = null;
    final Message msg = (Message) messages.toArray()[0];
    if ((msg != null) && "GenericOrderShippingMessage".equals(msg.getMsgId())) {
      if (msg.getMsgText().contains("cannot be shipped to")) {
        errorMessage = msg.getMsgText();
      }
    }
    return errorMessage;
  }
  
  /**
   * Verify and standardize address
   * 
   * @param shippingAddress
   * @param cart
   * @throws Exception
   */
  private void verifyAndStandardizeAddress(final ContactInfo address, final ShoppingCartHandler cart) throws Exception {
    final List<ContactInfo> contactInfos = new ArrayList<ContactInfo>();
    contactInfos.add(address);
    final AddressVerificationContainer container = CheckoutAPI.getAddressVerificationContainer(cart.getAddressVerificationHelper(), contactInfos);
    if (container != null) {
      final AddressVerificationData data = container.getAddress(contactInfos.get(0).getId());
      address.setAddressLine1(data.getVerifiedAddress1());
      address.setAddressLine2(data.getVerifiedAddress2());
      address.setCity(data.getVerifiedCity());
      address.setState(data.getVerifiedState());
      address.setZip(data.getVerifiedPostalCode());
      address.setCountry(data.getVerifiedCountry());
    }
  }
  
  /**
   * Returns specific error text during order validation
   * 
   * @param messages
   * @return String
   */
  private String getOrderValidationErrorMessage(final Collection<Message> messages) {
    String errorText = null;
    if (!messages.isEmpty()) {
      for (final Message message : messages) {
        if (MessageDefs.getMessage(MessageDefs.MSG_CCAuthMessage1).getMsgId().equals(message.getMsgId())) {
          errorText = "" + ShopRunnerConstants.SR_STATUS_14_INVALID_ADDRESS;
          break;
        } else if (MessageDefs.getMessage(MessageDefs.MSG_CCAuthMessage2).getMsgId().equals(message.getMsgId())) {
          errorText = CheckoutComponents.getShopRunner().getErrorInvalidSecCode();
          break;
        } else if (MessageDefs.getMessage(MessageDefs.MSG_CCAuthMessage3).getMsgId().equals(message.getMsgId())) {
          errorText = "" + ShopRunnerConstants.SR_STATUS_24_INVALID_CREDIT_CARD;;
          break;
        }
      }
    }
    return errorText;
  }
  
  /**
   * Clear profile data from ShopRunner
   * 
   * @param profile
   */
  private void clearProfileData(final NMProfile profile) {
    profile.setPropertyValue(ProfileProperties.Profile_defaultCreditCard, null);
    profile.setPropertyValue(ProfileProperties.Profile_Desc_creditCards, null);
    profile.setPropertyValue(ProfileProperties.Profile_shippingAddress, null);
    profile.setPropertyValue(ProfileProperties.Profile_homeAddress, null);
  }
  
  /**
   * Logger
   * 
   * @return
   */
  private GenericService getLogger() {
    return CommonComponentHelper.getLogger();
  }
  
  private void updateProfileBillingAddress(final NMProfile profile, final ContactInfo billingAddress) throws Exception {
    CheckoutAPI.updateProfileBillingAddress(profile, billingAddress);
    if (profile.isAnonymous()) {
      CheckoutAPI.updateProfileEmailAddress(profile, billingAddress.getEmailAddress());
      profile.setPropertyValue(ProfileProperties.Profile_receiveEmail, "yes");
    }
  }
  
  /**
   * Update Profile, order shipping address and order service levels
   * 
   * @param profile
   * @param shippingAddress
   * @param cart
   * @throws Exception
   */
  private void updateProfileAndOrderShippingInfo(final NMProfile profile, final ContactInfo shippingAddress, final ShoppingCartHandler cart) throws Exception {
    final NMOrderImpl order = cart.getNMOrder();
    final NMOrderManager orderMgr = (NMOrderManager) cart.getOrderManager();
    // update profile shipping address
    CheckoutAPI.updateProfileShippingAddress(profile, shippingAddress, false);
    // reset shipping address and update service levels in order
    CheckoutAPI.resetCommerceItemsToDefaultAddresses(profile, order, orderMgr);
    // update order service levels handles both SR eligible and mixed cart items - service level defaulted to SL3 if item is not SR eligible or if order is placed via Alipay.
    if (order.isAlipay()) {
      CheckoutAPI.updateAllShippingGroupServiceLevels(orderMgr, order, profile, ServiceLevel.SL3_SERVICE_LEVEL_TYPE, null);
    } else {
      final String serviceLevelCode = ServiceLevel.SL2_SERVICE_LEVEL_TYPE;
      CheckoutAPI.updateAllShippingGroupServiceLevels(orderMgr, order, profile, serviceLevelCode, null);
    }
  }
  
  /**
   * Reset Anonymous profile to registered profile
   * 
   * @param profile
   * @param regLoginEmail
   */
  private void resetProfile(final NMProfile profile, final String regLoginEmail, final DynamoHttpServletRequest request, final DynamoHttpServletResponse response) {
    try {
      final RepositoryItem pItem = CheckoutAPI.getProfileUsingEmail(regLoginEmail);
      if (pItem != null) {
        profile.setDataSource(pItem);
        profile.getProfileTools().setSecurityStatus(profile, 4);
        // Force profile cookies after user profile reset
        final CookieManager cookieManager = profile.getProfileTools().getCookieManager();
        cookieManager.forceProfileCookies(profile, request, response);
      }
    } catch (final Exception e) {
      getLogger().logDebug("Exception in profile reset: " + e.getMessage());
    }
  }
  
  /**
   * Set Order Info details
   * 
   * @param request
   * @return OrderInfo
   */
  private OrderInfo getOrderInfo(final DynamoHttpServletRequest request) {
    final OrderInfo orderInfo = new OrderInfo();
    final PCSSessionData pcs = (PCSSessionData) request.resolveName("/nm/utils/PCSSessionData");
    final BingSessionData bing = (BingSessionData) request.resolveName("/nm/utils/bing/BingSessionData");
    final LinkshareCookie linkShareCookie = (LinkshareCookie) request.resolveName("/nm/droplet/LinkShareCookie");
    String userPrefs = null;
    String clientTimeZone = null;
    String clientDateString = null;
    try {
      final String userData = request.getParameter("userData");
      final JSONParser parser = new JSONParser();
      final JSONObject fortyOneParameters = (JSONObject) parser.parse(userData);
      if ((fortyOneParameters != null) && !fortyOneParameters.isEmpty()) {
        userPrefs = fortyOneParameters.containsKey("userPrefs") ? (String) fortyOneParameters.get("userPrefs") : null;
        clientTimeZone = fortyOneParameters.containsKey("clientTimeZone") ? (String) fortyOneParameters.get("clientTimeZone") : null;
        clientDateString = fortyOneParameters.containsKey("clientDateString") ? (String) fortyOneParameters.get("clientDateString") : null;
      }
    } catch (final Exception e) {
      getLogger().logError("Error parsing forty one parameters from shoprunner" + e.getLocalizedMessage());
    }
    orderInfo.setClientDateString(clientDateString);
    orderInfo.setClientTimeZone(clientTimeZone);
    orderInfo.setFraudnetBrowserData(userPrefs);
    if (null != linkShareCookie) {
      orderInfo.setLinkShareSiteId(linkShareCookie.getSiteId(request));
      orderInfo.setLinkShareTimeStamp(linkShareCookie.getTimeStamp(request));
      orderInfo.setMid(linkShareCookie.getMid(request));
    }
    orderInfo.setPcsSessionData(pcs);
    orderInfo.setBingSessionData(bing);
    orderInfo.setGiftCardHolder(CheckoutComponents.getGiftCardHolder(request));
    return orderInfo;
  }
  
  /**
   * Get Shoprunner Cart from request
   * 
   * @param request
   * @return JSONObject
   */
  private JSONObject getShopRunnerCart(final DynamoHttpServletRequest request) {
    JSONObject srCart = null;
    try {
      final String[] jsonStringsCart = request.getParameterValues("cart");
      final JSONParser parser = new JSONParser();
      final Object cartObject = parser.parse(jsonStringsCart[0]);
      srCart = (JSONObject) cartObject;
    } catch (final Exception e) {
      getLogger().logDebug("Error in getting ShopRunner cart " + e.getMessage());
    }
    return srCart;
  }
  
  /**
   * Get Shoprunner payment info
   * 
   * @param srCheckOutId
   * @param orderTotal
   * @return GetShoprunnerPaymentInfoResponse
   */
  private GetShoprunnerPaymentInfoResponse getShopRunnerPaymentInfo(final String srCheckOutId, final double orderTotal) {
    GetShoprunnerPaymentInfoResponse srPaymentResponse = null;
    try {
      srPaymentResponse = CheckoutAPI.getShopRunnerPaymentInfo(srCheckOutId, orderTotal);
    } catch (final Exception e) {
      getLogger().logDebug("Error in getting ShopRunner payment info " + e.getMessage());
    }
    return srPaymentResponse;
  }
  
  /**
   * Submit order response to Shoprunner
   */
  @SuppressWarnings("unchecked")
  private void submitOrderResponseToShopRunner(final String orderId) {
    CheckoutAPI.clearEstimatedDeliveryDateInfo();
    final CheckoutPageModel pageModel = (CheckoutPageModel) getPageModel();
    final ShopRunnerResponse shopRunner = new ShopRunnerResponse();
    final JSONObject orderResponse = new JSONObject();
    orderResponse.put("status", ShopRunnerConstants.SR_STATUS_0_DEFAULT);
    orderResponse.put("message", CheckoutComponents.getShopRunner().getMessageOrderPlaced());
    orderResponse.put("orderNumber", orderId);
    orderResponse.put("redirectURL", ShopRunnerConstants.URL_ORDER_COMPLETE);
    shopRunner.setResponse(orderResponse);
    pageModel.setShopRunner(shopRunner);
  }
  
  /**
   * Get Anonymous profile if user is registered but not authenticated
   * 
   * @param cartHandler
   * @param request
   * @return NMProfile
   * @throws Exception
   */
  private NMProfile getAnonymousProfile(final ShoppingCartHandler cartHandler, final DynamoHttpServletRequest request) throws Exception {
    final NMProfileFormHandler profileFormHandler = (NMProfileFormHandler) CheckoutComponents.getProfileFormHandler(request);
    final LoginBean loginBean = new LoginBean();
    loginBean.setType("anonymous");
    final ResultBean resultBean = CheckoutAPI.handleLogin(cartHandler, loginBean, profileFormHandler);
    if (!resultBean.getMessages().isEmpty()) {
      buildShopRunnerCustomErrorResponse("Please try again");
    }
    return (NMProfile) profileFormHandler.getProfile();
  }
  
  /**
   * Validate Neiman's and shoprunner cart items
   * 
   * @param cart
   * @param cartHandler
   * @return String
   */
  private String validateSRCart(final JSONObject cart, final ShoppingCartHandler cartHandler) {
    String message = null;
    final JSONArray productList = (JSONArray) cart.get("products");
    if ((productList == null) || (productList.size() == 0)) {
      message = "" + ShopRunnerConstants.SR_STATUS_41_EMPTY_CART;
    }
    if ((message == null) && (productList.size() != cartHandler.getNMOrder().getCommerceItemCount())) {
      message = CheckoutComponents.getShopRunner().getErrorCartItemsMismatch();
    }
    String skuId = null;
    long count = 1;
    if (message == null) {
      // Shop runner cart
      final Map<String, Long> srCartMap = new HashMap<String, Long>();
      for (final Object p : productList) {
        final JSONObject product = (JSONObject) p;
        skuId = (String) product.get("sku");
        count = (Long) product.get("skuQty");
        count = srCartMap.containsKey(skuId) ? srCartMap.get(skuId) + count : count;
        srCartMap.put(skuId, count);
      }
      // NM cart
      final Map<String, Long> cartMap = new HashMap<String, Long>();
      for (final NMCommerceItem item : cartHandler.getNMOrder().getNmCommerceItems()) {
        skuId = item.getCatalogRefId();
        count = item.getQuantity();
        count = cartMap.containsKey(skuId) ? cartMap.get(skuId) + count : count;
        cartMap.put(skuId, count);
      }
      
      final Iterator<String> iter = srCartMap.keySet().iterator();
      while ((message == null) && iter.hasNext()) {
        final String key = iter.next();
        if (!cartMap.containsKey(key) || (srCartMap.get(key) != cartMap.get(key))) {
          message = CheckoutComponents.getShopRunner().getErrorSkuCountMismatch();
        }
      }
    }
    
    // validate service levels
    boolean mixedCart = false;
    for (final NMCommerceItem item : cartHandler.getNMOrder().getNmCommerceItems()) {
      if (!item.getProduct().getIsShopRunnerEligible()) {
        mixedCart = true;
        break;
      }
    }
    
    final JSONArray shippingGroups = (JSONArray) cart.get("shippingGroups");
    if ((message == null) && mixedCart && (shippingGroups.size() == 0)) {
      message = "" + ShopRunnerConstants.SR_STATUS_9_INVALID_SL;
    }
    if ((message == null) && (shippingGroups != null) && (shippingGroups.size() > 0)) {
      final Map<Integer, JSONObject> sgMap = new HashMap<Integer, JSONObject>();
      for (final Object obj : shippingGroups) {
        final JSONObject sg = (JSONObject) obj;
        final int sgIndex = ((Long) sg.get("shippingGroup")).intValue();
        sgMap.put(sgIndex, (JSONObject) ((JSONArray) sg.get("shipping")).get(0));
      }
      
      for (final Object object : productList) {
        final JSONObject product = (JSONObject) object;
        final boolean isSREligible = (Boolean) product.get("isSREligible");
        if (!isSREligible) {
          final int sgIndex = ((Long) product.get("shippingGroup")).intValue();
          // SmartPost : determining the free shipping service level based on amount and country code
          if (!sgMap.containsKey(sgIndex) || !((String) sgMap.get(sgIndex).get("method")).equals(CheckoutComponents.getServiceLevelArray().determineFreeShippingServiceLevel())) {
            message = "" + ShopRunnerConstants.SR_STATUS_9_INVALID_SL;;
            break;
          }
        }
      }
    }
    return message;
  }
  
  /**
   * Get payment information from shop runner
   * 
   * @param srPaymentResponse
   * @return PaymentInfo
   */
  private PaymentInfo getPaymentInfoFromSR(final GetShoprunnerPaymentInfoResponse srPaymentResponse) {
    final PaymentInfo paymentInfo = new PaymentInfo();
    final String ccType = srPaymentResponse.getCcType();
    paymentInfo.setCardType(ccType);
    paymentInfo.setCardTypeCode(ccType.toUpperCase());
    paymentInfo.setCardNumber(srPaymentResponse.getCcNumber());
    paymentInfo.setSkipSecCodeValidation(true);
    paymentInfo.setExpMonth(srPaymentResponse.getCcMonth());
    paymentInfo.setExpYear(srPaymentResponse.getCcYear());
    return paymentInfo;
  }
  
  /**
   * Get shop runner billing address from ESB response
   * 
   * @param srPaymentResponse
   * @return ContactInfo
   */
  private ContactInfo getBillingAddressFromSR(final GetShoprunnerPaymentInfoResponse srPaymentResponse) {
    final ContactInfo billingAddress = new ContactInfo();
    if ((srPaymentResponse.getFirstName() == null) || (srPaymentResponse.getBillingAddress1() == null) || (srPaymentResponse.getBillingZip() == null)) {
      return null;
    }
    String zip = srPaymentResponse.getBillingZip();
    if (zip.indexOf("-") != -1) {
      zip = zip.substring(0, zip.indexOf("-"));
    }
    billingAddress.setFirstName(srPaymentResponse.getFirstName());
    billingAddress.setLastName(srPaymentResponse.getLastName());
    billingAddress.setCountry(srPaymentResponse.getBillingCountry());
    billingAddress.setAddressLine1(srPaymentResponse.getBillingAddress1());
    billingAddress.setAddressLine2(srPaymentResponse.getBillingAddress2());
    billingAddress.setCity(srPaymentResponse.getBillingCity());
    billingAddress.setState(srPaymentResponse.getBillingState());
    billingAddress.setZip(zip);
    billingAddress.setDayTelephone(srPaymentResponse.getPhone());
    billingAddress.setEmailAddress(srPaymentResponse.getEmail());
    return billingAddress;
  }
  
  /**
   * Get Shipping address from Shop runner request
   * 
   * @param request
   * @param addressObject
   * @return ContactInfo
   */
  private ContactInfo getShippingAddressFromSR(final DynamoHttpServletRequest request, final JSONObject addressObject) {
    final ContactInfo shippingAddress = new ContactInfo();
    final String firstName = request.getParameter("firstName");
    final String lastName = request.getParameter("lastName");
    final String phoneNumber = request.getParameter("phone");
    final String emailAddress = request.getParameter("email");
    if ((addressObject == null) || !addressObject.containsKey("address1")) {
      return null;
    }
    final String address1 = (String) addressObject.get("address1");
    String address2 = null;
    if (addressObject.containsKey("address2")) {
      address2 = (String) addressObject.get("address2");
    }
    final String city = (String) addressObject.get("city");
    final String state = (String) addressObject.get("state");
    String zip = (String) addressObject.get("zip");
    if (zip.indexOf("-") != -1) {
      zip = zip.substring(0, zip.indexOf("-"));
    }
    final String country = (String) addressObject.get("country");
    
    shippingAddress.setFirstName(firstName);
    shippingAddress.setLastName(lastName);
    shippingAddress.setAddressLine1(address1);
    shippingAddress.setAddressLine2(address2);
    shippingAddress.setCity(city);
    shippingAddress.setState(state);
    shippingAddress.setZip(zip);
    shippingAddress.setCountry(country);
    shippingAddress.setDayTelephone(phoneNumber);
    shippingAddress.setEmailAddress(emailAddress);
    shippingAddress.setContactAddressName(ProfileProperties.Profile_shippingAddressName);
    
    return shippingAddress;
  }
  
  private boolean isCustomMessagePromoError(final Message promoMessage) {
    return !promoMessage.getMsgId().equals(MessageDefs.getMessage(MessageDefs.MSG_Promotion_ConflictingMarkdownPromoCode).getMsgId())
            && !promoMessage.getMsgId().equals(MessageDefs.getMessage(MessageDefs.MSG_Promotion_ConflictingDollarOffPromoCode).getMsgId())
            && !promoMessage.getMsgId().equals(MessageDefs.getMessage(MessageDefs.MSG_Promotion_GiftwrapReminder).getMsgId());
  }
  
  /**
   * User selects "edit shopping bag", system should remove SR session data, and return a redirect url
   */
  @SuppressWarnings("unchecked")
  private void reviewOrder(final NMProfile profile, final DynamoHttpServletRequest request) throws Exception {
    // Clear SR data
    clearShopRunnerData(profile, request);
    
    final CheckoutPageModel pageModel = (CheckoutPageModel) getPageModel();
    final JSONObject jsonResponse = new JSONObject();
    jsonResponse.put("status", 0);
    jsonResponse.put("redirectURL", ShopRunnerConstants.URL_CART);
    final ShopRunnerResponse shopRunner = new ShopRunnerResponse();
    shopRunner.setResponse(jsonResponse);
    pageModel.setShopRunner(shopRunner);
  }
  
  /**
   * Remove any stored SR data from system.
   */
  @SuppressWarnings("unchecked")
  private void abandonPRCart(final NMProfile profile, final DynamoHttpServletRequest request) throws Exception {
    // Clear SR data
    clearShopRunnerData(profile, request);
    
    final CheckoutPageModel pageModel = (CheckoutPageModel) getPageModel();
    final JSONObject jsonResponse = new JSONObject();
    jsonResponse.put("status", 0);
    final ShopRunnerResponse shopRunner = new ShopRunnerResponse();
    shopRunner.setResponse(jsonResponse);
    pageModel.setShopRunner(shopRunner);
  }
  
  // Clear ShopRunner data
  private void clearShopRunnerData(final NMProfile profile, final DynamoHttpServletRequest request) throws Exception {
    final ShoppingCartHandler cartHandler = CheckoutComponents.getCartHandler(request);
    CheckoutAPI.clearShopRunnerData(profile, cartHandler);
  }
  
  private void logExtraDebugInfo(final String msg) {
    if (CheckoutComponents.getShopRunner().isEnableExtraDebugInfo()) {
      getLogger().logWarning(msg);
    }
  }
  
}
