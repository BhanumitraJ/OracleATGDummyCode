package com.nm.commerce.promotion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.transaction.TransactionManager;

import atg.commerce.order.Order;
import atg.dtm.TransactionDemarcation;
import atg.dtm.TransactionDemarcationException;

import com.nm.cmos.data.CmosOrder;
import com.nm.collections.GiftWithPurchaseSelect;
import com.nm.collections.GiftWithPurchaseSelectArray;
import com.nm.collections.NMPromotion;
import com.nm.components.CommonComponentHelper;

public class CancelGwpSelect extends CancelPromotion {
  private GiftWithPurchaseSelectArray giftWithPurchaseSelectArray;
  
  public CancelGwpSelect() {
    super();
  }
  
  public List<String> getGwpSelectsToBeCancelled(CmosOrder cmosOrder, List<String> cancelledLineItems) {
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
      
      // get the complete order
      NMOrderImpl completeOrder = getCompleteOrder(orderId);
      if (completeOrder == null) return null;
      
      synchronized (completeOrder) {
        adjustCIsForBogo(completeOrder, cmosOrder);

        // Need To Remove Line(s) Being Cancelled
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
        alArchivePromos.addAll(getGiftWithPurchaseSelectArray().getAuditPromotions(completeOrder.getSubmittedDate()));
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
  
  private ArrayList<String> getCmosOrderLinesToCancel(NMOrderImpl order, ArrayList<String> orderPromoKeys, ArrayList<NMPromotion> promoArray, HashMap<String, String> hmCmosLineToPromoKeys) {
    ArrayList<String> alCmosOrderLinesToCancel = new ArrayList<String>();
    for (int i = 0; i < promoArray.size(); i++) {
      GiftWithPurchaseSelect gwp = (GiftWithPurchaseSelect) promoArray.get(i);
      if (orderPromoKeys.contains(gwp.getCode())) {
        // always ignore online cat only flag
        // the active online catalog may have changed
        gwp.setOnlineCatOnly(new Boolean(false));
        if (!isValidatePromo(gwp, order)) {
          getGenericLogger().debug("gwpSelectAudit code = " + gwp.getCode());
          Iterator<String> iter = hmCmosLineToPromoKeys.keySet().iterator();
          while (iter.hasNext()) {
            String key = (String) iter.next();
            getGenericLogger().debug("Processing: " + key + "" + hmCmosLineToPromoKeys.get(key));
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
  
  private boolean isValidatePromo(GiftWithPurchaseSelect gwpSelect, Order order) {
    boolean isValid = false;
    final boolean onlineCatOnly = false;
    final String department = gwpSelect.getDeptCodes().trim().toUpperCase();
    final String classCode = gwpSelect.getClassCodes().trim().toUpperCase();
    final String depiction = gwpSelect.getDepiction().trim().toUpperCase();
    final String designer = gwpSelect.getVendor().trim().toUpperCase();
    
    double doubleDollar = 0.0;
    
    String dollarQualifier = gwpSelect.getDollarQualifier();
    
    if (dollarQualifier != null && dollarQualifier.length() > 0) {
      Double tempDollar = Double.valueOf(gwpSelect.getDollarQualifier());
      doubleDollar = tempDollar.doubleValue();
    }
    
    int type = 0;
    try {
      type = Integer.parseInt(gwpSelect.getType().trim());
    } catch (NumberFormatException e) {
      // defaulted to 0
    }
    
    try {
      switch (type) {
        case 1: {
          if (promotionsHelper.deptClassDollarMatch(order, department, classCode, doubleDollar, onlineCatOnly)) {
            isValid = true;
          }
          break;
        }
        case 2: {
          if (promotionsHelper.depictionDollarMatch(order, depiction, doubleDollar)) {
            isValid = true;
          }
          break;
        }
        case 3: {
          if (promotionsHelper.vendorDollarMatch(order, designer, doubleDollar)) {
            isValid = true;
          }
          break;
        }
        case 4: {
          double customersTotal = 0;
          double deptClassDollar = promotionsHelper.vendorDollarDeptClassMatch(order, designer, department, classCode, onlineCatOnly, doubleDollar);
          customersTotal = customersTotal + deptClassDollar;
          
          if (customersTotal >= doubleDollar) {
            isValid = true;
          }
          break;
        }
      }
    } catch (Exception e) {
      if (isLoggingError()) logError(e.getMessage());
    }
    
    return isValid;
  }
  
  public GiftWithPurchaseSelectArray getGiftWithPurchaseSelectArray() {
    return giftWithPurchaseSelectArray;
  }
  
  public void setGiftWithPurchaseSelectArray(GiftWithPurchaseSelectArray giftWithPurchaseSelectArray) {
    this.giftWithPurchaseSelectArray = giftWithPurchaseSelectArray;
  }

}
