package com.nm.commerce.pagedef.evaluator.product;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.PageContext;

import atg.servlet.DynamoHttpServletRequest;

import com.nm.commerce.catalog.NMProduct;
import com.nm.commerce.pagedef.definition.PageDefinition;
import com.nm.commerce.pagedef.evaluator.SimplePageEvaluator;
import com.nm.components.CommonComponentHelper;
import com.nm.utils.GenericLogger;
import com.nm.utils.SeoUrlUtil;

/**
 * Somehow as part of the 2012 NM AB product page test, the direct link to the AB Test was exposed by the business. It appears that there are still some bots which are hitting this page resulting a
 * number of errors in the logs. For 120 days we are doing a 301 redirect to the real product page after which this entire class and its associated properties can be deleted along with the
 * /page/abtest/product.jsp page as well.
 * 
 * @author nmjjm4
 * 
 */
public class ABProductPageCleanUpEvaluator extends SimplePageEvaluator {
  
  protected GenericLogger log = CommonComponentHelper.getLogger();
  
  public boolean evaluate(PageDefinition pageDefinition, PageContext pageContext) throws Exception {
    DynamoHttpServletRequest request = getRequest();
    try {
      if (log.isLoggingDebug()) { 
    	  log.debug(request.getRequestURI());
    	  log.debug("Request for deprecated AB product page identified: " + request.getRequestURIWithQueryString());
      }
      String productId = request.getParameter("itemId");
      if (productId != null) {
        NMProduct product = CommonComponentHelper.getProdSkuUtil().getProductObject(productId);
        if (product != null) {
          String newUrl = null;
          if (SeoUrlUtil.isProductSeoEnabled()) {
            newUrl =
                    SeoUrlUtil.buildProductSemanticPath(product.getWebDesignerName(), product.getDisplayName(), null, request.getParameter("itemId"), request.getParameter("parentId"),
                            request.getParameter("masterId"), request.getParameter("grandMasterId"));
          } else {
            newUrl = request.getRequestURIWithQueryString().replace("/page/abtest/product.jsp", "/product.jsp");
          }
          if (newUrl != null) {
            log.warn("Performing 301 redirect to " + newUrl);
            getResponse().sendRedirect(newUrl);
            getResponse().setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
          }
        } else {
          log.warn("Product not found.  Redirecting to " + getBrandSpecs().getCategoryNotFoundPath());
          getResponse().sendRedirect(getBrandSpecs().getCategoryNotFoundPath());
          getResponse().setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
        }
      }
    } catch (Exception e) {
      String excmsg = "Exception " + e.getMessage() + " while handling redirect for deprecated AB product page ";
      if (request != null) {
        excmsg += "for request " + request.getRequestURIWithQueryString();
      }
      log.warn(excmsg);
    }
    return false;
  }
  
}
