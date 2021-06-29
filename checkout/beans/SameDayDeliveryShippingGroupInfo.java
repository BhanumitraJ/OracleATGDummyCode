package com.nm.commerce.checkout.beans;

import java.util.List;

import com.nm.commerce.pagedef.model.bean.ShippingGroupAux;
import com.nm.storeinventory.SameDayDeliveryInventoryInfo;

public class SameDayDeliveryShippingGroupInfo extends SameDayDeliveryOrderInfo {
  private List<SameDayDeliveryInventoryInfo> sddInventoryInfoList;
  private ShippingGroupAux shippingGroup;
  
  public List<SameDayDeliveryInventoryInfo> getSddInventoryInfoList() {
    return sddInventoryInfoList;
  }
  
  public void setSddInventoryInfoList(List<SameDayDeliveryInventoryInfo> sddInventoryInfoList) {
    this.sddInventoryInfoList = sddInventoryInfoList;
  }
  
  public ShippingGroupAux getShippingGroup() {
    return shippingGroup;
  }
  
  public void setShippingGroup(ShippingGroupAux shippingGroup) {
    this.shippingGroup = shippingGroup;
  }
}
