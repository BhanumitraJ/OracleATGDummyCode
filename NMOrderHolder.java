package com.nm.commerce;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import atg.commerce.CommerceException;
import atg.commerce.order.CommerceItem;
import atg.commerce.order.Order;
import atg.commerce.order.OrderHolder;

import com.nm.formhandler.checkout.OrderSubmitFormHandler;

/**
 * @author nmmpe NMOrderHandler was created to hold a session level flg,NeedingCartUtilityUpdate. during a session, this flag tells the code not to run CartUtility.java on the shoppingcart as of
 *         5-9-06 this was needed to keep user-entered promocodes during a session, regardless of the user's state-either Anon or Registered.
 */
public class NMOrderHolder extends OrderHolder {
  
  
  // for Proclivity
  private ArrayList<ProclivityCommerceItem> commerceItemsToRemove = new ArrayList<ProclivityCommerceItem>();
  private ArrayList<ProclivityCommerceItem> commerceItemsToUpdate = new ArrayList<ProclivityCommerceItem>();
  
  // remember which GWP/PWPs the user has declined, by session
  private Set<String> declinedGwpSelects = new HashSet<String>();
  private Set<String> declinedPwps = new HashSet<String>();
  
  private boolean mNeedingCartUtilityUpdate = true;
  
  private String userReferer = new String();
  
  private int responsiveState = 0;
  
  public static final int RESPONSIVE_STATE_NONE = 0;
  public static final int RESPONSIVE_STATE_BILLING = 1;
  public static final int RESPONSIVE_STATE_SHIPPING = 2;
  public static final int RESPONSIVE_STATE_ORDER_REVIEW = 3;
  
  public NMOrderHolder() {}
  
  private TempPayment temporaryPayment;
  
  private TempUserInfo tempUserInfo = new TempUserInfo();
  
  public TempUserInfo getTempUserInfo() {
    return tempUserInfo;
  }
  
  public void setTempUserInfo(final TempUserInfo tempUserInfo) {
    this.tempUserInfo = tempUserInfo;
  }
  
  public void storeTemporaryPayment(final TempPayment p) {
    if (null == temporaryPayment && null != p) {
      temporaryPayment = new TempPayment(p);
    } else if (p != null) {
      temporaryPayment.update(p);
    } else {
      temporaryPayment = p;
    }
  }
  
  public void storeTemporaryUserInfo(final TempUserInfo userInfo) {
    
    if (null != userInfo) {
      tempUserInfo.update(userInfo);
    } else {
      tempUserInfo.clear();
    }
    
  }
  
  public TempPayment getTemporaryPayment() {
    return temporaryPayment;
  }
  
  public void processTemporaryCard(final OrderSubmitFormHandler handler) {
    if (temporaryPayment != null) {
      temporaryPayment.processStoredCard(handler);
    }
  }
  
  public void resetMaskedCard(final OrderSubmitFormHandler handler) {
    if (temporaryPayment != null) {
      temporaryPayment.resetMaskedCardOnError(handler);
    }
  }
  
  public void setNeedingCartUtilityUpdate(final boolean pNeedingCartUtilityUpdate) {
    mNeedingCartUtilityUpdate = pNeedingCartUtilityUpdate;
  }
  
  public boolean isNeedingCartUtilityUpdate() {
    return mNeedingCartUtilityUpdate;
  }
  
  private ArrayList<NMCommerceItem> addMonogram = new ArrayList<NMCommerceItem>();

  public ArrayList<NMCommerceItem> getAddMonogram() {
    return this.addMonogram;
  }

  public void setAddMonogram(final ArrayList<NMCommerceItem> pAddMonogram) {
    this.addMonogram = pAddMonogram;
  }

  public void clearAddMonogram() {
    this.addMonogram.clear();
  }
  
  public ArrayList<ProclivityCommerceItem> getCommerceItemsToRemove() {
    return commerceItemsToRemove;
  }
  
  public void setCommerceItemsToRemove(final ArrayList<ProclivityCommerceItem> commerceItemsToRemove) {
    this.commerceItemsToRemove = commerceItemsToRemove;
  }
  
  public void addProclivityItemToRemove(final CommerceItem commerceItem) {
    try {
      final ProclivityCommerceItem pci = new ProclivityCommerceItem();
      pci.setProductId(commerceItem.getAuxiliaryData().getProductId());
      pci.setPrice(String.valueOf(commerceItem.getPriceInfo().getAmount()));
      pci.setSku(commerceItem.getCatalogRefId());
      pci.setQty(String.valueOf(commerceItem.getQuantity()));
      getCommerceItemsToRemove().add(pci);
      // NMProfile p = new NMProfile();
    } catch (final Exception e) {}
  }
  
  public ArrayList<ProclivityCommerceItem> getCommerceItemsToUpdate() {
    return commerceItemsToUpdate;
  }
  
  public void setCommerceItemsToUpdate(final ArrayList<ProclivityCommerceItem> commerceItemsToUpdate) {
    this.commerceItemsToUpdate = commerceItemsToUpdate;
  }
  
  public void addProclivityItemToUpdate(final CommerceItem commerceItem, final int currentQty, final int newQty) {
    try {
      final ProclivityCommerceItem pci = new ProclivityCommerceItem();
      pci.setProductId(commerceItem.getAuxiliaryData().getProductId());
      pci.setPrice(String.valueOf(commerceItem.getPriceInfo().getAmount()));
      pci.setSku(commerceItem.getCatalogRefId());
      pci.setQty(String.valueOf(commerceItem.getQuantity()));
      if (newQty < currentQty) {
        pci.setUpdateType(ProclivityCommerceItem.UpdateType.REMOVE);
        pci.setQty(String.valueOf(currentQty - newQty));
      } else if (newQty > currentQty) {
        pci.setUpdateType(ProclivityCommerceItem.UpdateType.ADD);
        pci.setQty(String.valueOf(newQty - currentQty));
      }
      
      getCommerceItemsToUpdate().add(pci);
      // NMProfile p = new NMProfile();
    } catch (final Exception e) {}
  }
  
  public void refreshCurrent() {
    setCurrent(null);
  }
  
  // exists solely to clear the list of items that
  // were removed after sending them to proclivity
  public String getClearCommerceItemsToRemove() {
    getCommerceItemsToRemove().clear();
    return "";
  }
  
  public String getClearCommerceItemsToUpdate() {
    getCommerceItemsToUpdate().clear();
    return "";
  }
  
  public void clearTagging() {
    getCommerceItemsToRemove().clear();
  }
  
  public Order createNewOrder() throws CommerceException {
    final Order newOrder = createInitialOrder(getProfile());
    final Order oldOrder = getCurrent();
    if (oldOrder != null) {
      @SuppressWarnings("unchecked")
      final Collection<Order> savedOrders = getSaved();
      savedOrders.add(oldOrder);
    }
    
    setCurrent(newOrder);
    return newOrder;
  }
  
  public String getUserReferer() {
    return this.userReferer;
  }
  
  public void setUserReferer(final String pUserReferer) {
    this.userReferer = pUserReferer;
  }
  
  public Set<String> getDeclinedGwpSelects() {
    return declinedGwpSelects;
  }
  
  public void setDeclinedGwpSelects(final Set<String> declinedGwpSelects) {
    this.declinedGwpSelects = declinedGwpSelects;
  }
  
  public Set<String> getDeclinedPwps() {
    return declinedPwps;
  }
  
  public void setDeclinedPwps(final Set<String> declinedPwps) {
    this.declinedPwps = declinedPwps;
  }
  
  public int getResponsiveState() {
    return responsiveState;
  }
  
  public void setResponsiveState(final int responsiveState) {
    this.responsiveState = responsiveState;
  }
  
  @SuppressWarnings("rawtypes")
  public String getLastOrderItemIds() {
    if (this.getLast() != null && this.getLast().getCommerceItems() != null) {
      final StringBuffer sb = new StringBuffer();
      for (final Iterator iter = this.getLast().getCommerceItems().iterator(); iter.hasNext();) {
        final CommerceItem ci = (CommerceItem) iter.next();
        if (sb.length() > 0) {
          sb.append(',');
        }
        sb.append(ci.getId());
      }
      return sb.toString();
    } else {
      return new String();
    }
  }
}
