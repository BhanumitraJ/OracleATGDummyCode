package com.nm.commerce.pagedef.model.omniture.checkout;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import atg.commerce.order.CommerceItem;
import atg.commerce.order.ShippingGroupCommerceItemRelationship;

import com.nm.commerce.NMCommerceItem;
import com.nm.commerce.checkout.beans.ResultBean;
import com.nm.commerce.pagedef.model.CheckoutPageModel;

public class OmnitureProduct {
  
  private String category;
  private String cmosItemCode;
  private String quantity;
  private String price;
  
  private boolean useSFLEvents = false;
  private boolean useWishlistEvents = false;
  private boolean useQty = false;
  private boolean usePrice = false;
  private boolean isOrderComplete = false;
  private boolean isItemAdded = false;
  
  private List<OmnitureProduct> products = new java.util.ArrayList<OmnitureProduct>();
  private Map<String, String> events = new java.util.HashMap<String, String>();
  
  public OmnitureProduct(ResultBean bean) {
    this(bean, false, false, false, false, false, false);
  }
  
  public OmnitureProduct(ResultBean bean, boolean includeSFLEvents, boolean includeWishlistEvents, boolean includeQty, boolean includePrice, boolean isOrderComplete, boolean isItemAdded) {
    this.cmosItemCode = bean.getCmosItemCode();
    this.quantity = String.valueOf(bean.getQuantity());
    this.price = String.valueOf(bean.getPrice());
    
    this.useWishlistEvents = includeWishlistEvents;
    this.useQty = includeQty;
    this.usePrice = includePrice;
    this.isOrderComplete = isOrderComplete;
    this.isItemAdded = isItemAdded;
    this.cmosItemCode = String.valueOf(bean.getCmosItemCode());
  }
  
  
  public OmnitureProduct(NMCommerceItem item, OmnitureProduct omProduct) {
    this(item, omProduct.useSFLEvents, omProduct.useWishlistEvents, omProduct.useQty, omProduct.usePrice, omProduct.isOrderComplete, omProduct.isItemAdded);
  }
  
  public OmnitureProduct(NMCommerceItem item, boolean includeSFLEvents, boolean includeWishlistEvents, boolean includeQty, boolean includePrice, boolean isOrderComplete, boolean isItemAdded) {
    this.cmosItemCode = item.getProduct().getCmosItemCode();
    this.quantity = String.valueOf(item.getQuantity());
    this.price = String.valueOf(item.getPriceInfo().getAmount());
    this.useWishlistEvents = includeWishlistEvents;
    this.useQty = includeQty;
    this.usePrice = includePrice;
    this.isOrderComplete = isOrderComplete;
    this.isItemAdded = isItemAdded;
    
    if (this.useWishlistEvents) {
      if (item.hasMiscFlag(NMCommerceItem.MISC_FLAG_SBR)) {
        this.events.put("event19", this.quantity);
      }
    }
    
    if (this.useSFLEvents) {
      
    }
  }
  
  protected String getProductsString() {
    StringBuffer sb = new StringBuffer();
    if (!products.isEmpty()) {
      for (int i = 0; i < products.size(); i++) {
        if (i != 0) {
          sb.append(",");
        }
        sb.append(products.get(i).getString());
      }
    } else {
      sb.append(getString());
    }
    return sb.toString();
  }
  
  protected String getCmosItemString() {
    StringBuffer sb = new StringBuffer();
    if (!products.isEmpty()) {
      for (int i = 0; i < products.size(); i++) {
        if (i != 0) {
          sb.append(",");
        }
        sb.append(products.get(i).getString());
      }
    } else {
      sb.append(getString());
    }
    return sb.toString();
  }
  
  public void addEvent(String eventName, String eventValue) {
    events.put(eventName, eventValue);
  }
  
  public String getEventString() {
    StringBuffer eventString = new StringBuffer();
    
    Set<String> keys = events.keySet();
    if (!keys.isEmpty()) {
      Iterator<String> i = keys.iterator();
      int cnt = 1;
      while (i.hasNext()) {
        if (cnt != 1) {
          eventString.append("|");
        }
        String key = i.next();
        eventString.append(key);
        eventString.append("=");
        eventString.append(events.get(key));
      }
    }
    return eventString.toString();
  }
  
  public String getString() {
    
    StringBuffer sb = new StringBuffer();
    
    if (category != null) {
      sb.append(category);
    }
    sb.append(';');
    if (cmosItemCode != null) {
      sb.append(cmosItemCode);
    }
    sb.append(';');
    if (quantity != null && isUseQty()) {
      sb.append(quantity);
    }
    sb.append(';');
    if (price != null && isUsePrice()) {
      sb.append(price);
    }
    sb.append(';');
    sb.append(getEventString());
    
    return sb.toString();
  }
  
  public String getCategory() {
    return category;
  }
  
  public String getCmosItemCode() {
    return cmosItemCode;
  }
  
  public String getQuantity() {
    return quantity;
  }
  
  public String getPrice() {
    return price;
  }
  
  public void setCategory(String category) {
    this.category = category;
  }
  
  public void setCmosItemCode(String cmosItemCode) {
    this.cmosItemCode = cmosItemCode;
  }
  
  public void setQuantity(String quantity) {
    this.quantity = quantity;
  }
  
  public void setPrice(String price) {
    this.price = price;
  }
  
  public boolean isUseSFLEvents() {
    return useSFLEvents;
  }
  
  public void setUseSFLEvents(boolean useSFLEvents) {
    this.useSFLEvents = useSFLEvents;
  }
  
  public boolean isUseWishlistEvents() {
    return useWishlistEvents;
  }
  
  public void setUseWishlistEvents(boolean useWishlistEvents) {
    this.useWishlistEvents = useWishlistEvents;
  }
  
  public boolean isUseQty() {
    return useQty;
  }
  
  public void setUseQty(boolean useQty) {
    this.useQty = useQty;
  }
  
  public boolean isOrderComplete() {
    return isOrderComplete;
  }
  
  public void setOrderComplete(boolean isOrderComplete) {
    this.isOrderComplete = isOrderComplete;
  }
  
  public boolean isItemAdded() {
    return isItemAdded;
  }
  
  public void setItemAdded(boolean isItemAdded) {
    this.isItemAdded = isItemAdded;
  }
  
  public boolean isUsePrice() {
    return usePrice;
  }
  
  public void setUsePrice(boolean usePrice) {
    this.usePrice = usePrice;
  }
  
}
