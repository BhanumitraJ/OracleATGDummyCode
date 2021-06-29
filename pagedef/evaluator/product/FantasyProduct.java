package com.nm.commerce.pagedef.evaluator.product;

import java.util.List;

public class FantasyProduct {
  private String defaultImgPath;
  private String giftId;
  private List<FantasyImage> fantasyAltImages;
  
  public void setDefaultImgPath(String defaultImgPath) {
    this.defaultImgPath = defaultImgPath;
  }
  
  public String getDefaultImgPath() {
    return defaultImgPath;
  }
  
  public void setGiftId(String giftId) {
    this.giftId = giftId;
  }
  
  public String getGiftId() {
    return giftId;
  }
  
  public void setFantasyAltImages(List<FantasyImage> fantasyImages) {
    this.fantasyAltImages = fantasyImages;
  }
  
  public List<FantasyImage> getFantasyAltImages() {
    return fantasyAltImages;
  }
  
  public class FantasyImage {
    private String mainImgPath;
    private String thumbnailImgPath;
    private String largerImgPath;
    private String shot;
    private String zoomImagePath;
    
    public FantasyImage(String main, String thumbnail, String larger, String shot, String zoomImage) {
      this.mainImgPath = main;
      this.thumbnailImgPath = thumbnail;
      this.largerImgPath = larger;
      this.shot = shot;
      this.zoomImagePath = zoomImage;
    }
    
    public String getMainImgPath() {
      return mainImgPath;
    }
    
    public void setMainImgPath(String mainImgPath) {
      this.mainImgPath = mainImgPath;
    }
    
    public String getThumbnailImgPath() {
      return thumbnailImgPath;
    }
    
    public void setThumbnailImgPath(String thumbnailImgPath) {
      this.thumbnailImgPath = thumbnailImgPath;
    }
    
    public String getLargerImgPath() {
      return largerImgPath;
    }
    
    public void setLargerImgPath(String largerImgPath) {
      this.largerImgPath = largerImgPath;
    }
    
    public String getShot() {
      return shot;
    }
    
    public void setShot(String shot) {
      this.shot = shot;
    }

	public String getZoomImagePath() {
		return zoomImagePath;
	}

	public void setZoomImagePath(String zoomImagePath) {
		this.zoomImagePath = zoomImagePath;
	}
  }
}
