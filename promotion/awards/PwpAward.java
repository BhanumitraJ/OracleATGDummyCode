package com.nm.commerce.promotion.awards;

import com.nm.collections.NMPromotionTypes;

public class PwpAward extends BaseAwardImpl {
  
  public final static String promoTypeStr = "PwpAward";
  
  public int getAwardType() {
    return NMPromotionTypes.PURCHASE_WITH_PURCHASE;
  }
}
