package com.nm.commerce.promotion.rules;

import java.util.Iterator;
import java.util.Set;

import com.nm.commerce.NMCommerceItem;
import com.nm.commerce.order.ICommerceObject;

public class QualifiedDollarRule extends AggregateRule {
  
  public QualifiedDollarRule() {
    setType(RuleHelper.QUALIFIED_DOLLAR_RULE);
    setName("Total Dollars Spent on Qualified Items");
    setValuePrefix("$");
  }
  
  protected boolean test(Set<NMCommerceItem> items) {
    // System.out.println("testing QualifiedDollarRule");
    double totalDollarAmount = 0d;
    if (items != null) {
      Iterator<NMCommerceItem> i = items.iterator();
      while (i.hasNext()) {
        NMCommerceItem item = (NMCommerceItem) i.next();
        
        // this may need to be done by getting total raw price and
        // subtracting percent off dollar discount...
        // in the case of stacked promo, we don't want to use any dollar
        // off discounts in the starting price for qualification
        totalDollarAmount += item.getCurrentItemPrice();
      }
      
      // System.out.println("   pass? " + RuleHelper.compare(totalDiscountedOrderAmount, (new Double(value)).doubleValue(), valueComparator));
    }
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
