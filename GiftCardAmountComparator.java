package com.nm.commerce;

import java.util.Comparator;

public class GiftCardAmountComparator implements Comparator {
  
  public GiftCardAmountComparator() {}
  
  // compare only amounts > 0, if its zero or less put it at the end of the list
  public int compare(Object obj, Object obj1) {
    
    int returnValue = 0;
    if (!(obj instanceof GiftCard) || !(obj1 instanceof GiftCard)) {
      throw new IllegalArgumentException("GiftCardAmountComparator can only compare objects of type QuantityRange");
    } else {
      GiftCard giftCard1 = (GiftCard) obj;
      GiftCard giftCard2 = (GiftCard) obj1;
      Double amount1 = new Double(giftCard1.getAmountAvailable());
      Double amount2 = new Double(giftCard2.getAmountAvailable());
      if (amount1.compareTo(amount2) == 0 && giftCard1.getCid() != null && !giftCard1.getCid().trim().equals("")) {
        return -1;
      }
      if (amount1.compareTo(amount2) == 0 && giftCard2.getCid() != null && !giftCard2.getCid().trim().equals("")) {
        return 1;
      }
      if (amount1.doubleValue() <= 0.0d) {
        return 1;
      }
      if (amount2.doubleValue() <= 0.0d) {
        return -1;
      }
      return amount1.compareTo(amount2);
    }
  }
  
  public boolean equals(Object obj) {
    return obj instanceof GiftCardAmountComparator;
  }
  
}
