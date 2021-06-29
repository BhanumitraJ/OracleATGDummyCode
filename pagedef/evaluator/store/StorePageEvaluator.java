package com.nm.commerce.pagedef.evaluator.store;

import static com.nm.tms.constants.TMSDataDictionaryConstants.STORES;
import static com.nm.tms.constants.TMSDataDictionaryConstants.STORES_LANDING;
import static com.nm.tms.constants.TMSDataDictionaryConstants.STORE_TYPE;
import static com.nm.tms.constants.TMSDataDictionaryConstants.STORE;
import static com.nm.tms.constants.TMSDataDictionaryConstants.STANDARD;

import java.util.regex.Pattern;

import javax.servlet.jsp.PageContext;

import atg.core.util.StringUtils;
import atg.repository.RepositoryItem;
import atg.servlet.DynamoHttpServletRequest;

import com.nm.collections.StoreAndRestUtil;
import com.nm.commerce.checkout.CheckoutComponents;
import com.nm.commerce.pagedef.definition.PageDefinition;
import com.nm.commerce.pagedef.definition.StorePageDefinition;
import com.nm.commerce.pagedef.evaluator.SimplePageEvaluator;
import com.nm.commerce.pagedef.model.PageModel;
import com.nm.commerce.pagedef.model.StorePageModel;
import com.nm.commerce.pagedef.model.bean.Breadcrumb;
import com.nm.common.StoreAndRestConstants;
import com.nm.components.CommonComponentHelper;
import com.nm.storeinventory.GeoLocation;
import com.nm.storeinventory.StoreInventoryFacade;
import com.nm.storeinventory.StoreLocation;
import com.nm.tms.core.TMSMessageContainer;
import com.nm.utils.BrandSpecs;
import com.nm.utils.NmoUtils;
import com.nm.utils.StringUtilities;
import com.nm.utils.VendorSpecs;

public class StorePageEvaluator extends SimplePageEvaluator {
  
  private final String DEFAULTLOCATION = "Dallas";
  private final Integer maxRadius = 99999;
  private final Integer interRadius = 500;
  private final Integer minRadius = 100;
  private final double defaultLatitude = 32.82;
  private final double defaultLongitude = -96.73;
  final StoreAndRestUtil storeAndRestUtil = CommonComponentHelper.getStoreAndRestUtil();
  
  public BrandSpecs getBrandspecs() {
    return (BrandSpecs) getRequest().resolveName("/nm/utils/BrandSpecs");
  }
  
  @Override
  public boolean evaluate(final PageDefinition pageDefinition, final PageContext pageContext) throws Exception {
    final boolean returnValue = super.evaluate(pageDefinition, pageContext);
    final StorePageDefinition storePageDefinition = (StorePageDefinition) pageDefinition;
    final DynamoHttpServletRequest request = getRequest();
    
    final String find = request.getParameter("find");
    final String formSubmit = request.getParameter("formSubmit");
    final String storeId = request.getParameter("storeId");
    
    String searchText = "";
    if (!StringUtilities.isNullOrEmpty(find)) {
      final Pattern patt1 = Pattern.compile("^[a-zA-Z][a-zA-Z, ]*$"); // city and state abbreviation
      final Pattern patt2 = Pattern.compile("^\\d{5}$"); // or zip code
      if (patt1.matcher(find).matches() || patt2.matcher(find).matches()) {
        searchText = find;
      }
    }
    
    final StorePageModel storePageModel = (StorePageModel) getPageModel();
    final StoreAndRestUtil storeAndRestUtil = CommonComponentHelper.getStoreAndRestUtil();
    if (StringUtilities.isNullOrEmpty(storeId)) {
      String storeIdList = null;
      
      final String cityAndRegion = storeAndRestUtil.retrieveCityAndRegionFromAkamai(request);
      storePageModel.setCityAndRegion(cityAndRegion);
      storePageModel.setSearchString(searchText);
      storePageModel.setPageName(STORES_LANDING);
      // If no geolocation and if no search is performed, then store
      // search page should be displayed.
      if (StringUtilities.isNullOrEmpty(cityAndRegion) && StringUtilities.isNullOrEmpty(formSubmit) && StringUtilities.isNullOrEmpty(searchText)) {
        pageDefinition.setContentPath(storePageDefinition.getStoresLanding());
      } else {
        if (!StringUtilities.isNullOrEmpty(searchText)) {//
          storeIdList = getStoreInfo(searchText, true);
        } else if (!StringUtilities.isNullOrEmpty(cityAndRegion) && StringUtilities.isNullOrEmpty(formSubmit)) {
          storeIdList = getStoreInfo(cityAndRegion, false);
        } else {
          storeIdList = getStoreInfo(searchText, false);
        }
      }
      if (null != storeIdList) {
        storePageModel.setStoreIds(storeIdList);
        
        storePageModel.setGoogleMapUrl(storeAndRestUtil.formGoogleMapUrl(pageDefinition, storePageModel.getStoreIds(), storePageModel.getLatitude(), storePageModel.getLongitude(),
                storePageModel.getRadius()));
        pageDefinition.setContentPath(storePageDefinition.getStoreResult());
      }
      
    } else {
      if (!StringUtilities.isNullOrEmpty(storeId)) {
        storePageModel.setGoogleMapUrl(storeAndRestUtil.formGoogleMapUrl(pageDefinition, storeId, 0, 0, 0));
        pageDefinition.setContentPath(storePageDefinition.getStorePage());
        final String geoLocation = request.getParameter("geoLocation");
        final String prev = request.getParameter("prev");
        final String fromMap = request.getParameter("fromMap");
        storePageModel.setBreadcrumbs(storeAndRestUtil.getBreadcrumbs(pageDefinition, geoLocation, "", null, null, prev, fromMap));
        request.setParameter("prev", prev);
        storePageModel.setPageName(STORE);
      }
      String storeType ="";
      final RepositoryItem storeLocation = storeAndRestUtil.getStoreInfo(storeId);
      if (null != storeLocation) {
        storePageModel.setStoreName((String) storeLocation.getPropertyValue(StoreAndRestConstants.STORE_NAME));
        storeType = (String)storeLocation.getPropertyValue(STORE_TYPE);
      }
      VendorSpecs vendorSpecs = (VendorSpecs)getRequest().resolveName("/nm/utils/VendorSpecs");
      if(null != storePageModel.getBreadcrumbs()){
      for(Breadcrumb crumb:storePageModel.getBreadcrumbs()){
      	if(null != crumb && !crumb.isClickable() &&  StringUtils.isEmpty(crumb.getTitle())){
      		if(STANDARD.equalsIgnoreCase(storeType)){
      			crumb.setTitle(vendorSpecs.getBrandOnly()+" "+storePageModel.getStoreName());
      		}else{
      			crumb.setTitle(storePageModel.getStoreName());
      		}
      	 }
       }
      }
     }
    storePageModel.setPageType(STORES);
    final TMSMessageContainer tmsMessageContainer = CheckoutComponents.getTMSMessageContainer(getRequest());
    CommonComponentHelper.getDataDictionaryUtils().processTMSDataDictionaryAttributes(pageDefinition, null, getProfile(), storePageModel, tmsMessageContainer);
    
    return returnValue;
  }
  
  @Override
  protected PageModel allocatePageModel() {
    return new StorePageModel();
  }
  
  private StoreInventoryFacade storeInventoryFacade;
  
  public StoreInventoryFacade getStoreInventoryFacade() {
    if (storeInventoryFacade == null) {
      storeInventoryFacade = (StoreInventoryFacade) getRequest().resolveName("/nm/storeinventory/StoreInventoryFacade");
    }
    return storeInventoryFacade;
  }
  
  private String getStoreInfo(final String searchText, final boolean defaultSearch) {
    StoreLocation storeLocation = null;
    String storeIds = null;
    GeoLocation location = null;
    String geoLocation = null;
    final StorePageModel storePageModel = (StorePageModel) getPageModel();
    if (StringUtilities.isNullOrEmpty(searchText)) {
      geoLocation = "Default Location-" + DEFAULTLOCATION;
      storeLocation = getStoreInventoryFacade().findNearestStoresOrderedByDistance(DEFAULTLOCATION, maxRadius, maxResults);
      storePageModel.setRadius(maxRadius);
    } else {
      geoLocation = searchText;
      storeLocation = getStoreInventoryFacade().findNearestStoresOrderedByDistance(searchText, minRadius, maxResults);
      storePageModel.setRadius(minRadius);
      if (null == storeLocation) {
        storeLocation = getStoreInventoryFacade().findNearestStoresOrderedByDistance(searchText, interRadius, maxResults);
        storePageModel.setRadius(interRadius);
        if (null == storeLocation) {
          storeLocation = getStoreInventoryFacade().findNearestStoresOrderedByDistance(searchText, maxRadius, maxResults);
          if ((null == storeLocation) && defaultSearch) {
            geoLocation = "Default Location-" + DEFAULTLOCATION;
            storeLocation = getStoreInventoryFacade().findNearestStoresOrderedByDistance(DEFAULTLOCATION, maxRadius, maxResults);
          }
          storePageModel.setRadius(maxRadius);
        }
      }
    }
    if (null != storeLocation) {
      storeIds = storeLocation.getStoreNumbers();
      location = storeLocation.getLocation();
      if (storeIds != null) {
        final String[] storeArray = storeIds.split(StoreAndRestConstants.SEPARATOR);
        if (null != storeArray) {
          storePageModel.setResultCount(storeArray.length);
          storePageModel.setProp60(geoLocation + ":" + storeArray.length);
        }
      }
    }
    
    if (CommonComponentHelper.getLogger().isLoggingDebug()) {
      CommonComponentHelper.getLogger().logDebug("StorePageEvaluator:getStoreInfo: location-" + geoLocation + " and radius-" + storePageModel.getRadius());
      
    }
    storePageModel.setGeoLocation(searchText);
    if (null != location) {
      storePageModel.setLatitude(location.getLatitude());
      storePageModel.setLongitude(location.getLongitude());
    } else {
      storePageModel.setLatitude(defaultLatitude);
      storePageModel.setLongitude(defaultLongitude);
    }
    
    return storeIds;
    
  }
  

}
