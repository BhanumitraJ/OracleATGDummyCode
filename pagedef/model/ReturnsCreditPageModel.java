package com.nm.commerce.pagedef.model;

import com.nm.commerce.pagedef.model.bean.Breadcrumb;

/**
 * The Class <code> ReturnsCreditPageModel</code>. Model containing variables to <br>
 * use throughout returns credit flow.
 * 
 * @author Cognizant
 */
public class ReturnsCreditPageModel extends PageModel {
  /** The current breadcrumb. */
  private Breadcrumb currentBreadcrumb;
  /** The breadcrumbs. */
  private Breadcrumb[] breadcrumbs;
  
  /**
   * Gets the breadcrumbs.
   * 
   * @return the breadcrumbs
   */
  public Breadcrumb[] getBreadcrumbs() {
    return breadcrumbs;
  }
  
  /**
   * Sets the breadcrumbs.
   * 
   * @param breadcrumbs
   *          the new breadcrumbs
   */
  public void setBreadcrumbs(Breadcrumb[] breadcrumbs) {
    this.breadcrumbs = breadcrumbs.clone();
  }
  
  /**
   * Gets the current breadcrumb.
   * 
   * @return the current breadcrumb
   */
  public Breadcrumb getCurrentBreadcrumb() {
    return currentBreadcrumb;
  }
  
  /**
   * Sets the current breadcrumb.
   * 
   * @param currentBreadcrumb
   *          the new current breadcrumb
   */
  public void setCurrentBreadcrumb(Breadcrumb currentBreadcrumb) {
    this.currentBreadcrumb = currentBreadcrumb;
  }
  
}
