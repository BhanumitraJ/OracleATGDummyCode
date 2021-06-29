package com.nm.commerce.upsell;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import atg.nucleus.GenericService;
import atg.nucleus.ServiceException;
import atg.repository.NamedQueryView;
import atg.repository.ParameterSupportView;
import atg.repository.Query;
import atg.repository.Repository;
import atg.repository.RepositoryException;
import atg.repository.RepositoryItem;

import com.nm.catalog.navigation.CategoryHelper;
import com.nm.catalog.navigation.NMCategory;
import com.nm.commerce.NMCommerceItem;
import com.nm.repository.ProductCatalogRepository;
import com.nm.repository.UpsellRepository;

/**
 * Class Name: UpsellProductMappingStrategy The abstract base class of specific mapping strategies. Handles everything for picking upsell products based on the cart. The only thing it cannot do is
 * pick what on a product is mapped to what categories.
 * 
 * This is a GoF Strategy Pattern implementation. See "Design Patterns: Elements of Reusable Object-Oriented Software" by Gamma, Helm, Johnson, and Vlissides (the Gang of Four).
 * 
 * @author C. Chadwick
 * @author $Author: nmavj1 $
 * @since 10/01/04 Last Modified Date: $Date: 2014/05/20 10:49:45CDT $
 * @version $Revision: 1.4 $
 */
public abstract class UpsellProductMappingStrategy extends GenericService {
  private Repository productCatalogRepository;
  private Repository upsellRepository;
  
  /**
   * Creates a new UpsellProductMappingStrategy object.
   */
  public UpsellProductMappingStrategy() {
    super();
    
    // Nothing to do
  }
  
  /**
   * Runs when the component starts up
   * 
   * @throws ServiceException
   */
  public void doStartService() throws ServiceException {
    super.doStartService();
    // Nothing to do
  }
  
  /**
   * Runs when the component is stopped
   * 
   * @throws ServiceException
   */
  public void doStopService() throws ServiceException {
    // Nothing to do
    super.doStopService();
  }
  
  /**
   * Getter for the productCatalogRepository property
   * 
   * @return Returns the productCatalogRepository.
   */
  public Repository getProductCatalogRepository() {
    return productCatalogRepository;
  }
  
  /**
   * Setter for the productCatalogRepository property
   * 
   * @param productCatalogRepository
   *          The productCatalogRepository to set.
   */
  public void setProductCatalogRepository(Repository productCatalogRepository) {
    this.productCatalogRepository = productCatalogRepository;
  }
  
  /**
   * Getter for the upsellRepository property
   * 
   * @return Returns the upsellRepository.
   */
  public Repository getUpsellRepository() {
    return upsellRepository;
  }
  
  /**
   * Setter for the upsellRepository property
   * 
   * @param upsellRepository
   *          The upsellRepository to set.
   */
  public void setUpsellRepository(Repository upsellRepository) {
    this.upsellRepository = upsellRepository;
  }
  
  /**
   * Gets the upsell categories for the products in the cart. Iterates through the products in the cart looking for a product that has associated categories.
   * 
   * @param rankedProductsInCart
   *          A sorted list of the products in the cart
   * @param listFactory
   *          The UpsellProductListFactory to use
   * 
   * @return A map of slot number to product repository items
   */
  public Map getUpsellProducts(List rankedProductsInCart, UpsellProductListFactory listFactory) {
    checkedLogDebug("UpsellProductMappingStrategy.getUpsellProducts()");
    
    Map upsellProductsMap = null;
    Map productListMap = null;
    
    try {
      productListMap = createProductListMap(rankedProductsInCart, listFactory);
      
    } catch (NotEnoughProductsException nepe) {
      if (isLoggingError()) {
        logError("\"Never send a human to do a machine's job.\" Unable to get enough products to fill upsell slots! Blank spaces WILL BE VISIBLE on the \"may we suggest\" section of the shopping cart page for this user!");
      }
      productListMap = nepe.getUpsellProductsThatWereFound();
      
    }
    
    // If, for some reason it's still null
    if (productListMap == null) {
      if (isLoggingError()) {
        logError("Unable to find any products for the upsell!");
      }
      productListMap = new HashMap();
    }
    upsellProductsMap = pickUpsellProducts(productListMap);
    return upsellProductsMap;
  }
  
  /**
   * Creates a map (with the slot number as the key) of products that could be chosen for the upsell
   * 
   * @param rankedProductsInCart
   *          - A list of products in the cart ranked so that the first product is the highest ranked
   * @param listFactory
   *          - The UpsellProductListFactory component
   * 
   * @return a slot number keyed map of product lists
   * 
   * @throws NotEnoughProductsException
   */
  protected Map createProductListMap(List rankedProductsInCart, UpsellProductListFactory listFactory) throws NotEnoughProductsException {
    checkedLogDebug("UpsellProductMappingStrategy.getProductListMap()");
    
    Map productLists = new HashMap();
    Iterator it = rankedProductsInCart.iterator();
    boolean keepSearching = true;
    
    // Iterate through the products in the cart until one has some associated
    // categories.
    NMCommerceItem currentProduct;
    Map categoriesMap;
    while (it.hasNext() && keepSearching) {
      currentProduct = (NMCommerceItem) it.next();
      categoriesMap = getCategoriesForProduct(currentProduct);
      checkedLogDebug("Currently considered product: " + currentProduct + " has " + categoriesMap.size() + " categories");
      
      if ((categoriesMap != null) && (categoriesMap.size() >= 5)) {
        try {
          productLists = createProductListMapForCategories(categoriesMap, listFactory);
          keepSearching = false;
        } catch (NotEnoughProductsException nepe) {
          if (isLoggingWarning()) {
            logWarning(
                    "\"I know kung fu!\" A user has purchased a product whose associated categories were insufficiently merchandised. Using the next highest ranked product or the default categories if no more products exist in the cart",
                    nepe);
          }
          
          // Do nothing else. Since keepSearching still is equal to true, we'll keep
          // iterating through the ranked products in the cart
        }
      }
    }
    
    // The null case and also what happens when the the cart is empty
    if ((productLists == null) || (productLists.size() < 1)) {
      checkedLogDebug("No products in the cart, using the default categories");
      
      categoriesMap = getDefaultCategories();
      
      try {
        productLists = createProductListMapForCategories(categoriesMap, listFactory);
      } catch (NotEnoughProductsException nepe) {
        if (isLoggingError()) {
          logError("\"Never send a human to do a machine's job.\" The default categories are OUT OF SELLABLE PRODUCTS! Blank spaces WILL BE VISIBLE on the \"may we suggest\" section of the shopping cart page for this user!");
        }
        productLists = nepe.getUpsellProductsThatWereFound();
      }
      
    }
    
    return productLists;
  }
  
  /**
   * Given a list of categories, pull out the products and create the list of products that should be put in the upsell
   * 
   * @param upsellProductCategoryMap
   * @param listFactory
   * 
   * @return a Map of products lists (slot number is the key)
   * 
   * @throws NotEnoughProductsException
   **/
  private Map createProductListMapForCategories(Map upsellProductCategoryMap, UpsellProductListFactory listFactory) throws NotEnoughProductsException {
    checkedLogDebug("UpsellProductMappingStrategy.createProductListMapForCategories()");
    
    // Take care of null
    if (upsellProductCategoryMap == null) {
      if (isLoggingError()) {
        logError("No categories were provided to choose products from. This should never happen. The product classification to upsell category mapping must be corrupted.");
      }
      return new HashMap();
    }
    
    int numSlots = upsellProductCategoryMap.size();
    int productTotal = 0;
    Map productListMap = new HashMap(numSlots);
    Set keySet = upsellProductCategoryMap.keySet();
    Iterator slotIterator = keySet.iterator();
    Integer slotNum;
    String categoryId;
    List productsInCategory;
    List productList;
    // Iterator through each slot, get the product list and add it to the productListMap
    while (slotIterator.hasNext()) {
      slotNum = (Integer) slotIterator.next();
      categoryId = (String) upsellProductCategoryMap.get(slotNum);
      checkedLogDebug("Picking product for category " + categoryId + " for slot " + slotNum);
      
      productsInCategory = getProductsInCategory(categoryId);
      if (productsInCategory == null) {
        productsInCategory = new ArrayList();
      }
      productList = listFactory.getProductList(productsInCategory);
      
      productTotal += productList.size();
      productListMap.put(slotNum, productList);
    }
    
    if (productTotal < numSlots) {
      throw new NotEnoughProductsException(
              "There are less products in all the categories than the number of categories. It is impossible to fill the all the lists with the products. Only able to find " + productTotal
                      + " products", productListMap);
    }
    
    return productListMap;
  }
  
  /**
   * The specific type of UpsellProductMappingStrategy should implement this. Given a product in the cart, it returns a map of slot numbers(Integers) to categoryIds (Strings).
   * 
   * @param productInCart
   *          The highest ranked product in the cart
   * 
   * @return A Map of slot numbers (Integer) to categoryId(String)
   */
  protected abstract Map getCategoriesForProduct(NMCommerceItem productInCart);
  
  /**
   * Returns the list of products that are in a particular category with the id of the parameter
   * 
   * @param categoryId
   *          A category ID
   * 
   * @return The list of products in that category
   */
  protected List<RepositoryItem> getProductsInCategory(String categoryId) {
    checkedLogDebug("UpsellProductMappingStrategy.getProductForCategoryId(" + categoryId + ")");
    
    List<RepositoryItem> childProducts = new ArrayList<RepositoryItem>();
    
    NMCategory categoryItem = CategoryHelper.getInstance().getNMCategory(categoryId);
    
    if (categoryItem == null) {
      if (isLoggingError()) {
        logError("The category with id " + categoryId + " was not found. Please either add the category or change the shopping cart upsell references to it");
      }
    } else {
      // childProducts = (List) categoryItem.getPropertyValue(ProductCatalogRepository.CATEGORY_CHILD_PRODUCTS);
      childProducts = categoryItem.getChildProducts();
    }
    return childProducts;
  }
  
  /**
   * Given a Map of product lists, return a Map with the same keys, but instead of returning a list, simply return a single product as the value portion of the pair.
   * 
   * @param productLists
   * 
   * @return A Map of RepositoryItems where the key is the slot number and the value is the product that should appear there
   * 
   * @throws NotEnoughProductsException
   *           If there are insuffient products to choose a unique product from the lists
   */
  protected Map pickUpsellProducts(Map productLists) {
    List currentProductList = null;
    Set minimalLists = new HashSet();
    Set emptyLists = new HashSet();
    Set keySet = productLists.keySet();
    Iterator productListsIt = keySet.iterator();
    Iterator pickerIt = keySet.iterator();
    Set pickedProductIds = new HashSet(productLists.size());
    String currentProductId = null;
    Map productMap = new HashMap();
    RepositoryItem pickedProduct;
    
    while (productListsIt.hasNext()) {
      Integer slotNum = (Integer) productListsIt.next();
      currentProductList = (List) productLists.get(slotNum);
      
      boolean keepSearching = true;
      
      // Keep searching until we find a unique product for this list
      while (keepSearching) {
        if (currentProductList.size() < 1) {
          pickedProduct = pickProductForEmptyList(pickerIt, productLists);
        } else {
          pickedProduct = (RepositoryItem) currentProductList.remove(0);
        }
        if (pickedProduct == null) {
          keepSearching = false;
        } else {
          currentProductId = getProductId(pickedProduct);
          if (!pickedProductIds.contains(currentProductId)) {
            productMap.put(slotNum, pickedProduct);
            pickedProductIds.add(currentProductId);
            keepSearching = false;
          }
        }
      }
    }
    if (isLoggingDebug()) {
      logDebug("Picked Products:");
      Iterator it = pickedProductIds.iterator();
      while (it.hasNext()) {
        logDebug((String) it.next());
      }
    }
    
    return productMap;
  }
  
  private RepositoryItem pickProductForEmptyList(Iterator productsListIterator, Map productLists) {
    boolean onceThroughAlready = false;
    Integer key;
    List currentProductList;
    
    do {
      if (!productsListIterator.hasNext()) {
        if (onceThroughAlready) {
          return null;
        }
        productsListIterator = productLists.keySet().iterator();
        onceThroughAlready = true;
      }
      
      key = (Integer) productsListIterator.next();
      currentProductList = (List) productLists.get(key);
    } while (currentProductList.size() < 1);
    
    return (RepositoryItem) currentProductList.remove(0);
  }
  
  private String getProductId(RepositoryItem product) {
    String id = (String) product.getPropertyValue(ProductCatalogRepository.PRODUCT_ID);
    
    return id;
  }
  
  /**
   * Returns the default categories in a map where slot is the key.
   * 
   * @return a map of categories where the key is the slot number and the value is a categoryId as a string
   */
  protected Map getDefaultCategories() {
    return getCategories(null, null, null, null);
  }
  
  /**
   * Gets the category ID map (slot num is the key) for the given depiction, department designer, and class codes. Nulls are acceptable and the method will choose the correct query so that the null
   * values are represented correctly.
   * 
   * @param depiction
   * @param department
   * @param designer
   * @param classCode
   * @return a Map of category IDs where the Slot number (Integer) is the key and the value is the category ID string
   */
  protected Map getCategories(String depiction, String department, String designer, String classCode) {
    boolean haveDepiction = (depiction != null);
    boolean haveDepartment = (department != null);
    boolean haveDesigner = (designer != null);
    boolean haveClass = (classCode != null); // should have set to false. Of course we don't have class...we're programmers
    List categoryIds = null;
    StringBuffer queryNameBuffer = new StringBuffer();
    List paramList = new ArrayList();
    if (!haveDepiction && !haveDepartment && !haveDesigner && !haveClass) {
      queryNameBuffer.append("DefaultCategories");
    }
    if (haveDepiction) {
      queryNameBuffer.append("Depiction");
      paramList.add(depiction);
    }
    if (haveDepartment) {
      queryNameBuffer.append("Department");
      paramList.add(department);
    }
    if (haveDesigner) {
      queryNameBuffer.append("Designer");
      paramList.add(designer);
    }
    if (haveClass) {
      queryNameBuffer.append("Class");
      paramList.add(classCode);
    }
    queryNameBuffer.append("Query");
    // Make the first character lowercase
    queryNameBuffer.setCharAt(0, Character.toLowerCase(queryNameBuffer.charAt(0)));
    
    try {
      
      String queryName = queryNameBuffer.toString();
      checkedLogDebug("Using query " + queryName + " Depiction=" + depiction + " Dept=" + department + " Designer=" + designer + " Class=" + classCode);
      checkedLogDebug("paramsList = " + paramList);
      NamedQueryView namedQueryView = (NamedQueryView) getUpsellRepository().getView(UpsellRepository.UPSELL_MAPPING_DESCRIPTOR);
      ParameterSupportView parameterizedView = (ParameterSupportView) namedQueryView;
      
      if ((namedQueryView == null) || (parameterizedView == null)) {
        if (isLoggingError()) {
          logError("Unable to get a NamedQueryView or ParameterSupportView when looking for upsell mapping object ");
        }
      } else {
        
        Query upsellMappingQuery = namedQueryView.getNamedQuery(queryName);
        
        // Create a parameter list with just our one parameter: the depiction code
        Object[] params = paramList.toArray();
        
        // Execute the query and use the results to create a list of categoryIds
        RepositoryItem[] upsellMappings = parameterizedView.executeQuery(upsellMappingQuery, params);
        
        if (upsellMappings != null) {
          if (upsellMappings.length > 1) {
            if (isLoggingWarning()) {
              logWarning("More than 1 upsellMapping was returned. Using the first item. Unfortunately that means this is the not the best uspell mapping");
            }
          }
          RepositoryItem upsellMapping = upsellMappings[0];
          if (upsellMapping != null) {
            categoryIds = (List) upsellMapping.getPropertyValue(UpsellRepository.UPSELL_MAPPING_CATEGORIES);
          }
        }
        
      }
    } catch (RepositoryException re) {
      if (isLoggingError()) {
        logError("Error while trying to find mapped categories", re);
      }
    }
    return convertCategoryListToMap(categoryIds);
    
  }
  
  private Map convertCategoryListToMap(List categoryIds) {
    
    Map categoryIdMap = null;
    if (categoryIds != null) {
      checkedLogDebug("convertCategoryListToMap() got " + categoryIds.size() + " categories");
      int size = categoryIds.size();
      categoryIdMap = new HashMap(categoryIds.size());
      // Would love to use an iterator, but the category Ids are not necessarily
      // unique within the list and therefore getting the index for the map key is impossible
      for (int i = 0; i < size; i++) {
        String categoryId = (String) categoryIds.get(i);
        Integer slotNum = new Integer(i);
        // The list is 0 based, but we want slot 1 to be indexed at 1
        slotNum = new Integer(slotNum.intValue() + 1);
        checkedLogDebug("Adding category " + categoryId + " to map at slot# " + slotNum);
        categoryIdMap.put(slotNum, categoryId);
      }
    }
    return categoryIdMap;
  }
  
  /**
   * Logs debug output.
   * 
   * @param message
   *          The debug message
   */
  public void checkedLogDebug(String message) {
    if (isLoggingDebug()) {
      logDebug(message);
    }
  }
  
  /**
   * 
   * Class Name: NotEnoughProductsException This class represents an exception when not enough products could be found. This is typically a problem when trying to populate the upsell with a group of
   * categories that collectively do not have enough products.
   * 
   * @author C. Chadwick (nmcjc1)
   * @author $Author: nmavj1 $
   * @since 10/1/04 Last Modified Date: $Date: 2014/05/20 10:49:45CDT $
   * @version $Revision: 1.4 $
   */
  protected class NotEnoughProductsException extends Exception {
    /**
     * Creates a new NotEnoughProductsException object.
     * 
     * @param message
     */
    public NotEnoughProductsException(String message, Map upsellProductsThatWereFound) {
      super(message);
      this.upsellProductsThatWereFound = upsellProductsThatWereFound;
    }
    
    public Map getUpsellProductsThatWereFound() {
      return upsellProductsThatWereFound;
    }
    
    private Map upsellProductsThatWereFound;
  }
  
}

// ====================================================================
// File: UpsellProductMappingStrategy.java
//
// Copyright (c) 2004 Neiman Marcus.
// All rights reserved.
//
// This is an unpublished proprietary source code of the Neiman Marcus Group.
// The copyright notice above does not evidence and actual or intended publication
// of said source code.
// ====================================================================
