package com.nm.commerce.checkout;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import atg.commerce.CommerceException;
import atg.commerce.gifts.InvalidDateException;
import atg.commerce.order.Order;
import atg.commerce.order.PaymentGroup;
import atg.commerce.order.PaymentGroupManager;
import atg.core.util.Address;
import atg.core.util.StringUtils;
import atg.payment.creditcard.CreditCardTools;
import atg.payment.creditcard.GenericCreditCardInfo;
import atg.repository.MutableRepository;
import atg.repository.MutableRepositoryItem;
import atg.repository.RepositoryException;
import atg.repository.RepositoryItem;
import atg.servlet.ServletUtil;
import atg.userprofiling.Profile;

import com.nm.ajax.checkoutb.beans.FinCenRequest;
import com.nm.ajax.checkoutb.beans.FinCenResponse;
import com.nm.collections.CreditCard;
import com.nm.collections.CreditCardArray;
import com.nm.commerce.CreditCardValidator;
import com.nm.commerce.GiftCard;
import com.nm.commerce.GiftCardHolder;
import com.nm.commerce.NMCommerceItem;
import com.nm.commerce.NMCommerceProfileTools;
import com.nm.commerce.NMCreditCard;
import com.nm.commerce.NMProcCreditCardModCheck;
import com.nm.commerce.NMProfile;
import com.nm.commerce.checkout.beans.PaymentInfo;
import com.nm.commerce.order.NMOrderManager;
import com.nm.commerce.order.NMPaymentGroupManager;
import com.nm.commerce.pricing.NMItemPriceInfo;
import com.nm.commerce.promotion.NMOrderImpl;
import com.nm.components.CommonComponentHelper;
import com.nm.formhandler.NMProfileFormHandler;
import com.nm.formhandler.ShoppingCartHandler;
import com.nm.international.fiftyone.checkoutapi.BorderFreeConstants;
import com.nm.profile.ProfileProperties;
import com.nm.sitemessages.AddressMessageFields;
import com.nm.sitemessages.Message;
import com.nm.sitemessages.MessageDefs;
import com.nm.utils.LocalizationUtils;
import com.nm.utils.StringUtilities;
import com.nm.utils.SystemSpecs;

/**
 * Package access - outside of this package, methods should be accessed through CheckoutAPI.
 */
/* package */class PaymentUtil {
  
  public static final String REPRICE_GIFT_CARDS = "GIFT_CARD";
  public static final String REPRICE_PREPAID_CARDS = "PREPAID_CARD";
  public static final String REPRICE_GIFT_AND_PREPAID_CARDS = "GIFT_AND_PREPAID_CARD";
  
  public static final int MAX_CREDIT_CARD_NAME_LENGTH = 50;
  private final LocalizationUtils localizationUtils = CheckoutComponents.getLocalizationUtils();
  private static PaymentUtil INSTANCE; // avoid static initialization
  
  // private constructor enforces singleton behavior
  private PaymentUtil() {}
  
  public static synchronized PaymentUtil getInstance() {
    INSTANCE = (INSTANCE == null) ? new PaymentUtil() : INSTANCE;
    return INSTANCE;
  }
  
  /**
   * Sets the email and login profile properties with the email address provided.
   * 
   * @param profile
   *          - the profile on which the email will be updated
   * @param emailAddr
   *          - the email address that will be set on the profile
   */
  public void updateProfileEmailAddress(final Profile profile, final String emailAddr) {
    if (null != emailAddr) {
      if (((NMProfile) profile).isAnonymous()) {
        // If the profile is anonymous and the email is being set for the first time
        // set receiveEmail to 1
        String currentEmail = (String) profile.getPropertyValue(ProfileProperties.Profile_email);
        if ((emailAddr.length() > 0) && ((currentEmail == null) || currentEmail.equals(""))) {
          profile.setPropertyValue(ProfileProperties.Profile_receiveEmail, "yes");
        }
        
      } else {
        profile.setPropertyValue(ProfileProperties.Profile_login, emailAddr.toUpperCase());
      }
      profile.setPropertyValue(ProfileProperties.Profile_email, emailAddr.toLowerCase());
    }
    
  }
  
  public void removePaymentGroup(final NMOrderImpl order, final NMOrderManager orderMgr, final String paymentGroupId) throws CommerceException {
    NMPaymentGroupManager paymentGroupMgr = (NMPaymentGroupManager) orderMgr.getPaymentGroupManager();
    
    synchronized (order) {
      paymentGroupMgr.removePaymentGroupFromOrder(order, paymentGroupId);
      updatePaymentGroups(REPRICE_GIFT_AND_PREPAID_CARDS, order, orderMgr);
      orderMgr.updateOrder(order);
    }
  }
  
  public void removeAllPaymentGroups(final NMOrderImpl order, final NMOrderManager orderMgr) throws CommerceException {
    NMPaymentGroupManager paymentGroupMgr = (NMPaymentGroupManager) orderMgr.getPaymentGroupManager();
    
    synchronized (order) {
      paymentGroupMgr.removeAllPaymentGroupsFromOrder(order);
      updatePaymentGroups(REPRICE_GIFT_AND_PREPAID_CARDS, order, orderMgr);
      orderMgr.updateOrder(order);
    }
  }
  
  public void updatePaymentGroups(final String repriceOrder, final NMOrderImpl order, final NMOrderManager orderManager) throws CommerceException {
    orderManager.updateOrder(order);
    
    if (repriceOrder != null) {
      if (repriceOrder.equals(REPRICE_GIFT_AND_PREPAID_CARDS)) {
        NMPaymentGroupManager paymentGroupManager = (NMPaymentGroupManager) orderManager.getPaymentGroupManager();
        // System.out.println("order price info before clear: " + order.debugPriceInfos());
        paymentGroupManager.clearPaymentGroupAmounts(order, new int[] {NMPaymentGroupManager.ALL_CARDS});
        // System.out.println("order price info after clear: " +
        // order.debugPriceInfos());
        List<NMCreditCard> paymentGroups = paymentGroupManager.getPaymentGroups(order, new int[] {NMPaymentGroupManager.GIFT_CARDS});
        applyAmountToNonCcPaymentGroups(paymentGroups, order, orderManager, paymentGroupManager);
        // System.out.println("order price info after apply: " + order.debugPriceInfos());
        paymentGroups = paymentGroupManager.getPaymentGroups(order, new int[] {NMPaymentGroupManager.PREPAID_CARDS});
        applyAmountToNonCcPaymentGroups(paymentGroups, order, orderManager, paymentGroupManager);
        
      } else if (repriceOrder.equals(REPRICE_GIFT_CARDS)) {
        NMPaymentGroupManager paymentGroupManager = (NMPaymentGroupManager) orderManager.getPaymentGroupManager();
        paymentGroupManager.clearPaymentGroupAmounts(order, new int[] {NMPaymentGroupManager.ALL_CARDS});
        List<NMCreditCard> paymentGroups = paymentGroupManager.getPaymentGroups(order, new int[] {NMPaymentGroupManager.GIFT_CARDS});
        applyAmountToNonCcPaymentGroups(paymentGroups, order, orderManager, paymentGroupManager);
        
      } else if (repriceOrder.equals(REPRICE_PREPAID_CARDS)) {
        NMPaymentGroupManager paymentGroupManager = (NMPaymentGroupManager) orderManager.getPaymentGroupManager();
        paymentGroupManager.clearPaymentGroupAmounts(order, new int[] {NMPaymentGroupManager.ALL_CARDS});
        List<NMCreditCard> paymentGroups = paymentGroupManager.getPaymentGroups(order, new int[] {NMPaymentGroupManager.PREPAID_CARDS});
        applyAmountToNonCcPaymentGroups(paymentGroups, order, orderManager, paymentGroupManager);
      }
    }
  }
  
  public void updatePaymentGroupAddresses(ShoppingCartHandler cart, NMProfile profile) throws Exception {
    final RepositoryItem billingAddr = OrderUtil.getInstance().getAddressFromKey(cart, profile, ProfileProperties.Profile_billingAddressName);
    final NMOrderImpl order = cart.getNMOrder();
    // MasterPass Changes : Skip the below step for MasterPass as the billing address need not be stored to NM Profile
    if (!order.isMasterPassPayGroupOnOrder()) {
      if (billingAddr != null) {
        @SuppressWarnings("unchecked")
        List<PaymentGroup> paymentGroups = order.getPaymentGroups();
        Iterator<PaymentGroup> iterator = paymentGroups.iterator();
        while (iterator.hasNext()) {
          NMCreditCard paymentGroup = (NMCreditCard) iterator.next();
          copyAddressToPaymentGroup(billingAddr, paymentGroup);
        }
      }
    }
  }
  
  private void applyAmountToNonCcPaymentGroups(List<NMCreditCard> paymentGroups, NMOrderImpl order, NMOrderManager orderManager, PaymentGroupManager paymentGroupManager) throws CommerceException {
    if (paymentGroups.size() > 0) {
      NMCreditCard paymentGroup;
      Iterator<NMCreditCard> paymentGroupIterator = paymentGroups.iterator();
      double orderAmountRemaining = OrderUtil.getInstance().getCurrentOrderAmountRemaining(order, orderManager);
      
      if (orderAmountRemaining < 0.0d) {
        Collections.reverse(paymentGroups);
        
        while ((OrderUtil.getInstance().getCurrentOrderAmountRemaining(order, orderManager) < 0.0d) && paymentGroupIterator.hasNext()) {
          paymentGroup = paymentGroupIterator.next();
          double giftCardAmount = 0;
          orderAmountRemaining = OrderUtil.getInstance().getCurrentOrderAmountRemaining(order, orderManager);
          
          giftCardAmount = paymentGroup.getAmountRemaining() + paymentGroup.getAmount();
          if (-orderAmountRemaining >= paymentGroup.getAmount()) {
            paymentGroupManager.removePaymentGroupFromOrder(order, paymentGroup.getId());
          } else {
            orderManager.removeOrderAmountFromPaymentGroup(order, paymentGroup.getId(), (long) (paymentGroup.getAmount() + 1));
            orderManager.addOrderAmountToPaymentGroup(order, paymentGroup.getId(), giftCardAmount);
            paymentGroupManager.recalculatePaymentGroupAmounts(order);
            paymentGroup.setAmountRemaining(giftCardAmount - paymentGroup.getAmount());
            orderManager.updateOrder(order);
          }
        }
      } else {
        while ((OrderUtil.getInstance().getCurrentOrderAmountRemaining(order, orderManager) > 0.0d) && paymentGroupIterator.hasNext()) {
          paymentGroup = paymentGroupIterator.next();
          if (paymentGroup.getAmountRemaining() > 0.0d) {
            double giftCardAmount = 0;
            giftCardAmount = paymentGroup.getAmountRemaining() + paymentGroup.getAmount();
            
            orderManager.removeOrderAmountFromPaymentGroup(order, paymentGroup.getId(), (long) (giftCardAmount + 1));
            orderManager.addOrderAmountToPaymentGroup(order, paymentGroup.getId(), giftCardAmount);
            paymentGroupManager.recalculatePaymentGroupAmounts(order);
            paymentGroup.setAmountRemaining(giftCardAmount - paymentGroup.getAmount());
            orderManager.updateOrder(order);
          }
        }
      }
    }
  }
  
  public double getSubTotalPriceOnGiftCardProductItems(NMOrderImpl order) {
    double subtotal = 0;
    for (NMCommerceItem nmci : order.getNmCommerceItems()) {
      String prodName = nmci.getCmosProdName();
      if (isGiftCard(prodName) && (nmci.getPriceInfo() != null)) {
        NMItemPriceInfo priceInfo = (NMItemPriceInfo) nmci.getPriceInfo();
        if (priceInfo.getPromotionalPrice() > 0.0) {
          subtotal += priceInfo.getPromotionalPrice();
        } else {
          subtotal += nmci.getPriceInfo().getRawTotalPrice();
        }
      }
    }
    
    return subtotal;
  }
  
  private boolean isGiftCard(String prodName) {
    boolean result = false;
    if ((prodName != null) && (prodName.toLowerCase().contains("gift")) && (prodName.toLowerCase().contains("card"))) {
      result = true;
    }
    return result;
  }
  
  public void addDefaultPaymentGroup(final NMOrderImpl order, final Profile profile, final NMOrderManager orderManager) {
    NMPaymentGroupManager paymentGroupManager = (NMPaymentGroupManager) orderManager.getPaymentGroupManager();
    Collection<NMCreditCard> creditCards = paymentGroupManager.getPaymentGroups(order, new int[] {NMPaymentGroupManager.CREDIT_CARDS});
    
    if (!creditCards.isEmpty()) {
      NMCreditCard creditCard = creditCards.iterator().next();
      final RepositoryItem cc = (RepositoryItem) profile.getPropertyValue(ProfileProperties.Profile_defaultCreditCard);
      
      if ((cc != null) && StringUtilities.isEmpty(creditCard.getCreditCardNumber())) {
        final String ccType = (String) cc.getPropertyValue(ProfileProperties.CreditCard_creditCardType);
        final String ccNumber = NMCreditCard.decryptCreditCardNumber((String) cc.getPropertyValue(ProfileProperties.CreditCard_creditCardNumber));
        final String ccExpMonth = NMCreditCard.decryptCreditCardNumber((String) cc.getPropertyValue(ProfileProperties.CreditCard_expirationMonth));
        final String ccExpYear = NMCreditCard.decryptCreditCardNumber((String) cc.getPropertyValue(ProfileProperties.CreditCard_expirationYear));
        creditCard.setCreditCardType(ccType);
        creditCard.setCreditCardNumber(ccNumber);
        creditCard.setExpirationMonth(ccExpMonth);
        creditCard.setExpirationYear(ccExpYear);
      }
    }
  }
  
  public Collection<Message> applyGiftCard(NMOrderImpl order, NMOrderManager orderMgr, GiftCardHolder giftCardHolder, String giftCardNumber, String giftCardCin) throws CommerceException {
    List<Message> messages = new ArrayList<Message>();
    
    if (StringUtilities.isEmpty(giftCardNumber)) {
      Message msg = MessageDefs.getMessage(MessageDefs.MSG_GenericFormErrorMessage);
      msg.setMsgId("missingCardNumber");
      msg.setMsgText("Please provide an entry for the Gift Card Number field.");
      messages.add(msg);
    }
    
    if (StringUtilities.isEmpty(giftCardCin)) {
      Message msg = MessageDefs.getMessage(MessageDefs.MSG_GenericFormErrorMessage);
      msg.setMsgId("missingCinNumber");
      msg.setMsgText("Please provide an entry for the CIN # field.");
      messages.add(msg);
    }
    
    if (messages.isEmpty() && !isGiftCardOnOrder(order, giftCardNumber)) {
      giftCardHolder.setGiftCardPageType(GiftCardHolder.DYNAMIC_INQUIRY);
      giftCardHolder.reset();
      
      GiftCard giftCard = giftCardHolder.getGiftCardListItem(0);
      giftCard.setCardNumber(StringUtilities.trimSpaces(giftCardNumber));
      giftCard.setCid(StringUtilities.trimSpaces(giftCardCin));
      
      final int addGiftCardResult = giftCardHolder.addGiftCardPaymentGroupsToOrder(order);
      updatePaymentGroups(REPRICE_GIFT_AND_PREPAID_CARDS, order, orderMgr);
      
      messages.addAll(getGiftCardErrorMessages(giftCardHolder, addGiftCardResult));
      if (addGiftCardResult == GiftCardHolder.GIFT_CARD_SYSTEM_UNAVAILABLE) {
        CheckoutComponents.getConfig().logError("Error " + addGiftCardResult + " adding giftcards to order");
      }
      
      orderMgr.updateOrder(order);
    }
    
    return messages;
  }
  
  private List<Message> getGiftCardErrorMessages(GiftCardHolder giftCardHolder, final int addGiftCardResult) {
    List<Message> messages = new ArrayList<Message>();
    SystemSpecs systemSpecs = CommonComponentHelper.getSystemSpecs();
    String ecareNumber = systemSpecs.getEcarePhoneNumber();
    
    Message msg = null;
    
    for (int i = 0; i < giftCardHolder.getMaxNumGiftCards(); ++i) {
      final GiftCard giftCard = giftCardHolder.getGiftCardListItem(i);
      String cardNumber = giftCard.getCardNumber();
      String cardCin = giftCard.getCid();
      String cardName = giftCard.getIsMerchCreditGiftCard() ? "merchandise credit gift card" : "gift card";
      
      if (!(StringUtilities.isEmpty(cardNumber) && StringUtilities.isEmpty(cardCin))) {
        final int transStatus = giftCard.getTransactionStatus();
        switch (transStatus) {
          case GiftCard.OK:
          break;
          
          case -1: // Error code for web invalid mod check
          case GiftCard.AMOUNT_OVER_BALANCE:
          case GiftCard.CANCELED:
          case GiftCard.NM_MERCH_ONLY:
          case GiftCard.ALREADY_ON_FILE:
          case GiftCard.NOT_EQUAL_TO_INIT_VALUE:
          case GiftCard.BALANCE_NOT_TRUE:
          case GiftCard.LOST_OR_STOLEN:
          case GiftCard.NOT_ADDING_VALUE:
          case GiftCard.REFUND_ONLY:
          case GiftCard.NOT_BEEN_SOLD:
            if (StringUtilities.isEmpty(giftCard.getCardNumber()) && !StringUtilities.isEmpty(giftCard.getCid())) {
              msg = MessageDefs.getMessage(MessageDefs.MSG_Missing_GiftCard);
            } else if (!StringUtilities.isEmpty(giftCard.getCardNumber()) && StringUtilities.isEmpty(giftCard.getCid())) {
              msg = MessageDefs.getMessage(MessageDefs.MSG_Missing_GiftCard_CIN);
            } else {
              msg = MessageDefs.getMessage(MessageDefs.MSG_Invalid_GiftCard);
              msg.setMsgText(String.format("This %s is invalid or requires additional assistance. Please try again. "
                      + "For additional assistance, please contact a customer care associate toll-free at %s.", cardName, ecareNumber));
            }
          break;
          
          case GiftCard.EXPIRED:
            msg = MessageDefs.getMessage(MessageDefs.MSG_Expired_GiftCard);
          break;
          
          case GiftCard.INVALID_CIN:
            msg = MessageDefs.getMessage(MessageDefs.MSG_Invalid_GiftCard_CIN);
            msg.setMsgText(String.format("The CIN number entered is invalid. Please try again." + "For additional assistance please contact a customer care associate toll-free at %s.", ecareNumber));
          break;
          
          case GiftCard.DIV_04_ONLY:
            msg = MessageDefs.getMessage(MessageDefs.MSG_GiftCard_ValidOnlyOn_NM);
          break;
          
          case GiftCard.DIV_05_ONLY:
            msg = MessageDefs.getMessage(MessageDefs.MSG_GiftCard_Valid_BG);
          break;
          
          case GiftCard.INVALID_INCIRCLE_CARD:
            msg = MessageDefs.getMessage(MessageDefs.MSG_Invalid_InCircleGiftCard);
          break;
          
          case GiftCard.CARD_NOT_ON_FILE:
          case GiftCard.INVALID_TRANSACTION_TYPE:
          case GiftCard.HOST_COMMUNICATION_ERROR:
          case GiftCard.FILES_NOT_OPEN:
          case GiftCard.BAD_FILE:
            msg = MessageDefs.getMessage(MessageDefs.MSG_GiftCard_Unavailable);
            msg.setMsgText(String.format("Our apologies.  Online gift card services are temporarily unavailable. "
                    + "Please try again, or call a customer service representative at %s for assistance.", ecareNumber));
          break;
          
          default:
            msg = MessageDefs.getMessage(MessageDefs.MSG_GiftCard_Unavailable);
            msg.setMsgText(String.format("Our apologies.  Online gift card services are temporarily unavailable. "
                    + "Please try again, or call a customer service representative at %s for assistance.", ecareNumber));
          break;
        }
        
        if (null != msg) {
          msg.setFieldId(msg.getFieldId() + i);
          messages.add(msg);
        }
      }
    }
    
    if ((addGiftCardResult != GiftCardHolder.NO_ERRORS) && messages.isEmpty()) {
      switch (addGiftCardResult) {
      
        case GiftCardHolder.ALL_CARDS_ARE_ZERO:
          msg = MessageDefs.getMessage(MessageDefs.MSG_All_GiftCards_Zero_Value);
          msg.setFieldId(msg.getFieldId() + "0");
          messages.add(msg);
        break;
        
        case GiftCardHolder.PROFILE_ID_NOT_FOUND:
          msg = MessageDefs.getMessage(MessageDefs.MSG_GiftCard_Profile_Not_Found);
          msg.setFieldId(msg.getFieldId() + "0");
          messages.add(msg);
        break;
        
        case GiftCardHolder.GIFT_CARD_PURCHASE:
          msg = MessageDefs.getMessage(MessageDefs.MSG_GiftCard_Purchase_GiftCard);
          msg.setFieldId(msg.getFieldId() + "0");
          messages.add(msg);
        break;
        
        case GiftCardHolder.GIFT_CARD_SYSTEM_UNAVAILABLE:
          msg = MessageDefs.getMessage(MessageDefs.MSG_GiftCard_Unavailable);
          msg.setFieldId(msg.getFieldId() + "0");
          msg.setMsgText(String.format("Our apologies.  Online gift card services are temporarily unavailable. " + "Please try again, or call a customer service representative at %s for assistance.",
                  ecareNumber));
          messages.add(msg);
        break;
      }
    }
    
    return messages;
  }
  
  public NMCreditCard getGiftCardOnOrder(final NMOrderImpl order, final String giftCardNumber) {
    List<NMCreditCard> giftCards = order.getGiftCards();
    for (NMCreditCard giftCard : giftCards) {
      if (giftCard.getCreditCardNumber().equals(giftCardNumber)) {
        return giftCard;
      }
    }
    return null;
  }
  
  public boolean isGiftCardOnOrder(final NMOrderImpl order, final String giftCardNumber) {
    return getGiftCardOnOrder(order, giftCardNumber) != null;
  }
  
  /**
   * Copied the method from the com.nm.ajax.checkout.payment.PaymentUtils class
   * 
   * @author nmve1
   * @param orderMgr
   * @param order
   * @return the amount remaining on order
   */
  public String calcAmountRemainingOnOrder(final NMOrderManager orderMgr, final Order order) {
    
    final double amtRemaining = orderMgr.getCurrentOrderAmountRemaining(order);
    final String strAmtRemaining = ((long) ((amtRemaining + 0.005) * 100.0)) + "";
    return (strAmtRemaining);
  }
  
  /**
   * Validated paymentInfo is updated with cleaned fields. If you are using this for validationOnly, be sure to use the paymentInfo you pass in afterwards.
   */
  public Collection<Message> addCreditCardToProfile(NMOrderImpl order, NMProfile profile, NMProfileFormHandler profileHandler, PaymentInfo paymentInfo, List<PaymentInfo> newCreditCards,
          List<PaymentInfo> removedCreditCards) throws RepositoryException {
    List<Message> messages = new ArrayList<Message>();
    Map<String, String> newCardsMap = new HashMap<String, String>();
    Set<String> removedIds = new HashSet<String>();
    
    messages.addAll(validatePaymentInfo(order, profile, paymentInfo));
    boolean isInternational = CheckoutAPI.isInternationalSession(profile);
    if (!paymentInfo.isSkipBillingAddressValidation()) {
      paymentInfo.setSkipEmailValidation(true);
      messages.addAll(AddressUtil.getInstance().validateAddress(paymentInfo, profileHandler, AddressMessageFields.KEY_BILLINGADDRESS, AddressUtil.BILLING_ADDRESS, isInternational));
    }
    
    if (newCreditCards != null) {
      for (PaymentInfo newCard : newCreditCards) {
        newCardsMap.put(newCard.getCardNumber(), newCard.getPaymentId());
      }
    }
    
    if (removedCreditCards != null) {
      for (PaymentInfo removedCard : removedCreditCards) {
        removedIds.add(removedCard.getPaymentId());
      }
    }
    
    NMCreditCard existingCreditCard = profile.getCreditCardByNumber(paymentInfo.getCardNumber());
    String existingMsg = null;
    
    if ((existingCreditCard != null) && !removedIds.contains(existingCreditCard.getRepositoryId())) {
      existingMsg = "A credit card with this number already exists.";
    } else if (newCardsMap.containsKey(paymentInfo.getCardNumber()) && !removedIds.contains(newCardsMap.get(paymentInfo.getCardNumber()))) {
      existingMsg = "A new credit card with this number already exists.";
    }
    
    if (existingMsg != null) {
      Message msg = MessageDefs.getMessage(MessageDefs.MSG_GenericFormErrorMessage);
      msg.setMsgText(existingMsg);
      msg.setMsgIntlText(existingMsg);
      msg.setFieldId("paytypeCardNumber");
      msg.setMsgId("existingCardConflict");
      
      messages.add(msg);
    }
    
    if (!paymentInfo.isValidationOnly() && messages.isEmpty()) {
      MutableRepository profileRepo = (MutableRepository) profile.getRepository();
      NMCommerceProfileTools profileTools = CheckoutComponents.getCommerceProfileTools();
      NMCreditCard creditCard = new NMCreditCard();
      MutableRepositoryItem billingAddress = null;
      boolean willBeDefault = profile.getCreditCards().isEmpty();
      
      if (willBeDefault) {
        billingAddress = profile.getBillingAddress();
      }
      
      if (billingAddress == null) {
        billingAddress = profileRepo.createItem(ProfileProperties.Profile_Desc_contactInfo);
        
        if (willBeDefault) {
          profile.setBillingAddress(billingAddress);
        }
      }
      
      creditCard.setRepositoryItem(profileTools.createCreditCardItem(profile));
      creditCard.setPropertyValue(ProfileProperties.CreditCard_billingAddress, billingAddress);
      copyFieldsToCreditCard(paymentInfo, creditCard.getRepositoryItem());
      profile.addCreditCard(creditCard);
    }
    
    return messages;
  }
  
  public Collection<Message> updateDefaultCreditCard(NMOrderImpl order, NMProfile profile, NMProfileFormHandler profileHandler, PaymentInfo paymentInfo) throws RepositoryException {
    List<Message> messages = new ArrayList<Message>();
    
    messages.addAll(validatePaymentInfo(order, profile, paymentInfo));
    
    if (!paymentInfo.isSkipBillingAddressValidation()) {
      paymentInfo.setSkipEmailValidation(true);
      messages.addAll(AddressUtil.getInstance().validateAddress(paymentInfo, profileHandler, AddressMessageFields.KEY_BILLINGADDRESS, AddressUtil.BILLING_ADDRESS));
    }
    
    if (!paymentInfo.isValidationOnly() && messages.isEmpty()) {
      MutableRepository profileRepo = (MutableRepository) profile.getRepository();
      NMCommerceProfileTools profileTools = CheckoutComponents.getCommerceProfileTools();
      NMCreditCard creditCard = profile.getDefaultCreditCard();
      
      if (creditCard == null) {
        creditCard = new NMCreditCard();
        creditCard.setRepositoryItem(profileTools.createCreditCardItem(profile));
        profile.addCreditCard(creditCard);
      }
      
      if (creditCard.getPropertyValue(ProfileProperties.CreditCard_billingAddress) == null) {
        creditCard.setPropertyValue(ProfileProperties.CreditCard_billingAddress, profileRepo.createItem(ProfileProperties.Profile_Desc_contactInfo));
      }
      
      copyFieldsToCreditCard(paymentInfo, creditCard.getRepositoryItem());
    }
    
    return messages;
  }
  
  public Collection<Message> addOrUpdateDefaultInternationalCreditCard(NMOrderImpl order, NMProfile profile, NMProfileFormHandler profileHandler, PaymentInfo paymentInfo) throws RepositoryException {
    
    NMCreditCard oldDefaultCard = profile.getDefaultCreditCard();
    List<Message> messages = new ArrayList<Message>();
    // Check if default card is available. If default card is available, then change default card to new card.
    if ((oldDefaultCard != null) && !StringUtils.isBlank(oldDefaultCard.getCreditCardNumberEncrypted())
            && !oldDefaultCard.getCreditCardNumber().equals(trimEmpty(cleanCardNumber(paymentInfo.getCardNumber())))) {
      
      messages.addAll(validatePaymentInfo(order, profile, paymentInfo));
      
      if (!paymentInfo.isSkipBillingAddressValidation()) {
        paymentInfo.setSkipEmailValidation(true);
        messages.addAll(AddressUtil.getInstance().validateAddress(paymentInfo, profileHandler, AddressMessageFields.KEY_BILLINGADDRESS, AddressUtil.BILLING_ADDRESS));
      }
      if (!paymentInfo.isValidationOnly() && messages.isEmpty()) {
        MutableRepository profileRepo = (MutableRepository) profile.getRepository();
        NMCommerceProfileTools profileTools = CheckoutComponents.getCommerceProfileTools();
        NMCreditCard suppliedCard = profile.getCreditCardByNumber(paymentInfo.getCardNumber());
        // Check new card is already in the list.
        if (suppliedCard == null) {
          suppliedCard = new NMCreditCard();
          suppliedCard.setRepositoryItem(profileTools.createCreditCardItem(profile));
          profile.addCreditCard(suppliedCard);
        }
        suppliedCard.setCreditCardNumber(paymentInfo.getCardNumber());
        suppliedCard.setCreditCardType(paymentInfo.getCardType());
        suppliedCard.setExpirationMonth(paymentInfo.getExpMonth());
        suppliedCard.setExpirationYear(paymentInfo.getExpYear());
        if (suppliedCard.getPropertyValue(ProfileProperties.CreditCard_billingAddress) == null) {
          suppliedCard.setPropertyValue(ProfileProperties.CreditCard_billingAddress, profileRepo.createItem(ProfileProperties.Profile_Desc_contactInfo));
        } else {
          suppliedCard.setPropertyValue(ProfileProperties.CreditCard_billingAddress, profile.getBillingAddress());
        }
        profile.setDefaultCreditCard(suppliedCard.getRepositoryItem());
        
        Map<String, MutableRepositoryItem> cards = profile.getCreditCards();
        if (cards != null) {
          // Remove the default credit card references.
          cards.remove(ProfileProperties.Profile_defaultCreditCard);
          cards.remove(suppliedCard.getRepositoryId());
          // Set new card as profile default.
          cards.put(ProfileProperties.Profile_defaultCreditCard, suppliedCard.getRepositoryItem());
          // Make old card as one of the card.
          cards.put(oldDefaultCard.getRepositoryId(), oldDefaultCard.getRepositoryItem());
        }
        copyFieldsToCreditCard(paymentInfo, suppliedCard.getRepositoryItem());
      }
    } else {
      // No default card available. Follow the existing logic.
      return updateDefaultCreditCard(order, profile, profileHandler, paymentInfo);
    }
    
    return messages;
  }
  
  public Collection<Message> removeCreditCardFromProfile(NMOrderImpl order, NMOrderManager orderMgr, NMProfile profile, NMCreditCard creditCard) throws Exception {
    List<Message> messages = new ArrayList<Message>();
    List<NMCreditCard> orderCreditCards = order.getCreditCards();
    
    if (messages.isEmpty()) {
      String removedPaymentId = null;
      
      if (orderCreditCards.contains(creditCard)) {
        removedPaymentId = orderCreditCards.get(orderCreditCards.indexOf(creditCard)).getPaymentId();
      }
      
      profile.removeCreditCard(creditCard);
      
      if (removedPaymentId != null) {
        NMCreditCard defaultCreditCard = profile.getDefaultCreditCard();
        if ((defaultCreditCard != null) && !order.getCreditCards().contains(defaultCreditCard)) {
          NMCreditCard paymentGroup = (NMCreditCard) order.getPaymentGroup(removedPaymentId);
          copyCreditCardToPaymentGroup(defaultCreditCard, paymentGroup);
        } else {
          removePaymentGroup(order, orderMgr, removedPaymentId);
        }
      }
    }
    
    return messages;
  }
  
  public Collection<Message> savePaymentChanges(NMOrderImpl order, NMProfile profile, NMProfileFormHandler profileHandler, String newDefaultCardName, List<PaymentInfo> newCreditCards,
          List<PaymentInfo> removedCreditCards) throws Exception {
    List<Message> messages = new ArrayList<Message>();
    Map<String, String> newCardsMap = new HashMap<String, String>();
    Set<String> removedCardIds = new HashSet<String>();
    NMCreditCard newDefaultCard = profile.getCreditCardMap().get(newDefaultCardName);
    
    if ((newCreditCards != null) && (newDefaultCard == null)) {
      for (PaymentInfo newCard : newCreditCards) {
        newCardsMap.put(newCard.getPaymentId(), newCard.getCardNumber());
      }
    }
    
    if (removedCreditCards != null) {
      for (PaymentInfo removedCard : removedCreditCards) {
        removedCardIds.add(removedCard.getPaymentId());
      }
    }
    
    if (((newDefaultCard == null) && !newCardsMap.containsKey(newDefaultCardName)) || ((newDefaultCard != null) && removedCardIds.contains(newDefaultCard.getRepositoryId()))) {
      Message msg = MessageDefs.getMessage(MessageDefs.MSG_GenericFormErrorMessage);
      String missingCreditCard = "No credit card found for with name " + newDefaultCardName;
      msg.setMsgText(missingCreditCard);
      // change starts setting error message for internatinal user
      msg.setMsgIntlText(missingCreditCard);
      // change ends setting error message for internatinal user
      msg.setFieldId("groupTop");
      msg.setError(false);
      messages.add(msg);
      
      return messages;
    }
    
    if (removedCreditCards != null) {
      for (PaymentInfo removedCard : removedCreditCards) {
        removedCard.setValidationOnly(false);
        profile.removeCreditCard(profile.getCreditCardById(removedCard.getPaymentId()));
      }
    }
    
    if (newCreditCards != null) {
      for (PaymentInfo newCard : newCreditCards) {
        newCard.setValidationOnly(false);
        newCard.setSkipSecCodeValidation(true);
        newCard.setSkipEmailValidation(true);
        addCreditCardToProfile(order, profile, profileHandler, newCard, null, null);
      }
    }
    
    if (newDefaultCard == null) {
      newDefaultCard = profile.getCreditCardByNumber(newCardsMap.get(newDefaultCardName));
    }
    
    profile.changeDefaultCreditCard(newDefaultCard);
    
    return messages;
  }
  
  public Collection<Message> changeCreditCardOnOrder(NMOrderImpl order, NMOrderManager orderMgr, RepositoryItem billingAddress, PaymentInfo paymentInfo) throws Exception {
    List<Message> messages = new ArrayList<Message>();
    NMPaymentGroupManager paymentGroupMgr = (NMPaymentGroupManager) orderMgr.getPaymentGroupManager();
    List<NMCreditCard> orderCreditCards = order.getCreditCards();
    NMProfile profile = CheckoutComponents.getProfile(ServletUtil.getCurrentRequest());
    boolean isInternational = CheckoutAPI.isInternationalSession(profile);
    NMCreditCard paymentGroup;
    boolean newPaymentGroup = false;
    
    if (orderCreditCards.isEmpty()) {
      paymentGroup = (NMCreditCard) paymentGroupMgr.createPaymentGroup();
      newPaymentGroup = true;
    } else {
      // assumes only one credit per order
      paymentGroup = orderCreditCards.get(0);
    }
    
    paymentGroup.setCreditCardNumber(paymentInfo.getCardNumber());
    paymentGroup.setCreditCardType(paymentInfo.getCardType());
    paymentGroup.setExpirationMonth(paymentInfo.getExpMonth());
    paymentGroup.setExpirationYear(paymentInfo.getExpYear());
    paymentGroup.setPayPalEmail(paymentInfo.getPayPalEmail());
    paymentGroup.setPayPalPayerId(paymentInfo.getPayPalPayerId());
    paymentGroup.setPayPalBillingAgreementId(paymentInfo.getPayPalBillingAgreementId());
    paymentGroup.setCardVerficationNumber(paymentInfo.getCardSecCode());
    // Defect fix:21603 Omniture: setting the amount
    if (isInternational) {
      if (paymentGroup.getAmount() == 0) {
        final String ccType = paymentGroup.getCreditCardType();
        if (!StringUtils.isBlank(ccType) && ccType.equalsIgnoreCase(BorderFreeConstants.CUP_TYPE)) {
          paymentGroup.setAmount((Double) order.getInternationalPriceItem().getPropertyValue("totalPrice"));
        } else {
          paymentGroup.setAmount(Double.parseDouble(localizationUtils.getUSDConvertedCurrency(profile.getCountryPreference(), profile.getCurrencyPreference(), (Double) order
                  .getInternationalPriceItem().getPropertyValue("totalPrice"))));
          
        }
        
      }
    }
    
    if (billingAddress != null) {
      copyAddressToPaymentGroup(billingAddress, paymentGroup);
    }
    
    if (newPaymentGroup) {
      paymentGroupMgr.addPaymentGroupToOrder(order, paymentGroup);
      orderMgr.addOrderAmountToPaymentGroup(order, paymentGroup.getId(), 0.0);
      orderMgr.updateOrder(order);
    }
    
    return messages;
  }
  
  public Collection<Message> changeCreditCardOnOrder(NMOrderImpl order, NMOrderManager orderMgr, NMProfile profile, NMCreditCard creditCard) throws Exception {
    List<Message> messages = new ArrayList<Message>();
    NMPaymentGroupManager paymentGroupMgr = (NMPaymentGroupManager) orderMgr.getPaymentGroupManager();
    List<NMCreditCard> orderCreditCards = order.getCreditCards();
    
    boolean newPaymentGroup = false;
    NMCreditCard paymentGroup;
    RepositoryItem billingAddress = (RepositoryItem) creditCard.getPropertyValue(ProfileProperties.CreditCard_billingAddress);
    
    if (orderCreditCards.isEmpty()) {
      paymentGroup = (NMCreditCard) paymentGroupMgr.createPaymentGroup();
      newPaymentGroup = true;
    } else {
      // assumes only one credit per order
      paymentGroup = orderCreditCards.get(0);
    }
    
    if ((billingAddress == null) || !hasRequiredBillingAddressProperties(billingAddress)) {
      creditCard.setPropertyValue(ProfileProperties.CreditCard_billingAddress, profile.getBillingAddress());
    } else {
      profile.setBillingAddress(billingAddress);
    }
    
    copyCreditCardToPaymentGroup(creditCard, paymentGroup);
    
    if (newPaymentGroup) {
      paymentGroupMgr.addPaymentGroupToOrder(order, paymentGroup);
      orderMgr.addOrderAmountToPaymentGroup(order, paymentGroup.getId(), 0.0);
      orderMgr.updateOrder(order);
    }
    
    return messages;
  }
  
  /**
   * Clears out payment group 0 if it has a zero value for an order so that CMOS does inadvertently charge a credit card instead of a gift card since we cannot communicate how much to charge on each
   * particular card (gift vs credit).
   */
  public void clearOutPaymentGroups(final NMOrderImpl order) {
    synchronized (order) {
      @SuppressWarnings("unchecked")
      List<NMCreditCard> paymentGroupList = order.getPaymentGroups();
      Iterator<NMCreditCard> iterator = paymentGroupList.iterator();
      
      while (iterator.hasNext()) {
        NMCreditCard paymentGroup = iterator.next();
        if ((paymentGroup != null) && (paymentGroup.isCreditCard() || paymentGroup.isPrepaidCard()) && (paymentGroup.getAmount() == 0.0d)) {
          paymentGroup.setCreditCardNumber("");
          paymentGroup.setCreditCardType("");
          paymentGroup.setCid("");
          paymentGroup.setExpirationMonth("");
          paymentGroup.setExpirationYear("");
        }
      }
    }
  }
  
  public Collection<Message> validatePaymentInfo(NMOrderImpl order, NMProfile profile, PaymentInfo paymentInfo) {
    if ((null != profile.getCountryPreference()) && !profile.getCountryPreference().equalsIgnoreCase("US")) {
      return validatePaymentInfo(order, profile, paymentInfo, true);
    } else {
      return validatePaymentInfo(order, profile, paymentInfo, false);
    }
  }
  
  public Collection<Message> validatePaymentInfo(NMOrderImpl order, NMProfile profile, PaymentInfo paymentInfo, boolean isInternationalOrder) {
    List<Message> messages = new ArrayList<Message>();
    CreditCardArray creditCardArray = CheckoutComponents.getCreditCardArray();
    CreditCard cardType = creditCardArray.getCardTypeByCode(paymentInfo.getCardTypeCode());
    
    if (cardType == null) {
      cardType = creditCardArray.getCardTypeByLongName(paymentInfo.getCardType());
    }
    
    if (StringUtilities.isEmpty(paymentInfo.getCardTypeCode()) && StringUtilities.isEmpty(paymentInfo.getCardType())) {
      Message msg = MessageDefs.getMessage(MessageDefs.MSG_GenericFormErrorMessage);
      msg.setMsgId("missingCardType");
      msg.setFieldId("paytypeCardType");
      String missingCardType = "Please provide an entry for the Card Type field.";
      msg.setMsgText(missingCardType);
      // setting error message for international order
      msg.setMsgIntlText(missingCardType);
      messages.add(msg);
    } else if (cardType == null) {
      Message msg = MessageDefs.getMessage(MessageDefs.MSG_GenericFormErrorMessage);
      msg.setMsgId("invalidCardTypeCode");
      msg.setFieldId("paytypeCardType");
      String invalidCardType = "The card type %s is invalid.";
      msg.setMsgText(String.format(invalidCardType, paymentInfo.getCardTypeCode()));
      // setting error message for international order
      msg.setMsgIntlText(String.format(invalidCardType, paymentInfo.getCardTypeCode()));
      messages.add(msg);
    }
    
    if (!messages.isEmpty()) {
      return messages;
    }
    
    String number = trimEmpty(cleanCardNumber(paymentInfo.getCardNumber()));
    String secCode = trimEmpty(paymentInfo.getCardSecCode());
    String type = cardType.getLongName();
    String expMonth = trimEmpty(paymentInfo.getExpMonth());
    String expYear = trimEmpty(paymentInfo.getExpYear());
    
    NMCreditCard profileCard = profile.getCreditCardByNumber(number);
    boolean cidAuthorized = profileCard != null ? profileCard.getCidAuthorized() : false;
    
    if (StringUtilities.isEmpty(number) && cardType.isCardNumReq()) {
      Message msg = MessageDefs.getMessage(MessageDefs.MSG_GenericFormErrorMessage);
      msg.setMsgId("missingCardNumber");
      msg.setFieldId("paytypeCardNumber");
      String missingCardNumber = "Please provide an entry for the Card Number field.";
      msg.setMsgText(missingCardNumber);
      // setting error message for international order
      msg.setMsgIntlText(missingCardNumber);
      messages.add(msg);
    }
    
    if (!paymentInfo.isSkipSecCodeValidation() && cardType.isSecCodeReq() && !cidAuthorized && StringUtilities.isEmpty(secCode)) {
      Message msg = MessageDefs.getMessage(MessageDefs.MSG_GenericFormErrorMessage);
      msg.setMsgId("missingCardSecCode");
      msg.setFieldId("paytypeCardSecCode");
      String missingCardSecurityCode = "Please provide an entry for the Card Security Code field.";
      msg.setMsgText(missingCardSecurityCode);
      // setting error message for international order
      msg.setMsgIntlText(missingCardSecurityCode);
      messages.add(msg);
    }
    
    if (StringUtilities.isEmpty(type)) {
      Message msg = MessageDefs.getMessage(MessageDefs.MSG_GenericFormErrorMessage);
      msg.setMsgId("missingCardType");
      msg.setFieldId("paytypeCardType");
      String cardTypeInvalidEntryErrorMessage = "Please provide a valid entry for the Card Type field.";
      msg.setMsgText(cardTypeInvalidEntryErrorMessage);
      // setting error message for international order
      msg.setMsgIntlText(cardTypeInvalidEntryErrorMessage);
      messages.add(msg);
    }
    
    if (cardType.isExpDateReq() && StringUtilities.isEmpty(expMonth)) {
      Message msg = MessageDefs.getMessage(MessageDefs.MSG_GenericFormErrorMessage);
      msg.setMsgId("missingExpMonth");
      msg.setFieldId("paytypeExpMonth");
      String missingExpirationMonth = "Please provide an entry for the Expiration Month field.";
      msg.setMsgText(missingExpirationMonth);
      // setting error message for international order
      msg.setMsgIntlText(missingExpirationMonth);
      messages.add(msg);
    }
    
    if (cardType.isExpDateReq() && StringUtilities.isEmpty(expYear)) {
      Message msg = MessageDefs.getMessage(MessageDefs.MSG_GenericFormErrorMessage);
      msg.setMsgId("missingExpYear");
      msg.setFieldId("paytypeExpYear");
      String missingExpirationYear = "Please provide an entry for the Expiration Year field.";
      msg.setMsgText(missingExpirationYear);
      // setting error message for international user
      msg.setMsgIntlText(missingExpirationYear);
      messages.add(msg);
    }
    
    // empty fields: skip rest of validation
    if (!messages.isEmpty()) {
      return messages;
    }
    
    if (!paymentInfo.isSkipSecCodeValidation() && cardType.isSecCodeReq() && !cidAuthorized) {
      if ((secCode.length() < 3) || (secCode.length() > 5)) {
        Message msg = MessageDefs.getMessage(MessageDefs.MSG_InvalidCreditCardCid);
        String invalidSecurityCode = "The security code entry is not valid.  Please re-enter the Card Security Code.";
        msg.setMsgText(invalidSecurityCode);
        // setting error message for international user
        msg.setMsgIntlText(invalidSecurityCode);
        msg.setFieldId("paytypeSecurityCode");
        messages.add(msg);
      } else {
        char[] cidChars = secCode.toCharArray();
        for (int i = 0; i < cidChars.length; i++) {
          char c = cidChars[i];
          if (!Character.isDigit(c)) {
            Message msg = MessageDefs.getMessage(MessageDefs.MSG_InvalidCreditCardCidNumeric);
            String securitCodeNumberCheckError = "The Security Code field should contain only numbers.";
            msg.setMsgText(securitCodeNumberCheckError);
            // setting error message for international user
            msg.setMsgIntlText(securitCodeNumberCheckError);
            msg.setFieldId("paytypeSecurityCode");
            messages.add(msg);
            break;
          }
        }
      }
    }
    
    if (cardType.isExpDateReq()) {
      try {
        int month = Integer.parseInt(expMonth);
        int year = Integer.parseInt(expYear);
        if ((month >= 1) && (month <= 12)) {
          Calendar expDate = Calendar.getInstance();
          expDate.set(year, month, 1);
          CreditCardValidator.validateExpirationDate(expDate);
        } else {
          throw new InvalidDateException("The month supplied is out of range.");
        }
        
        // ensure zero padding
        expMonth = String.format("%02d", month);
        
      } catch (NumberFormatException e) {
        Message msg = MessageDefs.getMessage(MessageDefs.MSG_InvalidCreditCardExpDate);
        msg.setFieldId("paytypeExpDate");
        messages.add(msg);
      } catch (InvalidDateException e) {
        Message msg = MessageDefs.getMessage(MessageDefs.MSG_InvalidCreditCardExpDate);
        msg.setFieldId("paytypeExpDate");
        msg.setMsgText(e.getMessage());
        msg.setMsgIntlText(e.getMessage());
        messages.add(msg);
      }
    }
    
    if (messages.isEmpty() && cardType.isCardNumReq()) {
      int validateResult;
      
      order.setPropertyValue("validateTenderType", true);
      order.setPropertyValue("ccAuthCode", null);
      
      GenericCreditCardInfo creditCardInfo = new GenericCreditCardInfo();
      creditCardInfo.setCreditCardNumber(number);
      creditCardInfo.setCreditCardType(type);
      creditCardInfo.setExpirationMonth(expMonth);
      creditCardInfo.setExpirationYear(expYear);
      /* if (!isInternationalOrder) { */
      if (cardType.isSpecialValidation()) {
        NMProcCreditCardModCheck procCreditCardModCheck = new NMProcCreditCardModCheck();
        validateResult = procCreditCardModCheck.validateSpecialCardUnencrypted(creditCardInfo);
      } else {
        validateResult = CreditCardTools.verifyCreditCard(creditCardInfo);
      }
      
      if (validateResult != CreditCardTools.SUCCESS) {
        order.incrementFailedCCAttemptCount();
        
        if (!order.hasReachedFailedCCAttemptLimit()) {
          Message msg = MessageDefs.getMessage(MessageDefs.MSG_InvalidCreditCard);
          msg.setFrgName(null);
          msg.setFieldId("groupAddPaytype");
          msg.setMsgText(CreditCardTools.getStatusCodeMessage(validateResult));
          messages.add(msg);
        }
      }
      /* } */
    }
    
    if (messages.isEmpty()) {
      // use cleaned values
      paymentInfo.setCardNumber(number);
      paymentInfo.setCardSecCode(secCode);
      paymentInfo.setCardType(type);
      paymentInfo.setExpMonth(cardType.isExpDateReq() ? expMonth : "");
      paymentInfo.setExpYear(cardType.isExpDateReq() ? expYear : "");
    }
    
    return messages;
  }
  
  public String trimEmpty(String val) {
    return val != null ? val.trim() : "";
  }
  
  public String cleanCardNumber(String cardNumber) {
    if (cardNumber == null) {
      return null;
    }
    
    return stripCharacters(cardNumber.trim(), "().- ");
  }
  
  public String stripCharacters(String value, String chars) {
    StringBuilder stripped = new StringBuilder();
    
    if ((value != null) && (value.trim().length() > 0)) {
      StringTokenizer st = new StringTokenizer(value, chars);
      
      while (st.hasMoreTokens()) {
        stripped.append(st.nextToken());
      }
    }
    
    return stripped.toString();
  }
  
  public PaymentInfo creditCardToPaymentInfo(NMCreditCard creditCard, RepositoryItem defaultBilling) throws RepositoryException {
    PaymentInfo paymentInfo = new PaymentInfo();
    boolean profileCardItem = creditCard.getRepositoryItem().getItemDescriptor().getItemDescriptorName().equals("credit-card");
    CreditCardArray creditCardArray = CheckoutComponents.getCreditCardArray();
    CreditCard cardType = creditCardArray.getCardTypeByLongName(creditCard.getCreditCardTypeDisplay());
    RepositoryItem billingAddress = null;
    
    if (profileCardItem) {
      billingAddress = (RepositoryItem) creditCard.getPropertyValue(ProfileProperties.CreditCard_billingAddress);
    } else {
      paymentInfo.setCardSecCode(creditCard.getCidTransient());
    }
    
    if (billingAddress == null) {
      billingAddress = defaultBilling;
    }
    
    if (billingAddress != null) {
      paymentInfo.copyFrom(AddressUtil.getInstance().profileAddressToContactInfo(billingAddress));
    }
    
    paymentInfo.setCardNumber(creditCard.getCreditCardNumber());
    paymentInfo.setCardType(cardType != null ? cardType.getLongName() : "");
    paymentInfo.setCardTypeCode(cardType != null ? cardType.getCode() : "");
    paymentInfo.setExpMonth(creditCard.getExpirationMonth());
    paymentInfo.setExpYear(creditCard.getExpirationYear());
    paymentInfo.setPayPalEmail(creditCard.getPayPalEmail());
    paymentInfo.setPayPalPayerId(creditCard.getPayPalPayerId());
    paymentInfo.setPayPalBillingAgreementId(creditCard.getPayPalBillingAgreementId());
    
    return paymentInfo;
  }
  
  public void copyFieldsToCreditCard(PaymentInfo paymentInfo, MutableRepositoryItem creditCard) {
    NMCreditCard nmCreditCard = new NMCreditCard();
    nmCreditCard.setRepositoryItem(creditCard);
    nmCreditCard.setCreditCardNumber(paymentInfo.getCardNumber());
    nmCreditCard.setCreditCardType(paymentInfo.getCardType());
    nmCreditCard.setExpirationMonth(paymentInfo.getExpMonth());
    nmCreditCard.setExpirationYear(paymentInfo.getExpYear());
    nmCreditCard.setPayPalEmail(paymentInfo.getPayPalEmail());
    nmCreditCard.setPayPalPayerId(paymentInfo.getPayPalPayerId());
    nmCreditCard.setPayPalBillingAgreementId(paymentInfo.getPayPalBillingAgreementId());
    
    MutableRepositoryItem billingAddress = (MutableRepositoryItem) creditCard.getPropertyValue(ProfileProperties.CreditCard_billingAddress);
    if (billingAddress != null) {
      AddressUtil.getInstance().copyFieldsToProfileAddress(paymentInfo, billingAddress);
    }
  }
  
  public void copyCreditCardToPaymentGroup(NMCreditCard creditCard, NMCreditCard paymentGroup) {
    paymentGroup.setCreditCardNumber(creditCard.getCreditCardNumber());
    paymentGroup.setCreditCardType(creditCard.getCreditCardType());
    paymentGroup.setExpirationMonth(creditCard.getExpirationMonth());
    paymentGroup.setExpirationYear(creditCard.getExpirationYear());
    paymentGroup.setCidAuthorized(creditCard.getCidAuthorized());
    paymentGroup.setPayPalEmail(creditCard.getPayPalEmail());
    paymentGroup.setPayPalPayerId(creditCard.getPayPalPayerId());
    paymentGroup.setPayPalBillingAgreementId(creditCard.getPayPalBillingAgreementId());
    
    RepositoryItem billingAddress = (RepositoryItem) creditCard.getPropertyValue(ProfileProperties.CreditCard_billingAddress);
    copyAddressToPaymentGroup(billingAddress, paymentGroup);
  }
  
  public void copyAddressToPaymentGroup(RepositoryItem address, NMCreditCard paymentGroup) {
    if (address == null) {
      return;
    }
    
    Address originalAddress = paymentGroup.getBillingAddress();
    
    if (originalAddress == null) {
      originalAddress = new Address();
    }
    
    originalAddress.setAddress1((String) address.getPropertyValue(ProfileProperties.Contact_address1));
    originalAddress.setAddress2((String) address.getPropertyValue(ProfileProperties.Contact_address2));
    originalAddress.setAddress3((String) address.getPropertyValue(ProfileProperties.Contact_address2));
    originalAddress.setCity((String) address.getPropertyValue(ProfileProperties.Contact_city));
    originalAddress.setCountry((String) address.getPropertyValue(ProfileProperties.Contact_country));
    originalAddress.setFirstName((String) address.getPropertyValue(ProfileProperties.Contact_firstName));
    originalAddress.setLastName((String) address.getPropertyValue(ProfileProperties.Contact_lastName));
    originalAddress.setMiddleName((String) address.getPropertyValue(ProfileProperties.Contact_middleName));
    originalAddress.setPostalCode((String) address.getPropertyValue(ProfileProperties.Contact_postalCode));
    originalAddress.setState((String) address.getPropertyValue(ProfileProperties.Contact_state));
    originalAddress.setSuffix((String) address.getPropertyValue(ProfileProperties.Contact_suffixCode));
    originalAddress.setPrefix((String) address.getPropertyValue(ProfileProperties.Contact_titleCode));
    originalAddress.setCounty((String) address.getPropertyValue(ProfileProperties.Contact_county));
    
    paymentGroup.setBillingAddress(originalAddress);
  }
  
  public void validateFinCen(FinCenRequest request, FinCenResponse response) {
    List<Message> messages = new ArrayList<Message>();
    
    // Validate Birth Date
    Message birthDateMessage = null;
    try {
      birthDateMessage = validateBirthDate(request.getBirthDate());
    } catch (ParseException e) {}
    if (null != birthDateMessage) {
      messages.add(birthDateMessage);
    }
    
    // Validate ID Number
    Message idNumberMessage = validateIDNumber(request);
    if (null != idNumberMessage) {
      messages.add(idNumberMessage);
    }
    
    // Add Messages
    if (messages.size() > 0) {
      response.setError(true);
      response.addMessages(messages);
    }
  }
  
  private Message validateBirthDate(String birthDate) throws ParseException {
    Message message = new Message();
    message.setError(true);
    message.setFieldId("groupFinCenErrors");
    
    if (StringUtils.isBlank(birthDate)) {
      message.setMsgText("You must enter the date in this format: MM/DD/YYYY.");
    } else if (birthDate.length() != 10) {
      message.setMsgText("You must enter the date in this format: MM/DD/YYYY.");
    } else if ((birthDate.charAt(2) != '/') || (birthDate.charAt(5) != '/')) {
      message.setMsgText("You must enter the date in this format: MM/DD/YYYY.");
    } else {
      Integer month = Integer.parseInt(birthDate.substring(0, 2));
      Integer day = Integer.parseInt(birthDate.substring(3, 5));
      Integer year = Integer.parseInt(birthDate.substring(6, 10));
      
      Date today = new Date();
      Calendar cal = Calendar.getInstance();
      cal.setTime(new Date());
      Integer thisYear = cal.get(Calendar.YEAR);
      
      if ((year < (thisYear - 1000)) || (year > (thisYear + 1000))) {
        message.setMsgText("The Year Value is invalid.");
      } else if (month <= 0) {
        message.setMsgText("The Month Value is invalid, it cannot be 00.");
      } else if (month > 12) {
        message.setMsgText("The Month Value is invalid, it cannot be bigger than 12.");
      } else if (day <= 0) {
        message.setMsgText("The Day Value is invalid, it cannot be 00.");
      } else if (((month == 4) || (month == 6) || (month == 9) || (month == 11)) && (day > 30)) {
        message.setMsgText("The Day Value is invalid, it is bigger than 30.");
      } else if (month == 2) {
        if (isLeapYear(year)) {
          if (day > 29) {
            message.setMsgText("The Day Value is invalid, it is bigger than 29.");
          }
        } else if (day > 28) {
          message.setMsgText("The Day Value is invalid, it is bigger than 28.");
        }
      } else if (day > 31) {
        message.setMsgText("The Day Value is invalid, it is bigger than 30.");
      } else {
        Date bDate = new SimpleDateFormat("MM/dd/yyyy").parse(birthDate);
        if (today.before(bDate)) {
          message.setMsgText("The date is invalid, it should not be a future date.");
        }
      }
    }
    
    if (StringUtils.isEmpty(message.getMsgText())) {
      return null;
    } else {
      return message;
    }
  }
  
  private boolean isLeapYear(Integer theYear) {
    boolean isLeapYear = false;
    
    if ((theYear % 4) == 0) {
      if ((theYear % 100) != 0) {
        isLeapYear = true;
      } else if ((theYear % 400) == 0) {
        isLeapYear = true;
      }
    }
    
    return isLeapYear;
  }
  
  private Message validateIDNumber(FinCenRequest request) {
    Message message = new Message();
    message.setError(true);
    message.setFieldId("groupFinCenErrors");
    
    if (StringUtils.isEmpty(request.getDriversLicense()) && StringUtils.isEmpty(request.getPassportNumber())) {
      message.setMsgText("You must enter an ID number, either DL# or Passport#.");
    } else if (!StringUtils.isEmpty(request.getDriversLicense()) && !StringUtils.isEmpty(request.getPassportNumber())) {
      message.setMsgText("You must enter only one ID number, either DL# or Passport#.");
    } else if ("US".equals(request.getCountry())) {
      if (StringUtils.isBlank(request.getDriversLicense())) {
        message.setMsgText("Invalid driver license number-it sbould not be only spaces.");
      } else if (request.getDriversLicense().length() > 30) {
        message.setMsgText("Invalid driver license number-it should not be more than 30 characters.");
      } else if (StringUtils.isBlank(request.getState())) {
        message.setMsgText("You must enter a state.");
      } else if (!isValidID(request.getDriversLicense())) {
        message.setMsgText("Invalid driver license number.");
      }
    } else {
      if (StringUtils.isBlank(request.getPassportNumber())) {
        message.setMsgText("Invalid passport number-it sbould not be only spaces.");
      } else if (request.getPassportNumber().length() > 30) {
        message.setMsgText("Invalid passport number-it should not be more than 30 characters.");
      } else if (StringUtils.isBlank(request.getCountry())) {
        message.setMsgText("You must enter a country.");
      } else if (!isValidID(request.getPassportNumber())) {
        message.setMsgText("Invalid passport number.");
      }
    }
    
    if (StringUtils.isEmpty(message.getMsgText())) {
      return null;
    } else {
      return message;
    }
  }
  
  private boolean isValidID(String id) {
    boolean valid = true;
    
    String validChars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_ ";
    
    for (Character c : id.toCharArray()) {
      if (validChars.indexOf(c) == -1) {
        valid = false;
      }
    }
    
    return valid;
  }
  
  private boolean hasRequiredBillingAddressProperties(RepositoryItem address) {
    boolean returnVal = true;
    if (StringUtilities.isEmpty((String) address.getPropertyValue(ProfileProperties.Contact_firstName))
            || StringUtilities.isEmpty((String) address.getPropertyValue(ProfileProperties.Contact_lastName))
            || StringUtilities.isEmpty((String) address.getPropertyValue(ProfileProperties.Contact_address1))
            || StringUtilities.isEmpty((String) address.getPropertyValue(ProfileProperties.Contact_city))
            || (StringUtilities.isEmpty((String) address.getPropertyValue(ProfileProperties.Contact_state)) && StringUtilities.isEmpty((String) address
                    .getPropertyValue(ProfileProperties.Contact_province))) || StringUtilities.isEmpty((String) address.getPropertyValue(ProfileProperties.Contact_country))
            || StringUtilities.isEmpty((String) address.getPropertyValue(ProfileProperties.Contact_postalCode))
            || StringUtilities.isEmpty((String) address.getPropertyValue(ProfileProperties.Contact_phoneNumber))) {
      returnVal = false;
    }
    return returnVal;
  }
  
  public void addDefaultPaymentGroupToOrder(final NMOrderImpl order, final NMOrderManager orderMgr) throws CommerceException {
    
    NMPaymentGroupManager paymentGroupManager = (NMPaymentGroupManager) orderMgr.getPaymentGroupManager();
    order.addPaymentGroup(paymentGroupManager.createPaymentGroup());
  }
  
}
