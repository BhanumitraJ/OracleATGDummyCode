package com.nm.commerce;

import java.text.NumberFormat;

import atg.core.util.StringUtils;

public class GiftCard {
  
  private String cardNumber = "";
  private String cid = "";
  private double amountAvailable = 0;
  private boolean isValid = true;
  private int transactionStatus;
  private char programType;
  private char usageType;
  
  public static final String GIFT_CARD_CODE = "NEX";
  
  public static final int OK = 0; // NO_ERRORS
  public static final int AMOUNT_OVER_BALANCE = 1; // INVALID_CARD_NUMBER
  public static final int CANCELED = 2; // INVALID_CARD_NUMBER
  public static final int NM_MERCH_ONLY = 3; // INVALID_CARD_NUMBER
  public static final int ALREADY_ON_FILE = 4; // INVALID_CARD_NUMBER
  public static final int NOT_EQUAL_TO_INIT_VALUE = 5; // INVALID_CARD_NUMBER
  public static final int BALANCE_NOT_TRUE = 6; // INVALID_CARD_NUMBER
  public static final int NOT_BEEN_SOLD = 7; // INVALID_CARD_NUMBER
  public static final int LOST_OR_STOLEN = 8; // INVALID_CARD_NUMBER
  public static final int EXPIRED = 9; // NO_ERRORS
  public static final int INVALID_CIN = 10;// INVALID_CARD_NUMBER
  public static final int NOT_ADDING_VALUE = 11;// INVALID_CARD_NUMBER
  public static final int DIV_04_ONLY = 12;// CANNOT BE USED AT BG OLCATIONS
  public static final int DIV_05_ONLY = 13;// CANNOT BE USED AT NM OLCATIONS EXCEPT 83
  public static final int REFUND_ONLY = 14;// REFUND CARD ONLY
  public static final int CARD_NOT_ON_FILE = 44;// INVALID_CARD_NUMBER
  public static final int INVALID_TRANSACTION_TYPE = 80;// GIFT_CARD_SYSTEM_UNAVAILABLE
  public static final int HOST_COMMUNICATION_ERROR = 95;// GIFT_CARD_SYSTEM_UNAVAILABLE
  public static final int FILES_NOT_OPEN = 98;// GIFT_CARD_SYSTEM_UNAVAILABLE
  public static final int BAD_FILE = 99;// GIFT_CARD_SYSTEM_UNAVAILABLE
  public static final int INVALID_INCIRCLE_CARD = 56;// GIFT_CARD_SYSTEM_UNAVAILABLE
  
  public static final String[] MERCH_CREDIT_GC_BIN_RANGE = {"28" , "29"};
  
  public GiftCard() {}
  
  public GiftCard(GiftCard giftCard) {
    copy(giftCard);
  }
  
  public void copy(GiftCard giftCard) {
    setCardNumber(giftCard.getCardNumber());
    setCid(giftCard.getCid());
    setAmountAvailable(giftCard.getAmountAvailable());
    setIsValid(giftCard.getIsValid());
    setTransactionStatus(giftCard.getTransactionStatus());
    setProgramType(giftCard.getProgramType());
    setUsageType(giftCard.getUsageType());
  }
  
  public String toString() {
    return "<--Card Number-->" + getCardNumber() + "<--CID->" + getCid() + "<--Amount Available-->" + getAmountAvailable() + "<--isValid-->" + getIsValid() + "<--Transaction Status-->"
            + getTransactionStatus() + "<--Program Type-->" + getProgramType() + "<--Usage Type-->" + getUsageType() + "<-->";
  }
  
  public String getBinNumber() {
    if (getCardNumber() != null && getCardNumber().length() > 1) {
      return getCardNumber().substring(0, 2);
    }
    
    return "";
  }
  
  public boolean getIsMerchCreditGiftCard() {
    for (int i = 0; i < MERCH_CREDIT_GC_BIN_RANGE.length; i++) {
      if (MERCH_CREDIT_GC_BIN_RANGE[i].equals(getBinNumber())) {
        return true;
      }
    }
    
    return false;
  }
  
  public boolean getIsBlank() {
    if ((getCardNumber().trim().equals("") || getCardNumber() == null) && (getCid().trim().equals("") || getCid() == null)) {
      return true;
    } else {
      return false;
    }
  }
  
  public int getTransactionStatus() {
    return transactionStatus;
  }
  
  public void setTransactionStatus(int transactionStatus) {
    this.transactionStatus = transactionStatus;
  }
  
  public char getProgramType() {
    return programType;
  }
  
  public void setProgramType(char programType) {
    this.programType = programType;
  }
  
  public char getUsageType() {
    return usageType;
  }
  
  public void setUsageType(char usageType) {
    this.usageType = usageType;
  }
  
  public boolean getIsValid() {
    return isValid;
  }
  
  public void setIsValid(boolean isValid) {
    this.isValid = isValid;
  }
  
  public String getCardNumber() {
    return cardNumber;
  }
  
  /**
   * The setCardNumber() has been updated to strip off '-' from the card number and then set the value to payment service for verification. This has been updated as part of WR26752,06 Beta release -
   * 03/14/2011
   */
  
  public void setCardNumber(String cardNumber) {
    if (!StringUtils.isBlank(cardNumber)) {
      cardNumber = StringUtils.removeCharacters(cardNumber, "-");
    }
    this.cardNumber = cardNumber;
  }
  
  public String getMaskedCid() {
    if (!getIsBlank() && getIsValid()) {
      return "****";
    } else {
      return cid;
    }
  }
  
  public String getCid() {
    return cid;
  }
  
  public void setCid(String cid) {
    if (!cid.equals("****")) {
      this.cid = cid;
    }
  }
  
  public String getAmountAvailableFormatted() {
    final NumberFormat nf = NumberFormat.getCurrencyInstance();
    final String fmt = nf.format(getAmountAvailable());
    return (fmt);
  }
  
  public double getAmountAvailable() {
    return amountAvailable;
  }
  
  public void setAmountAvailable(double amountAvailable) {
    this.amountAvailable = amountAvailable;
  }
  
  public void clear() {
    cardNumber = "";
    cid = "";
    amountAvailable = 0;
    isValid = true;
    
  }
  
  public String getGeneratedCid() {
    return generateGiftCardCid(getCardNumber());
  }
  
  public static String generateGiftCardCid(String cardNumber) {
    int bucket1 = 0;
    int bucket2 = 0;
    int bucket3 = 0;
    int bucket4 = 0;
    
    try {
      int cardLength = cardNumber.length();
      
      for (int i = 0; i < cardLength; i++) {
        int currentNumber = Integer.parseInt(String.valueOf(cardNumber.charAt(i)));
        
        // bucket1
        bucket1 += currentNumber;
        
        // bucket2
        if (i % 2 == 0) {
          if (currentNumber == 0) {
            bucket2 += 9;
          } else {
            bucket2 += currentNumber;
          }
        }
        
        // bucket3
        if (i % 2 != 0) {
          if (currentNumber == 0) {
            bucket3 += 7;
          } else {
            bucket3 += currentNumber;
          }
        }
        
        // bucket4
        bucket4 += bucket1 + bucket2 + bucket3;
      }// for
      
      return String.valueOf(bucket1 % 10) + String.valueOf(bucket2 % 10) + String.valueOf(bucket3 % 10) + String.valueOf(bucket4 % 10);
      
    } catch (Exception e) {
      return "";
    }
  }
  
}
