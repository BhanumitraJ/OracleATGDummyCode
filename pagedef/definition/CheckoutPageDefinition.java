package com.nm.commerce.pagedef.definition;

public class CheckoutPageDefinition extends PageDefinition {
  private String breadcrumbPath;
  private String innerContentPath;
  private String mainContentPath;
  private String cartSummaryPath;
  private String siloPath;
  
  private String manageAddressPath;
  private String managePaymentPath;
  private String shipToMultiplePath;
  private String orderReviewPath;
  
  private String backOrderMessage;
  private String internationalPreOrBackOrderMessage;
  private String shippingOptionsMessage;
  private String deliveryPhoneMessage;
  private String registerMessage;
  private String orderConfirmation;
  
  private String giftCardButtonText;
  
  private String orderTotalText;
  private Boolean canEditCart;
  
  private Boolean showEstShipDateOnItems;
  
  private String searchBoxText;
  private Boolean searchButtonImage;
  private Boolean showHeaderPromoScroll = false;
  // starts - variable for holding COD message
  private String codMessage;
  
  // third part checkout option separator
  private Boolean showThirdPartyCheckoutSeparator = false;
  
  private String employeeDiscountMessage;
  private String employeeDicountIneligible;
  
  
  public String getEmployeeDicountIneligible() {
	  return getValue(employeeDicountIneligible, basis, "getEmployeeDicountIneligible");
 }

 public void setEmployeeDicountIneligible(String employeeDicountIneligible) {
	this.employeeDicountIneligible = employeeDicountIneligible;
 }

 public String getEmployeeDiscountMessage() {
	  return getValue(employeeDiscountMessage, basis, "getEmployeeDiscountMessage");
 }

  public void setEmployeeDiscountMessage(String employeeDiscountMessage) {
	this.employeeDiscountMessage = employeeDiscountMessage;
  }
  
  
  /**
   * @return the codMessage
   */
  public String getCodMessage() {
    return getValue(codMessage, basis, "getCodMessage");
  }
  
  /**
   * @param codMessage
   *          the codMessage to set
   */
  public void setCodMessage(final String codMessage) {
    this.codMessage = codMessage;
  }
  
  // ENDS - variable for holding COD message
  public String getBreadcrumbPath() {
    return getValue(breadcrumbPath, basis, "getBreadcrumbPath");
  }
  
  public void setBreadcrumbPath(final String breadcrumbPath) {
    this.breadcrumbPath = breadcrumbPath;
  }
  
  public String getInnerContentPath() {
    return getValue(innerContentPath, basis, "getInnerContentPath");
  }
  
  public void setInnerContentPath(final String innerContentPath) {
    this.innerContentPath = innerContentPath;
  }
  
  public String getMainContentPath() {
    return getValue(mainContentPath, basis, "getMainContentPath");
  }
  
  public void setMainContentPath(final String mainContentPath) {
    this.mainContentPath = mainContentPath;
  }
  
  public String getCartSummaryPath() {
    return getValue(cartSummaryPath, basis, "getCartSummaryPath");
  }
  
  public void setCartSummaryPath(final String cartSummaryPath) {
    this.cartSummaryPath = cartSummaryPath;
  }
  
  public String getSiloPath() {
    return siloPath;
  }
  
  public void setSiloPath(final String siloPath) {
    this.siloPath = siloPath;
  }
  
  public String getManageAddressPath() {
    return getValue(manageAddressPath, basis, "getManageAddressPath");
  }
  
  public void setManageAddressPath(final String manageAddressPath) {
    this.manageAddressPath = manageAddressPath;
  }
  
  public String getManagePaymentPath() {
    return getValue(managePaymentPath, basis, "getManagePaymentPath");
  }
  
  public void setManagePaymentPath(final String managePaymentPath) {
    this.managePaymentPath = managePaymentPath;
  }
  
  public String getShipToMultiplePath() {
    return getValue(shipToMultiplePath, basis, "getShipToMultiplePath");
  }
  
  public void setShipToMultiplePath(final String shipToMultiplePath) {
    this.shipToMultiplePath = shipToMultiplePath;
  }
  
  public String getShippingOptionsMessage() {
    return getValue(shippingOptionsMessage, basis, "getShippingOptionsMessage");
  }
  
  public void setShippingOptionsMessage(final String shippingOptionsMessage) {
    this.shippingOptionsMessage = shippingOptionsMessage;
  }
  
  public String getBackOrderMessage() {
    return getValue(backOrderMessage, basis, "getBackOrderMessage");
  }
  
  public void setBackOrderMessage(final String backOrderMessage) {
    this.backOrderMessage = backOrderMessage;
  }
  
  public String getInternationalPreOrBackOrderMessage() {
    return getValue(internationalPreOrBackOrderMessage, basis, "getInternationalPreOrBackOrderMessage");
  }
  
  public void setInternationalPreOrBackOrderMessage(final String internationalPreOrBackOrderMessage) {
    this.internationalPreOrBackOrderMessage = internationalPreOrBackOrderMessage;
  }
  
  public String getDeliveryPhoneMessage() {
    return getValue(deliveryPhoneMessage, basis, "getDeliveryPhoneMessage");
  }
  
  public void setDeliveryPhoneMessage(final String deliveryPhoneMessage) {
    this.deliveryPhoneMessage = deliveryPhoneMessage;
  }
  
  public String getGiftCardButtonText() {
    return getValue(giftCardButtonText, basis, "getGiftCardButtonText");
  }
  
  public void setGiftCardButtonText(final String giftCardButtonText) {
    this.giftCardButtonText = giftCardButtonText;
  }
  
  public String getOrderTotalText() {
    return getValue(orderTotalText, basis, "getOrderTotalText");
  }
  
  public void setOrderTotalText(final String orderTotalText) {
    this.orderTotalText = orderTotalText;
  }
  
  public String getRegisterMessage() {
    return getValue(registerMessage, basis, "getRegisterMessage");
  }
  
  public void setRegisterMessage(final String registerMessage) {
    this.registerMessage = registerMessage;
  }
  
  public String getOrderConfirmation() {
    return getValue(orderConfirmation, basis, "getOrderConfirmation");
  }
  
  public void setOrderConfirmation(final String orderConfirmation) {
    this.orderConfirmation = orderConfirmation;
  }
  
  public Boolean getCanEditCart() {
    return getValue(canEditCart, basis, "getCanEditCart");
  }
  
  public void setCanEditCart(final Boolean canEditCart) {
    this.canEditCart = canEditCart;
  }
  
  public String getOrderReviewPath() {
    return getValue(orderReviewPath, basis, "getOrderReviewPath");
  }
  
  public void setOrderReviewPath(final String orderReviewPath) {
    this.orderReviewPath = orderReviewPath;
  }
  
  public Boolean getShowEstShipDateOnItems() {
    return getValue(showEstShipDateOnItems, basis, "getShowEstShipDateOnItems");
  }
  
  public void setShowEstShipDateOnItems(final Boolean showEstShipDateOnItems) {
    this.showEstShipDateOnItems = showEstShipDateOnItems;
  }
  
  public String getSearchBoxText() {
    return getValue(searchBoxText, basis, "getSearchBoxText");
  }
  
  public void setSearchBoxText(final String searchBoxText) {
    this.searchBoxText = searchBoxText;
  }
  
  public Boolean getSearchButtonImage() {
    return getValue(searchButtonImage, basis, "getSearchButtonImage");
  }
  
  public void setSearchButtonImage(final Boolean searchButtonImage) {
    this.searchButtonImage = searchButtonImage;
  }
  
  public Boolean getShowHeaderPromoScroll() {
    return getValue(showHeaderPromoScroll, basis, "getShowHeaderPromoScroll");
  }
  
  public void setShowHeaderPromoScroll(final Boolean showHeaderPromoScroll) {
    this.showHeaderPromoScroll = showHeaderPromoScroll;
  }
  
  public Boolean getShowThirdPartyCheckoutSeparator() {
    return showThirdPartyCheckoutSeparator;
  }
  
  public void setShowThirdPartyCheckoutSeparator(final Boolean showThirdPartyCheckoutSeparator) {
    this.showThirdPartyCheckoutSeparator = showThirdPartyCheckoutSeparator;
  }
  
}
