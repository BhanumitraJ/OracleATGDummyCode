package com.nm.commerce.promotion.rules;

import com.nm.commerce.order.ICommerceObject;
import com.nm.commerce.promotion.NMOrderImpl;

public class PromoCodeRule extends OrderRule {
  
  public PromoCodeRule() {
    setType(RuleHelper.PROMO_CODE_RULE);
    setName("Has Promo Code");
    setValueComparator(RuleHelper.EQUALS);
    setHiddenRule(true);
  }
  
  protected boolean test(ICommerceObject obj) {
    // System.out.println("testing PromoCodeRule");
    NMOrderImpl order = (NMOrderImpl) obj;
    if (order == null) return false;
    // System.out.println("   pass? " + order.getPromoCodeList() != null && order.getPromoCodeList().contains(value));
    // if promo code list contains the value of this promo code rule, then user has entered the code and the rule passes
    return (order.getPromoCodeList() != null && order.getPromoCodeList().contains(value.toUpperCase()));
  }
  
  public String getDisplayValue() {
    if (getValue() != null) return getName() + " " + getValue();
    return "";
  }
}
