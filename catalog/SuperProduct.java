package com.nm.commerce.catalog;

import atg.repository.RepositoryItem;

import com.nm.catalog.navigation.NMCategory;
import com.nm.utils.ProdSkuUtil;

/********************************************************
 * @author Elaine Hopkins
 * @since 3/26/2007 This data class accepts a product RepositoryItem and then has getters/setters that are populated via its helper class of SuperViewAllLogic.java. The purpose of this object is to
 *        provide templates with more information than just what is availalbe through the product RI.
 ********************************************************/

public class SuperProduct {
  
  private RepositoryItem productRI = null;
  private String parentCategoryName = "";
  private String headerDisplayName = "";
  private boolean firstProductInCategory = false;
  private boolean lastProductInCategory = false; // added for ShowcaseLogicHelper
  private boolean continuedCategory = false;
  private boolean continueOnNextPage = false;
  private String parentCategoryId = ""; // addition for sitetracker
  private int index = 0;
  private int totalProductsInCategory = 0; // total number of products in the current category. Added for ShowcaseLogicHelper
  private NMCategory parentCategory;
  private NMProduct nmProduct;
  
  /*************************************************************************
   * CONSTRUCTOR
   ************************************************************************/
  public SuperProduct(Object objProduct) {
    RepositoryItem productItem = (RepositoryItem) objProduct;
    this.setProductRI(productItem);
    ProdSkuUtil prodSkuUtil = new ProdSkuUtil();
    NMProduct nmProduct = prodSkuUtil.getProductObject(productItem);
    if (nmProduct != null) {
      this.setNmProduct(nmProduct);
    }
  }
  
  // ****************************************************
  // ATTRIBUTE GETTERS AND SETTERS START HERE
  /*****************************************************
   * @return Returns the productRI
   *****************************************************/
  public RepositoryItem getProductRI() {
    return this.productRI;
  }
  
  /*****************************************************
   * @param productRI
   *          The productRI to set.
   *****************************************************/
  public void setProductRI(RepositoryItem productRI) {
    this.productRI = productRI;
  }
  
  /*****************************************************
   * @return Returns the parentCategoryName
   *****************************************************/
  public String getParentCategoryName() {
    return this.parentCategoryName;
  }
  
  /*****************************************************
   * @param parentCategoryName
   *          The parentCategoryName to set.
   *****************************************************/
  public void setParentCategoryName(String parentCategoryName) {
    if (parentCategoryName != null) {
      this.parentCategoryName = parentCategoryName;
    }
  }
  
  /*****************************************************
   * @return Returns the headerDisplayName
   *****************************************************/
  public String getHeaderDisplayName() {
    return this.headerDisplayName;
  }
  
  /*****************************************************
   * @param headerDisplayName
   *          The headerDisplayName to set.
   *****************************************************/
  public void setHeaderDisplayName(String headerDisplayName) {
    if (headerDisplayName != null) {
      this.headerDisplayName = headerDisplayName;
    }
  }
  
  /*****************************************************
   * @return Returns the firstProductInCategory
   *****************************************************/
  public boolean getFirstProductInCategory() {
    return this.firstProductInCategory;
  }
  
  /*****************************************************
   * @param firstProductInCategory
   *          The firstProductInCategory to set.
   *****************************************************/
  public void setFirstProductInCategory(boolean firstProductInCategory) {
    this.firstProductInCategory = firstProductInCategory;
  }
  
  /*****************************************************
   * @return Returns the continuedCategory
   *****************************************************/
  public boolean getContinuedCategory() {
    return this.continuedCategory;
  }
  
  /*****************************************************
   * @param continuedCategory
   *          The continuedCategory to set.
   *****************************************************/
  public void setContinuedCategory(boolean continuedCategory) {
    this.continuedCategory = continuedCategory;
  }
  
  /*****************************************************
   * @return Returns the continueOnNextPage
   *****************************************************/
  public boolean getContinueOnNextPage() {
    return this.continueOnNextPage;
  }
  
  /*****************************************************
   * @param continueOnNextPage
   *          The continueOnNextPage to set.
   *****************************************************/
  public void setContinueOnNextPage(boolean continueOnNextPage) {
    this.continueOnNextPage = continueOnNextPage;
  }
  
  // addition for sitetracker
  /*****************************************************
   * @return Returns the parentCategoryId
   *****************************************************/
  public String getParentCategoryId() {
    return parentCategoryId;
  }
  
  /*****************************************************
   * @param pParentCategoryId
   *          The parentCategoryId to set
   *****************************************************/
  public void setParentCategoryId(String pParentCategoryId) {
    this.parentCategoryId = pParentCategoryId;
  }
  
  /*****************************************************
   * @return Returns the index
   *****************************************************/
  public int getIndex() {
    return this.index;
  }
  
  /*****************************************************
   * @param index
   *          The index to set.
   *****************************************************/
  public void setIndex(int index) {
    this.index = index;
  }
  
  public void setTotalProductsInCategory(int totalCount) {
    this.totalProductsInCategory = totalCount;
  }
  
  public int getTotalProductsInCategory() {
    return this.totalProductsInCategory;
  }
  
  public void setLastProductInCategory(boolean lastProductInCategory) {
    this.lastProductInCategory = lastProductInCategory;
  }
  
  public boolean isLastProductInCategory() {
    return lastProductInCategory;
  }
  
  public void setParentCategory(NMCategory parentCategory) {
    this.parentCategory = parentCategory;
  }
  
  public NMCategory getParentCategory() {
    return parentCategory;
  }
  
  public NMProduct getNmProduct() {
    return nmProduct;
  }
  
  public void setNmProduct(NMProduct nmProduct) {
    this.nmProduct = nmProduct;
  }
  
}// end class
