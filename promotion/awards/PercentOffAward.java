package com.nm.commerce.promotion.awards;

import java.util.Set;

import com.nm.collections.NMPromotionTypes;
import com.nm.commerce.NMCommerceItem;
import com.nm.commerce.order.IOrder;

public class PercentOffAward extends MarkdownAward {
  
  public final static String promoTypeStr = "PercentOffAward";
  
  @Override
  public boolean apply(IOrder order, String promoKey, String promoCode) {
    return false;
  }
  
  @Override
  public boolean apply(IOrder order, Set<NMCommerceItem> applicableItems, String promoKey, String promoCode) {
    return false;
  }
  
  public int getAwardType() {
    return NMPromotionTypes.PERCENT_OFF;
  }
  
}
