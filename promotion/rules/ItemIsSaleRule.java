package com.nm.commerce.promotion.rules;

import com.nm.commerce.order.ICommerceObject;
import com.nm.commerce.order.IOrderItem;

public class ItemIsSaleRule extends ItemRule {
  
  public ItemIsSaleRule() {
    setType(RuleHelper.ITEM_IS_SALE_RULE);
    setValueComparator(RuleHelper.EQUALS);
    setBooleanRule(true);
  }
  
  // if item is sale item, this test fails so item will not be added to qualified list
  public boolean test(ICommerceObject obj) {
    // System.out.println("testing ExcludeSaleItemRule");
    IOrderItem item = getItem(obj);
    
    if (item != null) {
      // System.out.println("   pass? " + !item.isSaleItem());
      if (!item.isSaleItem()) return true;
    }
    return false;
  }
  
  public String getDisplayValue() {
    return getName();
  }
}
