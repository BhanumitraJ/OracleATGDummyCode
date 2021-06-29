package com.nm.commerce.pagedef.model;

import java.util.List;

import com.nm.catalog.navigation.NMCategory;

/**
 * Model containing variables for use throughout a Flash Sale template page.
 */
public class FlashSaleTemplatePageModel extends TemplatePageModel {
  
  private List<NMCategory> activeFlashSaleCategories;
  private List<NMCategory> inactiveFlashSaleCategories;
  private NMCategory flashSaleCategory;
  private String categoryHeaderText;
  
  public List<NMCategory> getActiveFlashSaleCategories() {
    return activeFlashSaleCategories;
  }
  
  public void setActiveFlashSaleCategories(List<NMCategory> activeFlashSaleCategories) {
    this.activeFlashSaleCategories = activeFlashSaleCategories;
  }
  
  public List<NMCategory> getInactiveFlashSaleCategories() {
    return inactiveFlashSaleCategories;
  }
  
  public void setInactiveFlashSaleCategories(List<NMCategory> inactiveFlashSaleCategories) {
    this.inactiveFlashSaleCategories = inactiveFlashSaleCategories;
  }
  
  public NMCategory getFlashSaleCategory() {
    return flashSaleCategory;
  }
  
  public void setFlashSaleCategory(NMCategory flashSaleCategory) {
    this.flashSaleCategory = flashSaleCategory;
  }
  
  public String getCategoryHeaderText() {
    return categoryHeaderText;
  }
  
  public void setCategoryHeaderText(String categoryHeaderText) {
    this.categoryHeaderText = categoryHeaderText;
  }
  
}
