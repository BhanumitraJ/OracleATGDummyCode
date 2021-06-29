package com.nm.commerce.giftlist;

public class GiftlistConstants {
  public final static String ACTIVE_GIFT_REGISTRY = "ACTIVE_GIFT_REGISTRY";
  public final static String INACTIVE_GIFT_REGISTRY = "INACTIVE_GIFT_REGISTRY";
  public final static String ACTIVE_WISH_LIST = "ACTIVE_WISH_LIST";
  public final static String INACTIVE_WISH_LIST = "INACTIVE_WISH_LIST";
  public final static String WISH_LIST = "wish_list";
  
  // ---NOTE---//
  // Event in this case refers to something that has happened to the gift list
  // for audit trail purposes. It is not the same as an event that the gift
  // list is associated with (like a wedding, baby shower, etc).
  // ----------//
  
  /**
   * Gift registry event that represents creation of a gift list
   */
  public static final String CSRGIFTREGEVENT_LISTCREATED = "CSRGIFTREGEVENT_LISTCREATED";
  /**
   * Gift registry event that represents the saving of a gift list
   */
  public static final String CSRGIFTREGEVENT_LISTSAVED = "CSRGIFTREGEVENT_LISTSAVED";
  /**
   * Gift registry event that represents the deletion of a gift list
   */
  public static final String CSRGIFTREGEVENT_LISTDELETED = "CSRGIFTREGEVENT_LISTDELETED";
  /**
   * Gift registry event that represents the activation of a gift list
   */
  public static final String CSRGIFTREGEVENT_LISTACTIVE = "CSRGIFTREGEVENT_LISTACTIVE";
  /**
   * Gift registry event that represents the deactivation of a gift list
   */
  public static final String CSRGIFTREGEVENT_LISTDEACTIVATE = "CSRGIFTREGEVENT_LISTDEACTIVE";
  /**
   * Gift registry event that represents the changing of any address information associated with the gift list
   */
  public static final String CSRGIFTREGEVENT_ADDRESSCHANGE = "CSRGIFTREGEVENT_ADDRESSCHANGE";
  /**
   * Gift registry event that represents the changing of the gift list type
   */
  public static final String CSRGIFTREGEVENT_LISTTYPECHANGE = "CSRGIFTREGEVENT_GIFTTYPECHANGE";
  /**
   * Gift registry event that represents the adding of an item to the gift list
   */
  public static final String CSRGIFTREGEVENT_ITEMADDED = "CSRGIFTREGEVENT_ITEMADDED";
  /**
   * Gift registry event that represents the removing of an item from the gift list
   */
  public static final String CSRGIFTREGEVENT_ITEMREMOVED = "CSRGIFTREGEVENT_ITEMREMOVED";
  /**
   * Gift registry event that represents the changing of the quantity of an item on the gift list
   */
  public static final String GIFTREGEVENT_ITEMQUANTCHANGED = "CSRGIFTREGEVENT_ITEMQUANTCHANGED";
  
  public static String statusCode(String stCode) {
    
    String statusCode = stCode;
    
    if (stCode.equalsIgnoreCase("VS")) {
      statusCode = "Shipped";
    }
    if (stCode.equalsIgnoreCase("CX")) {
      statusCode = "Cancelled";
    }
    if (stCode.equalsIgnoreCase("RT")) {
      statusCode = "Returned";
    }
    if (stCode.equalsIgnoreCase("BO")) {
      statusCode = "On Backorder";
    }
    if (stCode.equalsIgnoreCase("RP")) {
      statusCode = "In Process";
    }
    if (stCode.equalsIgnoreCase("PT")) {
      statusCode = "In Process";
    }
    if (stCode.equalsIgnoreCase("RI")) {
      statusCode = "In Process";
    }
    if (stCode.equalsIgnoreCase("RX")) {
      statusCode = "In Process";
    }
    if (stCode.equalsIgnoreCase("RN")) {
      statusCode = "In Process";
    }
    if (stCode.equalsIgnoreCase("CB")) {
      statusCode = "In Process";
    }
    if (stCode.equalsIgnoreCase("PL")) {
      statusCode = "In Process";
    }
    if (stCode.equalsIgnoreCase("PD")) {
      statusCode = "In Process";
    }
    if (stCode.equalsIgnoreCase("MI")) {
      statusCode = "In Process";
    }
    if (stCode.equalsIgnoreCase("DS")) {
      statusCode = "In Process";
    }
    
    return statusCode;
  }
}
