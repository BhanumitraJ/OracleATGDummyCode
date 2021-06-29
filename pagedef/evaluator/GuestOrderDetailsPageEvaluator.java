package com.nm.commerce.pagedef.evaluator;

import static com.nm.tms.constants.TMSDataDictionaryConstants.ACCOUNTS;
import static com.nm.tms.constants.TMSDataDictionaryConstants.GUEST_ORDER_DETAILS;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.servlet.http.HttpSession;
import javax.servlet.jsp.PageContext;

import atg.core.util.StringUtils;
import atg.naming.NameContext;
import atg.repository.RepositoryException;
import atg.repository.RepositoryItem;

import com.nm.commerce.NMCommerceItem;
import com.nm.commerce.NMCommerceProfileTools;
import com.nm.commerce.NMProfile;
import com.nm.commerce.checkout.CheckoutComponents;
import com.nm.commerce.order.NMOrderManager;
import com.nm.commerce.pagedef.definition.PageDefinition;
import com.nm.commerce.pagedef.model.GuestOrderDetailsPageModel;
import com.nm.commerce.pagedef.model.PageModel;
import com.nm.commerce.pricing.NMOrderPriceInfo;
import com.nm.commerce.promotion.NMOrderImpl;
import com.nm.components.CommonComponentHelper;
import com.nm.international.fiftyone.checkoutapi.OrderRepositoryConstants;
import com.nm.order.history.vo.OrderLineItemVO;
import com.nm.returnscredit.ReturnsCreditConstants;
import com.nm.returnscredit.config.ReturnsCreditConfig;
import com.nm.returnscredit.integration.ReturnsCreditCmosOrderVO;
import com.nm.returnscredit.integration.ReturnsCreditLineItemVO;
import com.nm.returnscredit.integration.ReturnsCreditSession;
import com.nm.tms.core.TMSMessageContainer;
import com.nm.tms.utils.TMSDataDictionaryUtils;
import com.nm.utils.LocalizationUtils;

/**
 * This is an evaluator class which fetches the additional order details that are not present in REC session.
 * 
 */
public class GuestOrderDetailsPageEvaluator extends SimplePageEvaluator {
  public static final String ORDER_LOOKUP_PATH = "/orderhistory/orderLookup.jsp";
  public static final String FIFTY_ONE = "FIFTYONE";
  public static final String PHONE_ORDER_PATTERN = "\\d+";
  
  @Override
  protected PageModel allocatePageModel() {
    return new GuestOrderDetailsPageModel();
    
  }
  
  @Override
  public boolean evaluate(final PageDefinition pageDefinition, final PageContext pageContext) throws Exception {
    if (!super.evaluate(pageDefinition, pageContext)) {
      return false;
    }
    final NameContext nameContext = getRequest().getRequestScope();
    getRequest().setParameter("isRegForm", nameContext.getElement("isRegForm"));
    final GuestOrderDetailsPageModel pageModel = (GuestOrderDetailsPageModel) getPageModel();
    final ReturnsCreditSession returnsCreditSession = CheckoutComponents.getReturnsCreditSession(getRequest());
    final ReturnsCreditCmosOrderVO returnsCreditCmosOrderVO = returnsCreditSession.getReturnsCreditCmosOrderVO();
    if (null != returnsCreditCmosOrderVO) {
      returnsCreditSession.setReturnsCreditCmosOrderVO(returnsCreditCmosOrderVO);
      if (returnsCreditCmosOrderVO.isOrderReturnsEligible()) {
        returnsCreditSession.setSessionPageToken(ReturnsCreditConstants.RETURNS_CREDIT_SELECT_ITEMS_BODY_CLASS);
      }
      final String orderNumber = returnsCreditCmosOrderVO.getWebOrCmosOrderNumber();
      final HttpSession session = getRequest().getSession();
      final String userEnteredOrderNum = (String) session.getAttribute(ReturnsCreditConstants.ORDER_NUMBER);
      final String userEnteredEmail = (String) session.getAttribute(ReturnsCreditConstants.EMAIL);
      final String userEnteredPhone = (String) session.getAttribute(ReturnsCreditConstants.PHONE);
      getRequest().setParameter(ReturnsCreditConstants.ORDER_NUMBER, userEnteredOrderNum);
      getRequest().setParameter(ReturnsCreditConstants.EMAIL, userEnteredEmail);
      getRequest().setParameter(ReturnsCreditConstants.PHONE, userEnteredPhone);
      pageModel.setVendorId(returnsCreditCmosOrderVO.getVendorId());
      final boolean isPhoneOrder = isPhoneOrder(userEnteredOrderNum);
      pageModel.setPhoneOrder(isPhoneOrder);
      populateOrderPrice(orderNumber, pageModel, returnsCreditCmosOrderVO, isPhoneOrder);
      if (isPhoneOrder) {
        pageModel.setHideRegistration(true);
      }
      
    } else {// if REC session does not have order details redirect the user to order lookup page
      getResponse().sendLocalRedirect(ORDER_LOOKUP_PATH, getRequest());
    }
    pageModel.setPageName(GUEST_ORDER_DETAILS);
    pageModel.setPageType(ACCOUNTS);
    final TMSMessageContainer tmsMessageContainer = CheckoutComponents.getTMSMessageContainer(getRequest());
    final TMSDataDictionaryUtils dataDictionaryUtils = CommonComponentHelper.getDataDictionaryUtils();
    dataDictionaryUtils.processTMSDataDictionaryAttributes(pageDefinition, null, getProfile(), pageModel, tmsMessageContainer);
    
    return true;
  }
  
  /**
   * This order identifies whether the order is placed online or through phone
   * 
   * @param orderNumber
   * @return
   */
  private boolean isPhoneOrder(final String orderNumber) {
    boolean isPhoneOrder = false;
    if (null != orderNumber) {
      final Pattern pattern = Pattern.compile(PHONE_ORDER_PATTERN);
      if (pattern.matcher(orderNumber).matches()) {
        isPhoneOrder = true;
      }
    }
    
    return isPhoneOrder;
  }
  
  /**
   * This method fetches the order total,taxes and shipping amount
   * 
   * @param orderNum
   * @param pageModel
   * @throws RepositoryException
   */
  @SuppressWarnings({"rawtypes" , "rawtypes"})
  private void populateOrderPrice(final String orderNum, final GuestOrderDetailsPageModel pageModel, final ReturnsCreditCmosOrderVO orderVO, final boolean isPhoneOrder) throws RepositoryException {
    
    if (!StringUtils.isBlank(orderNum)) {
      final String orderRepositoryId = orderNum.substring(2);
      try {
        NMOrderImpl order = null;
        try {
          order = (NMOrderImpl) NMOrderManager.getOrderManager().loadOrder(orderRepositoryId);
        } catch (final Exception exception) {
          if (log.isLoggingError()) {
            log.logError(exception);
          }
        }
        if (order != null) {
          final NMCommerceProfileTools profileTools = CheckoutComponents.getCommerceProfileTools();
          final RepositoryItem profile = profileTools.getProfileForOrder(orderRepositoryId);
          final NMProfile orderProfile = profileTools.getNMProfile(profile.getRepositoryId());
          if (orderProfile.isRegisteredProfile()) {
            pageModel.setHideRegistration(true);
          }
          
          final String vendorId = pageModel.getVendorId();
          if (FIFTY_ONE.equals(vendorId)) {
            
            populateInternationalOrderDetails(orderNum, orderVO, orderProfile, order, pageModel);
          } else {
            final NMOrderPriceInfo priceInfo = (NMOrderPriceInfo) order.getPriceInfo();;
            priceInfo.getShipping();
            priceInfo.getAmount();
            pageModel.setTax(priceInfo.getTax());
            pageModel.setTotal(priceInfo.getTotal());
            pageModel.setShipping(priceInfo.getShipping());
            pageModel.setAmount(priceInfo.getAmount());
          }
          
        } else if (isPhoneOrder) {
          populatePriceForPhoneOrder(pageModel);
        }
        final Map<String, OrderLineItemVO> itemDetails = pageModel.getItemDetails();
        OrderLineItemVO itemVO = null;
        final ReturnsCreditConfig returnsCreditConfig = CommonComponentHelper.getReturnsCreditConfig();
        final List<String> validItemCancelCodes = returnsCreditConfig.getValidItemCancelCodes();
        String currentStatus = null;
        String status = null;
        final List<ReturnsCreditLineItemVO> lineItems = orderVO.getGuestOrderLineItems();
        for (final ReturnsCreditLineItemVO lineItem : lineItems) {
          currentStatus = lineItem.getCurrentStatus();
          final boolean cancancelItem = validItemCancelCodes.contains(currentStatus);
          status = getLineItemStatusDescription(currentStatus, lineItem.getCancelCode(), returnsCreditConfig);
          itemVO = new OrderLineItemVO();
          itemVO.setItemStatus(status);
          itemVO.setCanCancelItem(cancancelItem);
          itemDetails.put(lineItem.getOmsLineItemId(), itemVO);
        }
        
      } catch (final Exception e) {
        if (log.isLoggingError()) {
          log.logError(e);
        }
      }
    }
  }
  
  /**
   * This page populates the order details of international order from database
   * 
   * @param orderId
   * @param order
   * @param profile
   * @param nmOrder
   * @param pageModel
   */
  @SuppressWarnings("unchecked")
  public void populateInternationalOrderDetails(final String orderId, final ReturnsCreditCmosOrderVO returnsCreditCmosOrderVO, final NMProfile profile, final NMOrderImpl nmOrder,
          final GuestOrderDetailsPageModel pageModel) {
    final String countryCode = (String) nmOrder.getPropertyValue(OrderRepositoryConstants.COUNTRY_CODE);
    final List<ReturnsCreditLineItemVO> orderLineItems = returnsCreditCmosOrderVO.getGuestOrderLineItems();
    double totalIntParentheticalAmount = 0.0;
    String intlParentheticalAmount = "";
    double pricePerLineItem = 0.0;
    long quantity = 0;
    final LocalizationUtils localizationUtils = CommonComponentHelper.getLocalizationUtils();
    final String intlCurrencyCode = (String) nmOrder.getPropertyValue(OrderRepositoryConstants.INTERNATIONAL_CURRENCY_CODE);
    try {
      final Iterator<ReturnsCreditLineItemVO> lineItemDetailIterator = orderLineItems.iterator();
      while (lineItemDetailIterator.hasNext()) {
        final ReturnsCreditLineItemVO orderLine = lineItemDetailIterator.next();
        
        final List<NMCommerceItem> citems = nmOrder.getCommerceItems();
        for (final NMCommerceItem nmCommerceItem : citems) {
          if (nmCommerceItem.getCmosSKUId().equalsIgnoreCase(orderLine.getOmsSkuId())) {
            pricePerLineItem = (Double) nmCommerceItem.getInternationalPriceItem().getPropertyValue(OrderRepositoryConstants.LIST_PRICE);
            quantity = nmCommerceItem.getQuantity();
            if (nmCommerceItem.getProduct().getIntParentheticalCharge() != null) {
              intlParentheticalAmount = localizationUtils.getConvertedCurrency(countryCode, intlCurrencyCode, nmCommerceItem.getProduct().getIntParentheticalCharge());
              totalIntParentheticalAmount += Double.parseDouble(intlParentheticalAmount) * nmCommerceItem.getQuantity();
            }
          }
        }
      }
      
    } catch (final Exception e) {
      // This catch block is added to catch exceptions that occur while getting parenthetical charges sothat other prices are populated
      if (log.isLoggingError()) {
        log.logError(e);
      }
    }
    
    final RepositoryItem item = nmOrder.getInternationalPriceItem();
    pageModel.setFishAndWildLifeFee((Double) item.getPropertyValue(OrderRepositoryConstants.FISH_WILD_LIFEFEE));
    final double shipping = (Double) item.getPropertyValue(OrderRepositoryConstants.EXTRA_SHIPPING);
    final double handling = (Double) item.getPropertyValue(OrderRepositoryConstants.EXTRA_HANDLING);
    final double deliveryCharges = shipping + handling;
    final double discount = (Double) item.getPropertyValue(OrderRepositoryConstants.DISCOUNT);
    final double listPrice = (Double) item.getPropertyValue(OrderRepositoryConstants.LIST_PRICE);
    final double salePrice = (Double) item.getPropertyValue(OrderRepositoryConstants.SALE_PRICE);
    double total = listPrice;
    if (discount > 0.0) {
      total = salePrice;
    }
    pageModel.setOversizeOverweightPrice(totalIntParentheticalAmount);
    pageModel.setDeliveryAndProcessing(deliveryCharges);
    pageModel.setDutyOption(nmOrder.getDutiesPaymentMethod());
    pageModel.setTotalDuties((Double) item.getPropertyValue(OrderRepositoryConstants.DUTIES));
    pageModel.setCurrencyCode(intlCurrencyCode);
    pageModel.setTax((Double) item.getPropertyValue(OrderRepositoryConstants.TAXES));
    pageModel.setAmount(total);
    pageModel.setTotal((Double) item.getPropertyValue(OrderRepositoryConstants.TOTAL_PRICE));
    
  }
  
  /**
   * This method returns the line item status
   * 
   * @param item
   * @param returnsCreditConfig
   * @return
   */
  private String getLineItemStatusDescription(final String currentStatus, final String cancelCode, final ReturnsCreditConfig returnsCreditConfig) {
    String status = "";
    if ("CX".equals(currentStatus) && (null != cancelCode)) {
      if ("CX63".equals(cancelCode) || "CX65".equals(cancelCode) || "CXI".equals(cancelCode) || "CX64".equals(cancelCode)) {
        status = "Fulfilled from store";
        return status;
      }
    }
    final Map<String, String> orderStatusCodes = returnsCreditConfig.getOrderStatusCodes();
    if (null != orderStatusCodes) {
      status = orderStatusCodes.get(currentStatus);
    }
    
    return status;
  }
  
  private void populatePriceForPhoneOrder(final GuestOrderDetailsPageModel pageModel) {
    final ReturnsCreditSession returnsCreditSession = CheckoutComponents.getReturnsCreditSession(getRequest());
    
    final ReturnsCreditCmosOrderVO returnsCreditCmosOrderVO = returnsCreditSession.getReturnsCreditCmosOrderVO();
    final List<ReturnsCreditLineItemVO> cmosOrderLineItems = returnsCreditCmosOrderVO.getGuestOrderLineItems();
    double lineItemsTotal = 0.0;
    double totalTax = 0.0;
    double totalShipping = 0.0;
    int qty = 0;
    double itemPrice = 0.0;
    double itemTax = 0.0;
    double itemShippingCahrge = 0.0;
    String taxEachString = null;
    String shippingEach = null;
    for (final ReturnsCreditLineItemVO lineItem : cmosOrderLineItems) {
      
      try {
        qty = Integer.parseInt(lineItem.getQuantity());
        itemPrice = Double.parseDouble(lineItem.getPriceEach());
        taxEachString = lineItem.getTaxEach();
        shippingEach = lineItem.getShippingEach();
        if (!StringUtils.isBlank(taxEachString)) {
          itemTax = Double.parseDouble(taxEachString);
        }
        if (!StringUtils.isBlank(shippingEach)) {
          itemShippingCahrge = Double.parseDouble(shippingEach);
        }
        lineItemsTotal += qty * itemPrice;
        totalTax += itemTax;
        totalShipping += itemShippingCahrge;
      } catch (final NumberFormatException exception) {
        if (log.isLoggingError()) {
          log.logError(exception);
        }
      }
      
    }
    final double total = lineItemsTotal + totalTax + totalShipping;
    pageModel.setAmount(lineItemsTotal);
    pageModel.setTotal(total);
    pageModel.setTax(totalTax);
    pageModel.setShipping(totalShipping);
  }
}
