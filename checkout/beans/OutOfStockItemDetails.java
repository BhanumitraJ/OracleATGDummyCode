package com.nm.commerce.checkout.beans;

import java.util.Map;
import atg.repository.RepositoryItem;

public class OutOfStockItemDetails {
  private RepositoryItem dataSource;
  private String productId;
  private String color;
  private String size;
  private long qty;
  private String suiteId;
  private String webDesignerName;
  private String cmosCatalogId;
  private String cmosItemCode;
  private String displayName;
  private String skuId;
  /** The configuration set key. */
  private String configurationSetKey;
  /** The option choices. */
  private Map<String,String> optionChoices;
	/** The dynamic image url. */
	private String dynamicImageUrl;
  /**
	 * @return the optionChoices
	 */
	public Map<String, String> getOptionChoices() {
		return optionChoices;
	}

	/**
	 * @param optionChoices the optionChoices to set
	 */
	public void setOptionChoices(Map<String, String> optionChoices) {
		this.optionChoices = optionChoices;
	}

  public String getProductId() {
    return productId;
  }
  
  public void setProductId(final String productId) {
    this.productId = productId;
  }
  
  public String getColor() {
    return color;
  }
  
  public void setColor(final String color) {
    this.color = color;
  }
  
  public String getSize() {
    return size;
  }
  
  public void setSize(final String size) {
    this.size = size;
  }
  
  public long getQty() {
    return qty;
  }
  
  public void setQty(final long qty) {
    this.qty = qty;
  }
  
  public String getSuiteId() {
    return suiteId;
  }
  
  public void setSuiteId(final String suiteId) {
    this.suiteId = suiteId;
  }
  
  public String getCmosCatalogId() {
    return cmosCatalogId;
  }
  
  public void setCmosCatalogId(final String cmosCatalogId) {
    this.cmosCatalogId = cmosCatalogId;
  }
  
  public String getCmosItemCode() {
    return cmosItemCode;
  }
  
  public void setCmosItemCode(final String cmosItemCode) {
    this.cmosItemCode = cmosItemCode;
  }
  
  public String getDisplayName() {
    return displayName;
  }
  
  public void setDisplayName(final String displayName) {
    this.displayName = displayName;
  }
  
  /**
   * @return the skuId
   */
  public String getSkuId() {
    return skuId;
  }
  
  /**
   * @param skuId
   *          the skuId to set
   */
  public void setSkuId(final String skuId) {
    this.skuId = skuId;
  }
  
  public RepositoryItem getDataSource() {
    return dataSource;
  }
  
  public void setDataSource(final RepositoryItem dataSource) {
    this.dataSource = dataSource;
  }
  
  public String getWebDesignerName() {
    return webDesignerName;
  }
  
  public void setWebDesignerName(final String webDesignerName) {
    this.webDesignerName = webDesignerName;
  }

	/**
	 * @return the dynamicImageUrl
	 */
	public String getDynamicImageUrl() {
		return dynamicImageUrl;
	}

	/**
	 * @param dynamicImageUrl the dynamicImageUrl to set
	 */
	public void setDynamicImageUrl(String dynamicImageUrl) {
		this.dynamicImageUrl = dynamicImageUrl;
	}

	/**
	 * @return the configurationSetKey
	 */
	public String getConfigurationSetKey() {
		return configurationSetKey;
	}

	/**
	 * @param configurationSetKey the configurationSetKey to set
	 */
	public void setConfigurationSetKey(String configurationSetKey) {
		this.configurationSetKey = configurationSetKey;
	}
}
