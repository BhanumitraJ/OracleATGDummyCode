package com.nm.commerce.promotion.rules;

import java.util.Set;

import com.nm.commerce.order.ICommerceObject;
import com.nm.commerce.order.IOrder;
import com.nm.commerce.NMCommerceItem;

public class ApplyToQualifiedItemsRule extends BaseRule {
  
  public ApplyToQualifiedItemsRule() {
    setType(RuleHelper.APPLY_TO_QUALIFIED_ITEMS_RULE);
    setName("Qualified Items Only");
    setValueComparator(RuleHelper.EQUALS);
    setValue("true");
    setBooleanRule(true);
  }
  
  protected Set<NMCommerceItem> addQualifiedItems(IOrder order, Set<NMCommerceItem> qualifiedItems) {
    return qualifiedItems;
  }
  
  protected Set<NMCommerceItem> removeUnqualifiedItems(IOrder order, Set<NMCommerceItem> qualifiedItems) {
    return qualifiedItems;
  }
  
  protected boolean test(ICommerceObject obj) {
    return true;
  }
  
  public String getDisplayValue() {
    return getName();
  }
  
}
