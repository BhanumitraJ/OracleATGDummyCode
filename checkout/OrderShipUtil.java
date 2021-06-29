package com.nm.commerce.checkout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang.StringUtils;

import atg.commerce.order.HardgoodShippingGroup;
import atg.core.util.Address;
import atg.repository.RepositoryItem;

import com.nm.collections.CountryArray;
import com.nm.collections.ProvinceArray;
import com.nm.collections.ServiceLevel;
import com.nm.collections.ServiceLevelArray;
import com.nm.commerce.GiftCardHolder;
import com.nm.commerce.NMAddress;
import com.nm.commerce.NMCommerceItem;
import com.nm.commerce.NMProfile;
import com.nm.commerce.catalog.NMProduct;
import com.nm.commerce.catalog.NMSku;
import com.nm.commerce.promotion.NMOrderImpl;
import com.nm.common.INMGenericConstants;
import com.nm.components.CommonComponentHelper;
import com.nm.formhandler.ShoppingCartHandler;
import com.nm.profile.ProfileProperties;
import com.nm.sitemessages.Message;
import com.nm.sitemessages.MessageDefs;
import com.nm.utils.ProdCountryUtils;
import com.nm.utils.StringUtilities;
import com.nm.utils.SystemSpecs;

/**
 * Package access - outside of this package, methods should be accessed through CheckoutAPI.
 */
/* package */class OrderShipUtil {
  
  private static OrderShipUtil INSTANCE; // avoid static initialization
  private static final String US_COUNTRY_CODE = "US";
  private static final String CANADA_COUNTRY_CODE = "CA";
  private static final Set<String> DROPSHIP_MERCHANDISE_TYPES = new HashSet<String>(Arrays.asList(new String[] {"1" , "2" , "3" , "4" , "5"}));
  
  // private constructor enforces singleton behavior
  private OrderShipUtil() {}
  
  public static synchronized OrderShipUtil getInstance() {
    INSTANCE = (INSTANCE == null) ? new OrderShipUtil() : INSTANCE;
    return INSTANCE;
  }
  
  // US first
  private final Comparator<String> comp = new Comparator<String>() {
    @Override
    public int compare(final String o1, final String o2) {
      final String n1 = getCompareName(o1.toString());
      final String n2 = getCompareName(o2.toString());
      return n1.compareTo(n2);
    }
    
    private String getCompareName(final String name) {
      return US_COUNTRY_CODE.equals(name) ? "" : name;
    }
  };
  
  // returns the first error message about shipping an order to multiple countries.
  public String checkMultipleCountries(final List<String> countries) {
    final Set<String> groupCountries = new TreeSet<String>(comp);
    groupCountries.addAll(countries);
    return checkMultipleCountries(groupCountries);
  }
  
  private String checkMultipleCountries(final Set<String> groupCountries) {
    final Iterator<String> it = groupCountries.iterator();
    if (it.hasNext()) {
      final String firstCountry = it.next();
      if (it.hasNext()) {
        final String country = it.next();
        if (!firstCountry.equals(country)) {
          return createCountriesText(firstCountry, country);
        }
      }
    }
    return null;
  }
  
  private String createCountriesText(final String country1, final String country2) {
    final String name1 = getCountryAdjective(country1);
    final String name2 = getCountryAdjective(country2);
    
    String message = "We apologize we are unable to ship to both ";
    message += name1 + " and " + name2 + " addresses in a single order.";
    message += " You will need to separate your items into two separate orders.";
    return message;
  }
  
  private String getCountryAdjective(final String code) {
    if (US_COUNTRY_CODE.equals(code)) {
      return "U.S.";
    } else if (CANADA_COUNTRY_CODE.equals(code)) {
      return "Canadian";
    } else {
      return "Unknown";
    }
  }
  
  public String getShipToCanadaMessage() {
    final String sTCM =
            "Shipping to Canada? <a href='#' id='changeCountryLink'><b>You can click here</b></a> to change your shipping country and checkout with our international shipping partner, Borderfree.  <br> "
                    + "Or you can change your shipping address to a U.S. shipping address.";
    return sTCM;
  }
  
  public String getShipToInternationalMessage(final String countryName) {
    final String sTCM =
            "Shipping to " + countryName
                    + "? <a href='#' id='changeCountryLink'><b>You can click here</b></a> to change your shipping country and checkout with our international shipping partner, Borderfree.  <br> "
                    + "Or you can change your shipping address to a U.S. shipping address.";
    return sTCM;
  }
  
  private Message createMessage(final String text) {
    final Message msg = MessageDefs.getMessage(MessageDefs.MSG_GenericOrderShippingMessage);
    msg.setMsgText(text);
    return msg;
  }
  
  public Collection<Message> validateAddresses(final NMOrderImpl order, final NMProfile profile) {
    final List<Message> messages = new ArrayList<Message>();
    final List<HardgoodShippingGroup> shippingGroups = OrderUtil.getInstance().getHardgoodShippingGroups(order);
    final RepositoryItem billingAddress = profile.getBillingAddress();
    boolean missingAddress = false;
    
    for (final HardgoodShippingGroup shippingGroup : shippingGroups) {
      final Address address = shippingGroup.getShippingAddress();
      if ((address == null) || StringUtilities.isEmpty(address.getAddress1())) {
        missingAddress = true;
      }
    }
    // Added the condition for Express PayPal and MasterPass
    if (!order.isExpressPaypal() && !order.isMasterPassPayGroupOnOrder()) {
      if ((!missingAddress && (billingAddress == null)) || (billingAddress != null && StringUtilities.isEmpty((String) billingAddress.getPropertyValue(ProfileProperties.Contact_address1)))) {
        missingAddress = true;
      }
    }
    if (missingAddress) {
      final Message msg = new Message();
      msg.setFrgName("/page/checkoutb/shipping/missingAddresses.jsp");
      msg.setFieldId("groupTop");
      messages.add(msg);
    }
    
    return messages;
  }
  
  public Collection<Message> validateShipping(final ShoppingCartHandler cart, final NMOrderImpl order, final NMProfile profile) {
    return validateShipping(cart, order, profile, false);
  }
  
  public Collection<Message> validateShipping(final ShoppingCartHandler cart, final NMOrderImpl order, final NMProfile profile, final boolean international) {
    final List<Message> messages = new ArrayList<Message>();
    final Set<String> countries = new TreeSet<String>(comp);
    final SystemSpecs systemSpecs = CommonComponentHelper.getSystemSpecs();
    final ProdCountryUtils prodCountryUtils = CheckoutComponents.getProdCountryUtils();
    final CountryArray countryArray = CheckoutComponents.getCountryArray();
    final ProvinceArray provinceArray = CheckoutComponents.getProvinceArray();
    final ServiceLevelArray serviceLevelArray = CheckoutComponents.getServiceLevelArray();
    
    boolean shippingToInternational = true;
    
    for (final HardgoodShippingGroup shippingGroup : OrderUtil.getInstance().getHardgoodShippingGroups(order)) {
      final List<NMCommerceItem> items = ShippingGroupUtil.getInstance().getItems(shippingGroup);
      
      final Address address = shippingGroup.getShippingAddress();
      final String country = address.getCountry();
      String province = address.getState();
      if ((province == null) || province.isEmpty()) {
        final NMAddress nmAddress = profile.getShippingNMAddress();
        province = nmAddress.getProvince();
      }
      final Boolean poBoxObject = ShippingGroupUtil.getInstance().getAddress(shippingGroup).getFlgPOBox();
      final boolean poBox = (poBoxObject == null) ? false : poBoxObject.booleanValue();
      String countryName = "";
      
      if (!items.isEmpty()) {
        if (country != null) {
          countries.add(country);
        }
      }
      
      if (country != null) {
        countryName = countryArray.getCountry(country).getShortName();
      }
      boolean canadianShipToFiftyOneOrder = false;
      boolean canadianShipToDomesticOrder = false;
      boolean canadaShipTo = false;
      boolean domesticShipTo = false;
      if (!StringUtils.isBlank(country)) {
        if (country.equalsIgnoreCase("CA")) {
          canadaShipTo = true;
          canadianShipToFiftyOneOrder = canadaShipTo && systemSpecs.isFiftyOneEnabled();
          canadianShipToDomesticOrder = !canadianShipToFiftyOneOrder;
        } else if (country.equalsIgnoreCase("US")) {
          domesticShipTo = true;
        }
      }
      
      for (final NMCommerceItem item : items) {
        final NMProduct product = item.getProduct();
        final NMSku sku = item.getSku();
        
        if (domesticShipTo || (canadaShipTo && canadianShipToDomesticOrder)) {
          // Selected country is US or FiftyOne disabled brand with Canada address.
          
          if (!prodCountryUtils.isProductShippableToCountry(product.getDataSource(), country)) {
            messages.add(createMessage("The " + product.getDisplayName() + " cannot be shipped to " + countryName));
          } else if (!prodCountryUtils.isProductShippableToProvince(product.getDataSource(), country, province)) {
            final String provinceName = provinceArray.getProvince(country, province).getShortDesc();
            messages.add(createMessage("The " + product.getDisplayName() + " cannot be shipped to " + provinceName));
          } else if (ShippingUtil.getInstance().hasNonContinentalUSStates(item, province, messages, null)) {
            // ServiceLevel serviceLevel = serviceLevelArray.getServiceLevelMap().get(item.getServicelevel());
            // String slDescription = serviceLevel != null ? serviceLevel.getShortDesc() : "Unknown";
            // messages.add(createMessage("The " + product.getDisplayName() + " shipping to " + province + " cannot be shipped " + slDescription));
          } else if (!prodCountryUtils.isSkuShippableToCountry(sku.getDataSource(), country)) {
            messages.add(createMessage("This item cannot be shipped to " + countryName));
          }
        } else if (canadianShipToFiftyOneOrder && !international) {
          // Show error message for Canada ShipTo addresses when user selects from Address book..
          shippingToInternational = false;
        }
        
        if (poBox) {
          boolean cantShipPOBoxes = false;
          final String merchandiseType = product.getMerchandiseType();
          final String itemServiceLevel = item.getServicelevel();
          String errorMessageType = null;
          
          if (item.getPerishable()) {
            errorMessageType = "perishable item";
            cantShipPOBoxes = true;
          } else if ((merchandiseType != null) && merchandiseType.equalsIgnoreCase(GiftCardHolder.GIFT_CARD_MERCH_TYPE)) {
            errorMessageType = "gift card";
            cantShipPOBoxes = true;
          } else if ((merchandiseType != null) && DROPSHIP_MERCHANDISE_TYPES.contains(merchandiseType)) {
            cantShipPOBoxes = true;
          }
          
          if (cantShipPOBoxes) {
            String message = "The " + product.getDisplayName() + " cannot be shipped to a P.O. Box";
            if (errorMessageType != null) {
              message += " because it is a " + errorMessageType;
            }
            message += ". Please enter a new shipping destination.";
            messages.add(createMessage(message));
          } else if (ServiceLevel.SL2_SERVICE_LEVEL_TYPE.equals(itemServiceLevel) || ServiceLevel.SL1_SERVICE_LEVEL_TYPE.equals(itemServiceLevel)) {
            final ServiceLevel serviceLevel = serviceLevelArray.getServiceLevelMap().get(itemServiceLevel);
            final String slDescription = serviceLevel != null ? serviceLevel.getShortDesc() : "Unknown";
            messages.add(createMessage("The " + product.getDisplayName() + " cannot be shipped " + slDescription + " to a P.O. Box"));
          }
        }
      }
      if (!shippingToInternational && !"US".equals(country)) {
        messages.add(createMessage(getShipToInternationalMessage(countryName)));
      }
    }
    
    final Iterator<String> it = countries.iterator();
    if (it.hasNext()) {
      final String firstCountry = it.next();
      while (it.hasNext()) {
        final String country = it.next();
        if (!firstCountry.equals(country)) {
          messages.add(createMessage(createCountriesText(firstCountry, country)));
        }
      }
    }
    
    return messages;
  }
  
  public Collection<Message> updateShopRunnerPoBoxesToStandard(final NMOrderImpl order) {
    final List<Message> messages = new ArrayList<Message>();
    final boolean shopRunnerOrder = order.isShopRunnerOrder();
    
    final ShippingGroupUtil shippingGroupUtil = ShippingGroupUtil.getInstance();
    if (shopRunnerOrder) {
      for (final HardgoodShippingGroup shippingGroup : OrderUtil.getInstance().getHardgoodShippingGroups(order)) {
        final List<NMCommerceItem> items = shippingGroupUtil.getItems(shippingGroup);
        final String addressType = shippingGroupUtil.getAddress(shippingGroup).getAddressType();
        final Boolean isPOBox = shippingGroupUtil.getAddress(shippingGroup).getFlgPOBox();
        final String shipToCountry = shippingGroupUtil.getCountry(shippingGroup);
        boolean poBoxAddressType = false;
        if (((addressType != null) && addressType.equalsIgnoreCase("P")) || ((isPOBox != null) && isPOBox)) {
          poBoxAddressType = true;
        }
        
        for (final NMCommerceItem item : items) {
          final NMProduct product = item.getProduct();
          if (poBoxAddressType && product.getIsShopRunnerEligible() && item.getServicelevel().equalsIgnoreCase(ServiceLevel.SL2_SERVICE_LEVEL_TYPE)) {
            // SmartPost : setting the service level code based on the amount and ship to country
            String freeShippingShortDesc = INMGenericConstants.EMPTY_STRING;
            final ServiceLevelArray serviceLevelArray = CheckoutComponents.getServiceLevelArray();
            String serviceLevelCode = serviceLevelArray.determineFreeShippingServiceLevel();
            // SmartPost : getting the service level short description from the updated service level object for the selected service level code and the AB test group
            final ServiceLevel freeShippingServiceLevel = serviceLevelArray.getServiceLevelByCode(serviceLevelCode, serviceLevelArray.getServiceLevelGroupBasedOnAbTest());
            if (null != freeShippingServiceLevel) {
              freeShippingShortDesc = freeShippingServiceLevel.getShortDesc();
            }
            final Set<ServiceLevel> itemServiceLevels = ServiceLevelUtil.getInstance().getCommerceItemValidServiceLevels(item, shipToCountry);
            
            // get lowest service level code
            if (!itemServiceLevels.isEmpty()) {
              serviceLevelCode = itemServiceLevels.iterator().next().getCode();
            }
            
            item.setServicelevel(serviceLevelCode);
            // SmartPost : In the error message text which is set below, the short description of the service level is taken dynamically based on the free shipping service level
            final Message msg = MessageDefs.getMessage(MessageDefs.MSG_ShippingPoBoxServiceLevel);
            msg.setMsgText("Items shipped to a P.O. Box cannot be shipped using ShopRunner. Shipping Method set back to " + freeShippingShortDesc + ".");
            
            msg.setFieldId("groupServiceLevels");
            if (!messages.contains(msg) && !msg.getMsgText().isEmpty()) {
              messages.add(msg);
            }
          }
        }
      }
    }
    return messages;
  }
  
  /**
   * this method checks whether the order has any ShippingGroup without address
   * 
   * @param order
   * @return
   */
  public boolean hasEmptyShippingAddress(final NMOrderImpl order) {
    @SuppressWarnings("unchecked")
    final List<HardgoodShippingGroup> shippingGroups = order.getShippingGroups();
    boolean containsEmptyAddress = false;
    for (final HardgoodShippingGroup shippingGroup : shippingGroups) {
      final Address address = shippingGroup.getShippingAddress();
      if ((address == null) || StringUtilities.isEmpty(address.getAddress1())) {
        containsEmptyAddress = true;
        break;
      }
    }
    return containsEmptyAddress;
  }
  
}
