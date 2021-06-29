/*
 * <ATGCOPYRIGHT> Copyright (C) 1997-2001 Art Technology Group, Inc. All Rights Reserved. No use, copying or distribution of this work may be made except in accordance with a valid license agreement
 * from Art Technology Group. This notice must be included on all copies, modifications and derivatives of this work.
 * 
 * Art Technology Group (ATG) MAKES NO REPRESENTATIONS OR WARRANTIES ABOUT THE SUITABILITY OF THE SOFTWARE, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, OR NON-INFRINGEMENT. ATG SHALL NOT BE LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR DISTRIBUTING THIS SOFTWARE OR
 * ITS DERIVATIVES.
 * 
 * "Dynamo" is a trademark of Art Technology Group, Inc. </ATGCOPYRIGHT>
 */

package com.nm.commerce.order;

import static com.nm.common.INMGenericConstants.DOUBLE_ZERO;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import atg.commerce.CommerceException;
import atg.commerce.order.CommerceItem;
import atg.commerce.order.CreditCard;
import atg.commerce.order.InvalidParameterException;
import atg.commerce.order.InvalidVersionException;
import atg.commerce.order.Order;
import atg.commerce.order.OrderTools;
import atg.commerce.order.PaymentGroupImpl;
import atg.commerce.order.PaymentGroupOrderRelationship;
import atg.commerce.order.ShippingGroup;
import atg.commerce.order.ShippingGroupCommerceItemRelationship;
import atg.commerce.order.SimpleOrderManager;
import atg.commerce.profile.CommerceProfileTools;
import atg.core.util.ResourceUtils;
import atg.repository.MutableRepository;
import atg.repository.RepositoryException;
import atg.repository.RepositoryItem;

import com.nm.commerce.GiftCard;
import com.nm.commerce.NMCreditCard;
import com.nm.commerce.promotion.NMOrderImpl;

/**
 * Modified by
 * 
 * @author Chee-Chien Loo
 **/

public class NMOrderManager extends SimpleOrderManager {
  
  static final String MY_RESOURCE_NAME = "atg.commerce.order.OrderResources";
  private static final int ORDER_VERSION_MISMATCH_DELAY = 1000;
  
  /** Resource Bundle **/
  protected static java.util.ResourceBundle sResourceBundle = java.util.ResourceBundle.getBundle(MY_RESOURCE_NAME, java.util.Locale.getDefault());
  
  private boolean refreshEnabled = false;
  
  public void setRefreshEnabled(final boolean refreshEnabled) {
    this.refreshEnabled = refreshEnabled;
  }
  
  public boolean isRefreshEnabled() {
    return refreshEnabled;
  }
  
  public double getCurrentOrderAmountRemaining(final Order order) {
    return (getCurrentOrderAmountRemaining(order, false));
  }
  
  public double getCurrentOrderAmountRemaining(final Order order, final boolean excludeCreditCards) {
    if (order == null) {
      return 0.0d;
    }
    
    @SuppressWarnings("unchecked")
    final List<PaymentGroupOrderRelationship> paymentGroupRelationships = order.getPaymentGroupRelationships();
    final Iterator<PaymentGroupOrderRelationship> pgrIterator = paymentGroupRelationships.iterator();
    double amountAppliedToOrder = 0;
    
    while (pgrIterator.hasNext()) {
      final CreditCard cc = (atg.commerce.order.CreditCard) (pgrIterator.next()).getPaymentGroup();
      if (excludeCreditCards && !cc.getCreditCardType().equals("NEX")) {
        continue;
      }
      
      amountAppliedToOrder += cc.getAmount();
    }
    
    double amt = null != order.getPriceInfo() ? order.getPriceInfo().getTotal() - amountAppliedToOrder : DOUBLE_ZERO;
    if (amt < 0.0) {
      long longAmt = (long) (amt * 100.0);
      if (-0L == longAmt) {
        longAmt = 0L;
      }
      
      amt = longAmt / 100.0;
    }
    
    return (amt);
  }
  
  public void removeAllGiftCardsFromOrder(final Order order) {
    @SuppressWarnings("unchecked")
    final ArrayList<PaymentGroupImpl> paymentGroupClonedList = new ArrayList<PaymentGroupImpl>(order.getPaymentGroups());
    final Iterator<PaymentGroupImpl> paymentGroupIterator = paymentGroupClonedList.iterator();
    PaymentGroupImpl paymentGroup;
    
    try {
      while (paymentGroupIterator.hasNext()) {
        paymentGroup = paymentGroupIterator.next();
        if ((paymentGroup instanceof atg.commerce.order.CreditCard) && (((NMCreditCard) paymentGroup).getCreditCardType() != null)
                && ((NMCreditCard) paymentGroup).getCreditCardType().equals(GiftCard.GIFT_CARD_CODE)) {
          getPaymentGroupManager().removePaymentGroupFromOrder(order, paymentGroup.getId());
        }
      }
      updateOrder(order);
    } catch (final Exception e) {
      e.printStackTrace();
    }
  }
  
  // -------------------------------------
  /**
   * This method adds a SEPARATE new item to the order and shippingGroup. The item are then added to the given shippingGroup. If the item existed then the quantity is added to the shippingGroup only.
   * 
   * @param pOrder
   *          the order which the item is to be added to
   * @param pCatalogRefId
   *          the catalogRefId of the item being added
   * @param pProductId
   *          the productId of the item being added
   * @param pQuantity
   *          the quantity to add
   * @param pShippingGroup
   *          the shippingGroup to add the item to
   * @exception CommerceException
   *              thrown if the operation could not be completed. More information can be found within the exception.
   * @return CommerceItem the CommerceItem which was added or the one which the quantity was updated
   */
  public CommerceItem addSeparateItemToShippingGroup(final Order pOrder, final String pCatalogRefId, final String pProductId, final long pQuantity, final ShippingGroup pShippingGroup)
          throws CommerceException {
    // check for null parameters
    if (pOrder == null) {
      throw new InvalidParameterException(ResourceUtils.getMsgResource("InvalidOrderParameter", MY_RESOURCE_NAME, sResourceBundle));
    }
    
    if (pCatalogRefId == null) {
      throw new InvalidParameterException(ResourceUtils.getMsgResource("InvalidCatalogRefIdParameter", MY_RESOURCE_NAME, sResourceBundle));
    }
    
    if (pProductId == null) {
      throw new InvalidParameterException(ResourceUtils.getMsgResource("InvalidProductIdParameter", MY_RESOURCE_NAME, sResourceBundle));
    }
    
    if (pQuantity <= 0) {
      throw new InvalidParameterException(ResourceUtils.getMsgResource("InvalidQuantityParameter", MY_RESOURCE_NAME, sResourceBundle));
    }
    
    if (pShippingGroup == null) {
      throw new InvalidParameterException(ResourceUtils.getMsgResource("InvalidShippingGroupParameter", MY_RESOURCE_NAME, sResourceBundle));
    }
    
    CommerceItem item = null;
    
    try {
      final RepositoryItem catalogRef = getCatalogTools().findSKU(pCatalogRefId);
      if (catalogRef == null) {
        throw new InvalidParameterException(ResourceUtils.getMsgResource("InvalidCatalogRefIdParameter", MY_RESOURCE_NAME, sResourceBundle));
      }
      
      final RepositoryItem productRef = getCatalogTools().findProduct(pProductId);
      if (productRef == null) {
        throw new InvalidParameterException(ResourceUtils.getMsgResource("InvalidProductIdParameter", MY_RESOURCE_NAME, sResourceBundle));
      }
      
      item = getCommerceItemManager().createCommerceItem(pCatalogRefId, catalogRef, pProductId, productRef, pQuantity);
      
      // this is the only different from addItemToShippingGroup
      item = getCommerceItemManager().addAsSeparateItemToOrder(pOrder, item);
      
    } catch (final RepositoryException e) {
      throw new CommerceException(e);
    }
    
    getCommerceItemManager().addItemQuantityToShippingGroup(pOrder, item.getId(), pShippingGroup.getId(), pQuantity);
    
    return item;
  }
  
  protected int getLatestRepoVersion(final NMOrderImpl order) { // note: returns 0 if it fails
    int i = 0;
    final MutableRepository mutablerepository = (MutableRepository) getOrderTools().getOrderRepository();
    RepositoryItem repositoryitem = null;
    try {
      repositoryitem = mutablerepository.getItem(order.getId(), getOrderItemDescriptorName());
    } catch (final RepositoryException repositoryexception) {
      ; // ignore silently
    }
    if (repositoryitem != null) {
      final Integer integer = (Integer) repositoryitem.getPropertyValue("version");
      if (integer != null) {
        i = integer.intValue();
      }
    }
    return i;
  }
  
  /*
   * Not sure how to use resolve type differences here.
   */
  @SuppressWarnings({"rawtypes" , "unchecked"})
  public List<ShippingGroupCommerceItemRelationship> getShippingGroupCommerceItemRelationships(final Order pOrder) throws CommerceException {
    final List /* <CommerceItemRelationship> */ciRels = getCommerceItemManager().getAllCommerceItemRelationships(pOrder);
    final List /* <ShippingGroupRelationship> */sgRels = getShippingGroupManager().getAllShippingGroupRelationships(pOrder);
    ciRels.retainAll(sgRels);
    return ciRels;
  }
  
  public void refreshOrder(final NMOrderImpl order) {
    if (isRefreshEnabled() && (order.getVersion() < getLatestRepoVersion(order))) {
      order.refresh();
    }
  }
  
  /**
   * Override of the super's method to add copying of our NMOrderImpl specific properties/fields. (ATG version of cloning an Order)
   * 
   * @author nmtem1
   */
  @Override
  public void mergeOrders(final Order pSrcOrder, final Order pDestOrder, final boolean pMergeShippingGroups, final boolean pRemoveSrcOrder) throws CommerceException {
    super.mergeOrders(pSrcOrder, pDestOrder, pMergeShippingGroups, pRemoveSrcOrder);
    // copy/add our getter/setters here
    if ((pSrcOrder instanceof NMOrderImpl) && (pDestOrder instanceof NMOrderImpl)) {
      final NMOrderImpl nmsrc = (NMOrderImpl) pSrcOrder;
      final NMOrderImpl nmdest = (NMOrderImpl) pDestOrder;
      nmdest.setSystemCode(nmsrc.getSystemCode());
      nmdest.setTotalPromotionalItemValue(nmsrc.getTotalPromotionalItemValue());
      nmdest.setLostShipping(nmsrc.getLostShipping());
      nmdest.setPromoCode(nmsrc.getPromoCode());
      nmdest.setPromoName(nmsrc.getPromoName());
      nmdest.setActivatedPromoCode(nmsrc.getActivatedPromoCode());
      nmdest.setCcAppStatus(nmsrc.getCcAppStatus());
      nmdest.setCcAppStatusTransient(nmsrc.getCcAppStatusTransient());
      nmdest.setLsSiteId(nmsrc.getLsSiteId());
      nmdest.setLsSiteTime(nmsrc.getLsSiteTime());
      nmdest.setMid(nmsrc.getMid());
    }
  }
  
  /*
   * updateOrder is being overriden to supress stack trace display in logs. InvalidVersionException is caught within our updateOrder and only the exception message will be printed to the logs.
   * 
   * @see atg.commerce.order.OrderManager#updateOrder(atg.commerce.order.Order)
   */
  @Override
  public void updateOrder(final Order arg0) throws CommerceException {
    try {
      if (isLoggingDebug()) {
        logDebug("order object version: " + ((atg.commerce.order.OrderImpl) arg0).getVersion());
        logDebug("order object repository version: " + ((Integer) ((atg.commerce.order.OrderImpl) arg0).getRepositoryItem().getPropertyValue("version")).intValue());
        
        try {
          final MutableRepository mutRep = (MutableRepository) getOrderTools().getOrderRepository();
          final RepositoryItem repItem = mutRep.getItem(arg0.getId(), getOrderItemDescriptorName());
          final Integer versionObj = (Integer) repItem.getPropertyValue("version");
          final int version = versionObj.intValue();
          logDebug("repository version: " + version);
        } catch (final RepositoryException e) {
          logError(e);
        }
      }
      
      final int orderVersion = ((atg.commerce.order.OrderImpl) arg0).getVersion();
      final int repositoryVersion = ((Integer) ((atg.commerce.order.OrderImpl) arg0).getRepositoryItem().getPropertyValue("version")).intValue();
      
      if (orderVersion != repositoryVersion) {
        try {
          if (isLoggingInfo()) {
            logInfo("version mismatch detected for order: " + arg0.getId());
            logInfo("orderVersion: " + orderVersion);
            logInfo("repositoryVersion: " + repositoryVersion);
            logInfo("attempting to sleep to allow versions to sync up");
          }
          
          Thread.sleep(ORDER_VERSION_MISMATCH_DELAY);
        } catch (final InterruptedException e) {
          if (isLoggingError()) {
            logError(e);
          }
        }
      }
      
      // call super to perform normal updateOrder operations
      super.updateOrder(arg0);
    } catch (final InvalidVersionException ive) {
      // if logError enabled, log the exception message only
      if (isLoggingError()) {
        logError(ive.getMessage());
      }
      // if logDebug enabled, log the stack trace
      // using getStackTrace so we can actually write to logDebug
      // and not dump to console
      if (isLoggingDebug()) {
        // this only works in Java 1.4.x
        // StackTraceElement ste[] = ive.getStackTrace();
        // for (int i = 0; i < ste.length; i++) {
        // logDebug(ste[i].toString());
        // }
        // writing output to console because of issues with
        // java 1.3.1
        ive.printStackTrace();
      }
    }
  }
  
  public RepositoryItem getProfileForOrder(final Order order) throws RepositoryException {
    final OrderTools orderTools = getOrderTools();
    final CommerceProfileTools profileTools = orderTools.getProfileTools();
    final RepositoryItem profile = profileTools.getProfileForOrder(order);
    return (profile);
  }
}
