package com.nm.commerce.pagedef.model;

import java.util.List;

import com.nm.ajax.mynm.beans.MyNMProduct;
import com.nm.collections.DesignerIndexer;

/**
 * Model containing variables for use throughout a page.
 */
public class MyFavoritesPageModel extends EndecaDrivenPageModel {

	private List<DesignerIndexer.Category> myFavDesigners;
	private boolean loginStatus;
	private boolean recognizedStatus;
	private List<String> myFavEmailSubject;
	private String storeID;
	private String storeName;
	// product recoms in my favorite items page
	private List<MyNMProduct> recomCustFavProds;
	
	/** The item type. */
  private String itemType;
  
  /** The item status. */
  private String itemStatus;
  
  /** The favorited name. */
  private String favoritedName;
  
  /** flag for auto subscription for all designer alerts. **/
  private String autoDesignerAlert;
  
  private String designerSubscription;
  private String designerSubscribed;

	public List<MyNMProduct> getRecomCustFavProds() {
		return recomCustFavProds;
	}

	public void setRecomCustFavProds(List<MyNMProduct> recomCustFavProds) {
		this.recomCustFavProds = recomCustFavProds;
	}

	public String getStoreID() {
		return storeID;
	}

	public void setStoreID(String storeID) {
		this.storeID = storeID;
	}

	public String getStoreName() {
		return storeName;
	}

	public void setStoreName(String storeName) {
		this.storeName = storeName;
	}

	public boolean isLoginStatus() {
		return loginStatus;
	}

	public void setLoginStatus(boolean loginStatus) {
		this.loginStatus = loginStatus;
	}

	public List<String> getMyFavEmailSubject() {
		return myFavEmailSubject;
	}

	public void setMyFavEmailSubject(List<String> myFavEmailSubject) {
		this.myFavEmailSubject = myFavEmailSubject;
	}

	public List<DesignerIndexer.Category> getMyFavDesigners() {
		return myFavDesigners;
	}

	public void setMyFavDesigners(List<DesignerIndexer.Category> myFavDesigners) {
		this.myFavDesigners = myFavDesigners;
	}

	public boolean isRecognizedStatus() {
		return recognizedStatus;
	}

	public void setRecognizedStatus(boolean recognizedStatus) {
		this.recognizedStatus = recognizedStatus;
	}

  /**
   * Gets the item type.
   *
   * @return the itemType
   */
  public String getItemType() {
    return itemType;
  }

  /**
   * Sets the item type.
   *
   * @param itemType the itemType to set
   */
  public void setItemType(String itemType) {
    this.itemType = itemType;
  }

  /**
   * Gets the item status.
   *
   * @return the itemStatus
   */
  public String getItemStatus() {
    return itemStatus;
  }

  /**
   * Sets the item status.
   *
   * @param itemStatus the itemStatus to set
   */
  public void setItemStatus(String itemStatus) {
    this.itemStatus = itemStatus;
  }

  /**
   * Gets the favorited name.
   *
   * @return the favoritedName
   */
  public String getFavoritedName() {
    return favoritedName;
  }

  /**
   * Sets the favorited name.
   *
   * @param favoritedName the favoritedName to set
   */
  public void setFavoritedName(String favoritedName) {
    this.favoritedName = favoritedName;
  }


	public String getDesignerSubscription() {
		return designerSubscription;
	}

	public void setDesignerSubscription( String designerSubscription ) {
		this.designerSubscription = designerSubscription;
	}

	public String getDesignerSubscribed() {
		return designerSubscribed;
	}

	public void setDesignerSubscribed( String designerSubscribed ) {
		this.designerSubscribed = designerSubscribed;
	}

	public String getAutoDesignerAlert() {
		return autoDesignerAlert;
	}

	public void setAutoDesignerAlert( String autoDesignerAlert ) {
		this.autoDesignerAlert = autoDesignerAlert;
	}
  
}
