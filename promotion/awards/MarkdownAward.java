package com.nm.commerce.promotion.awards;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.nm.commerce.NMCommerceItem;
import com.nm.commerce.order.IOrder;
import com.nm.commerce.promotion.rules.ApplyToQualifiedItemsRule;
import com.nm.commerce.promotion.rules.Rule;

public abstract class MarkdownAward extends BaseAwardImpl {
  
  protected abstract boolean apply(IOrder order, String promoKey, String promoCode) throws Exception;
  
  protected abstract boolean apply(IOrder order, Set<NMCommerceItem> qualifiedItems, String promoKey, String promoCode) throws Exception;
  
  @Override
  public boolean apply(IOrder order, Set<NMCommerceItem> qualifiedItems, List<Rule> applicationRules, String promoKey, String promoCode) throws Exception {
    
    boolean applied = false;
    
    int applicationType = getApplicationType(applicationRules);
    
    switch (applicationType) {
    
    // apply to Order
      case 0: {
        // System.out.println(" 0 apply to order");
        if (apply(order, promoKey, promoCode)) applied = true;
        break;
      }
      
      // apply to Qualified Items
      case 1: {
        // System.out.println(" 1 apply to qualified items");
        if (apply(order, qualifiedItems, promoKey, promoCode)) applied = true;
        break;
      }
      
      // apply based on RuleSet
      case 2: {
        
        // System.out.println(" 2 use apply rules");
        Set<NMCommerceItem> applicableItems = new HashSet<NMCommerceItem>();
        
        if (applicationRules != null) {
          Iterator<Rule> appRules = applicationRules.iterator();
          if (appRules.hasNext()) {
            Rule rule = (Rule) appRules.next();
            applicableItems = rule.evaluate(order, applicableItems);
          }
        }
        
        if (apply(order, applicableItems, promoKey, promoCode)) applied = true;
        break;
      }
      
      // apply to Order
      default: {
        // System.out.println("default apply to order");
        if (apply(order, promoKey, promoCode)) applied = true;
        break;
      }
    }
    
    return applied;
  }
  
  /*
   * this method will change in the future if we allow any combination of qualification rules and application rules
   */
  public int getApplicationType(List<Rule> applicationRules) {
    // this is going to rely on application rules
    // if there are no application rules > apply to order
    // if there is ONE application rule AND the rule is instanceof ApplyToQualifiedItems > apply to qualified items
    // if there is ONE or more application rules AND NOT instanceof ApplyToQualifiedItems > apply rules to all order items to find qual items
    
    // apply to entire order by default
    if (applicationRules == null || applicationRules.isEmpty()) return 0;
    
    // if any rules specify apply to Qualified Items, then that rule takes precedence
    // all other rules will be ignored
    Iterator<Rule> i = applicationRules.iterator();
    while (i.hasNext()) {
      Rule rule = (Rule) i.next();
      if (rule instanceof ApplyToQualifiedItemsRule) return 1;
    }
    
    // use application rules
    return 2;
  }
}
