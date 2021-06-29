/*
 * <ATGCOPYRIGHT> * Copyright (C) 2001 Art Technology Group, Inc. * All Rights Reserved. No use, copying or distribution ofthis * work may be made except in accordance with a valid license * agreement
 * from Art Technology Group. This notice must be * included on all copies, modifications and derivatives of this * work. * * Art Technology Group (ATG) MAKES NO REPRESENTATIONS OR WARRANTIES * ABOUT
 * THE SUITABILITY OF THE SOFTWARE, EITHER EXPRESS OR IMPLIED, * INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF MERCHANTABILITY, * FITNESS FOR A PARTICULAR PURPOSE, OR NON-INFRINGEMENT. ATG
 * SHALL NOT BE * LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, * MODIFYING OR DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES. * * "Dynamo" is a trademark of Art Technology Group,
 * Inc. </ATGCOPYRIGHT>
 */

package com.nm.commerce.pricing;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;

import atg.commerce.order.CommerceItem;
import atg.commerce.order.CommerceItemImpl;
import atg.commerce.order.CommerceItemRelationship;
import atg.commerce.order.CreditCard;
import atg.commerce.order.HardgoodShippingGroup;
import atg.commerce.order.Order;
import atg.commerce.order.OrderTools;
import atg.commerce.order.PaymentGroup;
import atg.commerce.order.Relationship;
import atg.commerce.order.ShippingGroup;
import atg.commerce.order.ShippingGroupCommerceItemRelationship;
import atg.commerce.order.ShippingGroupRelationship;
import atg.commerce.pricing.ItemPriceInfo;
import atg.commerce.pricing.ItemPricingEngine;
import atg.commerce.pricing.OrderPricingEngine;
import atg.commerce.pricing.PricingException;
import atg.commerce.pricing.PricingModelHolder;
import atg.commerce.pricing.PricingTools;
import atg.commerce.pricing.ShippingPriceInfo;
import atg.commerce.pricing.ShippingPricingEngine;
import atg.commerce.pricing.TaxPriceInfo;
import atg.commerce.pricing.TaxPricingEngine;
import atg.core.util.Address;
import atg.core.util.StringUtils;
import atg.integrations.taxware.SalesTaxService;
import atg.integrations.taxware.TaxRequest;
import atg.integrations.taxware.TaxResult;
import atg.integrations.taxware.TaxwareCriticalException;
import atg.integrations.taxware.TaxwareMinorException;
import atg.nucleus.Nucleus;
import atg.payment.tax.ShippingDestination;
import atg.payment.tax.ShippingDestinationImpl;
import atg.payment.tax.TaxProcessor;
import atg.payment.tax.TaxRequestInfoImpl;
import atg.payment.tax.TaxableItem;
import atg.payment.tax.TaxableItemImpl;
import atg.repository.MutableRepository;
import atg.repository.MutableRepositoryItem;
import atg.repository.Repository;
import atg.repository.RepositoryException;
import atg.repository.RepositoryItem;
import atg.repository.RepositoryView;
import atg.repository.rql.RqlStatement;
import atg.service.util.CurrentDate;
import atg.servlet.DynamoHttpServletRequest;
import atg.servlet.ServletUtil;

import com.nm.ajax.checkout.utils.ComponentUtils;
import com.nm.ajax.checkout.utils.RepositoryUtils;
import com.nm.collections.ExtraAddress;
import com.nm.collections.ExtraAddressArray;
import com.nm.collections.ExtraShippingCharge;
import com.nm.collections.Giftwrap;
import com.nm.collections.GiftwrapArray;
import com.nm.collections.NMPromotion;
import com.nm.collections.NMPromotionArray;
import com.nm.collections.ServiceLevel;
import com.nm.commerce.GiftCardHolder;
import com.nm.commerce.NMCommerceItem;
import com.nm.commerce.NMProfile;
import com.nm.commerce.NMRepositoryContactInfo;
import com.nm.commerce.catalog.NMProdCountry;
import com.nm.commerce.catalog.NMProduct;
import com.nm.commerce.catalog.NMRestrictionCode;
import com.nm.commerce.checkout.CheckoutAPI;
import com.nm.commerce.checkout.CheckoutComponents;
import com.nm.commerce.pagedef.model.UIElementConstants;
import com.nm.commerce.promotion.NMOrderImpl;
import com.nm.common.INMGenericConstants;
import com.nm.common.SmartPostServiceLevelSessionBean;
import com.nm.components.CommonComponentHelper;
import com.nm.integration.ShopRunnerConstants;
import com.nm.utils.BrandSpecs;
import com.nm.utils.LocalizationUtils;
import com.nm.utils.ProdCountryUtils;
import com.nm.utils.PromoUtils;
import com.nm.utils.StringUtilities;
import com.nm.utils.SystemSpecs;
import com.nm.utils.fiftyone.FiftyOneUtils;
import com.nm.utils.pricing.PricingUtils;

/**
 * A class which can perform a variety of pricing functions across different types of PricingEngine. It simplifies interaction with a spectrum of PricingEngines. It also has a number of static
 * currency-related methods for use by all pricing engines.
 * <p>
 * Properties:
 * <ul>
 * <li><b>itemPricingEngine</b> points to an instance of an ItemPricingEngine for use by pricingTools methods that need an ItemPricingEngine.
 * <li><b>orderPricingEngine</b> points to an instance of an OrderPricingEngine for use by pricingTools methods that need an OrderPricingEngine.
 * <li><b>taxPricingEngine</b> points to an instance of an TaxPricingEngine for use by pricingTools methods that need an TaxPricingEngine.
 * <li><b>shippingPricingEngine</b> points to an instance of an ShippingPricingEngine for use by pricingTools methods that need an ShippingPricingEngine.
 * <li><b>defaultLocale</b> is the locale that's used to price Orders, Items, Shipping, and Tax if no other locale is explicitly specified.
 * </ul>
 * 
 * @see ItemPricingEngine
 * @see TaxPricingEngine
 * @see ShippingPricingEngine
 * @see OrderPricingEngine
 * 
 * @author: Sunil Bindal and Chee-Chien Loo
 * 
 */

public class NMPricingTools extends PricingTools {
  
  private static final String CURRENCY_CODE = "USD";
  private static final String BRAND_SPECS_PATH = "/nm/utils/BrandSpecs";
  private static final String SYSTEM_SPECS_PATH = "/nm/utils/SystemSpecs";
  private static final String GW_ARRAY_PATH = "/nm/collections/GiftwrapArray";
  private static final String EA_ARRAY_PATH = "/nm/collections/ExtraAddressArray";
  
  /**
   * The next three properties define the dates for the free shipping promotion as well as the total amount of merchandise that must be ordered before free shipping is applied. Note: this way of
   * implementing promotions is VERY temporary and should be removed when we find a better way to implement promotions. - nmmc5
   */
  private Date promoBeginDate;
  private Date promoEndDate;
  private double promoTotal;
  
  private SalesTaxService mSalesTaxService;
  private String findMessage;
  private CurrentDate mCurrentDate;
  private String mShippingCommodityCode;
  private Repository mChartRepository;
  private ExtraShippingData mExtraShippingData;
  private TaxProcessor mTaxProcessor;
  private OrderTools mOrderTools;
  private ExtraShippingCharge mExtraShippingCharge;
  private boolean loggingDebugCanada;
  private ProdCountryUtils prodCountryUtils;
  private GiftwrapArray thePromos = null;
  private ExtraAddressArray theEAPromos = null;
  private int internationalPriceScale;
  
  /**
   * Price the sub total and any order level discounts. The OrderPriceInfo of the Order will contain the raw subtotal for all the items, and the amount will include any order level discounts. The tax
   * and shipping are not set.
   * 
   * @return the total of the order, minus shipping and taxes
   * @param pOrder
   *          the order to price
   * @param pPricingModels
   *          the pricing models used in this pricing
   * @param pLocale
   *          the locale of the user, may be null
   * @param pProfile
   *          the user, may be null
   * @param pExtraParameters
   *          A Map of extra parameters to be used in the pricing, may be null
   * @exception PricingException
   *              if there was an error while computing the pricing information
   */
  @Override
  public double priceOrderForOrderTotal(final Order pOrder, @SuppressWarnings("rawtypes") final Collection pPricingModels, final java.util.Locale pLocale, final RepositoryItem pProfile,
          @SuppressWarnings("rawtypes") final Map pExtraParameters, final boolean pGenerateOrderRanges) throws PricingException {
    return priceOrderForOrderTotal(pOrder, pPricingModels, pLocale, pProfile, pExtraParameters);
  }
  
  @Override
  public double priceOrderForOrderTotal(final Order pOrder, final Locale pLocale, final RepositoryItem pProfile, @SuppressWarnings("rawtypes") final Map pExtraParameters) throws PricingException {
    return super.priceOrderForOrderTotal(pOrder, pLocale, pProfile, pExtraParameters);
  }
  
  protected ShippingGroupRelationship getShippingGroupRelationship(final CommerceItem ci) {
    @SuppressWarnings("unchecked")
    final List<ShippingGroupRelationship> shippingGroups = ci.getShippingGroupRelationships();
    if ((shippingGroups == null) || (shippingGroups.size() < 1)) {
      throw new NullPointerException("Commerce item " + ci.getId() + " has no shiping group");
    }
    
    return shippingGroups.get(0);
  }
  
  @SuppressWarnings("unchecked")
  protected ShippingGroupRelationship getShippingGroupRelationshipForRepricing(final CommerceItem ci) {
   
    final List<ShippingGroupRelationship> shippingGroups = ci.getShippingGroupRelationships();

    if ((shippingGroups != null) || (shippingGroups.size() > 0)) {
    	return shippingGroups.get(0);
    }
    
    if(isLoggingError())
    	logError("CommerceItem :- " + ci.getId() + " does not have an associated shipping group.");
    return null;
  }
  
  protected double calculateTotalDuty(final Order pOrder) throws PricingException {
    double dutyTotal = 0.00;
    double dutyCharge = 0.00;
    double dutyRate = 0.00;
    double price = 0.00;
    double giftWrapCharge = 0.00;
    
    // figure duty per sg country code.
    
    final List<Relationship> rShipList = getRelationships(pOrder);
    ShippingGroupCommerceItemRelationship sgRelationship = new ShippingGroupCommerceItemRelationship();
    
    for (final Relationship tempRel : rShipList) {
      if (tempRel instanceof ShippingGroupCommerceItemRelationship) {
        sgRelationship = (ShippingGroupCommerceItemRelationship) tempRel;
        final HardgoodShippingGroup hGSg = (HardgoodShippingGroup) sgRelationship.getShippingGroup();
        final int nbrOfItems = hGSg.getCommerceItemRelationshipCount();
        if (isLoggingDebug()) {
          logDebug("***NMPT calculateTotalDuty nbrOfItems of CI:" + nbrOfItems);
        }
        
        final CommerceItemRelationship CIR = (CommerceItemRelationship) tempRel;
        final NMCommerceItem CI = (NMCommerceItem) CIR.getCommerceItem();
        final NMItemPriceInfo ipi = (NMItemPriceInfo) CI.getPriceInfo();
        
        // 1. get shipping country
        final String state = hGSg.getShippingAddress().getState();
        final String country = hGSg.getShippingAddress().getCountry();
        if (isLoggingDebug()) {
          logDebug("***>>NMPT calculateTotalDuty item is:" + hGSg.getId());
          logDebug("***NMPT calculateTotalDuty country/state is ***" + country + "/" + state);
        }
        
        // 2. getprodCountry duty rate
        final String prodId = sgRelationship.getCommerceItem().getAuxiliaryData().getProductId();
        final NMProduct nmProd = new NMProduct(prodId);
        final Map<String, NMProdCountry> prodCountryList = nmProd.getCountryMap();
        final NMProdCountry nmPC = prodCountryList.get(country);
        if (nmPC != null) {
          dutyRate = nmPC.getDutyRate().doubleValue();
          final String countryId = nmPC.getCountryCode();
          if (isLoggingDebug()) {
            logDebug("***NMPT calculateTotalDuty found prods country in list:" + countryId + "-" + dutyRate);
          }
        }
        
        // 3. getProduct price
        price = ipi.getAmount();
        // System.out.println("***NMPT calculateTotalDuty price is :"+price);
        
        // 4. get Item Giftwrap charge if any
        giftWrapCharge = ipi.getOtherCharges();
        // System.out.println("***>>>NMPT calculateTotalDuty giftWrapCharge is:"+giftWrapCharge);
        
        // 6. setcommerceItem dutyCharge
        // System.out.println("***>>>NMPT calculateTotalDuty price + giftWrapCharge is:"+price+"-"+giftWrapCharge);
        dutyCharge = (price + giftWrapCharge) * dutyRate;
        dutyCharge = PricingUtils.amtDivisibleByQty(dutyCharge, CI.getQuantity());
        
        ipi.setDutyCharge(dutyCharge);
        // System.out.println("***>>>NMPT calculateTotalDuty dutyCharge is:"+ipi.getDutyCharge());
        dutyTotal = dutyTotal + dutyCharge;
      }
    }
    
    // System.out.println("***NMPT calculateTotalDuty orders dutyTotal:"+dutyTotal);
    return dutyTotal;
  }
  
  /**
   * Calculates the restriction code charges for each commerce item and applies it to the individual price infos. It also rolls up the charges to the order price info.
   * 
   * @param pOrder
   * @return
   * @throws PricingException
   */
  protected void calculateRestrictionCharges(final Order pOrder) throws PricingException {
    
    // This hash will store a collection of shipping group restriction
    // charges for a shipping group.
    final HashMap<String, ShippingGroupRestrictionCodeCharges> shippingGroupRestrictionMap = new HashMap<String, ShippingGroupRestrictionCodeCharges>();
    
    // Get the order price info so that we can update it as we iterator through each commerce item.
    final NMOrderPriceInfo orderPriceInfo = (NMOrderPriceInfo) pOrder.getPriceInfo();
    
    // Clear any preview restriction charges from the order
    orderPriceInfo.clearRestrictionCharges();
    
    final List<HardgoodShippingGroup> shippingGroups = getShippingGroups(pOrder);
    
    // Iterate through the shipping groups of the order
    for (final ShippingGroup shippingGroup : shippingGroups) {
      
      // clear the map since we're looking at this
      // shipping group for the first time.
      shippingGroupRestrictionMap.clear();
      
      if (shippingGroup instanceof HardgoodShippingGroup) {
        final HardgoodShippingGroup hardgoodShippingGroup = (HardgoodShippingGroup) shippingGroup;
        
        final String country = hardgoodShippingGroup.getShippingAddress().getCountry();
        
        final List<ShippingGroupCommerceItemRelationship> commerceItemRelationships = getCommerceItemRelationships(hardgoodShippingGroup);
        
        // Iterate through the items within the shipping group.
        for (final CommerceItemRelationship commerceItemRelationship : commerceItemRelationships) {
          final CommerceItem commerceItem = commerceItemRelationship.getCommerceItem();
          
          // clearing the commerce item restriction charge before evaluating
          
          final NMItemPriceInfo priceInfo = (NMItemPriceInfo) commerceItem.getPriceInfo();
          if (priceInfo != null) {
            priceInfo.removeItemRestrictionCharge();
          }
          
          final String productId = commerceItem.getAuxiliaryData().getProductId();
          
          final NMProduct product = new NMProduct(productId);
          
          // Get the restriction codes for this product based on the ship to country
          final List<NMRestrictionCode> restrictionCodes = product.getRestrictionCodes(country);
          
          // Iterate through the restriction codes for this product/country combo.
          for (final NMRestrictionCode restrictionCode : restrictionCodes) {
            final String code = restrictionCode.getRestrictionCode();
            
            final String calculationType = restrictionCode.getCalculationType();
            
            // Process Unit charge type restriction codes
            if (calculationType.equalsIgnoreCase(NMRestrictionCode.UNIT_CHARGE)) {
              applyUnitRestrictionChargeToCommerceItem(restrictionCode, commerceItem, orderPriceInfo);
              // Process Line charge type restriction codes
            } else if (calculationType.equalsIgnoreCase(NMRestrictionCode.LINE_CHARGE)) {
              applyLineRestrictionChargeToCommerceItem(restrictionCode, commerceItem, orderPriceInfo);
              // Process Ship To charge type restriction codes
            } else if (calculationType.equalsIgnoreCase(NMRestrictionCode.SHIP_TO_CHARGE)) {
              
              // for ship to charge restriction codes, we can not apply the
              // charge to the commerce item until we know how many
              // commerce items have that same charge. So we use the
              // shippingGroupRestrictionMap to store the ship to charges and
              // the associated commerce items.
              
              ShippingGroupRestrictionCodeCharges restrictionCharge = shippingGroupRestrictionMap.get(code);
              
              // if we haven't seen this restriction code for this shippnig group then
              // we'll add it to our hash.
              if (restrictionCharge == null) {
                restrictionCharge = new ShippingGroupRestrictionCodeCharges(restrictionCode);
                shippingGroupRestrictionMap.put(code, restrictionCharge);
              }
              
              // Add the current commerce item to the restriction charge. Once we know
              // all the items that have that restriction charge, we will apply the
              // appropriate amount to each commerce item.
              restrictionCharge.addCommerceItem(commerceItem);
            }
          }
        }
        
        // Check to see if we encountered any ship to restriction codes for this shipping group
        // If so, then we'll apply the total charge for each of those restrictions to the
        // commerce items that drove them.
        final Collection<ShippingGroupRestrictionCodeCharges> shippingGroupRestrictions = shippingGroupRestrictionMap.values();
        for (final ShippingGroupRestrictionCodeCharges restrictionCharge : shippingGroupRestrictions) {
          // Tell each restricionCharge to apply the ship to restriction charge to all
          // the commerce items that had that charge.
          restrictionCharge.applyCharges(orderPriceInfo);
        }
      }
    }
  }
  
  /**
   * 
   * @param restrictionCode
   * @param commerceItem
   * @param orderPriceInfo
   */
  private void applyUnitRestrictionChargeToCommerceItem(final NMRestrictionCode restrictionCode, final CommerceItem commerceItem, final NMOrderPriceInfo orderPriceInfo) {
    // for unit charge restriction codes, we calculate the
    // total charge based on the surcharge amount of the
    // restriction code times the number of units for the
    // commerce item.
    final double perUnitCharge = restrictionCode.getSurchargeAmount();
    final long quantity = commerceItem.getQuantity();
    
    final double totalCharge = perUnitCharge * quantity;
    
    final NMItemPriceInfo priceInfo = (NMItemPriceInfo) commerceItem.getPriceInfo();
    
    final String code = restrictionCode.getRestrictionCode();
    
    // Add this restriction charge to the price info of this
    // commerceitem.
    priceInfo.addItemRestrictionCharge(code, totalCharge);
    
    orderPriceInfo.accumulateItemRestrictionCharges(code, totalCharge);
  }
  
  /**
   * @param restrictionCode
   * @param commerceItem
   * @param orderPriceInfo
   */
  private void applyLineRestrictionChargeToCommerceItem(final NMRestrictionCode restrictionCode, final CommerceItem commerceItem, final NMOrderPriceInfo orderPriceInfo) {
    // for line charge restriction codes, the charge is simply
    // the surchange amount from the restriction code
    final double surchargeAmount = restrictionCode.getSurchargeAmount();
    
    final NMItemPriceInfo priceInfo = (NMItemPriceInfo) commerceItem.getPriceInfo();
    
    final String code = restrictionCode.getRestrictionCode();
    
    // Add this restriction charge to the price info of this
    // commerceitem.
    priceInfo.addItemRestrictionCharge(code, surchargeAmount);
    
    orderPriceInfo.accumulateItemRestrictionCharges(code, surchargeAmount);
  }
  
  @Override
  public double priceOrderForOrderTotal(final Order pOrder, @SuppressWarnings("rawtypes") final Collection pPricingModels, final Locale pLocale, final RepositoryItem pProfile,
          @SuppressWarnings("rawtypes") final Map pExtraParameters) throws PricingException {
    
    synchronized (pOrder) {
      final NMOrderPriceInfo info = (NMOrderPriceInfo) getOrderPricingEngine().priceOrder(pOrder, pPricingModels, pLocale, pProfile, pExtraParameters);
      
      double giftWrapCharges = round(calculateGiftWrapCharges(pOrder));
      
      final NMOrderImpl nmOrder = (NMOrderImpl) pOrder;
      if (nmOrder.isAlipay()) {
        giftWrapCharges = 0.0;
      }
      
      if (isLoggingDebug()) {
        logDebug("Giftwrap Charges: " + giftWrapCharges);
      }
      info.setGiftWrapping(giftWrapCharges);
      
      final double totalDuty = calculateTotalDuty(pOrder);
      final double assemblyDiscount = calculateAssemblyDiscountTotal(pOrder);
      
      if (isLoggingDebug()) {
        logDebug("totalDuty: " + totalDuty);
        logDebug("assemblyDiscount: " + assemblyDiscount);

      }
      info.setTotalDuty(totalDuty);
      info.setAssemblyDiscount(assemblyDiscount);
      
      pOrder.setPriceInfo(info);
      
      calculateRestrictionCharges(pOrder);
      
      // if (info != null)
      return info.getAmount();
      // else
      // return 0.0;
    }
  }
  
  /**
   * Calculate the total giftwrap charges
   * 
   * @return the total of the giftwrap charges
   * @param pOrder
   *          the order to get giftwrap ids from commerce items
   */
  protected double calculateGiftWrapCharges(final Order pOrder) {
    final List<NMCommerceItem> commerceItems = ((NMOrderImpl) pOrder).getNmCommerceItems();
    double total = 0.00;
    final NMOrderImpl nmOrder = (NMOrderImpl) pOrder;
    for (final NMCommerceItem item : commerceItems) {
      double wrappingCharges = item.getGiftWrapPrice();
      if (nmOrder.isAlipay()) {
        wrappingCharges = 0.0;
      }
      final NMItemPriceInfo iPriceInfo = (NMItemPriceInfo) item.getPriceInfo();
      iPriceInfo.setOtherCharges(wrappingCharges);
      total += wrappingCharges;
    }
    return total;
  }
  
  /**
   * Return the price for one giftwrap based on promotion.
   * 
   * @param pOrder
   * @return the price of a single giftwrap
   */
  public Object getGiftwrapPromoPrice(final Order pOrder) {
    try {
      // need to check if the customer has a giftwrap promotion and if so then get the price for that
      final String giftWrapKey = PromoUtils.getGivenPromoForOrder(pOrder, NMPromotionArray.GIFTWRAP_ARRAY);
      if ((giftWrapKey != null) && !giftWrapKey.trim().equals("")) {
        thePromos = (GiftwrapArray) Nucleus.getGlobalNucleus().resolveName(GW_ARRAY_PATH);
        final Giftwrap theGWpromo = thePromos.getPromotion(giftWrapKey);
        final String theGiftwrapPrice = theGWpromo.getGiftwrapPrice();
        
        // double giftWrapPrice = Double.valueOf(theGWpromo.getGiftwrapPrice()).doubleValue();
        
        if (isLoggingDebug()) {
          logDebug("NMPricingTools:getGiftwrapPromoPrice:giftWrapKey was " + giftWrapKey);
          logDebug("NMPricingTools:getGiftwrapPromoPrice:giftWrapPrice was " + theGiftwrapPrice);
        }
        
        return Double.valueOf(theGiftwrapPrice);
      }
    } catch (final NumberFormatException nfe) {
      if (isLoggingError()) {
        logError("A NumberFormatException error occurred NMPricingTools.getGiftwrapPromoPrice", nfe);
      }
    } catch (final NullPointerException npe) {
      if (isLoggingError()) {
        logError("A NullPointerException error occurred NMPricingTools.getGiftwrapPromoPrice", npe);
      }
    } catch (final Exception e) {
      if (isLoggingError()) {
        logError("An error occurred NMPricingTools.getGiftwrapPromoPrice", e);
      }
    }
    
    return null;
  }
  
 
  
  /**
   * Returns the price associated with a gift wrap code
   * 
   * @param giftWrapId
   *          - The CMOS gift wrap ID
   * @return the cost of that gift wrap
   */
  public RepositoryItem getGiftWrapItem(final String giftWrapId) {
    final Repository orderRepository = getOrderTools().getOrderRepository();
    RepositoryItem giftWrapItem = null;
    try {
      giftWrapItem = orderRepository.getItem(giftWrapId, "giftwrap");
    } catch (final RepositoryException re) {
      if (isLoggingError()) {
        logError("A repository exception occured while trying to determine the giftWrapCharge", re);
      }
    }
    return giftWrapItem;
    
  }
  
  /**
   * Price each shipping group, and set their priceInfo's to the result
   * 
   * @return the total of all the shipping
   * @param pOrder
   *          the order to price
   * @param pPricingModels
   *          the pricing models used in this pricing
   * @param pLocale
   *          the locale of the user, may be null
   * @param pProfile
   *          the user, may be null
   * @param pExtraParameters
   *          A Map of extra parameters to be used in the pricing, may be null
   * @exception PricingException
   *              if there was an error while computing the order total
   */
  
  @Override
  public double priceShippingForOrderTotal(final Order pOrder, @SuppressWarnings("rawtypes") final Collection pPricingModels, final java.util.Locale pLocale, final RepositoryItem pProfile,
          @SuppressWarnings("rawtypes") final java.util.Map pExtraParameters, final boolean pGenerateOrderRanges) throws PricingException {
    return priceShippingForOrderTotal(pOrder, pPricingModels, pLocale, pProfile, pExtraParameters);
  }
  
  @Override
  public double priceShippingForOrderTotal(final Order pOrder, final java.util.Locale pLocale, final RepositoryItem pProfile, @SuppressWarnings("rawtypes") final Map pExtraParameters)
          throws PricingException {
    return super.priceShippingForOrderTotal(pOrder, pLocale, pProfile, pExtraParameters);
  }
  
  @Override
  public double priceShippingForOrderTotal(final Order pOrder, final PricingModelHolder pPricingModels, final java.util.Locale pLocale, final RepositoryItem pProfile,
          @SuppressWarnings("rawtypes") final Map pExtraParameters) throws PricingException {
    return priceShippingForOrderTotal(pOrder, pPricingModels.getShippingPricingModels(), pLocale, pProfile, pExtraParameters);
  }
  
  @SuppressWarnings("unchecked")
  @Override
  public double priceShippingForOrderTotal(final Order pOrder, @SuppressWarnings("rawtypes") final Collection pPricingModels, final Locale pLocale, final RepositoryItem pProfile,
          @SuppressWarnings("rawtypes") final Map pExtraParameters) throws PricingException {
    
    synchronized (pOrder) {
      double total = 0.0;
      double totalShippingCost = 0.0;
      double parentheticalTotal = 0.0;
      
      final NMOrderImpl nmOrder = (NMOrderImpl) pOrder;
      final List<HardgoodShippingGroup> shippingGroups = getShippingGroups(pOrder);
      
      final int numberOfShippingGroups = sgWithCIRelationships(shippingGroups);
      
      if (shippingGroups != null) {
        try {
          final HashMap<String, String> shipRelMap = new HashMap<String, String>();
          final List<Relationship> relList = getRelationships(pOrder);
          BrandSpecs brandSpecs = CommonComponentHelper.getBrandSpecs();
          for (final Relationship tempRel : relList) {
            if (tempRel instanceof ShippingGroupCommerceItemRelationship) {
              final ShippingGroupCommerceItemRelationship sgRelationship = (ShippingGroupCommerceItemRelationship) tempRel;
              final NMCommerceItem commerceItem = (NMCommerceItem) sgRelationship.getCommerceItem();
              final RepositoryItem productRI = (RepositoryItem) commerceItem.getAuxiliaryData().getProductRef();
              
              if (new String("shippingGroupCommerceItem").equals(tempRel.getRelationshipClassType())
                      && GiftCardHolder.VIRTUAL_GIFT_CARD_MERCH_TYPE.equals(productRI.getPropertyValue("merchandiseType"))) {
                // do nothing because we don't want extra shipping charge for virtual gift cards
              } else if (StringUtilities.isNotEmpty(commerceItem.getPickupStoreNo()) && !commerceItem.getShipToStoreFlg()) {
                // do nothing for BOPS items
              } else if (StringUtilities.isNotEmpty(commerceItem.getPickupStoreNo()) && commerceItem.getShipToStoreFlg() && brandSpecs.isBOSSFreeShippingEnaled()) {
            	// do nothing for BOSS items
              }else {
                shipRelMap.put(sgRelationship.getShippingGroup().getDescription(), "");
              }
            }
          }
          
          if (shipRelMap.size() > 0) {
            total = total + ((shipRelMap.size() - 1) * determineExtraAddressCharge(pOrder));
          }
        } catch (final Exception e) {
          e.printStackTrace();
          total = total + ((numberOfShippingGroups - 1) * determineExtraAddressCharge(pOrder));
        }
        
        // loop through groups
        // if order is not us
        // then remove promo
        // delete token
        // clear data
        
        boolean ignoreSL2Items = false;
        if (nmOrder.isShopRunnerPromoActive()) {
          ignoreSL2Items = true;
        }
        
        // merchandiseTotal will not include BOPS items or ShopRunner SL2 items
        final double merchandiseTotal = calculateMerchandiseTotal(nmOrder.getNmCommerceItems(), true, ignoreSL2Items);
        parentheticalTotal = calculateParentheticalTotal(nmOrder.getNmCommerceItems());
        
        if (parentheticalTotal > 0.0) {
          nmOrder.setCheckForParentheticalCharges(true);
        } else {
          nmOrder.setCheckForParentheticalCharges(false);
        }
        
        if (isLoggingDebug()) {
          logDebug("Total of Extra Shipping Charge: " + total + "\nMerchandise Total: " + merchandiseTotal + "\nParenthetical Total: " + parentheticalTotal);
        }
        
        final RepositoryItem currentChart = findCurrentChart(nmOrder.getNmCommerceItems(), nmOrder.getActivatedPromoCode(), merchandiseTotal);
        
        // nmts5 start for capturing lost shipping
        final RepositoryItem realChart = findCurrentChart(nmOrder.getNmCommerceItems(), " ", 0);
        final String activatedPromoCode = nmOrder.getActivatedPromoCode();
        if ((activatedPromoCode != null) && (activatedPromoCode.toUpperCase().indexOf("FREEBASESHIPPING") != -1)) {
          if ((realChart != null)) {
            nmOrder.setLostShipping(findBaseShippingCost(realChart, merchandiseTotal));
          }
        }
        // nmts5 end
        
        if (isLoggingDebug()) {
          logDebug("Current Chart Rep Item: " + currentChart);
        }
        
        if ((merchandiseTotal != 0) && (currentChart != null)) {
          if (isLoggingDebug()) {
            logDebug("Base Shipping Cost is: " + findBaseShippingCost(currentChart, merchandiseTotal));
          }
          total = total + findBaseShippingCost(currentChart, merchandiseTotal);
        }
        
        if (isLoggingDebug()) {
          logDebug("Total After Base Shipping Cost is: " + total);
        }
        if (isLoggingDebug()) {
          logDebug("Non Continental Cost is: " + calculateNonContinentalShippingCharge(shippingGroups));
        }
        
        if (!nmOrder.is_iPhoneOrder()) {
          total = total + calculateNonContinentalShippingCharge(shippingGroups);
        }
        total = total + parentheticalTotal;
        
        // nmve1 - override the shipping total for international orders, and distribute the price over commerce items
        if (nmOrder.isInternationalOrder()) {
          total = CheckoutComponents.getFiftyOneUtils().getDomesticShippingCharge();
          if (isLoggingDebug()) {
            logDebug("Total after international shipping charge override: " + total);
          }
          
          if (null != pOrder.getCommerceItems()) {
            final double distLeftOverAmountForEachItem = total / pOrder.getCommerceItemCount();
            final double roundedDistLeftOverAmountForEachItem = new BigDecimal(distLeftOverAmountForEachItem).setScale(2, BigDecimal.ROUND_DOWN).doubleValue();
            
            if (isLoggingDebug()) {
              logDebug("distrubing over international shipping items: " + roundedDistLeftOverAmountForEachItem);
            }
            
            for (final NMCommerceItem item : nmOrder.getNmCommerceItems()) {
              final NMItemPriceInfo iPriceInfo = (NMItemPriceInfo) item.getPriceInfo();
              iPriceInfo.setFreightCharges(0d);
              
              if (total >= roundedDistLeftOverAmountForEachItem) {
                total = total - addAmountToFreight(item, roundedDistLeftOverAmountForEachItem);
              } else {
                total = total - addAmountToFreight(item, total);
              }
              if (isLoggingDebug()) {
                logDebug("Total after international shipping charge distribution iteration: " + total);
              }
            }
          } else {
            if (isLoggingError()) {
              logError("Error during international shipping charge distribution: the order has no commerce items");
            }
          }
        } else {
          // Distribute the price over commerce items
          distributingTotalBaseShippingCost(total, nmOrder.getNmCommerceItems(), ignoreSL2Items);
        }
        
        if (isLoggingDebug()) {
          logDebug("DEBUG: The total before calling shipping Engine: " + total);
        }
        
        for (final ShippingGroup shippingGroup : shippingGroups) {
          if (shippingGroup != null) {
            final Boolean sddFreeShippingCheck = sddFreeShippingCheck(merchandiseTotal, nmOrder);
            pExtraParameters.put("SDDFreeShipping", sddFreeShippingCheck);
            
            final ShippingPriceInfo info = getShippingPricingEngine().priceShippingGroup(pOrder, shippingGroup, pPricingModels, pLocale, pProfile, pExtraParameters);
            
            shippingGroup.setPriceInfo(info);
            
            if (isLoggingDebug()) {
              logDebug("DEBUG: The total service level charges for this ShippingGroup id: " + shippingGroup.getId() + " = " + info.getAmount());
            }
            
            if (info != null) {
              if (!nmOrder.isInternationalOrder()) {
                // distribute expedition cost over the commerce items
                distributeExpeditionCharge(shippingGroup, round(info.getAmount()));
              }
            }
          }
        }
        
      }
      
      totalShippingCost = calculateTotalShipCost(pOrder);
      // SmartPost : deducting the shipping amount based on the service level for Alipay orders
      
      final String abTestGroup = CheckoutComponents.getServiceLevelArray().getServiceLevelABTestGroup();
      if (nmOrder.isAlipay() && CommonComponentHelper.getSystemSpecs().isSmartPostEnabled() && abTestGroup.equalsIgnoreCase(UIElementConstants.AB_TEST_SERVICE_LEVEL_GROUPS)) {
        final SmartPostServiceLevelSessionBean smartPostServiceLevelSessionBean = CheckoutComponents.getSmartPostServiceLevelSessionBean(ServletUtil.getCurrentRequest());
        final String selectedServiceLevelCode = smartPostServiceLevelSessionBean.getSmartPostSelectedServiceLevel();
        if (StringUtilities.isNotEmpty(selectedServiceLevelCode)) {
          final ServiceLevel serviceLevel = CheckoutAPI.getServiceLevelByCodeAndCountry(ServiceLevel.SL3_SERVICE_LEVEL_TYPE, ShopRunnerConstants.US_COUNTRY_CODE);
          final double amt = serviceLevel.getAmount();
          if (totalShippingCost > 0) {
            totalShippingCost = totalShippingCost - amt;
          }
        }
      }
      
      // totalShippingCost = totalShippingCost(pOrder.getCommerceItems());
      
      // Total up all the freight charges of the commerce item level
      return totalShippingCost;
    }
  }
  
  /**
   * Checks to see if the merchandise total + gift wrapping qualifies for free same day delivery shipping
   * 
   * @param merchandiseTotal
   *          Double
   * @param nmOrder
   *          NMOrderImpl
   * @return if order qualifies for free same day delivery shipping
   */
  private Boolean sddFreeShippingCheck(final Double merchandiseTotal, final NMOrderImpl nmOrder) {
    Boolean freeSddShipping = Boolean.FALSE;
    if (CommonComponentHelper.getBrandSpecs().isSameDayDeliveryEnabled() && !nmOrder.isInternationalOrder()) {
      final Boolean isSameDayDeliveryOrder = CommonComponentHelper.getSameDayDeliveryUtil().isSameDayDeliveryOrder(nmOrder);
      if (isSameDayDeliveryOrder) {
        final NMOrderPriceInfo priceInfo = (NMOrderPriceInfo) nmOrder.getPriceInfo();
        final Double giftWrap = priceInfo.getGiftWrapping();
        final Double totalCost = merchandiseTotal + giftWrap;
        final Double sddOrderThreshold = CommonComponentHelper.getSameDayDeliveryUtil().getSddOrderTotalThreshold();
        if (totalCost >= sddOrderThreshold) {
          freeSddShipping = Boolean.TRUE;
        }
      }
    }
    return freeSddShipping;
  }
  
  protected int sgWithCIRelationships(final List<HardgoodShippingGroup> shippingGroups) {
    int sgNumber = 0;
    if (shippingGroups != null) {
      for (final ShippingGroup sg : shippingGroups) {
        final int count = sg.getCommerceItemRelationshipCount();
        if (count > 0) {
          sgNumber++;
        }
      }
    }
    return sgNumber;
  }
  
  // Total up all the freight charges of the commerce item level
  protected double totalShippingCost(final List<NMCommerceItem> commerceItems) {
    double total = 0.0;
    for (final NMCommerceItem item : commerceItems) {
      if (isLoggingDebug()) {
        logDebug("TotalShippingCost : " + item.getId() + "-" + item.getProductId() + "-" + item.getCmosProdName());
      }
      final NMItemPriceInfo iPriceInfo = (NMItemPriceInfo) item.getPriceInfo();
      final double freightCharges = iPriceInfo.getFreightCharges() + iPriceInfo.getAssemblyDiscount();
      iPriceInfo.setFreightCharges(freightCharges);
      if (StringUtilities.isNullOrEmpty(item.getPickupStoreNo()) || item.getShipToStoreFlg()) {
        total += freightCharges;
      }
    }
    return total;
  }
  
  // Total up all the freight charges of the commerce item level
  // 140706 vsal3: the hmPickupFlg map is deprecated as the old way to determine if an item is a BOPS
  // item and is no longer used in favor of the standard check (pickupStoreNo and shipToStoreFlg).
  // Furthermore, old code was setting FreightCharges on the commerceItem.priceInfo regardless of
  // BOPS, causing trickle-down issue with CMOS.
  protected double totalShippingCost(final List<NMCommerceItem> commerceItems, final Map<String, String> hmPickupFlg) {
    double total = 0.0;
    for (final NMCommerceItem item : commerceItems) {
      if (isLoggingDebug()) {
        logDebug("Calculcating Shipping for-" + item.getId() + "-" + item.getProductId() + "-" + item.getCmosProdName());
      }
      final NMItemPriceInfo iPriceInfo = (NMItemPriceInfo) item.getPriceInfo();
      final double freightCharges = iPriceInfo.getFreightCharges() + iPriceInfo.getAssemblyDiscount();
      if (StringUtilities.isNullOrEmpty(item.getPickupStoreNo()) || item.getShipToStoreFlg()) {
        iPriceInfo.setFreightCharges(freightCharges);
        total += freightCharges;
      }
    }
    if (isLoggingDebug()) {
      logDebug("New Calculated Shipping Cost :" + total);
    }
    return total;
  }
  
  protected double calculateTotalShipCost(final Order order) {
    double totalShipCost = 0.0;
    final NMOrderImpl orderImpl = (NMOrderImpl) order;
    totalShipCost = totalShippingCost(orderImpl.getNmCommerceItems(), NMCommerceItem.isPickupinStore(orderImpl));
    // nmve1 - override the shipping total for international orders
    totalShipCost = orderImpl.isInternationalOrder() ? CheckoutComponents.getFiftyOneUtils().getDomesticShippingCharge() : totalShipCost;
    
    if (isLoggingDebug()) {
      logDebug("calculateTotalShipCost(): " + totalShipCost);
    }
    return totalShipCost;
  }
  
  /**
   * Price the taxes for the entire order
   * 
   * @return the total amount in taxes
   * @param pOrder
   *          the order to price
   * @param pPricingModels
   *          the pricing models used in this pricing
   * @param pLocale
   *          the locale of the user, may be null
   * @param pProfile
   *          the user, may be null
   * @param pExtraParameters
   *          A Map of extra parameters to be used in the pricing, may be null
   * @exception PricingException
   *              if there was an error while computing the pricing information
   */
  @Override
  public double priceTaxForOrderTotal(final Order pOrder, @SuppressWarnings("rawtypes") final Collection pPricingModels, final Locale pLocale, final RepositoryItem pProfile,
          @SuppressWarnings("rawtypes") final Map pExtraParameters, final boolean pGenerateOrderRanges) throws PricingException {
    return priceTaxForOrderTotal(pOrder, pPricingModels, pLocale, pProfile, pExtraParameters);
  }
  
  @Override
  public double priceTaxForOrderTotal(final Order pOrder, final Locale pLocale, final RepositoryItem pProfile, @SuppressWarnings("rawtypes") final Map pExtraParameters) throws PricingException {
    return super.priceTaxForOrderTotal(pOrder, pLocale, pProfile, pExtraParameters);
  }
  
  @Override
  public double priceTaxForOrderTotal(final Order pOrder, final PricingModelHolder pPricingModels, final Locale pLocale, final RepositoryItem pProfile,
          @SuppressWarnings("rawtypes") final Map pExtraParameters) throws PricingException {
    return super.priceTaxForOrderTotal(pOrder, pPricingModels, pLocale, pProfile, pExtraParameters);
  }
  
  @Override
  public double priceTaxForOrderTotal(final Order pOrder, @SuppressWarnings("rawtypes") final Collection pPricingModels, final Locale pLocale, final RepositoryItem pProfile,
          @SuppressWarnings("rawtypes") final Map pExtraParameters) throws PricingException {
    
    synchronized (pOrder) {
      // NOTE: In this case, it is not using the TaxPricingEngine to calculate tax
      // TaxPriceInfo info = getTaxPricingEngine().priceTax(pOrder, pPricingModels, pLocale, pProfile, pExtraParameters);
      final TaxPriceInfo NMTaxInfo = DistributeTaxToCommerceItems(pOrder, pPricingModels, pLocale, pProfile, pExtraParameters);
      
      pOrder.setTaxPriceInfo(NMTaxInfo);
      if (NMTaxInfo != null) {
        return NMTaxInfo.getAmount();
      } else {
        return 0.0;
      }
    }
  }
  
  /**
   * Distribute tax to commerce item level. Note: This method does not use TaxPricingEngine to price tax. So if additional calculators(eg TaxDiscountCalculator) are added, it would not give effect on
   * the calculated tax!
   * 
   * @param pOrder
   *          the order to price
   * @param pPricingModels
   *          the pricing models used in this pricing
   * @param pLocale
   *          the locale of the user, may be null
   * @param pProfile
   *          the user, may be null
   * @param pExtraParameters
   *          A Map of extra parameters to be used in the pricing, may be null
   * @exception PricingException
   *              if there was an error while computing the pricing information
   */
  
  protected TaxPriceInfo DistributeTaxToCommerceItems(final Order pOrder, @SuppressWarnings("rawtypes") final Collection pPricingModels, final Locale pLocale, final RepositoryItem pProfile,
          @SuppressWarnings("rawtypes") final Map pExtraParameters) throws PricingException {
    
    double freightCharges = 0.0;
    long freightLong = 0;
    double giftWrapCharges = 0.0;
    double dutyAmount = 0.0;
    double assemblyDiscount = 0.0;
    double taxAmount = 0.0;
    
    String commodeCode = "";
    String numOfItems;
    // TaxResult result[];
    // Date today;
    TaxResult status;
    double commerceItemTax;
    NMItemPriceInfo iPriceInfo;
    final NMOrderImpl nmOrder = (NMOrderImpl) pOrder;
    
    // register the status' tax price, if applicable
    final TaxPriceInfo taxinfo = new TaxPriceInfo();
    
    // Fetching NM Profile from currrent request
    final DynamoHttpServletRequest request = ServletUtil.getCurrentRequest();
    final NMProfile profile = CheckoutComponents.getProfile(request);
    
    @SuppressWarnings("unchecked")
    final Map<String, TaxPriceInfo> shippingItemsTaxPriceInfos = taxinfo.getShippingItemsTaxPriceInfos();
    
    final List<HardgoodShippingGroup> SGrps = getShippingGroups(pOrder);
    
    if (isLoggingDebug()) {
      logDebug("Shipping Group size: " + SGrps.size());
    }
    
    // sum the status amounts
    double totalAmount = 0.0;
    double totalCityTax = 0.0;
    double totalCountyTax = 0.0;
    double totalStateTax = 0.0;
    double totalDistrictTax = 0.0;
    double totalCountryTax = 0.0;
    
    for (int si = 0; si < SGrps.size(); si++) {
      
      double sgTotalAmount = 0.0;
      double sgTotalCityTax = 0.0;
      double sgTotalCountyTax = 0.0;
      double sgTotalStateTax = 0.0;
      double sgTotalDistrictTax = 0.0;
      double sgTotalCountryTax = 0.0;
      
      final ShippingGroup sg = SGrps.get(si);
      
      final List<ShippingGroupCommerceItemRelationship> relCIs = getCommerceItemRelationships(sg);
      for (final CommerceItemRelationship ci : relCIs) {
        final NMCommerceItem item = (NMCommerceItem) ci.getCommerceItem();
        
        // configure the TaxRequestInfo
        final TaxRequestInfoImpl tri = new TaxRequestInfoImpl();
        
        // set the order id
        tri.setOrderId(pOrder.getId());
        
        /**
         * calculate billing address for all shipping groups ahead of time. billing address may not be available. Try to find the first PaymentGroup of the order that has an address associated with it
         * and use that as this tax calculation's billing address. If no payment group has an address, there's no billing address.
         */
        @SuppressWarnings("unchecked")
        final List<PaymentGroup> paymentGroups = pOrder.getPaymentGroups();
        if (paymentGroups != null) {
          for (final PaymentGroup pg : paymentGroups) {
            if (pg instanceof CreditCard) {
              final Address billingAddress = ((CreditCard) pg).getBillingAddress();
              if (billingAddress != null) {
                tri.setBillingAddress(billingAddress);
                break;
              }
            }
          }
        }
        final List<ShippingDestinationImpl> shippingDestinations = new LinkedList<ShippingDestinationImpl>();
        final ShippingDestinationImpl dest = new ShippingDestinationImpl();
        dest.setCurrencyCode(pOrder.getPriceInfo().getCurrencyCode());
        // dest.setShippingAmount(sg.getPriceInfo().getAmount());
        
        if (item.getPriceInfo() instanceof NMItemPriceInfo) {
          final NMItemPriceInfo pInfo = (NMItemPriceInfo) item.getPriceInfo();
          
          boolean iPhoneFree = false;
          if (nmOrder.is_iPhoneOrder()) {
            final String apc = nmOrder.getActivatedPromoCode();
            if ((apc != null) && !apc.trim().equals("")) {
              final StringTokenizer st = new StringTokenizer(apc, ",");
              while (st.hasMoreElements()) {
                final String theAPC = (String) st.nextElement();
                
                if ((theAPC != null) && theAPC.trim().equalsIgnoreCase("FREEBASESHIPPING")) {
                  iPhoneFree = true;
                }
              }
            }
          }
          
          if (iPhoneFree) {
            freightCharges = 0;
          } else {
            freightCharges = pInfo.getDeliveryAndProcessingTotal(false);
          }
          
          // freightCharges = pInfo.getDeliveryAndProcessingTotal(false);
          assemblyDiscount = pInfo.getAssemblyDiscount();
          
          giftWrapCharges = pInfo.getOtherCharges();
          dutyAmount = pInfo.getDutyAndBrokerageTotal(false);
          
          if (isLoggingDebug()) {
            logDebug("Total line freight: " + freightCharges + " and Giftwrap Charges: " + giftWrapCharges);
          }
          dest.setShippingAmount(freightCharges);
        }
        
        final Address shippingAddress = determineShippingAddress(sg);
        dest.setShippingAddress(shippingAddress);
        
        // extract the destination geocode to be sent to TaxWare
        String geocode = "";
        if (null != shippingAddress) {
          final NMRepositoryContactInfo shipGroupAddress = (NMRepositoryContactInfo) shippingAddress;
          if ((null != shipGroupAddress.getGeoCodeTaxKey()) && (shipGroupAddress.getGeoCodeTaxKey().trim().length() == 9)) {
            geocode = shipGroupAddress.getGeoCodeTaxKey().trim().substring(7);
            if (isLoggingDebug()) {
              logDebug("shipGroupAddress.getGeoCodeTaxKey(): " + shipGroupAddress.getGeoCodeTaxKey());
              logDebug("geocode: " + geocode);
            }
          }
        }
        
        // create a TaxableItem for all the items to be associated with this group
        final List<TaxableItemImpl> taxableItems = new LinkedList<TaxableItemImpl>();
        final TaxableItemImpl ti = new TaxableItemImpl();
        
        // NM Business Requirement: Apply tax on commerce items price, total line freight charges and giftwrap charges.
        
        taxAmount = item.getPriceInfo().getAmount() + giftWrapCharges + dutyAmount + assemblyDiscount;
        
        // just in case, we should round the taxableItem's amount.
        // this price should already be rounded
        
        if (isLoggingDebug()) {
          logDebug("rounding item: " + item.getId() + " amount for taxing: " + taxAmount);
        }
        
        final double roundedAmount = round(taxAmount);
        
        final long roundedAmountLong = new Double(roundedAmount * 100).longValue();
        
        if (isLoggingDebug()) {
          logDebug("rounded item price for taxing to : " + roundedAmount);
        }
        
        ti.setAmount(roundedAmount);
        ti.setCatalogRefId(item.getCatalogRefId());
        ti.setProductId(item.getAuxiliaryData().getProductId());
        
        ti.setQuantity(item.getQuantity());
        taxableItems.add(ti);
        
        // set taxable item amount to be group's taxable subtotal
        // dest.setTaxableItemAmount(item.getPriceInfo().getAmount());
        dest.setTaxableItemAmount(taxAmount);
        
        // set the taxableitems
        TaxableItem[] taxableItemArray = new TaxableItem[taxableItems.size()];
        taxableItemArray = taxableItems.toArray(taxableItemArray);
        dest.setTaxableItems(taxableItemArray);
        
        // if the destination's taxable amount is zero, don't even add it
        if (dest.getTaxableItemAmount() >= 0) {
          shippingDestinations.add(dest);
        }
        
        ShippingDestination[] shippingDestinationArray = new ShippingDestination[shippingDestinations.size()];
        shippingDestinationArray = shippingDestinations.toArray(shippingDestinationArray);
        tri.setShippingDestinations(shippingDestinationArray);
        // if there are no destinations, don't even bother calculating tax
        if (shippingDestinations.size() > 0) {
          double CIAmountTax = 0.0; // Holds the tax for the commerce item amount
          final double CIFreightChargesTax = 0.0; // Holds the tax for the commerce item freight charges
          // Changes to skip tax for Bfx orders. This is for BG and LC.
          final Boolean bfxOrder = (Boolean) request.getSession().getAttribute("bfxOrder");
          // Due to NM Taxation requirement, there will be two calls to the SaleTaxService.
          // - First request is for taxation on the commerce item amount
          // - Second request is for taxation on the commerce item freight charges
          
          // First call to tax service to get tax for commerce item total amount
          if (!CheckoutAPI.isInternationalSession(profile) && (null == bfxOrder)) {
            try {
              commodeCode = (String) ((RepositoryItem) item.getAuxiliaryData().getProductRef()).getPropertyValue("taxwareCategory");
              freightLong = new Double(freightCharges * 100).longValue();
              
              // Need to pass quantity(num of items) to taxware to get proper taxation
              numOfItems = String.valueOf(item.getQuantity());
              
              status = callTaxService(shippingAddress, commodeCode, roundedAmountLong, numOfItems, freightLong, geocode);
              
              CIAmountTax =
                      round(status.getCityTaxAmount() + status.getSecCityTaxAmount() + status.getCountyTaxAmount() + status.getSecCountyTaxAmount() + status.getProvinceTaxAmount()
                              + status.getSecProvinceTaxAmount() + status.getTerritoryTaxAmount() + status.getCountryTaxAmount());
              
              totalCityTax += status.getCityTaxAmount() + status.getSecCityTaxAmount();
              totalCountyTax += status.getCountyTaxAmount() + status.getSecCountyTaxAmount();
              totalStateTax += status.getProvinceTaxAmount() + status.getSecProvinceTaxAmount();
              totalDistrictTax += status.getTerritoryTaxAmount();
              totalCountryTax += status.getCountryTaxAmount();
              // totalAmount += status.getCityTaxAmount() + status.getSecCityTaxAmount() + status.getCountyTaxAmount() +
              // status.getSecCountyTaxAmount() + status.getProvinceTaxAmount() + status.getSecProvinceTaxAmount() +
              // status.getTerritoryTaxAmount() + status.getCountryTaxAmount();
              
              sgTotalCityTax += status.getCityTaxAmount() + status.getSecCityTaxAmount();
              sgTotalCountyTax += status.getCountyTaxAmount() + status.getSecCountyTaxAmount();
              sgTotalStateTax += status.getProvinceTaxAmount() + status.getSecProvinceTaxAmount();
              sgTotalDistrictTax += status.getTerritoryTaxAmount();
              sgTotalCountryTax += status.getCountryTaxAmount();
              sgTotalAmount +=
                      status.getCityTaxAmount() + status.getSecCityTaxAmount() + status.getCountyTaxAmount() + status.getSecCountyTaxAmount() + status.getProvinceTaxAmount()
                              + status.getSecProvinceTaxAmount() + status.getTerritoryTaxAmount() + status.getCountryTaxAmount();
              
              if (isLoggingDebug()) {
                logDebug("*** Tax Results from Taxware for Commerce Item Amount: " + roundedAmountLong + ", D&P: " + freightLong + ".");
                logDebug("Tax CI Amount: "
                        + (status.getCityTaxAmount() + status.getSecCityTaxAmount() + status.getCountyTaxAmount() + status.getSecCountyTaxAmount() + status.getProvinceTaxAmount()
                                + status.getSecProvinceTaxAmount() + status.getTerritoryTaxAmount() + status.getCountryTaxAmount()) + "\nCity Tax: "
                        + (status.getCityTaxAmount() + status.getSecCityTaxAmount()) + "\nCounty Tax: " + (status.getCountyTaxAmount() + status.getSecCountyTaxAmount()) + "\nState Tax: "
                        + (status.getProvinceTaxAmount() + status.getSecProvinceTaxAmount()) + "\nDistrict Tax: " + status.getTerritoryTaxAmount() + "\nCountry Tax: " + status.getCountryTaxAmount());
              }
              
            } catch (final TaxwareCriticalException e1) {
              if (isLoggingError()) {
                logError("TaxwareCriticalException caught in CI Amount Taxation: " + e1 + "<BR>\n");
              }
              throw new PricingException(e1.toString());
            } catch (final TaxwareMinorException e2) {
              final String sMsg = e2.toString();
              if (findMessage != null) {
                if (sMsg.indexOf(findMessage) == -1) {
                  // do nothing this item is not taxable
                }
              } else {
                if (isLoggingError()) {
                  logError("FindMessage not configured in component. Cannot accurately tax.");
                  logError("TaxwareMinorException caught: " + e2 + "<BR>\n");
                }
                throw new PricingException(e2.toString());
              }
            }
          }
          // Second call to tax service to get tax for freight charges on commerce item level
          
          commerceItemTax = CIAmountTax + CIFreightChargesTax;
          
          commerceItemTax = PricingUtils.amtDivisibleByQty(commerceItemTax, item.getQuantity());
          totalAmount += commerceItemTax;
          
          iPriceInfo = (NMItemPriceInfo) item.getPriceInfo();
          iPriceInfo.setTax(commerceItemTax);
          
        }
      } // end of commerce items loop
      
      // create relationship between shipping group and tax price info
      
      final TaxPriceInfo sgtaxinfo = new TaxPriceInfo();
      sgtaxinfo.setCurrencyCode(pOrder.getPriceInfo().getCurrencyCode());
      sgtaxinfo.setAmount(round(sgTotalAmount));
      sgtaxinfo.setCityTax(sgTotalCityTax);
      sgtaxinfo.setCountyTax(sgTotalCountyTax);
      sgtaxinfo.setStateTax(sgTotalStateTax);
      sgtaxinfo.setDistrictTax(sgTotalDistrictTax);
      sgtaxinfo.setCountryTax(sgTotalCountryTax);
      
      shippingItemsTaxPriceInfos.put(sg.getId(), sgtaxinfo);
      
      if (isLoggingDebug()) {
        logDebug("*** Total Tax Results for SG from Taxware: ***");
        logDebug("Tax Amount: " + sgTotalAmount + "\nCity Tax: " + sgTotalCityTax + "\nCounty Tax: " + sgTotalCountyTax + "\nState Tax: " + sgTotalStateTax + "\nDistrict Tax: " + sgTotalDistrictTax
                + "\nCountry Tax: " + sgTotalCountryTax);
      }
      
    } // end of shipping groups loop
    
    if (isLoggingDebug()) {
      logDebug("Total Tax of all commerce items: " + totalAmount);
    }
    
    final double rounded = round(totalAmount);
    
    if (isLoggingDebug()) {
      logDebug("rounded status amount to: " + rounded);
    }
    
    taxinfo.setCurrencyCode(pOrder.getPriceInfo().getCurrencyCode());
    taxinfo.setAmount(rounded);
    taxinfo.setCityTax(totalCityTax);
    taxinfo.setCountyTax(totalCountyTax);
    taxinfo.setStateTax(totalStateTax);
    taxinfo.setDistrictTax(totalDistrictTax);
    taxinfo.setCountryTax(totalCountryTax);
    return taxinfo;
  }
  
  /**
   * Call to SalesTaxService to get the sales tax for each commerce items
   * 
   * @param shippingAddress
   *          Shipping Address
   * @param commodeCode
   *          Commodity Code
   * @param amount
   *          the amount being taxed through sales tax service
   * @return TaxResult[0]
   */
  protected TaxResult callTaxService(final Address shippingAddress, final String commodeCode, final long amount, final String numOfItems, final String geocode) throws TaxwareCriticalException,
          TaxwareMinorException {
    return callTaxService(shippingAddress, commodeCode, amount, numOfItems, 0, geocode);
  }
  
  protected TaxResult callTaxService(final Address shippingAddress, final String commodeCode, final long amount, String numOfItems, final long dpAmount, final String geocode)
          throws TaxwareCriticalException, TaxwareMinorException {
    TaxRequest taxRequest[];
    TaxResult result[];
    
    if (isLoggingDebug()) {
      logDebug("Tax Request Array being created");
    }
    taxRequest = new TaxRequest[1];
    if (isLoggingDebug()) {
      logDebug("Tax Request Array created successfully");
    }
    result = null;
    if (isLoggingDebug()) {
      logDebug("Tax Result being initialized");
    }
    final Date today = getCurrentDate().getTimeAsDate();
    
    if (isLoggingDebug()) {
      logDebug("Create Request about to be called");
      logDebug("Country: " + shippingAddress.getCountry());
      logDebug("City: " + shippingAddress.getCity());
      logDebug("State: " + shippingAddress.getState());
      logDebug("Zip: " + shippingAddress.getPostalCode());
      logDebug("Date: " + today);
      logDebug("Price (LONG): " + amount);
      logDebug("CommodityServiceCode: " + commodeCode);
    }
    
    // taxware does not recognize NL, must use NF instead
    if ((shippingAddress.getCountry() != null) && (shippingAddress.getState() != null) && shippingAddress.getCountry().equals("CA") && shippingAddress.getState().equals("NL")) {
      taxRequest[0] = mSalesTaxService.createRequest(shippingAddress.getCountry(), shippingAddress.getCity(), "NF", shippingAddress.getPostalCode(), "", "", "", "", CURRENCY_CODE, 0, 0, 0, today);
    } else {
      taxRequest[0] =
              mSalesTaxService.createRequest(shippingAddress.getCountry(), shippingAddress.getCity(), shippingAddress.getState(), shippingAddress.getPostalCode(), "", "", "", "", CURRENCY_CODE, 0, 0,
                      0, today);
    }
    if (dpAmount > 0) {
      taxRequest[0].setFreight(dpAmount);
    }
    taxRequest[0].setPrice(amount);
    if (commodeCode != null) {
      taxRequest[0].setCommodityServiceCode(commodeCode);
    } else {
      if (isLoggingError()) {
        logError("CommodityServiceCode is null, not setting commodity service code then.");
      }
    }
    
    // We need to pass the number of items to taxware to allow exemptions done correctly
    // It has to pass in a string of size 7, with the additional space filled up with zeros, ie: "1" ==> "0000001"
    while (numOfItems.length() < 7) {
      numOfItems = '0' + numOfItems;
    }
    
    taxRequest[0].setFieldValue("NUMITEMS", numOfItems);
    taxRequest[0].setFieldValue("TAXSELPARM", "3");
    taxRequest[0].setFieldValue("CALCULATIONMODE", "G");
    taxRequest[0].setFieldValue("POT", "D");
    
    // set geocode
    taxRequest[0].setDstGeoCode(geocode);
    if (isLoggingDebug()) {
      logDebug("callTaxService geocode: " + geocode);
    }
    
    /*
     * NMRepositoryContactInfo addr = (NMRepositoryContactInfo)shippingAddress; String countyCode = addr.getCountyCode();
     * 
     * if (countyCode !=null && (! countyCode.trim().equals("")) ) { if(isLoggingDebug()) logDebug("the county code is populated and being passed to taxware--->"+countyCode);
     * taxRequest[0].setFieldValue("DSTCOUNTYCODE", countyCode); }
     */
    if (isLoggingDebug()) {
      taxRequest[0].setFieldValue("AUDFILEIND", "1");
    }
    if (isLoggingDebug()) {
      logDebug("Calling Taxware .......");
    }
    
    if (isLoggingDebug()) {
      logDebug("trying to dump the taxware request we are sending");
      taxRequest[0].dumpFields();
      logDebug("*************************done with request******************************************************************************");
    }
    result = mSalesTaxService.calculateSalesTax(taxRequest);
    if (isLoggingDebug()) {
      logDebug("trying to dump the taxware RESULT we recieved");
      result[0].dumpFields();
      logDebug("*************************done with result******************************************************************************");
    }
    
    if (isLoggingDebug()) {
      logDebug("Calling Taxware Success .......");
    }
    
    if (isLoggingDebugCanada() && (shippingAddress.getCountry() != null) && shippingAddress.getCountry().equals("CA")) {
      final StringBuffer sb = new StringBuffer();
      sb.append("province: " + shippingAddress.getState());
      sb.append("; amount taxed: " + amount);
      sb.append("; country tax rate: " + result[0].getCountryTaxRate());
      sb.append("; country tax amount: " + result[0].getCountryTaxAmount());
      sb.append("; province tax rate: " + result[0].getProvinceTaxRate());
      sb.append("; province tax amount: " + result[0].getProvinceTaxAmount());
      logDebug(sb.toString());
    }
    
    if (isLoggingDebug()) {
      final StringBuffer sb = new StringBuffer();
      // sb.append("county: " + countyCode);
      sb.append("; state: " + shippingAddress.getState());
      sb.append("; postal code: " + shippingAddress.getPostalCode());
      sb.append("; amount taxed: (LONG)" + amount);
      sb.append("; city tax rate: " + result[0].getCityTaxRate());
      sb.append("; city tax amount: " + result[0].getCityTaxAmount());
      sb.append("; county tax amount: " + result[0].getCountyTaxAmount());
      sb.append("; county tax rate: " + result[0].getCountyTaxRate());
      logDebug(sb.toString());
    }
    
    return result[0];
  }
  
  /**
   * Determines a shipping address based on a ShippingGroup
   * 
   * @param pGroup
   *          shipping group
   * @return shipping address
   */
  protected Address determineShippingAddress(final ShippingGroup pGroup) {
    if (pGroup instanceof HardgoodShippingGroup) {
      return ((HardgoodShippingGroup) pGroup).getShippingAddress();
    } else {
      return null;
    }
  } // end determineShippingAddress
  
  protected RepositoryItem findCurrentChart(final List<NMCommerceItem> commerceItems) {
    return findCurrentChart(commerceItems, null, 0);
  }
  
  /**
   * Look up for the latest chart with highest priority
   * 
   * @param commerceItems
   *          all the commerce items in the order
   * @param activatedPromoCode
   *          comma-delimited list of activated promo codes, to see if the order has a free shipping promotion
   * @param merchandiseTotal
   *          if the merchandise total (actually the total for which shipping is not free) is > 0 but the order is shoprunner, don't return free chart
   * @return chart repository item
   */
  protected RepositoryItem findCurrentChart(final List<NMCommerceItem> commerceItems, final String activatedPromoCode, final double merchandiseTotal) {
    if (commerceItems != null) {
      
      // begin check to see if we should use free shipping table
      boolean giveFreeBaseShipping = false;
      boolean isAtLeastOneSL2 = false;
      final SystemSpecs systemSpecs = (SystemSpecs) Nucleus.getGlobalNucleus().resolveName(SYSTEM_SPECS_PATH);
      
      for (final NMCommerceItem cItem : commerceItems) {
        if (cItem.getServicelevel().equalsIgnoreCase(ServiceLevel.SL2_SERVICE_LEVEL_TYPE)) {
          isAtLeastOneSL2 = true;
        }
      }
      
      // System.out.println("***NMPT*** giveFreeBaseShipping is"+giveFreeBaseShipping);
      // System.out.println("***NMPT*** activatedPromoCode listing is"+activatedPromoCode);
      
      if (activatedPromoCode != null) {
        if (activatedPromoCode.toUpperCase().indexOf("FREEBASESHIPPING") != -1) {
          giveFreeBaseShipping = true;
          // System.out.println("***NMPT*** giveFreeBaseShipping is now"+giveFreeBaseShipping);
        } else if ((activatedPromoCode.toUpperCase().indexOf("SHOPRUNNER") != -1) && isAtLeastOneSL2 && (merchandiseTotal == 0)) {
          giveFreeBaseShipping = true;
        }
      }
      
      if (giveFreeBaseShipping) {
        try {
          final RepositoryItem chart = mChartRepository.getItem("FREE", "chart");
          
          if (chart != null) {
            // System.out.println("***NMPT*** chart is not null");
            return chart;
          } else {
            throw new Exception("***NMPT*** free chart not found");
          }
        } catch (final Exception e) {
          // do nothing, if for whatever reason we fail trying to use
          // the free shipping table just go ahead and try to process
          // shipping the "normal" way
        }
      }
      // end check to see if we should use free shipping table
      
      RepositoryItem curProdItem = null;
      RepositoryItem curChartItem = null;
      for (final CommerceItemImpl tmpItem : commerceItems) {
        final RepositoryItem tmpProdItem = (RepositoryItem) tmpItem.getAuxiliaryData().getProductRef();
        
        if ((curProdItem == null) || isHigherPriority(curProdItem, tmpProdItem)) {
          final ShippingGroupRelationship sgr = getShippingGroupRelationship(tmpItem);
          final String countryCode = ((HardgoodShippingGroup) sgr.getShippingGroup()).getShippingAddress().getCountry();
          curProdItem = tmpProdItem;
          @SuppressWarnings("unchecked")
          final Map<String, RepositoryItem> countryMap = (Map<String, RepositoryItem>) curProdItem.getPropertyValue("countryMap");
          
          final RepositoryItem curProdCountryItem = countryMap.get(countryCode);
          if (curProdCountryItem != null) {
            curChartItem = (RepositoryItem) curProdCountryItem.getPropertyValue("chartId");
          } else {
            curChartItem = (RepositoryItem) curProdItem.getPropertyValue("chartId");
          }
        }
      }
      if (curChartItem != null) {
        return curChartItem;
      }
    } else {
      if (isLoggingError()) {
        logError("In findCurrentChart(), commerceItems is null!");
      }
    }
    return null;
  }
  
  /**
   * Look up for the latest chart with highest priority
   * 
   * @param commerceItems
   *          all the commerce items in the order
   * @return chart repository item
   */
  
  protected boolean isHigherPriority(final RepositoryItem currentProdItem, final RepositoryItem tempProdItem) {
    final String srcCatType = (String) currentProdItem.getPropertyValue("catalogType");
    final String desCatType = (String) tempProdItem.getPropertyValue("catalogType");
    final Timestamp srcDate = (Timestamp) currentProdItem.getPropertyValue("startDate");
    final Timestamp desDate = (Timestamp) tempProdItem.getPropertyValue("startDate");
    
    if (srcCatType == null) {
      return false;
    }
    // if (srcCatType == null)
    // return false;
    
    if (getExtraShippingData() != null) {
      final Properties priorityMap = getExtraShippingData().getPriorityMap();
      if (priorityMap != null) {
        final int i = priorityMap.getProperty(srcCatType).compareTo(priorityMap.getProperty(desCatType));
        if (i == 0) {
          if (srcDate.before(desDate)) {
            return true;
          }
        } else if (i > 0) {
          return true;
        }
      } else {
        if (isLoggingError()) {
          logError("PriorityMap property is null, please set it on /com/nm/commerce/pricing/ExtraShippingData");
        }
      }
    } else {
      if (isLoggingError()) {
        logError("/com/nm/commerce/pricing/ExtraShippingData is null");
      }
    }
    return false;
  }
  
  /**
   * Find the base shipping cost
   * 
   * @return the total shipping cost
   * @param chartItem
   *          the chart item to look up the price of shipping charges
   * @param price
   *          the price to be searched from the chart
   */
  
  protected double findBaseShippingCost(final RepositoryItem chartItem, final double price) {
    // Assuming valid chartItem and price is returned
    @SuppressWarnings("unchecked")
    final Set<RepositoryItem> chartElements = (Set<RepositoryItem>) chartItem.getPropertyValue("shipElements");
    if (chartElements != null) {
      for (final RepositoryItem element : chartElements) {
        final double lowerLimit = ((Double) element.getPropertyValue("lowerLimit")).doubleValue();
        final double upperLimit = ((Double) element.getPropertyValue("upperLimit")).doubleValue();
        final Boolean percentFlag = (Boolean) element.getPropertyValue("isPercent");
        final Double chargeAmount = (Double) element.getPropertyValue("chargeAmount");
        if ((lowerLimit <= price) && (upperLimit >= price)) {
          if (percentFlag.booleanValue()) {
            return (chargeAmount.doubleValue() * price) / 100;
          } else {
            return chargeAmount.doubleValue();
          }
        }
      }
      if (isLoggingError()) {
        logError("Error in finding Shipping Cost from Chart Elements");
      }
      return 0;
      
    } else {
      if (isLoggingError()) {
        logError("Property ChartElement is null");
      }
      return 0;
    }
  }
  
  protected double calculateNonContinentalShippingCharge(final List<HardgoodShippingGroup> shippingGroups) {
    double amt = 0.0;
    boolean isNonContinentalUSState = false;
    if (isLoggingDebug()) {
      logDebug("*** 1. in calculateNonContinentalShippingCharge***");
      logDebug("There are " + shippingGroups.size() + " involved");
    }
    
    for (final HardgoodShippingGroup rShip : shippingGroups) {
      final NMRepositoryContactInfo addr = (NMRepositoryContactInfo) rShip.getShippingAddress();
      isNonContinentalUSState = false;
      final String state = addr.getState();
      final String country = addr.getCountry();
      
      if (isLoggingDebug()) {
        logDebug("*** state is ***" + state);
      }
      
      if ((state != null) && isThisShippingGroupUsed(rShip) && (country != null) && country.equals("US")) {
        for (final String ncState : getExtraShippingData().getNonContinentalUSStates()) {
          if (ncState.equals(state)) {
            isNonContinentalUSState = true;
          } // if
        } // for
        
        if (isNonContinentalUSState && !shippingGroupExemptFromNonContinentalCharge(rShip)) {
          if (isLoggingDebug()) {
            logDebug("the amount to be charged is " + amt + getExtraShippingData().getExtraNonContinentalUSAddressCharge());
          }
          amt = amt + getExtraShippingData().getExtraNonContinentalUSAddressCharge();
        }
      }
    }// end while rshipIterator.hasNext()
    return amt;
  }
  
  /**
   * This method determines if a shipping group should be not be counted when calculating non continental US shipping charges.
   */
  public boolean shippingGroupExemptFromNonContinentalCharge(final HardgoodShippingGroup shipGroup) {
    final List<ShippingGroupCommerceItemRelationship> ciRels = getCommerceItemRelationships(shipGroup);
    
    // if at least one product in this shipping group is physically shipped (ie not a virtual gift card)
    // then this ship group is NOT exempt from the non continental ship charge...
    
    boolean containsPhysicallyShippedProduct = false;
    for (final Relationship rel : ciRels) {
      if ((rel instanceof ShippingGroupCommerceItemRelationship) && new String("shippingGroupCommerceItem").equals(rel.getRelationshipClassType())) {
        
        final String merchType = (String) ((RepositoryItem) ((ShippingGroupCommerceItemRelationship) rel).getCommerceItem().getAuxiliaryData().getProductRef()).getPropertyValue("merchandiseType");
        
        if ((merchType == null) || !merchType.equals(GiftCardHolder.VIRTUAL_GIFT_CARD_MERCH_TYPE)) {
          containsPhysicallyShippedProduct = true;
          break;
        }
      }
    }
    
    if (containsPhysicallyShippedProduct) {
      return false;
    } else {
      return true;
    }
  }
  
  /**
   * This method is used to identify whether the shipping group is PICK UP AT store if so , shipping group should be not be counted when calculating shipping charges. This returns boolean based on the
   * value store_no from the nm_ship_addr_aux for that shipping group THIS METHOD TO BE USED WITH THE HardgoodShippingGroup LEVEL
   */
  public boolean isShippingGroupPickUpAtStore(final HardgoodShippingGroup shipGroup) {
    // List<ShippingGroupCommerceItemRelationship> ciRels = getCommerceItemRelationships(shipGroup);
    // Iterator<ShippingGroupCommerceItemRelationship> ciRelsIterator = ciRels.iterator();
    // Relationship rel = null;
    
    // if at least one product in this shipping group is physically shipped (ie not a virtual gift card)
    // then this ship group is NOT exempt from the non continental ship charge...
    
    boolean containsShippedGroupWithStoreNum = false;
    
    // REMOVED LOOPING - data from relationships was not used.
    // while (ciRelsIterator.hasNext()) {
    // ShippingGroupCommerceItemRelationship rShip = (ShippingGroupCommerceItemRelationship) ciRelsIterator.next();
    final String strStoreNum = fetchPickupStoreNumber(shipGroup.getId());
    
    if (isLoggingDebug()) {
      logDebug("For Shipping Group ID : " + shipGroup.getId() + " store_no :" + strStoreNum);
    }
    
    if ((strStoreNum != null) && (strStoreNum.length() > 0)) {
      containsShippedGroupWithStoreNum = true;
      // break;
    }
    // }
    
    return containsShippedGroupWithStoreNum;
  }
  
  // Get the store number from the nm_ship_addr_aux table for the corresponding SG
  
  public String fetchStoreNumberforShipGroup(final HardgoodShippingGroup shipGroup) {
    // List<ShippingGroupCommerceItemRelationship> ciRels = getCommerceItemRelationships(shipGroup);
    // Iterator<ShippingGroupCommerceItemRelationship> ciRelsIterator = ciRels.iterator();
    // Relationship rel = null;
    
    // if at least one product in this shipping group is physically shipped (ie not a virtual gift card)
    // then this ship group is NOT exempt from the non continental ship charge...
    
    String strStoreNumber = "";
    
    // REMOVED LOOPING - data from relationships was not used.
    // while (ciRelsIterator.hasNext()) {
    // ShippingGroupCommerceItemRelationship rShip = (ShippingGroupCommerceItemRelationship) ciRelsIterator.next();
    final String strStoreNum = fetchPickupStoreNumber(shipGroup.getId());
    
    if (isLoggingDebug()) {
      logDebug("NMPricingTools.fetchStoreNumberforShipGroup() For Shipping Group ID : " + shipGroup.getId() + " store_no :" + strStoreNum);
    }
    
    if ((strStoreNum != null) && (strStoreNum.length() > 0)) {
      strStoreNumber = strStoreNum;
      // break;
    }
    // }
    
    return strStoreNumber;
  }
  
  // Get the store name
  
  public String fetchStoreNameforShipGroup(final HardgoodShippingGroup shipGroup) {
    // List<ShippingGroupCommerceItemRelationship> ciRels = getCommerceItemRelationships(shipGroup);
    // Iterator<ShippingGroupCommerceItemRelationship> ciRelsIterator = ciRels.iterator();
    // Relationship rel = null;
    
    // if at least one product in this shipping group is physically shipped (ie not a virtual gift card)
    // then this ship group is NOT exempt from the non continental ship charge...
    
    String strStoreName = "";
    
    // REMOVED LOOPING - data from relationships was not used.
    // while (ciRelsIterator.hasNext()) {
    // ShippingGroupCommerceItemRelationship rShip = (ShippingGroupCommerceItemRelationship) ciRelsIterator.next();
    final String strStoreNum = fetchPickupStoreNumber(shipGroup.getId());
    
    if (isLoggingDebug()) {
      logDebug(">>>NMPricingTools.fetchStoreNameforShipGroup()" + shipGroup.getId() + " store_no :" + strStoreNum);
    }
    
    if ((strStoreNum != null) && (strStoreNum.length() > 0)) {
      strStoreName = fetchPickupStoreName(strStoreNum);
      // break;
    }
    // }
    
    return strStoreName;
  }
  
  public void updateStoreNumberforShipGroup(final String shippingGroupID, final String strStoreNum) {
    try {
      final String strViewName = "hardgoodShippingGroup";
      final MutableRepository orderRepos = (MutableRepository) RepositoryUtils.getInstance().getOrderRepository();
      final MutableRepositoryItem ri = (MutableRepositoryItem) orderRepos.getItem(shippingGroupID, strViewName);
      ri.setPropertyValue("strStoreNum", strStoreNum);
      
    } catch (final Exception e) {
      e.printStackTrace();
    }
  }
  
  public String fetchPickupStoreNumber(final String shippingGroupID) {
    String strReturn = null;
    
    try {
      final String strViewName = "hardgoodShippingGroup";
      
      final MutableRepository orderRepos = (MutableRepository) RepositoryUtils.getInstance().getOrderRepository();
      
      final MutableRepositoryItem ri = (MutableRepositoryItem) orderRepos.getItem(shippingGroupID, strViewName);
      
      if (ri != null) {
        strReturn = (String) ri.getPropertyValue("strStoreNum");
      } else {
        if (isLoggingDebug()) {
          logDebug("NMPricingTools.fetchPickupStoreNumber() - Store # null for Shipping Group ID " + shippingGroupID);
        }
      }
      
      if ((strReturn != null) && (strReturn.length() > 0)) {
        if (isLoggingDebug()) {
          logDebug("NMPricingTools.fetchPickupStoreNumber() - Store # for Shipping Group ID " + strReturn);
        }
      }
      
    } catch (final Exception e) {
      e.printStackTrace();
    }
    return strReturn;
  }
  
  // Store Name based on the SG ID
  // If the store name doesn't exist -Return null
  // Else return the shortName , if its null then return the Long name
  // This should dip into nm_store_lcation table
  
  public String fetchPickupStoreName(final String storeId) {
    String strShortStoreName = null;
    String strFullStoreName = null;
    String strReturn = null;
    try {
      final Repository repository = (Repository) resolveName("/nm/xml/StoreInfoRepository");
      final RepositoryView view = repository.getView("storeLocation");
      final RqlStatement statement = RqlStatement.parseRqlStatement("storeNum = ?0");
      final Object params[] = new Object[1];
      params[0] = storeId;
      final RepositoryItem[] results = statement.executeQuery(view, params);
      
      if ((results != null) && (results.length > 0)) {
        strShortStoreName = (String) results[0].getPropertyValue("shortName");
        strFullStoreName = (String) results[0].getPropertyValue("name");
        
        if (isLoggingDebug()) {
          logDebug("NMPricingTools.fetchPickupStoreName() strShortStoreName:" + strShortStoreName);
          logDebug("NMPricingTools.fetchPickupStoreName() strFullStoreName :" + strFullStoreName);
        }
        
      }
      
      if (StringUtilities.isNotEmpty(strShortStoreName)) {
        strReturn = strShortStoreName;
      } else {
        strReturn = strFullStoreName;
      }
      
    } catch (final Exception e) {
      e.printStackTrace();
    }
    return strReturn;
  }
  
  // Added by INSSK for Request # 28736 on Jun 21, 2011 - Starts here
  /**
   * This method will get the store details for the given store number
   * 
   * @param storeId
   * @return
   */
  public RepositoryItem[] fetchPickupStoreDetails(final String storeNumber) throws RepositoryException {
    final Repository repository = (Repository) resolveName("/nm/xml/StoreInfoRepository");
    final RepositoryView view = repository.getView("storeLocation");
    final RqlStatement statement = RqlStatement.parseRqlStatement("storeNum = ?0");
    final Object params[] = new Object[1];
    params[0] = storeNumber;
    final RepositoryItem[] stores = statement.executeQuery(view, params);
    
    return stores;
  }
  
  // Added by INSSK for Request # 28736 on Jun 21, 2011 - Ends here
  
  /**
   * This method will determine if the shippinggroup relationship is actually valid for the currently selected shipping group. This method does not seem to 'break' the relationships that we were
   * seeing before. Helper Method to determine if a shipping group is actually being used. I consider a shipping group used if it relationship quantity is greater than 0
   * 
   * @return boolean
   * @param a
   *          shippingGroup
   */
  
  protected boolean isThisShippingGroupUsed(final HardgoodShippingGroup group) {
    long quantity = 0;
    final List<ShippingGroupCommerceItemRelationship> commerceItemRelationships = getCommerceItemRelationships(group);
    
    for (final ShippingGroupCommerceItemRelationship rShip : commerceItemRelationships) {
      if (rShip.getQuantity() > 0) {
        if (isLoggingDebug()) {
          logDebug("ShippingGroupCommerceItemRelationship Id: " + rShip.getId() + " quantity is: " + rShip.getQuantity() + " for Shipping Group:" + group.getId());
        }
        quantity = quantity + 1;
      }
    }
    if (quantity > 0) {
      return true;
    } else {
      return false;
    }
  }
  
  /**
   * Distribute the expedition charges to commerce items level based on NM shipping Algorithm
   * 
   * @param pShippingGroup
   *          the shipping group within an order
   * @param amount
   *          the amount of the expedition charges
   */
  protected void distributeExpeditionCharge(final ShippingGroup pShippingGroup, final double amount) {
    final List<ShippingGroupCommerceItemRelationship> relCIs = getCommerceItemRelationships(pShippingGroup);
    double first_half = amount / 2.0;
    
    first_half = round(first_half);
    final double second_half = amount - first_half;
    if (relCIs.size() != 0) {
      final Vector<CommerceItem> commerceItems = new Vector<CommerceItem>();
      for (final CommerceItemRelationship ci : relCIs) {
        final CommerceItem cItem = ci.getCommerceItem();
        commerceItems.addElement(cItem);
      }
      
      if (commerceItems.size() > 1) {
        NMItemPriceInfo iPriceInfo;
        NMCommerceItem ci;
        ci = (NMCommerceItem) commerceItems.get(0);
        iPriceInfo = (NMItemPriceInfo) ci.getPriceInfo();
        double freight;
        freight = iPriceInfo.getFreightCharges();
        freight = freight + first_half;
        freight = PricingUtils.amtDivisibleByQty(freight, ci.getQuantity());
        iPriceInfo.setFreightCharges(freight);
        ci = (NMCommerceItem) commerceItems.get(commerceItems.size() - 1);
        iPriceInfo = (NMItemPriceInfo) ci.getPriceInfo();
        freight = iPriceInfo.getFreightCharges();
        freight = freight + second_half;
        freight = PricingUtils.amtDivisibleByQty(freight, ci.getQuantity());
        iPriceInfo.setFreightCharges(freight);
      } else {
        NMItemPriceInfo iPriceInfo;
        final NMCommerceItem ci = (NMCommerceItem) commerceItems.get(0);
        iPriceInfo = (NMItemPriceInfo) ci.getPriceInfo();
        double freight = iPriceInfo.getFreightCharges();
        freight = freight + amount;
        freight = PricingUtils.amtDivisibleByQty(freight, ci.getQuantity());
        iPriceInfo.setFreightCharges(freight);
      }
    }
  }
  
  /**
   * Calculate Merchandise Total
   * 
   * @return the total price of all line items without parenthetical and not expedited shipping
   * @param commerceItems
   *          the list of commerce items within an order
   */
  protected double calculateMerchandiseTotal(final List<NMCommerceItem> commerceItems, final boolean ignoreBOPSItems, final boolean ignoreSL2Items) {
    final BrandSpecs brandSpecs = (BrandSpecs) Nucleus.getGlobalNucleus().resolveName(BRAND_SPECS_PATH);
    double sum = 0.0;
    if (commerceItems != null) {
      for (final NMCommerceItem item : commerceItems) {
        final RepositoryItem prodItem = (RepositoryItem) item.getAuxiliaryData().getProductRef();
        
        if (isLoggingDebug()) {
          logDebug("Product items in CalculateMechandiseTotal(): " + prodItem);
        }
        
        if (ignoreBOPSItems) {
          final boolean isBOPSItem = StringUtilities.isNotEmpty(item.getPickupStoreNo()) && !item.getShipToStoreFlg();
          
          if (isBOPSItem) {
            continue;
          }
        }
        
        if (ignoreSL2Items) {
          if ((item.getServicelevel() != null) && item.getServicelevel().equals(ServiceLevel.SL2_SERVICE_LEVEL_TYPE)) {
            continue;
          }
        }
        
        if (skipShiptoStoreItems(item)) {
        	continue;
        }
        
        final long size = item.getQuantity();
        boolean isShipToUS = true;
        boolean parenthetical = false;
        @SuppressWarnings("unchecked")
        final List<ShippingGroupRelationship> sgRels = item.getShippingGroupRelationships();
        String countryCode = INMGenericConstants.EMPTY_STRING;
        if (sgRels.size() < 1) {
          logError("Commerce item " + item.getId() + " has no shipping group relationships");
        } else {
          final ShippingGroupRelationship sgr = sgRels.get(0);
          if (null != sgr) {
            if (null != ((HardgoodShippingGroup) sgr.getShippingGroup())) {
              if (null != ((HardgoodShippingGroup) sgr.getShippingGroup()).getShippingAddress()) {
                countryCode = ((HardgoodShippingGroup) sgr.getShippingGroup()).getShippingAddress().getCountry();
              } else {
                if (isLoggingDebug()) {
                  logDebug("Commerce item " + item.getId() + " has no shipping Address for shipping group id " + ((HardgoodShippingGroup) sgr.getShippingGroup()).getId());
                }
              }
            } else {
              if (isLoggingDebug()) {
                logDebug("Commerce item " + item.getId() + " has no shipping groups");
              }
            }
          } else {
            if (isLoggingDebug()) {
              logDebug("shipping Group relationship size is " + sgRels.size() + "Commerce item " + item.getId() + " has no shipping group relationships");
            }
          }
          parenthetical = prodCountryUtils.getRelevantParentheticalFlag(prodItem, countryCode);
          if ((countryCode != null) && !countryCode.equals("US")) {
            isShipToUS = false;
          }
        }
        
        Double price = null;
        
        /** pricing for shipping costs should be based off the CI price, in the case of promos */
        
        if (!parenthetical) {
          price = new Double(item.getPriceInfo().getAmount());
          // SmartPost : Adding service level code dynamically in the below condition based on amount and shipping country
          if (brandSpecs.getEnableFlatRateShipping() && !item.getServicelevel().equalsIgnoreCase(CheckoutComponents.getServiceLevelArray().determineFreeShippingServiceLevel()) && isShipToUS) {
            // Do not add item price when it has expedited shipping service on flat rate shipping to US address
          } else {
            sum = sum + price.doubleValue();
          }
        }
        
        if (isLoggingDebug()) {
          logDebug("Commerce Item: " + item + ", quantity: " + size + ", price: " + price);
        }
      }
    } else {
      if (isLoggingError()) {
        logError("Commerce Items is null!");
      }
    }
    return sum;
  }
  
  /**
   * Calculate Parenthetical Total for line items.
   * 
   * @return the total of parenthetical charges of line items
   * @param commerceItems
   *          the list of commerce items within an order
   */
  
  protected double calculateParentheticalTotal(final List<NMCommerceItem> commerceItems) {
    double sum = 0.0;
    if (commerceItems != null) {
      for (final NMCommerceItem item : commerceItems) {
        final RepositoryItem prodItem = (RepositoryItem) item.getAuxiliaryData().getProductRef();
        
        if (isLoggingDebug()) {
          logDebug("Product items in CalculateParentheticalTotal(): " + prodItem + ", ParentheticalPromo " + item.getParentheticalPromo() + ".");
        }
        
        if (item.getParentheticalPromo()) {
          continue;
        }
        
        final Double itemQuantity = new Double(item.getQuantity());
        final ShippingGroupRelationship sgr = getShippingGroupRelationshipForRepricing(item);
        final String countryCode = (sgr != null) ? ((HardgoodShippingGroup) sgr.getShippingGroup()).getShippingAddress().getCountry() : null;
        final double parentheticalCharge = prodCountryUtils.getRelevantParentheticalCharge(prodItem, countryCode);
        
        final Double parentheticalAmount = new Double(itemQuantity.doubleValue() * parentheticalCharge);
        
        sum = sum + parentheticalAmount.doubleValue();
      }
    } else {
      if (isLoggingError()) {
        logError("Commerce Items is null!");
      }
    }
    return sum;
  }
  
  /**
   * Calculate Assembly Discount Total for line items.
   * 
   * @return the total of assembly discount of line items
   * @param commerceItems
   *          the list of commerce items within an order
   */
  
  protected double calculateAssemblyDiscountTotal(final Order pOrder) {
    
    double totalAmountOfRestrictedAssemblyProducts = 0.0;
    double assemblyDiscount = 0.0;
    final List<HardgoodShippingGroup> shippingGroups = getShippingGroups(pOrder);
    final ArrayList<CommerceItem> productsWithAssemblyRestriction = new ArrayList<CommerceItem>();
    
    final String activatedPromoCode = ((NMOrderImpl) pOrder).getActivatedPromoCode();
    
    // Iterate through the shipping groups of the order
    
    final ArrayList<NMCommerceItem> commerceItems = new ArrayList<NMCommerceItem>();
    
    for (final ShippingGroup shippingGroup : shippingGroups) {
      
      if (shippingGroup instanceof HardgoodShippingGroup) {
        final HardgoodShippingGroup hardgoodShippingGroup = (HardgoodShippingGroup) shippingGroup;
        
        final String country = hardgoodShippingGroup.getShippingAddress().getCountry();
        final String state = hardgoodShippingGroup.getShippingAddress().getState();
        final String zipCode = hardgoodShippingGroup.getShippingAddress().getPostalCode();

        
        if ((country != null) && (state != null)) {
          if (!country.equalsIgnoreCase("US")) {
            continue;
          }
          
          final List<ShippingGroupCommerceItemRelationship> commerceItemRelationships = getCommerceItemRelationships(hardgoodShippingGroup);
          
          // Iterate through the items within the shipping group.
          for (final CommerceItemRelationship commerceItemRelationship : commerceItemRelationships) {
            final CommerceItem commerceItem = commerceItemRelationship.getCommerceItem();
            final NMCommerceItem nmci = (NMCommerceItem) commerceItem;
            commerceItems.add(nmci);
            final RepositoryItem prodRI = (RepositoryItem) nmci.getAuxiliaryData().getProductRef();
            final ItemPriceInfo priceInfo = commerceItem.getPriceInfo();
            
            final String productId = commerceItem.getAuxiliaryData().getProductId();
            
            final NMProduct product = new NMProduct(productId);
            
            // Get the restriction codes for this product based on the
            // ship to country
            final List<NMRestrictionCode> restrictionCodes = product.getRestrictionCodes(country);
            
            // Iterate through the restriction codes for this product/country combo.
            boolean hasAssemblyRestriction = false;
            double parentheticalCharge = 0.0;
            for (final NMRestrictionCode restrictionCode : restrictionCodes) {
              final String code = restrictionCode.getRestrictionCode();
              
              if ((country != null) && !StringUtils.isBlank(state) && !StringUtils.isBlank(zipCode)) {
                if (code.equalsIgnoreCase(NMRestrictionCode.ASEEMBLY_CODE)) {
                  if (prodCountryUtils.isProductShippableToCountry(prodRI, country) && prodCountryUtils.isBeingShippedToRestrictedState(state) && prodCountryUtils.isBeingShippedToRestrictedZipCode(zipCode,state) ) {
                    hasAssemblyRestriction = true;
                    parentheticalCharge = prodCountryUtils.getRelevantParentheticalCharge(prodRI, country);
                    if (parentheticalCharge > 0.0) {
                      productsWithAssemblyRestriction.add(commerceItem);
                      if (isLoggingDebug()) {
				          logDebug("Restriction Enabled for ZipCode is" + zipCode+"and State is "+state+"parentheticalCharge is "+parentheticalCharge);
				        }
                    }
                  }
                }
              }
            }
            if (hasAssemblyRestriction && (parentheticalCharge > 0.0)) {
              totalAmountOfRestrictedAssemblyProducts += priceInfo.getRawTotalPrice();
            }
            hasAssemblyRestriction = false;
          }
        }
      }
    }
    
    if (totalAmountOfRestrictedAssemblyProducts > 0.0) {
      if (activatedPromoCode != null) {
        if (activatedPromoCode.toUpperCase().indexOf("FREEBASESHIPPING") == -1) {
          assemblyDiscount = findAssemblyDiscountOnChart(totalAmountOfRestrictedAssemblyProducts);
          distributeAssemblyDiscountAmongItems(productsWithAssemblyRestriction, assemblyDiscount, totalAmountOfRestrictedAssemblyProducts);
        } else { // If FREEBASESHIPPING promo was applied, then we need to clear out the assembly discounts on the items.
          for (final NMCommerceItem nmci : commerceItems) {
            final NMItemPriceInfo iPriceInfo = (NMItemPriceInfo) nmci.getPriceInfo();
            iPriceInfo.setAssemblyDiscount(0.0);
          }
        }
      } else {
        assemblyDiscount = findAssemblyDiscountOnChart(totalAmountOfRestrictedAssemblyProducts);
        distributeAssemblyDiscountAmongItems(productsWithAssemblyRestriction, assemblyDiscount, totalAmountOfRestrictedAssemblyProducts);
      }
    }
    return assemblyDiscount;
    
  }
  
  protected double findAssemblyDiscountOnChart(final double totalAmountOfRestrictedAssemblyProducts) {
    RepositoryItem[] results = null;
    RqlStatement statement = null;
    Object params[] = null;
    Double chargeAmount = new Double(0.0);
    
    try {
      final RepositoryView shipChart = mChartRepository.getView("chart");
      statement = RqlStatement.parseRqlStatement("code contains ?0");
      params = new Object[1];
      params[0] = "WG";
      
      results = statement.executeQuery(shipChart, params);
      if ((results != null) && (results.length > 0)) {
        @SuppressWarnings("unchecked")
        final Set<RepositoryItem> elementSets = (Set<RepositoryItem>) results[0].getPropertyValue("shipElements");
        final RepositoryItem currentElement = getCurrentElement(elementSets, new Double(totalAmountOfRestrictedAssemblyProducts));
        if (currentElement != null) {
          chargeAmount = (Double) currentElement.getPropertyValue("chargeAmount");
        }
      }
    } catch (final Exception e) {
      e.printStackTrace();
    }
    
    return chargeAmount.doubleValue();
  }
  
  protected void distributeAssemblyDiscountAmongItems(final ArrayList<CommerceItem> mProductsWithAssemblyRestriction, final double assemblyDiscount, final double totalCostOfAssemblyProducts) {
    double percentage = 0.0;
    double individualAmount = 0.0;
    
    for (final CommerceItem commerceItem : mProductsWithAssemblyRestriction) {
      final ItemPriceInfo priceInfo = commerceItem.getPriceInfo();
      percentage = round(priceInfo.getRawTotalPrice() / totalCostOfAssemblyProducts);
      individualAmount = round(assemblyDiscount * percentage);
      final NMItemPriceInfo nmPriceInfo = (NMItemPriceInfo) priceInfo;
      nmPriceInfo.setAssemblyDiscount(individualAmount);
    }
    
  }
  
  /**
   * Distribute the total base shipping cost to commerce items level based on NM shipping Algorithm This function takes the totalBaseShippingCost(includes base shipping cost and any other extra
   * charges to shipping) and spreads it out among the commerceItems. If a parenthetical is detected that cost should go on the commerce item that it belongs to.
   * 
   * @param totalBaseShippingCost
   *          the total of base shipping cost
   * @param commerceItems
   *          the list of commerce items within an order
   */
  protected void distributingTotalBaseShippingCost(final double totalBaseShippingCost, final List<NMCommerceItem> commerceItems, final boolean ignoreSL2Items) {
    double totalLeft = totalBaseShippingCost;
    
    final Set<NMCommerceItem> baseShippingMap = new HashSet<NMCommerceItem>();
    final Set<NMCommerceItem> nonBaseShippingMap = new HashSet<NMCommerceItem>();
    
    if (commerceItems != null) {
      BrandSpecs brandSpecs = CommonComponentHelper.getBrandSpecs();
      // first pass to add parentheticals to the correct commerce items
      // add priceInfos to appropriate map
      for (final NMCommerceItem item : commerceItems) {
        double ciShippingCost = 0d;
        final NMItemPriceInfo iPriceInfo = (NMItemPriceInfo) item.getPriceInfo();
        ciShippingCost = getParentheticalCostForCommerceItem(item);
        
        totalLeft = totalLeft - ciShippingCost;
        
        // if an item is "Ship to Store" ,do not consider it for distributing base shipping charge
        if (skipShiptoStoreItems(item)) {
          continue;
        }
        
        iPriceInfo.setFreightCharges(ciShippingCost);
      }
      
      // 2nd pass add base shipping from the charts
      // each iteration it determines the shipping cost for the commerce item. Base shipping cost cannot
      // exceed a certain amount. So if the current commerce item being evaluated causes the total base shipping to
      // exceed the maximum amount, it reduces it.
      for (final NMCommerceItem item : commerceItems) {
        
        // if an item is "Ship to Store" ,do not consider it for distributing base shipping charge
        if (skipShiptoStoreItems(item)) {
          continue;
        }
        
        // if an item is a BOPS do not consider it for distributing base shipping charge
        if (StringUtilities.isNotEmpty(item.getPickupStoreNo()) && !item.getShipToStoreFlg()) {
          continue;
        }
        
        // if we are ignoring shop runner items that are SL2, and this item is shipping SL2, do not consider it for distributing base shipping charge
        if (ignoreSL2Items && item.getServicelevel().equals(ServiceLevel.SL2_SERVICE_LEVEL_TYPE)) {
          continue;
        }
        
        final RepositoryItem prodItem = (RepositoryItem) item.getAuxiliaryData().getProductRef();
        double ciShippingCost = 0d;
        
        if (isLoggingDebug()) {
          logDebug("Product items in DistributingTotalBaseShippingCost(): " + prodItem);
        }
        
        final NMItemPriceInfo iPriceInfo = (NMItemPriceInfo) item.getPriceInfo();
        
        // if parenthetical is Y, never add base shipping to this commerce item
        // if parenthetical is N, add base shipping + parenthetical
        boolean isShipToUS = true;
        final ShippingGroupRelationship sgr = getShippingGroupRelationship(item);
        final String countryCode = ((HardgoodShippingGroup) sgr.getShippingGroup()).getShippingAddress().getCountry();
        if ((countryCode != null) && !countryCode.equals("US")) {
          isShipToUS = false;
        }
        final boolean parentheticalFlag = prodCountryUtils.getRelevantParentheticalFlag(prodItem, countryCode);
        if (parentheticalFlag) {
          
          if (isLoggingDebug()) {
            logDebug("Paren is Y and current Freight Cost--->" + iPriceInfo.getFreightCharges() + "<-----");
          }
          if (!item.getParentheticalPromo()) {
            nonBaseShippingMap.add(item);
          }
        } else {
          final String itemServiceLevel = item.getServicelevel();
          // SmartPost : Adding service level code dynamically in the below condition based on amount and shipping country
          if (brandSpecs.getEnableFlatRateShipping() && !itemServiceLevel.equalsIgnoreCase(CheckoutComponents.getServiceLevelArray().determineFreeShippingServiceLevel()) && isShipToUS) {
            // Do not include base shipping cost for items if flat rate shipping is enabled and it is using expedited
            // shipping to US address
          } else {
            ciShippingCost = getBaseShippingCostForCommerceItem(item);
          }
          
          if (isLoggingDebug()) {
            logDebug("Paren is N and ciShippingCost--->" + ciShippingCost + "<-----");
          }
          if (isLoggingDebug()) {
            logDebug("Totalleft - ciShippingCost---->" + (totalLeft - ciShippingCost) + "<------");
          }
          
          if ((totalLeft - ciShippingCost) >= 0d) {
            double freightCharges = iPriceInfo.getFreightCharges() + ciShippingCost;
            
            freightCharges = PricingUtils.amtDivisibleByQty(freightCharges, item.getQuantity());
            ciShippingCost = freightCharges - iPriceInfo.getFreightCharges();
            iPriceInfo.setFreightCharges(freightCharges);
            totalLeft = totalLeft - ciShippingCost;
          }
          baseShippingMap.add(item);
        }
      }
      
      // distribute the rest of the shipping evenly among all the items excepting base shipping
      // spread the left over shipping charges over the items with parenthetical != Y
      if ((totalLeft > 0d) && (baseShippingMap.size() > 0)) {
        final double distLeftOverAmountForEachItem = totalLeft / baseShippingMap.size();
        final double roundedDistLeftOverAmountForEachItem = new BigDecimal(distLeftOverAmountForEachItem).setScale(2, BigDecimal.ROUND_DOWN).doubleValue();
        
        if (isLoggingDebug()) {
          logDebug("distrubing over baseShippingItems--->" + roundedDistLeftOverAmountForEachItem + "<-----");
        }
        
        for (final NMCommerceItem item : baseShippingMap) {
          if (totalLeft >= roundedDistLeftOverAmountForEachItem) {
            totalLeft = totalLeft - addAmountToFreight(item, roundedDistLeftOverAmountForEachItem);
          } else {
            totalLeft = totalLeft - addAmountToFreight(item, totalLeft);
          }
        }
      } else if (nonBaseShippingMap.size() > 0) { // all items in the cart are Parenthetical = Y, spread the left
        // over shipping charges
        final double distLeftOverAmountForEachItem = totalLeft / nonBaseShippingMap.size();
        
        final double roundedDistLeftOverAmountForEachItem = new BigDecimal(distLeftOverAmountForEachItem).setScale(2, BigDecimal.ROUND_UP).doubleValue();
        
        if (isLoggingDebug()) {
          logDebug("distrubing over nonBaseShippingItems--->" + roundedDistLeftOverAmountForEachItem + "<-----");
        }
        
        for (final NMCommerceItem item : nonBaseShippingMap) {
          if (totalLeft >= roundedDistLeftOverAmountForEachItem) {
            totalLeft = totalLeft - addAmountToFreight(item, roundedDistLeftOverAmountForEachItem);
          } else {
            totalLeft = totalLeft - addAmountToFreight(item, totalLeft);
          }
        }
      }
      
      final int amtLeft = (int) ((totalLeft + .005) * 100.0);
      if (amtLeft > 0) {
        if (isLoggingDebug()) {
          logDebug("Abandoned shipping amount due to WR19790: " + (amtLeft / 100.0));
        }
      }
    } else {
      if (isLoggingError()) {
        logError("In distributingTotalBaseShippingCost() -- Commerce Items is null!");
      }
    }
  }
  
  private double addAmountToFreight(final NMCommerceItem item, final double amt) {
    final NMItemPriceInfo price = (NMItemPriceInfo) item.getPriceInfo();
    double newFreight = price.getFreightCharges() + amt;
    newFreight = PricingUtils.amtDivisibleByQty(newFreight, item.getQuantity());
    final double usedAmt = newFreight - price.getFreightCharges();
    price.setFreightCharges(newFreight);
    return usedAmt;
  }
  
  public double getParentheticalCostForCommerceItem(final NMCommerceItem commerceItem) {
    final RepositoryItem prodItem = (RepositoryItem) commerceItem.getAuxiliaryData().getProductRef();
    final ShippingGroupRelationship sgr = getShippingGroupRelationship(commerceItem);
    final String countryCode = ((HardgoodShippingGroup) sgr.getShippingGroup()).getShippingAddress().getCountry();
    final double parentheticalCharge = prodCountryUtils.getRelevantParentheticalCharge(prodItem, countryCode);
    double parentheticalCost = 0d;
    
    if (!commerceItem.getParentheticalPromo()) {
      if (parentheticalCharge > 0d) {
        parentheticalCost = parentheticalCharge * commerceItem.getQuantity();
      }
    }
    
    return parentheticalCost;
  }
  
  public double getBaseShippingCostForCommerceItem(final NMCommerceItem commerceItem) {
    final ShippingGroupRelationship sgr = getShippingGroupRelationship(commerceItem);
    final String countryCode = ((HardgoodShippingGroup) sgr.getShippingGroup()).getShippingAddress().getCountry();
    final RepositoryItem prodItem = (RepositoryItem) commerceItem.getAuxiliaryData().getProductRef();
    
    @SuppressWarnings("unchecked")
    final Map<String, RepositoryItem> countryMap = (Map<String, RepositoryItem>) prodItem.getPropertyValue("countryMap");
    final RepositoryItem prodCountryItem = countryMap.get(countryCode);
    RepositoryItem chartItem = null;
    
    if (prodCountryItem != null) {
      chartItem = (RepositoryItem) prodCountryItem.getPropertyValue("chartId");
    }
    
    if (null == chartItem) {
      chartItem = (RepositoryItem) prodItem.getPropertyValue("chartId");
    }
    
    if (null == chartItem) {
      logError("Unable to determine shipping chart for product " + prodItem.getRepositoryId() + " commerce item " + commerceItem.getId());
      return 0.0;
    }
    
    // shipping price should be based off adjusted price from promos
    final Double itemPrice = new Double(commerceItem.getPriceInfo().getAmount());
    // Double itemPrice = (Double)prodItem.getPropertyValue("retailPrice");
    double baseShippingCost = 0;
    
    @SuppressWarnings("unchecked")
    final Set<RepositoryItem> elementSets = (Set<RepositoryItem>) chartItem.getPropertyValue("shipElements");
    final RepositoryItem currentElement = getCurrentElement(elementSets, itemPrice);
    
    if (currentElement != null) {
      final Boolean percentFlag = (Boolean) currentElement.getPropertyValue("isPercent");
      final Double chargeAmount = (Double) currentElement.getPropertyValue("chargeAmount");
      if (percentFlag.booleanValue()) {
        baseShippingCost = (itemPrice.doubleValue() * chargeAmount.doubleValue()) / 100;
      } else {
        baseShippingCost = chargeAmount.doubleValue();
      }
      if (baseShippingCost < 0d) {
        baseShippingCost = 0d;
      }
    } else {
      System.err.println("Chart Item is not associated with this product, rep ID: " + prodItem.getRepositoryId() + "!");
    }
    // System.out.println("returning a base shipping cost of : "+baseShippingCost);
    return baseShippingCost;
  }
  
  public RepositoryItem getCurrentElement(final Set<RepositoryItem> elementSets, final Double itemPrice) {
    for (final RepositoryItem elementItem : elementSets) {
      final Double lowerLimit = (Double) elementItem.getPropertyValue("lowerLimit");
      final Double upperLimit = (Double) elementItem.getPropertyValue("upperLimit");
      
      if ((itemPrice.compareTo(lowerLimit) >= 0) && (itemPrice.compareTo(upperLimit) <= 0)) {
        return elementItem;
      }
    }
    return null;
  }
  
  
  private double determineExtraAddressCharge(final Order order) {
    double theExtraAddressCharge = 0;
    
    try {
      // look at the order to see if it has a extra address promo on it so we use the free price
     final String promoCode = PromoUtils.getGivenPromoForOrder(order, NMPromotionArray.EXTRA_ADDRESS_ARRAY);
      if (StringUtilities.isNotBlank(promoCode)) {
        theExtraAddressCharge = 0;
      } else {
        theExtraAddressCharge = retrieveCountryExtraAddressCharge(order);
        
      }
      
    } catch (final Exception e) {
      if (isLoggingError()) {
        logError("A Exception error occurred NMPricingTools.determineExtraAddressCharge", e);
      }
      
      theExtraAddressCharge = retrieveCountryExtraAddressCharge(order);
    }
    
    return theExtraAddressCharge;
  } // end method
  
  private double retrieveCountryExtraAddressCharge(final Order order) {
    double theExtraAddressCharge = 0;
    Double extraChargeValue = new Double(0);
    String country = "";
    
    final List<HardgoodShippingGroup> shippingGroups = getShippingGroups(order);
    for (final ShippingGroup shippingGroup : shippingGroups) {
      if (shippingGroup instanceof HardgoodShippingGroup) {
        final HardgoodShippingGroup hardgoodShippingGroup = (HardgoodShippingGroup) shippingGroup;
        country = hardgoodShippingGroup.getShippingAddress().getCountry();
      }
    }
    
    if ((country != null) && (country.trim().length() > 0)) {
      final Repository orderRepository = getOrderTools().getOrderRepository();
      try {
        final RepositoryItem rShippingCharge = orderRepository.getItem(country, "extraShippingCharges");
        
        if (rShippingCharge != null) {
          extraChargeValue = (Double) rShippingCharge.getPropertyValue("extraAddressCharge");
        }
        if (extraChargeValue == null) {
          theExtraAddressCharge = 0;
        } else {
          theExtraAddressCharge = extraChargeValue.doubleValue();
        }
      } catch (final RepositoryException e) {
        theExtraAddressCharge = 0;
        logError("Exception in NMPricingTools.retrieveCountryExtraAddressCharge:" + e);
      }
      
    } else {
      theExtraAddressCharge = 0;
    }
    
    return theExtraAddressCharge;
  }
  
  // convenience method for typing shipping groups
  private List<HardgoodShippingGroup> getShippingGroups(final Order order) {
    @SuppressWarnings("unchecked")
    final List<HardgoodShippingGroup> groups = order.getShippingGroups();
    return groups;
  }
  
  // convenience method for typing shipping relationships
  private List<ShippingGroupCommerceItemRelationship> getCommerceItemRelationships(final ShippingGroup group) {
    @SuppressWarnings("unchecked")
    final List<ShippingGroupCommerceItemRelationship> relationships = group.getCommerceItemRelationships();
    return relationships;
  }
  
  // convenience method for typing shipping groups
  private List<Relationship> getRelationships(final Order order) {
    @SuppressWarnings("unchecked")
    final List<Relationship> relationships = order.getRelationships();
    return relationships;
  }
  
  public ExtraShippingCharge getExtraShippingCharge() {
    if (mExtraShippingCharge == null) {
      mExtraShippingCharge = new ExtraShippingCharge();
      return mExtraShippingCharge;
    } else {
      return mExtraShippingCharge;
    }
  }
  
  public void setExtraShippingCharge(final ExtraShippingCharge pExtraShippingCharge) {
    mExtraShippingCharge = pExtraShippingCharge;
  }
  
  public Date getPromoBeginDate() {
    return this.promoBeginDate;
  }
  
  public void setPromoBeginDate(final Date promoBeginDate) {
    this.promoBeginDate = promoBeginDate;
  }
  
  public Date getPromoEndDate() {
    return this.promoEndDate;
  }
  
  public void setPromoEndDate(final Date promoEndDate) {
    this.promoEndDate = promoEndDate;
  }
  
  public double getPromoTotal() {
    return this.promoTotal;
  }
  
  public void setPromoTotal(final double promoTotal) {
    this.promoTotal = promoTotal;
  }
  
  public SalesTaxService getSalesTaxService() {
    return mSalesTaxService;
  }
  
  public void setSalesTaxService(final SalesTaxService pSalesTaxService) {
    mSalesTaxService = pSalesTaxService;
  }
  
  public String getFindMessage() {
    return findMessage;
  }
  
  public void setFindMessage(final String str) {
    findMessage = str;
  }
  
  public CurrentDate getCurrentDate() {
    return mCurrentDate;
  }
  
  public void setCurrentDate(final CurrentDate pCurrentDate) {
    mCurrentDate = pCurrentDate;
  }
  
  public String getShippingCommodityCode() {
    return mShippingCommodityCode;
  }
  
  public void setShippingCommodityCode(final String pShippingCommodityCode) {
    mShippingCommodityCode = pShippingCommodityCode;
  }
  
  public Repository getChartRepository() {
    return mChartRepository;
  }
  
  public void setChartRepository(final Repository pChartRepository) {
    mChartRepository = pChartRepository;
  }
  
  public ExtraShippingData getExtraShippingData() {
    return mExtraShippingData;
  }
  
  public void setExtraShippingData(final ExtraShippingData pExtraShippingData) {
    mExtraShippingData = pExtraShippingData;
  }
  
  @Override
  public OrderTools getOrderTools() {
    return mOrderTools;
  }
  
  @Override
  public void setOrderTools(final OrderTools pOrderTools) {
    mOrderTools = pOrderTools;
  }
  
  public void setTaxProcessor(final TaxProcessor pTaxProcessor) {
    mTaxProcessor = pTaxProcessor;
  }
  
  public TaxProcessor getTaxProcessor() {
    return mTaxProcessor;
  }
  
  public boolean isLoggingDebugCanada() {
    return loggingDebugCanada;
  }
  
  public void setLoggingDebugCanada(final boolean loggingDebugCanada) {
    this.loggingDebugCanada = loggingDebugCanada;
  }
  
  public ProdCountryUtils getProdCountryUtils() {
    return prodCountryUtils;
  }
  
  public void setProdCountryUtils(final ProdCountryUtils prodCountryUtils) {
    this.prodCountryUtils = prodCountryUtils;
  }
  
  /**
   * This class helps with the distribution of Shipping Group level restriction charges to the commerce item within the shipping group
   * 
   * @author nmwps
   */
  private static class ShippingGroupRestrictionCodeCharges {
    private NMRestrictionCode mRestrictionCode = null;
    private final ArrayList<CommerceItem> mCommerceItems = new ArrayList<CommerceItem>();
    
    public ShippingGroupRestrictionCodeCharges(final NMRestrictionCode restrictionCode) {
      mRestrictionCode = restrictionCode;
    }
    
    // public NMRestrictionCode getRestrictionCode() {
    // return mRestrictionCode;
    // }
    
    public void applyCharges(final NMOrderPriceInfo orderPriceInfo) {
      final String code = mRestrictionCode.getRestrictionCode();
      final double surchargeAmount = mRestrictionCode.getSurchargeAmount();
      final double count = mCommerceItems.size();
      final double itemCharge = round(surchargeAmount / count);
      double totalCharge = 0;
      
      for (final CommerceItem commerceItem : mCommerceItems) {
        final NMItemPriceInfo priceInfo = (NMItemPriceInfo) commerceItem.getPriceInfo();
        priceInfo.addItemRestrictionCharge(code, itemCharge);
        orderPriceInfo.accumulateItemRestrictionCharges(code, itemCharge);
        totalCharge += itemCharge;
      }
      
      final double roundingError = round(surchargeAmount - totalCharge);
      
      if ((Math.abs(roundingError) >= .01) && (mCommerceItems.size() > 0)) {
        final CommerceItem commerceItem = mCommerceItems.get(0);
        final NMItemPriceInfo priceInfo = (NMItemPriceInfo) commerceItem.getPriceInfo();
        priceInfo.addItemRestrictionCharge(code, itemCharge + roundingError);
        orderPriceInfo.accumulateItemRestrictionCharges(code, roundingError);
      }
    }
    
    private double round(final double pNumber) {
      return new Long(Math.round(pNumber * Math.pow(10, 2))).doubleValue() / Math.pow(10, 2);
    }
    
    public void addCommerceItem(final CommerceItem commerceItem) {
      mCommerceItems.add(commerceItem);
    }
    
    // public void clear() {
    // mCommerceItems.clear();
    // }
  }
  
  /**
   * This method override ATG rounding in case of international order so that price precision is kept to minimize the chance of API call response price mismatch with display price because of rounding
   * multiple times In case it is a domestic order, the default ATG implementation is followed
   */
  @Override
  public double round(final double pNumber) {
    final LocalizationUtils localizationUtils = (LocalizationUtils) Nucleus.getGlobalNucleus().resolveName("/nm/utils/LocalizationUtils");
    final NMProfile profile = localizationUtils.getProfile();
    // if international user, try send full precision to resolve rounding issue with BF
    if ((profile != null) && localizationUtils.isSupportedByFiftyOne(profile.getCountryPreference())) {
      return round(pNumber, internationalPriceScale);
    } else { // ATG default rounding rule, used for domestice cart
      return super.round(pNumber);
    }
  }
  
  public int getInternationalPriceScale() {
    return internationalPriceScale;
  }
  
  public void setInternationalPriceScale(final int internationalPriceScale) {
    this.internationalPriceScale = internationalPriceScale;
  }
  
  public boolean skipShiptoStoreItems(final NMCommerceItem item) {
    
    boolean skipShiptoStoreItems = false;
    // When an item is selected for "Ship To Store " do not consider it for
    // shipping price calculation
    BrandSpecs brandSpecs = CommonComponentHelper.getBrandSpecs();
    if (brandSpecs.isBOPSEnabled() && (brandSpecs.isS2sServiceLevelUpgradeEnabled() || brandSpecs.isBOSSFreeShippingEnaled()) && !StringUtils.isEmpty(item.getPickupStoreNo()) && item.getShipToStoreFlg()) {
      skipShiptoStoreItems = true;
    }
    return skipShiptoStoreItems;
  }
  
} // end of class

