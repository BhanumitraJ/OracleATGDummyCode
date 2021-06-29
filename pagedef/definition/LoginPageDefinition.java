package com.nm.commerce.pagedef.definition;

public class LoginPageDefinition extends PageDefinition {
  
  private String successUrl;
  private String errorUrl;
  private String checkDynamicUrl;
  
  public String getSuccessUrl() {
    return getValue(successUrl, basis, "getSuccessUrl");
  }
  
  public void setSuccessUrl(String successUrl) {
    this.successUrl = successUrl;
  }
  
  public String getErrorUrl() {
    return getValue(errorUrl, basis, "getErrorUrl");
  }
  
  public void setErrorUrl(String errorUrl) {
    this.errorUrl = errorUrl;
  }
  
  @Override
  public String toString() {
    return "LoginPageDefinition toString() [successUrl=" + successUrl + ", errorUrl=" + errorUrl + "]";
  }
  
  public String getCheckDynamicUrl() {
    return checkDynamicUrl;
  }
  
  public void setCheckDynamicUrl(String checkDynamicUrl) {
    this.checkDynamicUrl = checkDynamicUrl;
  }
  
}
