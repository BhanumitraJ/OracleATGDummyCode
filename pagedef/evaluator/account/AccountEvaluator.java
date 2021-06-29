package com.nm.commerce.pagedef.evaluator.account;

import static com.nm.returnscredit.ReturnsCreditConstants.ORDER_ID;
import static com.nm.tms.constants.TMSDataDictionaryConstants.ACCOUNT;
import static com.nm.tms.constants.TMSDataDictionaryConstants.ACCOUNT_OVERVIEW;
import static com.nm.tms.constants.TMSDataDictionaryConstants.ADDRESS;
import static com.nm.tms.constants.TMSDataDictionaryConstants.ADDRESS_BOOK;
import static com.nm.tms.constants.TMSDataDictionaryConstants.HISTORY;
import static com.nm.tms.constants.TMSDataDictionaryConstants.ORDER_DETAILS;
import static com.nm.tms.constants.TMSDataDictionaryConstants.ORDER_DETAILS_SECONDARY_ID;
import static com.nm.tms.constants.TMSDataDictionaryConstants.ORDER_HISTORY;
import static com.nm.tms.constants.TMSDataDictionaryConstants.PAY;
import static com.nm.tms.constants.TMSDataDictionaryConstants.PAYMENT_INFORMATION;
import static com.nm.tms.constants.TMSDataDictionaryConstants.VIEW;

import java.io.IOException;
import java.util.ArrayList;

import javax.servlet.http.HttpSession;
import javax.servlet.jsp.PageContext;

import atg.core.util.StringUtils;
import atg.repository.RepositoryException;
import atg.servlet.DynamoHttpServletRequest;
import atg.servlet.DynamoHttpServletResponse;
import atg.servlet.ServletUtil;
import atg.userprofiling.PropertyManager;

import com.nm.catalog.history.NMCatalogHistoryCollector;
import com.nm.catalog.history.NMCatalogHistoryItem;
import com.nm.commerce.NMCommerceProfileTools;
import com.nm.commerce.NMCreditCard;
import com.nm.commerce.NMProfile;
import com.nm.commerce.checkout.CheckoutComponents;
import com.nm.commerce.pagedef.definition.AccountPageDefinition;
import com.nm.commerce.pagedef.definition.PageDefinition;
import com.nm.commerce.pagedef.evaluator.SimplePageEvaluator;
import com.nm.commerce.pagedef.model.PageModel;
import com.nm.components.CommonComponentHelper;
import com.nm.formhandler.NMProfileFormHandler;
import com.nm.profile.ProfileProperties;
import com.nm.returnscredit.ReturnsCreditConstants;
import com.nm.returnscredit.ajax.ReturnsCreditAjaxReq;
import com.nm.returnscredit.integration.ReturnsCreditCmosOrderVO;
import com.nm.returnscredit.integration.ReturnsCreditException;
import com.nm.returnscredit.integration.ReturnsCreditSession;
import com.nm.returnscredit.util.ReturnsCreditUtil;
import com.nm.tms.constants.TMSDataDictionaryConstants;
import com.nm.tms.core.TMSMessageContainer;
import com.nm.tms.utils.TMSDataDictionaryUtils;
import com.nm.twoclickcheckout.TwoClickCheckoutConstants;
import com.nm.utils.NMFormHelper;
import com.nm.utils.SystemSpecs;

public class AccountEvaluator extends SimplePageEvaluator {
  @Override
  public boolean evaluate(final PageDefinition pageDefinition, final PageContext pageContext) throws Exception {
    
    boolean returnValue = isLoggedIn(pageDefinition);
    DynamoHttpServletRequest currentRequest = getRequest();
    final SystemSpecs systemSpecs = CommonComponentHelper.getSystemSpecs();
     final TMSDataDictionaryUtils dataDictionaryUtils = CommonComponentHelper.getDataDictionaryUtils();
    final ArrayList<String> tmsMessageList = new ArrayList<String>();
    final NMProfileFormHandler profileFormHandler = (NMProfileFormHandler)CheckoutComponents.getProfileFormHandler(currentRequest);
    // get the TMSMessageContainer
    final TMSMessageContainer tmsMessageContainer = CheckoutComponents.getTMSMessageContainer(currentRequest);    
    dataDictionaryUtils.setFormErrorsToTmsMessageList(profileFormHandler, tmsMessageList);
    tmsMessageContainer.setMessages(tmsMessageList);
    if(CommonComponentHelper.getTwoClickCheckoutUtil().isTwoClickCheckoutEnabled(getProfile())){
      currentRequest.setParameter(TwoClickCheckoutConstants.ENABLE_TWO_CLICK_CHECKOUT, true);
    }
    if (returnValue) {
      if (currentRequest.getSession().getAttribute("clarityEmail") == null) {
        try {
          currentRequest.getSession().setAttribute("clarityEmail", "true");
        } catch (final Exception e) { /* don't do anything if this fails */}
      }
      returnValue = super.evaluate(pageDefinition, pageContext);
      
      if (returnValue) {
        // if the page definition indicates that the account page is associated with a silo
        // then make sure that the nav history at a silo.
        final AccountPageDefinition accountPageDefinition = (AccountPageDefinition) pageDefinition;
        final String siloId = accountPageDefinition.getSiloId();
        if (!StringUtils.isBlank(siloId)) {
          final NMCatalogHistoryItem siloItem = getNavHistory().getSilo();
          if (siloItem == null || !siloId.equals(siloItem.getRepositoryId())) {
            NMCatalogHistoryCollector.collect(currentRequest, siloId);
          }
        }
        ensureDefaultCard(getProfile());
        // Changes to invoke returns eligibility service from account overview page.
        final String orderId = getRequest().getParameter(ORDER_ID);
        if (!StringUtils.isBlank(orderId) && systemSpecs.isReturnsCreditEnabled()) {
          callReturnsEligibilityService(orderId);
        }
      }
      getPageModel().setAccountRecentlyRegistered(getProfile().isAccountRecentlyRegistered());
      getProfile().setAccountRecentlyRegistered(false);
      processTMSDataDictionaryForAccountPages(pageDefinition, getPageModel(), dataDictionaryUtils, tmsMessageContainer);
      getProfile().getRecentlyCancelledItems().clear();
    }
    return returnValue;
  }
  
  private boolean isLoggedIn(final PageDefinition pageDefinition) throws IOException {
    boolean isLoggedIn = true;
    
    final Boolean requiresLogin = pageDefinition.getRequiresLogin();
    final NMProfile profile = getProfile();
    
    final int securityStatus = ((Integer) profile.getPropertyValue("securityStatus")).intValue();
    final PropertyManager mngr = getPropertyManager();
    
    if (requiresLogin.booleanValue() && securityStatus < mngr.getSecurityStatusLogin()) {
      final DynamoHttpServletResponse response = this.getResponse();
      String loginUrl = "/account/login.jsp";
      final SystemSpecs systemSpecs = (SystemSpecs) ServletUtil.getCurrentRequest().resolveName("/nm/utils/SystemSpecs");
      if (systemSpecs.getProductionSystemCode().equalsIgnoreCase("WN")) {
        String fromLeftNAv = getRequest().getParameter("fromLN");
        String requestURLQS = getRequest().getRequestURIWithQueryString();
        String refererurl = getRequest().getHeader("referer");
        HttpSession session = getRequest().getSession();
        if (fromLeftNAv != null && !fromLeftNAv.isEmpty() && fromLeftNAv.equalsIgnoreCase("myNMNav")) {
          session.setAttribute("login_to_acct", requestURLQS);
        } else {
          // String currentSessionUrl = (String) session.getAttribute("login_to_acct");
          if (refererurl != null && !refererurl.isEmpty() && !refererurl.contains("login.jsp")) {
            session.setAttribute("login_to_acct", refererurl);
          }
        }
      }
      loginUrl = NMFormHelper.reviseUrl(loginUrl);
      
      // if we are redirecting to the login page, decrement the pageCount so
      // that the email popup will not display prematurely
      final DynamoHttpServletRequest request = this.getRequest();
      final Object pageCount = request.getSession().getAttribute("pageCount");
      
      if (pageCount != null && (Integer) pageCount > 0) {
        request.getSession().setAttribute("pageCount", (Integer) pageCount - 1);
      }
      response.sendLocalRedirect(loginUrl, getRequest());
      isLoggedIn = false;
    }
    return isLoggedIn;
  }
  
  private void ensureDefaultCard(final NMProfile profile) throws RepositoryException {
    final NMCreditCard defaultCard = profile.getDefaultCreditCard();
    
    if (defaultCard != null && profile.getCreditCards().isEmpty()) {
      defaultCard.setPropertyValue(ProfileProperties.CreditCard_billingAddress, profile.getBillingAddress());
      profile.addCreditCard(defaultCard);
    }
    
    if (profile.getCreditCards().isEmpty()) {
      final NMCommerceProfileTools profileTools = (NMCommerceProfileTools) profile.getProfileTools();
      final NMCreditCard newDefaultCard = new NMCreditCard();
      newDefaultCard.setRepositoryItem(profileTools.createCreditCardItem(profile));
      newDefaultCard.setCreditCardNumber("");
      newDefaultCard.setCreditCardType("");
      newDefaultCard.setExpirationMonth("");
      newDefaultCard.setExpirationYear("");
      newDefaultCard.setPropertyValue(ProfileProperties.CreditCard_billingAddress, profile.getBillingAddress());
      
      profile.addCreditCard(newDefaultCard);
    }
  }
  
  protected PropertyManager getPropertyManager() {
    return (PropertyManager) getRequest().resolveName("/atg/userprofiling/PropertyManager");
  }
  
  /**
   * <p>
   * populate TMS properties specifc to account pages like Overview, Address booke,payament hist
   * </p>
   * 
   * @param orderId
   *          the order id
   */
  private void callReturnsEligibilityService(final String orderId) {
    final ReturnsCreditUtil returnsCreditUtil = CommonComponentHelper.getReturnsCreditUtil();
    final ReturnsCreditSession returnsCreditSession = CheckoutComponents.getReturnsCreditSession(getRequest());
    final ReturnsCreditAjaxReq returnCreditAjaxReq = new ReturnsCreditAjaxReq();
    returnCreditAjaxReq.setOrderId(orderId);
    try {
      returnsCreditUtil.resetReturnsCreditCmosOrderVO(returnsCreditSession);
      final ReturnsCreditCmosOrderVO returnsCreditCmosOrderVO = returnsCreditUtil.invokeReturnsOrderEligibilityService(returnCreditAjaxReq.getOrderId());
      if (null != returnsCreditCmosOrderVO && returnsCreditCmosOrderVO.isOrderReturnsEligible()) {
        returnsCreditSession.setReturnsCreditCmosOrderVO(returnsCreditCmosOrderVO);
        returnsCreditSession.setSessionPageToken(ReturnsCreditConstants.RETURNS_CREDIT_SELECT_ITEMS_BODY_CLASS);
      } else if (null != returnsCreditCmosOrderVO) {
        returnsCreditSession.setReturnsCreditCmosOrderVO(returnsCreditCmosOrderVO);
      }
    } catch (final ReturnsCreditException rec) {
      returnsCreditUtil.resetReturnsCreditCmosOrderVO(returnsCreditSession);
      if (log.isLoggingError()) {
        log.logError("ReturnsCreditException in Returns Eligibility Service" + rec.getMessage());
      }
      if (log.isLoggingDebug()) {
        log.logDebug("ReturnsCreditException in Returns Eligibility Service", rec);
      }
    }
  }
  
  /**
   * Populate TMS properties specific to account pages like overview,history,address book and payment information page.
   * 
   * @param pageDefinition
   *          the page definition
   * @param pageModel
   *          the page model
   * @throws IOException
   *           Signals that an I/O exception has occurred.
   */
  public void processTMSDataDictionaryForAccountPages(final PageDefinition pageDefinition, final PageModel pageModel,TMSDataDictionaryUtils dataDictionaryUtils,TMSMessageContainer tmsMessageContainer) throws IOException {
    final DynamoHttpServletRequest request = getRequest();
    final String view = request.getParameter(VIEW);
    if (view == null) {
      pageModel.setPageName(ACCOUNT_OVERVIEW);
    } else if (view.equals(HISTORY)) {
      pageModel.setPageName(ORDER_HISTORY);
    } else if (view.equals(ADDRESS)) {
      pageModel.setPageName(ADDRESS_BOOK);
    } else if (view.equals(PAY)) {
      pageModel.setPageName(PAYMENT_INFORMATION);
    } else if (view.equals(ORDER_DETAILS)) {
      pageModel.setPageName(ORDER_DETAILS_SECONDARY_ID);
     } else{
      pageModel.setPageName(TMSDataDictionaryConstants.GLOBAL);
    }
    String pageName = pageModel.getPageName();
    pageModel.setSecondaryIdentifier(pageName);
    pageModel.setPageType(ACCOUNT);
    if (!StringUtils.isBlank(pageName)) {
    	dataDictionaryUtils.processTMSDataDictionaryAttributes(pageDefinition, null, getProfile(), pageModel, tmsMessageContainer);
    }
  }
}