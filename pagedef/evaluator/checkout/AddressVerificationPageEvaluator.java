package com.nm.commerce.pagedef.evaluator.checkout;

import java.util.Map;

import javax.servlet.jsp.PageContext;

import com.nm.commerce.checkout.CheckoutComponents;
import com.nm.commerce.pagedef.definition.PageDefinition;
import com.nm.commerce.pagedef.model.CheckoutPageModel;
import com.nm.profile.AddressVerificationContainer;
import com.nm.profile.AddressVerificationData;
import com.nm.profile.AddressVerificationHelper;

public class AddressVerificationPageEvaluator extends CheckoutPageEvaluator {
  @Override
  public boolean evaluate(PageDefinition pageDefinition, PageContext pageContext) throws Exception {
    boolean returnValue = super.evaluate(pageDefinition, pageContext);
    
    CheckoutPageModel pageModel = (CheckoutPageModel) getPageModel();
    String verificationRequestId = getRequest().getParameter("verificationRequest");
    AddressVerificationHelper addressVerificationHelper = CheckoutComponents.getAddressVerificationHelper(getRequest());
    
    // if we did not receive a helper as a parameters (from SinglePageCheckout) then use our own.
    if (addressVerificationHelper == null) {
      addressVerificationHelper = new AddressVerificationHelper();
    }
    
    if (verificationRequestId != null) {
      AddressVerificationContainer addressVerificationContainer = addressVerificationHelper.getContainer(verificationRequestId);
      
      // if the container is found, then get the values from it that will be used by the
      // verification pages and store them in parameters.
      if (addressVerificationContainer != null) {
        // set the error redirect url to the successUrl of the form just in case something goes wrong. This
        // should cause of the bypass the verification page if an error occurs.
        // errorRedirectUrl = addressVerificationContainer.getSuccessURL();
        Map<String, AddressVerificationData> addresses = addressVerificationContainer.getAddresses();
        // System.out.println("addresses " + addresses);
        pageModel.setAddresses(addresses);
        
        // request.setParameter("addressArray", addresses);
        
        if (addresses != null) {
          pageModel.setNumberOfAddresses(addresses.size());
          // request.setParameter("numberOfAddresses", "" + addresses.size());
        }
        
        // request.setParameter("successURL", addressVerificationContainer.getSuccessURL());
        // request.setParameter("postAction", addressVerificationContainer.getPostAction());
        // pageModel.setAllowAddressRemoval(Boolean.valueOf(addressVerificationContainer.getAllowAddressRemoval()));
        pageModel.setAlwaysShowAddressHeading(Boolean.valueOf(addressVerificationContainer.getAlwaysShowAddressHeading()));
        
        int verificationType = addressVerificationContainer.getType();
        String formText;
        
        switch (verificationType) {
          case AddressVerificationContainer.ACCOUNT_VERIFICATION:
            formText = "/category/account/verifyAddress.html";
          break;
          case AddressVerificationContainer.CHECKOUT_VERIFICATION:
            formText = "/category/account/verifyShippingAddress.html";
          break;
          default:
            formText = "/category/account/verifyAddress.html";
          break;
        }
        pageModel.setFormtext(formText);
        // request.setParameter("formText", formText);
        
        // request.serviceLocalParameter("output", request, response);
        // successfulService = true;
      }
    }
    return returnValue;
  }
}
