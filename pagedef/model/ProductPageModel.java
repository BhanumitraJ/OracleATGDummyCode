package com.nm.commerce.pagedef.model;

import java.util.List;
import java.util.Map;
import java.util.Set;

import atg.repository.RepositoryItem;

import com.nm.ajax.myfavorites.utils.MyFavoritesConstants;
import com.nm.catalog.navigation.Breadcrumb;
import com.nm.commerce.catalog.ImageShot;
import com.nm.commerce.catalog.NMPagingPrevNext;
import com.nm.commerce.catalog.NMProduct;
import com.nm.commerce.catalog.NMProductPrice;
import com.nm.commerce.catalog.NMSku;
import com.nm.commerce.catalog.NMSuite;
import com.nm.commerce.pagedef.evaluator.product.RelatedProductBean;
import com.nm.commerce.pagedef.model.bean.RichRelevanceProductBean;
import com.nm.utils.SkuColorData;

public class ProductPageModel extends PageModel {
  private NMProduct product;
  private NMSuite selectedSuiteProduct;
  private List<NMSuite> suiteList;
  private boolean isChanel;
  private boolean isHermes;
  private String referringNavType;
  private List<NMProduct> displayProductList;
  private List<String> trackerBreadCrumbs;
  
  /** The display suite items with single sku. */
  private List<NMProduct> displaySuiteItemsWithSingleSku;
  
  /** The display cmos skus. */
  private List<String> displayCmosSkus;
  
  /** The display sellable skus. */
  private List<Boolean> displaySellableSkus;
  
  /** The has preselected sku. */
  private Set<NMProduct> preSelectedProduct;
  
  private String focusProductId;
  private boolean editPage;

  /**
   * @return the preSelectedProduct
   */
  public Set<NMProduct> getPreSelectedProduct() {
    return preSelectedProduct;
  }
  
  /**
   * @param preSelectedProduct
   *          the preSelectedProduct to set
   */
  public void setPreSelectedProduct(Set<NMProduct> preSelectedProduct) {
    this.preSelectedProduct = preSelectedProduct;
  }
  
  private List<NMSku> preSelectedSkus;
  
  /** The status list. */
  private List<String> statusList;
  
  /** The product expected availability. */
  private List<String> productExpectedAvailability;
  
  /**
   * @return the preSelectedSkus
   */
  public List<NMSku> getPreSelectedSkus() {
    return preSelectedSkus;
  }
  
  /**
   * @param preSelectedSkus
   *          the preSelectedSkus to set
   */
  public void setPreSelectedSkus(List<NMSku> preSelectedSkus) {
    this.preSelectedSkus = preSelectedSkus;
  }
  
  /**
   * @return the statusList
   */
  public List<String> getStatusList() {
    return statusList;
  }
  
  /**
   * @param statusList
   *          the statusList to set
   */
  public void setStatusList(List<String> statusList) {
    this.statusList = statusList;
  }
  
  /**
   * @return the productExpectedAvailability
   */
  public List<String> getProductExpectedAvailability() {
    return productExpectedAvailability;
  }
  
  /** The showable list. */
  private List<Boolean> showableList;
  
  /** The un sellable sku. */
  private boolean unSellableSku;
  
  /** The display product expected availability. */
  private List<String> displayProductExpectedAvailability;
  
  /** The display showable skus. */
  private List<Boolean> displayShowableSkus;
  
  /** The display inventory status. */
  private List<String> displayInventoryStatus;
  
  /** The display stock level. */
  private List<String> displayStockLevel;
  private List<ImageShot> validAlternateViewsList;
  private String myFavProds;
  private Map<String, NMProduct> productPageMap;
  private NMPagingPrevNext pagingPrevNext;
  private List<RelatedProductBean> displayYMALProductList;
  private List<RelatedProductBean> displayCTLProductList;
  private List<RelatedProductBean> displayMoreColorsList;
  private String pageDesignerName;
  private boolean isAllDesignersSame;
  private boolean isDisplayAlternateViews;
  private Boolean isLongSuperSuite; // if the product is a super suite with more than 5 items
  private boolean isAllShopRunnerEligible;
  private String overviewTitle;
  private String productCopyTop;
  private String productCopyBottom;
  private String productCutline;
  private String designerContent;
  private String detailsContent;
  private String detailsTitle;
  private String swatchImageUrl;
  private boolean displayTabs;
  private NMProductPrice productPrice;
  private String localeName;
  private String langCode;
  private String depiction;
  private RepositoryItem productTranslationRI;
  private String suiteSizeGuide;
  private String needHelpLayout;
  private RichRelevanceProductBean ymalProducts;
  private RichRelevanceProductBean oosRrProducts;
  // Same Day Delivery values
  private Boolean sddEligible;
  private Boolean sddPageLoadEligible;
  private Map<String, Boolean> suitLoadEligible;
  private String sddDefaultLocation;
  private String storeSkuInventoriesJSON;
  private Boolean isFireSddOmnitureOnPageLoad;
  private Boolean monogarammable;
  private Boolean configurableProduct;
  private String prodSelectBoxPath;
  private String restrictedPrdsToRR;
  private Boolean isItemEligibleForDiscount = false;
  
  private Boolean prodHasCTLItems;
  
  private Boolean urlRedirected;
  
  /**
   * Property holds alipayEligible value
   */
  private boolean alipayEligibile;
  private Breadcrumb breadcrumb;
  
  // My Favorites -MYNM67 - added for my favorites
  private String myFavItems;
  
  /** The video on page. */
  private boolean videoOnPage;
  
  private String displayCTLTitle = MyFavoritesConstants.COMPLETE_THE_LOOK;
  
  public String getDisplayCTLTitle() {
    return displayCTLTitle;
  }
  
  public void setDisplayCTLTitle(String displayCTLTitle) {
    this.displayCTLTitle = displayCTLTitle;
  }
  
  public Breadcrumb getBreadcrumb() {
    return breadcrumb;
  }
  
  public void setBreadcrumb(final Breadcrumb breadcrumb) {
    this.breadcrumb = breadcrumb;
  }
  
  public Boolean getSddPageLoadEligible() {
    return sddPageLoadEligible;
  }
  
  public void setSddPageLoadEligible(Boolean sddPageLoadEligible) {
		this.sddPageLoadEligible = sddPageLoadEligible;
	}
  public Map<String, Boolean> getSuitLoadEligible() {
		return suitLoadEligible;
	}

	public void setSuitLoadEligible(Map<String, Boolean> suitLoadEligible) {
		this.suitLoadEligible = suitLoadEligible;
	}

  /**
   * @return alipayEligibile
   */
  public boolean isAlipayEligibile() {
    return alipayEligibile;
  }
  
  /**
   * @param alipayEligibile
   */
  public void setAlipayEligibile(final boolean alipayEligibile) {
    this.alipayEligibile = alipayEligibile;
  }
  
  private boolean serverSideYmal = false; // if we get Ymal recommendation from server side integration
  private boolean serverSideOOSR = false;
  
  private String shippingAndDeliveryPolicyCatId;
  private int totalCount;
  private int currentPosition;
  private boolean displayPrev;
  private boolean displayNext;
  
  /**
   * @return the displayPrev
   */
  public boolean isDisplayPrev() {
    return displayPrev;
  }
  
  /**
   * @param displayPrev
   *          the displayPrev to set
   */
  public void setDisplayPrev(boolean displayPrev) {
    this.displayPrev = displayPrev;
  }
  
  /**
   * @return the displayNext
   */
  public boolean isDisplayNext() {
    return displayNext;
  }
  
  /**
   * @param displayNext
   *          the displayNext to set
   */
  public void setDisplayNext(boolean displayNext) {
    this.displayNext = displayNext;
  }
  
  /**
   * @return the currentPosition
   */
  public int getCurrentPosition() {
    return currentPosition;
  }
  
  /**
   * @param currentPosition
   *          the currentPosition to set
   */
  public void setCurrentPosition(int currentPosition) {
    this.currentPosition = currentPosition;
  }
  
  public int getTotalCount() {
    return totalCount;
  }
  
  public void setTotalCount(int totalCount) {
    this.totalCount = totalCount;
  }
  
  private Map<String, List<SkuColorData>> productColorSwatchData;
  
  public boolean isServerSideOOSR() {
    return serverSideOOSR;
  }
  
  public void setServerSideOOSR(final boolean serverSideOOSR) {
    this.serverSideOOSR = serverSideOOSR;
  }
  
  public ProductPageModel() {}
  
  public boolean isChanel() {
    return isChanel;
  }
  
  public void setChanel(final boolean isChanel) {
    this.isChanel = isChanel;
  }
  
  public RichRelevanceProductBean getOosRrProducts() {
    return oosRrProducts;
  }
  
  public void setOosRrProducts(final RichRelevanceProductBean oosRrProducts) {
    this.oosRrProducts = oosRrProducts;
  }
  
  public boolean isHermes() {
    return isHermes;
  }
  
  public void setHermes(final boolean isHermes) {
    this.isHermes = isHermes;
  }
  
  public void setProduct(final NMProduct product) {
    this.product = product;
  }
  
  public NMProduct getProduct() {
    return product;
  }
  
  public void setSelectedSuiteProduct(final NMSuite selectedSuiteProduct) {
    this.selectedSuiteProduct = selectedSuiteProduct;
  }
  
  public NMSuite getSelectedSuiteProduct() {
    return selectedSuiteProduct;
  }
  
  public void setSuiteList(final List<NMSuite> suiteList) {
    this.suiteList = suiteList;
  }
  
  public List<NMSuite> getSuiteList() {
    return suiteList;
  }
  
  public void setReferringNavType(final String referringNavType) {
    this.referringNavType = referringNavType;
  }
  
  public String getReferringNavType() {
    return referringNavType;
  }
  
  public boolean isDisplayAlternateViews() {
    return isDisplayAlternateViews;
  }
  
  public void setDisplayAlternateViews(final boolean isDisplayAlternateViews) {
    this.isDisplayAlternateViews = isDisplayAlternateViews;
  }
  
  public List<ImageShot> getValidAlternateViewsList() {
    return validAlternateViewsList;
  }
  
  public void setValidAlternateViewsList(final List<ImageShot> validAlternateViewsList) {
    this.validAlternateViewsList = validAlternateViewsList;
  }
  
  public String getMyFavProds() {
    return myFavProds;
  }
  
  public void setMyFavProds(final String myFavProds) {
    this.myFavProds = myFavProds;
  }
  
  public void setDisplayProductList(final List<NMProduct> displayProductList) {
    this.displayProductList = displayProductList;
  }
  
  public List<NMProduct> getDisplayProductList() {
    return displayProductList;
  }
  
  public void setProductPageMap(final Map<String, NMProduct> productPageMap) {
    this.productPageMap = productPageMap;
  }
  
  public Map<String, NMProduct> getProductPageMap() {
    return productPageMap;
  }
  
  public void setDisplayYMALProductList(final List<RelatedProductBean> displayYMALProductList) {
    this.displayYMALProductList = displayYMALProductList;
  }
  
  public void setDisplayCTLProductList(final List<RelatedProductBean> displayCTLProductList) {
    this.displayCTLProductList = displayCTLProductList;
  }
  
  public List<RelatedProductBean> getDisplayCTLProductList() {
    return displayCTLProductList;
  }
  
  public NMPagingPrevNext getPagingPrevNext() {
    return pagingPrevNext;
  }
  
  public void setPageDesignerName(final String pageDesignerName) {
    this.pageDesignerName = pageDesignerName;
  }
  
  public void setPagingPrevNext(final NMPagingPrevNext pagingPrevNext) {
    this.pagingPrevNext = pagingPrevNext;
  }
  
  public String getPageDesignerName() {
    return pageDesignerName;
  }
  
  public boolean getIsAllDesignersSame() {
    return isAllDesignersSame;
  }
  
  public void setAllDesignersSame(final boolean isAllDesignersSame) {
    this.isAllDesignersSame = isAllDesignersSame;
  }
  
  public void setIsLongSuperSuite(final Boolean isLongSuperSuite) {
    this.isLongSuperSuite = isLongSuperSuite;
  }
  
  public Boolean getIsLongSuperSuite() {
    return isLongSuperSuite;
  }
  
  public boolean getIsAllShopRunnerEligible() {
    return isAllShopRunnerEligible;
  }
  
  public void setAllShopRunnerEligible(final boolean isAllShopRunnerEligible) {
    this.isAllShopRunnerEligible = isAllShopRunnerEligible;
  }
  
  public void setOverviewTitle(final String overviewTitle) {
    this.overviewTitle = overviewTitle;
  }
  
  public String getOverviewTitle() {
    return overviewTitle;
  }
  
  public void setProductCopyTop(final String productCopyTop) {
    this.productCopyTop = productCopyTop;
  }
  
  public String getProductCopyTop() {
    return productCopyTop;
  }
  
  public void setProductCopyBottom(final String productCopyBottom) {
    this.productCopyBottom = productCopyBottom;
  }
  
  public String getProductCopyBottom() {
    return productCopyBottom;
  }
  
  public void setProductCutline(final String productCutline) {
    this.productCutline = productCutline;
  }
  
  public String getProductCutline() {
    return productCutline;
  }
  
  public void setDesignerContent(final String designerContent) {
    this.designerContent = designerContent;
  }
  
  public String getDesignerContent() {
    return designerContent;
  }
  
  public void setDetailsContent(final String detailsContent) {
    this.detailsContent = detailsContent;
  }
  
  public String getDetailsContent() {
    return detailsContent;
  }
  
  public void setDetailsTitle(final String detailsTitle) {
    this.detailsTitle = detailsTitle;
  }
  
  public String getDetailsTitle() {
    return detailsTitle;
  }
  
  public void setSwatchImageUrl(final String swatchImageUrl) {
    this.swatchImageUrl = swatchImageUrl;
  }
  
  public String getSwatchImageUrl() {
    return swatchImageUrl;
  }
  
  public void setDisplayTabs(final boolean displayTabs) {
    this.displayTabs = displayTabs;
  }
  
  public boolean getDisplayTabs() {
    return displayTabs;
  }
  
  public NMProductPrice getProductPrice() {
    return productPrice;
  }
  
  public void setProductPrice(final NMProductPrice productPrice) {
    this.productPrice = productPrice;
  }
  
  public void setLocaleName(final String localeName) {
    this.localeName = localeName;
  }
  
  public String getLocaleName() {
    return localeName;
  }
  
  public void setLangCode(final String langCode) {
    this.langCode = langCode;
  }
  
  public String getLangCode() {
    return langCode;
  }
  
  public void setProductTranslationRI(final RepositoryItem productTranslationRI) {
    this.productTranslationRI = productTranslationRI;
  }
  
  public RepositoryItem getProductTranslationRI() {
    return productTranslationRI;
  }
  
  public void setDepiction(final String depiction) {
    this.depiction = depiction;
  }
  
  public String getDepiction() {
    return depiction;
  }
  
  public void setSuiteSizeGuide(final String suiteSizeGuide) {
    this.suiteSizeGuide = suiteSizeGuide;
  }
  
  public String getSuiteSizeGuide() {
    return suiteSizeGuide;
  }
  
  public String getNeedHelpLayout() {
    return needHelpLayout;
  }
  
  public void setNeedHelpLayout(final String needHelpLayout) {
    this.needHelpLayout = needHelpLayout;
  }
  
  public RichRelevanceProductBean getYmalProducts() {
    return ymalProducts;
  }
  
  public void setYmalProducts(final RichRelevanceProductBean ymalProducts) {
    this.ymalProducts = ymalProducts;
  }
  
  public boolean isServerSideYmal() {
    return serverSideYmal;
  }
  
  public void setServerSideYmal(final boolean serverSideYmal) {
    this.serverSideYmal = serverSideYmal;
  }
  
  public List<RelatedProductBean> getDisplayMoreColorsList() {
    return displayMoreColorsList;
  }
  
  public void setDisplayMoreColorsList(final List<RelatedProductBean> displayMoreColorsList) {
    this.displayMoreColorsList = displayMoreColorsList;
  }
  
  /**
   * @return the shippingAndDeliveryPolicyCatId
   */
  public String getShippingAndDeliveryPolicyCatId() {
    return shippingAndDeliveryPolicyCatId;
  }
  
  /**
   * @param shippingAndDeliveryPolicyCatId
   *          the shippingAndDeliveryPolicyCatId to set
   */
  public void setShippingAndDeliveryPolicyCatId(final String shippingAndDeliveryPolicyCatId) {
    this.shippingAndDeliveryPolicyCatId = shippingAndDeliveryPolicyCatId;
  }
  
  public Map<String, List<SkuColorData>> getProductColorSwatchData() {
    return productColorSwatchData;
  }
  
  public void setProductColorSwatchData(final Map<String, List<SkuColorData>> productColorSwatchData) {
    this.productColorSwatchData = productColorSwatchData;
  }
  
  public Boolean getSddEligible() {
    return sddEligible;
  }
  
  public void setSddEligible(final Boolean sddEligbile) {
    this.sddEligible = sddEligbile;
  }
  
  public String getSddDefaultLocation() {
    return sddDefaultLocation;
  }
  
  public void setSddDefaultLocation(final String defaultLocation) {
    this.sddDefaultLocation = defaultLocation;
  }
  
  public String getMyFavItems() {
    return myFavItems;
  }
  
  public void setMyFavItems(final String myFavItems) {
    this.myFavItems = myFavItems;
  }
  
  public String getStoreSkuInventoriesJSON() {
    return storeSkuInventoriesJSON;
  }
  
  public void setStoreSkuInventoriesJSON(final String storeSkuInventoriesJSON) {
    this.storeSkuInventoriesJSON = storeSkuInventoriesJSON;
  }
  
  public void setFireSddOmnitureOnPageLoad(final Boolean isFireSddOmnitureOnPageLoad) {
    this.isFireSddOmnitureOnPageLoad = isFireSddOmnitureOnPageLoad;
  }
  
  public Boolean isFireSddOmnitureOnPageLoad() {
    return isFireSddOmnitureOnPageLoad;
  }
  
  public Boolean getMonogarammable() {
    return monogarammable;
  }
  
  public void setMonogarammable(final Boolean monogarammable) {
    this.monogarammable = monogarammable;
  }
  
  /**
   * @return the configurableProduct
   */
  public Boolean getConfigurableProduct() {
    if (null == configurableProduct) {
      configurableProduct = false;
    }
    return configurableProduct;
  }
  
  /**
   * @param configurableProduct
   *          the configurableProduct to set
   */
  public void setConfigurableProduct(Boolean configurableProduct) {
    this.configurableProduct = configurableProduct;
  }
  
  public String getProdSelectBoxPath() {
    return prodSelectBoxPath;
  }
  
  public void setProdSelectBoxPath(String prodSelectBoxPath) {
    this.prodSelectBoxPath = prodSelectBoxPath;
  }
  
  public String getRestrictedPrdsToRR() {
    return restrictedPrdsToRR;
  }
  
  public void setRestrictedPrdsToRR(String restrictedPrdsToRR) {
    this.restrictedPrdsToRR = restrictedPrdsToRR;
  }
  
  public Boolean getIsItemEligibleForDiscount() {
    return isItemEligibleForDiscount;
  }
  
  public void setIsItemEligibleForDiscount(Boolean isItemEligibleForDiscount) {
    this.isItemEligibleForDiscount = isItemEligibleForDiscount;
  }
  
  public Boolean getProdHasCTLItems() {
    return prodHasCTLItems;
  }
  
  public void setProdHasCTLItems(Boolean prodHasCTLItems) {
    this.prodHasCTLItems = prodHasCTLItems;
  }
  
  /**
   * Gets the display suite items with single sku.
   * 
   * @return the displaySuiteItemsWithSingleSku
   */
  public List<NMProduct> getDisplaySuiteItemsWithSingleSku() {
    return displaySuiteItemsWithSingleSku;
  }
  
  /**
   * Sets the display suite items with single sku.
   * 
   * @param displaySuiteItemsWithSingleSku
   *          the displaySuiteItemsWithSingleSku to set
   */
  public void setDisplaySuiteItemsWithSingleSku(final List<NMProduct> displaySuiteItemsWithSingleSku) {
    this.displaySuiteItemsWithSingleSku = displaySuiteItemsWithSingleSku;
  }
  
  /**
   * Gets the display cmos skus.
   * 
   * @return the displayCmosSkus
   */
  public List<String> getDisplayCmosSkus() {
    return displayCmosSkus;
  }
  
  /**
   * Gets the display sellable skus.
   * 
   * @return the displaySellableSkus
   */
  public List<Boolean> getDisplaySellableSkus() {
    return displaySellableSkus;
  }
  
  /**
   * Sets the display sellable skus.
   * 
   * @param displaySellableSkus
   *          the displaySellableSkus to set
   */
  public void setDisplaySellableSkus(final List<Boolean> displaySellableSkus) {
    this.displaySellableSkus = displaySellableSkus;
  }
  
  /**
   * Sets the display cmos skus.
   * 
   * @param displayCmosSkus
   *          the displayCmosSkus to set
   */
  public void setDisplayCmosSkus(final List<String> displayCmosSkus) {
    this.displayCmosSkus = displayCmosSkus;
  }
  
  /**
   * @param urlRedirected
   * @return the urlRedirected
   */
  public Boolean getUrlRedirected() {
    
    if (null == urlRedirected) {
      urlRedirected = false;
    }
    return urlRedirected;
  }
  
  /**
   * @param urlRedirected
   *          the urlRedirected to set
   */
  public void setUrlRedirected(Boolean urlRedirected) {
    this.urlRedirected = urlRedirected;
  }
  
  /**
   * @param productExpectedAvailability
   *          the productExpectedAvailability to set
   */
  public void setProductExpectedAvailability(List<String> productExpectedAvailability) {
    this.productExpectedAvailability = productExpectedAvailability;
  }
  
  /**
   * @return the showableList
   */
  public List<Boolean> getShowableList() {
    return showableList;
  }
  
  /**
   * @param showableList
   *          the showableList to set
   */
  public void setShowableList(List<Boolean> showableList) {
    this.showableList = showableList;
  }
  
  /**
   * @return the unSellableSku
   */
  public boolean isUnSellableSku() {
    return unSellableSku;
  }
  
  /**
   * @param unSellableSku
   *          the unSellableSku to set
   */
  public void setUnSellableSku(boolean unSellableSku) {
    this.unSellableSku = unSellableSku;
  }
  /**
   * @return the videoOnPage
   */
  public boolean isVideoOnPage() {
    return videoOnPage;
  }
  
  public String getFocusProductId() {
    return focusProductId;
  }
  
  public void setFocusProductId(String focusProductId) {
    this.focusProductId = focusProductId;
  }
  
  /**
   * @return the trackerBreadCrumbs
   */
  public List<String> getTrackerBreadCrumbs() {
	return trackerBreadCrumbs;
  }

  /**
   * @param trackerBreadCrumbs the trackerBreadCrumbs to set
   */
  public void setTrackerBreadCrumbs(List<String> trackerBreadCrumbs) {
	this.trackerBreadCrumbs = trackerBreadCrumbs;
  }

  /**
   * @param videoOnPage the videoOnPage to set
   */
  public void setVideoOnPage(boolean videoOnPage) {
    this.videoOnPage = videoOnPage;
  }

  /**
   * @return the displayProductExpectedAvailability
   */
  public List<String> getDisplayProductExpectedAvailability() {
    return displayProductExpectedAvailability;
  }

  /**
   * @param displayProductExpectedAvailability the displayProductExpectedAvailability to set
   */
  public void setDisplayProductExpectedAvailability(List<String> displayProductExpectedAvailability) {
    this.displayProductExpectedAvailability = displayProductExpectedAvailability;
  }

  /**
   * @return the displayShowableSkus
   */
  public List<Boolean> getDisplayShowableSkus() {
    return displayShowableSkus;
  }

  /**
   * @param displayShowableSkus the displayShowableSkus to set
   */
  public void setDisplayShowableSkus(List<Boolean> displayShowableSkus) {
    this.displayShowableSkus = displayShowableSkus;
  }

  /**
   * @return the displayInventoryStatus
   */
  public List<String> getDisplayInventoryStatus() {
    return displayInventoryStatus;
  }

  /**
   * @param displayInventoryStatus the displayInventoryStatus to set
   */
  public void setDisplayInventoryStatus(List<String> displayInventoryStatus) {
    this.displayInventoryStatus = displayInventoryStatus;
  }

  /**
   * @return the displayStockLevel
   */
  public List<String> getDisplayStockLevel() {
    return displayStockLevel;
  }

  /**
   * @param displayStockLevel the displayStockLevel to set
   */
  public void setDisplayStockLevel(List<String> displayStockLevel) {
    this.displayStockLevel = displayStockLevel;
  }

  public boolean isEditPage() {
	return editPage;
  }

  public void setEditPage(boolean editPage) {
	this.editPage = editPage;
  }

}
