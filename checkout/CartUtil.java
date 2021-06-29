package com.nm.commerce.checkout;

import static com.nm.monogram.utils.MonogramConstants.MONOGRAM_CHECKBOX_TEXT;
import static com.nm.monogram.utils.MonogramConstants.OPTIONAL_MONOGRAM_FLAG;
import static com.nm.monogram.utils.MonogramConstants.PLAIN_CHECKBOX_TEXT;
import static com.nm.monogram.utils.MonogramConstants.REQUIRED_MONOGRAM_FLAG;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import atg.commerce.order.CommerceItem;
import atg.commerce.order.OrderManager;
import atg.commerce.order.ShippingGroupCommerceItemRelationship;
import atg.core.util.StringUtils;
import atg.nucleus.GenericService;
import atg.nucleus.Nucleus;
import atg.repository.Repository;
import atg.repository.RepositoryException;
import atg.repository.RepositoryItem;
import atg.repository.RepositoryView;
import atg.repository.rql.RqlStatement;
import atg.servlet.DynamoHttpServletRequest;
import atg.servlet.ServletUtil;

import com.nm.ajax.catalog.product.utils.ProductUtils;
import com.nm.ajax.checkout.beans.RequestBean;
import com.nm.ajax.checkout.session.PersonalizedPromoTracking;
import com.nm.ajax.checkout.utils.ComponentUtils;
import com.nm.ajax.checkoutb.beans.PromoEmailClearRequest;
import com.nm.collections.CreditCard;
import com.nm.collections.ExtraAddressArray;
import com.nm.collections.GiftWithPurchaseArray;
import com.nm.collections.GiftWithPurchaseSelectArray;
import com.nm.collections.GiftwrapArray;
import com.nm.collections.NMPromotion;
import com.nm.collections.NMPromotionArray;
import com.nm.collections.PercentOffPromotionsArray;
import com.nm.collections.PromotionsArray;
import com.nm.collections.PurchaseWithPurchaseArray;
import com.nm.commerce.NMCommerceItem;
import com.nm.commerce.NMOrderHolder;
import com.nm.commerce.NMProfile;
import com.nm.commerce.catalog.NMProduct;
import com.nm.commerce.checkout.beans.CommerceItemUpdate;
import com.nm.commerce.checkout.beans.GiftOptionsUpdate;
import com.nm.commerce.checkout.beans.GiftWrapInfo;
import com.nm.commerce.checkout.beans.PromoBean;
import com.nm.commerce.checkout.beans.ResultBean;
import com.nm.commerce.checkout.beans.ResultBeanHelper;
import com.nm.commerce.order.NMOrderManager;
import com.nm.commerce.pricing.NMPricingTools;
import com.nm.commerce.promotion.NMOrderImpl;
import com.nm.commerce.promotion.action.PromotionEvaluation;
import com.nm.commerce.promotion.rules.RuleHelper;
import com.nm.commerce.promotion.rulesBased.Promotion;
import com.nm.common.INMGenericConstants;
import com.nm.common.NMBorderFreeGetQuoteResponse;
import com.nm.components.CommonComponentHelper;
import com.nm.formhandler.ShoppingCartHandler;
import com.nm.international.fiftyone.checkoutapi.vo.AvailablePaymentMethodsResposeVO;
import com.nm.profile.ProfileProperties;
import com.nm.sitemessages.Message;
import com.nm.sitemessages.MessageDefs;
import com.nm.utils.BrandSpecs;
import com.nm.utils.LocalizationUtils;
import com.nm.utils.PersonalizedCustData;
import com.nm.utils.ProdSkuUtil;
import com.nm.utils.PromotionsHelper;
import com.nm.utils.StringUtilities;
import com.nm.utils.SystemSpecs;
import com.nm.utils.dynamicCode.DynamicCodeUtils;

/**
 * Package access - outside of this package, methods should be accessed through CheckoutAPI.
 */
/* package */class CartUtil {
  
  private static CartUtil INSTANCE; // avoid static initialization
  private static final LocalizationUtils localizationUtils = ((LocalizationUtils) Nucleus.getGlobalNucleus().resolveName("/nm/utils/LocalizationUtils"));
  
  private static final int PERSONALIZED_NOTE_TYPE = 2;
  private static final String GIFT_NOTE_MESSAGES_FRAGMENT_ID = "820001";
  private static final String CHINA_UNION_PAY = "China UnionPay";
  public static final String promoTrackingComponent = "/nm/ajax/checkout/session/PersonalizedPromoTracking";
  
  // private constructor enforces singleton behavior
  private CartUtil() {}
  
  public static synchronized CartUtil getInstance() {
    INSTANCE = (INSTANCE == null) ? new CartUtil() : INSTANCE;
    return INSTANCE;
  }
  
  public ResultBean updateCommerceItem(final CommerceItemUpdate update, final ShoppingCartHandler cart, final NMProfile profile) throws Exception {
    ResultBean result = new ResultBean();
    
    final OrderUtil orderUtil = OrderUtil.getInstance();
    
    final NMOrderImpl order = cart.getNMOrder();
    final NMOrderHolder orderHolder = (NMOrderHolder) cart.getOrderHolder();
    final OrderManager orderManager = cart.getOrderManager();
    
    final String commerceItemId = update.getCommerceItemId();
    final NMCommerceItem commerceItem = (NMCommerceItem) order.getCommerceItem(commerceItemId);
    final long originalQuantity = commerceItem.getQuantity();
    
    // // tagging - capture the products on the cart before adding items
    // orderUtil.addCartProductAttribute(this);
    
    if (update.getQuantity() == 0) {
      if (commerceItem.isPwpItem()) {
        order.addPwpRejectPromoToMap(commerceItem.getSendCmosPromoKey(), commerceItem.getSendCmosPromoCode(), commerceItem.getProductId());
      }
      
      result = orderUtil.removeItemFromOrder(cart, commerceItemId);
      // OmnitureUtils.getInstance().scRemove(checkoutResp, nmCI);
      
      ResultBeanHelper.removeItemFromCart(result, commerceItem);
      
      orderUtil.removeEmptyShippingGroups(order, orderManager);
    } else {
      setQuantity(commerceItem, update.getQuantity());
      ShippingUtil.getInstance().handleClearTagging(cart);
      
      final Repository productRepository = CommonComponentHelper.getProductRepository();
      RepositoryItem skuItem = null;
      
      final String productId = commerceItem.getProductId();
      final NMProduct nmProduct = new NMProduct(productId);
      
      // if the sku ID is missing, derive it from size and color
      String skuId = update.getSkuId();
      if (StringUtilities.isEmpty(skuId)) {
        final String color = update.getColor();
        final String size = update.getSize();
        final ProductUtils productUtils = ProductUtils.getInstance();
        final ProdSkuUtil prodSkuUtil = CommonComponentHelper.getProdSkuUtil();
        skuId = productUtils.retrieveSku(productId, color, size, "", prodSkuUtil, nmProduct);
      }
      
      skuItem = productRepository.getItem(skuId, "sku");
      final int selectedInterval = update.getInterval();
      final String intervalText = ((selectedInterval == CommerceItemUpdate.NO_VALUE) || (selectedInterval == CommerceItemUpdate.NO_INTERVAL)) ? null : String.valueOf(selectedInterval);
      
      // tagging related to adding, removing replenishment
      
      //System.out.println("selectedInterval: " + selectedInterval);
      
      if (selectedInterval != CommerceItemUpdate.NO_VALUE) {
        boolean addedReplenishment = false;
        if (!String.valueOf(selectedInterval).equals(commerceItem.getSelectedInterval())) {
          String replenishment = "";
          if (selectedInterval == CommerceItemUpdate.NO_INTERVAL) {
            replenishment = "ReplenishmentRemoved";
          } else {
            replenishment = "Every " + selectedInterval + " days";
            addedReplenishment = true;
          }
          ResultBeanHelper.updateReplenishmentItem(result, commerceItem, replenishment, addedReplenishment);
        }
      } else {
        ResultBeanHelper.updateItemInCart(result, commerceItem, originalQuantity);
      }
      
      @SuppressWarnings("unchecked")
      final Map<String, RepositoryItem> skuProdMap = (Map<String, RepositoryItem>) skuItem.getPropertyValue("skuProdInfo");
      final RepositoryItem skuProd = skuProdMap.get(commerceItem.getAuxiliaryData().getProductId());
      
      commerceItem.setCatalogRefId(skuItem.getRepositoryId());
      commerceItem.setSelectedInterval(intervalText);
      commerceItem.setCmosSKUId((String) skuItem.getPropertyValue("cmosSKU"));
      commerceItem.setProdVariation1(nullEmpty(skuProd.getPropertyValue("cmVariation1")));
      commerceItem.setProdVariation2(nullEmpty(skuProd.getPropertyValue("cmVariation2")));
      commerceItem.setProdVariation3(nullEmpty(skuProd.getPropertyValue("cmVariation3")));
      if (update.getDynamicImageUrl() != null) { // dynamic image url changed in the cart page
        commerceItem.setDynamicImageUrl(update.getDynamicImageUrl());
      }
      final boolean isMonogramProduct = null != update.getSiCodes() && !update.getSiCodes().isEmpty();
      if (StringUtilities.isNotEmpty(update.getMonogramOption())) {
        if(MONOGRAM_CHECKBOX_TEXT.equalsIgnoreCase(update.getMonogramOption())){
          commerceItem.setSpecialInstCodes(update.getSiCodes());
          if (isMonogramProduct) {
            commerceItem.setCodeSetType(commerceItem.getProduct().getCodeSetType());
            String specialInstructionFlag = commerceItem.getProduct().getSpecialInstructionFlag();
            commerceItem.setSpecialInstructionFlag(specialInstructionFlag);
            if (specialInstructionFlag.equalsIgnoreCase(OPTIONAL_MONOGRAM_FLAG)) {
              commerceItem.setSpecialInstructionPrice(commerceItem.getProduct().getSpecialInstructionPrice());
            }
          }
        } else if (PLAIN_CHECKBOX_TEXT.equalsIgnoreCase(update.getMonogramOption())) {
          commerceItem.setCodeSetType(INMGenericConstants.EMPTY_STRING);
          commerceItem.setSpecialInstructionFlag(INMGenericConstants.EMPTY_STRING);
          commerceItem.setSpecialInstructionPrice(INMGenericConstants.DOUBLE_ZERO);
        }
      } else if (isMonogramProduct) {
    	  /*Old flow- Flag is not set, but found product has special instructions*/
        commerceItem.setSpecialInstCodes(update.getSiCodes());
        commerceItem.setCodeSetType(commerceItem.getProduct().getCodeSetType());
        commerceItem.setSpecialInstructionFlag(REQUIRED_MONOGRAM_FLAG);
      }
      
      if (commerceItem.getPerishable()) {
        final int year = update.getPerishableYear();
        final int month = update.getPerishableMonth() - 1;
        final int day = update.getPerishableDay();
        final Calendar calendar = new GregorianCalendar(year, month, day);
        commerceItem.setReqDeliveryDate(calendar.getTime());
      }
    }
    
    // perform product limit quantity validation
    result.getMessages().addAll(validateLimitQuantity(order));

    // save commerceitems in case we split
    @SuppressWarnings("unchecked")
    List<NMCommerceItem> originalCommerceItems = order.getCommerceItems();

    // reprice the order to get messages back, if any
    final List<Message> promoMessages = PricingUtil.getInstance().repriceOrder(cart, profile, SnapshotUtil.EDIT_ITEM_FROM_REGULAR_CART);
    result.getMessages().addAll(promoMessages);

    // repricing the order may have split the updated item into several line items
    @SuppressWarnings("unchecked")
    List<NMCommerceItem> currentCommerceItems = order.getCommerceItems();
    if (!currentCommerceItems.contains(commerceItem)) {
      ShippingUtil.getInstance().handleClearTagging(cart);
    }

    // super.processMessages(messageList, checkoutResp);
    
    orderManager.updateOrder(order);
    
    // // tagging - capture the products on the cart after adding items
    // orderUtil.updateCartProductAttribute(this);
    // // get ajax marketing tags
    // String marketingHtml = "";
    // GenericTagInfo tagInfo = GenericTagInfo.getInstance(this.getCurrentRequest());
    // tagInfo.setExcludeZeroDollarItems(true); //tellapart does not want gwp's at this time
    // // now process added products
    // tagInfo.setActionType(GenericTagInfo.PARTIAL_ADD);
    // marketingHtml += this.invokeNamedTemplate(NamedPaths.AJAX_MARKETING_TAGS);
    // // now process removed products
    // tagInfo.setActionType(GenericTagInfo.PARTIAL_REMOVE);
    // marketingHtml += this.invokeNamedTemplate(NamedPaths.AJAX_MARKETING_TAGS);
    // checkoutResp.setMarketingHtml(marketingHtml);
    
    // super.removeNonClientMessages(checkoutResp);
    
    return result;
  }
  
  public ResultBean updateGiftOptions(final GiftOptionsUpdate update, final ShoppingCartHandler cart) throws Exception {
    final ResultBean result = new ResultBean();
    
    final NMOrderImpl order = cart.getNMOrder();
    final String commerceItemId = update.getCommerceItemId();
    final NMCommerceItem commerceItem = (NMCommerceItem) order.getCommerceItem(commerceItemId);
    
    if (update.getGiftWrapOn()) {
      final RepositoryItem giftWrapItem = getGiftWrapItem();
      commerceItem.setGiftWrap(giftWrapItem);
      commerceItem.setGiftWrapSeparately(update.getGiftWrapSeparately());
    } else {
      commerceItem.setGiftWrap(null);
      commerceItem.setGiftWrapSeparately(false);
    }
    
    final int noteType = update.getGiftNoteType();
    commerceItem.setGiftNote(noteType);
    if (noteType == PERSONALIZED_NOTE_TYPE) {
      commerceItem.setNoteLine1(emptyNull(update.getNoteLine1()));
      commerceItem.setNoteLine2(emptyNull(update.getNoteLine2()));
      commerceItem.setNoteLine3(emptyNull(update.getNoteLine3()));
      commerceItem.setNoteLine4(emptyNull(update.getNoteLine4()));
      commerceItem.setNoteLine5(emptyNull(update.getNoteLine5()));
    } else {
      commerceItem.setNoteLine1(null);
      commerceItem.setNoteLine2(null);
      commerceItem.setNoteLine3(null);
      commerceItem.setNoteLine4(null);
      commerceItem.setNoteLine5(null);
    }
    
    return result;
  }
  
  private String emptyNull(final String text) {
    return (StringUtils.isEmpty(text) ? null : text);
  }
  
  public GiftWrapInfo getGiftWrapInfo() {
    final GiftWrapInfo info = new GiftWrapInfo();
    final RepositoryItem giftWrapItem = getGiftWrapItem();
    final Double giftWrapPrice = (Double) giftWrapItem.getPropertyValue("price");
    info.setFullPrice(giftWrapPrice.doubleValue());
    info.setMessages(getGiftNoteMessages());
    return info;
  }
  
  private RepositoryItem getGiftWrapItem() {
    final SystemSpecs systemSpecs = CommonComponentHelper.getSystemSpecs();
    final String giftWrapId = systemSpecs.getGiftwrapCode();
    final NMPricingTools pricingTools = CommonComponentHelper.getPricingTools();
    return pricingTools.getGiftWrapItem(giftWrapId);
  }
  
  private List<String> getGiftNoteMessages() {
    final List<String> noteMessages = new ArrayList<String>();
    
    try {
      final Repository productRepository = CommonComponentHelper.getProductRepository();
      final RepositoryItem noteFragment = productRepository.getItem(GIFT_NOTE_MESSAGES_FRAGMENT_ID, "htmlfragments");
      if (noteFragment != null) {
        final String noteValue = (String) noteFragment.getPropertyValue("frag_value");
        if (noteValue != null) {
          final String[] messages = noteValue.split(";");
          for (int i = 0; i < messages.length; i++) {
            final String message = messages[i].trim();
            if (message.length() > 0) {
              noteMessages.add(message);
            }
          }
        }
      }
    } catch (final RepositoryException e) {
      CommonComponentHelper.getLogger().error("Error retrieving gift note messages", e);
    }
    
    return noteMessages;
  }
  
  private void setQuantity(final NMCommerceItem commerceItem, final int quantity) {
    // Long currentQty = (Long)commerceItem.getPropertyValue("quantity");
    // final NMOrderHolder orderHolder =(NMOrderHolder) getShoppingCartHandler().getOrderHolder();
    // orderHolder.addProclivityItemToUpdate(commerceItem, currentQty.intValue(), quantity);
    
    commerceItem.setPropertyValue("quantity", new Long(quantity));
    @SuppressWarnings("unchecked")
    final List<ShippingGroupCommerceItemRelationship> relationships = commerceItem.getShippingGroupRelationships();
    for (final Iterator<ShippingGroupCommerceItemRelationship> it = relationships.iterator(); it.hasNext();) {
      final ShippingGroupCommerceItemRelationship relationship = it.next();
      relationship.setQuantity(quantity);
    }
  }
  
  public List<Message> validateLimitQuantity(final NMOrderImpl order) throws Exception {
    return validateLimitQuantity(order, true);
  }
  
  public List<Message> validateLimitQuantity(final NMOrderImpl order, final boolean messageErrorFlag) throws Exception {
    final ArrayList<Message> messages = new ArrayList<Message>();
    // List<String> maxPurchaseQtyList = new ArrayList<String>();
    final Repository productRepository = CommonComponentHelper.getProductRepository();
    
    final HashMap<String, Long> productQtyMap = new HashMap<String, Long>();
    final List<NMCommerceItem> commerceItems = order.getNmCommerceItems();
    final Iterator<NMCommerceItem> itCI = commerceItems.iterator();
    while (itCI.hasNext()) {
      final NMCommerceItem ci = itCI.next();
      final String productId = ci.getProductId();
      if (productQtyMap.containsKey(productId)) {
        final long totQty = productQtyMap.get(productId) + ci.getQuantity();
        productQtyMap.put(productId, totQty);
      } else {
        productQtyMap.put(productId, ci.getQuantity());
      }
    }
    
    final Set<Map.Entry<String, Long>> productQtySet = productQtyMap.entrySet();
    final Iterator<Map.Entry<String, Long>> itProduct = productQtySet.iterator();
    while (itProduct.hasNext()) {
      final Map.Entry<String, Long> m = itProduct.next();
      final String productId = m.getKey();
      // Long qty = (Long) m.getValue();
      
      final RepositoryItem productRI = productRepository.getItem(productId, "product");
      final boolean exceedMaxPurchaseQty = CommonComponentHelper.getProdSkuUtil().exceedMaxPurchaseQty(order, productRI);
      if (exceedMaxPurchaseQty) {
        final Message message = MessageDefs.getMessage(MessageDefs.MSG_LimitQuantity);
        message.setMsgText("We're sorry, the " + (String) productRI.getPropertyValue("displayName") + " is limited to " + CommonComponentHelper.getProdSkuUtil().getMaxPurchaseQty(productRI)
                + " per order. Please update the quantity.\n");
        message.setError(messageErrorFlag);
        messages.add(message);
      }
    }
    
    return messages;
  }
  
  public void checkDeclinedGwpSelects(final NMOrderImpl order, final DynamoHttpServletRequest request) {
    boolean changed = false;
    final Set<String> promoKeySet = order.getGwpSelectPromoMap().keySet();
    final Set<String> declinedGwpSelects = getDeclinedGwpSelects(request);
    for (final String declinedKey : declinedGwpSelects) {
      if (!promoKeySet.contains(declinedKey)) {
        declinedGwpSelects.remove(declinedKey);
        changed = true;
      }
    }
    if (changed) {
      setDeclinedGwpSelects(declinedGwpSelects, request);
    }
  }
  
  private Set<String> getDeclinedGwpSelects(final DynamoHttpServletRequest request) {
    final NMOrderHolder orderHolder = CheckoutComponents.getOrderHolder(request);
    return orderHolder.getDeclinedGwpSelects();
  }
  
  private void setDeclinedGwpSelects(final Set<String> declinedGwpSelects, final DynamoHttpServletRequest request) {
    final NMOrderHolder orderHolder = CheckoutComponents.getOrderHolder(request);
    orderHolder.setDeclinedGwpSelects(declinedGwpSelects);
  }
  
  /**
   * Determines if this is an international checkout session, based on the customer's country preference
   * 
   * @return
   */
  public boolean isInternationalSession(final NMProfile profile) {
    boolean internationalFlg = false;
    final String countryPreference = profile.getCountryPreference();
    if (StringUtilities.isNotEmpty(countryPreference)) {
      internationalFlg = localizationUtils.isSupportedByFiftyOne(countryPreference);
    }
    final GenericService config = CheckoutComponents.getConfig();
    if(config.isLoggingDebug()){
    	config.logDebug("isInternationalSession: " + internationalFlg);
    }
    return internationalFlg;
  }
  
  /**
   * Determines if the customer's order is empty
   * 
   * @return
   */
  public boolean isEmptyCart(final NMOrderImpl order) {
    final List<NMCommerceItem> items = order.getNmCommerceItems();
    boolean returnVal = false;
    if ((null == items) || (0 == items.size())) {
      returnVal = true;
    }
    
    return returnVal;
  }
  
  /**
   * Determines if the customer's order contains Virtual Gift Card
   * 
   * @return
   */
  public boolean containsVirtualGiftCard(final NMOrderImpl order) {
    
    final List<CommerceItem> items = order.getCommerceItems();
    final ProdSkuUtil prodSkuUtil = CommonComponentHelper.getProdSkuUtil();
    
    for (final CommerceItem item : items) {
      final NMCommerceItem ci = (NMCommerceItem) item;
      final NMProduct product = ci.getProduct();
      prodSkuUtil.isGiftCard(product.getDataSource());
      final boolean isVirtual = prodSkuUtil.isVirtualGiftCard(product.getDataSource());
      if (isVirtual) {
        return true;
      }
    }
    return false;
    
  }
  
  /**
   * This method checks if atleast one item in the Order is Alipay eligible
   * 
   * @param order
   * @return boolean
   */
  public boolean isAlipayEligible(final NMOrderImpl order) {
    final ProdSkuUtil prodSkuUtil = CommonComponentHelper.getProdSkuUtil();
    final List<CommerceItem> items = order.getCommerceItems();
    boolean isEligible = false;
    if ((null != items) && (items.size() > 0)) {
      for (final CommerceItem item : items) {
        final NMCommerceItem ci = (NMCommerceItem) item;
        if (ci.getAlipayEligibleFlg()) {
          isEligible = true;
        } else {
          isEligible = false;
        }
        if (isEligible) {
          break;
        }
      }
    }
    return isEligible;
  }
  
  /**
   * Determines if the customer's order contains Gift Card
   * 
   * @return
   */
  public boolean containsGiftCard(final NMOrderImpl order) {
    
    final List<CommerceItem> items = order.getCommerceItems();
    final ProdSkuUtil prodSkuUtil = CommonComponentHelper.getProdSkuUtil();
    
    for (final CommerceItem item : items) {
      final NMCommerceItem ci = (NMCommerceItem) item;
      final NMProduct product = ci.getProduct();
      prodSkuUtil.isGiftCard(product.getDataSource());
      final boolean isGiftCard = prodSkuUtil.isGiftCard(product.getDataSource());
      if (isGiftCard) {
        return true;
      }
    }
    return false;
    
  }
  
  /**
   * Applies the promo code
   * 
   * @author nmve1
   * 
   * @param promoBean
   * @param cart
   * @param profile
   * @return
   * @throws Exception
   */
  public ResultBean applyPromoCode(final PromoBean promoBean, final ShoppingCartHandler cart, final NMProfile profile) throws Exception {
    
    final GenericService config = CheckoutComponents.getConfig();
    if(config.isLoggingDebug()){
    	config.logDebug("--!--CartUtil applyPromoCode promoBean.getPromoCode->" + promoBean.getPromoCode() + ".");
    }
    final NMOrderImpl order = cart.getNMOrder();
    final NMOrderManager orderMgr = (NMOrderManager) cart.getOrderManager();
    final PromotionsHelper promoHelper = CheckoutComponents.getPromotionsHelper();
    final DynamoHttpServletRequest req = ServletUtil.getCurrentRequest();
    order.setPromoOrigin(promoBean.getOrigin());
    if(config.isLoggingDebug()){
    config.logDebug("--!--CartUtil applyPromoCode order.getId()->" + order.getId());
    }
    
    final ResultBean resultBean = new ResultBean();
    final List<Message> messageList = new ArrayList<Message>();
    final Map<String, String> emailPromos = order.getEmailValidationPromos();
    final ArrayList<String> badPromoCodes = new ArrayList<String>();
    
    try {
      boolean isPersonalizedPromo = false;
      String promoKey=null;
      final PersonalizedPromoTracking promoTracking = (PersonalizedPromoTracking) req.resolveName(promoTrackingComponent);
      final List<String> trackingPromoCodes = promoTracking.getPromoCodesPendingValidation();
      final List<String> trackingEmailAddrs = promoTracking.getPromoEmailAddresses();
      final Map<String, String> promoEmailMapping = promoTracking.getPromoEmailMapping();
      
      String requestedPromoCode = promoBean.getPromoCode();
      if (null == requestedPromoCode) {
        requestedPromoCode = "";
      } else {
        requestedPromoCode = requestedPromoCode.toUpperCase();
      }
      
      /*
       * START: mobile phase 1 does not support gwp multi sku, gwp select, or pwp.
       */
      /*
       * if(cartUtils.isMobile(codeReq.getOrigin())){ String revisedPromoCodes = ""; final String[] promoCodes = requestedPromoCode.split(","); for (int code = 0; code < promoCodes.length; ++code) {
       * final String promoCode = promoCodes[code].trim(); final NMPromotion[] promosForCode = cartUtils.getPromotionsForPromoCode(promoCode); if (null == promosForCode || promosForCode.length == 0){
       * badPromoCodes.add(promoCode); } else { for(int pos=0; pos < promosForCode.length; pos++){ if (promosForCode[pos] instanceof GiftWithPurchase) { //PromotionsHelper promoHelper =
       * getPromoHelper(); //RepositoryItem promotionalItemRI = promoHelper.getProdItem(((GiftWithPurchase)promosForCode[pos]).getGwpProduct(), null); RepositoryItem promotionalItemRI =
       * getProdSkuUtil().getProductRepositoryItem(((GiftWithPurchase)promosForCode[pos]).getGwpProduct()); if(getProdSkuUtil().getChildSkus(promotionalItemRI).size() > 1){ // add error message
       * Map<String, String> dynamicPropertyMap = new HashMap<String, String>(); dynamicPropertyMap.put("PROMO_CODE", promoCode); Message message =
       * MessageDefs.getMessage(MessageDefs.MSG_Promotion_MobileBadPromoCode, dynamicPropertyMap);
       * message.setMsgText("To receive the promotion '"+promoCode+"' please visit the full website or call "+getSystemSpecs().getEcarePhoneNumber()+"."); messageList.add(message); } else {
       * if(StringUtilities.isEmpty(revisedPromoCodes)){ revisedPromoCodes = promoCode; } else { revisedPromoCodes += "," + promoCode; } } } else if (promosForCode[pos] instanceof
       * GiftWithPurchaseSelect || promosForCode[pos] instanceof PurchaseWithPurchase) { // add error message Map<String, String> dynamicPropertyMap = new HashMap<String, String>();
       * dynamicPropertyMap.put("PROMO_CODE", promoCode); Message message = MessageDefs.getMessage(MessageDefs.MSG_Promotion_MobileBadPromoCode, dynamicPropertyMap);
       * message.setMsgText("To receive the promotion '"+promoCode+"' please visit the full website or call "+getSystemSpecs().getEcarePhoneNumber()+"."); messageList.add(message); } else {
       * if(StringUtilities.isEmpty(revisedPromoCodes)){ revisedPromoCodes = promoCode; } else { revisedPromoCodes += "," + promoCode; } } } } } if(StringUtilities.isEmpty(revisedPromoCodes)){
       * requestedPromoCode = ""; } else { requestedPromoCode = revisedPromoCodes; } }
       */
      /*
       * END: mobile phase 1 does not support gwp multi sku, gwp select, or pwp.
       */
      
      if (!promoTracking.isPendingValidationInProgress()) {
        promoTracking.setPendingValidationInProgress(true);
        promoTracking.setSavedCartOrigin(promoBean.getOrigin());
        
        String emailAddress = (String) order.getPropertyValue("homeEmail");
        if (StringUtilities.isNotEmpty(emailAddress) && !trackingEmailAddrs.contains(emailAddress)) {
          trackingEmailAddrs.add(emailAddress);
        }
        
        if (profile.isRegisteredProfile()) {
          emailAddress = (String) profile.getPropertyValue(ProfileProperties.Profile_email);
          if (StringUtilities.isNotEmpty(emailAddress) && !trackingEmailAddrs.contains(emailAddress)) {
            trackingEmailAddrs.add(emailAddress);
          }
        }
        
        final List<String> origCodes = promoTracking.getOriginalPromoCodes();
        final String[] promoCodes = requestedPromoCode.split(",");
        for (int code = 0; code < promoCodes.length; ++code) {
          final String promoCode = promoCodes[code].trim();
          if (StringUtilities.isNotEmpty(promoCode)) {
            origCodes.add(promoCode);
          }
          
          if (!trackingPromoCodes.contains(promoCode)) {
        	  final NMPromotion promosForCode = getPromotionsForPromoCode(promoCode, NMPromotionArray.PROMO_REQUIRES_EMAIL_VALIDATION);
        	  if(null != promosForCode && promosForCode.requiresEmailValidation()){
        		  trackingPromoCodes.add(promoCode);
        	  }
          }
        }
      }
      
      if (trackingPromoCodes.size() > 0) {
        final String emailAddress = promoBean.getPromoEmail();
        if (StringUtilities.isNotEmpty(emailAddress) && !trackingEmailAddrs.contains(emailAddress)) {
          trackingEmailAddrs.add(emailAddress);
        }
        
        while (trackingPromoCodes.size() > 0) {
          boolean currentCodeValid = false;
          final String promoCode = trackingPromoCodes.get(0);
          for (final Iterator<String> emailIter = trackingEmailAddrs.iterator(); emailIter.hasNext();) {
            final String email = emailIter.next();
            final int validEmail = promoHelper.validateEmailForPromo(promoCode, email);
            
            if (PromotionsHelper.PROMO_EMAIL_REDEEMED == validEmail) {
              trackingPromoCodes.remove(0);
              final List<String> origCodes = promoTracking.getOriginalPromoCodes();
              while (origCodes.remove(promoCode)) {
                promoHelper.incrementPromoEmailCount(promoCode, email, PromotionsHelper.PROMO_EMAIL_INC_ATTEMPT_COUNT);
              }
              final Map<String, Object> emailParams = new HashMap<String, Object>();
              emailParams.put("promoCode", promoCode);
              emailParams.put("promoCount", trackingPromoCodes.size() + "");
              // # final String frg = invokeNamedTemplate( NamedPaths.PROMO_HAS_BEEN_REDEEMED, emailParams );
              // # checkoutResp.setFrgLightbox( frg );
              final Message message = new Message();
              message.setError(false);
              message.setFieldId("PROMO_CODE_FIELD");
              message.setMsgId("PromoHasBeenRedeemed");
              message.setExtraParams(emailParams);
              messageList.add(message);
              resultBean.setMessages(messageList);
              if(config.isLoggingDebug()){
            	  config.logDebug("--!--CartUtil applyPromoCode PromoHasBeenRedeemed return.");
              }
              return (resultBean);
            }
            
            if (PromotionsHelper.PROMO_EMAIL_VALID == validEmail) {
              
              trackingPromoCodes.remove(0);
              promoHelper.incrementPromoEmailCount(promoCode, email, PromotionsHelper.PROMO_EMAIL_INC_ATTEMPT_COUNT);
              promoEmailMapping.put(promoCode, email);
              emailPromos.put(promoCode, email);
              promoBean.setPromoEmail(null);
              currentCodeValid = true;
              break;
            }
          }
          
          if ((trackingPromoCodes.size() > 0) && !currentCodeValid) {
            final Map<String, Object> emailParams = new HashMap<String, Object>();
            emailParams.put("initialEmailRequest", (null == promoBean.getPromoEmail()) ? "Y" : "N");
            emailParams.put("promoCode", promoCode);
            // # final String frg = invokeNamedTemplate( NamedPaths.REQUEST_PROMO_EMAIL, emailParams );
            // # checkoutResp.setFrgLightbox( frg );
            final Message message = new Message();
            message.setError(false);
            message.setFieldId("PROMO_CODE_FIELD");
            message.setMsgId("RequestPromoEmail");
            message.setExtraParams(emailParams);
            messageList.add(message);
            resultBean.setMessages(messageList);
            if(config.isLoggingDebug()){
            	config.logDebug("--!--CartUtil applyPromoCode RequestPromoEmail trackingPromoCodes.size() > 0 && !currentCodeValid return.");
            }
            return (resultBean);
          }
        }
      }
      
      final StringBuffer buf = new StringBuffer();
      for (final Iterator<String> i = promoTracking.getOriginalPromoCodes().iterator(); i.hasNext();) {
        final String code = i.next();
        if (buf.length() > 0) {
          buf.append(",");
        }
        
        buf.append(code);
      }
      if(config.isLoggingDebug()){
    	  config.logDebug("--!--CartUtil applyPromoCode buf.toString->" + buf.toString() + ", requestedPromoCode->" + requestedPromoCode + ".");
      }
      requestedPromoCode = buf.toString();
      if(config.isLoggingDebug()){
    	  config.logDebug("--!--CartUtil applyPromoCode requestedPromoCode->" + requestedPromoCode + ".");
      }
      if (promoTracking.isPendingValidationInProgress()) {
        promoBean.setOrigin(promoTracking.getSavedCartOrigin());
      }
      
      clearPromoCodePendingValidation(null);
      /*
       * if (StringUtilities.isEmpty(requestedPromoCode)) { logDebug("--!--CartService applyPromoCode StringUtilities.isEmpty(requestedPromoCode) return."); return(checkoutResp); }
       */
      // check comma delimited promo code string for conflicts and construct new non-conflict string
      
      final List<Message> conflictList = new ArrayList<Message>();
      if ((requestedPromoCode != null) && (requestedPromoCode.indexOf(",") != -1)) {
        requestedPromoCode = removeConflictsInDelimitedPromoCodeRequest(requestedPromoCode, badPromoCodes, conflictList, order);
      }
      
      if (requestedPromoCode != null) {
    	  if(config.isLoggingDebug()){
    		  config.logDebug("--!--CartUtil applyPromoCode requestedPromoCode != null.");
    	  }
        final StringTokenizer st = new StringTokenizer(requestedPromoCode, ",");
        while (st.hasMoreElements()) {
          String currentCode = (String) st.nextElement();
          if (currentCode != null) {
            currentCode = currentCode.trim().toUpperCase();
          }
          
          // Returns quickly with standard promotion code if not dynamic.
          final String filteredCode = DynamicCodeUtils.substituteCode(currentCode, order);
          if (filteredCode == null) {
            continue;
          }
          
          // before adding the requested promo code to the order, remove any promos in conflict and construct message
          final Message conflictingPromoCodeMessage = handleConflictingActivatedPromoCodes(filteredCode, order);
          
          BrandSpecs brandSpecs =  CommonComponentHelper.getBrandSpecs();
          Object persObj=profile.getEdwCustData().getPersonalizedCustData();
          List<String> persPromoCodes= null;
          if (persObj != null && brandSpecs.isEnableSitePersonalization()) {
            persPromoCodes=((PersonalizedCustData)persObj).getPersonalizedPromos();
          }
          
          NMPromotion nmpromo = null;
          ComponentUtils componentUtils = ComponentUtils.getInstance();
          NMPromotionArray promotionArray = componentUtils.getPromotionArray(NMPromotionArray.RULE_BASED_PROMOTIONS_ARRAY);
          if(null != promotionArray){
        	  nmpromo = getValidPromotion(promotionArray.getActivePromotionsWithPromoCode(filteredCode), NMPromotionArray.PERSONALIZED_FLAG);  
          }
          
          // for dollar off promotion 
          if(null != nmpromo){
            isPersonalizedPromo=true; 
            promoKey= RuleHelper.getPromoKeys((Promotion)nmpromo);
          }
          // for shipping 103 & 104 type promotions
          else {
        	  promotionArray = componentUtils.getPromotionArray(NMPromotionArray.SHIPPING_PROMOTIONS_ARRAY);
        	  if(null != promotionArray){
        		  nmpromo = getValidPromotion(promotionArray.getActivePromotionsWithPromoCode(filteredCode), NMPromotionArray.PERSONALIZED_FLAG);
        	  }
				if (null != nmpromo) {
					isPersonalizedPromo = true;
					if (null != nmpromo.getCode()) {
						promoKey = nmpromo.getCode().trim().toUpperCase();
					}
				}
          }
           
          
          
          // personalized flag is true for dollar off and shipping promotions and the code does not match api response, skip it. else apply.
          if(isPersonalizedPromo){    	  
            if(persPromoCodes!=null && persPromoCodes.contains(promoKey) ){
              addPromo(filteredCode, cart);
            }   	
            else{
              order.setBcPromoError(true);
            }
          }  
          else{
            addPromo(filteredCode, cart);
          }
          
          // ! OmnitureUtils.getInstance().submitGWP( checkoutResp, profile, order );
          
          // add promoCode as a param to the conflict message if it was applied to the order
          if (conflictingPromoCodeMessage != null) {
            if (order.getActivePromoCodeList().contains(filteredCode)) {
              final Map<String, Object> extras = new HashMap<String, Object>();
              extras.put("promoCode", filteredCode);
              conflictingPromoCodeMessage.setExtraParams(extras);
            }
            messageList.add(conflictingPromoCodeMessage);
          }
        }
        if(config.isLoggingDebug()){
        	config.logDebug("--!--CartUtil applyPromoCode msgstack messageList->" + messageList.size() + ".");
        }
      }
      if (messageList.isEmpty()) {
        if (!conflictList.isEmpty()) {
          messageList.add(conflictList.get(0));
        }
      }
      
      final Collection<String> promoCodes = StringUtilities.makeList(requestedPromoCode);
      final Iterator<String> iterator = promoCodes.iterator();
      
      final Set<NMPromotion> awardedPromos = order.getAwardedPromotions();
      if (null != awardedPromos) {
        for (final Iterator<NMPromotion> i = awardedPromos.iterator(); i.hasNext();) {
          final NMPromotion nmPromotion = i.next();
          if (nmPromotion.requiresEmailValidation()) {
            final String promoCode = nmPromotion.getPromoCodes();
            final String email = promoEmailMapping.get(promoCode);
            emailPromos.put(promoCode, email);
          }
        }
      }
      if(config.isLoggingDebug()){
    	  config.logDebug("--!--CartUtil applyPromoCode iterator.hasNext->" + iterator.hasNext() + ", requestedPromoCode->" + requestedPromoCode + ".");
      }
      if (iterator.hasNext()) {
        while (iterator.hasNext()) {
          final String promoCode = iterator.next();
          // do not process code if it is empty
          if (StringUtilities.isNotEmpty(promoCode)) {
        	  if(config.isLoggingDebug()){
        		  config.logDebug("--!--CartUtil applyPromoCode iterator.hasNext promoCode->" + promoCode + ".");
        	  }
            
            final String filteredCode = DynamicCodeUtils.toggleSubstitution(promoCode, order);
            
            if (isPromoApplied(filteredCode, order)) {
              final NMPromotion promotion = order.getAwardedPromotion(filteredCode);
              
              if (promotion != null) {
                final int promotionClass = promotion.getPromotionClass();
                if ((promotionClass == NMPromotion.GIFT_WRAP) && !order.hasGiftwrapping()) {
                  final Message message = MessageDefs.getMessage(MessageDefs.MSG_Promotion_GiftwrapReminder);
                  message.setError(true);
                  message.setMsgText("Please remember to select gift options for the item(s) you would like gift wrapped");
                  messageList.add(message);
                  if(config.isLoggingDebug()){
                	  config.logDebug("--!--CartUtil applyPromoCode iterator.hasNext promotionClass->" + message.getMsgText() + ".");
                  }
                } else if ((promoBean.getOrigin() == RequestBean.CHECKOUT_ORIGIN) && (promotionClass == NMPromotion.SHIPPING)
                        && !ShippingUtil.getInstance().areAllSelectedServiceLevelsPromotional(order)) {
                  final Message message = MessageDefs.getMessage(MessageDefs.MSG_Promotion_ShipMethodDisqualification);
                  message.setError(true);
                  message.setMsgText("Due to the shipping method selected, delivery & processing charges may not reflect the promotional amount. If this selection was made in error, you may select \"Promotional\" in the Shipping Method box to update your order.");
                  messageList.add(message);
                  if(config.isLoggingDebug()){
                	  config.logDebug("--!--CartUtil applyPromoCode iterator.hasNext codeReq.getOrigin->" + message.getMsgText() + ".");
                  }
                }
                if (promotion.isPromoReinforcementFlag()) {
                  final Message message = MessageDefs.getMessage(MessageDefs.MSG_Promotion_Reinforcement);
                  message.setError(true);
                  message.setMsgText(promotion.getPromoReinforcementHtml());
                  messageList.add(message);
                }
              }
            } else {
              badPromoCodes.add(promoCode);
            }
          }
        }
      } else if (requestedPromoCode.trim().length() > 0) {
        badPromoCodes.add(requestedPromoCode);
      }
      if(config.isLoggingDebug()){
    	  config.logDebug("--!--CartUtil applyPromoCode msgstack messageList->" + messageList.size() + ".");
      }
      if (badPromoCodes.size() > 0) {
        // ! if ( cartUtils.isMobile( promo.getOrigin() ) ) {
        // ! Map<String, String> dynamicPropertyMap = new HashMap<String, String>();
        // ! dynamicPropertyMap.put( "PROMO_CODE", StringUtilities.makeString( badPromoCodes ) );
        // ! Message message = MessageDefs.getMessage( MessageDefs.MSG_Promotion_MobileBadPromoCode, dynamicPropertyMap );
        // ! messageList.add( message );
        // ! }
        // ! else {
        final String promoCode = StringUtilities.makeString(badPromoCodes);
        final Message message = MessageDefs.getMessage(MessageDefs.MSG_Promotion_BadPromoCode);
        final HashMap<String, Object> parameters = new HashMap<String, Object>();
        
        parameters.put("promoCode", promoCode);
        message.setError(true);
        final StringBuffer msgText = new StringBuffer();
        final boolean plccEnabled = CommonComponentHelper.getSystemSpecs().isPercentOffPromoForPLCCEnabled();
        if(plccEnabled && ((NMOrderImpl)order).getPromoOrigin() == 1 && ((NMOrderImpl)order).hasPlccPromoError()){
        	((NMOrderImpl)order).setPlccPromoError(false);
    		msgText.append("This order does not qualify for the promotion <b>").append(promoCode).append("</b>.<br/>")
			.append("Please note: You must pay with a Neiman Marcus or Bergdorf Goodman credit card to qualify for the promotion ").append(promoCode).append("</b>.<br/>")
			.append("Click Place Order to checkout. You may also revise your order to qualify for our offer.");
        } 
        else if(((NMOrderImpl)order).isBcPromoError()){
          ((NMOrderImpl)order).setBcPromoError(false);
         // msgText.append(" You are not authorized best customer to access this promotion");
          msgText.append("Sorry, we could not apply the promo code to this order. Please check the qualifying details for this offer.");
        }
        else {
        msgText.append("Sorry, there was an error. Please check the code & qualifying details for this promotion <b>").append(promoCode).append("</b>.");
        }
        
        message.setMsgText(msgText.toString());
        message.setExtraParams(parameters);
        messageList.add(message);
        if(config.isLoggingDebug()){
        config.logDebug("--!--CartUtil applyPromoCode badPromoCodes.size() > 0 promoCode->" + promoCode + ", message->" + message.toString() + ".");
        }
        // ! }
        
      }
      if(config.isLoggingDebug()){
      config.logDebug("--!--CartUtil applyPromoCode messageList->" + messageList.size() + ".");
      }
      // ! super.processMessages( messageList, checkoutResp );
      if(config.isLoggingDebug()){
      config.logDebug("--!--CartUtil applyPromoCode after processMessages messageList->" + messageList.size() + ".");
      }
      
      // ! if ( cartUtils.isMobile( promo.getOrigin() ) ) {
      // ! cartUtils.addMobileCartForm( this, checkoutResp.getMessages(), checkoutResp );
      // ! }
      // ! else {
      // ! addCartResponse( checkoutResp, promo.getOrigin(), checkoutResp.getMessages(), SnapshotUtil.EDIT_ITEM_FROM_SUMMARY_CART );
      // ! addServiceLevels( checkoutResp );
      
      // ! shippingUtils.populateShippingRefreshFragments( profile, order, orderManager, this, promo, checkoutResp );
      
      // ! checkoutResp.setAmountRemainingOnOrder( PaymentUtil.getInstance().calcAmountRemainingOnOrder( orderMgr, order ) );
      // -
      // ! config.logDebug( "--------CartUtil  checkoutResp.getAmountRemainingOnOrder(): " + checkoutResp.getAmountRemainingOnOrder());
      // !}
      
      orderMgr.updateOrder(order);
    } catch (final Exception e) {
      config.logError(e);
      throw (e);
    }
    
    // ! super.removeNonClientMessages( checkoutResp );
    resultBean.setMessages(messageList);
    return (resultBean);
  }
  
  /**
   * @param clearReq
   * @return
   */
  public ResultBean clearPromoCodePendingValidation(final PromoEmailClearRequest clearReq) {
    final DynamoHttpServletRequest req = ServletUtil.getCurrentRequest();
    final PersonalizedPromoTracking promoTracking = (PersonalizedPromoTracking) req.resolveName(promoTrackingComponent);
    final List<String> trackingPromoCodes = promoTracking.getPromoCodesPendingValidation();
    trackingPromoCodes.clear();
    promoTracking.setPendingValidationInProgress(false);
    promoTracking.getOriginalPromoCodes().clear();
    
    return (new ResultBean());
  }
  
  /**
   * Copied the method from the com.nm.ajax.checkout.cart.CartUtils class
   * 
   * @author nmve1
   * 
   * @param promoCode
   * @return
   */
  public NMPromotion getPromotionsForPromoCode(final String promoCode, int condition) {
    NMPromotion promotion = null;
    final ComponentUtils componentUtils = ComponentUtils.getInstance();
    final int[] promoArrayTypes = componentUtils.getPromotionArrayTypes();
    final int checkOutArrayLength = promoArrayTypes.length;
    
    for (int i = 0; i < checkOutArrayLength; i++) {
    	final NMPromotionArray promotionArray = componentUtils.getPromotionArray(promoArrayTypes[i]);
    	if(null != promotionArray){
    		if(null != (promotion = getValidPromotion(promotionArray.getActivePromotionsWithPromoCode(promoCode), condition))){
        		break;
        	}
    	}
	}
    
    return promotion;
  }
  
  private NMPromotion getValidPromotion(NMPromotion promotion, final int condition) {
	  NMPromotion validPromotion = null;
	  if(null != promotion ) {
		 switch (condition) {
		 case NMPromotionArray.PROMO_REQUIRES_EMAIL_VALIDATION:
			 if(promotion.requiresEmailValidation()) {
				 validPromotion = promotion;
				 break;
			 }
		 case NMPromotionArray.PERSONALIZED_FLAG:
			 if(promotion.isPersonalizedFlag()) {
				 validPromotion = promotion;
				 break;
			 }
		 }
	  }
	  
	  return validPromotion;
  }
  
  /**
   * Copied the method from the com.nm.ajax.checkout.services.CartService class
   * 
   * @author nmve1
   * 
   * @param requestedPromoCode
   * @param badPromoCodes
   * @param messageList
   * @return
   */
  public String removeConflictsInDelimitedPromoCodeRequest(final String requestedPromoCode, final ArrayList<String> badPromoCodes, final List<Message> messageList, final NMOrderImpl order) {
    final GenericService config = CheckoutComponents.getConfig();
    // ex: DOLLAR1,DOLLAR2,PERCENT1
    // where DOLLAR1 and DOLLAR2 are both active promos, keep only
    // if promocodes in the incoming string are comma delimited and in conflict, keep the last code entered
    boolean codeListContainsConflictingCode = false;
    boolean foundConflict = false;
    
    final Message conflictMessage = MessageDefs.getMessage(MessageDefs.MSG_Promotion_ConflictingMarkdownPromoCode);
    
    final StringBuffer buildNonConflictString = new StringBuffer();
    
    final List<String> promoCodesIn = new ArrayList<String>();
    
    if (requestedPromoCode != null) {
      final StringTokenizer st = new StringTokenizer(requestedPromoCode, ",");
      while (st.hasMoreElements()) {
        final String codeIn = (String) st.nextElement();
        if (codeIn != null) {
          promoCodesIn.add(codeIn.trim().toUpperCase());
        }
      }
      Collections.reverse(promoCodesIn);
      
      boolean firstElement = true;
      
      final Iterator<String> i = promoCodesIn.iterator();
      
      while (i.hasNext()) {
        final String currentPromoCode = i.next();
        
        if (((RuleHelper.isActiveDollarOffPromoCode(currentPromoCode) || RuleHelper.isActivePercentOffPromoCode(currentPromoCode))) && codeListContainsConflictingCode) {
          // badPromoCodes.add(currentPromoCode);
          foundConflict = true;
          if(config.isLoggingDebug()){
          config.logDebug("--!--CartUtil removeConflictsInDelimitedPromoCodeRequest continue currentPromoCode->" + currentPromoCode + ".");
          }
          continue;
        }
        
        if ((RuleHelper.isActiveDollarOffPromoCode(currentPromoCode) || RuleHelper.isActivePercentOffPromoCode(currentPromoCode)) && !codeListContainsConflictingCode) {
          codeListContainsConflictingCode = true;
          if (!firstElement) {
            buildNonConflictString.append(",");
          }
          buildNonConflictString.append(currentPromoCode);
          firstElement = false;
          final Map<String, Object> extras = new HashMap<String, Object>();
          extras.put("promoCode", currentPromoCode);
          final StringBuffer msgTxt = new StringBuffer();
          msgTxt.append("Please note only one Dollar Off or Percent Off promotion can be used at a time.").append("<br>")
                  .append("The most recent promotional code you entered will be used on this order")
                  .append((null == currentPromoCode) || currentPromoCode.equals("") ? "." : ": " + "<b>" + DynamicCodeUtils.toggleSubstitution(currentPromoCode, order) + "</b>").append("<br>");
          conflictMessage.setError(true);
          conflictMessage.setMsgText(msgTxt.toString());
          conflictMessage.setExtraParams(extras);
          if(config.isLoggingDebug()){
          config.logDebug("--!--CartUtil removeConflictsInDelimitedPromoCodeRequest isActiveDollarOffPromoCode currentPromoCode->" + currentPromoCode + ".");
          }
        } else {
          if (!firstElement) {
            buildNonConflictString.append(",");
          }
          buildNonConflictString.append(currentPromoCode);
          firstElement = false;
          if(config.isLoggingDebug()){
          config.logDebug("--!--CartUtil removeConflictsInDelimitedPromoCodeRequest else currentPromoCode->" + currentPromoCode + ".");
          }
        }
      }
    }
    if (foundConflict) {
      messageList.add(conflictMessage);
    }
    if(config.isLoggingDebug()){
    config.logDebug("--!--CartUtil removeConflictsInDelimitedPromoCodeRequest buildNonConflictString->" + buildNonConflictString.toString() + ", requestedPromoCode->" + requestedPromoCode + ".");
    }
    return buildNonConflictString.toString();
  }
  
  /**
   * Copied the method from the com.nm.ajax.checkout.cart.CartUtils class
   * 
   * @author nmve1
   * 
   * @param promoCode
   * @param cart
   * @return
   * @throws Exception
   */
  public boolean addPromo(final String promoCode, final ShoppingCartHandler cart) throws Exception {
    // clearOrderDiscounts(order, orderMgr);
    
    boolean applied = false;
    final NMOrderImpl order = cart.getNMOrder();
    
    // final NMOrderManager orderMgr = (NMOrderManager)cart.getOrderManager();
    
    if ((promoCode != null) && (!promoCode.trim().equals(""))) {
      updatePromoField(promoCode, cart);
      
      // need to set this flag so if a Registered User who started the session anon
      // will keep any entered promocodes
      // he entered during his session IF he signs in...
      // OrderHolder orderHolder = new OrderHolder();
      // ((NMOrderHolder) getOrderHolder()).setNeedingCartUtilityUpdate(false);
      
      // #promoUtils.recalculateGwpSelectPromoMap( order );
      CheckoutComponents.getPromoUtils().recalculateGwpSelectPromoMap(order);
      
      applied = isPromoApplied(promoCode, order);
    }
    return applied;
  }
  
  /**
   * Copied the method from the com.nm.ajax.checkout.cart.CartUtils class
   * 
   * @author nmve1
   * 
   * @param promoCode
   * @param cart
   * @return
   * @throws Exception
   */
  // should already be in a transaction for this
  private String updatePromoField(final String promoCode, final ShoppingCartHandler cart) throws Exception {
    
    final NMOrderImpl order = cart.getNMOrder();
    final NMOrderManager orderMgr = (NMOrderManager) cart.getOrderManager();
    final PromotionEvaluation promoEvaluation = CheckoutComponents.getPromotionEvaluation();
    
    synchronized (order) {
      if ((promoCode != null) && (!promoCode.trim().equals(""))) {
        // don't fire an event for the scenario manager any more
        // promoCodeMessageSource.sendPromoCodeMessage(getPromoCode(), order);
        
        orderMgr.updateOrder(order);
        order.addPromoCode(promoCode);
        orderMgr.updateOrder(order);
        
        promoEvaluation.evaluatePromotions(order);
        
        if (order.shouldValidateCountry()) {
          // !
          // #ShippingUtils.getInstance().validateShipToCountries( order, orderMgr, new ArrayList<Message>(), service );
          ShippingUtil.getInstance().validateShipToCountries(cart, new ArrayList<Message>());
        }
      }
      
      return order.getPromoCode();
    }
  }
  
  /**
   * Copied the method from the com.nm.ajax.checkout.cart.CartUtils class
   * 
   * @author nmve1
   * 
   * @param promoToCheck
   * @param order
   * @return
   */
  public boolean isPromoApplied(String promoToCheck, final NMOrderImpl order) {
    promoToCheck = (promoToCheck != null) ? promoToCheck.trim().toUpperCase() : "";
    boolean found = false;
    
    if (!promoToCheck.equals("") && (order != null)) {
      
      // is the promo in the "active" list?
      final List<String> activePromos = order.getActivePromoCodeList();
      Iterator<String> iterator = activePromos.iterator();
      while (iterator.hasNext()) {
        final String promoCode = iterator.next();
        if ((promoCode != null) && promoCode.equals(promoToCheck)) {
          found = true;
          break;
        }
      }
      
      // is the promo in the "ignored" list?
      if (!found) {
        final List<String> ignoredPromos = order.getIgnoredPromoCodeList(NMOrderImpl.ALL_PROMOTION_TYPES);
        iterator = ignoredPromos.iterator();
        while (iterator.hasNext()) {
          final String promoCode = iterator.next();
          if ((promoCode != null) && promoCode.equals(promoToCheck)) {
            found = true;
            break;
          }
        }
      }
    }
    
    return found;
  }
  
  /**
   * Copied the method from the com.nm.ajax.checkout.services.CartService class
   * 
   * @author nmve1
   * 
   * @param requestedPromoCode
   * @param order
   * @return
   */
  public Message handleConflictingActivatedPromoCodes(final String requestedPromoCode, final NMOrderImpl order) {
    Message conflictingPromoCodeMessage = null;
    List<String> removedPercentOffs;
    List<String> removedDollarOffs;
    StringBuffer msgTxt = null;
    
    // if promoCode is in conflict with existing promo codes, remove existing conflicts
    if (RuleHelper.isActiveDollarOffPromoCode(requestedPromoCode)) {
      removedPercentOffs = removeConflictingPercentOffPromos(order);
      removedDollarOffs = removeConflictingDollarOffPromos(order);
      if (!removedPercentOffs.isEmpty()) {
        conflictingPromoCodeMessage = MessageDefs.getMessage(MessageDefs.MSG_Promotion_ConflictingMarkdownPromoCode);
        conflictingPromoCodeMessage.setError(true);
        msgTxt = new StringBuffer();
        msgTxt.append("Please note only one Dollar Off or Percent Off promotion can be used at a time.").append("<br>")
                .append("The most recent promotional code you entered will be used on this order")
                .append((null == requestedPromoCode) || requestedPromoCode.equals("") ? "." : ": " + "<b>" + DynamicCodeUtils.toggleSubstitution(requestedPromoCode, order) + "</b>").append("<br>");
        conflictingPromoCodeMessage.setMsgText(msgTxt.toString());
      } else {
        if (!removedDollarOffs.isEmpty()) {
          conflictingPromoCodeMessage = MessageDefs.getMessage(MessageDefs.MSG_Promotion_ConflictingDollarOffPromoCode);
          conflictingPromoCodeMessage.setError(true);
          msgTxt = new StringBuffer();
          msgTxt.append("Please note only one Dollar Off promotion can be used at a time.").append("<br>").append("The most recent promotional code you entered will be used on this order")
                  .append((null == requestedPromoCode) || requestedPromoCode.equals("") ? "." : ": " + "<b>" + DynamicCodeUtils.toggleSubstitution(requestedPromoCode, order) + "</b>").append("<br>");
          conflictingPromoCodeMessage.setMsgText(msgTxt.toString());
        }
      }
    }
    if (RuleHelper.isActivePercentOffPromoCode(requestedPromoCode)) {
      removedDollarOffs = removeConflictingDollarOffPromos(order);
      if (!removedDollarOffs.isEmpty()) {
        conflictingPromoCodeMessage = MessageDefs.getMessage(MessageDefs.MSG_Promotion_ConflictingMarkdownPromoCode);
        conflictingPromoCodeMessage.setError(true);
        msgTxt = new StringBuffer();
        msgTxt.append("Please note only one Dollar Off or Percent Off promotion can be used at a time.").append("<br>")
                .append("The most recent promotional code you entered will be used on this order")
                .append((null == requestedPromoCode) || requestedPromoCode.equals("") ? "." : ": " + "<b>" + DynamicCodeUtils.toggleSubstitution(requestedPromoCode, order) + "</b>").append("<br>");
        conflictingPromoCodeMessage.setMsgText(msgTxt.toString());
      }
    }
    
    return conflictingPromoCodeMessage;
  }
  
  /**
   * Copied the method from the com.nm.ajax.checkout.services.CartService class
   * 
   * @author nmve1
   * 
   * @param order
   * @return
   */
  public List<String> removeConflictingPercentOffPromos(final NMOrderImpl order) {
    final String promoCodes = order.getPromoCode();
    final List<String> removedPromoCodes = new ArrayList<String>();
    
    try {
      
      if (promoCodes != null) {
        final StringTokenizer st = new StringTokenizer(promoCodes, ",");
        while (st.hasMoreElements()) {
          final String promoCode = (String) st.nextElement();
          final Repository repo = (Repository) Nucleus.getGlobalNucleus().resolveName("/nm/xml/CsrPercentOffRepository");
          final RepositoryView view = repo.getView("percentOffPromotions");
          final RqlStatement statement = RqlStatement.parseRqlStatement("promoCodes = ?0");
          final Object params[] = new Object[] {promoCode};
          final RepositoryItem[] items = statement.executeQuery(view, params);
          if (items != null) {
            order.removePromoCode(promoCode);
            removedPromoCodes.add(promoCode);
            order.setRemoveActivatedPromoCode(promoCode);
            order.setRemoveUserActivatedPromoCode(promoCode);
          }
        }
      }
    } catch (final Exception e) {
      e.printStackTrace();
      order.setPromoCode(promoCodes);
    }
    return removedPromoCodes;
  }
  
  /**
   * Copied the method from the com.nm.ajax.checkout.services.CartService class
   * 
   * @author nmve1
   * 
   * @param order
   * @return
   */
  public List<String> removeConflictingDollarOffPromos(final NMOrderImpl order) {
    final String promoCodes = order.getPromoCode();
    final List<String> removedPromoCodes = new ArrayList<String>();
    
    try {
      
      if (promoCodes != null) {
        final StringTokenizer st = new StringTokenizer(promoCodes, ",");
        while (st.hasMoreElements()) {
          final String promoCode = (String) st.nextElement();
          if (RuleHelper.isActiveDollarOffPromoCode(promoCode)) {
            order.removePromoCode(promoCode);
            removedPromoCodes.add(promoCode);
            order.setRemoveActivatedPromoCode(promoCode);
            order.setRemoveUserActivatedPromoCode(promoCode);
          }
        }
      }
    } catch (final Exception e) {
      e.printStackTrace();
      order.setPromoCode(promoCodes);
    }
    return removedPromoCodes;
  }
  
  private String nullEmpty(final Object obj) {
    return (obj == null) ? "" : obj.toString();
  }
  
  /**
   * validateBorderFreeSupportedCard method will get the available payment methods from border free response session object and compares each one with the default credit card in the profile and
   * returns cardSupportedFlag as true if default credit card is one among them
   * 
   * @param NMProfile
   *          nmProfile
   * 
   * @return cardSupportedFlag
   */
  public boolean validateBorderFreeSupportedCard(final String defaultCreditCardType, final NMBorderFreeGetQuoteResponse nmBorderFreeGetQuoteResponse) {
    boolean isCardSupportedForCountry = false;
    boolean chinaUnionPay = false;
    final NMProfile profile = CheckoutComponents.getProfile(ServletUtil.getCurrentRequest());
    if (!StringUtils.isBlank(defaultCreditCardType) && !profile.getDefaultCreditCard().isPayPal()) {
      if ((null != nmBorderFreeGetQuoteResponse) && (null != nmBorderFreeGetQuoteResponse.getAvailablePaymentMethodsResponseVO())) {
        final AvailablePaymentMethodsResposeVO[] availablePaymentMethodsVO = nmBorderFreeGetQuoteResponse.getAvailablePaymentMethodsResponseVO();
        for (final AvailablePaymentMethodsResposeVO paymentMethod : availablePaymentMethodsVO) {
          final String cardType = paymentMethod.getDisplayName();
          if (cardType.equalsIgnoreCase(CHINA_UNION_PAY)) {
            chinaUnionPay = true;
          }
          if (cardType.equalsIgnoreCase(defaultCreditCardType)) {
            isCardSupportedForCountry = true;
            break;
          }
        }
      }
      if (!chinaUnionPay && defaultCreditCardType.equalsIgnoreCase(CHINA_UNION_PAY)) {
        isCardSupportedForCountry = true;
      }
    }
    return isCardSupportedForCountry;
  }
  
  /**
   * validateDomesticSupportedCard method will get the available payment methods supported by NMG for US country and compares each one with the default credit card in the profile and returns
   * domesticSupportedCardFlag as true if default credit card is one among them
   * 
   * @param NMProfile
   *          nmProfile
   * 
   * @return domesticSupportedCardFlag
   */
  public boolean validateDomesticSupportedCard(final String defaultCreditCardType) {
    boolean isDomesticSupportedCard = false;
    final CreditCard[] displayedCreditCards = CheckoutComponents.getCreditCardArray().getDisplayedCreditCards();
    if (!StringUtils.isBlank(defaultCreditCardType) && (null != displayedCreditCards)) {
      for (final CreditCard ccType : displayedCreditCards) {
        final String cardType = ccType.getLongName();
        if (cardType.equalsIgnoreCase(defaultCreditCardType)) {
          isDomesticSupportedCard = true;
          break;
        }
      }
    }
    final SystemSpecs systemSpecs = CommonComponentHelper.getSystemSpecs();
    if (!StringUtils.isBlank(defaultCreditCardType) && systemSpecs.isPaypalEnabled() && defaultCreditCardType.equalsIgnoreCase("PayPal")) {
      isDomesticSupportedCard = true;
    }
    return isDomesticSupportedCard;
  }
  
  /**
   * validateBorderFreeSupportedCountries method will get all the BorderFree supported countries from localization repository and compares each country with the default billing address country in the
   * profile. If both are equal it sets the boolean borderFreeSupportedCountryFlag to true and breaks
   * 
   * @param NMProfile
   *          nmProfile
   * 
   * @return boolean borderFreeSupportedCountryFlag
   */
  public boolean validateBorderFreeSupportedCountries(final NMProfile nmProfile) {
    boolean isBorderFreeSupportedCountry = false;
    if ((null != nmProfile.getBillingNMAddress()) && (null != nmProfile.getBillingNMAddress().getCountry())) {
      final RepositoryItem[] borderFreeSupportedCountries = localizationUtils.getAllShipEnabledCountries();
      if ((borderFreeSupportedCountries != null) && (borderFreeSupportedCountries.length > 0)) {
        for (final RepositoryItem borderFreeSupportedCountry : borderFreeSupportedCountries) {
          final String borderFreeSupportedCountryName = (String) borderFreeSupportedCountry.getPropertyValue("countryCode");
          if (borderFreeSupportedCountryName.equalsIgnoreCase(nmProfile.getBillingNMAddress().getCountry())) {
            isBorderFreeSupportedCountry = true;
            break;
          }
        }
      }
    }
    return isBorderFreeSupportedCountry;
  }
}
