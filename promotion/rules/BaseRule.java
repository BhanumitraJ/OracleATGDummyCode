package com.nm.commerce.promotion.rules;

import java.util.Set;

import atg.repository.RepositoryItem;

import com.nm.commerce.NMCommerceItem;
import com.nm.commerce.order.ICommerceObject;
import com.nm.commerce.order.IOrder;

public abstract class BaseRule implements Rule {
  
  protected String id;
  protected String name;
  protected String qualTitle;
  protected String applyTitle;
  protected int classification;
  protected int type;
  protected int valueComparator;
  protected String value;
  protected Rule nextAnd;
  protected Rule nextOr;
  protected String valuePrefix;
  protected String displayValue;
  protected int sequenceNum;
  protected boolean hiddenRule = false;
  protected boolean booleanRule = false;
  protected RepositoryItem checkoutTypeRepositoryItem;
  
  protected abstract boolean test(ICommerceObject obj);
  
  protected abstract Set<NMCommerceItem> addQualifiedItems(IOrder order, Set<NMCommerceItem> qualifiedItems);
  
  protected abstract Set<NMCommerceItem> removeUnqualifiedItems(IOrder order, Set<NMCommerceItem> qualifiedItems);
  
  @Override
  public Set<NMCommerceItem> evaluate(IOrder order, Set<NMCommerceItem> qualifiedItems) {
    addQualifiedItems(order, qualifiedItems);
    if ((qualifiedItems != null) && !qualifiedItems.isEmpty()) {
      qualifiedItems = pass(order, qualifiedItems);
    } else {
      return fail();
    }
    return removeUnqualifiedItems(order, qualifiedItems);
  }
  
  public Set<NMCommerceItem> pass(IOrder order, Set<NMCommerceItem> qualifiedItems) {
    if (nextAnd != null) {
      qualifiedItems = nextAnd.evaluate(order, qualifiedItems);
    }
    return qualifiedItems;
  }
  
  public Set<NMCommerceItem> fail() {
    return null;
  }
  
  public String getStringOperator() {
    
    switch (getValueComparator()) {
      case RuleHelper.EQUALS: {
        return RuleHelper.DISPLAY_EQUALS;
      }
      case RuleHelper.GREATERTHANOREQUAL: {
        return RuleHelper.DISPLAY_GREATERTHANOREQUAL;
      }
      case RuleHelper.NOTEQUAL: {
        return RuleHelper.DISPLAY_NOTEQUAL;
      }
      default: {
        return "";
      }
    }
  }
  
  @Override
  public String getValue() {
    return value;
  }
  
  public void setValue(String value) {
    this.value = value;
  }
  
  public String getName() {
    return name;
  }
  
  public void setName(String name) {
    this.name = name;
  }
  
  public Rule getNextAnd() {
    return nextAnd;
  }
  
  public void setNextAnd(Rule nextAnd) {
    this.nextAnd = nextAnd;
  }
  
  public Rule getNextOr() {
    return nextOr;
  }
  
  public void setNextOr(Rule nextOr) {
    this.nextOr = nextOr;
  }
  
  public String getId() {
    return id;
  }
  
  public void setId(String id) {
    this.id = id;
  }
  
  public int getClassification() {
    return classification;
  }
  
  public void setClassification(int classification) {
    this.classification = classification;
  }
  
  public int getType() {
    return type;
  }
  
  public void setType(int type) {
    this.type = type;
  }
  
  public String getValuePrefix() {
    return valuePrefix;
  }
  
  public void setValuePrefix(String valuePrefix) {
    this.valuePrefix = valuePrefix;
  }
  
  // this is the string representation of the rule as a whole (name and value details)
  public abstract String getDisplayValue();
  
  public void setDisplayValue(String displayValue) {
    this.displayValue = displayValue;
  }
  
  /**
   * @return the checkoutTypeRepositoryItem
   */
  public RepositoryItem getCheckoutTypeRepositoryItem() {
    return checkoutTypeRepositoryItem;
  }
  
  /**
   * @param checkoutTypeRepositoryItem
   *          the checkoutTypeRepositoryItem to set
   */
  public void setCheckoutTypeRepositoryItem(RepositoryItem checkoutTypeRepositoryItem) {
    this.checkoutTypeRepositoryItem = checkoutTypeRepositoryItem;
  }
  
  public String getQualifyDisplayValue() {
    return getDisplayValue();
  }
  
  public String getApplyDisplayValue() {
    return getDisplayValue();
  }
  
  public int getValueComparator() {
    return valueComparator;
  }
  
  public void setValueComparator(int valueComparator) {
    this.valueComparator = valueComparator;
  }
  
  public int getSequenceNum() {
    return sequenceNum;
  }
  
  public void setSequenceNum(int sequenceNum) {
    this.sequenceNum = sequenceNum;
  }
  
  public boolean isHiddenRule() {
    return hiddenRule;
  }
  
  public void setHiddenRule(boolean hiddenRule) {
    this.hiddenRule = hiddenRule;
  }
  
  public boolean isBooleanRule() {
    return booleanRule;
  }
  
  public void setBooleanRule(boolean booleanRule) {
    this.booleanRule = booleanRule;
  }
  
  public String getQualTitle() {
    return qualTitle;
  }
  
  public void setQualTitle(String qualTitle) {
    this.qualTitle = qualTitle;
  }
  
  public String getApplyTitle() {
    return applyTitle;
  }
  
  public void setApplyTitle(String applyTitle) {
    this.applyTitle = applyTitle;
  }
  
}
