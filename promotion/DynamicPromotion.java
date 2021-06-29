/**
 * 
 */
package com.nm.commerce.promotion;

import java.util.Date;

import atg.repository.RepositoryItem;

import com.nm.collections.NMPromotion;
import com.nm.commerce.promotion.rules.RuleHelper;
import com.nm.commerce.promotion.rulesBased.Promotion;

/**
 * @author vsal3
 * 
 */
public class DynamicPromotion extends NMPromotion {
  private String id;
  private String type;
  private String name;
  private String seed;
  private Date startDate;
  private Date endDate;
  private Date marketingStartDate;
  private Date marketingEndDate;
  private String userGenerated;
  private String promoCode;
  private Date dateGenerated;
  private String codesGenerated;
  private String promoKeys;
  
  public DynamicPromotion() {}
  
  public DynamicPromotion(RepositoryItem dynamicPromoReposItem) {
    this.id = (String) dynamicPromoReposItem.getPropertyValue("promotionId");
    this.type = (String) dynamicPromoReposItem.getPropertyValue("type");
    
    Object user = dynamicPromoReposItem.getPropertyValue("userGenerated");
    if (user != null)
      this.userGenerated = (String) user;
    else
      this.userGenerated = "";
    
    Object date = dynamicPromoReposItem.getPropertyValue("dateGenerated");
    if (date != null)
      this.dateGenerated = (Date) date;
    else
      this.dateGenerated = null;
    
    Object count = dynamicPromoReposItem.getPropertyValue("codesGenerated");
    if (count != null)
      this.codesGenerated = ((Integer) count).toString();
    else
      this.codesGenerated = "";
    
    this.seed = (String) dynamicPromoReposItem.getPropertyValue("generationSeed");
  }
  
  public void updatePromotion(RepositoryItem percentOffPromoReposItem) {
    this.startDate = (Date) percentOffPromoReposItem.getPropertyValue("startDate");
    this.endDate = (Date) percentOffPromoReposItem.getPropertyValue("endDate");
    this.name = (String) percentOffPromoReposItem.getPropertyValue("name");
    this.promoCode = (String) percentOffPromoReposItem.getPropertyValue("promoCodes");
    this.promoKeys = this.id;
  }
  
  public void updatePromotion(Promotion rulesBasedPromotion) {
    this.startDate = (Date) rulesBasedPromotion.getStartDate();
    this.endDate = (Date) rulesBasedPromotion.getEndDate();
    this.name = (String) rulesBasedPromotion.getName();
    this.promoCode = RuleHelper.getPromoCode(rulesBasedPromotion);
    this.promoKeys = (String) rulesBasedPromotion.getPromoKeys();
  }
  
  public String getId() {
    return id;
  }
  
  public void setId(String id) {
    this.id = id;
  }
  
  public String getName() {
    return name;
  }
  
  public void setName(String name) {
    this.name = name;
  }
  
  public String getPromoCode() {
    return promoCode;
  }
  
  public void setPromoCode(String promoCode) {
    this.promoCode = promoCode;
  }
  
  @Override
  public Date getStartDate() {
    return startDate;
  }
  
  @Override
  public Date getEndDate() {
    return endDate;
  }
  
  @Override
  public Date getMarketingStartDate() {
    return marketingStartDate;
  }
  
  @Override
  public Date getMarketingEndDate() {
    return marketingEndDate;
  }
  
  @Override
  public String getCode() {
    return id;
  }
  
  @Override
  public String getPromoCodes() {
    // TODO Auto-generated method stub
    return getPromoCode();
  }
  
  @Override
  public int getPromotionClass() {
    // TODO Auto-generated method stub
    return 0;
  }
  
  @Override
  public boolean isPromoReinforcementFlag() {
    // TODO Auto-generated method stub
    return false;
  }
  
  @Override
  public String getPromoReinforcementHtml() {
    // TODO Auto-generated method stub
    return null;
  }
  
  @Override
  public boolean requiresEmailValidation() {
    // TODO Auto-generated method stub
    return false;
  }
  
  @Override
  public void setCode(String value) {
    id = value;
  }
  
  @Override
  public String getType() {
    return this.type;
  }
  
  public Date getDateGenerated() {
    return dateGenerated;
  }
  
  public void setDateGenerated(Date dateGenerated) {
    this.dateGenerated = dateGenerated;
  }
  
  public String getUserGenerated() {
    return userGenerated;
  }
  
  public void setUserGenerated(String userGenerated) {
    this.userGenerated = userGenerated;
  }
  
  public String getCodesGenerated() {
    return codesGenerated;
  }
  
  public void setCodesGenerated(String codesGenerated) {
    this.codesGenerated = codesGenerated;
  }
  
  public void setStartDate(Date startDate) {
    this.startDate = startDate;
  }
  
  public void setEndDate(Date endDate) {
    this.endDate = endDate;
  }
  
  public String getSeed() {
    return seed;
  }
  
  public void setSeed(String seed) {
    this.seed = seed;
  }
  
  public String getPromoKeys() {
    return promoKeys;
  }
  
  public void setPromoKeys(String promoKeys) {
    this.promoKeys = promoKeys;
  }
  
}
