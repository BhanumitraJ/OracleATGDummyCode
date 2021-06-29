package com.nm.commerce.beans;

import java.io.Serializable;

/**
 * This class holds the properties for recently changed commerce item. A reference of it is maintained within order object to capture the recently changed commerce item data to be populated within
 * data dictionary attributes.
 */
public class RecentlyChangedCommerceItem  implements Serializable {
  
	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;

/** This property holds the cart status for changed item e.g {add or remove}. */
  private String cartStatus;
  
  /** This property holds the saveForLater status for changed item e.g {add or remove} */
  private String cartSaveForLaterStatus;
  
  /** This property holds the replenish time for changed item */
  private String cartProductReplenishTime;
  
  /** This property holds the origin for change for changed item. */
  private String cartChangeProductOrigin;
  
  /** This property holds the product id for changed item. */
  private String cartChangeProductId;
  
  /** This property holds the product cmos sku for changed item. */
  private String cartChangeProductCmosSku;
  
  /** This property holds the product quantity in cart for changed item. */
  private Long cartProductQuantity;
  
  /** This property holds the changed property for item. */
  private Long cartChangeProductQuantity;
  
  /** This property holds the change product price for changed item. */
  private Double cartChangeProductPrice;
  
  /** This property holds the product cmos item code for changed item. */
  private String cartChangeProductCmosItem;
  
  /** This property holds the product cmos catalog id for changed item. */
  private String cartChangeProductCmosCatalogId;
  
  /** This property holds the shipping from changed item. */
  private String cartChangeShippingFrom;
  
  /** This property holds the shipping received for changed item. */
  private String cartChangeShippingReceived;
  
  /** property to hold commerceItemId */
  private String commerceItemId;
  
  /** property to hold product name */
  private String productName;
  
  /**
   * @return the cartStatus
   */
  public String getCartStatus() {
    return cartStatus;
  }
  
  /**
   * @param cartStatus
   *          the cartStatus to set
   */
  public void setCartStatus(final String cartStatus) {
    this.cartStatus = cartStatus;
  }
  
  /**
   * @return the cartSaveForLaterStatus
   */
  public String getCartSaveForLaterStatus() {
    return cartSaveForLaterStatus;
  }
  
  /**
   * @param cartSaveForLaterStatus
   *          the cartSaveForLaterStatus to set
   */
  public void setCartSaveForLaterStatus(final String cartSaveForLaterStatus) {
    this.cartSaveForLaterStatus = cartSaveForLaterStatus;
  }
  
  /**
   * @return the cartProductReplenishTime
   */
  public String getCartProductReplenishTime() {
    return cartProductReplenishTime;
  }
  
  /**
   * @param cartProductReplenishTime
   *          the cartProductReplenishTime to set
   */
  public void setCartProductReplenishTime(final String cartProductReplenishTime) {
    this.cartProductReplenishTime = cartProductReplenishTime;
  }
  
  /**
   * @return the cartChangeProductOrigin
   */
  public String getCartChangeProductOrigin() {
    return cartChangeProductOrigin;
  }
  
  /**
   * @param cartChangeProductOrigin
   *          the cartChangeProductOrigin to set
   */
  public void setCartChangeProductOrigin(final String cartChangeProductOrigin) {
    this.cartChangeProductOrigin = cartChangeProductOrigin;
  }
  
  /**
   * @return the cartChangeProductId
   */
  public String getCartChangeProductId() {
    return cartChangeProductId;
  }
  
  /**
   * @param cartChangeProductId
   *          the cartChangeProductId to set
   */
  public void setCartChangeProductId(final String cartChangeProductId) {
    this.cartChangeProductId = cartChangeProductId;
  }
  
  /**
   * @return the cartChangeProductCmosSku
   */
  public String getCartChangeProductCmosSku() {
    return cartChangeProductCmosSku;
  }
  
  /**
   * @param cartChangeProductCmosSku
   *          the cartChangeProductCmosSku to set
   */
  public void setCartChangeProductCmosSku(final String cartChangeProductCmosSku) {
    this.cartChangeProductCmosSku = cartChangeProductCmosSku;
  }
  
  /**
   * @return the cartProductQuantity
   */
  public Long getCartProductQuantity() {
    return cartProductQuantity;
  }
  
  /**
   * @param cartProductQuantity
   *          the cartProductQuantity to set
   */
  public void setCartProductQuantity(final Long cartProductQuantity) {
    this.cartProductQuantity = cartProductQuantity;
  }
  
  /**
   * @return the cartChangeProductPrice
   */
  public Double getCartChangeProductPrice() {
    return cartChangeProductPrice;
  }
  
  /**
   * @param cartChangeProductPrice
   *          the cartChangeProductPrice to set
   */
  public void setCartChangeProductPrice(final Double cartChangeProductPrice) {
    
    this.cartChangeProductPrice = cartChangeProductPrice;
  }
  
  /**
   * @return the cartChangeProductCmosItem
   */
  public String getCartChangeProductCmosItem() {
    return cartChangeProductCmosItem;
  }
  
  /**
   * @param cartChangeProductCmosItem
   *          the cartChangeProductCmosItem to set
   */
  public void setCartChangeProductCmosItem(final String cartChangeProductCmosItem) {
    this.cartChangeProductCmosItem = cartChangeProductCmosItem;
  }
  
  /**
   * @return the cartChangeProductCmosCatalogId
   */
  public String getCartChangeProductCmosCatalogId() {
    return cartChangeProductCmosCatalogId;
  }
  
  /**
   * @param cartChangeProductCmosCatalogId
   *          the cartChangeProductCmosCatalogId to set
   */
  public void setCartChangeProductCmosCatalogId(final String cartChangeProductCmosCatalogId) {
    this.cartChangeProductCmosCatalogId = cartChangeProductCmosCatalogId;
  }
  
  /**
   * @return the cartChangeShippingFrom
   */
  public String getCartChangeShippingFrom() {
    return cartChangeShippingFrom;
  }
  
  /**
   * @param cartChangeShippingFrom
   *          the cartChangeShippingFrom to set
   */
  public void setCartChangeShippingFrom(final String cartChangeShippingFrom) {
    this.cartChangeShippingFrom = cartChangeShippingFrom;
  }
  
  /**
   * @return the cartChangeShippingReceived
   */
  public String getCartChangeShippingReceived() {
    return cartChangeShippingReceived;
  }
  
  /**
   * @param cartChangeShippingReceived
   *          the cartChangeShippingReceived to set
   */
  public void setCartChangeShippingReceived(final String cartChangeShippingReceived) {
    this.cartChangeShippingReceived = cartChangeShippingReceived;
  }
  
  /**
   * @return the cartChangeProductQuantity
   */
  public Long getCartChangeProductQuantity() {
    return cartChangeProductQuantity;
  }
  
  /**
   * @param cartChangeProductQuantity
   *          the cartChangeProductQuantity to set
   */
  public void setCartChangeProductQuantity(final Long cartChangeProductQuantity) {
    this.cartChangeProductQuantity = cartChangeProductQuantity;
  }

  /**
   * @return the commerceItemId
   */
  public String getCommerceItemId() {
	return commerceItemId;
  }

  /**
   * @param commerceItemId the commerceItemId to set
   */
  public void setCommerceItemId(String commerceItemId) {
	this.commerceItemId = commerceItemId;
  }

	/**
	 * @return the productName
	 */
	public String getProductName() {
		return productName;
	}

	/**
	 * @param productName
	 *            the productName to set
	 */
	public void setProductName(String productName) {
		this.productName = productName;
	}
}
