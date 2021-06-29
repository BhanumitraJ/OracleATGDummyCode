package com.nm.commerce.checkout;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.transaction.TransactionManager;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import atg.commerce.CommerceException;
import atg.commerce.order.CommerceItemManager;
import atg.commerce.order.HardgoodShippingGroup;
import atg.commerce.order.Order;
import atg.commerce.order.OrderManager;
import atg.commerce.order.ShippingGroup;
import atg.commerce.order.ShippingGroupCommerceItemRelationship;
import atg.core.util.Address;
import atg.core.util.StringUtils;
import atg.dtm.TransactionDemarcation;
import atg.dtm.TransactionDemarcationException;
import atg.nucleus.Nucleus;
import atg.nucleus.logging.ApplicationLogging;
import atg.nucleus.logging.ClassLoggingFactory;
import atg.repository.MutableRepositoryItem;
import atg.repository.Repository;
import atg.repository.RepositoryException;
import atg.repository.RepositoryItem;
import atg.servlet.DynamoHttpServletRequest;
import atg.servlet.DynamoHttpServletResponse;
import atg.userprofiling.Profile;

import com.neimanmarcus.service.esb.web_services.GetShoprunnerPaymentInfoServiceStub.GetShoprunnerPaymentInfoResponse;
import com.nm.ajax.checkout.addressbook.AddressUtils;
import com.nm.ajax.checkout.cart.CartUtils;
import com.nm.ajax.checkoutb.beans.FinCenRequest;
import com.nm.ajax.checkoutb.beans.FinCenResponse;
import com.nm.ajax.checkoutb.beans.PromoEmailClearRequest;
import com.nm.collections.Province;
import com.nm.collections.ServiceLevel;
import com.nm.commerce.GiftCard;
import com.nm.commerce.GiftCardHolder;
import com.nm.commerce.NMAddress;
import com.nm.commerce.NMCommerceItem;
import com.nm.commerce.NMCreditCard;
import com.nm.commerce.NMProfile;
import com.nm.commerce.NMRepositoryContactInfo;
import com.nm.commerce.beans.RecentlyChangedCommerceItem;
import com.nm.commerce.catalog.NMProduct;
import com.nm.commerce.checkout.VMeInfo.VMeUtil;
import com.nm.commerce.checkout.beans.CommerceItemUpdate;
import com.nm.commerce.checkout.beans.ContactInfo;
import com.nm.commerce.checkout.beans.GiftOptionsUpdate;
import com.nm.commerce.checkout.beans.GiftWrapInfo;
import com.nm.commerce.checkout.beans.GwpBean;
import com.nm.commerce.checkout.beans.LoginBean;
import com.nm.commerce.checkout.beans.OrderInfo;
import com.nm.commerce.checkout.beans.PaymentInfo;
import com.nm.commerce.checkout.beans.PromoBean;
import com.nm.commerce.checkout.beans.ResultBean;
import com.nm.commerce.checkout.beans.SaveForLaterItemCollection;
import com.nm.commerce.order.NMOrderManager;
import com.nm.commerce.pagedef.definition.CheckoutPageDefinition;
import com.nm.commerce.pagedef.model.CheckoutPageModel;
import com.nm.commerce.pagedef.model.bean.ShippingGroupAux;
import com.nm.commerce.promotion.NMOrderImpl;
import com.nm.common.INMGenericConstants;
import com.nm.common.NMBorderFreeGetQuoteResponse;
import com.nm.common.NMBorderFreePayPalResponse;
import com.nm.components.CommonComponentHelper;
import com.nm.discoverableservices.NMDiscoverableServiceProxyFactory;
import com.nm.discoverableservices.ShopRunnerESBUtilDiscoverable;
import com.nm.estimateddeliverydate.config.ShippingInfoFactory;
import com.nm.estimateddeliverydate.util.EstimatedDeliveryDateShippingGroupUtil;
import com.nm.estimateddeliverydate.vo.EstimatedDeliveryDateServiceLevelVO;
import com.nm.formhandler.NMProfileFormHandler;
import com.nm.formhandler.ShoppingCartHandler;
import com.nm.integration.CheckoutTypeException;
import com.nm.integration.VMeInfoVO;
import com.nm.integration.VmeException;
import com.nm.integration.util.paypal.PayPalException;
import com.nm.profile.AddressVerificationContainer;
import com.nm.profile.AddressVerificationHelper;
import com.nm.profile.ProfileProperties;
import com.nm.sitemessages.Message;
import com.nm.utils.LocalizationUtils;
import com.nm.utils.NmoUtils;
import com.nm.utils.ProdSkuUtil;
import com.nm.utils.ShopRunnerESBUtil;
import com.nm.utils.StringMap;
import com.nm.utils.StringUtilities;
import com.nm.utils.datasource.NMTransactionDemarcation;

/**
 * This class serves as a public facade to the checkout API. It dispatches method calls to the corresponding utility class.
 */
public class CheckoutAPI {
  
  // private constructor enforces singleton behavior
  private CheckoutAPI() {}
  
  private static ApplicationLogging applicationLogging;

  public static ApplicationLogging getApplicationLogging() {
    if (applicationLogging == null) {
      applicationLogging = ClassLoggingFactory.getFactory().getLoggerForClass(CheckoutAPI.class);
    }
    return applicationLogging;
  }
  public static String addressType;
  /** Property to hold shipping Address Map */
  private static Map shippingAddressMap;
  
  /** The Constant shopRunnerESBUtilDiscoverable. */
  private final static ShopRunnerESBUtilDiscoverable  shopRunnerESBUtilDiscoverable = (ShopRunnerESBUtilDiscoverable) NMDiscoverableServiceProxyFactory.createProxy(ShopRunnerESBUtil.getInstance());
  
  public static List<Message> repriceOrder(final ShoppingCartHandler cart, final NMProfile profile, final int context) throws Exception {
    boolean rollback = false;
    final TransactionDemarcation td = startTransaction();
    try {
      final List<Message> messages = PricingUtil.getInstance().repriceOrder(cart, profile, context);
      final NMOrderManager orderManager = (NMOrderManager) cart.getOrderManager();
      final NMOrderImpl order = cart.getNMOrder();
      orderManager.updateOrder(order);
      return messages;
    } catch (final Exception e) {
      rollback = true;
      throw e;
    } finally {
      td.end(rollback);
    }
  }
  
  public static ResultBean handleLogin(final ShoppingCartHandler cart, final LoginBean loginBean, final NMProfileFormHandler profileFormHandler) throws Exception {
    boolean rollback = false;
    final TransactionDemarcation td = startTransaction();
    try {
      return LoginUtil.getInstance().handleLogin(cart, loginBean, profileFormHandler);
    } catch (final Exception e) {
      rollback = true;
      throw e;
    } finally {
      td.end(rollback);
    }
  }
  
  public static void mergeSplitCommerceItems(final NMOrderImpl order, final NMOrderManager orderManager, final boolean ignoreGWPShipGroup, final boolean onlyMergeGWPItems,
          final boolean restrictToMustShipGWPs) throws Exception {
    boolean rollback = false;
    final TransactionDemarcation td = startTransaction();
    try {
      OrderUtil.getInstance().mergeSplitCommerceItems(order, orderManager, ignoreGWPShipGroup, onlyMergeGWPItems, restrictToMustShipGWPs);
    } catch (final Exception e) {
      rollback = true;
      throw e;
    } finally {
      td.end(rollback);
    }
  }
  
  public static Collection<Message> checkCmosStockLevel(final ShoppingCartHandler cart, final boolean allocate, final NMProfile profile) throws Exception {
    boolean rollback = false;
    final TransactionDemarcation td = startTransaction();
    try {
      return InventoryUtil.getInstance().checkCmosStockLevel(cart, allocate, profile);
    } catch (final Exception e) {
      System.out.println("Exception");
      rollback = true;
      throw e;
    } finally {
      td.end(rollback);
    }
  }
  
  public static String validateEmailandFormat(final String email, final List<Message> messages) {
    return ProfileUtil.getInstance().validateEmailandFormat(email, messages);
  }
  
  public static RepositoryItem getProfileUsingEmail(final String email) {
    return ProfileUtil.getInstance().getProfileUsingEmail(email);
  }
  
  public static ResultBean handleRegistration(final NMProfileFormHandler formHandler) throws Exception {
    boolean rollback = false;
    final TransactionDemarcation td = startTransaction();
    try {
      return ProfileUtil.getInstance().handleRegistration(formHandler);
    } catch (final Exception e) {
      System.out.println("Exception");
      rollback = true;
      throw e;
    } finally {
      td.end(rollback);
    }
  }
  
  public static boolean isEmptyCart(final NMOrderImpl order) {
    return CartUtil.getInstance().isEmptyCart(order);
  }
  
  public static boolean validateBorderFreeSupportedCard(final String defaultCreditCardType, final NMBorderFreeGetQuoteResponse nmBorderFreeGetQuoteResponse) {
    return CartUtil.getInstance().validateBorderFreeSupportedCard(defaultCreditCardType, nmBorderFreeGetQuoteResponse);
  }
  
  public static boolean validateBorderFreeSupportedCountries(final NMProfile nmProfile) {
    return CartUtil.getInstance().validateBorderFreeSupportedCountries(nmProfile);
  }
  
  public static boolean validateDomesticSupportedCard(final String defaultCreditCardType) {
    return CartUtil.getInstance().validateDomesticSupportedCard(defaultCreditCardType);
  }
  
  public static List<ShippingGroupCommerceItemRelationship> getShippingGroupCommerceItemRelationships(final Order pOrder, final OrderManager orderMgr) throws CommerceException {
    return OrderUtil.getInstance().getShippingGroupCommerceItemRelationships(pOrder, orderMgr);
  }
  
  public static Collection<NMCommerceItem> getDeliveryPhoneItems(final NMOrderImpl order, final OrderManager orderMgr) throws CommerceException {
    return OrderUtil.getInstance().getDeliveryPhoneItems(order, orderMgr);
  }
  
  public static Collection<NMCommerceItem> getReplenishmentItems(final NMOrderImpl order, final OrderManager orderMgr) throws CommerceException {
    return OrderUtil.getInstance().getReplenishmentItems(order, orderMgr);
  }
  
  public static boolean getIsShipFromStore(final NMOrderImpl order) {
    final ShippingGroupUtil shippingGroupUtil = ShippingGroupUtil.getInstance();
    final Collection<ShippingGroup> shippingGroups = shippingGroupUtil.getShippingGroups(order);
    return ShippingGroupUtil.getInstance().isShippingFromStore(shippingGroups);
  }
  
  public static boolean getIsShipToMultiple(final NMOrderImpl order) {
    return ShippingUtil.getInstance().getIsShipToMultiple(order);
  }
  
  public static boolean orderContainsWishlistItem(final NMOrderImpl order) {
    return ShippingUtil.getInstance().orderContainsWishlistItem(order);
  }
  
  public static boolean orderContainsShipToStoreEligibleCommerceItem(final CheckoutPageModel pageModel) {
    return ShippingUtil.getInstance().orderContainsShipToStoreEligibleCommerceItem(pageModel);
  }
  
  public static boolean areAllSelectedServiceLevelsPromotional(final NMOrderImpl order) throws Exception {
    return ShippingUtil.getInstance().areAllSelectedServiceLevelsPromotional(order);
  }
  
  public static boolean areAllSelectedServiceLevelsShopRunner(final NMOrderImpl order) throws Exception {
    return ShippingUtil.getInstance().areAllSelectedServiceLevelsShopRunner(order);
  }
  
  public static boolean getHasMultipleServiceLevels(final NMOrderImpl order) {
    return ServiceLevelUtil.getInstance().getHasMultipleServiceLevels(order);
  }
  
  public static Set<ServiceLevel> mergeServiceLevels(final List<Set<ServiceLevel>> shippingGroupServiceLevels) {
    return ServiceLevelUtil.getInstance().mergeServiceLevels(shippingGroupServiceLevels);
  }
  
  public static Set<ServiceLevel> getShippingGroupValidServiceLevels(final ShippingGroup shippingGroup, final NMOrderImpl order) {
    return ServiceLevelUtil.getInstance().getShippingGroupValidServiceLevelsWithAbTestsAndPromotions(shippingGroup, order);
  }
  
  public static ServiceLevel getShippingGroupServiceLevel(final ShippingGroup shippingGroup, final Set<ServiceLevel> serviceLevels) {
    return ServiceLevelUtil.getInstance().getShippingGroupServiceLevel(shippingGroup, serviceLevels);
  }
  
  public static List<NMCommerceItem> getShippingGroupItems(final ShippingGroup shippingGroup) {
    return ShippingGroupUtil.getInstance().getItems(shippingGroup);
  }
  
  public static int getShippingGroupQty(final ShippingGroup shippingGroup) {
    return ShippingGroupUtil.getInstance().getQty(shippingGroup);
  }
  
  public static List<ShippingGroup> getNonWishlistShippingGroups(final List<ShippingGroup> shippingGroups) {
    return ShippingUtil.getInstance().getNonWishlistShippingGroups(shippingGroups);
  }
  
  public static int getNonWishlistShippingGroupCount(final List<ShippingGroup> shippingGroups) {
    return ShippingUtil.getInstance().getNonWishlistShippingGroupCount(shippingGroups);
  }
  
  public static NMRepositoryContactInfo getShippingGroupAddress(final ShippingGroup shippingGroup) {
    return ShippingGroupUtil.getInstance().getAddress(shippingGroup);
  }
  
  public static List<ShippingGroup> getShippingGroupsForAddress(final NMOrderImpl order, final String addrId) {
    return ShippingUtil.getInstance().getShippingGroupsForAddress(order, addrId);
  }
  
  public static Collection<Message> changeShippingGroupAddress(final ShoppingCartHandler cart, final NMProfile profile, final String shippingGroupId, final String addressName) throws Exception {
    boolean rollback = false;
    final TransactionDemarcation td = startTransaction();
    try {
      return ShippingGroupUtil.getInstance().changeAddress(cart, profile, shippingGroupId, addressName);
    } catch (final Exception e) {
      rollback = true;
      throw e;
    } finally {
      td.end(rollback);
    }
  }
  
  public static Collection<Message> updateShippingGroupServiceLevel(final NMOrderManager orderMgr, final NMOrderImpl order, final NMProfile profile, final String shippingGroupId,
          final String serviceLevelCode, final String currentServiceLevelCode) throws Exception {
    boolean rollback = false;
    final TransactionDemarcation td = startTransaction();
    try {
      return ShippingGroupUtil.getInstance().updateServiceLevel(orderMgr, order, profile, shippingGroupId, serviceLevelCode, currentServiceLevelCode);
    } catch (final Exception e) {
      rollback = true;
      throw e;
    } finally {
      td.end(rollback);
    }
  }
  
  public static void updateShippingGroupAddressPayPal(final ShoppingCartHandler cart, final NMProfile profile, final Order order, final String sgId, final ContactInfo contactInfo) throws Exception {
    boolean rollback = false;
    final TransactionDemarcation td = startTransaction();
    try {
      ShippingUtil.getInstance().updateShippingGroupAddressPayPal(cart, profile, order, sgId, contactInfo);
    } catch (final Exception e) {
      rollback = true;
      throw e;
    } finally {
      td.end(rollback);
    }
  }
  
  public static Collection<Message> updateAllShippingGroupServiceLevels(final NMOrderManager orderMgr, final NMOrderImpl order, final NMProfile profile, final String serviceLevelCode,
          final String currentServiceLevelCode) throws Exception {
    boolean rollback = false;
    final TransactionDemarcation td = startTransaction();
    try {
      return ShippingGroupUtil.getInstance().updateAllServiceLevels(orderMgr, order, profile, serviceLevelCode, currentServiceLevelCode);
    } catch (final Exception e) {
      rollback = true;
      throw e;
    } finally {
      td.end(rollback);
    }
  }
  
  public static void updateAllItemServiceLevels(final NMOrderImpl order, final String serviceLevelCode) throws Exception {
    boolean rollback = false;
    final TransactionDemarcation td = startTransaction();
    try {
      ServiceLevelUtil.getInstance().setShippingGroupItemsServiceLevel(order, serviceLevelCode);
    } catch (final Exception e) {
      rollback = true;
      throw e;
    } finally {
      td.end(rollback);
    }
  }
  
  public static boolean doesOrderRequireAddressVerification(final NMProfile profile, final NMOrderImpl order) {
    return AddressVerificationUtil.getInstance().doesOrderRequireAddressVerification(profile, order);
  }
  
  public static Map<String, NMAddress> buildShippingAddresses(final Order order, final NMProfile profile) throws Exception {
    return AddressUtil.getInstance().buildShippingAddresses(order, profile);
  }
  
  public static ContactInfo getContactInfo(final NMProfile profile, final String profileProperty) {
    return ShippingUtil.getInstance().getContactInfo(profile, profileProperty);
  }
  
  public static boolean profileHasBillingAddress(final NMProfile profile) {
    return AddressUtils.getInstance().doesAddressHaveRequiredFields(profile.getBillingNMAddress());
  }
  
  public static boolean orderContainsShippingAddress(final Order order) {
    return ShippingUtil.getInstance().orderContainsShippingAddress(order);
  }
  
  public static boolean isInternationalOrderShipToValid(final Order order, final NMProfile profile) {
    return ShippingUtil.getInstance().isInternationalOrderShipToValid(order, profile);
  }
  
  public static boolean isDomesticOrderShipToValid(final Order order) {
    final List<ShippingGroup> groups = order.getShippingGroups();
    Address address = null;
    String shipToCounty = null;
    for (int pos = 0; pos < groups.size(); pos++) {
      final HardgoodShippingGroup sg = (HardgoodShippingGroup) groups.get(pos);
      address = sg.getShippingAddress();
      if (address != null) {
        shipToCounty = address.getCountry();
      }
      break;
    }
    return CheckoutComponents.getLocalizationUtils().isSupportedByOnline(shipToCounty);
  }
  
  public static void updateShippingGroupAddress(final ShoppingCartHandler cart, final NMProfile profile, final Order order, final String sgId, final ContactInfo contactInfo,
          final boolean isInternational) throws Exception {
    boolean rollback = false;
    final TransactionDemarcation td = startTransaction();
    try {
      ShippingUtil.getInstance().updateShippingGroupAddress(cart, profile, order, sgId, contactInfo, isInternational);
    } catch (final Exception e) {
      rollback = true;
      throw e;
    } finally {
      td.end(rollback);
    }
  }
  
  public static void updateProfileBillingAddress(final NMProfile profile, final ContactInfo addr) throws Exception {
    boolean rollback = false;
    final TransactionDemarcation td = startTransaction();
    try {
      ShippingUtil.getInstance().updateProfileBillingAddress(profile, addr);
    } catch (final Exception e) {
      rollback = true;
      throw e;
    } finally {
      td.end(rollback);
    }
  }
  
  /**
   * Create new default billing address. Make default card to one of the secondary cards. Removes the paymentGroup (default card) from Order.
   * 
   * @param profile
   *          Profile
   * @param addr
   *          New billing address
   * @param order
   *          Order
   * @param orderMgr
   *          OrderManager
   * @throws Possible
   *           exception.
   * */
  public static void createNewProfileBillingAddress(final NMProfile profile, final ContactInfo addr, final NMOrderImpl order, final NMOrderManager orderMgr) throws Exception {
    boolean rollback = false;
    final TransactionDemarcation td = startTransaction();
    try {
      // Make default payment info to secondary list. Add a blank payment info to profile as default.
      final Map<String, MutableRepositoryItem> cards = profile.getCreditCards();
      final NMCreditCard defaultCard = profile.getDefaultCreditCard();
      if ((cards != null) && !cards.isEmpty() && (defaultCard != null)) {
        if (cards.containsKey(defaultCard.getRepositoryId())) {
          cards.remove(defaultCard.getRepositoryId());
        }
        cards.put(defaultCard.getRepositoryId(), defaultCard.getRepositoryItem());
        profile.setDefaultCreditCard(null);
        cards.remove(ProfileProperties.Profile_defaultCreditCard);
        profile.setPropertyValue(ProfileProperties.Profile_billingAddress, null);
        final List<NMCreditCard> orderCards = order.getCreditCards();
        if ((orderCards != null) && !orderCards.isEmpty() && orderCards.contains(defaultCard)) {
          final String paymentGroupId = orderCards.get(orderCards.indexOf(defaultCard)).getPaymentId();
          orderCards.remove(defaultCard);
          removePaymentGroup(order, orderMgr, paymentGroupId);
        }
      }
      
      ShippingUtil.getInstance().updateProfileBillingAddress(profile, addr);
    } catch (final Exception e) {
      rollback = true;
      throw e;
    } finally {
      td.end(rollback);
    }
  }
  
  public static void updateProfileEmailAddress(final Profile profile, final String emailAddr) throws Exception {
    boolean rollback = false;
    final TransactionDemarcation td = startTransaction();
    try {
      PaymentUtil.getInstance().updateProfileEmailAddress(profile, emailAddr);
    } catch (final Exception e) {
      rollback = true;
      throw e;
    } finally {
      td.end(rollback);
    }
  }
  
  public static void updateProfileShippingAddress(final NMProfile profile, final ContactInfo addr, final boolean isInternational) throws Exception {
    boolean rollback = false;
    final TransactionDemarcation td = startTransaction();
    try {
      ShippingUtil.getInstance().updateProfileShippingAddress(profile, addr, isInternational);
    } catch (final Exception e) {
      rollback = true;
      throw e;
    } finally {
      td.end(rollback);
    }
  }
  
  public static ContactInfo profileAddressToContactInfo(final RepositoryItem contactInfo) {
    return AddressUtil.getInstance().profileAddressToContactInfo(contactInfo);
  }
  
  public static ContactInfo orderAddressToContactInfo(final RepositoryItem orderAddress) {
    return AddressUtil.getInstance().orderAddressToContactInfo(orderAddress);
  }
  
  public static List<Message> validateAddress(final ContactInfo addr, final NMProfileFormHandler profileHandler, final String addressFieldSetKey, final String addressType) {
    return AddressUtil.getInstance().validateAddress(addr, profileHandler, addressFieldSetKey, addressType);
  }
  
  public static List<Message> validateAddress(final ContactInfo addr, final NMProfileFormHandler profileHandler, final String addressFieldSetKey, final String addressType,
          final boolean isInternational) {
    return AddressUtil.getInstance().validateAddress(addr, profileHandler, addressFieldSetKey, addressType, isInternational);
  }
  
  // public static MutableRepositoryItem copyFieldsToOrderAddress( final ContactInfo fields, final MutableRepositoryItem orderAddress ) {
  // return AddressUtil.getInstance().copyFieldsToOrderAddress( fields, orderAddress );
  // }
  
  public static String addNewAddress(final NMProfile profile, final ContactInfo addr) throws Exception {
    boolean rollback = false;
    final TransactionDemarcation td = startTransaction();
    try {
      return AddressUtil.getInstance().addNewAddress(profile, addr);
    } catch (final Exception e) {
      rollback = true;
      throw e;
    } finally {
      td.end(rollback);
    }
  }
  
  public static String updateAddress(final ShoppingCartHandler cart, final NMProfile profile, final String addrNickName, final ContactInfo addr) throws Exception {
    boolean rollback = false;
    final TransactionDemarcation td = startTransaction();
    try {
      return AddressUtil.getInstance().updateAddress(cart, profile, addrNickName, addr, true);
    } catch (final Exception e) {
      rollback = true;
      throw e;
    } finally {
      td.end(rollback);
    }
  }
  
  public static Collection<Message> moveCommerceItemToAddress(final NMProfile profile, final ShoppingCartHandler cart, final NMOrderImpl order, final Map<String, String> itemAddrMap,
          final Map<String, String> serviceLevelMap, final String activeSGId) throws Exception {
    
    boolean rollback = false;
    final TransactionDemarcation td = startTransaction();
    try {
      return ShippingUtil.getInstance().moveCommerceItemToAddress(profile, cart, order, itemAddrMap, serviceLevelMap, activeSGId);
    } catch (final Exception e) {
      rollback = true;
      throw e;
    } finally {
      td.end(rollback);
    }
  }
  
  public static void resetCommerceItemsToDefaultAddresses(final Profile profile, final NMOrderImpl order, final OrderManager orderMgr) throws CommerceException, Exception {
    boolean rollback = false;
    final TransactionDemarcation td = startTransaction();
    try {
      OrderUtil.getInstance().resetCommerceItemsToDefaultAddresses(profile, order, orderMgr);
    } catch (final Exception e) {
      rollback = true;
      throw e;
    } finally {
      td.end(rollback);
    }
    
  }
  
  public static boolean addressesAreTheSame(final ContactInfo addr1, final ContactInfo addr2) {
    return AddressUtil.getInstance().addressesAreTheSame(addr1, addr2);
  }
  
  public static AddressVerificationContainer getAddressVerificationContainer(final NMProfile profile, final AddressVerificationHelper addressVerificationHelper, final int addressType,
          final NMOrderImpl order) throws Exception {
    return AddressVerificationUtil.getInstance().getAddressVerificationContainer(profile, addressVerificationHelper, addressType, order);
  }
  
  public static AddressVerificationContainer getAddressVerificationContainer(final AddressVerificationHelper addressVerificationHelper, final Collection<ContactInfo> contactInfos) throws Exception {
    return AddressVerificationUtil.getInstance().getAddressVerificationContainer(addressVerificationHelper, contactInfos);
  }
  
  public static String getAddressVerificationURL(final AddressVerificationHelper addressVerificationHelper, final Collection<ContactInfo> contactInfos) throws Exception {
    return AddressVerificationUtil.getInstance().getAddressVerificationURL(addressVerificationHelper, contactInfos);
  }
  
  public static void updateAddresses(final ShoppingCartHandler shoppingCartHandler, final NMProfile profile, final Order order, final Collection<ContactInfo> contactInfos) throws Exception {
    boolean rollback = false;
    final TransactionDemarcation td = startTransaction();
    try {
      AddressVerificationUtil.getInstance().updateAddresses(shoppingCartHandler, profile, order, contactInfos);
    } catch (final Exception e) {
      rollback = true;
      throw e;
    } finally {
      td.end(rollback);
    }
    
  }
  
  public static List<ContactInfo> getVerifiedAddresses(final AddressVerificationContainer addressVerificationContainer, final StringMap userSelections) {
    return AddressVerificationUtil.getInstance().getVerifiedAddresses(addressVerificationContainer, userSelections);
  }
  
  public static String getCityAndState(final AddressVerificationHelper addressVerificationHelper, final ContactInfo contactInfo) throws Exception {
    return AddressVerificationUtil.getInstance().getCityAndState(addressVerificationHelper, contactInfo);
  }
  
  public static ResultBean updateCommerceItem(final CommerceItemUpdate update, final ShoppingCartHandler cart, final NMProfile profile) throws Exception {
    boolean rollback = false;
    final TransactionDemarcation td = startTransaction();
    try {
      return CartUtil.getInstance().updateCommerceItem(update, cart, profile);
    } catch (final Exception e) {
      rollback = true;
      throw e;
    } finally {
      td.end(rollback);
    }
  }
  
  public static ResultBean updateGiftOptions(final GiftOptionsUpdate update, final ShoppingCartHandler cart) throws Exception {
    boolean rollback = false;
    final TransactionDemarcation td = startTransaction();
    try {
      return CartUtil.getInstance().updateGiftOptions(update, cart);
    } catch (final Exception e) {
      rollback = true;
      throw e;
    } finally {
      td.end(rollback);
    }
  }
  
  public static GiftWrapInfo getGiftWrapInfo() {
    return CartUtil.getInstance().getGiftWrapInfo();
  }
  
  public static ResultBean removeItemFromOrder(final ShoppingCartHandler cart, final String commerceId) throws Exception {
    boolean rollback = false;
    ResultBean result = null;
    final TransactionDemarcation td = startTransaction();
    try {
      final OrderManager orderMgr = cart.getOrderManager();
      result = OrderUtil.getInstance().removeItemFromOrder(cart, commerceId);
      orderMgr.updateOrder(cart.getOrder());
    } catch (final Exception e) {
      rollback = true;
      throw e;
    } finally {
      td.end(rollback);
    }
    return result;
  }
  
  public static String splitUpCommerceItem(final ShoppingCartHandler cart, final Profile profile, final Order order, final OrderManager orderMgr, final Locale userLocale, final String itemId,
          final boolean removeOriginal, final boolean splitOneItemOnly) throws Exception {
    boolean rollback = false;
    String result = "";
    final TransactionDemarcation td = startTransaction();
    try {
      result = OrderUtil.getInstance().splitUpCommerceItem(cart, profile, order, orderMgr, userLocale, itemId, removeOriginal, splitOneItemOnly);
    } catch (final Exception e) {
      rollback = true;
      throw e;
    } finally {
      td.end(rollback);
    }
    return result;
  }
  
  public static Collection<Message> applyGiftCard(final NMOrderImpl order, final NMOrderManager orderMgr, final GiftCardHolder giftCardHolder, final String cardNumber, final String cardCin)
          throws Exception {
    boolean rollback = false;
    final TransactionDemarcation td = startTransaction();
    try {
      return PaymentUtil.getInstance().applyGiftCard(order, orderMgr, giftCardHolder, cardNumber, cardCin);
    } catch (final Exception e) {
      rollback = true;
      throw e;
    } finally {
      td.end(rollback);
    }
  }
  
  public static void removePaymentGroup(final NMOrderImpl order, final NMOrderManager orderMgr, final String paymentGroupId) throws Exception {
    boolean rollback = false;
    final TransactionDemarcation td = startTransaction();
    try {
      PaymentUtil.getInstance().removePaymentGroup(order, orderMgr, paymentGroupId);
    } catch (final Exception e) {
      rollback = true;
      throw e;
    } finally {
      td.end(rollback);
    }
  }
  
  public static void removeAllPaymentGroups(final NMOrderImpl order, final NMOrderManager orderMgr) throws Exception {
    boolean rollback = false;
    final TransactionDemarcation td = startTransaction();
    try {
      PaymentUtil.getInstance().removeAllPaymentGroups(order, orderMgr);
    } catch (final Exception e) {
      rollback = true;
      throw e;
    } finally {
      td.end(rollback);
    }
  }
  
  public static NMCreditCard getGiftCard(final NMOrderImpl order, final String giftCardNumber) {
    return PaymentUtil.getInstance().getGiftCardOnOrder(order, giftCardNumber);
  }
  
  public static double getGiftCardBalance(final NMOrderImpl order) {
    final List<NMCreditCard> giftCardsOnOrder = order.getGiftCards();
    double gcBalance = 0.0;
    for (final NMCreditCard tempGiftCard : giftCardsOnOrder) {
      gcBalance += tempGiftCard.getAmount();
    }
    return gcBalance;
  }
  
  public static boolean isGiftCardAlreadyApplied(final NMOrderImpl order, final String number) {
    final List<NMCreditCard> giftCardsOnOrder = order.getGiftCards();
    for (final NMCreditCard tempGiftCard : giftCardsOnOrder) {
      if (number.equals(tempGiftCard.getCreditCardNumber())) {
        return true;
      }
    }
    return false;
  }
  
  public static GwpBean getNextGwp(final ShoppingCartHandler cart) throws Exception {
    boolean rollback = false;
    final TransactionDemarcation td = startTransaction();
    try {
      return GwpUtil.getInstance().getNextGwp(cart);
    } catch (final Exception e) {
      rollback = true;
      throw e;
    } finally {
      td.end(rollback);
    }
  }
  
  public static ResultBean addGwp(final ShoppingCartHandler cart, final GwpBean gwp) throws Exception {
    boolean rollback = false;
    final TransactionDemarcation td = startTransaction();
    try {
      return GwpUtil.getInstance().addGwp(cart, gwp);
    } catch (final Exception e) {
      rollback = true;
      throw e;
    } finally {
      td.end(rollback);
    }
  }
  
  public static ResultBean declineGwp(final ShoppingCartHandler cart, final GwpBean gwp) throws Exception {
    boolean rollback = false;
    final TransactionDemarcation td = startTransaction();
    try {
      return GwpUtil.getInstance().declineGwp(cart, gwp);
    } catch (final Exception e) {
      rollback = true;
      throw e;
    } finally {
      td.end(rollback);
    }
  }
  
  public static ResultBean restoreGwpSelects(final ShoppingCartHandler cart) throws Exception {
    boolean rollback = false;
    final TransactionDemarcation td = startTransaction();
    try {
      return GwpUtil.getInstance().restoreGwpSelects(cart);
    } catch (final Exception e) {
      rollback = true;
      throw e;
    } finally {
      td.end(rollback);
    }
  }
  
  public static Collection<Message> copyOrderCreditCardToProfile(final NMOrderImpl order, final NMProfile profile, final NMProfileFormHandler profileHandler, final NMCreditCard creditCard)
          throws Exception {
    final PaymentInfo paymentInfo = PaymentUtil.getInstance().creditCardToPaymentInfo(creditCard, profile.getBillingAddress());
    paymentInfo.setSkipBillingAddressValidation(true);
    paymentInfo.setSkipSecCodeValidation(true);
    
    return addCreditCardToProfile(order, profile, profileHandler, paymentInfo);
  }
  
  public static Collection<Message> addCreditCardToProfile(final NMOrderImpl order, final NMProfile profile, final NMProfileFormHandler profileHandler, final PaymentInfo paymentInfo) throws Exception {
    return addCreditCardToProfile(order, profile, profileHandler, paymentInfo, null, null);
  }
  
  public static Collection<Message> addCreditCardToProfile(final NMOrderImpl order, final NMProfile profile, final NMProfileFormHandler profileHandler, final PaymentInfo paymentInfo,
          final List<PaymentInfo> newCreditCards, final List<PaymentInfo> removedCreditCards) throws Exception {
    boolean rollback = false;
    final TransactionDemarcation td = startTransaction();
    try {
      return PaymentUtil.getInstance().addCreditCardToProfile(order, profile, profileHandler, paymentInfo, newCreditCards, removedCreditCards);
    } catch (final Exception e) {
      rollback = true;
      throw e;
    } finally {
      td.end(rollback);
    }
  }
  
  public static Collection<Message> updateDefaultCreditCard(final NMOrderImpl order, final NMProfile profile, final NMProfileFormHandler profileHandler, final PaymentInfo paymentInfo)
          throws Exception {
    boolean rollback = false;
    final TransactionDemarcation td = startTransaction();
    try {
      return PaymentUtil.getInstance().updateDefaultCreditCard(order, profile, profileHandler, paymentInfo);
    } catch (final Exception e) {
      rollback = true;
      throw e;
    } finally {
      td.end(rollback);
    }
  }
  
  public static Collection<Message>
          addOrUpdateDefaultInternationalCreditCard(final NMOrderImpl order, final NMProfile profile, final NMProfileFormHandler profileHandler, final PaymentInfo paymentInfo) throws Exception {
    boolean rollback = false;
    final TransactionDemarcation td = startTransaction();
    try {
      return PaymentUtil.getInstance().addOrUpdateDefaultInternationalCreditCard(order, profile, profileHandler, paymentInfo);
      
    } catch (final Exception e) {
      rollback = true;
      throw e;
    } finally {
      td.end(rollback);
    }
  }
  
  public static Collection<Message> removeCreditCardFromProfile(final NMOrderImpl order, final NMOrderManager orderMgr, final NMProfile profile, final NMCreditCard creditCard) throws Exception {
    boolean rollback = false;
    final TransactionDemarcation td = startTransaction();
    try {
      return PaymentUtil.getInstance().removeCreditCardFromProfile(order, orderMgr, profile, creditCard);
    } catch (final Exception e) {
      rollback = true;
      throw e;
    } finally {
      td.end(rollback);
    }
  }
  
  public static Collection<Message> changeCreditCardOnOrder(final NMOrderImpl order, final NMOrderManager orderMgr, final NMProfile profile, final NMCreditCard creditCard) throws Exception {
    boolean rollback = false;
    final TransactionDemarcation td = startTransaction();
    try {
      return PaymentUtil.getInstance().changeCreditCardOnOrder(order, orderMgr, profile, creditCard);
    } catch (final Exception e) {
      rollback = true;
      throw e;
    } finally {
      td.end(rollback);
    }
  }
  
  public static Collection<Message> changeCreditCardOnOrder(final NMOrderImpl order, final NMOrderManager orderMgr, final RepositoryItem billingAddress, final PaymentInfo paymentInfo)
          throws Exception {
    boolean rollback = false;
    final TransactionDemarcation td = startTransaction();
    try {
      return PaymentUtil.getInstance().changeCreditCardOnOrder(order, orderMgr, billingAddress, paymentInfo);
    } catch (final Exception e) {
      rollback = true;
      throw e;
    } finally {
      td.end(rollback);
    }
  }
  
  public static Collection<Message> validatePaymentInfo(final NMOrderImpl order, final NMProfile profile, final PaymentInfo paymentInfo) {
    return PaymentUtil.getInstance().validatePaymentInfo(order, profile, paymentInfo);
  }
  
  public static void copyFieldsToCreditCard(final PaymentInfo paymentInfo, final MutableRepositoryItem creditCard) {
    PaymentUtil.getInstance().copyFieldsToCreditCard(paymentInfo, creditCard);
  }
  
  public static PaymentInfo creditCardToPaymentInfo(final NMCreditCard creditCard, final RepositoryItem defaultBilling) throws RepositoryException {
    return PaymentUtil.getInstance().creditCardToPaymentInfo(creditCard, defaultBilling);
  }
  
  public static Collection<Message> validateOrderAddresses(final NMOrderImpl order, final NMProfile profile) {
    return OrderShipUtil.getInstance().validateAddresses(order, profile);
  }
  
  public static Collection<Message> validateOrderShipping(final ShoppingCartHandler cart, final NMOrderImpl order, final NMProfile profile) {
    return OrderShipUtil.getInstance().validateShipping(cart, order, profile, isInternationalSession(profile));
  }
  
  public static Collection<Message> updateShopRunnerPoBoxesToStandard(final NMOrderImpl order) {
    return OrderShipUtil.getInstance().updateShopRunnerPoBoxesToStandard(order);
  }
  
  public static Collection<Message> validateOrder(final ShoppingCartHandler cart, final NMProfile profile, final NMProfileFormHandler profileHandler, final OrderInfo orderInfo) throws Exception {
    return validateOrder(cart, profile, profileHandler, orderInfo, false);
  }
  
  public static Collection<Message> validateOrder(final ShoppingCartHandler cart, final NMProfile profile, final NMProfileFormHandler profileHandler, final OrderInfo orderInfo,
          final boolean isInternational) throws Exception {
    boolean rollback = false;
    final TransactionDemarcation td = startTransaction();
    try {
      return OrderUtil.getInstance().validateOrder(cart, profile, profileHandler, orderInfo, isInternational);
    } catch (final Exception e) {
      rollback = true;
      throw e;
    } finally {
      td.end(rollback);
    }
  }
  
  public static void validateStoreSkuInventory(final NMOrderImpl order) throws Exception {
    boolean rollback = false;
    final TransactionDemarcation td = startTransaction();
    try {
      OrderUtil.getInstance().validateStoreSkuInventory(order);
    } catch (final Exception e) {
      rollback = true;
      throw e;
    } finally {
      td.end(rollback);
    }
  }
  
  public static Collection<Message> savePaymentChanges(final NMOrderImpl order, final NMProfile profile, final NMProfileFormHandler profileHandler, final String newDefaultCardName,
          final List<PaymentInfo> newCreditCards, final List<PaymentInfo> removedCreditCards) throws Exception {
    boolean rollback = false;
    final TransactionDemarcation td = startTransaction();
    try {
      return PaymentUtil.getInstance().savePaymentChanges(order, profile, profileHandler, newDefaultCardName, newCreditCards, removedCreditCards);
    } catch (final Exception e) {
      rollback = true;
      throw e;
    } finally {
      td.end(rollback);
    }
  }
  
  public static void submitOrder(final ShoppingCartHandler cart, final NMProfile profile, final NMProfileFormHandler profileHandler, final OrderInfo orderInfo) throws Exception {
    boolean rollback = false;
    final TransactionDemarcation td = startTransaction();
    try {
      OrderUtil.getInstance().submitOrder(cart, profile, profileHandler, orderInfo);
    } catch (final Exception e) {
      rollback = true;
      throw e;
    } finally {
      td.end(rollback);
    }
  }
  
  /*
	 * 
	 */
  
  public static void clearOutPaymentGroups(final NMOrderImpl order) throws Exception {
    boolean rollback = false;
    final TransactionDemarcation td = startTransaction();
    try {
      PaymentUtil.getInstance().clearOutPaymentGroups(order);
    } catch (final Exception e) {
      rollback = true;
      throw e;
    } finally {
      td.end(rollback);
    }
  }
  
  /*
	 * 
	 */
  
  public static SaveForLaterItemCollection getSaveForLaterItems(final NMProfile profile, final OrderManager orderMgr) {
    return SaveForLaterUtil.getInstance().getSaveForLaterItems(profile, orderMgr);
  }
  
  public static ResultBean moveSFLItemToCart(final ShoppingCartHandler cart, final String sflItemId, final NMProfileFormHandler profileFormHandler) throws Exception {
    boolean rollback = false;
    final TransactionDemarcation td = startTransaction();
    try {
      return SaveForLaterUtil.getInstance().moveSFLItemToCart(cart, sflItemId, profileFormHandler);
    } catch (final Exception e) {
      rollback = true;
      throw e;
    } finally {
      td.end(rollback);
    }
  }
  
  public static ResultBean moveItemToSFLCart(final String commerceItemId, final ShoppingCartHandler cart, final NMProfileFormHandler profileFormHandler) throws Exception {
    boolean rollback = false;
    final TransactionDemarcation td = startTransaction();
    try {
      return SaveForLaterUtil.getInstance().moveItemToSFLCart(commerceItemId, cart, profileFormHandler);
    } catch (final Exception e) {
      rollback = true;
      throw e;
    } finally {
      td.end(rollback);
    }
  }
  
  public static ResultBean removeItemFromSFLCart(final String itemId, final NMProfileFormHandler profileFormHandler) throws Exception {
    boolean rollback = false;
    final TransactionDemarcation td = startTransaction();
    try {
      return SaveForLaterUtil.getInstance().removeItemFromSFLCart(itemId, profileFormHandler);
    } catch (final Exception e) {
      rollback = true;
      throw e;
    } finally {
      td.end(rollback);
    }
  }
  
  public static boolean isInternationalSession(final NMProfile profile) {
    return CartUtil.getInstance().isInternationalSession(profile);
  }
  
  /**
   * Applies the promo code
   * 
   * @author nmve1
   * @param promoBean
   * @param cart
   * @param profile
   * @return
   * @throws Exception
   */
  public static ResultBean applyPromoCode(final PromoBean promoBean, final ShoppingCartHandler cart, final NMProfile profile) throws Exception {
    boolean rollback = false;
    final TransactionDemarcation td = startTransaction();
    try {
      return CartUtil.getInstance().applyPromoCode(promoBean, cart, profile);
    } catch (final Exception e) {
      rollback = true;
      System.err.println("CheckoutAPI.applyPromoCode() CartUtil exception: " + e.getMessage());
      throw e;
    } finally {
      if (null != td) {
        try {
          td.end(rollback);
        } catch (final Exception e) {
          System.err.println("CheckoutAPI.applyPromoCode() end transaction exception: " + e.getMessage());
        }
      }
    }
  }
  
  /**
   * Clears promo email validation
   * 
   * @author nmve1
   * @param request
   * @return
   * @throws Exception
   */
  public static ResultBean clearPromoEmailValidation(final PromoEmailClearRequest request) throws Exception {
    boolean rollback = false;
    final TransactionDemarcation td = startTransaction();
    try {
      return CartUtil.getInstance().clearPromoCodePendingValidation(request);
    } catch (final Exception e) {
      rollback = true;
      System.err.println("CheckoutAPI.clearPromoEmailValidation() CartUtil exception: " + e.getMessage());
      throw e;
    } finally {
      if (null != td) {
        try {
          td.end(rollback);
        } catch (final Exception e) {
          System.err.println("CheckoutAPI.clearPromoEmailValidation() end transaction exception: " + e.getMessage());
        }
      }
    }
  }
  
  public static boolean doFinCenCheck(final NMOrderImpl order) {
    if (!order.validateFinCen()) {
      return false;
    }
    
    boolean doFinCenCheck = false;
    final double total = PaymentUtil.getInstance().getSubTotalPriceOnGiftCardProductItems(order);
    
    if (total >= 1000) {
      doFinCenCheck = true;
    }
    return doFinCenCheck;
  }
  
  public static void validateFinCen(final FinCenRequest request, final FinCenResponse response) {
    PaymentUtil.getInstance().validateFinCen(request, response);
  }
  
  public static boolean upgradeTwoDay(final NMOrderImpl order, final NMProfile profile, final ShippingGroupAux shippingGroup) {
    final HardgoodShippingGroup shipGroup = (HardgoodShippingGroup) shippingGroup.getShippingGroupRef();
    return OrderUtil.getInstance().upgradeTwoDay(order, profile, shipGroup);
  }
  
  public static boolean upgradeOvernight(final NMOrderImpl order, final NMProfile profile, final ShippingGroupAux shippingGroup) {
    final HardgoodShippingGroup shipGroup = (HardgoodShippingGroup) shippingGroup.getShippingGroupRef();
    return OrderUtil.getInstance().upgradeOvernight(order, profile, shipGroup);
  }
  
  public static boolean containsGiftCard(final NMOrderImpl order) {
    return CartUtil.getInstance().containsGiftCard(order);
  }
  
  public static boolean containsVirtualGiftCard(final NMOrderImpl order) {
    return CartUtil.getInstance().containsVirtualGiftCard(order);
  }
  
  public static boolean containsVirtualGiftCardsOnly(final NMOrderImpl order) {
    return PayPalUtil.getInstance().containsVirtualGiftCardsOnly(order);
  }
  
  public static boolean isAlipayEligible(final NMOrderImpl order) {
    return CartUtil.getInstance().isAlipayEligible(order);
  }
  
  public static String setExpressCheckout(final NMOrderImpl order, final NMProfile profile, final String rootUrl, final String returnPath, final String cancelPath) throws PayPalException {
    return PayPalUtil.getInstance().setExpressCheckout(order, profile, rootUrl, returnPath, cancelPath);
  }
  
  public static String setExpressCheckout(final NMOrderImpl order, final NMProfile profile, final String rootUrl, final String returnPath, final String returnValue, final String cancelPath)
          throws PayPalException {
    return PayPalUtil.getInstance().setExpressCheckout(order, profile, rootUrl, returnPath, returnValue, cancelPath);
  }
  
  public static Collection<Message> setVmeCheckout(final String encKey, final String purchaseData, final String callId) throws VmeException {
    return VMeUtil.getInstance().setVmeCheckout(encKey, purchaseData, callId);
  }
  
  // applies only one active dollar off promotion based on Checkout type selected in CSR and the type of order and then applies the Promotion to order
  public static List<Message> applyActiveDollarOffPromoForCheckoutType(final NMOrderImpl order, final NMProfile profile, final List<Message> messages) throws CheckoutTypeException {
    return CommonComponentHelper.getNMCheckoutTypeUtil().applyActiveDollarOffPromoForCheckoutType(order, profile, messages);
  }
  
  /**
   * This method will remove the replenishable option for the replenishable items in the cart.
   * 
   * @param order
   *          Order object
   * @param orderManager
   *          OrderManager object
   * @throws CommerceException
   *           possible exception while editing/updating order
   */
  public static void removeReplenishmentOptionFromOrder(final NMOrderImpl order, final NMOrderManager orderManager) throws CommerceException {
    OrderUtil.getInstance().removeReplenishmentOptionFromOrder(order, orderManager);
  }
  
  public static List<Message> getExpressCheckout(final ShoppingCartHandler cart, final NMProfile profile, final String token) throws Exception {
    boolean rollback = false;
    final List<Message> messages = new ArrayList<Message>();
    final TransactionDemarcation td = startTransaction();
    try {
      messages.addAll(PayPalUtil.getInstance().getExpressCheckout(cart, profile, token));
    } catch (final Exception e) {
      rollback = true;
      throw e;
    } finally {
      td.end(rollback);
    }
    return messages;
  }
  
  /**
   * This method is used to set the Express PayPal Shipping and Billing info to Order
   * 
   * @param cart
   * @param profile
   * @param nmBorderFreePayPalResponse
   * @throws Exception
   */
  public static void setExpPaypalInfoToOrder(final ShoppingCartHandler cart, final NMProfile profile, final NMBorderFreePayPalResponse nmBorderFreePayPalResponse) throws Exception {
    boolean rollback = false;
    final TransactionDemarcation td = startTransaction();
    try {
      final NMOrderImpl order = cart.getNMOrder();
      final NMOrderManager orderMgr = (NMOrderManager) cart.getOrderHolder().getOrderManager();
      final boolean isSuccessfulUpdate = OrderUtil.getInstance().setExpPaypalInfoToOrder(order, orderMgr, profile, nmBorderFreePayPalResponse);
      if (isSuccessfulUpdate) {
        orderMgr.updateOrder(order);
      }
    } catch (final Exception e) {
      rollback = true;
      throw e;
    } finally {
      td.end(rollback);
    }
  }
  
  public static void updateProfileShippingAddrToOrder(final ShoppingCartHandler cart, final NMProfile profile) throws Exception {
    boolean rollback = false;
    final TransactionDemarcation td = startTransaction();
    try {
      final NMOrderImpl order = cart.getNMOrder();
      final NMOrderManager orderMgr = (NMOrderManager) cart.getOrderManager();
      OrderUtil.getInstance().updateProfileShippingAddrToOrder(order, orderMgr, profile);
      orderMgr.updateOrder(order);
    } catch (final Exception e) {
      rollback = true;
      throw e;
    } finally {
      td.end(rollback);
    }
  }
  
  public static boolean usePayPalBillingAgreement(final ShoppingCartHandler cart, final NMProfile profile) throws Exception {
    boolean rollback = false;
    final TransactionDemarcation td = startTransaction();
    try {
      return PayPalUtil.getInstance().useBillingAgreement(cart, profile);
    } catch (final Exception e) {
      rollback = true;
      throw e;
    } finally {
      td.end(rollback);
    }
  }
  
  public static String getSetExpressCheckoutRedirectUrl(final String token) {
    return PayPalUtil.getInstance().getPayPal().getExpressCheckoutRedirectUrl(token);
  }
  
  public static Collection<Message> handlePayPalCheckout(final ShoppingCartHandler cart, final NMProfile profile, final String rootUrl, final String token) throws Exception {
    boolean rollback = false;
    final TransactionDemarcation td = startTransaction();
    try {
      return PayPalUtil.getInstance().handlePayPalCheckout(cart, profile, rootUrl, token);
    } catch (final Exception e) {
      rollback = true;
      throw e;
    } finally {
      td.end(rollback);
    }
  }
  
  public static boolean isShipToStoreEligibleByLocation(final NMProfile profile) {
    return ShipToStoreUtil.getInstance().isShipToStoreEligibleByLocation(profile);
  }
  
  public static boolean isShipToStoreEligibleByItems(final List<NMCommerceItem> nmCommerceItems) {
    return ShipToStoreUtil.getInstance().isShipToStoreEligibleByItems(nmCommerceItems);
  }
  
  public static boolean getIsShipToStore(final NMOrderImpl order) {
    return ShipToStoreUtil.getInstance().isShipToStore(order);
  }
  
  public static void setPayPalToken(final DynamoHttpServletRequest request, final String token) {
    PayPalUtil.getInstance().getPayPal().setToken(request, token);
  }
  
  public static String getPayPalToken(final DynamoHttpServletRequest request) {
    return PayPalUtil.getInstance().getPayPal().getToken(request);
  }
  
  public static void deletePayPalToken(final DynamoHttpServletRequest request) {
    PayPalUtil.getInstance().getPayPal().deleteToken(request);
  }
  
  public static List<Message> validateCartForBorderFreeCheckout(final NMProfile profile, final NMOrderImpl order) {
    return LoginUtil.getInstance().validateCartForBorderFreeCheckout(profile, order);
  }
  
  public static void resetInternationalOrderProperties(final NMOrderImpl order, final ShoppingCartHandler cart, final NMBorderFreeGetQuoteResponse bfResponse) throws Exception {
    boolean rollback = false;
    final TransactionDemarcation td = startTransaction();
    try {
      final OrderManager orderMgr = cart.getOrderManager();
      final boolean isSuccessfulUpdate = OrderUtil.getInstance().resetInternationalOrderProperties(order, bfResponse);
      if (isSuccessfulUpdate) {
        orderMgr.updateOrder(order);
      }
    } catch (final Exception e) {
      rollback = true;
      throw e;
    } finally {
      td.end(rollback);
    }
  }
  
  public static Collection<Message> createInternationalPayPalCard(final ShoppingCartHandler cart, final NMProfile profile, final String payPalPayerId) throws Exception {
    boolean rollback = false;
    final TransactionDemarcation td = startTransaction();
    try {
      final NMOrderImpl order = cart.getNMOrder();
      final NMOrderManager orderMgr = (NMOrderManager) cart.getOrderManager();
      return OrderUtil.getInstance().createPayPalCard(order, orderMgr, profile, payPalPayerId);
    } catch (final Exception e) {
      rollback = true;
      throw e;
    } finally {
      td.end(rollback);
    }
  }
  
  public static boolean isExpPaypalAddressSupportedByBorderfree(final Address address) {
    // ContactInfo address = getContactInfo(profile, ProfileProperties.Profile_billingAddress);
    boolean isValidAddr = false;
    if ((address != null) && !StringUtils.isBlank(address.getCountry())) {
      isValidAddr = isValidBorderFreeBillToCountry(address.getCountry()) && isProvinceAndZipCodeValid(address.getCountry(), address.getState(), address.getPostalCode());
    }
    return isValidAddr;
  }
  
  public static boolean isAddressSupportedByBorderfree(final ContactInfo address) {
    // ContactInfo address = getContactInfo(profile, ProfileProperties.Profile_billingAddress);
    boolean isValidAddr = false;
    if ((address != null) && !StringUtils.isBlank(address.getCountry())) {
      isValidAddr = isValidBorderFreeBillToCountry(address.getCountry()) && isProvinceAndZipCodeValid(address.getCountry(), address.getState(), address.getPostalCode());
    }
    return isValidAddr;
  }
  
  public static boolean isValidBorderFreeBillToCountry(final String countryCode) {
    boolean isValidCountry = false;
    // Borderfree suggested to use the Ship Enabled + US as billing country list.
    final RepositoryItem[] billToCountryList = CheckoutComponents.getLocalizationUtils().getAllShipEnabledCountries();
    if ((billToCountryList != null) && (billToCountryList.length > 0)) {
      for (final RepositoryItem countryItem : billToCountryList) {
        final String country = (String) countryItem.getPropertyValue("countryCode");
        if (countryCode.equals(country)) {
          isValidCountry = true;
          // Found the country. Do not iterate more.
          break;
        }
      }
    }
    return isValidCountry;
  }
  
  public static boolean isProvinceValidForCountry(final String country, final String provinceCode) {
    boolean isValidProvince = false;
    if (!StringUtils.isBlank(provinceCode)) {
      final Province province = CheckoutComponents.getProvinceArray().getProvince(country, provinceCode);
      // Make sure we have a valid province code for the country code.
      isValidProvince = !StringUtils.isBlank(province.getCountryCode()) && !StringUtils.isBlank(province.getProvinceCode());
    }
    return isValidProvince;
  }
  
  public static boolean isProvinceAndZipCodeValid(final String country, final String province, final String zipCode) {
    boolean isZipValid = false;
    boolean isProvinceValid = true;
    final LocalizationUtils utils = CheckoutComponents.getLocalizationUtils();
    if (utils.isZipcodeMandatoryForCountry(country)) {
      isZipValid = utils.isZipcodeValidForForeignCountry(zipCode, country);
    } else {
      // ZipCode is not mandatory for the country selected.
      isZipValid = true;
    }
    // By default provice is valid. Province check is only for CA, ensure Province check is controlled.
    if (!StringUtils.isBlank(utils.getShowGlobalProvincesForCountryCode()) && utils.getShowGlobalProvincesForCountryCode().equals(country)) {
      isProvinceValid = isProvinceValidForCountry(country, province);
    }
    return isZipValid && isProvinceValid;
  }
  
  /**
   * Utility method to Sanitize the International Shipping address before placing order.
   * 
   * @param order
   *          Order object
   * @param orderMgr
   *          OrderManager
   * @param profile
   *          Profile object
   * */
  @SuppressWarnings("unchecked")
  public static void copyInternationalShippingAddressToProfileFromOrder(final NMOrderImpl order, final OrderManager orderMgr, final NMProfile profile) throws Exception {
    if (order.getShippingGroupCount() > 0) {
      final List<ShippingGroup> shippingGroups = order.getShippingGroups();
      final HardgoodShippingGroup shippingGroup = (HardgoodShippingGroup) shippingGroups.get(0);
      final ContactInfo orderAddress = orderAddressToContactInfo(shippingGroup.getRepositoryItem());
      final ContactInfo profileAddress = profileAddressToContactInfo(profile.getShippingAddress());
      // Verify the address is same. Otherwise, update the address.
      if ((profileAddress == null)
              || (orderAddress == null)
              || !(StringUtilities.areEqualIgnoreCase(profileAddress.getFirstName(), orderAddress.getFirstName())
                      && StringUtilities.areEqualIgnoreCase(profileAddress.getLastName(), orderAddress.getLastName())
                      && StringUtilities.areEqualIgnoreCase(profileAddress.getAddress1(), orderAddress.getAddress1())
                      && StringUtilities.areEqualIgnoreCase(profileAddress.getAddress2(), orderAddress.getAddress2())
                      && StringUtilities.areEqualIgnoreCase(profileAddress.getCity(), orderAddress.getCity()) && StringUtilities.areEqualIgnoreCase(profileAddress.getZip(), orderAddress.getZip())
                      && StringUtilities.areEqualIgnoreCase(profileAddress.getCountry(), orderAddress.getCountry()) && StringUtilities.areEqualIgnoreCase(profileAddress.getPhoneNumber(),
                      orderAddress.getPhoneNumber()))) {
        boolean rollback = false;
        final TransactionDemarcation td = startTransaction();
        try {
          CheckoutAPI.updateProfileShippingAddress(profile, orderAddress, true);
        } catch (final Exception e) {
          rollback = true;
          throw new Exception("Unable to validate the International ShippingAddress");
        } finally {
          td.end(rollback);
        }
      }
    }
  }
  
  // 41279 - Multiple shipping addresses are not supported by International checkout.
  public static boolean cleanupShippingGroupsForInternationalCheckout(final NMOrderImpl order, final OrderManager orderMgr) throws Exception {
    boolean rollback = false;
    final TransactionDemarcation td = startTransaction();
    try {
      return OrderUtil.getInstance().cleanupShippingGroupsForInternationalCheckout(order, orderMgr);
    } catch (final Exception e) {
      rollback = true;
      throw e;
    } finally {
      td.end(rollback);
    }
  }
  
  // 41279 - Reset the commerce items to new address.
  public static void resetCommerceItemsToInternationalShippingAddress(final NMProfile profile, final ContactInfo address, final NMOrderImpl order, final OrderManager orderMgr)
          throws CommerceException, Exception {
    boolean rollback = false;
    final TransactionDemarcation td = startTransaction();
    try {
      OrderUtil.getInstance().resetCommerceItemsToInternationalShippingAddress(profile, address, order, orderMgr);
    } catch (final Exception e) {
      rollback = true;
      throw e;
    } finally {
      td.end(rollback);
    }
  }
  
  public static void resetCommerceItemsToShopRunnerShippingAddress(final NMProfile profile, final ContactInfo address, final NMOrderImpl order, final OrderManager orderMgr) throws CommerceException,
          Exception {
    boolean rollback = false;
    final TransactionDemarcation td = startTransaction();
    try {
      OrderUtil.getInstance().resetCommerceItemsToShopRunnerShippingAddress(profile, address, order, orderMgr);
    } catch (final Exception e) {
      rollback = true;
      throw e;
    } finally {
      td.end(rollback);
    }
  }
  
  /**
   * Get Shoprunner payment Info
   * 
   * @param shopRunnerCartId
   * @param orderTotal
   * @return GetShoprunnerPaymentInfoResponse
   * @throws Exception
   */
  public static GetShoprunnerPaymentInfoResponse getShopRunnerPaymentInfo(final String shopRunnerCartId, final double orderTotal) throws Exception {
    ApplicationLogging mLogging = getApplicationLogging();
    boolean rollback = false;
    final TransactionDemarcation td = startTransaction();
    try {
      return shopRunnerESBUtilDiscoverable.getShopRunnerPaymentInfo(shopRunnerCartId, orderTotal);
    } catch (final Exception e) {
      if (mLogging != null && mLogging.isLoggingError()) {
        mLogging.logError("Exception caught in getShopRunnerPaymentInfo " + e.getMessage());
      }
      if (mLogging != null && mLogging.isLoggingDebug()) {
        mLogging.logDebug("Exception caught in getShopRunnerPaymentInfo ", e);
      }
      rollback = true;
      throw e;
    } finally {
      td.end(rollback);
    }
  }
  
  /**
   * Method to set address to given shipping group.
   * 
   * @param shippingGrp
   *          Shipping Group to be updated
   * @param address
   *          Address value to be assigned to SG
   * @return transaction success status
   * @throws Exception
   * */
  public static boolean updateShipingGroupAddressToNewAddress(final ShippingGroupAux shippingGrp, final NMAddress address) throws Exception {
    boolean rollback = false;
    boolean success = false;
    final TransactionDemarcation td = startTransaction();
    try {
      final AddressUtil util = AddressUtil.getInstance();
      final ContactInfo contactInfo = util.profileAddressToContactInfo(address);
      util.copyFieldsToOrderAddress(contactInfo, shippingGrp.getAddress().getRepositoryItem());
      success = true;
    } catch (final Exception e) {
      rollback = true;
      throw e;
    } finally {
      td.end(rollback);
    }
    return success;
  }
  
  public static boolean isShopRunnerEnabled() {
    return ShopRunnerUtil.getInstance().isEnabled();
  }
  
  public static String getShopRunnerVendorId(final boolean isSRExpressCheckOut) {
    return ShopRunnerUtil.getInstance().getVendorId(isSRExpressCheckOut);
  }
  
  public static String getAlipayVendorId(final boolean isAlipayExpressCheckOut) {
    return ShopRunnerUtil.getInstance().getAlipayVendorId(isAlipayExpressCheckOut);
  }
  
  public static void setShopRunnerToken(final DynamoHttpServletRequest request, final DynamoHttpServletResponse response, final String token) {
    ShopRunnerUtil.getInstance().setToken(request, response, token);
  }
  
  public static String getShopRunnerToken(final DynamoHttpServletRequest request) {
    return ShopRunnerUtil.getInstance().getToken(request);
  }
  
  public static void deleteShopRunnerToken(final DynamoHttpServletRequest request, final DynamoHttpServletResponse response) {
    ShopRunnerUtil.getInstance().deleteToken(request, response);
  }
  
  /*
   * public static void deleteShopRunnerSessionId(DynamoHttpServletRequest request, DynamoHttpServletResponse response) { ShopRunnerUtil.getInstance().deleteSessionId(request, response); }
   * 
   * public static void setShopRunnerSessionId(DynamoHttpServletRequest request, DynamoHttpServletResponse response) { ShopRunnerUtil.getInstance().setSessionId(request, response); }
   */
  
  public static boolean isShopRunnerSessionValid(final DynamoHttpServletRequest request) {
    return ShopRunnerUtil.getInstance().isSessionValid(request);
  }
  
  public static String getPayPalTokenParam(final DynamoHttpServletRequest request) {
    return PayPalUtil.getInstance().getPayPal().getTokenParam(request);
  }
  
  public static String getPayPalReturnParam(final DynamoHttpServletRequest request) {
    return PayPalUtil.getInstance().getPayPal().getReturnParam(request);
  }
  
  public static boolean isPayPalReturned(final DynamoHttpServletRequest request) {
    return PayPalUtil.getInstance().getPayPal().isReturned(request);
  }
  
  public static boolean isPayPalCanceled(final DynamoHttpServletRequest request) {
    return PayPalUtil.getInstance().getPayPal().isCanceled(request);
  }
  
  public static boolean isDisplayPayPalInfo(final NMProfile profile, final String payPalToken) {
    return PayPalUtil.getInstance().getPayPal().isDisplayPaymentInfo(profile, payPalToken);
  }
  
  public static void setAddressType(final String addressType) {
    CheckoutAPI.addressType = addressType;
  }
  
  public static String getAddressType() {
    return CheckoutAPI.addressType;
  }
  
  public static Set<String> getValidServiceLevels(final String shipToCountry, final NMCommerceItem ci) {
    return ShippingUtil.getInstance().getValidServiceLevels(shipToCountry, ci);
  }
  
  public static ServiceLevel[] getValidServiceLevels(final Order order, final List<Set<String>> serviceLevelSets) throws RepositoryException {
    return ShippingUtil.getInstance().getValidServiceLevels(order, serviceLevelSets);
  }
  
  public static ServiceLevel[] getValidServiceLevelDesc(final Order order, final ServiceLevel[] serviceLevels, final String countryCode) throws RepositoryException {
    return ShippingUtil.getInstance().getValidServiceLevelDesc(order, serviceLevels, countryCode);
  }
  
  public static Set<ServiceLevel> applyAbTestsAndPromotions(final Set<ServiceLevel> serviceLevels, final NMOrderImpl order) {
    return ServiceLevelUtil.getInstance().applyAbTestsAndPromotions(serviceLevels, order);
  }
  
  public static Set<ServiceLevel> getCommerceItemValidServiceLevels(final NMCommerceItem item, final String shipToCountry) {
    return ServiceLevelUtil.getInstance().getCommerceItemValidServiceLevels(item, shipToCountry);
  }
  
  public static boolean isBopsEnabled() {
    return ShipToStoreUtil.getInstance().isBopsEnabled();
  }
  
  /**
   * Added the code to access the method in AddressUtil as scope of addressUtil is default
   * 
   * @param pinyinCheckString
   * @return
   */
  public static boolean invalidLatinOrPinyinCharacters(final String pinyinCheckString) {
    return AddressUtil.invalidLatinOrPinyinCharacters(pinyinCheckString);
  }
  
  public static ContactInfo processShopRunnerProfileData(final NMProfile profile, final JSONObject addressObj, final DynamoHttpServletRequest request) throws Exception {
    return ShopRunnerUtil.getInstance().processShopRunnerProfileData(profile, addressObj, request);
  }
  
  public static void clearShopRunnerData(final NMProfile profile, final ShoppingCartHandler cartHandler) throws Exception {
    ShopRunnerUtil.getInstance().clearShopRunnerData(profile, cartHandler);
  }
  
  public static int getActionRequiredPromoType(final String promoCode) {
    return ShopRunnerUtil.getInstance().getActionRequiredPromoType(promoCode);
  }
  
  public static JSONObject buildCart(final CheckoutPageModel pageModel, final NMOrderImpl order, final NMOrderManager orderMgr, final String dropShipPh) throws Exception {
    return ShopRunnerUtil.getInstance().buildCart(pageModel, order, orderMgr, dropShipPh);
  }
  
  public static JSONObject buildAlipayProduct(final JSONObject srProduct, final NMCommerceItem item, final NMProduct p, final CheckoutPageModel pageModel) {
    return ShopRunnerUtil.getInstance().buildAlipayProduct(srProduct, item, p, pageModel);
  }
  
  public static JSONArray buildSRAdornmentArray(final NMCommerceItem item) throws Exception {
    return ShopRunnerUtil.getInstance().buildSRAdornmentArray(item);
  }
  
  public static JSONArray buildPromotions(final NMOrderImpl order) {
    return ShopRunnerUtil.getInstance().buildPromotions(order);
  }
  
  public static JSONArray buildGiftCards(final NMOrderImpl order) {
    return ShopRunnerUtil.getInstance().buildGiftCards(order);
  }
  
  public static boolean isPromoAlreadyApplied(final String activatedPromoCode, final String promoCode) {
    return ShopRunnerUtil.getInstance().isPromoAlreadyApplied(activatedPromoCode, promoCode);
  }
  
  public static List<GiftCard> getGiftCardsFromOrder(final NMOrderImpl order) {
    return ShopRunnerUtil.getInstance().getGiftCardsFromOrder(order);
  }
  
  public static boolean updateOrderProfile(final ShoppingCartHandler cart, final RepositoryItem profile, final String orderId) throws Exception {
    boolean rollback = false;
    boolean updated = false;
    final TransactionDemarcation td = startTransaction();
    try {
      final NMOrderManager orderMgr = (NMOrderManager) cart.getOrderManager();
      NMOrderImpl order = null;
      order = (NMOrderImpl) orderMgr.loadOrder(orderId);
      order.setProfileId(profile.getRepositoryId());
      orderMgr.updateOrder(order);
      updated = true;
    } catch (final Exception e) {
      rollback = true;
      throw e;
    } finally {
      td.end(rollback);
    }
    return updated;
  }
  
  private static TransactionDemarcation startTransaction() throws TransactionDemarcationException {
    final TransactionDemarcation td = new NMTransactionDemarcation();
    final TransactionManager tm = CommonComponentHelper.getTransactionManager();
    td.begin(tm, TransactionDemarcation.REQUIRED);
    return td;
  }
  
  public static boolean hasEmptyShippingAddress(final NMOrderImpl order) {
    return OrderShipUtil.getInstance().hasEmptyShippingAddress(order);
  }
  
  public static void addDefaultPaymentGroupToOrder(final NMOrderImpl order, final NMOrderManager orderMgr) throws CommerceException, TransactionDemarcationException {
    boolean rollback = false;
    TransactionDemarcation td = null;
    try {
      td = startTransaction();
      PaymentUtil.getInstance().addDefaultPaymentGroupToOrder(order, orderMgr);
    } catch (final CommerceException commerceException) {
      rollback = true;
      throw commerceException;
    } catch (final TransactionDemarcationException tde) {
      rollback = true;
      throw tde;
    } finally {
      
      if (td != null) {
        td.end(rollback);
      }
      
    }
  }
  
  public static void updateItemStoreNo(final DynamoHttpServletRequest request, final NMOrderImpl order) throws Exception {
    TransactionDemarcation td = null;
    boolean rollback = false;
    try {
      td = startTransaction();
      ShipToStoreUtil.getInstance().updateItemStoreNo(request, order);
    } catch (final CommerceException commerceException) {
      rollback = true;
      throw commerceException;
    } catch (final TransactionDemarcationException tde) {
      rollback = true;
      throw tde;
    } finally {
      if (td != null) {
        td.end(rollback);
      }
      
    }
  }
  
  public static void changeShippingAddressToStoreAddress(final ShoppingCartHandler cart, final NMOrderImpl order, final NMProfile profile) throws Exception {
    TransactionDemarcation td = null;
    boolean rollback = false;
    try {
      td = startTransaction();
      ShipToStoreUtil.getInstance().changeShippingAddressToStoreAddress(cart, order, profile);
    } catch (final CommerceException commerceException) {
      rollback = true;
      throw commerceException;
    } catch (final TransactionDemarcationException tde) {
      rollback = true;
      throw tde;
    } finally {
      if (td != null) {
        td.end(rollback);
      }
    }
    
  }
  
  /**
   * Method to detect if we need to update the shipping address to a store address. At least 1 item should have pickup store and either BOPS enable or StoreCall selected.
   * 
   * @param bopsEnabled
   *          BOPS flag
   * @param associateLoggedIn
   * @param storeCallEnabled
   * @param order
   *          Order object to check, if user selected a store.
   * @return boolean indicating that need to update shipping address to store address for selected CommerceItems.
   * */
  public static boolean shouldUpdateShippingAddressToStoreAdress(final boolean bopsEnabled, final boolean associateLoggedIn, final boolean storeCallEnabled, final NMOrderImpl order) {
    final boolean updateRequired = (bopsEnabled || (associateLoggedIn && storeCallEnabled)) && order.hasBopsItem();
    return updateRequired;
  }
  
  public static boolean hasMustShipWithItemGWP(final List<NMCommerceItem> nmCommerceItems) {
    return OrderUtil.getInstance().isHasMustShipWithItemGWP(nmCommerceItems);
  }
  
  public static void evaluateGwpMustShipWith(final NMOrderImpl order, final NMOrderManager orderMgr) throws Exception {
    TransactionDemarcation td = null;
    boolean rollback = false;
    try {
      td = startTransaction();
      OrderUtil.getInstance().evaluateGwpMustShipWith(order, orderMgr);
    } catch (final CommerceException commerceException) {
      rollback = true;
      throw commerceException;
    } catch (final TransactionDemarcationException tde) {
      rollback = true;
      throw tde;
    } finally {
      if (td != null) {
        td.end(rollback);
      }
    }
    
  }
  
  public static void correctGwpMustShipWithAndBOPSIssue(final ShoppingCartHandler cart, final NMOrderImpl order) throws CommerceException, TransactionDemarcationException {
    TransactionDemarcation td = null;
    boolean rollback = false;
    try {
      td = startTransaction();
      ShipToStoreUtil.getInstance().correctGwpMustShipWithAndBOPSIssue(cart, order);
    } catch (final CommerceException commerceException) {
      rollback = true;
      throw commerceException;
    } catch (final TransactionDemarcationException tde) {
      rollback = true;
      throw tde;
    } finally {
      if (td != null) {
        td.end(rollback);
      }
    }
    
  }
  
  public static List<HardgoodShippingGroup> getHardGoodShippingGroups(final NMOrderImpl order) {
    return OrderUtil.getInstance().getHardgoodShippingGroups(order);
  }
  
  // VMe changes starts
  
  public static boolean isVmeReturned(final DynamoHttpServletRequest request) {
    return VMeUtil.getInstance().getvMe().isReturned(request);
  }
  
  public static void clearVmeDetails(final NMOrderImpl order) {
    VMeUtil.getInstance().clearVmeDetails(order);
  }
  
  public static List<String> splitVmeFirstAndLastName(final String fullName) throws Exception {
    return VMeUtil.getInstance().splitVmeFirstandLastName(fullName);
  }
  
  public static void resetBackVmeDetails(final VMeInfoVO vMeInfoVO, final NMOrderImpl order) {
    VMeUtil.getInstance().resetBackVmeDetails(vMeInfoVO, order);
  }
  
  public static boolean isVmeShippingAddrChanged(final NMOrderImpl order) throws Exception {
    return VMeUtil.getInstance().isVmeShippingAddrChanged(order);
  }
  
  // Vme changes
  public static List<Message> processVisaCheckout(final ShoppingCartHandler cart, final NMProfile profile) throws Exception {
    boolean rollback = false;
    List<Message> messages = new ArrayList<Message>();
    final TransactionDemarcation td = startTransaction();
    try {
      messages.addAll(VMeUtil.getInstance().updateBillingAndShippingDetails(cart, profile));
      if (messages.isEmpty()) {
        final NMOrderImpl order = cart.getNMOrder();
        // VisaCheckout - applying visa checkout only dollar off promotion to the order if the order is visa checkout order
        messages = applyActiveDollarOffPromoForCheckoutType(order, profile, messages);
      }
    } catch (final Exception e) {
      rollback = true;
      throw e;
    } finally {
      td.end(rollback);
    }
    return messages;
  }
  
  /**
   * deptQuantity restrictions
   */
  public static ResultBean checkDepartmentQuantityRestriction(final NMOrderImpl order) {
	    return OrderUtil.getInstance().checkDepartmentQuantityRestriction(order);	     
  }

  
  /**
   * Thie method invokes MasterPassUtil's clearMasterPassDetails method
   * 
   * @param order
   */
  public static void clearMasterPassDetails(final NMOrderImpl order) {
    CommonComponentHelper.getMasterPassUtil().clearMasterPassDetails(order);
  }
  
  /**
   * This method invokes MasterPassUtil's methods to set MasterPass data to Order
   * 
   * @param cart
   * @param profile
   * @return messages - type List<Message>
   * @throws Exception
   */
  public static List<Message> processMasterPassCheckout(final ShoppingCartHandler cart, final NMProfile profile) throws Exception {
    boolean rollback = false;
    List<Message> messages = new ArrayList<Message>();
    final TransactionDemarcation td = startTransaction();
    try {
      messages.addAll(CommonComponentHelper.getMasterPassUtil().updateBillingAndShippingDetails(cart, profile));
      final NMOrderImpl order = cart.getNMOrder();
      // MasterPass - applying Master Pass only dollar off promotion to the order if the order is Master Pass order
      messages = applyActiveDollarOffPromoForCheckoutType(order, profile, messages);
    } catch (final Exception e) {
      rollback = true;
      throw e;
    } finally {
      td.end(rollback);
    }
    return messages;
  }
  
  /**
   * This method returns CommerceItem from SaveForLaterItem
   * 
   * @param itemId
   * @param profile
   * @param orderMgr
   * @return
   */
  public static NMCommerceItem getCommerceItemFromSflItem(final String itemId, final NMProfile profile, final NMOrderManager orderMgr) {
    final NMCommerceItem item = SaveForLaterUtil.getInstance().getCommerCeItem(itemId, profile, orderMgr);
    return item;
  }
  
  public static ServiceLevel getServiceLevelByCodeAndCountry(final String serviceLevelCode, final String countryCode) {
    return ServiceLevelUtil.getInstance().getServiceLevelByCodeAndCountry(serviceLevelCode, countryCode);
  }
  
  public static boolean shopRunnerSL = false;
  
  public static boolean isShopRunnerSL() {
    return shopRunnerSL;
  }
  
  public static void setShopRunnerSL(final boolean shopRunnerSL) {
    CheckoutAPI.shopRunnerSL = shopRunnerSL;
  }
  
  /**
   * The purpose of this method is to call the updateShippingGroups in either EstimatedDeliveryDateShippingGroupUtil or ShippingGroupUtil based on the output of redirectBasedOnServiceCall method in
   * shippingInfoFactory
   * 
   * @param order
   *          the order
   * @param pageModel
   *          the page model
   * @param checkoutPageDefinition
   *          the checkout page definition
   */
  public static void updateShippingGroupsBasedOnServiceCall(NMOrderImpl order, CheckoutPageModel pageModel, CheckoutPageDefinition checkoutPageDefinition) {
    ShippingInfoFactory shippingInfoFactory = new ShippingInfoFactory();
    shippingInfoFactory.redirectBasedOnServiceCall(order, pageModel, checkoutPageDefinition).updateShippingGroups(order, pageModel, checkoutPageDefinition);
  }
  
  /**
   * The purpose of this method is to call the Update all shipping groups based on service levels from cmos .
   * 
   * @param orderMgr
   *          the order mgr
   * @param order
   *          the order
   * @param profile
   *          the profile
   * @param serviceLevelCode
   *          the service level code
   * @param currentServiceLevelCode
   *          the current service level code
   * @return the collection
   * @throws CommerceException
   *           the commerce exception
   */
  public static Collection<Message> updateAllShippingGroupsBasedOnServiceLevelsFromCmos(NMOrderManager orderMgr, NMOrderImpl order, NMProfile profile, String serviceLevelCode,
          String currentServiceLevelCode, boolean serviceLevelsFetchedFromCmos) throws CommerceException {
    Collection<Message> message = null;
	ApplicationLogging mLogging = getApplicationLogging();
	TransactionDemarcation td = null;
	boolean rollback = false;
	try {
	  td = startTransaction();
	  message = EstimatedDeliveryDateShippingGroupUtil.getInstance().updateServiceLevelsFromCmos(orderMgr, order, profile, serviceLevelCode, currentServiceLevelCode, serviceLevelsFetchedFromCmos);
	} catch (final TransactionDemarcationException transactionDemarcationException) {
	  rollback = true;
	  if (mLogging != null && mLogging.isLoggingError()) {
		mLogging.logError("Exception caught in updateAllShippingGroupsBasedOnServiceLevelsFromCmos, orderID = " + order.getId() + ", profileID = " + order.getProfileId() + ", caused by: "+ transactionDemarcationException.getMessage());
	  }
	  if (mLogging != null && mLogging.isLoggingDebug()) {
		mLogging.logDebug("Exception caught in updateAllShippingGroupsBasedOnServiceLevelsFromCmos: ", transactionDemarcationException);
	  }
	} finally {
	  try {
	    if(td!=null) {
		  td.end(rollback);
	    }
	  } catch(final TransactionDemarcationException tde){
	    if (mLogging != null && mLogging.isLoggingError()) {
	      mLogging.logError("Exception caught while ending transaction demarcation in updateAllShippingGroupsBasedOnServiceLevelsFromCmos: " + tde.getMessage());
	    }
	    if (mLogging != null && mLogging.isLoggingDebug()) {
	      mLogging.logDebug("Exception caught while ending transaction demarcation in updateAllShippingGroupsBasedOnServiceLevelsFromCmos: ", tde);
	    }
	  }
	}
	return message;	  
  }
  
  /**
   * The purpose of this method is to call the convert cmos service level response to service level object method through CheckoutAPI class.
   * 
   * @param estimatedDeliveryDateServiceLevelVO
   *          the estimated delivery date service level vo
   * @return the sets the
   */
  public static Set<ServiceLevel> convertCmosServiceLevelResponseToServiceLevelObject(final List<EstimatedDeliveryDateServiceLevelVO> estimatedDeliveryDateServiceLevelVO) {
    return EstimatedDeliveryDateShippingGroupUtil.getInstance().convertCmosServiceLevelResponseTOServiceLevelObject(estimatedDeliveryDateServiceLevelVO);
  }
  
  /**
   * The purpose of this method is to call the cmos service level by code method through CheckoutAPI class.
   * 
   * @param serviceLevelCode
   *          the service level code
   * @param serviceLevelsFromCmos
   *          the service levels from cmos
   * @return the cmos service level by code
   */
  public static ServiceLevel getCmosServiceLevelByCode(String serviceLevelCode, List<EstimatedDeliveryDateServiceLevelVO> serviceLevelsFromCmos) {
    return EstimatedDeliveryDateShippingGroupUtil.getInstance().getCmosServiceLevelByCode(serviceLevelCode, serviceLevelsFromCmos);
  }
  
  /**
   * The purpose of this method is to call the update Same day delivery properties method through CheckoutAPI class.
   * 
   * @param order
   *          the order
   * @param pageModel
   *          the page model
   */
  public static void updateSameDayDeliveryProperties(NMOrderImpl order, CheckoutPageModel pageModel) {
    EstimatedDeliveryDateShippingGroupUtil.getInstance().updateSameDayDeliveryProperties(order, pageModel);
  }
  
  /**
   * The purpose of this method is to clear session after a user checks out using express Shop Runner checkout.
   */
  public static void clearEstimatedDeliveryDateInfo() {
    final ShippingInfoUtil shippingInfoUtil = new ShippingInfoUtil();
    shippingInfoUtil.clearEstimatedDeliveryDateInfo();
  }
  
  /** This method is responsible for merging current and persistent order.
   * @param cart order holder
   * @param orderManager order manager
   * @throws Exception any exception thrown
   */
  public static void mergeCart(final ShoppingCartHandler cart, final OrderManager orderManager) throws Exception {
	  boolean rollback = false;
	  final TransactionDemarcation td = startTransaction();
	  try {

		  NMOrderImpl persistentOrder = null;
		  final NMOrderImpl currentOrder = cart.getNMOrder();

		  if (!cart.getOrderHolder().getSaved().isEmpty()) {
			  final int size = cart.getOrderHolder().getSaved().size();
			  persistentOrder = ((NMOrderImpl) (cart.getOrderHolder().getSaved().toArray())[size - 1]);           
			  removeDuplicateItems(persistentOrder,currentOrder,orderManager);
			  removePersCartItems(persistentOrder,orderManager);

			  synchronized (currentOrder) {
				  orderManager.mergeOrders( persistentOrder,currentOrder,true,false);
				  orderManager.updateOrder(currentOrder);
			  }
		  }
	  } catch (final Exception e) {
		  rollback = true;
		  throw e;
	  } finally {
		  td.end(rollback);
	  }
  }



  /** This method removes the duplicate items during merge of current and persistent order. 
   * @param persistentOrder logged in profile saved order
   * @param currentOrder current anonymous order
   * @param orderManager order manager
   * @throws Exception any exception thrown
   */
  @SuppressWarnings("unchecked")
  private static void removeDuplicateItems(NMOrderImpl persistentOrder,NMOrderImpl currentOrder, OrderManager orderManager) throws Exception {
	  final List<String> ids = new ArrayList<String>();
	  final List<String> itemsToRemove = new ArrayList<String>();
	  final List<NMCommerceItem> persCommerceItems = persistentOrder.getCommerceItems();
	  final List<NMCommerceItem> currentCommerceItems = currentOrder.getCommerceItems();
	  final CommerceItemManager itemManager = orderManager.getCommerceItemManager();

	  // take all items from current
	  for (final NMCommerceItem dci : currentCommerceItems) {
		  final String skuId = dci.getCatalogRefId();        
		  ids.add(skuId);
	  }

	  // compare with persistent
	  try {
		  for (final NMCommerceItem ci : persCommerceItems) {
			  if (ci != null) {
				  final String skuId = ci.getCatalogRefId();
				  if (ids.contains(skuId)) {
					  itemsToRemove.add(ci.getId());
				  }
			  }
		  }
		  synchronized (persistentOrder) {
			  // remove from persistent
			  for (final String cId : itemsToRemove) {
				  itemManager.removeItemFromOrder(persistentOrder, cId);
			  }
			  orderManager.updateOrder(persistentOrder);
		  }
	  } catch (final Exception exception) {
		  CommonComponentHelper.getLogger().logError("Exception : " +exception.getMessage());
		  throw exception;
	  }
  }
  
  /** This method removes the promotional items from given persistent order
   * @param persistentOrder persistent order
   * @param orderManager order manager reference
   * @throws Exception any exception thrown
   */
  @SuppressWarnings("unchecked")
  public static void removePersCartItems(NMOrderImpl persistentOrder,OrderManager orderManager) throws Exception {
	  final List<NMCommerceItem> persCommerceItems = persistentOrder.getCommerceItems();
	  final CommerceItemManager itemManager = orderManager.getCommerceItemManager();
	  final List<String> ids = new ArrayList<String>();

	  for (final NMCommerceItem ci : persCommerceItems) {
		  if (ci.isGwpItem() || ci.isPwpItem()) {
			  ids.add(ci.getId());
		  }
		  else  {
			  final ProdSkuUtil prodSkuUtil = CommonComponentHelper.getProdSkuUtil();
			  final RepositoryItem sku = prodSkuUtil.getCmosSkuRepositoryItem(ci.getCmosSKUId());
			  final RepositoryItem product = prodSkuUtil.getProductRepositoryItem((String) ci.getPropertyValue("productId"));

			  if(!prodSkuUtil.isSellableSku(product, sku)){
				  ids.add(ci.getId());
			  }
		  }
	  }

	  synchronized (persistentOrder) {
		  // remove from persistent
		  for (final String cId : ids) {
			  itemManager.removeItemFromOrder(persistentOrder, cId);
		  }
		  orderManager.updateOrder(persistentOrder);
	  }
  }
  /**
   * The below code captures the recently changed commerce item data into RecentlyChangedCommerceItem Bean and adds it into the order. This method is used in several scenarios like remove item,move
   * item to SFL,move to cart from SFL,edit commerce Item.
   * 
   * @param order
   *          - NMOrder Impl
   * @param commerceItem
   *          - Current commerce Item
   * @param cartProductQuantity
   *          - Changed Quantity
   * @param cartQuantity
   *          - cart quantity
   * @param changeOrigin
   *          - origin for change e.g{cart or save for later}
   * @param action
   *          - e.g {add or remove}
   * @param sflStatus
   *          - save for later status {add or remove}
   */
  public static void setRecentlyChangedItem(final NMOrderImpl order, final NMCommerceItem commerceItem, final Long cartProductQuantity, final Long cartQuantity, final String changeOrigin, final String action, final String sflStatus) {
    final List<RecentlyChangedCommerceItem> changedItems = order.getRecentlyChangedCommerceItems();
    final String cmosSKU = commerceItem.getCmosSKUId();
      RecentlyChangedCommerceItem recentlyChangedItem = order.getRecentlyChangedCommerceItem(cmosSKU);
      boolean validationAction = true;
      if (recentlyChangedItem != null) {
        //Already a recently changed item found on order for given SKU. Operate on this.
        if (!StringUtils.isBlank(action) && INMGenericConstants.REMOVE.equals(action)
          && INMGenericConstants.ADD.equals(recentlyChangedItem.getCartStatus())) {
        //Previously item added. Now it is remove request. Hence item should not be part of recently changed list.
        changedItems.remove(recentlyChangedItem);
        //Skip updating the list. We are not adding or removing item.
        validationAction = false;
        }
        else if(!StringUtils.isBlank(action) && INMGenericConstants.ADD.equals(action)
                && INMGenericConstants.REMOVE.equals(recentlyChangedItem.getCartStatus())){
          //Previously removed added. Now it is add request. Hence item should not be part of recently changed list.
          changedItems.remove(recentlyChangedItem);
          //Skip updating the list. We are not adding or removing item.
          validationAction = false;
        }
      }    
      else {
        //No recently changed item found for given cmos sku. Create one now.
        recentlyChangedItem = new RecentlyChangedCommerceItem();
      }
        
      if (validationAction) {
    	  recentlyChangedItem.setCartChangeProductCmosCatalogId(commerceItem.getCmosCatalogId());
          recentlyChangedItem.setCartChangeProductCmosItem(commerceItem.getCmosItemCode());
          recentlyChangedItem.setCartChangeProductCmosSku(cmosSKU);
          recentlyChangedItem.setCartChangeProductId(commerceItem.getProductId());
          recentlyChangedItem.setCartChangeProductOrigin(changeOrigin);
          recentlyChangedItem.setCartChangeProductPrice(CommonComponentHelper.getPricingTools().round(commerceItem.getPriceInfo().getAmount() / commerceItem.getQuantity()));
          recentlyChangedItem.setCartChangeProductQuantity(cartProductQuantity);
          recentlyChangedItem.setCartProductQuantity(cartQuantity);
          recentlyChangedItem.setCartChangeShippingFrom(commerceItem.getShipFrom());
          recentlyChangedItem.setCartChangeShippingReceived(commerceItem.getShipReceived());
          recentlyChangedItem.setCartProductReplenishTime(commerceItem.getSelectedInterval());
          recentlyChangedItem.setCartSaveForLaterStatus(sflStatus);
          recentlyChangedItem.setCartStatus(action);
          recentlyChangedItem.setCommerceItemId(commerceItem.getId());
          if (null != commerceItem.getProduct()) {
            recentlyChangedItem.setProductName(commerceItem.getProduct().getDisplayName());
          }
          changedItems.add(recentlyChangedItem);
        }
        order.setRecentlyChangedCommerceItems(changedItems);
  }
  /**
   * @return shippingAddressMap
   */
  public static Map getShippingAddressMap() {
    return shippingAddressMap;
  }
  
  /**
   * @param shippingAddressMap
   */
  public static void setShippingAddressMap(Map shippingAddressMap) {
    CheckoutAPI.shippingAddressMap = shippingAddressMap;
  }
  /**
   * the below method only provides the place holder whether to have an ajax call when user changes the payment in the order review page for last call Percentoff PLCC promotion.
   */
  public static boolean verifyReviewPagePromoCodePLCC(NMOrderImpl order) {
	final List<NMCommerceItem> nmCItems = order.getCommerceItems();
	boolean plcc114Flag = false;
	for (final NMCommerceItem item : nmCItems) {
		Set<String> promoKeys = (Set<String>) ((RepositoryItem) item.getAuxiliaryData().getProductRef()).getPropertyValue("promoKeys");
		if (NmoUtils.isNotEmptyCollection(promoKeys)) {
			Iterator promoKeysIter = promoKeys.iterator();
			while (promoKeysIter.hasNext()) {
				String promoKey = (String) promoKeysIter.next();
				Repository percentoffRep = (Repository) Nucleus.getGlobalNucleus().resolveName("/nm/xml/CsrPercentOffRepository");
				try {
					RepositoryItem percentoffRepItem = percentoffRep.getItem(promoKey, "percentOffPromotions");
					if (percentoffRepItem != null) {
						String promoType = (String) percentoffRepItem.getPropertyValue("type");
						boolean plccFlag = NmoUtils.getBooleanPropertyValue(percentoffRepItem, "plccFlag");
						if ("114".equals(promoType) && plccFlag) {
							plcc114Flag = true;
						}
					}
				} catch (RepositoryException exception) {
					CommonComponentHelper.getLogger().logError("CheckoutAPI|VerifyReviewPagePromoCodePLCC|Error : " +exception.getMessage());
				}
			}
		}
	}

	return plcc114Flag;
  }
  /**
   * @param payPalOrderId
   * @return payPalAuthorizationRedirectUrl
   */
  public static String payPalAuthorizationRedirectUrl(final String payPalOrderId) {
	    return PayPalUtil.getInstance().getPayPal().getPayPalAuthorizationRedirectUrl(payPalOrderId);
  }
  
  public static List<String> updateProductCountryValidServiceLevels(ShoppingCartHandler cart, final List commerceItems, final String shipToCountry, final String currentServiceLevel) {
    return ServiceLevelUtil.getInstance().updateProductCountryValidServiceLevels(cart, commerceItems, shipToCountry, currentServiceLevel);
  }
  
  public static void setValidServiceLevelsWhenCmosFails(ServiceLevel[] serviceLevels) {
    ServiceLevelUtil.getInstance().setServiceLevelsValid(serviceLevels);
  }
  
  /**
   * Copy fields to order address.
   * 
   * @param fields
   *          the fields
   * @param orderAddress
   *          the order address
   * @return the mutable repository item
   */
  public static MutableRepositoryItem copyFieldsToOrderAddress(final ContactInfo fields, final MutableRepositoryItem orderAddress) {
    return AddressUtil.getInstance().copyFieldsToOrderAddress(fields, orderAddress);
  }
  
  /**
   * Express checkout payments. This Method invokes the paymentCheckoutValidation method of CartUtils which contains the common logic for Payment Validation for normal checkout & express checkout.
   * 
   * @param pageModel
   *          the page model
   * @param order
   *          the order
   * @param profile
   *          the profile
   * @param getRequest
   *          the get request
   * @throws Exception
   *           the exception
   */
  public static void paymentCheckoutValidation(final CheckoutPageModel pageModel, final NMOrderImpl order, final NMOrderManager orderMgr, final NMProfile profile,
          final DynamoHttpServletRequest request,
          ArrayList<String> tmsMessageList) {
    CartUtils.getInstance().paymentCheckoutValidation(pageModel, order, orderMgr, profile, request, tmsMessageList);
  }
  
  /**
   * Copy billing addr to shipping addr.
   * 
   * @param profile
   *          the profile
   * @param isInternational
   *          the is international
   * @throws Exception
   *           the exception
   */
  public static void copyBillingAddrToShippingAddr(final NMProfile profile, final boolean isInternational) throws Exception {
    ShippingUtil.getInstance().copyBillingAddrToShippingAddr(profile, isInternational);
  }
}
