package com.nm.commerce.pagedef.evaluator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.jsp.PageContext;

import atg.nucleus.Nucleus;

import com.nm.commerce.NMProfile;
import com.nm.commerce.checkout.CheckoutAPI;
import com.nm.commerce.checkout.CheckoutComponents;
import com.nm.commerce.pagedef.definition.PageDefinition;
import com.nm.commerce.pagedef.model.PageModel;
import com.nm.commerce.pagedef.model.UIElementConstants;
import com.nm.components.CommonComponentHelper;
import com.nm.edo.EmployeeDiscountsConfig;
import com.nm.tms.core.TMSMessageContainer;
import com.nm.utils.LocalizationUtils;

/**
 * @author Cognizant. When user lands on ELP page, This class is used to Verify whether User's current experience is Domestic and if not make user experience default to domestic.
 * 
 */

public class EmployeeDiscountPageEvaluator extends SimplePageEvaluator {
  
  private static final String EDO = "EDO";
  private static final String EMPLOYEE_DISCOUNT = "Employee Discount";
  private static final String NM= "NM";
  @Override
  public boolean evaluate(final PageDefinition pageDefinition, final PageContext pageContext) throws Exception {
    final boolean continueEvaluation = preEvaluateUserRequest(getProfile());
    // Check if we need to continue evaluation for the user request.
    if (!continueEvaluation) {
      return false;
    }
    /* User experience is domestic. Evaluate the request and construct pageModel. */
    final boolean returnValue = super.evaluate(pageDefinition, pageContext);
    // Hide CLC in Employee Landing Page.
    getPageModel().getElementDisplaySwitchMap().put(UIElementConstants.CLC, Boolean.FALSE);
    getPageModel().setPageType(EDO);
    final TMSMessageContainer tmsMessageContainer = CheckoutComponents.getTMSMessageContainer(getRequest());
    CommonComponentHelper.getDataDictionaryUtils().processTMSDataDictionaryAttributes(pageDefinition, null, getProfile(), getPageModel(), tmsMessageContainer);
    return returnValue;    
  }
  
  /**
   * This method forces user experience to domestic, if user request is international.
   * 
   * @param profile
   *          Profile object.
   * @return boolean indicating successful evaluation.
   * @throws IOException
   *           possible page redirection error.
   * */
  protected boolean preEvaluateUserRequest(final NMProfile profile) throws IOException {
    boolean preEvalResult = true;
    final EmployeeDiscountsConfig employeeDiscountsConfig = (EmployeeDiscountsConfig) Nucleus.getGlobalNucleus().resolveName("/nm/edo/EmployeeDiscountsConfig");
    if (CheckoutAPI.isInternationalSession(profile)) {
      profile.setCountryPreference(LocalizationUtils.US_COUNTRY_CODE);
      profile.setCurrencyPreference(LocalizationUtils.US_CURRENCY_CODE);
      getResponse().sendRedirect(employeeDiscountsConfig.getEmployeeLandingPagePath());
      preEvalResult = false;
    }
    return preEvalResult;
  }
}
