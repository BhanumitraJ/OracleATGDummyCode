package com.nm.commerce.pagedef.model;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.nm.interfaces.Debugger;
import com.nm.search.endeca.NMSearchRecord;

public class BRSearchPageModel extends GenericSearchPageModel implements Debugger {
  
  /**
   * Property to hold maxCount
   */
  private String maxCount;
  /**
   * Property to hold brSearchRecord
   */
  private NMSearchRecord brSearchRecord;
  /**
   * Property to hold selectedDesigner
   */
  private List<String> selectedDesigner;
  
  /** The sort list. */
  private String[] sortList;
  
  /** The sort map. */
  private Map<String, String> sortMap;
  
  /** The selected sort value. */
  private String selectedSortValue;
  
  /** The brandNameCharacter */
  private Set<Character> brandNameCharacter;
  
  /** The brandList */
  private List<String> brandList;
  /** The brandStats */
  private List<String> brandStats;
  /** The brandMap */
  private Map<String, String> brandMap;
  /** The sizeList */
  private List<String> sizeList;
  
  /** The sizeMap */
  private Map<String, String> sizeMap;
  
  /** The selectedPrice */
  private List<String> selectedPrice;
  
  /** The price map. */
  private Map<String, String> priceMap;
  
  /** The price list. */
  private List<String> priceList;
  
  /** The color map. */
  private Map<String, String> colorMap;
  
  /**
   * Property to hold selectedSize
   */
  
  private List<String> selectedSize;
  
  private List<String> selectedColor;
  
  /**
   * Property to hold facetFieldsCategoryList
   */
  private List<Map<String, Object>> facetFieldsCategoryList;
  
  /**
   * Property to hold selectedCategoryList
   */
  private List<String> selectedCategoryList;
  
  /**
   * Property to hold categoryMap
   */
  private Map<String, String> categoryMap;
  
  /** The selected LL. */
  private String selectedLL;
  
  /** The selecte dMiles. */
  private String selectedMiles;
  
  /** The selected Store. */
  private String selectedStore;
  
  /** The selected Location Input. */
  private String selectedLocationInput;
  
  /** The Banner HTML. */
  private String bannerHtml;
  
  /** The Redirected URL */
  private String redirectedUrl;
  
  /** Autocorrected Keyword from BR **/
  private String autoCorrectedKeyword;
  
  /** The Depiction Redirect URL */
  private String depictionRedirectUrl;
  
  /** The Store Selected With Error */
  private boolean storeSelectedWithError;
  
  /* The Applied Filter Count */
  private int appliedFilterCount;
  
  // When BR is ready with Online feature; please uncomment this code
  /** The Online Only */
  // private boolean onlineOnly;
  //
  // /**
  // * @return the onlineOnly
  // */
  // public boolean isOnlineOnly() {
  // return onlineOnly;
  // }
  //
  // /**
  // * @param onlineOnly
  // * the onlineOnly to set
  // */
  // public void setOnlineOnly(final boolean onlineOnly) {
  // this.onlineOnly = onlineOnly;
  // }
  
  /**
   * @return the selectedLocationInput
   */
  public String getSelectedLocationInput() {
    return selectedLocationInput;
  }
  
  /**
   * @param selectedLocationInput
   *          the selectedLocationInput to set
   */
  public void setSelectedLocationInput(final String selectedLocationInput) {
    this.selectedLocationInput = selectedLocationInput;
  }
  
  /**
   * @return the selectedStore
   */
  public String getSelectedStore() {
    return selectedStore;
  }
  
  /**
   * @param selectedStore
   *          the selectedStore to set
   */
  public void setSelectedStore(final String selectedStore) {
    this.selectedStore = selectedStore;
  }
  
  /**
   * @return the selectedLL
   */
  public String getSelectedLL() {
    return selectedLL;
  }
  
  /**
   * @param selectedLL
   *          the selectedLL to set
   */
  public void setSelectedLL(final String selectedLL) {
    this.selectedLL = selectedLL;
  }
  
  /**
   * @return the selectedMiles
   */
  public String getSelectedMiles() {
    return selectedMiles;
  }
  
  /**
   * @param selectedMiles
   *          the selectedMiles to set
   */
  public void setSelectedMiles(final String selectedMiles) {
    this.selectedMiles = selectedMiles;
  }
  
  /**
   * @return the brandNameCharacter
   */
  public Set<Character> getBrandNameCharacter() {
    return brandNameCharacter;
  }
  
  /**
   * @param brandNameCharacter
   *          the brandNameCharacter to set
   */
  public void setBrandNameCharacter(final Set<Character> brandNameCharacter) {
    this.brandNameCharacter = brandNameCharacter;
  }
  
  /**
   * @return the selectedSortValue
   */
  public String getSelectedSortValue() {
    return selectedSortValue;
  }
  
  /**
   * @param selectedSortValue
   *          the selectedSortValue to set
   */
  public void setSelectedSortValue(final String selectedSortValue) {
    this.selectedSortValue = selectedSortValue;
  }
  
  /**
   * @return the sortMap
   */
  public Map<String, String> getSortMap() {
    return sortMap;
  }
  
  /**
   * @param sortMap
   *          the sortMap to set
   */
  public void setSortMap(final Map<String, String> sortMap) {
    this.sortMap = sortMap;
  }
  
  /**
   * @return the sortList
   */
  public String[] getSortList() {
    return sortList;
  }
  
  /**
   * @param sortList
   *          the sortList to set
   */
  public void setSortList(final String[] sortList) {
    this.sortList = sortList;
  }
  
  /**
   * @return the selectedDesigner
   */
  @Override
  public List<String> getSelectedDesigner() {
    return selectedDesigner;
  }
  
  /**
   * @param selectedDesigner
   *          the selectedDesigner to set
   */
  @Override
  public void setSelectedDesigner(final List<String> selectedDesigner) {
    this.selectedDesigner = selectedDesigner;
  }
  
  /**
   * @return the selectedPrice
   */
  @Override
  public List<String> getSelectedPrice() {
    return selectedPrice;
  }
  
  /**
   * @param selectedPrice
   *          the selectedPrice to set
   */
  @Override
  public void setSelectedPrice(final List<String> selectedPrice) {
    this.selectedPrice = selectedPrice;
  }
  
  /**
   * @return the selectedSize
   */
  @Override
  public List<String> getSelectedSize() {
    return selectedSize;
  }
  
  /**
   * @param selectedSize
   *          the selectedSize to set
   */
  @Override
  public void setSelectedSize(final List<String> selectedSize) {
    this.selectedSize = selectedSize;
  }
  
  /**
   * @return the priceMap
   */
  public Map<String, String> getPriceMap() {
    return priceMap;
  }
  
  /**
   * @param priceMap
   *          the priceMap to set
   */
  public void setPriceMap(final Map<String, String> priceMap) {
    this.priceMap = priceMap;
  }
  
  /**
   * @return the priceList
   */
  public List<String> getPriceList() {
    return priceList;
  }
  
  /**
   * @param priceList
   *          the priceList to set
   */
  public void setPriceList(final List<String> priceList) {
    this.priceList = priceList;
  }
  
  /**
   * @return the sizeList
   */
  public List<String> getSizeList() {
    return sizeList;
  }
  
  /**
   * @param sizeList
   *          the sizeList to set
   */
  public void setSizeList(final List<String> sizeList) {
    this.sizeList = sizeList;
  }
  
  /**
   * @return the sizeMap
   */
  public Map<String, String> getSizeMap() {
    return sizeMap;
  }
  
  /**
   * @param sizeMap
   *          the sizeMap to set
   */
  public void setSizeMap(final Map<String, String> sizeMap) {
    this.sizeMap = sizeMap;
  }
  
  /**
   * @return the brandList
   */
  public List<String> getBrandList() {
    return brandList;
  }
  
  /**
   * @param brandList
   *          the brandList to set
   */
  public void setBrandList(final List<String> brandList) {
    this.brandList = brandList;
  }
  
  /**
   * @return the brandStats
   */
  public List<String> getBrandStats() {
    return brandStats;
  }
  
  /**
   * @param brandStats
   *          the brandStats to set
   */
  public void setBrandStats(final List<String> brandStats) {
    this.brandStats = brandStats;
  }
  
  /**
   * @return the brandMap
   */
  public Map<String, String> getBrandMap() {
    return brandMap;
  }
  
  /**
   * @param brandMap
   *          the brandMap to set
   */
  public void setBrandMap(final Map<String, String> brandMap) {
    this.brandMap = brandMap;
  }
  
  @Override
  public String getDebugInfo() {
    // TODO Auto-generated method stub
    return null;
  }
  
  @Override
  public void setDebugInfo(final String debugInfo) {
    // TODO Auto-generated method stub
    
  }
  
  /**
   * @return the maxCount
   */
  public String getMaxCount() {
    return maxCount;
  }
  
  /**
   * @param maxCount
   *          the maxCount to set
   */
  public void setMaxCount(final String maxCount) {
    this.maxCount = maxCount;
  }
  
  /**
   * @return the brSearchRecord
   */
  public NMSearchRecord getBrSearchRecord() {
    return brSearchRecord;
  }
  
  /**
   * @param brSearchRecord
   *          the brSearchRecord to set
   */
  public void setBrSearchRecord(final NMSearchRecord brSearchRecord) {
    this.brSearchRecord = brSearchRecord;
  }
  
  /**
   * @return the colorMap
   */
  public Map<String, String> getColorMap() {
    return colorMap;
  }
  
  /**
   * @param colorMap
   *          the colorMap to set
   */
  public void setColorMap(final Map<String, String> colorMap) {
    this.colorMap = colorMap;
  }
  
  /**
   * @return the selectedColor
   */
  @Override
  public List<String> getSelectedColor() {
    return selectedColor;
  }
  
  /**
   * @param selectedColor
   *          the selectedColor to set
   */
  @Override
  public void setSelectedColor(final List<String> selectedColor) {
    this.selectedColor = selectedColor;
  }
  
  /**
   * @return the facetFieldsCategoryList
   */
  @Override
  public List<Map<String, Object>> getFacetFieldsCategoryList() {
    return facetFieldsCategoryList;
  }
  
  /**
   * @param facetFieldsCategoryList
   *          the facetFieldsCategoryList to set
   */
  @Override
  public void setFacetFieldsCategoryList(final List<Map<String, Object>> facetFieldsCategoryList) {
    this.facetFieldsCategoryList = facetFieldsCategoryList;
  }
  
  /**
   * @return the selectedCategoryList
   */
  @Override
  public List<String> getSelectedCategoryList() {
    return selectedCategoryList;
  }
  
  /**
   * @param selectedCategoryList
   *          the selectedCategoryList to set
   */
  @Override
  public void setSelectedCategoryList(final List<String> selectedCategoryList) {
    this.selectedCategoryList = selectedCategoryList;
  }
  
  /**
   * @return the categoryMap
   */
  public Map<String, String> getCategoryMap() {
    return categoryMap;
  }
  
  /**
   * @param categoryMap
   *          the categoryMap to set
   */
  public void setCategoryMap(final Map<String, String> categoryMap) {
    this.categoryMap = categoryMap;
  }
  
  /**
   * @return the bannerHtml
   */
  public String getBannerHtml() {
    return bannerHtml;
  }
  
  /**
   * @param bannerHtml
   *          the bannerHtml to set
   */
  public void setBannerHtml(final String bannerHtml) {
    this.bannerHtml = bannerHtml;
  }
  
  /**
   * @return the redirectedUrl
   */
  public String getRedirectedUrl() {
    return redirectedUrl;
  }
  
  /**
   * @param redirectedUrl
   *          the redirectedUrl to set
   */
  public void setRedirectedUrl(final String redirectedUrl) {
    this.redirectedUrl = redirectedUrl;
  }
  
  public String getAutoCorrectedKeyword() {
    return autoCorrectedKeyword;
  }
  
  public void setAutoCorrectedKeyword(final String autoCorrectedKeyword) {
    this.autoCorrectedKeyword = autoCorrectedKeyword;
  }
  
  /**
   * @return the depictionRedirectUrl
   */
  public String getDepictionRedirectUrl() {
    return depictionRedirectUrl;
  }
  
  /**
   * @param depictionRedirectUrl
   *          the depictionRedirectUrl to set
   */
  public void setDepictionRedirectUrl(final String depictionRedirectUrl) {
    this.depictionRedirectUrl = depictionRedirectUrl;
  }
  
  /**
   * @return the storeSelectedWithError
   */
  public boolean isStoreSelectedWithError() {
    return storeSelectedWithError;
  }
  
  /**
   * @param storeSelectedWithError
   *          the storeSelectedWithError to set
   */
  public void setStoreSelectedWithError(final boolean storeSelectedWithError) {
    this.storeSelectedWithError = storeSelectedWithError;
  }
  
  /**
   * @return the appliedFilterCount
   */
  public int getAppliedFilterCount() {
    return appliedFilterCount;
  }
  
  /**
   * @param appliedFilterCount
   *          the appliedFilterCount to set
   */
  public void setAppliedFilterCount(final int appliedFilterCount) {
    this.appliedFilterCount = appliedFilterCount;
  }
  
}
