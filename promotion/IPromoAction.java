package com.nm.commerce.promotion;

import atg.commerce.order.Order;

public interface IPromoAction {
  public void evaluatePromo(final Order order) throws PromoException;
}
