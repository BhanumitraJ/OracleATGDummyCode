package com.nm.commerce.promotion.rules;

import java.util.Set;

import com.nm.commerce.NMCommerceItem;
import com.nm.commerce.order.IOrder;

/**
 * The Class <code>PaymentTypeRule</code>
 * <p>
 * This is an abstract class for payment types like Visa Checkout, MasterPass etc
 * </p>
 * 
 * 
 */
public abstract class PaymentTypeRule extends BaseRule {
  
  /**
   * <p>
   * This method adds Promotion eligible items in Order to a set based on the test() implementation logic
   * </p>
   */
  @Override
  public Set<NMCommerceItem> addQualifiedItems(final IOrder order, final Set<NMCommerceItem> qualifiedItems) {
    if (test(order)) {
      qualifiedItems.addAll(order.getPromoEligibleOrderItems());
    }
    return qualifiedItems;
  }
  
  /**
   * <p>
   * This method clears Promotion eligible items from the set based on the test() implementation logic
   * </p>
   */
  @Override
  protected Set<NMCommerceItem> removeUnqualifiedItems(final IOrder order, final Set<NMCommerceItem> qualifiedItems) {
    if (!test(order)) {
      qualifiedItems.clear();
    }
    return qualifiedItems;
  }
  
}
