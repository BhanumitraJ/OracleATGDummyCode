/*
 * <ATGCOPYRIGHT> Copyright (C) 2001 Art Technology Group, Inc. All Rights Reserved. No use, copying or distribution ofthis work may be made except in accordance with a valid license agreement from
 * Art Technology Group. This notice must be included on all copies, modifications and derivatives of this work.
 * 
 * Art Technology Group (ATG) MAKES NO REPRESENTATIONS OR WARRANTIES ABOUT THE SUITABILITY OF THE SOFTWARE, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, OR NON-INFRINGEMENT. ATG SHALL NOT BE LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR DISTRIBUTING THIS SOFTWARE OR
 * ITS DERIVATIVES.
 * 
 * "Dynamo" is a trademark of Art Technology Group, Inc. </ATGCOPYRIGHT>
 */

package com.nm.commerce.pricing;

import atg.commerce.pricing.*;

import java.util.*;

import com.nm.commerce.catalog.NMRestrictionCode;

/**
 * Extended Dynamo Item Price Info class.
 * 
 * @author Chee-Chien Loo
 */

public class NMItemPriceInfo extends ItemPriceInfo {
  private static final long serialVersionUID = 1L;
  
  // ---------------------------------------------------------------------------
  // property: freightCharges
  
  double mFreightCharges;
  double mStoreRetailPriceDiscountAmount;
  
  public void setFreightCharges(double pFreightCharges) {
    mFreightCharges = pFreightCharges;
  }
  
  public double getFreightCharges() {
    return mFreightCharges;
  }
  
  // ---------------------------------------------------------------------------
  // property: otherCharges
  double mOtherCharges;
  
  public void setOtherCharges(double pOtherCharges) {
    mOtherCharges = pOtherCharges;
  }
  
  public double getOtherCharges() {
    return mOtherCharges;
  }
  
  // ---------------------------------------------------------------------------
  // property: tax
  double mTax;
  
  public void setTax(double pTax) {
    mTax = pTax;
  }
  
  public double getTax() {
    return mTax;
  }
  
  // ---------------------------------------------------------------------------
  // property: assemblyDiscount
  double mAssemblyDiscount;
  
  public void setAssemblyDiscount(double pAssemblyDiscount) {
    mAssemblyDiscount = pAssemblyDiscount;
  }
  
  public double getAssemblyDiscount() {
    return mAssemblyDiscount;
  }
  
  /**
   * Returns total delivery and processing charges which include freight plus restriction charges (that were classified as D&P).
   * 
   * Returns the total for taxable and non taxable items.
   * 
   * @return
   */
  public double getDeliveryAndProcessingTotal() {
    return getDeliveryAndProcessingTotal(true);
  }
  
  /**
   * Returns total delivery and processing charges which include freight plus restriction charges (that were classified as D&P).
   * 
   * Returns the total for taxable and/or non taxable items based on the includeNonTaxable parameter.
   * 
   * @param includeNonTaxable
   * @return
   */
  public double getDeliveryAndProcessingTotal(boolean includeNonTaxable) {
    double returnValue = getFreightCharges();
    
    try {
      returnValue += mRestrictionChargeHelper.getItemRestrictionChargeTotal(NMRestrictionCode.DELIVERY_AND_PROCESSING, includeNonTaxable);
    } catch (Exception exception) {
      exception.printStackTrace();
    }
    
    return returnValue;
  }
  
  /**
   * Returns total duty and brokerage charges which include duty plus restriction charges (that were classified as D&B).
   * 
   * Returns the total for taxable and non taxable items.
   * 
   * @return
   */
  public double getDutyAndBrokerageTotal() {
    return getDutyAndBrokerageTotal(true);
  }
  
  /**
   * Returns total duty and brokerage charges which include duty plus restriction charges (that were classified as D&B).
   * 
   * Returns the total for taxable and/or non taxable items based on the includeNonTaxable parameter.
   * 
   * @param includeNonTaxable
   * @return
   */
  public double getDutyAndBrokerageTotal(boolean includeNonTaxable) {
    double returnValue = getDutyCharge();
    
    try {
      returnValue += mRestrictionChargeHelper.getItemRestrictionChargeTotal(NMRestrictionCode.DUTY_AND_BROKERAGE, includeNonTaxable);
    } catch (Exception exception) {
      exception.printStackTrace();
    }
    
    return returnValue;
  }
  
  private NMRestrictionChargeHelper mRestrictionChargeHelper = new NMRestrictionChargeHelper();
  
  /**
   * Set the restriction charge map (via the helper)
   * 
   * @param map
   */
  public void setItemRestrictionCharges(Map<String, Double> map) {
    mRestrictionChargeHelper.setItemRestrictionCharges(map);
  }
  
  /**
   * Gets the restriction charge map (via the helper)
   * 
   * @return
   */
  public Map<String, Double> getItemRestrictionCharges() {
    return mRestrictionChargeHelper.getItemRestrictionCharges();
  }
  
  /**
   * Adds a restriction charge (via the helper)
   * 
   * @param restrictionCode
   * @param charge
   */
  public void addItemRestrictionCharge(String restrictionCode, double charge) {
    mRestrictionChargeHelper.addItemRestrictionCharge(restrictionCode, charge);
  }
  
  /**
   * Removes a restriction charge (via the helper)
   * 
   */
  public void removeItemRestrictionCharge() {
    mRestrictionChargeHelper.clearRestrictionCharges();
  }
  
  /**
   * The commerceItem's duty charge.
   * 
   * @param dutyCharge
   */
  double dutyCharge;
  
  public double getDutyCharge() {
    return dutyCharge;
  }
  
  public void setDutyCharge(double dutyCharge) {
    this.dutyCharge = dutyCharge;
  }
  
  /**
   * The commerceItem's promotional price.
   * 
   * @param promotionalPrice
   */
  double promotionalPrice;
  
  public double getPromotionalPrice() {
    return promotionalPrice;
  }
  
  public void setPromotionalPrice(double promotionalPrice) {
    this.promotionalPrice = promotionalPrice;
  }
  
  // --------------------------------------------------------------------------
  // property: discountAmount
  public double getDiscountAmount() {
    return getRawTotalPrice() - getAmount();
  }
  
  /**
   * nmve1
   */
  public double getAmount() {
    return super.getAmount() - getStoreRetailPriceDiscount();
  }
  
  /**
   * nmve1
   */
  public void setAmount(double amount) {
    super.setAmount(amount);
  }
  
  /**
   * nmve1
   */
  public void setRawTotalPrice(double rawTotalPrice) {
    super.setRawTotalPrice(rawTotalPrice);
  }
  
  /**
   * nmve1
   */
  public double getRawTotalPrice() {
    return super.getRawTotalPrice() - getStoreRetailPriceDiscount();
  }
  
  /**
   * nmve1
   */
  public double getStoreRetailPriceDiscount() {
    return mStoreRetailPriceDiscountAmount;
  }
  
  /**
   * nmve1
   */
  public void setStoreRetailPriceDiscount(double amount) {
    mStoreRetailPriceDiscountAmount = amount;
  }
  
  /**
   * The discount percentage applied from a percent off promo
   * 
   * @param promoPercentOff
   */
  String promoPercentOff;
  
  public String getPromoPercentOff() {
    return promoPercentOff;
  }
  
  public void setPromoPercentOff(String promoPercentOff) {
    this.promoPercentOff = promoPercentOff;
  }
  
  /**
   * The discount percentage applied from am extra percent off promo (stackable/non-stackable)
   * 
   * @param promoPercentOff
   */
  String promoExtraPercentOff;
  
  public String getPromoExtraPercentOff() {
    return promoExtraPercentOff;
  }
  
  public void setPromoExtraPercentOff(String promoExtraPercentOff) {
    this.promoExtraPercentOff = promoExtraPercentOff;
  }
  
  Map<String, Markdown> promotionsApplied = new HashMap<String, Markdown>();
  
  public Map<String, Markdown> getPromotionsApplied() {
    return promotionsApplied;
  }
  
  public void setPromotionsApplied(Map<String, Markdown> promotionsApplied) {
    this.promotionsApplied = promotionsApplied;
  }
  
  double employeeExtraDiscountPercent;
  double employeeDiscountPercent;
  double employeeDiscountAmount;
  double employeeExtraDiscountAmount;

/**
 * @return the employeeExtraDiscountPercent
 */
public double getEmployeeExtraDiscountPercent() {
	return employeeExtraDiscountPercent;
}

/**
 * @param employeeExtraDiscountPercent the employeeExtraDiscountPercent to set
 */
public void setEmployeeExtraDiscountPercent(double employeeExtraDiscountPercent) {
	this.employeeExtraDiscountPercent = employeeExtraDiscountPercent;
}

/**
 * @return the employeeDiscountPercent
 */
public double getEmployeeDiscountPercent() {
	return employeeDiscountPercent;
}

/**
 * @param employeeDiscountPercent the employeeDiscountPercent to set
 */
public void setEmployeeDiscountPercent(double employeeDiscountPercent) {
	this.employeeDiscountPercent = employeeDiscountPercent;
}

/**
 * @return the employeeDiscountAmount
 */
public double getEmployeeDiscountAmount() {
	return employeeDiscountAmount;
}

/**
 * @param employeeDiscountAmount the employeeDiscountAmount to set
 */
public void setEmployeeDiscountAmount(double employeeDiscountAmount) {
	this.employeeDiscountAmount = employeeDiscountAmount;
}

/**
 * @return the employeeExtraDiscountAmount
 */
public double getEmployeeExtraDiscountAmount() {
	return employeeExtraDiscountAmount;
}

/**
 * @param employeeExtraDiscountAmount the employeeExtraDiscountAmount to set
 */
public void setEmployeeExtraDiscountAmount(double employeeExtraDiscountAmount) {
	this.employeeExtraDiscountAmount = employeeExtraDiscountAmount;
}
  
}
