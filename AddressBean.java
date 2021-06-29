package com.nm.commerce;

/**
 * Bean to hold the Shipping Address fields.
 */
public class AddressBean {
  private String titleCode;
  private String firstName;
  private String lastName;
  private String address1;
  private String address2;
  private String city;
  private String state;
  private String country;
  private String province;
  private String postalCode;
  private String phoneNumber;
  private String accountNumber;
  private String email;
  private String confirmEmail;
  private String phoneType;
  private String dayPhoneExt;
  private String deliveryPhoneNumber;
  private String countryUSACode = "US";
  private String yearArray;
  private String signature;
  private boolean flgPOBox = false;
  
  public AddressBean() {}
  
  public String getTitleCode() {
    return titleCode;
  }
  
  public void setTitleCode(String titleCode) {
    this.titleCode = titleCode;
  }
  
  public String getFirstName() {
    return firstName;
  }
  
  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }
  
  public String getLastName() {
    return lastName;
  }
  
  public void setLastName(String lastName) {
    this.lastName = lastName;
  }
  
  public String getAddress1() {
    return address1;
  }
  
  public void setAddress1(String address1) {
    this.address1 = address1;
  }
  
  public String getAddress2() {
    return address2;
  }
  
  public void setAddress2(String address2) {
    this.address2 = address2;
  }
  
  public String getCity() {
    return city;
  }
  
  public void setCity(String city) {
    this.city = city;
  }
  
  public String getState() {
    return state;
  }
  
  public void setState(String state) {
    this.state = state;
  }
  
  public String getCountry() {
    return country;
  }
  
  public void setCountry(String country) {
    this.country = country;
  }
  
  public String getProvince() {
    return province;
  }
  
  public void setProvince(String province) {
    this.province = province;
  }
  
  public String getPostalCode() {
    return postalCode;
  }
  
  public void setPostalCode(String postalCode) {
    this.postalCode = postalCode;
  }
  
  public String getPhoneNumber() {
    return phoneNumber;
  }
  
  public void setPhoneNumber(String phoneNumber) {
    this.phoneNumber = phoneNumber;
  }
  
  public String getAccountNumber() {
    return accountNumber;
  }
  
  public void setAccountNumber(String accountNumber) {
    this.accountNumber = accountNumber;
  }
  
  public String getEmail() {
    return email;
  }
  
  public void setEmail(String email) {
    this.email = email;
  }
  
  public String getConfirmEmail() {
    return confirmEmail;
  }
  
  public void setConfirmEmail(String confirmEmail) {
    this.confirmEmail = confirmEmail;
  }
  
  public String getPhoneType() {
    return phoneType;
  }
  
  public void setPhoneType(String phoneType) {
    this.phoneType = phoneType;
  }
  
  public String getDayPhoneExt() {
    return dayPhoneExt;
  }
  
  public void setDayPhoneExt(String dayPhoneExt) {
    this.dayPhoneExt = dayPhoneExt;
  }
  
  public String getDeliveryPhoneNumber() {
    return deliveryPhoneNumber;
  }
  
  public void setDeliveryPhoneNumber(String deliveryPhoneNumber) {
    this.deliveryPhoneNumber = deliveryPhoneNumber;
  }
  
  public String getCountryUSACode() {
    return countryUSACode;
  }
  
  public void setCountryUSACode(String countryUSACode) {
    this.countryUSACode = countryUSACode;
  }
  
  public String getYearArray() {
    return yearArray;
  }
  
  public void setYearArray(String yearArray) {
    this.yearArray = yearArray;
  }
  
  public String getSignature() {
    return signature;
  }
  
  public void setSignature(String signature) {
    this.signature = signature;
  }
  
  public boolean getFlgPOBox() {
    return flgPOBox;
  }
  
  public void setFlgPOBox(boolean flgPOBox) {
    this.flgPOBox = flgPOBox;
  }
}
