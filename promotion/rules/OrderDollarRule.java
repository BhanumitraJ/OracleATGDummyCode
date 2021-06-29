package com.nm.commerce.promotion.rules;

import java.util.Iterator;
import java.util.List;

import com.nm.commerce.NMCommerceItem;
import com.nm.commerce.order.ICommerceObject;
import com.nm.commerce.promotion.NMOrderImpl;

public class OrderDollarRule extends OrderRule {
  
  public OrderDollarRule() {
    
    setType(RuleHelper.ORDER_DOLLAR_RULE);
    setName("Total Dollars Spent On Order");
    setValuePrefix("$");
  }
  
  protected boolean test(ICommerceObject obj) {
    // System.out.println("testing OrderDollarRule");
    double totalDiscountedOrderAmount = 0d;
    NMOrderImpl order = (NMOrderImpl) obj;
    List<NMCommerceItem> items = order.getPromoEligibleOrderItems();
    if (items != null) {
      Iterator<NMCommerceItem> i = items.iterator();
      while (i.hasNext()) {
        NMCommerceItem item = (NMCommerceItem) i.next();
        totalDiscountedOrderAmount += RuleHelper.getStartingItemPriceForQualification(item);
      }
      
      // System.out.println("   pass? " + RuleHelper.compare(totalDiscountedOrderAmount, (new Double(value)).doubleValue(), valueComparator));
    }
    
    return RuleHelper.compare(totalDiscountedOrderAmount, (new Double(value)).doubleValue(), valueComparator);
  }
  
  public String getDisplayValue() {
    if (getValue() != null) return getName() + " " + getStringOperator() + " " + getValuePrefix() + getValue();
    return "";
  }
}
