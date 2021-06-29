package com.nm.commerce.pagedef.model;

import java.util.List;
import java.util.Map;

import com.nm.collections.DesignerIndexer.Designer;

public class EndecaDesignerIndexPageModel extends EndecaDrivenPageModel {
  
  private Boolean isByCategory;
  private String rootCategoryId;
  private String categoryGroupCode;
  private String categoryName;
  private String requestedIndex;
  private String designerIndexVendorColumn;
  private Boolean useSiloSpecificPromos;
  private boolean indexHasSubCats;
  private Map<String, List<com.nm.collections.DesignerIndexer.Designer>> alphaDesignerMap;
  
  public void setIndexHasSubCats(boolean value) {
    indexHasSubCats = value;
  }
  
  public boolean getIndexHasSubCats() {
    return indexHasSubCats;
  }
  
  public void setIsByCategory(Boolean isByCategory) {
    this.isByCategory = isByCategory;
  }
  
  public Boolean getIsByCategory() {
    return isByCategory;
  }
  
  public void setRootCategoryId(String rootCategoryId) {
    this.rootCategoryId = rootCategoryId;
  }
  
  public String getRootCategoryId() {
    return rootCategoryId;
  }
  
  public void setCategoryGroupCode(String categoryGroupCode) {
    this.categoryGroupCode = categoryGroupCode;
  }
  
  public String getCategoryGroupCode() {
    return categoryGroupCode;
  }
  
  public void setCategoryName(String categoryName) {
    this.categoryName = categoryName;
  }
  
  public String getCategoryName() {
    return categoryName;
  }
  
  public void setRequestedIndex(String requestedIndex) {
    this.requestedIndex = requestedIndex;
  }
  
  public String getRequestedIndex() {
    return requestedIndex;
  }
  
  public String getDesignerIndexVendorColumn() {
    return designerIndexVendorColumn;
  }
  
  public void setDesignerIndexVendorColumn(String designerIndexVendorColumn) {
    this.designerIndexVendorColumn = designerIndexVendorColumn;
  }
  
  public Boolean getUseSiloSpecificPromos() {
    return useSiloSpecificPromos;
  }
  
  public void setUseSiloSpecificPromos(Boolean useSiloSpecificPromos) {
    this.useSiloSpecificPromos = useSiloSpecificPromos;
  }
  
  public Map<String, List<Designer>> getAlphaDesignerMap() {
    return alphaDesignerMap;
  }
  
  public void setAlphaDesignerMap(Map<String, List<Designer>> alphaDesignerMap) {
    this.alphaDesignerMap = alphaDesignerMap;
  }
  
}
