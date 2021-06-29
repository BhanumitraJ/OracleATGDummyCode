package com.nm.commerce.pagedef.model;

import com.nm.common.StoreAndRestaurantDetails;

public class StorePageModel extends PageModel {
  
  private String storeIds;
  private int resultCount;
  private String geoLocation;
  private double latitude;
  private double longitude;
  private long radius;
  private String prop60;
  private String storeName;
  private StoreAndRestaurantDetails storeDetails;
  private String cityAndRegion;
  private String searchString;
  
  public String getProp60() {
    return prop60;
  }
  
  public void setProp60(final String prop60) {
    this.prop60 = prop60;
  }
  
  public String getGeoLocation() {
    return geoLocation;
  }
  
  public void setGeoLocation(final String geoLocation) {
    this.geoLocation = geoLocation;
  }
  
  public String getStoreIds() {
    return storeIds;
  }
  
  public int getResultCount() {
    return resultCount;
  }
  
  public void setResultCount(final int resultCount) {
    this.resultCount = resultCount;
  }
  
  public void setStoreIds(final String storeIds) {
    this.storeIds = storeIds;
  }
  
  public double getLatitude() {
    return latitude;
  }
  
  public void setLatitude(final double latitude) {
    this.latitude = latitude;
  }
  
  public double getLongitude() {
    return longitude;
  }
  
  public void setLongitude(final double longitude) {
    this.longitude = longitude;
  }
  
  public long getRadius() {
    return radius;
  }
  
  public void setRadius(final long radius) {
    this.radius = radius;
  }
  
  public String getStoreName() {
    return storeName;
  }
  
  public void setStoreName(final String storeName) {
    this.storeName = storeName;
  }
  
  public StoreAndRestaurantDetails getStoreDetails() {
    return storeDetails;
  }
  
  public void setStoreDetails(final StoreAndRestaurantDetails storeDetails) {
    this.storeDetails = storeDetails;
  }
	/**
	 * @return the cityAndRegion
	 */
	public String getCityAndRegion() {
		return cityAndRegion;
	}
	/**
	 * @param cityAndRegion
	 *            the cityAndRegion to set
	 */
	public void setCityAndRegion(String cityAndRegion) {
		this.cityAndRegion = cityAndRegion;
	}

	/**
	 * @return the searchString
	 */
	public String getSearchString() {
		return searchString;
	}

	/**
	 * @param searchString the searchString to set
	 */
	public void setSearchString(String searchString) {
		this.searchString = searchString;
	}
}
