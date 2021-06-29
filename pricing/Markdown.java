package com.nm.commerce.pricing;

import atg.repository.RepositoryItem;

public class Markdown {
  private int type;
  private String percentDiscount;
  private double dollarDiscount;
  private String notes;
  private String promoKey;
  private String userEnteredPromoCode;
  private String details = "";
  private boolean showDetails = false;
  
  public final static int PERCENT_OFF = 0;
  public final static int DOLLAR_OFF = 1;
  
  public Markdown() {}
  
  public Markdown(String promoKeyIn, String promoCode, int typeIn, double dollarDiscountIn, String percentDiscountIn, RepositoryItem adPromo) {
    promoKey = promoKeyIn;
    userEnteredPromoCode = promoCode;
    type = typeIn;
    dollarDiscount = dollarDiscountIn;
    percentDiscount = percentDiscountIn;
    collectDetails(adPromo);
  }
  
  public void collectDetails(RepositoryItem adPromo) {
    if (adPromo == null) return;
    try {
      showDetails = ((Boolean) adPromo.getPropertyValue("detailsDisplayFlag")).booleanValue();
    } catch (Exception e) {}
    try {
      details = (String) adPromo.getPropertyValue("detailsHtml");
    } catch (Exception e) {}
  }
  
  public double getDollarDiscount() {
    return dollarDiscount;
  }
  
  public void setDollarDiscount(double dollarDiscount) {
    this.dollarDiscount = dollarDiscount;
  }
  
  public String getNotes() {
    return notes;
  }
  
  public void setNotes(String notes) {
    this.notes = notes;
  }
  
  public String getPercentDiscount() {
    return percentDiscount;
  }
  
  public void setPercentDiscount(String percentDiscount) {
    this.percentDiscount = percentDiscount;
  }
  
  public int getType() {
    return type;
  }
  
  public void setType(int type) {
    this.type = type;
  }
  
  public String getPromoKey() {
    return promoKey;
  }
  
  public void setPromoKey(String promoKey) {
    this.promoKey = promoKey;
  }
  
  public String getUserEnteredPromoCode() {
    return userEnteredPromoCode;
  }
  
  public void setUserEnteredPromoCode(String userEnteredPromoCode) {
    this.userEnteredPromoCode = userEnteredPromoCode;
  }
  
  public String getDetails() {
    return com.nm.search.endeca.NMSearchResult.forHTML(details);
  }
  
  public boolean isShowDetails() {
    return showDetails;
  }
  
  public boolean getShowDetails() {
    return showDetails;
  }
  
  public void setDetails(String details) {
    this.details = details;
  }
  
  public void setShowDetails(boolean showDetails) {
    this.showDetails = showDetails;
  }
}
