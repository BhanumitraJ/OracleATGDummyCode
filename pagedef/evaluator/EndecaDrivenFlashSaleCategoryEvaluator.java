package com.nm.commerce.pagedef.evaluator;

import com.nm.catalog.navigation.NMCategory;
import com.nm.catalog.navigation.NMCategory.FlashSaleStatus;
import com.nm.commerce.pagedef.definition.EndecaFlashSaleTemplatePageDefinition;
import com.nm.commerce.pagedef.definition.PageDefinition;
import com.nm.commerce.pagedef.model.EndecaDrivenPageModel;
import com.nm.commerce.pagedef.model.PageModel;
import com.nm.servlet.NMRequestHelper;
import com.nm.utils.SeoUrlUtil;
import com.nm.utils.StringUtilities;

import atg.servlet.DynamoHttpServletRequest;
import atg.servlet.DynamoHttpServletResponse;

import javax.servlet.jsp.PageContext;

public class EndecaDrivenFlashSaleCategoryEvaluator extends EndecaDrivenCategoryEvaluator {

  public static final String FIRST_PAGE_KEY = "NM_FIRST_PAGE_DEFINITON";

  /*
   * (non-Javadoc)
   *
   * @see com.nm.commerce.pagedef.evaluator.EndecaDrivenCategoryEvaluator#allocatePageModel()
   */
  @Override
  protected PageModel allocatePageModel() {
    return new EndecaDrivenPageModel();
  }

  /*
   * (non-Javadoc)
   *
   * @see com.nm.commerce.pagedef.evaluator.EndecaDrivenCategoryEvaluator#evaluate(com.nm.commerce.pagedef.definition.PageDefinition, javax.servlet.jsp.PageContext)
   */
  @Override
  public boolean evaluate(PageDefinition pageDefinition, PageContext pageContext) throws Exception {
    EndecaFlashSaleTemplatePageDefinition pageDef = (EndecaFlashSaleTemplatePageDefinition) pageDefinition;
    DynamoHttpServletRequest request = getRequest();
    boolean isDisplayMobile = NMRequestHelper.isMobileRequest(request);
    // LC Mobile Version Switcher
    if (StringUtilities.isNotEmpty(pageDef.getMobilePageDefinitionPath()) && isDisplayMobile) {
      pageDefinition = (PageDefinition) request.resolveName(pageDef.getMobilePageDefinitionPath());
      request.setAttribute(FIRST_PAGE_KEY, pageDefinition);
    }
    boolean returnValue = super.evaluate(pageDefinition, pageContext);
    EndecaDrivenPageModel pageModel = (EndecaDrivenPageModel) getPageModel();
    NMCategory nmCategory = getCategory();
    pageModel.setFlashSaleActive(false);
    pageModel.setDisplayMobile(isDisplayMobile);
    NMCategory flashSaleRootCategory = getCategory(pageDef.getFlashSaleCategoryId()); // Flash Sales uses a hard-coded category, flashSaleCategory
    FlashSaleStatus status = nmCategory.getFlashSaleStatus(false);
    if (status == FlashSaleStatus.EXPIRED) {
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
      populateFlashSale(flashSaleRootCategory, pageModel, false);
      pageModel.setFlashSaleActive(true);
    }
    return returnValue;
  }
  
}
