package com.nm.commerce.checkout.beans;

public class SameDayDeliveryOrderInfo {
  private Boolean sddEligibleByLocation;
  private Boolean sddEligibleByItems;
  
  public Boolean isSddEligible() {
    Boolean isSddEligible = Boolean.FALSE;
    if ((null != sddEligibleByLocation) && (null != sddEligibleByItems)) {
      isSddEligible = sddEligibleByLocation && sddEligibleByItems;
    }
    return isSddEligible;
  }
  
  public Boolean getSddEligibleByLocation() {
    return sddEligibleByLocation;
  }
  public void setSddEligibleByLocation(Boolean sddEligibleByLocation) {
    this.sddEligibleByLocation = sddEligibleByLocation;
  }
  public Boolean getSddEligibleByItems() {
    return sddEligibleByItems;
  }
  public void setSddEligibleByItems(Boolean sddEligibleByItems) {
    this.sddEligibleByItems = sddEligibleByItems;
  }
}
