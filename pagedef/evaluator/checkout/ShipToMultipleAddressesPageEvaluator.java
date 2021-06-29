package com.nm.commerce.pagedef.evaluator.checkout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpSession;
import javax.servlet.jsp.PageContext;

import atg.commerce.order.OrderManager;
import atg.naming.NameContext;
import atg.servlet.DynamoHttpServletRequest;
import atg.servlet.ServletUtil;

import com.nm.commerce.NMCommerceItem;
import com.nm.commerce.NMProfile;
import com.nm.commerce.checkout.CheckoutAPI;
import com.nm.commerce.checkout.CheckoutComponents;
import com.nm.commerce.pagedef.definition.CheckoutPageDefinition;
import com.nm.commerce.pagedef.definition.PageDefinition;
import com.nm.commerce.pagedef.model.CheckoutPageModel;
import com.nm.commerce.pagedef.model.bean.ShippingGroupAux;
import com.nm.commerce.promotion.NMOrderImpl;
import com.nm.components.CommonComponentHelper;
import com.nm.formhandler.ShoppingCartHandler;
import com.nm.formhandler.checkout.ManageAddressFormHandler;
import com.nm.repository.stores.Store;
import com.nm.sitemessages.Message;
import com.nm.sitemessages.MessageContainer;
import com.nm.sitemessages.MessageDefs;
import com.nm.tms.core.TMSMessageContainer;
import com.nm.tms.utils.TMSDataDictionaryUtils;
import com.nm.utils.GenerateRandomTokenUtil;
import com.nm.utils.StoreSkuInventoryUtils;
import com.nm.utils.StringUtilities;

public class ShipToMultipleAddressesPageEvaluator extends EstimatedShipDatePageEvaluator {
  
  @Override
  public boolean evaluate(final PageDefinition pageDefinition, final PageContext pageContext) throws Exception {
    final boolean returnValue = super.evaluate(pageDefinition, pageContext);
    
    final MessageContainer messages = CheckoutComponents.getMessageContainer(ServletUtil.getCurrentRequest());
    final CheckoutPageModel pageModel = (CheckoutPageModel) getPageModel();
    final CheckoutPageDefinition checkoutPageDefinition = (CheckoutPageDefinition) pageDefinition;
    
    final NMProfile profile = CheckoutComponents.getProfile(getRequest());
    final ShoppingCartHandler cart = CheckoutComponents.getCartHandler(getRequest());
    final NMOrderImpl order = cart.getNMOrder();
    final ManageAddressFormHandler formHandler = CheckoutComponents.getManageAddressFormHandler(getRequest());
    final ShippingGroupAux[] shippingGroups = pageModel.getShippingGroups();
    final OrderManager orderMgr = cart.getOrderManager();
    final Locale userLocale = cart.getUserLocale(getRequest(), null);
    final List<NMCommerceItem> items = order.getNmCommerceItems();
    final TMSDataDictionaryUtils dataDictionaryUtils = CommonComponentHelper.getDataDictionaryUtils();
    //get the TMSMessageContainer from request
    final TMSMessageContainer tmsMessageContainer = CheckoutComponents.getTMSMessageContainer(getRequest());
    final ArrayList<String> tmsMessageList = new ArrayList<String>();
    dataDictionaryUtils.buildMsgList(messages, tmsMessageList);
    try {
      final List<String> commerceItemIds = new ArrayList<String>(items.size());
      boolean changedShippingGroup = false;
      for (final NMCommerceItem item : items) {
        if (item.getQuantity() > 1) {
          commerceItemIds.add(item.getId());
          changedShippingGroup = true;
        }
      }
      for (final String itemId : commerceItemIds) {
        CheckoutAPI.splitUpCommerceItem(cart, profile, order, orderMgr, userLocale, itemId, true, false);
      }
      if (changedShippingGroup) {
        super.updateShippingGroups(order, pageModel, checkoutPageDefinition);
      }
    } catch (final Exception e) {
      e.printStackTrace();
      final Message msg = MessageDefs.getMessage(MessageDefs.MSG_GenericFormErrorMessage);
      msg.setFieldId("groupTop");
      msg.setMsgText("Could not update to multiple shipping addresses at this time.");
      tmsMessageList.add(msg.getMsgText());
      messages.add(msg);
    }
    setPageToken("shipToMultipleAddressesPageEvaluator", getRequest());
    Map shippingAddressMap = CheckoutAPI.buildShippingAddresses(cart.getNMOrder(), profile);
    pageModel.setAllShippingAddresses(shippingAddressMap);
    CheckoutAPI.setShippingAddressMap(shippingAddressMap);
    
    if (CommonComponentHelper.getBrandSpecs().isBOPSEnabled()) {
      updateBopsSpecificValues(pageModel, order, cart);
    }
    // Data Dictionary Attributes population.
    tmsMessageContainer.setMessages(tmsMessageList);
    dataDictionaryUtils.processTMSDataDictionaryAttributes(pageDefinition, order, profile, pageModel,tmsMessageContainer);
    // formHandler.setShippingGroupId(shippingGroups[0].getId());
    return returnValue;
  }
  
  private void updateBopsSpecificValues(final CheckoutPageModel pageModel, final NMOrderImpl order, final ShoppingCartHandler cart) {
    final List<NMCommerceItem> commerceItems = order.getCommerceItems();
    final Map<String, Store> cIdsToStores = new HashMap<String, Store>();
    if (!commerceItems.isEmpty()) {
      for (final NMCommerceItem nmCommerceItem : commerceItems) {
        final String pickupStoreNo = nmCommerceItem.getPickupStoreNo();
        if (StringUtilities.isNotEmpty(pickupStoreNo)) {
          final StoreSkuInventoryUtils skuUtils = CommonComponentHelper.getStoreSkuInventoryUtils();
          final Store store = CheckoutComponents.getShipToStoreHelper().getStoreAddressByStoreNumber(pickupStoreNo);
          cIdsToStores.put(nmCommerceItem.getId(), store);
        }
      }
      pageModel.setcIdsToStores(cIdsToStores);
    }
    
  }
  
  private void setPageToken(final String prefix, final DynamoHttpServletRequest request) {
    final NameContext nameContext = request.getRequestScope();
    final String uuid = GenerateRandomTokenUtil.generateRandomTokenWithCustomInfo(prefix);
    if (nameContext != null) {
      request.setAttribute("reqToken", uuid);
      nameContext.putElement("reqToken", uuid);
      final HttpSession session = request.getSession();
      session.setAttribute("pageToken", uuid);
    }
  }
  
}
