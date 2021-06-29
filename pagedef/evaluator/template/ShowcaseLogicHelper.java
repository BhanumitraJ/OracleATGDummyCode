package com.nm.commerce.pagedef.evaluator.template;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;

import atg.repository.RepositoryItem;
import atg.servlet.DynamoHttpServletRequest;
import atg.servlet.ServletUtil;

import com.nm.catalog.navigation.NMCategory;
import com.nm.commerce.catalog.SuperProduct;
import com.nm.commerce.pagedef.definition.ProductTemplatePageDefinition;
import com.nm.droplet.FilterCategoryGroup;
import com.nm.formhandler.CatalogFilter;
import com.nm.utils.CategoryFilterCode;
import com.nm.utils.GenericLogger;

/**
 * 
 * @author nmjh94 This class populates a SuperProduct list for showcase.jsp display purpose
 * 
 */
public class ShowcaseLogicHelper {
  List<SuperProduct> superProducts = new ArrayList<SuperProduct>();
  Map<SuperProduct, List<SuperProduct>> superProdMap = new LinkedHashMap<SuperProduct, List<SuperProduct>>();
  
  protected DynamoHttpServletRequest getRequest() {
    return ServletUtil.getCurrentRequest();
  }
  
  protected CategoryFilterCode getCategoryFilterCode() {
    return (CategoryFilterCode) getRequest().resolveName("/nm/utils/CategoryFilterCode");
  }
  
  protected FilterCategoryGroup getFilterCategoryGroup() {
    return (FilterCategoryGroup) getRequest().resolveName("/nm/droplet/FilterCategoryGroup");
  }
  
  public List<SuperProduct> rederDisplayableProductList(final ProductTemplatePageDefinition pageDefinition, final DynamoHttpServletRequest pRequest, final NMCategory category,
          final GenericLogger log, final List<NMCategory> childCategories) throws ServletException, IOException {
    List<RepositoryItem> currentCategoryProducts = null;
    NMCategory childCategory;
    if ((childCategories == null) || (childCategories.size() == 0)) {
      return superProducts;
    } else {
      final Iterator<NMCategory> children = childCategories.iterator();
      while (children.hasNext()) {
        childCategory = children.next();
        currentCategoryProducts = (List<RepositoryItem>) (childCategory.getPropertyValue("childProducts"));
        processProducts(currentCategoryProducts, childCategory, pRequest, pageDefinition, log);
      }
    }
    return superProducts;
  }
  
  private void processProducts(List<RepositoryItem> prodList, final NMCategory parentCategory, final DynamoHttpServletRequest pRequest, final ProductTemplatePageDefinition pageDefinition,
          final GenericLogger log) throws ServletException, IOException {
    try {
      final CatalogFilter mCatalogFilter = (CatalogFilter) pRequest.resolveName("/nm/formhandler/CatalogFilter");
      if (parentCategory != null) {
        mCatalogFilter.setIgnoreDisplayFlag(parentCategory.getFlgAlwaysShowProducts());
      }
      // Grabs the list of products, filters based on the CM3 flag Hide Internationally then returns back the list
      prodList = getFilterCategoryGroup().filterCategoryProductList(prodList);
      mCatalogFilter.setProductList(prodList);
      
      final ArrayList<RepositoryItem> filteredProducts = mCatalogFilter.getFilteredList();
      
      RepositoryItem currentProd = null;
      SuperProduct superProductItem = null;
      /** if no valid products to display, still show the empty category, add null to superProducts to hold place **/
      if (filteredProducts.size() == 0) {
        superProductItem = new SuperProduct(currentProd);
        superProductItem.setFirstProductInCategory(true);
        superProductItem.setTotalProductsInCategory(0);
        superProductItem.setParentCategory(parentCategory);
        superProducts.add(superProductItem);
      } else {
        final Iterator<RepositoryItem> productIterator = filteredProducts.iterator();
        int currentItemCount = 0;
        final int totalItemToDisplay = pageDefinition.getItemCount();
        
        while (productIterator.hasNext() && (currentItemCount < totalItemToDisplay)) {
          currentProd = productIterator.next();
          superProductItem = new SuperProduct(currentProd);
          currentItemCount++;
          if (currentItemCount == 1) {
            superProductItem.setFirstProductInCategory(true);
            superProductItem.setTotalProductsInCategory(filteredProducts.size());
          }
          superProductItem.setParentCategory(parentCategory);
          if ((currentItemCount == totalItemToDisplay) || !productIterator.hasNext()) {
            superProductItem.setLastProductInCategory(true);
          }
          superProducts.add(superProductItem);
        }
      }
    }// end try statement
    catch (final Exception ex) {
      if (log.isLoggingDebug()) {
        log.debug("com.nm.commerce.pagedef.evaluator.template.ShowcaseLogicHelper - processProducts method has Exception: " + ex.toString());
      }
      if (log.isLoggingError()) {
        log.debug("com.nm.commerce.pagedef.evaluator.template.ShowcaseLogicHelper - processProducts method has Exception: ", ex);
      }
    }
  }
  
  /**
   * Creating a map holds category as key and list of SuperProducts as value. Where value contains all its preferred products and child products,
   * 
   * @param pageDefinition
   * @param pRequest
   * @param category
   * @param log
   * @param childCategories
   * @return
   */
  public Map<SuperProduct, List<SuperProduct>> getSiloCarouselProdMap(final ProductTemplatePageDefinition pageDefinition, final DynamoHttpServletRequest pRequest, final NMCategory category,
          final GenericLogger log, final List<NMCategory> childCategories, final Boolean isMobile, final boolean fromEvalutor) {
    try {
      List<RepositoryItem> prefProductList = null;
      List<RepositoryItem> nonPrefProductList = null;
      List<RepositoryItem> maxPrefProductList = null;
      long startTime = 0;
      NMCategory childCategory;
      int prefProductCount = 0;
      int countSubCat = 0;
      if ((childCategories == null) || (childCategories.size() == 0)) {
        return superProdMap;
      } else {
        
        int categoryCarouselCountThreshold = pageDefinition.getCarouselsCount();
        if (isMobile) {
          categoryCarouselCountThreshold = pageDefinition.getCarouselsCountForMobile();
        }
        if (fromEvalutor && pageDefinition.getEnableLazyLoading()) {
          categoryCarouselCountThreshold = pageDefinition.getInitialCarouselsCount();
        }
        final Iterator<NMCategory> children = childCategories.iterator();
        while (children.hasNext() && (countSubCat < categoryCarouselCountThreshold)) {
          childCategory = children.next();
          if (isLoggingDebug()) {
            startTime = System.currentTimeMillis();
          }
          prefProductCount = 0;
          maxPrefProductList = new ArrayList<RepositoryItem>();
          
          prefProductList = childCategory.getPreferredProductItems();
          nonPrefProductList = (List<RepositoryItem>) (childCategory.getPropertyValue("childProducts"));
          
          if (prefProductList != null) {
            prefProductCount = prefProductList.size();
          }
          if (prefProductCount > pageDefinition.getPrefProductCount()) {
            
            for (int i = 0; i < pageDefinition.getPrefProductCount(); i++) {
              if ((prefProductList != null) && !prefProductList.isEmpty()) {
                maxPrefProductList.add(prefProductList.get(i));
              }
            }
            if (nonPrefProductList != null) {
              maxPrefProductList.addAll(nonPrefProductList);
            }
            getSiloCatProducts(maxPrefProductList, childCategory, pRequest, pageDefinition, log, isMobile);
          } else {
            if ((prefProductList != null) && (prefProductList.size() > 0) && (nonPrefProductList != null)) {
              prefProductList.addAll(nonPrefProductList);
            } else if (nonPrefProductList != null) {
              prefProductList = nonPrefProductList;
            }
            getSiloCatProducts(prefProductList, childCategory, pRequest, pageDefinition, log, isMobile);
          }
          
          countSubCat++;
          if (isLoggingDebug()) {
            logMsg("Time taken to process products under category :" + childCategory.getDisplayName() + ": is" + (System.currentTimeMillis() - startTime));
          }
        }
      }
      
    } catch (final Exception ex) {
      if (log.isLoggingError()) {
        log.debug("getSiloCarouselProdList method has Exception: ", ex);
      }
      
    }
    return superProdMap;
  }
  
  /**
   * This method process given list of products, validates and filter out all the non displayable products.
   * 
   * @param prodList
   * @param nmCategory
   * @param pRequest
   * @param pageDefinition
   * @param log
   * @throws ServletException
   * @throws IOException
   */
  public void getSiloCatProducts(List<RepositoryItem> prodList, final NMCategory nmCategory, final DynamoHttpServletRequest pRequest, final ProductTemplatePageDefinition pageDefinition,
          final GenericLogger log, final Boolean isMobile) throws ServletException, IOException {
    final NMCategory parentCategory = nmCategory;
    try {
      final CatalogFilter mCatalogFilter = (CatalogFilter) pRequest.resolveName("/nm/formhandler/CatalogFilter");
      if (parentCategory != null) {
        mCatalogFilter.setIgnoreDisplayFlag(parentCategory.getFlgAlwaysShowProducts());
      }
      
      prodList = getFilterCategoryGroup().filterCategoryProductList(prodList);
      mCatalogFilter.setProductList(prodList);
      
      final ArrayList<RepositoryItem> filteredProds = mCatalogFilter.getFilteredList();
      final ArrayList<RepositoryItem> filteredProducts = removeDuplicate(filteredProds);
      
      RepositoryItem currentProd = null;
      SuperProduct superProductItem = null;
      
      if (filteredProducts.size() == 0) {
        superProductItem = new SuperProduct(currentProd);
        superProductItem.setFirstProductInCategory(true);
        superProductItem.setTotalProductsInCategory(0);
        superProductItem.setParentCategory(parentCategory);
      } else {
        final Iterator<RepositoryItem> productIterator = filteredProducts.iterator();
        int currentItemCount = 0;
        SuperProduct superProductkey = null;
        int totalItemToDisplay = pageDefinition.getItemCount();
        if (isMobile) {
          totalItemToDisplay = pageDefinition.getItemCountForMobile();
        }
        while (productIterator.hasNext() && (currentItemCount < totalItemToDisplay)) {
          currentProd = productIterator.next();
          superProductItem = new SuperProduct(currentProd);
          currentItemCount++;
          if (currentItemCount == 1) {
            superProductkey = superProductItem;
            superProdMap.put(superProductkey, new ArrayList<SuperProduct>());
            superProdMap.get(superProductkey).add(superProductItem);
            superProductItem.setFirstProductInCategory(true);
            superProductItem.setTotalProductsInCategory(filteredProducts.size());
            
          } else {
            superProdMap.get(superProductkey).add(superProductItem);
          }
          superProductItem.setParentCategory(parentCategory);
          if ((currentItemCount == totalItemToDisplay) || !productIterator.hasNext()) {
            superProductItem.setLastProductInCategory(true);
          }
        }
      }
    }// end try statement
    catch (final Exception ex) {
      if (isLoggingError()) {
        logError("getSiloCarouselProdList method has Exception: " + ex);
      }
      ex.printStackTrace();
    }
  }
  
  /**
   * This method removes the duplicate products from the list
   * 
   * @param prodList
   * @return
   */
  public ArrayList<RepositoryItem> removeDuplicate(final ArrayList<RepositoryItem> prodList) {
    final Set<RepositoryItem> uniqueProdSet = new LinkedHashSet<RepositoryItem>(prodList);
    final ArrayList<RepositoryItem> uniqueProdList = new ArrayList<RepositoryItem>(uniqueProdSet);
    return uniqueProdList;
    
  }
  
  public void logMsg(final String value) {
    getCategoryFilterCode().logDebug(value);
  }
  
  public void logError(final String value) {
    getCategoryFilterCode().logError(value);
  }
  
  public boolean isLoggingDebug() {
    return getCategoryFilterCode().isLoggingDebug();
    
  }
  
  public boolean isLoggingError() {
    return getCategoryFilterCode().isLoggingError();
    
  }
}
