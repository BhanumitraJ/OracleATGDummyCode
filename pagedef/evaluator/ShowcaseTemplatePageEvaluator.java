package com.nm.commerce.pagedef.evaluator;

import java.util.List;

import javax.servlet.jsp.PageContext;

import atg.servlet.DynamoHttpServletRequest;

import com.nm.catalog.navigation.NMCategory;
import com.nm.commerce.catalog.SuperProduct;
import com.nm.commerce.pagedef.definition.PageDefinition;
import com.nm.commerce.pagedef.definition.ProductTemplatePageDefinition;
import com.nm.commerce.pagedef.evaluator.template.ShowcaseLogicHelper;
import com.nm.commerce.pagedef.model.TemplatePageModel;
import com.nm.components.CommonComponentHelper;

/**
 * Uses the ShowcaseLogicHelper to establish super product list.
 */
public class ShowcaseTemplatePageEvaluator extends TemplatePageEvaluator {
  
  @Override
  public boolean evaluate(final PageDefinition pageDefinition, final PageContext pageContext) throws Exception {
    if (!super.evaluate(pageDefinition, pageContext)) {
      return false;
    }
    final TemplatePageModel templatePageModel = (TemplatePageModel) getPageModel();
    final ProductTemplatePageDefinition productPageDefinition = (ProductTemplatePageDefinition) templatePageModel.getPageDefinition();
    templatePageModel.setPartiallyEvaluated(true);
    /* Process the TMS data dictionary attributes */
    CommonComponentHelper.getTemplateUtils().processTMSDataDictionaryForCategoryPages(productPageDefinition, templatePageModel, Boolean.FALSE);
    return true;
  }

  @Override
  public void evaluateContent(final PageContext context) throws Exception {
    final DynamoHttpServletRequest request = this.getRequest();
    final TemplatePageModel templatePageModel = (TemplatePageModel) getPageModel();
    final ProductTemplatePageDefinition productPageDefinition = (ProductTemplatePageDefinition) templatePageModel.getPageDefinition();
    final NMCategory category = templatePageModel.getCategory();

    category.setFilterCategoryGroup(getFilterCategoryGroup());
    final List<NMCategory> childCategories = category.getOrderedChildCategories(true);
    final ShowcaseLogicHelper showcaseHelper = new ShowcaseLogicHelper();
    final List<SuperProduct> superProducts = showcaseHelper.rederDisplayableProductList(productPageDefinition, request, category, log, childCategories);
    templatePageModel.setDisplayableSuperProductList(superProducts);
    templatePageModel.setRecordCount(superProducts.size());
    /* Process the TMS data dictionary attributes */
    CommonComponentHelper.getTemplateUtils().processTMSDataDictionaryForCategoryPages(productPageDefinition, templatePageModel, Boolean.TRUE);
  }
}
