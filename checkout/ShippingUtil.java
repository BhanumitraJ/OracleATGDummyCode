package com.nm.commerce.checkout;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;

import atg.commerce.CommerceException;
import atg.commerce.gifts.GiftlistManager;
import atg.commerce.order.CommerceItem;
import atg.commerce.order.CommerceItemManager;
import atg.commerce.order.CommerceItemRelationship;
import atg.commerce.order.HardgoodShippingGroup;
import atg.commerce.order.Order;
import atg.commerce.order.OrderManager;
import atg.commerce.order.OrderTools;
import atg.commerce.order.RepositoryContactInfo;
import atg.commerce.order.ShippingGroup;
import atg.commerce.order.ShippingGroupCommerceItemRelationship;
import atg.commerce.order.ShippingGroupManager;
import atg.commerce.profile.CommerceProfileTools;
import atg.core.util.Address;
import atg.nucleus.Nucleus;
import atg.nucleus.logging.ApplicationLogging;
import atg.nucleus.logging.ClassLoggingFactory;
import atg.repository.MutableRepository;
import atg.repository.MutableRepositoryItem;
import atg.repository.Repository;
import atg.repository.RepositoryException;
import atg.repository.RepositoryItem;
import atg.repository.RepositoryView;
import atg.repository.rql.RqlStatement;
import atg.servlet.DynamoHttpServletRequest;
import atg.servlet.ServletUtil;
import atg.userprofiling.Profile;

import com.nm.ajax.checkout.addressbook.AddressUtils;
import com.nm.ajax.checkout.shipping.ShippingException;
import com.nm.ajax.checkout.utils.ComponentUtils;
import com.nm.ajax.checkout.utils.RepositoryUtils;
import com.nm.collections.CountryArray;
import com.nm.collections.ProvinceArray;
import com.nm.collections.ServiceLevel;
import com.nm.collections.ServiceLevelArray;
import com.nm.commerce.NMAddress;
import com.nm.commerce.NMCommerceItem;
import com.nm.commerce.NMProfile;
import com.nm.commerce.NMRepositoryContactInfo;
import com.nm.commerce.checkout.beans.ContactInfo;
import com.nm.commerce.checkout.beans.ShippingGroupBean;
import com.nm.commerce.order.NMOrderManager;
import com.nm.commerce.pagedef.model.CheckoutPageModel;
import com.nm.commerce.pagedef.model.UIElementConstants;
import com.nm.commerce.pricing.ExtraShippingData;
import com.nm.commerce.promotion.NMOrderImpl;
import com.nm.common.INMGenericConstants;
import com.nm.common.SmartPostServiceLevelSessionBean;
import com.nm.components.CommonComponentHelper;
import com.nm.estimateddeliverydate.vo.EstimatedDeliveryDateCmosShippingVO;
import com.nm.estimateddeliverydate.vo.EstimatedDeliveryDateServiceLevelVO;
import com.nm.formhandler.ShoppingCartHandler;
import com.nm.profile.AddressVerificationHelper;
import com.nm.profile.ProfileProperties;
import com.nm.repository.stores.Store;
import com.nm.sitemessages.Message;
import com.nm.sitemessages.MessageDefs;
import com.nm.utils.BrandSpecs;
import com.nm.utils.ExceptionUtil;
import com.nm.utils.NmoUtils;
import com.nm.utils.ProdCountryUtils;
import com.nm.utils.ProdSkuUtil;
import com.nm.utils.PromotionsHelper;
import com.nm.utils.ServiceLevelUtils;
import com.nm.utils.StringUtilities;
import com.nm.utils.SystemSpecs;

/**
 * Package access - outside of this package, methods should be accessed through CheckoutAPI.
 */
/* package */class ShippingUtil {
  
  private static ShippingUtil INSTANCE; // avoid static initialization
  private final AddressUtils addressUtils = new AddressUtils();
  
  private final ApplicationLogging mLogging = ClassLoggingFactory.getFactory().getLoggerForClass(ShippingUtil.class);

  // private constructor enforces singleton behavior
  private ShippingUtil() {}
  
  public static synchronized ShippingUtil getInstance() {
    INSTANCE = (INSTANCE == null) ? new ShippingUtil() : INSTANCE;
    return INSTANCE;
  }
  
  public boolean hasNonContinentalUSStates(final Order order, final List<Message> messages, final String currentServiceLevelCode) throws CommerceException {
    boolean updateShippingMethodsInShippingGroups = false;
    
    @SuppressWarnings("unchecked")
    final List<ShippingGroup> sgList = order.getShippingGroups();
    for (final ShippingGroup shippingGroup : sgList) {
      final String state = ShippingGroupUtil.getInstance().getAddress(shippingGroup).getState();
      final List<NMCommerceItem> items = ShippingGroupUtil.getInstance().getItems(shippingGroup);
      
      for (final NMCommerceItem item : items) {
        if (hasNonContinentalUSStates(item, state, messages, currentServiceLevelCode)) {
          updateShippingMethodsInShippingGroups = true;
        }
      }
    }
    
    return updateShippingMethodsInShippingGroups;
  }
  
  public boolean hasNonContinentalUSStates(final NMCommerceItem item, String state, final List<Message> messages, final String currentServiceLevelCode) {
    // SmartPost ABTest : Replacing hard coded service level values with a generalized code
    final ServiceLevelArray serviceLevelArray = CheckoutComponents.getServiceLevelArray();
    String itemServiceLevelCode = item.getServicelevel();
    final Map<String, ServiceLevel> serviceLevels = serviceLevelArray.getServiceLevelMap();
    final ServiceLevel itemServiceLevel = serviceLevels.get(itemServiceLevelCode);
    final ExtraShippingData extraShippingData = CheckoutComponents.getExtraShippingData();
    final List<String> extraShippingChargeStates = Arrays.asList(extraShippingData.getExtraShippingChargeStates());
    final List<String> nonContUsStates = Arrays.asList(extraShippingData.getNonContinentalUSStates());
    final SmartPostServiceLevelSessionBean smartPostServiceLevelSessionBean = CheckoutComponents.getSmartPostServiceLevelSessionBean(ServletUtil.getCurrentRequest());
    String selectedServiceLevel = smartPostServiceLevelSessionBean.getSmartPostSelectedServiceLevel();
    ServiceLevel serviceLevel = null;
    if (StringUtilities.isEmpty(selectedServiceLevel)) {
      selectedServiceLevel = serviceLevelArray.determineFreeShippingServiceLevel();
    }
    
    if (state == null) {
      state = INMGenericConstants.EMPTY_STRING;
    }
    
    if (StringUtils.isBlank(itemServiceLevelCode)) {
      itemServiceLevelCode = serviceLevelArray.determineFreeShippingServiceLevel();
    }
    
    final BrandSpecs brandSpecs = (BrandSpecs) Nucleus.getGlobalNucleus().resolveName("/nm/utils/BrandSpecs");
    for (final String nonContUsState : nonContUsStates) {
      if (nonContUsState.equals(state)
              && (itemServiceLevelCode.equals(ServiceLevel.SL1_SERVICE_LEVEL_TYPE) || (!brandSpecs.isEnableTwoDayForNonContinentalUSStates() && itemServiceLevelCode
                      .equals(ServiceLevel.SL2_SERVICE_LEVEL_TYPE)))) {
        if (!extraShippingChargeStates.contains(state)) {
          // SmartPost changes : Generalize the service level that is set to a Commerce item
          if (selectedServiceLevel.equals(ServiceLevel.SL1_SERVICE_LEVEL_TYPE) && StringUtilities.isNotEmpty(currentServiceLevelCode)
                  && !selectedServiceLevel.equalsIgnoreCase(currentServiceLevelCode)) {
            selectedServiceLevel = currentServiceLevelCode;
          } else if ((selectedServiceLevel.equals(ServiceLevel.SL1_SERVICE_LEVEL_TYPE) && CommonComponentHelper.getSystemSpecs().isSmartPostEnabled())
                  || !brandSpecs.isEnableTwoDayForNonContinentalUSStates()) {
            selectedServiceLevel = serviceLevelArray.determineFreeShippingServiceLevel();
          }
          item.setServicelevel(selectedServiceLevel);
          // SmartPost : getting the service level short description from the updated service level object for the selected service level code and the AB test group
          serviceLevel = serviceLevelArray.getServiceLevelByCode(selectedServiceLevel, serviceLevelArray.getServiceLevelGroupBasedOnAbTest());
          final Message msg = MessageDefs.getMessage(MessageDefs.MSG_ShippingWrongServiceLevel);
          msg.setMsgText("We're sorry, items shipping to " + state + " cannot be shipped " + itemServiceLevel.getShortDesc() + ", Shipping Method set back to " + serviceLevel.getShortDesc() + ".");
          msg.setFieldId("groupServiceLevels");
          
          if (!messages.contains(msg)) {
            messages.add(msg);
          }
          
          return true;
        }
      }
    }
    
    return false;
  }
  
  public boolean validateShipToCountries(final ShoppingCartHandler cart, final List<Message> messages) throws Exception {
    return validateShipToCountries(cart, messages, "");
  }
  
  public boolean validateShipToCountries(final ShoppingCartHandler cart, final List<Message> messages, final String fieldSuffix) throws Exception {
    
    final OrderUtil orderUtil = OrderUtil.getInstance();
    final NMOrderImpl order = cart.getNMOrder();
    final OrderManager orderManager = cart.getOrderManager();
    
    boolean errorFound = false;
    boolean atLeastOneValidCountry = false;
    boolean atLeastOneInvalidCountry = false;
    final Set<String> countryCodes = new HashSet<String>();
    final Set<String> restrictedCountryErrorMessages = new HashSet<String>();
    
    synchronized (order) {
      final List<ShippingGroupCommerceItemRelationship> rShipList = orderUtil.getShippingGroupCommerceItemRelationships(order, orderManager);
      final Iterator<ShippingGroupCommerceItemRelationship> rShipIterator = rShipList.iterator();
      
      while (rShipIterator.hasNext()) {
        final ShippingGroupCommerceItemRelationship rShip = rShipIterator.next();
        final NMCommerceItem nmci = (NMCommerceItem) rShip.getCommerceItem();
        final RepositoryItem prodRI = (RepositoryItem) nmci.getAuxiliaryData().getProductRef();
        final String prodName = (String) prodRI.getPropertyValue("displayName");
        
        final RepositoryContactInfo addr = ShippingGroupUtil.getInstance().getAddress(rShip.getShippingGroup());
        final MutableRepositoryItem mutItem = addr.getRepositoryItem();
        final Boolean flgCountyCodeVer = (Boolean) mutItem.getPropertyValue("flgCountyCodeVer");
        
        if ((flgCountyCodeVer == null) || (!flgCountyCodeVer.booleanValue())) {
          setAddressVerificationCountyCode(cart, mutItem);
        }
        
        if (StringUtilities.isEmpty(ShippingGroupUtil.getInstance().getAddress(rShip.getShippingGroup()).getCountry())) {
          continue;
        }
        
        final Boolean flgPoBox = ShippingGroupUtil.getInstance().getAddress(rShip.getShippingGroup()).getFlgPOBox();
        
        boolean isPOBox = false;
        
        if (flgPoBox != null) {
          isPOBox = flgPoBox.booleanValue();
        }
        
        final ProvinceArray provinceArray = CheckoutComponents.getProvinceArray();
        final CountryArray countryArray = CheckoutComponents.getCountryArray();
        final NMRepositoryContactInfo shippingAddress = ShippingGroupUtil.getInstance().getAddress(rShip.getShippingGroup());
        final String provinceCode = shippingAddress.getState();
        final String countryCode = shippingAddress.getCountry();
        final String provinceName = provinceArray.getProvince(countryCode, provinceCode).getShortDesc();
        final String countryName = countryArray.getCountry(countryCode).getShortName();
        
        countryCodes.add(countryCode);
        
        final ProdCountryUtils prodCountryUtils = CheckoutComponents.getProdCountryUtils();
        if (!prodCountryUtils.isProductShippableToCountry(prodRI, countryCode)) {
          errorFound = true;
          atLeastOneInvalidCountry = true;
          
          if (nmci.isGwpItem()) {
            removeGwpFromOrder(cart, nmci);
            final Message msg = MessageDefs.getMessage(MessageDefs.MSG_GWPCannotBeShippedToCountry);
            msg.setMsgText("We're sorry, the free gift with purchase, " + prodName + ", cannot be shipped to " + countryName + ", so it has been removed from your shopping bag.");
            messages.add(msg);
          }
        } else {
          atLeastOneValidCountry = true;
          
          if (!prodCountryUtils.isProductShippableToProvince(prodRI, countryCode, provinceCode)) {
            errorFound = true;
            
            if (nmci.isGwpItem()) {
              removeGwpFromOrder(cart, nmci);
              final Message msg = MessageDefs.getMessage(MessageDefs.MSG_GWPCannotBeShippedToCountry);
              msg.setMsgText("We're sorry, the free gift with purchase, " + prodName + ", cannot be shipped to " + provinceName + ", so it has been removed from your shopping bag.");
              messages.add(msg);
            }
          } else if (isPOBox && countryCode.equals("CA")) {
            errorFound = true;
            
            if (nmci.isGwpItem()) {
              removeGwpFromOrder(cart, nmci);
              final Message msg = MessageDefs.getMessage(MessageDefs.MSG_GWPCannotBeShippedToCountry);
              msg.setMsgText("We're sorry, the free gift with purchase, " + prodName + ", cannot be shipped to a P.O. Box in Canada, so it has been removed from your shopping bag.");
              messages.add(msg);
            }
          }
        }
      }
      
      final Iterator<String> restrictedCountryErrorMessagesIterator = restrictedCountryErrorMessages.iterator();
      
      while (restrictedCountryErrorMessagesIterator.hasNext()) {
        String errorMessage = restrictedCountryErrorMessagesIterator.next();
        
        if (atLeastOneValidCountry && atLeastOneInvalidCountry) {
          errorMessage = errorMessage + "  Please return to your shopping bag to remove this item.  If you would like to ship this item to a U.S. address, please place a separate order for it.";
        } else {
          errorMessage = errorMessage + "  Please enter a new shipping destination.";
        }
        
        Message msg = MessageDefs.getMessage(MessageDefs.MSG_ProductCannotBeShippedToCountry);
        msg.setMsgText(errorMessage);
        messages.add(msg);
        
        if (countryCodes.size() > 1) {
          errorFound = true;
          msg = MessageDefs.getMessage(MessageDefs.MSG_ProductCannotBeShippedToCountry);
          msg.setMsgText("We apologize we are unable to ship to both U.S. and Canadian addresses in a single order.  You will need to separate your items into two separate orders.");
          messages.add(msg);
        }
      }
    }
    
    return errorFound;
  }
  
  public void validateGwpMultiSkus(final Order order) {
    final NMOrderImpl nmOrderImpl = (NMOrderImpl) order;
    
    if (nmOrderImpl.getHasGwpMultiSkuPromo()) {
      final Map<String, Collection<String>> provinceMap = nmOrderImpl.getShipToProvinces();
      final Collection<String> countries = provinceMap.keySet();
      
      if (!provinceMap.isEmpty()) {
        final ProdCountryUtils prodCountryUtils = CheckoutComponents.getProdCountryUtils();
        final ProdSkuUtil prodSkuUtil = CommonComponentHelper.getProdSkuUtil();
        
        final Collection<String> productIds = nmOrderImpl.getGwpMultiSkuProductIds();
        final Iterator<String> productIdIterator = productIds.iterator();
        while (productIdIterator.hasNext()) {
          boolean foundValidCountryAndProvince = false;
          final String productId = productIdIterator.next();
          final RepositoryItem productRepositoryItem = prodSkuUtil.getMutableProductRepositoryItem(productId);
          
          if (productRepositoryItem != null) {
            
            // look through countries to see if there is at least
            // one valid country. If there is, then look through
            // the provinces to see if there is any valid provinces.
            final Iterator<String> countryIterator = countries.iterator();
            while (countryIterator.hasNext() && !foundValidCountryAndProvince) {
              final String countryCode = countryIterator.next();
              
              if (prodCountryUtils.isProductShippableToCountry(productRepositoryItem, countryCode)) {
                final Collection<String> provinces = provinceMap.get(countryCode);
                final Iterator<String> provinceIterator = provinces.iterator();
                while (provinceIterator.hasNext()) {
                  final String provinceCode = provinceIterator.next();
                  
                  if (prodCountryUtils.isProductShippableToProvince(productRepositoryItem, countryCode, provinceCode)) {
                    foundValidCountryAndProvince = true;
                    break;
                  }
                }
              }
            }
            
            // if we didn't find any valid
            // countries/provinces/states then remove this gwp from
            // the order's
            // multi sku map so that we don't prompt the customer to
            // select one.
            if (!foundValidCountryAndProvince) {
              nmOrderImpl.removeGwpMultiSkuForProduct(productId);
            }
          }
        }
      }
    }
  }
  
  /**
   * Removes a GWP (or GWP Select) product from the order and also removes the Promo Code from the order
   * 
   * @param request
   * @param response
   * @param order
   * @param commerceItem
   * @throws ServletException
   * @throws IOException
   * @throws CommerceException
   */
  public void removeGwpFromOrder(final ShoppingCartHandler cart, final NMCommerceItem commerceItem) throws Exception {
    final OrderUtil orderUtil = OrderUtil.getInstance();
    final OrderManager orderManager = cart.getOrderManager();
    final NMOrderImpl order = cart.getNMOrder();
    
    // Save this item in a session variable so that it is not re-added when the scenarios fire
    setRestrictFreeGWP(commerceItem);
    
    if (order instanceof NMOrderImpl) {
      final NMOrderImpl nmOrderImpl = order;
      
      final List<String> promoKeys = commerceItem.getSendCmosPromoKeyList();
      
      final Iterator<String> iterator = promoKeys.iterator();
      
      // Find the GWP or GWP Select promotion and remove it from the order
      // This really should be handled in another class like the
      // order class or the PromotionsHelper class.
      while (iterator.hasNext()) {
        final String key = iterator.next();
        
        RepositoryItem promotionRI = PromotionsHelper.getGwpRepositoryItem(key);
        
        // If promotionRI is null then the promo is not a GWP, so check
        // to see if it is a GWP Select
        if (promotionRI == null) {
          promotionRI = PromotionsHelper.getGwpSelectRepositoryItem(key);
          
          // If promotionRI is not null then this is a GWP Select promo
          // Remove the key from the order's promo names
          if (promotionRI != null) {
            
            // check to see if there are other commerceitems tied to this promotion
            // if there are then we don't want to remove the promo from the order itself.
            final List<NMCommerceItem> itemsTiedToThisPromo = nmOrderImpl.getCommerceItemsWithPromoKey(key);
            
            // remove this commerceitem from the list and then check to see if it's empty.
            // if it is empty then it's ok to remove the promo from the order
            itemsTiedToThisPromo.remove(commerceItem);
            
            if (itemsTiedToThisPromo.isEmpty()) {
              nmOrderImpl.setRemovePromoName(key);
              nmOrderImpl.removeAwardedPromotionByKey(key);
            }
            break;
          }
        } else {
          // Promo was a GWP so remove the promo values from the order.
          final String promoCode = (String) promotionRI.getPropertyValue("promoCodes");
          nmOrderImpl.setRemoveActivatedPromoCode(promoCode);
          nmOrderImpl.setRemoveUserActivatedPromoCode(promoCode);
          nmOrderImpl.setRemovePromoName(key);
          nmOrderImpl.removeAwardedPromotionByKey(key);
          // and just in case this is a multi sku, remove it from the order's map
          nmOrderImpl.removeGwpMultiSkuPromoMapEntry(key);
          break;
        }
      }
    }
    
    handleClearTagging(cart);
    orderUtil.removeItemFromOrder(cart, commerceItem.getId());
    orderManager.updateOrder(order);
  }
  
  public void handleClearTagging(final ShoppingCartHandler cart) {
  }
  
  public void setRestrictFreeGWP(final NMCommerceItem nmci) {
    // FIXME - do we still need to do this?
    /*****************
     * HashSet restrictedFreeGWPItems = (HashSet) request.getSession().getAttribute("restrictedFreeGWPItems");
     * 
     * if (restrictedFreeGWPItems == null) { restrictedFreeGWPItems = new HashSet(); }
     * 
     * restrictedFreeGWPItems.add(nmci.getAuxiliaryData().getProductId()); request.getSession().setAttribute("restrictedFreeGWPItems", restrictedFreeGWPItems);
     ****************/
  }
  
  public void setAddressVerificationCountyCode(final ShoppingCartHandler cart, final MutableRepositoryItem contactInfo) throws Exception {
    final AddressVerificationHelper addressVerificationHelper = cart.getAddressVerificationHelper();
    addressVerificationHelper.setAddressTaxCode(contactInfo);
  }
  
  @SuppressWarnings("unchecked")
  public boolean getIsShipToMultiple(final Order order) {
    if (order.getShippingGroupCount() <= 1) {
      return false;
    }
    
    final List<ShippingGroup> shippingGroups = order.getShippingGroups();
    int totalGroups = 0;
    if (!shippingGroups.isEmpty()) {
      
      for (int i = 0; i < shippingGroups.size(); i++) {
        if (ShippingGroupUtil.getInstance().getQty(shippingGroups.get(i)) > 0) {
          totalGroups++;
          if (totalGroups > 1) {
            break;
          }
        }
      }
      return totalGroups > 1;
    }
    
    return false;
  }
  
  public ShippingUtil updateShippingMethodsInShippingGroups(final NMProfile profile, final List<Message> messages, final ShippingGroup shippingGroup, final String currentServiceLevelCode) {
    final ShippingGroupUtil shippingGroupUtil = ShippingGroupUtil.getInstance();
    final List<NMCommerceItem> items = shippingGroupUtil.getItems(shippingGroup);
    final DynamoHttpServletRequest req = ServletUtil.getCurrentRequest();
    for (final NMCommerceItem item : items) {
      final String itemServiceLevelCode = item.getServicelevel();
      final String shipToName = shippingGroup.getDescription();
      final String shipToCountry = shippingGroupUtil.getCountry(shippingGroup);
      Set<ServiceLevel> itemServiceLevels = new HashSet<ServiceLevel>();
      final Boolean isPOBox = AddressUtil.getInstance().getIsShipAddressPOBox(profile, shipToName);
      Boolean disablePOBoxForSR = false;
      final String addressType = shippingGroupUtil.getAddress(shippingGroup).getAddressType();
      if (((addressType != null) && addressType.equalsIgnoreCase("P")) || isPOBox.booleanValue()) {
        disablePOBoxForSR = isShopRunnerOrder(req);
      }
      // SmartPost : setting the service level code based on amount and shipping country
      final ServiceLevelArray serviceLevelArray = CheckoutComponents.getServiceLevelArray();
      String serviceLevelCode = serviceLevelArray.determineFreeShippingServiceLevel();
      String freeShippingShortDesc = INMGenericConstants.EMPTY_STRING;
      // SmartPost : getting the service level short description from the updated service level object for the selected service level code and the AB test group
      ServiceLevel freeShippingServiceLevel = null;
      
      EstimatedDeliveryDateCmosShippingVO estimatedDeliveryDateCmosShippingVO = CheckoutComponents.getEstimatedDeliveryDateSession(req).getEstimatedDeliveryDateCmosShippingVO();
      if ((null != estimatedDeliveryDateCmosShippingVO) && NmoUtils.isNotEmptyCollection(estimatedDeliveryDateCmosShippingVO.getServiceLevels())) {
        List<EstimatedDeliveryDateServiceLevelVO> estimatedDeliveryDateServiceLevelVO = estimatedDeliveryDateCmosShippingVO.getServiceLevels();
        itemServiceLevels = CheckoutAPI.convertCmosServiceLevelResponseToServiceLevelObject(estimatedDeliveryDateServiceLevelVO);
        freeShippingServiceLevel =
                CheckoutAPI.getCmosServiceLevelByCode(StringUtilities.isNotEmpty(currentServiceLevelCode) ? currentServiceLevelCode : serviceLevelCode, estimatedDeliveryDateServiceLevelVO);
      } else if (StringUtilities.isNotEmpty(currentServiceLevelCode)) {
        itemServiceLevels = ServiceLevelUtil.getInstance().getCommerceItemValidServiceLevels(item, shipToCountry);
        freeShippingServiceLevel = serviceLevelArray.getServiceLevelByCode(currentServiceLevelCode, serviceLevelArray.getServiceLevelGroupBasedOnAbTest());
      } else {
        itemServiceLevels = ServiceLevelUtil.getInstance().getCommerceItemValidServiceLevels(item, shipToCountry);
        freeShippingServiceLevel = serviceLevelArray.getServiceLevelByCode(serviceLevelCode, serviceLevelArray.getServiceLevelGroupBasedOnAbTest());
      }
      
      if (null != freeShippingServiceLevel) {
        freeShippingShortDesc = freeShippingServiceLevel.getShortDesc();
      }
      // get lowest service level code
      if (!itemServiceLevels.isEmpty()) {
        serviceLevelCode = freeShippingServiceLevel.getCode();
      }
      
      if (StringUtils.isBlank(itemServiceLevelCode)) {
        item.setServicelevel(serviceLevelCode);
      } else if ((isPOBox.booleanValue() || disablePOBoxForSR)
              && (itemServiceLevelCode.equals(ServiceLevel.SL2_SERVICE_LEVEL_TYPE) || itemServiceLevelCode.equals(ServiceLevel.SL1_SERVICE_LEVEL_TYPE))) {
        if (StringUtilities.isNotEmpty(currentServiceLevelCode)
                && (currentServiceLevelCode.equalsIgnoreCase(ServiceLevel.SL2_SERVICE_LEVEL_TYPE) || currentServiceLevelCode.equalsIgnoreCase(ServiceLevel.SL1_SERVICE_LEVEL_TYPE))) {
          
          serviceLevelCode = serviceLevelArray.determineFreeShippingServiceLevel();
          freeShippingServiceLevel = serviceLevelArray.getServiceLevelByCode(serviceLevelCode, serviceLevelArray.getServiceLevelGroupBasedOnAbTest());
          freeShippingShortDesc = freeShippingServiceLevel.getShortDesc();
        }
        item.setServicelevel(serviceLevelCode);
        if (StringUtilities.isEmpty(freeShippingShortDesc)) {
          freeShippingShortDesc = ServiceLevelUtil.getInstance().getServiceLevelByCodeAndCountry(serviceLevelCode, shipToCountry).getShortDesc();
        }
        // SmartPost : In the error message text which is set below, the short description of the service level is taken dynamically based on the free shipping service level
        final Message msg = MessageDefs.getMessage(MessageDefs.MSG_ShippingPoBoxServiceLevel);
        if (disablePOBoxForSR && itemServiceLevelCode.equals(ServiceLevel.SL2_SERVICE_LEVEL_TYPE)) {
          msg.setMsgText("Items shipped to a P.O. Box cannot be shipped using ShopRunner. Shipping Method set back to " + freeShippingShortDesc + ".");
        } else if (isPOBox.booleanValue()) {
          msg.setMsgText("Items shipped to a P.O. Box can only be shipped " + freeShippingShortDesc + ". Shipping Method set back to " + freeShippingShortDesc + ".");
        } else {
          msg.setMsgText("");
        }
        
        msg.setFieldId("groupServiceLevels");
        if (!messages.contains(msg) && !msg.getMsgText().isEmpty()) {
          messages.add(msg);
        }
      }
    }
    
    return this;
  }
  
  public Boolean isShopRunnerOrder(final DynamoHttpServletRequest request) {
    final SystemSpecs systemSpecs = ComponentUtils.getInstance().getSystemSpecs();
    final Boolean shopRunnerEnabled = systemSpecs.isShopRunnerEnabled();
    final String shopRunnerToken = CheckoutAPI.getShopRunnerToken(request);
    final String international = request.getParameter("isInternationalUser");
    Boolean isSR = false;
    if (shopRunnerEnabled) {
      if (shopRunnerToken != null) {
        if (international == null) {
          isSR = true;
        }
      }
    }
    return isSR;
  }
  
  public ContactInfo getContactInfo(final NMProfile profile, final String profileProperty) {
    final RepositoryItem contactInfo = (MutableRepositoryItem) profile.getPropertyValue(profileProperty);
    return (AddressUtil.getInstance().profileAddressToContactInfo(contactInfo, profileProperty));
    
  }
  
  public Address contactInfoToAddress(final ContactInfo contactInfo) {
    final Address addr = new Address();
    addr.setAddress1(contactInfo.getAddressLine1());
    addr.setAddress2(contactInfo.getAddressLine2());
    addr.setCity(contactInfo.getCity());
    addr.setCountry(contactInfo.getCountry());
    addr.setFirstName(contactInfo.getFirstName());
    addr.setLastName(contactInfo.getLastName());
    addr.setPostalCode(contactInfo.getZip());
    addr.setPrefix(contactInfo.getTitleCode());
    addr.setState(contactInfo.getState());
    addr.setSuffix(contactInfo.getSuffixCode());
    
    return (addr);
  }
  
  public void updateProfileBillingAddress(final NMProfile profile, final ContactInfo addr) throws Exception {
    try {
      updateContactInfo(profile, addr, ProfileProperties.Profile_billingAddress);
    } catch (final RepositoryException e) {
      final String msg = "Error updating billing address for profile " + profile.getRepositoryId() + ": " + ExceptionUtil.getExceptionInfo(e);
      throw new Exception(msg, e);
    }
  }
  
  public void copyBillingAddrToShippingAddr(final NMProfile profile, final boolean isInternational) throws Exception {
    final ContactInfo addr = getContactInfo(profile, ProfileProperties.Profile_billingAddress);
    updateProfileShippingAddress(profile, addr, isInternational);
    return;
  }
  
  public void updateProfileShippingAddress(final NMProfile profile, final ContactInfo addr, final boolean isInternational) throws Exception {
    try {
      if (isInternational && (profile.getShippingAddress() != null)) {
        final String shippingCountry = profile.getCountryPreference();
        final String defaultAdrCountry = (String) profile.getShippingAddress().getPropertyValue(ProfileProperties.Contact_country);
        // To update default shipping address, selected country should be equal to Shipping Country.
        final boolean shouldUpdateDefault = !StringUtils.isBlank(defaultAdrCountry) && defaultAdrCountry.equals(shippingCountry);
        if (shouldUpdateDefault) {
          updateContactInfo(profile, addr, ProfileProperties.Profile_shippingAddress);
        } else {
          // Add default address as secondary address. Make new address as default shipping address.
          addNewDefaultAddressToProfile(profile, addr);
        }
      } else {
        updateContactInfo(profile, addr, ProfileProperties.Profile_shippingAddress);
      }
      
    } catch (final RepositoryException e) {
      final String msg = "Error updating shipping address for profile " + profile.getRepositoryId() + ": " + ExceptionUtil.getExceptionInfo(e);
      throw new Exception(msg, e);
    }
  }
  
  public void updateContactInfo(final NMProfile profile, final ContactInfo addr, final String profileProperty) throws RepositoryException {
    final MutableRepository profileRepos = (MutableRepository) profile.getRepository();
    MutableRepositoryItem contactInfo = (MutableRepositoryItem) profile.getPropertyValue(profileProperty);
    if (null == contactInfo) {
      contactInfo = profileRepos.createItem(ProfileProperties.Profile_Desc_contactInfo);
      profile.setPropertyValue(profileProperty, contactInfo);
    }
    
    AddressUtil.getInstance().copyFieldsToProfileAddress(addr, contactInfo);
    profileRepos.updateItem(contactInfo);
  }
  
  public void updateShippingGroupAddress(final ShoppingCartHandler cart, final NMProfile profile, final Order order, final String sgId, final ContactInfo contactInfo, final boolean isInternational)
          throws Exception {
    final AddressUtil addressUtil = AddressUtil.getInstance();
    final List<ShippingGroup> shipGroups = order.getShippingGroups();
    for (final Iterator<ShippingGroup> i = shipGroups.iterator(); i.hasNext();) {
      final HardgoodShippingGroup sg = (HardgoodShippingGroup) i.next();
      if (sg.getId().equals(sgId) && (!isShipToStoreShippingGroup(sg) && !isInternational)) {
        final String addrNickName = sg.getDescription();
        String newNickName = null;
        if (isInternational && (profile.getShippingAddress() != null)) {
          final String shippingCountry = profile.getCountryPreference();
          final String defaultAdrCountry = (String) profile.getShippingAddress().getPropertyValue(ProfileProperties.Contact_country);
          // To update default shipping address, selected country should be equal to Shipping Country.
          final boolean shouldUpdateDefault = !StringUtils.isBlank(defaultAdrCountry) && contactInfo.getCountry().equals(shippingCountry);
          if (shouldUpdateDefault) {
            newNickName = addressUtil.updateAddress(cart, profile, addrNickName, contactInfo, true);
          } else {
            // Add default address as secondary address. Make new address as default shipping address.
            newNickName = addNewDefaultAddressToProfile(profile, contactInfo);
          }
        } else {
          newNickName = addressUtil.updateAddress(cart, profile, addrNickName, contactInfo, true);
        }
        final NMRepositoryContactInfo addr = (NMRepositoryContactInfo) sg.getShippingAddress();
        AddressUtil.getInstance().copyFieldsToOrderAddress(contactInfo, addr.getRepositoryItem());
        sg.setDescription(newNickName);
        return;
      }
    }
  }
  
  public void updateShippingGroupAddressPayPal(final ShoppingCartHandler cart, final NMProfile profile, final Order order, final String sgId, final ContactInfo contactInfo) throws Exception {
    final AddressUtil addressUtil = AddressUtil.getInstance();
    final List<ShippingGroup> shipGroups = order.getShippingGroups();
    for (final Iterator<ShippingGroup> i = shipGroups.iterator(); i.hasNext();) {
      final HardgoodShippingGroup sg = (HardgoodShippingGroup) i.next();
      if (sg.getId().equals(sgId)) {
        final String addrNickName = sg.getDescription();
        String newNickName = null;
        newNickName = addressUtil.updateAddress(cart, profile, addrNickName, contactInfo, false);
        final NMRepositoryContactInfo addr = (NMRepositoryContactInfo) sg.getShippingAddress();
        AddressUtil.getInstance().copyFieldsToOrderAddress(contactInfo, addr.getRepositoryItem());
        sg.setDescription(newNickName);
        return;
      }
    }
  }
  
  @SuppressWarnings("unchecked")
  public String addNewDefaultAddressToProfile(final NMProfile profile, final ContactInfo addr) {
    String nickname = "";
    try {
      nickname = AddressUtil.getInstance().addNewAddress(profile, addr);
      /* Make the default address switch */
      final MutableRepositoryItem newSecondaryAddress = (MutableRepositoryItem) profile.getPropertyValue(ProfileProperties.Profile_shippingAddress);
      final Map<String, MutableRepositoryItem> secondaryAddresses = (Map<String, MutableRepositoryItem>) profile.getPropertyValue(ProfileProperties.Profile_secondaryAddresses);
      final RepositoryItem newShippingAddress = secondaryAddresses.get(nickname);
      boolean oldDefaultAddressExists = false;
      if ((null != newSecondaryAddress)
              && ((null != newSecondaryAddress.getPropertyValue("address1")) && (null != newSecondaryAddress.getPropertyValue("lastName"))
                      && (null != newSecondaryAddress.getPropertyValue("firstName")) && (null != newSecondaryAddress.getPropertyValue("state")))) {
        oldDefaultAddressExists = true;
      }
      
      String shipAddressNickname = null;
      if (oldDefaultAddressExists) {
        shipAddressNickname = addressUtils.createNickName(newSecondaryAddress);
      }
      
      if (newShippingAddress != null) {
        profile.setPropertyValue(ProfileProperties.Profile_shippingAddress, newShippingAddress);
      }
      if (secondaryAddresses != null) {
        secondaryAddresses.remove(nickname);
        if (oldDefaultAddressExists) {
          secondaryAddresses.put(shipAddressNickname, newSecondaryAddress);
        }
      }
    } catch (final Exception e) {
      // Ignore the exception as it is been done for all other address changes.
    }
    return nickname;
  }
  
  public boolean orderContainsShippingAddress(final Order order) {
    final List<ShippingGroup> groups = order.getShippingGroups();
    if (getNonWishlistShippingGroupCount(groups) != 1) {
      // if there is not a single shipping group they all must have addresses
      return (true);
    }
    
    Address address = null;
    for (int pos = 0; pos < groups.size(); pos++) {
      if (!isWishlistShippingGroup(groups.get(pos)) || !isShipToStoreShippingGroup(groups.get(pos))) {
        final HardgoodShippingGroup sg = (HardgoodShippingGroup) groups.get(pos);
        address = sg.getShippingAddress();
        break;
      }
    }
    
    return ((null != address) && addressUtils.doesAddressHaveRequiredFields(address));
  }
  
  /**
   * Check ship to country is same as profile preferred country.
   * 
   * @param order
   *          order object
   * @param profile
   *          profile object
   * @return boolean indicating order ship to equals preferred country.
   */
  @SuppressWarnings("unchecked")
  public boolean isInternationalOrderShipToValid(final Order order, final NMProfile profile) {
    final List<ShippingGroup> groups = order.getShippingGroups();
    Address address = null;
    String shipToCounty = null;
    for (int pos = 0; pos < groups.size(); pos++) {
      if (!isWishlistShippingGroup(groups.get(pos))) {
        final HardgoodShippingGroup sg = (HardgoodShippingGroup) groups.get(pos);
        address = sg.getShippingAddress();
        if (address != null) {
          shipToCounty = address.getCountry();
        }
        break;
      }
    }
    final String preferredCountry = profile.getCountryPreference();
    return StringUtils.equals(shipToCounty, preferredCountry);
  }
  
  public boolean orderContainsWishlistItem(final Order order) {
    final List<ShippingGroup> groups = order.getShippingGroups();
    boolean isOrderContainsWishlistItem = false;
    for (final ShippingGroup group : groups) {
      if (isWishlistShippingGroup(group)) {
        isOrderContainsWishlistItem = true;
        break;
      }
    }
    return isOrderContainsWishlistItem;
  }
  
  public boolean orderContainsShipToStoreEligibleCommerceItem(final CheckoutPageModel pageModel) {
    final NMOrderImpl nmOrder = pageModel.getOrder();
    boolean shipToStore = false;
    if (null != nmOrder) {
      @SuppressWarnings("unchecked")
      final List<NMCommerceItem> items = nmOrder.getCommerceItems();
      if (null != items) {
        for (final NMCommerceItem item : items) {
          final NMCommerceItem commerceItem = item;
          if (commerceItem.getIsShipToStoreEligible()) {
            shipToStore = true;
            break;
          }
        }
      }
    }
    return shipToStore;
  }
  
  public List<ShippingGroup> getNonWishlistShippingGroups(final List<ShippingGroup> groups) {
    final List<ShippingGroup> nonWishlistGroups = new ArrayList<ShippingGroup>();
    for (final ShippingGroup group : groups) {
      if (!isWishlistShippingGroup(group) && !isShipToStoreShippingGroup(group)) {
        nonWishlistGroups.add(group);
      }
    }
    return nonWishlistGroups;
  }
  
  public int getNonWishlistShippingGroupCount(final List<ShippingGroup> groups) {
    return getNonWishlistShippingGroups(groups).size();
  }
  
  public boolean isWishlistShippingGroup(final ShippingGroup sg) {
    boolean isWishlist = false;
    final List<CommerceItemRelationship> cirList = sg.getCommerceItemRelationships();
    if (!cirList.isEmpty()) {
      // if one of the items is a wishlist item, then all items are wishlist items
      final CommerceItemRelationship cir = cirList.get(0);
      final NMCommerceItem ci = (NMCommerceItem) cir.getCommerceItem();
      if ((ci.getRegistryId() != null) && !ci.getRegistryId().equals("")) {
        isWishlist = true;
      }
    }
    return isWishlist;
  }
  
  public boolean isShipToStoreShippingGroup(final ShippingGroup sg) {
    final HardgoodShippingGroup group = (HardgoodShippingGroup) sg;
    Boolean value = (group.getPropertyValue("shipToStoreNumber") != null);
    
    if (value) {
      final List<CommerceItemRelationship> items = sg.getCommerceItemRelationships();
      if ((items == null) || (items.size() == 0)) {
        value = false;
        group.setPropertyValue("shipToStoreNumber", null);
      } else {
        for (final CommerceItemRelationship ciRel : items) {
          final NMCommerceItem ci = (NMCommerceItem) ciRel.getCommerceItem();
          if (StringUtilities.isEmpty(ci.getPickupStoreNo())) {
            value = false;
            group.setPropertyValue("shipToStoreNumber", null);
            break;
          }
        }
      }
    }
    
    return value;
  }
  
  public List<ShippingGroup> getShippingGroupsForAddress(final Order order, final String addrId) {
    final OrderUtil orderUtil = OrderUtil.getInstance();
    final List<ShippingGroup> sg = new ArrayList<ShippingGroup>();
    final Map<String, ShippingGroup> orderGroups = orderUtil.getOrderShippingGroups(order);
    for (final Iterator<ShippingGroup> i = orderGroups.values().iterator(); i.hasNext();) {
      final ShippingGroup group = i.next();
      if (addrId.equals(group.getDescription())) {
        sg.add(group);
      }
    }
    
    return (sg);
  }
  
  public void refreshShippingGroupServiceLevel(final NMOrderImpl order) throws Exception {
    @SuppressWarnings("unchecked")
    final List<ShippingGroup> sgs = order.getShippingGroups();
    for (int p = 0; p < sgs.size(); p++) {
      final String sgId = ((ShippingGroup) order.getShippingGroups().get(p)).getId();
      final String serviceLevel = getServiceLevelForShippingGroup(order, sgId);
      final ServiceLevel[] serviceLevels = getValidServiceLevelsForShippingGroup(order, sgId);
      String newServiceLevel = serviceLevel;
      for (int pos = 0; pos < serviceLevels.length; pos++) {
        final ServiceLevel sl = serviceLevels[pos];
        newServiceLevel = sl.getCode();
        if (sl.getCode().equals(serviceLevel)) {
          break;
        }
      }
      setShippingServiceLevel(order, newServiceLevel, sgId);
    }
  }
  
  public String getServiceLevelForShippingGroup(final Order order, final String sgId) throws RepositoryException {
    String serviceLevel = "";
    final ServiceLevel[] sl = getValidServiceLevelsForShippingGroup(order, sgId);
    if (sl.length == 1) {
      return sl[0].getCode();
    }
    
    @SuppressWarnings("unchecked")
    final List<ShippingGroup> groups = order.getShippingGroups();
    for (final Iterator<ShippingGroup> sgIter = groups.iterator(); sgIter.hasNext();) {
      final HardgoodShippingGroup sg = (HardgoodShippingGroup) sgIter.next();
      if (StringUtilities.isEmpty(sgId) || sg.getId().equals(sgId)) {
        final String shipToCountry = sg.getShippingAddress().getCountry();
        @SuppressWarnings("unchecked")
        final List<CommerceItemRelationship> items = sg.getCommerceItemRelationships();
        for (final Iterator<CommerceItemRelationship> itemIter = items.iterator(); itemIter.hasNext();) {
          final CommerceItemRelationship ciRel = itemIter.next();
          final NMCommerceItem ci = (NMCommerceItem) ciRel.getCommerceItem();
          final Set<String> prodValid = getValidServiceLevels(shipToCountry, ci);
          if (prodValid.size() > 1) {
            serviceLevel = ci.getServicelevel();
            break;
          }
        }
      }
      if (!serviceLevel.equals("")) {
        break;
      }
    }
    
    return serviceLevel;
  }
  
  public void setShippingServiceLevel(final Order order, final String serviceLevel, final String sgId) {
    @SuppressWarnings("unchecked")
    final List<ShippingGroup> groups = order.getShippingGroups();
    for (final Iterator<ShippingGroup> sgIter = groups.iterator(); sgIter.hasNext();) {
      final HardgoodShippingGroup sg = (HardgoodShippingGroup) sgIter.next();
      if (StringUtilities.isEmpty(sgId) || sg.getId().equals(sgId)) {
        final String shipToCountry = sg.getShippingAddress().getCountry();
        @SuppressWarnings("unchecked")
        final List<CommerceItemRelationship> items = sg.getCommerceItemRelationships();
        for (final Iterator<CommerceItemRelationship> itemIter = items.iterator(); itemIter.hasNext();) {
          final CommerceItemRelationship ciRel = itemIter.next();
          final NMCommerceItem ci = (NMCommerceItem) ciRel.getCommerceItem();
          final Set<String> prodValid = getValidServiceLevels(shipToCountry, ci);
          if (prodValid.contains(serviceLevel)) {
            ci.setServicelevel(serviceLevel);
          } else {
            if (!prodValid.isEmpty()) {
              final Iterator<String> it = prodValid.iterator();
              ci.setServicelevel(it.next());
            }
          }
        }
      }
    }
  }
  
  public ServiceLevel[] getValidServiceLevelsForShippingGroup(final Order order, final String sgId) throws RepositoryException {
    ShippingGroup sg = null;
    @SuppressWarnings("unchecked")
    final List<ShippingGroup> shippingGroups = order.getShippingGroups();
    for (final Iterator<ShippingGroup> i = shippingGroups.iterator(); i.hasNext();) {
      final ShippingGroup group = i.next();
      if ((group != null) && group.getId().equals(sgId)) {
        
        sg = group;
        break;
      }
    }
    String shipToCountry = null;
    if (sg != null) {
      shipToCountry = ((HardgoodShippingGroup) sg).getShippingAddress().getCountry();
    } else {
      System.out.println("ShippingUtils: getValidServiceLevelsForShippingGroup - sgId: " + sgId + " was not found on order: " + order.getId());
    }
    
    return getValidServiceLevelsForShippingGroup(order, sgId, shipToCountry);
  }
  
  public ServiceLevel[] getValidServiceLevelsForShippingGroup(final Order order, final String sgId, String shipToCountry) throws RepositoryException {
    ShippingGroup sg = null;
    @SuppressWarnings("unchecked")
    final List<ShippingGroup> shippingGroups = order.getShippingGroups();
    for (final Iterator<ShippingGroup> i = shippingGroups.iterator(); i.hasNext();) {
      final ShippingGroup group = i.next();
      if ((group != null) && group.getId().equals(sgId)) {
        sg = group;
        break;
      }
    }
    
    if (null == sg) {
      return (new ServiceLevel[0]);
    }
    
    if (StringUtilities.isEmpty(shipToCountry)) {
      shipToCountry = ((HardgoodShippingGroup) sg).getShippingAddress().getCountry();
      if (StringUtilities.isEmpty(shipToCountry)) {
        shipToCountry = "US"; // default to US if shipping address has not been entered and a country was not provided
      }
    }
    
    final List<Set<String>> serviceLevelSets = new ArrayList<Set<String>>();
    
    @SuppressWarnings("unchecked")
    final List<CommerceItemRelationship> items = sg.getCommerceItemRelationships();
    for (final Iterator<CommerceItemRelationship> i = items.iterator(); i.hasNext();) {
      final CommerceItemRelationship itemRel = i.next();
      final CommerceItem ci = itemRel.getCommerceItem();
      final Set<String> prodValid = getValidServiceLevels(shipToCountry, (NMCommerceItem) ci);
      serviceLevelSets.add(prodValid);
    }
    
    return getValidServiceLevelDesc(order, getValidServiceLevels(order, serviceLevelSets), shipToCountry);
  }
  
  public ServiceLevel[] getValidServiceLevelDesc(final Order order, final ServiceLevel[] serviceLevels, final String countryCode) throws RepositoryException {
    if (StringUtilities.isEmpty(countryCode) || "US".equalsIgnoreCase(countryCode)) {
      return serviceLevels;
    }
    
    String query = null;
    final Object[] params = new Object[] {countryCode};
    query = "((code = \"SL1\") or (code = \"SL2\") or (code = \"SL3\") or (code = \"SL4\")) and (countryCode = ?0)";
    final Repository slRepos = RepositoryUtils.getInstance().getServiceLevelRepository();
    final RepositoryView view = slRepos.getView("international_service_level");
    final RqlStatement statement = RqlStatement.parseRqlStatement(query);
    final RepositoryItem[] items = statement.executeQuery(view, params);
    
    int foreignPos = 0;
    final ServiceLevel[] foreignServiceLevels = new ServiceLevel[serviceLevels.length];
    if (items != null) {
      for (int i = 0; i < items.length; i++) {
        final ServiceLevel temp = new ServiceLevel();
        temp.setCode((String) items[i].getPropertyValue("code"));
        
        for (int p = 0; p < serviceLevels.length; p++) {
          if (temp.getCode().equals(serviceLevels[p].getCode())) {
            temp.setShortDesc((String) items[i].getPropertyValue("shortDesc"));
            temp.setLongDesc((String) items[i].getPropertyValue("longDesc"));
            temp.setAmount(((Double) items[i].getPropertyValue("amount")).doubleValue());
            foreignServiceLevels[foreignPos++] = temp;
            break;
          }
        }
      }
    }
    
    if (((NMOrderImpl) order).hasShippingPromotion() && (foreignServiceLevels.length > 0)) {
      final ServiceLevel sl = foreignServiceLevels[foreignServiceLevels.length - 1];
      sl.setShortDesc("Promotional");
      sl.setLongDesc("Promotional");
      sl.setAmount(0);
    }
    return foreignServiceLevels;
  }
  
  public Set<String> getValidServiceLevels(final String shipToCountry, final NMCommerceItem ci) {
    final Set<String> valid = new TreeSet<String>();
    final boolean flgDropShip = ((Boolean) ((RepositoryItem) ci.getAuxiliaryData().getCatalogRef()).getPropertyValue("flgDropShip")).booleanValue();
    // WR 41198 - retrieve service levels for drop ship items
    if (!flgDropShip || ci.getProduct().getIsDropShipMerchTypeEligibleToDisplayServiceLevels()) {
      final RepositoryItem product = (RepositoryItem) ci.getAuxiliaryData().getProductRef();
      final ServiceLevelUtils serviceLevelUtils = getServiceLevelUtils();
      serviceLevelUtils.setShipToCountry(shipToCountry);
      serviceLevelUtils.setProductRepositoryItem(product);
      final ServiceLevel[] serviceLevels = serviceLevelUtils.getValidServiceLevels();
      final int serviceLevelslength = serviceLevels.length;
      for (int pos = 0; pos < serviceLevelslength; pos++) {
        valid.add(serviceLevels[pos].getCode());
      }
    }
    return (valid);
  }
  
  public ServiceLevel[] getValidServiceLevels(final Order order, final List<Set<String>> serviceLevelSets) throws RepositoryException {
    final int numSets = serviceLevelSets.size();
    if (numSets == 0) {
      return (new ServiceLevel[0]);
    }
    Set<String> intersect = null;
    // calculate the intersect of all the sets containing more that one service level
    for (int i = 0; i < numSets; ++i) {
      final Set<String> currSet = serviceLevelSets.get(i);
      if (currSet.size() > 1) {
        if (null == intersect) {
          intersect = new TreeSet<String>(currSet);
        } else {
          intersect.retainAll(currSet);
        }
      }
    }
    
    if ((null == intersect) || (intersect.size() == 0)) {
      // the intersect of all sets with more than one SL is empty
      // so we will calculate the intersect of all sets
      intersect = null;
      for (int i = 0; i < numSets; ++i) {
        final Set<String> currSet = serviceLevelSets.get(i);
        if (!currSet.isEmpty()) {
          if (null == intersect) {
            intersect = new TreeSet<String>(currSet);
          } else {
            intersect.retainAll(currSet);
          }
        }
      }
    }
    
    if ((null == intersect) || (intersect.size() == 0)) {
      return (new ServiceLevel[0]);
    }
    
    final Repository serviceLevelRepos = RepositoryUtils.getInstance().getServiceLevelRepository();
    
    final ArrayList<ServiceLevel> validLevels = new ArrayList<ServiceLevel>();
    
    for (final Iterator<String> i = intersect.iterator(); i.hasNext();) {
      final String slCode = i.next();
      final ServiceLevel sl = new ServiceLevel();
      sl.setCode(slCode);
      
      final RepositoryItem item = serviceLevelRepos.getItem(slCode, "service_level");
      if (null != item) {
        sl.setAmount(((Double) item.getPropertyValue("amount")).doubleValue());
        sl.setLongDesc((String) item.getPropertyValue("longDesc"));
        sl.setShortDesc((String) item.getPropertyValue("shortDesc"));
      }
      
      validLevels.add(sl);
    }
    
    final PromotionsHelper promotionsHelper = ComponentUtils.getInstance().getPromotionsHelper();
    
    // service levels need to remain in the original order
    if (((NMOrderImpl) order).hasShippingPromotion()) {
      String serviceLevelCode = null;
      // SmartPost : Display Free Shipping option as the first among all the service levels listed on Order review page
      final String abTestGroup = CheckoutComponents.getServiceLevelArray().getServiceLevelABTestGroup();
      if (CommonComponentHelper.getSystemSpecs().isSmartPostEnabled() && abTestGroup.equalsIgnoreCase(UIElementConstants.AB_TEST_SERVICE_LEVEL_GROUPS)
              && intersect.contains(ServiceLevel.SL4_SERVICE_LEVEL_TYPE)) {
        serviceLevelCode = ServiceLevel.SL4_SERVICE_LEVEL_TYPE;
      } else if (intersect.contains(ServiceLevel.SL3_SERVICE_LEVEL_TYPE)) {
        serviceLevelCode = ServiceLevel.SL3_SERVICE_LEVEL_TYPE;
      } else if (intersect.contains(ServiceLevel.SL2_SERVICE_LEVEL_TYPE)) {
        serviceLevelCode = ServiceLevel.SL2_SERVICE_LEVEL_TYPE;
      } else if (intersect.contains(ServiceLevel.SL1_SERVICE_LEVEL_TYPE)) {
        serviceLevelCode = ServiceLevel.SL1_SERVICE_LEVEL_TYPE;
      }
      
      if (serviceLevelCode != null) {
        for (int pos = validLevels.size() - 1; pos >= 0; pos++) {
          final ServiceLevel serviceLevel = validLevels.get(pos);
          if (serviceLevelCode.equals(serviceLevel.getCode())) {
            if (promotionsHelper.hasActiveShippingPromo(order)) {
              serviceLevel.setAmount(0);
              serviceLevel.setShortDesc("Promotional");
              serviceLevel.setLongDesc("Promotional");
            } else {
              // promos like generic pass thru should be transparent to the customer on the front end
              // Do not override the service level descriptions and amount in these cases
              
            }
            break;
          }
        }
      }
    }
    
    return validLevels.toArray(new ServiceLevel[0]);
  }
  
  // nmts5 had to override for gift registry so a customer can ship to an address not associated with their profile
  // pProfileAddressName = giftlistId for items that are being sent to gift registry addresses
  public void copyProfileAddressToShippingGroupAddress(final String pProfileAddressName, final ShippingGroup pShippingGroup, final Profile profile, final Order order, final OrderManager orderMgr,
          final ShoppingCartHandler cart) throws Exception {
    final CommerceProfileTools commProfileTools = cart.getCommerceProfileTools();
    final atg.core.util.Address shippingGroupAddress = ((HardgoodShippingGroup) pShippingGroup).getShippingAddress();
    final MutableRepositoryItem profileAddress = (MutableRepositoryItem) commProfileTools.getProfileAddress(profile, pProfileAddressName);
    
    buildShipToList(profile, order, orderMgr);
    
    if (profileAddress != null) {
      final HardgoodShippingGroup shippingGroup = (HardgoodShippingGroup) pShippingGroup;
      final NMRepositoryContactInfo address = (NMRepositoryContactInfo) shippingGroup.getShippingAddress();
      address.setFlgGiftReg(new Boolean(false));
      address.setTransGiftRegId("");
      orderMgr.getOrderTools();
      OrderTools.copyAddress(profileAddress, shippingGroupAddress);
      // System.out.println("shippingGroup verification flag: " +
      // shippingGroupAddress);
      // System.out.println("profile verification flag: " +
      // profileAddress.getPropertyValue("verificationFlag"));
    } else {
      // nmts5 had to change what we are doing if the address does not
      // exist on the profile for gift reg
      final NMProfile theNMProfile = ((NMProfile) profile);
      final Map<String, String> tempMap = theNMProfile.getViewedRegistriesList();
      // Map tempMap = new
      // HashMap(theNMProfile.getViewedRegistriesList());
      
      final String theValue = tempMap.get(pProfileAddressName);
      
      if ((theValue != null) && !theValue.trim().equals("")) {
        final Repository profileRepository = (Repository) atg.nucleus.Nucleus.getGlobalNucleus().resolveName("/atg/userprofiling/ProfileAdapterRepository");
        final RepositoryItem Contactinfo = profileRepository.getItem(theValue, "contactInfo");
        final HardgoodShippingGroup shippingGroup = (HardgoodShippingGroup) pShippingGroup;
        final NMRepositoryContactInfo address = (NMRepositoryContactInfo) shippingGroup.getShippingAddress();
        
        address.setFlgGiftReg(new Boolean(true));
        address.setTransGiftRegId(pProfileAddressName);
        
        final GiftlistManager giftMgr = ComponentUtils.getInstance().getGiftlistManager();
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
        // end theValue is not null
      } else {
        if ((pProfileAddressName != null) && !pProfileAddressName.equals(pShippingGroup.getId())) {
          throw new ShippingException("Profile address " + " not found for profile " + profile.getRepositoryId());
        }
      }
    }
    
    pShippingGroup.setDescription(pProfileAddressName);
  }
  
  public void buildShipToList(final Profile profile, final Order order, final OrderManager orderMgr) throws Exception {
    final NMProfile theNMProfile = ((NMProfile) profile);
    final HashMap<String, String> glKeySetMap = new HashMap<String, String>(theNMProfile.getViewedRegistriesList());
    
    final List<ShippingGroupCommerceItemRelationship> rShipList = OrderUtil.getInstance().getShippingGroupCommerceItemRelationships(order, orderMgr);
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
      final Repository giftlistRepos = RepositoryUtils.getInstance().getGiftlistRepository();
      final atg.repository.RepositoryItem giftlist = giftlistRepos.getItem(regID, "gift-list");
      final atg.repository.RepositoryItem theCI = (atg.repository.RepositoryItem) giftlist.getPropertyValue("shippingAddress");
      theNMProfile.getViewedRegistriesList().put(regID, theCI.getRepositoryId());
      
    } // end while
  }
  
  public ServiceLevelUtils getServiceLevelUtils() {
    final String componentName = "/nm/utils/ServiceLevelUtils";
    final DynamoHttpServletRequest req = ServletUtil.getCurrentRequest();
    if (null == req) {
      return (null);
    }
    return (ServiceLevelUtils) req.resolveName(componentName);
  }
  
  /**
   * Copied the method from the com.nm.ajax.checkout.shipping.ShippingUtils class
   * 
   * Returns a list of ShippingGroupBeans. Each bean contains the shipping groups id and a list of valid service levels for that group.
   * 
   * @author nmve1
   * 
   * @param order
   * @return
   * @throws Exception
   */
  public List<ShippingGroupBean> getServiceLevelsByShippingGroup(final Order order) throws Exception {
    final List<ShippingGroupBean> returnValue = new ArrayList<ShippingGroupBean>();
    
    @SuppressWarnings("unchecked")
    final List<ShippingGroup> groups = order.getShippingGroups();
    for (final Iterator<ShippingGroup> sgIter = groups.iterator(); sgIter.hasNext();) {
      final ShippingGroup shippingGroup = sgIter.next();
      
      final ServiceLevel[] serviceLevels = getValidServiceLevelsForShippingGroup(order, shippingGroup.getId());
      
      final Comparator<ServiceLevel> comparator = ServiceLevel.getCodeComparator(false);
      Arrays.sort(serviceLevels, comparator);
      
      final ShippingGroupBean shippingGroupBean = new ShippingGroupBean(shippingGroup.getId());
      shippingGroupBean.setServiceLevels(serviceLevels);
      if (serviceLevels.length > 0) {
        returnValue.add(shippingGroupBean);
      }
    }
    
    return returnValue;
  }
  
  /**
   * Copied the method from the com.nm.ajax.checkout.shipping.ShippingUtils class
   * 
   * @author nmve1
   * 
   * @param order
   * @return
   * @throws Exception
   */
  public boolean areAllSelectedServiceLevelsPromotional(final Order order) throws Exception {
    boolean returnValue = true;
    
    @SuppressWarnings("unchecked")
    final List<ShippingGroup> shippingGroups = order.getShippingGroups();
    for (final Iterator<ShippingGroup> i = shippingGroups.iterator(); i.hasNext();) {
      final ShippingGroup group = i.next();
      final String shippingGroupId = group.getId();
      final String maximumServiceLevel = getMaximumServiceLevelForShippingGroup(order, shippingGroupId);
      final String currentServiceLevel = getServiceLevelForShippingGroup(order, shippingGroupId);
      
      if ((maximumServiceLevel != null) && (currentServiceLevel != null) && (currentServiceLevel.length() > 0) && !maximumServiceLevel.equals(currentServiceLevel)) {
        returnValue = false;
        break;
      }
    }
    
    return returnValue;
  }
  
  public boolean areAllSelectedServiceLevelsShopRunner(final Order order) throws Exception {
    boolean returnValue = true;
    
    final String shopRunnerCode = ServiceLevel.SL2_SERVICE_LEVEL_TYPE;
    
    @SuppressWarnings("unchecked")
    final List<ShippingGroup> shippingGroups = order.getShippingGroups();
    for (final Iterator<ShippingGroup> i = shippingGroups.iterator(); i.hasNext();) {
      final ShippingGroup group = i.next();
      final String shippingGroupId = group.getId();
      
      boolean containsSL2 = false;
      final ServiceLevel[] serviceLevels = this.getValidServiceLevelsForShippingGroup(order, shippingGroupId);
      for (final ServiceLevel sl : serviceLevels) {
        if (sl.getCode().equals(shopRunnerCode)) {
          containsSL2 = true;
          break;
        }
      }
      
      if (containsSL2) {
        
        final String currentServiceLevel = getServiceLevelForShippingGroup(order, shippingGroupId);
        
        if ((currentServiceLevel != null) && (currentServiceLevel.length() > 0) && !shopRunnerCode.equals(currentServiceLevel)) {
          returnValue = false;
          break;
        }
      }
    }
    
    return returnValue;
  }
  
  /**
   * Copied the method from the com.nm.ajax.checkout.shipping.ShippingUtils class
   * 
   * @author nmve1
   * 
   * @param order
   * @param shippingGroupId
   * @return
   * @throws Exception
   */
  public String getMaximumServiceLevelForShippingGroup(final Order order, final String shippingGroupId) throws Exception {
    String returnValue = null;
    
    final ServiceLevel[] serviceLevels = getValidServiceLevelsForShippingGroup(order, shippingGroupId);
    
    if (serviceLevels.length > 0) {
      final Comparator<ServiceLevel> comparator = ServiceLevel.getCodeComparator(false);
      Arrays.sort(serviceLevels, comparator);
      returnValue = serviceLevels[0].getCode();
    }
    
    return returnValue;
  }
  
  public String getShippingGroupForCI(final Order order, final String itemId) {
    final OrderUtil orderUtil = OrderUtil.getInstance();
    final CommerceItem ci = orderUtil.getCommerceItem(order, itemId);
    return orderUtil.getShippingGroup(ci).getId();
  }
  
  public boolean isEmptyShippingGroup(final Order order, final String sgId) {
    final OrderUtil orderUtil = OrderUtil.getInstance();
    final HardgoodShippingGroup hardgoodShippingGroup = orderUtil.getHardgoodShippingGroup(order, sgId);
    return ((null == hardgoodShippingGroup) || (hardgoodShippingGroup.getCommerceItemRelationshipCount() < 1));
  }
  
  public void removeEmptyShippingGroups(final Order order, final OrderManager orderMgr) throws CommerceException {
    @SuppressWarnings("unchecked")
    final List<ShippingGroup> groups = order.getShippingGroups();
    final List<String> removeGroupIds = new ArrayList<String>();
    for (final Iterator<ShippingGroup> i = groups.iterator(); i.hasNext();) {
      final ShippingGroup sg = i.next();
      if (sg.getCommerceItemRelationshipCount() < 1) {
        removeGroupIds.add(sg.getId());
      }
    }
    
    final ShippingGroupManager shipMgr = orderMgr.getShippingGroupManager();
    for (final Iterator<String> id = removeGroupIds.iterator(); id.hasNext();) {
      final String sgId = id.next();
      shipMgr.removeShippingGroupFromOrder(order, sgId);
    }
  }
  
  private Address copyShippingAddressToCurrentAddress(NMAddress nmAddress) {
    Address address = new Address();
    if (null != nmAddress.getFirstName()) {
      address.setFirstName(nmAddress.getFirstName());
    }
    if (null != nmAddress.getLastName()) {
      address.setLastName(nmAddress.getLastName());
    }
    if (null != nmAddress.getAddress1()) {
      address.setAddress1(nmAddress.getAddress1());
    }
    if (null != nmAddress.getAddress2()) {
      address.setAddress2(nmAddress.getAddress2());
    }
    if (null != nmAddress.getAddress3()) {
      address.setAddress3(nmAddress.getAddress3());
    }
    if (null != nmAddress.getPostalCode()) {
      address.setPostalCode(nmAddress.getPostalCode());
    }
    if (null != nmAddress.getCity()) {
      address.setCity(nmAddress.getCity());
    }
    if (null != nmAddress.getState()) {
      address.setState(nmAddress.getState());
    }
    if (null != nmAddress.getCountry()) {
      address.setCountry(nmAddress.getCountry());
    }
    return address;
  }
  
  public void moveItemToShippingGroup(final NMProfile profile, final ShoppingCartHandler cart, final Order order, final OrderManager orderMgr, final String addrId, final String itemId)
          throws Exception {
    final OrderUtil orderUtil = OrderUtil.getInstance();
    final CommerceItem ci = orderUtil.getCommerceItem(order, itemId);
    if (null == ci) {
      throw new ShippingException("Unable to find item " + itemId + " on order " + order.getId());
    }
    
    final ShippingGroup itemGroup = orderUtil.getShippingGroup(ci);
    final List<ShippingGroup> orderGroups = getShippingGroupsForAddress(order, addrId);
    // MasterPass changes starts
    boolean massPassFlag = false;
    Address orderGroupsListShippingGroupAddress = new Address();
    Address currentShippingAddress = new Address();
    RepositoryItem orderGroupsAddress = orderUtil.getAddressFromKey(cart, profile, addrId);
    if (null != orderGroupsAddress) {
      OrderTools.copyAddress(orderGroupsAddress, orderGroupsListShippingGroupAddress);
      currentShippingAddress = orderGroupsListShippingGroupAddress;
    } else {
      final Map shippingAddressMap = CheckoutAPI.getShippingAddressMap();
      if (shippingAddressMap != null) {
        Iterator iterator = shippingAddressMap.entrySet().iterator();
        while (iterator.hasNext()) {
          Entry thisEntry = (Entry) iterator.next();
          NMAddress nmAddress = (NMAddress) thisEntry.getValue();
          String addrKey = (String) thisEntry.getKey();
          if (addrId.equalsIgnoreCase(addrKey)) {
            OrderTools.copyAddress(copyShippingAddressToCurrentAddress(nmAddress), currentShippingAddress);
            break;
          }
        }
      } else {
        if (mLogging != null && mLogging.isLoggingError()) {
          mLogging.logError("shippingAddressMap object is null for profile= " + profile != null ? profile.getId() : "" + ", order=" + order != null ? order.getId() : "" + ", addrId=" + addrId
                  + ", itemId=" + itemId);
        }
      }
    }
    // MasterPass changes end
    ShippingGroup addrShippingGroup = null;
    if (orderGroups.size() == 0) {
      final RepositoryItem address = orderUtil.getAddressFromKey(cart, profile, addrId);
      Store store = null;
      if (null == address) {
        // check to see if the key returns a store
        store = CheckoutComponents.getShipToStoreHelper().getStoreAddressByStoreNumber(addrId);
        // MasterPass changes starts
        if ((null != currentShippingAddress) && (null == store)) {
          addrShippingGroup = orderMgr.getShippingGroupManager().createShippingGroup();
          orderMgr.getShippingGroupManager().addShippingGroupToOrder(order, addrShippingGroup);
          OrderTools.copyAddress(currentShippingAddress, ((HardgoodShippingGroup) addrShippingGroup).getShippingAddress());
          massPassFlag = true;
        }
        // MasterPass changes end
        // if the store is not found then throw the excpetion.
        if ((store == null) && !massPassFlag) {
          throw new ShippingException("Unable to find address " + addrId + " for profile " + profile.getRepositoryId());
        }
      }
      if (!massPassFlag) {
        addrShippingGroup = orderMgr.getShippingGroupManager().createShippingGroup();
        orderMgr.getShippingGroupManager().addShippingGroupToOrder(order, addrShippingGroup);
        
        if (store == null) {
          OrderTools.copyAddress(address, ((HardgoodShippingGroup) addrShippingGroup).getShippingAddress());
        } else {
          // this is a ship to store address, so explicitly copy the store's address to the HardgoodShippingGroup
          final NMOrderImpl nmOrder = (NMOrderImpl) order;
          CheckoutComponents.getShipToStoreHelper().mapStoreAddressToHardgoodShippingGroup(store, ((HardgoodShippingGroup) addrShippingGroup), profile, nmOrder);
        }
      }
      addrShippingGroup.setDescription(addrId);
    } else {
      addrShippingGroup = orderGroups.get(0);
      
      OrderTools.copyAddress(currentShippingAddress, ((HardgoodShippingGroup) addrShippingGroup).getShippingAddress());
      
    }
    
    // we only need to move the item if the from and to groups are different
    if (!itemGroup.getId().equals(addrShippingGroup.getId())) {
      final CommerceItemManager itemManager = orderMgr.getCommerceItemManager();
      final long qty = ci.getQuantity();
      itemManager.removeItemQuantityFromShippingGroup(order, ci.getId(), itemGroup.getId(), qty);
      itemManager.addItemQuantityToShippingGroup(order, ci.getId(), addrShippingGroup.getId(), qty);
    }
  }
  
  public boolean sgRequiresDeliveryPhone(final HardgoodShippingGroup sg) {
    @SuppressWarnings("unchecked")
    final List<CommerceItemRelationship> items = sg.getCommerceItemRelationships();
    for (final Iterator<CommerceItemRelationship> itemIter = items.iterator(); itemIter.hasNext();) {
      final CommerceItemRelationship ciRel = itemIter.next();
      final NMCommerceItem ci = (NMCommerceItem) ciRel.getCommerceItem();
      final RepositoryItem product = (RepositoryItem) ci.getAuxiliaryData().getProductRef();
      final boolean flgDeliveryPhone = ((Boolean) product.getPropertyValue("flgDeliveryPhone")).booleanValue();
      if (flgDeliveryPhone == true) {
        return true;
      }
    }
    return false;
  }
  
  public List<HardgoodShippingGroup> needDeliveryPhone(final Order order) {
    final OrderUtil orderUtil = OrderUtil.getInstance();
    final List<HardgoodShippingGroup> sgsNeedDeliveryPhone = new ArrayList<HardgoodShippingGroup>();
    final List<HardgoodShippingGroup> sgs = orderUtil.getHardgoodShippingGroups(order);
    for (int pos = 0; pos < sgs.size(); pos++) {
      final HardgoodShippingGroup sg = sgs.get(pos);
      final NMRepositoryContactInfo address = (NMRepositoryContactInfo) sg.getShippingAddress();
      final String delPhone = address.getDeliveryPhoneNumber();
      if (sgRequiresDeliveryPhone(sg) && StringUtilities.isEmpty(delPhone)) {
        sgsNeedDeliveryPhone.add(sg);
      }
    }
    return sgsNeedDeliveryPhone;
  }
  
  public List<Message> moveCommerceItemToAddress(final NMProfile profile, final ShoppingCartHandler cart, final NMOrderImpl order, final Map<String, String> itemAddrMap,
          final Map<String, String> serviceLevelMap, final String activeSGId) throws Exception {
    final NMOrderManager orderMgr = (NMOrderManager) cart.getOrderManager();
    final OrderUtil orderUtil = OrderUtil.getInstance();
    final OrderShipUtil orderShipUtil = OrderShipUtil.getInstance();
    final PaymentUtil paymentUtil = PaymentUtil.getInstance();
    final List<Message> messages = new ArrayList<Message>();
    // final String activeSGId = addr.getActiveShippingGroupId();
    final List<String> ciSGIds = new ArrayList<String>();
    
    // validate only one country choosen
    final List<String> countries = new ArrayList<String>();
    for (final Iterator<String> i = itemAddrMap.keySet().iterator(); i.hasNext();) {
      final String itemId = i.next();
      ciSGIds.add(getShippingGroupForCI(order, itemId));
      final String addrId = itemAddrMap.get(itemId);
      if (addrId != null) { // exclude GWP must ship with
        final RepositoryItem ri = orderUtil.getAddressFromKey(cart, profile, addrId);
        String country = "";
        if (ri != null) {
          country = (String) ri.getPropertyValue("country");
        } else {
          final Map<String, String> tempMap = profile.getViewedRegistriesList();
          final String theValue = tempMap.get(addrId);
          if (StringUtilities.isNotEmpty(theValue)) {
            final Repository profileRepository = profile.getRepository();
            final RepositoryItem contactinfo = profileRepository.getItem(theValue, ProfileProperties.Profile_Desc_contactInfo);
            country = (String) contactinfo.getPropertyValue("country");
          }
        }
        if (StringUtilities.isNotEmpty(country)) {
          countries.add(country);
        }
      }
    }
    if (!countries.isEmpty()) {
      final String multiCountryIssue = orderShipUtil.checkMultipleCountries(countries);
      if (StringUtilities.isNotEmpty(multiCountryIssue)) {
        // esp.setError(true);
        final Message message = MessageDefs.getMessage(MessageDefs.MSG_MultiShipMultiCountry);
        message.setMsgText(multiCountryIssue);
        messages.add(message);
      }
    }
    
    // process addresses
    for (final Iterator<String> i = itemAddrMap.keySet().iterator(); i.hasNext();) {
      final String itemId = i.next();
      final String addrId = itemAddrMap.get(itemId);
      if (addrId != null) { // exclude GWP must ship with
        moveItemToShippingGroup(profile, cart, order, orderMgr, addrId, itemId);
      }
    }
    
    // process service levels for new addresses
    for (final Iterator<String> j = serviceLevelMap.keySet().iterator(); j.hasNext();) {
      final String itemId = j.next();
      String curSl = serviceLevelMap.get(itemId);
      int curSlNo = 0;
      if ((curSl != null) && (curSl.length() > 2)) {
        curSlNo = Integer.parseInt(curSl.substring(2, 3));
      }
      final String curSgId = getShippingGroupForCI(order, itemId);
      if (curSlNo > 1) {
        for (final Iterator<String> k = serviceLevelMap.keySet().iterator(); k.hasNext();) {
          final String iId = k.next();
          final String nextSl = serviceLevelMap.get(iId);
          final String nextSgId = getShippingGroupForCI(order, iId);
          if (curSgId.equals(nextSgId)) {
            int nextSlNo = 0;
            if ((nextSl != null) && (nextSl.length() > 2)) {
              nextSlNo = Integer.parseInt(nextSl.substring(2, 3));
            }
            if (nextSlNo < curSlNo) {
              curSlNo = nextSlNo;
              curSl = nextSl;
            }
          }
        }
      }
      if (StringUtilities.isNotEmpty(curSl)) {
        setShippingServiceLevel(order, curSl, curSgId);
      }
    }
    
    orderUtil.evaluateGwpMustShipWith(order, orderMgr);
    orderUtil.mergeSplitCommerceItems(order, orderMgr, false, false, false);
    
    final String addressVerificationFragment = null;
    
    // if (order.hasVerifiedSpcPayment()) {
    // AddressVerificationCallback callback = new AddressVerificationCallback(addr, this);
    //
    // addressVerificationFragment = addressVerificationUtils.getAddressVerificationFragment(
    // AddressVerificationUtils.CHECK_SHIPPING_ADDRESSES, callback, order, this);
    // resp.setFrgAddressVerification(addressVerificationFragment);
    // }
    
    if (addressVerificationFragment == null) {
      final ArrayList<Message> newMessages = new ArrayList<Message>();
      validateShipToCountries(cart, newMessages);
      
      // remove empty shipping groups
      // List sgList = shippingUtils.getEmptyShippingGroupIds(order);
      // if (!sgList.isEmpty())
      // {
      // resp.setRemoveShippingGroups(sgList);
      // for (int pos = 0; pos < sgList.size(); pos++)
      // {
      // ShippingListResp removeResp = (ShippingListResp) sgList.get(pos);
      // if (removeResp.getShippingGroupId().equals(activeSGId))
      // {
      // addr.setActiveShippingGroupId("");
      // break;
      // }
      // }
      // }
      removeEmptyShippingGroups(order, orderMgr);
      
      final List<HardgoodShippingGroup> delPhoneList = needDeliveryPhone(order);
      if (!delPhoneList.isEmpty()) {
        // String sgId = ((HardgoodShippingGroup) delPhoneList.get(0)).getId();
        // ShippingEditOptionReq shippingEditOptionReq = new ShippingEditOptionReq();
        // shippingEditOptionReq.setShippingGroupId(sgId);
        // List tempRemoveShippingGroups = resp.getRemoveShippingGroups();
        // resp = shippingUtils.getEditShippingOptions(shippingEditOptionReq, profile, order, orderMgr, getPromoEvaluation(),
        // this);
        // if(shippingUtils.getShippingSectionStepValue(accordionState).equals(shippingUtils.getShippingEditCode())){
        // resp.setFrgShippingEdit(shippingUtils.getShippingEditFragment(profile, order, activeSGId, resp.getMessages(),
        // this, shippingUtils.getShippingEditCode()));
        // }
        // resp.setRemoveShippingGroups(tempRemoveShippingGroups);
        // if (sgId.equals(activeSGId))
        // {
        // resp.setNextTransStep("");
        // }
        // List aList = shippingUtils.getShippingListFragment(profile, order, resp.getMessages(), this);
        // for (int pos = 0; pos < aList.size(); pos++)
        // {
        // ShippingListResp slResp = (ShippingListResp) aList.get(pos);
        // if (slResp.getShippingGroupId().equals(sgId))
        // {
        // aList.remove(pos);
        // break;
        // }
        // }
        // resp.setUpdateShippingGroups(aList);
        // resp.getFrgShippingList().clear();
      } else {
        // String activeSGState = addr.getActiveShippingGroupState();
        // if (StringUtilities.isNotEmpty(addr.getActiveShippingGroupId()) && StringUtilities.isNotEmpty(activeSGState))
        // {
        // if (activeSGState.equals(shippingUtils.getShippingOptionCode()))
        // {
        // ShippingEditOptionResp shippingEditOptionResp = new ShippingEditOptionResp();
        // shippingEditOptionResp.setShippingEditOptionFrg(shippingUtils.getShippingEditOptionFragment(order, orderMgr,
        // profile, activeSGId, resp.getMessages(), this));
        // shippingEditOptionResp.setShippingGroupId(activeSGId);
        // resp.setFrgShippingEditOption(shippingEditOptionResp);
        // }
        // else if (activeSGState.equals(shippingUtils.getShippingEditCode()) && !ciSGIds.contains(activeSGId))
        // {
        // resp.setFrgShippingEdit(shippingUtils.getShippingEditFragment(profile, order, activeSGId, resp.getMessages(),
        // this, shippingUtils.getShippingEditCode()));
        // }
        // }
        
        // if ((StringUtilities.isNotEmpty(activeSGId) && isEmptyShippingGroup(order, activeSGId)) || ciSGIds.contains(activeSGId)) {
        // paymentUtils.getPaymentCheckoutResp(resp, profile, order, orderMgr, SnapshotUtil.CHANGE_SHIP_DESTINATION, this);
        // }
        // cartUtils.addSummaryFragments(this, resp.getMessages(), SnapshotUtil.CHANGE_SHIP_DESTINATION, resp);
        
      }
    }
    
    paymentUtil.calcAmountRemainingOnOrder(orderMgr, order);
    orderMgr.updateOrder(order);
    return messages;
  }
  
}
