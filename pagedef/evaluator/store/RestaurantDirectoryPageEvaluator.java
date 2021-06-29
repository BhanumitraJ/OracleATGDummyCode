package com.nm.commerce.pagedef.evaluator.store;

import javax.servlet.jsp.PageContext;

import atg.servlet.DynamoHttpServletRequest;

import com.nm.collections.StoreAndRestUtil;
import com.nm.commerce.pagedef.definition.PageDefinition;
import com.nm.commerce.pagedef.evaluator.SimplePageEvaluator;
import com.nm.commerce.pagedef.model.PageModel;
import com.nm.commerce.pagedef.model.RestaurantPageModel;
import com.nm.common.StoreAndRestaurantDetails;
import com.nm.components.CommonComponentHelper;

public class RestaurantDirectoryPageEvaluator extends SimplePageEvaluator {
  
  final StoreAndRestUtil storeAndRestUtil = CommonComponentHelper.getStoreAndRestUtil();
  
  @Override
  public boolean evaluate(final PageDefinition pageDefinition, final PageContext pageContext) throws Exception {
    final boolean returnValue = super.evaluate(pageDefinition, pageContext);
    final RestaurantPageModel pageModel = (RestaurantPageModel) getPageModel();
    final StoreAndRestaurantDetails restaurantDetails = storeAndRestUtil.getRestaurants();
    final String location = getRequest().getParameter("geoLocation");
    pageModel.setRestaurantDetails(restaurantDetails);
    pageModel.setBreadcrumbs(storeAndRestUtil.getBreadcrumbs(pageDefinition, location, null, null, null, null,null));
    return returnValue;
  }
  
  @Override
  protected PageModel allocatePageModel() {
    return new RestaurantPageModel();
  }
  
}
