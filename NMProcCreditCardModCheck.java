/*
 * <ATGCOPYRIGHT> Copyright (C) 2001 Art Technology Group, Inc. All Rights Reserved. No use, copying or distribution of this work may be made except in accordance with a valid license agreement from
 * Art Technology Group. This notice must be included on all copies, modifications and derivatives of this work.
 * 
 * Art Technology Group (ATG) MAKES NO REPRESENTATIONS OR WARRANTIES ABOUT THE SUITABILITY OF THE SOFTWARE, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, OR NON-INFRINGEMENT. ATG SHALL NOT BE LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR DISTRIBUTING THIS SOFTWARE OR
 * ITS DERIVATIVES.
 * 
 * "Dynamo" is a trademark of Art Technology Group, Inc. </ATGCOPYRIGHT>
 */

package com.nm.commerce;

import atg.commerce.order.processor.*;
import atg.service.pipeline.*;
import atg.commerce.order.*;
import atg.payment.creditcard.*;
import atg.core.util.ResourceUtils;
import com.nm.commerce.order.*;
import com.nm.commerce.promotion.NMOrderImpl;
import com.nm.utils.StringUtilities;

import java.util.*;

/**
 * This class extends the default ATG processor to provide the functionality for accepting Nieman Marcus credit card which does not require any validation...
 * 
 * @author Sunil Bindal & Chee Chien Loo
 */
public class NMProcCreditCardModCheck extends ProcCreditCardModCheck {
  
  static final String MY_RESOURCE_NAME = "atg.commerce.order.OrderResources";
  static final String CHINA_UNION_PAY = "China UnionPay";
  
  /** Resource Bundle **/
  private static java.util.ResourceBundle sResourceBundle = java.util.ResourceBundle.getBundle(MY_RESOURCE_NAME, java.util.Locale.getDefault());
  
  private final int SUCCESS = 1;
  
  // -----------------------------------------------
  public NMProcCreditCardModCheck() {}
  
  // -----------------------------------------------
  /**
   * Returns the valid return codes 1 - The processor completed
   * 
   * @return an integer array of the valid return codes.
   */
  public int[] getRetCodes() {
    int[] ret = {SUCCESS};
    return ret;
  }
  
  // -----------------------------------------------
  /**
   * This method executes simple credit card validation. If it succeeds SUCCESS is returned, otherwise STOP_CHAIN_EXECUTION_AND_ROLLBACK is returned. The credit card validation which occurs is the
   * functionality in the verifyCreditCard() method in CreditCardTools.
   * 
   * This method requires that an Order object be supplied in pParam in a HashMap. Use the PipelineConstants class' static members to key the objects in the HashMap.
   * 
   * @param pParam
   *          a HashMap which must contain an Order object
   * @param pResult
   *          a PipelineResult object which stores any information which must be returned from this method invokation
   * @return an integer specifying the processor's return code
   * @exception Exception
   *              throws any exception back to the caller
   * @see atg.service.pipeline.PipelineProcessor#runProcess(Object, PipelineResult)
   * @see atg.payment.creditcard.CreditCardTools#verifyCreditCard(CreditCardInfo)
   */
  public int runProcess(Object pParam, PipelineResult pResult) throws Exception {
    HashMap map = (HashMap) pParam;
    Order order = (Order) map.get(PipelineConstants.ORDER);
    NMOrderManager orderManager = (NMOrderManager) map.get(PipelineConstants.ORDERMANAGER);
    
    if (order == null) throw new InvalidParameterException(ResourceUtils.getMsgResource("InvalidOrderParameter", MY_RESOURCE_NAME, sResourceBundle));
    String cardId = new String();
    // this is an outputparam for error conditions.
    // It corresponds to the payment group id
    
    int status = verifyCreditCard(order, orderManager, cardId);
    if (status != CreditCardTools.SUCCESS) {
      ((NMOrderImpl) order).incrementFailedCCAttemptCount();
      
      if (!((NMOrderImpl) order).hasReachedFailedCCAttemptLimit()) {
        if (map.containsKey(PipelineConstants.LOCALE)) {
          Locale locale = (Locale) map.get(PipelineConstants.LOCALE);
          addHashedError(pResult, PipelineConstants.CREDITCARDVERIFYFAILED, cardId, CreditCardTools.getStatusCodeMessage(status, locale));
        } else {
          addHashedError(pResult, PipelineConstants.CREDITCARDVERIFYFAILED, cardId, CreditCardTools.getStatusCodeMessage(status));
        }
      } else {
        // do nothing in this case, we want to allow the cc to be accepted even if it is an
        // invalid number, the idea being that the order will be submitted and LP will flag
        // it for more investigation
      }
    }
    if (pResult.hasErrors()) {
      if (isLoggingDebug()) {
        Object[] errors = pResult.getErrors();
        for (int i = 0; i < errors.length; i++) {
          logDebug("CreditCardError:  " + errors[i]);
        }
      }
      return STOP_CHAIN_EXECUTION_AND_ROLLBACK;
    }
    return SUCCESS;
  }
  
  // Validate Unecrypted Credit Card Information (Neiman Marcus, Bergdorf & JBC)
  // Used for Adding/Changing account credit card information
  public int validateSpecialCardUnencrypted(GenericCreditCardInfo pCreditCard) {
    
    return validateCard(pCreditCard, pCreditCard.getCreditCardNumber());
    
  }
  
  // Validate Credit Card (Neiman Marucs & JBC)
  //
  public int validateSpecialCard(GenericCreditCardInfo pCreditCard) {
    
    String cardNumber = NMCreditCard.decryptCreditCardNumber(pCreditCard.getCreditCardNumber());
    // vsrb1 for cc# encryption
    
    return validateCard(pCreditCard, cardNumber);
  }
  
  //
  public int validateCard(GenericCreditCardInfo pCreditCard, String cardNumber) {
    
    int cardExpirationYear = 0;
    int cardExpirationMonth = 0;
    
    java.util.Calendar now = java.util.Calendar.getInstance();
    int year = now.get(Calendar.YEAR);
    // convert month from 0-11 to 1-12
    int month = now.get(Calendar.MONTH) + 1;
    // Convert year and month to integer values
    
    if (pCreditCard.getCreditCardType().equals("Neiman Marcus")) {
      if (cardNumber == null) {
        return 6;
      }
      if (cardNumber.substring(0, 4).equals("0000")) {
        cardNumber = cardNumber.substring(4, cardNumber.length());
      }
      for (int i = 0; i < cardNumber.length(); i++) {
        char c = cardNumber.charAt(i);
        if ((c < '0' || c > '9')) {
          return 2;
        }
      }
      if (cardNumber.length() == 12) {
        if (!cardNumber.substring(0, 2).equals("04")) {
          return 5;
        }
      }
      
      cardNumber = computeNeimanMarcusNumber(cardNumber);
      if (!((cardNumber.length() == 12))) {
        return 5;
      }
      
      if (getChecksum(cardNumber, false) != 0) {
        return 5;
      }
      
    } else if (pCreditCard.getCreditCardType().equals("Bergdorf Goodman")) {
      if (cardNumber == null) {
        return 6;
      }
      
      for (int i = 0; i < cardNumber.length(); i++) {
        char c = cardNumber.charAt(i);
        if ((c < '0' || c > '9')) {
          return 2;
        }
      }
      if (cardNumber.length() == 12) {
        if (!cardNumber.substring(0, 2).equals("05")) {
          return 5;
        }
      }
      
      cardNumber = computeNeimanMarcusNumber(cardNumber);
      if (!((cardNumber.length() == 12))) {
        return 5;
      }
      
      if (getChecksum(cardNumber, false) != 0) {
        return 5;
      }
      
    } else if (pCreditCard.getCreditCardType().equals("Japanese Credit Bank")) {
      
      try {
        cardExpirationYear = Integer.parseInt(pCreditCard.getExpirationYear());
      } catch (NumberFormatException exc) {
        return 7;
      }
      try {
        cardExpirationMonth = Integer.parseInt((String) pCreditCard.getExpirationMonth());
      } catch (NumberFormatException exc) {
        return 7;
      }
      
      // 3. Check that the card has not expired
      if (cardExpirationYear < year) {
        return 7;
      }
      if (cardExpirationYear == year) {
        if (cardExpirationMonth < month) {
          return 7;
        }
      }
      
      if (cardNumber == null) {
        return 6;
      }
      
      for (int i = 0; i < cardNumber.length(); i++) {
        char c = cardNumber.charAt(i);
        if ((c < '0' || c > '9')) {
          return 2;
        }
      }
      
      if (cardNumber.length() != 16) {
        return 5;
      }
      if (!cardNumber.substring(0, 2).equals("35")) {
        return 5;
      }
      if (getChecksum(cardNumber, false) != 0) {
        return 5;
      }
    }// validations done for china unionpay card type
    else if (StringUtilities.areEqualIgnoreCase(pCreditCard.getCreditCardType(), CHINA_UNION_PAY)) {
      try {
        cardExpirationYear = Integer.parseInt(pCreditCard.getExpirationYear());
      } catch (NumberFormatException exc) {
        return CreditCardTools.CARD_EXP_DATE_NOT_VALID;
      }
      try {
        cardExpirationMonth = Integer.parseInt((String) pCreditCard.getExpirationMonth());
      } catch (NumberFormatException exc) {
        return CreditCardTools.CARD_EXP_DATE_NOT_VALID;
      }
      
      // 3. Check that the card has not expired
      if (cardExpirationYear < year) {
        return CreditCardTools.CARD_EXPIRED;
      }
      if (cardExpirationYear == year) {
        if (cardExpirationMonth < month) {
          return CreditCardTools.CARD_EXPIRED;
        }
      }
      
      if (cardNumber == null) {
        return CreditCardTools.CARD_NUMBER_NOT_VALID;
      }
      
      for (int i = 0; i < cardNumber.length(); i++) {
        char c = cardNumber.charAt(i);
        if ((c < '0' || c > '9')) {
          return CreditCardTools.CARD_NUMBER_HAS_INVALID_CHARS;
        }
      }
      
      if (cardNumber.length() != 16) {
        return CreditCardTools.CARD_LENGTH_NOT_VALID;
      }
      /*
       * if (!cardNumber.substring(0, 2).equals("62")) { return CreditCardTools.CARD_NUMBER_DOESNT_MATCH_TYPE; }
       */
    }
    return CreditCardTools.SUCCESS;
    
  }
  
  // Returns a string with only digits in it
  private static String getDigitsOnly(String s) {
    StringBuffer digitsOnly = new StringBuffer();
    char c;
    for (int i = 0; i < s.length(); i++) {
      c = s.charAt(i);
      if (Character.isDigit(c)) {
        digitsOnly.append(c);
      }
    }
    return digitsOnly.toString();
  }
  
  // Validation Algorithm:(Luhn Formula)
  // 1. Prefix card number with 0's to produce 16 digit number only when asked
  // 2. Multiple the odd numbers by 1.
  // 3. Multiply the even numbers by 2. If the result is greater than 9, add the
  // two digits together to get a single digit (or subtract 9)
  // 4. Add up the digits from the multiplication steps.
  // 5. The checksum is the sum of all digits MOD 10
  public static int getChecksum(String cardNumber, boolean prependZeros) {
    if (prependZeros) {
      String prefix = "";
      for (int i = cardNumber.length(); i < 16; i++)
        prefix += "0";
      cardNumber = prefix + cardNumber;
    }
    
    // Even if validation is done before hand to ensure the cardnumber only has digits we are stripping
    // non digits out to ensure a clean run, since other methods may call this
    String digitsOnly = getDigitsOnly(cardNumber);
    int sum = 0;
    int digit = 0;
    int addend = 0;
    boolean timesTwo = false;
    
    for (int i = digitsOnly.length() - 1; i >= 0; i--) {
      digit = Integer.parseInt(digitsOnly.substring(i, i + 1));
      if (timesTwo) {
        addend = digit * 2;
        if (addend > 9) {
          addend -= 9;
        }
      } else {
        addend = digit;
      }
      sum += addend;
      timesTwo = !timesTwo;
    }
    
    return (sum % 10);
    
  }
  
  // Validation Algorithm:
  // 1. Prefix card number with 0's to produce 16 digit number
  // 2. Multiple the odd numbers by 1.
  // 3. Multiply the even numbers by 2. If the result is greater than 9, add the
  // two digits together to get a single digit (or subtract 9)
  // 4. Add up the digits from the multiplication steps.
  // 5. The checksum is the sum of all digits MOD 10
  private static int getChecksumConvert(String number, boolean prependZeros) {
    if (prependZeros) {
      String prefix = "";
      for (int i = number.length(); i < 16; i++)
        prefix += "0";
      number = prefix + number;
    }
    int checkSum = 0;
    for (int i = 0; i < number.length(); i++) {
      int digitVal = Character.getNumericValue(number.charAt(i));
      if ((i % 2) == 0) // multiply even offset numbers by 2
        digitVal *= 2;
      if (digitVal >= 10) digitVal -= 9;
      checkSum += digitVal;
    }
    return (checkSum % 10);
  }
  
  public int verifyCreditCard(Order order, NMOrderManager orderManager, String cardId) {
    if (orderManager.getCurrentOrderAmountRemaining(order) == 0.0d)
    // we don't process these
      return CreditCardTools.SUCCESS;
    Iterator iter = order.getPaymentGroups().iterator();
    while (iter.hasNext()) {
      PaymentGroup pg = (PaymentGroup) iter.next();
      if (!(pg instanceof NMCreditCard)) // why is this here?
        continue;
      CreditCard card = (CreditCard) pg;
      if (card.getCreditCardType().equals("NEX") || card.getCreditCardType().equals("PayPal")) return CreditCardTools.SUCCESS;
      if (card.getCreditCardType().equals("Bergdorf Goodman") || card.getCreditCardType().equals("Neiman Marcus") || card.getCreditCardType().equals("Japanese Credit Bank")) {
        return validateNeimansCard(card);
      }
      return CreditCardTools.verifyCreditCard((CreditCardInfo) pg);
    }
    return CreditCardTools.SUCCESS;
  }
  
  private int validateNeimansCard(CreditCard card) {
    GenericCreditCardInfo CreditCard = new GenericCreditCardInfo();
    CreditCard.setBillingAddress(new atg.core.util.Address());
    CreditCard.setCreditCardNumber((String) card.getPropertyValue("creditCardNumber"));
    CreditCard.setCreditCardType(card.getCreditCardType());
    CreditCard.setExpirationMonth((String) card.getPropertyValue("expirationMonth"));
    CreditCard.setExpirationYear((String) card.getPropertyValue("expirationYear"));
    return validateSpecialCard(CreditCard);
  }
  
  // Convert a 9 or 10 digit Neiman Marcus number to a 12 or 13 digit one
  // Steps:
  // 1. Strip out the 10th digit (if available)
  // 1a. Validate the remaining 9 digit number
  // 2. Prepend "04" and compute the checksum digit.
  // 4. If the checksum zero, then use 0. Any other number must be subtracted
  // from 10 to get the check digit.
  // 3. Reconstruct the 12 or 13 digit number from 04 + number + checksum
  
  public static String computeNeimanMarcusNumber(String number) {
    if (number.length() < 9 || number.length() > 10) return number;
    
    // see if the 9 digit number is valid
    if (getChecksum(number.substring(0, 9), true) != 0)
    // if the 9-digit number is not valid, simply return it
      return number;
    
    // compute the 12 digit number
    String suffix = "";
    if (number.length() == 10) suffix = number.substring(9, 10);
    number = "04" + number.substring(0, 9);
    // compute checksum (0-9) using the 10 digits in the number
    int checksum = getChecksumConvert(number, false);
    return number + ((10 - checksum) % 10) + suffix;
  }
  
  // Convert a 9 or 10 digit Bergdorf Goodman number to a 12 or 13 digit one
  // Steps:
  // 1. Strip out the 10th digit (if available)
  // 1a. Validate the remaining 9 digit number
  // 2. Prepend "05" and compute the checksum digit.
  // 4. If the checksum zero, then use 0. Any other number must be subtracted
  // from 10 to get the check digit.
  // 3. Reconstruct the 12 or 13 digit number from 04 + number + checksum
  
  public static String computeBergdorfGoodmanNumber(String number) {
    if (number.length() < 9 || number.length() > 10) return number;
    
    // see if the 9 digit number is valid
    if (getChecksum(number.substring(0, 9), true) != 0)
    // if the 9-digit number is not valid, simply return it
      return number;
    
    // compute the 12 digit number
    String suffix = "";
    if (number.length() == 10) suffix = number.substring(9, 10);
    number = "05" + number.substring(0, 9);
    // compute checksum (0-9) using the 10 digits in the number
    int checksum = getChecksumConvert(number, false);
    return number + ((10 - checksum) % 10) + suffix;
  }
}
