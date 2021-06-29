package com.nm.commerce.pricing;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.nm.commerce.catalog.NMRestrictionCode;

/**
 * This class helps the PriceInfo classes (such as NMItemPriceInfo and NMOrderPriceInfo) manage their shipping restriction charges
 * 
 * @author nmwps
 * 
 */
public class NMRestrictionChargeHelper implements java.io.Serializable {
  private static final long serialVersionUID = 1L;
  
  private Map<String, Double> mItemRestrictionCharges;
  
  
  /**
   * Clears the restriction charges stored in the map
   */
  public void clearRestrictionCharges() {
    if (mItemRestrictionCharges != null) {
      mItemRestrictionCharges.clear();
    }
  }
  
  /**
   * Sets the restiction charge map
   * 
   * @param map
   */
  public void setItemRestrictionCharges(Map<String, Double> map) {
    mItemRestrictionCharges = map;
  }
  
  /**
   * Returns the restriction charge map
   * 
   * @return
   */
  public Map<String, Double> getItemRestrictionCharges() {
    return mItemRestrictionCharges;
  }
  
  /**
   * Combines the charge provides with of charges for the restriction code provided
   * 
   * @param restrictionCode
   * @param charge
   */
  public void accumulateItemRestrictionCharges(String restrictionCode, double charge) {
    if (mItemRestrictionCharges == null) {
      mItemRestrictionCharges = new HashMap<String, Double>();
    }
    
    Double existingCharge = (Double) mItemRestrictionCharges.get(restrictionCode);
    
    if (existingCharge == null) {
      mItemRestrictionCharges.put(restrictionCode, new Double(charge));
    } else {
      mItemRestrictionCharges.put(restrictionCode, new Double(existingCharge.doubleValue() + charge));
    }
  }
  
  /**
   * Add/replaces the charge for the restriction code provided.
   * 
   * @param restrictionCode
   * @param charge
   */
  public void addItemRestrictionCharge(String restrictionCode, double charge) {
    if (mItemRestrictionCharges == null) {
      mItemRestrictionCharges = new HashMap<String, Double>();
    }
    
    mItemRestrictionCharges.put(restrictionCode, new Double(charge));
  }
  
  /**
   * Returns a map where the key is the display name of the restriction code and the value is the charge for that restriction code. The classifcation and includeNonTaxable parameters are used to
   * filter the restriction codes returned.
   * 
   * @param classification
   * @param includeNonTaxable
   * @return
   * @throws Exception
   */
  public Map<String, Double> getItemRestrictionChargeDetails(String classification, boolean includeNonTaxable) throws Exception {
    HashMap<String, Double> map = new HashMap<String, Double>();
    
    if (mItemRestrictionCharges != null) {
      
      Set<String> keys = mItemRestrictionCharges.keySet();
      
      Iterator<String> iterator = keys.iterator();
      
      while (iterator.hasNext()) {
        String key = (String) iterator.next();
        
        NMRestrictionCode restrictionCode = NMRestrictionCode.getRestrictionCode(key);
        
        if (restrictionCode != null && classification.equalsIgnoreCase(restrictionCode.getClassification())) {
          if (includeNonTaxable || restrictionCode.isTaxable()) {
            
            Double value = (Double) mItemRestrictionCharges.get(key);
            
            map.put(restrictionCode.getRestrictionCode(), value);
          }
        }
      }
    }
    
    return map;
  }
  
  /**
   * Returns the total charge for restriction codes that have the correct classification and taxable status.
   * 
   * @param classification
   * @param includeNonTaxable
   * @return
   * @throws Exception
   */
  public double getItemRestrictionChargeTotal(String classification, boolean includeNonTaxable) throws Exception {
    double returnValue = 0.0;
    
    Map<String, Double> itemRestrictionChargeDetails = getItemRestrictionChargeDetails(classification, includeNonTaxable);
    
    Collection<Double> values = itemRestrictionChargeDetails.values();
    
    Iterator<Double> iterator = values.iterator();
    
    while (iterator.hasNext()) {
      Double value = (Double) iterator.next();
      
      returnValue += value.doubleValue();
    }
    
    return returnValue;
  }
  
  /**
   * Returns the total charge for restriction codes that have the correct classification,taxable status and restriction code.
   * 
   * @param classification
   * @param includeNonTaxable
   * @return
   * @throws Exception
   */
  public double getItemRestrictionChargeTotalFromRestCode(String classification, boolean includeNonTaxable, String restrictionCode) throws Exception {
    double returnValue = 0.0;
    
    Map<String, Double> itemRestrictionChargeDetails = getItemRestrictionChargeDetails(classification, includeNonTaxable);
    if(itemRestrictionChargeDetails != null && itemRestrictionChargeDetails.containsKey("F") && restrictionCode == "F"){
      returnValue += (Double)itemRestrictionChargeDetails.get("F");
    } else if(itemRestrictionChargeDetails != null && restrictionCode == "O"){
      itemRestrictionChargeDetails.remove("F");
      Collection<Double> values = itemRestrictionChargeDetails.values();
      
      Iterator<Double> iterator = values.iterator();
      
      while (iterator.hasNext()) {
        Double value = (Double) iterator.next();
        
        returnValue += value.doubleValue();
      }
    }
    return returnValue;
  }
}
