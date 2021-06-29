package com.nm.commerce.checkout.VMeInfo;

import static com.nm.common.INMGenericConstants.CHECKOUT;
import static com.nm.integration.VmeConstants.VME;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpSession;

import atg.commerce.order.HardgoodShippingGroup;
import atg.commerce.order.ShippingGroup;
import atg.core.util.StringUtils;
import atg.json.JSONException;
import atg.json.JSONObject;
import atg.repository.MutableRepository;
import atg.repository.MutableRepositoryItem;
import atg.repository.RepositoryException;
import atg.repository.RepositoryItem;
import atg.servlet.ServletUtil;

import com.nm.collections.NMPromotion;
import com.nm.commerce.NMAddress;
import com.nm.commerce.NMCommerceItem;
import com.nm.commerce.NMCommerceProfileTools;
import com.nm.commerce.NMCreditCard;
import com.nm.commerce.NMProfile;
import com.nm.commerce.checkout.CheckoutAPI;
import com.nm.commerce.checkout.CheckoutComponents;
import com.nm.commerce.checkout.beans.ContactInfo;
import com.nm.commerce.order.NMOrderManager;
import com.nm.commerce.pricing.Markdown;
import com.nm.commerce.promotion.NMOrderImpl;
import com.nm.components.CommonComponentHelper;
import com.nm.formhandler.ShoppingCartHandler;
import com.nm.integration.PaymentDetailsVO;
import com.nm.integration.ShippingDetailsVO;
import com.nm.integration.VMe;
import com.nm.integration.VMeInfoVO;
import com.nm.integration.VmeConstants;
import com.nm.integration.VmeException;
import com.nm.profile.ProfileProperties;
import com.nm.sitemessages.Message;
import com.nm.utils.AddressNickname;
import com.nm.utils.LocalizationUtils;
import com.nm.utils.PromotionsHelper;
import com.nm.utils.StringUtilities;
import com.nm.utils.SystemSpecs;
import com.nm.utils.crypto.EncryptionException;

/**
 * This class gets the data from vme info session object and sets the data to profile and order based on various conditions.
 * 
 * @author Cognizant
 * 
 */
/**
 * @author vsmg8
 * 
 */
public class VMeUtil {
  
  private static VMeUtil INSTANCE;
  private static final int VME_ORDER = 3;
  private static final LocalizationUtils localizationUtils = CheckoutComponents.getLocalizationUtils();
  
  public static synchronized VMeUtil getInstance() {
    INSTANCE = INSTANCE == null ? new VMeUtil() : INSTANCE;
    return INSTANCE;
  }
  
  private final VMe vMe;
  
  // private constructor enforces singleton behavior
  private VMeUtil() {
    vMe = CheckoutComponents.getvMe();
  }
  
  public VMe getvMe() {
    return vMe;
  }
  
  /**
   * This method takes the shipping and payment details from VO objects which has the values returned from visa checkout and then sets those values to shipping group and payment group of the order
   * 
   * @param cart
   *          Shopping cart
   * @param profile
   *          profile object
   * @return possible error messages
   * @throws VmeException
   *           due to invalid data/settings
   */
  public Collection<Message> updateBillingAndShippingDetails(final ShoppingCartHandler cart, final NMProfile profile) throws VmeException {
    final SystemSpecs systemSpecs = CommonComponentHelper.getSystemSpecs();
    final NMOrderImpl order = cart.getNMOrder();
    final NMOrderManager orderMgr = (NMOrderManager) cart.getOrderHolder().getOrderManager();
    final MutableRepository profileRepo = (MutableRepository) profile.getRepository();
    List<Message> messages = new ArrayList<Message>();
    final VMeInfoVO vMeInfoVO = CheckoutComponents.getVMeInfoVO(ServletUtil.getCurrentRequest());
    
    // session timeout issue code changes starts
    if (profile.isSessionExpired()) {
      profile.setPropertyValue("securityStatus", new Integer(0));
    }
    final HttpSession session = ServletUtil.getCurrentRequest().getSession();
    final String activeSession = (String) session.getAttribute("NM_Checkout_SessionActive");
    if (!"YES".equalsIgnoreCase(activeSession) && profile.isAnonymous() && !profile.isRegisteredProfile()) {
      session.setAttribute("NM_Checkout_SessionActive", "YES");
      
    }
    // session timeout issue code changes ends
    if (profile.isAnonymous() && null != vMeInfoVO.getPaymentDetails() && null != vMeInfoVO.getPaymentDetails().getvMeEmail()
            && (StringUtilities.isEmpty(profile.getEmailAddressBeanProp()) || (StringUtilities.isNotEmpty(profile.getEmailAddressBeanProp()) && !profile.getEmailAddressBeanProp().equalsIgnoreCase(
                    vMeInfoVO.getPaymentDetails().getvMeEmail().toLowerCase())))) {
      profile.setEmailAddressBeanProp(vMeInfoVO.getPaymentDetails().getvMeEmail().toLowerCase());
    }
    messages = updateVmeShippingAddress(profile, vMeInfoVO, systemSpecs, order, orderMgr, profileRepo, cart);
    updateVmeBillingAddress(profile, vMeInfoVO, order, orderMgr, profileRepo);
    return messages;
  }
  
  /**
   * This method updates the shipping address details returned by visa checkout response to the order and profile.
   * 
   * @param profile
   * @param vMeInfoVO
   * @param systemSpecs
   * @param order
   * @param orderMgr
   * @param profileRepo
   * @param cart
   * @return
   * @throws Exception
   */
  public List<Message> updateVmeShippingAddress(final NMProfile profile, final VMeInfoVO vMeInfoVO, final SystemSpecs systemSpecs, final NMOrderImpl order, final NMOrderManager orderMgr,
          final MutableRepository profileRepo, final ShoppingCartHandler cart) throws VmeException {
    final List<Message> messages = new ArrayList<Message>();
    final ShippingDetailsVO shippingDetailsVO = vMeInfoVO.getShippingInfo();
    // setting the visa checkout returned shipping details to String variables
    if (null != shippingDetailsVO) {
      final String fullName = shippingDetailsVO.getName();
      final Map<String, String> vmeFieldLengthMap = getvMe().getvmeFieldLengthMap();
      String lastName = null;
      String firstName = null;
      if (fullName != null) {
        final String[] firstLast = fullName.split(VmeConstants.SPACE_SEPARATOR);
        if (firstLast.length == 2) {
          firstName = vMe.sanitizeFieldLength(firstLast[0], Integer.parseInt(vmeFieldLengthMap.get(VmeConstants.VME_NAME_FLD_LENGTH)));
          lastName = vMe.sanitizeFieldLength(firstLast[1], Integer.parseInt(vmeFieldLengthMap.get(VmeConstants.VME_NAME_FLD_LENGTH)));
        } else {
          final int firstSpaceIndex = fullName.indexOf(VmeConstants.SPACE_SEPARATOR);
          final int secondSpaceIndex = fullName.indexOf(VmeConstants.SPACE_SEPARATOR, firstSpaceIndex + 1);
          final String first = fullName.substring(0, secondSpaceIndex);
          final String last = fullName.substring(secondSpaceIndex + 1);
          firstName = vMe.sanitizeFieldLength(first, Integer.parseInt(vmeFieldLengthMap.get(VmeConstants.VME_NAME_FLD_LENGTH)));
          lastName = vMe.sanitizeFieldLength(last, Integer.parseInt(vmeFieldLengthMap.get(VmeConstants.VME_NAME_FLD_LENGTH)));
        }
      }
      final String country = shippingDetailsVO.getCountryCode();
      final String address1 = vMe.sanitizeFieldLength(shippingDetailsVO.getAddressLine1(), Integer.parseInt(vmeFieldLengthMap.get(VmeConstants.VME_ADDR_FLD_LENGTH)));
      final String address2 = vMe.sanitizeFieldLength(shippingDetailsVO.getAddressLine2(), Integer.parseInt(vmeFieldLengthMap.get(VmeConstants.VME_ADDR_FLD_LENGTH)));
      final String city = vMe.sanitizeFieldLength(shippingDetailsVO.getCity(), Integer.parseInt(vmeFieldLengthMap.get(VmeConstants.VME_CITY_STATE_FLD_LENGTH)));
      final String state = vMe.sanitizeFieldLength(shippingDetailsVO.getStateProvinceCode(), Integer.parseInt(vmeFieldLengthMap.get(VmeConstants.VME_CITY_STATE_FLD_LENGTH)));
      final String zip = vMe.sanitizeFieldLength(shippingDetailsVO.getPostalCode(), Integer.parseInt(vmeFieldLengthMap.get(VmeConstants.VME_POSTAL_FLD_LENGTH)));
      final String phoneNum = vMe.sanitizeFieldLength(shippingDetailsVO.getPhone(), Integer.parseInt(vmeFieldLengthMap.get(VmeConstants.VME_PHONE_FLD_LENGTH)));
      // checking whether the visa checkout returned shipping address country is US. If not throw an error message
      if (!StringUtils.isEmpty(country) && localizationUtils.isSupportedByFiftyOne(country)) {
        final Message msg = new Message();
        if (systemSpecs.isFiftyOneEnabled()) {
          msg.setMsgText("You have selected an international address on VMe. Please choose a U.S. address or <a href='#' id='changeCountryLink'><b>click here</b></a> to change your shipping country and checkout with our international shipping partner, Borderfree.");
        } else {
          msg.setMsgText("You have selected an international address on VMe. Please choose a U.S. shipping address.");
        }
        msg.setFieldId("groupTop");
        messages.add(msg);
      } else if (StringUtilities.isNotEmpty(StringUtilities.trimSpaces(address1))) {
        NMAddress shippingAddress = null;
        // creating nick name for vme returned shipping details
        final String vMeNickname = AddressNickname.createNickname(firstName, lastName, address1, city, state, zip, country);
        final NMAddress defaultShippingAddress = profile.getShippingNMAddress();
        // creating nickname for default shipping address present in the profile if any
        final String defaultShippingNickname = defaultShippingAddress != null ? defaultShippingAddress.getAddressNickname() : null;
        boolean isDefaultShippingAddress = false;
        boolean isOldShippingGroupAddress = false;
        @SuppressWarnings("unchecked")
        final List<ShippingGroup> shippingGroups = order.getShippingGroups();
        for (final ShippingGroup shippingGroup : CheckoutAPI.getNonWishlistShippingGroups(shippingGroups)) {
          if (!StringUtilities.isEmpty(vMeNickname)) {
            isOldShippingGroupAddress = shippingGroup.getDescription().equalsIgnoreCase(vMeNickname);
          }
        }
        if (!StringUtilities.isEmpty(vMeNickname)) {
          /**
           * checking whether default shipping address and vme address are same if not remove vme address if existing in profile and create a new shipping address object
           **/
          if (vMeNickname.equalsIgnoreCase(defaultShippingNickname)) {
            shippingAddress = defaultShippingAddress;
            isDefaultShippingAddress = true;
          } else {
            final MutableRepositoryItem secondaryAddress = profile.getSecondaryAddresses().remove(vMeNickname);
            if (secondaryAddress != null) {
              shippingAddress = new NMAddress(secondaryAddress);
            }
          }
        }
        if (shippingAddress == null) {
          MutableRepositoryItem addressItem;
          try {
            addressItem = profileRepo.createItem(ProfileProperties.Profile_Desc_contactInfo);
            shippingAddress = new NMAddress(addressItem);
          } catch (final RepositoryException e) {
            throw new VmeException("Unable to process Visa checkout due to issues while updating shipping address: " + e);
          }
        }
        // If default shipping and vme shipping are different then put the default shipping address into secondary address and create a new default shipping repo item
        if (!isDefaultShippingAddress) {
          if ((defaultShippingAddress != null) && !StringUtilities.isEmpty(defaultShippingAddress.getAddress1())) {
            profile.getSecondaryAddresses().put(defaultShippingAddress.getAddressNickname(), defaultShippingAddress.getRepositoryItem());
          }
          profile.setShippingAddress(shippingAddress.getRepositoryItem());
        }
        // set vme shipping details to default shipping address
        shippingAddress.setFirstName(firstName);
        shippingAddress.setLastName(lastName);
        shippingAddress.setCountry(country);
        shippingAddress.setAddress1(address1);
        shippingAddress.setAddress2(address2);
        shippingAddress.setCity(city);
        setStateOrProvince(country, state, shippingAddress);
        shippingAddress.setPostalCode(zip);
        shippingAddress.setPhoneNumber(StringUtilities.isNotEmpty(phoneNum) ? phoneNum : vMeInfoVO.getShippingInfo().getPhone());
        shippingAddress.setVerificationFlag(VME_ORDER);
        shippingAddress.updateNickname();
        // update order shipping address
        try {
          for (final ShippingGroup shippingGroup : CheckoutAPI.getNonWishlistShippingGroups(shippingGroups)) {
            if (CheckoutAPI.orderContainsShippingAddress(order)) {
              if (!isOldShippingGroupAddress) {
                final ContactInfo contactInfo = CheckoutAPI.profileAddressToContactInfo(shippingAddress.getRepositoryItem());
                CheckoutAPI.updateShippingGroupAddress(cart, profile, order, shippingGroup.getId(), contactInfo, false);
              }
            } else {
              CheckoutAPI.resetCommerceItemsToDefaultAddresses(profile, order, orderMgr);
            }
          }
        } catch (final RepositoryException e) {
          throw new VmeException("Unable to process Visa checkout due to issues while updating shipping address: " + e);
        } catch (final Exception e) {
          throw new VmeException("Unable to process Visa checkout due to issues while updating credit card: " + e);
        }
      }
    }
    return messages;
  }
  
  /**
   * This method updates the billing address details and card details returned by visa checkout response to the order and profile
   * 
   * @param profile
   * @param vMeInfoVO
   * @param order
   * @param orderMgr
   * @param profileRepo
   * @throws Exception
   */
  public void updateVmeBillingAddress(final NMProfile profile, final VMeInfoVO vMeInfoVO, final NMOrderImpl order, final NMOrderManager orderMgr, final MutableRepository profileRepo)
          throws VmeException {
    boolean newVMeCard = false;
    NMCreditCard vMeCard = null;
    final NMCreditCard defaultCard = profile.getDefaultCreditCard();
    final Map<String, String> vmeFieldLengthMap = getvMe().getvmeFieldLengthMap();
    if ((defaultCard == null) || StringUtilities.isEmpty(defaultCard.getCreditCardType())) {
      vMeCard = defaultCard;
    }
    // if vme returned card details are present in profile assign it to creditcard object
    if (vMeCard == null) {
      vMeCard = profile.getCreditCardByNumber(vMeInfoVO.getPaymentDetails().getAccountNumber());
    }
    try {
      if (vMeCard == null) {
        newVMeCard = true;
        final NMCommerceProfileTools profileTools = CheckoutComponents.getCommerceProfileTools();
        vMeCard = new NMCreditCard();
        vMeCard.setRepositoryItem(profileTools.createCreditCardItem(profile));
        
      }
      if (vMeCard.getBillingAddressItem() == null) {
        final RepositoryItem billingAddress = profileRepo.createItem(ProfileProperties.Profile_Desc_contactInfo);
        vMeCard.setPropertyValue(ProfileProperties.CreditCard_billingAddress, billingAddress);
      }
    } catch (final RepositoryException e) {
      throw new VmeException("Unable to process Visa checkout due to issues while updating the vme billing address: " + e);
    }
    // assign the vme returned card details and billing address details to credit card object
    final PaymentDetailsVO paymentDetails = vMeInfoVO.getPaymentDetails();
    vMeCard.setCreditCardType(vMeInfoVO.getCardNameMap().get(paymentDetails.getBrand()));
    vMeCard.setCreditCardNumber(paymentDetails.getAccountNumber());
    final NMCreditCard paymentGroup = order.getCreditCards().iterator().next();
    paymentGroup.setCheckoutType(CommonComponentHelper.getNMCheckoutTypeUtil().getCheckoutTypeRepositoryItem(VME));
    paymentGroup.setVmeCard(true);
    vMeCard.setExpirationMonth(paymentDetails.getExpMonth());
    vMeCard.setExpirationYear(paymentDetails.getExpYear());
    paymentGroup.setRiskIndicator(paymentDetails.getRiskData());
    paymentGroup.setVmeCallId(paymentDetails.getCallId());
    final NMAddress billingAddress = new NMAddress((MutableRepositoryItem) vMeCard.getBillingAddressItem());
    final String existingPhoneNumber = billingAddress.getPhoneNumber();
    final String billingCountry = paymentDetails.getBillingCountryCode();
    String billingFullName = paymentDetails.getBillingCardHolderName();
    String lastName = null;
    String firstName = null;
    boolean missingRequiredName = true;
    // Extract fistName and lastName from Billing Full Name.
    if (billingFullName != null) {
      billingFullName = billingFullName.trim();
      final String[] firstLast = billingFullName.split(VmeConstants.SPACE_SEPARATOR);
      if (firstLast.length == 2) {
        firstName = vMe.sanitizeFieldLength(firstLast[0], Integer.parseInt(vmeFieldLengthMap.get(VmeConstants.VME_NAME_FLD_LENGTH)));
        lastName = vMe.sanitizeFieldLength(firstLast[1], Integer.parseInt(vmeFieldLengthMap.get(VmeConstants.VME_NAME_FLD_LENGTH)));
        missingRequiredName = false;
      } else if (firstLast.length > 2) {
        final int firstSpaceIndex = billingFullName.indexOf(VmeConstants.SPACE_SEPARATOR);
        final int secondSpaceIndex = billingFullName.indexOf(VmeConstants.SPACE_SEPARATOR, firstSpaceIndex + 1);
        final String first = billingFullName.substring(0, secondSpaceIndex);
        final String last = billingFullName.substring(secondSpaceIndex + 1);
        firstName = vMe.sanitizeFieldLength(first, Integer.parseInt(vmeFieldLengthMap.get(VmeConstants.VME_NAME_FLD_LENGTH)));
        lastName = vMe.sanitizeFieldLength(last, Integer.parseInt(vmeFieldLengthMap.get(VmeConstants.VME_NAME_FLD_LENGTH)));
        missingRequiredName = false;
      }
    }
    // Assign extracted fistname and lastname from full name.
    if (!missingRequiredName) {
      billingAddress.setFirstName(firstName);
      billingAddress.setLastName(lastName);
    } else {
      // First name or lastname or both are missing. Throw error.
      throw new VmeException(vMe.getVmeResponseErrorMap().get(VMe.NAME_ERR_MSG));
    }
    
    billingAddress.setCountry(billingCountry);
    billingAddress.setAddress1(vMe.sanitizeFieldLength(paymentDetails.getBillingAddressLine1(), Integer.parseInt(vmeFieldLengthMap.get(VmeConstants.VME_ADDR_FLD_LENGTH))));
    billingAddress.setAddress2(vMe.sanitizeFieldLength(paymentDetails.getBillingAddressLine2(), Integer.parseInt(vmeFieldLengthMap.get(VmeConstants.VME_ADDR_FLD_LENGTH))));
    billingAddress.setCity(vMe.sanitizeFieldLength(paymentDetails.getBillingCity(), Integer.parseInt(vmeFieldLengthMap.get(VmeConstants.VME_CITY_STATE_FLD_LENGTH))));
    setStateOrProvince(billingCountry, vMe.sanitizeFieldLength(paymentDetails.getBillingStateProvinceCode(), Integer.parseInt(vmeFieldLengthMap.get(VmeConstants.VME_CITY_STATE_FLD_LENGTH))),
            billingAddress);
    billingAddress.setPostalCode(vMe.sanitizeFieldLength(paymentDetails.getBillingPostalCode(), Integer.parseInt(vmeFieldLengthMap.get(VmeConstants.VME_POSTAL_FLD_LENGTH))));
    String vmePhone = null;
    if (null != vMeInfoVO.getShippingInfo()) {
      vmePhone = vMeInfoVO.getShippingInfo().getPhone();
    }
    billingAddress.setPhoneNumber(StringUtilities.isEmpty(vmePhone) ? existingPhoneNumber : vMe.sanitizeFieldLength(vmePhone,
            Integer.parseInt(vmeFieldLengthMap.get(VmeConstants.VME_PHONE_FLD_LENGTH))));
    billingAddress.setVerificationFlag(VME_ORDER);
    profile.setBillingAddress(billingAddress.getRepositoryItem());
    // adding the new credit card object to profile
    if (newVMeCard) {
      profile.addCreditCard(vMeCard);
    }
    try {
      profile.changeDefaultCreditCard(vMeCard);
      // updating the order object with new credit card object
      CheckoutAPI.changeCreditCardOnOrder(order, orderMgr, profile, vMeCard);
    } catch (final RepositoryException e) {
      throw new VmeException("Unable to process Visa checkout due to issues while updating profile default card: " + e);
    } catch (final Exception e) {
      throw new VmeException("Unable to process Visa checkout due to issues while updating order credit card: " + e);
    }
  }
  
  /**
   * This method returns true if the vme adress is same as profileshipping address else returns false
   * 
   * @param cart
   * @param profile
   * @return
   * @throws Exception
   */
  @SuppressWarnings("unchecked")
  public boolean isVmeShippingAddrChanged(final NMOrderImpl order) {
    final VMeInfoVO vMeInfoVO = CheckoutComponents.getVMeInfoVO(ServletUtil.getCurrentRequest());
    final ShippingDetailsVO shippingDetailsVo = vMeInfoVO.getShippingInfo();
    boolean isAddrChanged = false;
    if (null != shippingDetailsVo) {
      final String fullName = shippingDetailsVo.getName();
      final List<String> firstAndLastNameList = splitVmeFirstandLastName(fullName);
      final String vMeNickname =
              AddressNickname.createNickname(firstAndLastNameList.get(0), firstAndLastNameList.get(1), shippingDetailsVo.getAddressLine1(), shippingDetailsVo.getCity(),
                      shippingDetailsVo.getStateProvinceCode(), shippingDetailsVo.getPostalCode(), shippingDetailsVo.getCountryCode());
      if (!StringUtilities.isEmpty(vMeNickname)) {
        final List<ShippingGroup> shippingGroups = order.getShippingGroups();
        for (final ShippingGroup shippingGroup : CheckoutAPI.getNonWishlistShippingGroups(shippingGroups)) {
          final ContactInfo addr = CheckoutAPI.orderAddressToContactInfo(((HardgoodShippingGroup) shippingGroup).getRepositoryItem());
          final String sgNickname = AddressNickname.createNickname(addr.getFirstName(), addr.getLastName(), addr.getAddress1(), addr.getCity(), addr.getState(), addr.getZip(), addr.getCountry());
          isAddrChanged = !sgNickname.equalsIgnoreCase(vMeNickname);
          if (isAddrChanged) {
            break;
          }
        }
      }
    }
    return isAddrChanged;
  }
  
  /**
   * This method gets the full name from shipping details VO and separates it into first name and last name. If there are two names then first and last name are split based on first space else more
   * than two names then second space is considered as separator
   * 
   * @param fullName
   * @return
   * @throws Exception
   */
  public List<String> splitVmeFirstandLastName(final String fullName) {
    String lastName = null;
    String firstName = null;
    if (fullName != null) {
      final String[] firstLast = fullName.split(VmeConstants.SPACE_SEPARATOR);
      if (firstLast.length == 2) {
        firstName = vMe.sanitizeFieldLength(firstLast[0], Integer.parseInt(vMe.getvmeFieldLengthMap().get(VmeConstants.VME_NAME_FLD_LENGTH)));
        lastName = vMe.sanitizeFieldLength(firstLast[1], Integer.parseInt(vMe.getvmeFieldLengthMap().get(VmeConstants.VME_NAME_FLD_LENGTH)));
      } else {
        final int firstSpaceIndex = fullName.indexOf(VmeConstants.SPACE_SEPARATOR);
        final int secondSpaceIndex = fullName.indexOf(VmeConstants.SPACE_SEPARATOR, firstSpaceIndex + 1);
        final String first = fullName.substring(0, secondSpaceIndex);
        final String last = fullName.substring(secondSpaceIndex + 1);
        firstName = vMe.sanitizeFieldLength(first, Integer.parseInt(vMe.getvmeFieldLengthMap().get(VmeConstants.VME_NAME_FLD_LENGTH)));
        lastName = vMe.sanitizeFieldLength(last, Integer.parseInt(vMe.getvmeFieldLengthMap().get(VmeConstants.VME_NAME_FLD_LENGTH)));
      }
    }
    final List<String> firstAndLastNameList = new ArrayList<String>();
    firstAndLastNameList.add(firstName);
    firstAndLastNameList.add(lastName);
    return firstAndLastNameList;
  }
  
  private void setStateOrProvince(final String country, final String stateOrProvince, final NMAddress address) {
    if (!localizationUtils.isSupportedByFiftyOne(country)) {
      address.setState(stateOrProvince);
      address.setProvince(null);
    } else {
      address.setState(null);
      address.setProvince(stateOrProvince);
    }
  }
  
  /**
   * This method decrypts the response from visa checkout and saves the decrypted data into VO to use it further.
   * 
   * @param encKey
   * @param purchaseData
   * @param callId
   */
  public Collection<Message> setVmeCheckout(final String encKey, final String purchaseData, final String callId) throws VmeException {
    Collection<Message> messages = new ArrayList<Message>();
    Map<String, Object> vmeData = new HashMap<String, Object>();
    try {
      String decryptedData = null;
      JSONObject jsonObj = null;
      JSONObject userInfoObj = null;
      JSONObject paymentInstrumentObj = null;
      JSONObject shippingAddressObj = null;
      JSONObject riskDataObj = null;
      decryptedData = vMe.getEncryptionManager().decrypt(encKey, purchaseData, vMe.getVmeTag());
      jsonObj = new JSONObject(decryptedData);
      userInfoObj = jsonObj.optJSONObject(VmeConstants.USER_DATA);
      paymentInstrumentObj = jsonObj.optJSONObject(VmeConstants.PAYMENT_INSTRUMENT);
      shippingAddressObj = jsonObj.optJSONObject(VmeConstants.SHIPPING_ADDRESS);
      riskDataObj = jsonObj.optJSONObject(VmeConstants.RISK_DATA);
      messages = vMe.validateVmeResponseData(userInfoObj, paymentInstrumentObj, shippingAddressObj);
      if (!messages.isEmpty()) {
        return messages;
      } else {
        vmeData = vMe.constructVmeInfoFromResponse(userInfoObj, paymentInstrumentObj, shippingAddressObj, riskDataObj, callId);
      }
    } catch (final JSONException e) {
      throw new VmeException("Unable to process Visa checkout due to issues while parsing vme response : " + e);
    } catch (final EncryptionException ene) {
      throw new VmeException("Unable to process Visa checkout due to issues while decrypting : " + ene);
    } catch (final Exception e) {
      throw new VmeException("Unable to process Visa checkout due to issues : " + e);
    }
    final VMeInfoVO vMeInfoVO = CheckoutComponents.getVMeInfoVO(ServletUtil.getCurrentRequest());
    if (messages.isEmpty()) {
      vMeInfoVO.setPaymentDetails((PaymentDetailsVO) vmeData.get(VmeConstants.VME_PAY_DETAILS));
      vMeInfoVO.setShippingInfo((ShippingDetailsVO) vmeData.get(VmeConstants.VME_SHIP_INFO));
    }
    return messages;
  }
  
  /**
   * This method clears the risk data , vme call id and sets isVmeCard attribute to false
   * 
   * @param order
   */
  public void clearVmeDetails(final NMOrderImpl order) {
    if ((order.getCreditCards() != null) && !order.getCreditCards().isEmpty()) {
      final NMCreditCard paymentGroup = order.getCreditCards().iterator().next();
      if ((paymentGroup != null) && paymentGroup.isVmeCard()) {
        paymentGroup.setVmeCard(false);
        paymentGroup.setRiskIndicator("");
        paymentGroup.setVmeCallId("");
        paymentGroup.setCheckoutType(null);
      }
    }
  }
  
  /**
   * This method resets the risk data , vme call id from VMeInfoVO session if it existi and sets isVmeCard attribute to true
   * 
   * @param order
   */
  public void resetBackVmeDetails(final VMeInfoVO vMeInfoVO, final NMOrderImpl order) {
    final PaymentDetailsVO paymentDetails = vMeInfoVO.getPaymentDetails();
    final NMCreditCard paymentGroup = order.getCreditCards().iterator().next();
    if ((paymentDetails != null) && (paymentGroup != null)) {
      paymentGroup.setVmeCard(true);
      paymentGroup.setRiskIndicator(paymentDetails.getRiskData());
      paymentGroup.setVmeCallId(paymentDetails.getCallId());
      paymentGroup.setCreditCardType(vMeInfoVO.getCardNameMap().get(paymentDetails.getBrand()));
      paymentGroup.setCheckoutType(CommonComponentHelper.getNMCheckoutTypeUtil().getCheckoutTypeRepositoryItem(VME));
    }
  }
  
  /**
   * This method returns the discount amount resulted by VisaCheckout.
   * 
   * @param order
   *          Order object
   * @return discount amount resulted by VisaCheckout
   * */
  public double getVisaCheckoutDiscountAmountOnOrder(final NMOrderImpl order) {
    double discount = 0.0;
    final List<NMCommerceItem> items = order.getNmCommerceItems();
    if ((items != null) && !items.isEmpty()) {
      final PromotionsHelper promoHelper = CheckoutComponents.getPromotionsHelper();
      // Loop through all items and sum the all visa chekout only markdowns.
      for (final NMCommerceItem item : items) {
        final Map<String, Markdown> markdownMap = item.getItemMarkdowns();
        if ((markdownMap != null) && !markdownMap.isEmpty()) {
          final Collection<Markdown> markdowns = markdownMap.values();
          for (final Markdown markdown : markdowns) {
            final NMPromotion promotion = promoHelper.getPromotionFromPromoId(markdown.getPromoKey());
            if (VME.equalsIgnoreCase(promotion.getKey()) && CHECKOUT.equalsIgnoreCase(promotion.getKeyType())) {
              discount += markdown.getDollarDiscount();
            }
          }
        }
      }
      // Check if we have any shipping promotions.
      if (order.hasShippingPromotion() || order.hasShippingUpgradePromotion()) {
        boolean foundVmeShippingPromo = false;
        try {
          final List<com.nm.collections.Promotion> promos = promoHelper.getShippingPromotions(order, null);
          if ((promos != null) && !promos.isEmpty()) {
            for (final NMPromotion promo : promos) {
              if (VME.equalsIgnoreCase(promo.getKey()) && CHECKOUT.equalsIgnoreCase(promo.getKeyType())) {
                foundVmeShippingPromo = true;
                break;
              }
            }
          }
        } catch (final ServletException e) {
          CommonComponentHelper.getLogger().logDebug("Visa Checkout Promotion issue:" + e);
        } catch (final IOException e) {
          CommonComponentHelper.getLogger().logDebug("Visa Checkout Promotion issue:" + e);
        }
        // VisaCheckout Shipping promotion found. Send the promotional shipping cost.
        if (foundVmeShippingPromo) {
          final double promotionalShipping = order.getLostShipping();
          discount += promotionalShipping;
        }
      }
    }
    return discount;
  }
}
