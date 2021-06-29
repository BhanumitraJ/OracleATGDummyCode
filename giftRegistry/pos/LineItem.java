/*
 * Created on Jan 27, 2005
 * 
 * This class contains the data passed by the POS system. It will have the information of one line item of an order sent from the POS. This class values are set when an Take/Send/Return/Order message
 * is sent from GROPS application to NMO.
 */
package com.nm.commerce.giftRegistry.pos;

import java.util.*;

/**
 * @author nmskr1
 */

public class LineItem {
  private String transactionType = "";
  private String cmosLineItemId = "";
  private String externalLineItemId = "";
  private String longSku = ""; // external_sku_id
  private String shortSku = ""; // pos_sku_id
  private String cmosCatalogId = "";
  private String cmosCatalogItem = "";
  private String cmosSkuId = "";
  private String quantity = "";
  private String serviceLevelCode = "";
  private String giftWrapCode = "";
  private String priceEach = "";
  private String taxEach = "";
  private String otherEach = "";
  private String freightEach = "";
  private String arriveByDate = "";
  private String currentSatus = "";
  private String currentStatusDate = "";
  private String trackingNumber = "";
  private String giftMessageOne = "";
  private String giftMessageTwo = "";
  private String xml = "";
  
  public LineItem() {}
  
  public LineItem(HashMap lineItemData) {
    setValue(lineItemData);
  }
  
  private void setValue(HashMap lineItemData) {
    Set keys = lineItemData.keySet();
    Iterator i = keys.iterator();
    while (i.hasNext()) {
      String key = (String) i.next();
      
      if (key.equals("transaction_type"))
        setTransactionType((String) lineItemData.get(key));
      else if (key.equals("cmos_line_item_id"))
        setCmosLineItemId((String) lineItemData.get(key));
      else if (key.equals("external_line_item_id"))
        setExternalLineItemId((String) lineItemData.get(key));
      else if (key.equals("pos_sku_id"))
        this.setShortSku((String) lineItemData.get(key));
      else if (key.equals("external_sku_id"))
        setLongSku((String) lineItemData.get(key));
      else if (key.equals("cmos_catalog_id"))
        setCmosCatalogId((String) lineItemData.get(key));
      else if (key.equals("cmos_catalog_item"))
        setCmosCatalogItem((String) lineItemData.get(key));
      else if (key.equals("cmos_sku_id"))
        setCmosSkuId((String) lineItemData.get(key));
      else if (key.equals("quantity"))
        setQuantity((String) lineItemData.get(key));
      else if (key.equals("service_level_code"))
        setServiceLevelCode((String) lineItemData.get(key));
      else if (key.equals("gift_wrap_code"))
        setGiftWrapCode((String) lineItemData.get(key));
      else if (key.equals("price_each"))
        setPriceEach((String) lineItemData.get(key));
      else if (key.equals("tax_each"))
        setTaxEach((String) lineItemData.get(key));
      else if (key.equals("other_each"))
        setOtherEach((String) lineItemData.get(key));
      else if (key.equals("freight_each"))
        setFreightEach((String) lineItemData.get(key));
      else if (key.equals("arrive_by_date"))
        setArriveByDate((String) lineItemData.get(key));
      else if (key.equals("other_each"))
        setOtherEach((String) lineItemData.get(key));
      else if (key.equals("current_status"))
        setCurrentSatus((String) lineItemData.get(key));
      else if (key.equals("current_status_date"))
        setCurrentStatusDate((String) lineItemData.get(key));
      else if (key.equals("tracking_number"))
        setTrackingNumber((String) lineItemData.get(key));
      else if (key.equals("gift_message1"))
        setGiftMessageOne((String) lineItemData.get(key));
      else if (key.equals("gift_message2")) setGiftMessageTwo((String) lineItemData.get(key));
    }
  }
  
  public void setTransactionType(String transactionType) {
    this.transactionType = transactionType;
  }
  
  public String getTransactionType() {
    return this.transactionType;
  }
  
  public void setCmosLineItemId(String cmosLineItemId) {
    this.cmosLineItemId = cmosLineItemId;
  }
  
  public String getCmosLineItemId() {
    return this.cmosLineItemId;
  }
  
  public void setExternalLineItemId(String externalLineItemId) {
    this.externalLineItemId = externalLineItemId;
  }
  
  public String getExternalLineItemId() {
    return this.externalLineItemId;
  }
  
  public void setLongSku(String longSku) {
    if (longSku.indexOf("-") > 0) longSku = stripCharacters(longSku);
    
    this.longSku = longSku;
  }
  
  public String getLongSku() {
    return this.longSku;
  }
  
  /**
   * This method strips '-' from any string passed to it.
   * 
   * @param value
   *          The value to be stripped
   * @return String The formatted value.
   **/
  public String stripCharacters(String value) {
    String valueReturned = "";
    if (value != null && value.trim().length() > 0) {
      StringTokenizer st = new StringTokenizer(value, "-");
      while (st.hasMoreTokens()) {
        valueReturned = valueReturned + st.nextToken();
      }
    }
    return valueReturned;
  }
  
  public void setShortSku(String shortSku) {
    this.shortSku = shortSku;
  }
  
  public String getShortSku() {
    return this.shortSku;
  }
  
  public void setCmosCatalogId(String cmosCatalogId) {
    this.cmosCatalogId = cmosCatalogId;
  }
  
  public String getCmosCatalogId() {
    return this.cmosCatalogId;
  }
  
  public void setCmosCatalogItem(String cmosCatalogItem) {
    this.cmosCatalogItem = cmosCatalogItem;
  }
  
  public String getCmosCatalogItem() {
    return this.cmosCatalogItem;
  }
  
  public void setCmosSkuId(String cmosSkuId) {
    this.cmosSkuId = cmosSkuId;
  }
  
  public String getCmosSkuId() {
    return this.cmosSkuId;
  }
  
  public void setQuantity(String quantity) {
    this.quantity = quantity;
  }
  
  public String getQuantity() {
    return this.quantity;
  }
  
  public void setServiceLevelCode(String serviceLevelCode) {
    this.serviceLevelCode = serviceLevelCode;
  }
  
  public String getServiceLevelCode() {
    return this.serviceLevelCode;
  }
  
  public void setGiftWrapCode(String giftWrapCode) {
    this.giftWrapCode = giftWrapCode;
  }
  
  public String getGiftWrapCode() {
    return this.giftWrapCode;
  }
  
  public void setPriceEach(String priceEach) {
    this.priceEach = priceEach;
  }
  
  public String getPriceEach() {
    return this.priceEach;
  }
  
  public void setTaxEach(String taxEach) {
    this.taxEach = taxEach;
  }
  
  public String getTaxEach() {
    return this.taxEach;
  }
  
  public void setOtherEach(String otherEach) {
    this.otherEach = otherEach;
  }
  
  public String getOtherEach() {
    return this.otherEach;
  }
  
  public void setFreightEach(String freightEach) {
    this.freightEach = freightEach;
  }
  
  public String getFreightEach() {
    return this.freightEach;
  }
  
  public void setArriveByDate(String arriveByDate) {
    this.arriveByDate = arriveByDate;
  }
  
  public String getArriveByDate() {
    return this.arriveByDate;
  }
  
  public void setCurrentSatus(String currentSatus) {
    this.currentSatus = currentSatus;
  }
  
  public String getCurrentSatus() {
    return this.currentSatus;
  }
  
  public void setCurrentStatusDate(String currentStatusDate) {
    this.currentStatusDate = currentStatusDate;
  }
  
  public String getCurrentStatusDate() {
    return this.currentStatusDate;
  }
  
  public void setTrackingNumber(String trackingNumber) {
    this.trackingNumber = trackingNumber;
  }
  
  public String getTrackingNumber() {
    return this.trackingNumber;
  }
  
  public void setGiftMessageOne(String giftMessageOne) {
    this.giftMessageOne = giftMessageOne;
  }
  
  public String getGiftMessageOne() {
    return this.giftMessageOne;
  }
  
  public void setGiftMessageTwo(String giftMessageTwo) {
    this.giftMessageTwo = giftMessageTwo;
  }
  
  public String getGiftMessageTwo() {
    return this.giftMessageTwo;
  }
  
  /*
   * This method returns this object in an xml form.
   */
  public String getXML() throws Exception {
    try {
      this.xml =
              "<line_item" + " transaction_type=\"" + this.transactionType + "\"" + " cmos_line_item_id=\"" + this.cmosLineItemId + "\"" + " external_line_item_id=\"" + this.externalLineItemId + "\""
                      + " external_sku_id=\"" + this.longSku + "\"" + " pos_sku_id=\"" + this.shortSku + "\"" + " cmos_catalog_id=\"" + this.cmosCatalogId + "\"" + " cmos_catalog_item=\""
                      + this.cmosCatalogItem + "\"" + " cmos_sku_id=\"" + this.cmosSkuId + "\"" + " quantity=\"" + this.quantity + "\"" + " service_level_code=\"" + this.serviceLevelCode + "\""
                      + " gift_wrap_code=\"" + this.giftWrapCode + "\"" + " price_each=\"" + this.priceEach + "\"" + " tax_each=\"" + this.taxEach + "\"" + " other_each=\"" + this.otherEach + "\""
                      + " freight_each=\"" + this.freightEach + "\"" + " arrive_by_date=\"" + this.arriveByDate + "\"" + " current_status=\"" + this.currentSatus + "\"" + " current_status_date=\""
                      + this.currentStatusDate + "\"" + " tracking_number=\"" + this.trackingNumber + "\"" + " gift_message1=\"" + this.giftMessageOne + "\"" + " gift_message2=\""
                      + this.giftMessageTwo + "\"" + ">" + "</line_item>";
    } catch (Exception e) {
      e.printStackTrace();
    }
    return this.xml;
  }
  
  /*
   * This method resets the values
   */
  public void reset() {
    transactionType = null;
    cmosLineItemId = null;
    externalLineItemId = null;
    longSku = null; // external_sku_id
    cmosCatalogId = null;
    cmosCatalogItem = null;
    cmosSkuId = null;
    quantity = null;
    serviceLevelCode = null;
    giftWrapCode = null;
    priceEach = null;
    taxEach = null;
    otherEach = null;
    freightEach = null;
    arriveByDate = null;
    currentSatus = null;
    currentStatusDate = null;
    trackingNumber = null;
    giftMessageOne = null;
    giftMessageTwo = null;
    xml = null;
  }
  
}
