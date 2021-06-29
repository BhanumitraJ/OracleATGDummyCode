/*
 * Created on Jan 27, 2005
 * 
 * This class contains the data passed by the POS system. It will have the information of the customer who made a purchase against the Gift Registry in Store i.e Bill To Customer. This class gets
 * populated for every order message received from GROPS application to process POS orders.
 */
package com.nm.commerce.giftRegistry.pos;

import java.util.*;

/**
 * @author nmskr1
 */

public class BillToCustomer {
  protected String externalCustomerId;
  protected String cmosCustomerId;
  protected String prefixCode;
  protected String firstName;
  protected String middleName;
  protected String lastName;
  protected String suffixCode;
  protected String issueType;
  protected String issueId;
  protected String issueOrigin;
  protected String dateBirth;
  protected String lineOne;
  protected String lineTwo;
  protected String lineThree;
  protected String city;
  protected String stateCode;
  protected String zipCode;
  protected String countryCode;
  protected String eMail;
  protected String dayPhone;
  protected String eveningPhone;
  
  private String xml;
  
  public BillToCustomer() {}
  
  public BillToCustomer(HashMap<String, String> billToCustomerData) {
    setValue(billToCustomerData);
  }
  
  protected void setValue(HashMap<String, String> billToCustomerData) {
    Set<String> keys = billToCustomerData.keySet();
    Iterator<String> i = keys.iterator();
    while (i.hasNext()) {
      String key = (String) i.next();
      
      if (key.equals("external_customer_id"))
        setExternalCustomerId((String) billToCustomerData.get(key));
      else if (key.equals("cmos_customer_id"))
        setCmosCustomerId((String) billToCustomerData.get(key));
      else if (key.equals("prefix_code"))
        setPrefixCode((String) billToCustomerData.get(key));
      else if (key.equals("first_name"))
        setFirstName((String) billToCustomerData.get(key));
      else if (key.equals("middle_name"))
        setMiddleName((String) billToCustomerData.get(key));
      else if (key.equals("last_name"))
        setLastName((String) billToCustomerData.get(key));
      else if (key.equals("suffix_code"))
        setSuffixCode((String) billToCustomerData.get(key));
      else if (key.equals("issue_type"))
        setIssueType((String) billToCustomerData.get(key));
      else if (key.equals("issue_id"))
        setIssueId((String) billToCustomerData.get(key));
      else if (key.equals("issue_origin"))
        setIssueOrigin((String) billToCustomerData.get(key));
      else if (key.equals("date_of_birth"))
        setDateBirth((String) billToCustomerData.get(key));
      else if (key.equals("email"))
        setEmail((String) billToCustomerData.get(key));
      else if (key.equals("line1"))
        setLineOne((String) billToCustomerData.get(key));
      else if (key.equals("line2"))
        setLineTwo((String) billToCustomerData.get(key));
      else if (key.equals("line3"))
        setLineThree((String) billToCustomerData.get(key));
      else if (key.equals("city"))
        setCity((String) billToCustomerData.get(key));
      else if (key.equals("state_code"))
        setStateCode((String) billToCustomerData.get(key));
      else if (key.equals("zip_code"))
        setZipCode((String) billToCustomerData.get(key));
      else if (key.equals("day_phone"))
        setZipCode((String) billToCustomerData.get(key));
      else if (key.equals("evening_phone"))
        setZipCode((String) billToCustomerData.get(key));
      else if (key.equals("country_code")) setCountryCode((String) billToCustomerData.get(key));
    }
  }
  
  public void setExternalCustomerId(String externalCustomerId) {
    this.externalCustomerId = externalCustomerId;
  }
  
  public String getExternalCustomerId() {
    return this.externalCustomerId;
  }
  
  public void setCmosCustomerId(String cmosCustomerId) {
    this.cmosCustomerId = cmosCustomerId;
  }
  
  public String getCmosCustomerId() {
    return this.cmosCustomerId;
  }
  
  public void setPrefixCode(String prefixCode) {
    this.prefixCode = prefixCode;
  }
  
  public String getPrefixCode() {
    return this.prefixCode;
  }
  
  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }
  
  public String getFirstName() {
    return this.firstName;
  }
  
  public void setMiddleName(String middleName) {
    this.middleName = middleName;
  }
  
  public String getMiddleName() {
    return middleName;
  }
  
  public void setLastName(String lastName) {
    this.lastName = lastName;
  }
  
  public String getLastName() {
    return this.lastName;
  }
  
  public void setIssueType(String issueType) {
    this.issueType = issueType;
  }
  
  public String getIssueType() {
    return this.issueType;
  }
  
  public void setIssueId(String issueId) {
    this.issueId = issueId;
  }
  
  public String getIssueId() {
    return this.issueId;
  }
  
  public void setIssueOrigin(String issueOrigin) {
    this.issueOrigin = issueOrigin;
  }
  
  public String getIssueOrigin() {
    return this.issueOrigin;
  }
  
  public void setDateBirth(String dateBirth) {
    this.dateBirth = dateBirth;
  }
  
  public String getDateBirth() {
    return this.dateBirth;
  }
  
  public void setSuffixCode(String suffixCode) {
    this.suffixCode = suffixCode;
  }
  
  public String getSuffixCode() {
    return this.suffixCode;
  }
  
  public void setLineOne(String lineOne) {
    this.lineOne = lineOne;
  }
  
  public String getLineOne() {
    return this.lineOne;
  }
  
  public void setLineTwo(String lineTwo) {
    this.lineTwo = lineTwo;
  }
  
  public String getLineTwo() {
    return this.lineTwo;
  }
  
  public void setLineThree(String lineThree) {
    this.lineThree = lineThree;
  }
  
  public String getLineThree() {
    return this.lineThree;
  }
  
  public void setCity(String city) {
    this.city = city;
  }
  
  public String getCity() {
    return city;
  }
  
  public void setStateCode(String stateCode) {
    this.stateCode = stateCode;
  }
  
  public String getStateCode() {
    return this.stateCode;
  }
  
  public void setZipCode(String zipCode) {
    this.zipCode = zipCode;
  }
  
  public String getZipCode() {
    return this.zipCode;
  }
  
  public void setCountryCode(String countryCode) {
    this.countryCode = countryCode;
  }
  
  public String getCountryCode() {
    return this.countryCode;
  }
  
  public void setEmail(String eMail) {
    this.eMail = eMail;
  }
  
  public String getEmail() {
    return this.eMail;
  }
  
  public void setDayPhone(String dayPhone) {
    this.dayPhone = dayPhone;
  }
  
  public String getDayPhone() {
    return this.dayPhone;
  }
  
  public void setEveningPhone(String eveningPhone) {
    this.eveningPhone = eveningPhone;
  }
  
  public String getEveningPhone() {
    return this.eveningPhone;
  }
  
  /*
   * This method returns this object in an xml form.
   */
  public String getXML() throws Exception {
    try {
      this.xml =
              "<bill_to_customer" + " external_customer_id=\"" + this.externalCustomerId + "\"" + " cmos_customer_id=\"" + this.cmosCustomerId + "\"" + " prefix_code=\"" + this.prefixCode + "\""
                      + " first_name=\"" + this.firstName + "\"" + " middle_name=\"" + this.middleName + "\"" + " last_name=\"" + this.lastName + "\"" + " suffix_code=\"" + this.suffixCode + "\""
                      + " email=\"" + this.eMail + "\"" + " line1=\"" + this.lineOne + "\"" + " line2=\"" + this.lineTwo + "\"" + " line3=\"" + this.lineThree + "\"" + " city=\"" + this.city + "\""
                      + " state_code=\"" + this.stateCode + "\"" + " zip_code=\"" + this.zipCode + "\"" + " country_code=\"" + this.countryCode + "\"" + " day_phone=\"" + this.countryCode + "\""
                      + " date_of_birth=\"" + this.dateBirth + "\"" + " issue_id=\"" + this.issueId + "\"" + " issue_type=\"" + this.issueType + "\"" + " issue_origin=\"" + this.issueOrigin + "\""
                      + " evening_phone=\"" + this.countryCode + "\"" + ">" + "</bill_to_customer>";
    } catch (Exception e) {
      e.printStackTrace();
    }
    return this.xml;
  }
  
  public void reset() {
    externalCustomerId = null;
    cmosCustomerId = null;
    prefixCode = null;
    firstName = null;
    middleName = null;
    lastName = null;
    suffixCode = null;
    issueType = null;
    issueId = null;
    issueOrigin = null;
    dateBirth = null;
    lineOne = null;
    lineTwo = null;
    lineThree = null;
    city = null;
    stateCode = null;
    zipCode = null;
    countryCode = null;
    xml = null;
  }
}
