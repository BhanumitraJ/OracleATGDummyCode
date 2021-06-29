package com.nm.commerce;

import atg.repository.*;
import atg.repository.rql.*;
import atg.nucleus.GenericService;
import java.util.*;
import com.nm.utils.*;

public class VirtualGiftCard {
  
  public static final String VGC_PENDING = "PENDING";
  public static final String VGC_ACTIVE = "ACTIVE";
  public static final String VGC_DISABLED = "DISABLED";
  public static final String VGC_ACTION_ACTIVATE = "A";
  public static final String VGC_ACTION_DISABLE = "D";
  public static final String VGC_ACTION_CREATEACTIVATE = "CA";
  public static final String VGC_EMAIL_STYLE_CODE_1 = "1";
  
  
  private MutableRepositoryItem repositoryItem;
  
  public VirtualGiftCard(String data) {
    try {
      setRepositoryItem(VirtualGiftCardManager.getVirtualGiftCardManager().createVirtualGiftCardRepositoryItem());
      setStatus(VirtualGiftCard.VGC_PENDING);
      VirtualGiftCardEvent vgcEvent = new VirtualGiftCardEvent();
      vgcEvent.setEventType(VirtualGiftCardEventType.ORDER_PLACED);
      if (data != null) vgcEvent.setData(data);
      addEvent(vgcEvent);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  
  public VirtualGiftCard() {
    try {
      setRepositoryItem(VirtualGiftCardManager.getVirtualGiftCardManager().createVirtualGiftCardRepositoryItem());
      setStatus(VirtualGiftCard.VGC_PENDING);
      VirtualGiftCardEvent vgcEvent = new VirtualGiftCardEvent();
      vgcEvent.setEventType(VirtualGiftCardEventType.ORDER_PLACED);
      addEvent(vgcEvent);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  
  public VirtualGiftCard(MutableRepositoryItem repositoryItem) {
    setRepositoryItem(repositoryItem);
    try {
      if (getStatus() == null || getStatus().equals("")) {
        setStatus(VirtualGiftCard.VGC_PENDING);
      }
      if (getEvents().size() == 0) {
        VirtualGiftCardEvent vgcEvent = new VirtualGiftCardEvent();
        vgcEvent.setEventType(VirtualGiftCardEventType.ORDER_PLACED);
        addEvent(vgcEvent);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  
  public String getVgcId() {
    return (String) getRepositoryItem().getPropertyValue("vgcId");
  }
  
  public void setOrderId(String orderId) {
    if (orderId != null) orderId = orderId.toUpperCase();
    getRepositoryItem().setPropertyValue("orderId", orderId);
  }
  
  public String getOrderId() {
    return (String) getRepositoryItem().getPropertyValue("orderId");
  }
  
  public void setCommerceItemId(String commerceItemId) {
    if (commerceItemId != null) commerceItemId = commerceItemId.toUpperCase();
    getRepositoryItem().setPropertyValue("commerceItemId", commerceItemId);
  }
  
  public String getCommerceItemId() {
    return (String) getRepositoryItem().getPropertyValue("commerceItemId");
  }
  
  public void setRecipientEmail(String recipientEmail) {
    if (recipientEmail != null) recipientEmail = recipientEmail.toUpperCase();
    getRepositoryItem().setPropertyValue("recipientEmail", recipientEmail);
  }
  
  public String getRecipientEmail() {
    return (String) getRepositoryItem().getPropertyValue("recipientEmail");
  }
  
  public void setFlgPickedUp(boolean flgPickedUp) {
    getRepositoryItem().setPropertyValue("flgPickedUp", new Boolean(flgPickedUp));
  }
  
  public boolean getFlgPickedUp() {
    return ((Boolean) getRepositoryItem().getPropertyValue("flgPickedUp")).booleanValue();
  }
  
  public void setNameTo(String nameTo) {
    getRepositoryItem().setPropertyValue("nameTo", nameTo);
  }
  
  public String getNameTo() {
    return (String) getRepositoryItem().getPropertyValue("nameTo");
  }
  
  public void setNameFrom(String nameFrom) {
    getRepositoryItem().setPropertyValue("nameFrom", nameFrom);
  }
  
  public String getNameFrom() {
    return (String) getRepositoryItem().getPropertyValue("nameFrom");
  }
  
  public void setMessage(String message) {
    getRepositoryItem().setPropertyValue("message", message);
  }
  
  public String getMessage() {
    return (String) getRepositoryItem().getPropertyValue("message");
  }
  
  public void setGiftCardNumber(String giftCardNumber) {
    getRepositoryItem().setPropertyValue("giftCardNumber", EncryptDecrypt.encrypt(giftCardNumber));
  }
  
  public String getGiftCardNumber() {
    String vgcNumber = (String) getRepositoryItem().getPropertyValue("giftCardNumber");
    if (vgcNumber == null || vgcNumber.length() < 1) {
      return "";
    }
    return EncryptDecrypt.decrypt(vgcNumber);
  }
  
  public void setGiftCardAmount(String giftCardAmount) {
    getRepositoryItem().setPropertyValue("giftCardAmount", giftCardAmount);
  }
  
  public String getGiftCardAmount() {
    return (String) getRepositoryItem().getPropertyValue("giftCardAmount");
  }
  
  public void setStyleCode(String styleCode) {
    getRepositoryItem().setPropertyValue("styleCode", styleCode);
  }
  
  public String getStyleCode() {
    return (String) getRepositoryItem().getPropertyValue("styleCode");
  }
  
  public void setStatus(String status) {
    getRepositoryItem().setPropertyValue("status", status);
  }
  
  public String getStatus() {
    return (String) getRepositoryItem().getPropertyValue("status");
  }
  
  public void setEvents(List events) {
    getRepositoryItem().setPropertyValue("events", events);
  }
  
  public List getEvents() {
    List theEvents = (List) getRepositoryItem().getPropertyValue("events");
    if (theEvents == null) {
      return new ArrayList();
    } else {
      return theEvents;
    }
  }
  
  public MutableRepositoryItem getEvent(int index) throws RepositoryException {
    if (index < getEvents().size()) {
      return (MutableRepositoryItem) (getEvents().get(index));
    } else {
      throw new RepositoryException("Event at index-->" + index + "<---Does not Exist");
    }
  }
  
  public Date getOrderPlacedTimeStamp() {
    try {
      if (getEvents().size() == 0) {
        VirtualGiftCardEvent vgcEvent = new VirtualGiftCardEvent();
        vgcEvent.setEventType(VirtualGiftCardEventType.ORDER_PLACED);
        addEvent(vgcEvent);
      }
      VirtualGiftCardEvent vgcEvent = new VirtualGiftCardEvent(getEvent(0));
      return vgcEvent.getEventTimeStamp();
    } catch (Exception e) {
      return new Date();
    }
  }
  
  public void addEvent(VirtualGiftCardEvent virtualGiftCardEvent) throws RepositoryException {
    getEvents().add(virtualGiftCardEvent.getRepositoryItem());
  }
  
  public void setRepositoryItem(MutableRepositoryItem repositoryItem) {
    this.repositoryItem = repositoryItem;
  }
  
  public MutableRepositoryItem getRepositoryItem() {
    return repositoryItem;
  }
  
}
