package com.nm.commerce.pagedef.evaluator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import atg.core.util.StringUtils;
import atg.nucleus.Nucleus;
import atg.repository.Repository;
import atg.repository.RepositoryItem;
import atg.repository.RepositoryView;
import atg.repository.rql.RqlStatement;

import com.nm.catalog.navigation.CategoryHelper;
import com.nm.catalog.navigation.NMCategory;
import com.nm.commerce.pagedef.definition.PageDefinition;
import com.nm.commerce.pagedef.definition.ProductTemplatePageDefinition;
import com.nm.commerce.pagedef.model.TemplatePageModel;

public class BannerUtils {
	
	 public static final String FEATURE_SUBCATS_NAME = "Feature Subcats";
	  public static final String ATTRIBUTE_BANNER_OVERRIDE = "bannerByLocale";
	  public static final String ATTRIBUTE_STATIC_IMAGE_OVERRIDE = "staticImageByLocale";
	  public static final String REST_OF_WORLD = "ROW";
	  private static Repository mCustomCatalogRepository = null;
	  private static Repository mGeographyRepository = null;
	  
	  
	  private static Repository getCustomCatalogRepository() {
		    if (mCustomCatalogRepository == null) {
		      mCustomCatalogRepository = (Repository) Nucleus.getGlobalNucleus().resolveName("/atg/commerce/catalog/ProductCatalog");
		    }
		    
		    return mCustomCatalogRepository;
		  }
		  
		  private static Repository getGeographyRepository() {
		    if (mGeographyRepository == null) {
		      mGeographyRepository = (Repository) Nucleus.getGlobalNucleus().resolveName("/nm/xml/GeographyRepository");
		    }
		    
		    return mGeographyRepository;
		  }
		  
		  // constants
		  private static final String COMMA = ",";
		  
	
   
    
 public static String getBannerCategoryIds(String countryPreference, TemplatePageModel templatePageModel, ProductTemplatePageDefinition productPageDefinition,
		 NMCategory category) {
	    
	    templatePageModel.setUserSelectedCountry(countryPreference);
	    String intlBannerFileName = productPageDefinition.getInternationalFeatureCategoryBannerFile();
	    String categoryId = category.getId();
	    String intlKey = categoryId + ":" + countryPreference + ":" + ATTRIBUTE_BANNER_OVERRIDE;
	    String rowKey = categoryId + ":" + REST_OF_WORLD + ":" + ATTRIBUTE_BANNER_OVERRIDE;
	    String regionKey = null;
	    String regionCode = getRegionCode(countryPreference);
	    RepositoryItem intlRepositoryItem = null;
	    RepositoryItem regionRepositoryItem = null;
	    RepositoryItem rowRepositoryItem = null;
	    String intlOverrideValue = null;
	    String rowOverrideValue = null;
	    String bannerCategoryIds = null;
	    String bannerFileName = productPageDefinition.getFeatureCategoryBannerFile();
	    
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
	        String countrySpecificBannerFileName = fileName[0] + "_" + countryPreference + ".html";
	        templatePageModel.setFeatureCategoryBannerFile(countrySpecificBannerFileName);
	      }
	    }
	    
	    return bannerCategoryIds;
	  }
 // returns string which contains feature banner catIds separated with ","
  
  public static String getFeatureBannerList(NMCategory category) {
    
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
  
  /*
   * This method returns the region code corresponding to the given country code
   */
  private static String getRegionCode(String countryCode) {
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
  
  public static void deriveFeatureBannerCategories(TemplatePageModel pageModel, PageDefinition pageDefinition, CategoryHelper categoryHelper,
		  String advertiseBannersCategoryId) throws IOException {
	    
	    String bannerCategoryIds = advertiseBannersCategoryId;
	    List<NMCategory> featureBannerCategories = new ArrayList<NMCategory>();
	    NMCategory bannerCategory;
	    
	    String[] bannerCatIds = bannerCategoryIds.split(COMMA);
	    for (String bannerCatId : bannerCatIds) {
	      bannerCategory = categoryHelper.getNMCategory(bannerCatId.trim(), null);
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
