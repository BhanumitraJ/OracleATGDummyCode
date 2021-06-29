package com.nm.commerce.catalog;

import atg.repository.RepositoryItem;

public class AkamaiVideoShot extends VideoShot {
  private String mAkamaiVideoUrl = null;
  
  public AkamaiVideoShot(RepositoryItem item, String akamaiVideoUrl, RepositoryItem productItem) {
    super(item, productItem);
    mAkamaiVideoUrl = akamaiVideoUrl;
  }
  
  public String getVideoURL() {
    String returnValue = null;
    
    if ((mAkamaiVideoUrl != null) && !isEmpty(super.getVideoURL())) {
      returnValue = mAkamaiVideoUrl + super.getVideoURL();
    } else {
      returnValue = super.getVideoURL();
    }
    
    return returnValue;
  }
}
