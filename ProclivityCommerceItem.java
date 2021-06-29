package com.nm.commerce;

public class ProclivityCommerceItem {
  
  public static enum UpdateType {
    ADD, REMOVE
  }
  
  private String qty;
  private String productId;
  private String sku;
  private String price;
  private UpdateType updateType;
  
  public String getQty() {
    return qty;
  }
  
  public void setQty(String qty) {
    this.qty = qty;
  }
  
  public String getProductId() {
    return productId;
  }
  
  public void setProductId(String productId) {
    this.productId = productId;
  }
  
  public String getSku() {
    return sku;
  }
  
  public void setSku(String sku) {
    this.sku = sku;
  }
  
  public String getPrice() {
    return price;
  }
  
  public void setPrice(String price) {
    this.price = price;
  }
  
  public UpdateType getUpdateType() {
    return updateType;
  }
  
  public void setUpdateType(UpdateType updateType) {
    this.updateType = updateType;
  }
  
}
