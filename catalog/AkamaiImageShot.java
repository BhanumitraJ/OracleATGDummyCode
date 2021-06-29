package com.nm.commerce.catalog;

import atg.repository.RepositoryItem;

public class AkamaiImageShot extends ImageShot {
  private String mAkamaiZoomUrl = null;
  private String mAkamaiProductUrl = null;
  
  public AkamaiImageShot(RepositoryItem item, String akamaiProductUrl, String akamaiZoomUrl, RepositoryItem productItem) {
    super(item, productItem);
    mAkamaiProductUrl = akamaiProductUrl;
    mAkamaiZoomUrl = akamaiZoomUrl;
  }
  
  public String getImageURL() {
    return getImageUrl(mAkamaiProductUrl, super.getImageURL());
  }
  
  public String getRelativeImageURL() {
    return super.getImageURL();
  }
  
  private String getImageUrl(String prefix, String url) {
    String returnValue = null;
    
    if ((prefix != null) && !isEmpty(url)) {
      returnValue = prefix + url;
    } else {
      returnValue = url;
    }
    
    return returnValue;
  }
  
  public String getLargePopupImageURL() {
    return getImageUrl(mAkamaiProductUrl, super.getLargePopupImageURL());
  }
  
  public String getSizeN_ImageURL() {
    return getImageUrl(mAkamaiProductUrl, super.getSizeN_ImageURL());
  }
  
  public String getProdMainImageURL() {
    return getImageUrl(mAkamaiProductUrl, super.getProdMainImageURL());
  }
  
  public String getZoomPopupImageURL() {
    return getImageUrl(mAkamaiZoomUrl, super.getZoomPopupImageURL());
  }
}
