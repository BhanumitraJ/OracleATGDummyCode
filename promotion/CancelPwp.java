package com.nm.commerce.promotion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import javax.transaction.TransactionManager;

import atg.commerce.order.Order;
import atg.dtm.TransactionDemarcation;
import atg.dtm.TransactionDemarcationException;
import atg.repository.RepositoryException;
import atg.repository.RepositoryItem;

import com.nm.cmos.data.CmosOrder;
import com.nm.collections.NMPromotion;
import com.nm.collections.PurchaseWithPurchase;
import com.nm.collections.PurchaseWithPurchaseArray;
import com.nm.components.CommonComponentHelper;
import com.nm.formhandler.ShoppingCartHandler;
import com.nm.utils.PromotionsHelper;

public class CancelPwp extends CancelPromotion {
  private PurchaseWithPurchaseArray purchaseWithPurchaseArray;
  
  public CancelPwp() {
    super();
  }
  
  public List<String> getPwpsToBeCancelled(CmosOrder cmosOrder, List<String> cancelledLineItems) {
    ArrayList<String> aList = new ArrayList<String>();
    
    TransactionDemarcation td = new TransactionDemarcation();
    try {
      TransactionManager tm = CommonComponentHelper.getTransactionManager();
      if (tm != null) {
        td.begin(tm, TransactionDemarcation.REQUIRES_NEW);
      }
    } catch (TransactionDemarcationException tde) {
      tde.printStackTrace();
      return null;
    }// end-try
    
    try {
      // get web order id
      String orderId = getWebOrderId(cmosOrder.order_header.external_order_number);
      if (orderId == null || orderId.length() < 1) return null;
      
      // get the completed order
      NMOrderImpl completeOrder = getCompleteOrder(orderId);
      if (completeOrder == null) return null;
      
      synchronized (completeOrder) {
        adjustCIsForBogo(completeOrder, cmosOrder);

        // Need To Remove Line(s) Being Cancelled or already cancelled
        completeOrder = removeCancelledLines(completeOrder, cmosOrder, cancelledLineItems);
        
        // If no lines remain, then there is nothing else to do
        if (completeOrder.getCommerceItemCount() < 1) {
          return null;
        }
        
        // Need To Reprice Order
        completeOrder = repriceOrder(completeOrder);
        
        // map commerce item to promo keys
        HashMap<String, String> hmCIToPromoKey = getCIToPromoKey(completeOrder);
        
        // get all promotion keys from the order
        ArrayList<String> orderPromoKeys = getOrderPromoKeys(completeOrder);
        
        // Map cmos order line id to promo keys
        HashMap<String, String> hmCmosLineToPromoKeys = getCmosLineToPromoKeys(cmosOrder.order_detail.ship_to_customer, hmCIToPromoKey);
        
        // Are all of the remaining commerce items zero dollors
        if (completeOrder.getPriceInfo().getAmount() == 0) {
          aList.addAll(hmCmosLineToPromoKeys.keySet());
          getGenericLogger().debug(completeOrder.getId() + ": order amount: " + completeOrder.getPriceInfo().getAmount() + " had the following cmos line items auto cancelled: " + aList);
          return aList;
        }
        
        // get all archive promos for the submitted order date
        ArrayList<NMPromotion> alArchivePromos = new ArrayList<NMPromotion>();
        alArchivePromos.addAll(getPurchaseWithPurchaseArray().getAuditPromotions(completeOrder.getSubmittedDate()));
        if (alArchivePromos == null || alArchivePromos.isEmpty()) {
          return null;
        }
        
        // validate and add cmos order line id to list to be cancelled
        aList = getCmosOrderLinesToCancel(completeOrder, orderPromoKeys, alArchivePromos, hmCmosLineToPromoKeys);
      }
    } catch (Exception e) {
      logError("CheckCSRPWP.executeAction(): Exception " + e.getMessage());
    } finally {
      try {
        // always roll back, do not want to ever save order changes
        td.end(true); // don't commit work
      } catch (TransactionDemarcationException tde) {
        tde.printStackTrace();
      }
    }
    
    return aList;
  }
  
  protected ArrayList<String> getCmosOrderLinesToCancel(NMOrderImpl order, ArrayList<String> orderPromoKeys, ArrayList<NMPromotion> promoArray, HashMap<String, String> hmCmosLineToPromoKeys) {
    ArrayList<String> alCmosOrderLinesToCancel = new ArrayList<String>();
    PurchaseWithPurchase pwp;
    for (int i = 0; i < promoArray.size(); i++) {
      pwp = (PurchaseWithPurchase) promoArray.get(i);
      if (orderPromoKeys.contains(pwp.getCode())) {
        // always ignore online cat only flag
        // the active online catalog may have changed
        pwp.setOnlineCatOnly(Boolean.toString(false));
        if (!isValidatePromo(pwp, order)) {
          Iterator<String> iter = hmCmosLineToPromoKeys.keySet().iterator();
          while (iter.hasNext()) {
            String key = (String) iter.next();
            String[] strings = ((String) hmCmosLineToPromoKeys.get(key)).split(",");
            for (int pos = 0; pos < strings.length; pos++) {
              if (strings[pos].trim().equalsIgnoreCase(pwp.getCode())) {
                alCmosOrderLinesToCancel.add(key);
              }
            }
          }
        }
      }
    }
    return alCmosOrderLinesToCancel;
  }
  
  private boolean isValidatePromo(PurchaseWithPurchase pwp, Order order) {
    boolean isValid = false;
    // boolean onlineCatOnly = false;
    HashSet<String> ignoredProducts = new HashSet<String>();
    final String promoCode = pwp.getPromoCodes().trim().toUpperCase();
    // final String department = pwp.getDeptCodes().trim().toUpperCase();
    // final String classCode = pwp.getClassCodes().trim().toUpperCase();
    // final String depiction = pwp.getDepiction().trim().toUpperCase();
    // final String catalog = pwp.getQualifyCatalog().trim().toUpperCase();
    // final String onlineCatTest = pwp.getOnlineCatOnly().trim().toUpperCase();
    // final String designer = pwp.getVendor().trim().toUpperCase();
    // final String items = pwp.getQualifyItems().trim().toUpperCase();
    final String promoKey = pwp.getCode().trim().toUpperCase();
    StringTokenizer products = new StringTokenizer(pwp.getPwpProduct(), ",");
    while (products.hasMoreTokens()) {
      String prodId = products.nextToken();
      ignoredProducts.add(prodId);
    }
    String lvl = null;
    
    // double doubleDollar = 0.0;
    
    String dollarQualifier = pwp.getDollarQualifier();
    
    if (dollarQualifier != null && dollarQualifier.length() > 0) {
      // Double tempDollar = Double.valueOf(pwp.getDollarQualifier());
      // doubleDollar = tempDollar.doubleValue();
    }
    
    // onlineCatOnly = onlineCatTest.equals("TRUE");
    int type = 0;
    try {
      type = Integer.parseInt(pwp.getType().trim());
    } catch (NumberFormatException e) {
      // defaulted to 0
    }
    
    try {
      switch (type) {
      
        case 1: {
          if (promotionsHelper.orderHasProdDollar(order, promoKey, pwp.getDollarQualifier().trim(), pwp.getSaleQualificationName())) {
            isValid = true;
          }
          break;
        }
        
        case 2: {
          if (promotionsHelper.promoCodeMatch(order, promoCode, pwp.getDollarQualifier().trim())) {
            if (promotionsHelper.orderHasProdDollar(order, promoKey, pwp.getDollarQualifier().trim(), pwp.getSaleQualificationName())) {
              isValid = true;
            }
          }
          break;
        }
        
        case 3: {
          if (promotionsHelper.promoCodeMatch(order, promoCode, pwp.getDollarQualifier().trim())) {
            RepositoryItem profile = getProfileForOrder(order);
            lvl = ShoppingCartHandler.findInCircleBenefitLvl(profile, order);
            
            if (promotionsHelper.isValidIncircleMbr(lvl)) {
              if (promotionsHelper.isQualifiedIncircleLvl(lvl, promoCode, "pwp")) {
                isValid = true;
              }
            }
          }
        }
        
        case 4: {
          if (promotionsHelper.promoCodeMatch(order, promoCode, pwp.getDollarQualifier().trim())
                  && promotionsHelper.orderHasProdDollar(order, promoKey, pwp.getDollarQualifier().trim(), pwp.getSaleQualificationName())) {
            if (promotionsHelper.isValidIncircleMbr(lvl)) {
              if (promotionsHelper.isQualifiedIncircleLvl(lvl, promoCode, "pwp")) {
                isValid = true;
              }
            }
          }
          break;
        }
        
        default: {
          // if it is not one of the types above, never delete promo item
          isValid = true;
        }
      }
    } catch (Exception e) {
      if (isLoggingError()) logError(e.getMessage());
    }
    
    return isValid;
  }
  
  // private HashMap getOrderProductPromoKeys(Order order) {
  // HashMap theMap = new HashMap();
  // List items = order.getCommerceItems();
  // Iterator iter = items.iterator();
  // while (iter.hasNext()) {
  // CommerceItemImpl thisItemImpl = (CommerceItemImpl) iter.next();
  // RepositoryItem productItem = (RepositoryItem) thisItemImpl.getAuxiliaryData().getProductRef();
  // Set theSet = new HashSet();
  // theSet.addAll(promotionsHelper.getProductPromoArchive(order.getSubmittedDate(), productItem.getRepositoryId()));
  // theMap.put(productItem.getRepositoryId(), theSet);
  // }
  // return theMap;
  // }
  
  public RepositoryItem getProfileForOrder(Order order) throws PromoException {
    RepositoryItem profile = null;
    
    try {
      if (getNMOrderManager() != null) {
        profile = getNMOrderManager().getOrderTools().getProfileTools().getProfileItem(order.getProfileId());
      }
    } catch (RepositoryException e) {
      throw new PromoException(e);
    }
    
    return profile;
  }
  
  public PurchaseWithPurchaseArray getPurchaseWithPurchaseArray() {
    return purchaseWithPurchaseArray;
  }
  
  public void setPurchaseWithPurchaseArray(PurchaseWithPurchaseArray purchaseWithPurchaseArray) {
    this.purchaseWithPurchaseArray = purchaseWithPurchaseArray;
  }

}
