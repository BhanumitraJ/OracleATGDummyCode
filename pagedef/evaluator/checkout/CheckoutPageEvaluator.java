package com.nm.commerce.pagedef.evaluator.checkout;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpSession;
import javax.servlet.jsp.PageContext;
import javax.transaction.TransactionManager;

import atg.core.util.StringUtils;
import atg.dtm.TransactionDemarcation;
import atg.dtm.TransactionDemarcationException;
import atg.nucleus.Nucleus;
import atg.nucleus.logging.ApplicationLogging;
import atg.nucleus.logging.ClassLoggingFactory;
import atg.service.lockmanager.ClientLockManager;
import atg.service.lockmanager.DeadlockException;
import atg.servlet.DynamoHttpServletRequest;
import atg.servlet.DynamoHttpServletResponse;

import com.nm.abtest.AbTestHelper;
import com.nm.commerce.NMCreditCard;
import com.nm.commerce.NMOrderHolder;
import com.nm.commerce.NMProfile;
import com.nm.commerce.checkout.CheckoutAPI;
import com.nm.commerce.checkout.CheckoutComponents;
import com.nm.commerce.order.NMOrderManager;
import com.nm.commerce.pagedef.definition.CheckoutPageDefinition;
import com.nm.commerce.pagedef.definition.PageDefinition;
import com.nm.commerce.pagedef.model.CheckoutPageModel;
import com.nm.commerce.pagedef.model.bean.Breadcrumb;
import com.nm.commerce.promotion.NMOrderImpl;
import com.nm.common.INMGenericConstants;
import com.nm.components.CommonComponentHelper;
import com.nm.formhandler.ShoppingCartHandler;
import com.nm.profile.ProfileProperties;
import com.nm.sitemessages.Message;
import com.nm.sitemessages.MessageContainer;
import com.nm.utils.LocalizationUtils;
import com.nm.utils.SystemSpecs;

public class CheckoutPageEvaluator extends BaseCheckoutPageEvaluator {
  public static final String MINICART_ABTESTGROUP = "minicartABTest";
  LocalizationUtils utils = ((LocalizationUtils) Nucleus.getGlobalNucleus().resolveName("/nm/utils/LocalizationUtils"));
  
  //property: Logger
  private static ApplicationLogging mLogger =
		  ClassLoggingFactory.getFactory().getLoggerForClass(CheckoutPageEvaluator.class);
  
  @Override
  public boolean evaluate(final PageDefinition pageDefinition, final PageContext pageContext) throws Exception {
    if (!super.evaluate(pageDefinition, pageContext)) {
      return false;
    }
    
    final CheckoutPageModel pageModel = (CheckoutPageModel) getPageModel();
    
    final CheckoutPageDefinition checkoutPageDef = (CheckoutPageDefinition) pageDefinition;
    
    final DynamoHttpServletRequest request = getRequest();
    final DynamoHttpServletResponse response = getResponse();
    
    final NMProfile profile = CheckoutComponents.getProfile(request);
    final ShoppingCartHandler cart = CheckoutComponents.getCartHandler(request);
    NMOrderImpl order = getCurrentOrder(cart);
    //The current order when last order is null in case of a new session or order.
    if (order == null){
      getResponse().sendRedirect(CART_PATH);
      return false;
    }
    // setting the origination of the promotion to cart(0)/orderreview(1), by setting 0 or 1.this is for PLCC percent off promotion for LastCall.
    if ("Billing".equals(pageDefinition.getId()) || "Shipping".equals(pageDefinition.getId())) {
      order.setPromoOrigin(0);
    } else {
      order.setPromoOrigin(1);
    }
    
    final NMOrderManager orderMgr = (NMOrderManager) cart.getOrderManager();
    final MessageContainer messages = CheckoutComponents.getMessageContainer(request);
    
    /**
     * InternationalUser is not able to Move billing and Shipping pages because of below check, When click on Checkout button on Cart page thats'why The below check commented.MoreOver Domestic flow
     * also never check this loop. Because of above reasons commented below if condition.
     * 
     */
    if (profile.isSessionExpired()) {
      ((NMOrderHolder) cart.getOrderHolder()).setResponsiveState(NMOrderHolder.RESPONSIVE_STATE_NONE);
      getResponse().sendRedirect(CART_PATH);
    }
    
    final HttpSession session = request.getSession();
    final String activeSession = (String) session.getAttribute("NM_Checkout_SessionActive");
    if (!"YES".equalsIgnoreCase(activeSession) && profile.isAnonymous() && !profile.isRegisteredProfile()) {
      session.setAttribute("NM_Checkout_SessionActive", "YES");
      // Masterpass changes : Clear out MasterPass details when there is a session timeout
      if (order.isMasterPassPayGroupOnOrder()) {
        CheckoutAPI.clearMasterPassDetails(order);
      }
      response.sendRedirect(CART_PATH);
    }
    /**
     * To get the promotion messages set in the session. It will be removed from session after reading
     */
    final Message[] promoMessages = (Message[]) session.getAttribute(INMGenericConstants.NM_CHECKOUT_PROMOTION_MESSAGE);
    if ((null != promoMessages) && (promoMessages.length != 0)) {
      messages.addAll(Arrays.asList(promoMessages));
      session.removeAttribute(INMGenericConstants.NM_CHECKOUT_PROMOTION_MESSAGE);
    }
    
    String profileId = profile.getRepositoryId();
 	boolean acquireLock = false;
 	
 	ClientLockManager lockManager = CheckoutComponents.getClientLockManager(request);
 	TransactionManager transactionManager = CheckoutComponents.getTransactionManager(request);
 	final SystemSpecs systemSpecs = (SystemSpecs) Nucleus.getGlobalNucleus().resolveName("/nm/utils/SystemSpecs");
 	
 	 boolean useClientLockForOrderUpdate = false;
     
     if (systemSpecs != null)
    	 useClientLockForOrderUpdate = systemSpecs.isUseClientLocksForOrderUpdate();
 	
 	try {
 		
 		if(useClientLockForOrderUpdate)
 			acquireLock = !lockManager.hasWriteLock( profileId, Thread.currentThread());
		
        if ( acquireLock )
        	lockManager.acquireWriteLock( profileId, Thread.currentThread() );
        
        final TransactionDemarcation td = new TransactionDemarcation();
        
        td.begin( transactionManager, TransactionDemarcation.REQUIRED );
        boolean shouldRollback = false;
        
        try {
        	
        	synchronized (order) {
			    // Repricing is done here to get the latest information of order and promotion before making cmos call to fetch the service levels
			    CheckoutAPI.repriceOrder(cart, profile, -1);
			    updateShippingGroups(order, pageModel, checkoutPageDef);
			    // VisaCheckout - clearing visa checkout details when paypal session is initiated
			    final List<NMCreditCard> orderCreditCards = order.getCreditCards();
			    if ((orderCreditCards != null) && !orderCreditCards.isEmpty()) {
			      final NMCreditCard paymentGroup = orderCreditCards.get(0);
			      if ((paymentGroup != null) && paymentGroup.isPayPal() && paymentGroup.isVmeCard()) {
			        CheckoutAPI.clearVmeDetails(order);
			      }
			      // MasterPass Changes :Clear out MasterPass details when Paypal or vme session is initiated
			      if ((paymentGroup.isPayPal() || paymentGroup.isVmeCard() || CommonComponentHelper.getEmployeeDiscountsUtil().isAssociateLoggedIn(profile)) && paymentGroup.isMasterPassCard()) {
			        CheckoutAPI.clearMasterPassDetails(order);
			      }
			    }
			    CheckoutAPI.repriceOrder(cart, profile, -1);
        	}
        } catch (final Exception e) {
			shouldRollback = true;
	        if (getLogger().isLoggingError()) {
	        	getLogger().logError(e);
	          }
	    } finally {
			try {
				td.end(shouldRollback);
			} catch (Throwable th) {
				getLogger().logError(th);
			}
		}
    } catch (TransactionDemarcationException e) {
		 if (getLogger().isLoggingError())
			getLogger().logError("CheckoutPageEvaluator:evaluate : TransactionDemarcationException " + e.getMessage());
	} catch (DeadlockException e) {
		if (getLogger().isLoggingError())
			getLogger().logError("CheckoutPageEvaluator:evaluate : DeadlockException " + e.getMessage());
	} catch (final Exception e) {
		if (getLogger().isLoggingError())  
			getLogger().logError(e);
	} finally {
		try {
			if (acquireLock)
				lockManager.releaseWriteLock(profileId, Thread.currentThread(), true);
		} catch (Throwable th) {
			if (getLogger().isLoggingError())
				getLogger().logError(th);
		}
	}
    updatePromotionFields(cart);
    pageModel.setProfile(profile);
    // INT-1378 set country name in billing address section
    if (CheckoutAPI.isInternationalSession(profile)) {
      if ((profile.getBillingNMAddress() != null) && (profile.getBillingNMAddress().getCountry() != null)) {
        pageModel.setBillingCountry(utils.getCountryNameForCountryCode(profile.getBillingNMAddress().getCountry()).trim());
      }
    }
    pageModel.setOrder(order);
    
    if (order != null) {
      pageModel.setAmountRemainingOnOrder(orderMgr.getCurrentOrderAmountRemaining(order, true));
      pageModel.setCommerceItemRelationships(CheckoutAPI.getShippingGroupCommerceItemRelationships(order, orderMgr));
      
      pageModel.setIsShipFromStore(CheckoutAPI.getIsShipFromStore(order));
      final boolean multiShip = CheckoutAPI.getIsShipToMultiple(order);
      pageModel.setIsShipToMultiple(multiShip);
      pageModel.setIsShipToStore(order.hasAllBopsItems());
      // not setting the Multiple Service Levels flag because BOPS project removed the ability to change service levels at an item level
      // pageModel.setHasMultipleServiceLevels(multiShip && CheckoutAPI.getHasMultipleServiceLevels(order));
      pageModel.setHasDeliveryPhoneItems(!CheckoutAPI.getDeliveryPhoneItems(order, cart.getOrderManager()).isEmpty());
    }
    
    pageModel.setMessages(messages.getIdMap());
    pageModel.setMessageFields(messages.getFieldIdMap());
    pageModel.setMessageGroups(messages.groupByPrefix("group"));
    pageModel.addMessageGroups(messages.groupByMsgId());
    
    pageModel.setBreadcrumbs(getBreadcrumbs(checkoutPageDef, profile, order));
    pageModel.setCurrentBreadcrumb(getCurrentBreadcrumb(pageModel.getBreadcrumbs()));
    pageModel.setCanEditCart(checkoutPageDef.getCanEditCart());
    
    // Abtesting for the mini cart
    String minicartAbTestGroup = AbTestHelper.getAbTestValue(getRequest(), MINICART_ABTESTGROUP);
    if (minicartAbTestGroup == null) {
      minicartAbTestGroup = "inactive";
    }
    request.setParameter(MINICART_ABTESTGROUP, minicartAbTestGroup);
    
    // pageModel.getOmnitureBean().setExtraData( CheckoutComponents.getResultBeanContainer( getRequest() ).getResultBean() );
    
    return true;
  }
  
  /**
   * The purpose of this method is to update the shipping groups based on service levels. This calls the update shipping group method either in EstimatedDeliveryDateShippingGroupUtil in case of ESB
   * service flow or ShippingGroupUtil in case of database flow and the Service levels will be fetched either from Cmos or form the database based on the service call made
   * 
   * @param order
   *          the order
   * @param pageModel
   *          the page model
   * @param checkoutPageDefinition
   *          the checkout page definition
   */
  protected void updateShippingGroups(final NMOrderImpl order, final CheckoutPageModel pageModel, final CheckoutPageDefinition checkoutPageDefinition) {
    CheckoutAPI.updateShippingGroupsBasedOnServiceCall(order, pageModel, checkoutPageDefinition);
  }
  
  /* ************************************************* */
  /*
   * The below methods are moved to ShippingInfoUtil class as part of Estimated Delivery Date project, As part of estimated delivery date projects in few scenarios we get the service levels from cmos
   * through ESB call, if the ESB call is not made or the ESB call to cmos fails we use the old method, to support both the flows we used factory design pattern and moved these methods to
   * ShippingInfoUtil class
   */
  /* updateShippingGroup() */
  /* getShippingGroup() */
  /* findSelectedServiceLevelCode() */
  /* getDeliveryPhoneShippingGroups() */
  /* getValidServiceLevels() */
  /* setItemServiceLevelToGroupServiceLevel() */
  /* ************************************************* */
  
  protected Breadcrumb[] getBreadcrumbs(final CheckoutPageDefinition currentPageDef, final NMProfile profile, final NMOrderImpl order) {
    Breadcrumb[] breadcrumbs;
    final String currentTitle = currentPageDef.getBreadcrumbTitle();
    boolean currentFound = false;
    final String payPalToken = CheckoutAPI.getPayPalToken(getRequest());
    final boolean displayPayPalInfo = CheckoutAPI.isDisplayPayPalInfo(profile, payPalToken);
    final List<NMCreditCard> orderCreditCards = order.getCreditCards();
    /* INT -581 Start */
    final String intlPayerID = getRequest().getParameter("PayerID");
    final String expressPayPalValue = getRequest().getParameter("isExpressPayPal");
    /* INT -581 End */
    final String last = getRequest().getParameter("prev");
    int lastPage;
    try {
      lastPage = Integer.parseInt(last);
    } catch (final NumberFormatException e) {
      lastPage = 0;
    }
    
    if (lastPage > 4) {
      lastPage = 0;
    }
    
    final String defaultCardKey = ProfileProperties.Profile_defaultCreditCard;
    Map<String, NMCreditCard> profileCards = null;
    if (profile != null) {
      profileCards = profile.getCreditCardMap();
    }
    
    final CheckoutPageDefinition shipping = CheckoutComponents.getShippingPageDefinition();
    final CheckoutPageDefinition billing = CheckoutComponents.getBillingPageDefinition();
    final CheckoutPageDefinition review = CheckoutComponents.getOrderReviewPageDefinition();
    final CheckoutPageDefinition confirm = CheckoutComponents.getOrderConfirmationPageDefinition();
    
    breadcrumbs =
            new Breadcrumb[] {new Breadcrumb(billing.getBreadcrumbUrl(), billing.getBreadcrumbTitle(), 1) , new Breadcrumb(shipping.getBreadcrumbUrl(), shipping.getBreadcrumbTitle(), 2) ,
                new Breadcrumb(review.getBreadcrumbUrl(), review.getBreadcrumbTitle(), 3) , new Breadcrumb(confirm.getBreadcrumbUrl(), confirm.getBreadcrumbTitle(), 4)};
    
    if (currentTitle != null) {
      for (final Breadcrumb breadcrumb : breadcrumbs) {
        if (!currentFound && currentTitle.equals(breadcrumb.getTitle())) {
          currentFound = true;
          breadcrumb.setCurrentPage(true);
          if (currentPageDef.getBreadcrumbSubpage()) {
            breadcrumb.setClickable(true);
            breadcrumb.setPageNum(0);
          }
        }
        if (!currentFound || (breadcrumb.getPageNum() <= lastPage)) {
          breadcrumb.setClickable(true);
        }
        /* INT -581 Start */
        if (!StringUtils.isBlank(intlPayerID) && CheckoutAPI.isInternationalSession(profile)) {
          if (breadcrumb.getTitle().equals(billing.getBreadcrumbTitle()) && StringUtils.isBlank(expressPayPalValue)) {
            breadcrumb.setClickable(false);
          } else if (breadcrumb.getTitle().equals(billing.getBreadcrumbTitle()) && !StringUtils.isBlank(expressPayPalValue)) {
            breadcrumb.setClickable(false);
          } else if (breadcrumb.getTitle().equals(shipping.getBreadcrumbTitle()) && !StringUtils.isBlank(expressPayPalValue)) {
            breadcrumb.setClickable(false);
          }
        } else {
          if (breadcrumb.getTitle().equals(billing.getBreadcrumbTitle())
                  && (!orderCreditCards.isEmpty() && (orderCreditCards.get(0).isPayPal() || orderCreditCards.get(0).isVmeCard() || orderCreditCards.get(0).isMasterPassCard()))) {
            breadcrumb.setClickable(false);
          }
        }
        
      }
    }
    
    return breadcrumbs;
  }
  
  protected Breadcrumb getCurrentBreadcrumb(final Breadcrumb[] breadcrumbs) {
    Breadcrumb current = null;
    for (final Breadcrumb b : breadcrumbs) {
      if (b.isCurrentPage()) {
        current = b;
      }
    }
    return current;
  }
  
  /**
  * @return ApplicationLogging object for logger.
  */
  private ApplicationLogging getLogger() {
	  return mLogger;
  }
}
