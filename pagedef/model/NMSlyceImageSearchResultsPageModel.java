package com.nm.commerce.pagedef.model;

import java.util.ArrayList;
import java.util.List;

import com.nm.commerce.catalog.NMProduct;

/**
 * 
 * This is a model class for Slyce image search results page
 * 
 */
public class NMSlyceImageSearchResultsPageModel extends TemplatePageModel {
  private List<NMProduct> displayableProducts = new ArrayList<NMProduct>();
  
  public List<NMProduct> getDisplayableProducts() {
    return displayableProducts;
  }
  
  public void setDisplayableProducts(final List<NMProduct> displayableProducts) {
    this.displayableProducts = displayableProducts;
  }
}
