/*
 * Created on Jan 29, 2005
 * 
 * This class generates the xml that is sent back to GROPS for every Order processing request it makes on NMO.
 */
package com.nm.commerce.giftRegistry.pos;

import java.util.*;

/**
 * @author nmskr1
 */
public class OrderValidation {
  private String storeNumber = "";
  private String terminalId = "";
  private String transactionId = "";
  private String orderDate = "";
  private String registryId = "";
  private String orderType = "";
  private String registryValidationMessage = "";
  private Vector lineItems = new Vector();
  private String xml;
  
  public OrderValidation() {}
  
  public void setStoreNumber(String storeNumber) {
    this.storeNumber = storeNumber;
  }
  
  public String getStoreNumber() {
    return storeNumber;
  }
  
  public void setTerminalId(String terminalId) {
    this.terminalId = terminalId;
  }
  
  public String getTerminalId() {
    return this.terminalId;
  }
  
  public void setTransactionId(String transactionId) {
    this.transactionId = transactionId;
  }
  
  public String getTransactionId() {
    return this.transactionId;
  }
  
  public void setOrderDate(String orderDate) {
    this.orderDate = orderDate;
  }
  
  public String getOrderDate() {
    return orderDate;
  }
  
  public void setRegistryId(String registryId) {
    this.registryId = registryId;
  }
  
  public String getRegistryId() {
    return this.registryId;
  }
  
  public void setOrderType(String orderType) {
    this.orderType = orderType;
  }
  
  public String getOrderType() {
    return this.orderType;
  }
  
  public void setRegistryValidationMessage(String registryValidationMessage) {
    this.registryValidationMessage = registryValidationMessage;
  }
  
  public String getRegistryValidationMessage() {
    return this.registryValidationMessage;
  }
  
  public void setLineItems(OrderValidationLineItem orderValidationLineItem) {
    lineItems.add(orderValidationLineItem);
  }
  
  public Vector getLineItems() {
    return lineItems;
  }
  
  public String getXML() throws Exception {
    String allLineItemsXml = "";
    for (int i = 0; i < lineItems.size(); i++)
      allLineItemsXml = allLineItemsXml + ((OrderValidationLineItem) lineItems.elementAt(i)).getXML();
    
    try {
      this.xml =
              "<?xml version=\"1.0\" encoding=\"utf-8\"?>" + "<order_validation" + " store_number=\"" + this.storeNumber + "\"" + " terminal_id=\"" + this.terminalId + "\"" + " transaction_id=\""
                      + this.transactionId + "\"" + " order_date=\"" + this.orderDate + "\"" + " registry_id=\"" + this.registryId + "\"" + " order_type=\"" + this.orderType + "\""
                      + " registry_validation_Message=\"" + this.registryValidationMessage + "\"" + ">" + allLineItemsXml + "</order_validation>";
    } catch (Exception e) {
      e.printStackTrace();
    }
    return this.xml;
  }
  
}
