package com.nm.commerce.catalog;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import atg.repository.RepositoryItem;

import com.nm.catalog.navigation.NMCategory;
import com.nm.droplet.FilterCategoryGroup;

public class DynamicChildProductsCategory extends NMCategory {
  
  private static final long serialVersionUID = 1L;
  private List<RepositoryItem> childProducts;
  
  public DynamicChildProductsCategory(NMCategory item) {
    this(item.getRepositoryItem(), item.getParentId());
  }
  
  public DynamicChildProductsCategory(RepositoryItem item) {
    this(item, new ArrayList<RepositoryItem>());
  }
  
  public DynamicChildProductsCategory(RepositoryItem item, List<RepositoryItem> childProducts) {
    super(item);
    setChildProducts(childProducts);
  }
  
  public DynamicChildProductsCategory(RepositoryItem item, String parentId) {
    this(item, parentId, new ArrayList<RepositoryItem>());
  }
  
  public DynamicChildProductsCategory(RepositoryItem item, String parentId, List<RepositoryItem> childProducts) {
    super(item, parentId);
    setChildProducts(childProducts);
  }
  
  public DynamicChildProductsCategory(RepositoryItem item, String parentId, FilterCategoryGroup fcg) {
    this(item, parentId, fcg, new ArrayList<RepositoryItem>());
  }
  
  public DynamicChildProductsCategory(RepositoryItem item, String parentId, FilterCategoryGroup fcg, ArrayList<RepositoryItem> childProducts) {
    super(item, parentId, fcg);
    setChildProducts(childProducts);
  }
  
  @Override
  public List<RepositoryItem> getChildProducts() {
    return childProducts;
  }
  
  public void setChildProducts(List<RepositoryItem> childProducts) {
    this.childProducts = childProducts;
  }
  
  public void addChildProducts(Collection<RepositoryItem> childProducts) {
    this.childProducts.addAll(childProducts);
  }
  
  public void addChildProducts(RepositoryItem ... childProducts) {
    for (RepositoryItem product : childProducts) {
      this.childProducts.add(product);
    }
  }
  
  public void removeChildProducts(Collection<RepositoryItem> childProducts) {
    this.childProducts.removeAll(childProducts);
  }
  
  public void removeChildProducts(RepositoryItem ... childProducts) {
    for (RepositoryItem product : childProducts) {
      this.childProducts.remove(product);
    }
  }
}
