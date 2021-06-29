package com.nm.commerce.checkout;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import atg.commerce.order.HardgoodShippingGroup;
import atg.commerce.order.Order;
import atg.commerce.order.ShippingGroup;
import atg.repository.MutableRepositoryItem;
import atg.servlet.ServletUtil;
import atg.userprofiling.Profile;

import com.nm.commerce.NMProfile;
import com.nm.commerce.NMRepositoryContactInfo;
import com.nm.commerce.checkout.beans.ContactInfo;
import com.nm.commerce.promotion.NMOrderImpl;
import com.nm.formhandler.ShoppingCartHandler;
import com.nm.integration.ShippingDetailsVO;
import com.nm.integration.VMe;
import com.nm.integration.VMeInfoVO;
import com.nm.integration.VmeConstants;
import com.nm.profile.AddressVerificationContainer;
import com.nm.profile.AddressVerificationData;
import com.nm.profile.AddressVerificationHelper;
import com.nm.profile.ProfileProperties;
import com.nm.utils.AddressNickname;
import com.nm.utils.StringMap;
import com.nm.utils.StringUtilities;

/**
 * Package access - outside of this package, methods should be accessed through CheckoutAPI.
 */
/* package */class AddressVerificationUtil {
  
  private static AddressVerificationUtil INSTANCE; // avoid static initialization
  
  public static final String FIRST_NAME = "firstName";
  public static final String LAST_NAME = "lastName";
  public static final String ADDRESS1 = "address1";
  public static final String ADDRESS2 = "address2";
  public static final String CITY = "city";
  public static final String STATE = "state";
  public static final String COUNTRY = "country";
  public static final String POSTAL_CODE = "postalCode";
  public static final String PHONE_NUMBER = "phoneNumber";
  public static final String PHONETYPE = "phoneType";
  public static final String NICKNAME = "nickname";
  public static final String GEO_CODE_LATITUDE = "geoCodeLatitude";
  public static final String GEO_CODE_LONGITUDE = "geoCodeLongitude";
  public static final String GEO_CODE_TAX_KEY = "geoCodeTaxKey";
  public static final String GEO_CODE_REFRESH_FLAG = "geoCodeRefreshFlag";
  public static final String LAST_GEO_CODE_REQ_DATE = "lastGeoCodeReqDate";
  
  /** indicates only shipping addresses on an order should be verified */
  public static final int CHECK_SHIPPING_ADDRESSES = 0;
  /** indicates only billing addresses should be verified */
  public static final int CHECK_BILLING_ADDRESSES = 1;
  /** indicates all addresses on the profile or order should be verified */
  public static final int CHECK_ALL_ADDRESSES = 2;
  /** indicates only vme changed shipping addresses on an order should be verified */
  public static final int CHECK_SHIPPING_ADDRESSES_FOR_VME = 3;
  
  private AddressVerificationUtil() {
    
  }
  
  public static synchronized AddressVerificationUtil getInstance() {
    INSTANCE = (INSTANCE == null) ? new AddressVerificationUtil() : INSTANCE;
    return INSTANCE;
  }
  
  /**
   * Returns the path to the address verification fragment which will be displayed by the client should address verification be required.
   * 
   * @param checkType
   *          the type of address to check. This could be shipping addresses only, or include all addresses.
   * @param addressVerificationHelper
   *          AddressVerificationHelper
   * @param order
   *          the order on the session
   * @return the address verification url, if verification is required, null otherwise
   * @throws Exception
   */
  public AddressVerificationContainer getAddressVerificationContainer(NMProfile profile, AddressVerificationHelper addressVerificationHelper, int checkType, Order order) throws Exception {
    AddressVerificationContainer returnValue = null;
    
    switch (checkType) {
      case CHECK_SHIPPING_ADDRESSES: {
        Collection<ContactInfo> unverifiedShippingAddresses = getUnverifiedShippingGroupAddresses(order);
        
        if (unverifiedShippingAddresses.size() > 0) {
          returnValue = getAddressVerificationContainer(addressVerificationHelper, unverifiedShippingAddresses);
        }
      }
      break;
      case CHECK_ALL_ADDRESSES: {
        Collection<ContactInfo> unverifiedAddresses = getUnverifiedBillingAddress(profile);
        unverifiedAddresses.addAll(getUnverifiedShippingGroupAddresses(order));
        
        if (unverifiedAddresses.size() > 0) {
          returnValue = getAddressVerificationContainer(addressVerificationHelper, unverifiedAddresses);
        }
      }
      break;
      case CHECK_SHIPPING_ADDRESSES_FOR_VME: {
        Collection<ContactInfo> unverifiedShippingAddresses = getUnverifiedShippingGroupAddresses(order);
        Collection<ContactInfo> unverifiedShippingOtherThanVmeAddress = new ArrayList<ContactInfo>();
        VMe vMe = CheckoutComponents.getvMe();
        
        final VMeInfoVO vMeInfoVO = CheckoutComponents.getVMeInfoVO(ServletUtil.getCurrentRequest());
        ShippingDetailsVO shippingDetailsVo = vMeInfoVO.getShippingInfo();
        if (null != shippingDetailsVo) {
          String fullName = shippingDetailsVo.getName();
          List<String> firstAndLastNameList = CheckoutAPI.splitVmeFirstAndLastName(fullName);
          String vMeNickname =
                  AddressNickname.createNickname(firstAndLastNameList.get(0), firstAndLastNameList.get(1), shippingDetailsVo.getAddressLine1(), shippingDetailsVo.getCity(),
                          shippingDetailsVo.getStateProvinceCode(), shippingDetailsVo.getPostalCode(), shippingDetailsVo.getCountryCode());
          
          for (ContactInfo addr : unverifiedShippingAddresses) {
            String sgNickname = AddressNickname.createNickname(addr.getFirstName(), addr.getLastName(), addr.getAddress1(), addr.getCity(), addr.getState(), addr.getZip(), addr.getCountry());
            if (!sgNickname.equalsIgnoreCase(vMeNickname)) {
              unverifiedShippingOtherThanVmeAddress.add(addr);
            }
          }
          
          if (unverifiedShippingOtherThanVmeAddress.size() > 0) {
            returnValue = getAddressVerificationContainer(addressVerificationHelper, unverifiedShippingOtherThanVmeAddress);
          }
        }
      }
      break;
    }
    return returnValue;
    
  }
  
  /**
   * Returns the path to the address verification fragment which will be displayed by the client should address verification be required.
   * 
   * @param contactInfos
   *          the addresses
   * @param addressVerificationHelper
   *          AddressVerificationHelper
   * @return the address verification url, if verification is required, null otherwise
   * @throws Exception
   */
  public String getAddressVerificationURL(AddressVerificationHelper addressVerificationHelper, Collection<ContactInfo> contactInfos) throws Exception {
    String returnValue = null;
    
    String addrKey = "";
    if (!contactInfos.isEmpty()) {
      ContactInfo contactInfo = contactInfos.iterator().next();
      addrKey = contactInfo.getAddressVerificationKey();
    }
    
    AddressVerificationContainer addressVerificationContainer = getAddressVerificationContainer(addressVerificationHelper, contactInfos);
    
    if (addressVerificationContainer != null) {
      addrKey = addrKey != null ? "&addrKey=" + addrKey : "";
      returnValue = "/checkout/addressverification.jsp?verificationRequest=" + addressVerificationContainer.getKey() + addrKey;
    }
    
    return returnValue;
  }
  
  public AddressVerificationContainer getAddressVerificationContainer(AddressVerificationHelper addressVerificationHelper, Collection<ContactInfo> contactInfos) throws Exception {
    AddressVerificationContainer addressVerificationContainer = null;
    
    Iterator<ContactInfo> iterator = contactInfos.iterator();
    int counter = 0;
    
    while (iterator.hasNext()) {
      ContactInfo contactInfo = (ContactInfo) iterator.next();
      
      Dictionary<String, Object> dictionary = createDictionaryFromContactInfo(contactInfo);
      Map<String, Object> results = addressVerificationHelper.verifyAddress(dictionary);
      
      if (results != null) {
        if (addressVerificationContainer == null) {
          addressVerificationContainer = addressVerificationHelper.createContainer(AddressVerificationContainer.ACCOUNT_VERIFICATION);
        }
        
        if (contactInfo.getId() == null) {
          contactInfo.setId("" + counter);
          ++counter;
        }
        
        addressVerificationContainer.addAddress(contactInfo.getAddressType(), contactInfo.getId(), dictionary, results);
      } else {
        Integer integer = (Integer) dictionary.get("verificationFlag");
        int verificationFlag = (integer != null) ? integer.intValue() : AddressVerificationHelper.HAS_NOT_BEEN_VERIFIED;
        contactInfo.setVerificationFlag(verificationFlag);
        contactInfo.setFlgCountyCodeVer(Boolean.valueOf("true"));
      }
    }
    
    return addressVerificationContainer;
  }
  
  /**
   * Retrieves all shipping group addresses on an order that have not been through address verification. Gift List addresses will be excluded.
   * 
   * @param order
   *          the order containing the addresses to verify
   * @param service
   *          the service which initiated this request
   * @return Collection the collection of shipping address which require address verification. This object will be empty if no addresses need to be verified.
   */
  public Collection<ContactInfo> getUnverifiedShippingGroupAddresses(final Order order) {
    ArrayList<ContactInfo> returnValue = new ArrayList<ContactInfo>();
    
    @SuppressWarnings("unchecked")
    List<HardgoodShippingGroup> shippingGroups = order.getShippingGroups();
    
    Iterator<HardgoodShippingGroup> iterator = shippingGroups.iterator();
    
    while (iterator.hasNext()) {
      HardgoodShippingGroup shippingGroup = (HardgoodShippingGroup) iterator.next();
      NMRepositoryContactInfo repositoryContactInfo = (NMRepositoryContactInfo) shippingGroup.getShippingAddress();
      Boolean flgGiftReg = (Boolean) repositoryContactInfo.getFlgGiftReg();
      boolean isWishlistAddress = (flgGiftReg != null) ? flgGiftReg.booleanValue() : false;
      boolean isStoreAddress = StringUtilities.isNotEmpty((String) shippingGroup.getPropertyValue("shiptostorenumber"));
      
      if (!isWishlistAddress && !isStoreAddress && AddressVerificationHelper.shouldAddressBeVerified(repositoryContactInfo.getRepositoryItem())) {
        ContactInfo contactInfo = createContactInfoFromNMContactInfo(repositoryContactInfo);
        contactInfo.setAddressType(AddressVerificationData.SECONDARY_ADDRESS);
        contactInfo.setContactAddressName(shippingGroup.getDescription());
        returnValue.add(contactInfo);
      }
    }
    
    return returnValue;
  }
  
  /**
   * Retrieves all unverified billing addresses on the profile specified.
   * 
   * @param profile
   *          the profile containing the billing address
   * @return the collection of unverified billing addresses on the profile. In theory there should only be a single billing address in this collection. If no billing addresses require verfication the
   *         collection will be empty.
   */
  public Collection<ContactInfo> getUnverifiedBillingAddress(Profile profile) {
    ArrayList<ContactInfo> returnValue = new ArrayList<ContactInfo>();
    
    MutableRepositoryItem item = (MutableRepositoryItem) profile.getPropertyValue("billingAddress");
    
    if (AddressVerificationHelper.shouldAddressBeVerified(item)) {
      ContactInfo contactInfo = AddressUtil.getInstance().profileAddressToContactInfo(item);
      contactInfo.setAddressType(AddressVerificationData.BILLING_ADDRESS);
      contactInfo.setContactAddressName(ProfileProperties.Profile_billingAddressName);
      returnValue.add(contactInfo);
    }
    
    return returnValue;
  }
  
  public boolean doesOrderRequireAddressVerification(NMProfile profile, NMOrderImpl order) {
    boolean returnValue = false;
    
    MutableRepositoryItem item = (MutableRepositoryItem) profile.getPropertyValue("billingAddress");
    
    if (AddressVerificationHelper.shouldAddressBeVerified(item)) {
      returnValue = true;
    } else {
      @SuppressWarnings("unchecked")
      List<HardgoodShippingGroup> shippingGroups = order.getShippingGroups();
      
      for (HardgoodShippingGroup shippingGroup : shippingGroups) {
        NMRepositoryContactInfo repositoryContactInfo = (NMRepositoryContactInfo) shippingGroup.getShippingAddress();
        Boolean flgGiftReg = (Boolean) repositoryContactInfo.getFlgGiftReg();
        boolean isWishlistAddress = (flgGiftReg != null) ? flgGiftReg.booleanValue() : false;
        if (!isWishlistAddress && AddressVerificationHelper.shouldAddressBeVerified(repositoryContactInfo.getRepositoryItem())) {
          returnValue = true;
          break;
        }
      }
    }
    
    return returnValue;
  }
  
  /**
   * Returns the path to the address verification fragment which will be displayed by the client should address verification be required.
   * 
   * @param ContactInfo
   *          contactInfo
   * @param addressVerificationHelper
   *          AddressVerificationHelper
   * @return the String <city>,<state>
   * @throws Exception
   */
  public String getCityAndState(AddressVerificationHelper addressVerificationHelper, ContactInfo contactInfo) throws Exception {
    String returnValue = null;
    
    Dictionary<String, Object> dictionary = createDictionaryFromContactInfo(contactInfo);
    returnValue = addressVerificationHelper.retrieveCityState(dictionary);
    
    return returnValue;
  }
  
  /**
   * Creates a contact info object from the repository item passed to the method. You must copy the address properties from the NMRepositoryContactInfo instead of it's wrapped repositoryItem because
   * the wrapped repositoryItem does not use the normal properties names (i.e. it uses "stateAddress" instead of "state").
   * 
   * @param address
   *          the object the contact info will be populated from
   * @return a ContactInfo object populated with the data from the parameter
   */
  public static ContactInfo createContactInfoFromNMContactInfo(NMRepositoryContactInfo address) {
    ContactInfo contactInfo = new ContactInfo();
    
    contactInfo.setId(address.getRepositoryItem().getRepositoryId());
    contactInfo.setTitleCode(address.getTitleCode());
    contactInfo.setFirstName(address.getFirstName());
    contactInfo.setLastName(address.getLastName());
    contactInfo.setSuffixCode(address.getSuffixCode());
    contactInfo.setCompanyName(address.getCompanyName());
    contactInfo.setAddressLine1(address.getAddress1());
    contactInfo.setAddressLine2(address.getAddress2());
    contactInfo.setCity(address.getCity());
    contactInfo.setState(address.getState());
    contactInfo.setCountry(address.getCountry());
    contactInfo.setZip(address.getPostalCode());
    contactInfo.setDayTelephone(address.getPhoneNumber());
    contactInfo.setDayTelephoneExt(address.getDayPhoneExt());
    contactInfo.setEveningTelephone(address.getEvePhoneNumber());
    contactInfo.setPhoneType(address.getPhoneType());
    
    Boolean value = (Boolean) address.getFlgPOBox();
    
    if (value != null) {
      contactInfo.setPoBox(value.toString());
    } else {
      contactInfo.setPoBox("false");
    }
    
    contactInfo.setGeoCodeLatitude(address.getGeoCodeLatitude());
    contactInfo.setGeoCodeLongitude(address.getGeoCodeLongitude());
    contactInfo.setGeoCodeTaxKey(address.getGeoCodeTaxKey());
    contactInfo.setGeoCodeRefreshFlag(address.getGeoCodeRefreshFlag());
    contactInfo.setLastGeoCodeReqDate(address.getLastGeoCodeReqDate());
    
    return contactInfo;
  }
  
  /**
   * Creates a dictionary of the contact info data passed to the object, this is the method used to created the dictionary in single page checkout
   * 
   * @param contactInfo
   *          the object used to populate the dictionary
   * @return a dictionary object representation of the contactInfo object
   */
  public static Dictionary<String, Object> createDictionaryFromContactInfo(ContactInfo contactInfo) {
    Hashtable<String, Object> dictionary = new Hashtable<String, Object>();
    
    dictionary.put("id", StringUtilities.checkNull(contactInfo.getId()));
    dictionary.put(FIRST_NAME, StringUtilities.checkNull(contactInfo.getFirstName()));
    dictionary.put(LAST_NAME, StringUtilities.checkNull(contactInfo.getLastName()));
    dictionary.put(ADDRESS1, StringUtilities.checkNull(contactInfo.getAddressLine1()));
    dictionary.put(ADDRESS2, StringUtilities.checkNull(contactInfo.getAddressLine2()));
    dictionary.put(CITY, StringUtilities.checkNull(contactInfo.getCity()));
    dictionary.put(STATE, StringUtilities.checkNull(contactInfo.getState()));
    dictionary.put(COUNTRY, StringUtilities.checkNull(contactInfo.getCountry()));
    dictionary.put(POSTAL_CODE, StringUtilities.checkNull(contactInfo.getZip()));
    dictionary.put(PHONE_NUMBER, StringUtilities.checkNull(contactInfo.getDayTelephone()));
    dictionary.put(NICKNAME, StringUtilities.checkNull(contactInfo.getContactAddressName()));
    dictionary.put(PHONETYPE, StringUtilities.checkNull(contactInfo.getPhoneType()));
    dictionary.put(GEO_CODE_LATITUDE, StringUtilities.checkNull(contactInfo.getGeoCodeLatitude()));
    dictionary.put(GEO_CODE_LONGITUDE, StringUtilities.checkNull(contactInfo.getGeoCodeLongitude()));
    dictionary.put(GEO_CODE_TAX_KEY, StringUtilities.checkNull(contactInfo.getGeoCodeTaxKey()));
    dictionary.put(GEO_CODE_REFRESH_FLAG, StringUtilities.checkNull(contactInfo.isGeoCodeRefreshFlag()));
    dictionary.put(LAST_GEO_CODE_REQ_DATE, StringUtilities.checkNull(contactInfo.getLastGeoCodeReqDate()));
    return dictionary;
  }
  
  /**
   * Updates the addresses on a given profile and order with the corresponding verified version of the address in the collection provided by the collection of contact Info items.
   * 
   * @param shoppingCartHandler
   * @param profile
   *          the profile on which the addresses will be modified
   * @param order
   *          the order on which the addresses will be modified
   * @param contactInfos
   *          the collection of verified addresses
   * @throws Exception
   *           if the addresses could not be retrieved or were not successfully updated.
   */
  public void updateAddresses(ShoppingCartHandler shoppingCartHandler, NMProfile profile, Order order, Collection<ContactInfo> contactInfos) throws Exception {
    Iterator<ContactInfo> iterator = contactInfos.iterator();
    OrderUtil orderUtil = OrderUtil.getInstance();
    AddressUtil addressutil = AddressUtil.getInstance();
    ShippingUtil shippingUtil = ShippingUtil.getInstance();
    
    while (iterator.hasNext()) {
      ContactInfo contactInfo = (ContactInfo) iterator.next();
      String addressName = contactInfo.getContactAddressName();
      
      MutableRepositoryItem profileAddress = (MutableRepositoryItem) orderUtil.getAddressFromKey(shoppingCartHandler, profile, addressName);
      
      if (profileAddress != null) {
        addressutil.updateAddress(shoppingCartHandler, profile, addressName, contactInfo, true);
      }
      
      List<ShippingGroup> shippingGroups = shippingUtil.getShippingGroupsForAddress(order, addressName);
      
      if (shippingGroups != null && shippingGroups.size() > 0) {
        shippingUtil.updateShippingGroupAddress(shoppingCartHandler, profile, order, contactInfo.getId(), contactInfo, false);
      }
    }
  }
  
  /**
   * This method takes an address container, a list of user selections (recommended vs original inputs) and returns a collection of contactInfos based on those selections.
   * 
   * @param addressVerificationContainer
   * @param userSelections
   * @return
   */
  public List<ContactInfo> getVerifiedAddresses(AddressVerificationContainer addressVerificationContainer, StringMap userSelections) {
    ArrayList<ContactInfo> returnValue = new ArrayList<ContactInfo>();
    
    if (addressVerificationContainer != null) {
      @SuppressWarnings("unchecked")
      Set<String> addressIds = userSelections.keySet();
      
      Iterator<String> iterator = addressIds.iterator();
      while (iterator.hasNext()) {
        String addressId = (String) iterator.next();
        @SuppressWarnings("unchecked")
        ArrayList<String> selectedOptions = (ArrayList<String>) userSelections.get(addressId);
        String selectedOption = null;
        
        if (selectedOptions.size() > 0) {
          selectedOption = (String) selectedOptions.get(0);
        }
        
        ContactInfo contactInfo = new ContactInfo();
        AddressVerificationData verificationData = addressVerificationContainer.getAddress(addressId);
        
        contactInfo.setId(addressId);
        contactInfo.setTitleCode(verificationData.getTitle());
        contactInfo.setFirstName(verificationData.getFirstName());
        contactInfo.setLastName(verificationData.getLastName());
        contactInfo.setSuffixCode(verificationData.getSuffix());
        contactInfo.setDayTelephone(verificationData.getPhoneNumber());
        contactInfo.setDayTelephoneExt(verificationData.getDayPhoneExt());
        contactInfo.setEveningTelephone(verificationData.getEvePhoneNumber());
        contactInfo.setCompanyName(verificationData.getFirmName());
        contactInfo.setPoBox(verificationData.getFlgPoBox().toString());
        contactInfo.setContactAddressName(verificationData.getNickname());
        contactInfo.setAddressType(verificationData.getType());
        contactInfo.setVerAddressType(verificationData.getAddressType());
        contactInfo.setVerCountyCode(verificationData.getCountyCode());
        contactInfo.setFlgCountyCodeVer(Boolean.valueOf("true"));
        contactInfo.setPhoneType(verificationData.getPhoneType());
        contactInfo.setGeoCodeLatitude(verificationData.getGeoCodeLatitude());
        contactInfo.setGeoCodeLongitude(verificationData.getGeoCodeLongitude());
        contactInfo.setGeoCodeTaxKey(verificationData.getGeoCodeTaxKey());
        contactInfo.setGeoCodeRefreshFlag(verificationData.getGeoCodeRefreshFlag());
        contactInfo.setLastGeoCodeReqDate(verificationData.getLastGeoCodeReqDate());
        
        if ("RECOMMENDED".equals(selectedOption)) {
          contactInfo.setAddressLine1(verificationData.getVerifiedAddress1());
          contactInfo.setAddressLine2(verificationData.getVerifiedAddress2());
          contactInfo.setCity(verificationData.getVerifiedCity());
          contactInfo.setState(verificationData.getVerifiedState());
          contactInfo.setZip(verificationData.getVerifiedPostalCode());
          contactInfo.setCountry(verificationData.getVerifiedCountry());
          contactInfo.setVerificationFlag(AddressVerificationHelper.VERIFIED_ADDRESS);
        } else {
          contactInfo.setAddressLine1(verificationData.getAddress1());
          contactInfo.setAddressLine2(verificationData.getAddress2());
          contactInfo.setCity(verificationData.getCity());
          contactInfo.setState(verificationData.getState());
          contactInfo.setZip(verificationData.getPostalCode());
          contactInfo.setCountry(verificationData.getCountry());
          contactInfo.setVerificationFlag(AddressVerificationHelper.USER_CONFIRMED_ADDRESS);
        }
        
        returnValue.add(contactInfo);
      }
    }
    
    return returnValue;
  }
  
}
