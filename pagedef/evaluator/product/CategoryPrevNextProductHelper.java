package com.nm.commerce.pagedef.evaluator.product;

import java.util.List;

import atg.servlet.DynamoHttpServletRequest;

import com.nm.catalog.navigation.CategoryHelper;
import com.nm.catalog.navigation.NMCategory;
import com.nm.commerce.catalog.NMPagingPrevNext;
import com.nm.commerce.catalog.NMProduct;
import com.nm.commerce.catalog.SuperProduct;
import com.nm.commerce.pagedef.evaluator.template.SuperViewAllLogicHelper;
import com.nm.formhandler.CatalogFilter;
import com.nm.utils.CategoryProductArray;

public class CategoryPrevNextProductHelper {
  
  public static NMPagingPrevNext getPagingForCategory(String categoryId, int index, DynamoHttpServletRequest request) {
    
    NMCategory nmCat = CategoryHelper.getInstance().getNMCategory(categoryId);
    
    if (nmCat != null) {
      // populate current list of products
      CategoryProductArray categoryProductArray = new CategoryProductArray();
      if (nmCat != null) {
        if (nmCat.getFlgSuperViewAll()) {
          SuperViewAllLogicHelper helper = new SuperViewAllLogicHelper(request);
          List<SuperProduct> superProducts = null;
          
          try {
            superProducts = helper.renderDisplayableProductList(nmCat);
          } catch (Exception e) {}
          
          if (superProducts != null) {
            categoryProductArray.setSuperProductArrayList(superProducts);
          } else {
            return null;
          }
          
        } else if (nmCat.getFlgAlwaysShowProducts()) {
          // Do not filter out non-sellable prods
          categoryProductArray.setProductArrayList(nmCat.getChildProducts());
        } else if (!nmCat.getFlgAlwaysShowProducts()) {
          CatalogFilter filter = new CatalogFilter();
          filter.setProductList(nmCat.getChildProducts());
          categoryProductArray.setProductArrayList(filter.getFilteredList());
        }
      }
      
      // Populate ProductPaging bean - this bean holds the data
      // to create the paging link on the jsp
      NMPagingPrevNext productPaging = new NMPagingPrevNext();
      productPaging.setCategoryId(categoryId);
      productPaging.setIndex(index + 1);
      productPaging.setTotalProducts(getListSize(categoryProductArray));
      
      if (productPaging.getTotalProducts() > 1 && index < productPaging.getTotalProducts() - 1) {
        productPaging.setNextProduct(new NMProduct(categoryProductArray.getProductArrayList().get(index + 1)));
      }
      
      if (index > 0 && index < productPaging.getTotalProducts()) {
        productPaging.setPreviousProduct(new NMProduct(categoryProductArray.getProductArrayList().get(index - 1)));
      }
      
      return productPaging;
      
    }
    
    return null;
  }
  
  private static int getListSize(CategoryProductArray categoryProductArray) {
    
    if (categoryProductArray != null && categoryProductArray.getProductArrayList() != null) {
      return categoryProductArray.getProductArrayList().size();
    }
    
    return 0;
  }
}
