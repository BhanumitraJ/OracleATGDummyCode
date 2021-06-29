package com.nm.commerce.promotion.awards;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import atg.commerce.order.OrderManager;
import atg.commerce.pricing.PricingTools;
import atg.nucleus.Nucleus;

import com.nm.collections.NMPromotionTypes;
import com.nm.commerce.NMCommerceItem;
import com.nm.commerce.order.IOrder;
import com.nm.commerce.pricing.Markdown;
import com.nm.commerce.pricing.NMItemPriceInfo;
import com.nm.commerce.promotion.NMOrderImpl;
import com.nm.commerce.promotion.rules.RuleHelper;
import com.nm.utils.PromotionsHelper;

public class DollarOffAward extends MarkdownAward {
  
  public DollarOffAward() {
    setName("Dollars Off");
    setValuePrefix("$");
  }
  
  @Override
  public String getDisplayValue() {
    if (getValue() != null) {
      return getValuePrefix() + getValue() + " " + getName();
    }
    return "";
  }
  
  @Override
  public Object getSortableValue() {
    return new Double(RuleHelper.getDoubleValue(getValue()));
  }
  
  @Override
  public boolean apply(IOrder order, Set<NMCommerceItem> applicableItems, String promoKey, String promoCode) throws Exception {
    
    if (applicableItems == null) {
      return false;
    }
    
    PricingTools pricingTools = RuleHelper.getPricingTools();
    
    Map<NMCommerceItem, Double> calculatedItemDiscounts = new HashMap<NMCommerceItem, Double>();
    
    boolean isApplied = false;
    
    double totalDollarForAllItems = 0d;
    // find total dollar value of all items
    Iterator<NMCommerceItem> i = applicableItems.iterator();
    while (i.hasNext()) {
      NMCommerceItem item = i.next();
      totalDollarForAllItems += RuleHelper.getStartingItemPriceForQualification(item);
    }
    
    double totalRolledUpDiscountForLineItems = 0d;
    
    if (totalDollarForAllItems > 0) {
      
      // System.out.println("attempting to use the ROUND method");
      
      // for each line item, determine the per each dollar off amount with the rounding method
      Iterator<NMCommerceItem> i2 = applicableItems.iterator();
      boolean hasPartialAward = false;
      
      while (i2.hasNext()) {
        // apply discount to item...total discount applied to all items must equal sum of individual discounts
        NMCommerceItem item = i2.next();
        
        // based on the % value of the item, get the same % dollar amount of the award
        
        BigDecimal totalDollarDiscountForLineItem = new BigDecimal(RuleHelper.getStartingItemPriceForQualification(item) / totalDollarForAllItems * RuleHelper.getDoubleValue(getValue()));
        // System.out.println("     totalDollarDiscountForLineItem " + totalDollarDiscountForLineItem);
        
        double roundedTotalDollarDiscountForLineItem = totalDollarDiscountForLineItem.setScale(4, BigDecimal.ROUND_HALF_UP).doubleValue();
        // System.out.println("     roundedTotalDollarDiscountForLineItem " + roundedTotalDollarDiscountForLineItem);
        
        // System.out.println("ROUND: roundedTotalDollarDiscountForLineItem " + roundedTotalDollarDiscountForLineItem);
        // System.out.println("ROUND: getStartingPriceForQualification(item) " + RuleHelper.getStartingItemPriceForQualification(item));
        
        double totalDollarDiscountPerEach = roundedTotalDollarDiscountForLineItem / item.getQuantity();
        // System.out.println("     totalDollarDiscountPerEach " + totalDollarDiscountPerEach);
        
        double roundedTotalDollarDiscountPerEach = pricingTools.round(totalDollarDiscountPerEach);
        // System.out.println("     roundedTotalDollarDiscountPerEach " + roundedTotalDollarDiscountPerEach);
        
        double rolledUpDiscountForLineItem = roundedTotalDollarDiscountPerEach * item.getQuantity();
        // System.out.println("     rolledUpDiscountForLineItem " + rolledUpDiscountForLineItem);
        
        double roundedRolledUpDiscountForLineItem = pricingTools.round(rolledUpDiscountForLineItem);
        // System.out.println("     roundedRolledUpDiscountForLineItem " + roundedRolledUpDiscountForLineItem);
        
        if (roundedRolledUpDiscountForLineItem > RuleHelper.getStartingItemPriceForQualification(item)) {
          // System.out.println("  ITEM DISCOUNT EXCEEDS ITEM AMT...granting partial award to bring new item amt to 0d");
          roundedRolledUpDiscountForLineItem = RuleHelper.getStartingItemPriceForQualification(item);
          hasPartialAward = true;
        }
        
        calculatedItemDiscounts.put(item, new Double(roundedRolledUpDiscountForLineItem));
        
        totalRolledUpDiscountForLineItems += roundedRolledUpDiscountForLineItem;
      }
      
      // System.out.println("");
      // System.out.println("     totalRolledUpDiscountForLineItems " + totalRolledUpDiscountForLineItems);
      // System.out.println("");
      
      // if the total dollar discount is less than the specified dollar off amount, then use the raise method
      // unless we know that we have granted a partial award, then the total discount granted could be less
      // than the award amount.
      if (totalRolledUpDiscountForLineItems >= RuleHelper.getDoubleValue(getValue()) || hasPartialAward) {
        
        try {
          addDiscountToLineItems((NMOrderImpl) order, calculatedItemDiscounts, promoKey, promoCode);
          isApplied = true;
        } catch (Exception e) {
          throw new Exception("Error applying rounded award to line items");
        }
      } else {
        
        totalRolledUpDiscountForLineItems = 0d;
        calculatedItemDiscounts.clear();
        
        // perform raise method
        Iterator<NMCommerceItem> i3 = applicableItems.iterator();
        while (i3.hasNext()) {
          // apply discount to item...total discount applied to all items must equal sum of individual discounts
          NMCommerceItem item = i3.next();
          
          // based on the % value of the item, get the same % dollar amount of the award
          
          BigDecimal totalDollarDiscountForLineItem = new BigDecimal(RuleHelper.getStartingItemPriceForQualification(item) / totalDollarForAllItems * RuleHelper.getDoubleValue(getValue()));
          
          // System.out.println("     totalDollarDiscountForLineItem " + totalDollarDiscountForLineItem);
          
          BigDecimal scaledTotalDollarDiscountForLineItem = totalDollarDiscountForLineItem.setScale(4, BigDecimal.ROUND_HALF_UP);
          // System.out.println("     scaledTotalDollarDiscountForLineItem " + scaledTotalDollarDiscountForLineItem);
          
          double roundedTotalDollarDiscountForLineItem = scaledTotalDollarDiscountForLineItem.doubleValue();
          // System.out.println("     roundedTotalDollarDiscountForLineItem " + roundedTotalDollarDiscountForLineItem);
          
          // System.out.println("RAISE: roundedTotalDollarDiscountForLineItem " + roundedTotalDollarDiscountForLineItem);
          // System.out.println("RAISE: getStartingPriceForQualification(item) " + RuleHelper.getStartingItemPriceForQualification(item));
          
          double totalDollarDiscountPerEach = roundedTotalDollarDiscountForLineItem / item.getQuantity();
          // System.out.println("     totalDollarDiscountPerEach " + totalDollarDiscountPerEach);
          
          double roundedTotalDollarDiscountPerEach = pricingTools.roundDown(totalDollarDiscountPerEach);
          // System.out.println("     roundedTotalDollarDiscountPerEach " + roundedTotalDollarDiscountPerEach);
          
          double raisedRoundedTotalDollarDiscountPerEach = roundedTotalDollarDiscountPerEach + .01;
          // System.out.println("     raisedRoundedTotalDollarDiscountPerEach " + raisedRoundedTotalDollarDiscountPerEach);
          
          double roundedTotalDollarDiscountPerEachRaised = pricingTools.round(raisedRoundedTotalDollarDiscountPerEach);
          // System.out.println("     roundedTotalDollarDiscountPerEachRaised " + roundedTotalDollarDiscountPerEachRaised);
          
          double rolledUpDiscountForLineItem = roundedTotalDollarDiscountPerEachRaised * item.getQuantity();
          // System.out.println("     rolledUpDiscountForLineItem " + rolledUpDiscountForLineItem);
          
          double roundedRolledUpDiscountForLineItem = pricingTools.round(rolledUpDiscountForLineItem);
          // System.out.println("     roundedRolledUpDiscountForLineItem " + roundedRolledUpDiscountForLineItem);
          
          if (roundedRolledUpDiscountForLineItem > RuleHelper.getStartingItemPriceForQualification(item)) {
            // System.out.println("  ITEM DISCOUNT EXCEEDS ITEM AMT...granting partial award to bring new item amt to 0d");
            roundedRolledUpDiscountForLineItem = RuleHelper.getStartingItemPriceForQualification(item);
          }
          
          calculatedItemDiscounts.put(item, new Double(roundedRolledUpDiscountForLineItem));
          
          totalRolledUpDiscountForLineItems += roundedRolledUpDiscountForLineItem;
        }
        
        // System.out.println("");
        // System.out.println("     totalRolledUpDiscountForLineItems " + totalRolledUpDiscountForLineItems);
        // System.out.println("");
        
        try {
          addDiscountToLineItems((NMOrderImpl) order, calculatedItemDiscounts, promoKey, promoCode);
          isApplied = true;
        } catch (Exception e) {
          throw new Exception("Error applying raised award to line items");
        }
      }
    }
    
    return isApplied;
  }
  
  private void addDiscountToLineItems(NMOrderImpl order, Map<NMCommerceItem, Double> itemDiscounts, String promoKey, String promoCode) throws Exception {
    
    OrderManager mOrderManager = RuleHelper.getOrderManager();
    
    final String PROMOHELPER_PATH = "/nm/utils/PromotionsHelper";
    PromotionsHelper mPromotionsHelper = (PromotionsHelper) Nucleus.getGlobalNucleus().resolveName(PROMOHELPER_PATH);
    
    Set<NMCommerceItem> mapKeys = itemDiscounts.keySet();
    Iterator<NMCommerceItem> i = mapKeys.iterator();
    while (i.hasNext()) {
      NMCommerceItem item = i.next();
      double lineItemDiscount = itemDiscounts.get(item).doubleValue();
      
      double newItemPrice = item.getCurrentItemPrice() - lineItemDiscount;
      
      if (newItemPrice >= 0) {
        // System.out.println("     newItemPrice " + newItemPrice);
        
        // System.out.println("     setting pricing tools");
        NMItemPriceInfo ipi = (NMItemPriceInfo) item.getPriceInfo();
        ipi.setAmount(newItemPrice);
        
        // type will tell us how to show what kind of discount it is
        // the amount of the discount will be the other important info (to show line item on site)
        Map<String, Markdown> promotionsApplied = ipi.getPromotionsApplied();
        Markdown dollarOff = new Markdown();
        dollarOff.setType(getType());
        dollarOff.setDollarDiscount(lineItemDiscount);
        dollarOff.setPromoKey(promoKey);
        dollarOff.setUserEnteredPromoCode(promoCode);
        promotionsApplied.put(getId(), dollarOff);
        
        ipi.setDiscounted(true);
        ipi.setPromotionsApplied(promotionsApplied);
        ipi.setOrderDiscountShare(lineItemDiscount);
        ipi.markAsFinal();
        item.setPriceInfo(ipi);
        
        // are these needed now that we are populating Markdown map?
        // item.setSendCmosPromoCode(promoCode);
        // item.setSendCmosPromoKey(promoKey);
      }
    }
    mPromotionsHelper.addActivePromoCodesToItems(order, promoKey, promoCode);
    try {
      // System.out.println("     *** updating order");
      mOrderManager.updateOrder(order);
    } catch (Exception e) {
      e.printStackTrace();
      throw e;
    }
    
  }
  
  @Override
  protected boolean apply(IOrder order, String promoKey, String promoCode) throws Exception {
    Set<NMCommerceItem> qualifiedItems = new HashSet<NMCommerceItem>(order.getPromoEligibleOrderItems());
    return apply(order, qualifiedItems, promoKey, promoCode);
  }
  
  public int getAwardType() {
    return NMPromotionTypes.RULE_BASED;
  }
  
  @Override
  public int getType() {
    return RuleHelper.DOLLAR_OFF_AWARD;
  }
}
