package com.nm.commerce.catalog.core;

import java.io.File;

/**
 * Refreshable element provides the options available for configuring a refreshable element for use on the site.
 * 
 * @author nmjjm4
 */
public class RefreshableElement extends TemplateElement {
  
  /**
   * @return the refreshable element type. Options include
   *         <ul>
   *         <li>0 - File Asset</li>
   *         <li>1 - Text Asset</li>
   *         <li>2 - Image Asset</li>
   *         </ul>
   */
  public Integer getRefreshableType() {
    return refreshableType;
  }
  
  /**
   * @return true if the path to the category file should be determined based on the category id of the file.
   */
  public Boolean getUseCategoryPathToFile() {
    return useCategoryPathToFile;
  }
  
  /**
   * @return the name of the file, this value should include the complete file path if useCategoryPathToFile is false
   */
  public String getFileName() {
    return fileName;
  }
  
  /**
   * @return the name of the file to use when this is in the sale silo. this value should include the complete file path if useCategoyrPathToFile is false;
   */
  public String getSaleFileName() {
    return saleFileName;
  }
  
  /**
   * @returns the text to display on the asset when the type is text based.
   */
  public String getAssetText() {
    return assetText;
  }
  
  /**
   * @return the comma-delimited list of file names for specific locales.
   */
  public String getLocaleFileNameMappings() {
    return localeFileNameMappings;
  }
  
  /**
   * @set refreshableType the refreshable element type. Options include
   *      <ul>
   *      <li>0 - File Asset</li>
   *      <li>1 - Text Asset</li>
   *      <li>2 - Image Asset</li>
   *      </ul>
   */
  public void setRefreshableType(Integer refreshableType) {
    this.refreshableType = refreshableType;
  }
  
  /**
   * @param useCategoryPathToFile
   *          should be true if the path to the file should be determined based on the category id of the file.
   */
  public void setUseCategoryPathToFile(Boolean useCategoryPathToFile) {
    this.useCategoryPathToFile = useCategoryPathToFile;
  }
  
  /**
   * @param fileName
   *          the name of the file, this value should include the complete file path if useCategoryPathToFile is false
   */
  public void setFileName(String fileName) {
    this.fileName = fileName;
  }
  
  /**
   * @param saleFileName
   *          the name of the file to use when this is in the sale silo. this value should include the complete file path if useCategoyrPathToFile is false;
   */
  public void setSaleFileName(String saleFileName) {
    this.saleFileName = saleFileName;
  }
  
  /**
   * @param assetText
   *          the text to display on the asset when the type is text based.
   */
  public void setAssetText(String assetText) {
    this.assetText = assetText;
  }
  
  /**
   * @return the allowFileOverride
   */
  public Boolean getAllowFileOverride() {
    return allowFileOverride;
  }
  
  /**
   * @param allowFileOverride
   *          indicates whether or not the refreshable HTML can be overriden by a hard-coded path to a refreshable html file. This is primarily used in conjunction with assets with
   *          useCategoryPathToFile=true.
   * 
   *          The problem we are solving with this attribute is an issue in Horchow dealing with the G-block in which we want to replace the individual static category images with a single "event"
   *          image that is the same for many categories. This attribute will allow us to overrid the category-based image with the centralized static image.
   * 
   *          For example: If allowFileOverride is true for an element, then, PRIOR to rendering the refreshable file, we'll check the asset override field (in the DB by category) for an override
   *          path. If it exists, we'll render it, otherwise, we'll continue on to the normal processing for rendering the refreshable asset.
   */
  public void setAllowFileOverride(Boolean allowFileOverride) {
    this.allowFileOverride = allowFileOverride;
  }
  
  /**
   * 
   * @param localeFileNameMappings
   *          the comma-delimited list of file names for specific locales.
   */
  public void setLocaleFileNameMappings(String localeFileNameMappings) {
    this.localeFileNameMappings = localeFileNameMappings;
  }
  
  private Integer refreshableType;
  private Boolean useCategoryPathToFile;
  private Boolean allowFileOverride;
  private String fileName;
  private String saleFileName;
  private String assetText;
  private String localeFileNameMappings;
  
  public static final Integer FILE_ASSET = new Integer(0);
  public static final Integer TEXT_ASSET = new Integer(1);
  public static final Integer IMAGE_ASSET = new Integer(2);
  
}
