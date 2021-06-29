/*
 * Created on Jan 27, 2005
 * 
 * This class contains the data passed by the POS system. It will have the information of the customer whom the item is shipped to i.e Ship To Customer. This class gets populated for every line itme
 * in the order message received from GROPS application to process POS orders.
 */
package com.nm.commerce.giftRegistry.pos;

import java.util.*;

/**
 * @author nmskr1
 */

public class ShipToCustomer extends BillToCustomer {
  private Vector lineItems = new Vector();
  private String xml;
  
  public ShipToCustomer() {
    super();
  }
  
  public ShipToCustomer(HashMap shipToCustomer) {
    // This is to populate the fields in the parent class.
    super.setValue(shipToCustomer);
    // This is to populate the fields from this class.
    this.setValue(shipToCustomer);
  }
  
  /*
   * This setValue method is slightly different from other classes setValue methods. Since there can be more then one line item in the xml that we receive from GROPS, when we initially parese the xml
   * into HashMap, I used a number appended to it before adding it to the hashmap. I did it because the HashMap cannot have two keys with the same value pointing to different line items. So for
   * example the keys would look like line_item0 if there is only one or line_item0, line_item1 etc. See the parseElements() method of the OrderMessage class to get an idea as to how the HashMap gets
   * populated.
   */
  protected void setValue(HashMap shipToCustomer) {
    Iterator i = shipToCustomer.keySet().iterator();
    String key = null;
    
    while (i.hasNext()) {
      key = (String) i.next();
      if (key.startsWith("line_item")) {
        LineItem lineItem = new LineItem((HashMap) shipToCustomer.get(key));
        addLineItem(lineItem);
      }
    }
  }
  
  public void addLineItem(LineItem lineItem) {
    lineItems.add(lineItem);
  }
  
  public Vector getLineItems() {
    return this.lineItems;
  }
  
  public String getXML() throws Exception {
    String allLineItemsXml = "";
    for (int i = 0; i < lineItems.size(); i++)
      allLineItemsXml = allLineItemsXml + ((LineItem) lineItems.elementAt(i)).getXML();
    
    try {
      this.xml =
              "<ship_to_customer" + " external_customer_id=\"" + externalCustomerId + "\"" + " cmos_customer_id=\"" + cmosCustomerId + "\"" + " prefix_code=\"" + prefixCode + "\"" + " first_name=\""
                      + firstName + "\"" + " middle_name=\"" + middleName + "\"" + " last_name=\"" + lastName + "\"" + " suffix_code=\"" + suffixCode + "\"" + " line1=\"" + lineOne + "\""
                      + " line2=\"" + lineTwo + "\"" + " line3=\"" + lineThree + "\"" + " city=\"" + city + "\"" + " state_code=\"" + stateCode + "\"" + " zip_code=\"" + zipCode + "\""
                      + " country_code=\"" + countryCode + "\"" + ">" + allLineItemsXml + "</ship_to_customer>";
    } catch (Exception e) {
      e.printStackTrace();
    }
    return this.xml;
  }
  
  public void reset() {
    super.reset();
    
    for (int i = 0; i < lineItems.size(); i++)
      ((LineItem) lineItems.elementAt(i)).reset();
    
    lineItems = new Vector();
    xml = null;
  }
}
