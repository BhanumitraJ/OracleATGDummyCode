package com.nm.commerce.pagedef.evaluator.store;

import static com.nm.tms.constants.TMSDataDictionaryConstants.STORES;
import static com.nm.tms.constants.TMSDataDictionaryConstants.STORE_LISTING;

import javax.servlet.jsp.PageContext;

import atg.nucleus.Nucleus;

import com.nm.collections.StoreAndRestUtil;
import com.nm.commerce.checkout.CheckoutComponents;
import com.nm.commerce.pagedef.definition.PageDefinition;
import com.nm.commerce.pagedef.definition.StoreAndRestaurantDirectoryPagedefinition;
import com.nm.commerce.pagedef.evaluator.SimplePageEvaluator;
import com.nm.commerce.pagedef.model.PageModel;
import com.nm.commerce.pagedef.model.StoreListingPageModel;
import com.nm.common.StoreAndRestaurantDetails;
import com.nm.components.CommonComponentHelper;
import com.nm.tms.core.TMSMessageContainer;
import com.nm.tms.tools.TMSDataDictionaryRepositoryTools;
import com.nm.tms.utils.TMSDataDictionaryUtils;

public class StoreListingPageEvaluator extends SimplePageEvaluator {
  StoreAndRestUtil storeAndRestUtil = CommonComponentHelper.getStoreAndRestUtil();
  
  @Override
  public boolean evaluate(final PageDefinition pageDefinition, final PageContext pageContext) throws Exception {
    final boolean returnValue = super.evaluate(pageDefinition, pageContext);
    final StoreListingPageModel pageModel = (StoreListingPageModel) getPageModel();
    
    final StoreAndRestaurantDetails bgStoreDetails = storeAndRestUtil.getBgStoreDetails();
    StoreAndRestaurantDirectoryPagedefinition storePagedefinition = (StoreAndRestaurantDirectoryPagedefinition) pageModel.getPageDefinition();
    pageModel.setStoreMap(storePagedefinition.getStoreMapbanner());
    pageModel.setStorePromo(storePagedefinition.getStorePromobanner());
    final TMSDataDictionaryRepositoryTools dataDictionaryRepositoryTools = 
    		(TMSDataDictionaryRepositoryTools)Nucleus.getGlobalNucleus().resolveName("/nm/tms/TMSDataDictionaryRepositoryTools");
    pageModel.setBgStoreDetails(bgStoreDetails);
    pageModel.setPageName(STORE_LISTING);
    pageModel.setPageType(STORES);
    pageModel.setUnsupportedBrowers(dataDictionaryRepositoryTools.getUnsupportedBrowsers());   
    pageModel.setCityAndRegion("New York, NY");
    
    final TMSMessageContainer tmsMessageContainer = CheckoutComponents.getTMSMessageContainer(getRequest());
    final TMSDataDictionaryUtils dataDictionaryUtils = CommonComponentHelper.getDataDictionaryUtils();
   
    dataDictionaryUtils.processTMSDataDictionaryAttributes(pageDefinition,null, getProfile(), pageModel, tmsMessageContainer);
    return returnValue;
  }
  
  @Override
  protected PageModel allocatePageModel() {
    return new StoreListingPageModel();
  }
}
