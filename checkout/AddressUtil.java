package com.nm.commerce.checkout;

import static com.nm.common.INMGenericConstants.EMPTY_STRING;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

import atg.commerce.CommerceException;
import atg.commerce.gifts.GiftlistManager;
import atg.commerce.order.HardgoodShippingGroup;
import atg.commerce.order.Order;
import atg.commerce.order.OrderManager;
import atg.commerce.order.OrderTools;
import atg.commerce.order.RepositoryContactInfo;
import atg.commerce.order.ShippingGroup;
import atg.commerce.order.ShippingGroupCommerceItemRelationship;
import atg.commerce.profile.CommercePropertyManager;
import atg.core.util.Address;
import atg.nucleus.Nucleus;
import atg.repository.DuplicateIdException;
import atg.repository.MutableRepository;
import atg.repository.MutableRepositoryItem;
import atg.repository.Repository;
import atg.repository.RepositoryException;
import atg.repository.RepositoryItem;

import com.nm.commerce.NMAddress;
import com.nm.commerce.NMCommerceItem;
import com.nm.commerce.NMProfile;
import com.nm.commerce.NMRepositoryContactInfo;
import com.nm.commerce.checkout.beans.ContactInfo;
import com.nm.commerce.promotion.NMOrderImpl;
import com.nm.formhandler.NMProfileFormHandler;
import com.nm.formhandler.ShoppingCartHandler;
import com.nm.profile.AddressVerificationHelper;
import com.nm.profile.ProfileProperties;
import com.nm.repository.stores.Store;
import com.nm.sitemessages.AddressMessageFields;
import com.nm.sitemessages.Message;
import com.nm.sitemessages.MessageDefs;
import com.nm.utils.AddressNickname;
import com.nm.utils.NmoUtils;
import com.nm.utils.RegexpUtils;
import com.nm.utils.StringUtilities;
import com.nm.utils.SystemSpecs;

/**
 * Package access - outside of this package, methods should be accessed through CheckoutAPI.
 */
/* package */class AddressUtil {
  
  private static AddressUtil INSTANCE; // avoid static initialization
  public static final String BILLING_ADDRESS = "billingaddress";
  public static final String SHIPPING_ADDRESS = "shippingaddress";
  public static final String DELIMITER_COLON = ";";
  public static final String AMPERSAND_HASH_SYMBOL = "&#";
  public static final String MY_SHIPPING_ADDRESS = "My Shipping Address";
  public static final int MINIMUM_MANDARIN_CHARACTER_RANGE = 11905;
  public static final int MAXIMUM_MANDARIN_CHARACTER_RANGE = 40900;
  
  public static final Pattern MANDARIN_CHARACTER_PATTERN = Pattern.compile("&#[\\d+]");
  public static final Pattern ALLOWED_PINYIN_CHARACTER = Pattern.compile("^19([2-7])$+|^20([0-7])$+|^21([0-4])$+|^21([7-9])$+|^22([0-1])$+|^22([4-9])$+|^23([2-9])$+|^24([1-6])$+|^25([0-3])$");
  public static final Pattern VALID_LATIN_CHARACTER = Pattern.compile("\\(+|\\)+|\\*+|\\|+|\\-+|\\++|[^\\p{L}+(\\-\\p{L}+)*\\d]+|[a-zA-z0-9]+");
  
  // private constructor enforces singleton behavior
  private AddressUtil() {}
  
  public static synchronized AddressUtil getInstance() {
    INSTANCE = (INSTANCE == null) ? new AddressUtil() : INSTANCE;
    return INSTANCE;
  }
  
  public String createNickName(final RepositoryItem contactInfo) {
    return NMAddress.createNickName(contactInfo);
  }
  
  public String createNickName(final ContactInfo addr) {
    return AddressNickname.createNickname(addr.getFirstName(), addr.getLastName(), addr.getAddressLine1(), addr.getCity(), addr.getState(), addr.getZip(), addr.getCountry());
  }
  
  public List<Message> validateAddress(final ContactInfo addr, final NMProfileFormHandler profileHandler, final String addressFieldSetKey, final String addressType) {
    return validateAddress(addr, profileHandler, addressFieldSetKey, addressType, false);
  }
  
  public List<Message> validateAddress(final ContactInfo addr, final NMProfileFormHandler profileHandler, final String addressFieldSetKey, final String addressType, final boolean isInternational) {
    final List<Message> messages = new ArrayList<Message>();
    messages.addAll(validateRequiredData(addr, addressFieldSetKey, addressType, isInternational));
    
    // validating for Latin or Pinyin characters if country preference is China
    messages.addAll(validateLatinOrPinyinCharacters(addr, addressFieldSetKey, addressType, isInternational));
    
    if (messages.isEmpty()) {
      messages.addAll(validateTelephone(addr, profileHandler, addressFieldSetKey, isInternational));
      messages.addAll(validatePostalCode(addr, profileHandler, addressFieldSetKey, isInternational));
    }
    if (messages.isEmpty() && (addressType.equals(BILLING_ADDRESS) && !addr.isSkipEmailValidation())) {
      messages.addAll(validateEmail(addr.getEmailAddress(), addressFieldSetKey, isInternational));
    }
    // handled error to be displayed related to PO box
    messages.addAll(validatePoBox(addr, addressFieldSetKey, addressType, profileHandler));
    return messages;
  }
  
  /**
   * handled error to be displayed related to PO box.Needed to created new method as profileFormHanlder was needed to be used.
   * 
   * @param addr
   * @param addressFieldSetKey
   * @param addressType
   * @param profileHandler
   * @return
   */
  private List<Message> validatePoBox(final ContactInfo addr, final String addressFieldSetKey, final String addressType, final NMProfileFormHandler profileHandler) {
    final List<Message> messages = new ArrayList<Message>();
    if (addressType.equals(SHIPPING_ADDRESS) && StringUtilities.areEqual("true", addr.getPoBox())) {
      final Message msg = MessageDefs.getMessage(MessageDefs.MSG_ShippingPoBoxServiceLevel);
      msg.setFieldId(AddressMessageFields.getFieldId(addressFieldSetKey, AddressMessageFields.FIELD_POBOX));
      messages.add(msg);
    }
    
    if (addressType.equals(BILLING_ADDRESS) && StringUtilities.areEqual("true", addr.getPoBox()) && profileHandler.isBillingSameAsShipping()) {
      final Message msg = MessageDefs.getMessage(MessageDefs.MSG_BillingPoBoxServiceLevel);
      msg.setFieldId(AddressMessageFields.getFieldId(addressFieldSetKey, AddressMessageFields.FIELD_POBOX));
      messages.add(msg);
    }
    return messages;
  }
  
  public List<Message> validateRequiredData(final ContactInfo addr, final String addressFieldSetKey, final String addressType, final boolean isInternational) {
    final List<Message> messages = new ArrayList<Message>();
    final SystemSpecs systemSpecs = (SystemSpecs) Nucleus.getGlobalNucleus().resolveName("/nm/utils/SystemSpecs");
    final boolean isFiftyOneEnabled = systemSpecs.isFiftyOneEnabled();
    if (NmoUtils.isEmpty(addr.getFirstName())) {
      final Message msg = MessageDefs.getMessage(MessageDefs.MSG_Address_MissingFirstName);
      msg.setFieldId(AddressMessageFields.getFieldId(addressFieldSetKey, AddressMessageFields.FIELD_FIRST_NAME));
      messages.add(msg);
    }
    if (NmoUtils.isEmpty(addr.getLastName())) {
      final Message msg = MessageDefs.getMessage(MessageDefs.MSG_Address_MissingLastName);
      msg.setFieldId(AddressMessageFields.getFieldId(addressFieldSetKey, AddressMessageFields.FIELD_LAST_NAME));
      messages.add(msg);
    }
    if (NmoUtils.isEmpty(addr.getCountry())) {
      final Message msg = MessageDefs.getMessage(MessageDefs.MSG_Address_MissingCountry);
      msg.setFieldId(AddressMessageFields.getFieldId(addressFieldSetKey, AddressMessageFields.FIELD_COUNTRY));
      messages.add(msg);
    }
    if (NmoUtils.isEmpty(addr.getAddress1())) {
      final Message msg = MessageDefs.getMessage(MessageDefs.MSG_Address_MissingAddressLine1);
      msg.setFieldId(AddressMessageFields.getFieldId(addressFieldSetKey, AddressMessageFields.FIELD_ADDRESS1));
      messages.add(msg);
    }
    if (NmoUtils.isEmpty(addr.getCity())) {
      final Message msg = MessageDefs.getMessage(MessageDefs.MSG_Address_MissingCity);
      msg.setFieldId(AddressMessageFields.getFieldId(addressFieldSetKey, AddressMessageFields.FIELD_CITY));
      messages.add(msg);
    }
    boolean foundStateVal = true;
    if (NmoUtils.isEmpty(addr.getState()) && addr.getCountry().equals("US")) {
      final Message msg = MessageDefs.getMessage(MessageDefs.MSG_Address_MissingStateProvince);
      msg.setFieldId(AddressMessageFields.getFieldId(addressFieldSetKey, AddressMessageFields.FIELD_STATE));
      messages.add(msg);
      foundStateVal = false;
    }
    
    if (NmoUtils.isEmpty(addr.getProvince()) && ((!addr.getCountry().equals("US") && isFiftyOneEnabled) || (!addr.getCountry().equals("CA") && !isFiftyOneEnabled && !foundStateVal))) {
      final Message msg = MessageDefs.getMessage(MessageDefs.MSG_Address_MissingStateProvince);
      msg.setFieldId(AddressMessageFields.getFieldId(addressFieldSetKey, AddressMessageFields.FIELD_PROVINCE));
      messages.add(msg);
    } else if (NmoUtils.isEmpty(addr.getProvince()) && NmoUtils.isEmpty(addr.getState())) {
      // Either State of Province should be present.
      final Message msg = MessageDefs.getMessage(MessageDefs.MSG_Address_MissingStateProvince);
      msg.setFieldId(AddressMessageFields.getFieldId(addressFieldSetKey, AddressMessageFields.FIELD_PROVINCE));
      messages.add(msg);
    }
    /*
     * if (NmoUtils.isEmpty(addr.getState()) && addr.getCountry().equals("US")) { Message msg = MessageDefs.getMessage(MessageDefs.MSG_Address_MissingStateProvince);
     * msg.setFieldId(AddressMessageFields.getFieldId(addressFieldSetKey,AddressMessageFields.FIELD_STATE)); messages.add(msg); } if (!addr.getCountry().equals("US") &&
     * NmoUtils.isEmpty(addr.getProvince())) { Message msg = MessageDefs.getMessage(MessageDefs.MSG_Address_MissingProvince);
     * msg.setFieldId(AddressMessageFields.getFieldId(addressFieldSetKey,AddressMessageFields.FIELD_PROVINCE)); messages.add(msg); }
     */
    if (!addr.isZipcodeOptional() && NmoUtils.isEmpty(addr.getPostalCode())) {
      Message msg = null;
      msg = MessageDefs.getMessage(MessageDefs.MSG_Address_MissingZipPostalCode);
      msg.setFieldId(AddressMessageFields.getFieldId(addressFieldSetKey, AddressMessageFields.FIELD_ZIP));
      messages.add(msg);
    }
    if (NmoUtils.isEmpty(addr.getPhoneNumber())) {
      final Message msg = MessageDefs.getMessage(MessageDefs.MSG_Address_MissingDayTelephone);
      msg.setFieldId(AddressMessageFields.getFieldId(addressFieldSetKey, AddressMessageFields.FIELD_DAY_TELEPHONE));
      messages.add(msg);
    }
    if ((addressType.equals(BILLING_ADDRESS)) && !addr.isSkipEmailValidation() && (NmoUtils.isEmpty(addr.getEmailAddress()))) {
      final Message msg = MessageDefs.getMessage(MessageDefs.MSG_Address_MissingEmail);
      msg.setFieldId(AddressMessageFields.getFieldId(addressFieldSetKey, AddressMessageFields.FIELD_EMAIL));
      messages.add(msg);
    }
    // State field is only mandatory for Borderfree addresses. Hence adding international check.
    /*
     * if (NmoUtils.isEmpty(addr.getState()) && addr.getCountry().equals("US")) { Message msg = MessageDefs.getMessage(MessageDefs.MSG_Address_MissingStateProvince);
     * msg.setFieldId(AddressMessageFields.getFieldId(addressFieldSetKey, AddressMessageFields.FIELD_PROVINCE)); messages.add(msg); }
     */
    
    return messages;
  }
  
  public List<Message> validateInvalidCharacters(final ContactInfo addr, final String addressFieldSetKey, final String addressType, final boolean isInternational) {
    final List<Message> messages = new ArrayList<Message>();
    
    String invalidChars = NmoUtils.invalidCharacters(addr.getFirstName());
    if (!NmoUtils.isEmpty(invalidChars)) {
      final Message msg = MessageDefs.getMessage(MessageDefs.MSG_InvalidCharacters);
      msg.setFieldId(AddressMessageFields.getFieldId(addressFieldSetKey, AddressMessageFields.FIELD_FIRST_NAME));
      // locale check for setting error msg text
      if (!isInternational) {
        msg.setMsgText(getInvalidCharacterError(invalidChars, "First Name"));
      } else {
        msg.setMsgIntlText(getInvalidCharacterError(invalidChars, "First Name/Given Name"));
      }
      messages.add(msg);
    }
    invalidChars = NmoUtils.invalidCharacters(addr.getLastName());
    if (!NmoUtils.isEmpty(invalidChars)) {
      final Message msg = MessageDefs.getMessage(MessageDefs.MSG_InvalidCharacters);
      msg.setFieldId(AddressMessageFields.getFieldId(addressFieldSetKey, AddressMessageFields.FIELD_LAST_NAME));
      // locale check for setting error msg text
      if (!isInternational) {
        msg.setMsgText(getInvalidCharacterError(invalidChars, "Last Name"));
      } else {
        msg.setMsgIntlText(getInvalidCharacterError(invalidChars, "Last Name/Family Name "));
      }
      messages.add(msg);
    }
    invalidChars = NmoUtils.invalidCharacters(addr.getCompanyName());
    if (!NmoUtils.isEmpty(invalidChars)) {
      final Message msg = MessageDefs.getMessage(MessageDefs.MSG_InvalidCharacters);
      msg.setFieldId(AddressMessageFields.getFieldId(addressFieldSetKey, AddressMessageFields.FIELD_COMPANY));
      msg.setMsgText(getInvalidCharacterError(invalidChars, "Company Name"));
      messages.add(msg);
    }
    invalidChars = NmoUtils.invalidCharacters(addr.getAddress1());
    if (!NmoUtils.isEmpty(invalidChars)) {
      final Message msg = MessageDefs.getMessage(MessageDefs.MSG_InvalidCharacters);
      msg.setFieldId(AddressMessageFields.getFieldId(addressFieldSetKey, AddressMessageFields.FIELD_ADDRESS1));
      // locale check for setting error msg text
      if (!isInternational) {
        msg.setMsgText(getInvalidCharacterError(invalidChars, "Address Line 1"));
      } else {
        msg.setMsgIntlText(getInvalidCharacterError(invalidChars, "Address 1"));
      }
      messages.add(msg);
    }
    invalidChars = NmoUtils.invalidCharacters(addr.getAddress2());
    if (!NmoUtils.isEmpty(invalidChars)) {
      final Message msg = MessageDefs.getMessage(MessageDefs.MSG_InvalidCharacters);
      msg.setFieldId(AddressMessageFields.getFieldId(addressFieldSetKey, AddressMessageFields.FIELD_ADDRESS2));
      // locale check for setting error msg text
      if (!isInternational) {
        msg.setMsgText(getInvalidCharacterError(invalidChars, "Address Line 2"));
      } else {
        msg.setMsgIntlText(getInvalidCharacterError(invalidChars, "Address 2"));
      }
      messages.add(msg);
    }
    invalidChars = NmoUtils.invalidCharacters(addr.getCity());
    if (!NmoUtils.isEmpty(invalidChars)) {
      final Message msg = MessageDefs.getMessage(MessageDefs.MSG_InvalidCharacters);
      msg.setFieldId(AddressMessageFields.getFieldId(addressFieldSetKey, AddressMessageFields.FIELD_CITY));
      // locale check for setting error msg text
      if (!isInternational) {
        msg.setMsgText(getInvalidCharacterError(invalidChars, "City"));
      } else {
        msg.setMsgIntlText(getInvalidCharacterError(invalidChars, "City"));
      }
      
      messages.add(msg);
    }
    invalidChars = NmoUtils.invalidCharacters(addr.getPostalCode());
    if (!NmoUtils.isEmpty(invalidChars)) {
      final Message msg = MessageDefs.getMessage(MessageDefs.MSG_InvalidCharacters);
      msg.setFieldId(AddressMessageFields.getFieldId(addressFieldSetKey, AddressMessageFields.FIELD_ZIP));
      // locale check for setting error msg text
      if (!isInternational) {
        msg.setMsgText(getInvalidCharacterError(invalidChars, "Zip/Postal Code"));
      } else {
        msg.setMsgIntlText(getInvalidCharacterError(invalidChars, "Postal Code/Zip Code"));
      }
      messages.add(msg);
    }
    invalidChars = NmoUtils.invalidCharacters(addr.getPhoneNumber());
    if (!NmoUtils.isEmpty(invalidChars)) {
      final Message msg = MessageDefs.getMessage(MessageDefs.MSG_InvalidCharacters);
      msg.setFieldId(AddressMessageFields.getFieldId(addressFieldSetKey, AddressMessageFields.FIELD_DAY_TELEPHONE));
      if (!isInternational) {
        msg.setMsgText(getInvalidCharacterError(invalidChars, "Telephone"));
      } else {
        msg.setMsgIntlText(getInvalidCharacterError(invalidChars, "Telephone"));
      }
      
      messages.add(msg);
    }
    invalidChars = NmoUtils.invalidCharacters(addr.getState());
    if (!NmoUtils.isEmpty(invalidChars)) {
      final Message msg = MessageDefs.getMessage(MessageDefs.MSG_InvalidCharacters);
      msg.setFieldId(AddressMessageFields.getFieldId(addressFieldSetKey, AddressMessageFields.FIELD_STATE));
      // locale check for setting error msg text
      if (!isInternational) {
        msg.setMsgText(getInvalidCharacterError(invalidChars, "State"));
      } else {
        msg.setFieldId(AddressMessageFields.getFieldId(addressFieldSetKey, AddressMessageFields.FIELD_PROVINCE));
        msg.setMsgIntlText(getInvalidCharacterError(invalidChars, "Province/State"));
      }
      messages.add(msg);
    }
    if (addressType.equals(BILLING_ADDRESS)) {
      invalidChars = NmoUtils.invalidCharacters(addr.getEmailAddress());
      if (!NmoUtils.isEmpty(invalidChars)) {
        final Message msg = MessageDefs.getMessage(MessageDefs.MSG_InvalidCharacters);
        msg.setFieldId(AddressMessageFields.getFieldId(addressFieldSetKey, AddressMessageFields.FIELD_EMAIL));
        msg.setMsgText(getInvalidCharacterError(invalidChars, "Email"));
        messages.add(msg);
      }
    }
    return messages;
  }
  
  /**
   * validateTelephone will call the NMProfileFormHandler's validatePostalCode method and create the necessary error messages based on the results.
   * 
   * @param addr
   *          - the contact info address to validate
   * @param addressFieldSetKey
   *          - the configured field set for error message creation
   * @return the list of error messages triggered during validation or an empty List if no errors were found.
   * @see com.nm.formhandler.NMProfileFormHandler
   */
  public List<Message> validateTelephone(final ContactInfo addr, final NMProfileFormHandler profileHandler, final String addressFieldSetKey, final boolean isInternational) {
    final List<Message> messages = new ArrayList<Message>();
    final String errorMsgText = profileHandler.validatePhone(addr.getDayTelephone(), addr.getCountry());
    if (!NmoUtils.isEmpty(errorMsgText)) {
      final Message msg = MessageDefs.getMessage(MessageDefs.MSG_InvalidPhone);
      msg.setFieldId(AddressMessageFields.getFieldId(addressFieldSetKey, AddressMessageFields.FIELD_DAY_TELEPHONE));
      // locale check for setting error msg text
      if (!isInternational) {
        msg.setMsgText("The " + errorMsgText);
      } else {
        msg.setMsgIntlText("The " + errorMsgText);
      }
      messages.add(msg);
    } else {
      addr.setDayTelephone(profileHandler.formatPhone(addr.getDayTelephone(), addr.getCountry()));
    }
    return messages;
  }
  
  /**
   * validate Postal Code will call the NMProfileFormHandler's validatePostalCode method and create the necessary error messages based on the results.
   * 
   * @param addr
   *          - the contact info address to validate
   * @param service
   *          - the service which is calling the validation
   * @param addressFieldSetKey
   *          - the configured field set for error message creation
   * @return the list of error messages triggered during validation or an empty List if no errors were found.
   * @see com.nm.formhandler.NMProfileFormHandler
   */
  public List<Message> validatePostalCode(final ContactInfo addr, final NMProfileFormHandler profileHandler, final String addressFieldSetKey, final boolean isInternational) {
    final List<Message> messages = new ArrayList<Message>();
    final String errorMsgText = profileHandler.validatePostalCode(addr.getZip(), addr.getCountry(), addr.getState(), null);
    if (!NmoUtils.isEmpty(errorMsgText)) {
      final Message msg = MessageDefs.getMessage(MessageDefs.MSG_InvalidPostalCode);
      msg.setFieldId(AddressMessageFields.getFieldId(addressFieldSetKey, AddressMessageFields.FIELD_ZIP));
      // locale check for setting error msg text
      if (!isInternational) {
        msg.setMsgText(errorMsgText);
      } else {
        msg.setMsgIntlText(errorMsgText);
      }
      messages.add(msg);
    }
    return messages;
  }
  
  /**
   * Validates the Email Address to ensure that the email does not contain invalid characters, spaces or commas, and is in a valid email address format.
   * 
   * @param email
   *          The Email Address for a user
   * @param messages
   *          - the List of messages to which errors identified here should be added.
   * @return String If this is populated then the validation found no error.
   **/
  public List<Message> validateEmail(final String email, final String addressFieldSetKey, final boolean isInternational) {
    final List<Message> messages = new ArrayList<Message>();
    boolean hasErrors = false;
    String invalidChars = "";
    if (email != null) {
      invalidChars = NmoUtils.invalidCharacters(email);
      hasErrors = !NmoUtils.isEmpty(invalidChars) || NmoUtils.checkForSpaces(email) || (email.indexOf(",") > -1);
    }
    
    if (hasErrors) {
      final Message msg = MessageDefs.getMessage(MessageDefs.MSG_InvalidEmailCharacters);
      final String msgText = "Your e-mail address contains invalid characters.";
      msg.setFieldId(AddressMessageFields.getFieldId(addressFieldSetKey, AddressMessageFields.FIELD_EMAIL));
      // locale check for setting error msg text
      if (!isInternational) {
        msg.setMsgText(msgText);
      } else {
        msg.setMsgIntlText(msgText);
      }
      messages.add(msg);
    }
    
    if (!hasErrors && !RegexpUtils.validateEmailAddress(email)) {
      hasErrors = true;
      final Message msg = MessageDefs.getMessage(MessageDefs.MSG_InvalidEmailAddress);
      msg.setFieldId(AddressMessageFields.getFieldId(addressFieldSetKey, AddressMessageFields.FIELD_EMAIL));
      // locale check for setting error msg text
      if (!isInternational) {
        msg.setMsgText("Your e-mail address is not valid.");
      } else {
        msg.setMsgIntlText("Sorry, this is not a valid email address. Please re-enter.");
      }
      messages.add(msg);
    }
    
    return messages;
  }
  
  /**
   * Updates the address specified by the address nickname for this profile with the data contained in the contact info object. This method will update the address for shipping, billing, and secondary
   * shipping addresses accordingly.
   * 
   * @param profile
   *          the profile containing the address to update
   * @param addrNickName
   *          the nickname of the address which will be updated
   * @param addr
   *          the address data for the changed address
   * @throws RepositoryException
   *           if the update fails
   */
  public String updateAddress(final ShoppingCartHandler cart, final NMProfile profile, final String addrNickName, final ContactInfo addr, final boolean swap) throws Exception {
    try {
      final CommercePropertyManager pm = cart.getPropertyManager();
      final String defaultShippingAddrName = pm.getDefaultShippingAddrName(java.util.Locale.US);
      final String defaultBillingAddrName = pm.getDefaultBillingAddrName(java.util.Locale.US);
      
      // final String defaultShippingAddrName = "";
      // final String defaultBillingAddrName = "new";
      
      // final ShippingUtils shipUtils = getShippingUtils();
      MutableRepositoryItem profAddr = null;
      String newNickName = addrNickName;
      if (addrNickName.equals(defaultShippingAddrName)) {
        // MasterPass changes : As MasterPass Shipping address not to be stored in NM account,skip the below step for MasterPass Orders
        if (!((NMOrderImpl) cart.getOrder()).isMasterPassPayGroupOnOrder()) {
          profAddr = (MutableRepositoryItem) profile.getPropertyValue(ProfileProperties.Profile_shippingAddress);
          copyFieldsToProfileAddress(addr, profAddr);
          ((MutableRepository) profile.getRepository()).updateItem(profAddr);
        }
        if (addr.getUpdateWishlistAddressFlag()) {
          // updateWishListAddress(service, profile);
        }
      } else if (addrNickName.equals(defaultBillingAddrName)) {
        profAddr = (MutableRepositoryItem) profile.getPropertyValue(ProfileProperties.Profile_billingAddress);
        copyFieldsToProfileAddress(addr, profAddr);
        ((MutableRepository) profile.getRepository()).updateItem(profAddr);
      } else {
        @SuppressWarnings("unchecked")
        final Map<String, MutableRepositoryItem> secAddrs = (Map<String, MutableRepositoryItem>) profile.getPropertyValue(ProfileProperties.Profile_secondaryAddresses);
        profAddr = secAddrs.get(addrNickName);
        if (null == profAddr) {
          // check to see if this is a ship to store address
          final Store store = CheckoutComponents.getShipToStoreHelper().getStoreAddressByStoreNumber(addrNickName);
          if ((store == null) && !((NMOrderImpl) cart.getOrder()).isMasterPassPayGroupOnOrder()) {
            newNickName = addNewAddress(profile, addr);
          }
        } else if (swap) {
          copyFieldsToProfileAddress(addr, profAddr);
          newNickName = createNickName(addr);
          if (!addrNickName.equals(newNickName)) {
            secAddrs.remove(addrNickName);
            secAddrs.put(newNickName, profAddr);
          }
          
          ((MutableRepository) profile.getRepository()).updateItem(profAddr);
        }
        
      }
      return (newNickName);
    }
    
    catch (final Exception e) {
      throw e;
    }
  }
  
  public String addNewAddress(final NMProfile profile, final ContactInfo addr) throws Exception {
    final String nickname = createNickName(addr);
    addr.setContactAddressName(nickname);
    final MutableRepository repos = (MutableRepository) profile.getRepository();
    final MutableRepositoryItem contact = repos.createItem(ProfileProperties.Profile_Desc_contactInfo);
    copyFieldsToProfileAddress(addr, contact);
    repos.addItem(contact);
    
    @SuppressWarnings("unchecked")
    final Map<String, MutableRepositoryItem> addressMap = (Map<String, MutableRepositoryItem>) profile.getPropertyValue(ProfileProperties.Profile_secondaryAddresses);
    addressMap.put(nickname, contact);
    return (nickname);
  }
  
  public boolean addressesAreTheSame(final ContactInfo addr1, final ContactInfo addr2) {
    if ((null == addr1) || (null == addr2)) {
      return (false);
    }
    
    return (StringUtilities.areEqualIgnoreCase(addr1.getFirstName(), addr2.getFirstName()) && StringUtilities.areEqualIgnoreCase(addr1.getLastName(), addr2.getLastName())
            && StringUtilities.areEqualIgnoreCase(addr1.getAddressLine1(), addr2.getAddressLine1()) && StringUtilities.areEqualIgnoreCase(addr1.getAddressLine2(), addr2.getAddressLine2())
            && StringUtilities.areEqualIgnoreCase(addr1.getCity(), addr2.getCity()) && StringUtilities.areEqualIgnoreCase(addr1.getState(), addr2.getState())
            && StringUtilities.areEqualIgnoreCase(addr1.getCountry(), addr2.getCountry()) && StringUtilities.areEqualIgnoreCase(addr1.getZip(), addr2.getZip()));
  }
  
  private String getInvalidCharacterError(final String invalidChars, final String fieldName) {
    return MessageDefs.getInvalidCharacterError(invalidChars, fieldName);
  }
  
  // Method to get InvalidLatinOrPinyinCharacters message
  private String getInvalidLatinOrPinyinCharacterError(final boolean invalidChars, final String fieldName) {
    return MessageDefs.getInvalidLatinOrPinyinCharacterError(invalidChars, fieldName);
  }
  
  private String getInvalidLengthForCountyField(final boolean invalidChars, final int maxLength) {
    return MessageDefs.getInvalidLengthForCountyFieldError(invalidChars, maxLength);
  }
  
  @SuppressWarnings("null")
  public AddressUtil copyProfileOrGiftlistAddressToShippingGroupAddress(final NMOrderImpl order, final OrderManager orderMgr, final NMProfile profile, final String addressName,
          final ShippingGroup shippingGroup) throws CommerceException, RepositoryException {
    
    try {
      final RepositoryItem profileAddress = CheckoutComponents.getCommerceProfileTools().getProfileAddress(profile, addressName);
      final NMRepositoryContactInfo shipGroupAddress = ShippingGroupUtil.getInstance().getAddress(shippingGroup);
      buildShipToRegistries(orderMgr, order, profile);
      
      final List<HardgoodShippingGroup> shippingGroups = order.getShippingGroups();
      Address currentShippingAddress = null;
      for (final HardgoodShippingGroup orderShippingGroup : shippingGroups) {
        currentShippingAddress = orderShippingGroup.getShippingAddress();
        if (null != currentShippingAddress) {
          break;
        }
      }
      
      if (null != profileAddress) {
        shipGroupAddress.setFlgGiftReg(new Boolean(false));
        shipGroupAddress.setTransGiftRegId("");
        OrderTools.copyAddress(profileAddress, shipGroupAddress);
      } else {
        shipGroupAddress.setFlgGiftReg(new Boolean(false));
        shipGroupAddress.setTransGiftRegId("");
        OrderTools.copyAddress(((RepositoryContactInfo) currentShippingAddress).getRepositoryItem(), shipGroupAddress);
      }
      
      if (!order.isMasterPassPayGroupOnOrder()) {
        if (profileAddress != null) {
          shipGroupAddress.setFlgGiftReg(new Boolean(false));
          shipGroupAddress.setTransGiftRegId("");
          OrderTools.copyAddress(profileAddress, shipGroupAddress);
        } else {
          final Map<String, String> viewedRegistries = profile.getViewedRegistriesList();
          final String registryAddressId = viewedRegistries.get(addressName);
          
          if (StringUtils.isNotBlank(registryAddressId)) {
            final RepositoryItem registryShippingAddress = profile.getRepositoryItem(registryAddressId, "contactInfo");
            
            shipGroupAddress.setFlgGiftReg(new Boolean(true));
            shipGroupAddress.setTransGiftRegId(addressName);
            
            final GiftlistManager giftlistMgr = profile.getGiftlistManager();
            final RepositoryItem giftlist = giftlistMgr.getGiftlist(addressName);
            final String deliveryPhoneNum = (String) giftlist.getPropertyValue("deliveryPhone");
            
            for (final NMCommerceItem item : ShippingGroupUtil.getInstance().getItems(shippingGroup)) {
              if (item.getProduct().getFlgDeliveryPhone()) {
                shipGroupAddress.setDeliveryPhoneNumber(deliveryPhoneNum);
              }
            }
            OrderTools.copyAddress(registryShippingAddress, shipGroupAddress);
          } else {
            if ((addressName != null) && !addressName.equals(shippingGroup.getId())) {
              throw new IllegalArgumentException("Registry address " + addressName + " is missing for profile " + profile);
            }
          }
        }
      }
      shippingGroup.setDescription(addressName);
      
    } catch (final IllegalArgumentException e) {
      throw e;
    }
    
    return this;
  }
  
  private AddressUtil buildShipToRegistries(final OrderManager orderMgr, final NMOrderImpl order, final NMProfile profile) throws CommerceException, RepositoryException {
    final Repository giftlistRepo = profile.getGiftlistRepository();
    final Map<String, String> viewedGiftlists = profile.getViewedRegistriesList();
    final Set<String> giftlistIds = profile.getViewedRegistryIds();
    final List<NMCommerceItem> shippingGroupItems = OrderUtil.getInstance().getShippingGroupCommerceItems(order, orderMgr);
    
    for (final NMCommerceItem item : shippingGroupItems) {
      final String registryId = item.getRegistryId();
      if (StringUtils.isNotEmpty(registryId)) {
        giftlistIds.add(registryId);
      }
    }
    
    viewedGiftlists.clear();
    
    for (final String registryId : giftlistIds) {
      final RepositoryItem giftlist = giftlistRepo.getItem(registryId, "gift-list");
      final NMRepositoryContactInfo giftlistAddress = new NMRepositoryContactInfo();
      giftlistAddress.setRepositoryItem((MutableRepositoryItem) giftlist.getPropertyValue("shippingAddress"));
      viewedGiftlists.put(registryId, giftlistAddress.getRepositoryItem().getRepositoryId());
    }
    
    return this;
  }
  
  public Boolean getIsShipAddressPOBox(final NMProfile profile, final String addressKey) {
    Boolean POBox = Boolean.valueOf("false");
    final NMAddress address = getAddressFromKey(profile, addressKey);
    
    if (address != null) {
      POBox = address.getFlgPOBox();
    }
    
    return POBox != null ? POBox : new Boolean(false);
  }
  
  public NMAddress getAddressFromKey(final NMProfile profile, final String addressKey) {
    
    if (StringUtils.isEmpty(addressKey)) {
      return null;
    }
    
    final CommercePropertyManager pm = (CommercePropertyManager) CheckoutComponents.getCommerceProfileTools().getPropertyManager();
    final String defaultShippingAddrName = pm.getDefaultShippingAddrName(java.util.Locale.US);
    final String defaultBillingAddrName = pm.getDefaultBillingAddrName(java.util.Locale.US);
    
    if (addressKey.equals(defaultShippingAddrName)) {
      return profile.getShippingNMAddress();
    } else if (addressKey.equals(defaultBillingAddrName)) {
      return profile.getBillingNMAddress();
    } else {
      final Map<String, MutableRepositoryItem> secondaryAddresses = profile.getSecondaryAddresses();
      final Set<String> addressNames = secondaryAddresses.keySet();
      
      for (final String addressName : addressNames) {
        if (addressName.equals(addressKey)) {
          return new NMAddress(secondaryAddresses.get(addressName));
        }
      }
    }
    
    return null;
  }
  
  @SuppressWarnings("static-access")
  public Map<String, NMAddress> buildShippingAddresses(final Order order, final NMProfile profile) throws CommerceException, DuplicateIdException, RepositoryException {
    final Map<String, NMAddress> addresses = new LinkedHashMap<String, NMAddress>();
    final Map<String, NMAddress> finalAddresses = new LinkedHashMap<String, NMAddress>();
    
    final CommercePropertyManager pm = (CommercePropertyManager) CheckoutComponents.getCommerceProfileTools().getPropertyManager();
    final String defaultShippingAddrName = pm.getDefaultShippingAddrName(java.util.Locale.US);
    if (null != profile.getShippingNMAddress()) {
      addresses.put(defaultShippingAddrName, profile.getShippingNMAddress());
    }
    final Map<String, NMAddress> secondaryNMAddressMap = profile.getSecondaryNMAddresses();
    addresses.putAll(secondaryNMAddressMap);
    MutableRepositoryItem tmpMutableRepositoryItem = null;
    String keyNickName = "";
    String storeNumber = "";
    NMAddress mapValue = null;
    Store store = null;
    final List<HardgoodShippingGroup> shippingGroups = order.getShippingGroups();
    Address shippingAddress = null;
    for (final HardgoodShippingGroup shippingGroup : shippingGroups) {
      shippingAddress = shippingGroup.getShippingAddress();
      final List<ShippingGroupCommerceItemRelationship> commerceItemRelationships = shippingGroup.getCommerceItemRelationships();
      
      for (final ShippingGroupCommerceItemRelationship shippingGroupCommerceItemRelationship : commerceItemRelationships) {
        final NMCommerceItem nMCommerceItem = (NMCommerceItem) shippingGroupCommerceItemRelationship.getCommerceItem();
        storeNumber = nMCommerceItem.getPickupStoreNo();
        break;
      }
      store = CheckoutComponents.getShipToStoreHelper().getStoreAddressByStoreNumber(storeNumber);
      String storeAddressNickName = null;
      if (null != store) {
        storeAddressNickName =
                AddressNickname.createShipAddressNickname(EMPTY_STRING, EMPTY_STRING, store.getName(), store.getAddress1(), store.getCity(), store.getState(), store.getZipcode(), EMPTY_STRING,
                        EMPTY_STRING);
      }
      final String shippingAddressNickName =
              AddressNickname.createShipAddressNickname(EMPTY_STRING, EMPTY_STRING, shippingAddress.getAddress1(), shippingAddress.getAddress2(), shippingAddress.getCity(),
                      shippingAddress.getState(), shippingAddress.getPostalCode(), EMPTY_STRING, EMPTY_STRING);
      // skip store address to be listed from the address dropdown listing in ship to multiple page
      if (!shippingAddressNickName.equalsIgnoreCase(storeAddressNickName)) {
        tmpMutableRepositoryItem = ((MutableRepository) profile.getRepository()).createItem("contactInfo");
        OrderTools.copyAddress(shippingAddress, tmpMutableRepositoryItem);
        final NMAddress nmAddress = new NMAddress(tmpMutableRepositoryItem);
        keyNickName = nmAddress.getAddressNickname();
        mapValue = addresses.get(MY_SHIPPING_ADDRESS);
        if ((null != mapValue) && keyNickName.equalsIgnoreCase(mapValue.getAddressNickname())) {
          // donot add this address to the map as it is already there.
        } else if (!addresses.containsKey(keyNickName)) {
          addresses.put(keyNickName, nmAddress);
          finalAddresses.put(keyNickName, nmAddress);
        }
      }
    }
    finalAddresses.putAll(addresses);
    // Add giftlist info
    final Set<String> giftlistIds = new LinkedHashSet<String>();
    
    @SuppressWarnings("unchecked")
    final List<NMCommerceItem> orderItems = order.getCommerceItems();
    for (final NMCommerceItem item : orderItems) {
      final String giftlistId = item.getRegistryId();
      if (StringUtils.isNotBlank(giftlistId)) {
        giftlistIds.add(giftlistId);
      }
    }
    
    final GiftlistManager giftMgr = profile.getGiftlistManager();
    
    for (final String giftlistId : giftlistIds) {
      final RepositoryItem riGiftlist = giftMgr.getGiftlist(giftlistId);
      final RepositoryItem riGiftlistType = (RepositoryItem) riGiftlist.getPropertyValue("giftlistType");
      final RepositoryItem riOwner = (RepositoryItem) riGiftlist.getPropertyValue("owner");
      final NMAddress riAddress = new NMAddress((MutableRepositoryItem) riGiftlist.getPropertyValue("shippingAddress"));
      final NMAddress billingAddress = new NMAddress((MutableRepositoryItem) riOwner.getPropertyValue("billingAddress"));
      String wlName = billingAddress.getFirstName() + " " + billingAddress.getLastName();
      
      if ("ACTIVE_WISH_LIST".equals(riGiftlistType.getPropertyValue("giftlistType"))) {
        wlName += " 's Wish List Address";
      } else if ("ACTIVE_GIFT_REGISTRY".equals(riGiftlistType.getPropertyValue("giftlistType"))) {
        wlName += " 's Registry Address";
      } else {
        wlName += " 's Address";
      }
      riAddress.setLabel(wlName);
      finalAddresses.put(giftlistId, riAddress);
    }
    // Do not show an empty entry of address in Ship to Multiple address dropdown
    if ((finalAddresses.size() > 1) && ((NMOrderImpl) order).isMasterPassPayGroupOnOrder() && ("...").equalsIgnoreCase(finalAddresses.get(MY_SHIPPING_ADDRESS).getAddressNickname().trim())) {
      finalAddresses.remove(MY_SHIPPING_ADDRESS);
    }
    
    return finalAddresses;
  }
  
  public MutableRepositoryItem copyFieldsToOrderAddress(final ContactInfo fields, final MutableRepositoryItem orderAddress) {
    orderAddress.setPropertyValue(OrderAddressProperties.Contact_address1, fields.getAddressLine1());
    orderAddress.setPropertyValue(OrderAddressProperties.Contact_address2, fields.getAddressLine2());
    orderAddress.setPropertyValue(OrderAddressProperties.Contact_city, fields.getCity());
    orderAddress.setPropertyValue(OrderAddressProperties.Contact_companyName, fields.getCompanyName());
    orderAddress.setPropertyValue(OrderAddressProperties.Contact_country, fields.getCountry());
    orderAddress.setPropertyValue(OrderAddressProperties.Contact_province, fields.getProvince());
    orderAddress.setPropertyValue(OrderAddressProperties.Contact_phoneNumber, fields.getDayTelephone());
    orderAddress.setPropertyValue(OrderAddressProperties.Contact_dayPhoneExt, fields.getDayTelephoneExt());
    orderAddress.setPropertyValue(OrderAddressProperties.Contact_evePhoneNumber, fields.getEveningTelephone());
    orderAddress.setPropertyValue(OrderAddressProperties.Contact_firstName, fields.getFirstName());
    orderAddress.setPropertyValue(OrderAddressProperties.Contact_lastName, fields.getLastName());
    orderAddress.setPropertyValue(OrderAddressProperties.Contact_flgPOBox, new Boolean(fields.getPoBox()));
    orderAddress.setPropertyValue(OrderAddressProperties.Contact_state, fields.getState());
    orderAddress.setPropertyValue(OrderAddressProperties.Contact_suffixCode, fields.getSuffixCode());
    orderAddress.setPropertyValue(OrderAddressProperties.Contact_titleCode, fields.getTitleCode());
    orderAddress.setPropertyValue(OrderAddressProperties.Contact_postalCode, fields.getZip());
    orderAddress.setPropertyValue(OrderAddressProperties.Contact_verificationFlag, new Integer(fields.getVerificationFlag()));
    orderAddress.setPropertyValue(OrderAddressProperties.Contact_verificationAddressType, fields.getVerAddressType());
    orderAddress.setPropertyValue(OrderAddressProperties.Contact_verificationCountyCode, fields.getVerCountyCode());
    orderAddress.setPropertyValue(OrderAddressProperties.Contact_flgCountyCodeVer, fields.getFlgCountyCodeVer());
    orderAddress.setPropertyValue(OrderAddressProperties.Contact_phoneType, fields.getPhoneType());
    orderAddress.setPropertyValue(OrderAddressProperties.Contact_verificationFlag, fields.getVerificationFlag());
    orderAddress.setPropertyValue(OrderAddressProperties.Contact_geoCodeLatitude, fields.getGeoCodeLatitude());
    orderAddress.setPropertyValue(OrderAddressProperties.Contact_geoCodeLongitude, fields.getGeoCodeLongitude());
    orderAddress.setPropertyValue(OrderAddressProperties.Contact_geoCodeTaxKey, fields.getGeoCodeTaxKey());
    orderAddress.setPropertyValue(OrderAddressProperties.Contact_geoCodeRefreshFlag, fields.isGeoCodeRefreshFlag());
    orderAddress.setPropertyValue(OrderAddressProperties.Contact_lastGeoCodeReqDate, fields.getLastGeoCodeReqDate());
    orderAddress.setPropertyValue(OrderAddressProperties.Contact_County, fields.getCounty());
    orderAddress.setPropertyValue(OrderAddressProperties.Contact_cpf, fields.getCpf());
    try {
      if (orderAddress.getItemDescriptor().getItemDescriptorName().equals("contactInfo")) {
        orderAddress.setPropertyValue(OrderAddressProperties.Contact_alias, fields.getAlias());
      }
    } catch (final RepositoryException re) {}
    return (orderAddress);
  }
  
  public MutableRepositoryItem copyFieldsToProfileAddress(final ContactInfo fields, final MutableRepositoryItem orderAddress) {
    
    orderAddress.setPropertyValue(ProfileProperties.Contact_address1, fields.getAddressLine1());
    orderAddress.setPropertyValue(ProfileProperties.Contact_address2, fields.getAddressLine2());
    orderAddress.setPropertyValue(ProfileProperties.Contact_city, fields.getCity());
    orderAddress.setPropertyValue(ProfileProperties.Contact_companyName, fields.getCompanyName());
    orderAddress.setPropertyValue(ProfileProperties.Contact_country, fields.getCountry());
    orderAddress.setPropertyValue(ProfileProperties.Contact_province, fields.getProvince());
    orderAddress.setPropertyValue(ProfileProperties.Contact_phoneNumber, fields.getDayTelephone());
    orderAddress.setPropertyValue(ProfileProperties.Contact_dayPhoneExt, fields.getDayTelephoneExt());
    orderAddress.setPropertyValue(ProfileProperties.Contact_evePhoneNumber, fields.getEveningTelephone());
    orderAddress.setPropertyValue(ProfileProperties.Contact_firstName, fields.getFirstName());
    orderAddress.setPropertyValue(ProfileProperties.Contact_lastName, fields.getLastName());
    orderAddress.setPropertyValue(ProfileProperties.Contact_flgPOBox, new Boolean(fields.getPoBox()));
    orderAddress.setPropertyValue(ProfileProperties.Contact_state, fields.getState());
    orderAddress.setPropertyValue(ProfileProperties.Contact_suffixCode, fields.getSuffixCode());
    orderAddress.setPropertyValue(ProfileProperties.Contact_titleCode, fields.getTitleCode());
    orderAddress.setPropertyValue(ProfileProperties.Contact_postalCode, fields.getZip());
    orderAddress.setPropertyValue(ProfileProperties.Contact_verificationFlag, new Integer(fields.getVerificationFlag()));
    orderAddress.setPropertyValue(ProfileProperties.Contact_verificationAddressType, fields.getVerAddressType());
    orderAddress.setPropertyValue(ProfileProperties.Contact_verificationCountyCode, fields.getVerCountyCode());
    orderAddress.setPropertyValue(ProfileProperties.Contact_flgCountyCodeVer, fields.getFlgCountyCodeVer());
    orderAddress.setPropertyValue(ProfileProperties.Contact_phoneType, fields.getPhoneType());
    orderAddress.setPropertyValue(ProfileProperties.Contact_verificationFlag, fields.getVerificationFlag());
    orderAddress.setPropertyValue(ProfileProperties.Contact_geoCodeLatitude, fields.getGeoCodeLatitude());
    orderAddress.setPropertyValue(ProfileProperties.Contact_geoCodeLongitude, fields.getGeoCodeLongitude());
    orderAddress.setPropertyValue(ProfileProperties.Contact_geoCodeTaxKey, fields.getGeoCodeTaxKey());
    orderAddress.setPropertyValue(ProfileProperties.Contact_geoCodeRefreshFlag, fields.isGeoCodeRefreshFlag());
    orderAddress.setPropertyValue(ProfileProperties.Contact_lastGeoCodeReqDate, fields.getLastGeoCodeReqDate());
    orderAddress.setPropertyValue(ProfileProperties.Contact_county, fields.getCounty());
    orderAddress.setPropertyValue(ProfileProperties.Contact_cpf, fields.getCpf());
    try {
      if (orderAddress.getItemDescriptor().getItemDescriptorName().equals("contactInfo")) {
        orderAddress.setPropertyValue(ProfileProperties.Contact_alias, fields.getAlias());
      }
    } catch (final RepositoryException re) {}
    return (orderAddress);
  }
  
  public ContactInfo orderAddressToContactInfo(final RepositoryItem orderAddress) {
    final ContactInfo addr = new ContactInfo();
    if (null != orderAddress) {
      addr.setAddressLine1((String) orderAddress.getPropertyValue(OrderAddressProperties.Contact_address1));
      addr.setAddressLine2((String) orderAddress.getPropertyValue(OrderAddressProperties.Contact_address2));
      addr.setCity((String) orderAddress.getPropertyValue(OrderAddressProperties.Contact_city));
      addr.setCompanyName((String) orderAddress.getPropertyValue(OrderAddressProperties.Contact_companyName));
      addr.setCountry((String) orderAddress.getPropertyValue(OrderAddressProperties.Contact_country));
      addr.setProvince((String) orderAddress.getPropertyValue(OrderAddressProperties.Contact_province));
      addr.setDayTelephone((String) orderAddress.getPropertyValue(OrderAddressProperties.Contact_phoneNumber));
      addr.setDayTelephoneExt((String) orderAddress.getPropertyValue(OrderAddressProperties.Contact_dayPhoneExt));
      addr.setEveningTelephone((String) orderAddress.getPropertyValue(OrderAddressProperties.Contact_evePhoneNumber));
      addr.setFirstName((String) orderAddress.getPropertyValue(OrderAddressProperties.Contact_firstName));
      addr.setLastName((String) orderAddress.getPropertyValue(OrderAddressProperties.Contact_lastName));
      final Boolean isPOBox = (Boolean) orderAddress.getPropertyValue(OrderAddressProperties.Contact_flgPOBox);
      if (isPOBox != null) {
        addr.setPoBox(isPOBox.toString());
      }
      addr.setState((String) orderAddress.getPropertyValue(OrderAddressProperties.Contact_state));
      addr.setSuffixCode((String) orderAddress.getPropertyValue(OrderAddressProperties.Contact_suffixCode));
      addr.setTitleCode((String) orderAddress.getPropertyValue(OrderAddressProperties.Contact_titleCode));
      addr.setZip((String) orderAddress.getPropertyValue(OrderAddressProperties.Contact_postalCode));
      addr.setId(orderAddress.getRepositoryId());
      addr.setPhoneType((String) orderAddress.getPropertyValue(OrderAddressProperties.Contact_phoneType));
      addr.setGeoCodeLatitude((Double) orderAddress.getPropertyValue(OrderAddressProperties.Contact_geoCodeLatitude));
      addr.setGeoCodeLongitude((Double) orderAddress.getPropertyValue(OrderAddressProperties.Contact_geoCodeLongitude));
      addr.setGeoCodeTaxKey((String) orderAddress.getPropertyValue(OrderAddressProperties.Contact_geoCodeTaxKey));
      addr.setGeoCodeRefreshFlag((Boolean) orderAddress.getPropertyValue(OrderAddressProperties.Contact_geoCodeRefreshFlag));
      addr.setLastGeoCodeReqDate((Date) orderAddress.getPropertyValue(OrderAddressProperties.Contact_lastGeoCodeReqDate));
      addr.setCounty((String) orderAddress.getPropertyValue(OrderAddressProperties.Contact_County));
      addr.setCpf((String) orderAddress.getPropertyValue(OrderAddressProperties.Contact_cpf));
    }
    
    return (addr);
  }
  
  public ContactInfo profileAddressToContactInfo(final RepositoryItem profileAddress) {
    final ContactInfo addr = new ContactInfo();
    if (null != profileAddress) {
      addr.setAddressLine1((String) profileAddress.getPropertyValue(ProfileProperties.Contact_address1));
      addr.setAddressLine2((String) profileAddress.getPropertyValue(ProfileProperties.Contact_address2));
      addr.setCity((String) profileAddress.getPropertyValue(ProfileProperties.Contact_city));
      addr.setCompanyName((String) profileAddress.getPropertyValue(ProfileProperties.Contact_companyName));
      addr.setCountry((String) profileAddress.getPropertyValue(ProfileProperties.Contact_country));
      addr.setProvince((String) profileAddress.getPropertyValue(ProfileProperties.Contact_province));
      addr.setDayTelephone((String) profileAddress.getPropertyValue(ProfileProperties.Contact_phoneNumber));
      addr.setDayTelephoneExt((String) profileAddress.getPropertyValue(ProfileProperties.Contact_dayPhoneExt));
      addr.setEveningTelephone((String) profileAddress.getPropertyValue(ProfileProperties.Contact_evePhoneNumber));
      addr.setFirstName((String) profileAddress.getPropertyValue(ProfileProperties.Contact_firstName));
      addr.setLastName((String) profileAddress.getPropertyValue(ProfileProperties.Contact_lastName));
      final Boolean isPOBox = (Boolean) profileAddress.getPropertyValue(ProfileProperties.Contact_flgPOBox);
      if (isPOBox != null) {
        addr.setPoBox(isPOBox.toString());
      }
      addr.setState((String) profileAddress.getPropertyValue(ProfileProperties.Contact_state));
      addr.setSuffixCode((String) profileAddress.getPropertyValue(ProfileProperties.Contact_suffixCode));
      addr.setTitleCode((String) profileAddress.getPropertyValue(ProfileProperties.Contact_titleCode));
      addr.setZip((String) profileAddress.getPropertyValue(ProfileProperties.Contact_postalCode));
      addr.setId(profileAddress.getRepositoryId());
      addr.setPhoneType((String) profileAddress.getPropertyValue(ProfileProperties.Contact_phoneType));
      addr.setGeoCodeLatitude((Double) profileAddress.getPropertyValue(ProfileProperties.Contact_geoCodeLatitude));
      addr.setGeoCodeLongitude((Double) profileAddress.getPropertyValue(ProfileProperties.Contact_geoCodeLongitude));
      addr.setGeoCodeTaxKey((String) profileAddress.getPropertyValue(ProfileProperties.Contact_geoCodeTaxKey));
      addr.setGeoCodeRefreshFlag((Boolean) profileAddress.getPropertyValue(ProfileProperties.Contact_geoCodeRefreshFlag));
      addr.setLastGeoCodeReqDate((Date) profileAddress.getPropertyValue(ProfileProperties.Contact_lastGeoCodeReqDate));
      addr.setCounty((String) profileAddress.getPropertyValue(ProfileProperties.Contact_county));
      addr.setCpf((String) profileAddress.getPropertyValue(ProfileProperties.Contact_cpf));
      
      final Integer value = (Integer) profileAddress.getPropertyValue(ProfileProperties.Contact_verificationFlag);
      if (value != null) {
        addr.setVerificationFlag(value.intValue());
      } else {
        addr.setVerificationFlag(AddressVerificationHelper.HAS_NOT_BEEN_VERIFIED);
      }
    }
    
    return (addr);
  }
  
  public ContactInfo profileAddressToContactInfo(final RepositoryItem profileAddress, final String contactName) {
    final ContactInfo addr = profileAddressToContactInfo(profileAddress);
    if (null != addr) {
      addr.setContactAddressName(contactName);
    }
    
    return (addr);
  }
  
  /**
   * invalidLatinOrPinyinCharacters will validate whether the input given in the form field is Latin, Pinyin or Mandarin sets the invalidLatinOrPinyinChar to true if it is Latin or Pinyin or returns
   * false if Mandarin is entered
   * 
   * @param value
   *          - input value given in the form field
   * @return boolean invalidLatinOrPinyinChar
   */
  public static boolean invalidLatinOrPinyinCharacters(final String value) {
    boolean invalidLatinOrPinyinChar = false;
    if ((value != null) && (value.trim().length() > 0)) {
      final String[] eachCharacterSet = value.split(DELIMITER_COLON);
      final Matcher matcher = VALID_LATIN_CHARACTER.matcher(value);
      if (!matcher.find()) {
        invalidLatinOrPinyinChar = true;
      } else {
        // validating for ascented vowels in the entered input
        final Pattern patternForASCII7 = Pattern.compile("\\p{ASCII}+");
        final Matcher allowedASCII7Characters = patternForASCII7.matcher(value);
        if (!allowedASCII7Characters.find()) {
          invalidLatinOrPinyinChar = true;
        } else {
          for (int i = 0; i < eachCharacterSet.length; i++) {
            // if(eachCharacterSet[i].contains(AMPERSAND_HASH_SYMBOL)){
            final Matcher m = MANDARIN_CHARACTER_PATTERN.matcher(eachCharacterSet[i]);
            if (m.find()) {
              final int encodedNumber = Integer.parseInt(eachCharacterSet[i].substring(eachCharacterSet[i].indexOf("#") + 1));
              if ((encodedNumber >= MINIMUM_MANDARIN_CHARACTER_RANGE) && (encodedNumber <= MAXIMUM_MANDARIN_CHARACTER_RANGE)) {
                invalidLatinOrPinyinChar = true;
                break;
              } else {
                final String enteredPinyin = eachCharacterSet[i].substring(eachCharacterSet[i].indexOf("#") + 1);
                final Matcher allowedPinyin = ALLOWED_PINYIN_CHARACTER.matcher(enteredPinyin);
                if (!m.find()) {
                  invalidLatinOrPinyinChar = true;
                  break;
                }
              }
            }
            
          }
        }
        
      }
    }
    return invalidLatinOrPinyinChar;
    
  }
  
  /**
   * validateCounty will validate whether the county input given in the form field is greater than maxlength and sets the lengthGreaterThanMaxAllowed to true if it is greater or returns false if less
   * than 3 characters
   * 
   * @return boolean lengthGreaterThanMaxAllowed
   */
  public static boolean validateCounty(final String value) {
    boolean lengthGreaterThanMaxAllowed = false;
    if ((value != null) && (value.trim().length() > 0)) {
      if (value.length() > 3) {
        lengthGreaterThanMaxAllowed = true;
      }
    }
    return lengthGreaterThanMaxAllowed;
  }
  
  /**
   * validateLatinOrPinyinCharacters will validate each input value for Mandarin. If Mandarin character is entered then sets the error message and adds it to the error message list sets the
   * invalidLatinOrPinyinChar to true if it is Latin or Pinyin or returns false if Mandarin is entered
   * 
   * @param addr
   *          - ContactInfo details
   * @param addressFieldSetKey
   * @return error message list
   */
  public List<Message> validateLatinOrPinyinCharacters(final ContactInfo addr, final String addressFieldSetKey, final String addressType, final boolean isInternational) {
    final List<Message> messages = new ArrayList<Message>();
    
    boolean invalidChars = invalidLatinOrPinyinCharacters(addr.getFirstName());
    if (invalidChars) {
      final Message msg = MessageDefs.getMessage(MessageDefs.MSG_InvalidLatinOrPinyinCharacters);
      msg.setFieldId(AddressMessageFields.getFieldId(addressFieldSetKey, AddressMessageFields.FIELD_FIRST_NAME));
      if (isInternational) {
        msg.setMsgIntlText(getInvalidLatinOrPinyinCharacterError(invalidChars, "First Name/Given Name"));
      } else {
        msg.setMsgText(getInvalidLatinOrPinyinCharacterError(invalidChars, "First Name"));
        
      }
      messages.add(msg);
    }
    invalidChars = invalidLatinOrPinyinCharacters(addr.getLastName());
    if (invalidChars) {
      final Message msg = MessageDefs.getMessage(MessageDefs.MSG_InvalidLatinOrPinyinCharacters);
      msg.setFieldId(AddressMessageFields.getFieldId(addressFieldSetKey, AddressMessageFields.FIELD_LAST_NAME));
      if (isInternational) {
        msg.setMsgIntlText(getInvalidLatinOrPinyinCharacterError(invalidChars, "Last Name/Family Name"));
      } else {
        msg.setMsgText(getInvalidLatinOrPinyinCharacterError(invalidChars, "Last Name"));
        
      }
      messages.add(msg);
    }
    /*
     * invalidChars = invalidLatinOrPinyinCharacters(addr.getCompanyName()); if( invalidChars ){ Message msg = MessageDefs.getMessage(MessageDefs.MSG_InvalidLatinOrPinyinCharacters);
     * msg.setFieldId(AddressMessageFields.getFieldId(addressFieldSetKey,AddressMessageFields.FIELD_COMPANY)); msg.setMsgIntlText(getInvalidLatinOrPinyinCharacterError(invalidChars, "Company Name"));
     * messages.add(msg); }
     */
    invalidChars = invalidLatinOrPinyinCharacters(addr.getAddress1());
    if (invalidChars) {
      final Message msg = MessageDefs.getMessage(MessageDefs.MSG_InvalidLatinOrPinyinCharacters);
      msg.setFieldId(AddressMessageFields.getFieldId(addressFieldSetKey, AddressMessageFields.FIELD_ADDRESS1));
      if (isInternational) {
        msg.setMsgIntlText(getInvalidLatinOrPinyinCharacterError(invalidChars, "Address 1"));
      } else {
        msg.setMsgText(getInvalidLatinOrPinyinCharacterError(invalidChars, "Address Line 1"));
        
      }
      messages.add(msg);
    }
    invalidChars = invalidLatinOrPinyinCharacters(addr.getAddress2());
    if (invalidChars) {
      final Message msg = MessageDefs.getMessage(MessageDefs.MSG_InvalidLatinOrPinyinCharacters);
      msg.setFieldId(AddressMessageFields.getFieldId(addressFieldSetKey, AddressMessageFields.FIELD_ADDRESS2));
      if (isInternational) {
        msg.setMsgIntlText(getInvalidLatinOrPinyinCharacterError(invalidChars, "Address 2"));
      } else {
        msg.setMsgText(getInvalidLatinOrPinyinCharacterError(invalidChars, "Address Line 2"));
        
      }
      messages.add(msg);
    }
    invalidChars = invalidLatinOrPinyinCharacters(addr.getCity());
    if (invalidChars) {
      final Message msg = MessageDefs.getMessage(MessageDefs.MSG_InvalidLatinOrPinyinCharacters);
      msg.setFieldId(AddressMessageFields.getFieldId(addressFieldSetKey, AddressMessageFields.FIELD_CITY));
      if (isInternational) {
        msg.setMsgIntlText(getInvalidLatinOrPinyinCharacterError(invalidChars, "City"));
      } else {
        msg.setMsgText(getInvalidLatinOrPinyinCharacterError(invalidChars, "City"));
        
      }
      
      messages.add(msg);
    }
    invalidChars = invalidLatinOrPinyinCharacters(addr.getPostalCode());
    if (invalidChars) {
      final Message msg = MessageDefs.getMessage(MessageDefs.MSG_InvalidLatinOrPinyinCharacters);
      msg.setFieldId(AddressMessageFields.getFieldId(addressFieldSetKey, AddressMessageFields.FIELD_ZIP));
      if (isInternational) {
        msg.setMsgIntlText(getInvalidLatinOrPinyinCharacterError(invalidChars, "Postal Code/Zip Code"));
      } else {
        msg.setMsgText(getInvalidLatinOrPinyinCharacterError(invalidChars, "Zip Code"));
      }
      messages.add(msg);
    }
    
    invalidChars = invalidLatinOrPinyinCharacters(addr.getVerCountyCode());
    if (invalidChars) {
      final Message msg = MessageDefs.getMessage(MessageDefs.MSG_InvalidLatinOrPinyinCharacters);
      msg.setFieldId(AddressMessageFields.getFieldId(addressFieldSetKey, AddressMessageFields.FIELD_COUNTY));
      if (isInternational) {
        msg.setMsgIntlText(getInvalidLatinOrPinyinCharacterError(invalidChars, "County"));
      } else {
        msg.setMsgText(getInvalidLatinOrPinyinCharacterError(invalidChars, "County"));
      }
      messages.add(msg);
    } else {
      final boolean lengthGreaterThanMaxAllowed = validateCounty(addr.getVerCountyCode());
      if (lengthGreaterThanMaxAllowed) {
        final Message msg = MessageDefs.getMessage(MessageDefs.MSG_LengthGreaterThanMaxAllowed);
        msg.setFieldId(AddressMessageFields.getFieldId(addressFieldSetKey, AddressMessageFields.FIELD_COUNTY));
        msg.setMsgIntlText(getInvalidLengthForCountyField(invalidChars, 3));
        messages.add(msg);
      }
    }
    invalidChars = invalidLatinOrPinyinCharacters(addr.getPhoneNumber());
    if (invalidChars) {
      final Message msg = MessageDefs.getMessage(MessageDefs.MSG_InvalidLatinOrPinyinCharacters);
      msg.setFieldId(AddressMessageFields.getFieldId(addressFieldSetKey, AddressMessageFields.FIELD_DAY_TELEPHONE));
      if (isInternational) {
        msg.setMsgIntlText(getInvalidLatinOrPinyinCharacterError(invalidChars, "Phone"));
      } else {
        msg.setMsgText(getInvalidLatinOrPinyinCharacterError(invalidChars, "Phone"));
      }
      
      messages.add(msg);
    }
    invalidChars = invalidLatinOrPinyinCharacters(addr.getState());
    if (invalidChars) {
      final Message msg = MessageDefs.getMessage(MessageDefs.MSG_InvalidLatinOrPinyinCharacters);
      msg.setFieldId(AddressMessageFields.getFieldId(addressFieldSetKey, AddressMessageFields.FIELD_PROVINCE));
      if (isInternational) {
        msg.setMsgIntlText(getInvalidLatinOrPinyinCharacterError(invalidChars, "Province/State"));
      } else {
        msg.setMsgText(getInvalidLatinOrPinyinCharacterError(invalidChars, "State"));
      }
      messages.add(msg);
    }
    if (addressType.equals(BILLING_ADDRESS)) {
      invalidChars = invalidLatinOrPinyinCharacters(addr.getEmailAddress());
      if (invalidChars) {
        final Message msg = MessageDefs.getMessage(MessageDefs.MSG_InvalidLatinOrPinyinCharacters);
        msg.setFieldId(AddressMessageFields.getFieldId(addressFieldSetKey, AddressMessageFields.FIELD_EMAIL));
        if (isInternational) {
          msg.setMsgIntlText(getInvalidLatinOrPinyinCharacterError(invalidChars, "Email"));
        }
        
        else {
          msg.setMsgText(getInvalidLatinOrPinyinCharacterError(invalidChars, "Email"));
        }
        messages.add(msg);
      }
    }
    return messages;
  }
  
}
