/**
 * 
 */
package com.nm.commerce.catalog;

import java.sql.Date;
import java.sql.ResultSet;

/**
 * @author Kory Jackson This class will help serve components from the Product Load Table.
 */
public class NMProductLoad {
  private String cmosCatalogId = null;
  private String cmosItemCode = null;
  private String proofName = null;
  private Date proofLoadDate = null;
  private String officeName = null;
  private String merchandiseClass = null;
  private String webDesignerName = null;
  private String editorialTracking = null;
  private int hidden;
  private int parked;
  
  /**
   * @return the cmosCatalogId
   */
  public String getCmosCatalogId() {
    return cmosCatalogId;
  }
  
  /**
   * @return the cmosItemCode
   */
  public String getCmosItemCode() {
    return cmosItemCode;
  }
  
  /**
   * @return the proofName
   */
  public String getProofName() {
    return proofName;
  }
  
  /**
   * @return the proofLoadDate
   */
  public Date getProofLoadDate() {
    return proofLoadDate;
  }
  
  /**
   * @return the officeName
   */
  public String getOfficeName() {
    return officeName;
  }
  
  /**
   * @return the merchandiseClass
   */
  public String getMerchandiseClass() {
    return merchandiseClass;
  }
  
  /**
   * @return the webDesignerName
   */
  public String getWebDesignerName() {
    return webDesignerName;
  }
  
  /**
   * @return the editorialTracking
   */
  public String getEditorialTracking() {
    return editorialTracking;
  }
  
  /**
   * @return the hidden
   */
  public int getHidden() {
    return hidden;
  }
  
  /**
   * @return the parked
   */
  public int getParked() {
    return parked;
  }
  
  /**
   * @param cmosCatalogId
   *          the cmosCatalogId to set
   */
  public void setCmosCatalogId(String cmosCatalogId) {
    this.cmosCatalogId = cmosCatalogId;
  }
  
  /**
   * @param cmosItemCode
   *          the cmosItemCode to set
   */
  public void setCmosItemCode(String cmosItemCode) {
    this.cmosItemCode = cmosItemCode;
  }
  
  /**
   * @param proofName
   *          the proofName to set
   */
  public void setProofName(String proofName) {
    this.proofName = proofName;
  }
  
  /**
   * @param proofLoadDate
   *          the proofLoadDate to set
   */
  public void setProofLoadDate(Date proofLoadDate) {
    this.proofLoadDate = proofLoadDate;
  }
  
  /**
   * @param officeName
   *          the officeName to set
   */
  public void setOfficeName(String officeName) {
    this.officeName = officeName;
  }
  
  /**
   * @param merchandiseClass
   *          the merchandiseClass to set
   */
  public void setMerchandiseClass(String merchandiseClass) {
    this.merchandiseClass = merchandiseClass;
  }
  
  /**
   * @param webDesignerName
   *          the webDesignerName to set
   */
  public void setWebDesignerName(String webDesignerName) {
    this.webDesignerName = webDesignerName;
  }
  
  /**
   * @param editorialTracking
   *          the editorialTracking to set
   */
  public void setEditorialTracking(String editorialTracking) {
    this.editorialTracking = editorialTracking;
  }
  
  /**
   * @param hidden
   *          the hidden to set
   */
  public void setHidden(int hidden) {
    this.hidden = hidden;
  }
  
  /**
   * @param parked
   *          the parked to set
   */
  public void setParked(int parked) {
    this.parked = parked;
  }
  
  public NMProductLoad(String plCmosCatalogId, String plCmosItemCode, String plProofName, Date plProofLoadDate, String plOfficeName, String plMerchandiseClass, String plWebDesignerName,
          String plEditorialTracking, int plHidden, int plParked) {
    
    cmosCatalogId = plCmosCatalogId;
    cmosItemCode = plCmosItemCode;
    proofName = plProofName;
    proofLoadDate = plProofLoadDate;
    officeName = plOfficeName;
    merchandiseClass = plMerchandiseClass;
    webDesignerName = plWebDesignerName;
    editorialTracking = plEditorialTracking;
    hidden = plHidden;
    parked = plParked;
  }
  
  public NMProductLoad(ResultSet rslt) {
    if (rslt == null) return;
    try {
      cmosCatalogId = rslt.getString("cmos_catalog_id");
      cmosItemCode = rslt.getString("cmos_item");
      proofName = rslt.getString("PROOF_NAME");
      proofLoadDate = rslt.getDate("PROOF_LOAD_DATE");
      officeName = rslt.getString("OFFICE_NAME");
      merchandiseClass = rslt.getString("MERCHANDISE_CLASS");
      webDesignerName = rslt.getString("WEB_DESIGNER_NAME");
      editorialTracking = rslt.getString("EDITORIAL_TRACKING");
      hidden = rslt.getInt("HIDDEN");
      parked = rslt.getInt("PARKED");
    } catch (Exception e) {
      System.out.println("NMProductLoad constructor - We failed" + e + ".");
    }
    
  }
}
