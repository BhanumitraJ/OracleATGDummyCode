package com.nm.commerce.pagedef.evaluator.account;

import java.io.IOException;
import java.sql.Timestamp;

import javax.servlet.jsp.PageContext;

import atg.repository.ItemDescriptorImpl;
import atg.repository.RepositoryItem;
import atg.servlet.DynamoHttpServletRequest;
import atg.servlet.DynamoHttpServletResponse;

import com.nm.commerce.NMProfile;
import com.nm.commerce.checkout.CheckoutAPI;
import com.nm.commerce.checkout.CheckoutComponents;
import com.nm.commerce.pagedef.definition.PageDefinition;
import com.nm.commerce.pagedef.evaluator.SimplePageEvaluator;
import com.nm.commerce.pagedef.model.PageModel;
import com.nm.components.CommonComponentHelper;
import com.nm.formhandler.NMProfileFormHandler;
import com.nm.repository.ProfileRepository;
import com.nm.tms.core.TMSMessageContainer;
import com.nm.tms.utils.TMSDataDictionaryUtils;
import com.nm.utils.EncryptDecrypt;
import com.nm.utils.StringUtilities;

public class ResetPasswordPageEvaluator extends SimplePageEvaluator {
  
  @Override
  public boolean evaluate(final PageDefinition pageDefinition, final PageContext pageContext) {
    boolean returnValue = false;
    try {
      returnValue = super.evaluate(pageDefinition, pageContext);
      final PageModel pageModel = getPageModel();
      Timestamp expTime = null;
      String currentPassword = "";
      final DynamoHttpServletRequest request = getRequest();
      final DynamoHttpServletResponse response = getResponse();
      
      final String login = request.getParameter("uId");
      final String tempPassword = request.getParameter("tempPwd");
      final NMProfileFormHandler profileHandler = (NMProfileFormHandler) CheckoutComponents.getProfileFormHandler(getRequest());
      if (!StringUtilities.isNullOrEmpty(login)) {
        final RepositoryItem rProfile = CheckoutAPI.getProfileUsingEmail(login);
        if (null != rProfile) {
          ItemDescriptorImpl itemDesc = (ItemDescriptorImpl)rProfile.getItemDescriptor();
          itemDesc.removeItemFromCache(rProfile.getRepositoryId(), true);
          expTime = (Timestamp) rProfile.getPropertyValue(ProfileRepository.TEMP_PASSWORD_EXP_DATE);
          currentPassword = (String) rProfile.getPropertyValue(ProfileRepository.PASSWORD_PROP);
          
          if (null != expTime && tempPassword.equals(EncryptDecrypt.decrypt(currentPassword))) {
            final Timestamp currentTime = new Timestamp(System.currentTimeMillis());
            if (currentTime.before(expTime)) {
              profileHandler.setLastValidEnteredEmailAddress(login);
              getProfile().setPropertyValue(ProfileRepository.LAST_ENTERED_EMAIL_ADDRESS, login);
            } else {
              getProfile().setPropertyValue(ProfileRepository.LAST_ENTERED_EMAIL_ADDRESS, login);
              returnValue = !redirectIfPossible("/account/resetPassword.jsp?isExpired=true", request, response);
            }
          } else {
            final boolean isAnonymous = ((NMProfile) profileHandler.getProfile()).isAnonymous();
            if (isAnonymous) {
              getProfile().setPropertyValue(ProfileRepository.LAST_ENTERED_EMAIL_ADDRESS, login);
              returnValue = !redirectIfPossible("/account/resetPassword.jsp?isExpired=invalidPwd", request, response);
            } else {
              getProfile().setPropertyValue(ProfileRepository.LAST_ENTERED_EMAIL_ADDRESS, login);
              returnValue = !redirectIfPossible("/account/account.jsp", request, response);
            }
          }
        } else {
          final String errorPage = "/error.jsp";
          if (log.isLoggingDebug()) {
            log.logDebug("Error in ResetPasswordPageEvaluator:evaluate: Profile is null");
          }
          returnValue = !redirectIfPossible(errorPage, request, response);
        }
      }
    } catch (final IOException e) {
      if (log.isLoggingError()) {
        log.logError("IO Exception in ResetPasswordPageEvaluator:evaluate" + e.getMessage());
      }
    } catch (final Exception e) {
      if (log.isLoggingError()) {
        log.logError("Error in ResetPasswordPageEvaluator:evaluate" + e.getMessage());
      }
    }
    final TMSDataDictionaryUtils dataDictionaryUtils = CommonComponentHelper.getDataDictionaryUtils();
    dataDictionaryUtils.processTMSDataDictionaryAttributes(pageDefinition, null, getProfile(), getPageModel(), new TMSMessageContainer());
    return returnValue;
  }
  
  /**
   * utility: redirectIfPossible If a redirect URL is provided, redirect, otherwise return quietly
   * 
   * @param redirectURL
   * @param pRequest
   * @param pResponse
   * @return
   * @throws IOException
   */
  private boolean redirectIfPossible(final String redirectURL, final DynamoHttpServletRequest pRequest, final DynamoHttpServletResponse pResponse) throws IOException {
    if (redirectURL == null || redirectURL.length() == 0) {
      return true;
    } else {
      pResponse.sendLocalRedirect(redirectURL, pRequest);
      return false;
    }
  }
}
