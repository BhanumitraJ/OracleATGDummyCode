package com.nm.commerce.order;

import java.util.Set;

public interface IOrderItem extends ICommerceObject {
  
  public double getRawTotalPrice();
  
  public double getCurrentItemPrice();
  
  public String getProductId();
  
  public String getDepartment();
  
  public String getClassCode();
  
  public boolean isInStock();
  
  public boolean isSaleItem();
  
  public int getItemQuantity();
  
  public Set<String> getPromoKeys();
}
