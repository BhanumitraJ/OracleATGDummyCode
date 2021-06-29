package com.nm.commerce.checkout.beans;

import java.util.ArrayList;

import com.nm.commerce.NMCommerceItem;

public class SaveForLaterItemCollection extends ArrayList<NMCommerceItem> {
  private static final long serialVersionUID = 1L;
  private int mNoLongerAvailableItemCount = 0;
  
  public int getNoLongerAvailableItemCount() {
    return mNoLongerAvailableItemCount;
  }
  
  public void setNoLongerAvailableItemCount(int value) {
    mNoLongerAvailableItemCount = value;
  }
}
