package com.nm.commerce.pagedef.model;

import com.nm.search.endeca.NMSearchResult;

/**
 * This model contains all the variables which are common between both Endeca and BR pages.
 * 
 * @author Cognizant
 * 
 */
public class GenericSearchPageModel extends TemplatePageModel {
  
  /** The query string. */
  private String queryString;
  /** The from param. */
  private String fromParam;
  /** The search term. */
  private String searchTerm;
  /** The search result. */
  private NMSearchResult searchResult;
  /** The zero dim record count. */
  private long zeroDimRecordCount;
  /** The BR result status. */
  private String brResultStatus;
  
  /** The internal Search Term. */
  private String internalSearchTerm;
  
  /**
   * Gets the br result status.
   * 
   * @return the br result status
   */
  public String getBrResultStatus() {
    return brResultStatus;
  }
  
  /**
   * Sets the br result status.
   * 
   * @param brResultStatus
   *          the new br result status
   */
  public void setBrResultStatus(final String brResultStatus) {
    this.brResultStatus = brResultStatus;
  }
  
  /**
   * @return the zeroDimRecordCount
   */
  public long getZeroDimRecordCount() {
    return zeroDimRecordCount;
  }
  
  /**
   * @param zeroDimRecordCount
   *          the zeroDimRecordCount to set
   */
  public void setZeroDimRecordCount(final long zeroDimRecordCount) {
    this.zeroDimRecordCount = zeroDimRecordCount;
  }
  
  /**
   * @return fromParam
   */
  public String getFromParam() {
    return fromParam;
  }
  
  /**
   * @param fromParam
   */
  public void setFromParam(final String fromParam) {
    this.fromParam = fromParam;
  }
  
  /**
   * @return queryString
   */
  public String getQueryString() {
    return queryString;
  }
  
  /**
   * @param queryString
   */
  public void setQueryString(final String queryString) {
    this.queryString = queryString;
  }
  
  /**
   * @return searchResult
   */
  public NMSearchResult getSearchResult() {
    return this.searchResult;
  }
  
  /**
   * @param searchResult
   */
  public void setSearchResult(final NMSearchResult searchResult) {
    this.searchResult = searchResult;
  }
  
  /**
   * @return the internalSearchTerm
   */
  public String getInternalSearchTerm() {
    return internalSearchTerm;
  }
  
  /**
   * @param internalSearchTerm
   *          the internalSearchTerm to set
   */
  public void setInternalSearchTerm(final String internalSearchTerm) {
    this.internalSearchTerm = internalSearchTerm;
  }
  
}
