package com.nm.commerce.promotion.rules;

import java.util.Iterator;
import java.util.Set;

import com.nm.commerce.NMCommerceItem;
import com.nm.commerce.order.ICommerceObject;

public class ApplicableItemsDollarRule extends AggregateRule {
  
  public ApplicableItemsDollarRule() {
    setType(RuleHelper.APPLICABLE_ITEMS_DOLLAR_RULE);
    setName("Total Dollars Spent on Applicable Items");
    setValuePrefix("$");
  }
  
  protected boolean test(Set<NMCommerceItem> items) {
    // System.out.println("testing QualifiedDollarRule");
    double totalDollarAmount = 0d;
    Iterator<NMCommerceItem> i = items.iterator();
    while (i.hasNext()) {
      NMCommerceItem item = (NMCommerceItem) i.next();
      totalDollarAmount += item.getCurrentItemPrice();
    }
    
    // System.out.println("   pass? " + RuleHelper.compare(totalDiscountedOrderAmount, (new Double(value)).doubleValue(), valueComparator));
    
    return RuleHelper.compare(totalDollarAmount, (new Double(value)).doubleValue(), valueComparator);
  }
  
  protected boolean test(ICommerceObject obj) {
    return false;
  }
  
  public String getDisplayValue() {
    if (getValue() != null) return getName() + " " + getStringOperator() + " " + getValuePrefix() + getValue();
    return "";
  }
}
