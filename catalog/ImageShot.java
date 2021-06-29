package com.nm.commerce.catalog;

import atg.repository.RepositoryItem;

import com.nm.servlet.filter.RedirectFilter;

/********************************************************
 * @author Elaine Hopkins
 * @since 3/31/2006 This data class accepts an object which is all the aux_media information for this particular image shot. The media information is assigned to appropriate attributes and also used
 *        to determine the values of other related attributes. This ImageShot object provides easy access to all necessary media type information.
 * 
 * 
 *        Images are classified through the use of two characters combinations First character = type of image shot (m=main, a=alternate, b=alternate2, c=alternate3, z=zoom, e=editorial) Second
 *        character = size of image (g=75x94, y=100x125, h=138x173, t=173x216, n=216x270, j=230x288, f=274x343, d=309x387, x=336x20, p=451x564, z= zoomIndicator) Example: ImageShot of mg = shot type
 *        of main in the g size of 75x94 Note: there is an image type of "z"...but also an image size of "z". When the second character of an image shot label is "z" it is being used as an indicator
 *        that the product shot type has zoom capability. (examples: mz, az, bz, cz, zz)
 * 
 ********************************************************/

public class ImageShot {
  
  private RepositoryItem dataSource = null;
  private RepositoryItem productDataSource = null;
  private String imageURL = "";
  private String imageText = "";
  private String imageName = "";
  private String shotType = "";
  private String shotTypeLetter = "";
  private String shotSize = "";
  private String shotLabel = "";
  private boolean canDisplayMainImage = false;
  private String largePopupImageURL = "";
  private String zoomPopupImageURL = "";
  private String sizeN_ImageURL = "";
  private String sizeF_ImageURL = "";
  private String zoomPopupMediaId = "";
  private String prodMainImageSize = "";
  private String prodMainImageURL = "";
  private String imageAltText = "";
  private Boolean isDynamic = false;
  private Boolean isDynamicColor = false;
  private Boolean isDynamicMonogram = false;
  private String onErrorImg = "shimImageRWD(this)";
  
  /*************************************************************************
   * CONSTRUCTOR
   ************************************************************************/
  
  public ImageShot() {}
  
  public ImageShot(RepositoryItem item, RepositoryItem productItem) {
    setDataSource(item);
    setProductDataSource(productItem);
    
    String imgURL = (String) getDataSource().getPropertyValue("url");
    Integer version = (Integer) getDataSource().getPropertyValue("version");
    
    if (imgURL != null && imgURL.length() > 0) {
      if (version != null) {
        imgURL = RedirectFilter.CACHE_PATH + "/" + version + imgURL;
      }
    } else {
      imgURL = null;
    }
    
    setImageURL(imgURL);
    setImageText((String) getDataSource().getPropertyValue("imageText"));
    
    if (imgURL != null) {
      int imgURLSize = imgURL.length();
      int lastSlashIndex = imgURL.lastIndexOf('/') + 1;
      String strImageName = imgURL.substring(lastSlashIndex, imgURLSize);
      setImageName(strImageName);
    }
  }
  
  /*************************************************************************
   * Name of Method: getUrlPathForTag gets the URL based on the tag from the Product Media tag association Example of result: /products/mp/NMTO5TY_mp.jpg
   ************************************************************************/
  private String getUrlPathForTag(String tag) {
    RepositoryItem productItem = getProductDataSource();
    if (productItem != null) {
      NMProduct nmProduct = new NMProduct(productItem);
      return nmProduct.getImageUrl(getShotTypeLetter() + tag);
    }
    return null;
  }
  
  /*************************************************************************
   * Name of Method: determineShotType determines the shot type based on first character of shotLabel and calls set method
   ************************************************************************/
  private void determineShotType() {
    String imgLabel = (String) getShotLabel();
    if (imgLabel != null) {
      if (imgLabel.length() > 2) {
        String strShotType = imgLabel.substring(0, 1);
        setShotType(strShotType);
        setShotTypeLetter(strShotType);
      } else if (imgLabel.length() == 1) {
        setShotType(imgLabel);
        setShotTypeLetter(imgLabel);
      }
    }
  }
  
  /*************************************************************************
   * Name of Method: determineShotSize determines the shot size based on second character of shotLabel and calls set method
   ************************************************************************/
  public void determineShotSize() {
    String imgLabel = (String) getShotLabel();
    if (imgLabel != null) {
      if (imgLabel.length() > 2) {
        String strShotSize = imgLabel.substring(1, 2);
        setShotSize(strShotSize);
      } else if (imgLabel.length() == 1) {
        // old image with only one character as label - no size
        // reference
      }
    }
  }
  
  /**
   * Returns the product ID based on the shot label
   */
  public String getProductId() {
    String imgLabel = (String) getShotLabel();
    if ((imgLabel != null) && (imgLabel.length() > 3)) {
      return imgLabel.substring(3);
    }
    return null;
  }
  
  public void setProdMainImageURL(String prodMainImageURL) {
    this.prodMainImageURL = prodMainImageURL;
  }
  
  public String getProdMainImageURL() {
    if (isEmpty(prodMainImageURL)) {
      prodMainImageURL = nullEmpty(getUrlPathForTag(prodMainImageSize));
    }
    return nullEmpty(prodMainImageURL);
  }
  
  public void setLargePopupImageURL(String largePopupImageURL) {
    this.largePopupImageURL = largePopupImageURL;
  }
  
  // lazy load this value when requested
  public String getLargePopupImageURL() {
    if (isEmpty(largePopupImageURL)) {
      largePopupImageURL = nullEmpty(getUrlPathForTag("p"));
    }
    return nullEmpty(largePopupImageURL);
  }
  
  public void setZoomPopupImageURL(String zoomPopupImageURL) {
    this.zoomPopupImageURL = zoomPopupImageURL;
  }
  
  // lazy load this value when requested
  public String getZoomPopupImageURL() {
    if (isEmpty(zoomPopupImageURL)) {
      zoomPopupImageURL = nullEmpty(getUrlPathForTag("z"));
    }
    return nullEmpty(zoomPopupImageURL);
  }
  
  public void setSizeF_ImageURL(String sizeF_ImageURL) {
    this.sizeF_ImageURL = sizeF_ImageURL;
  }
  
  // lazy load this value when requested
  public String getSizeF_ImageURL() {
    if (isEmpty(sizeF_ImageURL)) {
      sizeF_ImageURL = nullEmpty(getUrlPathForTag("f"));
    }
    return nullEmpty(sizeF_ImageURL);
  }
  
  public void setSizeN_ImageURL(String sizeN_ImageURL) {
    this.sizeN_ImageURL = sizeN_ImageURL;
  }
  
  // lazy load this value when requested
  public String getSizeN_ImageURL() {
    if (isEmpty(sizeN_ImageURL)) {
      sizeN_ImageURL = nullEmpty(getUrlPathForTag("n"));
    }
    return nullEmpty(sizeN_ImageURL);
  }
  
  public void setZoomPopupMediaId(String zoomPopupMediaId) {
    this.zoomPopupMediaId = zoomPopupMediaId;
  }
  
  // lazy load this value when requested
  public String getZoomPopupMediaId() {
    if (isEmpty(zoomPopupMediaId)) {
      String url = getZoomPopupImageURL();
      if (!isEmpty(url)) {
        // strip the folder structures
        int startIndex = url.lastIndexOf("/");
        setZoomPopupMediaId(url.substring(startIndex + 1));
      }
    }
    return zoomPopupMediaId;
  }
  
  public RepositoryItem getDataSource() {
    return this.dataSource;
  }
  
  public void setDataSource(RepositoryItem dataSource) {
    this.dataSource = dataSource;
  }
  
  public String getImageURL() {
    return this.imageURL;
  }
  
  public void setImageURL(String imageURL) {
    this.imageURL = imageURL;
  }
  
  public String getImageText() {
    return this.imageText;
  }
  
  public void setImageText(String imageText) {
    this.imageText = imageText;
  }
  
  public String getImageName() {
    return this.imageName;
  }
  
  public void setImageName(String imageName) {
    this.imageName = imageName;
  }
  
  public String getShotType() {
    return this.shotType;
  }
  
  public void setShotType(String shotType) {
    this.shotType = shotType;
  }
  
  public String getShotTypeLetter() {
    return this.shotTypeLetter;
  }
  
  public Boolean getIsDynamic() {
    return isDynamic;
  }
  
  public void setIsDynamic(boolean dynamic) {
    this.isDynamic = dynamic;
  }
  
  public void setShotTypeLetter(String shotTypeL) {
    this.shotTypeLetter = shotTypeL;
  }
  
  public String getShotSize() {
    return this.shotSize;
  }
  
  public void setShotSize(String shotSize) {
    this.shotSize = shotSize;
  }
  
  public String getShotLabel() {
    return this.shotLabel;
  }
  
  /*****************************************************
   * @param shotLabel
   *          The shotLabel to set with image label + _ + prodId Example: sn_cprod4300057
   *****************************************************/
  public void setShotLabel(String shotLabel) {
    this.shotLabel = shotLabel;
    determineShotType();
    determineShotSize();
  }
  
  public boolean getCanPopup() {
    return !isEmpty(getLargePopupImageURL());
  }
  
  public boolean getCanZoomPopup() {
    return !isEmpty(getZoomPopupImageURL());
  }
  
  public boolean getCanDisplayMainImage() {
    return this.canDisplayMainImage;
  }
  
  public void setCanDisplayMainImage(boolean canDisplayMainImage) {
    this.canDisplayMainImage = canDisplayMainImage;
  }
  
  public String getProdMainImageSize() {
    return this.prodMainImageSize;
  }
  
  public void setProdMainImageSize(String prodMainImageSize) {
    this.prodMainImageSize = prodMainImageSize;
  }
  
  public String getImageAltText() {
    return this.imageAltText;
  }
  
  public void setImageAltText(String imageAltText) {
    this.imageAltText = imageAltText;
  }
  
  public void setProductDataSource(RepositoryItem productDataSource) {
    this.productDataSource = productDataSource;
  }
  
  public RepositoryItem getProductDataSource() {
    return productDataSource;
  }
  
  protected boolean isEmpty(String text) {
    return (text == null) || (text.length() == 0);
  }
  
  // returns an empty string if the value is null
  protected String nullEmpty(String text) {
    return (text == null) ? "" : text;
  }

public Boolean getIsDynamicColor() {
	return isDynamicColor;
}

public void setIsDynamicColor(Boolean isDynamicColor) {
	this.isDynamicColor = isDynamicColor;
}

public Boolean getIsDynamicMonogram() {
	return isDynamicMonogram;
}

public void setIsDynamicMonogram(Boolean isDynamicMonogram) {
	this.isDynamicMonogram = isDynamicMonogram;
}

/**
 * @return the onErrorImg
 */
public String getOnErrorImg() {
	return onErrorImg;
}

/**
 * @param onErrorImg the onErrorImg to set
 */
public void setOnErrorImg(String onErrorImg) {
	this.onErrorImg = onErrorImg;
}

}
