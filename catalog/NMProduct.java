package com.nm.commerce.catalog;

import static com.nm.common.ProductCatalogRepositoryConstants.CMOS_CLASS;
import static com.nm.common.ProductCatalogRepositoryConstants.CMOS_SUB_CLASS;
import static com.nm.common.ProductCatalogRepositoryConstants.CMOS_SUPER_CLASS;
import static com.nm.common.ProductCatalogRepositoryConstants.CM_VARIATION_2;
import static com.nm.common.ProductCatalogRepositoryConstants.EDISON_PROPORTION_CODE;
import static com.nm.common.ProductCatalogRepositoryConstants.EDISON_PROPORTION_DESC;
import static com.nm.common.ProductCatalogRepositoryConstants.GENDER_CODE;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IllegalFormatConversionException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.util.CollectionUtils;

import atg.adapter.gsa.GSAItem;
import atg.core.util.StringUtils;
import atg.nucleus.Nucleus;
import atg.nucleus.logging.ApplicationLogging;
import atg.nucleus.logging.ClassLoggingFactory;
import atg.repository.MutableRepositoryItem;
import atg.repository.RemovedItemException;
import atg.repository.Repository;
import atg.repository.RepositoryItem;
import atg.servlet.DynamoHttpServletRequest;
import atg.servlet.ServletUtil;

import com.nm.abtest.AbTestHelper;
import com.nm.collections.Country;
import com.nm.collections.CountryArray;
import com.nm.commerce.GiftCardHolder;
import com.nm.commerce.NMProfile;
import com.nm.commerce.catalog.helper.ProductCutlineHelper;
import com.nm.commerce.catalog.helper.ProductMatrixJSHelper;
import com.nm.common.INMGenericConstants;
import com.nm.components.CommonComponentHelper;
import com.nm.configurator.utils.ConfiguratorConstants;
import com.nm.integration.ShopRunnerConstants;
import com.nm.monogram.utils.MonogramConstants;
import com.nm.servlet.filter.RedirectFilter;
import com.nm.utils.BrandSpecs;
import com.nm.utils.GenericLogger;
import com.nm.utils.LocalizationUtils;
import com.nm.utils.NmoUtils;
import com.nm.utils.ProdSkuUtil;
import com.nm.utils.StringUtilities;

public class NMProduct {
  public static final int TYPE = 0;
  public static final String PRICE_UNAVAILABLE_STRING = "Product Unavailable";
  
  // type of variations
  public static final int QTY_BOX_ONLY = 0;
  public static final int QTY_BOX_COLOR = 1;
  public static final int QTY_BOX_SIZE = 2;
  public static final int QTY_BOX_COLOR_SIZE = 3;
  public static final int CAT_SETUP_PROBLEM = 4;
  private Map<String, NMProductLocalizedPrice> localizedPrices = null;
  private final Boolean hasLocalizedPrice = null;
  private Boolean showFavIcon;
  
  private List<NMSku> skuList;
  private List<NMSku> sellableSkuList;
  private List<NMProduct> relatedProductList;
  private List<NMProduct> relatedProductList2;
  private List<NMSku> multiColorSellableSkuList;
  private List<NMSku> hexColorSellableSkuList;
  protected List<NMProduct> allProductsList; // product + related + etc...
  private List<Date> vendorRestrictedDateList;
  private List<NMProduct> productList;
  private List<NMProduct> unavailableProductList;
  private Map<String, NMProdCountry> countryMap;
  private RepositoryItem[] parentSuiteRepositoryItems;
  private Set<RepositoryItem> relatedConfigProducts;
  private List<NMProduct> suiteProductsList;
  // depending on suite implementation for related products these
  // may not be needed, but the currently implementation needs this
  // to display correctly for suites set as related products
  private boolean isFirstShowableProductInSuite;
  private boolean isLastShowableProductInSuite;
  private boolean isOnlyProductInSuite;
  private String parentSuiteId;
  private String moreInfoText;
  private String moreInfoLink;
  private Integer sellableVariation1Count;
  private Integer sellableVariation2Count;
  private Double configurationSetMaxRetail;
  private Boolean isShowable;
  protected Boolean isShopRunnerEligible;
  private Boolean isSomeShopRunnerEligible;
  private boolean isPriorityCodeVariation1;
  private boolean isRelatedProduct;
  private boolean isRelatedProduct2;
  private boolean isZeroPriceShowable;
  private boolean isStatusShowable;
  private boolean isNonZeroPriceShowable;
  private RepositoryItem dataSource;
  private ProdSkuUtil prodSkuUtil;
  private CountryArray countryArray;
  private int productNumber; // number used when generating the matrix
  // Added by INSSK for Request # 28731 on May 12, 2011 - Starts here
  protected int prodStatus;
  // Added by INSSK for Request # 28731 on May 12, 2011 - Ends here
  private boolean employeeDiscountEligible = false;
  
  private String endecaDerivedParentSuiteId;
  
  private Repository mcustomCatalogAuxRepository = null;
  
  private String betaSizeGuide = "";
  
  private final ProductImageUrlMap mImageUrlMap = new ProductImageUrlMap(this);
  private final ProductImageUrlMap mImageOrShimUrlMap = new ProductImageUrlMap(this, true);
  private final ProductImagePresentMap mImagePresentMap = new ProductImagePresentMap(this);
  private List<NMProduct> csProdRecommendationsList;
  private String retailPrice;
  
  private String currencyCode = null;
  
  private boolean isSellableItem;
  
  private boolean overrideImage;
  
  private boolean includeRetailMaxPrice;
  
  private boolean includeRetailMinPrice;
  
  private Boolean outOfStock;
  
  private String productPageId;
  
  private String displayName;
  
  private String webDesignerName;
  
  private String cmosCatalogItem;
  
  private String cmosItemCode;
  
  private String cmosCatalogId;
  
  /** The gender code */
  private String genderCode;
  
  /** The edison proportion code */
  private String edisonProportionCode;
  
  /** The cmos class */
  private String cmosClass;
  
  /** The cmos sub class */
  private String cmosSubClass;
  
  /** The cmos super class */
  private String cmosSuperClass;

  public static final String AUTOCTL_ABTEST = "AutoCTLABTest";
  public static final String AUTOCTL_ABTEST_INACTIVE = "inactive";
  
  /** The product available. */
  private List<String> productAvailable;
  
  /** The edison proportion description */
  private String edisonProportionDesc;
  
  /** The mlogging. */
  private final ApplicationLogging mLogging = ClassLoggingFactory.getFactory().getLoggerForClass(NMProduct.class);
  
  /**
   * @param price
   *          to get the product price without '$' and ','
   */
  public String getFormattedPrice() {
    try {
      final Double price = getPrice();
      if (price != null) {
        return String.format(INMGenericConstants.TWO_DIGIT_FORMAT, price);
      } else {
        return null;
      }
    } catch (final IllegalFormatConversionException e) {
      return null;
    }
  }
  
  public String getCurrencyCode() {
    if (currencyCode == null) {
      final DynamoHttpServletRequest request = ServletUtil.getCurrentRequest();
      if (request != null) {
        final NMProfile nmProfile = (NMProfile) request.resolveName("/atg/userprofiling/Profile");
        currencyCode = nmProfile.getCurrencyPreference();
      }
    }
    return currencyCode;
  }
  
  public void setCurrencyCode(final String currencyCode) {
    this.currencyCode = currencyCode;
  }
  
  private NMProductDisplayPrice productDisplayPrice = null;
  
  public NMProductDisplayPrice getProductDisplayPrice() {
    if (productDisplayPrice == null) {
      final LocalizationUtils utils = (LocalizationUtils) Nucleus.getGlobalNucleus().resolveName("/nm/utils/LocalizationUtils");
      productDisplayPrice = utils.populateProductDisplayPrice(this);
    }
    return productDisplayPrice;
  }
  
  public NMProductDisplayPrice populateProductDisplayPriceForSaleCategory() {
    if (productDisplayPrice == null) {
      final LocalizationUtils utils = (LocalizationUtils) Nucleus.getGlobalNucleus().resolveName("/nm/utils/LocalizationUtils");
      productDisplayPrice = utils.populateProductDisplayPrice(this, true);
    }
    return productDisplayPrice;
  }
  
  public String getRetailPrice() {
    if (this.retailPrice != null && !isIncludeRetailMaxPrice() && !isIncludeRetailMinPrice()) {
      return this.retailPrice;
    } else {
      final LocalizationUtils utils = (LocalizationUtils) Nucleus.getGlobalNucleus().resolveName("/nm/utils/LocalizationUtils");
      this.retailPrice = utils.getProductRetailPrice(this);
    }
    return this.retailPrice;
  }
  
  
  public String getRetailPrice(boolean ignoreLocalizedPricing) {
      
      if(ignoreLocalizedPricing) {
          return getPrice().toString();
      }
      
      return getRetailPrice();
  }
  
  
  public void setRetailPrice(final String retailPrice) {
    this.retailPrice = retailPrice;
  }
  
  /**
   * get localized price map for all countries that have localized price map key is country code map value is a NMProductLocalizedPrice (containing price value and price currency)
   * 
   * @return
   */
  @SuppressWarnings("unchecked")
  public Map<String, NMProductLocalizedPrice> getLocalizedPrices() {
    if (isIncludeRetailMaxPrice() || isIncludeRetailMinPrice())
      localizedPrices = null;
    if (localizedPrices == null) {
      localizedPrices = new HashMap<String, NMProductLocalizedPrice>();
      final Map<String, Object> localizedPriceMap = (Map<String, Object>) getPropertyValue("localizedPriceMap");
      if (localizedPriceMap != null && !localizedPriceMap.isEmpty()) {
        final Set<Map.Entry<String, Object>> localizedPriceMapEntrySet = localizedPriceMap.entrySet();
        NMProductLocalizedPrice localPriceItem = null;
        RepositoryItem priceItem = null;
        Double localPrice = null;
        String countryCode;
        String currencyCode;
        for (final Map.Entry<String, Object> entry : localizedPriceMapEntrySet) {
          countryCode = entry.getKey();
          priceItem = (RepositoryItem) entry.getValue();
          localPrice = getPrice(priceItem);
          currencyCode = (String) priceItem.getPropertyValue("currencyCode");
          if (currencyCode != null && !"".equals(currencyCode) && localPrice != null) {
            localPriceItem = new NMProductLocalizedPrice();
            localPriceItem.setCurrencyCode(currencyCode);
            localPriceItem.setPriceValue(localPrice);
            localizedPrices.put(countryCode, localPriceItem);
          }
        }
      }
    }
    return localizedPrices;
  }
  
  /**
   * Name of Method: NMProduct
   * 
   * @param RepositoryItem
   *          repositoryItem( type product )
   * 
   *          Initializes the object
   */
  public NMProduct(final RepositoryItem repositoryItem) {
    initialize(repositoryItem);
  }
  
  /**
   * Name of Method: NMProduct
   * 
   * @param String
   *          productId
   * 
   *          Initializes the object
   */
  public NMProduct(final String productId) {
    initialize(getProdSkuUtil().getProductRepositoryItem(productId));
  }
  
  /**
   * Name of Method: initialize
   * 
   * @param RepositoryItem
   *          repositoryItem
   * 
   *          common function used by the constructors to initialize
   */
  private void initialize(final RepositoryItem repositoryItem) {
    dataSource = null;
    prodSkuUtil = null;
    setDataSource(repositoryItem);
  }
  
  /**
   * Name of Method: refreshSubItems
   * 
   * This refreshes all the cached lists inside this object this would probably be called if the datasource changes
   */
  public void refreshSubItems() {
    setSellableSkuList(null);
    setRelatedProductList(null);
    setRelatedProductList2(null);
    setMultiColorSellableSkuList(null);
    setHexColorSellableSkuList(null);
    setVendorRestrictedDateList(null);
    sellableVariation1Count = null;
    sellableVariation2Count = null;
    isShowable = null;
    isShopRunnerEligible = null;
    isSomeShopRunnerEligible = null;
    productList = null;
    unavailableProductList = null;
    setIsPriorityCodeVariation1(false);
    setIsFirstShowableProductInSuite(false);
    setIsRelatedProduct(false);
    setIsRelatedProduct2(false);
    setParentSuiteId("");
    setProductNumber(9999);
    setIsZeroPriceShowable(false);
    setIsNonZeroPriceShowable(true);
    setIsStatusShowable(true);
    setSuggReplenishmentInterval("");
    
    /*
     * This block of code retrieves the retail price from repositoryItem and localizes it to the Shoppers currency Set in nmProfile. This converted value will be used by the Jsps instead of the retail
     * price in repositoryItem
     */
    final DynamoHttpServletRequest request = ServletUtil.getCurrentRequest();
    
    if (request != null) {
      final NMProfile nmProfile = (NMProfile) request.resolveName("/atg/userprofiling/Profile");
      
      if (nmProfile != null) {
        final LocalizationUtils utils = (LocalizationUtils) Nucleus.getGlobalNucleus().resolveName("/nm/utils/LocalizationUtils");
        currencyCode = nmProfile.getCurrencyPreference();
      }
    }
  }
  
  /**
   * Suite Id derived from an Endeca Search record for this product.
   * 
   * @return String
   */
  public String getEndecaDerivedParentSuiteId() {
    return this.endecaDerivedParentSuiteId;
  }
  
  /**
   * Suite Id from derived from an Endeca Search record for this product.
   * 
   * @param value
   */
  public void setEndecaDerivedParentSuiteId(final String value) {
    this.endecaDerivedParentSuiteId = value;
  }
  
  public boolean getHasParentSuiteOnTree() {
    return this.endecaDerivedParentSuiteId != null;
  }
  
  /**
   * Name of Method: getId
   * 
   * @return String -- product Id
   */
  public String getId() {
    return getDataSource().getRepositoryId();
  }
  
  public RepositoryItem getDataSource() {
    return dataSource;
  }
  
  public void setDataSource(final RepositoryItem dataSource) {
    this.dataSource = dataSource;
    if (dataSource != null) {
      refreshSubItems();
    }
  }
  
  /**
   * Name of Method: getProductList
   * 
   * @return List
   * 
   *         returns the list of product contained in this product object
   * 
   */
  public List<NMProduct> getProductList() {
    if (productList == null) {
      productList = new ArrayList<NMProduct>();
      productList.add(this);
    }
    
    return productList;
  }
  
  public List<NMProduct> getUnavailableProductList() {
    if (unavailableProductList == null) {
      unavailableProductList = new ArrayList<NMProduct>();
      
      if (!getIsShowable()) {
        unavailableProductList.add(this);
      }
    }
    return unavailableProductList;
  }
  
  /**
   * Name of Method: getIsPriorityCodeVariation1
   * 
   * @return boolean
   * 
   *         prioritizes the order of variation 1/2 in the matrix If true its color then size If false its size then color
   */
  public boolean getIsPriorityCodeVariation1() {
    if (getVariationType() == QTY_BOX_SIZE) {
      return false;
    } else {
      return isPriorityCodeVariation1;
    }
  }
  
  public void setIsPriorityCodeVariation1(final boolean isPriorityCodeVariation1) {
    this.isPriorityCodeVariation1 = isPriorityCodeVariation1;
  }
  
  /**
   * Name of Method: filterProductList
   * 
   * @param List
   *          -- A list of NMProduct objects
   * 
   *          occassionally products get merchandised incorrectly removes duplicate products from the productList
   */
  public void filterProductList(final List<NMProduct> productList) {
    // lets modify the list instead of regenerating it
    try {
      final Iterator<NMProduct> productListIterator = productList.iterator();
      final Map<String, String> dupCheckMap = new HashMap<String, String>();
      while (productListIterator.hasNext()) {
        final NMProduct nmProduct = productListIterator.next();
        final Object obj = dupCheckMap.get(nmProduct.getId());
        if (obj == null) {
          dupCheckMap.put(nmProduct.getId(), "");
        } else {
          productListIterator.remove(); // duplicate found, remove
          // it
        }
      }
    } catch (final Exception e) {
      e.printStackTrace();
    }
    
    // suite first and last showable flags need to be corrected because of
    // the filter process
    if (relatedProductList == null) {
      relatedProductList = new ArrayList<NMProduct>();
    }
    
    fixSuitesOnRelatedProductList(relatedProductList);
    if (relatedProductList2 == null) {
      relatedProductList2 = new ArrayList<NMProduct>();
    }
    
    fixSuitesOnRelatedProductList(relatedProductList2);
  }
  
  public void fixSuitesOnRelatedProductList(final List<NMProduct> relatedProductList) {
    NMProduct nmProduct = null;
    final int rplSize = relatedProductList.size();
    for (int pos = 0; pos < rplSize; pos++) {
      nmProduct = relatedProductList.get(pos);
      if (nmProduct.getIsInSuite()) {
        if (pos == 0 && pos == rplSize - 1) { // only product in list
          nmProduct.setIsFirstShowableProductInSuite(true);
          nmProduct.setIsLastShowableProductInSuite(true);
        } else if (pos == 0) { // is first product in list
          nmProduct.setIsFirstShowableProductInSuite(true);
          
          if (relatedProductList.get(pos + 1).getIsInSuite()) {
            
            if (!nmProduct.getParentSuiteId().equals(relatedProductList.get(pos + 1).getParentSuiteId())) {
              nmProduct.setIsLastShowableProductInSuite(true);
            }
            
          } else {
            nmProduct.setIsLastShowableProductInSuite(true);
          }
          
        } else if (pos == rplSize - 1) { // is last product in list
        
          nmProduct.setIsLastShowableProductInSuite(true);
          if (relatedProductList.get(pos - 1).getIsInSuite()) {
            if (!nmProduct.getParentSuiteId().equals(relatedProductList.get(pos - 1).getParentSuiteId())) {
              nmProduct.setIsFirstShowableProductInSuite(true);
            }
          } else {
            nmProduct.setIsFirstShowableProductInSuite(true);
          }
          
        } else { // all others
          if (relatedProductList.get(pos - 1).getIsInSuite()) {
            if (!nmProduct.getParentSuiteId().equals(relatedProductList.get(pos - 1).getParentSuiteId())) {
              nmProduct.setIsFirstShowableProductInSuite(true);
            }
          } else {
            nmProduct.setIsFirstShowableProductInSuite(true);
          }
          
          if (relatedProductList.get(pos + 1).getIsInSuite()) {
            if (!nmProduct.getParentSuiteId().equals(relatedProductList.get(pos + 1).getParentSuiteId())) {
              nmProduct.setIsLastShowableProductInSuite(true);
            }
          } else {
            nmProduct.setIsLastShowableProductInSuite(true);
          }
        }
        // if there is only one product left on the suite change the
        // values
        if (nmProduct.getIsFirstShowableProductInSuite() && nmProduct.getIsLastShowableProductInSuite()) {
          nmProduct.setIsFirstShowableProductInSuite(false);
          nmProduct.setIsLastShowableProductInSuite(false);
          nmProduct.setIsOnlyProductInSuite(true);
        }
      }
    }
  }
  
  /**
   * Name of Method: getVendorRestrictedDateList
   * 
   * @return List -- A list of restricted dates
   * 
   */
  public List<Date> getVendorRestrictedDateList() {
    if (vendorRestrictedDateList == null && getSkuList().size() > 0) {
      
      vendorRestrictedDateList = getProdSkuUtil().lookUpDatesByVendor(getSkuList().get(0).getVendorId());
    }
    
    return vendorRestrictedDateList;
  }
  
  public void setVendorRestrictedDateList(final List<Date> vendorRestrictedDateList) {
    this.vendorRestrictedDateList = vendorRestrictedDateList;
  }
  
  public boolean getHasVendorRestrictedDates() {
    if (getVendorRestrictedDateList() != null && getVendorRestrictedDateList().size() > 0) {
      return true;
    } else {
      return false;
    }
  }
  
  /**
   * Name of Method: getAllProductsList
   * 
   * @return List -- A list of NMProducts(this.product + its related products) sellable only
   * 
   */
  public List<NMProduct> getAllProductsList() {
    if (allProductsList == null) {
      allProductsList = new ArrayList<NMProduct>();
      if (getIsShowable()) {
        allProductsList.add(this);
        allProductsList.addAll(getRelatedProductList());
        allProductsList.addAll(getRelatedProductList2());
      }
      
      filterProductList(allProductsList);
    }
    return allProductsList;
  }
  
  public void setAllProductsList(final List<NMProduct> allProductsList) {
    this.allProductsList = allProductsList;
  }
  
  public boolean getAllProductsListPerishables() {
    boolean foundPerisable = false;
    final Iterator<NMProduct> allProductListIterator = getAllProductsList().iterator();
    
    while (allProductListIterator.hasNext()) {
      final NMProduct nmProduct = allProductListIterator.next();
      if (nmProduct.getIsPerishable()) {
        foundPerisable = true;
        break;
      }
    }
    
    return foundPerisable;
  }
  
  /**
   * Name of Method: getIsRelatedProductAvailable
   * 
   * @return boolean
   * 
   */
  public boolean getIsRelatedProductAvailable() {
    final Iterator<NMProduct> relatedProductListIterator = getRelatedProductList().iterator();
    while (relatedProductListIterator.hasNext()) {
      final NMProduct nmProduct = relatedProductListIterator.next();
      if (nmProduct.getIsShowable()) {
        return true;
      }
    }
    
    return false;
  }
  
  public boolean getIsRelatedProduct2Available() {
    final Iterator<NMProduct> relatedProductListIterator = getRelatedProductList2().iterator();
    while (relatedProductListIterator.hasNext()) {
      final NMProduct nmProduct = relatedProductListIterator.next();
      if (nmProduct.getIsShowable()) {
        return true;
      }
    }
    
    return false;
  }
  
  /**
   * process and returns the list of related products of current product as NMProduct
   * 
   * @return List -- a list of NMProducts
   * 
   *         The related products for this product.
   * 
   */
  public List<NMProduct> getRelatedProductList() {
    final BrandSpecs brandSpecs = (BrandSpecs) Nucleus.getGlobalNucleus().resolveName("/nm/utils/BrandSpecs");
    final GenericLogger logger = CommonComponentHelper.getLogger();
    List<RepositoryItem> relatedProductRIList = null;
    final RepositoryItem prodId = getProdSkuUtil().getProductRepositoryItem(getId());
    
    String autoCTLAbTest = AbTestHelper.getAbTestValue(getRequest(), AUTOCTL_ABTEST);
    if (autoCTLAbTest == null) {
      autoCTLAbTest = AUTOCTL_ABTEST_INACTIVE;
    }
    if ((relatedProductList == null) || ((relatedProductList != null) && relatedProductList.isEmpty())) {
      relatedProductList = new ArrayList<NMProduct>();
      // add related products of current product through API
      if ((brandSpecs.isEnableAutomateCTL()) && (autoCTLAbTest.equalsIgnoreCase("active"))) {
        try {
          if (!getProdSkuUtil().isSuite(prodId) && !getProdSkuUtil().isSuperSuite(prodId)) {
            relatedProductRIList = getProdSkuUtil().getCTLAccessoryProducts(getId(), brandSpecs.getBrandShortName());
          }
        } catch (final Exception e) {
          logger.error("Exception occurred while processing the CTL API call for product " + getId() + ":", e);
        }
        
      } else {
        // add suite items first
        final RepositoryItem[] rItems = getParentSuiteRepositoryItems();
        if (rItems != null) {
          final int rItemsLength = rItems.length;
          for (int pos = 0; pos < rItemsLength; pos++) {
            final RepositoryItem rItem = rItems[pos];
            
            @SuppressWarnings("unchecked")
            final Collection<String> flags = (Collection<String>) rItem.getPropertyValue("productFlags");
            
            if ((flags != null) && flags.contains("flgAutoMWS")) {
              // add current product's suites products
              addRelatedProductList(prodSkuUtil.getSuiteSubProducts(rItem));
            }
          }
        }
        relatedProductRIList = getProdSkuUtil().getAccessoryProducts(getDataSource());

      }
      // add current product related products
      addRelatedProductList(relatedProductRIList);
      
      filterProductList(relatedProductList);
    }
    
    return relatedProductList;
  }
  
  private List<NMProduct> addRelatedProductList(final List<RepositoryItem> relatedProductRIList) {
    NMProduct nmProduct = null;
    if (relatedProductRIList != null) {
      final Iterator<RepositoryItem> relatedProductRIListIterator = relatedProductRIList.iterator();
      while (relatedProductRIListIterator.hasNext()) {
        final RepositoryItem relatedProductRI = relatedProductRIListIterator.next();
        if (getId().equals(relatedProductRI.getRepositoryId())) {
          continue;
        }
        if (getProdSkuUtil().isSuite(relatedProductRI)) {
          final NMSuite nmSuite = new NMSuite(relatedProductRI);
          nmSuite.setIsZeroPriceShowable(getIsZeroPriceShowable());
          nmSuite.setIsNonZeroPriceShowable(getIsNonZeroPriceShowable());
          nmSuite.setIsStatusShowable(getIsStatusShowable());
          final Iterator<NMProduct> productListIterator = nmSuite.getProductList().iterator();
          while (productListIterator.hasNext()) {
            nmProduct = productListIterator.next();
            nmProduct.setIsZeroPriceShowable(getIsZeroPriceShowable());
            nmProduct.setIsNonZeroPriceShowable(getIsNonZeroPriceShowable());
            nmProduct.setIsStatusShowable(getIsStatusShowable());
            nmProduct.setIsRelatedProduct(true);
            relatedProductList.add(nmProduct);
          }
        } else if (getProdSkuUtil().isSuperSuite(relatedProductRI)) {
          // do nothing, supersuites should be omitted per
          // business rules
        } else {
          nmProduct = new NMProduct(relatedProductRI);
          nmProduct.setIsZeroPriceShowable(getIsZeroPriceShowable());
          nmProduct.setIsNonZeroPriceShowable(getIsNonZeroPriceShowable());
          nmProduct.setIsStatusShowable(getIsStatusShowable());
          nmProduct.setIsRelatedProduct(true);
          relatedProductList.add(nmProduct);
        }
      }
    }
    return relatedProductList;
  }
  
  public void setRelatedProductList(final List<NMProduct> relatedProductList) {
    this.relatedProductList = relatedProductList;
  }
  
  public List<NMProduct> getRelatedProductList2() {
    List<RepositoryItem> relatedProductRIList2 = null;
    NMProduct nmProduct = null;
    if (relatedProductList2 == null || relatedProductList2 != null && relatedProductList2.isEmpty()) {
      relatedProductList2 = new ArrayList<NMProduct>();
      relatedProductRIList2 = getProdSkuUtil().getRelatedProducts(getDataSource());
      
      if (relatedProductRIList2 != null) {
        final Iterator<RepositoryItem> relatedProductRIListIterator = relatedProductRIList2.iterator();
        
        while (relatedProductRIListIterator.hasNext()) {
          final RepositoryItem relatedProductRI = relatedProductRIListIterator.next();
          
          if (getProdSkuUtil().isSuite(relatedProductRI)) {
            final NMSuite nmSuite = new NMSuite(relatedProductRI);
            nmSuite.setIsZeroPriceShowable(getIsZeroPriceShowable());
            nmSuite.setIsNonZeroPriceShowable(getIsNonZeroPriceShowable());
            nmSuite.setIsStatusShowable(getIsStatusShowable());
            final Iterator<NMProduct> productListIterator = nmSuite.getProductList().iterator();
            while (productListIterator.hasNext()) {
              nmProduct = productListIterator.next();
              nmProduct.setIsZeroPriceShowable(getIsZeroPriceShowable());
              nmProduct.setIsNonZeroPriceShowable(getIsNonZeroPriceShowable());
              nmProduct.setIsStatusShowable(getIsStatusShowable());
              nmProduct.setIsRelatedProduct2(true);
              relatedProductList2.add(nmProduct);
            }
          } else if (getProdSkuUtil().isSuperSuite(relatedProductRI)) {
            // do nothing, supersuites should be omitted per
            // business rules
          } else {
            nmProduct = new NMProduct(relatedProductRI);
            nmProduct.setIsZeroPriceShowable(getIsZeroPriceShowable());
            nmProduct.setIsNonZeroPriceShowable(getIsNonZeroPriceShowable());
            nmProduct.setIsStatusShowable(getIsStatusShowable());
            nmProduct.setIsRelatedProduct2(true);
            relatedProductList2.add(nmProduct);
          }
        }
      }
      
      filterProductList(relatedProductList2);
    }
    return relatedProductList2;
  }
  
  public void setRelatedProductList2(final List<NMProduct> relatedProductList2) {
    this.relatedProductList2 = relatedProductList2;
  }
  
  /**
   * Name of Method: getType
   * 
   * @return int -- the subtype as definied in productcatalog.xml
   * 
   *         Product = 0 Suite = 1 SuperSuite = 2
   * 
   */
  public int getType() {
    int intValue = 0;
    try {
      intValue = ((Integer) getPropertyValue("type")).intValue();
    } catch (Exception exception) {
      mLogging.logError("Type for product is Null.");
    }
    return intValue;
  }
  
  /**
   * Name of Method: getSkuList
   * 
   * @return List -- returns a list of NMSku's, all of them regardless if they are sellable or not
   * 
   */
  public List<NMSku> getSkuList() {
    if (skuList == null) {
      skuList = new ArrayList<NMSku>();
      
      @SuppressWarnings("unchecked")
      final List<RepositoryItem> childSkusList = (List<RepositoryItem>) getPropertyValue("childSKUs");
      if (!CollectionUtils.isEmpty(childSkusList)) {
      final Iterator<RepositoryItem> childSkusIterator = childSkusList.iterator();
      while (childSkusIterator.hasNext()) {
        final NMSku nmSku = new NMSku(childSkusIterator.next());
        nmSku.setIsSellable(getProdSkuUtil().isSellableSku(getDataSource(), nmSku.getDataSource()));
        skuList.add(nmSku);
      }
    }
    }
    return skuList;
  }
  
  public void setSkuList(final List<NMSku> skuList) {
    this.skuList = skuList;
  }
  
  /**
   * Name of Method: getSellableSkuList
   * 
   * @return List -- returns a list of NMSku's, sellable only
   * 
   */
  public List<NMSku> getSellableSkuList() {
    // default functionality should check profile information before adding
    // sku to sellable list.
    final boolean checkProfile = true;
    return getSellableSkuList(checkProfile);
  }
  
  // In the case of eBay loaders (EbayUtils), we need to suppress the profile
  // check.
  public List<NMSku> getSellableSkuList(final boolean checkProfile) {
    if (sellableSkuList == null) {
      sellableSkuList = new ArrayList<NMSku>();
      
      @SuppressWarnings("unchecked")
      final List<RepositoryItem> childSkusList = (List<RepositoryItem>) getPropertyValue("childSKUs");
      if (!CollectionUtils.isEmpty(childSkusList)) {
        final Iterator<RepositoryItem> childSkusIterator = childSkusList.iterator();
        
        while (childSkusIterator.hasNext()) {
          final RepositoryItem skuItem = childSkusIterator.next();
          final NMSku nmSku = new NMSku(skuItem);
          nmSku.setIsSellable(getProdSkuUtil().isSellableSku(getDataSource(), nmSku.getDataSource()));
          if (nmSku.getIsSellable()) {
            if (checkProfile) {
              final NMProfile nmProfile = (NMProfile) ServletUtil.getCurrentRequest().resolveName("/atg/userprofiling/Profile");
              final LocalizationUtils utils = (LocalizationUtils) Nucleus.getGlobalNucleus().resolveName("/nm/utils/LocalizationUtils");
              if (utils.isSupportedByOnline(nmProfile.getCountryPreference())) {
                sellableSkuList.add(nmSku);
              } else {
                /*
                 * Only the eligible SKU of the product will be added to the sellableSkuList, currently the DropShip skus and ShipFromStore skus are eligible/restricted based on the respective flag,
                 * if the flag is inactive then the sku will not be added to sellableSkuList
                 */
                if (utils.isIntlEligibleSku(skuItem)) {
                  sellableSkuList.add(nmSku);
                }
              }
            } else {
              sellableSkuList.add(nmSku);
            }
          }
        }
      } else {
        // Note -- There are 117385 products which doesn't have child SKU(s) on the dcs_prd_chldsku table. Looks like it is expected behavior and below statement changed to debug instead of error logging.
        // select count(unique(product_id)) from dcs_product where product_id not in (select product_id from dcs_prd_chldsku);
        if ((mLogging != null) && mLogging.isLoggingDebug()) {
          mLogging.logDebug("ChildSku RepositoryItem object is null.");
        }
      }
    }
    
    if (0 == getType()) {
      try {
        if (sellableSkuList.isEmpty()) {
          
          if (getIsDisplayable()) {
            getProdSkuUtil().removeProductSkusFromCache(getDataSource());
          }
        } else {
          if (!getIsDisplayable()) {
            getProdSkuUtil().removeProductSkusFromCache(getDataSource());
          }
          
        }
      } catch (final Exception e) {
        System.out.println("Caught an exception trying to put product on queue" + e);
      }
    }
    
    return sellableSkuList;
  }
  
  public void setSellableSkuList(final List<NMSku> sellableSkuList) {
    this.sellableSkuList = sellableSkuList;
  }
  
  /**
   * Name of Method: getSku
   * 
   * @param int index -- the index in the list to get the sku
   * @return NMSku
   * 
   */
  public NMSku getSku(final int index) {
    NMSku sku = null;
    final List<NMSku> skus = getSkuList();
    if (skus != null) {
      if (index < skus.size()) {
        sku = skus.get(index);
      }
    }
    return sku;
  }
  
  /**
   * Name of Method: getMultiColorSellableSkuList
   * 
   * @return boolean
   * 
   */
  public boolean getHasMultiColorSellableSkuList() {
    if (getMultiColorSellableSkuList().size() > 0) {
      return true;
    } else {
      return false;
    }
  }
  
  /**
   * Name of Method: getMultiColorSellableSkuList
   * 
   * @return List -- NMSkus
   * 
   *         Some skus have multiple colors such as a makeup kit This returns a list of NMSkus that have this attribute Skus returned are all sellable
   */
  public List<NMSku> getMultiColorSellableSkuList() {
    if (multiColorSellableSkuList == null) {
      multiColorSellableSkuList = new ArrayList<NMSku>();
      final Iterator<NMSku> sellableSkuListIterator = getSellableSkuList().iterator();
      
      while (sellableSkuListIterator.hasNext()) {
        final NMSku nmSku = sellableSkuListIterator.next();
        
        if (nmSku.getHasMultiColors()) {
          multiColorSellableSkuList.add(nmSku);
        }
      }
    }
    
    return multiColorSellableSkuList;
  }
  
  public void setMultiColorSellableSkuList(final List<NMSku> multiColorSellableSkuList) {
    this.multiColorSellableSkuList = multiColorSellableSkuList;
  }
  
  /**
   * Name of Method: getHasHexColorSellableSkuList
   * 
   * @return boolean
   * 
   */
  public boolean getHasHexColorSellableSkuList() {
    if (getHexColorSellableSkuList().size() > 0) {
      return true;
    } else {
      return false;
    }
  }
  
  /**
   * Name of Method: getHexColorSellableSkuList
   * 
   * @return List -- NMSkus
   * 
   *         Skus may have a hexColor assigned to them, this method returns a list of the sellable skus that have Hex color
   */
  public List<NMSku> getHexColorSellableSkuList() {
    final Iterator<NMSku> sellableSkuListIterator = getSellableSkuList().iterator();
    
    if (hexColorSellableSkuList == null) {
      final Map<String, String> dupCheckMap = new HashMap<String, String>();
      hexColorSellableSkuList = new ArrayList<NMSku>();
      while (sellableSkuListIterator.hasNext()) {
        final NMSku nmSku = sellableSkuListIterator.next();
        final String hexColor = nmSku.getHexCode();
        
        if (hexColor != null && !hexColor.equals("")) {
          if (dupCheckMap.get(hexColor.trim()) == null) {
            hexColorSellableSkuList.add(nmSku);
            dupCheckMap.put(hexColor.trim(), "");
          }
        }
      }
    }
    return hexColorSellableSkuList;
  }
  
  public void setHexColorSellableSkuList(final List<NMSku> hexColorSellableSkuList) {
    this.hexColorSellableSkuList = hexColorSellableSkuList;
  }
  
  /**
   * Name of Method: getIsShowable
   * 
   * @return boolean
   * 
   *         if the product is showable this is true It is determined to be sellable if one sku is sellable and the product is displayable
   */
  public boolean getIsShowable() {
    if (isShowable == null) {
      boolean showProduct = true;
      try {    	  
    	  if (getIsEbayFlag() || !getIsDisplayable() || (getIsZeroPrice() && !getIsZeroPriceShowable()) || (!getIsZeroPrice() && !getIsNonZeroPriceShowable())) {
          showProduct = false;
        }
      } catch (final Exception e) {
        showProduct = false;
      }
      
      isShowable = new Boolean(showProduct);
      return showProduct;
    } else {
      return isShowable.booleanValue();
    }
  }
  
  /**
   * Name of Method: getIsShopRunnerEligible
   * 
   * @return boolean
   * 
   *         Returns true if the current item is ShopRunner eligible. ShopRunner eligibility is determined by a product that is SL2 eligible in the US Some exclusions apply
   */
  public boolean getIsShopRunnerEligible() {
    if (isShopRunnerEligible == null) {
      boolean eligible = false;
      if (getCountryMap().containsKey("US")) {
        if (getCountryMap().get("US").getServiceLevelCodes().contains("SL2")) {
          final String merchType = getMerchandiseType();
          
          // GC, VGC, and DropShip items should be excluded from
          // ShopRunner eligibility
          if (merchType != null
                  && (merchType.equals(GiftCardHolder.VIRTUAL_GIFT_CARD_MERCH_TYPE) || merchType.equals(GiftCardHolder.GIFT_CARD_MERCH_TYPE) || getIsDropship()
                          && !getIsDropShipMerchTypeEligibleToDisplayServiceLevels())) {
            eligible = false;
          } else {
            eligible = true;
          }
        }
      }
      
      isShopRunnerEligible = new Boolean(eligible);
      return eligible;
    } else {
      return isShopRunnerEligible.booleanValue();
    }
  }
  
  /**
   * Name of Method: getIsSomeShopRunnerEligible
   * 
   * @return boolean
   * 
   *         Returns true if at least one of the items in the product list is ShopRunner eligible
   */
  public boolean getIsSomeShopRunnerEligible() {
    if (isSomeShopRunnerEligible == null) {
      final List<NMProduct> productsList = getProductList();
      final Iterator<NMProduct> productsListIterator = productsList.iterator();
      
      boolean eligible = false;
      
      while (productsListIterator.hasNext()) {
        eligible = productsListIterator.next().getIsShopRunnerEligible();
        
        // break once we know that at least one item is eligible.
        if (eligible == true) {
          break;
        }
      }
      
      isSomeShopRunnerEligible = new Boolean(eligible);
      return eligible;
    } else {
      return isSomeShopRunnerEligible.booleanValue();
    }
  }
  
  /*
   * Ebay products should not be sold on core site. If flgEbayProduct flag is set for this product in the nm_product_flag_mv table then we should return true. The getIsShowable() method should set
   * showProduct to false in this case.
   * 
   * @return boolean
   */
  private boolean getIsEbayFlag() {
    return getMapFlag("flgEbayProduct");
  }
  
  /*
   * Name of Method: getVariationType This will generate variationType which is the switch value determined if the product has color,size,or a combination of each. Select boxes can then be created. 0
   * = only QTY box 1 = QTY box and Color 2 = QTY box and Size 3 = QTY box and Color and Size 4 = catalog setup problem, deprecated?
   */
  public int getVariationType() {
    if (getSellableVariation1Count() > 0 && getSellableVariation2Count() > 0) {
      return QTY_BOX_COLOR_SIZE;
    } else if (getSellableVariation1Count() > 0) {
      return QTY_BOX_COLOR;
    } else if (getSellableVariation2Count() > 0) {
      return QTY_BOX_SIZE;
    } else {
      return QTY_BOX_ONLY;
    }
  }
  
  public void setVariationType(final int variationType) {
    // not an attribute
  }
  
  /**
   * Name of Method: getSellableVariation2Count
   * 
   * @return int
   * 
   *         Returns the number of variation2's on this product(size variation) this is used to help generate the matrix
   */
  public int getSellableVariation2Count() {
    if (sellableVariation2Count == null) {
      final Iterator<NMSku> sellableSkuListIterator = getSellableSkuList().iterator();
      final Map<String, String> dupCheckMap = new HashMap<String, String>();
      
      while (sellableSkuListIterator.hasNext()) {
        final NMSku nmSku = sellableSkuListIterator.next();
        final String codeVariation2 = nmSku.getCodeVariation2();
        if (!codeVariation2.equals("")) {
          dupCheckMap.put(codeVariation2, "");
        }
      }
      
      sellableVariation2Count = new Integer(dupCheckMap.size());
    }
    
    return sellableVariation2Count.intValue();
  }
  
  /**
   * Name of Method: getSellableVariation1Count
   * 
   * @return int
   * 
   *         Returns the number of variation1's on this product(color variation) this is used to help generate the matrix
   */
  public int getSellableVariation1Count() {
    if (sellableVariation1Count == null) {
      final Iterator<NMSku> sellableSkuListIterator = getSellableSkuList().iterator();
      final Map<String, String> dupCheckMap = new HashMap<String, String>();
      
      while (sellableSkuListIterator.hasNext()) {
        final NMSku nmSku = sellableSkuListIterator.next();
        final String codeVariation1 = nmSku.getCodeVariation1();
        
        if (!codeVariation1.equals("")) {
          dupCheckMap.put(codeVariation1, "");
        }
      }
      
      sellableVariation1Count = new Integer(dupCheckMap.size());
    }
    
    return sellableVariation1Count.intValue();
  }
  
  public String getFirstDynamicSiShot() {
    final List<String> shotList = Arrays.asList("m", "a", "b", "c");
    final String shotSize = "g";
    String returnShot = shotList.get(0);
    for (final String imageShot : shotList) {
      try {
        final String imageKey = imageShot + shotSize;
        final RepositoryItem mediaItem = getMediaItem(imageKey);
        if (mediaItem != null) {
          final Boolean siFlagValue = (Boolean) mediaItem.getPropertyValue("flgDynamicImageSpecialInstruction");
          if (siFlagValue != null && siFlagValue) {
            returnShot = imageShot;
            break;
          }
        }
      } catch (final Exception exception) {
        exception.printStackTrace();
      }
    }
    return returnShot;
  }
  
  /**
   * Return the url for the requested image. If the product does not have the requested image then null is returned.
   * 
   * @param imageKey
   * @return
   */
  public String getDynamicImageUrl(final String imageKey) {
    String returnValue = null;
    try {
      final RepositoryItem mediaItem = getMediaItem(imageKey);
      if (mediaItem != null) {
        returnValue = (String) mediaItem.getPropertyValue("dynamicImageDefaultUrl");
      }
    } catch (final Exception exception) {
      exception.printStackTrace();
    }
    return returnValue;
  }
  
  /**
   * Return the url for the requested image. If the product does not have the requested image then null is returned.
   * 
   * @param imageKey
   * @return
   */
  public String getImageUrl(final String imageKey) {
    String returnValue = null;
    try {
      final RepositoryItem mediaItem = getMediaItem(imageKey);
      if (mediaItem != null) {
        returnValue = (String) mediaItem.getPropertyValue("url");
        final Integer version = (Integer) mediaItem.getPropertyValue("Version");
        if (returnValue != null && returnValue.length() > 0) {
          if (version != null) {
            returnValue = RedirectFilter.CACHE_PATH + "/" + version + returnValue;
          }
        } else {
          returnValue = null;
        }
      }
    } catch (final Exception exception) {
      exception.printStackTrace();
    }
    return returnValue;
  }
  
  public RepositoryItem getMediaItem(final String imageKey) {
    RepositoryItem returnValue = null;
    if (dataSource == null) {
      return returnValue;
    }
    final Repository repository = dataSource.getRepository();
    final Repository productCatalogAuxRepository = getCustomCatalogRepository();
    try {
      final RepositoryItem item = productCatalogAuxRepository.getItem(this.getId() + "~" + imageKey, "prod_media_aux");
      if (item != null) {
        final String mediaId = (String) item.getPropertyValue("mediaId");
        returnValue = repository.getItem(mediaId, "media");
      }
    } catch (final RemovedItemException exception) {
      // ignore this exception. it is a known condition that occurs when a
      // media item is removed via a service
    } catch (final Exception exception) {
      exception.printStackTrace();
    }
    
    return returnValue;
    
  }
  
  private Repository getCustomCatalogRepository() {
    if (mcustomCatalogAuxRepository == null) {
      mcustomCatalogAuxRepository = (Repository) Nucleus.getGlobalNucleus().resolveName("/atg/commerce/catalog/ProductCatalogAux");
    }
    return mcustomCatalogAuxRepository;
  }
  
  /**
   * Returns true if the product has the requested image and false if it does not. Images with an empty "" url do not count so false is returned in those cases.
   * 
   * @param imageKey
   * @return
   */
  public boolean hasImage(final String imageKey) {
    boolean returnValue = false;
    final String imageUrl = getImageUrl(imageKey);
    returnValue = imageUrl != null;
    
    return returnValue;
  }
  
  // /////////////////////////////////////
  //
  // JavaScript Generation Section
  //
  // /////////////////////////////////////
  // defines the properties of the product object
  public static final String[] jsProductObjectProperties = {"productNumber" , "prodId" , "skuId" , "var1" , "var2" , "displayName" , "perishable" ,
      "deliveryDays" , // need for perishable dropdown
      "expectedShipDate" , "expectedPlainShipDate" , "stockLevel" ,"backOrderFlag","stockAvailable", "variationType" , "status" , "stockStatusInText" , "onlyXLeftMessage" , "vendorRestrictedDates" , "suiteId" , "storeFulfillStatus" ,
      "maxPurchaseQty" , "suggestedInterval"};
  
  /**
   * Name of Method: getMatrixJavaScript
   * 
   * @return String
   * 
   *         This generates a 2 demisional javascript product matrix
   */
  public String getMatrixJavaScript() {
    
    return ProductMatrixJSHelper.getMatrixJavaScript(false, this);
  }
  
  public String getMatrixJavaScriptTextOnly() {
    return ProductMatrixJSHelper.getMatrixJavaScript(true, this);
  }
  
  /**
   * Name of Method: getProductObjectJavaScript
   * 
   * @return String
   * 
   *         This method returns a string that creates a javascript product object, the object is created based the properties defined in the String array jsProductObjectProperties
   */
  public String getProductObjectJavaScript() {
    final StringBuffer jsString = new StringBuffer(200);
    jsString.append("<SCRIPT LANGUAGE='JavaScript'>");
    jsString.append("\n");
    jsString.append("function product(");
    final int jsProductObjectPropertiesLength = jsProductObjectProperties.length;
    for (int i = 0; i < jsProductObjectPropertiesLength; i++) {
      jsString.append(jsProductObjectProperties[i]);
      if (i + 1 < jsProductObjectPropertiesLength) {
        jsString.append(",");
      }
    }
    jsString.append(") {");
    jsString.append("\n");
    for (int i = 0; i < jsProductObjectPropertiesLength; i++) {
      jsString.append("this." + jsProductObjectProperties[i] + " = " + jsProductObjectProperties[i] + ";");
      jsString.append("\n");
    }
    jsString.append("}");
    jsString.append("\n");
    jsString.append("</SCRIPT>\n");
    
    return jsString.toString();
  }
  
  /**
   * Name of Method: getMoreInfoText
   * 
   * @return String
   * 
   *         encoded text within the desciption could make this more info available this is generated when getLongDescriptionCutLine is called the firsttime
   * 
   *         The query below finds products that have this encoding. select * from dcs_product prd, nm_product_aux aux where long_description like '%!--EJMP%' and prd.product_id = aux.product_id and
   *         aux.flg_display = 1 and product_type = 0
   * 
   */
  public String getMoreInfoText() {
    return moreInfoText;
  }
  
  public void setMoreInfoText(final String moreInfoText) {
    this.moreInfoText = moreInfoText;
  }
  
  /**
   * Name of Method: getMoreInfoLink
   * 
   * @return String
   * 
   *         encoded text within the desciption could make this more info link available this is generated when getLongDescriptionCutLine is called the firsttime
   * 
   *         The query below finds products that have this encoding. select * from dcs_product prd, nm_product_aux aux where long_description like '%!--EJMP%' and prd.product_id = aux.product_id and
   *         aux.flg_display = 1 and product_type = 0
   * 
   */
  public String getMoreInfoLink() {
    return moreInfoLink;
  }
  
  public void setMoreInfoLink(final String moreInfoLink) {
    this.moreInfoLink = moreInfoLink;
  }
  
  /**
   * Get Alternate LongDescription Cutline for Product & Suite when 
   * Long Description Cutline is missing. Order should be: 
   * CopyTop + Long Description for child Products in order 
   * or web display if a product + Size Guide + Copy Bottom
   * 
   * @return cutLine long description
   */
  public String getLongDescriptionCutLine() {
    return ProductCutlineHelper.getLongDescriptionCutLine(this);
  }
  
  public String getLongDescription() {
    return (String) getPropertyValue("longDescription");
  }
  
  /**
   * 
   * Following code will get the property for the international product copy block for China Name of Method: getIntlLongDescription
   */
  public String getIntlLongDescription() {
    return (String) getPropertyValue("intlLongDescription");
  }
  
  public String getDescription() {
    return (String) getPropertyValue("description");
  }
  
  public String getMpsShortSkuId() {
    return (String) getPropertyValue("mpsShortSkuId");
  }
  
  public String getDepartment() {
    return (String) getPropertyValue("deptCode");
  }
  
  public String getEdisonDeliveryCode() {
    return (String) getPropertyValue("edisonDeliveryCode");
  }
  
  public boolean getIsCuspDepartment() throws Exception {
    final String CUSP_DEPT[] = getCuspDepartments().split(";");
    final List<String> cuspDepts = Arrays.asList(CUSP_DEPT);
    return cuspDepts.contains(getDepartment());
  }
  
  public String getCuspDepartments() throws Exception {
    String departmentCodes = "";
    final Repository repository = (Repository) Nucleus.getGlobalNucleus().resolveName("/atg/commerce/catalog/ProductCatalog");
    if (repository != null) {
      final RepositoryItem item = repository.getItem("cuspDepartments", "htmlfragments");
      if (item != null) {
        departmentCodes = (String) item.getPropertyValue("frag_value");
      }
    }
    
    return departmentCodes;
  }
  
  public String getDisplayName() {
    if (displayName == null) {
      displayName = (String) getPropertyValue("displayName");
    }
    return displayName;
  }
  
  public void setDisplayName(final String displayName) {
    setPropertyValue("displayName", displayName);
  }
  
  public int getDeliveryDays() {
    try {
      return ((Integer) getPropertyValue("deliveryDays")).intValue();
    } catch (final Exception e) {
      return 0;
    }
  }
  
  public void setDeliveryDays(final int deliveryDays) {
    setPropertyValue("deliveryDays", new Integer(deliveryDays));
  }
  
  /**
   * Name of Method: getIsPerishable
   * 
   * @return boolean
   * 
   *         check first Sku for perishable flag, if it is then the product is perishable.
   */
  public boolean getIsPerishable() {
    final NMSku firstSku = getSku(0);
    if (firstSku != null) {
      return firstSku.getIsPerishable();
    } else {
      return false;
    }
  }
  
  public boolean getIsPreOrder() {
    final Boolean isPreOrder = (Boolean) getPropertyValue("flgPreview");
    return isPreOrder == null ? false : isPreOrder;
  }
  
  public boolean getIsSellableItem() {
    @SuppressWarnings("unchecked")
    final List<RepositoryItem> childSkusList = (List<RepositoryItem>) getPropertyValue("childSKUs");
    final Iterator<RepositoryItem> childSkusIterator = childSkusList.iterator();
    while (childSkusIterator.hasNext()) {
      final NMSku nmSku = new NMSku(childSkusIterator.next());
      final RepositoryItem prodId = getDataSource();
      final RepositoryItem sku = nmSku.getDataSource();
      
      isSellableItem = getProdSkuUtil().isSellableSku(getDataSource(), nmSku.getDataSource());
      
      if (isSellableItem == true) {
        break;
      }
    }
    
    return isSellableItem;
  }
  
  /*
   * This method returns true if the current product lives in ANY category whose flgLiveTree flag is true.
   */
  public boolean getIsLiveOnTree() {
    boolean isLiveOnTree = false;
    @SuppressWarnings("unchecked")
    final Set<RepositoryItem> parentCatsList = (Set<RepositoryItem>) getPropertyValue("parentCategories");
    final Iterator<RepositoryItem> parentCatIterator = parentCatsList.iterator();
    while (parentCatIterator.hasNext()) {
      final RepositoryItem parentCatRI = parentCatIterator.next();
      isLiveOnTree = (Boolean) parentCatRI.getPropertyValue("flgLiveTree") == null ? false : ((Boolean) parentCatRI.getPropertyValue("flgLiveTree")).booleanValue();
      if (isLiveOnTree) {
        break;
      }
    }
    
    return isLiveOnTree;
  }
  
  public boolean getIsDisplayable() {
    return getProdSkuUtil().isDisplayable(getDataSource());
  }
  
  public boolean getHasSpecialInstructions() {
    return getProdSkuUtil().hasSpecialInstructions(getDataSource());
  }
  
  public boolean getCanHandleSpecialInstructions() {
    return getProdSkuUtil().canHandleSpecialInstructions(getDataSource());
  }
  
  public boolean getHasCodeSetType() {
    return getProdSkuUtil().hasCodeSetType(getDataSource());
  }
  
  public boolean getIsZeroPrice() {
    return getProdSkuUtil().isZeroValueItem(getDataSource());
  }
  
  public boolean getIsPromoProduct() {
    return getProdSkuUtil().showAsPromoProduct(getDataSource());
  }
  
  public boolean getIsExclusive() {
    return getProdSkuUtil().isExclusive(getDataSource());
  }
  
  public boolean getIsHideInternationally() {
    return getProdSkuUtil().isFlgHideInternationally(getDataSource());
  }
  
  public boolean getSuppressHexSwatch() {
    return getProdSkuUtil().getSuppressHexSwatch(getDataSource());
  }
  
  public boolean getIsRequiresSpecialAssistance() {
    if (getHasSpecialInstructions()) {
      if (!getHasCodeSetType()) {
        return true;
      }
    }
    return false;
  }
  
  public boolean getHasParentheticalToDisplay() {
    final Double parentheticalCharge = (Double) getPropertyValue("parentheticalCharge");
    if (parentheticalCharge != null && parentheticalCharge.doubleValue() != 0d) {
      return true;
    } else {
      return false;
    }
  }
  
  public Double getParentheticalCharge() {
    return (Double) getPropertyValue("parentheticalCharge");
  }
  
  public boolean getHasIntParentheticalToDisplay() {
    final Double intParentheticalCharge = (Double) getPropertyValue("intParentheticalCharge");
    if (intParentheticalCharge != null && intParentheticalCharge.doubleValue() != 0d) {
      return true;
    } else {
      return false;
    }
  }
  
  public Double getIntParentheticalCharge() {
    final Double intParentheticalCharge = (Double) getPropertyValue("intParentheticalCharge");
    if (intParentheticalCharge != null) {
      return (Double) getPropertyValue("intParentheticalCharge");
    } else {
      return 0.0;
    }
    
  }
  
  public ProdSkuUtil getProdSkuUtil() {
    if (prodSkuUtil == null) {
      prodSkuUtil = CommonComponentHelper.getProdSkuUtil();
    }
    return prodSkuUtil;
  }
  
  public CountryArray getCountryArray() {
    if (countryArray == null) {
      countryArray = CommonComponentHelper.getCountryArray();
    }
    return countryArray;
  }
  
  public boolean getIsInSuite() {
    if (getParentSuiteId() != null && !getParentSuiteId().equals("")) {
      return true;
    } else {
      return false;
    }
  }
  
  public String getWebDesignerName() {
    if (webDesignerName == null) {
      String prodId = null;
      webDesignerName = (String) getPropertyValue("cmDesignerName");
      
      if (webDesignerName == null) {
        webDesignerName = "";
      } else {
        prodId = getId();
        if (prodId != null && !prodId.equals("")) {
          webDesignerName = getProdSkuUtil().getWebDesignerAsciiName(prodId);
        }
      }
    }
    return webDesignerName;
  }
  
  public void setIsRelatedProduct(final boolean isRelatedProduct) {
    this.isRelatedProduct = isRelatedProduct;
  }
  
  public boolean getIsRelatedProduct() {
    return isRelatedProduct;
  }
  
  public void setIsRelatedProduct2(final boolean isRelatedProduct2) {
    this.isRelatedProduct2 = isRelatedProduct2;
  }
  
  public boolean getIsRelatedProduct2() {
    return isRelatedProduct2;
  }
  
  /**
   * Name of Method: getIsFirstShowableProductInSuite
   * 
   * @return boolean
   * 
   *         this is set when the product list is generated within NMSuite
   * 
   */
  public boolean getIsFirstShowableProductInSuite() {
    return isFirstShowableProductInSuite;
  }
  
  public void setIsFirstShowableProductInSuite(final boolean isFirstShowableProductInSuite) {
    this.isFirstShowableProductInSuite = isFirstShowableProductInSuite;
  }
  
  /**
   * Name of Method: getIsLastShowableProductInSuite
   * 
   * @return boolean
   * 
   *         this is set when the product list is generated within NMSuite
   * 
   */
  public boolean getIsLastShowableProductInSuite() {
    return isLastShowableProductInSuite;
  }
  
  public void setIsLastShowableProductInSuite(final boolean isLastShowableProductInSuite) {
    this.isLastShowableProductInSuite = isLastShowableProductInSuite;
  }
  
  /**
   * Name of Method: getIsOnlyProductInSuite
   * 
   * @return boolean
   * 
   *         this is set after the related product list is filtered
   * 
   */
  public boolean getIsOnlyProductInSuite() {
    return isOnlyProductInSuite;
  }
  
  public void setIsOnlyProductInSuite(final boolean isOnlyProductInSuite) {
    this.isOnlyProductInSuite = isOnlyProductInSuite;
  }
  
  /**
   * Name of Method: getIsStatusShowable()
   * 
   * @return boolean
   * 
   *         Determines if product status's are showable.
   * 
   */
  public boolean getIsStatusShowable() {
    return isStatusShowable;
  }
  
  public void setIsStatusShowable(final boolean isStatusShowable) {
    this.isStatusShowable = isStatusShowable;
  }
  
  /**
   * Name of Method: getIsZeroPriceShowable
   * 
   * @return boolean
   * 
   *         Are zero price products showable
   * 
   */
  public boolean getIsZeroPriceShowable() {
    return isZeroPriceShowable;
  }
  
  public void setIsZeroPriceShowable(final boolean isZeroPriceShowable) {
    this.isZeroPriceShowable = isZeroPriceShowable;
  }
  
  /**
   * Name of Method: getisZeroPriceShowable
   * 
   * @return boolean
   * 
   *         Are non zero price products showable
   * 
   */
  public boolean getIsNonZeroPriceShowable() {
    return isNonZeroPriceShowable;
  }
  
  public void setIsNonZeroPriceShowable(final boolean isNonZeroPriceShowable) {
    this.isNonZeroPriceShowable = isNonZeroPriceShowable;
  }
  
  /**
   * Name of Method: getParentSuiteId
   * 
   * @return String
   * 
   *         this is set when the product list is generated within NMSuite
   * 
   */
  public String getParentSuiteId() {
    return parentSuiteId;
  }
  
  protected void setParentSuiteId(final String parentSuiteId) {
    this.parentSuiteId = parentSuiteId;
  }
  
  public NMSuite getParentSuite() {
    final String parentSuiteId = getParentSuiteId();
    if (parentSuiteId == null || parentSuiteId.length() == 0) {
      return null;
    } else {
      return new NMSuite(parentSuiteId);
    }
  }
  
  /**
   * Name of Method: getProductNumber
   * 
   * @return int
   * 
   *         This is a value used for identifying the matrix, will be used in generation of the matrix and then on the font will use this value to assign numbers for form elements.
   * 
   */
  public int getProductNumber() {
    return productNumber;
  }
  
  public void setProductNumber(final int productNumber) {
    this.productNumber = productNumber;
  }
  
  /**
   * Name of Method: getDataSourceForMainImages
   * 
   * @return RepositoryItem
   * 
   *         The data source to use when rendering Images.
   * 
   */
  public RepositoryItem getDataSourceForMainImages() {
    return getDataSource();
  }
  
  /**
   * Name of Method: getPropertyValue
   * 
   * @param String
   *          propertyName
   * @return Object
   * 
   *         created so that you don't have to type getDataSource() everytime you want to call getPropertyValue
   */
  public Object getPropertyValue(final String propertyName) {
    try {
      final RepositoryItem ri = getDataSource();
      
      if (ri != null && propertyName != null) {
        return ri.getPropertyValue(propertyName);
      } else {
        return null;
      }
      
    } catch (final Exception e) {
      e.printStackTrace();
    }
    return null;
  }
  
  /**
   * Returns the auxiliary media map
   * 
   * @return
   */
  public Map<String, RepositoryItem> getAuxiliaryMedia() {
    @SuppressWarnings("unchecked")
    final Map<String, RepositoryItem> result = (Map<String, RepositoryItem>) getPropertyValue("auxiliaryMedia");
    return result;
  }
  
  /**
   * Returns the taxware category code
   * 
   * @return String
   */
  public String getTaxwareCategory() {
    return (String) getPropertyValue("taxwareCategory");
  }
  
  /**
   * Returns size guide html
   * 
   * @return String
   */
  public String getSizeGuide() {
    return (String) getPropertyValue("sizeGuide");
  }
  
  /**
   * Name of Method: setPropertyValue
   * 
   * @param String
   *          propertyName
   * 
   *          created so that you don't have to type getDataSource() everytime you want to call setPropertyValue
   */
  public void setPropertyValue(final String property, final Object value) {
    try {
      ((MutableRepositoryItem) getDataSource()).setPropertyValue(property, value);
    } catch (final Exception e) {
      e.printStackTrace();
    }
  }
  
  /**
   * Name of Method: getHasShowableHexColors
   * 
   * @return boolean
   * 
   *         Checks whether the product is using hex colors or not
   */
  public boolean getHasShowableHexColors() {
    if (getIsShowable() && (getHasMultiColorSellableSkuList() || getHasHexColorSellableSkuList())) {
      return true;
    } else {
      return false;
    }
  }
  
  public Map<String, NMProdCountry> getCountryMap() {
    if (countryMap == null) {
      countryMap = new HashMap<String, NMProdCountry>();
      
      @SuppressWarnings("unchecked")
      final Map<String, RepositoryItem> countryMapRI = (Map<String, RepositoryItem>) getPropertyValue("countryMap");
      final Iterator<Map.Entry<String, RepositoryItem>> i = countryMapRI.entrySet().iterator();
      while (i.hasNext()) {
        final Map.Entry<String, RepositoryItem> me = i.next();
        countryMap.put(me.getKey(), new NMProdCountry(me.getValue()));
      }
    }
    return countryMap;
  }
  
  /**
   * 
   * @return
   */
  public List<NMRestrictionCode> getRestrictionCodes(final String countryCode) {
    List<NMRestrictionCode> returnValue = new ArrayList<NMRestrictionCode>();
    final Map<String, NMProdCountry> countryMap = getCountryMap();
    
    if (countryMap != null) {
      final NMProdCountry prodCountry = countryMap.get(countryCode);
      if (prodCountry != null) {
        final List<NMRestrictionCode> restrictionCodes = prodCountry.getRestrictionCodes();
        if (restrictionCodes != null) {
          returnValue = restrictionCodes;
        }
      }
    }
    
    return returnValue;
  }
  
  // public Country[] getAllRestrictedShipToCountries() {
  // NMProfile nmProfile =
  // (NMProfile)ServletUtil.getCurrentRequest().resolveName("/atg/userprofiling/Profile");
  // ArrayList<Country> allRestrictedShipToCountries = new
  // ArrayList<Country>();
  // Country[] allShippableCountries =
  // getCountryArray().getAllShippableCountries();
  // for (int i = 0; i < allShippableCountries.length; i++) {
  // String countryCode = allShippableCountries[i].getLongName();
  // if ((!countryCode.equals("US")) &&
  // (!getCountryMap().containsKey(countryCode))) {
  // allRestrictedShipToCountries.add(allShippableCountries[i]);
  // if(isFiftyOneRestricted())
  // {
  // allRestrictedShipToCountries.remove(allShippableCountries[i]);
  // allRestrictedShipToCountries.add(getCountryArray().getCountry(nmProfile.getCountryPreference()));
  // Map<String,String> shippingEligibilityMap =
  // (Map<String,String>)ServletUtil.getCurrentRequest().getAttribute("isShippingSupported");
  // if(null == shippingEligibilityMap)
  // shippingEligibilityMap = new HashMap<String,String>();
  // shippingEligibilityMap.put(getId(), "false");
  // // ServletUtil.getCurrentRequest().setParameter("isShippingSupported",
  // false);
  // ServletUtil.getCurrentRequest().setAttribute("isShippingSupported",
  // shippingEligibilityMap);
  //
  // }
  // }
  //
  //
  // }
  //
  // List<NMProduct> relatedProducts = this.getRelatedProductList();
  // for(NMProduct relatedProduct: relatedProducts)
  // {
  // NMProduct product = new
  // NMProduct(relatedProduct.getDataSource().getRepositoryId());
  // product.getAllRestrictedRelatedProdCountries();
  // }
  // return (Country[])allRestrictedShipToCountries.toArray(new
  // Country[allRestrictedShipToCountries.size()]);
  // }
  
  public Country[] getAllRestrictedShipToCountries() {
    final NMProfile nmProfile = (NMProfile) ServletUtil.getCurrentRequest().resolveName("/atg/userprofiling/Profile");
    
    final ArrayList<Country> allRestrictedShipToCountries = new ArrayList<Country>();
    final Country[] allShippableCountries = getCountryArray().getAllShippableCountries();
    final List<String> countryCodeArrayList = new ArrayList<String>();
    for (int i = 0; i < allShippableCountries.length; i++) {
      
      final String countryCode = allShippableCountries[i].getLongName();
      countryCodeArrayList.add(countryCode);
      if (!countryCode.equals("US") && !getCountryMap().containsKey(countryCode)) {
        allRestrictedShipToCountries.add(allShippableCountries[i]);
      }
    }
    if (!countryCodeArrayList.contains(nmProfile.getCountryPreference())) {
      if (isFiftyOneRestricted()) {
        allRestrictedShipToCountries.clear();
        // allRestrictedShipToCountries.add(getCountryArray().getCountry(nmProfile.getCountryPreference()));
        Map<String, String> shippingEligibilityMap = (Map<String, String>) ServletUtil.getCurrentRequest().getAttribute("isShippingSupported");
        if (null == shippingEligibilityMap) {
          shippingEligibilityMap = new HashMap<String, String>();
        }
        shippingEligibilityMap.put(getId(), "false");
        // ServletUtil.getCurrentRequest().setParameter("isShippingSupported",
        // false);
        ServletUtil.getCurrentRequest().setAttribute("isShippingSupported", shippingEligibilityMap);
        
      }
    }
    
    final List<NMProduct> relatedProducts = this.getRelatedProductList();
    for (final NMProduct relatedProduct : relatedProducts) {
      final NMProduct product = new NMProduct(relatedProduct.getDataSource().getRepositoryId());
      product.getAllRestrictedRelatedProdCountries();
    }
    return allRestrictedShipToCountries.toArray(new Country[allRestrictedShipToCountries.size()]);
  }
  
  public void getRestrictedCountriesForRelatedProducts(final String prodId) {
    final NMProduct product = new NMProduct(prodId);
    product.getAllRestrictedShipToCountries();
  }
  
  // public void getAllRestrictedRelatedProdCountries()
  // {
  // NMProfile nmProfile =
  // (NMProfile)ServletUtil.getCurrentRequest().resolveName("/atg/userprofiling/Profile");
  // ArrayList<Country> allRestrictedShipToCountries = new
  // ArrayList<Country>();
  // Country[] allShippableCountries =
  // getCountryArray().getAllShippableCountries();
  // for (int i = 0; i < allShippableCountries.length; i++) {
  // String countryCode = allShippableCountries[i].getLongName();
  // if ((!countryCode.equals("US")) &&
  // (!getCountryMap().containsKey(countryCode))) {
  // allRestrictedShipToCountries.add(allShippableCountries[i]);
  // if(isFiftyOneRestricted())
  // {
  // allRestrictedShipToCountries.remove(allShippableCountries[i]);
  // allRestrictedShipToCountries.add(getCountryArray().getCountry(nmProfile.getCountryPreference()));
  // Map<String,String> shippingEligibilityMap =
  // (Map<String,String>)ServletUtil.getCurrentRequest().getAttribute("isShippingSupported");
  // if(null == shippingEligibilityMap)
  // shippingEligibilityMap = new HashMap<String,String>();
  // shippingEligibilityMap.put(getId(), "false");
  // // ServletUtil.getCurrentRequest().setParameter("isShippingSupported",
  // false);
  // ServletUtil.getCurrentRequest().setAttribute("isShippingSupported",
  // shippingEligibilityMap);
  //
  // }
  // }
  //
  // }
  // // return (Country[])allRestrictedShipToCountries.toArray(new
  // Country[allRestrictedShipToCountries.size()]);
  // }
  
  public void getAllRestrictedRelatedProdCountries() {
    final NMProfile nmProfile = (NMProfile) ServletUtil.getCurrentRequest().resolveName("/atg/userprofiling/Profile");
    
    final ArrayList<Country> allRestrictedShipToCountries = new ArrayList<Country>();
    final Country[] allShippableCountries = getCountryArray().getAllShippableCountries();
    final List<String> countryCodeArrayList = new ArrayList<String>();
    for (int i = 0; i < allShippableCountries.length; i++) {
      
      final String countryCode = allShippableCountries[i].getLongName();
      countryCodeArrayList.add(countryCode);
      if (!countryCode.equals("US") && !getCountryMap().containsKey(countryCode)) {
        allRestrictedShipToCountries.add(allShippableCountries[i]);
      }
    }
    if (!countryCodeArrayList.contains(nmProfile.getCountryPreference())) {
      if (isFiftyOneRestricted()) {
        // allRestrictedShipToCountries.remove(allShippableCountries[i]);
        // allRestrictedShipToCountries.add(getCountryArray().getCountry(nmProfile.getCountryPreference()));
        Map<String, String> shippingEligibilityMap = (Map<String, String>) ServletUtil.getCurrentRequest().getAttribute("isShippingSupported");
        if (null == shippingEligibilityMap) {
          shippingEligibilityMap = new HashMap<String, String>();
        }
        shippingEligibilityMap.put(getId(), "false");
        // ServletUtil.getCurrentRequest().setParameter("isShippingSupported",
        // false);
        ServletUtil.getCurrentRequest().setAttribute("isShippingSupported", shippingEligibilityMap);
        
      }
    }
    
  }
  
  protected DynamoHttpServletRequest getRequest() {
    return ServletUtil.getCurrentRequest();
  }
  
  public Boolean isFiftyOneRestricted() {
    final NMProfile nmProfile = (NMProfile) ServletUtil.getCurrentRequest().resolveName("/atg/userprofiling/Profile");
    final Set<String> restrictedCountriesSet = (Set<String>) getPropertyValue("restrictedCountryCodes");
    Boolean flgHideIntl = null;
    flgHideIntl = (Boolean) getPropertyValue("flgHideInternationally");
    
    /*
     * If a suite or super suite is hidden, grab the product Id then check if the flag is active in CM3 (nm_product_aux - FLG_HIDE_INTL). If hidden and the country is not US, restrict the entire suite
     * or super suite
     */
    if (!getParentSuiteId().isEmpty()) {
      final DynamoHttpServletRequest request = getRequest();
      final String productId = request.getParameter("itemId");
      final NMProduct nmProduct = prodSkuUtil.getProductObject(productId);
      final String userCountry = nmProfile.getCountryPreference();
      if (nmProduct.getIsHideInternationally() && !userCountry.equals("US")) {
        return true;
      }
    }
    
    /* if a product exist under an Endeca category, we need this check for hide intl */
    if (restrictedCountriesSet.contains("ALL") || flgHideIntl != null && flgHideIntl == true) {
      return true;
    } else if (restrictedCountriesSet.contains(nmProfile.getCountryPreference())) {
      return true;
    } else {
      return false;
    }
    
  }
  
  public boolean isRestrictedToCountry(final String countryCode) {
    if (countryCode == null) {
      return false;
    }
    
    @SuppressWarnings("unchecked")
    final Set<String> restrictedCountriesSet = (Set<String>) getPropertyValue("restrictedCountryCodes");
    
    if (restrictedCountriesSet == null) {
      return false;
    } else if (restrictedCountriesSet.contains("ALL") && !countryCode.equals("US")) {
      // restricted country code ALL should be interpreted as restricted
      // to all borderfree supported countries (which does not include the
      // US)
      return true;
    } else if (restrictedCountriesSet.contains(countryCode)) {
      return true;
    } else {
      return false;
    }
  }
  
  /**
   * Method to add country restriction to Product.
   * 
   * @param countryCode
   *          Restricted Country
   * */
  public void addRestrictedCountry(final String countryCode) {
    if (countryCode == null) {
      return;
    }
    
    @SuppressWarnings("unchecked")
    Set<String> restrictedCountriesSet = (Set<String>) getPropertyValue("restrictedCountryCodes");
    
    if (restrictedCountriesSet == null) {
      restrictedCountriesSet = new HashSet<String>();
    }
    
    restrictedCountriesSet.add(countryCode);
    setPropertyValue("restrictedCountryCodes", restrictedCountriesSet);
  }
  
  /**
   * Returns the CMOS Catalog Id for this product
   * 
   * @return
   */
  public String getCmosCatalogId() {
    if (cmosCatalogId == null) {
      cmosCatalogId = (String) getPropertyValue("cmosCatalogId");
    }
    return cmosCatalogId;
  }
  
  public String getProductPageProductId() {
    if (productPageId == null) {
      if (getIsInSuite()) {
        productPageId = getParentSuiteId();
      } else {
        productPageId = getId();
      }
    }
    return productPageId;
  }
  
  /**
   * Returns the codeSetType for this product
   * 
   * @return
   */
  public String getCodeSetType() {
    return (String) getPropertyValue("codeSetType");
  }
  
  /**
   * Retrieves the Special Instruction Flag for this product
   * 
   * @return Special Instruction Flag for the product
   */
  public String getSpecialInstructionFlag() {
    String specialInstructionFlag = (String) getPropertyValue("specialInstructionFlag");
    if (null == specialInstructionFlag) {
      specialInstructionFlag = MonogramConstants.NO_SPECIAL_INSTRUCTION_FLAG;
      if (StringUtilities.isNotEmpty(getCodeSetType())) {
        // Old flow
        specialInstructionFlag = MonogramConstants.REQUIRED_MONOGRAM_FLAG;
      }
    }
    return specialInstructionFlag;
  }
  
  /**
   * Retrieves the Special Instruction price (Service charge) for this product
   * 
   * @return Special Instruction Price for the product
   */
  public Double getSpecialInstructionPrice() {
    Double specialInstructionPrice = ((Double) getPropertyValue("specialInstructionPrice"));
    if (null == specialInstructionPrice) {
      specialInstructionPrice = 0.0d;
    }
    return specialInstructionPrice;
  }
  
  /**
   * Converts the special instruction price from USD to local price
   * 
   * @return Special Instruction Price for the product
   */
  public String getLocalizedSpecialInstructionPrice() {
    
      String localSpecialInstructionPrice;
      NMProfile nmProfile = null;
    
      if(ServletUtil.getCurrentRequest() != null) {
          nmProfile = (NMProfile) ServletUtil.getCurrentRequest()
                  .resolveName("/atg/userprofiling/Profile");
      }
    
      if (nmProfile != null) {
          final LocalizationUtils utils = ((LocalizationUtils) Nucleus.getGlobalNucleus()
                  .resolveName("/nm/utils/LocalizationUtils"));
          
          localSpecialInstructionPrice = utils.convertPrice(nmProfile.getCurrencyPreference(), 
                  nmProfile.getCountryPreference(), INMGenericConstants.US_CURRENCY_CODE, 
                  getSpecialInstructionPrice());
      
      } else {
          localSpecialInstructionPrice = getSpecialInstructionPrice().toString();
      }
      
    return localSpecialInstructionPrice;
  }
  
  /**
   * Calculate the monogrammed price for a product by adding a retail price and service charge of the product
   * 
   * @return the price for the monogrammed products
   */
  public String getMonogrammedPrice() {
      
      Double retailPrice = 0d;
      String retailPriceString = getRetailPrice();
      
      if(retailPriceString != null){
          retailPriceString = retailPriceString.replaceAll("[$,]", "");
          retailPriceString = retailPriceString.trim();
          
          try {
              retailPrice = Double.valueOf(retailPriceString);
          }catch(Exception e){
              GenericLogger logger = CommonComponentHelper.getLogger();
              if(logger != null){
                  logger.error("Exception while converting price string to double value");
              }
          }
      }
      
      Double monogramPrice = Double.valueOf(getLocalizedSpecialInstructionPrice()) 
              + retailPrice;
      
      return monogramPrice.toString();
  }
  
  /**
   * Returns the CMOS Item for this product
   * 
   * @return
   */
  public String getCmosItemCode() {
    if (cmosItemCode == null) {
      cmosItemCode = (String) getPropertyValue("cmosItemCode");
    }
    return cmosItemCode;
  }
  
  /**
   * The purpose of this method is to get the gender code for the product
   * 
   * @return the gender code
   */
  public String getGenderCode() {
    if (genderCode == null) {
      genderCode = (String) getPropertyValue(GENDER_CODE);
    }
    return genderCode;
  }
  
  /**
   * The purpose of this method is to get the edison proportion code for the product
   * 
   * @return the edison proportion code
   */
  public String getEdisonProportionCode() {
    if (edisonProportionCode == null) {
      edisonProportionCode = (String) getPropertyValue(EDISON_PROPORTION_CODE);
    }
    return edisonProportionCode;
  }
  
  /**
   * The purpose of this method is to get the cmos class for the product
   * 
   * @return the cmos class
   */
  public String getCmosClass() {
    if (cmosClass == null) {
      cmosClass = (String) getPropertyValue(CMOS_CLASS);
    }
    return cmosClass;
  }
  
  /**
   * The purpose of this method is to get the cmos sub class for the product
   * 
   * @return the cmos sub class
   */
  public String getCmosSubClass() {
    if (cmosSubClass == null) {
      cmosSubClass = (String) getPropertyValue(CMOS_SUB_CLASS);
    }
    return cmosSubClass;
  }
  
  /**
   * The purpose of this method is to get the cmos super class for the product
   * 
   * @return the cmos super class
   */
  public String getCmosSuperClass() {
    if (cmosSuperClass == null) {
      cmosSuperClass = (String) getPropertyValue(CMOS_SUPER_CLASS);
    }
    return cmosSuperClass;
  }

  public String getCmosCatalogItem() {
    if (cmosCatalogItem == null) {
      cmosCatalogItem = getCmosCatalogId() + "_" + getCmosItemCode();
    }
    return cmosCatalogItem;
  }
  
  public Double getPrice(RepositoryItem priceItem) {
    final NMProfile nmProfile = (NMProfile) ServletUtil.getCurrentRequest().resolveName("/atg/userprofiling/Profile");
    LocalizationUtils utils = null;
    double localConfigurationSetRetail = 0.0;
    if (nmProfile != null) {
      utils = ((LocalizationUtils) Nucleus.getGlobalNucleus().resolveName("/nm/utils/LocalizationUtils"));
    }
    if (getFlgConfigurable() && getConfigurationSetMaxRetail() != null && getConfigurationSetMaxRetail() != 0 && isIncludeRetailMaxPrice()) {
      localConfigurationSetRetail =
              Double.valueOf(utils.convertPrice(nmProfile.getCurrencyPreference(), nmProfile.getCountryPreference(), INMGenericConstants.US_CURRENCY_CODE, getConfigurationSetMaxRetail()));
      return (localConfigurationSetRetail + (Double) priceItem.getPropertyValue("price"));
    } else if (getFlgConfigurable() && getConfigurationSetMinRetail() != null && getConfigurationSetMinRetail() != 0 && isIncludeRetailMinPrice()) {
      localConfigurationSetRetail =
              Double.valueOf(utils.convertPrice(nmProfile.getCurrencyPreference(), nmProfile.getCountryPreference(), INMGenericConstants.US_CURRENCY_CODE, getConfigurationSetMinRetail()));
      return (localConfigurationSetRetail + (Double) priceItem.getPropertyValue("price"));
    } else {
      return (Double) priceItem.getPropertyValue("price");
    }
  }
  
  public Double getPrice() {
    if (getFlgConfigurable() && getConfigurationSetMaxRetail() != null && getConfigurationSetMaxRetail() != 0 && isIncludeRetailMaxPrice())
      return (getConfigurationSetMaxRetail() + (Double) getPropertyValue("retailPrice"));
    else if (getFlgConfigurable() && getConfigurationSetMinRetail() != null && getConfigurationSetMinRetail() != 0 && isIncludeRetailMinPrice())
      return (getConfigurationSetMinRetail() + (Double) getPropertyValue("retailPrice"));
    else
      return (Double) getPropertyValue("retailPrice");
  }
  
  public String getSaleType() {
    String saleType = "";
    String cmosCatalogID = getCmosCatalogId();
    if (StringUtilities.isNotEmpty(cmosCatalogID) && cmosCatalogID.length() > 1) {
      String twoLetters = cmosCatalogID.substring(0, 2);
      if (StringUtilities.areEqual(twoLetters, "OC")) {
        saleType = "Clearance";
      } else if (getFlgPricingAdornments()) {
        saleType = "Sale";
      } else {
        saleType = "Regular";
      }
    }
    return saleType;
  }
  
  public Map<String, RepositoryItem> getPricingAdornments() {
    @SuppressWarnings("unchecked")
    final Map<String, RepositoryItem> map = (Map<String, RepositoryItem>) getPropertyValue("pricingAdornments");
    return map;
  }
  
  public String getPriceString() {
    try {
      return NumberFormat.getCurrencyInstance().format(getPrice().doubleValue());
    } catch (final Exception e) {
      return "Price Unavailable";
    }
  }
  
  public List<String> getTrackerCampaigns() {
    final String campaignName = getProdSkuUtil().getCampaignName(getId());
    final List<String> campaignList = new ArrayList<String>();
    if (campaignName != null && !campaignName.trim().equals("")) {
      campaignList.add(campaignName);
    }
    return campaignList;
  }
  
  /**
   * @return the merchandiseType
   */
  public String getMerchandiseType() {
    return (String) getPropertyValue("merchandiseType");
  }
  
  /**
   * @return the template
   */
  public GSAItem getTemplate() {
    return (GSAItem) getPropertyValue("template");
  }
  
  public String getTemplateUrl() {
    return (String) getTemplate().getPropertyValue("url");
  }
  
  public List<String> getAllSizes() {
    return getAllSizesForColor("");
  }
  
  public List<String> getAllSizesForColor(final String requestedColor) {
    final List<String> sizes = new ArrayList<String>();
    Collections.sort(getSkuList(), NMSku.VARIATIONCODE2_ASC_COMPARATOR);
    final Iterator<NMSku> skuListIterator = getSkuList().iterator();
    int pos = 0;
    while (skuListIterator.hasNext()) {
      final NMSku nmSku = skuListIterator.next();
      final String cmVariation1 = getProdSkuUtil().getProdSkuVariation(getDataSource(), nmSku.getDataSource(), "cmVariation1");
      if (StringUtilities.isEmpty(requestedColor) || requestedColor.equals(cmVariation1)) {
        final String cmVariation2 = getProdSkuUtil().getProdSkuVariation(getDataSource(), nmSku.getDataSource(), "cmVariation2");
        if (StringUtilities.isNotEmpty(cmVariation2)) {
          if (!sizes.contains(cmVariation2)) {
            sizes.add(pos++, cmVariation2);
          }
        }
      }
    }
    return sizes;
  }
  
  public List<String> getSizes() {
    return getSizesForColor("");
  }
  
  public List<String> getSizesForColor(final String requestedColor) {
    final List<String> sizes = new ArrayList<String>();
    Collections.sort(getSellableSkuList(), NMSku.VARIATIONCODE2_ASC_COMPARATOR);
    final Iterator<NMSku> sellableSkuListIterator = getSellableSkuList().iterator();
    int pos = 0;
    while (sellableSkuListIterator.hasNext()) {
      final NMSku nmSku = sellableSkuListIterator.next();
      final String cmVariation1 = getProdSkuUtil().getProdSkuVariation(getDataSource(), nmSku.getDataSource(), "cmVariation1");
      if (StringUtilities.isEmpty(requestedColor) || requestedColor.equals(cmVariation1)) {
        String cmVariation2 = getProdSkuUtil().getProdSkuVariation(getDataSource(), nmSku.getDataSource(), "cmVariation2");
        if (StringUtilities.isNotNull(cmVariation2)) {
          cmVariation2 = cmVariation2.trim();
        }
        if (StringUtilities.isNotEmpty(cmVariation2)) {
          if (!sizes.contains(cmVariation2)) {
            sizes.add(pos++, cmVariation2);
          }
        }
      }
    }
    return sizes;
  }
  
  public List<String> getAllColors() {
    return getAllColors("");
  }
  
  public List<String> getAllColors(final String requestedSize) {
    final List<String> colors = new ArrayList<String>();
    Collections.sort(getSkuList(), NMSku.VARIATIONCODE1_ASC_COMPARATOR);
    final Iterator<NMSku> skuListIterator = getSkuList().iterator();
    int pos = 0;
    while (skuListIterator.hasNext()) {
      final NMSku nmSku = skuListIterator.next();
      final String cmVariation2 = getProdSkuUtil().getProdSkuVariation(getDataSource(), nmSku.getDataSource(), "cmVariation2");
      if (StringUtilities.isEmpty(requestedSize) || requestedSize.equals(cmVariation2)) {
        final String cmVariation1 = getProdSkuUtil().getProdSkuVariation(getDataSource(), nmSku.getDataSource(), "cmVariation1");
        if (StringUtilities.isNotEmpty(cmVariation1)) {
          if (!colors.contains(cmVariation1)) {
            colors.add(pos++, cmVariation1);
          }
        }
      }
    }
    return colors;
  }
  
  public List<String[]> getColorsHex() {
    final List<String[]> pairs = new ArrayList<String[]>();
    addColorsForSize("", null, pairs);
    return pairs;
  }
  
  public List<String> getColors() {
    return getColorsForSize("");
  }
  
  public List<String> getColorsForSize(final String requestedSize) {
    final List<String> colors = new ArrayList<String>();
    addColorsForSize(requestedSize, colors, null);
    return colors;
  }
  
  // adds sellable color values for the specified size.
  // variation entries are added to color array if specified.
  // variation / hex code pairs are added to pairs array if specified.
  private void addColorsForSize(final String requestedSize, final List<String> colors, final List<String[]> pairs) {
    final List<String> includedcolors = new ArrayList<String>();
    Collections.sort(getSellableSkuList(), NMSku.VARIATIONCODE1_ASC_COMPARATOR);
    final Iterator<NMSku> sellableSkuListIterator = getSellableSkuList().iterator();
    int pos = 0;
    while (sellableSkuListIterator.hasNext()) {
      final NMSku nmSku = sellableSkuListIterator.next();
      final String cmVariation2 = getProdSkuUtil().getProdSkuVariation(getDataSource(), nmSku.getDataSource(), "cmVariation2");
      if (StringUtilities.isEmpty(requestedSize) || requestedSize.equals(cmVariation2)) {
        final String cmVariation1 = getProdSkuUtil().getProdSkuVariation(getDataSource(), nmSku.getDataSource(), "cmVariation1");
        if (StringUtilities.isNotEmpty(cmVariation1)) {
          if (!includedcolors.contains(cmVariation1)) {
            if (pairs != null) {
              String hexCode = "";
              if (getHasMultiColorSellableSkuList()) {
                final List<RepositoryItem> multiColors = nmSku.getMultiColorList();
                final Iterator<RepositoryItem> multiColorsIterator = multiColors.iterator();
                while (multiColorsIterator.hasNext()) {
                  final RepositoryItem ri = multiColorsIterator.next();
                  hexCode = hexCode + (String) ri.getPropertyValue("hexColor");
                  if (multiColorsIterator.hasNext()) {
                    hexCode = hexCode + ";";
                  }
                }
              } else {
                hexCode = nmSku.getHexCode();
              }
              final String[] variation1AndHex = {cmVariation1 , hexCode};
              pairs.add(pos++, variation1AndHex);
            }
            
            if (colors != null) {
              colors.add(pos++, cmVariation1);
            }
            
            includedcolors.add(cmVariation1);
          }
        }
      }
    }
  }
  
  public String getBetaSizeGuide() {
    String sizeGuide = (String) getPropertyValue("sizeGuide");
    if (StringUtilities.isNotEmpty(sizeGuide)) {
      sizeGuide = sizeGuide.replace("<A", "<A id='" + getId() + "ModernSizeGuide'");
      sizeGuide = sizeGuide.replace("<a", "<a id='" + getId() + "ModernSizeGuide'");
      betaSizeGuide = sizeGuide.replaceFirst("/category/", "/content/");
    }
    return betaSizeGuide;
  }
  
  // Added by INSSK for Request # 28731 on May 12, 2011 - Starts here
  public int getProductStatusInStore() {
    return prodStatus;
  }
  
  public void setProductStatusInStore(final int prodStatus) {
    this.prodStatus = prodStatus;
  }
  
  // Added by INSSK for Request # 28731 on May 12, 2011 - Ends here
  
  /**
   * @return the totalInventory
   */
  public int getTotalInventory() {
    return getInventory(false);
  }
  
  private int getInventory(final boolean isOnline) {
    int totalInventory = 0; // total quantity for that product
    final BrandSpecs brandSpecs = (BrandSpecs) Nucleus.getGlobalNucleus().resolveName("/nm/utils/BrandSpecs");
    boolean checkProfile = true;
    
    try {
        
        if(ServletUtil.getCurrentRequest() == null) {
            //This is a non-dynamo request. no need of profile check
            checkProfile = false; 
        }
        
      if (sellableSkuList == null) {
        sellableSkuList = this.getSellableSkuList(checkProfile);
      }
      
      final Iterator<NMSku> sellableSkusIterator = sellableSkuList.iterator();
      while (sellableSkusIterator.hasNext()) {
        final NMSku nmSku = sellableSkusIterator.next();
        final String stockLevel = nmSku.getStockLevel();
        if (stockLevel != null && stockLevel.trim().length() > 0) {
          if (brandSpecs.isIncludeBossQtyInOnlyXLeft()) {
            if (!isOnline && nmSku.getIsStoreFulfillFlag()) { // StoreFulfillFlag is True
              totalInventory = totalInventory + nmSku.getBossQty();
            } else if (!isOnline && !nmSku.getIsStoreFulfillFlag()) { // StoreFulfillFlag is False
              totalInventory = totalInventory + nmSku.getBossQty() + Integer.parseInt(stockLevel);
            }
          } else {
            if (!isOnline || !nmSku.getIsStoreFulfillFlag()) {
              totalInventory = totalInventory + Integer.parseInt(stockLevel);
            }
          }
        }
      }
    } catch (final Exception ex) {
      ex.printStackTrace();
    }
    
    return totalInventory;
  }
  
  /**
   * Method calculates only online inventory excluding the store fulfill
   * 
   * @return onLine
   */
  public int getTotalInventoryWithoutStoreFulfill() {
    List<NMSku> skuList = getSkuList();
    int onLine = 0;
    for (NMSku sku : skuList) {
      if (!sku.getIsStoreFulfillFlag()) {
        onLine += Integer.parseInt(sku.getStockLevel());
      }
    }
    return onLine;
  }
  
  /**
   * @return the totalInventory for online
   */
  public int getTotalInventoryOnline() {
    return getInventory(true);
  }
  
  /**
   * @return the suggReplenishmentInterval
   */
  public String getSuggReplenishmentInterval() {
    String sggInterval = ""; // total quantity for that product
    try {
      if (sellableSkuList == null) {
        sellableSkuList = this.getSellableSkuList();
      }
      final Iterator<NMSku> sellableSkusIterator = sellableSkuList.iterator();
      while (sellableSkusIterator.hasNext()) {
        final NMSku nmSku = sellableSkusIterator.next();
        sggInterval = nmSku.getSuggReplenishmentInterval();
        if (sggInterval != null && !sggInterval.equals("")) {
          break;
        }
      }
    } catch (final Exception ex) {
      ex.printStackTrace();
    }
    return sggInterval;
  }
  
  public void setSuggReplenishmentInterval(final String suggReplenishmentInterval) {
    
  }
  
  /**
   * @return csProdRecommendationsList
   */
  public List<NMProduct> getCSProdRecommendationsList() {
    return csProdRecommendationsList;
  }
  
  /**
   * setCSProdRecommendationsList method
   * 
   * @param csProdRecommendationsList
   */
  public void setCSProdRecommendationsList(final List<NMProduct> csProdRecommendationsList) {
    this.csProdRecommendationsList = csProdRecommendationsList;
  }
  
  // convenience method for flags to take care of null results
  private boolean getFlag(final String key) {
    final Boolean object = (Boolean) getPropertyValue(key);
    return object == null ? false : object.booleanValue();
  }

  public Set<String> getFlags() {
    @SuppressWarnings("unchecked")
    final Set<String> flags = (Set<String>) getPropertyValue("productFlags");
    return flags;
  }
  
  public static boolean containsProductFlag(final RepositoryItem productItem, final String key) { 
      final Set<String> flags = (Set<String>)productItem.getPropertyValue("productFlags");
      boolean flgConfigurable = flags == null ? false : flags.contains(key);
      return flgConfigurable;
  }

  // convenience method for map flags to take care of null results
  private boolean getMapFlag(final String key) {
    @SuppressWarnings("unchecked")
    final Set<String> flags = (Set<String>) getPropertyValue("productFlags");
    return flags == null ? false : flags.contains(key);
  }
  
  public boolean getFlgDynamicImageColor() {
    return getMapFlag(ConfiguratorConstants.DYNAMIC_IMAGE_COLOR_FLAG);
  }
  
  public boolean getFlgDynamicImageSpecialInstruction() {
    return getMapFlag(ConfiguratorConstants.DYNAMIC_IMAGE_SI_FLAG);
  }
  
  
  // start NMOBLDS-2238
  public boolean getFlgConfigurable() {
    return getMapFlag(ConfiguratorConstants.DYNAMIC_IMAGE_CONFIGSET_FLAG);
  }
  
  // end NMOBLDS-2238
  
  public boolean getFlgOverrideImg() {
    return getMapFlag(ConfiguratorConstants.OVERRIDE_IMAGE);
  }
  
  public boolean getFlgMerchManual() {
    return getMapFlag("flgMerchManual");
  }
  
  public boolean getFlgFeatured() {
    return getMapFlag("flgFeatured");
  }
  
  @SuppressWarnings("unused")
  public boolean getFlgEditorial(){
	  return getMapFlag("flgEditorial");
  }
  
  public boolean getFlgPricingAdornments() {
    return getFlag("flgPricingAdornments");
  }
  
  public boolean getFlgParenthetical() {
    return getFlag("flgParenthetical");
  }
  
  public boolean getFlgExcludeFromMoreColorsService() {
    return getMapFlag("flgExcludeFromMoreColorsService");
  }
  
  public boolean getFlgDeliveryPhone() {
    return getFlag("flgDeliveryPhone");
  }
  
  public boolean getFlgSoldOut() {
    return !((Boolean) getPropertyValue("flgDisplay")).booleanValue();
  }
  
  public Map<String, String> getImageUrl() {
    return mImageUrlMap;
  }
  
  public Map<String, String> getImageOrShimUrl() {
    return mImageOrShimUrlMap;
  }
  
  public Map<String, Boolean> getImagePresent() {
    return mImagePresentMap;
  }
  
  public String getCanonicalUrl() {
    return (String) getPropertyValue("canonicalUrl");
  }
  
  public Date getSaleSiloMemberDate() {
    return (Date) getPropertyValue("saleSiloMemberDate");
  }
  
  public RepositoryItem[] getParentSuiteRepositoryItems() {
    if (parentSuiteRepositoryItems == null) {
      parentSuiteRepositoryItems = getProdSkuUtil().getSuitesContainingProduct(getId());
      if (parentSuiteRepositoryItems == null) {
        parentSuiteRepositoryItems = new RepositoryItem[0];
      }
    }
    
    return parentSuiteRepositoryItems;
  }
  
  public String getUseSuiteId(){
	  String suiteId="";
	 if(getParentSuiteRepositoryItems()!=null && parentSuiteRepositoryItems.length>0 && parentSuiteRepositoryItems[0]!=null){
		 suiteId= parentSuiteRepositoryItems[0].getRepositoryId();
	 }	 
	 return suiteId;
  }
  public int getLimitQty() {
    return getProdSkuUtil().getMaxPurchaseQty(getDataSource());
  }
  
  public double getSalePrice() {
    double returnValue = 0.0d;
    
    final Double salePrice = (Double) getPropertyValue("salePrice");
    if (salePrice != null) {
      returnValue = salePrice.doubleValue();
    }
    
    return returnValue;
  }
  
  public List<String> getSICodes() {
    @SuppressWarnings("unchecked")
    final List<String> codes = (List<String>) getPropertyValue("codes");
    return codes;
  }
  
  /**
   * Iterates through the SkuList and checks to see if at least one SKU is a item
   * 
   * @return if any of the SKUs are dropship
   * @see prodButtons.jsp
   */
  public boolean getIsDropship() {
    final List<NMSku> dropshipSkuList = this.getSkuList();
    final Iterator<NMSku> dropSkuIterator = dropshipSkuList.iterator();
    
    while (dropSkuIterator.hasNext()) {
      final boolean dropship = dropSkuIterator.next().getIsDropShip();
      if (dropship) {
        return true;
      }
    }
    return false;
  }
  
  /**
   * WR #41198 - Display service levels for drop ship items CR #42214 - Merch types 4(Stationery) & 5(Others) are only eligible to display service levels
   * 
   * @return boolean
   */
  public boolean getIsDropShipMerchTypeEligibleToDisplayServiceLevels() {
    boolean eligible = false;
    final BrandSpecs brandSpecs = (BrandSpecs) Nucleus.getGlobalNucleus().resolveName("/nm/utils/BrandSpecs");
    if (brandSpecs.isEnableDropShipServiceLevels() && getMerchandiseType() != null && (getMerchandiseType().equals("4") || getMerchandiseType().equals("5"))) {
      eligible = true;
    }
    return eligible;
  }
  
  /**
   * 
   * Returns true if the product is an ICE (Precious Jewelry) product
   * 
   * @author nmve1
   * @return true or false
   */
  public boolean isICEProduct() {
    boolean isICEProduct = false;
    int ICEItem = 0;
    final List<NMProduct> productsList = getProductList();
    final Iterator<NMProduct> productsListIterator = productsList.iterator();
    while (productsListIterator.hasNext()) {
      final String productCode = productsListIterator.next().getCmosItemCode();
      if (productCode.toUpperCase().startsWith("P") || productCode.toUpperCase().startsWith("J")) {
        ICEItem++;
      }
    }
    if (ICEItem != 0) {
      isICEProduct = true;
    } else {
      isICEProduct = false;
    }
    return isICEProduct;
  }
  
  /**
   * Returns true if the flag SuppressCheckout is enabled in CM3.
   * 
   * @return
   */
  public boolean isSuppressCheckout() {
    boolean isSuppressCheckout = false;
    final Boolean flgSuppressCheckout = (Boolean) getPropertyValue("flgSuppressCheckout");
    if (null != flgSuppressCheckout && flgSuppressCheckout) {
      isSuppressCheckout = true;
    }
    return isSuppressCheckout;
  }
  
  /**
   * 
   * Returns true if the product is a Gift Card (TGC or VGC)
   * 
   * @author nmcl54
   * @return true or false
   */
  public boolean getIsGiftCard() {
    if (getMerchandiseType() != null && (getMerchandiseType().equals(GiftCardHolder.GIFT_CARD_MERCH_TYPE) || getMerchandiseType().equals(GiftCardHolder.VIRTUAL_GIFT_CARD_MERCH_TYPE))) {
      return true;
    }
    return false;
  }
  
  public boolean isBOPSEligible() {
    boolean isElligible = true;
    final String merchandiseType = getMerchandiseType();
    final Set<String> INELIGIBLE_MERCHANDISE_TYPES = new HashSet<String>(Arrays.asList(new String[] {"1" , "2" , "3" , "4" , "5" , "7"}));
    if (getIsPerishable()) {
      isElligible = false;
    } else if (merchandiseType != null && merchandiseType.equalsIgnoreCase(GiftCardHolder.GIFT_CARD_MERCH_TYPE)) {
      isElligible = false;
    } else if (merchandiseType != null && INELIGIBLE_MERCHANDISE_TYPES.contains(merchandiseType)) {
      isElligible = false;
    } else if (getFlgParenthetical() && getParentheticalCharge() > 0) {
      isElligible = false;
    } /*
       * else if (getSelectedInterval() != null && !getSelectedInterval().equals("") && !getSelectedInterval().equals("0")){ isElligible = false; }
       */else if (isICEProduct()) {
      isElligible = false;
    }
    
    return isElligible;
  }
  
  public Boolean getSddEligible() {
    Boolean isEligible = Boolean.TRUE;
    if (isBOPSEligible()) {
      final Boolean isMonogramItem = CommonComponentHelper.getProdSkuUtil().isMonogramItem(getDataSource());
      if (isMonogramItem) {
        isEligible = Boolean.FALSE;
      } else if (getIsPreOrder()) {
        isEligible = Boolean.FALSE;
      }
    } else {
      isEligible = Boolean.FALSE;
    }
    return isEligible;
  }
  
  public boolean getIsGWPItem() throws Exception {
    if (1 == getType()) {
      return isGWPSuite();
    } else {
      return getIsZeroPrice();
    }
    
  }
  
  /**
   * Show or Suppress Fav icon based on if the product is sellable.
   * 
   * @return true /false
   * @throws Exception
   *           any exception thrown
   */
  public boolean isShowFavIcon() throws Exception {
    if (showFavIcon == null) {
      showFavIcon = false;
      final int type = getType();
      if ((1 == type) || (2 == type)) {
        showFavIcon = getIsShowable();
      } else if (0 == type) {
        showFavIcon = !getIsZeroPrice() && !getFlgSoldOut();
      }
    }
    return showFavIcon.booleanValue();
  }
  
  public void setShowFavIcon(final boolean showFavIcon) {
    this.showFavIcon = showFavIcon;
  }
  
  // SHOPRUN-136 changes start
  /**
   * This method iterates through the Sku list and checks if the Sku is Out of Stock
   * 
   * @return outOfStock
   */
  public boolean isOutOfStock() {
    if (outOfStock == null) {
      final List<NMSku> skuList = this.getSkuList();
      outOfStock = true;
      for (final NMSku nmsku : skuList) {
        if (nmsku.getIsSellable()) {
          if (nmsku.getInStock()) {
            outOfStock = false;
            break;
          }
        }
      }
    }
    return outOfStock.booleanValue();
  }
  
  /**
   * If the product is super suite, iterate subsuite and check if the products in suite is sellable. if 1 product is sellable, entire suite should be sellable and fav icon to be displayed.
   * 
   * @return true when sellable
   * @throws Exception
   *           any exception thrown
   */
  @SuppressWarnings("unchecked")
  private boolean isSellableSuperSuite() throws Exception {
    final List<RepositoryItem> subSuitesList = (List<RepositoryItem>) getPropertyValue("subsuites");
    
    NMProduct product;
    boolean isSellable = false;
    
    for (int i = 0; i < subSuitesList.size(); i++) {
      final RepositoryItem suiteItem = subSuitesList.get(i);
      final List<RepositoryItem> products = (List<RepositoryItem>) suiteItem.getPropertyValue("subproducts");
      for (final RepositoryItem productItem : products) {
        product = new NMProduct(productItem);
        if (!product.getSellableSkuList().isEmpty() && !product.getIsZeroPrice() && !getFlgSoldOut()) {
          isSellable = true;
          break;
        }
      }
      if (isSellable) {
        break;
      }
    }
    return isSellable;
    
  }
  
  /**
   * If the product is suite, iterate sub products and check if the product is sellable. if 1 product is sellable, entire suite should be sellable and fav icon to be displayed.
   * 
   * @return true when sellable
   * @throws Exception
   *           any exception thrown
   */
  @SuppressWarnings("unchecked")
  private boolean isSellableSuite() throws Exception {
    final List<RepositoryItem> products = (List<RepositoryItem>) getPropertyValue("subproducts");
    NMProduct product;
    boolean isSellable = false;
    for (int i = 0; i < products.size(); i++) {
      final RepositoryItem productItem = products.get(i);
      product = new NMProduct(productItem);
      if (!product.getSellableSkuList().isEmpty() && !product.getIsZeroPrice() && !getFlgSoldOut()) {
        isSellable = true;
        break;
      }
    }
    return isSellable;
  }
  
  private boolean isGWPSuite() throws Exception {
    @SuppressWarnings("unchecked")
    final List<RepositoryItem> products = (List<RepositoryItem>) getPropertyValue("subproducts");
    NMProduct product;
    for (int i = 0; i < products.size(); i++) {
      final RepositoryItem productItem = products.get(i);
      product = new NMProduct(productItem);
      if (product.getIsZeroPrice()) {
        return true;
      }
    }
    return false;
  }
  
  /**
   * Returns the webProductDisplayName for use in the error messages This is the concat of designer name and display name
   * 
   * @return String
   */
  public String getWebProductDisplayName() {
    String webProductDisplayName = null;
    String webDesignerName = null;
    String displayName = null;
    
    webDesignerName = getWebDesignerName();
    displayName = getDisplayName();
    
    if (!StringUtils.isEmpty(webDesignerName)) {
      webProductDisplayName = webDesignerName + " " + displayName;
    } else {
      webProductDisplayName = displayName;
    }
    
    return webProductDisplayName;
  }
  
  /**
   * @returns A boolean value that says whether this product has an editorial image shot or not
   */
  public boolean getHasEditorialImage() {
    return hasImage("ei");
  }
  
  /**
   * This method This method iterates through the Sku list and Checks if a Products' expected Ship to date is UNKNOWN
   * 
   * @return isUnknownShipDate
   */
  public boolean isUnknownShipDate() {
    final List<NMSku> skuList = this.getSkuList();
    boolean unknownShipDate = true;
    for (final NMSku nmsku : skuList) {
      if (nmsku.getIsSellable()) {
        if (nmsku.getExpectedShipDate() != null && nmsku.getExpShippingDays() >= 0) {
          unknownShipDate = false;
          break;
        }
      }
    }
    return unknownShipDate;
  }
  
  /**
   * This method iterates through the Sku list to check if the Product and given Sku is BackOrder
   * 
   * @return backOrder
   */
  public boolean isBackOrder() {
    final List<NMSku> skuList = this.getSkuList();
    boolean backOrder = false;
    for (final NMSku nmsku : skuList) {
      if (nmsku.getIsSellable()) {
        backOrder = getProdSkuUtil().onBackOrder(this.getDataSource(), nmsku.getDataSource());
        if (!backOrder) {
          break;
        }
      }
      
    }
    return backOrder;
  }
  
  /**
   * This method checks if the Product is a Fish & Wildlife.The restriction code 'F' is considered with respect to Canada Country code to identify a Fish & Wildlife Product
   * 
   * @return boolean
   */
  public boolean isFishAndWildLife() {
    boolean fishAndWildLife = false;
    final List<NMRestrictionCode> restrictionCodes = getRestrictionCodes(ShopRunnerConstants.CANADA_COUNTRY_CODE);
    if (restrictionCodes != null) {
      final Iterator<NMRestrictionCode> rCodeIterator = restrictionCodes.iterator();
      while (rCodeIterator.hasNext()) {
        final NMRestrictionCode rCode = rCodeIterator.next();
        if (rCode.getRestrictionCode().equalsIgnoreCase(ShopRunnerConstants.FISH_WILDLIFE_RESTR_CODE)) {
          fishAndWildLife = true;
          break;
        }
      }
    }
    return fishAndWildLife;
  }
  
  /**
   * This method checks if the Product is a Cite. The restriction code 'C' is considered with respect to Canada Country code to identify a Cite Product
   * 
   * @return boolean
   */
  public boolean isCiteProduct() {
    boolean citeProduct = false;
    final List<NMRestrictionCode> restrictionCodes = getRestrictionCodes(ShopRunnerConstants.CANADA_COUNTRY_CODE);
    if (restrictionCodes != null) {
      final Iterator<NMRestrictionCode> rCodeIterator = restrictionCodes.iterator();
      while (rCodeIterator.hasNext()) {
        final NMRestrictionCode rCode = rCodeIterator.next();
        if (rCode.getRestrictionCode().equalsIgnoreCase(ShopRunnerConstants.CITE_ITEM_RESTR_CODE)) {
          citeProduct = true;
          break;
        }
      }
    }
    return citeProduct;
  }
  
  /**
   * This method returns true if the current item is Alipay eligible.
   * 
   * @return boolean
   */
  public boolean isAlipayEligible() {
    return getProdSkuUtil().isAlipayEligible(this);
  }
  
  /**
   * This method returns true if the current suite has some product Alipay eligible product
   * 
   * @return isSomeAlipayEligible
   */
  public boolean getIsSomeAlipayEligible() {
    final List<NMProduct> productsList = getProductList();
    final Iterator<NMProduct> productsListIterator = productsList.iterator();
    boolean isSomeAlipayEligible = false;
    
    while (productsListIterator.hasNext()) {
      isSomeAlipayEligible = productsListIterator.next().isAlipayEligible();
      
      // break once we know that at least one item is eligible.
      if (isSomeAlipayEligible) {
        break;
      }
    }
    return isSomeAlipayEligible;
  }
  
  /**
   * This method returns the Country of Origin for a Product
   * 
   * @return String
   */
  public String getCountryOfOrigin() {
    return null != getPropertyValue("countryOfOrigin") ? (String) getPropertyValue("countryOfOrigin") : ShopRunnerConstants.EMPTY_STRING;
  }
  
  /**
   * This method iterates through the Sku list and checks if the SKUs' BOX weight is greater than the configured threshold
   * 
   * @return isBoxWeightGreater
   */
  public boolean isBoxWeightGreater(final double alipayProductBoxWeightThreshold) {
    final List<NMSku> skuList = this.getSkuList();
    boolean boxWeightGreater = false;
    for (final NMSku nmsku : skuList) {
      if (nmsku.getIsSellable()) {
        if (nmsku.getBoxWeight() > alipayProductBoxWeightThreshold) {
          boxWeightGreater = true;
          break;
        }
      }
    }
    return boxWeightGreater;
  }
  
  /**
   * This method iterates through the Sku list and checks if the SKUs' Ex[ected Ship Date is greater than the configured threshold
   * 
   * @param alipayProdDeliveryDaysThreshold
   * @return boolean
   */
  public boolean isExpShipDateGreater(final int alipayProdDeliveryDaysThreshold) {
    final List<NMSku> skuList = this.getSkuList();
    boolean expShipDateGreater = true;
    for (final NMSku nmsku : skuList) {
      if (nmsku.getIsSellable()) {
        if (nmsku.getExpShippingDays() <= alipayProdDeliveryDaysThreshold) {
          expShipDateGreater = false;
          break;
        }
      }
    }
    return expShipDateGreater;
  }
  
  // SHOPRUN-136 changes end
  
  /**
   * @return the overrideImage
   */
  public boolean getOverrideImage() {
    return getFlgOverrideImg();
  }
  
  public boolean isSUBSProduct() {
    final String cmosCatalog = getCmosCatalogId();
    if (cmosCatalog != null && cmosCatalog.equalsIgnoreCase("SUBS")) {
      return true;
    } else {
      return false;
    }
    
  }
  
  /**
   * getConfigurationSetId
   * 
   * @return
   */
  public Object getConfigurationSetId() {
    return getPropertyValue("configurationSetId");
  }
  
  /**
   * getConfigurationSetMaxRetail
   * 
   * @return
   */
  public Double getConfigurationSetMaxRetail() {
    if (getPropertyValue("configurationSetMaxRetail") != null) {
      return (Double) getPropertyValue("configurationSetMaxRetail");
    } else {
      return new Double(0.0);
    }
  }
  
  /**
   * getConfigurationSetMinRetail
   * 
   * @return
   */
  public Double getConfigurationSetMinRetail() {
    if (getPropertyValue("configurationSetMinRetail") != null) {
      return (Double) getPropertyValue("configurationSetMinRetail");
    } else {
      return new Double(0.0);
    }
  }
  
  public boolean isIncludeRetailMinPrice() {
    return includeRetailMinPrice;
  }
  
  public void setIncludeRetailMinPrice(boolean includeRetailMinPrice) {
    this.includeRetailMinPrice = includeRetailMinPrice;
  }
  
  public boolean isIncludeRetailMaxPrice() {
    return includeRetailMaxPrice;
  }
  
  public void setIncludeRetailMaxPrice(boolean includeRetailMaxPrice) {
    this.includeRetailMaxPrice = includeRetailMaxPrice;
  }
  
  /**
   * @return
   */
  public Set<RepositoryItem> getRelatedConfigProducts() {
    return (Set<RepositoryItem>) getPropertyValue("relatedConfigProducts");
  }
  
  /**
   * @return the employeeDiscountEligible
   */
  public boolean isEmployeeDiscountEligible() {
    return employeeDiscountEligible;
  }
  
  /**
   * @param employeeDiscountEligible
   *          the employeeDiscountEligible to set
   */
  public void setEmployeeDiscountEligible(boolean employeeDiscountEligible) {
    this.employeeDiscountEligible = employeeDiscountEligible;
  }
  
  public List<NMProduct> getSuiteProductsList() {
		List<NMProduct> childProds=new ArrayList<NMProduct>();
		NMSuite nmSuite=new NMSuite(this.getId());
		childProds=nmSuite.getProductList();
	    return childProds;
	}
	
	public void setSuiteProductsList(List<NMProduct> suiteProductsList) {
		this.suiteProductsList = suiteProductsList;
	}
  
  
  /**
   * Checks if product has one sku.
   * 
   * @return true, if product has one sku
   */
  public boolean isSingleSKUProduct() {
    boolean hasOneSku = false;
    final List<NMSku> sellableSkuList = getSellableSkuList();
    if (sellableSkuList.size() == 1) {
      hasOneSku = true;
    }
    return hasOneSku;
  }
  
  /**

   * Checks if unsellable sku exists.

   * 

   * @return true, if unsellable sku exists

   */

  public boolean isUnsellableSkuExists() {
    boolean hasUnsellableSku = false;
    if (getType() == 0) {
      final int sellableSkuListSize = getSellableSkuList().size();
      final int skuListSize = getSkuList().size();
      if (sellableSkuListSize < skuListSize) {
        hasUnsellableSku = true;
      }
    } else if (getType() == 1) {
      final List<NMProduct> nmProducts = getProductList();
      for (final NMProduct nmProduct : nmProducts) {
        final int sellableSkuListSize = nmProduct.getSellableSkuList().size();
        final int skuListSize = nmProduct.getSkuList().size();
        if (sellableSkuListSize < skuListSize) {
          hasUnsellableSku = true;
          break;
        }
      }
    }
    return hasUnsellableSku;
  }

  
  /**
   * Gets the sku showable list.
   * 
   * @return the sku showable list
   */
  public List<Boolean> getSkuShowableList() {
    return Collections.nCopies(getSellableSkuList().size(), getIsShowable());
  }
  
  /**
   * Gets the status list.
   * 
   * @return the status list
   */
  public List<String> getStatusList() {
    final List<String> productInventoryStatus = new ArrayList<String>();
    final List<NMSku> sellableSkuList = getSellableSkuList();
    final ProdSkuUtil prodSkuUtil = getProdSkuUtil();
    for (final NMSku sellableSku : sellableSkuList) {
      if (null != sellableSku) {
        prodSkuUtil.getSkuStatus(productInventoryStatus, this, sellableSku);
      }
    }
    return productInventoryStatus;
  }
  
  /**
   * Gets the product expected availability.This method returns the expected availability from now for all sellable skus in the product.
   * 
   * @return the product expected availability after computing the days difference from current day.
   */
  public List<String> getProductExpectedAvailability() {
    List<String> daysDifference = new ArrayList<String>();
    if(NmoUtils.isEmptyCollection(getProductAvailable())){
    final SimpleDateFormat simpleDateFormat = new java.text.SimpleDateFormat("MM/dd/yyyy");
    final ProdSkuUtil prodSkuUtil = getProdSkuUtil();
    final List<NMSku> sellableSkuList = getSellableSkuList();
    for (final NMSku sellableSku : sellableSkuList) {
      prodSkuUtil.getProductAvailability(simpleDateFormat, this, daysDifference, sellableSku);
    }
    this.setProductAvailable(daysDifference);
    }else {
      daysDifference = getProductAvailable();
    }
    return daysDifference;
  }
  
 
  /**
   * Gets the product availability.
   *
   * @return the product availability
   */
  public List<Boolean> getProductAvailability() {
    List<Boolean> isProductAvailableList = new ArrayList<Boolean>();
    if(NmoUtils.isEmptyCollection(getProductAvailable())){
      getProductExpectedAvailability();
    }
    if(NmoUtils.isNotEmptyCollection(getProductAvailable())){
     for(String productAvailable: getProductAvailable()){
        if(INMGenericConstants.DATE_UNAVAILABLE.equalsIgnoreCase(productAvailable)){
          isProductAvailableList.add(Boolean.FALSE);
        } else{
          isProductAvailableList.add(Boolean.TRUE);
        }
      }
    } else{
      isProductAvailableList.add(Boolean.FALSE);
    }
    return isProductAvailableList;
  }
 
  /**
   * Checks if sku is monogrammable. if its monogrammable, set the flag to true.
   * 
   * @return true, if monogrammable
   */
  public boolean isMonogrammable() {
    boolean monogrammable = false;
    if (!MonogramConstants.NO_SPECIAL_INSTRUCTION_FLAG.equalsIgnoreCase(getSpecialInstructionFlag())) {
      monogrammable = true;
    }
    return monogrammable;
  }
  
  /**
   * Gets the sku stock levels.
   * 
   * @return the sku stock levels
   */
  public List<String> getSkuStockLevels() {
    final List<String> skuStockList = new ArrayList<String>();
    final List<NMSku> sellableSkuList = getSellableSkuList();
    for (final NMSku sellableSku : sellableSkuList) {
      skuStockList.add(sellableSku.getStockLevel());
    }
    return skuStockList;
  }
  
  /**
   * Gets the retail price in USD.
   * 
   * @return the retail price in USD
   */
  public String getRetailPriceInUSD() {
    final Double retailPriceInUSD = Double.parseDouble(getRetailPrice());
    final LocalizationUtils utils = CommonComponentHelper.getLocalizationUtils();
    return utils.getUSDConvertedCurrency(utils.getProfile().getCountryPreference(), utils.getProfile().getCurrencyPreference(), retailPriceInUSD);
  }
  
  public String getStyleId() {
    if (getType() == 0) {
      return getSku(0).getCmosSku().substring(0, 9);
    } else {
      return "";
    }
  }
  
  /**
   * @return the productAvailable
   */
  public List<String> getProductAvailable() {
    return productAvailable;
  }

  /**
   * @param productAvailable the productAvailable to set
   */
  public void setProductAvailable(List<String> productAvailable) {
    this.productAvailable = productAvailable;
  }
  
  public String getTitle(){
	  String title="";
	  title=this.getCmosItemCode()+" "+this.getWebDesignerName()+" "+this.getDisplayName();
	  return title;
  }
  /**
   * Gets the edison proportion description for the product
   * 
   * @return the edison proportion desc
   */
  public String getEdisonProportionDesc() {
    if (edisonProportionDesc == null) {
      edisonProportionDesc = (String) getPropertyValue(EDISON_PROPORTION_DESC);
    }
    return edisonProportionDesc;
  }
  
  /**
   * Gets the all SKU's web filter colors for the product.
   * 
   * @return the all web filter colors
   */
  public List<String> getAllWebFilterColors() {
    final List<String> webFilterColors = new ArrayList<String>();
    for (NMSku sku : getSellableSkuList()) {
      final String webFilterColor = sku.getWebFilterColor();
      if(StringUtilities.isNotBlank(webFilterColor) && !webFilterColors.contains(webFilterColor)) {
        webFilterColors.add(webFilterColor);
      }
    }
    return webFilterColors;
  }
  
  /**
   * Gets the all sellable sku sizes.
   * 
   * @return the all sellable sku sizes
   */
  public List<String> getAllSellableSkuSizes() {
    final List<String> sizes = new ArrayList<String>();
    for (NMSku nmSku : getSellableSkuList()) {
      final String cmVariation2 = getProdSkuUtil().getProdSkuVariation(getDataSource(), nmSku.getDataSource(), CM_VARIATION_2);
      if (StringUtilities.isNotEmpty(cmVariation2) && !sizes.contains(cmVariation2)) {
        sizes.add(cmVariation2);
      }
    }
    return sizes;
  }
  public String getServiceLevel() {
    return (String) getPropertyValue("serviceLevelCode");
  }
  
  /**
   * Determines if the product is private/checks for sicode  BLOCKED_VENDOR_DISPLAY.
   * 
   * @return isBVD
   */
  public boolean isBVD() {
	boolean isBVD = false;
  	if(getSICodes() != null && getSICodes().contains("BVD")){
  		isBVD =  true;
  	}
  	return isBVD;
  }
}
