package com.nm.commerce.pagedef.evaluator.checkout;

import static com.nm.common.INMGenericConstants.ONE;

import javax.servlet.jsp.PageContext;

import com.nm.ajax.checkout.addressbook.AddressUtils;
import com.nm.commerce.NMOrderHolder;
import com.nm.commerce.NMProfile;
import com.nm.commerce.checkout.CheckoutComponents;
import com.nm.commerce.pagedef.definition.PageDefinition;
import com.nm.commerce.pagedef.model.CheckoutPageModel;
import com.nm.commerce.promotion.NMOrderImpl;
import com.nm.components.CommonComponentHelper;
import com.nm.twoclickcheckout.config.TwoClickCheckoutConfig;
import com.nm.twoclickcheckout.util.TwoClickCheckoutUtil;
import com.nm.utils.StringUtilities;

public class TwoClickCheckoutPageEvaluator extends CheckoutPageEvaluator {
  @Override
  public boolean evaluate(final PageDefinition pageDefinition, final PageContext pageContext) throws Exception {
    boolean returnValue = super.evaluate(pageDefinition, pageContext);
    final NMProfile nmProfile = CheckoutComponents.getProfile(getRequest());
    final CheckoutPageModel pageModel = (CheckoutPageModel) getPageModel();
    NMOrderImpl buyItNowOrder = null;
    TwoClickCheckoutConfig twoClickCheckoutConfig = null;
    final TwoClickCheckoutUtil twoClickCheckoutUtil = CommonComponentHelper.getTwoClickCheckoutUtil();
    if (null != twoClickCheckoutUtil) {
      twoClickCheckoutConfig = twoClickCheckoutUtil.getTwoClickCheckoutConfig();
      final NMOrderHolder orderHolder = CheckoutComponents.getOrderHolder(getRequest());
      buyItNowOrder = twoClickCheckoutUtil.createBuyItNowOrder(nmProfile, orderHolder);
      if (buyItNowOrder.isBuyItNowOrder() && buyItNowOrder.getCommerceItemCount() == ONE) {
        if (twoClickCheckoutUtil.profileContainsAllBillingAndShippingInfo(nmProfile)) {
          String orderReviewPageSecureUrl = twoClickCheckoutUtil.getSecureUrl(twoClickCheckoutConfig.getOrderReviewPageUrl(), getRequest(), getResponse());
          if (StringUtilities.isNotBlank(orderReviewPageSecureUrl)) {
            getResponse().sendRedirect(orderReviewPageSecureUrl);
          }
        } else {
          pageModel.setOrder(buyItNowOrder);
          pageModel.setProfileContainsBillingAddress(AddressUtils.getInstance().doesAddressHaveRequiredFields(nmProfile.getBillingAddress()));
          pageModel.setProfileContainsShippingAddress(twoClickCheckoutUtil.profileContainsDomesticShippingAddress(nmProfile));
          pageModel.setProfileContainsPaymentInfo(nmProfile.isDefaultCreditCardAuthorized());
          pageModel.setExpressCheckoutEnablePageUrl(twoClickCheckoutConfig.getExpressCheckoutEnablePageUrl());
          pageModel.setExpressCheckoutBillingPageUrl(twoClickCheckoutConfig.getExpressCheckoutBillingPageUrl());
          pageModel.setExpressCheckoutShippingPageUrl(twoClickCheckoutConfig.getExpressCheckoutShippingPageUrl());
          pageModel.setExpressCheckoutPaymentPageUrl(twoClickCheckoutConfig.getExpressCheckoutPaymentPageUrl());
        }
      } else {
        twoClickCheckoutUtil.moveSavedOrderBackToCurrent(orderHolder);
        getResponse().sendRedirect(twoClickCheckoutConfig.getCartPageUrl());
      }
    }
    return returnValue;
  }
}
