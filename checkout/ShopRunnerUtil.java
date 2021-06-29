package com.nm.commerce.checkout;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import atg.commerce.order.PaymentGroup;
import atg.commerce.order.ShippingGroupCommerceItemRelationship;
import atg.nucleus.GenericService;
import atg.nucleus.Nucleus;
import atg.repository.Repository;
import atg.repository.RepositoryException;
import atg.repository.RepositoryItem;
import atg.repository.RepositoryView;
import atg.repository.rql.RqlStatement;
import atg.servlet.DynamoHttpServletRequest;
import atg.servlet.DynamoHttpServletResponse;
import atg.servlet.ServletUtil;

import com.nm.ajax.checkout.utils.ComponentUtils;
import com.nm.collections.GiftWithPurchase;
import com.nm.collections.NMPromotion;
import com.nm.collections.NMPromotionArray;
import com.nm.collections.ServiceLevel;
import com.nm.collections.ServiceLevelArray;
import com.nm.commerce.GiftCard;
import com.nm.commerce.KeyValueBean;
import com.nm.commerce.NMCommerceItem;
import com.nm.commerce.NMCreditCard;
import com.nm.commerce.NMJSONObject;
import com.nm.commerce.NMProfile;
import com.nm.commerce.catalog.NMProduct;
import com.nm.commerce.catalog.NMSku;
import com.nm.commerce.checkout.beans.ContactInfo;
import com.nm.commerce.order.NMOrderManager;
import com.nm.commerce.pagedef.model.CheckoutPageModel;
import com.nm.commerce.pagedef.model.UIElementConstants;
import com.nm.commerce.pricing.Markdown;
import com.nm.commerce.promotion.NMOrderImpl;
import com.nm.common.INMGenericConstants;
import com.nm.common.SmartPostServiceLevelSessionBean;
import com.nm.components.CommonComponentHelper;
import com.nm.formhandler.ShoppingCartHandler;
import com.nm.integration.ShopRunner;
import com.nm.integration.ShopRunnerConstants;
import com.nm.profile.ProfileProperties;
import com.nm.utils.BrandSpecs;
import com.nm.utils.InventoryUtil;
import com.nm.utils.ProdSkuUtil;
import com.nm.utils.SpecialCodeTypes;
import com.nm.utils.StringUtilities;

/* package */class ShopRunnerUtil {
  
  private static ShopRunnerUtil INSTANCE;
  private final ShopRunner shopRunner;
  private LinkedHashMap<String, String> mParameterMap;
  private KeyValueBean[] adormentKeyValues = null;
  private List<String> adormentsCategories = null;
  
  private static final SpecialCodeTypes SPECIALCODETYPES = (SpecialCodeTypes) Nucleus.getGlobalNucleus().resolveName("/nm/utils/SpecialCodeTypes");
  
  @SuppressWarnings("unchecked")
  private static final ArrayList<String> ORDEREDTYPES = SPECIALCODETYPES.getOrderedList();
  
  private static final Repository SICODEREPOSITORY = (Repository) Nucleus.getGlobalNucleus().resolveName("/nm/xml/SpecialCodeRepository");
  
  public static synchronized ShopRunnerUtil getInstance() {
    INSTANCE = INSTANCE == null ? new ShopRunnerUtil() : INSTANCE;
    return INSTANCE;
  }
  
  public ShopRunner getShopRunner() {
    return shopRunner;
  }
  
  // private constructor enforces singleton behavior
  private ShopRunnerUtil() {
    shopRunner = CheckoutComponents.getShopRunner();
  }
  
  public void setToken(final DynamoHttpServletRequest request, final DynamoHttpServletResponse response, final String token) {
    shopRunner.setToken(request, response, token);
  }
  
  public String getToken(final DynamoHttpServletRequest request) {
    return shopRunner.getToken(request);
  }
  
  public void deleteToken(final DynamoHttpServletRequest request, final DynamoHttpServletResponse response) {
    shopRunner.deleteToken(request, response);
  }
  
  /*
   * public void deleteSessionId(DynamoHttpServletRequest request, DynamoHttpServletResponse response) { shopRunner.deleteSessionId(request, response); }
   * 
   * public void setSessionId(DynamoHttpServletRequest request, DynamoHttpServletResponse response) { shopRunner.setSessionId(request, response); }
   */
  
  public boolean isSessionValid(final DynamoHttpServletRequest request) {
    return shopRunner.isSessionValid(request);
  }
  
  public String getVendorId(final boolean isExpressCheckOut) {
    String vendorId = shopRunner.getVendorId();
    if (isExpressCheckOut) {
      vendorId = shopRunner.getExpressCOVendorId();
    }
    return vendorId;
  }
  
  /**
   * This Method returns alipay vendor Id for Alipay express checkout
   * 
   * @param isExpressCheckOut
   * @return String
   */
  public String getAlipayVendorId(final boolean isExpressCheckOut) {
    String alipayVendorId = null;
    if (isExpressCheckOut) {
      alipayVendorId = shopRunner.getAlipayVendorId();
    }
    return alipayVendorId;
  }
  
  public boolean isEnabled() {
    return shopRunner.isEnabled();
  }
  
  public ContactInfo processShopRunnerProfileData(final NMProfile profile, final JSONObject addressObj, final DynamoHttpServletRequest request) throws Exception {
    
    final ContactInfo addr = new ContactInfo();
    addr.setContactAddressName(ProfileProperties.Profile_shopRunnerAddressName);
    
    if (request.getParameter("firstName") != null) {
      profile.setShopRunnerFirstName(request.getParameter("firstName"));
      addr.setFirstName(request.getParameter("firstName"));
    }
    
    if (request.getParameter("lastName") != null) {
      profile.setShopRunnerLastName(request.getParameter("lastName"));
      addr.setLastName(request.getParameter("lastName"));
    }
    
    if (request.getParameter("phone") != null) {
      profile.setShopRunnerPhoneNumber(request.getParameter("phone"));
      addr.setDayTelephone(request.getParameter("phone"));
    }
    
    if (request.getParameter("email") != null) {
      profile.setShopRunnerEmail(request.getParameter("email"));
    }
    
    if (addressObj.get("address1") != null) {
      profile.setShopRunnerAddress1(addressObj.get("address1").toString());
      addr.setAddressLine1(addressObj.get("address1").toString());
    }
    
    if (addressObj.get("address2") != null) {
      profile.setShopRunnerAddress2(addressObj.get("address2").toString());
      addr.setAddressLine2(addressObj.get("address2").toString());
    }
    
    if (addressObj.get("city") != null) {
      profile.setShopRunnerCity(addressObj.get("city").toString());
      addr.setCity(addressObj.get("city").toString());
    }
    
    if (addressObj.get("state") != null) {
      profile.setShopRunnerState(addressObj.get("state").toString());
      addr.setState(addressObj.get("state").toString());
    }
    
    if (addressObj.get("zip") != null) {
      profile.setShopRunnerZip(addressObj.get("zip").toString());
      addr.setZip(addressObj.get("zip").toString().substring(0, 5));
    }
    
    if (addressObj.get("country") != null) {
      profile.setShopRunnerCountry(addressObj.get("country").toString());
      addr.setCountry(addressObj.get("country").toString());
    }
    
    return addr;
  }
  
  /**
   * Build ShopRunner Cart
   * 
   * @param pageModel
   * @param order
   * @param orderMgr
   * @return JSONObject
   * @throws Exception
   */
  @SuppressWarnings("unchecked")
  public JSONObject buildCart(final CheckoutPageModel pageModel, final NMOrderImpl order, final NMOrderManager orderMgr, final String dropShipPh) throws Exception {
    final JSONObject cart = new JSONObject();
    final ServiceLevelUtil serviceLevelUtil = ServiceLevelUtil.getInstance();
    boolean alipayEligible = false;
    if (null != pageModel.getElementDisplaySwitchMap().get(UIElementConstants.ALIPAY)) {
      alipayEligible = pageModel.getElementDisplaySwitchMap().get(UIElementConstants.ALIPAY);
    }
    
    cart.put("cartId", pageModel.getBrandOrderId());
    double orderShippingCharges = 0.0;
    orderShippingCharges = order.getPriceInfo().getShipping();
    // for updatePRCart - get new order balance to accommodate gift card
    // calculation
    double newOrderBalance = orderMgr.getCurrentOrderAmountRemaining(order, false);
    // SmartPost : Deduct Service level amount from Order for Alipay checkout
    final String abTestGroup = CheckoutComponents.getServiceLevelArray().getServiceLevelABTestGroup();
    if (alipayEligible && CommonComponentHelper.getSystemSpecs().isSmartPostEnabled() && abTestGroup.equalsIgnoreCase(UIElementConstants.AB_TEST_SERVICE_LEVEL_GROUPS)) {
      if (orderShippingCharges > 0) {
        final SmartPostServiceLevelSessionBean smartPostServiceLevelSessionBean = CheckoutComponents.getSmartPostServiceLevelSessionBean(ServletUtil.getCurrentRequest());
        final String selectedServiceLevelCode = smartPostServiceLevelSessionBean.getSmartPostSelectedServiceLevel();
        if (StringUtilities.isNotEmpty(selectedServiceLevelCode)) {
          final ServiceLevel serviceLevel = CheckoutAPI.getServiceLevelByCodeAndCountry(selectedServiceLevelCode, ShopRunnerConstants.US_COUNTRY_CODE);
          final double amt = serviceLevel.getAmount();
          orderShippingCharges = orderShippingCharges - amt;
          newOrderBalance = newOrderBalance - amt;
        }
      }
    }
    cart.put("orderShipping", orderShippingCharges);
    cart.put("orderSubTotal", order.getTrueRawSubtotal());
    cart.put("orderTax", order.getPriceInfo().getTax());
    if (alipayEligible) {
      final List<NMCommerceItem> commerceItems = order.getNmCommerceItems();
      double giftWraptotal = 0.00;
      for (final NMCommerceItem item : commerceItems) {
        final double wrappingCharges = item.getGiftWrapPrice();
        giftWraptotal += wrappingCharges;
      }
      newOrderBalance = newOrderBalance - giftWraptotal;
    }
    cart.put("orderTotal", newOrderBalance);
    
    final JSONArray products = new JSONArray();
    cart.put("products", products);
    final List<ShippingGroupCommerceItemRelationship> items = pageModel.getCommerceItemRelationships();
    final JSONArray shippingGroups = new JSONArray();
    for (int i = 0; i < items.size(); i++) {
      final ShippingGroupCommerceItemRelationship commerceItem = items.get(i);
      final NMCommerceItem item = (NMCommerceItem) commerceItem.getCommerceItem();
      final NMProduct p = item.getProduct();
      
      JSONObject product = buildProduct(item, p);
      // if order has a drop ship item, user should be prompted for phone
      // number
      if (!alipayEligible && item.getProduct().getFlgDeliveryPhone()) {
        product.put("dropship_ph_flag", true);
        if (!cart.containsKey("dropship_ph_flag")) {
          cart.put("dropship_ph_flag", true);
          cart.put("dropship_ph", dropShipPh);
        }
      }
      if (item.getGiftWrapFlag()) {
        final NMJSONObject giftWrapObject = new NMJSONObject();
        if (item.getGiftNote() != 0) {
          giftWrapObject.put("message", alipayEligible ? "" : item.getGiftNote());
        }
        giftWrapObject.put("price", alipayEligible ? "" : item.getGiftWrapPrice());
        product.put("giftWrap", giftWrapObject);
      }
      // Item level adornments
      final JSONArray adornmentsArray = buildSRAdornmentArray(item);
      if (!item.getGiftWrapFlag() && (item.getGiftNote() != 0)) {
        if (alipayEligible) {
          adornmentsArray.add(buildSRAdornment("gift note: ", null, null));
        } else {
          adornmentsArray.add(buildSRAdornment("gift note: ", "Yes", null));
        }
      }
      product.put("adornments", adornmentsArray);
      product.put("shippingGroup", i);
      // Appending additional Product Object attributes for Alipay Checkout
      if (alipayEligible) {
        product = buildAlipayProduct(product, item, p, pageModel);
      }
      products.add(product);
      
      final JSONObject shippingGroup = new JSONObject();
      shippingGroup.put("shippingGroup", i);
      
      if (!p.getIsShopRunnerEligible()) {
        final JSONArray shippingOptions = new JSONArray();
        shippingGroup.put("shipping", shippingOptions);
        
        final Set<ServiceLevel> serviceLevels = new TreeSet<ServiceLevel>();
        final String shipToCountry = "US";
        final List<Set<ServiceLevel>> itemServiceLevels = new ArrayList<Set<ServiceLevel>>();
        final Set<ServiceLevel> itemSLs = serviceLevelUtil.getCommerceItemValidServiceLevels(item, shipToCountry);
        if (itemSLs.size() > 0) {
          itemServiceLevels.add(new HashSet<ServiceLevel>(itemSLs));
          for (final ServiceLevel sl : serviceLevelUtil.mergeServiceLevels(itemServiceLevels)) {
            if ("Standard - 5-7 Business Days".equals(sl.getShortDesc())) {
              sl.setShortDesc("Standard");
            }
            serviceLevels.add(sl);
          }
        } else {
          final ServiceLevel serviceLevel = new ServiceLevel();
          // SmartPost : changing the service level code based on amount and ship to country
          String freeShippingShortDesc = INMGenericConstants.EMPTY_STRING;
          final ServiceLevelArray serviceLevelArray = CheckoutComponents.getServiceLevelArray();
          final String freeShippingServiceLevelCode = serviceLevelArray.determineFreeShippingServiceLevel();
          serviceLevel.setCode(freeShippingServiceLevelCode);
          // SmartPost : getting the service level short description from the updated service level object for the selected service level code and the AB test group
          final ServiceLevel freeShippingServiceLevel = serviceLevelArray.getServiceLevelByCode(freeShippingServiceLevelCode, serviceLevelArray.getServiceLevelGroupBasedOnAbTest());
          if (null != freeShippingServiceLevel) {
            freeShippingShortDesc = freeShippingServiceLevel.getShortDesc();
          }
          // Perishable item service level
          final String shortDesc = (item.getPerishable()) ? "Vendor Ships" : freeShippingShortDesc;
          serviceLevel.setShortDesc(shortDesc);
          serviceLevels.add(serviceLevel);
        }
        final ServiceLevel[] validServiceLevels = serviceLevels.toArray(new ServiceLevel[serviceLevels.size()]);
        
        if (validServiceLevels != null) {
          for (int n = 0; n < validServiceLevels.length; n++) {
            final ServiceLevel sl = validServiceLevels[n];
            if (sl != null) {
              final JSONObject shipping = new JSONObject();
              shipping.put("shippingDisplay", sl.getShortDesc().toUpperCase());
              shipping.put("method", sl.getCode());
              shipping.put("shipPrice", sl.getAmount());
              final boolean selectedFlag = n == 0 ? true : false;
              shipping.put("selected", selectedFlag);
              shippingOptions.add(shipping);
            }
          }
        }
        shippingGroups.add(shippingGroup);
      }
    }
    cart.put("shippingGroups", shippingGroups);
    /** adding atribute to cart object for Alipay */
    if (alipayEligible) {
      cart.put("aliPayOrder", alipayEligible);
    }
    return cart;
  }
  
  /**
   * Build Product
   * 
   * @param item
   * @param p
   * @return JSONObject
   */
  @SuppressWarnings("unchecked")
  private JSONObject buildProduct(final NMCommerceItem item, final NMProduct p) {
    final JSONObject product = new JSONObject();
    product.put("editQty", true);
    product.put("sku", item.getCatalogRefId());
    product.put("brandName", StringUtilities.emptyIfNull(p.getWebDesignerName()));
    product.put("skuName", StringUtilities.emptyIfNull(p.getDisplayName()));
    product.put("skuDescription", StringUtilities.emptyIfNull(p.getDescription()));
    product.put("isSREligible", p.getIsShopRunnerEligible());
    product.put("skuQty", item.getQuantity());
    product.put("message", "Test msg...to be decided");
    product.put("unitPrice", item.getCurrentItemPrice());
    String smallImageUrl = p.getImageOrShimUrl().get("mh");
    String largeImageUrl = p.getImageOrShimUrl().get("mx");
    final String suiteId = item.getSuiteId();
    if ((suiteId != null) && !suiteId.trim().equals("")) {
      final NMProduct suite = new NMProduct(suiteId);
      if (!p.getImagePresent().get("mh")) {
        smallImageUrl = suite.getImageUrl("mh");
      }
      if (!p.getImagePresent().get("mx")) {
        largeImageUrl = suite.getImageUrl("mx");
      }
    }
    product.put("smallImageUrl", smallImageUrl);
    product.put("largeImageUrl", largeImageUrl);
    return product;
  }
  
  /**
   * This method appends Alipay specific attributes to the existing ShopRunner Product Object
   * 
   * @param srProduct
   * @param item
   * @param p
   * @param pageModel
   * @return JSONObject
   */
  @SuppressWarnings("unchecked")
  public JSONObject buildAlipayProduct(final JSONObject srProduct, final NMCommerceItem item, final NMProduct p, final CheckoutPageModel pageModel) {
    final NMSku nmSku = new NMSku(item.getCatalogRefId());
    final String cmVariation2 = p.getProdSkuUtil().getProdSkuVariation(p.getDataSource(), nmSku.getDataSource(), ShopRunnerConstants.SKU_SIZE);
    boolean isAlipayEligible = false;
    final ProdSkuUtil prodSkuUtil = CommonComponentHelper.getProdSkuUtil();
    if (p.getIsShopRunnerEligible() && prodSkuUtil.isAlipayEligible(item) && !item.isGwpItem()) {
      isAlipayEligible = true;
    }
    srProduct.put("isAliPayEligible", isAlipayEligible);
    srProduct.put("type", StringUtilities.emptyIfNull(pageModel.getActiveCategory().getId()));
    srProduct.put("serialNumber", StringUtilities.emptyIfNull(nmSku.getCmosSku()));
    srProduct.put("model", StringUtilities.emptyIfNull(p.getCmosItemCode()));
    srProduct.put("size", StringUtilities.emptyIfNull(cmVariation2));
    srProduct.put("shippingWeight", nmSku.getBoxWeight());
    srProduct.put("shipmentDimensionsLength", nmSku.getItemLength());
    srProduct.put("shipmentDimensionsWidth", nmSku.getItemWidth());
    srProduct.put("shipmentDimensionsHeight", nmSku.getItemHeight());
    srProduct.put("countryOfOrigin", p.getCountryOfOrigin());
    srProduct.put("chinaImportEligibility", !p.isRestrictedToCountry(ShopRunnerConstants.CHINA_COUNTRY_CODE));
    if (getLogger().isLoggingDebug()) {
      getLogger().logDebug("The request being passed to SR/AP -> Sku::isAliPayEligible || " + StringUtilities.emptyIfNull(p.getDisplayName()) + "::" + srProduct.get("isAliPayEligible"));
    }
    return srProduct;
  }
  
  /**
   * Build Adornment(Product Details)
   * 
   * @param item
   * @return JSONArray
   */
  @SuppressWarnings("unchecked")
  public JSONArray buildSRAdornmentArray(final NMCommerceItem item) throws RepositoryException {
    JSONArray adornmentsArray = new JSONArray();
    final NMProduct p = item.getProduct();
    
    adornmentsArray.add(buildSRAdornment("Item:", item.getCmosCatalogId() + "_" + item.getCmosItemCode(), null));
    if ((item.getProdVariation1() != null) && !item.getProdVariation1().equals("")) {
      adornmentsArray.add(buildSRAdornment("Color:", item.getProdVariation1(), null));
    }
    if ((item.getProdVariation2() != null) && !item.getProdVariation2().equals("")) {
      adornmentsArray.add(buildSRAdornment("Size:", item.getProdVariation2(), null));
    }
    adornmentsArray.add(buildSRAdornment("Qty:", item.getQuantity(), null));
    
    // Item level promotions
    final Map<String, Markdown> markdowns = item.getItemMarkdowns();
    
    // Pricing Adornments
    if (p.getFlgPricingAdornments()) {
      final Map<String, RepositoryItem> pricingAdornments = p.getPricingAdornments();
      RepositoryItem currAdornment = null;
      for (int index = pricingAdornments.size(); index > 0; index--) {
        currAdornment = pricingAdornments.get("position" + index);
        final String pricingAdornmentStyle = null;
        if ((currAdornment != null) && ((index == 1) || ((Double) currAdornment.getPropertyValue("adornment_price") != Double.parseDouble(p.getRetailPrice())))) {
          if ((currAdornment.getPropertyValue("label") != null) && (currAdornment.getPropertyValue("adornment_price") != null)) {
            // if(index != 1 || markdowns.size() > 0) pricingAdornmentStyle = "strikethrough";
            adornmentsArray.add(buildSRAdornment(currAdornment.getPropertyValue("label") + ":",
                    NumberFormat.getCurrencyInstance().format((Double) currAdornment.getPropertyValue("adornment_price") * item.getQuantity()), pricingAdornmentStyle));
          }
        }
      }
    } else {
      if ((p.getRetailPrice() != null) && !p.getRetailPrice().equals("")) {
        adornmentsArray.add(buildSRAdornment("Price:", NumberFormat.getCurrencyInstance().format(Double.parseDouble(p.getRetailPrice()) * item.getQuantity()), null));
      }
    }
    
    // Item level Promotions
    final Iterator<String> iter = markdowns.keySet().iterator();
    while (iter.hasNext()) {
      final String key = iter.next();
      final Markdown markdown = markdowns.get(key);
      String promoMessage = "$ off ";
      if ((markdown.getPercentDiscount() != null) && !markdown.getPercentDiscount().equals("")) {
        promoMessage = markdown.getPercentDiscount() + "% off ";
      }
      adornmentsArray.add(buildSRAdornment(promoMessage, "-" + NumberFormat.getCurrencyInstance().format(markdown.getDollarDiscount()), "itemLevelPromotion"));
    }
    if (markdowns.size() > 0) {
      adornmentsArray.add(buildSRAdornment("Your Price:", NumberFormat.getCurrencyInstance().format(item.getCurrentItemPrice()), "finalPrice"));
    }
    // Monogram Items
    if (item.getIsMonogramItem()) {
      adornmentsArray = buildMonogramValues(item, adornmentsArray);
    }
    // Delivery & Processing Charge
    if ((p.getParentheticalCharge() != null) && (p.getParentheticalCharge() > 0)) {
      adornmentsArray.add(buildSRAdornment("Shipping:", NumberFormat.getCurrencyInstance().format(p.getParentheticalCharge()), null));
    }
    // Delivery Date
    if ((item.getReqDeliveryDate() != null) && !item.getReqDeliveryDate().equals("")) {
      adornmentsArray.add(buildSRAdornment("Delivery Date:", new SimpleDateFormat("MM/dd/yyyy").format(item.getReqDeliveryDate()), null));
    }
    // Expected Ship Date
    if ((item.getRealExpectedShipDate() != null) && !item.getRealExpectedShipDate().equals("")) {
      adornmentsArray.add(buildSRAdornment("Expected to Ship:", item.getRealExpectedShipDate(), null));
    }
    // Transient Status: 'Pre-Order', 'Backorder', 'In Stock', etc.
    final String transientStatus = item.getTransientStatus();
    String onlyXLeftMessage = getOnlyXLeftInventoryMessage(item);
    boolean displayInStock = false;
    final BrandSpecs brandSpecs = (BrandSpecs) Nucleus.getGlobalNucleus().resolveName("/nm/utils/BrandSpecs");
    if (transientStatus != null) {
      if (transientStatus.contains(p.getProdSkuUtil().getStatusPromotionalString())) {
        adornmentsArray.add(buildSRAdornment(null, "Promotion", "noKeyValue"));
      } else if (transientStatus.contains(p.getProdSkuUtil().getStatusPreOrderString())) {
        adornmentsArray.add(buildSRAdornment(null, "Pre-Order", "noKeyValue"));
      } else if (transientStatus.contains(p.getProdSkuUtil().getStatusBackorderString())) {
        adornmentsArray.add(buildSRAdornment(null, "Backorder", "noKeyValue"));
      } else if (transientStatus.contains(p.getProdSkuUtil().getStatusShipFromStoreString())) {
        adornmentsArray.add(buildSRAdornment(null, "This item ships from store", "noKeyValue"));
        final String shipFromStoreDelayMessage = brandSpecs.getShipFromStoreDelayMessage();
        adornmentsArray.add(buildSRAdornment(null, shipFromStoreDelayMessage, "noKeyValue_slightDelays"));
        displayInStock = !onlyXLeftMessage.isEmpty() ? true : false;
      } else if (transientStatus.contains(p.getProdSkuUtil().getStatusDropshipString())) {
        adornmentsArray.add(buildSRAdornment(null, "This item ships from vendor", "sfv"));
      } else if (transientStatus.contains(p.getProdSkuUtil().getStatusInStockString())) {
        displayInStock = true;
      }
      if (displayInStock) {
        final String inStockString = item.getProduct().getProdSkuUtil().getStatusInStockString();
        onlyXLeftMessage = !onlyXLeftMessage.isEmpty() ? inStockString + ", " + onlyXLeftMessage : inStockString;
        adornmentsArray.add(buildSRAdornment(null, onlyXLeftMessage, "noKeyValue"));
      }
    }
    
    return adornmentsArray;
  }
  
  /**
   * Build ShopRunner adornment object
   * 
   * @param key
   * @param value
   * @return
   */
  @SuppressWarnings({"unchecked"})
  private JSONObject buildSRAdornment(final Object key, final Object value, final String style) {
    final JSONObject adornment = new JSONObject();
    adornment.put("key", (key != null ? key : ""));
    adornment.put("value", (value != null ? value : ""));
    adornment.put("style", style);
    return adornment;
  }
  
  private JSONArray buildMonogramValues(final NMCommerceItem item, final JSONArray adornmentsArray) throws RepositoryException {
    adornmentsArray.add(buildSRAdornment(null, "Monogram", "noKeyValue"));
    String specialType = null;
    String siCode = null;
    String siCodeType = null;
    String siDataEntry = null;
    final String OUTPUT_DATA = "output data";
    final String MONO_STYLE = "mono style";
    final String TYPE_STYLE = "type style";
    final Map<String, String> siCodeMap = item.getSpecialInstCodes();
    final LinkedHashMap<String, String> sortedCodesValues = getSortedCodesValues(siCodeMap);
    mParameterMap = new LinkedHashMap<String, String>();
    
    if (adormentsCategories == null) {
      buildAdormentCategories();
    }
    
    adormentKeyValues = new KeyValueBean[adormentsCategories.size()];
    
    if (!sortedCodesValues.isEmpty()) {
      // load parameters
      final Iterator<String> siCodeMapIterator = sortedCodesValues.keySet().iterator();
      
      while (siCodeMapIterator.hasNext()) {
        siCode = siCodeMapIterator.next();
        siCodeType = getType(siCode);
        siDataEntry = siCodeMap.get(siCode);
        processAdormentValue(siCode, siCodeType, siDataEntry);
      }
      
      // unload parameters
      final Iterator<String> iter = mParameterMap.keySet().iterator();
      while (iter.hasNext()) {
        final String key = iter.next();
        final String value = mParameterMap.get(key);
        
        if (adormentsCategories.contains(key)) {
          // System.out.println(key + " | " + value);
          // System.out.println("Key index value :: " + adormentsCategories.indexOf(key));
          
          if (key.equals(OUTPUT_DATA)) {
            if (mParameterMap.containsKey("specialType")) {
              specialType = mParameterMap.get("specialType");
              
              if (specialType.equals("SINGLE_INIT") || specialType.equals("THREE_INIT_FML")) {
                adormentKeyValues[adormentsCategories.indexOf(key)] = new KeyValueBean("Your Name:", value);
              } else {
                adormentKeyValues[adormentsCategories.indexOf(key)] = new KeyValueBean("Your Initials:", value);
              }
            } else {
              adormentKeyValues[adormentsCategories.indexOf(key)] = new KeyValueBean("Your Name / Initials:", value);
            }
          } else if (key.equals(MONO_STYLE)) {
            adormentKeyValues[adormentsCategories.indexOf(key)] = new KeyValueBean("Style: ", mParameterMap.get("webValue"));
          } else if (key.equals(TYPE_STYLE)) {
            adormentKeyValues[adormentsCategories.indexOf(key)] = new KeyValueBean("Type Style: ", mParameterMap.get("webValue"));
          } else {
            adormentKeyValues[adormentsCategories.indexOf(key)] = new KeyValueBean(StringUtilities.titleCase(key), value);
          }
        }
      }
    }
    
    for (int i = 0; i < adormentKeyValues.length; i++) {
      final KeyValueBean bean = adormentKeyValues[i];
      if (bean != null) {
        adornmentsArray.add(buildSRAdornment(bean.getKey(), bean.getValue(), null));
      }
    }
    return adornmentsArray;
  }
  
  /*
   * Takes a map of codes/values in random order, and returns them in the order predicated by getOrderedTypes().
   */
  private LinkedHashMap<String, String> getSortedCodesValues(final Map<String, String> unorderedCodesValues) {
    final LinkedHashMap<String, String> reorderedMap = new LinkedHashMap<String, String>();
    
    final HashMap<String, String> unorderedCodesTypes = getCodesTypes(unorderedCodesValues);
    
    final Iterator<String> iOrderedTypes = ORDEREDTYPES.iterator();
    while (iOrderedTypes.hasNext()) {
      final String orderedType = iOrderedTypes.next();
      final Iterator<String> iUnorderedCodesTypes = unorderedCodesTypes.keySet().iterator();
      while (iUnorderedCodesTypes.hasNext()) {
        final String code = iUnorderedCodesTypes.next();
        final String value = unorderedCodesTypes.get(code);
        if (orderedType.equals(value)) {
          reorderedMap.put(code, unorderedCodesValues.get(code));
        }
      }
    }
    return reorderedMap;
  }
  
  private HashMap<String, String> getCodesTypes(final Map<String, String> codesValues) {
    final HashMap<String, String> codesTypes = new HashMap<String, String>();
    final Iterator<String> iCodesTypes = codesValues.keySet().iterator();
    while (iCodesTypes.hasNext()) {
      final String key = iCodesTypes.next();
      final String value = getType(key);
      codesTypes.put(key, value);
    }
    return codesTypes;
  }
  
  /**
   * queries the SpecialCodeRepository Descriptor:cmos_code_type table: nm_cmos_code_types returns the type of the special code
   */
  private String getType(final String pCode) {
    String type = null;
    try {
      final RepositoryView view = SICODEREPOSITORY.getView("cmos_code_type");
      final RqlStatement statement = RqlStatement.parseRqlStatement("specInstCode = ?0");
      
      final Object params[] = new Object[1];
      params[0] = pCode.trim();
      
      final RepositoryItem[] itemsArray = statement.executeQuery(view, params);
      
      if (itemsArray == null) {
        System.out.println("No items");
      } else {
        type = (String) itemsArray[0].getPropertyValue("type");
      }
    } catch (final RepositoryException re) {
      re.printStackTrace();
    }
    
    return type;
    
  }// end getType
  
  private void buildAdormentCategories() {
    adormentsCategories = new ArrayList<String>(40);
    adormentsCategories.add(0, "mono style");
    adormentsCategories.add(1, "type style");
    adormentsCategories.add(2, "border");
    adormentsCategories.add(3, "foil color");
    adormentsCategories.add(4, "ink color");
    adormentsCategories.add(5, "paper color");
    adormentsCategories.add(6, "thread color");
    adormentsCategories.add(7, "sentiment");
    adormentsCategories.add(8, "output data");
    adormentsCategories.add(9, "single initial");
    adormentsCategories.add(10, "two initials");
    adormentsCategories.add(11, "name 1");
    adormentsCategories.add(12, "name 2");
    adormentsCategories.add(13, "child name 1");
    adormentsCategories.add(14, "child name 2");
    adormentsCategories.add(15, "child name 3");
    adormentsCategories.add(16, "pet name");
    adormentsCategories.add(17, "line 1");
    adormentsCategories.add(18, "line 2");
    adormentsCategories.add(19, "line 3");
    adormentsCategories.add(20, "line 4");
    adormentsCategories.add(21, "line 5");
    adormentsCategories.add(22, "line 6");
    adormentsCategories.add(23, "personalization line 1");
    adormentsCategories.add(24, "personalization line 2");
    adormentsCategories.add(25, "personalization line 3");
    adormentsCategories.add(26, "personalization line 4");
    adormentsCategories.add(27, "what item 1");
    adormentsCategories.add(28, "what item 2");
    adormentsCategories.add(29, "what item 3");
    adormentsCategories.add(30, "address line 1");
    adormentsCategories.add(31, "address line 2");
    adormentsCategories.add(32, "address line 3");
    adormentsCategories.add(33, "address line 4");
    adormentsCategories.add(34, "year");
    adormentsCategories.add(35, "additional space");
    adormentsCategories.add(36, "numbers");
    adormentsCategories.add(37, "date");
    adormentsCategories.add(38, "phone number");
    adormentsCategories.add(39, "fax");
  }
  
  private void processAdormentValue(final String siCode, final String siCodeType, String dataEntry) {
    String webValue = null;
    String formatType = null;
    
    // System.out.println(siCode + "|" + siCodeType + "|" + dataEntry);
    
    try {
      final RepositoryView view = SICODEREPOSITORY.getView("cmos_code_definition");
      final RqlStatement statement = RqlStatement.parseRqlStatement("specInstCode = ?0 AND cmosValue = ?1");
      
      final Object params[] = new Object[2];
      params[0] = siCode;
      params[1] = dataEntry;
      
      final RepositoryItem[] itemsArray = statement.executeQuery(view, params);
      
      dataEntry = dataEntry.trim();
      
      if ((itemsArray != null) && (itemsArray.length > 0)) {
        addParameter("cmosValue", dataEntry);
        addParameter(siCodeType, dataEntry);
        
        formatType = (String) itemsArray[0].getPropertyValue("type");
        if (null != formatType) {
          addParameter("specialType", formatType);
          webValue = (String) itemsArray[0].getPropertyValue("webValue");
          addParameter("webValue", webValue);
          // System.out.println(" ==> " + formatType + "|" + webValue);
        }
      } else {
        addParameter(siCodeType, dataEntry);
      }
    } catch (final Exception re) {
      re.printStackTrace();
    }
  }
  
  private void addParameter(final String key, final String value) {
    if ((null != key) && (null != value)) {
      mParameterMap.put(key, value);
    }
  }
  
  /**
   * Get Only X left for In stock items
   * 
   * @param nmCommerceItem
   * @return String
   */
  private String getOnlyXLeftInventoryMessage(final NMCommerceItem nmCommerceItem) {
    String onlyXLeftMessage = "";
    final String stockStatus = nmCommerceItem.getTransientStatus();;
    final String prodInStock = nmCommerceItem.getProduct().getProdSkuUtil().getStatusInStockString();
    final InventoryUtil inventoryUtil = InventoryUtil.getInstance();
    
    if ((stockStatus != null) && (stockStatus.equalsIgnoreCase(prodInStock) || (stockStatus.indexOf(prodInStock) != -1))) {
      final int maxInvThreshold = inventoryUtil.getMaxInventoryThreshold(nmCommerceItem.getProductId());
      final int skuStockLevel = Integer.valueOf(nmCommerceItem.getSku().getStockLevel());
      if ((maxInvThreshold > 0) && (skuStockLevel <= maxInvThreshold)) {
        onlyXLeftMessage = "Only " + skuStockLevel + " Left";
      }
    }
    return onlyXLeftMessage;
  }
  
  /**
   * Build Promotions
   * 
   * @param order
   * @return JSONArray
   */
  @SuppressWarnings("unchecked")
  public JSONArray buildPromotions(final NMOrderImpl order) {
    final JSONArray promotions = new JSONArray();
    final String defaultPromoCode = "";
    boolean isGWPPromotion = false;
    boolean buildSRPromo = false;
    String activePromoCodeText = CheckoutComponents.getShopRunner().getMessagePromoApplied();
    double discountValue = 0.0;
    double tempDistValue = 0.0;
    final Collection<NMPromotion> awardedPromotions = order.getAwardedPromotions();
    if (!awardedPromotions.isEmpty()) {
      final Iterator<NMPromotion> iterator = awardedPromotions.iterator();
      while (iterator.hasNext()) {
        final NMPromotion promotion = iterator.next();
        activePromoCodeText = CheckoutComponents.getShopRunner().getMessagePromoApplied();
        if (promotion.hasPromoCodes()) {
          final String currentPromoCode = promotion.getPromoCodes();
          discountValue = getDiscountAmtBasedPromo(order, currentPromoCode.trim());
          tempDistValue = tempDistValue + discountValue;
          if (promotion.getPromotionClass() == NMPromotion.GIFT_WITH_PURCHASE) {
            activePromoCodeText += ": " + currentPromoCode;
            promotions.add(buildSRPromotion(currentPromoCode, "GWP", discountValue, activePromoCodeText));
          } else {
            activePromoCodeText += ": " + currentPromoCode;
            promotions.add(buildSRPromotion(currentPromoCode, "PROMO CODE DISCOUNT", discountValue, activePromoCodeText));
          }
        } else {
          if (promotion.getPromotionClass() == NMPromotion.GIFT_WITH_PURCHASE) {
            isGWPPromotion = true;
          } else if (!promotion.getName().contains("ShopRunner") && (promotion.getPromotionClass() != NMPromotion.EXTRA_ADDRESS)) {
            buildSRPromo = true;
          }
        }
      }
      if ((tempDistValue != order.getDisplayableTotalDiscountFromItems()) || (order.getDisplayableTotalDiscountFromItems() == 0)) {
        if (isGWPPromotion) {
          promotions.add(buildSRPromotion(defaultPromoCode, "GWP", order.getDisplayableTotalDiscountFromItems(), activePromoCodeText));
        } else if (buildSRPromo) {
          promotions.add(buildSRPromotion(defaultPromoCode, "PROMO CODE DISCOUNT", order.getDisplayableTotalDiscountFromItems(), activePromoCodeText));
        }
      }
    } else {
      if (order.getDisplayableTotalDiscountFromItems() > 0.0) {
        promotions.add(buildSRPromotion(defaultPromoCode, "PROMO CODE DISCOUNT", order.getDisplayableTotalDiscountFromItems(), activePromoCodeText));
      }
    }
    return promotions;
  }
  
  /**
   * Build SR promotion JSON
   * 
   * @param promoCode
   * @param promoType
   * @param promoValue
   * @param promoMessage
   * @return JSONObject
   */
  @SuppressWarnings("unchecked")
  private JSONObject buildSRPromotion(final String promoCode, final String promoType, final double promoValue, final String promoMessage) {
    final JSONObject promotion = new JSONObject();
    promotion.put("code", promoCode);
    promotion.put("type", promoType);
    promotion.put("value", promoValue);
    promotion.put("message", promoMessage);
    promotion.put("status", ShopRunnerConstants.SR_STATUS_0_DEFAULT);
    return promotion;
  }
  
  /**
   * Get Discount amount from promo code
   * 
   * @param order
   * @param currentPromoCode
   * @return double
   */
  private double getDiscountAmtBasedPromo(final NMOrderImpl order, final String currentPromoCode) {
    double discountValue = 0.0;
    final NMPromotion promotion = order.getAwardedPromotion(currentPromoCode);
    if (promotion != null) {
      final int promotionClass = promotion.getPromotionClass();
      discountValue = (promotionClass == NMPromotion.PERCENT_OFF) || (promotionClass == NMPromotion.RULE_BASED) ? order.getDisplayableTotalDiscountFromItems() : 0.0;
    }
    return discountValue;
  }
  
  public int getActionRequiredPromoType(final String promoCode) {
    final int validationPromo = 0;
    final ComponentUtils componentUtils = ComponentUtils.getInstance();
    final int[] promoArrayTypes = componentUtils.getPromotionArrayTypes();
    final int checkOutArrayLength = promoArrayTypes.length;
    
    for (int i = 0; i < checkOutArrayLength; i++) {
    	final NMPromotionArray promotionArray = componentUtils.getPromotionArray(promoArrayTypes[i]);
    	final NMPromotion nmpromo = promotionArray.getActivePromotionsWithPromoCode(promoCode);
    	if(null != nmpromo){
        if (nmpromo.requiresEmailValidation()) {
          return NMPromotion.GIFT_WITH_PURCHASE_SELECT;
        }
        
        if ((nmpromo.getPromotionClass() == NMPromotion.GIFT_WITH_PURCHASE) && nmpromo.hasPromoCodes()) {
          final GiftWithPurchase gwpPromotion = (GiftWithPurchase) nmpromo;
          final NMProduct gwpProd = new NMProduct(gwpPromotion.getGwpProduct());
          if ((gwpProd.getSellableVariation1Count() > 1) || (gwpProd.getSellableVariation2Count() > 1)) {
            return nmpromo.getPromotionClass();
          }
        }
        if (nmpromo.getPromotionClass() == NMPromotion.GIFT_WRAP) {
          return nmpromo.getPromotionClass();
        }
     }
    }
    return validationPromo;
  }
  
  /**
   * Check if promo code has already been applied to order
   * 
   * @param activatedPromoCode
   * @param promoCode
   * @return
   */
  public boolean isPromoAlreadyApplied(final String activatedPromoCode, final String promoCode) {
    boolean isAlreadyApplied = false;
    if ((activatedPromoCode != null) && !activatedPromoCode.equals("")) {
      final StringTokenizer token = new StringTokenizer(activatedPromoCode, ",");
      
      while (token.hasMoreElements()) {
        if (token.nextElement().equals(promoCode.toUpperCase())) {
          isAlreadyApplied = true;
          break;
        }
      }
    }
    return isAlreadyApplied;
  }
  
  /**
   * Build Gift cards
   * 
   * @param order
   * @return JSONArray
   */
  @SuppressWarnings("unchecked")
  public JSONArray buildGiftCards(final NMOrderImpl order) {
    final List<NMCreditCard> giftCardsOnOrder = order.getGiftCards();
    final JSONArray giftCardsArray = new JSONArray();
    for (final NMCreditCard giftCard : giftCardsOnOrder) {
      final NMJSONObject giftCardObject = new NMJSONObject();
      giftCardObject.put("number", "x" + giftCard.getMaskedCreditCardNumber().replaceAll("\\*", ""));
      giftCardObject.put("gcBalance", giftCard.getAmount());
      giftCardsArray.add(giftCardObject);
    }
    return giftCardsArray;
  }
  
  /**
   * Get gift cards from order
   * 
   * @param order
   * @return
   */
  public List<GiftCard> getGiftCardsFromOrder(final NMOrderImpl order) {
    final List<GiftCard> giftCards = new ArrayList<GiftCard>();
    for (final NMCreditCard card : order.getGiftCards()) {
      final GiftCard gc = new GiftCard();
      gc.setCardNumber(card.getCreditCardNumber());
      gc.setCid(card.getCidTransient());
      giftCards.add(gc);
    }
    return giftCards;
  }
  
  /**
   * Logger
   * 
   * @return
   */
  private GenericService getLogger() {
    return CommonComponentHelper.getLogger();
  }
  
  public void clearShopRunnerData(final NMProfile profile, final ShoppingCartHandler cartHandler) throws Exception {
    profile.setShopRunnerEmail(null);
    profile.setShopRunnerFirstName(null);
    profile.setShopRunnerLastName(null);
    profile.setShopRunnerPhoneNumber(null);
    profile.setShopRunnerAddress1(null);
    profile.setShopRunnerAddress2(null);
    profile.setShopRunnerCity(null);
    profile.setShopRunnerState(null);
    profile.setShopRunnerZip(null);
    profile.setShopRunnerCountry(null);
    
    final NMOrderImpl order = cartHandler.getNMOrder();
    final NMOrderManager orderMgr = (NMOrderManager) cartHandler.getOrderManager();
    order.setShopRunner(false);
    // Remove only gift cards applied in SR modal
    for (final PaymentGroup pg : order.getGiftCards()) {
      PaymentUtil.getInstance().removePaymentGroup(order, orderMgr, pg.getId());
    }
  }
}
