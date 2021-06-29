package com.nm.commerce.pagedef.evaluator.checkout;

import static com.nm.common.INMGenericConstants.SUCCESS;
import static com.nm.edo.constants.EmployeeDiscountConstants.FACILITY_NUMBER;
import static com.nm.integration.MasterPassConstants.MASTERPASS_STATUS;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.PageContext;

import org.jsoup.helper.StringUtil;

import atg.commerce.CommerceException;
import atg.commerce.order.HardgoodShippingGroup;
import atg.core.util.StringUtils;
import atg.droplet.DropletException;
import atg.nucleus.Nucleus;
import atg.servlet.DynamoHttpServletRequest;
import atg.servlet.DynamoHttpServletResponse;
import atg.servlet.ServletUtil;
import atg.userprofiling.NMCookieManager;

import com.nm.abtest.AbTestHelper;
import com.nm.ajax.checkout.utils.ComponentUtils;
import com.nm.collections.NMPromotion;
import com.nm.commerce.NMCommerceItem;
import com.nm.commerce.NMCreditCard;
import com.nm.commerce.NMOrderHolder;
import com.nm.commerce.NMProfile;
import com.nm.commerce.checkout.CheckoutAPI;
import com.nm.commerce.checkout.CheckoutComponents;
import com.nm.commerce.checkout.beans.PromoBean;
import com.nm.commerce.checkout.beans.ResultBean;
import com.nm.commerce.order.NMOrderManager;
import com.nm.commerce.pagedef.definition.PageDefinition;
import com.nm.commerce.pagedef.evaluator.SimplePageEvaluator;
import com.nm.commerce.pagedef.model.CheckoutPageModel;
import com.nm.commerce.pagedef.model.PageModel;
import com.nm.commerce.pagedef.model.UIElementConstants;
import com.nm.commerce.promotion.NMOrderImpl;
import com.nm.common.AlipayRestrBean;
import com.nm.common.NMBorderFreeGetQuoteResponse;
import com.nm.common.NMBorderFreePayPalResponse;
import com.nm.components.CommonComponentHelper;
import com.nm.edo.EmployeeDiscountsConfig;
import com.nm.edo.constants.EmployeeDiscountConstants;
import com.nm.edo.exception.EmployeeDiscountsException;
import com.nm.edo.util.EmployeeDiscountsUtil;
import com.nm.edo.vo.EmployeeAuthenticationDetailsVO;
import com.nm.formhandler.ShoppingCartHandler;
import com.nm.integration.VMeInfoVO;
import com.nm.integration.VmeException;
import com.nm.integration.util.paypal.PayPalException;
import com.nm.integration.util.paypal.PayPalTokenExpiredException;
import com.nm.international.fiftyone.checkoutapi.BorderFreeConstants;
import com.nm.international.fiftyone.checkoutapi.ESBErrorMessages;
import com.nm.international.fiftyone.checkoutapi.vo.AltBuyerResponseVO;
import com.nm.masterpass.integration.MasterPassCheckoutResponseVO;
import com.nm.masterpass.integration.MasterPassException;
import com.nm.masterpass.integration.MasterPassInfoVO;
import com.nm.returnseligibility.util.ReturnsEligibilityUtil;
import com.nm.sitemessages.Message;
import com.nm.sitemessages.MessageContainer;
import com.nm.utils.BrandSpecs;
import com.nm.utils.LocalizationUtils;
import com.nm.utils.PromotionsHelper;
import com.nm.utils.StringUtilities;
import com.nm.utils.UrlUtils;
import com.nm.utils.dynamicCode.DynamicCodeUtils;
import com.nm.utils.fiftyone.FiftyOneUtils;

public class BaseCheckoutPageEvaluator extends SimplePageEvaluator {
  
  private static final LocalizationUtils localizationUtils = (LocalizationUtils) Nucleus.getGlobalNucleus().resolveName("/nm/utils/LocalizationUtils");
  private static final FiftyOneUtils fiftyOneUtils = (FiftyOneUtils) Nucleus.getGlobalNucleus().resolveName("nm/utils/fiftyone/FiftyOneUtils");
  private static final ESBErrorMessages esbErrorMessages = (ESBErrorMessages) Nucleus.getGlobalNucleus().resolveName("/nm/international/fiftyone/checkoutapi/ESBErrorMessages");
  private static String CHINA_UNIONPAY_CARD_TYPE = "China UnionPay";
  private static String PROD_RESTR_QUERY_PARAM = "?prodRestr=";
  private static String MASTER_PASS_QUERY_PARAM = "?mpError=true";
  private static String COUNTRY_RESTR_QUERY_PARAM = "?countryRestr=";
  private static String EXPRESS_PAYPAL = "isExpressPayPal";
  private static String TRUE = "true";
  private final EmployeeDiscountsUtil employeeDiscountsUtil = CommonComponentHelper.getEmployeeDiscountsUtil();
  private final EmployeeDiscountsConfig employeeDiscountsConfig = CommonComponentHelper.getEmployeeDiscountsConfig();
  public static final String SMART_POST_ABTEST_GROUP = "smartPostABTestGrp";
  
  @Override
  protected PageModel allocatePageModel() {
    return new CheckoutPageModel();
  }
  
  @Override
  protected PageModel getPageModel() {
    final PageModel model = super.getPageModel();
    return model;
  }
  
  @SuppressWarnings("unchecked")
  @Override
  public boolean evaluate(final PageDefinition pageDefinition, final PageContext pageContext) throws Exception {
    if (!super.evaluate(pageDefinition, pageContext)) {
      return false;
    }
    getResponse().setHeader("Cache-Control", "no-cache, max-age=0, must-revalidate, no-store");
    final CheckoutPageModel pageModel = (CheckoutPageModel) getPageModel();
    final ShoppingCartHandler cart = CheckoutComponents.getCartHandler(getRequest());
    final NMOrderImpl order = getCurrentOrder(cart);
    final NMOrderHolder orderHolder = CheckoutComponents.getOrderHolder(getRequest());
    final NMProfile profile = CheckoutComponents.getProfile(getRequest());
    final String isExpressPayPal = getRequest().getParameter(EXPRESS_PAYPAL);
    final VMeInfoVO vMeInfoVO = CheckoutComponents.getVMeInfoVO(getRequest());
    final MasterPassInfoVO masterPassInfoVO = CheckoutComponents.getMasterPassInfoVO(getRequest());
    /* This value determines if the additional charges (taxation, shipping, etc) show up on the cart. It will be set in the CartPageEvaluator by an AB Test */
    pageModel.setShowEstimatedOrderTotal(true);
    
    // Retrieving the refreshable content for charge for returns or free returns.
    final ReturnsEligibilityUtil returnsEligibilityUtil = CommonComponentHelper.getReturnsEligibilityUtil();
    returnsEligibilityUtil.setReturnsChargeRefreshableContent(pageModel);
    
    // BOPS specific checks
    final boolean bopsEnabled = CheckoutAPI.isBopsEnabled();
    pageModel.setBopsEnabled(bopsEnabled);
    
    // Alipay changes to reset session component attribute
    final AlipayRestrBean apRestr = CheckoutComponents.getAlipayRestrBean(getRequest());
    apRestr.setAlipayRestrItemList(null);
    
    // Smart Post Changes
    final String abTestGroup = CheckoutComponents.getServiceLevelArray().getServiceLevelABTestGroup();
    if (CommonComponentHelper.getSystemSpecs().isSmartPostEnabled() && abTestGroup.equalsIgnoreCase(UIElementConstants.AB_TEST_SERVICE_LEVEL_GROUPS)) {
      // SmartPost : Retrieving the value of "smartPostABTestGrp" attribute created in CSR if Smart Post flag is enabled
      final String smartPostABTestGrp = AbTestHelper.getAbTestValue(getRequest(), SMART_POST_ABTEST_GROUP);
      // SmartPost Changes: Set Pagemodel attribute for SmartPost based on the AB Test group
      if (smartPostABTestGrp != null) {
        if (smartPostABTestGrp.equalsIgnoreCase(GROUP_A)) {
          pageModel.getElementDisplaySwitchMap().put(UIElementConstants.SERVICE_LEVEL_ABTEST_GROUP_1, Boolean.TRUE);
        } else if (smartPostABTestGrp.equalsIgnoreCase(GROUP_B)) {
          pageModel.getElementDisplaySwitchMap().put(UIElementConstants.SERVICE_LEVEL_ABTEST_GROUP_2, Boolean.TRUE);
        }
      }
    } else {
      // no else required
    }
    
    String productSystemCode = ComponentUtils.getInstance().getSystemSpecs().getProductionSystemCode();
    productSystemCode = productSystemCode != null ? productSystemCode : "";
    final String orderId = order == null ? "" : order.getId();
    final String orderNumber = productSystemCode + orderId;
    pageModel.setBrandOrderId(orderNumber);
    
    if (order != null) {
      pageModel.setItemCount(order.getTotalNumberOfItems());
      // setting collectShipping to true if multiple shipping address is being selected
      pageModel.setCollectShipping((CheckoutAPI.getNonWishlistShippingGroupCount(order.getShippingGroups()) == 1) && !order.hasAllBopsItems());
      /*
       * Following code will call getQuoteAPI.
       */
      
      /* changes for setting showPinYin,showCpf and showCounty variables as true or false based on the countries start */
      final String preferredCountry = profile.getCountryPreference();
      if (localizationUtils.getAllowPinYinCharactersForCountries().contains(preferredCountry)) {
        pageModel.setOnlyPinYin(true);
      } else {
        pageModel.setOnlyPinYin(false);
      }
      
      if (localizationUtils.getShowCPFFieldForCountries().contains(preferredCountry)) {
        pageModel.setShowCpf(true);
      } else {
        pageModel.setShowCpf(false);
      }
      
      if (localizationUtils.getShowCountyFieldForCountries().contains(preferredCountry)) {
        pageModel.setShowCounty(true);
      } else {
        pageModel.setShowCounty(false);
      }
      final boolean isInternationCheckout = CheckoutAPI.isInternationalSession(profile);
      if (isInternationCheckout) {
        pageModel.setZipcodeMandatory(localizationUtils.isZipcodeMandatoryForCountry(preferredCountry));
      }
      boolean isCardSupportedForCountry = false;
      String defaultCreditCardType = "";
      if ((null != profile.getDefaultCreditCard()) && (null != profile.getDefaultCreditCard().getCreditCardType())) {
        defaultCreditCardType = profile.getDefaultCreditCard().getCreditCardType();
      }
      if (!isInternationCheckout) {
        isCardSupportedForCountry = CheckoutAPI.validateDomesticSupportedCard(defaultCreditCardType);
        pageModel.setCardSupportedFlag(isCardSupportedForCountry);
        
        // vme changes : setting vme card flag as true if vme is selected
        final List<NMCreditCard> orderCreditCards = order.getCreditCards();
        if ((orderCreditCards != null) && !orderCreditCards.isEmpty()) {
          final NMCreditCard paymentGroup = orderCreditCards.get(0);
          
          if (paymentGroup.isVmeCard() && (null != vMeInfoVO.getPaymentDetails())) {
            pageModel.setVmeCard(true);
          } else if (paymentGroup.isMasterPassCard() && (null != masterPassInfoVO.getMasterPassCheckoutResponseVO())) {
            // MasterPass changes : setting MasterPass card flag as true if MasterPass is selected
            pageModel.setMasterPassCard(true);
          }
        }
        if (ComponentUtils.getInstance().getSystemSpecs().isvMeEnabled()) {
          // set rootUrl with the current request's scheme,host and port
          pageModel.setRootUrl(UrlUtils.getSchemeHostPort(getRequest()));
          pageModel.setActivePromoCodes(getActivePromoCodesForVme(order));
        }
        // vme changes : ends
      }
      /* changes for setting showPinYin variable and showCpf variable as true or false ends */
      /** ExpPaypal_Flow Start */
      if (isInternationCheckout && (order.getCommerceItemCount() > 0)) {
        final NMBorderFreePayPalResponse paypalResponse = CheckoutComponents.getNMBorderFreePayPalResponse(getRequest());
        if (getPageModel().getPageDefinition().getId().equalsIgnoreCase("OrderComplete") || getPageModel().getPageDefinition().getId().equalsIgnoreCase("PrintOrder")) {
          final NMOrderHolder oHolder = (NMOrderHolder) cart.getOrderHolder();
          final NMOrderImpl lastOrder = (NMOrderImpl) oHolder.getLast();
          if (lastOrder.isExpressPaypal()) {
            order.setExpressPaypal(true);
          }
        } else if (!StringUtils.isEmpty(isExpressPayPal) && isExpressPayPal.equalsIgnoreCase(TRUE)) {
          order.setExpressPaypal(true);
          
          if (paypalResponse != null) {
            final String payerId = getRequest().getParameter("PayerID");
            final String confirmationToken = getRequest().getParameter("token");
            paypalResponse.setPayerId(payerId);
            paypalResponse.setPaymentConfirmationToken(confirmationToken);
          }
        } else {
          order.setExpressPaypal(false);
        }
      } else {
        order.setExpressPaypal(false);
      }
      /** ExpPaypal_Flow End */
      
      pageModel.setOrderEligibleForFreeDeliveryProcessing(order.isEligibleForFreeDeliveryProcessing());
      if (CheckoutComponents.getConfig().isLoggingDebug()) {
        CheckoutComponents.getConfig().logDebug("BaseCheckoutPageEvaluator|Is order eligble for free processing : " + pageModel.isOrderEligibleForFreeDeliveryProcessing());
        CheckoutComponents.getConfig().logDebug("BaseCheckoutPageEvaluator|Returns Banner : " + pageModel.getReturnsChargeBannerRefreshableContent());
        CheckoutComponents.getConfig().logDebug("BaseCheckoutPageEvaluator|Returns Message : " + pageModel.getReturnsChargeMessageRefreshableContent());
      }
      final List<NMCommerceItem> commerceItemsList = order.getNmCommerceItems();
      /* call silentAuthentication on orderReview page and change employee login status */
      final boolean isOrderReviewPage = getPageModel().getPageDefinition().getId().equalsIgnoreCase("OrderReview");
      order.setEmployeeLoginStatus(NMOrderImpl.EMPLOYEE_LOGIN_STATUS_NONE);
      final NMOrderManager orderManager = (NMOrderManager) orderHolder.getOrderManager();
      if (!isInternationCheckout && employeeDiscountsUtil.isAssociateLoggedIn(profile)) {
        processEmployeeCheckout(pageModel, order, profile, orderManager, isOrderReviewPage);
      }
      
      if (isInternationCheckout && (order.getCommerceItemCount() > 0)) {
        final List<NMCommerceItem> nMcommerceItemsList = new ArrayList<NMCommerceItem>();
        nMcommerceItemsList.addAll(commerceItemsList);
        if ((null != nMcommerceItemsList) && (nMcommerceItemsList.size() > 0)) {
          for (final NMCommerceItem commerceItem : nMcommerceItemsList) {
            if (commerceItem.getProduct().getIsHideInternationally()) {
              final Boolean keepProductInCart =
                      localizationUtils.checkShoppingBagRestrictions(commerceItem.getId(), commerceItem.getProductId(), commerceItem.getSelectedInterval(), commerceItem.getCatalogRefId(),
                              commerceItem.getProduct().getWebProductDisplayName());
              if (!keepProductInCart) {
                try {
                  CheckoutAPI.removeItemFromOrder(cart, commerceItem.getId());
                } catch (final Exception ex) {
                  CommonComponentHelper.getLogger().logError("Unable to remove item from cart. Item :" + commerceItem.getId() + ". Error : " + ex.getMessage());
                }
                if (!getPageModel().getPageDefinition().getId().equalsIgnoreCase("Cart")) {
                  getResponse().sendRedirect(CART_PATH);
                }
              }
            }
          }
        }
      }
      
      if (!getPageModel().getPageDefinition().getId().equalsIgnoreCase("Cart")) {
        
        if (isInternationCheckout && (order.getCommerceItemCount() > 0)) {
          boolean isOrderConfirmed = false;
          boolean isChinaUnionPay = false;
          boolean isConvertRussiaCurrencyToUSD = false;
          final String chinaUnionPay = getRequest().getParameter("CUP");
          if (!StringUtils.isBlank(chinaUnionPay) && chinaUnionPay.equalsIgnoreCase("true")) {
            isChinaUnionPay = true;
          }
          if (isChinaUnionPay && isOrderReviewPage) {
            order.setChinaUnionPayPaymentType(true);
          } else {
            order.setChinaUnionPayPaymentType(false);
          }
          if (!order.isChinaUnionPayPaymentType()) {
            final boolean orderContainsChinaUnionPay = doesOrderContainChinaUnionPayCard(order);
            if (getPageModel().getPageDefinition().getId().equalsIgnoreCase("OrderComplete") || getPageModel().getPageDefinition().getId().equalsIgnoreCase("PrintOrder")) {
              isOrderConfirmed = true;
            }
            if (orderContainsChinaUnionPay && isOrderConfirmed) {
              order.setChinaUnionPayPaymentType(true);
            }
          }
          boolean isSelectedCardChinaUnionPay = false;
          
          if ((null != orderHolder.getTemporaryPayment()) && !StringUtils.isBlank(orderHolder.getTemporaryPayment().getCardType())) {
            final String cardTypeSelected = orderHolder.getTemporaryPayment().getCardType();
            if (cardTypeSelected.equalsIgnoreCase("CUP")) {
              isSelectedCardChinaUnionPay = true;
            }
          } else if (defaultCreditCardType.equalsIgnoreCase(CHINA_UNIONPAY_CARD_TYPE)) {
            isSelectedCardChinaUnionPay = true;
          }
          pageModel.setSelectedCardChinaUnionPay(isSelectedCardChinaUnionPay);
          
          // The parameter RUTOUSD is set in the orderReview.js. When a customer has a country preference of RU and
          // they are on the orderReview page, a pop event is activated prompting the customer that their current currency
          // will change to USD. After the customer accepts the prompt, a cookie variable is set to "firstTime".
          final String convertRussiaCurrency = getRequest().getParameter("RUTOUSD");
          CommonComponentHelper.getCookieManager();
          final String sessionVar = NMCookieManager.getCookieValue(getRequest().getCookies(), "sessionVar");
          if (sessionVar != null) {
            if (sessionVar.equals("firstTime")) {
              isConvertRussiaCurrencyToUSD = true;
              order.setRussiaCurrencyToUSD(true);
            }
          }
          
          if (!StringUtils.isBlank(convertRussiaCurrency) && convertRussiaCurrency.equalsIgnoreCase("true")) {
            isConvertRussiaCurrencyToUSD = true;
            order.setRussiaCurrencyToUSD(true);
          }
          if (isConvertRussiaCurrencyToUSD && isOrderReviewPage) {
            order.setRussiaCurrencyToUSD(true);
          } else {
            order.setRussiaCurrencyToUSD(false);
          }
          if (profile.getCountryPreference().equals("RU") && getPageModel().getPageDefinition().getId().equalsIgnoreCase("OrderComplete")) {
            isConvertRussiaCurrencyToUSD = true;
            order.setRussiaCurrencyToUSD(true);
          }
          
          // Make sure we have only one address in the order for international checkout.
          CheckoutAPI.cleanupShippingGroupsForInternationalCheckout(order, cart.getOrderManager());
          final boolean isPPX = isInternationCheckout && !StringUtils.isEmpty(isExpressPayPal) && isExpressPayPal.equalsIgnoreCase(TRUE);
          // INT-1900 changes starts here
          /* ExpPaypal_Flow Start */
          AltBuyerResponseVO altBuyerResponseVO = null;
          final NMBorderFreePayPalResponse nmBorderFreePayPalResponse = CheckoutComponents.getNMBorderFreePayPalResponse(getRequest());
          if (isPPX) {
            altBuyerResponseVO = fiftyOneUtils.getInternationalCheckoutAPIUtil().invokeAltBuyerDetails(profile, order);
            nmBorderFreePayPalResponse.setAltBuyerResponseVO(altBuyerResponseVO);
            if (CheckoutAPI.isInternationalSession(profile) && (null != nmBorderFreePayPalResponse.getAltBuyerResponseVO())
                    && (null == nmBorderFreePayPalResponse.getAltBuyerResponseVO().getExpPayPalErrMsg()) && StringUtils.isBlank(nmBorderFreePayPalResponse.getAltBuyerResponseVO().getErrorMsg())
                    && (nmBorderFreePayPalResponse.getPalPalResponseVO() != null) && (nmBorderFreePayPalResponse.getPalPalResponseVO().getAltPaymentUrl() != null)) {
              CheckoutAPI.setExpPaypalInfoToOrder(cart, profile, nmBorderFreePayPalResponse);
            } else if ((null != nmBorderFreePayPalResponse.getAltBuyerResponseVO())
                    && ((null != nmBorderFreePayPalResponse.getAltBuyerResponseVO().getExpPayPalErrMsg()) || !StringUtils.isBlank(nmBorderFreePayPalResponse.getAltBuyerResponseVO().getErrorMsg()))) {
              getResponse().sendRedirect(CART_PATH);
              return false;
            }
            pageModel.setExpressPayPal(true);
          }
          /* ExpPaypal_Flow End */
          NMBorderFreeGetQuoteResponse nmBorderFreeGetQuoteResponse = null;
          String paypalShipToCountry = null;
          if ((null != nmBorderFreePayPalResponse.getAltBuyerResponseVO()) && order.isExpressPaypal() && (null == nmBorderFreePayPalResponse.getAltBuyerResponseVO().getExpPayPalErrMsg())
                  && StringUtils.isBlank(nmBorderFreePayPalResponse.getAltBuyerResponseVO().getErrorMsg())) {
            paypalShipToCountry = nmBorderFreePayPalResponse.getAltBuyerResponseVO().getShipToCountryCode();
            if ((nmBorderFreePayPalResponse.getAltBuyerResponseVO() != null) && !StringUtils.isBlank(paypalShipToCountry) && !StringUtils.isBlank(profile.getCountryPreference())) {
              // Profile country preference
              final String prevCountryPref = profile.getCountryPreference();
              boolean hasProdRestr = false;
              boolean hasCountryRestr = false;
              if (localizationUtils.isSupportedByFiftyOne(paypalShipToCountry) && StringUtilities.areNotEqual(paypalShipToCountry, profile.getCountryPreference())) {
                // Temporarily set Profil'e country Preference to Express paypal's Ship to country in order to check restrictions applicable for that country
                profile.setCountryPreference(paypalShipToCountry);
                
                final List<String> commerceItemsTobeRemoved = new ArrayList<String>();
                List<NMCommerceItem> restrictedItems = new ArrayList<NMCommerceItem>();
                // check for NM restrictions
                if ((null != commerceItemsList) && (commerceItemsList.size() > 0)) {
                  for (final NMCommerceItem commerceItem : commerceItemsList) {
                    final Boolean keepProductInCart =
                            localizationUtils.checkShoppingBagRestrictions(commerceItem.getId(), commerceItem.getProductId(), commerceItem.getSelectedInterval(), commerceItem.getCatalogRefId(),
                                    commerceItem.getProduct().getWebProductDisplayName());
                    if (!keepProductInCart) {
                      commerceItemsTobeRemoved.add(commerceItem.getId());
                    }
                  }
                }
                
                // check for BorderFree restrictions for the Express paypal's Ship to country which is temporarily set in the profile
                if (commerceItemsTobeRemoved.isEmpty()) {
                  nmBorderFreeGetQuoteResponse = fiftyOneUtils.getInternationalCheckoutAPIUtil().callgetQuote(profile, order, null, true, null);
                  if (!StringUtil.isBlank(nmBorderFreeGetQuoteResponse.getErrorMessage()) && !nmBorderFreeGetQuoteResponse.isIgnorableWarning()) {
                    restrictedItems = nmBorderFreeGetQuoteResponse.getRestrictedItemList();
                  }
                }
                // reset profile's country preference
                profile.setCountryPreference(prevCountryPref);
                // redirect to Shopping Bag in case of Product restrictions with relevant message
                if ((!commerceItemsTobeRemoved.isEmpty() && (commerceItemsTobeRemoved.size() > 0)) || ((restrictedItems != null) && !restrictedItems.isEmpty() && (restrictedItems.size() > 0))) {
                  hasProdRestr = true;
                  getResponse().sendRedirect(CART_PATH + PROD_RESTR_QUERY_PARAM + hasProdRestr);
                } else {
                  // clearing the shipping carrier type and setting duty method to prepaid when express paypal is chosen
                  final List<HardgoodShippingGroup> shippingGroups = order.getShippingGroups();
                  for (final HardgoodShippingGroup sg : shippingGroups) {
                    final boolean notMatchingShipTo = order.isExpressPaypal() && !paypalShipToCountry.equalsIgnoreCase(profile.getCountryPreference());
                    if (notMatchingShipTo) {
                      order.setPropertyValue("shippingCarrierType", "");
                      order.setDutiesPaymentMethod(BorderFreeConstants.PREPAID);
                    }
                  }
                  
                  final boolean locallyPricedOrderBeforeCountryChange = order.isHasLocallyPricedItem();
                  // If no Product restrictions then the CLC country changes to Paypal's Ship-to-country where as currency points to previous selection
                  profile.setCountryPreference(paypalShipToCountry);
                  final boolean locallyPricedOrderAfterCountryChange = order.isHasLocallyPricedItem();
                  getRequest().setAttribute("omnitureEventForExpPayPal", true);
                  String queryString = getRequest().getQueryString();
                  if (locallyPricedOrderAfterCountryChange || (locallyPricedOrderBeforeCountryChange && !locallyPricedOrderAfterCountryChange)) {
                    queryString = queryString + "&payPalLocalizedPriceError=true";
                  }
                  // because the user is redirected to the order review page all state is lost, this is why for specific localized price functionality had to be saved to the URL
                  getResponse().sendRedirect(CheckoutComponents.getOrderReviewPageDefinition().getBreadcrumbUrl() + "?" + queryString);
                }
              } else if (StringUtilities.areEqual(paypalShipToCountry, profile.getCountryPreference())) {}// INT-1901 : redirect to Cart page if Paypal's Ship to country is not supported by BorderFree
              else {
                hasCountryRestr = true;
                getResponse().sendRedirect(CART_PATH + COUNTRY_RESTR_QUERY_PARAM + hasCountryRestr);
              }
            }
          } else if ((null != nmBorderFreePayPalResponse.getAltBuyerResponseVO()) && (null != nmBorderFreePayPalResponse.getAltBuyerResponseVO().getExpPayPalErrMsg())
                  && !StringUtils.isBlank(nmBorderFreePayPalResponse.getAltBuyerResponseVO().getErrorMsg())) {
            getResponse().sendRedirect(CART_PATH);
            return false;
          }
          // INT-1900 changes ends here
          nmBorderFreeGetQuoteResponse = fiftyOneUtils.getInternationalCheckoutAPIUtil().callgetQuote(profile, order, null, true, null);
          
          if (!StringUtil.isBlank(nmBorderFreeGetQuoteResponse.getErrorMessage())) {
            // CheckoutComponents.getMessageContainer(getRequest()).addAll(resSession.getMessages());
            pageModel.setBorderFreeMsgs(nmBorderFreeGetQuoteResponse.getErrorMessage());
            if (null != esbErrorMessages) {
              if (nmBorderFreeGetQuoteResponse.getErrorMessage().equalsIgnoreCase(esbErrorMessages.getErrorMessage("INTERNAL_ERROR", null))
                      || nmBorderFreeGetQuoteResponse.getErrorMessage().equalsIgnoreCase(esbErrorMessages.getErrorMessage("INV_DAT_MISCALCULATION", null))
                      || nmBorderFreeGetQuoteResponse.getErrorMessage().equalsIgnoreCase(esbErrorMessages.getErrorMessage("CHECKOUT_SYSTEM_ERROR", null))) {
                order.setChinaUnionPayPaymentType(false);
                pageModel.setBorderFreeConnectivityIssue(true);
              }
            }
          }
          if ((nmBorderFreePayPalResponse.getPalPalResponseVO() != null)
                  && (nmBorderFreePayPalResponse.getAltBuyerResponseVO() != null)
                  && (!StringUtils.isBlank(nmBorderFreePayPalResponse.getPalPalResponseVO().getErrorMessage())
                          || !StringUtils.isBlank(nmBorderFreePayPalResponse.getAltBuyerResponseVO().getErrorMsg())
                          || (null != nmBorderFreePayPalResponse.getAltBuyerResponseVO().getExpPayPalErrMsg()) || (null != nmBorderFreePayPalResponse.getPalPalResponseVO().getPayPalErrMsg()))) {
            pageModel.setBorderFreeConnectivityIssue(true);
          }
          if (nmBorderFreeGetQuoteResponse.getBasketTotalResponseVO() != null) {
            if (!(getPageModel().getPageDefinition().getId().equalsIgnoreCase("OrderComplete") || getPageModel().getPageDefinition().getId().equalsIgnoreCase("PrintOrder"))) {
              CheckoutAPI.resetInternationalOrderProperties(order, cart, nmBorderFreeGetQuoteResponse);
              // INT-1027 changes starts
              pageModel.setDefaultShippingCarrier((String) order.getPropertyValue("shippingCarrierType"));
              // INT-1027 changes ends
            }
          }
          
          isCardSupportedForCountry = CheckoutAPI.validateBorderFreeSupportedCard(defaultCreditCardType, nmBorderFreeGetQuoteResponse);
          final String resetPaymentInfo = getRequest().getParameter("resetPaymentInfo");
          boolean cupCancelledByUser = false;
          if (!StringUtils.isBlank(resetPaymentInfo) && resetPaymentInfo.equalsIgnoreCase("true")) {
            cupCancelledByUser = Boolean.valueOf(resetPaymentInfo);
          }
          if (cupCancelledByUser) {
            isCardSupportedForCountry = false;
          }
          pageModel.setCardSupportedFlag(isCardSupportedForCountry);
          
          getPageModel().setNmBorderFreeGetQuoteResponse(nmBorderFreeGetQuoteResponse);
          // 1874 story
          // bookmark redirection logic
          if (fiftyOneUtils.isDisableCheckoutButton()) {
            if (getPageModel().getPageDefinition().getId().equalsIgnoreCase("Billing") || getPageModel().getPageDefinition().getId().equalsIgnoreCase("Shipping") || isOrderReviewPage) {
              getResponse().sendLocalRedirect(CART_PATH, getRequest());
              return false;
            }
          }
        }
      }
      
    }
    
    if ((cart != null) && !CheckoutAPI.isVmeReturned(getRequest())) {
      if (!processPayPalExpressCheckout(cart, profile)) {
        return false;
      }
      pageModel.setAtLeastOneInternationalShipTo(cart.getHasAtLeastOneInternationalShipTo());
    }
    
    // VMe changes : adding a method to process VMe checkout
    final boolean visaCheckoutSuccess = processVisaCheckoutRequest(getRequest(), cart, profile);
    if (!visaCheckoutSuccess) {
      return false;
    }
    /* MasterPass changes Starts here */
    final boolean isMasterPassReturned = SUCCESS.equalsIgnoreCase(getRequest().getParameter(MASTERPASS_STATUS));
    if (isMasterPassReturned && (!pageModel.isMasterPassCard())) {
      final boolean masterPassCheckoutSuccess = processMasterPassCheckout(cart, profile);
      if (!masterPassCheckoutSuccess) {
        return false;
      }
    } /* MasterPass changes ends here */
    
    /*vendor restriction code start*/
		final BrandSpecs brandSpecs = CommonComponentHelper.getBrandSpecs();
		if (brandSpecs.isEnableDepartmentQuantityRestrictions() && !(getPageModel().getPageDefinition().getId().equalsIgnoreCase("OrderComplete") || getPageModel().getPageDefinition().getId().equalsIgnoreCase("PrintOrder"))) {
			ResultBean resultBean = CheckoutAPI.checkDepartmentQuantityRestriction(order);
			final List<Message> messages = new ArrayList<Message>();
			if (!resultBean.getMessages().isEmpty()) {
				messages.addAll(resultBean.getMessages());
				CheckoutComponents.getMessageContainer(getRequest()).addAll(messages);
				final List<String> errMsgList = buildErrorMsgList(messages);
				pageModel.setVendorQuantityErrorMsgList(errMsgList);
				pageModel.setVendorQuantityOmnitureData(resultBean.getOmnitureData());
				if (!getPageModel().getPageDefinition().getId().equalsIgnoreCase("Cart")) {
					getResponse().sendLocalRedirect(CART_PATH, getRequest());
					return false;
				}
			}
		}    
    /*vendor restriction code end*/
    
    // setting the boolean showEstimates as true or false to display the text estimated in the order widget
    if (order != null) {
      pageModel.setShowEstimate(getShowEstimates(profile, order));
    }
    return true;
  }
  
  protected NMOrderImpl getCurrentOrder(final ShoppingCartHandler cart) {
    return cart.getNMOrder();
  }
  /*vendor quantity restriction*/
  private List<String> buildErrorMsgList(final Collection<Message> msgList) {
      final List<String> errorMsgList = new ArrayList<String>();
      final Iterator<Message> msgIterator = msgList.iterator();
      Message msg = null;
      while (msgIterator.hasNext()) {
        msg = msgIterator.next();
        if(msg.getMsgId().equalsIgnoreCase("vendorQuantity")){
            errorMsgList.add(msg.getMsgText());
        }
      }
      return errorMsgList;
    }
  
  /*vendor quantity restriction*/
  
  /**
   * Updates GWP information, promo email flags, and promotion text in the model. Subclasses should call this after repricing the order.
   */
  protected void updatePromotionFields(final ShoppingCartHandler cart) throws Exception {
    final CheckoutPageModel pageModel = (CheckoutPageModel) getPageModel();
    final NMOrderImpl order = cart.getNMOrder();
    final NMOrderHolder orderHolder = (NMOrderHolder) cart.getOrderHolder();
    
    // updateGwpFields(pageModel, order);
    updatePromoEmailValidationFields(pageModel);
    pageModel.setActivePromoCodeText(getActivePromoCodeText(orderHolder, order, pageModel));
  }
  
  protected void updateGwpFields(final CheckoutPageModel model, final NMOrderImpl order) {
    boolean hasUnresolvedGwps = false;
    boolean hasDeclinedGwpSelects = false;
    
    if (!isEmpty(order.getGwpMultiSkuPromoMap())) {
      hasUnresolvedGwps = true;
    }
    if (!isEmpty(order.getGwpSelectPromoMap())) {
      hasUnresolvedGwps = true;
      hasDeclinedGwpSelects = true;
    }
    if (!isEmpty(order.getPwpMultiSkuPromoMap())) {
      hasUnresolvedGwps = true;
    }
    
    model.setHasUnresolvedGwps(hasUnresolvedGwps);
    model.setHasDeclinedGwpSelects(hasDeclinedGwpSelects);
  }
  
  /**
   * Updates the model with the promo email validation fields. This only applies to a specific type of promotion that requires email validation via a modal popup to gather additional information from
   * the user. The messages expected below will be generated during the normal promo application processing
   * 
   * @author nmve1
   * 
   * @param model
   * 
   * */
  protected void updatePromoEmailValidationFields(final CheckoutPageModel model) {
    
    final MessageContainer messagesContainer = CheckoutComponents.getMessageContainer(getRequest());
    final PromoBean promoBean = new PromoBean();
    Message message = null;
    for (final String key : messagesContainer.getIdMap().keySet()) {
      if (StringUtilities.isNotEmpty(key)) {
        if (key.equals("RequestPromoEmail")) {
          message = messagesContainer.getIdMap().get(key);
          if (null != message) {
            final Map<String, Object> extraParams = message.getExtraParams();
            if (null != extraParams) {
              if (extraParams.containsKey("initialEmailRequest")) {
                promoBean.setInitialEmailRequest(((String) extraParams.get("initialEmailRequest")).equalsIgnoreCase("Y") ? true : false);
              }
              if (extraParams.containsKey("promoCode")) {
                promoBean.setPromoCode((String) extraParams.get("promoCode"));
              }
              promoBean.setPromoEmailRequested(true);
            }
          }
          
          model.setPromoBean(promoBean);
        } else if (key.equals("PromoHasBeenRedeemed")) {
          message = messagesContainer.getIdMap().get(key);
          if (null != message) {
            final Map<String, Object> extraParams = message.getExtraParams();
            if (null != extraParams) {
              if (extraParams.containsKey("promoCount")) {
                promoBean.setPromoCount(Integer.parseInt((String) extraParams.get("promoCount")));
              }
              if (extraParams.containsKey("promoCode")) {
                promoBean.setPromoCode((String) extraParams.get("promoCode"));
              }
              promoBean.setPromoEmailRedeemed(true);
            }
          }
          
          model.setPromoBean(promoBean);
        }
      }
    }
  }
  
  /**
   * <p>
   * This method removes the replenishment option from order and also sets the variable storeCallEnabled to page model as true if store call is enabled. <br>
   * It also performs the silent authentication for employee profile.<br>
   * If authentication is success then, we are setting a flag already synched as true.<br>
   * </p>
   * 
   * @param pageModel
   *          CheckoutPageModel object
   * @param order
   *          NMOrderImpl object
   * @param profile
   *          NMProfile object
   * @param orderManager
   *          OrderManager object
   */
  protected void processEmployeeCheckout(final CheckoutPageModel pageModel, final NMOrderImpl order, final NMProfile profile, final NMOrderManager orderManager, final boolean isOrderReviewPage) {
    // User is Employee/Dependent. Mark order to evaluate Employee Pricing
    if (!isOrderReviewPage) {
      order.setEmployeeLoginStatus(NMOrderImpl.EMPLOYEE_ALREADY_LOGGED_IN);
    }
    /* call removeReplenishmentFromOrder method which removes replenishment if selectedInterval of commerceItem is not null. */
    try {
      CheckoutAPI.removeReplenishmentOptionFromOrder(order, orderManager);
    } catch (final CommerceException cex) {
      CommonComponentHelper.getLogger().logError("Error in method removeReplenshmentOptionFromOrder " + cex.getMessage());
    }
    // Store-call specific checks
    employeeDiscountsUtil.resetStoreCallEligibilityForAllItems(order, orderManager);
    if (employeeDiscountsConfig.isStoreCallEnabled()) {
      final String facilityNumber = (String) profile.getPropertyValue(FACILITY_NUMBER);
      if (!StringUtils.isBlank(facilityNumber)) {
        final String storeCallNo = employeeDiscountsConfig.getStoreNumberLookUpMap().get(facilityNumber);
        if (StringUtilities.isNotEmpty(storeCallNo)) {
          pageModel.setStoreCallEnabled(Boolean.TRUE);
          employeeDiscountsUtil.setStoreCallOptionForAllItems(order, storeCallNo);
        } else {
          pageModel.setStoreCallEnabled(Boolean.FALSE);
        }
      }
    }
    
    final List<NMCommerceItem> commerceItemsList = order.getNmCommerceItems();
    if ((null != commerceItemsList) && (commerceItemsList.size() > 0)) {
      for (final NMCommerceItem commerceItem : commerceItemsList) {
        if (commerceItem.isStoreCallEligible()) {
          pageModel.setOrderContainsStoreCallItems(Boolean.TRUE);
          break;
        }
      }
    }
    
    // Order state is OrderReview
    if (isOrderReviewPage) {
      String syncFailMsgKey = "";
      try {
        final EmployeeAuthenticationDetailsVO authDetailsVO = employeeDiscountsUtil.performSilentAuthentication(profile);
        if (authDetailsVO != null) {
          final boolean authStatus = employeeDiscountsUtil.isAuthenicationSuccessful(authDetailsVO);
          if (authStatus) {
            profile.setPropertyValue(EmployeeDiscountConstants.IS_ASSOCIATE_LOGGED_IN, true);
            // set order transient variable to award employee discounts (during reprice order), if any
            order.setEmployeeLoginStatus(NMOrderImpl.EMPLOYEE_PERFORMED_SILENT_AUTHENTICATION);
          } else {
            employeeDiscountsUtil.removeEmployeeDetailsFromProfile(authDetailsVO, profile);
            pageModel.getElementDisplaySwitchMap().put(UIElementConstants.CLC, Boolean.TRUE);
            syncFailMsgKey = EmployeeDiscountConstants.DEFAULT_SYNCFAIL;
            if (employeeDiscountsConfig.getAllowedEmployeeTypes().contains(authDetailsVO.getEmployeeType())) {
              syncFailMsgKey = EmployeeDiscountConstants.NO_DISC_ELIGIBLE;
            } else if (EmployeeDiscountConstants.RETIREE.equals(authDetailsVO.getEmployeeType())) {
              syncFailMsgKey = EmployeeDiscountConstants.RETIRE_SYNCFAIL;
            }
            if (!StringUtils.isBlank(syncFailMsgKey)) {
              pageModel.setPopupMsg(employeeDiscountsConfig.getMessage(syncFailMsgKey));
            }
          }
        }
        
      } catch (final EmployeeDiscountsException e) {
        CommonComponentHelper.getLogger().logError("Error in ESB call" + e.getMessage());
        if (e.getErrorCode().equalsIgnoreCase(EmployeeDiscountConstants.INTERNAL_SERVICE_ERROR) || e.getErrorCode().equalsIgnoreCase(EmployeeDiscountConstants.COMMUNICATION_FAILURE)) {
          pageModel.setPopupMsg(employeeDiscountsConfig.getMessage(EmployeeDiscountConstants.COMMUNICATION_FAILURE));
          profile.setPropertyValue(EmployeeDiscountConstants.IS_ASSOCIATE_LOGGED_IN, true);
        }
      }
    }
    
  }
  
  protected boolean isEmpty(final Map<String, ? extends Object> map) {
    return (map == null) || map.isEmpty();
  }
  
  protected boolean redirectAnonymous(final String redirectUrl) throws IOException {
    return redirectAnonymous(redirectUrl, "previous");
  }
  
  protected boolean redirectAnonymous(final String redirectUrl, final String pageName) throws IOException {
    final Message msg = new Message();
    msg.setMsgId("unauthorized");
    msg.setError(false);
    msg.setFieldId("groupTop");
    msg.setMsgText(String.format("Please login to access the %s page.", pageName));
    
    return redirectAnonymous(redirectUrl, msg);
  }
  
  protected boolean redirectAnonymous(final String redirectUrl, final Message message) throws IOException {
    final List<Message> messages = new ArrayList<Message>();
    messages.add(message);
    return redirectAnonymous(redirectUrl, messages);
  }
  
  protected boolean redirectAnonymous(final String redirectUrl, final Collection<Message> messages) throws IOException {
    final DynamoHttpServletRequest request = getRequest();
    final DynamoHttpServletResponse response = getResponse();
    final NMProfile profile = CheckoutComponents.getProfile(request);
    
    if (profile.isAnonymous()) {
      if (messages != null) {
        response.sendLocalRedirect(redirectUrl, request);
        CheckoutComponents.getMessageContainer(request).addAll(messages);
      } else {
        response.sendRedirect(redirectUrl);
      }
      
      response.setStatus(HttpServletResponse.SC_FOUND);
      
      return true;
    }
    
    return false;
  }
  
  public static boolean getShowEstimates(final NMProfile profile, final NMOrderImpl order) {
    return (profile.isAnonymous() && profile.isRegisteredProfile()) || (!profile.isAnonymous() && profile.isRegisteredProfile() && !CheckoutAPI.orderContainsShippingAddress(order))
            || (profile.isAnonymous() && !CheckoutAPI.orderContainsShippingAddress(order));
  }
  
  public static String getActivePromoCodes(final String activePromoCodeText) {
    String activePromoCodes = "";
    if (!StringUtils.isBlank(activePromoCodeText)) {
      final String[] promoText = activePromoCodeText.split(":");
      if (promoText.length > 1) {
        activePromoCodes = StringUtilities.isNotEmpty(promoText[1]) ? StringUtilities.trimSpaces(promoText[1]) : "";
      }
    }
    return activePromoCodes;
  }
  
  public static String getActivePromoCodeText(final NMOrderHolder orderHolder, final NMOrderImpl order, final CheckoutPageModel pageModel) throws Exception {
    final HashSet<String> promoCodes = new HashSet<String>();
    int displayCount = 0;
    final Collection<NMPromotion> awardedPromotions = order.getAwardedPromotions();
    final Iterator<NMPromotion> iterator = awardedPromotions.iterator();
    
    while (iterator.hasNext()) {
      final NMPromotion promotion = iterator.next();
      
      if (promotion.hasPromoCodes()) {
        final int promotionClass = promotion.getPromotionClass();
        
        switch (promotionClass) {
          case NMPromotion.GIFT_WITH_PURCHASE_SELECT:
            final Set<String> declinedGwpSelects = orderHolder.getDeclinedGwpSelects();
            String promoKey = promotion.getCode();
            if (!declinedGwpSelects.contains(promoKey)) {
              promoCodes.add(promotion.getPromoCodes());
            }
          break;
          case NMPromotion.PURCHASE_WITH_PURCHASE:
            final Set<String> declinedPwpSelects = orderHolder.getDeclinedPwps();
            promoKey = promotion.getCode();
            if (!declinedPwpSelects.contains(promoKey)) {
              promoCodes.add(promotion.getPromoCodes());
            }
          break;
          case NMPromotion.SHIPPING:
            final String promoType = promotion.getType();
            final PromotionsHelper promotionsHelper = CommonComponentHelper.getPromotionsHelper();
            if (!promotionsHelper.getTypesToExcludeFromActiveShippingPromos().contains(promoType)) {
              promoCodes.add(promotion.getPromoCodes());
              // if 135 shipping promotion is sucessfully applid display message to inform the user to select "Promotional Shipping" option
              // This changes are applicable to all brands except CUSP
              if ("135".equalsIgnoreCase(promoType)) {
                final BrandSpecs brandSpecs = CommonComponentHelper.getBrandSpecs();
                if (brandSpecs.isEnableIncirclePromoMsgDisplay()) {
                  pageModel.setShowIncirclePromoMsg(true);
                }
              }
            }
          break;
          default:
            promoCodes.add(promotion.getPromoCodes());
          break;
        }
      } else {
        final int promotionClass = promotion.getPromotionClass();
        
        switch (promotionClass) {
          case NMPromotion.GIFT_WITH_PURCHASE_SELECT:
            final Set<String> declinedGwpSelects = orderHolder.getDeclinedGwpSelects();
            String promoKey = promotion.getCode();
            if (!order.hasNoStoreFulfilledandDropshipFlag()) {
              displayCount = 0;
            } else if (!declinedGwpSelects.contains(promoKey)) {
              ++displayCount;
            }
          break;
          case NMPromotion.PURCHASE_WITH_PURCHASE:
            final Set<String> declinedPwpSelects = orderHolder.getDeclinedPwps();
            promoKey = promotion.getCode();
            if (!declinedPwpSelects.contains(promoKey)) {
              ++displayCount;
            }
          break;
          case NMPromotion.EXTRA_ADDRESS:
          break;
          case NMPromotion.GIFT_WRAP:
            final boolean hasGiftwrapping = order.hasGiftwrapping();
            if (hasGiftwrapping) {
              ++displayCount;
            }
          break;
          case NMPromotion.SHIPPING:
            final String promoType = promotion.getType();
            final PromotionsHelper promotionsHelper = CommonComponentHelper.getPromotionsHelper();
            if (!promotionsHelper.getTypesToExcludeFromActiveShippingPromos().contains(promoType)) {
              ++displayCount;
            }
          break;
          case NMPromotion.GIFT_WITH_PURCHASE:
          case NMPromotion.PERCENT_OFF:
          case NMPromotion.RULE_BASED:
            ++displayCount;
          break;
        }
      }
    }
    
    final StringBuffer returnValue = new StringBuffer("");
    
    displayCount += promoCodes.size();
    
    if (displayCount > 1) {
      returnValue.append("Promotions Applied");
    } else if (displayCount == 1) {
      returnValue.append("Promotion Applied");
    }
    
    if (promoCodes.size() > 0) {
      returnValue.append(": ");
      final Iterator<String> promoCodeIterator = promoCodes.iterator();
      returnValue.append(DynamicCodeUtils.toggleSubstitution(promoCodeIterator.next(), order));
      while (promoCodeIterator.hasNext()) {
        returnValue.append(",").append(DynamicCodeUtils.toggleSubstitution(promoCodeIterator.next(), order));
      }
    }
    
    return returnValue.toString();
  }
  
  protected boolean processPayPalExpressCheckout(final ShoppingCartHandler cart, final NMProfile profile) throws Exception {
    final String payPalToken = CheckoutAPI.getPayPalTokenParam(getRequest());
    
    if (StringUtilities.isNotEmpty(payPalToken) && CheckoutAPI.isPayPalReturned(getRequest())) {
      final String rootUrl = UrlUtils.getSchemeHostPort(getRequest());
      boolean error = false;
      
      try {
        if (getExpressCheckout(cart, profile, payPalToken)) {
          CheckoutAPI.setPayPalToken(getRequest(), payPalToken);
        } else {
          error = true;
        }
      } catch (final PayPalTokenExpiredException e) {
        final String newToken = getNewPayPalToken(cart, profile, rootUrl);
        
        if (StringUtilities.isNotEmpty(newToken)) {
          getResponse().sendRedirect(CheckoutAPI.getSetExpressCheckoutRedirectUrl(newToken));
          return false;
        } else {
          error = true;
        }
      } catch (final PayPalException e) {
        e.printStackTrace();
        error = true;
      }
      
      if (error) {
        String redirectUrl;
        final String message = "Could not obtain your PayPal account information at this time.";
        
        if ("cart".equals(CheckoutAPI.getPayPalReturnParam(getRequest()))) {
          redirectUrl = CART_PATH;
          CheckoutComponents.getPayPalFormHandler(getRequest()).addFormException(new DropletException(message));
        } else {
          redirectUrl = CheckoutComponents.getOrderReviewPageDefinition().getBreadcrumbUrl();
          
          final Message msg = new Message();
          msg.setMsgText(message);
          msg.setFieldId("groupTop");
          
          CheckoutComponents.getMessageContainer(getRequest()).add(msg);
        }
        
        getResponse().sendLocalRedirect(redirectUrl, getRequest());
        return false;
      }
    }
    
    return true;
  }
  
  protected boolean getExpressCheckout(final ShoppingCartHandler cart, final NMProfile profile, final String token) throws PayPalException {
    try {
      final List<Message> messages = CheckoutAPI.getExpressCheckout(cart, profile, token);
      CheckoutComponents.getMessageContainer(getRequest()).addAll(messages);
    } catch (final PayPalException e) {
      throw e;
    } catch (final Exception e) {
      e.printStackTrace();
      return false;
    }
    
    return true;
  }
  
  protected String getNewPayPalToken(final ShoppingCartHandler cart, final NMProfile profile, final String rootUrl) {
    String newToken = null;
    final String returnUrl = CheckoutComponents.getOrderReviewPageDefinition().getBreadcrumbUrl();
    final String cancelUrl = returnUrl;
    
    try {
      newToken = CheckoutAPI.setExpressCheckout(cart.getNMOrder(), profile, rootUrl, returnUrl, cancelUrl);
    } catch (final Exception e) {
      e.printStackTrace();
    }
    
    return newToken;
  }
  
  /**
   * This class loops over the credit cards in the order and sets orderContainsChinaUnionPay boolean as true if the order has china unionpay as the card
   * 
   * @param order
   * @return orderContainsChinaUnionPay
   */
  public boolean doesOrderContainChinaUnionPayCard(final NMOrderImpl order) {
    boolean orderContainsChinaUnionPay = false;
    if ((null != order.getCreditCards()) || !order.getCreditCards().isEmpty()) {
      for (final NMCreditCard cupCard : order.getCreditCards()) {
        final String cardType = cupCard.getCreditCardType();
        if (!StringUtils.isBlank(cardType) && cardType.equalsIgnoreCase(CHINA_UNIONPAY_CARD_TYPE)) {
          orderContainsChinaUnionPay = true;
          break;
        }
      }
    }
    return orderContainsChinaUnionPay;
  }
  
  // VMe changes starts
  /**
   * This method contains the logic to process VMe checkout. If vme token recieved is true then it saves all the purchase data to order and profile
   * 
   * @param cart
   *          Shopping Cart object
   * @param profile
   *          User profile object
   * @return boolean VisaCheckout success flag
   * @throws IOException
   *           redirect error
   */
  protected boolean processVisaCheckoutRequest(final DynamoHttpServletRequest request, final ShoppingCartHandler cart, final NMProfile profile) throws IOException {
    boolean processVme = true;
    if (CheckoutAPI.isVmeReturned(request)) {
      boolean visaCheckoutError = false;
      try {
        final List<Message> messages = CheckoutAPI.processVisaCheckout(cart, profile);
        if (!messages.isEmpty()) {
          CheckoutComponents.getMessageContainer(getRequest()).addAll(messages);
        }
      } catch (final VmeException vme) {
        CommonComponentHelper.getLogger().logError("Visa Checkout error:" + vme.getMessage(), vme);
        visaCheckoutError = true;
      } catch (final Exception e) {
        CommonComponentHelper.getLogger().logError("Visa Checkout error:" + e.getMessage(), e);
        visaCheckoutError = true;
      }
      if (visaCheckoutError) {
        final String message = "Not able to process Visa Checkout at this time.";
        final Message msg = new Message();
        msg.setMsgText(message);
        msg.setFieldId("groupTop");
        CheckoutComponents.getMessageContainer(getRequest()).add(msg);
        getResponse().sendLocalRedirect(CART_PATH, getRequest());
        processVme = false;
      }
    }
    return processVme;
  }
  
  // VMe changes starts
  /**
   * getting promocode list from order and adding period symbol as separator instead of comma separator
   * 
   * @return promoCodeList(String)
   */
  public String getActivePromoCodesForVme(final NMOrderImpl order) {
    String promoCodeList = "";
    final List<String> activePromos = order.getActivePromoCodeList();
    final Iterator<String> iterator = activePromos.iterator();
    while (iterator.hasNext()) {
      final String promoCode = iterator.next();
      if (!StringUtils.isBlank(promoCode)) {
        promoCodeList = (StringUtilities.isNotEmpty(promoCodeList) ? StringUtilities.trimSpaces(promoCodeList) + "." : "") + promoCode;
      }
    }
    return promoCodeList;
  }
  
  protected boolean getVMeCheckout(final ShoppingCartHandler cart, final NMProfile profile) throws VmeException {
    boolean vmeCheckout = true;
    try {
      final List<Message> messages = CheckoutAPI.processVisaCheckout(cart, profile);
      CheckoutComponents.getMessageContainer(getRequest()).addAll(messages);
    } catch (final VmeException vme) {
      vme.printStackTrace();
      vmeCheckout = false;
    } catch (final Exception e) {
      e.printStackTrace();
      vmeCheckout = false;
    }
    return vmeCheckout;
  }
  
  /**
   * This method invokes MasterPass Checkout Service and sets the masterPass Shipping and Payment details to Session object
   * 
   * @param cart
   * @param profile
   * @return masterPassCheckoutSuccess -boolean
   * @throws MasterPassException
   * @throws IOException
   */
  protected boolean processMasterPassCheckout(final ShoppingCartHandler cart, final NMProfile profile) throws MasterPassException, IOException {
    boolean masterPassCheckoutSuccess = true;
    final MasterPassInfoVO masterPassInfoVO = CheckoutComponents.getMasterPassInfoVO(ServletUtil.getCurrentRequest());
    MasterPassCheckoutResponseVO masterPassCheckoutResponseVO;
    try {
      masterPassCheckoutResponseVO = CommonComponentHelper.getMasterPassUtil().invokeMasterPassCheckoutService(cart, profile);
      masterPassInfoVO.setMasterPassCheckoutResponseVO(masterPassCheckoutResponseVO);
      final List<Message> messages = CheckoutAPI.processMasterPassCheckout(cart, profile);
      CheckoutComponents.getMessageContainer(getRequest()).addAll(messages);
    } catch (final MasterPassException mpe) {
      CommonComponentHelper.getLogger().logError("Error during MasterPass Checkout Service Call :" + mpe);
      getResponse().sendRedirect(CART_PATH + MASTER_PASS_QUERY_PARAM);
      masterPassCheckoutSuccess = false;
    } catch (final Exception e) {
      CommonComponentHelper.getLogger().logError("Error while processing MasterPass Checkout :" + e);
      CommonComponentHelper.getLogger().logError("Error while processing MasterPass Checkout :" + e.getStackTrace());
      getResponse().sendRedirect(CART_PATH + MASTER_PASS_QUERY_PARAM);
      masterPassCheckoutSuccess = false;
    }
    return masterPassCheckoutSuccess;
  }
  
  protected static final String CART_PATH = "/checkout/cart.jsp";
  
}
