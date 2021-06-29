package com.nm.commerce.pagedef.evaluator.product;

import atg.nucleus.Nucleus;
import atg.servlet.DynamoHttpServletRequest;

import com.nm.catalog.history.NMCatalogHistory;
import com.nm.catalog.navigation.CategoryHelper;
import com.nm.catalog.navigation.NMCategory;
import com.nm.commerce.catalog.NMPagingPrevNext;
import com.nm.utils.GenericLogger;

// package access
class ProductPagePrevNextHelper {
  
  private GenericLogger log;
  private DynamoHttpServletRequest request;
  
  public ProductPagePrevNextHelper(DynamoHttpServletRequest request) {
    this.request = request;
    log = (GenericLogger) Nucleus.getGlobalNucleus().resolveName("/nm/utils/GenericLogger");
  }
  
  public NMPagingPrevNext getPrevAndNextProducts() {
    if (request == null) {
      if (log.isLoggingDebug()) {
        log.debug("No DynamoHttpServletRequest - can't determin prev/next products");
      }
      return null;
    }
    
    NMCatalogHistory catalogHistory = (NMCatalogHistory) request.resolveName(NMCatalogHistory.COMPONENT_NAME);
    NMPagingPrevNext productPaging = null;
    if (catalogHistory.getLastShoppableClick() != null && "endeca".equals(catalogHistory.getLastShoppableClick().getType())) {
      // NOT READY FOR ENDECA PAGING YET
      // String currentProdE = request.getParameter("eItemId");
      // String currentProdP = request.getParameter("itemId");
      // String pageSize = request.getParameter("pageSize");
      // String position = request.getParameter("position");
      //
      //
      // EndecaPrevNextProductHelper endecaPrevNextProductHelper
      // = new EndecaPrevNextProductHelper(currentProdE, currentProdP, pageSize, position, catalogHistory, request);
      // productPaging = endecaPrevNextProductHelper.getEndecaPaging();
    } else if (catalogHistory.getLastShoppableClick() != null && "category".equals(catalogHistory.getLastShoppableClick().getType())) {
      productPaging = getPrevNextFromCategory();
    }
    
    return productPaging;
  }
  
  private NMPagingPrevNext getPrevNextFromCategory() {
    String categoryId = request.getParameter("parentId");
    String index = request.getParameter("index");
    String cmCat = request.getParameter("cmCat");
    
    try {
      
      if (!"search".equals(cmCat) && categoryId != null && categoryId != "") {
        NMCategory nmCat = CategoryHelper.getInstance().getNMCategory(categoryId);
        if (nmCat != null) {
          if (nmCat.getTemplateUrl().indexOf("showcase.jsp") != -1 || nmCat.getTemplateUrl().indexOf("commGroup.jsp") != -1) {
            categoryId = request.getParameter("overrideCategoryId");
          }
          
          int intIndex = -1;
          if (index == null) {
            intIndex = 0;
          } else {
            try {
              intIndex = Integer.parseInt(index);
            } catch (NumberFormatException e) {}
          }
          
          return CategoryPrevNextProductHelper.getPagingForCategory(categoryId, intIndex, request);
        }
      }
    } catch (Exception e) {
      log.error("Unable to generate NMPagingPrevNext: categoryId[" + categoryId + "] index[" + index + "]");
    }
    
    return null;
  }
  
}
