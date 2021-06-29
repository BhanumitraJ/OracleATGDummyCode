package com.nm.commerce.promotion.awards;

import java.util.List;
import java.util.Set;

import com.nm.commerce.NMCommerceItem;
import com.nm.commerce.order.IOrder;
import com.nm.commerce.promotion.rules.Rule;

public class BaseAwardImpl extends BaseAward {
  
  public final static String promoTypeStr = "BaseAwardImpl";
  
  public String getDisplayValue() {
    return promoTypeStr;
  }
  
  public Object getSortableValue() {
    return getValue();
  }
  
  public boolean apply(IOrder order, Set<NMCommerceItem> applicableItems, List<Rule> applicationRules, String promoKey, String promoCode) throws Exception {
    return false;
  }
}
