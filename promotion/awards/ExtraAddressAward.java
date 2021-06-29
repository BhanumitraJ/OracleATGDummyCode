package com.nm.commerce.promotion.awards;

import com.nm.collections.NMPromotionTypes;

public class ExtraAddressAward extends BaseAwardImpl {
  
  public final static String promoTypeStr = "ExtraAddressAward";
  
  public int getAwardType() {
    return NMPromotionTypes.EXTRA_ADDRESS;
  }
  
}
