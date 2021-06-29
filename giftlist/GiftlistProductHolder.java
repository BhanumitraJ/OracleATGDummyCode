package com.nm.commerce.giftlist;

import atg.nucleus.GenericService;
import java.util.*;
import atg.repository.*;
import atg.commerce.gifts.*;
import atg.commerce.*;

// This class is a session based object that holds giftlist items
// Its benefits include easy retrieval of gift items on the front end and
// performance gains from not having to sort each time a gift lists items
// are queried.
public class GiftlistProductHolder extends GenericService {
  // a value indicating the current sort the product list is using
  // private String sortBy;
  private String mGiftlistId;
  private GiftlistManager mGiftlistManager;
  
  // list of GiftItems that will be stored during the process of the user logging
  // in or creating an account
  private List<RepositoryItem> tempProductList = new ArrayList<RepositoryItem>();
  
  public List<RepositoryItem> getTempProductList() {
    return tempProductList;
  }
  
  public void setTempProductList(List<RepositoryItem> tempProductList) {
    this.tempProductList = tempProductList;
  }
  
  public void setGiftlistManager(GiftlistManager value) {
    mGiftlistManager = value;
  }
  
  public GiftlistManager getGiftlistManager() {
    return mGiftlistManager;
  }
  
  // Every time setGiftlistId is called a refresh is attempted but the list
  // will only be refreshed if it has become stale.
  public void setGiftlistId(String value) {
    mGiftlistId = value;
    
    if (mGiftlistId != null) {
      addTempItemsToGiftlist();
    }
  }
  
  public String getGiftlistId() {
    return mGiftlistId;
  }
  
  public List<GiftItem> getProductList() {
    List<GiftItem> giftItemList = null;
    
    try {
      RepositoryItem giftlist = getGiftlistManager().getGiftlist(getGiftlistId());
      GiftlistTools giftlistTools = getGiftlistManager().getGiftlistTools();
      @SuppressWarnings("unchecked")
      List<MutableRepositoryItem> repositoryItems = (List<MutableRepositoryItem>) giftlistTools.getGiftlistItems(giftlist);
      if (repositoryItems != null) {
        ListIterator<MutableRepositoryItem> iterator = (ListIterator<MutableRepositoryItem>) repositoryItems.listIterator(repositoryItems.size());
        giftItemList = new ArrayList<GiftItem>();
        while (iterator.hasPrevious()) {
          MutableRepositoryItem repositoryItem = iterator.previous();
          GiftItem giftItem = new GiftItem(repositoryItem);
          giftItemList.add(giftItem);
        }
      }
    } catch (CommerceException exception) {
      this.logError(exception);
      giftItemList = new ArrayList<GiftItem>();
    }
    
    return giftItemList;
  }
  
  public void removeItem(GiftItem giftItem) throws CommerceException, RepositoryException {
    String giftItemId = giftItem.getId();
    getGiftlistManager().removeItemFromGiftlist(getGiftlistId(), giftItemId);
  }
  
  public void addTempItemsToGiftlist() {
    try {
      if (!getTempProductList().isEmpty()) {
        Iterator<RepositoryItem> tempListIterator = getTempProductList().iterator();
        RepositoryItem repositoryItem;
        while (tempListIterator.hasNext()) {
          repositoryItem = (RepositoryItem) tempListIterator.next();
          ((NMGiftlistManager) getGiftlistManager()).addItemToGiftlistCombine(getGiftlistId(), repositoryItem.getRepositoryId());
        }
        // setStaleList(true);
        getTempProductList().clear();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
