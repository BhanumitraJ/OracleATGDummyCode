package com.nm.commerce.catalog;

import atg.repository.RepositoryItem;

public class VideoShot extends ImageShot {
  
  public VideoShot() {}
  
  public VideoShot(RepositoryItem repositoryItem, RepositoryItem productItem) {
    super(repositoryItem, productItem);
  }
  
  public String getVideoURL() {
    return getProdMainImageURL();
  }
  
}
