package com.nm.commerce.giftlist;

import java.util.*;

// This class is a session based object that holds giftlist items
// Its benefits include easy retrieval of gift items on the front end and
// performance gains from not having to sort each time a gift lists items
// are queried.
public class WishlistProductHolder extends GiftlistProductHolder {
  
  private List addToWishlistTempList = new ArrayList(); // list of WishlistItems that will be stored during the process of the user logging in and adding items to the wishlist
  
  public List getAddToWishlistTempList() {
    return addToWishlistTempList;
  }
  
  public void setAddToWishlistTempList(List addToWishlistTempList) {
    this.addToWishlistTempList = addToWishlistTempList;
  }
  
  // checks the site production system code and sorts using that sites sort conditions
  // protected void sortProductList(List<GiftItem> list) {
  // if( isValidSortAttempt() ) {
  // if( isLoggingDebug() ) {
  // logDebug("sort is valid, sorting by-->" + getSortBy() + "<---");
  // }
  // String productSystemCode = getSystemSpecs().getProductionSystemCode();
  // if( productSystemCode != null ) {
  // if( productSystemCode.equalsIgnoreCase("WC") ) {
  // if( getSortBy().equalsIgnoreCase("0") ) {
  // Collections.sort(list,GiftItem.VCAT_COMPARATOR);
  // } else if ( getSortBy().equalsIgnoreCase("1") ) {
  // Collections.sort(list,GiftItem.SALE_PRICE_DESC_COMPARATOR);
  // } else if ( getSortBy().equalsIgnoreCase("2") ) {
  // Collections.sort(list,GiftItem.STILL_NEED_DESC_COMPARATOR);
  // } else if ( getSortBy().equalsIgnoreCase("3") ) {
  // Collections.sort(list,GiftItem.BRAND_COMPARATOR);
  // } else {
  // Collections.sort(getProductList(),GiftItem.SALE_PRICE_DESC_COMPARATOR);
  // }
  // } else if( productSystemCode.equalsIgnoreCase("WH") ) {
  // if( getSortBy().equalsIgnoreCase("0") ) {
  // Collections.sort(list,GiftItem.VCAT_COMPARATOR);
  // } else if ( getSortBy().equalsIgnoreCase("1") ) {
  // Collections.sort(list,GiftItem.SALE_PRICE_DESC_COMPARATOR);
  // } else if ( getSortBy().equalsIgnoreCase("2") ) {
  // Collections.sort(list,GiftItem.STILL_NEED_DESC_COMPARATOR);
  // } else if ( getSortBy().equalsIgnoreCase("3") ) {
  // Collections.sort(list,GiftItem.BRAND_COMPARATOR);
  // } else {
  // Collections.sort(list,GiftItem.SALE_PRICE_DESC_COMPARATOR);
  // }
  // } else if( productSystemCode.equalsIgnoreCase("WN") ) {
  // if( getSortBy().equalsIgnoreCase("0") ) {
  // Collections.sort(list,GiftItem.VCAT_COMPARATOR);
  // } else if ( getSortBy().equalsIgnoreCase("1") ) {
  // Collections.sort(list,GiftItem.SALE_PRICE_DESC_COMPARATOR);
  // } else if ( getSortBy().equalsIgnoreCase("2") ) {
  // Collections.sort(list,GiftItem.STILL_NEED_DESC_COMPARATOR);
  // } else if ( getSortBy().equalsIgnoreCase("3") ) {
  // Collections.sort(list,GiftItem.BRAND_COMPARATOR);
  // } else {
  // Collections.sort(list,GiftItem.SALE_PRICE_DESC_COMPARATOR);
  // }
  // } else if( productSystemCode.equalsIgnoreCase("WB") ) {
  // if( getSortBy().equalsIgnoreCase("0") ) {
  // Collections.sort(list,GiftItem.VCAT_COMPARATOR);
  // } else if ( getSortBy().equalsIgnoreCase("1") ) {
  // Collections.sort(list,GiftItem.SALE_PRICE_DESC_COMPARATOR);
  // } else if ( getSortBy().equalsIgnoreCase("2") ) {
  // Collections.sort(list,GiftItem.STILL_NEED_DESC_COMPARATOR);
  // } else if ( getSortBy().equalsIgnoreCase("3") ) {
  // Collections.sort(list,GiftItem.BRAND_COMPARATOR);
  // } else {
  // Collections.sort(list,GiftItem.SALE_PRICE_DESC_COMPARATOR);
  // }
  // }
  // }
  // }
  // }
}
