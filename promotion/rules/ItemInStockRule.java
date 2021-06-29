package com.nm.commerce.promotion.rules;

import com.nm.commerce.order.ICommerceObject;
import com.nm.commerce.order.IOrderItem;

public class ItemInStockRule extends ItemRule {
  
  public ItemInStockRule() {
    setType(RuleHelper.ITEM_IN_STOCK_RULE);
    setName("Item is In Stock");
    setValue("true");
    setValueComparator(RuleHelper.EQUALS);
    setBooleanRule(true);
  }
  
  public boolean test(ICommerceObject obj) {
    // System.out.println("testing ItemInStockRule");
    IOrderItem item = getItem(obj);
    
    if (item != null) {
      // System.out.println("  pass? " + item.isInStock());
      if (item.isInStock()) return true;
    }
    return false;
  }
  
  public String getDisplayValue() {
    return getName();
  }
  
  public String getQualifyDisplayValue() {
    return "Select In Stock Items";
  }
  
  public String getApplyDisplayValue() {
    return "In Stock Items";
  }
}
