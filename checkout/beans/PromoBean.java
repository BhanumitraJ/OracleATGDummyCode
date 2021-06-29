package com.nm.commerce.checkout.beans;

/**
 * This class is used to pass promo info
 * 
 * @author nmve1
 * 
 */
public class PromoBean {
  
  private String promoCode;
  private String promoEmail;
  private int origin;
  private boolean promoEmailRequested;
  private boolean promoEmailRedeemed;
  private boolean initialEmailRequest;
  private int promoCount;
  
  public PromoBean() {}
  
  public String getPromoCode() {
    return promoCode;
  }
  
  public void setPromoCode(String promoCode) {
    this.promoCode = promoCode;
  }
  
  public String getPromoEmail() {
    return (promoEmail);
  }
  
  public void setPromoEmail(String promoEmail) {
    this.promoEmail = promoEmail;
  }
  
  public int getOrigin() {
    return origin;
  }
  
  public void setOrigin(int origin) {
    this.origin = origin;
  }
  
  public boolean isPromoEmailRequested() {
    return promoEmailRequested;
  }
  
  public void setPromoEmailRequested(boolean promoEmailRequested) {
    this.promoEmailRequested = promoEmailRequested;
  }
  
  public boolean isPromoEmailRedeemed() {
    return promoEmailRedeemed;
  }
  
  public void setPromoEmailRedeemed(boolean promoEmailRedeemed) {
    this.promoEmailRedeemed = promoEmailRedeemed;
  }
  
  public boolean isInitialEmailRequest() {
    return initialEmailRequest;
  }
  
  public void setInitialEmailRequest(boolean initialEmailRequest) {
    this.initialEmailRequest = initialEmailRequest;
  }
  
  public int getPromoCount() {
    return promoCount;
  }
  
  public void setPromoCount(int promoCount) {
    this.promoCount = promoCount;
  }
}
