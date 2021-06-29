package com.nm.commerce.pagedef.evaluator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.jsp.PageContext;

import atg.core.util.StringUtils;
import atg.servlet.DynamoHttpServletRequest;

import com.nm.catalog.navigation.NMCategory;
import com.nm.catalog.template.TemplateUtils;
import com.nm.commerce.catalog.SuperProduct;
import com.nm.commerce.pagedef.definition.ProductTemplatePageDefinition;
import com.nm.commerce.pagedef.evaluator.template.ShowcaseLogicHelper;
import com.nm.commerce.pagedef.model.TemplatePageModel;
import com.nm.components.CommonComponentHelper;
import com.nm.servlet.NMRequestHelper;

public class SiloProductCarouselEvaluator extends TemplatePageEvaluator {
  
  @Override
  public void evaluateContent(final PageContext context) throws Exception {
    
    final DynamoHttpServletRequest request = this.getRequest();
    final TemplatePageModel templatePageModel = (TemplatePageModel) getPageModel();
    final ProductTemplatePageDefinition productPageDefinition = (ProductTemplatePageDefinition) templatePageModel.getPageDefinition();
    final List<SuperProduct> superProducts = null;
    final ShowcaseLogicHelper showcaseHelper = new ShowcaseLogicHelper();
    final NMCategory category = templatePageModel.getCategory();
    category.setFilterCategoryGroup(getFilterCategoryGroup());
    final TemplateUtils templateUtils = CommonComponentHelper.getTemplateUtils();
    List<NMCategory> carouselSubCats = null;
    final boolean isMobile = NMRequestHelper.isMobileRequest(getRequest());
    if (templateUtils != null) {
      carouselSubCats = templateUtils.getSiloCategories(productPageDefinition.getTemplateId(), isMobile, category.getId(), productPageDefinition.getMaxCarouselInMobile());
    } else {
      carouselSubCats = new ArrayList<NMCategory>();
    }
    
    getBanner(productPageDefinition, templatePageModel, category);
    
    final Map<SuperProduct, List<SuperProduct>> superProdMap = showcaseHelper.getSiloCarouselProdMap(productPageDefinition, request, category, log, carouselSubCats, isMobile, true);
    // Creating map of <SuperProduct List<superProduct>
    // final Map<SuperProduct, List<SuperProduct>> superProdMap = templatePageModel.getSuperProdMap();
    templatePageModel.setSuperProdMap(superProdMap);
    templatePageModel.setRecordCount(superProdMap.size());
    carouselSubCats.clear();
  }
  
  private void getBanner(final ProductTemplatePageDefinition productPageDefinition, final TemplatePageModel templatePageModel, final NMCategory category) throws Exception {
    
    String bannerCategoryIds;
    final String bannerFileName = productPageDefinition.getFeatureCategoryBannerFile();
    
    templatePageModel.setUserSelectedCountry(getProfile().getCountryPreference());
    
    if (!getProfile().getCountryPreference().equalsIgnoreCase("US")) {
      bannerCategoryIds = BannerUtils.getBannerCategoryIds(getProfile().getCountryPreference(), templatePageModel, productPageDefinition, category);
    }
    
    else {
      bannerCategoryIds = BannerUtils.getFeatureBannerList(category);
      if (!StringUtils.isEmpty(bannerFileName)) {
        templatePageModel.setFeatureCategoryBannerFile(bannerFileName);
      }
    }
    
    if (productPageDefinition.getShowFeatureBanners() && !isEmpty(bannerCategoryIds)) {
      BannerUtils.deriveFeatureBannerCategories(templatePageModel, templatePageModel.getPageDefinition(), getCategoryHelper(), bannerCategoryIds);
      
    }
    
  }
  
}
