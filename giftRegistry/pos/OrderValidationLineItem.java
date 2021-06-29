/*
 * Created on Jan 29, 2005
 * 
 * * This class generates the xml that is sent back to GROPS for every Order processing request it makes on NMO. This represent a line item in an order.
 */
package com.nm.commerce.giftRegistry.pos;

/**
 * @author nmskr1
 */

public class OrderValidationLineItem {
  public String externalLineItemId = "";
  public String longSku = "";
  public String shortSku = "";
  public String seasonalFlag = "";
  public String cmosCatalogId = "";
  public String lineItemValidationMessage = "";
  public String xml = "";
  
  public OrderValidationLineItem() {}
  
  public void setExternalLineItemId(String externalLineItemId) {
    this.externalLineItemId = externalLineItemId;
  }
  
  public String getExternalLineItemId() {
    return this.externalLineItemId;
  }
  
  public void setLongSku(String longSku) {
    this.longSku = longSku;
  }
  
  public String getLongSku() {
    return this.longSku;
  }
  
  public void setShortSku(String shortSku) {
    this.shortSku = shortSku;
  }
  
  public String getShortSku() {
    return this.shortSku;
  }
  
  public void setSeasonalFlag(String seasonalFlag) {
    this.seasonalFlag = seasonalFlag;
  }
  
  public String getSeasonalFlag() {
    return this.seasonalFlag;
  }
  
  public void setCmosCatalogId(String cmosCatalogId) {
    this.cmosCatalogId = cmosCatalogId;
  }
  
  public String getCmosCatalogId() {
    return this.cmosCatalogId;
  }
  
  public void setLineItemValidationMessage(String lineItemValidationMessage) {
    this.lineItemValidationMessage = lineItemValidationMessage;
  }
  
  public String getLineItemValidationMessage() {
    return this.lineItemValidationMessage;
  }
  
  /*
   * This method returns this object in an xml form.
   */
  public String getXML() throws Exception {
    try {
      this.xml =
              "<line_item" + " external_line_item_id=\"" + this.externalLineItemId + "\"" + " external_sku_id=\"" + this.longSku + "\"" + " pos_sku_id=\"" + this.shortSku + "\"" + " seasonal_flag=\""
                      + this.seasonalFlag + "\"" + " cmos_catalog_id=\"" + this.cmosCatalogId + "\"" + " line_item_validation_message=\"" + this.lineItemValidationMessage + "\"" + ">"
                      + "</line_item>";
    } catch (Exception e) {
      e.printStackTrace();
    }
    return this.xml;
  }
}
