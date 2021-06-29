/*
 * Created on May 12, 2004
 */
package com.nm.commerce.giftlist;

import atg.repository.MutableRepositoryItem;
import java.util.Date;

/**
 * @author Gerald LePage Neiman Marcus Online
 * 
 *         GiftlistEvent.java
 */
public class GiftlistEvent {
  private MutableRepositoryItem dataSource;
  
  public GiftlistEvent(MutableRepositoryItem i) {
    setDataSource(i);
  }
  
  /**
   * @return
   */
  public String getEventId() {
    return ((String) getDataSource().getPropertyValue("eventId"));
  }
  
  /**
   * @return
   */
  public Date getEventTimestamp() {
    return ((Date) getDataSource().getPropertyValue("eventTimestamp"));
  }
  
  /**
   * @return
   */
  public String getEventType() {
    return ((String) getDataSource().getPropertyValue("eventType"));
  }
  
  /**
   * Get the gift registry event data property
   * 
   * @return
   */
  public String getData() {
    return ((String) getDataSource().getPropertyValue("data"));
  }
  
  /**
   * Set the timestamp property
   * 
   * @param t
   * @return
   */
  public void setEventTimestamp(Date t) {
    getDataSource().setPropertyValue("eventTimestamp", t);
  }
  
  /**
   * Sets the timestamp property of this item to right now
   */
  public void setEventTimestampToNow() {
    // Create a new date object that represents the time as of right now
    Date d = new Date();
    
    // Set that in this object
    setEventTimestamp(d);
  }
  
  /**
   * @param t
   * @return
   */
  public void setEventType(String t) {
    getDataSource().setPropertyValue("eventType", t);
  }
  
  /**
   * @param d
   * @return
   */
  public void setData(String d) {
    getDataSource().setPropertyValue("data", d);
  }
  
  /**
   * @return
   */
  public MutableRepositoryItem getDataSource() {
    return dataSource;
  }
  
  /**
   * @param item
   */
  public void setDataSource(MutableRepositoryItem item) {
    dataSource = item;
  }
}
