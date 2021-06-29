package com.nm.commerce.pagedef.model;

import java.util.List;
import java.util.Map;

import com.nm.ajax.search.beans.FacetDimension;
import com.nm.commerce.catalog.NMProduct;
import com.nm.commerce.pagedef.model.omniture.FilterOmniture;
import com.nm.common.INMGenericConstants;
import com.nm.scout.vo.OmnitureTermMappingVO;
import com.nm.search.endeca.NMDimensionValue;

public class EndecaDrivenPageModel extends EndecaPageModel {
  private String refinedFacetInfoJSON;
  private List<FacetDimension> facetList;
  private String refinements;
  private String requestedPage;
  private FilterOmniture filterOmniture;
  
  // used by EndecaExpandedResultsPageEvaluator, endecaExpandedResults.jsp
  // LC project to display something if there are no/low search results
  private boolean lowResults;
  private boolean nullResults;
  private String nullResultsCopy;
  private boolean showStoreLocatorLink;
  private String searchString;
  private List<OmnitureTermMappingVO> voList;
  private OmnitureTermMappingVO voForExpandedResults;
  private String expandedResultsLinkCopy;
  private String expandedResultsLinkUrl;
  private String navId;
  private String expandedResultsCopy;
  private List<NMProduct> expandedResults;
  private Map<String, List<NMDimensionValue>> saleDesignerListMap;
  private Map<String, List<NMDimensionValue>> categoryDesignerListMap;
  private Boolean isByCategory;
  private List<String> emptyAlphaList;
  private List<String> alphaHeaderList;
  private List<NMDimensionValue> saleDesignerCategories;
  private long saleRecordCount;
  private String designerPromoFile;
  private String seoSiloName;
  private String seoCategoryName;
  private String seoDesignerName;
  private String seoSuffix;
  private boolean perPageViewDisabled;
  private long categoryResults;
  private int pageNum;
  private String viewBy;
  
  /** The sort value. */
  private String sortValue;
  
  public EndecaDrivenPageModel() {}
  
  public String getRefinedFacetInfoJSON() {
    return refinedFacetInfoJSON;
  }
  
  public void setRefinedFacetInfoJSON(final String refinedFacetInfoJSON) {
    this.refinedFacetInfoJSON = refinedFacetInfoJSON;
  }
  
  public void setFacetList(final List<FacetDimension> facetListComplete) {
    this.facetList = facetListComplete;
  }
  
  public List<FacetDimension> getFacetList() {
    return facetList;
  }
  
  public void setRefinements(final String refinements) {
    this.refinements = refinements;
  }
  
  public String getRefinements() {
    return refinements;
  }
  
  public void setRequestedPage(final String requestedPage) {
    this.requestedPage = requestedPage;
  }
  
  public String getRequestedPage() {
    return requestedPage;
  }
  
  public void setFilterOmniture(final FilterOmniture filterOmniture) {
    this.filterOmniture = filterOmniture;
  }
  
  public FilterOmniture getFilterOmniture() {
    return filterOmniture;
  }
  
  public boolean isLowResults() {
    return lowResults;
  }
  
  public void setLowResults(final boolean lowResults) {
    this.lowResults = lowResults;
  }
  
  public boolean isNullResults() {
    return nullResults;
  }
  
  public void setNullResults(final boolean nullResults) {
    this.nullResults = nullResults;
  }
  
  public String getNullResultsCopy() {
    return nullResultsCopy;
  }
  
  public void setNullResultsCopy(final String nullResultsCopy) {
    this.nullResultsCopy = nullResultsCopy;
  }
  
  public boolean isShowStoreLocatorLink() {
    return showStoreLocatorLink;
  }
  
  public void setShowStoreLocatorLink(final boolean showStoreLocatorLink) {
    this.showStoreLocatorLink = showStoreLocatorLink;
  }
  
  public String getSearchString() {
    return searchString;
  }
  
  public void setSearchString(final String searchString) {
    this.searchString = searchString;
  }
  
  public List<OmnitureTermMappingVO> getVoList() {
    return voList;
  }
  
  public void setVoList(final List<OmnitureTermMappingVO> voList) {
    this.voList = voList;
  }
  
  public OmnitureTermMappingVO getVoForExpandedResults() {
    return voForExpandedResults;
  }
  
  public void setVoForExpandedResults(final OmnitureTermMappingVO voForExpandedResults) {
    this.voForExpandedResults = voForExpandedResults;
  }
  
  public String getExpandedResultsLinkCopy() {
    return expandedResultsLinkCopy;
  }
  
  public void setExpandedResultsLinkCopy(final String expandedResultsLinkCopy) {
    this.expandedResultsLinkCopy = expandedResultsLinkCopy;
  }
  
  public String getExpandedResultsLinkUrl() {
    return expandedResultsLinkUrl;
  }
  
  public void setExpandedResultsLinkUrl(final String expandedResultsLinkUrl) {
    this.expandedResultsLinkUrl = expandedResultsLinkUrl;
  }
  
  public String getNavId() {
    return navId;
  }
  
  public void setNavId(final String navId) {
    this.navId = navId;
  }
  
  public String getExpandedResultsCopy() {
    return expandedResultsCopy;
  }
  
  public void setExpandedResultsCopy(final String expandedResultsCopy) {
    this.expandedResultsCopy = expandedResultsCopy;
  }
  
  public List<NMProduct> getExpandedResults() {
    return expandedResults;
  }
  
  public void setExpandedResults(final List<NMProduct> expandedResults) {
    this.expandedResults = expandedResults;
  }
  
  public void setAlphaDesignerListMap(final Map<String, List<NMDimensionValue>> alphaDesignerListMap) {
    this.saleDesignerListMap = alphaDesignerListMap;
  }
  
  public Map<String, List<NMDimensionValue>> getAlphaDesignerListMap() {
    return saleDesignerListMap;
  }
  
  public void setCategoryDesignerListMap(final Map<String, List<NMDimensionValue>> categoryDesignerListMap) {
    this.categoryDesignerListMap = categoryDesignerListMap;
  }
  
  public Map<String, List<NMDimensionValue>> getCategoryDesignerListMap() {
    return categoryDesignerListMap;
  }
  
  public void setIsByCategory(final Boolean isByCategory) {
    this.isByCategory = isByCategory;
  }
  
  public Boolean getIsByCategory() {
    return isByCategory;
  }
  
  public void setAlphaHeaderList(final List<String> alphaHeaderList) {
    this.alphaHeaderList = alphaHeaderList;
  }
  
  public List<String> getAlphaHeaderList() {
    return alphaHeaderList;
  }
  
  public void setEmptyAlphaList(final List<String> emptyAlphaList) {
    this.emptyAlphaList = emptyAlphaList;
  }
  
  public List<String> getEmptyAlphaList() {
    return emptyAlphaList;
  }
  
  public void setSaleDesignerCategories(final List<NMDimensionValue> saleCategories) {
    this.saleDesignerCategories = saleCategories;
  }
  
  public List<NMDimensionValue> getSaleDesignerCategories() {
    return saleDesignerCategories;
  }

  public long getSaleRecordCount() {
    return saleRecordCount;
  }
  
  public void setSaleRecordCount(final long value) {
    saleRecordCount = value;
  }
  
  public String getDesignerPromoFile() {
    return designerPromoFile;
  }
  
  public void setDesignerPromoFile(final String designerPromoFile) {
    this.designerPromoFile = designerPromoFile;
  }
  
  public String getSeoSiloName() {
    return seoSiloName;
  }
  
  public void setSeoSiloName(final String seoSiloName) {
    this.seoSiloName = seoSiloName;
  }
  
  public String getSeoCategoryName() {
    return seoCategoryName;
  }
  
  public void setSeoCategoryName(final String seoCategoryName) {
    this.seoCategoryName = seoCategoryName;
  }
  
  public String getSeoDesignerName() {
    return seoDesignerName;
  }
  
  public void setSeoDesignerName(final String seoDesignerName) {
    this.seoDesignerName = seoDesignerName;
  }
  
  public String getSeoSuffix() {
    return seoSuffix;
  }
  
  public void setSeoSuffix(final String seoSuffix) {
    this.seoSuffix = seoSuffix;
  }
  
  public boolean isPerPageViewDisabled() {
    return perPageViewDisabled;
  }
  
  public void setPerPageViewDisabled(final boolean perPageViewDisabled) {
    this.perPageViewDisabled = perPageViewDisabled;
  }

  /**
   * Gets the sort value.
   * 
   * @return the sort value
   */
  public String getSortValue() {
    return sortValue;
  }

  /**
   * Sets the sort value.
   * 
   * @param sortValue
   *          the new sort value
   */
  public void setSortValue(String sortValue) {
    this.sortValue = sortValue;
  }

	/**
	 * @return the categoryResults
	 */
	public long getCategoryResults() {
		return categoryResults;
	}

	/**
	 * @param categoryResults the categoryResults to set
	 */
	public void setCategoryResults(long categoryResults) {
		this.categoryResults = categoryResults;
	}

	/**
	 * @return the pageNum
	 */
	public int getPageNum() {
		if ((categoryResults > 0 && categoryResults < 30) || ( categoryResults >=30 && !isPaging())) {
			pageNum = INMGenericConstants.ONE;
		}
		return pageNum;
	}

	/**
	 * @param pageNum the pageNum to set
	 */
	public void setPageNum(int pageNum) {
		this.pageNum = pageNum;
	}
	/**
	 * @return the viewBy
	 */
	public String getViewBy() {
		return viewBy;
	}

	/**
	 * @param viewBy the viewBy to set
	 */
	public void setViewBy(final String viewBy) {
		this.viewBy = viewBy;
	}
}
