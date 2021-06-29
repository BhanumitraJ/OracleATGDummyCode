package com.nm.commerce.catalog;

public class NMProductPrice {
  
  private String percentDiscount;
  private String highestPrice;
  private double highestPriced;
  private String lowestPrice;
  private double lowestPriced;
  private String highPaLabel;
  private String highPaPrice;
  private double highPaPriced;
  private String highOriginalPrice;
  private String lowOriginalPrice;
  private boolean hasSuiteAdornments;
  private String originalPriceRangeLabel;
  private String adornedPriceRangeLabel;
  
  public String getPercentDiscount() {
    return percentDiscount;
  }
  
  public String getHighestPrice() {
    return highestPrice;
  }
  
  public String getLowestPrice() {
    return lowestPrice;
  }
  
  public void setLowestPrice(String lowestPrice) {
    this.lowestPrice = lowestPrice;
  }
  
  public String getHighPaLabel() {
    return highPaLabel;
  }
  
  public String getHighPaPrice() {
    return highPaPrice;
  }
  
  public boolean getHasSuiteAdornments() {
    return hasSuiteAdornments;
  }
  
  public String getHighOriginalPrice() {
    return highOriginalPrice;
  }
  
  public String getLowOriginalPrice() {
    return lowOriginalPrice;
  }
  
  public String getOriginalPriceRangeLabel() {
    return originalPriceRangeLabel;
  }
  
  public String getAdornedPriceRangeLabel() {
    return adornedPriceRangeLabel;
  }
  
  public void setPercentDiscount(String percentDiscount) {
    this.percentDiscount = percentDiscount;
  }
  
  public void setHighestPrice(String highestPrice) {
    this.highestPrice = highestPrice;
  }
  
  public void setHighPaLabel(String highPaLabel) {
    this.highPaLabel = highPaLabel;
  }
  
  public void setHighPaPrice(String highPaPrice) {
    this.highPaPrice = highPaPrice;
  }
  
  public double getHighPaPriced() {
    double hPaPrice = 0.0;
    try {
      String highPriceAdornPrice = getHighPaPrice();
      hPaPrice = Double.parseDouble(highPriceAdornPrice.trim());
    } catch (Exception e) {
      return 0.0d;
    }
    return hPaPrice;
  }
  
  public double gethighestPriced() {
    double hghPrice = 0.0;
    try {
      String highPrice = getHighestPrice();
      hghPrice = Double.parseDouble(highPrice.trim());
    } catch (Exception e) {
      return 0.0d;
    }
    return hghPrice;
  }
  
  public double getlowestPriced() {
    double lowprice = 0.0;
    try {
      String highPrice = getLowestPrice();
      lowprice = Double.parseDouble(highPrice.trim());
    } catch (Exception e) {
      return 0.0d;
    }
    return lowprice;
  }
  
  public void setHasSuiteAdornments(boolean hasSuiteAdornments) {
    this.hasSuiteAdornments = hasSuiteAdornments;
  }
  
  public void setHighOriginalPrice(String highOriginalPrice) {
    this.highOriginalPrice = highOriginalPrice;
  }
  
  public void setLowOriginalPrice(String lowOriginalPrice) {
    this.lowOriginalPrice = lowOriginalPrice;
  }
  
  public void setOriginalPriceRangeLabel(String originalPriceRangeLabel) {
    this.originalPriceRangeLabel = originalPriceRangeLabel;
  }
  
  public void setAdornedPriceRangeLabel(String adornedPriceRangeLabel) {
    this.adornedPriceRangeLabel = adornedPriceRangeLabel;
  }
  
  public void setHighPaPriced(double highPaPriced) {
    this.highPaPriced = highPaPriced;
  }
  
  public void getHighestPriced(double highestPriced) {
    this.highestPriced = highestPriced;
  }
  
  public void getLowestPriced(double lowestPriced) {
    this.lowestPriced = lowestPriced;
  }
  
  @Override
  public String toString() {
    StringBuilder prices = new StringBuilder();
    prices.append(super.toString());
    prices.append("highestPrice=" + this.highestPrice + " ; ");
    prices.append("highestPriced=" + this.highestPriced + " ; ");
    prices.append("lowestPrice=" + this.lowestPrice + " ; ");
    prices.append("lowestPriced=" + this.lowestPriced + " ; ");
    prices.append("highOriginalPrice=" + this.highOriginalPrice + " ; ");
    prices.append("lowOriginalPrice=" + this.lowOriginalPrice + " ; ");
    prices.append("highPaPrice=" + this.highPaPrice + " ; ");
    prices.append("highPaPriced=" + this.highPaPriced + " ; ");
    prices.append("highPaLabel=" + this.highPaLabel + " ; ");
    prices.append("originalPriceRangeLabel=" + this.originalPriceRangeLabel + " ; ");
    prices.append("adornedPriceRangeLabel=" + this.adornedPriceRangeLabel + " ; ");
    prices.append("hasSuiteAdornments=" + this.hasSuiteAdornments + " ; ");
    prices.append("percentDiscount=" + this.percentDiscount + " ; ");
    return prices.toString();
  }
  
}
