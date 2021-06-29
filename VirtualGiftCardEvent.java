package com.nm.commerce;

import atg.repository.*;
import atg.repository.rql.*;
import atg.nucleus.GenericService;
import java.util.*;

public class VirtualGiftCardEvent {
  
  private MutableRepositoryItem repositoryItem;
  
  public VirtualGiftCardEvent() {
    try {
      setRepositoryItem(VirtualGiftCardManager.getVirtualGiftCardManager().createVirtualGiftCardEventRepositoryItem());
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  
  public VirtualGiftCardEvent(MutableRepositoryItem repositoryItem) {
    setRepositoryItem(repositoryItem);
  }
  
  public String getEventId() {
    return (String) getRepositoryItem().getPropertyValue("eventId");
  }
  
  public Date getEventTimeStamp() {
    return (Date) getRepositoryItem().getPropertyValue("eventTimeStamp");
  }
  
  public void setEventType(String eventType) {
    getRepositoryItem().setPropertyValue("eventType", eventType);
  }
  
  public String getEventType() {
    return (String) getRepositoryItem().getPropertyValue("eventType");
  }
  
  public void setData(String data) {
    getRepositoryItem().setPropertyValue("data", data);
  }
  
  public String getData() {
    return (String) getRepositoryItem().getPropertyValue("data");
  }
  
  public void setRepositoryItem(MutableRepositoryItem repositoryItem) {
    this.repositoryItem = repositoryItem;
  }
  
  public MutableRepositoryItem getRepositoryItem() {
    return repositoryItem;
  }
  
}
