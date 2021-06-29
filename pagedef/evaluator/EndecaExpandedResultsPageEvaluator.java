package com.nm.commerce.pagedef.evaluator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.servlet.jsp.PageContext;

import org.apache.commons.lang.StringUtils;

import atg.nucleus.Nucleus;
import atg.repository.Repository;
import atg.repository.RepositoryException;
import atg.repository.RepositoryItem;
import atg.servlet.DynamoHttpServletRequest;

import com.nm.catalog.navigation.CategoryHelper;
import com.nm.catalog.navigation.NMCategory;
import com.nm.commerce.catalog.NMProduct;
import com.nm.commerce.pagedef.definition.EndecaTemplatePageDefinition;
import com.nm.commerce.pagedef.definition.PageDefinition;
import com.nm.commerce.pagedef.model.EndecaDrivenPageModel;
import com.nm.components.CommonComponentHelper;
import com.nm.scout.vo.OmnitureCategoryMappingVO;
import com.nm.scout.vo.OmnitureLinkMappingVO;
import com.nm.scout.vo.OmnitureRefreshableMappingVO;
import com.nm.scout.vo.OmnitureSearchTermMappingVO;
import com.nm.scout.vo.OmnitureTermMappingVO;
import com.nm.scout.vo.OmnitureTermMappingVO.MappingType;
import com.nm.search.endeca.EndecaQueryHelper;
import com.nm.search.endeca.NMSearchPages;
import com.nm.search.endeca.NMSearchRecord;
import com.nm.search.endeca.NMSearchResult;
import com.nm.search.endeca.NMSearchUtil;
import com.nm.search.endeca.SearchParameters;

/**
 * Updates variables and sets up any redirects before page execution. Returns true if page content should be output.
 */
public class EndecaExpandedResultsPageEvaluator extends EndecaPageEvaluator {
  
  public final static int MINRECS = 1;
  public final static int MAXRECS = 5;
  public final static int PRODUCT_CNT_LOW = 3;
  public final static int PRODUCT_CNT_ZERO = 3;
  public final static String RENDER_MODE_LOW = "low"; // For low expanded results (1-5 products).
  public final static String RENDER_MODE_ZERO = "zero"; // For expanded results (0 products).
  public final static String RENDER_MODE_DEFAULT = "default"; // Same output logic from EndecaQuery.
  public final static String REPLACE_VAR = "_VAR_";
  public final static String NM_SEARCH_PAGES_COMPONENT = "/nm/search/endeca/NMSearchPages";
  public final static String TREE_TAB_KEY = "Your Results";
  public final static String MWS_NULL = "NullResultsMWS";
  public final static String MWS_LOW = "LowResultsMWS";
  public final static String SEE_MORE_NULL = "NullResultsSeeMore";
  public final static String SEE_MORE_LOW = "LowResultsSeeMore";
  
  @SuppressWarnings("unchecked")
  @Override
  public boolean evaluate(PageDefinition pageDefinition, PageContext pageContext) throws Exception {
    // super class will populate the search result, facets, pagination, etc. nmSearchResult is placed in pageModel
    super.evaluate(pageDefinition, pageContext);
    
    // now we look for low/null results, basically all the stuff in the OmnitureMappingsQuery bean originally
    // written for jhtml
    EndecaDrivenPageModel ePageModel = (EndecaDrivenPageModel) getPageModel();
    EndecaTemplatePageDefinition ePageDefinition = (EndecaTemplatePageDefinition) pageDefinition;
    DynamoHttpServletRequest request = getRequest();
    
    SearchParameters searchParameters = new SearchParameters();
    searchParameters.setPageSize(ePageModel.getPageSize());
    searchParameters.setQueryString(ePageModel.getQueryString());
    searchParameters.setAdditionalQueryString(ePageModel.getAdditionalQueryString());
    searchParameters.setDefaultQueryString(ePageDefinition.getDefaultQueryString());
    searchParameters.setExposedDimensions(ePageDefinition.getExposedDimensions());
    searchParameters.setProfileCampaigns((Set<?>) getProfile().getProfileCampaigns());
    searchParameters.setBuildTree(true);
    searchParameters.setItemId(request.getParameter("itemId"));
    searchParameters.setUseNavCatDim(Boolean.parseBoolean(ePageDefinition.getUseNavCatDim()));
    searchParameters.setFilterWeb1(ePageDefinition.getFilterWeb1());
    searchParameters.setExpandWeb1(ePageDefinition.getExpandWeb1());
    searchParameters.setSecondarySorts((String) request.getParameter("secondarySorts"));
    
    EndecaQueryHelper eQuery = (EndecaQueryHelper) request.resolveName("/nm/search/endeca/EndecaQueryHelper");
    EndecaQueryHelper.EndecaQueryOutput output;
    
    // NMSearchResult nmSearchResult = (NMSearchResult) request.getObjectParameter( SEARCH_RESULT );
    NMSearchResult nmSearchResult = ePageModel.getSearchResult();
    
    Boolean useExpandedResults = ePageDefinition.getUseExpandedResults();
    String seeMoreLink = "/search.jsp";
    
    String renderMode = RENDER_MODE_DEFAULT;
    List<RepositoryItem> mappings = new ArrayList<RepositoryItem>();
    boolean showNullResultsCopy = true;
    boolean showStoreLocatorLink = true;
    List<OmnitureTermMappingVO> voList = new ArrayList<OmnitureTermMappingVO>();
    OmnitureTermMappingVO voForExpandedResults = null;
    // At maximum only one of type
    // SEARCH_TERM_TYPE
    // or CATEGORY_TYPE
    // should exist.
    String searchString = getSearchString(ePageModel.getQueryString());
    ePageModel.setSearchString(searchString);
    
    if (useExpandedResults && nmSearchResult != null && searchString != null) {
      try {
        Repository repo = (Repository) CommonComponentHelper.getOmnitureTermMappingsRepository();
        RepositoryItem searchTermRI = (RepositoryItem) repo.getItem(searchString.toUpperCase(), "omnitureLowZeroTerm");
        if (searchTermRI != null) {
          mappings = (List<RepositoryItem>) searchTermRI.getPropertyValue("mappings");
          showNullResultsCopy = (Boolean) searchTermRI.getPropertyValue("showNullResultsCopy");
          showStoreLocatorLink = (Boolean) searchTermRI.getPropertyValue("showStoreLocatorLink");
        }
      } catch (RepositoryException e) {
        e.printStackTrace();
      }
      
      long recordCount = nmSearchResult.getTotalRecordCount();
      if (recordCount == 0) {
        renderMode = RENDER_MODE_ZERO;
      } else if (recordCount >= MINRECS && recordCount <= MAXRECS) {
        renderMode = RENDER_MODE_LOW;
      }
      
      if (mappings.size() > 0) {
        for (RepositoryItem item : mappings) {
          String mappingTypeId = (String) item.getPropertyValue("mappingTypeId");
          switch (MappingType.valueOf(mappingTypeId)) {
            case LINK_TYPE:
              voList.add(createOmnitureLinkMappingVO(item));
            break;
            case REFRESHABLE_TYPE:
              voList.add(createOmnitureRefreshableMappingVO(item));
            break;
            case SEARCH_TERM_TYPE:
              voForExpandedResults = createOmnitureSearchTermMappingVO(item);
            break;
            case CATEGORY_TYPE:
              voForExpandedResults = createOmnitureCategoryMappingVO(item);
            break;
          }
        }
      } else {
        renderMode = RENDER_MODE_DEFAULT;
      }
      
      if (RENDER_MODE_ZERO.equals(renderMode))
        ePageModel.setNullResults(true);
      else if (RENDER_MODE_LOW.equals(renderMode)) ePageModel.setLowResults(true);
    }
    
    if (RENDER_MODE_ZERO.equals(renderMode) && showNullResultsCopy) {
      String nullResultsCopy = ePageDefinition.getNullResultsCopyPrefix().trim().replace(REPLACE_VAR, searchString);
      ePageModel.setNullResultsCopy(nullResultsCopy);
    }
    
    if (RENDER_MODE_ZERO.equals(renderMode)) {
      ePageModel.setShowStoreLocatorLink(showStoreLocatorLink);
    }
    
    if (voList.size() > 0 && !RENDER_MODE_DEFAULT.equals(renderMode)) {
      ePageModel.setVoList(voList);
    }
    
    if (voForExpandedResults != null && !RENDER_MODE_DEFAULT.equals(renderMode)) {
      ePageModel.setVoForExpandedResults(voForExpandedResults);
      String expandedResultsCopy = null;
      List<NMProduct> productList = new ArrayList<NMProduct>();
      int maxProductCnt = RENDER_MODE_ZERO.equals(renderMode) ? PRODUCT_CNT_ZERO : PRODUCT_CNT_LOW;
      String navId = null;
      
      if (voForExpandedResults.getMappingType() == MappingType.CATEGORY_TYPE) {
        
        NMCategory categoryItem = null;
        categoryItem = CategoryHelper.getInstance().getNMCategory(((OmnitureCategoryMappingVO) voForExpandedResults).getCategoryId());
        if (categoryItem != null) {
          productList = getProductList(categoryItem, renderMode, maxProductCnt);
          String displayName = categoryItem.getDatasourceDisplayName(); // (String) categoryItem.getPropertyValue( "displayName" );
          if (displayName != null) {
            expandedResultsCopy = ePageDefinition.getExpandedResultsCopyPrefix().trim().replace(REPLACE_VAR, displayName.toUpperCase());
            navId =
                    RENDER_MODE_LOW.equals(renderMode) ? MWS_LOW + convertStringForMWS(searchString) + convertStringForMWS(displayName) : MWS_NULL + convertStringForMWS(searchString)
                            + convertStringForMWS(displayName);
          }
        }
      } else { // For case where MappingType equals SEARCH_TERM_TYPE
        String searchTerm = ((OmnitureSearchTermMappingVO) voForExpandedResults).getSearchTerm();
        expandedResultsCopy = ePageDefinition.getExpandedResultsCopyPrefix().trim().replace(REPLACE_VAR, searchTerm.toUpperCase());
        replaceSearchTerm(searchParameters, request, ePageModel.getQueryString(), searchTerm);
        output = eQuery.query(searchParameters);
        if (!output.isError()) {
          NMSearchResult searchResults = output.getSearchResult();
          long recordCount = searchResults.getTotalRecordCount();
          if (searchResults != null && recordCount > 0) {
            List<NMSearchRecord> records = searchResults.getSearchRecordList();
            for (NMSearchRecord rec : records) {
              productList.add(rec.getProduct());
              if (productList.size() >= maxProductCnt) {
                long leftOver = recordCount - maxProductCnt;
                if (leftOver > 0) {
                  ePageModel.setExpandedResultsLinkCopy(ePageDefinition.getExpandedResultsLinkCopyPrefix().trim().replace(REPLACE_VAR, searchTerm));
                  String queryString = output.getQueryString(); // request.getParameter("queryString");
                  queryString = NMSearchUtil.changeParameterValueDecoded(queryString, "treeTab", "");
                  queryString = NMSearchUtil.changeParameterValueDecoded(queryString, "navid", "");
                  ePageModel.setExpandedResultsLinkUrl(seeMoreLink + "?" + queryString + "&navid=" + (RENDER_MODE_ZERO.equals(renderMode) ? SEE_MORE_NULL : SEE_MORE_LOW)
                          + convertStringForMWS(searchString) + convertStringForMWS(searchTerm));
                }
                break;
              }
            }
            
            navId =
                    RENDER_MODE_LOW.equals(renderMode) ? MWS_LOW + convertStringForMWS(searchString) + convertStringForMWS(searchTerm) : MWS_NULL + convertStringForMWS(searchString)
                            + convertStringForMWS(searchTerm);
          }
        }
      }
      
      if (productList.size() > 0) {
        ePageModel.setNavId(navId);
        ePageModel.setExpandedResultsCopy(expandedResultsCopy);
        ePageModel.setExpandedResults(productList);
      }
    }
    
    return true;
  }
  
  private List<NMProduct> getProductList(RepositoryItem categoryItem, String renderMode, int maxProducts) {
    ArrayList<NMProduct> productResultsList = new ArrayList<NMProduct>();
    
    if (categoryItem != null) {
      @SuppressWarnings("unchecked")
      List<RepositoryItem> childProdList = (List<RepositoryItem>) categoryItem.getPropertyValue("childProducts");
      
      if (childProdList != null && childProdList.size() > 0) {
        for (RepositoryItem productRepositoryItem : childProdList) {
          if (((Boolean) productRepositoryItem.getPropertyValue("flgDisplay")).booleanValue()) {
            productResultsList.add(new NMProduct(productRepositoryItem));
          }
          if (productResultsList.size() >= maxProducts) break;
        }
      }
    }
    
    return productResultsList;
  }
  
  private void replaceSearchTerm(SearchParameters searchParameters, DynamoHttpServletRequest request, String queryString, String newSearchTerm) {
    NMSearchPages nmSearchPages = (NMSearchPages) Nucleus.getGlobalNucleus().resolveName(NM_SEARCH_PAGES_COMPONENT);
    Properties queryStringMap = nmSearchPages.getQueryStringMap();
    String newQueryString = NMSearchUtil.changeParameterValueDecoded(queryString, "Ntt", newSearchTerm);
    newQueryString = NMSearchUtil.changeParameterValueDecoded(newQueryString, "N", "0");
    searchParameters.setAdditionalQueryString((String) queryStringMap.get(TREE_TAB_KEY));
    searchParameters.setQueryString(newQueryString);
  }
  
  private String getSearchString(String queryStringInput) {
    Map<String, Object> queryStringParameterMap = NMSearchUtil.getParameterMap(queryStringInput);
    return (String) queryStringParameterMap.get("Ntt");
  }
  
  private OmnitureRefreshableMappingVO createOmnitureRefreshableMappingVO(RepositoryItem item) {
    OmnitureRefreshableMappingVO vo = new OmnitureRefreshableMappingVO();
    vo.setRefreshablePath((String) item.getPropertyValue("refreshablePath"));
    return vo;
  }
  
  private OmnitureLinkMappingVO createOmnitureLinkMappingVO(RepositoryItem item) {
    OmnitureLinkMappingVO vo = new OmnitureLinkMappingVO();
    vo.setUrl((String) item.getPropertyValue("url"));
    vo.setUrlText((String) item.getPropertyValue("urlText"));
    return vo;
  }
  
  private OmnitureCategoryMappingVO createOmnitureCategoryMappingVO(RepositoryItem item) {
    OmnitureCategoryMappingVO vo = new OmnitureCategoryMappingVO();
    vo.setCategoryId((String) item.getPropertyValue("categoryId"));
    return vo;
  }
  
  private OmnitureSearchTermMappingVO createOmnitureSearchTermMappingVO(RepositoryItem item) {
    OmnitureSearchTermMappingVO vo = new OmnitureSearchTermMappingVO();
    vo.setSearchTerm((String) item.getPropertyValue("searchTermRedirect"));
    return vo;
  }
  
  private String convertStringForMWS(String inputString) {
    StringBuffer sb = new StringBuffer();
    
    String[] tokens = StringUtils.split(inputString, " ");
    if (tokens.length > 0) {
      for (String token : tokens) {
        String newToken = StringUtils.capitalize(token.toLowerCase());
        sb.append(newToken);
      }
    } else {
      sb.append(StringUtils.capitalize(inputString.toLowerCase()));
    }
    
    return sb.toString();
  }
  
} // End of Class
