package com.nm.commerce.checkout.beans;

import java.util.List;

/**
 * General information about gift wrap options. Note that full price does not include any promotional discounts.
 */
public class GiftWrapInfo {
  private double fullPrice;
  private List<String> messages;
  
  public double getFullPrice() {
    return fullPrice;
  }
  
  public void setFullPrice(double fullPrice) {
    this.fullPrice = fullPrice;
  }
  
  public List<String> getMessages() {
    return messages;
  }
  
  public void setMessages(List<String> messages) {
    this.messages = messages;
  }
}
