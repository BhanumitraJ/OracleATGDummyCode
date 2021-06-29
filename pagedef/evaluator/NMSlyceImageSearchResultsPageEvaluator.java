package com.nm.commerce.pagedef.evaluator;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.servlet.jsp.PageContext;

import atg.core.util.StringUtils;
import atg.repository.RepositoryItem;
import atg.servlet.DynamoHttpServletRequest;

import com.nm.commerce.catalog.NMProduct;
import com.nm.commerce.checkout.CheckoutComponents;
import com.nm.commerce.pagedef.definition.PageDefinition;
import com.nm.commerce.pagedef.definition.ProductTemplatePageDefinition;
import com.nm.commerce.pagedef.model.NMSlyceImageSearchResultsPageModel;
import com.nm.commerce.pagedef.model.PageModel;
import com.nm.components.CommonComponentHelper;
import com.nm.tms.core.TMSMessageContainer;
import com.nm.tms.utils.TMSDataDictionaryUtils;
import com.nm.utils.ProdSkuUtil;

/**
 * This evaluator retrives the product details using the product ids sent by slyce
 */
public class NMSlyceImageSearchResultsPageEvaluator extends SimplePageEvaluator {
  private final String SLYCE_REQUEST_PARAM = "prodIds";
  private final String PAGE_TYPE = "Slyce";
  
  @Override
  public boolean evaluate(final PageDefinition pageDefinition, final PageContext pageContext) throws Exception {
    if (!super.evaluate(pageDefinition, pageContext)) {
      return false;
    }
    final DynamoHttpServletRequest request = getRequest();
    final String productIds = request.getParameter(SLYCE_REQUEST_PARAM);
    final NMSlyceImageSearchResultsPageModel pageModel = (NMSlyceImageSearchResultsPageModel) getPageModel();
    if (!StringUtils.isBlank(productIds)) {
      
      final ProductTemplatePageDefinition productPageDefinition = (ProductTemplatePageDefinition) pageDefinition;
      final StringTokenizer tokens = new StringTokenizer(productIds, ",");
      
      final List<String> slyceProdIdList = new ArrayList<String>();
      
      while (tokens.hasMoreElements()) {
        slyceProdIdList.add(tokens.nextToken());
      }
      final List<NMProduct> displayableProducts = getDisplayableProducts(slyceProdIdList, productPageDefinition.getSlyceProductCount());
      pageModel.setFirstThumbnailIndex(1);
      pageModel.setThumbnailCount(displayableProducts.size());
      pageModel.setDisplayableProducts(displayableProducts);
    }
    pageModel.setPageType(PAGE_TYPE);
    final TMSMessageContainer tmsMessageContainer = CheckoutComponents.getTMSMessageContainer(getRequest());
    final TMSDataDictionaryUtils dataDictionaryUtils = CommonComponentHelper.getDataDictionaryUtils();
    dataDictionaryUtils.processTMSDataDictionaryAttributes(pageDefinition, null, getProfile(), pageModel, tmsMessageContainer);
    
    return true;
  }
  
  @Override
  protected PageModel allocatePageModel() {
    return new NMSlyceImageSearchResultsPageModel();
  }
  
  /**
   * This method retrieves the displayable products list and it filters the blacklisted items
   * 
   * @param productIds
   * @return
   */
  public List<NMProduct> getDisplayableProducts(final List<String> productIds, final int slyceProductCount) {
    final List<RepositoryItem> prodItems = new ArrayList<RepositoryItem>();
    List<NMProduct> products = new ArrayList<NMProduct>();
    final ProdSkuUtil prodSkuUtil = CommonComponentHelper.getProdSkuUtil();
    RepositoryItem prodRepositoryItem = null;
    for (final String prodId : productIds) {
      prodRepositoryItem = prodSkuUtil.getProductRepositoryItem(prodId);
      if (null != prodRepositoryItem) {
        prodItems.add(prodRepositoryItem);
      }
    }
    products = getFilterCategoryGroup().filterProductList(prodItems);
    if (products.size() > slyceProductCount) {
      products = products.subList(0, slyceProductCount);
    }
    return products;
    
  }
  
}
