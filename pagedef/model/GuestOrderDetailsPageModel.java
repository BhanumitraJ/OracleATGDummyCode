package com.nm.commerce.pagedef.model;

import java.util.HashMap;
import java.util.Map;

import com.nm.order.history.vo.OrderLineItemVO;

public class GuestOrderDetailsPageModel extends PageModel {
  private double tax;
  private double total;
  private double shipping;
  private double amount;
  private double fishAndWildLifeFee;
  private double deliveryAndProcessing;
  private double totalDuties;
  private double oversizeOverweightPrice;
  private String vendorId;
  private String dutyOption;
  private String currencyCode;
  private boolean enableItemCancel;
  private boolean hideRegistration;
  private boolean phoneOrder;
  private final Map<String, OrderLineItemVO> itemDetails = new HashMap<String, OrderLineItemVO>();
  
  public double getAmount() {
    return amount;
  }
  
  public void setAmount(final double amount) {
    this.amount = amount;
  }
  
  public double getTax() {
    return tax;
  }
  
  public void setTax(final double tax) {
    this.tax = tax;
  }
  
  public double getTotal() {
    return total;
  }
  
  public void setTotal(final double total) {
    this.total = total;
  }
  
  public double getShipping() {
    return shipping;
  }
  
  public void setShipping(final double shipping) {
    this.shipping = shipping;
  }
  
  public String getVendorId() {
    return vendorId;
  }
  
  public void setVendorId(final String vendorId) {
    this.vendorId = vendorId;
  }
  
  public double getFishAndWildLifeFee() {
    return fishAndWildLifeFee;
  }
  
  public void setFishAndWildLifeFee(final double fishAndWildLifeFee) {
    this.fishAndWildLifeFee = fishAndWildLifeFee;
  }
  
  public double getDeliveryAndProcessing() {
    return deliveryAndProcessing;
  }
  
  public void setDeliveryAndProcessing(final double deliveryAndProcessing) {
    this.deliveryAndProcessing = deliveryAndProcessing;
  }
  
  public double getTotalDuties() {
    return totalDuties;
  }
  
  public void setTotalDuties(final double totalDuties) {
    this.totalDuties = totalDuties;
  }
  
  public double getOversizeOverweightPrice() {
    return oversizeOverweightPrice;
  }
  
  public void setOversizeOverweightPrice(final double oversizeOverweightPrice) {
    this.oversizeOverweightPrice = oversizeOverweightPrice;
  }
  
  public String getDutyOption() {
    return dutyOption;
  }
  
  public void setDutyOption(final String dutyOption) {
    this.dutyOption = dutyOption;
  }
  
  @Override
  public String getCurrencyCode() {
    return currencyCode;
  }
  
  @Override
  public void setCurrencyCode(final String currencyCode) {
    this.currencyCode = currencyCode;
  }
  
  public boolean isEnableItemCancel() {
    return enableItemCancel;
  }
  
  public void setEnableItemCancel(final boolean enableItemCancel) {
    this.enableItemCancel = enableItemCancel;
  }
  
  public Map<String, OrderLineItemVO> getItemDetails() {
    return itemDetails;
  }
  
  /**
   * @return the hideRegistration
   */
  public boolean isHideRegistration() {
    return hideRegistration;
  }
  
  /**
   * @param hideRegistration
   *          the hideRegistration to set
   */
  public void setHideRegistration(final boolean hideRegistration) {
    this.hideRegistration = hideRegistration;
  }
  
  /**
   * @return the phoneOrder
   */
  public boolean isPhoneOrder() {
    return phoneOrder;
  }
  
  /**
   * @param phoneOrder
   *          the phoneOrder to set
   */
  public void setPhoneOrder(final boolean phoneOrder) {
    this.phoneOrder = phoneOrder;
  }
  
}
