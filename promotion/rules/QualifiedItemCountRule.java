package com.nm.commerce.promotion.rules;

import java.util.Iterator;
import java.util.Set;

import com.nm.commerce.NMCommerceItem;
import com.nm.commerce.order.ICommerceObject;

public class QualifiedItemCountRule extends AggregateRule {
  
  public QualifiedItemCountRule() {
    setType(RuleHelper.QUALIFIED_COUNT_RULE);
    setName("Qualified Item Count");
  }
  
  protected boolean test(Set<NMCommerceItem> items) {
    
    int count = 0;
    if (items != null) {
      Iterator<NMCommerceItem> itemsI = items.iterator();
      while (itemsI.hasNext()) {
        NMCommerceItem item = (NMCommerceItem) itemsI.next();
        count += item.getQuantity();
      }
      return RuleHelper.compare(count, RuleHelper.getIntValue(getValue()), this.getValueComparator());
    }
    return false;
  }
  
  protected boolean test(ICommerceObject obj) {
    return false;
  }
  
  public String getDisplayValue() {
    return getName() + " " + getStringOperator() + " " + getValue();
  }
  
  public String getQualifyDisplayValue() {
    return "Qualified Item Count Total " + " " + getStringOperator() + " " + getValue();
  }
  
  public String getApplyDisplayValue() {
    return "Qualified Item Total Count " + " " + getStringOperator() + " " + getValue();
  }
}
