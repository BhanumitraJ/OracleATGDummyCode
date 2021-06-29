package com.nm.commerce.promotion.rulesBased;

import static com.nm.common.INMGenericConstants.CHECKOUT;
import static com.nm.common.INMGenericConstants.COLON_STRING;
import static com.nm.common.INMGenericConstants.ZERO;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import atg.commerce.order.Order;
import atg.nucleus.logging.ApplicationLogging;
import atg.nucleus.logging.ClassLoggingFactory;
import atg.servlet.DynamoHttpServletRequest;
import atg.servlet.ServletUtil;

import com.nm.ajax.csr.promotion.beans.PromotionBean;
import com.nm.ajax.csr.promotion.exception.ValidationException;
import com.nm.ajax.csr.promotion.services.CsrPromotionHelper;
import com.nm.collections.NMPromotion;
import com.nm.collections.NMPromotionTypes;
import com.nm.commerce.NMCommerceItem;
import com.nm.commerce.NMProfile;
import com.nm.commerce.promotion.NMOrderImpl;
import com.nm.commerce.promotion.rules.BaseRule;
import com.nm.commerce.promotion.rules.CheckoutTypePromotionRule;
import com.nm.commerce.promotion.rules.OrderDollarRule;
import com.nm.commerce.promotion.rules.PaymentTypeRule;
import com.nm.commerce.promotion.rules.QualifiedDollarRule;
import com.nm.commerce.promotion.rules.QualifiedItemCountRule;
import com.nm.commerce.promotion.rules.Rule;
import com.nm.commerce.promotion.rules.RuleHelper;
import com.nm.components.CommonComponentHelper;
import com.nm.integration.util.NMCheckoutTypeUtil;
import com.nm.repository.ProfileRepository;
import com.nm.utils.PromotionsHelper;

public class Promotion extends NMPromotion {
  
  public Promotion() {}
  
  /** The nm checkout type util. */
  NMCheckoutTypeUtil nmCheckoutTypeUtil = CommonComponentHelper.getNMCheckoutTypeUtil();
  
  /** The logger to log the events. */
  private static ApplicationLogging mLogger = ClassLoggingFactory.getFactory().getLoggerForClass(PromotionElement.class);
  
  public Promotion(PromotionBean promotion) throws ValidationException {
    
    setName(promotion.getName());
    setId(promotion.getId());
    
    if (promotion.getId() != null) {
      setCode(promotion.getId());
    } else {
      setCode("");
    }
    
    try {
      setClassification(new Integer(promotion.getClassification()).intValue());
    } catch (Exception e) {
      e.printStackTrace();
    }
    
    setUser(promotion.getCreatingUser());
    setComments(promotion.getComments());
    setPromoCode(promotion.getPromoCodes());
    
    setStartDateMo(promotion.getPromoStartMonth());
    setStartDateDay(promotion.getPromoStartDay());
    setStartDateYear(promotion.getPromoStartYear());
    setStartDateHour(promotion.getPromoStartHour());
    setStartDateMin(promotion.getPromoStartMinute());
    
    setEndDateMo(promotion.getPromoEndMonth());
    setEndDateDay(promotion.getPromoEndDay());
    setEndDateYear(promotion.getPromoEndYear());
    setEndDateHour(promotion.getPromoEndHour());
    setEndDateMin(promotion.getPromoEndMinute());
    
    setMarketingStartDateMo(promotion.getPromoMarkMonth());
    setMarketingStartDateDay(promotion.getPromoMarkDay());
    setMarketingStartDateYear(promotion.getPromoMarkYear());
    setMarketingStartDateHour(promotion.getPromoMarkHour());
    setMarketingStartDateMin(promotion.getPromoMarkMinute());
    
    setMarketingEndDateMo(promotion.getPromoMarkEndMonth());
    setMarketingEndDateDay(promotion.getPromoMarkEndDay());
    setMarketingEndDateYear(promotion.getPromoMarkEndYear());
    setMarketingEndDateHour(promotion.getPromoMarkEndHour());
    setMarketingEndDateMin(promotion.getPromoMarkEndMinute());
    
    // Eligible Countries
    Map countries = CsrPromotionHelper.buildMapFromString(promotion.getEligibleCountries());
    setFlgEligible(countries);
    
    List<String> validationErrors = new ArrayList<String>();
    // build Date objects from form fields
    try {
      promotion.buildValidatedStartDate();
      setStartDate(promotion.getStartDate());
    } catch (ValidationException e) {
      validationErrors.addAll(e.getErrors());
    }
    try {
      promotion.buildValidatedMarketingStartDate();
      setMarketingStartDate(promotion.getMarkDate());
    } catch (ValidationException e) {
      validationErrors.addAll(e.getErrors());
    }
    try {
      promotion.buildValidatedMarketingEndDate();
      setMarketingEndDate(promotion.getMarkEndDate());
    } catch (ValidationException e) {
      validationErrors.addAll(e.getErrors());
    }
    try {
      promotion.buildValidatedEndDate();
      setEndDate(promotion.getEndDate());
    } catch (ValidationException e) {
      validationErrors.addAll(e.getErrors());
    }
    
    setPromoReinforcementFlag(new Boolean(promotion.getPromoReinforcementFlag()).booleanValue());
    setPromoReinforcementHtml(promotion.getPromoReinforcementHtml());
    if (promotion.getRequiresEmailValidation() != null) {
      setRequiresEmailValidation(promotion.getRequiresEmailValidation().booleanValue());
    }
    
    if (promotion.getLimitPromoUse() != null) {
      setLimitPromoUse(promotion.getLimitPromoUse().booleanValue());
    }
    
    if (promotion.getDynamicFlag() != null) {
      setDynamicFlag(promotion.getDynamicFlag());
    }
    
    if (promotion.isPersonalizedFlag()) {
      setPersonalizedFlag(promotion.isPersonalizedFlag());
    }

    if (promotion.getLimitPromoUse() && promotion.getDynamicFlag()) {
      validationErrors.add("Dynamic Promotions may not be limited to 1 time use.");
    }
    
    if (promotion.getSpendMoreFlag() != null) {
      setSpendMoreFlag(promotion.getSpendMoreFlag());
    }
    
    // spendMore promotions can not be require email validation, 1 time limit, dynamic
    if (promotion.getSpendMoreFlag() && (promotion.getLimitPromoUse() || promotion.getDynamicFlag() || promotion.getRequiresEmailValidation())) {
      validationErrors.add("Spend More/Save More may not be used in conjunction with 1 time use limit, dynamic, or email validation.");
    }
    setExcludeEmployeesFlag(promotion.isExcludeEmployeesFlag());
    // MasterPass Changes : Set Checkout type bean value to NMPromotion instance
    setCheckoutType(promotion.getCheckoutType());
    
    if (!validationErrors.isEmpty()) {
      throw new ValidationException(validationErrors);
    }
  }
  
  @Override
  public PromotionBean getPromotionBean() {
    PromotionBean bean = super.getPromotionBean();
    
    bean.setId(getId());
    if (getId() == null) {
      bean.setCode("");
    } else {
      bean.setCode(getId());
    }
    
    bean.setPromoKeys(getPromoKeys());
    
    bean.setClassification(getClassification() + "");
    bean.setCreatingUser(getCreatingUser());
    bean.setComments(getComments());
    
    if (getPromoCodes() != null) {
      bean.setPromoCodes(getPromoCodes());
    } else {
      bean.setPromoCodes(getPromoCode());
    }
    
    // all dollar off promos are currently type 1
    bean.setType(getType());
    
    bean.setStartDate(getStartDate());
    bean.setEndDate(getEndDate());
    bean.setMarkDate(getMarketingStartDate());
    bean.setMarkEndDate(getMarketingEndDate());
    
    bean.setPromoReinforcementFlag(new Boolean(isPromoReinforcementFlag()).toString());
    bean.setPromoReinforcementHtml(getPromoReinforcementHtml());
    bean.setEligibleCountriesMap(getFlgEligible()); // Eligible countries
    bean.setRequiresEmailValidation(new Boolean(isRequiresEmailValidation()));
    bean.setLimitPromoUse(new Boolean(getLimitPromoUse()));
    
    bean.setDynamicFlag(getDynamicFlag());
    bean.setPersonalizedFlag(isPersonalizedFlag());
    bean.setSpendMoreFlag(getSpendMoreFlag());
    
    bean.setPromotionElements(getPromotionElements());
    
    return bean;
  }
  
  public boolean hasPromotionElement(final String promotionElementId) {
    boolean hasPromotionElement = false;
    for (PromotionElement promotionElement : promotionElements) {
      if (promotionElementId.equals(promotionElement.getId())) {
        hasPromotionElement = true;
        break;
      }
    }
    
    return hasPromotionElement;
  }
  
  public Promotion copy() {
    Promotion promotion = new Promotion();
    promotion.setName(getName());
    promotion.setId(getId());
    promotion.setCode(getId());
    promotion.setClassification(getClassification());
    
    promotion.setUser(getCreatingUser());
    promotion.setComments(getComments());
    promotion.setPromoCode(getPromoCodes());
    
    promotion.setStartDate(getStartDate());
    promotion.setMarketingStartDate(getMarketingStartDate());
    promotion.setMarketingEndDate(getMarketingEndDate());
    promotion.setEndDate(getEndDate());
    promotion.setLimitPromoUse(getLimitPromoUse());
    promotion.setFlgEligible(getFlgEligible()); // Eligible countries
    setPromoReinforcementFlag(isPromoReinforcementFlag());
    setPromoReinforcementHtml(getPromoReinforcementHtml());
    setRequiresEmailValidation(isRequiresEmailValidation());
    
    promotion.setDynamicFlag(getDynamicFlag());
    promotion.setPersonalizedFlag(isPersonalizedFlag());
    promotion.setSpendMoreFlag(getSpendMoreFlag());
    promotion.setExcludeEmployeesFlag(isExcludeEmployeesFlag());
    // MasterPass Changes : Set CheckoutType
    promotion.setCheckoutType(getCheckoutType());
    List<PromotionElement> promoElements = new ArrayList<PromotionElement>();
    if (getPromotionElements() != null) {
      promoElements = new ArrayList<PromotionElement>(getPromotionElements());
    }
    setPromotionElements(promoElements);
    
    return promotion;
  }
  
  private String id;
  private String name;
  // private String code; //this is needed for non rules based support
  private int classification;
  private Date startDate;
  private Date endDate;
  private Date marketingStartDate;
  private Date marketingEndDate;
  private String user;
  private String comments;
  private boolean requiresEmailValidation;
  private boolean limitPromoUse;
  private List<PromotionElement> promotionElements;
  private String promoCode;
  private boolean promoReinforcementFlag;
  private String promoReinforcementHtml;
  
  // support form fields for dates
  private String startDateMo;
  private String startDateDay;
  private String startDateYear;
  private String startDateHour;
  private String startDateMin;
  private String endDateMo;
  private String endDateDay;
  private String endDateYear;
  private String endDateHour;
  private String endDateMin;
  private String marketingStartDateMo;
  private String marketingStartDateDay;
  private String marketingStartDateYear;
  private String marketingStartDateHour;
  private String marketingStartDateMin;
  private String marketingEndDateMo;
  private String marketingEndDateDay;
  private String marketingEndDateYear;
  private String marketingEndDateHour;
  private String marketingEndDateMin;
  private Boolean dynamicFlag;
  private Boolean personalizedFlag;
  private Boolean spendMoreFlag;
  private Map flgEligible; // Eligible countries
  
  public static final int SCHEDULED = 0;
  public static final int ACTIVE = 1;
  public static final int EXPIRED = 2;
  
  @Override
  public String getType() {
    return "1";
  }
  
  public String getAward() {
    return CsrPromotionHelper.DOLLAR_OFF + "";
  }
  
  public String getCreatingUser() {
    return getUser();
  }
  
  public boolean evaluate(NMOrderImpl order) {
    
    boolean isAwarded = false;
    boolean passLimitPromo = true;
    
    String promoCodes = this.getPromoKeys().toUpperCase();
    if (order.getUsedLimitPromoMap().contains(this.getCode()) || order.getUsedLimitPromoMap().contains(promoCodes)) {
      passLimitPromo = false;
    }
    
    Set<NMCommerceItem> qualifiedItems = null;
    // a Promotion may contain more than one PromotionElements
    // based on the classification, we evaluate qualify logic
    switch (classification) {
    
    // standalone - evaluate the only promo element
      case 1: {
        // System.out.println("");
        // System.out.println("-- STANDALONE --");
        if ((promotionElements != null) && !promotionElements.isEmpty()) {
          
          PromotionElement promoElement = promotionElements.get(0);
          if ((promoElement != null) && passLimitPromo) {
            List<Rule> qualificationRules = promoElement.getQualificationRules();
            // MasterPass Phase-II- checks promotion is promo code key in type.
            if (nmCheckoutTypeUtil.ispromoCodeApplies(qualificationRules)) {
              if (isPaymentTypeMatches(order, qualificationRules)) {
                try {
                  PromotionsHelper promotionsHelper = CommonComponentHelper.getPromotionsHelper();
                  if (promotionsHelper.promoCodeMatch((Order) order, getPromoCodes())) {
                    if (isLoggingDebug()) {
                      logDebug("Promotion key matches with promo code user entered : " + getName());
                    }
                    qualifiedItems = promoElement.qualify(order);
                  }
                } catch (Exception exception) {
                  if (mLogger.isLoggingError()) {
                    mLogger.logError("Error while checking codes matches", exception);
                  }
                }
              }
            } else {
              // Auto applied promotion
              qualifiedItems = promoElement.qualify(order);
            }
            
            // To Check whether the user country eligible for the dollar off promotions
            if (validateEligibleCountry(isAwarded)) {
              isAwarded = promoElement.award(order, qualifiedItems);
            }
          }
        }
        break;
      }
      
      // tiered - evaluate all promo elements, but stop after the first qualification
      case 2: {
        // System.out.println("");
        // System.out.println("-- TIERED --");
        if ((promotionElements != null) && !promotionElements.isEmpty()) {
          Iterator<PromotionElement> i = promotionElements.iterator();
          while (i.hasNext()) {
            PromotionElement promoElement = i.next();
            
            if ((promoElement != null) && passLimitPromo) {
              qualifiedItems = promoElement.qualify(order);
              
              // To Check whether the user country eligible for the dollar off promotions
              if (validateEligibleCountry(isAwarded)) {
                
                isAwarded = promoElement.award(order, qualifiedItems);
                
                if (isAwarded) {
                  break;
                }
                
              }
            }
          }
        }
        break;
      }
      
      // stacked - evaluate all promo elements and apply those which qualify
      case 3: {
        // System.out.println("");
        // System.out.println("-- STACKED --");
        boolean awardedAtLeastOneInStack = false;
        if ((promotionElements != null) && !promotionElements.isEmpty()) {
          Iterator<PromotionElement> i = promotionElements.iterator();
          while (i.hasNext()) {
            PromotionElement promoElement = i.next();
            if ((promoElement != null) && passLimitPromo) {
              qualifiedItems = promoElement.qualify(order);
              
              // To Check whether the user country eligible for the dollar off promotions
              if (validateEligibleCountry(isAwarded)) {
                
                isAwarded = promoElement.award(order, qualifiedItems);
                
                if (isAwarded) {
                  awardedAtLeastOneInStack = true;
                }
                
              }
            }
          }
          isAwarded = awardedAtLeastOneInStack;
        }
        break;
      }
    }
    
    updatePromoCodesOnOrder(order, this, isAwarded);
    if (isAwarded) {
      addPromotionToOrder(order);
    }
    
    return isAwarded;
  }
  
  public boolean evaluateForSpendMoreSaveMore(NMOrderImpl order) {
    boolean qualifiesForSpendMoreSaveMore = false;
    String promoCodes = this.getPromoKeys().toUpperCase();

    if (order.getUsedLimitPromoMap().contains(this.getCode()) || order.getUsedLimitPromoMap().contains(promoCodes)) {
      return qualifiesForSpendMoreSaveMore;
    }
    
    try {
      PromotionsHelper promotionsHelper = CommonComponentHelper.getPromotionsHelper();
    
      switch (classification) {
      // standalone: there must be a order dollar total rule, a qualifying item rule or an item count rule
      // on the promotion to qualify for spend more save more messaging
        case 1: {
          if ((promotionElements != null) && !promotionElements.isEmpty()) {
            PromotionElement promoElement = promotionElements.get(0);

            if (promoElement != null) {
              if (promotionsHelper.orderHasProd(order, promoElement.getId())) {
                if (RuleHelper.getOrderDollarRules(promoElement).size() > 0 ||  // ex: dollars spent on order is at least x
                    RuleHelper.getQualifiedDollarRules(promoElement).size() > 0 ) {  // ex: dollars spent on qualified items is at least x
                  qualifiesForSpendMoreSaveMore = true;
                  break;
                }
              }
            }
          }
          break;
        }
        // tiered, stacked: if ANY promotion element has an order dollar rule, a qualified dollar rule or
        // an item count rule that is not met, promotion is a candidate for spend more messaging
        case 2: 
        case 3: {
          if ((promotionElements != null) && !promotionElements.isEmpty()) {
            Iterator<PromotionElement> promoElementIter = promotionElements.iterator();
            while (promoElementIter.hasNext()) {
              PromotionElement promoElement = promoElementIter.next();

              if (promoElement != null) {
                if (promotionsHelper.orderHasProd(order, promoElement.getId())) {
                  if (RuleHelper.getOrderDollarRules(promoElement).size() > 0 ||  // ex: dollars spent on order is at least x
                      RuleHelper.getQualifiedDollarRules(promoElement).size() > 0 ) {  // ex: dollars spent on qualified items is at least x
                    qualifiesForSpendMoreSaveMore = true;
                    break;
                  }
                }
              }
            }
          }
          break;
        }
      }
    } catch (Exception e) { }
    
    return qualifiesForSpendMoreSaveMore;
  }
  
  /**
   * Checks to see if the COUNTRY_PREFERENCE qualifies for the user to grant DOLLAR OFF PROMOTIONS. Supports both Neiman Marcus(US and Cannada) and FiftyOne countries Basically to check the country
   * eligibility for the CSR dollar off promotions to Apply
   * 
   * @param isAwarded
   *          boolean
   * @return
   */
  public boolean validateEligibleCountry(boolean isAwarded) {
    
    DynamoHttpServletRequest request = ServletUtil.getCurrentRequest();
    if (request != null) {
      
      NMProfile nmProfile = (NMProfile) request.resolveName("/atg/userprofiling/Profile");
      String country = (String) nmProfile.getPropertyValue(ProfileRepository.COUNTRY_PREFERENCE);
      
      // map of countries, key is country code, value is boolean
      Map<String, Boolean> countryEligibilityMap = this.getFlgEligible();
      
      // if countryEligibilityMap is not null AND countryEligibilityMap contains key country AND key value is true, then OFFER promo
      if ((countryEligibilityMap != null) && countryEligibilityMap.containsKey(country)) {
        
        Boolean eligibility = countryEligibilityMap.get(country);
        
        // Contains key country AND key value is false, then do not OFFER promo
        if (!eligibility) {
          return false;
        }
      }
    }
    return true;
  }
  
  private void addPromotionToOrder(NMOrderImpl order) {
    
    order.addAwardedPromotion(this);
    
    // does this order attribute need to be set?
    // order.getPriceInfo().setDiscounted(true);
    
  }
  
  private void updatePromoCodesOnOrder(Order order, Promotion promotion, boolean awarded) {
    
    String promoCode = RuleHelper.getPromoCode(promotion);
    
    if ((promoCode != null) && !promoCode.equals("")) {
      
      if (awarded) {
        ((NMOrderImpl) order).setActivatedPromoCode(promoCode);
        ((NMOrderImpl) order).setUserActivatedPromoCode(promoCode);
      } else {
        ((NMOrderImpl) order).setRemoveActivatedPromoCode(promoCode);
        ((NMOrderImpl) order).setRemoveUserActivatedPromoCode(promoCode);
      }
    }
    
    try {
      RuleHelper.getOrderManager().updateOrder(order);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  
  public String getClassificationDisplay() {
    return RuleHelper.getClassificationDisplay(classification);
  }
  
  public boolean getIsActive() {
    Date now = new Date();
    if (startDate.before(now) && endDate.after(now)) {
      return true;
    }
    return false;
  }
  
  public boolean getIsExpired() {
    Date now = new Date();
    if (endDate.before(now)) {
      return true;
    }
    return false;
  }
  
  public boolean getIsScheduled() {
    Date now = new Date();
    if (startDate.after(now)) {
      return true;
    }
    return false;
  }
  
  public int getStatus() {
    if (getIsScheduled()) {
      return SCHEDULED;
    }
    if (getIsActive()) {
      return ACTIVE;
    }
    if (getIsExpired()) {
      return EXPIRED;
    }
    return -1;
  }
  
  public String getStartDateDisplay() {
    if (startDate == null) {
      return "";
    }
    SimpleDateFormat format = new SimpleDateFormat("EEEEE, MM/dd/yyyy HH:mm");
    return format.format(startDate);
  }
  
  public String getEndDateDisplay() {
    if (endDate == null) {
      return "";
    }
    SimpleDateFormat format = new SimpleDateFormat("EEEEE, MM/dd/yyyy HH:mm");
    return format.format(endDate);
  }
  
  public String getEscapedComments() {
    String newComments = getComments();
    
    if (newComments != null) {
      newComments = newComments.replaceAll("'", "\\\\'");
      newComments = newComments.replaceAll("\n", " ");
      newComments = newComments.replaceAll("\r", " ");
    }
    
    return newComments;
  }
  
  public String getId() {
    return id;
  }
  
  public void setId(String id) {
    this.id = id;
  }
  
  @Override
  public String getName() {
    return name;
  }
  
  public void setName(String name) {
    this.name = name;
  }
  
  public int getClassification() {
    return classification;
  }
  
  public void setClassification(int classification) {
    this.classification = classification;
  }
  
  @Override
  public Date getStartDate() {
    return startDate;
  }
  
  public void setStartDate(Date startDate) {
    this.startDate = startDate;
  }
  
  @Override
  public Date getEndDate() {
    return endDate;
  }
  
  public void setEndDate(Date endDate) {
    this.endDate = endDate;
  }
  
  @Override
  public Date getMarketingStartDate() {
    return marketingStartDate;
  }
  
  public void setMarketingStartDate(Date marketingStartDate) {
    this.marketingStartDate = marketingStartDate;
  }
  
  @Override
  public Date getMarketingEndDate() {
    return marketingEndDate;
  }
  
  public void setMarketingEndDate(Date marketingEndDate) {
    this.marketingEndDate = marketingEndDate;
  }
  
  public String getUser() {
    return user;
  }
  
  public void setUser(String user) {
    this.user = user;
  }
  
  public String getComments() {
    return comments;
  }
  
  public void setComments(String comments) {
    this.comments = comments;
  }
  
  public List<PromotionElement> getPromotionElements() {
    return promotionElements;
  }
  
  public void setPromotionElements(List<PromotionElement> promotionElements) {
    this.promotionElements = promotionElements;
  }
  
  public String getPromoKeys() {
    return RuleHelper.getPromoKeys(this);
  }
  
  @Override
  public String getPromoCodes() {
    return RuleHelper.getPromoCode(this);
  }
  
  @Override
  public int getPromotionClass() {
    return NMPromotionTypes.RULE_BASED;
  }
  
  @Override
  public String getCode() {
    if (id != null) {
      return id;
    }
    return "";
  }
  
  public boolean isLimitPromoUse() {
    return limitPromoUse;
  }
  
  @Override
  public boolean getLimitPromoUse() {
    return limitPromoUse;
  }
  
  @Override
  public void setLimitPromoUse(boolean limitPromoUse) {
    this.limitPromoUse = limitPromoUse;
  }
  
  @Override
  public boolean requiresEmailValidation() {
    return isRequiresEmailValidation();
  }
  
  public boolean isRequiresEmailValidation() {
    return requiresEmailValidation;
  }
  
  public void setRequiresEmailValidation(boolean requiresEmailValidation) {
    this.requiresEmailValidation = requiresEmailValidation;
  }
  
  public String getPromoCode() {
    return this.promoCode;
  }
  
  public void setPromoCode(String promoCode) {
    if ((promoCode != null) && promoCode.equals("")) {
      promoCode = null;
    }
    this.promoCode = promoCode;
  }
  
  @Override
  public boolean isPromoReinforcementFlag() {
    return promoReinforcementFlag;
  }
  
  public void setPromoReinforcementFlag(boolean promoReinforcementFlag) {
    this.promoReinforcementFlag = promoReinforcementFlag;
  }
  
  @Override
  public String getPromoReinforcementHtml() {
    return promoReinforcementHtml;
  }
  
  public void setPromoReinforcementHtml(String promoReinforcementHtml) {
    this.promoReinforcementHtml = promoReinforcementHtml;
  }
  
  /**
   * @return the startDateMo
   */
  public String getStartDateMo() {
    return startDateMo;
  }
  
  /**
   * @param startDateMo
   *          the startDateMo to set
   */
  public void setStartDateMo(String startDateMo) {
    this.startDateMo = startDateMo;
  }
  
  /**
   * @return the startDateDay
   */
  public String getStartDateDay() {
    return startDateDay;
  }
  
  /**
   * @param startDateDay
   *          the startDateDay to set
   */
  public void setStartDateDay(String startDateDay) {
    this.startDateDay = startDateDay;
  }
  
  /**
   * @return the startDateYear
   */
  public String getStartDateYear() {
    return startDateYear;
  }
  
  /**
   * @param startDateYear
   *          the startDateYear to set
   */
  public void setStartDateYear(String startDateYear) {
    this.startDateYear = startDateYear;
  }
  
  /**
   * @return the startDateHour
   */
  public String getStartDateHour() {
    return startDateHour;
  }
  
  /**
   * @param startDateHour
   *          the startDateHour to set
   */
  public void setStartDateHour(String startDateHour) {
    this.startDateHour = startDateHour;
  }
  
  /**
   * @return the startDateMin
   */
  public String getStartDateMin() {
    return startDateMin;
  }
  
  /**
   * @param startDateMin
   *          the startDateMin to set
   */
  public void setStartDateMin(String startDateMin) {
    this.startDateMin = startDateMin;
  }
  
  /**
   * @return the endDateMo
   */
  public String getEndDateMo() {
    return endDateMo;
  }
  
  /**
   * @param endDateMo
   *          the endDateMo to set
   */
  public void setEndDateMo(String endDateMo) {
    this.endDateMo = endDateMo;
  }
  
  /**
   * @return the endDateDay
   */
  public String getEndDateDay() {
    return endDateDay;
  }
  
  /**
   * @param endDateDay
   *          the endDateDay to set
   */
  public void setEndDateDay(String endDateDay) {
    this.endDateDay = endDateDay;
  }
  
  /**
   * @return the endDateYear
   */
  public String getEndDateYear() {
    return endDateYear;
  }
  
  /**
   * @param endDateYear
   *          the endDateYear to set
   */
  public void setEndDateYear(String endDateYear) {
    this.endDateYear = endDateYear;
  }
  
  /**
   * @return the endDateHour
   */
  public String getEndDateHour() {
    return endDateHour;
  }
  
  /**
   * @param endDateHour
   *          the endDateHour to set
   */
  public void setEndDateHour(String endDateHour) {
    this.endDateHour = endDateHour;
  }
  
  /**
   * @return the endDateMin
   */
  public String getEndDateMin() {
    return endDateMin;
  }
  
  /**
   * @param endDateMin
   *          the endDateMin to set
   */
  public void setEndDateMin(String endDateMin) {
    this.endDateMin = endDateMin;
  }
  
  /**
   * @return the marketingStartDateMo
   */
  public String getMarketingStartDateMo() {
    return marketingStartDateMo;
  }
  
  /**
   * @param marketingStartDateMo
   *          the marketingStartDateMo to set
   */
  public void setMarketingStartDateMo(String marketingStartDateMo) {
    this.marketingStartDateMo = marketingStartDateMo;
  }
  
  /**
   * @return the marketingStartDateDay
   */
  public String getMarketingStartDateDay() {
    return marketingStartDateDay;
  }
  
  /**
   * @param marketingStartDateDay
   *          the marketingStartDateDay to set
   */
  public void setMarketingStartDateDay(String marketingStartDateDay) {
    this.marketingStartDateDay = marketingStartDateDay;
  }
  
  /**
   * @return the marketingStartDateYear
   */
  public String getMarketingStartDateYear() {
    return marketingStartDateYear;
  }
  
  /**
   * @param marketingStartDateYear
   *          the marketingStartDateYear to set
   */
  public void setMarketingStartDateYear(String marketingStartDateYear) {
    this.marketingStartDateYear = marketingStartDateYear;
  }
  
  /**
   * @return the marketingStartDateHour
   */
  public String getMarketingStartDateHour() {
    return marketingStartDateHour;
  }
  
  /**
   * @param marketingStartDateHour
   *          the marketingStartDateHour to set
   */
  public void setMarketingStartDateHour(String marketingStartDateHour) {
    this.marketingStartDateHour = marketingStartDateHour;
  }
  
  /**
   * @return the marketingStartDateMin
   */
  public String getMarketingStartDateMin() {
    return marketingStartDateMin;
  }
  
  /**
   * @param marketingStartDateMin
   *          the marketingStartDateMin to set
   */
  public void setMarketingStartDateMin(String marketingStartDateMin) {
    this.marketingStartDateMin = marketingStartDateMin;
  }
  
  /**
   * @return the marketingEndDateMo
   */
  public String getMarketingEndDateMo() {
    return marketingEndDateMo;
  }
  
  /**
   * @param marketingEndDateMo
   *          the marketingEndDateMo to set
   */
  public void setMarketingEndDateMo(String marketingEndDateMo) {
    this.marketingEndDateMo = marketingEndDateMo;
  }
  
  /**
   * @return the marketingEndDateDay
   */
  public String getMarketingEndDateDay() {
    return marketingEndDateDay;
  }
  
  /**
   * @param marketingEndDateDay
   *          the marketingEndDateDay to set
   */
  public void setMarketingEndDateDay(String marketingEndDateDay) {
    this.marketingEndDateDay = marketingEndDateDay;
  }
  
  /**
   * @return the marketingEndDateYear
   */
  public String getMarketingEndDateYear() {
    return marketingEndDateYear;
  }
  
  /**
   * @param marketingEndDateYear
   *          the marketingEndDateYear to set
   */
  public void setMarketingEndDateYear(String marketingEndDateYear) {
    this.marketingEndDateYear = marketingEndDateYear;
  }
  
  /**
   * @return the marketingEndDateHour
   */
  public String getMarketingEndDateHour() {
    return marketingEndDateHour;
  }
  
  /**
   * @param marketingEndDateHour
   *          the marketingEndDateHour to set
   */
  public void setMarketingEndDateHour(String marketingEndDateHour) {
    this.marketingEndDateHour = marketingEndDateHour;
  }
  
  /**
   * @return the marketingEndDateMin
   */
  public String getMarketingEndDateMin() {
    return marketingEndDateMin;
  }
  
  /**
   * @param marketingEndDateMin
   *          the marketingEndDateMin to set
   */
  public void setMarketingEndDateMin(String marketingEndDateMin) {
    this.marketingEndDateMin = marketingEndDateMin;
  }
  
  /**
   * @param code
   *          the code to set
   */
  @Override
  public void setCode(String code) {
    // this.code = code;
  }
  
  public Boolean getDynamicFlag() {
    return dynamicFlag;
  }
  
  public Boolean getIsDynamic() {
    return dynamicFlag;
  }
  
  public void setDynamicFlag(Boolean dynamicFlag) {
    this.dynamicFlag = dynamicFlag;
  }
  
  
  public Boolean getSpendMoreFlag() {
    return spendMoreFlag;
  }
  
  public Boolean getIsSpendMore() {
    return spendMoreFlag;
  }
  
  public void setSpendMoreFlag(Boolean spendMoreFlag) {
    this.spendMoreFlag = spendMoreFlag;
  }
  
  public Map getFlgEligible() {
    return flgEligible;
  }
  
  public void setFlgEligible(Map flgEligible) {
    this.flgEligible = flgEligible;
  }
  
  /**
   * Checks if promotion needs promo code key in and order payment type matches with payment types selected in promotion.
   * 
   * @param order
   *          the order
   * @param qualificationRules
   *          the qualification rules
   * @return true, if is promo code applies and payment type matches
   */
  public boolean isPaymentTypeMatches(final NMOrderImpl order, List<Rule> qualificationRules) {
    int currentRuleType = ZERO;
    List<String> selectedCheckoutType = new ArrayList<String>();
    for (Rule rule : qualificationRules) {
      currentRuleType = ((BaseRule) rule).getType();
      if ((rule instanceof PaymentTypeRule) && ((RuleHelper.MASTER_PASS_RULE != currentRuleType) && (RuleHelper.VISA_CHECKOUT_RULE != currentRuleType))) {
        selectedCheckoutType.add(((CheckoutTypePromotionRule) rule).getKey() + COLON_STRING + CHECKOUT);
      }
    }
    return nmCheckoutTypeUtil.checkSelectedPaymentTypePresentInPromotion(order, selectedCheckoutType);
  }
  
}
