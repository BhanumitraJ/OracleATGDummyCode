package com.nm.commerce.promotion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import com.nm.cmos.data.CmosOrder;
import com.nm.cmos.data.LineItem;
import com.nm.cmos.data.ShipTo;
import com.nm.collections.PercentOffPromotion;
import com.nm.commerce.NMCommerceItem;
import com.nm.commerce.order.NMOrderManager;
import com.nm.utils.GenericLogger;
import com.nm.utils.PromotionsHelper;

import atg.commerce.CommerceException;
import atg.commerce.pricing.PricingConstants;
import atg.commerce.pricing.PricingException;
import atg.commerce.pricing.PricingTools;
import atg.nucleus.GenericService;

public class CancelPromotion extends GenericService {
  private NMOrderManager nmOrderManager = (NMOrderManager) NMOrderManager.getOrderManager();
  protected PromotionsHelper promotionsHelper;
  private PricingTools pricingTools;
  private GenericLogger genericLogger;
  private Vector<String> validActiveStatusCodes = new Vector<String>();
  private Vector<String> validCancelStatusCodes = new Vector<String>();
  
  public CancelPromotion() {
    setValidActiveStatusCodes();
    setValidCancelStatusCodes();
  }
  
  protected NMOrderImpl getCompleteOrder(String orderId) {
    NMOrderImpl completeOrder = null;
    try {
      completeOrder = (NMOrderImpl) nmOrderManager.loadOrder(orderId);
    } catch (CommerceException e) {
      genericLogger.error(e.getMessage());
    }
    return completeOrder;
  }
  
  protected String getWebOrderId(String external_order_number) {
    if (external_order_number == null || external_order_number.length() < 3) return null;
    String orderId = "";
    try {
      orderId = external_order_number.substring(2, external_order_number.length());
    } catch (IndexOutOfBoundsException e) {
      genericLogger.error(e.getMessage());
    }
    return orderId;
  }
  
  protected NMOrderImpl repriceOrder(NMOrderImpl completeOrder) {
    genericLogger.debug("before performPricingOperation) " + completeOrder.getPriceInfo().getAmount());
    try {
      getPricingTools().performPricingOperation(PricingConstants.OP_REPRICE_ORDER, completeOrder, null, null, null, null);
    } catch (PricingException e1) {
      e1.printStackTrace();
    }
    genericLogger.debug("after performPricingOperation)  " + completeOrder.getPriceInfo().getAmount());
    
    return completeOrder;
  }
  
  protected NMOrderImpl removeCancelledLines(NMOrderImpl completeOrder, CmosOrder cmosOrder, List<String> cancelledLineItems) {
    final ShipTo[] shipto = cmosOrder.order_detail.ship_to_customer;
    for (int stPos = 0; stPos < shipto.length; stPos++) {
      LineItem[] lineitems = (LineItem[]) shipto[stPos].line_item;
      for (int liPos = 0; liPos < lineitems.length; liPos++) {
        LineItem lineitem = (LineItem) lineitems[liPos];
        if (cancelledLineItems.contains(lineitem.cmos_line_item_id) || getValidCancelStatusCodes().contains(lineitem.current_status)) {
          String tempCi = lineitem.external_line_item_id;
          try {
            getNMOrderManager().getCommerceItemManager().removeItemFromOrder(completeOrder, tempCi);
          } catch (CommerceException e) {
            e.printStackTrace();
          }
        }
      }
    }
    return completeOrder;
  }
  
  protected HashMap<String, String> getCmosLineToPromoKeys(ShipTo[] shiptos, HashMap<String, String> hmCIToPromoKey) {
    HashMap<String, String> hmCmosLineToPromoKeys = new HashMap<String, String>();
    for (int st = 0; st < shiptos.length; st++) {
      LineItem[] lineitem = shiptos[st].line_item;
      for (int li = 0; li < lineitem.length; li++) {
        // cmos sends the keys in the promotion_code field
        // and they are not always there so do not use
        // ShipTo info for promo keys
        String promoKeys = (String) hmCIToPromoKey.get(lineitem[li].external_line_item_id);
        if (promoKeys == null || promoKeys.equals("")) continue;
        String cmosLineItem = lineitem[li].cmos_line_item_id;
        if (promoKeys != null && cmosLineItem != null) {
          promoKeys = promoKeys.replaceAll("_", "");
          if (getValidActiveStatusCodes().contains(lineitem[li].current_status) && !promoKeys.equals("")) {
            hmCmosLineToPromoKeys.put(cmosLineItem, promoKeys);
          }
        }
      }
    }
    return hmCmosLineToPromoKeys;
  }
  
  protected ArrayList<String> getOrderPromoKeys(NMOrderImpl completeOrder) {
    ArrayList<String> alPromoKeys = new ArrayList<String>();
    List<NMCommerceItem> items = completeOrder.getNmCommerceItems();
    Iterator<NMCommerceItem> iter = items.iterator();
    while (iter.hasNext()) {
      NMCommerceItem thisItem = (NMCommerceItem) iter.next();
      List<String> promoKeys = thisItem.getSendCmosPromoKeyList();
      for (int pos = 0; pos < promoKeys.size(); pos++) {
        String promoKey = ((String) promoKeys.get(pos)).trim();
        if (!promoKey.equals("") && !alPromoKeys.contains(promoKey)) {
          alPromoKeys.add(promoKey);
        }
      }
    }
    return alPromoKeys;
  }
  
  protected HashMap<String, String> getCIToPromoKey(NMOrderImpl completeOrder) {
    HashMap<String, String> hmCIToPromoKey = new HashMap<String, String>();
    List<NMCommerceItem> items = completeOrder.getNmCommerceItems();
    Iterator<NMCommerceItem> iter = items.iterator();
    while (iter.hasNext()) {
      NMCommerceItem thisItem = (NMCommerceItem) iter.next();
      hmCIToPromoKey.put(thisItem.getId(), thisItem.getSendCmosPromoKey());
    }
    return hmCIToPromoKey;
  }

  /** 
   * Function looks at each line item, and if it is the "buy one" or 
   * "get one" of a BOGO, it adjusts it's amount to
   * the prorated amount so that it won't be auto-cancelled by the GWP and
   * PWP 'auto-cancel free item' logic.
   * @param completeOrder
   * @param cmosOrder
   */
  protected void adjustCIsForBogo(NMOrderImpl completeOrder, CmosOrder cmosOrder) {
    final ArrayList<String> bogoPromoKeys = new ArrayList<String>();
    final ArrayList<String> allPromoKeys = getOrderPromoKeys(completeOrder);
    Iterator<String> promoKeyIter = allPromoKeys.iterator();
    String promoKey = null;
    PercentOffPromotion percentOffPromotion = null;

    // Are there BOGOs on the order
    try {
      while (promoKeyIter.hasNext()) {
        promoKey = promoKeyIter.next();
        percentOffPromotion = promotionsHelper.getPercentOffPromotionViaKey(promoKey);

        if (percentOffPromotion != null && percentOffPromotion.isBogo() && !bogoPromoKeys.contains(promoKey))
        {
          bogoPromoKeys.add(promoKey);
          bogoPromoKeys.add(percentOffPromotion.getBuyOneGetOneQualifiedPromotion());
        }
      }
    } catch (Exception e) { }

    if (bogoPromoKeys.size() == 0) {
      return;
    }

    final List<NMCommerceItem> items = completeOrder.getNmCommerceItems();
    Iterator<NMCommerceItem> iter = items.iterator();
    final ShipTo[] shipto = cmosOrder.order_detail.ship_to_customer;
    
    while (iter.hasNext()) {
      NMCommerceItem thisItem = (NMCommerceItem) iter.next();
      List<String> promoKeys = thisItem.getSendCmosPromoKeyList();

      for (int pos = 0; pos < promoKeys.size(); pos++) {
        promoKey = ((String) promoKeys.get(pos)).trim();

        if (bogoPromoKeys.contains(promoKey)) {
          // this item is part of a BOGO and it's price was
          // pro-rated on the order.xml to CMOS; we must
          // readjust it to the cmosOrder line item priceEach
          
          for (int stPos = 0; stPos < shipto.length; stPos++) {
            LineItem[] lineitems = (LineItem[]) shipto[stPos].line_item;

            for (int liPos = 0; liPos < lineitems.length; liPos++) {
              LineItem lineitem = (LineItem) lineitems[liPos];

              if (lineitem.external_line_item_id.equals(thisItem.getId())) {
                double price = new Double(lineitem.price_each).doubleValue();
                thisItem.getPriceInfo().setAmount(price);
                break;
              }
            }
          }
          break;
        }
      }
    }
  }

  public PromotionsHelper getPromotionsHelper() {
    return promotionsHelper;
  }
  
  public void setPromotionsHelper(PromotionsHelper promotionsHelper) {
    this.promotionsHelper = promotionsHelper;
  }

  public PricingTools getPricingTools() {
    return pricingTools;
  }
  
  public void setPricingTools(PricingTools pricingTools) {
    this.pricingTools = pricingTools;
  }
  
  public GenericLogger getGenericLogger() {
    return genericLogger;
  }
  
  public void setGenericLogger(GenericLogger genericLogger) {
    this.genericLogger = genericLogger;
  }
  
  public NMOrderManager getNMOrderManager() {
    return this.nmOrderManager;
  }
  
  public Vector<String> getValidActiveStatusCodes() {
    return validActiveStatusCodes;
  }
  
  public void setValidActiveStatusCodes() {
    validActiveStatusCodes.clear();
    validActiveStatusCodes.add("RP");
    validActiveStatusCodes.add("MI");
    validActiveStatusCodes.add("RD");
    validActiveStatusCodes.add("BO");
    validActiveStatusCodes.add("QU");
    validActiveStatusCodes.add("RI");
    validActiveStatusCodes.add("FT");
  }
  
  public Vector<String> getValidCancelStatusCodes() {
    return validCancelStatusCodes;
  }
  
  public void setValidCancelStatusCodes() {
    validCancelStatusCodes.clear();
    validCancelStatusCodes.add("CX");
    validCancelStatusCodes.add("PX");
  }
}
