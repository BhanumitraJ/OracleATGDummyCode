package com.nm.commerce.checkout.beans;

import java.util.Locale;

import com.nm.commerce.GiftCardHolder;
import com.nm.utils.PCSSessionData;
import com.nm.utils.bing.BingSessionData;

public class OrderInfo {
  private PCSSessionData pcsSessionData;
  private BingSessionData bingSessionData;
  private GiftCardHolder giftCardHolder;
  private Locale userLocale;
  private String clientDateString;
  private String clientTimeZone;
  private String fraudnetBrowserData;
  private String originationCode;
  private String linkShareSiteId;
  private String linkShareTimeStamp;
  private String creditCardSecCode;
  private boolean srExpressCheckOut = false;
  private boolean mktOptIn;  
  private boolean alipayExpressCheckout = false;
  private String mid;	
	public String getMid() {
		return mid;
	}

	public void setMid( String mid ) {
		this.mid = mid;
	}
  public PCSSessionData getPcsSessionData() {
    return pcsSessionData;
  }
  
  public void setPcsSessionData(PCSSessionData pcsSessionData) {
    this.pcsSessionData = pcsSessionData;
  }
  
  public BingSessionData getBingSessionData() {
    return bingSessionData;
  }
  
  public void setBingSessionData(BingSessionData bingSessionData) {
    this.bingSessionData = bingSessionData;
  }
  
  public Locale getUserLocale() {
    return userLocale;
  }
  
  public void setUserLocale(Locale userLocale) {
    this.userLocale = userLocale;
  }
  
  public String getClientDateString() {
    return clientDateString;
  }
  
  public void setClientDateString(String clientDateString) {
    this.clientDateString = clientDateString;
  }
  
  public String getClientTimeZone() {
    return clientTimeZone;
  }
  
  public void setClientTimeZone(String clientTimeZone) {
    this.clientTimeZone = clientTimeZone;
  }
  
  public String getFraudnetBrowserData() {
    return fraudnetBrowserData;
  }
  
  public void setFraudnetBrowserData(String fraudnetBrowserData) {
    this.fraudnetBrowserData = fraudnetBrowserData;
  }
  
  public String getOriginationCode() {
    return originationCode;
  }
  
  public void setOriginationCode(String originationCode) {
    this.originationCode = originationCode;
  }
  
  public String getLinkShareSiteId() {
    return linkShareSiteId;
  }
  
  public void setLinkShareSiteId(String linkShareSiteId) {
    this.linkShareSiteId = linkShareSiteId;
  }
  
  public String getLinkShareTimeStamp() {
    return linkShareTimeStamp;
  }
  
  public void setLinkShareTimeStamp(String linkShareTimeStamp) {
    this.linkShareTimeStamp = linkShareTimeStamp;
  }
  
  public GiftCardHolder getGiftCardHolder() {
    return giftCardHolder;
  }
  
  public void setGiftCardHolder(GiftCardHolder giftCardHolder) {
    this.giftCardHolder = giftCardHolder;
  }
  
  public String getCreditCardSecCode() {
    return creditCardSecCode;
  }
  
  public void setCreditCardSecCode(String creditCardSecCode) {
    this.creditCardSecCode = creditCardSecCode;
  }
  
  /**
   * Is ShopRunner express checkout order
   * 
   * @return
   */
  public boolean isSRExpressCheckOut() {
    return srExpressCheckOut;
  }
  
  /**
   * Is Alipay express Checkout order
   * 
   * @return boolean
   */
  public boolean isAlipayExpressCheckout() {
    return alipayExpressCheckout;
  }
  
  /**
   * set Alipay express checkout
   * 
   * @param alipayExpressCheckout
   */
  public void setAlipayExpressCheckout(boolean alipayExpressCheckout) {
    this.alipayExpressCheckout = alipayExpressCheckout;
  }
  
  
  /**
   * Set ShopRunner express checkout
   * 
   * @param srExpressCheckOut
   */
  public void setSRExpressCheckOut(boolean srExpressCheckOut) {
    this.srExpressCheckOut = srExpressCheckOut;
  }
  
  public boolean isMktOptIn() {
    return mktOptIn;
  }
  
  public void setMktOptIn(boolean mktOptIn) {
    this.mktOptIn = mktOptIn;
  }
  
}
