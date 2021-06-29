package com.nm.commerce.promotion.action;

import javax.transaction.TransactionManager;

import atg.commerce.CommerceException;
import atg.commerce.order.Order;
import atg.commerce.order.OrderManager;
import atg.dtm.TransactionDemarcation;
import atg.dtm.TransactionDemarcationException;
import atg.nucleus.GenericService;
import atg.nucleus.Nucleus;
import atg.nucleus.ServiceException;
import atg.service.lockmanager.ClientLockManager;
import atg.service.lockmanager.DeadlockException;
import atg.servlet.ServletUtil;

import com.nm.commerce.NMProfile;
import com.nm.commerce.promotion.IPromoAction;
import com.nm.commerce.promotion.NMOrderImpl;
import com.nm.commerce.promotion.PromoException;
import com.nm.utils.PromotionsHelper;

public class PromotionEvaluation extends GenericService {
	
  private ClientLockManager mLockManager;
  private TransactionManager mTransactionManager;
  private boolean mUseClientLocksForOrderUpdate;
  
  public boolean isUseClientLocksForOrderUpdate() {
	return mUseClientLocksForOrderUpdate;
  }

  public void setUseClientLocksForOrderUpdate(boolean pUseClientLocksForOrderUpdate) {
	this.mUseClientLocksForOrderUpdate = pUseClientLocksForOrderUpdate;
  }
	
  public ClientLockManager getLockManager() {
	return mLockManager;
  }


  public void setLockManager(ClientLockManager pLockManager) {
	this.mLockManager = pLockManager;
  }

  public void setTransactionManager(TransactionManager pTransactionManager) {
	mTransactionManager = pTransactionManager;
  }

  public TransactionManager getTransactionManager() {
	return mTransactionManager;
  }
  @Override
  public void doStartService() throws ServiceException {
    super.doStartService();
    promoActions = actionConfig.getPromoActionsForEvent("evaluate.promotions");
    if (null == promoActions || 0 == promoActions.length) {
      final String msg = "No promotion actions were found for event 'evaluate.promotions'";
      logError(msg);
      // don't throw service exception, this allows us to configure evaluate.promotions
      // to be empty, gives us ability to backout new promo functionality
      // throw new ServiceException(msg);
    }
  }
  
  public ActionConfiguration getActionConfig() {
    return actionConfig;
  }
  
  public void setActionConfig(final ActionConfiguration actionConfig) {
    this.actionConfig = actionConfig;
  }
  
  public void evaluatePromotions(final Order order) throws PromoException {
    ((NMOrderImpl) order).clearAwardedPromotions();
    ((NMOrderImpl) order).clearEncouragePromotions();
    // Clear the promo messages on all items present in the order. Messages will be added to an item when promotion
    // is applied and corresponding item meets the qualifying criteria.
    final String PROMOHELPER_PATH = "/nm/utils/PromotionsHelper";
    PromotionsHelper mPromotionsHelper = (PromotionsHelper) Nucleus.getGlobalNucleus().resolveName(PROMOHELPER_PATH);
    mPromotionsHelper.clearPromoMessagesOnItems((NMOrderImpl) order);
    mPromotionsHelper.clearBogoSendCmosKeyOnItems((NMOrderImpl) order);
    if (null != promoActions) {
      for (int i = 0; i < promoActions.length; ++i) {
        try {
          if (isLoggingDebug()) {
            logDebug("before promo " + i + ": " + promoActions[i].getClass().getName());
            logDebug("repository version: " + ((Integer) ((atg.commerce.order.OrderImpl) order).getRepositoryItem().getPropertyValue("version")).intValue());
            logDebug("object version: " + ((atg.commerce.order.OrderImpl) order).getVersion());
          }
          
          OrderManager.getOrderManager().updateOrder(order);
          promoActions[i].evaluatePromo(order);
          OrderManager.getOrderManager().updateOrder(order);
          
          if (isLoggingDebug()) {
            logDebug("after promo " + i + ": " + promoActions[i].getClass().getName());
            logDebug("repository version: " + ((Integer) ((atg.commerce.order.OrderImpl) order).getRepositoryItem().getPropertyValue("version")).intValue());
            logDebug("object version: " + ((atg.commerce.order.OrderImpl) order).getVersion());
          }
        } catch (CommerceException e) {
          if (isLoggingError()) {
            logError("An exception was thrown while trying to update the order.", e);
          }
        }
      }
    }
  }
  
  public void setEvaluatePromotions(Order order) {
	  
	 final NMProfile nmProfile = (NMProfile) ServletUtil.getCurrentRequest().resolveName("/atg/userprofiling/Profile");
  	 String profileId = nmProfile.getRepositoryId();
  	 boolean acquireLock = false;
	 
  	 try {
  		if(isUseClientLocksForOrderUpdate()) {
	        acquireLock = !getLockManager().hasWriteLock( profileId, Thread.currentThread());
			
	        if ( acquireLock )
				getLockManager().acquireWriteLock( profileId, Thread.currentThread() );
        }
        
        final TransactionDemarcation td = new TransactionDemarcation();
		final TransactionManager transactionManager = getTransactionManager();
		
		if(transactionManager != null)
			td.begin( getTransactionManager(), TransactionDemarcation.REQUIRED );
		
		boolean shouldRollback = false;
  	
		try {
			synchronized (order) {
				evaluatePromotions(order);
			}
	    } catch (PromoException e) {
	    	shouldRollback = true;
		    if (isLoggingError()) {
		      logError("An exception was thrown while trying to evaluate promotions.", e);
		    } 
	    } catch (final Exception e) {
			shouldRollback = true;
	        if (isLoggingError()) {
	            logError(e);
	          }
	    } finally {
			try {
				td.end(shouldRollback);
			} catch (Throwable th) {
				logError(th);
			}
		}
  	 } catch (TransactionDemarcationException e) {
			vlogError("PromotionEvaluation:setEvaluatePromotions : TransactionDemarcationException " + e.getMessage());
		} catch (DeadlockException e) {
			vlogError("PromotionEvaluation:setEvaluatePromotions : DeadlockException " + e.getMessage());
		} catch (final Exception e) {
	          logError(e);
		} finally {
			try {
				if (acquireLock)
					getLockManager().releaseWriteLock(profileId, Thread.currentThread(), true);
			} catch (Throwable th) {
				logError(th);
			}
		}
  }
  
  private ActionConfiguration actionConfig;
  private IPromoAction[] promoActions;
}
