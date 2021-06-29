package com.nm.commerce;

import static com.nm.common.INMGenericConstants.CHECKOUT;
import static com.nm.common.INMGenericConstants.CHECKOUT_TYPE;
import static com.nm.common.INMGenericConstants.COLON_STRING;
import static com.nm.common.INMGenericConstants.EMPTY_STRING;
import static com.nm.integration.MasterPassConstants.MP;
import atg.commerce.order.CreditCard;
import atg.repository.RepositoryItem;

import com.nm.profile.ProfileProperties;
import com.nm.utils.EncryptDecrypt;
import com.nm.utils.StringUtilities;

public class NMCreditCard extends CreditCard implements Comparable<NMCreditCard> {
  
  public String getCreditCardTypeDisplay() {
    final String type = super.getCreditCardType();
    if ("NEX".equals(type)) {
      return ("Gift Card");
    }
    
    return (type);
  }
  
  public String getCreditCardTypeFormatted() {
    final String type = getCreditCardTypeDisplay();
    return type != null ? type.replaceAll("[^\\w]", "").toLowerCase() : null;
  }
  
  public double getAmountRemaining() {
    return ((Double) getPropertyValue("amountRemaining")).doubleValue();
  }
  
  public void setAmountRemaining(final double amountRemaining) {
    setPropertyValue("amountRemaining", new Double(amountRemaining));
  }
  
  public String getCidEncrypted() {
    return (String) getPropertyValue("cid");
  }
  
  public String getCid() {
    return decryptCreditCardNumber((String) getPropertyValue("cid"));
  }
  
  public void setCid(final String cid) {
    setPropertyValue("cid", encryptCreditCardNumber(cid));
  }
  
  public String getCidTransient() {
    return (String) getPropertyValue("cidTransient");
  }
  
  public void setCidTransient(final String cidTransient) {
    setPropertyValue("cidTransient", cidTransient);
  }
  
  public boolean getCidAuthorized() {
    return (Boolean) getPropertyValue(ProfileProperties.CreditCard_cidAuthorized);
  }
  
  public void setCidAuthorized(final boolean cidAuthorized) {
    setPropertyValue(ProfileProperties.CreditCard_cidAuthorized, cidAuthorized);
  }
  
  public String getExpirationMonthEncrypted() {
    return super.getExpirationMonth();
  }
  
  @Override
  public String getExpirationMonth() {
    return EncryptDecrypt.decrypt(super.getExpirationMonth(), "NONE");
  }
  
  @Override
  public void setExpirationMonth(final String expirationMonth) {
    super.setExpirationMonth(encryptCreditCardNumber(expirationMonth));
  }
  
  public String getExpirationYearEncrypted() {
    return super.getExpirationYear();
  }
  
  @Override
  public String getExpirationYear() {
    return EncryptDecrypt.decrypt(super.getExpirationYear(), "NONE");
  }
  
  @Override
  public void setExpirationYear(final String expirationYear) {
    super.setExpirationYear(encryptCreditCardNumber(expirationYear));
  }
  
  public String getCreditCardNumberEncrypted() {
    return super.getCreditCardNumber();
  }
  
  // vsrb1
  // /public String getCreditCardNumber() { return decryptCreditCardNumber((String)getPropertyValue("creditCardNumber")); }
  @Override
  public String getCreditCardNumber() {
    return decryptCreditCardNumber(super.getCreditCardNumber());
  }
  
  // vsrb1
  // /public void setCreditCardNumber( String creditCardNumber ) { setPropertyValue("creditCardNumber",encryptCreditCardNumber(creditCardNumber)); }
  @Override
  public void setCreditCardNumber(final String creditCardNumber) {
    super.setCreditCardNumber(encryptCreditCardNumber(creditCardNumber));
  }
  
  /**
   * Returns a masked version of a credit card number
   */
  public String getMaskedCreditCardNumber() {
    String returnValue = null;
    final String creditCardNumber = decryptCreditCardNumber(super.getCreditCardNumber());
    returnValue = getMaskedCreditCardNumber(creditCardNumber);
    return returnValue;
  }
  
  public String getLastFourDigits() {
    return getLastFourDigits(getCreditCardNumber());
  }
  
  public static String getLastFourDigits(final String creditCardNumber) {
    if (StringUtilities.isEmpty(creditCardNumber)) {
      return creditCardNumber;
    }
    return creditCardNumber.substring(creditCardNumber.length() - 4);
  }
  
  public static String getMaskedCreditCardNumber(final String creditCardNumber) {
    String returnValue = null;
    
    if (creditCardNumber != null) {
      if (creditCardNumber.length() > 4) {
        final int cardLength = creditCardNumber.length();
        final int maskedDigits = cardLength - 4;
        String strMaskedDigits = "";
        for (int i = 0; i < maskedDigits; i++) {
          strMaskedDigits += "*";
        }
        returnValue = strMaskedDigits + creditCardNumber.substring(cardLength - 4, cardLength);
      } else {
        returnValue = creditCardNumber;
      }
    }
    
    return returnValue;
  }
  
  // vsrb1
  public static String decryptCreditCardNumber(final String encryptedString) {
    String creditCardNumber = null;
    if ((encryptedString != null) && (encryptedString.trim().length() > 0)) {
      creditCardNumber = EncryptDecrypt.decrypt(encryptedString);
    }
    if (creditCardNumber == null) {
      creditCardNumber = encryptedString;
    }
    
    return creditCardNumber;
  } // end decryptCreditCardNumber method
  
  // vsrb1
  public static String encryptCreditCardNumber(final String creditCardNumber) {
    String encryptedString = null;
    if ((creditCardNumber != null) && (creditCardNumber.trim().length() > 0)) {
      encryptedString = EncryptDecrypt.encrypt(creditCardNumber);
    } // end if
    if (encryptedString == null) {
      encryptedString = creditCardNumber;
    }
    
    return encryptedString;
  } // end encryptCreditCardNumber method
  
  public boolean isVmeCard() {
    final Boolean flag = (Boolean) getPropertyValue("vMePayment");
    return (flag != null) ? flag.booleanValue() : false;
  }
  
  public void setVmeCard(final boolean value) {
    setPropertyValue("vMePayment", new Boolean(value));
  }
  
  /**
   * @return RepositoryItem
   */
  public RepositoryItem getCheckoutType() {
    return ((RepositoryItem) getPropertyValue(CHECKOUT_TYPE));
  }
  
  /**
   * @param item
   */
  public void setCheckoutType(final RepositoryItem item) {
    setPropertyValue(CHECKOUT_TYPE, item);
  }
  
  /**
   * This method checks if the Checkout Type is MasterPass
   * 
   * @return isMasterPassCard -type boolean
   */
  public boolean isMasterPassCard() {
    boolean isMasterPassCard = false;
    if (null != getCheckoutType()) {
      final String checkoutTypeId = getCheckoutType().getRepositoryId();
      if ((null != checkoutTypeId) && checkoutTypeId.equalsIgnoreCase(MP + COLON_STRING + CHECKOUT)) {
        isMasterPassCard = true;
      }
    }
    return isMasterPassCard;
  }
  
  /**
   * This method will return the current checkout type
   * 
   * @return checkoutType
   */
  public String getCurrentCheckoutType() {
    String checkoutType = EMPTY_STRING;
    if (null != getCheckoutType()) {
      checkoutType = getCheckoutType().getRepositoryId();
    }
    return checkoutType;
  }
  
  public String getVmeCallId() {
    return (String) getPropertyValue("vMeCallId");
  }
  
  public void setVmeCallId(final String vMeCallId) {
    setPropertyValue("vMeCallId", vMeCallId);
  }
  
  public String getRiskIndicator() {
    return (String) getPropertyValue("riskIndicator");
  }
  
  public void setRiskIndicator(final String riskIndicator) {
    setPropertyValue("riskIndicator", riskIndicator);
  }
  
  public boolean isPrepaidCard() {
    final Boolean flag = (Boolean) getPropertyValue("flgPrepaidCard");
    return (flag != null) ? flag.booleanValue() : false;
  }
  
  public boolean isGiftCard() {
    final String type = getCreditCardType();
    return ((type != null) && type.equals("NEX"));
  }
  
  public boolean isPayPal() {
    final String type = getCreditCardType();
    return ((type != null) && type.equals("PayPal"));
  }
  
  public boolean isCreditCard() {
    return (!isPrepaidCard() && !isGiftCard());
  }
  
  public void setPrepaidCard(final boolean value) {
    setPropertyValue("flgPrepaidCard", new Boolean(value));
  }
  
  public double getAmountAvailable() {
    return getAmount() + getAmountRemaining();
  }
  
  public RepositoryItem getBillingAddressItem() {
    return (RepositoryItem) getPropertyValue("billingAddress");
  }
  
  public String getPayPalEmail() {
    return (String) getPropertyValue("payPalEmail");
  }
  
  public void setPayPalEmail(final String email) {
    setPropertyValue("payPalEmail", email);
  }
  
  public String getPayPalPayerId() {
    return (String) getPropertyValue("payPalPayerId");
  }
  
  public void setPayPalPayerId(final String payerId) {
    setPropertyValue("payPalPayerId", payerId);
  }
  
  public String getPayPalBillingAgreementId() {
    return (String) getPropertyValue("payPalBillingAgreementId");
  }
  
  public void setPayPalBillingAgreementId(final String billingAgreementId) {
    setPropertyValue("payPalBillingAgreementId", billingAgreementId);
  }
  
  public String getRepositoryId() {
    return getRepositoryItem().getRepositoryId();
  }
  
  public String getAccountKI() {
    return (String) getPropertyValue("accountKI");
  }
  
  public void setAccountKI(final String accountKI) {
    setPropertyValue("accountKI", accountKI);
  }
  
  @Override
  public String toString() {
    if (isPayPal()) {
      return getPayPalEmail();
    } else {
      return getMaskedCreditCardNumber();
    }
  }
  
  @Override
  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof NMCreditCard)) {
      return false;
    }
    
    final NMCreditCard otherCard = (NMCreditCard) other;
    
    if (isPayPal()) {
      if (StringUtilities.isEmpty(getPayPalEmail())) {
        return false;
      }
      
      return getPayPalEmail().equals(otherCard.getPayPalEmail());
    } else {
      if (StringUtilities.isEmpty(getCreditCardNumber())) {
        return false;
      }
      
      return getCreditCardNumber().equals(otherCard.getCreditCardNumber());
    }
  }
  
  @Override
  public int compareTo(final NMCreditCard other) {
    int result = 0;
    
    if (!StringUtilities.isEmpty(getCreditCardType()) && !StringUtilities.isEmpty(other.getCreditCardType())) {
      result = getCreditCardType().compareTo(other.getCreditCardType());
    }
    
    if ((result == 0) && !StringUtilities.isEmpty(getLastFourDigits()) && !StringUtilities.isEmpty(other.getLastFourDigits())) {
      result = getLastFourDigits().compareTo(other.getLastFourDigits());
    }
    
    if (result == 0) {
      result = getRepositoryId().compareTo(other.getRepositoryId());
    }
    
    return result;
  }
  
  private static final long serialVersionUID = -618471483453870238L;
}
