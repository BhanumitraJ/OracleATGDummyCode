package com.nm.commerce;

public class VirtualGiftCardEventType // 1 2 3 4
{ // 1234567890123456789012345678901234567890
  public static final String ORDER_PLACED = "ORDER PLACED";
  public static final String ORDER_CANCELLED = "ORDER CANCELLED";
  public static final String GIFTCARD_ACTIVATE = "GIFTCARD ACTIVATED";
  public static final String GIFTCARD_DEACTIVATE = "GIFTCARD DEACTIVATED";
  public static final String SENT_EMAIL_WEB = "EMAIL SENT BY WEB";
  public static final String SENT_EMAIL_CSR = "EMAIL SENT BY CSR";
  public static final String CARD_RETRIEVED = "CARD PICKED UP";
  public static final String CARD_RETRIEVAL_FAIL = "CARD PICKUP FAILURE";
  public static final String CARD_LINEITEM_CANCELLED = "CARD LINE ITEM CANCELLED";
  public static final String CARD_LINEITEM_CANCELLED_PEND = "CARD LINE ITEM CANCELLED - PENDING";
  public static final String CARD_LINEITEM_RETURNED = "CARD LINE ITEM RETURNED";
  
  public VirtualGiftCardEventType() {}
  
}
