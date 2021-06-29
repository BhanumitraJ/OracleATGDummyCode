package com.nm.commerce.catalog;

public class NMPagingPrevNext {
  
  private int index;
  private int totalProducts;
  private NMProduct nextProduct;
  private NMProduct previousProduct;
  private String nextProductId;
  private String previousProductId;
  private String categoryId;
  
  public int getIndex() {
    return index;
  }
  
  public void setIndex(int index) {
    this.index = index;
  }
  
  public int getTotalProducts() {
    return totalProducts;
  }
  
  public void setTotalProducts(int totalProducts) {
    this.totalProducts = totalProducts;
  }
  
  public NMProduct getNextProduct() {
    return nextProduct;
  }
  
  public void setNextProduct(NMProduct nextProduct) {
    this.nextProduct = nextProduct;
  }
  
  public NMProduct getPreviousProduct() {
    return previousProduct;
  }
  
  public void setPreviousProduct(NMProduct previousProduct) {
    this.previousProduct = previousProduct;
  }
  
  public String getNextProductId() {
    return nextProductId;
  }
  
  public void setNextProductId(String nextProductId) {
    this.nextProductId = nextProductId;
  }
  
  public String getPreviousProductId() {
    return previousProductId;
  }
  
  public void setPreviousProductId(String previousProductId) {
    this.previousProductId = previousProductId;
  }
  
  public String getCategoryId() {
    return categoryId;
  }
  
  public void setCategoryId(String categoryId) {
    this.categoryId = categoryId;
  }
  
}
