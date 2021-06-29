package com.nm.commerce.promotion.rules;

import static com.nm.common.INMGenericConstants.TRUE_STRING;

import java.util.List;

import com.nm.commerce.NMCreditCard;
import com.nm.commerce.order.ICommerceObject;
import com.nm.commerce.promotion.NMOrderImpl;

/**
 * This class is used to create a new rule for visa checkout only dollar off promotion
 * 
 * @author cognizant
 * 
 */
public class VisaCheckoutPromotionRule extends PaymentTypeRule {
  /** The Constant VISA_CHECKOUT_PAYMENT */
  public static final String VISA_CHECKOUT_PAYMENT = "Payment type is Visa Checkout only (auto-applied)";
  
  /**
   * Constructor VisaCheckoutPromotionRule.
   */
  public VisaCheckoutPromotionRule() {
    setType(RuleHelper.VISA_CHECKOUT_RULE);
    setName(VISA_CHECKOUT_PAYMENT);
    setValue(TRUE_STRING);
    setValueComparator(RuleHelper.EQUALS);
    setBooleanRule(true);
  }
  
  /**
   * <p>
   * This method has been overridden for Visa Checkout <br/>
   * to identify if the Payment group on Order is Visa Checkout.
   * </p>
   */
  @Override
  protected boolean test(final ICommerceObject obj) {
    final NMOrderImpl order = (NMOrderImpl) obj;
    final List<NMCreditCard> orderCreditCards = order.getCreditCards();
    if (!orderCreditCards.isEmpty()) {
      final NMCreditCard paymentGroup = orderCreditCards.get(0);
      // checking if the payment is done through visa checkout
      if (paymentGroup.isVmeCard()) {
        return true;
      }
    }
    return false;
  }
  
  /**
   * <p>
   * This method retrieves the rule name that would be displayed<br/>
   * in the Qualification rules dropdown in CSR
   * </p>
   */
  @Override
  public String getDisplayValue() {
    return getName();
  }
  
}
