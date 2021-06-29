package com.nm.commerce.catalog;

import java.util.*;

import atg.repository.*;

public class NMProdCountry {
  private RepositoryItem dataSource;
  
  public RepositoryItem getDataSource() {
    return dataSource;
  }
  
  public void setDataSource(RepositoryItem dataSource) {
    this.dataSource = dataSource;
  }
  
  public NMProdCountry(RepositoryItem repositoryItem) {
    setDataSource(repositoryItem);
  }
  
  public Object getPropertyValue(String propertyName) {
    try {
      return getDataSource().getPropertyValue(propertyName);
    } catch (Exception e) {
      e.printStackTrace();
    }
    
    return null;
  }
  
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
  public List<NMRestrictionCode> getRestrictionCodes() {
    ArrayList<NMRestrictionCode> returnValue = new ArrayList<NMRestrictionCode>();
    
    @SuppressWarnings("unchecked")
    Set<RepositoryItem> restrictionCodes = (Set<RepositoryItem>) getPropertyValue("prodCountryRestrictionCodes");
    
    if (restrictionCodes != null) {
      Iterator<RepositoryItem> iterator = restrictionCodes.iterator();
      
      while (iterator.hasNext()) {
        RepositoryItem item = (RepositoryItem) iterator.next();
        returnValue.add(new NMRestrictionCode(item));
      }
    }
    
    return returnValue;
  }
  
  /**
   * 
   * @param restrictionCodes
   */
  public void setRestrictionCodes(Set<NMRestrictionCode> restrictionCodes) {
    setPropertyValue("prodCountryRestrictionCodes", restrictionCodes);
  }
  
  public String getRepositoryId() {
    return getDataSource().getRepositoryId();
  }
  
  public String getId() {
    return getDataSource().getRepositoryId();
  }
  
  public String getProductId() {
    return (String) getPropertyValue("productId");
  }
  
  public void setProductId(String productId) {
    setPropertyValue("productId", productId);
  }
  
  public String getCountryCode() {
    return (String) getPropertyValue("countryCode");
  }
  
  public void setCountryCode(String countryCode) {
    setPropertyValue("countryCode", countryCode);
  }
  
  public Boolean getFlgParenthetical() {
    return (Boolean) getPropertyValue("flgParenthetical");
  }
  
  public void setFlgParenthetical(Boolean flgParenthetical) {
    setPropertyValue("flgParenthetical", flgParenthetical);
  }
  
  public Double getParentheticalCharge() {
    return (Double) getPropertyValue("parentheticalCharge");
  }
  
  public void setParentheticalCharge(Double parentheticalCharge) {
    setPropertyValue("parentheticalCharge", parentheticalCharge);
  }
  
  public RepositoryItem getChartId() {
    return (RepositoryItem) getPropertyValue("chartId");
  }
  
  public void setChartId(RepositoryItem chartId) {
    setPropertyValue("chartId", chartId);
  }
  
  public Double getDutyRate() {
    return (Double) getPropertyValue("dutyRate");
  }
  
  public void setDutyRate(Double dutyRate) {
    setPropertyValue("dutyRate", dutyRate);
  }
  
  public String getServiceLevelCodes() {
    return (String) getPropertyValue("serviceLevelCodes");
  }
  
  public void setServiceLevelCodes(String serviceLevelCodes) {
    setPropertyValue("serviceLevelCodes", serviceLevelCodes);
  }
  
  public List<RepositoryItem> getRestrictedProvinces() {
    @SuppressWarnings("unchecked")
    List<RepositoryItem> result = (List<RepositoryItem>) getPropertyValue("restrictedProvinces");
    return result;
  }
  
  public void setRestrictedProvinces(List<RepositoryItem> restrictedProvinces) {
    setPropertyValue("restrictedProvinces", restrictedProvinces);
  }
}
