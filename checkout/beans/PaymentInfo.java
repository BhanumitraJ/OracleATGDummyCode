package com.nm.commerce.checkout.beans;

public class PaymentInfo extends ContactInfo {
  public static final int NEW = 0;
  public static final int REMOVED = 1;
  
  private String paymentId;
  private int paymentType;
  private String cardNumber;
  private String cardSecCode;
  private String cardType;
  private String cardTypeCode;
  private String expMonth;
  private String expYear;
  private String payPalEmail;
  private String payPalPayerId;
  private String payPalBillingAgreementId;
  private boolean skipSecCodeValidation;
  
  public void copyFrom(PaymentInfo paymentInfo) {
    setPaymentId(paymentInfo.getPaymentId());
    setPaymentType(paymentInfo.getPaymentType());
    setCardNumber(paymentInfo.getCardNumber());
    setCardSecCode(paymentInfo.getCardSecCode());
    setCardType(paymentInfo.getCardType());
    setCardTypeCode(paymentInfo.getCardTypeCode());
    setExpMonth(paymentInfo.getExpMonth());
    setExpYear(paymentInfo.getExpYear());
    setPayPalEmail(paymentInfo.getPayPalEmail());
    setPayPalPayerId(paymentInfo.getPayPalPayerId());
    setPayPalBillingAgreementId(paymentInfo.getPayPalBillingAgreementId());
    setSkipSecCodeValidation(paymentInfo.isSkipSecCodeValidation());
    
    super.copyFrom(paymentInfo);
  }
  
  public String getPaymentId() {
    return paymentId;
  }
  
  public void setPaymentId(String paymentId) {
    this.paymentId = paymentId;
  }
  
  public String getCardNumber() {
    return cardNumber;
  }
  
  public void setCardNumber(String cardNumber) {
    this.cardNumber = cardNumber;
  }
  
  public String getCardSecCode() {
    return cardSecCode;
  }
  
  public void setCardSecCode(String cardSecCode) {
    this.cardSecCode = cardSecCode;
  }
  
  public String getCardTypeCode() {
    return cardTypeCode;
  }
  
  public void setCardTypeCode(String cardTypeCode) {
    this.cardTypeCode = cardTypeCode;
  }
  
  public String getExpMonth() {
    return expMonth;
  }
  
  public void setExpMonth(String expMonth) {
    this.expMonth = expMonth;
  }
  
  public String getExpYear() {
    return expYear;
  }
  
  public void setExpYear(String expYear) {
    this.expYear = expYear;
  }
  
  public String getCardType() {
    return cardType;
  }
  
  public void setCardType(String cardType) {
    this.cardType = cardType;
  }
  
  public boolean isSkipSecCodeValidation() {
    return skipSecCodeValidation;
  }
  
  public void setSkipSecCodeValidation(boolean skipSecCodeValidation) {
    this.skipSecCodeValidation = skipSecCodeValidation;
  }
  
  public int getPaymentType() {
    return paymentType;
  }
  
  public void setPaymentType(int paymentType) {
    this.paymentType = paymentType;
  }
  
  public String getPayPalEmail() {
    return payPalEmail;
  }
  
  public void setPayPalEmail(String payPalEmail) {
    this.payPalEmail = payPalEmail;
  }
  
  public String getPayPalPayerId() {
    return payPalPayerId;
  }
  
  public void setPayPalPayerId(String payPalPayerId) {
    this.payPalPayerId = payPalPayerId;
  }
  
  public String getPayPalBillingAgreementId() {
    return payPalBillingAgreementId;
  }
  
  public void setPayPalBillingAgreementId(String payPalBillingAgreementId) {
    this.payPalBillingAgreementId = payPalBillingAgreementId;
  }
  
}
