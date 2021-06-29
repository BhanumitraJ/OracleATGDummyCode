package com.nm.commerce.pagedef.model;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import atg.repository.RepositoryItem;

import com.nm.catalog.navigation.Breadcrumb;
import com.nm.catalog.navigation.NMCategory;
import com.nm.commerce.catalog.NMProduct;
import com.nm.commerce.catalog.SuperProduct;
import com.nm.commerce.pagedef.definition.TemplatePageDefinition;
import com.nm.commerce.pagedef.model.bean.SortOption;
import com.nm.search.GenericPagingBean;

/**
 * Model containing variables for use throughout a template page.
 */
public class TemplatePageModel extends PageModel {
  private NMCategory category;
  
  private boolean isFiltered;
  private boolean isPaging;
  private boolean isSuperViewAll;
  private boolean isViewAll;
  private boolean showBackToTop;
  private boolean showGraphicBlock;
  private boolean showPromoTile;
  private Map<String, String> promoTiles = new HashMap<String, String>();
  private boolean isFlashSale;
  private int columnCount;
  private int firstThumbnailIndex;
  private int pageCount;
  private int pageNumber;
  private int thumbnailCount;
  private String sortParam;
  private String thumbnailParentCategoryId;
  private boolean isEmailRequired; // true if a category requires an email for access and the customer has not provided one
  private boolean isSubcatProductCollection;
  private boolean intlBannerOverride;
  private String userSelectedCountry;
  private String featureCategoryBannerFile;
  
  private String staticImageFile;
  private boolean staticImageOverride;
  private String contentHeaderFile;
  private boolean headerImageOverride;
  
  private Breadcrumb breadcrumb;
  private List<NMProduct> myFavNMProducts;
  private List<NMProduct> myFavNMsoldOutProducts;
  private String[] promoTilePositions;
  private Map<SuperProduct, List<SuperProduct>> superProdMap = new LinkedHashMap<SuperProduct, List<SuperProduct>>();
  private Set<String> designerBreadCrumb;
  private Breadcrumb siloBreadcrumb;
  
  /** The filter persist. */
  private boolean filterPersist;
  
  /** The filter persist values. */
  private String filterPersistValues;
  
  /** category's preferred products count */
  private int preferredProducts;

  public Breadcrumb getBreadcrumb() {
    return breadcrumb;
  }
  
  public void setBreadcrumb(final Breadcrumb breadcrumb) {
    this.breadcrumb = breadcrumb;
  }
  
  private String thumbnailMasterCategoryId;
  private String returnParameters;
  private GenericPagingBean pagination;
  private List<SortOption> sorts;
  private SortOption currentSort;
  
  private List<? extends RepositoryItem> displayableProductList;
  private List<? extends RepositoryItem> displayableSubcatList;
  private Map<NMCategory, List<NMCategory>> subCatChildCatList;
  private List<SuperProduct> displayableSuperProductList;
  private List<NMProduct> displayableNMProductList;
  private List<? extends RepositoryItem> displayableFeatureBannerCatList;
  private Map<String, String> asciiDesignerNamePairing;
  
  // the original pageDefinition requested. this is kept to retain properties
  // like the original template ID, used for tracking. for example: if the subsequent
  // (alternate) pages of a P5 are switch to P6, the originalPageDefinition remains P5.
  private TemplatePageDefinition originalPageDefinition;
  
  // Added for enhancing the Fashion dash template to support Endeca Driven Categories
  private List<NMCategory> activeFlashSaleCategories;
  private List<NMCategory> inactiveFlashSaleCategories;
  private NMCategory flashSaleCategory;
  private boolean isFlashSaleActive;
  private boolean displayGraphicHeader2;
  private int graphicHeaderPosition;
  private int graphicHeaderRowPosition = 7;
  
  // My Favorites -MYNM67 - added for my favorites
  private String myFavItems;
  
  // Sale Silo Category Carousel Page Model
  private List<NMCategory> carouselList;
  private String carouselCategoryId;
  private String carouselTemplateId;
  private String carouselSubCategoryId;
  private int carouselShowOrHide;
  private int carouselCategoryChildOrder;
  
  /** The sort method. */
  private String sortMethod;
  
  /** The RR strategy. */
  private String rrStrategy;
  
  /** The RR test id. */
  private String rrTestId;
  
  /** The RR treatment id. */
  private String rrTreatmentId;
  
  /** The RR control. */
  private boolean rrControl;
  
  /** The PCS enabled. */
  private boolean pcsEnabled;
  
  /** The PCS request successful. */
  private boolean pcsRequestSuccessful;
  
  /** The PCS response successful. */
  private boolean pcsResponseSuccessful;
  
  /** The filter type. */
  private List<String> filterType;
  
  /** The filter selections. */
  private List<String> filterSelections;
  
  /** the selected refinements **/
  private List<String> selectedRefinements;
  private String aemResponse;
  
  public NMCategory getCategory() {
    return category;
  }
  
  public void setCategory(final NMCategory category) {
    this.category = category;
  }
  
  public boolean isFiltered() {
    return isFiltered;
  }
  
  public void setFiltered(final boolean isFiltered) {
    this.isFiltered = isFiltered;
  }
  
  public boolean isPaging() {
    return isPaging;
  }
  
  public void setPaging(final boolean isPaging) {
    this.isPaging = isPaging;
  }
  
  public boolean isViewAll() {
    return isViewAll;
  }
  
  public void setViewAll(final boolean isViewAll) {
    this.isViewAll = isViewAll;
  }
  
  public boolean isSuperViewAll() {
    return isSuperViewAll;
  }
  
  public void setSuperViewAll(final boolean isSuperViewAll) {
    this.isSuperViewAll = isSuperViewAll;
  }
  
  public boolean isFlashSale() {
    return isFlashSale;
  }
  
  public void setFlashSale(final boolean isFlashSale) {
    this.isFlashSale = isFlashSale;
  }
  
  public List<? extends RepositoryItem> getDisplayableProductList() {
    return displayableProductList;
  }
  
  public void setDisplayableProductList(final List<? extends RepositoryItem> displayableProductList) {
    this.displayableProductList = displayableProductList;
  }
  
  public List<? extends RepositoryItem> getDisplayableSubcatList() {
    return displayableSubcatList;
  }
  
  public void setDisplayableSubcatList(final List<? extends RepositoryItem> displayableSubcatList) {
    this.displayableSubcatList = displayableSubcatList;
  }
  
  public List<? extends RepositoryItem> getDisplayableFeatureBannerCatList() {
    return displayableFeatureBannerCatList;
  }
  
  public void setDisplayableFeatureBannerCatList(final List<? extends RepositoryItem> displayableFeatureBannerCatList) {
    this.displayableFeatureBannerCatList = displayableFeatureBannerCatList;
  }
  
  public boolean getShowGraphicBlock() {
    return showGraphicBlock;
  }
  
  public void setShowGraphicBlock(final boolean showGraphicBlock) {
    this.showGraphicBlock = showGraphicBlock;
  }
  
  public int getPageCount() {
    return pageCount;
  }
  
  public void setPageCount(final int pageCount) {
    this.pageCount = pageCount;
  }
  
  public int getPageNumber() {
    return pageNumber;
  }
  
  public void setPageNumber(final int pageNumber) {
    this.pageNumber = pageNumber;
  }
  
  public int getFirstThumbnailIndex() {
    return firstThumbnailIndex;
  }
  
  public void setFirstThumbnailIndex(final int firstThumbnailIndex) {
    this.firstThumbnailIndex = firstThumbnailIndex;
  }
  
  public int getThumbnailCount() {
    return thumbnailCount;
  }
  
  public void setThumbnailCount(final int thumbnailCount) {
    this.thumbnailCount = thumbnailCount;
  }
  
  public boolean isEmailRequired() {
    return isEmailRequired;
  }
  
  public void setEmailRequired(final boolean isEmailRequired) {
    this.isEmailRequired = isEmailRequired;
  }
  
  public boolean isSubcatProductCollection() {
    return isSubcatProductCollection;
  }
  
  public void setSubcatProductCollection(final boolean isSubcatProductCollection) {
    this.isSubcatProductCollection = isSubcatProductCollection;
  }
  
  public int getColumnCount() {
    return columnCount;
  }
  
  public void setColumnCount(final int columnCount) {
    this.columnCount = columnCount;
  }
  
  public boolean getShowBackToTop() {
    return showBackToTop;
  }
  
  public void setShowBackToTop(final boolean showBackToTop) {
    this.showBackToTop = showBackToTop;
  }
  
  public TemplatePageDefinition getOriginalPageDefinition() {
    return originalPageDefinition;
  }
  
  public void setOriginalPageDefinition(final TemplatePageDefinition originalPageDefinition) {
    this.originalPageDefinition = originalPageDefinition;
  }
  
  public void setAsciiDesignerNamePairing(final Map<String, String> asciiDesignerNamePairing) {
    this.asciiDesignerNamePairing = asciiDesignerNamePairing;
  }
  
  public Map<String, String> getAsciiDesignerNamePairing() {
    return asciiDesignerNamePairing;
  }
  
  public void setThumbnailParentCategoryId(final String thumbnailParentCategoryId) {
    this.thumbnailParentCategoryId = thumbnailParentCategoryId;
  }
  
  public String getThumbnailParentCategoryId() {
    return thumbnailParentCategoryId;
  }
  
  public void setThumbnailMasterCategoryId(final String thumbnailMasterCategoryId) {
    this.thumbnailMasterCategoryId = thumbnailMasterCategoryId;
  }
  
  public String getThumbnailMasterCategoryId() {
    return thumbnailMasterCategoryId;
  }
  
  public void setDisplayableSuperProductList(final List<SuperProduct> displayableSuperProductList) {
    this.displayableSuperProductList = displayableSuperProductList;
  }
  
  public List<SuperProduct> getDisplayableSuperProductList() {
    return displayableSuperProductList;
  }
  
  public String getReturnParameters() {
    return returnParameters;
  }
  
  public void setReturnParameters(final String returnParameters) {
    this.returnParameters = returnParameters;
  }
  
  /**
   * @return the pagination
   */
  public GenericPagingBean getPagination() {
    return pagination;
  }
  
  /**
   * @param pagination
   *          the pagination to set
   */
  public void setPagination(final GenericPagingBean pagination) {
    this.pagination = pagination;
  }
  
  public void setSortParam(final String value) {
    sortParam = value;
  }
  
  public String getSortParam() {
    return sortParam;
  }
  
  public void setSorts(final List<SortOption> value) {
    sorts = value;
  }
  
  public List<SortOption> getSorts() {
    return sorts;
  }
  
  public void setCurrentSort(final SortOption value) {
    currentSort = value;
  }
  
  public SortOption getCurrentSort() {
    return currentSort;
  }
  
  public boolean isIntlBannerOverride() {
    return intlBannerOverride;
  }
  
  public void setIntlBannerOverride(final boolean intlBannerOverride) {
    this.intlBannerOverride = intlBannerOverride;
  }
  
  public String getUserSelectedCountry() {
    return userSelectedCountry;
  }
  
  public void setUserSelectedCountry(final String userSelectedCountry) {
    this.userSelectedCountry = userSelectedCountry;
  }
  
  public String getFeatureCategoryBannerFile() {
    return featureCategoryBannerFile;
  }
  
  public void setFeatureCategoryBannerFile(final String featureCategoryBannerFile) {
    this.featureCategoryBannerFile = featureCategoryBannerFile;
  }
  
  public String getStaticImageFile() {
    return staticImageFile;
  }
  
  public void setStaticImageFile(final String staticImageFile) {
    this.staticImageFile = staticImageFile;
  }
  
  public boolean isStaticImageOverride() {
    return staticImageOverride;
  }
  
  public void setStaticImageOverride(final boolean staticImageOverride) {
    this.staticImageOverride = staticImageOverride;
  }
  
  public String getContentHeaderFile() {
    return contentHeaderFile;
  }
  
  public void setContentHeaderFile(final String contentHeaderFile) {
    this.contentHeaderFile = contentHeaderFile;
  }
  
  public boolean isHeaderImageOverride() {
    return headerImageOverride;
  }
  
  public void setHeaderImageOverride(final boolean headerImageOverride) {
    this.headerImageOverride = headerImageOverride;
  }
  
  /**
   * @return the activeFlashSaleCategories
   */
  public List<NMCategory> getActiveFlashSaleCategories() {
    return activeFlashSaleCategories;
  }
  
  /**
   * @param activeFlashSaleCategories
   *          the activeFlashSaleCategories to set
   */
  public void setActiveFlashSaleCategories(final List<NMCategory> activeFlashSaleCategories) {
    this.activeFlashSaleCategories = activeFlashSaleCategories;
  }
  
  /**
   * @return the inactiveFlashSaleCategories
   */
  public List<NMCategory> getInactiveFlashSaleCategories() {
    return inactiveFlashSaleCategories;
  }
  
  /**
   * @param inactiveFlashSaleCategories
   *          the inactiveFlashSaleCategories to set
   */
  public void setInactiveFlashSaleCategories(final List<NMCategory> inactiveFlashSaleCategories) {
    this.inactiveFlashSaleCategories = inactiveFlashSaleCategories;
  }
  
  /**
   * @return the flashSaleCategory
   */
  public NMCategory getFlashSaleCategory() {
    return flashSaleCategory;
  }
  
  /**
   * @param flashSaleCategory
   *          the flashSaleCategory to set
   */
  public void setFlashSaleCategory(final NMCategory flashSaleCategory) {
    this.flashSaleCategory = flashSaleCategory;
  }
  
  /**
   * @return the isFlashSaleActive
   */
  public boolean isFlashSaleActive() {
    return isFlashSaleActive;
  }
  
  /**
   * @param isFlashSaleActive
   *          the isFlashSaleActive to set
   */
  public void setFlashSaleActive(final boolean isFlashSaleActive) {
    this.isFlashSaleActive = isFlashSaleActive;
  }
  
  public List<NMProduct> getDisplayableNMProductList() {
    return displayableNMProductList;
  }
  
  public void setDisplayableNMProductList(final List<NMProduct> displayableNMProductList) {
    this.displayableNMProductList = displayableNMProductList;
  }
  
  public String getMyFavItems() {
    return myFavItems;
  }
  
  public void setMyFavItems(final String myFavItems) {
    this.myFavItems = myFavItems;
  }
  
  public List<NMProduct> getMyFavNMProducts() {
    return myFavNMProducts;
  }
  
  public void setMyFavNMProducts(final List<NMProduct> myFavNMProducts) {
    this.myFavNMProducts = myFavNMProducts;
  }
  
  public List<NMProduct> getMyFavNMsoldOutProducts() {
    return myFavNMsoldOutProducts;
  }
  
  public void setMyFavNMsoldOutProducts(final List<NMProduct> myFavNMsoldOutProducts) {
    this.myFavNMsoldOutProducts = myFavNMsoldOutProducts;
  }
  
  public Map<NMCategory, List<NMCategory>> getSubCatChildCatList() {
    return subCatChildCatList;
  }
  
  public void setSubCatChildCatList(final Map<NMCategory, List<NMCategory>> subCatChildCatList) {
    this.subCatChildCatList = subCatChildCatList;
  }
  
  public boolean isShowPromoTile() {
    return showPromoTile;
  }
  
  public void setShowPromoTile(final boolean showPromoTile) {
    this.showPromoTile = showPromoTile;
  }
  
  public Map<String, String> getPromoTiles() {
    return promoTiles;
  }
  
  public void setPromoTiles(final Map<String, String> promoTiles) {
    this.promoTiles = promoTiles;
  }
  
  public boolean isDisplayGraphicHeader2() {
    return displayGraphicHeader2;
  }
  
  public void setDisplayGraphicHeader2(final boolean displayGraphicHeader2) {
    this.displayGraphicHeader2 = displayGraphicHeader2;
  }
  
  public String[] getPromoTilePositions() {
    return promoTilePositions;
  }
  
  public void setPromoTilePositions(final String[] promoTilePositions) {
    this.promoTilePositions = promoTilePositions;
  }
  
  public int getGraphicHeaderPosition() {
    return graphicHeaderPosition;
  }
  
  public void setGraphicHeaderPosition(final int graphicHeaderPosition) {
    this.graphicHeaderPosition = graphicHeaderPosition;
  }
  
  public int getGraphicHeaderRowPosition() {
    return graphicHeaderRowPosition;
  }
  
  public void setGraphicHeaderRowPosition(final int graphicHeaderRowPosition) {
    this.graphicHeaderRowPosition = graphicHeaderRowPosition;
  }
  
  public List<NMCategory> getCarouselList() {
    return carouselList;
  }
  
  public void setCarouselList(final List<NMCategory> carouselList) {
    this.carouselList = carouselList;
  }
  
  public String getCarouselCategoryId() {
    return carouselCategoryId;
  }
  
  public void setCarouselCategoryId(final String carouselCategoryId) {
    this.carouselCategoryId = carouselCategoryId;
  }
  
  public String getCarouselTemplateId() {
    return carouselTemplateId;
  }
  
  public void setCarouselTemplateId(final String carouselTemplateId) {
    this.carouselTemplateId = carouselTemplateId;
  }
  
  public String getCarouselSubCategoryId() {
    return carouselSubCategoryId;
  }
  
  public void setCarouselSubCategoryId(final String carouselSubCategoryId) {
    this.carouselSubCategoryId = carouselSubCategoryId;
  }
  
  public int getCarouselShowOrHide() {
    return carouselShowOrHide;
  }
  
  public void setCarouselShowOrHide(final int carouselShowOrHide) {
    this.carouselShowOrHide = carouselShowOrHide;
  }
  
  public int getCarouselCategoryChildOrder() {
    return carouselCategoryChildOrder;
  }
  
  public void setCarouselCategoryChildOrder(final int carouselCategoryChildOrder) {
    this.carouselCategoryChildOrder = carouselCategoryChildOrder;
  }
  
  public Map<SuperProduct, List<SuperProduct>> getSuperProdMap() {
    return superProdMap;
  }
  
  public void setSuperProdMap(final Map<SuperProduct, List<SuperProduct>> superProdMap) {
    this.superProdMap = superProdMap;
  }
  
  /**
   * Gets the sort method.
   * 
   * @return the sort method
   */
  public String getSortMethod() {
    return sortMethod;
  }
  
  /**
   * Sets the sort method.
   * 
   * @param sortMethod
   *          the new sort method
   */
  public void setSortMethod(final String sortMethod) {
    this.sortMethod = sortMethod;
  }

  /**
   * Gets the RR strategy.
   * 
   * @return the RR strategy
   */
  public String getRrStrategy() {
    return rrStrategy;
  }
  
  /**
   * Sets the RR strategy.
   * 
   * @param rrStrategy
   *          the new RR strategy
   */
  public void setRrStrategy(final String rrStrategy) {
    this.rrStrategy = rrStrategy;
  }

  /**
   * Gets the RR test id.
   * 
   * @return the RR test id
   */
  public String getRrTestId() {
    return rrTestId;
  }
  
  /**
   * Sets the RR test id.
   * 
   * @param rrTestId
   *          the new RR test id
   */
  public void setRrTestId(final String rrTestId) {
    this.rrTestId = rrTestId;
  }
  
  /**
   * Gets the RR treatment id.
   * 
   * @return the RR treatment id
   */
  public String getRrTreatmentId() {
    return rrTreatmentId;
  }
  
  /**
   * Sets the RR treatment id.
   * 
   * @param rrTreatmentId
   *          the new RR treatment id
   */
  public void setRrTreatmentId(final String rrTreatmentId) {
    this.rrTreatmentId = rrTreatmentId;
  }
  
  /**
   * Checks if is RR control.
   * 
   * @return true, if is RR control
   */
  public boolean isRrControl() {
    return rrControl;
  }
  
  /**
   * Sets the RR control.
   * 
   * @param rrControl
   *          the new RR control
   */
  public void setRrControl(final boolean rrControl) {
    this.rrControl = rrControl;
  }

  /**
   * Checks if is PCS enabled.
   * 
   * @return true, if is PCS enabled
   */
  public boolean isPcsEnabled() {
    return pcsEnabled;
  }
  
  /**
   * Sets the PCS enabled.
   * 
   * @param pcsEnabled
   *          the new PCS enabled
   */
  public void setPcsEnabled(final boolean pcsEnabled) {
    this.pcsEnabled = pcsEnabled;
  }
  
  /**
   * Checks if is PCS request successful.
   * 
   * @return true, if is PCS request successful
   */
  public boolean isPcsRequestSuccessful() {
    return pcsRequestSuccessful;
  }
  
  /**
   * Sets the PCS request successful.
   * 
   * @param pcsRequestSuccessful
   *          the new PCS request successful
   */
  public void setPcsRequestSuccessful(final boolean pcsRequestSuccessful) {
    this.pcsRequestSuccessful = pcsRequestSuccessful;
  }
  
  /**
   * Checks if is PCS response successful.
   * 
   * @return true, if is PCS response successful
   */
  public boolean isPcsResponseSuccessful() {
    return pcsResponseSuccessful;
  }
  
  /**
   * Sets the PCS response successful.
   * 
   * @param pcsResponseSuccessful
   *          the new PCS response successful
   */
  public void setPcsResponseSuccessful(final boolean pcsResponseSuccessful) {
    this.pcsResponseSuccessful = pcsResponseSuccessful;
  }

  /**
   * Gets the filter type.
   * 
   * @return the filter type
   */
  public List<String> getFilterType() {
    return filterType;
  }
  
  /**
   * Sets the filter type.
   * 
   * @param filterType
   *          the new filter type
   */
  public void setFilterType(final List<String> filterType) {
    this.filterType = filterType;
  }
  
  /**
   * Gets the filter selections.
   * 
   * @return the filter selections
   */
  public List<String> getFilterSelections() {
    return filterSelections;
  }
  
  /**
   * Sets the filter selections.
   * 
   * @param filterSelections
   *          the new filter selections
   */
  public void setFilterSelections(final List<String> filterSelections) {
    this.filterSelections = filterSelections;
  }
  
  /**
   * @return the selectedRefinements
   */
  public List<String> getSelectedRefinements() {
    return selectedRefinements;
  }

  /**
   * @param selectedRefinements
   *          the selectedRefinements to set
   */
  public void setSelectedRefinements(final List<String> selectedRefinements) {
    this.selectedRefinements = selectedRefinements;
  }
  
	public String getAemResponse() {
		return aemResponse;
	}

  public void setAemResponse(final String aemResponse) {
		this.aemResponse = aemResponse;
	}

  /**
   * @return the designerBreadCrumb
   */
  public Set<String> getDesignerBreadCrumb() {
    return designerBreadCrumb;
  }

  /**
   * @param designerBreadCrumb
   *          the designerBreadCrumb to set
   */
  public void setDesignerBreadCrumb(final Set<String> designerBreadCrumb) {
    this.designerBreadCrumb = designerBreadCrumb;
  }

/**
 * @return the siloBreadcrumb
 */
public Breadcrumb getSiloBreadcrumb() {
	return siloBreadcrumb;
}

/**
 * @param siloBreadcrumb the siloBreadcrumb to set
 */
public void setSiloBreadcrumb(final Breadcrumb siloBreadcrumb) {
	this.siloBreadcrumb = siloBreadcrumb;
}
  /**
   * Checks if is filter persist.
   * 
   * @return true, if is filter persist
   */
  public boolean isFilterPersist() {
    return filterPersist;
  }
  
  /**
   * Sets the filter persist.
   * 
   * @param filterPersist
   *          the new filter persist
   */
  public void setFilterPersist(final boolean filterPersist) {
    this.filterPersist = filterPersist;
  }
  
  /**
   * Gets the filter persist values.
   * 
   * @return the filter persist values
   */
  public String getFilterPersistValues() {
    return filterPersistValues;
  }
  
  /**
   * Sets the filter persist values.
   * 
   * @param filterPersistValues
   *          the filter persist values
   */
  public void setFilterPersistValues(final String filterPersistValues) {
    this.filterPersistValues = filterPersistValues;
  }
  /**
   * @return the preferredProducts
   */
  public int getPreferredProducts() {
  	return preferredProducts;
  }

  /**
   * @param preferredProducts the preferredProducts to set
   */
  public void setPreferredProducts(int preferredProducts) {
  	this.preferredProducts = preferredProducts;
  }
}
