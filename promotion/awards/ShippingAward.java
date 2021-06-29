package com.nm.commerce.promotion.awards;

import java.util.List;
import java.util.Set;

import com.nm.collections.NMPromotionTypes;
import com.nm.commerce.NMCommerceItem;
import com.nm.commerce.order.IOrder;
import com.nm.commerce.promotion.rules.Rule;

public class ShippingAward extends BaseAward {
  
  public final static String promoTypeStr = "ShippingAward";
  
  public String getDisplayValue() {
    return "";
  }
  
  public Object getSortableValue() {
    return null;
  }
  
  public boolean apply(IOrder order, Set<NMCommerceItem> applicableItems, List<Rule> applicationRules, String promoKey, String promoCode) {
    return false;
  }
  
  public int getAwardType() {
    return NMPromotionTypes.SHIPPING;
  }
}
