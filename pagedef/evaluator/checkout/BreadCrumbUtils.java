/**
 * 
 */
package com.nm.commerce.pagedef.evaluator.checkout;

import atg.servlet.DynamoHttpServletRequest;

import com.nm.commerce.checkout.CheckoutComponents;
import com.nm.commerce.pagedef.definition.CheckoutPageDefinition;
import com.nm.commerce.pagedef.model.bean.Breadcrumb;

/**
 * @author vsrg4
 * 
 */
public class BreadCrumbUtils {
  
  /**
   * This method provides the Checkout Flow BreadCrumbs for International user.
   * 
   * @param currentPageDef
   *          Current Page Definition
   * @param request
   *          Dynamo Request
   * @return BreadCrumb array for requested page definition.
   */
  public static Breadcrumb[] getInternationalCheckoutBreadcrumbs(CheckoutPageDefinition currentPageDef, DynamoHttpServletRequest request) {
    Breadcrumb[] breadcrumbs;
    String currentTitle = currentPageDef.getBreadcrumbTitle();
    boolean currentFound = false;
    
    String last = request.getParameter("prev");
    int lastPage;
    try {
      lastPage = Integer.parseInt(last);
    } catch (NumberFormatException e) {
      lastPage = 0;
    }
    
    if (lastPage > 4) {
      lastPage = 0;
    }
    CheckoutPageDefinition shipping = CheckoutComponents.getShippingIntlPageDefinition();
    CheckoutPageDefinition billing = CheckoutComponents.getBillingIntlPageDefinition();
    CheckoutPageDefinition review = CheckoutComponents.getOrderReviewPageDefinition();
    CheckoutPageDefinition confirm = CheckoutComponents.getOrderConfirmationPageDefinition();
    
    breadcrumbs =
            new Breadcrumb[] {new Breadcrumb(shipping.getBreadcrumbUrl(), shipping.getBreadcrumbTitle(), 1) , new Breadcrumb(billing.getBreadcrumbUrl(), billing.getBreadcrumbTitle(), 2) ,
                new Breadcrumb(review.getBreadcrumbUrl(), review.getBreadcrumbTitle(), 3) , new Breadcrumb(confirm.getBreadcrumbUrl(), confirm.getBreadcrumbTitle(), 4)};
    
    if (currentTitle != null) {
      for (Breadcrumb breadcrumb : breadcrumbs) {
        if (!currentFound && currentTitle.equals(breadcrumb.getTitle())) {
          currentFound = true;
          breadcrumb.setCurrentPage(true);
          if (currentPageDef.getBreadcrumbSubpage()) {
            breadcrumb.setClickable(true);
            breadcrumb.setPageNum(0);
          }
        }
        if (!currentFound || breadcrumb.getPageNum() <= lastPage) {
          breadcrumb.setClickable(true);
        }
      }
    }
    
    return breadcrumbs;
  }
}
