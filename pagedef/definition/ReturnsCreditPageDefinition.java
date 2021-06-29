package com.nm.commerce.pagedef.definition;

/**
 * <p>
 * The class <code>ReturnsCreditPageDefinition</code> is used to define basic page definition <br>
 * like layout, main content area,inner content area, breadcrumb url for Returns Credit Flow.
 * </p>
 * 
 * @author Cognizant
 * 
 */
public class ReturnsCreditPageDefinition extends PageDefinition {
  /** The Constant GET_INNER_CONTENT_PATH. */
  private static final String GET_INNER_CONTENT_PATH = "getInnerContentPath";
  /** The Constant GET_BREADCRUMB_SUBPAGE. */
  private static final String GET_BREADCRUMB_SUBPAGE = "getBreadcrumbSubpage";
  /** The Constant GET_BREADCRUMB_TITLE. */
  private static final String GET_BREADCRUMB_TITLE = "getBreadcrumbTitle";
  /** The Constant GET_BREADCRUMB_URL. */
  private static final String GET_BREADCRUMB_URL = "getBreadcrumbUrl";
  /** The Constant GET_BREADCRUMB_PATH. */
  private static final String GET_BREADCRUMB_PATH = "getBreadcrumbPath";
  /** The Constant GET_MAIN_CONTENT_PATH. */
  private static final String GET_MAIN_CONTENT_PATH = "getMainContentPath";
  /** The breadcrumb url. */
  private String breadcrumbUrl;
  /** The breadcrumb title. */
  private String breadcrumbTitle;
  /** The breadcrumb subpage. */
  private Boolean breadcrumbSubpage;
  /** The breadcrumb path. */
  private String breadcrumbPath;
  /** The main content path. */
  private String mainContentPath;
  /** The inner content path. */
  private String innerContentPath;
  /** The order lookup path. */
  private String orderLookupPath;
  
/**
   * Gets the main content path.
   * 
   * @return the main content path
   */
  public String getMainContentPath() {
    return getValue(mainContentPath, basis, GET_MAIN_CONTENT_PATH);
  }
  
  /**
   * Sets the main content path.
   * 
   * @param mainContentPath
   *          the new main content path
   */
  public void setMainContentPath(String mainContentPath) {
    this.mainContentPath = mainContentPath;
  }
  
  /**
   * Gets the breadcrumb path.
   * 
   * @return the breadcrumb path
   */
  public String getBreadcrumbPath() {
    return getValue(breadcrumbPath, basis, GET_BREADCRUMB_PATH);
  }
  
  /**
   * Sets the breadcrumb path.
   * 
   * @param breadcrumbPath
   *          the new breadcrumb path
   */
  public void setBreadcrumbPath(String breadcrumbPath) {
    this.breadcrumbPath = breadcrumbPath;
  }
  
  /**
   * Gets the breadcrumb url.
   * 
   * @return the breadcrumb url
   */
  public String getBreadcrumbUrl() {
    return getValue(breadcrumbUrl, basis, GET_BREADCRUMB_URL);
  }
  
  /**
   * Sets the breadcrumb url.
   * 
   * @param breadcrumbUrl
   *          the new breadcrumb url
   */
  public void setBreadcrumbUrl(String breadcrumbUrl) {
    this.breadcrumbUrl = breadcrumbUrl;
  }
  
  /**
   * Gets the breadcrumb title.
   * 
   * @return the breadcrumb title
   */
  public String getBreadcrumbTitle() {
    return getValue(breadcrumbTitle, basis, GET_BREADCRUMB_TITLE);
  }
  
  /**
   * Sets the breadcrumb title.
   * 
   * @param breadcrumbTitle
   *          the new breadcrumb title
   */
  public void setBreadcrumbTitle(String breadcrumbTitle) {
    this.breadcrumbTitle = breadcrumbTitle;
  }
  
  /**
   * Gets the breadcrumb subpage.
   * 
   * @return the breadcrumb subpage
   */
  public Boolean getBreadcrumbSubpage() {
    return getValue(breadcrumbSubpage, basis, GET_BREADCRUMB_SUBPAGE);
  }
  
  /**
   * Sets the breadcrumb subpage.
   * 
   * @param breadcrumbSubpage
   *          the new breadcrumb subpage
   */
  public void setBreadcrumbSubpage(Boolean breadcrumbSubpage) {
    this.breadcrumbSubpage = breadcrumbSubpage;
  }
  
  /**
   * Gets the inner content path.
   * 
   * @return the inner content path
   */
  public String getInnerContentPath() {
    return getValue(innerContentPath, basis, GET_INNER_CONTENT_PATH);
  }
  
  public void setInnerContentPath(String innerContentPath) {
    this.innerContentPath = innerContentPath;
  }
  
  /**
   * @return the orderLookupPath
   */
  public String getOrderLookupPath() {
    return orderLookupPath;
  }
  
  /**
   * @param orderLookupPath
   *          the orderLookupPath to set
   */
  public void setOrderLookupPath(String orderLookupPath) {
    this.orderLookupPath = orderLookupPath;
  }
  
}
