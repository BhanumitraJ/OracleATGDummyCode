package com.nm.commerce;

import static com.nm.common.INMGenericConstants.EMPTY_STRING;
import static com.nm.common.INMGenericConstants.VIRTUAL_GIFT_CARD;
import static com.nm.international.fiftyone.checkoutapi.OrderRepositoryConstants.CONFIGURATION_KEY;
import static com.nm.international.fiftyone.checkoutapi.OrderRepositoryConstants.OPTION_CHOICES;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import atg.commerce.CommerceException;
import atg.commerce.order.AuxiliaryData;
import atg.commerce.order.CommerceItemImpl;
import atg.commerce.order.CommerceItemManager;
import atg.commerce.order.HardgoodShippingGroup;
import atg.commerce.order.OrderImpl;
import atg.commerce.order.OrderManager;
import atg.commerce.order.ShippingGroup;
import atg.commerce.order.ShippingGroupCommerceItemRelationship;
import atg.commerce.order.ShippingGroupRelationship;
import atg.commerce.pricing.ItemPriceInfo;
import atg.core.util.StringUtils;
import atg.nucleus.Nucleus;
import atg.repository.MutableRepository;
import atg.repository.MutableRepositoryItem;
import atg.repository.Repository;
import atg.repository.RepositoryException;
import atg.repository.RepositoryItem;
import atg.repository.RepositoryView;
import atg.repository.rql.RqlStatement;

import com.nm.ajax.checkout.utils.RepositoryUtils;
import com.nm.collections.GiftWithPurchase;
import com.nm.collections.GiftWithPurchaseArray;
import com.nm.collections.GiftWithPurchaseSelect;
import com.nm.collections.GiftWithPurchaseSelectArray;
import com.nm.collections.PurchaseWithPurchase;
import com.nm.collections.PurchaseWithPurchaseArray;
import com.nm.commerce.catalog.NMProduct;
import com.nm.commerce.catalog.NMSku;
import com.nm.commerce.order.IOrderItem;
import com.nm.commerce.pricing.Markdown;
import com.nm.commerce.pricing.NMItemPriceInfo;
import com.nm.commerce.pricing.NMPricingTools;
import com.nm.commerce.promotion.CheckCsrGwp;
import com.nm.commerce.promotion.CheckCsrGwpSelect;
import com.nm.commerce.promotion.CheckCsrPwp;
import com.nm.commerce.promotion.NMOrderImpl;
import com.nm.common.INMGenericConstants;
import com.nm.components.CommonComponentHelper;
import com.nm.international.fiftyone.checkoutapi.OrderRepositoryConstants;
import com.nm.monogram.utils.MonogramConstants;
import com.nm.utils.BrandSpecs;
import com.nm.utils.GenericLogger;
import com.nm.utils.ListMaker;
import com.nm.utils.NmoUtils;
import com.nm.utils.ObjectUtilities;
import com.nm.utils.ProdSkuUtil;
import com.nm.utils.StringUtilities;

/**
 * This class overrides the functionality in CommerceItemImpl. This class should be an interface that extends CommerceItem. A new class NMCommerceItemImpl should be created that extends
 * CommerceItemImpl and the functionality in this class should be placed there.
 *
 * @author ???? Last Modified $Date: 2014/06/29 12:31:10CDT $ Last Modified By $Author: nmavj1 $
 * @version $Revision: 1.51.2.26.1.1 $
 */
public class NMCommerceItem extends CommerceItemImpl implements Cloneable, IOrderItem, Comparable<Object> {
  private static final long serialVersionUID = 1L;
  
  public static final String MISC_FLAG_SBR = "sbr";
  public static final int NO_GIFT_NOTE = 0;
  public static final int BLANK_GIFT_NOTE = 1;
  public static final int CUSTOM_GIFT_NOTE = 2;
  public static final int TEMP_RESTRICTION = 1;
  public static final int PERM_RESTRICTION = 2;
  
  private final GiftWithPurchaseArray gwpArray;
  private final GiftWithPurchaseSelectArray gwpSelectArray;
  private final PurchaseWithPurchaseArray pwpArray;
  private boolean shoppingCardItemSplitInProgress = false;
  private final GenericLogger log = CommonComponentHelper.getLogger();
  private final ProdSkuUtil prodSkuUtil = CommonComponentHelper.getProdSkuUtil();
  private int restrictionType;
  private String shipToStoreInterval;
  boolean payPalRestricted = false;
  private String shipFrom;
  private String shipReceived;
  private double bogoQualifiedItemDiscount = 0d;
  private double bogoQualifyingItemMarkup = 0d;
  private String cmosEstimatedDeliveryDate;
  
private Map<String, String> optionalChoices;
  
  public NMCommerceItem() {
    this(null);
  }
  
  public NMCommerceItem(final MutableRepositoryItem commerceItem) {
    setRepositoryItem(commerceItem);
    
    gwpArray = CheckCsrGwp.getGiftWithPurchaseArray();
    gwpSelectArray = CheckCsrGwpSelect.getGiftWithPurchaseSelectArray();
    pwpArray = CheckCsrPwp.getPurchaseWithPurchaseArray();
  }
  
  public NMProduct getProduct() {
    return new NMProduct((RepositoryItem) getAuxiliaryData().getProductRef());
  }
  
  public NMSku getSku() {
    return new NMSku((RepositoryItem) getAuxiliaryData().getCatalogRef());
  }

  public boolean isPayPalRestricted() {
    return payPalRestricted;
  }
  
  public void setPayPalRestricted(final boolean payPalRestricted) {
    this.payPalRestricted = payPalRestricted;
  }

  /**
   * @return
   */
  public boolean getProductDisplayFlag() {
    boolean returnValue = true;
    
    try {
      final RepositoryItem productItem = CommonComponentHelper.getProductRepository().getItem(getProductId(), "product");
      
      if (productItem != null) {
        final Boolean displayFlag = (Boolean) productItem.getPropertyValue("flgDisplay");
        returnValue = displayFlag != null ? displayFlag.booleanValue() : true;
      } else {
        returnValue = false;
      }
    } catch (final Exception exception) {}
    
    return returnValue;
  }
  
  /**
   * @return the restrictionType
   */
  public int getRestrictionType() {
    return restrictionType;
  }
  
  /**
   * @param restrictionType
   *          the restrictionType to set
   */
  public void setRestrictionType(final int restrictionType) {
    this.restrictionType = restrictionType;
  }
  
  public String getShipToStoreInterval() {
    return shipToStoreInterval;
  }
  
  public void setShipToStoreInterval(final String shipToStoreInterval) {
    this.shipToStoreInterval = shipToStoreInterval;
  }
  
  /**
   * Does the product have a suggested replenishment interval
   *
   * @return
   */
  public boolean isSuggReplenishmentInterval() {
    boolean returnValue = false;
    final String productId = getProductId();
    final NMProduct nmProduct = new NMProduct(productId);
    if (StringUtilities.isNotEmpty(nmProduct.getSuggReplenishmentInterval())) {
      returnValue = true;
    }
    return returnValue;
  }
  
  public boolean isShoppingCardItemSplitInProgress() {
    return shoppingCardItemSplitInProgress;
  }
  
  public void setShoppingCardItemSplitInProgress(final boolean shoppingCardItemSplitInProgress) {
    this.shoppingCardItemSplitInProgress = shoppingCardItemSplitInProgress;
  }
  
  public boolean equalCommerceItems(final NMCommerceItem ci, final boolean ignoreGWPShipGroup) {
    return equalCommerceItems(ci, ignoreGWPShipGroup, true);
  }
  
  public boolean equalCommerceItems(final NMCommerceItem ci, final boolean ignoreGWPShipGroup, final boolean ignorePercentOffPromotions) {
    if (null == ci) {
      return false;
    }
    
    // To prevent commerce items with different percent off promotions applied from being
    // consolidated, we should test for percent off promotions
    // applied before determining the equality of the commerce items.
    boolean poPromoMatch = false;
    if (!ignorePercentOffPromotions) {
      Integer currPromoPercentOff;
      try {
        currPromoPercentOff = new Integer(((NMItemPriceInfo) getPriceInfo()).getPromoPercentOff());
      } catch (final NumberFormatException e) {
        currPromoPercentOff = 0;
      }
      Integer ciPromoPercentOff;
      try {
        ciPromoPercentOff = new Integer(((NMItemPriceInfo) ci.getPriceInfo()).getPromoPercentOff());
      } catch (final NumberFormatException e) {
        ciPromoPercentOff = 0;
      }
      
      if (currPromoPercentOff.equals(0) && ciPromoPercentOff.equals(0)) {
        poPromoMatch = true;
      } else if ((currPromoPercentOff > 0) || (ciPromoPercentOff > 0)) {
        final Map<String, Markdown> currPromotionsApplied = ((NMItemPriceInfo) getPriceInfo()).getPromotionsApplied();
        final Map<String, Markdown> ciPromotionsApplied = ((NMItemPriceInfo) ci.getPriceInfo()).getPromotionsApplied();
        
        if (currPromotionsApplied.equals(ciPromotionsApplied)) {
          poPromoMatch = true;
        } else {
          final ArrayList<String> currPercentOffPromoKeys = getPercentOffPromoKeys(currPromotionsApplied);
          final ArrayList<String> ciPercentOffPromoKeys = getPercentOffPromoKeys(ciPromotionsApplied);
          if (currPercentOffPromoKeys.containsAll(ciPercentOffPromoKeys) && ciPercentOffPromoKeys.containsAll(currPercentOffPromoKeys)) {
            poPromoMatch = true;
          }
        }
      }
    }
    boolean siCodesMatch = false;
    if ((getSpecialInstCodes() == null) && (ci.getSpecialInstCodes() == null)) {
      siCodesMatch = true;
    } else if ((getSpecialInstCodes() == null) || (ci.getSpecialInstCodes() == null)) {
      siCodesMatch = false;
    } else if (getSpecialInstCodes().equals(ci.getSpecialInstCodes())) {
      siCodesMatch = true;
    } else {
      siCodesMatch = false;
    }
    
    boolean delDatesMatch = false;
    final Date date1 = getReqDeliveryDate();
    final Date date2 = ci.getReqDeliveryDate();
    if (date1 == date2) {
      delDatesMatch = true;
    } else {
      delDatesMatch = date1 == null ? false : date1.equals(date2);
    }
    
    // check for BOPS
    boolean pickupStoreNoMatch = false;
    final String pickupStoreNo = getPickupStoreNo();
    if ((pickupStoreNo == null) && (ci.getPickupStoreNo() == null)) {
      pickupStoreNoMatch = true;
    } else if ((pickupStoreNo != null) && pickupStoreNo.equals(ci.getPickupStoreNo())) {
      pickupStoreNoMatch = true;
    }
    
    boolean shipToStoreFlgMatch = false;
    if (getShipToStoreFlg() == ci.getShipToStoreFlg()) {
      shipToStoreFlgMatch = true;
    }
    
		boolean isConfiguratorProductMatch = false;
		if ((getConfigurationKey() != null && ci.getConfigurationKey() != null && getConfigurationKey().trim().equals(ci.getConfigurationKey().trim())) || (getConfigurationKey() == null && ci.getConfigurationKey() == null)) {
			isConfiguratorProductMatch = true;
		}
    
    if (getCmosCatalogId().equals(ci.getCmosCatalogId()) && getCmosItemCode().equals(ci.getCmosItemCode()) && getCmosSKUId().equals(ci.getCmosSKUId()) && equalGiftWrap(ci) && siCodesMatch
            && delDatesMatch && equalReplenishmentInterval(ci) && pickupStoreNoMatch && shipToStoreFlgMatch && isConfiguratorProductMatch && (ignorePercentOffPromotions || poPromoMatch)) {
      if (ignoreGWPShipGroup && isGwpItem() && ci.isGwpItem()) {
        return true;
      }
      
      return equalShippinGroups(ci);
    }
    
    return false;
  }
  
  private ArrayList<String> getPercentOffPromoKeys(final Map<String, Markdown> ciPromotionsApplied) {
    final ArrayList<String> percentOffPromoKeys = new ArrayList<String>();
    final Set<String> keys = ciPromotionsApplied.keySet();
    final Iterator<String> i = keys.iterator();
    while (i.hasNext()) {
      final Markdown markdown = ciPromotionsApplied.get(i.next());
      // System.out.println("   -- markdown.getPromoKey() " + markdown.getPromoKey());
      if (markdown.getType() == Markdown.PERCENT_OFF) {
        Integer percentoff;
        try {
          percentoff = new Integer(markdown.getPercentDiscount());
        } catch (final NumberFormatException e) {
          percentoff = 0;
        }
        if (percentoff > 0) {
          percentOffPromoKeys.add(markdown.getPromoKey());
        }
      }
    }
    return percentOffPromoKeys;
  }
  
  private boolean equalGiftWrap(final NMCommerceItem ci) {
    final RepositoryItem gw1 = getGiftWrap();
    final RepositoryItem gw2 = ci.getGiftWrap();
    if (gw1 == null) {
      return gw2 == null;
    }
    
    if (gw2 == null) {
      return gw1 == null;
    }
    
    return ObjectUtilities.equalObjects(gw1.getPropertyValue("name"), gw2.getPropertyValue("name")) && (getGiftNote() == ci.getGiftNote())
            && ObjectUtilities.equalObjects(getNoteLine1(), ci.getNoteLine1()) && ObjectUtilities.equalObjects(getNoteLine2(), ci.getNoteLine2())
            && ObjectUtilities.equalObjects(getNoteLine3(), ci.getNoteLine3()) && ObjectUtilities.equalObjects(getNoteLine4(), ci.getNoteLine4());
  }
  
  private boolean equalReplenishmentInterval(final NMCommerceItem ci) {
    
    String interval1 = "";
    String interval2 = "";
    
    if (getSelectedInterval() != null) {
      interval1 = getSelectedInterval();
    }
    
    if (ci.getSelectedInterval() != null) {
      interval2 = ci.getSelectedInterval();
    }
    
    if (interval1.equals(interval2)) {
      return true;
    } else {
      return false;
    }
  }
  
  private boolean equalShippinGroups(final NMCommerceItem ci) {
    final String sgId1 = ((ShippingGroupCommerceItemRelationship) getShippingGroupRelationships().get(0)).getShippingGroup().getId();
    final String sgId2 = ((ShippingGroupCommerceItemRelationship) ci.getShippingGroupRelationships().get(0)).getShippingGroup().getId();
    if (sgId1.equals(sgId2)) {
      return getServicelevel().equals(ci.getServicelevel());
    }
    
    return false;
  }
  
  public boolean isMultiWithDollarHurdleGwp() {
    final String promoCat = getCoreMetricsCategory();
    if (!"gwp".equals(promoCat)) {
      return false;
    }
    
    final String promoKeyList = getSendCmosPromoKey();
    if (null == promoKeyList) {
      return false;
    }
    
    final String[] promoKeys = promoKeyList.split(",");
    if (null == promoKeys) {
      return false;
    }
    
    for (int i = 0; i < promoKeys.length; ++i) {
      final GiftWithPurchase gwp = gwpArray.getPromotion(promoKeys[i]);
      if ((null != gwp) && gwp.isMultiGWPDollarQualActive()) {
        return true;
      }
    }
    
    return false;
  }
  
  public boolean isGwpItem() {
    final String promoCat = getCoreMetricsCategory();
    
    if (!"gwp".equals(promoCat)) {
      return false;
    }
    
    return true;
  }
  
  public boolean isPwpItem() {
    final String promoCat = getCoreMetricsCategory();
    
    if (!"pwp".equals(promoCat)) {
      return false;
    }
    
    return true;
  }
  
  public boolean isMustShipWithItemGwp() {
    if (!isGwpItem() && !isPwpItem()) {
      return false;
    }
    
    final String promoKeyList = getSendCmosPromoKey();
    if (null == promoKeyList) {
      return false;
    }
    
    final String[] promoKeys = promoKeyList.split(",");
    if (null == promoKeys) {
      return false;
    }
    
    for (int i = 0; i < promoKeys.length; ++i) {
      // Regular GWPs
      final GiftWithPurchase gwp = gwpArray.getPromotion(promoKeys[i]);
      if ((null != gwp) && gwp.isGWPShipwithItemActive()) {
        return true;
      }
      
      // GWP Selects
      final GiftWithPurchaseSelect gwpSelect = gwpSelectArray.getPromotion(promoKeys[i]);
      if ((null != gwpSelect) && gwpSelect.isGWPShipwithItemActive()) {
        return true;
      }
      
      // Regular PWPs
      final PurchaseWithPurchase pwp = pwpArray.getPromotion(promoKeys[i]);
      
      if ((null != pwp) && pwp.isGWPShipwithItemActive()) {
        return true;
      }
      
    }
    
    return false;
  }
  
  /**
   * Checks to see if the dropship flag is set to true or false.
   *
   * @author nmkdj
   * @return boolean
   */
  public boolean isDropship() {
    try {
      final RepositoryItem sku = CommonComponentHelper.getProductRepository().getItem(getCatalogRefId(), "sku");
      final Boolean flgDropship = (Boolean) sku.getPropertyValue("flgDropship");
      
      if ((flgDropship != null) && flgDropship.booleanValue()) {
        return true;
      } else {
        return false;
      }
    } catch (final Exception e) {
      return false;
    }
  }
  
  public String getSkuNumber() {
    return getCatalogRefId();
  }
  
  public boolean getDropshipInHouse() {
    try {
      final RepositoryItem sku = CommonComponentHelper.getProductRepository().getItem(getCatalogRefId(), "sku");
      final Boolean flgDropship = (Boolean) sku.getPropertyValue("flgDropship");
      final Boolean inStock = (Boolean) sku.getPropertyValue("inStock");
      
      if ((flgDropship != null) && (inStock != null) && flgDropship.booleanValue() && inStock.booleanValue()) {
        return true;
      } else {
        return false;
      }
    } catch (final Exception e) {
      return false;
    }
  }
  
  /**
   * This Method will check whether sku is eligible for back order message or not for cart pages.
   * @return isbackOrderStatus
   */

	public boolean isDisplayBackOrderMessage() {
		RepositoryItem product = null;
		boolean isbackOrderStatus = false;
		RepositoryItem sku = null;
		final BrandSpecs brandSpecs = CommonComponentHelper.getBrandSpecs();
		final ProdSkuUtil ProdSkuUtil = CommonComponentHelper.getProdSkuUtil();
		if( brandSpecs.isEnableOrderStatusChange()){
			try {
				sku = CommonComponentHelper.getProductRepository().getItem(getCatalogRefId(), "sku");
				product = ProdSkuUtil.getProductRepositoryItem(getCmosCatalogId(),getCmosItemCode());				
				if (sku != null && product != null) {
					boolean isBackOrderMessageEnabled = ProdSkuUtil.isBackOrderMessageEnabled(product, sku);
					int stockLevel = Integer.parseInt((String) sku.getPropertyValue("stockLevel"));
					if (isBackOrderMessageEnabled && getQuantity() > stockLevel  ) {
						isbackOrderStatus = true;
					}
				}
			} catch (RepositoryException e) {
				isbackOrderStatus = false;
				if (log.isLoggingError()) {
					log.logError("ERROR***: RepositoryException  in isDisplayBackOrderMessage"+ e);
				}
			} catch (Exception e) {
				isbackOrderStatus = false;
				if (log.isLoggingError()) {
					log.logError("ERROR***: exception  in isDisplayBackOrderMessage"+ e);
				}
			}
		}
		return isbackOrderStatus;
	}
  
	
  
  public boolean isDropshipInHouse() {
    return getDropshipInHouse();
  }
  
  public void setDropshipInHouse(final boolean dropshipInHouse) {
    // this.dropshipInHouse = dropshipInHouse;
  }
  
  public boolean getTransientStatusIncludesQuantity() {
    try {
      if (getTransientStatus().toLowerCase().indexOf("quantity") != -1) {
        return true;
      } else {
        return false;
      }
    } catch (final Exception e) {
      return false;
    }
  }
  
  public boolean isTransientStatusIncludesQuantity() {
    return getTransientStatusIncludesQuantity();
  }
  
  public void setPromoType(final String promoType) {
    setPropertyValue("promoType", promoType);
  }
  
  public String getPromoType() {
    return (String) getPropertyValue("promoType");
  }
  
  public void setPromoTimeStamp(final String promoTimeStamp) {
    setPropertyValue("promoTimeStamp", promoTimeStamp);
  }
  
  public String getPromoTimeStamp() {
    final String promoTimeStamp = (String) getPropertyValue("promoTimeStamp");
    if (promoTimeStamp == null) {
      return "";
    } else {
      return promoTimeStamp.trim();
    }
  }
  
  public void setRegistryId(final String registryId) {
    setPropertyValue("registryId", registryId);
  }
  
  public String getRegistryId() {
    return (String) getPropertyValue("registryId");
  }
  
  public void setRelatedProduct(final String pRelatedProduct) {
    setPropertyValue("relatedProduct", pRelatedProduct);
  }
  
  public String getRelatedProduct() {
    return (String) getPropertyValue("relatedProduct");
  }
  
  public void setCoreMetricsCategory(final String coreMetricsCategory) {
    setPropertyValue("coreMetricsCategory", coreMetricsCategory);
  }
  
  public String getCoreMetricsCategory() {
    return (String) getPropertyValue("coreMetricsCategory");
  }
  
  public void setPromoName(final String promoName) {
    setPropertyValue("promoName", promoName);
  }
  
  public String getPromoName() {
    return (String) getPropertyValue("promoName");
  }
  
  public void setPickupStoreNo(final String storeNo) {
    setPropertyValue("pickupStoreNo", storeNo);
  }
  
  public String getPickupStoreNo() {
    return (String) getPropertyValue("pickupStoreNo");
  }
  
  public void setShipToStoreFlg(final boolean shipToStoreFlg) {
    setPropertyValue("shipToStoreFlg", shipToStoreFlg);
  }
  
  public boolean getShipToStoreFlg() {
    return ((Boolean) getPropertyValue("shipToStoreFlg")).booleanValue();
  }
  
  public void setFulfillmentFacility(final String fulfillmentFacility) {
    setPropertyValue("fulfillmentFacility", fulfillmentFacility);
  }
  
  public String getFulfillmentFacility() {
    return (String) getPropertyValue("fulfillmentFacility");
  }
  
  public void setSpecialInstCodes(final Map<String, String> specialInstCodes) {
    setPropertyValue("specialInstCodes", specialInstCodes);
  }
  
  public Map<String, String> getSpecialInstCodes() {
    if (!StringUtils.isBlank(getConfigurationKey())) {
  		return new HashMap<String, String>();
    }
    @SuppressWarnings("unchecked")
    final Map<String, String> codes = (Map<String, String>) getPropertyValue("specialInstCodes");
    return codes;
  }
  
  public void setCodeSetType(final String codeSetType) {
    setPropertyValue("codeSetType", codeSetType);
  }
  
  public String getCodeSetType() {
    return (String) getPropertyValue("codeSetType");
  }
  
  /**
   * @param specialInstructionFlag
   *          the specialInstructionFlag to set
   */
  public void setSpecialInstructionFlag(final String specialInstructionFlag) {
    setPropertyValue("specialInstructionFlag", specialInstructionFlag);
  }

  public String getSpecialInstructionFlag() {
	  RepositoryItem product = (RepositoryItem) getAuxiliaryData().getProductRef();
    String specialInstructionFlag = (String) product.getPropertyValue("specialInstructionFlag");
    if (StringUtilities.isEmpty(specialInstructionFlag)) {
      specialInstructionFlag = MonogramConstants.NO_SPECIAL_INSTRUCTION_FLAG;
      if (StringUtilities.isNotEmpty((String) product.getPropertyValue("codeSetType"))) {
    	  //Old flow
		  specialInstructionFlag = MonogramConstants.REQUIRED_MONOGRAM_FLAG;
    }
    } else if (StringUtilities.areEqual(specialInstructionFlag, MonogramConstants.NO_SPECIAL_INSTRUCTION_FLAG) && StringUtilities.isNotEmpty((String) product.getPropertyValue("codeSetType"))) {
      // Old flow
      specialInstructionFlag = MonogramConstants.REQUIRED_MONOGRAM_FLAG;
    }
    
    return specialInstructionFlag;
  }
  
  /**
   * @param specialInstructionPrice
   *          the specialInstructionPrice to set
   */
  public void setSpecialInstructionPrice(final Double specialInstructionPrice) {
    setPropertyValue("specialInstructionPrice", specialInstructionPrice);
  }

  public double getSpecialInstructionPrice() { 
      RepositoryItem product = (RepositoryItem) getAuxiliaryData().getProductRef();
    Double specialInstructionPrice = (Double) product.getPropertyValue("specialInstructionPrice");
      if (null == specialInstructionPrice) {
        specialInstructionPrice = 0.0d;
      }
    return specialInstructionPrice;
  }
  
  public void setSuiteId(final String suiteId) {
    setPropertyValue("suiteId", suiteId);
  }
  
  public String getSuiteId() {
    return (String) getPropertyValue("suiteId");
  }
  
  public void setTransientAvailableDate(final String transientAvailableDate) {
    setPropertyValue("transientAvailableDate", transientAvailableDate);
  }
  
  public String getTransientAvailableDate() {
    return (String) getPropertyValue("transientAvailableDate");
  }
  
  public void setAutoGiftwrapFlg(final boolean autoGiftwrapFlg) {
    setPropertyValue("autoGiftwrapFlg", new Boolean(autoGiftwrapFlg));
  }
  
  public boolean getAutoGiftwrapFlg() {
    return ((Boolean) getPropertyValue("autoGiftwrapFlg")).booleanValue();
  }
  
  public void setAlipayEligibleFlg(final boolean alipayEligibleFlg) {
    setPropertyValue("alipayEligibleFlg", new Boolean(alipayEligibleFlg));
  }
  
  public boolean getAlipayEligibleFlg() {
    return ((Boolean) getPropertyValue("alipayEligibleFlg")).booleanValue();
  }
  
  public void setTransientStatus(final String transientStatus) {
    setPropertyValue("transientStatus", transientStatus);
  }
  
  public String getTransientStatus() {
    return (String) getPropertyValue("transientStatus");
  }
  
  public void setReqDeliveryDate(final Date reqDeliveryDate) {
    setPropertyValue("reqDeliveryDate", reqDeliveryDate);
  }
  
  public Date getReqDeliveryDate() {
    return (Date) getPropertyValue("reqDeliveryDate");
  }
  
  public void setCommerceItemDate(final Date commerceItemDate) {
    setPropertyValue("commerceItemDate", commerceItemDate);
  }
  
  public Date getCommerceItemDate() {
    return (Date) getPropertyValue("commerceItemDate");
  }
  
  public void setReporting_code(final String in) {
    setPropertyValue("reporting_code", in);
  }
  
  public String getReporting_code() {
    final Object o = getPropertyValue("reporting_code");
    if (o == null) {
      return "";
    }
    return (String) o;
  }
  
  public void setDeptCode(final String deptCode) {
    setPropertyValue("deptCode", deptCode);
  }
  
  public String getDeptCode() {
    return (String) getPropertyValue("deptCode");
  }
  
  public void setSourceCode(final String sourceCode) {
    setPropertyValue("sourceCode", sourceCode);
  }
  
  public String getSourceCode() {
    return (String) getPropertyValue("sourceCode");
  }
  
  public void setWebDesignerName(final String webDesignerName) {
    setPropertyValue("cmDesignerName", webDesignerName);
  }
  
  public String getWebDesignerName() {
    return (String) getPropertyValue("cmDesignerName");
  }
  
  public void setCmosCatalogId(final String cmosCatalogId) {
    setPropertyValue("cmosCatalogId", cmosCatalogId);
  }
  
  public String getCmosCatalogId() {
    return (String) getPropertyValue("cmosCatalogId");
  }
  
  public void setCmosSKUId(final String cmosSKUId) {
    setPropertyValue("cmosSKUId", cmosSKUId);
  }
  
  public String getCmosSKUId() {
    return (String) getPropertyValue("cmosSKUId");
  }
  
  public void setCmosItemCode(final String cmosItemCode) {
    setPropertyValue("cmosItemCode", cmosItemCode);
  }
  
  public String getCmosItemCode() {
    return (String) getPropertyValue("cmosItemCode");
  }
  
  public void setCmosDetailId(final String cmosDetailId) {
    setPropertyValue("cmosDetailId", cmosDetailId);
  }
  
  public String getCmosDetailId() {
    return (String) getPropertyValue("cmosDetailId");
  }
  
  public void setCmosProdName(final String cmosProdName) {
    setPropertyValue("cmosProdName", cmosProdName);
  }
  
  public String getCmosProdName() {
    return (String) getPropertyValue("cmosProdName");
  }
  
  public void setProdVariation1(final String prodVariation1) {
    setPropertyValue("prodVariation1", prodVariation1);
  }
  
  public String getProdVariation1() {
    return (String) getPropertyValue("prodVariation1");
  }
  
  public void setProdVariation2(final String prodVariation2) {
    setPropertyValue("prodVariation2", prodVariation2);
  }
  
  public String getProdVariation2() {
    return (String) getPropertyValue("prodVariation2");
  }
  
  public void setProdVariation3(final String prodVariation3) {
    setPropertyValue("prodVariation3", prodVariation3);
  }
  
  public String getProdVariation3() {
    return (String) getPropertyValue("prodVariation3");
  }
  
  public void setStatusCode(final String statusCode) {
    setPropertyValue("statusCode", statusCode);
  }
  
  public String getStatusCode() {
    return (String) getPropertyValue("statusCode");
  }
  
  public void setShipMethod(final String shipMethod) {
    setPropertyValue("shipMethod", shipMethod);
  }
  
  public String getShipMethod() {
    return (String) getPropertyValue("shipMethod");
  }
  
  public void setServicelevel(final String serviceLevel) {
    if (StringUtilities.isNotNull(serviceLevel)) {
      setPropertyValue("serviceLevel", serviceLevel);
    }
  }
  
  public String getEmployeeStoreCall() {
    return (String) getPropertyValue("employeeStoreCall");
  }
  
  public void setEmployeeStoreCall(final String employeeStoreCall) {
    setPropertyValue("employeeStoreCall", employeeStoreCall);
  }
  
  public String getServicelevel() {
    return (String) getPropertyValue("serviceLevel");
  }
  
  public void setCmosTrackingNumber(final String cmosTrackingNumber) {
    setPropertyValue("cmosTrackingNumber", cmosTrackingNumber);
  }
  
  public String getCmosTrackingNumber() {
    return (String) getPropertyValue("cmosTrackingNumber");
  }
  
  public void setGiftWrap(final RepositoryItem giftWrap) {
    setPropertyValue("giftWrap", giftWrap);
  }
  
  public RepositoryItem getGiftWrap() {
    return (RepositoryItem) getPropertyValue("giftWrap");
  }
  
  /**
   * The purpose of this method is to get the gift wrap code.
   * 
   * @return the gift wrap code
   */
  public String getGiftWrapCode() {
    RepositoryItem giftRepository = this.getGiftWrap();
    if (null != giftRepository) {
      return (String) getGiftWrap().getPropertyValue("id");
    }
    return EMPTY_STRING;
  }

  public void setCategoryId(final String categoryId) {
    setPropertyValue("categoryId", categoryId);
  }
  
  public String getCategoryId() {
    return (String) getPropertyValue("categoryId");
  }
  
  public void setParentheticalPromo(final boolean in) {
    setPropertyValue("parentheticalPromo", new Boolean(in));
  }
  
  public boolean getParentheticalPromo() {
    final Object o = getPropertyValue("parentheticalPromo");
    if (o == null) {
      return false;
    }
    return ((Boolean) o).booleanValue();
  }
  
  public void setQuickOrder(final boolean quickOrder) {
    setPropertyValue("quickOrder", new Boolean(quickOrder));
  }
  
  public boolean getQuickOrder() {
    return ((Boolean) getPropertyValue("quickOrder")).booleanValue();
  }
  
  public void setPerishable(final boolean perishable) {
    setPropertyValue("perishable", new Boolean(perishable));
  }
  
  // The original item in rare cases returns null for the perishableFlag, lookup added for this
  // case
  public boolean getPerishable() {
    Boolean isPerishable = (Boolean) getPropertyValue("perishable");
    if (isPerishable == null) {
      final String cmossku = getCmosSKUId();
      try {
        final Repository productRepository = (Repository) Nucleus.getGlobalNucleus().resolveName("/atg/commerce/catalog/ProductCatalog");
        if (log.isLoggingDebug()) { 
        	log.logDebug("WARNING*: NMCI.getPerishable() (perishableFlag = null, Attempting Lookup) cmossku:perishable[" + cmossku + ":" + isPerishable + "]");
        }
        final RepositoryView view = productRepository.getView("sku");
        final RqlStatement statement = RqlStatement.parseRqlStatement("(cmosSKU = ?0 )");
        final Object params[] = new Object[1];
        params[0] = cmossku;
        final RepositoryItem[] items = statement.executeQuery(view, params);
        final RepositoryItem ri = items[0];
        if (params.length > 0) {
          isPerishable = (Boolean) ri.getPropertyValue("flgPerishable");
          if (log.isLoggingDebug()) {
        	  log.logDebug("INFO****: NMCI.getPerishable() (Lookup Success) cmossku:perishable[" + cmossku + ":" + isPerishable + "]");
          }
        } else {
        	if (log.isLoggingDebug()) {
        		log.logDebug("ERROR***: NMCI.getPerishable() (Lookup Failure) cmossku:perishable[" + cmossku + ":" + isPerishable + "]");
        	}
        }
      } catch (final Exception e) {
    	  if (log.isLoggingError()) {
    		  log.logError("ERROR***: NMCI.getPerishable() (Lookup Error) cmossku:perishable[" + cmossku + ":" + isPerishable + "]");
    	  }
        isPerishable = new Boolean(false);
      }
    }
    return isPerishable.booleanValue();
  }
  
  public void setGiftNote(final int giftNote) {
    setPropertyValue("giftNote", new Integer(giftNote));
  }
  
  public int getGiftNote() {
    return ((Integer) getPropertyValue("giftNote")).intValue();
  }
  
  public void setNoteLine1(final String noteLine1) {
    setPropertyValue("noteLine1", noteLine1);
  }
  
  public String getNoteLine1() {
    return (String) getPropertyValue("noteLine1");
  }
  
  public void setNoteLine2(final String noteLine2) {
    setPropertyValue("noteLine2", noteLine2);
  }
  
  public String getNoteLine2() {
    return (String) getPropertyValue("noteLine2");
  }
  
  public void setNoteLine3(final String noteLine3) {
    setPropertyValue("noteLine3", noteLine3);
  }
  
  public String getNoteLine3() {
    return (String) getPropertyValue("noteLine3");
  }
  
  public void setNoteLine4(final String noteLine4) {
    setPropertyValue("noteLine4", noteLine4);
  }
  
  public String getNoteLine4() {
    return (String) getPropertyValue("noteLine4");
  }
  
  public void setNoteLine5(final String noteLine5) {
    setPropertyValue("noteLine5", noteLine5);
  }
  
  public String getNoteLine5() {
    return (String) getPropertyValue("noteLine5");
  }
  
  public boolean getStoreFulfillFlag() {
    Boolean isStoreFulfillFlag = new Boolean(false);
    final String cmossku = getCmosSKUId();
    try {
      final Repository productRepository = (Repository) Nucleus.getGlobalNucleus().resolveName("/atg/commerce/catalog/ProductCatalog");
      final RepositoryView view = productRepository.getView("sku");
      final RqlStatement statement = RqlStatement.parseRqlStatement("(cmosSKU = ?0 )");
      final Object params[] = new Object[1];
      params[0] = cmossku;
      final RepositoryItem[] items = statement.executeQuery(view, params);
      final RepositoryItem ri = items[0];
      if (params.length > 0) {
        isStoreFulfillFlag = (Boolean) ri.getPropertyValue("storeFulfillFlag");
        // System.out.println
        // ("INFO****: NMCI.getStoreFulfillFlag() (Lookup Success) cmossku:storeFulfillFlag["
        // + cmossku + ":" + isStoreFulfillFlag + "]");
        if (isStoreFulfillFlag == null) {
          isStoreFulfillFlag = new Boolean(false);
        }
      } else {
    	  if (log.isLoggingInfo()) {
    		  log.logInfo("NMCI.getStoreFulfillFlag() (Lookup Failure) cmossku:storeFulfillFlag[" + cmossku + ":" + isStoreFulfillFlag + "]");
    	  }
      }
    } catch (final Exception e) {
    	if (log.isLoggingError()) { 
    		log.logError("ERROR***: NMCI.getStoreFulfillFlag() (Lookup Error) cmossku:storeFulfillFlag[" + cmossku + ":" + isStoreFulfillFlag + "]");
    	}
      isStoreFulfillFlag = new Boolean(false);
    }
    return isStoreFulfillFlag.booleanValue();
    
  }
  
  public String getSendCmosPromoCode() {
    return (String) getPropertyValue("sendCmosPromoCode");
  }
  
  public void setSendCmosPromoCode(String sendCmosPromoCode) {
    if ((sendCmosPromoCode != null) && !sendCmosPromoCode.trim().equals("")) {
      sendCmosPromoCode = sendCmosPromoCode.trim().toUpperCase();
      String existingSendCmosPromoCode = getSendCmosPromoCode();
      existingSendCmosPromoCode = StringUtilities.addValueToDelimitedString(sendCmosPromoCode, existingSendCmosPromoCode, ",", 256);
      setPropertyValue("sendCmosPromoCode", existingSendCmosPromoCode);
    }
  }
  
  public void setRemoveSendCmosPromoCode(String sendCmosPromoCode) {
    if ((sendCmosPromoCode != null) && !sendCmosPromoCode.trim().equals("")) {
      sendCmosPromoCode = sendCmosPromoCode.trim().toUpperCase();
      String existingsendCmosPromoCode = getSendCmosPromoCode();
      existingsendCmosPromoCode = StringUtilities.removeValueFromDelimitedString(sendCmosPromoCode, existingsendCmosPromoCode, ",", 256);
      setPropertyValue("sendCmosPromoCode", existingsendCmosPromoCode);
    }
  }
  
  public List<String> getSendCmosPromoKeyList() {
    final String keys = getSendCmosPromoKey();
    return ListMaker.makeList(keys, ",");
  }
  
  public String getSendCmosPromoKey() {
    return (String) getPropertyValue("sendCmosPromoKey");
  }
  
  public void setSendCmosPromoKey(String sendCmosPromoKey) {
    if ((sendCmosPromoKey != null) && !sendCmosPromoKey.trim().equals("")) {
      sendCmosPromoKey = sendCmosPromoKey.trim().toUpperCase();
      String existingSendCmosPromoKey = getSendCmosPromoKey();
      existingSendCmosPromoKey = StringUtilities.addValueToDelimitedString(sendCmosPromoKey, existingSendCmosPromoKey, ",", 256);
      setPropertyValue("sendCmosPromoKey", existingSendCmosPromoKey);
    }
  }
  
  public void setRemoveSendCmosPromoKey(String sendCmosPromoKey) {
    if ((sendCmosPromoKey != null) && !sendCmosPromoKey.trim().equals("")) {
      sendCmosPromoKey = sendCmosPromoKey.trim().toUpperCase();
      String existingsendCmosPromoKey = getSendCmosPromoKey();
      existingsendCmosPromoKey = StringUtilities.removeValueFromDelimitedString(sendCmosPromoKey, existingsendCmosPromoKey, ",", 256);
      setPropertyValue("sendCmosPromoKey", existingsendCmosPromoKey);
    }
  }
  
  /**
   * Gets the depiction code from a cmosItemCode.
   *
   * @param cmosItemCode
   * @return The item's depiction code (the first character). If the item does not have a depicition code, a space is returned.
   */
  public char getDepictionCode() {
    char depictionCode = getCmosItemCode().charAt(0);
    if (Character.isDigit(depictionCode)) {
      depictionCode = ' ';
    }
    return depictionCode;
  }
  
  /**
   * Should the products in this commerce item be gift wrapped in one box or all in seperate boxes?
   *
   * @return giftWrapSeperately
   */
  public boolean isGiftWrapSeparately() {
    final Object isGiftWrapSeparatelyObject = getPropertyValue("giftWrapSeparately");
    if (isGiftWrapSeparatelyObject == null) {
      return false;
    }
    return ((Boolean) getPropertyValue("giftWrapSeparately")).booleanValue();
  }
  
  /**
   * Should the products in this commerce item be gift wrapped in one box or all in seperate boxes?
   *
   * @param giftWrapSeperately
   */
  public void setGiftWrapSeparately(final boolean giftWrapSeparately) {
    setPropertyValue("giftWrapSeparately", new Boolean(giftWrapSeparately));
  }
  
  /**
   * The total price for the gift wrapping.
   *
   * @return giftWrapPrice
   */
  public double getGiftWrapPrice() {
    if (getFlgBngl()) {
      return 0.0d;
    }
    
    final Object giftWrapPriceObject = getPropertyValue("giftWrapPrice");
    if (giftWrapPriceObject == null) {
      return 0.0d;
    }
    return ((Double) getPropertyValue("giftWrapPrice")).doubleValue();
  }
  
  /**
   * The total price for the gift wrapping.
   *
   * @param giftWrapPrice
   */
  public void setGiftWrapPrice(final double giftWrapPrice) {
    setPropertyValue("giftWrapPrice", new Double(giftWrapPrice));
  }
  
  /**
   * Clones the current object, but the cloned object has a unique commerce id.
   *
   * @param CommerceItemManager
   *          - an instace of the commerce item manager. Needed in order to create a new valid commerce item with a unique id
   * @return a new NMCommerceItem that is an exact copy of the current instance except for the ID, returns null
   */
  @Override
  public Object clone() {
    long qty = getQuantity();
    if (0 == qty) {
      qty = 1;
    }
    
    NMCommerceItem newItem = null;
    try {
      final CommerceItemManager manager = OrderManager.getOrderManager().getCommerceItemManager();
      newItem = (NMCommerceItem) manager.createCommerceItem(getCatalogRefId(), getAuxiliaryData().getProductId(), qty);
      copyPropertiesInto(newItem);
    } catch (final CommerceException ce) {
      ce.printStackTrace();
    }
    return newItem;
    
  }
  
  /**
   * Copies the NM specific properties of this object into the passed in object
   *
   * @param destination
   *          - The NMCommerce item to copy the properties into
   */
  public void copyPropertiesInto(final NMCommerceItem destination) {
    destination.setCatalogId(getCatalogId());
    destination.setCatalogKey(getCatalogKey());
    destination.setCatalogRefId(getCatalogRefId());
    destination.setCategoryId(getCategoryId());
    destination.setDeptCode(getDeptCode());
    destination.setSourceCode(getSourceCode());
    destination.setWebDesignerName(getWebDesignerName());
    destination.setCmosCatalogId(getCmosCatalogId());
    destination.setCmosDetailId(getCmosDetailId());
    destination.setCmosItemCode(getCmosItemCode());
    destination.setCmosProdName(getCmosProdName());
    destination.setCmosSKUId(getCmosSKUId());
    destination.setCmosTrackingNumber(getCmosTrackingNumber());
    destination.setCodeSetType(getCodeSetType());
    destination.setCommerceItemClassType(getCommerceItemClassType());
    destination.setCoreMetricsCategory(getCoreMetricsCategory());
    destination.setReporting_code(getReporting_code());
    destination.setGiftWrap(getGiftWrap());
    destination.setGiftWrapSeparately(isGiftWrapSeparately());
    destination.setGiftNote(getGiftNote());
    destination.setNoteLine1(getNoteLine1());
    destination.setNoteLine2(getNoteLine2());
    destination.setNoteLine3(getNoteLine3());
    destination.setNoteLine4(getNoteLine4());
    destination.setNoteLine5(getNoteLine5());
    destination.setPerishable(getPerishable());
    // destination.setPriceInfo(getPriceInfo()); removed to prevent the same object from being
    // used in cloning
    // destination.setPriceInfoRepositoryItem(getPriceInfoRepositoryItem());
    destination.setProdVariation1(getProdVariation1());
    destination.setProdVariation2(getProdVariation2());
    destination.setProdVariation3(getProdVariation3());
    destination.setPromoName(getPromoName());
    destination.setPromoTimeStamp(getPromoTimeStamp());
    destination.setPromoType(getPromoType());
    destination.setRelatedProduct(getRelatedProduct());
    destination.setQuantity(getQuantity());
    destination.setQuickOrder(getQuickOrder());
    destination.setFlgBngl(getFlgBngl());
    destination.setReqDeliveryDate(getReqDeliveryDate());
    destination.setCommerceItemDate(getCommerceItemDate());
    destination.setServicelevel(getServicelevel());
    destination.setShipMethod(getShipMethod());
    destination.setSpecialInstCodes(getSpecialInstCodes());
    destination.setStatusCode(getStatusCode());
    destination.setSuiteId(getSuiteId());
    destination.setTransientAvailableDate(getTransientAvailableDate());
    destination.setAutoGiftwrapFlg(getAutoGiftwrapFlg());
    destination.setTransientStatus(getTransientStatus());
    destination.setRegistryId(getRegistryId());
    destination.setParentheticalPromo(getParentheticalPromo());
    destination.setSendCmosPromoCode(getSendCmosPromoCode());
    destination.setSendCmosPromoKey(getSendCmosPromoKey());
    destination.setDropshipInHouse(getDropshipInHouse());
    destination.setMiscFlags(getMiscFlags());
    destination.setPickupStoreNo(getPickupStoreNo());
    destination.setSelectedInterval(getSelectedInterval());
    destination.setFulfillmentFacility(getFulfillmentFacility());
    destination.setAlipayEligibleFlg(getAlipayEligibleFlg());
    destination.setDynamicImageUrl(getDynamicImageUrl());
    // added for NMOBLD-2184
    destination.setOptionChoices(getOptionChoices());
    destination.setConfigurationKey(getConfigurationKey());
    destination.setConfigSetPrice(getConfigSetPrice());
    // added for EDD
    destination.setCmosEstimatedDeliveryDate(getCmosEstimatedDeliveryDate()); 
  }
  
  /**
   * There are a lot of business rules that require the commerce items gift wrap optiions to reset to their default state. Instead of modifying the properties everywhere it makes sense to put that
   * here so that if the options change they only need to be changed in one place.
   */
  public void resetGiftWrapOptions() {
    
    setGiftWrap(null);
    setGiftWrapSeparately(false);
    setGiftNote(NO_GIFT_NOTE);
    setNoteLine1("");
    setNoteLine2("");
    setNoteLine3("");
    setNoteLine4("");
    setNoteLine5("");
  }
  
  /**
   * The default gift wrap requirements for the Neimans GR items are different from other items. Therefore, this function has been created to set the default gift wrap options for such items. This
   * method is called only when an item is a Neimans GR item.
   */
  public void resetGiftWrapOptionsForNeimansGR(final RepositoryItem ri) {
    setGiftWrap(ri);
    setGiftWrapSeparately(false);
    setGiftNote(CUSTOM_GIFT_NOTE);
    setNoteLine1("Best Wishes!");
    setNoteLine2("");
    setNoteLine3("");
    setNoteLine4("");
    setNoteLine5("");
  }
  
  @Override
  public ItemPriceInfo getPriceInfo() {
    if (super.getPriceInfo() != null) {
      return super.getPriceInfo();
    } else {
      // CommonComponentHelper.getLogger().error("CommerceItemImpl.getPriceInfo() is null.  Using empty PriceInfo object.");
      return new NMItemPriceInfo();
    }
  }
  
  /*
   * Returns the media id to be used as a background to the /sipreview servlet Large image is rendered during checkout as a popup image
   */
  public String getSwatchMediaLargeId() {
    String swatchMediaLargeId = null;
    final AuxiliaryData auxData = getAuxiliaryData();
    if (auxData != null) {
      final String productId = auxData.getProductId();
      final String suiteId = getSuiteId();
      final ProdSkuUtil prodSkuUtil = CommonComponentHelper.getProdSkuUtil();
      swatchMediaLargeId = prodSkuUtil.getSwatchMediaLargeId(productId, suiteId);
    }
    
    return swatchMediaLargeId != null ? swatchMediaLargeId : "";
  }
  
  /*
   * Returns the media id to be used as a background to the /sipreview servlet Small image is rendered during checkout process
   */
  public String getSwatchMediaSmallId() {
    String swatchMediaSmallId = null;
    final AuxiliaryData auxData = getAuxiliaryData();
    if (auxData != null) {
      final String productId = auxData.getProductId();
      final String suiteId = getSuiteId();
      final ProdSkuUtil prodSkuUtil = CommonComponentHelper.getProdSkuUtil();
      swatchMediaSmallId = prodSkuUtil.getSwatchMediaSmallId(productId, suiteId);
    }
    
    return swatchMediaSmallId != null ? swatchMediaSmallId : "";
  }
  
  /*
   * Returns the hex color of the selected Sku background to the /sipreview servlet
   */
  public String getSwatchHexColor() {
    String swatchHexColor = null;
    final String skuId = getCatalogRefId();
    if (skuId != null) {
      final ProdSkuUtil prodSkuUtil = CommonComponentHelper.getProdSkuUtil();
      swatchHexColor = prodSkuUtil.getSwatchHexColor(skuId);
    }
    
    return swatchHexColor != null ? swatchHexColor : "";
  }
  
  public boolean getProductHasSwatchMedia() {
    boolean returnValue = false;
    
    final AuxiliaryData auxData = getAuxiliaryData();
    if (auxData != null) {
      final String productId = auxData.getProductId();
      final String suiteId = getSuiteId();
      final String skuId = getCatalogRefId();
      final ProdSkuUtil prodSkuUtil = CommonComponentHelper.getProdSkuUtil();
      returnValue = prodSkuUtil.getProductHasSwatchMedia(productId, suiteId, skuId);
    }
    
    return returnValue;
  }
  
  public boolean hasMiscFlag(final String flagName) {
    boolean returnValue = false;
    
    // get the flags as a list to see if the value is already
    // in the list. We can't do a string indexOf because flagName may
    // be a substring of an existing flag name.
    final List<String> miscFlagList = getMiscFlagList();
    
    if ((miscFlagList != null) && (miscFlagList.size() > 0)) {
      returnValue = miscFlagList.contains(flagName);
    }
    
    return returnValue;
  }
  
  public void addMiscFlag(final String flagName) {
    if (flagName != null) {
      if (!hasMiscFlag(flagName)) {
        String miscFlags = getMiscFlags();
        
        if (miscFlags != null) {
          miscFlags += miscFlags + "," + flagName;
        } else {
          miscFlags = flagName;
        }
        
        setMiscFlags(miscFlags);
      }
    }
  }
  
  public List<String> getMiscFlagList() {
    final String miscFlags = getMiscFlags();
    return ListMaker.makeList(miscFlags, ",");
  }
  
  public void setMiscFlags(final String value) {
    setPropertyValue("miscFlags", value);
  }
  
  public String getMiscFlags() {
    return (String) getPropertyValue("miscFlags");
  }
  
  @Override
  public double getCurrentItemPrice() {
    
    // System.out.println("----------------------------------");
    double currentPrice = getRawTotalPrice();
    final Map<String, Markdown> promotionsApplied = ((NMItemPriceInfo) getPriceInfo()).getPromotionsApplied();
    final double promotionalPrice = ((NMItemPriceInfo) getPriceInfo()).getPromotionalPrice();
    
    final Set<String> keys = promotionsApplied.keySet();
    final Iterator<String> i = keys.iterator();
    while (i.hasNext()) {
      final Markdown markdown = promotionsApplied.get(i.next());
      // System.out.println("   -- markdown.getDollarDiscount() " +
      // markdown.getDollarDiscount());
      currentPrice -= markdown.getDollarDiscount();
    }
    if (promotionalPrice > 0.0) {
      currentPrice = promotionalPrice;
    }
    // EDO changes
    final NMItemPriceInfo PriceInfo = (NMItemPriceInfo) getPriceInfo();
    final double employeeDiscount = PriceInfo.getEmployeeDiscountAmount();
    final double employeeExtraDiscount = PriceInfo.getEmployeeExtraDiscountAmount();
    final double totalEmployeeDiscount = employeeDiscount + employeeExtraDiscount;
    if (totalEmployeeDiscount > 0.0) {
      currentPrice -= totalEmployeeDiscount;
    }
    // EDO-89 Story
    // System.out.println("----------------------------------");
    return currentPrice;
  }
  
  public Map<String, Markdown> getItemMarkdowns() {
    return ((NMItemPriceInfo) getPriceInfo()).getPromotionsApplied();
  }
  
  @Override
  public double getRawTotalPrice() {
    return getPriceInfo().getRawTotalPrice();
  }
  
  @Override
  public String getProductId() {
    return getAuxiliaryData().getProductId();
  }
  
  @Override
  public String getDepartment() {
    return getDeptCode();
  }
  
  @Override
  public String getClassCode() {
    // return getClassCode();
    
    log.error("!!!!!!!! Returning error from NMCommerceItem.getClassCode().  This code originally resulted in an infinite recursive loop called from rules based promotions. "
            + "We could not determine what this data was supposed to be calling so it was errored out for now.  Calling the original would have brought down the server. "
            + " the location from which was this was called was in ItemClassCodeRule and ItemMultiDeptRule. Please contact NMO Development. !!!!!!!!!!!!");
    
    return "error";
  }
  
  @Override
  public boolean isInStock() {
    try {
      final RepositoryItem skuItem = CommonComponentHelper.getProductRepository().getItem(getCatalogRefId(), "sku");
      if (skuItem != null) {
        final Boolean flgDropship = (Boolean) skuItem.getPropertyValue("flgDropship");
        final Boolean inStock = (Boolean) skuItem.getPropertyValue("inStock");
        if (((inStock != null) && inStock.booleanValue()) || ((flgDropship != null) && flgDropship.booleanValue())) {
          return true;
        }
      }
    } catch (final Exception e) {}
    return false;
  }
  
  public String getRealExpectedShipDate() {
    final ProdSkuUtil prodSkuUtil = CommonComponentHelper.getProdSkuUtil();
    final boolean isOptionalPlain = StringUtilities.isEmpty(getCodeSetType()) && (getSpecialInstructionFlag().equalsIgnoreCase("O") || getSpecialInstructionFlag().equalsIgnoreCase("F"));
    return prodSkuUtil.getRealExpectedShipDate(getProduct().getDataSource(), getSku().getDataSource(), isOptionalPlain);
  }
  
  /**
   * Returns the store retail price
   *
   * @return the store retail price value
   */
  public double getStoreRetailPrice() {
    double storeRetailPrice = 0.0d;
    
    try {
      final RepositoryItem skuItem = CommonComponentHelper.getProductRepository().getItem(getCatalogRefId(), "sku");
      if (skuItem != null) {
        if (skuItem.getPropertyValue("storeRetailPrice") != null) {
          storeRetailPrice = ((Double) skuItem.getPropertyValue("storeRetailPrice")).doubleValue();
        }
      }
    } catch (final Exception e) {
      e.printStackTrace();
    }
    
    return storeRetailPrice;
  }
  
  @Override
  public boolean isSaleItem() {
    try {
      final RepositoryItem productItem = CommonComponentHelper.getProductRepository().getItem(getProductId(), "product");
      if ((productItem != null) && (productItem.getPropertyValue("flgPricingAdornments") != null)) {
        return ((Boolean) productItem.getPropertyValue("flgPricingAdornments")).booleanValue();
      }
    } catch (final Exception e) {}
    
    return false;
  }
  
  @Override
  public int getItemQuantity() {
    final int quantity = new Long(getQuantity()).intValue();
    return quantity;
  }
  
  public double getTotalAmountPerUnit() {
    double totalAmountPerUnit = getPriceInfo().getAmount() / (getQuantity() * 1.0);
    totalAmountPerUnit = Math.floor((totalAmountPerUnit * 100.0) + 0.5) / 100.0;
    return totalAmountPerUnit;
  }
  
  public double getDiscountPerUnit() {
    final NMItemPriceInfo itemInfo = (NMItemPriceInfo) getPriceInfo();
    double discountPerUnit = itemInfo.getDiscountAmount() / (getQuantity() * 1.0);
    discountPerUnit = Math.floor((discountPerUnit * 100.0) + 0.5) / 100.0;
    return discountPerUnit;
    
  }
  
  /**
   * this flag is set to true when the commerce item is a transfer from a save for later list
   */
  private boolean saveForLaterTransfer;
  
  /**
   * @return the saveForLaterTransfer
   */
  public boolean isSaveForLaterTransfer() {
    return saveForLaterTransfer;
  }
  
  /**
   * @param saveForLaterTransfer
   *          the saveForLaterTransfer to set
   */
  public void setSaveForLaterTransfer(final boolean saveForLaterTransfer) {
    this.saveForLaterTransfer = saveForLaterTransfer;
  }
  
  @Override
  public Set<String> getPromoKeys() {
    Set<String> promoKeys = null;
    
    if (getAuxiliaryData() != null) {
      try {
        final RepositoryItem productItem = CommonComponentHelper.getProductRepository().getItem(getAuxiliaryData().getProductId(), "product");
        if (productItem != null) {
          @SuppressWarnings("unchecked")
          final Set<String> keys = (Set<String>) productItem.getPropertyValue("promoKeys");
          promoKeys = keys;
        }
      } catch (final Exception e) {
        e.printStackTrace();
      }
    }
    return promoKeys;
  }
  
  public void setFlgBngl(final boolean flagBNGL) {
    setPropertyValue("flagBNGL", new Boolean(flagBNGL));
  }
  
  public boolean getFlgBngl() {
    Boolean isBngl = (Boolean) getPropertyValue("flagBNGL");
    if (isBngl == null) {
      isBngl = new Boolean(false);
    }
    return isBngl.booleanValue();
  }
  
  public void setSelectedInterval(final String selectedInterval) {
    setPropertyValue("selectedInterval", selectedInterval);
  }
  
  public String getSelectedInterval() {
    final String selectedInterval = (String) getPropertyValue("selectedInterval");
    
    return selectedInterval;
  }
  
  @SuppressWarnings("deprecation")
  public static Map<String, String> isPickupinStoreNumber(final NMOrderImpl order) {
    final GenericLogger log = CommonComponentHelper.getLogger();
    
    final Map<String, String> hmPickUPatStore = new HashMap<String, String>();
    
    try {
      final String strViewName = "hardgoodShippingGroup";
      
      if (order != null) {
        if (order instanceof OrderImpl) {
          
          @SuppressWarnings("unchecked")
          final List<ShippingGroupCommerceItemRelationship> items = OrderManager.getOrderManager().getAllShippingGroupRelationships(order);
          final Iterator<ShippingGroupCommerceItemRelationship> iter = items.iterator();
          
          // Examine each commerceItem relationship
          while (iter.hasNext()) {
            
            // Getting the ShippingGroupCommerceItemRelationship
            
            final ShippingGroupCommerceItemRelationship sgCIRel = iter.next();
            
            // **************************************************
            
            // Get the Commerceitems
            final NMCommerceItem theCI = (NMCommerceItem) sgCIRel.getCommerceItem();
            
            // Get the Shipping group
            final ShippingGroup thisSG = sgCIRel.getShippingGroup();
            
            // Get the HardgoodShippingGroup
            final HardgoodShippingGroup hardgoodShipGroup = (HardgoodShippingGroup) thisSG;
            
            // **************************************************
            String strIndicatorFlg = "Y";
            
            log.debug("NMCommerceItem.isPickupinStore()" + theCI.getId() + "-" + theCI.getProductId() + "-" + theCI.getCmosProdName() + "-" + hardgoodShipGroup.getId());
            
            final MutableRepository orderRepos = (MutableRepository) RepositoryUtils.getInstance().getOrderRepository();
            
            final MutableRepositoryItem ri = (MutableRepositoryItem) orderRepos.getItem(hardgoodShipGroup.getId(), strViewName);
            
            final String strStoreNum = (String) ri.getPropertyValue("strStoreNum");
            
            log.debug("NMCommerceItem.isPickupinStore()" + theCI.getId() + "-" + theCI.getProductId() + "-" + theCI.getCmosProdName() + "-" + hardgoodShipGroup.getId() + "-" + strStoreNum);
            
            if ((strStoreNum != null) && (strStoreNum.length() > 0)) {
              strIndicatorFlg = strStoreNum;
            } else {
              strIndicatorFlg = "N";
            }
            
            hmPickUPatStore.put(theCI.getId(), strIndicatorFlg);
            
          }// end while iter
        }// end order instance of orderImpl
      }// end order null
      else {
    	  if (log.isLoggingDebug()) {
    		  log.logDebug("Order to be modified is null");
    	  }
      }
      
    } catch (final Exception e) {
    	if (log.isLoggingDebug()) {
    		log.logError("Exception: NMCommerceItem.java in isPickupinStoreNumber(): ", e);
    	}
    }
    return hmPickUPatStore;
  }
  
  /*
   * This following method is to check based on order/ci level
   */
  @SuppressWarnings("deprecation")
  public static Map<String, String> isPickupinStore(final NMOrderImpl order) {
    final GenericLogger log = CommonComponentHelper.getLogger();
    
    final Map<String, String> hmPickUPatStore = new HashMap<String, String>();
    
    try {
      final String strViewName = "hardgoodShippingGroup";
      
      if (order != null) {
        if (order instanceof OrderImpl) {
          
          @SuppressWarnings("unchecked")
          final List<ShippingGroupCommerceItemRelationship> items = OrderManager.getOrderManager().getAllShippingGroupRelationships(order);
          final Iterator<ShippingGroupCommerceItemRelationship> iter = items.iterator();
          
          // Examine each commerceItem relationship
          while (iter.hasNext()) {
            
            // Getting the ShippingGroupCommerceItemRelationship
            
            final ShippingGroupCommerceItemRelationship sgCIRel = iter.next();
            
            // **************************************************
            
            // Get the Commerceitems
            final NMCommerceItem theCI = (NMCommerceItem) sgCIRel.getCommerceItem();
            
            // Get the Shipping group
            final ShippingGroup thisSG = sgCIRel.getShippingGroup();
            
            // Get the HardgoodShippingGroup
            final HardgoodShippingGroup hardgoodShipGroup = (HardgoodShippingGroup) thisSG;
            
            // **************************************************
            String strIndicatorFlg = "Y";
            
            log.debug("NMCommerceItem.isPickupinStore()" + theCI.getId() + "-" + theCI.getProductId() + "-" + theCI.getCmosProdName() + "-" + hardgoodShipGroup.getId());
            
            final MutableRepository orderRepos = (MutableRepository) RepositoryUtils.getInstance().getOrderRepository();
            
            final MutableRepositoryItem ri = (MutableRepositoryItem) orderRepos.getItem(hardgoodShipGroup.getId(), strViewName);
            
            final String strStoreNum = (String) ri.getPropertyValue("strStoreNum");
            
            log.debug("NMCommerceItem.isPickupinStore()" + theCI.getId() + "-" + theCI.getProductId() + "-" + theCI.getCmosProdName() + "-" + hardgoodShipGroup.getId() + "-" + strStoreNum);
            
            if ((strStoreNum != null) && (strStoreNum.length() > 0)) {
              strIndicatorFlg = "Y";
            } else {
              strIndicatorFlg = "N";
            }
            
            hmPickUPatStore.put(theCI.getId(), strIndicatorFlg);
            
          }// end while iter
        }// end order instance of orderImpl
      }// end order null
      else {
    	  if (log.isLoggingDebug()) {
    		  log.logDebug("Order to be modified is null");
    	  }
      }
      
    } catch (final Exception e) {
      if (log.isLoggingError()) {
    	  log.logError("Exception: NMCommerceItem.java in isPickupinStore(): " , e);
      }
    }
    return hmPickUPatStore;
  }
  
  @SuppressWarnings("deprecation")
  public static Map<String, String> isPickupinStore(final NMOrderImpl order, final String str) {
    final GenericLogger log = CommonComponentHelper.getLogger();
    
    final Map<String, String> hmPickUPatStore = new HashMap<String, String>();
    
    try {
      final String strViewName = "hardgoodShippingGroup";
      
      if (order != null) {
        if (order instanceof OrderImpl) {
          NMPricingTools pricingTools = CommonComponentHelper.getPricingTools();
          @SuppressWarnings("unchecked")
          final List<ShippingGroupCommerceItemRelationship> items = OrderManager.getOrderManager().getAllShippingGroupRelationships(order);
          final Iterator<ShippingGroupCommerceItemRelationship> iter = items.iterator();
          
          // Examine each commerceItem relationship
          while (iter.hasNext()) {
            
            // Getting the ShippingGroupCommerceItemRelationship
            
            final ShippingGroupCommerceItemRelationship sgCIRel = iter.next();
            
            // **************************************************
            
            // Get the Commerceitems
            final NMCommerceItem theCI = (NMCommerceItem) sgCIRel.getCommerceItem();
            
            // Get the Shipping group
            final ShippingGroup thisSG = sgCIRel.getShippingGroup();
            
            // Get the HardgoodShippingGroup
            final HardgoodShippingGroup hardgoodShipGroup = (HardgoodShippingGroup) thisSG;
            
            // **************************************************
            String strIndicatorFlg = "Y";
            log.debug("NMCommerceItem.isPickupinStore()(Itemlevel-Result) : " + theCI.getId() + "-" + theCI.getProductId() + "-" + theCI.getCmosProdName() + "-" + hardgoodShipGroup.getId());
            
            final MutableRepository orderRepos = (MutableRepository) RepositoryUtils.getInstance().getOrderRepository();
            
            final MutableRepositoryItem ri = (MutableRepositoryItem) orderRepos.getItem(hardgoodShipGroup.getId(), strViewName);
            
            final String strStoreNum = (String) ri.getPropertyValue("strStoreNum");
            
            log.debug("NMCommerceItem.isPickupinStore()(Itemlevel-Result) : " + theCI.getId() + "-" + theCI.getProductId() + "-" + theCI.getCmosProdName() + "-" + hardgoodShipGroup.getId() + "-"
                    + strStoreNum);
            
            if ((strStoreNum != null) && (strStoreNum.length() > 0)) {
              strIndicatorFlg = "Y";
              strIndicatorFlg = pricingTools.fetchPickupStoreName(strStoreNum);
            } else {
              strIndicatorFlg = "N";
            }
            
            hmPickUPatStore.put(theCI.getId(), strIndicatorFlg);
            
          }// end while iter
        }// end order instance of orderImpl
      }// end order null
      else {
        if (log.isLoggingDebug()) { 
        	log.logDebug("Order to be modified is null");
        }
      }
      
    } catch (final Exception e) {
    	if (log.isLoggingError()) {
    		log.logError("Exception: NMCommerceItem.java in isPickupinStore(): ", e);
    	}
    }
    return hmPickUPatStore;
  }
  
  /**
   * Checks if this is a pick up in a store item
   *
   * @author nmve1
   * @return boolean
   */
  public boolean isPickupInStore() {
    
    @SuppressWarnings("unchecked")
    final Iterator<ShippingGroupRelationship> iterSgr = getShippingGroupRelationships().iterator();
    
    final MutableRepository orderRepos = (MutableRepository) RepositoryUtils.getInstance().getOrderRepository();
    
    ShippingGroupRelationship sgr = null;
    String storeNo = null;
    boolean flg = false;
    
    try {
      while (iterSgr.hasNext()) {
        sgr = iterSgr.next();
        final MutableRepositoryItem item = (MutableRepositoryItem) orderRepos.getItem(sgr.getId(), "hardgoodShippingGroup");
        if (null != item) {
          storeNo = (String) item.getPropertyValue("strStoreNum");
          if ((null != storeNo) && (storeNo.trim().length() > 0)) {
            flg = true;
          }
        }
      }
    } catch (final Exception e) {
      System.err.println("NMCommerceItem.isPickupInStore() exception: " + e.getMessage());
      e.printStackTrace();
    }
    
    return flg;
  }
  
  /**
   * Checks if this item is shipping to a store
   */
  public boolean isShipToStore() {
    @SuppressWarnings("unchecked")
    final Iterator<ShippingGroupRelationship> iterSgr = getShippingGroupRelationships().iterator();
    
    ShippingGroupRelationship sgr = null;
    String shipToStoreNumber = null;
    boolean flg = false;
    
    try {
      while (iterSgr.hasNext()) {
        sgr = iterSgr.next();
        final HardgoodShippingGroup group = (HardgoodShippingGroup) sgr.getShippingGroup();
        if (null != group) {
          shipToStoreNumber = (String) group.getPropertyValue("shipToStoreNumber");
          if (StringUtilities.isNotEmpty(shipToStoreNumber)) {
            flg = true;
          }
        }
      }
    } catch (final Exception e) {
      System.err.println("NMCommerceItem.isPickupInStore() exception: " + e.getMessage());
      e.printStackTrace();
    }
    
    return flg;
  }
  
  /**
   * Checks if the item ships from store
   *
   * @author nmve1
   * @return boolean
   */
  public boolean isShipFromStore() {
    return getStoreFulfillFlag();
  }
  
  @Override
  public int compareTo(final Object o) {
    int comp = getCommerceItemDate().compareTo(((NMCommerceItem) o).getCommerceItemDate());
    if (getCommerceItemDate().equals(((NMCommerceItem) o).getCommerceItemDate())) {
      comp = getId().compareTo(((NMCommerceItem) o).getId());
    }
    
    return comp;
  }
  
  @Override
  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof NMCommerceItem)) {
      return false;
    }
    
    final NMCommerceItem otherItem = (NMCommerceItem) other;
    
    return getId() != null ? getId().equals(otherItem.getId()) : null;
  }
  
  @Override
  public int hashCode() {
    return getId().hashCode();
  }
  
  public String getQuantityPriceString() {
    final Double price = getQuantityPrice();
    return NumberFormat.getCurrencyInstance().format(price);
  }
  
  public double getQuantityPrice() {
    return getProduct().getPrice().doubleValue() * getQuantity();
  }
  
  public boolean isMonogramItem() {
    return CommonComponentHelper.getProdSkuUtil().isMonogramItem((RepositoryItem) getAuxiliaryData().getProductRef());
  }
  
  public boolean getIsMonogramItem() {
    return isMonogramItem();
  }
  
  public boolean getGiftWrapFlag() {
    return getGiftWrap() != null;
  }
  
  /**
   * The purpose of this method is to check if is virtual gift card item.
   * 
   * @return true, if is virtual gift card
   */
  public boolean isVirtualGiftCard() {
    return VIRTUAL_GIFT_CARD.equalsIgnoreCase(getCodeSetType());
  }
  
  /**
   * The purpose of this method is to check if is drop ship merch type eligible to display service levels.
   *
   * @return the checks if is drop ship merch type eligible to display service levels
   */
  public boolean isDropShipMerchTypeEligibleToDisplayServiceLevels() {
    return getProduct().getIsDropShipMerchTypeEligibleToDisplayServiceLevels();
  }
  
  /**
   *
   * Following code will set the property internationalPriceItem for the international product copy block for China Name of Method: setInternationalPriceItem
   */
  public void setInternationalPriceItem(final RepositoryItem internationalPriceItem) {
    setPropertyValue("internationalPriceItem", internationalPriceItem);
  }
  
  /**
   *
   * Following code will get the property internationalPriceItem for the international product copy block for China Name of Method: getInternationalPriceItem
   */
  public RepositoryItem getInternationalPriceItem() {
    return (RepositoryItem) getPropertyValue("internationalPriceItem");
  }
  
  public boolean getIsShipToStoreEligible() {
    return isShipToStoreEligible();
  }
  
  public boolean getIsICECommerceItem() {
    if (getCmosItemCode().toUpperCase().startsWith("P") || getCmosItemCode().toUpperCase().startsWith("J")) {
      return true;
    } else {
      return false;
    }
  }
  
  public void setDynamicImageUrl(String url) {
    if (!StringUtils.isBlank(url) && url.indexOf("http:") < 0) {
  		url = "http:" + url;
    }
    setPropertyValue("dynamicImageUrl", url);
  }
  
  public String getDynamicImageUrl() {
    return (String) getPropertyValue("dynamicImageUrl");
  }
  
  public boolean isShipToStoreEligible() {
    boolean isEligible = true;
    
    final Set<String> INELIGIBLE_MERCHANDISE_TYPES = new HashSet<String>(Arrays.asList(new String[] {"1" , "2" , "3" , "4" , "5" , "7"}));
    final String merchandiseType = getProduct().getMerchandiseType();
    final ProdSkuUtil prodSkuUtil = CommonComponentHelper.getProdSkuUtil();
    final String statusString = prodSkuUtil.getStatusString(getProduct().getDataSource(), getSku().getDataSource());
    boolean backOrder = false;
    if (StringUtilities.isNotEmpty(statusString) && statusString.equals(prodSkuUtil.getStatusBackorderString())) {
      backOrder = true;
    }
    
    if (getPerishable()) {
      isEligible = false;
    } else if ((merchandiseType != null) && merchandiseType.equalsIgnoreCase(GiftCardHolder.GIFT_CARD_MERCH_TYPE)) {
      isEligible = false;
    } else if ((merchandiseType != null) && INELIGIBLE_MERCHANDISE_TYPES.contains(merchandiseType)) {
      isEligible = false;
    } else if (getProduct().getFlgParenthetical() && (getProduct().getParentheticalCharge() > 0)) {
      isEligible = false;
    } else if ((getSelectedInterval() != null) && !getSelectedInterval().equals("") && !getSelectedInterval().equals("0")) {
      isEligible = false;
    } else if (getIsICECommerceItem()) {
      isEligible = false;
    } else if (getProduct().getIsPreOrder()) {
      isEligible = false;
    } else if (backOrder) {
      isEligible = false;
    }
    
    return isEligible;
  }
  
  /***
   * getter method for jsp to access store call eligibility
   *
   * @return store call eligibility
   */
  public boolean getIsStoreCallEligible() {
    return isStoreCallEligible();
  }
  
  /**
   * Method to determine the store call eligibility.
   *
   * @return store call eligibility
   * */
  public boolean isStoreCallEligible() {
    return NmoUtils.getBooleanValue(getPropertyValue("storeCallEligible"));
  }
  
  public void setStoreCallEligibile(final boolean storeCallEnabled) {
    setPropertyValue("storeCallEligible", storeCallEnabled);
  }
  
  public Boolean getSddEligible() {
    final Boolean isEligible = isShipToStoreEligible() && !isMonogramItem();
    return isEligible;
  }
  
  public String getActivePromoCodesEligible() {
    return (String) getPropertyValue(OrderRepositoryConstants.ACTIVE_PROMO_CODES_ELIGIBLE);
  }
  
  public void setActivePromoCodesEligible(final String eligiblePromoCode) {
    setPropertyValue(OrderRepositoryConstants.ACTIVE_PROMO_CODES_ELIGIBLE, getPromoCodes(eligiblePromoCode, getActivePromoCodesEligible()));
  }
  
  public String getActivePromoCodesInEligible() {
    return (String) getPropertyValue(OrderRepositoryConstants.ACTIVE_PROMO_CODES_INELIGIBLE);
  }
  
  public void setActivePromoCodesInEligible(final String inEligiblePromoCode) {
    setPropertyValue(OrderRepositoryConstants.ACTIVE_PROMO_CODES_INELIGIBLE, getPromoCodes(inEligiblePromoCode, getActivePromoCodesInEligible()));
    
  }
  
  /**
   * This method removes the trailing comma from ActivePromoCodesEligible to display in web
   *
   *
   */
  public String getActivePromoCodesEligibleText() {
    String activePromoCodesEligibleText = getActivePromoCodesEligible();
    if ((activePromoCodesEligibleText != null) && activePromoCodesEligibleText.endsWith(",")) {
      activePromoCodesEligibleText = activePromoCodesEligibleText.substring(0, activePromoCodesEligibleText.length() - 1);
    }
    return activePromoCodesEligibleText;
  }
  
  /**
   * This method removes the trailing comma from ActivePromoCodesInEligible to display in web
   *
   */
  public String getActivePromoCodesInEligibleText() {
    String activePromoCodesInEligibleText = getActivePromoCodesInEligible();
    if ((activePromoCodesInEligibleText != null) && activePromoCodesInEligibleText.endsWith(",")) {
      activePromoCodesInEligibleText = activePromoCodesInEligibleText.substring(0, activePromoCodesInEligibleText.length() - 1);
    }
    return activePromoCodesInEligibleText;
    
  }
  
  /**
   * Verifies the applied PromoCode is already there in the Active PromoCodes list of item and if not there ,then adds the promoCode and returns the ActivePromoCodes as comma separated strings
   *
   * @param promoCode
   * @param existingPromoCodes
   * @return
   */
  private String getPromoCodes(final String promoCode, String existingPromoCodes) {
    String promoCodes = null;
    if (!StringUtils.isEmpty(promoCode)) {
      if (StringUtils.isEmpty(existingPromoCodes)) {
        existingPromoCodes = "";
      }
      boolean foundIt = false;
      final StringTokenizer tempPromoCodes = new StringTokenizer(existingPromoCodes, ",");
      if (null != tempPromoCodes) {
        while (tempPromoCodes.hasMoreTokens()) {
          final String tempPromoCode = tempPromoCodes.nextToken();
          if (promoCode.equals(tempPromoCode)) {
            foundIt = true;
            break;
          }
          
        }
        
      }
      
      if (!foundIt) {
        final StringBuffer activePromoCodes = new StringBuffer();
        activePromoCodes.append(existingPromoCodes);
        activePromoCodes.append(promoCode);
        activePromoCodes.append(",");
        promoCodes = activePromoCodes.toString();
      } else {
        promoCodes = existingPromoCodes;
      }
      
    }
    return promoCodes;
  }
  
  // start NMOBLDS-2167
  /**
   * Sets the configuration key.
   * 
   * @param configurationKey
   *          the new configuration key
   */
  public void setConfigurationKey(final String configurationKey) {
    setPropertyValue(CONFIGURATION_KEY, configurationKey);
  }
  
  /**
   * Gets the configuration key.
   * 
   * @return the configuration key
   */
  public String getConfigurationKey() {
    return (String) getPropertyValue(CONFIGURATION_KEY);
  }
  
  // end NMOBLDS-2167
  // start NMOBLDS-2184
  /**
   * Gets the option choices.
   * 
   * @return the option choices
   */
	public Map<String, String> getOptionChoices() {
		Map<String, String> optionChoices = (Map<String, String>) getPropertyValue(OPTION_CHOICES);
    if (((optionChoices == null) || optionChoices.isEmpty()) && !StringUtils.isBlank(getConfigurationKey())) {
			try {
				CommonComponentHelper.getSelectionSetUtils().setSelectionSetServiceValuesInNMCommerceItem(this);
			} catch (Exception ce) {
				log.error(ce.getMessage());
			}
		}else{
		  this.optionalChoices = optionChoices;
		}
		return this.optionalChoices;
	}
  
  /**
   * Sets the option choices.
   * 
   * @param optionChoices
   *          the option choices
   */
  public void setOptionChoices(Map<String, String> optionChoices) {
    setPropertyValue(OPTION_CHOICES, optionChoices);
    this.optionalChoices = optionChoices;
  }

  /**
   * Gets the option choices.
   * 
   * @return the option choices
   */
	public double getConfigSetPrice() {
	  Double configSetPrice = (Double) getPropertyValue(OrderRepositoryConstants.CONFIG_SET_PRICE);
	  if (null == configSetPrice) {
	  	configSetPrice = 0.0d;
    }
		return configSetPrice;
	}
  
  /**
   * Sets the option choices.
   * 
   * @param optionChoices
   *          the option choices
   */
  public void setConfigSetPrice(double configSetPrice) {
    setPropertyValue(OrderRepositoryConstants.CONFIG_SET_PRICE, configSetPrice);
  }
  
  // end NMOBLDS-2167
  
  /**
   * @return the displayItemTotalPrice
   */
  public double getDisplayItemTotalPrice() {
    return Double.parseDouble(getProduct().getRetailPrice()) * getQuantity();
  }
  
  /**
   * @return the displaySpecialInstructionPrice
   */
  public double getDisplaySpecialInstructionPrice() {
    return Double.parseDouble(getProduct().getLocalizedSpecialInstructionPrice()) * getQuantity();
  }
  
  // bogo group is a hash that indicates the qualified and qualifying item relationship
  // for example, the qualified item (the buy one) and the *n* qualifying items
  // (the get one(s)) will have the same unique text value
  public void setBogoGroup(final String bogoGroup) {
    setPropertyValue("bogoGroup", bogoGroup);
  }

  public String getBogoGroup() {
    return (String) getPropertyValue("bogoGroup");
  }

  public double getBogoQualifiedItemDiscount() {
    return bogoQualifiedItemDiscount;
  }

  public void setBogoQualifiedItemDiscount(double bogoQualifiedItemDiscount) {
    this.bogoQualifiedItemDiscount = bogoQualifiedItemDiscount;
  }

  public double getBogoQualifyingItemMarkup() {
    return bogoQualifyingItemMarkup;
  }

  public void setBogoQualifyingItemMarkup(double bogoQualifyingItemMarkup) {
    this.bogoQualifyingItemMarkup = bogoQualifyingItemMarkup;
  }
  
  /**
   * The purpose of this method is to check whether the status string is for status pre order item.
   * 
   * @return true, if is status pre order string
   */
  public boolean isStatusPreOrderString() {
    return prodSkuUtil.getStatusPreOrderString().equalsIgnoreCase(getTransientStatus());
  }
  
  /**
   * The purpose of this method is to check whether the status string is for status back order item.
   * 
   * @return true, if is status back order string
   */
  public boolean isStatusBackOrderString() {
    return prodSkuUtil.getStatusBackorderString().equalsIgnoreCase(getTransientStatus());
  }
  
  /**
   * The purpose of this method is to check whether the status string is for status drop ship item.
   * 
   * @return true, if is status drop ship string
   */
  public boolean isStatusDropShipString() {
    return prodSkuUtil.getStatusDropshipString().equalsIgnoreCase(getTransientStatus());
  }
  
  /**
   * The purpose of this method is to check if expected ship date to be displayed for the item in minicart and checkout pages in International flow.
   * 
   * @return true, if is intl expected ship date enabled
   */
  public boolean isIntlExpectedShipDateEnabled() {
    // the expected ship date is displayed only for pre order back order and dropship item
    return (isStatusPreOrderString() || isStatusBackOrderString() || isStatusDropShipString());
  }
  
  
  /**
   * Gets the shipfrom based on dropShip,StoreFulfill and pickupStoreNo flag.
   * 
   * @return the ship from
   */
  public String getShipFrom() {
    final NMSku nmSku = getSku();
    if (nmSku.getIsDropShip()) {
      this.shipFrom = INMGenericConstants.VENDOR;
    } else if ((!StringUtils.isBlank(getPickupStoreNo())) || nmSku.getIsStoreFulfillFlag()) {
      if(INMGenericConstants.EIGHTY_NINE.equalsIgnoreCase(getPickupStoreNo()) || INMGenericConstants.EIGHTY.equalsIgnoreCase(getPickupStoreNo())){
        this.shipFrom = INMGenericConstants.WAREHOUSE;
      }else{
        this.shipFrom = INMGenericConstants.STORE;
      }
    } else {
      this.shipFrom = INMGenericConstants.WAREHOUSE;
    }
    return this.shipFrom;
  }
  
  /**
   * Gets the shipReceived based on the and pickupStore flag and Store number set in pickupStore flag.
   * 
   * @return the ship received
   */
  public String getShipReceived() {
    if (isShipToStoreEligible()) {
      if (!StringUtils.isBlank(getPickupStoreNo()) && (INMGenericConstants.EIGHTY_NINE.equalsIgnoreCase(getPickupStoreNo()) || INMGenericConstants.EIGHTY.equalsIgnoreCase(getPickupStoreNo()))) {
         this.shipReceived = INMGenericConstants.STORE_CALL;
      } else if (!StringUtils.isBlank(getPickupStoreNo())) {
         this.shipReceived = INMGenericConstants.STORE;
      } else {
         this.shipReceived = INMGenericConstants.CUSTOMER;
      }
    } else {
      this.shipReceived = INMGenericConstants.CUSTOMER;
    }
    return this.shipReceived;
    
  }
  
  /**
   *  Gets the Estimated Delivery Dates as sent by CMOS
   *  
   * @return the cmosEstimatedDeliveryDate
   */
  public String getCmosEstimatedDeliveryDate() {
	return cmosEstimatedDeliveryDate;
  }

  /**
   * Sets the CMOS Estimated Delivery Date
   * 
   * @param cmosEstimatedDeliveryDate the cmosEstimatedDeliveryDate to set
   */
  public void setCmosEstimatedDeliveryDate(String cmosEstimatedDeliveryDate) {
	this.cmosEstimatedDeliveryDate = cmosEstimatedDeliveryDate;
  }
  
  /**
   * Persist the CMOS Estimated Delivery Date by setting the property value directly on the repository item
   */
  public void persistCmosEstimatedDeliveryDate() {
	if(StringUtilities.isNotEmpty(getCmosEstimatedDeliveryDate())) {
	  setPropertyValue("cmosEstimatedDeliveryDate", getCmosEstimatedDeliveryDate());
	}
  }
} // NMCommerceItem
