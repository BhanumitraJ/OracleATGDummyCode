package com.nm.commerce.checkout.beans;

import java.util.Map;

public class CommerceItemUpdate {
  public static final int NO_VALUE = -1;
  public static final int NO_INTERVAL = 0;
  
  private String commerceItemId;
  private int quantity;
  private int perishableDay;
  private int perishableMonth;
  private int perishableYear;
  private String color;
  private int interval;
  private String size;
  private String skuId;
  private String dynamicImageUrl;
  private Map<String, String> siCodes;
  private String monogramOption;
  
  public int getQuantity() {
    return quantity;
  }
  
  public void setQuantity(int quantity) {
    this.quantity = quantity;
  }
  
  public String getColor() {
    return color;
  }
  
  public void setColor(String color) {
    this.color = color;
  }
  
  public int getInterval() {
    return interval;
  }
  
  public void setInterval(int interval) {
    this.interval = interval;
  }
  
  public String getSize() {
    return size;
  }
  
  public void setSize(String size) {
    this.size = size;
  }
  
  public String getSkuId() {
    return skuId;
  }
  
  public void setSkuId(String skuId) {
    this.skuId = skuId;
  }
  
  public int getPerishableDay() {
    return perishableDay;
  }
  
  public void setPerishableDay(int perishableDay) {
    this.perishableDay = perishableDay;
  }
  
  public int getPerishableMonth() {
    return perishableMonth;
  }
  
  public void setPerishableMonth(int perishableMonth) {
    this.perishableMonth = perishableMonth;
  }
  
  public int getPerishableYear() {
    return perishableYear;
  }
  
  public void setPerishableYear(int perishableYear) {
    this.perishableYear = perishableYear;
  }
  
  public void setCommerceItemId(String commerceItemId) {
    this.commerceItemId = commerceItemId;
  }
  
  public String getCommerceItemId() {
    return commerceItemId;
  }

  /**
   * @return the dynamicImageUrl
   */
  public String getDynamicImageUrl() {
    return dynamicImageUrl;
  }

  /**
   * @param dynamicImageUrl the dynamicImageUrl to set
   */
  public void setDynamicImageUrl(String dynamicImageUrl) {
    this.dynamicImageUrl = dynamicImageUrl;
  }

  public Map<String, String> getSiCodes() {
    return siCodes;
  }

  public void setSiCodes(Map<String, String> siCodes) {
    this.siCodes = siCodes;
  }
  
  /**
   * @return the monogramOption
   */
  public String getMonogramOption() {
    return monogramOption;
  }
  
  /**
   * @param monogramOption
   *          the monogramOption to set
   */
  public void setMonogramOption(String monogramOption) {
    this.monogramOption = monogramOption;
  }
}
