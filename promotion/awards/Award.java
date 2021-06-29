package com.nm.commerce.promotion.awards;

import java.util.List;
import java.util.Set;

import com.nm.commerce.NMCommerceItem;
import com.nm.commerce.order.IOrder;
import com.nm.commerce.promotion.rules.Rule;

public interface Award {
  
  public boolean apply(IOrder order, Set<NMCommerceItem> applicableItems, List<Rule> applicationRules, String promoKey, String promoCode) throws Exception;
  
  public String getValue();
  
  public int getType();
  
  public Object getSortableValue();
  
}
