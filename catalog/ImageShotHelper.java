package com.nm.commerce.catalog;

import java.util.List;
import java.util.Map;

import atg.core.util.StringUtils;
import atg.repository.RepositoryItem;
import atg.servlet.DynamoHttpServletRequest;
import atg.servlet.ServletUtil;

import com.nm.abtest.AbTestHelper;
import com.nm.common.INMGenericConstants;
import com.nm.components.CommonComponentHelper;
import com.nm.configurator.utils.ProductDynamicImageUrlGenerator;
import com.nm.utils.AkamaiUrlHelper;
import com.nm.utils.ProdSkuUtil;
import com.nm.utils.StringUtilities;

/********************************************************
 * @author Elaine Hopkins
 * @since 3/31/2006 This helper class accepts a repository item of product through it's get method. It will iterate through all of the images loaded in the product's auxiliaryMedia map and for each
 *        create an imageShot object. The imageShot objects will be stored/returned in a HashMap with keys that correspond to the standard image shot names (i.e.: mg, mp, ag,etc) + _the prodId
 *        (mg_prod4567852)
 * 
 * 
 *        Images are classified through the use of two characters combinations First character = type of image shot (m=main, a=alternate, b=alternate2, c=alternate3, z=zoom, e=editorial) Second
 *        character = size of image (g=75x94, y=100x125, h=138x173, t=173x216, n=216x270, j=230x288, f=274x343, d=309x387, x=336x20, p=451x564, z= zoomIndicator) Example: ImageShot of mg = shot type
 *        of main in the g size of 75x94 Note: there is an image type of "z"...but also an image size of "z". When the second character of an image shot label is "z" it is being used as an indicator
 *        that the product shot type has zoom capability. (examples: mz, az, bz, cz, zz)
 * 
 ********************************************************/

public class ImageShotHelper {
  
  private String[] imageShotTypesList;
  private String mainImageType;
  private String mainImageSize;
  private String mainImageSizeGroups;
  private String alternateImageSize;
  private String zoomIndicator;
  private String videoImageType;
  private String videoAltImageSize;
  private String videoMainImageSize;
  private String quickViewImageType;
  private String quickViewImageSize;
  private String[] quickViewAltImageTypes;
  private String quickViewType = "";
  private String editorialImageType;
  private Boolean productPageVideoEnabled;
  private boolean productDetailPage;
  private ProductDynamicImageUrlGenerator productDynamicImageUrlGenerator;
  protected AkamaiUrlHelper akamaiUrlHelper = null;
  private static final String CHILD_ITEM_ID = "childItemId";
  
  /** The refined color key. */
  private String refinedColorKey;

  /*********************************************
   * CONSTRUCTOR
   *********************************************/
  public ImageShotHelper() {}
  
  /**
   * 
   * @param product
   * @param imageKey
   * @param secure
   * @param useDynamicImg
   *          // determine if we want to display dynamic image or not, on some templates we do not want to display dynamic images even they are available
   * @return
   */
  public ImageShot createImageShot(final NMProduct product, final String imageKey, final boolean secure, final boolean useDynamicImg) {
    ImageShot imageShot = null;
    final RepositoryItem imageItem = product.getMediaItem(imageKey);
/*    product.getId();*/
    DynamoHttpServletRequest request = ServletUtil.getCurrentRequest();
    Boolean isProductDetailPage = false;
    if(request != null){
    	String requestURI= request.getRequestURI();
      if (!StringUtilities.isNullOrEmpty(requestURI) && requestURI.contains("product.jsp")) {
    		isProductDetailPage = true;
    	}
    }
   
   
       
    if (imageItem != null) {
      
      imageShot = createImageShot(imageItem, secure, product.getDataSource(), useDynamicImg);
      
      final Object flg = imageItem.getPropertyValue("flgDynamicImageSkuColor");
      // FIXME Refactor isDynamicImg to isDynamicColorImage
      boolean isDynamicImg = false;
      if (flg != null) {
        isDynamicImg = (Boolean) flg;
        // FIXME At this point in time the .jsp's treat 'isDynamic' as the color swatch flag. This needs to be refactored to use isDynamicColor.
        imageShot.setIsDynamicColor(isDynamicImg);
        imageShot.setIsDynamic(isDynamicImg);
      }
      final Object flg2 = imageItem.getPropertyValue("flgDynamicImageSpecialInstruction");
      boolean isDynamicMonoImg = false;
      if (flg2 != null) {
        isDynamicMonoImg = (Boolean) flg2;
        imageShot.setIsDynamicMonogram(isDynamicMonoImg);
      }
      // if we want to use dynamic image and current media item has dynamic image
     
      String elimsGroup = AbTestHelper.getAbTestValue(request, AbTestHelper.ELIMS_SUITE_GROUP);
      if (product.getFlgConfigurable()) {
      	initializeImageShot(imageShot, imageKey, mainImageSize, product);
      }else if (useDynamicImg && (isDynamicImg || isDynamicMonoImg)) {
        initializeDynamicImageShot(imageShot, imageKey, mainImageSize, product);
      } else if (StringUtilities.isNotEmpty(elimsGroup) && StringUtilities.areEqualIgnoreCase(elimsGroup, "Y") && isProductDetailPage) {
        initializeImageShot(imageShot, imageKey, mainImageSizeGroups, product);
      } else {
        initializeImageShot(imageShot, imageKey, mainImageSize, product);
      }
    }
    return imageShot;
  }
  
  // key is the two-letter image shot / size code
  public ImageShot createImageShot(final NMProduct product, final String imageKey, final boolean secure) {
    return createImageShot(product, imageKey, secure, false);
  }
  
  public ImageShot createViewShotWithCustomMainSize(final NMProduct product, final String imageKey, final String mainImageSize, final boolean secure) {
    ImageShot imageShot = null;
    final RepositoryItem imageItem = product.getMediaItem(imageKey);
    if (imageItem != null) {
		imageShot = createImageShot(imageItem, secure, product.getDataSource());
        initializeImageShot(imageShot, imageKey, mainImageSize, product);
    }
    return imageShot;
  }
  
  // key is the two-letter image shot / size code
  public ImageShot createVideoShot(final NMProduct product, final String imageKey, final boolean secure) {
    return createVideoShot(product, imageKey, secure, null, null);
  }
  
  public ImageShot createVideoShot(final NMProduct product, final String imageKey, final boolean secure, final String selectedSize, final String selectedAltSize) {
    ImageShot imageShot = null;
    final RepositoryItem imageItem = product.getMediaItem(imageKey);
    if (imageItem != null) {
      imageShot = createVideoShot(imageItem, secure, product.getDataSource());
      String videoSize = selectedSize;
      String altSize = selectedAltSize;
      // Video size override is not available. Use the image shot selection for Video.
      if (StringUtilities.isEmpty(videoSize)) {
        videoSize = mainImageSize;
      }
      // Video thumb size override is not available. Use the image shot selection for Video.
      if (StringUtilities.isEmpty(altSize)) {
        altSize = alternateImageSize;
      }
      initializeVideoShot(imageShot, imageKey, videoSize, altSize, product);
    }
    return imageShot;
  }
  
  // key is the two-letter image shot / size code
  /**
   * set image url for dynamic images
   * 
   * @param imageShot
   * @param key
   * @param mainImageSize
   * @param product
   */
  private void initializeDynamicImageShot(final ImageShot imageShot, final String key, final String mainImageSize, final NMProduct product) {
    imageShot.setShotLabel(key + "_" + product.getId());
    final String currentShotType = imageShot.getShotType();
    final String currentShotTypeMainKey = currentShotType + mainImageSize;
    final String currentShotTypeAlternateKey = currentShotType + alternateImageSize;
    final String zoomShotTypeAlternateKey = currentShotType + "z";
    imageShot.setCanDisplayMainImage(true);
    
    // set color_key to be the first available SKU color
    final List<NMSku> skus = product.getSellableSkuList();
    NMSku sku4color = null;
    Map<String,String> staticSkuImageMap = null;
    String staticSkuImageAssetName = null;
    String mainUrl ="";
    String altUrl = "";
    String zoomPopupImageURL="";
    final ProdSkuUtil prodSkuUtil = CommonComponentHelper.getProdSkuUtil();
    final String colorKeyQueryValue = prodSkuUtil.getValueByQuertString(CHILD_ITEM_ID);
    String[] queryStringParams = null;
    if(!StringUtils.isEmpty(colorKeyQueryValue)){
    	queryStringParams = colorKeyQueryValue.split(INMGenericConstants.STRING_UNDERSCORE);
    }
    
    // if no sellable sku, default to the first sku
    if (skus.size() == 0) {
      final List<RepositoryItem> allSkus = (List<RepositoryItem>) product.getPropertyValue("childSKUs");
      if (allSkus.size() > 0) {
        sku4color = new NMSku(allSkus.get(0));
      }
    } else {
     
      CommonComponentHelper.getLogger();
      
      for (final NMSku sku : skus) {
        if(sku4color != null && staticSkuImageMap != null){
        	break;
        }
        if (sku4color == null) {
          final boolean isDefaultSkucolor = prodSkuUtil.checkIfDefaultSkuColor(product, sku);
          if (isDefaultSkucolor && StringUtils.isEmpty(getRefinedColorKey())) {
            sku4color = sku;
          } else if (sku.getCodeVariation1().equals(getRefinedColorKey())) {
            sku4color = sku;
          }
        }
        if(staticSkuImageMap == null && queryStringParams != null && queryStringParams.length >= 2 && queryStringParams[1] != null && sku.getCodeVariation1().equals(queryStringParams[1])){
        	staticSkuImageMap = sku.getSkuStaticImageUrl();
        }
      }
      if (sku4color == null) {
        sku4color = skus.get(0);
      }
    }
    String color_key = "";
    if (sku4color != null) {
      color_key = sku4color.getCodeVariation1();
      if(staticSkuImageMap == null){
    	  staticSkuImageMap = sku4color.getSkuStaticImageUrl();
      }
    }
    if(staticSkuImageMap != null){
        for (Map.Entry<String, String> entry : staticSkuImageMap.entrySet())
        {
            if(entry.getKey().substring(0,1).equals(key.substring(0, 1))){
                staticSkuImageAssetName = entry.getValue();
            }
        }
    }

    imageShot.setProdMainImageSize(mainImageSize);
    if(StringUtils.isEmpty(staticSkuImageAssetName) && sku4color != null){
        mainUrl = productDynamicImageUrlGenerator.generateDynamicColorImageShotFullUrl(productDynamicImageUrlGenerator.getRenderServer(), color_key, currentShotTypeMainKey, product, sku4color,
                  imageShot.getIsDynamicColor());
        altUrl =  productDynamicImageUrlGenerator.generateDynamicColorImageShotFullUrl(productDynamicImageUrlGenerator.getRenderServer(), color_key, currentShotTypeAlternateKey, product, sku4color,
                  imageShot.getIsDynamicColor());
        zoomPopupImageURL =  productDynamicImageUrlGenerator.generateDynamicColorImageShotFullUrl(productDynamicImageUrlGenerator.getRenderServer(), color_key, zoomShotTypeAlternateKey, product, sku4color,
                             imageShot.getIsDynamicColor());
    }
    else{
        mainUrl = productDynamicImageUrlGenerator.generateStaticSkuImageUrl(productDynamicImageUrlGenerator.getImageServer(), currentShotTypeMainKey, staticSkuImageAssetName);
        altUrl =  productDynamicImageUrlGenerator.generateStaticSkuImageUrl(productDynamicImageUrlGenerator.getImageServer(), currentShotTypeAlternateKey, staticSkuImageAssetName);
        zoomPopupImageURL =    productDynamicImageUrlGenerator.generateStaticSkuImageUrl(productDynamicImageUrlGenerator.getImageServer(), zoomShotTypeAlternateKey, staticSkuImageAssetName);
    }

    imageShot.setProdMainImageURL(productDynamicImageUrlGenerator.updateDynamicColorImageShotFullUrl(imageShot.getIsDynamicMonogram(), mainUrl, product));
	imageShot.setImageURL(productDynamicImageUrlGenerator.updateDynamicColorImageShotFullUrl(imageShot.getIsDynamicMonogram(), altUrl, product));
	imageShot.setZoomPopupImageURL(productDynamicImageUrlGenerator.updateDynamicColorImageShotFullUrl(imageShot.getIsDynamicMonogram(), zoomPopupImageURL, product));
    final String imageAltText = product.getPropertyValue("cmosCatalogId") + "_" + product.getPropertyValue("cmosItemCode");
    imageShot.setImageAltText(imageAltText);
    imageShot.setOnErrorImg("RWD.product_images.scene7Error(this,336,420)");
  }

  // key is the two-letter image shot / size code
  private void initializeImageShot(final ImageShot imageShot, final String key, final String mainImageSize, final NMProduct product) {
    imageShot.setShotLabel(key + "_" + product.getId());
    
    final String currentShotType = imageShot.getShotType();
    final String currentShotTypeMainKey = currentShotType + mainImageSize;
    final String currentShotTypeAlternateKey = currentShotType + alternateImageSize;
    
    imageShot.setProdMainImageSize(mainImageSize);
    	/**
    	 * Code changes for NMOBLDS-6521
    	 */
		//final String mainUrl = product.getFlgConfigurable() ? product.getDynamicImageUrl(currentShotTypeMainKey) : product.getImageUrl(currentShotTypeMainKey);
		//final String alternateUrl = product.getFlgConfigurable() ? product.getDynamicImageUrl(currentShotTypeAlternateKey) : product.getImageUrl(currentShotTypeAlternateKey);
		final String mainUrl = product.getImageUrl(currentShotTypeMainKey);
		final String alternateUrl = product.getImageUrl(currentShotTypeAlternateKey);
    if (!isEmpty(mainUrl) && !isEmpty(alternateUrl)) {
      imageShot.setProdMainImageURL(mainUrl);
      imageShot.setCanDisplayMainImage(true);
    }
    
    if(product.getFlgConfigurable()){
    	imageShot.setOnErrorImg("RWD.product_images.scene7Error(this,336,420)");
    }
    final String imageAltText = product.getPropertyValue("cmosCatalogId") + "_" + product.getPropertyValue("cmosItemCode");
    imageShot.setImageAltText(imageAltText);
  }
  
  /***
   * Method to initialize all video properties.
   * 
   * @param videoShot
   * @param key
   * @param videoSize
   * @param altImageSize
   * @param product
   */
  private void initializeVideoShot(final ImageShot videoShot, final String key, final String videoSize, final String altImageSize, final NMProduct product) {
    videoShot.setShotLabel(key + "_" + product.getId());
    
    final String currentShotType = videoShot.getShotType();
    final String currentShotTypeMainKey = currentShotType + videoSize;
    final String currentShotTypeAlternateKey = currentShotType + altImageSize;
    
    videoShot.setProdMainImageSize(videoSize);
    
    final String mainUrl = product.getImageUrl(currentShotTypeMainKey);
    final String alternateUrl = product.getImageUrl(currentShotTypeAlternateKey);
    if (!isEmpty(mainUrl) && !isEmpty(alternateUrl)) {
      videoShot.setProdMainImageURL(mainUrl);
      videoShot.setCanDisplayMainImage(true);
    }
    
    final String imageAltText = product.getPropertyValue("cmosCatalogId") + "_" + product.getPropertyValue("cmosItemCode");
    videoShot.setImageAltText(imageAltText);
  }
  
  private ImageShot createImageShot(final RepositoryItem item, final boolean isSecure, final RepositoryItem productItem, final boolean useDynamicImg) {
    ImageShot returnValue = null;
    
    if (akamaiUrlHelper != null && !useDynamicImg) {
      final String akamaiProductUrl = akamaiUrlHelper.getAkamaiUrl(isSecure, AkamaiUrlHelper.PRODUCT_URL, "");
      final String akamaiZoomUrl = akamaiUrlHelper.getAkamaiUrl(isSecure, AkamaiUrlHelper.ZOOM_URL, "");
      returnValue = new AkamaiImageShot(item, akamaiProductUrl, akamaiZoomUrl, productItem);
    } else {
      returnValue = new ImageShot(item, productItem);
    }
    return returnValue;
  }
  
  private ImageShot createImageShot(final RepositoryItem item, final boolean isSecure, final RepositoryItem productItem) {
    return createImageShot(item, isSecure, productItem, false);
  }
	// Below Code changes for NMOBLDS-6521  - Defect NMOBLDS-7349
/*	private ImageShot createDynamicImageShot(final RepositoryItem item, final boolean isSecure, final RepositoryItem productItem) {
		return createImageShot(item, isSecure, productItem, true);
	}
*/
  private VideoShot createVideoShot(final RepositoryItem item, final boolean isSecure, final RepositoryItem productItem) {
    VideoShot returnValue = null;
    
    if (akamaiUrlHelper != null) {
      final String akamaiVideoUrl = akamaiUrlHelper.getAkamaiUrl(isSecure, AkamaiUrlHelper.VIDEO_URL, "");
      returnValue = new AkamaiVideoShot(item, akamaiVideoUrl, productItem);
    } else {
      returnValue = new VideoShot(item, productItem);
    }
    
    return returnValue;
  }
  
  /************************************************************
   * Getters & Setters
   *************************************************************/
  
  /**
   * @return Returns the imageShotTypesList of image shot types that are to be used.
   */
  public String[] getImageShotTypesList() {
    return imageShotTypesList;
  }
  
  /**
   * @param imageShotTypesList
   *          Sets the image shot types that are to be used.
   */
  public void setImageShotTypesList(final String[] imageShotTypesList) {
    this.imageShotTypesList = imageShotTypesList;
  }
  
  /**
   * @return Returns the image shot type of the main image on the product page.
   */
  public String getMainImageType() {
    return mainImageType;
  }
  
  /**
   * @param mainImageType
   *          sets the image shot type of the main image on the product page.
   */
  public void setMainImageType(final String mainImageType) {
    this.mainImageType = mainImageType;
  }
  
  /**
   * @return Returns the image shot size of the main image on the product page for groups.
   */
  public String getMainImageSizeGroups() {
    return mainImageSizeGroups;
  }
  
  /**
   * @param mainImageSizeGroups
   *          sets the image shot size of the main image on the product page.
   */
  public void setMainImageSizeGroups(final String mainImageSizeGroups) {
    this.mainImageSizeGroups = mainImageSizeGroups;
  }
  
  /**
   * @return Returns the image shot size of the main image on the product page.
   */
  public String getMainImageSize() {
    return mainImageSize;
  }
  
  /**
   * @param mainImageSize
   *          sets the image shot size of the main image on the product page.
   */
  public void setMainImageSize(final String mainImageSize) {
    this.mainImageSize = mainImageSize;
  }
  

  /**
   * @return Returns the image shot size of the alternate images on the product page.
   */
  public String getAlternateImageSize() {
    return alternateImageSize;
  }
  
  /**
   * @param alternateImageSize
   *          sets the image shot size of the alternate images on the product page.
   */
  public void setAlternateImageSize(final String alternateImageSize) {
    this.alternateImageSize = alternateImageSize;
  }
  
  /**
   * @return Returns the indicator to used to designate whether zoom functionality is available for a product image.
   */
  public String getZoomIndicator() {
    return zoomIndicator;
  }
  
  /**
   * @param zoomIndicator
   *          sets the indicator value that is used to designate whether zoom functionality is available for a product image.
   */
  public void setZoomIndicator(final String zoomIndicator) {
    this.zoomIndicator = zoomIndicator;
  }
  
  public void setVideoImageType(final String value) {
    videoImageType = value;
  }
  
  public String getVideoImageType() {
    return videoImageType;
  }
  
  public String getVideoAltImageSize() {
    return videoAltImageSize;
  }
  
  public void setVideoAltImageSize(final String videoAltImageSize) {
    this.videoAltImageSize = videoAltImageSize;
  }
  
  public String getVideoMainImageSize() {
    return videoMainImageSize;
  }
  
  public void setVideoMainImageSize(final String videoMainImageSize) {
    this.videoMainImageSize = videoMainImageSize;
  }
  
  public void setProductPageVideoEnabled(final Boolean value) {
    productPageVideoEnabled = value;
  }
  
  public Boolean isProductPageVideoEnabled() {
    return productPageVideoEnabled;
  }
  

  public boolean isProductDetailPage() {
    return productDetailPage;
  }
  
  public void setProductDetailPage(boolean productDetailPage) {
    this.productDetailPage = productDetailPage;
  }

  public void setAkamaiUrlHelper(final AkamaiUrlHelper value) {
    akamaiUrlHelper = value;
  }
  
  public void setUseSecureProtocol(final boolean value) {
    akamaiUrlHelper.setUseSecureProtocol(value);
  }
  
  public String getQuickViewImageType() {
    return quickViewImageType;
  }
  
  public void setQuickViewImageType(final String quickViewImageType) {
    this.quickViewImageType = quickViewImageType;
  }
  
  public String getQuickViewImageSize() {
    return quickViewImageSize;
  }
  
  public void setQuickViewImageSize(final String quickViewImageSize) {
    this.quickViewImageSize = quickViewImageSize;
  }
  
  public String[] getQuickViewAltImageTypes() {
    return quickViewAltImageTypes;
  }
  
  public void setQuickViewAltImageTypes(final String[] quickViewAltImageTypes) {
    this.quickViewAltImageTypes = quickViewAltImageTypes;
  }
  
  public String getQuickViewType() {
    return quickViewType;
  }
  
  public void setQuickViewType(final String quickViewType) {
    this.quickViewType = quickViewType;
  }
  
  /**
   * @return the editorialImageType
   */
  public String getEditorialImageType() {
    return editorialImageType;
  }
  
  /**
   * @param editorialImageType
   *          the editorialImageType to set
   */
  public void setEditorialImageType(final String editorialImageType) {
    this.editorialImageType = editorialImageType;
  }
  
  private boolean isEmpty(final String text) {
    return text == null || text.length() == 0;
  }
  
  public ProductDynamicImageUrlGenerator getProductDynamicImageUrlGenerator() {
    return productDynamicImageUrlGenerator;
  }
  
  public void setProductDynamicImageUrlGenerator(final ProductDynamicImageUrlGenerator productDynamicImageUrlGenerator) {
    this.productDynamicImageUrlGenerator = productDynamicImageUrlGenerator;
  }
  
  /**
   * @return the refinedColorKey
   */
  public String getRefinedColorKey() {
    return refinedColorKey;
  }
  
  /**
   * @param refinedColorKey
   *          the refinedColorKey to set
   */
  public void setRefinedColorKey(String refinedColorKey) {
    this.refinedColorKey = refinedColorKey;
  }
}// end class
