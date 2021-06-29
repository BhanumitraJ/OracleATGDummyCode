package com.nm.commerce.promotion.rules;

import static com.nm.common.INMGenericConstants.CHECKOUT;
import static com.nm.common.INMGenericConstants.TRUE_STRING;
import static com.nm.common.INMGenericConstants.ZERO;
import static com.nm.integration.MasterPassConstants.AMERICAN_EXPRESS_CHECKOUT_PAYMENT;
import static com.nm.integration.MasterPassConstants.AMEX;
import static com.nm.integration.MasterPassConstants.DINERS;
import static com.nm.integration.MasterPassConstants.DINERS_CLUB_CHECKOUT_PAYMENT;
import static com.nm.integration.MasterPassConstants.DISCOVER;
import static com.nm.integration.MasterPassConstants.DISCOVER_CHECKOUT_PAYMENT;
import static com.nm.integration.MasterPassConstants.MASTER_CARD;
import static com.nm.integration.MasterPassConstants.MASTER_CARD_CHECKOUT_PAYMENT;
import static com.nm.integration.MasterPassConstants.MASTER_PASS_CHECKOUT_PAYMENT;
import static com.nm.integration.MasterPassConstants.MP;
import static com.nm.integration.MasterPassConstants.MP_CHECKOUT_PAYMENT;
import static com.nm.integration.MasterPassConstants.NMBG_CHECKOUT_PAYMENT;
import static com.nm.integration.MasterPassConstants.NM_BG;
import static com.nm.integration.MasterPassConstants.NM_GENERIC_SYSTEM_SPECS;
import static com.nm.integration.MasterPassConstants.PAYPAL;
import static com.nm.integration.MasterPassConstants.PAYPAL_CHECKOUT_PAYMENT;
import static com.nm.integration.MasterPassConstants.SHOP_RUNNER;
import static com.nm.integration.MasterPassConstants.SHOP_RUNNER_CHECKOUT_PAYMENT;
import static com.nm.integration.MasterPassConstants.VISA;
import static com.nm.integration.MasterPassConstants.VISA_CHECKOUT_PAYMENT;
import static com.nm.integration.MasterPassConstants.VME;
import static com.nm.integration.MasterPassConstants.VME_CHECKOUT_PAYMENT;

import java.util.List;

import atg.core.util.StringUtils;
import atg.repository.RepositoryException;
import atg.repository.RepositoryItem;

import com.nm.commerce.NMCreditCard;
import com.nm.commerce.checkout.CheckoutComponents;
import com.nm.commerce.checkout.CheckoutConfig;
import com.nm.commerce.order.ICommerceObject;
import com.nm.commerce.promotion.NMOrderImpl;
import com.nm.common.INMGenericConstants;
import com.nm.components.CommonComponentHelper;

/**
 * This class is used to create a new rule for various Payment types like MasterPass
 * 
 */
public class CheckoutTypePromotionRule extends PaymentTypeRule {
  
  private String key;
  private String keyType;
  private static final CheckoutConfig config = CheckoutComponents.getConfig();
  
  /**
   * @return the keyType
   */
  private String getKeyType() {
    return keyType;
  }
  
  /**
   * @param keyType
   *          the keyType to set
   */
  private void setKeyType(String keyType) {
    this.keyType = keyType;
  }
  
  /**
   * @return the key
   */
  public String getKey() {
    return key;
  }
  
  /**
   * @param key
   *          the key to set
   */
  private void setKey(String key) {
    this.key = key;
  }
  
  public CheckoutTypePromotionRule() {}
  
  /**
   * 
   * This is one argument constructor which accepts input parameter as type </br> The type should be defined by the checkout types and the corresponding case statement should be called to populate the
   * required values
   * 
   * for eg: For MasterPass : type= 16, case = MASTER_PASS_RULE = 16. Thus entering the case statement and populating all the required field for MasterPass
   * 
   * Constructor CheckoutTypePromotionRule.
   * 
   * @param type
   *          the type
   */
  public CheckoutTypePromotionRule(final int type) {
    switch (type) {
      case RuleHelper.MASTER_PASS_RULE: {
        setType(type);
        setName(MASTER_PASS_CHECKOUT_PAYMENT);
        setKey(MP);
      }
      break;
      case RuleHelper.NMBG_PLCC_RULE: {
        setType(type);
        setName(NMBG_CHECKOUT_PAYMENT);
        setKey(NM_BG);
      }
      break;
      case RuleHelper.VISA_RULE: {
        setType(type);
        setName(VISA_CHECKOUT_PAYMENT);
        setKey(VISA);
      }
      break;
      case RuleHelper.MASTER_CARD_RULE: {
        setType(type);
        setName(MASTER_CARD_CHECKOUT_PAYMENT);
        setKey(MASTER_CARD);
      }
      break;
      case RuleHelper.DISCOVER_RULE: {
        setType(type);
        setName(DISCOVER_CHECKOUT_PAYMENT);
        setKey(DISCOVER);
      }
      break;
      case RuleHelper.PAYPAL_RULE: {
        setType(type);
        setName(PAYPAL_CHECKOUT_PAYMENT);
        setKey(PAYPAL);
      }
      break;
      case RuleHelper.SHOPRUNNER_RULE: {
        setType(type);
        setName(SHOP_RUNNER_CHECKOUT_PAYMENT);
        setKey(SHOP_RUNNER);
      }
      break;
      case RuleHelper.AMEX_RULE: {
        setType(type);
        setName(AMERICAN_EXPRESS_CHECKOUT_PAYMENT);
        setKey(AMEX);
      }
      break;
      case RuleHelper.DINERS_RULE: {
        setType(type);
        setName(DINERS_CLUB_CHECKOUT_PAYMENT);
        setKey(DINERS);
      }
      break;
      case RuleHelper.MP_RULE: {
        setType(type);
        setName(MP_CHECKOUT_PAYMENT);
        setKey(MP);
      }
      break;
      case RuleHelper.VME_RULE: {
        setType(type);
        setName(VME_CHECKOUT_PAYMENT);
        setKey(VME);
      }
      break;
      default: {
        if (config.isLoggingInfo()) {
          config.logInfo(this.getClass().getName() + ": The input parameter 'type' for CheckoutTypePromotionRule constructor doesn't match any of the cases " + type);
        }
      }
    }
    setValue(TRUE_STRING);
    setValueComparator(RuleHelper.EQUALS);
    setBooleanRule(true);
    setKeyType(CHECKOUT);
    setDataSource(getKey(), getKeyType());
  }
  
  /**
   * This method takes key and keyType as input parameter and get the repository item from NM_SYSTEM_SPECS_TYPE for the corresponding key and keyType</br> The repository item is then set to
   * CheckoutTypeRepositoryItem of Base Class
   * 
   * @param key
   *          the key
   * @param keyType
   *          the key type
   */
  public void setDataSource(String key, String keyType) {
    if (!StringUtils.isBlank(key) && !StringUtils.isBlank(keyType)) {
      try {
        RepositoryItem checkoutTypeItem = CommonComponentHelper.getSystemSpecsRepository().getItem(key + INMGenericConstants.COLON_STRING + keyType, NM_GENERIC_SYSTEM_SPECS);
        setCheckoutTypeRepositoryItem(checkoutTypeItem);
      } catch (RepositoryException e) {
        if (config.isLoggingError()) {
          config.logError(this.getClass().getName() + ": Error while setting the repository item ", e);
        }
      }
    }
  }
  
  /**
   * <p>
   * This method has been overridden for all Checkout Types <br/>
   * to identify if the payment group Checkout type and CSR selected checkout type are same
   * </p>
   */
  @Override
  protected boolean test(final ICommerceObject obj) {
    boolean returnValue = false;
    final NMOrderImpl order = (NMOrderImpl) obj;
    final List<NMCreditCard> orderCreditCards = order.getCreditCards();
    if (!orderCreditCards.isEmpty()) {
      // MasterPass Phase-II - just returning true, since payment type validation is checked in promotion class for (promo key in) $Off promotion
      if (RuleHelper.MASTER_PASS_RULE != getType()) {
        returnValue = true;
      } else {
        final NMCreditCard paymentGroup = orderCreditCards.get(ZERO);
        // Check if the payment group Checkout type and CSR selected checkout type are same
        if (((paymentGroup != null) && (paymentGroup.getCheckoutType() != null)) && paymentGroup.getCheckoutType().getRepositoryId().equalsIgnoreCase(getKey() + ":" + getKeyType())) {
          returnValue = true;
        }
      }
    }
    return returnValue;
  }
  
  /**
   * <p>
   * This method retrieves the rule name that would be displayed<br/>
   * in the Qualification rules dropdown in CSR
   * </p>
   */
  @Override
  public String getDisplayValue() {
    return getName();
  }
}
