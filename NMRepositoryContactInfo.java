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

import atg.commerce.order.RepositoryContactInfo;
import atg.repository.MutableRepositoryItem;
import atg.repository.RepositoryItem;

import com.nm.utils.AddressNickname;

@SuppressWarnings("serial")
public class NMRepositoryContactInfo extends RepositoryContactInfo {
  public String getSuffixCode() {
    return (String) mRepositoryItem.getPropertyValue("suffixCode");
  }
  
  public void setSuffixCode(String pSuffixCode) {
    mRepositoryItem.setPropertyValue("suffixCode", pSuffixCode);
  }
  
  public String getTitleCode() {
    return (String) mRepositoryItem.getPropertyValue("titleCode");
  }
  
  public void setTitleCode(String pTitleCode) {
    mRepositoryItem.setPropertyValue("titleCode", pTitleCode);
  }
  
  public String getFirmName() {
    return (String) mRepositoryItem.getPropertyValue("firmName");
  }
  
  public void setFirmName(String pFirmName) {
    mRepositoryItem.setPropertyValue("firmName", pFirmName);
  }
  
  public String getDayPhoneExt() {
    return (String) mRepositoryItem.getPropertyValue("dayPhoneExt");
  }
  
  public void setDayPhoneExt(String pDayPhoneExt) {
    mRepositoryItem.setPropertyValue("dayPhoneExt", pDayPhoneExt);
  }
  
  public String getEvePhoneNumber() {
    return (String) mRepositoryItem.getPropertyValue("evePhoneNumber");
  }
  
  public void setEvePhoneNumber(String pEvePhoneNumber) {
    mRepositoryItem.setPropertyValue("evePhoneNumber", pEvePhoneNumber);
  }
  
  public String getEvePhoneExt() {
    return (String) mRepositoryItem.getPropertyValue("evePhoneExt");
  }
  
  public void setEvePhoneExt(String pEvePhoneExt) {
    mRepositoryItem.setPropertyValue("evePhoneExt", pEvePhoneExt);
  }
  
  public String getProvince() {
    return (String) mRepositoryItem.getPropertyValue("province");
  }
  
  public void setProvince(String province) {
    mRepositoryItem.setPropertyValue("province", province);
  }
  
  public Boolean getFlgPOBox() {
    return (Boolean) mRepositoryItem.getPropertyValue("flgPOBox");
  }
  
  public void setFlgPOBox(Boolean pFlgPOBox) {
    mRepositoryItem.setPropertyValue("flgPOBox", pFlgPOBox);
  }
  
  public String getDeliveryPhoneNumber() {
    return (String) mRepositoryItem.getPropertyValue("deliveryPhoneNumber");
  }
  
  public void setDeliveryPhoneNumber(String pDeliveryPhoneNumber) {
    mRepositoryItem.setPropertyValue("deliveryPhoneNumber", pDeliveryPhoneNumber);
  }
  
  public Boolean getFlgGiftReg() {
    return (Boolean) mRepositoryItem.getPropertyValue("flgGiftReg");
  }
  
  public void setFlgGiftReg(Boolean pFlgGiftReg) {
    mRepositoryItem.setPropertyValue("flgGiftReg", pFlgGiftReg);
  }
  
  public String getTransGiftRegId() {
    return (String) mRepositoryItem.getPropertyValue("transGiftRegId");
  }
  
  public void setTransGiftRegId(String pTransGiftRegId) {
    mRepositoryItem.setPropertyValue("transGiftRegId", pTransGiftRegId);
  }
  
  public Integer getVerificationFlag() {
    return (Integer) mRepositoryItem.getPropertyValue("verificationFlag");
  }
  
  public void setVerificationFlag(Integer value) {
    mRepositoryItem.setPropertyValue("verificationFlag", value);
  }
  
  public String getAddressType() {
    return (String) mRepositoryItem.getPropertyValue("addressType");
  }
  
  public void setAddressType(String pAddressType) {
    mRepositoryItem.setPropertyValue("addressType", pAddressType);
  }
  
  public String getCountyCode() {
    return (String) mRepositoryItem.getPropertyValue("countyCode");
  }
  
  public void setCountyCode(String pCountyCode) {
    mRepositoryItem.setPropertyValue("countyCode", pCountyCode);
  }
  
  public Boolean getFlgCountyCodeVer() {
    return (Boolean) mRepositoryItem.getPropertyValue("flgCountyCodeVer");
  }
  
  public void setFlgCountyCodeVer(Boolean pFlgCountyCodeVer) {
    mRepositoryItem.setPropertyValue("flgCountyCodeVer", pFlgCountyCodeVer);
  }
  
  public String getStoreNumber() {
    return (String) mRepositoryItem.getPropertyValue("strStoreNum");
  }
  
  public void setStoreNumber(String pStoreNum) {
    mRepositoryItem.setPropertyValue("strStoreNum", pStoreNum);
  }
  
  public String getPhoneType() {
    return (String) mRepositoryItem.getPropertyValue("phoneType");
  }
  
  public void setPhoneType(String pPhoneType) {
    mRepositoryItem.setPropertyValue("phoneType", pPhoneType);
  }
  
  public Double getGeoCodeLatitude() {
    return (Double) mRepositoryItem.getPropertyValue("geoCodeLatitude");
  }
  
  public void setGeoCodeLatitude(Double pGeoCodeLatitude) {
    mRepositoryItem.setPropertyValue("geoCodeLatitude", pGeoCodeLatitude);
  }
  
  public Double getGeoCodeLongitude() {
    return (Double) mRepositoryItem.getPropertyValue("geoCodeLongitude");
  }
  
  public void setGeoCodeLongitude(Double pGeoCodeLongitude) {
    mRepositoryItem.setPropertyValue("geoCodeLongitude", pGeoCodeLongitude);
  }
  
  public String getGeoCodeTaxKey() {
    return (String) mRepositoryItem.getPropertyValue("geoCodeTaxKey");
  }
  
  public void setGeoCodeTaxKey(String pGeoCodeTaxKey) {
    mRepositoryItem.setPropertyValue("geoCodeTaxKey", pGeoCodeTaxKey);
  }
  
  public Boolean getGeoCodeRefreshFlag() {
    return (Boolean) mRepositoryItem.getPropertyValue("geoCodeRefreshFlag");
  }
  
  public void setGeoCodeRefreshFlag(Boolean pGeoCodeRefreshFlag) {
    mRepositoryItem.setPropertyValue("geoCodeRefreshFlag", pGeoCodeRefreshFlag);
  }
  
  public Date getLastGeoCodeReqDate() {
    return (Date) mRepositoryItem.getPropertyValue("lastGeoCodeReqDate");
  }
  
  public void setLastGeoCodeReqDate(Date pLastGeoCodeReqDate) {
    mRepositoryItem.setPropertyValue("lastGeoCodeReqDate", pLastGeoCodeReqDate);
  }
  
  public String getShipToStoreNumber() {
    return (String) mRepositoryItem.getPropertyValue("shipToStoreNumber");
  }
  
  public void setShipToStoreNumber(String shipToStoreNumber) {
    mRepositoryItem.setPropertyValue("shipToStoreNumber", shipToStoreNumber);
  }
  
  public String getCpf() {
    return (String) mRepositoryItem.getPropertyValue("cpf");
  }
  
  public void setCpf(String cpf) {
    mRepositoryItem.setPropertyValue("cpf", cpf);
  }
  
  public static void copyProfileContactInfo(RepositoryItem srcRI, MutableRepositoryItem destRI) {
    if (srcRI != null && destRI != null) {
      destRI.setPropertyValue("titleCode", (String) srcRI.getPropertyValue("titleCode"));
      destRI.setPropertyValue("firstName", (String) srcRI.getPropertyValue("firstName"));
      destRI.setPropertyValue("middleName", (String) srcRI.getPropertyValue("middleName"));
      destRI.setPropertyValue("lastName", (String) srcRI.getPropertyValue("lastName"));
      destRI.setPropertyValue("suffixCode", (String) srcRI.getPropertyValue("suffixCode"));
      destRI.setPropertyValue("firmName", (String) srcRI.getPropertyValue("firmName"));
      destRI.setPropertyValue("address1", (String) srcRI.getPropertyValue("address1"));
      destRI.setPropertyValue("address2", (String) srcRI.getPropertyValue("address2"));
      destRI.setPropertyValue("address3", (String) srcRI.getPropertyValue("address3"));
      destRI.setPropertyValue("city", (String) srcRI.getPropertyValue("city"));
      destRI.setPropertyValue("postalCode", (String) srcRI.getPropertyValue("postalCode"));
      destRI.setPropertyValue("phoneNumber", (String) srcRI.getPropertyValue("phoneNumber"));
      destRI.setPropertyValue("dayPhoneExt", (String) srcRI.getPropertyValue("dayPhoneExt"));
      destRI.setPropertyValue("evePhoneNumber", (String) srcRI.getPropertyValue("evePhoneNumber"));
      destRI.setPropertyValue("evePhoneExt", (String) srcRI.getPropertyValue("evePhoneExt"));
      destRI.setPropertyValue("faxNumber", (String) srcRI.getPropertyValue("faxNumber"));
      destRI.setPropertyValue("country", (String) srcRI.getPropertyValue("country"));
      destRI.setPropertyValue("flgPOBox", (Boolean) srcRI.getPropertyValue("flgPOBox"));
      destRI.setPropertyValue("country", (String) srcRI.getPropertyValue("country"));
      destRI.setPropertyValue("state", (String) srcRI.getPropertyValue("state"));
      destRI.setPropertyValue("province", (String) srcRI.getPropertyValue("province"));
      destRI.setPropertyValue("companyName", (String) srcRI.getPropertyValue("companyName"));
      destRI.setPropertyValue("faxNumber", (String) srcRI.getPropertyValue("faxNumber"));
      destRI.setPropertyValue("jobTitle", (String) srcRI.getPropertyValue("jobTitle"));
      destRI.setPropertyValue("county", (String) srcRI.getPropertyValue("county"));
      destRI.setPropertyValue("ownerId", (String) srcRI.getPropertyValue("ownerId"));
      destRI.setPropertyValue("prefix", (String) srcRI.getPropertyValue("prefix"));
      destRI.setPropertyValue("suffix", (String) srcRI.getPropertyValue("suffix"));
      destRI.setPropertyValue("verificationFlag", (Integer) srcRI.getPropertyValue("verificationFlag"));
      destRI.setPropertyValue("addressType", (String) srcRI.getPropertyValue("addressType"));
      destRI.setPropertyValue("countyCode", (String) srcRI.getPropertyValue("countyCode"));
      destRI.setPropertyValue("flgCountyCodeVer", (Boolean) srcRI.getPropertyValue("flgCountyCodeVer"));
      destRI.setPropertyValue("geoCodeLatitude", (Double) srcRI.getPropertyValue("geoCodeLatitude"));
      destRI.setPropertyValue("geoCodeLongitude", (Double) srcRI.getPropertyValue("geoCodeLongitude"));
      destRI.setPropertyValue("geoCodeTaxKey", (String) srcRI.getPropertyValue("geoCodeTaxKey"));
      destRI.setPropertyValue("geoCodeRefreshFlag", (Boolean) srcRI.getPropertyValue("geoCodeRefreshFlag"));
      destRI.setPropertyValue("lastGeoCodeReqDate", (Date) srcRI.getPropertyValue("lastGeoCodeReqDate"));
      /* shipToStoreNumber was implemented by the ship to store project */
      destRI.setPropertyValue("shipToStoreNumber", (String) srcRI.getPropertyValue("shipToStoreNumber"));
      destRI.setPropertyValue("cpf", (String) srcRI.getPropertyValue("cpf"));
      
      /*
       * strStoreNum was implemented by the pickup from store project
       * 
       * This property was throwing an error in WISH LIST Click on footer Wish List Create a Wish List New To NeimanMarcus? CLICK HERE TO START Enter Account Information, Personal Information and
       * click REGISTER Continue past address verification Choose Same as personal address, fill out required information and click SUBMIT
       */
      // destRI.setPropertyValue("strStoreNum" ,(String)srcRI.getPropertyValue("strStoreNum"));
      destRI.setPropertyValue("phoneType", (String) srcRI.getPropertyValue("phoneType"));
    }
  }
  
  public static void copyProfileContactInfo(NMRepositoryContactInfo src, NMRepositoryContactInfo dest) {
    copyProfileContactInfo(src.getRepositoryItem(), dest.getRepositoryItem());
  }
  
  public static String createNickName(RepositoryItem shippingAddress) {
    if (shippingAddress == null) {
      return null;
    }
    
    String firstName = (String) shippingAddress.getPropertyValue("firstName");
    String lastName = (String) shippingAddress.getPropertyValue("lastName");
    String address1 = (String) shippingAddress.getPropertyValue("address1");
    String city = (String) shippingAddress.getPropertyValue("city");
    String state = (String) shippingAddress.getPropertyValue("stateAddress");
    String zip = (String) shippingAddress.getPropertyValue("postalCode");
    String country = (String) shippingAddress.getPropertyValue("country");
    
    return AddressNickname.createNickname(firstName, lastName, address1, city, state, zip, country);
  }
  
} // end of class
