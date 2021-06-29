package com.nm.commerce;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpSession;
import javax.transaction.TransactionManager;

import org.apache.commons.lang.StringUtils;

import atg.commerce.CommerceException;
import atg.commerce.order.CommerceItemManager;
import atg.commerce.order.Order;
import atg.commerce.order.OrderHolder;
import atg.commerce.order.OrderManager;
import atg.commerce.order.Relationship;
import atg.commerce.pricing.PricingModelHolder;
import atg.commerce.profile.CommerceProfileTools;
import atg.commerce.states.OrderStates;
import atg.dtm.TransactionDemarcation;
import atg.dtm.TransactionDemarcationException;
import atg.nucleus.Nucleus;
import atg.repository.Repository;
import atg.repository.RepositoryException;
import atg.repository.RepositoryItem;
import atg.repository.RepositoryView;
import atg.repository.rql.RqlStatement;
import atg.service.lockmanager.ClientLockManager;
import atg.service.lockmanager.DeadlockException;
import atg.servlet.DynamoHttpServletRequest;
import atg.servlet.DynamoHttpServletResponse;
import atg.servlet.ServletUtil;
import atg.servlet.sessiontracking.SessionManager;
import atg.userprofiling.NMCookieManager;

import com.nm.commerce.checkout.CheckoutAPI;
import com.nm.commerce.order.NMOrderManager;
import com.nm.commerce.promotion.NMOrderImpl;
import com.nm.commerce.promotion.action.PromotionEvaluation;
import com.nm.components.CommonComponentHelper;
import com.nm.edo.EmployeeDiscountsConfig;
import com.nm.formhandler.CsrGiftListHandler;
import com.nm.formhandler.ShoppingCartHandler;
import com.nm.repository.ProfileRepository;
import com.nm.utils.BaseCXPClient;
import com.nm.utils.BrandSpecs;
import com.nm.utils.CartUtility;
import com.nm.utils.CustomerDataVO;
import com.nm.utils.EDWCustData;
import com.nm.utils.LinkedEmailUtils;
import com.nm.utils.StringUtilities;
import com.nm.utils.SystemSpecs;

/**
 * 
 * NM extension of the ATG CommerceProfileTools class
 * 
 * To execute our custom CartUtility class functionality, loadUserShoppingCartForLogin was overridden to allow for our unique functionality.
 * 
 * <b>PERS2</b> As part of the profile consolidaton project this class was reduced in scope, especially as it relates to cart loading and profile switching. We now rely on ATG's core functionality to
 * load carts, and run our custom functions after this work has been done.
 * 
 * <p>
 * 
 * @author: nmjll4
 * @version: $Revision: 1.23.1.4 $ Last Modified: $Date: 2014/07/17 14:05:39CDT $ Last Modified By: $Author: Chandrasekaran Raju (vscr3) $
 * 
 * 
 *           $Log: NMCommerceProfileTools.java $ Revision 1.23.1.4 2014/07/17 14:05:39CDT Chandrasekaran Raju (vscr3) included log Revision 1.23.1.3 2014/07/17 13:49:17CDT Chandrasekaran Raju (vscr3)
 *           Linked email is included when cheetah or profile email id is null Revision 1.23.1.2 2014/07/16 17:27:47CDT Chandrasekaran Raju (vscr3) Added emailid to the ucid api call Revision 1.23.1.1
 *           2014/06/26 15:48:16CDT Arindam Roy (vsar8) Append the Brand code with Web Id Revision 1.23 2014/03/26 04:47:10CDT Jayaprakash Reddy (vsjp6) Enabled logDebug for testing purpose and will
 *           be disabled after testing is done Revision 1.22 2014/03/24 05:37:45CDT Jayaprakash Reddy (vsjp6) Modified to add a method which calls UCID service Revision 1.21 2014/02/24 11:13:33CST
 *           nmavj1 merging 2014_02_PROD Revision 1.20.2.1 2014/01/28 08:32:05CST nmavj1 merging post stabilization jan patch Revision 1.20.1.1 2014/01/17 09:47:00CST Samir Soman (vsss13) changed the
 *           code where shouldValidate country was set to false. Now it is set to true. Revision 1.20 2013/10/29 10:08:43CDT nmavj1 merging 2013_10_PROD Revision 1.19.1.5 2013/10/22 16:16:04CDT nmavj1
 * 
 *           Revision 1.19.1.4 2013/10/21 14:11:59CDT nmavj1
 * 
 *           Revision 1.19.1.3 2013/10/18 18:37:54CDT nmavj1
 * 
 *           Revision 1.19.1.2 2013/10/17 16:26:07CDT nmavj1
 * 
 *           Revision 1.19.1.1 2013/10/16 15:58:08CDT nmavj1
 * 
 *           Revision 1.19 2010/08/03 02:58:00CDT nmavj1 merging 2010_10_PROD into mainline Revision 1.18.1.2 2010/07/19 18:24:53CDT nmavj1
 * 
 *           Revision 1.18.1.1 2010/03/01 10:19:31CST nmavj1 Duplicate revision Revision 1.17.1.2 2010/02/09 18:47:50CST Donald T Schultz (nmts5)
 * 
 *           Revision 1.17 2010/01/18 20:08:23CST nmavj1 merging 2010_03_DEV into mainline Revision 1.16.1.2 2010/01/13 12:05:39CST Donald T Schultz (nmts5)
 * 
 *           Revision 1.16.1.1 2009/10/02 14:38:09CDT Donald T Schultz (nmts5) Duplicate revision Revision 1.15 2009/07/24 10:24:23CDT nmavj1 merging sale silo nav branch into mainline Revision 1.14
 *           2009/06/19 12:52:27CDT nmavj1 merging single page into mainline Revision 1.13 2009/06/19 10:54:14CDT nmavj1 merging single page checkout into mainline Revision 1.10.2.4 2009/06/08
 *           14:00:17CDT William P Shows (NMWPS) make sure the spc verification flag is reset when order is loaded Revision 1.10.2.3 2009/06/01 10:08:31CDT nmavj1 merging 2009_07_PROD into single page
 *           Revision 1.11.1.2 2009/05/26 10:26:23CDT Michael Everheart (nmmpe) added cleardeletedProducts calls Revision 1.11 2009/05/06 09:30:42CDT nmavj1 merging 2009_06_PROD into mainline Revision
 *           1.10.1.1 2009/03/09 15:55:59CDT Mike Cendana (nmmc5) Duplicate revision Revision 1.9 2009/03/03 15:46:12CST nmavj1 merging 2009_05_DEV into mainline Revision 1.8 2009/02/17 10:23:47CST
 *           nmavj1 merging 2009_03_PROD branch into mainline Revision 1.7 2008/12/01 11:38:49CST Mike Cendana (nmmc5) atg is loading the entire order when querying for ids, only need to select
 *           incomplete orders to clear from cache, reduce load on db Revision 1.6 2008/11/21 14:06:45CST Mike Cendana (nmmc5) merge from oct patch branch Revision 1.5 2008/04/29 13:59:07CDT Mike
 *           Cendana (nmmc5) merge endecaproject2 into mainline Revision 1.4 2007/04/23 13:36:54CDT Michael Everheart (nmmpe) removed System.out Revision 1.3 2006/10/26 09:50:21CDT Mike Cendana
 *           (nmmc5)
 * 
 *           Revision 1.10 2006/10/25 13:41:10CDT nmavj1 merging Isaac into mainline Revision 1.8.1.2 2006/09/05 22:47:23CDT nmavj1 Merging Helene into Isaac Revision 1.4.1.5 2006/08/08 12:34:48CDT
 *           nmavj1 Resolution Merge. Revision 1.4.1.4 2006/08/02 10:13:37CDT nmavj1 Resolution Merge. Revision 1.4.1.3 2006/07/31 08:51:06CDT nmavj1 Resolution Merge. Revision 1.4.1.2 2006/07/12
 *           11:56:22CDT nmavj1 Resolution Merge. Revision 1.2.1.4 2006/06/29 10:27:37CDT Butch Seaman (nmbs3) Resolution Merge. Revision 1.2.1.3 2006/06/28 15:59:08CDT Butch Seaman (nmbs3) Resolution
 *           Merge. Revision 1.2.1.2 2006/06/13 09:21:49CDT nmavj1 Resolution Merge. Revision 1.1.6.2 2006/05/18 16:25:50CDT nmavj1 Resolution Merge. Revision 1.1.3.2 2006/04/07 13:06:04CDT Butch
 *           Seaman (nmbs3) Resolution Merge. Revision 1.1.1.1 2006/02/14 18:53:23CST vscd1 initial import of personalization Revision 1.26.96.2 2006/01/20 21:55:48 nmmpe removed commented out methods
 * 
 *           Revision 1.26.94.4 2006/01/18 22:28:09 nmjll4 PERS2: Removed unused code; organized imports
 * 
 */
public class NMCommerceProfileTools extends CommerceProfileTools {
  
    /** The base CXP Client. */
  private BaseCXPClient baseCXPClient = null;
  
  /**
   * Gets the base CXP client.
   * 
   * @return the base CXP client
   */
  public BaseCXPClient getBaseCXPClient() {
    return baseCXPClient;
  }
  
  /**
   * Sets the base CXP client.
   * 
   * @param baseCXPClient the new base CXP  client
   */
  public void setBaseCXPClient(final BaseCXPClient baseCXPClient) {
    this.baseCXPClient = baseCXPClient;
  }
  
  private SessionManager sessionManager;
  
  public SessionManager getSessionManager() {
    return sessionManager;
  }
  
  public void setSessionManager(final SessionManager sm) {
    sessionManager = sm;
  }
     
  public NMCommerceProfileTools() {}
  
  // ----------------------------------------------------
  // / vsrb1 mVerbose Used for testing purposes only.
  // / Should be set to false, except to generate clean debug output.
  // / Also sets verbose property in NMOrderImpl.
  // / Note that NMOrderImpl is not currently a component,
  // / so get/set methods not called by Nucleus
  // / verbose is instead set in loadShoppingCarts method
  private boolean mVerbose = false;
  
  public void setVerbose(final boolean pVerbose) {
    mVerbose = pVerbose;
  }
  
  public boolean isVerbose() {
    return mVerbose;
  }
  
  // --------------------------------------------------------------------------------
  
  // / vsrb1 used for testing purposes only.
  // / Should be set to false, except to generate expired carts.
  // / Note that NMOrderImpl is not currently a component,
  // / so get/set methods not called by Nucleus
  // / testBadDate is instead set in loadShoppingCarts method
  private boolean mTestBadDate = false;
  
  public boolean isTestBadDate() {
    return mTestBadDate;
  }
  
  public void setTestBadDate(final boolean pTestBadDate) {
    mTestBadDate = pTestBadDate;
  }
  
  // --------------------------------------------------------------------------------
  
  // / vsrb1 turn on or off the call to CartUtiltiy in loadShoppingCarts method
  private boolean mCallingCartUtility = true;
  
  public boolean isCallingCartUtility() {
    return mCallingCartUtility;
  }
  
  public void setCallingCartUtility(final boolean pCallingCartUtility) {
    mCallingCartUtility = pCallingCartUtility;
  }
  
  private ClientLockManager mLockManager;
  private TransactionManager mTransactionManager;
	
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
	
  // Put in when we disable CartUtility call becuase of SCP disabled.
  protected void clearGiftCards(final NMOrderImpl nmOrderImpl) {
    // System.out.println("***inside clearGiftCards***");
    try {
      synchronized (nmOrderImpl) {
        if (nmOrderImpl != null) {
          final Order order = nmOrderImpl;
          final NMOrderManager om = (NMOrderManager) getOrderManager();
          om.removeAllGiftCardsFromOrder(order);
          om.updateOrder(nmOrderImpl);
        }
      } // end synchron
    } catch (final Exception e) {
      e.printStackTrace();
    } // end try-catch
  }
  
  public void retrieveCartFromLinkedEmail(final DynamoHttpServletRequest request, final RepositoryItem profile, final OrderHolder shoppingCart) throws Exception {
    
    if (request != null) {
      final String ecId = request.getParameter("ecid");
      String uEm = request.getParameter("uEm");
      if (uEm == null) {
        uEm = request.getParameter("uEM");
      }
      String linkedEmail = null;
      final RepositoryItem userRepositoryItem;
      
      if (profile.getPropertyValue("linkedEmail") != null) {
        linkedEmail = (String) profile.getPropertyValue("linkedEmail");
      } else {
        linkedEmail = LinkedEmailUtils.getInstance().getDecryptedCookie(request);
        if ((linkedEmail == null) || linkedEmail.isEmpty()) {
          if (uEm != null) {
            linkedEmail = uEm;
          }
        }
      }
      
      if (linkedEmail != null) {
        retrieveOrdersWithEmail(linkedEmail, shoppingCart, profile);
      }
    }
  }
  
  public void retrieveOrdersWithEmail(final String linkedEmail, final OrderHolder shoppingCart, final RepositoryItem profile) throws Exception {
    final Repository linkedEmailRepository = LinkedEmailUtils.getInstance().getLinkedEmailRepository();
    RqlStatement statement;
    try {
      final NMOrderManager orderMgr = (NMOrderManager) OrderManager.getOrderManager();
      final CommerceItemManager itemManager = orderMgr.getCommerceItemManager();
      statement = RqlStatement.parseRqlStatement("contactEmail = ?0");
      final Object params[] = new Object[1];
      params[0] = linkedEmail;
      final RepositoryView view = linkedEmailRepository.getView("linkEmailOderSkuData");
      final RepositoryItem[] items = statement.executeQuery(view, params);
      if (items != null) {
        for (int pos = 0; pos < items.length; pos++) {
          if (isLoggingDebug()) {
            logDebug(items[pos].getPropertyValue("promoKey") + "  " + items[pos].getPropertyValue("productPageSort") + "  " + pos);
          }
          final RepositoryItem linkedEmailOrders = items[pos];
          final String orderId = (String) linkedEmailOrders.getPropertyValue("orderId");
          try {
            final NMOrderImpl order = (NMOrderImpl) orderMgr.loadOrder(orderId);
            synchronized (order) {
              final List<NMCommerceItem> commerceItems = order.getNmCommerceItems();
              for (int i = 0; i < commerceItems.size(); ++i) {
                final NMCommerceItem ci = commerceItems.get(i);
                if (!ci.getSpecialInstCodes().isEmpty() || ci.getPerishable()) {
                  final String merchType = (String) ((RepositoryItem) ci.getAuxiliaryData().getProductRef()).getPropertyValue("merchandiseType");
                  if (!merchType.equals("7") || !merchType.equals("6")) {
                    itemManager.removeItemFromOrder(order, ci.getId());
                  }
                }
              }
            }
            if ((order != null) && order.getStateAsString().equalsIgnoreCase(OrderStates.INCOMPLETE) && (order.getCommerceItemCount() > 0)) {
                final NMOrderImpl orderClone = (NMOrderImpl) orderMgr.createOrder(profile.getRepositoryId());
                orderMgr.mergeOrders(order, orderClone, true, false);
                orderMgr.addOrder(orderClone);
                shoppingCart.setCurrent(orderClone);
            }
          } catch (final CommerceException e) {
            e.printStackTrace();
          }
          
        }
      }
    } catch (final RepositoryException e) {
      e.printStackTrace();
    }
  }
  
  /**
   * Override of the super's method.
   * 
   * This method is called at: start of session, registered login. super.loadShoppingCarts is called prior to taking any custom actions against the cart/order. It also does not reprice all the orders
   * in the shopping cart to avoid modifying other session's orders. But rather only reprices the one order that is current and owned by this session. (this keeps us from changing the last-modified
   * time and version on those orders)
   * <p>
   * 
   * @param profile
   *          a repository-item that represents the profile of the user
   * @param shoppingCart
   *          holds the orders for this profile/user
   * @param pricingmodelholder
   *          we don't do anything directly/locally with this
   * @param pLocale
   *          the language locale for messages from the system
   */
  @Override
  public void loadUserShoppingCartForLogin(final RepositoryItem profile, final OrderHolder shoppingCart, final PricingModelHolder pricingmodelholder, final java.util.Locale pLocale)
          throws CommerceException {
    final BrandSpecs brandSpecs = (BrandSpecs) Nucleus.getGlobalNucleus().resolveName("/nm/utils/BrandSpecs");
    final SystemSpecs systemSpecs = (SystemSpecs) Nucleus.getGlobalNucleus().resolveName("/nm/utils/SystemSpecs");
    final DynamoHttpServletRequest request = ServletUtil.getCurrentRequest();
    final String ecId = request.getParameter("ecid");
    
    checkedLogDebug("loadUserShoppingCartForLogin called ...");
    
    try {
		// get the ids of all the orders associated with this profile
		final int incomplete = atg.commerce.states.StateDefinitions.ORDERSTATES.getStateValue(atg.commerce.states.OrderStates.INCOMPLETE);
	    final java.util.List orderIds = getOrderManager().getOrderQueries().getOrderIdsForProfile(profile.getRepositoryId(), incomplete);
	    
	    if (orderIds != null) {
	      for (int i = 0; i < orderIds.size(); i++) {
	        try {
	          // remove this order from cache to ensure we are not working with a stale version
	          ((atg.repository.ItemDescriptorImpl) getOrderManager().getOrderTools().getOrderRepository().getItemDescriptor("order")).removeItemFromCache((String) orderIds.get(i), true);
	        } catch (final atg.repository.RepositoryException e) {
	          if (isLoggingError()) {
	            logError("An error occured while trying to remove order " + orderIds.get(i) + " from cache.", e);
	          }
	        }
	      }
	    }
    } catch (Exception ex){
    	if (isLoggingError()) {
            logError("An error occured while retrieving the ids of all the orders associated with the current profile :- " + profile.getRepositoryId() +
            		"The error message :- " + ex.getMessage());
          }
    }
    
    boolean useClientLockForOrderUpdateInDAFPipeine = false;
    
    if (systemSpecs != null)
    	useClientLockForOrderUpdateInDAFPipeine = systemSpecs.isUseClientLocksForOrderUpdateInPipeline();
    else {
    	if (isLoggingError()) {
    		logError("NMCommerceProfileTools:loadUserShoppingCartForLogin :- systemSpecs is NULL. Client Lock will not be used.");
    	}
    }
    
	String profileId = profile.getRepositoryId();
	boolean acquireLock = false;
    
    try {
    	if(useClientLockForOrderUpdateInDAFPipeine) {
	        acquireLock = !getLockManager().hasWriteLock( profileId, Thread.currentThread());
			
	        if ( acquireLock )
				getLockManager().acquireWriteLock( profileId, Thread.currentThread() );
        }
        
        final TransactionDemarcation td = new TransactionDemarcation();
		final TransactionManager transactionManager = getTransactionManager();
		
		if(transactionManager != null)
			td.begin( getTransactionManager(), TransactionDemarcation.REQUIRED );
		
		boolean shouldRollback = false;
    	
	    if (shoppingCart != null) {
	      try {
	        // call super
	        super.loadShoppingCarts(profile, shoppingCart);
	        
	        if(brandSpecs.isEnableMergeCart()){
	          NMOrderImpl persistentOrder = null;
	          if (!shoppingCart.getSaved().isEmpty()) {
	            final int size = shoppingCart.getSaved().size();
	            persistentOrder = ((NMOrderImpl) (shoppingCart.getSaved().toArray())[size - 1]);
	            
	            if (persistentOrder != null) {
	              //synchronized (persistentOrder) {
		              final List<NMCommerceItem> destCommerceItems = persistentOrder.getCommerceItems();
		              final SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yy");
		              final Date lastModDate = persistentOrder.getLastModifiedDate();
		              String lastMod = "previous session";
		              if(lastModDate!=null){
		                lastMod = sdf.format(lastModDate);
		              }
		              
		              CheckoutAPI.removePersCartItems(persistentOrder,getOrderManager());
		              repriceOrder(persistentOrder, profile, pricingmodelholder, pLocale, getRepriceOrderPricingOp());
		              
		              if ((destCommerceItems != null) && !destCommerceItems.isEmpty()) {
		                final boolean showMergeCart = checkShowMergeCart(persistentOrder,shoppingCart.getCurrent(),profile);
		                if(showMergeCart){
		                  final HttpSession session = ServletUtil.getCurrentRequest().getSession();
		                  session.setAttribute("CartLastModifiedDate", lastMod);
		                  session.setAttribute("ProfileCartItems", destCommerceItems);
		                  session.setAttribute("showMergeCartWindow", true);
		                }
		              }
		           // }
	             }
	          }
	        }
	        
	        final NMOrderImpl order = (NMOrderImpl) shoppingCart.getCurrent();
	        final EmployeeDiscountsConfig employeeDiscountsConfig = (EmployeeDiscountsConfig) Nucleus.getGlobalNucleus().resolveName("/nm/edo/EmployeeDiscountsConfig");
	        final List<NMCommerceItem> commerceItems = order.getNmCommerceItems();
	        
	        synchronized (order) {
		        for (final NMCommerceItem ci : commerceItems) {
		          if (StringUtilities.isNotEmpty(ci.getPickupStoreNo())) {
		            final String storeConfig = employeeDiscountsConfig.getStoreCallParticipatedStoresConfig().get(ci.getPickupStoreNo());
		            if (StringUtilities.isNotEmpty(storeConfig)) {
		              ci.setPickupStoreNo(StringUtils.EMPTY);
		            }
		          }
		        }
		        getOrderManager().updateOrder(order);
	        }
	        
	        // this will carry over the isNeedingCartUtilityUpdate flag during the same session
	        if (shoppingCart.isCurrentEmpty()) {
	          if ((ecId != null) && ecId.contains(brandSpecs.getLinkedEmailPrefix())) {
	            retrieveCartFromLinkedEmail(request, profile, shoppingCart);
	          }
	        }
	        
	        final NMOrderHolder orderHolder = (NMOrderHolder) shoppingCart;
	        
	        synchronized (order) {
		        try {
		          getCartUtility().clearDeletedProducts((NMOrderManager) getOrderManager(), order);
		        } catch (final Exception e) {
		          logError("loadUserShoppingCartForLogin getCartUtility.clearDeletedProducts had error " + e);
		          
		        }
		        
		        order.setSpcState(NMOrderImpl.SPC_STATE_NONE);
		        order.setValidateCountry(true);
		        // order.clearPWPRejectPromoMap();
		        
		        if (!order.isTransient()) {
		          repriceOrder(order, profile, pricingmodelholder, pLocale, getRepriceOrderPricingOp());
		          persistShoppingCarts(profile, shoppingCart);
		          if (isCallingCartUtility()) {
		           // synchronized (order) {
		              if (orderHolder.isNeedingCartUtilityUpdate()) {
		                getCartUtility().updateShoppingCart((NMOrderManager) getOrderManager(), order, orderHolder);
		              }
		          //  }
		          } else { // if we disable SCP and CartUtility then this will run instead.
		            //synchronized (order) {
		              clearGiftCards(order);
		           // }
		          }
		          
		        }
	      	}
	        
	      } catch (final CommerceException e) {
	       
	    	shouldRollback = true;  
	    	// lets start printing to logerror-rather than dump to output
	        if (isLoggingError()) {
	          
	          logError("NMCPT.loadUserShoppingCartForLogin() failed due to a commerce exception: " + e.getMessage());
	        }
	        
	      } catch (final Exception e) {
	        
	    	shouldRollback = true;
	    	// lets start printing to logerror-rather than dump to output
	        if (isLoggingError()) {
	          
	          logError("NMCPT.loadUserShoppingCartForLogin() failed due to a exception: " + e.getMessage());
	        }
	        
	    } finally {
			try {
				td.end(shouldRollback);
			} catch (Throwable th) {
				logError(th);
			}
		  }
	      
	    }
    } catch (TransactionDemarcationException e) {
		vlogError("NMCommerceProfileTools:loadUserShoppingCartForLogin : TransactionDemarcationException " + e.getMessage());
	} catch (DeadlockException e) {
		vlogError("NMCommerceProfileTools:loadUserShoppingCartForLogin : DeadlockException " + e.getMessage());
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
  
  /**
   * Convenience method to do the debug checking and debug logging together. Assumes user of method only ever wants it logged if its enabled.
   * 
   * @param pMessage
   *          the message that is displayed IF isLoggingDebug() returns true.
   */
  public final void checkedLogDebug(final String pMessage) {
    if (isLoggingDebug()) {
      logDebug(pMessage);
    }
  }
  
  /**
   * Getter for the cartUtiliy property. Returns the cartUtility component
   * 
   * @return
   */
  public CartUtility getCartUtility() {
    return cartUtility;
  }
  
  /**
   * Setter for the cartUtility property.
   * 
   * @param utility
   */
  public void setCartUtility(final CartUtility cartUtility) {
    this.cartUtility = cartUtility;
  }
  
  private CartUtility cartUtility;
  
  /**
   * This method returns an instance of NMProfile for a given repositoryId.
   * 
   * @see CsrGiftListHandler.handleUpdateGiftRegistry
   * @see CsrGiftListHandler.handleUpdateSKUGiftRegistry
   */
  public NMProfile getNMProfile(final String repositoryId) {
    NMProfile nmProfile = null;
    try {
      final RepositoryItem repItem = getProfileRepository().getItem(repositoryId, ProfileRepository.USER_DESCRIPTOR);
      // RepositoryItem repItem = getProfileRepository().getItem(
      // repositoryId);
      nmProfile = new NMProfile();
      if (repItem != null) {
        nmProfile.setDataSource(repItem);
        nmProfile.getTransActiveGiftRegistry();
      }
    } catch (final Exception e) {
      e.printStackTrace();
    }
    return nmProfile;
  }
  
  private PromotionEvaluation promotionEvaluation;
  
  public PromotionEvaluation getPromotionEvaluation() {
    return promotionEvaluation;
  }
  
  public void setPromotionEvaluation(final PromotionEvaluation promotionEvaluation) {
    this.promotionEvaluation = promotionEvaluation;
  }
  
  /**
   * This method will attempt to compare the version of the session based order object with the version of the order in the repository. If the object version is less than the repository version, then
   * this method will refresh the order object with the latest data from the repository. If the object version is greater than the repository version, then this method will sleep in an attempt to let
   * the repository "catch up" with the latest updates.
   */
  public void syncOrderWithRepositoryIfNeccessary(final DynamoHttpServletRequest pRequest, final DynamoHttpServletResponse pResponse) throws IOException, ServletException {
    final OrderHolder orderHolder = (OrderHolder) pRequest.resolveName(mShoppingCartPath);
    
    if (!orderHolder.isCurrentExists()) {
      return;
    }
    
    NMOrderImpl order = (NMOrderImpl) orderHolder.getCurrent();
    
    if (order == null) {
      return;
    }
    
    int objectVersion = order.getVersion();
    int repositoryVersion = ((Integer) order.getRepositoryItem().getPropertyValue("version")).intValue();
    
    if (objectVersion == repositoryVersion) {
      return;
    } else if (objectVersion > repositoryVersion) {
      try {
        if (isLoggingInfo()) {
          logInfo("object version is greater than repository version, attempting to sleep");
        }
        
        Thread.sleep(500);
      } catch (final InterruptedException e) {
        if (isLoggingError()) {
          logError(e);
        }
      }
      
      return;
    } else if (objectVersion < repositoryVersion) {
    	
        if (isLoggingInfo()) {
	          logInfo("object version is less than repository version, attempting to refresh order");
	          logInfo("before refresh: objectVersion=" + objectVersion + " repositoryVersion=" + repositoryVersion);
	        }

        try{
	        // explicitly remove order relationships from repository cache, not sufficient to just remove the order from cache
	        final List shipRelList = getOrderManager().getShippingGroupManager().getAllShippingGroupRelationships(order);
	        final List payRelList = getOrderManager().getPaymentGroupManager().getAllPaymentGroupRelationships(order);
	        final List relList = new ArrayList();
	        
	        relList.addAll(shipRelList);
	        relList.addAll(payRelList);
	        
	        final Iterator relListIterator = relList.iterator();
	        Relationship relationship;
	        
	        while (relListIterator.hasNext()) {
	          relationship = (Relationship) relListIterator.next();
	          ((atg.adapter.gsa.GSAItemDescriptor) getOrderManager().getOrderTools().getOrderRepository().getItemDescriptor("relationship")).invalidateCachedItem(relationship.getId());
	          ((atg.adapter.gsa.GSAItemDescriptor) getOrderManager().getOrderTools().getOrderRepository().getItemDescriptor("shipItemRel")).invalidateCachedItem(relationship.getId());
	          ((atg.adapter.gsa.GSAItemDescriptor) getOrderManager().getOrderTools().getOrderRepository().getItemDescriptor("payOrderRel")).invalidateCachedItem(relationship.getId());
	        }
    	}  catch (final Exception e) {
	        if (isLoggingError()) {
	            logError(e);
	          }
	    } 

    	final NMProfile nmProfile = (NMProfile) ServletUtil.getCurrentRequest().resolveName("/atg/userprofiling/Profile");
    	String profileId = nmProfile.getRepositoryId();
    	boolean acquireLock = false;
    	
    	final SystemSpecs systemSpecs = (SystemSpecs) Nucleus.getGlobalNucleus().resolveName("/nm/utils/SystemSpecs");
    	boolean useClientLockForOrderUpdateInDAFPipeine = false;
        
        if (systemSpecs != null)
        	useClientLockForOrderUpdateInDAFPipeine = systemSpecs.isUseClientLocksForOrderUpdateInPipeline();
    	else {
	    	if (isLoggingError()) {
	    		logError("NMCommerceProfileTools:syncOrderWithRepositoryIfNeccessary :- systemSpecs is NULL. Client Lock will not be used.");
	    	}
        }
    	
	      try {
	        
	        if(useClientLockForOrderUpdateInDAFPipeine) {
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
		        // save a copy of the session based promo code so that we can re apply it later
		        final String originalPromoCode = order.getTransientPromoCode();
		        
		        // rebuild the session based order object from the repository
		        ((NMOrderHolder) orderHolder).refreshCurrent();
		        order = (NMOrderImpl) orderHolder.getCurrent();
		        
		        synchronized (order) {
			        // re apply the original session based promo code
			        if ((originalPromoCode != null) && (!originalPromoCode.trim().equals(""))) {
			          String newPromoCode = order.getPromoCode();
			          
			          if (newPromoCode == null) {
			            newPromoCode = "";
			          }
			          
			          newPromoCode += originalPromoCode;
			          order.setPromoCode(newPromoCode);
			        }
			        
			        // re evaluate promotions and reprice the order to restore any promos that may have been removed by refresh
			        final ShoppingCartHandler sch = (ShoppingCartHandler) pRequest.resolveName("/atg/commerce/order/ShoppingCartModifier");
			        
			        if (sch != null) {
			          sch.handleRepriceOrder(pRequest, pResponse);
			          promotionEvaluation.evaluatePromotions(order);
			          sch.handleRepriceOrder(pRequest, pResponse);
			        }
		        }
		        
		        objectVersion = order.getVersion();
		        repositoryVersion = ((Integer) order.getRepositoryItem().getPropertyValue("version")).intValue();
		        
		        if (isLoggingInfo()) {
		          logInfo("after refresh: objectVersion=" + objectVersion + " repositoryVersion=" + repositoryVersion);
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
				vlogError("NMCommerceProfileTools:syncOrderWithRepositoryIfNeccessary : TransactionDemarcationException " + e.getMessage());
			} catch (DeadlockException e) {
				vlogError("NMCommerceProfileTools:syncOrderWithRepositoryIfNeccessary : DeadlockException " + e.getMessage());
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
      
      return;
    }
  }
  
  public void setEdwCustData(final NMProfile profile, final String cookieWebId) {
    String webId = null;
    String webIdPrefix = null;
    String emailId = null;
    String linkedEmailId = null;
    EDWCustData custData = null;
    CustomerDataVO custDataVO = null;
    
    webIdPrefix = CommonComponentHelper.getBrandSpecs().getWebIdPrefix();
    
    if (null != webIdPrefix) {
      if (null != cookieWebId) {
        webId = webIdPrefix.concat(cookieWebId);
      } else {
        webId = webIdPrefix.concat(profile.getWebId());
        
      }
    }
    
    final DynamoHttpServletRequest request = ServletUtil.getCurrentRequest();
    final Cookie omnitureCookie = NMCookieManager.getCookie(request.getCookies(), NMCookieManager.OMNITURE_COOKIE);
    String omnitureCookieValue = null;
    custDataVO = new CustomerDataVO();
    if (null != omnitureCookie) {
      omnitureCookieValue = omnitureCookie.getValue();
      custDataVO.setOmnitureCookieValue(omnitureCookieValue);
    }
    
    if (null != webId) {
      emailId = profile.getEmailAddressBeanProp();
      linkedEmailId = profile.getLinkedEmail();
      final boolean isInternational = CheckoutAPI.isInternationalSession(profile);
      custDataVO.setWebId(webId);
      custDataVO.setInternational(isInternational);
      if (StringUtilities.isNotEmpty(emailId)) {
        custDataVO.setEmailId(emailId);
        custData = getBaseCXPClient().getEdwCustData(custDataVO);
      } else if (StringUtilities.isNotEmpty(linkedEmailId)) {
        custDataVO.setEmailId(linkedEmailId);
        custData = getBaseCXPClient().getEdwCustData(custDataVO);
      } else {
        custData = getBaseCXPClient().getEdwCustData(custDataVO);
      }
      
      profile.setEdwCustData(custData);
      
      if (isLoggingDebug()) {
        logDebug("ProfileId is :  " + profile.getRepositoryId() + "  WebId is :  " + webId + "   UCID is : " + profile.getUcId() + " LinkedEmailId is : " + linkedEmailId + " EmailId is : " + emailId);
        logDebug("Omniture Cookie value is :    " + omnitureCookieValue);
      }
    } else {
      if (isLoggingError()) {
        logError("webIdPrefix or webId is null ");
      }
    }
    
  }
  
  /** This method checks if the mergeCart popup to be shown to user.
   * @param persistentOrder persistent order
   * @param current current cart
   * @param profile profile
   * @return true if mergeCart popup to be shown.
   */
  @SuppressWarnings("unchecked")
  private boolean checkShowMergeCart(final NMOrderImpl persistentOrder,final Order current, final RepositoryItem profile) {
    
    final List<NMCommerceItem> persCommerceItems = persistentOrder.getCommerceItems();
    final List<NMCommerceItem> anonCommerceItems = current.getCommerceItems();
    
    final List<String> anonIds = new ArrayList<String>();
    final List<String> persIds = new ArrayList<String>();
    int count = 0;
    
    for (final NMCommerceItem dci : anonCommerceItems) {
      if (!dci.isGwpItem()) {
        final String skuId = dci.getCatalogRefId();
        anonIds.add(skuId);
      }
    }
    
    for (final NMCommerceItem ci : persCommerceItems) {
      if ((ci != null) && (!ci.isGwpItem())) {
        final String skuId = ci.getCatalogRefId();
        persIds.add(skuId);
        
        if (anonIds.contains(skuId)) {
          count++;
          continue;
        }
      }
    }
    
    final NMProfile nmProfile = getNMProfile(profile.getRepositoryId());
    if (nmProfile.isRegisteredProfile()
            && (profile.getRepositoryId().equalsIgnoreCase(current.getProfileId()))) {
      return false;
    }
    
    final int anonCount = anonIds.size();
    final int persCount = persIds.size();
    
    // no items match
    if ((anonCount > 0) && (persCount > 0) && (count == 0)) {
      return true;
    }
    
    // all items match
    if (count == anonCount) {
      if (count == persCount) {
        return false;
      } else {
        return true;// persistent has more items
      }
    }
    
    // partial items match
    if (count < anonCount) {
      if (count == persCount) { // anonymous cart has more items
        return false;
      } else {
        return true; // both cart has different items
      }
    }
    return false;
  }
  
} // end class
