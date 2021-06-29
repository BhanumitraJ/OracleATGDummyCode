package com.nm.commerce.checkout;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.nm.collections.NMPromotion;
import com.nm.commerce.promotion.NMOrderImpl;

public class OrderSnapshot {
  
  public OrderSnapshot(final NMOrderImpl order) {
    final Set<NMPromotion> promos = new HashSet<NMPromotion>(order.getAwardedPromotions());
    awardedPromotions = Collections.unmodifiableSet(promos);
    gwpSelectPromoMap = Collections.unmodifiableMap(order.getGwpSelectPromoMap());
    pwpSelectPromoMap = Collections.unmodifiableMap(order.getPwpMultiSkuPromoMap());
  }
  
  public Set<NMPromotion> getAwardedPromotions() {
    return (awardedPromotions);
  }
  
  public Map<String, String> getGwpSelectPromoMap() {
    return gwpSelectPromoMap;
  }
  
  public Map<String, ?> getPwpSelectPromoMap() {
    return pwpSelectPromoMap;
  }
  
  private Set<NMPromotion> awardedPromotions = new HashSet<NMPromotion>();
  private Map<String, String> gwpSelectPromoMap = new HashMap<String, String>();
  private Map<String, ?> pwpSelectPromoMap = new HashMap<String, Object>();
}
