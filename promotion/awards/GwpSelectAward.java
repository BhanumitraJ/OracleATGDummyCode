package com.nm.commerce.promotion.awards;

import com.nm.collections.NMPromotionTypes;

public class GwpSelectAward extends BaseAwardImpl {
  
  public final static String promoTypeStr = "GwpSelectAward";
  
  public int getAwardType() {
    return NMPromotionTypes.GIFT_WITH_PURCHASE_SELECT;
  }
}
