package com.nm.commerce.pagedef.model.omniture;

/**
 * Bean class. Holding user selected filter(s) information, for Omniture parameters in EndecaDrivenCategory page
 * 
 * @author nmjh94
 * 
 */

public class FilterOmniture {
  private String filterCount;
  private String filterTypes;
  private String filterDetails;
  private String totalCount;
  private String pageNumber;
  
  /**
   * number of filters dimension values user applied
   * 
   * @param filterCount
   */
  public void setFilterCount(String filterCount) {
    this.filterCount = filterCount;
  }
  
  public String getFilterCount() {
    return filterCount;
  }
  
  /**
   * filter dimensions, in format of DESIGNER|COLOR|SIZE
   * 
   * @param filterTypes
   */
  public void setFilterTypes(String filterTypes) {
    this.filterTypes = filterTypes;
  }
  
  public String getFilterTypes() {
    return filterTypes;
  }
  
  /**
   * filterDetails format: [dimension name]:[selected dimension value]|[dimension name]:[selected dimension value], e.g.: Color:Black|Color:Red
   * 
   * @param filterDetails
   */
  public void setFilterDetails(String filterDetails) {
    this.filterDetails = filterDetails;
  }
  
  public String getFilterDetails() {
    return filterDetails;
  }
  
  /**
   * total number of resulted search records
   * 
   * @param totalCount
   */
  public void setTotalCount(String totalCount) {
    this.totalCount = totalCount;
  }
  
  public String getTotalCount() {
    return totalCount;
  }
  
  /**
   * requested page
   * 
   * @param pageNumber
   */
  public void setPageNumber(String pageNumber) {
    this.pageNumber = pageNumber;
  }
  
  public String getPageNumber() {
    return pageNumber;
  }
  
}
