package com.nm.commerce.promotion.rules;

import com.nm.commerce.NMCommerceItem;
import com.nm.commerce.order.ICommerceObject;

public class ItemDollarRule extends ItemRule {
  
  public ItemDollarRule() {
    
    setType(RuleHelper.ITEM_DOLLAR_RULE);
    setName("Dollars Spent On Item");
    setValuePrefix("$");
  }
  
  // public static final String name = "Item Dollar Rule";
  
  protected boolean test(ICommerceObject obj) {
    NMCommerceItem item = (NMCommerceItem) obj;
    double itemPrice = item.getCurrentItemPrice();
    return RuleHelper.compare(itemPrice, (new Double(value)).doubleValue(), valueComparator);
  }
  
  public String getDisplayValue() {
    if (getValue() != null) return getValuePrefix() + getValue() + " " + getName();
    return "";
  }
}
