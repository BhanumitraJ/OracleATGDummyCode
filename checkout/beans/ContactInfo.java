package com.nm.commerce.checkout.beans;

import java.util.Date;

import com.nm.tms.core.TMSAjaxResponse;

public class ContactInfo extends TMSAjaxResponse {
  public String getContactAddressName() {
    return (contactAddressName);
  }
  
  public void setContactAddressName(final String contactAddressName) {
    this.contactAddressName = contactAddressName;
  }
  
  public int getAddressType() {
    return addressType;
  }
  
  public void setAddressType(int value) {
    addressType = value;
  }
  
  public int getVerificationFlag() {
    return verificationFlag;
  }
  
  public void setVerificationFlag(int value) {
    verificationFlag = value;
  }
  
  /**
   * Add to support common interface with ATG contact info repository items
   * 
   * @return
   */
  public String getAddress1() {
    return addressLine1;
  }
  
  public String getAddressLine1() {
    return (addressLine1);
  }
  
  public void setAddressLine1(String addressLine1) {
    this.addressLine1 = addressLine1;
  }
  
  /**
   * Add to support common interface with ATG contact info repository items
   * 
   * @return
   */
  public String getAddress2() {
    return addressLine2;
  }
  
  public String getAddressLine2() {
    return (addressLine2);
  }
  
  public void setAddressLine2(String addressLine2) {
    this.addressLine2 = addressLine2;
  }
  
  public String getCity() {
    return (city);
  }
  
  public void setCity(String city) {
    this.city = city;
  }
  
  public String getCompanyName() {
    return (companyName);
  }
  
  public void setCompanyName(String companyName) {
    this.companyName = companyName;
  }
  
  public String getCountry() {
    return (country);
  }
  
  public void setCountry(String country) {
    this.country = country;
  }
  
  public String getProvince() {
    return (province);
  }
  
  public void setProvince(String province) {
    this.province = province;
  }
  
  public String getDayTelephone() {
    return (dayTelephone);
  }
  
  /**
   * Add to support common interface with ATG contact info repository items
   * 
   * @return
   */
  public String getPhoneNumber() {
    return dayTelephone;
  }
  
  public void setDayTelephone(String dayTelephone) {
    this.dayTelephone = dayTelephone;
  }
  
  public String getDayTelephoneExt() {
    return (dayTelephoneExt);
  }
  
  public void setDayTelephoneExt(String dayTelephoneExt) {
    this.dayTelephoneExt = dayTelephoneExt;
  }
  
  /**
   * Add to support common interface with ATG contact info repository items
   * 
   * @return
   */
  public String getEvePhoneNumber() {
    return eveningTelephone;
  }
  
  public String getEveningTelephone() {
    return (eveningTelephone);
  }
  
  public void setEveningTelephone(String eveningTelephone) {
    this.eveningTelephone = eveningTelephone;
  }
  
  public String getFirstName() {
    return (firstName);
  }
  
  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }
  
  public String getLastName() {
    return (lastName);
  }
  
  public void setLastName(String lastName) {
    this.lastName = lastName;
  }
  
  public String getPoBox() {
    return (poBox);
  }
  
  public void setPoBox(String poBox) {
    this.poBox = poBox;
  }
  
  public String getState() {
    return (state);
  }
  
  public void setState(String state) {
    this.state = state;
  }
  
  public String getEmailAddress() {
    return emailAddress;
  }
  
  public void setEmailAddress(String emailAddress) {
    this.emailAddress = emailAddress;
  }
  
  public String getSuffixCode() {
    return (suffixCode);
  }
  
  public void setSuffixCode(String suffixCode) {
    this.suffixCode = suffixCode;
  }
  
  public String getTitleCode() {
    return (titleCode);
  }
  
  public void setTitleCode(String titleCode) {
    this.titleCode = titleCode;
  }
  
  /**
   * Add to support common interface with ATG contact info repository items
   * 
   * @return
   */
  public String getPostalCode() {
    return zip;
  }
  
  public String getZip() {
    return (zip);
  }
  
  public void setZip(String zip) {
    this.zip = zip;
  }
  
  public String getId() {
    return id;
  }
  
  public void setId(String id) {
    this.id = id;
  }
  
  public String getVerAddressType() {
    return verAddressType;
  }
  
  public void setVerAddressType(String value) {
    verAddressType = value;
  }
  
  public String getVerCountyCode() {
    return verCountyCode;
  }
  
  public void setVerCountyCode(String value) {
    verCountyCode = value;
  }
  
  public Boolean getFlgCountyCodeVer() {
    return flgCountyCodeVer;
  }
  
  public void setFlgCountyCodeVer(Boolean value) {
    flgCountyCodeVer = value;
  }
  
  public boolean getUpdateWishlistAddressFlag() {
    return updateWishlistAddressFlag;
  }
  
  public void setUpdateWishlistAddressFlag(boolean flg) {
    updateWishlistAddressFlag = flg;
  }
  
  public String getAlias() {
    return alias;
  }
  
  public void setAlias(String alias) {
    this.alias = alias;
  }
  
  public boolean getMakeDefaultAddressFlag() {
    return makeDefaultAddressFlag;
  }
  
  public void setMakeDefaultAddressFlag(boolean flg) {
    this.makeDefaultAddressFlag = flg;
  }
  
  public void setPhoneType(String phoneType) {
    this.phoneType = phoneType;
  }
  
  public String getPhoneType() {
    return phoneType;
  }
  
  public boolean getUpdateDefaultShippingAddressFlag() {
    return updateDefaultShippingAddressFlag;
  }
  
  public void setUpdateDefaultShippingAddressFlag(boolean flg) {
    updateDefaultShippingAddressFlag = flg;
  }
  
  public String getAddressVerificationKey() {
    return addressVerificationKey;
  }
  
  public void setAddressVerificationKey(String addressVerificationKey) {
    this.addressVerificationKey = addressVerificationKey;
  }
  
  public boolean isSkipEmailValidation() {
    return skipEmailValidation;
  }
  
  public void setSkipEmailValidation(boolean skipEmailValidation) {
    this.skipEmailValidation = skipEmailValidation;
  }
  
  public boolean isSkipBillingAddressValidation() {
    return skipBillingAddressValidation;
  }
  
  public void setSkipBillingAddressValidation(boolean skipBillingAddressValidation) {
    this.skipBillingAddressValidation = skipBillingAddressValidation;
  }
  
  public boolean isValidationOnly() {
    return validationOnly;
  }
  
  public void setValidationOnly(boolean validationOnly) {
    this.validationOnly = validationOnly;
  }
  
  public Double getGeoCodeLatitude() {
    return geoCodeLatitude;
  }
  
  public void setGeoCodeLatitude(Double geoCodeLatitude) {
    this.geoCodeLatitude = geoCodeLatitude;
  }
  
  public Double getGeoCodeLongitude() {
    return geoCodeLongitude;
  }
  
  public void setGeoCodeLongitude(Double geoCodeLongitude) {
    this.geoCodeLongitude = geoCodeLongitude;
  }
  
  public String getGeoCodeTaxKey() {
    return geoCodeTaxKey;
  }
  
  public void setGeoCodeTaxKey(String geoCodeTaxKey) {
    this.geoCodeTaxKey = geoCodeTaxKey;
  }
  
  public Boolean isGeoCodeRefreshFlag() {
    return geoCodeRefreshFlag;
  }
  
  public void setGeoCodeRefreshFlag(Boolean geoCodeRefreshFlag) {
    this.geoCodeRefreshFlag = geoCodeRefreshFlag;
  }
  
  public Date getLastGeoCodeReqDate() {
    return lastGeoCodeReqDate;
  }
  
  public void setLastGeoCodeReqDate(Date lastGeoCodeReqDate) {
    this.lastGeoCodeReqDate = lastGeoCodeReqDate;
  }
  
  /**
   * @return the county
   */
  public String getCounty() {
    return county;
  }
  
  /**
   * @param county
   *          the county to set
   */
  public void setCounty(String county) {
    this.county = county;
  }
  
  /**
   * @return the zipcodeOptional
   */
  public boolean isZipcodeOptional() {
    return zipcodeOptional;
  }
  
  /**
   * @param zipcodeOptional
   *          the zipcodeOptional to set
   */
  public void setZipcodeOptional(boolean zipcodeOptional) {
    this.zipcodeOptional = zipcodeOptional;
  }
  
  public void copyFrom(ContactInfo contactInfo) {
    contactAddressName = contactInfo.contactAddressName;
    addressLine1 = contactInfo.addressLine1;
    addressLine2 = contactInfo.addressLine2;
    city = contactInfo.city;
    companyName = contactInfo.companyName;
    country = contactInfo.country;
    province = contactInfo.province;
    dayTelephone = contactInfo.dayTelephone;
    dayTelephoneExt = contactInfo.dayTelephoneExt;
    eveningTelephone = contactInfo.eveningTelephone;
    firstName = contactInfo.firstName;
    lastName = contactInfo.lastName;
    poBox = contactInfo.poBox;
    state = contactInfo.state;
    suffixCode = contactInfo.suffixCode;
    titleCode = contactInfo.titleCode;
    zip = contactInfo.zip;
    id = contactInfo.id;
    verificationFlag = contactInfo.verificationFlag;
    addressType = contactInfo.addressType;
    verAddressType = contactInfo.verAddressType;
    verCountyCode = contactInfo.verCountyCode;
    flgCountyCodeVer = contactInfo.flgCountyCodeVer;
    makeDefaultAddressFlag = contactInfo.makeDefaultAddressFlag;
    alias = contactInfo.alias;
    phoneType = contactInfo.phoneType;
    updateDefaultShippingAddressFlag = contactInfo.updateDefaultShippingAddressFlag;
    addressVerificationKey = contactInfo.addressVerificationKey;
    skipBillingAddressValidation = contactInfo.skipBillingAddressValidation;
    skipEmailValidation = contactInfo.skipEmailValidation;
    validationOnly = contactInfo.validationOnly;
    cpf = contactInfo.cpf;
    geoCodeLatitude = contactInfo.geoCodeLatitude;
    geoCodeLongitude = contactInfo.geoCodeLongitude;
    geoCodeTaxKey = contactInfo.geoCodeTaxKey;
    geoCodeRefreshFlag = contactInfo.geoCodeRefreshFlag;
    lastGeoCodeReqDate = contactInfo.lastGeoCodeReqDate;
    cpf = contactInfo.cpf;
    county = contactInfo.county;
    zipcodeOptional = contactInfo.zipcodeOptional;
  }
  
  private String contactAddressName;
  private String addressLine1;
  private String addressLine2;
  private String city;
  private String companyName;
  private String country;
  private String province;
  private String dayTelephone;
  private String dayTelephoneExt;
  private String eveningTelephone;
  private String firstName;
  private String lastName;
  private String poBox;
  private String state;
  private String emailAddress;
  private String suffixCode;
  private String titleCode;
  private String zip;
  private String id;
  private int verificationFlag;
  private int addressType;
  private String verAddressType;
  private String verCountyCode;
  private Boolean flgCountyCodeVer;
  private boolean updateWishlistAddressFlag;
  private boolean makeDefaultAddressFlag;
  private String alias;
  private String phoneType;
  private boolean updateDefaultShippingAddressFlag;
  private String addressVerificationKey;
  private boolean skipEmailValidation;
  private boolean skipBillingAddressValidation;
  private boolean validationOnly;
  private Double geoCodeLatitude;
  private Double geoCodeLongitude;
  private String geoCodeTaxKey;
  private Boolean geoCodeRefreshFlag;
  private Date lastGeoCodeReqDate;
  private boolean zipcodeOptional;
  
  // Internationalization changes
  private String cpf;
  private String county;
  
  public String getCpf() {
    return cpf;
  }
  
  public void setCpf(String cpf) {
    this.cpf = cpf;
  }
}
