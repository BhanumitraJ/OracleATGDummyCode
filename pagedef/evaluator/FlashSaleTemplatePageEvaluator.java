package com.nm.commerce.pagedef.evaluator;

import com.nm.catalog.navigation.NMCategory;
import com.nm.catalog.navigation.NMCategory.FlashSaleStatus;
import com.nm.commerce.pagedef.definition.FlashSaleTemplatePageDefinition;
import com.nm.commerce.pagedef.definition.PageDefinition;
import com.nm.commerce.pagedef.model.FlashSaleTemplatePageModel;
import com.nm.commerce.pagedef.model.PageModel;
import com.nm.servlet.NMRequestHelper;
import com.nm.utils.SeoUrlUtil;
import com.nm.utils.StringUtilities;

import atg.servlet.DynamoHttpServletRequest;
import atg.servlet.DynamoHttpServletResponse;

import javax.servlet.jsp.PageContext;

/**
 * Performs additional checks for valid, public category. Establishes refined template page definition (P9 may become P9ViewAll).
 *
 * Andrew Longley 2012/07/14: rewrote for JSP conversion, removed checks for saleType (public/private/secure - we don't care any more), removed loopholes for showprod=true and productId being present
 * on URL. Now the logic is: A: the flash sale page will show if (1) the request is internal, or if (2) the flash sale is active (flgEnable) AND it is within the time of the flash sale B: neither of
 * these being met, if it's before the flash sale, redirect to coming soon C: else redirect to the Flash Sale silo page
 *
 * Note that we only every use the first child of the flash sale collection on each category. CM2 displays 2 sets of data for two sale definitions, but we ignore the right-hand sale definition.
 *
 * If we get to the flash sale page (A above), we build a list of flash sale categories to display in the left nav. We never include the two internal sub categories of the Fashion Dash category in CM2
 * ("featured products" and "default products", defined by the html fragments defaultProdCategoryId and featuredProdCategoryId). A1. if we're internal (internal IP, flg_enable = false), display all
 * other subcategories of Fashion Dash A2. otherwise, show each subcategory that is active (flgEnable) and that is "on" (date is within the promo dates)
 */
public class FlashSaleTemplatePageEvaluator extends ProductTemplatePageEvaluator {
  @Override
  protected PageModel allocatePageModel() {
    return new FlashSaleTemplatePageModel();
  }

  /**
   * Function determines the current flash sales, the inactive/future flash sales, and determines if we need to redirect based on parameters passed in, whether the user is anonymous or logged in, and
   * the date
   *
   */
  @Override
  public boolean evaluate(PageDefinition pageDefinition, PageContext pageContext) throws Exception {
    FlashSaleTemplatePageDefinition flashSaleTemplatePageDefinition = (FlashSaleTemplatePageDefinition) pageDefinition;
    DynamoHttpServletRequest request = this.getRequest();
    boolean isDisplayMobile = NMRequestHelper.isMobileRequest(request);
    // LC Mobile Version Switcher
    if (StringUtilities.isNotEmpty(flashSaleTemplatePageDefinition.getMobilePageDefinitionPath()) && isDisplayMobile) {
      pageDefinition = (PageDefinition) request.resolveName(flashSaleTemplatePageDefinition.getMobilePageDefinitionPath());
      request.setAttribute(FIRST_PAGE_KEY, pageDefinition);
    }
    boolean returnValue = super.evaluate(pageDefinition, pageContext);

    FlashSaleTemplatePageModel flashSaleTemplatePageModel = (FlashSaleTemplatePageModel) getPageModel();
    flashSaleTemplatePageModel.setDisplayMobile(isDisplayMobile);
    NMCategory flashSaleCategory = flashSaleTemplatePageModel.getCategory(); // derived from itemId on the URL
    // Flash Sales use two hard-coded categories, flashSaleCategory and yourFlashSaleCategory
    NMCategory flashSaleRootCategory = getCategory(flashSaleTemplatePageDefinition.getFlashSaleCategoryId());
    FlashSaleStatus status = flashSaleCategory.getFlashSaleStatus(false);
    if (status == FlashSaleStatus.EXPIRED && !flashSaleCategory.getRepositoryId().equals(flashSaleRootCategory.getRepositoryId())) {
      DynamoHttpServletResponse response = this.getResponse();
      String url = "/category.jsp?itemId=" + flashSaleRootCategory.getId();
      if (SeoUrlUtil.isCategorySeoEnabled() && flashSaleRootCategory.getId() != null) {
        String semanticUrl = SeoUrlUtil.buildSemanticPath(url);
        if (semanticUrl != null && !semanticUrl.equals("")) {
          url = semanticUrl;
        }
      }
      response.sendLocalRedirect(url, getRequest());
    } else {
      populateFlashSale(flashSaleRootCategory, flashSaleTemplatePageModel, false);
    }
    return returnValue;
  }
}
