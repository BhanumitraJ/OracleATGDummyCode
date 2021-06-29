package com.nm.commerce.pagedef.evaluator.checkout;

import static com.nm.common.INMGenericConstants.US_COUNTRY_CODE;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.jsp.PageContext;

import atg.commerce.order.HardgoodShippingGroup;
import atg.commerce.order.OrderManager;
import atg.commerce.order.ShippingGroupCommerceItemRelationship;
import atg.core.util.StringUtils;
import atg.nucleus.logging.ApplicationLogging;
import atg.nucleus.logging.ClassLoggingFactory;
import atg.userprofiling.NMCookieManager;

import com.nm.ajax.checkout.addressbook.AddressUtils;
import com.nm.ajax.checkout.utils.ComponentUtils;
import com.nm.commerce.NMAddress;
import com.nm.commerce.NMProfile;
import com.nm.commerce.NMRepositoryContactInfo;
import com.nm.commerce.checkout.CheckoutAPI;
import com.nm.commerce.checkout.CheckoutComponents;
import com.nm.commerce.pagedef.definition.PageDefinition;
import com.nm.commerce.pagedef.model.CheckoutPageModel;
import com.nm.commerce.pagedef.model.PageModel;
import com.nm.commerce.pagedef.model.bean.ShippingGroupAux;
import com.nm.commerce.promotion.NMOrderImpl;
import com.nm.components.CommonComponentHelper;
import com.nm.formhandler.ShoppingCartHandler;
import com.nm.formhandler.checkout.ShippingAddressFormHandler;
import com.nm.integration.BoxHop;
import com.nm.profile.AddressVerificationHelper;
import com.nm.sitemessages.Message;
import com.nm.tms.core.TMSMessageContainer;
import com.nm.tms.utils.TMSDataDictionaryUtils;
import com.nm.twoclickcheckout.config.TwoClickCheckoutConfig;
import com.nm.twoclickcheckout.util.TwoClickCheckoutUtil;
import com.nm.utils.StringUtilities;

public class ShippingAddressPageEvaluator extends CheckoutPageEvaluator {
  
  private final ApplicationLogging mLogging = ClassLoggingFactory.getFactory().getLoggerForClass(ShippingAddressPageEvaluator.class);
	
  @Override
  protected PageModel allocatePageModel() {
    return new CheckoutPageModel();
  }
  
  @Override
  public boolean evaluate(PageDefinition pageDefinition, PageContext pageContext) throws Exception {
    boolean returnValue = super.evaluate(pageDefinition, pageContext);
    CheckoutPageModel pageModel = (CheckoutPageModel) getPageModel();
    final ShoppingCartHandler cart = CheckoutComponents.getCartHandler(getRequest());
    final NMOrderImpl order = cart.getNMOrder();
    final OrderManager orderMgr = cart.getOrderManager();
    NMProfile profile = CheckoutComponents.getProfile(getRequest());
    boolean isInternational = CheckoutAPI.isInternationalSession(profile);
    final TMSDataDictionaryUtils dataDictionaryUtils = CommonComponentHelper.getDataDictionaryUtils();
    //get the TMSMessageContainer from request
    final TMSMessageContainer tmsMessageContainer = (TMSMessageContainer) getRequest().resolveName("/nm/tms/TMSMessageContainer");
    final ArrayList<String> tmsMessageList = new ArrayList<String>();
    
    String sgId = getRequest().getParameter("sgId");
    String changeShippingAddress = getRequest().getParameter("change");
    
    // Defect 41960 changes starts
    if (StringUtils.isBlank(sgId)) {
      sgId = pageModel.getShippingGroups()[0].getId();
    }
    // Defect 41960 changes ends
    ShippingAddressFormHandler addr = CheckoutComponents.getShippingAddressFormHandler(getRequest());
    List<Message> messages = addr.getMessageList();
    
    if (messages.isEmpty()) {
      String profileCountry = profile.getCountryPreference();
      List<ShippingGroupAux> shippingGroups = Arrays.asList(pageModel.getShippingGroups());
      for (ShippingGroupAux sg : shippingGroups) {
        HardgoodShippingGroup shippingGroup = (HardgoodShippingGroup) sg.getShippingGroupRef();
        NMRepositoryContactInfo address = (NMRepositoryContactInfo) shippingGroup.getShippingAddress();
        if (sg.getId().equals(sgId) || ((sgId == null) && !address.getFlgGiftReg())) {
          // changes starts - making shipping group as null when previous shipping address country not equal to profile country
          if (order.getCommerceItemCount() > 0) {
            boolean notMatchingShipTo = (null != sg.getAddress()) && (null != sg.getAddress().getCountry()) && !sg.getAddress().getCountry().equalsIgnoreCase(profileCountry);
            boolean clearShippingGrp = false;
            // Check if Ship to is valid for International
            if (isInternational) {
              
              if (notMatchingShipTo) {
                // Verify if default address is matching profile country.
                NMAddress defaultShipAddr = profile.getShippingNMAddress();
                boolean shouldPopulateDefaultShipTo = (defaultShipAddr != null) && !StringUtils.isBlank(defaultShipAddr.getCountry()) && defaultShipAddr.getCountry().equalsIgnoreCase(profileCountry);
                
                if (shouldPopulateDefaultShipTo) {
                  // ShippingGroup has stale information, update the shipping group with default.
                  CheckoutAPI.updateShipingGroupAddressToNewAddress(sg, defaultShipAddr);
                } else {
                  // Default Ship To Address Country not matching selected country. Get the address from User.
                  clearShippingGrp = true;
                }
              }
            } else if (!CheckoutAPI.isDomesticOrderShipToValid(order)) {
              clearShippingGrp = true;
            } else if (order.hasBopsItem()) {
              clearShippingGrp = true;
            }
            if (clearShippingGrp) {
              sg.getAddress().setFirstName("");
              sg.getAddress().setLastName("");
              sg.getAddress().setAddress1("");
              sg.getAddress().setAddress2("");
              sg.getAddress().setCity("");
              sg.getAddress().setCountyCode("");
              sg.getAddress().setPostalCode("");
              sg.getAddress().setState("");
              sg.getAddress().setPhoneNumber("");
              sg.getAddress().setCountry(profile.getCountryPreference());
            }
            // vme changes
            if (!isInternational && ((null != changeShippingAddress) && changeShippingAddress.equalsIgnoreCase("true"))) {
              NMAddress defaultShipAddr = profile.getShippingNMAddress();
              if (null != defaultShipAddr) {
                defaultShipAddr.setPropertyValue("verificationFlag", AddressVerificationHelper.HAS_NOT_BEEN_VERIFIED);
              }
              
            }
          }
          // changes ends
          if (ComponentUtils.getInstance().getSystemSpecs().isBoxHopEnabled()) {
            String boxhopaddr = NMCookieManager.getCookieValue(getRequest().getCookies(), "boxhopaddr");
            if (!StringUtilities.isEmpty(boxhopaddr)) {
              populateBoxhopAddress(sg, boxhopaddr);
            }
          }
          
          /*
           * Registered Checkout ALL information in account STARTS
           */
          String defaultShippingCountry = null;
          if ((null != profile.getShippingNMAddress()) && (null != profile.getShippingNMAddress().getCountry())) {
            defaultShippingCountry = profile.getShippingNMAddress().getCountry();
          }
          if (ComponentUtils.getInstance().getSystemSpecs().isFiftyOneEnabled() && (!profile.isAnonymous())
                  && (((null != defaultShippingCountry) && !defaultShippingCountry.equalsIgnoreCase(profile.getCountryPreference())))) {
            addr.prefillFirstAndLastNameFields(profile);
          } else {
            addr.prefillFields(sg.getAddress(), sgId);
          }
          /*
           * Registered Checkout ALL information in account ENDS
           */
          break;
        }
      }
    }else{
      dataDictionaryUtils.buildMsgList(messages, tmsMessageList);
    }
    
    if (addr.getMessageList() != null) {
      Map<String, String> errMsgMap = buildErrorMsgMap(addr.getMessageList(), addr.getCountry(), isInternational);
      pageModel.setErrorMsgMap(errMsgMap);
    }
    List<ShippingGroupCommerceItemRelationship> commerceItemRelationships = CheckoutAPI.getShippingGroupCommerceItemRelationships(order, orderMgr);
    pageModel.setCommerceItemRelationships(commerceItemRelationships);
    updateGwpFields(pageModel, order);
    // Data Dictionary Attributes population.
    getPageModel().setAccountRecentlyRegistered(getProfile().isAccountRecentlyRegistered());
    getProfile().setAccountRecentlyRegistered(false);

    final TwoClickCheckoutUtil twoClickCheckoutUtil = CommonComponentHelper.getTwoClickCheckoutUtil();
    if (null != twoClickCheckoutUtil) {
      final TwoClickCheckoutConfig twoClickCheckoutConfig = twoClickCheckoutUtil.getTwoClickCheckoutConfig();
      pageModel.setExpressCheckoutEnablePageUrl(twoClickCheckoutConfig.getExpressCheckoutEnablePageUrl());
      pageModel.setExpressCheckoutBillingPageUrl(twoClickCheckoutConfig.getExpressCheckoutBillingPageUrl());
      pageModel.setExpressCheckoutShippingPageUrl(twoClickCheckoutConfig.getExpressCheckoutShippingPageUrl());
      pageModel.setExpressCheckoutPaymentPageUrl(twoClickCheckoutConfig.getExpressCheckoutPaymentPageUrl());
    }
    tmsMessageContainer.setMessages(tmsMessageList);
    dataDictionaryUtils.processTMSDataDictionaryAttributes(pageDefinition, order, profile, pageModel,tmsMessageContainer);
    pageModel.setProfileContainsPaymentInfo(profile.isDefaultCreditCardAuthorized());
    pageModel.setProfileContainsBillingAddress(AddressUtils.getInstance().doesAddressHaveRequiredFields(profile.getBillingAddress())
            && US_COUNTRY_CODE.equalsIgnoreCase(profile.getBillingNMAddress().getCountry()));
    return returnValue;
  }
  
  private Map<String, String> buildErrorMsgMap(List<Message> msgList, String country, boolean isInternational) {
    Map<String, String> errorMsgMap = new HashMap<String, String>();
    Iterator<Message> msgIterator = msgList.iterator();
    Message msg = null;
    while (msgIterator.hasNext()) {
      msg = msgIterator.next();
      // changes to get international validation message text
      if (!isInternational) {
        errorMsgMap.put(msg.getFieldId(), msg.getMsgText());
      } else {
        errorMsgMap.put(msg.getFieldId(), msg.getMsgIntlText());
      }
    }
    return errorMsgMap;
  }
  
  public void populateBoxhopAddress(ShippingGroupAux shippingGroupAux, String boxhopaddr) {
    try {
      boxhopaddr = URLDecoder.decode(boxhopaddr, "UTF-8");
      String[] boxHopAddress = boxhopaddr.split(",");
      final BoxHop boxHop = CheckoutComponents.getBoxHop(getRequest());
      NMRepositoryContactInfo shippingAddress = shippingGroupAux.getAddress();
      if (boxHop.isShippingPageRegistration()
              || (StringUtilities.isEmpty(shippingAddress.getAddress1()) && StringUtilities.isEmpty(shippingAddress.getPostalCode()) && StringUtilities.isEmpty(shippingAddress.getCity()))) {
        shippingAddress.setFirstName(StringUtilities.checkNull(boxHopAddress[0]));
        shippingAddress.setLastName(StringUtilities.checkNull(boxHopAddress[1]));
        shippingAddress.setAddress1(StringUtilities.checkNull(boxHopAddress[2]));
        shippingAddress.setAddress2(StringUtilities.checkNull(boxHopAddress[3]));
        shippingAddress.setCity(StringUtilities.checkNull(boxHopAddress[4]));
        shippingAddress.setState(StringUtilities.checkNull(boxHopAddress[5]));
        shippingAddress.setPostalCode(StringUtilities.checkNull(boxHopAddress[6]));
        shippingAddress.setCounty(StringUtilities.checkNull(boxHopAddress[7]));
        shippingAddress.setPhoneNumber(StringUtilities.checkNull(boxHopAddress[8]));
        boxHop.setShippingPageRegistration(false);
      }
    } catch (UnsupportedEncodingException e) {
      if (mLogging.isLoggingError()) {
    	  mLogging.logError(" decoding cookie in populateBoxhopAddress : " + e.getMessage());
      }
      
    }
    
  }
  
}
