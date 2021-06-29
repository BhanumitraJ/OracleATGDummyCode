package com.nm.commerce.pagedef.evaluator;

import static com.nm.returnscredit.ReturnsCreditConstants.ORDER_LOOKUP_URL;
import static com.nm.tms.constants.TMSDataDictionaryConstants.VIEW;
import static com.nm.tms.constants.TMSDataDictionaryConstants.GUEST_ORDER_HISTORY;
import static com.nm.tms.constants.TMSDataDictionaryConstants.GUEST_ORDER_DETAILS;
import static com.nm.tms.constants.TMSDataDictionaryConstants.ACCOUNT;

import java.io.IOException;

import javax.servlet.jsp.PageContext;
import atg.servlet.DynamoHttpServletRequest;

import atg.naming.NameContext;

import com.nm.commerce.checkout.CheckoutComponents;
import com.nm.commerce.pagedef.definition.PageDefinition;
import com.nm.commerce.pagedef.definition.ReturnsCreditPageDefinition;
import com.nm.commerce.pagedef.model.PageModel;
import com.nm.commerce.pagedef.model.ReturnsCreditPageModel;
import com.nm.commerce.pagedef.model.bean.Breadcrumb;
import com.nm.components.CommonComponentHelper;
import com.nm.returnscredit.ReturnsCreditConstants;
import com.nm.returnscredit.config.ReturnsCreditConfig;
import com.nm.returnscredit.integration.ReturnsCreditSession;
import com.nm.returnscredit.util.ReturnsCreditUtil;
import com.nm.tms.core.TMSMessageContainer;
import com.nm.tms.utils.TMSDataDictionaryUtils;
import com.nm.utils.StringUtilities;
import com.nm.utils.SystemSpecs;

/**
 * <p>
 * The Class <code> ReturnsCreditPageEvaluator</code>. sets up any redirects<br>
 * before page execution to evaluate returns credit of user session.
 * </p>
 * 
 * @author Cognizant
 */
public class ReturnsCreditPageEvaluator extends SimplePageEvaluator {
  /** The Constant ORDER_LOOKUP. */
  public static final String ORDER_LOOKUP = "orderLookup";
  /** The Constant NO_CACHE_MAX_AGE_0_MUST_REVALIDATE_NO_STORE. */
  private static final String NO_CACHE_MAX_AGE_0_MUST_REVALIDATE_NO_STORE = "no-cache, max-age=0, must-revalidate, no-store";
  /** The Constant CACHE_CONTROL. */
  private static final String CACHE_CONTROL = "Cache-Control";
  /** The Constant PREVIOUS. */
  private static final String PREVIOUS = "prev";
  /** The Constant CONFIRMATION_TITLE. */
  private static final String CONFIRMATION_TITLE = "Confirmation";
  /** The Constant HOME_PAGE_URL. */
  private static final String HOME_PAGE_URL = "homePageUrl";
  /** The Constant ORDER_HISTORY_URL. */
  private static final String ORDER_HISTORY_URL = "orderHistoryUrl";
  
  /**
   * Extend this method for a more specific to Returns Credit Page Model
   */
  @Override
  protected PageModel allocatePageModel() {
    return new ReturnsCreditPageModel();
  }
  
  @Override
  public boolean evaluate(final PageDefinition pageDefinition, final PageContext pageContext) throws Exception {
    if (!super.evaluate(pageDefinition, pageContext)) {
      return false;
    }
    // Cache Control header has been set to invalidate the browser cache and invoke the page evaluator when user does browser back button.
    getResponse().setHeader(CACHE_CONTROL, NO_CACHE_MAX_AGE_0_MUST_REVALIDATE_NO_STORE);
    final ReturnsCreditPageModel returnsCreditPageModel = (ReturnsCreditPageModel) getPageModel();
    final ReturnsCreditSession returnsCreditSession = CheckoutComponents.getReturnsCreditSession(getRequest());
    if (pageDefinition instanceof ReturnsCreditPageDefinition) {
      final ReturnsCreditPageDefinition returnsCreditPageDefinition = (ReturnsCreditPageDefinition) pageDefinition;
      if(StringUtilities.isNotBlank(returnsCreditPageDefinition.getBreadcrumbUrl())){
    	  returnsCreditPageModel.setBreadcrumbs(getBreadcrumbs(returnsCreditPageDefinition, returnsCreditPageModel));
          returnsCreditPageModel.setCurrentBreadcrumb(getCurrentBreadcrumb(returnsCreditPageModel.getBreadcrumbs()));  
      }
      // Data Dictionary Attributes population.
      final DynamoHttpServletRequest request = getRequest();
      final String view = request.getParameter(VIEW);
      // Populate TMS properties specific to Guest order page
      if (null != view && view.equalsIgnoreCase(GUEST_ORDER_HISTORY)){
    	  returnsCreditPageModel.setPageName(GUEST_ORDER_DETAILS);
    	  returnsCreditPageModel.setPageType(ACCOUNT); 
      }
      final TMSDataDictionaryUtils dataDictionaryUtils = CommonComponentHelper.getDataDictionaryUtils();
      dataDictionaryUtils.processTMSDataDictionaryAttributes(pageDefinition, null, getProfile(), returnsCreditPageModel, new TMSMessageContainer());
    }
    return evaluateReturnsCreditForUserSession(returnsCreditSession);
  }
  
  /**
   * <p>
   * Evaluate returns credit for user session.ReturnsCredit is enabled , <br>
   * then checks whether the user session is available then redirect to account page, <br>
   * else checks if the returnscredit session does not have cmosordervo then redirect to account page.
   * </p>
   * 
   * @param returnsCreditSession
   *          the returns credit session
   * @return true, if successful
   * @throws IOException
   *           Signals that an I/O exception has occurred.
   */
  public boolean evaluateReturnsCreditForUserSession(final ReturnsCreditSession returnsCreditSession) throws IOException {
    final SystemSpecs systemSpecs = CommonComponentHelper.getSystemSpecs();
    final ReturnsCreditPageModel returnsCreditPageModel = (ReturnsCreditPageModel) getPageModel();
    final String bodyClass = returnsCreditPageModel.getBodyClass();
    final NameContext nameContext = getRequest().getRequestScope();
    final boolean authorized = getProfile().isAuthorized();
    final ReturnsCreditUtil returnsCreditUtil = CommonComponentHelper.getReturnsCreditUtil();
    Object pageToken = null;
    final String sessionPageToken = returnsCreditSession.getSessionPageToken();
    if (nameContext != null) {
      pageToken = nameContext.getElement(ReturnsCreditConstants.PAGE_TOKEN);
    }
    final String pageId = returnsCreditPageModel.getPageId();
    if (systemSpecs.isReturnsCreditEnabled()) {
      if ((sessionPageToken != null) && sessionPageToken.contains(bodyClass)) {
        // validate the session page token to check whether it contains page level body class or not so it wouldn't redirect. Evaluator will take care of to navigate to corresponding page.
        if (authorized && bodyClass.equalsIgnoreCase(ORDER_LOOKUP)) {
          getResponse().sendLocalRedirect(getReturnsCreditConfig().getUrlMap().get(ORDER_HISTORY_URL), getRequest());
        } else if ((pageToken == null) && (returnsCreditSession.getNewgisticsUrl() != null)) {
          returnsCreditUtil.resetReturnsCreditCmosOrderVO(returnsCreditSession);
        }
      } else if ((pageToken == null)
              && (pageId.equals(ReturnsCreditConstants.RETURNS_CREDIT_SELECT_ITEMS_DEFINITION) || pageId.equals(ReturnsCreditConstants.RETURNS_CREDIT_REVIEW_ITEMS_DEFINITION) || pageId
                      .equals(ReturnsCreditConstants.RETURNS_CREDIT_CONFIRMATION_DEFINITION))) {
        validateRegisteredProfileAndRedirectPage(authorized, returnsCreditSession);
      } else if ((sessionPageToken != null) && sessionPageToken.contains(ReturnsCreditConstants.CONFIRMATION_PAGE_BODYCLASS) && bodyClass.equalsIgnoreCase(ORDER_LOOKUP)) {
        // we have to reset the session VO once user has successfully initiated the returns / printed the label
        returnsCreditUtil.resetReturnsCreditCmosOrderVO(returnsCreditSession);
      }
      updateSessionTokenWithBodyClass(returnsCreditSession, bodyClass);
    } else {
      if (authorized) {
        getResponse().sendLocalRedirect(getReturnsCreditConfig().getUrlMap().get(ORDER_HISTORY_URL), getRequest());
      } else {
        getResponse().sendLocalRedirect(getReturnsCreditConfig().getUrlMap().get(HOME_PAGE_URL), getRequest());
      }
    }
    if ((null == returnsCreditSession.getReturnsCreditCmosOrderVO()) && !bodyClass.equalsIgnoreCase(ORDER_LOOKUP)) {
      validateRegisteredProfileAndRedirectPage(authorized, returnsCreditSession);
    }
    return true;
  }
  
  /**
   * Validate registered profile and redirect page.
   * 
   * @param authorized
   *          the authorized
   * @param returnsCreditSession
   * @throws IOException
   *           Signals that an I/O exception has occurred.
   */
  public void validateRegisteredProfileAndRedirectPage(final boolean authorized, final ReturnsCreditSession returnsCreditSession) throws IOException {
    if (authorized) {
      getResponse().sendLocalRedirect(getReturnsCreditConfig().getUrlMap().get(ORDER_HISTORY_URL), getRequest());
    } else {
      getResponse().sendLocalRedirect(getReturnsCreditConfig().getUrlMap().get(ORDER_LOOKUP_URL), getRequest());
      returnsCreditSession.setReturnsCreditCmosOrderVO(null);
    }
  }
  
  /**
   * Gets the returns credit config.
   * 
   * @return the returns credit config
   */
  public ReturnsCreditConfig getReturnsCreditConfig() {
    return CommonComponentHelper.getReturnsCreditConfig();
  }
  
  /**
   * Update session token with body class.
   * 
   * @param returnsCreditSession
   *          the returns credit session
   * @param bodyClass
   *          the body class
   */
  private void updateSessionTokenWithBodyClass(final ReturnsCreditSession returnsCreditSession, final String bodyClass) {
    final String sessionPageToken = returnsCreditSession.getSessionPageToken() == null ? ReturnsCreditConstants.EMPTY_STRING : returnsCreditSession.getSessionPageToken();
    final String sessionTokens[] = sessionPageToken.split(ReturnsCreditConstants.UNDERSCORE_STRING);
    final StringBuffer returnValue = new StringBuffer();
    if ((sessionTokens != null) && (sessionTokens.length == 1)) {
      returnValue.append(bodyClass).append(ReturnsCreditConstants.UNDERSCORE_STRING).append(sessionPageToken);
    } else {
      returnValue.append(bodyClass).append(ReturnsCreditConstants.UNDERSCORE_STRING).append(sessionTokens[sessionTokens.length - 1]);
    }
    returnsCreditSession.setSessionPageToken(returnValue.toString());
  }
  
  /**
   * Gets the breadcrumbs.
   * 
   * @param returnsCreditPageDefinition
   *          the returns credit page definition
   * @return the breadcrumbs
   */
  protected Breadcrumb[] getBreadcrumbs(final ReturnsCreditPageDefinition returnsCreditPageDefinition, final ReturnsCreditPageModel returnsCreditPageModel) {
    Breadcrumb[] breadcrumbs = null;
    final String currentTitle = returnsCreditPageDefinition.getBreadcrumbTitle();
    boolean currentFound = false;
    final String last = getRequest().getParameter(PREVIOUS);
    int lastPage;
    try {
      lastPage = Integer.parseInt(last);
    } catch (final NumberFormatException e) {
      lastPage = 0;
    }
    if (lastPage > 4) {
      lastPage = 0;
    }
    breadcrumbs = constructBreadcrumbObject();
    if (currentTitle != null) {
      for (final Breadcrumb breadcrumb : breadcrumbs) {
        if (!currentFound && currentTitle.equals(breadcrumb.getTitle())) {
          currentFound = true;
          breadcrumb.setCurrentPage(true);
          if (returnsCreditPageDefinition.getBreadcrumbSubpage()) {
            breadcrumb.setClickable(true);
            breadcrumb.setPageNum(0);
          }
        }
        if (!currentFound || (breadcrumb.getPageNum() <= lastPage)) {
          if (!currentTitle.equalsIgnoreCase(CONFIRMATION_TITLE)) {
            breadcrumb.setClickable(true);
          }
          if (returnsCreditPageModel.getPageId().equalsIgnoreCase(ReturnsCreditConstants.RETURNS_CREDIT_SELECT_ITEMS_DEFINITION) && getProfile().isAuthorized()) {
            breadcrumb.setUrl(getReturnsCreditConfig().getUrlMap().get(ORDER_HISTORY_URL));
          }
        }
      }
    }
    return breadcrumbs;
  }
  
  /**
   * Construct breadcrumb object
   * 
   * @return the breadcrumb[] array will return breadcrumb values for orderLookup, selectItem,reviewItems and confirmation page.
   * 
   */
  public Breadcrumb[] constructBreadcrumbObject() {
    final ReturnsCreditPageDefinition orderLookupPageDefinition = CheckoutComponents.getOrderLookupPageDefinition();
    final ReturnsCreditPageDefinition returnsSelectItemPageDefinition = CheckoutComponents.getReturnsSelectItemPageDefinition();
    final ReturnsCreditPageDefinition returnsReviewPageDefinition = CheckoutComponents.getReturnsReviewPageDefinition();
    final ReturnsCreditPageDefinition returnsConfirmationPageDefinition = CheckoutComponents.getReturnsConfirmationPageDefinition();
    Breadcrumb[] breadcrumbs = null;
    breadcrumbs =
            new Breadcrumb[] {new Breadcrumb(orderLookupPageDefinition.getBreadcrumbUrl(), orderLookupPageDefinition.getBreadcrumbTitle(), 1) ,
                new Breadcrumb(returnsSelectItemPageDefinition.getBreadcrumbUrl(), returnsSelectItemPageDefinition.getBreadcrumbTitle(), 2) ,
                new Breadcrumb(returnsReviewPageDefinition.getBreadcrumbUrl(), returnsReviewPageDefinition.getBreadcrumbTitle(), 3) ,
                new Breadcrumb(returnsConfirmationPageDefinition.getBreadcrumbUrl(), returnsConfirmationPageDefinition.getBreadcrumbTitle(), 4)};
    return breadcrumbs;
  }
  
  /**
   * Gets the current breadcrumb.
   * 
   * @param breadcrumbs
   *          the breadcrumbs
   * @return the current breadcrumb
   */
  public Breadcrumb getCurrentBreadcrumb(final Breadcrumb[] breadcrumbs) {
    Breadcrumb current = null;
    for (final Breadcrumb breadcrumb : breadcrumbs) {
      if (breadcrumb.isCurrentPage()) {
        current = breadcrumb;
      }
    }
    return current;
  }
  
}
