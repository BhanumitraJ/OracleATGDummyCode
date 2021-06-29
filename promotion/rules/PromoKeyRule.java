package com.nm.commerce.promotion.rules;

import java.util.Set;

import com.nm.commerce.order.ICommerceObject;
import com.nm.commerce.order.IOrderItem;

public class PromoKeyRule extends ItemRule {
  
  public PromoKeyRule() {
    setType(RuleHelper.PROMO_KEY_RULE);
    setName("Item Has Promo Key");
    setValueComparator(RuleHelper.EQUALS);
    setBooleanRule(true);
  }
  
  public boolean test(ICommerceObject obj) {
    
    IOrderItem item = getItem(obj);
    
    if (item != null) {
      Set<String> promoKeys = item.getPromoKeys();
      return (promoKeys != null && promoKeys.contains(getValue()));
    }
    return false;
  }
  
  public String getDisplayValue() {
    return getName() + " " + getValue();
  }
  
  public String getQualifyDisplayValue() {
    return "Select Items with Promo Key " + getValue();
  }
  
  public String getApplyDisplayValue() {
    return "Items With Promo Key " + getValue();
  }
}
