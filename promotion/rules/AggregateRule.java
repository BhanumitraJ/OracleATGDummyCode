package com.nm.commerce.promotion.rules;

import java.util.Set;

import com.nm.commerce.NMCommerceItem;
import com.nm.commerce.order.IOrder;

public abstract class AggregateRule extends OrderRule {
  
  protected Set<NMCommerceItem> addQualifiedItems(IOrder order, Set<NMCommerceItem> items) {
    if (test(items)) return items;
    return null;
  }
  
  protected Set<NMCommerceItem> removeUnqualifiedItems(IOrder order, Set<NMCommerceItem> items) {
    if (!test(items)) items.clear();
    return items;
  }
  
  protected abstract boolean test(Set<NMCommerceItem> items);
}
