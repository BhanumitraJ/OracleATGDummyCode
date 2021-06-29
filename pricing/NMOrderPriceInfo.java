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

import java.util.Map;
import java.util.TreeMap;

import atg.commerce.pricing.OrderPriceInfo;

import com.nm.commerce.catalog.NMRestrictionCode;

/**
 * Extended Dynamo Order Price Info class.
 * 
 * @author Chee-Chien Loo
 */

public class NMOrderPriceInfo extends OrderPriceInfo {
  // ---------------------------------------------------------------------------
  // property: giftWrapping
  
  double mGiftWrapping;
  double fishWildLifeFee;
  double orderDiscountTotal;
  
  public double getOrderDiscountTotal() {
    return orderDiscountTotal;
  }
  
  public void setOrderDiscountTotal(double orderDiscountTotal) {
    this.orderDiscountTotal = orderDiscountTotal;
  }
  
  public double getFishWildLifeFee() {
    
    return fishWildLifeFee;
  }
  
  public void setFishWildLifeFee(double fishWildLifeFee) {
    this.fishWildLifeFee = fishWildLifeFee;
  }
  
  public void setGiftWrapping(double pGiftWrapping) {
    mGiftWrapping = pGiftWrapping;
  }
  
  public double getGiftWrapping() {
    return mGiftWrapping;
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
   * Returns the total for the order
   */
  public double getTotal() {
    return round(getAmount() + getDeliveryAndProcessingTotal(true) + getTax() + getGiftWrapping() + getDutyAndBrokerageTotal(true));
  }
  
  public double round(double pNumber) {
    return (new Long(Math.round(pNumber * (Math.pow(10, 2))))).doubleValue() / Math.pow(10, 2);
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
    double returnValue = getShipping();
    
    try {
      returnValue += mRestrictionChargeHelper.getItemRestrictionChargeTotal(NMRestrictionCode.DELIVERY_AND_PROCESSING, includeNonTaxable);
    } catch (Exception exception) {
      exception.printStackTrace();
    }
    
    return returnValue;
  }
  /**
   * Returns total delivery and processing charges which include freight plus restriction charges (that were classified as D&P), excluding F+W charges
   * 
   * Returns the total for taxable and/or non taxable items based on the includeNonTaxable parameter.
   * 
   * @param includeNonTaxable
   * @return
   */
  public double getDandPTotalWithoutFishWildlife() {
    double returnValue = getShipping();
    
    try {
      returnValue += mRestrictionChargeHelper.getItemRestrictionChargeTotalFromRestCode(NMRestrictionCode.DELIVERY_AND_PROCESSING, true, "O");
    } catch (Exception exception) {
      exception.printStackTrace();
    }
    
    return returnValue;
  }
  
  /**
   * Returns F+W restriction charges (that were classified as D&P).
   * 
   * Returns the total for taxable and/or non taxable items based on the includeNonTaxable parameter.
   * 
   * @param includeNonTaxable
   * @return
   */
  public double getFishAndWildLifeChargesForBG() {
    double returnValue = 0.0;
    
    try {
      returnValue += mRestrictionChargeHelper.getItemRestrictionChargeTotalFromRestCode(NMRestrictionCode.DELIVERY_AND_PROCESSING, true, "F");
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
    double returnValue = getTotalDuty();
    
    try {
      returnValue += mRestrictionChargeHelper.getItemRestrictionChargeTotal(NMRestrictionCode.DUTY_AND_BROKERAGE, includeNonTaxable);
    } catch (Exception exception) {
      exception.printStackTrace();
    }
    
    return returnValue;
  }
  
  // TotalDuty is the order's total Duty charge. We dont save it in the order separately since we save each line items duty charge.
  // This is mainly for convenience on the jhtml pages.
  // Should probably do this better sometime later...
  public double mTotalDuty;
  
  public double getTotalDuty() {
    return mTotalDuty;
  }
  
  public void setTotalDuty(double pTotalDuty) {
    mTotalDuty = pTotalDuty;
  }
  
  /**
   * Returns the detail breakout of the delivery and processing charges. It will not return any lines if the only item is the shipping/freight.
   * 
   * @return
   */
  public Map getDeliveryAndProcessingDetail() {
    TreeMap map = new TreeMap();
    
    try {
      
      Map itemRestrictionChargeDetails = mRestrictionChargeHelper.getItemRestrictionChargeDetails(NMRestrictionCode.DELIVERY_AND_PROCESSING, true);
      
      // if we have item restriction charges then add the standard
      // delivery and processing and the item charges to the map
      if (itemRestrictionChargeDetails.size() > 0) {
        map.put("Delivery and Processing", new Double(getShipping()));
        map.putAll(itemRestrictionChargeDetails);
      }
      
    } catch (Exception exception) {
      exception.printStackTrace();
    }
    
    return map;
  }
  
  /**
   * Returns the detail breakout of the duty and freight charges. It will not return any lines if the only item is the duty.
   * 
   * @return
   */
  public Map getDutyAndBrokerageDetail() {
    TreeMap map = new TreeMap();
    
    try {
      Map itemRestrictionChargeDetails = mRestrictionChargeHelper.getItemRestrictionChargeDetails(NMRestrictionCode.DUTY_AND_BROKERAGE, true);
      
      // if we have item restriction charges then add the standard
      // duty and brokerage and the item charges to the map
      if (itemRestrictionChargeDetails.size() > 0) {
        map.put("Duty and Brokerage", new Double(this.getTotalDuty()));
        map.putAll(itemRestrictionChargeDetails);
      }
    } catch (Exception exception) {
      exception.printStackTrace();
    }
    
    return map;
  }
  
  private NMRestrictionChargeHelper mRestrictionChargeHelper = new NMRestrictionChargeHelper();
  
  /**
   * 
   */
  public void clearRestrictionCharges() {
    mRestrictionChargeHelper.clearRestrictionCharges();
  }
  
  /**
   * 
   * @param map
   */
  public void setItemRestrictionCharges(Map map) {
    mRestrictionChargeHelper.setItemRestrictionCharges(map);
  }
  
  /**
   * 
   * @return
   */
  public Map getItemRestrictionCharges() {
    return mRestrictionChargeHelper.getItemRestrictionCharges();
  }
  
  /**
   * 
   * @param restrictionCode
   * @param charge
   */
  public void accumulateItemRestrictionCharges(String restrictionCode, double charge) {
    mRestrictionChargeHelper.accumulateItemRestrictionCharges(restrictionCode, charge);
  }
}
