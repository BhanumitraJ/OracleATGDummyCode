package com.nm.commerce.promotion.rules;

import java.util.Iterator;
import java.util.Set;

import com.nm.commerce.NMCommerceItem;
import com.nm.commerce.order.ICommerceObject;
import com.nm.commerce.order.IOrder;
import com.nm.commerce.order.IOrderItem;

public abstract class ItemRule extends BaseRule {
  
  protected IOrderItem getItem(ICommerceObject obj) {
    if (obj instanceof IOrderItem) return (IOrderItem) obj;
    return null;
  }
  
  protected Set<NMCommerceItem> addQualifiedItems(IOrder order, Set<NMCommerceItem> qualifiedItems) {
    
    Iterator<NMCommerceItem> i = order.getPromoEligibleOrderItems().iterator();
    
    while (i.hasNext()) {
      NMCommerceItem item = (NMCommerceItem) i.next();
      if (test(item)) qualifiedItems.add(item);
    }
    
    return qualifiedItems;
  }
  
  protected Set<NMCommerceItem> removeUnqualifiedItems(IOrder order, Set<NMCommerceItem> qualifiedItems) {
    
    Iterator<NMCommerceItem> i = qualifiedItems.iterator();
    while (i.hasNext()) {
      IOrderItem item = (IOrderItem) i.next();
      if (!test(item)) i.remove();
    }
    return qualifiedItems;
  }
}
