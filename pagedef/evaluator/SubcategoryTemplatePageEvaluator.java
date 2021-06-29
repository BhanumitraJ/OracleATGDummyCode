package com.nm.commerce.pagedef.evaluator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.jsp.PageContext;

import atg.core.util.StringUtils;
import atg.nucleus.Nucleus;
import atg.nucleus.ServiceMap;
import atg.repository.Repository;
import atg.repository.RepositoryItem;
import atg.repository.RepositoryView;
import atg.repository.rql.RqlStatement;
import atg.servlet.DynamoHttpServletRequest;

import com.nm.catalog.navigation.CategoryHelper;
import com.nm.catalog.navigation.NMCategory;
import com.nm.commerce.catalog.SuperProduct;
import com.nm.commerce.pagedef.definition.PageDefinition;
import com.nm.commerce.pagedef.definition.ProductTemplatePageDefinition;
import com.nm.commerce.pagedef.definition.SubcategoryTemplatePageDefinition;
import com.nm.commerce.pagedef.definition.TemplatePageDefinition;
import com.nm.commerce.pagedef.evaluator.template.SuperViewAllLogicHelper;
import com.nm.commerce.pagedef.model.PageModel;
import com.nm.commerce.pagedef.model.TemplatePageModel;
import com.nm.components.CommonComponentHelper;
import com.nm.formhandler.CatalogFilter;
import com.nm.utils.BrandSpecs;
import com.nm.utils.NMFormHelper;

/**
 * Performs additional checks for valid, public category. Establishes refined template page definition (SC may become SC_3).
 */
public class SubcategoryTemplatePageEvaluator extends TemplatePageEvaluator {
  public static final String FEATURE_SUBCATS_NAME = "Feature Subcats";
  public static final String ATTRIBUTE_BANNER_OVERRIDE = "bannerByLocale";
  public static final String ATTRIBUTE_STATIC_IMAGE_OVERRIDE = "staticImageByLocale";
  public static final String REST_OF_WORLD = "ROW";
  public static final String SC3_EXP_TEMPLATE = "mSC3Exp";
  private Repository mCustomCatalogRepository = null;
  private Repository mGeographyRepository = null;
  
  // constants
  private static final String COMMA = ",";
  
  @Override
  public boolean evaluate(PageDefinition pageDefinition, PageContext pageContext) throws Exception {
    boolean redirect = super.evaluate(pageDefinition, pageContext);
    final TemplatePageModel templatePageModel = (TemplatePageModel) getPageModel();
    final PageDefinition templatePageDefinition = templatePageModel.getPageDefinition();
    BrandSpecs brandSpecs = (BrandSpecs) Nucleus.getGlobalNucleus().resolveName("/nm/utils/BrandSpecs");
    templatePageModel.setPartiallyEvaluated(true);
    /* Process the TMS data dictionary attributes */
    CommonComponentHelper.getTemplateUtils().processTMSDataDictionaryForCategoryPages(templatePageDefinition, templatePageModel, Boolean.FALSE);
    return redirect;
  }
  
  private Repository getCustomCatalogRepository() {
    if (mCustomCatalogRepository == null) {
      mCustomCatalogRepository = (Repository) Nucleus.getGlobalNucleus().resolveName("/atg/commerce/catalog/ProductCatalog");
    }
    
    return mCustomCatalogRepository;
  }
  
  private Repository getGeographyRepository() {
    if (mGeographyRepository == null) {
      mGeographyRepository = (Repository) Nucleus.getGlobalNucleus().resolveName("/nm/xml/GeographyRepository");
    }
    
    return mGeographyRepository;
  }
  
  @Override
  public void evaluateContent(PageContext context) throws Exception {
    super.evaluateContent(context);
    
    TemplatePageModel templatePageModel = (TemplatePageModel) getPageModel();
    PageDefinition templatePageDefinition = templatePageModel.getPageDefinition();
    NMCategory category = templatePageModel.getCategory();
    
    String bannerCategoryIds;
    
    if (templatePageModel.isSuperViewAll()) {
      ProductTemplatePageDefinition firstPageDefinition = (ProductTemplatePageDefinition) templatePageDefinition;
      TemplatePageDefinition subsequentPageDefinition = firstPageDefinition.getAlternatePageDefinition();
      
      // get the displayable product list
      SuperViewAllLogicHelper superViewAllLogic = new SuperViewAllLogicHelper(firstPageDefinition, getRequest());
      List<SuperProduct> superProducts = superViewAllLogic.renderDisplayableProductList(category);
      templatePageModel.setDisplayableSuperProductList(superProducts);
      int totalItemCount = templatePageModel.getDisplayableSuperProductList().size();
      calculatePaging(templatePageModel, firstPageDefinition, subsequentPageDefinition, totalItemCount, getRequest());
    } else {
      SubcategoryTemplatePageDefinition subcategoryPageDefinition = (SubcategoryTemplatePageDefinition) templatePageDefinition;
      calculateThumbnailIteration(templatePageModel, subcategoryPageDefinition, getRequest());
      String bannerFileName = subcategoryPageDefinition.getFeatureCategoryBannerFile();
      // we may need a product list to render a feature item
      if (subcategoryPageDefinition.getFeatureItemCount() > 0) {
        CatalogFilter catalogFilter = (CatalogFilter) getRequest().resolveName("/nm/formhandler/CatalogFilter");
        List<RepositoryItem> products = catalogFilter.initializeCatalogFilter(category, getRequest(), getProfile());
        templatePageModel.setDisplayableProductList(products);
        if (products != null) {
          templatePageModel.setRecordCount(products.size());
        }
      }
      
      templatePageModel.setUserSelectedCountry(getProfile().getCountryPreference());
      
      if (!getProfile().getCountryPreference().equalsIgnoreCase("US")) {
        bannerCategoryIds = getBannerCategoryIds(getProfile().getCountryPreference(), templatePageModel, subcategoryPageDefinition, category);
      }

      else {
        bannerCategoryIds = getFeatureBannerList(category);
        templatePageModel.setFeatureCategoryBannerFile(bannerFileName);
      }
      
      if (subcategoryPageDefinition.getShowFeatureBanners() && !isEmpty(bannerCategoryIds)) {
        deriveFeatureBannerCategories(templatePageModel, subcategoryPageDefinition, bannerCategoryIds);
        
      }
    }
    /* Process the TMS data dictionary attributes */
    CommonComponentHelper.getTemplateUtils().processTMSDataDictionaryForCategoryPages(templatePageDefinition, templatePageModel, true);
  }
  
  // returns string which contains feature banner catIds separated with ","
  
  private String getFeatureBannerList(NMCategory category) {
    
    Map<String, String> featureBannerList = category.getFeatureBannerAttribute();
    StringBuffer bannerCategoryIds = new StringBuffer();
    
    if (featureBannerList != null && !featureBannerList.isEmpty()) {
    	Set<String> featureBannerSet = featureBannerList.keySet();
    	Iterator<String> itr =(Iterator<String>) featureBannerSet.iterator();
    	int attributeCount=0;
    	while(itr.hasNext()) {
    		String attributeKey = itr.next();
    		if(!(featureBannerList.get(attributeKey).equalsIgnoreCase("default"))) {
    			bannerCategoryIds.append(featureBannerList.get(attributeKey));
    				if (featureBannerList.size() > attributeCount) {
    					bannerCategoryIds.append(COMMA);
    					attributeCount++;
    	          }
    		}
    	}
    }
    
    return bannerCategoryIds.toString();
  }
  
  private String getBannerCategoryIds(String countryPreference, TemplatePageModel templatePageModel, SubcategoryTemplatePageDefinition subcategoryPageDefinition, NMCategory category) {
    
    String userSelectedCountry = getProfile().getCountryPreference();
    templatePageModel.setUserSelectedCountry(userSelectedCountry);
    String intlBannerFileName = subcategoryPageDefinition.getInternationalFeatureCategoryBannerFile();
    String categoryId = category.getId();
    String intlKey = categoryId + ":" + userSelectedCountry + ":" + ATTRIBUTE_BANNER_OVERRIDE;
    String rowKey = categoryId + ":" + REST_OF_WORLD + ":" + ATTRIBUTE_BANNER_OVERRIDE;
    String regionKey = null;
    String regionCode = getRegionCode(userSelectedCountry);
    RepositoryItem intlRepositoryItem = null;
    RepositoryItem regionRepositoryItem = null;
    RepositoryItem rowRepositoryItem = null;
    String intlOverrideValue = null;
    String rowOverrideValue = null;
    String bannerCategoryIds = null;
    String bannerFileName = subcategoryPageDefinition.getFeatureCategoryBannerFile();
    
    try {
      intlRepositoryItem = getCustomCatalogRepository().getItem(intlKey, "categoryAttributeIntlValue");
      if (intlRepositoryItem != null) {
        intlOverrideValue = (String) intlRepositoryItem.getPropertyValue("attributeValue");
      } else {
        if (!StringUtils.isEmpty(regionCode)) {
          regionKey = categoryId + ":" + regionCode + ":" + ATTRIBUTE_BANNER_OVERRIDE;
          regionRepositoryItem = getCustomCatalogRepository().getItem(regionKey, "categoryAttributeIntlValue");
        }
        if (regionRepositoryItem != null) {
          intlOverrideValue = (String) regionRepositoryItem.getPropertyValue("attributeValue");
        } else {
          rowRepositoryItem = getCustomCatalogRepository().getItem(rowKey, "categoryAttributeIntlValue");
          if (rowRepositoryItem != null) {
            rowOverrideValue = (String) rowRepositoryItem.getPropertyValue("attributeValue");
          }
        }
      }
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    
    if (intlRepositoryItem == null) {
      if (regionRepositoryItem != null) {
        bannerCategoryIds = getFeatureBannerList(category);
        String[] fileName = bannerFileName.split("\\.");
        String regionSpecificBannerFileName = fileName[0] + "_" + regionCode + ".html";
        templatePageModel.setFeatureCategoryBannerFile(regionSpecificBannerFileName);
        
      } else if (rowRepositoryItem != null) {
        if (rowOverrideValue == "") {
          bannerCategoryIds = "";
        } else {
          bannerCategoryIds = rowOverrideValue;
          templatePageModel.setFeatureCategoryBannerFile(intlBannerFileName);
        }
      } else {
        // bannerCategoryIds = category.getLongDescription();
        bannerCategoryIds = getFeatureBannerList(category);
        templatePageModel.setFeatureCategoryBannerFile(bannerFileName);
        
      }
    } else {
      if (intlOverrideValue == "") {
        bannerCategoryIds = "";
      } else {
        bannerCategoryIds = intlOverrideValue;
        String[] fileName = bannerFileName.split("\\.");
        String countrySpecificBannerFileName = fileName[0] + "_" + userSelectedCountry + ".html";
        templatePageModel.setFeatureCategoryBannerFile(countrySpecificBannerFileName);
      }
    }
    
    return bannerCategoryIds;
  }
  
  /*
   * This method returns the region code corresponding to the given country code
   */
  private String getRegionCode(String countryCode) {
    RqlStatement statement = null;
    RepositoryItem[] countryRgnValues = null;
    String regionCode = "";
    try {
      RepositoryView CountryRgnView = getGeographyRepository().getView("countryRegionMapping");
      if (CountryRgnView != null) {
        statement = RqlStatement.parseRqlStatement("country_cd = ?0");
        Object param[] = new Object[1];
        param[0] = countryCode;
        countryRgnValues = statement.executeQuery(CountryRgnView, param);
        
        if (countryRgnValues != null && countryRgnValues.length > 0) {
          
          for (RepositoryItem item : countryRgnValues) {
            if (item != null) {
              regionCode = (String) item.getPropertyValue("region_code");
            }
          }
        }
      }
    } catch (Exception e) {
      // do nothing , repository error
      e.printStackTrace();
    }
    return regionCode;
  }
  
  @Override
  protected PageDefinition derivePageDefinition(PageDefinition pageDefinition, PageModel pageModel) throws Exception {
    PageDefinition currentPageDefinition = super.derivePageDefinition(pageDefinition, pageModel);
    if (currentPageDefinition == null) {
      return null;
    }
    
    DynamoHttpServletRequest request = this.getRequest();
    TemplatePageModel templatePageModel = (TemplatePageModel) pageModel;
    NMCategory category = templatePageModel.getCategory();
    
    if (category == null) {
      String redirectUrl = NMFormHelper.reviseUrl(getBrandSpecs().getCategoryNotFoundPath());
      getResponse().sendRedirect(redirectUrl);
      return null;
    }
    
    category.setFilterCategoryGroup(getFilterCategoryGroup());
    
    Boolean isSuperAll = false;
    if (isViewAll(SUPER_ALL_VALUE, request)) {
      if (category.getFlgSuperViewAll()) {
        isSuperAll = true;
      } else {
        if (log.isLoggingWarning()) {
          log.logWarning("User attempted superviewall on  " + request.getParameter("itemId"));
        }
      }
    }
    
    SubcategoryTemplatePageDefinition subcategoryPageDefinition = (SubcategoryTemplatePageDefinition) currentPageDefinition;
    
    // we have to evaluate child categories here, in case there is a page substitution.
    // for example: sc becomes sc_4 if there are 4 subcats
    List<NMCategory> childCategories = null;
    NMCategory thumbnailParentCategory = getParentCategory(category, templatePageModel, request, subcategoryPageDefinition);
    if (thumbnailParentCategory != null) {
      thumbnailParentCategory.setFilterCategoryGroup(getFilterCategoryGroup());
      childCategories = thumbnailParentCategory.getOrderedChildCategories(true);
    }
    
    // a substitution map entry might change the page definition
    ServiceMap substitutionMap = subcategoryPageDefinition.getSubstitutionMap();
    String countKey = childCategories == null ? "0" : String.valueOf(childCategories.size());
    SubcategoryTemplatePageDefinition mapDefinition = (SubcategoryTemplatePageDefinition) substitutionMap.get(countKey);
    subcategoryPageDefinition = mapDefinition == null ? subcategoryPageDefinition : mapDefinition;
    
    templatePageModel.setSuperViewAll(Boolean.valueOf(isSuperAll));
    
    if (isSuperAll) {
      currentPageDefinition = subcategoryPageDefinition.getSuperAllPageDefinition();
    } else {
      currentPageDefinition = subcategoryPageDefinition;
      
      int restrictElementCount = subcategoryPageDefinition.getRestrictElementCount();
      if (restrictElementCount == 0 || childCategories == null || childCategories.size() == 0) {
        templatePageModel.setDisplayableSubcatList(new ArrayList<RepositoryItem>());
      } else {
        templatePageModel.setDisplayableSubcatList(childCategories);
      }
    }
    if (getCategory().getTemplateId().equalsIgnoreCase(SC3_EXP_TEMPLATE)) {
      templatePageModel.setSubCatChildCatList(getSubCatForChildCategories(childCategories));
    }
    return currentPageDefinition;
  }
  
  private Map<NMCategory, List<NMCategory>> getSubCatForChildCategories(List<NMCategory> childCategories) {
    Map<NMCategory, List<NMCategory>> subCatChildMap = new LinkedHashMap<NMCategory, List<NMCategory>>();
    if (childCategories != null && !childCategories.isEmpty()) {
      for (int i = 0; i < childCategories.size(); i++) {
        final List<NMCategory> children = childCategories.get(i).getOrderedChildCategories(true);
        if (children != null && !children.isEmpty()) {
          subCatChildMap.put(childCategories.get(i), children);
        } else {
          final List<NMCategory> nochildren = new ArrayList<NMCategory>();
          nochildren.add(childCategories.get(i));
          subCatChildMap.put(childCategories.get(i), nochildren);
        }
      }
    }
    return subCatChildMap;
  }
  
  /*
   * adapted from com.nm.droplet.CategoryAtIndex.java In case child items need to be pulled out from the descendant of the active category
   */
  private NMCategory getParentCategory(NMCategory category, TemplatePageModel model, DynamoHttpServletRequest req, SubcategoryTemplatePageDefinition subcategoryPageDefinition) {
    NMCategory thumbnailParentCategory = category;
    
    // if alternateSaleCategoryId is specified, substitute it only for sale silo category
    String alternateSaleCategoryId = subcategoryPageDefinition.getAlternateSaleCategoryId();
    if (!isEmpty(alternateSaleCategoryId)) {
      BrandSpecs specs = (BrandSpecs) req.resolveName("/nm/utils/BrandSpecs");
      String saleSiloId = specs.getSaleCategoryId();
      if (category.getId().equals(saleSiloId)) {
        NMCategory categoryItem = CategoryHelper.getInstance().getNMCategory(alternateSaleCategoryId);
        if (categoryItem != null) {
          thumbnailParentCategory = categoryItem;
        }
      }
    }
    
    String parentIndex = subcategoryPageDefinition.getThumbnailParentIndex();
    if (!isEmpty(parentIndex)) {
      try {
        // List<NMCategory> children =initializeFilteredCategories(category, model, req);
        List<NMCategory> children = category.getChildCategories(false);
        if (children != null) {
          if (parentIndex.equalsIgnoreCase("last")) {
            thumbnailParentCategory = children.get(children.size() - 1);
          } else if (parentIndex.equalsIgnoreCase("feature")) {
            // if the "Feature Subcats" category does not exist we don't want to show any thumbnails
            thumbnailParentCategory = null;
            boolean foundCat = false;
            for (int i = 0; i < children.size() && foundCat == false; i++) {
              NMCategory subcat = children.get(i);
              // The Feature Subcats is an auto-generated category via button in CM2 so the display name should match
              if (subcat.getDisplayName().equalsIgnoreCase(FEATURE_SUBCATS_NAME)) {
                thumbnailParentCategory = subcat;
                foundCat = true;
              }
            }
            
          } else {
            try {
              Integer index = new Integer(parentIndex);
              if (index < children.size()) {
                thumbnailParentCategory = children.get(index);
              } else {
                thumbnailParentCategory = children.get(children.size() - 1);
              }
              
            } catch (Exception e) {
              thumbnailParentCategory = children.get(0);
            }
          }
        }
      } catch (Exception e) {
        log.error(e.getMessage());
        e.printStackTrace();
      }
    }
    
    if (thumbnailParentCategory != null) {
      String thumbnailParentCategoryId = thumbnailParentCategory.getId();
      String thumbnailMasterCategoryId = thumbnailParentCategory.getParentId();
      model.setThumbnailParentCategoryId(thumbnailParentCategoryId);
      model.setThumbnailMasterCategoryId(thumbnailMasterCategoryId);
    }
    return thumbnailParentCategory;
  }
  
  private void calculateThumbnailIteration(TemplatePageModel pageModel, SubcategoryTemplatePageDefinition firstPage, DynamoHttpServletRequest request) throws IOException {
    
    // determine the elements to display for the current page.
    int pageNumber = 1;
    int firstThumbnailIndex = 1;
    int itemsOnFirstPage = pageModel.getDisplayableSubcatList().size();
    Integer minimum = firstPage.getMinimumNumberCategories();
    int restrictElementCount = firstPage.getRestrictElementCount();
    
    int columnCount = firstPage.getColumnCount();
    boolean isSuperViewAll = pageModel.isSuperViewAll();
    
    if (isSuperViewAll) {
      // itemsOnFirstPage = pageModel.getDisplayableSubcatList().size();
      
    } else {
      if (itemsOnFirstPage < minimum) {
        pageModel.getDisplayableSubcatList().clear();
        
      } else if (restrictElementCount != -1 && restrictElementCount < pageModel.getDisplayableSubcatList().size()) {
        itemsOnFirstPage = restrictElementCount;
      }
      
      NMCategory category = pageModel.getCategory();
      if (!isEmpty(firstPage.getGraphicBlockPath()) && category.getFlgStaticImage()) {
        pageModel.setShowGraphicBlock(true);
      }
    }
    int thumbnailCount = itemsOnFirstPage;
    pageModel.setPageNumber(pageNumber);
    pageModel.setFirstThumbnailIndex(firstThumbnailIndex);
    pageModel.setThumbnailCount(thumbnailCount);
    pageModel.setColumnCount(columnCount);
  }
  
  private void deriveFeatureBannerCategories(TemplatePageModel pageModel, SubcategoryTemplatePageDefinition subcategoryPageDefinition, String advertiseBannersCategoryId) throws IOException {
    
    String bannerCategoryIds = advertiseBannersCategoryId;
    List<NMCategory> featureBannerCategories = new ArrayList<NMCategory>();
    NMCategory bannerCategory;
    
    String[] bannerCatIds = bannerCategoryIds.split(COMMA);
    for (String bannerCatId : bannerCatIds) {
      bannerCategory = getCategoryHelper().getNMCategory(bannerCatId.trim(), null);
      if (bannerCategory != null) {
        if (bannerCategory.getFlgImgAvailable()) {
          featureBannerCategories.add(bannerCategory);
        }
      }
    }
    if (!featureBannerCategories.isEmpty()) {
      pageModel.setDisplayableFeatureBannerCatList(featureBannerCategories);
    }
  }
}
