/*
 * <ATGCOPYRIGHT> Copyright (C) 1997-2001 Art Technology Group, Inc. All Rights Reserved. No use, copying or distribution ofthis work may be made except in accordance with a valid license agreement
 * from Art Technology Group. This notice must be included on all copies, modifications and derivatives of this work.
 * 
 * Art Technology Group (ATG) MAKES NO REPRESENTATIONS OR WARRANTIES ABOUT THE SUITABILITY OF THE SOFTWARE, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, OR NON-INFRINGEMENT. ATG SHALL NOT BE LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR DISTRIBUTING THIS SOFTWARE OR
 * ITS DERIVATIVES.
 * 
 * "Dynamo" is a trademark of Art Technology Group, Inc. </ATGCOPYRIGHT>
 */

package com.nm.commerce.pricing;

import static com.nm.common.INMGenericConstants.DOUBLE_ZERO;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;

import atg.commerce.order.CommerceItemRelationship;
import atg.commerce.order.HardgoodShippingGroup;
import atg.commerce.order.Order;
import atg.commerce.order.ShippingGroup;
import atg.commerce.pricing.PricingException;
import atg.commerce.pricing.ShippingCalculatorImpl;
import atg.commerce.pricing.ShippingPriceInfo;
import atg.repository.RepositoryException;
import atg.repository.RepositoryItem;
import atg.servlet.ServletUtil;

import com.nm.collections.ServiceLevel;
import com.nm.collections.ServiceLevelArray;
import com.nm.commerce.NMCommerceItem;
import com.nm.commerce.NMRepositoryContactInfo;
import com.nm.commerce.checkout.CheckoutAPI;
import com.nm.commerce.checkout.CheckoutComponents;
import com.nm.common.SmartPostServiceLevelSessionBean;
import com.nm.estimateddeliverydate.session.EstimatedDeliveryDateSession;
import com.nm.estimateddeliverydate.vo.EstimatedDeliveryDateCmosShippingVO;
import com.nm.utils.NmoUtils;
import com.nm.utils.StringUtilities;

/**
 * A shipping calculator that sets the shipping amount
 * <P>
 * If the property <code>addAmount</code> is true then instead of setting the price quote amount to the value of the <code>amount</code> property, the calculator adds the amount to the current amount
 * in the price quote. This can be used to configure a "surcharge" calculator, which increases the shipping price.
 * 
 * <P>
 * 
 * The <code>shippingMethod</code> property should be set to the name of a particular delivery process. For example: UPS Ground, UPS 2-day or UPS Next Day.
 * 
 * <P>
 * 
 * If the <code>ignoreShippingMethod</code> property is true, then this calculator does not expose a shipping method name (through getAvailableMethods). In addition this calculator will always attempt
 * to perform pricing. This option is available if the user is not given a choice of different shipping methods.
 * 
 * @beaninfo description: A shipping calculator that sets the shipping amount to a fixed price. attribute: componentCategory Pricing Calculators
 * 
 * @author Sunil Bindal
 * 
 */

public class NMShippingCalculator extends ShippingCalculatorImpl {
  
  // -------------------------------------
  // Constants
  // -------------------------------------
  
  // -------------------------------------
  // Member Variables
  // -------------------------------------
  
  // -------------------------------------
  // Properties
  // -------------------------------------
  ExtraShippingData mExtraData;
  
  public ExtraShippingData getExtraShippingData() {
    return mExtraData;
  }
  
  public void setExtraShippingData(final ExtraShippingData pExtraShippingData) {
    mExtraData = pExtraShippingData;
  }
  
  private ServiceLevelArray serviceLevelArray;
  
  // -------------------------------------
  // Constructors
  // -------------------------------------
  
  public ServiceLevelArray getServiceLevelArray() {
    return serviceLevelArray;
  }
  
  public void setServiceLevelArray(final ServiceLevelArray serviceLevelArray) {
    this.serviceLevelArray = serviceLevelArray;
  }
  
  // -------------------------------------
  // Constructors
  // -------------------------------------
  
  /**
   * Constructs an instanceof FixedPriceShippingCalculator
   */
  public NMShippingCalculator() {}
  
  /**
   * Returns the amount which should be used as the price for this shipping group
   * 
   * @param pPriceQuote
   *          the price of the input shipping group
   * @param pShippingGroup
   *          the shipping group for which an amount is needed
   * @param pPricingModel
   *          a discount which could affect the shipping group's price
   * @param pLocale
   *          the locale in which the price is calculated
   * @param pProfile
   *          the profile of the person for whom the amount in being generated.
   * @param pExtraParameters
   *          any extra parameters that might affect the amount calculation
   * @return the amount for pricing the input pShippingGroup
   * @exception PricingException
   *              if there is a problem getting the amount (price) for the input shipping group
   */
  
  @Override
  protected double getAmount(final Order pOrder, final ShippingPriceInfo pPriceQuote, final ShippingGroup pShippingGroup, final RepositoryItem pPricingModel, final Locale pLocale,
          final RepositoryItem pProfile, final Map pExtraParameters) throws PricingException {
    return getAmount(pPriceQuote, pShippingGroup, pPricingModel, pLocale, pProfile, pExtraParameters);
  }
  
  @Override
  protected double getAmount(final ShippingPriceInfo pPriceQuote, final ShippingGroup pShippingGroup, final RepositoryItem pPricingModel, final Locale pLocale, final RepositoryItem pProfile,
          final Map pExtraParameters) throws PricingException {
    double amt = 0.0;
    final SmartPostServiceLevelSessionBean smartPostServiceLevelSessionBean = CheckoutComponents.getSmartPostServiceLevelSessionBean(ServletUtil.getCurrentRequest());
    final String selectedServiceLevel = smartPostServiceLevelSessionBean.getSmartPostSelectedServiceLevel();
    // String shipMethod = pShippingGroup.getShippingMethod();
    
    // Added implementation to get service level code from the commerce items level.
    // NM Business Requirement: If different shipping methods are used for the same shipping group,
    // we need to add up all the charges of the shipping methods chosen within the shipping group.
    
    final Vector SLVector = new Vector();
    final NMPricingTools pricingTools = (NMPricingTools) getPricingTools();
    final List relCIs = pShippingGroup.getCommerceItemRelationships();
    
    for (int i = 0; i < relCIs.size(); i++) {
      final CommerceItemRelationship ci = (CommerceItemRelationship) relCIs.get(i);
      final NMCommerceItem citem = (NMCommerceItem) ci.getCommerceItem();
      
      if (pricingTools.skipShiptoStoreItems(citem)) {
        return 0.0;// The current shipping group represents 'Ship To Store'. Hence there will be no shipping charges;
      }
      
      // If ServiceLevel method has not been added to the vector, this section will add SL into the vector
      if (StringUtilities.isEmpty(selectedServiceLevel)) {
        SLVector.addElement(CheckoutComponents.getServiceLevelArray().determineFreeShippingServiceLevel());
      } else if (!SLVector.contains(citem.getServicelevel())) {
        SLVector.addElement(citem.getServicelevel());
      }
    }
    
    // If the shipping address is in AK, HI or PR and a service level of next day or second day has been chosen,
    // a shipping surcharge needs to be applied. Pull state information from shipping group and compare to
    // property containing list of states that require the shipping surcharge. If state is contained in the list,
    // apply the charge.
    
    final HardgoodShippingGroup hardgoodShipGroup = (HardgoodShippingGroup) pShippingGroup;
    final NMRepositoryContactInfo addr = (NMRepositoryContactInfo) hardgoodShipGroup.getShippingAddress();
    final String state = addr.getState();
    final String country = addr.getCountry();
    
    // Loop through the vector of SLs and return the total charges for the chosen shipping methods
    // of each shipping group (unique address)
    for (int j = 0; j < SLVector.size(); j++) {
      final String serviceLevel = (String) SLVector.get(j);
      final boolean sddFreeShipping = (Boolean) pExtraParameters.get("SDDFreeShipping");
      amt += ServiceLevelLookup(serviceLevel, country, sddFreeShipping);
      
      final List extraShippingChargeStates = Arrays.asList(getExtraShippingData().getExtraShippingChargeStates());
      if ((extraShippingChargeStates.contains(state) && country.equals("US"))
              && (serviceLevel.equalsIgnoreCase(ServiceLevel.SL1_SERVICE_LEVEL_TYPE) || serviceLevel.equalsIgnoreCase(ServiceLevel.SL2_SERVICE_LEVEL_TYPE))) {
        // Remove extraNonContinentalUSAddressCharge that was added in the Standard shipping cost
        amt -= getExtraShippingData().getExtraNonContinentalUSAddressCharge();
        // Add surcharge for shipping SL1 or SL2 to AK, HI or PR
        amt += getExtraShippingData().getExtraShippingCharge();
      }
    }
    
    if (amt < 0.0) {
      amt = 0.0;
    }
    
    if (isLoggingDebug()) {
      logDebug("Debug: Service Level Charges and Extra Shipping Surcharges Returned is : " + amt);
    }
    
    return amt;
  }
  
  /**
   * Returns the amount of the service level charges from Service Level Repository given the service level method.
   * 
   * @param shipMethod
   *          the given ship method
   * @param countryCode
   *          the country code of the ShippingGroup
   * @param sddFreeShipping
   *          flag indicating that order sub total qualifies user for free shipping
   * @return the amount for the service level charges
   * @exception RepositoryException
   *              if there is a problem getting item from repository
   */
  protected double ServiceLevelLookup(final String shipMethod, final String countryCode, final boolean sddFreeShipping) {
    double amt = 0.0;
    // SL2 shipping should be free for ShopRunner Users
    final boolean isShopRunner =
            (shipMethod != null) && (countryCode != null) && shipMethod.equals(ServiceLevel.SL2_SERVICE_LEVEL_TYPE) && countryCode.equalsIgnoreCase("US") && CheckoutAPI.isShopRunnerEnabled()
                    && (CheckoutAPI.getShopRunnerToken(ServletUtil.getCurrentRequest()) != null);
    // SL0 shipping should be free if flag is set
    final boolean isSameDayDelivery = (shipMethod != null) && (countryCode != null) && shipMethod.equals(ServiceLevel.SL0_SERVICE_LEVEL_TYPE) && countryCode.equalsIgnoreCase("US") && sddFreeShipping;
    
    if (isShopRunner || isSameDayDelivery) {
      return amt;
    } else {
      try {
        EstimatedDeliveryDateSession estimatedDeliveryDateSession = CheckoutComponents.getEstimatedDeliveryDateSession(ServletUtil.getCurrentRequest());
        EstimatedDeliveryDateCmosShippingVO estimatedDeliveryDateCmosShippingVO = estimatedDeliveryDateSession.getEstimatedDeliveryDateCmosShippingVO();
        if (estimatedDeliveryDateSession.isExcludeServiceLevelsFromDisplay()) {
          // In case of order contains only dropship or only virtual gift card then the service levels are not displayed in order review page and hence the service level amount will be zero
          amt = DOUBLE_ZERO;
        } else if (null != estimatedDeliveryDateCmosShippingVO && NmoUtils.isNotEmptyCollection(estimatedDeliveryDateCmosShippingVO.getServiceLevels())) {
          for (ServiceLevel servicelevel : CheckoutAPI.convertCmosServiceLevelResponseToServiceLevelObject(estimatedDeliveryDateCmosShippingVO.getServiceLevels())) {
            if (shipMethod.equalsIgnoreCase(servicelevel.getCode())) {
              amt = servicelevel.getAmount();
              break;
            }
            // if the shipMethod code is not present in cmos response then the default selected Service level code's amount will be sent as the default selected Service level will be selected in order
            // review page
            if (servicelevel.isSelected()) {
              amt = servicelevel.getAmount();
            }
          }
        } else {
          amt = serviceLevelArray.getServiceLevelAmount(shipMethod, countryCode);
        }
      } catch (final RepositoryException e) {
        logError(e);
      }
      
    }
    return amt;
  }
  
} // end of class
