package com.nm.commerce.checkout.beans;

public class LoginBean {
  public static final String REGISTERED = "registered";
  public static final String ANONYMOUS = "anonymous";
  
  private String email;
  private String password;
  private String type;
  
  public LoginBean() {}
  
  public String getEmail() {
    return email;
  }
  
  public String getPassword() {
    return password;
  }
  
  public String getType() {
    return type;
  }
  
  public boolean isAnonymous() {
    return (ANONYMOUS.equalsIgnoreCase(type));
  }
  
  public void setEmail(String value) {
    email = value;
  }
  
  public void setPassword(String value) {
    password = value;
  }
  
  public void setType(String value) {
    type = value;
  }
}
