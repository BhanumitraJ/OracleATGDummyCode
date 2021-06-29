package com.nm.commerce.promotion.rules;

import java.util.Set;

import com.nm.commerce.NMCommerceItem;
import com.nm.commerce.order.IOrder;

public interface Rule {
  
  public String getValue();
  
  public Set<NMCommerceItem> evaluate(IOrder order, Set<NMCommerceItem> qualifiedItems);
  
}
