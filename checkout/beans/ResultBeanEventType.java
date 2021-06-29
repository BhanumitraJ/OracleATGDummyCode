package com.nm.commerce.checkout.beans;

public enum ResultBeanEventType {
  
  REMOVE_ORDER_ITEM("RemoveCartItem"), UPDATE_ORDER_ITEM("UpdateCartItem"), UPDATE_ORDER_ITEM_MINICART("UpdateMiniCartItem"), UPDATE_REPLENISHMENT_ITEM("UpdateReplenishmentItem"), ADD_ORDER_ITEM(
          "AddCartItem"), ADD_SFL_ITEM("AddSaveForLaterItem"), ADD_SFL_ITEM_MINICART("AddSaveForLaterMiniCartItem"), MOVE_SFL_TO_CART("MoveSaveForLaterItemToCart"), REMOVE_SFL_ITEM(
          "RemoveSaveForLaterItem"), ANONYMOUS_LOGIN("AnonymousLogin"), REGISTERED_LOGIN("RegisteredLogin"), ADD_GWP_SAMPLES("AddGWPSamples");
  
  private String mLabel;
  
  ResultBeanEventType(String label) {
    mLabel = label;
  }
  
  public String getLabel() {
    return mLabel;
  }
  
  public String toString() {
    return mLabel;
  }
  
  public String getName() {
    return name();
  }
}
