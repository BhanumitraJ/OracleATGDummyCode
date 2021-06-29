package com.nm.commerce.promotion.action;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.transaction.TransactionManager;

import atg.commerce.CommerceException;
import atg.commerce.order.InvalidVersionException;
import atg.commerce.order.Order;
import atg.commerce.order.OrderHolder;
import atg.commerce.order.OrderManager;
import atg.commerce.order.OrderQueries;
import atg.commerce.promotion.PromotionConstants;
import atg.commerce.states.OrderStates;
import atg.commerce.states.StateDefinitions;
import atg.dtm.TransactionDemarcation;
import atg.dtm.TransactionDemarcationException;
import atg.nucleus.Nucleus;
import atg.nucleus.naming.ComponentName;
import atg.process.ProcessException;
import atg.process.ProcessExecutionContext;
import atg.process.action.ActionImpl;
import atg.repository.MutableRepositoryItem;
import atg.repository.RepositoryException;
import atg.servlet.DynamoHttpServletRequest;

import com.nm.collections.NMPromotion;
import com.nm.commerce.promotion.IPromoAction;
import com.nm.commerce.promotion.NMOrderImpl;
import com.nm.commerce.promotion.PromoException;
import com.nm.commerce.promotion.rulesBased.Promotion;
import com.nm.components.CommonComponentHelper;
import com.nm.utils.ExceptionUtil;

public abstract class RulesBasedPromotionAction extends ActionImpl implements IPromoAction {
  
  protected OrderManager mOrderManager = null;
  protected ComponentName mOrderHolderComponent = null;
  public static final String SHOPPING_CART_PATH = "/atg/commerce/ShoppingCart";
  
  @Override
  public void initialize(final Map pParameters) throws ProcessException {
    mOrderManager = (OrderManager) Nucleus.getGlobalNucleus().resolveName("/atg/commerce/order/OrderManager");
    mOrderHolderComponent = ComponentName.getComponentName(SHOPPING_CART_PATH);
  }
  
  @Override
  protected void executeAction(final ProcessExecutionContext pContext) throws ProcessException {
    
    try {
      final Order order = getOrderToModify(pContext);
      evaluatePromo(order);
    } catch (final Exception e) {
      throw new ProcessException(ExceptionUtil.getExceptionInfo(e), e);
    }
  }
  
  @Override
  public void evaluatePromo(final Order order) throws PromoException {
    final TransactionDemarcation td = new TransactionDemarcation();
    boolean rollBack = false;
    
    try {
      final TransactionManager tm = CommonComponentHelper.getTransactionManager();
      if (tm != null) {
        td.begin(tm, TransactionDemarcation.REQUIRED);
      }
    } catch (final TransactionDemarcationException tde) {
      tde.printStackTrace();
      throw new PromoException(tde);
    }
    
    try {
      if (mOrderManager == null) {
        throw new PromoException(PromotionConstants.getStringResource(PromotionConstants.ORDER_MANAGER_NOT_FOUND));
      }
      
      try {
        
        final NMOrderImpl orderImpl = (NMOrderImpl) order;
        if ((orderImpl != null) && (orderImpl.getCommerceItemCount() > 0)) {
          
          final Map<String, NMPromotion> activePromotions = getActivePromotions();
          if (activePromotions != null) {
            final Iterator<NMPromotion> activePromosI = activePromotions.values().iterator();
            final boolean evalPromosForEmployee = orderImpl.isEvaluatePromosForEmployee();
            // for each active promo (for the type being evaluated)
            while (activePromosI.hasNext()) {
              final Promotion promo = (Promotion) activePromosI.next();
              /* isExcludeEmployeesFlag is true ,don't apply DollarOff Promotion to the employee order */
              if (evalPromosForEmployee && promo.isExcludeEmployeesFlag()) {
                continue;
              }
              if (promo != null) {
                final boolean isAwarded = promo.evaluate(orderImpl);
                
                // check for spend more save more messaging
                if (!isAwarded) {
                  if (promo.getSpendMoreFlag() && promo.evaluateForSpendMoreSaveMore(orderImpl)) {
                    orderImpl.addEncouragePromotion(promo);
                  }
                }
                mOrderManager.updateOrder(orderImpl);
              }
            }
          }
        }
        
        // System.out.println("RulesBasedPromo -the order version---------before updateorder----------->" + orderImpl.getVersion());
        mOrderManager.updateOrder(order);
        // System.out.println("RulesBasedPromo -the order version---------after updateorder----------->" + orderImpl.getVersion());
      } catch (final InvalidVersionException ive) {
        ive.printStackTrace();
      } catch (final Exception cleare) {
        cleare.printStackTrace();
        throw new PromoException(cleare);
      }
    } catch (final Exception e) {
      rollBack = true;
      // System.out.println("RulesBasedPromotionAction.executeAction(): Exception ");
      throw new PromoException(e);
    } finally {
      try {
        td.end(rollBack); // commit work
      } catch (final TransactionDemarcationException tde) {
        tde.printStackTrace();
        throw new PromoException(tde);
      }
    }
  }
  
  public abstract Map<String, NMPromotion> getActivePromotions();
  
  private Order getOrderToModify(final ProcessExecutionContext pContext) throws CommerceException, RepositoryException {
    
    final DynamoHttpServletRequest request = pContext.getRequest();
    
    // If we are executing this action in the context of a session then we
    // want to get a hold
    // of the OrderHolder and use the Order within it as the Order that we
    // want to get
    // information from.
    
    if (request != null) {
      final OrderHolder orderHolder = (OrderHolder) request.resolveName(mOrderHolderComponent);
      return orderHolder.getCurrent();
    }
    
    // Get the profile from the context
    final String profileId = ((MutableRepositoryItem) pContext.getSubject()).getRepositoryId();
    // System.out.println("profileId from UpdateACtivatedPC is:"+profileId);
    
    final OrderQueries mOrderQueries = mOrderManager.getOrderQueries();
    // Only change orders that are in the "incomplete" state
    final List l = mOrderQueries.getOrdersForProfileInState(profileId, StateDefinitions.ORDERSTATES.getStateValue(OrderStates.INCOMPLETE));
    
    // Just modify the user's first order that's in the correct state
    if (l != null) {
      if (l.size() > 0) {
        return (Order) l.get(0);
      }
    }
    
    return null;
  }
}
