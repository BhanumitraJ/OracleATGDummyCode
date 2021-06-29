package com.nm.commerce.catalog;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.IllegalFormatConversionException;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import atg.nucleus.Nucleus;
import atg.repository.RepositoryItem;

import com.nm.collections.Country;
import com.nm.common.INMGenericConstants;
import com.nm.utils.BrandSpecs;
import com.nm.utils.StringUtilities;

public class NMSuite extends NMProduct {
  
  public static final int TYPE = 1; // repository subtype number
  
  private List<NMProduct> productList;
  private List<NMProduct> unavailableProductList;
  
  public NMSuite(final RepositoryItem repositoryItem) {
    super(repositoryItem);
  }
  
  public NMSuite(final String prodId) {
    super(prodId);
  }
  
  /**
   * Name of Method: getProductList
   * 
   * @return List - NMProducts
   * 
   *         returns the products associated with this suite
   * 
   */
  @Override
  public List<NMProduct> getProductList() {
    boolean hasSellableProds = false;
    NMProduct nmProductForQueue = null;
    NMProduct nmSellableProdForQueue = null;
    if (productList == null) {
      productList = new ArrayList<NMProduct>();
      
      @SuppressWarnings("unchecked")
      final List<RepositoryItem> subProductsList = (List<RepositoryItem>) getPropertyValue("subproducts");
      
      final Iterator<RepositoryItem> subProductsIterator = subProductsList.iterator();
      
      boolean firstProduct = true;
      
      while (subProductsIterator.hasNext()) {
        final NMProduct nmProduct = new NMProduct(subProductsIterator.next());
        nmProduct.setIsZeroPriceShowable(getIsZeroPriceShowable());
        nmProduct.setIsNonZeroPriceShowable(getIsNonZeroPriceShowable());
        nmProduct.setIsStatusShowable(getIsStatusShowable());
        
        if (firstProduct) {
          if (nmProduct.getIsShowable()) {
            nmProduct.setIsFirstShowableProductInSuite(true);
            firstProduct = false;
          }
        }
        
        if (nmProduct.getIsShowable()) {
          hasSellableProds = true;
          nmSellableProdForQueue = nmProduct;
        } else {
          nmProductForQueue = nmProduct;
        }
        
        nmProduct.setParentSuiteId(getId());
        
        productList.add(nmProduct);
      }
      
      for (int pos = productList.size() - 1; pos >= 0; pos--) {
        final NMProduct nmProduct = productList.get(pos);
        if (nmProduct.getIsShowable()) {
          nmProduct.setIsLastShowableProductInSuite(true);
          break;
        }
      }
    }
    // System.out.println("hasSellableProds suite products-->"+hasSellableProds);
    // System.out.println("hasSellableProds suite nmProductForQueue-->"+nmProductForQueue);
    // System.out.println("hasSellableProds suite nmSellableProdForQueue-->"+nmSellableProdForQueue);
    
    try {
      if (!getIsDisplayable() && hasSellableProds && (nmSellableProdForQueue != null)) {
        // System.out.println("**** product id: " + nmSellableProdForQueue.getId()
        // + " putting non displayable suite on queue - true PUT ON QUEUE X");
        getProdSkuUtil().removeProductSkusFromCache(nmSellableProdForQueue.getDataSource());
        
      }
      if (!hasSellableProds && (nmProductForQueue != null)) {
        // System.out.println("**** product id: " + nmProductForQueue.getId()
        // + " putting child suite item on queue - true PUT ON QUEUE");
        
        getProdSkuUtil().removeProductSkusFromCache(nmProductForQueue.getDataSource());
      }
    } catch (final Exception e) {
      System.out.println("Caught an exception trying to put suite on queue" + e);
      e.printStackTrace();
    }
    
    return productList;
  }
  
  public void setProductList(final List<NMProduct> productList) {
    this.productList = productList;
  }
  
  /**
   * Name of Method: getUnavailableProductList
   * 
   * @return List - NMProducts
   * 
   *         returns the unavailable products associated with this suite
   * 
   */
  @Override
  public List<NMProduct> getUnavailableProductList() {
    if (unavailableProductList == null) {
      unavailableProductList = new ArrayList<NMProduct>();
      
      @SuppressWarnings("unchecked")
      final List<RepositoryItem> subProductsList = (List<RepositoryItem>) getPropertyValue("subproducts");
      
      final Iterator<RepositoryItem> subProductsIterator = subProductsList.iterator();
      
      while (subProductsIterator.hasNext()) {
        final NMProduct nmProduct = new NMProduct(subProductsIterator.next());
        nmProduct.setIsZeroPriceShowable(getIsZeroPriceShowable());
        nmProduct.setIsNonZeroPriceShowable(getIsNonZeroPriceShowable());
        nmProduct.setIsStatusShowable(getIsStatusShowable());
        
        nmProduct.setParentSuiteId(getId());
        
        if (!nmProduct.getIsShowable()) {
          unavailableProductList.add(nmProduct);
        }
      }
    }
    return unavailableProductList;
  }
  
  public void setUnavailableProductList(final List<NMProduct> unavailableProductList) {
    this.unavailableProductList = unavailableProductList;
  }
  
  /**
   * Name of Method: showProduct
   * 
   * @return boolean Determines if this suite is showable, it is showable if 1 of the products in the suite is showable.
   * 
   */
  @Override
  public boolean getIsShowable() {
    final Iterator<NMProduct> productListIterator = getProductList().iterator();
    while (productListIterator.hasNext()) {
      final NMProduct nmProduct = productListIterator.next();
      if (nmProduct.getIsShowable()) {
        return true;
      }
    }
    
    return false;
  }
  

  /**
   * Name of Method: getAllProductsList
   * 
   * @return List
   * 
   *         returns all the products in this suite and the suites related products in one List, it filters out the duplicate products
   * 
   */
  @Override
  public List<NMProduct> getAllProductsList() {
    if (allProductsList == null) {
      allProductsList = new ArrayList<NMProduct>();
      
      if (getIsShowable()) {
        allProductsList.addAll(getProductList());
        allProductsList.addAll(getRelatedProductList());
        allProductsList.addAll(getRelatedProductList2());
      }
      
      filterProductList(allProductsList);
    }
    
    return allProductsList;
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
   * Name of Method: getMatrixJavaScript
   * 
   * @return String
   * 
   *         returns a String that consists of the Javascript matrix for each product.
   * 
   */
  @Override
  public String getMatrixJavaScript() {
    
    final StringBuffer jsString = new StringBuffer(100);
    
    final Iterator<NMProduct> productListIterator = getProductList().iterator();
    while (productListIterator.hasNext()) {
      final NMProduct nmProduct = productListIterator.next();
      jsString.append(nmProduct.getMatrixJavaScript());
    }
    
    return jsString.toString();
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
    setProductList(null);
  }
  
  /**
   * Name of Method: getHasShowableHexColors()
   * 
   * return boolean
   * 
   * Checks to see if the products in the suite are using hexadecimal or not
   * 
   */
  @Override
  public boolean getHasShowableHexColors() {
    final Iterator<NMProduct> productListIterator = getProductList().iterator();
    while (productListIterator.hasNext()) {
      final NMProduct nmProduct = productListIterator.next();
      if (nmProduct.getHasShowableHexColors()) {
        return true;
      }
    }
    return false;
  }
  
  public Double getLowestPrice() {
    Double lowestPrice = null;
    
    final Iterator<NMProduct> productListIterator = getProductList().iterator();
    
    while (productListIterator.hasNext()) {
      final NMProduct nmProduct = productListIterator.next();
      
      if (nmProduct.getIsShowable()) {
        if (lowestPrice == null) {
          lowestPrice = nmProduct.getPrice();
        } else if (lowestPrice.compareTo(nmProduct.getPrice()) > 0) {
          lowestPrice = nmProduct.getPrice();
        }
      }
      
    }
    
    return lowestPrice;
  }
  
  public Double getHighestPrice() {
    Double highestPrice = null;
    
    final Iterator<NMProduct> productListIterator = getProductList().iterator();
    
    while (productListIterator.hasNext()) {
      final NMProduct nmProduct = productListIterator.next();
      
      if (nmProduct.getIsShowable()) {
        if (highestPrice == null) {
          highestPrice = nmProduct.getPrice();
        } else if (highestPrice.compareTo(nmProduct.getPrice()) < 0) {
          highestPrice = nmProduct.getPrice();
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
      if (!nmProduct.getTrackerCampaigns().isEmpty()) {
        
        for (int i = 0; i < nmProduct.getTrackerCampaigns().size(); i++) {
          final String trackerName = nmProduct.getTrackerCampaigns().get(i);
          
          if ((trackerName != null) && !campaignList.contains(trackerName)) {
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
      if (productList == null) {
        productList = this.getProductList();
      }
      // System.out.println("product list length in suite is "+productList.size());
      final Iterator<NMProduct> subProductsIterator = productList.iterator();
      while (subProductsIterator.hasNext()) {
        final NMProduct nmProduct = subProductsIterator.next();
        if (isOnline) {
          totalInventory = totalInventory + nmProduct.getTotalInventoryOnline();
        } else {
          totalInventory = totalInventory + nmProduct.getTotalInventory();
        }
      }
      // System.out.println("my totalInventory in suite is "+totalInventory);
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
    final List<NMProduct> childProducts = getProductList();
    final Set<Country> restrictedHashCountries = new HashSet<Country>();
    for (final NMProduct product : childProducts) {
      for (final Country country : product.getAllRestrictedShipToCountries()) {
        restrictedHashCountries.add(country);
      }
    }
    final Country[] restrictedCountries = restrictedHashCountries.toArray(new Country[0]);
    return restrictedCountries;
  }
  
  @Override
  /**
   * Iterates through the productList and checks to see if at all the NMProducts are a dropship product
   * @return if all  of the NMProducs are dropship
   * @see prodButtons.jsp
   */
  public boolean getIsDropship() {
    final List<NMProduct> subProductsList = getProductList();
    final Iterator<NMProduct> subPorductsListIterator = subProductsList.iterator();;
    while (subPorductsListIterator.hasNext()) {
      final NMProduct product = subPorductsListIterator.next();
      final boolean dropship = product.getIsDropship();
      if (!dropship && product.getIsShowable()) {
        return false;
      }
    }
    return true;
  }
  
  @Override
  public String getMerchandiseType() {
    final List<NMProduct> subProductsList = getProductList();
    final Iterator<NMProduct> subPorductsListIterator = subProductsList.iterator();
    String merchandiseType = "";
    while (subPorductsListIterator.hasNext()) {
      merchandiseType = subPorductsListIterator.next().getMerchandiseType();
      if (StringUtilities.isNotEmpty(merchandiseType) && (merchandiseType.equals("6") || merchandiseType.equals("7"))) {
        break;
      }
    }
    return merchandiseType;
  }
  
  /**
   * Returns true if all of the products in the product list are ShopRunner eligible.
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
  
  /**
   * validates if all the products in suite are eligible for alipay.
   * 
   * @return boolean
   */
  @Override
  public boolean isAlipayEligible() {
    final List<NMProduct> productsList = getProductList();
    final Iterator<NMProduct> productsListIterator = productsList.iterator();
    boolean eligible = false;
    
    while (productsListIterator.hasNext()) {
      eligible = getProdSkuUtil().isAlipayEligible(productsListIterator.next());
      if (!eligible) {
        break;
      }
    }
    return eligible;
  }
  
  /*@Override
  public boolean getIsPreOrder() {
    final List<NMProduct> subProductsList = getProductList();
    final Iterator<NMProduct> subPorductsListIterator = subProductsList.iterator();
    final Repository productRepository = CommonComponentHelper.getProductRepository();
    boolean preOrder = false;
    final String suiteId = subPorductsListIterator.next().getParentSuiteId();
    try {
      final RepositoryItem product = productRepository.getItem(suiteId, "product");
      if (product.getPropertyValue("flgPreview") != null) {
        preOrder = (Boolean) product.getPropertyValue("flgPreview");
      }
    } catch (final Exception e) {
      
      e.printStackTrace();
    }
    return preOrder;
  }*/
  //adding new method definition as the old one is obsolete
  public boolean getIsPreOrder() {
	  List<NMProduct> cprods= getProductList();
	  final Iterator<NMProduct> cprodsIterator = cprods.iterator();
	  float index=0.0f;
	  float cutoff=cprods.size();
	  while (cprodsIterator.hasNext()){
		  final NMProduct nmProduct = cprodsIterator.next();
		  if(nmProduct.getIsPreOrder()){
	        	index++;
	        }  
	  }
	 /* System.out.println("index->"+index);
	  System.out.println("cutoff->"+cutoff/2);*/
      if(cutoff>0 && index>=(cutoff/2)){
    	 return true;
      }
      return false;
  }
  //method to return prod title
  public String getTitle() {
	  final BrandSpecs brandSpecs = (BrandSpecs) Nucleus.getGlobalNucleus().resolveName("/nm/utils/BrandSpecs");
	  int childDetailcount=brandSpecs.getChildDetailCount();
	  String title="";
	  List<NMProduct> cprods= getProductList();
	  final Iterator<NMProduct> cprodsIterator = cprods.iterator();
	  int index=0;
	  while (cprodsIterator.hasNext()&& index<childDetailcount){
		  final NMProduct nmProduct = cprodsIterator.next();
		  if(nmProduct.getIsSellableItem()){
			  title =title+nmProduct.getDisplayName()+" ";
			  index++;
		  }
	  }
	return title;
  }
  
  public String getCarouselTitle() {
	  final BrandSpecs brandSpecs = (BrandSpecs) Nucleus.getGlobalNucleus().resolveName("/nm/utils/BrandSpecs");
	  int childDetailcount=brandSpecs.getChildDetailCountCarousel();
	  String title="";
	  List<NMProduct> cprods= getProductList();
	  final Iterator<NMProduct> cprodsIterator = cprods.iterator();
	  int index=0;
	  while (cprodsIterator.hasNext()&& index<childDetailcount){
		  final NMProduct nmProduct = cprodsIterator.next();
		  if(nmProduct.getIsSellableItem()){
			  title =title+nmProduct.getDisplayName()+" ";
			  index++;
		  }
	  }
	return title;
  }
  
  public String getWebDesignerName() {
	  String designerName="";
	  List<NMProduct> cprods= getProductList();
	  final Iterator<NMProduct> cprodsIterator = cprods.iterator();
	  while (cprodsIterator.hasNext()){
		  final NMProduct nmProduct = cprodsIterator.next();
		  if(nmProduct.getIsSellableItem()){
			  designerName = nmProduct.getWebDesignerName();
			  break;
		  }
	  }
	return designerName;
  }
  /**
   * Iterates through the productList and checks to see if at least one product is SDD Eligible
   * 
   * @return Boolean if at least one product in the suite is SDD Eligible
   */
  @Override
  public Boolean getSddEligible() {
    final List<NMProduct> subProductsList = getProductList();
    final Iterator<NMProduct> subPorductsListIterator = subProductsList.iterator();
    Boolean sddEligible = Boolean.FALSE;
    while (subPorductsListIterator.hasNext()) {
      final NMProduct product = subPorductsListIterator.next();
      if (null != product) {
        sddEligible = product.getSddEligible();
        if (sddEligible) {
          break;
        }
      }
    }
    return sddEligible;
  }
  
  @Override
  public String getFormattedPrice() {
    try {
      final Double lowestPrice = getLowestPrice();
      final Double highestPrice = getHighestPrice();
      if ((lowestPrice != null) && (highestPrice != null) && (lowestPrice.compareTo(highestPrice) != 0)) {
        return String.format(INMGenericConstants.TWO_DIGIT_FORMAT, lowestPrice) + " - " + String.format(INMGenericConstants.TWO_DIGIT_FORMAT, highestPrice);
      } else if (getLowestPrice() != null) {
        return String.format(INMGenericConstants.TWO_DIGIT_FORMAT, getLowestPrice());
      } else {
        return null;
      }
    } catch (final IllegalFormatConversionException illegalFormatConversionException) {
      return null;
    }
  }
}
