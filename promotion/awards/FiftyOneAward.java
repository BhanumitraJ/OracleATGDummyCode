package com.nm.commerce.promotion.awards;

import java.util.Set;

import com.nm.collections.NMPromotionTypes;
import com.nm.commerce.NMCommerceItem;
import com.nm.commerce.order.IOrder;

public class FiftyOneAward extends MarkdownAward {
  public final static String promoTypeStr = "FiftyOneAward";
  
  @Override
  public boolean apply(IOrder order, String promoKey, String promoCode) {
    return false;
  }
  
  @Override
  public boolean apply(IOrder order, Set<NMCommerceItem> applicableItems, String promoKey, String promoCode) {
    return false;
  }
  
  public int getAwardType() {
    return NMPromotionTypes.FIFTYONE_PASS_THROUGH;
  }
}
