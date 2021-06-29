package com.nm.commerce.promotion.action;

import java.util.Map;

import com.nm.ajax.checkout.utils.ComponentUtils;
import com.nm.collections.NMPromotion;
import com.nm.collections.NMPromotionArray;
public class MarkdownAction extends RulesBasedPromotionAction {
	
  public Map<String, NMPromotion> getActivePromotions() {
	ComponentUtils componentUtils = ComponentUtils.getInstance();
	NMPromotionArray promotionArray = componentUtils.getPromotionArray(NMPromotionArray.RULE_BASED_PROMOTIONS_ARRAY);
	return promotionArray.getAllActivePromotions();
  }
  
}
