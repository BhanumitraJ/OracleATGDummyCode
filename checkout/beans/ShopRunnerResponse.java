package com.nm.commerce.checkout.beans;

import org.json.simple.JSONObject;

import com.nm.commerce.ShopRunnerGiftCard;

public class ShopRunnerResponse {
  
  private JSONObject response;
  private ShopRunnerGiftCard giftCard;
  private int statusCode = 0;
  
  public ShopRunnerResponse() {
    
  }
  
  public ShopRunnerResponse(JSONObject response, int statusCode) {
    this.response = response;
    this.statusCode = statusCode;
  }
  
  public JSONObject getResponse() {
    return response;
  }
  
  public void setResponse(JSONObject response) {
    this.response = response;
  }
  
  public int getStatusCode() {
    return statusCode;
  }
  
  public void setStatusCode(int statusCode) {
    this.statusCode = statusCode;
  }
  
  public ShopRunnerGiftCard getGiftCard() {
    return giftCard;
  }
  
  public void setGiftCard(ShopRunnerGiftCard giftCard) {
    this.giftCard = giftCard;
  }
  
}
