package com.nm.commerce.pagedef.model;

public class LoginPageModel extends PageModel {
  
  private String successUrl;
  private String errorUrl;
  private String refererUrl;
  
  public LoginPageModel() {
    super();
  }
  
  public String getSuccessUrl() {
    return successUrl;
  }
  
  public void setSuccessUrl(String successUrl) {
    this.successUrl = successUrl;
  }
  
  public String getErrorUrl() {
    return errorUrl;
  }
  
  public void setErrorUrl(String errorUrl) {
    this.errorUrl = errorUrl;
  }
  
  public String getRefererUrl() {
    return refererUrl;
  }
  
  public void setRefererUrl(String refererUrl) {
    this.refererUrl = refererUrl;
  }
  
  @Override
  public String toString() {
    return "LoginPageModel [successUrl=" + successUrl + ", errorUrl=" + errorUrl + ", refererUrl=" + refererUrl + "]";
  }
  
}
