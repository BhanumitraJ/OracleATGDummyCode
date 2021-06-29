package com.nm.commerce.pagedef.evaluator;

import javax.servlet.http.HttpSession;
import javax.servlet.jsp.PageContext;

import org.apache.commons.lang.StringUtils;

import atg.servlet.DynamoHttpServletRequest;
import atg.servlet.DynamoHttpServletResponse;
import atg.servlet.ServletUtil;

import com.nm.authentication.AuthenticationHelper;
import com.nm.commerce.pagedef.definition.LoginPageDefinition;
import com.nm.commerce.pagedef.definition.PageDefinition;
import com.nm.commerce.pagedef.model.LoginPageModel;
import com.nm.commerce.pagedef.model.PageModel;
import com.nm.components.CommonComponentHelper;
import com.nm.formhandler.NMProfileFormHandler;
import com.nm.tms.core.TMSMessageContainer;
import com.nm.tms.utils.TMSDataDictionaryUtils;

public class LoginPageEvaluator extends SimplePageEvaluator {
  
  private static final String SIGNIN_SUCCESS_URL = "/templates/loginredirect.jsp";
  private static final String HOMEPAGE_URL = "/index.jsp";
  private static final String ACCOUNT = "account";
  
  @Override
  protected PageModel allocatePageModel() {
    return new LoginPageModel();
  }
  
  @Override
  public boolean evaluate(PageDefinition pageDefinition, PageContext pageContext) throws Exception {
    
    boolean returnValue = true;
    
    if (super.evaluate(pageDefinition, pageContext)) {
      
      LoginPageModel pageModel = (LoginPageModel) getPageModel();
      LoginPageDefinition loginPageDef = (LoginPageDefinition) pageDefinition;
      
      String headerSignIn = getRequest().getParameter("headerSignIn");
      String checkDynamicUrl = loginPageDef.getCheckDynamicUrl();
      String strSuccessUrl = loginPageDef.getSuccessUrl();
      String strErrorUrl = loginPageDef.getErrorUrl();
      
      // Data Dictionary Attributes population.
      pageModel.setPageName(ACCOUNT);
      TMSDataDictionaryUtils dataDictionaryUtils = CommonComponentHelper.getDataDictionaryUtils();
      dataDictionaryUtils.processTMSDataDictionaryAttributes(pageDefinition, null, getProfile(), pageModel, new TMSMessageContainer());
      // This flag is the one which indicates whether to do all sort of checks to hadle multiple sucess path for the login.jsp
      // as in LC where the user can be redirected to accout page or the previous page on click of signin
      
      if (!("Y").equalsIgnoreCase(checkDynamicUrl)) {
        pageModel.setSuccessUrl(strSuccessUrl);
        pageModel.setErrorUrl(strErrorUrl);
        return true;
      }
      
      HttpSession session = getRequest().getSession();
      String loginRefererString = (String) session.getAttribute("login_refer_url");
      AuthenticationHelper authHelper = (AuthenticationHelper) getRequest().resolveName("/nm/authentication/AuthenticationHelper");
      boolean isAuth = false;
      isAuth = authHelper.sessionHasLoggedInRegisteredUser(getRequest());
      
      // System.out.println("********************************************************");
      // System.out.println("Referrer from Req:: "+getRequest().getHeader("referer"));
      // System.out.println("headerSignIn :: "+headerSignIn );
      // System.out.println("Session Url:: "+loginRefererString);
      // System.out.println("Auth::"+isAuth);
      // System.out.println("********************************************************");
      
      if (("Y").equalsIgnoreCase(headerSignIn) && ("Y").equalsIgnoreCase(checkDynamicUrl)) {
        if (StringUtils.isEmpty(loginRefererString)) {
          session.setAttribute("login_refer_url", generateRedirectUrl());
          
        } else if (!getProfileHandler().getFormError()) {
          session.setAttribute("login_refer_url", loginRefererString);
        }
        
        pageModel.setSuccessUrl(SIGNIN_SUCCESS_URL);
        pageModel.setErrorUrl(strErrorUrl + "?headerSignIn=y");
      } else {
        if (!getProfileHandler().getFormError() && !isAuth) {
          session.setAttribute("login_refer_url", strSuccessUrl);
        } else {
          session.setAttribute("login_refer_url", loginRefererString);
        }
        pageModel.setSuccessUrl(SIGNIN_SUCCESS_URL);
        pageModel.setErrorUrl(strErrorUrl);
      }
      
    } else {
      returnValue = false;
    }
    return returnValue;
  }
  
  private String generateRedirectUrl() {
    
    String refUrl = null;
    try {
      refUrl = getRequest().getHeader("referer");
      
      if (StringUtils.isEmpty(refUrl)) {
        refUrl = HOMEPAGE_URL;
      }
      
    } catch (Exception e) {
      e.printStackTrace();
    }
    // System.out.println(">>>LoginPageEvaluator.generateRedirectUrl()"+refUrl);
    return refUrl;
    
  }
  
  protected DynamoHttpServletRequest getRequest() {
    return ServletUtil.getCurrentRequest();
  }
  
  protected DynamoHttpServletResponse getResponse() {
    return ServletUtil.getCurrentResponse();
  }
  
  protected NMProfileFormHandler getProfileHandler() {
    return (NMProfileFormHandler) getRequest().resolveName("/atg/userprofiling/ProfileFormHandler");
  }
  
} // End of Class
