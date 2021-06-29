package com.nm.commerce.pagedef.model;

import java.util.List;
import java.util.Map;

import com.nm.collections.DesignerIndexer;
import com.nm.collections.DesignerIndexer.Category;
import com.nm.collections.DesignerIndexer.Designer;

public class DesignerIndexPageModel extends TemplatePageModel {
  
  private Map<String, List<Designer>> alphaDesignerMap;
  private Map<String, List<String>> alphaDesignerCountryRestrictionMap;
  private Map<String, List<Designer>> categoryDesignerMap;
  private Boolean isByCategory;
  private List<String> emptyAlphaList;
  private List<Category> designerIndexCategories;
  private String rootCategoryId;
  private String categoryGroupCode;
  private DesignerIndexer.Category activeDesignerCategory;
  private String categoryName;
  private String requestedIndex;
  private String designerIndexVendorColumn;
  private Boolean useSiloSpecificPromos;
  private boolean indexHasSubCats;
  private String seoSiloName;
  private String seoCategoryName;
  private boolean useSeoHyperlinks;
  private String currentCountryCode;  
  private List<Designer> alphaSortedFavDesigners;
  
  private String designerPromoFile;
  private boolean designerPromoOverride;
  private boolean loginStatus;
  // merchant recommended designers
  private List<Designer> recommendedDesigners;
  
  public boolean isLoginStatus() {
    return loginStatus;
  }
  
  public void setLoginStatus(boolean loginStatus) {
    this.loginStatus = loginStatus;
  }
  
  public void setIndexHasSubCats(boolean value) {
    indexHasSubCats = value;
  }
  
  public boolean getIndexHasSubCats() {
    return indexHasSubCats;
  }
  
  public Map<String, List<String>> getAlphaDesignerCountryRestrictionMap() {
    return alphaDesignerCountryRestrictionMap;
  }
  
  public void setAlphaDesignerCountryRestrictionMap(Map<String, List<String>> alphaDesignerCountryRestrictionMap) {
    this.alphaDesignerCountryRestrictionMap = alphaDesignerCountryRestrictionMap;
  }
  
  public void setAlphaDesignerMap(Map<String, List<Designer>> alphaDesignerMap) {
    this.alphaDesignerMap = alphaDesignerMap;
  }
  
  public Map<String, List<Designer>> getAlphaDesignerMap() {
    return alphaDesignerMap;
  }
  
  public void setCategoryDesignerMap(Map<String, List<Designer>> categoryDesignerMap) {
    this.categoryDesignerMap = categoryDesignerMap;
  }
  
  public Map<String, List<Designer>> getCategoryDesignerMap() {
    return categoryDesignerMap;
  }
  
  public void setIsByCategory(Boolean isByCategory) {
    this.isByCategory = isByCategory;
  }
  
  public Boolean getIsByCategory() {
    return isByCategory;
  }
  
  public void setEmptyAlphaList(List<String> emptyAlphaList) {
    this.emptyAlphaList = emptyAlphaList;
  }
  
  public List<String> getEmptyAlphaList() {
    return emptyAlphaList;
  }
  
  public void setDesignerIndexCategories(List<Category> designerIndexCategories) {
    this.designerIndexCategories = designerIndexCategories;
  }
  
  public List<Category> getDesignerIndexCategories() {
    return designerIndexCategories;
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
  
  public void setActiveDesignerCategory(DesignerIndexer.Category activeDesignerCategory) {
    this.activeDesignerCategory = activeDesignerCategory;
  }
  
  public DesignerIndexer.Category getActiveDesignerCategory() {
    return activeDesignerCategory;
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
  
  public String getSeoSiloName() {
    return seoSiloName;
  }
  
  public void setSeoSiloName(String seoSiloName) {
    this.seoSiloName = seoSiloName;
  }
  
  public String getSeoCategoryName() {
    return seoCategoryName;
  }
  
  public void setSeoCategoryName(String seoCategoryName) {
    this.seoCategoryName = seoCategoryName;
  }
  
  public boolean getUseSeoHyperlinks() {
    return useSeoHyperlinks;
  }
  
  public void setUseSeoHyperlinks(boolean useSeoHyperlinks) {
    this.useSeoHyperlinks = useSeoHyperlinks;
  }
  
  public String getDesignerPromoFile() {
    return designerPromoFile;
  }
  
  public void setDesignerPromoFile(String designerPromoFile) {
    this.designerPromoFile = designerPromoFile;
  }
  
  public boolean isDesignerPromoOverride() {
    return designerPromoOverride;
  }
  
  public void setDesignerPromoOverride(boolean designerPromoOverride) {
    this.designerPromoOverride = designerPromoOverride;
  }
  
  public String getCurrentCountryCode() {
    return currentCountryCode;
  }
  
  public void setCurrentCountryCode(String currentCountryCode) {
    this.currentCountryCode = currentCountryCode;
  }
  
  public List<Designer> getAlphaSortedFavDesigners() {
    return alphaSortedFavDesigners;
  }
  
  public void setAlphaSortedFavDesigners(List<Designer> alphaSortedFavDesigners) {
    this.alphaSortedFavDesigners = alphaSortedFavDesigners;
  }

  public List<Designer> getRecommendedDesigners() {
	  return recommendedDesigners;
  }

  public void setRecommendedDesigners(List<Designer> recommendedDesigners) {
	  this.recommendedDesigners = recommendedDesigners;
  }
  
}
