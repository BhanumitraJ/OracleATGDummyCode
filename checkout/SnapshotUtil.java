package com.nm.commerce.checkout;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.nm.collections.NMPromotion;
import com.nm.collections.PercentOffPromotion;
import com.nm.collections.Promotion;
import com.nm.collections.PurchaseWithPurchase;
import com.nm.commerce.promotion.NMOrderImpl;
import com.nm.components.CommonComponentHelper;
import com.nm.sitemessages.Message;
import com.nm.sitemessages.MessageDefs;
import com.nm.utils.PromotionsHelper;

public class SnapshotUtil {
  public static final int EDIT_ITEM_FROM_SUMMARY_CART = 0;
  public static final int EDIT_ITEM_FROM_REGULAR_CART = 1;
  public static final int CHANGE_SHIP_METHOD = 2;
  public static final int CHANGE_SHIP_DESTINATION = 3;
  public static final int CHANGE_TENDER_TYPE = 4;
  public static final int STOCK_CHECK = 5;
  public static final int LIMIT_PROMO = 6;
  
  public static Collection<Message> generatePromoStatusMessages(final OrderSnapshot orig, final OrderSnapshot curr, final int context, NMOrderImpl order, Set<String> declinedGwpSelects,
          Set<String> declinedPwpSelects) {
    
    final HashMap<String, Message> messages = new HashMap<String, Message>();
    // Set<NMPromotion> origList = orig.getAwardedPromotions();
    // Iterator<NMPromotion> iterator = origList.iterator();
    Set<NMPromotion> currList = curr.getAwardedPromotions();
    // iterator = currList.iterator();
    Iterator<NMPromotion> currIterator = currList.iterator();
    ArrayList<String> limitedPromotions = new ArrayList<String>();
    ArrayList<String> declinedLimitedPromos = new ArrayList<String>();
    
    if (order.shouldValidateLimitPromoUse() && (context != EDIT_ITEM_FROM_SUMMARY_CART && context != EDIT_ITEM_FROM_REGULAR_CART)) {
      for (Iterator<NMPromotion> j = currIterator; j.hasNext();) {
        final NMPromotion promotion = (NMPromotion) j.next();
        String codeForList = "";
        if (promotion.getPromotionClass() == NMPromotion.RULE_BASED) {
          codeForList = ((com.nm.commerce.promotion.rulesBased.Promotion) promotion).getPromoKeys();
        } else
          codeForList = promotion.getCode();
        if (promotion.getLimitPromoUse()) {
          if (codeForList.indexOf(",") > 0) {
            String[] promoCodes = codeForList.split(",");
            for (int i = 0; i < promoCodes.length; i++) {
              if (!limitedPromotions.contains(promoCodes[i]) && !order.getUsedLimitPromoMap().contains(promoCodes[i])) {
                limitedPromotions.add(promoCodes[i]);
              }
            }
          } else if (!limitedPromotions.contains(codeForList) && !order.getUsedLimitPromoMap().contains(codeForList)) {
            limitedPromotions.add(codeForList);
          }
        }
      }
      try {
        PromotionsHelper promotionsHelper = CommonComponentHelper.getPromotionsHelper();
        declinedLimitedPromos = promotionsHelper.hasNotReachedPromoLimit(order, limitedPromotions);
      } catch (Exception e) {
        System.out.println("SnapshotUtil: Error retrieving declined one limit time promotion results. " + e.getMessage());
      }
    }
    // Dev testing
    // declinedLimitedPromos.add(new String("G1570WN"));
    
    final Set<NMPromotion> disqualified = new HashSet<NMPromotion>(orig.getAwardedPromotions());
    disqualified.removeAll(curr.getAwardedPromotions());
    ArrayList<NMPromotion> removablePromotions = new ArrayList<NMPromotion>();
    if (declinedLimitedPromos.size() > 0 && (context != EDIT_ITEM_FROM_SUMMARY_CART && context != EDIT_ITEM_FROM_REGULAR_CART)) {
      currIterator = currList.iterator();
      for (Iterator<NMPromotion> j = currIterator; j.hasNext();) {
        final NMPromotion promotion = (NMPromotion) j.next();
        for (Iterator<String> k = declinedLimitedPromos.iterator(); k.hasNext();) {
          String promoKey = (String) k.next();
          if (promotion.getPromotionClass() == NMPromotion.RULE_BASED) {
            String promoKeys = ((com.nm.commerce.promotion.rulesBased.Promotion) promotion).getPromoKeys().toUpperCase();
            if (promoKeys.contains(promoKey)) {
              removablePromotions.add(promotion);
              // order.getUsedLimitPromoMap().add(promotion.getCode());
            }
          } else {
            if (promotion.getCode().equalsIgnoreCase(promoKey)) {
              removablePromotions.add(promotion);
              // order.getUsedLimitPromoMap().add(promotion.getCode());
              
            }
          }
        }
      }
      
      disqualified.addAll(removablePromotions);
    }
    
    boolean isDisqualifiedFromOther = false;
    boolean isPartOfRemovablePromos = false;
    for (Iterator<NMPromotion> i = disqualified.iterator(); i.hasNext();) {
      Message message = null;
      final NMPromotion promo = (NMPromotion) i.next();
      
      if (context == EDIT_ITEM_FROM_REGULAR_CART || context == EDIT_ITEM_FROM_SUMMARY_CART) isDisqualifiedFromOther = true;
      if (promo.getLimitPromoUse()) {
        if (!isDisqualifiedFromOther) {
          isPartOfRemovablePromos = removablePromotions.contains(promo);
          message = getPromoMessage(promo, context, order, declinedGwpSelects, orig, declinedPwpSelects, isDisqualifiedFromOther, isPartOfRemovablePromos);
          if (message != null) {
            if (message.getMsgId().contains("OneTimeLimit")) {
              if (removablePromotions.contains(promo)) {
                String messageId = message.getMsgId() + promo.getCode();
                message.setMsgId(messageId);
                String limitPromoMessage = message.getMsgText() + " " + promo.getName() + " one-time use promotion " + promo.getCode() + " because the promotion was applied to a previous order.";
                message.setMsgText(limitPromoMessage);
                order.getUsedLimitPromoMap().add(promo.getCode());
                if (promo.getPromotionClass() == NMPromotion.RULE_BASED) {
                  String promoKeys = ((com.nm.commerce.promotion.rulesBased.Promotion) promo).getPromoKeys().toUpperCase();
                  order.getUsedLimitPromoMap().add(promoKeys);
                }
              } else
                message = null;
            }
          }
        } else
          message = null;
      } else {
        message = getPromoMessage(promo, context, order, declinedGwpSelects, orig, declinedPwpSelects, isDisqualifiedFromOther, isPartOfRemovablePromos);
        if (message != null) {
            if (message.getMsgId().contains("OneTimeLimit")) {
            	String promoName=promo.getName();
            	if(promo instanceof PercentOffPromotion && ((PercentOffPromotion)promo).getPlccFlag()){
            		promoName = promo.getPromoCodes();
            		message.setMsgId(message.getMsgId()+"_PLCC_ERROR_"+promoName);
                }else{
                	String messageId = message.getMsgId() + promo.getCode();
                	message.setMsgId(messageId);
                }
                String limitPromoMessage = message.getMsgText() + " " + promoName+".";
                message.setMsgText(limitPromoMessage);
            }
        }
      }
      
      if (message != null) {
        messages.put(message.getMsgId(), message);
      }
    }
    isDisqualifiedFromOther = false;
    
    final Set<NMPromotion> isqualified = new HashSet<NMPromotion>(curr.getAwardedPromotions());
    isqualified.removeAll(orig.getAwardedPromotions());
    
    for (Iterator<NMPromotion> i = isqualified.iterator(); i.hasNext();) {
      final NMPromotion promo = (NMPromotion) i.next();
      Message message = getGiftwrapReminder(promo, context, order);
      
      if (message != null) {
        messages.put(message.getMsgId(), message);
      }
    }
    
    return (messages.values());
  }
  
  private static Message getPromoMessage(final NMPromotion promo, final int context, final NMOrderImpl order, Set<String> declinedGwpSelects, OrderSnapshot orig, Set<String> declinedPwpSelects,
          boolean isDisqualifiedFromCart, boolean isPartOfRemovablePromos) {
    Message returnValue = null;
    
    // if we're doing a stock check and the promo is an instance of
    // NMPromotion (which
    // is the based class of all our promotion classes) then return a
    // generic
    // disqualification message.
    
    int promotionClass = promo.getPromotionClass();
    
    switch (promotionClass) {
      case NMPromotion.EXTRA_ADDRESS: {
        returnValue = null;
      }
      break;
      
      case NMPromotion.GIFT_WITH_PURCHASE: {
        boolean hasPromoCodes = promo.hasPromoCodes();
        
        if (hasPromoCodes && (context == EDIT_ITEM_FROM_SUMMARY_CART || context == EDIT_ITEM_FROM_REGULAR_CART)) {
          returnValue = MessageDefs.getMessage(MessageDefs.MSG_Promotion_GenericDisqualification);
          
        } else if (!hasPromoCodes && context == EDIT_ITEM_FROM_SUMMARY_CART) {
          returnValue = MessageDefs.getMessage(MessageDefs.MSG_Promotion_GenericDisqualification);
        } else if (context == STOCK_CHECK) {
          if (isPartOfRemovablePromos) {
            returnValue = MessageDefs.getMessage(MessageDefs.MSG_OneTimeLimitMessagePercentOff);
          } else {
            returnValue = MessageDefs.getMessage(MessageDefs.MSG_Promotion_GenericDisqualification);
          }
          
        } else if (context == LIMIT_PROMO) {
          if (isDisqualifiedFromCart) {
            returnValue = MessageDefs.getMessage(MessageDefs.MSG_Promotion_GenericDisqualification);
          } else {
            returnValue = MessageDefs.getMessage(MessageDefs.MSG_OneTimeLimitMessagePercentOff);
          }
        }
      }
      break;
      
      case NMPromotion.GIFT_WITH_PURCHASE_SELECT: {
        String promoKey = promo.getCode();
        Map<String, String> gwpSelectPromoMap = orig.getGwpSelectPromoMap();
        if (!declinedGwpSelects.contains(promoKey) && !gwpSelectPromoMap.containsKey(promo.getCode())) {
          if (context == EDIT_ITEM_FROM_SUMMARY_CART || context == EDIT_ITEM_FROM_REGULAR_CART || context == STOCK_CHECK) {
            returnValue = MessageDefs.getMessage(MessageDefs.MSG_Promotion_GenericDisqualification);
          }
        }
      }
      break;
      
      case NMPromotion.PURCHASE_WITH_PURCHASE: {
        
        final PurchaseWithPurchase promotion = (PurchaseWithPurchase) promo;
        
        String promoKey = promo.getCode();
        Map<String, ?> pwpSelectPromoMap = order.getPwpMultiSkuPromoMap();
        
        if (context == EDIT_ITEM_FROM_SUMMARY_CART || context == EDIT_ITEM_FROM_REGULAR_CART) {
          if (!declinedPwpSelects.contains(promoKey) && !pwpSelectPromoMap.containsKey(promo.getCode())) {
            if (!order.hasPwpPromotion(promotion.getPwpProduct())) returnValue = MessageDefs.getMessage(MessageDefs.MSG_Promotion_GenericDisqualification);
          }
        } else if (context == CHANGE_TENDER_TYPE) {
          if (!declinedPwpSelects.contains(promoKey) && !pwpSelectPromoMap.containsKey(promo.getCode())) {
            if (promotion.isInCirlcePromotion() && !order.hasPwpPromotion(promotion.getPwpProduct())) {
              returnValue = MessageDefs.getMessage(MessageDefs.MSG_Promotion_GenericDisqualification);
            }
          }
        } else if (context == STOCK_CHECK) {
          returnValue = MessageDefs.getMessage(MessageDefs.MSG_Promotion_GenericDisqualification);
        }
      }
      break;
      
      case NMPromotion.GIFT_WRAP: {
        boolean hasPromoCodes = promo.hasPromoCodes();
        
        if (hasPromoCodes && (context == EDIT_ITEM_FROM_SUMMARY_CART || context == EDIT_ITEM_FROM_REGULAR_CART)) {
          returnValue = MessageDefs.getMessage(MessageDefs.MSG_Promotion_GenericDisqualification);
          
        } else if (!hasPromoCodes && context == EDIT_ITEM_FROM_SUMMARY_CART) {
          boolean hasGiftwrapping = order.hasGiftwrapping();
          
          if (hasGiftwrapping) {
            returnValue = MessageDefs.getMessage(MessageDefs.MSG_Promotion_GenericDisqualification);
          }
        } else if (context == STOCK_CHECK) {
          returnValue = MessageDefs.getMessage(MessageDefs.MSG_Promotion_GenericDisqualification);
        }
      }
      break;
      
      case NMPromotion.PERCENT_OFF: {
        boolean hasPromoCodes = promo.hasPromoCodes();
        
        if (hasPromoCodes && (context == EDIT_ITEM_FROM_SUMMARY_CART || context == EDIT_ITEM_FROM_REGULAR_CART)) {
          returnValue = MessageDefs.getMessage(MessageDefs.MSG_Promotion_GenericDisqualification);
          
        } else if (!hasPromoCodes && context == EDIT_ITEM_FROM_SUMMARY_CART) {
          returnValue = MessageDefs.getMessage(MessageDefs.MSG_Promotion_GenericDisqualification);
        } else if (context == CHANGE_TENDER_TYPE) {
          returnValue = MessageDefs.getMessage(MessageDefs.MSG_OneTimeLimitMessagePercentOff);
        } else if (context == LIMIT_PROMO) {
          if (isDisqualifiedFromCart)
            returnValue = MessageDefs.getMessage(MessageDefs.MSG_Promotion_GenericDisqualification);
          else
            returnValue = MessageDefs.getMessage(MessageDefs.MSG_OneTimeLimitMessagePercentOff);
        } else if (context == STOCK_CHECK) {
          if (isPartOfRemovablePromos)
            returnValue = MessageDefs.getMessage(MessageDefs.MSG_OneTimeLimitMessagePercentOff);
          else
            returnValue = MessageDefs.getMessage(MessageDefs.MSG_Promotion_GenericDisqualification);
        } else if (((PercentOffPromotion)promo).isBogo() && context == EDIT_ITEM_FROM_REGULAR_CART) {
        	returnValue = MessageDefs.getMessage(MessageDefs.MSG_Promotion_GenericDisqualification);
        }
      }
      break;
      
      case NMPromotion.FIFTYONE_PASS_THROUGH: {
        returnValue = MessageDefs.getMessage(MessageDefs.MSG_Promotion_GenericDisqualification);
      }
      break;
      
      case NMPromotion.SHIPPING: {
        final Promotion promotion = (Promotion) promo;
        boolean hasPromoCodes = promotion.hasPromoCodes();
        if (hasPromoCodes && (context == EDIT_ITEM_FROM_SUMMARY_CART || context == EDIT_ITEM_FROM_REGULAR_CART)) {
          returnValue = MessageDefs.getMessage(MessageDefs.MSG_Promotion_GenericDisqualification);
          
        } else if (!hasPromoCodes && context == EDIT_ITEM_FROM_SUMMARY_CART) {
          returnValue = MessageDefs.getMessage(MessageDefs.MSG_Promotion_GenericDisqualification);
          
        } else if (context == CHANGE_SHIP_METHOD) {
          returnValue = MessageDefs.getMessage(MessageDefs.MSG_Promotion_ShipMethodDisqualification);
          
        } else if (context == CHANGE_SHIP_DESTINATION) {
          if (promo.getLimitPromoUse() && !order.getUsedLimitPromoMap().contains(promotion.getCode())) {
            returnValue = MessageDefs.getMessage(MessageDefs.MSG_OneTimeLimitMessageShipping);
          } else if (!order.getUsedLimitPromoMap().contains(promotion.getCode())) {
            returnValue = MessageDefs.getMessage(MessageDefs.MSG_Promotion_ShipDestDisqualification);
          }
          
        } else if (context == CHANGE_TENDER_TYPE) {
          if (promotion.isInCirlcePromotion()) {
            returnValue = MessageDefs.getMessage(MessageDefs.MSG_Promotion_IncircleDisqualification);
          } else {
            returnValue = MessageDefs.getMessage(MessageDefs.MSG_Promotion_PayMethodDisqualification);
          }
          
        } else if (context == CHANGE_TENDER_TYPE) {
          returnValue = MessageDefs.getMessage(MessageDefs.MSG_OneTimeLimitMessageShipping);
          
        } else if (context == LIMIT_PROMO) {
          if (isDisqualifiedFromCart) {
            returnValue = MessageDefs.getMessage(MessageDefs.MSG_Promotion_GenericDisqualification);
          } else {
            returnValue = MessageDefs.getMessage(MessageDefs.MSG_OneTimeLimitMessageShipping);
          }
          
        } else if (context == STOCK_CHECK) {
          if (isPartOfRemovablePromos) {
            returnValue = MessageDefs.getMessage(MessageDefs.MSG_OneTimeLimitMessageShipping);
          } else {
            returnValue = MessageDefs.getMessage(MessageDefs.MSG_Promotion_GenericDisqualification);
          }
        }
      }
      break;
      
      case NMPromotion.RULE_BASED: {
        boolean hasPromoCodes = promo.hasPromoCodes();
        if (hasPromoCodes && (context == EDIT_ITEM_FROM_SUMMARY_CART || context == EDIT_ITEM_FROM_REGULAR_CART)) {
          returnValue = MessageDefs.getMessage(MessageDefs.MSG_Promotion_GenericDisqualification);
        } else if (!hasPromoCodes && context == EDIT_ITEM_FROM_SUMMARY_CART) {
          returnValue = MessageDefs.getMessage(MessageDefs.MSG_Promotion_GenericDisqualification);
        } else if (context == CHANGE_TENDER_TYPE) {
          returnValue = MessageDefs.getMessage(MessageDefs.MSG_OneTimeLimitMessageDollarOff);
        } else if (context == LIMIT_PROMO) {
          if (isDisqualifiedFromCart) {
            returnValue = MessageDefs.getMessage(MessageDefs.MSG_Promotion_GenericDisqualification);
          } else {
            returnValue = MessageDefs.getMessage(MessageDefs.MSG_OneTimeLimitMessageDollarOff);
          }
        } else if (context == STOCK_CHECK) {
          if (isPartOfRemovablePromos) {
            returnValue = MessageDefs.getMessage(MessageDefs.MSG_OneTimeLimitMessageDollarOff);
          } else {
            returnValue = MessageDefs.getMessage(MessageDefs.MSG_Promotion_GenericDisqualification);
          }
        }
        
      }
      break;
    }
    
    return returnValue;
  }
  
  private static Message getGiftwrapReminder(final NMPromotion promo, final int context, final NMOrderImpl order) {
    Message returnValue = null;
    
    int promotionClass = promo.getPromotionClass();
    
    switch (promotionClass) {
    
      case NMPromotion.GIFT_WRAP: {
        boolean hasPromoCodes = promo.hasPromoCodes();
        
        if (hasPromoCodes && (context == EDIT_ITEM_FROM_SUMMARY_CART || context == EDIT_ITEM_FROM_REGULAR_CART)) {
          returnValue = MessageDefs.getMessage(MessageDefs.MSG_Promotion_GiftwrapReminder);
        } else if (!hasPromoCodes && context == EDIT_ITEM_FROM_REGULAR_CART) {
          boolean hasGiftwrapping = order.hasGiftwrapping();
          if (!hasGiftwrapping) {
            returnValue = MessageDefs.getMessage(MessageDefs.MSG_Promotion_GiftwrapReminder);
          }
        }
      }
      break;
    }
    return returnValue;
  }
  
}
