package com.nm.commerce.promotion.service;

import static com.nm.common.INMGenericConstants.CHECKOUT_TYPES;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;
import javax.transaction.TransactionManager;

import atg.dtm.TransactionDemarcation;
import atg.dtm.TransactionDemarcationException;
import atg.nucleus.Nucleus;
import atg.nucleus.logging.ApplicationLogging;
import atg.nucleus.logging.ClassLoggingFactory;
import atg.repository.MutableRepository;
import atg.repository.MutableRepositoryItem;
import atg.repository.Repository;
import atg.repository.RepositoryException;
import atg.repository.RepositoryItem;
import atg.repository.RepositoryView;
import atg.repository.rql.RqlStatement;

import com.nm.ajax.checkout.utils.ComponentUtils;
import com.nm.collections.NMPromotion;
import com.nm.collections.RulesBasedPromotionArray;
import com.nm.commerce.promotion.PromotionAttributeTypes;
import com.nm.commerce.promotion.awards.BaseAward;
import com.nm.commerce.promotion.rules.BaseRule;
import com.nm.commerce.promotion.rules.PromoKeyRule;
import com.nm.commerce.promotion.rules.Rule;
import com.nm.commerce.promotion.rules.RuleHelper;
import com.nm.commerce.promotion.rulesBased.Promotion;
import com.nm.commerce.promotion.rulesBased.PromotionElement;
import com.nm.components.CommonComponentHelper;
import com.nm.utils.CSRPromotionsHelper;
import com.nm.utils.dynamicCode.DynamicCodeUtils;

public class RulesBasedPromotionService {
  
  public RulesBasedPromotionService() {}
  
  private final ApplicationLogging mLogging = ClassLoggingFactory.getFactory().getLoggerForClass(RulesBasedPromotionService.class);
  
  // archive status constants
  public static final String DELETE = "DELETE";
  public static final String UPDATE = "UPDATE";
  public static final String INSERT = "INSERT";
  
  private DataSource connectionPool = (DataSource) Nucleus.getGlobalNucleus().resolveName("/atg/dynamo/service/jdbc/JTDataSource");
  private final RulesBasedPromotionArray rulesBasedPromotionArray = (RulesBasedPromotionArray) Nucleus.getGlobalNucleus().resolveName("/nm/collections/RulesBasedPromotionArray");
  private Connection conn = null;
  
  public DataSource getConnectionPool() {
    return connectionPool;
  }
  
  public void setConnectionPool(final DataSource connectionPool) {
    this.connectionPool = connectionPool;
  }
  
  /*
   * getPromotionByPromotionElementId returns the single RBPromotion using only a 'PromoKey' not the actual repositoryItem. There should never be more than 1 promotion returned, but if there is, the
   * returned promotion will be the last in the list.
   */
  public Promotion getPromotionByPromotionElementId(final String promotionElementId) {
    Promotion foundRBPromotion = null;
    
    try {
      foundRBPromotion = (Promotion) rulesBasedPromotionArray.getPromotionElement(promotionElementId);
    } catch (final Exception e) {
      if (mLogging.isLoggingError()) {
        mLogging.logError("-------->>>>>>  PH ruleBasedService.getPromotion error is:", e);
      }
    }
    
    return foundRBPromotion;
  }
  
  // each member is really a Promotion
  public Map<String, NMPromotion> getAllPromotions() throws Exception {
    return rulesBasedPromotionArray.getAllPromotions();
  }
  
  public List<Promotion> getAllExpiredPromos(final int daysPastExpiration) throws Exception {
    
    final Calendar cal = Calendar.getInstance();
    cal.add(Calendar.DATE, -daysPastExpiration);
    final Date date = cal.getTime();
    
    final List<Promotion> promotions = new ArrayList<Promotion>();
    
    try {
      final Repository promoRepository = (Repository) Nucleus.getGlobalNucleus().resolveName("/nm/xml/RulesBasedPromotionRepository");
      final RepositoryView view = promoRepository.getView("rulesBasedPromotion");
      final RqlStatement stmt = RqlStatement.parseRqlStatement("endDate < ?0");
      final Object[] params = new Object[1];
      params[0] = date;
      final RepositoryItem[] items = stmt.executeQuery(view, params);
      
      if (items != null) {
        for (int i = 0; i < items.length; i++) {
          final RepositoryItem promotionRI = items[i];
          // all rules-based NMPromotions are Promotions
          promotions.add((Promotion) rulesBasedPromotionArray.repositoryItemToBean(promotionRI));
        }
      }
    } catch (final Exception e) {
      throw new Exception("There was a problem retrieving promotions");
    }
    
    return promotions;
    
  }
  
  /*public List<Promotion> getAllActivePromotions() {
    
    
    final List<Promotion> activePromotions = new ArrayList<Promotion>();
    
    try {
      
      final Date now = new Date();
      
      // this method does the ALL query
      Map<String, NMPromotion> promotions = rulesBasedPromotionArray.getAllPromotions();
      
      if (promotions != null) {
        final Iterator<NMPromotion> i = promotions.values().iterator();
        while (i.hasNext()) {
          // all rules-based NMPromotions are Promotions
          final Promotion promo = (Promotion) i.next();
          if ((promo.getStartDate() != null) && promo.getStartDate().before(now) && (promo.getEndDate() != null) && promo.getEndDate().after(now)) {
            activePromotions.add(promo);
          }
        }
      }
    } catch (final Exception e) {
      if (mLogging.isLoggingError()) {
        mLogging.logError("Exception: RulesBasedPromotionService.java in getAllActivePromotions(): ", e);
      }
    }
    
    return activePromotions;
    
  }*/
  
  public List<Promotion> getAllActivePromotionsForRulesBased() {
    final List<Promotion> activePromotions = new ArrayList<Promotion>();
    
    try {
      
      // this method does the ALL query
      Map<String, NMPromotion> promotions = rulesBasedPromotionArray.getAllPromotions();
      
      if (promotions != null) {
        final Iterator<NMPromotion> i = promotions.values().iterator();
        while (i.hasNext()) {
          // all rules-based NMPromotions are Promotions
          final Promotion promo = (Promotion) i.next();
          if (promo.getStatus() == 0 || promo.getStatus() == 1) { // SCHEDULED = 0 || ACTIVE = 1
            activePromotions.add(promo);
          }
        }
      }
    } catch (final Exception e) {
      if (mLogging.isLoggingError()) {
        mLogging.logError("Exception: RulesBasedPromotionService.java in getAllActivePromotions(): ", e);
      }
    }
    
    return activePromotions;
    
  }
  
  public Promotion getPromotion(final String promoId) {
    return (Promotion) rulesBasedPromotionArray.getPromotion(promoId);
  }
  
  public PromotionElement createPromotionElement() {
    final PromotionElement promotionElement = new PromotionElement();
    if (mLogging.isLoggingDebug()) {
      mLogging.logDebug("creating promo element");
    }
    try {
      final MutableRepository promoRepository = (MutableRepository) Nucleus.getGlobalNucleus().resolveName("/nm/xml/RulesBasedPromotionRepository");
      final MutableRepositoryItem promotionElementRI = promoRepository.createItem("promotionElement");
      promotionElement.setItem(promotionElementRI);
      promotionElement.setId(promotionElementRI.getRepositoryId());
    } catch (final RepositoryException e) {
      if (mLogging.isLoggingError()) {
        mLogging.logError("Exception: RulesBasedPromotionService.java in getPromotion(): ", e);
      }
    }
    return promotionElement;
  }
  
  public boolean savePromotion(Promotion promotion, final List<String> promoKeysToCleanup, final CSRPromotionsHelper csrPromotionsHelper) throws Exception {
    
    final TransactionManager transManager = (TransactionManager) Nucleus.getGlobalNucleus().resolveName("/atg/dynamo/transaction/TransactionManager");
    final TransactionDemarcation trans = new TransactionDemarcation();
    boolean rollback = false;
    String action = UPDATE;
    
    try {
      
      if (mLogging.isLoggingDebug()) {
        mLogging.logDebug("saving promotion ");
      }
      trans.begin(transManager, TransactionDemarcation.REQUIRES_NEW);
      
      final MutableRepository promoRepository = (MutableRepository) Nucleus.getGlobalNucleus().resolveName("/nm/xml/RulesBasedPromotionRepository");
      
      boolean isNewPromotion = false;
      MutableRepositoryItem promotionRI = null;
      if ((promotion.getId() == null) || promotion.getId().equals("")) {
        promotionRI = promoRepository.createItem("rulesBasedPromotion");
        isNewPromotion = true;
        action = INSERT;
      } else {
        promotionRI = promoRepository.getItemForUpdate(promotion.getId(), "rulesBasedPromotion");
      }
      
      if (mLogging.isLoggingDebug()) {
        mLogging.logDebug("inside rules service " + promotion.getLimitPromoUse());
      }
      
      if (promotionRI != null) {
        promotionRI.setPropertyValue("type", new Integer(promotion.getClassification()));
        promotionRI.setPropertyValue("name", promotion.getName());
        promotionRI.setPropertyValue("startDate", promotion.getStartDate());
        promotionRI.setPropertyValue("endDate", promotion.getEndDate());
        promotionRI.setPropertyValue("marketingStartDate", promotion.getMarketingStartDate());
        promotionRI.setPropertyValue("marketingEndDate", promotion.getMarketingEndDate());
        promotionRI.setPropertyValue("user", promotion.getUser());
        promotionRI.setPropertyValue("comments", promotion.getComments());
        promotionRI.setPropertyValue("requiresEmailValidation", new Boolean(promotion.isRequiresEmailValidation()));
        promotionRI.setPropertyValue("limitPromoUse", new Boolean(promotion.getLimitPromoUse()));
        promotionRI.setPropertyValue("promoReinforcementFlag", new Boolean(promotion.isPromoReinforcementFlag()));
        promotionRI.setPropertyValue("promoReinforcementHtml", promotion.getPromoReinforcementHtml());
        promotionRI.setPropertyValue("dynamicFlag", promotion.getDynamicFlag());
        promotionRI.setPropertyValue("spendMoreFlag", promotion.getSpendMoreFlag());
        promotionRI.setPropertyValue("flgEligible", promotion.getFlgEligible());
        promotionRI.setPropertyValue("excludeEmployeesFlag", new Boolean(promotion.isExcludeEmployeesFlag())); // Save Employee Exclusion flag
        promotionRI.setPropertyValue("personalizedFlag", promotion.isPersonalizedFlag());
        final List<MutableRepositoryItem> promotionElements = new ArrayList<MutableRepositoryItem>();
        // save each promo element
        if (promotion.getPromotionElements() != null) {
          final Iterator<PromotionElement> i = promotion.getPromotionElements().iterator();
          while (i.hasNext()) {
            
            final PromotionElement promotionElement = i.next();
            
            // System.out.println("getting promoElement id: " + promotionElement.getId());
            
            boolean isNewPromotionElement = false;
            MutableRepositoryItem promotionElementRI = null;
            if (promotionElement.getItem() != null) {
              promotionElementRI = (MutableRepositoryItem) promotionElement.getItem();
              promotionElement.setItem(null);
              isNewPromotionElement = true;
              promotionElement.setIsNew(true);
            } else {
              promotionElementRI = promoRepository.getItemForUpdate(promotionElement.getId(), "promotionElement");
            }
            
            if (promotionElementRI != null) {
              promotionElementRI.setPropertyValue("name", promotionElement.getName());
              
              final Set<MutableRepositoryItem> rules = new HashSet<MutableRepositoryItem>();
              final Set<MutableRepositoryItem> awards = new HashSet<MutableRepositoryItem>();
              
              // write promoCode rule to each element of the promotion
              if ((promotion.getPromoCode() != null) && !promotion.getPromoCode().equals("")) {
                if (promotionElement.getQualificationRules() == null) {
                  promotionElement.setQualificationRules(new ArrayList<Rule>());
                }
                
                // check for existing promo code rule
                BaseRule promoCodeRule = (BaseRule) RuleHelper.getPromoCodeRule(promotionElement);
                if (promoCodeRule == null) {
                  promoCodeRule = RuleHelper.getRule(RuleHelper.PROMO_CODE_RULE);
                  promoCodeRule.setValue(promotion.getPromoCode());
                  promotionElement.getQualificationRules().add(promoCodeRule);
                } else {
                  promoCodeRule.setValue(promotion.getPromoCode());
                }
              } else {
                RuleHelper.removePromoCodeRule(promotionElement);
              }
              
              int qualRuleIndex = 0;
              
              // write QUAL rules
              if (promotionElement.getQualificationRules() != null) {
                final Iterator<Rule> qualRulesI = promotionElement.getQualificationRules().iterator();
                
                while (qualRulesI.hasNext()) {
                  final BaseRule rule = (BaseRule) qualRulesI.next();
                  saveRule(promoRepository, rule, RuleHelper.QUALIFICATION_RULE, rules, qualRuleIndex);
                  qualRuleIndex++;
                  
                  // setup this promotion element as an advertise promo
                  if (rule instanceof PromoKeyRule) {
                    csrPromotionsHelper.updateAdvertisePromotion(rule.getValue(), promotionElement.getName(), promotion.getMarketingStartDate(), promotion.getMarketingEndDate());
                  }
                }
              }
              
              // write AGGREGATE rules - these are qual rules, we just keep a separate list for evaluation apart from qual eval
              if (promotionElement.getAggregateRules() != null) {
                final Iterator<Rule> aggregateRulesI = promotionElement.getAggregateRules().iterator();
                while (aggregateRulesI.hasNext()) {
                  final BaseRule rule = (BaseRule) aggregateRulesI.next();
                  saveRule(promoRepository, rule, RuleHelper.QUALIFICATION_RULE, rules, qualRuleIndex);
                  qualRuleIndex++;
                }
              }
              
              // write APPLY rules
              if (promotionElement.getApplicationRules() != null) {
                final Iterator<Rule> applyRulesI = promotionElement.getApplicationRules().iterator();
                int applyRuleIndex = 0;
                while (applyRulesI.hasNext()) {
                  final BaseRule rule = (BaseRule) applyRulesI.next();
                  saveRule(promoRepository, rule, RuleHelper.APPLICATION_RULE, rules, applyRuleIndex);
                  applyRuleIndex++;
                  
                  // setup this promotion element as an advertise promo
                  if (rule instanceof PromoKeyRule) {
                    csrPromotionsHelper.updateAdvertisePromotion(rule.getValue(), promotionElement.getName(), promotion.getMarketingStartDate(), promotion.getMarketingEndDate());
                  }
                }
              }
              
              // write AWARDS
              if (promotionElement.getAwards() != null) {
                final Iterator<BaseAward> awardsI = promotionElement.getAwards().iterator();
                while (awardsI.hasNext()) {
                  final BaseAward award = awardsI.next();
                  
                  boolean isNewAward = false;
                  MutableRepositoryItem awardRI = null;
                  if (award.getId() == null) {
                    awardRI = promoRepository.createItem("award");
                    isNewAward = true;
                  } else {
                    awardRI = promoRepository.getItemForUpdate(award.getId(), "award");
                  }
                  
                  if (awardRI != null) {
                    awardRI.setPropertyValue("type", new Integer(award.getType()));
                    awardRI.setPropertyValue("value", award.getValue());
                    
                    // //write or update award
                    if (isNewAward) {
                      // add new award
                      promoRepository.addItem(awardRI);
                    } else {
                      // update existing award
                      promoRepository.updateItem(awardRI);
                    }
                    
                    awards.add(awardRI);
                  }
                }
              }
              
              promotionElementRI.setPropertyValue("rules", rules);
              promotionElementRI.setPropertyValue("awards", awards);
              
              // //write or update promotion element
              if (isNewPromotionElement) {
                // add new promo element
                promoRepository.addItem(promotionElementRI);
              } else {
                // update existing award
                promoRepository.updateItem(promotionElementRI);
              }
              
              promotionElement.setId(promotionElementRI.getRepositoryId());
              promotionElements.add(promotionElementRI);
            }
          }
        }
        
        promotionRI.setPropertyValue("promotionElements", promotionElements);
        /* MasterPass Phase-II Changes Starts here */
        // Set checkoutTypes property with set of SystemSpecs repository items
        final Set<RepositoryItem> checkoutTypes = CommonComponentHelper.getNMCheckoutTypeUtil().getSelectedCheckoutTypesRepositoryItems(promotion);
        if (null != promotionRI.getPropertyValue(CHECKOUT_TYPES)) {
          promotionRI.setPropertyValue(CHECKOUT_TYPES, null);
        }
        promotionRI.setPropertyValue(CHECKOUT_TYPES, checkoutTypes);
        /* MasterPass Phase-II Changes ends here */
        // //write or update promotion
        if (isNewPromotion) {
          // add new promo element
          promoRepository.addItem(promotionRI);
        } else {
          // update existing award
          promoRepository.updateItem(promotionRI);
        }
        
        promotion = getPromotion(promotionRI.getRepositoryId());
        rulesBasedPromotionArray.archivePromotion(promotion, action, promotion.getUser());
        
        // clean up any removed promo keys
        final Iterator<String> i = promoKeysToCleanup.iterator();
        while (i.hasNext()) {
          rulesBasedPromotionArray.cleanAdvertisePromotion(i.next(), promotion.getUser(), false);
        }
        
        // dynamic promotions: if the promotion is dynamic, generate a seed and insert a record to the dynamic promotions table
        if (isNewPromotion && promotion.getDynamicFlag()) {
          final MutableRepository dynamicPromoRepository = (MutableRepository) Nucleus.getGlobalNucleus().resolveName("/nm/xml/CsrPromotionsRepository");
          final MutableRepositoryItem dynamicPromotionRI = dynamicPromoRepository.createItem("dynamicPromotion");
          dynamicPromotionRI.setPropertyValue("promotionId", promotionRI.getPropertyValue("id"));
          dynamicPromotionRI.setPropertyValue("type", "" + promotion.getClassification());
          dynamicPromotionRI.setPropertyValue("generationSeed", DynamicCodeUtils.generateSeed(DynamicCodeUtils.SEED_LENGTH));
          dynamicPromoRepository.addItem(dynamicPromotionRI);
        }
        
        return true;
      }
    } catch (final RepositoryException e) {
      rollback = true;
      if (mLogging.isLoggingError()) {
        mLogging.logError("RepositoryException: RulesBasedPromotionService.java in savePromotion(): ", e);
      }
      throw e;
    } catch (final TransactionDemarcationException e) {
      rollback = true;
      if (mLogging.isLoggingError()) {
        mLogging.logError("TransactionDemarcationException: RulesBasedPromotionService.java in savePromotion(): ", e);
      }
      throw e;
    } catch (final Exception e) {
      rollback = true;
      if (mLogging.isLoggingError()) {
        mLogging.logError("Exception: RulesBasedPromotionService.java in savePromotion(): ", e);
      }
      throw new Exception("There was an error saving the promotion");
    } finally {
      
      try {
        trans.end(rollback);
      } catch (final Exception e) {
        if (mLogging.isLoggingError()) {
          mLogging.logError("Exception ending TransactionDemarcation: RulesBasedPromotionService.java in savePromotion(): ", e);
        }
      }
    }
    
    return false;
  }
  
  private void saveRule(final MutableRepository promoRepository, final BaseRule rule, final int ruleType, final Set<MutableRepositoryItem> rules, final int sequenceNum) throws Exception {
    boolean isNewRule = false;
    MutableRepositoryItem ruleRI = null;
    if (rule.getId() == null) {
      ruleRI = promoRepository.createItem("rule");
      isNewRule = true;
    } else {
      ruleRI = promoRepository.getItemForUpdate(rule.getId(), "rule");
    }
    
    if (ruleRI != null) {
      ruleRI.setPropertyValue("classification", new Integer(ruleType));
      ruleRI.setPropertyValue("type", new Integer(rule.getType()));
      ruleRI.setPropertyValue("valueComparator", new Integer(rule.getValueComparator()));
      ruleRI.setPropertyValue("value", rule.getValue());
      ruleRI.setPropertyValue("sequenceNum", new Integer(sequenceNum));
      
      // //write or update qual rule
      if (isNewRule) {
        // add new rule
        promoRepository.addItem(ruleRI);
      } else {
        // update existing rule
        promoRepository.updateItem(ruleRI);
      }
      
      rules.add(ruleRI);
    }
  }
  
  public boolean deletePromotion(final String promotionId, final String user, final CSRPromotionsHelper csrPromotionsHelper) throws Exception {
    return rulesBasedPromotionArray.deletePromotion(promotionId, user);
  }
  
  public HashMap<String, Object> getMessageAttributes(final Promotion promotion, final PromotionElement element, final String action, final String user, final String userName) {
    
    final HashMap<String, Object> parameters = new HashMap<String, Object>();
    final String brandCode = ComponentUtils.getInstance().getSystemSpecs().getProductionSystemCode();
    
    parameters.put(PromotionAttributeTypes.ATTR_key, nullCheck(element.getId()));
    parameters.put(PromotionAttributeTypes.ATTR_action, nullCheck(action));
    parameters.put(PromotionAttributeTypes.ATTR_subject, "DOLLAR OFF PROMO has been added, deleted or updated");
    parameters.put(PromotionAttributeTypes.ATTR_promotype, PromotionAttributeTypes.PROMOTYPE_DOLLAR_OFF);
    parameters.put(PromotionAttributeTypes.ATTR_signature, user + " " + userName);
    parameters.put(PromotionAttributeTypes.ATTR_name, nullCheck(promotion.getName()) + " : " + nullCheck(element.getName()));
    parameters.put(PromotionAttributeTypes.ATTR_onetime, "");
    parameters.put(PromotionAttributeTypes.ATTR_comments, nullCheck(promotion.getComments()));
    
    parameters.put(PromotionAttributeTypes.ATTR_startdate, promotion.getStartDate());
    parameters.put(PromotionAttributeTypes.ATTR_enddate, promotion.getEndDate());
    parameters.put(PromotionAttributeTypes.ATTR_markdate, promotion.getMarketingStartDate());
    parameters.put(PromotionAttributeTypes.ATTR_markenddate, promotion.getMarketingEndDate());
    
    if (promotion.getClassification() == 1) {
      parameters.put(PromotionAttributeTypes.ATTR_tieredstackedflag, "");
    } else if (promotion.getClassification() == 2) {
      parameters.put(PromotionAttributeTypes.ATTR_tieredstackedflag, "T");
    } else if (promotion.getClassification() == 3) {
      parameters.put(PromotionAttributeTypes.ATTR_tieredstackedflag, "S");
    } else {
      parameters.put(PromotionAttributeTypes.ATTR_tieredstackedflag, "");
    }
    if (promotion.getLimitPromoUse()) {
      parameters.put(PromotionAttributeTypes.ATTR_limitMaxUse, 1);
    } else {
      parameters.put(PromotionAttributeTypes.ATTR_limitMaxUse, 0);
    }
    
    parameters.put(PromotionAttributeTypes.ATTR_promocode, "");
    parameters.put(PromotionAttributeTypes.ATTR_dollar, "");
    parameters.put(PromotionAttributeTypes.ATTR_orderdollar, "");
    parameters.put(PromotionAttributeTypes.ATTR_dollaroff, "");
    
    if (promotion.getDynamicFlag()) {
      parameters.put(PromotionAttributeTypes.ATTR_dynamicFlag, 1);
    } else {
      parameters.put(PromotionAttributeTypes.ATTR_dynamicFlag, 0);
    }
    
    if (promotion.getSpendMoreFlag()) {
      parameters.put(PromotionAttributeTypes.ATTR_spendMoreFlag, 1);
    } else {
      parameters.put(PromotionAttributeTypes.ATTR_spendMoreFlag, 0);
    }
    
    final List<Rule> rules = element.getAllRules();
    
    // when there is a promokey rule in the qualified set of rule, we always need to populated the ATTR_dollar (filtered) value
    // from a dollar hurdle rule (either order total OR qualified total)
    // if there is NO promokey rule in the qualified set, we ALWAYS send the dollar hurdle in the ATTR_orderdollar field
    
    boolean elementHasPromoKeyQualifier = false;
    if (RuleHelper.getPromoKeyQualificationRule(element) != null) {
      elementHasPromoKeyQualifier = true;
    }
    
    if (rules != null) {
      final Iterator<Rule> i2 = rules.iterator();
      
      while (i2.hasNext()) {
        final Object rule = i2.next();
        
        if (rule instanceof com.nm.commerce.promotion.rules.PromoCodeRule) {
          final String promoCode = ((com.nm.commerce.promotion.rules.PromoCodeRule) rule).getValue();
          parameters.put(PromotionAttributeTypes.ATTR_promocode, nullCheck(promoCode));
        } else if (rule instanceof com.nm.commerce.promotion.rules.QualifiedDollarRule) {
          final String dollarQualifier = ((com.nm.commerce.promotion.rules.BaseRule) rule).getValue();
          if (elementHasPromoKeyQualifier) {
            parameters.put(PromotionAttributeTypes.ATTR_dollar, nullCheck(dollarQualifier));
          } else {
            parameters.put(PromotionAttributeTypes.ATTR_orderdollar, nullCheck(dollarQualifier));
          }
        } else if (rule instanceof com.nm.commerce.promotion.rules.OrderDollarRule) {
          final String orderDollarQualifier = ((com.nm.commerce.promotion.rules.BaseRule) rule).getValue();
          if (elementHasPromoKeyQualifier) {
            parameters.put(PromotionAttributeTypes.ATTR_dollar, nullCheck(orderDollarQualifier));
          } else {
            parameters.put(PromotionAttributeTypes.ATTR_orderdollar, nullCheck(orderDollarQualifier));
          }
        } else if (rule instanceof com.nm.commerce.promotion.rules.QualifiedItemCountRule) {
          final String itemCount = ((com.nm.commerce.promotion.rules.QualifiedItemCountRule) rule).getValue();
          parameters.put(PromotionAttributeTypes.ATTR_itemCount, nullCheck(itemCount));
        }
      }
    }
    
    final List<BaseAward> awards = element.getAwards();
    
    if (awards != null) {
      final Iterator<BaseAward> i3 = awards.iterator();
      
      while (i3.hasNext()) {
        final Object award = i3.next();
        
        if (award instanceof com.nm.commerce.promotion.awards.DollarOffAward) {
          final String dollarOff = ((com.nm.commerce.promotion.awards.DollarOffAward) award).getValue();
          parameters.put(PromotionAttributeTypes.ATTR_dollaroff, nullCheck(dollarOff));
        }
      }
    }
    
    parameters.put(PromotionAttributeTypes.ATTR_type, "");
    parameters.put(PromotionAttributeTypes.ATTR_typetext, "");
    parameters.put(PromotionAttributeTypes.ATTR_percentoff, "");
    
    parameters.put(PromotionAttributeTypes.ATTR_depiction, "");
    parameters.put(PromotionAttributeTypes.ATTR_catalog, "");
    parameters.put(PromotionAttributeTypes.ATTR_classcodes, "");
    parameters.put(PromotionAttributeTypes.ATTR_items, "");
    parameters.put(PromotionAttributeTypes.ATTR_departments, "");
    parameters.put(PromotionAttributeTypes.ATTR_vendor, "");
    
    parameters.put(PromotionAttributeTypes.ATTR_excludeclasscodes, "");
    parameters.put(PromotionAttributeTypes.ATTR_excludeitems, "");
    parameters.put(PromotionAttributeTypes.ATTR_excludedepartments, "");
    parameters.put(PromotionAttributeTypes.ATTR_excludevendor, "");
    // Added as part of setting personalized promo flag to cmos.
    parameters.put(PromotionAttributeTypes.ATTR_personalizedFlag, promotion.isPersonalizedFlag());
    return parameters;
    
  }
  
  public String nullCheck(final String s) {
    if (s == null) {
      return "";
    } else {
      return s;
    }
  }
}
