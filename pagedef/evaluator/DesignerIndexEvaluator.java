package com.nm.commerce.pagedef.evaluator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.PageContext;

import atg.core.util.StringUtils;
import atg.nucleus.Nucleus;
import atg.repository.Repository;
import atg.repository.RepositoryItem;
import atg.servlet.DynamoHttpServletRequest;

import com.nm.abtest.AbTestHelper;
import com.nm.ajax.myfavorites.utils.MyFavoritesUtil;
import com.nm.authentication.AuthenticationHelper;
import com.nm.catalog.navigation.Breadcrumb;
import com.nm.catalog.navigation.Breadcrumb.BreadCrumbItem;
import com.nm.catalog.navigation.NMCategory;
import com.nm.collections.DesignerIndexer;
import com.nm.collections.DesignerIndexer.Designer;
import com.nm.commerce.pagedef.definition.DesignerIndexPageDefinition;
import com.nm.commerce.pagedef.definition.PageDefinition;
import com.nm.commerce.pagedef.definition.TemplatePageDefinition;
import com.nm.commerce.pagedef.model.DesignerIndexPageModel;
import com.nm.commerce.pagedef.model.PageModel;
import com.nm.commerce.pagedef.model.TemplatePageModel;
import com.nm.components.CommonComponentHelper;
import com.nm.droplet.FilterRedirect;
import com.nm.tms.constants.TMSDataDictionaryConstants;
import com.nm.tms.core.TMSMessageContainer;
import com.nm.tms.utils.TMSDataDictionaryUtils;
import com.nm.utils.NmoUtils;
import com.nm.utils.SeoUrlUtil;
import com.nm.utils.StringUtilities;

public class DesignerIndexEvaluator extends SimplePageEvaluator {
  public static final String ATTRIBUTE_DESIGNER_PROMO_OVERRIDE = "imgAvailableByLocale";
  public static final String REST_OF_WORLD = "ROW";
  private static final String DESIGNER= "Designer";
  private static final String DESIGNER_INDEX ="DesignerIndex";
  private static final String DINDEX="dIndex";
  private static final String DRAWER ="drawer";
  private Repository mCustomCatalogRepository = null;
  
  public boolean evaluate(PageDefinition pageDefinition, PageContext pageContext) throws Exception {
    boolean eval = super.evaluate(pageDefinition, pageContext);
    // redirect if not allowed to access the staging category
    if (requiresRedirect()) {
      return false;
    }
    
    
    NMCategory activeCategory = super.getPageModel().getActiveCategory();
    DesignerIndexPageModel model = (DesignerIndexPageModel) getPageModel();
    DesignerIndexPageDefinition definition = (DesignerIndexPageDefinition) pageDefinition;
    DynamoHttpServletRequest request = getRequest();
    String countryCode = getProfile().getCountryPreference();
    model.setCurrentCountryCode(countryCode);
    boolean indexBySilo = definition.isIndexBySilo();
    String categoryGroup = "";
    String designerIndexVendorColumn = definition.getDesignerIndexVendorColumn();
    Boolean useSiloSpecificPromos = definition.getUseSiloSpecificPromos();
    if (activeCategory != null) {
      ((TemplatePageModel) getPageModel()).setCategory(activeCategory);
      categoryGroup = AbTestHelper.getAbTestValue(request, AbTestHelper.GROUP_NAME_KEY);
      if (categoryGroup != null) {
        getCategoryFilterCode().setFilterCode(categoryGroup);
      }
      activeCategory.setFilterCategoryGroup(getFilterCategoryGroup());
    }
    try {
      DesignerIndexer indexer = DesignerIndexUtils.getDesignerIndexer();
      String rootCatId = indexer.getDesignerIndexRoot();
      String activeCatId = request.getParameter("itemId");
      if (categoryGroup == null || categoryGroup.trim().equals("")) {
        categoryGroup = "#";
      }
      
      categoryGroup = categoryGroup.toUpperCase();
      model.setCategoryGroupCode(categoryGroup);
      model.setRootCategoryId(rootCatId);
      model.setDesignerIndexVendorColumn(designerIndexVendorColumn);
      model.setUseSiloSpecificPromos(useSiloSpecificPromos);
      List<DesignerIndexer.Category> categories = null;
      ArrayList<String> emptyAlphaList = new ArrayList<String>(Arrays.asList(DesignerIndexer.ALPHA_LIST));
      
      if (definition.getUseSeoHyperlinks()) {
        NMCategory category = getNavHistory().getNavHistorySilo();
        String siloName = category.getDisplayName();
        siloName = SeoUrlUtil.buildGenericSeoDisplayName(siloName);
        model.setSeoSiloName(siloName);
        model.setSeoCategoryName(""); // default
        model.setUseSeoHyperlinks(true);
      }
      
      if (indexBySilo) {
        // Filter out the 'By Category' options so that only those related to this silo-specific designerIndex are displayed.
        String selectedCategoryId = request.getParameter("bySiloCat");
        if (StringUtilities.isNotBlank(request.getParameter("dIndex"))) {
        	model.setIsByCategory(false);
        } else if (getBrandSpecs().isAZDesignerSilo() && StringUtilities.isEmpty(selectedCategoryId))  {
        	model.setIsByCategory(definition.getIsByCategory());
        } else {
        	model.setIsByCategory(true);
        }
        
        categories = getFilterCategoryGroup().filterDesignerCategories(DesignerIndexUtils.filterCategoryListBySilo(indexer, categoryGroup, activeCatId));
        DesignerIndexer.Category activeDesignerCategory = DesignerIndexUtils.getActiveDesignerCategory(categories, selectedCategoryId);
        if (activeDesignerCategory != null) {
          model.setActiveDesignerCategory(activeDesignerCategory);
          model.setIndexHasSubCats(activeDesignerCategory.getChildCategories() != null && activeDesignerCategory.getChildCategories().size() > 0);
          if (definition.getUseSeoHyperlinks()) {
            model.setSeoCategoryName(SeoUrlUtil.buildGenericSeoDisplayName(activeDesignerCategory.getDisplayName()));
          }
        }
        Map<String, List<Designer>> filteredAlphaMap = DesignerIndexUtils.filterAlphaMapBySilo(indexer, categoryGroup, activeCatId);
        emptyAlphaList.removeAll(filteredAlphaMap.keySet());
        model.setAlphaDesignerMap(filteredAlphaMap);
      } else {
        DesignerIndexUtils.renderAlphaList(model, indexer, categoryGroup);
        emptyAlphaList.removeAll(model.getAlphaDesignerMap().keySet());
        Set<String> validAlphaKeys = model.getAlphaDesignerMap().keySet();
        for (String alphaKey : validAlphaKeys) {
          List<String> restrictedCountryList = model.getAlphaDesignerCountryRestrictionMap().get(alphaKey);
          if (restrictedCountryList != null && restrictedCountryList.contains(countryCode)) {
            emptyAlphaList.add(alphaKey);
          }
        }
        
        categories = getFilterCategoryGroup().filterDesignerCategories(indexer.getCategoryHierarchyMap().get(categoryGroup));
        model.setIsByCategory(definition.getIsByCategory());
        model.setActiveDesignerCategory(DesignerIndexUtils.getActiveDesignerCategory(categories, activeCatId));
        model.setIndexHasSubCats(indexer.getIndexHasSubCats());
      }
      
      model.setCategoryDesignerMap(indexer.getCategoryDesignerMap().get(categoryGroup));
      model.setEmptyAlphaList(emptyAlphaList);
      model.setDesignerIndexCategories(categories);
      
      model.setBreadcrumb(new Breadcrumb(this.getNavHistory(), definition.getBreadcrumbHideSiloLevel()));
      // setting the designer bread crumb to be used for bread_crumb attribute for TMS 
      if(!NmoUtils.isEmptyCollection(model.getBreadcrumb().getCrumb())){
        List<BreadCrumbItem> breadCrumbItems = model.getBreadcrumb().getCrumb();
        Set<String> breadCrumbValues = new LinkedHashSet<String>();        
        for(BreadCrumbItem breadCrumb : breadCrumbItems){
          breadCrumbValues.add(breadCrumb.getDisplayName());
        } 
        if(DESIGNER.equalsIgnoreCase(request.getParameter(DRAWER)) && !StringUtils.isEmpty(request.getParameter(DINDEX))){
          breadCrumbValues.add(request.getParameter(DINDEX));
        }
        if(model.getActiveDesignerCategory()!=null){
          breadCrumbValues.add(model.getActiveDesignerCategory().getDisplayName());
        }
        model.setDesignerBreadCrumb(breadCrumbValues);
      }
      //ends
      
      String requestParameter = request.getParameter("view");
      
      List<String> favDesigners = null;
      boolean isCachingEnabled = pageDefinition.getEnableCaching();
      
      /** For common favorites page -designer */
      if (null != requestParameter && (requestParameter.equals("favd") || requestParameter.equals("favdhelp")) ) {
        AuthenticationHelper authHelper = (AuthenticationHelper) getRequest().resolveName("/nm/authentication/AuthenticationHelper");
        boolean isAuth = false;
        isAuth = authHelper.sessionHasLoggedInRegisteredUser(getRequest());
        model.setLoginStatus(isAuth);
        favDesigners = MyFavoritesUtil.getInstance().getMyFavoriteItemsList(getProfile(), "200",true);
        
        model.setAlphaSortedFavDesigners(DesignerIndexUtils.filterAlphaMapByFavId(indexer, "#", favDesigners));
        model.setRecommendedDesigners(DesignerIndexUtils.getRecommendedDesigners(indexer,favDesigners));
        
        // disable caching for favorites page
        pageDefinition.setEnableCaching(false);
      } else {
        pageDefinition.setEnableCaching(isCachingEnabled);
        model.setPageName(DESIGNER_INDEX);
      }
      
      /** My Favorites - code tuned to pass fav items list as param to favorites droplet .*/
      if(getBrandSpecs().isEnableMyFavorites()){   
    	  model.setMyFavItems(org.apache.commons.lang.StringUtils.join(MyFavoritesUtil.getInstance().getMyFavoriteItemsList(getProfile(),"200",false), ','));
      }
      
    } catch (NullPointerException e) {
      // this could be caused by DesignerIndexer not ready
    }
    boolean hasInternationalAttribute;
    
    hasInternationalAttribute = setDesignerPromoFileName(definition, model, model.getCategory().getId());
    if (!hasInternationalAttribute && !model.getCategory().getId().equals(model.getRootCategoryId().toString())) {
      setDesignerPromoFileName(definition, model, model.getRootCategoryId());
    }
    model.setPageType(TMSDataDictionaryConstants.MY_NM);
    // Data Dictionary Attributes population.
    final TMSDataDictionaryUtils dataDictionaryUtils = CommonComponentHelper.getDataDictionaryUtils();    
    dataDictionaryUtils.processTMSDataDictionaryAttributes(pageDefinition, null, getProfile(), model, new TMSMessageContainer());
    return eval;
  }
  
  private boolean setDesignerPromoFileName(DesignerIndexPageDefinition definition, DesignerIndexPageModel model, String categoryId) {
    boolean hasInternationalAttribute = true;
    String userSelectedCountry = getProfile().getCountryPreference();
    String designerPromoFileName = definition.getDesignerPromoFile();
    String internationalDesignerPromoFileName = definition.getInternationalDesignerPromoFile();
    String intlKey = categoryId + ":" + userSelectedCountry + ":" + ATTRIBUTE_DESIGNER_PROMO_OVERRIDE;
    String rowKey = categoryId + ":" + REST_OF_WORLD + ":" + ATTRIBUTE_DESIGNER_PROMO_OVERRIDE;
    RepositoryItem intlRepositoryItem = null;
    RepositoryItem rowRepositoryItem = null;
    String intlOverrideValue = null;
    String rowOverrideValue = null;
    
    if (userSelectedCountry != "US") {
      
      try {
        intlRepositoryItem = (RepositoryItem) getCustomCatalogRepository().getItem(intlKey, "categoryAttributeIntlValue");
        if (intlRepositoryItem != null) {
          intlOverrideValue = (String) intlRepositoryItem.getPropertyValue("attributeValue");
        } else {
          rowRepositoryItem = (RepositoryItem) getCustomCatalogRepository().getItem(rowKey, "categoryAttributeIntlValue");
          if (rowRepositoryItem != null) {
            rowOverrideValue = (String) rowRepositoryItem.getPropertyValue("attributeValue");
          }
        }
      } catch (Exception e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      
      if (intlRepositoryItem == null) {
        if (rowRepositoryItem == null) {
          hasInternationalAttribute = false;
          if (categoryId.equals(model.getRootCategoryId())) {
            model.setDesignerPromoFile(designerPromoFileName);
          }
        } else {
          model.setDesignerPromoOverride(true);
          if (rowOverrideValue.equalsIgnoreCase("true")) {
            model.setDesignerPromoFile(internationalDesignerPromoFileName);
          } else {
            model.setDesignerPromoFile("");
          }
          
        }
        model.setDesignerPromoOverride(false);
      } else {
        model.setDesignerPromoOverride(true);
        if (intlOverrideValue.equalsIgnoreCase("true")) {
          String[] fileName = designerPromoFileName.split("\\.");
          String countrySpecificDesignerPromoFileName = fileName[0] + "_" + userSelectedCountry + ".html";
          model.setDesignerPromoFile(countrySpecificDesignerPromoFileName);
        } else {
          model.setDesignerPromoFile("");
        }
      }
    } else {
      model.setDesignerPromoFile(designerPromoFileName);
    }
    
    return hasInternationalAttribute;
  }
  
  protected PageModel allocatePageModel() {
    return new DesignerIndexPageModel();
  }
  
  @Override
  protected PageDefinition derivePageDefinition(PageDefinition pageDefinition, PageModel pageModel) throws Exception {
    TemplatePageModel templatePageModel = (TemplatePageModel) pageModel;
    TemplatePageDefinition templatePageDefinition = (TemplatePageDefinition) pageDefinition;
    templatePageModel.setOriginalPageDefinition(templatePageDefinition);
    
    return templatePageDefinition;
  }
  
  /**
   * Uses the FilterRedirect droplet's evaluations methods to determine if the user has access to the category specified. This should redirect if they have the staging category in their path and they
   * are not authorized.
   * 
   * @return true if the system has redirected
   * @throws IOException
   */
  private boolean requiresRedirect() throws IOException {
    boolean isRedirected = false;
    FilterRedirect filter = (FilterRedirect) getRequest().resolveName("/nm/droplet/FilterRedirect");
    getRequest().setParameter(FilterRedirect.CATEGORY_TO_FILTER, getBrandSpecs().getHiddenCategoryId());
    isRedirected = filter.restrictCategory(getRequest());
    if (isRedirected) {
      getResponse().sendRedirect(getBrandSpecs().getCategoryNotFoundPath());
      getResponse().setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
    }
    return isRedirected;
  }
  
  private Repository getCustomCatalogRepository() {
    if (mCustomCatalogRepository == null) {
      mCustomCatalogRepository = (Repository) Nucleus.getGlobalNucleus().resolveName("/atg/commerce/catalog/ProductCatalog");
    }
    
    return mCustomCatalogRepository;
  }
}
