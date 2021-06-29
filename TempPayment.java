package com.nm.commerce;

import com.nm.formhandler.checkout.OrderSubmitFormHandler;

/**
 * The TempPayment object is a small object used to store payment information in the event a customer has entered the data, but not submitted the form from the combined payment/orderreview page.
 * 
 * @author nmjjm4
 * 
 */
public class TempPayment {
  
  private String cardNumber;
  private String securityCode;
  private String expirationMonth;
  private String expirationYear;
  private String cardType;
  private boolean isSignedIntoPaypal;
  
  /**
   * The TempPayment object should only ever be created from the payment service methods. Creating the private constructor will restrict object creation outside this mechanism.
   * 
   * @see com.nm.ajax.checkoutb.services.PaymentService
   */
  private TempPayment() {
    // do not allow default object creation.
  }
  
  /**
   * This object should only be generated from the PaymentService. Since the xstream classes which create this object do not utilize the constructor it will be passed to this object which will encrypt
   * all necessary data.
   * 
   * @param obj
   */
  protected TempPayment(TempPayment obj) {
    this();
    this.cardNumber = NMCreditCard.encryptCreditCardNumber(obj.getCardNumber(false));
    this.securityCode = NMCreditCard.encryptCreditCardNumber(obj.getSecurityCode(false));
    this.expirationMonth = obj.getExpirationMonth();
    this.expirationYear = obj.getExpirationYear();
    this.cardType = obj.getCardType();
    this.isSignedIntoPaypal = obj.isSignedIntoPaypal();
  }
  
  protected void update(TempPayment obj) {
    if (obj.getCardNumber(false) != null && cardNumber != null && getMaskedCardNumber().equals(obj.getCardNumber(false))) {
      /*
       * don't do anyting. we have been sent back the masked card number which should indicate that the customer did not change the value. Leave the existing card number in the object alone.
       */
    } else {
      this.cardNumber = NMCreditCard.encryptCreditCardNumber(obj.getCardNumber(false));
    }
    
    if (obj.getSecurityCode(false) != null && securityCode != null && obj.getSecurityCode(false).indexOf("*") != -1) {
      /*
       * don't do anyting. we have been sent back the masked card number which should indicate that the customer did not change the value. Leave the existing card number in the object alone.
       */
      
    } else {
      this.securityCode = NMCreditCard.encryptCreditCardNumber(obj.getSecurityCode(false));
    }
    
    this.expirationMonth = obj.getExpirationMonth();
    this.expirationYear = obj.getExpirationYear();
    this.cardType = obj.getCardType();
    this.isSignedIntoPaypal = obj.isSignedIntoPaypal();
  }
  
  private String getCardNumber(boolean decrypt) {
    if (decrypt) {
      return getCardNumber();
    } else {
      return cardNumber;
    }
  }
  
  private String getSecurityCode(boolean decrypt) {
    if (decrypt) {
      return getSecurityCode();
    } else {
      return securityCode;
    }
  }
  
  public String getCardNumber() {
    return NMCreditCard.decryptCreditCardNumber(cardNumber);
  }
  
  public String getSecurityCode() {
    return NMCreditCard.decryptCreditCardNumber(securityCode);
  }
  
  public String getExpirationMonth() {
    return expirationMonth;
  }
  
  public String getExpirationYear() {
    return expirationYear;
  }
  
  public String getCardType() {
    return cardType;
  }
  
  public void setCardType(String cardType) {
	this.cardType = cardType;
  }

  public String getMaskedCardNumber() {
    return NMCreditCard.getMaskedCreditCardNumber(NMCreditCard.decryptCreditCardNumber(cardNumber));
  }
  
  public String getMaskedSecurityCode() {
    String mask = "";
    String code = getSecurityCode();
    if (code != null) {
      mask = code.replaceAll(".", "*");
    }
    return mask;
  }
  
  public boolean isMaskedSecurityCode(OrderSubmitFormHandler handler) {
    return handler.getCreditCardSecCode() != null && handler.getCreditCardSecCode().indexOf("*") != -1;
  }
  
  private boolean isMaskedCard(OrderSubmitFormHandler handler) {
    return NMCreditCard.getMaskedCreditCardNumber(handler.getCreditCardNumber()).equals(getMaskedCardNumber());
  }
  
  private boolean isEqualCard(OrderSubmitFormHandler handler) {
    return !isMaskedCard(handler) && handler.getCreditCardNumber().equals(this.getCardNumber(true));
  }
  
  private boolean isEqualSecurityCode(OrderSubmitFormHandler handler) {
    return !isMaskedSecurityCode(handler) && handler.getCreditCardSecCode().equals(this.getSecurityCode(true));
  }
  
  /**
   * Called from the handler. If we are pulling the card data from this object we only want to display it in the masked format.
   * 
   * @param handler
   */
  public void processStoredCard(OrderSubmitFormHandler handler) {
    if (handler.getCreditCardNumber() != null && isMaskedCard(handler)) {
      handler.setCreditCardNumber(getCardNumber());
    }
    if (handler.getCreditCardSecCode() != null && isMaskedSecurityCode(handler)) {
      handler.setCreditCardSecCode(this.getSecurityCode());
    }
  }
  
  /**
   * Called from the handler when an error is generated. If the card number has not been modified we retrieve and reset the data with the masked number. Otherwise it is assumed that the data is coming
   * from the formhandler itself and that will be populated from there.
   * 
   * @param handler
   */
  public void resetMaskedCardOnError(OrderSubmitFormHandler handler) {
    if (handler.getCreditCardNumber() != null && isEqualCard(handler)) {
      handler.setCreditCardNumber(getCardNumber());
    }
    if (handler.getCreditCardSecCode() != null && isEqualSecurityCode(handler)) {
      handler.setCreditCardSecCode(this.getSecurityCode());
    }
  }
  
  public boolean isSignedIntoPaypal() {
    return isSignedIntoPaypal;
  }
  
  public void setSignedIntoPaypal(boolean isSignedIntoPaypal) {
    this.isSignedIntoPaypal = isSignedIntoPaypal;
  }
  
}
