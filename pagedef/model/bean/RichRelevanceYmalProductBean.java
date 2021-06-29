package com.nm.commerce.pagedef.model.bean;

import java.util.List;

/**
 * this java bean class holds parsed JSON object from Richrelevance Ymal recommendation server side integration
 * 
 * @author nmjh94
 * 
 */
public class RichRelevanceYmalProductBean {
  private String viewGuid;
  private List<Placement> placements;
  
  private String status;
  
  public class YmalProduct {
    private String genre;
    private String id;
    private String clickURL;
    private List<String> categoryIds;
    private String regionPriceDescription;
    private String name;
    private String brand;
    private List<Category> categories;
    private String price;
    private String rating;
    private List<String> attributes;
    private String imageURL;
    private String priceString;
    
    public String getGenre() {
      return genre;
    }
    
    public void setGenre(String genre) {
      this.genre = genre;
    }
    
    public String getId() {
      return id;
    }
    
    public void setId(String id) {
      this.id = id;
    }
    
    public String getClickURL() {
      return clickURL;
    }
    
    public void setClickURL(String clickURL) {
      this.clickURL = clickURL;
    }
    
    public List<String> getCategoryIds() {
      return categoryIds;
    }
    
    public void setCategoryIds(List<String> categoryIds) {
      this.categoryIds = categoryIds;
    }
    
    public String getRegionPriceDescription() {
      return regionPriceDescription;
    }
    
    public void setRegionPriceDescription(String regionPriceDescription) {
      this.regionPriceDescription = regionPriceDescription;
    }
    
    public String getName() {
      return name;
    }
    
    public void setName(String name) {
      this.name = name;
    }
    
    public String getBrand() {
      return brand;
    }
    
    public void setBrand(String brand) {
      this.brand = brand;
    }
    
    public List<Category> getCategories() {
      return categories;
    }
    
    public void setCategories(List<Category> categories) {
      this.categories = categories;
    }
    
    public String getPrice() {
      return price;
    }
    
    public void setPrice(String price) {
      this.price = price;
    }
    
    public String getRating() {
      return rating;
    }
    
    public void setRating(String rating) {
      this.rating = rating;
    }
    
    public List<String> getAttributes() {
      return attributes;
    }
    
    public void setAttributes(List<String> attributes) {
      this.attributes = attributes;
    }
    
    public String getImageURL() {
      return imageURL;
    }
    
    public void setImageURL(String imageURL) {
      this.imageURL = imageURL;
    }
    
    public String getPriceString() {
      return priceString;
    }
    
    public void setPriceString(String priceString) {
      this.priceString = priceString;
    }
    
  }
  
  public class Category {
    private String categoryId;
    private String categoryName;
    
    public String getCategoryId() {
      return categoryId;
    }
    
    public void setCategoryId(String categoryId) {
      this.categoryId = categoryId;
    }
    
    public String getCategoryName() {
      return categoryName;
    }
    
    public void setCategoryName(String categoryName) {
      this.categoryName = categoryName;
    }
    
  }
  
  public class Placement {
    private String placement;
    private List<YmalProduct> products;
    private String htmlElementId;
    private String placementType;
    private String strategyMessage;
    
    public String getPlacement() {
      return placement;
    }
    
    public void setPlacement(String placement) {
      this.placement = placement;
    }
    
    public List<YmalProduct> getProducts() {
      return products;
    }
    
    public void setProducts(List<YmalProduct> products) {
      this.products = products;
    }
    
    public String getHtmlElementId() {
      return htmlElementId;
    }
    
    public void setHtmlElementId(String htmlElementId) {
      this.htmlElementId = htmlElementId;
    }
    
    public String getPlacementType() {
      return placementType;
    }
    
    public void setPlacementType(String placementType) {
      this.placementType = placementType;
    }
    
    public String getStrategyMessage() {
      return strategyMessage;
    }
    
    public void setStrategyMessage(String strategyMessage) {
      this.strategyMessage = strategyMessage;
    }
    
  }
  
  public String getViewGuid() {
    return viewGuid;
  }
  
  public void setViewGuid(String viewGuid) {
    this.viewGuid = viewGuid;
  }
  
  public List<Placement> getPlacements() {
    return placements;
  }
  
  public void setPlacements(List<Placement> placements) {
    this.placements = placements;
  }
  
  public String getStatus() {
    return status;
  }
  
  public void setStatus(String status) {
    this.status = status;
  }
}
