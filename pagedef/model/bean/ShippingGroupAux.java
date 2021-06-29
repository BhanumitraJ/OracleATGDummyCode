package com.nm.commerce.pagedef.model.bean;

import java.util.List;

import org.apache.commons.lang.StringUtils;

import atg.commerce.order.ShippingGroup;
import atg.commerce.order.ShippingGroupCommerceItemRelationship;

import com.nm.collections.ServiceLevel;
import com.nm.commerce.NMCommerceItem;
import com.nm.commerce.NMRepositoryContactInfo;
import com.nm.utils.StringUtilities;

public class ShippingGroupAux {
  private ShippingGroup shippingGroupRef;
  private String id;
  private String description;
  private NMCommerceItem[] items;
  private int quantity;
  private NMRepositoryContactInfo address;
  private ServiceLevel serviceLevel;
  private ServiceLevel[] validServiceLevels;
  private String estimatedShipDate;
  private String nickname;
  private NMCommerceItem[] deliveryPhoneItems;
  
  public ShippingGroup getShippingGroupRef() {
    return shippingGroupRef;
  }
  
  public void setShippingGroupRef(ShippingGroup shippingGroupRef) {
    this.shippingGroupRef = shippingGroupRef;
  }
  
  public String getId() {
    return id;
  }
  
  public void setId(String id) {
    this.id = id;
  }
  
  public String getDescription() {
    return description;
  }
  
  public void setDescription(String description) {
    this.description = description;
  }
  
  public NMCommerceItem[] getItems() {
    return items;
  }
  
  public void setItems(NMCommerceItem[] items) {
    this.items = items;
  }
  
  public int getQuantity() {
    return quantity;
  }
  
  public void setQuantity(int quantity) {
    this.quantity = quantity;
  }
  
  public NMRepositoryContactInfo getAddress() {
    return address;
  }
  
  public void setAddress(NMRepositoryContactInfo address) {
    this.address = address;
  }
  
  public ServiceLevel getServiceLevel() {
    return serviceLevel;
  }
  
  public void setServiceLevel(ServiceLevel serviceLevel) {
    this.serviceLevel = serviceLevel;
  }
  
  public ServiceLevel[] getValidServiceLevels() {
    return validServiceLevels;
  }
  
  public void setValidServiceLevels(ServiceLevel[] validServiceLevels) {
    this.validServiceLevels = validServiceLevels;
  }
  
  @Override
  public String toString() {
    StringBuilder b = new StringBuilder();
    
    if (getShippingGroupRef() != null) {
      b.append(getShippingGroupRef());
    }
    
    if (getServiceLevel() != null) {
      if (b.length() > 0) {
        b.append("\n\n");
      }
      b.append(getServiceLevel());
    }
    
    String s = b.toString();
    
    return StringUtils.isNotBlank(s) ? s : super.toString();
  }
  
  public String getNickname() {
    return nickname;
  }
  
  public void setNickname(String nickname) {
    this.nickname = nickname;
  }
  
  public NMCommerceItem[] getDeliveryPhoneItems() {
    return deliveryPhoneItems;
  }
  
  public void setDeliveryPhoneItems(NMCommerceItem[] deliveryPhoneItems) {
    this.deliveryPhoneItems = deliveryPhoneItems;
  }
  
  public String getEstimatedShipDate() {
    return estimatedShipDate;
  }
  
  public void setEstimatedShipDate(String estimatedShipDate) {
    this.estimatedShipDate = estimatedShipDate;
  }
  
  public boolean getAllItemsAreShipToStore() {
    List<ShippingGroupCommerceItemRelationship> sgCIRels = getShippingGroupRef().getCommerceItemRelationships();
    for (ShippingGroupCommerceItemRelationship sgCIRel : sgCIRels) {
      NMCommerceItem ci = (NMCommerceItem) sgCIRel.getCommerceItem();
      if (!ci.getShipToStoreFlg()) {
        return false;
      }
    }
    return true;
  }
  
  public boolean getAllItemsArePickupInStore() {
    List<ShippingGroupCommerceItemRelationship> sgCIRels = getShippingGroupRef().getCommerceItemRelationships();
    for (ShippingGroupCommerceItemRelationship sgCIRel : sgCIRels) {
      NMCommerceItem ci = (NMCommerceItem) sgCIRel.getCommerceItem();
      if (ci.getShipToStoreFlg() || StringUtilities.isEmpty(ci.getPickupStoreNo())) {
        return false;
      }
    }
    return true;
  }
  
  /**
   * Determines if the shipping group needs a delivery phone number field when same day delivery is selected
   * on order review by looping through the commerce items in the shipping group to see if there is at least
   * one item that is not pickup in store and not GWP.
   * @return if the shipping group requires a delivery phone number
   */
  public Boolean getRequiresDeliveryPhoneNumberForSdd() {
    Boolean requiresDeliveryPhoneNumberField = false;
    NMCommerceItem[] shippingGroupItems = getItems();
    if (null != shippingGroupItems) {
      for(NMCommerceItem item : shippingGroupItems) {
        if (StringUtilities.isEmpty(item.getPickupStoreNo()) && !item.isGwpItem()) {
          requiresDeliveryPhoneNumberField = true;
        }
      }
    }
    return requiresDeliveryPhoneNumberField;
  }
  /**
   * Determines if the shipping group is same day delivery by looping through the commerce items in the shipping group
   * to see if at least one commerce item is SL0 and the fulfillment facility is not empty
   * @return if the shipping group is same day delivery
   */
  public Boolean getSddShippingGroup() {
    Boolean sddShippingGroup = false;
    NMCommerceItem[] shippingGroupItems = getItems();
    if (null != shippingGroupItems) {
      for (NMCommerceItem item : shippingGroupItems) {
        if ((StringUtilities.isNotEmpty(item.getFulfillmentFacility())) && (ServiceLevel.SL0_SERVICE_LEVEL_TYPE.equals(item.getServicelevel()))) {
          sddShippingGroup = true;
        }
      }
    }
    return sddShippingGroup;
  }
  
}
