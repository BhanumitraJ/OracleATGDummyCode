package com.nm.commerce.checkout;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpSession;

import atg.commerce.CommerceException;
import atg.commerce.gifts.GiftlistManager;
import atg.commerce.order.CommerceItem;
import atg.commerce.order.CommerceItemManager;
import atg.commerce.order.HardgoodShippingGroup;
import atg.commerce.order.Order;
import atg.commerce.order.OrderHolder;
import atg.commerce.order.OrderImpl;
import atg.commerce.order.OrderManager;
import atg.commerce.order.OrderQueries;
import atg.commerce.order.OrderTools;
import atg.commerce.order.ShippingGroup;
import atg.commerce.order.ShippingGroupCommerceItemRelationship;
import atg.commerce.order.ShippingGroupManager;
import atg.commerce.pricing.PricingModelHolder;
import atg.commerce.profile.CommercePropertyManager;
import atg.commerce.states.OrderStates;
import atg.commerce.states.StateDefinitions;
import atg.core.util.StringUtils;
import atg.nucleus.GenericService;
import atg.nucleus.Nucleus;
import atg.repository.MutableRepository;
import atg.repository.MutableRepositoryItem;
import atg.repository.Repository;
import atg.repository.RepositoryException;
import atg.repository.RepositoryItem;
import atg.repository.RepositoryItemImpl;
import atg.service.dynamo.Configuration;
import atg.service.pipeline.PipelineResult;
import atg.service.pipeline.RunProcessException;
import atg.servlet.DynamoHttpServletRequest;
import atg.servlet.ServletUtil;
import atg.userprofiling.Profile;

import com.nm.ajax.checkout.session.PersonalizedPromoTracking;
import com.nm.ajax.checkout.utils.ComponentUtils;
import com.nm.ajax.checkout.utils.RepositoryUtils;
import com.nm.collections.GiftWithPurchaseArray;
import com.nm.collections.GiftWithPurchaseSelectArray;
import com.nm.collections.IGiftWithPurchase;
import com.nm.collections.NMPromotion;
import com.nm.collections.PurchaseWithPurchaseArray;
import com.nm.collections.ServiceLevel;
import com.nm.collections.ServiceLevelArray;
import com.nm.commerce.NMAddress;
import com.nm.commerce.NMCommerceItem;
import com.nm.commerce.NMCommerceProfileTools;
import com.nm.commerce.NMCreditCard;
import com.nm.commerce.NMOrderHolder;
import com.nm.commerce.NMProfile;
import com.nm.commerce.NMRepositoryContactInfo;
import com.nm.commerce.checkout.beans.ContactInfo;
import com.nm.commerce.checkout.beans.OrderInfo;
import com.nm.commerce.checkout.beans.ResultBean;
import com.nm.commerce.checkout.beans.ResultBeanHelper;
import com.nm.commerce.giftlist.GiftlistConstants;
import com.nm.commerce.giftlist.NMGiftlistManager;
import com.nm.commerce.order.NMOrderManager;
import com.nm.commerce.order.NMPaymentGroupManager;
import com.nm.commerce.pricing.Markdown;
import com.nm.commerce.pricing.NMItemPriceInfo;
import com.nm.commerce.promotion.CheckCsrGwp;
import com.nm.commerce.promotion.CheckCsrGwpSelect;
import com.nm.commerce.promotion.CheckCsrPwp;
import com.nm.commerce.promotion.NMOrderImpl;
import com.nm.common.INMGenericConstants;
import com.nm.common.NMBorderFreeBasketItem;
import com.nm.common.NMBorderFreeGetQuoteResponse;
import com.nm.common.NMBorderFreePayPalResponse;
import com.nm.common.SmartPostServiceLevelSessionBean;
import com.nm.components.CommonComponentHelper;
import com.nm.discoverableservices.CmosStringCommDiscoverable;
import com.nm.discoverableservices.NMDiscoverableServiceProxyFactory;
import com.nm.droplet.LinkshareCookie;
import com.nm.droplet.DeptQuantityRestrictionLookup;
import com.nm.edo.constants.EmployeeDiscountConstants;
import com.nm.edo.util.EmployeeDiscountsUtil;
import com.nm.estimateddeliverydate.config.EstimatedDeliveryDateConfig;
import com.nm.formhandler.NMProfileFormHandler;
import com.nm.formhandler.ShoppingCartHandler;
import com.nm.integration.ShopRunnerConstants;
import com.nm.integration.VMeInfoVO;
import com.nm.international.fiftyone.checkoutapi.BorderFreeConstants;
import com.nm.masterpass.integration.MasterPassInfoVO;
import com.nm.profile.ProfileProperties;
import com.nm.repository.ProfileRepository;
import com.nm.returnseligibility.util.ReturnsEligibilityUtil;
import com.nm.services.CmosStringComm;
import com.nm.services.ccauth.CCAuthContainer;
import com.nm.sitemessages.Message;
import com.nm.sitemessages.MessageContainer;
import com.nm.sitemessages.MessageDefs;
import com.nm.sitemessages.OmnitureData;
import com.nm.sitepersonalization.SitePersonalizationConstants;
import com.nm.storeinventory.ItemAvailabilityLevel;
import com.nm.utils.AssociatePinUtil;
import com.nm.utils.BrandSpecs;
import com.nm.utils.EncryptDecrypt;
import com.nm.utils.EnvironmentUtils;
import com.nm.utils.ExceptionUtil;
import com.nm.utils.LinkedEmailUtils;
import com.nm.utils.LocalizationUtils;
import com.nm.utils.NmoUtils;
import com.nm.utils.ProdSkuUtil;
import com.nm.utils.PromotionsHelper;
import com.nm.utils.StringUtilities;
import com.nm.utils.SystemSpecs;
import com.nm.utils.bing.BingSessionData;
import com.nm.utils.bing.BingUtils;
import com.nm.utils.fiftyone.FiftyOneUtils;

/**
 * Package access - outside of this package, methods should be accessed through CheckoutAPI.
 */
/* package */class OrderUtil {
  
  private static OrderUtil INSTANCE; // avoid static initialization
  
  private static GiftWithPurchaseArray gwpArray = CheckCsrGwp.getGiftWithPurchaseArray();
  private static GiftWithPurchaseSelectArray gwpSelectArray = CheckCsrGwpSelect.getGiftWithPurchaseSelectArray();
  private static PurchaseWithPurchaseArray pwpArray = CheckCsrPwp.getPurchaseWithPurchaseArray();
  private static final String SHIPGRP_SERVICELVL_DELIM = ":";
  
  private static final String UPGRADE2NDDAY = "UPGRADE2NDDAY";
  private static final String UPGRADEOVERNIGHT = "UPGRADEOVERNIGHT";
  EmployeeDiscountsUtil employeeDiscountsUtil = CommonComponentHelper.getEmployeeDiscountsUtil();
  private final CmosStringCommDiscoverable cmosStringCommDiscoverable = 
          (CmosStringCommDiscoverable) NMDiscoverableServiceProxyFactory.createProxy(ComponentUtils.getInstance().getCmosStringComm());
  
  /**
   * Gets the returns charge util.
   * 
   * @return the returns charge util
   */
  public ReturnsEligibilityUtil getReturnsEligibilityUtil() {
    return CommonComponentHelper.getReturnsEligibilityUtil();
  }
  
  // private constructor enforces singleton behavior
  private OrderUtil() {}
  
  public static synchronized OrderUtil getInstance() {
    INSTANCE = INSTANCE == null ? new OrderUtil() : INSTANCE;
    return INSTANCE;
  }
  
  /**
   * Gets the current request.
   * 
   * @return the current request
   */
  private DynamoHttpServletRequest getCurrentRequest() {
    return ServletUtil.getCurrentRequest();
  }
  
  /**
   * Gets the profile through CheckoutComponents class.
   * 
   * @return the profile
   */
  private NMProfile getProfile() {
    return CheckoutComponents.getProfile(getCurrentRequest());
  }
  
  /**
   * Gets the localization utils through CheckoutComponents class.
   * 
   * @return the localization utils
   */
  private LocalizationUtils getLocalizationUtils() {
    return CheckoutComponents.getLocalizationUtils();
  }

  // 41279 - Reset commerceItem to new address and make this address as Default shipping address.
  public void resetCommerceItemsToInternationalShippingAddress(final NMProfile profile, final ContactInfo address, final NMOrderImpl order, final OrderManager orderMgr) throws CommerceException,
          Exception {
    final AddressUtil addressUtil = AddressUtil.getInstance();
    final List<ShippingGroup> shipGroups = order.getShippingGroups();
    if (order.getShippingGroupCount() == 1) {
      final HardgoodShippingGroup sg = (HardgoodShippingGroup) shipGroups.get(0);
      final NMRepositoryContactInfo addr = (NMRepositoryContactInfo) sg.getShippingAddress();
      addressUtil.copyFieldsToOrderAddress(address, addr.getRepositoryItem());
      sg.setDescription("My Shipping Address");
      
    } else {
      throw new CommerceException("International order should not contain multiple shipping addresses.");
    }
  }
  
  public void resetCommerceItemsToShopRunnerShippingAddress(final NMProfile profile, final ContactInfo address, final NMOrderImpl order, final OrderManager orderMgr) throws CommerceException,
          Exception {
    final AddressUtil addressUtil = AddressUtil.getInstance();
    final List<ShippingGroup> shipGroups = order.getShippingGroups();
    if (order.getShippingGroupCount() == 1) {
      final HardgoodShippingGroup sg = (HardgoodShippingGroup) shipGroups.get(0);
      final NMRepositoryContactInfo addr = (NMRepositoryContactInfo) sg.getShippingAddress();
      addressUtil.copyFieldsToOrderAddress(address, addr.getRepositoryItem());
      sg.setDescription("My Shipping Address");
      
    } else {
      throw new CommerceException("ShopRunner order should not contain multiple shipping addresses.");
    }
  }
  
  public void resetCommerceItemsToDefaultAddresses(final Profile profile, final NMOrderImpl order, final OrderManager orderMgr) throws CommerceException, Exception {
    final ShippingUtil shippingUtil = ShippingUtil.getInstance();
    final List<NMCommerceItem> commerceItemsList = order.getNmCommerceItems();
    @SuppressWarnings("unchecked")
    final List<ShippingGroup> sgList = order.getShippingGroups();
    final Iterator<NMCommerceItem> commerceItemsListIterator = commerceItemsList.iterator();
    
    final CommerceItemManager itemMgr = orderMgr.getCommerceItemManager();
    final ShippingGroupManager sgMgr = orderMgr.getShippingGroupManager();
    final Map<String, ShippingGroup> groups = getOrderShippingGroups(order);
    
    boolean isNewShippingGroup = false;
    ShippingGroup newShippingGroup = null;
    if ((shippingUtil.getNonWishlistShippingGroupCount(sgList) == 1) && (groups.size() != 0)) {
      final Iterator<String> i = groups.keySet().iterator();
      while (i.hasNext()) {
        final String sgId = i.next();
        final ShippingGroup sg = groups.get(sgId);
        if (!shippingUtil.isWishlistShippingGroup(sg)) {
          newShippingGroup = sg;
          break;
        }
      }
    } else {
      isNewShippingGroup = true;
      sgMgr.removeAllShippingGroupsFromOrder(order);
      
      // non-wish list/registry items are all assigned to My Shipping Address
      // System.out.println("about to create a new Shipping Group");
      newShippingGroup = orderMgr.getShippingGroupManager().createShippingGroup();
      newShippingGroup.setDescription("My Shipping Address");
      orderMgr.getShippingGroupManager().addShippingGroupToOrder(order, newShippingGroup);
    }
    
    final MutableRepositoryItem profileAddress = (MutableRepositoryItem) profile.getPropertyValue(ProfileProperties.Profile_shippingAddress);
    
    OrderTools.copyAddress(profileAddress, ((HardgoodShippingGroup) newShippingGroup).getShippingAddress());
    if (isNewShippingGroup) {
      while (commerceItemsListIterator.hasNext()) {
        final NMCommerceItem ci = commerceItemsListIterator.next();
        if ((ci.getRegistryId() != null) && !ci.getRegistryId().equals("")) {
          ((NMProfile) profile).getViewedRegistriesList().put(ci.getRegistryId(), "");
          final Iterator<ShippingGroup> sgListIterator = sgList.iterator();
          String sgMatchId = null;
          
          while (sgListIterator.hasNext()) {
            final ShippingGroup sgItem = sgListIterator.next();
            final String currentShippingGroupDescription = sgItem.getDescription();
            
            if ((currentShippingGroupDescription != null) && currentShippingGroupDescription.equalsIgnoreCase(ci.getRegistryId())) {
              sgMatchId = sgItem.getId();
              break;
            }
          }
          
          if ((sgMatchId != null) && !sgMatchId.equals("")) {
            itemMgr.addItemQuantityToShippingGroup(order, ci.getId(), sgMatchId, ci.getQuantity());
          } else {
            final ShippingGroup newRegistryShippingGroup = orderMgr.getShippingGroupManager().createShippingGroup();
            orderMgr.getShippingGroupManager().addShippingGroupToOrder(order, newRegistryShippingGroup);
            copyProfileAddressToShippingGroupAddress(profile, order, orderMgr, ci.getRegistryId(), newRegistryShippingGroup);
            itemMgr.addItemQuantityToShippingGroup(order, ci.getId(), newRegistryShippingGroup.getId(), ci.getQuantity());
          }
        } else {
          itemMgr.addItemQuantityToShippingGroup(order, ci.getId(), newShippingGroup.getId(), ci.getQuantity());
        }
      }
    }
    
    orderMgr.updateOrder(order);
  }
  
  /**
   * This method will lookup the address specified by the nickname provided change the address on the shipping group specified to that address. If the address cannot be found on the profile, the
   * method will check to see if the address specified is on the one of the registries viewed by the profile. If the address is found in the list of registry addresses, the FlgGiftReg flag will be set
   * to true and that address will be copied to the shipping group specified.
   * 
   * @param profile
   *          - the profile containing the order to be interrogated
   * @param order
   *          - the order containing the shipping group addresses to be modified
   * @param orderMgr
   *          - the orderManager used to manipulate the order
   * @param pProfileAddressName
   *          - the nickname of the address to find on the profile
   * @param pShippingGroup
   *          - the shipping group object whose address will be modified.
   * @throws Exception
   *           - if the address cannot be found on the profile or the registries viewed by that profile.
   */
  public void copyProfileAddressToShippingGroupAddress(final Profile profile, final Order order, final OrderManager orderMgr, final String pProfileAddressName, final ShippingGroup pShippingGroup)
          throws Exception {
    try {
      final MutableRepositoryItem profileAddress = (MutableRepositoryItem)
      
      CheckoutComponents.getCommerceProfileTools().getProfileAddress(profile, pProfileAddressName);
      
      buildShipToList(profile, order, orderMgr);
      
      if (profileAddress != null) {
        final HardgoodShippingGroup shippingGroup = (HardgoodShippingGroup) pShippingGroup;
        final NMRepositoryContactInfo address = (NMRepositoryContactInfo) shippingGroup.getShippingAddress();
        address.setFlgGiftReg(new Boolean(false));
        address.setTransGiftRegId("");
        OrderTools.copyAddress(profileAddress, address);
      } else { // nmts5 had to change what we are doing if the address does not exist on the profile for gift reg
        final NMProfile theNMProfile = (NMProfile) profile;
        final Map<String, String> tempMap = theNMProfile.getViewedRegistriesList();
        
        final String theValue = tempMap.get(pProfileAddressName);
        
        if ((theValue != null) && !theValue.trim().equals("")) {
          final Repository profileRepository = profile.getRepository();
          final RepositoryItem Contactinfo = profileRepository.getItem(theValue, ProfileProperties.Profile_Desc_contactInfo);
          final HardgoodShippingGroup shippingGroup = (HardgoodShippingGroup) pShippingGroup;
          final NMRepositoryContactInfo address = (NMRepositoryContactInfo) shippingGroup.getShippingAddress();
          
          address.setFlgGiftReg(new Boolean(true));
          address.setTransGiftRegId(pProfileAddressName);
          
          final GiftlistManager giftMgr = CheckoutComponents.getGiftlistManager();
          final RepositoryItem giftlist = giftMgr.getGiftlist(pProfileAddressName);
          final String theDelPhone = (String) giftlist.getPropertyValue("deliveryPhone");
          
          @SuppressWarnings("unchecked")
          final List<NMCommerceItem> commerceItemList = orderMgr.getCommerceItemManager().getCommerceItemsFromShippingGroup(shippingGroup);
          final Iterator<NMCommerceItem> commerceItemListIterator = commerceItemList.iterator();
          
          NMCommerceItem commerceItemRI;
          while (commerceItemListIterator.hasNext()) {
            commerceItemRI = commerceItemListIterator.next();
            if (((Boolean) ((RepositoryItem) commerceItemRI.getAuxiliaryData().getProductRef()).getPropertyValue("flgDeliveryPhone")).booleanValue()) {
              address.setDeliveryPhoneNumber(theDelPhone);
              break;
            }
          }
          
          OrderTools.copyAddress(Contactinfo, address);
        } else {
          if ((pProfileAddressName != null) && !pProfileAddressName.equals(pShippingGroup.getId())) {
            throw new Exception("Profile address " + pProfileAddressName + " is missing for profile: " + profile.getRepositoryId());
          }
        }
      }
      
      pShippingGroup.setDescription(pProfileAddressName);
    } catch (final Exception e) {
      throw new Exception("Error in copyProfileAddressToShippingGroupAddress: " + ExceptionUtil.getExceptionInfo(e), e);
    }
  }
  
  public void evaluateGwpMustShipWith(final NMOrderImpl order, final OrderManager orderMgr) throws CommerceException {
    synchronized (order) {
      mergeSplitCommerceItems(order, (NMOrderManager) orderMgr, true, true, true);
      
      final List<NMCommerceItem> cartItems = order.getNmCommerceItems();
      final List<NMCommerceItem> gwpItems = getMustShipWithItemGWPs(cartItems);
      
      // if there are no must ship with item GWP's then there is nothing to do
      if (gwpItems.size() < 1) {
        return;
      }
      
      final NMOrderManager orderManager = (NMOrderManager) orderMgr;
      final Map<String, List<NMCommerceItem>> shipGrps = splitItemsByShippingGroup(cartItems);
      final Map<String, Double> qualShipGrps = new HashMap<String, Double>();
      for (final Iterator<NMCommerceItem> i = gwpItems.iterator(); i.hasNext();) {
        final NMCommerceItem gwpItem = i.next();
        double highAmt = 0.0;
        String highSgId = null;
        String highServiceLvl = null;
        
        final long gwpQty = gwpItem.getQuantity();
        long gwpAvailQty = gwpQty;
        gwpItem.setQuantity(0L);
        final String[] promoKeys = gwpItem.getSendCmosPromoKey().split(",");
        for (int k = 0; k < promoKeys.length; ++k) {
          final String promoKey = promoKeys[k];
          
          // this method returns the total GWP qualifying dollars or
          // qty on the order and
          // populates the map qualShipGrps which tracks the
          // qualifying dollars or qty in
          // each shipping group. Shipping groups with no qualifying
          // dollars or qty will
          // not be present in this Map.
          boolean useDollarQualifier = true;
          final double dollarQualifier = getDollarQualifier(promoKey);
          if (dollarQualifier == 0.0) {
            useDollarQualifier = false;
          }
          
          // divider is the dollarQualifer unless dollarQualifer is 0,
          // then the divider is 1.0 (1 gwp for each qual item)
          double qualifyingDivider = dollarQualifier;
          if (useDollarQualifier) {
            calcQualDollarsByShipGroup(promoKey, shipGrps, qualShipGrps);
          } else {
            calcQualQtyByShipGroup(promoKey, shipGrps, qualShipGrps);
            // one gwp for each qualifying item
            qualifyingDivider = 1.0;
          }
          
          for (final Iterator<String> qualGrp = qualShipGrps.keySet().iterator(); qualGrp.hasNext();) {
            final String sgslKey = qualGrp.next();
            final String sgId = getShipGrpFromKey(sgslKey);
            final String serviceLevel = getServiceLvlFromKey(sgslKey);
            
            // calculate the quantity of GWPs that should be
            // assigned to this shipping group
            // if there is a dollarQualifer, the calculation is
            // based on the percentage of qualifying dollars
            // (sgQualAmt represents dollars)
            // if dollarQualifer is $0, the calculation is based on
            // the num of qual items (sgQualAmt represents qty)
            final double sgQualAmt = qualShipGrps.get(sgslKey).doubleValue();
            
            if ((null == highSgId) || (sgQualAmt > highAmt)) {
              highAmt = sgQualAmt;
              highSgId = sgId;
              highServiceLvl = serviceLevel;
            }
            
            long allocQty = 0;
            if (qualifyingDivider == 1.0) {
              allocQty = (long) sgQualAmt;
            } else {
              allocQty = (long) (sgQualAmt / qualifyingDivider);
            }
            
            if (allocQty > gwpAvailQty) {
              allocQty = gwpAvailQty;
            }
            
            if (allocQty > 0) {
              gwpAvailQty = moveItemQtyToShippingGroup(order, orderMgr, gwpItem, gwpAvailQty, allocQty, sgId, serviceLevel);
            }
          }
        }
        
        // if after allocating GWP items to shipping groups there is any
        // remaining
        // then assign them to the highest dollar qualifying shipping
        // group we
        // found in the order.
        if ((gwpAvailQty > 0) && (null != highSgId)) {
          gwpAvailQty = addRemainingGwpToShippingGroup(order, orderMgr, gwpItem, gwpAvailQty, highSgId, highServiceLvl);
        }
        
        if (gwpAvailQty > 0) {
          final GenericService config = CheckoutComponents.getConfig();
          config.logError("Error assigning GWPs to order " + order.getId() + ": " + gwpAvailQty + " remaining unassigned");
        }
        
        if (gwpItem.getQuantity() < 1) {
          final CommerceItemManager itemManager = orderMgr.getCommerceItemManager();
          itemManager.removeItemFromOrder(order, gwpItem.getId());
        }
      }
      
      orderManager.updateOrder(order);
    }
    return;
  }
  
  /**
   * <p>
   * This method sets ALIPAY or ShopRunner's Info(vendorId and token) on Orders
   * </p>
   * 
   * @param order
   * @param request
   * @param orderInfo
   */
  private void setVendorInfoToOrder(final OrderInfo orderInfo, final NMOrderImpl order, final DynamoHttpServletRequest request) {
    final SystemSpecs systemSpecs = ComponentUtils.getInstance().getSystemSpecs();
    final BrandSpecs brandSpecs = (BrandSpecs) Nucleus.getGlobalNucleus().resolveName("/nm/utils/BrandSpecs");
    final Boolean shopRunnerEnabled = systemSpecs.isShopRunnerEnabled();
    final boolean isShopRunnerSL = CheckoutAPI.isShopRunnerSL();
    final String shopRunnerVendorId = CheckoutAPI.getShopRunnerVendorId(orderInfo.isSRExpressCheckOut());
    final List<NMCommerceItem> lineItems = order.getNmCommerceItems();
    final LocalizationUtils localizationUtils = getLocalizationUtils();
    final Boolean alipayEnabled = systemSpecs.isAlipayEnabled();
    final String shopRunnerToken = CheckoutAPI.getShopRunnerToken(request);
    final String alipayVendorId = CheckoutAPI.getAlipayVendorId(orderInfo.isAlipayExpressCheckout());
    final String international = request.getParameter("isInternationalUser");
    // Changes for updating vendor information for BFX orders. This is for LC and BG.
    final Boolean bfxOrder = (Boolean) request.getSession().getAttribute("bfxOrder");
    try {
      if ((null != bfxOrder) && bfxOrder) {
        final MutableRepository orderRepository = (MutableRepository) RepositoryUtils.getInstance().getOrderRepository();
        order.setVendorId(FiftyOneUtils.VENDOR_ID);
        final List<ShippingGroup> shippingGroupsLst = order.getShippingGroups();
        for (final ShippingGroup shippingGroup : shippingGroupsLst) {
          final RepositoryItem repHSG = orderRepository.getItem(shippingGroup.getId(), "hardgoodShippingGroup");
          final String shipToaddressLine2 = NmoUtils.invalidCharacterCleanup((String) repHSG.getPropertyValue("address2"));
          if (StringUtilities.isNotEmpty(shipToaddressLine2)) {
            order.setVendorOrderId(shipToaddressLine2);
            break;
          }
        }
      } else {
        CheckoutComponents.getConfig().logError("No Referer exixts or Referer is empty");
      }
    } catch (final RepositoryException repositoryExc) {
      if (CheckoutComponents.getConfig().isLoggingError()) {
        CheckoutComponents.getConfig().logError("OrderRepository hardgoodShippingGroup had a problem ", repositoryExc);
      }
    }
    
    if ((alipayEnabled && localizationUtils.findCountryCodeUsingEdgescape(request).equalsIgnoreCase(ShopRunnerConstants.CHINA_COUNTRY_CODE)) || (shopRunnerEnabled && isShopRunnerSL)) {
      if ((shopRunnerToken != null) && (international == null)) {
        if (orderInfo.isAlipayExpressCheckout()) {
          // set VendorId as ALIPAY for Alipay Orders
          order.setVendorId(alipayVendorId);
          order.setVendorOrderId(shopRunnerToken);
        } else {
          // Ensure at least one SR Eligible item is in the order
          for (int i = 0; i < lineItems.size(); i++) {
            if (lineItems.get(i).getProduct().getIsShopRunnerEligible()) {
              order.setVendorId(shopRunnerVendorId);
              order.setVendorOrderId(shopRunnerToken);
              break;
            }
          }
        }
      }
    } else {
      order.setRemoveActivatedPromoCode("SHOPRUNNER");
      order.setRemovePromoName(brandSpecs.getShopRunnerPromoName());
    }
  }
  
  public void mergeSplitCommerceItems(final NMOrderImpl order, final NMOrderManager orderManager, final boolean ignoreGWPShipGroup, final boolean onlyMergeGWPItems,
          final boolean restrictToMustShipGWPs) throws CommerceException {
    // we should never merge 2 commerce items with difference percent off
    // promotions.
    final boolean ignorePercentOffPromotions = false;
    boolean changesMade = false;
    final CommerceItemManager itemManager = orderManager.getCommerceItemManager();
    final List<NMCommerceItem> removeCiList = new ArrayList<NMCommerceItem>();
    int curIndex = 0;
    synchronized (order) {
      List<NMCommerceItem> items = order.getNmCommerceItems();
      while (curIndex < items.size()) {
        final NMCommerceItem curCi = items.get(curIndex);
        
        if (onlyMergeGWPItems) {
          if (restrictToMustShipGWPs && !curCi.isMustShipWithItemGwp()) {
            ++curIndex;
            continue;
          }
          
          if (!curCi.isGwpItem()) {
            ++curIndex;
            continue;
          }
        }
        
        for (int i = curIndex + 1; i < items.size(); ++i) {
          final NMCommerceItem ci = items.get(i);
          
          if (curCi.equalCommerceItems(ci, ignoreGWPShipGroup, ignorePercentOffPromotions)) {
            if (!ci.isPwpItem()) {
              final long curCiQty = curCi.getQuantity();
              final long ciQty = ci.getQuantity();
              final String curCiShipId = getShippingGroupId(curCi);
              
              changesMade = true;
              curCi.setQuantity(curCiQty + ciQty);
              curCi.getPriceInfo().setAmount(curCi.getPriceInfo().getAmount() + ci.getPriceInfo().getAmount());
              removeCiList.add(ci);
              itemManager.addItemQuantityToShippingGroup(order, curCi.getId(), curCiShipId, ciQty);
            }
          }
        }
        
        if (removeCiList.size() > 0) {
          for (final Iterator<NMCommerceItem> id = removeCiList.iterator(); id.hasNext();) {
            final NMCommerceItem ci = id.next();
            itemManager.removeItemFromOrder(order, ci.getId());
          }
          
          removeCiList.clear();
          items = order.getNmCommerceItems();
        }
        
        ++curIndex;
      }
      
      if (changesMade) {
        final DynamoHttpServletRequest request = getCurrentRequest();
        final NMProfile profile = CheckoutComponents.getProfile(request);
        final ShoppingCartHandler cart = CheckoutComponents.getCartHandler(request);
        orderManager.updateOrder(order);
        try {
          CheckoutAPI.repriceOrder(cart, profile, -1);
        } catch (final Exception e) {
          
          CheckoutComponents.getConfig().logError("Error while Repricing Order : ", e);
          throw new CommerceException(e);
        }
      }
    }
  }
  
  private Map<String, List<NMCommerceItem>> splitItemsByShippingGroup(final List<NMCommerceItem> cartItems) {
    final Map<String, List<NMCommerceItem>> shipGrps = new HashMap<String, List<NMCommerceItem>>();
    
    // split the order by shipping group
    for (final Iterator<NMCommerceItem> i = cartItems.iterator(); i.hasNext();) {
      final NMCommerceItem ci = i.next();
      if (!ci.isMustShipWithItemGwp()) {
        final String sgId = getShippingGroupId(ci);
        List<NMCommerceItem> sgItemList = shipGrps.get(sgId);
        if (null == sgItemList) {
          sgItemList = new ArrayList<NMCommerceItem>();
          shipGrps.put(sgId, sgItemList);
        }
        sgItemList.add(ci);
      }
    }
    
    return shipGrps;
  }
  
  public Map<String, ShippingGroup> getOrderShippingGroups(final Order order) {
    final List<NMCommerceItem> cartItems = ((NMOrderImpl) order).getNmCommerceItems();
    final Map<String, ShippingGroup> shipGrps = new HashMap<String, ShippingGroup>();
    
    // split the order by shipping group
    for (final Iterator<NMCommerceItem> i = cartItems.iterator(); i.hasNext();) {
      final NMCommerceItem ci = i.next();
      final ShippingGroup group = getShippingGroup(ci);
      final String sgId = group.getId();
      if (!shipGrps.containsKey(sgId)) {
        shipGrps.put(sgId, group);
      }
    }
    
    return shipGrps;
  }
  
  public List<HardgoodShippingGroup> getHardgoodShippingGroups(final Order order) {
    final ArrayList<HardgoodShippingGroup> hardgoodShippingGroupList = new ArrayList<HardgoodShippingGroup>();
    @SuppressWarnings("unchecked")
    final List<HardgoodShippingGroup> aList = order.getShippingGroups();
    for (int pos = 0; pos < aList.size(); pos++) {
      final HardgoodShippingGroup hg = (HardgoodShippingGroup) order.getShippingGroups().get(pos);
      hardgoodShippingGroupList.add(hg);
    }
    return hardgoodShippingGroupList;
  }
  
  public boolean shippingGroupFilledIn(final Order order) {
    final List<HardgoodShippingGroup> activeShippingGroups = getHardgoodShippingGroups(order);
    boolean shippingGroupFilledIn = false;
    if (!activeShippingGroups.isEmpty()) {
      shippingGroupFilledIn = true;
      for (int pos = 0; pos < activeShippingGroups.size(); pos++) {
        final HardgoodShippingGroup hg = activeShippingGroups.get(pos);
        final String hgFirstName = hg.getShippingAddress().getFirstName();
        if (StringUtilities.isEmpty(hgFirstName)) {
          shippingGroupFilledIn = false;
          break;
        }
      }
    }
    return shippingGroupFilledIn;
  }
  
  /**
   * Return a specific shipping group from current order
   * 
   * @param order
   * @return
   */
  public HardgoodShippingGroup getHardgoodShippingGroup(final Order order, final String sgId) {
    if ((sgId == null) || "".equals(sgId)) {
      return null;
    }
    
    HardgoodShippingGroup hg = null;
    @SuppressWarnings("unchecked")
    final List<HardgoodShippingGroup> aList = order.getShippingGroups();
    for (int pos = 0; pos < aList.size(); pos++) {
      if (sgId.equals(((HardgoodShippingGroup) order.getShippingGroups().get(pos)).getId())) {
        hg = (HardgoodShippingGroup) order.getShippingGroups().get(pos);
        break;
      }
    }
    return hg;
  }
  
  private List<NMCommerceItem> getMustShipWithItemGWPs(final List<NMCommerceItem> cartItems) {
    final List<NMCommerceItem> gwpItems = new ArrayList<NMCommerceItem>();
    for (final Iterator<NMCommerceItem> i = cartItems.iterator(); i.hasNext();) {
      final NMCommerceItem item = i.next();
      if (item.isMustShipWithItemGwp()) {
        gwpItems.add(item);
      }
    }
    return gwpItems;
  }
  
  /**
   * Returns true if there is at least one item that is a must ship with item GWP
   * 
   */
  public boolean isHasMustShipWithItemGWP(final List<NMCommerceItem> cartItems) {
    return getMustShipWithItemGWPs(cartItems).size() > 0;
  }
  
  private double getDollarQualifier(final String promoKey) {
    final IGiftWithPurchase gwp = getGwpPromotion(promoKey);
    if (null == gwp) {
      // WR29698 - Since promokey could be any of the other keys beside GWP/GWP Select
      // the logger.error would not apply, so commenting out.
      // logger.error("Unable to find GWP or GWPSelect with promoKey " + promoKey);
      return 0.0;
    }
    
    return Double.parseDouble(gwp.getDollarQualifier());
  }
  
  private IGiftWithPurchase getGwpPromotion(final String promoKey) {
    // check to see if promoKey returns a regular GWP promotion
    IGiftWithPurchase gwp = gwpArray.getPromotion(promoKey);
    
    if (null == gwp) {
      // check to see if promoKey returns a GWP select promotion
      gwp = gwpSelectArray.getPromotion(promoKey);
      if (null == gwp) {
        gwp = pwpArray.getPromotion(promoKey);
      }
    }
    return gwp;
  }
  
  private long moveItemQtyToShippingGroup(final Order order, final OrderManager orderMgr, final NMCommerceItem item, final long availQty, final long qty, final String shipGrpId,
          final String serviceLvl) throws CommerceException {
    if (qty < 1) {
      return availQty;
    }
    
    final NMOrderManager orderManager = (NMOrderManager) orderMgr;
    final CommerceItemManager itemManager = orderManager.getCommerceItemManager();
    final String origShipGrpId = getShippingGroupId(item);
    
    // if we are just changing the quantity of the item without moving it to
    // a different shipping group/service level. note that we don't adjust
    // the shipping group
    // quantity on this condition. all the GWPs were allocated to this
    // shipping
    // group when we started so we will let moves to other shipping groups
    // remove quantity from this one.
    final String serviceLevel = serviceLvl;
    if (origShipGrpId.equals(shipGrpId) && serviceLevel.equalsIgnoreCase(item.getServicelevel())) {
      item.setQuantity(qty);
      return availQty - qty;
    }
    
    // the requested quantity is being moved to a new shipping group. create a
    // new commerce item in the requested shipping group and assign the quantity to it.
    NMCommerceItem clonedItem = (NMCommerceItem) item.clone();
    clonedItem.setQuantity(qty);
    clonedItem.setServicelevel(serviceLevel);
    clonedItem = (NMCommerceItem) itemManager.addAsSeparateItemToOrder(order, clonedItem);
    itemManager.removeItemQuantityFromShippingGroup(order, item.getId(), origShipGrpId, qty);
    itemManager.addItemQuantityToShippingGroup(order, clonedItem.getId(), shipGrpId, qty);
    
    return availQty - qty;
  }
  
  private long addRemainingGwpToShippingGroup(final NMOrderImpl order, final OrderManager orderMgr, final NMCommerceItem item, final long qty, final String shipGrpId, final String serviceLvl)
          throws CommerceException {
    if (qty < 1) {
      return 0L;
    }
    
    final NMOrderManager orderManager = (NMOrderManager) orderMgr;
    final CommerceItemManager itemManager = orderManager.getCommerceItemManager();
    final String origShipGrpId = getShippingGroupId(item);
    
    // if the remaining GWP quantity is already in this shipping group just update qty
    final String serviceLevel = serviceLvl;
    if (origShipGrpId.equals(shipGrpId) && serviceLevel.equalsIgnoreCase(item.getServicelevel())) {
      item.setQuantity(item.getQuantity() + qty);
      return 0L;
    }
    
    // look through the order to see if this shipping group already has this GWP item
    final List<NMCommerceItem> cartItems = order.getNmCommerceItems();
    for (final Iterator<NMCommerceItem> i = cartItems.iterator(); i.hasNext();) {
      final NMCommerceItem ci = i.next();
      final String ciSG = getShippingGroupId(ci);
      // SmartPostABTest : modifying the ci.getServiceLevel based on smartPost AB test group
      if (shipGrpId.equals(ciSG) && ci.equalCommerceItems(item, true) && ci.getServicelevel().equals(serviceLevel)) {
        ci.setQuantity(ci.getQuantity() + qty);
        itemManager.removeItemQuantityFromShippingGroup(order, item.getId(), origShipGrpId, qty);
        itemManager.addItemQuantityToShippingGroup(order, ci.getId(), shipGrpId, qty);
        ci.setServicelevel(serviceLevel);
        return 0L;
      }
    }
    
    // the shipping group assigned to receive the GWPs does not already have a GWP
    // commerce item so we will create a new one
    NMCommerceItem clonedItem = (NMCommerceItem) item.clone();
    clonedItem.setQuantity(qty);
    clonedItem.setServicelevel(serviceLevel);
    clonedItem = (NMCommerceItem) itemManager.addAsSeparateItemToOrder(order, clonedItem);
    itemManager.removeItemQuantityFromShippingGroup(order, item.getId(), origShipGrpId, qty);
    itemManager.addItemQuantityToShippingGroup(order, clonedItem.getId(), shipGrpId, qty);
    
    return 0L;
  }
  
  private double calcQualDollarsByShipGroup(final String promoKey, final Map<String, List<NMCommerceItem>> shipGrps, final Map<String, Double> qualShipGrps) {
    
    qualShipGrps.clear();
    
    final PromotionsHelper promoHelper = CommonComponentHelper.getPromotionsHelper();
    final IGiftWithPurchase gwp = getGwpPromotion(promoKey);
    if (null == gwp) {
      // WR29698 - Since promokey could be any of the other keys beside GWP/GWP Select
      // the logger.error would not apply, so commenting out.
      // logger.error("Unable to find GWP with promoKey " + promoKey);
      return 0.0;
    }
    
    if (!gwp.isGWPShipwithItemActive()) {
      return 0.0;
    }
    
    // loop through all the shipping groups and sum the qualifying dollars for each
    double totalQualDollars = 0.0;
    for (final Iterator<String> j = shipGrps.keySet().iterator(); j.hasNext();) {
      final String sgId = j.next();
      final List<NMCommerceItem> sgItems = shipGrps.get(sgId);
      for (final Iterator<NMCommerceItem> k = sgItems.iterator(); k.hasNext();) {
        final NMCommerceItem sgCi = k.next();
        if (promoHelper.itemQualifiesForPromo(sgCi, gwp)) {
          final String serviceLvl = sgCi.getServicelevel();
          final String sgslKey = buildShipGrpServiceLvlKey(sgId, serviceLvl);
          final double sgRelQualDollars = sgCi.getPriceInfo().getAmount();
          Double dollars = qualShipGrps.get(sgslKey);
          if (null == dollars) {
            dollars = new Double(sgRelQualDollars);
          } else {
            dollars = new Double(dollars.doubleValue() + sgRelQualDollars);
          }
          
          qualShipGrps.put(sgslKey, dollars);
          totalQualDollars += sgRelQualDollars;
        }
      }
    }
    return totalQualDollars;
  }
  
  private double calcQualQtyByShipGroup(final String promoKey, final Map<String, List<NMCommerceItem>> shipGrps, final Map<String, Double> qualShipGrps) {
    
    qualShipGrps.clear();
    final PromotionsHelper promoHelper = CommonComponentHelper.getPromotionsHelper();
    final IGiftWithPurchase gwp = getGwpPromotion(promoKey);
    if (null == gwp) {
      // WR29698 - Since promokey could be any of the other keys beside GWP/GWP Select
      // the logger.error would not apply, so commenting out.
      // logger.error("Unable to find GWP with promoKey " + promoKey);
      return 0.0;
    }
    
    if (!gwp.isGWPShipwithItemActive()) {
      return 0.0;
    }
    
    // loop through all the shipping groups and sum the qualifying quantity for each
    double totalQualQty = 0.0;
    for (final Iterator<String> j = shipGrps.keySet().iterator(); j.hasNext();) {
      final String sgId = j.next();
      final List<NMCommerceItem> sgItems = shipGrps.get(sgId);
      for (final Iterator<NMCommerceItem> k = sgItems.iterator(); k.hasNext();) {
        final NMCommerceItem sgCi = k.next();
        if (promoHelper.itemQualifiesForPromo(sgCi, gwp)) {
          final String serviceLvl = sgCi.getServicelevel();
          final String sgslKey = buildShipGrpServiceLvlKey(sgId, serviceLvl);
          final double sgRelQualQty = sgCi.getQuantity();
          Double qualQty = qualShipGrps.get(sgslKey);
          if (null == qualQty) {
            qualQty = new Double(sgRelQualQty);
          } else {
            qualQty = new Double(qualQty.doubleValue() + sgRelQualQty);
          }
          
          qualShipGrps.put(sgslKey, qualQty);
          totalQualQty += sgRelQualQty;
        }
      }
    }
    
    return totalQualQty;
  }
  
  public boolean updateLineItemStatus(final Order order, final OrderManager orderMgr) throws Exception {
    RepositoryItem sku = null;
    RepositoryItem product = null;
    final HashMap<String, Integer> skuStockQuantities = new HashMap<String, Integer>();
    
    synchronized (order) {
      final List<ShippingGroupCommerceItemRelationship> rShipList = getShippingGroupCommerceItemRelationships(order, orderMgr);
      final Iterator<ShippingGroupCommerceItemRelationship> rShipIterator = rShipList.iterator();
      
      while (rShipIterator.hasNext()) {
        final ShippingGroupCommerceItemRelationship rShip = rShipIterator.next();
        final NMCommerceItem nmci = (NMCommerceItem) rShip.getCommerceItem();
        sku = CommonComponentHelper.getProductRepository().getItem(nmci.getCatalogRefId(), "sku");
        product = CommonComponentHelper.getProdSkuUtil().getProductRepositoryItem(nmci.getCmosCatalogId(), nmci.getCmosItemCode());
        
        if (!skuStockQuantities.containsKey(sku.getRepositoryId())) {
          try {
            skuStockQuantities.put(sku.getRepositoryId(), new Integer((String) sku.getPropertyValue("stockLevel")));
          } catch (final Exception e) {
            skuStockQuantities.put(sku.getRepositoryId(), new Integer(0));
          }
        }
        
        final String quantity = Long.toString(nmci.getQuantity());
        int stockLevel = skuStockQuantities.get(sku.getRepositoryId()).intValue();
        final String statusString = CommonComponentHelper.getProdSkuUtil().getStatusString(product, sku, quantity, stockLevel);
        nmci.setTransientStatus(statusString);
        
        stockLevel = stockLevel - (int) nmci.getQuantity();
        skuStockQuantities.put(sku.getRepositoryId(), new Integer(stockLevel));
      }
    }
    
    return true;
  }
  
  public Collection<Message> validateOrder(final ShoppingCartHandler cart, final NMProfile profile, final NMProfileFormHandler profileHandler, final OrderInfo orderInfo) throws Exception {
    return validateOrder(cart, profile, profileHandler, orderInfo, false);
  }
  
  public Collection<Message>
          validateOrder(final ShoppingCartHandler cart, final NMProfile profile, final NMProfileFormHandler profileHandler, final OrderInfo orderInfo, final boolean isInternational) throws Exception {
    final List<Message> messages = new ArrayList<Message>();
    final NMOrderImpl order = cart.getNMOrder();
    final NMOrderManager orderMgr = (NMOrderManager) cart.getOrderManager();
    
    orderMgr.updateOrder(order);
    
    messages.addAll(performFinalBalanceCheck(order, orderMgr));
    messages.addAll(OrderShipUtil.getInstance().validateAddresses(order, profile));
    // Added the condition for Express PayPal and MasterPass
    if (!profileHandler.validateEmailAddress((String) profile.getPropertyValue("email")) && !order.isExpressPaypal() && !order.isMasterPassPayGroupOnOrder()) {
      messages.add(MessageDefs.getMessage(MessageDefs.MSG_InvalidEmailAddress));
    }
    
    if (messages.isEmpty()) {
      messages.addAll(OrderShipUtil.getInstance().validateShipping(cart, order, profile, isInternational));
    }
    
    if (messages.isEmpty()) {
      messages.addAll(InventoryUtil.getInstance().performFinalStockCheck(cart, order, orderMgr, profile));
    }
    
    if (messages.isEmpty()) {
      final NMCreditCard paymentGroup = order.getCreditCards().iterator().next();
      paymentGroup.setCidTransient(orderInfo.getCreditCardSecCode());
      
      order.setPropertyValue("FRAUDNET_BROWSER_DATA", orderInfo.getFraudnetBrowserData());
      
      // make sure that the billing address has been applied to all the payment groups.
      // Added the condition for Express PayPal and MasterPass
      if (!order.isExpressPaypal() && !order.isMasterPassPayGroupOnOrder()) {
        PaymentUtil.getInstance().updatePaymentGroupAddresses(cart, profile);
      }
      // CC auth for intl order will happen in BF
      if (!isInternational) {
        // Mask performCcAuth() for Alipay Orders
        final SystemSpecs systemSpecs = ComponentUtils.getInstance().getSystemSpecs();
        final LocalizationUtils localizationUtils = getLocalizationUtils();
        // Set Account KI for Alipay Order
        if (systemSpecs.isAlipayEnabled() && localizationUtils.findCountryCodeUsingEdgescape(getCurrentRequest()).equalsIgnoreCase(ShopRunnerConstants.CHINA_COUNTRY_CODE)) {
          setAccountKI(order, orderMgr);
        } else {
          messages.addAll(performCcAuth(profile, order, orderMgr));
        }
      }
    }
    
    orderMgr.updateOrder(order);
    
    return messages;
  }
  
  public void validateStoreSkuInventory(final NMOrderImpl order) throws CommerceException {
    
    final List<NMCommerceItem> items = order.getNmCommerceItems();
    // first mark the shipping groups as stores
    for (final NMCommerceItem item : items) {
      if (StringUtilities.isNotEmpty(item.getPickupStoreNo())) {
        final ItemAvailabilityLevel availabilityLevel =
                CommonComponentHelper.getStoreSkuInventoryUtils().getSkuInventoryByStoreNum(item.getSkuNumber(), item.getPickupStoreNo(), Long.toString(item.getQuantity()));
        if (availabilityLevel.isInStore()) {
          item.setShipToStoreFlg(false);
        } else {
          item.setShipToStoreFlg(true);
        }
      }
    }
    final OrderManager om = OrderManager.getOrderManager();
    om.updateOrder(order);
  }
  
  /**
   * This Method sets account Key Identifier to NMCreditCard's instance
   * 
   * @param order
   * @param orderMgr
   */
  private void setAccountKI(final NMOrderImpl order, final NMOrderManager orderMgr) {
    final NMPaymentGroupManager paymentGroupManager = (NMPaymentGroupManager) orderMgr.getPaymentGroupManager();
    final List<NMCreditCard> cards = paymentGroupManager.getPaymentGroups(order, new int[] {NMPaymentGroupManager.CREDIT_CARDS , NMPaymentGroupManager.PREPAID_CARDS});
    if (!cards.isEmpty() && !order.hasReachedFailedCCAttemptLimit()) {
      final Iterator<NMCreditCard> iterator = cards.iterator();
      while (iterator.hasNext()) {
        final NMCreditCard paymentGroup = iterator.next();
        String creditCardCode = null;
        if (!StringUtilities.isEmpty(paymentGroup.getCreditCardType())) {
          creditCardCode = NmoUtils.dynamoToCmosCreditCard(paymentGroup.getCreditCardType());
        }
        String accountKi = null;
        if (!StringUtilities.isEmpty(creditCardCode) && !StringUtilities.isEmpty(paymentGroup.getCreditCardNumber())) {
          accountKi = CheckoutComponents.getKeyIdentifierUtils(getCurrentRequest()).getKeyIdentifier(paymentGroup.getCreditCardNumber(), creditCardCode);
          if (StringUtilities.isNotEmpty(accountKi)) {
            paymentGroup.setAccountKI(accountKi);
          }
        }
      }
    }
  }
  
  private Collection<Message> performCcAuth(final NMProfile profile, final NMOrderImpl order, final NMOrderManager orderMgr) {
    final List<Message> messages = new ArrayList<Message>();
    
    final NMPaymentGroupManager paymentGroupManager = (NMPaymentGroupManager) orderMgr.getPaymentGroupManager();
    final List<NMCreditCard> cards = paymentGroupManager.getPaymentGroups(order, new int[] {NMPaymentGroupManager.CREDIT_CARDS , NMPaymentGroupManager.PREPAID_CARDS});
    
    if (!cards.isEmpty() && !order.hasReachedFailedCCAttemptLimit()) {
      final String cmosAuthCode = (String) order.getPropertyValue("ccAuthCode");
      
      if ((cmosAuthCode == null) || !cmosAuthCode.equals("-1")) {
        final Iterator<NMCreditCard> iterator = cards.iterator();
        final SystemSpecs systemSpecs = ComponentUtils.getInstance().getSystemSpecs();
        final String mode = systemSpecs.getEnvironmentType().substring(0, 1);
        final double amountRemaining = getCurrentOrderAmountRemaining(order, orderMgr);
        CCAuthContainer ccAuthConIn = null;
        int ccAuthCode = 0;
        
        boolean errorFound = false;
        while (iterator.hasNext() && !errorFound) {
          final NMCreditCard paymentGroup = iterator.next();
          String accountKi = null;
          if (!StringUtilities.isEmpty(paymentGroup.getCreditCardType()) && !StringUtilities.isEmpty(paymentGroup.getCreditCardNumber()) && !paymentGroup.isPayPal()) {
            // setting email & phone
            
            // MasterPass Changes starts here
            String email = null;
            if (!order.isMasterPassPayGroupOnOrder()) {
              email = ((String) profile.getPropertyValue("email")).toLowerCase();
            } else {
              // As email is not stored in profile in MasterPass Checkout, set Order's email property with CreditCard's email
              email = ((String) paymentGroup.getPropertyValue(ProfileProperties.Profile_email)).toLowerCase();
            }
            // replace xml special characters
            email = email.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("'", "&apos;").replace("\"", "&quot;");
            order.setPropertyValue("homeEmail", email);
            RepositoryItem billingAddr = null;
            if (!order.isMasterPassPayGroupOnOrder()) {
              billingAddr = (RepositoryItem) profile.getPropertyValue("billingAddress");
              order.setPropertyValue("homePhoneNumber", billingAddr.getPropertyValue("phoneNumber"));
            } else {
              // As Billing address is not stored in profile in MasterPass Checkout, set Order's Phone Number property with CreditCard's billing
              // address Phone number
              order.setPropertyValue("homePhoneNumber", ((NMRepositoryContactInfo) paymentGroup.getBillingAddress()).getPhoneNumber());
            }
            // MasterPass Changes ends here
            ccAuthConIn = new CCAuthContainer(order, paymentGroup, amountRemaining, mode);
            final VMeInfoVO vmeInfo = CheckoutComponents.getVMeInfoVO(getCurrentRequest());
            
            CCAuthContainer ccAuthConOut = null;
            
            try {
              // send cc message to cmos
              //final CmosStringComm cmosComm = ComponentUtils.getInstance().getCmosStringComm();
              //final Object o = cmosComm.communicate(ccAuthConIn);
              final Object o = cmosStringCommDiscoverable.callCmosCcAuth(ccAuthConIn);
              if ((o != null) && (o instanceof CCAuthContainer)) {
                // use cc message received
                ccAuthConOut = (CCAuthContainer) o;
                accountKi = ccAuthConOut.getAccountKI();
                final String paytransId = ccAuthConOut.getPayTransId();
                final String avsResponseCode = ccAuthConOut.getAvsResponseCode();
                if (StringUtilities.isNotEmpty(accountKi)) {
                  paymentGroup.setAccountKI(accountKi);
                }
                if (StringUtilities.isNotEmpty(paytransId)) {
                  vmeInfo.setPayTransId(paytransId);
                }
                if (StringUtilities.isNotEmpty(avsResponseCode)) {
                  vmeInfo.setAvsResponseCode(avsResponseCode);
                }
              } else {
                // unknown message receive, use the
                // message sent
                ccAuthConOut = ccAuthConIn;
              }
              
              ccAuthCode = ccAuthConOut.getAuthCode();
            } catch (final CmosStringComm.BadReturnError exception) {
              ccAuthCode = 0;
              CheckoutComponents.getConfig().logError("OrderUtils performCCAuth: Exception. " + exception.getMessage());
            } catch (final CmosStringComm.PreconditionError exception) {
              ccAuthCode = 0;
              CheckoutComponents.getConfig().logError("OrderUtils performCCAuth: Exception. " + exception.getMessage());
            } catch (final IOException exception) {
              ccAuthCode = 0;
              CheckoutComponents.getConfig().logError("OrderUtils performCCAuth: Exception. " + exception.getMessage());
            } catch (final Exception e) {
              // there was a problem so let the order flow
              // to CMOS
              ccAuthCode = 0;
              CheckoutComponents.getConfig().logError("OrderUtils performCCAuth: Exception. ", e);
            }
            
            order.setPropertyValue("ccAuthCode", String.valueOf(ccAuthCode));
            // vme changes - setting ccAuthCode to vmeInfoVo session object
            vmeInfo.setCcAuthCode(String.valueOf(ccAuthCode));
            switch (ccAuthCode) {
              case 1: {
                final Message msg = MessageDefs.getMessage(MessageDefs.MSG_CCAuthMessage1);
                messages.add(msg);
                errorFound = true;
                break;
              }
              case 2: {
                final Message msg = MessageDefs.getMessage(MessageDefs.MSG_CCAuthMessage2);
                messages.add(msg);
                errorFound = true;
                break;
              }
              case 3: {
                final Message msg = MessageDefs.getMessage(MessageDefs.MSG_CCAuthMessage3);
                messages.add(msg);
                errorFound = true;
                break;
              }
              default:
              break;
            }
          }
        }
      }
    }
    
    return messages;
  }
  
  public Collection<Message> performFinalBalanceCheck(final NMOrderImpl order, final NMOrderManager orderMgr) {
    final List<Message> messages = new ArrayList<Message>();
    final double amtRemaining = orderMgr.getCurrentOrderAmountRemaining(order);
    
    if (amtRemaining > 0.0d) {
      final List<NMCreditCard> creditCards = order.getCreditCards();
      
      for (final NMCreditCard creditCard : creditCards) {
        if (!StringUtilities.isEmpty(creditCard.getCreditCardNumber()) || creditCard.isPayPal()) {
          return messages;
        }
      }
      
      final Message msg = MessageDefs.getMessage(MessageDefs.MSG_MultiTender_BalanceRemaining);
      msg.setFieldId("groupTop");
      messages.add(msg);
    }
    
    return messages;
  }
  
  public void submitOrder(final ShoppingCartHandler cart, final NMProfile profile, final NMProfileFormHandler profileHandler, final OrderInfo orderInfo) throws Exception {
    final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
    final NMOrderImpl order = cart.getNMOrder();
    final NMOrderManager orderMgr = (NMOrderManager) cart.getOrderManager();
    
    //added to persist line item level estimated delivery dates if enabled 
    final EstimatedDeliveryDateConfig estimatedDeliveryDateConfig = CommonComponentHelper.getEstimatedDeliveryDateConfig();
    final boolean persistLineItemEstimatedDeliveryDate = estimatedDeliveryDateConfig.isPersistLineItemEstimatedDeliveryDateEnabled();
    if (CheckoutComponents.getConfig().isLoggingDebug()) {
      CheckoutComponents.getConfig().logDebug("Persisting commerce item level estimated delivery date enabled flag is: " + persistLineItemEstimatedDeliveryDate);
    }
    
    for (final NMCommerceItem item : order.getOrderItems()) {
      orderInfo.getPcsSessionData().updateProductsPurchased(item.getProductId().trim());
      
      //added to persist line item level estimated delivery dates if enabled
      if(persistLineItemEstimatedDeliveryDate) {
        if (CheckoutComponents.getConfig().isLoggingDebug()) {
    	  CheckoutComponents.getConfig().logDebug("Persisting estimated delivery date for item: " + item.getId());
    	}
        item.persistCmosEstimatedDeliveryDate();
      }
    }
    
    order.setPropertyValue("fraudnetDate", dateFormatter.format(new Date()));
    
    final NMCreditCard paymentGroup = order.getCreditCards().iterator().next();
    paymentGroup.setCidTransient(orderInfo.getCreditCardSecCode());
    
    // make sure that the billing address has been applied to all the payment groups.
    PaymentUtil.getInstance().updatePaymentGroupAddresses(cart, profile);
    
    // apply the store retail price, if the items on the order qualify
    processStoreRetailPrice(cart, profile);
    
    final BingSessionData bingSessionData = orderInfo.getBingSessionData();
    if (bingSessionData.isBingRequest()) {
      String bingTransId = null;
      
      if (bingSessionData.isBingSearch()) {
        bingTransId = BingUtils.BING_CASHBACK_SEARCH_PREFIX + bingSessionData.getBingTransactionId();
      } else if (bingSessionData.isBingShopping()) {
        bingTransId = BingUtils.BING_CASHBACK_SHOPPING_PREFIX + bingSessionData.getBingTransactionId();
      }
      
      order.setPropertyValue("bingTransactionId", bingTransId);
    }
    
    placeOrder(cart, profile);
    moveOrderToConfirmation(cart, profile, profileHandler, orderInfo);
    
    setOrderFraudFields(cart, orderInfo);
    final DynamoHttpServletRequest request = getCurrentRequest();
    order.setPropertyValue(INMGenericConstants.INCIRCLE_EARNED_LEVEL, String.valueOf(request.getSession().getAttribute(INMGenericConstants.INCIRCLE_EARNED_LEVEL)));
    order.setPropertyValue(INMGenericConstants.INCIRCLE_BONUS_POINTS, String.valueOf(request.getSession().getAttribute(INMGenericConstants.INCIRCLE_BONUS_POINTS)));
    setVendorInfoToOrder(orderInfo, order, request);
    moveToOrderCommit(cart, profile, orderInfo, request);
    updatePersonalizationStatsForOrder(profile, order);
    
    updatePromoEmailValidationUseCount(order);
    
    updateOrderReportTable(order);
    EmailUtil.getInstance().sendPapEmail(order);
    
    final NMCreditCard profileCard = profile.getCreditCardByNumber(paymentGroup.getCreditCardNumber());
    if (profileCard != null) {
      profileCard.setCidAuthorized(true);
    }
    
    PaymentUtil.getInstance().clearOutPaymentGroups(order);
    orderInfo.getGiftCardHolder().reset();
    
    // OmnitureUtils.getInstance().orderComplete(checkoutResp, profile, order);
    // Setting Associate Pin and Hash to NMO order
    if (employeeDiscountsUtil.isAssociateLoggedIn(profile)) {
      final String encAssociateId = (String) profile.getPropertyValue(EmployeeDiscountConstants.ASSOCIATE_HASH);
      final String encAssociatePin = (String) profile.getPropertyValue(EmployeeDiscountConstants.ASSOCIATE_PIN);
      if (!StringUtils.isBlank(encAssociateId) && !StringUtils.isBlank(encAssociatePin)) {
        order.setPropertyValue("encAssociateId", encAssociateId);
        order.setPropertyValue("encAssociatePin", encAssociatePin);
      }
    }
    orderMgr.updateOrder(order);
    
    LinkedEmailUtils.getInstance().removeLinkedEmailOrder(order.getId());
    
    try {
      // orderUtils.sendOrderConfirmationEmail(profile, order, this);
      // wr37811 biz asked to turn off order ack emails
      EmailUtil.getInstance().sendSubscriptionConfEmail(cart, profile);
    } catch (final Exception e) {
      CheckoutComponents.getConfig().logError("***** PaymentService.submitOrder ***** Error sending email");
    }
    // clearing the vme session info object once the order is placed.
    clearVmeInfoVo();
    
    // SmartPost : clearing the user selected service level stored in SmartPostServiceLevelSessionBean once the order is placed for smart post functionality
    clearSmartPostServiceLevelSessionBean();
    
    // MasterPass Changes : clearing the MasterPass session info object once the order is placed.
    clearMasterPassInfoVo();
    
    if ((request.getSession().getAttribute(AssociatePinUtil.ASSOCIATE_MODE) != null) && (Boolean) request.getSession().getAttribute(AssociatePinUtil.ASSOCIATE_MODE)) {
      request.getSession().setAttribute(AssociatePinUtil.ASSOCIATE_CHECKOUT_COMPLETE, true);
    }
  }
  
  /**
   * Clearing the vmeInfoVO session object. Setting the shipping and payment details in vmeInfoVo to null once the order is placed
   */
  private void clearVmeInfoVo() {
    final VMeInfoVO vmeInfo = CheckoutComponents.getVMeInfoVO(getCurrentRequest());
    vmeInfo.setPaymentDetails(null);
    vmeInfo.setShippingInfo(null);
  }
  
  /**
   * <p>
   * SmartPost : clearing user selected service level code from the SmartPostServiceLevelBean
   * <p>
   */
  private void clearSmartPostServiceLevelSessionBean() {
    final SmartPostServiceLevelSessionBean smartPostServiceLevelSessionBean = CheckoutComponents.getSmartPostServiceLevelSessionBean(getCurrentRequest());
    smartPostServiceLevelSessionBean.setSmartPostSelectedServiceLevel(INMGenericConstants.EMPTY_STRING);
  }
  
  /**
   * Clearing the MasterPassInfoVO session object. Setting the ShoppingCart and Checkout service response details in MasterPassInfoVO to null once the order is placed
   */
  private void clearMasterPassInfoVo() {
    final MasterPassInfoVO masterPassInfoVO = CheckoutComponents.getMasterPassInfoVO(getCurrentRequest());
    masterPassInfoVO.setMasterPassShoppingCartResponseVO(null);
    masterPassInfoVO.setMasterPassCheckoutResponseVO(null);
  }
  
  /**
   * This method will get the commerce items from order and iterates over them and removes the replenishable option for the replenishable items in the cart.
   * 
   * @param order
   *          Order object
   * @param orderManager
   *          OrderManager object
   * @throws CommerceException
   *           possible exception while editing/updating order
   */
  public void removeReplenishmentOptionFromOrder(final NMOrderImpl order, final NMOrderManager orderManager) throws CommerceException {
    final List<NMCommerceItem> commerceItems = order.getNmCommerceItems();
    boolean orderUpdated = false;
    for (final NMCommerceItem commerceItem : commerceItems) {
      if (!StringUtils.isBlank(commerceItem.getSelectedInterval())) {
        commerceItem.setSelectedInterval(StringUtilities.EMPTY);
        orderUpdated = true;
      }
    }
    if (orderUpdated) {
      orderManager.updateOrder(order);
    }
  }
  
  /**
   * Applies the store retail price to each item on the order if it's lower than the retail price and the order is Ship from Store or Pickup in Store (The Shared Inventory project)
   * 
   * @author nmve1
   * @param order
   * @param orderMgr
   * @param profile
   * @param service
   */
  private void processStoreRetailPrice(final ShoppingCartHandler cart, final NMProfile profile) {
    final NMOrderImpl order = cart.getNMOrder();
    final NMOrderManager orderMgr = (NMOrderManager) cart.getOrderManager();
    NMCommerceItem item = null;
    NMItemPriceInfo itemPrice = null;
    
    @SuppressWarnings("unchecked")
    final Iterator<NMCommerceItem> iter = order.getCommerceItems().iterator();
    
    while (iter.hasNext()) {
      item = iter.next();
      itemPrice = (NMItemPriceInfo) item.getPriceInfo();
      
      // check whether this is a ship from store or a pick up in store item
      if ((item.isPickupInStore() || item.isShipFromStore()) && (item.getStoreRetailPrice() > 0.0) && ((item.getStoreRetailPrice() * item.getQuantity()) < item.getRawTotalPrice())) {
        
        itemPrice.setStoreRetailPriceDiscount(item.getRawTotalPrice() - (item.getStoreRetailPrice() * item.getQuantity()));
        
        try {
          // bypass promotion evaluation
          order.setBypassPromoEvaluation(true);
          
          // recalculate only the percent off promo discount to use
          // the store retail price
          boolean isPercentOff = false;
          double newDollarDiscount = 0.0;
          final Map<String, Markdown> markdowns = item.getItemMarkdowns();
          if (markdowns != null) {
            final Iterator<String> i = markdowns.keySet().iterator();
            while (i.hasNext()) {
              final String key = i.next();
              final Markdown markdown = markdowns.get(key);
              if ((markdown != null) && (markdown.getType() == Markdown.PERCENT_OFF)) {
                isPercentOff = true;
                newDollarDiscount = (item.getStoreRetailPrice() * item.getQuantity() * new Double(markdown.getPercentDiscount())) / 100;
                markdown.setDollarDiscount(newDollarDiscount);
                markdowns.put(key, markdown);
              }
            }
          }
          
          // modify the amount only for the percent off promo
          if (isPercentOff) {
            itemPrice.setAmount((item.getRawTotalPrice() - newDollarDiscount) + itemPrice.getStoreRetailPriceDiscount());
          }
          
          itemPrice.setDiscounted(true);
          itemPrice.setPromotionsApplied(markdowns);
          itemPrice.setOrderDiscountShare(newDollarDiscount);
          itemPrice.markAsFinal("Store Retail Price Adjustment");
          item.setPriceInfo(itemPrice);
          
          // update the order
          orderMgr.updateOrder(order);
          
          // reprice the order
          PricingUtil.getInstance().repriceOrder(cart, profile, SnapshotUtil.STOCK_CHECK);
        } catch (final CommerceException ce) {
          CommonComponentHelper.getLogger().error("Error while updating the order (processStoreRetailPrice) ", ce);
        } catch (final Exception e) {
          CommonComponentHelper.getLogger().error("Error while repricing the order (processStoreRetailPrice) ", e);
        }
      }
    }
  }
  
  private void placeOrder(final ShoppingCartHandler cart, final NMProfile profile) throws Exception {
    final NMOrderImpl order = cart.getNMOrder();
    final NMOrderManager orderMgr = (NMOrderManager) cart.getOrderManager();
    final double orderAmountRemaining = getCurrentOrderAmountRemaining(order, orderMgr);
    
    if (orderAmountRemaining > 0.0d) {
      final List<NMCreditCard> creditCards = order.getCreditCards();
      if (creditCards.size() > 0) {
        final NMCreditCard paymentGroup = creditCards.get(0);
        orderMgr.addOrderAmountToPaymentGroup(order, paymentGroup.getId(), orderAmountRemaining);
        orderMgr.updateOrder(order);
      }
    }
    
    ((MutableRepositoryItem) profile).setPropertyValue("lastPurchase", new Date());
    
    // this is being put in to initialize the wish list that a user has viewed
    // it is transient but is causing some problems in uat so this is a temp fix.
    ((MutableRepositoryItem) profile).setPropertyValue(ProfileRepository.VIEWED_REGISTRIES_PROP, null);
  }
  
  public void moveOrderToConfirmation(final ShoppingCartHandler cart, final NMProfile profile, final NMProfileFormHandler profileHandler, final OrderInfo orderInfo) throws Exception {
    final NMOrderImpl order = cart.getNMOrder();
    final NMOrderManager orderMgr = (NMOrderManager) cart.getOrderManager();
    final PricingModelHolder upm = cart.getUserPricingModels();
    
    final Configuration siteConfig = (Configuration) Nucleus.getGlobalNucleus().resolveName("/atg/dynamo/Configuration");
    CommonComponentHelper.getLogger().info(
            "Submitted Order #--->" + order.getId() + "---profile--->" + profile.getRepositoryId() + "---order total-->" + order.getPriceInfo().getTotal() + "---Server-->"
                    + siteConfig.getSiteHttpServerName() + "_" + siteConfig.getSiteHttpServerPort() + "<----");
    cart.runProcessMoveToConfirmation(profile, order, upm, orderInfo.getUserLocale());
    
    // following code from ShoppingCartHandler.postMoveToConfirmation
    final List<NMCreditCard> creditCards = order.getCreditCards();
    NMCreditCard creditCard = null;
    if ((creditCards.size() > 0)) {
      creditCard = creditCards.iterator().next();
      final NMRepositoryContactInfo billAddr = (NMRepositoryContactInfo) creditCard.getBillingAddress();
      final RepositoryItem profBillAddr = (RepositoryItem) profile.getPropertyValue(ProfileProperties.Profile_billingAddress);
      // MasterPass Changes Starts here : Profile doesn't contain any address stored in MasterPass Checkout. Thus, skipping the below step for MasterPass
      if (!order.isMasterPassPayGroupOnOrder()) {
        OrderTools.copyAddress(profBillAddr, billAddr);
      }
    }
    if (!order.isMasterPassPayGroupOnOrder()) {
      if (getCurrentOrderAmountRemaining(order, orderMgr) == 0.0d) {
        // some form of payment was made that didn't require a credit card.
        // Since this payment type covered the
        // entire balance and a credit card wasn't needed we will not
        // override the existing value of the credit card
        // in the profile.
      } else if (creditCard != null) {
        final RepositoryItemImpl defaultCreditCard = (RepositoryItemImpl) profile.getPropertyValue(ProfileProperties.Profile_defaultCreditCard);
        
        if (defaultCreditCard == null) {
          final MutableRepositoryItem cc = orderMgr.getOrderTools().getProfileTools().createCreditCardItem(profile);
          cc.setPropertyValue("creditCardNumber", EncryptDecrypt.encrypt(creditCard.getCreditCardNumber()));
          cc.setPropertyValue("creditCardType", creditCard.getCreditCardType());
          cc.setPropertyValue("expirationMonth", EncryptDecrypt.encrypt(creditCard.getExpirationMonth()));
          cc.setPropertyValue("expirationYear", EncryptDecrypt.encrypt(creditCard.getExpirationYear()));
          profile.setPropertyValue("defaultCreditCard", cc);
        }
      }
    }
    
    final RepositoryItem billingAddr = (RepositoryItem) profile.getPropertyValue("billingAddress");
    
    String email = null;
    if (!order.isMasterPassPayGroupOnOrder()) {
      email = ((String) profile.getPropertyValue("email")).toLowerCase();
    } else if (null != creditCard) {
      email = ((String) creditCard.getPropertyValue(ProfileProperties.Profile_email)).toLowerCase();
    }
    
    order.setPropertyValue("homeEmail", email);
    
    if (profile.isAuthorized()) {
      order.setPropertyValue("homeUserName", profile.getRepositoryId());
    } else {
      order.setPropertyValue("homeUserName", null);
    }
    
    // mReceiveEmail is a property of the NMProfileFormHandler
    final String receiveEmail = (String) profile.getPropertyValue(ProfileProperties.Profile_receiveEmail);
    
    // setting the receiveEmail property on billpayment page to true, &
    // updating/adding items in email registrar table.
    // The table entry should not be put for Alipay and Shoprunner express checkout orders.
    // Marketing mail will be handled by CMOS since they are receving the mkt opt-in flag in order
    if (!(orderInfo.isSRExpressCheckOut() || orderInfo.isAlipayExpressCheckout())) {
      if ((receiveEmail != null) && receiveEmail.equalsIgnoreCase("yes")) {
        profileHandler.addOrUpdateEmailRegistrar(getCurrentRequest()); // added for personalization 2005 & 2006 project
      }
    }
    
    // vsrb1 End Change for shopping cart persistence
    if ("yes".equalsIgnoreCase(receiveEmail)) {
      order.setPropertyValue("flgSpam", Boolean.TRUE);
    } else {
      order.setPropertyValue("flgSpam", Boolean.FALSE);
    }
    
    order.setPropertyValue("mktOptIn", new Boolean(orderInfo.isMktOptIn()));
    order.setPropertyValue("cmosCustId", ((RepositoryItem) profile).getPropertyValue("cmosCustID"));
    if (!order.isMasterPassPayGroupOnOrder()) {
      order.setPropertyValue("homeFirstName", billingAddr.getPropertyValue("firstName"));
      order.setPropertyValue("homeMiddleName", billingAddr.getPropertyValue("middleName"));
      order.setPropertyValue("homeLastName", billingAddr.getPropertyValue("lastName"));
      order.setPropertyValue("homeAddress1", billingAddr.getPropertyValue("address1"));
      order.setPropertyValue("homeAddress2", billingAddr.getPropertyValue("address2"));
      order.setPropertyValue("homeCity", billingAddr.getPropertyValue("city"));
      order.setPropertyValue("homeState", billingAddr.getPropertyValue("state"));
      order.setPropertyValue("homeCountry", billingAddr.getPropertyValue("country"));
      order.setPropertyValue("homeProvince", billingAddr.getPropertyValue("lastName"));
      order.setPropertyValue("homePostalCode", billingAddr.getPropertyValue("postalCode"));
      order.setPropertyValue("phoneType", billingAddr.getPropertyValue("phoneType"));
      order.setPropertyValue("homePhoneNumber", billingAddr.getPropertyValue("phoneNumber"));
      order.setPropertyValue("homeFaxNumber", billingAddr.getPropertyValue("faxNumber"));
      order.setPropertyValue("homeSuffixCode", billingAddr.getPropertyValue("suffixCode"));
      order.setPropertyValue("homeTitleCode", billingAddr.getPropertyValue("titleCode"));
      order.setPropertyValue("homeFirmName", billingAddr.getPropertyValue("firmName"));
      order.setPropertyValue("homeAddress3", billingAddr.getPropertyValue("address3"));
      order.setPropertyValue("homeDayPhoneExt", billingAddr.getPropertyValue("dayPhoneExt"));
      order.setPropertyValue("homeEvePhoneNumber", billingAddr.getPropertyValue("evePhoneNumber"));
      order.setPropertyValue("homeEvePhoneExt", billingAddr.getPropertyValue("evePhoneExt"));
      order.setPropertyValue("verificationFlag", billingAddr.getPropertyValue("verificationFlag"));
      order.setPropertyValue("addressType", billingAddr.getPropertyValue("addressType"));
      order.setPropertyValue("countyCode", billingAddr.getPropertyValue("countyCode"));
      order.setPropertyValue("systemCode", order.getPropertyValue("systemCode"));
      order.setPropertyValue("geoCodeLatitude", billingAddr.getPropertyValue("geoCodeLatitude"));
      order.setPropertyValue("geoCodeLongitude", billingAddr.getPropertyValue("geoCodeLongitude"));
      order.setPropertyValue("geoCodeTaxKey", billingAddr.getPropertyValue("geoCodeTaxKey"));
      order.setPropertyValue("geoCodeRefreshFlag", billingAddr.getPropertyValue("geoCodeRefreshFlag"));
      order.setPropertyValue("lastGeoCodeReqDate", billingAddr.getPropertyValue("lastGeoCodeReqDate"));
    } else if (null != creditCard) {
      final NMRepositoryContactInfo billAddress = (NMRepositoryContactInfo) creditCard.getBillingAddress();
      // Set Order billing properies with MasterPass Card billing address values
      setOrderProperties(order, billAddress);
    }// MasterPass Changes ends here
    
    // fin Cen Gift Card Info
    if (!order.validateFinCen()) {
      // fin Cen Gift Card Info
      final String birthDate = (String) order.getPropertyValue("birthDate");
      final String[] bArray = birthDate.split("/");
      final int bMonth = Integer.parseInt(bArray[0]) - 1;
      final int bDay = Integer.parseInt(bArray[1]);
      final int bYear = Integer.parseInt(bArray[2]);
      
      final Calendar dateOfBirth = Calendar.getInstance();
      
      dateOfBirth.set(bYear, bMonth, bDay, 0, 0, 0);
      order.setPropertyValue("dateOfBirth", dateOfBirth.getTime());
      order.setPropertyValue("issueIdType", order.getPropertyValue("issueType"));
      order.setPropertyValue("issueIdOrigin", order.getPropertyValue("issueOrigin"));
      order.setPropertyValue("issueIdNumber", order.getPropertyValue("issueId"));
    }
    
    // FIXME where does kiosk id come from?
    // if( getKioskId() != null && !getKioskId().trim().equals("") ) {
    // order.setPropertyValue("kioskId", getKioskId());
    // order.setPropertyValue("pinNumber", getPinNumber());
    // }
    
    final CheckoutConfig config = CheckoutComponents.getConfig();
    if (((String) profile.getPropertyValue("userCode")).equalsIgnoreCase(config.getTestCheckoutUserType())) {
      order.setPropertyValue("systemCode", config.getSystemCodeForTestUser());
    }
    
    // nmkec remove the profile id for all incomplete orders in the
    // transient profile to ensure they do not appear in shopping cart
    RepositoryItem unRegisteredProfileRI = null;
    try {
      unRegisteredProfileRI = (RepositoryItem) profile.getPropertyValue("unregisteredProfile");
    } catch (final IllegalArgumentException ie) {
      // UnRegistered Profile does not exisit
    }
    
    if (unRegisteredProfileRI != null) {
      final String unRegisterProfile = unRegisteredProfileRI.getRepositoryId();
      final OrderQueries oq = orderMgr.getOrderQueries();
      @SuppressWarnings("unchecked")
      final List<OrderImpl> transientOrderIds = oq.getOrdersForProfileInState(unRegisterProfile, StateDefinitions.ORDERSTATES.getStateValue(OrderStates.INCOMPLETE));
      
      if ((transientOrderIds != null) && (transientOrderIds.size() > 0)) {
        for (int i = 0; i < transientOrderIds.size(); i++) {
          transientOrderIds.get(i).setPropertyValue("profileId", null);
        }
      }
    }
    
    // nmkec Validate the profile Id matches the id in the order, if not
    // update the order
    final String profileId = profile.getRepositoryId();
    final String orderProfileId = (String) order.getPropertyValue("profileId");
    
    if ((orderProfileId == null) || !orderProfileId.equals(profileId)) {
      order.setPropertyValue("profileId", profileId);
    }
    
    order.incOutstandingEvents();
  }
  
  /**
   * This method sets MasterPass Card billing address to Order
   * 
   * @param order
   * @param billAddress
   */
  private void setOrderProperties(final NMOrderImpl order, final NMRepositoryContactInfo billAddress) {
    order.setPropertyValue("homeFirstName", billAddress.getFirstName());
    order.setPropertyValue("homeMiddleName", billAddress.getMiddleName());
    order.setPropertyValue("homeLastName", billAddress.getLastName());
    order.setPropertyValue("homeAddress1", billAddress.getAddress1());
    order.setPropertyValue("homeAddress2", billAddress.getAddress2());
    order.setPropertyValue("homeCity", billAddress.getCity());
    order.setPropertyValue("homeState", billAddress.getState());
    order.setPropertyValue("homeCountry", billAddress.getCountry());
    order.setPropertyValue("homeProvince", billAddress.getProvince());
    order.setPropertyValue("homePostalCode", billAddress.getPostalCode());
    order.setPropertyValue("phoneType", billAddress.getPhoneType());
    order.setPropertyValue("homePhoneNumber", billAddress.getPhoneNumber());
    order.setPropertyValue("homeFaxNumber", billAddress.getFaxNumber());
    order.setPropertyValue("homeSuffixCode", billAddress.getSuffixCode());
    order.setPropertyValue("homeTitleCode", billAddress.getTitleCode());
    order.setPropertyValue("homeFirmName", billAddress.getFirmName());
    order.setPropertyValue("homeAddress3", billAddress.getAddress3());
    order.setPropertyValue("homeDayPhoneExt", billAddress.getDayPhoneExt());
    order.setPropertyValue("homeEvePhoneNumber", billAddress.getEvePhoneNumber());
    order.setPropertyValue("homeEvePhoneExt", billAddress.getEvePhoneExt());
    order.setPropertyValue("verificationFlag", billAddress.getVerificationFlag());
    order.setPropertyValue("addressType", billAddress.getAddressType());
    order.setPropertyValue("countyCode", billAddress.getCountyCode());
    order.setPropertyValue("systemCode", order.getPropertyValue("systemCode"));
    order.setPropertyValue("geoCodeLatitude", billAddress.getGeoCodeLatitude());
    order.setPropertyValue("geoCodeLongitude", billAddress.getGeoCodeLongitude());
    order.setPropertyValue("geoCodeTaxKey", billAddress.getGeoCodeTaxKey());
    order.setPropertyValue("geoCodeRefreshFlag", billAddress.getGeoCodeRefreshFlag());
    order.setPropertyValue("lastGeoCodeReqDate", billAddress.getLastGeoCodeReqDate());
  }
  
  private void setOrderFraudFields(final ShoppingCartHandler cart, final OrderInfo orderInfo) {
    final NMOrderImpl order = cart.getNMOrder();
    final NMOrderHolder orderHolder = (NMOrderHolder) cart.getOrderHolder();
    final DynamoHttpServletRequest request = getCurrentRequest();
    
    order.setPropertyValue("requestContentType", NmoUtils.invalidCharacterCleanup(request.getContentType()));
    order.setPropertyValue("protocol", NmoUtils.invalidCharacterCleanup(request.getProtocol()));
    
    if ((request.getHeader("nmcip") != null) && !"".equals(request.getHeader("nmcip"))) {
      order.setPropertyValue("remoteAddr", NmoUtils.invalidCharacterCleanup(request.getHeader("nmcip")));
      order.setPropertyValue("remoteHost", NmoUtils.invalidCharacterCleanup(request.getHeader("nmcip")));
    } else {
      order.setPropertyValue("remoteAddr", NmoUtils.invalidCharacterCleanup(request.getRemoteAddr()));
      order.setPropertyValue("remoteHost", NmoUtils.invalidCharacterCleanup(request.getRemoteHost()));
    }
    
    order.setPropertyValue("method", NmoUtils.invalidCharacterCleanup(request.getMethod()));
    order.setPropertyValue("requestedUrl", NmoUtils.invalidCharacterCleanup(request.getRequestURI()));
    order.setPropertyValue("queryString", NmoUtils.invalidCharacterCleanup(request.getQueryString()));
    order.setPropertyValue("remoteUser", NmoUtils.invalidCharacterCleanup(request.getRemoteUser()));
    order.setPropertyValue("authorityType", NmoUtils.invalidCharacterCleanup(request.getAuthType()));
    order.setPropertyValue("httpUserAgent", NmoUtils.invalidCharacterCleanup(createHeaderFragment(request, "USER-AGENT")));
    order.setPropertyValue("httpHost", NmoUtils.invalidCharacterCleanup(createHeaderFragment(request, "HOST")));
    order.setPropertyValue("httpAccept", NmoUtils.invalidCharacterCleanup(createHeaderFragment(request, "ACCEPT")));
    order.setPropertyValue("httpAcceptCharset", NmoUtils.invalidCharacterCleanup(createHeaderFragment(request, "ACCEPT-CHARSET")));
    order.setPropertyValue("httpAcceptEncoding", NmoUtils.invalidCharacterCleanup(createHeaderFragment(request, "ACCEPT-ENCODING")));
    order.setPropertyValue("httpAcceptLanguage", NmoUtils.invalidCharacterCleanup(createHeaderFragment(request, "ACCEPT-LANGUAGE")));
    order.setPropertyValue("siteHttpServerName", NmoUtils.invalidCharacterCleanup(EnvironmentUtils.getHostName()));
    order.setPropertyValue("httpCookie", NmoUtils.invalidCharacterCleanup(request.getHeader("COOKIE")));
    order.setPropertyValue("httpReferer", NmoUtils.invalidCharacterCleanup(orderHolder.getUserReferer()));
    order.setPropertyValue("browserDateAndTime", NmoUtils.invalidCharacterCleanup(orderInfo.getClientDateString()));
    order.setPropertyValue("browserTimeZone", NmoUtils.invalidCharacterCleanup(orderInfo.getClientTimeZone()));
    order.setPropertyValue("FRAUDNET_BROWSER_DATA", NmoUtils.invalidCharacterCleanup(orderInfo.getFraudnetBrowserData()));
    order.setPropertyValue("reporting_code", NmoUtils.invalidCharacterCleanup(orderInfo.getOriginationCode()));
    
    String itemsRemoved = "n";
    String timeOnSite = "0";
    final HttpSession session = request.getSession();
    if (session != null) {
      final Object o = session.getAttribute("itemsRemoved");
      if ((o != null) && (o instanceof String)) {
        itemsRemoved = (String) o;
      }
      session.removeAttribute("itemsRemoved");
      try {
        final long tosMillis = System.currentTimeMillis() - session.getCreationTime();
        timeOnSite = "" + tosMillis;
      } catch (final IllegalStateException ise) {
        // Session has been invalidated
      }
    }
    order.setPropertyValue("itemsRemoved", itemsRemoved);
    order.setPropertyValue("timeOnSite", timeOnSite);
    
    String lossPreventionInfo = getLossPreventionInfo(request, order);
    
    if (lossPreventionInfo.length() > 2048) {
      lossPreventionInfo = lossPreventionInfo.substring(0, 2048);
    }
    
    order.setPropertyValue("lossPrevInfo", lossPreventionInfo);
    
    // WR-32612 Adding all HTTP Headers as fraudnet fields
    final StringBuffer buf = new StringBuffer();
    @SuppressWarnings("rawtypes")
    final Enumeration headerNames = request.getHeaderNames();
    if (headerNames != null) {
      while (headerNames.hasMoreElements()) {
        final String headerName = (String) headerNames.nextElement();
        if (headerName != null) {
          final Object headerValue = request.getHeader(headerName);
          if (headerValue != null) {
            if (buf.length() > 0) {
              buf.append("||");
            }
            buf.append(headerName).append("|").append(NmoUtils.invalidCharacterCleanup(headerValue.toString()));
          }
        }
      }
    }
    
    order.setPropertyValue("httpHeaders", buf.toString());
  }
  
  private void moveToOrderCommit(final ShoppingCartHandler cart, final NMProfile profile, final OrderInfo orderInfo, final DynamoHttpServletRequest request) throws Exception {
    final NMOrderImpl order = cart.getNMOrder();
    final NMOrderManager orderMgr = (NMOrderManager) cart.getOrderManager();
    
    profile.setPropertyValue("flgFreeBaseShipping", new Boolean(false));
    
    if ((String) order.getPropertyValue("ccAppStatusTransient") != null) {
      order.setPropertyValue("ccAppStatus", order.getPropertyValue("ccAppStatusTransient"));
    } else {
      order.setPropertyValue("ccAppStatus", "");
    }
    
    // To be used for the CID project, giftcards will never have their CID
    // stored to the database but credit cards will
    // then the loader will remove them when the order is sent to CMOS
    // that is the plan anyway. - uncommented LAJ
    @SuppressWarnings("unchecked")
    final List<NMCreditCard> paymentGroupList = order.getPaymentGroups();
    final Iterator<NMCreditCard> paymentGroupIterator = paymentGroupList.iterator();
    while (paymentGroupIterator.hasNext()) {
      final NMCreditCard paymentGroup = paymentGroupIterator.next();
      String cidTransient = paymentGroup.getCidTransient();
      
      if (cidTransient != null) {
        cidTransient = cidTransient.trim();
      }
      
      paymentGroup.setCid(cidTransient);
    }
    
    final List<ShippingGroupCommerceItemRelationship> rShipList = getShippingGroupCommerceItemRelationships(order, orderMgr);
    final Iterator<ShippingGroupCommerceItemRelationship> rShipIterator = rShipList.iterator();
    
    while (rShipIterator.hasNext()) {
      final ShippingGroupCommerceItemRelationship rShip = rShipIterator.next();
      final HardgoodShippingGroup shippingGroup = (HardgoodShippingGroup) rShip.getShippingGroup();
      final NMCommerceItem cItem = (NMCommerceItem) rShip.getCommerceItem();
      
      if ((cItem.getRegistryId() != null) && !cItem.getRegistryId().trim().equals("")) {
        final Long incomingQtyLong = new Long(cItem.getQuantity());
        final long long1 = incomingQtyLong.longValue();
        final NMGiftlistManager nmGM = (NMGiftlistManager) CheckoutComponents.getGiftlistManager();
        String giftItemId = nmGM.lookupGiftItem(cItem.getRegistryId(), cItem.getCatalogRefId(), cItem.getAuxiliaryData().getProductId());
        
        // If a non-NMGR item is sent to a NMGR address, then the item is treated as an NMGR item
        // even though it is not added to the NMGR. In such a situation,
        // the lookUpGiftItem() method above will return a null giftItemId.
        // Since such an item does not need its tally to be increased,
        // we can avoid the calling of the following method.
        
        final RepositoryItem giftlistRI = nmGM.getGiftlist(cItem.getRegistryId());
        final RepositoryItem giftlistType = nmGM.getGiftListTypeRI(((RepositoryItem) giftlistRI.getPropertyValue("giftlistType")).getRepositoryId());
        
        // if gift item is not found and it belongs to active wishlist,
        // it is a purchased giftcard that needs to be added to the
        // active wishlist for display
        if ((giftItemId == null) && giftlistType.getRepositoryId().equalsIgnoreCase(GiftlistConstants.ACTIVE_WISH_LIST)) {
          nmGM.addItemToGiftlist(cItem.getRegistryId(), cItem.getCatalogRefId(), cItem.getAuxiliaryData().getProductId(), cItem.getQuantity(), false);
          giftItemId = nmGM.lookupGiftItem(cItem.getRegistryId(), cItem.getCatalogRefId(), cItem.getAuxiliaryData().getProductId());
        }
        
        if ((giftItemId != null) && (giftItemId.length() > 0)) {
          nmGM.increaseGiftlistItemQuantityPurchased(cItem.getRegistryId(), giftItemId, long1);
        }
        
      }
      
      // this is where we will set the intl delivery phone number from the
      // profile homephone if its null
      final NMRepositoryContactInfo address = (NMRepositoryContactInfo) shippingGroup.getShippingAddress();
      final String countryCode = address.getCountry();
      
      if ((countryCode != null) && !countryCode.equals("US")) {
        final String theDelPhone = address.getDeliveryPhoneNumber();
        if (theDelPhone == null) {
          final String oHPN = order.getPropertyValue("homePhoneNumber").toString();
          address.setDeliveryPhoneNumber(oHPN);
        }
      }
      
      // SmartPostABTest : modifying item.getServiceLevel based on smartPost AB test group.
      // Also Replacing the hard coding service level codes with a generic method which takes service level dynamically in the below conditions
      final ServiceLevelArray serviceLevelArray = CheckoutComponents.getServiceLevelArray();
      final String cItemServiceLevel = cItem.getServicelevel();
      ServiceLevel serviceLevel = new ServiceLevel();
      if (upgradeOvernight(order, profile, shippingGroup) && (cItemServiceLevel.equals(ServiceLevel.SL3_SERVICE_LEVEL_TYPE) || cItemServiceLevel.equals(ServiceLevel.SL2_SERVICE_LEVEL_TYPE))) {
        cItem.setServicelevel(ServiceLevel.SL1_SERVICE_LEVEL_TYPE);
        serviceLevel = serviceLevelArray.getServiceLevelByCode(ServiceLevel.SL1_SERVICE_LEVEL_TYPE, serviceLevelArray.getServiceLevelGroupBasedOnAbTest());
        order.setLostShipping(serviceLevel.getAmount());
      } else if (upgradeTwoDay(order, profile, shippingGroup) && cItemServiceLevel.equals(ServiceLevel.SL3_SERVICE_LEVEL_TYPE)) {
        cItem.setServicelevel(ServiceLevel.SL2_SERVICE_LEVEL_TYPE);
        serviceLevel = serviceLevelArray.getServiceLevelByCode(ServiceLevel.SL2_SERVICE_LEVEL_TYPE, serviceLevelArray.getServiceLevelGroupBasedOnAbTest());
        order.setLostShipping(serviceLevel.getAmount());
      }
      
      CommonComponentHelper.getSameDayDeliveryUtil().validateSDDFacility(cItem);
    }
    
    removeWishlistIds(order);
    
    try {
      final LinkshareCookie linkshare = (LinkshareCookie) Nucleus.getGlobalNucleus().resolveName("/nm/droplet/LinkShareCookie");
      
      if ((linkshare != null) && linkshare.getEnableLSTracking()) {
        order.setLsSiteId(orderInfo.getLinkShareSiteId());
        order.setLsSiteTime(orderInfo.getLinkShareTimeStamp());
        order.setMid(orderInfo.getMid());
      }
    } catch (final Exception e) {
      // don't stop anything if linkshare fails
    }
    
    // ------- end code from ShoppingCartHandler.preMoveToOrderCommit
    
    if (!order.is_iPhoneOrder()) {
      PricingUtil.getInstance().runProcessRepriceOrder("ORDER_TOTAL", cart, profile);
    }
    
    // order.clearAwardedPromotions();
    
    // Manipulates charges for returns and sets return fee amount to order.
    if (StringUtilities.isNotEmpty(getReturnsEligibilityUtil().getConfiguredFeeType())) {
      getReturnsEligibilityUtil().persistReturnFeeData(order, profile, request);
    }
    
    // ------- start code from
    // atg.commerce.order.ShoppingCartFormHandler.handleMoveToOrderCommit
    final Locale userLocale = orderInfo.getUserLocale();
    
    final PipelineResult result = orderMgr.processOrder(order, userLocale);
    if (result.hasErrors()) {
      final Object[] errors = result.getErrors();
      final StringBuffer msg = new StringBuffer("Process order error:");
      for (int i = 0; i < errors.length; ++i) {
        msg.append(" [").append(errors[i].toString()).append("]");
      }
      
      throw new Exception(msg.toString());
    }
    
    // start code to persist promotional markdown data so that the order
    // fulfiller can send it to cmos
    persistMarkdownData(order, orderMgr);
    // end code to persist promotional markdown data so that the order
    // fulfiller can send it to cmos
    
    final OrderHolder oh = cart.getShoppingCart();
    if (null != oh) {
      oh.setLast(order);
      oh.setCurrent(null);
    }
    
    // ------- end code from
    // atg.commerce.order.ShoppingCartFormHandler.handleMoveToOrderCommit
  }
  
  public boolean upgradeTwoDay(final NMOrderImpl order, final NMProfile profile, final HardgoodShippingGroup shippingGroup) {
    boolean upgradeTwoDay = false;
    final String activatedPromoCode = getAPCode(order);
    final String shipstate = shippingGroup.getShippingAddress().getState();
    final String STN = shippingGroup.getDescription();
    final List<String> extraShippingChargeStates = Arrays.asList(CheckoutComponents.getConfig().getExtraShippingChargeStates());
    final Boolean isPOBox = AddressUtil.getInstance().getIsShipAddressPOBox(profile, STN);
    
    try {
      if ((activatedPromoCode != null) && (activatedPromoCode.indexOf(UPGRADE2NDDAY) != -1) && (!isNonConUSStates(shipstate) || extraShippingChargeStates.contains(shipstate))
              && !isPOBox.booleanValue()) {
        upgradeTwoDay = true;
      }
    } catch (final CommerceException e) {
      e.printStackTrace();
    }
    
    return upgradeTwoDay;
  }
  
  public boolean upgradeOvernight(final NMOrderImpl order, final NMProfile profile, final HardgoodShippingGroup shippingGroup) {
    boolean upgradeOvernight = false;
    
    final String activatedPromoCode = getAPCode(order);
    final String shipstate = shippingGroup.getShippingAddress().getState();
    final String STN = shippingGroup.getDescription();
    final List<String> extraShippingChargeStates = Arrays.asList(CheckoutComponents.getConfig().getExtraShippingChargeStates());
    final Boolean isPOBox = AddressUtil.getInstance().getIsShipAddressPOBox(profile, STN);
    
    try {
      if ((activatedPromoCode != null) && (activatedPromoCode.indexOf(UPGRADEOVERNIGHT) != -1) && (!isNonConUSStates(shipstate) || extraShippingChargeStates.contains(shipstate))
              && !isPOBox.booleanValue()) {
        upgradeOvernight = true;
      }
    } catch (final CommerceException e) {
      e.printStackTrace();
    }
    
    return upgradeOvernight;
  }
  
  public String getLossPreventionInfo(final DynamoHttpServletRequest pRequest, final NMOrderImpl order) {
    final StringBuffer sb = new StringBuffer();
    sb.append(pRequest.getContentType());
    sb.append("~" + pRequest.getProtocol());
    
    if ((pRequest.getHeader("nmcip") != null) && !"".equals(pRequest.getHeader("nmcip"))) {
      sb.append("~" + pRequest.getHeader("nmcip"));
      sb.append("~" + pRequest.getHeader("nmcip"));
    } else {
      sb.append("~" + pRequest.getRemoteAddr());
      sb.append("~" + pRequest.getRemoteHost());
    }
    
    sb.append("~" + pRequest.getMethod());
    sb.append("~" + pRequest.getRequestURI());
    sb.append("~" + pRequest.getQueryString());
    sb.append("~" + pRequest.getRemoteUser());
    sb.append("~" + pRequest.getAuthType());
    sb.append("~" + createHeaderFragment(pRequest, "USER-AGENT"));
    sb.append("~" + createHeaderFragment(pRequest, "HOST"));
    sb.append("~" + createHeaderFragment(pRequest, "ACCEPT"));
    sb.append("~" + createHeaderFragment(pRequest, "ACCEPT-CHARSET"));
    sb.append("~" + createHeaderFragment(pRequest, "ACCEPT-ENCODING"));
    sb.append("~" + createHeaderFragment(pRequest, "ACCEPT-LANGUAGE"));
    sb.append("~" + EnvironmentUtils.getHostName() + "_" + EnvironmentUtils.getAtgInstanceName());
    sb.append("~" + pRequest.getHeader("COOKIE"));
    sb.append("~" + pRequest.getHeader("REFERER"));
    sb.append("~" + (String) order.getPropertyValue("browserDateAndTime"));
    sb.append("~" + (String) order.getPropertyValue("browserTimeZone"));
    return sb.toString();
  }
  
  private String createHeaderFragment(final DynamoHttpServletRequest request, final String headerName) {
    final StringBuffer tempHeader = new StringBuffer();
    String headerNameComplete = null;
    @SuppressWarnings("unchecked")
    final Enumeration<String> headerNameAttributes = request.getHeaders(headerName);
    
    while (headerNameAttributes.hasMoreElements()) {
      tempHeader.append(headerNameAttributes.nextElement()).append(",");
    }
    if ((tempHeader != null) && (tempHeader.length() > 0)) {
      headerNameComplete = tempHeader.substring(0, tempHeader.length() - 1); // to remove the last ","
    }
    
    return headerNameComplete;
  }
  
  /*
   * This method persists promotional markdown data to the repository. This allows the fulfillment module to send this data to cmos.
   */
  public void persistMarkdownData(final NMOrderImpl order, final OrderManager orderMgr) throws Exception {
    final List<ShippingGroupCommerceItemRelationship> shipGroupRels = getShippingGroupCommerceItemRelationships(order, orderMgr);
    final Iterator<ShippingGroupCommerceItemRelationship> shipGroupRelsIterator = shipGroupRels.iterator();
    
    while (shipGroupRelsIterator.hasNext()) {
      final ShippingGroupCommerceItemRelationship shipGroupRel = shipGroupRelsIterator.next();
      final NMCommerceItem cItem = (NMCommerceItem) shipGroupRel.getCommerceItem();
      final NMItemPriceInfo ipi = (NMItemPriceInfo) cItem.getPriceInfo();
      final Map<String, Markdown> promotionsApplied = ipi.getPromotionsApplied();
      
      if (promotionsApplied != null) {
        final Iterator<String> promotionsAppliedKeySetIterator = promotionsApplied.keySet().iterator();
        
        while (promotionsAppliedKeySetIterator.hasNext()) {
          final String promoId = promotionsAppliedKeySetIterator.next();
          final String amountInfoId = cItem.getPriceInfoRepositoryItem().getRepositoryId();
          final String markdownId = amountInfoId + ":" + promoId;
          
          final Markdown markdown = promotionsApplied.get(promoId);
          MutableRepositoryItem markdownRI = null;
          final MutableRepository orderRepository = (MutableRepository) orderMgr.getOrderTools().getOrderRepository();
          boolean existsInRepository;
          
          if (orderRepository.getItem(markdownId, "markdown") == null) {
            existsInRepository = false;
            markdownRI = orderRepository.createItem(markdownId, "markdown");
          } else {
            existsInRepository = true;
            markdownRI = orderRepository.getItemForUpdate(markdownId, "markdown");
          }
          
          markdownRI.setPropertyValue("type", new Integer(markdown.getType()));
          markdownRI.setPropertyValue("percentDiscount", markdown.getPercentDiscount());
          markdownRI.setPropertyValue("dollarDiscount", new Double(markdown.getDollarDiscount()));
          markdownRI.setPropertyValue("notes", markdown.getNotes());
          markdownRI.setPropertyValue("promoKey", markdown.getPromoKey());
          markdownRI.setPropertyValue("userEnteredPromoCode", markdown.getUserEnteredPromoCode());
          
          if (existsInRepository) {
            orderRepository.updateItem(markdownRI);
          } else {
            orderRepository.addItem(markdownRI);
          }
        }
      }
    }
  }
  
  public String getAPCode(final NMOrderImpl order) {
    String aPCode = order.getActivatedPromoCode();
    
    // This will remove the comma that is appended to the promocode.
    if ((aPCode != null) && (aPCode.trim().length() > 0)) {
      final int endIndex = aPCode.length() - 1;
      final StringBuffer temp = new StringBuffer(aPCode.substring(0, endIndex));
      aPCode = temp.toString();
    } else {
      aPCode = "No Code";
    }
    
    return aPCode;
  }
  
  public boolean isNonConUSStates(final String cItemState) throws CommerceException {
    final String[] nonContStates = CheckoutComponents.getConfig().getNonContinentalUSStates();
    
    for (int i = 0; i < nonContStates.length; i++) {
      if (nonContStates[i].equals(cItemState)) {
        return true;
      }
    }
    
    return false;
  }
  
  /**
   * METHOD: removeWishlistIds PURPOSE: This method removes the id's from the repository property registryId when the type the id represents is ACTIVE_WISHLIST PARAMETERS: CONTENTS: Class Variables
   */
  public void removeWishlistIds(final NMOrderImpl order) throws CommerceException {
    final GiftlistManager glm = CheckoutComponents.getGiftlistManager();
    final List<NMCommerceItem> commerceItemList = order.getNmCommerceItems();
    final Iterator<NMCommerceItem> commerceItemListIterator = commerceItemList.iterator();
    while (commerceItemListIterator.hasNext()) {
      final NMCommerceItem nmci = commerceItemListIterator.next();
      final String giftlistId = nmci.getRegistryId();
      if ((giftlistId != null) && !giftlistId.trim().equals("")) {
        final RepositoryItem giftlistRI = ((NMGiftlistManager) glm).getGiftlist(giftlistId);
        final RepositoryItem giftlistType = ((NMGiftlistManager) glm).getGiftListTypeRI(((RepositoryItem) giftlistRI.getPropertyValue("giftlistType")).getRepositoryId());
        if ((giftlistType != null) && giftlistType.getRepositoryId().equalsIgnoreCase(GiftlistConstants.ACTIVE_WISH_LIST)) {
          nmci.setRegistryId("");
        }
      }
    }
  }
  
  /**
   * Updates the user's personalization info since they just made an order
   */
  private void updatePersonalizationStatsForOrder(final Profile profile, final Order order) {
    // Set the last purchase date on the profile
    // Get the date as of right now
    final Date rightNow = new Date();
    ((MutableRepositoryItem) profile).setPropertyValue("lastPurchase", rightNow);
    
    final NMProfile nmProfile = (NMProfile) profile;
    final NMOrderImpl lastOrder = (NMOrderImpl) order;
    nmProfile.compileStats(lastOrder);
  }
  
  private void updateOrderReportTable(final Order order) {
    final NMOrderImpl lastOrder = (NMOrderImpl) order;
    
    try {
      final int cic = lastOrder.getCommerceItemCount();
      final String scic = Integer.toString(cic);
      final String pcl1 = lastOrder.getPromoCodeList().toString();
      String pcl2 = pcl1.replaceAll("\\[", "");
      pcl2 = pcl2.replaceAll("\\]", "");
      // System.out.println(">>>>>>--- **** SCH updateOrderReportTable
      // pcl2 is:"+pcl2);
      final MutableRepository oRR = (MutableRepository) Nucleus.getGlobalNucleus().resolveName("/nm/xml/OrderReportRepository");
      final MutableRepositoryItem oMRRItem = oRR.createItem(lastOrder.getId(), "orderReportItem");
      
      oMRRItem.setPropertyValue("profileId", lastOrder.getProfileId());
      oMRRItem.setPropertyValue("submittedDate", lastOrder.getSubmittedDate());
      oMRRItem.setPropertyValue("orderAmount", new Double(lastOrder.getPriceInfo().getAmount()));
      // lastOrder.getsitoryItem().getPropertyValue("amount"));
      oMRRItem.setPropertyValue("lostShipping", lastOrder.getPropertyValue("lostShipping"));
      oMRRItem.setPropertyValue("reporting_code", lastOrder.getPropertyValue("reporting_code"));
      oMRRItem.setPropertyValue("activatedPromoCodes", lastOrder.getActivatedPromoCode().toString());
      oMRRItem.setPropertyValue("userEnteredPromoCodes", pcl2);
      oMRRItem.setPropertyValue("commerceItemCount", new Double(scic));
      // not needed as of 7-29-08, could be sometime
      // oMRRItem.setPropertyValue("itemList",((String)lastOrder.getCommerceItems().toString()));
      // Add the item to repository
      // System.out.println(">>>>>>--- **** SCH updateOrderReportTable
      // oMRRItem is:"+oMRRItem.toString());
      oRR.addItem(oMRRItem);
      // System.out.println(">>>>>>--- **** SCH updateOrderReportTable oRR
      // is:"+oRR.toString());
    } catch (final RepositoryException repositoryExc) {
      if (CheckoutComponents.getConfig().isLoggingError()) {
        CheckoutComponents.getConfig().logError("SCH OrderReportRepository updateOrderReportTable had a problem adding this orderReportItem ", repositoryExc);
      }
    }
  }
  
  public CommerceItem getCommerceItem(final Order order, final String itemId) {
    final List<NMCommerceItem> cartItems = ((NMOrderImpl) order).getNmCommerceItems();
    for (final Iterator<NMCommerceItem> i = cartItems.iterator(); i.hasNext();) {
      final CommerceItem ci = i.next();
      if (ci.getId().equals(itemId)) {
        return ci;
      }
    }
    
    return null;
  }
  
  /**
   * Retrieves all ShippingGroupCommerceItemRelationship objects associated with the specified order.
   * 
   * @param pOrder
   *          - The order from which the shipping group commerce item relationships will be retrieved
   * @param orderMgr
   *          - The order manager to use for retrieval fo the relationships
   * @return List of all shipping group commerce item relationships on the order
   */
  public List<ShippingGroupCommerceItemRelationship> getShippingGroupCommerceItemRelationships(final Order pOrder, final OrderManager orderMgr) throws CommerceException {
    
    @SuppressWarnings("unchecked")
    final List<ShippingGroupCommerceItemRelationship> ciRels = orderMgr.getCommerceItemManager().getAllCommerceItemRelationships(pOrder);
    
    @SuppressWarnings("unchecked")
    final List<ShippingGroupCommerceItemRelationship> sgRels = orderMgr.getShippingGroupManager().getAllShippingGroupRelationships(pOrder);
    
    ciRels.retainAll(sgRels);
    return ciRels;
  }
  
  public List<NMCommerceItem> getShippingGroupCommerceItems(final Order pOrder, final OrderManager orderMgr) throws CommerceException {
    final List<NMCommerceItem> items = new ArrayList<NMCommerceItem>();
    for (final ShippingGroupCommerceItemRelationship itemRel : getShippingGroupCommerceItemRelationships(pOrder, orderMgr)) {
      items.add((NMCommerceItem) itemRel.getCommerceItem());
    }
    return items;
  }
  
  /**
   * Splits up the passed in commerce item. If splitOneItemOnly is false, then it splits the commerce item into separate commerce items each with a quantity of 1. If splitOneItemOnly is true, then it
   * splits the commerce item into 2 commerce items, one with a qty of 1 and the other with a quantity of one less than the original. If the commerce item passed has a quantity of 1, then nothing
   * changes This routine gets the service -- if you have the service available, you should directly call the splitUpCommerceItem() method with the service passed in. Splits up the passed in commerce
   * item into separate commerce items each with a quantity of 1. If the commerce item passed has a quantity of 1, then nothing changes Splits up the passed in commerce item into separate commerce
   * items each with a quantity of 1. If the commerce item passed has a quantity of 1, then nothing changes
   * 
   * @param jenga
   */
  public String splitUpCommerceItem(final ShoppingCartHandler cart, final Profile profile, final Order order, final OrderManager orderMgr, final Locale userLocale, final String itemId,
          final boolean removeOriginal, final boolean splitOneItemOnly) throws Exception {
    final NMCommerceItem item = (NMCommerceItem) order.getCommerceItem(itemId);
    final String sgMatchId = getShippingGroup(item).getId();
    final long qty = item.getQuantity();
    final long cloneQty = 1;
    final long remainingQty = qty - cloneQty;
    long numDuplicatesToCreate = qty;
    
    if (splitOneItemOnly) {
      numDuplicatesToCreate = 1;
    }
    
    // If the number of duplicates to create is less than two then do
    // nothing since
    // this commerce item cannot be broken up
    if (qty < 2) {
      return sgMatchId;
    }
    
    // If the service or profile could not be retrieved, we cannot successfully reprice.
    // therefore, don't do the split.
    if (profile == null) {
      return sgMatchId;
    }
    
    // remove gift options before cloning
    if (!item.getFlgBngl()) {
      item.setGiftWrap(null);
      item.setGiftWrapSeparately(false);
      item.setGiftNote(0);
      item.setNoteLine1(null);
      item.setNoteLine2(null);
      item.setNoteLine3(null);
      item.setNoteLine4(null);
      item.setNoteLine5(null);
    }
    
    // Unleash the clones
    
    final CommerceItemManager itemMgr = orderMgr.getCommerceItemManager();
    final ProdSkuUtil prodSkuUtil = CommonComponentHelper.getProdSkuUtil();
    RepositoryItem sku = null;
    RepositoryItem product = null;
    int stocklevel = 0;
    try {
      final Repository prodRepos = CommonComponentHelper.getProductRepository();
      sku = prodRepos.getItem(item.getCatalogRefId(), "sku");
      product = prodSkuUtil.getProductRepositoryItem(item.getCmosCatalogId(), item.getCmosItemCode());
      stocklevel = Integer.parseInt((String) sku.getPropertyValue("stockLevel"));
    } catch (final Exception e) {
      System.out.println("ShoppingCartHandler splitUpCommerceItem failed with, " + e + ".");
    }
    
    for (int i = 0; i < numDuplicatesToCreate; i++) {
      // set the item quantity to the appropriate amount so the clone qty will be correct.
      item.setQuantity(cloneQty);
      cloneCommerceItem(item, order, orderMgr, prodSkuUtil, sku, product, stocklevel, sgMatchId);
      stocklevel = stocklevel - 1;
    }
    
    if (splitOneItemOnly) {
      // set the item quantity to the appropriate amount so the clone qty will be correct.
      item.setQuantity(remainingQty);
      cloneCommerceItem(item, order, orderMgr, prodSkuUtil, sku, product, stocklevel, sgMatchId);
    }
    
    if (removeOriginal) {
      ShippingUtil.getInstance().handleClearTagging(cart);
      // Remove the original item from the order
      itemMgr.removeItemFromOrder(order, item.getId());
      
      // Run any associated scenarios with the removal
      // have to reprice the order here so when scenarios are called
      // they have a price not zero.
      PricingUtil.getInstance().repriceOrder(cart, (NMProfile)profile, -1);
      //runProcessRepriceOrder("ORDER_TOTAL", cart, order, profile, userLocale);
    }
    orderMgr.updateOrder(order);
    
    return sgMatchId;
  }
  
  private void cloneCommerceItem(final NMCommerceItem item, final Order order, final OrderManager orderMgr, final ProdSkuUtil prodSkuUtil, final RepositoryItem sku, final RepositoryItem product,
          final int stocklevel, final String sgMatchId) {
    final CommerceItemManager itemMgr = orderMgr.getCommerceItemManager();
    try {
      // Create the clone
      NMCommerceItem clone = (NMCommerceItem) item.clone();
      
      System.out.println("cloned item " + clone);
      
      // Blow away the promotimestamp so one has to be generated
      clone.setPromoTimeStamp(null);
      
      // Add to order
      clone = (NMCommerceItem) itemMgr.addAsSeparateItemToOrder(order, clone);
      
      // Add to shipping group
      /*
       * int stockLevel, String shipDate, String qtyOnOrder) /** Name of Method: getStatusDisplay @param int statusDisplayType { 1=text; 2=image } @param boolean dropShip @param boolean inStock @param
       * boolean exclusive @param boolean preOrder
       * 
       * @param String discontinuedCode @param boolean hasPOQty
       * 
       * @return String statusString This logic is used to determine what 'status' the product and sku should display public String getStatusDisplay(int statusDisplayType, boolean dropShip, boolean
       * inStock, boolean exclusive, boolean preOrder, String discontinuedCode, boolean hasPOQty, boolean isMonoItem, boolean isVirtualGiftCard) {
       */
      
      try {
        clone.setTransientStatus(prodSkuUtil.getStatusString(product, sku, Long.toString(clone.getQuantity()), stocklevel));
      } catch (final Exception e) {
        System.out.println("OrderUti cloneCommerceItem failed with, " + e + ".");
      }
      
      orderMgr.getCommerceItemManager().addItemQuantityToShippingGroup(order, clone.getId(), sgMatchId, clone.getQuantity());
    } catch (final Exception e) {
      System.out.println("OrderUtil cloneCommerceItem failed with, " + e + ".");
    }
    
  }
  
  public void runProcessRepriceOrder(final String pricingOper, final ShoppingCartHandler cart, final Order order, final Profile profile, final Locale userLocale) throws RunProcessException,
          ServletException, IOException {
    final PricingModelHolder upm = cart.getUserPricingModels();
    cart.runProcessRepriceOrder(pricingOper, order, upm, userLocale, profile);
    
    // NMOrderPriceInfo priceInfo = (NMOrderPriceInfo)order.getPriceInfo();
  }
  
  /**
   * Returns the address repository item associated with the addressKey
   */
  public RepositoryItem getAddressFromKey(final ShoppingCartHandler sc, final Profile profile, final String addressKey) throws Exception {
    RepositoryItem returnValue = null;
    
    if (StringUtilities.isEmpty(addressKey)) {
      return returnValue;
    }
    
    final CommercePropertyManager pm = sc.getPropertyManager();
    final String defaultShippingAddrName = pm.getDefaultShippingAddrName(java.util.Locale.US);
    final String defaultBillingAddrName = pm.getDefaultBillingAddrName(java.util.Locale.US);
    
    if (addressKey.equals(defaultShippingAddrName)) {
      returnValue = (RepositoryItem) profile.getPropertyValue(ProfileProperties.Profile_shippingAddress);
    } else if (addressKey.equals(defaultBillingAddrName)) {
      returnValue = (RepositoryItem) profile.getPropertyValue(ProfileProperties.Profile_billingAddress);
    } else {
      @SuppressWarnings("unchecked")
      final Map<String, MutableRepositoryItem> addressMap = (Map<String, MutableRepositoryItem>) profile.getPropertyValue(ProfileProperties.Profile_secondaryAddresses);
      final Set<String> keySet = addressMap.keySet();
      final Iterator<String> keyIterator = keySet.iterator();
      
      for (int i = 0; i < keySet.size(); i++) {
        final String key = keyIterator.next();
        final RepositoryItem value = addressMap.get(key);
        
        if (key.equals(addressKey)) {
          returnValue = value;
          break;
        }
      }
      
    }
    
    return returnValue;
  }
  
  public ResultBean removeItemFromOrder(final ShoppingCartHandler cart, final String commerceId) throws Exception {
    final ResultBean result = new ResultBean();
    
    final NMOrderHolder orderHolder = (NMOrderHolder) cart.getOrderHolder();
    final NMOrderImpl order = cart.getNMOrder();
    final NMProfile profile = getProfile();
    
    ResultBeanHelper.removeItemFromCart(result, (NMCommerceItem) order.getCommerceItem(commerceId));
    
    ShippingUtil.getInstance().handleClearTagging(cart);
    cart.setRemovalCommerceIds(new String[] {commerceId});
    cart.getOrderManager().getCommerceItemManager().removeItemFromOrder(order, commerceId);
    
    // reprice the order to get messages back, if any
    final List<Message> promoMessages = PricingUtil.getInstance().repriceOrder(cart, profile, SnapshotUtil.EDIT_ITEM_FROM_REGULAR_CART);
    result.setMessages(promoMessages);
    
    return result;
  }
  
  public void removeItem(final NMOrderHolder orderHolder, final OrderManager orderMgr,/* DynamoHttpServletRequest request, */final String commerceItemId, final ShoppingCartHandler cart)
          throws Exception {
    final NMOrderImpl order = cart.getNMOrder();
    ShippingUtil.getInstance().handleClearTagging(cart);
    final CommerceItemManager commerceItemManager = orderMgr.getCommerceItemManager();
    commerceItemManager.removeAllRelationshipsFromCommerceItem(order, commerceItemId);
    commerceItemManager.removeItemFromOrder(order, commerceItemId);
    
    /*
     * HttpSession session = request.getSession(); if(session != null) { session.setAttribute("itemsRemoved", "y"); //WR 33318: track itemsRemoved for CMOS/FraudNet }
     */
  }
  
  public double getCurrentOrderAmountRemaining(final Order order, final OrderManager orderMgr) {
    try {
      return ((NMOrderManager) orderMgr).getCurrentOrderAmountRemaining(order);
    } catch (final Exception e) {
      System.out.println(e);
      return 0.0d;
    }
  }
  
  public void removeEmptyShippingGroups(final NMOrderImpl order, final OrderManager orderMgr) throws CommerceException {
    final List<String> removeGroupIds = new ArrayList<String>();
    final ShippingGroupManager shipMgr = orderMgr.getShippingGroupManager();
    @SuppressWarnings("unchecked")
    final List<ShippingGroup> shippingGroups = order.getShippingGroups();
    
    for (final ShippingGroup shippingGroup : shippingGroups) {
      if (shippingGroup.getCommerceItemRelationshipCount() < 1) {
        removeGroupIds.add(shippingGroup.getId());
      }
    }
    
    for (final String sgId : removeGroupIds) {
      shipMgr.removeShippingGroupFromOrder(order, sgId);
    }
  }
  
  /**
   * Builds the viewed registries list for the profile specified by interrogating the order's ShippingGroupCommerceItemRelationships. The reviewd registries Map for the profile will be rebuilt to
   * contain the registryId (map key) and registry shipping address (map value) for any ShippingGroupCommerceItemRelationship that is from a registry.
   * 
   * @param profile
   *          - The profile whose order is to be examined
   * @param order
   *          - The order to be interrogated
   * @param orderMgr
   *          - the order manager used to process the order
   * @throws CommerceException
   * @throws RepositoryException
   */
  public void buildShipToList(final Profile profile, final Order order, final OrderManager orderMgr) throws CommerceException, RepositoryException {
    final NMProfile theNMProfile = (NMProfile) profile;
    final HashMap<String, String> glKeySetMap = new HashMap<String, String>(theNMProfile.getViewedRegistriesList());
    
    final List<ShippingGroupCommerceItemRelationship> rShipList = getShippingGroupCommerceItemRelationships(order, orderMgr);
    final Iterator<ShippingGroupCommerceItemRelationship> rShipIterator = rShipList.iterator();
    
    while (rShipIterator.hasNext()) {
      final ShippingGroupCommerceItemRelationship sgcRel = rShipIterator.next();
      final NMCommerceItem nmci = (NMCommerceItem) sgcRel.getCommerceItem();
      final String glId = nmci.getRegistryId();
      if ((glId != null) && !glId.trim().equals("")) {
        glKeySetMap.put(glId, "");
      }
    }
    
    theNMProfile.getViewedRegistriesList().clear();
    
    final Collection<String> viewedGL = glKeySetMap.keySet();
    final Iterator<String> GLIterator = viewedGL.iterator();
    while (GLIterator.hasNext()) {
      final String regID = GLIterator.next();
      final Repository giftListRepos = (Repository) Nucleus.getGlobalNucleus().resolveName("/atg/commerce/gifts/Giftlists");
      final RepositoryItem giftlist = giftListRepos.getItem(regID, "gift-list");
      final RepositoryItem theCI = (atg.repository.RepositoryItem) giftlist.getPropertyValue("shippingAddress");
      theNMProfile.getViewedRegistriesList().put(regID, theCI.getRepositoryId());
    }
  }
  
  public Collection<NMCommerceItem> getDeliveryPhoneItems(final NMOrderImpl order, final OrderManager orderMgr) throws CommerceException {
    final List<NMCommerceItem> items = getShippingGroupCommerceItems(order, orderMgr);
    final List<NMCommerceItem> deliveryPhoneItems = new ArrayList<NMCommerceItem>();
    for (final NMCommerceItem item : items) {
      /* The DeliveryPhone option in order review page should be restricted for International DropShip item as per DropShip/ShipFromStore project requirement */
      if (item.getProduct().getFlgDeliveryPhone() && !(item.isDropship() && getLocalizationUtils().checkForInternational(getProfile()))) {
        deliveryPhoneItems.add(item);
      }
    }
    return deliveryPhoneItems;
  }
  
  public Collection<NMCommerceItem> getReplenishmentItems(final NMOrderImpl order, final OrderManager orderMgr) throws CommerceException {
    final List<NMCommerceItem> items = getShippingGroupCommerceItems(order, orderMgr);
    final List<NMCommerceItem> replenishmentItems = new ArrayList<NMCommerceItem>();
    
    for (final NMCommerceItem item : items) {
      final String interval = item.getSelectedInterval();
      if (!StringUtilities.isEmpty(interval)) {
        replenishmentItems.add(item);
      }
    }
    
    return replenishmentItems;
  }
  
  public String getShippingGroupId(final NMCommerceItem item) {
    return ShippingGroupUtil.getInstance().getShippingGroupId(item);
  }
  
  public ShippingGroup getShippingGroup(final CommerceItem item) {
    final ShippingGroup shipGrp = getShipGroupRelationship(item).getShippingGroup();
    return shipGrp;
  }
  
  public ShippingGroupCommerceItemRelationship getShipGroupRelationship(final CommerceItem item) {
    final ShippingGroupCommerceItemRelationship shipRel = (ShippingGroupCommerceItemRelationship) item.getShippingGroupRelationships().get(0);
    return shipRel;
  }
  
  private String getShipGrpFromKey(final String key) {
    final String[] a = key.split(SHIPGRP_SERVICELVL_DELIM);
    return a[0];
  }
  
  private String getServiceLvlFromKey(final String key) {
    final String[] a = key.split(SHIPGRP_SERVICELVL_DELIM);
    return a[1];
  }
  
  private String buildShipGrpServiceLvlKey(final String shipGrp, final String serviceLvl) {
    return shipGrp + SHIPGRP_SERVICELVL_DELIM + serviceLvl;
  }
  
  private void updatePromoEmailValidationUseCount(final NMOrderImpl order) throws RepositoryException {
    final PromotionsHelper promotionsHelper = CheckoutComponents.getPromotionsHelper();
    final DynamoHttpServletRequest req = getCurrentRequest();
    final String promoTrackingComponent = "/nm/ajax/checkout/session/PersonalizedPromoTracking";
    final Map<String, String> emailPromos = order.getEmailValidationPromos();
    final PersonalizedPromoTracking promoTracking = (PersonalizedPromoTracking) req.resolveName(promoTrackingComponent);
    final Set<NMPromotion> awardedPromos = order.getAwardedPromotions();
    final Map<String, String> promoEmailMapping = promoTracking.getPromoEmailMapping();
    if (null != awardedPromos) {
      for (final Iterator<NMPromotion> i = awardedPromos.iterator(); i.hasNext();) {
        final NMPromotion nmPromotion = i.next();
        if (nmPromotion.requiresEmailValidation()) {
          final String promoCode = nmPromotion.getPromoCodes();
          final String email = promoEmailMapping.get(promoCode);
          emailPromos.put(promoCode, email);
        }
      }
    }
    promotionsHelper.updatePromoEmailValidationUseCount(order);
    
  }
  
  /* ExpPaypal_Flow : settingexpPaypal billing and shipping address to Order. */
  public boolean setExpPaypalInfoToOrder(final NMOrderImpl order, final NMOrderManager orderMgr, final NMProfile profile, final NMBorderFreePayPalResponse nmBorderFreePayPalResponse) throws Exception {
    
    final HardgoodShippingGroup sg = (HardgoodShippingGroup) order.getShippingGroups().get(0);
    final NMRepositoryContactInfo shippingAddress = (NMRepositoryContactInfo) sg.getShippingAddress();
    if ((nmBorderFreePayPalResponse != null) && (nmBorderFreePayPalResponse.getAltBuyerResponseVO() != null)) {
      setExpPaypalShippingAddrToOrder(nmBorderFreePayPalResponse, shippingAddress);
      setExpPaypalBilingAddrToOrder(order, orderMgr, nmBorderFreePayPalResponse);
      return true;
    }
    return false;
  }
  
  /** ExpPaypal_Flow : settingexpPaypal billing and shipping address to Order. */
  
  /**
   * This method is used to sanitize the field values
   * 
   * @param fieldValue
   * @param fieldLength
   * @return sanitizedFieldValue
   */
  public String sanitizeFieldLength(final String fieldValue, final int fieldLength) {
    String sanitizedFieldValues = "";
    if (fieldValue.length() > fieldLength) {
      sanitizedFieldValues = fieldValue.substring(0, fieldLength);
    } else {
      sanitizedFieldValues = fieldValue;
    }
    return sanitizedFieldValues;
  }
  
  /**
   * This method is used to set Express PayPal Shipping Addres to Order
   * 
   * @param nmBorderFreePayPalResponse
   * @param shippingAddress
   */
  public void setExpPaypalShippingAddrToOrder(final NMBorderFreePayPalResponse nmBorderFreePayPalResponse, final NMRepositoryContactInfo shippingAddress) {
    final LocalizationUtils localizationUtils = getLocalizationUtils();
    String sanitizedFieldValue = "";
    shippingAddress.setCountry(nmBorderFreePayPalResponse.getAltBuyerResponseVO().getShipToCountryCode());
    if (nmBorderFreePayPalResponse.getAltBuyerResponseVO().getShipToFirstName() != null) {
      sanitizedFieldValue =
              sanitizeFieldLength(nmBorderFreePayPalResponse.getAltBuyerResponseVO().getShipToFirstName(),
                      Integer.parseInt(localizationUtils.getExpPayPalFieldLength().get(BorderFreeConstants.PPX_NAME_FLD_LENGTH)));
      shippingAddress.setFirstName(sanitizedFieldValue);
    }
    if (nmBorderFreePayPalResponse.getAltBuyerResponseVO().getShipToLastName() != null) {
      sanitizedFieldValue =
              sanitizeFieldLength(nmBorderFreePayPalResponse.getAltBuyerResponseVO().getShipToLastName(),
                      Integer.parseInt(localizationUtils.getExpPayPalFieldLength().get(BorderFreeConstants.PPX_NAME_FLD_LENGTH)));
      shippingAddress.setLastName(sanitizedFieldValue);
    }
    
    if (nmBorderFreePayPalResponse.getAltBuyerResponseVO().getShipToAddress1() != null) {
      sanitizedFieldValue =
              sanitizeFieldLength(nmBorderFreePayPalResponse.getAltBuyerResponseVO().getShipToAddress1(),
                      Integer.parseInt(localizationUtils.getExpPayPalFieldLength().get(BorderFreeConstants.PPX_ADDR_FLD_LENGTH)));
      shippingAddress.setAddress1(sanitizedFieldValue);
    }
    if (nmBorderFreePayPalResponse.getAltBuyerResponseVO().getShipToAddress2() != null) {
      sanitizedFieldValue =
              sanitizeFieldLength(nmBorderFreePayPalResponse.getAltBuyerResponseVO().getShipToAddress2(),
                      Integer.parseInt(localizationUtils.getExpPayPalFieldLength().get(BorderFreeConstants.PPX_ADDR_FLD_LENGTH)));
      shippingAddress.setAddress2(sanitizedFieldValue);
    }
    if (nmBorderFreePayPalResponse.getAltBuyerResponseVO().getShipToCity() != null) {
      sanitizedFieldValue =
              sanitizeFieldLength(nmBorderFreePayPalResponse.getAltBuyerResponseVO().getShipToCity(),
                      Integer.parseInt(localizationUtils.getExpPayPalFieldLength().get(BorderFreeConstants.PPX_CITY_STATE_FLD_LENGTH)));
      shippingAddress.setCity(sanitizedFieldValue);
    }
    if (nmBorderFreePayPalResponse.getAltBuyerResponseVO().getShipToPostalCode() != null) {
      sanitizedFieldValue =
              sanitizeFieldLength(nmBorderFreePayPalResponse.getAltBuyerResponseVO().getShipToPostalCode(),
                      Integer.parseInt(localizationUtils.getExpPayPalFieldLength().get(BorderFreeConstants.PPX_POSTAL_FLD_LENGTH)));
      shippingAddress.setPostalCode(sanitizedFieldValue);
    }
    if (nmBorderFreePayPalResponse.getAltBuyerResponseVO().getShipToPrimaryPhone() != null) {
      sanitizedFieldValue =
              sanitizeFieldLength(nmBorderFreePayPalResponse.getAltBuyerResponseVO().getShipToPrimaryPhone(),
                      Integer.parseInt(localizationUtils.getExpPayPalFieldLength().get(BorderFreeConstants.PPX_PHONE_FLD_LENGTH)));
      shippingAddress.setPhoneNumber(sanitizedFieldValue);
    }
    shippingAddress.setEmail(nmBorderFreePayPalResponse.getAltBuyerResponseVO().getShipToEmail());
    if (nmBorderFreePayPalResponse.getAltBuyerResponseVO().getShipToRegion() != null) {
      sanitizedFieldValue =
              sanitizeFieldLength(nmBorderFreePayPalResponse.getAltBuyerResponseVO().getShipToRegion(),
                      Integer.parseInt(localizationUtils.getExpPayPalFieldLength().get(BorderFreeConstants.PPX_CITY_STATE_FLD_LENGTH)));
      shippingAddress.setState(sanitizedFieldValue);
    }
    
  }
  
  /**
   * This method is used to set Express PayPal Billing Address in Order
   * 
   * @param order
   * @param orderMgr
   * @param nmBorderFreePayPalResponse
   * @throws CommerceException
   */
  public void setExpPaypalBilingAddrToOrder(final NMOrderImpl order, final NMOrderManager orderMgr, final NMBorderFreePayPalResponse nmBorderFreePayPalResponse) throws CommerceException {
    final LocalizationUtils localizationUtils = getLocalizationUtils();
    final NMPaymentGroupManager paymentGroupMgr = (NMPaymentGroupManager) orderMgr.getPaymentGroupManager();
    final List<NMCreditCard> orderCreditCards = order.getCreditCards();
    NMCreditCard paymentGroup;
    String sanitizedFieldValue = "";
    boolean newPaymentGroup = false;
    if (orderCreditCards.isEmpty()) {
      paymentGroup = (NMCreditCard) paymentGroupMgr.createPaymentGroup();
      newPaymentGroup = true;
    } else {
      // assumes only one credit per order
      paymentGroup = orderCreditCards.get(0);
    }
    paymentGroup.setCreditCardNumber("");
    paymentGroup.setCreditCardType(BorderFreeConstants.INTL_PAY_PAL);
    paymentGroup.setExpirationMonth("");
    paymentGroup.setExpirationYear("");
    paymentGroup.setPayPalPayerId("");
    paymentGroup.setPayPalBillingAgreementId("");
    paymentGroup.setCardVerficationNumber("");
    final NMRepositoryContactInfo billingAddress = (NMRepositoryContactInfo) paymentGroup.getBillingAddress();
    
    if (nmBorderFreePayPalResponse.getAltBuyerResponseVO() != null) {
      if (nmBorderFreePayPalResponse.getAltBuyerResponseVO().getBillToAddress1() != null) {
        sanitizedFieldValue =
                sanitizeFieldLength(nmBorderFreePayPalResponse.getAltBuyerResponseVO().getBillToAddress1(),
                        Integer.parseInt(localizationUtils.getExpPayPalFieldLength().get(BorderFreeConstants.PPX_ADDR_FLD_LENGTH)));
        billingAddress.setAddress1(sanitizedFieldValue);
      }
      if (nmBorderFreePayPalResponse.getAltBuyerResponseVO().getBillToAddress2() != null) {
        sanitizedFieldValue =
                sanitizeFieldLength(nmBorderFreePayPalResponse.getAltBuyerResponseVO().getBillToAddress2(),
                        Integer.parseInt(localizationUtils.getExpPayPalFieldLength().get(BorderFreeConstants.PPX_ADDR_FLD_LENGTH)));
        billingAddress.setAddress2(sanitizedFieldValue);
      }
      if (nmBorderFreePayPalResponse.getAltBuyerResponseVO().getBillToFirstName() != null) {
        sanitizedFieldValue =
                sanitizeFieldLength(nmBorderFreePayPalResponse.getAltBuyerResponseVO().getBillToFirstName(),
                        Integer.parseInt(localizationUtils.getExpPayPalFieldLength().get(BorderFreeConstants.PPX_NAME_FLD_LENGTH)));
        billingAddress.setFirstName(sanitizedFieldValue);
      }
      if (nmBorderFreePayPalResponse.getAltBuyerResponseVO().getBillToLastName() != null) {
        sanitizedFieldValue =
                sanitizeFieldLength(nmBorderFreePayPalResponse.getAltBuyerResponseVO().getBillToLastName(),
                        Integer.parseInt(localizationUtils.getExpPayPalFieldLength().get(BorderFreeConstants.PPX_NAME_FLD_LENGTH)));
        billingAddress.setLastName(sanitizedFieldValue);
      }
      if (nmBorderFreePayPalResponse.getAltBuyerResponseVO().getBillToCity() != null) {
        sanitizedFieldValue =
                sanitizeFieldLength(nmBorderFreePayPalResponse.getAltBuyerResponseVO().getBillToCity(),
                        Integer.parseInt(localizationUtils.getExpPayPalFieldLength().get(BorderFreeConstants.PPX_CITY_STATE_FLD_LENGTH)));
        billingAddress.setCity(sanitizedFieldValue);
      }
      if (nmBorderFreePayPalResponse.getAltBuyerResponseVO().getBillToRegion() != null) {
        sanitizedFieldValue =
                sanitizeFieldLength(nmBorderFreePayPalResponse.getAltBuyerResponseVO().getBillToRegion(),
                        Integer.parseInt(localizationUtils.getExpPayPalFieldLength().get(BorderFreeConstants.PPX_CITY_STATE_FLD_LENGTH)));
        billingAddress.setProvince(sanitizedFieldValue);
        billingAddress.setState(sanitizedFieldValue);
      }
      if (nmBorderFreePayPalResponse.getAltBuyerResponseVO().getBillToPostalCode() != null) {
        sanitizedFieldValue =
                sanitizeFieldLength(nmBorderFreePayPalResponse.getAltBuyerResponseVO().getBillToPostalCode(),
                        Integer.parseInt(localizationUtils.getExpPayPalFieldLength().get(BorderFreeConstants.PPX_POSTAL_FLD_LENGTH)));
        billingAddress.setPostalCode(sanitizedFieldValue);
      }
      billingAddress.setCountry(nmBorderFreePayPalResponse.getAltBuyerResponseVO().getBillToCountryCode());
      if (nmBorderFreePayPalResponse.getAltBuyerResponseVO().getBillToPrimaryPhone() != null) {
        sanitizedFieldValue =
                sanitizeFieldLength(nmBorderFreePayPalResponse.getAltBuyerResponseVO().getBillToPrimaryPhone(),
                        Integer.parseInt(localizationUtils.getExpPayPalFieldLength().get(BorderFreeConstants.PPX_PHONE_FLD_LENGTH)));
        billingAddress.setPhoneNumber(sanitizedFieldValue);
      }
      paymentGroup.setPayPalEmail(nmBorderFreePayPalResponse.getAltBuyerResponseVO().getBillToEmail());
      billingAddress.setEmail(nmBorderFreePayPalResponse.getAltBuyerResponseVO().getBillToEmail());
      paymentGroup.setPropertyValue(ProfileProperties.Profile_email, nmBorderFreePayPalResponse.getAltBuyerResponseVO().getBillToEmail());
      paymentGroup.setBillingAddress(billingAddress);
    }
  }
  
  public void updateProfileShippingAddrToOrder(final NMOrderImpl order, final NMOrderManager orderMgr, final NMProfile profile) {
    final List<ShippingGroup> orderShippingGroups = order.getShippingGroups();
    for (final ShippingGroup shipAddress : orderShippingGroups) {
      final NMRepositoryContactInfo shippingAddress = (NMRepositoryContactInfo) ((HardgoodShippingGroup) shipAddress).getShippingAddress();
      final ContactInfo nmAddress = CheckoutAPI.getContactInfo(profile, ProfileProperties.Profile_shippingAddress);
      if (nmAddress != null) {
        shippingAddress.setFirstName(!StringUtils.isEmpty(nmAddress.getFirstName()) ? nmAddress.getFirstName() : "");
        shippingAddress.setLastName(!StringUtils.isEmpty(nmAddress.getLastName()) ? nmAddress.getLastName() : "");
        shippingAddress.setCountry(!StringUtils.isEmpty(nmAddress.getCountry()) ? nmAddress.getCountry() : "");
        shippingAddress.setAddress1(!StringUtils.isEmpty(nmAddress.getAddress1()) ? nmAddress.getAddress1() : "");
        shippingAddress.setAddress2(!StringUtils.isEmpty(nmAddress.getAddress2()) ? nmAddress.getAddress2() : "");
        shippingAddress.setCity(!StringUtils.isEmpty(nmAddress.getCity()) ? nmAddress.getCity() : "");
        shippingAddress.setPostalCode(!StringUtils.isEmpty(nmAddress.getPostalCode()) ? nmAddress.getPostalCode() : "");
        shippingAddress.setPhoneNumber(!StringUtils.isEmpty(nmAddress.getPhoneNumber()) ? nmAddress.getPhoneNumber() : "");
        shippingAddress.setEmail(!StringUtils.isEmpty(nmAddress.getEmailAddress()) ? nmAddress.getEmailAddress() : "");
        
      }
    }
    
  }
  
  // PayPal create PayPal card for international orders
  public Collection<Message> createPayPalCard(final NMOrderImpl order, final NMOrderManager orderMgr, final NMProfile profile, final String payPalPayerId) throws Exception {
    final MessageContainer messages = CheckoutComponents.getMessageContainer(getCurrentRequest());
    final LocalizationUtils localizationUtils = getLocalizationUtils();
    NMCreditCard paypalCard = null;
    final NMCreditCard defaultCard = profile.getDefaultCreditCard();
    final MutableRepository profileRepo = (MutableRepository) profile.getRepository();
    if ((defaultCard == null) || StringUtilities.isEmpty(defaultCard.getCreditCardType())) {
      paypalCard = defaultCard;
    }
    if (paypalCard == null) {
      final NMCommerceProfileTools profileTools = CheckoutComponents.getCommerceProfileTools();
      paypalCard = new NMCreditCard();
      paypalCard.setRepositoryItem(profileTools.createCreditCardItem(profile));
    }
    if (paypalCard.getBillingAddressItem() == null) {
      final RepositoryItem billingAddress = profileRepo.createItem(ProfileProperties.Profile_Desc_contactInfo);
      paypalCard.setPropertyValue(ProfileProperties.CreditCard_billingAddress, billingAddress);
    }
    paypalCard.setCreditCardType(BorderFreeConstants.INTL_PAY_PAL);
    paypalCard.setPayPalEmail(localizationUtils.getDefaultEmail());
    paypalCard.setPayPalPayerId(payPalPayerId);
    final NMAddress billingAddress = new NMAddress((MutableRepositoryItem) paypalCard.getBillingAddressItem());
    billingAddress.setAddress1(profile.getBillingAddress().getPropertyValue(ProfileProperties.Contact_address1).toString());
    billingAddress.setAddress2(profile.getBillingAddress().getPropertyValue(ProfileProperties.Contact_address2).toString());
    billingAddress.setCountry(profile.getBillingAddress().getPropertyValue(ProfileProperties.Contact_country).toString());
    billingAddress.setFirstName(profile.getBillingAddress().getPropertyValue(ProfileProperties.Contact_firstName).toString());
    billingAddress.setLastName(profile.getBillingAddress().getPropertyValue(ProfileProperties.Contact_lastName).toString());
    billingAddress.setPhoneNumber(profile.getBillingAddress().getPropertyValue(ProfileProperties.Contact_phoneNumber).toString());
    billingAddress.setPostalCode(profile.getBillingAddress().getPropertyValue(ProfileProperties.Contact_postalCode).toString());
    billingAddress.setCity(profile.getBillingAddress().getPropertyValue(ProfileProperties.Contact_city).toString());
    if (!StringUtils.isBlank(profile.getBillingAddress().getPropertyValue(ProfileProperties.Contact_state).toString())) {
      billingAddress.setState(profile.getBillingAddress().getPropertyValue(ProfileProperties.Contact_state).toString());
    }
    if (!StringUtils.isBlank(profile.getBillingAddress().getPropertyValue(ProfileProperties.Contact_province).toString())) {
      billingAddress.setProvince(profile.getBillingAddress().getPropertyValue(ProfileProperties.Contact_province).toString());
    }
    profile.setBillingAddress(billingAddress.getRepositoryItem());
    messages.addAll(CheckoutAPI.changeCreditCardOnOrder(order, orderMgr, profile, paypalCard));
    if (!messages.isEmpty()) {
      final Message message = new Message();
      message.setMsgText("We are unable to checkout with PayPal at this time.");
      message.setFieldId(BorderFreeConstants.GROUP_TOP);
      messages.add(message);
    }
    return messages;
  }
  
  /* International changes start - Set Inernational order values */
  @SuppressWarnings("unchecked")
  public boolean resetInternationalOrderProperties(final NMOrderImpl order, final NMBorderFreeGetQuoteResponse bfResponse) throws RepositoryException {
    boolean success = false;
    if (order.getCommerceItemCount() > 0) {
      final MutableRepository orderRepository = (MutableRepository) RepositoryUtils.getInstance().getOrderRepository();
      final List<NMCommerceItem> items = order.getCommerceItems();
      final Map<String, NMBorderFreeBasketItem> itemMap = bfResponse.getItemMap();
      for (final NMCommerceItem item : items) {
        NMBorderFreeBasketItem bfItem = itemMap.get(item.getCmosSKUId());
        if (bfItem == null) {
          // Promotional item. Do not throw error. Just save default values;
          // throw new RepositoryException("Not all commerce items are sent to Borderfree for pricing. Missing SKU from Cart : "+item.getCmosSKUId());
          bfItem = new NMBorderFreeBasketItem();
          bfItem.setQuantity((int) item.getQuantity());
        }
        MutableRepositoryItem priceInfo = (MutableRepositoryItem) item.getPropertyValue("internationalPriceItem");
        boolean isNewItem = false;
        if (priceInfo == null) {
          priceInfo = orderRepository.createItem("internationalPriceItem");
          isNewItem = true;
        }
        priceInfo.setPropertyValue("listPrice", bfItem.getListPrice());
        priceInfo.setPropertyValue("salePrice", bfItem.getListPrice());
        priceInfo.setPropertyValue("discount", bfItem.getDiscounts());
        priceInfo.setPropertyValue("extraHandling", bfItem.getExtraHandling());
        priceInfo.setPropertyValue("extraShipping", bfItem.getExtraShipping());
        priceInfo.setPropertyValue("fishAndWildLifeFee", bfItem.getFees());
        priceInfo.setPropertyValue("taxes", bfItem.getTaxes());
        priceInfo.setPropertyValue("duties", bfItem.getDuties());
        priceInfo.setPropertyValue("totalPrice", bfItem.getTotalPrice());
        if (isNewItem) {
          orderRepository.addItem(priceInfo);
        } else {
          orderRepository.updateItem(priceInfo);
        }
        item.setPropertyValue("internationalPriceItem", priceInfo);
        orderRepository.updateItem(item.getRepositoryItem());
      }
      MutableRepositoryItem intlOrdPriceInfo = (MutableRepositoryItem) order.getPropertyValue("internationalPriceItem");
      boolean isNewItem = false;
      if (intlOrdPriceInfo == null) {
        intlOrdPriceInfo = orderRepository.createItem("internationalPriceItem");
        isNewItem = true;
      }
      intlOrdPriceInfo.setPropertyValue("listPrice", bfResponse.getBasketTotalResponseVO().getTotalListPrice());
      intlOrdPriceInfo.setPropertyValue("salePrice", bfResponse.getBasketTotalResponseVO().getTotalSalePrice());
      intlOrdPriceInfo.setPropertyValue("discount", bfResponse.getBasketTotalResponseVO().getTotalItemDiscount());
      intlOrdPriceInfo.setPropertyValue("extraHandling", bfResponse.getBasketTotalResponseVO().getTotalHandling());
      intlOrdPriceInfo.setPropertyValue("extraShipping", bfResponse.getBasketTotalResponseVO().getTotalShipping());
      intlOrdPriceInfo.setPropertyValue("fishAndWildLifeFee", bfResponse.getBasketTotalResponseVO().getTotlFishWildLifeFee());
      intlOrdPriceInfo.setPropertyValue("taxes", bfResponse.getBasketTotalResponseVO().getTotalTaxes());
      intlOrdPriceInfo.setPropertyValue("duties", bfResponse.getBasketTotalResponseVO().getTotalDuties());
      intlOrdPriceInfo.setPropertyValue("totalPrice", bfResponse.getBasketTotalResponseVO().getTotalPrice());
      if (isNewItem) {
        orderRepository.addItem(intlOrdPriceInfo);
      } else {
        orderRepository.updateItem(intlOrdPriceInfo);
      }
      
      if (!StringUtils.isBlank(bfResponse.getBorderFreeCouponCode())) {
        order.setPropertyValue("bfCouponCode", bfResponse.getBorderFreeCouponCode());
      }
      // INT-1027 changes starts
      String orderShippingOption = null;
      if (null != order.getPropertyValue("shippingCarrierType")) {
        orderShippingOption = order.getPropertyValue("shippingCarrierType").toString();
      }
      if (StringUtils.isBlank(orderShippingOption) && (bfResponse.getSelectedShippingMethod() != null)) {
        order.setDeliveryPromiseMin(bfResponse.getSelectedShippingMethod().getDeliveryPromiseMinimum());
        order.setDeliveryPromiseMax(bfResponse.getSelectedShippingMethod().getDeliveryPromiseMaximum());
        order.setPropertyValue("shippingCarrierType", bfResponse.getSelectedShippingMethod().getShippingMethodId());
      }
      // INT-1027 changes ends
      // isInternationalOrder and fraudStatus will be set only after placing order.
      // order.setPropertyValue("fraudStatus", "");
      // order.setPropertyValue("isInternationalOrder", "");
      // order.setPropertyValue("merchantOrderId", "");
      if (!StringUtils.isBlank(bfResponse.getQuoteId())) {
        order.setPropertyValue("quoteIdUsed", bfResponse.getQuoteId());
      }
      order.setPropertyValue("lcpRuleId", bfResponse.getLcpRuleId());
      // order.setPropertyValue("dutiesPaymentMethod", bfResponse.getDutiesPaymentMethod());
      order.setPropertyValue("internationalCurrencyCode", bfResponse.getCurrencyCode());
      order.setPropertyValue("countryCode", bfResponse.getCountry());
      order.setPropertyValue("internationalPriceItem", intlOrdPriceInfo);
      if (!StringUtils.isBlank(bfResponse.getDutiesPaymentMethod())) {
        order.setDutiesPaymentMethod(bfResponse.getDutiesPaymentMethod());
      }
      success = true;
    }
    return success;
  }
  
  /**
   * deptQuantity restriction
   * @param order
   * @param orderMgr
   * @return
   * @throws CommerceException
   */
  
  public ResultBean checkDepartmentQuantityRestriction(final NMOrderImpl order){
	  
	    final List<Message> messages = new ArrayList<Message>();
	    ResultBean resultBean= new ResultBean();
	    final HashMap tempItems = new HashMap();	    
	    if(order !=null){
	    final List commerceItemList = order.getCommerceItems();	    
	    if(commerceItemList !=null && !commerceItemList.isEmpty()){
	    final Iterator commerceItemIterator = commerceItemList.iterator();
	    while (commerceItemIterator.hasNext()) {
	      try {
	        final NMCommerceItem commerceItem = (NMCommerceItem) commerceItemIterator.next();	        
	        if (commerceItem.getPriceInfo().getAmount() > 0) {
	          final String itemKey = commerceItem.getCmosSKUId();
	          if(!StringUtils.isBlank(itemKey)){
	          TempItem tempItem = (TempItem) tempItems.get(itemKey);	          
	          // if tempItem is null then we have not seen this catalog item before
	          if (tempItem == null) {
	            tempItem = new TempItem();
	            if(commerceItem.getQuantity() > 0){
	               tempItem.qtyWanted = new Long(commerceItem.getQuantity()).intValue();
	            }	            					
				if(!StringUtils.isBlank(commerceItem.getCmosItemCode()) && commerceItem.getProduct() != null && !StringUtils.isBlank(commerceItem.getProduct().getDepartment())){
					tempItem.cmosItemCode = commerceItem.getCmosItemCode();
					tempItem.deptCode = commerceItem.getDepartment();
		            tempItem.webDesignerName=commerceItem.getProduct().getWebDesignerName();	
				}
	            tempItem.commerceItems.add(commerceItem);
	            tempItems.put(itemKey, tempItem);
	          } else { // if we have already seen this item, then simply increase the requested quantity
	            tempItem.qtyWanted += new Long(commerceItem.getQuantity()).intValue();
	            tempItem.commerceItems.add(commerceItem);
	          }
	         }
	        }
	      } catch (final Exception e) {
	        CommonComponentHelper.getLogger().error("Error looping through the ShoppingCartQtyMap (vendor quantity) ", e);
	      }
	    }
	    final Iterator tempItemsIterator = tempItems.keySet().iterator();
	    final HashMap<String, Message> designerNameMap = new HashMap<String, Message>();
	    final HashMap<String, OmnitureData> omnitureDataMap = new HashMap<String, OmnitureData>();
		while (tempItemsIterator.hasNext()) {
			final String key = (String) tempItemsIterator.next();
			final TempItem tempItem = (TempItem) tempItems.get(key);
			if (!StringUtils.isBlank(tempItem.deptCode)) {
				RepositoryItem vendorQuantityData = CommonComponentHelper.getProdSkuUtil().getDeptQuantityData(tempItem.deptCode);
				if (vendorQuantityData != null) {
					int vendorquantity = (Integer) vendorQuantityData.getPropertyValue("quantity");
					if (tempItem.qtyWanted > vendorquantity) {
						OmnitureData  omnitureData= new OmnitureData();
						final Message message = MessageDefs.getMessage(MessageDefs.MSG_GenericFormErrorMessage);
						message.setMsgText("You may not purchase more than "+vendorquantity+" of any one item from "+tempItem.webDesignerName+ " at a time. Please update quantity to continue.");
						message.setError(false);
						message.setMsgId("vendorQuantity");
						omnitureData.setSkuQuantity(vendorquantity);
						omnitureData.setCmosItemCode(tempItem.cmosItemCode);
						designerNameMap.put(tempItem.webDesignerName, message);
						omnitureDataMap.put(tempItem.cmosItemCode, omnitureData);
					}
				}
			}
		}
	    resultBean.getMessages().addAll(designerNameMap.values());
	    resultBean.getOmnitureData().addAll(omnitureDataMap.values());
	    }
	    }
	    return resultBean;
  } 
  
//Inner class for Vendor Quantiry Restriction.
  class TempItem {
	    private int qtyWanted;
	    private final List<NMCommerceItem> commerceItems = new ArrayList<NMCommerceItem>();
	    private String cmosItemCode;
	    private String webDesignerName;		    
	    private String deptCode;
	  } 
  //Inner class for Vendor Quantiry Restriction.
  
  /* International changes end - Set Inernational order values */
  // 41279 - Multiple shipping addresses are not supported by International checkout.
  public boolean cleanupShippingGroupsForInternationalCheckout(final NMOrderImpl order, final OrderManager orderMgr) throws CommerceException {
    boolean cleanupDone = false;
    if (order.getShippingGroupCount() > 1) {
      if (CommonComponentHelper.getLogger().isLoggingDebug()) {
        CommonComponentHelper.getLogger().logDebug("Found multiple shipping groups on order : " + order.getId());
      }
      final ShippingGroupManager sgMgr = orderMgr.getShippingGroupManager();
      final List<ShippingGroup> shippingGroups = order.getShippingGroups();
      final ShippingGroup firstShippingGroup = shippingGroups.get(0);
      final List<String> sgsToRemove = new ArrayList();
      for (int i = 1; i < shippingGroups.size(); i++) {
        final ShippingGroup temp = shippingGroups.get(i);
        sgsToRemove.add(temp.getId());
      }
      for (final String strSgId : sgsToRemove) {
        sgMgr.removeShippingGroupFromOrder(order, strSgId);
      }
      final CommerceItemManager itemMgr = orderMgr.getCommerceItemManager();
      final List<CommerceItem> ciList = order.getCommerceItems();
      for (final CommerceItem ci : ciList) {
        if (ci.getShippingGroupRelationshipCount() <= 0) {
          itemMgr.addItemQuantityToShippingGroup(order, ci.getId(), firstShippingGroup.getId(), ci.getQuantity());
        }
      }
      orderMgr.updateOrder(order);
      cleanupDone = true;
      if (CommonComponentHelper.getLogger().isLoggingDebug()) {
        CommonComponentHelper.getLogger().logDebug("Shipping Group cleanup done on order : " + order.getId());
      }
    } else {
      cleanupDone = true;
    }
    if (CommonComponentHelper.getLogger().isLoggingDebug()) {
      CommonComponentHelper.getLogger().logDebug("ShipGroup Cleanup success ? " + cleanupDone);
    }
    return cleanupDone;
  }
  
}
