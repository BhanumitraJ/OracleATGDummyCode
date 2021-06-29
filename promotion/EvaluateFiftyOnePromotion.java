package com.nm.commerce.promotion;

import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import atg.commerce.order.Order;
import atg.commerce.order.OrderHolder;
import atg.commerce.order.OrderManager;
import atg.dtm.TransactionDemarcation;
import atg.dtm.TransactionDemarcationException;
import atg.nucleus.Nucleus;
import atg.process.ProcessException;
import atg.process.ProcessExecutionContext;
import atg.process.action.ActionImpl;
import atg.repository.RepositoryException;
import atg.repository.RepositoryItem;
import atg.servlet.DynamoHttpServletRequest;
import atg.servlet.ServletUtil;

import com.nm.collections.FiftyOnePromotion;
import com.nm.collections.FiftyOnePromotionArray;
import com.nm.collections.NMPromotion;
import com.nm.commerce.NMProfile;
import com.nm.repository.ProfileRepository;
import com.nm.utils.ExceptionUtil;
import com.nm.utils.LocalizationUtils;
import com.nm.utils.PromotionsHelper;
import com.nm.utils.datasource.DataSourceUtil;

public class EvaluateFiftyOnePromotion extends ActionImpl implements IPromoAction {
  private static final String SHOPPING_CART_PATH = "/atg/commerce/ShoppingCart";
  private static final String ORDERMANAGER_PATH = "/atg/commerce/order/OrderManager";
  private static final String PROMOTION_ARRAY_PATH = "/nm/collections/FiftyOnePromotionArray";
  
  private FiftyOnePromotionArray mPromotionArray = null;
  private boolean logDebug = true;
  private boolean logError = true;
  private PromotionsHelper mPromotionsHelper = null;
  
  private static FiftyOnePromotionArray getPromotionArray() {
    return ((FiftyOnePromotionArray) Nucleus.getGlobalNucleus().resolveName(PROMOTION_ARRAY_PATH));
  }
  
  public void initialize(Map pParameters) throws ProcessException {
    mPromotionArray = getPromotionArray();
    mPromotionsHelper = (PromotionsHelper) Nucleus.getGlobalNucleus().resolveName("/nm/utils/PromotionsHelper");
  }
  
  protected void executeAction(ProcessExecutionContext pContext) throws ProcessException {
    try {
      DynamoHttpServletRequest request = pContext.getRequest();
      OrderHolder orderHolder = (OrderHolder) request.resolveName(SHOPPING_CART_PATH);
      Order order = (NMOrderImpl) orderHolder.getCurrent();
      evaluatePromo(order);
    } catch (final Exception e) {
      throw new ProcessException(ExceptionUtil.getExceptionInfo(e), e);
    }
  }
  
  public void evaluatePromo(Order atgOrder) throws PromoException {
    TransactionDemarcation transaction = new TransactionDemarcation();
    boolean rollBack = false;
    boolean orderWasModified = false;
    
    try {
      transaction = DataSourceUtil.getInstance().beginTransaction();
      
      NMOrderImpl order = (NMOrderImpl) atgOrder;
      DynamoHttpServletRequest request = ServletUtil.getCurrentRequest();
      
      OrderManager orderManager = (OrderManager) request.resolveName(ORDERMANAGER_PATH);
      Map<String, NMPromotion> promoMap = mPromotionArray.getAllPromotionsArray();
      Collection<NMPromotion> promoList = promoMap.values();
      List<String> userEnteredPromoCodes = order.getPromoCodeList();
      
      final NMProfile profile = (NMProfile) ServletUtil.getCurrentRequest().resolveName("/atg/userprofiling/Profile");
      String country = (String) profile.getPropertyValue(ProfileRepository.COUNTRY_PREFERENCE);
      LocalizationUtils localizationUtils = ((LocalizationUtils) Nucleus.getGlobalNucleus().resolveName("/nm/utils/LocalizationUtils"));
      FiftyOnePromotion fiftyOnePromotion = null;
      if (localizationUtils.isSupportedByFiftyOne(country)) {
        Date mystartDate = Calendar.getInstance().getTime();
        
        //Removing expired promotions
        for (NMPromotion promotion : promoList) {
          fiftyOnePromotion = (FiftyOnePromotion) promotion;
          if (mystartDate.before(fiftyOnePromotion.getStartDate()) || mystartDate.after(fiftyOnePromotion.getEndDate())) {
            orderWasModified |= removePromotionFromOrder(order, fiftyOnePromotion);
          }
        }
        //Adding active promotions
        for (NMPromotion promotion : promoList) {
          fiftyOnePromotion = (FiftyOnePromotion) promotion;	
          // if the promotion is active then evaluate the qualification criteria associated with
          // the promotions type, otherwise remove the promotion from the order
          if (mystartDate.after(fiftyOnePromotion.getStartDate()) && mystartDate.before(fiftyOnePromotion.getEndDate())) {
            String promoCode = fiftyOnePromotion.getPromoCodes();
            int type = Integer.parseInt(fiftyOnePromotion.getType());
            switch (type) {
              case 1: {
                if (userEnteredPromoCodes.contains(promoCode)) {
                  orderWasModified |= addPromotionToOrder(order, fiftyOnePromotion);
                } else {
                  orderWasModified |= removePromotionFromOrder(order, fiftyOnePromotion);
                }
                break;
              }
            }
          } 
        }    
      }else {
        // country is not supported by FiftyOne so remove any FiftyOne promotions that may be
        // active on the order (based on previous country preference or change in FiftyOne support)
    	  for (NMPromotion promotion: promoList) {
    		  fiftyOnePromotion = (FiftyOnePromotion) promotion;
    		  orderWasModified |= removePromotionFromOrder(order, fiftyOnePromotion);
    	  }
      }
      
      if (orderWasModified) {
        orderManager.updateOrder(order);
      }
    } catch (Exception exception) {
      rollBack = true;
      if (isLoggingError()) {
        logError(exception);
      }
      throw new PromoException(exception);
    } finally {
      try {
        if (transaction != null) {
          transaction.end(rollBack);
        }
      } catch (TransactionDemarcationException tde) {
        if (isLoggingError()) {
          logError(tde);
        }
        throw new PromoException(tde);
      }
    }
  }
  
  private boolean addPromotionToOrder(NMOrderImpl order, FiftyOnePromotion promotion) throws Exception {
    String key = promotion.getCode();
    String promoCode = promotion.getPromoCodes();
    
    // all this logic to add and remove to the codes is ridiculous and should be rewriten
    order.setActivatedPromoCode(promoCode);
    order.setUserActivatedPromoCode(promoCode);
    order.setPromoName(key);
    mPromotionsHelper.addKEYToCI(key, order);
    order.addAwardedPromotion(promotion);
    
    // always return true until promotion/order logic is rewritten
    return true;
  }
  
  private boolean removePromotionFromOrder(NMOrderImpl order, FiftyOnePromotion promotion) {
    String key = promotion.getCode();
    String promoCode = promotion.getPromoCodes();
    
    // all this logic to add and remove to the codes is ridiculous and should be rewriten
    order.setRemoveActivatedPromoCode(promoCode);
    order.setRemoveUserActivatedPromoCode(promoCode);
    order.setRemovePromoName(key);
    mPromotionsHelper.removeKEYToCI(key, order);
    order.removeAwardedPromotionByKey(key);
    
    // always return true until promotion/order logic is rewritten
    return true;
  }
  
  private void setLoggingDebug(boolean bin) {
    logDebug = bin;
  }
  
  private boolean isLoggingDebug() {
    return logDebug;
  }
  
  private void setLoggingError(boolean bin) {
    logError = bin;
  }
  
  private boolean isLoggingError() {
    return logError;
  }
  
  private void logError(String string) {
    Nucleus.getGlobalNucleus().logError("checkCsrGWP: " + string);
  }
  
  private void logError(Throwable throwable) {
    Nucleus.getGlobalNucleus().logError(throwable);
  }
  
  private void logDebug(String string) {
    Nucleus.getGlobalNucleus().logDebug("checkCsrGWP: " + string);
  }
  
  private RepositoryItem getProfileForOrder(OrderManager orderManager, Order order) throws PromoException {
    RepositoryItem profile = null;
    
    try {
      profile = orderManager.getOrderTools().getProfileTools().getProfileItem(order.getProfileId());
    } catch (RepositoryException e) {
      throw new PromoException(e);
    }
    
    return profile;
  }
}
