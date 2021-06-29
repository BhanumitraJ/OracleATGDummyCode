package com.nm.commerce.checkout;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import atg.commerce.profile.CommerceProfileTools;
import atg.droplet.DropletException;
import atg.droplet.DropletFormException;
import atg.repository.RepositoryItem;
import atg.servlet.DynamoHttpServletRequest;
import atg.servlet.DynamoHttpServletResponse;
import atg.servlet.ServletUtil;
import atg.userprofiling.Profile;

import com.nm.commerce.NMProfile;
import com.nm.commerce.checkout.beans.ResultBean;
import com.nm.commerce.checkout.beans.ResultBeanEventType;
import com.nm.commerce.checkout.beans.ResultBeanHelper;
import com.nm.formhandler.NMProfileFormHandler;
import com.nm.sitemessages.Message;
import com.nm.sitemessages.MessageDefs;
import com.nm.utils.NmoUtils;
import com.nm.utils.RegexpUtils;

/**
 * Package access - outside of this package, methods should be accessed through CheckoutAPI.
 */
/* package */class ProfileUtil {
  
  private static ProfileUtil INSTANCE; // avoid static initialization
  public static CommerceProfileTools commProfileTools = null;
  
  // private constructor enforces singleton behavior
  private ProfileUtil() {}
  
  public static synchronized ProfileUtil getInstance() {
    INSTANCE = INSTANCE == null ? new ProfileUtil() : INSTANCE;
    return INSTANCE;
  }
  
  /**
   * getProfileUsingemail retrieves the profile with the specified email address
   * 
   * @param email
   *          - the email for the profile to pull
   * @return the profile repository item or null if none was found
   */
  public RepositoryItem getProfileUsingEmail(final String email) {
    final CommerceProfileTools profileTools = CheckoutComponents.getCommerceProfileTools();
    RepositoryItem profile = null;
    if (null != profileTools) {
      profile = profileTools.getItem(email.toUpperCase(), null);
    }
    return profile;
  }
  
  public void handleMergeSFLItems(final Set anonItems, final Profile profile) {
    if (profile != null && anonItems != null) {
      Set currentSFLItems = (Set) profile.getPropertyValue("saveForLaterCartItems");
      if (currentSFLItems == null) {
        currentSFLItems = new HashSet();
      }
      currentSFLItems.addAll(anonItems);
    }
  }
  
  /**
   * Validates the Email Address to ensure that the email does not contain invalid characters, spaces or commas, and is in a valid email address format.
   * 
   * @param email
   *          The Email Address for a user
   * @param messages
   *          - the List of messages to which errors identified here should be added.
   * @return String If this is populated then the validation found no error.
   **/
  public String validateEmailandFormat(final String email, final List messages) {
    boolean hasErrors = false;
    String invalidChars = "";
    if (email != null) {
      invalidChars = NmoUtils.invalidCharacters(email);
      hasErrors = !invalidChars.equals("") || NmoUtils.checkForSpaces(email) || email.indexOf(",") > -1;
    }
    
    if (hasErrors) {
      Message msg = MessageDefs.getMessage(MessageDefs.MSG_InvalidEmailCharacters);
      String msgText = "Your e-mail address contains invalid characters.";
      if (!invalidChars.equals("")) {
        msg = new Message(msg);
        msg.setFrgName(null);
        msgText = MessageDefs.getInvalidCharacterError(invalidChars, "e-mail");
      }
      msg.setMsgText(msgText);
      messages.add(msg);
    }
    
    if (!hasErrors && !RegexpUtils.validateEmailAddress(email)) {
      hasErrors = true;
      final Message msg = MessageDefs.getMessage(MessageDefs.MSG_InvalidEmailAddress);
      msg.setMsgText("Your e-mail address is not valid.");
      messages.add(msg);
    }
    
    return hasErrors ? null : email.toLowerCase();
  }
  
  public ResultBean handleRegistration(final NMProfileFormHandler formHandler) throws Exception {
    final ResultBean result = new ResultBean();
    
    final DynamoHttpServletRequest request = ServletUtil.getCurrentRequest();
    final DynamoHttpServletResponse response = ServletUtil.getCurrentResponse();
    formHandler.handleCreate(request, response);
    // System.out.println("Errors " + formHandler.getFormError());
    
    final ArrayList<Message> messages = new ArrayList<Message>();
    
    if (formHandler.getFormError()) {
      final Collection<?> exceptions = formHandler.getFormExceptions();
      final Iterator<?> iterator = exceptions.iterator();
      while (iterator.hasNext()) {
        Message message = null;
        final DropletException exception = (DropletException) iterator.next();
        
        String exceptionMessage = exception.getMessage();
        String errorCode = exception.getErrorCode();
/*        String propertyPathForSecurityFields = ((DropletFormException)exception).getPropertyName();*/
        
        if (errorCode != null) {
          if (errorCode.toUpperCase().indexOf("PASS") >= 0) {
            message = MessageDefs.getMessage(MessageDefs.MSG_GenericFormErrorMessage);
            message.setFieldId("password");
            message.setMsgText(exceptionMessage);
          }        
          
          if (errorCode.toUpperCase().indexOf("USERALREADYEXISTS") >= 0) {
            message = MessageDefs.getMessage(MessageDefs.MSG_EmailAlreadyRegisteredRepl);
          }
        }
        
        if (message == null) {
          message = MessageDefs.getMessage(MessageDefs.MSG_GenericFormErrorMessage);
          message.setFieldId("userName");
          message.setMsgText(exceptionMessage);
        }
        
        messages.add(message);
      }
      
    } else {
      ResultBeanHelper.login(result, (NMProfile) formHandler.getProfile(), ResultBeanEventType.REGISTERED_LOGIN);
      
    }
    result.getMessages().addAll(messages);
    return result;
  }
  
}
