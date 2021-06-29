package com.nm.commerce.giftlist;

import java.util.List;

public class WishlistPagingModel {
  public double mVgcTotal;
  public double mTgcTotal;
  public List<GiftItem> mFilteredList;
  public int mCurrPageNumber;
  public int mPrevPageNumber;
  public int mNextPageNumber;
  public List<Integer> mPageNumberList;
  public int mLastPageNumber;
  public String mWishlistUrl;
  public int mTotalNumberOfPages;
  
  public WishlistPagingModel() {
    
  }
  
  public double getVgcTotal() {
    return mVgcTotal;
  }
  
  public void setVgcTotal(double value) {
    this.mVgcTotal = value;
  }
  
  public double getTgcTotal() {
    return mTgcTotal;
  }
  
  public void setTgcTotal(double value) {
    this.mTgcTotal = value;
  }
  
  public List<GiftItem> getFilteredList() {
    return mFilteredList;
  }
  
  public void setFilteredList(List<GiftItem> value) {
    this.mFilteredList = value;
  }
  
  public int getCurrPageNumber() {
    return mCurrPageNumber;
  }
  
  public void setCurrPageNumber(int value) {
    this.mCurrPageNumber = value;
  }
  
  public int getPrevPageNumber() {
    return mPrevPageNumber;
  }
  
  public void setPrevPageNumber(int value) {
    this.mPrevPageNumber = value;
  }
  
  public int getNextPageNumber() {
    return mNextPageNumber;
  }
  
  public void setNextPageNumber(int value) {
    this.mNextPageNumber = value;
  }
  
  public List<Integer> getPageNumberList() {
    return mPageNumberList;
  }
  
  public void setPageNumberList(List<Integer> value) {
    this.mPageNumberList = value;
  }
  
  public int getLastPageNumber() {
    return mLastPageNumber;
  }
  
  public void setLastPageNumber(int value) {
    this.mLastPageNumber = value;
  }
  
  public String getWishlistUrl() {
    return mWishlistUrl;
  }
  
  public void setWishlistUrl(String value) {
    this.mWishlistUrl = value;
  }
  
  public int getTotalNumberOfPages() {
    return mTotalNumberOfPages;
  }
  
  public void setTotalNumberOfPages(int value) {
    this.mTotalNumberOfPages = value;
  }
}
