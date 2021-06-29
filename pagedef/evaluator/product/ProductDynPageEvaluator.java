package com.nm.commerce.pagedef.evaluator.product;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.PageContext;

import atg.servlet.DynamoHttpServletRequest;
import atg.servlet.DynamoHttpServletResponse;

import com.nm.commerce.pagedef.definition.PageDefinition;
import com.nm.commerce.pagedef.evaluator.SimplePageEvaluator;
import com.nm.commerce.pagedef.model.PageModel;
import com.nm.commerce.pagedef.model.ProductPageModel;
import com.nm.components.CommonComponentHelper;
import com.nm.droplet.FilterRedirect;
import com.nm.utils.GenericLogger;
import com.nm.utils.NmoUtils;

/**
 * Updates variables and sets up any redirects before page execution. Returns true if page content should be output.
 */
public class ProductDynPageEvaluator extends SimplePageEvaluator {
  
  protected GenericLogger log = CommonComponentHelper.getLogger();
  
  @Override
  protected PageModel allocatePageModel() {
    return new ProductPageModel();
  }
  
  @Override
  public boolean evaluate(PageDefinition pageDefinition, PageContext pageContext) throws Exception {
    if (requiresRedirect()) {
      return false;
    }
    
    boolean returnValue = super.evaluate(pageDefinition, pageContext);
    DynamoHttpServletRequest request = getRequest();
    String productIds = request.getParameter("itemIds");
    
    if (!NmoUtils.isEmpty(productIds)) {
      return true;
    } else {
      returnValue = false;
      redirectToProductNotFound(request);
    }
    return returnValue;
  }
  
  private void redirectToProductNotFound(DynamoHttpServletRequest request) throws IOException {
    DynamoHttpServletResponse response = getResponse();
    String queryString = request.getQueryString();
    if (queryString != null && queryString.length() > 0) {
      queryString = "?" + queryString;
    }
    response.sendRedirect("/product/productNotFound.jsp" + queryString);
    response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
  }
  
  /**
   * Uses the FilterRedirect droplet's evaluations methods to determine if the user has access to the category specified. This should redirect if they have the staging category in their path and they
   * are not authorized.
   * 
   * @return true if the system has redirected
   * @throws IOException
   */
  private boolean requiresRedirect() throws IOException {
    boolean isRedirected = false;
    DynamoHttpServletRequest request = getRequest();
    FilterRedirect filter = (FilterRedirect) request.resolveName("/nm/droplet/FilterRedirect");
    request.setParameter(FilterRedirect.CATEGORY_TO_FILTER, getBrandSpecs().getHiddenCategoryId());
    request.setParameter(FilterRedirect.FILTER_TYPE, "HIDDEN");
    isRedirected = filter.restrictCategory(request);
    if (isRedirected) {
      getResponse().sendRedirect(getBrandSpecs().getCategoryNotFoundPath());
      getResponse().setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
    }
    return isRedirected;
  }
}
