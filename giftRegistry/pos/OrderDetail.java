/*
 * Created on Jan 28, 2005
 * 
 * This class contains the data passed by the POS system. It will have the information of the Ship To Customer and the Line Item of the order. This class gets populated for every line item in the
 * order message received from GROPS application to process POS orders.
 */
package com.nm.commerce.giftRegistry.pos;

import java.util.*;

/**
 * @author nmskr1
 */

public class OrderDetail {
  private ShipToCustomer shipToCustomer;
  private String xml;
  
  public OrderDetail() {}
  
  public OrderDetail(HashMap orderDetailData) {
    setValue(orderDetailData);
  }
  
  private void setValue(HashMap orderDetailData) {
    Set keys = orderDetailData.keySet();
    Iterator i = keys.iterator();
    while (i.hasNext()) {
      String key = (String) i.next();
      
      if (key.equals("ship_to_customer")) shipToCustomer = new ShipToCustomer((HashMap) orderDetailData.get(key));
    }
  }
  
  public void setShipToCustomer(ShipToCustomer shipToCustomer) {
    this.shipToCustomer = shipToCustomer;
  }
  
  public ShipToCustomer getShipToCustomer() {
    return this.shipToCustomer;
  }
  
  public String getXML() throws Exception {
    try {
      this.xml = "<order_detail>" + this.shipToCustomer.getXML() + "</order_detail>";
    } catch (Exception e) {
      e.printStackTrace();
    }
    return this.xml;
  }
  
  private void reset() {
    this.shipToCustomer.reset();
  }
}
