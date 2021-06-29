package com.nm.commerce.checkout.beans;

import com.nm.collections.ServiceLevel;

public class ShippingGroupBean {
  private String id;
  private ServiceLevel[] serviceLevels;
  
  public ShippingGroupBean(String id) {
    this.id = id;
  }
  
  public ServiceLevel[] getServiceLevels() {
    return serviceLevels;
  }
  
  public String getId() {
    return id;
  }
  
  public void setServiceLevels(ServiceLevel[] value) {
    serviceLevels = value;
  }
  
  public void setId(String value) {
    id = value;
  }
}
