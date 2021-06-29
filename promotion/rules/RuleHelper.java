package com.nm.commerce.promotion.rules;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import atg.commerce.order.OrderManager;
import atg.commerce.pricing.PricingTools;
import atg.nucleus.Nucleus;
import atg.repository.Repository;
import atg.repository.RepositoryItem;
import atg.repository.RepositoryView;
import atg.repository.rql.RqlStatement;

import com.nm.ajax.checkout.utils.ComponentUtils;
import com.nm.collections.NMPromotion;
import com.nm.collections.NMPromotionArray;
import com.nm.commerce.NMCommerceItem;
import com.nm.commerce.pricing.Markdown;
import com.nm.commerce.pricing.NMItemPriceInfo;
import com.nm.commerce.promotion.awards.BaseAward;
import com.nm.commerce.promotion.awards.DollarOffAward;
import com.nm.commerce.promotion.rulesBased.Promotion;
import com.nm.commerce.promotion.rulesBased.PromotionElement;

public class RuleHelper {
  
  // VALUE COMPARATORS
  public static final int EQUALS = 1;
  public static final String DISPLAY_EQUALS = "Is";
  public static final int GREATERTHANOREQUAL = 2;
  public static final String DISPLAY_GREATERTHANOREQUAL = "Is At Least";
  public static final int GREATERTHAN = 3;
  public static final String DISPLAY_GREATERTHAN = "Is Greater Than";
  public static final int LESSTHAN = 4;
  public static final String DISPLAY_LESSTHAN = "Is Less Than";
  public static final int LESSTHANOREQUAL = 5;
  public static final String DISPLAY_LESSTHANOREQUAL = "Is Not More Than";
  public static final int NOTEQUAL = 6;
  public static final String DISPLAY_NOTEQUAL = "Is Not";
  
  // RULES
  public static final int PROMO_CODE_RULE = 1;
  public static final int PROMO_KEY_RULE = 2;
  public static final int APPLY_TO_QUALIFIED_ITEMS_RULE = 3;
  public static final int APPLICABLE_ITEMS_DOLLAR_RULE = 4;
  public static final int ORDER_DOLLAR_RULE = 5;
  public static final int ITEM_DEPT_RULE = 6;
  public static final int ITEM_MULTI_DEPT_RULE = 7;
  public static final int QUALIFIED_COUNT_RULE = 8;
  public static final int QUALIFIED_DOLLAR_RULE = 9;
  public static final int ITEM_IN_STOCK_RULE = 10;
  public static final int ITEM_IS_SALE_RULE = 11;
  public static final int ITEM_IS_NOT_SALE_RULE = 12;
  public static final int ITEM_CLASS_CODE_RULE = 13;
  
  // these may not be needed for phase 1
  public static final int ITEM_DOLLAR_RULE = 14;
  
  // VisaCheckout - adding a constant visa checkout rule
  public static final int VISA_CHECKOUT_RULE = 15;
  
  // MasterPass - adding constants for MasterPass rule
  public static final int MASTER_PASS_RULE = 16;
  
  /** Int variable to hold the Constant NMBG_PLCC_RULE. */
  public static final int NMBG_PLCC_RULE = 17;
  
  /** Int variable to hold the Constant VISA_RULE. */
  public static final int VISA_RULE = 18;
  
  /** Int variable to hold the Constant MASTER_CARD_RULE. */
  public static final int MASTER_CARD_RULE = 19;
  
  /** Int variable to hold the Constant DISCOVER_RULE. */
  public static final int DISCOVER_RULE = 20;
  
  /** Int variable to hold the Constant PAYPAL_RULE. */
  public static final int PAYPAL_RULE = 21;
  
  /** Int variable to hold the Constant SHOPRUNNER_RULE. */
  public static final int SHOPRUNNER_RULE = 22;
  
  /** Int variable to hold the Constant AMEX_RULE. */
  public static final int AMEX_RULE = 23;
  
  /** Int variable to hold the Constant DINERS_RULE. */
  public static final int DINERS_RULE = 24;
  
  /** Int variable to hold the Constant MP_RULE. */
  public static final int MP_RULE = 26;
  
  /** Int variable to hold the Constant VME_RULE. */
  public static final int VME_RULE = 27;
  
  // AWARDS
  public static final int DOLLAR_OFF_AWARD = 1;
  
  // RULE TYPE
  public static final int QUALIFICATION_RULE = 0;
  public static final int APPLICATION_RULE = 1;
  
  // PROMOTION CLASSIFICATION
  public static final int STANDALONE = 1;
  public static final int TIERED = 2;
  public static final int STACKED = 3;
  
  // COOKIE INFORMATION FOR ONE TIME PROMOS
  public static String COOKIE_NAME = "OTPOP"; // One Time Percent Off Promo
  public static String COOKIE_DELIMITER = "~";
  
  public static double getDoubleValue(final String valueStr) {
    double value = 0d;
    try {
      if (valueStr != null) {
        value = new Double(valueStr).doubleValue();
      }
    } catch (final Exception e) {}
    return value;
  }
  
  public static int getIntValue(final String valueStr) {
    int value = 0;
    try {
      if (valueStr != null) {
        value = new Integer(valueStr).intValue();
      }
    } catch (final Exception e) {}
    return value;
  }
  
  public static PricingTools getPricingTools() {
    return (PricingTools) Nucleus.getGlobalNucleus().resolveName("/atg/commerce/pricing/PricingTools");
  }
  
  public static OrderManager getOrderManager() {
    return (OrderManager) Nucleus.getGlobalNucleus().resolveName("/atg/commerce/order/OrderManager");
  }
  
  private static NMPromotionArray getPromotionArray(){
	  ComponentUtils componentUtils = ComponentUtils.getInstance();
	  NMPromotionArray promotionArray = componentUtils.getPromotionArray(NMPromotionArray.RULE_BASED_PROMOTIONS_ARRAY);
	  return promotionArray;
  }
  
  /**
   * Return the starting price for the purposes of a promotion qualification check. The starting price is the raw total price minus all percent off markdowns on the commerce item. Prior to January
   * 2014, only the "last" percent off markdown was applied, presumably as a sort of double check that only one percent off markdown could be applied. After January 2014, as a result of the stackable
   * promo project, all percent off markdowns are applied. Primarily this would take effect with two 113s.
   * 
   * @param item
   * @return
   */
  public static double getStartingItemPriceForQualification(final NMCommerceItem item) {
    double startingPrice = item.getRawTotalPrice();
    
    final Map<String, Markdown> markdowns = item.getItemMarkdowns();
    
    if (markdowns != null) {
      final Iterator<String> i = markdowns.keySet().iterator();
      while (i.hasNext()) {
        final Markdown markdown = markdowns.get(i.next());
        if ((markdown != null) && (markdown.getType() == Markdown.PERCENT_OFF)) {
          startingPrice -= markdown.getDollarDiscount();
        }
      }
    }
    // EDO changes
    final NMItemPriceInfo PriceInfo = (NMItemPriceInfo) item.getPriceInfo();
    final double employeeDiscount = PriceInfo.getEmployeeDiscountAmount();
    final double employeeExtraDiscount = PriceInfo.getEmployeeExtraDiscountAmount();
    final double totalEmployeeDiscount = employeeDiscount + employeeExtraDiscount;
    if (totalEmployeeDiscount > 0.0) {
      startingPrice -= totalEmployeeDiscount;
    }
    // EDO changes
    return startingPrice;
  }
  
  public static void removePromoCodeRule(final Promotion promotion) {
    if (promotion.getPromotionElements() != null) {
      final Iterator<PromotionElement> i = promotion.getPromotionElements().iterator();
      while (i.hasNext()) {
        final PromotionElement promotionElement = i.next();
        removePromoCodeRule(promotionElement);
      }
    }
  }
  
  public static List<String> getPromoKeyRuleValues(final Promotion promotion) {
    
    final List<String> promoKeyRules = new ArrayList<String>();
    if ((promotion != null) && (promotion.getPromotionElements() != null)) {
      final Iterator<PromotionElement> i = promotion.getPromotionElements().iterator();
      while (i.hasNext()) {
        final PromotionElement element = i.next();
        if (element != null) {
          final Rule rule = getPromoKeyRule(element);
          if (rule != null) {
            promoKeyRules.add(rule.getValue());
          }
        }
      }
    }
    return promoKeyRules;
  }
  
  public static Rule getPromoKeyRule(final PromotionElement promotionElement) {
    
    if ((promotionElement != null) && (promotionElement.getQualificationRules() != null)) {
      final Iterator<Rule> j = promotionElement.getQualificationRules().iterator();
      while (j.hasNext()) {
        final Rule rule = j.next();
        if (rule instanceof PromoKeyRule) {
          return rule;
        }
      }
    }
    
    if ((promotionElement != null) && (promotionElement.getApplicationRules() != null)) {
      final Iterator<Rule> j = promotionElement.getApplicationRules().iterator();
      while (j.hasNext()) {
        final Rule rule = j.next();
        if (rule instanceof PromoKeyRule) {
          return rule;
        }
      }
    }
    
    return null;
  }
  
  public static Rule getPromoKeyQualificationRule(final PromotionElement promotionElement) {
    
    if ((promotionElement != null) && (promotionElement.getQualificationRules() != null)) {
      final Iterator<Rule> j = promotionElement.getQualificationRules().iterator();
      while (j.hasNext()) {
        final Rule rule = j.next();
        if (rule instanceof PromoKeyRule) {
          return rule;
        }
      }
    }
    
    return null;
  }
  
  public static Rule getPromoKeyApplicationRule(final PromotionElement promotionElement) {
    
    if ((promotionElement != null) && (promotionElement.getApplicationRules() != null)) {
      final Iterator<Rule> j = promotionElement.getApplicationRules().iterator();
      while (j.hasNext()) {
        final Rule rule = j.next();
        if (rule instanceof PromoKeyRule) {
          return rule;
        }
      }
    }
    
    return null;
  }
  
  public static Rule getPromoCodeRule(final PromotionElement promotionElement) {
    
    if ((promotionElement != null) && (promotionElement.getQualificationRules() != null)) {
      final Iterator<Rule> j = promotionElement.getQualificationRules().iterator();
      while (j.hasNext()) {
        final Rule rule = j.next();
        if (rule instanceof PromoCodeRule) {
          return rule;
        }
      }
    }
    
    return null;
  }
  
  public static ArrayList<Rule> getOrderDollarRules(final PromotionElement promotionElement) {
    
    final ArrayList<Rule> orderDollarRules = new ArrayList<Rule>();
    if ((promotionElement != null) && (promotionElement.getQualificationRules() != null)) {
      final Iterator<Rule> j = promotionElement.getQualificationRules().iterator();
      while (j.hasNext()) {
        final Rule rule = j.next();
        if (rule instanceof OrderDollarRule) {
          orderDollarRules.add(rule);
        }
      }
    }
    
    return orderDollarRules;
  }
  
  public static ArrayList<Rule> getQualifiedDollarRules(final PromotionElement promotionElement) {

    final ArrayList<Rule> qualifiedDollarRules = new ArrayList<Rule>();
    if ((promotionElement != null) && (promotionElement.getAggregateRules() != null)) {
      final Iterator<Rule> j = promotionElement.getAggregateRules().iterator();
      while (j.hasNext()) {
        final Rule rule = j.next();
        if (rule instanceof QualifiedDollarRule) {
        	qualifiedDollarRules.add(rule);
        }
      }
    }
    
    return qualifiedDollarRules;
  }

  public static ArrayList<Rule> getQualifiedItemCountRules(final PromotionElement promotionElement) {

    final ArrayList<Rule> qualifiedItemCountRules = new ArrayList<Rule>();
    if ((promotionElement != null) && (promotionElement.getAggregateRules() != null)) {
      final Iterator<Rule> j = promotionElement.getAggregateRules().iterator();
      while (j.hasNext()) {
        final Rule rule = j.next();
        if (rule instanceof QualifiedItemCountRule) {
        	qualifiedItemCountRules.add(rule);
        }
      }
    }
    
    return qualifiedItemCountRules;
  }

  public static void removePromoCodeRule(final PromotionElement promotionElement) {
    
    if ((promotionElement != null) && (promotionElement.getQualificationRules() != null)) {
      final Iterator<Rule> j = promotionElement.getQualificationRules().iterator();
      while (j.hasNext()) {
        final Rule rule = j.next();
        if (rule instanceof PromoCodeRule) {
          j.remove();
        }
      }
    }
  }
  
  public static String getPromoKeys(final Promotion promotion) {
    
    final StringBuffer promoKeys = new StringBuffer();
    boolean firstElement = true;
    if ((promotion != null) && (promotion.getPromotionElements() != null) && !promotion.getPromotionElements().isEmpty()) {
      final Iterator<PromotionElement> i = promotion.getPromotionElements().iterator();
      while (i.hasNext()) {
        final PromotionElement promoElement = i.next();
        if (promoElement != null) {
          if (!firstElement) {
            promoKeys.append(", ");
          }
          promoKeys.append(promoElement.getId());
          firstElement = false;
        }
      }
    }
    
    return promoKeys.toString();
  }
  
  public static String getPromoCode(final Promotion promotion) {
    
    String promoCode = null;
    
    if ((promotion != null) && (promotion.getPromotionElements() != null) && !promotion.getPromotionElements().isEmpty()) {
      final PromotionElement promoElement = promotion.getPromotionElements().get(0);
      promoCode = getPromoCode(promoElement);
    }
    
    return promoCode;
  }
  
  public static String getPromoCode(final PromotionElement promotionElement) {
    
    String promoCode = null;
    
    if ((promotionElement != null) && (promotionElement.getQualificationRules() != null)) {
      final Iterator<Rule> j = promotionElement.getQualificationRules().iterator();
      while (j.hasNext()) {
        final BaseRule rule = (BaseRule) j.next();
        if (rule instanceof PromoCodeRule) {
          promoCode = rule.getValue();
        }
      }
    }
    
    return promoCode;
  }
  
  public static boolean isActiveDollarOffPromoCode(String codeIn) {
    
    if (codeIn != null) {
      codeIn = codeIn.trim().toUpperCase();
      final Map<String, NMPromotion> activePromos = getPromotionArray().getAllActivePromotions();
      final Iterator<NMPromotion> activePromosIter = activePromos.values().iterator();
      while (activePromosIter.hasNext()) {
        final Promotion activePromo = (Promotion)activePromosIter.next();
        final String activePromoCode = getPromoCode(activePromo);
        if ((activePromoCode != null) && codeIn.equals(activePromoCode.trim().toUpperCase())) {
          return true;
        }
      }
    }
    
    return false;
  }
  
  public static boolean isDollarOffPromoKey(final String promoKeyIn) throws Exception {
    
    final Map<String, NMPromotion> promos = getPromotionArray().getAllPromotions();
    final Iterator<NMPromotion> promosIter = promos.values().iterator();
    while (promosIter.hasNext()) {
      // all rules-based NMPromotions are Promotions
      final Promotion promo = (Promotion) promosIter.next();
      final List<String> promoKeyRuleValuesForPromo = getPromoKeyRuleValues(promo);
      if ((promoKeyIn != null) && promoKeyRuleValuesForPromo.contains(promoKeyIn)) {
        return true;
      }
    }
    
    return false;
  }
  
  public static boolean isActiveDollarOffPromoKey(final String promoKeyIn) {
    
    final Map<String, NMPromotion> activePromos = getPromotionArray().getAllActivePromotions();
    final Iterator<NMPromotion> activePromosIter = activePromos.values().iterator();
    while (activePromosIter.hasNext()) {
      final Promotion activePromo = (Promotion)activePromosIter.next();
      final List<String> promoKeyRuleValuesForPromo = getPromoKeyRuleValues(activePromo);
      if ((promoKeyIn != null) && promoKeyRuleValuesForPromo.contains(promoKeyIn)) {
        return true;
      }
    }
    
    return false;
  }
  
  public static Promotion getActiveDollarOffPromoKey(final String promoKeyIn) {
    
    final Map<String, NMPromotion> activePromos = getPromotionArray().getAllActivePromotions();
    final Iterator<NMPromotion> activePromosIter = activePromos.values().iterator();
    Promotion activePromo = null;
    while (activePromosIter.hasNext()) {
      activePromo = (Promotion)activePromosIter.next();
      final List<String> promoKeyRuleValuesForPromo = getPromoKeyRuleValues(activePromo);
      if ((promoKeyIn != null) && promoKeyRuleValuesForPromo.contains(promoKeyIn)) {
        return activePromo;
      }
    }
    
    return activePromo;
  }
  
  public static boolean isActivePercentOffPromoCode(String promoCode) {
    if (promoCode != null) {
      promoCode = promoCode.trim().toUpperCase();
      final Date now = new Date();
      try {
        final Repository repo = (Repository) Nucleus.getGlobalNucleus().resolveName("/nm/xml/CsrPercentOffRepository");
        final RepositoryView view = repo.getView("percentOffPromotions");
        final RqlStatement statement = RqlStatement.parseRqlStatement("promoCodes = ?0 AND startDate <= ?1 AND endDate >= ?2");
        final Object params[] = new Object[] {promoCode , now , now};
        final RepositoryItem[] items = statement.executeQuery(view, params);
        
        if (items != null) {
          return true;
        }
      } catch (final Exception e) {
        e.printStackTrace();
      }
    }
    
    return false;
  }
  
  public static boolean compare(final double d1, final double d2, final int operator) {
    
    switch (operator) {
      case EQUALS: {
        return (d1 == d2);
      }
      case GREATERTHAN: {
        return (d1 > d2);
      }
      case LESSTHAN: {
        return (d1 < d2);
      }
      case GREATERTHANOREQUAL: {
        return (d1 >= d2);
      }
      case LESSTHANOREQUAL: {
        return (d1 <= d2);
      }
      case NOTEQUAL: {
        return (d1 != d2);
      }
    }
    
    return false;
  }
  
  public static boolean compare(final String a, final String b, final int operator) {
    
    if ((a == null) || (b == null)) {
      return false;
    }
    
    switch (operator) {
      case EQUALS: {
        return (a.equals(b));
      }
      case NOTEQUAL: {
        return (!a.equals(b));
      }
    }
    
    return false;
  }
  
  public static boolean compare(final int a, final int b, final int operator) {
    
    switch (operator) {
      case EQUALS: {
        return (a == b);
      }
      case GREATERTHAN: {
        return (a > b);
      }
      case LESSTHAN: {
        return (a < b);
      }
      case GREATERTHANOREQUAL: {
        return (a >= b);
      }
      case LESSTHANOREQUAL: {
        return (a <= b);
      }
      case NOTEQUAL: {
        return (a != b);
      }
    }
    
    return false;
  }
  
  public static BaseRule getRule(final int type) {
    
    switch (type) {
      case RuleHelper.PROMO_CODE_RULE: {
        return new PromoCodeRule();
      }
      case RuleHelper.PROMO_KEY_RULE: {
        return new PromoKeyRule();
      }
      case RuleHelper.ORDER_DOLLAR_RULE: {
        return new OrderDollarRule();
      }
      case RuleHelper.ITEM_DEPT_RULE: {
        return new ItemDeptRule();
      }
      case RuleHelper.ITEM_MULTI_DEPT_RULE: {
        return new ItemMultiDeptRule();
      }
      case RuleHelper.APPLY_TO_QUALIFIED_ITEMS_RULE: {
        return new ApplyToQualifiedItemsRule();
      }
      case RuleHelper.APPLICABLE_ITEMS_DOLLAR_RULE: {
        return new ApplicableItemsDollarRule();
      }
      case RuleHelper.QUALIFIED_COUNT_RULE: {
        return new QualifiedItemCountRule();
      }
      case RuleHelper.ITEM_IN_STOCK_RULE: {
        return new ItemInStockRule();
      }
      case RuleHelper.ITEM_IS_SALE_RULE: {
        return new ItemIsSaleRule();
      }
      case RuleHelper.ITEM_IS_NOT_SALE_RULE: {
        return new ItemIsNotSaleRule();
      }
      case RuleHelper.ITEM_CLASS_CODE_RULE: {
        return new ItemClassCodeRule();
      }
      case RuleHelper.QUALIFIED_DOLLAR_RULE: {
        return new QualifiedDollarRule();
      }
      case RuleHelper.VISA_CHECKOUT_RULE: {
        return new VisaCheckoutPromotionRule();
      }
      case NMBG_PLCC_RULE:
      case VISA_RULE:
      case MASTER_CARD_RULE:
      case DISCOVER_RULE:
      case PAYPAL_RULE:
      case SHOPRUNNER_RULE:
      case AMEX_RULE:
      case DINERS_RULE:
      case VME_RULE:
      case MP_RULE: {
        final CheckoutTypePromotionRule rule = new CheckoutTypePromotionRule(type);
        if (null != rule.getCheckoutTypeRepositoryItem()) {
          return rule;
        }
        return null;
      }
      default: {
        CheckoutTypePromotionRule rule = new CheckoutTypePromotionRule(type);
        if (null != rule.getCheckoutTypeRepositoryItem()) {
          return rule;
        }
        return null;
      }
    }
  }
  
  public static BaseAward getAward(final int type) {
    
    // System.out.println("getting award for type " + type);
    
    switch (type) {
      case RuleHelper.DOLLAR_OFF_AWARD: {
        return new DollarOffAward();
      }
      default: {
        return null;
      }
      
    }
  }
  
  public static String getClassificationDisplay(final int type) {
    switch (type) {
      case STANDALONE: {
        return "Standalone";
      }
      case TIERED: {
        return "Tiered";
      }
      case STACKED: {
        return "Stacked";
      }
      default: {
        return "";
      }
    }
  }
  
}
