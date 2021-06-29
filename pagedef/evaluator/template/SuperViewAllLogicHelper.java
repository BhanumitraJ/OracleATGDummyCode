package com.nm.commerce.pagedef.evaluator.template;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletException;

import atg.nucleus.Nucleus;
import atg.repository.RepositoryItem;
import atg.servlet.DynamoHttpServletRequest;

import com.nm.catalog.navigation.NMCategory;
import com.nm.commerce.catalog.SuperProduct;
import com.nm.commerce.pagedef.definition.ProductTemplatePageDefinition;
import com.nm.formhandler.CatalogFilter;
import com.nm.utils.GenericLogger;

/*
 * 
 * This class is adopted from the com.nm.droplet.SuperViewAllLogic.java
 */
public class SuperViewAllLogicHelper {
  
  private String previousCategoryDisplayName = null;
  private int index = 0;
  
  private GenericLogger log;
  
  private DynamoHttpServletRequest pRequest;
  private int itemCount;
  private int columnCount;
  
  public SuperViewAllLogicHelper(ProductTemplatePageDefinition pageDefinition, DynamoHttpServletRequest request) {
    log = (GenericLogger) Nucleus.getGlobalNucleus().resolveName("/nm/utils/GenericLogger");
    
    pRequest = request;
    
    itemCount = pageDefinition.getItemCount();
    columnCount = pageDefinition.getColumnCount();
  }
  
  public SuperViewAllLogicHelper(int itemCount, int columnCount, DynamoHttpServletRequest request) {
    log = (GenericLogger) Nucleus.getGlobalNucleus().resolveName("/nm/utils/GenericLogger");
    
    pRequest = request;
    
    this.itemCount = itemCount;
    this.columnCount = columnCount;
  }
  
  public SuperViewAllLogicHelper(DynamoHttpServletRequest request) {
    log = (GenericLogger) Nucleus.getGlobalNucleus().resolveName("/nm/utils/GenericLogger");
    
    pRequest = request;
    
    int DEFAULT_NUMBER_OF_ELEMENTS_PER_PAGE = 36;
    int DEFAULT_NUMBER_OF_ELEMENTS_IN_ROW = 4;
    
    this.itemCount = DEFAULT_NUMBER_OF_ELEMENTS_PER_PAGE;
    this.columnCount = DEFAULT_NUMBER_OF_ELEMENTS_IN_ROW;
  }
  
  public List<SuperProduct> renderDisplayableProductList(NMCategory topCategory) throws ServletException, IOException {
    
    String topCategoryDisplayName = null;
    String firstLevelCategoryDisplayName = null;
    String currentCategoryDisplayName = null;
    String headerDisplayName = null;
    String currentCategoryId = null;// addition for sitetracker
    List<SuperProduct> superProducts = new ArrayList<SuperProduct>();
    
    try {
      List<RepositoryItem> topCategoryProducts = null;
      List<RepositoryItem> currentCategoryProducts = null;
      if (topCategory != null) {
        topCategoryDisplayName = (String) topCategory.getPropertyValue("displayName");
        currentCategoryDisplayName = (String) topCategory.getPropertyValue("displayName");
        currentCategoryId = (String) topCategory.getRepositoryId();// addition for sitetracker
        // Check for child categories (level 1) under the top category
        List<NMCategory> level_1_FilteredCats = topCategory.getOrderedChildCategories(true);
        if (level_1_FilteredCats != null && !level_1_FilteredCats.isEmpty()) {
          if (log.isLoggingDebug()) {
            log.debug(">>>>>>>>DEBUG****** SuperViewAllLogicHelper: FILTERED - Level 1 Categories = " + level_1_FilteredCats);
          }
          
          // Loop each level 1 category checking to see if they have subcategories (level 2)
          List<NMCategory> level_2_FilteredCats = null;
          
          for (NMCategory currentLevel_1_Cat : level_1_FilteredCats) {
            firstLevelCategoryDisplayName = (String) currentLevel_1_Cat.getPropertyValue("displayName");
            currentCategoryDisplayName = (String) currentLevel_1_Cat.getPropertyValue("displayName");
            currentCategoryId = (String) currentLevel_1_Cat.getRepositoryId(); // addition for sitetracker
            
            level_2_FilteredCats = currentLevel_1_Cat.getOrderedChildCategories(true);
            if (level_2_FilteredCats != null && !level_2_FilteredCats.isEmpty()) {
              // The subcategories must be filtered out according to the session based filter code for category group
              if (log.isLoggingDebug()) {
                log.debug(">>>>>>>>DEBUG****** SuperViewAllLogicHelper: Level 1 Category (" + currentLevel_1_Cat.getRepositoryId() + ") has these FILTERED subCategories = " + level_2_FilteredCats);
              }
              
              // Loop each level 2 filtered categories and process the products under each
              currentCategoryProducts = null;
              for (NMCategory currentLevel_2_Cat : level_2_FilteredCats) {
                currentCategoryDisplayName = (String) currentLevel_2_Cat.getPropertyValue("displayName");
                currentCategoryId = (String) currentLevel_2_Cat.getRepositoryId();// addition for sitetracker
                headerDisplayName = topCategoryDisplayName + ": " + firstLevelCategoryDisplayName + ": " + currentCategoryDisplayName;
                
                currentCategoryProducts = (List<RepositoryItem>) currentLevel_2_Cat.getPropertyValue("childProducts");
                if (currentCategoryProducts != null) {
                  processProducts(currentCategoryProducts, currentCategoryDisplayName, currentCategoryId, headerDisplayName, superProducts);
                  
                } else if (log.isLoggingDebug()) {
                  log.debug(">>>>>>>>DEBUG****** SuperViewAllLogicHelper: No products living directly under level 2 category (" + currentLevel_2_Cat.getRepositoryId() + ")");
                }
              }// end while loop of Level 2 categories
            } else {
              // Current level 1 category does not have subcategories - check for any products that live directly under it
              currentCategoryProducts = (List<RepositoryItem>) currentLevel_1_Cat.getPropertyValue("childProducts");
              if (currentCategoryProducts != null) {
                headerDisplayName = topCategoryDisplayName + ": " + currentCategoryDisplayName;
                processProducts(currentCategoryProducts, currentCategoryDisplayName, currentCategoryId, headerDisplayName, superProducts);
              } else if (log.isLoggingDebug()) {
                log.debug(">>>>>>>>DEBUG****** SuperViewAllLogicHelper: No categories or products live under level 1 category of " + currentLevel_1_Cat.getRepositoryId());
              }
            }
          }// end while loop of Level 1 categories
        } else {
          // No level 1 categories - check for products directly under the top category
          if (log.isLoggingDebug()) {
            log.debug(">>>DEBUG>>>SuperViewAllLogicHelper:Service: No categories live under the top category of: " + topCategory);
          }
          
          topCategoryProducts = (List<RepositoryItem>) topCategory.getPropertyValue("childProducts");
          
          if (topCategoryProducts != null) {
            headerDisplayName = topCategoryDisplayName;
            processProducts(topCategoryProducts, currentCategoryDisplayName, currentCategoryId, headerDisplayName, superProducts);
            
          } else {
            if (log.isLoggingDebug()) {
              log.debug(">>>>>>>>DEBUG****** SuperViewAllLogicHelper: No categories or products live under top category");
            }
            return superProducts;
          }
        }// end null check of level 1 Categories
      }// end null check of topCategory
    } // end try statement
    catch (Exception ex) {
      ex.printStackTrace();
      if (log.isLoggingDebug()) {
        log.debug("com.nm.commerce.pagedef.evaluator.template.SuperViewAllLogicHelper has Exception: " + ex.toString());
      }
    }
    
    return superProducts;
    
  }// end renderDisplayableProductList
  
  /*********************************************************
   * This method will first filter out only valid sellable products. Then it will loop through the valid products creating a SuperProduct object for each and adding each to the superProducts array.
   * Logic will be applied to determine the product's placement on the display page, whether or not it is at the end or beginning of a page and also if it is the last product of that category and its
   * placement on that particular row. Then the attributes of the SuperProduct object are set accordingly.
   * 
   * @param List
   *          list of product repository items
   * @param String
   *          current parent category display name
   * @param String
   *          current category Id for site tracker
   * @param String
   *          concatenated display names of top category and 1 or 2 subcats below depending on where the product lives on tree
   * @param DynamoHttpServletRequest
   * @param DynamoHttpServletResponse
   * @exception ServletException
   * @exception IOException
   ********************************************************************/
  
  private void processProducts(List<RepositoryItem> pProductList, String pCategoryDisplayName, String currentCategoryId, String pHeaderDisplayName, List<SuperProduct> superProducts)
          throws ServletException, IOException {
    
    try {
      CatalogFilter mCatalogFilter = (CatalogFilter) pRequest.resolveName("/nm/formhandler/CatalogFilter");
      mCatalogFilter.setProductList(pProductList);
      ArrayList<RepositoryItem> filteredProducts = (ArrayList<RepositoryItem>) mCatalogFilter.getFilteredList();
      int currentCategoryProductCount = 0;
      SuperProduct superProductItem = null;
      int numberOfElementsPerPage = itemCount;
      int numberOfElementsInRow = columnCount;
      
      if (log.isLoggingDebug()) {
        log.debug(">>>>>>>>DEBUG****** SuperViewAllLogicHelper - processProducts: Filtered products of category - " + pCategoryDisplayName + " are:" + filteredProducts);
      }
      
      if (previousCategoryDisplayName == null) {
        // only on the very first category, force the preivousCategoryDisplayName to be the same as the current category
        // this will ensure that if the category products continue onto another page, the (cont'd) text will be added to the header
        previousCategoryDisplayName = pCategoryDisplayName;
      }
      Iterator<RepositoryItem> productIterator = filteredProducts.iterator();
      while (productIterator.hasNext()) {
        RepositoryItem currentProduct = (RepositoryItem) productIterator.next();
        currentCategoryProductCount++;
        
        superProductItem = new SuperProduct(currentProduct);
        superProductItem.setParentCategoryName(pCategoryDisplayName);
        superProductItem.setParentCategoryId(currentCategoryId);
        superProductItem.setHeaderDisplayName(pHeaderDisplayName);
        
        // Need to add 1 to the superProducts list size to determine the new product's placement on the page
        // Once all the attributes are set, add the new SuperProduct to the list
        
        if ((superProducts.size() + 1) < numberOfElementsPerPage) {
          if (currentCategoryProductCount == 1) {
            // The current product is on the first page - the first product needs a header
            superProductItem.setFirstProductInCategory(true);
            
          } else if (!(previousCategoryDisplayName.equals(pCategoryDisplayName)) && (currentCategoryProductCount == 1)) {
            // the current product is the start of a new category in the middle of the first page and requires a new header
            superProductItem.setFirstProductInCategory(true);
            
          }
        } else {
          if (((superProducts.size() + 1) % numberOfElementsPerPage) == 0) {
            // the current product is the last product of a page - if there are more products to process
            // in this category, capture the category name and add " continue on next page" text
            if (currentCategoryProductCount < filteredProducts.size()) {
              superProductItem.setContinueOnNextPage(true);
            }
            
          } else if (((superProducts.size() + 1) % numberOfElementsPerPage) == 1) {
            if (currentCategoryProductCount == 1) {
              // the current product is the start of a new category on a new page and requires a new header
              superProductItem.setFirstProductInCategory(true);
            } else {
              // the current product is the first product at the top of a new page and is a continuation of a category
              superProductItem.setContinuedCategory(true);
            }
            
          } else if (!(previousCategoryDisplayName.equals(pCategoryDisplayName)) && (currentCategoryProductCount == 1)) {
            // the current product is the start of a new category in the middle of a page and requires a new header
            superProductItem.setFirstProductInCategory(true);
          }
        }
        if (!productIterator.hasNext()) {
          superProductItem.setLastProductInCategory(true);
        }
        
        // set index for current super product (the null spacer products on the template will not be given an index)
        superProductItem.setIndex(index);
        index = index + +1;
        
        // add each SuperProduct data holder to the superProducts arrayList which is what will feed the page elements fragment
        superProducts.add(superProductItem);
      }// end while loop of products
      
      previousCategoryDisplayName = pCategoryDisplayName;
      
      // All valid products with category have been looped - check to see if the last display row is filled with thumbnails
      // or do we need to add empty spacers to ensure the correct layout, page numbering and display of category headers
      int columnIndex = currentCategoryProductCount % numberOfElementsInRow;
      if (columnIndex > 0) {
        // The category is ending without completely filling the row with thumbnails so add spacers
        for (int i = 0; i < (numberOfElementsInRow - columnIndex); i++) {
          superProducts.add(null);
        }
      }
      
    }// end try statement
    catch (Exception ex) {
      if (log.isLoggingDebug()) {
        log.debug("com.nm.commerce.pagedef.evaluator.template.SuperViewAllLogicHelper - processProducts method has Exception: " + ex.toString());
      }
      
    }
  }// end processProducts
  
}
