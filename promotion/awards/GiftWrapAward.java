package com.nm.commerce.promotion.awards;

import com.nm.collections.NMPromotionTypes;

public class GiftWrapAward extends BaseAwardImpl {
  
  public final static String promoTypeStr = "GiftWrapAward";
  
  public int getAwardType() {
    return NMPromotionTypes.GIFT_WRAP;
  }
}
