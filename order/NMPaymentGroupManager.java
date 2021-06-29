package com.nm.commerce.order;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import atg.commerce.CommerceException;
import atg.commerce.order.PaymentGroupManager;
import atg.commerce.order.PaymentGroupOrderRelationship;

import com.nm.commerce.NMCreditCard;
import com.nm.commerce.promotion.NMOrderImpl;

public class NMPaymentGroupManager extends PaymentGroupManager {
  public static final int CREDIT_CARDS = 0;
  public static final int GIFT_CARDS = 1;
  public static final int PREPAID_CARDS = 2;
  public static final int ALL_CARDS = 3;
  
  public NMPaymentGroupManager() {}
  
  public void clearPaymentGroupAmounts(NMOrderImpl order, int[] types) throws CommerceException {
    @SuppressWarnings("unchecked")
    List<PaymentGroupOrderRelationship> paymentGroupRelationships = order.getPaymentGroupRelationships();
    Iterator<PaymentGroupOrderRelationship> iterator = paymentGroupRelationships.iterator();
    while (iterator.hasNext()) {
      PaymentGroupOrderRelationship paymentGroupRelationship = (PaymentGroupOrderRelationship) iterator.next();
      NMCreditCard paymentGroup = (NMCreditCard) paymentGroupRelationship.getPaymentGroup();
      
      for (int i = 0; i < types.length; ++i) {
        switch (types[i]) {
          case ALL_CARDS:
            if (paymentGroup.isGiftCard() || paymentGroup.isPrepaidCard()) {
              double amountRemaining = paymentGroup.getAmountRemaining();
              double amount = paymentGroup.getAmount();
              paymentGroup.setAmountRemaining(amountRemaining + amount);
            }
            paymentGroupRelationship.setAmount(0.0);
            paymentGroup.setAmount(0.0d);
          break;
          case CREDIT_CARDS:
            if (paymentGroup.isCreditCard()) {
              paymentGroup.setAmount(0.0d);
              paymentGroupRelationship.setAmount(0.0);
            }
          break;
          case GIFT_CARDS:
            if (paymentGroup.isGiftCard()) {
              double amountRemaining = paymentGroup.getAmountRemaining();
              double amount = paymentGroup.getAmount();
              paymentGroup.setAmountRemaining(amountRemaining + amount);
              paymentGroup.setAmount(0.0d);
              paymentGroupRelationship.setAmount(0.0);
            }
          break;
          case PREPAID_CARDS:
            if (paymentGroup.isPrepaidCard()) {
              double amountRemaining = paymentGroup.getAmountRemaining();
              double amount = paymentGroup.getAmount();
              paymentGroup.setAmountRemaining(amountRemaining + amount);
              paymentGroup.setAmount(0.0d);
              paymentGroupRelationship.setAmount(0.0);
            }
          break;
        }
      }
    }
  }
  
  public List<NMCreditCard> getPaymentGroups(NMOrderImpl order, int[] types) {
    ArrayList<NMCreditCard> returnValue = new ArrayList<NMCreditCard>();
    
    @SuppressWarnings("unchecked")
    List<NMCreditCard> paymentGroups = order.getPaymentGroups();
    Iterator<NMCreditCard> iterator = paymentGroups.iterator();
    while (iterator.hasNext()) {
      NMCreditCard paymentGroup = (NMCreditCard) iterator.next();
      
      for (int i = 0; i < types.length; ++i) {
        switch (types[i]) {
          case ALL_CARDS:
            returnValue.add(paymentGroup);
          break;
          case CREDIT_CARDS:
            if (paymentGroup.isCreditCard()) {
              returnValue.add(paymentGroup);
            }
          break;
          case GIFT_CARDS:
            if (paymentGroup.isGiftCard()) {
              returnValue.add(paymentGroup);
            }
          break;
          case PREPAID_CARDS:
            if (paymentGroup.isPrepaidCard()) {
              returnValue.add(paymentGroup);
            }
          break;
        }
      }
    }
    
    return returnValue;
  }
}
