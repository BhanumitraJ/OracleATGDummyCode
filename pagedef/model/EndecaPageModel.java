package com.nm.commerce.pagedef.model;

import java.util.HashMap;

import com.nm.catalog.navigation.NMCategory;
import com.nm.collections.DesignerIndexer;
import com.nm.common.INMGenericConstants;
import com.nm.interfaces.Debugger;
import com.nm.search.endeca.NMSearchResult;

public class EndecaPageModel extends GenericSearchPageModel implements Debugger {
  
  private String additionalQueryString;
  private NMCategory category;
  private int overlayKey = 0;
  
  private String omnitureexception;
  private String queryStringOutput;
  private String navObject;
  private boolean isError = false;
  private boolean isSaleQuery = false;
  private String templatePath;
  private boolean userConstrainedResults = false;
  private final HashMap<String, NMSearchResult> refinementResultsMap = new HashMap<String, NMSearchResult>();
  private NMCategory endecaCategory;
  private NMSearchResult altResult;
  private String fixedDimId;
  private DesignerIndexer.Designer fixedDesigner;
  
  private String debugInfo;
  private boolean isBREnabled;
  private boolean hasBRResults;
  private boolean bRSCalled;
  private String wFallUrl;
  
  @Override
  public NMCategory getCategory() {
    return category;
  }
  
  @Override
  public void setCategory(final NMCategory category) {
    this.category = category;
  }
  
  public String getAdditionalQueryString() {
    return additionalQueryString;
  }
  
  public void setAdditionalQueryString(final String additionalQueryString) {
    this.additionalQueryString = additionalQueryString;
  }
  
  public int getOverlayKey() {
    overlayKey++;
    return overlayKey;
  }
  
  public String getQueryStringOutput() {
    return queryStringOutput;
  }
  
  public void setQueryStringOutput(final String queryStringOutput) {
    this.queryStringOutput = queryStringOutput;
  }
  
  public HashMap getRefinementResult() {
    return refinementResultsMap;
  }
  
  public void setRefinementResult(final String refinementName, final NMSearchResult refinementResults) {
    this.refinementResultsMap.put(refinementName, refinementResults);
  }
  
  public String getOmnitureException() {
    return omnitureexception;
  }
  
  public void setOmnitureException(final String omnitureexception) {
    this.omnitureexception = omnitureexception;
  }
  
  public String getNavObject() {
    return navObject;
  }
  
  public void setNavObject(final String navObject) {
    this.navObject = navObject;
  }
  
  public boolean isError() {
    return isError;
  }
  
  public void setError(final boolean isError) {
    this.isError = isError;
  }
  
  public String getTemplatePath() {
    return templatePath;
  }
  
  public void setTemplatePath(final String templatePath) {
    this.templatePath = templatePath;
  }
  
  public NMCategory getEndecaCategory() {
    return endecaCategory;
  }
  
  public void setEndecaCategory(final NMCategory endecaCategory) {
    this.endecaCategory = endecaCategory;
  }
  
  public boolean getUserConstrainedResults() {
    return userConstrainedResults;
  }
  
  public void setUserConstrainedResults(final boolean userConstrainedResults) {
    this.userConstrainedResults = userConstrainedResults;
  }
  
  @Override
  public long getRecordCount() {
    long returnValue = 0;
    if (getSearchResult() != null) {
      returnValue = getSearchResult().getTotalRecordCount();
    }
    return returnValue;
  }
  
  public void setAltResult(final NMSearchResult altResult) {
    this.altResult = altResult;
  }
  
  public NMSearchResult getAltResult() {
    return altResult;
  }
  
  public boolean isSaleQuery() {
    return isSaleQuery;
  }
  
  public void setSaleQuery(final boolean isSaleQuery) {
    this.isSaleQuery = isSaleQuery;
  }
  
  @Override
  public String getDebugInfo() {
    return debugInfo;
  }
  
  @Override
  public void setDebugInfo(final String debugInfo) {
    this.debugInfo = debugInfo;
  }
  
  public boolean isBREnabled() {
    return isBREnabled;
  }
  
  public void setBREnabled(final boolean isBREnabled) {
    this.isBREnabled = isBREnabled;
  }
  
  public boolean isHasBRResults() {
    return hasBRResults;
  }
  
  public void setHasBRResults(final boolean hasBRResults) {
    this.hasBRResults = hasBRResults;
  }
  
  public boolean getHasBRResults() {
    return hasBRResults;
  }
  
  public void setBRSCalled(final boolean bRSCalled) {
    this.bRSCalled = bRSCalled;
  }
  
  public boolean getBRSCalled() {
    return bRSCalled;
  }
  
  public String getwFallUrl() {
    return wFallUrl;
  }
  
  public void setwFallUrl(final String wFallUrl) {
    this.wFallUrl = wFallUrl;
  }
  
  public String getFixedDimId() {
    return fixedDimId;
  }

  public void setFixedDimId(String fixedDimId) {
    this.fixedDimId = fixedDimId;
  }

  public DesignerIndexer.Designer getFixedDesigner() {
    return fixedDesigner;
  }

  public void setFixedDesigner(DesignerIndexer.Designer fixedDesigner) {
    this.fixedDesigner = fixedDesigner;
  }

/**
   * Gets the current page number for the endeca search result page.
   * 
   * @return the page numbers
   */
  public int getPageNumber() {
    int pageNumber = INMGenericConstants.ONE;
    if (isPaging()) {
      pageNumber = getPagination().getCurrentPageNumber();
    }
    return pageNumber;
  }
  
}
