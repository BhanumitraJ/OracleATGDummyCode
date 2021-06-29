/*
 * <ATGCOPYRIGHT> Copyright (C) 1997-2000 Art Technology Group, Inc. All Rights Reserved. No use, copying or distribution of this work may be made except in accordance with a valid license agreement
 * from Art Technology Group. This notice must be included on all copies, modifications and derivatives of this work.
 * 
 * Art Technology Group (ATG) MAKES NO REPRESENTATIONS OR WARRANTIES ABOUT THE SUITABILITY OF THE SOFTWARE, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, OR NON-INFRINGEMENT. ATG SHALL NOT BE LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR DISTRIBUTING THIS SOFTWARE OR
 * ITS DERIVATIVES.
 * 
 * "Dynamo" is a trademark of Art Technology Group, Inc. </ATGCOPYRIGHT>
 */

package com.nm.commerce;

import java.util.Date;

import atg.repository.MutableRepositoryItem;
import atg.repository.RepositoryException;
import atg.repository.RepositoryItem;

import com.nm.profile.ProfileProperties;
import com.nm.repository.MutableRepositoryItemDecorator;
import com.nm.utils.AddressNickname;

public class NMAddress extends MutableRepositoryItemDecorator {
  
  // It is used to display registry(giftlist) name on ManageAddress as well as Ship To Multiple Addresses.
  private String label;
  
  public NMAddress(MutableRepositoryItem contactInfo) {
    super(contactInfo);
    updateNickname();
  }
  
  public String getLabel() {
    return label;
  }
  
  public void setLabel(String label) {
    this.label = label;
  }
  
  public String updateNickname() {
    String nickname = createNickName(getRepositoryItem());
    setNickname(nickname);
    return nickname;
  }
  
  public String getAddressNickname() {
    String nickname = createNickName(getRepositoryItem());
    return nickname;
  }
  
  public static String createNickName(RepositoryItem contactInfo) {
    if (contactInfo == null) {
      return null;
    }
    
    try {
      if (!contactInfo.getItemDescriptor().getItemDescriptorName().equals(ProfileProperties.Profile_Desc_contactInfo)) {
        return null;
      }
    } catch (RepositoryException e) {
      return null;
    }
    
    String firstName = (String) contactInfo.getPropertyValue(ProfileProperties.Contact_firstName);
    String lastName = (String) contactInfo.getPropertyValue(ProfileProperties.Contact_lastName);
    String address1 = (String) contactInfo.getPropertyValue(ProfileProperties.Contact_address1);
    String city = (String) contactInfo.getPropertyValue(ProfileProperties.Contact_city);
    String state = (String) contactInfo.getPropertyValue(ProfileProperties.Contact_state);
    String zip = (String) contactInfo.getPropertyValue(ProfileProperties.Contact_postalCode);
    String country = (String) contactInfo.getPropertyValue(ProfileProperties.Contact_country);
    
    return AddressNickname.createNickname(firstName, lastName, address1, city, state, zip, country);
  }
  
  @Override
  public void setRepositoryItem(RepositoryItem repositoryItem) {
    try {
      String type = repositoryItem.getItemDescriptor().getItemDescriptorName();
      if (!type.equals(ProfileProperties.Profile_Desc_contactInfo)) {
        throw new IllegalArgumentException(String.format("repository item must be of type '%s' was '%s'", ProfileProperties.Profile_Desc_contactInfo, type));
      }
    } catch (RepositoryException e) {
      throw new RuntimeException(e);
    }
    
    super.setRepositoryItem(repositoryItem);
  }
  
  // printPropertyMethods()
  public String getAddressType() {
    return (String) getPropertyValue("addressType");
  }
  
  public void setAddressType(String addressType) {
    setPropertyValue("addressType", addressType);
  }
  
  public String getPostalCode() {
    return (String) getPropertyValue("postalCode");
  }
  
  public void setPostalCode(String postalCode) {
    setPropertyValue("postalCode", postalCode);
  }
  
  public String getPrefix() {
    return (String) getPropertyValue("prefix");
  }
  
  public void setPrefix(String prefix) {
    setPropertyValue("prefix", prefix);
  }
  
  public String getCity() {
    return (String) getPropertyValue("city");
  }
  
  public void setCity(String city) {
    setPropertyValue("city", city);
  }
  
  public String getCountry() {
    return (String) getPropertyValue("country");
  }
  
  public void setCountry(String country) {
    setPropertyValue("country", country);
  }
  
  public String getPhoneNumber() {
    return (String) getPropertyValue("phoneNumber");
  }
  
  public void setPhoneNumber(String phoneNumber) {
    setPropertyValue("phoneNumber", phoneNumber);
  }
  
  public String getLastName() {
    return (String) getPropertyValue("lastName");
  }
  
  public void setLastName(String lastName) {
    setPropertyValue("lastName", lastName);
  }
  
  public String getCountyCode() {
    return (String) getPropertyValue("countyCode");
  }
  
  public void setCountyCode(String countyCode) {
    setPropertyValue("countyCode", countyCode);
  }
  
  public String getTitleCode() {
    return (String) getPropertyValue("titleCode");
  }
  
  public void setTitleCode(String titleCode) {
    setPropertyValue("titleCode", titleCode);
  }
  
  public String getState() {
    return (String) getPropertyValue("state");
  }
  
  public void setState(String state) {
    setPropertyValue("state", state);
  }
  
  public String getAddress3() {
    return (String) getPropertyValue("address3");
  }
  
  public void setAddress3(String address3) {
    setPropertyValue("address3", address3);
  }
  
  public String getAddress2() {
    return (String) getPropertyValue("address2");
  }
  
  public void setAddress2(String address2) {
    setPropertyValue("address2", address2);
  }
  
  public String getAddress1() {
    return (String) getPropertyValue("address1");
  }
  
  public void setAddress1(String address1) {
    setPropertyValue("address1", address1);
  }
  
  public String getJobTitle() {
    return (String) getPropertyValue("jobTitle");
  }
  
  public void setJobTitle(String jobTitle) {
    setPropertyValue("jobTitle", jobTitle);
  }
  
  public Integer getVerificationFlag() {
    return (Integer) getPropertyValue("verificationFlag");
  }
  
  public void setVerificationFlag(Integer verificationFlag) {
    setPropertyValue("verificationFlag", verificationFlag);
  }
  
  public String getAlias() {
    return (String) getPropertyValue("alias");
  }
  
  public void setAlias(String alias) {
    setPropertyValue("alias", alias);
  }
  
  public Boolean getFlgPOBox() {
    return (Boolean) getPropertyValue("flgPOBox");
  }
  
  public void setFlgPOBox(Boolean flgPOBox) {
    setPropertyValue("flgPOBox", flgPOBox);
  }
  
  public String getOwnerId() {
    return (String) getPropertyValue("ownerId");
  }
  
  public void setOwnerId(String ownerId) {
    setPropertyValue("ownerId", ownerId);
  }
  
  public Boolean getFlgCountyCodeVer() {
    return (Boolean) getPropertyValue("flgCountyCodeVer");
  }
  
  public void setFlgCountyCodeVer(Boolean flgCountyCodeVer) {
    setPropertyValue("flgCountyCodeVer", flgCountyCodeVer);
  }
  
  public String getFirstName() {
    return (String) getPropertyValue("firstName");
  }
  
  public void setFirstName(String firstName) {
    setPropertyValue("firstName", firstName);
  }
  
  public String getEvePhoneNumber() {
    return (String) getPropertyValue("evePhoneNumber");
  }
  
  public void setEvePhoneNumber(String evePhoneNumber) {
    setPropertyValue("evePhoneNumber", evePhoneNumber);
  }
  
  public String getFaxNumber() {
    return (String) getPropertyValue("faxNumber");
  }
  
  public void setFaxNumber(String faxNumber) {
    setPropertyValue("faxNumber", faxNumber);
  }
  
  public String getCompanyName() {
    return (String) getPropertyValue("companyName");
  }
  
  public void setCompanyName(String companyName) {
    setPropertyValue("companyName", companyName);
  }
  
  public String getCounty() {
    return (String) getPropertyValue("county");
  }
  
  public void setCounty(String county) {
    setPropertyValue("county", county);
  }
  
  public String getProvince() {
    return (String) getPropertyValue("province");
  }
  
  public void setProvince(String province) {
    setPropertyValue("province", province);
  }
  
  public String getId() {
    return (String) getPropertyValue("id");
  }
  
  public void setId(String id) {
    setPropertyValue("id", id);
  }
  
  public String getFirmName() {
    return (String) getPropertyValue("firmName");
  }
  
  public void setFirmName(String firmName) {
    setPropertyValue("firmName", firmName);
  }
  
  public String getNickname() {
    return (String) getPropertyValue("nickname");
  }
  
  public void setNickname(String nickname) {
    setPropertyValue("nickname", nickname);
  }
  
  public String getPhoneType() {
    return (String) getPropertyValue("phoneType");
  }
  
  public void setPhoneType(String phoneType) {
    setPropertyValue("phoneType", phoneType);
  }
  
  public String getDayPhoneExt() {
    return (String) getPropertyValue("dayPhoneExt");
  }
  
  public void setDayPhoneExt(String dayPhoneExt) {
    setPropertyValue("dayPhoneExt", dayPhoneExt);
  }
  
  public String getSuffix() {
    return (String) getPropertyValue("suffix");
  }
  
  public void setSuffix(String suffix) {
    setPropertyValue("suffix", suffix);
  }
  
  public String getMiddleName() {
    return (String) getPropertyValue("middleName");
  }
  
  public void setMiddleName(String middleName) {
    setPropertyValue("middleName", middleName);
  }
  
  public String getSuffixCode() {
    return (String) getPropertyValue("suffixCode");
  }
  
  public void setSuffixCode(String suffixCode) {
    setPropertyValue("suffixCode", suffixCode);
  }
  
  public String getEvePhoneExt() {
    return (String) getPropertyValue("evePhoneExt");
  }
  
  public void setEvePhoneExt(String evePhoneExt) {
    setPropertyValue("evePhoneExt", evePhoneExt);
  }
  
  public Double getGeoCodeLatitude() {
    return (Double) getPropertyValue("geoCodeLatitude");
  }
  
  public void setGeoCodeLatitude(Double geoCodeLatitude) {
    setPropertyValue("geoCodeLatitude", geoCodeLatitude);
  }
  
  public Double getGeoCodeLongitude() {
    return (Double) getPropertyValue("geoCodeLongitude");
  }
  
  public void setGeoCodeLongitude(Double geoCodeLongitude) {
    setPropertyValue("geoCodeLongitude", geoCodeLongitude);
  }
  
  public String getGeoCodeTaxKey() {
    return (String) getPropertyValue("geoCodeTaxKey");
  }
  
  public void setGeoCodeTaxKey(String geoCodeTaxKey) {
    setPropertyValue("geoCodeTaxKey", geoCodeTaxKey);
  }
  
  public Boolean getGeoCodeRefreshFlag() {
    return (Boolean) getPropertyValue("geoCodeRefreshFlag");
  }
  
  public void setGeoCodeRefreshFlag(Boolean geoCodeRefreshFlag) {
    setPropertyValue("geoCodeRefreshFlag", geoCodeRefreshFlag);
  }
  
  public Date getLastGeoCodeReqDate() {
    return (Date) getPropertyValue("lastGeoCodeReqDate");
  }
  
  public void setLastGeoCodeReqDate(Date lastGeoCodeReqDate) {
    setPropertyValue("lastGeoCodeReqDate", lastGeoCodeReqDate);
  }
  
}
