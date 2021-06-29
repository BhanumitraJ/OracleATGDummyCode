package com.nm.commerce.checkout.beans;

import com.nm.commerce.beans.BaseResultBean;

public class ResultBean extends BaseResultBean {

  private ResultBeanEventType eventType;
  private double price;
  private String productId;
  private String cmosSkus;
  private String cmosProductId;
  private String cmosItemCode;
  private long quantity;
  private long originalQuantity;
  private ContactInfo billingAddress;
  private String replenishment;
  private boolean addedReplishment;
  private String commerceItemId;
  private String cmosCatalogId;
  private String selectedInterval;

  public void setOriginalQuantity(final long value) {
    originalQuantity = value;
  }

  public long getOriginalQuantity() {
    return originalQuantity;
  }

  public String getCommerceItemId() {
    return commerceItemId;
  }

  public void setCommerceItemId(final String value) {
    commerceItemId = value;
  }

  public boolean isAddedReplishment() {
    return addedReplishment;
  }

  public void setAddedReplishment(final boolean addedReplishment) {
    this.addedReplishment = addedReplishment;
  }

  public String getReplenishment() {
    return replenishment;
  }

  public void setReplenishment(final String replenishment) {
    this.replenishment = replenishment;
  }

  public ResultBeanEventType getEventType() {
    return eventType;
  }

  public void setEventType(final ResultBeanEventType eventType) {
    this.eventType = eventType;
  }

  public double getPrice() {
    return price;
  }

  public void setPrice(final double price) {
    this.price = price;
  }

  public String getProductId() {
    return productId;
  }

  public void setProductId(final String productId) {
    this.productId = productId;
  }

  public String getCmosProductId() {
    return cmosProductId;
  }

  public void setCmosProductId(final String cmosProductId) {
    this.cmosProductId = cmosProductId;
  }

  public String getCmosItemCode() {
    return cmosItemCode;
  }

  public void setCmosItemCode(final String cmosItemCode) {
    this.cmosItemCode = cmosItemCode;
  }

  public long getQuantity() {
    return quantity;
  }

  public void setQuantity(final long quantity) {
    this.quantity = quantity;
  }

  public ContactInfo getBillingAddress() {
    return billingAddress;
  }

  public void setBillingAddress(final ContactInfo billingAddress) {
    this.billingAddress = billingAddress;
  }

  public String getCmosSkus() {
    return cmosSkus;
  }

  public void setCmosSkus(final String cmosSkus) {
    this.cmosSkus = cmosSkus;
  }

  /**
   * @return the cmosCatalogId
   */
  public String getCmosCatalogId() {
    return cmosCatalogId;
  }

  /**
   * @param cmosCatalogId the cmosCatalogId to set
   */
  public void setCmosCatalogId(String cmosCatalogId) {
    this.cmosCatalogId = cmosCatalogId;
  }

  /**
   * @return the selectedInterval
   */
  public String getSelectedInterval() {
    return selectedInterval;
  }

  /**
   * @param selectedInterval the selectedInterval to set
   */
  public void setSelectedInterval(String selectedInterval) {
    this.selectedInterval = selectedInterval;
  }

}
