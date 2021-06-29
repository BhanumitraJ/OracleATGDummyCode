package com.nm.commerce.pagedef.evaluator.store;

import static com.nm.tms.constants.TMSDataDictionaryConstants.STORES;
import javax.servlet.jsp.PageContext;

import com.nm.collections.StoreAndRestUtil;
import com.nm.commerce.checkout.CheckoutComponents;
import com.nm.commerce.pagedef.definition.PageDefinition;
import com.nm.commerce.pagedef.evaluator.SimplePageEvaluator;
import com.nm.commerce.pagedef.model.PageModel;
import com.nm.commerce.pagedef.model.StorePageModel;
import com.nm.common.StoreAndRestaurantDetails;
import com.nm.components.CommonComponentHelper;
import com.nm.tms.core.TMSMessageContainer;

public class StoreDirectoryPageEvaluator extends SimplePageEvaluator {
  final StoreAndRestUtil storeAndRestUtil = CommonComponentHelper.getStoreAndRestUtil();
  
  @Override
  public boolean evaluate(final PageDefinition pageDefinition, final PageContext pageContext) throws Exception {
    final boolean returnValue = super.evaluate(pageDefinition, pageContext);
    final StorePageModel pageModel = (StorePageModel) getPageModel();
    final StoreAndRestaurantDetails storeDetails = storeAndRestUtil.getStoreDetails();
    final String location = getRequest().getParameter("geoLocation");
    pageModel.setStoreDetails(storeDetails);
    pageModel.setBreadcrumbs(storeAndRestUtil.getBreadcrumbs(pageDefinition, location, null, null, null, null, null));
    pageModel.setPageType(STORES);
    final TMSMessageContainer tmsMessageContainer = CheckoutComponents.getTMSMessageContainer(getRequest());
    CommonComponentHelper.getDataDictionaryUtils().processTMSDataDictionaryAttributes(pageDefinition, null, getProfile(), pageModel, tmsMessageContainer);
    
    return returnValue;
  }
  
  @Override
  protected PageModel allocatePageModel() {
    return new StorePageModel();
  }
  
}
