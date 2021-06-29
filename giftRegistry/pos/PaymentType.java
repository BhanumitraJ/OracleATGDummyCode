/*
 * Created on Jan 28, 2005
 * 
 * This class contains the data passed by the POS system. It will have the information of the payment type. This class gets populated for every order message received from GROPS application to process
 * POS orders.
 */
package com.nm.commerce.giftRegistry.pos;

import java.util.*;

/**
 * @author nmskr1
 */

public class PaymentType {
  private String type;
  private String account;
  private String expirationDate;
  private String amountReceived;
  private String xml;
  
  public PaymentType() {}
  
  public PaymentType(HashMap paymentTypeData) {
    setValue(paymentTypeData);
  }
  
  protected void setValue(HashMap paymentTypeData) {
    Set keys = paymentTypeData.keySet();
    Iterator i = keys.iterator();
    while (i.hasNext()) {
      String key = (String) i.next();
      
      if (key.equals("type"))
        setType((String) paymentTypeData.get(key));
      else if (key.equals("account"))
        setAccount((String) paymentTypeData.get(key));
      else if (key.equals("expiration_date"))
        setExpirationDate((String) paymentTypeData.get(key));
      else if (key.equals("amount_received")) setAmountReceived((String) paymentTypeData.get(key));
    }
  }
  
  public void setType(String type) {
    this.type = type;
  }
  
  public String getType() {
    return this.type;
  }
  
  private void setAccount(String account) {
    this.account = account;
  }
  
  private String getAccount() {
    return this.account;
  }
  
  public void setExpirationDate(String expirationDate) {
    this.expirationDate = expirationDate;
  }
  
  public String getExpirationDate() {
    return this.expirationDate;
  }
  
  public void setAmountReceived(String amountReceived) {
    this.amountReceived = amountReceived;
  }
  
  public String getAmountReceived() {
    return this.amountReceived;
  }
  
  public String getXML() throws Exception {
    try {
      this.xml =
              "<payment" + " type=\"" + this.type + "\"" + " account=\"" + this.account + "\"" + " expiration_date=\"" + this.expirationDate + "\"" + " amount_received=\"" + this.amountReceived
                      + "\"" + ">" + "</payment>";
    } catch (Exception e) {
      e.printStackTrace();
    }
    return this.xml;
  }
  
}
