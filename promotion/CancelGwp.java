package com.nm.commerce.promotion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.transaction.TransactionManager;

import atg.commerce.order.CommerceItemImpl;
import atg.commerce.order.Order;
import atg.dtm.TransactionDemarcation;
import atg.dtm.TransactionDemarcationException;
import atg.repository.RepositoryItem;

import com.nm.cmos.data.CmosOrder;
import com.nm.collections.GiftWithPurchase;
import com.nm.collections.GiftWithPurchaseArray;
import com.nm.collections.NMPromotion;
import com.nm.components.CommonComponentHelper;

public class CancelGwp extends CancelPromotion {
  private GiftWithPurchaseArray giftWithPurchaseArray;
  
  public CancelGwp() {
    super();
  }
  
  public List<String> getGwpsToBeCancelled(CmosOrder cmosOrder, List<String> cancelledLineItems) {
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
        alArchivePromos.addAll(getGiftWithPurchaseArray().getAuditPromotions(completeOrder.getSubmittedDate()));
        if (alArchivePromos == null || alArchivePromos.isEmpty()) {
          return null;
        }
        
        // validate and add cmos order line id to list to be cancelled
        aList = getCmosOrderLinesToCancel(completeOrder, orderPromoKeys, alArchivePromos, hmCmosLineToPromoKeys);
      }
    } catch (Exception e) {
      logError("CheckCSRGWP.executeAction(): Exception " + e.getMessage());
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
    GiftWithPurchase gwp;
    for (int i = 0; i < promoArray.size(); i++) {
      gwp = (GiftWithPurchase) promoArray.get(i);
      if (orderPromoKeys.contains(gwp.getCode())) {
        // always ignore online cat only flag
        // the active online catalog may have changed
        gwp.setOnlineCatOnly(Boolean.toString(false));
        if (!isValidatePromo(gwp, order)) {
          Iterator<String> iter = hmCmosLineToPromoKeys.keySet().iterator();
          while (iter.hasNext()) {
            String key = (String) iter.next();
            String[] strings = ((String) hmCmosLineToPromoKeys.get(key)).split(",");
            for (int pos = 0; pos < strings.length; pos++) {
              if (strings[pos].trim().equalsIgnoreCase(gwp.getCode())) {
                alCmosOrderLinesToCancel.add(key);
              }
            }
          }
        }
      }
    }
    return alCmosOrderLinesToCancel;
  }
  
  private boolean isValidatePromo(GiftWithPurchase gwp, Order order) {
    boolean isValid = false;
    boolean onlineCatOnly = false;
    final String promoCode = gwp.getPromoCodes().trim().toUpperCase();
    final String department = gwp.getDeptCodes().trim().toUpperCase();
    final String classCode = gwp.getClassCodes().trim().toUpperCase();
    final String depiction = gwp.getDepiction().trim().toUpperCase();
    final String catalog = gwp.getQualifyCatalog().trim().toUpperCase();
    final String onlineCatTest = gwp.getOnlineCatOnly().trim().toUpperCase();
    final String designer = gwp.getVendor().trim().toUpperCase();
    final String items = gwp.getQualifyItems().trim().toUpperCase();
    final String promoKey = gwp.getCode().trim().toUpperCase();
    
    double doubleDollar = 0.0;
    
    String dollarQualifier = gwp.getDollarQualifier();
    
    if (dollarQualifier != null && dollarQualifier.length() > 0) {
      Double tempDollar = Double.valueOf(gwp.getDollarQualifier());
      doubleDollar = tempDollar.doubleValue();
    }
    
    onlineCatOnly = onlineCatTest.equals("TRUE");
    int type = 0;
    try {
      type = Integer.parseInt(gwp.getType().trim());
    } catch (NumberFormatException e) {
      // defaulted to 0
    }
    
    try {
      switch (type) {
        case 1: {
          if (promotionsHelper.dollarQualifies(order, gwp.getDollarQualifier())) {
            isValid = true;
          }
          break;
        }
        
        case 3: {
          if (promotionsHelper.promoCodeMatch(order, promoCode) && promotionsHelper.dollarQualifies(order, gwp.getDollarQualifier())) {
            isValid = true;
          }
          break;
        }
        
        case 6: {
          if (promotionsHelper.deptClassDollarMatch(order, department, classCode, doubleDollar, onlineCatOnly)) {
            isValid = true;
          }
          break;
        }
        
        case 7: {
          if (promotionsHelper.promoCodeMatch(order, promoCode) && promotionsHelper.deptClassDollarMatch(order, department, classCode, doubleDollar, onlineCatOnly)) {
            isValid = true;
          }
          break;
        }
        
        case 10: {
          if (promotionsHelper.depictionDollarMatch(order, depiction, doubleDollar)) {
            isValid = true;
          }
          break;
        }
        
        case 11: {
          if (promotionsHelper.promoCodeMatch(order, promoCode) && promotionsHelper.depictionDollarMatch(order, depiction, doubleDollar)) {
            isValid = true;
          }
          break;
        }
        
        case 12: {
          if (promotionsHelper.itemDollarMatch(order, catalog, items, doubleDollar)) {
            isValid = true;
          }
          break;
        }
        
        case 13: {
          if (promotionsHelper.promoCodeMatch(order, promoCode) && promotionsHelper.itemDollarMatch(order, catalog, items, doubleDollar)) {
            isValid = true;
          }
          break;
        }
        
        case 15: {
          if (promotionsHelper.catalogDollarMatch(order, catalog, doubleDollar)) {
            isValid = true;
          }
          break;
        }
        
        case 17: {
          if (promotionsHelper.promoCodeMatch(order, promoCode) && promotionsHelper.catalogDollarMatch(order, catalog, doubleDollar)) {
            isValid = true;
          }
          break;
        }
        
        case 19: {
          if (promotionsHelper.vendorDollarMatch(order, designer, doubleDollar)) {
            isValid = true;
          }
          break;
        }
        
        case 21: {
          if (promotionsHelper.promoCodeMatch(order, promoCode) && promotionsHelper.vendorDollarMatch(order, designer, doubleDollar)) {
            isValid = true;
          }
          break;
        }
        
        case 23: {
          double customersTotal = 0;
          double deptClassDollar = promotionsHelper.vendorDollarDeptClassMatch(order, designer, department, classCode, onlineCatOnly, doubleDollar);
          customersTotal = customersTotal + deptClassDollar;
          
          if (customersTotal >= doubleDollar) {
            isValid = true;
          }
          break;
        }
        
        case 24: {
          double deptClassDollar = promotionsHelper.vendorDollarDeptClassMatch(order, designer, department, classCode, onlineCatOnly, doubleDollar);
          double customersTotal = deptClassDollar;
          
          if (promotionsHelper.promoCodeMatch(order, promoCode) && (customersTotal >= doubleDollar)) {
            isValid = true;
          }
          break;
        }
        
        case 26: {
          // ignore categories when cancelling promos, the categories may
          // have changed
          if (promotionsHelper.dollarQualifies(order, gwp.getDollarQualifier())) {
            isValid = true;
          }
          break;
        }
        
        case 28: {
          // ignore categories when cancelling promos, the categories may
          // have changed
          if (promotionsHelper.promoCodeMatch(order, promoCode) && promotionsHelper.dollarQualifies(order, gwp.getDollarQualifier())) {
            isValid = true;
          }
          break;
        }
        
        case 31: {
          if (promotionsHelper.multiDeptClassDollarMatch(order, department, doubleDollar, onlineCatOnly)) {
            isValid = true;
          }
          break;
        }
        
        case 32: {
          if (promotionsHelper.promoCodeMatch(order, promoCode) && promotionsHelper.multiDeptClassDollarMatch(order, department, doubleDollar, onlineCatOnly)) {
            isValid = true;
          }
          break;
        }
        
        case 35: {
          if (promotionsHelper.multiDeptClassVendorDollarMatch(order, department, designer, doubleDollar, onlineCatOnly)) {
            isValid = true;
          }
          break;
        }
        
        case 36: {
          if (promotionsHelper.promoCodeMatch(order, promoCode) && promotionsHelper.multiDeptClassVendorDollarMatch(order, department, designer, doubleDollar, onlineCatOnly)) {
            isValid = true;
          }
          break;
        }
        
        case 38: {
          if (promotionsHelper.orderHasProdDollar(order, promoKey, gwp.getDollarQualifier().trim(), false, getOrderProductPromoKeys(order))) {
            isValid = true;
          }
          break;
        }
        
        case 39: {
          if (promotionsHelper.promoCodeMatch(order, promoCode) && promotionsHelper.orderHasProdDollar(order, promoKey, gwp.getDollarQualifier().trim(), false, getOrderProductPromoKeys(order))) {
            isValid = true;
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
  
  private HashMap<String, Set<String>> getOrderProductPromoKeys(Order order) {
    HashMap<String, Set<String>> theMap = new HashMap<String, Set<String>>();
    @SuppressWarnings("unchecked")
    List<CommerceItemImpl> items = order.getCommerceItems();
    Iterator<CommerceItemImpl> iter = items.iterator();
    while (iter.hasNext()) {
      CommerceItemImpl thisItemImpl = (CommerceItemImpl) iter.next();
      RepositoryItem productItem = (RepositoryItem) thisItemImpl.getAuxiliaryData().getProductRef();
      Set<String> theSet = new HashSet<String>();
      theSet.addAll(promotionsHelper.getProductPromoArchive(order.getSubmittedDate(), productItem.getRepositoryId()));
      theMap.put(productItem.getRepositoryId(), theSet);
    }
    return theMap;
  }
  
  public GiftWithPurchaseArray getGiftWithPurchaseArray() {
    return giftWithPurchaseArray;
  }
  
  public void setGiftWithPurchaseArray(GiftWithPurchaseArray giftWithPurchaseArray) {
    this.giftWithPurchaseArray = giftWithPurchaseArray;
  }

}
