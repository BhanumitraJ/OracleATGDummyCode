/*
 * Created on Jan 28, 2005
 * 
 * This class contains the Order data passed by the POS system. This class gets populated for every order message received from GROPS application to process POS orders.
 */
package com.nm.commerce.giftRegistry.pos;

import java.util.*;
import java.io.File;
import java.io.*;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * @author nmskr1
 */
public class OrderMessage {
  private String sourceSystem;
  private String targetSystem;
  private OrderHeader orderHeader;
  private BillToCustomer billToCustomer;
  private OrderDetail orderDetail;
  private PaymentType paymentType;
  private String xml;
  
  public OrderMessage() {}
  
  public OrderMessage(HashMap orderMessageData) {
    setValue(orderMessageData);
  }
  
  public OrderMessage(String xml) {
    // Get the Document object from an xml String.
    Document doc = getDocument(xml);
    
    // Get the root of the Document
    Element root = doc.getDocumentElement();
    
    // build a HashMap from the Document.
    HashMap xmlData = parseElements(root);
    
    // Instantiate the Registry Elements.
    setValue(xmlData);
  }
  
  private void setValue(HashMap orderMessageData) {
    Set keys = orderMessageData.keySet();
    Iterator i = keys.iterator();
    while (i.hasNext()) {
      String key = (String) i.next();
      
      if (key.equals("source_system"))
        setSourceSystem((String) orderMessageData.get(key));
      else if (key.equals("target_system"))
        setTargetSystem((String) orderMessageData.get(key));
      else if (key.equals("order_header"))
        orderHeader = new OrderHeader((HashMap) orderMessageData.get(key));
      else if (key.equals("bill_to_customer"))
        billToCustomer = new BillToCustomer((HashMap) orderMessageData.get(key));
      else if (key.equals("payment"))
        paymentType = new PaymentType((HashMap) orderMessageData.get(key));
      else if (key.equals("order_detail")) orderDetail = new OrderDetail((HashMap) orderMessageData.get(key));
    }
  }
  
  public void setSourceSystem(String sourceSystem) {
    this.sourceSystem = sourceSystem;
  }
  
  public String getSourceSystem() {
    return this.sourceSystem;
  }
  
  public void setTargetSystem(String targetSystem) {
    this.targetSystem = targetSystem;
  }
  
  public String getTargetSystem() {
    return targetSystem;
  }
  
  public void setOrderHeader(OrderHeader orderHeader) {
    this.orderHeader = orderHeader;
  }
  
  public OrderHeader getOrderHeader() {
    return orderHeader;
  }
  
  public void setBillToCustomer(BillToCustomer billToCustomer) {
    this.billToCustomer = billToCustomer;
  }
  
  public BillToCustomer getBillToCustomer() {
    return this.billToCustomer;
  }
  
  public void setPaymentType(PaymentType paymentType) {
    this.paymentType = paymentType;
  }
  
  public PaymentType getPaymentType() {
    return this.paymentType;
  }
  
  public void setOrderDetail(OrderDetail orderDetail) {
    this.orderDetail = orderDetail;
  }
  
  public OrderDetail getOrderDetail() {
    return orderDetail;
  }
  
  /*
   * This method returns a Document object for a given xml String.
   */
  private Document getDocument(String xml) {
    // String xmlVersionEncoding = "<?xml version='1.0' encoding='utf-8'?>\n";
    // xml = xmlVersionEncoding + xml;
    Document doc = null;
    File temp = null;
    
    try {
      DocumentBuilder builder = getDocumentBuilder();
      
      // Create a temperary file to hold the xml input received from GROPS.
      temp = File.createTempFile("input", ".xml");
      
      // if(temp != null)
      // System.out.println("Temp file creted.");
      
      // Write the xml into the temp file.
      BufferedWriter out = new BufferedWriter(new FileWriter(temp));
      out.write(xml);
      out.close();
      // System.out.println("Written the xml into the temp file.");
      
      // Create the Document object from the temp file.
      doc = builder.parse(temp);
      // if(doc != null)
      // System.out.println("Created the temp file from xml input");
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      // Delete the file later.
      temp.deleteOnExit();
    }
    return doc;
  }
  
  /*
   * This method instantiates a DocumentBuilder and returns it.
   */
  private DocumentBuilder getDocumentBuilder() {
    DocumentBuilder builder = null;
    try {
      DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
      builder = dbFactory.newDocumentBuilder();
    } catch (Exception e) {
      e.printStackTrace();
    }
    return builder;
  }
  
  /**
   * This method retrieves the attributes from the given element and populates the HashMap.
   * */
  private HashMap getAttributes(Element e) {
    HashMap hm = new HashMap();
    NamedNodeMap nnm = e.getAttributes();
    
    for (int i = 0; i < nnm.getLength(); i++) {
      Node n = nnm.item(i);
      if (n instanceof Attr) {
        Attr attribute = (Attr) n;
        // System.out.println(attribute.getName() + " = " + attribute.getValue());
        hm.put(attribute.getName(), attribute.getValue());
      }
    }
    return hm;
  }
  
  /*
   * This method parses a given element and all its child elements into a HashMap. Depending on the child elements of a given element, the returned HashMap can be a HashMap or HashMap of HashMaps. By
   * passing a root element of the xml document, we can parse the entire document into a HashMap.
   */
  private HashMap parseElements(Element e) {
    // System.out.println("\n\n PARSING ELEMENT " + e.getNodeName() + "\n\n");
    HashMap attributes = new HashMap();
    HashMap leafElement = new HashMap();
    HashMap xmlData = new HashMap();
    HashMap childData = new HashMap();
    
    // Get the attributes of the element as we travers
    // down the xml elements.
    if (e.hasAttributes()) {
      xmlData = getAttributes(e);
    }
    
    // System.out.println("Parent Node Name " + e.getNodeName());
    if (e.hasChildNodes()) {
      NodeList nl = e.getChildNodes();
      // if(e.getNodeName().equals("GIFT_LIST"))
      // System.out.println("" + nl.toString());
      for (int i = 0; i < nl.getLength(); i++) {
        Node n = nl.item(i);
        if (n instanceof Element) {
          
          // child data gets asigned value only when
          // the recursion reaches the leaf element.
          // System.out.println("ELEMENT " + e.getNodeName() + "HAS CHILDEREN \n\n");
          childData = parseElements((Element) n);
          
          // We are doing this to create a different key when
          // there are more then one GIFT element in the GIFT_LIST.
          // HashMap cannot have the same key with more then one value, it will
          // overwrite it.
          // if(n.getNodeName().equals("GIFT") || n.getNodeName().equals("EVENT_DATE"))
          if (n.getNodeName().equals("line_item")) {
            // System.out.println("Adding Node Name 1 " + n.getNodeName());
            xmlData.put(n.getNodeName() + i, childData);
          } else {
            // System.out.println("Adding Node Name 2" + n.getNodeName());
            xmlData.put(n.getNodeName(), childData);
          }
        }
      }
    } else {
      // System.out.println("Adding Node Name 3" + e.getNodeName());
      xmlData.put(e.getNodeName(), attributes);
    }
    return xmlData;
  }
  
  public String getXML() throws Exception {
    try {
      this.xml =
              "<?xml version='1.0' encoding='utf-8'?>" + "<order_message" + " source_system=\"" + this.sourceSystem + "\"" + " target_system=\"" + this.targetSystem + "\"" + ">"
                      + orderHeader.getXML() + billToCustomer.getXML() + paymentType.getXML() + orderDetail.getXML() + "</order_message>";
    } catch (Exception e) {
      e.printStackTrace();
    }
    return this.xml;
  }
}
