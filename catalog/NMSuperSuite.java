package com.nm.commerce.catalog;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import atg.repository.RepositoryItem;

import com.nm.collections.Country;
import com.nm.components.CommonComponentHelper;
import com.nm.utils.GenericLogger;

public class NMSuperSuite extends NMProduct {
  
  public static final int TYPE = 2;
  
  private List<NMSuite> suiteList;
  private Map<String, NMSuite> suiteMap;
  private String currentSuiteId;
  protected GenericLogger log = CommonComponentHelper.getLogger();
  
  public NMSuperSuite(final RepositoryItem repositoryItem) {
    super(repositoryItem);
  }
  
  public NMSuperSuite(final String prodId) {
    super(prodId);
    
  }
  
  /**
   * Name of Method: getDefaultSuite
   * 
   * @return List -- NMSuite
   * 
   *         The default suite is the first suite in the suiteList
   * 
   */
  public NMSuite getDefaultSuite() {
    final List<NMSuite> suite = getSuiteList();
    if (suite.size() == 0) {
      if (log.isLoggingWarning()) {
        log.warn("Super Suite: " + this.getId() + " doesn't have displayable subsuites.");
      }
      return null;
    } else {
      return getSuiteList().get(0);
    }
  }
  
  /**
   * Name of Method: getCurrentSelectedSuite
   * 
   * @return List -- NMSuite
   * 
   *         One suite is always displayed at a time, that means there is always a currently selected suite If a user selected the suite the value would be in currentSuiteId. If not the default suite
   *         is selected.
   * 
   */
  public NMSuite getCurrentSelectedSuite() {
    final NMSuite nmSuite = getSuiteMap().get(getCurrentSuiteId());
    if (nmSuite != null) {
      return nmSuite;
    } else {
      return getDefaultSuite();
    }
  }
  
  public String getCurrentSuiteId() {
    return currentSuiteId;
  }
  
  public void setCurrentSuiteId(final String currentSuiteId) {
    this.currentSuiteId = currentSuiteId;
  }
  
  /**
   * Name of Method: getSuiteMap
   * 
   * @return List -- Map
   * 
   *         key = suite Id value = NMSuite object associated with that id
   * 
   * 
   */
  public Map<String, NMSuite> getSuiteMap() {
    
    if (suiteMap == null) {
      suiteMap = new HashMap<String, NMSuite>();
      
      final Iterator<NMSuite> suiteListIterator = getSuiteList().iterator();
      
      while (suiteListIterator.hasNext()) {
        final NMSuite nmSuite = suiteListIterator.next();
        
        suiteMap.put(nmSuite.getId(), nmSuite);
      }
    }
    
    return suiteMap;
    
  }
  
  public void setSuiteMap(final Map<String, NMSuite> suiteMap) {
    this.suiteMap = suiteMap;
  }
  
  /**
   * Name of Method: getSuiteList
   * 
   * @return List -- NMSuites get the list of the suites in this super suite
   * 
   */
  public List<NMSuite> getSuiteList() {
    if (suiteList == null) {
      suiteList = new ArrayList<NMSuite>();
      
      @SuppressWarnings("unchecked")
      final List<RepositoryItem> subSuitesList = (List<RepositoryItem>) getPropertyValue("subsuites");
      
      final Iterator<RepositoryItem> subSuitesIterator = subSuitesList.iterator();
      
      while (subSuitesIterator.hasNext()) {
        final NMSuite nmSuite = new NMSuite((subSuitesIterator.next()));
        nmSuite.setIsZeroPriceShowable(getIsZeroPriceShowable());
        nmSuite.setIsNonZeroPriceShowable(getIsNonZeroPriceShowable());
        nmSuite.setIsStatusShowable(getIsStatusShowable());
        suiteList.add(nmSuite);
      }
    }
    return suiteList;
  }
  
  public void setSuiteList(final List<NMSuite> suiteList) {
    this.suiteList = suiteList;
  }
  
  @Override
  public List<NMProduct> getUnavailableProductList() {
    try {
      return getCurrentSelectedSuite().getUnavailableProductList();
    } catch (final Exception e) {
      return new ArrayList<NMProduct>();
    }
  }
  
  /**
   * Returns the products in the currently selected suite.
   */
  @Override
  public List<NMProduct> getProductList() {
    try {
      return getCurrentSelectedSuite().getProductList();
    } catch (final Exception e) {
      return new ArrayList<NMProduct>();
    }
  }
  
  /**
   * Returns all the products in each of the suites plus related items
   */
  @Override
  public List<NMProduct> getAllProductsList() {
    return getAllProductsList(true);
  }
  
  /**
   * Returns all the products in each of the suites. Duplicate products have been removed. If includeRelated is true then the list will include related items (based on the implementation in NMSuite
   * 
   * @param boolean - includeRelated
   * @return
   */
  public List<NMProduct> getAllProductsList(final boolean includeRelated) {
    final HashMap<String, NMProduct> productMap = new HashMap<String, NMProduct>();
    
    final List<NMSuite> suiteList = getSuiteList();
    
    if (suiteList != null) {
      final Iterator<NMSuite> suiteIterator = suiteList.iterator();
      
      while (suiteIterator.hasNext()) {
        final NMSuite suite = suiteIterator.next();
        
        List<NMProduct> productList = null;
        
        if (includeRelated) {
          productList = suite.getAllProductsList();
        } else {
          productList = suite.getProductList();
        }
        
        if (productList != null) {
          final Iterator<NMProduct> productIterator = productList.iterator();
          
          while (productIterator.hasNext()) {
            final NMProduct product = productIterator.next();
            
            productMap.put(product.getId(), product);
          }
        }
      }
    }
    
    return new ArrayList<NMProduct>(productMap.values());
  }
  
  @Override
  public boolean getAllProductsListPerishables() {
    boolean foundPerisable = false;
    final Iterator<NMProduct> allProductListIterator = getAllProductsList().iterator();
    
    while (allProductListIterator.hasNext()) {
      final NMProduct nmProduct = allProductListIterator.next();
      if (nmProduct.getIsPerishable()) {
        foundPerisable = true;
        break;
      }
    }
    return foundPerisable;
  }
  
  /**
   * Name of Method: getIsShowable
   * 
   * @return boolean
   * 
   *         Determines if this Suite is showable, this is true if at least 1 of its suites is showable.
   */
  @Override
  public boolean getIsShowable() {
    final Iterator<NMSuite> suiteListIterator = getSuiteList().iterator();
    
    while (suiteListIterator.hasNext()) {
      final NMSuite nmSuite = suiteListIterator.next();
      if (nmSuite.getIsShowable()) {
        return true;
      }
    }
    
    return false;
  }
  
  /**
   * Name of Method: getDisplayName
   * 
   * @return String
   * 
   *         If the super suite doesn't have a display name it looks on the currently selected suite for its display name
   */
  @Override
  public String getDisplayName() {
    final String displayName = super.getDisplayName();
    
    if ((displayName == null) || displayName.trim().equals("")) {
      return getCurrentSelectedSuite().getDisplayName();
    } else {
      return displayName;
    }
  }
  
  /**
   * Name of Method: getLongDescriptionCutLine
   * 
   * @return String
   * 
   *         If the super suite doesn't have a long description it looks on the currently selected suite for its long description name
   */
  @Override
  public String getLongDescriptionCutLine() {
    final String longDescriptionCutLine = super.getLongDescriptionCutLine();
    
    if ((longDescriptionCutLine == null) || longDescriptionCutLine.trim().equals("")) {
      return getCurrentSelectedSuite().getLongDescriptionCutLine();
    } else {
      return longDescriptionCutLine;
    }
  }
  
  /**
   * Name of Method: getMoreInfoText
   * 
   * @return String
   * 
   *         If the super suite doesn't have a long description it looks on the currently selected suite for its more info text
   */
  @Override
  public String getMoreInfoText() {
    final String longDescriptionCutLine = super.getLongDescriptionCutLine();
    
    if ((longDescriptionCutLine == null) || longDescriptionCutLine.trim().equals("")) {
      return getCurrentSelectedSuite().getMoreInfoText();
    } else {
      return super.getMoreInfoText();
    }
  }
  
  /**
   * Name of Method: getMoreInfoLink
   * 
   * @return String
   * 
   *         If the super suite doesn't have a long description it looks on the currently selected suite for its more info link
   */
  @Override
  public String getMoreInfoLink() {
    final String longDescriptionCutLine = super.getLongDescriptionCutLine();
    
    if ((longDescriptionCutLine == null) || longDescriptionCutLine.trim().equals("")) {
      return getCurrentSelectedSuite().getMoreInfoLink();
    } else {
      return super.getMoreInfoLink();
    }
  }
  
  /**
   * Name of Method: getDataSourceForMainImages
   * 
   * @return RepositoryItem
   * 
   *         The data source to use when rendering Images. The images for super suites are pull from the currently selected suite.
   * 
   */
  @Override
  public RepositoryItem getDataSourceForMainImages() {
    return getCurrentSelectedSuite().getDataSource();
  }
  
  /**
   * Name of Method: refreshSubItems
   * 
   * refreshes all its cached lists
   * 
   */
  @Override
  public void refreshSubItems() {
    super.refreshSubItems();
    setSuiteList(null);
    setSuiteMap(null);
  }
  
  public Double getLowestPrice() {
    Double lowestPrice = null;
    
    final Iterator<NMSuite> suiteListIterator = getSuiteList().iterator();
    
    while (suiteListIterator.hasNext()) {
      final NMSuite nmSuite = suiteListIterator.next();
      
      if (nmSuite.getIsShowable()) {
        if (lowestPrice == null) {
          lowestPrice = nmSuite.getLowestPrice();
        } else if (lowestPrice.compareTo(nmSuite.getLowestPrice()) > 0) {
          lowestPrice = nmSuite.getLowestPrice();
        }
      }
      
    }
    
    return lowestPrice;
  }
  
  public Double getHighestPrice() {
    Double highestPrice = null;
    
    final Iterator<NMSuite> suiteListIterator = getSuiteList().iterator();
    
    while (suiteListIterator.hasNext()) {
      final NMSuite nmSuite = suiteListIterator.next();
      
      if (nmSuite.getIsShowable()) {
        if (highestPrice == null) {
          highestPrice = nmSuite.getHighestPrice();
        } else if (highestPrice.compareTo(nmSuite.getHighestPrice()) < 0) {
          highestPrice = nmSuite.getHighestPrice();
        }
      }
      
    }
    
    return highestPrice;
  }
  
  @Override
  public Double getPrice() {
    return getHighestPrice();
  }
  
  @Override
  public String getPriceString() {
    try {
      if (getLowestPrice().compareTo(getHighestPrice()) != 0) {
        return NumberFormat.getCurrencyInstance().format(getLowestPrice()) + " - " + NumberFormat.getCurrencyInstance().format(getHighestPrice());
      } else {
        return NumberFormat.getCurrencyInstance().format(getLowestPrice());
      }
    } catch (final Exception e) {
      return PRICE_UNAVAILABLE_STRING;
    }
    
  }
  
  @Override
  public List<String> getTrackerCampaigns() {
    final List<String> campaignList = new ArrayList<String>();
    final List<NMProduct> subProductsList = getProductList();
    
    final Iterator<NMProduct> subProductsIterator = subProductsList.iterator();
    
    while (subProductsIterator.hasNext()) {
      final NMProduct nmProduct = subProductsIterator.next();
      if (!(nmProduct.getTrackerCampaigns().isEmpty())) {
        
        for (int i = 0; i < nmProduct.getTrackerCampaigns().size(); i++) {
          final String trackerName = nmProduct.getTrackerCampaigns().get(i);
          
          if ((trackerName != null) && (!campaignList.contains(trackerName))) {
            campaignList.add(trackerName);
          }
        }
      }
    }
    
    return campaignList;
  }
  
  /**
   * @return the totalInventory
   */
  @Override
  public int getTotalInventory() {
    return getInventory(false);
  }
  
  private int getInventory(final boolean isOnline) {
    int totalInventory = 0;
    try {
      if (suiteList == null) {
        suiteList = this.getSuiteList();
      }
      // System.out.println("suiteList length in suite is "+suiteList.size());
      final Iterator<NMSuite> subSuitesIterator = suiteList.iterator();
      while (subSuitesIterator.hasNext()) {
        final NMSuite nmSuite = subSuitesIterator.next();
        if (isOnline) {
          totalInventory = totalInventory + nmSuite.getTotalInventoryOnline();
        } else {
          totalInventory = totalInventory + nmSuite.getTotalInventory();
        }
      }
      
      // System.out.println("my totalInventory in supersuite is "+totalInventory);
    } catch (final Exception ex) {
      ex.printStackTrace();
    }
    
    return totalInventory;
  }
  
  @Override
  public int getTotalInventoryOnline() {
    return getInventory(true);
  }
  
  @Override
  public Country[] getAllRestrictedShipToCountries() {
    final NMSuite current = getCurrentSelectedSuite();
    return current != null ? current.getAllRestrictedShipToCountries() : null;
  }
  
  /**
   * Returns true if all of the products in the product list are ShopRunner eligible
   * 
   * @return boolean
   */
  @Override
  public boolean getIsShopRunnerEligible() {
    if (isShopRunnerEligible == null) {
      final List<NMProduct> productsList = getProductList();
      final Iterator<NMProduct> productsListIterator = productsList.iterator();
      
      boolean eligible = false;
      
      while (productsListIterator.hasNext()) {
        eligible = productsListIterator.next().getIsShopRunnerEligible();
        
        if (eligible == false) {
          break;
        }
      }
      
      isShopRunnerEligible = new Boolean(eligible);
      return eligible;
    } else {
      return isShopRunnerEligible.booleanValue();
    }
  }
  
}
