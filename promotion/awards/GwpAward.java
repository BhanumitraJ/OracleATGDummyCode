package com.nm.commerce.promotion.awards;

import com.nm.collections.NMPromotionTypes;

public class GwpAward extends BaseAwardImpl {
  
  public final static String promoTypeStr = "GwpAward";
  
  public int getAwardType() {
    return NMPromotionTypes.GIFT_WITH_PURCHASE;
  }
}
