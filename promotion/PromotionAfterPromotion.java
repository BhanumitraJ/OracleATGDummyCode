package com.nm.commerce.promotion;

import atg.repository.RepositoryItem;
import java.util.Date;

public class PromotionAfterPromotion {
  private RepositoryItem mDatasource = null;
  private boolean mIsActive = false;
  
  public PromotionAfterPromotion(RepositoryItem repositoryItem) {
    mDatasource = repositoryItem;
  }
  
  public String getInitialPromotion() {
    return (String) mDatasource.getPropertyValue("initialPromotion");
  }
  
  public String getLinkedPromotion() {
    return (String) mDatasource.getPropertyValue("linkedPromotion");
  }
  
  public String getConfirmationText() {
    return (String) mDatasource.getPropertyValue("confirmationText");
  }
  
  public Date getCreationDate() {
    return (Date) mDatasource.getPropertyValue("creationDate");
  }
  
  public int getLimitEntries() {
    int returnValue = -1;
    
    Integer limitEntries = (Integer) mDatasource.getPropertyValue("limitEntries");
    
    if (limitEntries != null) {
      returnValue = limitEntries.intValue();
    }
    
    return returnValue;
  }
  
  public void setIsActive(boolean value) {
    mIsActive = value;
  }
  
  public boolean getIsActive() {
    return mIsActive;
  }
}
