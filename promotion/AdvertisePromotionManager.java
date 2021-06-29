package com.nm.commerce.promotion;

import static com.nm.common.INMGenericConstants.DOLLAR_SIGN;
import static com.nm.common.INMGenericConstants.EMPTY_STRING;
import static com.nm.monogram.utils.MonogramConstants.FREE_MONOGRAM_FLAG;
import static com.nm.monogram.utils.MonogramConstants.OPTIONAL_MONOGRAM_FLAG;

import com.nm.ajax.checkout.utils.ComponentUtils;
import com.nm.api.util.ApiUtil;
import com.nm.collections.GiftWithPurchase;
import com.nm.collections.GiftWithPurchaseArray;
import com.nm.collections.NMPromotion;
import com.nm.collections.NMPromotionArray;
import com.nm.collections.PercentOffPromotion;
import com.nm.collections.PurchaseWithPurchase;
import com.nm.collections.PurchaseWithPurchaseArray;
import com.nm.commerce.catalog.NMProduct;
import com.nm.commerce.catalog.NMProductDisplayPrice;
import com.nm.commerce.catalog.NMSuite;
import com.nm.commerce.catalog.NMSuperSuite;
import com.nm.commerce.pagedef.evaluator.ProductPriceHelper;
import com.nm.commerce.promotion.rules.RuleHelper;
import com.nm.commerce.promotion.rulesBased.Promotion;
import com.nm.common.INMGenericConstants;
import com.nm.components.CommonComponentHelper;
import com.nm.components.CommonComponentLogger;
import com.nm.components.GenericLogger;
import com.nm.configurator.utils.ConfiguratorConstants;
import com.nm.repository.ProductCatalogRepository;
import com.nm.utils.BrandSpecs;
import com.nm.utils.CSRPromotionsHelper;
import com.nm.utils.NmoUtils;
import com.nm.utils.PersonalizedCustData;
import com.nm.utils.PromotionsHelper;
import com.nm.utils.StringUtilities;
import com.nm.utils.SystemSpecs;
import com.nm.utils.cache.NMCacheFactory;

import com.tangosol.net.NamedCache;
import com.tangosol.util.Filter;
import com.tangosol.util.ValueExtractor;
import com.tangosol.util.extractor.KeyExtractor;
import com.tangosol.util.filter.AlwaysFilter;
import com.tangosol.util.filter.AndFilter;
import com.tangosol.util.filter.EqualsFilter;
import com.tangosol.util.filter.LikeFilter;
import com.tangosol.util.processor.ConditionalRemove;

import atg.commerce.pricing.PricingTools;
import atg.core.util.StringUtils;
import atg.nucleus.Nucleus;
import atg.repository.MutableRepositoryItem;
import atg.repository.Repository;
import atg.repository.RepositoryException;
import atg.repository.RepositoryItem;
import atg.servlet.DynamoHttpServletRequest;
import atg.servlet.ServletUtil;

import java.io.IOException;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.servlet.ServletException;

public class AdvertisePromotionManager {

  protected static GenericLogger logger = CommonComponentLogger.getAdvPromoManagerLogger();

  public static final String TEMPLATE = "template";
  public static final String PRODUCT = "product";
  public static final String ITEM = "item";
  public static final String YMAL = "ymal";
  public static final String CHECKOUT = "checkout";
  public static final String INCLUDE_MODAL = "includeModal";
  public static final String GIFT_CARD = "6"; // Traditional Gift Card Merch Type
  public static final String VIRTUAL_GIFT_CARD = "7"; // Virtual Gift Card Merch Type
  public final static String HEX_COLOR_REGEX = "[0-9a-fA-F]{3,6}";

  // ==========================================================
  // INPUT PARAMETERS
  // ==========================================================
  public final static String REPOSITORY_ITEM = "repItem";
  public final static String TYPE = "type";
  public final static String SUITE = "suite";

  // ==========================================================
  // OPARAMS
  // ==========================================================
  public final static String SERVICE_OUTPUT_PARAM_VAL = "serviceParam";
  public final static String DEFAULT = "default";
  public final static String EMPTY = "empty";
  public final static String ERROR = "error";

  // ==========================================================
  // OUTPUT PARAMETERS - Returned via request parameters
  // ==========================================================
  public final static String ERROR_MSG = "errorMsg";
  public final static String HTML_CODE = "htmlCode";
  public final static String PROMO_PRICE = "promoPrice";
  public final static String PROMO_PRICE_TEXT = "promoPriceText";
  public final static String TEMPLATE_DISPLAY_FLAG = "templateDisplayFlag";
  public final static String THUMB_ALT = "thumbAlt";
  public final static String THUMBNAIL_OUTLINE_COLOR = "thumbnailOutlineColor"; // template outline thumbnails
  public final static String THUMBNAIL_OUTLINE_FLAG = "thumbnailOutlineDisplayFlag";
  public final static String THUMBNAIL_PROMO_COLOR = "thumbnailPromoColor"; // template promoPrice color
  public final static String THUMBNAIL_PROMO_COLOR_FLAG = "thumbnailPromoColorFlag";
  public final static String YMAL_PROMO_COLOR = "ymalPromoColor";
  public final static String YMAL_PROMO_COLOR_FLAG = "ymalPromoColorFlag";
  public final static String PRODUCT_PROMO_COLOR = "productPromoColor";
  public final static String PRODUCT_PROMO_COLOR_FLAG = "productPromoColorFlag";
  public final static String LINE_PROMO_COLOR = "linePromoColor";
  public final static String LINE_PROMO_COLOR_FLAG = "linePromoColorFlag";
  public final static String DETAILS_PROMO_COLOR = "detailsPromoColor";
  public final static String DETAILS_PROMO_COLOR_FLAG = "detailsPromoColorFlag";
  public final static String STACKING_PROMO_COLOR = "stackingPromoColor"; // the color of the stacking percent off promo
  public final static String STACKING_PROMO_COLOR_FLAG = "stackingPromoColorFlag";
  public final static String YMAL_DISPLAY_FLAG = "ymalDisplayFlag";
  public final static String PRODUCT_DISPLAY_FLAG = "productDisplayFlag";
  public final static String ITEM_DISPLAY_FLAG = "itemDisplayFlag";
  public final static String ENABLE_PREVIEW_FLAG = "enablePreviewFlag";
  public final static String ENABLE_PROMO_PRICE_DISPLAY = "enablePromoPriceDisplay";
  public final static String MARK_START_DATE = "markStartDate";
  public final static String MARK_END_DATE = "markEndDate";
  public final static String PRICING_ADORNMENTS = "pricingAdornments";
  public final static String ADORNMENT_UNDERSCORE_PRICE = "adornment_price";
  public final static String TYPE_113 = "113";
  public final static String DISPLAY_FLAG = "DisplayFlag";
  public final static String HTML = "Html";

  public final static String TEMPLATE_PRICING_ADORNMENT_FLAG = "templatePricingAdornmentFlag";
  public final static String TEMPLATE_PRICING_ADORNMENT_TEXT = "templatePricingAdornmentText";
  public final static String TEMPLATE_PRICING_ADORNMENT_COLOR_FLAG = "templatePricingAdornmentColorFlag";
  public final static String TEMPLATE_PRICING_ADORNMENT_COLOR = "templatePricingAdornmentColor";

  public final static String PRODUCTPAGE_PRICING_ADORNMENT_FLAG = "productPagePricingAdornmentFlag";
  public final static String PRODUCTPAGE_PRICING_ADORNMENT_TEXT = "productPagePricingAdornmentText";
  public final static String PRODUCTPAGE_PRICING_ADORNMENT_COLOR_FLAG = "productPagePricingAdornmentColorFlag";
  public final static String PRODUCTPAGE_PRICING_ADORNMENT_COLOR = "productPagePricingAdornmentColor";

  public final static String CHECKOUT_PRICING_ADORNMENT_FLAG = "checkoutPricingAdornmentFlag";
  public final static String CHECKOUT_PRICING_ADORNMENT_TEXT = "checkoutPricingAdornmentText";
  public final static String CHECKOUT_PRICING_ADORNMENT_COLOR_FLAG = "checkoutPricingAdornmentColorFlag";
  public final static String CHECKOUT_PRICING_ADORNMENT_COLOR = "checkoutPricingAdornmentColor";

  public final static String ADORNMENT_PRICE = "adornmentPrice";
  public final static String SERVICE_OUTPUT_PARAMS = "serviceOutputParameters";
  public final static String URI_WITH_QUERY_STRING = "uriWithQueryString";
  public final static String PROMO_KEYS = "promoKeys";
  public final static String PROMO_KEY = "promoKey";
  public final static String COUNTRY_CODE = "countryCode";
  public final static String IS_QUICK_VIEW = "isQuickview";
  public final static String USE_PROMO_PRICE_DISPLAY = "usePromoPriceDisplay";
  public final static String CURRENCY_CODE = "currencyCode";
  public final static String ENABLE_PROMO_PREVIEW = "enablePromotionsPreview";

  public final static String PROMO_PRICE_RANGE_DISPLAY_PERCENT_OFF_TEXT = "promoPriceRangeDisplayPercentOffText";
  public final static String PROMO_PRICE_RANGE_DISPLAY_ADV_MIN_PRICE = "promoPriceRangeDisplayAdvertisedMinPrice";
  public final static String PROMO_PRICE_RANGE_DISPLAY_ADV_MAX_PRICE = "promoPriceRangeDisplayAdvertisedMaxPrice";
  public final static String USE_PROMO_PRICE_RANGE_DISPLAY = "usePromoPriceRangeDisplay";
  public final static String PROMO_PERCENT_OFF_VALUE = "promoPercentOffValue";
  public final static String PROMO_PRICE_DISPLAY_PERCENT_OFF_TEXT = "promoPriceDisplayPercentOffText";
  public final static String EXTRA_PROMO_PRICE_DISPLAY_PERCENT_OFF_TEXT = "extraPromoPriceDisplayPercentOffText";
  public final static String PROMO_PRICE_DISPLAY_ADVERTISED_PRICE = "promoPriceDisplayAdvertisedPrice";
  public final static String EXTRA_PROMO_PRICE_DISPLAY_ADV_PRICE = "extraPromoPriceDisplayAdvertisedPrice";
  public final static String PROMO_EXTRA_PERCENT_OFF_VALUE = "promoExtraPercentOffValue";
  public final static String CONFIGURATOR_LEFT_NAV_TOP_PROMO_TEXT = "configuratorLeftNavTopPromoText";

  public final static String ADDITIONAL_PROMO_DISPLAY_PERCENT_OFF_TEXT = "additionalPromoDisplayPercentOffText";

  public final static String PERSONALIZED_OBJECT = "persObj";
  public final static String LINE_ITEM_PROMOTION_GROUPS = "lineItemPromotionGroups";
  public final static String CHILD_PROD_IN_GROUP = "childProdIds";
  public final static String DOLLAR_PROMOTION = "D";
  
  public final static String NEXT_PROMO_DATE = "nextPromoDate";

  // Objects need to be set before calling the getAdvertisePromotions() method
  // Repository objects
  private Repository productCatalogRepository;
  private Repository advertisePromotionRepository;
  private Repository csrProductPromoRepository;

  // Helper objects
  private CSRPromotionsHelper csrPromotionsHelper;
  private PromotionsHelper mPromotionsHelper;

  // Others
  private BrandSpecs brandSpecs;
  private SystemSpecs systemSpecs;

  private String pageType = "";
  private String errorMsg = "";
  private final TreeMap<Integer, String> htmlMap = new TreeMap<Integer, String>();
  private String mThumbnailOutlineColor = null;
  private int mFirstThumbnailPromoIndex = Integer.MAX_VALUE;
  private Boolean thumbnailOutlineDisplayFlag = Boolean.FALSE;
  private boolean includeModal = false;
  private String mThumbnailPromoColor = null;
  private String mYmalPromoColor = null;
  private String mLinePromoColor = null;
  private String mStackingPromoColor = null;

  private Boolean thumbnailPromoColorFlag = Boolean.FALSE;
  private Boolean ymalPromoColorFlag = Boolean.FALSE;
  private Boolean productPromoColorFlag = Boolean.FALSE;
  private Boolean linePromoColorFlag = Boolean.FALSE;
  private Boolean detailsPromoColorFlag = Boolean.FALSE;
  private Boolean stackingPromoColorFlag = Boolean.FALSE;
  private List<String> promoTextList;
  private Boolean mPromoPriceDisplay = Boolean.FALSE; // this flag is set to true if the item has a template html promotion

  private Boolean templatePricingAdornmentFlag = Boolean.FALSE;
  private String templatePricingAdornmentText = null;
  private Boolean templatePricingAdornmentColorFlag = Boolean.FALSE;
  private String templatePricingAdornmentColor = null;

  private Boolean productPagePricingAdornmentFlag = Boolean.FALSE;
  private String productPagePricingAdornmentText = null;
  private Boolean productPagePricingAdornmentColorFlag = Boolean.FALSE;
  private String productPagePricingAdornmentColor = null;

  private Boolean checkoutPricingAdornmentFlag = Boolean.FALSE;
  private String checkoutPricingAdornmentText = null;
  private Boolean checkoutPricingAdornmentColorFlag = Boolean.FALSE;
  private String checkoutPricingAdornmentColor = null;
  private String promoPrice = null;
  private String promoPriceText = null;
  private Double adornmentPrice = 0d;
  
  // this should contain the date/time of the closest upcoming promo start or end, indicating when this product cache should be invalidated.
  private Date nextPromoDate = null;
  
  private final boolean isDynamoRequest;

  private void processPriceAdorn(final RepositoryItem repItem) {
    @SuppressWarnings("unchecked")
    final Map<Object, Object> repoPricingAdornments = (Map<Object, Object>) repItem.getPropertyValue(PRICING_ADORNMENTS);
    final Object[] keys = repoPricingAdornments.keySet().toArray();

    if (keys.length > 0) {
      final int adornPriceKey = keys.length - 1;
      final RepositoryItem adornment = (RepositoryItem) repoPricingAdornments.get((String) keys[adornPriceKey]);
      setAdornmentPrice((null != adornment) ? (Double) adornment.getPropertyValue(ADORNMENT_UNDERSCORE_PRICE) : 0d);
    } else {
      setAdornmentPrice(0d);
    }
  }

  public AdvertisePromotionManager() {
    productCatalogRepository = (Repository) Nucleus.getGlobalNucleus().resolveName("/atg/commerce/catalog/ProductCatalog");
    advertisePromotionRepository = CommonComponentHelper.getCsrAdvertisePromosRepository();
    csrProductPromoRepository = CommonComponentHelper.getCsrProductPromosRepository();
    csrPromotionsHelper = (CSRPromotionsHelper) Nucleus.getGlobalNucleus().resolveName("/nm/utils/CSRPromotionsHelper");
    mPromotionsHelper = CommonComponentHelper.getPromotionsHelper();
    brandSpecs = CommonComponentHelper.getBrandSpecs();
    systemSpecs = CommonComponentHelper.getSystemSpecs();
    DynamoHttpServletRequest request = ServletUtil.getCurrentRequest();
    isDynamoRequest = (request == null) ? false : true;
  }

  private boolean validateAdvertisePromotionData(Map<String, Object> inputParams, Map<String, Object> outputParams, Map<String, Object> processParams) {
    final Boolean serviceOutputParameters = (Boolean) inputParams.get(SERVICE_OUTPUT_PARAMS);
    final Boolean serviceOutput = serviceOutputParameters != null ? serviceOutputParameters.booleanValue() : true;
    final String pageType = ((String) inputParams.get(TYPE)).toLowerCase();
    setPageType(pageType);
    final Object objIncludeModal = inputParams.get(INCLUDE_MODAL);

    if (objIncludeModal != null && "true".equalsIgnoreCase((String) objIncludeModal)) {
      includeModal = true;
    }

    // must receive "template", "product", "item" or "ymal" as the type
    if (!TEMPLATE.equalsIgnoreCase(pageType) && !PRODUCT.equalsIgnoreCase(pageType) && !ITEM.equalsIgnoreCase(pageType) && !YMAL.equalsIgnoreCase(pageType) && !CHECKOUT.equalsIgnoreCase(pageType)) {
      outputParams.put(ERROR_MSG, "Invalid lookup type received: " + pageType + ".");

      if (serviceOutput) {
        outputParams.put(SERVICE_OUTPUT_PARAM_VAL, ERROR);
      }

      return false;
    }

    final Object objRepItem = inputParams.get(REPOSITORY_ITEM);

    if (objRepItem == null || !(objRepItem instanceof RepositoryItem)) {
      errorMsg = "No product was received.";
      String uri = (String) inputParams.get(URI_WITH_QUERY_STRING);
      logError("AdvertisePromotionManager.service: errorMsg:" + errorMsg + " -> (" + objRepItem + ") " + uri);

      outputParams.put(ERROR_MSG, errorMsg);

      if (serviceOutput) {
        outputParams.put(SERVICE_OUTPUT_PARAM_VAL, ERROR);
      }

      return false;
    }

    RepositoryItem productItem = (RepositoryItem) objRepItem;

    if (!validMerchType(productItem)) {
      if (serviceOutput) {
        outputParams.put(SERVICE_OUTPUT_PARAM_VAL, EMPTY);
      }
      return false;
    }

    processParams.put("productItem", productItem);
    processParams.put("serviceOutput", serviceOutput);

    return true;
  }

  /**
   * The Service method
   *
   * @param DynamoHttpServletRequest
   * @param DynamoHttpServletResponse
   * @exception ServletException
   * @exception IOException
   */
  public Map<String, Object> getAdvertisePromotions(Map<String, Object> inputParams) throws Exception {

    Map<String, Object> outputParams = new HashMap<String, Object>();
    try {
      logDebug("Getting advpromo for the input params " + inputParams);

      resetAndInitializeDefaultValues();// resets the initial values. This code change is for the code seperation.

      final boolean enablePreview = getBooleanValue(inputParams.get(ENABLE_PROMO_PREVIEW), false);
      RepositoryItem repItem = null;
      boolean serviceOutput = false;
      Map<String, Object> processParams = new HashMap<String, Object>();

      processParams.put("serviceOutput", Boolean.FALSE);
      processParams.put("productItem", repItem);
      boolean isValidPromo = validateAdvertisePromotionData(inputParams, outputParams, processParams);

      // if any data is missing or invalid, we should not continue processing.
      if (!isValidPromo) {
        return outputParams;
      }
      repItem = (RepositoryItem) processParams.get("productItem");
      serviceOutput = (Boolean) processParams.get("serviceOutput");

      outputParams.put(THUMB_ALT, findAltText(repItem));
      @SuppressWarnings("unchecked")
      final Collection<String> promoKeys = (Collection<String>) inputParams.get(PROMO_KEYS);

      if (promoKeys != null && promoKeys.size() > 0) {
        try {
          logDebug("promokeys present. keys=" + promoKeys);
          processPromoKeys(promoKeys, enablePreview, repItem, inputParams, outputParams);
        } catch (final Exception exception) {
          errorMsg = exception.getMessage();
          logError("AdvertisePromotionManager.service: ", exception);
        }
      } else {
        // determine if it is a product, suite, or super suite
        final Integer type = (Integer) repItem.getPropertyValue(TYPE);
        try {
          switch (type.intValue()) {
            case 2: // this product is a supersuite
              logDebug("product type is 2");
              // processProductPriceRange(repItem.getRepositoryId(), enablePreview, "supersuite", inputParams, outputParams);
              break;
            case 1: // this product is a suite
              logDebug("product type is 1");
              processProductPriceRange(repItem, enablePreview, SUITE, inputParams, outputParams);
              break;
            case 0: // this product is not a suite or supersuite
              logDebug("product type is 0");
              String specialInstructionFlag = (String) repItem.getPropertyValue("specialInstructionFlag");

              if (OPTIONAL_MONOGRAM_FLAG.equalsIgnoreCase(specialInstructionFlag) || FREE_MONOGRAM_FLAG.equalsIgnoreCase(specialInstructionFlag)) {
                logDebug("optional/free monogram");
                processProductPriceRange(repItem, enablePreview, PRODUCT, inputParams, outputParams);
              } else {
                logDebug("normal product");
                processProduct(repItem, enablePreview, PRODUCT, inputParams, outputParams);
              }
              break;
          }
        } catch (final ClassCastException cce) {
          errorMsg = cce.getMessage();
          logError("AdvertisePromotionManager.service: ClassCastException:", cce);

        } catch (final Exception exception) {
          errorMsg = exception.getMessage();
          logError("AdvertisePromotionManager: Exception while getting advPromo", exception);
        }
      }

      outputParams.put(THUMBNAIL_OUTLINE_FLAG, thumbnailOutlineDisplayFlag);
      outputParams.put(THUMBNAIL_OUTLINE_COLOR, getThumbnailOutlineColor());
      outputParams.put(THUMBNAIL_PROMO_COLOR_FLAG, thumbnailPromoColorFlag);
      outputParams.put(THUMBNAIL_PROMO_COLOR, getThumbnailPromoColor());
      outputParams.put(YMAL_PROMO_COLOR_FLAG, ymalPromoColorFlag);
      outputParams.put(YMAL_PROMO_COLOR, getYmalPromoColor());
      outputParams.put(PRODUCT_PROMO_COLOR_FLAG, productPromoColorFlag);
      outputParams.put(LINE_PROMO_COLOR_FLAG, linePromoColorFlag);
      outputParams.put(LINE_PROMO_COLOR, getLinePromoColor());
      outputParams.put(STACKING_PROMO_COLOR, getStackingPromoColor());
      outputParams.put(STACKING_PROMO_COLOR_FLAG, stackingPromoColorFlag);
      outputParams.put(DETAILS_PROMO_COLOR_FLAG, detailsPromoColorFlag);

      assignPricingAdornMentFlags(outputParams, repItem, false);// assigns the pricing adornment flags.this change is for code simplification.
      outputParams.put(ADORNMENT_PRICE, getAdornmentPrice());

      final Integer type = (Integer) repItem.getPropertyValue("type");
      if (getHtmlMap().isEmpty() && !getPromoPriceDisplay() && (getThumbnailOutlineColor() == null || getThumbnailOutlineColor().trim().equals(""))) {
        outputParams.put(SERVICE_OUTPUT_PARAM_VAL, EMPTY);
        if ((type.intValue() == 1 && getPageType().equalsIgnoreCase(TEMPLATE))) {
          outputParams.put(SERVICE_OUTPUT_PARAM_VAL, DEFAULT);
        }
        return outputParams;

      } else {
        outputParams.put(HTML_CODE, getHtmlString(includeModal));
        outputParams.put(THUMBNAIL_OUTLINE_COLOR, getThumbnailOutlineColor());
        outputParams.put(THUMBNAIL_PROMO_COLOR, getThumbnailPromoColor());
        outputParams.put(YMAL_PROMO_COLOR, getYmalPromoColor());
        outputParams.put(LINE_PROMO_COLOR, getLinePromoColor());

        boolean flgConfigurable = NMProduct.containsProductFlag(repItem, ConfiguratorConstants.DYNAMIC_IMAGE_CONFIGSET_FLAG);
        if (flgConfigurable) {
          outputParams.put(TEMPLATE_PRICING_ADORNMENT_FLAG, false);
          outputParams.put(TEMPLATE_PRICING_ADORNMENT_COLOR_FLAG, false);

        } else {
          outputParams.put(TEMPLATE_PRICING_ADORNMENT_FLAG, templatePricingAdornmentFlag);
          outputParams.put(TEMPLATE_PRICING_ADORNMENT_COLOR_FLAG, templatePricingAdornmentColorFlag);
        }

        outputParams.put(TEMPLATE_PRICING_ADORNMENT_TEXT, templatePricingAdornmentText);
        outputParams.put(TEMPLATE_PRICING_ADORNMENT_COLOR, getTemplatePricingAdornmentColor());

        outputParams.put(PRODUCTPAGE_PRICING_ADORNMENT_FLAG, productPagePricingAdornmentFlag);
        outputParams.put(PRODUCTPAGE_PRICING_ADORNMENT_TEXT, productPagePricingAdornmentText);
        outputParams.put(PRODUCTPAGE_PRICING_ADORNMENT_COLOR_FLAG, productPagePricingAdornmentColorFlag);
        outputParams.put(PRODUCTPAGE_PRICING_ADORNMENT_COLOR, getProductPagePricingAdornmentColor());

        outputParams.put(CHECKOUT_PRICING_ADORNMENT_FLAG, checkoutPricingAdornmentFlag);
        outputParams.put(CHECKOUT_PRICING_ADORNMENT_TEXT, checkoutPricingAdornmentText);
        outputParams.put(CHECKOUT_PRICING_ADORNMENT_COLOR_FLAG, checkoutPricingAdornmentColorFlag);
        outputParams.put(CHECKOUT_PRICING_ADORNMENT_COLOR, checkoutPricingAdornmentColor);
        outputParams.put(ADORNMENT_PRICE, getAdornmentPrice());
        outputParams.put(PROMO_PRICE, getPromoPrice());
        outputParams.put(PROMO_KEY, promoKeyForDollarOffExist(repItem));// NMOBLDS-5866

        outputParams.put(NEXT_PROMO_DATE, nextPromoDate); // this value may be null

        if (serviceOutput) {
          outputParams.put(SERVICE_OUTPUT_PARAM_VAL, DEFAULT);
        }

        return outputParams;
      }

    } catch (final Exception e) {
      e.printStackTrace();
    }

    return outputParams;
  }

  private void resetAndInitializeDefaultValues() {

    errorMsg = "";
    pageType = "";
    includeModal = false;
    resetHtmlMap();
    setPromoPriceDisplay(false);

    mThumbnailOutlineColor = null;
    mThumbnailPromoColor = null;
    mYmalPromoColor = null;
    mLinePromoColor = null;
    mStackingPromoColor = null;
    mFirstThumbnailPromoIndex = Integer.MAX_VALUE;

    templatePricingAdornmentFlag = Boolean.FALSE;
    templatePricingAdornmentText = "";
    templatePricingAdornmentColorFlag = Boolean.FALSE;
    templatePricingAdornmentColor = "";
    productPagePricingAdornmentFlag = Boolean.FALSE;
    productPagePricingAdornmentText = "";
    productPagePricingAdornmentColorFlag = Boolean.FALSE;
    productPagePricingAdornmentColor = "";
    checkoutPricingAdornmentFlag = Boolean.FALSE;
    checkoutPricingAdornmentText = "";
    checkoutPricingAdornmentColorFlag = Boolean.FALSE;
    checkoutPricingAdornmentColor = "";

  }

  /**
   * This method uses the product id from a Configurator PDP and if promoKey(s) exist, cycles through each returning a boolean value based on if the promoKey is a dollar off key NMOBLDS-5866
   */
  private Boolean promoKeyForDollarOffExist(final RepositoryItem product) {
    boolean dollarPromoExist = false;
    @SuppressWarnings("unchecked")
    final Set<String> promoKeys = (Set<String>) product.getPropertyValue(PROMO_KEYS);
    for (final String promoKey : promoKeys) {
      if (DOLLAR_PROMOTION.startsWith(promoKey)) {
        dollarPromoExist = true;
        break;
      }
    }
    return dollarPromoExist;
  }

  protected String findAltText(final RepositoryItem repItem) {
    if (repItem == null) {
      return "";
    }

    String thumbnailAltText = (String) repItem.getPropertyValue("thumbnailAltText");

    if (null == thumbnailAltText) {
      final StringBuffer altTextBuffer = new StringBuffer();

      try {
        final String webDesignerName = (String) repItem.getPropertyValue("cmDesignerName");

        if (StringUtilities.isNotBlank(webDesignerName) && !webDesignerName.equalsIgnoreCase("null")) {
          altTextBuffer.append(webDesignerName + " ");
        }

        final String displayName = (String) repItem.getPropertyValue("displayName");

        if (StringUtilities.isNotBlank(displayName) && !displayName.equalsIgnoreCase("null")) {
          altTextBuffer.append(displayName);
        }

        thumbnailAltText = altTextBuffer.toString();
        ((MutableRepositoryItem) repItem).setPropertyValue("thumbnailAltText", thumbnailAltText);
      } catch (final Exception e) {
        logError("findAltText in AdvertisePromotionManager FAILED with ", e);
      }
    }

    return thumbnailAltText;
  }

  /**
   * Process product, looping through promoKeys
   */
  private void processProduct(final RepositoryItem productRepItem, final boolean enablePreview, final String productType, Map<String, Object> inputParams, Map<String, Object> outputParams)
          throws Exception {
    try {
      processPriceAdorn(productRepItem);
      @SuppressWarnings("unchecked")
      final Set<String> promoKeys = (Set<String>) productRepItem.getPropertyValue(PROMO_KEYS);
      processPromoKeys(promoKeys, enablePreview, productRepItem, inputParams, outputParams);

    } catch (final RepositoryException re) {
      logError("AdvertisePromotionManager.processProduct: RepositoryException:", re);
    }
  }

  /**
   * Process product, looping through promoKeys
   *
   */
  @SuppressWarnings({"unchecked" , "rawtypes"})
  private void processProductPriceRange(final RepositoryItem productRepItem, final boolean enablePreview, final String productType, Map<String, Object> inputParams, Map<String, Object> outputParams)
          throws Exception {

    try {
      String productId = productRepItem.getRepositoryId();
      logDebug("process Product price range. productId=" + productId + " productType=" + productType);

      List<NMProduct> subProducts = new ArrayList<NMProduct>();
      thumbnailOutlineDisplayFlag = Boolean.FALSE;
      RepositoryItem percentOffpromoRepItem = null;
      boolean usePromopriceRangeDisplay = false;
      String countryCode = (String) inputParams.get(COUNTRY_CODE);
      List<String> alChildProdIds = null;

      if (productType.equals("product")) {
        NMProduct nmProduct = new NMProduct(productId);
        subProducts.add(nmProduct);

      } else if (productType.equals("suite")) {
        final NMSuite suite = new NMSuite(productId);
        subProducts = suite.getProductList();

      } else {
        final NMSuperSuite superSuite = new NMSuperSuite(productId);
        final List<NMSuite> suites = superSuite.getSuiteList();

        for (final NMSuite suite : suites) {
          subProducts.addAll(suite.getProductList());
        }
      }

      logDebug("sub products size=" + subProducts.size());
      logDebug("sub products = " + subProducts.toString());

      if (subProducts.size() > 0) {
        final ArrayList<String> priceList = new ArrayList<String>();

        if (StringUtilities.isNotBlank(getPageType()) && getPageType().equalsIgnoreCase("TEMPLATE") && StringUtilities.isNotBlank(productType) && SUITE.equalsIgnoreCase(productType)) {
          HashMap hmLinePromoDetails = new HashMap();
          outputParams.put(LINE_ITEM_PROMOTION_GROUPS, hmLinePromoDetails);
          String prodIds = (String) inputParams.get(CHILD_PROD_IN_GROUP);
          if (!StringUtilities.isNullOrEmpty(prodIds)) {
            String sChildProdIds[] = prodIds.split(",");
            if (sChildProdIds != null && sChildProdIds.length > 0) {
              alChildProdIds = new ArrayList<String>();
              alChildProdIds = Arrays.asList(sChildProdIds);
            }
          }
        }
        for (final NMProduct subProduct : subProducts) {
          String advertisedPrice = "";
          String advertisedMonogramPrice = "";
          String retailPrice = null;
          retailPrice = subProduct.getRetailPrice(!isDynamoRequest);
          logDebug("retailPrice = " + retailPrice);

          String specialInstructionFlag = subProduct.getSpecialInstructionFlag();
          logDebug("specialInstructionFlag = " + specialInstructionFlag);

          String monogramRetailPrice = EMPTY_STRING;

          if (specialInstructionFlag.equalsIgnoreCase(OPTIONAL_MONOGRAM_FLAG)) {
            monogramRetailPrice = subProduct.getMonogrammedPrice();

          } else {
            monogramRetailPrice = subProduct.getRetailPrice(!isDynamoRequest);
          }
          logDebug("monogramRetailPrice = " + monogramRetailPrice);

          final RepositoryItem repItem = subProduct.getDataSource();

          if (subProduct.getIsDisplayable()) {
            logDebug("sub product isDisplayable is true");
            final Set<String> promoKeys = (Set<String>) repItem.getPropertyValue(PROMO_KEYS);
            RepositoryItem subProductItem = CommonComponentHelper.getProdSkuUtil().getProductRepositoryItem(subProduct.getId());
            final PercentOffPromotion percentOffPromotion = processPromoKeysForPriceRangeCal(promoKeys, enablePreview, subProductItem, inputParams, outputParams);
            if (StringUtilities.isNotBlank(getPageType()) && getPageType().equalsIgnoreCase("TEMPLATE") && StringUtilities.isNotBlank(productType) && SUITE.equalsIgnoreCase(productType)) {
              if (percentOffPromotion != null && validateEligibleCountry(percentOffPromotion.getFlgEligible(), countryCode)) {

                if ("113".equals(percentOffPromotion.getType())) {
                  if (alChildProdIds != null && alChildProdIds.size() > 0 && alChildProdIds.contains(subProduct.getId())) {
                    processProductPromoGroups(percentOffPromotion.getCode(), enablePreview, true, subProduct.getId(), percentOffPromotion, null, inputParams, outputParams);
                  }
                } else {
                  processProductPromo(percentOffPromotion.getCode(), enablePreview, true, productRepItem, null, null, inputParams, outputParams);
                }
              }

              if (alChildProdIds != null && alChildProdIds.size() > 0 && alChildProdIds.contains(subProduct.getId())) {
                Double price = 0d;

                final Map<Object, Object> repoPricingAdornments = (Map<Object, Object>) repItem.getPropertyValue(PRICING_ADORNMENTS);

                final Object[] keys = repoPricingAdornments.keySet().toArray();

                for (int i = 0; i < keys.length; i++) {
                  final String position = (String) keys[i];
                  final RepositoryItem adornment = (RepositoryItem) repoPricingAdornments.get(position);
                  price = (Double) adornment.getPropertyValue("adornment_price");
                }

                HashMap hmChilds = (HashMap) outputParams.get(LINE_ITEM_PROMOTION_GROUPS);
                if (hmChilds != null) {
                  HashMap hmChild = (HashMap) hmChilds.get(subProduct.getId());
                  if (hmChild != null) {
                    hmChild.put(ADORNMENT_PRICE, price);
                  } else {
                    hmChild = new HashMap();
                    hmChild.put(ADORNMENT_PRICE, price);
                  }
                  hmChilds.put(subProduct.getId(), hmChild);
                }
              }

            } else {
              if (percentOffPromotion != null && validateEligibleCountry(percentOffPromotion.getFlgEligible(), countryCode)) {
                if ("113".equals(percentOffPromotion.getType())) {
                  logDebug("promo type is 113");

                  final RepositoryItem promoRepItem = advertisePromotionRepository.getItem(percentOffPromotion.getCode(), mPromotionsHelper.getAdvertisePromotionItemDescriptor());

                  if ((Boolean) promoRepItem.getPropertyValue("enablePromoPriceDisplay") && brandSpecs.isEnablePromoPriceDisplay()) {
                    advertisedPrice = getAdvertisedPromoPrice(percentOffPromotion.getPercentOff(), retailPrice);
                    logDebug("advertised price = " + advertisedPrice);

                    if (advertisedPrice != null && advertisedPrice != "") {
                      if (advertisedPrice.contains("$")) {
                        priceList.add(advertisedPrice.substring(1));

                      } else {
                        priceList.add(advertisedPrice);
                      }

                      usePromopriceRangeDisplay = true;
                      percentOffpromoRepItem = promoRepItem;

                      if (TEMPLATE.equals(pageType)) {
                        setPromoPriceDisplay(true);
                      }

                    } else {
                      priceList.add(retailPrice);
                    }

                    if (specialInstructionFlag.equalsIgnoreCase(OPTIONAL_MONOGRAM_FLAG) || specialInstructionFlag.equalsIgnoreCase(FREE_MONOGRAM_FLAG)) {
                      advertisedMonogramPrice = getAdvertisedPromoPrice(percentOffPromotion.getPercentOff(), monogramRetailPrice);
                      logDebug("advertisedMonogramPrice = " + advertisedMonogramPrice);
                      if (!StringUtilities.isEmpty(advertisedMonogramPrice)) {
                        if (advertisedMonogramPrice.contains(DOLLAR_SIGN)) {
                          priceList.add(advertisedMonogramPrice.substring(1));

                        } else {
                          priceList.add(advertisedMonogramPrice);
                        }

                      } else {
                        priceList.add(monogramRetailPrice);
                      }
                    }
                  }

                  if (!getPageType().equalsIgnoreCase(TEMPLATE)) {
                    processProductPromo(percentOffPromotion.getCode(), enablePreview, true, productRepItem, percentOffPromotion, null, inputParams, outputParams);
                  }

                } else {
                  priceList.add(retailPrice);
                  processProductPromo(percentOffPromotion.getCode(), enablePreview, true, productRepItem, null, null, inputParams, outputParams);

                  if (specialInstructionFlag.equalsIgnoreCase(OPTIONAL_MONOGRAM_FLAG) || specialInstructionFlag.equalsIgnoreCase(FREE_MONOGRAM_FLAG)) {
                    priceList.add(monogramRetailPrice);
                  }
                }

              } else {
                priceList.add(retailPrice);

                if (specialInstructionFlag.equalsIgnoreCase(OPTIONAL_MONOGRAM_FLAG) || specialInstructionFlag.equalsIgnoreCase(FREE_MONOGRAM_FLAG)) {
                  priceList.add(monogramRetailPrice);
                }
              }
            }
          }
        }

        logDebug("priceList = " + priceList.toString());

        String priceRange = "";
        if (usePromopriceRangeDisplay) {
          priceRange = calculatePriceRange(priceList);
          final String[] priceArray = priceRange.split("-");
          final DecimalFormat DECIMAL_FORMAT_ADVERTISE_PRICE = new DecimalFormat("######.00");
          final String minPrice = DECIMAL_FORMAT_ADVERTISE_PRICE.format(new Double(priceArray[0]));
          final String maxPrice = DECIMAL_FORMAT_ADVERTISE_PRICE.format(new Double(priceArray[1]));

          outputParams.put(PROMO_PRICE_RANGE_DISPLAY_PERCENT_OFF_TEXT, "NOW");
          outputParams.put(PROMO_PRICE_RANGE_DISPLAY_ADV_MIN_PRICE, minPrice);
          outputParams.put(PROMO_PRICE_RANGE_DISPLAY_ADV_MAX_PRICE, maxPrice);
          outputParams.put(USE_PROMO_PRICE_RANGE_DISPLAY, true);

          if (percentOffpromoRepItem != null) {
            final String pageType = getPageType();
            String htmlPropertyName = null;
            boolean promoDisplayType = false;

            if (ITEM.equals(pageType)) {
              htmlPropertyName = "itemHtml";
              promoDisplayType = false;

            } else if (PRODUCT.equals(pageType)) {
              htmlPropertyName = "productHtml";

            } else if (YMAL.equals(pageType)) {
              htmlPropertyName = "ymalHtml";
              promoDisplayType = false;

            } else if (CHECKOUT.equals(pageType)) {
              htmlPropertyName = "templateHtml";
              promoDisplayType = false;

            } else {
              htmlPropertyName = "templateHtml";
              promoDisplayType = true;
            }

            logDebug("htmlPropertyName = " + htmlPropertyName);
            final String htmlPropertyValue = (String) percentOffpromoRepItem.getPropertyValue(htmlPropertyName);

            displayOutlineAndHtmlText(enablePreview, percentOffpromoRepItem, false, pageType, htmlPropertyValue, promoDisplayType, false);
          }
        }
      }

    } catch (final Exception e) {
      logError("Exception at AdvertisePromotionManager.processProductPriceRange", e);
    }
  }

  private String calculatePriceRange(final ArrayList<String> priceList) {

    double minPrice = 0;
    double maxPrice = 0;
    String priceString = null;
    final List<Double> prices = new ArrayList<Double>();

    if (priceList != null && priceList.size() > 0) {
      for (final String string : priceList) {
        prices.add(new Double(string));
      }

      minPrice = Collections.min(prices);
      maxPrice = Collections.max(prices);
    }

    priceString = String.valueOf(minPrice) + "-" + String.valueOf(maxPrice);
    logDebug("price range : " + priceString);

    return priceString;
  }

  private void processPromoKeys(final Collection<String> promoKeysList, final boolean enablePreview, final RepositoryItem productRepItem, Map<String, Object> inputParams,
          Map<String, Object> outputParams) throws Exception {
    try {
      Set<String> promoKeys = null;
      if (NmoUtils.isNotEmptyCollection(promoKeysList)) {
        promoKeys = getPromokeysByPageType((Set<String>) promoKeysList);
      }
      if (NmoUtils.isNotEmptyCollection(promoKeys)) {
        boolean isPromoDisplayable = false;
        final ArrayList<PercentOffPromotion> percentOffPromotions = new ArrayList<PercentOffPromotion>();
        NMPromotion buyOnePromotion = null;
        PercentOffPromotion getOnePromotion = null;
        String countryCode = (String) inputParams.get(COUNTRY_CODE);

        Object persObj = (Object) inputParams.get(PERSONALIZED_OBJECT);
        List<String> persPromoCodes = null;
        if (persObj != null && brandSpecs.isEnableSitePersonalization()) {
          persPromoCodes = ((PersonalizedCustData) persObj).getPersonalizedPromos();
        }

        // Iterator through the list of promo keys and process them.
        // PercentOffPromotions are handled separately since we have to
        // find all of them in the collection and then pick the best one to three.
        for (final String promoKey : promoKeys) {
          logDebug("Processing promokey = " + promoKey);

          // Check to see if the promo key is associated with a PercentOffPromotion
          PercentOffPromotion percentOffPromotion = mPromotionsHelper.getPercentOffPromotionViaKey(promoKey);

          if (percentOffPromotion != null && percentOffPromotion.isPersonalizedFlag()) {
            if (!(promoKey != null && (persPromoCodes != null && persPromoCodes.contains(promoKey)))) {
              logDebug("Skipping the personalized promotion");
              continue;
            }
          }

          // If this key is a PercentOffPromotion then save it until later otherwise
          // go ahead and process the promo (once we have all the percent off promotions
          // we'll select the best one and only display it).
          if (percentOffPromotion != null) {
            if (validateEligibleCountry(percentOffPromotion.getFlgEligible(), countryCode)) {
              percentOffPromotions.add(percentOffPromotion);
            }

          } else {
            com.nm.collections.Promotion shippingPromotion = mPromotionsHelper.getShippingPromotionViaKey(promoKey);

            if (shippingPromotion != null && shippingPromotion.isPersonalizedFlag()) {
              if (!(promoKey != null && (persPromoCodes != null && persPromoCodes.contains(promoKey)))) {
                logDebug("Skipping the personalized promotion");
                continue;
              }
            }

            // If this key is for a 103 shipping promotion, it may be a pass-through
            // bogo promotion. If so, instead of adding the shipping promotion add a
            // temporary percent off promotion *if* the product doesn't ALREADY have
            // the associated BOGO promo
            if (shippingPromotion != null && "103".equals(shippingPromotion.getType())) {
              boolean okToProcess = true;
              Map<String, NMPromotion> allPercentOffPromos = ComponentUtils.getInstance().getPromotionArray(NMPromotionArray.PERCENT_OFF_ARRAY).getAllPromotions();
              Iterator<NMPromotion> promoIterator = allPercentOffPromos.values().iterator();
              while (promoIterator.hasNext()) {
                PercentOffPromotion percentOffPromo = (PercentOffPromotion) promoIterator.next();
                if (percentOffPromo.isBogo() && shippingPromotion.getCode().equals(percentOffPromo.getBuyOneGetOneQualifiedPromotion())) {
                  if (!promoKeys.contains(percentOffPromo.getCode())) {
                    if (mPromotionsHelper.validateDate(percentOffPromo) && validateEligibleCountry(percentOffPromo.getFlgEligible(), countryCode)) {
                      // this is a 103 for a BOGO and the product does not already
                      // have the bogo percent off promo, so save the buy one and
                      // get one for later consideration
                      if (getOnePromotion == null || new Integer(percentOffPromo.getPercentOff()) > new Integer(getOnePromotion.getPercentOff())) {
                        buyOnePromotion = shippingPromotion;
                        getOnePromotion = percentOffPromo;
                      }
                    }
                  }

                  okToProcess = false;
                  break;
                }
              }

              if (okToProcess) {
                processOtherPromotions(promoKey, enablePreview, productRepItem, inputParams, outputParams);
              }

            } else {
              processOtherPromotions(promoKey, enablePreview, productRepItem, inputParams, outputParams);
            }
          }
        }

        boolean useAdditionalPromoTextSlot = systemSpecs.isAdditionalPromoTextDisplayEnabled();

        // Get the first slot percent off promotion that has html to display
        final PercentOffPromotion firstSlotPercentOffPromo = mPromotionsHelper.getBestPercentOffPromotionWithHtml(percentOffPromotions, getPageType(), enablePreview, useAdditionalPromoTextSlot);

        PercentOffPromotion extraPercentOffPromotion = null;

        if (firstSlotPercentOffPromo != null) {
          isPromoDisplayable = true;

          // Look for a second slot percent off promo to advertise
          if ("113".equals(firstSlotPercentOffPromo.getType()) && useAdditionalPromoTextSlot) {
            final int maxDefaultPercentOff = systemSpecs.getDefaultMaxPercentoff();

            extraPercentOffPromotion = mPromotionsHelper.getExtraPercentOffPromotionWithHtml(percentOffPromotions, firstSlotPercentOffPromo, getPageType(), enablePreview, maxDefaultPercentOff);
          }

          processProductPromo(firstSlotPercentOffPromo.getCode(), enablePreview, isPromoDisplayable, productRepItem, firstSlotPercentOffPromo, extraPercentOffPromotion, inputParams, outputParams);
        }

        if (useAdditionalPromoTextSlot) {
          if (((TEMPLATE.equals(pageType)) || (ITEM.equals(pageType)) || (PRODUCT.equals(pageType)))) {
            addAdditionalPromoText(percentOffPromotions, firstSlotPercentOffPromo, enablePreview, extraPercentOffPromotion, buyOnePromotion, getOnePromotion, inputParams, outputParams);
          }
        }

      }
    } catch (final RepositoryException re) {
      logError("AdvertisePromotionManager.processProduct: RepositoryException:", re);
      re.printStackTrace();
    }
  }

  private void processOtherPromotions(final String promoKey, final boolean enablePreview, final RepositoryItem productRepItem, Map<String, Object> inputParams, Map<String, Object> outputParams) {

    try {
      boolean isPromoDisplayable = false;
      final GiftWithPurchaseArray gwpArray = CheckCsrGwp.getGiftWithPurchaseArray();
      final PurchaseWithPurchaseArray pwpArray = CheckCsrPwp.getPurchaseWithPurchaseArray();
      GiftWithPurchase gwpItem = null;
      PurchaseWithPurchase pwpItem = null;
      Promotion dollaroff = null;
      NMPromotion shippingPromotion = null;
      String countryCode = (String) inputParams.get(COUNTRY_CODE);

      Object persObj = (Object) inputParams.get(PERSONALIZED_OBJECT);
      List<String> persPromoCodes = null;
      if (persObj != null && brandSpecs.isEnableSitePersonalization()) {
        persPromoCodes = ((PersonalizedCustData) persObj).getPersonalizedPromos();
      }
      if ((gwpItem = gwpArray.getPromotion(promoKey)) != null) {
        // get the displayability of the gwp item, so as to control the display of the outlines and advertisements.
        final String gwpProductId = gwpItem.getGwpProduct();
        final NMProduct nmProduct = new NMProduct(gwpProductId);

        // force cutline promos to drop off(not display) when gwp inventory <= 10
        // Commented the "gwpInventory > 10" to reduce the thread block on production
        // final int gwpInventory = nmProduct.getTotalInventory();
        // isPromoDisplayable = nmProduct.getIsDisplayable() && gwpInventory > 10;
        isPromoDisplayable = nmProduct.getIsDisplayable();

      } else if ((pwpItem = pwpArray.getPromotion(promoKey)) != null && validateEligibleCountry(pwpItem.getFlgEligible(), countryCode)) {
        // get the displayability of the pwp item, so as to control the display of the outlines and advertisements.
        RepositoryItem productItem = CommonComponentHelper.getProductRepository().getItem(pwpItem.getPwpProduct(), "product");
        isPromoDisplayable = NmoUtils.getBooleanPropertyValue(productItem, ProductCatalogRepository.PRODUCT_FLAG_DISPLAY);

      } else if ((dollaroff = RuleHelper.getActiveDollarOffPromoKey(promoKey)) != null) {
        // Validate the dollar off promotion country eligible
        if (dollaroff != null && validateEligibleCountry(dollaroff.getFlgEligible(), countryCode)) {
          isPromoDisplayable = true;
          if (dollaroff.isPersonalizedFlag()) {
            if (!(promoKey != null && (persPromoCodes != null && persPromoCodes.contains(promoKey)))) {
              isPromoDisplayable = false;
              return;
            }
          }
        }

      } else if ((shippingPromotion = mPromotionsHelper.getShippingPromotionViaKey(promoKey)) != null) { // check for shipping promotions here.
        final String shippingPromoType = shippingPromotion.getType();
        // For 103,104 Shipping promotions,do country validation
        // Show promo cutlines only for eligible countries
        final String SystemCode = systemSpecs.getProductionSystemCode();
        if ((!StringUtilities.isEmpty(SystemCode) && !"LC".equalsIgnoreCase(SystemCode)) && ("103".equals(shippingPromoType) || "104".equals(shippingPromoType))) {
          if (mPromotionsHelper.validateCountryForShipping(shippingPromotion, countryCode)) {
            isPromoDisplayable = true;
          }

        } else {
          isPromoDisplayable = true;
        }

      } else if (mPromotionsHelper.getGiftWrapPromotionViaKey(promoKey)) {
        // check for giftwrap promotions here
        isPromoDisplayable = true;
      }

      processProductPromo(promoKey, enablePreview, isPromoDisplayable, productRepItem, null, null, inputParams, outputParams);

    } catch (final RepositoryException re) {
      logError("AdvertisePromotionManager.processProduct: RepositoryException:", re);

    } catch (final Exception e) {
      logError("Exception inside processOtherPromotions()", e);
    }
  }

  private PercentOffPromotion processPromoKeysForPriceRangeCal(final Collection<String> promoKeys, final boolean enablePreview, final RepositoryItem productRepItem, Map<String, Object> inputParams,
          Map<String, Object> outputParams) throws Exception {

    try {
      PercentOffPromotion percentOffPromotion = null;
      final ArrayList<PercentOffPromotion> percentOffPromotions = new ArrayList<PercentOffPromotion>();

      Object persObj = (Object) inputParams.get(PERSONALIZED_OBJECT);
      List<String> persPromoCodes = null;
      if (persObj != null && brandSpecs.isEnableSitePersonalization()) {
        persPromoCodes = ((PersonalizedCustData) persObj).getPersonalizedPromos();
      }

      // Iterator through the list of promo keys and process them.
      // PercentOffPromotions are handled separately since we have to
      // find all of them in the collection and then pick the best one.
      for (final String promoKey : promoKeys) {
        // Check to see if the promo key is associated with a PercentOffPromotion
        percentOffPromotion = mPromotionsHelper.getPercentOffPromotionViaKey(promoKey);

        if (percentOffPromotion != null && percentOffPromotion.isPersonalizedFlag()) {
          if (!(promoKey != null && (persPromoCodes != null && persPromoCodes.contains(promoKey)))) {
            logDebug("Skipping the personalized promotion");
            continue;
          }
        }

        // If this key is a PercentOffPromotion then save it until later otherwise
        // go ahead and process the promo (once we have all the percent off promotions
        // we'll select the best one and only display it).
        if (percentOffPromotion != null) {
          percentOffPromotions.add(percentOffPromotion);
        } else {
          // it's not a percentOffPromotion so check the other types
          processOtherPromotions(promoKey, enablePreview, productRepItem, inputParams, outputParams);
        }
      }

      // Get the best of the percent off promotions that have html to display and process it.
      percentOffPromotion = mPromotionsHelper.getBestPercentOffPromotionWithHtml(percentOffPromotions, getPageType(), enablePreview, false);

      return percentOffPromotion;

    } catch (final RepositoryException re) {
      logError("AdvertisePromotionManager.processProduct: RepositoryException:", re);
      re.printStackTrace();
    }

    return null;
  }

  /**
   * Checks to see whether the local user qualifies for the promotions to be displayed(Advertise) based on his COUNTRY_PREFERENCE . Supports both Neiman Marcus(US and Cannada) and FiftyOne countries
   * get NMProfile for the user and checks country eligibility for the promo
   */
  private boolean validateEligibleCountry(final Map<String, Boolean> countryEligibilityMap, String countryCode) throws PromoException {
    // if countryEligibilityMap contains key country AND key value is true, then display(Advertise) promo
    return !NmoUtils.isEmptyMap(countryEligibilityMap) ? countryEligibilityMap.get(countryCode).booleanValue() : false;
  }

  private String getAdvertisedPromoPrice(final String percentOff, final String price) {

    final boolean isFiftyOneEnabled = systemSpecs.isFiftyOneEnabled();
    String priceToDisplay = "";

    try {
      final PricingTools mPricingTools = CommonComponentHelper.getPricingTools();
      final double discountRate = Double.parseDouble(percentOff) / 100.0;
      final double retailPrice = ApiUtil.toDouble(price, 0d);
      final double roundedRetailPrice = mPricingTools.round(retailPrice);

      final double discountAmount = discountRate * roundedRetailPrice;
      final double roundedDiscountAmount = mPricingTools.round(discountAmount);
      final double newamnt = mPricingTools.round(roundedRetailPrice - roundedDiscountAmount);

      if (newamnt != 0) {
        priceToDisplay = (isFiftyOneEnabled) ? (newamnt + "") : (DOLLAR_SIGN + newamnt);
      }
    } catch (final Exception e) {
      logError("Exception in getAdvertisedPromoPrice method", e);
    }

    return priceToDisplay;
  }

  /**
   * Check to see if promotion is set to display on product page.
   */
  private void processProductPromo(final String promoKeyId, final boolean enablePreview, final boolean isPromoDisplayable, final RepositoryItem productRepItem,
          final PercentOffPromotion firstLinePercentOffPromo, final PercentOffPromotion extraPercentOffPromotion, Map<String, Object> inputParams, Map<String, Object> outputParams) {

    try {
      RepositoryItem promoRepItem = advertisePromotionRepository.getItem(promoKeyId, mPromotionsHelper.getAdvertisePromotionItemDescriptor());

      RepositoryItem extraPromoRepItem = null;
      templatePricingAdornmentFlag = Boolean.FALSE;
      templatePricingAdornmentText = "";
      templatePricingAdornmentColorFlag = Boolean.FALSE;
      templatePricingAdornmentColor = "";
      productPagePricingAdornmentFlag = Boolean.FALSE;
      productPagePricingAdornmentText = "";
      productPagePricingAdornmentColorFlag = Boolean.FALSE;
      productPagePricingAdornmentColor = "";
      checkoutPricingAdornmentFlag = Boolean.FALSE;
      checkoutPricingAdornmentText = "";
      checkoutPricingAdornmentColorFlag = Boolean.FALSE;
      checkoutPricingAdornmentColor = "";

      if (promoRepItem == null) {
        promoRepItem = advertisePromotionRepository.getItem(StringUtils.toUpperCase(promoKeyId), mPromotionsHelper.getAdvertisePromotionItemDescriptor());
        if (promoRepItem == null) {
          return;
        }
      }

      final String pageType = getPageType();
      final NMProduct nmProduct = new NMProduct(productRepItem);
      String retailPrice = nmProduct.getRetailPrice(!isDynamoRequest);
      final boolean isConfigurable = nmProduct.getFlgConfigurable();
      NMProductDisplayPrice productDisplayPrice = nmProduct.getProductDisplayPrice(); // added for NMOBLDS-3270
      // common method to get the property values for the given pageType property
      Map<String, Object> displayProperties = getDisplayProperties(pageType, isConfigurable, promoRepItem);
      // assigns the adornment price,flag and colors.This code change is for code seperation.
      assignPricingAdornMentFlags(outputParams, promoRepItem, true);
      Map<String, Object> processProperties = new HashMap<String, Object>();
      // Get the underlying promo Check to see if type is 113 (%Off without qualifiers) Check to see that the enablePromoPriceDisplay is true from advertise-promo setup
      // Get the retail price from the product Calculate the %Off and substract it from the retail price If the Adv promo has text label use that, else use what's in html fragment for default.
      outputParams.put(USE_PROMO_PRICE_DISPLAY, false);
      boolean percentOffPromotionsAreStackable = false;

      // stackable/non-stackable: the percent off is multiplicative
      if (firstLinePercentOffPromo != null && extraPercentOffPromotion != null && "113".equals(firstLinePercentOffPromo.getType())
              && ("113".equals(extraPercentOffPromotion.getType()) || "112".equals(extraPercentOffPromotion.getType()))
              && firstLinePercentOffPromo.getStackableFlag() != extraPercentOffPromotion.getStackableFlag()) {

        percentOffPromotionsAreStackable = true;
      }

      String advertisedPrice = "";
      String htmlExtraPromoPropertyValue = "";
      Boolean enablePreviewFlag = (Boolean) promoRepItem.getPropertyValue(ENABLE_PREVIEW_FLAG);

      putToMap(processProperties, htmlExtraPromoPropertyValue, advertisedPrice, null, retailPrice, (String) displayProperties.get("htmlPropertyName"),
              (String) displayProperties.get("htmlPropertyValue"), (String) inputParams.get(IS_QUICK_VIEW), (String) promoRepItem.getPropertyValue("itemHtml"), 0.0d, percentOffPromotionsAreStackable,
              (Boolean) displayProperties.get("displayPropertyValue"), (Boolean) displayProperties.get("promoDisplayType"), enablePreviewFlag, enablePreview, isConfigurable, productDisplayPrice);

      processPercentOffPromotionData(firstLinePercentOffPromo, promoRepItem, inputParams, outputParams, processProperties);
      processExtraPercentOffPromotionData(extraPercentOffPromotion, firstLinePercentOffPromo, inputParams, outputParams, extraPromoRepItem, promoRepItem, processProperties);

      String htmlPropertyValue = (String) processProperties.get("htmlPropertyValue");
      htmlExtraPromoPropertyValue = (String) processProperties.get("htmlExtraPromoPropertyValue");
      boolean displayHtml = (Boolean) processProperties.get("displayHtml");
      advertisedPrice = (String) processProperties.get("advertisedPrice");
      extraPromoRepItem = (RepositoryItem) processProperties.get("extraPromoRepItem");
      if ((TEMPLATE.equals(pageType) || YMAL.equals(pageType)) && "true".equals(outputParams.get(USE_PROMO_PRICE_DISPLAY).toString())) {

        assignPricingAdornMentFlags(outputParams, promoRepItem, false);// assigns the adornment price,flag and colors
        outputParams.put(ADORNMENT_PRICE, getAdornmentPrice());
        setPromoPriceDisplay(true);

        if (!enablePreviewFlag && enablePreview) {
          outputParams.put(USE_PROMO_PRICE_DISPLAY, false);
        }
        displayOutlineAndHtmlText(enablePreview, promoRepItem, false, pageType, htmlPropertyValue, isPromoDisplayable, false);

        if (extraPromoRepItem != null) {
          displayOutlineAndHtmlText(enablePreview, extraPromoRepItem, false, pageType, htmlExtraPromoPropertyValue, isPromoDisplayable, true);
        }
      } else {
        displayOutlineAndHtmlText(enablePreview, promoRepItem, displayHtml, pageType, htmlPropertyValue, isPromoDisplayable, false);
        // added for NMOBLDS-3270
        outputParams.put(ConfiguratorConstants.PRODUCT_PROMO_HTML_TEXT, htmlPropertyValue);

        if (extraPromoRepItem != null) {
          displayOutlineAndHtmlText(enablePreview, extraPromoRepItem, displayHtml, pageType, htmlExtraPromoPropertyValue, isPromoDisplayable, true);
        }
      }

      // added for NMOBLDS-3270
      if (isConfigurable && StringUtilities.isNotEmpty(advertisedPrice) && (firstLinePercentOffPromo != null || extraPercentOffPromotion != null)) {
        setConfiguratorRetailPrice(outputParams, isConfigurable, advertisedPrice);
      }

    } catch (final RepositoryException re) {
      logError("AdvertisePromotionManager.processProductPromo: RepositoryException:", re);
      re.printStackTrace();
    } catch (final NullPointerException npe) {
      logError("AdvertisePromotionManager.processProductPromo: NullPointerException:", npe);
      npe.printStackTrace();
    }
  }

  private Map<String, Object> getDisplayProperties(String pageType, boolean isConfigurator, RepositoryItem promoRepItem) {

    String displayPropertyName = "";
    String htmlPropertyName = "";
    boolean promoDisplayType = false;
    final Map<String, Object> displayProperties = new HashMap<String, Object>();

    if (ITEM.equals(pageType)) {
      displayPropertyName = "itemDisplayFlag";
      htmlPropertyName = "itemHtml";
      promoDisplayType = true;
    } else if (PRODUCT.equals(pageType)) {
      displayPropertyName = "productDisplayFlag";
      htmlPropertyName = "productHtml";

      // addded for NMOBLDS-3270
      if (isConfigurator) {
        promoDisplayType = true;
      }
    } else if (YMAL.equals(pageType)) {
      displayPropertyName = "ymalDisplayFlag";
      htmlPropertyName = "ymalHtml";
      promoDisplayType = true;
    } else if (CHECKOUT.equals(pageType)) {
      displayPropertyName = ENABLE_PROMO_PRICE_DISPLAY;
      htmlPropertyName = "templateHtml";
      promoDisplayType = true;

    } else {
      displayPropertyName = "templateDisplayFlag";
      htmlPropertyName = "templateHtml";
      promoDisplayType = true;
    }
    
    final boolean displayPropertyValue = NmoUtils.getBooleanPropertyValue(promoRepItem, displayPropertyName);
    final String htmlPropertyValue = (String) promoRepItem.getPropertyValue(htmlPropertyName);
    
    displayProperties.put("displayPropertyName", displayPropertyName);
    displayProperties.put("displayPropertyValue", displayPropertyValue);
    displayProperties.put("htmlPropertyName", htmlPropertyName);
    displayProperties.put("htmlPropertyValue", htmlPropertyValue);
    displayProperties.put("promoDisplayType", promoDisplayType);
    
    return displayProperties;
  }

  private Map<String, Object> processPercentOffPromotionData(PercentOffPromotion firstLinePercentOffPromo, RepositoryItem promoRepItem, Map<String, Object> inputParams,
          Map<String, Object> outputParams, Map<String, Object> processProperties) {

    final DecimalFormat DECIMAL_FORMAT_ADVERTISE_PRICE = new DecimalFormat("######.00");
    String htmlExtraPromoPropertyValue = (String) processProperties.get("htmlExtraPromoPropertyValue");
    String advertisedPrice = (String) processProperties.get("advertisedPrice");
    String promoPercentOffValue = (String) processProperties.get("promoPercentOffValue");
    String retailPrice = (String) processProperties.get("retailPrice");
    String htmlPropertyName = (String) processProperties.get("htmlPropertyName");
    String htmlPropertyValue = (String) processProperties.get("htmlPropertyValue");
    String isQuickView = (String) processProperties.get("isQuickView");
    String lineItemPropertyValue = (String) processProperties.get("lineItemPropertyValue");
    double multipliedPercentOff = (Double) processProperties.get("multipliedPercentOff");
    boolean percentOffPromotionsAreStackable = (Boolean) processProperties.get("percentOffPromotionsAreStackable");
    boolean displayHtml = (Boolean) processProperties.get("displayHtml");
    boolean promoDisplayType = (Boolean) processProperties.get("promoDisplayType");
    boolean enablePreviewFlag = (Boolean) processProperties.get("enablePreviewFlag");
    boolean enablePreview = (Boolean) processProperties.get("enablePreview");
    boolean isConfigurable = (Boolean) processProperties.get("isConfigurable");
    NMProductDisplayPrice productDisplayPrice = (NMProductDisplayPrice) processProperties.get("productDisplayPrice");

    if (((!enablePreviewFlag && !enablePreview) || (enablePreviewFlag && enablePreview) || (!enablePreview && enablePreviewFlag))) {

      if (firstLinePercentOffPromo != null) {

        if (!"113".equals(firstLinePercentOffPromo.getType())) {
          outputParams.put(PROMO_PRICE_DISPLAY_PERCENT_OFF_TEXT, htmlPropertyValue);
          outputParams.put(PROMO_PRICE_TEXT, htmlPropertyValue);
        }

        outputParams.put(PROMO_PERCENT_OFF_VALUE, firstLinePercentOffPromo.getPercentOff());
        // assigns the pricing adornment flags.this change is for code simplification.
        assignPricingAdornMentFlags(outputParams, promoRepItem, false);
        outputParams.put(ADORNMENT_PRICE, getAdornmentPrice());

        if (promoDisplayType && (Boolean) promoRepItem.getPropertyValue("enablePromoPriceDisplay") && brandSpecs.isEnablePromoPriceDisplay()) {

          String formattedAdvertisedPrice = "";
          // start NMOBLDS-3270
          String configuratorPromoMinMaxPrice = "";

          if (isConfigurable && ("true".equalsIgnoreCase(isQuickView) || pageType.equalsIgnoreCase(PRODUCT) || pageType.equalsIgnoreCase("template"))) {
            // passing configurator total price from configurator ajax call
            advertisedPrice = getConfiguratorPromoAppliedOnRetailPrice(firstLinePercentOffPromo, retailPrice, inputParams, outputParams);

            configuratorPromoMinMaxPrice = configuratorPromoAppliedOnMinMaxPrice(firstLinePercentOffPromo, productDisplayPrice, inputParams, outputParams);
            // end NMOBLDS-3270

          } else {
            advertisedPrice = getAdvertisedPromoPrice(firstLinePercentOffPromo.getPercentOff(), retailPrice);
          }

          if (!"".equals(advertisedPrice)) {
            if (advertisedPrice.contains("$")) {
              formattedAdvertisedPrice = DECIMAL_FORMAT_ADVERTISE_PRICE.format(new Double(advertisedPrice.substring(1)));
            } else {
              formattedAdvertisedPrice = DECIMAL_FORMAT_ADVERTISE_PRICE.format(new Double(advertisedPrice));
            }
          }

          setPromoPrice(formattedAdvertisedPrice);

          if (!"".equals(advertisedPrice)) {
            if (displayHtml && !StringUtilities.isEmpty(htmlPropertyValue)) {
              outputParams.put(USE_PROMO_PRICE_DISPLAY, true);

              if (isConfigurable && (pageType.equalsIgnoreCase("template") || ("true".equalsIgnoreCase(isQuickView)))) {
                outputParams.put(PROMO_PRICE_DISPLAY_ADVERTISED_PRICE, configuratorPromoMinMaxPrice);
                outputParams.put(PROMO_PRICE, configuratorPromoMinMaxPrice);
                outputParams.put(PROMO_PRICE_DISPLAY_PERCENT_OFF_TEXT, "NOW");
              } else {
                outputParams.put(PROMO_PRICE_DISPLAY_ADVERTISED_PRICE, formattedAdvertisedPrice);
                outputParams.put(PROMO_PRICE, formattedAdvertisedPrice);

                if (isConfigurable) {
                  outputParams.put(PROMO_PRICE_DISPLAY_PERCENT_OFF_TEXT, lineItemPropertyValue);
                  outputParams.put(PROMO_PRICE_TEXT, lineItemPropertyValue);
                } else {
                  outputParams.put(PROMO_PRICE_DISPLAY_PERCENT_OFF_TEXT, htmlPropertyValue);
                  outputParams.put(PROMO_PRICE_TEXT, htmlPropertyValue);
                }

                outputParams.put(CONFIGURATOR_LEFT_NAV_TOP_PROMO_TEXT, htmlPropertyValue);
              }
            } else if (isConfigurable && StringUtilities.isEmpty(htmlPropertyValue)) {
              outputParams.put(PROMO_PRICE_DISPLAY_ADVERTISED_PRICE, formattedAdvertisedPrice);
              outputParams.put(PROMO_PRICE, formattedAdvertisedPrice);
              outputParams.put(PROMO_PRICE_DISPLAY_PERCENT_OFF_TEXT, "NOW");
              outputParams.put(CONFIGURATOR_LEFT_NAV_TOP_PROMO_TEXT, INMGenericConstants.EMPTY_STRING);
            }
          }
        }
      }

    }

    putToMap(processProperties, htmlExtraPromoPropertyValue, advertisedPrice, promoPercentOffValue, retailPrice, htmlPropertyName, htmlPropertyValue, isQuickView, lineItemPropertyValue,
            multipliedPercentOff, percentOffPromotionsAreStackable, displayHtml, promoDisplayType, enablePreviewFlag, enablePreview, isConfigurable, productDisplayPrice);

    return processProperties;

  }

  private Map<String, Object> processExtraPercentOffPromotionData(PercentOffPromotion extraPercentOffPromotion, PercentOffPromotion firstLinePercentOffPromo, Map<String, Object> inputParams,
          Map<String, Object> outputParams, RepositoryItem extraPromoRepItem, RepositoryItem promoRepItem, Map<String, Object> processProperties) {

    final DecimalFormat DECIMAL_FORMAT_ADVERTISE_PRICE = new DecimalFormat("######.00");
    String htmlExtraPromoPropertyValue = (String) processProperties.get("htmlExtraPromoPropertyValue");
    String advertisedPrice = (String) processProperties.get("advertisedPrice");
    String promoPercentOffValue = (String) processProperties.get("promoPercentOffValue");
    String retailPrice = (String) processProperties.get("retailPrice");
    String htmlPropertyName = (String) processProperties.get("htmlPropertyName");
    String htmlPropertyValue = (String) processProperties.get("htmlPropertyValue");
    String isQuickView = (String) processProperties.get("isQuickView");
    String lineItemPropertyValue = (String) processProperties.get("lineItemPropertyValue");
    double multipliedPercentOff = (Double) processProperties.get("multipliedPercentOff");
    boolean percentOffPromotionsAreStackable = (Boolean) processProperties.get("percentOffPromotionsAreStackable");
    boolean displayHtml = (Boolean) processProperties.get("displayHtml");
    boolean promoDisplayType = (Boolean) processProperties.get("promoDisplayType");
    boolean enablePreviewFlag = (Boolean) processProperties.get("enablePreviewFlag");
    boolean enablePreview = (Boolean) processProperties.get("enablePreview");
    boolean isConfigurable = (Boolean) processProperties.get("isConfigurable");
    NMProductDisplayPrice productDisplayPrice = (NMProductDisplayPrice) processProperties.get("productDisplayPrice");

    if (extraPercentOffPromotion != null) {
      try {
        outputParams.put(PROMO_EXTRA_PERCENT_OFF_VALUE, extraPercentOffPromotion.getPercentOff());
        extraPromoRepItem = advertisePromotionRepository.getItem(extraPercentOffPromotion.getCode(), mPromotionsHelper.getAdvertisePromotionItemDescriptor());
        htmlExtraPromoPropertyValue = (String) extraPromoRepItem.getPropertyValue(htmlPropertyName);

        if (ITEM.equals(pageType)) {
          boolean displayPromoColorFlag = NmoUtils.getBooleanPropertyValue(extraPromoRepItem, "linePromoColorFlag");
          if (displayPromoColorFlag) {
            updateStackingPromoColor((String) extraPromoRepItem.getPropertyValue("linePromoColor"), displayPromoColorFlag);
          }
        } else {
          boolean displayPromoColorFlag = NmoUtils.getBooleanPropertyValue(extraPromoRepItem, "thumbnailPromoColorFlag");
          if (displayPromoColorFlag) {
            updateStackingPromoColor((String) extraPromoRepItem.getPropertyValue("thumbnailPromoColor"), displayPromoColorFlag);
          }
        }

        if (promoDisplayType && "113".equals(extraPercentOffPromotion.getType()) && (Boolean) extraPromoRepItem.getPropertyValue("enablePromoPriceDisplay") && brandSpecs.isEnablePromoPriceDisplay()) {

          String formattedAdvertisedPrice = "";

          if (percentOffPromotionsAreStackable) {
            multipliedPercentOff = PromotionsHelper.getStackedPercentOff(firstLinePercentOffPromo, extraPercentOffPromotion, true);
            promoPercentOffValue = Double.toString(multipliedPercentOff);
          } else {
            promoPercentOffValue = extraPercentOffPromotion.getPercentOff();
          }

          advertisedPrice = getAdvertisedPromoPrice(promoPercentOffValue, retailPrice);

          if (!"".equals(advertisedPrice)) {
            if (advertisedPrice.contains(DOLLAR_SIGN)) {
              formattedAdvertisedPrice = DECIMAL_FORMAT_ADVERTISE_PRICE.format(new Double(advertisedPrice.substring(1)));

            } else {
              formattedAdvertisedPrice = DECIMAL_FORMAT_ADVERTISE_PRICE.format(new Double(advertisedPrice));
            }
          }
          if (displayHtml && !StringUtilities.isEmpty(htmlExtraPromoPropertyValue)) {
            if (NmoUtils.getBooleanPropertyValue(promoRepItem, ENABLE_PROMO_PRICE_DISPLAY)) {
              outputParams.put(EXTRA_PROMO_PRICE_DISPLAY_ADV_PRICE, formattedAdvertisedPrice);
              outputParams.put(EXTRA_PROMO_PRICE_DISPLAY_PERCENT_OFF_TEXT, htmlExtraPromoPropertyValue);

            } else {
              outputParams.put(USE_PROMO_PRICE_DISPLAY, true);
              outputParams.put(PROMO_PRICE_DISPLAY_PERCENT_OFF_TEXT, htmlExtraPromoPropertyValue);
              outputParams.put(PROMO_PRICE_DISPLAY_ADVERTISED_PRICE, formattedAdvertisedPrice);
            }
          }

        } else if (promoDisplayType && displayHtml && !StringUtilities.isEmpty(htmlExtraPromoPropertyValue)) {
          if (NmoUtils.getBooleanPropertyValue(promoRepItem, ENABLE_PROMO_PRICE_DISPLAY)) {
            outputParams.put(EXTRA_PROMO_PRICE_DISPLAY_PERCENT_OFF_TEXT, htmlExtraPromoPropertyValue);

          } else {
            outputParams.put(USE_PROMO_PRICE_DISPLAY, true);
            outputParams.put(PROMO_PRICE_DISPLAY_PERCENT_OFF_TEXT, htmlExtraPromoPropertyValue);
          }
        }
      } catch (RepositoryException e) {

      }
    }
    processProperties.put("extraPromoRepItem", extraPromoRepItem);

    putToMap(processProperties, htmlExtraPromoPropertyValue, advertisedPrice, promoPercentOffValue, retailPrice, htmlPropertyName, htmlPropertyValue, isQuickView, lineItemPropertyValue,
            multipliedPercentOff, percentOffPromotionsAreStackable, displayHtml, promoDisplayType, enablePreviewFlag, enablePreview, isConfigurable, productDisplayPrice);

    return processProperties;
  }

  private void assignPricingAdornMentFlags(Map<String, Object> outputParams, RepositoryItem promoRepItem, boolean initialize) {
    if (initialize) {

      templatePricingAdornmentFlag = (Boolean) promoRepItem.getPropertyValue(TEMPLATE_PRICING_ADORNMENT_FLAG);
      templatePricingAdornmentText = (String) promoRepItem.getPropertyValue(TEMPLATE_PRICING_ADORNMENT_TEXT);
      templatePricingAdornmentColorFlag = (Boolean) promoRepItem.getPropertyValue(TEMPLATE_PRICING_ADORNMENT_COLOR_FLAG);
      templatePricingAdornmentColor = (String) promoRepItem.getPropertyValue(TEMPLATE_PRICING_ADORNMENT_COLOR);
      productPagePricingAdornmentFlag = (Boolean) promoRepItem.getPropertyValue(PRODUCTPAGE_PRICING_ADORNMENT_FLAG);
      productPagePricingAdornmentText = (String) promoRepItem.getPropertyValue(PRODUCTPAGE_PRICING_ADORNMENT_TEXT);
      productPagePricingAdornmentColorFlag = (Boolean) promoRepItem.getPropertyValue(PRODUCTPAGE_PRICING_ADORNMENT_COLOR_FLAG);
      productPagePricingAdornmentColor = (String) promoRepItem.getPropertyValue(PRODUCTPAGE_PRICING_ADORNMENT_COLOR);
      checkoutPricingAdornmentFlag = (Boolean) promoRepItem.getPropertyValue(CHECKOUT_PRICING_ADORNMENT_FLAG);
      checkoutPricingAdornmentText = (String) promoRepItem.getPropertyValue(CHECKOUT_PRICING_ADORNMENT_TEXT);
      checkoutPricingAdornmentColorFlag = (Boolean) promoRepItem.getPropertyValue(CHECKOUT_PRICING_ADORNMENT_COLOR_FLAG);
      checkoutPricingAdornmentColor = (String) promoRepItem.getPropertyValue(CHECKOUT_PRICING_ADORNMENT_COLOR);

    } else {

      outputParams.put(TEMPLATE_PRICING_ADORNMENT_FLAG, templatePricingAdornmentFlag);
      outputParams.put(TEMPLATE_PRICING_ADORNMENT_TEXT, templatePricingAdornmentText);
      outputParams.put(TEMPLATE_PRICING_ADORNMENT_COLOR_FLAG, templatePricingAdornmentColorFlag);
      outputParams.put(TEMPLATE_PRICING_ADORNMENT_COLOR, templatePricingAdornmentColor);
      outputParams.put(PRODUCTPAGE_PRICING_ADORNMENT_FLAG, productPagePricingAdornmentFlag);
      outputParams.put(PRODUCTPAGE_PRICING_ADORNMENT_TEXT, productPagePricingAdornmentText);
      outputParams.put(PRODUCTPAGE_PRICING_ADORNMENT_COLOR_FLAG, productPagePricingAdornmentColorFlag);
      outputParams.put(PRODUCTPAGE_PRICING_ADORNMENT_COLOR, productPagePricingAdornmentColor);
      outputParams.put(CHECKOUT_PRICING_ADORNMENT_FLAG, checkoutPricingAdornmentFlag);
      outputParams.put(CHECKOUT_PRICING_ADORNMENT_TEXT, checkoutPricingAdornmentText);
      outputParams.put(CHECKOUT_PRICING_ADORNMENT_COLOR_FLAG, checkoutPricingAdornmentColorFlag);
      outputParams.put(CHECKOUT_PRICING_ADORNMENT_COLOR, checkoutPricingAdornmentColor);
    }
  }

  private void putToMap(Map<String, Object> processProperties, String htmlExtraPromoPropertyValue, String advertisedPrice, String promoPercentOffValue, String retailPrice, String htmlPropertyName,
          String htmlPropertyValue, String isQuickView, String lineItemPropertyValue, double multipliedPercentOff, boolean percentOffPromotionsAreStackable, boolean displayHtml,
          boolean promoDisplayType, boolean enablePreviewFlag, boolean enablePreview, boolean isConfigurable, NMProductDisplayPrice productDisplayPrice) {

    processProperties.put("htmlExtraPromoPropertyValue", htmlExtraPromoPropertyValue);
    processProperties.put("advertisedPrice", advertisedPrice);
    processProperties.put("multipliedPercentOff", multipliedPercentOff);
    processProperties.put("percentOffPromotionsAreStackable", percentOffPromotionsAreStackable);
    processProperties.put("promoPercentOffValue", promoPercentOffValue);
    processProperties.put("retailPrice", retailPrice);
    processProperties.put("displayHtml", displayHtml);
    processProperties.put("promoDisplayType", promoDisplayType);
    processProperties.put("htmlPropertyName", htmlPropertyName);
    processProperties.put("enablePreviewFlag", enablePreviewFlag);
    processProperties.put("enablePreview", enablePreview);
    processProperties.put("htmlPropertyValue", htmlPropertyValue);
    processProperties.put("isConfigurable", isConfigurable);
    processProperties.put("isQuickView", isQuickView);
    processProperties.put("productDisplayPrice", productDisplayPrice);
    processProperties.put("lineItemPropertyValue", lineItemPropertyValue);

  }

  /*
   * private void getFromMap(Map<String, Object> processProperties, String htmlExtraPromoPropertyValue, String advertisedPrice, String promoPercentOffValue, String retailPrice, String
   * htmlPropertyName, String htmlPropertyValue, String isQuickView, String lineItemPropertyValue, double multipliedPercentOff, boolean percentOffPromotionsAreStackable, boolean displayHtml, boolean
   * promoDisplayType, boolean enablePreviewFlag, boolean enablePreview, boolean isConfigurable, NMProductDisplayPrice productDisplayPrice ){
   *
   * }
   */

  private void setConfiguratorRetailPrice(Map<String, Object> outputParams, boolean isConfigurable, String advertisedPrice) {
    final DecimalFormat DECIMAL_FORMAT_ADVERTISE_PRICE = new DecimalFormat("######.00");
    final PricingTools mPricingTools = CommonComponentHelper.getPricingTools();

    try {
      outputParams.put(ConfiguratorConstants.CONFIGURATOR_RETAIL_PRICE, DECIMAL_FORMAT_ADVERTISE_PRICE.format(mPricingTools.round(Double.valueOf(advertisedPrice))));
    } catch (Exception e) {
      logError("Caught exception while setting up configurator retail price value in request parameter:", e);
    }

  }

  /**
   * Gets the configurator promo applied on retail price.
   *
   * @param percentOff113Promotion
   *          the percent off113 promotion
   * @param retailPrice
   *          the retail price
   * @param currentRequest
   *          the current request
   * @return the configurator promo applied on retail price
   */
  private String getConfiguratorPromoAppliedOnRetailPrice(final PercentOffPromotion percentOff113Promotion, String retailPrice, Map<String, Object> inputParams, Map<String, Object> outputParams) {

    String configuratorTotalPrice = (String) inputParams.get(ConfiguratorConstants.CONFIGURATOR_TOTAL_PRICE);

    if (StringUtilities.isNotBlank(configuratorTotalPrice)) {
      retailPrice = getAdvertisedPromoPrice(percentOff113Promotion.getPercentOff(), configuratorTotalPrice);
    }

    return retailPrice;
  }

  /**
   * Gets the configurator promo min max price.
   *
   * @param percentOff113Promotion
   *          the percent off113 promotion
   * @param productDisplayPrice
   *          the product display price
   * @param DECIMAL_FORMAT_ADVERTISE_PRICE
   *          the decimal format advertise price
   * @return the configurator promo min max price
   */
  private String configuratorPromoAppliedOnMinMaxPrice(final PercentOffPromotion percentOff113Promotion, NMProductDisplayPrice productDisplayPrice, Map<String, Object> inputParams,
          Map<String, Object> outputParams) {

    String configuratorAdvertisedPromoMinPrice;
    String configuratorAdvertisedPromoMaxPrice;
    DecimalFormat decimalFormat = new DecimalFormat("######.00");
    String currencyPreference = "";
    String nmCurrencyPreference = (String) inputParams.get(CURRENCY_CODE);

    if (!nmCurrencyPreference.equalsIgnoreCase(INMGenericConstants.US_CURRENCY_CODE)) {
      currencyPreference = INMGenericConstants.STRING_DOUBLE_SPACE;
    }

    final String minPrice = decimalFormat.format(new Double(productDisplayPrice.getlowestPriced()));
    final String maxPrice = decimalFormat.format(new Double(productDisplayPrice.gethighestPriced()));
    configuratorAdvertisedPromoMinPrice = getAdvertisedPromoPrice(percentOff113Promotion.getPercentOff(), minPrice);
    configuratorAdvertisedPromoMaxPrice = getAdvertisedPromoPrice(percentOff113Promotion.getPercentOff(), maxPrice);
    StringBuffer sbConfiguratorPromoMinMaxPrice = new StringBuffer();

    sbConfiguratorPromoMinMaxPrice.append(currencyPreference).append(
            ProductPriceHelper.generatePriceStringForConfiguratorProducts(configuratorAdvertisedPromoMinPrice, configuratorAdvertisedPromoMaxPrice));

    return sbConfiguratorPromoMinMaxPrice.toString();
  }

  public void displayOutlineAndHtmlText(final boolean enablePreview, final RepositoryItem promoRepItem, final boolean displayHtml, final String pageType, final String htmlPropertyValue,
          final boolean isPromoDisplayable, final boolean isExtraPromoText) {
    // See if preview is enabled at the session level.If so then we don't need to worry about the dates.
    if (enablePreview) {
      // See if preview is enabled at the promotion level.
      // If so then we don't need to worry about displaying outlines and Htmltext.
      Boolean propertyValue = (Boolean) promoRepItem.getPropertyValue(ENABLE_PREVIEW_FLAG);
      final boolean enablePreviewFlag = propertyValue.booleanValue();

      if (enablePreviewFlag) {
        final int productPageSort = ((Integer) promoRepItem.getPropertyValue("productPageSort")).intValue();

        if (displayHtml) {
          addHtmlMap(productPageSort, htmlPropertyValue);
        }
        if (!isExtraPromoText) {
          if (TEMPLATE.equals(pageType)) {
            final boolean displayThumbnailOutline = NmoUtils.getBooleanPropertyValue(promoRepItem, "thumbnailOutlineDisplayFlag");
            if (displayThumbnailOutline) {
              updateThumbnailOutlineColor(productPageSort, (String) promoRepItem.getPropertyValue("thumbnailOutlineColor"), displayThumbnailOutline);
            }
            updatePromoColor(promoRepItem, pageType);
          }
          updatePromoColor(promoRepItem, pageType);
        }
      }

    } else {

      final Object markStartDate = promoRepItem.getPropertyValue("markStartDate");
      final Object markEndDate = promoRepItem.getPropertyValue("markEndDate");

      if (markStartDate instanceof Timestamp && markEndDate instanceof Timestamp) {
        // verify that the marketing date range is valid
        if (verifyDateRange((Date) markStartDate, (Date) markEndDate)) {
          final int productPageSort = ((Integer) promoRepItem.getPropertyValue("productPageSort")) != null ? ((Integer) promoRepItem.getPropertyValue("productPageSort")).intValue() : 0;
          if (TEMPLATE.equals(pageType)) {
            final boolean showTemplateText = NmoUtils.getBooleanPropertyValue(promoRepItem, "templateDisplayFlag");
            if (showTemplateText && isPromoDisplayable) {
              if (displayHtml) {
                addHtmlMap(productPageSort, htmlPropertyValue);
              }
            }
            if (!isExtraPromoText) {
              final boolean displayThumbnailOutline = NmoUtils.getBooleanPropertyValue(promoRepItem, "thumbnailOutlineDisplayFlag");
              if (displayThumbnailOutline && isPromoDisplayable) {
                updateThumbnailOutlineColor(productPageSort, (String) promoRepItem.getPropertyValue("thumbnailOutlineColor"), displayThumbnailOutline);
              }
              updatePromoColor(promoRepItem, pageType);
            }
          } else {
            if (isPromoDisplayable) {
              if (displayHtml) {
                addHtmlMap(productPageSort, htmlPropertyValue);
              }
              if (!isExtraPromoText) {
                updatePromoColor(promoRepItem, pageType);
              }
            }
          }
        }
      }
    }
  }

  private void updatePromoColor(final RepositoryItem promoRepItem, final String pageType) {
    if (YMAL.equals(pageType)) {
      boolean displayymalPromoColorFlag = NmoUtils.getBooleanPropertyValue(promoRepItem, "ymalPromoColorFlag");
      if (displayymalPromoColorFlag) {
        mYmalPromoColor = (String) promoRepItem.getPropertyValue("ymalPromoColor");
        ymalPromoColorFlag = Boolean.valueOf(displayymalPromoColorFlag);
      }

    } else {
      boolean displaythumbnailPromoColorFlag = NmoUtils.getBooleanPropertyValue(promoRepItem, "thumbnailPromoColorFlag");
      if (displaythumbnailPromoColorFlag) {
        mThumbnailPromoColor = (String) promoRepItem.getPropertyValue("thumbnailPromoColor");
        thumbnailPromoColorFlag = Boolean.valueOf(displaythumbnailPromoColorFlag);
      }
      boolean displaylinePromoColorFlag = NmoUtils.getBooleanPropertyValue(promoRepItem, "linePromoColorFlag");
      if (displaylinePromoColorFlag) {
        mLinePromoColor = (String) promoRepItem.getPropertyValue("linePromoColor");
        linePromoColorFlag = Boolean.valueOf(displaylinePromoColorFlag);
      }
    }
  }

  public CSRPromotionsHelper getCsrPromotionsHelper() {
    return csrPromotionsHelper;
  }

  public void setCsrPromotionsHelper(final CSRPromotionsHelper csrPromotionsHelper) {
    this.csrPromotionsHelper = csrPromotionsHelper;
  }

  protected void addNullPromo(final String promoKeyId, final String productId) {
    Date fsDate = null;
    Date feDate = null;

    final DateFormat df = new SimpleDateFormat("MMM-dd-yyyy");

    try {
      fsDate = df.parse("May-05-3011");
      feDate = df.parse("May-06-3011");

    } catch (final ParseException pe) {
      logError("--* APHTMLL Exception addNullPromo thrown while formatting dates: ", pe);
    }

    final String pName = "NOTFOUND";
    logDebug("***** APHTMLL addNullPromo of promoKey:" + promoKeyId);

    try {
      csrPromotionsHelper.createAdvertisePromotion(promoKeyId, pName, fsDate, feDate);

    } catch (final Exception e) {
      logError("APHTMLL Exception addNullPromo thrown while createAdvertisePromotion with key:" + promoKeyId + "***", e);
    }
  }

  // It is not being used.
  protected void processOutLines(final RepositoryItem adPromo, final boolean enablePreview) {

    if (enablePreview) {
      setandDisplayThumbnailOutlineandColor(adPromo);

    } else {
      final Object markStartDate = adPromo.getPropertyValue("markStartDate");
      final Object markEndDate = adPromo.getPropertyValue("markEndDate");

      if (markStartDate instanceof Timestamp && markEndDate instanceof Timestamp) {

        // verify that the marketing date range is valid
        if (verifyDateRange((Date) markStartDate, (Date) markEndDate)) {
          setandDisplayThumbnailOutlineandColor(adPromo);
        }
      }
    }
  }

  /**
   * It is not being used
   *
   * @param adPromo
   */
  private void setandDisplayThumbnailOutlineandColor(final RepositoryItem adPromo) {
    thumbnailOutlineDisplayFlag = Boolean.FALSE;
    if (adPromo == null) {
      return;
    }

    final Object o = adPromo.getPropertyValue("thumbnailOutlineDisplayFlag");

    if (o != null) {
      if (o instanceof Boolean) {
        thumbnailOutlineDisplayFlag = (Boolean) o;
      }
    }
  }

  /**
   * Verify that current date is between date1 and date2.
   */
  private boolean verifyDateRange(final Date date1, final Date date2) {
    boolean inDateRange = false;

    if (date1 != null && date2 != null) {
      final Date currDate = new Date();

      if (date1.getTime() <= currDate.getTime() && currDate.getTime() <= date2.getTime()) {
        inDateRange = true;
      }
    }

    return inDateRange;
  }
  
  /**
   * Keep track of the closest upcoming promo start or end among all the promotions for a product. This will be used for invalidating the cache when a promotion starts or expires.
   *
   * @param startDate
   * @param endDate
   */
  private void updateNextPromoDate(final Date startDate, final Date endDate) {
    final Date currDate = new Date();
    if (startDate.after(currDate) && (null == nextPromoDate || startDate.before(nextPromoDate))) {
      nextPromoDate = startDate;
    } else if (endDate.after(currDate) && (null == nextPromoDate || endDate.before(nextPromoDate))) {
      nextPromoDate = endDate;
    }
    return;
  }
  
  /**
   * Verify merchandise type.Should never advertise a promotion on any type of gift card.
   */
  protected boolean validMerchType(final RepositoryItem productItem) {
    String merchType = "";

    if (productItem != null) {
      // determine if it is a product, suite, or super suite
      final Integer productType = (Integer) productItem.getPropertyValue(TYPE);

      if (productType != null) {
        if (productType.intValue() == 0) { // productType: Product = 0, Suite = 1, SuperSuite = 2
          merchType = (String) productItem.getPropertyValue("merchandiseType");

          if (merchType == null) {
            merchType = "";
          }
        }
      }
    }

    if (merchType.equals(GIFT_CARD) || merchType.equals(VIRTUAL_GIFT_CARD)) {
      return false;

    } else {
      return true;
    }
  }

  /**
   * Return HTML string to be added to the product or template page
   */
  private String getHtmlString(final boolean includeModal) {
    final StringBuffer outputString = new StringBuffer("");
    final Set<Map.Entry<Integer, String>> set = getHtmlMap().entrySet();
    final Iterator<Map.Entry<Integer, String>> iterator = set.iterator();

    // This map is used to detect duplicate html text between promotions.
    final HashMap<String, String> duplicateChecker = new HashMap<String, String>();

    if (iterator.hasNext()) {
      Map.Entry<Integer, String> entry = iterator.next();
      String htmlText = entry.getValue();

      if (htmlText != null) {
        // The key to detecting the duplicates is a lower case version
        // of the html with all the spaces removed.
        String key = htmlText.toLowerCase().replaceAll("\\s+", "");

        if (promoTextList != null && promoTextList.size() > 0) {
          if (promoTextList.contains(htmlText) && (TEMPLATE.equals(pageType) || ITEM.equals(pageType))) {
            promoTextList.remove(htmlText);
            htmlText = "";
          }
        }

        // Put the first promotion's key into the duplicate checker.
        duplicateChecker.put(key, key);

        // Add the text to the output string.
        if (includeModal) {
          outputString.append(buildModalString(htmlText));

        } else {
          outputString.append(htmlText);
        }

        while (iterator.hasNext()) {
          entry = iterator.next();
          htmlText = entry.getValue();
          key = htmlText.toLowerCase().replaceAll("\\s+", "");

          if (promoTextList != null && promoTextList.size() > 0) {
            if (promoTextList.contains(htmlText) && (TEMPLATE.equals(pageType) || ITEM.equals(pageType))) {
              promoTextList.remove(htmlText);
              htmlText = "";
            }
          }

          // Check to see if the html key matches a promotion that we
          // have already added to the output string.If we have
          // seen this html before then add it to the output string.
          if (!duplicateChecker.containsKey(key)) {
            if (includeModal) {
              outputString.append(buildModalString(htmlText));

            } else if (!"".equals(htmlText)) {
              outputString.append("<br> ").append(htmlText);
            }

            duplicateChecker.put(key, key);
          }
        }
      }
    }

    if (promoTextList != null && promoTextList.size() > 0) {
      promoTextList.clear();
    }

    return outputString.toString();
  }

  /**
   * Add one or two promotions to the "additionalPromoDisplayPercentOffText" parameter. Considerations are the best bogo promotion, the first and second line promotions, the best "other" promotion
   * (non bogo, non 112/113 stackable promotion).
   *
   * @param percentOffPromotions
   *          all percent off promotions for this product
   * @param firstSlotPromotion
   *          a 113 if the product has one
   * @param extraPromotion
   *          a stackable promotion if the product has one
   * @param buyOnePromotion
   *          if the product has a buy one promotion
   * @param getOnePromotion
   *          if the product has a buy one promotion, this will be the associated get one (with the highest percent off)
   */
  private void addAdditionalPromoText(final List<PercentOffPromotion> percentOffPromotions, PercentOffPromotion firstSlotPromotion, final boolean enablePreview, PercentOffPromotion extraPromotion,
          NMPromotion buyOnePromotion, PercentOffPromotion getOnePromotion, Map<String, Object> inputParams, Map<String, Object> outputParams) {

    final String htmlPropertyName = pageType + HTML;
    String displayText = "";
    String alternateDisplayText = "";
    int percentOffPromotionsSize = percentOffPromotions.size();
    boolean percentOffPromotionsAreStackable = false;

    // stackable/non-stackable: the percent off is multiplicative
    if (firstSlotPromotion != null && extraPromotion != null && "113".equals(firstSlotPromotion.getType()) && ("113".equals(extraPromotion.getType()) || "112".equals(extraPromotion.getType()))
            && firstSlotPromotion.getStackableFlag() != extraPromotion.getStackableFlag()) {
      percentOffPromotionsAreStackable = true;
    }

    // we will only be stackable if there is no BOGO and no better "other" promo, so if
    // we are stackable we can safely fall out now
    if (percentOffPromotionsAreStackable) {
      return;
    }

    try {
      PercentOffPromotion bestBogoPromo = null;

      if (getOnePromotion != null && buyOnePromotion != null) {
        RepositoryItem promoRepoItem = advertisePromotionRepository.getItem(buyOnePromotion.getCode(), mPromotionsHelper.getAdvertisePromotionItemDescriptor());

        if (mPromotionsHelper.doesAdvertisePromotionHaveHtml(promoRepoItem, pageType)) {
          bestBogoPromo = getOnePromotion;
        }
      }

      // find best bogo and best "other" promo
      PercentOffPromotion bestOtherPromo = null;
      promoTextList = new ArrayList<String>();

      for (final PercentOffPromotion promotion : percentOffPromotions) {

        final RepositoryItem promoRepoItem = advertisePromotionRepository.getItem(promotion.getCode(), mPromotionsHelper.getAdvertisePromotionItemDescriptor());

        final boolean enablePreviewFlag = ((Boolean) promoRepoItem.getPropertyValue(ENABLE_PREVIEW_FLAG)).booleanValue();

        if (!mPromotionsHelper.validateDate(promotion)) {
          // If the preview flag OR the preview parameter is set to false, don't consider this promotion (continue to the next promo)
          if (!(enablePreviewFlag && enablePreview)) {
            continue;
          }
        }

        if (!promotion.equals(firstSlotPromotion) && !promotion.equals(extraPromotion) && !"113".equals(promotion.getType())) {

          if (promotion.isBogo()) {
            if (bestBogoPromo == null || new Integer(promotion.getPercentOff()) > new Integer(bestBogoPromo.getPercentOff())) {

              if (mPromotionsHelper.doesAdvertisePromotionHaveHtml(promoRepoItem, pageType)) {
                bestBogoPromo = promotion;
              }
            }

          } else {
            if (firstSlotPromotion != null && "113".equals(firstSlotPromotion.getType())) {
              if (firstSlotPromotion.getStackableFlag() != promotion.getStackableFlag()) {
                continue;
              }
            }

            if (bestOtherPromo == null || new Integer(promotion.getPercentOff()) > new Integer(bestOtherPromo.getPercentOff())) {
              if (mPromotionsHelper.doesAdvertisePromotionHaveHtml(promoRepoItem, pageType)) {
                bestOtherPromo = promotion;
              }
            }
          }

          if (mPromotionsHelper.doesAdvertisePromotionHaveHtml(promoRepoItem, pageType)) {
            String htmlPropertyValue = (String) promoRepoItem.getPropertyValue(htmlPropertyName);

            if (!PRODUCT.equals(pageType)) {
              promoTextList.add(htmlPropertyValue);
              displayOutlineAndHtmlText(enablePreview, promoRepoItem, true, pageType, htmlPropertyValue, true, true);

              if (firstSlotPromotion == null) {
                updateDogEarForBogoPromo(promoRepoItem);
              }

            } else {
              if (percentOffPromotionsSize > 1 && firstSlotPromotion != null && bestOtherPromo != null
                      && Integer.parseInt(bestOtherPromo.getPercentOff()) > Integer.parseInt(firstSlotPromotion.getPercentOff())) {
                promoTextList.add(htmlPropertyValue);
                displayOutlineAndHtmlText(enablePreview, promoRepoItem, true, pageType, htmlPropertyValue, true, true);
              }
            }
          }

        } else {

          if (percentOffPromotionsSize == 1 && !"113".equals(promotion.getType()) && mPromotionsHelper.doesAdvertisePromotionHaveHtml(promoRepoItem, pageType)) {
            alternateDisplayText = (String) promoRepoItem.getPropertyValue(htmlPropertyName);
            promoTextList.add(alternateDisplayText);
          }
        }
      }

      // if we have a 113 in the first slot
      if (firstSlotPromotion != null && "113".equals(firstSlotPromotion.getType())) {
        if (bestBogoPromo != null) {
          if (extraPromotion == null) {
            RepositoryItem promoRepoItem = null;

            if (bestBogoPromo.equals(getOnePromotion)) {
              promoRepoItem = advertisePromotionRepository.getItem(buyOnePromotion.getCode(), mPromotionsHelper.getAdvertisePromotionItemDescriptor());

            } else {
              promoRepoItem = advertisePromotionRepository.getItem(bestBogoPromo.getCode(), mPromotionsHelper.getAdvertisePromotionItemDescriptor());
            }

            displayText = (String) promoRepoItem.getPropertyValue(htmlPropertyName);
            promoTextList.add(displayText);
            displayOutlineAndHtmlText(enablePreview, promoRepoItem, true, pageType, displayText, true, true);

            if (bestOtherPromo != null && new Integer(bestOtherPromo.getPercentOff()).intValue() > new Integer(firstSlotPromotion.getPercentOff()).intValue()) {
              promoRepoItem = advertisePromotionRepository.getItem(bestOtherPromo.getCode(), mPromotionsHelper.getAdvertisePromotionItemDescriptor());

              displayText += "<BR>" + (String) promoRepoItem.getPropertyValue(htmlPropertyName);
            }
          }

        } else if (extraPromotion == null) {
          if (bestOtherPromo != null && new Integer(bestOtherPromo.getPercentOff()).intValue() > new Integer(firstSlotPromotion.getPercentOff()).intValue()) {
            final RepositoryItem promoRepoItem = advertisePromotionRepository.getItem(bestOtherPromo.getCode(), mPromotionsHelper.getAdvertisePromotionItemDescriptor());

            displayText = (String) promoRepoItem.getPropertyValue(htmlPropertyName);
          }
        }

      } else if (firstSlotPromotion == null) {
        // if the first slot is empty
        if (bestBogoPromo != null) {
          RepositoryItem promoRepoItem = null;

          if (bestBogoPromo.equals(getOnePromotion)) {
            promoRepoItem = advertisePromotionRepository.getItem(buyOnePromotion.getCode(), mPromotionsHelper.getAdvertisePromotionItemDescriptor());

          } else {
            promoRepoItem = advertisePromotionRepository.getItem(bestBogoPromo.getCode(), mPromotionsHelper.getAdvertisePromotionItemDescriptor());
          }

          displayText = (String) promoRepoItem.getPropertyValue(htmlPropertyName);
          promoTextList.add(displayText);
          displayOutlineAndHtmlText(enablePreview, promoRepoItem, true, pageType, displayText, true, true);
          updateDogEarForBogoPromo(promoRepoItem);

          if (bestOtherPromo != null && new Integer(bestOtherPromo.getPercentOff()).intValue() > new Integer(bestBogoPromo.getPercentOff()).intValue()) {

            promoRepoItem = advertisePromotionRepository.getItem(bestOtherPromo.getCode(), mPromotionsHelper.getAdvertisePromotionItemDescriptor());

            String htmlPropertyValue = (String) promoRepoItem.getPropertyValue(htmlPropertyName);
            displayText += "<BR>" + htmlPropertyValue;
            updateDogEarForBogoPromo(promoRepoItem);

            if (PRODUCT.equals(pageType)) {
              promoTextList.add(htmlPropertyValue);
              displayOutlineAndHtmlText(enablePreview, promoRepoItem, true, pageType, htmlPropertyValue, true, true);
            }
          }

        } else if (bestOtherPromo != null) {
          RepositoryItem promoRepoItem = advertisePromotionRepository.getItem(bestOtherPromo.getCode(), mPromotionsHelper.getAdvertisePromotionItemDescriptor());

          displayText = (String) promoRepoItem.getPropertyValue(htmlPropertyName);
          promoTextList.add(displayText);
          displayOutlineAndHtmlText(enablePreview, promoRepoItem, true, pageType, displayText, true, true);
          updateDogEarForBogoPromo(promoRepoItem);
        }
      }
    } catch (Exception e) {
      logError("Exception in addAdditionalPromoText method", e);
    }

    if (displayText != "") {
      setPromoPriceDisplay(true);

    } else if (alternateDisplayText != "") {
      displayText = alternateDisplayText;
      setPromoPriceDisplay(true);
    }

    outputParams.put(ADDITIONAL_PROMO_DISPLAY_PERCENT_OFF_TEXT, displayText);
  }

  /**
   * This method takes the product promokeys, first it verifies the start and end dates, then based on the template that user is on, checks for the corresponding flag enabled or not and takes the only
   * enabled keys for the further processing.
   *
   * @param promoKeys
   * @return filtered promokeys
   */
  private Set<String> getPromokeysByPageType(final Set<String> promoKeys) {

    final Set<String> pageTypePromoKeys = new HashSet<String>();
    try {

      for (final String promoKey : promoKeys) {
        RepositoryItem promoRepItem = advertisePromotionRepository.getItem(StringUtils.toUpperCase(promoKey), mPromotionsHelper.getAdvertisePromotionItemDescriptor());

        if (promoRepItem != null) {
          final Date startDate = (Date) (promoRepItem.getPropertyValue(MARK_START_DATE));
          final Date endDate = (Date) (promoRepItem.getPropertyValue(MARK_END_DATE));
          if (startDate != null && endDate != null) {
            final boolean validDate = verifyDateRange(startDate, endDate);
            final Date currDate = new Date();
            // The below condition is to verify the expired promotions so that we can allow future promotions.
            if (!validDate && (currDate.getTime() > endDate.getTime())) {
              continue;
            }
            // Keep track of the next upcoming promo start or end date
            updateNextPromoDate(startDate, endDate);
          }

          Map<String, Object> displayProperties = getDisplayProperties(getPageType(), false, promoRepItem);
          final boolean displayPropertyValue = (Boolean) displayProperties.get("displayPropertyValue");
          final boolean enabledPreview = NmoUtils.getBooleanPropertyValue(promoRepItem, ENABLE_PREVIEW_FLAG);

          if (displayPropertyValue || enabledPreview) {
            pageTypePromoKeys.add(promoKey);
          }
        }
      }

    } catch (final RepositoryException re) {
      logError("AdvertisePromotionManager.getPromokeysByPageType: RepositoryException:", re);
    }

    return pageTypePromoKeys;
  }

  /**
   * Method updates the dogear color based on the promotion's product page sort value
   *
   * @param promoRepoItem
   */
  private void updateDogEarForBogoPromo(final RepositoryItem promoRepoItem) {
    final boolean displayThumbnailOutline = NmoUtils.getBooleanPropertyValue(promoRepoItem, "thumbnailOutlineDisplayFlag");
    if (displayThumbnailOutline) {
      final int productPageSort = ((Integer) promoRepoItem.getPropertyValue("productPageSort")).intValue();
      updateThumbnailOutlineColor(productPageSort, (String) promoRepoItem.getPropertyValue("thumbnailOutlineColor"), displayThumbnailOutline);
    }

    updatePromoColor(promoRepoItem, pageType);
  }

  private String buildModalString(final String htmlText) {
    final StringBuffer outputString = new StringBuffer("");
    outputString.append("<div class=\"promo\" pageType=\"" + getPageType() + "\" >");
    outputString.append(htmlText);
    outputString.append("<div class=\"more-promo-modal\">");
    outputString.append("<div class=\"close\"></div>");
    outputString.append("</div>");
    outputString.append("</div>");

    return outputString.toString();
  }

  private String getThumbnailPromoColor() {
    return getPromoColor(mThumbnailPromoColor != null ? mThumbnailPromoColor : "");
  }

  private String getYmalPromoColor() {
    return getPromoColor(mYmalPromoColor != null ? mYmalPromoColor : "");
  }

  private String getLinePromoColor() {
    return getPromoColor(mLinePromoColor != null ? mLinePromoColor : "");
  }

  private String getStackingPromoColor() {
    return getPromoColor(mStackingPromoColor != null ? mStackingPromoColor : "");
  }

  private String getPromoColor(String promoColor) {
    if (StringUtilities.isNotBlank(promoColor)) {
      // prepend with hash if hex color
      if (promoColor.matches(HEX_COLOR_REGEX)) {
        promoColor = "#" + promoColor;
      }
    }
    return promoColor;
  }

  private void updateStackingPromoColor(final String hexCode, final boolean displayStackingPromoColor) {
    mStackingPromoColor = hexCode;
    stackingPromoColorFlag = Boolean.valueOf(displayStackingPromoColor);
  }

  private String getThumbnailOutlineColor() {
    return getPromoColor(mThumbnailOutlineColor != null ? mThumbnailOutlineColor : "");
  }

  private void updateThumbnailOutlineColor(final int productPageSort, final String hexCode, final boolean displayThumbnailOutline) {
    if (productPageSort < mFirstThumbnailPromoIndex) {
      mFirstThumbnailPromoIndex = productPageSort;
      mThumbnailOutlineColor = hexCode;
      thumbnailOutlineDisplayFlag = Boolean.valueOf(displayThumbnailOutline);
    }
  }

  /**
   * Return the html added to the tree map
   */
  private TreeMap<Integer, String> getHtmlMap() {
    return htmlMap;
  }

  /**
   * Clear html information from the tree map
   */
  private void resetHtmlMap() {
    htmlMap.clear();
  }

  /**
   * Keep list in order of "productPageSort". Only add new promo's to the list
   */
  private void addHtmlMap(final int i, final String htmlCode) {
    if (!htmlMap.containsKey(Integer.valueOf(i))) {
      htmlMap.put(Integer.valueOf(i), htmlCode);
    }
  }

  /**
   * Return the requesting page type
   */
  public String getPageType() {
    return pageType;
  }

  /**
   * Set the requesting page type
   */
  public void setPageType(final String pageType) {
    this.pageType = pageType;
  }

  public Boolean getPromoPriceDisplay() {
    return mPromoPriceDisplay;
  }

  public void setPromoPriceDisplay(final Boolean mPromoPriceDisplay) {
    this.mPromoPriceDisplay = mPromoPriceDisplay;
  }

  public Boolean isThumbnailOutlineDisplayFlag() {
    return thumbnailOutlineDisplayFlag;
  }

  public void setThumbnailOutlineDisplayFlag(final Boolean thumbnailOutlineDisplayFlag) {
    this.thumbnailOutlineDisplayFlag = thumbnailOutlineDisplayFlag;
  }

  public Boolean isThumbnailPromoColorFlag() {
    return thumbnailPromoColorFlag;
  }

  public void setThumbnailPromoColorFlag(final Boolean thumbnailPromoColorFlag) {
    this.thumbnailPromoColorFlag = thumbnailPromoColorFlag;
  }

  public Boolean isYmalPromoColorFlag() {
    return ymalPromoColorFlag;
  }

  public void setYmalPromoColorFlag(final Boolean ymalPromoColorFlag) {
    this.ymalPromoColorFlag = ymalPromoColorFlag;
  }

  public Boolean isProductPromoColorFlag() {
    return productPromoColorFlag;
  }

  public void setProductPromoColorFlag(final Boolean productPromoColorFlag) {
    this.productPromoColorFlag = productPromoColorFlag;
  }

  public Boolean isLinePromoColorFlag() {
    return linePromoColorFlag;
  }

  public void setLinePromoColorFlag(final Boolean linePromoColorFlag) {
    this.linePromoColorFlag = linePromoColorFlag;
  }

  public Boolean isDetailsPromoColorFlag() {
    return detailsPromoColorFlag;
  }

  public void setDetailsPromoColorFlag(final Boolean detailsPromoColorFlag) {
    this.detailsPromoColorFlag = detailsPromoColorFlag;
  }

  public Double getAdornmentPrice() {
    return adornmentPrice;
  }

  public void setAdornmentPrice(final Double price) {
    this.adornmentPrice = price;
  }

  public String getPromoPrice() {
    return promoPrice;
  }

  public void setPromoPrice(final String price) {
    this.promoPrice = price;
  }

  public String getPromoPriceText() {
    return promoPriceText;
  }

  public void setPromoPriceText(final String priceText) {
    this.promoPriceText = priceText;
  }

  // public Boolean isTemplatePricingAdornmentFlag() {
  // return templatePricingAdornmentFlag;
  // }
  //
  // public void setTemplatePricingAdornmentFlag(final Boolean templatePricingAdornmentFlag) {
  // this.templatePricingAdornmentFlag = templatePricingAdornmentFlag;
  // }

  // public String getTemplatePricingAdornmentText() {
  // final String pAText = templatePricingAdornmentText != null ? templatePricingAdornmentText : "";
  // return pAText;
  // }

  public Boolean isTemplatePricingAdornmentColorFlag() {
    return templatePricingAdornmentColorFlag;
  }

  public void setTemplatePricingAdornmentColorFlag(final Boolean templatePricingAdornmentColorFlag) {
    this.templatePricingAdornmentColorFlag = templatePricingAdornmentColorFlag;
  }

  private String getTemplatePricingAdornmentColor() {
    return getPromoColor(templatePricingAdornmentColor != null ? templatePricingAdornmentColor : "");
  }

  public Boolean isProductPagePricingAdornmentFlag() {
    return productPagePricingAdornmentFlag;
  }

  public void setProductPagePricingAdornmentFlag(final Boolean productPagePricingAdornmentFlag) {
    this.productPagePricingAdornmentFlag = productPagePricingAdornmentFlag;
  }

  public String getProductPagePricingAdornmentText() {
    final String pAText = productPagePricingAdornmentText != null ? productPagePricingAdornmentText : "";
    return pAText;
  }

  public Boolean isProductPagePricingAdornmentColorFlag() {
    return productPagePricingAdornmentColorFlag;
  }

  public void setProductPagePricingAdornmentColorFlag(final Boolean productPagePricingAdornmentColorFlag) {
    this.productPagePricingAdornmentColorFlag = productPagePricingAdornmentColorFlag;
  }

  public String getProductPagePricingAdornmentColor() {
    return getPromoColor(productPagePricingAdornmentColor != null ? productPagePricingAdornmentColor : "");
  }

  public Boolean isCheckoutPricingAdornmentFlag() {
    return checkoutPricingAdornmentFlag;
  }

  public void setCheckoutPricingAdornmentFlag(final Boolean checkoutPricingAdornmentFlag) {
    this.checkoutPricingAdornmentFlag = checkoutPricingAdornmentFlag;
  }

  public String getCheckoutPricingAdornmentText() {
    final String pAText = checkoutPricingAdornmentText != null ? checkoutPricingAdornmentText : "";
    return pAText;
  }

  public Boolean isCheckoutPricingAdornmentColorFlag() {
    return checkoutPricingAdornmentColorFlag;
  }

  public void setCheckoutPricingAdornmentColorFlag(final Boolean checkoutPricingAdornmentColorFlag) {
    this.checkoutPricingAdornmentColorFlag = checkoutPricingAdornmentColorFlag;
  }

  private boolean getBooleanValue(Object value, boolean defaultValue) {

    boolean result = defaultValue;

    if (value == null) {
      return result;
    }

    try {
      result = Boolean.parseBoolean(value.toString());
    } catch (Exception e) {}

    return result;
  }

  private void logError(String message) {
    logger.logError(message);
    System.out.println(message);
  }

  private void logError(String message, Exception e) {
    logger.logError(message, e);
    System.out.println(message);
    e.printStackTrace();
  }

  private void logDebug(String message) {
    if (logger.isLoggingDebug()) {
      logger.logDebug(message);
    }
  }

  /**
   * Check to see if promotion is set to display on product page.
   */
  @SuppressWarnings({"unchecked" , "rawtypes"})
  private void processProductPromoGroups(final String promoKeyId, final boolean enablePreview, final boolean isPromoDisplayable, final String productId,
          final PercentOffPromotion firstLinePercentOffPromo, final PercentOffPromotion extraPercentOffPromotion, Map<String, Object> inputParams, Map<String, Object> outputParams) {
    
    try {
      final RepositoryItem promoRepItem = advertisePromotionRepository.getItem(promoKeyId, mPromotionsHelper.getAdvertisePromotionItemDescriptor());
      
      boolean tPricingAdornmentFlag = Boolean.FALSE;
      String tPricingAdornmentText = "";
      boolean tPricingAdornmentColorFlag = Boolean.FALSE;
      String tPricingAdornmentColor = "";
      boolean tPromoColorFlag = false;
      String mTPromoColor = null;
      HashMap hmChildDetails = new HashMap();
      if (promoRepItem == null) {
        return;
      }
      
      HashMap hmChilds = (HashMap) outputParams.get(LINE_ITEM_PROMOTION_GROUPS);
      if (hmChilds == null) {
        return;
      }
      String displayPropertyName = null;
      String htmlPropertyName = null;
      boolean promoDisplayType = false;
      final NMProduct nmProduct = new NMProduct(productId);
      String retailPrice = nmProduct.getRetailPrice(!isDynamoRequest);
      
      // added for NMOBLDS-3270
      NMProductDisplayPrice productDisplayPrice = nmProduct.getProductDisplayPrice();
      
      displayPropertyName = "templateDisplayFlag";
      htmlPropertyName = "templateHtml";
      promoDisplayType = true;
      
      tPricingAdornmentFlag = (Boolean) promoRepItem.getPropertyValue(TEMPLATE_PRICING_ADORNMENT_FLAG);
      tPricingAdornmentText = (String) promoRepItem.getPropertyValue(TEMPLATE_PRICING_ADORNMENT_TEXT);
      tPricingAdornmentColorFlag = (Boolean) promoRepItem.getPropertyValue(TEMPLATE_PRICING_ADORNMENT_COLOR_FLAG);
      tPricingAdornmentColor = (String) promoRepItem.getPropertyValue(TEMPLATE_PRICING_ADORNMENT_COLOR);
      
      final Boolean propertyValue = (Boolean) promoRepItem.getPropertyValue(displayPropertyName);
      final boolean displayHtml = propertyValue != null ? propertyValue.booleanValue() : false;
      final String htmlPropertyValue = (String) promoRepItem.getPropertyValue(htmlPropertyName);
      String lineItemPropertyValue = (String) promoRepItem.getPropertyValue("itemHtml");
      
      // Get the underlying promo
      // Check to see if type is 113 (%Off without qualifiers)
      // Check to see that the enablePromoPriceDisplay is true from advertise-promo setup
      // Get the retail price from the product
      // Calculate the %Off and substract it from the retail price
      // If the Adv promo has text label use that, else use what's in html fragment for default.
      String advertisedPrice = "";
      hmChildDetails.put(USE_PROMO_PRICE_DISPLAY, false);
      
      final DecimalFormat DECIMAL_FORMAT_ADVERTISE_PRICE = new DecimalFormat("######.00");
      Boolean propertyVal = (Boolean) promoRepItem.getPropertyValue(ENABLE_PREVIEW_FLAG);
      
      if (propertyVal != null && ((!propertyVal && !enablePreview) || (propertyVal && enablePreview))) {
        if (firstLinePercentOffPromo != null) {
          
          if (!"113".equals(firstLinePercentOffPromo.getType())) {
            hmChildDetails.put(PROMO_PRICE_DISPLAY_PERCENT_OFF_TEXT, htmlPropertyValue);
            hmChildDetails.put(PROMO_PRICE_TEXT, htmlPropertyValue);
          }
          
          hmChildDetails.put(PROMO_PERCENT_OFF_VALUE, firstLinePercentOffPromo.getPercentOff());
          hmChildDetails.put(TEMPLATE_PRICING_ADORNMENT_FLAG, tPricingAdornmentFlag);
          hmChildDetails.put(TEMPLATE_PRICING_ADORNMENT_TEXT, tPricingAdornmentText);
          hmChildDetails.put(TEMPLATE_PRICING_ADORNMENT_COLOR_FLAG, tPricingAdornmentColorFlag);
          hmChildDetails.put(TEMPLATE_PRICING_ADORNMENT_COLOR, tPricingAdornmentColor);
          
          if (promoDisplayType && (Boolean) promoRepItem.getPropertyValue("enablePromoPriceDisplay") && brandSpecs.isEnablePromoPriceDisplay()) {
            
            String formattedAdvertisedPrice = "";
            
            // start NMOBLDS-3270
            String configuratorPromoMinMaxPrice = "";
            
            if (nmProduct.getFlgConfigurable()) {
              
              // passing configurator total price from configurator ajax call
              advertisedPrice = getConfiguratorPromoAppliedOnRetailPrice(firstLinePercentOffPromo, retailPrice, inputParams, outputParams);
              
              configuratorPromoMinMaxPrice = configuratorPromoAppliedOnMinMaxPrice(firstLinePercentOffPromo, productDisplayPrice, inputParams, outputParams);
              // end NMOBLDS-3270
              
            } else {
              advertisedPrice = getAdvertisedPromoPrice(firstLinePercentOffPromo.getPercentOff(), retailPrice);
            }
            
            if (!"".equals(advertisedPrice)) {
              if (advertisedPrice.contains("$")) {
                formattedAdvertisedPrice = DECIMAL_FORMAT_ADVERTISE_PRICE.format(new Double(advertisedPrice.substring(1)));
                
              } else {
                formattedAdvertisedPrice = DECIMAL_FORMAT_ADVERTISE_PRICE.format(new Double(advertisedPrice));
              }
            }
            
            setPromoPrice(formattedAdvertisedPrice);
            
            if (!"".equals(advertisedPrice)) {
              if (displayHtml && !ApiUtil.isEmpty(htmlPropertyValue)) {
                hmChildDetails.put(USE_PROMO_PRICE_DISPLAY, true);
                
                if (nmProduct.getFlgConfigurable()) {
                  hmChildDetails.put(PROMO_PRICE_DISPLAY_ADVERTISED_PRICE, configuratorPromoMinMaxPrice);
                  hmChildDetails.put(PROMO_PRICE, configuratorPromoMinMaxPrice);
                  hmChildDetails.put(PROMO_PRICE_DISPLAY_PERCENT_OFF_TEXT, "NOW");
                  
                } else {
                  hmChildDetails.put(PROMO_PRICE_DISPLAY_ADVERTISED_PRICE, formattedAdvertisedPrice);
                  hmChildDetails.put(PROMO_PRICE, formattedAdvertisedPrice);
                  
                  if (nmProduct.getFlgConfigurable()) {
                    hmChildDetails.put(PROMO_PRICE_DISPLAY_PERCENT_OFF_TEXT, lineItemPropertyValue);
                    hmChildDetails.put(PROMO_PRICE_TEXT, lineItemPropertyValue);
                    
                  } else {
                    hmChildDetails.put(PROMO_PRICE_DISPLAY_PERCENT_OFF_TEXT, htmlPropertyValue);
                    hmChildDetails.put(PROMO_PRICE_TEXT, htmlPropertyValue);
                  }
                  
                  hmChildDetails.put(CONFIGURATOR_LEFT_NAV_TOP_PROMO_TEXT, htmlPropertyValue);
                }
                
              } else if (nmProduct.getFlgConfigurable() && ApiUtil.isEmpty(htmlPropertyValue)) {
                hmChildDetails.put(PROMO_PRICE_DISPLAY_ADVERTISED_PRICE, formattedAdvertisedPrice);
                hmChildDetails.put(PROMO_PRICE, formattedAdvertisedPrice);
                hmChildDetails.put(PROMO_PRICE_DISPLAY_PERCENT_OFF_TEXT, "NOW");
                hmChildDetails.put(CONFIGURATOR_LEFT_NAV_TOP_PROMO_TEXT, INMGenericConstants.EMPTY_STRING);
              }
            }
            
            tPromoColorFlag = (Boolean) promoRepItem.getPropertyValue("thumbnailPromoColorFlag");
            if (tPromoColorFlag) {
              mTPromoColor = (String) promoRepItem.getPropertyValue("thumbnailPromoColor");
              // prepend with hash if hex color
              if (mTPromoColor.matches(HEX_COLOR_REGEX)) {
                mTPromoColor = "#" + mTPromoColor;
              }

            }
            
          }
          if (tPromoColorFlag) {
            hmChildDetails.put(THUMBNAIL_PROMO_COLOR_FLAG, tPromoColorFlag);
            if (!StringUtilities.isNullOrEmpty(mTPromoColor)) {
              hmChildDetails.put(THUMBNAIL_PROMO_COLOR, mTPromoColor);
            }
          }
        }
      }
      
      if (hmChildDetails != null && !hmChildDetails.isEmpty()) {
        hmChilds.put(productId, hmChildDetails);
      }
      hmChildDetails = null;
      
    } catch (final RepositoryException re) {
      logError("AdvertisePromotionManager.processProductPromoGroup: RepositoryException:", re);
      re.printStackTrace();
      
    } catch (final NullPointerException npe) {
      logError("AdvertisePromotionManager.processProductPromoGroup: NullPointerException:", npe);
      npe.printStackTrace();
    }
  }

  /*
   * This method invalidates all entries of the advertise promo cache (a custom coherence cache) for a given product.
   */
  public static void invalidateCacheForProduct(final String productId) {
    NamedCache advertisePromotionCache = NMCacheFactory.getAdvertisePromotionCache();
    ValueExtractor extractor = new KeyExtractor("getProductId");
    Filter filter = new EqualsFilter(extractor, productId);
    ConditionalRemove conditionalRemove = new ConditionalRemove(AlwaysFilter.INSTANCE, false);
    advertisePromotionCache.invokeAll(filter, conditionalRemove);
  }

  /*
   * This method invalidates all of the advertise promo cache (a custom coherence cache) for all products related to the given promotion.
   */
  public static void invalidateCacheForPromo(final String promoKey) {
    String lookupKey = "%" + promoKey + "%";
    NamedCache advertisePromotionCache = NMCacheFactory.getAdvertisePromotionCache();
    KeyExtractor extractor = new KeyExtractor("getPromoKeys");
    Filter filter = new LikeFilter(extractor, lookupKey, (char) 0, false);
    ConditionalRemove conditionalRemove = new ConditionalRemove(AlwaysFilter.INSTANCE, false);
    advertisePromotionCache.invokeAll(filter, conditionalRemove);
  }

  /*
   * This method invalidates all of the advertise promo cache (a custom coherence cache) for all products related to 2 given promotions.
   * 
   * This method is used when reordering advertise promotions from the CSR tool. It invalidates for any product that exists on both promotions being swapped.
   */
  public static void invalidateCacheForPromoMove(final String promoKey, final String otherPromoKey) {
    NamedCache advertisePromotionCache = NMCacheFactory.getAdvertisePromotionCache();
    KeyExtractor extractor = new KeyExtractor("getPromoKeys");
    String lookupKey = "%" + promoKey + "%";
    Filter filter1 = new LikeFilter(extractor, lookupKey, (char) 0, false);
    lookupKey = "%" + otherPromoKey + "%";
    Filter filter2 = new LikeFilter(extractor, lookupKey, (char) 0, false);
    Filter filter = new AndFilter(filter1, filter2);
    ConditionalRemove conditionalRemove = new ConditionalRemove(AlwaysFilter.INSTANCE, false);
    advertisePromotionCache.invokeAll(filter, conditionalRemove);
  }
  
}
