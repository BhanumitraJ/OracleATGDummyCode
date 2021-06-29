/**
 * 
 */
package com.nm.commerce;

/**
 * The TempUserInfo object is used to store UserRegistration information in the event a customer has entered the data, but not submitted and navigates away from orderreview page.
 * 
 */
public class TempUserInfo {
  
  private boolean optedToRegister;
  private String email;
  private String password;
  private String confirmPassword;
  private boolean profileCreated = false;
  
  public TempUserInfo() {
    
  }
  
  /**
   * This object should only be generated from the OrderReviewService. Since the xstream classes which create this object do not utilize the constructor it will be passed to this object which will
   * encrypt all necessary data.
   * 
   * @param userInfo
   */
  public TempUserInfo(final TempUserInfo userInfo) {
    this();
    this.optedToRegister = userInfo.isOptedToRegister();
    this.email = userInfo.getEmail();
    this.password = userInfo.getPassword();
    this.confirmPassword = userInfo.getConfirmPassword();
  }
  
  public boolean isOptedToRegister() {
    return optedToRegister;
  }
  
  public void setOptedToRegister(final boolean optedToRegister) {
    this.optedToRegister = optedToRegister;
  }
  
  public String getEmail() {
    return email;
  }
  
  public void setEmail(final String email) {
    this.email = email;
  }
  
  public String getPassword() {
    return password;
  }
  
  public void setPassword(final String password) {
    this.password = password;
  }
  
  public String getConfirmPassword() {
    return confirmPassword;
  }
  
  public void setConfirmPassword(final String confirmPassword) {
    this.confirmPassword = confirmPassword;
  }
  
 
  public boolean isProfileCreated() {
    return profileCreated;
  }

  public void setProfileCreated(boolean profileCreated) {
    this.profileCreated = profileCreated;
  }

  public void update(final TempUserInfo userInfo) {
    this.optedToRegister = userInfo.isOptedToRegister();
    this.email = userInfo.getEmail();
    this.password = userInfo.getPassword();
    this.confirmPassword = userInfo.getConfirmPassword();
  }
  
  public void clear() {
    this.optedToRegister = false;
    this.email = null;
    this.password = null;
    this.confirmPassword = null;
  }
}
