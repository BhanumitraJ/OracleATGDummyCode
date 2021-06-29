package com.nm.commerce.checkout;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.Cookie;

import org.w3.namespace.btpresponse.ErrorDocument;

import atg.commerce.order.CommerceItemRelationship;
import atg.commerce.order.OrderManager;
import atg.commerce.order.ShippingGroup;
import atg.core.util.StringUtils;
import atg.droplet.DropletException;
import atg.nucleus.Nucleus;
import atg.repository.RepositoryItem;
import atg.servlet.DynamoHttpServletRequest;
import atg.servlet.DynamoHttpServletResponse;
import atg.servlet.ServletUtil;

import com.nm.ajax.checkout.utils.ComponentUtils;
import com.nm.collections.NMPromotion;
import com.nm.commerce.NMCommerceItem;
import com.nm.commerce.NMOrderHolder;
import com.nm.commerce.NMProfile;
import com.nm.commerce.catalog.NMProduct;
import com.nm.commerce.catalog.NMRestrictionCode;
import com.nm.commerce.catalog.NMSuite;
import com.nm.commerce.checkout.beans.LoginBean;
import com.nm.commerce.checkout.beans.ResultBean;
import com.nm.commerce.checkout.beans.ResultBeanEventType;
import com.nm.commerce.checkout.beans.ResultBeanHelper;
import com.nm.commerce.order.NMOrderManager;
import com.nm.commerce.pricing.Markdown;
import com.nm.commerce.pricing.NMItemPriceInfo;
import com.nm.commerce.pricing.NMPricingTools;
import com.nm.commerce.promotion.NMOrderImpl;
import com.nm.common.NMBorderFreeGetQuoteResponse;
import com.nm.components.CommonComponentHelper;
import com.nm.discoverableservices.FiftyOneUtilsDiscoverable;
import com.nm.discoverableservices.NMDiscoverableServiceProxyFactory;
import com.nm.droplet.LinkshareCookie;
import com.nm.droplet.NMProtocolChange;
import com.nm.formhandler.NMProfileFormHandler;
import com.nm.formhandler.ShoppingCartHandler;
import com.nm.international.fiftyone.FiftyOneCommunicationException;
import com.nm.international.fiftyone.FiftyOneException;
import com.nm.international.fiftyone.FiftyOneMapper;
import com.nm.international.fiftyone.FiftyOneTimeoutException;
import com.nm.international.fiftyone.data.BasketDetails;
import com.nm.international.fiftyone.data.BasketItem;
import com.nm.international.fiftyone.data.Body;
import com.nm.international.fiftyone.data.COPShippingOptions;
import com.nm.international.fiftyone.data.DeliveryServiceType;
import com.nm.international.fiftyone.data.DomesticBasket;
import com.nm.international.fiftyone.data.Identity;
import com.nm.international.fiftyone.data.MerchantCheckout;
import com.nm.international.fiftyone.data.OrderDetails;
import com.nm.international.fiftyone.data.Sender;
import com.nm.international.fiftyone.data.SessionDetails;
import com.nm.international.fiftyone.data.TransferBuyerDomesticSession;
import com.nm.sitemessages.Message;
import com.nm.sitemessages.MessageDefs;
import com.nm.utils.AssociatePinUtil;
import com.nm.utils.BrandSpecs;
import com.nm.utils.CookieUtil;
import com.nm.utils.LinkedEmailUtils;
import com.nm.utils.LocalizationUtils;
import com.nm.utils.NmoUtils;
import com.nm.utils.PromotionsHelper;
import com.nm.utils.StringUtilities;
import com.nm.utils.SystemSpecs;
import com.nm.utils.fiftyone.FiftyOneUtils;

/**
 * Package access - outside of this package, methods should be accessed through CheckoutAPI.
 */
/* package */class LoginUtil {
  
  private static LoginUtil INSTANCE; // avoid static initialization
  
  // Added by INSSK for Request # 28736 on Jun 21, 2011 - Starts here
  private static final NMPricingTools pricingTools = (NMPricingTools) Nucleus.getGlobalNucleus().resolveName("/atg/commerce/pricing/PricingTools");
  // Added by INSSK for Request # 28736 on Jun 21, 2011 - Ends here
  
  // nmve1 - 33089 International project
  private static final FiftyOneUtils fiftyOneUtils = ((FiftyOneUtils) Nucleus.getGlobalNucleus().resolveName("/nm/utils/fiftyone/FiftyOneUtils"));
  private static final LocalizationUtils localizationUtils = ((LocalizationUtils) Nucleus.getGlobalNucleus().resolveName("/nm/utils/LocalizationUtils"));
  private static final NMProtocolChange protocolChange = (NMProtocolChange) Nucleus.getGlobalNucleus().resolveName("/atg/dynamo/droplet/ProtocolChange");
  private String affiliateNetworkId = fiftyOneUtils.getAffiliateNetworkId();
  private String affiliateMerchantId = fiftyOneUtils.getAffiliateMerchantId();
  
  // private constructor enforces singleton behavior
  private LoginUtil() {}
  
  public static synchronized LoginUtil getInstance() {
    INSTANCE = (INSTANCE == null) ? new LoginUtil() : INSTANCE;
    return INSTANCE;
  }
  
  public ResultBean handleLogin(ShoppingCartHandler cart, LoginBean loginBean, NMProfileFormHandler profileFormHandler) throws Exception {
    
    // This is a non-standard way to retrieve the request and response, but for API purposes this is the how we will proceed.
    DynamoHttpServletRequest request = ServletUtil.getCurrentRequest();
    DynamoHttpServletResponse response = ServletUtil.getCurrentResponse();
    
    NMProfile profile = (NMProfile) profileFormHandler.getProfile();
    NMOrderImpl order = cart.getNMOrder();
    OrderManager orderMgr = cart.getOrderManager();
    
    ResultBean result = new ResultBean();
    // boolean userLoggedInAtCheckout = false;
    if (!loginBean.isAnonymous()) {
      AssociatePinUtil.clearAssociateSessionInfo(request);
    } else {
      if (null != request.getSession().getAttribute(AssociatePinUtil.ASSOCIATE_MODE)) {
        profileFormHandler.handleHardLogout(request, response);
      }
      request.getSession().removeAttribute(AssociatePinUtil.CONFIRMED_ASSOCIATE);
    }
    
    // reset cookie for merge cart
    Cookie cookie = CookieUtil.getCookie("showMergeWindowCount", request);
    if (cookie != null) {
      String cookieValue = cookie.getValue();

    if (!StringUtilities.isNullOrEmpty(cookieValue)) {
      CookieUtil.setCookie(request, response, "showMergeWindowCount", null);
      }
    }

    
    // GetQuote call start
    /*
     * if ( CheckoutAPI.isInternationalSession( profile ) ) { com.nm.international.fiftyone.checkoutapi.Message borderFreeMsg=borderFreeApiUtil.callgetQuote(profile, order);
     * getQuoteResponseTranslator.convertToValueObject(order, profile, borderFreeMsg);
     * 
     * }
     */
    
    // GetQuote call End
    
    // determine if this is an international checkout session; if so, bypass the regular checkout flow
    // if ( CheckoutAPI.isInternationalSession( profile ) ) {
    // initiate theM Buyer Transfer Post(BTP) to return the FiftyOne Envoy Flash object,
    // which gets embedded into our international checkout page, thus starting the international session.
    // result.getMessages().addAll( getInternationalSession( request, response ) );
    // } else {
    
    if (loginBean.isAnonymous()) {
      if (anonymousLogin(request, response, profileFormHandler)) {
        result.getMessages().addAll(commonLogin(request, response, cart, profileFormHandler));
        ResultBeanHelper.login(result, (NMProfile) profileFormHandler.getProfile(), ResultBeanEventType.ANONYMOUS_LOGIN);
        // OmnitureUtils.getInstance().anonymousLogin(response, profile, order);
      } else {
        Message message = new Message();
        message.setMsgText("");
        message.setError(true);
        result.getMessages().add(message);
      }
    } else {
      ArrayList<Message> messages = new ArrayList<Message>();
      if (registeredLogin(loginBean.getEmail(), loginBean.getPassword(), request, response, profileFormHandler, messages)) {
        order = cart.getNMOrder();
        if (!CheckoutAPI.isEmptyCart(order)) {
          // userLoggedInAtCheckout = true;
          result.getMessages().addAll(commonLogin(request, response, cart, profileFormHandler));
          /*
           * response.setUpdateAccountLink( Boolean.TRUE ); userLoggedInAtCheckout = true;
           */
        }
        /* OmnitureUtils.getInstance().successfulLogin(response, profile, order); */
        
        ResultBeanHelper.login(result, (NMProfile) profileFormHandler.getProfile(), ResultBeanEventType.REGISTERED_LOGIN);
        
      } else {
        Message message = new Message();
        message.setMsgText("");
        message.setError(true);
        result.getMessages().add(message);
        result.getMessages().addAll(messages);
      }
    }
    // }
    
    /*
     * GenericTagInfo tagInfo = GenericTagInfo.getInstance( this.request ); tagInfo.setActionType( GenericTagInfo.LOGIN ); tagInfo.setLoginType( userLoggedInAtCheckout ? GenericTagInfo.ACCOUNT_LOGIN :
     * GenericTagInfo.ANONYMOUS_LOGIN ); response.setMarketingHtml( this.invokeNamedTemplate( NamedPaths.AJAX_MARKETING_TAGS ) );
     */
    order = cart.getNMOrder();
    orderMgr.updateOrder(order);
    
    return result;
  }
  
  private boolean anonymousLogin(DynamoHttpServletRequest request, DynamoHttpServletResponse response, NMProfileFormHandler profileFormHandler) throws Exception {
    profileFormHandler.handleCheckoutWithoutSignIn(request, response);
    
    NMProfile profile = (NMProfile) profileFormHandler.getProfile();
    RepositoryItem repositoryItem = (RepositoryItem) profile.getPropertyValue("shippingAddress");
    
    if (null == repositoryItem || null == repositoryItem.getPropertyValue("address1")) {
      profileFormHandler.resetCommerceItemsToDefaultAddresses(request, response);
    }
    
    return !profileFormHandler.getFormError();
  }
  
  private boolean registeredLogin(String email, String password, DynamoHttpServletRequest request, DynamoHttpServletResponse response, NMProfileFormHandler profileFormHandler,
          List<Message> messageList) throws Exception {
    boolean returnValue = false;
    
    ArrayList<Message> messages = new ArrayList<Message>();
    
    String validationResult = ProfileUtil.getInstance().validateEmailandFormat(email, messages);
    
    if (null != validationResult) {
      
      final ShoppingCartHandler cart = CheckoutComponents.getCartHandler(request);
      // need to set this flag so if a Registered User who started the session anon
      // will keep any entered promocodes
      // he entered during his session IF he signs in...
      NMOrderHolder orderHolder = (NMOrderHolder) cart.getOrderHolder();
      if (null != orderHolder) {
        orderHolder.setNeedingCartUtilityUpdate(false);
      }
      
      profileFormHandler.setLoginInfo(email, password);
      profileFormHandler.handleCheckoutLogin(request, response);
      
      returnValue = !profileFormHandler.getFormError();
      
      /* **********************************************************************************************************
       * if we have identified errors from the login that need to be reported back to the user we need to convert those messages to Message objects so they can be passed back to the front end
       * *********************************************************************************************************
       */
      if (!returnValue) {
        @SuppressWarnings("unchecked")
        Collection<DropletException> exceptions = profileFormHandler.getFormExceptions();
        Iterator<DropletException> iterator = exceptions.iterator();
        BrandSpecs brandSpecs = (BrandSpecs) request.resolveName("/nm/utils/BrandSpecs");
        String brandName = brandSpecs.getBrandName();
        
        while (iterator.hasNext()) {
          Message message = null;
          DropletException exception = (DropletException) iterator.next();
          
          String errorCode = exception.getErrorCode();
          
          if (null != errorCode) {
            if (errorCode.equals(NMProfileFormHandler.MSG_INVALID_PASSWORD)) {
              message = (Message) MessageDefs.getMessage(MessageDefs.MSG_LoginInvalidPassword);
              message.setMsgText("The Password supplied was invalid");
              messages.add(message);
              
            } else if (errorCode.equals(NMProfileFormHandler.MSG_INVALID_LOGIN)) {
              // SystemSpecs systemSpecs = (SystemSpecs) request.resolveName( "/nm/utils/SystemSpecs" );;
              // String productionSystemCode = systemSpecs.getProductionSystemCode();
              message = (Message) MessageDefs.getMessage(MessageDefs.MSG_LoginUnknowEmail);
              message.setMsgText("The e-mail is not yet registered with " + brandName + ". Registering is quick & easy and the most " + "convenient way to shop. <a href=\"/account/register.jsp"
                      + "\">Register Now *****</a>");
              messages.add(message);
            }
          }
          
          if (null == message) {
            message = MessageDefs.getMessage(MessageDefs.MSG_LoginInvalidPassword);
          }
        }
        
        /* NEED TO ADD MESSAGES TO CURRENT MESSAGELIST */
        
        messageList.addAll(messages);
      }
    } else {
      
      /* NEED TO ADD MESSAGES TO CURRENT MESSAGELIST */
      
      messageList.addAll(messages);
    }
    return returnValue;
  }
  
  private List<Message> commonLogin(DynamoHttpServletRequest request, DynamoHttpServletResponse response, ShoppingCartHandler cart, NMProfileFormHandler profileFormHandler) throws Exception {
    NMOrderImpl order = cart.getNMOrder();
    final NMOrderManager orderMgr = (NMOrderManager) cart.getOrderManager();
    NMProfile profile = (NMProfile) profileFormHandler.getProfile();
    
    // update linked email
    LinkedEmailUtils.getInstance().updateLinkedEmailOrder(order);
    
    PaymentUtil paymentUtil = PaymentUtil.getInstance();
    OrderUtil orderUtil = OrderUtil.getInstance();
    ShippingUtil shippingUtil = ShippingUtil.getInstance();
    CartUtil cartUtil = CartUtil.getInstance();
    RepositoryItem repositoryItem = (RepositoryItem) profile.getPropertyValue("shippingAddress");
    
    // mobile checkout can only have one shipping group
    if (repositoryItem == null || repositoryItem.getPropertyValue("address1") == null) {
      profileFormHandler.resetCommerceItemsToDefaultAddresses(request, response);
    }
    
    order.setValidateCountry(true);
    paymentUtil.addDefaultPaymentGroup(order, profile, orderMgr);
    // paymentUtils.clearCid( order ); // reset cid when customer enters checkout section //method not available yet and most likely not needed at this point
    orderUtil.removeEmptyShippingGroups(order, orderMgr);
    orderUtil.mergeSplitCommerceItems(order, orderMgr, false, false, false);
    /* User preferred country is not US or CA, we dont need to refresh service levels */
    String countryPref = profile.getCountryPreference();
    if (!StringUtils.isBlank(countryPref) && ("US".equals(countryPref) || "CA".equals(countryPref))) {
      shippingUtil.refreshShippingGroupServiceLevel(order);
    }
    
    final List<Message> messages = new ArrayList<Message>();
    
    if ((ServletUtil.getCurrentRequest().getSession().getAttribute("showMergeCartWindow") == null) || (!(Boolean) ServletUtil.getCurrentRequest().getSession().getAttribute("showMergeCartWindow"))) {
      messages.addAll(InventoryUtil.getInstance().checkCmosStockLevel(cart, false, profile));
    }

    // if ( CheckoutReq.SERVICE_ORIGIN != checkoutReq.getOrigin() && !cartUtils.isMobile( checkoutReq.getOrigin() ) ) {
    // OmnitureUtils.getInstance().startCheckout( checkoutResp, profile, order );
    // }
    // set state after omniture is genereated
    order.setSpcState(NMOrderImpl.SPC_STATE_CHECKOUT); // not sure if this needs to be done
    
    if (CheckoutAPI.isEmptyCart(order)) {
      // RETURN SOMETHING FOR AN EMPTY CART
      orderMgr.updateOrder(order);
      return messages;
    }
    
    // needs ShoppingCartHandler cart
    shippingUtil.validateShipToCountries(cart, messages);
    List<Message> promoMessages = PricingUtil.getInstance().repriceOrder(cart, profile, SnapshotUtil.CHANGE_SHIP_DESTINATION);
    messages.addAll(promoMessages);
    
    CheckoutComponents.getPromoUtils().recalculateGwpSelectPromoMap(order);
    cartUtil.checkDeclinedGwpSelects(order, request);
    
    order.setValidateLimitPromoUse(false);
    
    orderMgr.updateOrder(order);
    
    // removeNonClientMessages( checkoutResp );
    
    return messages;
    
  }
  
  /**
   * Validate the cart againest borderfree service.
   * 
   * @param profile
   *          NMProfile object
   * @param order
   *          NMOrder object
   * @return validation error messages, if any.
   */
  public List<Message> validateCartForBorderFreeCheckout(final NMProfile profile, final NMOrderImpl order) {
    List<Message> messages = new ArrayList<Message>();
    NMBorderFreeGetQuoteResponse response = fiftyOneUtils.getInternationalCheckoutAPIUtil().callgetQuote(profile, order, null, true, null);
    if (response != null) {
      if (response.isIncompleteQuote() || (response.getRestrictedItemList() != null && !response.getRestrictedItemList().isEmpty())) {
        Message message = new Message();
        message.setMsgText(response.getErrorMessage());
        messages.add(message);
      }
    }
    return messages;
  }
  
  /*
   * public List<Message> getInternationalSession( DynamoHttpServletRequest request, DynamoHttpServletResponse response ) { final ShoppingCartHandler cart = CheckoutComponents.getCartHandler( request
   * ); final NMOrderImpl order = CheckoutComponents.getCurrentOrder( request ); final NMProfile profile = CheckoutComponents.getProfile( request ); final OrderManager orderMgr =
   * cart.getOrderManager(); List<Message> messages = new ArrayList<Message>();
   * 
   * try { // trigger Buyer Transfer Post(BTP) to FiftyOne String envoyUrl = buyerTransferPost( profile, order, orderMgr, request, response ); if ( StringUtilities.isNotEmpty( envoyUrl ) ) { // the
   * envoyUrl will be used by the FiftyOne Envoy(embedded Flash object) request.getSession().setAttribute( "envoyUrl", envoyUrl ); request.getSession().setAttribute( "pathToEnvoyJS",
   * fiftyOneUtils.getPathToEnvoyJS() ); request.getSession().setAttribute( "paypalCartUrl", fiftyOneUtils.getPaypalCartUrl() ); request.getSession().setAttribute( "xiRedirectUrl",
   * protocolChange.getSecureURL( fiftyOneUtils.getSiteUrl() + fiftyOneUtils.getXiRedirectUrl(), request, response ) ); request.getSession().setAttribute( "pathToXISWF", fiftyOneUtils.getPathToXISWF()
   * ); request.getSession().setAttribute( "clearCartUrl", fiftyOneUtils.getClearCartUrl() ); } else { CheckoutComponents.getConfig().logError(
   * "Buyer Transfer Post(BTP) Error: the fullEnvoyURL is empty. The international session will not be started." ); Message message = new Message(); message.setMsgText( "" ); message.setError( true );
   * messages.add( message ); } } catch ( Exception e ) { e.printStackTrace(); CheckoutComponents.getConfig().logError( "Buyer Transfer Post(BTP) Error: ", e ); Message message = new Message();
   * message.setMsgText( "" ); message.setError( true ); messages.add( message ); } return messages; }
   */
  
  /**
   * The method generates and posts an XML message used to share the customer's data with FiftyOne. The creation and transfer of the Buyer Transfer Post is triggered when the customer clicks the
   * international Checkout button. The generated message contains the following information: Basket details - content of the customer's shopping cart such as SKUs, quantities, USD prices, etc. Order
   * details - order value, order level discount, etc. Shipping options - standard and express shipping and handling costs, etc. Session details - customer's IP, preferred currency, shipping country,
   * basket URL, etc. 33089 - International project
   * 
   * @author nmve1
   * 
   * @param profile
   * @param order
   * @param orderMgr
   * @param request
   * @param response
   * @throws Exception
   * @return fullEnvoyURL
   */
  public String buyerTransferPost(final NMProfile profile, final NMOrderImpl order, final OrderManager orderMgr, final DynamoHttpServletRequest request, final DynamoHttpServletResponse response)
          throws Exception {
    CheckoutComponents.getConfig().logDebug("Start CheckoutService buyerTransferPost");
    
    atg.service.idgen.IdGenerator idGenerator = (atg.service.idgen.IdGenerator) Nucleus.getGlobalNucleus().resolveName("/atg/dynamo/service/IdGenerator");
    
    String fullEnvoyURL = null;
    
    final com.nm.international.fiftyone.data.Message message = new com.nm.international.fiftyone.data.Message();
    final com.nm.international.fiftyone.data.Header header = new com.nm.international.fiftyone.data.Header();
    final Body body = new Body();
    final MerchantCheckout merchantCheckout = new MerchantCheckout();
    final TransferBuyerDomesticSession transferBuyerDomesticSession = new TransferBuyerDomesticSession();
    final DomesticBasket domesticBasket = new DomesticBasket();
    
    @SuppressWarnings("unchecked")
    List<ShippingGroup> shippingGroupsLst = order.getShippingGroups();
    try {
      
      for (ShippingGroup shippingGroup : shippingGroupsLst) {
        // Get the Line Items within this shipTo.
        @SuppressWarnings("unchecked")
        List<CommerceItemRelationship> commerceItemRelationshipsLst = shippingGroup.getCommerceItemRelationships();
        NMCommerceItem commerceItem = null;
        BasketItem basketItem = null;
        int basketItemID = 0;
        final BasketItem[] basketItems = new BasketItem[commerceItemRelationshipsLst.size()];
        CheckoutComponents.getConfig().logDebug(" Nbr of commerce items in this Buyer Transfer Post(BTP): " + commerceItemRelationshipsLst.size());
        
        for (int i = 0; i < commerceItemRelationshipsLst.size(); i++) {
          commerceItem = (NMCommerceItem) commerceItemRelationshipsLst.get(i).getCommerceItem();
          basketItem = new BasketItem();
          NMItemPriceInfo itemPriceInfo = (NMItemPriceInfo) commerceItem.getPriceInfo();
          RepositoryItem repProd = (RepositoryItem) commerceItem.getAuxiliaryData().getProductRef();
          final NMProduct nmProduct = new NMProduct(repProd.getRepositoryId());
          
          // populate BasketItem
          basketItemID++;
          basketItem.setBasketItemID(basketItemID);
          basketItem.setMerchantSKU(NmoUtils.invalidCharacterCleanup((String) commerceItem.getPropertyValue("cmosSKUId")));
          basketItem.setProductName(NmoUtils.invalidCharacterCleanup((String) repProd.getPropertyValue("cmDesignerName")) + " "
                  + NmoUtils.invalidCharacterCleanup((String) repProd.getPropertyValue("displayName")));
          basketItem.setProductURL("");
          basketItem.setProductDescription(""); // not required
          basketItem.setProductQuantity((Long) commerceItem.getPropertyValue("quantity"));
          basketItem.setProductExtraShipping((Double) nmProduct.getIntParentheticalCharge());
          basketItem.setProductExtraHandling(0.00); // not required
          basketItem.setProductVAT(0.00); // future field
          basketItem.setProductInventory1("In Stock"); // not required. A description of the inventory status (for example "In Stock", "Available", or "Backordered"). It's always going to be
                                                       // "In Stock"
          basketItem.setProductInventory2(""); // future field
          basketItem.setProductColor((null == commerceItem.getProdVariation1()) ? "" : commerceItem.getProdVariation1());
          basketItem.setProductSize((null == commerceItem.getProdVariation2()) ? "" : commerceItem.getProdVariation2());
          
          // if item is subject to fish and wildlife fee, display additional messaging to customer
          // must use the Canadian restriction codes because cmos will only send codes for US and Canada
          List<NMRestrictionCode> restrictionCodes = nmProduct.getRestrictionCodes("CA");
          String productAttributes = "";
          
          if (restrictionCodes != null) {
            Iterator<NMRestrictionCode> rCodeIterator = restrictionCodes.iterator();
            
            while (rCodeIterator.hasNext()) {
              NMRestrictionCode rCode = rCodeIterator.next();
              
              if (rCode.getRestrictionCode().equals("F")) {
                // Fix for defect # 41410
                productAttributes = fiftyOneUtils.getFishAndWildlifeMessage1() + " " + fiftyOneUtils.getFishAndWildlifeMessage2();
              }
            }
          }
          
          basketItem.setProductAttributes(productAttributes);
          
          String suiteId = commerceItem.getSuiteId();
          String mgUrl = null;
          
          if (nmProduct.getAuxiliaryMedia() != null) {
            if (nmProduct.getAuxiliaryMedia().get("mg") != null) {
              mgUrl = (String) ((RepositoryItem) nmProduct.getAuxiliaryMedia().get("mg")).getPropertyValue("url");
            }
          }
          
          if ((mgUrl == null) || (mgUrl.trim().equals(""))) {
            if ((suiteId != null) && (!suiteId.trim().equals(""))) {
              RepositoryItem suiteRI = CommonComponentHelper.getProdSkuUtil().getSuiteRepositoryItem(suiteId);
              
              if (suiteRI != null) {
                NMSuite nmSuite = new NMSuite(suiteRI);
                
                if (nmSuite.getAuxiliaryMedia() != null) {
                  if (nmSuite.getAuxiliaryMedia().get("mg") != null) {
                    mgUrl = (String) ((RepositoryItem) nmSuite.getAuxiliaryMedia().get("mg")).getPropertyValue("url");
                  }
                }
              }
            }
          }
          
          if (mgUrl != null) {
            basketItem.setProductImageURL(getSystemSpecs(request).getSiteUrl().replace("http:", "https:") + mgUrl);
          } else {
            basketItem.setProductImageURL("");
          }
          
          basketItem.setProductRemoveURL(""); // future field
          basketItem.setProductQtyChangeURL(""); // future field
          if (itemPriceInfo != null) {
            basketItem.setProductListPrice(new Double(itemPriceInfo.getListPrice()));
            if ((basketItem.getProductListPrice() * basketItem.getProductQuantity()) != itemPriceInfo.getAmount()) {
              basketItem.setProductDiscount(pricingTools.round(new Double(((basketItem.getProductListPrice() * basketItem.getProductQuantity()) - itemPriceInfo.getAmount())
                      / basketItem.getProductQuantity())));
            } else {
              basketItem.setProductDiscount(0.00);
            }
          } else {
            // this logic exists in the NMOrderFulfiller, but may not belong here, so we'll just throw an exception
            throw new FiftyOneException("The PriceInfo is null in the repository view.");
          }
          
          basketItem.setProductSalePrice(pricingTools.round(basketItem.getProductListPrice() - basketItem.getProductDiscount())); // The final price of the product (equal to ProductListPrice -
                                                                                                                                  // ProductDiscount)
          
          // zero dollar items are not allowed to ship internationally, this evaluation should never be true under normal conditions
          if ((basketItem.getProductSalePrice() == null) || (basketItem.getProductSalePrice() <= 0.0)) {
            throw new Exception("Basket item cannot have a zero dollar sale price.");
          }
          
          // use the custom data field to send an xml message for passback in the po file
          // the goal is to ensure the po file contains all data required to fulfill the order
          com.nm.international.fiftyone.data.NmCustomData nmCustomData = new com.nm.international.fiftyone.data.NmCustomData();
          
          nmCustomData.setServiceLevelCode("SL3");
          nmCustomData.setTransactionType("A");
          nmCustomData.setCmosCatalogId(commerceItem.getCmosCatalogId());
          nmCustomData.setCmosCatalogItem(commerceItem.getCmosItemCode());
          nmCustomData.setAtgLineItemId(commerceItem.getId());
          nmCustomData.setAtgSkuId(commerceItem.getCatalogRefId());
          
          // generate a unique id to be used in the fiftyone order repository, this id should not be the commerce item id, this guarantees it will be unique in cmos
          String uniqueLineItemId = NmoUtils.invalidCharacterCleanup((String) order.getPropertyValue("systemCode")) + idGenerator.generateStringId("commerceItem");
          nmCustomData.setExternalLineItemId(uniqueLineItemId);
          
          StringBuffer markDowns = new StringBuffer("");
          StringBuffer promotionCodes = new StringBuffer("");
          StringBuffer promotionKeys = new StringBuffer("");
          
          Map<String, Markdown> promosApplied = itemPriceInfo.getPromotionsApplied();
          
          if (promosApplied != null) {
            Set<String> keySet = promosApplied.keySet();
            Iterator<String> keySetIterator = keySet.iterator();
            
            while (keySetIterator.hasNext()) {
              String key = (String) keySetIterator.next();
              Markdown markdown = promosApplied.get(key);
              
              markDowns.append(pricingTools.round(markdown.getDollarDiscount()) + ",");
              promotionCodes.append(markdown.getUserEnteredPromoCode() + ",");
              promotionKeys.append(markdown.getPromoKey() + ",");
            }
          }
          
          // merge with non-markdown promotions (logic copied from NMOrderFulfiller)
          HashSet<String> promoKeysToBeApplied = new HashSet<String>();
          String promoName = (String) order.getPropertyValue("promoName");
          if (promoName != null && promoName.trim().length() > 0) {
            PromotionsHelper promotionsHelper = CommonComponentHelper.getPromotionsHelper();
            String[] promoKeys = promoName.split(",");
            for (String promoKey : promoKeys) {
              promoKey = promoKey.trim();
              if (promoKey.length() > 0 && promotionsHelper.promotionNeedsRollup(promoKey)) {
                promoKeysToBeApplied.add(promoKey);
              }
            }
            
            for (String promoKey : promoKeysToBeApplied) {
              promotionKeys.append(promoKey).append(",");
              
              String promoCodes = null;
              NMPromotion promotion = promotionsHelper.getPromotionFromPromoId(promoKey);
              if (promotion != null) {
                promoCodes = promotion.getPromoCodes();
              }
              
              if (!StringUtils.isEmpty(promoCodes)) {
                promotionCodes.append(promoCodes).append(",");
              } else {
                promotionCodes.append(",");
              }
              markDowns.append(",");
            }
          }
          
          nmCustomData.setMarkdown(markDowns.toString());
          nmCustomData.setPromotionCode(promotionCodes.toString());
          nmCustomData.setPromotionKey(promotionKeys.toString());
          
          com.nm.international.fiftyone.data.SpecialInstruction[] specialInstructions = null;
          Map<String, String> siCodes = commerceItem.getSpecialInstCodes();
          
          if (siCodes != null) {
            specialInstructions = new com.nm.international.fiftyone.data.SpecialInstruction[siCodes.size()];
            Set<String> keySet = siCodes.keySet();
            Iterator<String> keySetIterator = keySet.iterator();
            int index = 0;
            
            while (keySetIterator.hasNext()) {
              String key = keySetIterator.next();
              String value = siCodes.get(key);
              
              com.nm.international.fiftyone.data.SpecialInstruction si = new com.nm.international.fiftyone.data.SpecialInstruction();
              si.setInstruction(key);
              si.setResponse(value);
              specialInstructions[index] = si;
              
              index++;
            }
          } else {
            specialInstructions = new com.nm.international.fiftyone.data.SpecialInstruction[0];
          }
          
          nmCustomData.setSpecialInstructions(specialInstructions);
          
          basketItem.setCustomData(FiftyOneMapper.mapNmCustomDataToXml(nmCustomData));
          
          basketItems[i] = basketItem;
        }
        // populate BasketDetails
        final BasketDetails basketDetails = new BasketDetails();
        basketDetails.setBasketItems(basketItems);
        domesticBasket.setBasketDetails(basketDetails);
        
        // populate OrderDetails
        double totalProductSaleValue = 0.00, totalItemLevelDiscount = 0.00;
        final OrderDetails orderDetails = new OrderDetails();
        for (BasketItem item : basketItems) {
          totalProductSaleValue += item.getProductSalePrice() * item.getProductQuantity();
          totalItemLevelDiscount += item.getProductDiscount() * item.getProductQuantity();
        }
        totalProductSaleValue = pricingTools.round(totalProductSaleValue);
        totalItemLevelDiscount = pricingTools.round(totalItemLevelDiscount);
        // the total value of the products in the order (TotalProductSaleValue = ProductSalePrice * ProductQuantity summed over all Basket Items).
        // FiftyOne has maximum order-value and item-value limits, it will the customer know if the limit has been exceeded
        orderDetails.setTotalProductSaleValue(totalProductSaleValue);
        orderDetails.setTotalItemLevelDiscounts(totalItemLevelDiscount);
        // The total value of the order level discounts. This is different from product level discounts.
        // It is important to keep this field separate so that one can prorate the order discount to all items in the order (e.g., "Buy more than $100 and get a $20 discount").
        orderDetails.setTotalOrderLevelDiscounts(order.getPriceInfo().getDiscountAmount());
        orderDetails.setTotalDiscounts(pricingTools.round(order.getPriceInfo().getDiscountAmount() + totalItemLevelDiscount));
        orderDetails.setTotalProductVAT(0.00); // future FiftyOne element, needs to be set to 0.00
        orderDetails.setTotalServiceVAT(0.00); // future FiftyOne element, needs to be set to 0.00
        orderDetails.setTotalVAT(0.00); // future FiftyOne element, needs to be set to 0.00
        
        com.nm.international.fiftyone.data.NmCustomData nmCustomData = new com.nm.international.fiftyone.data.NmCustomData();
        nmCustomData.setAtgOrderId(NmoUtils.invalidCharacterCleanup((String) order.getPropertyValue("systemCode")) + order.getId());
        nmCustomData.setSystemCode((String) order.getPropertyValue("systemCode"));
        orderDetails.setCustomData(FiftyOneMapper.mapNmCustomDataToXml(nmCustomData));
        
        domesticBasket.setOrderDetails(orderDetails);
        
        transferBuyerDomesticSession.setDomesticBasket(domesticBasket);
        
        COPShippingOptions copShippingOptions = new COPShippingOptions();
        
        // populate DeliveryServiceType
        final DeliveryServiceType[] deliveryServiceTypes = new DeliveryServiceType[1];
        final DeliveryServiceType deliveryServiceType = new DeliveryServiceType();
        deliveryServiceType.setType("Standard");
        deliveryServiceType.setDeliveryServiceName("");
        deliveryServiceType.setShippingPrice(fiftyOneUtils.getDomesticShippingCharge());
        deliveryServiceType.setHandlingPrice(0.00);
        deliveryServiceType.setDeliveryPromiseMin(1);
        deliveryServiceType.setDeliveryPromiseMax(5);
        deliveryServiceType.setExtraInsurancePrice(0.00);
        deliveryServiceTypes[0] = deliveryServiceType;
        
        copShippingOptions.setDeliveryServiceTypes(deliveryServiceTypes);
        transferBuyerDomesticSession.setCopShippingOptions(copShippingOptions);
        
        // populate SessionDetails
        final SessionDetails sessionDetails = new SessionDetails();
        sessionDetails.setMerchantBaseCurrency(FiftyOneUtils.DEFAULT_CURRENCY_CODE);
        /*
         * The ID used to link the data post to FiftyOne with the buyer when they are transferred to the FiftyOne Embedded Checkout. This ID is also to be used when sending information back to your
         * site for the buyer to resume their session on your point-of-sale system. Sometimes international customers may go back to your site to change country or currency in the Context Chooser page
         * so that they can ship to a different country or buy in a different currency. When they re-enter the Envoy, they will see an incorrect (old) country and currency. You should use their
         * session ID plus the selected country code and currency code for FiftyOne's BuyerSessionID. Doing so ensures that for the same session, the same country and same currency (as selected by the
         * customer at the beginning of the session) will be cached. Changing country and/or currency in your Context Chooser page will create a new BuyerSessionID, thus discarding the prior cached
         * information.
         */
        sessionDetails.setBuyerIPAddress(request.getRemoteAddr());
        String countryPreference = profile.getCountryPreference();
        if (null != countryPreference && !countryPreference.trim().equals("")) {
          sessionDetails.setBuyerPreferredDeliveryCountry(countryPreference);
          CheckoutComponents.getConfig().logDebug(" BuyerPreferredDeliveryCountry: " + countryPreference);
        } else {
          sessionDetails.setBuyerPreferredDeliveryCountry(FiftyOneUtils.DEFAULT_COUNTRY_CODE);
        }
        sessionDetails.setBuyerPreferredDeliveryCountryPostalCode(""); // not required and we wouldn't have this
        String currencyPreference = profile.getCurrencyPreference();
        if (null != currencyPreference && !currencyPreference.trim().equals("")) {
          sessionDetails.setBuyerPreferredCurrency(currencyPreference);
          CheckoutComponents.getConfig().logDebug(" BuyerPreferredDeliveryCurrency: " + currencyPreference);
        } else {
          sessionDetails.setBuyerPreferredCurrency(FiftyOneUtils.DEFAULT_CURRENCY_CODE);
        }
        // if the preferred country is defaulted to 'US', the customer should not enter the Envoy.
        if (sessionDetails.getBuyerPreferredDeliveryCountry().equals("US")) {
          throw new Exception("The BuyerPreferredDeliveryCountry is US. The customer should not enter the Envoy.");
        }
        sessionDetails.setBuyerSessionID(sessionDetails.getBuyerPreferredDeliveryCountry() + "_" + sessionDetails.getBuyerPreferredCurrency() + "_" + profile.getId() + "_"
                + System.currentTimeMillis());
        CheckoutComponents.getConfig().logDebug(" BuyerSessionID: " + sessionDetails.getBuyerSessionID());
        // The unique ID for the customer's basket if different from the buyer session id.
        // Unique for every basket that gets posted to FiftyOne. We'll use the the buyer session id.
        sessionDetails.setBuyerBasketID(sessionDetails.getBuyerSessionID());
        long quoteId = localizationUtils.getQuoteIdForShopperCurrency(sessionDetails.getBuyerPreferredCurrency());
        if (quoteId <= 0) {
          throw new Exception("Invalid quote id");
        } else {
          CheckoutComponents.getConfig().logDebug(" QuoteIDUsed: " + quoteId);
          sessionDetails.setQuoteIDUsed(quoteId); // required in the E4X_FX_Rates table. Each Quote ID is mapped to a single currency
        }
        sessionDetails.setBuyerPreferredLanguage("EN"); // not required, but will default to English. The customer could change the language while in the Envoy
        // the URL to the customer's shopping bag
        // the customers will be returned to this URL to remove restricted items from the cart
        // BasketURL is also used to return customers to the cart page if they cancel a PayPal order
        sessionDetails.setBasketURL(protocolChange.getSecureURL(fiftyOneUtils.getSiteUrl() + fiftyOneUtils.getXiRedirectUrl(), request, response));
        
        String uniqueOrderId = NmoUtils.invalidCharacterCleanup((String) order.getPropertyValue("systemCode")) + idGenerator.generateStringId("order");
        String atgOrderId = NmoUtils.invalidCharacterCleanup((String) order.getPropertyValue("systemCode")) + order.getId();
        
        CheckoutComponents.getConfig().logDebug("uniqueOrderId: " + uniqueOrderId);
        CheckoutComponents.getConfig().logDebug("atgOrderId: " + atgOrderId);
        
        sessionDetails.setMerchantOrderId(uniqueOrderId);
        sessionDetails.setMerchantOrderRef(""); // not needed, per BSM
        // the ID of the Landed Cost Preconditioning rule that was configured in the Pricing Customization tool and was passed from the FiftyOne Embassy to the Ambassador.
        String lcpRuleId = null;
        lcpRuleId = localizationUtils.getLcpRuleID(sessionDetails.getBuyerPreferredDeliveryCountry(), localizationUtils.getFiftyOneUtils().getMerchantId());
        if (null != lcpRuleId) {
          sessionDetails.setLcpRuleID(Long.valueOf(lcpRuleId));
        } else {
          sessionDetails.setLcpRuleID(0);
        }
        CheckoutComponents.getConfig().logDebug(" LcpRuleID: " + sessionDetails.getLcpRuleID());
        sessionDetails.setHeadlineMessage1(""); // not needed
        sessionDetails.setHeadlineMessage2(""); // future field
        sessionDetails.setContextChooserPageURL(""); // future field
        sessionDetails.setUsCartStartPageURL(protocolChange.getSecureURL(fiftyOneUtils.getSiteUrl() + fiftyOneUtils.getXiRedirectUrl(), request, response)); // our U.S. cart start page
        sessionDetails.setMyAccountURL(""); // future field
        sessionDetails.setAlternatePaymentURL1(protocolChange.getSecureURL(fiftyOneUtils.getSiteUrl() + "/internationalCheckoutPaypal.jsp", request, response)); // the URL that will serve as the
                                                                                                                                                                 // ReturnURL during PayPal Checkouts. !
                                                                                                                                                                 // need to finalize, once the Envoy
                                                                                                                                                                 // international checkout page
                                                                                                                                                                 // embedding is done
        sessionDetails.setAlternatePaymentURL2(protocolChange.getSecureURL(fiftyOneUtils.getSiteUrl() + "/common/csr/images/header_logo.gif", request, response)); // the URL containing the custom
                                                                                                                                                                   // header image displayed in the
                                                                                                                                                                   // top-left corner of the PayPal
                                                                                                                                                                   // Login and Order Review pages. !
                                                                                                                                                                   // need to finalize, once the Envoy
                                                                                                                                                                   // international checkout page
                                                                                                                                                                   // embedding is done
        sessionDetails.setAlternatePaymentURL3(""); // future field
        sessionDetails.setAlternatePaymentURL4(""); // future field
        
        // Data fields needed for 51 to pass necessary data to LinkShare
        // Take care of null values
        if (affiliateNetworkId == null) {
          affiliateNetworkId = "";
        }
        if (affiliateMerchantId == null) {
          affiliateMerchantId = "";
        }
        
        try {
          final LinkshareCookie linkshare = ComponentUtils.getInstance().getLinkshareCookie();
          if (linkshare != null && linkshare.getEnableLSTracking()) {
            String affiliateSiteId = linkshare.getSiteId(request);
            if (affiliateSiteId == null) {
              sessionDetails.setAffliliateSiteID("");
            } else {
              sessionDetails.setAffliliateSiteID(String.format("%s|%s|%s", affiliateNetworkId, affiliateMerchantId, affiliateSiteId));
            }
            sessionDetails.setAffliliateSiteIDTimestamp(linkshare.getDateTimeStamp(request));
          }
        } catch (Exception e) {
          // don't stop anything if linkshare fails
        }
        
        String activatedPromoCode = order.getActivatedPromoCode();
        if (!StringUtils.isEmpty(activatedPromoCode)) {
          // if activatedPromoCode ends with "," then strip it off
          activatedPromoCode = activatedPromoCode.replaceAll(",$", "");
          sessionDetails.setCouponAuthorization(activatedPromoCode);
        } else {
          sessionDetails.setCouponAuthorization("");
        }
        
        // FiftyOne Envoy callback URL's
        String envoyCallbackUrl =
                protocolChange.getSecureURL(fiftyOneUtils.getSiteUrl() + "/internationalCheckoutCallback.jsp" + "?" + FiftyOneUtils.ENVOY_PARAM_ORDER_ID + "=" + atgOrderId + "&"
                        + FiftyOneUtils.ENVOY_PARAM_FIFTYONE_ORDER_ID + "=" + uniqueOrderId + "&" + FiftyOneUtils.ENVOY_PARAM_STATUS + "=", request, response);
        CheckoutComponents.getConfig().logDebug("envoyCallbackUrl: " + envoyCallbackUrl);
        // the URL that the Envoy calls in an invisible iframe when an order is accepted (GREEN)
        sessionDetails.setSuccessURL(envoyCallbackUrl + FiftyOneUtils.ENVOY_STATUS_SUCCESS);
        // the order is marked for fraud review (YELLOW). The pending and successful order are treated the same at this stage
        sessionDetails.setPendingURL(envoyCallbackUrl + FiftyOneUtils.ENVOY_STATUS_PENDING);
        /*
         * The Envoy calls the Failure URL in an invisible iframe each time an order attempt is made and fails (RED). An on-screen error message is also displayed to the customer. A maximum of 3
         * purchase attempts can be made before the Envoy will display a generic error message and prevent further action (other than Return to Shopping Cart). Each time the order is rejected, the URL
         * listed in FailureURL is called.
         */
        sessionDetails.setFailureURL(envoyCallbackUrl + FiftyOneUtils.ENVOY_STATUS_FAILURE);
        
        // set the server based callback url, this request is sent from a fiftyone server as opposed to the customers browser
        String serverBasedCallbackURL = getSystemSpecs(request).getEnvoyCallbackURL();
        
        if ((serverBasedCallbackURL == null) || (serverBasedCallbackURL.trim().equals(""))) {
          sessionDetails.setCallbackURL("");
        } else {
          sessionDetails.setCallbackURL(serverBasedCallbackURL + uniqueOrderId);
        }
        
        transferBuyerDomesticSession.setSessionDetails(sessionDetails);
        // the brand specific merchant id, such as Neiman Marcus, Last Call, Bergdorf Goodman, etc.
        transferBuyerDomesticSession.setMerchId(localizationUtils.getFiftyOneUtils().getMerchantId());
        merchantCheckout.setTransferBuyerDomesticSession(transferBuyerDomesticSession);
        body.setMerchantCheckout(merchantCheckout);
        
        // populate header
        Sender sender = new Sender();
        Identity identity = new Identity();
        // the super merchant id. The brand specific merchant id will be set as the merchId attribute of the TransferBuyerDomesticSession element
        identity.setMerchantId(localizationUtils.getFiftyOneUtils().getSuperMerchantId());
        identity.setUserId(localizationUtils.getFiftyOneUtils().getEmbassyServicesUserId());
        identity.setPassword(localizationUtils.getFiftyOneUtils().getEmbassyServicesPassword());
        sender.setName(FiftyOneUtils.MERCHANT_NAME);
        sender.setIdentity(identity);
        header.setSender(sender);
        
        // populate message
        message.setHeader(header);
        message.setBody(body);
        
        // map to BTP XML
        // FiftyOneMapper mapper = new FiftyOneMapper();
        final String btpRequestXml = FiftyOneMapper.mapMessageDataToBTPRequestXML(message);
        CheckoutComponents.getConfig().logDebug(" BTP Request XML for order: " + atgOrderId + "-" + uniqueOrderId + " : " + btpRequestXml);
        
        // perform Buyer Transfer Post(BTP)
        try {
          FiftyOneUtilsDiscoverable fiftyOneUtilsDiscoverable = (FiftyOneUtilsDiscoverable) NMDiscoverableServiceProxyFactory.createProxy(localizationUtils.getFiftyOneUtils());
          final String btpResponseXml = fiftyOneUtilsDiscoverable.buyerTransferPost(btpRequestXml);
          if (null != btpResponseXml && !btpResponseXml.trim().equals("")) {
            final com.nm.international.fiftyone.data.Message messageResponse = FiftyOneMapper.mapBTPResponseXMLToMessageData(btpResponseXml);
            if (null != messageResponse.getPayload().getSetCheckoutSessionResponse()) { // Successful BTP response
              // process the successful BTP response by embedding the Envoy object into the international checkout page
              fullEnvoyURL = messageResponse.getPayload().getSetCheckoutSessionResponse().getEnvoyInitialParams().getFullEnvoyUrl();
              CheckoutComponents.getConfig().logDebug(" FullEnvoyURL for order: " + atgOrderId + "-" + uniqueOrderId + " : " + fullEnvoyURL);
              
              // if successful btp response is received, create a new fiftyone order in the repository for tracking purposes
              fiftyOneUtils.createFiftyoneOrderFromMessageData(message);
              // send the same BTP message to ESB for soft allocation
              // fiftyOneUtils.sendBTP2ESB( btpRequestXml, atgOrderId + "-" + uniqueOrderId, true ); No longer being used as of October 2013 WR43114
            }
            
            else if (null != messageResponse.getPayload().getErrorResponse()) // Error
            {
              // process the BTP response error
              CheckoutComponents.getConfig().logError("BTP Response Error for order: " + atgOrderId + "-" + uniqueOrderId);
              CheckoutComponents.getConfig().logError("ErrorID: " + messageResponse.getPayload().getErrorResponse().getId());
              if (null != messageResponse.getPayload().getErrorResponse().getErrors()) {
                ErrorDocument.Error[] errors = messageResponse.getPayload().getErrorResponse().getErrors().getErrorArray();
                for (int i = 0; i < errors.length; i++) {
                  CheckoutComponents.getConfig().logError(" ErrorID: " + errors[i].getId());
                  CheckoutComponents.getConfig().logError(" ErrorDesc: " + errors[i].getDetails());
                  CheckoutComponents.getConfig().logError(" ErrorMsg: " + errors[i].getMessage());
                }
                
              }
              throw new FiftyOneException("BTP Response Error", messageResponse.getBody().getError());
            }
            
            else {
              throw new FiftyOneException("Unknown FiftOne response type");
            }
            
          }
          
        } catch (FiftyOneCommunicationException foce) {
          CheckoutComponents.getConfig().logError("FiftyOneCommunicationException caught: " + foce.getMessage());
        } catch (FiftyOneTimeoutException fote) {
          CheckoutComponents.getConfig().logError("FiftyOneTimeoutException caught: " + fote.getMessage());
        } catch (FiftyOneException foe) {
          CheckoutComponents.getConfig().logError("FiftyOneException caught: " + foe.getMessage());
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
      throw new Exception(e);
    }
    CheckoutComponents.getConfig().logDebug("End CartFormHandler buyerTransferPost");
    return fullEnvoyURL;
  }
  
  protected SystemSpecs getSystemSpecs(DynamoHttpServletRequest request) {
    SystemSpecs systemSpecs = (SystemSpecs) request.resolveName("/nm/utils/SystemSpecs");
    return systemSpecs;
  }
  
}
