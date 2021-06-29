package com.nm.commerce;

import org.json.simple.JSONObject;

public class ShopRunnerGiftCard {
  
  String number;
  String code;
  Double giftCardBalance;
  Double newOrderBalance;
  JSONObject messageObject;
  
  public ShopRunnerGiftCard() {
    this(null, null, null, null, null);
  }
  
  public ShopRunnerGiftCard(String number, String code, Double giftCardBalance, Double newOrderBalance, JSONObject messageObject) {
    this.number = number;
    this.code = code;
    this.giftCardBalance = giftCardBalance;
    this.newOrderBalance = newOrderBalance;
    this.messageObject = messageObject;
  }
  
  public String getNumber() {
    return number;
  }
  
  public void setNumber(String number) {
    this.number = number;
  }
  
  public String getCode() {
    return code;
  }
  
  public void setCode(String code) {
    this.code = code;
  }
  
  public Double getGiftCardBalance() {
    return giftCardBalance;
  }
  
  public void setGiftCardBalance(Double giftCardBalance) {
    this.giftCardBalance = giftCardBalance;
  }
  
  public Double getNewOrderBalance() {
    return newOrderBalance;
  }
  
  public void setNewOrderBalance(Double newOrderBalance) {
    this.newOrderBalance = newOrderBalance;
  }
  
  public JSONObject getMessageObject() {
    return messageObject;
  }
  
  public void setMessageObject(JSONObject messageObject) {
    this.messageObject = messageObject;
  }
  
}
