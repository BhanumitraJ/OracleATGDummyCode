package com.nm.commerce.pagedef.evaluator.checkout;

import java.util.ArrayList;

import javax.servlet.http.HttpSession;
import javax.servlet.jsp.PageContext;

import atg.naming.NameContext;
import atg.servlet.DynamoHttpServletRequest;

import com.nm.commerce.NMProfile;
import com.nm.commerce.checkout.CheckoutAPI;
import com.nm.commerce.checkout.CheckoutComponents;
import com.nm.commerce.pagedef.definition.PageDefinition;
import com.nm.commerce.pagedef.model.CheckoutPageModel;
import com.nm.commerce.pagedef.model.bean.ShippingGroupAux;
import com.nm.components.CommonComponentHelper;
import com.nm.formhandler.ShoppingCartHandler;
import com.nm.formhandler.checkout.ManageAddressFormHandler;
import com.nm.sitemessages.MessageContainer;
import com.nm.tms.core.TMSMessageContainer;
import com.nm.tms.utils.TMSDataDictionaryUtils;
import com.nm.utils.GenerateRandomTokenUtil;
import com.nm.utils.NMFormHelper;

public class ManageAddressPageEvaluator extends OrderReviewPageEvaluator {
  
  @Override
  public boolean evaluate(final PageDefinition pageDefinition, final PageContext pageContext) throws Exception {
    final boolean returnValue = super.evaluate(pageDefinition, pageContext);
    
    final CheckoutPageModel pageModel = (CheckoutPageModel) getPageModel();
    final TMSDataDictionaryUtils dataDictionaryUtils = CommonComponentHelper.getDataDictionaryUtils();
    // get the TMSMessageContainer from request
    final TMSMessageContainer tmsMessageContainer = CheckoutComponents.getTMSMessageContainer(getRequest());
    final ArrayList<String> tmsMessageList = new ArrayList<String>();
    
    final NMProfile profile = CheckoutComponents.getProfile(getRequest());
    final ShoppingCartHandler cart = CheckoutComponents.getCartHandler(getRequest());
    final ManageAddressFormHandler formHandler = CheckoutComponents.getManageAddressFormHandler(getRequest());
    final MessageContainer messagesContainer = CheckoutComponents.getMessageContainer(getRequest());
    dataDictionaryUtils.buildMsgList(messagesContainer.getAll(), tmsMessageList);
    final ShippingGroupAux[] shippingGroups = pageModel.getShippingGroups();
    
    if (pageModel.isInvalidOrderAddresses()) {
      final String redirectUrl = NMFormHelper.reviseUrl("/checkout/orderreview.jsp");
      getResponse().sendLocalRedirect(redirectUrl, getRequest());
      return false;
    }
    setPageToken("manageAddressPageEvaluator", getRequest());
    
    pageModel.setAllShippingAddresses(CheckoutAPI.buildShippingAddresses(cart.getNMOrder(), profile));
    
    formHandler.setShippingGroupId(shippingGroups[0].getId());
    // Data Dictionary Attributes population.
    tmsMessageContainer.setMessages(tmsMessageList);
    dataDictionaryUtils.processTMSDataDictionaryAttributes(pageDefinition, cart.getNMOrder(), profile, pageModel,tmsMessageContainer);
    return returnValue;
  }
  
  private void setPageToken(final String prefix, final DynamoHttpServletRequest request) {
    final NameContext nameContext = request.getRequestScope();
    final String uuid = GenerateRandomTokenUtil.generateRandomTokenWithCustomInfo(prefix);
    if (nameContext != null) {
      request.setAttribute("reqToken", uuid);
      nameContext.putElement("reqToken", uuid);
      HttpSession session = request.getSession();
      session.setAttribute("pageToken", uuid);
    }
  }
  
}
