package com.nm.commerce.promotion.rules;

import java.util.Set;

import com.nm.commerce.NMCommerceItem;
import com.nm.commerce.order.ICommerceObject;
import com.nm.commerce.order.IOrder;

public abstract class OrderRule extends BaseRule {
  
  protected IOrder getOrder(ICommerceObject obj) {
    if (obj instanceof IOrder) return (IOrder) obj;
    return null;
  }
  
  protected Set<NMCommerceItem> addQualifiedItems(IOrder order, Set<NMCommerceItem> qualifiedItems) {
    if (test(order)) qualifiedItems.addAll(order.getPromoEligibleOrderItems());
    return qualifiedItems;
  }
  
  protected Set<NMCommerceItem> removeUnqualifiedItems(IOrder order, Set<NMCommerceItem> qualifiedItems) {
    if (!test(order)) qualifiedItems.clear();
    return qualifiedItems;
  }
}
