package com.nm.commerce.checkout;

import static com.nm.estimateddeliverydate.EstimatedDeliveryDateConstants.ORDER_REVIEW_TEXT;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

import atg.commerce.CommerceException;
import atg.commerce.order.CommerceItem;
import atg.commerce.order.CommerceItemManager;
import atg.commerce.order.CommerceItemRelationship;
import atg.commerce.order.HardgoodShippingGroup;
import atg.commerce.order.InvalidParameterException;
import atg.commerce.order.Order;
import atg.commerce.order.OrderManager;
import atg.commerce.order.ShippingGroup;
import atg.commerce.order.ShippingGroupCommerceItemRelationship;
import atg.commerce.order.ShippingGroupNotFoundException;
import atg.core.util.Address;

import com.nm.collections.ServiceLevel;
import com.nm.commerce.NMCommerceItem;
import com.nm.commerce.NMProfile;
import com.nm.commerce.NMRepositoryContactInfo;
import com.nm.commerce.order.NMOrderManager;
import com.nm.commerce.pagedef.definition.CheckoutPageDefinition;
import com.nm.commerce.pagedef.model.CheckoutPageModel;
import com.nm.commerce.pagedef.model.bean.ShippingGroupAux;
import com.nm.commerce.promotion.NMOrderImpl;
import com.nm.components.CommonComponentHelper;
import com.nm.estimateddeliverydate.config.ShippingInfo;
import com.nm.formhandler.ShoppingCartHandler;
import com.nm.sitemessages.Message;
import com.nm.utils.LocalizationUtils;

public class ShippingGroupUtil extends ShippingInfoUtil implements ShippingInfo {
  private static ShippingGroupUtil INSTANCE; // avoid static initialization
  private static final CheckoutConfig config = CheckoutComponents.getConfig();
  
  public ShippingGroupUtil() {}
  
  public static synchronized ShippingGroupUtil getInstance() {
    INSTANCE = (INSTANCE == null) ? new ShippingGroupUtil() : INSTANCE;
    return INSTANCE;
  }
  
  public List<Message> changeAddress(ShoppingCartHandler cart, NMProfile profile, String shippingGroupId, String shippingAddressName) throws Exception {
    
    ShippingGroup shippingGroup = getShippingGroup(cart.getNMOrder(), shippingGroupId);
    
    if (shippingGroup == null) {
      throw new IllegalArgumentException("Could not find shipping group for id " + shippingGroupId);
    }
    
    return changeAddress(cart, profile, shippingGroup, shippingAddressName);
  }
  
  public List<Message> changeAddress(ShoppingCartHandler cart, NMProfile profile, ShippingGroup shippingGroup, String shippingAddressName) throws Exception {
    
    List<Message> messages = new ArrayList<Message>();
    
    CheckoutConfig config = CheckoutComponents.getConfig();
    AddressUtil addressUtil = AddressUtil.getInstance();
    ShippingUtil shippingUtil = ShippingUtil.getInstance();
    ServiceLevelUtil serviceLevelUtil = ServiceLevelUtil.getInstance();
    PricingUtil pricingUtil = PricingUtil.getInstance();
    OrderUtil orderUtil = OrderUtil.getInstance();
    
    try {
      OrderManager orderMgr = cart.getOrderManager();
      NMOrderImpl order = cart.getNMOrder();
      
      removeDelPhone(order, orderMgr, shippingGroup);
      
      addressUtil.copyProfileOrGiftlistAddressToShippingGroupAddress(order, orderMgr, profile, shippingAddressName, shippingGroup);
      
      mergeDuplicates(order, orderMgr, shippingGroup);
      
      ServiceLevel serviceLevel = serviceLevelUtil.getShippingGroupServiceLevel(shippingGroup);
      serviceLevelUtil.setShippingGroupItemServiceLevels(order, shippingGroup, serviceLevel);
      
      shippingUtil.updateShippingMethodsInShippingGroups(profile, messages, shippingGroup,null);
      shippingUtil.hasNonContinentalUSStates(order, messages, null);
      // shippingUtil.validateShipToCountries(cart, messages);
      
      messages.addAll(pricingUtil.repriceOrder(cart, profile, SnapshotUtil.CHANGE_SHIP_DESTINATION));
      
      orderUtil.removeEmptyShippingGroups(order, orderMgr);
      orderMgr.updateOrder(order);
      
    } catch (Exception e) {
      config.logError("Error changing shipping group address: " + e);
      throw e;
    }
    
    return messages;
  }
  
  public List<Message> updateServiceLevel(NMOrderManager orderMgr, NMOrderImpl order, NMProfile profile, String shippingGroupId, String serviceLevelCode, String currentServiceLevelCode)
          throws CommerceException {
    ServiceLevelUtil serviceLevelUtil = ServiceLevelUtil.getInstance();
    ShippingGroup shippingGroup = getShippingGroup(order, shippingGroupId);
    
    if (shippingGroup == null) {
      throw new IllegalArgumentException("Could not find shipping group for id " + shippingGroupId);
    }
    
    ServiceLevel serviceLevel = serviceLevelUtil.getShippingGroupValidServiceLevel(order, shippingGroup, serviceLevelCode);
    
    if (serviceLevel == null) {
      throw new IllegalArgumentException("Invalid service level code " + serviceLevelCode);
    }
    
    return super.updateServiceLevel(orderMgr, order, profile, shippingGroup, serviceLevel, currentServiceLevelCode, false);
  }
  /* updateServiceLevel method moved to super class */
  
  public Collection<Message> updateAllServiceLevels(NMOrderManager orderMgr, NMOrderImpl order, NMProfile profile, String serviceLevelCode, String currentServiceLevelCode) throws CommerceException {
    Set<Message> messages = new HashSet<Message>();
    
    for (ShippingGroup shippingGroup : getShippingGroups(order)) {
      ServiceLevel serviceLevel = ServiceLevelUtil.getInstance().getShippingGroupValidServiceLevel(order, shippingGroup, serviceLevelCode);
      if (ServiceLevel.SL0_SERVICE_LEVEL_TYPE.equals(serviceLevelCode) && (serviceLevel == null)) {
        serviceLevel = CheckoutAPI.getServiceLevelByCodeAndCountry(ServiceLevel.SL0_SERVICE_LEVEL_TYPE, LocalizationUtils.US_COUNTRY_CODE);
      }
      
      if (serviceLevel != null) {
        messages.addAll(updateServiceLevel(orderMgr, order, profile, shippingGroup, serviceLevel, currentServiceLevelCode, false));
      }
    }
    
    return messages;
  }
  
  // function determines if all items in all shipping groups have the same
  // service level, and if so, returns that service level; otherwise, return null
  public String getServiceLevel(NMOrderImpl order) {
    String serviceLevelCode = null;
    
    try {
      out : for (ShippingGroup shippingGroup : getShippingGroups(order)) {
        for (NMCommerceItem item : getItems(shippingGroup)) {
          
          if (serviceLevelCode == null) {
            serviceLevelCode = item.getServicelevel();
          } else {
            if (!serviceLevelCode.equals(item.getServicelevel())) {
              serviceLevelCode = null;
              break out;
            }
          }
        }
      }
    } catch (Exception e) {
      serviceLevelCode = null;
    }
    
    return serviceLevelCode;
  }
  
  public boolean mergeDuplicates(NMOrderImpl order, OrderManager orderMgr, ShippingGroup shippingGroup) throws CommerceException {
    boolean mergedGroups = false;
    
    final String mergeToId = shippingGroup.getId();
    final String mergeToAddressName = shippingGroup.getDescription();
    @SuppressWarnings("unchecked")
    final List<ShippingGroup> orderShippingGroups = order.getShippingGroups();
    
    for (ShippingGroup mergeFromShippingGroup : orderShippingGroups) {
      String mergeFromId = mergeFromShippingGroup.getId();
      String mergeFromAddressName = mergeFromShippingGroup.getDescription();
      List<NMCommerceItem> itemsToMove = new ArrayList<NMCommerceItem>();
      
      if (!mergeToId.equals(mergeFromId) && mergeToAddressName.equals(mergeFromAddressName)) {
        for (NMCommerceItem item : getItems(mergeFromShippingGroup)) {
          final ShippingGroup ciGroup = getShippingGroup(item);
          if (ciGroup.getId().equals(mergeFromId)) {
            itemsToMove.add(item);
          }
        }
      }
      
      for (NMCommerceItem itemToMove : itemsToMove) {
        final long qty = itemToMove.getQuantity();
        moveItemQuantity(orderMgr, order, itemToMove.getId(), mergeFromId, mergeToId, qty);
      }
    }
    
    return mergedGroups;
  }
  
  private ShippingGroupUtil moveItemQuantity(OrderManager orderMgr, Order order, String commerceItemId, String fromShippingGroupId, String toShippingGroupId, long quantity) throws CommerceException {
    
    CommerceItemManager itemMgr = orderMgr.getCommerceItemManager();
    itemMgr.removeItemQuantityFromShippingGroup(order, commerceItemId, fromShippingGroupId, quantity);
    itemMgr.addItemQuantityToShippingGroup(order, commerceItemId, toShippingGroupId, quantity);
    
    return this;
  }
  
  public boolean isShippingFromStore(Collection<ShippingGroup> shippingGroups) {
    for (ShippingGroup shippingGroup : shippingGroups) {
      if (isShippingFromStore(shippingGroup)) {
        return true;
      }
    }
    
    return false;
  }
  
  public boolean isShippingFromStore(ShippingGroup shippingGroup) {
    List<NMCommerceItem> items = getItems(shippingGroup);
    
    for (NMCommerceItem item : items) {
      if (item.getSku().getIsStoreFulfillFlag()) {
        return true;
      }
    }
    
    return false;
  }
  
  public String getCountry(ShippingGroup shippingGroup) {
    String shipToCountry = "US";
    String country = getAddress(shippingGroup).getCountry();
    if (StringUtils.isNotEmpty(country)) {
      shipToCountry = country;
    }
    return shipToCountry;
  }
  
  public ShippingGroupUtil updateDelPhone(OrderManager orderMgr, Order order, ShippingGroup shippingGroup, String deliveryPhone) throws CommerceException {
    
    getAddress(shippingGroup).setDeliveryPhoneNumber(deliveryPhone);
    
    orderMgr.updateOrder(order);
    
    return this;
  }
  
  public ShippingGroupUtil removeDelPhone(Order order, OrderManager orderMgr, ShippingGroup shippingGroup) throws CommerceException {
    return updateDelPhone(orderMgr, order, shippingGroup, "");
  }
  
  public int getQty(ShippingGroup shippingGroup) {
    int qty = 0;
    
    for (NMCommerceItem item : getItems(shippingGroup)) {
      if (!item.isMustShipWithItemGwp()) {
        qty += item.getQuantity();
      }
    }
    
    return qty;
  }
  
  public NMRepositoryContactInfo getAddress(ShippingGroup shippingGroup) {
    Address address = ((HardgoodShippingGroup) shippingGroup).getShippingAddress();
    return (NMRepositoryContactInfo) address;
  }
  
  public List<NMCommerceItem> getItems(ShippingGroup shippingGroup) {
    List<NMCommerceItem> items = new ArrayList<NMCommerceItem>();
    @SuppressWarnings("unchecked")
    List<CommerceItemRelationship> itemRels = shippingGroup.getCommerceItemRelationships();
    
    for (CommerceItemRelationship itemRel : itemRels) {
      items.add((NMCommerceItem) itemRel.getCommerceItem());
    }
    
    return items;
  }
  
  public String getShippingGroupId(final CommerceItem item) {
    final String shipGrpId = getShippingGroup(item).getId();
    return (shipGrpId);
  }
  
  public ShippingGroup getShippingGroup(Order order, String shippingGroupId) {
    try {
      return order.getShippingGroup(shippingGroupId);
    } catch (ShippingGroupNotFoundException e) {
      config.logWarning(String.format("Shipping group %s not found on order %s: %s", shippingGroupId, order, e));
      return null;
    } catch (InvalidParameterException e) {
      config.logError(e);
      return null;
    }
  }
  
  public ShippingGroup getShippingGroup(final CommerceItem item) {
    final ShippingGroup shipGrp = getShipGroupRelationship(item).getShippingGroup();
    return (shipGrp);
  }
  
  @SuppressWarnings("unchecked")
  public List<ShippingGroup> getShippingGroups(NMOrderImpl order) {
    return order.getShippingGroups();
  }
  
  public ShippingGroupCommerceItemRelationship getShipGroupRelationship(final CommerceItem item) {
    final ShippingGroupCommerceItemRelationship shipRel = ((ShippingGroupCommerceItemRelationship) item.getShippingGroupRelationships().get(0));
    return (shipRel);
  }
  
  /**
   * The purpose of this method is to fetch service level from database and updates shipping groups based on it.
   * 
   * @param order
   *          the order
   * @param pageModel
   *          the page model
   * @param checkoutPageDefinition
   *          the checkout page definition
   * @param shippingGroups
   *          the shipping groups
   */
  public void updateShippingGroups(final NMOrderImpl order, final CheckoutPageModel pageModel, final CheckoutPageDefinition checkoutPageDefinition) {
    final List<ShippingGroupAux> shippingGroups = getShippingGroups(order, checkoutPageDefinition);
    super.updateShippingGroups(order, pageModel, checkoutPageDefinition, shippingGroups);
    final Set<ServiceLevel> validServiceLevels = super.getValidServiceLevels(shippingGroups);
    
    if (checkoutPageDefinition.getId().equalsIgnoreCase(ORDER_REVIEW_TEXT)) {
      // Clear session info
      clearEstimatedDeliveryDateInfo();
    }

    if (pageModel.getDisplaySameDayDeliverySL()) {
      // add service level;
      final ServiceLevel sl0 = CheckoutAPI.getServiceLevelByCodeAndCountry(ServiceLevel.SL0_SERVICE_LEVEL_TYPE, LocalizationUtils.US_COUNTRY_CODE);
      
      // set SDD text to service level object
      sl0.setEstimatedArrivalDate(CommonComponentHelper.getSameDayDeliveryUtil().getSddDeliveryText());
      validServiceLevels.add(sl0);
    }
    pageModel.setValidServiceLevels(validServiceLevels.toArray(new ServiceLevel[validServiceLevels.size()]));
  }
  
  /**
   * The purpose of this method is to get the shipping groups, this method calls the super class method where the service levels will be fetched from database.
   * 
   * @param order
   *          the order
   * @param checkoutPageDefinition
   *          the checkout page definition
   * @return the shipping groups
   */
  public List<ShippingGroupAux> getShippingGroups(final NMOrderImpl order, final CheckoutPageDefinition checkoutPageDefinition) {
    return super.getShippingGroups(order, checkoutPageDefinition, null);
  }

}
