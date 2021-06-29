package com.nm.commerce.pagedef.model;

import java.util.List;
import java.util.Vector;

import com.nm.commerce.NMCommerceItemTempHolder;
import com.nm.commerce.NMProfile;
import com.nm.commerce.giftlist.GiftItem;
import com.nm.commerce.giftlist.WishlistPagingModel;
import com.nm.formhandler.RegistryAccountFormHandler;
import com.nm.formhandler.RegistryProductFormHandler;

public class WishlistPageModel extends PageModel {
  private List<NMCommerceItemTempHolder> mTempProductList;
  private boolean mIsFirstProductOnCart = false;
  // private List<GiftItem> mFilteredList;
  private RegistryProductFormHandler mProductFormHandler;
  private RegistryAccountFormHandler mAccountFormHandler;
  private NMProfile mProfile;
  private String mGiftCardCategory;
  private String mGiftCategory;
  private boolean onlyPinYin;
  
  /**
   * Get the category for gift cards (initially used on the no results display on find wishlist)
   * 
   * @return
   */
  public String getGiftCardCategory() {
    return mGiftCardCategory;
  }
  
  /**
   * Set the category for gift cards (initially used on the no results display on find wishlist)
   * 
   * @param giftCardCategory
   */
  public void setGiftCardCategory(String giftCardCategory) {
    mGiftCardCategory = giftCardCategory;
  }
  
  /**
   * Set the category for gifts (initially used on the no results display on find wishlist)
   * 
   * @return
   */
  public String getGiftCategory() {
    return mGiftCategory;
  }
  
  /**
   * Get the category for gifts (initially used on the no results display on find wishlist)
   * 
   * @param giftCategory
   */
  public void setGiftCategory(String giftCategory) {
    mGiftCategory = giftCategory;
  }
  
  private WishlistPagingModel mPagingModel;
  
  public WishlistPageModel() {}
  
  public WishlistPagingModel getPaging() {
    return mPagingModel;
  }
  
  public void setPagingModel(WishlistPagingModel value) {
    mPagingModel = value;
  }
  
  public void setProductFormHandler(RegistryProductFormHandler value) {
    mProductFormHandler = value;
  }
  
  public void setAccountFormHandler(RegistryAccountFormHandler value) {
    mAccountFormHandler = value;
  }
  
  public void setProfile(NMProfile value) {
    mProfile = value;
  }
  
  public List<GiftItem> getFilteredList() {
    return (mPagingModel != null) ? mPagingModel.getFilteredList() : null;
  }
  
  public boolean getIsFirstProductOnCart() {
    return mIsFirstProductOnCart;
  }
  
  public List<NMCommerceItemTempHolder> getTempProductList() {
    return mTempProductList;
  }
  
  public void setIsFirstProductOnCart(boolean value) {
    mIsFirstProductOnCart = value;
  }
  
  public void setTempProductList(List<NMCommerceItemTempHolder> value) {
    mTempProductList = value;
  }
  
  public boolean getHasFormError() {
    boolean returnValue = false;
    
    if (mProductFormHandler != null) {
      returnValue = mProductFormHandler.getFormError();
    } else if (mAccountFormHandler != null) {
      returnValue = mAccountFormHandler.getFormError();
    }
    
    return returnValue;
  }
  
  public Vector getFormErrors() {
    Vector returnValue = null;
    
    if (mProductFormHandler != null) {
      returnValue = mProductFormHandler.getFormExceptions();
    } else if (mAccountFormHandler != null) {
      returnValue = mAccountFormHandler.getFormExceptions();
    } else {
      returnValue = new Vector();
    }
    
    return returnValue;
  }
  
  public boolean getIsFilteredListEmpty() {
    boolean returnValue = true;
    
    List<GiftItem> filteredList = getFilteredList();
    
    if (filteredList != null) {
      returnValue = filteredList.isEmpty();
    }
    
    return returnValue;
  }
  
  public NMProfile getProfile() {
    return mProfile;
  }
  
  public String getWishlistId() {
    return mProductFormHandler.getGiftlistId();
  }
  
  public double getTgcTotal() {
    return mPagingModel.getTgcTotal();
  }
  
  public double getVgcTotal() {
    return mPagingModel.getVgcTotal();
  }
  
  public boolean isOnlyPinYin() {
    return onlyPinYin;
  }
  
  public void setOnlyPinYin(boolean onlyPinYin) {
    this.onlyPinYin = onlyPinYin;
  }
}
