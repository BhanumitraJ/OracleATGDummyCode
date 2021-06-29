package com.nm.commerce.pagedef.model;

import java.util.List;

import com.nm.bops.BopsItem;
import com.nm.commerce.catalog.NMSuite;
import com.nm.repository.stores.Store;

public class PrintBopsPageModel extends PageModel {
  private Store store;
  private List<BopsItem> itemList;
  private NMSuite suite;
  private boolean storeBopsEligible;
  
  public Store getStore() {
    return store;
  }
  
  public void setStore(Store store) {
    this.store = store;
  }
  
  public List<BopsItem> getItemList() {
    return itemList;
  }
  
  public void setItemList(List<BopsItem> itemList) {
    this.itemList = itemList;
  }
  
  public NMSuite getSuite() {
    return suite;
  }
  
  public void setSuite(NMSuite suite) {
    this.suite = suite;
  }
  
  public boolean getStoreBopsEligible() {
    return storeBopsEligible;
  }
  
  public void setStoreBopsEligible(boolean storeBopsEligible) {
    this.storeBopsEligible = storeBopsEligible;
  }
  
}
