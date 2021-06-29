package com.nm.commerce.checkout;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import atg.commerce.CommerceException;
import atg.commerce.order.CommerceItemManager;
import atg.commerce.order.CommerceItemRelationship;
import atg.commerce.order.HardgoodShippingGroup;
import atg.commerce.order.OrderManager;
import atg.commerce.order.ShippingGroup;
import atg.commerce.order.ShippingGroupCommerceItemRelationship;
import atg.commerce.profile.CommercePropertyManager;
import atg.nucleus.Nucleus;
import atg.repository.RepositoryItem;
import atg.servlet.DynamoHttpServletRequest;
import atg.servlet.ServletUtil;

import com.nm.commerce.NMCommerceItem;
import com.nm.commerce.NMProfile;
import com.nm.commerce.order.NMOrderManager;
import com.nm.commerce.promotion.NMOrderImpl;
import com.nm.formhandler.ShoppingCartHandler;
import com.nm.repository.stores.Store;
import com.nm.utils.BrandSpecs;
import com.nm.utils.ShipToStoreHelper;
import com.nm.utils.StringUtilities;

/* package */class ShipToStoreUtil {
  
  private static ShipToStoreUtil INSTANCE;
  
  private final ShipToStoreHelper shipToStoreHelper;
  
  // private constructor enforces singleton behavior
  private ShipToStoreUtil() {
    shipToStoreHelper = CheckoutComponents.getShipToStoreHelper();
  }
  
  public static synchronized ShipToStoreUtil getInstance() {
    INSTANCE = (INSTANCE == null) ? new ShipToStoreUtil() : INSTANCE;
    return INSTANCE;
  }
  
  public boolean isShipToStoreEligibleByLocation(final NMProfile profile) {
    final RepositoryItem billingAddr = (RepositoryItem) profile.getPropertyValue("billingAddress");
    String billingZip = "";
    if (billingAddr != null) {
      billingZip = (String) billingAddr.getPropertyValue("postalCode");
    }
    final String cityAndState = shipToStoreHelper.retrieveCityAndRegionFromAkamai();
    return shipToStoreHelper.determineIfUserQualifiesForShipToStore(billingZip, cityAndState);
  }
  
  /**
   * This method loops through the commerce items to see if at least one is ship to store eligible
   * 
   * @param items
   *          List of NMComerceItem
   * @return false if all items are not ship to store eligible, true of at least one item is ship to store eligible
   */
  public boolean isShipToStoreEligibleByItems(final List<NMCommerceItem> items) {
    boolean isShipToStoreEligible = false;
    
    for (final NMCommerceItem item : items) {
      if (item.isShipToStoreEligible()) {
        isShipToStoreEligible = true;
        break;
      }
    }
    return isShipToStoreEligible;
  }
  
  public boolean isShipToStore(final NMOrderImpl order) {
    boolean value = false;
    
    final List<HardgoodShippingGroup> groups = order.getShippingGroups();
    for (final HardgoodShippingGroup g : groups) {
      final Boolean isShipToStore = (g.getPropertyValue("shipToStoreNumber") != null);
      if ((null != isShipToStore) && isShipToStore) {
        value = true;
        break;
      }
    }
    return value;
  }
  
  public boolean isBopsEnabled() {
    final BrandSpecs brandSpecs = (BrandSpecs) Nucleus.getGlobalNucleus().resolveName("/nm/utils/BrandSpecs");
    return brandSpecs.isBOPSEnabled();
  }
  
  public void updateItemStoreNo(final DynamoHttpServletRequest request, final NMOrderImpl order) throws Exception {
    final List<NMCommerceItem> items = order.getNmCommerceItems();
    if (items != null) {
      for (final NMCommerceItem item : items) {
        final String inputStoreNo = request.getParameter("storeNo" + item.getId());
        final String ciStoreNo = item.getPickupStoreNo();
        if ((inputStoreNo != null) && !inputStoreNo.equals(ciStoreNo)) {
          if (inputStoreNo.equals("") && StringUtilities.isNotEmpty(ciStoreNo)) {
            // store number is cleared remove shipping group associated with this CI
            removeShippingGroup(order, item);
            
          }
          item.setPickupStoreNo(inputStoreNo);
        }
      }
    }
    final OrderManager om = OrderManager.getOrderManager();
    om.updateOrder(order);
  }
  
  private void removeShippingGroup(final NMOrderImpl order, final NMCommerceItem item) throws Exception {
    final ShoppingCartHandler cart = CheckoutComponents.getCartHandler(ServletUtil.getCurrentRequest());
    final NMOrderManager orderMgr = (NMOrderManager) cart.getOrderManager();
    final CommerceItemManager itemManager = orderMgr.getCommerceItemManager();
    final CommercePropertyManager pm = cart.getPropertyManager();
    final String defaultShippingAddrName = pm.getDefaultShippingAddrName(java.util.Locale.US);
    final HardgoodShippingGroup sg = (HardgoodShippingGroup) OrderUtil.getInstance().getShippingGroup(item);
    // check if the shipping group is a store address
    if (StringUtilities.isNotEmpty((String) sg.getPropertyValue("shipToStoreNumber"))) {
      sg.setPropertyValue("shipToStoreNumber", null);
      ShippingUtil.getInstance().getShippingGroupForCI(order, item.getId());
      if (StringUtilities.isNotEmpty(defaultShippingAddrName)) {
        ShippingUtil.getInstance().moveItemToShippingGroup(CheckoutComponents.getProfile(ServletUtil.getCurrentRequest()), cart, order, orderMgr, defaultShippingAddrName, item.getId());
      }
      if (ShippingUtil.getInstance().isEmptyShippingGroup(order, sg.getId())) {
        orderMgr.getShippingGroupManager().removeShippingGroupFromOrder(order, sg.getId());
      }
    }
  }
  
  /**
   * Loops through the shippingGroups and commerceItems to see if the commerceItem has a pickupStoreNo ,if so then change the shippingGroup's shipping address to the store's address.
   * 
   * @param cart
   *          ShoppingCartHandler
   * @param order
   *          NMOrderImpl
   * @param profile
   *          NMProfile
   * @throws Exception
   */
  public void changeShippingAddressToStoreAddress(final ShoppingCartHandler cart, final NMOrderImpl order, final NMProfile profile) throws Exception {
    final List<NMCommerceItem> items = order.getNmCommerceItems();
    final NMOrderManager orderMgr = (NMOrderManager) cart.getOrderManager();
    final OrderUtil orderUtil = OrderUtil.getInstance();
    final CommerceItemManager itemManager = orderMgr.getCommerceItemManager();
    for (final NMCommerceItem item : items) {
      final String pickupStoreNo = item.getPickupStoreNo();
      final ShippingGroup itemGroup = orderUtil.getShippingGroup(item);
      final long qty = item.getQuantity();
      if (StringUtilities.isNotEmpty(pickupStoreNo)) {
        final Store store = CheckoutComponents.getShipToStoreHelper().getStoreAddressByStoreNumber(pickupStoreNo);
        ShippingGroup sg = findShippingGroup(order.getShippingGroups(), pickupStoreNo);
        if (sg != null) {
          // shipping group with store number exists already
          itemManager.removeItemQuantityFromShippingGroup(order, item.getId(), itemGroup.getId(), qty);
          itemManager.addItemQuantityToShippingGroup(order, item.getId(), sg.getId(), qty);
          CheckoutComponents.getShipToStoreHelper().mapStoreAddressToHardgoodShippingGroup(store, (HardgoodShippingGroup) sg, profile, order);
          ShippingUtil.getInstance().removeEmptyShippingGroups(order, orderMgr);
        } else {
          if (order.hasAllBopsItems() && (items.size() == 1)) {
            sg = ((ShippingGroupCommerceItemRelationship) item.getShippingGroupRelationships().get(0)).getShippingGroup();
            sg.setDescription("store address");
            CheckoutComponents.getShipToStoreHelper().mapStoreAddressToHardgoodShippingGroup(store, (HardgoodShippingGroup) sg, profile, order);
          } else {
            // create new shipping group
            final ShippingGroup newShippingGroup = orderMgr.getShippingGroupManager().createShippingGroup();
            newShippingGroup.setDescription("store address");
            // item.setServicelevel(serviceLevel)
            orderMgr.getShippingGroupManager().addShippingGroupToOrder(order, newShippingGroup);
            itemManager.removeItemQuantityFromShippingGroup(order, item.getId(), itemGroup.getId(), qty);
            itemManager.addItemQuantityToShippingGroup(order, item.getId(), newShippingGroup.getId(), qty);
            CheckoutComponents.getShipToStoreHelper().mapStoreAddressToHardgoodShippingGroup(store, (HardgoodShippingGroup) newShippingGroup, profile, order);
            ShippingUtil.getInstance().removeEmptyShippingGroups(order, orderMgr);
          }
        }
        
      } else {
        final CommercePropertyManager pm = cart.getPropertyManager();
        final String defaultShippingAddrName = pm.getDefaultShippingAddrName(java.util.Locale.US);
        final HardgoodShippingGroup sg = (HardgoodShippingGroup) OrderUtil.getInstance().getShippingGroup(item);
        final String sgStoreNumber = (String) sg.getPropertyValue("shipToStoreNumber");
        if (StringUtilities.isNotEmpty(sgStoreNumber)) {
          // this commerce item is shipping to a shipping group with a store number
          // even though it does not have a store number, change to default shipping group
          if (StringUtilities.isNotEmpty(defaultShippingAddrName)) {
            final Map<String, String> itemAddrMap = new HashMap<String, String>();
            itemAddrMap.put(item.getId(), defaultShippingAddrName);
            CheckoutAPI.moveCommerceItemToAddress(profile, cart, order, itemAddrMap, new HashMap<String, String>(), null);
          }
        }
        
      }
    }
    
    final OrderManager om = OrderManager.getOrderManager();
    om.updateOrder(order);
  }
  
  private ShippingGroup findShippingGroup(final List<ShippingGroup> shippingGroupList, final String pickupStoreNo) {
    for (final ShippingGroup shippingGroup : shippingGroupList) {
      final String s2sNumber = (String) ((HardgoodShippingGroup) shippingGroup).getPropertyValue("shipToStoreNumber");
      if (StringUtilities.isNotEmpty(s2sNumber) && s2sNumber.equals(pickupStoreNo)) {
        return shippingGroup;
      }
    }
    return null;
  }
  
  public void correctGwpMustShipWithAndBOPSIssue(final ShoppingCartHandler cart, final NMOrderImpl order) throws CommerceException {
    final List<NMCommerceItem> items = order.getNmCommerceItems();
    final NMOrderManager orderMgr = (NMOrderManager) cart.getOrderManager();
    final OrderUtil orderUtil = OrderUtil.getInstance();
    final CommerceItemManager itemManager = orderMgr.getCommerceItemManager();
    // first mark the shipping groups as stores
    for (final NMCommerceItem item : items) {
      final String pickupStoreNo = item.getPickupStoreNo();
      if (StringUtilities.isNotEmpty(pickupStoreNo)) {
        final HardgoodShippingGroup sg = (HardgoodShippingGroup) orderUtil.getShippingGroup(item);
        final String s2sNumber = (String) sg.getPropertyValue("shipToStoreNumber");
        if (StringUtilities.isEmpty(s2sNumber)) {
          // the ship to store number is empty, set the ship to store number
          sg.setPropertyValue("shipToStoreNumber", pickupStoreNo);
        }
      }
    }
    // now go through every commerce item in the shipping group and check if the item is a gwpMustShipWith
    final List<HardgoodShippingGroup> shippingGroups = orderUtil.getHardgoodShippingGroups(order);
    for (final HardgoodShippingGroup sg : shippingGroups) {
      final String s2sNumber = (String) sg.getPropertyValue("shipToStoreNumber");
      if (StringUtilities.isNotEmpty(s2sNumber)) {
        final List<CommerceItemRelationship> commerceItemRelationshipsLst = sg.getCommerceItemRelationships();
        for (final CommerceItemRelationship commerceItemRelationship : commerceItemRelationshipsLst) {
          final NMCommerceItem ci = (NMCommerceItem) commerceItemRelationship.getCommerceItem();
          if (ci.isMustShipWithItemGwp()) {
            // set the pickup store number
            // set the ship to store flg as true because all GWP items have to ship from online inventory
            ci.setPickupStoreNo(s2sNumber);
            ci.setShipToStoreFlg(true);
          }
        }
      }
    }
    
    ShippingUtil.getInstance().removeEmptyShippingGroups(order, orderMgr);
    final OrderManager om = OrderManager.getOrderManager();
    om.updateOrder(order);
    
  }
  
}
