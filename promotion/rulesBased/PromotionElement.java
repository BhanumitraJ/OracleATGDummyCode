package com.nm.commerce.promotion.rulesBased;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import atg.nucleus.logging.ApplicationLogging;
import atg.nucleus.logging.ClassLoggingFactory;
import atg.repository.RepositoryItem;

import com.nm.commerce.NMCommerceItem;
import com.nm.commerce.order.IOrder;
import com.nm.commerce.promotion.awards.Award;
import com.nm.commerce.promotion.awards.BaseAward;
import com.nm.commerce.promotion.rules.Rule;
import com.nm.commerce.promotion.rules.RuleHelper;
import com.nm.components.CommonComponentHelper;
import com.nm.integration.util.NMCheckoutTypeUtil;
import com.nm.utils.PromotionsHelper;

public class PromotionElement {
  
  private String id;
  private String name = "";
  private List<Rule> qualificationRules;
  private List<Rule> aggregateRules;
  private List<Rule> applicationRules;
  private List<Rule> allRules;
  private List<BaseAward> awards;
  private RepositoryItem item;
  private boolean isNew = false;
  
  NMCheckoutTypeUtil nmCheckoutTypeUtil = CommonComponentHelper.getNMCheckoutTypeUtil();
  PromotionsHelper promotionsHelper = CommonComponentHelper.getPromotionsHelper();
  private static ApplicationLogging mLogger = ClassLoggingFactory.getFactory().getLoggerForClass(PromotionElement.class);
  
  public PromotionElement() {}
  
  public PromotionElement(PromotionElement element) {
    this.id = element.getId();
    this.name = element.getName();
    this.item = element.getItem();
    if (element.getQualificationRules() != null) {
      this.qualificationRules = new ArrayList<Rule>(element.getQualificationRules());
    }
    if (element.getAggregateRules() != null) {
      this.aggregateRules = new ArrayList<Rule>(element.getAggregateRules());
    }
    if (element.getApplicationRules() != null) {
      this.applicationRules = new ArrayList<Rule>(element.getApplicationRules());
    }
    if (element.getAwards() != null) {
      this.awards = new ArrayList<BaseAward>(element.getAwards());
    }
  }
  
  public Set<NMCommerceItem> qualify(IOrder order) {
    
    Set<NMCommerceItem> qualifiedItems = new HashSet<NMCommerceItem>();
    if (getQualificationRules() != null) {
      // run through rules against order and items to test to get qualified item list
      Iterator<Rule> qualRules = qualificationRules.iterator();
      // System.out.println("evaluating QUALIFICATION rules");
      if (qualRules.hasNext()) {
        Rule rule = qualRules.next();
        qualifiedItems = rule.evaluate(order, Collections.synchronizedSet(new HashSet<NMCommerceItem>()));
      }
    }
    
    if (getAggregateRules() != null) {
      // check qualified item list against aggregate rules to determine eligibility
      Iterator<Rule> aggregateRules = getAggregateRules().iterator();
      // System.out.println("evaluating AGGREGATE rules");
      if (aggregateRules.hasNext()) {
        Rule rule = aggregateRules.next();
        // System.out.println("       evaluating aggregate rule " + ((BaseRule)rule).getDisplayValue());
        qualifiedItems = rule.evaluate(order, qualifiedItems);
      }
    }
    
    // System.out.println("qualified items: " + qualifiedItems);
    
    return qualifiedItems;
  }
  
  protected boolean award(IOrder order, Set<NMCommerceItem> qualifiedItems) {
    boolean awarded = false;
    
    if ((qualifiedItems == null) || qualifiedItems.isEmpty()) {
      return awarded;
    }
    
    Rule promoCodeRule = RuleHelper.getPromoCodeRule(this);
    String promoCode = null;
    if (promoCodeRule != null) {
      promoCode = promoCodeRule.getValue();
    }
    
    Iterator<BaseAward> awardsI = getAwards().iterator();
    // for each award associated to this PromotionElement
    // check application rules and apply according to specification
    while (awardsI.hasNext()) {
      BaseAward award = awardsI.next();
      try {
        if (award.apply(order, qualifiedItems, applicationRules, getId(), promoCode)) {
          awarded = true;
        }
      } catch (Exception e) {
        // e.printStackTrace();
        // System.out.println("Unable to award promo '" + getName() + "' : " + e.getMessage());
      }
    }
    
    return awarded;
  }
  
  public Object getSortableAwardValue() {
    if ((getAwards() != null) && !getAwards().isEmpty()) {
      Award award = getAwards().get(0);
      if (award != null) {
        return award.getSortableValue();
      }
    }
    return null;
  }
  
  public void setIsNew(boolean value) {
    isNew = value;
  }
  
  public boolean isNew() {
    return isNew;
  }
  
  public void setQualificationRules(List<Rule> qualificationRules) {
    this.qualificationRules = qualificationRules;
  }
  
  public void setApplicationRules(List<Rule> applicationRules) {
    this.applicationRules = applicationRules;
  }
  
  public void setAwards(List<BaseAward> awards) {
    this.awards = awards;
  }
  
  public String getName() {
    return name;
  }
  
  public void setName(String name) {
    this.name = name;
  }
  
  public List<Rule> getQualificationRules() {
    return qualificationRules;
  }
  
  public List<Rule> getApplicationRules() {
    return applicationRules;
  }
  
  public List<BaseAward> getAwards() {
    return awards;
  }
  
  public String getId() {
    return id;
  }
  
  public void setId(String id) {
    this.id = id;
  }
  
  public List<Rule> getAllRules() {
    allRules = new ArrayList<Rule>();
    if (getQualificationRules() != null) {
      allRules.addAll(getQualificationRules());
    }
    if (getAggregateRules() != null) {
      allRules.addAll(getAggregateRules());
    }
    if (getApplicationRules() != null) {
      allRules.addAll(getApplicationRules());
    }
    
    return allRules;
  }
  
  public List<Rule> getAllQualificationRules() {
    allRules = new ArrayList<Rule>();
    if (getQualificationRules() != null) {
      allRules.addAll(getQualificationRules());
    }
    if (getAggregateRules() != null) {
      allRules.addAll(getAggregateRules());
    }
    return allRules;
  }
  
  public void setAllRules(List<Rule> allRules) {
    this.allRules = allRules;
  }
  
  public List<Rule> getAggregateRules() {
    return aggregateRules;
  }
  
  public void setAggregateRules(List<Rule> aggregateRules) {
    this.aggregateRules = aggregateRules;
  }
  
  public RepositoryItem getItem() {
    return item;
  }
  
  public void setItem(RepositoryItem item) {
    this.item = item;
  }
}
