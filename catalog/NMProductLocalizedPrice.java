package com.nm.commerce.catalog;

public class NMProductLocalizedPrice {
  private String currencyCode;
  private Double priceValue;
  
  public String getCurrencyCode() {
    return currencyCode;
  }
  
  public void setCurrencyCode(String currencyCode) {
    this.currencyCode = currencyCode;
  }
  
  public Double getPriceValue() {
    return priceValue;
  }
  
  public void setPriceValue(Double priceValue) {
    this.priceValue = priceValue;
  }
  
}
