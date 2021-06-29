package com.nm.commerce.pagedef.evaluator.checkout;

import static com.nm.common.INMGenericConstants.ACTIVE;
import static com.nm.estimateddeliverydate.EstimatedDeliveryDateConstants.ALIPAY_EXPRESS_CHECKOUT;
import static com.nm.estimateddeliverydate.EstimatedDeliveryDateConstants.EDD_NEW_LAYOUT_AB_TEST_GROUP;
import static com.nm.estimateddeliverydate.EstimatedDeliveryDateConstants.SHOPRUNNER_EXPRESS_CHECKOUT;
import static com.nm.tms.constants.TMSDataDictionaryConstants.APPROXIMATE_DELIVERY;
import static com.nm.twoclickcheckout.TwoClickCheckoutConstants.ORDER_COMPLETE_PAGE_DEFINITION_ID;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpSession;
import javax.servlet.jsp.PageContext;

import atg.commerce.order.HardgoodShippingGroup;
import atg.core.util.StringUtils;
import atg.nucleus.Nucleus;
import atg.repository.RepositoryItem;
import atg.servlet.DynamoHttpServletRequest;

import com.nm.abtest.AbTestHelper;
import com.nm.collections.ServiceLevel;
import com.nm.commerce.GiftCardHolder;
import com.nm.commerce.NMCommerceItem;
import com.nm.commerce.NMCreditCard;
import com.nm.commerce.NMOrderHolder;
import com.nm.commerce.NMProfile;
import com.nm.commerce.TempUserInfo;
import com.nm.commerce.catalog.NMProduct;
import com.nm.commerce.catalog.NMProductPrice;
import com.nm.commerce.checkout.CheckoutAPI;
import com.nm.commerce.checkout.CheckoutComponents;
import com.nm.commerce.order.NMOrderManager;
import com.nm.commerce.pagedef.definition.PageDefinition;
import com.nm.commerce.pagedef.evaluator.ProductPriceHelper;
import com.nm.commerce.pagedef.model.CheckoutPageModel;
import com.nm.commerce.pagedef.model.bean.RichRelevanceProductBean;
import com.nm.commerce.pagedef.model.bean.ShippingGroupAux;
import com.nm.commerce.promotion.NMOrderImpl;
import com.nm.common.INMGenericConstants;
import com.nm.common.NMBorderFreeGetQuoteResponse;
import com.nm.common.ServiceLevelConstants;
import com.nm.components.CommonComponentHelper;
import com.nm.estimateddeliverydate.session.EstimatedDeliveryDateSession;
import com.nm.estimateddeliverydate.util.EstimatedDeliveryDateShippingGroupUtil;
import com.nm.estimateddeliverydate.vo.EstimatedDeliveryDateCmosShippingVO;
import com.nm.estimateddeliverydate.vo.EstimatedDeliveryDateServiceLevelVO;
import com.nm.formhandler.ShoppingCartHandler;
import com.nm.international.fiftyone.checkoutapi.vo.AvailableShippingMethodsResposeVO;
import com.nm.profile.ProfileProperties;
import com.nm.tms.constants.TMSDataDictionaryConstants;
import com.nm.tms.core.TMSMessageContainer;
import com.nm.tms.utils.TMSDataDictionaryUtils;
import com.nm.twoclickcheckout.util.TwoClickCheckoutUtil;
import com.nm.utils.LinkedEmailUtils;
import com.nm.utils.LocalizationUtils;
import com.nm.utils.NmoUtils;
import com.nm.utils.PromotionsHelper;
import com.nm.utils.RichRelevanceClient;
import com.nm.utils.SameDayDeliveryUtil;
import com.nm.utils.StringUtilities;
import com.nm.utils.SystemSpecs;

public class OrderCompletePageEvaluator extends EstimatedShipDatePageEvaluator {
  
  @SuppressWarnings("unchecked")
  @Override
  public boolean evaluate(final PageDefinition pageDefinition, final PageContext pageContext) throws Exception {
    
    if (!super.evaluate(pageDefinition, pageContext)) {
      return false;
    }
    final DynamoHttpServletRequest request = getRequest();
    final LocalizationUtils utils = (LocalizationUtils) Nucleus.getGlobalNucleus().resolveName("/nm/utils/LocalizationUtils");
    final ShoppingCartHandler cart = CheckoutComponents.getCartHandler(getRequest());
    final NMOrderImpl order = cart.getNMOrder();
    
    final NMOrderManager orderMgr = (NMOrderManager) cart.getOrderManager();
    final NMProfile profile = CheckoutComponents.getProfile(getRequest());
    final CheckoutPageModel pageModel = (CheckoutPageModel) getPageModel();
    final NMOrderHolder orderHolder = (NMOrderHolder) cart.getOrderHolder();
    final NMOrderImpl lastOrder = (NMOrderImpl) orderHolder.getLast();
    final TMSDataDictionaryUtils dataDictionaryUtils = CommonComponentHelper.getDataDictionaryUtils();
    // get the TMSMessageContainer from CommonComponentHelper
    final TMSMessageContainer tmsMessageContainer = CheckoutComponents.getTMSMessageContainer(request);
    final ArrayList<String> tmsMessageList = new ArrayList<String>();
    pageModel.setOrderEligibleForDiscount(lastOrder.isOrderEligibleForDiscount());
    pageModel.setOrderHolder(orderHolder);
    pageModel.setCart(cart);
    // need to get the last order in order to display the Promotion(s)
    // Applied... text
    // on the order complete screen; getCurrentOrder(cart) returns the last
    // order here
    
    /* If the service levels are fetched from cmos through ESB call, the page modal values should be set from the Estimated Delivery Date session component */
    EstimatedDeliveryDateSession estimatedDeliveryDateSession = CheckoutComponents.getEstimatedDeliveryDateSession(request);
    EstimatedDeliveryDateCmosShippingVO estimatedDeliveryDateCmosShippingVO = estimatedDeliveryDateSession.getEstimatedDeliveryDateCmosShippingVO();
    if (null != estimatedDeliveryDateCmosShippingVO) {
      boolean orderHasAllDropshipItem = false;
      List<EstimatedDeliveryDateServiceLevelVO> serviceLevels = estimatedDeliveryDateCmosShippingVO.getServiceLevels();
      if (NmoUtils.isNotEmptyCollection(serviceLevels)) {
        Set<ServiceLevel> validServiceLevels = CheckoutAPI.convertCmosServiceLevelResponseToServiceLevelObject(serviceLevels);
        pageModel.setValidServiceLevels(validServiceLevels.toArray(new ServiceLevel[validServiceLevels.size()]));
        pageModel.setServiceLevelsFetchedFromCmos(true);
      } else {
    	/* check if all the item in the order is dropship and is not eligible to display the service level then return new empty object as in that case 
    	 * any service levels fetched from cmos would have been cleared. */
        orderHasAllDropshipItem = lastOrder.isOrderHasAllDropshipItemNotEligibleToDisplayServiceLevels();
      }
      // added for having the line item grouping based on their EDD
      if (ACTIVE.equalsIgnoreCase(AbTestHelper.getAbTestValue(request, EDD_NEW_LAYOUT_AB_TEST_GROUP))) {
    	if(pageModel.isServiceLevelsFetchedFromCmos() || (!estimatedDeliveryDateSession.isEstimatedDeliveryDateCmosCallFailed() && orderHasAllDropshipItem)) {
          String selectedServiceLevel = getServiceLevel(lastOrder);
          EstimatedDeliveryDateShippingGroupUtil.getInstance().groupItemsByEstimatedDeliveryDate(pageModel, Arrays.asList(pageModel.getShippingGroups()), selectedServiceLevel);
          if (!NmoUtils.isEmptyMap(pageModel.getGroupedLineItemsByEstimatedDeliveryDate()) && !ALIPAY_EXPRESS_CHECKOUT.equalsIgnoreCase(lastOrder.getVendorId())
          		&& !SHOPRUNNER_EXPRESS_CHECKOUT.equalsIgnoreCase(lastOrder.getVendorId())) {
            pageModel.setEstimatedDeliveryDateGroupingLayout(true);
          }
    	}
      }
    }
    
    final String lastOrderActivePromoCodeText = BaseCheckoutPageEvaluator.getActivePromoCodeText(orderHolder, getCurrentOrder(cart), pageModel);
    pageModel.setActivePromoCodeText(lastOrderActivePromoCodeText);
    pageModel.setItemCount(getCurrentOrder(cart).getTotalNumberOfItems());
    pageModel.setShowEstimate(false);
    
    final String abTestGroup = CheckoutComponents.getServiceLevelArray().getServiceLevelABTestGroup();
    if (CommonComponentHelper.getLogger().isLoggingDebug()) {
      CommonComponentHelper.getLogger().logDebug(
              "[Page : Order Confirmation]SmartPost ABTest Status : " + abTestGroup + " SmartPost/NonSmartPost group : "
                      + CheckoutComponents.getServiceLevelArray().getServiceLevelGroupBasedOnAbTest());
    }
    
    // V.me changes starts
    final SystemSpecs systemSpecs = CommonComponentHelper.getSystemSpecs();
    String email = null;
    if (!lastOrder.isMasterPassPayGroupOnOrder()) {
      email = pageModel.getProfile().getEmailAddressBeanProp();
    } else {
      // MasterPass Changes : Set MasterPass specific Page Model attributes with MasterPass data
      final List<NMCreditCard> creditCards = lastOrder.getCreditCards();
      NMCreditCard creditCard = null;
      if (!creditCards.isEmpty()) {
        creditCard = creditCards.iterator().next();
        email = ((String) creditCard.getPropertyValue(ProfileProperties.Profile_email)).toLowerCase();
        if (!StringUtilities.isEmpty(email)) {
          pageModel.setMasterPassEmail(email);
        }
        if (null != creditCard.getBillingAddress()) {
          pageModel.setMasterPassBillingFirstName(creditCard.getBillingAddress().getFirstName());
          pageModel.setMasterPassBillingLastName(creditCard.getBillingAddress().getLastName());
        }
      }
    }
    if (systemSpecs.isvMeEnabled() && StringUtilities.isNotEmpty(email)) {
      final RepositoryItem profileRegistered = CheckoutAPI.getProfileUsingEmail(email);
      if (null != profileRegistered) {
        pageModel.setProfileRegistered(true);
      }
    }
    // V.me changes ends
    /* Rich Revelavance ymal recommendations from server side API call */
    /*
     * country check for server side RR integration.
     */
    if (systemSpecs.isRichRelevanceServerSideEnabled() && systemSpecs.isRrServerSideCountryEnabled()) {
      
      @SuppressWarnings("unchecked")
      final List<NMCommerceItem> cList = lastOrder.getCommerceItems();
      final StringBuffer commerceitems = new StringBuffer();
      final StringBuffer qtyParamBuffer = new StringBuffer("");
      final StringBuffer priceParamBuffer = new StringBuffer("");
      final DecimalFormat decimalFormat = new DecimalFormat("0.00");
      for (final NMCommerceItem cItem : cList) {
        if (cList.size() > 1) {
          cItem.getProductId();
          commerceitems.append(cItem.getProductId());
          commerceitems.append("|");
          qtyParamBuffer.append(cItem.getQuantity()).append("|");
          if (cItem.getPriceInfo() != null) {
            priceParamBuffer.append(decimalFormat.format(cItem.getPriceInfo().getListPrice())).append("|");
          }
        } else {
          commerceitems.append(cItem.getProductId());
          qtyParamBuffer.append(cItem.getQuantity());
          if (cItem.getPriceInfo() != null) {
            priceParamBuffer.append(decimalFormat.format(cItem.getPriceInfo().getListPrice()));
          }
        }
      }
      if (lastOrder.getCommerceItemCount() > 0) {
        final NMProduct nmProduct = ((NMCommerceItem) lastOrder.getCommerceItems().get(0)).getProduct();
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
        if (!((isBeautyProduct && (pageModel.isChanel() || pageModel.isHermes())) || manulSuggestCM2)) {
          final RichRelevanceClient richRelevanceData = (RichRelevanceClient) request.resolveName("/nm/utils/RichRelevanceClient");
          final HttpSession session = request.getSession();
          final String sessionId = session.getId();
          final String userId = getProfile().getEdwCustData().getUcid();
          final String blackList = "";
          final String rrOrderIdParam = lastOrder.getId();
          try {
            final RichRelevanceProductBean richRelevanceProducts =
                    richRelevanceData.getProductRecordsFromRichRelevance(userId, sessionId, commerceitems.toString(), "purchase_complete_page.horizontal", blackList, getProfile()
                            .getCountryPreference(), rrOrderIdParam, qtyParamBuffer.toString(), priceParamBuffer.toString());
            pageModel.setYmalProducts(richRelevanceProducts);
          } catch (final Exception e) {
            // Rich Relevance Error should not break the page.
            CommonComponentHelper.getLogger().logError("Error occured while fetching YMAL content from RichRelevance.." + e.getMessage());
          }
          pageModel.setServerSideYmal(true);
        }
      }
    }
    // INT-1902 Story Start
    final boolean isInternationCheckout = CheckoutAPI.isInternationalSession(profile);
    if (isInternationCheckout && lastOrder.isExpressPaypal()) {
      pageModel.setExpressPayPal(lastOrder.isExpressPaypal());
      final List<NMCreditCard> lastOrderCreditCards = lastOrder.getCreditCards();
      if (!lastOrderCreditCards.isEmpty()) {
        final NMCreditCard paymentGroup = lastOrderCreditCards.get(0);
        pageModel.setExpPayPalEmailId(!StringUtils.isBlank(paymentGroup.getPayPalEmail()) ? paymentGroup.getPayPalEmail() : "");
      }
      
    }
    // INT-1902 Story End
    if (CommonComponentHelper.getBrandSpecs().isSameDayDeliveryEnabled() && !isInternationCheckout && !pageModel.isServiceLevelsFetchedFromCmos()) {
      final boolean isSameDayDeliveryOrder = CommonComponentHelper.getSameDayDeliveryUtil().isSameDayDeliveryOrder(order);
      pageModel.setDisplaySameDayDeliverySL(isSameDayDeliveryOrder);
      final ServiceLevel[] serviceLevels = pageModel.getValidServiceLevels();
      final ArrayList<ServiceLevel> validServiceLevels = new ArrayList<ServiceLevel>(Arrays.asList(serviceLevels));
      final ServiceLevel sl0 = CheckoutAPI.getServiceLevelByCodeAndCountry(ServiceLevel.SL0_SERVICE_LEVEL_TYPE, LocalizationUtils.US_COUNTRY_CODE);
      sl0.setEstimatedArrivalDate(CommonComponentHelper.getSameDayDeliveryUtil().getSddDeliveryText());
      validServiceLevels.add(sl0);
      pageModel.setValidServiceLevels(validServiceLevels.toArray(new ServiceLevel[validServiceLevels.size()]));
    }
    setOrderCreditCard(order, orderMgr, profile);
    final List<NMCreditCard> orderCreditCards = order.getCreditCards();
    final List<NMCreditCard> lastOrderCreditCards = lastOrder.getCreditCards();
    if ((lastOrderCreditCards != null) && !lastOrderCreditCards.isEmpty()) {
      final NMCreditCard paymentGroupLast = lastOrderCreditCards.get(0);
      if (paymentGroupLast.isVmeCard()) {
        pageModel.setVmeCard(true);
      } else if (paymentGroupLast.isMasterPassCard()) {
        pageModel.setMasterPassCard(true);
      }
    }
    if (!orderCreditCards.isEmpty()) {
      final NMCreditCard paymentGroup = orderCreditCards.get(0);
      if (!StringUtilities.isEmpty(paymentGroup.getCreditCardNumber()) || paymentGroup.isPayPal()) {
        final NMCreditCard profileCard = paymentGroup.isPayPal() ? profile.getPayPalByEmail(paymentGroup.getPayPalEmail()) : profile.getCreditCardByNumber(paymentGroup.getCreditCardNumber());
        
        if (profileCard != null) {
          paymentGroup.setCidAuthorized(profileCard.getCidAuthorized());
        }
        
        pageModel.setOrderCreditCard(paymentGroup);
      }
    }
    
    final ShippingGroupAux[] shippingGroups = pageModel.getShippingGroups();
    
    for (int n = 0; n < shippingGroups.length; n++) {
      final ShippingGroupAux shipAux = shippingGroups[n];
      final String countryCode = ((HardgoodShippingGroup) shipAux.getShippingGroupRef()).getShippingAddress().getCountry();
      if ((countryCode != null) && !countryCode.equals("US")) {
        pageModel.setAtLeastOneInternationalShipTo(true);
        break;
      }
    }
    
    // handle Promo After Promo messages
    final PromotionsHelper promotionsHelper = CheckoutComponents.getPromotionsHelper();
    final String papConfirmText = promotionsHelper.getPapConfirmText(getCurrentOrder(cart));
    if (null != papConfirmText) {
      pageModel.setConfirmText(papConfirmText);
    }
    
    // check for BOPS and gwp must ship with issue
    if (!isInternationCheckout && lastOrder.hasBopsItem() && CheckoutAPI.hasMustShipWithItemGWP(lastOrder.getNmCommerceItems())) {
      CheckoutAPI.correctGwpMustShipWithAndBOPSIssue(cart, lastOrder);
    }
    // check for same day delivery and gwp must ship with issue
    if (!isInternationCheckout && CommonComponentHelper.getBrandSpecs().isSameDayDeliveryEnabled()) {
      final SameDayDeliveryUtil sameDayDeliveryUtil = CommonComponentHelper.getSameDayDeliveryUtil();
      final List<NMCommerceItem> items = lastOrder.getNmCommerceItems();
      final Boolean sddOrder = sameDayDeliveryUtil.isOrderHasAtLeastOneSDDItem(items);
      if (sddOrder && CheckoutAPI.hasMustShipWithItemGWP(items)) {
        sameDayDeliveryUtil.correctGwpMustShipWithAndSDDIssue(order, pageModel.getShippingGroups());
      }
    }
    
    LinkedEmailUtils.getInstance().removeLinkedEmailOrder(pageModel.getOrder().getId());
    
    final TempUserInfo tempUserInfo = orderHolder.getTempUserInfo();
    
    if (tempUserInfo.isOptedToRegister() && tempUserInfo.isProfileCreated()) {
      request.setAttribute("profileCreated", Boolean.TRUE);
    }
    profile.getOosItems().clear();
    profile.getOosCartRecommendations().clear();
    // Data Dictionary Attributes population.
    setServiceLevelShortDescription(pageModel, lastOrder);
    setEstimatedDeliveryDate(pageModel, lastOrder);
    setInternationalPriceDetails(lastOrder, profile);
    setOrderAmountAttributes(pageModel, lastOrder);
    tmsMessageContainer.setMessages(tmsMessageList);
    dataDictionaryUtils.processTMSDataDictionaryAttributes(pageDefinition, lastOrder, profile, pageModel, tmsMessageContainer);
    
    /* If the order place is buy it now order (2cc) then set the previous saved order as current order */
    if (lastOrder.isBuyItNowOrder() && ORDER_COMPLETE_PAGE_DEFINITION_ID.equalsIgnoreCase(pageDefinition.getId())) {
      final TwoClickCheckoutUtil twoClickCheckoutUtil = CommonComponentHelper.getTwoClickCheckoutUtil();
      if (null != twoClickCheckoutUtil) {
        twoClickCheckoutUtil.moveSavedOrderBackToCurrent(orderHolder);
      }
    }
    return true;
  }
  
  /**
   * Sets the order amount attributes.
   *
   * @param pageModel the page model
   * @param lastOrder the last order
   */
  private void setOrderAmountAttributes(CheckoutPageModel pageModel, NMOrderImpl lastOrder) {
    List<Double> highAdornPrice = new ArrayList<Double>();
    List<Double> highPrice = new ArrayList<Double>();
    List<Double> lowPrice = new ArrayList<Double>();
    final List<NMCommerceItem> commerceItems = lastOrder.getCommerceItems();
    final ProductPriceHelper priceHelper = new ProductPriceHelper();
     NMProductPrice nmProductPrice;
     double highAdornmentPrice;
     double highestPrice;
     double lowestprice;
    for (final NMCommerceItem commerceItem : commerceItems) {
      
      nmProductPrice = priceHelper.generateProductPricing(commerceItem.getProduct(), false);
      highAdornmentPrice = nmProductPrice.getHighPaPriced();// Price Adornment
      highestPrice = nmProductPrice.gethighestPriced(); // High Dollar Amount
      lowestprice = nmProductPrice.getlowestPriced(); // Low Dollar Amount
      highAdornPrice.add(highAdornmentPrice);
      highPrice.add(highestPrice);
      lowPrice.add(lowestprice);
      
      }
    pageModel.setHighAdornPrice(highAdornPrice);
    pageModel.setHighPrice(highPrice);
    pageModel.setLowPrice(lowPrice);
  }
  
  /**
   * Gets the service level.
   * 
   * @param lastOrder
   *          the last order
   * @return the service level
   */
  @SuppressWarnings("unchecked")
  private String getServiceLevel(final NMOrderImpl lastOrder) {
    String selectedServiceLevel = null;
    List<NMCommerceItem> commerceItems = lastOrder.getCommerceItems();
    for (NMCommerceItem commerceItem : commerceItems) {
      if (!commerceItem.isVirtualGiftCard()) {
        selectedServiceLevel = commerceItem.getServicelevel();
        break;
      }
    }
    return selectedServiceLevel;
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
  
  /**
   * Sets the service level short description to page model. selected shipping method is a list property which holds the shipping method description for each commerce item. if item is pickup from
   * store set the shipping method description to standard. if item is a virtual gift card set the shipping method description to via email if item is a drop ship item set the shipping method
   * description to drop ship
   * 
   * 
   * @param pageModel
   *          the page model
   * @param lastOrder
   *          the last order
   */
  private void setServiceLevelShortDescription(final CheckoutPageModel pageModel, final NMOrderImpl lastOrder) {
    @SuppressWarnings("unchecked")
    final String SL4 = "SL4";
    final List<NMCommerceItem> commerceItems = lastOrder.getCommerceItems();
    NMProduct nmProduct;
    boolean eGiftCard;
    boolean dropShip;
    boolean gwpProduct;
    String serviceLevelCode;
    final List<String> shippingMethodDescriptionList = new ArrayList<String>();
    for (final NMCommerceItem commerceItem : commerceItems) {
      if (!lastOrder.isInternationalOrder()) {
        nmProduct = commerceItem.getProduct();
        eGiftCard = StringUtilities.areEqual(nmProduct.getMerchandiseType(), GiftCardHolder.VIRTUAL_GIFT_CARD_MERCH_TYPE);
        dropShip = commerceItem.isDropship();
        gwpProduct = commerceItem.isGwpItem();
        if ((StringUtils.isEmpty(commerceItem.getPickupStoreNo()))) {
          if (eGiftCard) {
            shippingMethodDescriptionList.add(ServiceLevelConstants.VIA_EMAIL);
          } else if (dropShip) {
            if (CommonComponentHelper.getLogger().isLoggingDebug()) {
              CommonComponentHelper.getLogger().logDebug("dropShip");
            }
            shippingMethodDescriptionList.add(ServiceLevelConstants.DROP_SHIP);
          } else if (gwpProduct) {
            if (CommonComponentHelper.getLogger().isLoggingDebug()) {
              CommonComponentHelper.getLogger().logDebug("gwpProduct");
            }
            shippingMethodDescriptionList.add(TMSDataDictionaryConstants.STANDARD);
          } else if (SL4.equalsIgnoreCase(commerceItem.getServicelevel())) {
            shippingMethodDescriptionList.add(TMSDataDictionaryConstants.STANDARD);
          } else {
            if (pageModel.getValidServiceLevels() != null && pageModel.getValidServiceLevels().length > 0) {
              for (final ServiceLevel serviceLevel : pageModel.getValidServiceLevels()) {
                serviceLevelCode = serviceLevel.getCode();
                if ((null != serviceLevel) && (null != serviceLevelCode)) {
                  if (CommonComponentHelper.getLogger().isLoggingDebug()) {
                    CommonComponentHelper.getLogger().vlogDebug("serviceLevel.getCode() {0} commerceItem.getServicelevel() {1} ", serviceLevelCode, commerceItem.getServicelevel());
                  }
                  if (serviceLevelCode.equalsIgnoreCase(commerceItem.getServicelevel())) {
                    if (serviceLevel.getShortDesc().isEmpty()) {
                      shippingMethodDescriptionList.add(serviceLevel.getPromotionalMessage());
                    } else if (ServiceLevel.SR_CUSTOM_SHORT_DESC.equalsIgnoreCase(serviceLevel.getShortDesc())) {
                      shippingMethodDescriptionList.add(ServiceLevelConstants.promotionalMessage + INMGenericConstants.WHITE_SPACE + serviceLevel.getShortDesc());
                    } else {
                      shippingMethodDescriptionList.add(serviceLevel.getShortDesc());
                    }
                  }
                }
              }
            } else {
              if (CommonComponentHelper.getLogger().isLoggingDebug()) {
                CommonComponentHelper.getLogger().logDebug("ValidServiceLevels null or length zero");
              }
              shippingMethodDescriptionList.add(TMSDataDictionaryConstants.STANDARD);
            }
          }
        } else {
          shippingMethodDescriptionList.add(TMSDataDictionaryConstants.STANDARD);
        }
      } else {
        final NMBorderFreeGetQuoteResponse borderFreeQuoteResponse = pageModel.getNmBorderFreeGetQuoteResponse();
        if (borderFreeQuoteResponse != null) {
          final AvailableShippingMethodsResposeVO shippingMethodVO = borderFreeQuoteResponse.getSelectedShippingMethod();
          if (shippingMethodVO != null) {
            shippingMethodDescriptionList.add(shippingMethodVO.getDisplayName());
          }
        }
      }
    }
    pageModel.setSelectedShippingMethodDescriptions(shippingMethodDescriptionList);
  }
  
  /**
   * Sets the estimated delivery date to page model.
   * 
   * @param pageModel
   *          the page model
   * @param lastOrder
   *          the last order
   */
  private void setEstimatedDeliveryDate(final CheckoutPageModel pageModel, final NMOrderImpl lastOrder) {
    final List<String> estimatedDeliveryDate = new ArrayList<String>();
    final String DATE_FORMAT = "MM/dd/yyyy";
    final SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
    @SuppressWarnings("unchecked")
    final List<NMCommerceItem> commerceItems = lastOrder.getCommerceItems();
    for (final NMCommerceItem commerceItem : commerceItems) {
      if ((StringUtils.isBlank(commerceItem.getPickupStoreNo()))) {
        for (final ServiceLevel serviceLevel : pageModel.getValidServiceLevels()) {
          if ((null != serviceLevel) && (null != serviceLevel.getCode())) {
            if (serviceLevel.getCode().equalsIgnoreCase(commerceItem.getServicelevel())) {
              if (StringUtilities.isNotEmpty(commerceItem.getRealExpectedShipDate())) {
                estimatedDeliveryDate.add(INMGenericConstants.EMPTY_STRING);
                break;
              }
              if (null != commerceItem.getReqDeliveryDate()) {
                estimatedDeliveryDate.add(dateFormat.format(commerceItem.getReqDeliveryDate()));
              } else if (StringUtilities.isNotEmpty(serviceLevel.getEstimatedArrivalDate())) {
                estimatedDeliveryDate.add(formatDateRangeString(serviceLevel.getEstimatedArrivalDate()));
              } else {
                estimatedDeliveryDate.add(INMGenericConstants.EMPTY_STRING);
              }
            }
          }
        }
      } else {
        estimatedDeliveryDate.add(INMGenericConstants.EMPTY_STRING);
      }
    }
    pageModel.setEstimatedDeliveryDates(estimatedDeliveryDate);
  }
  
  /**
   * This method removes the string "Approximate Delivery " from the String passed in the parameter
   * 
   * @param stringToFormat
   * @return
   */
  public String formatDateRangeString(String stringToFormat) {
    String formattedString = stringToFormat;
    if (!StringUtils.isEmpty(stringToFormat) && stringToFormat.contains(APPROXIMATE_DELIVERY)) {
      formattedString = formattedString.replace(APPROXIMATE_DELIVERY, "");
    }
    return formattedString;
  }
  
  /**
   * Sets the International price details in Localized currency.
   * 
   * @param lastOrder
   *          the last order
   * @param profile
   *          the profile
   */
  private void setInternationalPriceDetails(final NMOrderImpl lastOrder, final NMProfile profile) {
    if (null != lastOrder.getInternationalPriceItem()) {
      final double deliveryCharge = (Double) lastOrder.getInternationalPriceItem().getPropertyValue("extraShipping") + (Double) lastOrder.getInternationalPriceItem().getPropertyValue("extraHandling");
      if (lastOrder.isChinaUnionPayPaymentType() || lastOrder.isConvertRussiaCurrencyToUSD()) {
        lastOrder.setInternationalOrderDuties((Double) lastOrder.getInternationalPriceItem().getPropertyValue("duties"));
        lastOrder.setInternationalTaxes((Double) lastOrder.getInternationalPriceItem().getPropertyValue("taxes"));
        lastOrder.setInternationalShippingCost(deliveryCharge);
      } else {
        lastOrder.setInternationalOrderDuties(Double.parseDouble(utils.getUSDConvertedCurrency(getProfile().getCountryPreference(), getProfile().getCurrencyPreference(), (Double) lastOrder
                .getInternationalPriceItem().getPropertyValue("duties"))));
        lastOrder.setInternationalTaxes((Double.parseDouble(utils.getUSDConvertedCurrency(getProfile().getCountryPreference(), getProfile().getCurrencyPreference(), (Double) lastOrder
                .getInternationalPriceItem().getPropertyValue("taxes")))));
        lastOrder.setInternationalShippingCost((Double.parseDouble(utils.getUSDConvertedCurrency(getProfile().getCountryPreference(), getProfile().getCurrencyPreference(), deliveryCharge))));
      }
    }
  }
  
  @Override
  protected NMOrderImpl getCurrentOrder(final ShoppingCartHandler cart) {
    return (NMOrderImpl) cart.getOrderHolder().getLast();
  }
  
}
