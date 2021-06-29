package com.nm.commerce.catalog;

import atg.repository.*;

import com.nm.components.CommonComponentHelper;

/**
 * Class represents a restriction code associated with shipping restrictions.
 * 
 * @author nmwps
 * 
 */
public class NMRestrictionCode {
  
  public static final String UNIT_CHARGE = "U";
  public static final String LINE_CHARGE = "L";
  public static final String SHIP_TO_CHARGE = "S";
  
  public static final String DUTY_AND_BROKERAGE = "DB";
  public static final String DELIVERY_AND_PROCESSING = "DP";
  public static final String ASEEMBLY_CODE = "W";
  
  private RepositoryItem mDataSource;
  
  public RepositoryItem getDataSource() {
    return mDataSource;
  }
  
  public void setDataSource(RepositoryItem dataSource) {
    mDataSource = dataSource;
  }
  
  /**
   * 
   * @param repositoryItem
   */
  public NMRestrictionCode(RepositoryItem repositoryItem) {
    setDataSource(repositoryItem);
  }
  
  /**
   * Created so that you don't have to type getDataSource() every time you want to call getPropertyValue
   * 
   * @param String
   *          propertyName
   * @return Object
   */
  public Object getPropertyValue(String propertyName) {
    try {
      return getDataSource().getPropertyValue(propertyName);
    } catch (Exception e) {
      e.printStackTrace();
    }
    
    return null;
  }
  
  /**
   * 
   * created so that you don't have to type getDataSource() every time you want to call setPropertyValue
   * 
   * @param String
   *          propertyName
   */
  public void setPropertyValue(String property, Object value) {
    try {
      ((MutableRepositoryItem) getDataSource()).setPropertyValue(property, value);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  
  /**
   * 
   * @return
   */
  public String getRestrictionCode() {
    return (String) getPropertyValue("restrictionCode");
  }
  
  /**
   * 
   * @return
   */
  public String getDescription() {
    return (String) getPropertyValue("description");
  }
  
  /**
   * 
   * @return
   */
  public String getDisplayName() {
    return (String) getPropertyValue("displayName");
  }
  
  /**
   * 
   * @return
   */
  public String getCalculationType() {
    return (String) getPropertyValue("calculationType");
  }
  
  /**
   * 
   * @return
   */
  public double getSurchargeAmount() {
    double returnValue = 0;
    
    Double surchargeAmount = (Double) getPropertyValue("surchargeAmount");
    
    if (surchargeAmount != null) {
      returnValue = surchargeAmount.doubleValue();
    }
    
    return returnValue;
  }
  
  /**
   * 
   * @return
   */
  public int getAdditionalDeliveryDays() {
    int returnValue = 0;
    
    Integer additionalDeliveryDays = (Integer) getPropertyValue("additionalDeliveryDays");
    
    if (additionalDeliveryDays != null) {
      returnValue = additionalDeliveryDays.intValue();
    }
    
    return returnValue;
  }
  
  /**
   * 
   * @return
   */
  public boolean isTaxable() {
    boolean returnValue = false;
    
    Boolean taxable = (Boolean) getPropertyValue("taxableFlag");
    
    if (taxable != null) {
      returnValue = taxable.booleanValue();
    }
    
    return returnValue;
  }
  
  /**
   * 
   * @return
   */
  public String getClassification() {
    return (String) getPropertyValue("classification");
  }
  
  /**
   * 
   * @param code
   * @return
   * @throws Exception
   */
  public static NMRestrictionCode getRestrictionCode(String code) throws Exception {
    NMRestrictionCode restrictionCode = null;
    
    Repository repository = CommonComponentHelper.getProductRepository();
    
    RepositoryItem item = repository.getItem(code, "shippingRestrictionCode");
    
    if (item != null) {
      restrictionCode = new NMRestrictionCode(item);
    }
    
    return restrictionCode;
  }
}
