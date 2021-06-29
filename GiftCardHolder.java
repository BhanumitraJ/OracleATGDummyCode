package com.nm.commerce;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import atg.commerce.order.CreditCard;
import atg.commerce.order.Order;
import atg.commerce.order.OrderManager;
import atg.commerce.order.PaymentGroupImpl;
import atg.nucleus.GenericService;
import atg.repository.RepositoryItem;

import com.nm.commerce.order.NMOrderManager;
import com.nm.commerce.order.NMPaymentGroupManager;
import com.nm.giftcard.client.GiftCardBalanceService;
import com.nm.utils.NMLogger;

/*
 * Any page that use this class MUST set giftCardPageType.
 * 
 * <SETVALUE BEAN="GiftCardHolder.giftCardPageType" VALUE="DYNAMIC_INQUIRY"> Use this when you need the text boxes to be dynamic. If maxCards=5 and 1 card is applied to order, 4 gift cards will be
 * available to set.
 * 
 * <SETVALUE BEAN="GiftCardHolder.giftCardPageType" VALUE="STATIC_INQUIRY"> Use this when you need the text boxes to be static. If maxCards=5, there will always be 5 gift cards available to set.
 */

public class GiftCardHolder extends GenericService {
  
  private int maxNumGiftCards;
  private int maxNumErrorsPerSession;
  private int invalidCardErrorCount = 0;
  private String giftCardPageType = DYNAMIC_INQUIRY;
  private ArrayList giftCardList;
  private ArrayList giftCardTempList;
  private GiftCardBalanceService giftCardBalanceService;
  private NMLogger giftCardLogger;
  private String currentUserIP;
  
  private boolean isApplyButtonClicked = false;
  private boolean isCheckValueButtonClicked = false;
  
  private static final GiftCardAmountComparator GIFT_CARD_AMOUNT_COMPARATOR = new GiftCardAmountComparator();
  public static final String STATIC_INQUIRY = "STATIC_INQUIRY";
  public static final String DYNAMIC_INQUIRY = "DYNAMIC_INQUIRY";
  
  public static final int NO_ERRORS = 0;
  public static final int DUPLICATE_CARD = 10;
  public static final int INVALID_CARD_NUMBER = 20;
  public static final int ALL_CARDS_ARE_ZERO = 30;
  public static final int PROFILE_ID_NOT_FOUND = 40;
  public static final int GIFT_CARD_PURCHASE = 50;
  public static final int GIFT_CARD_SYSTEM_UNAVAILABLE = 60;
  
  public static final String GIFT_CARD_MERCH_TYPE = "6";
  public static final String VIRTUAL_GIFT_CARD_MERCH_TYPE = "7";
  
  private int maxInvalidGiftCardAttempts;
  
  public GiftCardHolder() {}
  
  @Override
  public void doStartService() {
    reset();
  }
  
  public OrderManager getOrderManager() {
    return OrderManager.getOrderManager();
  }
  
  public NMLogger getGiftCardLogger() {
    return giftCardLogger;
  }
  
  public void setGiftCardLogger(NMLogger giftCardLogger) {
    this.giftCardLogger = giftCardLogger;
  }
  
  protected boolean getIsApplyButtonClicked() {
    return isApplyButtonClicked;
  }
  
  protected void setIsApplyButtonClicked(boolean isApplyButtonClicked) {
    this.isApplyButtonClicked = isApplyButtonClicked;
  }
  
  protected boolean getIsCheckValueButtonClicked() {
    return isCheckValueButtonClicked;
  }
  
  protected void setIsCheckValueButtonClicked(boolean isCheckValueButtonClicked) {
    this.isCheckValueButtonClicked = isCheckValueButtonClicked;
  }
  
  public GiftCardBalanceService getGiftCardBalanceService() {
    return giftCardBalanceService;
  }
  
  public void setGiftCardBalanceService(GiftCardBalanceService giftCardBalanceService) {
    this.giftCardBalanceService = giftCardBalanceService;
  }
  
  public int getInvalidCardErrorCount() {
    return invalidCardErrorCount;
  }
  
  public void setInvalidCardErrorCount(int invalidCardErrorCount) {
    this.invalidCardErrorCount = invalidCardErrorCount;
  }
  
  public int getMaxNumErrorsPerSession() {
    return maxNumErrorsPerSession;
  }
  
  public void setMaxNumErrorsPerSession(int maxNumErrorsPerSession) {
    this.maxNumErrorsPerSession = maxNumErrorsPerSession;
  }
  
  public int getMaxNumGiftCards() {
    return maxNumGiftCards;
  }
  
  public void setMaxNumGiftCards(int maxNumGiftCards) {
    this.maxNumGiftCards = maxNumGiftCards;
  }
  
  public String getCurrentUserIP() {
    return currentUserIP;
  }
  
  public void setCurrentUserIP(String currentUserIP) {
    this.currentUserIP = currentUserIP;
  }
  
  public ArrayList getGiftCardTempList() {
    return giftCardTempList;
  }
  
  protected void setGiftCardTempList(ArrayList giftCardTempList) {
    this.giftCardTempList = giftCardTempList;
  }
  
  public synchronized GiftCard getGiftCardListItem(int index) {
    try {
      return (GiftCard) giftCardList.get(index);
    } catch (Exception e) {
      return new GiftCard();
    }
  }
  
  public ArrayList getGiftCardList() {
    return giftCardList;
  }
  
  protected void setGiftCardList(ArrayList giftCardList) {
    this.giftCardList = giftCardList;
  }
  
  public String getGiftCardPageType() {
    return giftCardPageType;
  }
  
  public void setGiftCardPageType(String giftCardPageType) {
    ArrayList tempSwapList;
    if (giftCardPageType.equals(STATIC_INQUIRY) && !getGiftCardPageType().equals(STATIC_INQUIRY)) {
      tempSwapList = getGiftCardTempList();
      setGiftCardTempList(getGiftCardList());
      setGiftCardList(tempSwapList);
    } else if (giftCardPageType.equals(DYNAMIC_INQUIRY) && !getGiftCardPageType().equals(DYNAMIC_INQUIRY)) {
      tempSwapList = getGiftCardList();
      setGiftCardList(getGiftCardTempList());
      setGiftCardTempList(tempSwapList);
    }
    this.giftCardPageType = giftCardPageType;
  }
  
  // if the user attempts too many invalid inquiry attempts during their session this returns true.
  public boolean getIsValidInquiryAttempt() {
    if (getInvalidCardErrorCount() >= getMaxNumErrorsPerSession()) {
      if (getInvalidCardErrorCount() == getMaxNumErrorsPerSession()) {
        getGiftCardLogger().logActivity(getCurrentUserIP(), "GIFT CARD INQUIRY");
        setInvalidCardErrorCount(getInvalidCardErrorCount() + 1);
      }
      return false;
    } else {
      return true;
    }
  }
  
  public boolean getAreAllGiftCardsBlank() {
    Iterator giftCardIterator = getGiftCardList().iterator();
    while (giftCardIterator.hasNext()) {
      if (!((GiftCard) giftCardIterator.next()).getIsBlank()) {
        return false;
      }
    }
    return true;
  }
  
  // returns true if gift cards exist as payment groups
  public boolean getOrderContainsGiftCards() {
    String currentPageType = getGiftCardPageType();
    boolean returnValue;
    
    setGiftCardPageType(DYNAMIC_INQUIRY);
    if (getGiftCardList().size() < getMaxNumGiftCards()) {
      returnValue = true;
    } else {
      returnValue = false;
    }
    setGiftCardPageType(currentPageType);
    return returnValue;
  }
  
  // removes all the current cards on the GiftCardList and adds MAX number of blank gift cards
  // this should also remove any payment groups or relationships with giftCards
  public void reset() {
    setGiftCardList(new ArrayList());
    for (int i = 0; i < getMaxNumGiftCards(); i++) {
      addBlankGiftCard(getGiftCardList());
    }
    
    setGiftCardTempList(new ArrayList());
    for (int i = 0; i < getMaxNumGiftCards(); i++) {
      addBlankGiftCard(getGiftCardTempList());
    }
  }
  
  // clears the contents of all the cards on the giftCardList
  public void setClearCards(String type) {
    int currentSize = getGiftCardList().size();
    
    setGiftCardList(new ArrayList());
    for (int i = 0; i < currentSize; i++) {
      addBlankGiftCard(getGiftCardList());
    }
    
    setGiftCardTempList(new ArrayList());
    for (int i = 0; i < getMaxNumGiftCards(); i++) {
      addBlankGiftCard(getGiftCardTempList());
    }
  }
  
  // true if card is valid
  protected boolean giftCardModCheck(GiftCard giftCard) {
    if (NMProcCreditCardModCheck.getChecksum(giftCard.getCardNumber(), false) == 0 && giftCard.getCid().equals(giftCard.getGeneratedCid())) {
      return true;
    }
    return false;
  }
  
  // validates all the cards in the Gift Card List.
  // Validation is determined by calling the backend giftcard system and whether or not the
  // card as already been used in the order. The profileId is required and is used as the identify for the call
  // to the back-end system.
  // RETURNS: NO_ERRORS, PROFILE_ID_NOT_FOUND, GIFT_CARD_SYSTEM_UNAVAILABLE
  public synchronized int checkGiftCardValues(String profileId) {
    if (getIsCheckValueButtonClicked() == false) {
      try {
        setIsCheckValueButtonClicked(true);
        GiftCard tempGiftCard = null;
        boolean validInquiryAttempt = true;
        ArrayList tempGiftCardList = new ArrayList();
        
        if (profileId == null || profileId.equals("")) {
          return PROFILE_ID_NOT_FOUND;
        }
        
        for (int i = 0; i < getGiftCardList().size(); i++) {
          tempGiftCard = getGiftCardListItem(i);
          // System.out.println("The entered Card Number is : " + tempGiftCard.getCardNumber());
          if (!tempGiftCard.getIsBlank()) {
            String tempGcNumber = tempGiftCard.getCardNumber();
            if (tempGcNumber.startsWith("80") || tempGcNumber.startsWith("81") || tempGcNumber.startsWith("82") || tempGcNumber.startsWith("83")) {
              // System.out.println("Inside the check for incircle cards : " + tempGcNumber);
              validInquiryAttempt = false;
              tempGiftCard.setIsValid(false);
              tempGiftCard.setTransactionStatus(56);
            } else {
              // System.out.println("Outsie the check for incircle cards : " + tempGiftCard.getCardNumber());
              if (giftCardModCheck(tempGiftCard)) {
                // System.out.println("The entered Card Number is : " + tempGiftCard.getCardNumber());
                // System.out.println("inside the ModCheck condition : " + tempGiftCard.getCardNumber());
                tempGiftCard.setAmountAvailable(0.0d);
                tempGiftCard.setIsValid(true);
                tempGiftCardList.add(tempGiftCard);
              } else {
                // System.out.println("inside else loop where transaction is set for -1 : " + tempGiftCard.getCardNumber());
                validInquiryAttempt = false;
                tempGiftCard.setIsValid(false);
                tempGiftCard.setTransactionStatus(-1);
              }
            }
          } else {
            tempGiftCard.setIsValid(true);
          }
        }
        
        if (isLoggingDebug()) {
          logDebug("getGiftCardBalancerService-->" + getGiftCardBalanceService() + "<---");
        }
        // Calling gift card service if atleast one gift card is valid.
        if (tempGiftCardList.size() != 0) {
          try {
            tempGiftCardList = (ArrayList) getGiftCardBalanceService().checkGiftCardBalance(profileId, tempGiftCardList);
          } catch (Exception e) {
            return GIFT_CARD_SYSTEM_UNAVAILABLE;
          }
        }
        // Logic to add the valid cards from the service to the giftcardList as they may be invalid cards in the giftcardList
        List<GiftCard> tempList = new ArrayList<GiftCard>();
        for (int i = 0; i < getGiftCardList().size(); i++) {
          for (int k = 0; k < tempGiftCardList.size(); k++) {
            GiftCard giftCardCurrent = getGiftCardListItem(i);
            GiftCard giftCardFromService = (GiftCard) tempGiftCardList.get(k);
            if (giftCardCurrent.getCardNumber().equals(giftCardFromService.getCardNumber()) && giftCardCurrent.getCid().equals(giftCardFromService.getCid())) {
              if (!tempList.contains(giftCardFromService)) {
                giftCardCurrent.setAmountAvailable(giftCardFromService.getAmountAvailable());
                giftCardCurrent.setTransactionStatus(giftCardFromService.getTransactionStatus());
                tempList.add(giftCardFromService);
              } else {
                getGiftCardList().remove(i);
              }
            }
          }
        }
        
        if (isLoggingDebug()) {
          for (int i = 0; i < getGiftCardList().size(); i++) {
            logDebug("giftcard--" + i + "--->" + getGiftCardListItem(i) + "<-----");
          }
        }
        
        Iterator giftCardIterator = getGiftCardList().iterator();
        while (giftCardIterator.hasNext()) {
          tempGiftCard = (GiftCard) giftCardIterator.next();
          switch (tempGiftCard.getTransactionStatus()) {
            case GiftCard.AMOUNT_OVER_BALANCE:
            case GiftCard.CANCELED:
            case GiftCard.NM_MERCH_ONLY:
            case GiftCard.ALREADY_ON_FILE:
            case GiftCard.NOT_EQUAL_TO_INIT_VALUE:
            case GiftCard.BALANCE_NOT_TRUE:
            case GiftCard.NOT_BEEN_SOLD:
            case GiftCard.LOST_OR_STOLEN:
            case GiftCard.EXPIRED:
            case GiftCard.INVALID_CIN:
            case GiftCard.NOT_ADDING_VALUE:
            case GiftCard.DIV_04_ONLY:
            case GiftCard.DIV_05_ONLY:
            case GiftCard.REFUND_ONLY:
            case GiftCard.INVALID_INCIRCLE_CARD:
            case GiftCard.CARD_NOT_ON_FILE:
              validInquiryAttempt = false;
              tempGiftCard.setIsValid(false);
            break;
            
            case GiftCard.INVALID_TRANSACTION_TYPE:
            case GiftCard.HOST_COMMUNICATION_ERROR:
            case GiftCard.FILES_NOT_OPEN:
            case GiftCard.BAD_FILE:
              return GIFT_CARD_SYSTEM_UNAVAILABLE;
              
            case GiftCard.OK:
            default:
          }
        }
        
        if (validInquiryAttempt) {
          // if( !getGiftCardPageType().equals(STATIC_INQUIRY) ) {
          if (isLoggingDebug()) {
            logDebug("sorting.................");
          }
          sortByGiftCardAmounts(getGiftCardList());
          // }
          return NO_ERRORS;
        } else {
          setInvalidCardErrorCount(getInvalidCardErrorCount() + 1);
          return INVALID_CARD_NUMBER;
        }
      } finally {
        setIsCheckValueButtonClicked(false);
      }
    } else {
      return NO_ERRORS;
    }
  }
  
  // Check if cards are duplicates. The definition of a duplicate is a card that has the exact same card number/cid combination. Two or more cards that
  // exist in either the giftCardList or in a payment group get flagged as a duplicate.
  // RETURNS: DUPLICATE_CARD or NO_ERRORS
  protected int checkForDuplicateGiftCards(Order order) {
    ArrayList paymentGroupClonedList = new ArrayList(order.getPaymentGroups());
    Iterator paymentGroupIterator = paymentGroupClonedList.iterator();;
    Iterator giftCardIterator = getGiftCardList().iterator();
    GiftCard tempGiftCard = null;
    String giftCardListCheckString = "";
    String paymentGroupListCheckString = "";
    int returnValue = NO_ERRORS;
    PaymentGroupImpl paymentGroup;
    
    while (paymentGroupIterator.hasNext()) {
      paymentGroup = (PaymentGroupImpl) paymentGroupIterator.next();
      if (paymentGroup instanceof NMCreditCard && ((NMCreditCard) paymentGroup).getCreditCardNumber() != null && ((NMCreditCard) paymentGroup).getCidTransient() != null) {
        paymentGroupListCheckString += ((NMCreditCard) paymentGroup).getCreditCardNumber().trim() + ((NMCreditCard) paymentGroup).getCidTransient().trim() + "~";
      }
    }
    
    while (giftCardIterator.hasNext()) {
      tempGiftCard = (GiftCard) giftCardIterator.next();
      
      if (!tempGiftCard.getIsBlank()) {
        if (paymentGroupListCheckString.indexOf(tempGiftCard.getCardNumber() + tempGiftCard.getCid()) != -1) {
          tempGiftCard.setIsValid(false);
          returnValue = DUPLICATE_CARD;
        }
        
        if (giftCardListCheckString.indexOf(tempGiftCard.getCardNumber() + tempGiftCard.getCid()) != -1) {
          tempGiftCard.setIsValid(false);
          Iterator giftCardIterator2 = getGiftCardList().iterator(); // another iterator to flag the other card that is a duplicate
          GiftCard tempGiftCard2 = null;
          while (giftCardIterator2.hasNext()) {
            tempGiftCard2 = (GiftCard) giftCardIterator2.next();
            if (tempGiftCard2.getCardNumber().trim().equalsIgnoreCase(tempGiftCard.getCardNumber()) && tempGiftCard2.getCid().trim().equalsIgnoreCase(tempGiftCard.getCid())) {
              tempGiftCard2.setIsValid(false);
            }
          }
          returnValue = DUPLICATE_CARD;
        }
      }
      giftCardListCheckString += tempGiftCard.getCardNumber() + tempGiftCard.getCid() + "~";
    }
    
    return returnValue;
  }
  
  // This checks to see if all gift cards have a zero balance.
  // RETURNS: NO_ERRORS, ALL_CARDS_ARE_ZERO
  protected int checkAllCardsAppliedHaveZeroBalance() {
    boolean allCardsValuesAreZero = true;
    Iterator giftCardIterator = getGiftCardList().iterator();
    GiftCard tempGiftCard = null;
    int numBlankCards = 0;
    
    while (giftCardIterator.hasNext()) {
      tempGiftCard = (GiftCard) giftCardIterator.next();
      if (!tempGiftCard.getIsBlank() && tempGiftCard.getAmountAvailable() != 0.0d) {
        return NO_ERRORS;
      }
      if (tempGiftCard.getIsBlank()) {
        numBlankCards++;
      }
    }
    
    if (numBlankCards == getMaxNumGiftCards() || numBlankCards == getGiftCardList().size()) {
      return NO_ERRORS;
    }
    
    return ALL_CARDS_ARE_ZERO;
  }
  
  // Checks if a customer is buying a gift card with a gift card.
  // RETURNS: NO_ERRORS, GIFT_CARD_PURCHASE
  public int checkIfGiftCardsAreBeingPurchased(Order order) {
    Iterator commerceItemIterator = order.getCommerceItems().iterator();
    NMCommerceItem tempCommerceItem = null;
    
    if (getAreAllGiftCardsBlank() && !getOrderContainsGiftCards()) {
      return NO_ERRORS;
    }
    
    while (commerceItemIterator.hasNext()) {
      tempCommerceItem = (NMCommerceItem) commerceItemIterator.next();
      try {
        if (((String) ((RepositoryItem) tempCommerceItem.getAuxiliaryData().getProductRef()).getPropertyValue("merchandiseType")).trim().equalsIgnoreCase(GIFT_CARD_MERCH_TYPE)
                || ((String) ((RepositoryItem) tempCommerceItem.getAuxiliaryData().getProductRef()).getPropertyValue("merchandiseType")).trim().equalsIgnoreCase(VIRTUAL_GIFT_CARD_MERCH_TYPE)) {
          return GIFT_CARD_PURCHASE;
        }
      } catch (Exception e) {/* if some strange ocurrence happens trying to retrieve the productRef, don't throw errors */}
    }
    
    return NO_ERRORS;
  }
  
  // checks all errors, the return value is the first error that occurs through the order of execution or NO_ERRORS.
  protected int checkForAllErrors(Order order) {
    int returnValue = checkIfGiftCardsAreBeingPurchased(order);
    
    if (returnValue == NO_ERRORS) {
      returnValue = checkGiftCardValues(order.getProfileId());
    }
    if (returnValue == NO_ERRORS) {
      returnValue = checkForDuplicateGiftCards(order);
    }
    if (returnValue == NO_ERRORS) {
      returnValue = checkAllCardsAppliedHaveZeroBalance();
    }
    
    return returnValue;
  }
  
  // Makes payment groups out of the gift cards if the cards are valid
  public synchronized int addGiftCardPaymentGroupsToOrder(Order order) {
    int returnValue = NO_ERRORS;
    if (getIsApplyButtonClicked() == false) {
      try {
        synchronized (order) {
          setIsApplyButtonClicked(true);
          returnValue = checkForAllErrors(order);
          if (returnValue == NO_ERRORS) {
            Iterator giftCardIterator = getGiftCardList().iterator();
            GiftCard tempGiftCard = null;
            NMCreditCard paymentGroup = null;
            NMOrderManager orderManager = (NMOrderManager) getOrderManager();
            NMPaymentGroupManager paymentGroupManager = (NMPaymentGroupManager) orderManager.getPaymentGroupManager();
            
            while (giftCardIterator.hasNext()) {
              tempGiftCard = (GiftCard) giftCardIterator.next();
              if (tempGiftCard.getIsValid() && !tempGiftCard.getIsBlank() && tempGiftCard.getAmountAvailable() > 0.0d) {
                paymentGroup = (NMCreditCard) paymentGroupManager.createPaymentGroup();
                paymentGroup.setCreditCardType(GiftCard.GIFT_CARD_CODE);
                paymentGroup.setCreditCardNumber(tempGiftCard.getCardNumber());
                paymentGroup.setCidTransient(tempGiftCard.getCid());
                paymentGroup.setAmountRemaining(tempGiftCard.getAmountAvailable());
                order.addPaymentGroup(paymentGroup);
                orderManager.addOrderAmountToPaymentGroup(order, paymentGroup.getId(), 0.0);
                giftCardIterator.remove();
              }
            }
          } else {
            return returnValue;
          }
        }
      } catch (Exception e) {
        e.printStackTrace();
      } finally {
        setIsApplyButtonClicked(false);
      }
      return returnValue;
    } else {
      return NO_ERRORS;
    }
  }
  
  public synchronized int validateAndAddGiftCardsToOrder(Order order) {
    int returnValue = NO_ERRORS;
    if (getIsApplyButtonClicked() == false) {
      try {
        synchronized (order) {
          setIsApplyButtonClicked(true);
          returnValue = checkForAllErrors(order);
          if (returnValue == NO_ERRORS) {
            Iterator giftCardIterator = getGiftCardList().iterator();
            double currentOrderAmountRemaining = 0;
            GiftCard tempGiftCard = null;
            CreditCard paymentGroup = null;
            
            while (giftCardIterator.hasNext()) {
              currentOrderAmountRemaining = ((NMOrderManager) getOrderManager()).getCurrentOrderAmountRemaining(order);
              tempGiftCard = (GiftCard) giftCardIterator.next();
              if (tempGiftCard.getIsValid() && !tempGiftCard.getIsBlank() && tempGiftCard.getAmountAvailable() > 0.0d && currentOrderAmountRemaining > 0) {
                paymentGroup = (CreditCard) getOrderManager().createPaymentGroup();
                paymentGroup.setCreditCardType(GiftCard.GIFT_CARD_CODE);
                paymentGroup.setCreditCardNumber(tempGiftCard.getCardNumber());
                ((NMCreditCard) paymentGroup).setCidTransient(tempGiftCard.getCid());
                getOrderManager().addPaymentGroupToOrder(order, paymentGroup);
                if (tempGiftCard.getAmountAvailable() <= currentOrderAmountRemaining) {
                  getOrderManager().addOrderAmountToPaymentGroup(order, paymentGroup.getId(), tempGiftCard.getAmountAvailable());
                  getOrderManager().recalculatePaymentGroupAmounts(order);
                  getOrderManager().updateOrder(order);
                  giftCardIterator.remove();
                } else {
                  getOrderManager().addOrderAmountToPaymentGroup(order, paymentGroup.getId(), currentOrderAmountRemaining);
                  getOrderManager().recalculatePaymentGroupAmounts(order);
                  paymentGroup.setPropertyValue("amountRemaining", new Double(tempGiftCard.getAmountAvailable() - paymentGroup.getAmount()));
                  getOrderManager().updateOrder(order);
                  giftCardIterator.remove();
                  break;
                }
              }
            }
          } else {
            return returnValue;
          }
        }
      } catch (Exception e) {
        e.printStackTrace();
      } finally {
        setIsApplyButtonClicked(false);
      }
      return returnValue;
    } else {
      return NO_ERRORS;
    }
  }
  
  public boolean removeGiftCardFromOrder(Order order, String giftCardPaymentGroupId) {
    try {
      synchronized (order) {
        getOrderManager().removePaymentGroupFromOrder(order, giftCardPaymentGroupId);
        addBlankGiftCard(getGiftCardList());
        getOrderManager().updateOrder(order);
      }
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
    return true;
  }
  
  public void addBlankGiftCard() {
    addBlankGiftCard(getGiftCardList());
  }
  
  public void addBlankGiftCard(ArrayList giftCardList) {
    giftCardList.add(new GiftCard());
  }
  
  // Sorts the Cards in Ascending Order by amount available, amounts that are zero are put the bottom of the list
  public void sortByGiftCardAmounts(ArrayList sortedList) {
    Collections.sort(sortedList, GIFT_CARD_AMOUNT_COMPARATOR);
  }
  
  // if the user attempts too many invalid inquiry attempts during their session this returns true.
  public boolean getIsInValidGiftCardAttempts() {
    if (getInvalidCardErrorCount() > getMaxInvalidGiftCardAttempts()) {
      getGiftCardLogger().logActivity(getCurrentUserIP(), "INVALID GIFT CARD ATTEMPT");
      return true;
    } else {
      return false;
    }
  }
  
  public int getMaxInvalidGiftCardAttempts() {
    return maxInvalidGiftCardAttempts;
  }
  
  public void setMaxInvalidGiftCardAttempts(int maxInvalidGiftCardAttempts) {
    this.maxInvalidGiftCardAttempts = maxInvalidGiftCardAttempts;
  }
  
}
