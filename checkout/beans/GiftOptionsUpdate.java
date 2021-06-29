package com.nm.commerce.checkout.beans;

public class GiftOptionsUpdate {
  private String commerceItemId;
  private boolean giftWrapOn;
  private boolean giftWrapSeparately;
  private int giftNoteType;
  private String noteLine1;
  private String noteLine2;
  private String noteLine3;
  private String noteLine4;
  private String noteLine5;
  
  public int getGiftNoteType() {
    return giftNoteType;
  }
  
  public void setGiftNoteType(int giftNoteType) {
    this.giftNoteType = giftNoteType;
  }
  
  public boolean getGiftWrapOn() {
    return giftWrapOn;
  }
  
  public void setGiftWrapOn(boolean giftWrapOn) {
    this.giftWrapOn = giftWrapOn;
  }
  
  public boolean getGiftWrapSeparately() {
    return giftWrapSeparately;
  }
  
  public void setGiftWrapSeparately(boolean giftWrapSeparately) {
    this.giftWrapSeparately = giftWrapSeparately;
  }
  
  public String getNoteLine1() {
    return noteLine1;
  }
  
  public void setNoteLine1(String noteLine1) {
    this.noteLine1 = noteLine1;
  }
  
  public String getNoteLine2() {
    return noteLine2;
  }
  
  public void setNoteLine2(String noteLine2) {
    this.noteLine2 = noteLine2;
  }
  
  public String getNoteLine3() {
    return noteLine3;
  }
  
  public void setNoteLine3(String noteLine3) {
    this.noteLine3 = noteLine3;
  }
  
  public String getNoteLine4() {
    return noteLine4;
  }
  
  public void setNoteLine4(String noteLine4) {
    this.noteLine4 = noteLine4;
  }
  
  public String getNoteLine5() {
    return noteLine5;
  }
  
  public void setNoteLine5(String noteLine5) {
    this.noteLine5 = noteLine5;
  }
  
  public String getCommerceItemId() {
    return commerceItemId;
  }
  
  public void setCommerceItemId(String commerceItemId) {
    this.commerceItemId = commerceItemId;
  }
}
