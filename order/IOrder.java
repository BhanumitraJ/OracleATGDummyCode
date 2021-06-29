package com.nm.commerce.order;

import java.util.List;

import com.nm.commerce.NMCommerceItem;

public interface IOrder extends ICommerceObject {
  
  public List<NMCommerceItem> getOrderItems();
  
  public List<NMCommerceItem> getPromoEligibleOrderItems();
}
