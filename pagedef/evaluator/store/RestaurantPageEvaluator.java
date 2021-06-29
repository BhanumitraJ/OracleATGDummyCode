package com.nm.commerce.pagedef.evaluator.store;

import static com.nm.tms.constants.TMSDataDictionaryConstants.STORES;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.jsp.PageContext;

import atg.core.util.StringUtils;
import atg.repository.RepositoryItem;
import atg.servlet.DynamoHttpServletRequest;

import com.nm.collections.StoreAndRestUtil;
import com.nm.collections.StoreExtendedHoursInfo;
import com.nm.commerce.checkout.CheckoutComponents;
import com.nm.commerce.pagedef.definition.PageDefinition;
import com.nm.commerce.pagedef.evaluator.SimplePageEvaluator;
import com.nm.commerce.pagedef.model.PageModel;
import com.nm.commerce.pagedef.model.RestaurantPageModel;
import com.nm.common.StoreAndRestConstants;
import com.nm.components.CommonComponentHelper;
import com.nm.tms.core.TMSMessageContainer;
import com.nm.utils.StringUtilities;

/**
 * Updates the restaurant variables, sets google url for google map image.
 */

public class RestaurantPageEvaluator extends SimplePageEvaluator {
  final StoreAndRestUtil storeAndRestUtil = CommonComponentHelper.getStoreAndRestUtil();
  
  @Override
  public boolean evaluate(final PageDefinition pageDefinition, final PageContext pageContext) throws Exception {
    final boolean returnValue = super.evaluate(pageDefinition, pageContext);
    final DynamoHttpServletRequest request = getRequest();
    
    final String restId = request.getParameter("theRest");
    final String storeId = request.getParameter("storeId");
    final String isRestLocator = request.getParameter("isRestLocator");
    
    final RestaurantPageModel restaurantPageModel = (RestaurantPageModel) getPageModel();
    
    if (storeId != null) {
      final String storeDir = storeId.replaceAll("/", "");
      restaurantPageModel.setStoreDir(storeDir);
      final RepositoryItem storeLocation = storeAndRestUtil.getStoreInfo(storeId);
      if (null != storeLocation) {
        final String directions = (String) storeLocation.getPropertyValue(StoreAndRestConstants.MAP_URL);
        restaurantPageModel.setDirectionsUrl(directions);
        restaurantPageModel.setStoreName((String) storeLocation.getPropertyValue(StoreAndRestConstants.STORE_NAME));
      }
    }
    if (restId != null) {
      final RepositoryItem restInfo = storeAndRestUtil.getRestaurantInfo(restId);
      restaurantPageModel.setRestId(restId);
      if (null != restInfo) {
        getRestInfo(restInfo, restaurantPageModel);
      }
    }
    
    getStoreExtendedHours(restaurantPageModel, storeId);
    final StoreAndRestUtil storeAndRestUtil = CommonComponentHelper.getStoreAndRestUtil();
    restaurantPageModel.setGoogleMapUrl(storeAndRestUtil.formGoogleMapUrl(pageDefinition, storeId, 0, 0, 0));
    if (StringUtils.isEmpty(isRestLocator) || !"true".equalsIgnoreCase(isRestLocator)) {
      final String geoLocation = request.getParameter("geoLocation");
      final String prev = request.getParameter("prev");
      final String view = request.getParameter("view");
      String storeName = restaurantPageModel.getStoreName();
      if (!StringUtils.isEmpty(view) && "fromFav".equalsIgnoreCase(view)) {
        storeName = StoreAndRestConstants.MY_FAVORITE_STORE;
      }
      restaurantPageModel.setBreadcrumbs(storeAndRestUtil.getBreadcrumbs(pageDefinition, geoLocation, storeName, restaurantPageModel.getRestTitle(), storeId, prev, null));
    }
    restaurantPageModel.setPageType(STORES);
    final TMSMessageContainer tmsMessageContainer = CheckoutComponents.getTMSMessageContainer(getRequest());
    CommonComponentHelper.getDataDictionaryUtils().processTMSDataDictionaryAttributes(pageDefinition, null, getProfile(), restaurantPageModel, tmsMessageContainer);
    return returnValue;
    
  }
  
  /**
   * Populates the restaurant's extended-hours property, and sets the display-flag for displaying holiday-hours link
   * 
   * @param restaurantPageModel
   *          The Model object
   * @param storeId
   *          The id for the store
   */
  
  private void getStoreExtendedHours(final RestaurantPageModel restaurantPageModel, final String storeId) {
    LinkedHashMap<String, StoreExtendedHoursInfo> extHoursData = new LinkedHashMap<String, StoreExtendedHoursInfo>();
    final LinkedHashMap<String, String> storeRestMap = new LinkedHashMap<String, String>();
    final StoreAndRestUtil storeAndRestUtil = CommonComponentHelper.getStoreAndRestUtil();
    final Map<String, LinkedHashMap> storeHoursDetails;
    boolean displayFlag = false;
    try {
      if (StringUtilities.isNotEmpty(storeId)) {
        storeHoursDetails = storeAndRestUtil.getStoreExtendedHours(storeId);
        extHoursData = storeHoursDetails.get(StoreAndRestConstants.EXTENDED_HOURS);
        
        if (extHoursData.values() != null && !extHoursData.values().isEmpty()) {
          displayFlag = true;
        }
      }
      restaurantPageModel.setDisplayExtendedHours(displayFlag);
    } catch (final Exception e) {
      if (log.isLoggingError()) {
        log.logError("RestaurantPageEvaluator exception: " + e.getMessage());
      }
    }
  }
  
  /**
   * Populates the restInfo item-descriptor's properties.
   * 
   * @param restaurantPageModel
   *          The Model object
   * @param restInfo
   *          The repository-item restInfo, to get the properties and populate
   */
  
  private void getRestInfo(final RepositoryItem restInfo, final RestaurantPageModel restaurantPageModel) {
    restaurantPageModel.setRestTitle((String) restInfo.getPropertyValue(StoreAndRestConstants.REST_TITLE));
    restaurantPageModel.setDesc((String) restInfo.getPropertyValue(StoreAndRestConstants.RESTAURANT_DESCRIPTION));
    restaurantPageModel.setLongDescription((String) restInfo.getPropertyValue(StoreAndRestConstants.RESTAURANT_LONG_DESCRIPTION));
    final String restaurantHours = (String) restInfo.getPropertyValue(StoreAndRestConstants.DESCRIPTION);
    
    if (null != restaurantHours) {
      restaurantPageModel.setRestaurantHours(storeAndRestUtil.formStoreHoursMap(restaurantHours));
      restaurantPageModel.setSeoHourMap(storeAndRestUtil.formSEOStoreHours(restaurantHours));
    }
    
    restaurantPageModel.setHolidayHours((List<RepositoryItem>) restInfo.getPropertyValue(StoreAndRestConstants.RESTAURANT_HOLIDAY_HOURS));
    restaurantPageModel.setChildInfo((List<RepositoryItem>) restInfo.getPropertyValue(StoreAndRestConstants.RESTAURANT_CHILD_INFO));
    restaurantPageModel.setChildContent((List<RepositoryItem>) restInfo.getPropertyValue(StoreAndRestConstants.RESTAURANT_CHILD_CONTENT));
    restaurantPageModel.setAddress((RepositoryItem) restInfo.getPropertyValue(StoreAndRestConstants.RESTAURANT_ADDRESS));
    restaurantPageModel.setMenu((String) restInfo.getPropertyValue(StoreAndRestConstants.RESTAURANT_MENU));
    restaurantPageModel.setManagerName((String) restInfo.getPropertyValue(StoreAndRestConstants.RESTAURANT_MANAGER_NAME));
    restaurantPageModel.setPrivateEvent((String) restInfo.getPropertyValue(StoreAndRestConstants.RESTAURANT_PRIVATEVENT));
    restaurantPageModel.setReservation((String) restInfo.getPropertyValue(StoreAndRestConstants.RESTAURANT_RESERVATION));
    
  }
  
  @Override
  protected PageModel allocatePageModel() {
    return new RestaurantPageModel();
  }
  
}
