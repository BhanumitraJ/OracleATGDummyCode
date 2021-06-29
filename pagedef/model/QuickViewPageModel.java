package com.nm.commerce.pagedef.model;

import java.util.List;
import java.util.Map;

import com.nm.utils.SkuColorData;

public class QuickViewPageModel extends ProductPageModel {
  private String prodPageLink;
  private boolean hasMoreProducts;
  private boolean superSuiteError = false; // value is true when super suite product doesn't have any sub suites
  private Map<String, List<SkuColorData>> productColorSwatchData;
  private String focusProductId;
  
  /** The quick view type. */
  private String quickViewType;

  /**
   * @return the quickViewType
   */
  public String getQuickViewType() {
    return quickViewType;
  }

  /**
   * @param quickViewType the quickViewType to set
   */
  public void setQuickViewType(String quickViewType) {
    this.quickViewType = quickViewType;
  }

  @Override
  public Map<String, List<SkuColorData>> getProductColorSwatchData() {
    return productColorSwatchData;
  }
  
  @Override
  public void setProductColorSwatchData(final Map<String, List<SkuColorData>> productColorSwatchData) {
    this.productColorSwatchData = productColorSwatchData;
  }
  
  public String getFocusProductId() {
    return focusProductId;
  }
  
  public void setFocusProductId(final String focusProductId) {
    this.focusProductId = focusProductId;
  }
  
  public String getProdPageLink() {
    return prodPageLink;
  }

  public void setProdPageLink(final String prodPageLink) {
    this.prodPageLink = prodPageLink;
  }

  public boolean isHasMoreProducts() {
    return hasMoreProducts;
  }

  public void setHasMoreProducts(final boolean hasMoreProducts) {
    this.hasMoreProducts = hasMoreProducts;
  }

  public void setSuperSuiteError(final boolean superSuiteError) {
    this.superSuiteError = superSuiteError;
  }

  public boolean isSuperSuiteError() {
    return superSuiteError;
  }

}
