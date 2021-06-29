package com.nm.commerce.pagedef.model;

import java.util.List;
import java.util.Map;

import atg.repository.RepositoryItem;

import com.nm.common.StoreAndRestaurantDetails;

public class RestaurantPageModel extends PageModel {
  private String restId;
  private String restTitle;
  private String desc;
  private List<RepositoryItem> childInfo;
  private List<RepositoryItem> childContent;
  private RepositoryItem address;
  private Map<String, String> restaurantHours;
  private Map<String, String> seoHourMap;
  private List<RepositoryItem> holidayHours;
  private String longDescription;
  private String menu;
  private String managerName;
  private String reservation;
  private String restDescription;
  private boolean displayExtendedHours;
  private String directionsUrl;
  private String storeDir;
  private String storeName;
  private StoreAndRestaurantDetails restaurantDetails;
  private String privateEvent;
  
  public String getRestId() {
    return restId;
  }
  
  public void setRestId(final String restId) {
    this.restId = restId;
  }
  
  public String getRestTitle() {
    return restTitle;
  }
  
  public void setRestTitle(final String restTitle) {
    this.restTitle = restTitle;
  }
  
  public String getDesc() {
    return desc;
  }
  
  public void setDesc(final String desc) {
    this.desc = desc;
  }
  
  public List<RepositoryItem> getChildInfo() {
    return childInfo;
  }
  
  public void setChildInfo(final List<RepositoryItem> childInfo) {
    this.childInfo = childInfo;
  }
  
  public List<RepositoryItem> getChildContent() {
    return childContent;
  }
  
  public void setChildContent(final List<RepositoryItem> childContent) {
    this.childContent = childContent;
  }
  
  public RepositoryItem getAddress() {
    return address;
  }
  
  public void setAddress(final RepositoryItem address) {
    this.address = address;
  }
  
  public List<RepositoryItem> getHolidayHours() {
    return holidayHours;
  }
  
  public void setHolidayHours(final List<RepositoryItem> holidayHours) {
    this.holidayHours = holidayHours;
  }
  
  public Map<String, String> getRestaurantHours() {
    return restaurantHours;
  }
  
  public void setRestaurantHours(final Map<String, String> restaurantHours) {
    this.restaurantHours = restaurantHours;
  }
  
  public String getLongDescription() {
    return longDescription;
  }
  
  public void setLongDescription(final String longDescription) {
    this.longDescription = longDescription;
  }
  
  public String getMenu() {
    return menu;
  }
  
  public void setMenu(final String menu) {
    this.menu = menu;
  }
  
  public String getManagerName() {
    return managerName;
  }
  
  public void setManagerName(final String managerName) {
    this.managerName = managerName;
  }
  
  public String getReservation() {
    return reservation;
  }
  
  public void setReservation(final String reservation) {
    this.reservation = reservation;
  }
  
  public String getRestDescription() {
    return restDescription;
  }
  
  public void setRestDescription(final String restDescription) {
    this.restDescription = restDescription;
  }
  
  public boolean isDisplayExtendedHours() {
    return displayExtendedHours;
  }
  
  public void setDisplayExtendedHours(final boolean displayExtendedHours) {
    this.displayExtendedHours = displayExtendedHours;
  }
  
  public String getDirectionsUrl() {
    return directionsUrl;
  }
  
  public void setDirectionsUrl(final String directionsUrl) {
    this.directionsUrl = directionsUrl;
  }
  
  public String getStoreDir() {
    return storeDir;
  }
  
  public void setStoreDir(final String storeDir) {
    this.storeDir = storeDir;
  }
  
  public String getStoreName() {
    return storeName;
  }
  
  public void setStoreName(final String storeName) {
    this.storeName = storeName;
  }
  
  public StoreAndRestaurantDetails getRestaurantDetails() {
    return restaurantDetails;
  }
  
  public void setRestaurantDetails(final StoreAndRestaurantDetails restaurantDetails) {
    this.restaurantDetails = restaurantDetails;
  }
  
  public Map<String, String> getSeoHourMap() {
    return seoHourMap;
  }
  
  public void setSeoHourMap(final Map<String, String> seoHourMap) {
    this.seoHourMap = seoHourMap;
  }
  
  public String getPrivateEvent() {
    return privateEvent;
  }
  
  public void setPrivateEvent(String privateEvent) {
    this.privateEvent = privateEvent;
  }
  
}
