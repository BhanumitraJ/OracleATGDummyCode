package com.nm.commerce.pagedef.evaluator.checkout;

import java.util.ArrayList;
import java.util.Collection;

import javax.servlet.jsp.PageContext;

import com.nm.commerce.NMCreditCard;
import com.nm.commerce.NMProfile;
import com.nm.commerce.checkout.CheckoutAPI;
import com.nm.commerce.checkout.CheckoutComponents;
import com.nm.commerce.pagedef.definition.PageDefinition;
import com.nm.commerce.pagedef.model.CheckoutPageModel;
import com.nm.commerce.promotion.NMOrderImpl;
import com.nm.common.NMBorderFreePayPalResponse;
import com.nm.components.CommonComponentHelper;
import com.nm.formhandler.NMProfileFormHandler;
import com.nm.formhandler.ShoppingCartHandler;
import com.nm.tms.core.TMSMessageContainer;
import com.nm.tms.utils.TMSDataDictionaryUtils;
import com.nm.utils.NMFormHelper;
import com.nm.utils.StringUtilities;

public class ManagePaymentPageEvaluator extends OrderReviewPageEvaluator {
  
  @Override
  public boolean evaluate(final PageDefinition pageDefinition, final PageContext pageContext) throws Exception {
    final boolean returnValue = super.evaluate(pageDefinition, pageContext);
    
    final CheckoutPageModel pageModel = (CheckoutPageModel) getPageModel();
    final String changePPX = getRequest().getParameter("changePPX");
    final NMProfile profile = CheckoutComponents.getProfile(getRequest());
    final NMProfileFormHandler profileHandler = (NMProfileFormHandler) CheckoutComponents.getProfileFormHandler(getRequest());
    final NMOrderImpl order = CheckoutComponents.getCurrentOrder(getRequest());
    final Collection<NMCreditCard> profileCards = profile.getCreditCardMap().values();
    final TMSDataDictionaryUtils dataDictionaryUtils = CommonComponentHelper.getDataDictionaryUtils();
    //get the TMSMessageContainer from request
    final TMSMessageContainer tmsMessageContainer = CheckoutComponents.getTMSMessageContainer(getRequest());
    final ArrayList<String> tmsMessageList = new ArrayList<String>();
    
    if (redirectAnonymous(CheckoutComponents.getOrderReviewPageDefinition().getBreadcrumbUrl(), "manage payment")) {
      return false;
    }
    
    if (pageModel.isInvalidOrderAddresses()) {
      final String redirectUrl = NMFormHelper.reviseUrl("/checkout/orderreview.jsp");
      getResponse().sendLocalRedirect(redirectUrl, getRequest());
      return false;
    }
    
    for (final NMCreditCard creditCard : order.getCreditCards()) {
      // MasterPass Changes : As CreditCard details need not be stored for Profile in MasterPass Checkout,Skip the below step for MasterPass
      if (!profileCards.contains(creditCard) && !("true").equalsIgnoreCase(changePPX) && !order.isMasterPassPayGroupOnOrder()) {
        CheckoutAPI.copyOrderCreditCardToProfile(order, profile, profileHandler, creditCard);
      }
    }
    pageModel.setCreditCardMap(profile.getCreditCardMap());
    // setting te order credit card to page model attribut if the order is masterpass
    if (order.isMasterPassPayGroupOnOrder()) {
      for (final NMCreditCard creditCard : order.getCreditCards()) {
        if (null != creditCard) {
          pageModel.setMasterPassCreditcard(creditCard);
          break;
        }
      }
    }
    
    final boolean isPPX = CheckoutAPI.isInternationalSession(profile) && !StringUtilities.isEmpty(changePPX) && Boolean.valueOf(changePPX);
    if (isPPX) {
      final NMBorderFreePayPalResponse bfPPXResp = CheckoutComponents.getNMBorderFreePayPalResponse(getRequest());
      if ((bfPPXResp != null) && StringUtilities.areEqual(bfPPXResp.getPaymentConfirmationToken(), getRequest().getParameter("token"))) {
        // Copy Shipping Address to profile. When use trying to change the PaymentType for Paypal Express
        final ShoppingCartHandler cart = CheckoutComponents.getCartHandler(getRequest());
        CheckoutAPI.copyInternationalShippingAddressToProfileFromOrder(order, cart.getOrderManager(), profile);
      }
    }
    // Data Dictionary Attributes population.
    tmsMessageContainer.setMessages(tmsMessageList);
    dataDictionaryUtils.processTMSDataDictionaryAttributes(pageDefinition, order, profile, pageModel, tmsMessageContainer);
    return returnValue;
  }
}
