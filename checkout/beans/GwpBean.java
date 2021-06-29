package com.nm.commerce.checkout.beans;

import java.util.ArrayList;
import java.util.List;

import com.nm.commerce.catalog.NMProduct;

/**
 * Represents a GWP, GWP select, or PWP promotion. These are passed to and returned from the CheckoutAPI interface.
 */
public class GwpBean {
  public static final int REGULAR_TYPE = 0;
  public static final int SELECT_TYPE = 1;
  public static final int PWP_TYPE = 2;
  
  private int type;
  private String promoKey;
  private String name;
  private String productId;
  private String details;
  private int itemCount;
  private List<NMProduct> products = new ArrayList<NMProduct>();
  private List<Selection> selections = new ArrayList<Selection>();
  
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
  
  public String getName() {
    return name;
  }
  
  public void setName(String name) {
    this.name = name;
  }
  
  public String getProductId() {
    return productId;
  }
  
  public void setProductId(String productId) {
    this.productId = productId;
  }
  
  public String getDetails() {
    return details;
  }
  
  public void setDetails(String details) {
    this.details = details;
  }
  
  public int getItemCount() {
    return itemCount;
  }
  
  public void setItemCount(int itemCount) {
    this.itemCount = itemCount;
  }
  
  public List<NMProduct> getProducts() {
    return products;
  }
  
  public void setProducts(List<NMProduct> products) {
    this.products = products;
  }
  
  public List<Selection> getSelections() {
    return selections;
  }
  
  public void addSelection(Selection selection) {
    selections.add(selection);
  }
  
  public static class Selection {
    private String productId;
    private String size;
    private String color;
    
    public Selection(String productId) {
      this.productId = productId;
    }
    
    public String getProductId() {
      return productId;
    }
    
    public String getSize() {
      return size;
    }
    
    public void setSize(String size) {
      this.size = size;
    }
    
    public String getColor() {
      return color;
    }
    
    public void setColor(String color) {
      this.color = color;
    }
    
    public String toString() {
      return "Selection: " + productId + " " + size + " " + color;
    }
  }
}
