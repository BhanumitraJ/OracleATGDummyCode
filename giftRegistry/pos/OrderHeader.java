/*
 * Created on Jan 28, 2005
 * 
 * This class contains the data passed by the POS system. It will have the Order header information. This class gets populated for every order message received from GROPS application to process POS
 * orders.
 */
package com.nm.commerce.giftRegistry.pos;

import java.util.*;

/**
 * @author nmskr1
 */

public class OrderHeader {
  private String cmosOrderNumber;
  private String externalOrderNumber;
  private String orderDate;
  private String orderType;
  private String storeNumber;
  private String registryId;
  private String employeePin;
  private String associateLastName;
  private String terminalId;
  private String transactionId;
  private String deliveryInstructions;
  private String xml;
  
  public OrderHeader() {}
  
  public OrderHeader(HashMap orderHeaderData) {
    setValue(orderHeaderData);
  }
  
  private void setValue(HashMap paymentTypeData) {
    Set keys = paymentTypeData.keySet();
    Iterator i = keys.iterator();
    while (i.hasNext()) {
      String key = (String) i.next();
      
      if (key.equals("cmos_order_number"))
        setCmosOrderNumber((String) paymentTypeData.get(key));
      else if (key.equals("external_order_number"))
        setExternalOrderNumber((String) paymentTypeData.get(key));
      else if (key.equals("order_date"))
        setOrderDate((String) paymentTypeData.get(key));
      else if (key.equals("order_type"))
        setOrderType((String) paymentTypeData.get(key));
      else if (key.equals("store_number"))
        setStoreNumber((String) paymentTypeData.get(key));
      else if (key.equals("registry_id"))
        setRegistryId((String) paymentTypeData.get(key));
      else if (key.equals("employee_pin"))
        setEmployeePin((String) paymentTypeData.get(key));
      else if (key.equals("associate_lname"))
        setAssociateLastName((String) paymentTypeData.get(key));
      else if (key.equals("terminal_id"))
        setTerminalId((String) paymentTypeData.get(key));
      else if (key.equals("transaction_id"))
        setTransactionId((String) paymentTypeData.get(key));
      else if (key.equals("delivery_instructions")) setDeliveryInstructions((String) paymentTypeData.get(key));
    }
  }
  
  public void setCmosOrderNumber(String cmosOrderNumber) {
    this.cmosOrderNumber = cmosOrderNumber;
  }
  
  public String getCmosOrderNumber() {
    return this.cmosOrderNumber;
  }
  
  public void setExternalOrderNumber(String externalOrderNumber) {
    this.externalOrderNumber = externalOrderNumber;
  }
  
  public String getExternalOrderNumber() {
    return this.externalOrderNumber;
  }
  
  public void setOrderDate(String orderDate) {
    this.orderDate = orderDate;
  }
  
  public String getOrderDate() {
    return orderDate;
  }
  
  public void setOrderType(String orderType) {
    this.orderType = orderType;
  }
  
  public String getOrderType() {
    return this.orderType;
  }
  
  public void setStoreNumber(String storeNumber) {
    this.storeNumber = storeNumber;
  }
  
  public String getStoreNumber() {
    return this.storeNumber;
  }
  
  public void setRegistryId(String registryId) {
    this.registryId = registryId;
  }
  
  public String getRegistryId() {
    return registryId;
  }
  
  public void setEmployeePin(String employeePin) {
    this.employeePin = employeePin;
  }
  
  public String getEmployeePin() {
    return employeePin;
  }
  
  public void setAssociateLastName(String associateLastName) {
    this.associateLastName = associateLastName;
  }
  
  public String getAssociateLastName() {
    return associateLastName;
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
  
  public void setDeliveryInstructions(String deliveryInstructions) {
    this.deliveryInstructions = deliveryInstructions;
  }
  
  public String getDeliveryInstructions() {
    return this.deliveryInstructions;
  }
  
  public String getXML() throws Exception {
    try {
      this.xml =
              "<order_header" + " cmos_order_number=\"" + this.cmosOrderNumber + "\"" + " external_order_number=\"" + this.externalOrderNumber + "\"" + " order_date=\"" + this.orderDate + "\""
                      + " order_type=\"" + this.orderType + "\"" + " store_number=\"" + this.storeNumber + "\"" + " registry_id=\"" + this.registryId + "\"" + " employee_pin=\"" + this.employeePin
                      + "\"" + " associate_lname=\"" + this.associateLastName + "\"" + " terminal_id=\"" + this.terminalId + "\"" + " transaction_id=\"" + this.transactionId + "\""
                      + " delivery_instructions=\"" + this.deliveryInstructions + "\"" + ">" + "</order_header>";
    } catch (Exception e) {
      e.printStackTrace();
    }
    return this.xml;
  }
}
