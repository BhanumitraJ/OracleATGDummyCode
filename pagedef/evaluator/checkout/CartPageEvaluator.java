package com.nm.commerce.pagedef.evaluator.checkout;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.PageContext;

import org.jsoup.helper.StringUtil;

import atg.commerce.order.CommerceItemNotFoundException;
import atg.commerce.order.InvalidParameterException;
import atg.commerce.order.OrderManager;
import atg.commerce.order.ShippingGroupCommerceItemRelationship;
import atg.core.util.StringUtils;
import atg.nucleus.Nucleus;
import atg.repository.RepositoryItem;
import atg.servlet.DynamoHttpServletRequest;
import atg.servlet.ServletUtil;

import com.nm.ajax.checkout.beans.CartProductInfo;
import com.nm.ajax.marketing.GenericTagInfo;
import com.nm.commerce.NMCommerceItem;
import com.nm.commerce.NMCreditCard;
import com.nm.commerce.NMOrderHolder;
import com.nm.commerce.NMProfile;
import com.nm.commerce.beans.RecentlyChangedCommerceItem;
import com.nm.commerce.catalog.NMProduct;
import com.nm.commerce.checkout.CheckoutAPI;
import com.nm.commerce.checkout.CheckoutComponents;
import com.nm.commerce.checkout.SnapshotUtil;
import com.nm.commerce.checkout.beans.ResultBean;
import com.nm.commerce.checkout.beans.ResultBeanEventType;
import com.nm.commerce.checkout.beans.SaveForLaterItemCollection;
import com.nm.commerce.order.NMOrderManager;
import com.nm.commerce.pagedef.definition.CheckoutPageDefinition;
import com.nm.commerce.pagedef.definition.PageDefinition;
import com.nm.commerce.pagedef.model.CheckoutPageModel;
import com.nm.commerce.pagedef.model.bean.RichRelevanceProductBean;
import com.nm.commerce.pricing.NMPricingTools;
import com.nm.commerce.promotion.NMOrderImpl;
import com.nm.common.INMGenericConstants;
import com.nm.common.NMBorderFreeGetQuoteResponse;
import com.nm.common.NMBorderFreePayPalResponse;
import com.nm.components.CommonComponentHelper;
import com.nm.configurator.exception.ConfiguratorException;
import com.nm.configurator.utils.ConfiguratorUtils;
import com.nm.droplet.LocalizationGeneralUtilityDroplet;
import com.nm.formhandler.ShoppingCartHandler;
import com.nm.formhandler.checkout.ShoppingBagUpdateFormHandler;
import com.nm.international.fiftyone.checkoutapi.BorderFreeConstants;
import com.nm.international.fiftyone.checkoutapi.ESBErrorMessages;
import com.nm.repository.ProfileRepository;
import com.nm.sitemessages.Message;
import com.nm.sitemessages.MessageContainer;
import com.nm.storeinventory.ItemAvailabilityLevel;
import com.nm.tms.core.TMSMessageContainer;
import com.nm.tms.utils.TMSDataDictionaryUtils;
import com.nm.twoclickcheckout.util.TwoClickCheckoutUtil;
import com.nm.utils.LinkedEmailUtils;
import com.nm.utils.LocalizationUtils;
import com.nm.utils.NMFormHelper;
import com.nm.utils.NmoUtils;
import com.nm.utils.RichRelevanceClient;
import com.nm.utils.StoreSkuInventoryUtils;
import com.nm.utils.StringUtilities;
import com.nm.utils.SystemSpecs;
import com.nm.utils.fiftyone.FiftyOneUtils;

public class CartPageEvaluator extends BaseCheckoutPageEvaluator {
  /** The Constant ITEM_NOT_AVAILABLE_MSG. */
  private static final String ITEM_NOT_AVAILABLE_MSG = "Item Not Available";
  private static final FiftyOneUtils fiftyOneUtils = (FiftyOneUtils) Nucleus.getGlobalNucleus().resolveName("nm/utils/fiftyone/FiftyOneUtils");
  private static final ESBErrorMessages esbErrorMessages = (ESBErrorMessages) Nucleus.getGlobalNucleus().resolveName("/nm/international/fiftyone/checkoutapi/ESBErrorMessages");
  
  @Override
  public boolean evaluate(final PageDefinition pageDefinition, final PageContext pageContext) throws Exception {
    final DynamoHttpServletRequest request = getRequest();
    final ShoppingCartHandler cart = CheckoutComponents.getCartHandler(request);
    final NMOrderHolder orderHolder = (NMOrderHolder) cart.getOrderHolder();
    /* If the order is two click checkout order and the two click checkout flow is cancelled then the saved order should be merged. */
    NMOrderImpl order = cart.getNMOrder();
    if (order.isBuyItNowOrder()) {
      final TwoClickCheckoutUtil twoClickCheckoutUtil = CommonComponentHelper.getTwoClickCheckoutUtil();
      if (null != twoClickCheckoutUtil) {
        order = twoClickCheckoutUtil.moveSavedOrderBackToCurrent(orderHolder);
      }
    }

    if (!super.evaluate(pageDefinition, pageContext)) {
      return false;
    }
    final String countryChange = getRequest().getParameter("countryChange");
    final ShoppingBagUpdateFormHandler restrHandler = CheckoutComponents.getShoppingBagUpdateFormHandler(request);
    
    final boolean advance = "true".equals(getRequest().getParameter("co"));
    final NMProfile profile = CheckoutComponents.getProfile(request);
    
    CheckoutPageDefinition redirectPageDefinition = null;
    boolean isBorderFreeSupportedCountry = false;
    boolean isCardSupportedForCountry = false;
    String defaultCreditCardType = "";
    if (null != profile.getDefaultCreditCard() && null != profile.getDefaultCreditCard().getCreditCardType()) {
      defaultCreditCardType = profile.getDefaultCreditCard().getCreditCardType();
    }
    final SystemSpecs systemSpecs = CommonComponentHelper.getSystemSpecs();
    final CheckoutPageModel pageModel = (CheckoutPageModel) getPageModel();
    final NMOrderManager orderMgr = (NMOrderManager) cart.getOrderManager();
    final LocalizationUtils utils = (LocalizationUtils) Nucleus.getGlobalNucleus().resolveName("/nm/utils/LocalizationUtils");
    final TMSDataDictionaryUtils dataDictionaryUtils = CommonComponentHelper.getDataDictionaryUtils();
    // get the TMSMessageContainer
    final TMSMessageContainer tmsMessageContainer = CheckoutComponents.getTMSMessageContainer(request);
    final ArrayList<String> tmsMessageList = new ArrayList<String>();
    dataDictionaryUtils.setFormErrorsToTmsMessageList(cart, tmsMessageList);
    dataDictionaryUtils.setFormErrorsToTmsMessageList(restrHandler, tmsMessageList);
    
    // setting the origination of the promotion to cart page, by setting 0.this is for PLCC percent off promotion for LastCall.
    order.setPromoOrigin(0);
    // Removing abtest logic for AbTestHelper.ESTIMATED_CART_TOTAL as Site production confirmed this not being used.
    // This flag is only used on shopping bag page, when false, we do not show estimated itemized order total.
    // Billing and Shipping page also uses the same flag and logic in ordercharges.jsp, but this flag overridden to true in BaseCheckoutPageEvaluator.java
    pageModel.setShowEstimatedOrderTotal(!getShowEstimates(profile, order));
    
    if (CheckoutAPI.isInternationalSession(profile) && CheckoutAPI.isEmptyCart(order)) {
      pageModel.setShowEstimatedOrderTotal(false);
    }
    
    if (CommonComponentHelper.getBrandSpecs().isBOPSEnabled()) {
      updateBopsSpecificValues(pageModel, orderMgr, order, cart);
    }
    
    /*
     * removing payment group from order for international user if it has gift card applied
     */
    final String countryPref = profile.getCountryPreference();
    if (!countryPref.equalsIgnoreCase("US")) {
      final List<NMCreditCard> paymentGroupList = order.getPaymentGroups();
      final Iterator<NMCreditCard> iterator = paymentGroupList.iterator();
      while (iterator.hasNext()) {
        final NMCreditCard paymentGroup = iterator.next();
        if (paymentGroup != null && (paymentGroup.isPrepaidCard() || paymentGroup.isGiftCard())) {
          final double amountRemaining = paymentGroup.getAmountRemaining();
          final double amount = paymentGroup.getAmount();
          paymentGroup.setAmountRemaining(amountRemaining + amount);
          paymentGroup.setCreditCardNumber("");
          paymentGroup.setCreditCardType("");
          paymentGroup.setCid("");
          paymentGroup.setExpirationMonth("");
          paymentGroup.setExpirationYear("");
        }
      }
    }
    
    // LocalizationData localizationData = LocalizationData.extractData(
    // getRequest() );
    // int estimatedZipCode = ( localizationData == null ) ? -1 :
    // localizationData.getFirstZipCode();
    
    List<Message> messages = new ArrayList<Message>();
    
    final List<NMCommerceItem> commerceItemsList = order.getNmCommerceItems();
    // Start NMOBLDS-3260
    displayMsgForOOSConfiguredItems(pageModel, commerceItemsList, profile.getCountryPreference());
    // End
    final LocalizationGeneralUtilityDroplet droplet = new LocalizationGeneralUtilityDroplet();
    /* error message display for the removed restricted products */
    if (commerceItemsList.size() > 0 && utils.isSupportedByFiftyOne(profile.getCountryPreference())) {
      getRequest().setAttribute("shoppingBagErrorMessage", restrHandler.getRemovedProdNames());
      final String errorMsg = (String) getRequest().getAttribute("shoppingBagErrorMessage");
      droplet.splitErrorMessage(errorMsg);
      tmsMessageList.add(errorMsg);
    }
    /* END: error message display for the removed restricted products */

    orderMgr.updateOrder(order);
    
    Collection<Message> promoMessages = CheckoutAPI.repriceOrder(cart, profile, SnapshotUtil.EDIT_ITEM_FROM_REGULAR_CART);
    pageModel.setOrderEligibleForFreeDeliveryProcessing(order.isEligibleForFreeDeliveryProcessing());
    pageModel.setParentheticalChargesPresent(order.isCheckForParentheticalCharges());
    messages.addAll(promoMessages);
    updatePromotionFields(cart);
    updateGwpFields(pageModel, order);
    dataDictionaryUtils.buildMsgList(promoMessages, tmsMessageList);
    
    final List<ShippingGroupCommerceItemRelationship> commerceItemRelationships = CheckoutAPI.getShippingGroupCommerceItemRelationships(order, orderMgr);
    pageModel.setCommerceItemRelationships(commerceItemRelationships);
    
    pageModel.setOrder(order);
    
    final int statusCode = 0;
    
    pageModel.setIsShipToMultiple(CheckoutAPI.getIsShipToMultiple(order));
    
    pageModel.setOrderContainsVirtualGiftCard(CheckoutAPI.containsVirtualGiftCard(order));
    
    pageModel.setOrderContainsGiftCard(CheckoutAPI.containsGiftCard(order));
    
    pageModel.setOrderContainsWishListItem(CheckoutAPI.orderContainsWishlistItem(order));
    
    pageModel.setEmptyCart(CheckoutAPI.isEmptyCart(order));
    
    // Save For Later
    final SaveForLaterItemCollection saveForLaterItems = CheckoutAPI.getSaveForLaterItems(profile, orderMgr);
    boolean showAllSaveForLaterItems = false;
    
    // if there aren't any items in the cart/bag then always show all the
    // save for later items.
    if (CheckoutAPI.isEmptyCart(order) || new Boolean(getRequest().getParameter("showAllSFL"))) {
      showAllSaveForLaterItems = true;
    }
    final int numberOfItemsInList = saveForLaterItems.size();
    final int nonSellableItemCount = saveForLaterItems.getNoLongerAvailableItemCount();
    
    final int minimumToShow = Math.max(3, nonSellableItemCount);
    
    final boolean showSeeAllLink = numberOfItemsInList > minimumToShow && !showAllSaveForLaterItems;
    
    final int numberToDisplay = showAllSaveForLaterItems ? saveForLaterItems.size() : minimumToShow;
    
    pageModel.setSaveForLaterItems(saveForLaterItems);
    pageModel.setNumSaveForLaterItemsToDisplay(new Integer(numberToDisplay));
    pageModel.setShowSeeAllSaveForLaterLink(new Boolean(showSeeAllLink));
    
    /*
     * Changes Start here- Rich Revelavance ymal recommendations from server side API call
     */
    if (systemSpecs.isRichRelevanceServerSideEnabled() && systemSpecs.isRrServerSideCountryEnabled()) {
      @SuppressWarnings("unchecked")
      final List<NMCommerceItem> cList = order.getCommerceItems();
      final StringBuffer commerceitems = new StringBuffer();
      for (final NMCommerceItem cItem : cList) {
        if (cList.size() > 1) {
          cItem.getProductId();
          commerceitems.append(cItem.getProductId());
          commerceitems.append("|");
        } else {
          commerceitems.append(cItem.getProductId());
        }
      }
      
      if (order.getCommerceItemCount() > 0) {
        final NMProduct nmProduct = ((NMCommerceItem) order.getCommerceItems().get(0)).getProduct();
        final Boolean isBeautyProduct = utils.checkBeautyProduct(nmProduct);
        final RepositoryItem datasource = nmProduct.getDataSource();
        final Collection collection = (Collection) datasource.getPropertyValue("productFlags");
        final Object collectionItem = "flgMerchManual";
        boolean manulSuggestCM2 = false;
        if (collection.contains(collectionItem)) {
          manulSuggestCM2 = true;
        }
        final boolean isChanel = "chanel".equalsIgnoreCase(nmProduct.getWebDesignerName());
        final boolean isHermes = "hermes".equalsIgnoreCase(nmProduct.getWebDesignerName());
        pageModel.setChanel(isChanel);
        pageModel.setHermes(isHermes);
        if (!(isBeautyProduct && (pageModel.isChanel() || pageModel.isHermes()) || manulSuggestCM2)) {
          final RichRelevanceClient ymalData = (RichRelevanceClient) request.resolveName("/nm/utils/RichRelevanceClient");
          final HttpSession session = request.getSession();
          final String sessionId = session.getId();
          final String userId = getProfile().getEdwCustData().getUcid();
          final String blackList = "";
          final RichRelevanceProductBean ymalProducts =
                  ymalData.getProductRecordsFromRichRelevance(userId, sessionId, commerceitems.toString(), "cart_page.horizontal", blackList, getProfile().getCountryPreference());
          pageModel.setYmalProducts(ymalProducts);
          pageModel.setServerSideYmal(true);
        }
      }
    }
    // Login
    if (order.getCommerceItemCount() == 0) {
      final NMOrderImpl lastOrder = (NMOrderImpl) orderHolder.getLast();
      
      if (lastOrder != null) {
        pageModel.setLastOrder(lastOrder);
        // String productSystemCode =
        // systemSpecs.getProductionSystemCode();
        // String lastOrderNumber = ( ( productSystemCode != null ) ?
        // productSystemCode : "" ) + lastOrder.getId();
        // pageModel.setOrderNumber(lastOrderNumber);
      }
    }
    
    boolean mergeCart = false;
    Object showMergeCart = ServletUtil.getCurrentRequest().getSession().getAttribute("showMergeCartWindow");
    if (showMergeCart != null && (Boolean) showMergeCart) {
      mergeCart = true;
    }
    
    MessageContainer messagesContainer = CheckoutComponents.getMessageContainer(getRequest());
    // add the message from the message container into the TMSMessageList
    dataDictionaryUtils.buildMsgList(messagesContainer.getAll(), tmsMessageList);
    // ShopRunner changes start - Inventory error messages read from session
    // for ShopRunner specific need - session data cleared after read
    if (messagesContainer.isEmpty() && getRequest().getSession().getAttribute("INVENTORY_ERROR_MESSAGES") != null) {
      messagesContainer = (MessageContainer) getRequest().getSession().getAttribute("INVENTORY_ERROR_MESSAGES");
      getRequest().getSession().setAttribute("INVENTORY_ERROR_MESSAGES", null);
    }
    // ShopRunner changes end
    pageModel.setMessages(messagesContainer.getIdMap());
    pageModel.setMessageFields(messagesContainer.getFieldIdMap());
    // pageModel.setMessageGroups(messagesContainer.groupByPrefix("PROMO_CODE_FIELD"));
    // pageModel.setMessageGroups(messagesContainer.groupByMsgId("BadPromoCode"));
    pageModel.setMessageGroups(messagesContainer.groupByMsgId());
    
    pageModel.setCheckoutState(orderHolder.getResponsiveState());
    // pageModel.getOmnitureBean().setExtraData(
    // CheckoutComponents.getResultBeanContainer( getRequest()
    // ).getResultBean() );
    
    final boolean orderHasReplenshmentItems = order.hasReplenishmentItems();
    pageModel.setOrderHasReplenishmentItems(orderHasReplenshmentItems);
    pageModel.setOrderHasActiveReplenishmentItems(order.hasActiveReplishmentItems());
    
    boolean passwordLoginError = false;
    boolean emailLoginError = false;
    boolean lockedAccountError = false;
    for (final String key : messagesContainer.getIdMap().keySet()) {
      if (StringUtilities.isNotEmpty(key)) {
        if (key.equals("LoginInvalidPassword")) {
          passwordLoginError = true;
        } else if (key.equals("LoginLockedAccount")) {
          lockedAccountError = true;
        } else if (key.equals("LogingUnknowEmail") || key.equals("InvalidEmailAddress")) {
          emailLoginError = true;
        }
        final Message message = pageModel.getMessages().get(key);
        String omnitureMessageText = message.getOmnitureOverrideMessage();
        if (StringUtilities.isEmpty(omnitureMessageText)) {
          omnitureMessageText = message.getMsgText();
        }
      }
    }
    
    String email = "";
    if (!profile.isAnonymous()) {
      pageModel.setIsLoggedIn(new Boolean(true));
      email = (String) profile.getPropertyValue("email");
    } else if (profile.isRegisteredProfile()) {
      email = (String) profile.getPropertyValue("email");
    }
    if (StringUtilities.isEmpty(email) || emailLoginError || passwordLoginError) {
      email = (String) profile.getPropertyValue(ProfileRepository.LAST_ENTERED_EMAIL_ADDRESS);
    }
    
    pageModel.setEmail(email);
    pageModel.setPasswordLoginError(passwordLoginError);
    pageModel.setEmailLoginError(emailLoginError);
    pageModel.setLockedAccountError(lockedAccountError);
    final boolean isInternational = CheckoutAPI.isInternationalSession(profile);
    if (!isInternational) {
      isCardSupportedForCountry = CheckoutAPI.validateDomesticSupportedCard(defaultCreditCardType);
      pageModel.setCardSupportedFlag(isCardSupportedForCountry);
    }
    
    LinkedEmailUtils.getInstance().updateLinkedEmailOrder(order);
    
    updateTagInfo(order);
    // INT-1027 changes starts
    final String quotedForCountry = order.getCountry();
    if (isInternational) {
      order.setPropertyValue("shippingCarrierType", "");
      order.setDutiesPaymentMethod(BorderFreeConstants.PREPAID);
    }
    
    // This Logic has been moved to CartUtils as it possess common functionality for regular checkout & expresscheckout.
    CheckoutAPI.paymentCheckoutValidation(pageModel, order, orderMgr, profile, getRequest(), tmsMessageList);
    /*
     * Following code will call getQuoteAPI.
     */
    String borderFreeErrorMessages = null;
    if (isInternational && order.getCommerceItemCount() > 0) {
      // Make sure, we have valid Billing Address.
      boolean expressPayPalFlag = false;
      order.setChinaUnionPayPaymentType(false);
      order.setRussiaCurrencyToUSD(false); // If customer leaves the order
      // review page, this will
      // reset the currency back
      // to the original.
      final ResultBean rb = new ResultBean();
      final NMBorderFreePayPalResponse nmBorderFreePayPalResponse = CheckoutComponents.getNMBorderFreePayPalResponse(getRequest());
      final NMBorderFreeGetQuoteResponse nmBorderFreeGetQuoteResponse = fiftyOneUtils.getInternationalCheckoutAPIUtil().callgetQuote(profile, order, null, true, null);
      pageModel.setIncompleteBorderFreeQuote(nmBorderFreeGetQuoteResponse.isIncompleteQuote());
      if (!StringUtil.isBlank(nmBorderFreeGetQuoteResponse.getErrorMessage())) {
        borderFreeErrorMessages = nmBorderFreeGetQuoteResponse.getErrorMessage();
      }
      isBorderFreeSupportedCountry = CheckoutAPI.validateBorderFreeSupportedCountries(profile);
      isCardSupportedForCountry = CheckoutAPI.validateBorderFreeSupportedCard(defaultCreditCardType, nmBorderFreeGetQuoteResponse);
      pageModel.setCardSupportedFlag(isCardSupportedForCountry);
      
      // INT-1900 : retrieve Express Paypal message on Product
      // restrictions
      final boolean hasPaypalProdRestrMsg = "true".equals(getRequest().getParameter("prodRestr"));
      final boolean hasPaypalCountryRestrMsg = "true".equals(getRequest().getParameter("countryRestr"));
      if (hasPaypalProdRestrMsg) {
        pageModel.setRestrMsg(esbErrorMessages.getErrorMessage(BorderFreeConstants.PPX_PROD_RESTR_MSG, countryPref));
        // add the paypal message to tmsMessageList
        tmsMessageList.add(esbErrorMessages.getErrorMessage(BorderFreeConstants.PPX_PROD_RESTR_MSG, countryPref));
        // INT-1940 : Omniture related changes for Express Paypal
        rb.getAdditionalAttributes().put("omEventType", "expressPaypal");
        CheckoutComponents.getResultBeanContainer(getRequest()).setResultBean(rb);
      } else if (hasPaypalCountryRestrMsg) {
        pageModel.setRestrMsg(esbErrorMessages.getErrorMessage(BorderFreeConstants.PPX_COUNTRY_RESTR_MSG, countryPref));
        // add the paypal country restriction message to the tmsMessageList
        tmsMessageList.add(esbErrorMessages.getErrorMessage(BorderFreeConstants.PPX_COUNTRY_RESTR_MSG, countryPref));
        // INT-1940 : Omniture related changes for Express Paypal
        rb.getAdditionalAttributes().put("omEventType", "expressPaypal");
        CheckoutComponents.getResultBeanContainer(getRequest()).setResultBean(rb);
      } else {
        pageModel.setRestrMsg("");
      }
      
      if (!mergeCart && !StringUtil.isBlank(nmBorderFreeGetQuoteResponse.getErrorMessage()) && !nmBorderFreeGetQuoteResponse.isIgnorableWarning()) {
        final List<NMCommerceItem> restrictedItems = nmBorderFreeGetQuoteResponse.getRestrictedItemList();
        if (restrictedItems != null && restrictedItems.size() > 0) {
          boolean allRestrictedItemsRemoved = true;
          for (final NMCommerceItem item : restrictedItems) {
            try {
              if (nmBorderFreeGetQuoteResponse.getErrorMessage().trim() != esbErrorMessages.getErrorMessage("RESTR_SKU_NOT_FOUND", countryPref).trim()) {
                // set the recently changed item for product that are removed due to border free restrictions
                CheckoutAPI.setRecentlyChangedItem(order, item, item.getQuantity(), 0L, INMGenericConstants.CART, INMGenericConstants.REMOVE, null);
                CheckoutAPI.removeItemFromOrder(cart, item.getId());
              }
            } catch (final Exception e) {
              // Unable to remove items from cart.
              allRestrictedItemsRemoved = false;
              CommonComponentHelper.getLogger().logError("Unable to remove item from cart. Item :" + item.getId() + ". Error : " + e.getMessage());
            }
          }
          nmBorderFreeGetQuoteResponse.setRestrictedItemList(null);
          // Do not disable submit button when all restricted items
          // are removed.
          pageModel.setDisableSubmitOption(!allRestrictedItemsRemoved);
          try {
            // Re-price the Cart after removing items.
            orderMgr.updateOrder(order);
            promoMessages = CheckoutAPI.repriceOrder(cart, profile, SnapshotUtil.EDIT_ITEM_FROM_REGULAR_CART);
            messages.addAll(promoMessages);
            updatePromotionFields(cart);
            updateGwpFields(pageModel, order);
            pageModel.setCommerceItemRelationships(CheckoutAPI.getShippingGroupCommerceItemRelationships(order, orderMgr));
          } catch (final Exception e) {
            CommonComponentHelper.getLogger().logError("Unable to retrieve the commerce item relationships." + e.getMessage());
          }
        } else {
          // Disable Submit button when there is some error from
          // Borderfree.
          pageModel.setDisableSubmitOption(true);
        }
        pageModel.setBorderFreeMsgs(nmBorderFreeGetQuoteResponse.getErrorMessage());
        tmsMessageList.add(nmBorderFreeGetQuoteResponse.getErrorMessage());
        
        pageModel.setEmptyCart(CheckoutAPI.isEmptyCart(order));
      } else if (!StringUtil.isBlank(nmBorderFreeGetQuoteResponse.getErrorMessage())) {
        pageModel.setBorderFreeMsgs(nmBorderFreeGetQuoteResponse.getErrorMessage());
        tmsMessageList.add(nmBorderFreeGetQuoteResponse.getErrorMessage());
      }
      if (nmBorderFreeGetQuoteResponse.getBasketTotalResponseVO() != null) {
        CheckoutAPI.resetInternationalOrderProperties(order, cart, nmBorderFreeGetQuoteResponse);
      } else {
        pageModel.setIncompleteBorderFreeQuote(true);
      }
      if (nmBorderFreePayPalResponse.getPalPalResponseVO() != null
              && (null != nmBorderFreePayPalResponse.getPalPalResponseVO().getPayPalErrMsg() || !StringUtils.isBlank(nmBorderFreePayPalResponse.getPalPalResponseVO().getErrorMessage()))) {
        if (nmBorderFreePayPalResponse.getPalPalResponseVO().getErrorMessage() != null) {
          expressPayPalFlag = true;
          pageModel.setExpressPayPalFlag(expressPayPalFlag);
          pageModel.setBorderFreeMsgs(nmBorderFreePayPalResponse.getPalPalResponseVO().getErrorMessage());
          tmsMessageList.add(nmBorderFreePayPalResponse.getPalPalResponseVO().getErrorMessage());
        }
        if (null != nmBorderFreePayPalResponse.getPalPalResponseVO().getPayPalErrMsg()) {
          for (int i = 0; i < nmBorderFreePayPalResponse.getPalPalResponseVO().getPayPalErrMsg().size(); i++) {
            expressPayPalFlag = true;
            pageModel.setExpressPayPalFlag(expressPayPalFlag);
            pageModel.setBorderFreeMsgs(nmBorderFreePayPalResponse.getPalPalResponseVO().getPayPalErrMsg().get(i));
            tmsMessageList.add(nmBorderFreePayPalResponse.getPalPalResponseVO().getPayPalErrMsg().get(i));
          }
        }
        
        nmBorderFreePayPalResponse.getPalPalResponseVO().setErrorMessage(null);
        nmBorderFreePayPalResponse.getPalPalResponseVO().setPayPalErrMsg(null);
        
      }
      if (nmBorderFreePayPalResponse.getAltBuyerResponseVO() != null
              && (null != nmBorderFreePayPalResponse.getAltBuyerResponseVO().getExpPayPalErrMsg() || !StringUtils.isBlank(nmBorderFreePayPalResponse.getAltBuyerResponseVO().getErrorMsg()))) {
        if (nmBorderFreePayPalResponse.getAltBuyerResponseVO().getErrorMsg() != null) {
          expressPayPalFlag = true;
          pageModel.setExpressPayPalFlag(expressPayPalFlag);
          pageModel.setBorderFreeMsgs(nmBorderFreePayPalResponse.getAltBuyerResponseVO().getErrorMsg());
          tmsMessageList.add(nmBorderFreePayPalResponse.getAltBuyerResponseVO().getErrorMsg());
        }
        if (null != nmBorderFreePayPalResponse.getAltBuyerResponseVO().getExpPayPalErrMsg()) {
          for (int i = 0; i < nmBorderFreePayPalResponse.getAltBuyerResponseVO().getExpPayPalErrMsg().size(); i++) {
            expressPayPalFlag = true;
            pageModel.setExpressPayPalFlag(expressPayPalFlag);
            pageModel.setBorderFreeMsgs(nmBorderFreePayPalResponse.getAltBuyerResponseVO().getExpPayPalErrMsg().get(i));
            tmsMessageList.add(nmBorderFreePayPalResponse.getAltBuyerResponseVO().getExpPayPalErrMsg().get(i));
          }
        }
        // INT-1940 : Omniture related changes for Express Paypal
        rb.getAdditionalAttributes().put("omEventType", "expressPaypal");
        CheckoutComponents.getResultBeanContainer(getRequest()).setResultBean(rb);
        nmBorderFreePayPalResponse.getAltBuyerResponseVO().setErrorMsg(null);
        nmBorderFreePayPalResponse.getAltBuyerResponseVO().setExpPayPalErrMsg(null);
      }
      /* INT-1874 start-checkout button disable for Red status order */
      if (fiftyOneUtils.isDisableCheckoutButton() && systemSpecs.isFiftyOneEnabled()) {
        pageModel.setDisableSubmitOption(true);
        pageModel.setBorderFreeMsgs(esbErrorMessages.getErrorMessage(BorderFreeConstants.CHECKOUT_BUTTON_DISABLE_MES, countryPref));
        tmsMessageList.add(esbErrorMessages.getErrorMessage(BorderFreeConstants.CHECKOUT_BUTTON_DISABLE_MES, countryPref));
      }
      /* INT-1874 End- checkout button disable for Red status order */
      getPageModel().setNmBorderFreeGetQuoteResponse(nmBorderFreeGetQuoteResponse);
    }
    
    /*
     * Registered Checkout ALL information in account ENDS
     */
    boolean noBorderFreeErrors = true;
    if (CheckoutAPI.isInternationalSession(profile) && !StringUtil.isBlank(borderFreeErrorMessages)) {
      noBorderFreeErrors = false;
    }
    
    if (mergeCart) {
      // stay in cart page
    } else if (advance && systemSpecs.isFiftyOneEnabled() && !profile.isAnonymous() && noBorderFreeErrors) {
      if (CheckoutAPI.profileHasBillingAddress(profile)) {
        if (CheckoutAPI.orderContainsShippingAddress(order) || order.hasAllBopsItems()) {
          // Registered Checkout ALL information in account :Starts
          String defaultShippingCountry = null;
          if (null != profile.getShippingNMAddress() && null != profile.getShippingNMAddress().getCountry()) {
            defaultShippingCountry = profile.getShippingNMAddress().getCountry();
          }
          if (!CheckoutAPI.isInternationalSession(profile)) {
            if (null != defaultShippingCountry && defaultShippingCountry.equalsIgnoreCase(profile.getCountryPreference())
                    && (null == profile.getDefaultCreditCard() || StringUtils.isBlank(profile.getDefaultCreditCard().getCreditCardType()) || isCardSupportedForCountry)) {
              redirectPageDefinition = CheckoutComponents.getOrderReviewPageDefinition();
            } else {
              redirectPageDefinition = CheckoutComponents.getBillingPageDefinition();
            }
            getResponse().sendRedirect(redirectPageDefinition.getBreadcrumbUrl());
            getResponse().setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
            return false;
          } else {
            if (isBorderFreeSupportedCountry && null != defaultShippingCountry && defaultShippingCountry.equalsIgnoreCase(profile.getCountryPreference())
                    && (null == profile.getDefaultCreditCard() || StringUtils.isBlank(profile.getDefaultCreditCard().getCreditCardType()) || isCardSupportedForCountry)) {
              redirectPageDefinition = CheckoutComponents.getOrderReviewPageDefinition();
            } else {
              redirectPageDefinition = CheckoutComponents.getBillingPageDefinition();
            }
            getResponse().sendRedirect(redirectPageDefinition.getBreadcrumbUrl());
            getResponse().setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
            return false;
          } // Registered Checkout ALL information in account :Ends
        } else {
          // Registered Checkout ALL information in account : Starts
          redirectPageDefinition = CheckoutComponents.getBillingPageDefinition();
          // Registered Checkout ALL information in account : Ends
          final String redirectUrl = NMFormHelper.reviseUrl(redirectPageDefinition.getBreadcrumbUrl());
          getResponse().sendRedirect(redirectUrl);
          getResponse().setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
          return false;
        }
      } else {
        // Registered Checkout ALL information in account : Starts
        redirectPageDefinition = CheckoutComponents.getBillingPageDefinition();
        // Registered Checkout ALL information in account : Ends
        getResponse().sendRedirect(redirectPageDefinition.getBreadcrumbUrl());
        getResponse().setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
        return false;
      }
    } else if (advance && !profile.isAnonymous() && !systemSpecs.isFiftyOneEnabled()) {
      // Non FiftyOne Enabled brands.
      if (CheckoutAPI.profileHasBillingAddress(profile)) {
        if (CheckoutAPI.orderContainsShippingAddress(order)) {
          redirectPageDefinition = CheckoutComponents.getOrderReviewPageDefinition();
          getResponse().sendRedirect(redirectPageDefinition.getBreadcrumbUrl());
          getResponse().setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
          return false;
        } else {
          redirectPageDefinition = CheckoutComponents.getBillingPageDefinition();
          getResponse().sendRedirect(redirectPageDefinition.getBreadcrumbUrl());
          getResponse().setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
          return false;
        }
      } else {
        redirectPageDefinition = CheckoutComponents.getBillingPageDefinition();
        getResponse().sendRedirect(redirectPageDefinition.getBreadcrumbUrl());
        getResponse().setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
        return false;
      }
    } else if (profile.isAnonymous() && !profile.isRegisteredProfile()) {
      String uEm = request.getParameter("uEm");
      if (uEm != null) {
        final HttpSession session = getRequest().getSession();
        session.setAttribute("login_to_acct", "/checkout/cart.jsp?source=topnav");
      }
    }
    
    final String type = request.getParameter("type");
    final String device_type = request.getParameter("device_type");
    final String mobile_id = request.getParameter("mobile_id");
    final String isApp = request.getParameter("x-isapp");
    
    final HttpSession session = request.getSession();
    
    if (!StringUtilities.isEmpty(type)) {
      session.setAttribute("type", type);
    }
    
    if (!StringUtilities.isEmpty(device_type)) {
      session.setAttribute("device_type", device_type);
    }
    
    if (!StringUtilities.isEmpty(mobile_id)) {
      session.setAttribute("mobile_id", mobile_id);
    }
    
    if (!StringUtilities.isEmpty(isApp)) {
      session.setAttribute("isApp", isApp);
    }
    final List<NMCreditCard> orderCreditCards = order.getCreditCards();
    if (NmoUtils.isNotEmptyCollection(orderCreditCards)) {
        final NMCreditCard paymentGroup = orderCreditCards.get(0);
        if(paymentGroup.isPayPal()){
            pageModel.setPayPalLoggedIn(true);
         }
    }
    // Data Dictionary Attributes population.
    resetPropertiesForRecentlyChangedCommerceItem(order);
    tmsMessageContainer.setMessages(tmsMessageList);
    dataDictionaryUtils.processTMSDataDictionaryAttributes(pageDefinition, order, profile, pageModel, tmsMessageContainer);
    // clear the recently changed properties from the order as these properties needs to be re-populated for next cart action.
    order.getRecentlyChangedCommerceItems().clear();
    order.setChangeToPickupInStore(false);
    return true;
  }
  
  /**
   * This method is used to reset the properties for the recentlyChangedCommerceItems
   * 
   * @param order
   */
  private void resetPropertiesForRecentlyChangedCommerceItem(final NMOrderImpl order) throws CommerceItemNotFoundException, InvalidParameterException {
    NMCommerceItem commerceItem;
    final NMPricingTools pricingTools = CommonComponentHelper.getPricingTools();
    final List<RecentlyChangedCommerceItem> recentlyChangedCommerceItems = order.getRecentlyChangedCommerceItems();
    if (null != recentlyChangedCommerceItems) {
      for (final RecentlyChangedCommerceItem recentlyChangedCommerceItem : recentlyChangedCommerceItems) {
        final String recentlyChangedCommerceItemId = recentlyChangedCommerceItem.getCommerceItemId();
        // if commerceId is not null and commerceId exists in order
        if (null != recentlyChangedCommerceItemId && order.containsCommerceItem(recentlyChangedCommerceItemId)) {
          commerceItem = (NMCommerceItem) order.getCommerceItem(recentlyChangedCommerceItem.getCommerceItemId());
          if (null != commerceItem) {
            recentlyChangedCommerceItem.setCartChangeProductPrice(pricingTools.round(commerceItem.getPriceInfo().getAmount() / commerceItem.getQuantity()));
            recentlyChangedCommerceItem.setCartProductReplenishTime(commerceItem.getSelectedInterval());
          }
        }
      }
    }
  }
  
  /**
   * Determine oos configured items.
   * 
   * @param pageModel
   *          the page model
   * @param commerceItemsList
   *          the commerce items list
   * @param countryCode
   */
  private void displayMsgForOOSConfiguredItems(final CheckoutPageModel pageModel, final List<NMCommerceItem> commerceItemsList, final String countryCode) {
    final Map<String, String> messageForOOSConfiguredItems = new HashMap<String, String>();
    final ConfiguratorUtils configuratorUtils = CommonComponentHelper.getConfiguratorUtils();
    final Iterator<NMCommerceItem> nmCItem = commerceItemsList.iterator();
    while (nmCItem.hasNext()) {
      final NMCommerceItem nmCommerceItem = nmCItem.next();
      try {
        if (!StringUtils.isBlank(nmCommerceItem.getConfigurationKey()) && configuratorUtils.evaluateSelectionSet(nmCommerceItem, countryCode)) {
          messageForOOSConfiguredItems.put(nmCommerceItem.getId(), ITEM_NOT_AVAILABLE_MSG);
        }
      } catch (final ConfiguratorException cfe) {
        CommonComponentHelper.getLogger().logError("Error in Configurator App System :" + cfe.getMessage());
      }
    }
    pageModel.setUnavailableMsgForConfiguredItem(messageForOOSConfiguredItems);
  }
  
  private void updateBopsSpecificValues(final CheckoutPageModel pageModel, final OrderManager orderMgr, final NMOrderImpl order, final ShoppingCartHandler cart) {
    final List<NMCommerceItem> commerceItems = order.getCommerceItems();
    if (!commerceItems.isEmpty()) {
      for (final NMCommerceItem nmCommerceItem : commerceItems) {
        final String pickupStoreNo = nmCommerceItem.getPickupStoreNo();
        if (StringUtilities.isNotEmpty(pickupStoreNo)) {
          final StoreSkuInventoryUtils skuUtils = CommonComponentHelper.getStoreSkuInventoryUtils();
          final ItemAvailabilityLevel inventoryLevel = skuUtils.getSkuInventoryByStoreNum(nmCommerceItem.getSkuNumber(), pickupStoreNo, nmCommerceItem.getQuantity() + "");
          if (!inventoryLevel.isInStore()) {
            // the sku is not at a store, set ship to store flag for BOSS - Buy Online Ship to Store
            nmCommerceItem.setShipToStoreFlg(true);
          }
        }
      }
    }
    
  }
  
  private void updateTagInfo(final NMOrderImpl order) {
    final ResultBean resultBean = CheckoutComponents.getResultBeanContainer(getRequest()).getResultBean();
    if (resultBean != null) {
      final GenericTagInfo tagInfo = GenericTagInfo.getInstance(getRequest());
      final Map<String, CartProductInfo> cartProductMap = tagInfo.getCartProductMap();
      // process all commerce items
      @SuppressWarnings("unchecked")
      final List<NMCommerceItem> commerceItems = order.getCommerceItems();
      if (!commerceItems.isEmpty()) {
        for (final NMCommerceItem nmCommerceItem : commerceItems) {
          final CartProductInfo pci = new CartProductInfo();
          pci.setCommerceId(nmCommerceItem.getId());
          pci.setProductId(nmCommerceItem.getProductId());
          pci.setPrice(nmCommerceItem.getPriceInfo().getAmount() / nmCommerceItem.getQuantity());
          pci.setOriginalQty(nmCommerceItem.getQuantity());
          pci.setCurrentQty(nmCommerceItem.getQuantity());
          pci.setPwpItem(nmCommerceItem.isPwpItem());
          cartProductMap.put(nmCommerceItem.getId(), pci);
        }
      }
      
      CartProductInfo pci = cartProductMap.get(resultBean.getCommerceItemId());
      
      final ResultBeanEventType type = resultBean.getEventType();
      if (type != null) {
        switch (type) {
          case ADD_SFL_ITEM:
          case REMOVE_ORDER_ITEM:
            if (pci == null) {
              pci = new CartProductInfo();
              pci.setCommerceId(resultBean.getCommerceItemId());
              pci.setProductId(resultBean.getProductId());
              pci.setPrice(resultBean.getPrice() / resultBean.getOriginalQuantity());
              pci.setOriginalQty(resultBean.getOriginalQuantity());
              pci.setCurrentQty(0);
              // pci.setPwpItem(nmCommerceItem.isPwpItem());
              cartProductMap.put(pci.getCommerceId(), pci);
            } else {
              pci.setOriginalQty(resultBean.getOriginalQuantity());
            }
            tagInfo.setActionType(GenericTagInfo.PARTIAL_REMOVE);
          break;
          case MOVE_SFL_TO_CART:
            if (pci != null) {
              pci.setOriginalQty(resultBean.getOriginalQuantity());
            }
            tagInfo.setActionType(GenericTagInfo.PARTIAL_ADD);
          break;
          case UPDATE_ORDER_ITEM:
            if (pci != null) {
              pci.setOriginalQty(resultBean.getOriginalQuantity());
            }
            
            if (resultBean.getQuantity() > resultBean.getOriginalQuantity()) {
              tagInfo.setActionType(GenericTagInfo.PARTIAL_ADD);
            } else {
              tagInfo.setActionType(GenericTagInfo.PARTIAL_REMOVE);
            }
          break;
        }
      }
    }
  }
  
}
