package com.nm.commerce;

import java.util.List;

public class CFAGiftCardBalance {
  
  private List<GiftCard> giftCards;
  private boolean isError;
  
  public List<GiftCard> getGiftCards() {
    return giftCards;
  }
  
  public void setGiftCards(List<GiftCard> giftCards) {
    this.giftCards = giftCards;
  }
  
  public boolean isError() {
    return isError;
  }
  
  public void setError(boolean isError) {
    this.isError = isError;
  }
}
