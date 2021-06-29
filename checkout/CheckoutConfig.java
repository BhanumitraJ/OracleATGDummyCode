package com.nm.commerce.checkout;

import java.util.Map;

import atg.nucleus.GenericService;

/**
 * This class will hold configuration information specific to checkout API. In addition, it will provide logging config and access for non-component classes.
 */
public class CheckoutConfig extends GenericService {
  
  public CheckoutConfig() {};
  
  public String getTestCheckoutUserType() {
    return testCheckoutUserType;
  }
  
  public void setTestCheckoutUserType(String testCheckoutUserType) {
    this.testCheckoutUserType = testCheckoutUserType;
  }
  
  public String getSystemCodeForTestUser() {
    return systemCodeForTestUser;
  }
  
  public void setSystemCodeForTestUser(String systemCodeForTestUser) {
    this.systemCodeForTestUser = systemCodeForTestUser;
  }
  
  public String[] getExtraShippingChargeStates() {
    return extraShippingChargeStates;
  }
  
  public void setExtraShippingChargeStates(String[] extraShippingChargeStates) {
    this.extraShippingChargeStates = extraShippingChargeStates;
  }
  
  public String[] getNonContinentalUSStates() {
    return nonContinentalUSStates;
  }
  
  public void setNonContinentalUSStates(String[] nonContinentalUSStates) {
    this.nonContinentalUSStates = nonContinentalUSStates;
  }
  
  public String getCustomerServicePhone() {
    return customerServicePhone;
  }
  
  public void setCustomerServicePhone(String customerServicePhone) {
    this.customerServicePhone = customerServicePhone;
  }
  
  
  /**
   * @return the minicartBannerMap
   */
  public Map<String, String> getMinicartBannerMap() {
	return minicartBannerMap;
  }

  /**
   * @param minicartBannerMap the minicartBannerMap to set
   */
  public void setMinicartBannerMap(Map<String, String> minicartBannerMap) {
	this.minicartBannerMap = minicartBannerMap;
  }

  private String testCheckoutUserType;
  private String systemCodeForTestUser;
  private String[] extraShippingChargeStates;
  private String[] nonContinentalUSStates;
  private String customerServicePhone;
  private Map<String, String> minicartBannerMap;
}
